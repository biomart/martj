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
 * Default Filter for a Dataset.  Holds a FilterDescription, and
 * the value to apply for that filter.  This allows the Dataset to
 * provide the UI with any default filters that need to be added to a 
 * query when it is selected.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DefaultFilter {
   
   /**
    * This will throw an exception
    * @throws ConfigurationException
    */
   public DefaultFilter() throws ConfigurationException {
     this(null, "");  
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
     int tmp = fdesc.hashCode();
     
     this.value = value;
     tmp = (31 * tmp) + value.hashCode();
     
     hashcode = tmp;
   }

  /**
   * Returns the FilterDescription
   * @return FilterDescription object
   */
  public FilterDescription getUIFilterDescription() {
    return fdesc;
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
		return hashcode;
	}

   private final String value;
   private final FilterDescription fdesc;
   private final int hashcode;
}
