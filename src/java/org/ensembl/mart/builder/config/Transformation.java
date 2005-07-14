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

/**
 * Container for a set of Mart AttributeCollections
 *   
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Transformation extends BaseNamedConfigurationObject {

  private List transformationUnits = new ArrayList();
  private Hashtable transUnitNameMap = new Hashtable();

  
  private final String includeCentralFilterKey = "includeCentralFilter";
  //private final String datasetKey = "dataset";
  private final String tableTypeKey = "tableType";
  private final String centralTableKey = "centralTable";
  private final String userTableNameKey = "userTableName";
  
  private int[] reqFields = {0,1,2,3};// rendered red in AttributeTable
  
  /**
   * Copy constructor. Constructs an exact copy of an existing Transformation.
   * @param ap Transformation to copy.
   */
  public Transformation(Transformation ap) {
  	super (ap);
  	
  	List agroups = ap.getTransformationUnits();
  	for (int i = 0, n = agroups.size(); i < n; i++) {
      Object group = agroups.get(i);
      addTransformationUnit( new TransformationUnit( (TransformationUnit) group ));     
    }
  }
   
/**
 * Empty Constructor should really only be used by the DatasetConfigEditor
 */
	public Transformation() {
		super();
		setAttribute(includeCentralFilterKey, null);
		//setAttribute(datasetKey, null);	
		setAttribute(tableTypeKey, null);
		setAttribute(centralTableKey, null);
		setAttribute(userTableNameKey, null);
		setRequiredFields(reqFields);			
	}

	/**
	 * Constructor for an Transformation represented by internalName internally.
	 * 
	 * @param internalName String name to internally represent the Transformation
	 * @throws ConfigurationException when the internalName is null or empty
	 */
	public Transformation(String internalName) throws ConfigurationException {
		this(internalName, "","","","");
	}

	/**
	 * Constructor for an Transformation named internally by internalName, with a 
	 * displayName and described by description.
	 * 
	 * @param internalName String name to internally represent the Transformation.  Must not be null.
	 * @param displayName String name to represent the Transformation
	 * @param description String description of the Transformation
	 * @throws ConfigurationException when the internalName is null or empty
	 */
	public Transformation(String internalName, String includeCentralFilter, String tableType, String centralTable, String userTableName) throws ConfigurationException {
		super(internalName);
		setAttribute(includeCentralFilterKey, includeCentralFilter);
		//setAttribute(datasetKey, dataset);	
		setAttribute(tableTypeKey, tableType);
		setAttribute(centralTableKey, centralTable);
		setAttribute(userTableNameKey, userTableName);
		setRequiredFields(reqFields);		
	}

	/**
	 * Add a single TransformationUnit to the Transformation.
	 * 
	 * @param a An TransformationUnit object
	 */
	public void addTransformationUnit(TransformationUnit a) {
		System.out.println("ADDING TUNIT");
		transformationUnits.add(a);
		transUnitNameMap.put(a.getInternalName(), a);
	}

  /**
   * Remove an TransformationUnit from the Transformation
   * @param a -- TransformationUnit to be removed.
   */
  public void removeTransformationUnit(TransformationUnit a) {
    transUnitNameMap.remove(a.getInternalName());
    transformationUnits.remove(a); 
  }
  
  /**
   * Insert an TransformationUnit at a particular position within the List of TransformationUnit/DSTransformationLine objects
   * contained in the Transformation. TransformationUnit/DSTransformationLine objects at or after this position are shifted right.
   * @param position -- position to insert the given TransformationUnit
   * @param a -- TransformationUnit to insert.
   */
  public void insertTransformationUnit(int position, TransformationUnit a) {
    transformationUnits.add(position, a);
    transUnitNameMap.put(a.getInternalName(), a);
  }
  
  /**
   * Insert an TransformationUnit before a specific TransformationUnit/DSTransformationLine, named by internalName.
   * @param internalName -- name of the TransformationUnit/DSTransformationLine before which the given TransformationUnit should be inserted.
   * @param a -- TransformationUnit to insert.
   * @throws ConfigurationException when the Transformation does not contain an TransformationUnit/DSTransformationLine named by internalName.
   */
  public void insertTransformationUnitBeforeTransformationUnit(String internalName, TransformationUnit a) throws ConfigurationException {
    if (!transUnitNameMap.containsKey(internalName))
      throw new ConfigurationException("TransformationUnit does not contain TransformationLine " + internalName + "\n");
    
    insertTransformationUnit( transformationUnits.indexOf( transUnitNameMap.get(internalName) ), a );
  }
  
  /**
   * Insert an TransformationUnit after a specific TransformationUnit/DSTransformationLine, named by internalName.
   * @param internalName -- name of the TransformationUnit/DSTransformationLine after which the given TransformationUnit should be inserted.
   * @param a -- TransformationUnit to insert.
   * @throws ConfigurationException when the Transformation does not contain an TransformationUnit/DSTransformationLine named by internalName.
   */
  public void insertTransformationUnitAfterTransformationUnit(String internalName, TransformationUnit a) throws ConfigurationException {
    if (!transUnitNameMap.containsKey(internalName))
      throw new ConfigurationException("TransformationUnit does not contain TransformationLine " + internalName + "\n");
    
    insertTransformationUnit( transformationUnits.indexOf( transUnitNameMap.get(internalName) ) + 1, a );
  }
  
	/**
	 * Add a group of TransformationUnit objects at once.  Note, subsequent calls
	 * to addTransformationLine or setTransformationLine will add to what has already been added.
	 * 
	 * @param a an array of TransformationUnit objects
	 */
	public void addTransformationUnits(TransformationUnit[] a) {
		for (int i = 0, n = a.length; i < n; i++) {
			transformationUnits.add(a[i]);
			transUnitNameMap.put(a[i].getInternalName(), a[i]);
		}
	}


	/**
	 * Returns a List of TransformationUnit/DSTransformationLine objects contained in the Transformation, in the order they were added.
	 * 
	 * @return A List of TransformationUnit/DSTransformationLine objects
	 */
	public List getTransformationUnits() {
    //return a copy
		return new ArrayList(transformationUnits);
	}

	/**
	 * Returns a specific TransformationUnit named by internalName.
	 * 
	 * @param internalName String name of the requested TransformationUnit
	 * @return an Object (either TransformationUnit or DSTransformationLine), or null
	 */
	public Object getTransformationUnitByName(String internalName) {
		if (transUnitNameMap.containsKey(internalName))
			return transUnitNameMap.get(internalName);
		else
			return null;
	}



	/**
	 * debug output
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append(", TransformationLines=").append(transformationUnits);
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of Transformation objects
	 */
	public boolean equals(Object o) {
		return o instanceof Transformation && hashCode() == ((Transformation) o).hashCode();
	}

	public int hashCode() {
		int tmp = super.hashCode();

		for (Iterator iter = transformationUnits.iterator(); iter.hasNext();) {
			Object element = iter.next();
		    tmp = (31 * tmp) + ((TransformationUnit) element).hashCode();
			
		}

		return tmp;
	}

  /**
   * Returns the outFormats for this att page.
   * 
   * @return key.
   */
  public String getIncludeCentralFilter() {
	return getAttribute(includeCentralFilterKey);
  }
	
  /**
   * @param key - outFormats for this att page
   */
  public void setIncludeCentralFilter(String outFormats) {
	 setAttribute(includeCentralFilterKey, outFormats);
  }

  /**
   * @param centralTable - centralTable for the tableType
   */
  public void setCentralTable(String centralTable) {
	setAttribute(centralTableKey, centralTable);
  }

  /**
   * Returns the CentralTable.
   * 
   * @return String centralTable.
   */
  public String getCentralTable() {
	return getAttribute(centralTableKey);
  }

  /**
   * @param centralTable - centralTable for the tableType
   */
  public void setUserTableName(String userTableName) {
	setAttribute(userTableNameKey, userTableName);
  }

  /**
   * Returns the CentralTable.
   * 
   * @return String centralTable.
   */
  public String getUserTableName() {
	return getAttribute(userTableNameKey);
  }
  
	/**
	 * @param tableType - tableType in mart table
	 */
	public void setTableType(String tableType) {
	  setAttribute(tableTypeKey, tableType);
	}

	/**
	 * Returns the tableType.
	 * 
	 * @return String tableType
	 */
	public String getTableType() {
	  return getAttribute(tableTypeKey);
	}
  
  
}
