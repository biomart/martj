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
 
package org.ensembl.mart.shell;

/**
 * Mapper object allowing simple internalName -> value or key + composite_key -> internalName
 * mappings for UIObjects.  key is generic, but should be a string that, either alone or in
 * combination with a composite_key, uniquely maps to an internalName.  The system is designed such
 * that UIMappers constructed with all 3 parameters will only return true for the two argument canMap
 * method, while UIMappers constructed with 2 parameters will only return true for the one argument canMap
 * method.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class UIMapper {
	/**
	 * Constructor for a Mapper between a primary_key and internalName, with no composite_key
	 * 
	 * @param primary_key - String primary_key for the object mapping to the given internalName
	 * @param internalName - String internal name of mapping object
	 */
  public UIMapper(String primary_key, String internalName) {
  	this(primary_key, "", internalName);
  }
  
	/**
	 * Constructor for a Mapper between a primary_key + composite_key, and internalName
	 * 
	 * @param primary_key - String primary_key for the object
	 * @param composite_key - String composite_key which, with the primary_key, maps to the object with the given internalName
	 * @param internalName - String internal name of mapping object
	 */
  public UIMapper(String primary_key, String tableConstraint, String internalName) {
  	this.primary_key = primary_key;
  	int tmp = this.primary_key.hashCode();
  	
  	this.composite_key = tableConstraint;
  	tmp = (31 * tmp) + this.composite_key.hashCode();
  	
  	this.internalName = internalName;
		tmp = (31 * tmp) + this.internalName.hashCode();
		
		hashcode = tmp;
  }

	/**
	 * Returns the primary_key for this UIMapper
	 * @return String primary_key
	 */
	public String getPrimaryKey() {
		return primary_key;
	}

	/**
	 * Returns the composite_key for this UIMapper
	 * @return String composite_key
	 */
	public String getCompositeKey() {
		return composite_key;
	}
  
  /**
   * Determine if the UIMapper can map to a primary_key alone.  Returns false if the UIMapper was created with a 
   * non null composite_key, or if the given primary_key is not equal to the UIMapper primary_key.  Returns true if composite_key
   * is empty, and the given primary_key equals the UIMapper primary_key.
   * 
   * @param primary_key - String key to map to an internalName
   * @return boolean, true if given primary_key equals UIMapper primary_key and UIMapper composite_key is empty, false otherwise
   */
  public boolean canMap(String primary_key) {
    if (! composite_key.equals(""))
      return false;
    return this.primary_key.equals(primary_key);
  }
  
  /**
   * Determine if the UIMapper can map to a primary_key, composite_key combination.  Returns false if the UIMapper was created with an empty
   * composite_key, or when the given primary_key and composite_key do not equal the internal primary_key, and composite_key respectively.
   * Returns true if composite_key is not null and given primary_key equals internal primary_key, given composite_key equals internal composite_key.
   * @param primary_key - String key to map, in combination with a composite_key to an internalName
   * @param composite_key - String composite_key to map, in combination with a primary_key, to an internalName
   * @return boolean, true if composite_key is not empty, given primary_key equals internal primary_key, and given composite_key equals internal composite_key. False otherwise.
   */
  public boolean canMap(String primary_key, String tableConstraint) {
  	return ( !(this.composite_key.equals("")) && this.composite_key.equals(tableConstraint) && this.primary_key.equals(primary_key));
  }
  
  /**
   * Retuns the internalName that this UIMapper maps to.
   * @return String internalName
   */
  public String getInternalName() {
  	return internalName;
  }
  
  /**
	 * Allows Equality Comparisons manipulation of UIMapper objects
	 */
	public boolean equals(Object o) {
		return o instanceof UIMapper && hashCode() == ((UIMapper) o).hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
     return hashcode;
	}

  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		if ( !composite_key.equals(""))
		  buf.append(" primary_key ").append(primary_key).append(" + composite_key ").append(composite_key).append(" -> internalName ").append(internalName);
		else
		  buf.append(" primary_key ").append(primary_key).append(" -> internalName ").append(internalName);
		buf.append("]");

		return buf.toString();
	}

  private final String primary_key, composite_key, internalName;
  private final int hashcode;
}
