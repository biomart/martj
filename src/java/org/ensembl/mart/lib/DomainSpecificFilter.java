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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Object to facilitate the resolution of Domain Specific Filters made available
 * by a specific instance of a Mart Database.  Domain Specific Filters require
 * logic outside of the simple main/dimension table SQL where clauses that most 
 * filters work with.  For this reason, a DomainSpecificFilterHandler system
 * has been implimented to allow mart developers to create objects implimenting
 * this interface to resolve Complex filters into simple Query Filters.  The handler
 * for a DomainSpecificFilter must match an enum of one of the DomainSpecificFilterHandler 
 * implimenting objects available.  The cludgyParameter is then passed as a string to the 
 * DomainSpecificFilterHandler modifyQuery method to provide the information it needs.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see DSFilterHandler
 */
public class DomainSpecificFilter {
	
	public static String MARKER = "Marker";
	public static String BAND = "Band";
	public static String ENCODE = "Encode";
	public static String QTL = "Qtl";
	public static String EXPRESSION = "Expression";
	
	public static final List SUPPORTED_DSFILTERS = Collections.unmodifiableList(Arrays.asList( new String[] {"Marker", "Band", "Encode", "Qtl", "Expression" } ) );
	
	/**
	 * Constructor for a DomainSpecificFilter object
	 * 
	 * @param handler - name of handler class which handles this filter (or currently a key to built in types).
	 * @param cludgyParameter - Encodes fieldname and value in a really cludgy way. To be changed!
	 */
	public DomainSpecificFilter(String objectCode, String handlerParameter) {
		this.handler = objectCode;
		this.cludgyParameter = handlerParameter;
    
    hashcode = handlerParameter.hashCode();
    hashcode = (31 * hashcode) + objectCode.hashCode();
	}

  /**
   * Copy constructor.
   * @param o - a DomainSpecificFilter object to copy
   */
  public DomainSpecificFilter(DomainSpecificFilter o) {
  	handler = o.getHandler();
  	cludgyParameter = o.getCludgyParameter();
    
    hashcode = cludgyParameter.hashCode();
    hashcode = (31 * hashcode) + handler.hashCode();
  }
  
	/**
	 * Returns the cludgyParameter.
	 * 
	 * @return String cludgyParameter
	 */
	public String getCludgyParameter() {
		return cludgyParameter;
	}

	/**
	 * Returns the handler.
	 * 
	 * @return String handler
	 */
	public String getHandler() {
		return handler;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("handler=").append(handler);
		buf.append(", handlerParam=").append(cludgyParameter);
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of DomainSpecificFilter objects
	 */
	public boolean equals(Object o) {
		return o instanceof DomainSpecificFilter && hashCode() == ((DomainSpecificFilter) o).hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return hashcode;
	}

  private final String cludgyParameter;
  private final String handler;
  private int hashcode = 0; //hashcode for immutable object
}
