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
  * Allows a FilterDescription Object to code whether to enable another FilterDescription Object
  * in the UI, possibly based on a particular value of the enabling FilterDescription.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class SeqModule extends BaseConfigurationObject {
  private final String linkNameKey = "linkName";
  private final String moduleNameKey = "moduleName";

	/**
	 * Copy Constructor. Constructs a new SeqModule that is a
	 * exact copy of an existing SeqModule.
	 * @param e SeqModule Object to copy.
	 */ 
  public SeqModule(SeqModule e) {
  	super(e); 
  }
  
  public SeqModule() {
  	super();
    
    setAttribute(linkNameKey, null);
    setAttribute(moduleNameKey, null);
  }
  
  /**
   * Construct an SeqModule Object with a ref.
   * @param ref - String internalName of the FilterDescription to SeqModule.
   * @throws ConfigurationException when ref is null or empty.
   */
  public SeqModule(String linkName) throws ConfigurationException {
  	this(linkName, null);
  }
  
  /**
   * Construct an SeqModule Object with a ref, and a valueCondition.
   * @param ref - String internalName of the FilterDescription to SeqModule.
   * @param valueCondition - String Condition for Value of the Enabling FilterDescription required for it to SeqModule the referent FilterDescription.
   * @throws ConfigurationException when ref is null or empty.
   */
  public SeqModule(String linkName, String moduleName) throws ConfigurationException {
  	super();
  	
  	if (linkName == null || "".equals(linkName))
  	  throw new ConfigurationException("SeqModule objects must have a linkName.\n");
  	  
  	setAttribute(linkNameKey, linkName);
  	setAttribute(moduleNameKey, moduleName);
  }

	/**
	 * Get the Reference for this SeqModule.  Refers to the internalName of a FilterDescription to SeqModule.
	 * @return String internalName of the referring FilterDescription.
	 */
	public String getLinkName() {
		return getAttribute(linkNameKey);
	}

	/**
	 * Get the ValueCondition, if set.
	 * @return String valueCondition
	 */
	public String getModuleName() {
		return getAttribute(moduleNameKey);
	}

  /**
   * Set the internalName of the Filter to SeqModule when this Filter is used
   * @param ref -- internalName of the filter to SeqModule
   */
  public void setLinkName(String ref) {
		setAttribute(linkNameKey, ref);
  }

  /**
   * Set a value at which the referenced Filter should be SeqModuled.
   * @param valueCondition -- value at which the referenced Filter should be SeqModuled.
   */
  public void setModuleName(String valueCondition) {
		setAttribute(moduleNameKey, valueCondition);
  }
	  
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of SeqModule objects
	 */
	public boolean equals(Object o) {
		return o instanceof SeqModule && hashCode() == o.hashCode();
	}

	/**
	 * always false
	 */
	public boolean isBroken() {
		return false;
	}
}
