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
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Option extends BaseConfigurationObject {
  
  
  
  private String field;
  private String tableConstraint;
  private String value;
  private String ref;
  
	public Option() throws ConfigurationException {
		this("", false, "", "", "", "", "", "");
	}

	public Option(String internalName, boolean isSelectable)
		throws ConfigurationException {
		this(internalName, isSelectable, "", "", "", "", "", "");
	}

	public Option(
		String internalName,
		boolean isSelectable,
		String displayName,
		String description,
    String field,
    String tableConstraint,
    String value,
    String ref)
		throws ConfigurationException {

		super(internalName, displayName, description);

		this.isSelectable = isSelectable;
    this.field = field;
    this.tableConstraint = tableConstraint;
  
    this.value = value;
    this.ref = ref;

	}

	/**
	 * adda Option object to this Option.  Options are stored in the order that they are added.
	 * @param o - an Option object
	 */
	public void addOption(Option o) {
		Integer oRankInt = new Integer(oRank);
		uiOptions.put(oRankInt, o);
		uiOptionNameMap.put(o.getInternalName(), oRankInt);
		oRank++;
		hasOptions = true;
    hashcode = -1;
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
    hashcode = -1;
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
	 * Determine if this Option contains an Option.  This only determines if the specified internalName
	 * maps to a specific Option in the Option during a shallow search.  It does not do a deep search
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
	 * @param internalName - String name of the requested Option.
	 * @return Option object named by internalName
	 */
	public Option getOptionByName(String internalName) {
		if (uiOptionNameMap.containsKey(internalName))
			return (Option) uiOptions.get(
				(Integer) uiOptionNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Determine if this Option is Selectable in the UI.
	 * @return boolean, true if selectable, false otherwise
	 */
	public boolean isSelectable() {
		return isSelectable;
	}

	/**
	 * Determine if this Option has underlying Options.
	 * @return boolean, true if this Option has underlying options, false if not.
	 */
	public boolean hasOptions() {
		return hasOptions;
	}

	/**
	 * Debug output
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append(", isSelectable=").append(isSelectable);

		if (hasOptions)
			buf.append(", options=").append(uiOptions);
		buf.append("]");

		return buf.toString();
	}
	/**
	 * Allows Equality Comparisons manipulation of Option objects
	 */
	public boolean equals(Object o) {
		return o instanceof Option && hashCode() == ((Option) o).hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {

		if (hashcode == -1) {

			hashcode = super.hashCode();

			hashcode += (isSelectable) ? 1 : 0;

			for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
				Option option = (Option) iter.next();
				hashcode = (31 * hashcode) + option.hashCode();
			}
		}
		return hashcode;
	}

  /**
     * @return
     */
    public String getField() {
      return field;
    }

    /**
     * @return
     */
    public String getTableConstraint() {
      return tableConstraint;
    }

	private int hashcode = -1;

	private final boolean isSelectable;
	private boolean hasOptions = false;

	//options can contain options
	private int oRank = 0;
	private TreeMap uiOptions = new TreeMap();
	private Hashtable uiOptionNameMap = new Hashtable();
	

	/**
	 * @return
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * @return
	 */
	public String getValue() {
		return value;
	}

}
