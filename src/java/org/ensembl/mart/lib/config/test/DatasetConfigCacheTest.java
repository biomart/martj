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

import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSConfigAdaptor;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.DatasetConfigCache;
import org.ensembl.mart.lib.config.DatasetConfigIterator;
import org.ensembl.mart.lib.config.DatasetConfigXMLUtils;

/** 
* @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
* @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
*/
public class DatasetConfigCacheTest extends TestCase {

  private Logger logger = Logger.getLogger(DatasetConfigCacheTest.class.getName());
  private final String[] keys = new String[] { "test" , "case"  };
  private DatasetConfigCache cache = null;
  private DSConfigAdaptor adaptor = null;
  private DatasetConfig orig = null;
  
  public DatasetConfigCacheTest(String arg0) {
    super(arg0);
  }
  
  public void testDatasetConfigCache() throws Exception {
    //TODO: implement

    assertTrue("Orig should not be cached at this point!\n", !cache.cacheExists(orig.getDataset(), orig.getInternalName()));
    
    cache.addDatasetConfig(orig);
    assertTrue("Orig should be cached at this point!\n", cache.cacheExists(orig.getDataset(), orig.getInternalName()));
    assertTrue("Orig cache should be up to date at this point!\n", cache.cacheUpToDate(orig.getMessageDigest(), orig.getDataset(), orig.getInternalName()));
    
    DatasetConfig nDSC = cache.getDatasetConfig(orig.getDataset(), orig.getInternalName(), adaptor);
    assertEquals("Orig and nDSC should equal\n", orig, nDSC);
    
    cache.removeDatasetConfig(orig.getDataset(), orig.getInternalName());
    assertTrue("Orig should not be cached at this point!\n", !cache.cacheExists(orig.getDataset(), orig.getInternalName()));
    
    //add new copy
    DatasetConfig lazyLoadedDSC = new DatasetConfig(orig, false, true);
    lazyLoadedDSC.setDSConfigAdaptor(null);
    cache.addDatasetConfig(lazyLoadedDSC);
    DatasetConfig lazyLoadedFromCache = cache.getDatasetConfig(lazyLoadedDSC.getDataset(), lazyLoadedDSC.getInternalName(), null);
    
    assertEquals("lazyLoaded DatasetConfig Copy should equal DatasetConfig retrieved from cache with a null adaptor!", lazyLoadedDSC, lazyLoadedFromCache);
    
    //lazyLoadFromCache
    lazyLoadedDSC.setDSConfigAdaptor(adaptor); //so that we can get a non lazyLoaded copy to lazyLoad from cache
    lazyLoadedFromCache = new DatasetConfig(lazyLoadedDSC, false, false);
    lazyLoadedDSC.setDSConfigAdaptor(null); //now equals will run through everything
    lazyLoadedFromCache.setDSConfigAdaptor(null); //now equals will run through everything
    cache.lazyLoadWithCache(lazyLoadedFromCache);
    assertEquals("lazyLoaded DatasetConfig Copy should equal DatasetConfig lazyLoaded from Cache!\n", lazyLoadedDSC, lazyLoadedFromCache);
    
    cache.removeDatasetConfig(lazyLoadedDSC.getDataset(), lazyLoadedDSC.getInternalName());
    lazyLoadedDSC = null; //gc
    lazyLoadedFromCache = null; //gc
    
    cache.addDatasetConfig(orig);
    DatasetConfig newDigestDSC = new DatasetConfig(orig, false, false);
    newDigestDSC.setDescription("THIS IS A TOTALLY NEW DESCRIPTION");
    byte[] newDigest = DatasetConfigXMLUtils.DatasetConfigToMessageDigest(newDigestDSC);
    
    assertTrue("Cache should NOT be up to date at this point!\n", !cache.cacheUpToDate(newDigest, orig.getDataset(), orig.getInternalName()));
    assertTrue("Cache should NOT contain orig after cacheUpToDate fails!\n", !cache.cacheExists(orig.getDataset(), orig.getInternalName()));
  }
  
  private void clearCache() {
    try {
      cache.clearCache();
    } catch (ConfigurationException e) {
      throw new RuntimeException("Couldnt Clear cache: "+ e.getMessage() + "\nshutting down the test!\n");
    }
  }
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(DatasetConfigCacheTest.class);
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
     super.setUp();
     logger.setLevel(Level.INFO);
     adaptor = URLDSConfigAdaptorTest.getSampleDSConfigAdaptor();
     
     DatasetConfigIterator iter = adaptor.getDatasetConfigs();
     if (!iter.hasNext())
       throw new ConfigurationException("Couldnt get original DatasetConfig from URLDSViewAdaptorTest\n");
     
     orig = new DatasetConfig((DatasetConfig) iter.next(), false, false); //non lazyLoad copy
     orig.setMessageDigest(DatasetConfigXMLUtils.DatasetConfigToMessageDigest(orig));
     cache = new DatasetConfigCache(adaptor, keys);
     clearCache();
     cache = new DatasetConfigCache(adaptor, keys);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    clearCache();
    super.tearDown();
  }

}
