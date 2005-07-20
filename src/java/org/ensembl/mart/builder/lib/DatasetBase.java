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

package org.ensembl.mart.builder.lib;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.ensembl.mart.lib.config.ConfigurationException;

/**
 * Contains all of the information required by a UI to display a dataset,
 * Container for a set of Transformation objects
 *  
 *   
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */
public class DatasetBase extends BaseNamedConfigurationObject {

  private List transformations = new ArrayList();
  private Hashtable transformationNameMap = new Hashtable();
  private final String mainTableNameKey = "mainTable";

  /**
   * Empty constructor.  Should really only be used by the MartBuilder
   */
  public DatasetBase() {
    super();
    setAttribute(mainTableNameKey, null);
  }

  /**
   * Constructor for an Transformation represented by internalName internally.
   * 
   * @param internalName String name to internally represent the Transformation
   */
  public DatasetBase(String internalName) throws ConfigurationException {
	  this(internalName,"");
  }


  /**
   * Constructs a Dataset named by internalName.
   * internalName is a single word that references this dataset, 
   * used to get the dataset from the TransformationConfiguration by name.
   * 
   * @param internalName String name to represent this Dataset
   */
  public DatasetBase(String internalName, String mainTable) throws ConfigurationException {
    super(internalName);
    
    setAttribute(mainTableNameKey, mainTable);
  }
  

  

  public String getMainTable() {
	return getAttribute(mainTableNameKey);
  }

  
  
  
  
  /**
   * Copy constructor. Constructs an exact copy of an existing Dataset.
   * @param dataset Dataset to copy.
   */
  public DatasetBase(DatasetBase dataset) {
	super (dataset);
  	
	TransformationBase[] transformations = dataset.getTransformations();
	for (int i = 0, n = transformations.length; i < n; i++) {
	  Object transformation = transformations[i];
	  addTransformation( new TransformationBase( (TransformationBase) transformation));     
	}
  }

  /**
   * Add a Transformation to the Dataset.
   * @param transformation Transformation object.
   */
  public void addTransformation(TransformationBase transformation) {
    transformations.add(transformation);
    transformationNameMap.put(transformation.getInternalName(), transformation);
  }

  /**
   * Remove a Transformation from the Dataset.
   * @param transformation -- Transformation to be removed.
   */
  public void removeTransformation(TransformationBase transformation) {
    transformationNameMap.remove(transformation.getInternalName());
    transformations.remove(transformation);
  }

  /**
   * Insert a Transformation at a specific Position within the Transformation list.
   * Transformations at or after the given position will be shifted right).
   * @param position -- Position to insert the Transformation
   * @param f -- Transformation to insert.
   */
  public void insertTransformation(int position, TransformationBase transformation) {
    transformations.add(position, transformation);
    transformationNameMap.put(transformation.getInternalName(), transformation);
  }

  /**
   * Add a group of Transformation objects in one call.
   * Note, subsequent calls to addTransformation or addTransformations
   * will add to what has been added before.
   * 
   * @param transformation Transformation[] array of Transformation objects.
   */
  public void addTransformations(TransformationBase[] transformation) {
    for (int i = 0, n = transformation.length; i < n; i++) {
      transformations.add(transformation[i]);
      transformationNameMap.put(transformation[i].getInternalName(), transformation);
    }
  }

  /**
   * Returns a list of all Transformation objects contained within the Dataset, in the order they were added.
   * @return Transformation[]
   */
  public TransformationBase[] getTransformations() {
    TransformationBase[] fs = new TransformationBase[transformations.size()];
    transformations.toArray(fs);
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
   * Allows Equality Comparisons manipulation of Dataset objects.
   * Note, currently does not use Message Digest information.
   * Also, If the lazy load fails, a RuntimeException is thrown.
   */
  public boolean equals(Object o) {
    return o instanceof DatasetBase && hashCode() == ((DatasetBase) o).hashCode();
  }

}
