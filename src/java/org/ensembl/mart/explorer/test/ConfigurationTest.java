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

package org.ensembl.mart.explorer.test;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ensembl.mart.explorer.config.AttributeCollection;
import org.ensembl.mart.explorer.config.AttributeGroup;
import org.ensembl.mart.explorer.config.AttributePage;
import org.ensembl.mart.explorer.config.Dataset;
import org.ensembl.mart.explorer.config.FilterCollection;
import org.ensembl.mart.explorer.config.FilterGroup;
import org.ensembl.mart.explorer.config.FilterPage;
import org.ensembl.mart.explorer.config.FilterSet;
import org.ensembl.mart.explorer.config.FilterSetDescription;
import org.ensembl.mart.explorer.config.MartConfiguration;
import org.ensembl.mart.explorer.config.MartDTDEntityResolver;
import org.ensembl.mart.explorer.config.MartXMLutils;
import org.ensembl.mart.explorer.config.UIAttributeDescription;
import org.ensembl.mart.explorer.config.UIFilterDescription;
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
		newbuilder.setEntityResolver(new MartDTDEntityResolver(conn)); // set the EntityResolver to a mart DB aware version, allowing it to get the DTD from the DB.
		
		Document newdoc = newbuilder.build(MartXMLutils.getInputSourceFor(conn, xmlTestID));
		
		out = new ByteArrayOutputStream();
		xout.output(newdoc, out);
			
		String newxml = out.toString();
		out.close();
		
		assertEquals("Warning, initial xml does not match final xml after a roundtrip.", initxml, newxml);
	}
	
	public void testMartConfiguration() throws Exception {
	  MartConfiguration martconf = engine.getMartConfiguration(xmlTestID);
	  
	  //Mart Data Correct
	  String testIName = "ensembl_mart_14_1";
	  String IName = martconf.getInternalName();
	  String testDName = "Ensembl Mart version 14.1";
	  String DName = martconf.getDisplayName();
	  String testDesc = "First Mart Created from version 14 Ensembl Datasources";
	  String Desc = martconf.getDescription();
	  
	  assertEquals("Warning, MartName not correctly set for MartConfiguration", testIName, IName);
	  assertEquals("Warning Mart Display Name not correctly set for MartConfiguration", testDName, DName);
	  assertEquals("Warning Mart Description not correctly set for MartConfiguration", testDesc, Desc);
	  
	  // Dataset Data Correct
	  Dataset[] ds = martconf.getDatasets();
	  assertEquals("Warning, should only be one dataset, got "+ds.length, 1, ds.length);
	  
	  Dataset d = ds[0];
	  testIName = "test_dataset";
	  IName = d.getInternalName();
	  testDName = "Test of a Dataset";
	  DName = d.getDisplayName();
	  testDesc = "For Testing Purposes Only";
	  Desc = d.getDescription();
	   
		assertEquals("Warning, Internal Name not correctly set for Dataset", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for Dataset", testDName, DName);
		assertEquals("Warning, Description not correctly set for Dataset", testDesc, Desc);
		
		//contains/get for MartConfiguration-Dataset
		boolean containsTest = martconf.containsDataset(testIName);
		String testGetByName = null;
		
		assertTrue("Warning, MartConfiguration should contain test_dataset, but doesnt", containsTest);
		if (containsTest) {
		  testGetByName = martconf.getDatasetByName(testIName).getInternalName();
		  assertEquals("Warning, getDatasetByName InternalName incorrect", testIName, testGetByName);
		}
		
		String[] sbs = d.getStarBases();
		assertEquals("Warning, should only get one starbase", 1, sbs.length);
		assertEquals("Warning, didnt get the expected starbase", "test_starbase", sbs[0]);
		
		String[] pks = d.getPrimaryKeys();
		assertEquals("Warning, should only get one primary key", 1, pks.length);
		assertEquals("Warning, didnt get the expected primary key", "test_primaryKey", pks[0]);
		
		//FilterPage data correct
		FilterPage[] fps = d.getFilterPages();
		assertEquals("Warning, should only get one filter page", 1, fps.length);
		
		FilterPage fp = fps[0];
		testIName = "testFilterPage";
		IName = fp.getInternalName();
		testDName = "Test A Filter Page";
		DName = fp.getDisplayName();
		Desc = fp.getDescription();
		
		assertEquals("Warning, Internal Name not correctly set for FilterPage", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterPage", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterPage", testDesc, Desc);
		
		// contains/get for Dataset-FilterPage
		containsTest = d.containsFilterPage(testIName);
		assertTrue("Warning, Dataset should contain testFilterPage, but doesnt", containsTest);
		if (containsTest) {		
		  testGetByName = d.getFilterPageByName(testIName).getInternalName();
		  assertEquals("Warning, getFilterPageByName InternalName incorrect", testIName, testGetByName);
		}
		  
		//FilterGroup data correct
		FilterGroup[] fgs = fp.getFilterGroups();
		assertEquals("Warning, should only get one filterGroup", 1, fgs.length);
		
		FilterGroup fg = fgs[0];
		testIName = "testFilterGroup";
		IName = fg.getInternalName();
		testDName = "Test A FilterGroup:";
		DName = fg.getDisplayName();
		Desc = fg.getDescription();
		
		assertEquals("Warning, Internal Name not correctly set for FilterGroup", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterGroup", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterGroup", testDesc, Desc);
		
		// contains/get for FilterPage-FilterGroup
		containsTest = fp.containsFilterGroup(testIName);
		assertTrue("Warning, FilterPage should contain testFilterGroup, but doesnt", containsTest);
		if (containsTest)		{
			testGetByName = fp.getFilterGroupByName(testIName).getInternalName();
		  assertEquals("Warning, getFilterGroupByName InternalName incorrect", testIName, testGetByName);
		}
		
		//FilterSet data correct
		assertTrue("Warning, FilterGroup should contain a FilterSet", fg.hasFilterSets());
		FilterSet[] fsets = fg.getFilterSets();
		assertEquals("Warning, should only get one FilterSet", 1, fsets.length);
		
		FilterSet fset = fsets[0];
		testIName = "testFilterSet";
		IName = fset.getInternalName();
		String fsetName = IName; // use during test of FilterSet Functionality
		testDName = "Test of a Filter Set";
		DName = fset.getDisplayName();
		Desc = fset.getDescription();
		String testType = "radio";
		String Type = fset.getType();
		
		assertEquals("Warning, Internal Name not correctly set for FilterSet", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterSet", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterSet", testDesc, Desc);
		assertEquals("Warning, Type not correctly set for FilterSet", testType, Type);		
		
	    //	contains/get for FilterGroup-FilterSet
	    containsTest = fg.containsFilterSet(testIName);
		assertTrue("Warning, FilterGroup should contain testFilterSet, but doesnt", containsTest);
		if (containsTest)		{
		  testGetByName = fg.getFilterSetByName(testIName).getInternalName();
		  assertEquals("Warning, getFilterSetByName InternalName incorrect", testIName, testGetByName);
		}
		
		//FilterSetDescription data correct
		FilterSetDescription[] fsds = fset.getFilterSetDescriptions();
		assertEquals("Warning, FilterSet should contain only one FilterSetDescription", 1, fsds.length);
		
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
		
		assertEquals("Warning, Internal Name not correctly set for FilterSetDescription", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterSetDescription", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterSetDescription", testDesc, Desc);
		assertEquals("Warning, tableConstraintModifier not set correctly for FilterSetDescription", testTableConstraintModifier, TableConstraintModifier);
		assertEquals("Warning, fieldNameModifier not set correctly for FilterSetDescription", testFieldNameModifier, FieldNameModifier);
		
		//contains/get for FIlterSet-FilterSetDescription
		containsTest = fset.containsFilterSetDescription(testIName);
		assertTrue("Warning, FilterSet should contain testFilterSetDescription, but doesnt", containsTest);
		if (containsTest) {
			testGetByName = fset.getFilterSetDescriptionByName(testIName).getInternalName();
			assertEquals("Warning, getFilterSetDescriptionByName internalName incorrect", testIName, testGetByName);
		}
					 		
		//FilterCollection data correct
		FilterCollection[] fcs = fg.getFilterCollections();
		assertEquals("Warning, should get two filter collections", 2, fcs.length);
		
		// first FilterCollection is not in a FilterSet
		FilterCollection fc = fcs[0];
		testIName = "testFilterCollection";
		IName = fc.getInternalName();
		testDName = "Test of a FilterCollection";
		DName = fc.getDisplayName();
		Desc = fc.getDescription();
        testType = "list";
        Type = fc.getType();
    		
		assertEquals("Warning, Internal Name not correctly set for FilterCollection", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection", testDesc, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection", testType, Type);
		assertTrue("First FilterCollection should not be in a FilterSet", ! fc.inFilterSet());
		
    //	contains/get for FilterGroup-FilterCollection
		containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFilterCollection, but doesnt", containsTest);
		if (containsTest)		{
		 testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
		 assertEquals("Warning, getFilterCollectionByName InternalName incorrect", testIName, testGetByName);
		}
		
		//UIFilterDescription data correct
		UIFilterDescription[] fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should only get one filter description with first FilterCollection", 1, fs.length);
		
		UIFilterDescription f = fs[0];
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
		
		assertEquals("Warning, Internal Name not correctly set for UIFilterDescription", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescription", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescription", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescription", testType, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescription", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription", testQualifier, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescription", testTableConstraint, TableConstraint);
		assertTrue("Warning, first FilterCollections UIFilterDescription should not be in a FilterSet", ! f.inFilterSet());
		
		//	contains/get for FilterCollection-UIFilterDescription
		containsTest = fc.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterCollection should contain testUIFilterDescription, but doesnt", containsTest);
		if (containsTest)		{
		 testGetByName = fc.getUIFilterDescriptionByName(testIName).getInternalName();
		 assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect", testIName, testGetByName);
		}
		
		//	contains/get for FilterPage-UIFilterDescription (Tests all lower groups getByName as well
		containsTest = fp.containsUIFilterDescription(testIName);
		assertTrue("Warning, FilterPage should contain testUIFilterDescription, but doesnt", containsTest);
		if (containsTest)		{
		 testGetByName = fp.getUIFilterDescriptionByName(testIName).getInternalName();
		 assertEquals("Warning, getUIFilterDescriptionByName InternalName incorrect", testIName, testGetByName);
		 
		 //test getPageFor functionality as well
		 assertEquals("Warning, Did not get the correct Page for the UIFilterDescription", "testFilterPage", d.getPageForUIFilterDescription(testIName).getInternalName());
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
    		
		assertEquals("Warning, Internal Name not correctly set for FilterCollection", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for FilterCollection", testDName, DName);
		assertEquals("Warning, Description not correctly set for FilterCollection", testDesc, Desc);
		assertEquals("Warning, Type not correctly set for FilterCollection", testType, Type);
		assertEquals("Warning, FilterSetName not correctly set for FilterCollection", testFilterSetName, FilterSetName);
		assertTrue("First FilterCollection should not be in a FilterSet", ! fc.inFilterSet());
		
	    //	contains/get for FilterGroup-FilterCollection
		containsTest = fg.containsFilterCollection(testIName);
		assertTrue("Warning, FilterGroup should contain testFiltersetCollection, but doesnt", containsTest);
		if (containsTest)		{
		 testGetByName = fg.getFilterCollectionByName(testIName).getInternalName();
		 assertEquals("Warning, getFilterCollectionByName InternalName incorrect", testIName, testGetByName);
		}
		
		//UIFilterDescription data correct
		fs = fc.getUIFilterDescriptions();
		assertEquals("Warning, should get two filter descriptions", 2, fs.length);

        // first UIFilterDescription of this Collection is part of the FilterSet, and requires a fieldNameModifier		
		UIFilterDescription fField = fs[0];
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
		int testFilterSetReq = 1;
		int FilterSetReq = fField.getFilterSetReq();
		String testModifiedFieldName = "ensemblsyn_exclusive";
		String ModifiedFieldName = fsd.getFieldNameModifier()+fField.getFieldName();
		
		assertEquals("Warning, Internal Name not correctly set for UIFilterDescriptionField", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescriptionField", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescriptionField", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescriptionField", testType, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescriptionField", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescriptionField", testQualifier, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescriptionField", testTableConstraint, TableConstraint);
		assertEquals("Warning, filterSetReq not set correctly for UIFilterDescriptionField", testFilterSetReq, FilterSetReq);
		assertEquals("Warning, modified Field Name not correct",testModifiedFieldName, ModifiedFieldName);
		assertTrue("Warning, second FilterCollections UIFilterDescriptionField should be in a FilterSet", fField.inFilterSet());		
		
		//second UIFilterDescription of this FilterCollection is part of the FilterSet, and requires a tableConstraintModifier
		UIFilterDescription fTable= fs[1];
		testIName = "filterSetUIFilterDescriptionTable";
		IName = fTable.getInternalName();
		testDName = "A TEST TABLE MODIFIER";
		DName = fTable.getDisplayName(); 
		Desc = fTable.getDescription();
		Type = fTable.getType(); 
		 testFieldName = "gene_stable_id_v";
		 FieldName = fTable.getFieldName();
         Qualifier = fTable.getQualifier();
		 testFilterSetReq = 2;
		 FilterSetReq = fTable.getFilterSetReq();
		 testTableConstraint = "_dm";
		 TableConstraint = fTable.getTableConstraint();
		 String testModifiedTableName = "ensemblgene_dm";
		 String ModifiedTableName = fsd.getTableConstraintModifier()+fTable.getTableConstraint();
		
		assertEquals("Warning, Internal Name not correctly set for UIFilterDescription", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIFilterDescription", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIFilterDescription", testDesc, Desc);
		assertEquals("Warning, Type not set correctly for UIFilterDescription", testType, Type);
		assertEquals("Warning, FieldName not set correctly for UIFilterDescription", testFieldName, FieldName);
		assertEquals("Warning, Qualifier not set correctly for UIFitlerDescription", testQualifier, Qualifier);
		assertEquals("Warning, TableConstraint not set correctly for UIFilterDescription", testTableConstraint, TableConstraint);
		assertEquals("Warning, Modified TableConstraint not correct", testModifiedTableName, ModifiedTableName);
		assertTrue("Warning, second FilterCollection second UIFilterDescription should be in a FilterSet", fTable.inFilterSet());
		
		// AttributePage data correct
		AttributePage[] aps = d.getAttributePages();
		assertEquals("Warning, should only get one filter page", 1, aps.length);
		
		AttributePage ap = aps[0];
		testIName = "testAttributePage";
		IName = ap.getInternalName();
		testDName = "Test of an Attribute Page";
		DName = ap.getDisplayName();
		Desc = ap.getDescription();
		
		assertEquals("Warning, Internal Name not correctly set for AttributePage", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributePage", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributePage", testDesc, Desc);
		
		// contains/get for Dataset-AttributePage
		containsTest = d.containsAttributePage(testIName);
		assertTrue("Warning, Dataset should contain testAttributePage, but doesnt", containsTest);
		if (containsTest) {		
			testGetByName = d.getAttributePageByName(testIName).getInternalName();
			assertEquals("Warning, getAttributePageByName InternalName incorrect", testIName, testGetByName);
		}
		  
		//AttributeGroup data correct
		AttributeGroup[] ags = ap.getAttributeGroups();
		assertEquals("Warning, should only get one AttributeGroup", 1, ags.length);
		
		AttributeGroup ag = ags[0];
		testIName = "testAttributeGroup";
		IName = ag.getInternalName();
		testDName = "test of an Attribute Group:";
		DName = ag.getDisplayName();
		Desc = ag.getDescription();
		
		assertEquals("Warning, Internal Name not correctly set for AttributeGroup", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributeGroup", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributeGroup", testDesc, Desc);
		
		// contains/get for AttributePage-AttributeGroup
		containsTest = ap.containsAttributeGroup(testIName);
		assertTrue("Warning, AttributePage should contain testAttributeGroup, but doesnt", containsTest);
		if (containsTest)		{
			testGetByName = ap.getAttributeGroupByName(testIName).getInternalName();
			assertEquals("Warning, getAttributeGroupByName InternalName incorrect", testIName, testGetByName);
		}
		
		//AttributeCollection data correct
		AttributeCollection[] acs = ag.getAttributeCollections();
		assertEquals("Warning, should only get one attribute collection", 1, acs.length);
		
		AttributeCollection ac = acs[0];
		testIName = "testAttributeCollection";
		IName = ac.getInternalName();
		testDName = "Test of an AttributeCollection:";
		DName = ac.getDisplayName();
		Desc = ac.getDescription();
		int testMaxSelect = 1;
		int MaxSelect = ac.getMaxSelect();
		
		assertEquals("Warning, Internal Name not correctly set for AttributeCollection", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for AttributeCollection", testDName, DName);
		assertEquals("Warning, Description not correctly set for AttributeCollection", testDesc, Desc);
		assertEquals("Warning, Max Select not correctly set for AttributeCollection", testMaxSelect, MaxSelect);
		
		//	contains/get for AttributeGroup-AttributeCollection
		containsTest = ag.containsAttributeCollection(testIName);
		assertTrue("Warning, AttributeGroup should contain testAttributeCollection, but doesnt", containsTest);
		if (containsTest)		{
		 testGetByName = ag.getAttributeCollectionByName(testIName).getInternalName();
		 assertEquals("Warning, getAttributeCollectionByName InternalName incorrect", testIName, testGetByName);
		}
		
		//UIAttributeDescription data correct
		UIAttributeDescription[] as = ac.getUIAttributeDescriptions();
		assertEquals("Warning, should only get one attribute description", 1, as.length);
		
		UIAttributeDescription a = as[0];
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
		String HPage= a.getHomePageURL();
		String testLPage = "http://test.org?test";
		String LPage = a.getLinkoutURL();
		
		assertEquals("Warning, Internal Name not correctly set for UIAttributeDescription", testIName, IName);
		assertEquals("Warning, Display Name not correctly set for UIAttributeDescription", testDName, DName);
		assertEquals("Warning, Description not correctly set for UIAttributeDescription", testDesc, Desc);
		assertEquals("Warning, FieldName not correctly set for UIAttributeDescription", testFieldName, FieldName);
		assertEquals("Warning, TableConstraint not correctly set for UIAttributeDescription", testTableConstraint, TableConstraint);
		assertEquals("Warning, MaxLength not correctly set for UIAttributeDescription", testMaxLength, MaxLength);
		assertEquals("Warning, Source not correctly set for UIAttributeDescription", testSource, Source);
		assertEquals("Warning, HomepageURL not correctly set for UIAttributeDescription", testHPage, HPage);
		assertEquals("Warning, LinkoutURL not correctly set for UIAttributeDescription", testLPage, LPage);
		
		//	contains/get for AttributeCollection-UIAttributeDescription
		containsTest = ac.containsUIAttributeDescription(testIName);
		assertTrue("Warning, AttributeCollection should contain testUIAttributeDescription, but doesnt", containsTest);
		if (containsTest)		{
		 testGetByName = ac.getUIAttributeDescriptionByName(testIName).getInternalName();
		 assertEquals("Warning, getUIAttributeDescriptionByName InternalName incorrect", testIName, testGetByName);
		}
		
		//	contains/get for AttributePage-UIAttributeDescription (Tests all lower groups getByName as well
		containsTest = ap.containsUIAttributeDescription(testIName);
		assertTrue("Warning, AttributePage should contain testUIAttributeDescription, but doesnt", containsTest);
		if (containsTest)		{
		 testGetByName = ap.getUIAttributeDescriptionByName(testIName).getInternalName();
		 assertEquals("Warning, getUIAttributeDescriptionByName InternalName incorrect", testIName, testGetByName);
		 
		 //test getPageFor functionality as well
		 assertEquals("Warning, Did not get the correct Page for the UIAttributeDescription", "testAttributePage", d.getPageForUIAttributeDescription(testIName).getInternalName());
		}
	}
	
	public final String xmlTestID = "test.xml";
}
