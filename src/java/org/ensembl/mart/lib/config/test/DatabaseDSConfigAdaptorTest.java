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

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatabaseDSConfigAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetConfigUtils;
import org.ensembl.mart.lib.config.DatabaseLocation;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.DatasetConfigXMLUtils;
import org.ensembl.mart.lib.config.MartLocation;
import org.ensembl.mart.lib.config.MartLocationBase;
import org.ensembl.mart.lib.config.SimpleDSConfigAdaptor;
import org.ensembl.mart.lib.test.Base;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSConfigAdaptorTest extends Base {
  private Logger logger = Logger.getLogger(DatabaseDSConfigAdaptorTest.class.getName());
  private DatasetConfigXMLUtils dscutils = DatasetConfigXMLUtilsTest.DEFAULTUTILS;

  private DatabaseDatasetConfigUtils dbutils = null;

  public static final String USER = "test";

  private static final String MODTESTDATASETVIEWFILE = Base.UNITTESTDIR + "/testDatasetConfigMod.xml";

  /**
   * Returns an instance of a DatabaseDSConfigAdaptor from the given DataSource, for
   * the user DatabaseDSConfigAdaptorTest.USER.  If the meta_DatasetConfig_[user] table
   * does not exist, an exception is thrown.
   * @param dsource -- DataSource for Mart Database to be tested against.
   * @return DatabaseDSConfigAdaptor
   * @throws Exception when meta_DatasetConfig_[user] doesnt exist, and for all underlying exceptions
   */
  public static DatabaseDSConfigAdaptor getSampleDatasetConfigAdaptor(
    DetailedDataSource dsource,
    DatabaseDatasetConfigUtils dbutils)
    throws Exception {
    if (!dbutils.datasetConfigUserTableExists(USER))
      throw new Exception("meta_DatasetConfig_" + USER + " must exist in the Mart Database\n");

    DatabaseDSConfigAdaptor ret = new DatabaseDSConfigAdaptor(dsource, USER, true, false, true);

    return ret;
  }

  /**
   * @param arg0
   */
  public DatabaseDSConfigAdaptorTest(String arg0) {
    super(arg0);
  }

  public void testDatabaseDSConfigAdaptor() throws Exception {
    //TODO: major refactor
    assertTrue(
      "meta_DatasetConfig_test does not exist, must exist for test to run\n",
      dbutils.datasetConfigUserTableExists(USER));

    DatabaseDSConfigAdaptor refdbdsva = getSampleDatasetConfigAdaptor(martJDataSource, dbutils);

    assertTrue("meta_DatasetConfig_test must be empty for test to run\n", refdbdsva.getNumDatasetConfigs() == 0);

    DatasetConfig refdsv = DatasetConfigXMLUtilsTest.TestDatasetConfigInstance(false);
    byte[] refDigest = dscutils.getMessageDigestForDatasetConfig(refdsv);

    DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, USER, refdsv, true);

    refdbdsva.update();

    assertTrue(
      "DatabaseDSConfigAdaptor should have 1 DatasetConfig after store and update\n",
      refdbdsva.getNumDatasetConfigs() == 1);
    //		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig internalName after store and update\n", refdbdsva.getDatasetInternalNames().length == 1);
    //		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig displayName after store and update\n", refdbdsva.getDatasetDisplayNames().length == 1);

    DatasetConfig ndsv = new DatasetConfig((DatasetConfig) refdbdsva.getDatasetConfigs().next(), false, true);
    ndsv.setDSConfigAdaptor(null); //so equals will check elements, rather than adaptor

    assertEquals("DatasetConfig retrieved after store does not equal reference DatasetConfig\n", refdsv, ndsv);
    //assertTrue("DatabaseDSConfigAdaptor should support internalName of reference DatasetConfig\n", refdbdsva.supportsInternalName(refdsv.getInternalName()));
    //		assertTrue("DatabaseDSConfigAdaptor should support displayName of reference DatasetConfig\n", refdbdsva.supportsDisplayName(refdsv.getDisplayName()));

    DatasetConfig modDSV =
      dscutils.getDatasetConfigForXMLStream(
        DatasetConfigXMLUtilsTest.class.getClassLoader().getResourceAsStream(MODTESTDATASETVIEWFILE));
    modDSV.setDSConfigAdaptor(new SimpleDSConfigAdaptor(modDSV)); //overrides lazyLoad
    modDSV.setMessageDigest(dscutils.getMessageDigestForDatasetConfig(modDSV));

    DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, USER, modDSV, true);
    refdbdsva.update();

    assertTrue(
      "DatabaseDSConfigAdaptor should have 1 DatasetConfig after mod, store and update\n",
      refdbdsva.getNumDatasetConfigs() == 1);
    //		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig internalName after mod, store and update\n", refdbdsva.getDatasetInternalNames().length == 1);
    //		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig displayName after mod, store and update\n", refdbdsva.getDatasetDisplayNames().length == 1);

    DatasetConfig newestDSV = (DatasetConfig) refdbdsva.getDatasetConfigs().next();

    assertTrue(
      "DatasetConfig in DatabaseDatasetConfigAdaptor after mod, store, and update should equal modified DatasetConfig loaded in from file system based on message digest\n",
      MessageDigest.isEqual(modDSV.getMessageDigest(), newestDSV.getMessageDigest()));

    byte[] newestDigest = newestDSV.getMessageDigest();
    assertTrue(
      "DatasetConfig in DatabaseDatasetConfigAdatpro after mod, store, and update should not equal Reference DatasetConfig based on message digest\n",
      !(MessageDigest.isEqual(refDigest, newestDigest)));

    assertTrue("Could not removeDatasetConfig\n", refdbdsva.removeDatasetConfig(newestDSV));
    //		assertTrue("Adaptor should be empty after removeDatasetConfig\n", refdbdsva.getDatasetInternalNames().length == 0);
    //		assertTrue("Adaptor should be empty after removeDatasetConfig\n", refdbdsva.getDatasetDisplayNames().length == 0);
    assertTrue("Adaptor should be empty after removeDatasetConfig\n", refdbdsva.getNumDatasetConfigs() == 0);

    refdbdsva.update();

    assertTrue(
      "DatabaseDSConfigAdaptor should have 1 DatasetConfig after removeDatasetConfig and update\n",
      refdbdsva.getNumDatasetConfigs() == 1);
    //		assertTrue(
    //			"DatabaseDSConfigAdaptor should have 1 DatasetConfig internalName after removeDatasetConfig and update\n",
    //			refdbdsva.getDatasetInternalNames().length == 1);
    //		assertTrue(
    //			"DatabaseDSConfigAdaptor should have 1 DatasetConfig displayName after removeDatasetConfig and update\n",
    //			refdbdsva.getDatasetDisplayNames().length == 1);

    MartLocation[] martlocs = refdbdsva.getMartLocations();
    assertTrue("getMartLocations didnt return anything\n", martlocs.length > 0);

    MartLocation thisLoc = martlocs[0];
    assertEquals(
      "MartLocation type should be " + MartLocationBase.DATABASE + "\n",
      MartLocationBase.DATABASE,
      thisLoc.getType());
    assertTrue(
      "MartLocation returned from getMartLocations should be a URLLocation\n",
      thisLoc instanceof DatabaseLocation);
    assertNull(
      "Password in resulting MartLocation should be null before setPassword is called\n",
      ((DatabaseLocation) thisLoc).getPassword());

    String testPassword = "testpassword";
    refdbdsva.setDatabasePassword(testPassword);
    MartLocation newLoc = refdbdsva.getMartLocations()[0];
    assertEquals(
      "Password should match after setPassword call\n",
      testPassword,
      ((DatabaseLocation) newLoc).getPassword());
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(DatabaseDSConfigAdaptorTest.class);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    dropTestConfigTable(martJDataSource);
    super.tearDown();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();

    dbutils = new DatabaseDatasetConfigUtils(dscutils, martJDataSource);
    assertTrue(
      "Could not create test meta_DatasetConfig table\n"
        + " please ensure that "
        + Base.MARTJ_DB_CONFIG_URL
        + " specifies connection parameters for a user with write access and that the meta_datasetconfig tables exist\n",
      hasWriteAccessAndTestMetaTable(martJDataSource));
  }

  public static void dropTestConfigTable(DetailedDataSource dsource) throws Exception {
    DatabaseDatasetConfigUtils dbutils = new DatabaseDatasetConfigUtils(DatasetConfigXMLUtilsTest.DEFAULTUTILS, dsource);
    
    if (dbutils.datasetConfigUserTableExists(USER)) {
      String metaTable = dbutils.getDSConfigTableFor(USER);
      Connection conn = null;
      try {
        conn = dsource.getConnection();
        conn.createStatement().executeUpdate("drop table " + metaTable);
      } finally {
        DetailedDataSource.close(conn);
      }
    }
    dbutils = null;
  }
  
  public static boolean hasWriteAccessAndTestMetaTable(DetailedDataSource dsource) {
    boolean hasWriteAccess = false;
    
    Logger logger = Logger.getLogger(DatabaseDSConfigAdaptorTest.class.getName());
    
    DatabaseDatasetConfigUtils dbutils = new DatabaseDatasetConfigUtils(DatasetConfigXMLUtilsTest.DEFAULTUTILS, dsource);
    Connection conn = null;

    String metaTable = null;
    try {
      conn = dsource.getConnection();
            
      if (dbutils.datasetConfigUserTableExists(USER)) {
        metaTable = dbutils.getDSConfigTableFor(USER);
        conn.createStatement().executeUpdate("delete from " + metaTable);
        hasWriteAccess = true;
      } else if (dbutils.baseDSConfigTableExists()) {
        metaTable = dbutils.getDSConfigTableFor(null) + "_" + USER; 
        String createSQL = "create table " + metaTable + " select * from " + dbutils.getDSConfigTableFor(null) + " where 1 = 0";
        conn.createStatement().execute(
          createSQL);
        hasWriteAccess = true;
      } else
        hasWriteAccess = false;
    } catch (SQLException e) {
      if (logger.isLoggable(Level.INFO))
        logger.info("Could not create table " + metaTable + " " + e.getMessage() + "\n");      
      hasWriteAccess = false;
    } catch (ConfigurationException e) {
      if (logger.isLoggable(Level.INFO))
        logger.info("Could not create table " + metaTable + " " + e.getMessage() + "\n");
              
      hasWriteAccess = false;
    } finally {
      DetailedDataSource.close(conn);
    }

    dbutils = null;
    return hasWriteAccess;
  }

}
