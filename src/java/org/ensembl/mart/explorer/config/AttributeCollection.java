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
		buf.append(", maxSelect=").append(maxSelect);
		buf.append(", UIAttributeDescriptions=").append(uiAttributes);
		buf.append("]");

		return buf.toString();
	}

  public boolean equals(Object o) {
		if (!(o instanceof AttributeCollection))
			return false;

		AttributeCollection otype = (AttributeCollection) o;

		if (! (internalName.equals(otype.getInternalName()) ) )
			return false;
	  
		if (! (displayName.equals(otype.getDisplayName()) ) )
			return false;
	  
		if (! (description.equals(otype.getDescription()) ) )
			return false;				

    if (! ( maxSelect == otype.getMaxSelect() ) )
      return false;

    //other AttributeCollection must contain all UIAttributeDescriptions that this AttributeCollection contains
    for (Iterator iter = uiAttributes.values().iterator(); iter.hasNext();) {
			UIAttributeDescription element = (UIAttributeDescription) iter.next();
			
			if (! ( otype.containsUIAttributeDescription( element.getInternalName() ) ) )
			  return false;
			if (! ( element.equals( otype.getUIAttributeDescriptionByName( element.getInternalName() ) ) ) )
			  return false;
		}  
		
		return true;
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
