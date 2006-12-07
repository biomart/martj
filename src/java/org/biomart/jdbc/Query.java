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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.jdbc.SubQuery.JDBCSubQuery;
import org.biomart.jdbc.SubQuery.XMLSubQuery;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class Query {

	/**
	 * An unknown object type to be passed straight through.
	 */
	static final int INFERRED = -100;

	/**
	 * An ascii stream object type.
	 */
	static final int ASCII_STREAM = -101;

	/**
	 * A big decimal object type.
	 */
	static final int BIG_DECIMAL = -102;

	/**
	 * A binary stream object type.
	 */
	static final int BINARY_STREAM = -103;

	/**
	 * A boolean object type.
	 */
	static final int BOOLEAN = -104;

	/**
	 * A byte object type.
	 */
	static final int BYTE = -105;

	/**
	 * A byte[] object type.
	 */
	static final int BYTES = -106;

	/**
	 * A character stream object type.
	 */
	static final int CHARACTER_STREAM = -107;

	/**
	 * A long object type.
	 */
	static final int LONG = -108;

	/**
	 * A short object type.
	 */
	static final int SHORT = -109;

	/**
	 * A time object type.
	 */
	static final int TIME = -110;

	/**
	 * A timestamp object type.
	 */
	static final int TIMESTAMP = -111;

	/**
	 * A URL object type.
	 */
	static final int URL = -112;

	/**
	 * A unicode stream object type.
	 */
	static final int UNICODE_STREAM = -113;

	// What statement do we belong to?
	private QueryStatement stmt;

	// Reference to the column names in our results.
	private List columnNames = new ArrayList();

	// Are we initialised?
	private boolean initialised = false;

	// Parameters.
	private QueryParam[] queryParams = new QueryParam[0];

	// Metadata about results.
	private ResultSetMetaData resultSetMetaData;

	// Are we open?
	private boolean closed = false;

	// Our sub query has pointers to the other sub queries.
	private SubQuery subQuery;

	/**
	 * Create and compiles a query object based around the given SQL.
	 * 
	 * @param stmt
	 *            the statement object we are associated with.
	 * @param sql
	 *            the SQL to build the query out of.
	 * @throws SQLException
	 *             if the compilation fails.
	 */
	Query(final QueryStatement stmt, final String sql) throws SQLException {
		this.stmt = stmt;
		this.compile(sql);
	}

	/**
	 * Obtains an ordered list of the column names in the results.
	 * 
	 * @return the list of column names that will appear in the results.
	 */
	List getColumnNames() {
		return this.columnNames;
	}

	private void compile(final String sql) throws SQLException {
		// TODO - method stub
		// Populate column names and size of query params array.
		// Create subqueries but don't open any connections yet.
		// Note that we use 1-indexed maps for attribute/exportable
		// /importable/filter positions for both ourselves and the
		// subquery objects.
		// Use catalog from connection if specified, otherwise
		// use one given in query, otherwise use default catalog
		// from registry. (Catalog=Mart)
		// Use schemas as given in query, otherwise use default
		// schema from mart in registry. (Schema=Dataset)
	}

	/**
	 * Clears the query parameters that have been set so far.
	 */
	void clearParameters() {
		for (int i = 0; i < this.queryParams.length; i++)
			this.queryParams[i] = null;
	}

	/**
	 * Closes all connections this query may have open at present.
	 * 
	 * @throws SQLException
	 *             if the connections cannot be closed.
	 */
	public void close() throws SQLException {
		if (this.isClosed() || !this.initialised)
			return;
		final List subQueries = new ArrayList();
		subQueries.add(this.subQuery);
		for (int i = 0; i < subQueries.size(); i++) {
			final SubQuery sq = (SubQuery) subQueries.get(i);
			try {
				sq.close();
			} catch (SQLException e) {
				// Ignore. We don't care.
			}
			subQueries.addAll(sq.getAllSubQueries());
		}
		// Close all query connections.
		this.closed = true;
	}

	/**
	 * Checks to see if this query has been closed or not.
	 * 
	 * @return <tt>true</tt> if the query is closed.
	 */
	public boolean isClosed() {
		return this.closed;
	}

	public void finalize() {
		try {
			if (!this.isClosed())
				this.close();
		} catch (SQLException e) {
			// We don't care. Ignore it.
		}
	}

	/**
	 * Sets a query parameter.
	 * 
	 * @param parameterIndex
	 *            the index of the parameter to set.
	 * @param param
	 *            the value to give the parameter.
	 * @throws SQLException
	 *             if there was a problem, for instance if the parameter was out
	 *             of range.
	 */
	void setParameter(int parameterIndex, QueryParam param) throws SQLException {
		if (parameterIndex < 0 || parameterIndex >= this.queryParams.length)
			throw new SQLException(Resources.get("paramIndexOutRange", ""
					+ parameterIndex));
		this.queryParams[parameterIndex-1] = param;
	}

	private void initialise() throws SQLException {
		for (int i = 0; i < this.queryParams.length; i++)
			if (this.queryParams[i] == null)
				throw new SQLException(Resources.get("paramMissing", "" + i+1));
		final List subQueries = new ArrayList();
		subQueries.add(this.subQuery);
		for (int i = 0; i < subQueries.size(); i++) {
			final SubQuery sq = (SubQuery) subQueries.get(i);
			subQueries.addAll(sq.getAllSubQueries());
			// Open query connection.
			sq.open();
			// Set the type maps etc. on each from our own connection.
			// This applies to JDBC connections only.
			if (sq instanceof JDBCSubQuery) {
				final JDBCSubQuery jsq = (JDBCSubQuery)sq;
				jsq.setTypeMap(this.stmt.getConnection().getTypeMap());
				jsq.setMaxFieldSize(this.stmt.getMaxFieldSize());
			}
			// Set the parameters.
			final Map paramMap = sq.getParameterMapping();
			for (final Iterator m = paramMap.entrySet().iterator(); m.hasNext();) {
				final Map.Entry me = (Map.Entry) m.next();
				final int sqIndex = ((Integer) me.getKey()).intValue();
				final int qIndex = ((Integer) me.getValue()).intValue();
				final QueryParam param = this.queryParams[qIndex-1];
				if (sq instanceof XMLSubQuery)
					((XMLSubQuery) sq).setParameter(sqIndex, param
							.toXMLParameter());
				else if (sq instanceof JDBCSubQuery)
					param.toJDBCParameter(((JDBCSubQuery) sq)
							.getPreparedStatement(), sqIndex);
				else
					throw new RuntimeException(); // Should never happen.
			}
		}
		// Set the max rows on the first one only from our own statement.
		this.subQuery.setMaxRows(this.stmt.getMaxRows());
		// Populate resultset metadata by copying from connections (default
		// to string).
		this.resultSetMetaData = new SubQueryMetaData(this, this.subQuery);
	}

	/**
	 * Returns the next row from the query. Note that this is a list because the
	 * next row could actually be several rows, depending on how the joins to
	 * other datasets multiply it (or not). Each member of the list will be an
	 * object array. If there is no current row, the returned list will be
	 * empty.
	 * 
	 * @return a list of rows returned. The list will never be <tt>null</tt>.
	 * @throws SQLException
	 *             if something goes wrong fetching the rows.
	 */
	public List getNextRow() throws SQLException {
		// Nothing more? Don't do it then.
		if (!this.hasMoreRows())
			return Collections.EMPTY_LIST;
		// Set up a place to hold results.
		List results = new ArrayList();
		// Process first query.
		// First row will always be a single row.
		final Object[] firstRow = new Object[this.columnNames.size()];
		Map colMap = this.subQuery.getResultMapping();
		final Object[] firstSubQueryRow = (Object[]) this.subQuery.getNextRow().get(0);
		for (final Iterator m = colMap.entrySet().iterator(); m.hasNext();) {
			final Map.Entry me = (Map.Entry) m.next();
			final int sqIndex = ((Integer) me.getKey()).intValue();
			final int qIndex = ((Integer) me.getValue()).intValue();
			firstRow[qIndex-1] = firstSubQueryRow[sqIndex-1];
		}
		results.add(firstRow);
		// Set up a place to hold all exported values for each subquery.
		final Map exportedValues = new HashMap();
		// Process exportables from first row.
		// First row will always be a single row of exportables.
		Map expMap = this.subQuery.getExportableMapping();
		final Object[] firstExports = new Object[expMap.size()];
		for (final Iterator m = expMap.entrySet().iterator(); m.hasNext();) {
			final Map.Entry me = (Map.Entry) m.next();
			final int sqIndex = ((Integer) me.getKey()).intValue();
			final int qIndex = ((Integer) me.getValue()).intValue();
			firstExports[qIndex-1] = firstSubQueryRow[sqIndex-1];
		}		
		exportedValues.put(this.subQuery, Collections.singletonList(firstExports));
		// Somewhere to keep track of combinations of importables
		// and sub queries so that we don't execute them twice
		// for the same importables. 
		final Map seenImportables = new HashMap();
		// Somewhere to keep track of the exportables produced
		// by each set of importables. 
		final Map seenExportables = new HashMap();
		// Process recursive links to child subqueries.
		final List subQueries = new ArrayList();
		subQueries.add(this.subQuery.getAllSubQueries());
		for (int i = 0; i < subQueries.size(); i++) {
			// Get the child and parent subqueries.
			final SubQuery sq = (SubQuery) subQueries.get(i);
			final SubQuery parent = sq.getParentSubQuery();
			// What rows are we importing into this subquery?
			// The list contains one set of values per row in the parent query.
			final List importedValues = (List) exportedValues.get(parent);
			// Somewhere to store the combined rows.
			final List combinedRows = new ArrayList();
			// Somewhere to store the multiplied exportables from the parent.
			final List extendedParentExportedValues = new ArrayList();
			// Somewhere to store all the exported values from this subquery,
			// one set of exported values per row of combined results.
			final List sqExportedValues = new ArrayList();
			// Loop over each input row, which we can gather from the
			// size of the importedValues set which will have one set
			// of values for each parent row.
			for (int parentRowNum = 0; parentRowNum < importedValues.size(); parentRowNum++) {
				final Object[] importedValue = (Object[]) importedValues.get(parentRowNum);
				final List importedRows = seenImportables.containsKey(importedValue) ? (List)seenImportables.get(importedValue) : new ArrayList();
				final List exportedRows = seenExportables.containsKey(importedValue) ? (List)seenExportables.get(importedValue) : new ArrayList();
				// If not seen this subquery/importables combo before,
				// populate it and remember results.
				if (importedRows.isEmpty()){
					// Set up the importable into the subquery.
					final Map impMap = sq.getImportableMapping();
					for (final Iterator m = impMap.entrySet().iterator(); m
							.hasNext();) {
						final Map.Entry me = (Map.Entry) m.next();
						final int sqIndex = ((Integer) me.getKey()).intValue();
						final int qIndex = ((Integer) me.getValue()).intValue();
						firstExports[qIndex-1] = firstRow[sqIndex-1];
						if (sq instanceof XMLSubQuery)
							((XMLSubQuery) sq).setParameter(sqIndex, ""
									+ importedValue[qIndex-1]);
						else if (sq instanceof JDBCSubQuery)
							((JDBCSubQuery) sq).getPreparedStatement().setObject(
									sqIndex, importedValue[qIndex-1]);
						else
							throw new RuntimeException(); // Should never happen.
					}
					// Get exportable and column mappings from subquery.
					expMap = sq.getExportableMapping();
					colMap = sq.getResultMapping();
					// Execute query and loop over results.
					for (final Iterator nextRowIterator = sq.getNextRow().iterator(); nextRowIterator.hasNext();) {
						// For each row returned, we need to construct a template
						// the same size as the eventual combined results of the whole
						// query, and populate it accordingly. This template will
						// be used later to construct the combined row.
						final Object[] sqRow = (Object[]) nextRowIterator.next();
						final Object[] importedRow = new Object[this.columnNames.size()];
						for (final Iterator m = colMap.entrySet().iterator(); m
								.hasNext();) {
							final Map.Entry me = (Map.Entry) m.next();
							final int sqIndex = ((Integer) me.getKey()).intValue();
							final int qIndex = ((Integer) me.getValue()).intValue();
							importedRow[qIndex-1] = sqRow[sqIndex-1];
						}
						importedRows.add(importedRow);
						// For each row returned, we also need to grab the
						// exportable values and remember those too.
						expMap = sq.getExportableMapping();
						final Object[] exportedRow = new Object[expMap.size()];
						for (final Iterator m = expMap.entrySet().iterator(); m
								.hasNext();) {
							final Map.Entry me = (Map.Entry) m.next();
							final int sqIndex = ((Integer) me.getKey()).intValue();
							final int qIndex = ((Integer) me.getValue()).intValue();
							exportedRow[qIndex-1] = sqRow[sqIndex-1];
						}
						exportedRows.add(exportedRow);
					}
					seenImportables.put(importedValue, importedRows);
					seenExportables.put(importedValue, exportedRows);
				}
				// Now that we have results from the subquery, loop over
				// each row from the subquery and merge it with the parent
				// row. Remember the combinations.
				final Object[] combinedRow = (Object[]) results.get(parentRowNum);
				for (final Iterator importedRowIterator = importedRows.iterator(); importedRowIterator.hasNext(); ) {
					final Object[] importedRow = (Object[])importedRowIterator.next();
					for (int impColNum = 0; impColNum < importedRow.length; impColNum++)
						if (importedRow[impColNum]!=null)
							combinedRow[impColNum] = importedRow[impColNum];
					combinedRows.add(combinedRow);
				}	
				// Add entries to the exported rows for each of the
				// imported rows, which will be in the same order.
				// We do this several times so that the exported values
				// become 1:1 with each row so that they can be imported
				// into the nextquery.
				sqExportedValues.addAll(exportedRows);
				// Replicate parent exported rows to prevent size
				// mismatches.
				for (int x = 0; x < exportedRows.size(); x++)
					extendedParentExportedValues.add(importedValue);
			}
			// Update the results so far to contain all the combined rows.
			results = combinedRows;
			exportedValues.put(sq, sqExportedValues);
			exportedValues.put(parent, extendedParentExportedValues);
			// Add any further subqueries to the end of our list.
			subQueries.addAll(sq.getAllSubQueries());
		}
		return results;
	}

	/**
	 * Checks to see if there going to be any more rows returned by this query.
	 * 
	 * @return <tt>true</tt> if there are.
	 * @throws SQLException
	 *             if there was a problem checking.
	 */
	public boolean hasMoreRows() throws SQLException {
		if (!this.initialised)
			this.initialise();
		return this.subQuery.hasNextRow();
	}

	/**
	 * Obtains resultset metadata, which is populate by the initialise method.
	 * 
	 * @return the meta data.
	 * @throws SQLException
	 *             if the data could not be compiled.
	 */
	public ResultSetMetaData getResultSetMetaData() throws SQLException {
		if (!this.initialised)
			this.initialise();
		return this.resultSetMetaData;
	}

	/**
	 * Provides parameterisation abilities.
	 */
	static class QueryParam {
		private Object value;

		private int sqlType;

		private String sqlTypeName;

		private Object operator;

		/**
		 * Constructs a simple param with a simple type.
		 * 
		 * @param value
		 *            the value for the param.
		 * @param sqlType
		 *            the type for the param. See {@link Types}.
		 */
		public QueryParam(Object value, int sqlType) {
			this(value, sqlType, null);
		}

		/**
		 * Constructs a simple param with a simple type and name.
		 * 
		 * @param value
		 *            the value for the param.
		 * @param sqlType
		 *            the type for the param. See {@link Types}.
		 * @param sqlTypeName
		 *            the name of the type.
		 */
		public QueryParam(Object value, int sqlType, String sqlTypeName) {
			this(value, sqlType, sqlTypeName, null);
		}

		/**
		 * Constructs a simple param with a simple type, name and an optional
		 * additional parameter for use in conversion.
		 * 
		 * @param value
		 *            the value for the param.
		 * @param sqlType
		 *            the type for the param. See {@link Types}.
		 * @param sqlTypeName
		 *            the name of the type.
		 * @param operator
		 *            an optional parameter to use during conversion of the
		 *            value into a database value.
		 */
		public QueryParam(Object value, int sqlType, String sqlTypeName,
				Object operator) {
			this.value = value;
			this.sqlType = sqlType;
			this.sqlTypeName = sqlTypeName;
			this.operator = operator;
		}

		/**
		 * Converts this parameter into a string for use with XML queries.
		 * 
		 * @return the converted string. Never <tt>null</tt> but may be empty.
		 * @throws SQLException
		 *             if it cannot be converted.
		 */
		public String toXMLParameter() throws SQLException {
			// Check each of the types we recognise.
			if (this.value == null)
				return "null";
			else
				switch (this.sqlType) {
				case Query.ASCII_STREAM:
				case Query.BINARY_STREAM:
				case Query.UNICODE_STREAM:
					final byte[] bytes = new byte[((Integer) this.operator)
							.intValue()];
					try {
						((InputStream) this.value).read(bytes);
					} catch (IOException e) {
						final SQLException se = new SQLException();
						se.initCause(e);
						throw se;
					}
					return new String(bytes);
				case Query.BYTES:
					return new String((byte[]) this.value);
				case Query.CHARACTER_STREAM:
					final char[] chars = new char[((Integer) this.operator)
							.intValue()];
					try {
						((Reader) this.value).read(chars);
					} catch (IOException e) {
						final SQLException se = new SQLException();
						se.initCause(e);
						throw se;
					}
					return new String(chars);
				default:
					// Otherwise, default.
					return "" + this.value;
				}
		}

		/**
		 * Assigns this parameter to the given JDBC statement.
		 * 
		 * @param stmt
		 *            the statement to assign to.
		 * @param index
		 *            the index in the statement to assign to.
		 * @throws SQLException
		 *             if it cannot be converted.
		 */
		public void toJDBCParameter(final PreparedStatement stmt,
				final int index) throws SQLException {
			// Check each of the types we recognise.
			if (this.value == null)
				stmt.setNull(index, this.sqlType, this.sqlTypeName);
			else
				switch (this.sqlType) {
				case Query.INFERRED:
					stmt.setObject(index, this.value);
					break;
				case Query.ASCII_STREAM:
					stmt.setAsciiStream(index, (InputStream) this.value,
							((Integer) this.operator).intValue());
					break;
				case Query.BIG_DECIMAL:
					stmt.setBigDecimal(index, (BigDecimal) this.value);
					break;
				case Query.BINARY_STREAM:
					stmt.setBinaryStream(index, (InputStream) this.value,
							((Integer) this.operator).intValue());
					break;
				case Query.BOOLEAN:
					stmt.setBoolean(index, ((Boolean) this.value)
							.booleanValue());
					break;
				case Query.BYTE:
					stmt.setByte(index, ((Byte) this.value).byteValue());
					break;
				case Query.BYTES:
					stmt.setBytes(index, (byte[]) this.value);
					break;
				case Query.CHARACTER_STREAM:
					stmt.setCharacterStream(index, (Reader) this.value,
							((Integer) this.operator).intValue());
					break;
				case Types.DATE:
					if (this.operator != null
							&& this.operator instanceof Calendar)
						stmt.setDate(index, (Date) this.value,
								(Calendar) this.operator);
					else
						stmt.setDate(index, (Date) this.value);
					break;
				case Types.DOUBLE:
					stmt.setDouble(index, ((Double) this.value).doubleValue());
					break;
				case Types.FLOAT:
					stmt.setFloat(index, ((Float) this.value).floatValue());
					break;
				case Types.INTEGER:
					stmt.setInt(index, ((Integer) this.value).intValue());
					break;
				case Query.LONG:
					stmt.setLong(index, ((Long) this.value).longValue());
					break;
				case Query.SHORT:
					stmt.setShort(index, ((Short) this.value).shortValue());
					break;
				case Types.TIME:
					if (this.operator != null
							&& this.operator instanceof Calendar)
						stmt.setTime(index, (Time) this.value,
								(Calendar) this.operator);
					else
						stmt.setTime(index, (Time) this.value);
					break;
				case Types.TIMESTAMP:
					if (this.operator != null
							&& this.operator instanceof Calendar)
						stmt.setTimestamp(index, (Timestamp) this.value,
								(Calendar) this.operator);
					else
						stmt.setTimestamp(index, (Timestamp) this.value);
					break;
				case Query.URL:
					stmt.setURL(index, (URL) this.value);
					break;
				case Query.UNICODE_STREAM:
					stmt.setUnicodeStream(index, (InputStream) this.value,
							((Integer) this.operator).intValue());
					break;
				default:
					// Otherwise, default.
					if (this.operator != null
							&& this.operator instanceof Integer)
						stmt.setObject(index, this.value, this.sqlType,
								((Integer) this.operator).intValue());
					else
						stmt.setObject(index, this.value, this.sqlType);
					break;
				}
		}
	}
}
