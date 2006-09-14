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
import java.util.List;
import java.util.logging.Logger;
/**
 * 
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 */

public class DynamicFilterContent extends BaseNamedConfigurationObject {

	private Hashtable uiOptionNameMap = new Hashtable();
	private List uiOptions = new ArrayList();
	private boolean hasOptions = false;

	private Logger logger =
	  Logger.getLogger(DynamicFilterContent.class.getName());


  private final String otherFiltersKey = "otherFilters"; 
  private final String pointerDatasetKey = "pointerDataset";
  private final String pointerInterfaceKey = "pointerInterface";
  //private final String pointerAttributeKey = "pointerAttribute";
  private final String pointerFilterKey = "pointerFilter";
  
  private int[] reqFields = {0,1};// rendered red in AttributeTable

  /**
   * Copy constructor. Constructs an exact copy of an existing DynamiacFilterContent.
   * @param a DynamicFilterContent to copy.
   */
  public DynamicFilterContent(DynamicFilterContent a) {
    super(a);
	setRequiredFields(reqFields);
  }

  /**
   * Empty Constructor should only be used by DatasetConfigEditor
   *
   */
  public DynamicFilterContent() {
    super();
    
    setAttribute(otherFiltersKey, null);
	setAttribute(pointerDatasetKey, null);
	setAttribute(pointerInterfaceKey, null);
	//setAttribute(pointerAttributeKey, null);
	setAttribute(pointerFilterKey, null);
	setRequiredFields(reqFields);
  }

  /**
   * Constructor for a DynamicFilterContent.
   * 
   * @param internalName String name to internally represent the DynamicFilterContent. Must not be null or empty.
   * @param otherFilters .
   * @throws ConfigurationException when required parameters are null or empty
   */
  public DynamicFilterContent(String internalName,String otherFilters) throws ConfigurationException {
    super(internalName, "","");

    setAttribute(otherFiltersKey, otherFilters);
	setAttribute(pointerDatasetKey, "");
	setAttribute(pointerInterfaceKey, "");
//	setAttribute(pointerAttributeKey, pointerAttribute);
	setAttribute(pointerFilterKey, "");
	setRequiredFields(reqFields);
  }
  
  /**
   * Constructor for a DynamicFilterContent.
   * 
   * @param internalName String name to internally represent the DynamicFilterContent. Must not be null or empty.
   * @param otherFilters .
   * @throws ConfigurationException when required parameters are null or empty
   */
  public DynamicFilterContent(String internalName,String otherFilters,String pointerDataset,String pointerInterface,String pointerFilter) throws ConfigurationException {
	super(internalName, "","");

	setAttribute(otherFiltersKey, otherFilters);
	setAttribute(pointerDatasetKey, pointerDataset);
	setAttribute(pointerInterfaceKey, pointerInterface);
//	setAttribute(pointerAttributeKey, pointerAttribute);
	setAttribute(pointerFilterKey, pointerFilter);
	setRequiredFields(reqFields);
  }

  /**
   * @param otherFilters - String 
   */
  public void setOtherFilters(String otherFilters) {
    setAttribute(otherFiltersKey, otherFilters);
  }

  /**
   * Returns the otherFilters
   * @return String otherFilters.
   */
  public String getOtherFilters() {
    return getAttribute(otherFiltersKey);
  }

  /**
   * @param pointerDataset - pointer dataset, used for placeholder attributes
   */
  public void setPointerDataset(String pointerDataset) {
	setAttribute(pointerDatasetKey, pointerDataset);
  }

  /**
   * Returns the pointerDataset.
   * 
   * @return String pointerDataset
   */
  public String getPointerDataset() {
	return getAttribute(pointerDatasetKey);
  }

  /**
   * @param pointerInterface - pointer interface, used for placeholder attributes
   */
  public void setPointerInterface(String pointerInterface) {
	setAttribute(pointerInterfaceKey, pointerInterface);
  }

  /**
   * Returns the pointerInterface.
   * 
   * @return String pointerInterface
   */
  public String getPointerInterface() {
	return getAttribute(pointerInterfaceKey);
  }
  
  /**
   * @param pointerfilter - pointer filter, used for placeholder filters
   */
  public void setPointerFilter(String pointerFilter) {
	setAttribute(pointerFilterKey, pointerFilter);
  }

  /**
   * Returns the pointerDataset.
   * 
   * @return String pointerFilter
   */
  public String getPointerFilter() {
	return getAttribute(pointerFilterKey);
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
 * Remove Options from the FilterDescription.
 */
public void removeOptions() {
  //uiOptionNameMap.clear();
  //uiOptions.clear();
  uiOptionNameMap = new Hashtable();
  uiOptions = new ArrayList();
  hasOptions = false;
  //Option[] ops = getOptions();
  //for (int i = 0; i < ops.length; i++){
  //	removeOption(ops[i]);
  //}
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



  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[ DynamicFilterContent:");
    buf.append(super.toString());
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons of DynamicFilterContent objects
   */
  public boolean equals(Object o) {
    return o instanceof DynamicFilterContent
      && hashCode() == ((DynamicFilterContent) o).hashCode();
  }

  public boolean isBroken() {
	return false;
  }

}
