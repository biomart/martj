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
	if (internalName == null || internalName.equals(""))
	  throw new ConfigurationException("FilterPage must have an internalName");
	  
	this.internalName = internalName;  
	this.displayName = displayName;
	this.description = description;
}

/**
 * Returns the internalName to internally represent the FilterPage.
 * 
 * @return String internalName
 */
public String getInternalName() {
	return internalName;
}

/**
 * Returns the displayName of the FilterPage
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
	filterGroupNameMap.put(fg.getInternalName(), fgRankInt);
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
		filterGroupNameMap.put(fg[i].getInternalName(), fgRankInt);		
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
 * Returns a particular FilterGroup object, named by the given internalName.
 * 
 * @param internalName String name of the FilterGroup
 * @return FilterGroup object
 */
public FilterGroup getFilterGroupByName(String internalName) {
	 return (FilterGroup) filterGroups.get( (Integer) filterGroupNameMap.get(internalName) );
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
	* Convenience method for non graphical UI.  Allows a call against the FilterPage for a particular UIFilterDescription.
	* 
	* @param internalName name of the requested UIFilterDescription
	* @return UIFilterDescription object
	* @throws ConfigurationException when the UIFilterDescription is not found.  Note, it is best to first call containsUIFilterDescription,
	*                   as there is a caching system to cache a UIFilterDescription during a call to containsUIFilterDescription.
	*/
	 public UIFilterDescription getUIFilterDescriptionByName(String internalName) throws ConfigurationException {
			boolean found = false;
		  
			if (lastFilt != null && lastFilt.getInternalName().equals(internalName)) {
				found = true;
			}
			else {
				for (Iterator iter = (Iterator) filterGroups.keySet().iterator(); iter.hasNext();) {
					FilterGroup group = (FilterGroup) filterGroups.get( (Integer) iter.next() );
					if (group.containsUIFilterDescription(internalName)) {
						lastFilt = group.getUIFilterDescriptionByName(internalName);
						found = true;
						break;
					}
				}
			}
			if (found)
				 return lastFilt;
			else
				 throw new ConfigurationException("Could not find UIFilterDescription "+internalName+" in this FilterPage");
	 }
   
	 /**
		* Convenience method for non graphical UI.  Can determine if the FilterPage contains a specific UIFilterDescription.
		*  As an optimization for initial calls to containsUIFilterDescription with an immediate call to getUIFilterDescriptionByName if
		*  found, this method caches the UIFilterDescription it has found.
		* 
		* @param internalName name of the requested UIFilterDescription object
		* @return boolean, true if found, false if not.
		*/
	 public boolean containsUIFilterDescription(String internalName) throws ConfigurationException {
		boolean found = false;
		
		if (lastFilt != null && lastFilt.getInternalName().equals(internalName)) {
			found = true;
		}
		else {   	  
			for (Iterator iter = (Iterator) filterGroups.keySet().iterator(); iter.hasNext();) {
				FilterGroup group = (FilterGroup) filterGroups.get( (Integer) iter.next() );
				if (group.containsUIFilterDescription(internalName)) {
					lastFilt = group.getUIFilterDescriptionByName(internalName);
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
	buf.append(" internalName=").append(internalName);
	buf.append(", displayName=").append(displayName);	buf.append(" displayName=").append(displayName);
	buf.append(", description=").append(description);
	buf.append(", FilterCollections=").append(filterGroups);
	buf.append("]");
	return buf.toString();
}

private final String displayName, description, internalName;

private int fcRank = 0;
private TreeMap filterGroups = new TreeMap();
private Hashtable filterGroupNameMap = new Hashtable();

//cache one UIFilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
private UIFilterDescription lastFilt = null;
}
