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

package org.ensembl.mart.explorer.config;

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
public class Dataset {

	/*
	 * Datasets must have an internalName, so dont allow parameterless construction
	 */
	private Dataset() throws ConfigurationException {
		this("", "", ""); // will never happen
	}
	/**
	 * Constructs a Dataset named by internalName and displayName.
	 *  internalName is a single word that references this dataset, used to get the dataset from the MartConfiguration by name.
	 *  displayName is the String to display in any UI.
	 * 
	 * @param internalName String name to represent this Dataset
	 * @param displayName String name to display.
	 */
	public Dataset(String martName, String displayName)
		throws ConfigurationException {
		this(martName, displayName, "");
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
	public Dataset(String martName, String displayName, String description)
		throws ConfigurationException {
		if (martName == null)
			throw new ConfigurationException("Datasets must contain a displayName");

		this.internalName = martName;
		this.displayName = displayName;
		this.description = description;
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
	 * Returns the displayName of the Dataset.
	 * 
	 * @return displayName String.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the internalName to represent this dataset.
	 * 
	 * @return String internalName
	 */
	public String getInternalName() {
		return internalName;
	}

	/**
	 * Returns the Description of the Dataset.
	 * 
	 * @return description String
	 */
	public String getDescription() {
		return description;
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
		* Convenience method for non graphical UI.  Allows a call against the Dataset for a particular UIAttributeDescription.
		* Note, it is best to first call containsUIAttributeDescription,
		* as there is a caching system to cache a UIAttributeDescription during a call to containsUIAttributeDescription.
		* 
		* @param displayName name of the requested UIAttributeDescription
		* @return Object (either UIAttributeDescription or UIDSAttributeDescription)
		*/
	public Object getUIAttributeDescriptionByName(String internalName) {
		if ( containsUIAttributeDescription(internalName) )
			return lastAtt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the Dataset contains a specific UIAttributeDescription.
		*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
		*  found, this method caches the UIAttributeDescription it has found.
		* 
		* @param displayName name of the requested UIAttributeDescription
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIAttributeDescription(String internalName) {
		boolean found = false;

		if (lastAtt == null) {
			for (Iterator iter = (Iterator) attributePages.keySet().iterator();
				iter.hasNext();
				) {
				AttributePage page =
					(AttributePage) attributePages.get((Integer) iter.next());
				if (page.containsUIAttributeDescription(internalName)) {
					lastAtt = page.getUIAttributeDescriptionByName(internalName);
					found = true;
					break;
				}
			}
		}
		else {
			String lastAttName;
			if (lastAtt instanceof UIAttributeDescription)
			  lastAttName =  ((UIAttributeDescription) lastAtt).getInternalName();
			else
			  lastAttName =  ((UIDSAttributeDescription) lastAtt).getInternalName();
			
			if (lastAttName.equals(internalName))
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
		* UIFilterDescription/UIDSFilterDescription Object. Note, it is best to first call 
		* containsUIFilterDescription, as there is a caching system to cache a FilterDescription Object 
		* during a call to containsUIFilterDescription.
		* 
		* @param displayName name of the requested UIFilterDescription
		* @return Object (either instanceof UIFilterDescription or UIDSFilterDescription)
		*/
	public Object getUIFilterDescriptionByName(String internalName) {
		if ( containsUIFilterDescription(internalName) )
			return lastFilt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the Dataset contains a specific UIFilterDescription/UIDSFilterDescription object.
		*  As an optimization for initial calls to containsUIFilterDescription with an immediate call to getUIFilterDescriptionByName if
		*  found, this method caches the UIFilterDescription Object it has found.
		* 
		* @param displayName name of the requested UIFilterDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsUIFilterDescription(String internalName) {
		boolean found = false;

		if (lastFilt == null) {
			for (Iterator iter = (Iterator) filterPages.keySet().iterator();
				iter.hasNext();
				) {
				FilterPage page = (FilterPage) filterPages.get((Integer) iter.next());
				if (page.containsUIFilterDescription(internalName)) {
					lastFilt = page.getUIFilterDescriptionByName(internalName);
					found = true;
					break;
				}
			}
		}
		else {
			String lastIntName;
			if (lastFilt instanceof UIFilterDescription)
				lastIntName = ( (UIFilterDescription) lastFilt).getInternalName();
			else if (lastFilt instanceof UIDSFilterDescription)
				lastIntName = ( (UIDSFilterDescription) lastFilt).getInternalName();
			else
				lastIntName = ""; // should not get here
			  
			if ( lastIntName.equals(internalName) )
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
	 * Returns the FilterPage containing a specific UIFilterDescription, named by internalName
	 * 
	 * @param internalName -- String name of the FilterPage containing the requested UIFilterDescription
	 * 
	 * @return FilterPage object containing the requested UIFilterDescription
	 */
	public FilterPage getPageForUIFilterDescription(String internalName) {
	  boolean found = false;
		FilterPage page = null;
		
		for (Iterator iter = (Iterator) filterPages.keySet().iterator();iter.hasNext();) {
			page = (FilterPage) filterPages.get((Integer) iter.next());
			
			if (page.containsUIFilterDescription(internalName)) {
				found = true;
				break;
			}
		}
		
		if (found)
		  return page;
		else
		  return null;
	}

/**
 * Returns the AttributePage containing a specific UIAttributeDescription named by internalName.
 * 
 * @param internalName -- String internalName of the requested UIAttributeDescription
 * @return AttributePage containing requested UIAttributeDescription
 */
  public AttributePage getPageForUIAttributeDescription(String internalName) {
    boolean found = false;
    AttributePage page = null;
    
    for (Iterator iter = (Iterator) attributePages.keySet().iterator(); iter.hasNext();) {
      page = (AttributePage) attributePages.get( (Integer) iter.next());
      
      if (page.containsUIAttributeDescription(internalName)) {
      	found = true;
      	break;
      }
    }
    
    if (found)
      return page;
    else
      return null;
  }
  
	/**
	 * Convenience Method to get all FilterDescription Objects in all Pages/Groups/Collections within a Dataset.
	 * 
	 * @return List of UIFilterDescription/UIDSFilterDescription objects
	 */
	public List getAllUIFilterDescriptions() {
		List filts = new ArrayList();
  	
		for (Iterator iter = filterPages.keySet().iterator(); iter.hasNext();) {
			FilterPage fp = (FilterPage) filterPages.get((Integer) iter.next());
  		
			filts.addAll( fp.getAllUIFilterDescriptions() );
		}
		
		return filts;  	
	}

	/**
	 * Convenience Method to get all UIAttributeDescription objects in all Pages/Groups/Collections within a Dataset.
	 * 
	 * @return List of UIAttributeDescription/UIDSAttributeDescription objects
	 */
	public List getAllUIAttributeDescriptions() {
		List atts = new ArrayList();
  	
		for (Iterator iter = filterPages.keySet().iterator(); iter.hasNext();) {
			AttributePage ap = (AttributePage) filterPages.get((Integer) iter.next());
  		
			atts.addAll(ap.getAllUIAttributeDescriptions());
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
	 * Provides output useful for debugging purposes.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		buf.append(" internalName=").append(internalName);
		buf.append(", displayName=").append(displayName);
		buf.append(", description=").append(description);
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
  	int tmp = internalName.hashCode();
  	
		tmp = (31 * tmp) + displayName.hashCode();
		tmp = (31 * tmp) + description.hashCode();
		
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
	private final String internalName, displayName, description;

	// cache one UIAttributeDescription/UIDSAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
	private Object lastAtt = null;
	//cache one FilterDescription Object for call to containsUIFilterDescription or getUIFiterDescriptionByName
	private Object lastFilt = null;
}
