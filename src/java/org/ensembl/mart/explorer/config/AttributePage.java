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
 * Container for a set of Mart AttributeCollections
 *   
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributePage {

/**
 * Constructor for a nameless, descriptionless AttributePage.
 */
public AttributePage() {
	this("", "");
}

/**
 * Constructor for an AttributePage named by displayName.
 * 
 * @param displayName String name to represent the AttributePage
 */
public AttributePage(String displayName) {
	this(displayName, "");
}

/**
 * Constructor for an AttributePage named by displayName and 
 * described by description.
 * 
 * @param displayName String name to represent the AttributePage
 * @param description String description of the AttributePage
 */
public AttributePage(String displayName, String description) {
	this.displayName = displayName;
	this.description = description;
}

/**
 * Returns the displayName of the AttributePage
 * 
 * @return String displayName
 */
public String getDisplayName() {
	return displayName;
}

/**
 * Returns the description of the AttributePage
 * 
 * @return String description
 */
public String getDescription() {
	return description;
}

/**
 * Add a single attributeGroup to the AttributePage.
 * 
 * @param a An AttributeGroup object
 */
public void addAttributeGroup(AttributeGroup a) {
	Integer rankInt = new Integer(agroupRank);
	attributeGroups.put(rankInt, a);
	attGroupNameMap.put(a.getDisplayName(), rankInt);
	agroupRank++;
}

/**
 * Add a group of AttributeGroup objects at once.  Note, subsequent calls
 * to addAttributeGroup or setAttributeGroup will add to what has already been added.
 * 
 * @param a an array of AttributeGroup objects
 */
public void setAttributeGroups(AttributeGroup[] a) {
	for (int i = 0, n=a.length; i < n; i++) {
		Integer rankInt = new Integer(agroupRank);
		attributeGroups.put(rankInt, a[i]);
		attGroupNameMap.put(a[i].getDisplayName(), rankInt);
		agroupRank++;		
	}
}

/**
 * Returns a list of AttributeGroups contained in the AttributePage, in the order they were added.
 * 
 * @return An array of AttributeGroup objects
 */
public AttributeGroup[] getAttributeGroups() {
	AttributeGroup[] a = new AttributeGroup[ attributeGroups.size() ];
	attributeGroups.values().toArray(a);
	return a;
}

/**
 * Returns a specific AttributeGroup named by displayName.
 * 
 * @param displayName String name of the requested AttributeGroup
 * @return an AttributeGroup object
 */
public AttributeGroup getAttributeGroupByName(String displayName) {
	  return (AttributeGroup) attributeGroups.get( (Integer) attGroupNameMap.get(displayName) );
}

/**
 * Check whether the AttributePage contains a particular AttributeGroup named by displayName.
 * 
 * @param displayName String name of the AttributeGroup
 * @return boolean, true if AttributePage contains AttributeGroup, false if not
 */
public boolean containsAttributeGroup(String displayName) {
	return attGroupNameMap.containsKey(displayName);
}

/**
	* Convenience method for non graphical UI.  Allows a call against the AttributePage for a particular UIAttributeDescription.
	* 
	* @param displayName name of the requested UIAttributeDescription
	* @return UIAttributeDescription object
	* @throws ConfigurationException when the UIAttributeDescription is not found.  Note, it is best to first call containsUIAttributeDescription,
	*                   as there is a caching system to cache a UIAttributeDescription during a call to containsUIAttributeDescription.
	*/
	 public UIAttributeDescription getUIAttributeDescriptionByName(String displayName) throws ConfigurationException {
			boolean found = false;
		  
			if (lastAtt != null && lastAtt.getDisplayName().equals(displayName)) {
				found = true;
			}
			else {
				for (Iterator iter = (Iterator) attributeGroups.keySet().iterator(); iter.hasNext();) {
					AttributeGroup group = (AttributeGroup) attributeGroups.get( (Integer) iter.next() );
					if (group.containsUIAttributeDescription(displayName)) {
						lastAtt = group.getUIAttributeDescriptionByName(displayName);
						found = true;
						break;
					}
				}
			}
			if (found)
				 return lastAtt;
			else
				 throw new ConfigurationException("Could not find UIAttributeDescription "+displayName+" in this AttributePage");
	 }
   
	 /**
		* Convenience method for non graphical UI.  Can determine if the AttributePage contains a specific UIAttributeDescription.
		*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
		*  found, this method caches the UIAttributeDescription it has found.
		* 
		* @param displayName name of the requested UIAttributeDescription
		* @return boolean, true if found, false if not.
		*/
	 public boolean containsUIAttributeDescription(String displayName) throws ConfigurationException {
		boolean found = false;
		
		if (lastAtt != null && lastAtt.getDisplayName().equals(displayName)) {
			found = true;
		}
		else {   	  
			for (Iterator iter = (Iterator) attributeGroups.keySet().iterator(); iter.hasNext();) {
				AttributeGroup group = (AttributeGroup) attributeGroups.get( (Integer) iter.next() );
				if (group.containsUIAttributeDescription(displayName)) {
					lastAtt = group.getUIAttributeDescriptionByName(displayName);
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
	buf.append(", AttributeGroups=").append(attributeGroups);
	buf.append("]");
	
	return buf.toString();
}

private final String displayName, description;
private int agroupRank = 0;
private TreeMap attributeGroups = new TreeMap();
private Hashtable attGroupNameMap = new Hashtable();

//cache one UIAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
private UIAttributeDescription lastAtt = null;
}
