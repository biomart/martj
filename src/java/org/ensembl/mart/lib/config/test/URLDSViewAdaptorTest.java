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

import java.net.URL;

import junit.framework.TestCase;

import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;

/**
 * Loads sample dataset view configuration file via an adaptor (searches the classpath) 
 * and reads some data from it to ensure the adaptor worked.
 * 
 * Sample file is: "data/XML/homo_sapiens__ensembl_genes.xml"
 */
public class URLDSViewAdaptorTest extends TestCase {

	/**
	 * Constructor for DSViewAdaptors.
	 * @param arg0
	 */
	public URLDSViewAdaptorTest(String arg0) {
		super(arg0);
	}

	public void testURLDSViewAdaptor() throws Exception {


    URLDSViewAdaptor adaptor = getSampleDSViewAdaptor();

		DatasetView[] views = adaptor.getDatasetViews();
		assertTrue("No datasets loaded.", views.length > 0);
		DatasetView view = views[0];
		assertNotNull(view.getDescription());
		assertNotNull(view.getDisplayName());
		assertNotNull(view.getInternalName());
		assertTrue(view.getAllFilterDescriptions().size() > 0);
		assertTrue(view.getAllFilterDescriptions().size() > 0);
	}

	public static URL getTestDatasetURL() throws Exception {

		URL url = URLDSViewAdaptorTest.class.getClassLoader().getResource(DatasetViewXMLUtilsTest.TESTDATASETVIEWFILE);
    assertNotNull("Missing dataset file: " + DatasetViewXMLUtilsTest.TESTDATASETVIEWFILE + "\n", url);
    
    return url;
	}
  
  public static URLDSViewAdaptor getSampleDSViewAdaptor() throws Exception {
    URLDSViewAdaptor adaptor = new URLDSViewAdaptor( getTestDatasetURL(), false);
    assertNotNull( adaptor );
    return adaptor;
  }

	public static void main(String[] args) {
		junit.textui.TestRunner.run(URLDSViewAdaptorTest.class);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

}
