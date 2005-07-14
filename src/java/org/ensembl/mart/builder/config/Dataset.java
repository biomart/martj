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
public class Dataset extends BaseNamedConfigurationObject {

  //private int[] reqFields = {0,3,4,5};// rendered red in AttributeTable
  
  private DSConfigAdaptor adaptor = null;
  private byte[] digest = null;

  private List transformations = new ArrayList();
  //private boolean hasBrokenFilterPages = false;

  private Hashtable transformationNameMap = new Hashtable();

  private Logger logger = Logger.getLogger(Dataset.class.getName());


  /**
   * Empty constructor.  Should really only be used by the DatasetEditor
   */
  public Dataset() {
    super();
  }

  /**
   * Constructs a Dataset named by internalName and displayName.
   *  internalName is a single word that references this dataset, used to get the dataset from the MartConfiguration by name.
   *  displayName is the String to display in any UI.
   * 
   * @param internalName String name to represent this Dataset
   * @param displayName String name to display.
   * @param dataset String prefix for all tables in the Mart Database for this DatasetCode. Must not be null
   */
  public Dataset(String internalName) throws ConfigurationException {
    super(internalName);
  }
  

  /**
   * Copy constructor. Constructs an exact copy of an existing Transformation.
   * @param ap Transformation to copy.
   */
  public Dataset(Dataset ap) {
	super (ap);
  	
	Transformation[] agroups = ap.getTransformations();
	for (int i = 0, n = agroups.length; i < n; i++) {
	  Object group = agroups[i];
	  addTransformation( new Transformation( (Transformation) group ));     
	}
  }

  
  /**
   * Add a FilterPage to the Dataset.
   * 
   * @param f FiterPage object.
   */
  public void addTransformation(Transformation f) {
    transformations.add(f);
    transformationNameMap.put(f.getInternalName(), f);
  }

  /**
   * Remove a FilterPage from the Dataset.
   * @param f -- FilterPage to be removed.
   */
  public void removeTransformation(Transformation f) {
    transformationNameMap.remove(f.getInternalName());
    for (int i = 0; i < transformations.size(); i++){
    	Transformation fp = (Transformation) transformations.get(i);
    }
    transformations.remove(f);
  }

  /**
   * Insert a FilterPage at a specific Position within the FilterPage list.
   * FilterPages at or after the given position will be shifted right).
   * @param position -- Position to insert the FilterPage
   * @param f -- FilterPage to insert.
   */
  public void insertTransformation(int position, Transformation f) {
    transformations.add(position, f);
    transformationNameMap.put(f.getInternalName(), f);
  }

  /**
   * Insert a FilterPage before a specified FilterPage, named by internalName.
   * @param internalName -- name of the FilterPage before which the given FilterPage should be inserted.
   * @param f -- FilterPage to be inserted.
   * @throws ConfigurationException when the Dataset does not contain a FilterPage named by internalName.
   */
  public void insertTransformationBeforeTransformation(String internalName, Transformation f) throws ConfigurationException {
    if (!transformationNameMap.containsKey(internalName))
      throw new ConfigurationException("Dataset does not contain TransformationUnit " + internalName + "\n");
     insertTransformation(transformations.indexOf(transformationNameMap.get(internalName)), f);
  }

  /**
   * Insert a FilterPage after a specified FilterPage, named by internalName.
   * @param internalName -- name of the FilterPage after which the given FilterPage should be inserted.
   * @param f -- FilterPage to be inserted.
   * @throws ConfigurationException when the Dataset does not contain a FilterPage named by internalName.
   */
  public void insertTransformationAfterTransformation(String internalName, Transformation f) throws ConfigurationException {
    if (!transformationNameMap.containsKey(internalName))
      throw new ConfigurationException("Dataset does not contain TransformationUnit " + internalName + "\n");
    insertTransformation(transformations.indexOf(transformationNameMap.get(internalName)) + 1, f);
  }

  /**
   * Add a group of FilterPage objects in one call.
   * Note, subsequent calls to addFilterPage or addFilterPages
   * will add to what has been added before.
   * 
   * @param f FilterPage[] array of FilterPage objects.
   */
  public void addTransformations(Transformation[] f) {
    for (int i = 0, n = f.length; i < n; i++) {
      transformations.add(f[i]);
      transformationNameMap.put(f[i].getInternalName(), f);
    }
  }

  /**
   * Returns a list of all FilterPage objects contained within the Dataset, in the order they were added.
   * @return FilterPage[]
   */
  public Transformation[] getTransformations() {
    Transformation[] fs = new Transformation[transformations.size()];
    transformations.toArray(fs);
    return fs;
  }


  /**
   * Returns a digest suitable for comparison with a digest computed on another version
   * of the XML underlying this Dataset. 
   * @return byte[] digest
   */
  public byte[] getMessageDigest() {
    return digest;
  }

  /**
   * Set a Message Digest for the Dataset.  This must be a digest
   * generated by a java.security.MessageDigest object with the given algorithmName
   * method.  
   * @param bs - byte[] digest computed
   */
  public void setMessageDigest(byte[] bs) {
    digest = bs;
  }

  /**
   * set the DSConfigAdaptor used to instantiate a particular Dataset object.
   * @param dsva -- DSConfigAdaptor implimenting object.
   */
  public void setDSConfigAdaptor(DSConfigAdaptor dsva) {
    adaptor = dsva;
  }

  /**
   * Get the DSConfigAdaptor implimenting object used to instantiate this Dataset object.
   * @return DSConfigAdaptor used to instantiate this Dataset
   */
  public DSConfigAdaptor getDSConfigAdaptor() {
    return adaptor;
  }

/*  private void lazyLoad() {
    if (transformations.size() == 0) {
      if (adaptor == null)
        throw new RuntimeException("Dataset objects must be provided a DSConfigAdaptor to facilitate lazyLoading\n");
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
   * Allows Equality Comparisons manipulation of Dataset objects.
   * Note, currently does not use Message Digest information.
   * Also, If the lazy load fails, a RuntimeException is thrown.
   */
  public boolean equals(Object o) {
    return o instanceof Dataset && hashCode() == ((Dataset) o).hashCode();
  }

  /**
   * hashCode for Dataset
   * Note, currently does not compare digest data, even if present.
   * In order to prevent an automatic lazyLoad for hashcode/equals comparisons,
   * the method first checks to determine if the Dataset has a DSConfigAdaptor
   * set.  If it does, then it is assumed that two Dataset objects containing the same
   * DatasetCode, InternalName, DisplayName, description, and DSConfigAdaptor are equal (based on the fact that they come from the
   * same source).  If this Dataset does not have a DSConfigAdaptor, then it must be fully loaded (otherwise,
   * it is an invalid Dataset), so no lazyLoad will be necessary.
   */
  public int hashCode() {

    int tmp = super.hashCode();
    
    //use the adaptor instead of the actual values, if it has a valid adaptor
    if (adaptor != null && !(adaptor instanceof SimpleDSConfigAdaptor)) {
      tmp = (31 * tmp) + adaptor.hashCode();
    } else {
      
      for (Iterator iter = transformations.iterator(); iter.hasNext();) {
        Transformation element = (Transformation) iter.next();
        tmp = (31 * tmp) + element.hashCode();
      }

    }
    return tmp;
  }


  /**
   * @return adaptor that created this instance, can be null.
   */
  public DSConfigAdaptor getAdaptor() {
    return adaptor;

  }
}
