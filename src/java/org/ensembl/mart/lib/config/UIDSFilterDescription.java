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
 
package org.ensembl.mart.lib.config;

/**
 * Description of a Domain Specific Filter made available by an instance of a Mart Database.
 * Domain Specific Filters are handled in the Engine by a DomainSpecificFilterHandler implimenting object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see org.ensembl.mart.lib.DSFilterHandler
 */
public class UIDSFilterDescription extends BaseConfigurationObject {
	
	/**
	 * This will throw a ConfigurationException
	 * 
	 * @throws ConfigurationException
	 */
	public UIDSFilterDescription() throws ConfigurationException {
		this("", "", "", "", "", "", "");
	}
	
	/**
	 * Constructor for a minimal UIDSFilterDescription, with internalName, type, and handler set.
	 * 
	 * @param internalName - String name to internally reference this Description
	 * @param type - String type of UI Display
	 * @param handler - String type of DomainSpecificFilterHandler to use to resolve this Filter
	 * 
	 * @throws ConfigurationException when internalName or type are null, or when the handler == 0
	 */
	public UIDSFilterDescription(String internalName, String type, String handler) throws ConfigurationException {
	  this(internalName, type, handler, "", "", "", "");	
	}

  /**
   * Constructor for a fully defined UIDSFilterDescription.
   * 
	 * @param internalName - String name to internally reference this FilterDescription
	 * @param type - String type of UI Display
	 * @param handler - String type of DomainSpecificFilterHandler to use to resolve this FilterDescription
	 * @param filterSetReq - String FilterSet Modification Requirement.  If this is not null, inFilterSet is set to true.
   * @param displayName - String name to display in a UI for this FilterDescription
   * @param description - String descriptive information for this FilterDescription
   * @param optionName String name represention an Option that holds Options for this UIDSFilterDescription
   * 
   * @throws ConfigurationException when internalName, type, or handler are null
   */
  public UIDSFilterDescription(String internalName, String type, String handler, String filterSetReq, String displayName, String description, String optionName) throws ConfigurationException {
  	
    super( internalName, displayName, description );
    
    if ( type == null || type.equals("")
  	  || handler == null || handler.equals(""))
  	  throw new ConfigurationException("UIDSFilterDescription object must have a type and handler");
  	  
  	this.type = type;
  	this.handler = handler;
  	
  	if (! ( filterSetReq == null || filterSetReq.equals("")  ) )
  	  inFilterSet = true;
  	  
  	this.filterSetReq = filterSetReq;
  	this.optionName = optionName;
  	
  	//generate hashcode for immutable object
  	hshcode = inFilterSet ? 1 : 0;
		hshcode = (31 * hshcode) + this.internalName.hashCode();
		hshcode = (31 * hshcode) + this.displayName.hashCode();
		hshcode = (31 * hshcode) + this.type.hashCode();
	  hshcode = (31 * hshcode) + this.description.hashCode();
	  hshcode = (31 * hshcode) + this.handler.hashCode();
	  hshcode = (31 * hshcode) + this.filterSetReq.hashCode();
		hshcode = (31 * hshcode) + this.optionName.hashCode();
  }  
  
	/**
	 * Returns the handler of this UIDSFilterDescription
	 * 
	 * @return String handler
	 */
	public String getHandler() {
		return handler;
	}

	/**
	 * Returns the optionName
	 * @return String optionName
	 */
	public String getOptionName() {
		return optionName;
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

		buf.append("[ UIDSFilterDescription:");
		buf.append( super.toString() );
		buf.append(", type=").append(type);
		buf.append(", handler=").append(handler);
		
		if (inFilterSet)
		  buf.append(", filterSetReq=").append(filterSetReq);
		  
		buf.append(", optionName=").append(optionName);
		  
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
	
  private String type;
  private String handler;
  private String filterSetReq;
  private String optionName;
  private boolean inFilterSet = false;
  private int hshcode = 0;
}
