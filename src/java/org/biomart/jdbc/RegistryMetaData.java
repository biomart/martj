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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.biomart.jdbc.exceptions.RegistryException;
import org.biomart.jdbc.model.Attribute;
import org.biomart.jdbc.model.Dataset;
import org.biomart.jdbc.model.Mart;
import org.biomart.jdbc.model.Registry;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class RegistryMetaData implements DatabaseMetaData {

	private RegistryConnection conn;

	private RegistryDataSource dataSource;

	/**
	 * Work out metadata for a given connection.
	 * 
	 * @param conn
	 *            the connection to get data for.
	 * @param dataSource
	 *            the dataSource to get data for.
	 * @throws SQLException
	 *             if the data cannot be read.
	 */
	RegistryMetaData(final RegistryConnection conn,
			final RegistryDataSource dataSource) throws SQLException {
		this.conn = conn;
		this.dataSource = dataSource;
	}

	private boolean match(final String name, final String pattern) {
		// Null patterns match everything.
		if (pattern == null)
			return true;
		// Otherwise, modify the pattern and use it to test.
		String compiledPattern = pattern;
		compiledPattern = compiledPattern.replaceAll("[^\\\\]_", ".");
		compiledPattern = compiledPattern.replaceAll("[^\\\\]%", ".*");
		compiledPattern = compiledPattern.replaceAll("\\\\", "");
		compiledPattern = "^" + compiledPattern + "$";
		return Pattern.compile(compiledPattern).matcher(name).matches();
	}

	public boolean allProceduresAreCallable() throws SQLException {
		// We don't do procedures.
		return false;
	}

	public boolean allTablesAreSelectable() throws SQLException {
		// Of course they are!
		return true;
	}

	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		// We don't do transactions.
		return false;
	}

	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		// We don't do transactions.
		return false;
	}

	public boolean deletesAreDetected(int type) throws SQLException {
		// We don't do deletes.
		return false;
	}

	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		// Um, probably not?
		return false;
	}

	public ResultSet getAttributes(String catalog, String schemaPattern,
			String typeNamePattern, String attributeNamePattern)
			throws SQLException {
		throw new SQLException(Resources.get("typeNotImplemented"));
	}

	public ResultSet getBestRowIdentifier(String catalog, String schema,
			String table, int scope, boolean nullable) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("SCOPE");
		colNames.add("COLUMN_NAME");
		colNames.add("DATA_TYPE");
		colNames.add("TYPE_NAME");
		colNames.add("COLUMN_SIZE");
		colNames.add("BUFFER_LENGTH");
		colNames.add("DECIMAL_DIGITS");
		colNames.add("PSEUDO_COLUMN");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public String getCatalogSeparator() throws SQLException {
		// We always use full-stops between names.
		return ".";
	}

	public String getCatalogTerm() throws SQLException {
		// We prefer the name "Mart".
		return "Mart";
	}

	public ResultSet getCatalogs() throws SQLException {
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		final List rows = new ArrayList();
		try {
			final Registry registry = this.dataSource.getRegistry();
			for (final Iterator i = registry.getMartNames().iterator(); i
					.hasNext();) {
				final Mart mart = registry.getMart((String) i.next());
				final Map row = new HashMap();
				row.put("TABLE_CAT", mart.getName());
				rows.add(row);
			}
		} catch (RegistryException e) {
			final SQLException se = new SQLException();
			se.initCause(e);
			throw se;
		}
		return new RegistryResultSet(colNames, rows);
	}

	public ResultSet getColumnPrivileges(String catalog, String schema,
			String table, String columnNamePattern) throws SQLException {
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		colNames.add("TABLE_SCHEM");
		colNames.add("TABLE_NAME");
		colNames.add("COLUMN_NAME");
		colNames.add("GRANTOR");
		colNames.add("GRANTEE");
		colNames.add("PRIVILEGE");
		colNames.add("IS_GRANTABLE");
		final ResultSet columns = this.getColumns(catalog, schema, table,
				columnNamePattern);
		final List rows = new ArrayList();
		while (columns.next()) {
			final Map row = new HashMap();
			row.put("TABLE_CAT", columns.getString("TABLE_CAT"));
			row.put("TABLE_SCHEM", columns.getString("TABLE_SCHEM"));
			row.put("TABLE_NAME", columns.getString("TABLE_NAME"));
			row.put("COLUMN_NAME", columns.getString("COLUMN_NAME"));
			row.put("GRANTOR", null);
			row.put("GRANTEE", this.getUserName());
			row.put("PRIVILEGE", "SELECT");
			row.put("IS_GRANTABLE", "NO");
			rows.add(row);
		}
		return new RegistryResultSet(colNames, rows);
	}

	public ResultSet getColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		colNames.add("TABLE_SCHEM");
		colNames.add("TABLE_NAME");
		colNames.add("COLUMN_NAME");
		colNames.add("DATA_TYPE");
		colNames.add("TYPE_NAME");
		colNames.add("COLUMN_SIZE");
		colNames.add("BUFFER_LENGTH");
		colNames.add("DECIMAL_DIGITS");
		colNames.add("NUM_PREC_RADIX");
		colNames.add("NULLABLE");
		colNames.add("REMARKS");
		colNames.add("COLUMN_DEF");
		colNames.add("SQL_DATA_TYPE");
		colNames.add("SQL_DATETIME_SUB");
		colNames.add("CHAR_OCTET_LENGTH");
		colNames.add("ORDINAL_POSITION");
		colNames.add("IS_NULLABLE");
		colNames.add("SCOPE_CATALOG");
		colNames.add("SCOPE_SCHEMA");
		colNames.add("SCOPE_TABLE");
		colNames.add("SOURCE_DATA_TYPE");
		final List rows = new ArrayList();
		try {
			final Registry registry = this.dataSource.getRegistry();
			for (final Iterator i = registry.getMartNames().iterator(); i
					.hasNext();) {
				final Mart mart = registry.getMart((String) i.next());
				if (!this.match(mart.getName(), catalog))
					continue;
				for (final Iterator j = mart.getDatasetNames().iterator(); j
						.hasNext();) {
					final Dataset dataset = mart.getDataset((String) j.next());
					if (!this.match(dataset.getName(), schemaPattern))
						continue;
					if (!this.match(dataset.getName(), tableNamePattern))
						continue;
					int ordinal = 1; // Column pos within table.
					for (final Iterator k = dataset.getAttributeNames()
							.iterator(); k.hasNext();) {
						final Attribute attr = dataset.getAttribute((String) k
								.next());
						if (!this.match(attr.getName(), columnNamePattern))
							continue;
						final Map row = new HashMap();
						row.put("TABLE_CAT", mart.getName());
						row.put("TABLE_SCHEM", dataset.getName());
						row.put("TABLE_NAME", dataset.getName());
						row.put("COLUMN_NAME", attr.getName());
						row.put("DATA_TYPE", "String");
						row.put("TYPE_NAME", "" + Types.VARCHAR);
						row.put("COLUMN_SIZE", "0");
						row.put("BUFFER_LENGTH", null);
						row.put("DECIMAL_DIGITS", "0");
						row.put("NUM_PREC_RADIX", "10");
						row.put("NULLABLE", ""
								+ DatabaseMetaData.columnNullable);
						row.put("REMARKS", "");
						row.put("COLUMN_DEF", null);
						row.put("SQL_DATA_TYPE", "0");
						row.put("SQL_DATETIME_SUB", "0");
						row.put("CHAR_OCTET_LENGTH", "0");
						row.put("ORDINAL_POSITION", "" + ordinal++);
						row.put("IS_NULLABLE", "YES");
						row.put("SCOPE_CATALOG", null);
						row.put("SCOPE_SCHEMA", null);
						row.put("SCOPE_TABLE", null);
						row.put("SOURCE_DATA_TYPE", null);
						rows.add(row);
					}
				}
			}
		} catch (RegistryException e) {
			final SQLException se = new SQLException();
			se.initCause(e);
			throw se;
		}
		return new RegistryResultSet(colNames, rows);
	}

	public Connection getConnection() throws SQLException {
		return this.conn;
	}

	public ResultSet getCrossReference(String primaryCatalog,
			String primarySchema, String primaryTable, String foreignCatalog,
			String foreignSchema, String foreignTable) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("PKTABLE_CAT");
		colNames.add("PKTABLE_SCHEM");
		colNames.add("PKTABLE_NAME");
		colNames.add("PKCOLUMN_NAME");
		colNames.add("FKTABLE_CAT");
		colNames.add("FKTABLE_SCHEM");
		colNames.add("FKTABLE_NAME");
		colNames.add("FKCOLUMN_NAME");
		colNames.add("KEY_SEQ");
		colNames.add("UPDATE_RULE");
		colNames.add("DELETE_RULE");
		colNames.add("FK_NAME");
		colNames.add("PK_NAME");
		colNames.add("DEFERRABILITY");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public int getDatabaseMajorVersion() throws SQLException {
		return RegistryDataSource.MAJOR_VERSION;
	}

	public int getDatabaseMinorVersion() throws SQLException {
		return RegistryDataSource.MINOR_VERSION;
	}

	public String getDatabaseProductName() throws SQLException {
		return "BioMart";
	}

	public String getDatabaseProductVersion() throws SQLException {
		return RegistryDataSource.MAJOR_VERSION + "."
				+ RegistryDataSource.MINOR_VERSION;
	}

	public int getDefaultTransactionIsolation() throws SQLException {
		// We don't do transactions.
		return Connection.TRANSACTION_NONE;
	}

	public int getDriverMajorVersion() {
		return RegistryDataSource.MAJOR_VERSION;
	}

	public int getDriverMinorVersion() {
		return RegistryDataSource.MINOR_VERSION;
	}

	public String getDriverName() throws SQLException {
		return "BioMart";
	}

	public String getDriverVersion() throws SQLException {
		return RegistryDataSource.MAJOR_VERSION + "."
				+ RegistryDataSource.MINOR_VERSION;
	}

	public ResultSet getExportedKeys(String catalog, String schema, String table)
			throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("PKTABLE_CAT");
		colNames.add("PKTABLE_SCHEM");
		colNames.add("PKTABLE_NAME");
		colNames.add("PKCOLUMN_NAME");
		colNames.add("FKTABLE_CAT");
		colNames.add("FKTABLE_SCHEM");
		colNames.add("FKTABLE_NAME");
		colNames.add("FKCOLUMN_NAME");
		colNames.add("KEY_SEQ");
		colNames.add("UPDATE_RULE");
		colNames.add("DELETE_RULE");
		colNames.add("FK_NAME");
		colNames.add("PK_NAME");
		colNames.add("DEFERRABILITY");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public String getExtraNameCharacters() throws SQLException {
		// We don't have any others.
		return "";
	}

	public String getIdentifierQuoteString() throws SQLException {
		// We use double quotes to alias columns.
		return "\"";
	}

	public ResultSet getImportedKeys(String catalog, String schema, String table)
			throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("PKTABLE_CAT");
		colNames.add("PKTABLE_SCHEM");
		colNames.add("PKTABLE_NAME");
		colNames.add("PKCOLUMN_NAME");
		colNames.add("FKTABLE_CAT");
		colNames.add("FKTABLE_SCHEM");
		colNames.add("FKTABLE_NAME");
		colNames.add("FKCOLUMN_NAME");
		colNames.add("KEY_SEQ");
		colNames.add("UPDATE_RULE");
		colNames.add("DELETE_RULE");
		colNames.add("FK_NAME");
		colNames.add("PK_NAME");
		colNames.add("DEFERRABILITY");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		colNames.add("TABLE_SCHEM");
		colNames.add("TABLE_NAME");
		colNames.add("NON_UNIQUE");
		colNames.add("INDEX_QUALIFIER");
		colNames.add("INDEX_NAME");
		colNames.add("TYPE");
		colNames.add("ORDINAL_POSITION");
		colNames.add("COLUMN_NAME");
		colNames.add("ASC_OR_DESC");
		colNames.add("CARDINALITY");
		colNames.add("PAGES");
		colNames.add("FILTER_CONDITION");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public int getJDBCMajorVersion() throws SQLException {
		return RegistryDataSource.MAJOR_VERSION;
	}

	public int getJDBCMinorVersion() throws SQLException {
		return RegistryDataSource.MINOR_VERSION;
	}

	public int getMaxBinaryLiteralLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxCatalogNameLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxCharLiteralLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxColumnNameLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxColumnsInGroupBy() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxColumnsInIndex() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxColumnsInOrderBy() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxColumnsInSelect() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxColumnsInTable() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxConnections() throws SQLException {
		// Unlimited.
		return 0;
	}

	public int getMaxCursorNameLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxIndexLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxProcedureNameLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxRowSize() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxSchemaNameLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxStatementLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxStatements() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxTableNameLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxTablesInSelect() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public int getMaxUserNameLength() throws SQLException {
		// Limit unknown.
		return 0;
	}

	public String getNumericFunctions() throws SQLException {
		// We don't support any.
		return "";
	}

	public ResultSet getPrimaryKeys(String catalog, String schema, String table)
			throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		colNames.add("TABLE_SCHEM");
		colNames.add("TABLE_NAME");
		colNames.add("COLUMN_NAME");
		colNames.add("KEY_SEQ");
		colNames.add("PK_NAME");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public ResultSet getProcedureColumns(String catalog, String schemaPattern,
			String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("PROCEDURE_CAT");
		colNames.add("PROCEDURE_SCHEM");
		colNames.add("PROCEDURE_NAME");
		colNames.add("COLUMN_NAME");
		colNames.add("COLUMN_TYPE");
		colNames.add("DATA_TYPE");
		colNames.add("TYPE_NAME");
		colNames.add("PRECISION");
		colNames.add("LENGTH");
		colNames.add("SCALE");
		colNames.add("RADIX");
		colNames.add("NULLABLE");
		colNames.add("REMARKS");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public String getProcedureTerm() throws SQLException {
		return "Procedure";
	}

	public ResultSet getProcedures(String catalog, String schemaPattern,
			String procedureNamePattern) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("PROCEDURE_CAT");
		colNames.add("PROCEDURE_SCHEM");
		colNames.add("PROCEDURE_NAME");
		colNames.add("reserved1");
		colNames.add("reserved2");
		colNames.add("reserved3");
		colNames.add("REMARKS");
		colNames.add("PROCEDURE_TYPE");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public int getResultSetHoldability() throws SQLException {
		// We don't do transactions.
		return ResultSet.HOLD_CURSORS_OVER_COMMIT;
	}

	public String getSQLKeywords() throws SQLException {
		// We don't (yet) use any non-standard keywords.
		return "";
	}

	public int getSQLStateType() throws SQLException {
		// We have no idea. Let's go with a random default.
		return DatabaseMetaData.sqlStateSQL99;
	}

	public String getSchemaTerm() throws SQLException {
		// Schemas are datasets.
		return "Dataset";
	}

	public ResultSet getSchemas() throws SQLException {
		final List colNames = new ArrayList();
		colNames.add("TABLE_SCHEM");
		colNames.add("TABLE_CATALOG");
		final List rows = new ArrayList();
		try {
			final Registry registry = this.dataSource.getRegistry();
			for (final Iterator i = registry.getMartNames().iterator(); i
					.hasNext();) {
				final Mart mart = registry.getMart((String) i.next());
				for (final Iterator j = mart.getDatasetNames().iterator(); j
						.hasNext();) {
					final Dataset dataset = mart.getDataset((String) j.next());
					final Map row = new HashMap();
					row.put("TABLE_SCHEM", dataset.getName());
					row.put("TABLE_CATALOG", mart.getName());
					rows.add(row);
				}
			}
		} catch (RegistryException e) {
			final SQLException se = new SQLException();
			se.initCause(e);
			throw se;
		}
		return new RegistryResultSet(colNames, rows);
	}

	public String getSearchStringEscape() throws SQLException {
		// We use backslash to escape things.
		return "\\";
	}

	public String getStringFunctions() throws SQLException {
		// We don't support any.
		return "";
	}

	public ResultSet getSuperTables(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		colNames.add("TABLE_SCHEMA");
		colNames.add("TABLE_NAME");
		colNames.add("SUPERTABLE_NAME");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public ResultSet getSuperTypes(String catalog, String schemaPattern,
			String typeNamePattern) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("TYPE_CAT");
		colNames.add("TYPE_SCHEMA");
		colNames.add("TYPE_NAME");
		colNames.add("SUPERTYPE_CAT");
		colNames.add("SUPERTYPE_SCHEMA");
		colNames.add("SUPERTYPE_NAME");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public String getSystemFunctions() throws SQLException {
		// Currently none, but maybe in future?
		return "";
	}

	public ResultSet getTablePrivileges(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		colNames.add("TABLE_SCHEMA");
		colNames.add("TABLE_NAME");
		colNames.add("GRANTOR");
		colNames.add("GRANTEE");
		colNames.add("PRIVILEGE");
		colNames.add("IS_GRANTABLE");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public ResultSet getTableTypes() throws SQLException {
		final List colNames = new ArrayList();
		colNames.add("TABLE_TYPE");
		final List rows = new ArrayList();
		final Map resultRow = new HashMap();
		resultRow.put("TABLE_TYPE", "TABLE");
		rows.add(resultRow);
		return new RegistryResultSet(colNames, rows);
	}

	public ResultSet getTables(String catalog, String schemaPattern,
			String tableNamePattern, String[] types) throws SQLException {
		// Types are ignored for now.
		final List colNames = new ArrayList();
		colNames.add("TABLE_CAT");
		colNames.add("TABLE_SCHEM");
		colNames.add("TABLE_NAME");
		colNames.add("TABLE_TYPE");
		colNames.add("REMARKS");
		colNames.add("TYPE_CAT");
		colNames.add("TYPE_SCHEM");
		colNames.add("TYPE_NAME");
		colNames.add("SELF_REFERENCING_COL_NAME");
		colNames.add("REF_GENERATION");
		final ResultSet schemas = this.getSchemas();
		final List rows = new ArrayList();
		while (schemas.next()) {
			final Map row = new HashMap();
			final String catalogName = schemas.getString("TABLE_CATALOG");
			final String schemaName = schemas.getString("TABLE_SCHEM");
			if (!(this.match(catalogName, catalog)
					&& this.match(schemaName, schemaPattern) && this.match(
					schemaName, tableNamePattern)))
				continue;
			row.put("TABLE_CAT", catalogName);
			row.put("TABLE_SCHEM", schemaName);
			row.put("TABLE_NAME", schemaName);
			row.put("TABLE_TYPE", "TABLE");
			row.put("REMARKS", "");
			row.put("TYPE_CAT", null);
			row.put("TYPE_SCHEM", null);
			row.put("TYPE_NAME", null);
			row.put("SELF_REFERENCING_COL_NAME", null);
			row.put("REF_GENERATION", null);
			rows.add(row);
		}
		return new RegistryResultSet(colNames, rows);
	}

	public String getTimeDateFunctions() throws SQLException {
		// We don't support any.
		return "";
	}

	public ResultSet getTypeInfo() throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("TYPE_NAME");
		colNames.add("DATA_TYPE");
		colNames.add("PRECISION");
		colNames.add("LITERAL_PREFIX");
		colNames.add("LITERAL_SUFFIX");
		colNames.add("CREATE_PARAMS");
		colNames.add("NULLABLE");
		colNames.add("CASE_SENSITIVE");
		colNames.add("SEARCHABLE");
		colNames.add("UNSIGNED_ATTRIBUTE");
		colNames.add("FIXED_PREC_SCALE");
		colNames.add("AUTO_INCREMENT");
		colNames.add("LOCAL_TYPE_NAME");
		colNames.add("MINIMUM_SCALE");
		colNames.add("MAXIMUM_SCALE");
		colNames.add("SQL_DATA_TYPE");
		colNames.add("SQL_DATETIME_SUB");
		colNames.add("NUM_PREC_RADIX");
		final List rows = new ArrayList();
		final Map stringRow = new HashMap();
		stringRow.put("TYPE_NAME", "String");
		stringRow.put("DATA_TYPE", "" + Types.VARCHAR);
		stringRow.put("PRECISION", "0");
		stringRow.put("LITERAL_PREFIX", null);
		stringRow.put("LITERAL_SUFFIX", null);
		stringRow.put("CREATE_PARAMS", null);
		stringRow.put("NULLABLE", "" + DatabaseMetaData.typeNullable);
		stringRow.put("CASE_SENSITIVE", "true");
		stringRow.put("SEARCHABLE", "" + DatabaseMetaData.typeSearchable);
		stringRow.put("UNSIGNED_ATTRIBUTE", "false");
		stringRow.put("FIXED_PREC_SCALE", "false");
		stringRow.put("AUTO_INCREMENT", "false");
		stringRow.put("LOCAL_TYPE_NAME", null);
		stringRow.put("MINIMUM_SCALE", "0");
		stringRow.put("MAXIMUM_SCALE", "0");
		stringRow.put("SQL_DATA_TYPE", "0");
		stringRow.put("SQL_DATETIME_SUB", "0");
		stringRow.put("NUM_PREC_RADIX", "10");
		rows.add(stringRow);
		return new RegistryResultSet(colNames, rows);
	}

	public ResultSet getUDTs(String catalog, String schemaPattern,
			String typeNamePattern, int[] types) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("TYPE_CAT");
		colNames.add("TYPE_SCHEMA");
		colNames.add("TYPE_NAME");
		colNames.add("CLASS_NAME");
		colNames.add("DATA_TYPE");
		colNames.add("REMARKS");
		colNames.add("BASE_TYPE");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public String getURL() throws SQLException {
		return "" + this.dataSource.getRegistryURL();
	}

	public String getUserName() throws SQLException {
		// No users on this database.
		return null;
	}

	public ResultSet getVersionColumns(String catalog, String schema,
			String table) throws SQLException {
		// We don't do this as we only have one table.
		final List colNames = new ArrayList();
		colNames.add("SCOPE");
		colNames.add("COLUMN_NAME");
		colNames.add("DATA_TYPE");
		colNames.add("TYPE_NAME");
		colNames.add("COLUMN_SIZE");
		colNames.add("BUFFER_LENGTH");
		colNames.add("DECIMAL_DIGITS");
		colNames.add("PSEUDO_COLUMN");
		return new RegistryResultSet(colNames, Collections.EMPTY_LIST);
	}

	public boolean insertsAreDetected(int type) throws SQLException {
		// We don't do inserts.
		return false;
	}

	public boolean isCatalogAtStart() throws SQLException {
		// Yes, catalogs always prefix tables.
		return true;
	}

	public boolean isReadOnly() throws SQLException {
		// Yup.
		return true;
	}

	public boolean locatorsUpdateCopy() throws SQLException {
		// We're read-only.
		return false;
	}

	public boolean nullPlusNonNullIsNull() throws SQLException {
		// Yes.
		return true;
	}

	public boolean nullsAreSortedAtEnd() throws SQLException {
		// Yes.
		return true;
	}

	public boolean nullsAreSortedAtStart() throws SQLException {
		// No.
		return false;
	}

	public boolean nullsAreSortedHigh() throws SQLException {
		// Yes.
		return true;
	}

	public boolean nullsAreSortedLow() throws SQLException {
		// No.
		return false;
	}

	public boolean othersDeletesAreVisible(int type) throws SQLException {
		// We're read-only.
		return false;
	}

	public boolean othersInsertsAreVisible(int type) throws SQLException {
		// We're read-only.
		return false;
	}

	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		// We're read-only.
		return false;
	}

	public boolean ownDeletesAreVisible(int type) throws SQLException {
		// We're read-only.
		return false;
	}

	public boolean ownInsertsAreVisible(int type) throws SQLException {
		// We're read-only.
		return false;
	}

	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		// We're read-only.
		return false;
	}

	public boolean storesLowerCaseIdentifiers() throws SQLException {
		// Yes.
		return true;
	}

	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		// Yes.
		return true;
	}

	public boolean storesMixedCaseIdentifiers() throws SQLException {
		// No.
		return false;
	}

	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		// No.
		return false;
	}

	public boolean storesUpperCaseIdentifiers() throws SQLException {
		// No.
		return false;
	}

	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		// Unfortunately no!
		return false;
	}

	public boolean supportsANSI92FullSQL() throws SQLException {
		// Definitely not.
		return false;
	}

	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		// Not even that.
		return false;
	}

	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsBatchUpdates() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsColumnAliasing() throws SQLException {
		// Yes.
		return true;
	}

	public boolean supportsConvert() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsConvert(int fromType, int toType)
			throws SQLException {
		// No.
		return false;
	}

	public boolean supportsCoreSQLGrammar() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsCorrelatedSubqueries() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsDataDefinitionAndDataManipulationTransactions()
			throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsDataManipulationTransactionsOnly()
			throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsExpressionsInOrderBy() throws SQLException {
		// Not yet, but maybe later.
		return false;
	}

	public boolean supportsExtendedSQLGrammar() throws SQLException {
		// Um, no.
		return false;
	}

	public boolean supportsFullOuterJoins() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsGetGeneratedKeys() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsGroupBy() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsGroupByBeyondSelect() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsGroupByUnrelated() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		// WTF? Have no idea, so probably not.
		return false;
	}

	public boolean supportsLikeEscapeClause() throws SQLException {
		// Yes.
		return true;
	}

	public boolean supportsLimitedOuterJoins() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsMinimumSQLGrammar() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsMultipleOpenResults() throws SQLException {
		// Yes.
		return true;
	}

	public boolean supportsMultipleResultSets() throws SQLException {
		// Yes.
		return true;
	}

	public boolean supportsMultipleTransactions() throws SQLException {
		// No, we don't do transactions.
		return false;
	}

	public boolean supportsNamedParameters() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsNonNullableColumns() throws SQLException {
		// Yes.
		return true;
	}

	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		// We don't do transactions, so yes.
		return true;
	}

	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		// We don't do transactions, so yes.
		return true;
	}

	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		// We don't do transactions, so yes.
		return true;
	}

	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		// We don't do transactions, so yes.
		return true;
	}

	public boolean supportsOrderByUnrelated() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsOuterJoins() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsPositionedDelete() throws SQLException {
		// No, we're read only.
		return false;
	}

	public boolean supportsPositionedUpdate() throws SQLException {
		// No, we're read only.
		return false;
	}

	public boolean supportsResultSetConcurrency(int type, int concurrency)
			throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY
				&& concurrency == ResultSet.CONCUR_READ_ONLY;
	}

	public boolean supportsResultSetHoldability(int holdability)
			throws SQLException {
		// We don't give a toss.
		return true;
	}

	public boolean supportsResultSetType(int type) throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	public boolean supportsSavepoints() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsSchemasInDataManipulation() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		// Yes, probably.
		return true;
	}

	public boolean supportsSelectForUpdate() throws SQLException {
		// No, we're read-only.
		return false;
	}

	public boolean supportsStatementPooling() throws SQLException {
		// Dunno what this is, so we'll say no.
		return false;
	}

	public boolean supportsStoredProcedures() throws SQLException {
		// No, not yet.
		return false;
	}

	public boolean supportsSubqueriesInComparisons() throws SQLException {
		// No, not yet.
		return false;
	}

	public boolean supportsSubqueriesInExists() throws SQLException {
		// No, not yet.
		return false;
	}

	public boolean supportsSubqueriesInIns() throws SQLException {
		// No, not yet.
		return false;
	}

	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		// No, not yet.
		return false;
	}

	public boolean supportsTableCorrelationNames() throws SQLException {
		// Yes.
		return true;
	}

	public boolean supportsTransactionIsolationLevel(int level)
			throws SQLException {
		// We don't do transactions, so all isolations work.
		return true;
	}

	public boolean supportsTransactions() throws SQLException {
		// No.
		return false;
	}

	public boolean supportsUnion() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean supportsUnionAll() throws SQLException {
		// Not yet.
		return false;
	}

	public boolean updatesAreDetected(int type) throws SQLException {
		// Never, we are read-only.
		return false;
	}

	public boolean usesLocalFilePerTable() throws SQLException {
		// No way!
		return false;
	}

	public boolean usesLocalFiles() throws SQLException {
		// No.
		return false;
	}

	private class RegistryResultSet implements ResultSet {

		private List rows;

		private List colNames;

		private int currentRow;

		private boolean lastColWasNull = false;

		private RegistryResultsMetaData metaData;

		/**
		 * Construct a result set based around the given rows.
		 * 
		 * @param rows
		 *            the rows to return in results. Each row consists of a map
		 *            of key->value pairs, where the keys are column names from
		 *            the colNames list. Values must all be Strings.
		 * @param colNames
		 *            the list of column names in order that should be returned
		 *            by the result set.
		 */
		public RegistryResultSet(final List colNames, final List rows) {
			this.colNames = colNames;
			this.rows = rows;
			this.currentRow = -1;
		}

		public boolean absolute(int row) throws SQLException {
			if (row < 1 || row > this.rows.size())
				throw new SQLException(Resources.get("colIndexOutRange", ""
						+ row));
			this.currentRow = row - 1;
			return false;
		}

		public void afterLast() throws SQLException {
			this.currentRow = this.rows.size();
		}

		public void beforeFirst() throws SQLException {
			this.currentRow = -1;
		}

		public void cancelRowUpdates() throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void clearWarnings() throws SQLException {
			// There aren't any warnings.
		}

		public void close() throws SQLException {
			// Has no effect.
		}

		public void deleteRow() throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public int findColumn(String columnName) throws SQLException {
			int index = this.colNames.indexOf(columnName) + 1;
			if (index < 1)
				throw new SQLException(Resources.get("unknownColName",
						columnName));
			return index;
		}

		public boolean first() throws SQLException {
			this.currentRow = 0;
			return true;
		}

		public Array getArray(int i) throws SQLException {
			return this.objectToArray(this.getObject(i));
		}

		public Array getArray(String colName) throws SQLException {
			return this.objectToArray(this.getObject(colName));
		}

		private Array objectToArray(Object obj) throws SQLException {
			try {
				return (Array) obj;
			} catch (ClassCastException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
		}

		public InputStream getAsciiStream(int i) throws SQLException {
			return this.objectToAsciiStream(this.getObject(i));
		}

		public InputStream getAsciiStream(String colName) throws SQLException {
			return this.objectToAsciiStream(this.getObject(colName));
		}

		private InputStream objectToAsciiStream(Object obj) throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof InputStream)
				return (InputStream) obj;
			else if (obj instanceof Clob)
				return ((Clob) obj).getAsciiStream();
			else if (obj instanceof String)
				return new ByteArrayInputStream(((String) obj).getBytes());
			else
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
		}

		public BigDecimal getBigDecimal(int i) throws SQLException {
			return this.objectToBigDecimal(this.getObject(i), 0);
		}

		public BigDecimal getBigDecimal(String colName) throws SQLException {
			return this.objectToBigDecimal(this.getObject(colName), 0);
		}

		public BigDecimal getBigDecimal(int i, int scale) throws SQLException {
			return this.objectToBigDecimal(this.getObject(i), scale);
		}

		public BigDecimal getBigDecimal(String colName, int scale)
				throws SQLException {
			return this.objectToBigDecimal(this.getObject(colName), scale);
		}

		private BigDecimal objectToBigDecimal(Object obj, int scale)
				throws SQLException {
			if (obj == null)
				return new BigDecimal(0);
			else if (obj instanceof BigDecimal) {
				final BigDecimal bd = (BigDecimal) obj;
				bd.setScale(scale);
				return bd;
			} else
				try {
					final BigDecimal bd = new BigDecimal("" + obj);
					bd.setScale(scale);
					return bd;
				} catch (NumberFormatException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public InputStream getBinaryStream(int i) throws SQLException {
			return this.objectToBinaryStream(this.getObject(i));
		}

		public InputStream getBinaryStream(String colName) throws SQLException {
			return this.objectToBinaryStream(this.getObject(colName));
		}

		private InputStream objectToBinaryStream(Object obj)
				throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof InputStream)
				return (InputStream) obj;
			else if (obj instanceof Blob)
				return ((Blob) obj).getBinaryStream();
			else if (obj instanceof String)
				return new ByteArrayInputStream(((String) obj).getBytes());
			else
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
		}

		public Blob getBlob(int i) throws SQLException {
			return this.objectToBlob(this.getObject(i));
		}

		public Blob getBlob(String colName) throws SQLException {
			return this.objectToBlob(this.getObject(colName));
		}

		private Blob objectToBlob(Object obj) throws SQLException {
			try {
				return (Blob) obj;
			} catch (ClassCastException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
		}

		public boolean getBoolean(int i) throws SQLException {
			return this.objectToBoolean(this.getObject(i));
		}

		public boolean getBoolean(String colName) throws SQLException {
			return this.objectToBoolean(this.getObject(colName));
		}

		private boolean objectToBoolean(Object obj) throws SQLException {
			if (obj == null)
				return false;
			else if (obj instanceof Boolean)
				return ((Boolean) obj).booleanValue();
			else
				return Boolean.valueOf("" + obj).booleanValue();
		}

		public byte getByte(int i) throws SQLException {
			return this.objectToByte(this.getObject(i));
		}

		public byte getByte(String colName) throws SQLException {
			return this.objectToByte(this.getObject(colName));
		}

		private byte objectToByte(Object obj) throws SQLException {
			if (obj == null)
				return 0;
			else if (obj instanceof Byte)
				return ((Byte) obj).byteValue();
			else
				try {
					return Byte.valueOf("" + obj).byteValue();
				} catch (NumberFormatException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public byte[] getBytes(int i) throws SQLException {
			return this.objectToBytes(this.getObject(i));
		}

		public byte[] getBytes(String colName) throws SQLException {
			return this.objectToBytes(this.getObject(colName));
		}

		private byte[] objectToBytes(Object obj) throws SQLException {
			if (obj == null)
				return new byte[0];
			else if (obj instanceof byte[])
				return (byte[]) obj;
			else
				return ("" + obj).getBytes();
		}

		public Reader getCharacterStream(int i) throws SQLException {
			return this.objectToCharacterStream(this.getObject(i));
		}

		public Reader getCharacterStream(String colName) throws SQLException {
			return this.objectToCharacterStream(this.getObject(colName));
		}

		private Reader objectToCharacterStream(Object obj) throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof Reader)
				return (Reader) obj;
			else if (obj instanceof Clob)
				return ((Clob) obj).getCharacterStream();
			else if (obj instanceof String)
				return new StringReader("" + obj);
			else
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
		}

		public Clob getClob(int i) throws SQLException {
			return this.objectToClob(this.getObject(i));
		}

		public Clob getClob(String colName) throws SQLException {
			return this.objectToClob(this.getObject(colName));
		}

		private Clob objectToClob(Object obj) throws SQLException {
			try {
				return (Clob) obj;
			} catch (ClassCastException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
		}

		public int getConcurrency() throws SQLException {
			return ResultSet.CONCUR_READ_ONLY;
		}

		public String getCursorName() throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public Date getDate(int i) throws SQLException {
			return this.objectToDate(this.getObject(i), Calendar.getInstance());
		}

		public Date getDate(String colName) throws SQLException {
			return this.objectToDate(this.getObject(colName), Calendar
					.getInstance());
		}

		public Date getDate(int i, Calendar cal) throws SQLException {
			return this.objectToDate(this.getObject(i), cal);
		}

		public Date getDate(String colName, Calendar cal) throws SQLException {
			return this.objectToDate(this.getObject(colName), cal);
		}

		private Date objectToDate(Object obj, Calendar cal) throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof Date)
				return (Date) obj;
			else
				try {
					return new Date(DateFormat.getInstance().parse("" + obj)
							.getTime());
				} catch (ParseException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public double getDouble(int i) throws SQLException {
			return this.objectToDouble(this.getObject(i));
		}

		public double getDouble(String colName) throws SQLException {
			return this.objectToDouble(this.getObject(colName));
		}

		private double objectToDouble(Object obj) throws SQLException {
			if (obj == null)
				return 0.0;
			else if (obj instanceof Double)
				return ((Double) obj).doubleValue();
			else
				try {
					return Double.parseDouble("" + obj);
				} catch (NumberFormatException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public int getFetchDirection() throws SQLException {
			return ResultSet.FETCH_FORWARD;
		}

		public int getFetchSize() throws SQLException {
			return 1;
		}

		public float getFloat(int i) throws SQLException {
			return this.objectToFloat(this.getObject(i));
		}

		public float getFloat(String colName) throws SQLException {
			return this.objectToFloat(this.getObject(colName));
		}

		private float objectToFloat(Object obj) throws SQLException {
			if (obj == null)
				return 0.0f;
			else if (obj instanceof Float)
				return ((Float) obj).floatValue();
			else
				try {
					return Float.parseFloat("" + obj);
				} catch (NumberFormatException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public int getInt(int i) throws SQLException {
			return this.objectToInt(this.getObject(i));
		}

		public int getInt(String colName) throws SQLException {
			return this.objectToInt(this.getObject(colName));
		}

		private int objectToInt(Object obj) throws SQLException {
			if (obj == null)
				return 0;
			else if (obj instanceof Integer)
				return ((Integer) obj).intValue();
			else
				try {
					return Integer.parseInt("" + obj);
				} catch (NumberFormatException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public long getLong(int i) throws SQLException {
			return this.objectToLong(this.getObject(i));
		}

		public long getLong(String colName) throws SQLException {
			return this.objectToLong(this.getObject(colName));
		}

		private long objectToLong(Object obj) throws SQLException {
			if (obj == null)
				return 0;
			else if (obj instanceof Long)
				return ((Long) obj).longValue();
			else
				try {
					return Long.parseLong("" + obj);
				} catch (NumberFormatException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public ResultSetMetaData getMetaData() throws SQLException {
			if (this.metaData == null)
				this.metaData = new RegistryResultsMetaData(this);
			return this.metaData;
		}

		public Object getObject(int columnIndex) throws SQLException {
			return this.getObject(columnIndex);
		}

		public Object getObject(String columnName) throws SQLException {
			return this.getObject(columnName, null);
		}

		public Object getObject(int i, Map map) throws SQLException {
			// For now, we ignore the type map.
			if (this.isBeforeFirst())
				throw new SQLException(Resources.get("queryNotStarted"));
			else if (this.isAfterLast())
				throw new SQLException(Resources.get("queryExhausted"));
			else if (i < 1 || i > this.colNames.size())
				throw new SQLException(Resources
						.get("colIndexOutRange", "" + i));
			// Get column name.
			final String colName = (String) this.colNames.get(i - 1);
			final Object obj = ((Map) this.rows.get(this.currentRow))
					.get(colName);
			this.lastColWasNull = obj == null;
			return obj;
		}

		public Object getObject(String colName, Map map) throws SQLException {
			int index = this.findColumn(colName);
			return this.getObject(index + 1, map);
		}

		public Ref getRef(int i) throws SQLException {
			return this.objectToRef(this.getObject(i));
		}

		public Ref getRef(String colName) throws SQLException {
			return this.objectToRef(this.getObject(colName));
		}

		private Ref objectToRef(Object obj) throws SQLException {
			try {
				return (Ref) obj;
			} catch (ClassCastException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
		}

		public int getRow() throws SQLException {
			return this.currentRow + 1;
		}

		public short getShort(int i) throws SQLException {
			return this.objectToShort(this.getObject(i));
		}

		public short getShort(String colName) throws SQLException {
			return this.objectToShort(this.getObject(colName));
		}

		private short objectToShort(Object obj) throws SQLException {
			if (obj == null)
				return 0;
			else if (obj instanceof Short)
				return ((Short) obj).shortValue();
			else
				try {
					return Short.parseShort("" + obj);
				} catch (NumberFormatException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public Statement getStatement() throws SQLException {
			return null;
		}

		public String getString(int i) throws SQLException {
			return this.objectToString(this.getObject(i));
		}

		public String getString(String colName) throws SQLException {
			return this.objectToString(this.getObject(colName));
		}

		private String objectToString(Object obj) throws SQLException {
			if (obj == null)
				return null;
			else
				return "" + obj;
		}

		public Time getTime(int i) throws SQLException {
			return this.objectToTime(this.getObject(i), Calendar.getInstance());
		}

		public Time getTime(String colName) throws SQLException {
			return this.objectToTime(this.getObject(colName), Calendar
					.getInstance());
		}

		public Time getTime(int i, Calendar cal) throws SQLException {
			return this.objectToTime(this.getObject(i), cal);
		}

		public Time getTime(String colName, Calendar cal) throws SQLException {
			return this.objectToTime(this.getObject(colName), cal);
		}

		private Time objectToTime(Object obj, Calendar cal) throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof Time)
				return (Time) obj;
			else
				try {
					return new Time(DateFormat.getInstance().parse("" + obj)
							.getTime());
				} catch (ParseException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public Timestamp getTimestamp(int i) throws SQLException {
			return this.objectToTimestamp(this.getObject(i), Calendar
					.getInstance());
		}

		public Timestamp getTimestamp(String colName) throws SQLException {
			return this.objectToTimestamp(this.getObject(colName), Calendar
					.getInstance());
		}

		public Timestamp getTimestamp(int i, Calendar cal) throws SQLException {
			return this.objectToTimestamp(this.getObject(i), cal);
		}

		public Timestamp getTimestamp(String colName, Calendar cal)
				throws SQLException {
			return this.objectToTimestamp(this.getObject(colName), cal);
		}

		private Timestamp objectToTimestamp(Object obj, Calendar cal)
				throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof Timestamp)
				return (Timestamp) obj;
			else
				try {
					return new Timestamp(DateFormat.getInstance().parse(
							"" + obj).getTime());
				} catch (ParseException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public int getType() throws SQLException {
			return ResultSet.TYPE_SCROLL_INSENSITIVE;
		}

		public URL getURL(int i) throws SQLException {
			return this.objectToURL(this.getObject(i));
		}

		public URL getURL(String colName) throws SQLException {
			return this.objectToURL(this.getObject(colName));
		}

		private URL objectToURL(Object obj) throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof URL)
				return (URL) obj;
			else
				try {
					return new URL("" + obj);
				} catch (MalformedURLException e) {
					throw new SQLException(Resources.get("noTypeConversion",
							obj.getClass().getName()));
				}
		}

		public InputStream getUnicodeStream(int i) throws SQLException {
			return this.objectToUnicodeStream(this.getObject(i));
		}

		public InputStream getUnicodeStream(String colName) throws SQLException {
			return this.objectToUnicodeStream(this.getObject(colName));
		}

		private InputStream objectToUnicodeStream(Object obj)
				throws SQLException {
			if (obj == null)
				return null;
			else if (obj instanceof InputStream)
				return (InputStream) obj;
			else
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
		}

		public SQLWarning getWarnings() throws SQLException {
			return null;
		}

		public void insertRow() throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public boolean isAfterLast() throws SQLException {
			return this.currentRow >= this.rows.size();
		}

		public boolean isBeforeFirst() throws SQLException {
			return this.currentRow < 0;
		}

		public boolean isFirst() throws SQLException {
			return this.currentRow == 0;
		}

		public boolean isLast() throws SQLException {
			return this.currentRow == this.rows.size() - 1;
		}

		public boolean last() throws SQLException {
			this.currentRow = this.rows.size() - 1;
			return true;
		}

		public void moveToCurrentRow() throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void moveToInsertRow() throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public boolean next() throws SQLException {
			this.currentRow++;
			return this.currentRow >= 0 && this.currentRow < this.rows.size();
		}

		public boolean previous() throws SQLException {
			this.currentRow--;
			return this.currentRow >= 0 && this.currentRow < this.rows.size();
		}

		public void refreshRow() throws SQLException {
			// Does nothing.
		}

		public boolean relative(int rows) throws SQLException {
			this.currentRow += rows;
			if (this.currentRow < 0)
				this.currentRow = -1;
			else if (this.currentRow > this.rows.size())
				this.currentRow = this.rows.size();
			return this.currentRow >= 0 && this.currentRow < this.rows.size();
		}

		public boolean rowDeleted() throws SQLException {
			return false;
		}

		public boolean rowInserted() throws SQLException {
			return false;
		}

		public boolean rowUpdated() throws SQLException {
			return false;
		}

		public void setFetchDirection(int direction) throws SQLException {
			// Ignored.
		}

		public void setFetchSize(int rows) throws SQLException {
			// Ignored.
		}

		public void updateArray(int columnIndex, Array x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateArray(String columnName, Array x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateAsciiStream(int columnIndex, InputStream x, int length)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateAsciiStream(String columnName, InputStream x,
				int length) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBigDecimal(int columnIndex, BigDecimal x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBigDecimal(String columnName, BigDecimal x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBinaryStream(int columnIndex, InputStream x,
				int length) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBinaryStream(String columnName, InputStream x,
				int length) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBlob(int columnIndex, Blob x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBlob(String columnName, Blob x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBoolean(int columnIndex, boolean x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBoolean(String columnName, boolean x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateByte(int columnIndex, byte x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateByte(String columnName, byte x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBytes(int columnIndex, byte[] x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateBytes(String columnName, byte[] x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateCharacterStream(int columnIndex, Reader x, int length)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateCharacterStream(String columnName, Reader reader,
				int length) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateClob(int columnIndex, Clob x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateClob(String columnName, Clob x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateDate(int columnIndex, Date x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateDate(String columnName, Date x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateDouble(int columnIndex, double x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateDouble(String columnName, double x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateFloat(int columnIndex, float x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateFloat(String columnName, float x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateInt(int columnIndex, int x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateInt(String columnName, int x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateLong(int columnIndex, long x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateLong(String columnName, long x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateNull(int columnIndex) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateNull(String columnName) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateObject(int columnIndex, Object x, int scale)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateObject(int columnIndex, Object x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateObject(String columnName, Object x, int scale)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateObject(String columnName, Object x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateRef(int columnIndex, Ref x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateRef(String columnName, Ref x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateRow() throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateShort(int columnIndex, short x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateShort(String columnName, short x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateString(int columnIndex, String x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateString(String columnName, String x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateTime(int columnIndex, Time x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateTime(String columnName, Time x) throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateTimestamp(int columnIndex, Timestamp x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public void updateTimestamp(String columnName, Timestamp x)
				throws SQLException {
			throw new SQLException(Resources.get("connectionReadOnly"));
		}

		public boolean wasNull() throws SQLException {
			return this.lastColWasNull;
		}

	}

	public class RegistryResultsMetaData implements ResultSetMetaData {

		private RegistryResultSet rs;

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
		RegistryResultsMetaData(final RegistryResultSet rs) throws SQLException {
			this.rs = rs;
			this.populate();
		}

		private void populate() throws SQLException {
			// Copy column details from columns in result set.
			for (final Iterator i = rs.colNames.iterator(); i.hasNext();) {
				final String colName = (String) i.next();
				final MetaData md = new MetaData();
				// Fake it all with strings.
				md.columnTypeName = "String";
				md.columnClassName = "java.lang.String";
				md.columnType = Types.VARCHAR;
				md.isSigned = false;
				md.isCurrency = false;
				md.scale = 0;
				md.precision = 0;
				md.catalogName = "BioMart";
				md.schemaName = "BioMart";
				md.tableName = md.schemaName;
				md.columnName = (String) colName;
				md.columnLabel = md.columnName;
				md.columnDisplaySize = Integer.MAX_VALUE;
			}
		}

		private MetaData getMetaData(int column) throws SQLException {
			if (column < 1 || column >= this.metaData.length)
				throw new SQLException(Resources.get("colIndexOutRange", ""
						+ column));
			return this.metaData[column - 1];
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

}
