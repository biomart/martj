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
 * Container for a set of Mart AttributeCollections.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public final class AttributeGroup {

public AttributeGroup() {
	this("","");
}

public AttributeGroup(String displayName) {
	this(displayName, "");
}

public AttributeGroup(String displayName, String description) {
	this.displayName = displayName;
	this.description = description;
}

public String getDisplayName() {
	return displayName;
}

public String getDescription() {
	return description;
}

public void addAttributeCollection(AttributeCollection c) {
	Integer cRankInt = new Integer(cRank);
	attributeCollections.put(cRankInt, c);
	attributeCollectionNameMap.put(c.getDisplayName(), cRankInt);
	cRank++;
}

public void setAttributeCollections(AttributeCollection[] c) {
	for (int i = 0; i < c.length; i++) {
		Integer cRankInt = new Integer(cRank);
		attributeCollections.put(cRankInt, c[i]);
		attributeCollectionNameMap.put(c[i].getDisplayName(), cRankInt);
		cRank++;		
	}
}

public AttributeCollection[] getAttributeCollections() {
	AttributeCollection[] a = new AttributeCollection[attributeCollections.size()];
	attributeCollections.values().toArray(a);
	return a;
}

public AttributeCollection getAttributeCollectionByName(String displayName) {
	return (AttributeCollection) attributeCollections.get((Integer) attributeCollectionNameMap.get(displayName));
}

public boolean containsAttributeCollection(String displayName) {
	return attributeCollectionNameMap.containsKey(displayName);
}

/**
	* Convenience method for non graphical UI.  Allows a call against the AttributeGroup for a particular UIAttributeDescription.
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
				for (Iterator iter = (Iterator) attributeCollections.keySet().iterator(); iter.hasNext();) {
					AttributeCollection collection = (AttributeCollection) attributeCollections.get( (Integer) iter.next() );
					if (collection.containsUIAttributeDescription(displayName)) {
						lastAtt = collection.getUIAttributeDescriptionByName(displayName);
						found = true;
						break;
					}
				}
			}
			if (found)
				 return lastAtt;
			else
				 throw new ConfigurationException("Could not find UIAttributeDescription "+displayName+" in this AttributeGroup");
	 }
   
	 /**
		* Convenience method for non graphical UI.  Can determine if the AttributeGroup contains a specific UIAttributeDescription.
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
			for (Iterator iter = (Iterator) attributeCollections.keySet().iterator(); iter.hasNext();) {
				AttributeCollection collection = (AttributeCollection) attributeCollections.get( (Integer) iter.next() );
				if (collection.containsUIAttributeDescription(displayName)) {
					lastAtt = collection.getUIAttributeDescriptionByName(displayName);
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
	buf.append(", attributeCollections=").append(attributeCollections);
	buf.append("]");
	
	return buf.toString();
}
private final String displayName, description;
private int cRank = 0;
private TreeMap attributeCollections = new TreeMap();
private Hashtable attributeCollectionNameMap = new Hashtable();

//cache one UIAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
private UIAttributeDescription lastAtt = null;
}
