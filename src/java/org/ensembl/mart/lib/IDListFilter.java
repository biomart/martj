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
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IDListFilter object for storing a list of IDs of a particular
 * type to restrict Queries on.  IDListFilter objects can be
 * created on a single id (if you are only going to add one id,
 * you should just use a BasicFilter object instead, this one is more
 * useful for the MartExplorerTool to add multiple ids in succession),
 * a String[] of ids, a File of ids, a URL of ids, or
 * an InputStream of ids.  For each type of IDListFilter Object,
 *  there should be a corresponding UnprocessedFilterHandler object
 * to resolve the underlying data Object into a IDListFilter Object
 * with a list of Strings to apply in a SQL where x in (list) clause.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @see UnprocessedFilterHandler
 */
public class IDListFilter implements Filter {

	/**
	 * enums over UnprocessedFilterHandler implimenting class names
	 */
	public static final String STRING = "org.ensembl.mart.lib.StringIDListFilterHandler";
	public static final String FILE = "org.ensembl.mart.lib.FileIDListFilterHandler";
	public static final String URL = "org.ensembl.mart.lib.URLIDListFilterHandler";
	public static final String SUBQUERY = "org.ensembl.mart.lib.SubQueryIDListFilterHandler";

	/**
	 * Construct an STRING type IDListFilter object of a given field name on a String[] List of 
	 * identifiers.
	 * 
	 * @param String name - field name
	 * @param String[] identifiers
   * @see StringIDListFilterHandler
	 */
	public IDListFilter(String field, String[] identifiers) {
		this(field, null, identifiers, null);
	}

	/**
	 * Construct an IDListFilter object of a given field name and tableConstraint, on a String[] List of 
	 * identifiers.
	 * 
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param String[] identifiers
   * @see StringIDListFilterHandler
	 */
	public IDListFilter(String field, String tableConstraint, String[] identifiers) {
		this(field, tableConstraint, identifiers, null);
	}

	/**
	 * Construct an IDListFilter object of a given field name and tableConstraint, on a String[] List of 
	 * identifiers, with a user supplied handler (default handler for this type of IDListFilter is IDListFilter.STRING).
	 * 
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param String[] identifiers
   * @param handler -- handler object to process this Filter, default is used if null
   * @see StringIDListFilterHandler
	 */
  public IDListFilter(String field, String tableConstraint, String[] identifiers, String handler) {
		this.field = field;
		this.tableConstraint = tableConstraint;
		this.identifiers = Arrays.asList(identifiers);
		this.file = null;
		this.url = null;
		this.subQuery = null;
		
		if (handler != null)
		  this.handler = handler;
		else
		  this.handler = STRING;
		
		setHashCode();
  }
  
	/**
	 * Construct a FILE type IDListFilter object of a given field name from a file
	 * containing identifiers.
	 * 
	 * @param String name - field name
	 * @param File file
   * @see FileIDListFilterHandler
	 */
	public IDListFilter(String field, File file) {
		this(field, null, file, null);
	}

	/**
	 * Construct a FILE type IDListFilter object of a given field name from a file
	 * containing identifiers.
	 * 
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param File file
   * @see FileIDListFilterHandler
	 */
	public IDListFilter(String name, String tableConstraint, File file) {
     this(name, tableConstraint, file, null);
	}

	/**
	 * Construct a FILE type IDListFilter object of a given field name from a file
	 * containing identifiers, with a user supplied handler (the default for this type of
	 * IDListFilter is IDListFilter.FILE).
	 * 
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param File file
   * @param handler -- handler object to process this Filter, default is used if null
   * @see FileIDListFilterHandler
	 */
  public IDListFilter(String name, String tableConstraint, File file, String handler) {
		this.field = name;
		this.tableConstraint = tableConstraint;
		this.file = file;
		this.url = null;
		this.subQuery = null;
		
		if (handler != null)
		  this.handler = handler;
		else
		  this.handler = FILE;
		  
		setHashCode();
  }
  
	/**
	 * Construct a URL type IDListFilter object of a given field name 
	 * from a specified URL object containing identifiers.  
	 * 
	 * @param String name - field name
	 * @param URL url
   * @see URLIDListFilterHandler
	 */
	public IDListFilter(String name, URL url) {
		this(name, null, url, null);
	}

	/**
	 * Construct a URL type IDListFilter object of a given field name 
	 * and table constraint, from a specified URL object containing identifiers. 
	 * 
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param URL url - url pointing to resource with IDs
   * @see URLIDListFilterHandler
	 */
	public IDListFilter(String name, String tableConstraint, URL url) {
    this (name, tableConstraint, url, null);
	}

  /**
   * Construct a URL type IDListFilter, with a given fieldName and tableConstraint, with
   * a user supplied handler (default handler for this type of Filter is IDListFilter.URL)
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param URL url - url pointing to resource with IDs
   * @param handler -- handler object to process this Filter, default is used if null
   * @see URLIDListFilterHandler
   */
  public IDListFilter(String name, String tableConstraint, URL url, String handler) {
		this.field = name;
		this.tableConstraint = tableConstraint;
		this.url = url;
		this.file = null;
		this.subQuery = null;
		
		if (handler != null)
		  this.handler = handler;
		else  
		  this.handler = URL;
		setHashCode();
  }
  
	/**
	 * Construct a SUBQUERY type IDListFilter object of a given field name
	 * from a Query object that fits the constraints of a subQuery.
	 * @param String name - field name
	 * @param Query subQuery - Query that, when evaluated, returns a list of IDs
	 * @see QueryIDListFilterHandler 
	 */
	public IDListFilter(String name, Query subQuery) {
		this(name, null, subQuery, null);
	}

	/**
	 * Construct a SUBQUERY type IDListFilter object of a given field name
	 * and table constraint, from a Query object that fits the constraints of
	 * a subQuery.
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param Query subQuery - Query that, when evaluated, returns a list of IDs
	 * @see QueryIDListFilterHandler 
	 */
	public IDListFilter(String name, String tableConstraint, Query subQuery) {
		this(name, tableConstraint, subQuery, null);
	}

  /**
   * Construct a SUBQUERY type IDListFilter object of a given field name, tableConstraint, with
   * a user supplied handler (default handler for this type of Filter is IDListFilter.SUBQUERY)
	 * @param String name - field name
	 * @param String tableConstraint - table constraint for field name
	 * @param Query subQuery - Query that, when evaluated, returns a list of IDs
   * @param handler -- handler object to process this Filter, default is used if null
 	 * @see QueryIDListFilterHandler
   */
  public IDListFilter(String name, String tableConstraint, Query subQuery, String handler) {
		this.field = name;
		this.tableConstraint = tableConstraint;
		this.subQuery = subQuery;
		this.file = null;
		this.url = null;
		
		if (handler != null)
		  this.handler = handler;
		else
		  this.handler = SUBQUERY;
		setHashCode();
  }
  
	private void setHashCode() {
		hashcode = (field == null) ? 0 : field.hashCode();
		hashcode = (tableConstraint != null) ? (31 * hashcode) + tableConstraint.hashCode() : hashcode;
		hashcode = (handler != null) ? (31 * hashcode) + handler.hashCode() : hashcode;

		if (identifiers.size() > 0) {
			for (int i = 0, n = identifiers.size(); i < n; i++) {
				String element = (String) identifiers.get(i);
				hashcode = (31 * hashcode) + element.hashCode();
			}
		}

		hashcode = (file != null) ? (31 * hashcode) + file.hashCode() : hashcode;
		hashcode = (url != null) ? (31 * hashcode) + url.hashCode() : hashcode;
		hashcode = (subQuery != null) ? (31 * hashcode) + subQuery.hashCode() : hashcode;
	}

	/**
	 * returns the Where Clause for the SQL
	 * 
	 * @return String where clause 'IN (quoted list of identifiers)'
	 * @throws InvalidListException when IDListFilter is not a STRING type.
	 */
	public String getWhereClause() {
		StringBuffer buf = new StringBuffer();
		buf.append(field).append(" IN (");
		for (int i = 0, n = identifiers.size(); i < n; i++) {
			String element = (String) identifiers.get(i);
			if (i > 0)
				buf.append(", ");
			buf.append("\"").append(element).append("\"");
		}

		buf.append(" ) ");
		return buf.toString();
	}

	/**
	 * same as getWhereClause()
	 */
	public String getRightHandClause() {

		StringBuffer buf = new StringBuffer();
		buf.append(" IN (");
		for (int i = 0, n = identifiers.size(); i < n; i++) {
			String element = (String) identifiers.get(i);
			if (i > 0)
				buf.append(", ");
			buf.append("\"").append(element).append("\"");
		}
		buf.append(" ) ");
		return buf.toString();
	}

	/**
	 * get the String[] List of identifiers
	 * 
	 * @return String[] identifiers
	 */
	public String[] getIdentifiers() {
		String[] ret = new String[identifiers.size()];
		identifiers.toArray(ret);
		return ret;
	}

	/**
	 * get the Field Name of the IDListFilter
	 * 
	 * @return String field name
	 */
	public String getField() {
		return field;
	}

	public String getValue() {
		return null;
	}

	/**
	 * Returns the tableConstraint for this IDListFilter Object
	 * @return String tableConstriant
	 */
	public String getTableConstraint() {
		return tableConstraint;
	}

	/**
	 * Returns the File object underlying a FILE type IDListFilter Object.
	 * 
	 * @return File file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Returns the Query object underlying a SUBQUERY type IDListFilter Object
	 * @return Query subQuery
	 */
	public Query getSubQuery() {
		return subQuery;
	}

	/**
	 * Returns the underlying URL Object underlying a URL type IDListFilter Object
	 * @return URL url
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * Not applicable to this type of Filter. Returns null
	 */
	public String getCondition() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.Filter#getHandler()
	 */
	public String getHandler() {
		return handler;
	}

	/**
	 * returns a description of the object useful for logging systems.
	 * 
	 * @return String description(field=field,identifiers=list of identifiers)
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" field=").append(field);
		buf.append(", tableConstraint=").append(tableConstraint);
		buf.append(", handler=").append(handler);
		buf.append(", identifiers=").append(identifiers);
		buf.append(", File=").append(file);
		buf.append(", URL=").append(url);
		buf.append(", Query=").append(subQuery);

		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of IDListFilter objects
	 */
	public boolean equals(Object o) {
		return o instanceof IDListFilter && hashCode() == o.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return hashcode;
	}

	private final String field;
	private final String tableConstraint;
	private final String handler;

	private final Query subQuery; // for Query based Filter
	private final File file;
	private final URL url;

	private List identifiers = new ArrayList();

	private int hashcode = 0; //hashcode for immutable object
}
