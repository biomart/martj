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
package org.biomart.builder.controller.dialects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.controller.JDBCSchema;
import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.DataSet.DataSetConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.model.DataSet.DataSetTableRestriction;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.MartConstructorAction.Concat;
import org.biomart.builder.model.MartConstructorAction.Create;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.ExpressionAddColumns;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.Merge;
import org.biomart.builder.model.MartConstructorAction.OptimiseAddColumn;
import org.biomart.builder.model.MartConstructorAction.OptimiseUpdateColumn;
import org.biomart.builder.model.MartConstructorAction.Partition;
import org.biomart.builder.model.MartConstructorAction.PlaceHolder;
import org.biomart.builder.model.MartConstructorAction.Reduce;
import org.biomart.builder.model.MartConstructorAction.Rename;
import org.biomart.builder.model.MartConstructorAction.Union;

/**
 * Understands how to create SQL and DDL for a PostgreSQL database.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.13, 14th August 2006
 * @since 0.1
 */
public class PostgreSQLDialect extends DatabaseDialect {

	private boolean cleanState;

	private int indexCount;

	public boolean understandsDataLink(final DataLink dataLink) {

		// Convert to JDBC version.
		if (!(dataLink instanceof JDBCDataLink))
			return false;
		final JDBCDataLink jddl = (JDBCDataLink) dataLink;

		try {
			return jddl.getConnection().getMetaData().getDatabaseProductName()
					.equals("PostgreSQL");
		} catch (final SQLException e) {
			throw new MartBuilderInternalError(e);
		}
	}

	public void reset() {
		this.cleanState = true;
		this.indexCount = 0;
	}

	public List executeSelectDistinct(final Column col) throws SQLException {
		final String colName = col.getName();
		final String tableName = col.getTable().getName();
		final Schema schema = col.getTable().getSchema();

		// The complex case - where we need to do a union of
		// select distincts.
		if (schema instanceof SchemaGroup) {
			final Set results = new HashSet();
			for (final Iterator i = ((SchemaGroup) schema).getSchemas()
					.iterator(); i.hasNext();) {
				final Schema member = (Schema) i.next();
				results.addAll(this.executeSelectDistinct(member
						.getTableByName(tableName).getColumnByName(colName)));
			}
			return new ArrayList(results);
		}

		// The simple case where we actually do a select distinct.
		final List results = new ArrayList();
		final String schemaName = ((JDBCSchema) schema).getDatabaseSchema();
		final Connection conn = ((JDBCSchema) schema).getConnection();
		final ResultSet rs = conn.prepareStatement(
				"select distinct " + colName + " from " + schemaName + "."
						+ tableName).executeQuery();
		while (rs.next())
			results.add(rs.getString(1));
		rs.close();
		return results;
	}

	public List executeSelectRows(final Table table, final int offset,
			final int count) throws SQLException {
		final String tableName = table.getName();
		final Schema schema = table.getSchema();

		// Build up a list of column names.
		final StringBuffer colNames = new StringBuffer();
		for (final Iterator i = table.getColumns().iterator(); i.hasNext();) {
			colNames.append(((Column) i.next()).getName());
			if (i.hasNext())
				colNames.append(',');
		}

		// The simple case where we actually do a select.
		final List results = new ArrayList();
		final String schemaName = ((JDBCSchema) schema).getDatabaseSchema();
		final Connection conn = ((JDBCSchema) schema).getConnection();
		final ResultSet rs = conn.prepareStatement(
				"select " + colNames.toString() + " from " + schemaName + "."
						+ tableName + " limit " + count + " offset " + count)
				.executeQuery();
		while (rs.next()) {
			final List values = new ArrayList();
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
				values.add(rs.getObject(i));
			results.add(values);
		}
		rs.close();
		return results;
	}

	public String[] getStatementsForAction(final MartConstructorAction action,
			final boolean includeComments) throws ConstructorException {

		final List statements = new ArrayList();

		// Initial schema creation step.
		if (this.cleanState)
			statements.add("create schema " + action.getDataSetSchemaName());
		this.cleanState = false;

		if (includeComments)
			statements.add("-- " + action.getStatusMessage());

		try {
			final String className = action.getClass().getName();
			final String methodName = "do"
					+ className.substring(className.lastIndexOf('$') + 1);
			final Method method = this.getClass().getMethod(methodName,
					new Class[] { action.getClass(), List.class });
			method.invoke(this, new Object[] { action, statements });
		} catch (final InvocationTargetException ite) {
			final Throwable t = ite.getCause();
			if (t instanceof ConstructorException)
				throw (ConstructorException) t;
			else
				throw new ConstructorException(t);
		} catch (final Throwable t) {
			if (t instanceof ConstructorException)
				throw (ConstructorException) t;
			else
				throw new ConstructorException(t);
		}

		return (String[]) statements.toArray(new String[0]);
	}

	public void doReduce(final Reduce action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String srcTableName = action.getSourceTableName();
		final String trgtSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String trgtTableName = action.getTargetTableName();
		final String reduceSchemaName = action.getReducedTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getReducedTableSchema())
						.getDatabaseSchema();
		final String reduceTableName = action.getReducedTableName();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ srcSchemaName + "," + trgtSchemaName + "," + reduceSchemaName
				+ ",pg_catalog");

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + reduceSchemaName + "." + reduceTableName
				+ " as select b.* from " + srcSchemaName + "." + srcTableName
				+ " as a inner join " + trgtSchemaName + "." + trgtTableName
				+ " as b on ");
		for (int i = 0; i < action.getTargetTableKeyColumns().size(); i++) {
			if (i > 0)
				sb.append(',');
			final String pkColName = ((Column) action
					.getSourceTableKeyColumns().get(i)).getName();
			final String fkColName = ((Column) action
					.getTargetTableKeyColumns().get(i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName);
		}

		statements.add(sb.toString());
	}

	public void doDrop(final Drop action, final List statements)
			throws Exception {
		final String schemaName = action.getDropTableSchema() == null ? action
				.getDataSetSchemaName() : ((JDBCSchema) action
				.getDropTableSchema()).getDatabaseSchema();
		final String tableName = action.getDropTableName();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ schemaName + ",pg_catalog");

		statements.add("drop table " + schemaName + "." + tableName);
	}

	public void doRename(final Rename action, final List statements)
			throws Exception {
		final String schemaName = action.getRenameTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getRenameTableSchema())
						.getDatabaseSchema();
		final String oldTableName = action.getRenameTableOldName();
		final String newTableName = action.getRenameTableNewName();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ schemaName + ",pg_catalog");

		statements.add("alter table " + schemaName + "." + oldTableName
				+ " rename to " + newTableName);
	}

	public void doUnion(final Union action, final List statements)
			throws Exception {
		final String schemaName = action.getUnionTableSchema() == null ? action
				.getDataSetSchemaName() : ((JDBCSchema) action
				.getUnionTableSchema()).getDatabaseSchema();
		final String tableName = action.getUnionTableName();
		final StringBuffer sb = new StringBuffer();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ schemaName + ",pg_catalog");

		sb.append("create table " + schemaName + "." + tableName
				+ " as select * from ");
		for (int i = 0; i < action.getTargetTableSchemas().size(); i++) {
			if (i > 0)
				sb.append(" union select * from ");
			final String targetSchemaName = action.getTargetTableSchemas().get(
					i) == null ? action.getDataSetSchemaName()
					: ((Schema) action.getTargetTableSchemas().get(i))
							.getName();
			final String targetTableName = (String) action
					.getTargetTableNames().get(i);
			sb.append(targetSchemaName);
			sb.append('.');
			sb.append(targetTableName);
		}
		statements.add(sb.toString());
	}

	public void doPlaceHolder(final PlaceHolder action, final List statements) {
		statements.add("--");
	}

	public void doIndex(final Index action, final List statements) {
		final String schemaName = action.getIndexTableSchema() == null ? action
				.getDataSetSchemaName() : ((JDBCSchema) action
				.getIndexTableSchema()).getDatabaseSchema();
		final String tableName = action.getIndexTableName();
		final StringBuffer sb = new StringBuffer();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ schemaName + ",pg_catalog");

		sb.append("create index " + tableName + "_I_" + this.indexCount++
				+ " on " + schemaName + "." + tableName + "(");
		for (final Iterator i = action.getIndexColumns().iterator(); i
				.hasNext();) {
			final Object obj = i.next();
			if (obj instanceof Column) {
				final Column col = (Column) obj;
				sb.append(col.getName());
			} else if (obj instanceof String)
				sb.append(obj);
			else
				throw new MartBuilderInternalError();
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(")");

		statements.add(sb.toString());
	}

	public void doOptimiseAddColumn(final OptimiseAddColumn action,
			final List statements) {
		final String schemaName = action.getTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTableSchema()).getDatabaseSchema();
		final String tableName = action.getTableName();
		final String colName = action.getColumnName();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ schemaName + ",pg_catalog");

		statements.add("alter table " + schemaName + "." + tableName
				+ " add column " + colName + " integer default 0");
	}

	public void doOptimiseUpdateColumn(final OptimiseUpdateColumn action,
			final List statements) throws Exception {
		final String pkSchemaName = action.getPkTableSchema() == null ? action
				.getDataSetSchemaName() : ((JDBCSchema) action
				.getPkTableSchema()).getDatabaseSchema();
		final String pkTableName = action.getPkTableName();
		final String fkSchemaName = action.getFkTableSchema() == null ? action
				.getDataSetSchemaName() : ((JDBCSchema) action
				.getFkTableSchema()).getDatabaseSchema();
		final String fkTableName = action.getFkTableName();
		final String colName = action.getOptimiseColumnName();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ pkSchemaName + "," + fkSchemaName + ",pg_catalog");

		final StringBuffer sb = new StringBuffer();
		sb.append("update " + pkSchemaName + "." + pkTableName + " set "
				+ colName + "=(select count(1) from " + fkSchemaName + "."
				+ fkTableName + " b where ");
		for (int i = 0; i < action.getPkColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final Column pkCol = (Column) action.getPkColumns().get(i);
			final Column fkCol = (Column) action.getFkColumns().get(i);
			sb.append(pkSchemaName + "." + pkTableName + ".");
			sb.append(pkCol.getName());
			sb.append("=b.");
			sb.append(fkCol.getName());
		}
		for (int i = 0; i < action.getCountNotNullColumns().size(); i++) {
			final Column col = (Column) action.getCountNotNullColumns().get(i);
			// Skip those in FK as already checked.
			if (action.getFkColumns().contains(col))
				continue;
			// Check column not null.
			sb.append(" and b.");
			sb.append(col.getName());
			sb.append(" is not null");
		}
		sb.append(')');

		statements.add(sb.toString());
	}

	public void doCreate(final Create action, final List statements) {
		final String createTableSchema = action.getNewTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getNewTableSchema()).getDatabaseSchema();
		final String createTableName = action.getNewTableName();
		final String fromTableSchema = action.getSelectFromTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSelectFromTableSchema())
						.getDatabaseSchema();
		final String fromTableName = action.getSelectFromTableName();
		final DataSetTableRestriction tblRestriction = action
				.getTableRestriction();
		final boolean useDistinct = action.isUseDistinct();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ createTableSchema + "," + fromTableSchema + ",pg_catalog");

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + createTableSchema + "." + createTableName
				+ " as select ");
		if (useDistinct)
			sb.append("distinct ");
		for (final Iterator i = action.getSelectFromColumns().iterator(); i
				.hasNext();) {
			final Column col = (Column) i.next();
			if (action.isUseAliases()) {
				final DataSetColumn dsCol = (DataSetColumn) col;
				if (dsCol instanceof WrappedColumn) {
					sb.append("a.");
					sb.append(((WrappedColumn) dsCol).getWrappedColumn()
							.getName());
					sb.append(" as ");
				} else if (dsCol instanceof SchemaNameColumn) {
					sb.append('\'');
					sb.append(fromTableSchema);
					sb.append("' as ");
				} else
					// Ouch!
					throw new MartBuilderInternalError();
			}
			sb.append(col.getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + fromTableSchema + "." + fromTableName + " as a");

		// Do restriction.
		if (tblRestriction != null) {
			sb.append(" where ");
			sb.append(tblRestriction.getSubstitutedExpression("a"));
		}

		statements.add(sb.toString());
	}

	public void doPartition(final Partition action, final List statements) {
		final String partTableSchema = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String partTableName = action.getTargetTableName();
		final String fromTableSchema = action.getPartitionTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getPartitionTableSchema())
						.getDatabaseSchema();
		final String fromTableName = action.getPartitionTableName();
		final String partColumnName = action.getPartitionColumnName();
		final Object partColumnValue = action.getPartitionColumnValue();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ partTableSchema + "," + fromTableSchema + ",pg_catalog");

		if (partColumnValue != null) {
			String escapedValue = partColumnValue.toString();
			escapedValue = escapedValue.replaceAll("\\\\", "\\\\");
			escapedValue = escapedValue.replaceAll("'", "\\'");
			statements.add("create table " + partTableSchema + "."
					+ partTableName + " as select * from " + fromTableSchema
					+ "." + fromTableName + " where " + partColumnName + "='"
					+ escapedValue + "'");
		} else
			statements.add("create table " + partTableSchema + "."
					+ partTableName + " as select * from " + fromTableSchema
					+ "." + fromTableName + " where " + partColumnName
					+ " is null");
	}

	public void doMerge(final Merge action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String srcTableName = action.getSourceTableName();
		final String trgtSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String trgtTableName = action.getTargetTableName();
		final String mergeSchemaName = action.getMergedTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getMergedTableSchema())
						.getDatabaseSchema();
		final String mergeTableName = action.getMergedTableName();
		final DataSetRelationRestriction relRestriction = action
				.getRelationRestriction();
		final DataSetTableRestriction tblRestriction = action
				.getTargetTableRestriction();
		final boolean firstIsSource = action.isFirstTableSourceTable();
		final boolean useDistinct = action.isUseDistinct();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ srcSchemaName + "," + trgtSchemaName + "," + mergeSchemaName
				+ ",pg_catalog");

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + mergeSchemaName + "." + mergeTableName
				+ " as select ");
		if (useDistinct)
			sb.append("distinct ");
		sb.append("a.*");
		for (final Iterator i = action.getTargetTableColumns().iterator(); i
				.hasNext();) {
			sb.append(",b.");
			final Column col = (Column) i.next();
			if (action.isUseAliases()) {
				final DataSetColumn dsCol = (DataSetColumn) col;
				if (dsCol instanceof WrappedColumn) {
					sb.append(((WrappedColumn) dsCol).getWrappedColumn()
							.getName());
					sb.append(" as ");
				} else if (dsCol instanceof SchemaNameColumn) {
					sb.append('\'');
					sb.append(trgtSchemaName);
					sb.append("' as ");
				} else
					// Ouch!
					throw new MartBuilderInternalError();
			}
			sb.append(col.getName());
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName
				+ " as a left join " + trgtSchemaName + "." + trgtTableName
				+ " as b on ");
		for (int i = 0; i < action.getTargetTableKeyColumns().size(); i++) {
			if (i > 0)
				sb.append(',');
			final String pkColName = ((Column) action
					.getSourceTableKeyColumns().get(i)).getName();
			final String fkColName = ((Column) action
					.getTargetTableKeyColumns().get(i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName);
		}

		// Do restriction.
		if (relRestriction != null || tblRestriction != null)
			sb.append(" where ");
		if (relRestriction != null)
			sb.append(relRestriction.getSubstitutedExpression(
					firstIsSource ? "a" : "b", firstIsSource ? "b" : "a"));
		if (relRestriction != null && tblRestriction != null)
			sb.append(" and ");
		if (tblRestriction != null)
			sb.append(tblRestriction.getSubstitutedExpression("b"));

		statements.add(sb.toString());
	}

	public void doExpressionAddColumns(final ExpressionAddColumns action,
			final List statements) throws Exception {
		final String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String srcTableName = action.getSourceTableName();
		final String trgtSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String trgtTableName = action.getTargetTableName();
		final boolean useGroupBy = action.getUseGroupBy();
		final Collection selectCols = action.getSourceTableColumns();
		final Collection expressCols = action.getExpressionColumns();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ srcSchemaName + "," + trgtSchemaName + ",pg_catalog");

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + trgtSchemaName + "." + trgtTableName
				+ " as select ");
		for (final Iterator i = selectCols.iterator(); i.hasNext();) {
			final Column col = (Column) i.next();
			sb.append(col.getName());
			sb.append(',');
		}
		for (final Iterator i = expressCols.iterator(); i.hasNext();) {
			final ExpressionColumn col = (ExpressionColumn) i.next();
			sb.append(col.getSubstitutedExpression());
			sb.append(" as ");
			sb.append(col.getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName);
		if (useGroupBy) {
			sb.append(" group by ");
			for (final Iterator i = selectCols.iterator(); i.hasNext();) {
				final Column col = (Column) i.next();
				sb.append(col.getName());
				if (i.hasNext())
					sb.append(',');
			}
		}
		statements.add(sb.toString());
	}

	public void doConcat(final Concat action, final List statements) {
		final String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String srcTableName = action.getSourceTableName();
		final List srcTableKeyCols = action.getSourceTableKeyColumns();
		final String trgtSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String trgtTableName = action.getTargetTableName();
		final String trgtColName = action.getConcatColumnName();
		final String concatSchemaName = action.getConcatTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getConcatTableSchema())
						.getDatabaseSchema();
		final String concatTableName = action.getConcatTableName();
		final String columnSep = action.getColumnSeparator();
		final String recordSep = action.getRecordSeparator();
		final DataSetRelationRestriction relRestriction = action
				.getRelationRestriction();
		final DataSetTableRestriction tblRestriction = action
				.getTargetTableRestriction();
		final boolean firstIsSource = action.isFirstTableSourceTable();

		statements.add("set search_path=" + action.getDataSetSchemaName() + ","
				+ srcSchemaName + "," + trgtSchemaName + "," + concatSchemaName
				+ ",pg_catalog");

		final StringBuffer sb = new StringBuffer();

		sb.append("create table " + concatSchemaName + "." + concatTableName
				+ " as select a.*, array_to_string(array(select ");
		for (final Iterator i = action.getTargetTableConcatColumns().iterator(); i
				.hasNext();) {
			final Column col = (Column) i.next();
			sb.append("b.");
			sb.append(col.getName());
			if (i.hasNext()) {
				sb.append("||'");
				sb.append(columnSep);
				sb.append("'||");
			}
		}
		sb.append(" from ");
		sb.append(trgtSchemaName + "." + trgtTableName);
		sb.append(" as b where ");
		for (int i = 0; i < action.getTargetTableKeyColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = ((Column) action
					.getSourceTableKeyColumns().get(i)).getName();
			final String fkColName = ((Column) action
					.getTargetTableKeyColumns().get(i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName);
		}

		// Do restrictions.
		if (relRestriction != null) {
			sb.append(" and ");
			sb.append(relRestriction.getSubstitutedExpression(
					firstIsSource ? "a" : "b", firstIsSource ? "b" : "a"));
		}
		if (tblRestriction != null) {
			sb.append(" and ");
			sb.append(tblRestriction.getSubstitutedExpression("b"));
		}

		sb.append("),'");
		sb.append(recordSep);
		sb.append("') as ");
		sb.append(trgtColName);

		sb.append(" from " + srcSchemaName + "." + srcTableName + " as a ");

		// Do group-by.
		sb.append(" group by ");
		for (final Iterator i = srcTableKeyCols.iterator(); i.hasNext();) {
			final Column srcKeyCol = (Column) i.next();
			sb.append("a.");
			sb.append(srcKeyCol.getName());
			if (i.hasNext())
				sb.append(',');
		}

		statements.add(sb.toString());
	}
}
