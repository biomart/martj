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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
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
public class SubQueryMetaData implements ResultSetMetaData {

	private SubQuery subQuery;

	private Query query;

	private MetaData[] metaData;

	/**
	 * Build metadata from the given sub query.
	 * 
	 * @param query
	 *            the query object the subquery belongs to.
	 * @param subQuery
	 *            the sub query containing our info.
	 * @throws SQLException
	 *             if metadata could not be built.
	 */
	SubQueryMetaData(final Query query, final SubQuery subQuery)
			throws SQLException {
		this.subQuery = subQuery;
		this.query = query;
		this.metaData = new MetaData[query.getColumnNames().size()];
		this.populate();
	}

	private void populate() throws SQLException {
		// Copy column details from subquery objects.
		final List subQueries = new ArrayList();
		subQueries.add(this.subQuery);
		for (int i = 0; i < subQueries.size(); i++) {
			final SubQuery sq = (SubQuery) subQueries.get(i);
			subQueries.addAll(sq.getAllSubQueries());
			final MetaData md = new MetaData();
			if (sq instanceof XMLSubQuery) {
				// Fake it all with strings.
				final Map colMap = sq.getResultMapping();
				for (final Iterator m = colMap.entrySet().iterator(); m
						.hasNext();) {
					final Map.Entry me = (Map.Entry) m.next();
					final int qIndex = ((Integer) me.getValue()).intValue();
					md.columnTypeName = "String";
					md.columnClassName = "java.lang.String";
					md.columnType = Types.VARCHAR;
					md.isSigned = false;
					md.isCurrency = false;
					md.scale = 0;
					md.precision = 0;
					md.catalogName = sq.getMartName();
					md.schemaName = sq.getDatasetName();
					md.tableName = md.schemaName;
					md.columnName = (String) this.query.getColumnNames().get(
							qIndex-1);
					md.columnLabel = md.columnName;
					md.columnDisplaySize = Integer.MAX_VALUE;
				}
			} else if (sq instanceof JDBCSubQuery) {
				// Copy the data from the JDBC connection.
				final ResultSetMetaData sqMetaData = ((JDBCSubQuery) sq)
						.getMetaData();
				final Map colMap = sq.getResultMapping();
				for (final Iterator m = colMap.entrySet().iterator(); m
						.hasNext();) {
					final Map.Entry me = (Map.Entry) m.next();
					final int sqIndex = ((Integer) me.getKey()).intValue();
					final int qIndex = ((Integer) me.getValue()).intValue();
					md.columnTypeName = sqMetaData.getColumnTypeName(sqIndex);
					md.columnClassName = sqMetaData.getColumnClassName(sqIndex);
					md.columnType = sqMetaData.getColumnType(sqIndex);
					md.isSigned = sqMetaData.isSigned(sqIndex);
					md.isCurrency = sqMetaData.isCurrency(sqIndex);
					md.scale = sqMetaData.getScale(sqIndex);
					md.precision = sqMetaData.getPrecision(sqIndex);
					md.catalogName = sq.getMartName();
					md.schemaName = sq.getDatasetName();
					md.tableName = md.schemaName;
					md.columnName = (String) this.query.getColumnNames().get(
							qIndex-1);
					md.columnLabel = md.columnName;
					md.columnDisplaySize = sqMetaData
							.getColumnDisplaySize(sqIndex);
				}
			} else
				throw new RuntimeException(); // Should never happen.
		}
	}

	private MetaData getMetaData(int column) throws SQLException {
		if (column < 1 || column >= this.metaData.length)
			throw new SQLException(Resources.get("colIndexOutRange", ""
					+ column));
		return this.metaData[column-1];
	}

	public String getCatalogName(int column) throws SQLException {
		return this.getMetaData(column).catalogName;
	}

	public String getColumnClassName(int column) throws SQLException {
		return this.getMetaData(column).columnClassName;
	}

	public int getColumnCount() throws SQLException {
		return this.metaData.length;
	}

	public int getColumnDisplaySize(int column) throws SQLException {
		return this.getMetaData(column).columnDisplaySize;
	}

	public String getColumnLabel(int column) throws SQLException {
		return this.getMetaData(column).columnLabel;
	}

	public String getColumnName(int column) throws SQLException {
		return this.getMetaData(column).columnName;
	}

	public int getColumnType(int column) throws SQLException {
		return this.getMetaData(column).columnType;
	}

	public String getColumnTypeName(int column) throws SQLException {
		return this.getMetaData(column).columnTypeName;
	}

	public int getPrecision(int column) throws SQLException {
		return this.getMetaData(column).precision;
	}

	public int getScale(int column) throws SQLException {
		return this.getMetaData(column).scale;
	}

	public String getSchemaName(int column) throws SQLException {
		return this.getMetaData(column).schemaName;
	}

	public String getTableName(int column) throws SQLException {
		return this.getMetaData(column).tableName;
	}

	public boolean isAutoIncrement(int column) throws SQLException {
		// No, never.
		return false;
	}

	public boolean isCaseSensitive(int column) throws SQLException {
		// No, never.
		return false;
	}

	public boolean isCurrency(int column) throws SQLException {
		return this.getMetaData(column).isCurrency;
	}

	public boolean isDefinitelyWritable(int column) throws SQLException {
		// No, never.
		return false;
	}

	public int isNullable(int column) throws SQLException {
		// Technically yes although we are write-only.
		return ResultSetMetaData.columnNullableUnknown;
	}

	public boolean isReadOnly(int column) throws SQLException {
		// Yes.
		return true;
	}

	public boolean isSearchable(int column) throws SQLException {
		// Of course!
		return true;
	}

	public boolean isSigned(int column) throws SQLException {
		return this.getMetaData(column).isSigned;
	}

	public boolean isWritable(int column) throws SQLException {
		// No, we're read-only.
		return false;
	}

	private class MetaData {
		/**
		 * @see ResultSetMetaData#isSigned(int)
		 */
		boolean isSigned;

		/**
		 * @see ResultSetMetaData#isCurrency(int)
		 */
		boolean isCurrency;

		/**
		 * @see ResultSetMetaData#getTableName(int)
		 */
		String tableName;

		/**
		 * @see ResultSetMetaData#getSchemaName(int)
		 */
		String schemaName;

		/**
		 * @see ResultSetMetaData#getScale(int)
		 */
		int scale;

		/**
		 * @see ResultSetMetaData#getPrecision(int)
		 */
		int precision;

		/**
		 * @see ResultSetMetaData#getColumnTypeName(int)
		 */
		String columnTypeName;

		/**
		 * @see ResultSetMetaData#getColumnType(int)
		 */
		int columnType;

		/**
		 * @see ResultSetMetaData#getColumnName(int)
		 */
		String columnName;

		/**
		 * @see ResultSetMetaData#getColumnLabel(int)
		 */
		String columnLabel;

		/**
		 * @see ResultSetMetaData#getColumnDisplaySize(int)
		 */
		int columnDisplaySize;

		/**
		 * @see ResultSetMetaData#getCatalogName(int)
		 */
		String catalogName;

		/**
		 * @see ResultSetMetaData#getColumnClassName(int)
		 */
		String columnClassName;
	}
}
