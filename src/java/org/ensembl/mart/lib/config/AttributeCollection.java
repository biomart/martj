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

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributeCollection extends BaseConfigurationObject {

  private int maxSelect;
  private List uiAttributes = new ArrayList();
  private Hashtable uiAttributeNameMap = new Hashtable();

  //cache one AttributeDescription for call to supports/getAttributeDescriptionByFieldNameTableConstraint
  private AttributeDescription lastSupportingAttribute = null;

  /**
   * Copy constructor.  Constructs a new AttributeCollection which
   * is an exact copy of an existing AttributeCollection.
   * @param ac AttributeCollection to copy
   */
  public AttributeCollection(AttributeCollection ac) {
  	super(ac);
  	
  	maxSelect = ac.getMaxSelect();
  	
  	List ads = ac.getAttributeDescriptions();
  	for (int i = 0, n = ads.size(); i < n; i++) {
      Object ad = ads.get(i);
      if (ad instanceof AttributeDescription)
        addAttributeDescription( new AttributeDescription( (AttributeDescription) ad ) );
      //else not needed      
    }  
  }
  /**
   * Empty Constructor should only be used by DatasetViewEditor.
   *
   */
  public AttributeCollection() {
    super();
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
    super( internalName, displayName, description);
		this.maxSelect = maxSelect;
	}

  /**
   * Set the maxSelect value for this AttributeCollection
   * @param i -- int value to limit selections of Attributes in groups.
   */
  public void setMaxSelect(int i) {
    maxSelect = i;
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
	 * Add a AttributeDescription to the AtttributeCollection.
	 * 
	 * @param a a AttributeDescription object.
	 */
	public void addAttributeDescription(AttributeDescription a) {
		uiAttributes.add(a);
		uiAttributeNameMap.put(a.getInternalName(), a);
	}

  /**
   * Remove an AttributeDescription from this AttributeCollection.
   * @param a -- AttributeDescription to be removed.
   */
  public void removeAttributeDescription(AttributeDescription a) {
    uiAttributeNameMap.remove(a.getInternalName());
    uiAttributes.remove(a);
  }
  
  /**
   * Insert an AttributeDescription at a particular position within the AttributeCollection.
   * AttributeDescriptions set at or after the given position are shift right.
   * @param position -- position at which to insert the given AttributeDescription
   * @param a -- AttributeDescription to insert
   */
  public void insertAttributeDescription(int position, AttributeDescription a) {
    uiAttributes.add(position, a);
    uiAttributeNameMap.put(a.getInternalName(), a);
  }
  
  /**
   * Insert an AttributeDescription before a specific AttributeDescription, named by internalName.
   * @param internalName -- AttributeDescription before which the given AttributeDescription should be inserted.
   * @param a -- AttributeDescription to insert.
   * @throws ConfigurationException when the AttributeCollection does not contain an AttributeDescription named by internalName.
   */
  public void insertAttributeDescriptionBeforeAttributeDescription(String internalName, AttributeDescription a) throws ConfigurationException {
    if (!uiAttributeNameMap.containsKey(internalName))
      throw new ConfigurationException("AttributeCollection does not contain AttributeDescription " + internalName + "\n");
    insertAttributeDescription( uiAttributes.indexOf( uiAttributeNameMap.get(internalName) ), a );
  }
  
  /**
   * Insert an AttributeDescription after a specific AttributeDescription, named by internalName.
   * @param internalName -- AttributeDescription after which the given AttributeDescription should be inserted.
   * @param a -- AttributeDescription to insert.
   * @throws ConfigurationException when the AttributeCollection does not contain an AttributeDescription named by internalName.
   */
  public void insertAttributeDescriptionAfterAttributeDescription(String internalName, AttributeDescription a) throws ConfigurationException {
    if (!uiAttributeNameMap.containsKey(internalName))
      throw new ConfigurationException("AttributeCollection does not contain AttributeDescription " + internalName + "\n");
    insertAttributeDescription( uiAttributes.indexOf( uiAttributeNameMap.get(internalName) ) + 1, a );
  }
  
	/**
	 * Add a group of AttributeDescription objects in one call.  Note, subsequent calls to addAttributeDescription or addAttributeDescriptions
   * will add to what was added before.
	 * 
	 * @param a an Array of AttributeDescription objects.
	 */
	public void addAttributeDescriptions(AttributeDescription[] a) {
		for (int i = 0, n = a.length; i < n; i++) {
			uiAttributes.add(a[i]);
			uiAttributeNameMap.put(a[i].getInternalName(), a[i]);
		}
	}
	
	/**
	 * Returns a List of AttributeDescription objects, in the order they were added.
	 * 
	 * @return List of AttributeDescription objects.
	 */
	public List getAttributeDescriptions() {
    //return a copy
		return new ArrayList(uiAttributes);
	}


	/**
		* Get a specific AttributeDescription, named by internalName.
		*  
		* @param internalName name of the requested AttributeDescription
		* @return AttributeDescription requested, or null
		*/
	public AttributeDescription getAttributeDescriptionByInternalName(String internalName) {
		if ( containsAttributeDescription(internalName) )
			return (AttributeDescription) uiAttributeNameMap.get(internalName);
		else
			return null;
	}

	/**
		* Check if this AttributeCollection contains a specific AttributeDescription named
		* by internalName.
		*  
		* @param internalName name of the requested AttributeDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsAttributeDescription(String internalName) {
  	return uiAttributeNameMap.containsKey(internalName);
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
   * Determine if this AttributeCollection supports a given field and tableConstraint.  Caches the first supporting AttributeDescription
   * that it finds, for subsequent call to getAttributeDescriptionByFieldNameTableConstraint.
   * @param field
   * @param tableConstraint
   * @return boolean, true if an AttributeDescription contained in this AttributeCollection supports the field and tableConstraint, false otherwise
   */
  public boolean supports(String field, String tableConstraint) {
  	boolean supports = false;
  	
  	for (Iterator iter = uiAttributes.iterator(); iter.hasNext();) {
			AttributeDescription element = (AttributeDescription) iter.next();
			
			if (element.supports(field, tableConstraint)) {
				lastSupportingAttribute = element;
				supports = true;
				break;
			}
		}
  	return supports;
  }
  
  /**
   * Returns a List of possible internalNames to add to the MartCompleter command completion system.
   * @return List of possible completions.
   */
  public List getCompleterNames() {
  	List names = new ArrayList();
  	
  	for (Iterator iter = uiAttributes.iterator(); iter.hasNext();) {
			AttributeDescription element = (AttributeDescription) iter.next();
			names.add(element.getInternalName());
		}
  	return names;
  }
  
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append( super.toString() );
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
		int tmp = super.hashCode();
		
		for (Iterator iter = uiAttributes.iterator(); iter.hasNext();) {
			AttributeDescription element = (AttributeDescription) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}
		return tmp;
	}
}
