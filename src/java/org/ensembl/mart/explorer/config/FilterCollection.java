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
 * Container for a group of Mart FilterCollections.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterCollection {

/*
 * FilterCollection must have a displayName, and type.  So disable parameterless constructor
 */
private FilterCollection() {
	this("", "", "");
}

/**
 * Constructor for a FilterCollection named by displayName, of type type, with an optional description.
 * 
 * @param displayName String name to represent the FilterCollection.  Must not be null.
 * @param description String description of the FilterCollection.
 * @param type String type of the FilterCollection. Must not be null.
 * @throws RuntimeException when paremeter requirements are not met
 */
public FilterCollection(String displayName, String description, String type) {
	if (displayName == null || type == null)
	  throw new RuntimeException("FilterCollections must have a displayName and type");
	  
	  this.displayName = displayName;
	  this.description = description;
	  this.type = type;
}

/**
 * Returns the displayName of the FilterCollection
 * 
 * @return String displayName
 */
public String getDisplayName() {
	return displayName;
}

/**
 * Returns the description of the FilterCollection
 * 
 * @return String description
 */
public String getDescription() {
	return description;
}

/**
 * Returns the type of the FilterCollection.
 * 
 * @return String type
 */
public String getType() {
	return type;
}

/**
 * Add a UIFilterDescription object to this FilterCollection.
 * 
 * @param f a UIFilterDescription object
 */
public void addUIFilter(UIFilterDescription f) {
	Integer fRankInt = new Integer(fRank);
	uiFilters.put(fRankInt, f);
	uiFilterNameMap.put(f.getDisplayName(), fRankInt);
	fRank++;
}

/**
 * Set a group of UIFilterDescription objects in one call.
 * Note, subsequent calls to addUIFilter and setUIFilter will add to what has
 * been added previously.
 * 
 * @param f an array of UIFilterDescription objects.
 */
public void setUIFilters(UIFilterDescription[] f) {
	for (int i = 0, n=f.length; i < n; i++) {
		Integer fRankInt = new Integer(fRank);
		uiFilters.put(fRankInt, f[i]);
		uiFilterNameMap.put(f[i].getDisplayName(), fRankInt);
		fRank++;		
	}
}

/**
 * Returns a array of UIFilterDescription objects, in the order they were added.
 * 
 * @return array of UIFilterDescription objects
 */
public UIFilterDescription[] getUIFilters() {
	UIFilterDescription[] uf = new UIFilterDescription[uiFilters.size()];
	uiFilters.values().toArray(uf);
	return uf;
}

/**
 * Returns a specific UIFilterDescription, named by displayName.
 * 
 * @param displayName String name of the requested UIFilterDescription
 * @return UIFilterDescription object
 */
public UIFilterDescription getUIFilterbyName(String displayName) {
	return (UIFilterDescription) uiFilters.get( (Integer) uiFilterNameMap.get(displayName));
}

/**
 * Check if this FilterCollection contains a specific UIFilterDescription.
 * 
 * @param displayName String name of the requested UIFilterDescription
 * @return boolean, true if FilterCollection contains the UIFilterDescription, false if not.
 */
public boolean containsUIFilter(String displayName) {
	return uiFilterNameMap.containsKey(displayName);
}

/**
	* Convenience method for non graphical UI.  Allows a call against the FilterCollection for a particular UIFilterDescription.
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
				for (Iterator iter = (Iterator) uiFilters.keySet().iterator(); iter.hasNext();) {
					UIFilterDescription filter = (UIFilterDescription) uiFilters.get( (Integer) iter.next() );
					if (filter.getDisplayName().equals(displayName)) {
						lastFilt = filter;
						found = true;
						break;
					}
				}
			}
			if (found)
				 return lastFilt;
			else
				 throw new ConfigurationException("Could not find UIFilterDescription "+displayName+" in this FilterCollection");
	 }
   
	 /**
		* Convenience method for non graphical UI.  Can determine if the FilterCollection contains a specific UIFilterDescription.
		*  As an optimization for initial calls to containsUIFilterDescription with an immediate call to getUIFilterDescriptionByName if
		*  found, this method caches the UIFilterDescription it has found.
		* 
		* @param displayName name of the requested UIFilterDescription object
		* @return boolean, true if found, false if not.
		*/
	 public boolean containsUIFilterDescription(String displayName) {
		boolean found = false;
		
		if (lastFilt != null && lastFilt.getDisplayName().equals(displayName)) {
			found = true;
		}
		else {   	  
			for (Iterator iter = (Iterator) uiFilters.keySet().iterator(); iter.hasNext();) {
				UIFilterDescription filter = (UIFilterDescription) uiFilters.get( (Integer) iter.next() );
				if (filter.getDisplayName().equals(displayName)) {
					lastFilt = filter;
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
	buf.append(", type=").append(type);
	buf.append(", UIFilters=").append(uiFilters);
	buf.append("]");
		
	return buf.toString();
}

private final String displayName, description, type;
private int fRank = 0;
private TreeMap uiFilters = new TreeMap();
private Hashtable uiFilterNameMap = new Hashtable();

//cache one UIFilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
private UIFilterDescription lastFilt = null;
}
