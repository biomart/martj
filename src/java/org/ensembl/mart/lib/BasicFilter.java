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
 * Basic Implimentation of a Filter object.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class BasicFilter implements Filter {

	/**
	 * constructs a BasicFilter object, which can be added to a Query
	 * 
	 * @param field -- String type.  The type of filter being applied
	 * @param qualifier -- String qualifier of the clause, eg. =<>
	 * @param value -- parameter of the qualifier, applicable to the type.
	 */
	public BasicFilter(String field, String qualifier, String value) {
		this(field, null, null, qualifier, value);
	}

	/**
	 * constructs a BasicFilter object with a tableConstraint, which can be added to a Query
	 * 
	 * @param field -- String type.  The type of filter being applied
	 * @param tableConstraint -- String table where field is found
	 * @param qualifier -- String qualifier of the clause, eg. =<>
	 * @param value -- parameter of the qualifier, applicable to the type.
	 */
	public BasicFilter(String field, String tableConstraint, String key, String qualifier, String value) {
		this(field, tableConstraint, key, qualifier, value, null);
	}

	/**
	 * constructs a BasicFilter object with a tableConstraint and handler, 
	 * which can be added to a Query
	 * 
	 * @param field -- String type.  The type of filter being applied
	 * @param tableConstraint -- String hint for table or tables where field is found
	 * @param qualifier -- String qualifier of the clause, eg. =<>
	 * @param value -- parameter of the qualifier, applicable to the type.
	 * @param handler -- name of UnprocessedFilterHandler implimenting class to load to handle this Filter, or null if
	 *                                  no processing is required.
	 */
	public BasicFilter(String field, String tableConstraint, String key, String qualifier, String value, String handler) {
		this.field = field;
		this.tableConstraint = tableConstraint;
		this.key = key;
		this.qualifier = qualifier;
		this.value = value;
		this.handler = handler;

		hashcode = (this.field == null) ? 0 : this.field.hashCode();
		hashcode = (this.qualifier != null) ? (31 * hashcode) + this.qualifier.hashCode() : hashcode;
		hashcode = (this.value != null) ? (31 * hashcode) + ((this.value == null) ? 0 : this.value.hashCode()) : hashcode;
		hashcode = (this.tableConstraint != null) ? (31 * hashcode) + this.tableConstraint.hashCode() : hashcode;
		hashcode = (this.key != null) ? (31 * hashcode) + this.key.hashCode() : hashcode;
		hashcode = (this.handler != null) ? (31 * hashcode) + this.handler.hashCode() : hashcode; 
	}

	/**
	 * returns the qualifier for the filter
	 * @return String qualifier =<>
	 */
	public String getQualifier() {
		return qualifier;
	}

	/**
	 * returns the value set for the query filter
	 * 
	 * @return String value 
	 */
	public String getValue() {
		return value;
	}

	/**
	 * returns the type set for the query filter
	 * 
	 * @return String type
	 */
	public String getField() {
		return field;
	}

	public String getTableConstraint() {
		return tableConstraint;
	}

	public String getKey() {
	  return key;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.Filter#getHandler()
	 */
	public String getHandler() {
		return handler;
	}

	/**
	 * prints information about the filter, for logging purposes
	 *
	 * @return String filter information (field=type\nqualifier=qualifier\nvalue=value)
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" field=").append(field);
    buf.append(", tableConstraint=").append(tableConstraint);
    buf.append(", key=").append(key);
		buf.append(" ,qualifier=").append(qualifier);
		buf.append(" ,value=").append(value);
		buf.append(", handler=").append(handler);
		buf.append("]");

		return buf.toString();
	}

	/**
	 * returns a where clause with the type, qualifier, and a bind parameter 
	 * for the value, suitable for inclusion in a SQL PreparedStatement
	 * 
	 * @return String where clause
	 */
	public String getWhereClause() {
		return field + qualifier + "?";
	}

	/**
	 * returns the right hand of the where clause, with the qualifier, and
	 * a bind value suitable for inclusion into a SQL PreparedStatement.
	 */
	public String getRightHandClause() {
		return qualifier + "?";
	}

	/**
	 * returns the value of the filter
	 * 
	 * @return String value
	 */
	public String sqlValue() {
		return value;
	}

	/**
	 * Allows Equality Comparisons manipulation of BasicFilter objects
	 */
	public boolean equals(Object o) {
		return o instanceof BasicFilter && hashCode() == ((BasicFilter) o).hashCode();
	}

	public int hashCode() {
		return hashcode;
	}

	private final String field;
	private final String qualifier;
	private final String value;
	private final String tableConstraint;
	private final String key;
	private final String handler;
	private int hashcode = 0; //hashcode for immutable object
}
