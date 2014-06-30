//==============================================================================
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
//===	Contact: Jeroen Ticheler email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.kernel;

/**
 * Template Method abstract class to handle faster indexing of many metadata
 * documents.
 * Each subclass must implement process method and define a custom constructor 
 * with all parameters required to exec the process method
 */
public abstract class MetadataIndexerProcessor {
    private DataManager dm;

    public MetadataIndexerProcessor(DataManager dm) {
        this.dm = dm;
    }

    public abstract void process() throws Exception;

    protected DataManager getDataManager() {
        return dm;
    }
}
