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
import java.util.TreeMap;

/**
 * Container for a set of Mart FilterCollections.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterPage {

/**
 * Constructor for a nameless, descriptionless FilterPage.
 */
public FilterPage() {
	this("", "");
}

/**
 * Constructs a FilterPage object named by displayName.
 * 
 * @param displayName String name to represent the FilterPage
 */
public FilterPage(String displayName) {
	this(displayName, "");
}

/**
 * Constructs a FilterPage object named by displayName, with a description.
 * Note, FilterPages can have null displayNames.
 * 
 * @param displayName String name to represent the FilterPage
 * @param description String description of the FilterPage
 */
public FilterPage(String displayName, String description) {
	this.displayName = displayName;
	this.description = description;
}

/**
 * Returns the displayName of the FilterPage
 * Note, may be null.
 * 
 * @return String displayName
 */
public String getDisplayName() {
	return displayName;
}

/**
 * Returns the description of the FilterPage.
 * 
 * @return String description
 */
public String getDescription() {
	return description;
}

/**
 * Adds a FilterGroup to the FilterPage.
 * 
 * @param fg a FilterGroup object.
 */
public void addFilterGroup(FilterGroup fg) {
	Integer fgRankInt = new Integer(fcRank);
	filterGroups.put(fgRankInt, fg);
	filterGroupNameMap.put(fg.getDisplayName(), fgRankInt);
}

/**
 * Sets a group of FilterGroup objects in one call.
 * Note, subsequent calls to addFilterGroup and setFilterGroups will add to what
 * has already been aded.
 * 
 * @param fg An Array of FilterGroup objects.
 */
public void setFilterGroups(FilterGroup[] fg) {
	for (int i = 0, n=fg.length; i < n; i++) {
		Integer fgRankInt = new Integer(fcRank);
		filterGroups.put(fgRankInt, fg[i]);
		filterGroupNameMap.put(fg[i].getDisplayName(), fgRankInt);		
	}
}

/**
 * Returns an Array of FilterGroup objects, in the order they were added.
 * 
 * @return Array of FilterGroup objects.
 */
public FilterGroup[] getFilterGroups() {
	 FilterGroup[] fg = new FilterGroup[filterGroups.size()];
	 filterGroups.values().toArray(fg);
	 return fg;
}

/**
 * Returns a particular FilterGroup object, named by the given displayName.
 * 
 * @param displayName String name of the FilterGroup
 * @return FilterGroup object
 */
public FilterGroup getFilterGroupByName(String displayName) {
	 return (FilterGroup) filterGroups.get( (Integer) filterGroupNameMap.get(displayName) );
}

/**
 * Check whether the FilterPage contains a given FilterGroup.
 * 
 * @param displayName String name of the given FilterGroup
 * @return boolean, true if FilterPage contains the FilterGroup, false if not
 */
public boolean containsFilterGroup(String displayName) {
   return filterGroupNameMap.containsKey(displayName);
}

/**
	* Convenience method for non graphical UI.  Allows a call against the FilterPage for a particular UIFilterDescription.
	* 
	* @param displayName name of the requested UIFilterDescription
	* @return UIFilterDescription object
	* @throws ConfigurationException when the UIFilterDescription is not found.  Note, it is best to first call containsUIFilterDescription,
	*                   as there is a caching system to cache a UIFilterDescription during a call to containsUIFilterDescription.
	*/
	 public UIFilterDescription getUIFilterDescriptionByName(String displayName) throws ConfigurationException {
			boolean found = false;
		  
			if (lastFilt != null && lastFilt.getDisplayName().equals(displayName)) {
				found = true;
			}
			else {
				for (Iterator iter = (Iterator) filterGroups.keySet().iterator(); iter.hasNext();) {
					FilterGroup group = (FilterGroup) filterGroups.get( (Integer) iter.next() );
					if (group.containsUIFilterDescription(displayName)) {
						lastFilt = group.getUIFilterDescriptionByName(displayName);
						found = true;
						break;
					}
				}
			}
			if (found)
				 return lastFilt;
			else
				 throw new ConfigurationException("Could not find UIFilterDescription "+displayName+" in this FilterPage");
	 }
   
	 /**
		* Convenience method for non graphical UI.  Can determine if the FilterPage contains a specific UIFilterDescription.
		*  As an optimization for initial calls to containsUIFilterDescription with an immediate call to getUIFilterDescriptionByName if
		*  found, this method caches the UIFilterDescription it has found.
		* 
		* @param displayName name of the requested UIFilterDescription object
		* @return boolean, true if found, false if not.
		*/
	 public boolean containsUIFilterDescription(String displayName) throws ConfigurationException {
		boolean found = false;
		
		if (lastFilt != null && lastFilt.getDisplayName().equals(displayName)) {
			found = true;
		}
		else {   	  
			for (Iterator iter = (Iterator) filterGroups.keySet().iterator(); iter.hasNext();) {
				FilterGroup group = (FilterGroup) filterGroups.get( (Integer) iter.next() );
				if (group.containsUIFilterDescription(displayName)) {
					lastFilt = group.getUIFilterDescriptionByName(displayName);
					found = true;
					break;
				}
			}
		}
		return found;  
	 }
	 
public String toString() {
	StringBuffer buf = new StringBuffer();
	
	buf.append("[");
	buf.append(" displayName=").append(displayName);
	buf.append(", description=").append(description);
	buf.append(", FilterCollections=").append(filterGroups);
	buf.append("]");
	return buf.toString();
}

private final String displayName, description;

private int fcRank = 0;
private TreeMap filterGroups = new TreeMap();
private Hashtable filterGroupNameMap = new Hashtable();

//cache one UIFilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
private UIFilterDescription lastFilt = null;
}
