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

/**
 * Contains all of the information necessary for the UI to display the information for a specific filter,
 * and add this filter as a Filter to a Query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FilterDescription extends QueryFilterSettings {

	private Hashtable uiOptionNameMap = new Hashtable();
	private List uiOptions = new ArrayList();
	private boolean hasOptions = false;

	private List Enables = new ArrayList();
	private List Disables = new ArrayList();

	private String handler;
	private String field;
	private String type;
	private String legalQualifiers;
	private String tableConstraint;
	private String value;

	//cache one supporting Option for call to supports
	Option lastSupportingOption = null;

	private String qualifier;

	/**
	 * Empty Constructor should only be used by DatasetViewEditor
	 *
	 */
	public FilterDescription() {
		super();
	}

	/**
	 * Constructor for a FilterDescription named by internalName internally, with a field, type, and legalQualifiers.
	 * 
	 * @param internalName String internal name of the FilterDescription. Must not be null or empty.
	 * @param field String name of the field to reference in the mart.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param legalQualifiers String, comma-separated list of legalQualifiers to use in a MartShell MQL
	 * @throws ConfigurationException when required values are null or empty, or when a filterSetName is set, but no filterSetReq is submitted.
	 */
	public FilterDescription(String internalName, String field, String type, String legalQualifiers) throws ConfigurationException {
		this(internalName, field, type, "", legalQualifiers, "", "", null, "");
	}

	/**
	 * Constructor for a fully defined FilterDescription
	 * 
	 * @param internalName String internal name of the FilterDescription. Must not be null or empty.
	 * @param field String name of the field to reference in the mart.
	 * @param type String type of filter.  Must not be null or empty.
	 * @param qualifier String qualifier to apply to a filter for this Filter.
	 * @param legal_qualifiers String, comma-separated list of legalQualifiers to use in a MartShell MQL
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
		String qualifier,
		String legalQualifiers,
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
		this.qualifier = qualifier;
		this.legalQualifiers = legalQualifiers;
		this.handler = handler;
		this.tableConstraint = tableConstraint;
	}

	/**
	 * Returns the description, given an internalName which may, in some cases, map to an Option instead of this FilterDescription.
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return String description
	 */
	public String getDescription(String internalName) {
		if (this.internalName.equals(internalName))
			return description;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptionNameMap.get(internalName)).getDescription();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return description;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptionNameMap.get(optionIname)).getDescription(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Returns the displayName, given an internalName which may, in some cases, map to an Option instead of this FilterDescription.
	 * @param internalName -- internalName of either this FilterDescription, or an Option contained within this FilterDescription
	 * @return String displayName
	 */
	public String getDisplayname(String internalName) {
		if (this.internalName.equals(internalName))
			return displayName;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptionNameMap.get(internalName)).getDisplayName();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return displayName;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptionNameMap.get(optionIname)).getDisplayName(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Set the field for this FilterDescription
	 * @param field -- String field
	 */
	public void setField(String field) {
		this.field = field;
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
				return ((Option) uiOptionNameMap.get(internalName)).getField();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return field;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptionNameMap.get(optionIname)).getField(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Set the type for this FilterDescription
	 * @param type -- String type of FilterDescription
	 */
	public void setType(String type) {
		this.type = type;
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
				return ((Option) uiOptionNameMap.get(internalName)).getType();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return type;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptionNameMap.get(optionIname)).getType(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Sets the tableConstraint for this FilterDescription
	 * @param tableConstraint -- String tableConstraint
	 */
	public void setTableConstraint(String tableConstraint) {
		this.tableConstraint = tableConstraint;
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
				return ((Option) uiOptionNameMap.get(internalName)).getTableConstraint();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return tableConstraint;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptionNameMap.get(optionIname)).getTableConstraint(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Sets the handler for this FilterDescription
	 * @param handler -- String handler.  Should reference a Java package that can be used by the Java Classloader to load an instance of a Class
	 */
	public void setHandler(String handler) {
		this.handler = handler;
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
				return ((Option) uiOptionNameMap.get(internalName)).getHandler();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (this.internalName.equals(refIname))
					return handler;
				else if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptionNameMap.get(optionIname)).getHandler(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Sets the legalQualifiers for this FilterDescription.
	 * @param legalQualifiers -- String legalQualifers, Comma separated list
	 */
	public void setLegalQualifiers(String legalQualifiers) {
		this.legalQualifiers = legalQualifiers;
	}

	/**
	 * Returns the legalQualifiers to use in MartShell.
	 * 
	 * @return String legalQualifiers
	 */
	public String getLegalQualifiers() {
		return legalQualifiers;
	}

	/**
	 * Gets the legalQualifiers for the FilterDescription/Option named by internalName, which may be this particular FilterDescription,
	 * or a child Option (possibly occuring within in a PushAction).
	 * @param internalName -- String internalName of FilterDescription/Option for which legalQualifiers is desired.
	 * @return String legalQualifiers.
	 */
	public String getLegalQualifiers(String internalName) {
		if (this.internalName.equals(internalName))
			return legalQualifiers;
		else {
			if (uiOptionNameMap.containsKey(internalName))
				return ((Option) uiOptionNameMap.get(internalName)).getLegalQualifiers();
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				// pushOption option
				String[] names = internalName.split("\\.");
				String optionIname = names[0];
				String refIname = names[1];

				if (uiOptionNameMap.containsKey(optionIname))
					return ((Option) uiOptionNameMap.get(optionIname)).getLegalQualifiers(refIname);
				else
					return null; // nothing found
			} else
				return null; // nothing found
		}
	}

	/**
	 * Returns the internalName of the FilterDescription/Option that supports a given field and tableConstraint, which
	 * could be this particular FilterDescription, or a child Option (possibly occuring within a PushAction).
	 * @param field --  String field
	 * @param tableConstraint -- String table
	 * @return String internalName of supporting FilterDescription/Option
	 */
	public String getInternalNameByFieldNameTableConstraint(String field, String tableConstraint) {
		String ret = null;

		if (supports(field, tableConstraint)) {
			if (this.field != null && this.field.equals(field) && this.tableConstraint != null && this.tableConstraint.equals(tableConstraint))
				ret = internalName;
			else
				ret = lastSupportingOption.getInternalNameByFieldNameTableConstraint(field, tableConstraint);
		}

		return ret;
	}

	/**
	 * Determine if this FilterDescription, or a child Option (possibly occuring within a PushAction)
	 * supports the given field and tableConstraint.  If the supporting Object is a child Option, this Option is cached for a subsequent call
   * to getInternalNameByFieldNameTableConstraint(String, String).
	 * @param field -- String field
	 * @param tableConstraint -- String tableConstraint
	 * @return boolean, true if this FilterDescription or a child Option supports the given FilterDescription, false otherwise.
	 */
	public boolean supports(String field, String tableConstraint) {
		boolean supports = (this.field != null && this.field.equals(field) && this.tableConstraint != null && this.tableConstraint.equals(tableConstraint));

		if (!supports) {
			if (lastSupportingOption == null) {
				for (Iterator iter = uiOptions.iterator(); iter.hasNext();) {
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
		buf.append(", legalQualifiers=").append(legalQualifiers);
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

			int hshcode = super.hashCode();
      
			if (field != null)
				hshcode = (31 * hshcode) + field.hashCode();
			if (type != null)
				hshcode = (31 * hshcode) + type.hashCode();
			if (qualifier != null)
				hshcode = (31 * hshcode) + qualifier.hashCode();
			if (legalQualifiers != null)
				hshcode = (31 * hshcode) + legalQualifiers.hashCode();
			if (tableConstraint != null)
				hshcode = (31 * hshcode) + tableConstraint.hashCode();
			if (description != null)
				hshcode = (31 * hshcode) + description.hashCode();

			for (Iterator iter = uiOptions.iterator(); iter.hasNext();) {
				Option option = (Option) iter.next();
				hshcode = (31 * hshcode) + option.hashCode();
			}

		return hshcode;
	}

	/**
	 * add an Option object to this FilterDescription.  Options are stored in the order that they are added.
	 * @param o - an Option object
	 */
	public void addOption(Option o) {
		uiOptions.add(o);
		uiOptionNameMap.put(o.getInternalName(), o);
		hasOptions = true;
	}

  /**
   * Remove an Option from the FilterDescription.
   * @param o -- Option to be removed
   */
  public void removeOption(Option o) {
    uiOptionNameMap.remove(o.getInternalName());
    uiOptions.remove(o);
    if (uiOptions.size() < 1)
     hasOptions = false;
  }
  
  /**
   * Insert an Option at a specific position within the list of Options for this FilterDescription.
   * Options occuring at or after the given position are shifted right.
   * @param position -- position to insert the given Option
   * @param o -- Option to be inserted.
   */
  public void insertOption(int position, Option o) {
    uiOptions.add(position, o);
    uiOptionNameMap.put(o.getInternalName(), o);
    hasOptions = true;
  }
  
  /**
   * Insert an Option before a specified Option, named by internalName.
   * @param internalName -- String internalName of the Option before which the given Option should be inserted.
   * @param o -- Option to insert.
   * @throws ConfigurationException when the FilterDescription does not contain an Option named by internalName.
   */
  public void insertOptionBeforeOption(String internalName, Option o) throws ConfigurationException {
    if (!uiOptionNameMap.containsKey(internalName))
      throw new ConfigurationException("FilterDescription does not contain an Option " + internalName + "\n");
    insertOption( uiOptions.indexOf( uiOptionNameMap.get(internalName) ), o );
  }
  
  /**
   * Insert an Option after a specified Option, named by internalName.
   * @param internalName -- String internalName of the Option after which the given Option should be inserted.
   * @param o -- Option to insert.
   * @throws ConfigurationException when the FilterDescription does not contain an Option named by internalName.
   */
  public void insertOptionAfterOption(String internalName, Option o) throws ConfigurationException {
    if (!uiOptionNameMap.containsKey(internalName))
      throw new ConfigurationException("FilterDescription does not contain an Option " + internalName + "\n");
    insertOption( uiOptions.indexOf( uiOptionNameMap.get(internalName) ) + 1, o );
  }
  
  /**
   * Add a group of Option objects in one call.  Subsequent calls to
   * addOption or addOptions will add to what was added before, in the order that they are added.
   * @param o - an array of Option objects
   */
  public void addOptions(Option[] o) {
    for (int i = 0, n = o.length; i < n; i++) {
      uiOptions.add(o[i]);
      uiOptionNameMap.put(o[i].getInternalName(), o[i]);
    }
    hasOptions = true;
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
	public Option getOptionByInternalName(String internalName) {
		if (uiOptionNameMap.containsKey(internalName))
			return (Option) uiOptionNameMap.get(internalName);
		else
			return null;
	}

	/**
	 * Get all Option objects available as an array.  Options are returned in the order they were added.
	 * @return Option[]
	 */
	public Option[] getOptions() {
		Option[] ret = new Option[uiOptions.size()];
		uiOptions.toArray(ret);
		return ret;
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
   * Remove an Enable object from this FilterDescription.
   * @param e -- Enable Object to remove.
   */
  public void removeEnable(Enable e) {
    Enables.remove(e);
  }
  
	/**
	 * Add an array of Enable objects in one call, adds to what was added via previous calls to addEnable/setEnables methods.
	 * @param e - Array of Enable Objects
	 */
	public void addEnables(Enable[] e) {
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
   * Remove a Disable Object from the FilterDescription.
   * @param e -- Disable Object to remove.
   */
  public void removeDisable(Disable e) {
    Disables.remove(e);
  }
  
	/**
	 * Add an array of Disable objects in one call, adds to what was added via previous calls to addDisable/setDisables methods.
	 * @param e - Array of Disable Objects
	 */
	public void addDisables(Disable[] e) {
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

  /**
   * Returns a List of String names that MartShell can display in its command completion system.
   * @return List of Strings
   */
	public List getCompleterNames() {
		List names = new ArrayList();

		if (field != null && field.length() > 0 && type != null && type.length() > 0) {
			//add internalName, and any PushOption names that are found
			if (!names.contains(internalName))
				names.add(internalName);

			for (Iterator iter = uiOptions.iterator(); iter.hasNext();) {
				Option element = (Option) iter.next();
				names.addAll(element.getCompleterNames());
			}
		} else {
			for (Iterator iter = uiOptions.iterator(); iter.hasNext();) {
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

  /**
   * Returns a List of String qualifiers that MartShell can display in its command completion system, for a given
   * internalName, which refers to this FilterDescription, or one of its child Options.
   * @param internalName -- name of the FilterDescription/child Option for which the qualifiers are desired.
   * @return List Strings
   */
	public List getCompleterQualifiers(String internalName) {
		List quals = new ArrayList();

		if (this.internalName.equals(internalName)) {
			//filterDescription has legalQualifiers
			if (this.legalQualifiers != null && this.legalQualifiers.length() > 0)
				quals.addAll(Arrays.asList(legalQualifiers.split(",")));
		} else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
			//PushOption Filter Option has legalQualifiers 
			String[] iname_info = internalName.split("\\.");
			String supername = iname_info[0];
			String refname = iname_info[1];

			Option superOption = getOptionByInternalName(supername);
			PushAction[] pos = superOption.getPushActions();

			for (int i = 0, n = pos.length; i < n; i++) {
				PushAction po = pos[i];

				if (po.containsOption(refname)) {
					Option[] os = po.getOptionByInternalName(refname).getOptions();

					for (int j = 0, l = os.length; j < l; j++) {
						Option option = os[j];
						String opquals = option.getLegalQualifiers();

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
			//subOption has legalQualifiers
			if (containsOption(internalName)) {
				Option option = getOptionByInternalName(internalName);
				String opquals = option.getLegalQualifiers();

				if (opquals != null && opquals.length() > 0) {
					quals.addAll(Arrays.asList(opquals.split(",")));
				}
			}
		}

		return quals;
	}

  /**
   * Returns a List of String completer values for a given internalName referring to either this FilterDescription,
   * or one of its child Options.
   * @param internalName -- String name for FilterDescription/Option for which values are desired.
   * @return List Strings
   */
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

			Option superOption = getOptionByInternalName(supername);
			PushAction[] pos = superOption.getPushActions();

			for (int i = 0, n = pos.length; i < n; i++) {
				PushAction po = pos[i];
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
				Option[] ops = getOptionByInternalName(internalName).getOptions();

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

  /**
   * Set the value for this FilterDescription.
   * @param value -- String value.
   */
  public void setValue(String value) {
    this.value = value;
  }
  /**
   * Get the value for this FilterDscription
   * @return String value
   */
	public String getValue() {
		return value;
	}

	/**
	 * Recurses through options beneath all PushAction Objects to set option.parent.
	 * 
	 * <br>
	 * For optionPush->option1 option1.parent = optionPush.ref
	 * </br>
	 * 
	 * <br>
	 * For optionPush->option1->option1_1 : option1_1.parent = option1
	 * </bre>
	 * 
	 * @param d
	 */
	public void setParentsForAllPushOptionOptions(DatasetView d) throws ConfigurationException {

		setParentsForAllPushOptionOptions(d, getOptions());

	}

	/**
	 * Recurses through options looking for Options inside PushActions. Set the parent
	 * for these Options to the FieldDesscription specified by PushOption.ref.
	 * @param d
	 * @param options
	 */
	private void setParentsForAllPushOptionOptions(DatasetView dataset, Option[] options) throws ConfigurationException {

		for (int i = 0, n = options.length; i < n; i++) {

			Option option = options[i];
			PushAction[] pushOptions = option.getPushActions();

			for (int j = 0; j < pushOptions.length; j++) {

				PushAction pushOption = pushOptions[j];

				String targetName = pushOption.getRef();
				QueryFilterSettings parent = dataset.getFilterDescriptionByInternalName(targetName);

				if (parent == null)
					throw new ConfigurationException(
						"OptionPush.ref = " + targetName + " Refers to a FilterDescription that is not in dataset " + dataset.getInternalName());

				// Assign the target FilterDescription to each option.
				Option[] options2 = pushOption.getOptions();
				for (int k = 0; k < options2.length; k++) {
					Option option2 = options2[k];
					option2.setParent(parent);

					// Recurse incase this option has any PushOptions
					setParentsForAllPushOptionOptions(dataset, option2.getOptions());
				}
			}
		}
	}

	/**
	 * Same as getField(). Included to make FilterDescription implement QueryFilterSettings
	 * interface.
	 * @return field
	 */
	public String getFieldFromContext() {
		return field;
	}

	/**
	 * Same as getValue(). Included to make FilterDescription implement QueryFilterSettings
	 * interface.
	 * @return value
	 */
	public String getValueFromContext() {
		return value;

	}

	/**
	 * Same as getType(). Included to make FilterDescription implement QueryFilterSettings
	 * interface.
	 * @return type
	 */
	public String getTypeFromContext() {
		return type;
	}

	/**
	 * Same as getHandler(). Included to make FilterDescription implement QueryFilterSettings
	 * interface.
	 * @return handler
	 */
	public String getHandlerFromContext() {
		return handler;
	}

  /**
   * Sets the Qualifier for this FilterDescription.
   * @param qualifier -- String qualifier
   */
  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }
  
	/**
	 * Returns the qualifier
	 * @return String qualifier
	 */
	public String getQualifier() {
		return qualifier;
	}

	/**
	 * Same as getQualifier.  Included to make FilterDescription impliment QueryFilterSettings
	 * interface.
	 * @return qualifier
	 */
	public String getQualifierFromContext() {
		return qualifier;
	}

  /**
   * Same as getlegalQualifiers.  Included to make FilterDescription impliment QueryFilterSettings
   * interface.
   * @return legalQualifiers
   */
  public String getLegalQualifiersFromContext() {
    return legalQualifiers;
  }
  
	/**
	 * Same as getTableConstraint(). Included to make FilterDescription implement QueryFilterSettings
	 * interface.
	 * @return tableConstraint
	 */
	public String getTableConstraintFromContext() {
		return tableConstraint;
	}
}
