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

import java.util.Iterator;
import java.util.Properties;

/**
 * Base Object from which all Configuration objects inherit. Provides a properties object
 * to hold attribute keys and values, and setAttribute/getAttribute methods.  
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public abstract class BaseConfigurationObject {
	//Properties Object holds values from XML attributes keyed to AttributeTitle returned by getXMLAttributeTitles.
	protected Properties attributes = new Properties();
	
	/**
	 * Copy constructor for all Configuration objects. Propogates all keys from the objects
	 * attributes properties to the new Object.
	 * @param bo
	 */
	public BaseConfigurationObject(BaseConfigurationObject bo) {
		for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			setAttribute(new String(key), new String(bo.getAttribute(key)));
		}
	}
	
	/**
	 * Initializes the attributes properties.
	 */
	public BaseConfigurationObject() {
		//doesnt do anything, except return an empty object
	}
	
	/**
	 * Set the XML Attribute for a particular key. This method is primarily for DatasetViewXMLUtils and DatasetViewEditor.  Client code should
	 * use the setXXX methods. Note, keys with null values are not added to the object. 
	 * @param key - String key for this attribute
	 * @param value - String value for this attribute
	 */
	public void setAttribute(String key, String value) {
		if (value != null)
		  attributes.setProperty(key, value);
	}
  
	/**
	 * Get the value of an attribute for a given key. This method is primarily for DatasetViewEditor.  Client code should
	 * use the getXXX methods.
	 * @param key- 
	 * @return
	 */
	public String getAttribute(String key) {
		return attributes.getProperty(key);
	}
	
	/**
	 * Get the XML Attribute Titles for this object. This is meant for use
	 * by DatasetViewEditor.
	 * @return String[] List of XMLAttribute Titles.
	 */
	public String[] getXmlAttributeTitles() {
		String[] titles = new String[attributes.size()];
		attributes.keySet().toArray(titles);
		return titles;
	}

  /**
   * All Configuration Objects must impliment a flag to determine
   * if a validated version of an object (one that has been returned
   * by the DatasetView.validate() method) is broken in some way.
   * @return boolean, true if the Object contains broken members, false otherwise
   */
  public abstract boolean isBroken();
  
  public String toString() {
		StringBuffer buf = new StringBuffer();

    int i = 0;
		for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
			if (i > 0)
			  buf.append(",");
			  
			String key = (String) iter.next();
			buf.append(" ").append(key).append("=").append( attributes.getProperty(key) );
		}

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of BaseConfigurationObject objects
	 */
	public boolean equals(Object o) {
		return o instanceof BaseConfigurationObject && hashCode() == o.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
	    int tmp = 17;
	    
      for (Iterator iter = attributes.values().iterator(); iter.hasNext();) {
				String value = (String) iter.next();
				
				tmp += (value != null) ? value.hashCode() : 0;
			}
	    
	    return tmp;
	}

}
