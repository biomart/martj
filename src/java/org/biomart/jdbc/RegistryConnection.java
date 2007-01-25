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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.jdbc.exceptions.RegistryException;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class RegistryConnection implements Connection {

	// Our data source, so that we can log to it and find out
	// other interesting stuff from it.
	private RegistryDataSource dataSource;

	// Are we closed?
	private boolean closed = false;

	// Do we have warnings?
	private SQLWarning warnings;

	// Custom type map?
	private Map customTypes = Collections.EMPTY_MAP;

	// Our set of open statements.
	private List openStatements = new ArrayList();
	
	// Currently selected catalog?
	private String catalog;
	
	// Metadata.
	private DatabaseMetaData metaData;

	/**
	 * Only for use by {@link RegistryDataSource#getRegistryConnection()}.
	 * 
	 * @param dataSource
	 *            the data source object we are connected to.
	 * @throws SQLException if the datasource initialisation failed.
	 */
	RegistryConnection(final RegistryDataSource dataSource) throws SQLException {
		this.dataSource = dataSource;
		try {
			this.dataSource.initialise();
		} catch (RegistryException e) {
			final SQLException se = new SQLException();
			se.initCause(e);
			throw se;
		}
	}

	public void clearWarnings() throws SQLException {
		this.warnings = null;
	}

	public void close() throws SQLException {
		// We need to track and close all the statements we issued.
		for (final Iterator i = this.openStatements.iterator(); i.hasNext(); )
			try {
				((Statement)i.next()).close();
			} catch (SQLException e) {
				// We don't care. Get rid of it anyway.
			}
		this.openStatements.clear();
		this.closed = true;
		this.dataSource.connectionClosed(this);
	}

	/**
	 * A statement has been closed, so we can forget about it.
	 * 
	 * @param stmt 
	 * 			the statement that has been closed.
	 */
	void statementClosed(Statement stmt) {
		synchronized (this.openStatements) {
			this.openStatements.remove(stmt);
		}
	}

	private void ensureOpen() throws SQLException {
		if (this.isClosed())
			throw new SQLException(Resources.get("closedConnection"));
	}

	public void commit() throws SQLException {
		// Has no effect as we do not support transactions.
	}

	public Statement createStatement() throws SQLException {
		return this.createStatement(ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
	}

	public Statement createStatement(final int resultSetType,
			final int resultSetConcurrency) throws SQLException {
		return this.createStatement(ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	public Statement createStatement(final int resultSetType,
			final int resultSetConcurrency, final int resultSetHoldability)
			throws SQLException {
		this.ensureOpen();
		// We don't care what holdability or concurrency get used.
		if (resultSetType != ResultSet.TYPE_FORWARD_ONLY)
			throw new SQLException(Resources.get("forwardOnly"));
		QueryStatement stmt = new QueryStatement(this);
		synchronized (this.openStatements) {
			this.openStatements.add(stmt);
		}
		return stmt;
	}

	public boolean getAutoCommit() throws SQLException {
		// Has no effect as we do not support transactions.
		return false;
	}

	public String getCatalog() throws SQLException {
		return this.catalog;
	}

	public int getHoldability() throws SQLException {
		// We don't have transactions, therefore cursors can last forever.
		return ResultSet.HOLD_CURSORS_OVER_COMMIT;
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		if (this.metaData==null)
			this.metaData = new RegistryMetaData(this, this.dataSource);
		return this.metaData;
	}

	public int getTransactionIsolation() throws SQLException {
		// We are never in a transaction.
		return Connection.TRANSACTION_NONE;
	}

	public Map getTypeMap() throws SQLException {
		return this.customTypes;
	}

	public SQLWarning getWarnings() throws SQLException {
		return this.warnings;
	}

	public boolean isClosed() throws SQLException {
		return this.closed;
	}

	public boolean isReadOnly() throws SQLException {
		return true;
	}

	public String nativeSQL(final String sql) throws SQLException {
		// We don't do translation.
		return sql;
	}

	public CallableStatement prepareCall(final String sql) throws SQLException {
		return this.prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
	}

	public CallableStatement prepareCall(final String sql,
			final int resultSetType, final int resultSetConcurrency)
			throws SQLException {
		return this.prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
	}

	public CallableStatement prepareCall(final String sql,
			final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		throw new SQLException(Resources.get("callableNotSupported"));
	}

	public PreparedStatement prepareStatement(final String sql)
			throws SQLException {
		return this.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
	}

	public PreparedStatement prepareStatement(final String sql,
			final int autoGeneratedKeys) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public PreparedStatement prepareStatement(final String sql,
			final int[] columnIndexes) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public PreparedStatement prepareStatement(final String sql,
			final String[] columnNames) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public PreparedStatement prepareStatement(final String sql,
			final int resultSetType, final int resultSetConcurrency)
			throws SQLException {
		return this.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
	}

	public PreparedStatement prepareStatement(final String sql,
			final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		this.ensureOpen();
		// We don't care what holdability or concurrency get used.
		if (resultSetType != ResultSet.TYPE_FORWARD_ONLY)
			throw new SQLException(Resources.get("forwardOnly"));
		QueryStatement stmt = new QueryStatement(this, sql);
		synchronized (this.openStatements) {
			this.openStatements.add(stmt);
		}
		return stmt;
	}

	public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
		// Has no effect as we do not support transactions.
	}

	public void rollback() throws SQLException {
		// Has no effect as we do not support transactions.
	}

	public void rollback(final Savepoint savepoint) throws SQLException {
		// Has no effect as we do not support transactions.
	}

	public void setAutoCommit(final boolean autoCommit) throws SQLException {
		// Has no effect as we do not support transactions.
	}

	public void setCatalog(final String catalog) throws SQLException {
		this.catalog = catalog;
	}

	public void setHoldability(final int holdability) throws SQLException {
		// We can ignore this, as we don't have transactions.
	}

	public void setReadOnly(final boolean readOnly) throws SQLException {
		// Has no effect as we are always read-only.
	}

	public Savepoint setSavepoint() throws SQLException {
		// Has no effect as we do not support transactions.
		return null;
	}

	public Savepoint setSavepoint(final String name) throws SQLException {
		// Has no effect as we do not support transactions.
		return null;
	}

	public void setTransactionIsolation(final int level) throws SQLException {
		// Has no effect as we do not support transactions.
	}

	public void setTypeMap(final Map map) throws SQLException {
		this.customTypes = map;
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
