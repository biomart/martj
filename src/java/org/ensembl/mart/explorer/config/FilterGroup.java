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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Container for a group of Mart FilterCollections.  Allows categorical grouping of collections
 * of filters.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterGroup {

	/*
	 * FilterGroups must have an internalName, so disable parameterless construction.
	 */
	public FilterGroup() throws ConfigurationException {
		this("", "", ""); // will never happen
	}

	/**
	 * Constructor for a FilterGroup represented internally by internalName.
	 * 
	 * @param internalName name to internally represent the FilterGroup
	 * @throws ConfigurationException when internalName is null or empty
	 */
	public FilterGroup(String internalName) throws ConfigurationException {
		this(internalName, "", "");
	}

	/**
	 * Constructor for a FilterGroup named internally by internalName, with a displayName, and a description.
	 * 
	 * @param internalName String name to internally represent the filterGroup. Must not be null.
	 * @param displayName
	 * @param description
	 * @throws ConfigurationException when internalName is null or empty.
	 */
	public FilterGroup(String internalName, String displayName, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals(""))
			throw new ConfigurationException("FilterGroup must contain an internalName");

		this.internalName = internalName;
		this.displayName = displayName;
		this.description = description;
	}

	/**
	 * Returns the internalName of the FilterGroup
	 * 
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the displayName of the FilterGroup.
	 * 
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the description of the FilterGroup
	 * 
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Add a FilterCollection to the FilterGroup
	 * 
	 * @param f a FilterCollection object
	 */
	public void addFilterCollection(FilterCollection f) {
		Integer cRankInt = new Integer(cRank);
		filterCollections.put(cRankInt, f);
		filterCollectionNameMap.put(f.getInternalName(), cRankInt);
		cRank++;
	}

	/**
	 * Set a group of FilterCollection objects in one call.  Note, subsequent calls
	 * to addFilterCollection or setFilterCollections will add to what was previously added.
	 * 
	 * @param f an Array of FilterCollection objects
	 */
	public void setFilterCollections(FilterCollection[] f) {
		for (int i = 0, n = f.length; i < n; i++) {
			Integer cRankInt = new Integer(cRank);
			filterCollections.put(cRankInt, f[i]);
			filterCollectionNameMap.put(f[i].getInternalName(), cRankInt);
			cRank++;
		}
	}

  /**
   * Add a FilterSet to the FilterGroup.
   * 
   * @param f a FilterSet object
   */
  public void addFilterSet(FilterSet f) {
  	if (! hasFilterSets )
  	  hasFilterSets = true;
  	  
  	filterSets.put(f.getInternalName(), f);
  }
  
  /**
   * Set a group of FilterSet objects in one call. Note, subsequent calls to addFilterSet, setFilterSets
   * will add to what was added before.
   * 
   * @param f an array of FilterSet objects
   */
  public void setFilterSets(FilterSet[] f) {
		if (! hasFilterSets )
			hasFilterSets = true;
			  	
  	for (int i = 0, n = f.length; i < n; i++) {
			FilterSet set = f[i];
			filterSets.put(set.getInternalName(), set);
		}
  }
  
	/**
	 * Returns an array of FilterCollection objects, in the order they were added
	 * 
	 * @return Array of FilterCollection objects
	 */
	public FilterCollection[] getFilterCollections() {
		FilterCollection[] fc = new FilterCollection[filterCollections.size()];
		filterCollections.values().toArray(fc);
		return fc;
	}

	/**
	 * Returns a particular FilterCollection named by internalName
	 * 
	 * @param internalName String name of the requested FilterCollection
	 * 
	 * @return a FilterCollection object, or null
	 */
	public FilterCollection getFilterCollectionByName(String internalName) {
		if (filterCollectionNameMap.containsKey(internalName))
			return (FilterCollection) filterCollections.get((Integer) filterCollectionNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check if a FilterGroup contains a given FilterCollection, of name internalName
	 * 
	 * @param internalName String name of the requested FilterCollection
	 * @return boolean true if FilterGroup contains the FilterCollection, false if not
	 */
	public boolean containsFilterCollection(String internalName) {
		return filterCollectionNameMap.containsKey(internalName);
	}

  /**
   * Returns all FilterSets contained by this FilterGroup.
   * 
   * @return an array of FilterSet objects.
   */
  public FilterSet[] getFilterSets() {
  	FilterSet[] f = new FilterSet[ filterSets.size() ];
  	filterSets.values().toArray(f);
  	return f;
  }
  
  /**
   * Returns a specific FilterSet, named by internalName.
   * 
   * @param internalName - String internal name of the requested FilterSet.
   * @return FilterSet named by internalName, or null.
   */
  public FilterSet getFilterSetByName(String internalName) {
  	if (filterSets.containsKey(internalName))
  	  return (FilterSet) filterSets.get(internalName);
  	else
  	  return null;
  }
  
  /**
   * Check if a FilterGroup contains a specific FilterSet, named by internalName.
   * 
   * @param internalName - String internal name of the FilterSet.
   * @return boolean, true if found, false if not.
   */
  public boolean containsFilterSet(String internalName) {
  	return filterSets.containsKey(internalName);
  }
  
  /**
   * Method for UI to determine if a FilterGroup has FilterSets to render.
   *  
   * @return boolean, true if FilterSets have been added to this FilterGroup, false if not.
   */
  public boolean hasFilterSets() {
  	return hasFilterSets;
  }
  
	/**
		* Convenience method for non graphical UI.  Allows a call against the FilterGroup for a particular UIFilterDescription/UIDSFilterDescription object.
		* Note, it is best to first call containsUIFilterDescription, as there is a caching system to cache a UIFilterDescription during a call 
		* to containsUIFilterDescription.
		* 
		* @param internalName name of the requested UIFilterDescription
		* @return requested Object (either instanceof UIFilterDescription or UIDSFilterDescription), or null.
		*/
	public Object getUIFilterDescriptionByName(String internalName) {
		if ( containsUIFilterDescription(internalName) )
			return lastFilt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the FilterGroup contains a specific UIFilterDescription/UIDSFilterDescription object.
		*  As an optimization for initial calls to containsUIFilterDescription with an immediate call to getUIFilterDescriptionByName if
		*  found, this method caches the UIFilterDescription it has found.
		* 
		* @param internalName name of the requested UIFilterDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIFilterDescription(String internalName) {
		boolean found = false;

		if (lastFilt == null) {
			for (Iterator iter = (Iterator) filterCollections.keySet().iterator(); iter.hasNext();) {
				FilterCollection collection = (FilterCollection) filterCollections.get((Integer) iter.next());
				if (collection.containsUIFilterDescription(internalName)) {
					lastFilt = collection.getUIFilterDescriptionByName(internalName);
					found = true;
					break;
				}
			}
		}
		else {
			String lastIntName;
			if (lastFilt instanceof UIFilterDescription)
			  lastIntName = ( (UIFilterDescription) lastFilt).getInternalName();
			else if (lastFilt instanceof UIDSFilterDescription)
			  lastIntName = ( (UIDSFilterDescription) lastFilt).getInternalName();
			else
			  lastIntName = ""; // should not get here
			  
			if ( lastIntName.equals(internalName) )
			  found = true;
			else {
				lastFilt = null;
				found = containsUIFilterDescription(internalName);
			}
		}
		return found;
	}

  /**
   * Convenience method to get all UIFilterDescription/UIDSFilterDescription objects 
   * contained in all FilterCollections in this FilterGroup.
   * 
   * @return List of FilterDescription objects.
   */
  public List getAllUIFilterDescriptions() {
  	List filts = new ArrayList();
  	
		for (Iterator iter = filterCollections.keySet().iterator(); iter.hasNext();) {
			FilterCollection fc = (FilterCollection) filterCollections.get((Integer) iter.next());
  		
			filts.addAll(fc.getUIFilterDescriptions());
		}
		
		return filts;
  }
  
  /**
   * Convenience method for non graphical UI to check if a FilterGroup contains a specific
   * FilterSetDescription.
   * 
   * @param internalName - String name that internally represents the requested FilterSetDescription
   * @return boolean, true if found within one of the filterSet objects contained in the filterGroup, false if not found
   */
  public boolean containsFilterSetDescription(String internalName) {
  	if (! hasFilterSets)
  	  return false;
  	  
  	boolean found = false;
  	
  	if (lastFSetDescription == null) {
  		for (Iterator iter = filterSets.values().iterator(); iter.hasNext();) {
				FilterSet fset = (FilterSet) iter.next();
				if (fset.containsFilterSetDescription(internalName)) {
					lastFSetDescription = fset.getFilterSetDescriptionByName(internalName);
					found = true;
					break;
				}
			}
  	}
  	else {
  		if (lastFSetDescription.getInternalName().equals(internalName))
  		  found = true;
  		else {
  			lastFSetDescription = null;
  			found = containsFilterSetDescription(internalName);
  		}
  	}
  	return found;
  }
  
  /**
   * Convenience method for non graphical UI to get a specific FilterSetDescription by name.
   * 
   * @param internalName - String name that internally represents the requested FilterSetDescription
   * @return FilterSetDescription object requested, or null if not contained within this FilterGroup
   */
  public FilterSetDescription getFilterSetDescriptionByName(String internalName) {
  	if (! hasFilterSets)
  	  return null;
  	else if (containsFilterSetDescription(internalName))
  	  return lastFSetDescription;
  	else
  	  return null;
  }
  
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", filterCollections=").append(filterCollections);
		if (hasFilterSets)
		  buf.append(", filterSets=").append(filterSets);
		buf.append("]");

		return buf.toString();
	}

  /**
	 * Allows Equality Comparisons manipulation of FilterGroup objects
	 */
	public boolean equals(Object o) {
		return o instanceof FilterGroup && hashCode() == ((FilterGroup) o).hashCode();
	}

  public int hashCode() {
		int tmp = internalName.hashCode();
		tmp = (31 * tmp) + displayName.hashCode();
		tmp = (31 * tmp) + description.hashCode();
		
		for (Iterator iter = filterCollections.values().iterator(); iter.hasNext();) {
			FilterCollection element = (FilterCollection) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}
		
		if (hasFilterSets) {
			for (Iterator iter = filterSets.values().iterator(); iter.hasNext();) {
				FilterSet element = (FilterSet) iter.next();
				tmp = (31 * tmp) + element.hashCode();
			}
		}
		
		return tmp;
  }
  
	private final String internalName, displayName, description;
	private int cRank = 0; //keep track of collection order
	private boolean hasFilterSets = false;
	
	private TreeMap filterCollections = new TreeMap();
	private Hashtable filterCollectionNameMap = new Hashtable();
  private Hashtable filterSets = new Hashtable(); // do not need to presever order of filterSets
  
	//cache one FilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
	private Object lastFilt = null;
	
	//cache one FilterSetDescription for a call to containsFilterSetDescription or getFilterSetDescriptionByName
	private FilterSetDescription lastFSetDescription = null;
}
