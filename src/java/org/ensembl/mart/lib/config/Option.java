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
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Option extends QueryFilterSettings {

  private QueryFilterSettings parent;
	private String field;
	private String tableConstraint;
	private String value;
	private String ref;
	private String qualifiers;
	private String type;
	private String handler;
	private int hashcode = -1;

	private final boolean isSelectable;
	private boolean hasOptions = false;

	//options can contain options
	private int oRank = 0;
	private TreeMap uiOptions = new TreeMap();
	private Hashtable uiOptionNameMap = new Hashtable();
	private List uiOptionPushes = new ArrayList();
	private Option lastSupportingOption = null;
	// cache one Option per call to supports/getOptionByFieldNameTableConstraint

	public Option(String internalName, boolean isSelectable) throws ConfigurationException {
		this(internalName, isSelectable, "", "", "", "", "", "", "", "", null);
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
		String qualifiers,
		String handler)
		throws ConfigurationException {

		super(internalName, displayName, description);

		this.isSelectable = isSelectable;
		this.field = field;
		this.tableConstraint = tableConstraint;
		this.qualifiers = qualifiers;
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
	public Option getOptionByInternalName(String internalName) {
		if (uiOptionNameMap.containsKey(internalName))
			return (Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName));
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

  public String getDisplayName(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getDisplayName(refIname);
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
			return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getDescription(refIname);
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
	  * @return
	  */
	public String getField() {
		return field;
	}

	public String getField(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getField(refIname);
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
	 * @return
	 */
	public String getTableConstraint() {
		return tableConstraint;
	}

	public String getTableConstraint(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getTableConstraint(refIname);
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
	 * @return
	 */
	public String getHandler() {
		return handler;
	}

	public String getHandler(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getHandler(refIname);
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
   * Searches each PushOption for potential completer names.  If it contains Options acting as Filters 
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
					completer = internalName+"."+option.getInternalName();
				} else if (option.getValue() != null && option.getValue().length() > 0 ) {
					//push option value, should get superoption.pushoptionref as name
					completer = internalName+"."+element.getRef();
				} // else not needed
				
				if (! ( completer == null || names.contains(completer) ) )
				  names.add(completer);
			}
		}
		return names;
  }
  
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

	/**
	 * Get all PushOption objects available as an array.  OptionPushes are returned in the order they were added.
	 * @return PushOption[]
	 */
	public PushAction[] getPushOptions() {
		return (PushAction[]) uiOptionPushes.toArray(new PushAction[uiOptionPushes.size()]);
	}

	/**
	 * @param object
	 */
	public void addPushOption(PushAction optionPush) {
		uiOptionPushes.add(optionPush);
		hashcode = -1;
	}

	/**
	 * @return
	 */
	public String getQualifiers() {
		return qualifiers;
	}

	public String getQualifiers(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getQualifiers(refIname);
		else {
			if (uiOptionPushes.size() < 1)
				return null;
			else {
				for (int i = 0, n = uiOptionPushes.size(); i < n; i++) {
					PushAction element = (PushAction) uiOptionPushes.get(i);
					if (element.containsOption(refIname))
						return element.getOptionByInternalName(refIname).getQualifiers();
				}
				return null; // nothing found
			}
		}
	}

	/**
	 * @return
	 */
	public String getType() {
		return type;
	}

	public String getType(String refIname) {
		if (uiOptionNameMap.containsKey(refIname))
			return ((Option) uiOptions.get((Integer) uiOptionNameMap.get(internalName))).getType(refIname);
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

	public Option getOptionByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supports(field, tableConstraint))
			return lastSupportingOption;
		else
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
		buf.append(", qualifiers=").append(qualifiers);
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

		if (hashcode == -1) {

			hashcode = super.hashCode();

			hashcode = (isSelectable) ? (31 * hashcode) + 1 : hashcode;
			hashcode = (31 * hashcode) + field.hashCode();
			hashcode = (31 * hashcode) + tableConstraint.hashCode();
			hashcode = (31 * hashcode) + value.hashCode();
			hashcode = (31 * hashcode) + ref.hashCode();
			hashcode = (31 * hashcode) + qualifiers.hashCode();
			hashcode = (31 * hashcode) + type.hashCode();
			hashcode = (handler != null) ? (31 * hashcode) + handler.hashCode() : hashcode;

			for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
				hashcode = (31 * hashcode) + iter.next().hashCode();
			}

			for (Iterator iter = uiOptionPushes.iterator(); iter.hasNext();) {
				hashcode = (31 * hashcode) + iter.next().hashCode();
			}
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
    if ( field!=null ) return field;
    else return getParent().getFieldFromContext();
  }
  
  
  /**
   * Returns value based on context. 
   * @return value if set otherwise getParent().getValueFromContext().
   */
  public String getValueFromContext() {
      if ( value!=null ) return value;
      else return getParent().getValueFromContext();
    }
    

  /**
   * Returns type based on context. 
   * @return type if set otherwise getParent().getTypeFromContext().
   */
	public String getTypeFromContext() {
    if ( type!=null ) return type;
    else return getParent().getTypeFromContext();
	}

  /**
   * Returns handler based on context. 
   * @return handler if set otherwise getParent().getHandlerFromContext().
   */
	public String getHandlerFromContext() {
    if ( handler!=null ) return handler;
    else return getParent().getHandlerFromContext();
	}

  /**
   * Returns tableContraint based on context. 
   * @return tableConstraint if set otherwise getParent().getTableConstraintFromContext().
   */
	public String getTableConstraintFromContext() {
    if ( tableConstraint!=null ) return tableConstraint;
    else return getParent().getTableConstraintFromContext();
  }
  
}
