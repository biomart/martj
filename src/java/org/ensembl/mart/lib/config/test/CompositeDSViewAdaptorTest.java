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

import org.ensembl.mart.lib.config.CompositeDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetViewUtils;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.lib.test.Base;

/**
 * Tests CompositeDSViewAdaptor.
 */
public class CompositeDSViewAdaptorTest extends Base {


  public void testAll() throws Exception {
    //TODO: major refactor
    setUp();
    
    CompositeDSViewAdaptor adaptor = new CompositeDSViewAdaptor();
  
    //assertTrue( "Initial CompositeDSViewAdaptor should be empty\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "Initial CompositeDSViewAdaptor should be empty\n", adaptor.getDatasetViews().length==0 );
    
    URLDSViewAdaptor urlAdaptor = URLDSViewAdaptorTest.getSampleDSViewAdaptor();
    
    adaptor.add( urlAdaptor );
    assertTrue( "Adaptor should return 1 DatasetView after add(urlAdaptor)\n", adaptor.getDatasetViews().length==1 );
    //assertTrue( "Adaptor should return 1 DatasetView after add(urlAdaptor)\n", adaptor.getDatasetDisplayNames().length==1 );
    DatasetView view = adaptor.getDatasetViews()[0];
    assertNotNull( "DatasetView returned from URLDSViewAdaptor based adaptor is null\n", view );
    assertTrue( "DatasetView should have filter descriptions but doesnt\n", view.getAllFilterDescriptions().size()>0 );
    
    assertTrue( "Could not remove Adaptor\n", adaptor.remove( urlAdaptor ) );
    //assertTrue( "getDatasetDisplayNames should return zero elements after remove\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "getDatasetViews should return zero elements after remove\n", adaptor.getDatasetViews().length==0 );

    adaptor.add( urlAdaptor );
    adaptor.clear();
    //assertTrue( "adaptor should be empty after clear\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "adaptor should be empty after clear\n", adaptor.getDatasetViews().length==0 );

    adaptor.add( urlAdaptor );
    assertTrue( "Could not removeDatasetView\n" , adaptor.removeDatasetView(view) );
    assertTrue( "There should be no DatasetView objects left after removeDatasetView\n", adaptor.getDatasetViews().length == 0 );
    
    adaptor.clear();
    //assertTrue( "adaptor should be empty after clear after removeDatasetView\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "adaptor should be empty after clear after removeDatasetView\n", adaptor.getDatasetViews().length==0 );
    
    // this falls over if _meta_DatasetView_DatabaseDSViewAdaptorTest.USER doesnt exist
    DatabaseDSViewAdaptor dbAdaptor = DatabaseDSViewAdaptorTest.getSampleDatasetViewAdaptor(martJDataSource);
    
    // make sure testDataset.xml is stored in the table, then update it
    DatabaseDSViewAdaptor.storeDatasetView(martJDataSource, DatabaseDSViewAdaptorTest.USER, DatasetViewXMLUtilsTest.TestDatasetViewInstance(false), true);
    dbAdaptor.update();
    
    adaptor.add( dbAdaptor );
    assertTrue( "Adaptor should return one DatasetView after add(dbAdaptor)\n", adaptor.getDatasetViews().length==1 );
    //assertTrue( "Adaptor should return one DatasetView after add(dbAdaptor)\n", adaptor.getDatasetDisplayNames().length==1 );
    
    view = adaptor.getDatasetViews()[0];
    assertNotNull( "DatasetView returned by getDatasetViews is null\n", view );
    assertTrue( "DatasetView should have filter descriptions but doesnt\n", view.getAllFilterDescriptions().size()>0 );
    
    //clean up the _meta_DatasetView table
    String metatable = DatabaseDatasetViewUtils.getDSViewTableFor(martJDataSource, DatabaseDSViewAdaptorTest.USER);
    DatabaseDatasetViewUtils.DeleteOldDSViewEntriesFor(martJDataSource, metatable, view.getDataset(), view.getInternalName(), view.getDisplayName());
    
    assertTrue( "Could not remove dbAdaptor\n", adaptor.remove( dbAdaptor ));
    //assertTrue("getDatasetDisplayNames should return zero elements after remove\n", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue("getDatasetViews should return zero elements after remove\n", adaptor.getDatasetViews().length==0 );
    
    adaptor.add( urlAdaptor );
    adaptor.add( dbAdaptor );
    adaptor.clear();
    //assertTrue( "adaptor should be empty after clear", adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( "adaptor should be empty after clear", adaptor.getDatasetViews().length==0 );
    
    adaptor.add (urlAdaptor );
    adaptor.add( dbAdaptor );
    assertTrue( "getMartlocations should return 2 MartLocation objects after 2 adds\n", adaptor.getMartLocations().length == 2 );                      
  }

	/**
	 * Constructor for CompositeDSViewAdaptorTest.
	 * @param arg0
	 */
	public CompositeDSViewAdaptorTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(CompositeDSViewAdaptorTest.class);
	}

}
