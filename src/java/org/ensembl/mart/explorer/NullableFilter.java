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

package org.ensembl.mart.explorer;

/**
 * Implimentation of a Filter object for null and not null filters,
 *  eg, 'where x is null' or 'where x is not null'
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class NullableFilter implements Filter {

	public static final String isNULL = " ";
	public static final String isNotNULL = " not ";

	/**
	 * default constructor
	 */
	public NullableFilter() {
	}

	/**
	 * constructor for a fully defined NullableFilter.
	 * static isNULL and isNotNull variables can be used
	 * to set the condition
	 * 
	 * @param type - String, type of filter
	 * @param condition - String, one of isNULL or isNotNull
	 */
	public NullableFilter(String type, String condition) {
		this.type = type;
		this.condition = "is" + condition + "null ";
	}
	/**
	 * returns the type specified
	 * 
	 * @return String type
	 */
	public String getName() {
		return type;
	}

	/**
	 * sets the type
	 * 
	 * @param type -- String type of the query (roughly corresponds to
	 * a field in a mart table).
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * returns the where clause for the SQL as type is null
	 */
	public String getWhereClause() {
		return type + condition;
	}

	/**
	 * sets the condition.  condition can be specified by explicit use
	 * of the static isNULL and isNotNull variables.
	 * 
	 * @param condition - String
	 */
	public void setCondition(String condition) {
		this.condition = "is" + condition + "null ";
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

	public void setTableConstraint(String tableConstraint) {
		this.tableConstraint = tableConstraint;
	}

	public String getTableConstraint() {
		return tableConstraint;
	}

	private String tableConstraint;
	private String type;
	private String condition;
}
