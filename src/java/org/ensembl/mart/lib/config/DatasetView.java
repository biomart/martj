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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a view of a mart dataset where a dataset is one or more main tables plus zero 
 * or more dimension tables.
 * 
 * <p>A DatasetView specifies the dataset and a description of the attributes 
 * and filters it contains. These attributes and filters are grouped hierarchically:
 * <b>Page -> Group -> Collection -> Description</b>. It also currently contains 
 * information about the
 * the primary key(s) used in joining. This will be removed in future versions.</p> 
 * 
 * <p>DatasetView Objects support a lazy load optimization strategy.
 * They can be instantiated with a miniumum of information (internalName), and lazy loaded when
 * the rest of the information is needed.  Any call to a get method will cause the object to attempt
 * to lazy load.  Lazy loading is only attempted when there are no FilterPage or AttributePage objects
 * loaded into the DatasetView.  A failed attempt to lazy load throws a RuntimeException.
 * Note that any call to toString, equals, and hashCode will cause lazy loading to occur, 
 * which can lead to some issues (see the documentation for each of these methods below).</p>
 *   
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatasetView extends BaseNamedConfigurationObject {

  private final String datasetKey = "dataset";
  private DSViewAdaptor adaptor = null;
  private byte[] digest = null;

  private List attributePages = new ArrayList();
  private boolean hasBrokenAttributePages = false;

  private List filterPages = new ArrayList();
  private boolean hasBrokenFilterPages = false;

  private Hashtable attributePageNameMap = new Hashtable();
  private Hashtable filterPageNameMap = new Hashtable();
  private List starBases = new ArrayList();
  private boolean hasBrokenStarBases = false;

  private List primaryKeys = new ArrayList();
  private boolean hasBrokenPrimaryKeys = false;

  private List defaultFilters = new ArrayList();
  private boolean hasDefaultFilters = false;
  private boolean hasBrokenDefaultFilters = false;

  private List uiOptions = new ArrayList();
  private Hashtable uiOptionNameMap = new Hashtable();
  private boolean hasOptions = false;
  private boolean hasBrokenOptions = false;

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

  private Logger logger = Logger.getLogger(DatasetView.class.getName());
  
  /**
   * Copy Constructor allowing client to specify whether to lazyLoad the copy at initiation, rather
   * than defering to a call to getXXX.
   * @param ds -- DatasetView to copy
   * @param lazyLoad - boolean, if true, copy will lazyLoad, if false, copy will defer lazyLoading until a getXXX method is called.
   */
  public DatasetView(DatasetView ds, boolean lazyLoad) {
    super(ds);

    setDataset(ds.getDataset());
    byte[] digest = ds.getMessageDigest();
    if (digest != null)
      setMessageDigest(digest);

    //if the DatasetView has an underlying DSViewAdaptor implementing Object, this can be substituted for the
    //actual element content, and defer the loading of this content to the lazyLoad system.  This requires that
    //all DSViewAdaptor implementing objects either implement a lazyLoad method and insert themselves into every
    //DatasetView that they manage, or, in the absence of a sensible lazyLoad method, ensure that all content is
    //is loaded, and __NOT__ insert themselves into the DatasetView that they manage.
    if (ds.getAdaptor() == null) {

      addStarBases(ds.getStarBases());
      addPrimaryKeys(ds.getPrimaryKeys());

      DefaultFilter[] dfilts = ds.getDefaultFilters();
      for (int i = 0, n = dfilts.length; i < n; i++) {
        addDefaultFilter(new DefaultFilter(dfilts[i]));
      }

      Option[] os = ds.getOptions();
      for (int i = 0, n = os.length; i < n; i++) {
        addOption(new Option(os[i]));
      }

      AttributePage[] apages = ds.getAttributePages();
      for (int i = 0, n = apages.length; i < n; i++) {
        addAttributePage(new AttributePage(apages[i]));
      }

      FilterPage[] fpages = ds.getFilterPages();
      for (int i = 0, n = fpages.length; i < n; i++) {
        addFilterPage(new FilterPage(fpages[i]));
      }
    } else
      setDSViewAdaptor(ds.getAdaptor());
    
    if (lazyLoad)
      lazyLoad();
  }
  
  /**
   * Copy Constructor. Creates a new copy of an existing DatasetView object.
   * Note, if the existing DatasetView object has a DatasetViewAdaptor set to it, 
   * this is transferred instead of the actual filterPages and attributePages, deferring
   * the population of these objects to the lazyLoad method called when a getXXX method is
   * called.
   * @param ds DatasetView to copy
   */
  public DatasetView(DatasetView ds) {
    this(ds, false);
  }

  /**
   * Empty constructor.  Should really only be used by the DatasetViewEditor
   */
  public DatasetView() {
    super();

    setAttribute(datasetKey, null);
  }

  /**
   * Constructs a DatasetView named by internalName and displayName.
   *  internalName is a single word that references this dataset, used to get the dataset from the MartConfiguration by name.
   *  displayName is the String to display in any UI.
   * 
   * @param internalName String name to represent this DatasetView
   * @param displayName String name to display.
   * @param dataset String prefix for all tables in the Mart Database for this Dataset. Must not be null
   */
  public DatasetView(String internalName, String displayName, String dataset) throws ConfigurationException {
    this(internalName, displayName, dataset, "");
  }

  /**
   * Constructs a DatasetView named by internalName and displayName, with a description of
   *  the dataset.
   * 
   * @param internalName String name to represent this DatasetView. Must not be null
   * @param displayName String name to display in an UI.
   * @param dataset String prefix for all tables in the Mart Database for this Dataset. Must not be null
   * @param description String description of the DatasetView.
   * @throws ConfigurationException if required values are null.
   */
  public DatasetView(String internalName, String displayName, String dataset, String description)
    throws ConfigurationException {
    super(internalName, displayName, description);

    if (dataset == null)
      throw new ConfigurationException("DatasetView objects must contain a dataset\n");
    setAttribute(datasetKey, dataset);
  }

  /**
   * Sets the dataset for this DatasetView object
   * @param dataset -- Dataset that this view represnets.
   */
  public void setDataset(String dataset) {
    setAttribute(datasetKey, dataset);
  }

  /**
   * @return the prefix for the mart database tables in this Dataset
   */
  public String getDataset() {
    return attributes.getProperty(datasetKey);
  }

  /**
   * add a Option object to this DatasetView.  Options are stored in the order that they are added.
   * @param o - an Option object
   */
  public void addOption(Option o) {
    uiOptions.add(o);
    uiOptionNameMap.put(o.getInternalName(), o);
    hasOptions = true;
  }

  /**
   * Remove a Option objectfrom this DatasetView.  Maintains order of other objects as they were added.
   * @param o - An option to remove.
   */
  public void removeOption(Option o) {
	lazyLoad();
    uiOptionNameMap.remove(o.getInternalName());
    uiOptions.remove(o);
    if (uiOptions.size() < 1)
      hasOptions = false;
  }

  /**
   * Insert an Option at a specific position within the option list.  Options
   * at or after this position are shifted right.
   * @param position - position within the list of options in the DatasetView.
   * @param o - Option to be inserted
   */
  public void insertOption(int position, Option o) {
	lazyLoad();
    uiOptionNameMap.put(o.getInternalName(), o);
    uiOptions.add(position, o);
    hasOptions = true;
  }

  /**
   * Insert an Option before an existing Option, defined by internalName.
   * @param internalName -- name of the Option before which the new option should be inserted.
   * @param o -- Option to be inserted
   * @throws ConfigurationException if the DatasetView does not contain the Option named by the given internalName.
   */
  public void insertOptionBeforeOption(String internalName, Option o) throws ConfigurationException {
	lazyLoad();
    if (!uiOptionNameMap.containsKey(internalName))
      throw new ConfigurationException("DatasetView does not contain Option " + internalName + "\n");

    insertOption(uiOptions.indexOf(uiOptionNameMap.get(internalName)), o);
  }

  /**
   * Insert an Option after an existing Option, defined by internalName.
   * @param internalName -- name of the Option after which the new option should be inserted.
   * @param o -- Option to be inserted
   * @throws ConfigurationException if the DatasetView does not contain the Option named by the given internalName.
   */
  public void insertOptionAfterOption(String internalName, Option o) throws ConfigurationException {
	lazyLoad();
    if (!uiOptionNameMap.containsKey(internalName))
      throw new ConfigurationException("DatasetView does not contain Option " + internalName + "\n");

    insertOption(uiOptions.indexOf(uiOptionNameMap.get(internalName)) + 1, o);
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
   * Add a DefaultFilter object to this DatasetView.
   * @param df - A DefaultFilter object
   */
  public void addDefaultFilter(DefaultFilter df) {
    hasDefaultFilters = true;
    if (!defaultFilters.contains(df))
      defaultFilters.add(df);
  }

  public void removeDefaultFilter(DefaultFilter df) {
	lazyLoad();
    defaultFilters.remove(df);
    if (defaultFilters.size() < 1)
      hasDefaultFilters = false;
  }

  /**
   * Add a set of DefaultFilter objects in one call.
   * Note, subsequent calls to addDefaultFilter or addDefaultFilters
   * will add to what was added before.
   * @param df - An Array of DefaultFilter objects
   */
  public void addDefaultFilters(DefaultFilter[] df) {
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

  public void removeStarBase(String starname) {
	lazyLoad();
    starBases.remove(starname);
  }

  /**
   * Add a group of star names for a DatasetView with one call.
   * Note, subsequent calls to addStarBases or addStarBase will add
   * starBases to what has been added before.
   * 
   * @param starnames String[] Array of star names.
   */
  public void addStarBases(String[] starnames) {
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

  public void removePrimaryKey(String primaryKey) {
	lazyLoad();
    primaryKeys.remove(primaryKey);
  }

  /**
   * Add a group of primary keys at once.
   * Note, subsequent calls to addPrimaryKey or addPrimaryKeys
   * will add primary keys to what has been added before.
   * 
   * @param pkeys String[] array of primary keys.
   */
  public void addPrimaryKeys(String[] pkeys) {
    primaryKeys.addAll(Arrays.asList(pkeys));
  }

  /**
   * Add an AttributePage to the DatasetView.
   * 
   * @param a -- AttributePage to be added.
   */
  public void addAttributePage(AttributePage a) {
    attributePages.add(a);
    attributePageNameMap.put(a.getInternalName(), a);
  }

  /**
   * Remove an AttributePage from the DatasetView.
   * @param a -- AttributePage to be removed.
   */
  public void removeAttributePage(AttributePage a) {
	lazyLoad();
    attributePageNameMap.remove(a.getInternalName());
    attributePages.remove(a);
  }

  /**
   * Insert an AttributePage at a particular Position within the List
   * of AttributePages contained in the DatasetView. AttributePages at
   * or after the given position are shifted right.
   * @param position -- position to insert the AttributePage.
   * @param a -- AttributePage to be inserted.
   */
  public void insertAttributePage(int position, AttributePage a) {
	lazyLoad();
  	System.out.println("PAGES\t" + attributePages.size());
    attributePages.add(position, a);
    attributePageNameMap.put(a.getInternalName(), a);
  }

  /**
   * Insert an AttributePage before another AttributePage, named by internalName.
   * @param internalName -- internalName of the AttributePage before which the given AttributePage should be inserted.
   * @param a -- AttributePage to be inserted.
   * @throws ConfigurationException when the DatasetView does not contain an AttributePage named by the given internalName.
   */
  public void insertAttributePageBeforeAttributePage(String internalName, AttributePage a)
    throws ConfigurationException {
    lazyLoad();
    if (!attributePageNameMap.containsKey(internalName))
      throw new ConfigurationException("This DatasetView does not contain AttributePage " + internalName + "\n");

    insertAttributePage(attributePages.indexOf(attributePageNameMap.get(internalName)), a);
  }

  /**
   * Insert an AttributePage after another AttributePage, named by internalName.
   * @param internalName -- internalName of the AttributePage after which the given AttributePage should be inserted.
   * @param a -- AttributePage to be inserted.
   * @throws ConfigurationException when the DatasetView does not contain an AttributePage named by the given internalName.
   */
  public void insertAttributePageAfterAttributePage(String internalName, AttributePage a) throws ConfigurationException {
	lazyLoad();
    if (!attributePageNameMap.containsKey(internalName))
      throw new ConfigurationException("This DatasetView does not contain AttributePage " + internalName + "\n");

    insertAttributePage(attributePages.indexOf(attributePageNameMap.get(internalName)) + 1, a);
  }

  /**
   * Add a group of AttributePage objects at once.
   * Note, subsequent calls to addAttributePage or
   * addAttributePages will add to what has been added before.
   * 
   * @param a AttributePage[] array of AttributePages.
   */
  public void addAttributePages(AttributePage[] a) {
    for (int i = 0; i < a.length; i++) {
      attributePages.add(a[i]);
      attributePageNameMap.put(a[i].getInternalName(), a[i]);
    }
  }

  /**
   * Add a FilterPage to the DatasetView.
   * 
   * @param f FiterPage object.
   */
  public void addFilterPage(FilterPage f) {
    filterPages.add(f);
    filterPageNameMap.put(f.getInternalName(), f);
  }

  /**
   * Remove a FilterPage from the DatasetView.
   * @param f -- FilterPage to be removed.
   */
  public void removeFilterPage(FilterPage f) {
	lazyLoad();
    filterPageNameMap.remove(f.getInternalName());
    filterPages.remove(f);
  }

  /**
   * Insert a FilterPage at a specific Position within the FilterPage list.
   * FilterPages at or after the given position will be shifted right).
   * @param position -- Position to insert the FilterPage
   * @param f -- FilterPage to insert.
   */
  public void insertFilterPage(int position, FilterPage f) {
	lazyLoad();
    filterPages.add(position, f);
    filterPageNameMap.put(f.getInternalName(), f);
  }

  /**
   * Insert a FilterPage before a specified FilterPage, named by internalName.
   * @param internalName -- name of the FilterPage before which the given FilterPage should be inserted.
   * @param f -- FilterPage to be inserted.
   * @throws ConfigurationException when the DatasetView does not contain a FilterPage named by internalName.
   */
  public void insertFilterPageBeforeFilterPage(String internalName, FilterPage f) throws ConfigurationException {
	lazyLoad();
    if (!filterPageNameMap.containsKey(internalName))
      throw new ConfigurationException("DatasetView does not contain FilterPage " + internalName + "\n");
    insertFilterPage(filterPages.indexOf(filterPageNameMap.get(internalName)), f);
  }

  /**
   * Insert a FilterPage after a specified FilterPage, named by internalName.
   * @param internalName -- name of the FilterPage after which the given FilterPage should be inserted.
   * @param f -- FilterPage to be inserted.
   * @throws ConfigurationException when the DatasetView does not contain a FilterPage named by internalName.
   */
  public void insertFilterPageAfterFilterPage(String internalName, FilterPage f) throws ConfigurationException {
	lazyLoad();
    if (!filterPageNameMap.containsKey(internalName))
      throw new ConfigurationException("DatasetView does not contain FilterPage " + internalName + "\n");
    insertFilterPage(filterPages.indexOf(filterPageNameMap.get(internalName)) + 1, f);
  }

  /**
   * Add a group of FilterPage objects in one call.
   * Note, subsequent calls to addFilterPage or addFilterPages
   * will add to what has been added before.
   * 
   * @param f FilterPage[] array of FilterPage objects.
   */
  public void addFilterPages(FilterPage[] f) {
    for (int i = 0, n = f.length; i < n; i++) {
      filterPages.add(f[i]);
      filterPageNameMap.put(f[i].getInternalName(), f);
    }
  }

  /**
   * Determine if this DatasetView has Options Available.
   * 
   * @return boolean, true if Options are available, false if not.
   */
  public boolean hasOptions() {
    lazyLoad();
    return hasOptions;
  }

  /**
   * Get all Option objects available as an array.  Options are returned in the order they were added.
   * @return Option[]
   */
  public Option[] getOptions() {
    lazyLoad();
    Option[] ret = new Option[uiOptions.size()];
    uiOptions.toArray(ret);
    return ret;
  }

  /**
   * Determine if this DatasetView has DefaultFilters available.
   * @return boolean, true if DefaultFilter(s) are available, false if not
   */
  public boolean hasDefaultFilters() {
    lazyLoad();
    return hasDefaultFilters;
  }

  /**
   * Returns all DefaultFilter Objects added to the DatasetView.
   * @return DefaultFilter[] array of DefaultFilter objects.
   */
  public DefaultFilter[] getDefaultFilters() {
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
    AttributePage[] as = new AttributePage[attributePages.size()];
    attributePages.toArray(as);
    return as;
  }

  /**
   * Returns a particular AttributePage named by a given displayName.
   * 
   * @param displayName String name of a particular AttributePage
   * @return AttributePage object named by the given displayName, or null.
   */
  public AttributePage getAttributePageByInternalName(String internalName) {
    lazyLoad();
    if (attributePageNameMap.containsKey(internalName))
      return (AttributePage) attributePageNameMap.get(internalName);
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
    lazyLoad();
    return attributePageNameMap.containsKey(internalName);
  }

  /**
   * Returns a list of all FilterPage objects contained within the DatasetView, in the order they were added.
   * @return FilterPage[]
   */
  public FilterPage[] getFilterPages() {
    lazyLoad();
    FilterPage[] fs = new FilterPage[filterPages.size()];
    filterPages.toArray(fs);
    return fs;
  }

  /**
   * Returns a particular FilterPage object named by a given displayName.
   * 
   * @param displayName String name of a particular FilterPage
   * @return FilterPage object named by the given displayName, or null
   */
  public FilterPage getFilterPageByName(String internalName) {
    lazyLoad();
    if (filterPageNameMap.containsKey(internalName))
      return (FilterPage) filterPageNameMap.get(internalName);
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
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
    boolean found = false;

    if (lastAtt == null) {
      for (Iterator iter = (Iterator) attributePages.iterator(); iter.hasNext();) {
        AttributePage page = (AttributePage) iter.next();
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
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
    boolean supports = false;

    for (Iterator iter = attributePages.iterator(); iter.hasNext();) {
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
    lazyLoad();
    boolean contains = false;

    if (lastFilt == null) {
      if ((internalName.indexOf(".") > 0) && !(internalName.endsWith("."))) {
        String[] refs = internalName.split("\\.");

        if (refs.length > 1 && containsFilterDescription(refs[1]))
          contains = true;
      }

      if (!contains) {
        for (Iterator iter = (Iterator) filterPages.iterator(); iter.hasNext();) {
          FilterPage page = (FilterPage) iter.next();
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
      else if (
        (internalName.indexOf(".") > 0)
          && !(internalName.endsWith("."))
          && lastFilt.getInternalName().equals(internalName.split("\\.")[1]))
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
    lazyLoad();
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
    lazyLoad();
    boolean supports = false;

    if (lastSupportingFilter == null) {
      for (Iterator iter = filterPages.iterator(); iter.hasNext();) {
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
    lazyLoad();
    for (Iterator iter = (Iterator) filterPages.iterator(); iter.hasNext();) {
      FilterPage page = (FilterPage) iter.next();

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
    lazyLoad();
    List pages = new ArrayList();

    for (Iterator iter = (Iterator) filterPages.iterator(); iter.hasNext();) {
      FilterPage page = (FilterPage) iter.next();

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
    lazyLoad();
    for (Iterator iter = (Iterator) attributePages.iterator(); iter.hasNext();) {
      AttributePage page = (AttributePage) iter.next();

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
    lazyLoad();
    List pages = new ArrayList();

    for (Iterator iter = (Iterator) attributePages.iterator(); iter.hasNext();) {
      AttributePage page = (AttributePage) iter.next();

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
    lazyLoad();
    List filts = new ArrayList();

    for (Iterator iter = filterPages.iterator(); iter.hasNext();) {
      Object fpo = iter.next();

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
    lazyLoad();
    List atts = new ArrayList();

    for (Iterator iter = attributePages.iterator(); iter.hasNext();) {
      Object apo = iter.next();

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
    lazyLoad();
    return starBases.contains(starBase);
  }

  /**
   * Convenience method to facilitate equals comparisons of datasets.
   * 
   * @param pkey -- String name of the primary key requested
   * @return true if DatasetView contains the primary key, false if not
   */
  public boolean containsPrimaryKey(String pkey) {
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
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
    lazyLoad();
    List names = new ArrayList();

    for (Iterator iter = attributePages.iterator(); iter.hasNext();) {
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
    lazyLoad();
    List names = new ArrayList();

    for (Iterator iter = filterPages.iterator(); iter.hasNext();) {
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
    lazyLoad();
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
    lazyLoad();
    if (internalName.indexOf(".") > 0
      && !(internalName.endsWith("."))
      && containsFilterDescription(internalName.split("\\.")[1])) {
      String refname = internalName.split("\\.")[1];
      return getFilterDescriptionByInternalName(refname).getCompleterQualifiers(refname);
    } else
      return getFilterDescriptionByInternalName(internalName).getCompleterQualifiers(internalName);
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
   * @param bs - byte[] digest computed
   */
  public void setMessageDigest(byte[] bs) {
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

  private void lazyLoad() {
    if (filterPages.size() == 0 && attributePages.size() == 0) {
      if (adaptor == null)
        throw new RuntimeException("DatasetView objects must be provided a DSViewAdaptor to facilitate lazyLoading\n");
      try {
        if (logger.isLoggable(Level.INFO))
          logger.info("LAZYLOAD\n");

        adaptor.lazyLoad(this);
      } catch (ConfigurationException e) {
        throw new RuntimeException("Could not lazyload datasetview " + e.getMessage(), e);
      }
    }
  }

  /**
   * Provides output useful for debugging purposes.  Calls lazyload, so could throw a RuntimeException
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();

    lazyLoad();

    buf.append("[");
    buf.append(super.toString());
    buf.append(", starnames=").append(starBases);
    buf.append(", primarykeys=").append(primaryKeys);
    buf.append(", options=").append(uiOptions);
    buf.append(", defaultFilters=").append(defaultFilters);
    buf.append(", filterPages=").append(filterPages);
    buf.append(", attributePages=").append(attributePages);
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
    lazyLoad();

    int tmp = super.hashCode();

    for (int i = 0, n = starBases.size(); i < n; i++) {
      String element = (String) starBases.get(i);
      tmp = (31 * tmp) + element.hashCode();
    }

    for (int i = 0, n = primaryKeys.size(); i < n; i++) {
      String element = (String) primaryKeys.get(i);
      tmp = (31 * tmp) + element.hashCode();
    }

    for (int i = 0, n = uiOptions.size(); i < n; i++) {
      Option element = (Option) uiOptions.get(i);
      tmp = (31 * tmp) + element.hashCode();
    }

    for (int i = 0, n = defaultFilters.size(); i < n; i++) {
      DefaultFilter element = (DefaultFilter) defaultFilters.get(i);
      tmp = (31 * tmp) + element.hashCode();
    }

    for (Iterator iter = filterPages.iterator(); iter.hasNext();) {
      FilterPage element = (FilterPage) iter.next();
      tmp = (31 * tmp) + element.hashCode();
    }

    for (Iterator iter = attributePages.iterator(); iter.hasNext();) {
      AttributePage element = (AttributePage) iter.next();
      tmp = (31 * tmp) + element.hashCode();
    }

    return tmp;
  }

  /**
   * Set the hasBrokenStarBases flag to true, meaning one or more starbases do not match an existing main table in a
   * particular Mart instance.
   */
  public void setStarsBroken() {
    hasBrokenStarBases = true;
  }

  /**
   * Determine if this DatasetView has broken StarBases.
   * @return boolean
   */
  public boolean hasBrokenStarBases() {
    return hasBrokenStarBases;
  }

  /**
   * Sets the hasBrokenPrimarykeys flag to true, meaning one more more primary keys are not valid with respect to a
   * particular Mart instance
   */
  public void setPrimaryKeysBroken() {
    hasBrokenPrimaryKeys = true;
  }

  /**
   * Determine if this DatasetView has broken primary keys.
   * @return boolean
   */
  public boolean hasBrokenPrimaryKeys() {
    return hasBrokenPrimaryKeys;
  }

  /**
   *  Sets the hasBrokenAttributePages flag to true, meaning one or more AttributePage Objects
   * contain broken AttributeDescriptions with respect to a particular Mart instance.
   */
  public void setAttributePagesBroken() {
    hasBrokenAttributePages = true;
  }

  public boolean hasBrokenAttributePages() {
    return hasBrokenAttributePages;
  }

  /**
   *  Sets the hasBrokenFilterPages flag to true, meaning one or more FilterPage Objects
   * contain broken FilterDescriptions with respect to a particular Mart instance.
   */
  public void setFilterPagesBroken() {
    hasBrokenFilterPages = true;
  }

  public boolean hasBrokenFilterPages() {
    return hasBrokenFilterPages;
  }

  /**
   * Sets the hasBrokenDefaultFilters flag to true, meaning one or more of this DatasetView Object's
   * DefaultFilters have broken FilterDescription Objects with respect to a particular Mart instance.
   */
  public void setDefaultFiltersBroken() {
    hasBrokenDefaultFilters = true;
  }

  /**
   * Determine if this DatasetView Object has broken DefaultFilter Objects.
   * @return boolean
   */
  public boolean hasBrokenDefaultFilters() {
    return hasBrokenDefaultFilters;
  }

  /**
   * Sets the hasBrokenOptions flag to true, meaning this DatasetView has one or more Option Objects which are broken with respect
   * to a particular Mart instance.
   */
  public void setOptionsBroken() {
    hasBrokenOptions = true;
  }

  /**
   * Determine if this DatasetView has broken Options.
   * @return boolean
   */
  public boolean hasBrokenOptions() {
    return hasBrokenOptions;
  }

  /**
   * True if hasBrokenStarBases or hasBrokenPrimaryKeys or hasBrokenDefaultFilters or hasBrokenOptions or hasBrokenAttributePages
   * or hasBrokenFilterPages is true.
   * @return boolean
   */
  public boolean isBroken() {
    return hasBrokenStarBases
      || hasBrokenPrimaryKeys
      || hasBrokenDefaultFilters
      || hasBrokenOptions
      || hasBrokenAttributePages
      || hasBrokenFilterPages;
  }

  /**
   * @return adaptor that created this instance, can be null.
   */
  public DSViewAdaptor getAdaptor() {
    return adaptor;

  }
}
