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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.MartConstructorAction.Concat;
import org.biomart.builder.model.MartConstructorAction.Create;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.ExpressionAddColumns;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.Merge;
import org.biomart.builder.model.MartConstructorAction.OptimiseAddColumn;
import org.biomart.builder.model.MartConstructorAction.OptimiseCopyColumn;
import org.biomart.builder.model.MartConstructorAction.OptimiseUpdateColumn;
import org.biomart.builder.model.MartConstructorAction.Partition;
import org.biomart.builder.model.MartConstructorAction.PlaceHolder;
import org.biomart.builder.model.MartConstructorAction.Reduce;
import org.biomart.builder.model.MartConstructorAction.RenameTable;
import org.biomart.builder.model.MartConstructorAction.Union;
import org.biomart.common.controller.JDBCSchema;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;

/**
 * Understands how to create SQL and DDL for an Oracle database.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class OracleDialect extends DatabaseDialect {
	// Check we only make the aggregate functions once.
	private static boolean GROUP_CONCAT_CREATED;

	private int indexCount;

	public void doConcat(final Concat action, final List statements) {
		final String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String srcTableName = action.getSourceTableName();
		final List srcTableKeyCols = action.getSourceTablePKColumns();
		final String trgtSchemaName = action.getConcatTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getConcatTableSchema())
						.getDatabaseSchema();
		final String trgtTableName = action.getConcatTableName();
		final String trgtColName = action.getTargetTableConcatColumnName();
		final String concatSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String concatTableName = action.getTargetTableName();
		final String columnSep = action.getColumnSeparator();
		final String recordSep = action.getRecordSeparator();
		final boolean firstIsSource = action.isFirstTableSourceTable();

		final StringBuffer sb = new StringBuffer();

		// If we haven't defined the group_concat function yet, define it.
		// Note limitation of total 32767 characters for group_concat result.
		if (!OracleDialect.GROUP_CONCAT_CREATED) {
			final BufferedReader br = new BufferedReader(
					new InputStreamReader(
							Resources
									.getResourceAsStream("ora_group_concat.sql")));
			try {
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
					sb.append(System.getProperty("line.separator"));
				}
			} catch (final IOException e) {
				throw new BioMartError(e);
			}
			OracleDialect.GROUP_CONCAT_CREATED = true;
		}

		sb.append("create table " + concatSchemaName + "." + concatTableName
				+ " as select a.*, group_concat(concat_expr(");
		for (final Iterator i = action.getConcatTableConcatColumns().iterator(); i
				.hasNext();) {
			final Column col = (Column) i.next();
			sb.append(col.getName());
			if (i.hasNext()) {
				sb.append("||'");
				sb.append(columnSep);
				sb.append("'||");
			}
		}
		sb.append(",'");
		sb.append(recordSep);
		sb.append("')) as ");
		sb.append(trgtColName);

		sb.append(" from " + srcSchemaName + "." + srcTableName
				+ " a inner join " + trgtSchemaName + "." + trgtTableName
				+ " b on ");
		for (int i = 0; i < action.getConcatTableFKColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = ((Column) action.getSourceTablePKColumns()
					.get(i)).getName();
			final String fkColName = ((Column) action.getConcatTableFKColumns()
					.get(i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName + "");
		}

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

	public void doCreate(final Create action, final List statements) {
		final String createTableSchema = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String createTableName = action.getTargetTableName();
		final String fromTableSchema = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String fromTableName = action.getSourceTableName();
		final boolean useDistinct = action.isUseDistinct();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + createTableSchema + "." + createTableName
				+ " as select ");
		if (useDistinct)
			sb.append("distinct ");
		for (final Iterator i = action.getSelectFromColumns().iterator(); i
				.hasNext();) {
			final Column col = (Column) i.next();
			sb.append("a.");
			if (action.isUseAliases()) {
				final DataSetColumn dsCol = (DataSetColumn) col;
				if (dsCol instanceof WrappedColumn) {
					sb.append(((WrappedColumn) dsCol).getWrappedColumn()
							.getName());
					sb.append(" as ");
				} else if (dsCol instanceof SchemaNameColumn) {
					sb.append('\'');
					sb.append(fromTableSchema);
					sb.append("' as ");
				} else
					// Ouch!
					throw new BioMartError();
			} else if (action.isUseInheritedAliases())
				if (col instanceof InheritedColumn) {
					sb.append(((InheritedColumn) col).getInheritedColumn()
							.getName());
					sb.append(" as ");
				}
			sb.append(col.getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + fromTableSchema + "." + fromTableName + " a");

		statements.add(sb.toString());
	}

	public void doDrop(final Drop action, final List statements)
			throws Exception {
		final String schemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String tableName = action.getTargetTableName();
		statements.add("drop table " + schemaName + "." + tableName + "");
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
		final Collection expressCols = action.getTargetExpressionColumns();

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
		sb.append(" from " + srcSchemaName + "." + srcTableName + "");
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

	public void doIndex(final Index action, final List statements) {
		final String schemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String tableName = action.getTargetTableName();
		final StringBuffer sb = new StringBuffer();

		sb.append("create index " + schemaName + "." + tableName + "_I_"
				+ this.indexCount++ + " on " + schemaName + "." + tableName
				+ "(");
		for (final Iterator i = action.getIndexColumns().iterator(); i
				.hasNext();) {
			final Object obj = i.next();
			if (obj instanceof Column) {
				final Column col = (Column) obj;
				sb.append(col.getName());
			} else if (obj instanceof String)
				sb.append(obj);
			else
				throw new BioMartError();
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(")");

		statements.add(sb.toString());
	}

	public void doMerge(final Merge action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String srcTableName = action.getSourceTableName();
		final String trgtSchemaName = action.getMergeTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getMergeTableSchema())
						.getDatabaseSchema();
		final String trgtTableName = action.getMergeTableName();
		final String mergeSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String mergeTableName = action.getTargetTableName();
		final boolean firstIsSource = action.isFirstTableSourceTable();
		final boolean useDistinct = action.isUseDistinct();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + mergeSchemaName + "." + mergeTableName
				+ " as select ");
		if (useDistinct)
			sb.append("distinct ");
		final List sourceCols = action.getSourceTableSelectColumns();
		if (sourceCols == null)
			sb.append("a.*");
		else
			for (final Iterator i = sourceCols.iterator(); i.hasNext();) {
				final Column col = (Column) i.next();
				sb.append("a.");
				if (action.isUseLHSAliases()) {
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
						throw new BioMartError();
				}
				sb.append(col.getName());
				if (i.hasNext())
					sb.append(',');
			}
		for (final Iterator i = action.getMergeTableSelectColumns().iterator(); i
				.hasNext();) {
			sb.append(",b.");
			final Column col = (Column) i.next();
			if (action.isUseRHSAliases()) {
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
					throw new BioMartError();
			}
			sb.append(col.getName());
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName
				+ " a left join " + trgtSchemaName + "." + trgtTableName
				+ " b on ");
		for (int i = 0; i < action.getMergeTableJoinColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = ((Column) action
					.getSourceTableJoinColumns().get(i)).getName();
			final String fkColName = ((Column) action
					.getMergeTableJoinColumns().get(i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName + "");
		}
		
		statements.add(sb.toString());
	}

	public void doOptimiseAddColumn(final OptimiseAddColumn action,
			final List statements) {
		final String schemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String tableName = action.getTargetTableName();
		final String colName = action.getColumnName();
		statements.add("alter table " + schemaName + "." + tableName
				+ " add (" + colName + " number default 0)");
	}

	public void doOptimiseUpdateColumn(final OptimiseUpdateColumn action,
			final List statements) throws Exception {
		final String pkSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String pkTableName = action.getTargetTableName();
		final String fkSchemaName = action.getCountTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getCountTableSchema())
						.getDatabaseSchema();
		final String fkTableName = action.getCountTableName();
		final String colName = action.getOptimiseColumnName();

		final String countStmt = action.getUseBoolInstead() ? "decode(count(1),0,0,1)"
				: "count(1)";

		final StringBuffer sb = new StringBuffer();
		sb
				.append("update " + pkSchemaName + "." + pkTableName
						+ " a set " + colName + "=(select " + countStmt
						+ " from " + fkSchemaName + "." + fkTableName
						+ " b where ");
		for (int i = 0; i < action.getTargetTablePKColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final Column pkCol = (Column) action.getTargetTablePKColumns().get(
					i);
			final Column fkCol = (Column) action.getCountTableFKColumns()
					.get(i);
			sb.append("a.");
			sb.append(pkCol.getName());
			sb.append("=b.");
			sb.append(fkCol.getName());
		}
		for (int i = 0; i < action.getCountTableNotNullColumns().size(); i++) {
			final Column col = (Column) action.getCountTableNotNullColumns()
					.get(i);
			// Skip those in FK as already checked.
			if (action.getCountTableFKColumns().contains(col))
				continue;
			// Check column not null.
			sb.append(" and b.");
			sb.append(col.getName());
			sb.append(" is not null");
		}
		sb.append(')');

		statements.add(sb.toString());
	}

	public void doOptimiseCopyColumn(final OptimiseCopyColumn action,
			final List statements) throws Exception {
		final String pkSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String pkTableName = action.getTargetTableName();
		final String fkSchemaName = action.getCountTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getCountTableSchema())
						.getDatabaseSchema();
		final String fkTableName = action.getCountTableName();
		final String interSchemaName = action.getIntermediateTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getIntermediateTableSchema())
						.getDatabaseSchema();
		final String interTableName = action.getIntermediateTableName();
		final String colName = action.getOptimiseColumnName();

		final String countStmt = action.getUseBoolInstead() ? "max" : "sum";
		final StringBuffer sb = new StringBuffer();

		if (interTableName.equals(fkTableName)
				&& ((interSchemaName == fkSchemaName) || (interSchemaName != null && interSchemaName
						.equals(fkSchemaName)))

		) {
			// Direct link.
			sb.append("update " + pkSchemaName + "." + pkTableName
					+ " a set " + colName + "=(select " + countStmt
					+ "(c." + colName + ") from " + fkSchemaName + ".`"
					+ fkTableName + "` c where ");
			for (int i = 0; i < action.getTargetTablePKColumns().size(); i++) {
				if (i > 0)
					sb.append(" and ");
				final Column pkCol = (Column) action.getTargetTablePKColumns()
						.get(i);
				sb.append("a.");
				sb.append(pkCol.getName());
				sb.append("=c.");
				sb.append(pkCol.getName());
				sb.append("");
			}
			sb.append(')');
		} else {
			// Intermediate table link.
			sb.append("update " + pkSchemaName + "." + pkTableName
					+ " a set " + colName + "=(select " + countStmt
					+ "(c." + colName + ") from " + fkSchemaName + "."
					+ fkTableName + " b inner join " + interSchemaName
					+ "." + interTableName + " c on ");
			for (int i = 0; i < action.getCountTableFKColumns().size(); i++) {
				if (i > 0)
					sb.append(" and ");
				final Column fkCol = (Column) action.getCountTableFKColumns()
						.get(i);
				sb.append("b.");
				sb.append(fkCol.getName());
				sb.append("=c.");
				sb.append(fkCol.getName());
				sb.append("");
			}			
			sb.append(" where ");
			for (int i = 0; i < action.getTargetTablePKColumns().size(); i++) {
				if (i > 0)
					sb.append(" and ");
				final Column pkCol = (Column) action.getTargetTablePKColumns()
						.get(i);
				sb.append("a.");
				sb.append(pkCol.getName());
				sb.append("=b.");
				sb.append(pkCol.getName());
				sb.append("");
			}
			sb.append(')');
		}

		statements.add(sb.toString());
	}

	public void doPartition(final Partition action, final List statements) {
		final String partTableSchema = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String partTableName = action.getTargetTableName();
		final String fromTableSchema = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String fromTableName = action.getSourceTableName();
		final String partColumnName = action.getPartitionColumnName();
		final Object partColumnValue = action.getPartitionColumnValue();
		StringBuffer sb = new StringBuffer();

		String escapedValue = " is null";
		if (partColumnValue != null) {
			escapedValue = partColumnValue.toString();
			escapedValue = escapedValue.replaceAll("\\\\", "\\\\");
			escapedValue = escapedValue.replaceAll("'", "\\'");
			escapedValue = "='" + escapedValue + "'";
		}
		sb.append("create table " + partTableSchema + "." + partTableName
				+ " as select ");

		if (action.getSourceTablePKColumns().isEmpty()) {
			// Do the partition as a simple select where.
			for (Iterator i = action.getSourceTableAllColumns().iterator(); i
					.hasNext();) {
				final String colName = ((Column) i.next()).getName();
				sb.append(colName);
				if (i.hasNext())
					sb.append(',');
			}
			sb.append(" from " + fromTableSchema + "." + fromTableName
					+ " where ");
		} else {
			// Do the partition as a self-leftjoin-self so that
			// we don't lose any foreign keys.
			sb.append("distinct ");
			final List remainingCols = new ArrayList(action
					.getSourceTableAllColumns());
			remainingCols.removeAll(action.getSourceTableFKColumns());
			for (Iterator i = action.getSourceTableFKColumns().iterator(); i
					.hasNext();) {
				final String colName = ((Column) i.next()).getName();
				sb.append("a." + colName);
				sb.append(",");
			}
			for (Iterator i = remainingCols.iterator(); i.hasNext();) {
				final String colName = ((Column) i.next()).getName();
				sb.append("b." + colName);
				if (i.hasNext())
					sb.append(',');
			}
			// select a.FK cols and b.Remaining cols
			sb.append(" from " + fromTableSchema + "." + fromTableName
					+ " a left join " + fromTableSchema + "."
					+ fromTableName + " b on ");
			for (Iterator i = action.getSourceTablePKColumns().iterator(); i
					.hasNext();) {
				final String joinColName = ((Column) i.next()).getName();
				sb
						.append("a." + joinColName + "=b." + joinColName
								+ "");
				sb.append(" and ");
			}
			sb.append("b.");
		}
		sb.append(partColumnName);
		sb.append(escapedValue);
		statements.add(sb.toString());
	}

	public void doPlaceHolder(final PlaceHolder action, final List statements) {
		statements.add("--");
	}

	public void doReduce(final Reduce action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getSourceTableSchema())
						.getDatabaseSchema();
		final String srcTableName = action.getSourceTableName();
		final String trgtSchemaName = action.getReduceTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getReduceTableSchema())
						.getDatabaseSchema();
		final String trgtTableName = action.getReduceTableName();
		final String reduceSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String reduceTableName = action.getTargetTableName();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + reduceSchemaName + "." + reduceTableName
				+ " as select distinct ");
		final List remainingCols = new ArrayList(action
				.getReduceTableAllColumns());
		remainingCols.removeAll(action.getReduceTableFKColumns());
		// Select source.PK and target.remaining
		for (Iterator i = action.getSourceTablePKColumns().iterator(); i
				.hasNext();) {
			final String colName = ((Column) i.next()).getName();
			sb.append("a." + colName);
			sb.append(",");
		}
		for (Iterator i = remainingCols.iterator(); i.hasNext();) {
			final String colName = ((Column) i.next()).getName();
			sb.append("b." + colName);
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName
				+ " a left join " + trgtSchemaName + "." + trgtTableName
				+ " b on ");
		for (int i = 0; i < action.getReduceTableFKColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = ((Column) action.getSourceTablePKColumns()
					.get(i)).getName();
			final String fkColName = ((Column) action.getReduceTableFKColumns()
					.get(i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName + "");
		}

		statements.add(sb.toString());
	}

	public void doRenameTable(final RenameTable action, final List statements)
			throws Exception {
		final String schemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String oldTableName = action.getTargetTableOldName();
		final String newTableName = action.getTargetTableName();
		statements.add("alter table " + schemaName + "." + oldTableName
				+ " rename to " + newTableName + "");
	}

	public void doUnion(final Union action, final List statements)
			throws Exception {
		final String schemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName()
				: ((JDBCSchema) action.getTargetTableSchema())
						.getDatabaseSchema();
		final String tableName = action.getTargetTableName();
		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + schemaName + "." + tableName
				+ " as select * from ");
		for (int i = 0; i < action.getSourceTableSchemas().size(); i++) {
			if (i > 0)
				sb.append(" union select * from ");
			final String targetSchemaName = action.getSourceTableSchemas().get(
					i) == null ? action.getDataSetSchemaName()
					: ((Schema) action.getSourceTableSchemas().get(i))
							.getName();
			final String targetTableName = (String) action
					.getSourceTableNames().get(i);
			sb.append(targetSchemaName);
			sb.append(".");
			sb.append(targetTableName);
		}
		statements.add(sb.toString());
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
				"select distinct " + colName + " from " + schemaName
						+ "." + tableName + "").executeQuery();
		while (rs.next())
			results.add(rs.getString(1));
		rs.close();
		return results;
	}

	public List executeSelectRows(final Table table, final int offset,
			final int count) throws SQLException {
		final String tableName = table.getName();
		final Schema schema = table.getSchema();

		final StringBuffer colNames = new StringBuffer();
		for (final Iterator i = table.getColumns().iterator(); i.hasNext();) {
			colNames.append(((Column) i.next()).getName());
			if (i.hasNext())
				colNames.append(',');
		}

		final List results = new ArrayList();
		final String schemaName = ((JDBCSchema) schema).getDatabaseSchema();
		final Connection conn = ((JDBCSchema) schema).getConnection();
		final ResultSet rs = conn.prepareStatement(
				"select * from (select rownum as __seqnum,"
						+ colNames.toString() + " from " + schemaName + "."
						+ tableName + ") where __seqnum > " + offset
						+ " and __seqnum <= " + (offset + count))
				.executeQuery();
		while (rs.next()) {
			final List values = new ArrayList();
			// Start at column 2 in order to skip __seqnum column.
			for (int i = 2; i <= rs.getMetaData().getColumnCount(); i++)
				values.add(rs.getObject(i));
			results.add(values);
		}
		rs.close();
		return results;
	}

	public String[] getStatementsForAction(final MartConstructorAction action,
			final boolean includeComments) throws ConstructorException {

		final List statements = new ArrayList();

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

	public void reset() {
		OracleDialect.GROUP_CONCAT_CREATED = false;
	}

	public boolean understandsDataLink(final DataLink dataLink) {

		// Convert to JDBC version.
		if (!(dataLink instanceof JDBCDataLink))
			return false;
		final JDBCDataLink jddl = (JDBCDataLink) dataLink;

		try {
			return jddl.getConnection().getMetaData().getDatabaseProductName()
					.equals("Oracle");
		} catch (final SQLException e) {
			throw new BioMartError(e);
		}
	}
}
