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

    /**
     * returns the query type (one of ATTRIBUTE or SEQUENCE)
     * @return int querytype
     */
    public int getType() {  return querytype; }

    /**
     * test to determine if a specified attribute object is 
     * contained within the attribute list of the Query.
     * 
     * @param attribute
     * @return boolean
     */
	public boolean hasAttribute( Attribute attribute ) {
		return attributes.contains( attribute );
    }

    /**
     * add an Attribute object to the list of Attributes
     * for the Query.
     * 
     * @param Attribute attribute
     */
	public void addAttribute( Attribute attribute ) {
		if ( !attributes.contains( attribute ) )
			attributes.add( attribute );
    }

     /**
      * remove an Attribute object from the list of Attributes
      * 
      * @param Attribute attribute
      */
     public void removeAttribute(  Attribute attribute ) {
	     if ( attributes.contains( attribute ) )
		     attributes.remove( attribute );
     }

     /**
      * get all Attributes as an Attribute :ist
      * 
      * @return Attribute[] attributes
      */
     public Attribute[] getAttributes() {
         Attribute[] a = new Attribute[ attributes.size() ];
         attributes.toArray( a );
         return a;
     }

     /**
      * set an entire list of Attribute objects
      * @param List attributes
      */
     public void setAttributes(List attributes) {
        this.attributes = attributes;
     }
  
     /**
      * get all Filter objects as a Filter[] Array
      * 
      * @return Filters[] filters
      */
     public Filter[] getFilters() {
         Filter[] f = new Filter[ filters.size() ];
         filters.toArray( f );
         return f;
     }

     /**
      * set an entire list of Filter objects
      * 
      * @param Filter[] filters
      */
     public void setFilters(Filter[] filters) {
	    this.filters = Arrays.asList( filters );
     }

     /**
      * add a single Filter object
      * 
      * @param Filter filter
      */
     public void addFilter(Filter filter) {
         // new filters with the same hashCode() override old ones
         if ( filters.contains( filter ) )
      	     filters.remove( filter );
             filters.add( filter );
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
	public SequenceDescription getSequenceDescription(){
    	return seqd;
    }
    
     /**
      * returns a description of the Query for logging purposes
      * 
      * @return String description (primaryKeys=primaryKeys\nstarBases=starBases\nattributes=attributes\nfilters=filters)
      */
     public String toString() {
         StringBuffer buf = new StringBuffer();

		 buf.append("[");
         buf.append(", starBases=[").append( StringUtil.toString( starBases ) );
				 buf.append("], primaryKeys=[").append( StringUtil.toString( primaryKeys ) );
         buf.append("], querytype=").append(stringquerytype);
         buf.append(", attributes=").append(attributes);
         buf.append(", filters=").append(filters);
         
         if ( seqd != null)
             buf.append(", sequencedescription=").append(seqd);
             
         buf.append("]");

         return buf.toString();
    }

    /**
     * get the primaryKeys of the Query
     * @return String primaryKeys
     */
    public String[] getPrimaryKeys(){
            return primaryKeys;
	}

    /**
     * set the primaryKeys for the Query
     * @param String primaryKeys
     */
    public void setPrimaryKeys(String[] primaryKeys){
            this.primaryKeys = primaryKeys;
	}

    /**
     * get the starBases for the Query
     * @return String starBases
     */
    public String[] getStarBases(){
            return starBases;
        }

    /**
     * set the starBases for the Query
     * @param String starBases
     */
    public void setStarBases(String[] starBases){
            this.starBases = starBases;
    }

    private List attributes = new Vector();
    private List filters = new Vector();

      
  private int querytype = Query.ATTRIBUTE; // default to ATTRIBUTE, over ride for SEQUENCE
  private SequenceDescription seqd;
  private String[] primaryKeys;
  private String[] starBases;
  private String stringquerytype;
}
