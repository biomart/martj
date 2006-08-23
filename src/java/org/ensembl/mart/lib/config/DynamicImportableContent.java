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
  * 
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */
public class DynamicImportableContent extends BaseNamedConfigurationObject {
  private final String linkNameKey = "linkName";
  private final String linkVersionKey = "linkVersion";
  private final String nameKey = "name";
  private final String filtersKey = "filters";
  private final String orderByKey = "orderBy";
  private int[] reqFields = {0,5,7,8};// rendered red in AttributeTable
 
	/**
	 * Copy Constructor. Constructs a new DynamicImportableContent that is a
	 * exact copy of an existing DynamicImportableContent.
	 * @param e DynamicImportableContent Object to copy.
	 */ 
  public DynamicImportableContent(DynamicImportableContent e) {
  	super(e); 
  }
  
  public DynamicImportableContent() {
  	super();
    
    setAttribute(linkNameKey, null);
	setAttribute(linkVersionKey, null);
    setAttribute(nameKey, null);
	setAttribute(filtersKey, null);
    setAttribute(orderByKey, null);
    setRequiredFields(reqFields);
  }

  public DynamicImportableContent(String linkName)  throws ConfigurationException {
    this(linkName, null, null, linkName, null, null, null, null);
  }
  
  /**
   * Construct an DynamicImportableContent Object with a ref.
   * @param ref - String internalName of the FilterDescription to DynamicImportableContent.
   * @throws ConfigurationException when ref is null or empty.
   */
  public DynamicImportableContent(String internalName, String displayName, String description, String linkName) throws ConfigurationException {
  	this(internalName, displayName, description, linkName, null, null, null, null);
  }
  
  /**
   * Construct an DynamicImportableContent Object with a ref, and a valueCondition.
   * @param ref - String internalName of the FilterDescription to DynamicImportableContent.
   * @param valueCondition - String Condition for Value of the Enabling FilterDescription required for it to DynamicImportableContent the referent FilterDescription.
   * @throws ConfigurationException when ref is null or empty.
   */
  public DynamicImportableContent(String internalName, String displayName, String description, String linkName, 
  		String linkVersion, String moduleName, String filters, String orderBy) throws ConfigurationException {
  	super(internalName, displayName, description);
  	
  	if (linkName == null || "".equals(linkName))
  	  throw new ConfigurationException("DynamicImportableContent objects must have a linkName.\n");
  	  
  	setAttribute(linkNameKey, linkName);
	setAttribute(linkVersionKey, linkVersion);
  	setAttribute(nameKey, moduleName);
	setAttribute(filtersKey, filters);
    setAttribute(orderByKey, orderBy);
    setRequiredFields(reqFields);
  }

	/**
	 * Get the Reference for this DynamicImportableContent.  Refers to the internalName of a FilterDescription to DynamicImportableContent.
	 * @return String internalName of the referring FilterDescription.
	 */
	public String getLinkName() {
		return getAttribute(linkNameKey);
	}
	
	/**
	 * Get the Reference for this DynamicImportableContent.  Refers to the internalName of a FilterDescription to DynamicImportableContent.
	 * @return String internalName of the referring FilterDescription.
	 */
	public String getLinkVersion() {
		return getAttribute(linkVersionKey);
	}

	/**
	 * Get the ValueCondition, if set.
	 * @return String valueCondition
	 */
	public String getName() {
		return getAttribute(nameKey);
	}

	/**
	 * Get the ValueCondition, if set.
	 * @return String valueCondition
	 */
	public String getFilters() {
		return getAttribute(filtersKey);
	}
  
  public String getOrderBy() {
    return getAttribute(orderByKey);
  }

  /**
   * Set the internalName of the Filter to DynamicImportableContent when this Filter is used
   * @param ref -- internalName of the filter to DynamicImportableContent
   */
  public void setLinkName(String ref) {
		setAttribute(linkNameKey, ref);
  }
  
  /**
   * Set the internalName of the Filter to DynamicImportableContent when this Filter is used
   * @param ref -- internalName of the filter to DynamicImportableContent
   */
  public void setLinkVersion(String ref) {
		setAttribute(linkVersionKey, ref);
  }  

  /**
   * Set a value at which the referenced Filter should be DynamicImportableContentd.
   * @param valueCondition -- value at which the referenced Filter should be DynamicImportableContentd.
   */
  public void setName(String valueCondition) {
		setAttribute(nameKey, valueCondition);
  }
	
  /**
   * Set a value at which the referenced Filter should be DynamicImportableContentd.
   * @param valueCondition -- value at which the referenced Filter should be DynamicImportableContentd.
   */
  public void setFilters(String valueCondition) {
		setAttribute(filtersKey, valueCondition);
  }

  public void setOrderBy(String orderBy) {
    	setAttribute(orderByKey, orderBy);  
  }
  
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of DynamicImportableContent objects
	 */
	public boolean equals(Object o) {
		return o instanceof DynamicImportableContent && hashCode() == o.hashCode();
	}

	/**
	 * always false
	 */
	public boolean isBroken() {
		return false;
	}
}
