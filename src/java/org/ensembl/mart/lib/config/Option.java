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
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Option extends QueryFilterSettings {

  private String qualifier;
	private QueryFilterSettings parent;
	private String field;
	private String tableConstraint;
	private String value;
	private String ref;
	private String legalQualifiers;
	private String type;
	private String handler;

	private boolean isSelectable;
	private boolean hasOptions = false;

	//options can contain options
	private List uiOptions = new ArrayList();
	private Hashtable uiOptionNameMap = new Hashtable();
	private List uiOptionPushes = new ArrayList();

	// cache one Option per call to supports/getOptionByFieldNameTableConstraint
	private Option lastSupportingOption = null;

  /**
   * Copy Constructor.  Creates a copy of an existing Option.
   * @param o - Option to copy
   */
  public Option(Option o) {
    super(o);
    
  	isSelectable = o.isSelectable();
  	field = o.getField();
  	tableConstraint = o.getTableConstraint();
  	value = o.getValue();
  	ref = o.getRef();
  	type = o.getType();
  	qualifier = o.getQualifier();
  	legalQualifiers = o.getLegalQualifiers();
  	handler = o.getHandler();
  	
  	Option[] os = o.getOptions();
  	for (int i = 0, n = os.length; i < n; i++) {
      addOption( new Option( os[i] ) );
    }
    
    PushAction[] pas = o.getPushActions();
    for (int i = 0, n = pas.length; i < n; i++) {
      addPushAction( new PushAction(pas[i] ) );
    }
  }

  /**
   * Special Copy constructor allowing a FilterDescription to
   * be converted to an Option based upon their common fields.
   * This is a destructive operation, in that not all fields and
   * child objects of the Option are supported by the FilterDescription,
   * and vice versa.  In particular, the isSelectable field is set to
   * true. For these reasons, this method is reserved for use by
   * the DatasetViewEditor application to facilitate the conversion
   * between these two objects, with subsequent editing by the user.
   * @param fd - FilterDescription to be converted to an Option
   */
  public Option(FilterDescription fd) {
  	super(fd);
  	
		isSelectable = true;
		field = fd.getField();
		tableConstraint = fd.getTableConstraint();
		value = fd.getValue();
		type = fd.getType();
		qualifier = fd.getQualifier();
		legalQualifiers = fd.getLegalQualifiers();
		handler = fd.getHandler();
  	
		Option[] os = fd.getOptions();
		for (int i = 0, n = os.length; i < n; i++) {
			addOption( new Option( os[i] ) );
		}
  	  
  }
  
	/**
	 * Empty Constructor should only be used by DatasetViewEditor.
	 *
	 */
	public Option() {
		super();
	}

	public Option(String internalName, boolean isSelectable) throws ConfigurationException {
		this(internalName, isSelectable, "", "", "", "", "", "", "", "", "", null);
	}

	public Option(
		String internalName,
		boolean isSelectable,
		String displayName,
		String description,
		String field,
		String tableConstraint,
		String value,
		String ref,
		String type,
		String qualifier,
		String legalQualifiers,
		String handler)
		throws ConfigurationException {

		super(internalName, displayName, description);

		this.isSelectable = isSelectable;
		this.field = field;
		this.tableConstraint = tableConstraint;
		this.qualifier = qualifier;
		this.legalQualifiers = legalQualifiers;
		this.type = type;

		this.value = value;
		this.ref = ref;
		this.handler = handler;
	}

	/**
	 * add an Option object to this Option.  Options are stored in the order that they are added.
	 * @param o - an Option object
	 */
	public void addOption(Option o) {
		uiOptions.add(o);
		uiOptionNameMap.put(o.getInternalName(), o);
		hasOptions = true;
	}

	/**
	 * Remove an Option from this Option.
	 * @param o -- Option to be removed.
	 */
	public void removeOption(Option o) {
		uiOptionNameMap.remove(o.getInternalName());
		uiOptions.remove(o);

		if (uiOptions.size() < 1)
			hasOptions = false;
	}

	/**
	 * Insert an Option at a specific position within the Option list for this Option.
	 * Options occuring at or after this position are shifted right.
	 * @param position -- position to insert the given Option
	 * @param o -- Option to insert.
	 */
	public void insertOption(int position, Option o) {
		uiOptions.add(position, o);
		uiOptionNameMap.put(o.getInternalName(), o);
		hasOptions = true;
	}

	/**
	 * Insert an Option before a specific Option in the list, named by internalName.
	 * @param internalName -- internalName of Option before which the given Option is to be inserted.
	 * @param o -- Option to be inserted
	 * @throws ConfigurationExction when the Option does not contain an Option named by internalName
	 */
	public void insertOptionBeforeOption(String internalName, Option o) throws ConfigurationException {
		if (!uiOptionNameMap.containsKey(internalName))
			throw new ConfigurationException("Option does not contain an Option " + internalName + "\n");
		insertOption(uiOptions.indexOf(uiOptionNameMap.get(internalName)), o);
	}

	/**
	 * Insert an Option after a specific Option in the list, named by internalName.
	 * @param internalName -- internalName of Option after which the given Option is to be inserted.
	 * @param o -- Option to be inserted
	 * @throws ConfigurationExction when the Option does not contain an Option named by internalName
	 */
	public void insertOptionAfterOption(String internalName, Option o) throws ConfigurationException {
		if (!uiOptionNameMap.containsKey(internalName))
			throw new ConfigurationException("Option does not contain an Option " + internalName + "\n");
		insertOption(uiOptions.indexOf(uiOptionNameMap.get(internalName)) + 1, o);
	}

	/**
	 * Add a group of Option objects in one call.  Subsequent calls to
	 * addOption or setOptions will add to what was added before, in the order that they are added.
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
	 * Get all Option objects available as an array.  Options are returned in the order they were added.
	 * @return Option[]
	 */
	public Option[] getOptions() {
		Option[] ret = new Option[uiOptions.size()];
		uiOptions.toArray(ret);
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
	public Option getOptionByInternalName(String internalName) {
		if (uiOptionNameMap.containsKey(internalName))
			return (Option) uiOptionNameMap.get(internalName);
		else
			return null;
	}

  /**
   * Set the selectability of this Option (true or false)
   * @param isSelectable -- boolean, true if this Option is selectable, false otherwise
   */
  public void setSelectable(boolean isSelectable) {
    this.isSelectable = isSelectable;
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

	public String getDisplayName(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptionNameMap.get(internalName)).getDisplayName(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getDisplayName();
				}
				return null; // nothing found
			}
		}
	}

	public String getDescription(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptionNameMap.get(internalName)).getDescription(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getDescription();
				}
				return null; // nothing found
			}
		}
	}

  /**
   * Set the field for this Option
   * @param field -- String field of the Option.
   */
  public void setField(String field) {
    this.field = field;
  }
  
	/**
	 * Returns the field for this Option
	 * @return String field
	 */
	public String getField() {
		return field;
	}

	/**
	 * Returns the field for this Option, or the field for a child Option (possibly within a PushAction) 
	 * of this Option, named by refIname
	 * @param refIname -- name of Option for which Field is desired.
	 * @return String field
	 */
	public String getField(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptionNameMap.get(internalName)).getField(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getField();
				}
				return null; // nothing found
			}
		}
	}

	/**
	 * Set the tableConstraint.
	 * @param tableConstraint -- String table name pattern.
	 */
	public void setTableConstraint(String tableConstraint) {
		this.tableConstraint = tableConstraint;
	}

	/**
	 * Returns the tableConstraint for this Option
	 * @return String tableConstraint
	 */
	public String getTableConstraint() {
		return tableConstraint;
	}

	/**
	 * Returns the tableConstraint for this Option, or a child Option (possibly within a PushAction),
	 * named by refIname.
	 * @param refIname -- name of Option for which tableConstraint is desired.
	 * @return String tableConstraint
	 */
	public String getTableConstraint(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptionNameMap.get(internalName)).getTableConstraint(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getTableConstraint();
				}
				return null; // nothing found
			}
		}
	}

	/**
	 * Set the handler
	 * @param handler -- String handler.  Should be a Java package name, that can be fed to the ClassLoader.
	 */
	public void setHandler(String handler) {
		this.handler = handler;
	}

	/**
	 * Returns the handler for this Option
	 * @return String handler
	 */
	public String getHandler() {
		return handler;
	}

	/**
	 * Get the Handler for this Option, or a child Option (possibly within a PushAction),
	 * named by refIname.
	 * @param refIname -- String name Option for which Handler is desired.
	 * @return String handler
	 */
	public String getHandler(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptionNameMap.get(internalName)).getHandler(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getHandler();
				}
				return null; // nothing found
			}
		}
	}

	/**
	 * Searches each PushAction for potential completer names.  If it contains Options acting as Filters 
	 * (eg, to be pushed to some other FilterDescription), adds 'internalName.option.getInternalName()' to the list of potential completer names.
	 * If it references another FilterDescription, and contains value Options, adds 'internalName.pushOptions.getRef()' to the list.
	 * @return List of potential completer names
	 */
	public List getCompleterNames() {
		List names = new ArrayList();
		for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
			PushAction element = (PushAction) uiOptionPushes.get(i);
			Option[] ops = element.getOptions();

			for (int j = 0, o = ops.length; j < o; j++) {
				Option option = ops[j];
				String completer = null;

				if (option.getField() != null && option.getField().length() > 0 && option.getType() != null && option.getType().length() > 0) {
					//push option filter, should get superoption.subotion as name
					completer = internalName + "." + option.getInternalName();
				} else if (option.getValue() != null && option.getValue().length() > 0) {
					//push option value, should get superoption.pushoptionref as name
					completer = internalName + "." + element.getRef();
				} // else not needed

				if (!(completer == null || names.contains(completer)))
					names.add(completer);
			}
		}
		return names;
	}

	/**
	 * Sets the ref for this Option.
	 * @param ref -- String ref, which refers to another FilterDescription or Option internalName.
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * Get the Ref for this Option.
	 * @return String ref.
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * Set the value for this Option.
	 * @param value -- Value for the Option.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Returns the Value for this Option
	 * @return String value for the Option
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Get all PushOption objects available as an array.  OptionPushes are returned in the order they were added.
	 * @return PushOption[]
	 */
	public PushAction[] getPushActions() {
		return (PushAction[]) uiOptionPushes.toArray(new PushAction[uiOptionPushes.size()]);
	}

	/**
	 * Add a PushAction to this Option.
	 * @param PushAction object to be added.
	 */
	public void addPushAction(PushAction optionPush) {
		uiOptionPushes.add(optionPush);
	}

  /**
   * Add a group of PushAction objects in one call.
   * @param pushactions  Array of PushActions
   */
  public void addPushActions(PushAction[] pushactions) {
    uiOptionPushes.addAll(Arrays.asList(pushactions));
  }
  
	/**
	 * Remove a PushAction from this Option.
	 * @param pa -- PushAction to be removed.
	 */
	public void removePushAction(PushAction pa) {
		uiOptionPushes.remove(pa);
	}

	/**
	 * Set the legalQualifiers for this Option.
	 * @param legalQualifiers -- String comma separated list of legal Qualifiers for this Option.
	 */
	public void setLegalQualifiers(String legalQualifiers) {
		this.legalQualifiers = legalQualifiers;
	}

	/**
	 * Get the legal Qualifiers for this Option.
	 * @return String legalQualifiers
	 */
	public String getLegalQualifiers() {
		return legalQualifiers;
	}

	/**
	 * Get the legalQualifiers for this Option, or a child Option (possibly in a PushAction) named by refIname.
	 * @param refIname -- internalName of Option for which legalQualifiers is desired.
	 * @return String legalQualifiers.
	 */
	public String getLegalQualifiers(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptionNameMap.get(internalName)).getLegalQualifiers(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getLegalQualifiers();
				}
				return null; // nothing found
			}
		}
	}

	/**
	 * Set the type for this Option.
	 * @param type -- String type of Option.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the type of this Option
	 * @return String type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get the type for this Option, or a child Option (possibly within a PushAction) named by
	 * refIname.
	 * @param refIname -- String name of Option for which type is desired.
	 * @return String type.
	 */
	public String getType(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptionNameMap.get(internalName)).getType(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getType();
				}
				return null; // nothing found
			}
		}
	}

	/**
	 * Determine if an Option supports a given field and tableConstraint.
	 * 
	 * @param field -- String mart database field
	 * @param tableConstraint -- String mart database table
	 * @return boolean, true if the field and tableConstraint for this Option match the given field and tableConstraint, false otherwise
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

				if (!supports) {
					for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
						PushAction element = (PushAction) uiOptionPushes.get(i);
						if (element.supports(field, tableConstraint)) {
							lastSupportingOption = element.getOptionByFieldNameTableConstraint(field, tableConstraint);
							supports = true;
							break;
						}
					}
				}
			} else {
				if (lastSupportingOption.supports(field, tableConstraint))
					supports = true;
				else {
					lastSupportingOption = null;
					supports = supports(field, tableConstraint);
				}
			}
		}
		return supports;
	}

	/**
	 * Get an Option by its field and tableConstraint.
	 * @param field -- Field for desired Option
	 * @param tableConstraint -- tableConstraint for desired Option.
	 * @return Option supporting this field and tableConstraint (eg, getOptionByFieldNameTableConstraint(f,t).supports(f,t) will always be true).
	 */
	public Option getOptionByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supports(field, tableConstraint))
			return lastSupportingOption;
		else
			return null;
	}

	/**
	 * Get the internalName of an Option by a given field and tableConstraint.
	 * @param field -- field for Option for which internalName is desired
	 * @param tableConstraint -- tableConstraint for Option for which internalName is desired
	 * @return String internalName
	 */
	public String getInternalNameByFieldNameTableConstraint(String field, String tableConstraint) {
		if (this.field != null && this.field.equals(field) && this.tableConstraint != null && this.tableConstraint.equals(tableConstraint))
			return internalName;
		else {
			for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
				PushAction element = (PushAction) uiOptionPushes.get(i);
				if (element.supports(field, tableConstraint)) {
					return internalName + "." + element.geOptionInternalNameByFieldNameTableConstraint(field, tableConstraint);
				}
			}
		}

		return null;
	}

	/**
		* Debug output
		*/
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(super.toString());
		buf.append(", isSelectable=").append(isSelectable);
		buf.append(", field=").append(field);
		buf.append(", tableConstraint=").append(tableConstraint);
		buf.append(", value=").append(value);
		buf.append(", ref=").append(ref);
		buf.append(", qualifier=").append(qualifier);
		buf.append(", legalQualifiers=").append(legalQualifiers);
		buf.append(", type=").append(type);
		buf.append(", handler=");
		if (handler != null)
			buf.append(handler);
		else
			buf.append("null");

		if (hasOptions)
			buf.append(", options=").append(uiOptions);
		buf.append("]");

		return buf.toString();
	}
	/**
		* Allows Equality Comparisons manipulation of Option objects
		*/
	public boolean equals(Object o) {
		return o instanceof Option && hashCode() == o.hashCode();
	}

	/* (non-Javadoc)
		* @see java.lang.Object#hashCode()
		*/
	public int hashCode() {

		int hashcode = super.hashCode();

		hashcode = (isSelectable) ? (31 * hashcode) + 1 : hashcode;
		hashcode = (31 * hashcode) + field.hashCode();
		hashcode = (31 * hashcode) + tableConstraint.hashCode();
		hashcode = (31 * hashcode) + value.hashCode();
		hashcode = (31 * hashcode) + ref.hashCode();
		hashcode = (31 * hashcode) + qualifier.hashCode();
		hashcode = (31 * hashcode) + legalQualifiers.hashCode();
		hashcode = (31 * hashcode) + type.hashCode();
		hashcode = (handler != null) ? (31 * hashcode) + handler.hashCode() : hashcode;

		for (Iterator iter = uiOptions.iterator(); iter.hasNext();) {
			hashcode = (31 * hashcode) + iter.next().hashCode();
		}

		for (Iterator iter = uiOptionPushes.iterator(); iter.hasNext();) {
			hashcode = (31 * hashcode) + iter.next().hashCode();
		}

		return hashcode;
	}

	public void setParent(QueryFilterSettings parent) {
		this.parent = parent;
	}

	public QueryFilterSettings getParent() {
		return parent;
	}

	/**
	 * Returns field based on context. 
	 * @return field if set otherwise getParent().getFieldFromContext().
	 */
	public String getFieldFromContext() {
		if (valid(field))
			return field;
		else
			return getParent().getFieldFromContext();
	}

	/**
	 * Returns value based on context. 
	 * @return value if set otherwise getParent().getValueFromContext().
	 */
	public String getValueFromContext() {
		if (valid(value))
			return value;
		else
			return getParent().getValueFromContext();
	}

	/**
	 * Returns type based on context. 
	 * @return type if set otherwise getParent().getTypeFromContext().
	 */
	public String getTypeFromContext() {
		if (valid(type))
			return type;
		else
			return getParent().getTypeFromContext();
	}
  
  /**
   * Returns the legalQualifiers based on context.
   * @return legalQualifiers if set, otherwise getParent().getLegalQualifiersFromContext().
   */
  public String getLegalQualifiersFromContext() {
    if (valid(legalQualifiers))
      return legalQualifiers;
    else
      return getParent().getLegalQualifiersFromContext();
  }

	/**
	 * Returns handler based on context. 
	 * @return handler if set otherwise getParent().getHandlerFromContext().
	 */
	public String getHandlerFromContext() {
		if (valid(handler))
			return handler;
		else
			return getParent().getHandlerFromContext();
	}

	/**
	 * Returns tableContraint based on context. 
	 * @return tableConstraint if set otherwise getParent().getTableConstraintFromContext().
	 */
	public String getTableConstraintFromContext() {
		if (valid(tableConstraint))
			return tableConstraint;
		else
			return getParent().getTableConstraintFromContext();
	}

  /**
   * Set the qualifier for this Option
   * @param qualifier -- Qualifier to use in BasicFilter objects for this Option.
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
	 * Returns the qualifier based on context.
	 * @return qualifier if set otherwise getParent().getQualifierFromContext().
	 */
	public String getQualifierFromContext() {
		if (valid(qualifier))
			return qualifier;
		else
			return getParent().getQualifierFromContext();
	}
}
