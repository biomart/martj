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
 * Description of a Domain Specific Filter made available by an instance of a Mart Database.
 * Domain Specific Filters are handled in the Engine by a DomainSpecificFilterHandler implimenting object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see DomainSpecificFilterHandler
 */
public class UIDSFilterDescription {
	
	/**
	 * This will throw a ConfigurationException
	 * 
	 * @throws ConfigurationException
	 */
	public UIDSFilterDescription() throws ConfigurationException {
		this("", "", "", "", "", "");
	}
	
	/**
	 * Constructor for a minimal UIDSFilterDescription, with internalName, type, and objectCode set.
	 * 
	 * @param internalName - String name to internally reference this Description
	 * @param type - String type of UI Display
	 * @param objectCode - String type of DomainSpecificFilterHandler to use to resolve this Filter
	 * 
	 * @throws ConfigurationException when internalName or type are null, or when the objectCode == 0
	 * @see DomainSpecificFilterHandler
	 */
	public UIDSFilterDescription(String internalName, String type, String objectCode) throws ConfigurationException {
	  this(internalName, type, objectCode, "", "", "");	
	}

  /**
   * Constructor for a fully defined UIDSFilterDescription.
   * 
	 * @param internalName - String name to internally reference this FilterDescription
	 * @param type - String type of UI Display
	 * @param objectCode - String type of DomainSpecificFilterHandler to use to resolve this FilterDescription
	 * @param filterSetReq - String FilterSet Modification Requirement.  If this is not null, inFilterSet is set to true.
   * @param displayName - String name to display in a UI for this FilterDescription
   * @param description - String descriptive information for this FilterDescription
   * 
   * @throws ConfigurationException when internalName, type, or objectCode are null
   */
  public UIDSFilterDescription(String internalName, String type, String objectCode, String filterSetReq, String displayName, String description) throws ConfigurationException {
  	if (internalName == null || internalName.equals("")
  	  || type == null || type.equals("")
  	  || objectCode == null || objectCode.equals(""))
  	  throw new ConfigurationException("UIDSFilterDescription object must have an internalName, type and objectCode");
  	  
  	this.internalName = internalName;
  	this.type = type;
  	this.objectCode = objectCode;
  	
  	if (! ( filterSetReq == null || filterSetReq.equals("")  ) )
  	  inFilterSet = true;
  	  
  	this.filterSetReq = filterSetReq;
  	this.displayName = displayName;
  	this.description = description;
  	
  	//generate hashcode for immutable object
  	hshcode = inFilterSet ? 1 : 0;
		hshcode = (31 * hshcode) + internalName.hashCode();
		hshcode = (31 * hshcode) + displayName.hashCode();
		hshcode = (31 * hshcode) + type.hashCode();
	  hshcode = (31 * hshcode) + description.hashCode();
	  hshcode = (31 * hshcode) + objectCode.hashCode();
	  hshcode = (31 * hshcode) + filterSetReq.hashCode();
  }  
  
	/**
	 * Returns the Description of this UIDSFilterDescription.
	 * 
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the displayName of this UIDSFilterDescription
	 * 
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the internalName of this UIDSFilterDescription
	 * 
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the objectCode of this UIDSFilterDescription
	 * 
	 * @return String objectCode
	 */
	public String getObjectCode() {
		return objectCode;
	}

	/**
	 * Returns the Type of this UIDSFilterDescription
	 * 
	 * @return String type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns filterSetReq
	 * @return String filterSetReq
	 */
	public String getFilterSetReq() {
		return filterSetReq;
	}

	/**
	 * Check if this UIDSFilterDescription is in a FilterSet.
	 * @return boolean, true if filterSetReq is set, false if null or empty.
	 */
	public boolean IsInFilterSet() {
		return inFilterSet;
	}
  
  /**
   * Used for debugging output.
   */
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", type=").append(type);
		buf.append(", objectCode=").append(objectCode);
		
		if (inFilterSet)
		  buf.append(", filterSetReq=").append(filterSetReq);
		  
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows equality comparisons of UIDSFilterDescription objects
	 */
	public boolean equals(Object o) {
    return o instanceof UIDSFilterDescription && hashCode() == ( (UIDSFilterDescription) o).hashCode(); 
	}

	/**
	 * Allows Collections manipulation of UIDSFIlterDescription objects
	 */
	public int hashCode() {
		 return hshcode;
	}
	
  private final String internalName, displayName, description, type, objectCode, filterSetReq;
  private boolean inFilterSet = false;
  private int hshcode = 0;
}
