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

		Document doc = builder.build(MartXMLutils.getInputSourceFor(conn, xmlTestID));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLOutputter xout = new XMLOutputter();
		xout.output(doc, out);

		String initxml = out.toString();
		out.close();

		MartXMLutils.storeConfiguration(conn, xmlTestID, doc);

		SAXBuilder newbuilder = new SAXBuilder();
		newbuilder.setValidation(true);
		newbuilder.setEntityResolver(new MartDTDEntityResolver(conn));
		// set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.

		Document newdoc = newbuilder.build(MartXMLutils.getInputSourceFor(conn, xmlTestID));

		out = new ByteArrayOutputStream();
		xout.output(newdoc, out);

		String newxml = out.toString();
		out.close();

		assertEquals("Warning, initial xml does not match final xml after a roundtrip.\n", initxml, newxml);
	}







	public void testMartConfiguration() throws Exception {
		MartConfiguration martconf = new MartConfigurationFactory().getInstance(
    engine.getConnection(),
    xmlTestID);

		//Mart Data Correct
		String testIName = "ensembl_mart_14_1";
		String IName = martconf.getInternalName();
		String testDName = "Ensembl Mart version 14.1";
		String DName = martconf.getDisplayName();
		String testDesc = "First Mart Created from version 14 Ensembl Datasources";
		String Desc = martconf.getDescription();

		assertEquals("Warning, MartName not correctly set for MartConfiguration\n", testIName, IName);
		assertEquals("Warning Mart Display Name not correctly set for MartConfiguration\n", testDName, DName);
		assertEquals("Warning Mart Description not correctly set for MartConfiguration\n", testDesc, Desc);

		// Dataset Data Correct
		Dataset[] ds = martconf.getDatasets();
		assertEquals("Warning, should only be one dataset, got " + ds.length + "\n", 1, ds.length);

		Dataset d = ds[0];
		testIName = "test_dataset";
		IName = d.getInternalName();
		testDName = "Test of a Dataset";
		DName = d.getDisplayName();
		testDesc = "For Testing Purposes Only";
		Desc = d.getDescription();

		assertEquals("Warning, Internal Name not correctly set for Dataset\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Dataset\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Dataset\n", testDesc, Desc);

		//contains/get for MartConfiguration-Dataset
		boolean containsTest = martconf.containsDataset(testIName);
		String testGetByName = null;

		assertTrue("Warning, MartConfiguration should contain test_dataset, but doesnt\n", containsTest);
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

		//FilterPage data correct
		FilterPage[] fps = d.getFilterPages();
		assertEquals("Warning, should only get one filter page\n", 1, fps.length);

		FilterPage fp = fps[0];
		testIName = "testFilterPage";
		IName = fp.getInternalName();
		testDName = "Test A Filter Page";
		DName = fp.getDisplayName();
		Desc = fp.getDescription();

		assertEquals("Warning, Internal Name not correctly set for FilterPage\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterPage\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterPage\n", testDesc, Desc);

		// contains/get for Dataset-FilterPage
		containsTest = d.containsFilterPage(testIName);
		assertTrue("Warning, Dataset should contain testFilterPage, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = d.getFilterPageByName(testIName).getInternalName();
			assertEquals("Warning, getFilterPageByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterGroup data correct
		List fgs = fp.getFilterGroups();
		assertEquals("Warning, should get two filterGroups in FilterPage\n", 2, fgs.size());

    // first filterGroup is a DSFilterGroup object
		assertTrue("First FilterGroup in the FilterPage should be a DSFilterGroup object", fgs.get(0) instanceof DSFilterGroup);
		
		DSFilterGroup dsfg = (DSFilterGroup) fgs.get(0);
    testIName = "testDSFilterGroup";
    IName = dsfg.getInternalName();
    testDName = "Test A DSFilterGroup:";
    DName = dsfg.getDisplayName();
    Desc = dsfg.getDescription();
		String testObjectCode = "testObjectCode";
    String ObjectCode = dsfg.getObjectCode();
    
		assertEquals("Warning, Internal Name not correctly set for DSFilterGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for DSFilterGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for DSFilterGroup\n", testDesc, Desc);
		assertEquals("Warning, ObjectCode not set correctly for DSFilterGroup\n", testObjectCode, ObjectCode);

		// contains/get for FilterPage-FilterGroup
		containsTest = fp.containsFilterGroup(testIName);
		assertTrue("Warning, FilterPage should contain testFilterGroup, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (DSFilterGroup) fp.getFilterGroupByName(testIName)).getInternalName();
			assertEquals("Warning, getFilterGroupByName InternalName incorrect\n", testIName, testGetByName);
		}
				
    // second FilterGroup is a DSFilterGroup object, with everything contained within it
    assertTrue("Second FilterGroup in the FilterPage should be a FilterGroup object", fgs.get(1) instanceof FilterGroup);
    
		FilterGroup fg = (FilterGroup) fgs.get(1);
		testIName = "testFilterGroup";
		IName = fg.getInternalName();
		testDName = "Test A FilterGroup:";
		DName = fg.getDisplayName();
		Desc = fg.getDescription();

		assertEquals("Warning, Internal Name not correctly set for FilterGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterGroup\n", testDesc, Desc);

		// contains/get for FilterPage-FilterGroup
		containsTest = fp.containsFilterGroup(testIName);
		assertTrue("Warning, FilterPage should contain testFilterGroup, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (FilterGroup) fp.getFilterGroupByName(testIName)).getInternalName();
			assertEquals("Warning, getFilterGroupByName InternalName incorrect\n", testIName, testGetByName);
		}

		//FilterSet data correct
		assertTrue("Warning, FilterGroup should contain a FilterSet\n", fg.hasFilterSets());
		FilterSet[] fsets = fg.getFilterSets();
		assertEquals("Warning, should only get one FilterSet\n", 1, fsets.length);

		FilterSet fset = fsets[0];
		testIName = "testFilterSet";
		IName = fset.getInternalName();
		String fsetName = IName; // use during test of FilterSet Functionality
		testDName = "Test of a Filter Set";
		DName = fset.getDisplayName();
		Desc = fset.getDescription();
		String testType = "radio";
		String Type = fset.getType();

		assertEquals("Warning, Internal Name not correctly set for FilterSet\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterSet\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterSet\n", testDesc, Desc);
		assertEquals("Warning, Type not correctly set for FilterSet\n", testType, Type);

		//	contains/get for FilterGroup-FilterSet
		containsTest = fg.containsFilterSet(testIName);
		assertTrue("Warning, FilterGroup should contain testFilterSet, but doesnt\n", containsTest);
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
		assertEquals("Warning, Description not correctly set for FilterSetDescription\n", testDesc, Desc);
		assertEquals("Warning, tableConstraintModifier not set correctly for FilterSetDescription\n", testTableConstraintModifier, TableConstraintModifier);
		assertEquals("Warning, fieldNameModifier not set correctly for FilterSetDescription\n", testFieldNameModifier, FieldNameModifier);

		//contains/get for FIlterSet-FilterSetDescription
		containsTest = fset.containsFilterSetDescription(testIName);
		assertTrue("Warning, FilterSet should contain testFilterSetDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = fset.getFilterSetDescriptionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterSetDescriptionByName internalName incorrect\n", testIName, testGetByName);
		}

		//FilterCollection data correct
		FilterCollection[] fcs = fg.getFilterCollections();
		assertEquals("Warning, should get three filter collections\n", 3, fcs.length);

		// first FilterCollection is not in a FilterSet
		FilterCollection fc = fcs[0];
		testIName = "testFilterCollection";
		IName = fc.getInternalName();
		testDName = "Test of a FilterCollection";
		DName = fc.getDisplayName();
		Desc = fc.getDescription();
		testType = "list";
		Type = fc.getType();

		assertEquals("Warning, Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection\n", testDesc, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection\n", testType, Type);
		assertTrue("First FilterCollection should not be in a FilterSet\n", !fc.inFilterSet());

		//	contains/get for FilterGroup-FilterCollection
		containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFilterCollection, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//UIFilterDescription data correct
    List fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should get two filter descriptions with first FilterCollection\n", 2, fs.size());

    Object ob = fs.get(0);
    assertTrue("Warning, First FilterDescription of First FilterCollection should be an instance of UIFilterDescription.\n", ob instanceof UIFilterDescription);
    
		UIFilterDescription f = (UIFilterDescription) ob; 
		testIName = "testUIFilterDescription";
		IName = f.getInternalName();
		testDName = "A TEST ID, DOESNT EXIST";
		DName = f.getDisplayName();
		Desc = f.getDescription();
		Type = f.getType();
		String testFieldName = "test_id";
		String FieldName = f.getFieldName();
		String testQualifier = "in";
		String Qualifier = f.getQualifier();
		String testTableConstraint = "gene_main";
		String TableConstraint = f.getTableConstraint();

		assertEquals("Warning, Internal Name not correctly set for UIFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescription\n", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescription\n", testType, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescription\n", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription\n", testQualifier, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescription\n", testTableConstraint, TableConstraint);
		assertTrue("Warning, first FilterCollections UIFilterDescription should not be in a FilterSet\n", !f.inFilterSet());

		//	contains/get for FilterCollection-UIFilterDescription
		containsTest = fc.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterCollection should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (UIFilterDescription) fc.getUIFilterDescriptionByName(testIName) ).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//	contains/get for FilterPage-UIFilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterPage should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (UIFilterDescription) fp.getUIFilterDescriptionByName(testIName) ).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);

			//test getPageFor functionality as well
			assertEquals(
				"Warning, Did not get the correct Page for the UIFilterDescription\n",
				"testFilterPage",
				d.getPageForUIFilterDescription(testIName).getInternalName());
		}

    // second FilterDescription is a UIDSFilterDescription object
		ob = fs.get(1);
		assertTrue("Warning, Second FilterDescription of First FilterCollection should be an instance of UIDSFilterDescription.\n", ob instanceof UIDSFilterDescription);
    
		UIDSFilterDescription dsf = (UIDSFilterDescription) ob; 
		testIName = "testUIDSFilterDescription";
		IName = dsf.getInternalName();
		testDName = "A TEST ID, DOESNT EXIST";
		DName = dsf.getDisplayName();
		Desc = dsf.getDescription();
		Type = dsf.getType();
		testFieldName = "test_id";
    ObjectCode = dsf.getObjectCode();

		assertEquals("Warning, Internal Name not correctly set for UIDSFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIDSFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIDSFilterDescription\n", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIDSFilterDescription\n", testType, Type);
    assertEquals("Warning, ObjectCode not set correctly for UIDSFilterDescription\n", testObjectCode, ObjectCode);

		//	contains/get for FilterCollection-UIFilterDescription
		containsTest = fc.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterCollection should contain testUIFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (UIDSFilterDescription) fc.getUIFilterDescriptionByName(testIName) ).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//	contains/get for FilterPage-UIFilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterPage should contain testUIDSFilterDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (UIDSFilterDescription) fp.getUIFilterDescriptionByName(testIName) ).getInternalName();
			assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect\n", testIName, testGetByName);

			//test getPageFor functionality as well
			assertEquals(
				"Warning, Did not get the correct Page for the UIFilterDescription\n",
				"testFilterPage",
				d.getPageForUIFilterDescription(testIName).getInternalName());
		}
				 
		//second FilterCollection is a member of the FilterSet
		fc = fcs[1];
		testIName = "testFilterSetCollection";
		IName = fc.getInternalName();
		testDName = "A TEST OF FILTER SETS";
		DName = fc.getDisplayName();
		Desc = fc.getDescription();
		testType = "list";
		Type = fc.getType();
		String testFilterSetName = "testFilterSet";
		String FilterSetName = fc.getFilterSetName();

		assertEquals("Warning, Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection\n", testDesc, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection\n", testType, Type);
		assertEquals("Warning, FilterSetName not correctly set for FilterCollection\n", testFilterSetName, FilterSetName);
		assertTrue("Second FilterCollection should be in a FilterSet\n", fc.inFilterSet());

		//	contains/get for FilterGroup-FilterCollection
		containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFiltersetCollection, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//UIFilterDescription data correct
		fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should get three filter descriptions\n", 3, fs.size());

		// first FilterDescription of this FilterCollection is a UIFilterDescription, is part of the FilterSet, and requires a tableConstraintModifier
		assertTrue("Warning, First FilterDescription in FilterSetFilterCollection should be a UIFilterDescription", fs.get(0) instanceof UIFilterDescription);		
		UIFilterDescription fField = (UIFilterDescription) fs.get(0);
		testIName = "filterSetUIFilterDescriptionField";
		IName = fField.getInternalName();
		testDName = "A TEST FIELD MODIFIER";
		DName = fField.getDisplayName();
		Desc = fField.getDescription();
		Type = fField.getType();
		testFieldName = "syn_exclusive";
		FieldName = fField.getFieldName();
		testQualifier = "in";
		Qualifier = fField.getQualifier();
		testTableConstraint = "gene_main";
		TableConstraint = fField.getTableConstraint();
		String testFilterSetReq = "field";
	  String FilterSetReq = fField.getFilterSetReq();
		String testModifiedName = "ensemblsyn_exclusive";
		String ModifiedName = null;

    if ( FilterSetReq.equals( FilterSetDescription.MODFIELDNAME ) )
		  ModifiedName = fsd.getFieldNameModifier() + fField.getFieldName();
		else
			ModifiedName = fsd.getTableConstraintModifier() + fField.getTableConstraint();

		assertEquals("Warning, Internal Name not correctly set for UIFilterDescriptionField\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescriptionField\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescriptionField\n", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescriptionField\n", testType, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescriptionField\n", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescriptionField\n", testQualifier, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescriptionField\n", testTableConstraint, TableConstraint);
		assertEquals("Warning, filterSetReq not set correctly for UIFilterDescriptionField\n", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, modified Field Name not correct\n", testModifiedName, ModifiedName);
		assertTrue("Warning, second FilterCollections UIFilterDescriptionField should be in a FilterSet\n", fField.inFilterSet());

		//second FilterDescription of this FilterCollection is a UIFilterDescription, is part of the FilterSet, and requires a tableConstraintModifier
		assertTrue("Warning, Second FilterDescription in FilterSetFilterCollection should be a UIFilterDescription", fs.get(1) instanceof UIFilterDescription);
		UIFilterDescription fTable = (UIFilterDescription) fs.get(1);
		testIName = "filterSetUIFilterDescriptionTable";
		IName = fTable.getInternalName();
		testDName = "A TEST TABLE MODIFIER";
		DName = fTable.getDisplayName();
		Desc = fTable.getDescription();
		Type = fTable.getType();
		testFieldName = "gene_stable_id_v";
		FieldName = fTable.getFieldName();
		Qualifier = fTable.getQualifier();
		testFilterSetReq = "table";
		FilterSetReq = fTable.getFilterSetReq();
		testTableConstraint = "_dm";
		TableConstraint = fTable.getTableConstraint();
		testModifiedName = "ensemblgene_dm";

		if ( FilterSetReq.equals( FilterSetDescription.MODFIELDNAME ) )
			ModifiedName = fsd.getFieldNameModifier() + fTable.getFieldName();
		else
			ModifiedName = fsd.getTableConstraintModifier() + fTable.getTableConstraint();

		assertEquals("Warning, Internal Name not correctly set for UIFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescription\n", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescription\n", testType, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescription\n", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription\n", testQualifier, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescription\n", testTableConstraint, TableConstraint);
		assertEquals("Warning, filterSetReq not set correctly for UIFilterDescriptionField\n", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, Modified TableConstraint not correct\n", testModifiedName, ModifiedName);
		assertTrue("Warning, second FilterCollection second UIFilterDescription should be in a FilterSet\n", fTable.inFilterSet());

    //third FilterDescription is a UIDSFilterDescription, is part of a FilterSet, and requires a tableConstraintModifier
    assertTrue("Warning, third FilterDescription in FilterSetFilterCollection should be a UIDSFilterDescription\n", fs.get(2) instanceof UIDSFilterDescription);
    UIDSFilterDescription dsfTable = (UIDSFilterDescription) fs.get(2);
    testIName = "filterSetUIDSFilterDescription";
    IName = dsfTable.getInternalName();
    testDName = "A TEST ID, DOESNT EXIST";
    DName = dsfTable.getDisplayName();
    Desc = dsfTable.getDescription();
    Type = dsfTable.getType();
    testFilterSetReq = "table";
    FilterSetReq = dsfTable.getFilterSetReq();
		testObjectCode = "testObjectCode";
	  ObjectCode = dsf.getObjectCode();
    
		assertEquals("Warning, Internal Name not correctly set for UIDSFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIDSFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIDSFilterDescription\n", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIDSFilterDescription\n", testType, Type);
		assertEquals("Warning, filterSetReq not set correctly for UIDSFilterDescriptionField\n", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, ObjectCode not set correctly for UIDSFilterDescription\n", testObjectCode, ObjectCode);
		assertTrue("Warning, third UIDSFilterDescription should be in a FilterSet\n", dsfTable.IsInFilterSet());
		
		// third FilterCollection has Options
		fc = fcs[2];
		testIName = "testOptionCollection";
		IName = fc.getInternalName();
		testDName = "A TEST OF Options";
		DName = fc.getDisplayName();
		Desc = fc.getDescription();
		testType = "list";
		Type = fc.getType();

		assertEquals("Warning, Internal Name not correctly set for FilterCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection\n", testDesc, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection\n", testType, Type);
		assertTrue("Third FilterCollection should have Options\n", fc.hasOptions());
		
		//	contains/get for FilterGroup-FilterCollection
		containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFiltersetCollection, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}		
		
		// Option data correct
		Option[] options = fc.getOptions();
		assertEquals("Warning, should get two Options in Third FilterCollection\n", 2, options.length);
		
		// first option does not contain options, and isSelectable is true
		Option option = options[0];
		testIName = "testOption";
		IName = option.getInternalName();
		testDName = "A Test Option";
		DName = option.getDisplayName();
		Desc = option.getDescription();
	  
		assertEquals("Warning, Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Option\n", testDesc, Desc);
		assertTrue("First Option " + option + " should be selectable\n", option.isSelectable());
		assertTrue("First Option " + option + "should not have Options\n", ! option.hasOptions());
		
		//contains/get for FilterCollection-Option
		containsTest = fc.containsOption(testIName);
		assertTrue("Warning, Third FilterCollection should contain testOption, but doesnt\n", containsTest);
		if(containsTest) {
			testGetByName = fc.getOptionByName(testIName).getInternalName();
			assertEquals("Warning, getOptionByName InternalName incorrect\n", testIName, testGetByName);		
		}
		
		// Second option contains one option, and isSelectable is false
		option = options[1];
		testIName = "testOptionWithOption";
		IName = option.getInternalName();
		testDName = "A Test Option With an Option";
		DName = option.getDisplayName();
		Desc = option.getDescription();
	  
		assertEquals("Warning, Internal Name not correctly set for Option\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Option\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Option\n", testDesc, Desc);
		assertTrue("Second Option" + option + " should not be selectable\n", ! option.isSelectable());
		assertTrue("Second Option" + option + " should have Options\n", option.hasOptions());
		
		//contains/get for FilterCollection-Option
		containsTest = fc.containsOption(testIName);
		assertTrue("Warning, Third FilterCollection should contain testOptionWithOption, but doesnt\n", containsTest);
		if(containsTest) {
			testGetByName = fc.getOptionByName(testIName).getInternalName();
			assertEquals("Warning, getOptionByName InternalName incorrect\n", testIName, testGetByName);		
		}

    Option[] subOptions = option.getOptions();
    assertEquals("Warning, second option should only contain one Option\n", 1, subOptions.length);
    
		// sub option does not contain options, and isSelectable is true
		Option suboption = subOptions[0];
		testIName = "testOptionInOption";
		IName = suboption.getInternalName();
		testDName = "A Test Option In an Option";
		DName = suboption.getDisplayName();
		Desc = suboption.getDescription();
	  
		assertEquals("Warning, Internal Name not correctly set for Sub Option\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Sub Option\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for Sub Option\n", testDesc, Desc);
		assertTrue("Sub Option should be selectable\n", suboption.isSelectable());
		assertTrue("Sub Option should not have Options\n", ! suboption.hasOptions());
		
		//contains/get for Option-Option
		containsTest = option.containsOption(testIName);
		assertTrue("Warning, Second Option should contain testOptionInOption, but doesnt\n", containsTest);
		if(containsTest) {
			testGetByName = option.getOptionByName(testIName).getInternalName();
			assertEquals("Warning, getOptionByName InternalName incorrect\n", testIName, testGetByName);		
		}
		
		//UIFilterDescription data correct
		fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should get two filter descriptions\n", 2, fs.size());

		// first FilterDescription of this FilterCollection is a UIFilterDescription, and has Options in first Option
		assertTrue("Warning, First FilterDescription in OptionFilterCollection should be a UIFilterDescription", fs.get(0) instanceof UIFilterDescription);		
		fField = (UIFilterDescription) fs.get(0);
		testIName = "OptionFilterDescription";
		IName = fField.getInternalName();
		testDName = "A FilterDescription With An Option";
		DName = fField.getDisplayName();
		Desc = fField.getDescription();
		Type = fField.getType();
		testFieldName = "test_id";
		FieldName = fField.getFieldName();
		testQualifier = "in";
		Qualifier = fField.getQualifier();
    String testOptionName = "testOption";
    String OptionName = fField.getOptionName();
    
		assertEquals("Warning, Internal Name not correctly set for UIFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescription\n", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescription\n", testType, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescription\n", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription\n", testQualifier, Qualifier);
		assertEquals("Warning, optionName not correctly set for UIFilterDescription\n", testOptionName, OptionName);
		assertTrue("Warning, Third FilterColletion should contain option refereced by UIFilterDescription but doesnt\n", fc.containsOption(OptionName) );
		
		//second FilterDescription is a UIDSFilterDescription, and has Options in Second Option
		assertTrue("Warning, second FilterDescription in optionFilterCollection should be a UIDSFilterDescription\n", fs.get(1) instanceof UIDSFilterDescription);
    dsf = (UIDSFilterDescription) fs.get(1);
		testIName = "optionUIDSFilterDescription";
		IName = dsf.getInternalName();
		testDName = "A TEST ID, DOESNT EXIST";
		DName = dsf.getDisplayName();
		Desc = dsf.getDescription();
		Type = dsf.getType();
		ObjectCode = dsf.getObjectCode();
		testOptionName = "testOptionWithOption";
		OptionName = dsf.getOptionName();
		
		assertEquals("Warning, Internal Name not correctly set for UIDSFilterDescription\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIDSFilterDescription\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIDSFilterDescription\n", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIDSFilterDescription\n", testType, Type);
		assertEquals("Warning, filterSetReq not set correctly for UIDSFilterDescriptionField\n", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, ObjectCode not set correctly for UIDSFilterDescription\n", testObjectCode, ObjectCode);
		assertEquals("Warning, optionName not set correctly for UIDSFilterDescription\n", testOptionName, OptionName);
		assertTrue("Warning, Third FilterColletion should contain option refereced by UIDSFilterDescription but doesnt\n", fc.containsOption(OptionName));
    
		// AttributePage data correct
		AttributePage[] aps = d.getAttributePages();
		assertEquals("Warning, should only get one filter page\n", 1, aps.length);

		AttributePage ap = aps[0];
		testIName = "testAttributePage";
		IName = ap.getInternalName();
		testDName = "Test of an Attribute Page";
		DName = ap.getDisplayName();
		Desc = ap.getDescription();

		assertEquals("Warning, Internal Name not correctly set for AttributePage\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributePage\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributePage\n", testDesc, Desc);

		// contains/get for Dataset-AttributePage
		containsTest = d.containsAttributePage(testIName);
		assertTrue("Warning, Dataset should contain testAttributePage, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = d.getAttributePageByName(testIName).getInternalName();
			assertEquals("Warning, getAttributePageByName InternalName incorrect\n", testIName, testGetByName);
		}

		//AttributeGroup data correct
		List ags = ap.getAttributeGroups();
		assertEquals("Warning, should get two AttributeGroup\n", 2, ags.size());

    //first AttributeGroup in the AttributePage is a DSAttributeGroup
    assertTrue("Warning, First AttributeGroup in the AttributePage should be a DSAttributeGroup", ags.get(0) instanceof DSAttributeGroup);

    DSAttributeGroup dsag = (DSAttributeGroup) ags.get(0);
		testIName = "testDSAttributeGroup";
		IName = dsag.getInternalName();
		testDName = "test of an DSAttribute Group:";
		DName = dsag.getDisplayName();
		Desc = dsag.getDescription();
		ObjectCode = dsag.getObjectCode();
    
		assertEquals("Warning, Internal Name not correctly set for DSFilterGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for DSFilterGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for DSFilterGroup\n", testDesc, Desc);
		assertEquals("Warning, ObjectCode not set correctly for DSFilterGroup\n", testObjectCode, ObjectCode);
		    
    //second AttributeGroup in the AttributePage is an AttributeGroup, with everything in it
		assertTrue("Warning, Second AttributeGroup in the AttributePage should be an AttributeGroup", ags.get(1) instanceof AttributeGroup);
		
		AttributeGroup ag = (AttributeGroup) ags.get(1);
		testIName = "testAttributeGroup";
		IName = ag.getInternalName();
		testDName = "test of an Attribute Group:";
		DName = ag.getDisplayName();
		Desc = ag.getDescription();

		assertEquals("Warning, Internal Name not correctly set for AttributeGroup\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributeGroup\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributeGroup\n", testDesc, Desc);

		// contains/get for AttributePage-AttributeGroup
		containsTest = ap.containsAttributeGroup(testIName);
		assertTrue("Warning, AttributePage should contain testAttributeGroup, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (AttributeGroup) ap.getAttributeGroupByName(testIName) ).getInternalName();
			assertEquals("Warning, getAttributeGroupByName InternalName incorrect\n", testIName, testGetByName);
		}

		//AttributeCollection data correct
		AttributeCollection[] acs = ag.getAttributeCollections();
		assertEquals("Warning, should only get one attribute collection\n", 1, acs.length);

		AttributeCollection ac = acs[0];
		testIName = "testAttributeCollection";
		IName = ac.getInternalName();
		testDName = "Test of an AttributeCollection:";
		DName = ac.getDisplayName();
		Desc = ac.getDescription();
		int testMaxSelect = 1;
		int MaxSelect = ac.getMaxSelect();

		assertEquals("Warning, Internal Name not correctly set for AttributeCollection\n", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributeCollection\n", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributeCollection\n", testDesc, Desc);
		assertEquals("Warning, Max Select not correctly set for AttributeCollection\n", testMaxSelect, MaxSelect);

		//	contains/get for AttributeGroup-AttributeCollection
		containsTest = ag.containsAttributeCollection(testIName);
		assertTrue("Warning, AttributeGroup should contain testAttributeCollection, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ag.getAttributeCollectionByName(testIName).getInternalName();
			assertEquals("Warning, getAttributeCollectionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//UIAttributeDescription data correct
		List as = ac.getUIAttributeDescriptions();
		assertEquals("Warning, should get one attribute description\n", 1, as.size());

    // first AttributeDescription is a UIAttributeDescription
		assertTrue("First AttributeDescription should be a UIAttributeDescription", as.get(0) instanceof UIAttributeDescription);
		
		UIAttributeDescription a = (UIAttributeDescription) as.get(0);
		testIName = "testUIAttributeDescription";
		IName = a.getInternalName();
		testDName = "Test of a UIAttributeDescription";
		DName = a.getDisplayName();
		Desc = a.getDescription();
		testFieldName = "test_id";
		FieldName = a.getFieldName();
		testTableConstraint = "gene_main";
		TableConstraint = a.getTableConstraint();
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
		assertEquals("Warning, Description not correctly set for UIAttributeDescription\n", testDesc, Desc);
		assertEquals("Warning, FieldName not correctly set for UIAttributeDescription\n", testFieldName, FieldName);
		assertEquals("Warning, TableConstraint not correctly set for UIAttributeDescription\n", testTableConstraint, TableConstraint);
		assertEquals("Warning, MaxLength not correctly set for UIAttributeDescription\n", testMaxLength, MaxLength);
		assertEquals("Warning, Source not correctly set for UIAttributeDescription\n", testSource, Source);
		assertEquals("Warning, HomepageURL not correctly set for UIAttributeDescription\n", testHPage, HPage);
		assertEquals("Warning, LinkoutURL not correctly set for UIAttributeDescription\n", testLPage, LPage);

		//	contains/get for AttributeCollection-UIAttributeDescription
		containsTest = ac.containsUIAttributeDescription(testIName);
		assertTrue("Warning, AttributeCollection should contain testUIAttributeDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (UIAttributeDescription) ac.getUIAttributeDescriptionByName(testIName) ).getInternalName();
			assertEquals("Warning, getUIAttributeDescriptionByName InternalName incorrect\n", testIName, testGetByName);
		}

		//	contains/get for AttributePage-UIAttributeDescription (Tests all lower groups getByName as well
		containsTest = ap.containsUIAttributeDescription(testIName);
		assertTrue("Warning, AttributePage should contain testUIAttributeDescription, but doesnt\n", containsTest);
		if (containsTest) {
			testGetByName = ( (UIAttributeDescription) ap.getUIAttributeDescriptionByName(testIName) ).getInternalName();
			assertEquals("Warning, getUIAttributeDescriptionByName InternalName incorrect\n", testIName, testGetByName);

			//test getPageFor functionality as well
			assertEquals(
				"Warning, Did not get the correct Page for the UIAttributeDescription\n",
				"testAttributePage",
				d.getPageForUIAttributeDescription(testIName).getInternalName());
		}
	}

    public void testConfFile() throws Exception {    	
		String confFile = "data/xmltest/test_file.xml";
		URL confURL = ClassLoader.getSystemResource(confFile);
		MartConfiguration martconf = new MartConfigurationFactory().getInstance(confURL);
		
		String testMartName = "test_file";
		String martName = martconf.getInternalName();
		assertEquals("martName from file "+confURL.toString()+" isnt correct", testMartName, martName);
    }
    
	public final String xmlTestID = "test.xml";
}
