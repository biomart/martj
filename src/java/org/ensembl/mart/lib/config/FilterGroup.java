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
public class FilterGroup extends BaseConfigurationObject {

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
		super( internalName, displayName, description );
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
		* Convenience method for non graphical UI.  Allows a call against the FilterGroup for a particular FilterDescription object.
		* Note, it is best to first call containsFilterDescription, as there is a caching system to cache a FilterDescription during a call 
		* to containsFilterDescription.
		* 
		* @param internalName name of the requested FilterDescription
		* @return requested FilterDescription, or null.
		*/
	public FilterDescription getFilterDescriptionByInternalName(String internalName) {
		if ( containsFilterDescription(internalName) )
			return lastFilt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the FilterGroup contains a specific FilterDescription object.
		*  As an optimization for initial calls to containsFilterDescription with an immediate call to getFilterDescriptionByInternalName if
		*  found, this method caches the FilterDescription it has found.
		* 
		* @param internalName name of the requested FilterDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsFilterDescription(String internalName) {
		boolean found = false;

		if (lastFilt == null) {
			for (Iterator iter = (Iterator) filterCollections.keySet().iterator(); iter.hasNext();) {
				FilterCollection collection = (FilterCollection) filterCollections.get((Integer) iter.next());
				if (collection.containsFilterDescription(internalName)) {
					lastFilt = collection.getFilterDescriptionByInternalName(internalName);
					found = true;
					break;
				}
			}
		}
		else {
			if ( lastFilt.getInternalName().equals(internalName) )
			  found = true;
			else {
				lastFilt = null;
				found = containsFilterDescription(internalName);
			}
		}
		return found;
	}

  /**
   * Convenience method to get all FilterDescription objects 
   * contained in all FilterCollections in this FilterGroup.
   * 
   * @return List of FilterDescription objects.
   */
  public List getAllFilterDescriptions() {
  	List filts = new ArrayList();
  	
		for (Iterator iter = filterCollections.keySet().iterator(); iter.hasNext();) {
			FilterCollection fc = (FilterCollection) filterCollections.get((Integer) iter.next());
  		
			filts.addAll(fc.getFilterDescriptions());
		}
		
		return filts;
  }

	/**
	 * Returns the FilterCollection for a particular FilterDescription
	 * based on its internalName.
	 * 
	 * @param internalName - String internalName of the Filter Description for which the collection is being requested.
	 * @return FilterCollection for the FilterDescription provided, or null
	 */
	public FilterCollection getCollectionForFilter(String internalName) {
		if (! containsFilterDescription(internalName))
			return null;
		else if (lastColl == null) {
			for (Iterator iter = filterCollections.keySet().iterator(); iter.hasNext();) {
				FilterCollection fc = (FilterCollection) filterCollections.get((Integer) iter.next());
				
				if (fc.containsFilterDescription(internalName)) {
					lastColl = fc;
					break;
				}
			}
			return lastColl;
		}
		else {
			if (lastColl.getInternalName().equals(internalName))
				return lastColl;
			else {
				lastColl = null;
				return getCollectionForFilter(internalName);
			}
		}
	}

  /**
   * Get a FilterDescription object that supports a given field and tableConstraint.  Useful for mapping from a Filter object
   * added to a Query back to its FilterDescription.
   * @param field -- String field of a mart database table
   * @param tableConstraint -- String tableConstraint of a mart database
   * @return FilterDescription object supporting the given field and tableConstraint, or null.
   */  
  public FilterDescription getFilterDescriptionByFieldNameTableConstraint(String field, String tableConstraint) {
  	if (supports(field, tableConstraint))
  	  return lastSupportingFilter;
  	else
  	  return null;
  }
  
  /**
   * Determine if this FilterGroup contains a FilterDescription that supports a given field and tableConstraint.
   * Calling this method will cache any FilterDescription that supports the field and tableConstraint, and this will
   * be returned by a getFilterDescriptionByFieldNameTableConstraint call.
   * @param field -- String field of a mart database table
   * @param tableConstraint -- String tableConstraint of a mart database
   * @return boolean, true if the FilterGroup contains a FilterDescription supporting a given field, tableConstraint, false otherwise.
   */
  public boolean supports(String field, String tableConstraint) {
  	boolean supports = false;
  	
  	if (lastSupportingFilter == null) {
  		for (Iterator iter = filterCollections.values().iterator(); iter.hasNext();) {
				 FilterCollection element = (FilterCollection) iter.next();
				 if (element.supports(field, tableConstraint)) {
				 	lastSupportingFilter = element.getFilterDescriptionByFieldNameTableConstraint(field, tableConstraint);
				 	supports = true;
				 	break;
				 }
			}
  	} else {
  		if (lastSupportingFilter.supports(field, tableConstraint))
  		  supports = true;
  		else {
  			lastSupportingFilter = null;
  			supports = supports(field, tableConstraint);
  		}
  	}
  	return supports;
  }
  
  /**
   * debug output
   */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append( super.toString() );
		buf.append(", filterCollections=").append(filterCollections);
		buf.append("]");

		return buf.toString();
	}
		
  /**
	 * Allows Equality Comparisons manipulation of FilterGroup objects
	 */
	public boolean equals(Object o) {
		return o instanceof FilterGroup && hashCode() == o.hashCode();
	}

  public int hashCode() {
		int tmp = super.hashCode();
		
		for (Iterator iter = filterCollections.values().iterator(); iter.hasNext();) {
			FilterCollection element = (FilterCollection) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}
		
		return tmp;
  }
  
  private int cRank = 0; //keep track of collection order
		
	private TreeMap filterCollections = new TreeMap();
	private Hashtable filterCollectionNameMap = new Hashtable();
    
	//cache one FilterDescription for call to containsFilterDescription or getUIFiterDescriptionByName
	private FilterDescription lastFilt = null;
	
	//cache one FilterCollection for call to getCollectionForFilter
	private FilterCollection lastColl = null;
	
	//cache one FilterDescription for call to supports
	private FilterDescription lastSupportingFilter = null; 
}
