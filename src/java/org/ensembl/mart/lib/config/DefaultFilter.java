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
 * Default Filter for a DatasetView.  Holds a FilterDescription, and
 * the value to apply for that filter.  This allows the DatasetView to
 * provide the UI with any default filters that need to be added to a 
 * query when it is selected.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DefaultFilter extends BaseConfigurationObject {
  private boolean hasBrokenFilter;
  private final String valueKey = "value";
  private FilterDescription fdesc;
  
  /**
   * Copy constructor. Creates an exact copy of an existing DefaultFilter.
   * @param df DefaultFilter to copy.
   */
  public DefaultFilter(DefaultFilter df) {
  	super(df);
  	
  	setFilterDescription(new FilterDescription( df.getFilterDescription() ) );
  }
  
   /**
    * Empty Constructor.  Should really only be used by the DatasetViewEditor
    */
   public DefaultFilter() {
   	super();
    
    setAttribute(valueKey, null);  
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
   	super();
   	
     if (value == null || value.equals("")
         || fdesc == null)
       throw new ConfigurationException("DefaultFilter Objects must be instantiated with a FilterDescription and a value for that filter\n");
     
     this.fdesc = fdesc;
     setAttribute(valueKey, value);
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
  public void setValue(String value) {
    setAttribute(valueKey, value);
  }
  
  /**
   * Returns the FilterDescription
   * @return FilterDescription object
   */
  public FilterDescription getFilterDescription() {
    return fdesc;
  }
	
  /**
   * Returns the value for the filter
   * @return String value
   */
  public String getValue() {
    return getAttribute(valueKey);
  }
   
   public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		super.toString();
		buf.append(" FilterDescription=").append(fdesc);
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
		int tmp = super.hashCode();
    tmp = (fdesc != null) ? (31 * tmp) + fdesc.hashCode() : tmp;
     
    return tmp;
	}

	/**
	 * Set the hasBrokenFilter flag to true, meaning its FilterDescription is broken.
	 */
	public void setFilterBroken() {
    hasBrokenFilter = true;
	}
	
	/**
	 * Determine if this DefaultFilter 's FilterDescription Object is broken.
	 * @return boolean
	 */
	public boolean hasBrokenFilter() {
		return hasBrokenFilter;
	}
	
	/**
	 * True if hasBrokenFilter is true
	 * @return boolean
	 */
	public boolean isBroken() {
		return hasBrokenFilter;
	}
 }
