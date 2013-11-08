//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.kernel.mef;

import static org.fao.geonet.kernel.mef.MEFConstants.DIR_PRIVATE;
import static org.fao.geonet.kernel.mef.MEFConstants.DIR_PUBLIC;
import static org.fao.geonet.kernel.mef.MEFConstants.FILE_INFO;
import static org.fao.geonet.kernel.mef.MEFConstants.FILE_METADATA;
import static org.fao.geonet.kernel.mef.MEFConstants.FILE_METADATA_19139;
import static org.fao.geonet.kernel.mef.MEFConstants.FS;
import static org.fao.geonet.kernel.mef.MEFConstants.MD_DIR;
import static org.fao.geonet.kernel.mef.MEFConstants.SCHEMA;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import jeeves.server.context.ServiceContext;
import org.fao.geonet.utils.Xml;

import org.fao.geonet.Constants;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataRelation;
import org.fao.geonet.domain.ReservedOperation;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.mef.MEFLib.Format;
import org.fao.geonet.kernel.mef.MEFLib.Version;
import org.fao.geonet.kernel.schema.MetadataSchema;
import org.fao.geonet.lib.Lib;
import org.fao.geonet.repository.MetadataRelationRepository;
import org.jdom.Element;

class MEF2Exporter {
	/**
	 * Create a MEF2 file in ZIP format.
	 * 
	 * @param context
	 * @param uuids
	 *            List of records to export.
	 * @param format
	 *            {@link Format} to export.
	 * @param skipUUID
	 * @param stylePath
	 * @return MEF2 File
	 * @throws Exception
	 */
	public static String doExport(ServiceContext context, Set<String> uuids,
			Format format, boolean skipUUID, String stylePath, boolean resolveXlink, boolean removeXlinkAttribute) throws Exception {

		File file = File.createTempFile("mef-", ".mef");
		FileOutputStream fos = new FileOutputStream(file);
		ZipOutputStream zos = new ZipOutputStream(fos);

        for (Object uuid1 : uuids) {
            String uuid = (String) uuid1;
            createMetadataFolder(context, uuid, zos, skipUUID, stylePath,
                    format, resolveXlink, removeXlinkAttribute);
        }

		// --- cleanup and exit
		zos.close();

		return file.getAbsolutePath();
	}

	/**
	 * Create a metadata folder according to MEF {@link Version} 2
	 * specification. If current record is based on an ISO profil, the
	 * stylesheet /convert/to19139.xsl is used to map to ISO. Both files are
	 * included in MEF file. Export relevant information according to format
	 * parameter.
	 * 
	 * @param context
	 * @param uuid
	 *            Metadata record to export
	 * @param zos
	 *            Zip file to add new record
	 * @param skipUUID
	 * @param stylePath
	 * @param format
	 * @throws Exception
	 */
	private static void createMetadataFolder(ServiceContext context,
			String uuid, ZipOutputStream zos, boolean skipUUID,
			String stylePath, Format format, boolean resolveXlink, boolean removeXlinkAttribute) throws Exception {

		MEFLib.createDir(zos, uuid + FS);

		Metadata record = MEFLib.retrieveMetadata(context, uuid, resolveXlink, removeXlinkAttribute);

		String id = "" + record.getId();
		String isTemp = record.getDataInfo().getType().codeString;
		String schema = record.getDataInfo().getSchemaId();

		if (!"y".equals(isTemp) && !"n".equals(isTemp))
			throw new Exception("Cannot export sub template");

		String pubDir = Lib.resource.getDir(context, "public", id);
		String priDir = Lib.resource.getDir(context, "private", id);

		// --- create folders
		MEFLib.createDir(zos, uuid + FS + DIR_PUBLIC);
		MEFLib.createDir(zos, uuid + FS + DIR_PRIVATE);

		// Always save metadata in iso 19139
		if (schema.contains("iso19139") && !schema.equals("iso19139")) {
			// ie. this is an ISO profil.
            GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
            DataManager dm = gc.getBean(DataManager.class);
		    MetadataSchema metadataSchema = dm.getSchema(schema);
			String path = metadataSchema.getSchemaDir() + "/convert/to19139.xsl";

			ByteArrayInputStream data19139 = formatData(record, true,
					path);
			MEFLib.addFile(zos, uuid + FS + MD_DIR + FILE_METADATA_19139,
					data19139);
		}

		// --- save native metadata
		ByteArrayInputStream data = formatData(record);
		MEFLib.addFile(zos, uuid + FS + MD_DIR + FILE_METADATA, data);

		// --- save Feature Catalog
		String ftUUID = getFeatureCatalogID(context, record.getId());
		if (!ftUUID.equals("")) {
			Metadata ft = MEFLib.retrieveMetadata(context, ftUUID, resolveXlink, removeXlinkAttribute);
			ByteArrayInputStream ftData = formatData(ft);
			MEFLib.addFile(zos, uuid + FS + SCHEMA + FILE_METADATA, ftData);
		}

		// --- save info file
		byte[] binData = MEFLib.buildInfoFile(context, record, format, pubDir,
				priDir, skipUUID).getBytes(Constants.ENCODING);

		MEFLib.addFile(zos, uuid + FS + FILE_INFO, new ByteArrayInputStream(
				binData));

		// --- save thumbnails and maps

		if (format == Format.PARTIAL || format == Format.FULL) {
			MEFLib.savePublic(zos, pubDir, uuid);
        }

		if (format == Format.FULL) {
			try {
                Lib.resource.checkPrivilege(context, id, ReservedOperation.download);
				MEFLib.savePrivate(zos, priDir, uuid);
			} catch (Exception e) {
				// Current user could not download private data
			}
		}
	}

	/**
	 * Format xml data
	 * 
	 * @param metadata
	 * @return
	 * @throws Exception
	 */
	private static ByteArrayInputStream formatData(Metadata metadata)
			throws Exception {
		return formatData(metadata, false, "");
	}

	/**
	 * Format xml data
	 * 
	 * @param metadata
	 * @param transform
	 * @return ByteArrayInputStream
	 * @throws Exception
	 */
	private static ByteArrayInputStream formatData(Metadata metadata,
			boolean transform, String stylePath) throws Exception {
		String xmlData = metadata.getData();

		Element md = Xml.loadString(xmlData, false);

		// Resolving Xlinks before export
		// md = Processor.processXLink(md);

		// Apply a stylesheet transformation when schema is ISO profil
		if (transform) {
			md = Xml.transform(md, stylePath);
		}

		String data = Xml.getString(md);

		if (!data.startsWith("<?xml")) {
			data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" + data;
        }

		byte[] binData = data.getBytes(Constants.ENCODING);

		return new ByteArrayInputStream(binData);
	}

	/**
	 * Get Feature Catalog ID if exists using relation table.
	 * 
	 * @param context
	 * @param metadataId
	 *            Metadata record id to search for feature catalogue for.
	 * @return String Feature catalogue uuid.
	 * @throws Exception
	 */
	private static String getFeatureCatalogID(ServiceContext context, int metadataId) throws Exception {
		GeonetContext gc = (GeonetContext) context
				.getHandlerContext(Geonet.CONTEXT_NAME);
		DataManager dm = gc.getBean(DataManager.class);

        List<MetadataRelation> relations = context.getBean(MetadataRelationRepository.class).findAllById_MetadataId(metadataId);

		if (relations.isEmpty()) {
			return "";
        }

		// Assume only one feature catalogue is available for a metadata record.
		int ftId =  relations.get(0).getId().getRelatedId();

        String ftUuid = dm.getMetadataUuid("" + ftId);

		return ftUuid != null ? ftUuid : "";
	}
}