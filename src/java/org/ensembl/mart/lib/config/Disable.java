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
  * Allows a FilterDescription Object to code whether to disable another FilterDescription Object
  * in the UI, possibly based on a particular value of the disabling FilterDescription.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Disable extends BaseConfigurationObject {
	private final String refKey = "ref";
	private final String valueConditionKey = "valueCondition";
	
	/**
	 * Copy Constructor. Constructs a new Disable that is a
	 * exact copy of an existing Disable.
	 * @param d Disable Object to copy.
	 */
	public Disable(Disable d) {
    super(d);
	}
	
  /**
   * Empty Constructor should only be used by DatasetConfigEditor
   */
  public Disable() {
  	super();
    
    setAttribute(refKey, null);
    setAttribute(valueConditionKey, null);
  }
  
	/**
	 * Construct a Disable Object with a ref.
	 * @param ref - String internalName of the FilterDescription to disable.
	 * @throws ConfigurationException when ref is null or empty.
	 */
	public Disable(String ref) throws ConfigurationException {
		this(ref, null);
	}
  
	/**
	 * Construct a Disable Object with a ref, and a valueCondition.
	 * @param ref - String internalName of the FilterDescription to disable.
	 * @param valueCondition - String Condition for Value of the Enabling FilterDescription required for it to disable the referent FilterDescription.
	 * @throws ConfigurationException when ref is null or empty.
	 */
	public Disable(String ref, String valueCondition) throws ConfigurationException {
		super();
		
		if (ref == null || "".equals(ref))
			throw new ConfigurationException("Disable objects must have a ref.\n");
  	  
		setAttribute(refKey, ref);
		setAttribute(valueConditionKey, valueCondition);
	}

	/**
	 * Get the Reference for this Disable.  Refers to the internalName of a FilterDescription to Disable.
	 * @return String internalName of the referring FilterDescription.
	 */
	public String getRef() {
		return getAttribute(refKey);
	}

	/**
	 * Get the ValueCondition, if set.
	 * @return String valueCondition
	 */
	public String getValueCondition() {
		return getAttribute(valueConditionKey);
	}

	/**
	 * Set the internalName of the Filter to Disable when this Filter is used
	 * @param ref -- internalName of the filter to enable
	 */
	public void setRef(String ref) {
		setAttribute(refKey, ref);
	}

	/**
	 * Set a value at which the referenced Filter should be enabled.
	 * @param valueCondition -- value at which the referenced Filter should be enabled.
	 */
	public void setValueCondition(String valueCondition) {
		setAttribute(valueConditionKey, valueCondition);
	}
		  
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of Disable objects
	 */
	public boolean equals(Object o) {
		return o instanceof Disable && hashCode() == o.hashCode();
	}
	
	/**
	 * always false
	 */
	public boolean isBroken() {
		return false;
	}
}
