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

import org.ensembl.mart.lib.test.Base;
import org.ensembl.mart.lib.config.CompositeDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDatasetViewUtils;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;

/**
 * Tests CompositeDSViewAdaptor.
 */
public class CompositeDSViewAdaptorTest extends Base {


  public void testAll() throws Exception {
    setUp();
    
    CompositeDSViewAdaptor adaptor = new CompositeDSViewAdaptor();
  
    assertTrue( adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( adaptor.getDatasetViews().length==0 );
    
    URLDSViewAdaptor urlAdaptor = URLDSViewAdaptorTest.getSampleDSViewAdaptor();
    
    adaptor.add( urlAdaptor );
    assertTrue( adaptor.getDatasetViews().length==1 );
    assertTrue( adaptor.getDatasetDisplayNames().length==1 );
    DatasetView view = adaptor.getDatasetViews()[0];
    assertNotNull( view );
    assertTrue( view.getAllFilterDescriptions().size()>0 );
    
    assertTrue( adaptor.remove( urlAdaptor ) );
    assertTrue( adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( adaptor.getDatasetViews().length==0 );

    adaptor.add( urlAdaptor );
    adaptor.clear();
    assertTrue( adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( adaptor.getDatasetViews().length==0 );

    // this falls over if _meta_DatasetView_DatabaseDSViewAdaptorTest.USER doesnt exist
    DatabaseDSViewAdaptor dbAdaptor = DatabaseDSViewAdaptorTest.getSampleDatasetViewAdaptor(martJDataSource);
    
    // make sure testDataset.xml is stored in the table, then update it
    DatabaseDSViewAdaptor.storeDatasetView(martJDataSource, DatabaseDSViewAdaptorTest.USER, DatasetViewXMLUtilsTest.TestDatasetViewInstance(false), true);
    dbAdaptor.update();
    
    adaptor.add( dbAdaptor );
    assertTrue( adaptor.getDatasetViews().length==1 );
    assertTrue( adaptor.getDatasetDisplayNames().length==1 );
    
    view = adaptor.getDatasetViews()[0];
    assertNotNull( view );
    assertTrue( view.getAllFilterDescriptions().size()>0 );
    
    //clean up the _meta_DatasetView table
    String metatable = DatabaseDatasetViewUtils.getDSViewTableFor(martJDataSource, DatabaseDSViewAdaptorTest.USER);
    DatabaseDatasetViewUtils.DeleteOldDSViewEntriesFor(martJDataSource, metatable, view.getInternalName(), view.getDisplayName());
    
    assertTrue( adaptor.remove( dbAdaptor ));
    assertTrue( adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( adaptor.getDatasetViews().length==0 );
    
    adaptor.add( urlAdaptor );
    adaptor.add( dbAdaptor );
    adaptor.clear();
    assertTrue( adaptor.getDatasetDisplayNames().length==0 );
    assertTrue( adaptor.getDatasetViews().length==0 );                  
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
