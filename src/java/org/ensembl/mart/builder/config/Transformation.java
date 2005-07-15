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
 * 
 * Contains all of the information required by a UI to display a transformation,
 * Container for a set of TransformationUnit objects
 *
 *   
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */
public class Transformation extends BaseNamedConfigurationObject {

  private List transformationUnits = new ArrayList();
  private Hashtable transUnitNameMap = new Hashtable();
  
  private final String includeCentralFilterKey = "includeCentralFilter";
  private final String tableTypeKey = "tableType";
  private final String centralTableKey = "centralTable";
  private final String userTableNameKey = "userTableName";
  
  private int[] reqFields = {0,1,2,3};// rendered red in AttributeTable
  
  /**
   * Copy constructor. Constructs an exact copy of an existing Transformation.
   * @param transformation Transformation to copy.
   */
  public Transformation(Transformation transformation) {
  	super (transformation);
  	
  	List transformationUnits = transformation.getTransformationUnits();
  	for (int i = 0, n = transformationUnits.size(); i < n; i++) {
      Object transformationUnit = transformationUnits.get(i);
      addTransformationUnit( new TransformationUnit( (TransformationUnit) transformationUnit ));     
    }
  }
   
/**
 * Empty Constructor should really only be used by the MartBuilder
 */
	public Transformation() {
		super();
		setAttribute(includeCentralFilterKey, null);
		setAttribute(tableTypeKey, null);
		setAttribute(centralTableKey, null);
		setAttribute(userTableNameKey, null);
		setRequiredFields(reqFields);			
	}

	/**
	 * Constructor for an Transformation represented by internalName internally.
	 * 
	 * @param internalName String name to internally represent the Transformation
	 */
	public Transformation(String internalName) throws ConfigurationException {
		this(internalName, "","","","");
	}

	/**
	 * Constructor for an Transformation named internally by internalName, with a 
	 * displayName and described by description.
	 * 
	 * @param internalName String name to internally represent the Transformation.  Must not be null.
	 * @param tableType
	 * @param centralTable
	 * @param userTableName
	 * @throws ConfigurationException when the internalName is null or empty
	 */
	public Transformation(String internalName, String includeCentralFilter, String tableType, String centralTable, String userTableName) throws ConfigurationException {
		super(internalName);
		setAttribute(includeCentralFilterKey, includeCentralFilter);	
		setAttribute(tableTypeKey, tableType);
		setAttribute(centralTableKey, centralTable);
		setAttribute(userTableNameKey, userTableName);
		setRequiredFields(reqFields);		
	}

	/**
	 * Add a single TransformationUnit to the Transformation.
	 * 
	 * @param a TransformationUnit object
	 */
	public void addTransformationUnit(TransformationUnit transformationUnit) {
		transformationUnits.add(transformationUnit);
		transUnitNameMap.put(transformationUnit.getInternalName(), transformationUnit);
	}

  /**
   * Remove an TransformationUnit from the Transformation
   * @param transformationUnit -- TransformationUnit to be removed.
   */
  public void removeTransformationUnit(TransformationUnit transformationUnit) {
    transUnitNameMap.remove(transformationUnit.getInternalName());
    transformationUnits.remove(transformationUnit); 
  }
  
  /**
   * Insert an TransformationUnit at a particular position within the List of TransformationUnit/DSTransformationLine objects
   * contained in the Transformation. TransformationUnit/DSTransformationLine objects at or after this position are shifted right.
   * @param position -- position to insert the given TransformationUnit
   * @param transformationUnit -- TransformationUnit to insert.
   */
  public void insertTransformationUnit(int position, TransformationUnit transformationUnit) {
    transformationUnits.add(position, transformationUnit);
    transUnitNameMap.put(transformationUnit.getInternalName(), transformationUnit);
  }
   
	/**
	 * Add a group of TransformationUnit objects at once.  Note, subsequent calls
	 * to addTransformationLine or setTransformationLine will add to what has already been added.
	 * 
	 * @param transformationUnit an array of TransformationUnit objects
	 */
	public void addTransformationUnits(TransformationUnit[] tUnits) {
		for (int i = 0, n = tUnits.length; i < n; i++) {
			transformationUnits.add(tUnits[i]);
			transUnitNameMap.put(tUnits[i].getInternalName(), tUnits[i]);
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
	 * debug output
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append(", TransformationUnits=").append(transformationUnits);
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
   * Returns the includeCentralFilter for this Transformation.
   * 
   * @return String includeCentralFilter.
   */
  public String getIncludeCentralFilter() {
	return getAttribute(includeCentralFilterKey);
  }
	
  /**
   * @param String includeCentralFilter
   */
  public void setIncludeCentralFilter(String outFormats) {
	 setAttribute(includeCentralFilterKey, outFormats);
  }

  /**
   * @param String centralTable
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
   * @param String userTableName
   */
  public void setUserTableName(String userTableName) {
	setAttribute(userTableNameKey, userTableName);
  }

  /**
   * Returns the userTableName
   * 
   * @return String userTableName.
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
