/* Generated by Together */

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

/**
 * Implimentation of a Filter object for null and not null filters,
 *  eg, 'where x is null' or 'where x is not null'
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class NullableFilter implements Filter {

	public static final String isNULL = "is null";
	public static final String isNotNULL = "is not null";
	public static final String isNULL_NUM = "!= 1";
	public static final String isNotNULL_NUM = "= 1";

/**
 * Constructor for a basic NullableFilter, with field and condition set.
 * 
	 * @param field - String, field of filter
	 * @param condition - String, one of isNULL or isNotNull
 */
	public NullableFilter(String field, String condition) {
    this(field, null, condition);
	}

	/**
	 * constructor for a fully defined NullableFilter.
	 * static isNULL and isNotNull variables can be used
	 * to set the condition
	 * 
	 * @param field - String, field of filter
	 * @param tableConstraint - String, tableConstraint for the Filter
	 * @param condition - String, one of isNULL or isNotNull
	 */	
	public NullableFilter(String field, String tableConstraint, String condition) {
		this.field = field;
		this.tableConstraint = tableConstraint;
		this.condition = condition;
    
    hashcode = (this.field == null) ? 0 : this.field.hashCode();
    hashcode = (31 * hashcode) + ( (this.tableConstraint == null) ? 0 : this.tableConstraint.hashCode() );
    hashcode = (31 * hashcode) + ( (this.condition == null) ? 0 : this.condition.hashCode() );		
	}
	
	/**
	 * returns the field specified
	 * 
	 * @return String field
	 */
	public String getField() {
		return field;
	}

	/**
	 * returns the where clause for the SQL as field is null
	 */
	public String getWhereClause() {
		return field + " " + condition;
	}

	/**
	 * returns the right side of an SQL where clause
	 */
	public String getRightHandClause() {
		return condition;
	}

	/*
	 * @see org.ensembl.mart.explorer.Filter#getValue()
	 */
	public String getValue() {
		return null;
	}

	public String getTableConstraint() {
		return tableConstraint;
	}

  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("field=").append(field);
		buf.append(", tableConstraint=").append(tableConstraint);
		buf.append(", condition=").append(condition);
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of NullableFilter objects
	 */
	public boolean equals(Object o) {
		return o instanceof NullableFilter && hashCode() == ((NullableFilter) o).hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
    return hashcode;
	}

	private final String field, tableConstraint, condition;
  private int hashcode = 0; //hashcode for immutable object
}
