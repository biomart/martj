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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
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
	 * Constructor for a FilterDescription named by internalName internally, with a field, type, and qualifiers.
	 * 
	 * @param internalName String internal name of the FilterDescription. Must not be null or empty.
	 * @param field String name of the field to reference in the mart.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param qualifiers String, comma-separated list of qualifiers to use in a MartShell MQL
	 * @throws ConfigurationException when required values are null or empty, or when a filterSetName is set, but no filterSetReq is submitted.
	 */
	public FilterDescription(String internalName, String field, String type, String qualifiers)
		throws ConfigurationException {
		this(internalName, field, type, qualifiers, "", "", null, "");
	}

	/**
	 * Constructor for a fully defined FilterDescription
	 * 
	 * @param internalName String internal name of the FilterDescription. Must not be null or empty.
	 * @param field String name of the field to reference in the mart.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param qualifiers String, comma-separated list of qualifiers to use in a MartShell MQL
	 * @param displayName String name to display in a UI
	 * @param tableConstraint String table basename to constrain SQL field
	 * @param handler String, specifying the handler to use for a FilterDescription
	 * @param description String description of the Filter
	 * 
	 * @throws ConfigurationException when required values are null or empty
	 * @see FilterSet
	 * @see FilterDescription
	 */
	public FilterDescription(
		String internalName,
		String field,
		String type,
		String qualifiers,
		String displayName,
		String tableConstraint,
		String handler,
		String description)
		throws ConfigurationException {

		super(internalName, displayName, description);

		if (type == null || type.equals(""))
			throw new ConfigurationException("FilterDescription requires a type.");

		this.field = field;
		this.type = type;
		this.qualifiers = qualifiers;
		this.handler = handler;
		this.tableConstraint = tableConstraint;
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
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return field;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(optionIname))).getField(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
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
	 * Returns the type, given an internalName which may, in some cases, map to an Option instead of this FilterDescription.
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return String type
	 */
	public String getType(String internalName) {
		if (this.internalName.equals(internalName))
			return type;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getType();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return type;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(optionIname))).getType(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
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
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return tableConstraint;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(optionIname))).getTableConstraint(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Get the Handler for this FilterDescription, if any
	 * @return String handler, or null.
	 */
	public String getHandler() {
		return handler;
	}

	/**
	 * Returns the handler, given an internalName which may, in some cases, map to an Option instead of this FilterDescription.
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return String handler
	 */
	public String getHandler(String internalName) {
		if (this.internalName.equals(internalName))
			return handler;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getHandler();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return handler;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(optionIname))).getHandler(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Returns the qualifiers to use in MartShell.
	 * 
	 * @return String qualifiers
	 */
	public String getQualifiers() {
		return qualifiers;
	}

	public String getQualifiers(String internalName) {
		if (this.internalName.equals(internalName))
			return qualifiers;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getQualifiers();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(optionIname))).getQualifiers(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	public String getInternalNameByFieldNameTableConstraint(String field, String tableConstraint) {
		String ret = null;

		if (supports(field, tableConstraint)) {
			if (this.field != null
				&& this.field.equals(field)
				&& this.tableConstraint != null
				&& this.tableConstraint.equals(tableConstraint))
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

	public boolean supports(String field, String tableConstraint) {
		boolean supports =
			(this.field != null
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
		buf.append(", qualifiers=").append(qualifiers);
		buf.append(", tableConstraint=").append(tableConstraint);

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
			hshcode = super.hashCode();
			hshcode = (31 * hshcode) + field.hashCode();
			hshcode = (31 * hshcode) + type.hashCode();
			hshcode = (31 * hshcode) + qualifiers.hashCode();
			hshcode = (31 * hshcode) + tableConstraint.hashCode();
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

	/**
	 * Add an Enable object to this FilterDescription, allowing it to Enable another FilterDescription in the GUI.
	 * @param e, Enable Object to add.
	 */
	public void addEnable(Enable e) {
		Enables.add(e);
	}

	/**
	 * Set an array of Enable objects in one call, adds to what was added via previous calls to addEnable/setEnables methods.
	 * @param e - Array of Enable Objects
	 */
	public void setEnables(Enable[] e) {
		for (int i = 0, n = e.length; i < n; i++) {
			Enables.add(e[i]);
		}
	}

	/**
	 * Get all Enable Objects for this FilterDescription in an Array.
	 * @return Array of Enable Objects
	 */
	public Enable[] getEnables() {
		Enable[] ret = new Enable[Enables.size()];
		Enables.toArray(ret);
		return ret;
	}

	/**
	 * Add an Disable object to this FilterDescription, allowing it to Disable another FilterDescription in the GUI.
	 * @param e, Disable Object to add.
	 */
	public void addDisable(Disable e) {
		Disables.add(e);
	}

	/**
	 * Set an array of Disable objects in one call, adds to what was added via previous calls to addDisable/setDisables methods.
	 * @param e - Array of Disable Objects
	 */
	public void setDisables(Disable[] e) {
		for (int i = 0, n = e.length; i < n; i++) {
			Disables.add(e[i]);
		}
	}

	/**
	 * Get all Disable Objects for this FilterDescription in an Array.
	 * @return Array of Disable Objects
	 */
	public Disable[] getDisables() {
		Disable[] ret = new Disable[Disables.size()];
		Disables.toArray(ret);
		return ret;
	}

	public List getCompleterNames() {
		List names = new ArrayList();

		if (field != null && field.length() > 0 && type != null && type.length() > 0) {
			//add internalName, and any PushOptions names that are found
			if (!names.contains(internalName))
				names.add(internalName);

			for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
				Option element = (Option) iter.next();
				names.addAll(element.getCompleterNames());
			}
		} else {
			for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
				Option element = (Option) iter.next();
				String opfield = element.getField();
				String optype = element.getType();

				if (opfield != null && opfield.length() > 0 && optype != null && optype.length() > 0) {
					if (!names.contains(element.getInternalName()))
						names.add(element.getInternalName());
				} else {
					//try pushOptions
					names.addAll(element.getCompleterNames());
				}
			}
		}
		return names;
	}

	public List getCompleterQualifiers(String internalName) {
		List quals = new ArrayList();

		if (this.internalName.equals(internalName)) {
			//filterDescription has qualifiers
			if (this.qualifiers != null && this.qualifiers.length() > 0)
				quals.addAll(Arrays.asList(qualifiers.split(",")));
		} else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
			//PushOption Filter Option has qualifiers 
			String[] iname_info = internalName.split("\\.");
			String supername = iname_info[0];
			String refname = iname_info[1];
      
			Option superOption = getOptionByName(supername);
			PushOptions[] pos = superOption.getPushOptions();
      
			for (int i = 0, n = pos.length; i < n; i++) {
				PushOptions po = pos[i];

				if (po.containsOption(refname)) {
					Option[] os = po.getOptionByInternalName(refname).getOptions();

					for (int j = 0, l = os.length; j < l; j++) {
						Option option = os[j];
						String opquals = option.getQualifiers();

						if (opquals != null && opquals.length() > 0) {
							List theseQs = Arrays.asList(opquals.split(","));
							for (int k = 0, m = theseQs.size(); k < m; j++) {
								String qual = (String) theseQs.get(j);
								if (!quals.contains(qual))
									quals.add(qual);
							}
						}
					}
				}
			}
		} else {
			//subOption has qualifiers
			if (containsOption(internalName)) {
				Option option = getOptionByName(internalName);
				String opquals = option.getQualifiers();

				if (opquals != null && opquals.length() > 0) {
					quals.addAll( Arrays.asList( opquals.split(",") ) );
				}
			}
		}

		return quals;
	}

	public List getCompleterValues(String internalName) {
		List vals = new ArrayList();

		if (this.internalName.equals(internalName)) {
			Option[] myops = getOptions();

			for (int i = 0, n = myops.length; i < n; i++) {
				Option option = myops[i];
				String opvalue = option.getValue();

				if (opvalue != null && opvalue.length() > 0) {
					if (!vals.contains(opvalue))
						vals.add(opvalue);
				}
			}
		} else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
			//PushOption Option either Filter Option with Value Options, or Value Options
			String[] iname_info = internalName.split("\\.");
			String supername = iname_info[0];
			String refname = iname_info[1];

			Option superOption = getOptionByName(supername);
			PushOptions[] pos = superOption.getPushOptions();

			for (int i = 0, n = pos.length; i < n; i++) {
				PushOptions po = pos[i];
				if (po.getRef().equals(refname)) {
					//value options
					Option[] suboptions = po.getOptions();
					for (int j = 0, m = suboptions.length; j < m; j++) {
						Option option = suboptions[j];
						String opvalue = option.getValue();

						if (opvalue != null && opvalue.length() > 0) {
							if (!vals.contains(opvalue))
								vals.add(opvalue);
						}
					}
				} else {
					//Option Filter with Value Options
					if (po.containsOption(refname)) {
						Option[] os = po.getOptionByInternalName(refname).getOptions();

						for (int j = 0, l = os.length; j < l; j++) {
							Option option = os[j];
							String opvalue = option.getValue();

							if (opvalue != null && opvalue.length() > 0) {
								if (!vals.contains(opvalue))
									vals.add(opvalue);
							}
						}
					}
				}
			}
		} else {
			if (containsOption(internalName)) {
				Option[] ops = getOptionByName(internalName).getOptions();

				for (int i = 0, n = ops.length; i < n; i++) {
					Option option = ops[i];
					String opvalue = option.getValue();

					if (opvalue != null && opvalue.length() > 0) {
						if (!vals.contains(opvalue))
							vals.add(opvalue);
					}
				}
			}
		}

		return vals;
	}

	private Hashtable uiOptionNameMap = new Hashtable();
	private TreeMap uiOptions = new TreeMap();
	private boolean hasOptions = false;
	private int oRank = 0;
	private List Enables = new ArrayList();
	private List Disables = new ArrayList();

	private String handler;
	private String field;
	private String type;
	private String qualifiers;
	private String tableConstraint;
	private int hshcode = -1;

	//cache one supporting Option for call to supports
	Option lastSupportingOption = null;
}
