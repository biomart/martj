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

import java.io.File;
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

  private final String TESTFILEPATH = "RegistryDSConfigAdaptorTestFile.xml";
  
  private final String TESTMARTREGISTRYFILE = "data/XML/testMartRegistryFile.xml";
  private final String TESTMARTREGFILEINAME = "test_datasetview_registry_file";
  
  private final String TESTMARTREGISTRYDB = "data/XML/testMartRegistryDB.xml";
  private final String TESTMARTREGDBINAME = "test_datasetview_db";
  private final String TESTDBDSVIEW = "data/XML/testDatasetConfigRegDB.xml";
  private final String USER = "ensro";
  
  private final String TESTMARTREGISTRYREGISTRY = "data/XML/testMartRegistryRegistry.xml";
  private final String TESTMARTREGISTRYREGINAME = "test_dataset_registry";
  
//  private final String TESTMARTREGISTRYDUP = "data/XML/testMartRegistryRegistryDuplicate.xml";
  private final String TESTMARTREGISTRYCOMPOSITE = "data/XML/testMartRegistryComposite.xml";
  
  private URL getURL(String path) throws Exception {
    URL url = RegistryDSConfigAdaptor.class.getClassLoader().getResource(path);
    assertNotNull("Missing file " + path + "\n", url);
    return url;
  }
  
  public void testAll() throws Exception {
    //TODO: major refactor
    //empty
    RegistryDSConfigAdaptor regadaptor = new RegistryDSConfigAdaptor();
    assertNotNull("Empty RegistryDSConfigAdaptor should not be null\n", regadaptor);
    
    //set URL to load File, just to test if it has a DatasetConfig
    regadaptor.setRegistryURL(getURL(TESTMARTREGISTRYFILE));
    
    assertEquals("File RegistryDSConfigAdaptor should have 1 DatasetConfig after setRegistryURL on empty\n", 1, regadaptor.getNumDatasetConfigs());
    
    //testMartRegistryFile
    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYFILE));
    //assertEquals("RECIEVED WRONG DatasetConfig internalName for File RegistryDSConfigAdaptor\n", TESTMARTREGFILEINAME, regadaptor.getDatasetInternalNames()[0]);
    
//    RegistryDSConfigAdaptor filebak = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYFILE)); // to test dup
    
    //testMartRegistryDB
    assertTrue("meta_DatasetConfig_ensro does not exist, must exist for test to run\n", DatabaseDatasetConfigUtils.DSConfigUserTableExists(martJDataSource, USER));
    
    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYDB));
    //assertEquals("DB RegistryDSConfigAdaptor should be empty before store and update\n", 0, regadaptor.getDatasetInternalNames().length);
    DatasetConfig dbdsview = (DatasetConfig) new URLDSConfigAdaptor(getURL(TESTDBDSVIEW)).getDatasetConfigs().next();
    DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, USER, dbdsview, true);
    
    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYDB));
    
    //assertEquals("DB RegistryDSConfigAdaptor should have 1 DatasetConfig after store, recreate\n", 1, regadaptor.getDatasetDisplayNames().length);
    assertEquals("DB RegistryDSConfigAdaptor Dataset internalName is incorrect\n", TESTMARTREGDBINAME, ((DatasetConfig) regadaptor.getDatasetConfigs().next()).getInternalName());
    
    //testMartRegistryRegistry
    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYREGISTRY));
    assertEquals("Registry RegistryDSConfigAdaptor should have 1 DatasetConfig\n", 1, regadaptor.getNumDatasetConfigs());
    //assertEquals("Registry RegistryDSConfigAdaptor Dataset internalName is incorrect\n", TESTMARTREGISTRYREGINAME, regadaptor.getDatasetInternalNames()[0]);
    

//TODO: our duplicate adaptor management became more complex with the addition of user defined adaptor names, need to rethink how to determine sameness of adaptors
      
//    //testMartRegistryRegistryDup alone
//    
//    //just for this test, set the adaptorName of testMartRegistryDup equal to that for filebak
//    regadaptor.setName(filebak.getName());
//     
//    regadaptor = new RegistryDSConfigAdaptor(getURL(TESTMARTREGISTRYDUP));
//    assertEquals("Dup RegistryDSConfigAdaptor should equal File RegistryDSConfigAdaptor\n", filebak, regadaptor);
//    
//    //testMartRegistryRegistryDup.add(testMartRegistryFile)
//    regadaptor.add(filebak);
//    assertEquals("Dup RegistryDSConfigAdaptor should equal File RegistryDSConfigAdaptor after add(filebak)\n", filebak, regadaptor);
    
    //testMartRegistryComposite
    regadaptor = new RegistryDSConfigAdaptor( getURL( TESTMARTREGISTRYCOMPOSITE ) );
    
    assertEquals("Composite RegistryDSConfigAdaptor should have 3 DatasetConfigs\n", 3, regadaptor.getNumDatasetConfigs());
    
    //getMartRegistry
    MartRegistry mr = regadaptor.getMartRegistry();
    assertEquals("MartRegistry from Composite RegistryDSConfigAdaptor should have 3 MartLocations\n", 3, mr.getMartLocations().length);
    
    //Store new datasetView to DB, and update
    DatasetConfigIterator dsvi = URLDSConfigAdaptorTest.getSampleDSConfigAdaptor().getDatasetConfigs();
    assertTrue("URLDSConfigAdaptor should have one DatasetConfig\n", dsvi.hasNext());
    DatasetConfig newDatasetConfig = (DatasetConfig) dsvi.next();
    DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, USER, newDatasetConfig, true);
    
    regadaptor.update();
    assertEquals("Composite RegistryDSConfigAdaptor should contain 4 DatasetConfigs after store and update\n", 4, regadaptor.getNumDatasetConfigs());

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
    
    //get rid of stored DatasetConfig on ensro user table
    DatabaseDatasetConfigUtils.DeleteOldDSConfigEntriesFor(martJDataSource, DatabaseDatasetConfigUtils.getDSConfigTableFor(martJDataSource, USER), newDatasetConfig.getDataset(), newDatasetConfig.getInternalName(), newDatasetConfig.getDisplayName());

//  TODO: sameness issue    
//    //storeMartRegistry and retrieve
//    File testFile = new File(TESTFILEPATH);
//    if (testFile.exists())
//      testFile.delete();
//    
//    RegistryDSConfigAdaptor.StoreMartRegistry(mr, testFile);
//    assertTrue("TestFile " + TESTFILEPATH + " should exist after storeMartRegistry\n", testFile.exists());
//    
//    RegistryDSConfigAdaptor inReg = new RegistryDSConfigAdaptor(getURL(TESTFILEPATH));
//    assertEquals("Input File Registry after Store should equal Composite RegistryDSConfigAdaptor\n", regadaptor, inReg);
//    
//    testFile.delete();
    
    //new Registry(URLDSConfigAdaptor)
    URLDSConfigAdaptor urladaptor = URLDSConfigAdaptorTest.getSampleDSConfigAdaptor();
    RegistryDSConfigAdaptor urlReg = new RegistryDSConfigAdaptor(urladaptor);
//    assertTrue("URLDSConfigAdaptor RegistryDSConfigAdaptor should support urladaptors DatasetConfig\n", urlReg.supportsInternalName(urladaptor.getDatasetInternalNames()[0]));

//  TODO: sameness issue    
//    //new Registry(URLDSConfigAdaptor, url) should be equal to previous
//    RegistryDSConfigAdaptor urlRegURL = new RegistryDSConfigAdaptor(urladaptor, getURL(TESTMARTREGISTRYCOMPOSITE)); // different URL, same DatasetConfig
//    assertEquals("URLDSConfigAdaptor + URL RegistryDSConfigAdaptor should equal non URL URLDSConfigAdaptor RegistryDSConfigAdaptor\n", urlReg, urlRegURL);
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();

    if (DatabaseDatasetConfigUtils.DSConfigUserTableExists(martJDataSource, USER)) {
      String metaTable = DatabaseDatasetConfigUtils.getDSConfigTableFor(martJDataSource, USER);
      Connection conn = martJDataSource.getConnection();
      conn.createStatement().executeUpdate("delete from " + metaTable);
      conn.close();
    }
  }


  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    if (DatabaseDatasetConfigUtils.DSConfigUserTableExists(martJDataSource, USER)) {
      String metaTable = DatabaseDatasetConfigUtils.getDSConfigTableFor(martJDataSource, USER);
      Connection conn = martJDataSource.getConnection();
      conn.createStatement().executeUpdate("delete from " + metaTable);
      conn.close();
    }
    
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
