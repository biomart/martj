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

package org.ensembl.mart.explorer.config;

/**
 * Contains all of the information necessary for the UI to display the information for a specific filter,
 * and add this filter as a Filter to a Query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class UIFilterDescription {

	/**
	 * This will throw a ConfigurationException.
	 */
	public UIFilterDescription() throws ConfigurationException {
		this("", "", "", "", "", "", 0, "");
	}

/**
 * Constructor for a UIFilterDescription named by internalName internally, with a fieldName, type, and qualifier.
 * 
 * @param internalName String internal name of the UIFilterDescription. Must not be null or empty.
 * @param fieldName String name of the field to reference in the mart. Must not be null or empty.
 * @param type String type of filter.  Must not be null or empty.
 * @param qualifier String qualifier to use in a SQL where clause.
 * @throws ConfigurationException when required values are null or empty, or when a filterSetName is set, but no filterSetReq is submitted.
 */
  public UIFilterDescription(String internalName, String fieldName, String type,	String qualifier) throws ConfigurationException {
  	this(internalName, fieldName, type, qualifier, "", "", 0, "");
  }
  
	/**
	 * Constructor for a UIFilterDescription named by internalName internally, with a fieldName, type, and qualifier.
	 * 
	 * @param internalName String internal name of the UIFilterDescription. Must not be null or empty.
	 * @param fieldName String name of the field to reference in the mart. Must not be null or empty.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param qualifier String qualifier to use in a SQL where clause.
	 * @param displayName String name to display in a UI
	 * @param tableConstraint String table basename to constrain SQL fieldName
	 * @param filterSetReq int, which of the modifications specified by a FilterSetDescription are required by this UIFilterDescription
	 * @param description String description of the Filter
	 * 
	 * @throws ConfigurationException when required values are null or empty, or when a filterSetName is set, but no filterSetReq is submitted.
	 * @see FilterSet, FilterDescription
	 */
	public UIFilterDescription(String internalName, String fieldName, String type,	String qualifier,	String displayName, String tableConstraint,	int filterSetReq, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals("") 
		  || fieldName == null || fieldName.equals("")
			|| type == null || type.equals("")) 
			throw new ConfigurationException("UIFilterDescription requires a displayName, fieldName, type, and qualifier");
      
        this.internalName = internalName;
		this.displayName = displayName;
		this.fieldName = fieldName;
		this.type = type;
		this.qualifier = qualifier;
		this.tableConstraint = tableConstraint;
		this.filterSetReq = filterSetReq;
		
		if (filterSetReq > 0)
		  inFilterSet = true;
		  
		this.description = description;
		
		hshcode = internalName.hashCode();
		hshcode = (31 * hshcode) + displayName.hashCode();
		hshcode = (31 * hshcode) + fieldName.hashCode();
		hshcode = (31 * hshcode) + type.hashCode();
		hshcode = (31 * hshcode) + qualifier.hashCode();
		hshcode = (31 * hshcode) + tableConstraint.hashCode();
		hshcode = (31 * hshcode) + filterSetReq;
        hshcode = (31 * hshcode) + description.hashCode();
	}

/**
 * Returns the InternalName of the UIFilterDescription.
 * 
 * @return String internalName
 */
	public String getInternalName() {
		return internalName;
	}

/**
 * Returns the displayName of the UIFilterDescription.
 * 
 * @return String displayName
 */
  public String getDisplayName() {
  	return displayName;
  }
  
  /**
   * returns the fieldName.
   * @return String fieldName
   */
	public String getFieldName() {
		return fieldName;
	}

/**
 * Returns the type.
 * 
 * @return String type.
 */
	public String getType() {
		return type;
	}

  /**
   * Returns the qualifier to use SQL where clause.
   * 
   * @return String qualifier
   */
	public String getQualifier() {
		return qualifier;
	}

	/**
		 * Returns the tableConstraint for the fieldName.
		 * 
		 * @return String tableConstraint
		 */
	public String getTableConstraint() {
		return tableConstraint;
	}
  
  /**
   * Returns a value to determine which UIFilterDescription SQL specifier (tableConstraint or fieldName) to modify
   * with contents from the FilterSetDescription.  Must match one of the static ints defined by FilterSetDescription.
   * 
   * @return int filterSetReq
   * @see FilterSetDescription
   */
  public int getFilterSetReq() {
  	return filterSetReq;
  }
  
  /**
   * Check to see if ths UIFilterDescription is in a FilterSet
   * 
   * @return true if it is in a FilterSet, false if not
   */
  public boolean inFilterSet() {
  	return inFilterSet;
  }
  
/**
 * Returns the description.
 * 
 * @return String description
 */
	public String getDescription() {
		return description;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", fieldName=").append(fieldName);
		buf.append(", type=").append(type);
		buf.append(", qualifier=").append(qualifier);
		buf.append(", tableConstraint=").append(tableConstraint);
		
		if (inFilterSet)
		  buf.append(", filterSetReq=").append(filterSetReq);
		  
		buf.append(", description=").append(description);
		buf.append("]");

		return buf.toString();
	}

  public boolean equals(Object o) {
		if (!(o instanceof UIFilterDescription))
			return false;

		UIFilterDescription otype = (UIFilterDescription) o;
		
		if (! (internalName.equals(otype.getInternalName()) ) )
			return false;
	  
		if (! (displayName.equals(otype.getDisplayName()) ) )
			return false;

		if (! ( fieldName.equals(otype.getFieldName() ) ) )
					return false;

		if (! ( type.equals(otype.getType() ) ) )
					return false;

		if (! ( qualifier.equals(otype.getQualifier() ) ) )
					return false;

		if (! ( tableConstraint.equals(otype.getTableConstraint() ) ) )
					return false;
		
		if (! ( filterSetReq == otype.getFilterSetReq() ) )
		  return false;
		  									  
		if (! (description.equals(otype.getDescription()) ) )
			return false;

        if (inFilterSet && ! (otype.inFilterSet()) )
          return false;
          
        if (otype.inFilterSet() && ! ( inFilterSet ) )
          return false;
          
		return true;
	}
	
	public int hashCode() {
     return hshcode;
	}
	
	private final String internalName, displayName,	fieldName,	type,	qualifier,	tableConstraint, description;
	private final int filterSetReq;
	private boolean inFilterSet = false;
	private int hshcode = 0;
}
