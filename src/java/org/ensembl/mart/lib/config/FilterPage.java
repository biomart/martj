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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Container for a set of Mart FilterCollections.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterPage extends BaseConfigurationObject {

	/*
	* FilterPages must have an internalName, so disable parameterless construction
	*/
	private FilterPage() throws ConfigurationException {
		this("", "", "");
	}

	/**
	 * Constructs a FilterPage object named by internalName.
	 * 
	 * @param internalName String name to internally represent the FilterPage
	 */
	public FilterPage(String internalName) throws ConfigurationException {
		this(internalName, "", "");
	}

	/**
	 * Constructs a FilterPage object named by internalName, with a displayName, and a description.
	 * 
	 * @param internalName String name to internally represent the FilterPage. Must not be null.
	 * @param displayName String name to represent the FilterPage
	 * @param description String description of the FilterPage
	 * @throws ConfigurationException when the internalName is null or empty
	 */
	public FilterPage(String internalName, String displayName, String description) throws ConfigurationException {
    super( internalName, displayName, description);
    
	}

	/**
	 * Adds a FilterGroup to the FilterPage.
	 * 
	 * @param fg a FilterGroup object.
	 */
	public void addFilterGroup(FilterGroup fg) {
		Integer fgRankInt = new Integer(fcRank);
		filterGroups.put(fgRankInt, fg);
		filterGroupNameMap.put(fg.getInternalName(), fgRankInt);
		fcRank++;
	}

	/**
	 * Sets a group of FilterGroup objects in one call.
	 * Note, subsequent calls to addFilterGroup and setFilterGroups will add to what
	 * has already been aded.
	 * 
	 * @param fg An Array of FilterGroup objects.
	 */
	public void setFilterGroups(FilterGroup[] fg) {
		for (int i = 0, n = fg.length; i < n; i++) {
			Integer fgRankInt = new Integer(fcRank);
			filterGroups.put(fgRankInt, fg[i]);
			filterGroupNameMap.put(fg[i].getInternalName(), fgRankInt);
			fcRank++;
		}
	}

	/**
	 * Adds a DSFilterGroup to the FilterPage.
	 * 
	 * @param fg a DSFilterGroup object.
	 */
	public void addDSFilterGroup(DSFilterGroup fg) {
		Integer fgRankInt = new Integer(fcRank);
		filterGroups.put(fgRankInt, fg);
		filterGroupNameMap.put(fg.getInternalName(), fgRankInt);
		fcRank++;
	}

	/**
	 * Sets a group of DSFilterGroup objects in one call.
	 * Note, subsequent calls to addFilterGroup/addDSFilterGroup and setFilterGroups/setDSFilterGroups will add to what
	 * has already been aded.
	 * 
	 * @param fg An Array of DSFilterGroup objects.
	 */
	public void setDSFilterGroups(DSFilterGroup[] fg) {
		for (int i = 0, n = fg.length; i < n; i++) {
			Integer fgRankInt = new Integer(fcRank);
			filterGroups.put(fgRankInt, fg[i]);
			filterGroupNameMap.put(fg[i].getInternalName(), fgRankInt);
			fcRank++;
		}
	}

	/**
	 * Returns a List of FilterGroup/DSFilterGroup objects, in the order they were added.
	 * 
	 * @return List of FilterGroup/DSFilterGroup objects.
	 */
	public List getFilterGroups() {
		return new ArrayList(filterGroups.values());
	}

	/**
	 * Returns a particular FilterGroup object, named by the given internalName.
	 * 
	 * @param internalName String name of the FilterGroup
	 * @return Object (either FilterGroup or DSFilterGroup), or null.
	 */
	public Object getFilterGroupByName(String internalName) {
		if (filterGroupNameMap.containsKey(internalName))
			return filterGroups.get((Integer) filterGroupNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check whether the FilterPage contains a given FilterGroup.
	 * 
	 * @param internalName String name of the given FilterGroup
	 * @return boolean, true if FilterPage contains the FilterGroup, false if not
	 */
	public boolean containsFilterGroup(String internalName) {
		return filterGroupNameMap.containsKey(internalName);
	}

	/**
		* Convenience method for non graphical UI.  Allows a call against the FilterPage for a particular FilterDescription.
		* Note, it is best to first call containsFilterDescription,
		* as there is a caching system to cache a FilterDescription during a call to containsFilterDescription.
		* 
		* @param internalName name of the requested FilterDescription
		* @return FilterDescription object, or null.
		*/
	public Object getFilterDescriptionByInternalName(String internalName) {
		if (containsFilterDescription(internalName))
			return lastFilt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the FilterPage contains a specific FilterDescription.
		*  As an optimization for initial calls to containsFilterDescription with an immediate call to getFilterDescriptionByInternalName,
		*  this method caches the first FilterDescription it has found matching internalName.
		* 
		* @param internalName name of the requested FilterDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsFilterDescription(String internalName) {
		boolean found = false;

		if (lastFilt == null) {
			for (Iterator iter = (Iterator) filterGroups.keySet().iterator(); iter.hasNext();) {
				Object group = filterGroups.get((Integer) iter.next());

				if (group instanceof FilterGroup && ((FilterGroup) group).containsFilterDescription(internalName)) {
					lastFilt = ((FilterGroup) group).getFilterDescriptionByInternalName(internalName);
					found = true;
					break;
				}
			}
		} else {
			String lastIntName;
			if (lastFilt instanceof FilterDescription)
				lastIntName = ((FilterDescription) lastFilt).getInternalName();
			else if (lastFilt instanceof MapFilterDescription)
				lastIntName = ((MapFilterDescription) lastFilt).getInternalName();
			else
				lastIntName = ""; // should not get here

			if (lastIntName.equals(internalName))
				found = true;
			else {
				lastFilt = null;
				found = containsFilterDescription(internalName);
			}
		}
		return found;
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
	 * Determine if this FilterPage contains a FilterDescription that supports a given field and tableConstraint.
	 * Calling this method will cache any FilterDescription that supports the field and tableConstraint, and this will
	 * be returned by a getFilterDescriptionByFieldNameTableConstraint call.
	 * @param field -- String field of a mart database table
	 * @param tableConstraint -- String tableConstraint of a mart database
	 * @return boolean, true if the FilterPage contains a FilterDescription supporting a given field, tableConstraint, false otherwise.
	 */
	public boolean supports(String field, String tableConstraint) {
		boolean supports = false;
  	
		if (lastSupportingFilter == null) {
			for (Iterator iter = filterGroups.values().iterator(); iter.hasNext();) {
				 FilterGroup element = (FilterGroup) iter.next();
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
	 * Convenience Method to get all FilterDescription/MapFilterDescription objects 
	 * in all Groups/Collections within a FilterPage.
	 * 
	 * @return List of FilterDescription/MapFilterDescription objects
	 */
	public List getAllFilterDescriptions() {
		List filts = new ArrayList();

		for (Iterator iter = filterGroups.keySet().iterator(); iter.hasNext();) {
			Object fg = (FilterGroup) filterGroups.get((Integer) iter.next());
			if (fg instanceof FilterGroup)
				filts.addAll(((FilterGroup) fg).getAllFilterDescriptions());
		}

		return filts;
	}

	/**
	 * Convenience method for non graphical UI to check if a FilterPage contains a specific
	 * FilterSetDescription.
	 * 
	 * @param internalName - String name that internally represents the requested FilterSetDescription
	 * @return boolean, true if found within one of the filterSet objects contained in one of the filterGroups, false if not found
	 */
	public boolean containsFilterSetDescription(String internalName) {
		boolean found = false;

		if (lastFSetDescription == null) {
			for (Iterator iter = (Iterator) filterGroups.keySet().iterator(); iter.hasNext();) {
				Object group = filterGroups.get((Integer) iter.next());
				if (group instanceof FilterGroup) {
					if (((FilterGroup) group).containsFilterSetDescription(internalName)) {
						lastFSetDescription = ((FilterGroup) group).getFilterSetDescriptionByName(internalName);
						found = true;
						break;
					}
				}
			}
		} else {
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
	 * @return FilterSetDescription object requested, or null if not contained within this FilterPage
	 */
	public FilterSetDescription getFilterSetDescriptionByName(String internalName) {
		if (containsFilterSetDescription(internalName))
			return lastFSetDescription;
		else
			return null;
	}

	public FilterSetDescription[] getAllFilterSetDescriptions() {
		List fsds = new ArrayList();

		for (Iterator iter = (Iterator) filterGroups.values().iterator(); iter.hasNext();) {
			Object group = iter.next();
			if (group instanceof FilterGroup) {
				fsds.addAll(Arrays.asList(((FilterGroup) group).getAllFilterSetDescriptions()));
			}
		}
		FilterSetDescription[] f = new FilterSetDescription[fsds.size()];
		fsds.toArray(f);
		return f;
	}

	/**
	 * Returns a FilterGroup for a particular Filter Description (FilterDescription or MapFilterDescription)
	 * based on its internalName.
	 * 
	 * @param internalName - String internalname for which a group is requested
	 * @return FilterGroup object containing Filter Description with given internalName, or null.
	 */
	public FilterGroup getGroupForFilter(String internalName) {
		if (!containsFilterDescription(internalName))
			return null;
		else if (lastGroup == null) {
			for (Iterator iter = filterGroups.values().iterator(); iter.hasNext();) {
				Object groupo = iter.next();

				if (groupo instanceof FilterGroup) {
					FilterGroup group = (FilterGroup) groupo;
					if (group.containsFilterDescription(internalName)) {
						lastGroup = group;
						break;
					}
				}
			}
			return lastGroup;
		} else {
			if (lastGroup.getInternalName().equals(internalName))
				return lastGroup;
			else {
				lastGroup = null;
				return getGroupForFilter(internalName);
			}
		}
	}

	/**
	 * Returns a FilterCollection for a particular Filter Description (FilterDescription or MapFilterDescription)
	 * based on its internalName.
	 * 
	 * @param internalName - String internalname for which a collection is requested
	 * @return FilterCollection object containing Filter Description with given internalName, or null.
	 */
	public FilterCollection getCollectionForFilter(String internalName) {
		if (!containsFilterDescription(internalName))
			return null;
		else if (lastColl == null) {
			lastColl = getGroupForFilter(internalName).getCollectionForFilter(internalName);
			return lastColl;
		} else {
			if (lastColl.getInternalName().equals(internalName))
				return lastColl;
			else {
				lastColl = null;
				return getCollectionForFilter(internalName);
			}
		}
	}

	/**
	 * debug output
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append( super.toString() );
		buf.append(", FilterGroups=").append(filterGroups);
		buf.append("]");
		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of FilterPage objects
	 */
	public boolean equals(Object o) {
		return o instanceof FilterPage && hashCode() == ((FilterPage) o).hashCode();
	}

	public int hashCode() {
		int tmp = super.hashCode();

		for (Iterator iter = filterGroups.values().iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof FilterGroup)
				tmp = (31 * tmp) + ((FilterGroup) element).hashCode();
			else
				tmp = (31 * tmp) + ((DSFilterGroup) element).hashCode();
		}

		return tmp;
	}

 

	private int fcRank = 0;
	private TreeMap filterGroups = new TreeMap();
	private Hashtable filterGroupNameMap = new Hashtable();

	//cache one FilterDescription Object for call to containsUIFilterDescription or getUIFiterDescriptionByName
	private Object lastFilt = null;

	//cache one FilterSetDescription for call to containsFilterSetDescription or getFilterSetDescrioptionByNae
	private FilterSetDescription lastFSetDescription = null;

	//cache one FilterGroup for call to getGroupForFilter
	private FilterGroup lastGroup = null;

	//cache one FilterCollection for call to getCollectionForFilter
	private FilterCollection lastColl = null;
	
	//cache one FilterDescription for call to supports/getFilterDescriptionByFieldNameTableConstraint
	private FilterDescription lastSupportingFilter = null;
}
