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

import javax.sql.DataSource;

/**
 * Container for a mart dataset.   A dataset contains information about the
 *  main (or star) table(s) that it provides, the primary key(s) for these table(s),
 * and a List of AttributePages and FilterPages containing all of the attributes 
 * and filters that it provides. DatasetView Objects support a lazy load optimization strategy.
 * They can be instantiated with a miniumum of information (internalName), and lazy loaded when
 * the rest of the information is needed.  Any call to a get method will cause the object to attempt
 * to lazy load.  Lazy loading is only attempted when there are no FilterPage or AttributePage objects
 * loaded into the DatasetView.  Note that any call to toString, equals, and hashCode will cause lazy 
 * loading to occur, which can lead to some issues (see the documentation for each of these methods below).
 *   
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatasetView extends BaseConfigurationObject {

	/*
	 * Datasets must have an internalName, so dont allow parameterless construction
	 */
	public DatasetView() throws ConfigurationException {
		this("", "", "");
	}
	/**
	 * Constructs a DatasetView named by internalName and displayName.
	 *  internalName is a single word that references this dataset, used to get the dataset from the MartConfiguration by name.
	 *  displayName is the String to display in any UI.
	 * 
	 * @param internalName String name to represent this DatasetView
	 * @param displayName String name to display.
	 */
	public DatasetView(String internalName, String displayName) throws ConfigurationException {
		this(internalName, displayName, "");
	}

	/**
	 * Constructs a DatasetView named by internalName and displayName, with a description of
	 *  the dataset.
	 * 
	 * @param internalName String name to represent this DatasetView. Must not be null
	 * @param displayName String name to display in an UI.
	 * @param description String description of the DatasetView.
	 * @throws ConfigurationException if required values are null.
	 */
	public DatasetView(String internalName, String displayName, String description) throws ConfigurationException {
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
	 * Add a DefaultFilter object to this DatasetView.
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
	* Adds a star name to the list for this DatasetView.  A star name is the
	* name of a central, or main, table to which all mart facts are tied.
	* Datasets can contain more than one star name.
	* 
	* @param starname  String name of a main table for a mart.
	*/
	public void addStarBase(String starname) {
		starBases.add(starname);
	}

	/**
	 * Set all star names for a DatasetView with one call.
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
	 * Add an AttributePage to the DatasetView.
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
	 * Add a FilterPage to the DatasetView.
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
	 * Determine if this DatasetView has Options Available.
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
	 * Determine if this DatasetView has DefaultFilters available.
	 * @return boolean, true if DefaultFilter(s) are available, false if not
	 */
	public boolean hasDefaultFilters() {
		return hasDefaultFilters;
	}

	/**
	 * Returns all DefaultFilter Objects added to the DatasetView.
	 * @return DefaultFilter[] array of DefaultFilter objects.
	 */
	public DefaultFilter[] getDefaultFilters() {
		DefaultFilter[] ret = new DefaultFilter[defaultFilters.size()];
		defaultFilters.toArray(ret);
		return ret;
	}

	/**
	 * Returns the list of star names for this DatasetView.
	 * 
	 * @return starBases String[]
	 */
	public String[] getStarBases() {
		String[] s = new String[starBases.size()];
		starBases.toArray(s);
		return s;
	}

	/**
	 * Returns a list of primary keys for this DatasetView.
	 * 
	 * @return pkeys String[]
	 */
	public String[] getPrimaryKeys() {
		String[] p = new String[primaryKeys.size()];
		primaryKeys.toArray(p);
		return p;
	}

	/**
	 * Returns a list of all AttributePage objects contained in this DatasetView, in the order they were added.
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
	public AttributePage getAttributePageByInternalName(String internalName) {
		if (attributePageNameMap.containsKey(internalName))
			return (AttributePage) attributePages.get((Integer) attributePageNameMap.get(internalName));
		else
			return null;
	}

	/**
	 * Check whether a DatasetView contains a particular AttributePage named by displayName.
	 * 
	 * @param displayName String name of the AttributePage
	 * @return boolean true if AttributePage is contained in the DatasetView, false if not.
	 */
	public boolean containsAttributePage(String internalName) {
		return attributePageNameMap.containsKey(internalName);
	}

	/**
	 * Returns a list of all FilterPage objects contained within the DatasetView, in the order they were added.
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
	 * Check whether a DatasetView contains a particular FilterPage named by displayName.
	 * 
	 * @param displayName String name of the FilterPage
	 * @return boolean true if FilterPage is contained in the DatasetView, false if not.
	 */
	public boolean containsFilterPage(String internalName) {
		return filterPageNameMap.containsKey(internalName);
	}

	/**
		* Convenience method for non graphical UI.  Allows a call against the DatasetView for a particular AttributeDescription.
		* Note, it is best to first call containsAttributeDescription,
		* as there is a caching system to cache a AttributeDescription during a call to containsAttributeDescription.
		* 
		* @param internalName name of the requested AttributeDescription
		* @return AttributeDescription
		*/
	public AttributeDescription getAttributeDescriptionByInternalName(String internalName) {
		if (containsAttributeDescription(internalName))
			return lastAtt;
		else
			return null;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the DatasetView contains a specific AttributeDescription.
		*  As an optimization for initial calls to containsAttributeDescription with an immediate call to getAttributeDescriptionByName if
		*  found, this method caches the AttributeDescription it has found.
		* 
		* @param internalName name of the requested AttributeDescription
		* @return boolean, true if found, false if not.
		*/
	public boolean containsAttributeDescription(String internalName) {
		boolean found = false;

		if (lastAtt == null) {
			for (Iterator iter = (Iterator) attributePages.keySet().iterator(); iter.hasNext();) {
				AttributePage page = (AttributePage) attributePages.get((Integer) iter.next());
				if (page.containsAttributeDescription(internalName)) {
					lastAtt = page.getAttributeDescriptionByInternalName(internalName);
					found = true;
					break;
				}
			}
		} else {
			if (lastAtt.getInternalName().equals(internalName))
				found = true;
			else {
				lastAtt = null;
				found = containsAttributeDescription(internalName);
			}
		}
		return found;
	}

	/**
		* Convenience method for non graphical UI.  Allows a call against the DatasetView for a particular 
		* FilterDescription Object. Note, it is best to first call containsFilterDescription, as there is a 
		* caching system to cache a FilterDescription Object during a call to containsFilterDescription.
		* 
		* @param displayName name of the requested FilterDescription
		* @return FilterDescription found, or null
		*/
	public FilterDescription getFilterDescriptionByInternalName(String internalName) {
		if (containsFilterDescription(internalName))
			return lastFilt;
		else
			return null;
	}

	/**
	 * Retrieve a specific AttributeDescription that supports a given field and tableConstraint.
	 * @param field
	 * @param tableConstraint
	 * @return AttributeDescription supporting the field and tableConstraint, or null
	 */
	public AttributeDescription getAttributeDescriptionByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supportsAttributeDescription(field, tableConstraint))
			return lastSupportingAttribute;
		else
			return null;
	}

	/**
	 * Determine if this DatasetView supports a given field and tableConstraint for an Attribute.  
	 * Caches the first supporting AttributeDescription that it finds, for subsequent call to 
	 * getAttributeDescriptionByFieldNameTableConstraint.
	 * @param field
	 * @param tableConstraint
	 * @return boolean, true if an AttributeDescription contained in this AttributePage supports the field and tableConstraint, false otherwise
	 */
	public boolean supportsAttributeDescription(String field, String tableConstraint) {
		boolean supports = false;

		for (Iterator iter = attributePages.values().iterator(); iter.hasNext();) {
			AttributePage element = (AttributePage) iter.next();

			if (element.supports(field, tableConstraint)) {
				lastSupportingAttribute = element.getAttributeDescriptionByFieldNameTableConstraint(field, tableConstraint);
				supports = true;
				break;
			}
		}
		return supports;
	}

	/**
		* Convenience method for non graphical UI.  Can determine if the DatasetView contains a specific FilterDescription/MapFilterDescription object.
		*  As an optimization for initial calls to containsFilterDescription with an immediate call to getFilterDescriptionByInternalName if
		*  found, this method caches the FilterDescription Object it has found.
		* 
		* @param displayName name of the requested FilterDescription object
		* @return boolean, true if found, false if not.
		*/
	public boolean containsFilterDescription(String internalName) {
		boolean contains = false;

		if (lastFilt == null) {
			if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
				String[] refs = internalName.split("\\.");

				if (refs.length > 1 && containsFilterDescription(refs[1]))
					contains = true;
			}

			if (!contains) {
				for (Iterator iter = (Iterator) filterPages.keySet().iterator(); iter.hasNext();) {
					FilterPage page = (FilterPage) filterPages.get((Integer) iter.next());
					if (page.containsFilterDescription(internalName)) {
						lastFilt = page.getFilterDescriptionByInternalName(internalName);
						contains = true;
						break;
					}
				}
			}
		} else {
			if (lastFilt.getInternalName().equals(internalName))
				contains = true;
			else if (lastFilt.containsOption(internalName))
				contains = true;
			else if ((internalName.indexOf(".") > 0) && !(internalName.endsWith(".")) && lastFilt.getInternalName().equals(internalName.split("\\.")[1]))
				contains = true;
			else {
				lastFilt = null;
				contains = containsFilterDescription(internalName);
			}
		}
		return contains;
	}

	/**
	 * Get a FilterDescription object that supports a given field and tableConstraint.  Useful for mapping from a Filter object
	 * added to a Query back to its FilterDescription.
	 * @param field -- String field of a mart database table
	 * @param tableConstraint -- String tableConstraint of a mart database
	 * @return FilterDescription object supporting the given field and tableConstraint, or null.
	 */
	public FilterDescription getFilterDescriptionByFieldNameTableConstraint(String field, String tableConstraint) {
		if (supportsFilterDescription(field, tableConstraint))
			return lastSupportingFilter;
		else
			return null;
	}

	/**
	 * Determine if this DatasetView contains a FilterDescription that supports a given field and tableConstraint.
	 * Calling this method will cache any FilterDescription that supports the field and tableConstraint, and this will
	 * be returned by a getFilterDescriptionByFieldNameTableConstraint call.
	 * @param field -- String field of a mart database table
	 * @param tableConstraint -- String tableConstraint of a mart database
	 * @return boolean, true if the DatasetView contains a FilterDescription supporting a given field, tableConstraint, false otherwise.
	 */
	public boolean supportsFilterDescription(String field, String tableConstraint) {
		boolean supports = false;

		if (lastSupportingFilter == null) {
			for (Iterator iter = filterPages.values().iterator(); iter.hasNext();) {
				FilterPage element = (FilterPage) iter.next();
				if (element.supports(field, tableConstraint)) {
					lastSupportingFilter = element.getFilterDescriptionByFieldNameTableConstraint(field, tableConstraint);
					supports = true;
					break;
				}
			}
		} else {
			if (lastSupportingFilter.supports(field, tableConstraint))
				supports = true;
			else {
				lastSupportingFilter = null;
				supports = supportsFilterDescription(field, tableConstraint);
			}
		}
		return supports;
	}

	/**
	 * Convenience method for non graphical UIs.
	 * Returns the FilterPage containing a specific FilterDescription, named by internalName
	 * Note, if a FlilterDescription is contained within multiple FilterPages, this
	 * will return the first FilterPage that contains the requested FilterDescription.
	 * 
	 * @param internalName -- String name of the FilterPage containing the requested FilterDescription
	 * 
	 * @return FilterPage object containing the requested FilterDescription
	 */
	public FilterPage getPageForFilter(String internalName) {
		for (Iterator iter = (Iterator) filterPages.keySet().iterator(); iter.hasNext();) {
			FilterPage page = (FilterPage) filterPages.get((Integer) iter.next());

			if (page.containsFilterDescription(internalName))
				return page;
		}
		return null;
	}

	/**
	 * Returns all FilterPages that contain the FilterDescription mapped by the given internalName
	 * @param internalName - internalName of a FilterDescription
	 * @return List of FilterPages containing the FilterDescription named by internalName
	 */
	public List getPagesForFilter(String internalName) {
		List pages = new ArrayList();

		for (Iterator iter = (Iterator) filterPages.keySet().iterator(); iter.hasNext();) {
			FilterPage page = (FilterPage) filterPages.get((Integer) iter.next());

			if (page.containsFilterDescription(internalName))
				pages.add(page);
		}

		return pages;
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

			if (page.containsAttributeDescription(internalName))
				return page;
		}
		return null;
	}

	/**
	 * Returns all AttributePages that contain the AttributeDescription mapped by the given internalName
	 * @param internalName - internalName of a AttributeDescription
	 * @return List of AttributePages containing the AttributeDescription named by internalName
	 */
	public List getPagesForAttribute(String internalName) {
		List pages = new ArrayList();

		for (Iterator iter = (Iterator) attributePages.keySet().iterator(); iter.hasNext();) {
			AttributePage page = (AttributePage) attributePages.get((Integer) iter.next());

			if (page.containsAttributeDescription(internalName))
				pages.add(page);
		}

		return pages;
	}

	/**
	 * Convenience Method to get all FilterDescription Objects in all Pages/Groups/Collections within a DatasetView.
	 * 
	 * @return List of FilterDescription/MapFilterDescription objects
	 */
	public List getAllFilterDescriptions() {
		List filts = new ArrayList();

		for (Iterator iter = filterPages.keySet().iterator(); iter.hasNext();) {
			Object fpo = filterPages.get((Integer) iter.next());

			if (fpo instanceof FilterPage) {
				FilterPage fp = (FilterPage) fpo;
				filts.addAll(fp.getAllFilterDescriptions());
			}
		}

		return filts;
	}

	/**
	 * Convenience Method to get all AttributeDescription objects in all Pages/Groups/Collections within a DatasetView.
	 * 
	 * @return List of AttributeDescription objects
	 */
	public List getAllAttributeDescriptions() {
		List atts = new ArrayList();

		for (Iterator iter = attributePages.keySet().iterator(); iter.hasNext();) {
			Object apo = attributePages.get((Integer) iter.next());

			if (apo instanceof AttributePage) {
				AttributePage ap = (AttributePage) apo;
				atts.addAll(ap.getAllAttributeDescriptions());
			}
		}

		return atts;
	}

	/**
	 * Convenience method to facilitate equals comparisons of datasets.
	 * 
	 * @param starBase -- String name of the starBase requested
	 * @return true if DatasetView contains the starBase, false if not
	 */
	public boolean containsStarBase(String starBase) {
		return starBases.contains(starBase);
	}

	/**
	 * Convenience method to facilitate equals comparisons of datasets.
	 * 
	 * @param pkey -- String name of the primary key requested
	 * @return true if DatasetView contains the primary key, false if not
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
		if (!containsFilterDescription(internalName))
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
		if (!containsFilterDescription(internalName)) {
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
	 * Returns an AttributeGroup object for a specific AttributeDescription
	 * based on its internalName.
	 * 
	 * @param internalName - String internalName of Attribute Description for which a group is requested.
	 * @return FilterGroup for Attrribute Description provided, or null
	 */
	public AttributeGroup getGroupForAttribute(String internalName) {
		if (!containsFilterDescription(internalName))
			return null;
		else if (lastAttGroup == null) {
			lastAttGroup = getPageForAttribute(internalName).getGroupForAttributeDescription(internalName);
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
	 * Returns an AttributeCollection object for a specific AttributeDescription
	 * based on its internalName.
	 * 
	 * @param internalName - String internalName of Attribute Description for which a collection is requested.
	 * @return AttributeCollection for Attribute Description provided, or null
	 */
	public AttributeCollection getCollectionForAttribute(String internalName) {
		if (!containsAttributeDescription(internalName)) {
			return null;
		} else if (lastFiltColl == null) {
			lastAttColl = getGroupForAttribute(internalName).getCollectionForAttributeDescription(internalName);
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
	 * Returns a List of potential AttributeDescription.internalName to the MartCompleter command completion system.
	 * @returns List of internalNames
	 */
	public List getAttributeCompleterNames() {
		List names = new ArrayList();

		for (Iterator iter = attributePages.values().iterator(); iter.hasNext();) {
			AttributePage page = (AttributePage) iter.next();
			List pagenames = page.getCompleterNames();
			for (int i = 0, n = pagenames.size(); i < n; i++) {
				String name = (String) pagenames.get(i);
				if (!names.contains(name))
					names.add(name);
			}
		}
		return names;
	}

	/**
	 * Returns a List of potential FilterDescription.internalName to the MartCompleter command completion system.
	 * @returns List of internalNames
	 */
	public List getFilterCompleterNames() {
		List names = new ArrayList();

		for (Iterator iter = filterPages.values().iterator(); iter.hasNext();) {
			FilterPage page = (FilterPage) iter.next();
			List pagenames = page.getCompleterNames();
			for (int i = 0, n = pagenames.size(); i < n; i++) {
				String name = (String) pagenames.get(i);
				if (!names.contains(name))
					names.add(name);
			}
		}
		return names;
	}

	/**
	 * Allows MartShell to get all values associated with a given internalName (which may be of form x.y).
	 * Behaves differently than getFilterDescriptionByInternalName when internalName is x.y and y is the name of
	 * an actual filterDescription.
	 * @param internalName
	 * @return List of values to complete
	 */
	public List getFilterCompleterValuesByInternalName(String internalName) {
		if (internalName.indexOf(".") > 0)
			return getFilterDescriptionByInternalName(internalName.split("\\.")[0]).getCompleterValues(internalName);
		else
			return getFilterDescriptionByInternalName(internalName).getCompleterValues(internalName);
	}

	/**
	 * Allows MartShell to get all qualifiers associated with a given internalName (which may be of form x.y).
	 * Behaves differently than getFilterDescriptionByInternalName when internalName is x.y and y is the name of
	 * an actual filterDescription.
	 * @param internalName
	 * @return List of qualifiers to complete
	 */
	public List getFilterCompleterQualifiersByInternalName(String internalName) {
		if (internalName.indexOf(".") > 0 && !(internalName.endsWith(".")) && containsFilterDescription(internalName.split("\\.")[1])) {
			String refname = internalName.split("\\.")[1];
			return getFilterDescriptionByInternalName(refname).getCompleterQualifiers(refname);
		} else
			return getFilterDescriptionByInternalName(internalName).getCompleterQualifiers(internalName);
	}

	/**
	 * Returns the DataSource for this DatasetView, or null.
	 * @return DataSource datasource
	 */
	public DataSource getDatasource() {
		return datasource;
	}

	/**
	 * Set the DataSource and user for this DatasetView.
	 * @param source -- DataSource
	 */
	public void setDatasource(DataSource source) {
		datasource = source;
	}

	/**
	 * Returns the String name of the java.security.MessageDigest algoritm used to generate
	 * the digest stored with setDigest;
	 * @return String name of algorithm
	 */
	public String getDigestAlgorithm() {
		return digestAlgorithm;
	}

	/**
	 * Returns a digest suitable for comparison with a digest computed on another version
	 * of the XML underlying this DatasetView. 
	 * @return byte[] digest
	 */
	public byte[] getMessageDigest() {
		return digest;
	}

	/**
	 * Set a Message Digest for the DatasetView.  This must be a digest
	 * generated by a java.security.MessageDigest object with the given algorithmName
	 * method. 
	 * @param algorithmName -- String name of the java.security.MessageDigest used to calculate the digest 
	 * @param bs - byte[] digest computed
	 */
	public void setMessageDigest(String algorithmName, byte[] bs) {
		digestAlgorithm = algorithmName;
		digest = bs;
	}

	/**
	 * set the DSViewAdaptor used to instantiate a particular DatasetView object.
	 * @param dsva -- DSViewAdaptor implimenting object.
	 */
	public void setDSViewAdaptor(DSViewAdaptor dsva) {
		adaptor = dsva;
	}

	/**
	 * Get the DSViewAdaptor implimenting object used to instantiate this DatasetView object.
	 * @return DSViewAdaptor used to instantiate this DatasetView
	 */
	public DSViewAdaptor getDSViewAdaptor() {
		return adaptor;
	}

	private void lazyLoad() throws ConfigurationException {
		if (filterPages.size() == 0 && attributePages.size() == 0) {
			if (adaptor == null)
				throw new ConfigurationException("DatasetView objects must be provided a DSViewAdaptor to facilitate lazyLoading\n");
			adaptor.lazyLoad(this);
		}
	}

	/**
	 * Provides output useful for debugging purposes.  If the underlying lazy load fails, the toString
   * output will reflect this.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		try {
			lazyLoad();
		} catch (ConfigurationException e) {
			buf.append("Could not lazyLoad DatasetView object\n" + e.getMessage() + "\n");
			return buf.toString();
		}

		buf.append("[");
		buf.append(super.toString());
		buf.append(", starnames=").append(starBases);
		buf.append(", primarykeys=").append(primaryKeys);
		buf.append(", filterPages=").append(filterPages);
		buf.append(", attributePages=").append(attributePages);
		buf.append(", datasource=").append(datasource);
		buf.append(", digestAlgorithm=").append(digestAlgorithm);
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of DatasetView objects.
	 * Note, currently does not use Message Digest information.
   * Also, If the lazy load fails, a RuntimeException is thrown.
	 */
	public boolean equals(Object o) {
		return o instanceof DatasetView && hashCode() == ((DatasetView) o).hashCode();
	}

	/**
	 * hashCode for DatasetView
	 * Note, currently does not compare digest data, even if present.
	 * Also Note that this method calls the underlying lazyLoad() function, using the lazyLoad(DatasetView dsv) method from the DSViewAdaptor implimenting Object
	 * that it was instantiated with.  Depending upon the DSViewAdaptor implimentation being used, if DatasetView objects are stored in Hash based collections immediately
	 * upon instantiation, this could remove the speed optimization that the lazy loading system is designed to provide.  Also, if any Exceptions are encountered by
	 * a particular implimenation during lazyLoad, this will lead to a RuntimeException when hashCode is called.
	 */
	public int hashCode() {
		try {
			lazyLoad();
		} catch (ConfigurationException e) {
			throw new RuntimeException("Could not lazyLoad DatasetView in call to hashCode\n" + e.getMessage() + "\n", e);
		}

		int tmp = super.hashCode();

		if (datasource != null)
			tmp = (31 * tmp) + datasource.hashCode();

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

	private DSViewAdaptor adaptor = null;
	private DataSource datasource = null;
	private byte[] digest = null;
	private String digestAlgorithm = null;

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

	// cache one AttributeDescription for call to containsAttributeDescription or getAttributeDescriptionByInternalName
	private AttributeDescription lastAtt = null;

	//cache one AttributeDescription for call to supportsAttributeDescription/getAttributeDescriptionByFieldNameTableConstraint
	private AttributeDescription lastSupportingAttribute = null;

	//cache one FilterDescription Object for call to containsFilterDescription or getFiterDescriptionByInternalName
	private FilterDescription lastFilt = null;

	//cache one FilterGroup for call to getGroupForFilter
	private FilterGroup lastFiltGroup = null;

	//cache one FilterCollection for call to getCollectionForFilter
	private FilterCollection lastFiltColl = null;

	//cache one AttributeGroup for call to getGroupForAttribute
	private AttributeGroup lastAttGroup = null;

	//cache one AttributeCollection for call to getCollectionForAttribute
	private AttributeCollection lastAttColl = null;

	//cache one FilterDescription for call to supports/getFilterDescriptionByFieldNameTableConstraint
	private FilterDescription lastSupportingFilter = null;
}
