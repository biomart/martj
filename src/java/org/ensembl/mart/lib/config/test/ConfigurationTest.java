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

//TODO: test qualifier
package org.ensembl.mart.lib.config.test;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.DSAttributeGroup;
import org.ensembl.mart.lib.config.DSFilterGroup;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.DefaultFilter;
import org.ensembl.mart.lib.config.Disable;
import org.ensembl.mart.lib.config.Enable;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;
import org.ensembl.mart.lib.config.MartDTDEntityResolver;
import org.ensembl.mart.lib.config.MartXMLutils;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.PushAction;
import org.ensembl.mart.lib.test.Base;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * Tests for the Mart Configuration System.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class ConfigurationTest extends Base {

	public static void main(String[] args) {
		if (args.length > 0)
			TestRunner.run(TestClass(args[0]));
		else
			TestRunner.run(suite());
	}

	public static Test suite() {
		return new TestSuite(ConfigurationTest.class);
	}

	public static Test TestClass(String testclass) {
		TestSuite suite = new TestSuite();
		suite.addTest(new ConfigurationTest(testclass));
		return suite;
	}

	public ConfigurationTest(String name) {
		super(name);
	}

	/**
	 * Tests the ability to retrieve an XML configuration file from the Mart Database,
	 * parse it with JDOM using validation (implicitly tests getting the DTD from the Mart Database as well),
	 * store it back into the Database, retrieve it again, and compare to the original.
	 * 
	 * @throws Exception
	 */
	public void testXMLRoundTrip() throws Exception {

		Connection conn = getDBConnection();

		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(true); // validate against the DTD
		builder.setEntityResolver(new MartDTDEntityResolver(conn)); // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.

		Document doc = builder.build(MartXMLutils.getInputSourceFor(conn, XMLTESTID));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLOutputter xout = new XMLOutputter();
		xout.output(doc, out);

		String initxml = out.toString();
		out.close();

		MartXMLutils.storeConfiguration(conn, XMLTESTID, doc, false);

		SAXBuilder newbuilder = new SAXBuilder();
		newbuilder.setValidation(true);
		newbuilder.setEntityResolver(new MartDTDEntityResolver(conn));
		// set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.

		Document newdoc = newbuilder.build(MartXMLutils.getInputSourceFor(conn, XMLTESTID));

		out = new ByteArrayOutputStream();
		xout.output(newdoc, out);

		String newxml = out.toString();
		out.close();

		assertEquals("initial xml does not match final xml after a roundtrip.\n", initxml, newxml);
	}

	public void testMartConfiguration() throws Exception {
    Connection conn = martJDataSource.getConnection();
		MartConfiguration martconf = new MartConfigurationFactory().getInstance(conn, XMLTESTID);
    DatabaseUtil.close( conn );

		//Mart Data Correct
		String testIName = "ensembl_mart_14_1";
		String IName = martconf.getInternalName();
		String testDName = "Ensembl Mart version 14.1";
		String DName = martconf.getDisplayName();
		String Desc = martconf.getDescription();

		assertEquals("MartName not correctly set for MartConfiguration\n", testIName, IName);
		assertEquals("Warning Mart Display Name not correctly set for MartConfiguration\n", testDName, DName);
		assertEquals("Warning Mart Description not correctly set for MartConfiguration\n", TESTDESC, Desc);

    //layout test
    layoutTest( martconf.getLayout() );
    
		// DatasetView Data Correct
		DatasetView[] ds = martconf.getDatasets();
		assertEquals("should only be one dataset, got " + ds.length + "\n", 1, ds.length);
		datasetTest(martconf, ds[0]);
	}

  private void layoutTest( FilterDescription d ) throws Exception {
  	String testIName = "testConfFilterDescription";
  	String IName = d.getInternalName();
  	String testDName = "A Test MartConfiguration FilterDescription";
  	String DName = d.getDisplayName();
  	String Desc = d.getDescription();
		String Type = d.getType();
    
		assertEquals("Internal Name not correctly set for Layout\n", testIName, IName);
		assertEquals("Display Name not correctly set for Layout\n", testDName, DName);
		assertEquals("Description not correctly set for Layout\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for Layout\n", TESTTYPE, Type);
		assertTrue("Layout should have options\n", d.hasOptions());
		
		Option[] os = d.getOptions();
		assertEquals("Layout should have one option, has" + os.length + "\n", 1, os.length);
		layoutOptionTest(os[0]);		
  }
  
  private void layoutOptionTest(Option o) throws  Exception {
  	String testIName = "testOption";
  	String IName = o.getInternalName();
  	String testDName = "A Test Option";
  	String DName = o.getDisplayName();
  	String Desc = o.getDescription();
  	
		assertEquals("InternalName not correctly set for Layout Option\n", testIName, IName);
		assertEquals("DisplayName not correctly set for Layout Option\n", testDName, DName);
		assertEquals("Description not correctly set for Layout Option\n", TESTDESC, Desc);
		assertTrue("isSelectable should be true for Layout Option\n", o.isSelectable());
  }
  
	private void datasetTest(MartConfiguration martconf, DatasetView d) throws Exception {
		String testIName = "test_dataset";
		String IName = d.getInternalName();
		String testDName = "Test of a DatasetView";
		String DName = d.getDisplayName();
		String Desc = d.getDescription();

		assertEquals("Internal Name not correctly set for DatasetView\n", testIName, IName);
		assertEquals("Display Name not correctly set for DatasetView\n", testDName, DName);
		assertEquals("Description not correctly set for DatasetView\n", TESTDESC, Desc);

		//contains/get for MartConfiguration-DatasetView
		boolean containsTest = martconf.containsDataset(testIName);
		String testGetByName = null;

		assertTrue("MartConfiguration should contain " + testIName + ", but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = martconf.getDatasetByName(testIName).getInternalName();
			assertEquals("getDatasetByName InternalName incorrect\n", testIName, testGetByName);
		}

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
  
  private void datasetOptionTest(Option option) throws Exception {
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
  
  private void datasetDefaultFilterTest(DefaultFilter df) throws Exception {
    String testValue = "1";
    String Value = df.getValue();
    FilterDescription testFDesc = new FilterDescription("testDefaultFilterDescription", "test_id", TESTTYPE, "", TESTQUALIFIERS,  "A TEST ID, DOESNT EXIST", "gene_main", null, TESTDESC);
                                                            
    assertEquals("value not correctly set for DatasetView DefaultFilter\n", testValue, Value);
    assertEquals("FilterDescription not correct for DatasetView DefaultFilter\n", testFDesc, df.getUIFilterDescription());
  }
  
	private void filterPageTest(DatasetView d, FilterPage fp) throws Exception {
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

	private void attributePageTest(DatasetView d, AttributePage ap) throws Exception {
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

	private void firstFilterGroupTest(FilterPage fp, Object group) throws Exception {
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

	private void firstAttributeGroupTest(AttributePage ap, Object group) {
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

	private void secondFilterGroupTest(DatasetView d, FilterPage fp, Object group) throws Exception {
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

	private void secondAttributeGroupTest(DatasetView d, AttributePage ap, Object group) throws Exception {
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

	private void firstFilterCollectionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc) throws Exception {
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
		firstFColFourthFdescTest(d,fp,fg,fc,(FilterDescription) fs.get(3));
	}

	private void secondFilterCollectionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc) throws Exception {
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

	private void attributeCollectionTest(DatasetView d, AttributePage ap, AttributeGroup ag, AttributeCollection ac) throws Exception {
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
	
	private void firstFColFirstFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) {
		String IName = f.getInternalName();
		String testDName = "A TEST ID, DOESNT EXIST";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "test_id";
		String Field = f.getField();
		String qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", REFINAME, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
		assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+REFINAME+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+REFINAME+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+REFINAME+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+REFINAME+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+REFINAME+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
	}

	private void firstFColSecFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
    //second FilterDescription from First FilterCollection contains an Enables Object
    String testIName = "enableFilter";
		String IName = f.getInternalName();
		String testDName = "Filter With Enable";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "enable_test_id";
		String Field = f.getField();
		String qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
		assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
		
		Enable[] e = f.getEnables();
		assertEquals("enableFilter should have one Enable Object\n", 1, e.length);
		EnableTest(e[0]);
	}

	private void firstFColThirdFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		//second FilterDescription from First FilterCollection contains an Enables Object
		String testIName = "disableFilter";
		String IName = f.getInternalName();
		String testDName = "Filter With Disable";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "disable_test_id";
		String Field = f.getField();
		String qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
		assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
		
		Disable[] disables = f.getDisables();
		assertEquals("disableFilter should have one Disable Object\n", 1, disables.length);
		DisableTest(disables[0]);
	}
	
	private void firstFColFourthFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "testHandlerFilterDescription";
		String IName = f.getInternalName();
		String testDName = "A TEST ID, DOESNT EXIST";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "handlerField";
		String Field = f.getField();
		String qualifiers = f.getLegalQualifiers();
		String handler = f.getHandler();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
		assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);		
	}
	
	private void secondFColFirstFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "filterDescriptionValueOption";
		String IName = f.getInternalName();
		String testDName = "A TEST Value Option";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "value_option_id";
		String Field = f.getField();
		String qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
		assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
		
		//test valueOption
		Option[] o = f.getOptions();
		assertEquals("testOptionsFilter Should contain one Option\n", 1, o.length);
		ValueOptionTest(o[0]);    
	}

	private void secondFColSecFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "filterDescriptionTreeValueOption";
		String IName = f.getInternalName();
		String testDName = "A TEST Tree Value Option";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testField = "test_id";
		String Field = f.getField();
		String qualifiers = f.getLegalQualifiers();
		String testTableConstraint = "tree_value_dm";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for FilterDescription\n", testField, Field);
		assertEquals("Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
		
		//test treeValueOption
		Option[] o = f.getOptions();
		assertEquals("testOptionsFilter Should contain one Option\n", 1, o.length);
		TreeValueOptionTest(o[0]);
	}

	private void secondFColThirdFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
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

	private void secondFColFourthFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) {
		String testIName = "FilterDescriptionOptionPushOptions";
		String IName = f.getInternalName();
		String testDName = "A TEST OF OPTION WITH PUSHOPTIONS";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();

		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain FilterDescriptionOptionPushOptions, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getUIFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain FilterDescriptionOptionPushOptions, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for the FilterDescription\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}
		
		Option[] o = f.getOptions();
		assertEquals("FilterDescriptionOptionPushOptions Should contain one Option\n", 1, o.length);
  	pushOptionOptionTest(d, fp, fg, fc, f, o[0]);
  }

	private void secondFColFifthFdescTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f) throws Exception {
		String testIName = "testPushOptionOptionFilter";
		String IName = f.getInternalName();
		String testDName = "A TEST OF A PUSHOPTION FILTER OPTION";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		
		assertEquals("Internal Name not correctly set for FilterDescription\n", testIName, IName);
		assertEquals("Display Name not correctly set for FilterDescription\n", testDName, DName);
		assertEquals("Description not correctly set for FilterDescription\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for FilterDescription\n", TESTTYPE, Type);

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain testOptionsPushOptionOptionFilter, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain testOptionsPushOptionOptionFilter, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByName InternalName incorrect\n", IName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Did not get the correct Page for testOptionsPushOptionOptionFilter\n", "testFilterPage", d.getPageForFilter(IName).getInternalName());
		}
		
		Option[] o = f.getOptions();
		assertEquals("pushOptionFilter Should contain one Option\n", 1, o.length);
		PushOptionFilterOptionTest(d, fp, fg, fc, f, o[0]);
	}

	private void EnableTest(Enable e) throws Exception {
  	String testRef = "testFilterDescription";
  	String Ref = e.getRef();
  	String testValueCondition = "1";
  	String ValueCondition = e.getValueCondition();
  	
  	assertEquals("Enable Ref incorrect\n", testRef, Ref);
  	assertEquals("Enable ValueCondition incorrect\n", testValueCondition, ValueCondition);
  }
  
	private void DisableTest(Disable d) throws Exception {
		String testRef = "testFilterDescription";
		String Ref = d.getRef();
		String testValueCondition = "1";
		String ValueCondition = d.getValueCondition();
  	
		assertEquals("Disable Ref incorrect\n", testRef, Ref);
		assertEquals("Disable ValueCondition incorrect\n", testValueCondition, ValueCondition);
	}
	
	private void ValueOptionTest(Option option) {
    String testIName = "valueOption";
    String IName = option.getInternalName();
    String testValue = "1";
    String Value = option.getValue();
    
    assertTrue("ValueOption should be Selectable\n", option.isSelectable());
    assertEquals("ValueOption internalName incorrect\n", testIName, IName);
    assertEquals("ValueOption value incorrect\n", testValue, Value);
	}
	
	private void TreeValueOptionTest(Option option) {
		String testIName = "treeValueOption";
		String IName = option.getInternalName();
		
		assertTrue("TreeValueOption should not be Selectable\n", !option.isSelectable());
		assertEquals("TreeValueOption internalName incorrect\n", testIName, IName);
		
		Option[] options = option.getOptions();
		assertEquals("TreeValueOption should have one Option\n", 1, options.length);
		ValueOptionTest(options[0]);		
	}
	
	 private void OptionFilterOneTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option) {
		String testIName = "filterOptionOne";
		String IName = option.getInternalName();
		String testDName = "A Test Option Filter";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String Type = option.getType();
		String testField = "test_id";
		String Field = option.getField();
		String qualifiers = option.getLegalQualifiers();
		String testTableConstraint = "filterOne_dm";
		String TableConstraint = option.getTableConstraint();

		assertEquals("Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Description not correctly set for Option\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for Option\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for Option\n", testField, Field);
		assertEquals("Qualifier not set correctly for Option\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", datasetSupports);
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
	 }

	private void OptionFilterTwoTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option) {
		String testIName = "filterOptionTwo";
		String IName = option.getInternalName();
		String testDName = "A Test Option Filter";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String Type = option.getType();
		String testField = "test_id";
		String Field = option.getField();
		String qualifiers = option.getLegalQualifiers();
		String testTableConstraint = "filterTwo_dm";
		String TableConstraint = option.getTableConstraint();

		assertEquals("Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Description not correctly set for Option\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for Option\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for Option\n", testField, Field);
		assertEquals("Qualifier not set correctly for Option\n", TESTQUALIFIERS, qualifiers);
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
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);	 		
	 }
	 
	private void pushOptionOptionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option) {
		String testIName = "pushOptionOption";
		String IName = option.getInternalName();
		String testDName = "A TEST OPTION WITH PUSHOPTIONS";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String Type = option.getType();
		String testField = "pushOptionOption_id";
		String Field = option.getField();
		String qualifiers = option.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = option.getTableConstraint();

		assertEquals("Internal Name not correctly set for pushOptionOption\n", testIName, IName);
		assertEquals("Display Name not correctly set for pushOptionOption\n", testDName, DName);
		assertEquals("Description not correctly set for pushOptionOption\n", TESTDESC, Desc);
		assertEquals("Type not set correctly for pushOptionOption\n", TESTTYPE, Type);
		assertEquals("FieldName not set correctly for pushOptionOption\n", testField, Field);
		assertEquals("Qualifier not set correctly for pushOptionOption\n", TESTQUALIFIERS, qualifiers);
		assertEquals("TableConstraint not set correctly for pushOptionOption\n", testTableConstraint, TableConstraint);
		assertTrue("pushOptionOption should be Selectable\n", option.isSelectable());

		//  contains/get for FilterCollection-FilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain pushOptionOption FilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain pushOptionOption FilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}
		
		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", d.supportsFilterDescription(Field, TableConstraint));
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
		
		PushAction[] pos = option.getPushActions();
		assertEquals("pushOptionOption should have one PushOption\n", 1, pos.length);
		PushOptionValueTest(d, option, pos[0]);	 		
	}
	
	private void PushOptionValueTest(DatasetView d, Option superoption, PushAction p) {
		String testIName = "TestValuePushOptions";
		String IName = p.getInternalName();
		String testDName = "A TEST PUSHOPTIONS";
		String DName = p.getDisplayName();
		String Desc = p.getDescription();
		String testRef = "testFilterDescription";
		String Ref = p.getRef();
		
		assertEquals("PushOption internalName incorrect\n", testIName, IName);
		assertEquals("PushOption displayName incorrect\n", testDName, DName);
		assertEquals("PushOption Description incorrect\n", TESTDESC, Desc);
		assertEquals("PushOption Ref incorrect\n", testRef, Ref);
		
		String testINameGetByName = superoption.getInternalName()+"."+Ref;
		assertTrue("DatasetView should contain FilterDescription for " + testINameGetByName + "\n", d.containsFilterDescription(testINameGetByName));
		FilterDescription testFilter = d.getFilterDescriptionByInternalName(Ref);
		FilterDescription Filter = d.getFilterDescriptionByInternalName(testINameGetByName);
		
		assertEquals("DatasetView returned the wrong FilterDescription for " + testINameGetByName + "\n", testFilter, Filter);
		assertEquals("Did not get the correct Field for " + testINameGetByName + "\n", testFilter.getField(), Filter.getField(testINameGetByName));
		
		Option[] options = p.getOptions();
		assertEquals("PushOptionValue should have one Option\n", 1, options.length);
		PushOptionValueOptionTest(options[0]);
	}
	
	private void PushOptionValueOptionTest(Option option) {
		String testIName = "testPushOptionOption";
		String IName = option.getInternalName();
		String testDName = "A TEST PUSHOPTIONS OPTION";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		String testValue = "1";
		String Value = option.getValue();
		
		assertTrue("testPushOptionOption should be Selectable\n", option.isSelectable());
		assertEquals("testPushOptionOption internalName incorrect\n", testIName, IName);
		assertEquals("testPushOptionOption displayName incorrect\n", testDName, DName);
		assertEquals("testPushOptionOption Description incorrect\n", TESTDESC, Desc);
		assertEquals("testPushOptionOption Value incorrect\n", testValue, Value);
	}

	private void PushOptionFilterOptionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option option) throws Exception {
		String testIName = "PushOptionFilterOption";
		String IName = option.getInternalName();
		String testDName = "A TEST OPTION WITH PUSHOPTION FILTER OPTION";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
		
		assertTrue("PushOptionFilterOption should be Selectable\n", option.isSelectable());
		assertEquals("PushOptionFilterOption internalName incorrect\n", testIName, IName);
		assertEquals("PushOptionFilterOption displayName incorrect\n", testDName, DName);
		assertEquals("PushOptionFilterOption Description incorrect\n", TESTDESC, Desc);
		
		PushAction[] pos = option.getPushActions();
		assertEquals("PushOptionFilterOption should have one PushOption\n", 1, pos.length);
		PushOptionFilterOptionPushOptionTest(d, fp, fg, fc, f, option, pos[0]);
	}
	
	private void PushOptionFilterOptionPushOptionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option o, PushAction p) throws Exception {
		String testIName = "OptionFilterPushOption";
		String IName = p.getInternalName();
		String testDName = "A TEST PUSHOPTIONS WITH OPTION FILTER";
		String DName = p.getDisplayName();
		String Desc = p.getDescription();
		String testRef = "testFilterDescription";
		String Ref = p.getRef();
		
		assertEquals("OptionFilterPushOption internalName incorrect\n", testIName, IName);
		assertEquals("OptionFilterPushOption displayName incorrect\n", testDName, DName);
		assertEquals("OptionFilterPushOption Description incorrect\n", TESTDESC, Desc);
		assertEquals("OptionFilterPushOption Ref incorrect\n", testRef, Ref);
		
		Option[] options = p.getOptions();
		assertEquals("OptionFilterPushOption should have one Option\n", 1, options.length);
		OptionFilterPushOptionOptionTest(d,fp,fg,fc,f,o,options[0]);
	}
	
	private void OptionFilterPushOptionOptionTest(DatasetView d, FilterPage fp, FilterGroup fg, FilterCollection fc, FilterDescription f, Option superoption, Option o) throws Exception {
		String testIName = "PushOptionFilterOption";
		String IName = o.getInternalName();
		String testDName = "A TEST FILTER OPTION IN A PUSHOPTION";
		String DName = o.getDisplayName();
		String Desc = o.getDescription();
		String Type = o.getType();
		String testField = "pushOptionFilterOption_id";
		String Field = o.getField();
		String qualifiers = o.getLegalQualifiers();
		String testTableConstraint = "gene_main";
		String TableConstraint = o.getTableConstraint();
		
		assertTrue("PushOptionFilterOption should be selectable\n", o.isSelectable());
		assertEquals("PushOptionFilterOption internalName incorrect\n", testIName, IName);
		assertEquals("PushOptionFilterOption displayName incorrect\n", testDName, DName);
		assertEquals("PushOptionFilterOption description incorrect\n", TESTDESC, Desc);
		assertEquals("PushOptionFilterOption type incorrect\n", TESTTYPE, Type);
		assertEquals("PushOptionFilterOption field incorrect\n", testField, Field);
		assertEquals("PushOptionFilterOption qualifiers incorrect\n", TESTQUALIFIERS, qualifiers);
		assertEquals("PushOptionFilterOption tableConstraint incorrect\n", testTableConstraint, TableConstraint);
		
		//  contains/get for FilterCollection-OptionFilterDescription
		boolean containsTest = fc.containsFilterDescription(IName);
		assertTrue("FilterCollection should contain PushOptionFilterOption FilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterDescription) fc.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}

		//  contains/get for FilterPage-FilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsFilterDescription(IName);
		assertTrue("FilterPage should contain PushOptionFilterOption FilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((FilterDescription) fp.getFilterDescriptionByInternalName(IName)).getInternalName();
			assertEquals("getFilterDescriptionByInternalName InternalName incorrect\n", f.getInternalName(), testGetByName);
		}
		
		//test supports, getFilterDescriptionByFieldNameTableConstraint functionality
		boolean datasetSupports = d.supportsFilterDescription(Field, TableConstraint);
		assertTrue("DatasetView should support field and tableConstraint for "+IName+"\n", datasetSupports);
		FilterDescription g = d.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterPage should support field and tableConstraint for "+IName+"\n", fp.supports(Field, TableConstraint));
		FilterDescription h = fp.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterGroup should support field and tableConstraint for "+IName+"\n", fg.supports(Field, TableConstraint));
		FilterDescription i = fg.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterCollection should support field and tableConstraint for "+IName+"\n", fc.supports(Field, TableConstraint));
		FilterDescription j = fc.getFilterDescriptionByFieldNameTableConstraint(Field, TableConstraint);
		
		assertTrue("FilterDescripton should support field and tableConstraint for "+IName+"\n", f.supports(Field, TableConstraint));
		
		assertEquals("DatasetView returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, g);
		assertEquals("FilterPage returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, h);
		assertEquals("FilterGroup returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, i);
		assertEquals("FilterCollection returned wrong supporting FilterDescription for FieldName TableConstraint\n", f, j);
		
		String testINameGetByName = superoption.getInternalName()+"."+IName;
		
		assertTrue("DatasetView should contain FilterDescription for " + testINameGetByName + "\n", d.containsFilterDescription(testINameGetByName));
		FilterDescription Filter = d.getFilterDescriptionByInternalName(testINameGetByName);
		
		assertEquals("DatasetView returned the wrong FilterDescription for " + testINameGetByName + "\n", f, Filter);
		assertEquals("Did not get the correct Field for " + testINameGetByName + "\n", f.getField(testINameGetByName), Filter.getField(testINameGetByName));
		
		String FieldByIName = f.getField(testINameGetByName);
		String TableConstraintByIName = f.getTableConstraint(testINameGetByName);
		String QualifiersByIName = f.getLegalQualifiers(testINameGetByName);
		String TypeByIName = f.getType(testINameGetByName);
		
		assertEquals("PushOptionFilterOption getField By InternalName incorrect\n", testField, FieldByIName);
		assertEquals("PushOptionFilterOption getTable By InternalName incorrect\n", testTableConstraint, TableConstraintByIName);
		assertEquals("PushOptionFilterOption getQualifiers By InternalName incorrect\n", TESTQUALIFIERS, QualifiersByIName);
		assertEquals("PushOptionFilterOption getType By InternalName incorrect\n", TESTTYPE, TypeByIName);
	}
	
	private void attributeCollectionAdescTest(DatasetView d, AttributePage ap, AttributeGroup ag, AttributeCollection ac, AttributeDescription a) throws Exception {
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
			assertEquals(
				"Did not get the correct Page for the AttributeDescription\n",
				"testAttributePage",
				d.getPageForAttribute(testIName).getInternalName());
		}
		
		assertTrue("testAttributeDescription should be supported by AttributePage\n", d.supportsAttributeDescription(Field, TableConstraint));
		assertEquals("AttributePage should return testAttributeDescription for Field TableConstraint\n", a, d.getAttributeDescriptionByFieldNameTableConstraint(Field, TableConstraint));
	}

	public void testConfFile() throws Exception {
		String confFile = "data/xmltest/test_file.xml";
		URL confURL = ClassLoader.getSystemResource(confFile);
		MartConfiguration martconf = new MartConfigurationFactory().getInstance(confURL);

		String testMartName = "test_file";
		String martName = martconf.getInternalName();
		assertEquals("martName from file " + confURL.toString() + " isnt correct", testMartName, martName);
	}

	private final String XMLTESTID = "test.xml";
	private final String TESTDESC = "For Testing Purposes Only";
	private final String TESTHANDLER = "testHandler";
	private final String TESTTYPE = "list";
	private final String TESTQUALIFIERS = "in,=";
	private final	String REFINAME = "testFilterDescription";
}
