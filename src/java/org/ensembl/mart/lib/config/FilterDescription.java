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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Contains all of the information necessary for the UI to display the information for a specific filter,
 * and add this filter as a Filter to a Query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterDescription extends BaseConfigurationObject {

	/**
	 * Constructor for a FilterDescription named by internalName internally, with a field, type, and qualifier.
	 * 
	 * @param internalName String internal name of the FilterDescription. Must not be null or empty.
	 * @param field String name of the field to reference in the mart. Must not be null or empty.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param qualifier String qualifier to use in a SQL where clause.
	 * @throws ConfigurationException when required values are null or empty, or when a filterSetName is set, but no filterSetReq is submitted.
	 */
	public FilterDescription(String internalName, String field, String type, String qualifier)
		throws ConfigurationException {
		this(internalName, field, type, qualifier, "", "", "", "");
	}

	/**
	 * Constructor for a FilterDescription named by internalName internally, with a field, type, and qualifier.
	 * 
	 * @param internalName String internal name of the FilterDescription. Must not be null or empty.
	 * @param field String name of the field to reference in the mart.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param qualifier String qualifier to use in a SQL where clause.
	 * @param displayName String name to display in a UI
	 * @param tableConstraint String table basename to constrain SQL field
	 * @param filterSetReq String, which of the modifications specified by a FilterSetDescription are required by this FilterDescription
	 * @param description String description of the Filter
	 * @param optionName String name represention an Option that holds Options for this FilterDescription
	 * 
	 * @throws ConfigurationException when required values are null or empty
	 * @see FilterSet
	 * @see FilterDescription
	 */
	public FilterDescription(
		String internalName,
		String field,
		String type,
		String qualifier,
		String displayName,
		String tableConstraint,
		String filterSetReq,
		String description)
		throws ConfigurationException {

		super(internalName, displayName, description);

		if (type == null || type.equals(""))
			throw new ConfigurationException("FilterDescription requires a type.");

		this.field = field;
		this.type = type;
		this.qualifier = qualifier;
		this.tableConstraint = tableConstraint;
		this.filterSetReq = filterSetReq;

		if (!(filterSetReq == null || filterSetReq.equals("")))
			inFilterSet = true;
	}

	/**
	 * returns the field.
	 * @return String field
	 */
	public String getField() {
		return field;
	}

	/**
	 * Returns the field, given an internalName which may, in some cases, map to an Option instead of this FilterDescription.
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return String field
	 */
	public String getField(String internalName) {
		if (this.internalName.equals(internalName))
			return field;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getField();
			else
				return null;
		}
	}

	/**
	 * Returns the type.
	 * 
	 * @return String type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the qualifier to use SQL where clause.
	 * 
	 * @return String qualifier
	 */
	public String getQualifier() {
		return qualifier;
	}

  public String getInternalNameByFieldNameTableConstraint(String field, String tableConstraint) {
  	String ret = null;
  	
  	if (supports(field, tableConstraint)) {
  		if (this.field != null && this.field.equals(field) && this.tableConstraint != null && this.tableConstraint.equals(tableConstraint))
  		  ret = internalName;
  		else {
  			  for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
						Option element = (Option) iter.next();
						if (element.supports(field, tableConstraint)) {
							ret = element.getInternalName();
							break;
						}
					}
  		}
  	}

  	  return ret;
  }
  
	/**
		 * Returns the tableConstraint for the field.
		 * 
		 * @return String tableConstraint
		 */
	public String getTableConstraint() {
		return tableConstraint;
	}

	/**
	 * Returns the tableConstraint, given an internalName which may, in some cases, map to an Option instead of this FilterDescription.
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return String tableConstraint
	 */
	public String getTableConstraint(String internalName) {
		if (this.internalName.equals(internalName))
			return tableConstraint;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getTableConstraint();
			else
				return null;
		}
	}

	/**
	 * Returns a value to determine which FilterDescription SQL specifier (tableConstraint or field) to modify
	 * with contents from the FilterSetDescription.  Must match one of the static ints defined by FilterSetDescription.
	 * 
	 * @return String filterSetReq
	 * @see FilterSetDescription
	 */
	public String getFilterSetReq() {
		return filterSetReq;
	}

	/**
	 * Returns the same as getFilterSetReq, but using an internalName that may, in some cases, map to an Option
	 * contained in this FilterDescription, instead of this FilterDescription itself.
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return String filterSetReq
	 * @see FilterSetDescription
	 */
	public String getFilterSetReq(String internalName) {
		if (this.internalName.equals(internalName))
			return filterSetReq;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getFilterSetReq();
			else
				return null;
		}
	}

	/**
	 * Check to see if this FilterDescription is in a FilterSet
	 * 
	 * @return true if it is in a FilterSet, false if not
	 */
	public boolean inFilterSet() {
		return inFilterSet;
	}

	/**
	 * Returns the same as inFilterSet, but with an internalName which may, in some cases, map to an Option contained
	 * in this FilterDescription, rather than this FilterDescription itself.
	 * 
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return boolean, If the internalName matches the internalName of this FilterDescription object, returns true if this object
	 * is in a FilterSet, false otherwise.  If the internalName does not match that of this FilterDescription, then, if it maps to an Option
	 * contained within the FilterDescription, returns true if that Option is in a FilterSet, false if not.  If the internalName does not map
	 * to an Option, returns true if this FilterDescription is in a filterset, false otherwise.
	 */
	public boolean inFilterSet(String internalName) {
		if (this.internalName.equals(internalName))
			return inFilterSet;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).inFilterSet();
			else
				return inFilterSet;
		}
	}

	public boolean supports(String field, String tableConstraint) {
		boolean supports = (this.field != null
		&& this.field.equals(field)
		&& this.tableConstraint != null
		&& this.tableConstraint.equals(tableConstraint));

		if (!supports) {
			if (lastSupportingOption == null) {
				for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
					Option element = (Option) iter.next();
					if (element.supports(field, tableConstraint)) {
						lastSupportingOption = element;
						supports = true;
						break;
					}
				}
			} else {
				if (lastSupportingOption.supports(field, tableConstraint))
					supports = true;
        else {
        	lastSupportingOption = null;
        	return supports(field, tableConstraint);
        }
			}
		}

		return supports;
	}

	/**
	 * debug output
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[ FilterDescription:");
		buf.append(super.toString());
		buf.append(", field=").append(field);
		buf.append(", type=").append(type);
		buf.append(", qualifier=").append(qualifier);
		buf.append(", tableConstraint=").append(tableConstraint);

		if (inFilterSet)
			buf.append(", filterSetReq=").append(filterSetReq);

		if (hasOptions)
			buf.append(", Options=").append(uiOptions);
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Collections manipulation of FilterDescription objects
	 */
	public boolean equals(Object o) {
		return o instanceof FilterDescription && hashCode() == o.hashCode();
	}

	public int hashCode() {

		if (hshcode == -1) {

			hshcode = inFilterSet ? 1 : 0;
			hshcode = (31 * hshcode) + internalName.hashCode();
			hshcode = (31 * hshcode) + displayName.hashCode();
			hshcode = (31 * hshcode) + field.hashCode();
			hshcode = (31 * hshcode) + type.hashCode();
			hshcode = (31 * hshcode) + qualifier.hashCode();
			hshcode = (31 * hshcode) + tableConstraint.hashCode();
			hshcode = (31 * hshcode) + filterSetReq.hashCode();
			hshcode = (31 * hshcode) + description.hashCode();
			for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
				Option option = (Option) iter.next();
				hshcode = (31 * hshcode) + option.hashCode();
			}

		}
		return hshcode;
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
		hshcode = -1;
	}

	/**
	 * Determine if this FilterDescription contains an Option.  This only determines if the specified internalName
	 * maps to a specific Option in the FilterDescription during a shallow search.  It does not do a deep search
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
			return (Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName));
		else
			return null;
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
		hshcode = -1;
	}

	/**
	 * Determine if this FilterCollection has Options Available.
	 * 
	 * @return boolean, true if Options are available, false if not.
	 */
	public boolean hasOptions() {
		return hasOptions;
	}

	private Hashtable uiOptionNameMap = new Hashtable();
	private TreeMap uiOptions = new TreeMap();
	private boolean hasOptions = false;
	private int oRank = 0;

	private String field;
	private String type;
	private String qualifier;
	private String filterSetReq;
	private String tableConstraint;
	private boolean inFilterSet = false;
	private int hshcode = -1;

	//cache one supporting Option for call to supports
	Option lastSupportingOption = null;
}
