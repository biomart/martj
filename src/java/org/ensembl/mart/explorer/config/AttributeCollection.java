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
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributeCollection {

/*
 * AttributeCollections must have a displayName, type, and maxSelect
 * so disable parameterless constructor
 */
private AttributeCollection() {
	this("", "", "", 0); // will never happen
}

/**
 * Constructor for a AttributeCollection named by displayName, of type type, with maxSelect value maxSelect.
 * May have description description.
 * 
 * @param displayName String name to represent the AttributeCollection.  Must not be null
 * @param description String description of the AttributeCollection
 * @param type String type of the AttributeCollection.  Must not be null
 * @param maxSelect int maximum allowable combined attribute selections.  Must not be less than 1
 * @throws RuntimeException if required parameters are not defined.
 */
public AttributeCollection(String displayName, String description, String type, int maxSelect) {
	if (displayName == null || type == null || maxSelect < 1)
	throw new RuntimeException("AttributeCollections must contain a displayName, type, and maxSelect value");
	
	this.displayName = displayName;
	this.description = description;
	this.type = type;
	this.maxSelect = maxSelect;
}

/**
 * Returns the displayName of the AttributeCollection.
 * 
 * @return String name of the AttributeCollection
 */
public String getDisplayName() {
	return displayName;
}

/**
 * Returns the description of the AttributeCollection
 * 
 * @return String description of the AttributeCollection
 */
public String getDescription() {
	return description;
}

/**
 * Returns the type of the AttributeCollection.
 * 
 * @return String type of the AttributeCollection
 */
public String getType() {
	return type;
}

/**
 * Returns the maxSelect value for attributes in this AttributeCollection.
 * 
 * @return int maxSelect value
 */
public int getMaxSelect() {
	return maxSelect;
}

/**
 * Add a UIAttributeDescription to the AtttributeCollection.
 * 
 * @param a a UIAttributeDescription object.
 */
public void addUIAttribute(UIAttributeDescription a) {
	Integer aRankInt = new Integer(aRank);
	uiAttributes.put(aRankInt, a);
	uiAttributeNameMap.put(a.getDisplayName(), aRankInt);
	aRank++;
}

/**
 * Set a group of UIAttributeDescription objects in one call.  Note, subsequent calls to addUIAttribute or setUIAttribute
 * will add to what was added before.
 * 
 * @param a an Array of UIAttributeDescription objects.
 */
public void setUIAttributes(UIAttributeDescription[] a) {
	for (int i = 0, n=a.length; i < n; i++) {
		Integer aRankInt = new Integer(aRank);
		uiAttributes.put(aRankInt, a[i]);
		uiAttributeNameMap.put(a[i].getDisplayName(), aRankInt);
		aRank++;		
	}
}

/**
 * Returns an array of UIAttributeDescription objects, in the order they were added.
 * 
 * @return array of UIAttributeDescription objects.
 */
public UIAttributeDescription[] getUIAttributes() {
	UIAttributeDescription[] a = new UIAttributeDescription[uiAttributes.size()];
	uiAttributes.values().toArray(a);
	return a;
}

/**
 * Returns a particular UIAttributeDescription named by displayName.
 * 
 * @param displayName String name of the requested UIAttributeDescription
 * @return UIAttributeDescription object
 */
public UIAttributeDescription getUIAttributeByName(String displayName) {
	return (UIAttributeDescription) uiAttributes.get((Integer) uiAttributeNameMap.get(displayName) );
}

/**
 * Check if a particular UIAttributeDescription object is contained within the AttributeCollection.
 * 
 * @param displayName String name of the requested UIAttributeDescription
 * @return boolean, true if UIAttributeDescription is contained within the AttributeCollection, false if not
 */
public boolean containsUIAttribute(String displayName) {
	return uiAttributeNameMap.containsKey(displayName);
}

/**
	* Convenience method for non graphical UI.  Allows a call against the AttributeCollection for a particular UIAttributeDescription.
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
				for (Iterator iter = (Iterator) uiAttributes.keySet().iterator(); iter.hasNext();) {
					UIAttributeDescription attribute = (UIAttributeDescription) uiAttributes.get( (Integer) iter.next() );
					if (attribute.getDisplayName().equals(displayName)) {
						lastAtt = attribute;
						found = true;
						break;
					}
				}
			}
			if (found)
				 return lastAtt;
			else
				 throw new ConfigurationException("Could not find UIAttributeDescription "+displayName+" in this AttributeCollection");
	 }
   
	 /**
		* Convenience method for non graphical UI.  Can determine if the AttributeCollection contains a specific UIAttributeDescription.
		*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
		*  found, this method caches the UIAttributeDescription it has found.
		* 
		* @param displayName name of the requested UIAttributeDescription object
		* @return boolean, true if found, false if not.
		*/
	 public boolean containsUIAttributeDescription(String displayName) {
		boolean found = false;
		
		if (lastAtt != null && lastAtt.getDisplayName().equals(displayName)) {
			found = true;
		}
		else {   	  
			for (Iterator iter = (Iterator) uiAttributes.keySet().iterator(); iter.hasNext();) {
				UIAttributeDescription attribute = (UIAttributeDescription) uiAttributes.get( (Integer) iter.next() );
				if (attribute.getDisplayName().equals(displayName)) {
					lastAtt = attribute;
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
  buf.append(", maxSelect=").append(maxSelect);
  buf.append(", UIAttributes=").append(uiAttributes);
  buf.append("]");
  
  return buf.toString();	
}

private final String displayName, description, type;
private final int maxSelect;
private int aRank = 0;
private TreeMap uiAttributes = new TreeMap();
private Hashtable uiAttributeNameMap = new Hashtable();

//cache one UIAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
private UIAttributeDescription lastAtt = null;
}
