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
 
package org.ensembl.mart.explorer.config;

import java.util.Hashtable;
import java.util.Iterator;

/**
 * FilterSets allow generic groupings of UIFilterDescriptions within a FilterGroup 
 * into functional units, allowing them to contain generic fieldName or tableConstraint 
 * values that can be further qualified via reference to a specific FilterSetDescription.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterSet {

  /**
   * This will throw a ConfigurationException.
   * 
   * @throws ConfigurationException
   */
  public FilterSet() throws ConfigurationException {
  	this("","","","");
  }
  
  /**
   * Constructor for a minimally defined FilterSet, with only internalName and type.
   * 
   * @param internalName - String name to internally represent the FilterSet.
   * @param type - String type of set, for the UI to know how to display the options.
   * 
   * @throws ConfigurationException when internalName or type are null or empty.
   */
  public FilterSet(String internalName, String type) throws ConfigurationException {
  	this(internalName, type, "", "");
  }
  
  /**
   * 
   * @param internalName - String name to internally represent the FilterSet.
   * @param type - String type of set, for the UI to know how to display the options.
   * @param displayName - String used to represent the FilterSet in a UI.
   * @param description - Description of the FilterSet.
   * @throws ConfigurationException when internalName or type are null or empty.
   */
  public FilterSet(String internalName, String type, String displayName, String description) throws ConfigurationException {
    if ( internalName == null || internalName.equals("")
          || type == null || type.equals("") )
       throw new ConfigurationException("FilterSets must be instantiated with an internalName and type");
     
    this.internalName = internalName;
    this.type = type;
    this.displayName = displayName;
    this.description = description;  	
  }
  
  /**
   * adds a FilterSetDescription to the FilterSet.
   *  
   * @param f - FilterSetDescription to be added
   */
  public void addFilterSetDescription(FilterSetDescription f) {
  	filterSetDescriptions.put(f.getInternalName(), f);
  }
  
  /**
   * Set a group of FilterSetDescription objects in one call.  Note, subsequenct calls
   * to addFilterSetDescription or setFilterSetDescriptions will add to what was added
   * previously.
   * 
   * @param f - an array of FilterSetDescription objects
   */
  public void setFilterSetDescriptions(FilterSetDescription[] f) {
  	for (int i = 0, n = f.length; i < n; i++) {
			filterSetDescriptions.put(f[i].getInternalName(), f[i]);			
		}
  }
  
	/**
	 * Returns the description.
	 * 
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the displayName.
	 * 
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the internalName.
	 * 
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the type.
	 * 
	 * @return String type.
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Returns all FilterSetDescription objects in the order they were added.
	 * 
	 * @return array of FilterSetDescription objects.
	 */
	public FilterSetDescription[] getFilterSetDescriptions() {
		FilterSetDescription[] f = new FilterSetDescription[filterSetDescriptions.size()];
		filterSetDescriptions.values().toArray(f);
		return f;
	}
   
  /**
   * Check if a FilterSet contains a specific FilterSetDescription, named by internalName.
   * 
   * @param internalName - String internal name of the requested FilterSetDescription.
   * 
   * @return boolean, true if found, false if not
   */
  public boolean containsFilterSetDescription(String internalName) {
    return filterSetDescriptions.containsKey(internalName);	
  }
  
  /**
   * Returns a specific FilterSetDescription, named by internalName, or null.
   * 
   * @param internalName - String internal name of the requested FilterSetDescription.
   * @return FilterSetDescription requested, or null.
   */
  public FilterSetDescription getFilterSetDescriptionByName(String internalName) {
  	if (filterSetDescriptions.containsKey(internalName))
  	  return (FilterSetDescription) filterSetDescriptions.get( internalName );
  	else
  	  return null;
  }
  
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", FilterSetDescriptions=").append(filterSetDescriptions);
		buf.append("]");

		return buf.toString();
	}
	
  /**
	 * Allows Equality Comparisons manipulation of FilterSet objects
	 */
	public boolean equals(Object o) {
		return o instanceof FilterSet && hashCode() == ((FilterSet) o).hashCode();
	}
	
  public int hashCode() {
		int tmp = internalName.hashCode();
		tmp = (31 * tmp) + type.hashCode();
		tmp = (31 * tmp) + displayName.hashCode();
		tmp = (31 * tmp) + description.hashCode();
		
		for (Iterator iter = filterSetDescriptions.values().iterator(); iter.hasNext();) {
			FilterSetDescription element = (FilterSetDescription) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}
		return tmp;  	
  }
  
  private final String internalName, displayName, description, type;
  
  private Hashtable filterSetDescriptions = new Hashtable();
}
