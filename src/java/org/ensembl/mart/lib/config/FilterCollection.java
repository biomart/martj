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
 * Container for a group of Mart FilterCollections.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterCollection extends BaseConfigurationObject {

	/*
	 * FilterCollection must have a internalName, and type.  So disable parameterless constructor
	 */
	private FilterCollection() throws ConfigurationException {
		this("", "", "", "", "");
	}

	/**
	 * Constructor for a FilterCollection named by intenalName, with a displayName, type.
	 * 
	 * @param internalName String name to internally represent the FilterCollection.  Must not be null.
	 * @param type String type of the FilterCollection. Must not be null.
	 * @throws ConfigurationException when paremeter requirements are not met
	 */
	public FilterCollection(String internalName, String type) throws ConfigurationException {
		this(internalName, type, "", "", "");
	}

	/**
	 * Constructor for a FilterCollection named by intenalName, with a displayName, type, and optional description.
	 * 
	 * @param internalName String name to internally represent the FilterCollection.  Must not be null or empty.
	 * @param type String type of the FilterCollection. Must not be null or empty.
	 * @param displayName String name to represent the FilterCollection.
	 * @param filterSetName String internalName of the FilterSet this FilterCollection to which this FilterCollection is a member.  May be null. 
	 * @param description String description of the FilterCollection.
	 * @throws ConfigurationException when paremeters are null or empty
	 */
	public FilterCollection(
		String internalName,
		String type,
		String displayName,
		String filterSetName,
		String description)
		throws ConfigurationException {

		super(internalName, displayName, description);

		if (!(filterSetName == null || filterSetName.equals("")))
			inFilterSet = true;

		this.filterSetName = filterSetName;
		this.type = type;
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
	 * Add a FilterDescription object to this FilterCollection.
	 * 
	 * @param f a FilterDescription object
	 */
	public void addUIFilter(FilterDescription f) {
		Integer fRankInt = new Integer(fRank);
		uiFilters.put(fRankInt, f);
		uiFilterNameMap.put(f.getInternalName(), fRankInt);
		fRank++;
	}

	/**
	 * Set a group of FilterDescription objects in one call.
	 * Note, subsequent calls to addUIFilter and setUIFilter will add to what has
	 * been added previously.
	 * 
	 * @param f an array of FilterDescription objects.
	 */
	public void setUIFilters(FilterDescription[] f) {
		for (int i = 0, n = f.length; i < n; i++) {
			Integer fRankInt = new Integer(fRank);
			uiFilters.put(fRankInt, f[i]);
			uiFilterNameMap.put(f[i].getInternalName(), fRankInt);
			fRank++;
		}
	}

	/**
	 * add a MapFilterDescription object to this FilterCollection. Both UIFIlterDescriptions
	 * and UIDSFIlterDescriptions are stored in the same List, in the order they are
	 * added.  Subsequent calls to add or set methods for both types of filters
	 * will add to all filters added previously. 
	 * @param f a MapFilterDescription object
	 */
	public void addUIDSFilterDescription(MapFilterDescription f) {
		Integer fRankInt = new Integer(fRank);
		uiFilters.put(fRankInt, f);
		uiFilterNameMap.put(f.getInternalName(), fRankInt);
		fRank++;
	}

	/**
	 * set a group of MapFilterDescription objects to this FilterCollection in one call. 
	 * Both UIFIlterDescriptions and UIDSFIlterDescriptions are stored in the same List, 
	 * in the order they are added.  Subsequent calls to add or set methods for both types of 
	 * filters will add to all filters added previously.
	 * @param f an array of MapFilterDescription objects
	 */
	public void setUIDSFilterDescriptions(MapFilterDescription[] f) {
		for (int i = 0, n = f.length; i < n; i++) {
			Integer fRankInt = new Integer(fRank);
			uiFilters.put(fRankInt, f[i]);
			uiFilterNameMap.put(f[i].getInternalName(), fRankInt);
			fRank++;
		}
	}

	/**
	 * Returns the internalName of the FilterSet this Collection belongs within.
	 * 
	 * @return String filterSetName
	 */
	public String getFilterSetName() {
		return filterSetName;
	}

	/**
	 * Check if this FilterCollection is a member of a FilterSet.
	 * 
	 * @return boolean true if member of a FilterSet, false if not
	 */
	public boolean inFilterSet() {
		return inFilterSet;
	}

	/**
	 * Returns a List of FilterDescription/MapFilterDescription objects, 
	 * in the order they were added.
	 * 
	 * @return List of FilterDescription objects
	 */
	public List getFilterDescriptions() {
		return new ArrayList(uiFilters.values());
	}

	/**
	 * Returns a specific FilterDescription/MapFilterDescription, named by internalName, or
	 * containing an Option named by internalName.
	 * 
	 * @param internalName String name of the requested FilterDescription
	 * @return Object requested, or null.
	 */
	public Object getFilterDescriptionByInternalName(String internalName) {
		if (containsFilterDescription(internalName))
		  return lastFilt;
		else
		  return null;
	}

	/**
	 * Check if this FilterCollection contains a specific FilterDescription/MapFilterDescription object.
	 * 
	 * @param internalName String name of the requested FilterDescription
	 * @return boolean, true if FilterCollection contains the FilterDescription, false if not.
	 */
	public boolean containsFilterDescription(String internalName) {
		boolean contains = false;

		if (lastFilt == null) {
			contains = uiFilterNameMap.containsKey(internalName);

			if (!contains) {
				for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
					Object element = iter.next();
					if (element instanceof FilterDescription) {
						if (((FilterDescription) element).containsOption(internalName)) {
							lastFilt = element;
							contains = true;
							break;
						}
					}
				}
			}
		} else {
			if (lastFilt instanceof FilterDescription && ( (FilterDescription) lastFilt).getInternalName().equals(internalName))
			  contains = true;
			else if (lastFilt instanceof MapFilterDescription && ( (MapFilterDescription) lastFilt).getInternalName().equals(internalName))
				contains = true;
			else {
				lastFilt = null;
				contains = containsFilterDescription(internalName);
			}
		}
		return contains;
	}

	/**
	 * Get a FilterDescription for a given field and tableConstraint.  Useful for mapping the field and tableConstraint from a Filter
	 * object added to a Query object back to its MartConfiguration FilterDescription.
	 * @param field -- String mart database field
	 * @param tableConstraint -- String mart database tableConstraint
	 * @return FilterDescription supporting the given field and tableConstraint, or null.
	 */
	public FilterDescription getFilterDescriptionByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supports(field, tableConstraint))
			return lastSupportFilt;
		else
			return null;
	}

  /**
   * Determine if this FilterCollection contains a FilterDescription supporting a given field and tableConstraint.
   * @param field - String field of a mart database table
   * @param TableConstraint -- String tableConstraint of a mart database table
   * @return boolean, true if a FilterDescription contained within this collection supports the field and tableConstraint, false otherwise.
   */
	public boolean supports(String field, String TableConstraint) {
		boolean supports = false;

    if (lastSupportFilt == null) {
    	for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
				Object element = iter.next();
				
				if (element instanceof FilterDescription) {
					if ( ( (FilterDescription) element ).supports(field, TableConstraint) ) {
						lastSupportFilt = (FilterDescription) element;
						supports = true;
						break; 
					}
				}
			}
    } else {
    	if (lastSupportFilt.supports(field, TableConstraint))
    	  supports = true;
    	else {
    		lastSupportFilt = null;
    		supports = supports(field, TableConstraint);
    	}
    }
		return supports;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append(", type=").append(type);

		if (inFilterSet)
			buf.append(", filterSetName=").append(filterSetName);

		buf.append(", UIFilterDescriptions=").append(uiFilters);
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of FilterCollection objects
	 */
	public boolean equals(Object o) {
		return o instanceof FilterCollection && hashCode() == o.hashCode();
	}

	public int hashCode() {
		int hashcode = inFilterSet ? 1 : 0;
		hashcode = super.hashCode();
		hashcode = (31 * hashcode) + filterSetName.hashCode();

		for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof FilterDescription)
				hashcode = (31 * hashcode) + ((FilterDescription) element).hashCode();
			else
				hashcode = (31 * hashcode) + ((MapFilterDescription) element).hashCode();
		}

		return hashcode;
	}

	private String filterSetName;
	private String type;
	private boolean inFilterSet = false;
	// uiFilters
	private int fRank = 0;
	private TreeMap uiFilters = new TreeMap();
	private Hashtable uiFilterNameMap = new Hashtable();

	//cache one FilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
	private Object lastFilt = null;

	//cache one FilterDescription for call to supports
	private FilterDescription lastSupportFilt = null;
}
