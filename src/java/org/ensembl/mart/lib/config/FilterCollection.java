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

	/**
	 * Constructor for a FilterCollection named by intenalName, with a displayName, type.
	 * 
	 * @param internalName String name to internally represent the FilterCollection.  Must not be null.
	 * @throws ConfigurationException when paremeter requirements are not met
	 */
	public FilterCollection(String internalName) throws ConfigurationException {
		this(internalName, "", "");
	}

	/**
	 * Constructor for a FilterCollection named by intenalName, with a displayName, type, and optional description.
	 * 
	 * @param internalName String name to internally represent the FilterCollection.  Must not be null or empty.
	 * @param displayName String name to represent the FilterCollection. 
	 * @param description String description of the FilterCollection.
	 * @throws ConfigurationException when paremeters are null or empty
	 */
	public FilterCollection(String internalName, String displayName, String description) throws ConfigurationException {

		super(internalName, displayName, description);
	}

	/**
	 * Add a FilterDescription object to this FilterCollection.
	 * 
	 * @param f a FilterDescription object
	 */
	public void addFilterDescription(FilterDescription f) {
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
	public void setFilterDescriptions(FilterDescription[] f) {
		for (int i = 0, n = f.length; i < n; i++) {
			Integer fRankInt = new Integer(fRank);
			uiFilters.put(fRankInt, f[i]);
			uiFilterNameMap.put(f[i].getInternalName(), fRankInt);
			fRank++;
		}
	}

	/**
	 * Returns a List of FilterDescription objects, 
	 * in the order they were added.
	 * 
	 * @return List of FilterDescription objects
	 */
	public List getFilterDescriptions() {
		return new ArrayList(uiFilters.values());
	}

	/**
	 * Returns a specific FilterDescription, named by internalName, or
	 * containing an Option named by internalName.
	 * 
	 * @param internalName String name of the requested FilterDescription
	 * @return FilterDescription requested, or null.
	 */
	public FilterDescription getFilterDescriptionByInternalName(String internalName) {
		if (containsFilterDescription(internalName))
			return lastFilt;
		else
			return null;
	}

	/**
	 * Check if this FilterCollection contains a specific FilterDescription object.
	 * 
	 * @param internalName String name of the requested FilterDescription
	 * @return boolean, true if FilterCollection contains the FilterDescription, false if not.
	 */
	public boolean containsFilterDescription(String internalName) {
		boolean contains = false;

		if (lastFilt == null) {
			contains = uiFilterNameMap.containsKey(internalName);

			if (contains)
				lastFilt = (FilterDescription) uiFilters.get((Integer) uiFilterNameMap.get(internalName));
			else if ( ( internalName.indexOf(".") > 0 ) && !( internalName.endsWith(".") ) ) {
				String[] testNames = internalName.split("\\.");
				String testRefName = testNames[0]; // x in x.y
				String testIname = testNames[1]; // y in x.y

        if (uiFilterNameMap.containsKey(testIname)) {
        	// y is an actual filter, with its values stored in a PushOption in another Filter					
					lastFilt = (FilterDescription) uiFilters.get((Integer) uiFilterNameMap.get(testIname));
					contains = true;
				} else {
					// y may be a Filter stored in a PushOption within another Filter
					for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
						FilterDescription element = (FilterDescription) iter.next();

						if (element.containsOption(testRefName)) {
							Option superOption = element.getOptionByInternalName(testRefName);
							
							PushAction[] pos = superOption.getPushActions();
							for (int i = 0, n = pos.length; i < n; i++) {
								PushAction po = pos[i];
								if (po.containsOption(testIname)) {
									lastFilt = element;
									contains = true;
									break;
								}
							}
						}
						
						if (contains)
						  break;
					}
				}
			} else {
				for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
					FilterDescription element = (FilterDescription) iter.next();
					if (element.containsOption(internalName)) {
						lastFilt = element;
						contains = true;
						break;
					}
				}
			}
		} else {
			if (lastFilt.getInternalName().equals(internalName))
				contains = true;
			else if (lastFilt.containsOption(internalName))
				contains = true;
			else if ( (internalName.indexOf(".") > 0) &&  !(internalName.endsWith(".")) && lastFilt.getInternalName().equals( internalName.split("\\.")[1] ) )
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
					if (((FilterDescription) element).supports(field, TableConstraint)) {
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

	/**
	 * Returns a List of possible internalNames to add to the MartCompleter command completion system.
	 * internalNames may be of the form x.y.  Also, internalNames that are not of the form x.y, but are found to be equal to y in
	 * another internalName of form x.y will not be added as potential completers. 
	 * @return List of potential completer names
	 */
	public List getCompleterNames() {
		List names = new ArrayList();

		for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
			FilterDescription element = (FilterDescription) iter.next();
			names.addAll(element.getCompleterNames());
		}

		return names;
	}

	/**
	 * Allows MartShell to get all values associated with a given internalName (which may be of form x.y).
	 * Behaves differently than getFilterDescriptionByInternalName when internalName is x.y and y is the name of
	 * an actual filterDescription.
	 * @param internalName
	 * @return List of values to complete
	 */
	public List getCompleterValuesByInternalName(String internalName) {
		if (internalName.indexOf(".") > 0)
			return getFilterDescriptionByInternalName(internalName.split("\\.")[0]).getCompleterValues(internalName);
		else
			return getFilterDescriptionByInternalName(internalName).getCompleterValues(internalName);
	}

	/**
	 * Allows MartShell to get all qualifiers associated with a given internalName (which may be of form x.y).
	 * Behaves differently than getFilterDescriptionByInternalName when internalName is x.y and y is the name of
	 * an actual filterDescription.
	 * @param internalName
	 * @return List of qualifiers to complete
	 */
	public List getFilterCompleterQualifiersByInternalName(String internalName) {
		if (internalName.indexOf(".") > 0 && !(internalName.endsWith(".")) && containsFilterDescription( internalName.split("\\.")[1] ) ) {
			String refname = internalName.split("\\.")[1];
			return getFilterDescriptionByInternalName(refname).getCompleterQualifiers(refname);
		} else
			return getFilterDescriptionByInternalName(internalName).getCompleterQualifiers(internalName);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
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
		int hashcode = super.hashCode();

		for (Iterator iter = uiFilters.values().iterator(); iter.hasNext();) {
			Object element = iter.next();
			hashcode = (31 * hashcode) + element.hashCode();
		}

		return hashcode;
	}

	// uiFilters
	private int fRank = 0;
	private TreeMap uiFilters = new TreeMap();
	private Hashtable uiFilterNameMap = new Hashtable();

	//cache one FilterDescription for call to containsFilterDescription or getFiterDescriptionByInternalName
	private FilterDescription lastFilt = null;

	//cache one FilterDescription for call to supports
	private FilterDescription lastSupportFilt = null;
}
