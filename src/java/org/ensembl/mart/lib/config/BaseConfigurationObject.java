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
 * Basic Object from which all Configuration Objects inherit.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class BaseConfigurationObject {

  protected String internalName;
  protected String displayName;
  protected String description;
  protected List xmlAttributeTitles = new ArrayList();
  
	/**
	 * Determines if string is an invalid attribute value.
	 * @param s
	 * @return true if string is null or the empty string.
	 */
	public static final boolean isInvalid(String s) {
		return s == null && "".equals(s);
	}


  /**
   * Determines if string is a validate attribute value.
   * @param s
   * @return true if string is not null and not empty
   */
  public static final boolean valid(String s) {
    return s!=null && !"".equals(s);
  }

  /**
   * Copy constructor.  Creates an exact copy of an existing object.
   * @param bo - BaseConfigurationObject to copy.
   */
  public BaseConfigurationObject(BaseConfigurationObject bo) {
  	internalName = bo.getInternalName(); 
  	displayName = bo.getDisplayName();
  	description = bo.getDescription();
  	
  	setXmlAttributeTitles(bo.getXmlAttributeTitles());
  }
  
  public BaseConfigurationObject() {
    //doesnt do anything, but returns an empty object
  }
  
	public BaseConfigurationObject(
		String internalName,
		String displayName,
		String description)
		throws ConfigurationException {
		if (internalName == null || internalName.equals(""))
			throw new ConfigurationException("Configuration Object must contain an internalName\n");

		this.internalName = internalName;
		this.displayName = displayName;
		this.description = description;
	}

	/**
	 * Returns the Description
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
   * Sets the description for this object
   * @param string
   */
  public void setDescription(String string) {
    description = string;
  }

  /**
   * Sets the displayName for this object
   * @param string
   */
  public void setDisplayName(String string) {
    displayName = string;
  }

  /**
   * Sets the internalName for this object
   * @param string
   */
  public void setInternalName(String string) {
    internalName = string;
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
	
	public int hashCode() {
    int tmp = 17;
    tmp = tmp * 37 + ((internalName != null) ? internalName.hashCode() : 0);
    tmp = tmp * 37 + ((displayName != null) ? displayName.hashCode() : 0);
    tmp = tmp * 37 + ((description != null) ? description.hashCode() : 0);
    return tmp;
	}

	public boolean equals(Object o) {
		return o instanceof BaseConfigurationObject && o.hashCode() == hashCode();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append("]");

		return buf.toString();
	}
}
