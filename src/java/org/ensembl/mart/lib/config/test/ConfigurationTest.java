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

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.DSAttributeGroup;
import org.ensembl.mart.lib.config.DSFilterGroup;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSet;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;
import org.ensembl.mart.lib.config.MartDTDEntityResolver;
import org.ensembl.mart.lib.config.MartXMLutils;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.UIAttributeDescription;
import org.ensembl.mart.lib.config.UIDSFilterDescription;
import org.ensembl.mart.lib.config.UIFilterDescription;
import org.ensembl.mart.lib.config.DefaultFilter;
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

		MartXMLutils.storeConfiguration(conn, XMLTESTID, doc);

		SAXBuilder newbuilder = new SAXBuilder();
		newbuilder.setValidation(true);
		newbuilder.setEntityResolver(new MartDTDEntityResolver(conn));
		// set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.

		Document newdoc = newbuilder.build(MartXMLutils.getInputSourceFor(conn, XMLTESTID));

		out = new ByteArrayOutputStream();
		xout.output(newdoc, out);

		String newxml = out.toString();
		out.close();

		assertEquals("Warning, initial xml does not match final xml after a roundtrip.\n", initxml, newxml);
	}

	public void testMartConfiguration() throws Exception {
		MartConfiguration martconf = new MartConfigurationFactory().getInstance(engine.getConnection(), XMLTESTID);

		//Mart Data Correct
		String testIName = "ensembl_mart_14_1";
		String IName = martconf.getInternalName();
		String testDName = "Ensembl Mart version 14.1";
		String DName = martconf.getDisplayName();
		String Desc = martconf.getDescription();

		assertEquals("Warning, MartName not correctly set for MartConfiguration\n", testIName, IName);
		assertEquals("Warning Mart Display Name not correctly set for MartConfiguration\n", testDName, DName);
		assertEquals("Warning Mart Description not correctly set for MartConfiguration\n", TESTDESC, Desc);

		// Dataset Data Correct
		Dataset[] ds = martconf.getDatasets();
		assertEquals("Warning, should only be one dataset, got " + ds.length + "\n", 1, ds.length);
		datasetTest(martconf, ds[0]);
	}

	private void datasetTest(MartConfiguration martconf, Dataset d) throws Exception {
		String testIName = "test_dataset";
		String IName = d.getInternalName();
		String testDName = "Test of a Dataset";
		String DName = d.getDisplayName();
		String Desc = d.getDescription();

		assertEquals("Warning, Internal Name not correctly set for Dataset\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Dataset\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Dataset\n", TESTDESC, Desc);

		//contains/get for MartConfiguration-Dataset
		boolean containsTest = martconf.containsDataset(testIName);
		String testGetByName = null;

		assertTrue("Warning, MartConfiguration should contain " + testIName + ", but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = martconf.getDatasetByName(testIName).getInternalName();
			assertEquals("Warning, getDatasetByName InternalName incorrect\n", testIName, testGetByName);
		}

		String[] sbs = d.getStarBases();
		assertEquals("Warning, should only get one starbase\n", 1, sbs.length);
		assertEquals("Warning, didnt get the expected starbase\n", "test_starbase", sbs[0]);

		String[] pks = d.getPrimaryKeys();
		assertEquals("Warning, should only get one primary key\n", 1, pks.length);
		assertEquals("Warning, didnt get the expected primary key\n", "test_primaryKey", pks[0]);

    //Option data correct
    assertTrue("Warning, Dataset should have Options.\n", d.hasOptions());
    Option[] ops = d.getOptions();
    assertEquals("Warning, Dataset should have 1 Option.\n", 1, ops.length);
    
    datasetOptionTest(ops[0]);
  
  
    //defaultFilter data correct
    assertTrue("Warning, Dataset should have DefaultFilters\n", d.hasDefaultFilters());
    DefaultFilter[] dfs = d.getDefaultFilters();
    assertEquals("Warning, Dataset should have one Default Filter\n", 1, dfs.length);
    
    datasetDefaultFilterTest(dfs[0]);
      
		//FilterPage data correct
		FilterPage[] fps = d.getFilterPages();
		assertEquals("Warning, should only get one filter page\n", 1, fps.length);

		filterPageTest(d, fps[0]);

		// AttributePage data correct
		AttributePage[] aps = d.getAttributePages();
		assertEquals("Warning, should only get one filter page\n", 1, aps.length);

		attributePageTest(d, aps[0]);
	}
  
  private void datasetOptionTest(Option option) throws Exception {
    // dataset option does not have suboption, isSelectable is true
    String testIName = "dataset option";
    String IName = option.getInternalName();
    String testDName = "A Test Dataset Option";
    String DName = option.getDisplayName();
    String Desc = option.getDescription();
    
    assertEquals("Warning, InternalName not correctly set for Dataset Option\n", testIName, IName);
    assertEquals("Warning, DisplayName not correctly set for Dataset Option\n", testDName, DName);
    assertEquals("Warning, Description not correctly set for Dataset Option\n", TESTDESC, Desc);
    assertTrue("Warning, isSelectable should be true for Dataset Option\n", option.isSelectable());
  }
  
  private void datasetDefaultFilterTest(DefaultFilter df) throws Exception {
    String testValue = "1";
    String Value = df.getValue();
    UIFilterDescription testFDesc = new UIFilterDescription("testDefaultFilterDescription", 
                                                            "test_id", 
                                                            TESTTYPE, 
                                                            TESTQUALIFIER, 
                                                            "A TEST ID, DOESNT EXIST", 
                                                            "gene_main", 
                                                            "", 
                                                            TESTDESC, 
                                                            "");
                                                            
    assertEquals("Warning, value not correctly set for Dataset DefaultFilter\n", testValue, Value);
    assertEquals("Warning, UIFilterDescription not correct for Dataset DefaultFilter\n", testFDesc, df.getUIFilterDescription());
  }
  
	private void filterPageTest(Dataset d, FilterPage fp) throws Exception {
		String testIName = "testFilterPage";
		String IName = fp.getInternalName();
		String testDName = "Test A Filter Page";
		String DName = fp.getDisplayName();
		String Desc = fp.getDescription();

		assertEquals("Warning, Internal Name not correctly set for FilterPage\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterPage\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterPage\n", TESTDESC, Desc);

		// contains/get for Dataset-FilterPage
		boolean containsTest = d.containsFilterPage(testIName);
		assertTrue("Warning, Dataset should contain testFilterPage, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = d.getFilterPageByName(testIName).getInternalName();
			assertEquals("Warning, getFilterPageByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterGroup data correct
		List fgs = fp.getFilterGroups();
		assertEquals("Warning, should get two filterGroups in FilterPage\n", 2, fgs.size());

		firstFilterGroupTest(fp, fgs.get(0));
		secondFilterGroupTest(d, fp, fgs.get(1));
	}

	private void attributePageTest(Dataset d, AttributePage ap) throws Exception {
		String testIName = "testAttributePage";
		String IName = ap.getInternalName();
		String testDName = "Test of an Attribute Page";
		String DName = ap.getDisplayName();
		String Desc = ap.getDescription();

		assertEquals("Warning, Internal Name not correctly set for AttributePage\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributePage\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributePage\n", TESTDESC, Desc);

		// contains/get for Dataset-AttributePage
		boolean containsTest = d.containsAttributePage(testIName);
		assertTrue("Warning, Dataset should contain testAttributePage, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = d.getAttributePageByName(testIName).getInternalName();
			assertEquals("Warning, getAttributePageByName InternalName incorrect\n", testIName, testGetByName);
		}

		//AttributeGroup data correct
		List ags = ap.getAttributeGroups();
		assertEquals("Warning, should get two AttributeGroup\n", 2, ags.size());

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

		assertEquals("Warning, Internal Name not correctly set for DSFilterGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for DSFilterGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for DSFilterGroup\n", TESTDESC, Desc);
		assertEquals("Warning, Handler not set correctly for DSFilterGroup\n", TESTHANDLER, Handler);

		// contains/get for FilterPage-FilterGroup
		boolean containsTest = fp.containsFilterGroup(testIName);
		assertTrue("Warning, FilterPage should contain testFilterGroup, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((DSFilterGroup) fp.getFilterGroupByName(testIName)).getInternalName();
			assertEquals("Warning, getFilterGroupByName InternalName incorrect\n", testIName, testGetByName);
		}
	}

	private void firstAttributeGroupTest(AttributePage ap, Object group) {
		//first AttributeGroup in the AttributePage is a DSAttributeGroup
		assertTrue("Warning, First AttributeGroup in the AttributePage should be a DSAttributeGroup", group instanceof DSAttributeGroup);

		DSAttributeGroup dsag = (DSAttributeGroup) group;
		String testIName = "testDSAttributeGroup";
		String IName = dsag.getInternalName();
		String testDName = "test of an DSAttribute Group:";
		String DName = dsag.getDisplayName();
		String Desc = dsag.getDescription();
		String Handler = dsag.getHandler();

		assertEquals("Warning, Internal Name not correctly set for DSFilterGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for DSFilterGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for DSFilterGroup\n", TESTDESC, Desc);
		assertEquals("Warning, Handler not set correctly for DSFilterGroup\n", TESTHANDLER, Handler);
	}

	private void secondFilterGroupTest(Dataset d, FilterPage fp, Object group) throws Exception {
		// second FilterGroup is a FilterGroup object, with everything contained within it
		assertTrue("Second FilterGroup in the FilterPage should be a FilterGroup object", group instanceof FilterGroup);

		FilterGroup fg = (FilterGroup) group;
		String testIName = "testFilterGroup";
		String IName = fg.getInternalName();
		String testDName = "Test A FilterGroup:";
		String DName = fg.getDisplayName();
		String Desc = fg.getDescription();

		assertEquals("Warning, Internal Name not correctly set for FilterGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterGroup\n", TESTDESC, Desc);

		// contains/get for FilterPage-FilterGroup
		boolean containsTest = fp.containsFilterGroup(testIName);
		assertTrue("Warning, FilterPage should contain testFilterGroup, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((FilterGroup) fp.getFilterGroupByName(testIName)).getInternalName();
			assertEquals("Warning, getFilterGroupByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterSet data correct
		assertTrue("Warning, FilterGroup should contain a FilterSet\n", fg.hasFilterSets());
		FilterSet[] fsets = fg.getFilterSets();
		assertEquals("Warning, should only get one FilterSet\n", 1, fsets.length);

		filterSetTest(fg, fsets[0]);

		//FilterCollection data correct
		FilterCollection[] fcs = fg.getFilterCollections();
		assertEquals("Warning, should get three filter collections\n", 3, fcs.length);

		firstFilterCollectionTest(d, fp, fg, fcs[0]);
		secondFilterCollectionTest(d, fp, fg, fcs[1]);
		thirdFilterCollectionTest(d, fp, fg, fcs[2]);
	}

	private void secondAttributeGroupTest(Dataset d, AttributePage ap, Object group) throws Exception {
		//second AttributeGroup in the AttributePage is an AttributeGroup, with everything in it
		assertTrue("Warning, Second AttributeGroup in the AttributePage should be an AttributeGroup", group instanceof AttributeGroup);

		AttributeGroup ag = (AttributeGroup) group;
		String testIName = "testAttributeGroup";
		String IName = ag.getInternalName();
		String testDName = "test of an Attribute Group:";
		String DName = ag.getDisplayName();
		String Desc = ag.getDescription();

		assertEquals("Warning, Internal Name not correctly set for AttributeGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributeGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributeGroup\n", TESTDESC, Desc);

		// contains/get for AttributePage-AttributeGroup
		boolean containsTest = ap.containsAttributeGroup(testIName);
		assertTrue("Warning, AttributePage should contain testAttributeGroup, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((AttributeGroup) ap.getAttributeGroupByName(testIName)).getInternalName();
			assertEquals("Warning, getAttributeGroupByName InternalName incorrect\n", testIName, testGetByName);
		}

		//AttributeCollection data correct
		AttributeCollection[] acs = ag.getAttributeCollections();
		assertEquals("Warning, should only get one attribute collection\n", 1, acs.length);

		attributeCollectionTest(d, ap, ag, acs[0]);
	}

	private void filterSetTest(FilterGroup fg, FilterSet fset) throws Exception {
		String testIName = "testFilterSet";
		String IName = fset.getInternalName();
		String fsetName = IName; // use during test of FilterSet Functionality
		String testDName = "Test of a Filter Set";
		String DName = fset.getDisplayName();
		String Desc = fset.getDescription();
		String Type = fset.getType();

		assertEquals("Warning, Internal Name not correctly set for FilterSet\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterSet\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterSet\n", TESTDESC, Desc);
		assertEquals("Warning, Type not correctly set for FilterSet\n", TESTTYPE, Type);

		//  contains/get for FilterGroup-FilterSet
		boolean containsTest = fg.containsFilterSet(testIName);
		assertTrue("Warning, FilterGroup should contain testFilterSet, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = fg.getFilterSetByName(testIName).getInternalName();
			assertEquals("Warning, getFilterSetByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterSetDescription data correct
		FilterSetDescription[] fsds = fset.getFilterSetDescriptions();
		assertEquals("Warning, FilterSet should contain only one FilterSetDescription\n", 1, fsds.length);

		FilterSetDescription fsd = fsds[0];
		testIName = "testFilterSetDescription";
		IName = fsd.getInternalName();
		testDName = "Test of a FilterSetDescription";
		DName = fsd.getDisplayName();
		String testTableConstraintModifier = "ensemblgene";
		Desc = fsd.getDescription();
		String TableConstraintModifier = fsd.getTableConstraintModifier();
		String testFieldNameModifier = "ensembl";
		String FieldNameModifier = fsd.getFieldNameModifier();

		assertEquals("Warning, Internal Name not correctly set for FilterSetDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterSetDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterSetDescription\n", TESTDESC, Desc);
		assertEquals("Warning, tableConstraintModifier not set correctly for FilterSetDescription\n", testTableConstraintModifier, TableConstraintModifier);
		assertEquals("Warning, fieldNameModifier not set correctly for FilterSetDescription\n", testFieldNameModifier, FieldNameModifier);

		//contains/get for FIlterSet-FilterSetDescription
		containsTest = fset.containsFilterSetDescription(testIName);
		assertTrue("Warning, FilterSet should contain testFilterSetDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = fset.getFilterSetDescriptionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterSetDescriptionByName internalName incorrect\n", testIName, testGetByName);
		}
	}

	private void firstFilterCollectionTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc) throws Exception {
		// first FilterCollection is not in a FilterSet
		String testIName = "testFilterCollection";
		String IName = fc.getInternalName();
		String testDName = "Test of a FilterCollection";
		String DName = fc.getDisplayName();
		String Desc = fc.getDescription();
		String Type = fc.getType();

		assertEquals("Warning, Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection\n", TESTDESC, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection\n", TESTTYPE, Type);
		assertTrue("First FilterCollection should not be in a FilterSet\n", !fc.inFilterSet());

		//  contains/get for FilterGroup-FilterCollection
		boolean containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFilterCollection, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//UIFilterDescription data correct
		List fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should get two filter descriptions with first FilterCollection\n", 2, fs.size());

		firstFColFirstFdescTest(d, fp, fg, fc, fs.get(0));
		firstFColSecFdescTest(d, fp, fg, fc, fs.get(1));
	}

	private void secondFilterCollectionTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc) throws Exception {
		//second FilterCollection is a member of the FilterSet
		String testIName = "testFilterSetCollection";
		String IName = fc.getInternalName();
		String testDName = "A TEST OF FILTER SETS";
		String DName = fc.getDisplayName();
		String Desc = fc.getDescription();
		String Type = fc.getType();
		String testFilterSetName = "testFilterSet";
		String FilterSetName = fc.getFilterSetName();

		assertEquals("Warning, Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection\n", TESTDESC, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection\n", TESTTYPE, Type);
		assertEquals("Warning, FilterSetName not correctly set for FilterCollection\n", testFilterSetName, FilterSetName);
		assertTrue("Second FilterCollection should be in a FilterSet\n", fc.inFilterSet());

		//  contains/get for FilterGroup-FilterCollection
		boolean containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFiltersetCollection, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//UIFilterDescription data correct
		List fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should get three filter descriptions\n", 3, fs.size());

		secondFColFirstFdescTest(d, fp, fg, fc, fs.get(0));
		secondFColSecFdescTest(d, fp, fg, fc, fs.get(1));
		secondFColThirdFdescTest(d, fp, fg, fc, fs.get(2));
	}

	private void thirdFilterCollectionTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc) throws Exception {
		// third FilterCollection has Options
		String testIName = "testOptionCollection";
		String IName = fc.getInternalName();
		String testDName = "A TEST OF Options";
		String DName = fc.getDisplayName();
		String Desc = fc.getDescription();
		String Type = fc.getType();

		assertEquals("Warning, Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection\n", TESTDESC, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection\n", TESTTYPE, Type);
		assertTrue("Third FilterCollection should have Options\n", fc.hasOptions());

		//  contains/get for FilterGroup-FilterCollection
		boolean containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFiltersetCollection, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		// Option data correct
		Option[] options = fc.getOptions();
		assertEquals("Warning, should get two Options in Third FilterCollection\n", 2, options.length);

		thirdFColFirstOptiontest(fc, options[0]);
		thirdFColSecOptionTest(fc, options[1]);

		//UIFilterDescription data correct
		List fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should get two filter descriptions\n", 2, fs.size());

		thirdFColFirstFdescTest(d, fp, fg, fc, fs.get(0));
		thirdFColSecFdescTest(d, fp, fg, fc, fs.get(1));
	}

	private void attributeCollectionTest(Dataset d, AttributePage ap, AttributeGroup ag, AttributeCollection ac) throws Exception {
		String testIName = "testAttributeCollection";
		String IName = ac.getInternalName();
		String testDName = "Test of an AttributeCollection:";
		String DName = ac.getDisplayName();
		String Desc = ac.getDescription();
		int testMaxSelect = 1;
		int MaxSelect = ac.getMaxSelect();

		assertEquals("Warning, Internal Name not correctly set for AttributeCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributeCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributeCollection\n", TESTDESC, Desc);
		assertEquals("Warning, Max Select not correctly set for AttributeCollection\n", testMaxSelect, MaxSelect);

		//  contains/get for AttributeGroup-AttributeCollection
		boolean containsTest = ag.containsAttributeCollection(testIName);
		assertTrue("Warning, AttributeGroup should contain testAttributeCollection, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ag.getAttributeCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getAttributeCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//UIAttributeDescription data correct
		List as = ac.getUIAttributeDescriptions();
		assertEquals("Warning, should get one attribute description\n", 1, as.size());

		attributeCollectionFdescTest(d, ap, ag, ac, as.get(0));
	}

	private void firstFColFirstFdescTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc, Object ob) {
		assertTrue("Warning, First FilterDescription of First FilterCollection should be an instance of UIFilterDescription.\n", ob instanceof UIFilterDescription);

		UIFilterDescription f = (UIFilterDescription) ob;
		String testIName = "testUIFilterDescription";
		String IName = f.getInternalName();
		String testDName = "A TEST ID, DOESNT EXIST";
		String DName = f.getDisplayName();
		String Desc = f.getDescription();
		String Type = f.getType();
		String testFieldName = "test_id";
		String FieldName = f.getFieldName();
		String Qualifier = f.getQualifier();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Warning, Internal Name not correctly set for UIFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescription\n", TESTDESC, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescription\n", TESTTYPE, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescription\n", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescription\n", testTableConstraint, TableConstraint);
		assertTrue("Warning, first FilterCollections UIFilterDescription should not be in a FilterSet\n", !f.inFilterSet());

		//  contains/get for FilterCollection-UIFilterDescription
		boolean containsTest = fc.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterCollection should contain testUIFilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((UIFilterDescription) fc.getUIFilterDescriptionByName(testIName)).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//  contains/get for FilterPage-UIFilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((UIFilterDescription) fp.getUIFilterDescriptionByName(testIName)).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Warning, Did not get the correct Page for the UIFilterDescription\n", "testFilterPage", d.getPageForFilter(testIName).getInternalName());
		}
	}

	private void firstFColSecFdescTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc, Object ob) throws Exception {
		//  second FilterDescription is a UIDSFilterDescription object
		assertTrue(
			"Warning, Second FilterDescription of First FilterCollection should be an instance of UIDSFilterDescription.\n",
			ob instanceof UIDSFilterDescription);

		UIDSFilterDescription dsf = (UIDSFilterDescription) ob;
		String testIName = "testUIDSFilterDescription";
		String IName = dsf.getInternalName();
		String testDName = "A TEST ID, DOESNT EXIST";
		String DName = dsf.getDisplayName();
		String Desc = dsf.getDescription();
		String Type = dsf.getType();
		String testFieldName = "test_id";
		String Handler = dsf.getHandler();

		assertEquals("Warning, Internal Name not correctly set for UIDSFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIDSFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIDSFilterDescription\n", TESTDESC, Desc);
		assertEquals("Warning, Type not set correctly for UIDSFilterDescription\n", TESTTYPE, Type);
		assertEquals("Warning, Handler not set correctly for UIDSFilterDescription\n", TESTHANDLER, Handler);

		//  contains/get for FilterCollection-UIFilterDescription
		boolean containsTest = fc.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterCollection should contain testUIFilterDescription, but doesnt\n", containsTest);

		String testGetByName = null;
		if (containsTest) {
			testGetByName = ((UIDSFilterDescription) fc.getUIFilterDescriptionByName(testIName)).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//  contains/get for FilterPage-UIFilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterPage should contain testUIDSFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((UIDSFilterDescription) fp.getUIFilterDescriptionByName(testIName)).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);

			//test getPageFor functionality as well
			assertEquals("Warning, Did not get the correct Page for the UIFilterDescription\n", "testFilterPage", d.getPageForFilter(testIName).getInternalName());
		}
	}

	private void secondFColFirstFdescTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc, Object ob) throws Exception {
		// first FilterDescription of this FilterCollection is a UIFilterDescription, is part of the FilterSet, and requires a tableConstraintModifier
		assertTrue("Warning, First FilterDescription in FilterSetFilterCollection should be a UIFilterDescription", ob instanceof UIFilterDescription);
		UIFilterDescription fField = (UIFilterDescription) ob;
		String testIName = "filterSetUIFilterDescriptionField";
		String IName = fField.getInternalName();
		String testDName = "A TEST FIELD MODIFIER";
		String DName = fField.getDisplayName();
		String Desc = fField.getDescription();
		String Type = fField.getType();
		String testFieldName = "syn_exclusive";
		String FieldName = fField.getFieldName();
		String Qualifier = fField.getQualifier();
		String testTableConstraint = "gene_main";
		String TableConstraint = fField.getTableConstraint();
		String testFilterSetReq = "field";
		String FilterSetReq = fField.getFilterSetReq();
		String testModifiedName = "ensemblsyn_exclusive";
		String ModifiedName = null;

		String testFilterSetDIName = "testFilterSetDescription";
		if (FilterSetReq.equals(FilterSetDescription.MODFIELDNAME))
			ModifiedName =
				fg.getFilterSetByName(fc.getFilterSetName()).getFilterSetDescriptionByName(testFilterSetDIName).getFieldNameModifier() + fField.getFieldName();
		else
			ModifiedName =
				fg.getFilterSetByName(fc.getFilterSetName()).getFilterSetDescriptionByName(testFilterSetDIName).getTableConstraintModifier()
					+ fField.getTableConstraint();

		assertEquals("Warning, Internal Name not correctly set for UIFilterDescriptionField\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescriptionField\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescriptionField\n", TESTDESC, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescriptionField\n", TESTTYPE, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescriptionField\n", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescriptionField\n", TESTQUALIFIER, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescriptionField\n", testTableConstraint, TableConstraint);
		assertEquals("Warning, filterSetReq not set correctly for UIFilterDescriptionField\n", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, modified Field Name not correct\n", testModifiedName, ModifiedName);
		assertTrue("Warning, second FilterCollections UIFilterDescriptionField should be in a FilterSet\n", fField.inFilterSet());
	}

	private void secondFColSecFdescTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc, Object ob) throws Exception {
		//  second FilterDescription of this FilterCollection is a UIFilterDescription, is part of the FilterSet, and requires a tableConstraintModifier
		assertTrue("Warning, Second FilterDescription in FilterSetFilterCollection should be a UIFilterDescription", ob instanceof UIFilterDescription);
		UIFilterDescription fTable = (UIFilterDescription) ob;
		String testIName = "filterSetUIFilterDescriptionTable";
		String IName = fTable.getInternalName();
		String testDName = "A TEST TABLE MODIFIER";
		String DName = fTable.getDisplayName();
		String Desc = fTable.getDescription();
		String Type = fTable.getType();
		String testFieldName = "gene_stable_id_v";
		String FieldName = fTable.getFieldName();
		String Qualifier = fTable.getQualifier();
		String testFilterSetReq = "table";
		String FilterSetReq = fTable.getFilterSetReq();
		String testTableConstraint = "_dm";
		String TableConstraint = fTable.getTableConstraint();
		String testModifiedName = "ensemblgene_dm";
		String ModifiedName = null;

		String testFilterSetDIName = "testFilterSetDescription";
		if (FilterSetReq.equals(FilterSetDescription.MODFIELDNAME))
			ModifiedName =
				fg.getFilterSetByName(fc.getFilterSetName()).getFilterSetDescriptionByName(testFilterSetDIName).getFieldNameModifier() + fTable.getFieldName();
		else
			ModifiedName =
				fg.getFilterSetByName(fc.getFilterSetName()).getFilterSetDescriptionByName(testFilterSetDIName).getTableConstraintModifier()
					+ fTable.getTableConstraint();

		assertEquals("Warning, Internal Name not correctly set for UIFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescription\n", TESTDESC, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescription\n", TESTTYPE, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescription\n", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescription\n", testTableConstraint, TableConstraint);
		assertEquals("Warning, filterSetReq not set correctly for UIFilterDescriptionField\n", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, Modified TableConstraint not correct\n", testModifiedName, ModifiedName);
		assertTrue("Warning, second FilterCollection second UIFilterDescription should be in a FilterSet\n", fTable.inFilterSet());
	}

	private void secondFColThirdFdescTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc, Object ob) throws Exception {
		//third FilterDescription is a UIDSFilterDescription, is part of a FilterSet, and requires a tableConstraintModifier
		assertTrue("Warning, third FilterDescription in FilterSetFilterCollection should be a UIDSFilterDescription\n", ob instanceof UIDSFilterDescription);
		UIDSFilterDescription dsfTable = (UIDSFilterDescription) ob;
		String testIName = "filterSetUIDSFilterDescription";
		String IName = dsfTable.getInternalName();
		String testDName = "A TEST ID, DOESNT EXIST";
		String DName = dsfTable.getDisplayName();
		String Desc = dsfTable.getDescription();
		String Type = dsfTable.getType();
		String testFilterSetReq = "table";
		String FilterSetReq = dsfTable.getFilterSetReq();
		String Handler = dsfTable.getHandler();

		assertEquals("Warning, Internal Name not correctly set for UIDSFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIDSFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIDSFilterDescription\n", TESTDESC, Desc);
		assertEquals("Warning, Type not set correctly for UIDSFilterDescription\n", TESTTYPE, Type);
		assertEquals("Warning, filterSetReq not set correctly for UIDSFilterDescriptionField\n", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, Handler not set correctly for UIDSFilterDescription\n", TESTHANDLER, Handler);
		assertTrue("Warning, third UIDSFilterDescription should be in a FilterSet\n", dsfTable.IsInFilterSet());
	}

	private void thirdFColFirstOptiontest(FilterCollection fc, Option option) throws Exception {
		// first option does not contain options, and isSelectable is true
		String testIName = "testOption";
		String IName = option.getInternalName();
		String testDName = "A Test Option";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
	
		assertEquals("Warning, Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Option\n", TESTDESC, Desc);
		assertTrue("First Option " + option + " should be selectable\n", option.isSelectable());
		assertTrue("First Option " + option + "should not have Options\n", !option.hasOptions());
	
		//contains/get for FilterCollection-Option
		boolean containsTest = fc.containsOption(testIName);
		assertTrue("Warning, Third FilterCollection should contain testOption, but doesnt\n", containsTest);
	
		String testGetByName = null;
		if (containsTest) {
			testGetByName = fc.getOptionByName(testIName).getInternalName();
			assertEquals("Warning, getOptionByName InternalName incorrect\n", testIName, testGetByName);
		}
	}

	private void thirdFColSecOptionTest(FilterCollection fc, Option option) throws Exception {
		// Second option contains one option, and isSelectable is false   
		String testIName = "testOptionWithOption";
		String IName = option.getInternalName();
		String testDName = "A Test Option With an Option";
		String DName = option.getDisplayName();
		String Desc = option.getDescription();
	
		assertEquals("Warning, Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Option\n", TESTDESC, Desc);
		assertTrue("Second Option" + option + " should not be selectable\n", !option.isSelectable());
		assertTrue("Second Option" + option + " should have Options\n", option.hasOptions());
	
		//contains/get for FilterCollection-Option
		boolean containsTest = fc.containsOption(testIName);
		assertTrue("Warning, Third FilterCollection should contain testOptionWithOption, but doesnt\n", containsTest);
	
		String testGetByName = null;
		if (containsTest) {
			testGetByName = fc.getOptionByName(testIName).getInternalName();
			assertEquals("Warning, getOptionByName InternalName incorrect\n", testIName, testGetByName);
		}
	
		Option[] subOptions = option.getOptions();
		assertEquals("Warning, second option should only contain one Option\n", 1, subOptions.length);
	
		thirdFColSecOptionSubOptionTest(option, subOptions[0]);
	}

	private void thirdFColSecOptionSubOptionTest(Option option, Option suboption) throws Exception {
		// sub option does not contain options, and isSelectable is true
		String testIName = "testOptionInOption";
		String IName = suboption.getInternalName();
		String testDName = "A Test Option In an Option";
		String DName = suboption.getDisplayName();
		String Desc = suboption.getDescription();
	
		assertEquals("Warning, Internal Name not correctly set for Sub Option\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Sub Option\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Sub Option\n", TESTDESC, Desc);
		assertTrue("Sub Option should be selectable\n", suboption.isSelectable());
		assertTrue("Sub Option should not have Options\n", !suboption.hasOptions());
	
		//contains/get for Option-Option
		boolean containsTest = option.containsOption(testIName);
		assertTrue("Warning, Second Option should contain testOptionInOption, but doesnt\n", containsTest);
	
		String testGetByName = null;
		if (containsTest) {
			testGetByName = option.getOptionByName(testIName).getInternalName();
			assertEquals("Warning, getOptionByName InternalName incorrect\n", testIName, testGetByName);
		}
	}
  
  private void thirdFColFirstFdescTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc, Object ob) throws Exception {
    // first FilterDescription of this FilterCollection is a UIFilterDescription, and has Options in first Option
    assertTrue("Warning, First FilterDescription in OptionFilterCollection should be a UIFilterDescription", ob instanceof UIFilterDescription);
    UIFilterDescription fField = (UIFilterDescription) ob;
    String testIName = "OptionFilterDescription";
    String IName = fField.getInternalName();
    String testDName = "A FilterDescription With An Option";
    String DName = fField.getDisplayName();
    String Desc = fField.getDescription();
    String Type = fField.getType();
    String testFieldName = "test_id";
    String FieldName = fField.getFieldName();
    String Qualifier = fField.getQualifier();
    String testOptionName = "testOption";
    String OptionName = fField.getOptionName();

    assertEquals("Warning, Internal Name not correctly set for UIFilterDescription\n", testIName, IName);
    assertEquals("Warning, Display Name not correctly set for UIFilterDescription\n", testDName, DName);
    assertEquals("Warning, Description not correctly set for UIFilterDescription\n", TESTDESC, Desc);
    assertEquals("Warning, Type not set correctly for UIFilterDescription\n", TESTTYPE, Type);
    assertEquals("Warning, FieldName not set correctly for UIFilterDescription\n", testFieldName, FieldName);
    assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription\n", TESTQUALIFIER, Qualifier);
    assertEquals("Warning, optionName not correctly set for UIFilterDescription\n", testOptionName, OptionName);
    assertTrue("Warning, Third FilterColletion should contain option refereced by UIFilterDescription but doesnt\n", fc.containsOption(OptionName));
  }
  
  private void thirdFColSecFdescTest(Dataset d, FilterPage fp, FilterGroup fg, FilterCollection fc, Object ob) throws Exception {
    //second FilterDescription is a UIDSFilterDescription, and has Options in Second Option
    assertTrue("Warning, second FilterDescription in optionFilterCollection should be a UIDSFilterDescription\n", ob instanceof UIDSFilterDescription);
    UIDSFilterDescription dsf = (UIDSFilterDescription) ob;
    String testIName = "optionUIDSFilterDescription";
    String IName = dsf.getInternalName();
    String testDName = "A TEST ID, DOESNT EXIST";
    String DName = dsf.getDisplayName();
    String Desc = dsf.getDescription();
    String Type = dsf.getType();
    String Handler = dsf.getHandler();
    String testOptionName = "testOptionWithOption";
    String OptionName = dsf.getOptionName();

    assertEquals("Warning, Internal Name not correctly set for UIDSFilterDescription\n", testIName, IName);
    assertEquals("Warning, Display Name not correctly set for UIDSFilterDescription\n", testDName, DName);
    assertEquals("Warning, Description not correctly set for UIDSFilterDescription\n", TESTDESC, Desc);
    assertEquals("Warning, Type not set correctly for UIDSFilterDescription\n", TESTTYPE, Type);
    assertEquals("Warning, Handler not set correctly for UIDSFilterDescription\n", TESTHANDLER, Handler);
    assertEquals("Warning, optionName not set correctly for UIDSFilterDescription\n", testOptionName, OptionName);
    assertTrue("Warning, Third FilterColletion should contain option refereced by UIDSFilterDescription but doesnt\n", fc.containsOption(OptionName));
  }

	private void attributeCollectionFdescTest(Dataset d, AttributePage ap, AttributeGroup ag, AttributeCollection ac, Object ob) throws Exception {
		// first AttributeDescription is a UIAttributeDescription
		assertTrue("First AttributeDescription should be a UIAttributeDescription", ob instanceof UIAttributeDescription);

		UIAttributeDescription a = (UIAttributeDescription) ob;
		String testIName = "testUIAttributeDescription";
		String IName = a.getInternalName();
		String testDName = "Test of a UIAttributeDescription";
		String DName = a.getDisplayName();
		String Desc = a.getDescription();
		String testFieldName = "test_id";
		String FieldName = a.getFieldName();
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

		assertEquals("Warning, Internal Name not correctly set for UIAttributeDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIAttributeDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIAttributeDescription\n", TESTDESC, Desc);
		assertEquals("Warning, FieldName not correctly set for UIAttributeDescription\n", testFieldName, FieldName);
		assertEquals("Warning, TableConstraint not correctly set for UIAttributeDescription\n", testTableConstraint, TableConstraint);
		assertEquals("Warning, MaxLength not correctly set for UIAttributeDescription\n", testMaxLength, MaxLength);
		assertEquals("Warning, Source not correctly set for UIAttributeDescription\n", testSource, Source);
		assertEquals("Warning, HomepageURL not correctly set for UIAttributeDescription\n", testHPage, HPage);
		assertEquals("Warning, LinkoutURL not correctly set for UIAttributeDescription\n", testLPage, LPage);

		//  contains/get for AttributeCollection-UIAttributeDescription
		boolean containsTest = ac.containsUIAttributeDescription(testIName);
		assertTrue("Warning, AttributeCollection should contain testUIAttributeDescription, but doesnt\n", containsTest);
    
    String testGetByName = null;
		if (containsTest) {
			testGetByName = ((UIAttributeDescription) ac.getUIAttributeDescriptionByName(testIName)).getInternalName();
			assertEquals("Warning, getUIAttributeDescriptionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//  contains/get for AttributePage-UIAttributeDescription (Tests all lower groups getByName as well
		containsTest = ap.containsUIAttributeDescription(testIName);
		assertTrue("Warning, AttributePage should contain testUIAttributeDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ((UIAttributeDescription) ap.getUIAttributeDescriptionByName(testIName)).getInternalName();
			assertEquals("Warning, getUIAttributeDescriptionByName InternalName incorrect\n", testIName, testGetByName);

			//test getPageFor functionality as well
			assertEquals(
				"Warning, Did not get the correct Page for the UIAttributeDescription\n",
				"testAttributePage",
				d.getPageForAttribute(testIName).getInternalName());
		}
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
	private final String TESTQUALIFIER = "in";
}
