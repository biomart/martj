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
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributeCollection {

	/*
	 * AttributeCollections must have a internalName, type, and maxSelect
	 * so disable parameterless constructor
	 */
	private AttributeCollection() throws ConfigurationException {
		this("", 0, "", ""); // will never happen
	}

	/**
	 * Constructor for an AttributeCollection named by internalName, with a type.
	 * 
	*  @param internalName String name to internally represent the AttributeCollection.  Must not be null
	 * @param maxSelect int maximum allowable combined attribute selections.  Must not be less than 1
	 * @throws ConfigurationException when the required values are null or empty.
	 */
	public AttributeCollection(String internalName) throws ConfigurationException {
		this(internalName, 0, "", "");
	}

	/**
	 * Constructor for a AttributeCollection named by internalName, with a displayName, type and maxSelect value.
	 * May have description description.
	 * 
	 * @param internalName String name to internally represent the AttributeCollection.  Must not be null
	 * @param maxSelect int maximum allowable combined attribute selections.
	 * @param displayName String name to represent the AttributeCollection.
	 * @param description String description of the AttributeCollection
	 * @throws ConfigurationException if required parameters are null or empty.
	 */
	public AttributeCollection(String internalName, int maxSelect, String displayName, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals(""))
			throw new ConfigurationException("AttributeCollections must contain an internalName and maxSelect value");

		this.internalName = internalName;
		this.displayName = displayName;
		this.description = description;
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
	 * Set a group of UIAttributeDescription objects in one call.  Note, subsequent calls to addUIAttribute, setUIAttribute,
	 * addUIDSAttribute or setUIDSAttributes will add to what was added before.
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
	 * Returns a List of UIAttributeDescription objects, in the order they were added.
	 * 
	 * @return List of UIAttributeDescription objects.
	 */
	public List getUIAttributeDescriptions() {
		return new ArrayList(uiAttributes.values());
	}


	/**
		* Get a specific UIAttributeDescription, named by internalName.
		*  
		* @param internalName name of the requested UIAttributeDescription
		* @return UIAttributeDescription requested, or null
		*/
	public UIAttributeDescription getUIAttributeDescriptionByName(String internalName) {
		if ( containsUIAttributeDescription(internalName) )
			return (UIAttributeDescription) uiAttributes.get( (Integer) uiAttributeNameMap.get(internalName));
		else
			return null;
	}

	/**
		* Check if this AttributeCollection contains a specific UIAttributeDescription named
		* by internalName.
		*  
		* @param internalName name of the requested UIAttributeDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIAttributeDescription(String internalName) {
  	return uiAttributeNameMap.containsKey(internalName);
	}
  
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", maxSelect=").append(maxSelect);
		buf.append(", AttributeDescriptions=").append(uiAttributes);
		buf.append("]");

		return buf.toString();
	}

  /**
	 * Allows Equality Comparisons manipulation of AttributeCollection objects
	 */
	public boolean equals(Object o) {
		return o instanceof AttributeCollection && hashCode() == ((AttributeCollection) o).hashCode();
	}

	public int hashCode() {
		int tmp = internalName.hashCode();
		tmp = (31 * tmp) + displayName.hashCode();
		tmp = (31 * tmp) + description.hashCode();
		
		for (Iterator iter = uiAttributes.values().iterator(); iter.hasNext();) {
			UIAttributeDescription element = (UIAttributeDescription) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}
		return tmp;
	}
  
	private final String internalName, displayName, description;
	private final int maxSelect;
	private int aRank = 0;
	private TreeMap uiAttributes = new TreeMap();
	private Hashtable uiAttributeNameMap = new Hashtable();

	//cache one UIAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
	private UIAttributeDescription lastAtt = null;
}
