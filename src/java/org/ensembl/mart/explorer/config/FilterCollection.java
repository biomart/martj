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
public class FilterCollection {

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
	public FilterCollection(String internalName, String type, String displayName, String filterSetName, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals("") || type == null || type.equals(""))
			throw new ConfigurationException("FilterCollections must have an internalName and type");

		this.internalName = internalName;
		this.displayName = displayName;
		
		if ( ! ( filterSetName == null || filterSetName.equals("")  ) )
		 inFilterSet = true;
		 
		this.filterSetName = filterSetName;
		this.description = description;
		this.type = type;
	}

	/**
	 * Returns the internalName of the FilterCollection
	 * 
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the displayName of the FilterGroup.
	 * 
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the description of the FilterCollection
	 * 
	 * @return String description
	 */
	public String getDescription() {
		return description;
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
	 * Add a UIFilterDescription object to this FilterCollection.
	 * 
	 * @param f a UIFilterDescription object
	 */
	public void addUIFilter(UIFilterDescription f) {
		Integer fRankInt = new Integer(fRank);
		uiFilters.put(fRankInt, f);
		uiFilterNameMap.put(f.getInternalName(), fRankInt);
		fRank++;
	}

	/**
	 * Set a group of UIFilterDescription objects in one call.
	 * Note, subsequent calls to addUIFilter and setUIFilter will add to what has
	 * been added previously.
	 * 
	 * @param f an array of UIFilterDescription objects.
	 */
	public void setUIFilters(UIFilterDescription[] f) {
		for (int i = 0, n = f.length; i < n; i++) {
			Integer fRankInt = new Integer(fRank);
			uiFilters.put(fRankInt, f[i]);
			uiFilterNameMap.put(f[i].getInternalName(), fRankInt);
			fRank++;
		}
	}

	/**
	 * add a UIDSFilterDescription object to this FilterCollection. Both UIFIlterDescriptions
	 * and UIDSFIlterDescriptions are stored in the same List, in the order they are
	 * added.  Subsequent calls to add or set methods for both types of filters
	 * will add to all filters added previously. 
	 * @param f a UIDSFilterDescription object
	 */
	public void addUIDSFilterDescription(UIDSFilterDescription f) {
		Integer fRankInt = new Integer(fRank);
		uiFilters.put(fRankInt, f);
		uiFilterNameMap.put(f.getInternalName(), fRankInt);
		fRank++;
	}

	/**
	 * set a group of UIDSFilterDescription objects to this FilterCollection in one call. 
	 * Both UIFIlterDescriptions and UIDSFIlterDescriptions are stored in the same List, 
	 * in the order they are added.  Subsequent calls to add or set methods for both types of 
	 * filters will add to all filters added previously.
	 * @param f an array of UIDSFilterDescription objects
	 */
	public void setUIDSFilterDescriptions(UIDSFilterDescription[] f) {
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
	 * Returns a List of UIFilterDescription/UIDSFilterDescription objects, 
	 * in the order they were added.
	 * 
	 * @return List of FilterDescription objects
	 */
	public List getUIFilterDescriptions() {
		return new ArrayList(uiFilters.values());
	}

	/**
	 * Returns a specific UIFilterDescription/UIDSFilterDescription, named by internalName.
	 * 
	 * @param internalName String name of the requested FilterDescription
	 * @return Object requested, or null.
	 */
	public Object getUIFilterDescriptionByName(String internalName) {
		if (uiFilterNameMap.containsKey(internalName))
			return uiFilters.get((Integer) uiFilterNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check if this FilterCollection contains a specific UIFilterDescription/UIDSFilterDescription object.
	 * 
	 * @param internalName String name of the requested FilterDescription
	 * @return boolean, true if FilterCollection contains the UIFilterDescription, false if not.
	 */
	public boolean containsUIFilterDescription(String internalName) {
		return uiFilterNameMap.containsKey(internalName);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
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
		return o instanceof FilterCollection && hashCode() == ((FilterCollection) o).hashCode();
	}

	public int hashCode() {
		int tmp = inFilterSet ? 1 : 0;
		tmp = (31 * tmp) + internalName.hashCode();
		tmp = (31 * tmp) + displayName.hashCode();
		tmp = (31 * tmp) + filterSetName.hashCode();
		tmp = (31 * tmp) + description.hashCode();

		for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof UIFilterDescription) 
			  tmp = (31 * tmp) + ( (UIFilterDescription) element).hashCode();
			else
			  tmp = (31 * tmp) + ( (UIDSFilterDescription) element).hashCode();
		}

		return tmp;
	}

	private final String internalName, displayName, description, filterSetName, type;
	private boolean inFilterSet = false;

	private int fRank = 0;
	private TreeMap uiFilters = new TreeMap();
	private Hashtable uiFilterNameMap = new Hashtable();

	//cache one UIFilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
	private UIFilterDescription lastFilt = null;
}
