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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * PushOption specifies a set of options that should be pushed onto
 * a filter. These options replace any options currently available on
 * that filter. It contains the name of the filter, available via getRef(),
 * and the options that are to be pushed, available via getOptions().
 */
public class PushAction extends BaseConfigurationObject {

	private String ref;
	private List options = new ArrayList();
	private Option lastOption = null; // cache one Option for call to containsOption/getOptionByInternalName
	private Option lastSupportingOption = null; // cache one Option for call to supports/getOptionByFieldNameTableConstraint

  /**
   * Empty Constructor should only be used by DatasetViewEditor
   */
  public PushAction() {
    super();
  }
  
	/**
	 * @param internalName
	 * @param displayName
	 * @param description
	 * @throws ConfigurationException
	 */
	public PushAction(String internalName, String displayName, String description, String ref) throws ConfigurationException {

		super(internalName, displayName, description);

		if (ref == null || ref.equals(""))
			throw new ConfigurationException("Configuration Object must contain a ref\n");

		this.ref = ref;
	}

  /**
   * Set the internalName of the FilterDescription to push options when this PushAction is activated
   * @param string -- internalName of FilterDescription to push options when this PushAction is activated
   */
  public void setRef(String string) {
    ref = string;
  }
  
	/**
	 * @return name of filter the options should be set on.
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * @param option an option that should be set on the target filter.
	 */
	public void addOption(Option option) {
		options.add(option);
	}

  /**
   * Remove an Option from this PushAction
   * @param option Option to remove
   */
  public void removeOption(Option option) {
    options.remove(option);
  }
  
  /**
   * Insert an Option at a specific position in the Options contained in this PushAction.
   * Options occuring at or after this position are shifted right.
   * @param position -- position at which to insert the given option
   * @param option -- option to insert
   */
  public void insertOption(int position, Option option) {
    options.add(position, option);
  }
  
  /**
   * Insert an Option before an existing Option, named by internalName.
   * @param internalName -- internalName of Option before which the given Option should be inserted
   * @param option -- Option to insert
   * @throws ConfigurationException when no Option with internalName exists.
   */
  public void insertOptionBeforeOption(String internalName, Option option) throws ConfigurationException {
    if(!containsOption(internalName))
      throw new ConfigurationException("PushAction does not contain Option named " + internalName + "\n");
    insertOption(options.indexOf(lastOption), option);
  }
  
  /**
   * Insert an Option after an existing Option, named by internalName.
   * @param internalName -- internalName of Option after which the given Option should be inserted
   * @param option -- Option to insert
   * @throws ConfigurationException when no Option with internalName exists.
   */
  public void insertOptionAfterOption(String internalName, Option option) throws ConfigurationException {
    if(!containsOption(internalName))
      throw new ConfigurationException("PushAction does not contain Option named " + internalName + "\n");
    insertOption(options.indexOf(lastOption) + 1, option);
  }
  
  /**
   * Add a group of Options in one call.
   * @param os -- Array of Option objects
   */
  public void addOptions(Option[] os) {
    options.addAll(Arrays.asList(os));
  }
  
	/**
	 * @return Option[] all options to be pushed.
	 */
	public Option[] getOptions() {

		return (Option[]) options.toArray(new Option[options.size()]);
	}

	/**
	 * Determine if this PushOption contains a specific Option named by internalName.
	 * Caches the Option with this internalName if found, for subsequent call to getOptionByInternalName.
	 * @param internalName - String name mapping to an Option contained within this PushOption.
	 * @return true if this PushOption contains an Option named by internalName, false otherwise.
	 */
	public boolean containsOption(String internalName) {
		boolean ret = false;

		if (lastOption == null) {
			for (int i = 0, n = options.size(); i < n; i++) {
				Option element = (Option) options.get(i);
				if (element.getInternalName().equals(internalName)) {
					ret = true;
					lastOption = element;
					break;
				}
			}
		} else {
			if (lastOption.getInternalName().equals(internalName))
				ret = true;
			else {
				lastOption = null;
				return containsOption(internalName);
			}
		}
		return ret;
	}

	/**
	 * Get an Option with a specific internalName, contained within this PushOption.
	 * @param internalName - name mapping to an Option contained within this PushOption.
	 * @return Option named by internalName, or null
	 */
	public Option getOptionByInternalName(String internalName) {
		if (containsOption(internalName))
			return lastOption;
		else
			return null;
	}

	/**
	 * Determine if this PushOption contains an Option supporting a specific field and TableConstraint.
	 * Also caches the first supporting Option it finds, for subsequent call to getOptionByFieldNameTableConstraint.
	 * @param field - String field name in a mart database table
	 * @param tableConstraint - String tableConstraint mapping to a mart database
	 * @return true if supporting Option found, false if not
	 */
	public boolean supports(String field, String tableConstraint) {
		boolean supports = false;
		for (int i = 0, n = options.size(); i < n; i++) {
			Option element = (Option) options.get(i);
			if (element.supports(field, tableConstraint)) {
				lastSupportingOption = element;
				supports = true;
				break;
			}
		}
		return supports;
	}

	/**
	 * Get an Option supporting a specific field, tableConstraint, contained within this PushOption.
	 * Calling supports first caches the last supporting Option, if found, making subsequent calls to
	 * getOptionByFieldNameTableConstraint faster.
	 * @param field - String field name in a mart database table
	 * @param tableConstraint - String tableConstraint mapping to a mart database
	 * @return Option supporting this field, tableConstraint combination, or null.
	 */
	public Option getOptionByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supports(field, tableConstraint))
			return lastSupportingOption;
		else
			return null;
	}

	/**
	 * Get the internalName for an Option within the PushAction which supports a given field and tableConstraint.
	 * @param field -- field of the requested Option
	 * @param tableConstraint -- tableConstraint of the requestedOption
	 * @return String internalName of the Option supporting the field and tableConstraint, or null if none found
	 */
	public String geOptionInternalNameByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supports(field, tableConstraint))
			return lastSupportingOption.getInternalNameByFieldNameTableConstraint(field, tableConstraint);
		else
			return null;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append(", ref=").append(ref);
		buf.append(", options=").append(options);
		buf.append("]");

		return buf.toString();
	}
  
  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {

    int hashcode = super.hashCode();
    hashcode = (31 * hashcode) + ref.hashCode();

    for (Iterator iter = options.iterator(); iter.hasNext();) {
      hashcode = (31 * hashcode) + iter.next().hashCode();
    }

    return hashcode;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    return o instanceof PushAction && hashCode() == o.hashCode();
  }
}
