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

import java.util.ArrayList;
import java.util.List;

 /** 
  * Allows a FilterDescription Object to code whether to enable another FilterDescription Object
  * in the UI, possibly based on a particular value of the enabling FilterDescription.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Enable {
  private String ref;
  private String valueCondition;
	private List xmlAttributeTitles = new ArrayList();

	/**
	 * Copy Constructor. Constructs a new Enable that is a
	 * exact copy of an existing Enable.
	 * @param e Enable Object to copy.
	 */ 
  public Enable(Enable e) {
  	ref = e.getRef(); 
  	valueCondition = e.getValueCondition(); 
  }
  
  public Enable() {
  }
  
  /**
   * Construct an Enable Object with a ref.
   * @param ref - String internalName of the FilterDescription to enable.
   * @throws ConfigurationException when ref is null or empty.
   */
  public Enable(String ref) throws ConfigurationException {
  	this(ref, null);
  }
  
  /**
   * Construct an Enable Object with a ref, and a valueCondition.
   * @param ref - String internalName of the FilterDescription to enable.
   * @param valueCondition - String Condition for Value of the Enabling FilterDescription required for it to enable the referent FilterDescription.
   * @throws ConfigurationException when ref is null or empty.
   */
  public Enable(String ref, String valueCondition) throws ConfigurationException {
  	if (ref == null || "".equals(ref))
  	  throw new ConfigurationException("Enable objects must have a ref.\n");
  	  
  	this.ref = ref;
  	this.valueCondition = valueCondition;
  }

	/**
	 * Get the Reference for this Enable.  Refers to the internalName of a FilterDescription to Enable.
	 * @return String internalName of the referring FilterDescription.
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * Get the ValueCondition, if set.
	 * @return String valueCondition
	 */
	public String getValueCondition() {
		return valueCondition;
	}

  /**
   * Set the internalName of the Filter to Enable when this Filter is used
   * @param string -- internalName of the filter to enable
   */
  public void setRef(String string) {
    ref = string;
  }

  /**
   * Set a value at which the referenced Filter should be enabled.
   * @param string -- value at which the referenced Filter should be enabled.
   */
  public void setValueCondition(String string) {
    valueCondition = string;
  }

	/**
	 * Get the XML Attribute Titles for this object. This is meant for use
	 * by DatasetViewEditor.
	 * @return String[] List of XMLAttribute Titles.
	 */
	public String[] getXmlAttributeTitles() {
		String[] titles = new String[xmlAttributeTitles.size()];
		xmlAttributeTitles.toArray(titles);
		return titles;
	}

	/**
	 * Sets the XML Attribute Titles for this object.  This should equal
	 * the titles of all XML Attributes for the element. This is meant for use
	 * by DatasetViewEditor.
	 * @param list
	 */
	public void setXmlAttributeTitles(String[] list) {
		for (int i = 0, n = list.length; i < n; i++) {
			xmlAttributeTitles.add(list[i]);
		}
	}
	  
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("ref=").append(ref);
		buf.append(", valueCondition=").append(valueCondition);
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of Enable objects
	 */
	public boolean equals(Object o) {
		return o instanceof Enable && hashCode() == o.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
    int hshcode = ref.hashCode();
    hshcode = (valueCondition != null) ? (31 * hshcode) + valueCondition.hashCode() : hshcode;
		return hshcode;
  }
}
