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
	 * AttributeCollections must have a internalName, type, and maxSelect
	 * so disable parameterless constructor
	 */
	private AttributeCollection() throws ConfigurationException {
		this("", "", 0, "", ""); // will never happen
	}

	/**
	 * Constructor for an AttributeCollection named by internalName, with a type and maxSelect value.
	 * 
	* @param internalName String name to internally represent the AttributeCollection.  Must not be null
	 * @param type String type of the AttributeCollection.  Must not be null
	 * @param maxSelect int maximum allowable combined attribute selections.  Must not be less than 1
	 * @throws ConfigurationException when the required values are null or empty.
	 */
	public AttributeCollection(String internalName, String type, int maxSelect) throws ConfigurationException {
		this(internalName, type, maxSelect, "", "");
	}

	/**
	 * Constructor for a AttributeCollection named by internalName, with a displayName, type and maxSelect value.
	 * May have description description.
	 * 
	 * @param internalName String name to internally represent the AttributeCollection.  Must not be null
	 * @param type String type of the AttributeCollection.  Must not be null
	 * @param maxSelect int maximum allowable combined attribute selections.  Must not be less than 1
	 * @param displayName String name to represent the AttributeCollection.
	 * @param description String description of the AttributeCollection
	 * @throws ConfigurationException if required parameters are null or empty.
	 */
	public AttributeCollection(String internalName, String type, int maxSelect, String displayName, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals("") || type == null || type.equals("") || maxSelect < 1)
			throw new ConfigurationException("AttributeCollections must contain an internalName, type, and maxSelect value");

		this.internalName = internalName;
		this.displayName = displayName;
		this.description = description;
		this.type = type;
		this.maxSelect = maxSelect;
	}

	/**
	 * Returns the internalName of the AttributeCollection.
	 * 
	 * @return String internalName of the AttributeCollection
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the displayName of the AttributeCollection.
	 * 
	 * @return String displayName.
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
		uiAttributeNameMap.put(a.getInternalName(), aRankInt);
		aRank++;
	}

	/**
	 * Set a group of UIAttributeDescription objects in one call.  Note, subsequent calls to addUIAttribute or setUIAttribute
	 * will add to what was added before.
	 * 
	 * @param a an Array of UIAttributeDescription objects.
	 */
	public void setUIAttributes(UIAttributeDescription[] a) {
		for (int i = 0, n = a.length; i < n; i++) {
			Integer aRankInt = new Integer(aRank);
			uiAttributes.put(aRankInt, a[i]);
			uiAttributeNameMap.put(a[i].getInternalName(), aRankInt);
			aRank++;
		}
	}

	/**
	 * Returns an array of UIAttributeDescription objects, in the order they were added.
	 * 
	 * @return array of UIAttributeDescription objects.
	 */
	public UIAttributeDescription[] getUIAttributeDescriptions() {
		UIAttributeDescription[] a = new UIAttributeDescription[uiAttributes.size()];
		uiAttributes.values().toArray(a);
		return a;
	}


	/**
		* Convenience method for non graphical UI.  Allows a call against the AttributeCollection for a particular UIAttributeDescription.
		* Note, it is best to first call containsUIAttributeDescription,
		*  as there is a caching system to cache a UIAttributeDescription during a call to containsUIAttributeDescription.
		* 
		* @param internalName name of the requested UIAttributeDescription
		* @return UIAttributeDescription object
		*/
	public UIAttributeDescription getUIAttributeDescriptionByName(String internalName) {
		boolean found = false;

		if (lastAtt != null && lastAtt.getInternalName().equals(internalName)) {
			found = true;
		} else {
			for (Iterator iter = (Iterator) uiAttributes.keySet().iterator(); iter.hasNext();) {
				UIAttributeDescription attribute = (UIAttributeDescription) uiAttributes.get((Integer) iter.next());
				if (attribute.getInternalName().equals(internalName)) {
					lastAtt = attribute;
					found = true;
					break;
				}
			}
		}
		if (found)
			return lastAtt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the AttributeCollection contains a specific UIAttributeDescription.
		*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
		*  found, this method caches the UIAttributeDescription it has found.
		* 
		* @param internalName name of the requested UIAttributeDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIAttributeDescription(String internalName) {
		boolean found = false;

		if (lastAtt != null && lastAtt.getInternalName().equals(internalName)) {
			found = true;
		} else {
			for (Iterator iter = (Iterator) uiAttributes.keySet().iterator(); iter.hasNext();) {
				UIAttributeDescription attribute = (UIAttributeDescription) uiAttributes.get((Integer) iter.next());
				if (attribute.getInternalName().equals(internalName)) {
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
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", type=").append(type);
		buf.append(", maxSelect=").append(maxSelect);
		buf.append(", UIAttributes=").append(uiAttributes);
		buf.append("]");

		return buf.toString();
	}

	private final String internalName, displayName, description, type;
	private final int maxSelect;
	private int aRank = 0;
	private TreeMap uiAttributes = new TreeMap();
	private Hashtable uiAttributeNameMap = new Hashtable();

	//cache one UIAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
	private UIAttributeDescription lastAtt = null;
}
