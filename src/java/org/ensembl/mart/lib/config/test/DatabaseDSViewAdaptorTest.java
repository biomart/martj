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
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetViewUtils;
import org.ensembl.mart.lib.config.DatabaseLocation;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.DatasetViewXMLUtils;
import org.ensembl.mart.lib.config.MartLocation;
import org.ensembl.mart.lib.config.MartLocationBase;
import org.ensembl.mart.lib.test.Base;
import org.ensembl.mart.util.BigPreferences;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSViewAdaptorTest extends Base {
	private Logger logger = Logger.getLogger(DatabaseDSViewAdaptorTest.class.getName());
	public static final String USER = "test";
	private static final String MODTESTDATASETVIEWFILE = "data/XML/testDatasetViewMod.xml";

	/**
	 * Returns an instance of a DatabaseDSViewAdaptor from the given DataSource, for
	 * the user DatabaseDSViewAdaptorTest.USER.  If the meta_DatasetView_[user] table
	 * does not exist, an exception is thrown.
	 * @param dsource -- DataSource for Mart Database to be tested against.
	 * @return DatabaseDSViewAdaptor
	 * @throws Exception when meta_DatasetView_[user] doesnt exist, and for all underlying exceptions
	 */
	public static DatabaseDSViewAdaptor getSampleDatasetViewAdaptor(DetailedDataSource dsource) throws Exception {
		if (!DatabaseDatasetViewUtils.DSViewUserTableExists(dsource, USER))
			throw new Exception("meta_DatasetView_" + USER + " must exist in the Mart Database\n");

		DatabaseDSViewAdaptor ret = new DatabaseDSViewAdaptor(dsource, USER);

		return ret;
	}

	/**
	 * @param arg0
	 */
	public DatabaseDSViewAdaptorTest(String arg0) {
		super(arg0);
	}

	public void testDatabaseDSViewAdaptor() throws Exception {
    //TODO: major refactor
		assertTrue("meta_DatasetView_test does not exist, must exist for test to run\n", DatabaseDatasetViewUtils.DSViewUserTableExists(martJDataSource, USER));

		DatabaseDSViewAdaptor refdbdsva = getSampleDatasetViewAdaptor(martJDataSource);

		assertTrue("meta_DatasetView_test must be empty for test to run\n", refdbdsva.getDatasetViews().length == 0);

		DatasetView refdsv = DatasetViewXMLUtilsTest.TestDatasetViewInstance(false);
		byte[] refDigest = DatasetViewXMLUtils.DatasetViewToMessageDigest(refdsv);

		DatabaseDSViewAdaptor.storeDatasetView(martJDataSource, USER, refdsv, true);

		refdbdsva.update();

		assertTrue("DatabaseDSViewAdaptor should have 1 DatasetView after store and update\n", refdbdsva.getDatasetViews().length == 1);
//		assertTrue("DatabaseDSViewAdaptor should have 1 DatasetView internalName after store and update\n", refdbdsva.getDatasetInternalNames().length == 1);
//		assertTrue("DatabaseDSViewAdaptor should have 1 DatasetView displayName after store and update\n", refdbdsva.getDatasetDisplayNames().length == 1);

		DatasetView ndsv = refdbdsva.getDatasetViews()[0];

		//use assertSame, as this tests the object reference.  Does not test actual equality, as this may be a stub DatasetView
//		assertSame(
//			"DatabaseDSViewAdaptor does not return correct DatasetView for getByInternalName\n",
//			ndsv,
//			refdbdsva.getDatasetViewByInternalName(ndsv.getInternalName()));
//		assertSame(
//			"DatabaseDSViewAdaptor does not return correct DatasetView for getByDisplayName\n",
//			ndsv,
//			refdbdsva.getDatasetViewByDisplayName(ndsv.getDisplayName()));

		//by testing all of the functionality of the DatasetView, we test the underlying lazyload functionality of DatasetView    
		//DatasetViewXMLUtilsTest.validateDatasetView(ndsv);

		assertEquals("DatasetView retrieved after store does not equal reference DatasetView\n", refdsv, ndsv);
		//assertTrue("DatabaseDSViewAdaptor should support internalName of reference DatasetView\n", refdbdsva.supportsInternalName(refdsv.getInternalName()));
//		assertTrue("DatabaseDSViewAdaptor should support displayName of reference DatasetView\n", refdbdsva.supportsDisplayName(refdsv.getDisplayName()));

		DatasetView modDSV =
			DatasetViewXMLUtils.XMLStreamToDatasetView(DatasetViewXMLUtilsTest.class.getClassLoader().getResourceAsStream(MODTESTDATASETVIEWFILE), false);
		modDSV.setMessageDigest(DatasetViewXMLUtils.DatasetViewToMessageDigest(modDSV));

		DatabaseDSViewAdaptor.storeDatasetView(martJDataSource, USER, modDSV, true);
		refdbdsva.update();

		assertTrue("DatabaseDSViewAdaptor should have 1 DatasetView after mod, store and update\n", refdbdsva.getDatasetViews().length == 1);
//		assertTrue("DatabaseDSViewAdaptor should have 1 DatasetView internalName after mod, store and update\n", refdbdsva.getDatasetInternalNames().length == 1);
//		assertTrue("DatabaseDSViewAdaptor should have 1 DatasetView displayName after mod, store and update\n", refdbdsva.getDatasetDisplayNames().length == 1);

		DatasetView newestDSV = refdbdsva.getDatasetViews()[0];

		assertTrue(
			"DatasetView in DatabaseDatasetViewAdaptor after mod, store, and update should equal modified DatasetView loaded in from file system based on message digest\n",
			MessageDigest.isEqual(modDSV.getMessageDigest(), newestDSV.getMessageDigest()));

		byte[] newestDigest = newestDSV.getMessageDigest();
		assertTrue(
			"DatasetView in DatabaseDatasetViewAdatpro after mod, store, and update should not equal Reference DatasetView based on message digest\n",
			!(MessageDigest.isEqual(refDigest, newestDigest)));

		assertTrue("Could not removeDatasetView\n", refdbdsva.removeDatasetView(newestDSV));
//		assertTrue("Adaptor should be empty after removeDatasetView\n", refdbdsva.getDatasetInternalNames().length == 0);
//		assertTrue("Adaptor should be empty after removeDatasetView\n", refdbdsva.getDatasetDisplayNames().length == 0);
		assertTrue("Adaptor should be empty after removeDatasetView\n", refdbdsva.getDatasetViews().length == 0);

		refdbdsva.update();

		assertTrue("DatabaseDSViewAdaptor should have 1 DatasetView after removeDatasetView and update\n", refdbdsva.getDatasetViews().length == 1);
//		assertTrue(
//			"DatabaseDSViewAdaptor should have 1 DatasetView internalName after removeDatasetView and update\n",
//			refdbdsva.getDatasetInternalNames().length == 1);
//		assertTrue(
//			"DatabaseDSViewAdaptor should have 1 DatasetView displayName after removeDatasetView and update\n",
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
		junit.textui.TestRunner.run(DatabaseDSViewAdaptorTest.class);
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

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	public void setUp() throws Exception {
		super.setUp();

    //clear the entire DataseDSViewAdaptor preferences, so that it can be properly tested
    BigPreferences.userNodeForPackage(DatabaseDSViewAdaptor.class).removeNode();
    BigPreferences.userNodeForPackage(DatabaseDSViewAdaptor.class).flush();
    
		if (DatabaseDatasetViewUtils.DSViewUserTableExists(martJDataSource, USER)) {
			String metaTable = DatabaseDatasetViewUtils.getDSViewTableFor(martJDataSource, USER);
			Connection conn = martJDataSource.getConnection();
			conn.createStatement().executeUpdate("delete from " + metaTable);
			conn.close();
		}
	}

}
