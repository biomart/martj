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

package org.ensembl.mart.explorer;

import java.util.*;

import org.ensembl.util.StringUtil;

/**
 * Object for storing the parameters to construct a query against a Mart
 * database.  Parameters consist of at least one Attribute (a requested field
 * from the database) implimenting object. Parameters can include Filter 
 * implimenting objects to restrict the Query on user supplied conditions.
 * Parameters can also include SequenceDescriptions.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @see Attribute
 * @see Filter
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
  	  Attribute[] oa = oq.getAttributes();
  	  Attribute[] na = new Attribute[oa.length];
  	  for (int i = 0, n = oa.length; i < n; i++)
			  na[i] = new FieldAttribute((FieldAttribute) oa[i]);
			  
      setAttributes(na);
  	}
    
    if (oq.getFilters().length > 0) {
      Filter[] ofs = oq.getFilters();
      Filter[] nfs = new Filter[ofs.length];
      for (int i = 0, n = ofs.length; i < n; i++) {
				Filter fo = ofs[i];
				Filter fn = null;
				
				if (fo instanceof BasicFilter)
				  fn = new BasicFilter((BasicFilter) fo);
				else if (fo instanceof IDListFilter)
				  fn = new IDListFilter((IDListFilter) fo);
				else
				  fn = new NullableFilter((NullableFilter) fo); //only three possible
				  
				nfs[i] = fn;
			}
      
      setFilters(nfs);
    }
  	
  	if (oq.hasDomainSpecificFilters) {
  		DomainSpecificFilter[] odsfs = oq.getDomainSpecificFilters();
  		DomainSpecificFilter[] ndsfs = new DomainSpecificFilter[odsfs.length];
  		for (int i = 0, n = odsfs.length; i < n; i++)
  		  ndsfs[i] = new DomainSpecificFilter(odsfs[i]);
  		  
  		setDomainSpecificFilters(ndsfs);
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
		if (!attributes.contains(attribute))
			attributes.add(attribute);
	}

	/**
	 * remove an Attribute object from the list of Attributes
	 * 
	 * @param Attribute attribute
	 */
	public void removeAttribute(Attribute attribute) {
		if (attributes.contains(attribute))
			attributes.remove(attribute);
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
		this.attributes = new ArrayList(Arrays.asList(attributes));
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
			if (element.getName().equals(name))
			  return element;
		}
		return null;
  }
	/**
	 * set an entire list of Filter objects
	 * 
	 * @param Filter[] filters
	 */
	public void setFilters(Filter[] filters) {
		this.filters =  new ArrayList(Arrays.asList(filters));
	}

	/**
	 * add a single Filter object
	 * 
	 * @param Filter filter
	 */
	public void addFilter(Filter filter) {
		filters.add(filter);
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
   * 
   * @param DomainSpecificFilter[] dsfilters
   */
  public void setDomainSpecificFilters(DomainSpecificFilter[] dsfilters) {
  	hasDomainSpecificFilters = true;
  	this.dsfilters = Arrays.asList(dsfilters);
  }
  
  /**
   * add a single DomainSpecificFilter object
   * 
   * @param DomainSpecificFilter dsfilter
   */
  public void addDomainSpecificFilter(DomainSpecificFilter dsfilter) {
  	if (! hasDomainSpecificFilters)
  	  hasDomainSpecificFilters = true;
   	dsfilters.add(dsfilter);
  }
  
  public boolean hasDomainSpecificFilters() {
  	return hasDomainSpecificFilters;
  }
  
	/**
	 * Sets a SequenceDescription to the Query, and sets querytype = SEQUENCE. 
	* @param s A SequenceDescription object.
	*/
	public void setSequenceDescription(SequenceDescription s) {
		this.seqd = s;
		this.querytype = Query.SEQUENCE;
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
		this.primaryKeys = primaryKeys;
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
		this.starBases = starBases;
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
		
		if(hasDomainSpecificFilters)
			buf.append(", dsfilters=").append(dsfilters);

		if (seqd != null)
			buf.append(", sequencedescription=").append(seqd);

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
  private boolean hasDomainSpecificFilters = false; // will be set to true if a DomainSpecificFilterObject is added
	private int querytype = Query.ATTRIBUTE; // default to ATTRIBUTE, over ride for SEQUENCE
	private SequenceDescription seqd;
	private String[] primaryKeys;
	private String[] starBases;
}
