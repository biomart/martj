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
 * Default Filter for a DatasetView.  Holds a FilterDescription, and
 * the value to apply for that filter.  This allows the DatasetView to
 * provide the UI with any default filters that need to be added to a 
 * query when it is selected.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DefaultFilter {
  private String value;
  private FilterDescription fdesc;
	private List xmlAttributeTitles = new ArrayList();
  
  /**
   * Copy constructor. Creates an exact copy of an existing DefaultFilter.
   * @param df DefaultFilter to copy.
   */
  public DefaultFilter(DefaultFilter df) {
  	value = df.getValue();
  	
  	setFilterDescription(new FilterDescription( df.getFilterDescription() ) );
  }
  
   /**
    * Empty Constructor.  Should really only be used by the DatasetViewEditor
    */
   public DefaultFilter() {  
   }
   
   /**
    * Constructor for a DefaultFilter with a FilterDescription, and a value to apply
    * to that Description.
    * 
    * @param fdesc - FilterDescription to apply as default
    * @param value - value to apply for the FilterDescription
    * @throws ConfigurationException when value is null or empty, or FilterDescription is null
    */
   public DefaultFilter(FilterDescription fdesc, String value) throws ConfigurationException {
     if (value == null || value.equals("")
         || fdesc == null)
       throw new ConfigurationException("DefaultFilter Objects must be instantiated with a FilterDescription and a value for that filter\n");
     
     this.fdesc = fdesc;
     this.value = value;
   }

  /**
   * Sets the FilterDescription for this DefaultFilter
   * @param description -- FilterDescription
   */
  public void setFilterDescription(FilterDescription description) {
    fdesc = description;
  }

  /**
   * Sets the value for this DefaultFilter
   * @param string
   */
  public void setValue(String string) {
    value = string;
  }
  
  /**
   * Returns the FilterDescription
   * @return FilterDescription object
   */
  public FilterDescription getFilterDescription() {
    return fdesc;
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
	
  /**
   * Returns the value for the filter
   * @return String value
   */
  public String getValue() {
    return value;
  }
   
   public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" FilterDescription=").append(fdesc);
    buf.append(", value=").append(value);
		buf.append("]");

		return buf.toString();
	}
 
  /**
	 * Allows Equality Comparisons manipulation of DefaultFilter objects
	 */
	public boolean equals(Object o) {
		return o instanceof DefaultFilter && hashCode() == ((DefaultFilter) o).hashCode();
	}
  
   /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
    int tmp = fdesc.hashCode();
    tmp = (31 * tmp) + value.hashCode();
     
    return tmp;
	}
}
