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

package org.ensembl.mart.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.util.StringUtil;

/**
 * A mart query object. Instances of this class specify all of the parameters
 * necessary for execution against a mart instance. Also any 
 * QueryChangeListeners, added with <code>addQueryChangeListener(listener)</code>,
 * are notified of changes in the query's state such as the addition of an attribute or remval of
 * a filter.
 * 
 * <p>If the log level is set to >= FINE then the query is written to the log
 * after it's state is changed but before calling the listeners.</p>
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @see Attribute
 * @see SequenceDescription 
 * @see Filter
 * @see QueryChangeListener
 * 
 */

public class Query {

  private final static Logger logger = Logger.getLogger(Query.class.getName());

  private List listeners = new ArrayList();

  /**
   * enums over query types
   * clients can set type using the constant
   * and test / get results as well
   */
  public final static int ATTRIBUTE = 1;
  public final static int SEQUENCE = 2;

  public Query() {
  }

  public Query(Query oq) {

    initialise(oq);
  }

  /**
   * Inititise this query by removing all current properties
   * and copying all the properties from oq.
   * 
   * @param oq
   */
  public void initialise(Query oq) {

    setDataset(oq.getDataset());
    setDataSource(oq.getDataSource());

    removeAllAttributes();
    if (oq.getAttributes().length > 0) {
      Attribute[] oatts = oq.getAttributes();
      //    TODO copy attribute and filter by value rather than reference
      for (int i = 0; i < oatts.length; ++i)
        addAttribute(oatts[i]);
    }

    removeAllFilters();
    if (oq.getFilters().length > 0) {
      Filter[] ofilts = oq.getFilters();
      //    TODO copy attribute and filter by value rather than reference
      for (int i = 0; i < ofilts.length; ++i)
        addFilter(ofilts[i]);
    }

    // TODO copy other querytypes?
    if (oq.querytype == Query.SEQUENCE)
      setSequenceDescription(
        new SequenceDescription(oq.getSequenceDescription()));

    if (oq.getStarBases().length > 0) {
      String[] oStars = oq.getStarBases();
      String[] nStars = new String[oStars.length];
      System.arraycopy(oStars, 0, nStars, 0, oStars.length);

      setStarBases(nStars);
    } else {
      setStarBases(null);
    }

    if (oq.getPrimaryKeys().length > 0) {
      String[] oPkeys = oq.getPrimaryKeys();
      String[] nPkeys = new String[oPkeys.length];
      System.arraycopy(oPkeys, 0, nPkeys, 0, oPkeys.length);

      setPrimaryKeys(nPkeys);
    } else {
      setPrimaryKeys(null);
    }

    limit = oq.getLimit();

  }

  /**
   * returns the query type (one of ATTRIBUTE or SEQUENCE)
   * @return int querytype
   */
  public int getType() {
    return querytype;
  }

  /**
   * test to determine if a specified attribute object is 
   * contained within the attribute list of the Query.
   * 
   * @param attribute
   * @return boolean
   */
  public boolean hasAttribute(Attribute attribute) {
    return attributes.contains(attribute);
  }

  /**
   * Adds attribute to the end of the attributes array. 
   * @param attribute item to be added.
   * @throws IllegalArgumentException if attribute is 
   * null or already added.
   */
  public void addAttribute(Attribute attribute) {
    addAttribute(attributes.size(), attribute);
  }

  /**
   * remove a Filter object from the list of Attributes
   * 
   * @param Filter filter
   */
  public void removeFilter(Filter filter) {
    int index = filters.indexOf(filter);
    if (index > -1) {
      filters.remove(index);
      log();
      for (int i = 0; i < listeners.size(); ++i)
        ((QueryChangeListener) listeners.get(i)).filterRemoved(
          this,
          index,
          filter);
    }

  }

  /**
   * get all Attributes as an Attribute :ist
   * 
   * @return Attribute[] attributes
   */
  public Attribute[] getAttributes() {
    Attribute[] a = new Attribute[attributes.size()];
    attributes.toArray(a);
    return a;
  }

  /**
   * get all Filter objects as a Filter[] Array
   * 
   * @return Filters[] filters
   */
  public Filter[] getFilters() {
    Filter[] f = new Filter[filters.size()];
    filters.toArray(f);
    return f;
  }

  /**
   * Allows the retrieval of a specific Filter object with a specified field name.
   * 
   * @param name - name of the fieldname for this Filter.
   * @return Filter object named by given field name.
   */
  public Filter getFilterByName(String name) {
    for (int i = 0, n = filters.size(); i < n; i++) {
      Filter element = (Filter) filters.get(i);
      if (element.getField().equals(name))
        return element;
    }
    return null;
  }

  /**
   * Add filter to end of filter list. 
   * 
   * @param Filter filter to be added.
   * @throws IllegalArgumentException if filter is 
   * null or already added.
   */
  public void addFilter(Filter filter) {
    addFilter(filters.size(), filter);
  }

  /**
   * Add filter to end of filter list. 
   * 
   * @param index position where to insert the filter in the filters array.
   * @param filter filter to be added
   * @throws IllegalArgumentException if attribute is 
   * null or already added.
   */
  public void addFilter(int index, Filter filter) {

    if (filter == null)
      throw new IllegalArgumentException("Can not add a null filter");
    if (filters.contains(filter))
      throw new IllegalArgumentException("Filter already present: " + filter);

    filters.add(index, filter);
    log();
    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).filterAdded(this, index, filter);
  }

  /**
   * Remove Attribute if present, otherwise do nothing.
   * 
   * @param Attribute attribute to be removed.
   */
  public void removeAttribute(Attribute attribute) {
    int index = attributes.indexOf(attribute);
    if (index > -1) {
      attributes.remove(index);
      log();
      for (int i = 0; i < listeners.size(); ++i)
        ((QueryChangeListener) listeners.get(i)).attributeRemoved(
          this,
          index,
          attribute);
    }
  }

  /**
   * Sets a SequenceDescription to the Query, and sets querytype = SEQUENCE. 
  * @param s A SequenceDescription object.
  */
  public void setSequenceDescription(SequenceDescription s) {
    SequenceDescription oldSequenceDescription = this.sequenceDescription;
    this.sequenceDescription = s;
    this.querytype = Query.SEQUENCE;

    log();

    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).sequenceDescriptionChanged(
        this,
        oldSequenceDescription,
        this.sequenceDescription);

  }

  /**
   * returns the SequenceDescription for this Query.
  * @return SequenceDescription
  */
  public SequenceDescription getSequenceDescription() {
    return sequenceDescription;
  }

  /**
   * get the primaryKeys of the Query
   * @return String primaryKeys
   */
  public String[] getPrimaryKeys() {
    return primaryKeys;
  }

  /**
   * set the primaryKeys for the Query
   * @param String primaryKeys
   */
  public void setPrimaryKeys(String[] primaryKeys) {
    String[] old = this.primaryKeys;
    this.primaryKeys = primaryKeys;
    log();
    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).primaryKeysChanged(
        this,
        old,
        this.primaryKeys);
  }

  /**
   * get the starBases for the Query
   * @return String starBases
   */
  public String[] getStarBases() {
    return starBases;
  }

  /**
   * set the starBases for the Query
   * @param String starBases
   */
  public void setStarBases(String[] starBases) {
    String[] old = this.starBases;
    this.starBases = starBases;

    log();

    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).starBasesChanged(
        this,
        old,
        this.starBases);
  }

  /**
   * Set a limit for the Query.
   * @param inlimit - int limit to add to the Query
   */
  public void setLimit(int inlimit) {
    if (inlimit > 0) {
      int old = this.limit;
      this.limit = inlimit;
      log();
      for (int i = 0; i < listeners.size(); ++i)
        ((QueryChangeListener) listeners.get(i)).limitChanged(
          this,
          old,
          this.limit);
    }
  }
  /**
   * Determine if the Query has a limit > 0.
   * @return true if limit > 0, false if not
   */
  public boolean hasLimit() {
    return (limit > 0);
  }
  /**
   * Returns the limit for the Query. limit == 0 means no limit
   * @return limit
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Returns the total number of filters of all types added to the Query
   * @return int count of all filters added
   */
  public int getTotalFilterCount() {
    return filters.size();
  }

  /**
   * returns a description of the Query for logging purposes
   * 
   * @return String description (primaryKeys=primaryKeys\nstarBases=starBases\nattributes=attributes\nfilters=filters)
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[");
    buf.append(" dataset=").append(dataset);
    buf.append(", dataSource=").append(dataSource);
    buf.append(", starBases=[").append(StringUtil.toString(starBases));
    buf.append("], primaryKeys=[").append(StringUtil.toString(primaryKeys));
    buf.append("], querytype=").append(querytype);
    buf.append(", attributes=").append(attributes);
    buf.append(", filters=").append(filters);

    if (sequenceDescription != null)
      buf.append(", sequencedescription=").append(sequenceDescription);

    buf.append(", limit=").append(limit);
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons manipulation of Query objects
   * Mainly for testing of copy constructor
   */
  public boolean equals(Object o) {
    return o instanceof Query && hashCode() == ((Query) o).hashCode();
  }

  public int hashCode() {
    int tmp = 0;
    if (querytype == Query.SEQUENCE) {
      tmp = (31 * tmp) + sequenceDescription.hashCode();
      tmp = (31 * tmp) + querytype;
    }

    for (int i = 0, n = starBases.length; i < n; i++)
      tmp = (31 * tmp) + starBases[i].hashCode();

    for (int i = 0, n = primaryKeys.length; i < n; i++)
      tmp = (31 * tmp) + primaryKeys[i].hashCode();

    for (int i = 0, n = attributes.size(); i < n; i++) {
      FieldAttribute element = (FieldAttribute) attributes.get(i);
      tmp = (31 * tmp) + element.hashCode();
    }

    for (int i = 0, n = filters.size(); i < n; i++)
      tmp = (31 * tmp) + filters.get(i).hashCode();

    tmp *= 31;
    if (datasetView != null)
      tmp += datasetView.hashCode();

    tmp *= 31;
    if (queryName != null)
      tmp += queryName.hashCode();

    return tmp;
  }

  private List attributes = new Vector();
  private List filters = new Vector();

  private String queryName;
  private int querytype = Query.ATTRIBUTE;
  // default to ATTRIBUTE, over ride for SEQUENCE
  private SequenceDescription sequenceDescription;
  private String[] primaryKeys;
  private String[] starBases;
  private DatasetView datasetView;
  private int limit = 0; // add a limit clause to the SQL with an int > 0

  /**
   * Datasource this query applies to.
   */
  private DataSource dataSource;

  /**
   * Name of dataset this query applies to.
   */
  private String dataset;

  public synchronized QueryChangeListener[] getQueryChangeListeners() {
    return (QueryChangeListener[]) listeners.toArray(
      new QueryChangeListener[listeners.size()]);
  }

  public synchronized void removeQueryChangeListener(QueryChangeListener listener) {
    listeners.remove(listener);
  }

  /**
   * Convenience method that removes all attributes from the query. Notifies listeners.
   */
  public void removeAllAttributes() {

    Attribute[] attributes = getAttributes();

    for (int i = 0, n = attributes.length; i < n; i++) {
      removeAttribute(attributes[i]);
    }

  }

  /**
   * Removes all Filters from the query. Each removed Filter will
   * generate a separate property change event.
   */
  public void removeAllFilters() {

    Filter[] filters = getFilters();

    for (int i = 0, n = filters.length; i < n; i++) {
      removeFilter(filters[i]);
    }

  }

  /**
   * Removes all QueryChangeListeners. Note: does not notify
   * listeners that they have been removed.
   */
  public void removeAllQueryChangeListeners() {
    listeners.clear();
  }

  /**
   * Replace the oldFilter with the new one. 
   * @param oldFilter
   * @param newFilter
   * @throws RuntimeException if oldFilter is not currently in the query.
   * TODO remove replaceFilter() + listener method.
   */
  public void replaceFilter(Filter oldFilter, Filter newFilter) {

    int index = filters.indexOf(oldFilter);
    if (index == -1)
      throw new IllegalArgumentException(
        "Old filter can not be removed because not in query: " + oldFilter);

    filters.remove(index);
    filters.add(index, newFilter);

    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).filterChanged(
        this,
        oldFilter,
        newFilter);

  }

  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * Sets the value and notifies listeners.
   * @param dataSource new dataSource.
   */
  public void setDataSource(DataSource dataSource) {
    DataSource oldDatasource = this.dataSource;
    this.dataSource = dataSource;
    log();
    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).datasourceChanged(
        this,
        oldDatasource,
        this.dataSource);
  }

  public String getDataset() {
    return dataset;
  }

  /**
   * Sets the value and propagates a PropertyChange event to listeners. The 
   * property name is in the event is "dataset". No event is propagated 
   * if the parameter is equal to the current dataset.
   * @param dataset new dataset.
   */
  public void setDataset(String datasetName) {

    if (this.dataset == datasetName
      || datasetName != null
      && datasetName.equals(this.dataset))
      return;

    String oldDatasetName = this.dataset;
    this.dataset = datasetName;

    log();

    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).datasetChanged(
        this,
        oldDatasetName,
        this.dataset);
  }

  /**
   * Returns the name that has been set for this Query, null if not set
   * @return String Query name
   */
  public String getQueryName() {
    return queryName;
  }

  /**
   * @param string -- String name to apply to this Query.
   */
  public void setQueryName(String queryName) {

    if (this.queryName!=null && this.queryName.equals(queryName))
      return;

    String old = this.queryName;
    this.queryName = queryName;

    log();

    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).queryNameChanged(
        this,
        old,
        this.queryName);
  }

  /**
   * @param listener
   */
  public void addQueryChangeListener(QueryChangeListener listener) {
    listeners.add(listener);
  }

  /**
   * Adds attribute at the specified index. 
   * @param index position in array to add attribute.
   * @param attribute item to be added to attributes array.
   * @throws IllegalArgumentException if attribute is 
   * null or already added.
   */
  public void addAttribute(int index, Attribute attribute) {

    if (attribute == null)
      throw new IllegalArgumentException("Can not add a null attribute");
    if (attributes.contains(attribute))
      throw new IllegalArgumentException(
        "attribute already present: " + attribute);

    attributes.add(index, attribute);
    log();
    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).attributeAdded(
        this,
        index,
        attribute);

  }

  /**
   * Cause the result of toString() to be printed to the log if 
   * logging level for this class is >= FINE. Useful for debugging
   * and test purposes. Called automatically by all the state changing
   * methods such as addFilter(...) and setDataset(...).
   */
  public void log() {
    if (logger.isLoggable(Level.FINE))
      logger.fine(this.toString());
  }

  /**
   * Unsets all property values.
   */
  public void clear() {

    setDataSource(null);
    setDataset(null);
    setDatasetView(null);

    removeAllAttributes();
    removeAllFilters();

    setLimit(0);

    setPrimaryKeys(null);
    setStarBases(null);
  }

  public DatasetView getDatasetView() {
    return datasetView;
  }

  public void setDatasetView(DatasetView datasetView) {
    DatasetView old = this.datasetView;
    this.datasetView = datasetView;
    log();
    for (int i = 0; i < listeners.size(); ++i)
      ((QueryChangeListener) listeners.get(i)).datasetViewChanged(
        this,
        old,
        datasetView);
  }

}
