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
 * Container for a set of Mart AttributeCollections.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public final class AttributeGroup extends BaseConfigurationObject {

	/*
	 * AttributeGroups must have an internalName, so disable parameterless constructor.
	 */
	private AttributeGroup() throws ConfigurationException {
		this("", "", ""); // will never happen
	}

	/**
	 * Constructor for an AttributeGroup named by internalName.
	 * 
	 * @param internalName String name to internally represent the AttributeGroup.
	 * @throws ConfigurationException when internalName is null or empty
	 */
	public AttributeGroup(String internalName) throws ConfigurationException {
		this(internalName, "", "");
	}

	/**
	 * Constructor for a fully defined AttributeGroup, with an internalName, displayName, and description.
	 * 
	 * @param internalName String name to internally represent the AttributeGroup. Must not be null or empty
	 * @param displayName String name to display in a UI.
	 * @param description String description of the AttributeGroup.
	 * @throws ConfigurationException when required parameters are null or empty
	 */
	public AttributeGroup(String internalName, String displayName, String description) throws ConfigurationException {
		super( internalName, displayName, description);
    
	}

	/**
	 * Add an AttributeColllection the the AttributeGroup.
	 * 
	 * @param c an AttributeCollection object.
	 */
	public void addAttributeCollection(AttributeCollection c) {
		Integer cRankInt = new Integer(cRank);
		attributeCollections.put(cRankInt, c);
		attributeCollectionNameMap.put(c.getInternalName(), cRankInt);
		cRank++;
	}

	/**
	 * Set a group of AttributeCollection objects with one call.  Note, subsequent calls to addAttributeCollection
	 * or setAttributeCollections will add to what has been added before.
	 * 
	 * @param c an Array of AttributeCollection objects.
	 */
	public void setAttributeCollections(AttributeCollection[] c) {
		for (int i = 0; i < c.length; i++) {
			Integer cRankInt = new Integer(cRank);
			attributeCollections.put(cRankInt, c[i]);
			attributeCollectionNameMap.put(c[i].getInternalName(), cRankInt);
			cRank++;
		}
	}

	/**
	 * Get all of the AttributeCollection objects contained in this AttributeGroup in an array, in the order they were added.
	 * 
	 * @return Array of AttributeCollection objects.
	 */
	public AttributeCollection[] getAttributeCollections() {
		AttributeCollection[] a = new AttributeCollection[attributeCollections.size()];
		attributeCollections.values().toArray(a);
		return a;
	}

	/**
	 * Returns a specific AttributeCollection, named by internalName.
	 * 
	 * @param internalName String internal name of  the AttributeCollection
	 * @return an AttributeCollection object, or null
	 */
	public AttributeCollection getAttributeCollectionByName(String internalName) {
		if (attributeCollectionNameMap.containsKey(internalName))
			return (AttributeCollection) attributeCollections.get((Integer) attributeCollectionNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check to determine if this AttributeGroup contains a specific AttributeCollection named by intenalName.
	 * 
	 * @param internalName String internal name of the AttributeCollection
	 * @return boolean, true if found, false if not.
	 */
	public boolean containsAttributeCollection(String internalName) {
		return attributeCollectionNameMap.containsKey(internalName);
	}

	/**
		* Convenience method for non graphical UI.  Allows a call against the AttributeGroup for a particular AttributeDescription.
	  * Note, it is best to first call containsUIAttributeDescription,
		* as there is a caching system to cache a AttributeDescription during a call to containsUIAttributeDescription.
		* 
		* @param internalName name of the requested AttributeDescription
		* @return AttributeDescription requested, or null
		*/
	public AttributeDescription getAttributeDescriptionByInternalName(String internalName) {
		if ( containsAttributeDescription(internalName) )
			return lastAtt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the AttributeGroup contains a specific AttributeDescription.
		*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
		*  found, this method caches the AttributeDescription it has found.
		* 
		* @param internalName name of the requested AttributeDescription
		* @return boolean, true if found, false if not.
		*/
	public boolean containsAttributeDescription(String internalName){
		boolean found = false;

    if (lastAtt == null) {
			for (Iterator iter = (Iterator) attributeCollections.keySet().iterator(); iter.hasNext();) {
				AttributeCollection collection = (AttributeCollection) attributeCollections.get((Integer) iter.next());
				if (collection.containsAttributeDescription(internalName)) {
					lastAtt = collection.getAttributeDescriptionByInternalName(internalName);
					found = true;
					break;
				}
			}    	
    }
    else {
			if (lastAtt.getInternalName().equals(internalName))
			  found = true;
			else {
			  lastAtt = null;
			  found = containsAttributeDescription(internalName);
			} 
		}
		return found;
	}

	/**
	 * Retrieve a specific AttributeDescription that supports a given field and tableConstraint.
	 * @param field
	 * @param tableConstraint
	 * @return AttributeDescription supporting the field and tableConstraint, or null
	 */
	public AttributeDescription getAttributeDescriptionByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supports(field, tableConstraint))
			return lastSupportingAttribute;
		else
			return null;
	}
  
	/**
	 * Determine if this AttributeGroup supports a given field and tableConstraint.  Caches the first supporting AttributeDescription
	 * that it finds, for subsequent call to getAttributeDescriptionByFieldNameTableConstraint.
	 * @param field
	 * @param tableConstraint
	 * @return boolean, true if an AttributeDescription contained in this AttributeGroup supports the field and tableConstraint, false otherwise
	 */
	public boolean supports(String field, String tableConstraint) {
		boolean supports = false;
  	
		for (Iterator iter = attributeCollections.values().iterator(); iter.hasNext();) {
			AttributeCollection element = (AttributeCollection) iter.next();
			
			if (element.supports(field, tableConstraint)) {
				lastSupportingAttribute = element.getAttributeDescriptionByFieldNameTableConstraint(field, tableConstraint);
				supports = true;
				break;
			}
		}
		return supports;
	}
  
  /**
   * Convenience method. Returns all of the AttributeDescription objects 
   * contained in all of the AttributeCollections.
   * 
   * @return List of AttributeDescription objects
   */
  public List getAllAttributeDescriptions() {
  	List atts = new ArrayList();
  	
  	for (Iterator iter = attributeCollections.keySet().iterator(); iter.hasNext();) {
  		AttributeCollection ac = (AttributeCollection) attributeCollections.get((Integer) iter.next());
  		
			atts.addAll(ac.getAttributeDescriptions());
		}
		
		return atts;
  }
  
  /**
   * Returns the AttributeCollection for a particular Attribute (AttributeDescription or UIDSAttributeDescription)
   * based on its internalName.
   * 
   * @param internalName - String internalName of the Attribute Description for which the collection is being requested.
   * @return AttributeCollection for the AttributeDescription provided, or null
   */
  public AttributeCollection getCollectionForAttributeDescription(String internalName) {
  	if (! containsAttributeDescription(internalName))
  	  return null;
  	else if (lastColl == null) {
			for (Iterator iter = attributeCollections.keySet().iterator(); iter.hasNext();) {
				AttributeCollection ac = (AttributeCollection) attributeCollections.get((Integer) iter.next());
				if (ac.containsAttributeDescription(internalName)) {
					lastColl = ac;
					break;
				}
			}
			return lastColl;
  	}
  	else {
  		if (lastColl.getInternalName().equals(internalName))
  		  return lastColl;
  		else {
  			lastColl = null;
  			return getCollectionForAttributeDescription(internalName);
  		}
  	}
  }
  
  /**
   * Returns a List of internalNames for the MartCompleter command completion system.
   * @return List of internalNames
   */
  public List getCompleterNames() {
  	List names = new ArrayList();
  	
  	for (Iterator iter = attributeCollections.values().iterator(); iter.hasNext();) {
			AttributeCollection acol = (AttributeCollection) iter.next();
			names.addAll(acol.getCompleterNames());
		}
  	
  	return names;
  }
  /**
   * debug output
   */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
    buf.append( super.toString() );
    buf.append(", attributeCollections=").append(attributeCollections);
		buf.append("]");

		return buf.toString();
	}

  /**
	 * Allows Equality Comparisons manipulation of AttributeGroup objects
	 */
	public boolean equals(Object o) {
		return o instanceof AttributeGroup && hashCode() == ((AttributeGroup) o).hashCode();
	}

  public int hashCode() {
		int tmp = super.hashCode();
		
		for (Iterator iter = attributeCollections.values().iterator(); iter.hasNext();) {
			AttributeCollection element = (AttributeCollection) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}
		
		return tmp;
  }
  
	private int cRank = 0;
	private TreeMap attributeCollections = new TreeMap();
	private Hashtable attributeCollectionNameMap = new Hashtable();

	//cache one AttributeDescription for call to containsUIAttributeDescription or getAttributeDescriptionByName
	private AttributeDescription lastAtt = null;
	//cache one AttributeCollecton for call to getCollectionForAttribute
	private AttributeCollection lastColl = null;
	//cache one AttributeDescription for call to supports/getAttributeDescriptionByFieldNameTableConstraint
	private AttributeDescription lastSupportingAttribute = null;
}
