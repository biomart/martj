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

	/*
	 * UIFilterDescriptions require a internalName, fieldName, type, and qualifier.  disable parameterless constructor
	 */
	private UIFilterDescription() throws ConfigurationException {
		this("", "", "", "", "", "", ""); // this will never happen
	}

/**
 * Constructor for a UIFilterDescription named by internalName internally, with a fieldName, type, and qualifier.
 * 
 * @param internalName String internal name of the UIFilterDescription. Must not be null or empty.
 * @param fieldName String name of the field to reference in the mart. Must not be null or empty.
 * @param type String type of filter.  Must not be null or empty.
 * @param qualifier String qualifier to use in a SQL where clause. Must not be null or empty.
 * @throws ConfigurationException when required values are null or empty.
 */
  public UIFilterDescription(String internalName, String fieldName, String type,	String qualifier) throws ConfigurationException {
  	this(internalName, fieldName, type, qualifier, "", "", "");
  }
  
	/**
	 * Constructor for a UIFilterDescription named by internalName internally, with a fieldName, type, and qualifier.
	 * 
	 * @param internalName String internal name of the UIFilterDescription. Must not be null or empty.
	 * @param fieldName String name of the field to reference in the mart. Must not be null or empty.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param qualifier String qualifier to use in a SQL where clause. Must not be null displayName.
	 * @param displayName String name to display in a UI
	 * @param tableConstraint String table basename to constrain SQL fieldName
	 * @param description String description of the Filter
	 * @throws ConfigurationException when required values are null or empty.
	 */
	public UIFilterDescription(String internalName, String fieldName, String type,	String qualifier,	String displayName, String tableConstraint,	String description) throws ConfigurationException {
		if (internalName == null || internalName.equals("") 
		  || fieldName == null || fieldName.equals("")
			|| type == null || type.equals("")
			|| qualifier == null || qualifier.equals("")) 
			throw new ConfigurationException("UIFilterDescription requires a displayName, fieldName, type, and qualifier");

    this.internalName = internalName;
		this.displayName = displayName;
		this.fieldName = fieldName;
		this.type = type;
		this.qualifier = qualifier;
		this.tableConstraint = tableConstraint;
		this.description = description;
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
		buf.append(", description=").append(description);
		buf.append("]");

		return buf.toString();
	}

	private final String internalName, displayName,	fieldName,	type,	qualifier,	tableConstraint,	description;
}
