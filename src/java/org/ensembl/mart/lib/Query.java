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
  	
  	if (oq.hasUnprocessedListFilters) {
      IDListFilter[] ofilts = oq.getUnprocessedListFilters();
      Filter[] nfilts = new Filter[ofilts.length];
      for (int i = 0, n = ofilts.length; i < n; i++)
        nfilts[i] = (Filter) ofilts[i];
      
      setFilters(nfilts);
  	}
  	
  	if (oq.hasDomainSpecificFilters) {
      DomainSpecificFilter[] odsfilts = oq.getDomainSpecificFilters();
      DomainSpecificFilter[] ndsfilts = new DomainSpecificFilter[odsfilts.length];
      System.arraycopy(odsfilts, 0, ndsfilts,0,odsfilts.length);
  		setDomainSpecificFilters(ndsfilts);
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
    changeSupport.firePropertyChange("attribute", old, this.attributes);
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
		for (int i = 0, n = unprocessedfilters.size(); i < n; i++) {
			Filter element = (Filter) unprocessedfilters.get(i);
			if (element.getField().equals(name))
			  return element;
		}
		return null;
  }
  
	/**
	 * set an entire list of Filter objects.  Subsequent calls to setFilters will add to 
	 * what was added with previous addFilter/setFilters calls.  This method handles the 
	 * logic for collecting any non STRING type IDListFilter objects for later processing 
	 * by IDListFilterHandler objects, and sets the hasUnprocessedListFilters flag to true 
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
	 * add a single Filter object.  This method handles the logic for collecting any
	 * non STRING type IDListFilter objects for later processing by IDListFilterHandler objects,
	 * and sets the hasUnprocessedListFilters flag to true when they are encountered.
	 * 
	 * @param Filter filter
	 */
	public void addFilter(Filter filter) {
    Object old = getFilters();
		if (filter instanceof IDListFilter && ( (IDListFilter) filter).getType() != IDListFilter.STRING ) {
		  unprocessedfilters.add((IDListFilter) filter);
		  hasUnprocessedListFilters = true;
		}
		else
		  filters.add(filter);
      

    changeSupport.firePropertyChange("filters", old, getFilters() );
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
   * get all DomainSpecificFilter objects as a DomainSpecificFilter[] Array
   * 
   * @return DomainSpecificFilter[] dsfilters
   */
  public DomainSpecificFilter[] getDomainSpecificFilters() {
  	DomainSpecificFilter[] dsf = new DomainSpecificFilter[dsfilters.size()];
  	dsfilters.toArray(dsf);
  	return dsf;
  }
  
  /**
   * set an entire list of DomainSpecificFilter objects
   * subsequent calls to setDomainSpecificFilter will add to what was added before
   * 
   * @param DomainSpecificFilter[] dsfilters
   */
  public void setDomainSpecificFilters(DomainSpecificFilter[] dsfilters) {
    Object old = getDomainSpecificFilters();
  	hasDomainSpecificFilters = true;
  	this.dsfilters.addAll(Arrays.asList(dsfilters));

    changeSupport.firePropertyChange("domain_specific_filters", old, getDomainSpecificFilters() );
  }
  
  /**
   * add a single DomainSpecificFilter object
   * 
   * @param DomainSpecificFilter dsfilter
   */
  public void addDomainSpecificFilter(DomainSpecificFilter dsfilter) {
    Object old = getDomainSpecificFilters();
    hasDomainSpecificFilters = true;
   	dsfilters.add(dsfilter);
    changeSupport.firePropertyChange("domain_specific_filters", old, getDomainSpecificFilters() );  
  }
  
  /**
   * Determine if the Query contains Domain Specific Filters to process
   * @return true if it contains Domain Specific Filters, false if not
   */
  public boolean hasDomainSpecificFilters() {
  	return hasDomainSpecificFilters;
  }
  
  /**
   * Determine if the Query contains unprocessed IDListFilter objects.
   * @return true if it contains unprocessed IDListFilter objects, false if not
   */
  public boolean hasUnprocessedListFilters() {
  	return hasUnprocessedListFilters;
  }
  
  public IDListFilter[] getUnprocessedListFilters() {
  	IDListFilter[] ret = new IDListFilter[unprocessedfilters.size()];
  	unprocessedfilters.toArray(ret);
  	return ret;
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
  	return filters.size() + dsfilters.size() + unprocessedfilters.size();	
  }
	/**
	 * returns a description of the Query for logging purposes
	 * 
	 * @return String description (primaryKeys=primaryKeys\nstarBases=starBases\nattributes=attributes\nfilters=filters)
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(", starBases=[").append(StringUtil.toString(starBases));
		buf.append("], primaryKeys=[").append(StringUtil.toString(primaryKeys));
		buf.append("], querytype=").append(querytype);
		buf.append(", attributes=").append(attributes);
		buf.append(", filters=").append(filters);
		
		if (hasUnprocessedListFilters)
		  buf.append(", unprocessedfilters=").append(unprocessedfilters);
		  
		if(hasDomainSpecificFilters)
			buf.append(", dsfilters=").append(dsfilters);

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
		int tmp = hasDomainSpecificFilters ? 1 : 0;
		tmp = (31 * tmp);
		tmp += hasUnprocessedListFilters ? 1 : 0;
		tmp = (31 * tmp) + limit;
		
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
		
		for (int i = 0, n = filters.size(); i < n; i++) {
			Object element = filters.get(i);
			if (element instanceof BasicFilter)
			  tmp = (31 * tmp) + ( (BasicFilter) element).hashCode();
			else if (element instanceof IDListFilter)
			  tmp = (31 * tmp) + ( (IDListFilter) element).hashCode();
			else
			  tmp = (31 * tmp) + ( (NullableFilter) element).hashCode();
		}
		
		if (hasUnprocessedListFilters) {
			for (int i = 0, n = unprocessedfilters.size(); i < n; i++) {
				IDListFilter element = (IDListFilter) unprocessedfilters.get(i);
				tmp = (31 * tmp) + element.hashCode();
			}
		}
		
		if (hasDomainSpecificFilters) {
			for (int i = 0, n = dsfilters.size(); i < n; i++) {
				DomainSpecificFilter element = (DomainSpecificFilter) dsfilters.get(i);
				tmp = (31 * tmp) + element.hashCode();
      }
		}
		
		return tmp;
	}
	
	private List attributes = new Vector();
	private List filters = new Vector();
  private List dsfilters = new Vector(); // will hold DomainSpecificFilter objects
  private List unprocessedfilters = new Vector(); // will hold non STRING type IDListFilter objects

  private boolean hasDomainSpecificFilters = false; // will be set to true if a DomainSpecificFilterObject is added
  private boolean hasUnprocessedListFilters = false; // will be set to true if a non STRING type IDListFilter Object is added
	private int querytype = Query.ATTRIBUTE; // default to ATTRIBUTE, over ride for SEQUENCE
	private SequenceDescription seqd;
	private String[] primaryKeys;
	private String[] starBases;
	private int limit = 0; // add a limit clause to the SQL with an int > 0
  private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
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

}
