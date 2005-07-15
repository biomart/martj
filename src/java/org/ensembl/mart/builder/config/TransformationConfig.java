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

package org.ensembl.mart.builder.config;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 
 *   
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */
public class TransformationConfig extends BaseNamedConfigurationObject {

  //private int[] reqFields = {0,3,4,5};// rendered red in AttributeTable
  
  //private DSConfigAdaptor adaptor = null;
  private byte[] digest = null;

  private List datasets = new ArrayList();
  //private boolean hasBrokenFilterPages = false;

  private Hashtable datasetNameMap = new Hashtable();

  private Logger logger = Logger.getLogger(TransformationConfig.class.getName());

  /**
   * Copy Constructor allowing client to specify whether to lazyLoad the copy at initiation, rather
   * than defering to a call to getXXX.
   * @param ds -- TransformationConfig to copy
   * @param propogateExistingElements -- specifies that the copy should have any existing Elements copied to the
   *        new Copy. If this is false, then the system may defer this to the lazyLoad system if an adaptor has
   *        been set on the TransformationConfig object being copied. This option cannot be true if lazyLoad is true
   * @param preLazyLoad - boolean, if true, copy will automatically lazyLoad, if false, copy will defer lazyLoading until a getXXX method is called. 
   *        This cannot be set to true if propogateExistingElements is set to true.
   * @throws ConfigurationException if both propogateExistingElements is true, and lazyLoad is true.
   */
  public TransformationConfig(TransformationConfig ds, boolean propogateExistingElements, boolean preLazyLoad) throws ConfigurationException {
    super(ds);
    if (propogateExistingElements && preLazyLoad)
      throw new ConfigurationException("You can not copy an existing TransformationConfig using both propogateExistingElements and lazyLoad\n");
	
    //byte[] digest = ds.getMessageDigest();
    //if (digest != null)
     // setMessageDigest(digest);

    //if the TransformationConfig has an underlying DSConfigAdaptor implementing Object, this can be substituted for the
    //actual element content, and defer the loading of this content to the lazyLoad system.  This requires that
    //all DSConfigAdaptor implementing objects either implement a lazyLoad method and insert themselves into every
    //TransformationConfig that they manage, or, in the absence of a sensible lazyLoad method, ensure that all content is
    //is loaded, and __NOT__ insert themselves into the TransformationConfig that they manage.
    //if (ds.getAdaptor() == null || propogateExistingElements) {

      Dataset[] fpages = ds.getDatasets();
      for (int i = 0, n = fpages.length; i < n; i++) {
        addDataset(new Dataset(fpages[i]));
      }
    //}
    //else
      //setDSConfigAdaptor(ds.getAdaptor());

     // try to replace lazyloading system with explicit call
	  


    //if (preLazyLoad)
      //lazyLoad();
  }

  /**
   * Empty constructor.  Should really only be used by the TransformationConfigEditor
   */
  public TransformationConfig() {
    super();
  }

  /**
   * Constructs a TransformationConfig named by internalName and displayName.
   *  internalName is a single word that references this dataset, used to get the dataset from the MartConfiguration by name.
   *  displayName is the String to display in any UI.
   * 
   * @param internalName String name to represent this TransformationConfig
   * @param displayName String name to display.
   * @param dataset String prefix for all tables in the Mart Database for this DatasetCode. Must not be null
   */
  public TransformationConfig(String internalName) throws ConfigurationException {
    super(internalName);
  }
  
  
  /**
   * Add a FilterPage to the TransformationConfig.
   * 
   * @param f FiterPage object.
   */
  public void addDataset(Dataset f) {
    datasets.add(f);
    datasetNameMap.put(f.getInternalName(), f);
  }

  /**
   * Remove a FilterPage from the TransformationConfig.
   * @param f -- FilterPage to be removed.
   */
  public void removeDataset(Dataset f) {
    //lazyLoad();
    datasetNameMap.remove(f.getInternalName());
    for (int i = 0; i < datasets.size(); i++){
    	Dataset fp = (Dataset) datasets.get(i);
    }
    datasets.remove(f);
  }

  /**
   * Insert a FilterPage at a specific Position within the FilterPage list.
   * FilterPages at or after the given position will be shifted right).
   * @param position -- Position to insert the FilterPage
   * @param f -- FilterPage to insert.
   */
  public void insertDataset(int position, Dataset f) {
    //lazyLoad();
    datasets.add(position, f);
    datasetNameMap.put(f.getInternalName(), f);
  }


  /**
   * Add a group of FilterPage objects in one call.
   * Note, subsequent calls to addFilterPage or addFilterPages
   * will add to what has been added before.
   * 
   * @param f FilterPage[] array of FilterPage objects.
   */
  public void addDatasets(Dataset[] f) {
    for (int i = 0, n = f.length; i < n; i++) {
      datasets.add(f[i]);
      datasetNameMap.put(f[i].getInternalName(), f);
    }
  }

  /**
   * Returns a list of all FilterPage objects contained within the TransformationConfig, in the order they were added.
   * @return FilterPage[]
   */
  public Dataset[] getDatasets() {
    //lazyLoad();
    Dataset[] fs = new Dataset[datasets.size()];
    datasets.toArray(fs);
    return fs;
  }


  /**
   * Returns a digest suitable for comparison with a digest computed on another version
   * of the XML underlying this TransformationConfig. 
   * @return byte[] digest
   */
    public byte[] getMessageDigest() {
    return digest;
  }

  /**
   * Set a Message Digest for the TransformationConfig.  This must be a digest
   * generated by a java.security.MessageDigest object with the given algorithmName
   * method.  
   * @param bs - byte[] digest computed
   */
  public void setMessageDigest(byte[] bs) {
    digest = bs;
  }

  /**
   * set the DSConfigAdaptor used to instantiate a particular TransformationConfig object.
   * @param dsva -- DSConfigAdaptor implimenting object.
   */

/*
  public void setDSConfigAdaptor(DSConfigAdaptor dsva) {
    adaptor = dsva;
  }

  /**
   * Get the DSConfigAdaptor implimenting object used to instantiate this TransformationConfig object.
   * @return DSConfigAdaptor used to instantiate this TransformationConfig
   */
  
/*  
  public DSConfigAdaptor getDSConfigAdaptor() {
    return adaptor;
  }

  private void lazyLoad() {
    if (datasets.size() == 0) {
      if (adaptor == null)
        throw new RuntimeException("TransformationConfig objects must be provided a DSConfigAdaptor to facilitate lazyLoading\n");
      try {
        if (logger.isLoggable(Level.INFO))
          logger.info("LAZYLOAD\n");

        
        adaptor.lazyLoad(this);
        
      } catch (ConfigurationException e) {
        throw new RuntimeException("Could not lazyload datasetconfig " + e.getMessage(), e);
      } catch(OutOfMemoryError e) {
        System.err.println("Problem on thread:" + Thread.currentThread());
        new Exception("Ran out of memory. Could not lazyload datasetconfig. ", e).printStackTrace();
        throw e;
      }
    }
  }
*/
  /**
   * Provides output useful for debugging purposes.
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[");
    buf.append(super.toString());
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons manipulation of TransformationConfig objects.
   * Note, currently does not use Message Digest information.
   * Also, If the lazy load fails, a RuntimeException is thrown.
   */
  public boolean equals(Object o) {
    return o instanceof TransformationConfig && hashCode() == ((TransformationConfig) o).hashCode();
  }

  /**
   * hashCode for TransformationConfig
   * Note, currently does not compare digest data, even if present.
   * In order to prevent an automatic lazyLoad for hashcode/equals comparisons,
   * the method first checks to determine if the TransformationConfig has a DSConfigAdaptor
   * set.  If it does, then it is assumed that two TransformationConfig objects containing the same
   * DatasetCode, InternalName, DisplayName, description, and DSConfigAdaptor are equal (based on the fact that they come from the
   * same source).  If this TransformationConfig does not have a DSConfigAdaptor, then it must be fully loaded (otherwise,
   * it is an invalid TransformationConfig), so no lazyLoad will be necessary.
   */
  public int hashCode() {

    int tmp = super.hashCode();
    
    //use the adaptor instead of the actual values, if it has a valid adaptor
    //if (adaptor != null && !(adaptor instanceof SimpleDSConfigAdaptor)) {
      //tmp = (31 * tmp) + adaptor.hashCode();
    //} else {
      
      for (Iterator iter = datasets.iterator(); iter.hasNext();) {
        Dataset element = (Dataset) iter.next();
        tmp = (31 * tmp) + element.hashCode();
      }

    //}
    return tmp;
  }


  /**
   * @return adaptor that created this instance, can be null.
   */
//  public DSConfigAdaptor getAdaptor() {
//    return adaptor;

//  }
}
