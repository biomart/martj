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
	public FilterCollection(String internalName, String type, String displayName, String filterSetName, String description) throws ConfigurationException {
		
    super( internalName, displayName, description );
    
		if ( ! ( filterSetName == null || filterSetName.equals("")  ) )
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
   * add a Option object to this FilterCollection.  Options are stored in the order that they are added.
   * @param o - an Option object
   */
  public void addOption(Option o) {
		Integer oRankInt = new Integer(oRank);
		uiOptions.put(oRankInt, o);
		uiOptionNameMap.put(o.getInternalName(), oRankInt);
		oRank++;
		hasOptions = true;
  }
  
  /**
   * Set a group of Option objects in one call.  Subsequent calls to
   * addOption or setOptions will add to what was added before, in the order that they are added.
   * @param o - an array of Option objects
   */
  public void setOptions(Option[] o) {
		for (int i = 0, n = o.length; i < n; i++) {
			Integer oRankInt = new Integer(oRank);
			uiOptions.put(oRankInt, o[i]);
			uiOptionNameMap.put(o[i].getInternalName(), oRankInt);
			oRank++;			
		}  	
		hasOptions = true;
  }
  
  /**
   * Determine if this FilterCollection has Options Available.
   * 
   * @return boolean, true if Options are available, false if not.
   */
  public boolean hasOptions() {
  	return hasOptions;
  }
  
  /**
   * Get all Option objects available as an array.  Options are returned in the order they were added.
   * @return Option[]
   */
  public Option[] getOptions() {
		Option[] ret = new Option[uiOptions.size()];
		uiOptions.values().toArray(ret);
		return ret;  	 
  }
  
  /**
   * Determine if this FilterCollection contains an Option.  This only determines if the specified internalName
   * maps to a specific Option in the FilterCollection during a shallow search.  It does not do a deep search
   * within the Options.
   * 
   * @param internalName - String name of the requested Option
   * @return boolean, true if found, false if not found.
   */
  public boolean containsOption(String internalName) {
		return uiOptionNameMap.containsKey(internalName);
  }
  
  /**
   * Get a specific Option named by internalName.  This does not do a deep search within Options.
   * 
   * @param internalName - String name of the requested Option.   * 
   * @return Option object named by internalName
   */
  public Option getOptionByName(String internalName) {
		if (uiOptionNameMap.containsKey(internalName))
				return (Option) uiOptions.get( (Integer) uiOptionNameMap.get(internalName) );
			else
				return null;  	
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
		buf.append( super.toString() );
		buf.append(", type=").append(type);

    if (hasOptions)
      buf.append(", Options=").append(uiOptions);
      
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
		int hashcode = inFilterSet ? 1 : 0;
		hashcode = super.hashCode();
		hashcode = (31 * hashcode) + filterSetName.hashCode();
		
		for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof UIFilterDescription) 
			  hashcode = (31 * hashcode) + ( (UIFilterDescription) element).hashCode();
			else
			  hashcode = (31 * hashcode) + ( (UIDSFilterDescription) element).hashCode();
		}

		for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
			Option option = (Option) iter.next();
			hashcode = (31 * hashcode) + option.hashCode();
		}
		
		return hashcode;
	}

  private String filterSetName;
  private String type;
	private boolean inFilterSet = false;
	private boolean hasOptions = false;
	
	//options
	private int oRank = 0;
	private TreeMap uiOptions = new TreeMap();
	private Hashtable uiOptionNameMap = new Hashtable();

  // uiFilters
	private int fRank = 0;
	private TreeMap uiFilters = new TreeMap();
	private Hashtable uiFilterNameMap = new Hashtable();

	//cache one UIFilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
	private UIFilterDescription lastFilt = null;
}
