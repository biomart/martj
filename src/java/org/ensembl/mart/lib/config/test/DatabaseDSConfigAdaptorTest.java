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
import java.util.logging.Logger;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.config.DatabaseDSConfigAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetConfigUtils;
import org.ensembl.mart.lib.config.DatabaseLocation;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.DatasetConfigXMLUtils;
import org.ensembl.mart.lib.config.MartLocation;
import org.ensembl.mart.lib.config.MartLocationBase;
import org.ensembl.mart.lib.test.Base;
import org.ensembl.mart.util.BigPreferences;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSConfigAdaptorTest extends Base {
	private Logger logger = Logger.getLogger(DatabaseDSConfigAdaptorTest.class.getName());
	public static final String USER = "test";
	private static final String MODTESTDATASETCONFIGFILE = "data/XML/testDatasetConfigMod.xml";

	/**
	 * Returns an instance of a DatabaseDSConfigAdaptor from the given DataSource, for
	 * the user DatabaseDSConfigAdaptorTest.USER.  If the meta_DatasetConfig_[user] table
	 * does not exist, an exception is thrown.
	 * @param dsource -- DataSource for Mart Database to be tested against.
	 * @return DatabaseDSConfigAdaptor
	 * @throws Exception when meta_DatasetConfig_[user] doesnt exist, and for all underlying exceptions
	 */
	public static DatabaseDSConfigAdaptor getSampleDatasetConfigAdaptor(DetailedDataSource dsource) throws Exception {
		if (!DatabaseDatasetConfigUtils.DSConfigUserTableExists(dsource, USER))
			throw new Exception("meta_DatasetConfig_" + USER + " must exist in the Mart Database\n");

		DatabaseDSConfigAdaptor ret = new DatabaseDSConfigAdaptor(dsource, USER);

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
		assertTrue("meta_DatasetConfig_test does not exist, must exist for test to run\n", DatabaseDatasetConfigUtils.DSConfigUserTableExists(martJDataSource, USER));

		DatabaseDSConfigAdaptor refdbdsva = getSampleDatasetConfigAdaptor(martJDataSource);

		assertTrue("meta_DatasetConfig_test must be empty for test to run\n", refdbdsva.getDatasetConfigs().length == 0);

		DatasetConfig refdsv = DatasetConfigXMLUtilsTest.TestDatasetConfigInstance(false);
		byte[] refDigest = DatasetConfigXMLUtils.DatasetConfigToMessageDigest(refdsv);

		DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, USER, refdsv, true);

		refdbdsva.update();

		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig after store and update\n", refdbdsva.getDatasetConfigs().length == 1);
//		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig internalName after store and update\n", refdbdsva.getDatasetInternalNames().length == 1);
//		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig displayName after store and update\n", refdbdsva.getDatasetDisplayNames().length == 1);

		DatasetConfig ndsv = refdbdsva.getDatasetConfigs()[0];

		//use assertSame, as this tests the object reference.  Does not test actual equality, as this may be a stub DatasetConfig
//		assertSame(
//			"DatabaseDSConfigAdaptor does not return correct DatasetConfig for getByInternalName\n",
//			ndsv,
//			refdbdsva.getDatasetConfigByInternalName(ndsv.getInternalName()));
//		assertSame(
//			"DatabaseDSConfigAdaptor does not return correct DatasetConfig for getByDisplayName\n",
//			ndsv,
//			refdbdsva.getDatasetConfigByDisplayName(ndsv.getDisplayName()));

		//by testing all of the functionality of the DatasetConfig, we test the underlying lazyload functionality of DatasetConfig    
		//DatasetConfigXMLUtilsTest.validateDatasetConfig(ndsv);

		assertEquals("DatasetConfig retrieved after store does not equal reference DatasetConfig\n", refdsv, ndsv);
		//assertTrue("DatabaseDSConfigAdaptor should support internalName of reference DatasetConfig\n", refdbdsva.supportsInternalName(refdsv.getInternalName()));
//		assertTrue("DatabaseDSConfigAdaptor should support displayName of reference DatasetConfig\n", refdbdsva.supportsDisplayName(refdsv.getDisplayName()));

		DatasetConfig modDSV =
			DatasetConfigXMLUtils.XMLStreamToDatasetConfig(DatasetConfigXMLUtilsTest.class.getClassLoader().getResourceAsStream(MODTESTDATASETCONFIGFILE), false);
		modDSV.setMessageDigest(DatasetConfigXMLUtils.DatasetConfigToMessageDigest(modDSV));

		DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, USER, modDSV, true);
		refdbdsva.update();

		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig after mod, store and update\n", refdbdsva.getDatasetConfigs().length == 1);
//		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig internalName after mod, store and update\n", refdbdsva.getDatasetInternalNames().length == 1);
//		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig displayName after mod, store and update\n", refdbdsva.getDatasetDisplayNames().length == 1);

		DatasetConfig newestDSV = refdbdsva.getDatasetConfigs()[0];

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
		assertTrue("Adaptor should be empty after removeDatasetConfig\n", refdbdsva.getDatasetConfigs().length == 0);

		refdbdsva.update();

		assertTrue("DatabaseDSConfigAdaptor should have 1 DatasetConfig after removeDatasetConfig and update\n", refdbdsva.getDatasetConfigs().length == 1);
//		assertTrue(
//			"DatabaseDSConfigAdaptor should have 1 DatasetConfig internalName after removeDatasetConfig and update\n",
//			refdbdsva.getDatasetInternalNames().length == 1);
//		assertTrue(
//			"DatabaseDSConfigAdaptor should have 1 DatasetConfig displayName after removeDatasetConfig and update\n",
//			refdbdsva.getDatasetDisplayNames().length == 1);

		MartLocation[] martlocs = refdbdsva.getMartLocations();
		assertTrue("getMartLocations didnt return anything\n", martlocs.length > 0);

		MartLocation thisLoc = martlocs[0];
		assertEquals("MartLocation type should be " + MartLocationBase.DATABASE + "\n", MartLocationBase.DATABASE, thisLoc.getType());
		assertTrue("MartLocation returned from getMartLocations should be a URLLocation\n", thisLoc instanceof DatabaseLocation);
		assertNull("Password in resulting MartLocation should be null before setPassword is called\n", ((DatabaseLocation) thisLoc).getPassword());

		String testPassword = "testpassword";
		refdbdsva.setDatabasePassword(testPassword);
		MartLocation newLoc = refdbdsva.getMartLocations()[0];
		assertEquals("Password should match after setPassword call\n", testPassword, ((DatabaseLocation) newLoc).getPassword());
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(DatabaseDSConfigAdaptorTest.class);
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

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	public void setUp() throws Exception {
		super.setUp();

    //clear the entire DataseDSConfigAdaptor preferences, so that it can be properly tested
    BigPreferences.userNodeForPackage(DatabaseDSConfigAdaptor.class).removeNode();
    BigPreferences.userNodeForPackage(DatabaseDSConfigAdaptor.class).flush();
    
		if (DatabaseDatasetConfigUtils.DSConfigUserTableExists(martJDataSource, USER)) {
			String metaTable = DatabaseDatasetConfigUtils.getDSConfigTableFor(martJDataSource, USER);
			Connection conn = martJDataSource.getConnection();
			conn.createStatement().executeUpdate("delete from " + metaTable);
			conn.close();
		}
	}

}
