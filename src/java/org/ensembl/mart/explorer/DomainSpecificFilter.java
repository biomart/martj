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

/**
 * Object to facilitate the resolution of Domain Specific Filters made available
 * by a specific instance of a Mart Database.  Domain Specific Filters require
 * logic outside of the simple main/dimension table SQL where clauses that most 
 * filters work with.  For this reason, a DomainSpecificFilterHandler system
 * has been implimented to allow mart developers to create objects implimenting
 * this interface to resolve Complex filters into simple Query Filters.  The objectCode
 * for a DomainSpecificFilter must match an enum of one of the DomainSpecificFilterHandler 
 * implimenting objects available.  The handlerParameter is then passed as a string to the 
 * DomainSpecificFilterHandler modifyQuery method to provide the information it needs.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see DomainSpecificFilterHandler
 */
public class DomainSpecificFilter {
	
	/**
	 * Constructor for a DomainSpecificFilter object
	 * 
	 * @param objectCode - String referencing a specific DomainSpecificFilterHandler implimenting object enum
	 * @param handlerParameter - String parameter to pass to the DomainSpecificFilterHandler objects modifyQuery method.
	 */
	public DomainSpecificFilter(String objectCode, String handlerParameter) {
		this.objectCode = objectCode;
		this.handlerParameter = handlerParameter;
	}

  /**
   * Copy constructor.
   * @param o - a DomainSpecificFilter object to copy
   */
  public DomainSpecificFilter(DomainSpecificFilter o) {
  	objectCode = o.getObjectCode();
  	handlerParameter = o.getHandlerParameter();
  }
  
	/**
	 * Returns the handlerParameter.
	 * 
	 * @return String handlerParameter
	 */
	public String getHandlerParameter() {
		return handlerParameter;
	}

	/**
	 * Returns the objectCode.
	 * 
	 * @return String objectCode
	 */
	public String getObjectCode() {
		return objectCode;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("objectCode=").append(objectCode);
		buf.append(", handlerParam=").append(handlerParameter);
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
    int tmp = handlerParameter.hashCode();
		tmp = (31 * tmp) + objectCode.hashCode();
		return tmp;
	}

  private final String handlerParameter, objectCode;
}
