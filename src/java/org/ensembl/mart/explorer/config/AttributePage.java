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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Container for a set of Mart AttributeCollections
 *   
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributePage {

	/*
	 * AttributePages must have an internalName. So disable paremeterless construction
	 */
	private AttributePage() throws ConfigurationException {
		this("", "", "");
	}

	/**
	 * Constructor for an AttributePage represented by internalName internally.
	 * 
	 * @param internalName String name to internally represent the AttributePage
	 * @throws ConfigurationException when the internalName is null or empty
	 */
	public AttributePage(String internalName) throws ConfigurationException {
		this(internalName, "", "");
	}

	/**
	 * Constructor for an AttributePage named internally by internalName, with a 
	 * displayName and described by description.
	 * 
	 * @param internalName String name to internally represent the AttributePage.  Must not be null.
	 * @param displayName String name to represent the AttributePage
	 * @param description String description of the AttributePage
	 * @throws ConfigurationException when the internalName is null or empty
	 */
	public AttributePage(String internalName, String displayName, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals(""))
			throw new ConfigurationException("AttributePage must have an internalName");

		this.internalName = internalName;
		this.displayName = displayName;
		this.description = description;
	}

	/**
	 * Returns the internalName of the AttributePage
	 * 
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the displayName to display in a UI.
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
		attGroupNameMap.put(a.getInternalName(), rankInt);
		agroupRank++;
	}

	/**
	 * Add a group of AttributeGroup objects at once.  Note, subsequent calls
	 * to addAttributeGroup or setAttributeGroup will add to what has already been added.
	 * 
	 * @param a an array of AttributeGroup objects
	 */
	public void setAttributeGroups(AttributeGroup[] a) {
		for (int i = 0, n = a.length; i < n; i++) {
			Integer rankInt = new Integer(agroupRank);
			attributeGroups.put(rankInt, a[i]);
			attGroupNameMap.put(a[i].getInternalName(), rankInt);
			agroupRank++;
		}
	}

	/**
	 * Returns a list of AttributeGroups contained in the AttributePage, in the order they were added.
	 * 
	 * @return An array of AttributeGroup objects
	 */
	public AttributeGroup[] getAttributeGroups() {
		AttributeGroup[] a = new AttributeGroup[attributeGroups.size()];
		attributeGroups.values().toArray(a);
		return a;
	}

	/**
	 * Returns a specific AttributeGroup named by internalName.
	 * 
	 * @param internalName String name of the requested AttributeGroup
	 * @return an AttributeGroup object
	 */
	public AttributeGroup getAttributeGroupByName(String internalName) {
		if (attGroupNameMap.containsKey(internalName))
			return (AttributeGroup) attributeGroups.get((Integer) attGroupNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check whether the AttributePage contains a particular AttributeGroup named by internalName.
	 * 
	 * @param internalName String name of the AttributeGroup
	 * @return boolean, true if AttributePage contains AttributeGroup, false if not
	 */
	public boolean containsAttributeGroup(String internalName) {
		return attGroupNameMap.containsKey(internalName);
	}

	/**
		* Convenience method for non graphical UI.  Allows a call against the AttributePage for a particular UIAttributeDescription.
	 * Note, it is best to first call containsUIAttributeDescription,  
		*  as there is a caching system to cache a UIAttributeDescription during a call to containsUIAttributeDescription.
		*  
		* @param internalName name of the requested UIAttributeDescription
		* @return UIAttributeDescription object, or null
		*/
	public UIAttributeDescription getUIAttributeDescriptionByName(String internalName) {
		boolean found = false;

		if (lastAtt != null && lastAtt.getInternalName().equals(internalName)) {
			found = true;
		} else {
			for (Iterator iter = (Iterator) attributeGroups.keySet().iterator(); iter.hasNext();) {
				AttributeGroup group = (AttributeGroup) attributeGroups.get((Integer) iter.next());
				if (group.containsUIAttributeDescription(internalName)) {
					lastAtt = group.getUIAttributeDescriptionByName(internalName);
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
		* Convenience method for non graphical UI.  Can determine if the AttributePage contains a specific UIAttributeDescription.
		*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
		*  found, this method caches the UIAttributeDescription it has found.
		* 
		* @param internalName name of the requested UIAttributeDescription
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIAttributeDescription(String internalName){
		boolean found = false;

		if (lastAtt != null && lastAtt.getInternalName().equals(internalName)) {
			found = true;
		} else {
			for (Iterator iter = (Iterator) attributeGroups.keySet().iterator(); iter.hasNext();) {
				AttributeGroup group = (AttributeGroup) attributeGroups.get((Integer) iter.next());
				if (group.containsUIAttributeDescription(internalName)) {
					lastAtt = group.getUIAttributeDescriptionByName(internalName);
					found = true;
					break;
				}
			}
		}
		return found;
	}

	/**
	 * Convenience method. Returns all of the UIAttributeDescriptions contained in all of the AttributeGroups.
	 * 
	 * @return UIAttributeDescription[]
	 */
	public UIAttributeDescription[] getAllUIAttributeDescriptions() {
		List atts = new ArrayList();
  	
		for (Iterator iter = attributeGroups.keySet().iterator(); iter.hasNext();) {
			AttributeGroup ag = (AttributeGroup) attributeGroups.get((Integer) iter.next());
  		
			atts.addAll(Arrays.asList(ag.getAllUIAttributeDescriptions()));
		}
		
		UIAttributeDescription[] a = new UIAttributeDescription[atts.size()];
		atts.toArray(a);
		return a;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
		buf.append(", AttributeGroups=").append(attributeGroups);
		buf.append("]");

		return buf.toString();
	}

  public boolean equals(Object o) {
		if (!(o instanceof AttributePage))
			return false;

		AttributePage otype = (AttributePage) o;
		
		if (! (internalName.equals(otype.getInternalName()) ) )
			return false;
	  
		if (! (displayName.equals(otype.getDisplayName()) ) )
			return false;
	  
		if (! (description.equals(otype.getDescription()) ) )
			return false;				
		
		//other AttributePage must contain all AttributeGroups contained by this AttributePage
    for (Iterator iter = attributeGroups.values().iterator(); iter.hasNext();) {
			AttributeGroup element = (AttributeGroup) iter.next();
			
			if (! ( otype.containsAttributeGroup( element.getInternalName() ) ) )
			  return false;
			if (! ( element.equals( otype.getAttributeGroupByName( element.getInternalName() ) ) ) )
			  return false;
		}

		//this AttributePage must contain all AttributeGroups contained by other AttributePage
		AttributeGroup[] apages = otype.getAttributeGroups();
		for (int i = 0, n = apages.length; i < n; i++) {
			AttributeGroup group = apages[i];
			if (! ( attributeGroups.containsValue(group) ) )
			  return false;
		}
		
		return true;
	}
  
  public int hashCode() {
  	int tmp = internalName.hashCode();
		tmp = (31 * tmp) + displayName.hashCode();
		tmp = (31 * tmp) + description.hashCode();
		
		for (Iterator iter = attributeGroups.values().iterator(); iter.hasNext();) {
			AttributeGroup element = (AttributeGroup) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}
		
  	return tmp;
  }
  
	private final String internalName, displayName, description;
	private int agroupRank = 0;
	private TreeMap attributeGroups = new TreeMap();
	private Hashtable attGroupNameMap = new Hashtable();

	//cache one UIAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
	private UIAttributeDescription lastAtt = null;
}
