/*
    Copyright (C) 2003 EBI, GRL

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package org.ensembl.mart.lib.config.test;

import java.util.List;

import junit.framework.TestCase;

import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.DSAttributeGroup;
import org.ensembl.mart.lib.config.DSFilterGroup;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.DatasetViewXMLUtils;
import org.ensembl.mart.lib.config.DefaultFilter;
import org.ensembl.mart.lib.config.Disable;
import org.ensembl.mart.lib.config.Enable;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.PushAction;
import org.jdom.Document;

/**
 * Tests for a DatasetView.dtd compliant testDatasetView.xml.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatasetViewXMLUtilsTest extends TestCase {

	public static final String TESTDATASETVIEWFILE = "data/XML/testDatasetView.xml";

  private static final String TESTDESC = "For Testing Purposes Only";
  private static final String TESTHANDLER = "testHandler";
  private static final String TESTTYPE = "list";
  private static final String TESTQUALIFIER = "in";
  private static final String TESTLEGALQUALIFIERS = "in,=";
  private static final String REFINAME = "testFilterDescription";
  
	/**
	 * Returns an instance of the testDatasetView.xml based XML object
	 * @param validate -- if true, XML is validated against DatasetView.dtd in the Classpath
	 * @return DatasetView
	 * @throws Exception 
	 */
	public static DatasetView TestDatasetViewInstance(boolean validate) throws Exception {
		return DatasetViewXMLUtils.XMLStreamToDatasetView(DatasetViewXMLUtilsTest.class.getClassLoader().getResourceAsStream(TESTDATASETVIEWFILE), validate);
	}

	public DatasetViewXMLUtilsTest(String arg0) {
		super(arg0);
	}

	public void testDatasetView() throws Exception {
    DatasetView dsv = TestDatasetViewInstance(true);
		validateDatasetView(dsv);
    validateDatasetViewSynchronization(dsv);
	}

  public static void validateDatasetViewSynchronization(DatasetView rDSV) throws Exception {
    Document rDoc = DatasetViewXMLUtils.DatasetViewToDocument(rDSV);
    byte[] rDigest = DatasetViewXMLUtils.DocumentToMessageDigest(rDoc);
    
    DatasetView nDSV = DatasetViewXMLUtils.DocumentToDatasetView(rDoc);
    
    assertTrue("reference DatasetView does not equal DatasetView after synchronization\n", rDSV.equals(nDSV));
    
    byte[] nDigest = DatasetViewXMLUtils.DatasetViewToMessageDigest(nDSV);
    
    assertTrue("Message Digests do not equal for same DatasetView object after synchronization\n", java.security.MessageDigest.isEqual(rDigest, nDigest));    
  }
  
	public static void validateDatasetView(DatasetView d) throws Exception {
		String testIName = "test_dataset";
		String IName = d.getInternalName();
		String testDName = "Test of a DatasetView";
		String DName = d.getDisplayName();
		String Desc = d.getDescription();
    String testDatasetPrefix = "test";
    String DatasetPrefix = d.getDataset();

		assertEquals("Internal Name not correctly set for DatasetView\n", testIName, IName);
		assertEquals("Display Name not correctly set for DatasetView\n", testDName, DName);
		assertEquals("Description not correctly set for DatasetView\n", TESTDESC, Desc);
    assertEquals("DatasetPrefix not correctly set for DatasetView\n", testDatasetPrefix, DatasetPrefix);

		String[] sbs = d.getStarBases();
		assertEquals("should only get one starbase\n", 1, sbs.length);
		assertEquals("didnt get the expected starbase\n", "test_starbase", sbs[0]);

		String[] pks = d.getPrimaryKeys();
		assertEquals("should only get one primary key\n", 1, pks.length);
		assertEquals("didnt get the expected primary key\n", "test_primaryKey", pks[0]);

		//Option data correct
		assertTrue("DatasetView should have Options.\n", d.hasOptions());
		Option[] ops = d.getOptions();
		assertEquals("DatasetView should have 1 Option.\n", 1, ops.length);

		datasetOptionTest(ops[0]);

		//defaultFilter data correct
		assertTrue("DatasetView should have DefaultFilters\n", d.hasDefaultFilters());
		DefaultFilter[] dfs = d.getDefaultFilters();
		assertEquals("DatasetView should have one Default Filter\n", 1, dfs.length);

		datasetDefaultFilterTest(dfs[0]);

		//FilterPage data correct
		FilterPage[] fps = d.getFilterPages();
		assertEquals("should only get one filter page\n", 1, fps.length);

		filterPageTest(d, fps[0]);

		// AttributePage data correct
		AttributePage[] aps = d.getAttributePages();
		assertEquals("should only get one filter page\n", 1, aps.length);

		attributePageTest(d, aps[0]);
	}

	private static void datasetOptionTest(Option option) throws Exception {
		// dataset option does not have suboption, isSelectable is true
		String testIName = "dataset option";
		String IName = option.getInternalName();
		String testDName = "A Test DatasetView Option";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();

		assertEquals("InternalName not correctly set for DatasetView Option\n", testIName, IName);
		assertEquals("DisplayName not correctly set for DatasetView Option\n", testDName, DName);
		assertEquals("Description not correctly set for DatasetView Option\n", TESTDESC, Desc);
		assertTrue("isSelectable should be true for DatasetView Option\n", option.isSelectable());
	}

	private static void datasetDefaultFilterTest(DefaultFilter df) throws Exception {
		String testValue = "1";
		String Value = df.getValue();
		FilterDescription testFDesc =
			new FilterDescription("testDefaultFilterDescription", "test_id", TESTTYPE, TESTQUALIFIER, TESTLEGALQUALIFIERS, "A TEST ID, DOESNT EXIST", "gene_main", null, TESTDESC);

		assertEquals("value not correctly set for DatasetView DefaultFilter\n", testValue, Value);
		assertEquals("FilterDescription not correct for DatasetView DefaultFilter\n", testFDesc, df.getUIFilterDescription());
	}

	private static void filterPageTest(DatasetView d, FilterPage fp) throws Exception {
		String testIName = "testFilterPage";
		String IName = fp.getInternalName();
		String testDName = "Test A Filter Page";
		String DName = fp.getDisplayName();
		String Desc = fp.getDescription();

		assertEquals("Internal Name not correctly set for FilterPage\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterPage\n", testDName, DName);
		assertEquals("Description not correctly set for FilterPage\n", TESTDESC, Desc);

		// contains/get for DatasetView-FilterPage
		boolean containsTest = d.containsFilterPage(testIName);
		assertTrue("DatasetView should contain testFilterPage, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = d.getFilterPageByName(testIName).getInternalName();
			assertEquals("getFilterPageByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterGroup data correct
		List fgs = fp.getFilterGroups();
		assertEquals("should get two filterGroups in FilterPage\n", 2, fgs.size());

		firstFilterGroupTest(fp, fgs.get(0));
		secondFilterGroupTest(d, fp, fgs.get(1));
	}

	private static void attributePageTest(DatasetView d, AttributePage ap) throws Exception {
		String testIName = "testAttributePage";
		String IName = ap.getInternalName();
		String testDName = "Test of an Attribute Page";
		String DName = ap.getDisplayName();
		String Desc = ap.getDescription();

		assertEquals("Internal Name not correctly set for AttributePage\n", testIName, IName);
		assertEquals("Display Name not correctly set for AttributePage\n", testDName, DName);
		assertEquals("Description not correctly set for AttributePage\n", TESTDESC, Desc);

		// contains/get for DatasetView-AttributePage
		boolean containsTest = d.containsAttributePage(testIName);
		assertTrue("DatasetView should contain testAttributePage, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = d.getAttributePageByInternalName(testIName).getInternalName();
			assertEquals("getAttributePageByName InternalName incorrect\n", testIName, testGetByName);
		}

		//AttributeGroup data correct
		List ags = ap.getAttributeGroups();
		assertEquals("should get two AttributeGroup\n", 2, ags.size());

		firstAttributeGroupTest(ap, ags.get(0));
		secondAttributeGroupTest(d, ap, ags.get(1));
	}

	private static void firstFilterGroupTest(FilterPage fp, Object group) throws Exception {
		// first filterGroup is a DSFilterGroup object
		assertTrue("First FilterGroup in the FilterPage should be a DSFilterGroup object", group instanceof DSFilterGroup);

		DSFilterGroup dsfg = (DSFilterGroup) group;
		String testIName = "testDSFilterGroup";
		String IName = dsfg.getInternalName();
		String testDName = "Test A DSFilterGroup:";
		String DName = dsfg.getDisplayName();
		String Desc = dsfg.getDescription();
		String Handler = dsfg.getHandler();

		assertEquals("Internal Name not correctly set for DSFilterGroup\n", testIName, IName);
		assertEquals("Display Name not correctly set for DSFilterGroup\n", testDName, DName);
		assertEquals("Description not correctly set for DSFilterGroup\n", TESTDESC, Desc);
		assertEquals("Handler not set correctly for DSFilterGroup\n", TESTHANDLER, Handler);

		// contains/get for FilterPage-FilterGroup
		boolean containsTest = fp.containsFilterGroup(testIName);
		assertTrue("FilterPage should contain testFilterGroup, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((DSFilterGroup) fp.getFilterGroupByName(testIName)).getInternalName();
			assertEquals("getFilterGroupByName InternalName incorrect\n", testIName, testGetByName);
		}
	}

	private static void firstAttributeGroupTest(AttributePage ap, Object group) {
		//first AttributeGroup in the AttributePage is a DSAttributeGroup
		assertTrue("First AttributeGroup in the AttributePage should be a DSAttributeGroup", group instanceof DSAttributeGroup);

		DSAttributeGroup dsag = (DSAttributeGroup) group;
		String testIName = "testDSAttributeGroup";
		String IName = dsag.getInternalName();
		String testDName = "test of an DSAttribute Group:";
		String DName = dsag.getDisplayName();
		String Desc = dsag.getDescription();
		String Handler = dsag.getHandler();

		assertEquals("Internal Name not correctly set for DSFilterGroup\n", testIName, IName);
		assertEquals("Display Name not correctly set for DSFilterGroup\n", testDName, DName);
		assertEquals("Description not correctly set for DSFilterGroup\n", TESTDESC, Desc);
		assertEquals("Handler not set correctly for DSFilterGroup\n", TESTHANDLER, Handler);
	}

	private static void secondFilterGroupTest(DatasetView d, FilterPage fp, Object group) throws Exception {
		// second FilterGroup is a FilterGroup object, with everything contained within it
		assertTrue("Second FilterGroup in the FilterPage should be a FilterGroup object", group instanceof FilterGroup);

		FilterGroup fg = (FilterGroup) group;
		String testIName = "testFilterGroup";
		String IName = fg.getInternalName();
		String testDName = "Test A FilterGroup:";
		String DName = fg.getDisplayName();
		String Desc = fg.getDescription();

		assertEquals("Internal Name not correctly set for FilterGroup\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterGroup\n", testDName, DName);
		assertEquals("Description not correctly set for FilterGroup\n", TESTDESC, Desc);

		// contains/get for FilterPage-FilterGroup
		boolean containsTest = fp.containsFilterGroup(testIName);
		assertTrue("FilterPage should contain testFilterGroup, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterGroup) fp.getFilterGroupByName(testIName)).getInternalName();
			assertEquals("getFilterGroupByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterCollection data correct
		FilterCollection[] fcs = fg.getFilterCollections();
		assertEquals("should get two filter collections\n", 2, fcs.length);

		firstFilterCollectionTest(d, fp, fg, fcs[0]);
		secondFilterCollectionTest(d, fp, fg, fcs[1]);
	}

	private static void secondAttributeGroupTest(DatasetView d, AttributePage ap, Object group) throws Exception {
		//second AttributeGroup in the AttributePage is an AttributeGroup, with everything in it
		assertTrue("Second AttributeGroup in the AttributePage should be an AttributeGroup", group instanceof AttributeGroup);

		AttributeGroup ag = (AttributeGroup) group;
		String testIName = "testAttributeGroup";
		String IName = ag.getInternalName();
		String testDName = "test of an Attribute Group:";
		String DName = ag.getDisplayName();
		String Desc = ag.getDescription();

		assertEquals("Internal Name not correctly set for AttributeGroup\n", testIName, IName);
		assertEquals("Display Name not correctly set for AttributeGroup\n", testDName, DName);
		assertEquals("Description not correctly set for AttributeGroup\n", TESTDESC, Desc);

		// contains/get for AttributePage-AttributeGroup
		boolean containsTest = ap.containsAttributeGroup(testIName);
		assertTrue("AttributePage should contain testAttributeGroup, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((AttributeGroup) ap.getAttributeGroupByName(testIName)).getInternalName();
			assertEquals("getAttributeGroupByName InternalName incorrect\n", testIName, testGetByName);
		}

		//AttributeCollection data correct
		AttributeCollection[] acs = ag.getAttributeCollections();
		assertEquals("should only get one attribute collection\n", 1, acs.length);

		attributeCollectionTest(d, ap, ag, acs[0]);
	}

	private static void firstFilterCollectionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc) throws Exception {
		// first FilterCollection Does Not Contain Any Options
		String testIName = "testFilterCollection";
		String IName = fc.getInternalName();
		String testDName = "Test of a FilterCollection";
		String DName = fc.getDisplayName();
		String Desc = fc.getDescription();

		assertEquals("Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Description not correctly set for FilterCollection\n", TESTDESC, Desc);

		//  contains/get for FilterGroup-FilterCollection
		boolean containsTest = fg.containsFilterCollection(testIName);
		assertTrue("FilterGroup should contain testFilterCollection, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterDescription data correct
		List fs = fc.getFilterDescriptions();
		assertEquals("should get four filter descriptions with first FilterCollection\n", 4, fs.size());

		firstFColFirstFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(0));
		firstFColSecFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(1));
		firstFColThirdFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(2));
		firstFColFourthFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(3));
	}

	private static void secondFilterCollectionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc) throws Exception {
		//second FilterCollection is a member of the FilterSet
		String testIName = "testOptionsCollection";
		String IName = fc.getInternalName();
		String testDName = "A TEST OF Options";
		String DName = fc.getDisplayName();
		String Desc = fc.getDescription();

		assertEquals("Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Description not correctly set for FilterCollection\n", TESTDESC, Desc);

		//  contains/get for FilterGroup-FilterCollection
		boolean containsTest = fg.containsFilterCollection(testIName);
		assertTrue("FilterGroup should contain testOptionsCollection, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterDescription data correct
		List fs = fc.getFilterDescriptions();
		assertEquals("should get five filter descriptions\n", 5, fs.size());

		secondFColFirstFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(0));
		secondFColSecFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(1));
		secondFColThirdFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(2));
		secondFColFourthFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(3));
		secondFColFifthFdescTest(d, fp, fg, fc, (FilterDescription) fs.get(4));
	}

	private static void attributeCollectionTest(DatasetView d, AttributePage ap, AttributeGroup ag, AttributeCollection ac) throws Exception {
		String testIName = "testAttributeCollection";
		String IName = ac.getInternalName();
		String testDName = "Test of an AttributeCollection:";
		String DName = ac.getDisplayName();
		String Desc = ac.getDescription();
		int testMaxSelect = 1;
		int MaxSelect = ac.getMaxSelect();

		assertEquals("Internal Name not correctly set for AttributeCollection\n", testIName, IName);
		assertEquals("Display Name not correctly set for AttributeCollection\n", testDName, DName);
		assertEquals("Description not correctly set for AttributeCollection\n", TESTDESC, Desc);
		assertEquals("Max Select not correctly set for AttributeCollection\n", testMaxSelect, MaxSelect);

		//  contains/get for AttributeGroup-AttributeCollection
		boolean containsTest = ag.containsAttributeCollection(testIName);
		assertTrue("AttributeGroup should contain testAttributeCollection, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ag.getAttributeCollectionByName(testIName).getInternalName();
			assertEquals("getAttributeCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//AttributeDescription data correct
		List as = ac.getAttributeDescriptions();
		assertEquals("should get one attribute description\n", 1, as.size());

		attributeCollectionAdescTest(d, ap, ag, ac, (AttributeDescription) as.get(0));
	}

	private static void firstFColFirstFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) {
		String IName = f.getInternalName();
		String testDName = "A TEST ID, DOESNT EXIST";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "test_id";
		String Field = f.getField();
    String qualifier = f.getQualifier();
		String legal_qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", REFINAME, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
    assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for UIFitlerDescription\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for FilterDescription\n", testTableConstraint, TableConstraint);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(REFINAME);
		assertTrue("FilterCollection should contain testUIFilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(REFINAME)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", REFINAME, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(REFINAME);
		assertTrue("FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(REFINAME)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", REFINAME, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(REFINAME).getInternalName());
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + REFINAME + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + REFINAME + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + REFINAME + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + REFINAME + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + REFINAME + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
	}

	private static void firstFColSecFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		//second FilterDescription from First FilterCollection contains an Enables Object
		String testIName = "enableFilter";
		String IName = f.getInternalName();
		String testDName = "Filter With Enable";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "enable_test_id";
		String Field = f.getField();
    String qualifier = f.getQualifier();
		String legal_qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
    assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for UIFitlerDescription\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for FilterDescription\n", testTableConstraint, TableConstraint);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain testUIFilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);

		Enable[] e = f.getEnables();
		assertEquals("enableFilter should have one Enable Object\n", 1, e.length);
		EnableTest(e[0]);
	}

	private static void firstFColThirdFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		//second FilterDescription from First FilterCollection contains an Enables Object
		String testIName = "disableFilter";
		String IName = f.getInternalName();
		String testDName = "Filter With Disable";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "disable_test_id";
		String Field = f.getField();
    String qualifier = f.getQualifier();
		String legal_qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
    assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for UIFitlerDescription\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for FilterDescription\n", testTableConstraint, TableConstraint);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain testUIFilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);

		Disable[] disables = f.getDisables();
		assertEquals("disableFilter should have one Disable Object\n", 1, disables.length);
		DisableTest(disables[0]);
	}

	private static void firstFColFourthFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "testHandlerFilterDescription";
		String IName = f.getInternalName();
		String testDName = "A TEST ID, DOESNT EXIST";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "handlerField";
		String Field = f.getField();
    String qualifier = f.getQualifier();
		String legal_qualifiers = f.getLegalQualifiers();
		String handler = f.getHandler();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
    assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for UIFitlerDescription\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("Handler not set correctly for FilterDescription\n", TESTHANDLER, handler);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain testHandlerFilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		String TableConstraint = f.getTableConstraint();
		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
	}

	private static void secondFColFirstFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "filterDescriptionValueOption";
		String IName = f.getInternalName();
		String testDName = "A TEST Value Option";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "value_option_id";
		String Field = f.getField();
    String qualifier = f.getQualifier();
		String legal_qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
    assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, qualifier);
    assertEquals("Legal Qualifiers not set correctly for UIFitlerDescription\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for FilterDescription\n", testTableConstraint, TableConstraint);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain testOptionsFilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);

		//test valueOption
		Option[] o = f.getOptions();
		assertEquals("testOptionsFilter Should contain one Option\n", 1, o.length);
		ValueOptionTest(o[0]);
	}

	private static void secondFColSecFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "filterDescriptionTreeValueOption";
		String IName = f.getInternalName();
		String testDName = "A TEST Tree Value Option";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "test_id";
		String Field = f.getField();
    String qualifier = f.getQualifier();
		String legal_qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "tree_value_dm";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
    assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for UIFitlerDescription\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for FilterDescription\n", testTableConstraint, TableConstraint);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain filterDescriptionTreeValueOption, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);

		//test treeValueOption
		Option[] o = f.getOptions();
		assertEquals("testOptionsFilter Should contain one Option\n", 1, o.length);
		TreeValueOptionTest(o[0]);
	}

	private static void secondFColThirdFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "FilterDescriptionOptionFilters";
		String IName = f.getInternalName();
		String testDName = "A TEST Option Filters";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain filterDescriptionTreeValueOption, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		Option[] o = f.getOptions();
		assertEquals("FilterDescriptionOptionFilters should have two options\n", 2, o.length);

		OptionFilterOneTest(d, fp, fg, fc, f, o[0]);
		OptionFilterTwoTest(d, fp, fg, fc, f, o[1]);
	}

	private static void secondFColFourthFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) {
		String testIName = "FilterDescriptionOptionPushAction";
		String IName = f.getInternalName();
		String testDName = "A TEST OF OPTION WITH PUSHACTION";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain FilterDescriptionOptionPushAction, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain FilterDescriptionOptionPushAction, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		Option[] o = f.getOptions();
		assertEquals("FilterDescriptionOptionPushAction Should contain one Option\n", 1, o.length);
		pushActionOptionTest(d, fp, fg, fc, f, o[0]);
	}

	private static void secondFColFifthFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "testPushActionOptionFilter";
		String IName = f.getInternalName();
		String testDName = "A TEST OF A PUSHACTION FILTER OPTION";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain testOptionsPushActionOptionFilter, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testOptionsPushActionOptionFilter, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for testOptionsPushActionOptionFilter\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}

		Option[] o = f.getOptions();
		assertEquals("pushActionFilter Should contain one Option\n", 1, o.length);
		PushActionFilterOptionTest(d, fp, fg, fc, f, o[0]);
	}

	private static void EnableTest(Enable e) throws Exception {
		String testRef = "testFilterDescription";
		String Ref = e.getRef();
		String testValueCondition = "1";
		String ValueCondition = e.getValueCondition();

		assertEquals("Enable Ref incorrect\n", testRef, Ref);
		assertEquals("Enable ValueCondition incorrect\n", testValueCondition, ValueCondition);
	}

	private static void DisableTest(Disable d) throws Exception {
		String testRef = "testFilterDescription";
		String Ref = d.getRef();
		String testValueCondition = "1";
		String ValueCondition = d.getValueCondition();

		assertEquals("Disable Ref incorrect\n", testRef, Ref);
		assertEquals("Disable ValueCondition incorrect\n", testValueCondition, ValueCondition);
	}

	private static void ValueOptionTest(Option option) {
		String testIName = "valueOption";
		String IName = option.getInternalName();
		String testValue = "1";
		String Value = option.getValue();

		assertTrue("ValueOption should be Selectable\n", option.isSelectable());
		assertEquals("ValueOption internalName incorrect\n", testIName, IName);
		assertEquals("ValueOption value incorrect\n", testValue, Value);
	}

	private static void TreeValueOptionTest(Option option) {
		String testIName = "treeValueOption";
		String IName = option.getInternalName();

		assertTrue("TreeValueOption should not be Selectable\n", !option.isSelectable());
		assertEquals("TreeValueOption internalName incorrect\n", testIName, IName);

		Option[] options = option.getOptions();
		assertEquals("TreeValueOption should have one Option\n", 1, options.length);
		ValueOptionTest(options[0]);
	}

	private static void OptionFilterOneTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option) {
		String testIName = "filterOptionOne";
		String IName = option.getInternalName();
		String testDName = "A Test Option Filter";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String Type = option.getType();
		String testField = "test_id";
		String Field = option.getField();
    String qualifier = option.getQualifier();
		String legal_qualifiers = option.getLegalQualifiers();
		String testTableConstraint = "filterOne_dm";
		String TableConstraint = option.getTableConstraint();

		assertEquals("Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Description not correctly set for Option\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for Option\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for Option\n", testField, Field);
    assertEquals("Qualifier not set correctly for Option\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for Option\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for Option\n", testTableConstraint, TableConstraint);
		assertTrue("filterOptionOne should be Selectable\n", option.isSelectable());

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain filterOptionOne FilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain filterOptionOne FilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		boolean datasetSupports = d.supportsFilterDescription(Field, TableConstraint);
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", datasetSupports);
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
	}

	private static void OptionFilterTwoTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option) {
		String testIName = "filterOptionTwo";
		String IName = option.getInternalName();
		String testDName = "A Test Option Filter";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String Type = option.getType();
		String testField = "test_id";
		String Field = option.getField();
    String qualifier = option.getQualifier();
		String legal_qualifiers = option.getLegalQualifiers();
		String testTableConstraint = "filterTwo_dm";
		String TableConstraint = option.getTableConstraint();

		assertEquals("Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Description not correctly set for Option\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for Option\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for Option\n", testField, Field);
    assertEquals("Qualifier not set correctly for Option\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for Option\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for Option\n", testTableConstraint, TableConstraint);
		assertTrue("filterOptionTwo should be Selectable\n", option.isSelectable());

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain filterOptionTwo FilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain filterOptionTwo FilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
	}

	private static void pushActionOptionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option) {
		String testIName = "pushActionOption";
		String IName = option.getInternalName();
		String testDName = "A TEST OPTION WITH PUSHACTION";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String Type = option.getType();
		String testField = "pushActionOption_id";
		String Field = option.getField();
    String qualifier = option.getQualifier();
		String legal_qualifiers = option.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = option.getTableConstraint();

		assertEquals("Internal Name not correctly set for pushActionOption\n", testIName, IName);
		assertEquals("Display Name not correctly set for pushActionOption\n", testDName, DName);
		assertEquals("Description not correctly set for pushActionOption\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for pushActionOption\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for pushActionOption\n", testField, Field);
    assertEquals("Qualifier not set correctly for pushActionOption\n", TESTQUALIFIER, qualifier);
		assertEquals("Legal Qualifiers not set correctly for pushActionOption\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("TableConstraint not set correctly for pushActionOption\n", testTableConstraint, TableConstraint);
		assertTrue("pushActionOption should be Selectable\n", option.isSelectable());

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain pushActionOption FilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain pushActionOption FilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);

		PushAction[] pos = option.getPushActions();
		assertEquals("pushActionOption should have one PushAction\n", 1, pos.length);
		PushActionValueTest(d, option, pos[0]);
	}

	private static void PushActionValueTest(DatasetView d, Option superoption, PushAction p) {
		String testIName = "TestValuePushAction";
		String IName = p.getInternalName();
		String testDName = "A TEST PUSHACTION";
		String DName = p.getDisplayName();
		String Desc = p.getDescription();
		String testRef = "testFilterDescription";
		String Ref = p.getRef();

		assertEquals("PushAction internalName incorrect\n", testIName, IName);
		assertEquals("PushAction displayName incorrect\n", testDName, DName);
		assertEquals("PushAction Description incorrect\n", TESTDESC, Desc);
		assertEquals("PushAction Ref incorrect\n", testRef, Ref);

		String testINameGetByName = superoption.getInternalName() + "." + Ref;
		assertTrue("DatasetView should contain FilterDescription for " + testINameGetByName + "\n", d.containsFilterDescription(testINameGetByName));
		FilterDescription testFilter = d.getFilterDescriptionByInternalName(Ref);
		FilterDescription Filter = d.getFilterDescriptionByInternalName(testINameGetByName);

		assertEquals("DatasetView returned the wrong FilterDescription for " + testINameGetByName + "\n", testFilter, Filter);
		assertEquals("Did not get the correct Field for " + testINameGetByName + "\n", testFilter.getField(), Filter.getField(testINameGetByName));

		Option[] options = p.getOptions();
		assertEquals("PushActionValue should have one Option\n", 1, options.length);
		PushActionValueOptionTest(options[0]);
	}

	private static void PushActionValueOptionTest(Option option) {
		String testIName = "testPushActionOption";
		String IName = option.getInternalName();
		String testDName = "A TEST PUSHACTION OPTION";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String testValue = "1";
		String Value = option.getValue();

		assertTrue("testPushActionOption should be Selectable\n", option.isSelectable());
		assertEquals("testPushActionOption internalName incorrect\n", testIName, IName);
		assertEquals("testPushActionOption displayName incorrect\n", testDName, DName);
		assertEquals("testPushActionOption Description incorrect\n", TESTDESC, Desc);
		assertEquals("testPushActionOption Value incorrect\n", testValue, Value);
	}

	private static void PushActionFilterOptionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option)
		throws Exception {
		String testIName = "PushActionFilterOption";
		String IName = option.getInternalName();
		String testDName = "A TEST OPTION WITH PUSHACTION FILTER OPTION";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();

		assertTrue("PushActionFilterOption should be Selectable\n", option.isSelectable());
		assertEquals("PushActionFilterOption internalName incorrect\n", testIName, IName);
		assertEquals("PushActionFilterOption displayName incorrect\n", testDName, DName);
		assertEquals("PushActionFilterOption Description incorrect\n", TESTDESC, Desc);

		PushAction[] pos = option.getPushActions();
		assertEquals("PushActionFilterOption should have one PushAction\n", 1, pos.length);
		PushActionFilterOptionPushActionTest(d, fp, fg, fc, f, option, pos[0]);
	}

	private static void PushActionFilterOptionPushActionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f ,Option o, PushAction p) throws Exception {
		String testIName = "OptionFilterPushAction";
		String IName = p.getInternalName();
		String testDName = "A TEST PUSHACTION WITH OPTION FILTER";
		String DName = p.getDisplayName();
		String Desc = p.getDescription();
		String testRef = "testFilterDescription";
		String Ref = p.getRef();

		assertEquals("OptionFilterPushAction internalName incorrect\n", testIName, IName);
		assertEquals("OptionFilterPushAction displayName incorrect\n", testDName, DName);
		assertEquals("OptionFilterPushAction Description incorrect\n", TESTDESC, Desc);
		assertEquals("OptionFilterPushAction Ref incorrect\n", testRef, Ref);

		Option[] options = p.getOptions();
		assertEquals("OptionFilterPushAction should have one Option\n", 1, options.length);
		OptionFilterPushActionOptionTest(d, fp, fg, fc, f, o, options[0]);
	}

	private static void OptionFilterPushActionOptionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option superoption, Option o) throws Exception {
		String testIName = "PushActionFilterOption";
		String IName = o.getInternalName();
		String testDName = "A TEST FILTER OPTION IN A PUSHACTION";
		String DName = o.getDisplayName();
		String Desc = o.getDescription();
		String Type = o.getType();
		String testField = "pushActionFilterOption_id";
		String Field = o.getField();
    String qualifier = o.getQualifier();
		String legal_qualifiers = o.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = o.getTableConstraint();

		assertTrue("PushActionFilterOption should be selectable\n", o.isSelectable());
		assertEquals("PushActionFilterOption internalName incorrect\n", testIName, IName);
		assertEquals("PushActionFilterOption displayName incorrect\n", testDName, DName);
		assertEquals("PushActionFilterOption description incorrect\n", TESTDESC, Desc);
		assertEquals("PushActionFilterOption type incorrect\n", TESTTYPE, Type);
		assertEquals("PushActionFilterOption field incorrect\n", testField, Field);
    assertEquals("PushActionFilterOption qualifier incorrect\n", TESTQUALIFIER, qualifier);
		assertEquals("PushActionFilterOption legal qualifiers incorrect\n", TESTLEGALQUALIFIERS, legal_qualifiers);
		assertEquals("PushActionFilterOption tableConstraint incorrect\n", testTableConstraint, TableConstraint);

		//  contains/get for FilterCollection-OptionFilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain PushActionFilterOption FilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain PushActionFilterOption FilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		boolean datasetSupports = d.supportsFilterDescription(Field, TableConstraint);
		assertTrue("DatasetView should support field and tableConstraint for " + IName + "\n", datasetSupports);
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterPage should support field and tableConstraint for " + IName + "\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterGroup should support field and tableConstraint for " + IName + "\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterCollection should support field and tableConstraint for " + IName + "\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);

		assertTrue("FilterDescripton should support field and tableConstraint for " + IName + "\n", f.supports(Field, TableConstraint));

		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);

		String testINameGetByName = superoption.getInternalName() + "." + IName;

		assertTrue("DatasetView should contain FilterDescription for " + testINameGetByName + "\n", d.containsFilterDescription(testINameGetByName));
		FilterDescription Filter = d.getFilterDescriptionByInternalName(testINameGetByName);

		assertEquals("DatasetView returned the wrong FilterDescription for " + testINameGetByName + "\n", f, Filter);
		assertEquals("Did not get the correct Field for " + testINameGetByName + "\n", f.getField(testINameGetByName), Filter.getField(testINameGetByName));

		String FieldByIName = f.getField(testINameGetByName);
		String TableConstraintByIName = f.getTableConstraint(testINameGetByName);
		String QualifiersByIName = f.getLegalQualifiers(testINameGetByName);
		String TypeByIName = f.getType(testINameGetByName);

		assertEquals("PushActionFilterOption getField By InternalName incorrect\n", testField, FieldByIName);
		assertEquals("PushActionFilterOption getTable By InternalName incorrect\n", testTableConstraint, TableConstraintByIName);
		assertEquals("PushActionFilterOption getQualifiers By InternalName incorrect\n", TESTLEGALQUALIFIERS, QualifiersByIName);
		assertEquals("PushActionFilterOption getType By InternalName incorrect\n", TESTTYPE, TypeByIName);
	}

	private static void attributeCollectionAdescTest(DatasetView d, AttributePage ap, AttributeGroup ag, AttributeCollection ac, AttributeDescription a)
		throws Exception {
		String testIName = "testAttributeDescription";
		String IName = a.getInternalName();
		String testDName = "Test of a AttributeDescription";
		String DName = a.getDisplayName();
		String Desc = a.getDescription();
		String testField = "test_id";
		String Field = a.getField();
		String testTableConstraint = "gene_main";
		String TableConstraint = a.getTableConstraint();
		int testMaxLength = 1;
		int MaxLength = a.getMaxLength();
		String testSource = "test source";
		String Source = a.getSource();
		String testHPage = "http://test.org";
		String HPage = a.getHomePageURL();
		String testLPage = "http://test.org?test";
		String LPage = a.getLinkoutURL();

		assertEquals("Internal Name not correctly set for AttributeDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for AttributeDescription\n", testDName, DName);
		assertEquals("Description not correctly set for AttributeDescription\n", TESTDESC, Desc);
		assertEquals("FieldName not correctly set for AttributeDescription\n", testField, Field);
		assertEquals("TableConstraint not correctly set for AttributeDescription\n", testTableConstraint, TableConstraint);
		assertEquals("MaxLength not correctly set for AttributeDescription\n", testMaxLength, MaxLength);
		assertEquals("Source not correctly set for AttributeDescription\n", testSource, Source);
		assertEquals("HomepageURL not correctly set for AttributeDescription\n", testHPage, HPage);
		assertEquals("LinkoutURL not correctly set for AttributeDescription\n", testLPage, LPage);

		//  contains/get for AttributeCollection-AttributeDescription
		boolean containsTest = ac.containsAttributeDescription(testIName);
		assertTrue("AttributeCollection should contain testUIAttributeDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((AttributeDescription) ac.getAttributeDescriptionByInternalName(testIName)).getInternalName();
			assertEquals("getUIAttributeDescriptionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//  contains/get for AttributePage-AttributeDescription (Tests all lower groups getByName as well
		containsTest = ap.containsAttributeDescription(testIName);
		assertTrue("AttributePage should contain testUIAttributeDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((AttributeDescription) ap.getAttributeDescriptionByInternalName(testIName)).getInternalName();
			assertEquals("getUIAttributeDescriptionByName InternalName incorrect\n", testIName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the AttributeDescription\n", "testAttributePage", d.getPageForAttribute(testIName).getInternalName());
		}

		assertTrue("testAttributeDescription should be supported by AttributePage\n", d.supportsAttributeDescription(Field, TableConstraint));
		assertEquals(
			"AttributePage should return testAttributeDescription for Field TableConstraint\n",
			a,
			d.getAttributeDescriptionByFieldNameTableConstraint(Field, TableConstraint));
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(DatasetViewXMLUtilsTest.class);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}
}
