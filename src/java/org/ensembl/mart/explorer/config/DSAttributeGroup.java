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
 * Place Holder object to signal to the UI where to render complex
 * Domain Specific Attribute widgets that do not fit the generic
 * MartConfiguration scheme.  Does not contain any lower level objects.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DSAttributeGroup {

	/**
	 * This will throw a ConfigurationException
	 * @throws ConfigurationException
	 */
	public DSAttributeGroup() throws ConfigurationException {
		this("", "", "", "");
	}
  
	/**
	 * Constructor for a minimal DSAttributeGroup, with internalName set.
	 * @param internalName - String name to internally represent the object.
	 * @throws ConfigurationException when internalName is null or empty
	 */
	public DSAttributeGroup(String internalName) throws ConfigurationException {
		this(internalName, "", "", "");
	}
  
	/**
	 * Constructor for a fully qualified DSAttributeGroup
	 * @param internalName - String name to internally represent the object.
	 * @param displayName - String name to display in a UI
	 * @param description - String description of the DSAttributeGroup
	 * @param objectCode - String signal to the UI to determine a rendering module
	 * @throws ConfigurationException when internalName is null or empty
	 */
	public DSAttributeGroup(String internalName, String displayName, String description, String objectCode) throws ConfigurationException {
		if (internalName == null || internalName.equals(""))
			throw new ConfigurationException("DSAttributeGroup objects must be initialized with an internalName\n");
  	
		this.internalName = internalName;
		hashcode = internalName.hashCode();
		this.displayName = displayName;
		hashcode = (31 * hashcode) + displayName.hashCode();
		this.description = description;
		hashcode = (31 * hashcode) + description.hashCode();
		this.objectCode = objectCode;
		hashcode = (31 * hashcode) + objectCode.hashCode();
	}
	
	/**
	 * Returns the description
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the displayName
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the internalName
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the objectCode
	 * @return Sring objectCode
	 */
	public String getObjectCode() {
		return objectCode;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" DSAttributeGroup: internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", objectCode=").append(objectCode);
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of DSAttributeGroup objects
	 */
	public boolean equals(Object o) {
		return o instanceof DSAttributeGroup && hashCode() == ((DSAttributeGroup) o).hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return hashcode;
	}
	
  private final String internalName, displayName, description, objectCode;
  private int hashcode = 0;
}
