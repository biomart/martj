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
public class Option {
	public Option() throws ConfigurationException {
		this("", false, "", "");
	}
	
	public Option(String internalName, boolean isSelectable) throws ConfigurationException {
		this(internalName, isSelectable, "", "");
	}
	
	public Option(String internalName, boolean isSelectable, String displayName, String description) throws ConfigurationException {
		if (internalName == null || internalName.equals(""))
		  throw new ConfigurationException("Option objects must have an internalName, and a boolean isSelectable field\n");
		  
		this.internalName = internalName;
		baseHashcode = internalName.hashCode();
		
		this.isSelectable = isSelectable;
		baseHashcode = (31 * baseHashcode);
		baseHashcode += (isSelectable) ? 1 : 0;
		 
		this.displayName = displayName;
		baseHashcode = (31 * baseHashcode) + displayName.hashCode();
		
		this.description = description;
		baseHashcode = (31 * baseHashcode) + description.hashCode();		
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
  	  return (Option) uiOptions.get( (Integer) uiOptionNameMap.get(internalName) );
  	else
  	  return null;
  }
  
	/**
	 * get the Description of the Option
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * get the displayName of the Option
	 * @return String displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * get the InternalName of the Option
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
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
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
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
		int hashcode = baseHashcode;
		
		for (Iterator iter = uiOptions.values().iterator(); iter.hasNext();) {
			Option option = (Option) iter.next();
			hashcode = (31 * hashcode) + option.hashCode();
		}
		
		return hashcode;
	}
	
  private final String internalName, displayName, description;
  private final boolean isSelectable;
  private boolean hasOptions = false;
  private int baseHashcode;
  
	//options can contain options
	private int oRank = 0;
	private TreeMap uiOptions = new TreeMap();
	private Hashtable uiOptionNameMap = new Hashtable();
}
