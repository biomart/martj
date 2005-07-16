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
import org.ensembl.mart.lib.config.ConfigurationException;
/**
 * Contains all of the information required by a UI to display a transformation config,
 * Container for a set of Dataset objects
 *   
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */
public class TransformationConfig extends BaseNamedConfigurationObject {

  private List datasets = new ArrayList();
  private Hashtable datasetNameMap = new Hashtable();

  /**
   * Copy Constructor.
   * @param ds -- TransformationConfig to copy
   * @throws ConfigurationException if both propogateExistingElements is true, and lazyLoad is true.
   */
  public TransformationConfig(TransformationConfig ds) throws ConfigurationException {
    super(ds);
    Dataset[] datasets = ds.getDatasets();
    for (int i = 0, n = datasets.length; i < n; i++) {
       addDataset(new Dataset(datasets[i]));
    }
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
   * Add a Dataset to the TransformationConfig.
   * 
   * @param f FiterPage object.
   */
  public void addDataset(Dataset f) {
    datasets.add(f);
    datasetNameMap.put(f.getInternalName(), f);
  }

  /**
   * Remove a Dataset from the TransformationConfig.
   * @param f -- Dataset to be removed.
   */
  public void removeDataset(Dataset f) {
    datasetNameMap.remove(f.getInternalName());
    for (int i = 0; i < datasets.size(); i++){
    	Dataset fp = (Dataset) datasets.get(i);
    }
    datasets.remove(f);
  }

  /**
   * Insert a Dataset at a specific Position within the Dataset list.
   * Datasets at or after the given position will be shifted right).
   * @param position -- Position to insert the Dataset
   * @param f -- Dataset to insert.
   */
  public void insertDataset(int position, Dataset f) {
    datasets.add(position, f);
    datasetNameMap.put(f.getInternalName(), f);
  }


  /**
   * Add a group of Dataset objects in one call.
   * Note, subsequent calls to addDataset or addDatasets
   * will add to what has been added before.
   * 
   * @param f Dataset[] array of Dataset objects.
   */
  public void addDatasets(Dataset[] f) {
    for (int i = 0, n = f.length; i < n; i++) {
      datasets.add(f[i]);
      datasetNameMap.put(f[i].getInternalName(), f);
    }
  }

  /**
   * Returns a list of all Dataset objects contained within the TransformationConfig, in the order they were added.
   * @return Dataset[]
   */
  public Dataset[] getDatasets() {
    Dataset[] fs = new Dataset[datasets.size()];
    datasets.toArray(fs);
    return fs;
  }

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

    for (Iterator iter = datasets.iterator(); iter.hasNext();) {
        Dataset element = (Dataset) iter.next();
        tmp = (31 * tmp) + element.hashCode();
    }
    return tmp;
  }

}
