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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.biomart.jdbc.Query.QueryParam;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class QueryStatement implements PreparedStatement {

	// What fetch size do we have? Defaults to 100.
	private int fetchSize = 100;

	// What connection are we based on?
	private RegistryConnection registryConn;

	// Do we have a query in storage?
	private Query query;

	// Are we closed?
	private boolean closed = false;

	// Do we have warnings?
	private SQLWarning warnings;

	// Do we have current result sets?
	private Collection openResultSets = new HashSet();

	// Do we have a limited number of results?
	private int maxRowCount = 0;

	// Do we restrict string/lob sizes?
	private int maxColSize = 0;

	/**
	 * Constructs a new simple statement associated with the given connection.
	 * The execute() statements that do not accept SQL as a parameter will not
	 * work. Those that do accept SQL will work as normal.
	 * 
	 * @param conn
	 *            the connection this statement is part of.
	 */
	public QueryStatement(RegistryConnection conn) {
		this.registryConn = conn;
		this.query = null;
	}

	/**
	 * Constructs a new query statement associated with the given connection.
	 * Uses the given SQL to use in execute() statements that do not accept SQL
	 * as a parameter. For those execute() statements that do
	 * 
	 * 
	 * @param conn
	 *            the connection this statement is part of.
	 * @param sql
	 *            the SQL we are going to execute.
	 * @throws SQLException
	 *             if the SQL is not valid.
	 */
	public QueryStatement(RegistryConnection conn, String sql)
			throws SQLException {
		this.registryConn = conn;
		this.query = new Query(this, sql);
	}

	private boolean isClosed() {
		return this.closed;
	}

	private void ensureOpen() throws SQLException {
		if (this.isClosed())
			throw new SQLException(Resources.get("closedStatement"));
	}

	public void addBatch() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void clearParameters() throws SQLException {
		if (this.query == null)
			throw new SQLException(Resources.get("plainStmtReqSQL"));
		this.query.clearParameters();
	}

	private ResultSet doExecuteQuery(Query query) throws SQLException {
		this.ensureOpen();
		ResultSet rs = new QueryResultSet(this, query);
		rs.setFetchSize(this.getFetchSize());
		synchronized (this.openResultSets) {
			this.openResultSets.add(rs);
		}
		return rs;
	}

	/**
	 * Informs us that the result set has been closed.
	 * 
	 * @param rs
	 *            the result set that has been closed.
	 */
	void resultSetClosed(final ResultSet rs) {
		synchronized (this.openResultSets) {
			this.openResultSets.remove(rs);
		}
	}

	public boolean execute() throws SQLException {
		return this.executeQuery() != null;
	}

	public ResultSet executeQuery() throws SQLException {
		if (this.query == null)
			throw new SQLException(Resources.get("plainStmtReqSQL"));
		return this.doExecuteQuery(this.query);
	}

	public int executeUpdate() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		// We can't do this, and the API excuses us from having to.
		return null;
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		// We don't support this.
		return null;
	}

	public void setArray(int i, Array x) throws SQLException {
		this.setObject(i, x, Types.ARRAY);
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		this.setObject(parameterIndex, x, Query.ASCII_STREAM, null, new Integer(length));
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x)
			throws SQLException {
		this.setObject(parameterIndex, x, Query.BIG_DECIMAL);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		this.setObject(parameterIndex, x, Query.BINARY_STREAM, null, new Integer(length));
	}

	public void setBlob(int i, Blob x) throws SQLException {
		this.setObject(i, x, Types.BLOB);
	}

	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		this.setObject(parameterIndex, Boolean.valueOf(x), Query.BOOLEAN);
	}

	public void setByte(int parameterIndex, byte x) throws SQLException {
		this.setObject(parameterIndex, Byte.toString(x), Query.BYTE);
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		this.setObject(parameterIndex, x, Query.BYTES, null, null);
	}

	public void setCharacterStream(int parameterIndex, Reader reader, int length)
			throws SQLException {
		this.setObject(parameterIndex, reader, Query.CHARACTER_STREAM, null,
				new Integer(length));
	}

	public void setClob(int i, Clob x) throws SQLException {
		this.setObject(i, x, Types.CLOB);
	}

	public void setDate(int parameterIndex, Date x) throws SQLException {
		this.setDate(parameterIndex, x, Calendar.getInstance());
	}

	public void setDate(int parameterIndex, Date x, Calendar cal)
			throws SQLException {
		this.setObject(parameterIndex, x, Types.DATE, null, cal);
	}

	public void setDouble(int parameterIndex, double x) throws SQLException {
		this.setObject(parameterIndex, new Double(x), Types.DOUBLE);
	}

	public void setFloat(int parameterIndex, float x) throws SQLException {
		this.setObject(parameterIndex, new Float(x), Types.FLOAT);
	}

	public void setInt(int parameterIndex, int x) throws SQLException {
		this.setObject(parameterIndex, new Integer(x), Types.INTEGER);
	}

	public void setLong(int parameterIndex, long x) throws SQLException {
		this.setObject(parameterIndex, new Long(x), Query.LONG);
	}

	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		// We don't care about type.
		this.setObject(parameterIndex, null, sqlType);
	}

	public void setNull(int paramIndex, int sqlType, String typeName)
			throws SQLException {
		// We don't care about type or typeName.
		this.setObject(paramIndex, null, sqlType, typeName, null);
	}

	public void setObject(int parameterIndex, Object x) throws SQLException {
		this.setObject(parameterIndex, x, Query.INFERRED);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType)
			throws SQLException {
		this.setObject(parameterIndex, x, targetSqlType, null, null);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType,
			int scale) throws SQLException {
		this.setObject(parameterIndex, x, targetSqlType, null, new Integer(
				scale));
	}

	private void setObject(int parameterIndex, Object x, int targetSqlType,
			String sqlTypeName, Object operator) throws SQLException {
		this.query.setParameter(parameterIndex, new QueryParam(x,
				targetSqlType, sqlTypeName, operator));
	}

	public void setRef(int i, Ref x) throws SQLException {
		this.setObject(i, x, Types.REF);
	}

	public void setShort(int parameterIndex, short x) throws SQLException {
		this.setObject(parameterIndex, new Short(x), Query.SHORT);
	}

	public void setString(int parameterIndex, String x) throws SQLException {
		this.setObject(parameterIndex, x, Types.VARCHAR);
	}

	public void setTime(int parameterIndex, Time x) throws SQLException {
		this.setTime(parameterIndex, x, Calendar.getInstance());
	}

	public void setTime(int parameterIndex, Time x, Calendar cal)
			throws SQLException {
		this.setObject(parameterIndex, x, Types.TIME, null, cal);
	}

	public void setTimestamp(int parameterIndex, Timestamp x)
			throws SQLException {
		this.setTimestamp(parameterIndex, x, Calendar.getInstance());
	}

	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
			throws SQLException {
		this.setObject(parameterIndex, x, Types.TIMESTAMP, null, cal);
	}

	public void setURL(int parameterIndex, URL x) throws SQLException {
		this.setObject(parameterIndex, x, Query.URL);
	}

	public void setUnicodeStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		throw new SQLException(Resources.get("deprecated"));
	}

	public void addBatch(String sql) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void cancel() throws SQLException {
		// We don't support cancellation, so we just ignore it.
	}

	public void clearBatch() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void clearWarnings() throws SQLException {
		this.warnings = null;
	}

	public void close() throws SQLException {
		// We need to track and close all the statements we issued.
		for (final Iterator i = this.openResultSets.iterator(); i.hasNext(); )
			try {
				((ResultSet)i.next()).close();
			} catch (SQLException e) {
				// We don't care. Get rid of it anyway.
			}
		this.openResultSets.clear();
		this.closed = true;
		this.registryConn.statementClosed(this);
	}

	public boolean execute(String sql) throws SQLException {
		return this.executeQuery(sql) != null;
	}

	public boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public boolean execute(String sql, String[] columnNames)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public int[] executeBatch() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		if (this.query != null)
			throw new SQLException(Resources.get("prepStmtNoReqSQL"));
		return this.doExecuteQuery(new Query(this, sql));
	}

	public int executeUpdate(String sql) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public Connection getConnection() throws SQLException {
		return this.registryConn;
	}

	public int getFetchDirection() throws SQLException {
		// We only do forwards.
		return ResultSet.FETCH_FORWARD;
	}

	public int getFetchSize() throws SQLException {
		return this.fetchSize;
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public int getMaxFieldSize() throws SQLException {
		return this.maxColSize;
	}

	public int getMaxRows() throws SQLException {
		return this.maxRowCount;
	}

	public boolean getMoreResults() throws SQLException {
		return this.getMoreResults(Statement.CLOSE_CURRENT_RESULT);
	}

	public boolean getMoreResults(int current) throws SQLException {
		// We only have a single result set.
		final ResultSet results = this.getResultSet();
		if (current == Statement.CLOSE_CURRENT_RESULT
				|| current == Statement.CLOSE_ALL_RESULTS)
			results.close();
		// We always return false as there is never another resultset.
		return false;
	}

	public int getQueryTimeout() throws SQLException {
		// We don't do timeouts, so 0 indicates no timeout.
		return 0;
	}

	public ResultSet getResultSet() throws SQLException {
		// This is only for plain statements.
		if (this.openResultSets.isEmpty())
			throw new SQLException(Resources.get("plainStmtReqSQL"));
		// We return the most recent result set.
		return (ResultSet)this.openResultSets.iterator().next();
	}

	public int getResultSetConcurrency() throws SQLException {
		// We are always read-only.
		return ResultSet.CONCUR_READ_ONLY;
	}

	public int getResultSetHoldability() throws SQLException {
		// We don't have commits, so can hold forever.
		return ResultSet.HOLD_CURSORS_OVER_COMMIT;
	}

	public int getResultSetType() throws SQLException {
		// We are always forward-only.
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	public int getUpdateCount() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public SQLWarning getWarnings() throws SQLException {
		return this.warnings;
	}

	public void setCursorName(String name) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		// We don't do escape processing as we process all SQL directly.
	}

	public void setFetchDirection(int direction) throws SQLException {
		// Ignore this, we don't care as we can only read forwards.
	}

	public void setFetchSize(int rows) throws SQLException {
		if (rows < 1)
			throw new SQLException(Resources.get("fetchSizeTooSmall"));
		// Changing here only affects new result sets, not existing ones.
		this.fetchSize = rows;
	}

	public void setMaxFieldSize(int max) throws SQLException {
		this.maxColSize = max;
	}

	public void setMaxRows(int max) throws SQLException {
		this.maxRowCount = max;
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		// Ignore this as we don't support timeouts.
	}

	public void finalize() {
		try {
			if (!this.isClosed())
				this.close();
		} catch (SQLException e) {
			// Like, do we care?
		}
	}
}
