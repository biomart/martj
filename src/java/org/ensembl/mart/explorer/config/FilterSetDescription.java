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
 * Description for a specific FilterSet instance.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterSetDescription {
	
	/**
	 * If this gets called, it will throw a MartConfigurationException.
	 * 
	 * @throws ConfigurationException when internalName tableConstraintModifier, or fieldNameModifier is null.
	 */
	public FilterSetDescription() throws ConfigurationException {
		this("","","","", "");
	}
	
	/**
	 * Constructor for a basic FilterSetDescription with just the internalName, tableConstraintModifier
	 * and fieldNameModifier defined.
	 * 
	 * @param internalName - name to internally reference the FilterSetDescription
	 * @param tableConstraintModifier - string used to further qualify the tableConstraint for a UIFilterDescription.
	 * @param fieldNameModifier - string used to further qualify the fieldName for a UIFilterDescription.
	 * 
	 * @throws ConfigurationException when any of the three values are null.
	 */
	public FilterSetDescription(String internalName, String tableConstraintModifier, String fieldNameModifier) throws ConfigurationException {
		this(internalName, tableConstraintModifier, fieldNameModifier, "", "");
	}
	
	/**
	 * Constructor for a fully defined FilterSetDescription.
	 * 
	 * @param internalName - name to internally reference the FilterSetDescription.
	 * @param tableConstraintModifier - string used to further qualify the tableConstraint for a UIFilterDescription. 
	 * @param fieldNameModifier - string used to further qualify the fieldName for a UIFilterDescription.
	 * @param displayName - information to display in a UI
	 * @param description - description of the FilterSet.
	 * 
	 * @throws ConfigurationException when internalName, tableConstraintModifier, or fieldNameModifier are null or empty
	 */
	public FilterSetDescription(String internalName, String tableConstraintModifier, String fieldNameModifier, String displayName, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals("")
		    || tableConstraintModifier == null || tableConstraintModifier.equals("")
		    || fieldNameModifier == null || fieldNameModifier.equals(""))
		    throw new ConfigurationException("FilterSets must be defined with an internalName, tableConstraintModifier, and fieldNameModifier");
		    
		this.internalName = internalName;
		this.tableConstraintModifier = tableConstraintModifier;
		this.fieldNameModifier = fieldNameModifier;
		this.displayName = displayName;
		this.description = description;
		
		thisHashCode = internalName.hashCode();
		thisHashCode = (31 * thisHashCode) + tableConstraintModifier.hashCode();
		thisHashCode = (31 * thisHashCode) + fieldNameModifier.hashCode();
		thisHashCode = (31 * thisHashCode) + displayName.hashCode();
		thisHashCode = (31 * thisHashCode) + description.hashCode(); 
	}
	
	/**
	 * Returns the description of the FilterSetDescription.
	 * 
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the displayName to represent the FilterSetDescription in a UI
	 * 
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the internalName to internally represent the FilterSetDescription
	 * 
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the tableConstraintModifier.
	 * 
	 * @return String tableConstraintModifier
	 */
	public String getTableConstraintModifier() {
		return tableConstraintModifier;
	}

	/**
	 * Returns the fieldNameModifier.
	 * 
	 * @return String fieldNameModifier
	 */
	public String getFieldNameModifier() {
		return fieldNameModifier;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("[");
		buf.append("internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", tableConstraintModifier=").append(tableConstraintModifier);
		buf.append(", fieldNameModifier=").append(fieldNameModifier);
		buf.append("[");
		
		return buf.toString();
	}
	
	/**
	 * equals method for direct configuration object comparisons, and list operations.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof FilterSetDescription))
			return false;

		FilterSetDescription otype = (FilterSetDescription) o;
		
		if (! ( internalName.equals( otype.getInternalName() ) ) )
		  return false;
		  
		if (! ( displayName.equals( otype.getDisplayName() ) ) )
		  return false;
		  
		if (! ( tableConstraintModifier.equals( otype.getTableConstraintModifier() ) ) )
      return false;
    
    if (! ( fieldNameModifier.equals( otype.getFieldNameModifier() ) ) )
      return false;
    
    if (! ( description.equals( otype.getDescription() ) ) )
      return false;
      
		return true;
	}
	
	/**
	 * hashCode for direct configuration object comparisons, and list operations.
	 */
	public int hashCode() {
		return thisHashCode;
	}
	
	private final String internalName, tableConstraintModifier, fieldNameModifier, displayName, description;
	private int thisHashCode = 0;
	
	//static enums for UIFilterDescription FilterSetRequirements
	public static final String MODFIELDNAME = "field";
	public static final String MODTABLECONSTRAINT = "table";
}
