package org.fao.geonet.repository;

import org.fao.geonet.domain.GeonetEntity;
import org.fao.geonet.domain.IsoLanguage;
import org.fao.geonet.domain.IsoLanguage_;
import org.jdom.Element;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test LocalizedEntityRepository
 * User: Jesse
 * Date: 9/9/13
 * Time: 3:16 PM
 */
public class LocalizedEntityRepositoryTest extends AbstractSpringDataTest {

    @Autowired
    private IsoLanguageRepository _repository;

    @Test
    public void testFindAllAsXml() throws Exception {
        IsoLanguage language = IsoLanguageRepositoryTest.createIsoLanguage(_inc);
        language.getLabelTranslations().put("eng", "eng1");
        language.getLabelTranslations().put("fra", "fra1");
        language = _repository.save(language);

        IsoLanguage language2 = IsoLanguageRepositoryTest.createIsoLanguage(_inc);
        language2.getLabelTranslations().put("eng", "eng2");
        language2.getLabelTranslations().put("fra", "fra2");
        language2 = _repository.save(language2);

        Element xml = _repository.findAllAsXml();

        assertEquals(IsoLanguage.class.getSimpleName().toLowerCase(), xml.getName());
        assertEquals(2, xml.getChildren().size());

        for (Element element : (List<Element>) xml.getChildren()) {
            assertEquals(GeonetEntity.RECORD_EL_NAME, element.getName());
            assertNotNull(element.getChild(GeonetEntity.LABEL_EL_NAME));

            IsoLanguage entity = language;
            if (element.getChildText("id").equalsIgnoreCase("" + language2.getId())) {
                entity = language2;
            }

            assertEquals(entity.getId(), Integer.valueOf(element.getChildText("id")).intValue());
            assertEquals(entity.getCode(), element.getChildText("code"));
            assertEquals(entity.getShortCode(), element.getChildText("shortcode"));
            assertEquals(entity.getLabel("eng"), element.getChild(GeonetEntity.LABEL_EL_NAME).getChildText("eng"));
            assertEquals(entity.getLabel("fra"), element.getChild(GeonetEntity.LABEL_EL_NAME).getChildText("fra"));
        }
    }

    @Test
    public void testFindAllAsXmlSpec() throws Exception {
        IsoLanguage language = IsoLanguageRepositoryTest.createIsoLanguage(_inc);
        language.getLabelTranslations().put("eng", "eng1");
        language.getLabelTranslations().put("fra", "fra1");
        language = _repository.save(language);

        final int firstId = language.getId();

        IsoLanguage language2 = IsoLanguageRepositoryTest.createIsoLanguage(_inc);
        language2.getLabelTranslations().put("eng", "eng2");
        language2.getLabelTranslations().put("fra", "fra2");
        language2 = _repository.save(language2);

        Element xml = _repository.findAllAsXml(new Specification<IsoLanguage>() {
            @Override
            public Predicate toPredicate(Root<IsoLanguage> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

                return cb.gt(root.get(IsoLanguage_.id), firstId);
            }
        });

        assertEquals(IsoLanguage.class.getSimpleName().toLowerCase(), xml.getName());
        assertEquals(1, xml.getChildren().size());

        Element element = (Element) xml.getChildren().get(0);
        assertEquals(GeonetEntity.RECORD_EL_NAME, element.getName());
        assertNotNull(element.getChild(GeonetEntity.LABEL_EL_NAME));

        IsoLanguage entity = language;
        if (element.getChildText("id").equalsIgnoreCase("" + language2.getId())) {
            entity = language2;
        }

        assertEquals(entity.getId(), Integer.valueOf(element.getChildText("id")).intValue());
        assertEquals(entity.getCode(), element.getChildText("code"));
        assertEquals(entity.getShortCode(), element.getChildText("shortcode"));
        assertEquals(entity.getLabel("eng"), element.getChild(GeonetEntity.LABEL_EL_NAME).getChildText("eng"));
        assertEquals(entity.getLabel("fra"), element.getChild(GeonetEntity.LABEL_EL_NAME).getChildText("fra"));
    }


    @Test
    public void testFindAllAsXmlSortBy() throws Exception {
        IsoLanguage language = IsoLanguageRepositoryTest.createIsoLanguage(_inc);
        language.getLabelTranslations().put("eng", "eng1");
        language.getLabelTranslations().put("fra", "fra1");
        language = _repository.save(language);

        IsoLanguage language2 = IsoLanguageRepositoryTest.createIsoLanguage(_inc);
        language2.getLabelTranslations().put("eng", "eng2");
        language2.getLabelTranslations().put("fra", "fra2");
        language2 = _repository.save(language2);

        Element xml = _repository.findAllAsXml(null, new Sort(Sort.Direction.DESC, IsoLanguage_.id.getName()));

        assertEquals(String.valueOf(language2.getId()), ((Element) xml.getChildren().get(0)).getChildText("id"));
        assertEquals(String.valueOf(language.getId()), ((Element) xml.getChildren().get(1)).getChildText("id"));

        xml = _repository.findAllAsXml(null, new Sort(Sort.Direction.ASC, IsoLanguage_.id.getName()));

        assertEquals(String.valueOf(language.getId()), ((Element) xml.getChildren().get(0)).getChildText("id"));
        assertEquals(String.valueOf(language2.getId()), ((Element) xml.getChildren().get(1)).getChildText("id"));
    }
}
