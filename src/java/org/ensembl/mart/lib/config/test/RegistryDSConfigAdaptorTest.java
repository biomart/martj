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

import java.net.URL;
import java.sql.Connection;

import org.ensembl.mart.lib.config.DatabaseDSConfigAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetConfigUtils;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.DatasetConfigIterator;
import org.ensembl.mart.lib.config.MartRegistry;
import org.ensembl.mart.lib.config.RegistryDSConfigAdaptor;
import org.ensembl.mart.lib.config.URLDSConfigAdaptor;
import org.ensembl.mart.lib.test.Base;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class RegistryDSConfigAdaptorTest extends Base {

  
  private final String TESTMARTREGISTRYFILE = Base.UNITTESTDIR + "/testMartRegistryFile.xml";
  private final String TESTDBDSVIEW = Base.UNITTESTDIR + "/testDatasetConfigRegDB.xml";
  private final String TESTMARTREGISTRYDB = Base.UNITTESTDIR + "/testMartRegistryDB.xml";
  private final String TESTMARTREGISTRYREGISTRY = Base.UNITTESTDIR + "/testMartRegistryRegistry.xml";
  //private final String TESTMARTREGISTRYDUP = Base.UNITTESTDIR + "/testMartRegistryRegistryDuplicate.xml";
  private final String TESTMARTREGISTRYCOMPOSITE = Base.UNITTESTDIR + "/testMartRegistryComposite.xml";

  private final String TESTMARTREGDBINAME = "test_datasetview_db";
  
  
  private DatabaseDatasetConfigUtils dbutils;
  
  private URL getURL(String path) throws Exception {
    URL url = RegistryDSConfigAdaptor.class.getClassLoader().getResource(path);
    assertNotNull("Missing file " + path + "\n", url);
    return url;
  }
  
  public void testAll() throws Exception {
    //TODO: major refactor
    //empty
    RegistryDSConfigAdaptor regadaptor = new RegistryDSConfigAdaptor(false, true, true);
    assertNotNull("Empty RegistryDSConfigAdaptor should not be null\n", regadaptor);
    
    //set URL to load File, just to test if it has a DatasetConfig
    regadaptor.setRegistryURL(getURL(TESTMARTREGISTRYFILE));
    
    assertEquals("File RegistryDSConfigAdaptor should have 1 DatasetConfig after setRegistryURL on empty\n", 1, regadaptor.getNumDatasetConfigs());
    
//    //testMartRegistryFile
//    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYFILE), false, true, true);
//    //assertEquals("RECIEVED WRONG DatasetConfig internalName for File RegistryDSConfigAdaptor\n", TESTMARTREGFILEINAME, regadaptor.getDatasetInternalNames()[0]);
    
//    RegistryDSConfigAdaptor filebak = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYFILE)); // to test dup
//    
//    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYDB), false, true, true);
//    //assertEquals("DB RegistryDSConfigAdaptor should be empty before store and update\n", 0, regadaptor.getDatasetInternalNames().length);
//    DatasetConfig dbdsview = (DatasetConfig) new URLDSConfigAdaptor(getURL(TESTDBDSVIEW), false, true, true).getDatasetConfigs().next();
//    DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, DatabaseDSConfigAdaptorTest.USER, dbdsview, true);
//    
//    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYDB), false, true, true);
//    
//    //assertEquals("DB RegistryDSConfigAdaptor should have 1 DatasetConfig after store, recreate\n", 1, regadaptor.getDatasetDisplayNames().length);
//    //assertEquals("DB RegistryDSConfigAdaptor Dataset internalName is incorrect\n", TESTMARTREGDBINAME, ((DatasetConfig) regadaptor.getDatasetConfigs().next()).getInternalName());
//    
//    //testMartRegistryRegistry
//    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYREGISTRY), false, true, true);
//    //assertEquals("Registry RegistryDSConfigAdaptor should have 1 DatasetConfig\n", 1, regadaptor.getNumDatasetConfigs());
//    //assertEquals("Registry RegistryDSConfigAdaptor Dataset internalName is incorrect\n", TESTMARTREGISTRYREGINAME, regadaptor.getDatasetInternalNames()[0]);
//    
//
////TODO: our duplicate adaptor management became more complex with the addition of user defined adaptor names, need to rethink how to determine sameness of adaptors
//      
////    //testMartRegistryRegistryDup alone
////    
////    //just for this test, set the adaptorName of testMartRegistryDup equal to that for filebak
////    regadaptor.setName(filebak.getName());
////     
////    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYDUP));
////    assertEquals("Dup RegistryDSConfigAdaptor should equal File RegistryDSConfigAdaptor\n", filebak, regadaptor);
////    
////    //testMartRegistryRegistryDup.add(testMartRegistryFile)
////    regadaptor.add(filebak);
////    assertEquals("Dup RegistryDSConfigAdaptor should equal File RegistryDSConfigAdaptor after add(filebak)\n", filebak, regadaptor);
//    
//    //testMartRegistryComposite
//    regadaptor = new RegistryDSConfigAdaptor( getURL( TESTMARTREGISTRYCOMPOSITE ), false, true, true );
//    
//    assertEquals("Composite RegistryDSConfigAdaptor should have 3 DatasetConfigs\n", 3, regadaptor.getNumDatasetConfigs());
//    
//    //getMartRegistry
//    MartRegistry mr = regadaptor.getMartRegistry();
//    assertEquals("MartRegistry from Composite RegistryDSConfigAdaptor should have 3 MartLocations\n", 3, mr.getMartLocations().length);
//    
//    //Store new datasetView to DB, and update
//    DatasetConfigIterator dsvi = URLDSConfigAdaptorTest.getSampleDSConfigAdaptor().getDatasetConfigs();
//    assertTrue("URLDSConfigAdaptor should have one DatasetConfig\n", dsvi.hasNext());
//    DatasetConfig newDatasetConfig = (DatasetConfig) dsvi.next();
//    DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, DatabaseDSConfigAdaptorTest.USER, newDatasetConfig, true);
//    
//    regadaptor.update();
//    assertEquals("Composite RegistryDSConfigAdaptor should contain 4 DatasetConfigs after store and update\n", 4, regadaptor.getNumDatasetConfigs());

//TODO: sameness issue    
//    // new registry, no URL
//    RegistryDSConfigAdaptor regreg = new RegistryDSConfigAdaptor(mr);
//    
//    boolean testequals = regadaptor.equals(regreg);
//    assertTrue("Non URL MartRegistry based RegistryDSConfigAdaptor should equal Composite RegistryDSConfigAdaptor\n", testequals);
//    
//    //new registry, + URL should be equal to previous
//    RegistryDSConfigAdaptor regurl = new RegistryDSConfigAdaptor(mr, getURL(TESTMARTREGISTRYDB));  //URL is different, but underlying MartRegistry should be the same
//    assertEquals("MartRegistry+URL RegistryDSConfigAdaptor should equal Composite RegistryDSConfigAdaptor\n", regadaptor, regurl);
//    assertEquals("MartRegistry+URL RegistryDSConfigAdaptor should equal Non URL RegistryDSConfigAdaptor\n", regreg, regurl); 
//    
//    //get rid of stored DatasetConfig on test user table
//    dbutils.deleteOldDSConfigEntriesFor(dbutils.getDSConfigTableFor(DatabaseDSConfigAdaptorTest.USER), newDatasetConfig.getDataset(), newDatasetConfig.getInternalName(), newDatasetConfig.getDisplayName());
//
////  TODO: sameness issue    
////    //storeMartRegistry and retrieve
////    File testFile = new File(TESTFILEPATH);
////    if (testFile.exists())
////      testFile.delete();
////    
////    RegistryDSConfigAdaptor.StoreMartRegistry(mr, testFile);
////    assertTrue("TestFile " + TESTFILEPATH + " should exist after storeMartRegistry\n", testFile.exists());
////    
////    RegistryDSConfigAdaptor inReg = new RegistryDSConfigAdaptor(getURL(TESTFILEPATH));
////    assertEquals("Input File Registry after Store should equal Composite RegistryDSConfigAdaptor\n", regadaptor, inReg);
////    
////    testFile.delete();
//    
//    //new Registry(URLDSConfigAdaptor)
//    URLDSConfigAdaptor urladaptor = URLDSConfigAdaptorTest.getSampleDSConfigAdaptor();
//    RegistryDSConfigAdaptor urlReg = new RegistryDSConfigAdaptor(urladaptor);
////    assertTrue("URLDSConfigAdaptor RegistryDSConfigAdaptor should support urladaptors DatasetConfig\n", urlReg.supportsInternalName(urladaptor.getDatasetInternalNames()[0]));
//
////  TODO: sameness issue    
////    //new Registry(URLDSConfigAdaptor, url) should be equal to previous
////    RegistryDSConfigAdaptor urlRegURL = new RegistryDSConfigAdaptor(urladaptor, getURL(TESTMARTREGISTRYCOMPOSITE)); // different URL, same DatasetConfig
////    assertEquals("URLDSConfigAdaptor + URL RegistryDSConfigAdaptor should equal non URL URLDSConfigAdaptor RegistryDSConfigAdaptor\n", urlReg, urlRegURL);
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();

    dbutils = new DatabaseDatasetConfigUtils(DatasetConfigXMLUtilsTest.DEFAULTUTILS, martJDataSource);
//    assertTrue(
//      "Could not create test meta_DatasetConfig table\n"
//        + " please ensure that "
//        + Base.MARTJ_DB_CONFIG_URL
//        + " specifies connection parameters for a user with write access and that the meta_datasetconfig tables exist\n",
//      DatabaseDSConfigAdaptorTest.hasWriteAccessAndTestMetaTable(martJDataSource));
  }


  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
//    DatabaseDSConfigAdaptorTest.dropTestConfigTable(martJDataSource);
    
    super.tearDown();
  }
  
  /**
   * Constructor for RegistryDSConfigAdaptorTest.
   * @param arg0
   */
  public RegistryDSConfigAdaptorTest(String arg0) {
    super(arg0);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(RegistryDSConfigAdaptorTest.class);
  }
}
