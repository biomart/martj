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
 * Container for a mart dataset.   A dataset contains information about the
 *  main (or star) table(s) that it provides, the primary key(s) for these table(s),
 * and a List of AttributePages and FilterPages containing all of the attributes 
 * and filters that it provides.
 *   
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class Dataset extends BaseConfigurationObject {

	/*
	 * Datasets must have an internalName, so dont allow parameterless construction
	 */
	public Dataset() throws ConfigurationException {
		this("", "", "");
	}
	/**
	 * Constructs a Dataset named by internalName and displayName.
	 *  internalName is a single word that references this dataset, used to get the dataset from the MartConfiguration by name.
	 *  displayName is the String to display in any UI.
	 * 
	 * @param internalName String name to represent this Dataset
	 * @param displayName String name to display.
	 */
	public Dataset(String internalName, String displayName) throws ConfigurationException {
		this(internalName, displayName, "");
	}

	/**
	 * Constructs a Dataset named by internalName and displayName, with a description of
	 *  the dataset.
	 * 
	 * @param internalName String name to represent this Dataset. Must not be null
	 * @param displayName String name to display in an UI.
	 * @param description String description of the Dataset.
	 * @throws ConfigurationException if required values are null.
	 */
	public Dataset(String internalName, String displayName, String description) throws ConfigurationException {
		super(internalName, displayName, description);
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
   * Add a DefaultFilter object to this Dataset.
   * @param df - A DefaultFilter object
   */
  public void addDefaultFilter(DefaultFilter df) {
    hasDefaultFilters = true;
    if (!defaultFilters.contains(df))
      defaultFilters.add(df);
  }
  
  /**
   * Add a set of DefaultFilter objects in one call.
   * Note, subsequent calls to addDefaultFilter or setDefaultFilter
   * will add to what was added before.
   * @param df - An Array of DefaultFilter objects
   */
  public void setDefaultFilters(DefaultFilter[] df) {
    for (int i = 0, n = df.length; i < n; i++) {
			addDefaultFilter(df[i]);
		}
  }
  
 	/**
	 * Adds a star name to the list for this Dataset.  A star name is the
	 * name of a central, or main, table to which all mart facts are tied.
	 * Datasets can contain more than one star name.
	 * 
	 * @param starname  String name of a main table for a mart.
	 */
	public void addStarBase(String starname) {
		starBases.add(starname);
	}

	/**
	 * Set all star names for a Dataset with one call.
	 * Note, subsequent calls to setStars or addStar will add
	 * starBases to what has been added before.
	 * 
	 * @param starnames String[] Array of star names.
	 */
	public void setStarBases(String[] starnames) {
		starBases.addAll(Arrays.asList(starnames));
	}

	/**
	 * Adds a primary key to the dataset.  This is the key that joins the dimension tables
	 *  with the star table.  Datasets with multiple starBases may have multiple primary keys.
	 * 
	 * @param primaryKey String name of primary key.
	 */
	public void addPrimaryKey(String primaryKey) {
		primaryKeys.add(primaryKey);
	}

	/**
	 * Set all primary keys in one method.
	 * Note, subsequent calls to addPrimaryKey or setPrimaryKeys
	 * will add primary keys to what has been added before.
	 * 
	 * @param pkeys String[] array of primary keys.
	 */
	public void setPrimaryKeys(String[] pkeys) {
		primaryKeys.addAll(Arrays.asList(pkeys));
	}

	/**
	 * Add an AttributePage to the Dataset.
	 * 
	 * @param a
	 */
	public void addAttributePage(AttributePage a) {
		Integer rankInt = new Integer(apageRank);
		attributePages.put(rankInt, a);
		attributePageNameMap.put(a.getInternalName(), rankInt);
		apageRank++;
	}

	/**
	 * Set a group of AttributePage objects in one call.
	 * Note, subsequent calls to addAttributePage or
	 * setAttributePages will add to what has been added before.
	 * 
	 * @param a AttributePage[] array of AttributePages.
	 */
	public void setAttributePages(AttributePage[] a) {
		for (int i = 0; i < a.length; i++) {
			Integer rankInt = new Integer(apageRank);
			attributePages.put(rankInt, a[i]);
			attributePageNameMap.put(a[i].getInternalName(), rankInt);
			apageRank++;
		}
	}

	/**
	 * Add a FilterPage to the Dataset.
	 * 
	 * @param f FiterPage object.
	 */
	public void addFilterPage(FilterPage f) {
		Integer rankInt = new Integer(fpageRank);
		filterPages.put(rankInt, f);
		filterPageNameMap.put(f.getInternalName(), rankInt);
		fpageRank++;
	}

	/**
	 * Set a group of FilterPage objects in one call.
	 * Note, subsequent calls to addFilterPage or setFilterPages
	 * will add to what has been added before.
	 * 
	 * @param f FilterPage[] array of FilterPage objects.
	 */
	public void setFilterPages(FilterPage[] f) {
		for (int i = 0, n = f.length; i < n; i++) {
			Integer rankInt = new Integer(fpageRank);
			filterPages.put(rankInt, f[i]);
			filterPageNameMap.put(f[i].getInternalName(), rankInt);
			fpageRank++;
		}
	}

  /**
   * Determine if this Dataset has Options Available.
   * 
   * @return boolean, true if Options are available, false if not.
   */
  public boolean hasOptions() {
    return hasOptions;
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
   * Determine if this Dataset has DefaultFilters available.
   * @return boolean, true if DefaultFilter(s) are available, false if not
   */
  public boolean hasDefaultFilters() {
    return hasDefaultFilters;
  }
  
  /**
   * Returns all DefaultFilter Objects added to the Dataset.
   * @return DefaultFilter[] array of DefaultFilter objects.
   */
  public DefaultFilter[] getDefaultFilters() {
    DefaultFilter[] ret = new DefaultFilter[defaultFilters.size()];
    defaultFilters.toArray(ret);
    return ret;
  }
  
	/**
	 * Returns the list of star names for this Dataset.
	 * 
	 * @return starBases String[]
	 */
	public String[] getStarBases() {
		String[] s = new String[starBases.size()];
		starBases.toArray(s);
		return s;
	}

	/**
	 * Returns a list of primary keys for this Dataset.
	 * 
	 * @return pkeys String[]
	 */
	public String[] getPrimaryKeys() {
		String[] p = new String[primaryKeys.size()];
		primaryKeys.toArray(p);
		return p;
	}

	/**
	 * Returns a list of all AttributePage objects contained in this Dataset, in the order they were added.
	 * 
	 * @return attributePages AttributePage[]
	 */
	public AttributePage[] getAttributePages() {
		AttributePage[] as = new AttributePage[attributePages.size()];
		attributePages.values().toArray(as);
		return as;
	}

	/**
	 * Returns a particular AttributePage named by a given displayName.
	 * 
	 * @param displayName String name of a particular AttributePage
	 * @return AttributePage object named by the given displayName, or null.
	 */
	public AttributePage getAttributePageByName(String internalName) {
		if (attributePageNameMap.containsKey(internalName))
			return (AttributePage) attributePages.get((Integer) attributePageNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check whether a Dataset contains a particular AttributePage named by displayName.
	 * 
	 * @param displayName String name of the AttributePage
	 * @return boolean true if AttributePage is contained in the Dataset, false if not.
	 */
	public boolean containsAttributePage(String internalName) {
		return attributePageNameMap.containsKey(internalName);
	}

	/**
	 * Returns a list of all FilterPage objects contained within the Dataset, in the order they were added.
	 * @return FilterPage[]
	 */
	public FilterPage[] getFilterPages() {
		FilterPage[] fs = new FilterPage[filterPages.size()];
		filterPages.values().toArray(fs);
		return fs;
	}

	/**
	 * Returns a particular FilterPage object named by a given displayName.
	 * 
	 * @param displayName String name of a particular FilterPage
	 * @return FilterPage object named by the given displayName, or null
	 */
	public FilterPage getFilterPageByName(String internalName) {
		if (filterPageNameMap.containsKey(internalName))
			return (FilterPage) filterPages.get((Integer) filterPageNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check whether a Dataset contains a particular FilterPage named by displayName.
	 * 
	 * @param displayName String name of the FilterPage
	 * @return boolean true if FilterPage is contained in the Dataset, false if not.
	 */
	public boolean containsFilterPage(String internalName) {
		return filterPageNameMap.containsKey(internalName);
	}

	/**
		* Convenience method for non graphical UI.  Allows a call against the Dataset for a particular AttributeDescription.
		* Note, it is best to first call containsUIAttributeDescription,
		* as there is a caching system to cache a AttributeDescription during a call to containsUIAttributeDescription.
		* 
		* @param internalName name of the requested AttributeDescription
		* @return AttributeDescription
		*/
	public Object getUIAttributeDescriptionByName(String internalName) {
		if (containsUIAttributeDescription(internalName))
			return lastAtt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the Dataset contains a specific AttributeDescription.
		*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
		*  found, this method caches the AttributeDescription it has found.
		* 
		* @param internalName name of the requested AttributeDescription
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIAttributeDescription(String internalName) {
		boolean found = false;

		if (lastAtt == null) {
			for (Iterator iter = (Iterator) attributePages.keySet().iterator(); iter.hasNext();) {
				AttributePage page = (AttributePage) attributePages.get((Integer) iter.next());
				if (page.containsUIAttributeDescription(internalName)) {
					lastAtt = page.getUIAttributeDescriptionByName(internalName);
					found = true;
					break;
				}
			}
		} else {
			if (lastAtt.getInternalName().equals(internalName))
				found = true;
			else {
				lastAtt = null;
				found = containsUIAttributeDescription(internalName);
			}
		}
		return found;
	}

	/**
		* Convenience method for non graphical UI.  Allows a call against the Dataset for a particular 
		* FilterDescription/MapFilterDescription Object. Note, it is best to first call 
		* containsUIFilterDescription, as there is a caching system to cache a FilterDescription Object 
		* during a call to containsUIFilterDescription.
		* 
		* @param displayName name of the requested FilterDescription
		* @return Object (either instanceof FilterDescription or MapFilterDescription)
		*/
	public Object getUIFilterDescriptionByName(String internalName) {
		if (containsUIFilterDescription(internalName))
			return lastFilt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the Dataset contains a specific FilterDescription/MapFilterDescription object.
		*  As an optimization for initial calls to containsUIFilterDescription with an immediate call to getUIFilterDescriptionByName if
		*  found, this method caches the FilterDescription Object it has found.
		* 
		* @param displayName name of the requested FilterDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIFilterDescription(String internalName) {
		boolean found = false;

		if (lastFilt == null) {
			for (Iterator iter = (Iterator) filterPages.keySet().iterator(); iter.hasNext();) {
				FilterPage page = (FilterPage) filterPages.get((Integer) iter.next());
				if (page.containsUIFilterDescription(internalName)) {
					lastFilt = page.getUIFilterDescriptionByName(internalName);
					found = true;
					break;
				}
			}
		} else {
			String lastIntName;
			if (lastFilt instanceof FilterDescription)
				lastIntName = ((FilterDescription) lastFilt).getInternalName();
			else if (lastFilt instanceof MapFilterDescription)
				lastIntName = ((MapFilterDescription) lastFilt).getInternalName();
			else
				lastIntName = ""; // should not get here

			if (lastIntName.equals(internalName))
				found = true;
			else {
				lastFilt = null;
				found = containsUIFilterDescription(internalName);
			}
		}
		return found;
	}

	/**
	 * Convenience method for non graphical UIs.
	 * Returns the FilterPage containing a specific FilterDescription, named by internalName
	 * Note, if a UIFlilterDescription is contained within multiple FilterPages, this
	 * will return the first FilterPage that contains the requested FilterDescription.
	 * 
	 * @param internalName -- String name of the FilterPage containing the requested FilterDescription
	 * 
	 * @return FilterPage object containing the requested FilterDescription
	 */
	public FilterPage getPageForFilter(String internalName) {
		for (Iterator iter = (Iterator) filterPages.keySet().iterator(); iter.hasNext();) {
			FilterPage page = (FilterPage) filterPages.get((Integer) iter.next());

			if (page.containsUIFilterDescription(internalName))
				return page;
		}
		return null;
	}

	/**
	 * Returns the AttributePage containing a specific AttributeDescription named by internalName.
	 * Note, if a AttributeDescription is contained in multiple AttributePages, this will
	 * return the first AttributePage that contains the requested AttributeDescription.
	 * 
	 * @param internalName -- String internalName of the requested AttributeDescription
	 * @return AttributePage containing requested AttributeDescription
	 */
	public AttributePage getPageForAttribute(String internalName) {
		for (Iterator iter = (Iterator) attributePages.keySet().iterator(); iter.hasNext();) {
			AttributePage page = (AttributePage) attributePages.get((Integer) iter.next());

			if (page.containsUIAttributeDescription(internalName))
				return page;
		}
		return null;
	}

	/**
	 * Convenience Method to get all FilterDescription Objects in all Pages/Groups/Collections within a Dataset.
	 * 
	 * @return List of FilterDescription/MapFilterDescription objects
	 */
	public List getAllUIFilterDescriptions() {
		List filts = new ArrayList();

		for (Iterator iter = filterPages.keySet().iterator(); iter.hasNext();) {
			Object fpo = filterPages.get((Integer) iter.next());

      if (fpo instanceof FilterPage) {
        FilterPage fp = (FilterPage) fpo;
			  filts.addAll(fp.getAllUIFilterDescriptions());
      }
		}

		return filts;
	}

	/**
	 * Convenience Method to get all AttributeDescription objects in all Pages/Groups/Collections within a Dataset.
	 * 
	 * @return List of AttributeDescription objects
	 */
	public List getAllUIAttributeDescriptions() {
		List atts = new ArrayList();

		for (Iterator iter = attributePages.keySet().iterator(); iter.hasNext();) {
			  Object apo = attributePages.get((Integer) iter.next());
        
        if (apo instanceof AttributePage) {
          AttributePage ap = (AttributePage) apo;
			    atts.addAll(ap.getAllUIAttributeDescriptions());
        }
		}

		return atts;
	}

	/**
	 * Convenience method to facilitate equals comparisons of datasets.
	 * 
	 * @param starBase -- String name of the starBase requested
	 * @return true if Dataset contains the starBase, false if not
	 */
	public boolean containsStarBase(String starBase) {
		return starBases.contains(starBase);
	}

	/**
	 * Convenience method to facilitate equals comparisons of datasets.
	 * 
	 * @param pkey -- String name of the primary key requested
	 * @return true if Dataset contains the primary key, false if not
	 */
	public boolean containsPrimaryKey(String pkey) {
		return primaryKeys.contains(pkey);
	}
  /**
   * Returns a FilterGroup object for a specific Filter Description (FilterDescription, MapFilterDescription)
   * based on its internalName.
   * 
   * @param internalName - String internalName of Filter Description for which a group is requested.
   * @return FilterGroup for Attrribute Description provided, or null
   */
	public FilterGroup getGroupForFilter(String internalName) {
		if (!containsUIFilterDescription(internalName))
			return null;
		else if (lastFiltGroup == null) {
			lastFiltGroup = getPageForFilter(internalName).getGroupForFilter(internalName);
			return lastFiltGroup;
		} else {
			if (lastFiltGroup.getInternalName().equals(internalName))
				return lastFiltGroup;
			else {
				lastFiltGroup = null;
				return getGroupForFilter(internalName);
			}
		}
	}

  /**
   * Returns a FilterCollection object for a specific Filter Description (FilterDescription, MapFilterDescription)
   * based on its internalName.
   * 
   * @param internalName - String internalName of Filter Description for which a collection is requested.
   * @return FilterCollection for Attrribute Description provided, or null
   */
	public FilterCollection getCollectionForFilter(String internalName) {
		if (!containsUIFilterDescription(internalName)) {
			return null;
		} else if (lastFiltColl == null) {
      lastFiltColl = getGroupForFilter(internalName).getCollectionForFilter(internalName);
      return lastFiltColl;
		} else {
      if (lastFiltColl.getInternalName().equals(internalName))
        return lastFiltColl;
      else {
        lastFiltColl = null;
        return getCollectionForFilter(internalName);
      }
		}
	}

  /**
   * Returns an AttributeGroup object for a specific Attribute Description (AttributeDescription, UIDSAttributeDescription)
   * based on its internalName.
   * 
   * @param internalName - String internalName of Attribute Description for which a group is requested.
   * @return FilterGroup for Attrribute Description provided, or null
   */
	public AttributeGroup getGroupForAttribute(String internalName) {
    if (!containsUIFilterDescription(internalName))
      return null;
    else if (lastAttGroup == null) {
      lastAttGroup = getPageForAttribute(internalName).getGroupForAttribute(internalName);
      return lastAttGroup;
    } else {
      if (lastAttGroup.getInternalName().equals(internalName))
        return lastAttGroup;
      else {
        lastAttGroup = null;
        return getGroupForAttribute(internalName);
      }
    }    
	}
  
  /**
   * Returns an AttributeCollection object for a specific Attribute Description (AttributeDescription, UIDSAttributeDescription)
   * based on its internalName.
   * 
   * @param internalName - String internalName of Attribute Description for which a collection is requested.
   * @return AttributeCollection for Attribute Description provided, or null
   */
	public AttributeCollection getCollectionForAttribute(String internalName) {
    if (!containsUIAttributeDescription(internalName)) {
      return null;
    } else if (lastFiltColl == null) {
      lastAttColl = getGroupForAttribute(internalName).getCollectionForAttribute(internalName);
      return lastAttColl;
    } else {
      if (lastFiltColl.getInternalName().equals(internalName))
        return lastAttColl;
      else {
        lastFiltColl = null;
        return getCollectionForAttribute(internalName);
      }
    }
	}

	/**
	 * Provides output useful for debugging purposes.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		buf.append(super.toString());
		buf.append(", starnames=").append(starBases);
		buf.append(", primarykeys=").append(primaryKeys);
		buf.append(", filterPages=").append(filterPages);
		buf.append(", attributePages=").append(attributePages);
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of Dataset objects
	 */
	public boolean equals(Object o) {
		return o instanceof Dataset && hashCode() == ((Dataset) o).hashCode();
	}

	public int hashCode() {
		int tmp = super.hashCode();

		for (int i = 0, n = starBases.size(); i < n; i++) {
			String element = (String) starBases.get(i);
			tmp = (31 * tmp) + element.hashCode();
		}

		for (int i = 0, n = primaryKeys.size(); i < n; i++) {
			String element = (String) primaryKeys.get(i);
			tmp = (31 * tmp) + element.hashCode();
		}

		for (Iterator iter = filterPages.values().iterator(); iter.hasNext();) {
			FilterPage element = (FilterPage) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}

		for (Iterator iter = attributePages.values().iterator(); iter.hasNext();) {
			AttributePage element = (AttributePage) iter.next();
			tmp = (31 * tmp) + element.hashCode();
		}

		return tmp;
	}

	//keep track of ordering of filter and attribute pages
	private int apageRank = 0;
	private int fpageRank = 0;

	private TreeMap attributePages = new TreeMap();
	private TreeMap filterPages = new TreeMap();
	private Hashtable attributePageNameMap = new Hashtable();
	private Hashtable filterPageNameMap = new Hashtable();
	private List starBases = new ArrayList();
	private List primaryKeys = new ArrayList();
  
  private List defaultFilters = new ArrayList();
  private boolean hasDefaultFilters = false;
  
  private int oRank = 0;
  private TreeMap uiOptions = new TreeMap();
  private Hashtable uiOptionNameMap = new Hashtable();
  private boolean hasOptions = false;  
	
	// cache one AttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
	private AttributeDescription lastAtt = null;
	//cache one FilterDescription Object for call to containsUIFilterDescription or getUIFiterDescriptionByName
	private Object lastFilt = null;

	//cache one FilterGroup for call to getGroupForFilter
	private FilterGroup lastFiltGroup = null;

	//cache one FilterCollection for call to getCollectionForFilter
	private FilterCollection lastFiltColl = null;

	//cache one AttributeGroup for call to getGroupForAttribute
	private AttributeGroup lastAttGroup = null;

	//cache one AttributeCollection for call to getCollectionForAttribute
	private AttributeCollection lastAttColl = null;
}
