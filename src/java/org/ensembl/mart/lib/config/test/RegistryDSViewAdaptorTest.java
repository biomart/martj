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

import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetViewUtils;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.MartRegistry;
import org.ensembl.mart.lib.config.RegistryDSViewAdaptor;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.lib.test.Base;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class RegistryDSViewAdaptorTest extends Base {

  private final String TESTFILEPATH = "RegistryDSViewAdaptorTestFile.xml";
  
  private final String TESTMARTREGISTRYFILE = "data/XML/testMartRegistryFile.xml";
  private final String TESTMARTREGFILEINAME = "test_datasetview_registry_file";
  
  private final String TESTMARTREGISTRYDB = "data/XML/testMartRegistryDB.xml";
  private final String TESTMARTREGDBINAME = "test_datasetview_db";
  private final String TESTDBDSVIEW = "data/XML/testDatasetViewRegDB.xml";
  private final String USER = "test";
  
  private final String TESTMARTREGISTRYREGISTRY = "data/XML/testMartRegistryRegistry.xml";
  private final String TESTMARTREGISTRYREGINAME = "test_dataset_registry";
  
  private final String TESTMARTREGISTRYDUP = "data/XML/testMartRegistryRegistryDuplicate.xml";
  private final String TESTMARTREGISTRYCOMPOSITE = "data/XML/testMartRegistryComposite.xml";
  
  private URL getURL(String path) throws Exception {
    URL url = RegistryDSViewAdaptor.class.getClassLoader().getResource(path);
    assertNotNull("Missing file " + path + "\n", url);
    return url;
  }
  
  public void testAll() throws Exception {
    setUp();
    
    //empty
    RegistryDSViewAdaptor regadaptor = new RegistryDSViewAdaptor();
    assertNotNull("Empty RegistryDSViewAdaptor should not be null\n", regadaptor);
    
    //set URL to load File, just to test if it has a DatasetView
    regadaptor.setRegistryURL(getURL(TESTMARTREGISTRYFILE));
    
    assertEquals("File RegistryDSViewAdaptor should have 1 DatasetView after setRegistryURL on empty\n", 1, regadaptor.getDatasetViews().length);
    
    //testMartRegistryFile
    regadaptor = new RegistryDSViewAdaptor(getURL(TESTMARTREGISTRYFILE));
    assertEquals("RECIEVED WRONG DatasetView internalName for File RegistryDSViewAdaptor\n", TESTMARTREGFILEINAME, regadaptor.getDatasetInternalNames()[0]);
    
    RegistryDSViewAdaptor filebak = new RegistryDSViewAdaptor(getURL(TESTMARTREGISTRYFILE)); // to test dup
    
    //testMartRegistryDB
    assertTrue("_meta_DatasetView_ensro does not exist, must exist for test to run\n", DatabaseDatasetViewUtils.DSViewUserTableExists(martJDataSource, USER));
    
    regadaptor = new RegistryDSViewAdaptor(getURL(TESTMARTREGISTRYDB));
    assertEquals("DB RegistryDSViewAdaptor should be empty before store and update\n", 0, regadaptor.getDatasetInternalNames().length);
    DatasetView dbdsview = new URLDSViewAdaptor(getURL(TESTDBDSVIEW)).getDatasetViews()[0];
    DatabaseDSViewAdaptor.storeDatasetView(martJDataSource, USER, dbdsview, true);
    
    regadaptor = new RegistryDSViewAdaptor(getURL(TESTMARTREGISTRYDB));
    
    assertEquals("DB RegistryDSViewAdaptor should have 1 DatasetView after store, recreate\n", 1, regadaptor.getDatasetDisplayNames().length);
    assertEquals("DB RegistryDSViewAdaptor Dataset internalName is incorrect\n", TESTMARTREGDBINAME, regadaptor.getDatasetViews()[0].getInternalName());
    
    //testMartRegistryRegistry
    regadaptor = new RegistryDSViewAdaptor(getURL(TESTMARTREGISTRYREGISTRY));
    assertEquals("Registry RegistryDSViewAdaptor should have 1 DatasetView\n", 1, regadaptor.getDatasetViews().length);
    assertEquals("Registry RegistryDSViewAdaptor Dataset internalName is incorrect\n", TESTMARTREGISTRYREGINAME, regadaptor.getDatasetInternalNames()[0]);
    
    //testMartRegistryRegistryDup alone
     
    regadaptor = new RegistryDSViewAdaptor(getURL(TESTMARTREGISTRYDUP));
    assertEquals("Dup RegistryDSViewAdaptor should equal File RegistryDSViewAdaptor\n", filebak, regadaptor);
    
    //testMartRegistryRegistryDup.add(testMartRegistryFile)
    regadaptor.add(filebak);
    assertEquals("Dup RegistryDSViewAdaptor should equal File RegistryDSViewAdaptor after add(filebak)\n", filebak, regadaptor);
    
    //testMartRegistryComposite
    regadaptor = new RegistryDSViewAdaptor( getURL( TESTMARTREGISTRYCOMPOSITE ) );
    
    assertEquals("Composite RegistryDSViewAdaptor should have 3 DatasetViews\n", 3, regadaptor.getDatasetViews().length);
    assertTrue("Composite RegistryDSViewAdaptor should support File DatasetView internalName\n", regadaptor.supportsInternalName(TESTMARTREGFILEINAME));
    assertTrue("Composite RegistryDSViewAdaptor should support DB DatasetView internalName\n", regadaptor.supportsInternalName(TESTMARTREGDBINAME));
    assertTrue("Composite RegistryDSViewAdaptor should support Registry DatasetView internalName\n", regadaptor.supportsInternalName(TESTMARTREGISTRYREGINAME));
    
    //getMartRegistry
    MartRegistry mr = regadaptor.getMartRegistry();
    assertEquals("MartRegistry from Composite RegistryDSViewAdaptor should have 3 MartLocations\n", 3, mr.getMartLocations().length);
    
    //Store new datasetView to DB, and update
    DatasetView newDatasetView = URLDSViewAdaptorTest.getSampleDSViewAdaptor().getDatasetViews()[0];
    DatabaseDSViewAdaptor.storeDatasetView(martJDataSource, USER, newDatasetView, true);
    
    regadaptor.update();
    assertEquals("Composite RegistryDSViewAdaptor should contain 4 DatasetViews after store and update\n", 4, regadaptor.getDatasetViews().length);
    assertTrue("Composite RegistryDSViewAdaptor should now support newDatasetView after store and update\n", regadaptor.supportsInternalName(newDatasetView.getInternalName()));
    
    // new registry, no URL
    RegistryDSViewAdaptor regreg = new RegistryDSViewAdaptor(mr);
    
    boolean testequals = regadaptor.equals(regreg);
    assertTrue("Non URL MartRegistry based RegistryDSViewAdaptor should equal Composite RegistryDSVIewAdaptor\n", testequals);
    
    //new registry, + URL should be equal to previous
    RegistryDSViewAdaptor regurl = new RegistryDSViewAdaptor(mr, getURL(TESTMARTREGISTRYDB));  //URL is different, but underlying MartRegistry should be the same
    assertEquals("MartRegistry+URL RegistryDSViewAdaptor should equal Composite RegistryDSViewAdaptor\n", regadaptor, regurl);
    assertEquals("MartRegistry+URL RegistryDSViewAdaptor should equal Non URL RegistryDSViewAdaptor\n", regreg, regurl); 
    
    //get rid of stored DatasetView on ensro user table
    DatabaseDatasetViewUtils.DeleteOldDSViewEntriesFor(martJDataSource, DatabaseDatasetViewUtils.getDSViewTableFor(martJDataSource, USER), newDatasetView.getInternalName(), newDatasetView.getDisplayName());
    
    //storeMartRegistry and retrieve
    File testFile = new File(TESTFILEPATH);
    if (testFile.exists())
      testFile.delete();
    
    RegistryDSViewAdaptor.StoreMartRegistry(mr, testFile);
    assertTrue("TestFile " + TESTFILEPATH + " should exist after storeMartRegistry\n", testFile.exists());
    
    RegistryDSViewAdaptor inReg = new RegistryDSViewAdaptor(getURL(TESTFILEPATH));
    assertEquals("Input File Registry after Store should equal Composite RegistryDSViewAdaptor\n", regadaptor, inReg);
    
    testFile.delete();
    
    //new Registry(URLDSViewAdaptor)
    URLDSViewAdaptor urladaptor = URLDSViewAdaptorTest.getSampleDSViewAdaptor();
    RegistryDSViewAdaptor urlReg = new RegistryDSViewAdaptor(urladaptor);
    assertTrue("URLDSViewAdaptor RegistryDSViewAdaptor should support urladaptors DatasetView\n", urlReg.supportsInternalName(urladaptor.getDatasetInternalNames()[0]));
    
    //new Registry(URLDSViewAdaptor, url) should be equal to previous
    RegistryDSViewAdaptor urlRegURL = new RegistryDSViewAdaptor(urladaptor, getURL(TESTMARTREGISTRYCOMPOSITE)); // different URL, same DatasetView
    assertEquals("URLDSViewAdaptor + URL RegistryDSViewAdaptor should equal non URL URLDSViewAdaptor RegistryDSViewAdaptor\n", urlReg, urlRegURL);
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();

    if (DatabaseDatasetViewUtils.DSViewUserTableExists(martJDataSource, USER)) {
      String metaTable = DatabaseDatasetViewUtils.getDSViewTableFor(martJDataSource, USER);
      Connection conn = martJDataSource.getConnection();
      conn.createStatement().executeUpdate("delete from " + metaTable);
      conn.close();
    }
  }


  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    if (DatabaseDatasetViewUtils.DSViewUserTableExists(martJDataSource, USER)) {
      String metaTable = DatabaseDatasetViewUtils.getDSViewTableFor(martJDataSource, USER);
      Connection conn = martJDataSource.getConnection();
      conn.createStatement().executeUpdate("delete from " + metaTable);
      conn.close();
    }
    
    super.tearDown();
  }
  
  /**
   * Constructor for RegistryDSViewAdaptorTest.
   * @param arg0
   */
  public RegistryDSViewAdaptorTest(String arg0) {
    super(arg0);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(RegistryDSViewAdaptorTest.class);
  }
}
