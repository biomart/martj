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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

import org.ensembl.util.StringUtil;

import javax.sql.DataSource;
/**
 * Object for storing the parameters to construct a query against a Mart
 * database.  Parameters consist of at least one Attribute (a requested field
 * from the database) implimenting object or a SequenceDescription object. 
 * Parameters can include Filter implimenting objects, or DSFilter objects
 * to restrict the Query on user supplied conditions.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @see Attribute
 * @see SequenceDescription 
 * @see Filter
 * @see DSFilter
 */
public class Query {
  
	
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
  	if (oq.getAttributes().length > 0) {
      Attribute[] oatts = oq.getAttributes();
      Attribute[] natts = new Attribute[oatts.length];
      System.arraycopy(oatts, 0, natts, 0, oatts.length);
      setAttributes(natts);
  	}
    
    if (oq.getFilters().length > 0) {
      Filter[] ofilts = oq.getFilters();
      Filter[] nfilts = new Filter[ofilts.length];
      System.arraycopy(ofilts,0,nfilts,0,ofilts.length);
      setFilters(nfilts);
    }
    
    if (oq.querytype == Query.SEQUENCE)
      setSequenceDescription(new SequenceDescription(oq.getSequenceDescription()));
      
    if (oq.getStarBases().length > 0) {
    	String[] oStars = oq.getStarBases();
    	String[] nStars = new String[oStars.length];
    	System.arraycopy(oStars, 0, nStars, 0, oStars.length);
    	
    	setStarBases(nStars);
    }
    
    if (oq.getPrimaryKeys().length > 0) {
    	String[] oPkeys = oq.getPrimaryKeys();
    	String[] nPkeys = new String[oPkeys.length];
    	System.arraycopy(oPkeys, 0, nPkeys, 0, oPkeys.length);
    	
    	setPrimaryKeys(nPkeys);
    }
    
    if (oq.hasLimit())
      limit = oq.getLimit();

    setDatasetInternalName( oq.getDatasetInternalName() );
    setDataSource( oq.getDataSource() );

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
	 * add an Attribute object to the list of Attributes
	 * for the Query.
	 * 
	 * @param Attribute attribute
	 */
	public void addAttribute(Attribute attribute) {
		if (!attributes.contains(attribute)) {
			attributes.add(attribute);
      changeSupport.firePropertyChange("attribute", null, attribute);
    }
	}

	/**
	 * remove a Filter object from the list of Attributes
	 * 
	 * @param Filter filter
	 */
	public void removeFilter(Filter filter) {
		if (filters.contains(filter)) {
		
			filters.remove(filter);
      changeSupport.firePropertyChange("filter", filter, null);
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
	 * set an entire list of Attribute objects
	 * @param List attributes
	 */
	public void setAttributes(Attribute[] attributes) {
    Object old = this.attributes;
		this.attributes = new ArrayList(Arrays.asList(attributes));
    changeSupport.firePropertyChange("attributes", old, this.attributes);
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
	 * set an entire list of Filter objects.  Subsequent calls to setFilters will add to 
	 * what was added with previous addFilter/setFilters calls.  This method handles the 
	 * logic for collecting any non STRING type IDListFilter objects for later processing 
	 * by UnprocessedFilterHandler objects, and sets the hasUnprocessedListFilters flag to true 
	 * when they are encountered
	 * 
	 * @param Filter[] filters
	 */
	public void setFilters(Filter[] filters) {
    Object old = getFilters();
		for (int i = 0, n = filters.length; i < n; i++)
      addFilter(filters[i]);
    changeSupport.firePropertyChange("filters", old, getFilters() );
	}

	/**
	 * add a single Filter object.
	 * 
	 * @param Filter filter
	 */
	public void addFilter(Filter filter) {
		 filters.add(filter);    

    changeSupport.firePropertyChange("filter", null, filter );
	}

  /**
   * remove an Attribute object from the list of Attributes
   * 
   * @param Attribute attribute
   */
  public void removeAttribute(Attribute attribute) {
    if (attributes.contains(attribute)) {
    
      attributes.remove(attribute);
      changeSupport.firePropertyChange("attribute", attribute, null);
    }
  }
    
	/**
	 * Sets a SequenceDescription to the Query, and sets querytype = SEQUENCE. 
	* @param s A SequenceDescription object.
	*/
	public void setSequenceDescription(SequenceDescription s) {
    Object old = getSequenceDescription();
		this.seqd = s;
		this.querytype = Query.SEQUENCE;
    changeSupport.firePropertyChange("sequence_description", old, getSequenceDescription() );
	}

	/**
	 * returns the SequenceDescription for this Query.
	* @return SequenceDescription
	*/
	public SequenceDescription getSequenceDescription() {
		return seqd;
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
    Object old = getPrimaryKeys();
		this.primaryKeys = primaryKeys;
    changeSupport.firePropertyChange("primary_keys", old, getPrimaryKeys() );
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
    Object old = getStarBases();
		this.starBases = starBases;
    changeSupport.firePropertyChange("star_bases", old, getStarBases() );
	}

  /**
   * Set a limit for the Query.
   * @param inlimit - int limit to add to the Query
   */
  public void setLimit(int inlimit) {
  	if (inlimit > 0) {
      int old = this.limit;
  	  this.limit = inlimit;
      changeSupport.firePropertyChange("limit", old, this.limit );
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
    buf.append( " datasetInternalName=").append(datasetInternalName);
    buf.append(", dataSource=").append( dataSource );
		buf.append(", starBases=[").append(StringUtil.toString(starBases));
		buf.append("], primaryKeys=[").append(StringUtil.toString(primaryKeys));
		buf.append("], querytype=").append(querytype);
		buf.append(", attributes=").append(attributes);
		buf.append(", filters=").append(filters);
		
		if (seqd != null)
			buf.append(", sequencedescription=").append(seqd);

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
		  tmp = (31 * tmp) + seqd.hashCode();
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
		
		return tmp;
	}
	
	private List attributes = new Vector();
	private List filters = new Vector();

  private String queryName;
	private int querytype = Query.ATTRIBUTE; // default to ATTRIBUTE, over ride for SEQUENCE
	private SequenceDescription seqd;
	private String[] primaryKeys;
	private String[] starBases;
	private int limit = 0; // add a limit clause to the SQL with an int > 0
  private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	/**
	 * Datasource this query applies to.
	 */
	private DataSource dataSource;

	/**
	 * Name of dataset this query applies to.
	 */
	private String datasetInternalName;

  /**
   * @param listener
   */
  public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.addPropertyChangeListener(listener);
  }

  /**
   * @param propertyName
   * @param listener
   */
  public synchronized void addPropertyChangeListener(
    String propertyName,
    PropertyChangeListener listener) {
    changeSupport.addPropertyChangeListener(propertyName, listener);
  }

  /**
   * @return
   */
  public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
    return changeSupport.getPropertyChangeListeners();
  }

  /**
   * @param propertyName
   * @return
   */
  public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
    return changeSupport.getPropertyChangeListeners(propertyName);
  }

  /**
   * @param propertyName
   * @return
   */
  public synchronized boolean hasListeners(String propertyName) {
    return changeSupport.hasListeners(propertyName);
  }

  /**
   * @param listener
   */
  public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.removePropertyChangeListener(listener);
  }

  /**
   * @param propertyName
   * @param listener
   */
  public synchronized void removePropertyChangeListener(
    String propertyName,
    PropertyChangeListener listener) {
    changeSupport.removePropertyChangeListener(propertyName, listener);
  }

	/**
	 * Removes all attributes from the query. Each removed attribute will
   * generate a separate property change event.
	 */
	public void removeAllAttributes() {
    
		Attribute[] attributes = getAttributes();
    
    for (int i = 0, n = attributes.length; i < n; i++) {
			removeAttribute( attributes[i] );
		}
		
	}


  /**
   * Removes all Filters from the query. Each removed Filter will
   * generate a separate property change event.
   */
  public void removeAllFilters() {
    
    Filter[] filters = getFilters();
    
    for (int i = 0, n = filters.length; i < n; i++) {
      removeFilter( filters[i] );
    }
    
  }


  /**
   * Removes all PropertyChangeListeners from the query. Each removed PropertyChangeListener will
   * generate a separate property change event.
   */
  public void removeAllPropertyChangeListeners() {
    
    PropertyChangeListener[] propertyChangeListeners = getPropertyChangeListeners();
    
    for (int i = 0, n = propertyChangeListeners.length; i < n; i++) {
      removePropertyChangeListener( propertyChangeListeners[i] );
    }
    
  }

  /**
   * Replace the oldFilter with the new one. Fires a property change event
   * where event.name="filters", event.oldValue=filter array before change, 
   * event.newValue=filter array after change.
   * @param oldFilter
   * @param newFilter
   * @throws RuntimeException if oldFilter is not currently in the query.
   */
  public void replaceFilter(Filter oldFilter, Filter newFilter) {

    int index = filters.indexOf( oldFilter );
    if ( index==-1 ) 
      throw new RuntimeException("Old filter not in query: " + oldFilter);

    filters.remove( index );
    filters.add(index, newFilter );
    
    changeSupport.firePropertyChange("filter", oldFilter, newFilter );
  }

	public DataSource getDataSource() {
		return dataSource;
	}

  /**
   * Sets the value and propagates a PropertyChange event to listeners. The 
   * property name is in the event is "dataSource".
   * @param dataSource new dataSource.
   */
	public void setDataSource(DataSource dataSource) {
    DataSource oldDatasource = this.dataSource;
		this.dataSource = dataSource;
    changeSupport.firePropertyChange("dataSource", oldDatasource, dataSource );
	}

	public String getDatasetInternalName() {
		return datasetInternalName;
	}

  /**
   * Sets the value and propagates a PropertyChange event to listeners. The 
   * property name is in the event is "datasetInternalName". No event is propagated 
   * if the parameter is equal to the current datasetInternalName.
   * @param datasetInternalName new datasetInternalName.
   */
	public void setDatasetInternalName(String datasetName) {
    
    if ( this.datasetInternalName==datasetName 
        || datasetName!=null && datasetName.equals(this.datasetInternalName) )
        return;
         
    String oldDatasetName = this.datasetInternalName;
		this.datasetInternalName = datasetName;
    changeSupport.firePropertyChange("datasetInternalName", oldDatasetName, datasetName );
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
	public void setQueryName(String string) {
		queryName = string;
	}

}
