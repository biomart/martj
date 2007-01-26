/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.biomart.jdbc.exceptions.RegistryException;
import org.biomart.jdbc.model.Registry;
import org.biomart.jdbc.model.Mart.JDBCMart;
import org.biomart.jdbc.model.Mart.XMLMart;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public abstract class SubQuery {

	private Registry registry;

	private SubQuery parentSubQuery;

	private Collection subQueries = new HashSet();

	private Map importableMapping = new HashMap();

	private Map exportableMapping = new HashMap();

	private Map resultMapping = new HashMap();

	private Map parameterMapping = new HashMap();

	private int maxRows;

	private String martName;

	private String datasetName;

	private Map attributes = new TreeMap();

	private Map filters = new TreeMap();

	/**
	 * Constructs a sub query which will refer to the given registry to find out
	 * attributes etc.
	 * 
	 * @param registry
	 *            the registry to look things up in.
	 */
	public SubQuery(Registry registry) {
		this.registry = registry;
	}

	/**
	 * Obtain the registry we're working with.
	 * 
	 * @return the registry.
	 */
	Registry getRegistry() {
		return this.registry;
	}

	/**
	 * Opens a connection. The connection details are expected to be supplied
	 * via the constructor, which will probably pass in a
	 * {@link RegistryConnection} from which this object can extract the info it
	 * needs.
	 * 
	 * @throws SQLException
	 *             if the connection could not be opened.
	 */
	abstract void open() throws SQLException;

	/**
	 * Closes the connection. Silently ignores it if the connection is already
	 * closed.
	 * 
	 * @throws SQLException
	 *             if the connection could not be closed.
	 */
	void close() throws SQLException {
		this.closeSubQuery();
		for (final Iterator i = this.subQueries.iterator(); i.hasNext();)
			try {
				((SubQuery) i.next()).close();
			} catch (final Exception e) {
				// Don't care.
			}
	}

	/**
	 * Closes only this subquery.
	 * 
	 * @throws SQLException
	 *             if it could not be closed.
	 */
	abstract void closeSubQuery() throws SQLException;

	/**
	 * Returns the next row of the query.
	 * 
	 * @return a list of Object[] rows of results.
	 * @throws SQLException
	 *             if anything went wrong getting the results.
	 */
	abstract Object[] getNextRow() throws SQLException;

	/**
	 * Check to see if this query has another row to return. Generally only
	 * applies to a query with no importables. Other queries need not worry.
	 * 
	 * @return <tt>true</tt> if there are more rows.
	 * @throws SQLException
	 *             if the check could not be made.
	 */
	abstract boolean hasNextRow() throws SQLException;

	public void finalize() {
		try {
			this.close();
		} catch (SQLException e) {
			// We don't care.
		}
	}

	/**
	 * Adds a subquery.
	 * 
	 * @param subQuery
	 *            the subquery to add.
	 * @throws SQLException
	 *             if something went wrong.
	 */
	void addSubQuery(SubQuery subQuery) throws SQLException {
		this.subQueries.add(subQuery);
	}

	/**
	 * Returns all subqueries that depend on this query executing. The list may
	 * be empty but never <tt>null</tt>.
	 * 
	 * @return a list of subqueries.
	 */
	Collection getAllSubQueries() {
		return this.subQueries;
	}

	/**
	 * Sets a parent subquery.
	 * 
	 * @param subQuery
	 *            the subquery to use as a parent.
	 * @throws SQLException
	 *             if something went wrong.
	 */
	void setParentSubQuery(SubQuery subQuery) throws SQLException {
		this.parentSubQuery = subQuery;
	}

	/**
	 * Gets the subquery that this one depends on.
	 * 
	 * @return the parent subquery. <tt>null</tt> if there isn't one.
	 */
	SubQuery getParentSubQuery() {
		return this.parentSubQuery;
	}

	/**
	 * Return a map that maps parameters (filters) in the subquery to the
	 * equivalent parameters in the main {@link Query}. Both are 1-indexed. The
	 * key is the subquery index and the value is the main query index.
	 * 
	 * @return the map of parameters. May be empty but never <tt>null</tt>.
	 */
	Map getParameterMapping() {
		return this.parameterMapping;
	}

	/**
	 * Return a map that maps results (attributes) in the subquery to the
	 * equivalent results in the main {@link Query}. Both are 1-indexed. The
	 * key is the subquery index and the value is the main query index.
	 * 
	 * @return the map of results. May be empty but never <tt>null</tt>.
	 */
	Map getResultMapping() {
		return this.resultMapping;
	}

	/**
	 * Return a map that maps exportables (results) in the subquery to the order
	 * they need to be in (e.g. 1,2,3). Both are 1-indexed. The key is the
	 * subquery index and the value is the main query index.
	 * 
	 * @return the map of exportables. May be empty but never <tt>null</tt>.
	 */
	Map getExportableMapping() {
		return this.exportableMapping;
	}

	/**
	 * Return a map that maps importables (parameters) in the subquery to the
	 * order they need to be in (e.g. 1,2,3). Both are 1-indexed. The key is the
	 * subquery index and the value is the main query index.
	 * 
	 * @return the map of exportables. May be empty but never <tt>null</tt>.
	 */
	Map getImportableMapping() {
		return this.importableMapping;
	}

	/**
	 * Set a map that maps parameters (filters) in the subquery to the
	 * equivalent parameters in the main {@link Query}. Both are 1-indexed. The
	 * key is the subquery index and the value is the main query index.
	 * 
	 * @param mapping
	 *            the map of parameters. May be empty but never <tt>null</tt>.
	 */
	void setParameterMapping(Map mapping) {
		this.parameterMapping = mapping;
	}

	/**
	 * Return a map that maps results (attributes) in the subquery to the
	 * equivalent results in the main {@link Query}. Both are 1-indexed. The
	 * key is the subquery index and the value is the main query index.
	 * 
	 * @param mapping
	 *            the map of results. May be empty but never <tt>null</tt>.
	 */
	void setResultMapping(Map mapping) {
		this.resultMapping = mapping;
	}

	/**
	 * Return a map that maps exportables (results) in the subquery to the order
	 * they need to be in (e.g. 1,2,3). Both are 1-indexed. The key is the
	 * subquery index and the value is the main query index.
	 * 
	 * @param mapping
	 *            the map of exportables. May be empty but never <tt>null</tt>.
	 */
	void setExportableMapping(Map mapping) {
		this.exportableMapping = mapping;
	}

	/**
	 * Return a map that maps importables (parameters) in the subquery to the
	 * order they need to be in (e.g. 1,2,3). Both are 1-indexed. The key is the
	 * subquery index and the value is the main query index.
	 * 
	 * @param mapping
	 *            the map of exportables. May be empty but never <tt>null</tt>.
	 */
	void setImportableMapping(Map mapping) {
		this.importableMapping = mapping;
	}

	/**
	 * Set max rows. Only useful for JDBC versions.
	 * 
	 * @see Statement#setMaxRows(int)
	 * @param rows
	 *            the rows.
	 * @throws SQLException
	 *             if there is something wrong with the map.
	 */
	void setMaxRows(int rows) throws SQLException {
		if (rows < 0)
			throw new SQLException(Resources.get("maxRowMinZero"));
		this.maxRows = rows;
	}

	/**
	 * Set max rows. The subquery should never return more than this number of
	 * rows.
	 * 
	 * @see Statement#setMaxRows(int)
	 * @return the rows.
	 */
	protected int getMaxRows() {
		return this.maxRows;
	}

	/**
	 * What mart name did this column come from?
	 * 
	 * @return the name of the mart the column came from.
	 */
	String getMartName() {
		return this.martName;
	}

	/**
	 * What dataset name did this column come from?
	 * 
	 * @return the name of the dataset the column came from.
	 */
	String getDatasetName() {
		return this.datasetName;
	}

	/**
	 * What mart name did this column come from?
	 * 
	 * @param name
	 *            the name of the mart.
	 * @throws SQLException
	 *             if the name is not recognised.
	 */
	void setMartName(String name) throws SQLException {
		this.martName = name;
	}

	/**
	 * What dataset name did this column come from?
	 * 
	 * @param name
	 *            the name of the dataset.
	 * @throws SQLException
	 *             if the name is not recognised.
	 */
	void setDatasetName(String name) throws SQLException {
		if (this.martName == null)
			throw new SQLException(Resources.get("setMartFirst"));
		this.datasetName = name;
	}

	/**
	 * Adds an attribute column.
	 * 
	 * @param index
	 *            the position to add the attribute column at.
	 * @param col
	 *            the attribute column to add.
	 */
	void addAttribute(int index, String col) {
		this.attributes.put(new Integer(index), col);
	}

	/**
	 * Returns the selected attribute columns in order.
	 * 
	 * @return the selected attribute columns.
	 * @throws SQLException
	 *             if something went wrong.
	 */
	protected Collection getAttributes() throws SQLException {
		return this.attributes.values();
	}

	/**
	 * Adds a filter column.
	 * 
	 * @param index
	 *            the position to add the filter column at.
	 * @param col
	 *            the filter column to add.
	 */
	void addFilter(int index, String col) {
		this.filters.put(new Integer(index), col);
	}

	/**
	 * Returns the selected filter columns in order.
	 * 
	 * @return the selected filter columns.
	 * @throws SQLException
	 *             if something went wrong.
	 */
	protected Collection getFilters() throws SQLException {
		return this.filters.values();
	}

	/**
	 * This inner interface defines ways to interact with XML sub queries.
	 */
	public class XMLSubQuery extends SubQuery {
		private URL serverURL;

		private Map parameters;

		private int rowsReturned;

		private BufferedReader br;

		private Object[] nextRow;

		/**
		 * Delegates to {@link SubQuery#SubQuery(Registry)}.
		 * 
		 * @param registry
		 */
		public XMLSubQuery(Registry registry) {
			super(registry);
		}

		void open() throws SQLException {
			if (this.br != null)
				return;
			try {
				final XMLMart mart = (XMLMart) this.getRegistry().getMart(
						this.getMartName());
				this.serverURL = mart.getServerURL();
			} catch (RegistryException e) {
				final SQLException se = new SQLException();
				se.initCause(e);
				throw se;
			}
			this.rowsReturned = 0;
			this.parameters = new HashMap();
		}

		private void startQuery() throws SQLException {
			if (this.parameters == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			// Construct Query XML
			final StringBuffer queryXML = new StringBuffer();
			// TODO Construct the XML, taking note of any importables set.
			// Submit the query.
			try {
				final URLConnection conn = this.serverURL.openConnection();
				conn.addRequestProperty("query", queryXML.toString());
				conn.connect();
				this.br = new BufferedReader(new InputStreamReader(conn
						.getInputStream()));
			} catch (final IOException ie) {
				final SQLException e = new SQLException();
				e.initCause(ie);
				throw e;
			}
			// Read the first row.
			this.readNextRow();
		}

		private void readNextRow() throws SQLException {
			if (this.br == null)
				return;
			try {
				final String line = this.br.readLine();
				if (line == null)
					this.nextRow = null;
				else
					this.nextRow = line.split("\t");
			} catch (final IOException ie) {
				final SQLException se = new SQLException();
				se.initCause(ie);
				throw se;
			}
		}

		void closeSubQuery() throws SQLException {
			if (this.br != null)
				try {
					this.br.close();
				} catch (final IOException ie) {
					final SQLException se = new SQLException();
					se.initCause(ie);
					throw se;
				}
		}

		Object[] getNextRow() throws SQLException {
			if (!this.hasNextRow())
				return new Object[0];
			final Object[] currentRow = this.nextRow;
			this.rowsReturned++;
			this.readNextRow();
			return currentRow;
		}

		boolean hasNextRow() throws SQLException {
			if (this.br == null)
				this.startQuery();
			return (this.nextRow != null)
					&& (this.getMaxRows() != 0 && this.rowsReturned < this
							.getMaxRows());
		}

		/**
		 * Set a parameter as a string value. Parameter is 1-indexed.
		 * 
		 * @param paramIndex
		 *            the index.
		 * @param value
		 *            the value.
		 * @throws SQLException
		 *             if the index is out of range.
		 */
		void setParameter(int paramIndex, String value) throws SQLException {
			if (this.parameters == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			if (paramIndex < 1 || paramIndex > this.getFilters().size())
				throw new SQLException(Resources.get("paramIndexOutRange", ""
						+ paramIndex));
			this.parameters.put(new Integer(paramIndex - 1), value == null ? ""
					: value);
		}
	}

	/**
	 * This inner interface defines ways to interact with JDBC sub queries.
	 */
	public class JDBCSubQuery extends SubQuery {

		private JDBCMart mart;

		private Connection conn;

		private PreparedStatement stmt;

		private ResultSet rs;

		private int colCount;

		/**
		 * Delegates to {@link SubQuery#SubQuery(Registry)}.
		 * 
		 * @param registry
		 */
		public JDBCSubQuery(Registry registry) {
			super(registry);
		}

		void open() throws SQLException {
			if (this.stmt != null)
				return;
			try {
				this.mart = (JDBCMart) this.getRegistry().getMart(
						this.getMartName());
			} catch (RegistryException e) {
				final SQLException se = new SQLException();
				se.initCause(e);
				throw se;
			}
			this.conn = mart.getConnection();
			// TODO - construct SQL from attributes, filters,
			// mart name, dataset name, etc.
			String sql = "";
			this.stmt = conn.prepareStatement(sql);
		}

		void closeSubQuery() throws SQLException {
			try {
				this.rs.close();
			} catch (SQLException e) {
				// We don't care.
			}
			try {
				this.stmt.close();
			} catch (SQLException e) {
				// We still don't care.
			}
			try {
				this.conn.close();
			} catch (SQLException e) {
				// We still don't care.
			} finally {
				this.mart.connectionClosed(conn);
			}
		}

		private void startQuery() throws SQLException {
			if (this.stmt == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			this.stmt.execute();
			this.rs = this.stmt.getResultSet();
			this.colCount = this.rs.getMetaData().getColumnCount();
		}

		Object[] getNextRow() throws SQLException {
			if (!this.hasNextRow() || !this.rs.next())
				return new Object[0];
			final List row = new ArrayList();
			for (int i = 1; i <= this.colCount; i++)
				row.add(i - 1, this.rs.getObject(i));
			return row.toArray();
		}

		boolean hasNextRow() throws SQLException {
			if (this.rs == null)
				this.startQuery();
			return !this.rs.isLast();
		}

		void setMaxRows(int rows) throws SQLException {
			super.setMaxRows(rows);
			if (this.stmt == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			this.stmt.setMaxRows(rows);
		}

		/**
		 * Set type translation map. Only useful for JDBC versions.
		 * 
		 * @see Connection#setTypeMap(Map)
		 * @param typeMap
		 *            the type map.
		 * @throws SQLException
		 *             if there is something wrong with the map.
		 */
		void setTypeMap(Map typeMap) throws SQLException {
			if (this.stmt == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			this.stmt.getConnection().setTypeMap(typeMap);
		}

		/**
		 * Set max field size. Only useful for JDBC versions.
		 * 
		 * @see Statement#setMaxFieldSize(int)
		 * @param size
		 *            the field size.
		 * @throws SQLException
		 *             if there is something wrong with the map.
		 */
		void setMaxFieldSize(int size) throws SQLException {
			if (this.stmt == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			this.stmt.setMaxFieldSize(size);
		}

		/**
		 * Obtains the prepared statement, so that we can set parameters
		 * directly on it.
		 * 
		 * @return the prepared statement.
		 * @throws SQLException
		 *             if there is a problem.
		 */
		PreparedStatement getPreparedStatement() throws SQLException {
			if (this.stmt == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			return this.stmt;
		}

		/**
		 * Obtains metadata about the columns in this query, so that we can use
		 * them to compile our own.
		 * 
		 * @return the meta data.
		 * @throws SQLException
		 *             if there is a problem.
		 */
		ResultSetMetaData getMetaData() throws SQLException {
			if (this.stmt == null)
				throw new SQLException(Resources.get("subqueryNotOpen"));
			return this.rs.getMetaData();
		}
	}
}
