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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib.config.test;

import org.ensembl.mart.lib.config.CompositeDSConfigAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSConfigAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetConfigUtils;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.URLDSConfigAdaptor;
import org.ensembl.mart.lib.test.Base;

/**
 * Tests CompositeDSConfigAdaptor.
 */
public class CompositeDSConfigAdaptorTest extends Base {


  public void testAll() throws Exception {
    //TODO: major refactor
    setUp();
    
    CompositeDSConfigAdaptor adaptor = new CompositeDSConfigAdaptor();
  
    //assertTrue( "Initial CompositeDSConfigAdaptor should be empty\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "Initial CompositeDSConfigAdaptor should be empty\n", adaptor.getNumDatasetConfigs() ==0 );
    
    URLDSConfigAdaptor urlAdaptor = URLDSConfigAdaptorTest.getSampleDSConfigAdaptor();
    
    adaptor.add( urlAdaptor );
    assertTrue( "Adaptor should return 1 DatasetConfig after add(urlAdaptor)\n", adaptor.getNumDatasetConfigs() == 1 );
    //assertTrue( "Adaptor should return 1 DatasetConfig after add(urlAdaptor)\n", adaptor.getDatasetDisplayNames().length==1 );
    DatasetConfig view = (DatasetConfig) adaptor.getDatasetConfigs().next();
    assertNotNull( "DatasetConfig returned from URLDSConfigAdaptor based adaptor is null\n", view );
    assertTrue( "DatasetConfig should have filter descriptions but doesnt\n", view.getAllFilterDescriptions().size()>0 );
    
    assertTrue( "Could not remove Adaptor\n", adaptor.remove( urlAdaptor ) );
    //assertTrue( "getDatasetDisplayNames should return zero elements after remove\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "getDatasetConfigs should return zero elements after remove\n", adaptor.getNumDatasetConfigs()==0 );

    adaptor.add( urlAdaptor );
    adaptor.clear();
    //assertTrue( "adaptor should be empty after clear\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "adaptor should be empty after clear\n", adaptor.getNumDatasetConfigs()==0 );

    adaptor.add( urlAdaptor );
    assertTrue( "Could not removeDatasetConfig\n" , adaptor.removeDatasetConfig(view) );
    assertTrue( "There should be no DatasetConfig objects left after removeDatasetConfig\n", adaptor.getNumDatasetConfigs() == 0 );
    
    adaptor.clear();
    //assertTrue( "adaptor should be empty after clear after removeDatasetConfig\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "adaptor should be empty after clear after removeDatasetConfig\n", adaptor.getNumDatasetConfigs()==0 );
    
    // this falls over if meta_DatasetConfig_DatabaseDSConfigAdaptorTest.USER doesnt exist
    DatabaseDSConfigAdaptor dbAdaptor = DatabaseDSConfigAdaptorTest.getSampleDatasetConfigAdaptor(martJDataSource);
    
    // make sure testDataset.xml is stored in the table, then update it
    DatabaseDSConfigAdaptor.storeDatasetConfig(martJDataSource, DatabaseDSConfigAdaptorTest.USER, DatasetConfigXMLUtilsTest.TestDatasetConfigInstance(false), true);
    dbAdaptor.update();
    
    adaptor.add( dbAdaptor );
    assertTrue( "Adaptor should return one DatasetConfig after add(dbAdaptor)\n", adaptor.getNumDatasetConfigs()==1 );
    //assertTrue( "Adaptor should return one DatasetConfig after add(dbAdaptor)\n", adaptor.getDatasetDisplayNames().length==1 );
    
    view = (DatasetConfig) adaptor.getDatasetConfigs().next();
    assertNotNull( "DatasetConfig returned by getDatasetConfigs is null\n", view );
    assertTrue( "DatasetConfig should have filter descriptions but doesnt\n", view.getAllFilterDescriptions().size()>0 );
    
    //clean up the meta_DatasetConfig table
    String metatable = DatabaseDatasetConfigUtils.getDSConfigTableFor(martJDataSource, DatabaseDSConfigAdaptorTest.USER);
    DatabaseDatasetConfigUtils.DeleteOldDSConfigEntriesFor(martJDataSource, metatable, view.getDataset(), view.getInternalName(), view.getDisplayName());
    
    assertTrue( "Could not remove dbAdaptor\n", adaptor.remove( dbAdaptor ));
    //assertTrue("getDatasetDisplayNames should return zero elements after remove\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue("getDatasetConfigs should return zero elements after remove\n", adaptor.getNumDatasetConfigs()==0 );
    
    adaptor.add( urlAdaptor );
    adaptor.add( dbAdaptor );
    adaptor.clear();
    //assertTrue( "adaptor should be empty after clear", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "adaptor should be empty after clear", adaptor.getNumDatasetConfigs()==0 );
    
    adaptor.add (urlAdaptor );
    adaptor.add( dbAdaptor );
    assertTrue( "getMartlocations should return 2 MartLocation objects after 2 adds\n", adaptor.getMartLocations().length == 2 );                      
  }

	/**
	 * Constructor for CompositeDSConfigAdaptorTest.
	 * @param arg0
	 */
	public CompositeDSConfigAdaptorTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(CompositeDSConfigAdaptorTest.class);
	}

}
