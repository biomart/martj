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
 * Datasets must have a displayName, so dont allow parameterless construction
 */
 private Dataset() throws ConfigurationException {
 	this("", "", ""); // will never happen
 }
  /**
   * Constructs a Dataset named by martName and displayName.
   *  martName is a single word that references this dataset, used to get the dataset from the MartConfiguration by name.
   *  displayName is the String to display in any UI.
   * 
   * @param martName String name to represent this Dataset
   * @param displayName String name to display.
   */
  public Dataset(String martName, String displayName) throws ConfigurationException {
  	this(martName, displayName, "");
  }
  
  /**
   * Constructs a Dataset named by martName and displayName, with a description of
   *  the dataset.
   * 
   * @param martName String name to represent this Dataset. Must not be null
   * @param displayName String name to display in an UI. Must not be null.
   * @param description String description of the Dataset.
   * @throws ConfigurationException if required values are null.
   */
  public Dataset(String martName, String displayName, String description) throws ConfigurationException {
  	if(martName == null || displayName == null)
  	  throw new ConfigurationException("Datasets must contain a displayName");
  
  this.martName = martName;	  
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
  public void addStar(String starname) {
  	stars.add(starname);
  }
  
  /**
   * Set all star names for a Dataset with one call.
   * Note, subsequent calls to setStars or addStar will add
   * stars to what has been added before.
   * 
   * @param starnames String[] Array of star names.
   */
  public void setStars(String[] starnames) {
  	stars.addAll( Arrays.asList(starnames) );
  }
  
  /**
   * Adds a primary key to the dataset.  This is the key that joins the dimension tables
   *  with the star table.  Datasets with multiple stars may have multiple primary keys.
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
  	primaryKeys.addAll( Arrays.asList(pkeys) );
  }
  
  /**
   * Add an AttributePage to the Dataset.
   * 
   * @param a
   */
  public void addAttributePage(AttributePage a) {
		Integer rankInt = new Integer(apageRank);
		attributePages.put(rankInt, a);
		attributePageNameMap.put(a.getDisplayName(), rankInt);
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
			attributePageNameMap.put(a[i].getDisplayName(), rankInt);
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
  	filterPageNameMap.put(f.getDisplayName(), rankInt);
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
  	for (int i = 0, n=f.length; i < n; i++) {
  		Integer rankInt = new Integer(fpageRank);
			filterPages.put(rankInt, f[i]);
			filterPageNameMap.put(f[i].getDisplayName(), rankInt);
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
   * Returns the martName to represent this dataset.
   * 
   * @return String martName
   */
  public String getMartName() {
  	return martName;
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
   * @return stars String[]
   */
  public String[] getStars() {
  	String[] s = new String[ stars.size() ];
  	stars.toArray(s);
  	return s;
  }
  
  /**
   * Returns a list of primary keys for this Dataset.
   * 
   * @return pkeys String[]
   */
  public String[] getPrimaryKeys() {
  	String[] p = new String[ primaryKeys.size() ];
  	primaryKeys.toArray(p);
  	return p;
  }
  
  /**
   * Returns a list of all AttributePage objects contained in this Dataset, in the order they were added.
   * 
   * @return attributePages AttributePage[]
   */
  public AttributePage[] getAttributePages() {
  	AttributePage[] as = new AttributePage[ attributePages.size() ];
  	attributePages.values().toArray(as);
  	return as;
  }
  
  /**
   * Returns a particular AttributePage named by a given displayName.
   * 
   * @param displayName String name of a particular AttributePage
   * @return AttributePage object named by the given displayName
   */
  public AttributePage getAttributePageByName(String displayName) {
  	return (AttributePage) attributePages.get( (Integer) attributePageNameMap.get(displayName) );
  }
  
	/**
	 * Check whether a Dataset contains a particular AttributePage named by displayName.
	 * 
	 * @param displayName String name of the AttributePage
	 * @return boolean true if AttributePage is contained in the Dataset, false if not.
	 */
  public boolean containsAttributePage(String displayName) {
  	return attributePageNameMap.containsKey(displayName);
  }
   
  /**
   * Returns a list of all FilterPage objects contained within the Dataset, in the order they were added.
   * @return FilterPage[]
   */
  public FilterPage[] getFilterPages() {
  	FilterPage[] fs = new FilterPage[ filterPages.size()];
  	filterPages.values().toArray(fs);
  	return fs;
  }
  
  /**
   * Returns a particular FilterPage object named by a given displayName.
   * 
   * @param displayName String name of a particular FilterPage
   * @return FilterPage object named by the given displayName
   */
  public FilterPage getFilterPageByName(String displayName) {
  	return (FilterPage) filterPages.get( (Integer) filterPageNameMap.get(displayName));
  }
  
  /**
   * Check whether a Dataset contains a particular FilterPage named by displayName.
   * 
   * @param displayName String name of the FilterPage
   * @return boolean true if FilterPage is contained in the Dataset, false if not.
   */
  public boolean containsFilterPage(String displayName) {
  	return filterPageNameMap.containsKey(displayName);
  }
  
	/**
		* Convenience method for non graphical UI.  Allows a call against the Dataset for a particular UIAttributeDescription.
		* 
		* @param displayName name of the requested UIAttributeDescription
		* @return UIAttributeDescription object
		* @throws ConfigurationException when the UIAttributeDescription is not found.  Note, it is best to first call containsUIAttributeDescription,
		*                   as there is a caching system to cache a UIAttributeDescription during a call to containsUIAttributeDescription.
		*/
		 public UIAttributeDescription getUIAttributeDescriptionByName(String displayName) throws ConfigurationException {
				boolean found = false;
		  
				if (lastAtt != null && lastAtt.getDisplayName().equals(displayName)) {
					found = true;
				}
				else {
					for (Iterator iter = (Iterator) attributePages.keySet().iterator(); iter.hasNext();) {
						AttributePage page = (AttributePage) attributePages.get( (Integer) iter.next() );
						if (page.containsUIAttributeDescription(displayName)) {
							lastAtt = page.getUIAttributeDescriptionByName(displayName);
							found = true;
							break;
						}
					}
				}
				if (found)
					 return lastAtt;
				else
					 throw new ConfigurationException("Could not find UIAttributeDescription "+displayName+" in this dataset");
		 }
   
		 /**
			* Convenience method for non graphical UI.  Can determine if the Dataset contains a specific UIAttributeDescription.
			*  As an optimization for initial calls to containsUIAttributeDescription with an immediate call to getUIAttributeDescriptionByName if
			*  found, this method caches the UIAttributeDescription it has found.
			* 
			* @param displayName name of the requested UIAttributeDescription
			* @return boolean, true if found, false if not.
			*/
		 public boolean containsUIAttributeDescription(String displayName) throws ConfigurationException {
			boolean found = false;
		
			if (lastAtt != null && lastAtt.getDisplayName().equals(displayName)) {
				found = true;
			}
			else {   	  
				for (Iterator iter = (Iterator) attributePages.keySet().iterator(); iter.hasNext();) {
					AttributePage page = (AttributePage) attributePages.get( (Integer) iter.next() );
					if (page.containsUIAttributeDescription(displayName)) {
						lastAtt = page.getUIAttributeDescriptionByName(displayName);
						found = true;
						break;
					}
				}
			}
			return found;  
		 }
		 
	/**
		* Convenience method for non graphical UI.  Allows a call against the Dataset for a particular UIFilterDescription.
		* 
		* @param displayName name of the requested UIFilterDescription
		* @return UIFilterDescription object
		* @throws ConfigurationException when the UIFilterDescription is not found.  Note, it is best to first call containsUIFilterDescription,
		*                   as there is a caching system to cache a UIFilterDescription during a call to containsUIFilterDescription.
		*/
		 public UIFilterDescription getUIFilterDescriptionByName(String displayName) throws ConfigurationException {
				boolean found = false;
		  
				if (lastFilt != null && lastFilt.getDisplayName().equals(displayName)) {
					found = true;
				}
				else {
					for (Iterator iter = (Iterator) filterPages.keySet().iterator(); iter.hasNext();) {
						FilterPage page = (FilterPage) filterPages.get( (Integer) iter.next() );
						if (page.containsUIFilterDescription(displayName)) {
							lastFilt = page.getUIFilterDescriptionByName(displayName);
							found = true;
							break;
						}
					}
				}
				if (found)
					 return lastFilt;
				else
					 throw new ConfigurationException("Could not find UIFilterDescription "+displayName+" in this dataset");
		 }
   
		 /**
			* Convenience method for non graphical UI.  Can determine if the Dataset contains a specific UIFilterDescription.
			*  As an optimization for initial calls to containsUIFilterDescription with an immediate call to getUIFilterDescriptionByName if
			*  found, this method caches the UIFilterDescription it has found.
			* 
			* @param displayName name of the requested UIFilterDescription object
			* @return boolean, true if found, false if not.
			*/
		 public boolean containsUIFilterDescription(String displayName) throws ConfigurationException {
			boolean found = false;
		
			if (lastFilt != null && lastFilt.getDisplayName().equals(displayName)) {
				found = true;
			}
			else {   	  
				for (Iterator iter = (Iterator) filterPages.keySet().iterator(); iter.hasNext();) {
					FilterPage page = (FilterPage) filterPages.get( (Integer) iter.next() );
					if (page.containsUIFilterDescription(displayName)) {
						lastFilt = page.getUIFilterDescriptionByName(displayName);
						found = true;
						break;
					}
				}
			}
			return found;  
		 }
		 
  /**
   * Provides output useful for debugging purposes.
   */
  public String toString() {
  	StringBuffer buf = new StringBuffer();
  	buf.append("[");
  	buf.append(" dataset=").append(displayName);
  	buf.append(", description=").append(description);
  	buf.append(", starnames=").append(stars);
  	buf.append(", primarykeys=").append(primaryKeys);
  	buf.append(", filterPages=").append(filterPages);
  	buf.append(", attributePages=").append(attributePages);
  	buf.append("]");
  	
  	return buf.toString();
  }
  
//keep track of ordering of filter and attribute pages
  private int apageRank = 0;
  private int fpageRank = 0; 
  
  private TreeMap attributePages = new TreeMap();
  private TreeMap filterPages = new TreeMap();
  private Hashtable attributePageNameMap = new Hashtable();
  private Hashtable filterPageNameMap = new Hashtable();
  private List stars = new ArrayList();
  private List primaryKeys = new ArrayList();
  private final String martName, displayName, description;
  
  // cache one UIAttributeDescription for call to containsUIAttributeDescription or getUIAttributeDescriptionByName
  private UIAttributeDescription lastAtt = null;
//cache one UIFilterDescription for call to containsUIFilterDescription or getUIFiterDescriptionByName
 private UIFilterDescription lastFilt = null;
}
