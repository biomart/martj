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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.MartConstructorAction.AddExpression;
import org.biomart.builder.model.MartConstructorAction.ConcatJoin;
import org.biomart.builder.model.MartConstructorAction.CopyOptimiserDirect;
import org.biomart.builder.model.MartConstructorAction.CopyOptimiserVia;
import org.biomart.builder.model.MartConstructorAction.CreateOptimiser;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.DropColumns;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.Join;
import org.biomart.builder.model.MartConstructorAction.LeftJoin;
import org.biomart.builder.model.MartConstructorAction.Rename;
import org.biomart.builder.model.MartConstructorAction.Select;
import org.biomart.builder.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition.RecursionType;
import org.biomart.common.controller.JDBCSchema;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;

/**
 * Understands how to create SQL and DDL for a MySQL database.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class MySQLDialect extends DatabaseDialect {

	// Check we only set the group concat size once.
	private static boolean GROUP_CONCAT_SIZED;

	private int indexCount;

	public void doConcatJoin(final ConcatJoin action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getDataSetSchemaName();
		final String srcTableName = action.getLeftTable();
		final String trgtSchemaName = action.getRightSchema();
		final String trgtTableName = action.getRightTable();
		final String mergeTableName = action.getResultTable();
		final boolean isRecursive = action.getRecursionType() != RecursionType.NONE;
		final boolean isDoubleRecursive = action
				.getRecursionSecondFromColumns() != null;
		final String recursionTempTable = "MART_RECURSE";

		// If we haven't defined the group_concat size yet, define it.
		// Note limitation of total 18446744073709551616 characters for
		// group_concat result.
		if (!MySQLDialect.GROUP_CONCAT_SIZED) {
			statements.add("set group_concat_max_len=18446744073709551616");
			MySQLDialect.GROUP_CONCAT_SIZED = true;
		}
		
		final StringBuffer sb = new StringBuffer();

		if (isRecursive) {
			// Start block.
			statements.add("DELIMITER // ");
			sb.append("CREATE PROCEDURE () mart_recurse MODIFIES SQL DATA BEGIN DECLARE rows_updated INT DEFAULT 1; ");
			// Create intial temp table
			sb.append("create table " + action.getDataSetSchemaName() + "."
					+ recursionTempTable + " as select ");
			for (final Iterator i = action.getRightJoinColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append("a.");
				sb.append(entry);
				sb.append(",");
			}
			sb.append(action.getConcatColumnDefinition()
					.getSubstitutedExpression("a"));
			sb.append(" as ");
			sb.append(action.getConcatColumnName());
			sb.append(",1 as finalRow");
			for (final Iterator i = action.getRecursionFromColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append(",");
				sb.append("b.");
				sb.append(entry);
			}
			sb.append(" from " + srcSchemaName + "." + srcTableName
					+ " as x inner join ");
			sb.append(trgtSchemaName + "." + trgtTableName);
			sb.append(" as a on ");
			for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
				if (i > 0)
					sb.append(" and ");
				final String pkColName = (String) action.getLeftJoinColumns()
						.get(i);
				final String fkColName = (String) action.getRightJoinColumns()
						.get(i);
				sb.append("x." + pkColName + "=a." + fkColName);
			}
			if (action.getTableRestriction() != null) {
				sb.append(" and (");
				sb.append(action.getTableRestriction()
						.getSubstitutedExpression("a"));
				sb.append(')');
			}
			sb.append(" inner join ");
			if (isDoubleRecursive) {
				sb.append(trgtSchemaName + "." + action.getRecursionTable());
				sb.append(" as c on ");
				for (int i = 0; i < action.getRecursionFromColumns().size(); i++) {
					if (i > 0)
						sb.append(" and ");
					final String pkColName = (String) action
							.getRecursionFromColumns().get(i);
					final String fkColName = (String) action
							.getRecursionToColumns().get(i);
					sb.append("a." + pkColName + "=c." + fkColName);
				}
				sb.append(" inner join ");
				sb.append(trgtSchemaName + "." + trgtTableName);
				sb.append(" as b on ");
				for (int i = 0; i < action.getRecursionSecondFromColumns()
						.size(); i++) {
					if (i > 0)
						sb.append(" and ");
					final String pkColName = (String) action
							.getRecursionSecondFromColumns().get(i);
					final String fkColName = (String) action
							.getRecursionSecondToColumns().get(i);
					sb.append("c." + pkColName + "=b." + fkColName);
				}
			} else {
				sb.append(trgtSchemaName + "." + trgtTableName);
				sb.append(" as b on ");
				for (int i = 0; i < action.getRecursionFromColumns().size(); i++) {
					if (i > 0)
						sb.append(" and ");
					final String pkColName = (String) action
							.getRecursionFromColumns().get(i);
					final String fkColName = (String) action
							.getRecursionToColumns().get(i);
					sb.append("a." + pkColName + "=b." + fkColName);
				}
			}
			sb.append("; ");
			// Index rtJoinCols.
			sb.append("create index " + recursionTempTable + "_I_" + this.indexCount++
				+ " on " + action.getDataSetSchemaName() + "."
					+ recursionTempTable + "(");
			for (final Iterator i = action.getRightJoinColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append(entry);
				if (i.hasNext())
					sb.append(',');
			}
			sb.append("); ");
			// Index parentFromCols.
			sb.append("create index " + recursionTempTable + "_I_" + this.indexCount++
					+ " on " + action.getDataSetSchemaName() + "."
						+ recursionTempTable + "(");
			for (final Iterator i = action.getRecursionFromColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append(entry);
				if (i.hasNext())
					sb.append(',');
			}
			sb.append("); ");
			// Index finalRow.
			sb.append("create index " + recursionTempTable + "_I_" + this.indexCount++
					+ " on " + action.getDataSetSchemaName() + "."
						+ recursionTempTable + "(finalRow); ");
			// Loop
			sb.append("WHILE rows_updated > 1 DO ");
			// Insert into table with flag = 0.
			sb.append("insert into " + action.getDataSetSchemaName() + "."
					+ recursionTempTable + " select ");
			for (final Iterator i = action.getRightJoinColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append("x.");
				sb.append(entry);
				sb.append(",");
			}
			if (action.getRecursionType() == RecursionType.APPEND) {
				sb.append("concat(x.");
				sb.append(action.getConcatColumnName());
				sb.append(",'");
				sb.append(action.getConcatColumnDefinition().getConcSep()
						.replaceAll("'", "\\'"));
				sb.append("',");
				sb.append(action.getConcatColumnDefinition()
						.getSubstitutedExpression("a"));
				sb.append(')');
			} else {
				sb.append("concat(");
				sb.append(action.getConcatColumnDefinition()
						.getSubstitutedExpression("a"));
				sb.append(",'");
				sb.append(action.getConcatColumnDefinition().getConcSep()
						.replaceAll("'", "\\'"));
				sb.append("',");
				sb.append("x.");
				sb.append(action.getConcatColumnName());
				sb.append(')');
			}
			sb.append(" as ");
			sb.append(action.getConcatColumnName());
			sb.append(",0 as finalRow");
			for (final Iterator i = action.getRecursionFromColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append(",");
				sb.append("b.");
				sb.append(entry);
			}
			sb.append(" from " + action.getDataSetSchemaName() + "."
					+ recursionTempTable + " as x inner join ");
			sb.append(trgtSchemaName + "." + trgtTableName);
			sb.append(" as a on ");
			for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
				if (i > 0)
					sb.append(" and ");
				final String pkColName = (String) action.getLeftJoinColumns()
						.get(i);
				final String fkColName = (String) action.getRightJoinColumns()
						.get(i);
				sb.append("x." + pkColName + "=a." + fkColName);
			}
			if (action.getTableRestriction() != null) {
				sb.append(" and (");
				sb.append(action.getTableRestriction()
						.getSubstitutedExpression("a"));
				sb.append(')');
			}
			sb.append(" inner join ");
			if (isDoubleRecursive) {
				sb.append(trgtSchemaName + "." + action.getRecursionTable());
				sb.append(" as c on ");
				for (int i = 0; i < action.getRecursionFromColumns().size(); i++) {
					if (i > 0)
						sb.append(" and ");
					final String pkColName = (String) action
							.getRecursionFromColumns().get(i);
					final String fkColName = (String) action
							.getRecursionToColumns().get(i);
					sb.append("a." + pkColName + "=c." + fkColName);
				}
				sb.append(" inner join ");
				sb.append(trgtSchemaName + "." + trgtTableName);
				sb.append(" as b on ");
				for (int i = 0; i < action.getRecursionSecondFromColumns()
						.size(); i++) {
					if (i > 0)
						sb.append(" and ");
					final String pkColName = (String) action
							.getRecursionSecondFromColumns().get(i);
					final String fkColName = (String) action
							.getRecursionSecondToColumns().get(i);
					sb.append("c." + pkColName + "=b." + fkColName);
				}
			} else {
				sb.append(trgtSchemaName + "." + trgtTableName);
				sb.append(" as b on ");
				for (int i = 0; i < action.getRecursionFromColumns().size(); i++) {
					if (i > 0)
						sb.append(" and ");
					final String pkColName = (String) action
							.getRecursionFromColumns().get(i);
					final String fkColName = (String) action
							.getRecursionToColumns().get(i);
					sb.append("a." + pkColName + "=b." + fkColName);
				}
			}
			sb.append("; ");
			// Delete old rows where old row flag = 1 and parentFromCols
			// are not null.
			sb.append("delete from " + action.getDataSetSchemaName() + "."
					+ recursionTempTable + " where finalRow=1");
			for (final Iterator i = action.getRecursionFromColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append(" and ");
				sb.append(entry);
				sb.append(" is not null");
			}
			sb.append("; ");
			// Update rowsUpdated.
			sb.append("set rows_updated = ROW_COUNT(); ");
			// Update old row flag.
			sb
					.append("update " + action.getDataSetSchemaName() + "."
							+ recursionTempTable
							+ " set finalRow=1 where finalRow=0; ");
			// End loop when expanded all successfully.
			sb.append("END WHILE; ");
			// Finish up.
			sb.append(" END //");
			// Reset the statement buffer.
			statements.add(sb.toString());
			sb.setLength(0);
			statements.add("DELIMITER ; ");
			statements.add("CALL mart_recurse();");
			statements.add("DROP PROCEDURE mart_recurse;");
		}

		// Now do the grouping on the nicely recursed (or original if not recursed) table.
		sb.append("create table " + action.getDataSetSchemaName() + "."
				+ mergeTableName + " as select ");
		for (final Iterator i = action.getLeftJoinColumns().iterator(); i
				.hasNext();) {
			final String entry = (String) i.next();
			sb.append("a.");
			sb.append(entry);
			sb.append(",");
		}
		sb.append("group_concat(");
		if (isRecursive) {
			sb.append("b.");
			sb.append(action.getConcatColumnName());
		} else {
			sb.append(action.getConcatColumnDefinition()
					.getSubstitutedExpression("b"));
		}
		sb.append(" separator '");
		sb.append(action.getConcatColumnDefinition().getRowSep().replaceAll(
				"'", "\\'"));
		sb.append("') as ");
		sb.append(action.getConcatColumnName());
		sb.append(" from " + srcSchemaName + "." + srcTableName
				+ " as a inner join ");
		if (isRecursive)
			sb.append(action.getDataSetSchemaName() + "." + recursionTempTable);
		else
			sb.append(trgtSchemaName + "." + trgtTableName);
		sb.append(" as b on ");
		for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = (String) action.getLeftJoinColumns()
					.get(i);
			final String fkColName = (String) action.getRightJoinColumns()
					.get(i);
			sb.append("a." + pkColName + "=b." + fkColName + "");
		}
		if (!isRecursive && action.getRelationRestriction() != null) {
			sb.append(" and ");
			sb.append(action.getRelationRestriction().getSubstitutedExpression(
					action.isRelationRestrictionLeftIsFirst() ? "a" : "b",
					action.isRelationRestrictionLeftIsFirst() ? "b" : "a"));
		}
		if (!isRecursive && action.getTableRestriction() != null) {
			sb.append(" and (");
			sb.append(action.getTableRestriction()
					.getSubstitutedExpression("b"));
			sb.append(')');
		}
		sb.append(" group by ");
		for (final Iterator i = action.getLeftJoinColumns().iterator(); i
				.hasNext();) {
			final String entry = (String) i.next();
			sb.append("a.");
			sb.append(entry);
			if (i.hasNext())
				sb.append(",");
		}

		statements.add(sb.toString());
		
		if (isRecursive) {
			statements.add("drop table " + action.getDataSetSchemaName() + "."
					+ recursionTempTable);
		}
	}

	public void doRename(final Rename action, final List statements)
			throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String oldTableName = action.getFrom();
		final String newTableName = action.getTo();
		statements.add("rename table " + schemaName + "." + oldTableName
				+ " to " + schemaName + "." + newTableName + "");
	}

	public void doSelect(final Select action, final List statements) {
		final String createTableSchema = action.getDataSetSchemaName();
		final String createTableName = action.getResultTable();
		final String fromTableSchema = action.getSchema();
		final String fromTableName = action.getTable();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + createTableSchema + "." + createTableName
				+ " as select ");
		for (final Iterator i = action.getSelectColumns().entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append("a.");
			sb.append(entry.getKey());
			if (!entry.getKey().equals(entry.getValue())) {
				sb.append(" as ");
				sb.append(entry.getValue());
			}
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + fromTableSchema + "." + fromTableName + " as a");
		if (action.getTableRestriction() != null) {
			sb.append(" where ");
			sb.append(action.getTableRestriction()
					.getSubstitutedExpression("a"));
		}
		if (action.getPartitionColumn() != null) {
			if (action.getTableRestriction() != null)
				sb.append(" and ");
			else
				sb.append(" where ");
			if (action.getPartitionRangeDef() != null)
				sb.append(action.getPartitionRangeDef()
						.getSubstitutedExpression(action.getPartitionValue(),
								"a", action.getPartitionColumn()));
			else {
				sb.append("a.");
				sb.append(action.getPartitionColumn());
				if (action.getPartitionValue() == null)
					sb.append(" is null");
				else {
					sb.append("='");
					sb
							.append(action.getPartitionValue().replaceAll("'",
									"\\'"));
					sb.append('\'');
				}
			}
		}

		statements.add(sb.toString());
	}

	public void doAddExpression(final AddExpression action,
			final List statements) {
		final String createTableSchema = action.getDataSetSchemaName();
		final String createTableName = action.getResultTable();
		final String fromTableSchema = action.getDataSetSchemaName();
		final String fromTableName = action.getTable();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + createTableSchema + "." + createTableName
				+ " as select ");
		for (final Iterator i = action.getSelectColumns().iterator(); i
				.hasNext();) {
			final String entry = (String) i.next();
			sb.append(entry);
			sb.append(',');
		}
		for (final Iterator i = action.getExpressionColumns().entrySet()
				.iterator(); i.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append((String) entry.getValue());
			sb.append(" as ");
			sb.append((String) entry.getKey());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + fromTableSchema + "." + fromTableName);
		if (action.getGroupByColumns() != null) {
			sb.append(" group by ");
			for (final Iterator i = action.getGroupByColumns().iterator(); i
					.hasNext();) {
				final String entry = (String) i.next();
				sb.append(entry);
				if (i.hasNext())
					sb.append(',');
			}
		}
		statements.add(sb.toString());
	}

	public void doIndex(final Index action, final List statements) {
		final String schemaName = action.getDataSetSchemaName();
		final String tableName = action.getTable();
		final StringBuffer sb = new StringBuffer();

		sb.append("create index " + tableName + "_I_" + this.indexCount++
				+ " on " + schemaName + "." + tableName + "(");
		for (final Iterator i = action.getColumns().iterator(); i.hasNext();) {
			sb.append(i.next());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(")");

		statements.add(sb.toString());
	}

	public void doJoin(final Join action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getDataSetSchemaName();
		final String srcTableName = action.getLeftTable();
		final String trgtSchemaName = action.getRightSchema();
		final String trgtTableName = action.getRightTable();
		final String mergeTableName = action.getResultTable();

		final String joinType = (action.getPartitionColumn() != null 
				|| (action.getRelationRestriction()!=null && action.getRelationRestriction().isHard()) 
				|| (action.getTableRestriction()!=null && action.getTableRestriction().isHard())) ? "inner"
				: "left";

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + action.getDataSetSchemaName() + "."
				+ mergeTableName + " as select a.*");
		for (final Iterator i = action.getSelectColumns().entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append(",b.");
			sb.append(entry.getKey());
			if (!entry.getKey().equals(entry.getValue())) {
				sb.append(" as ");
				sb.append(entry.getValue());
			}
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName + " as a "
				+ joinType + " join " + trgtSchemaName + "." + trgtTableName
				+ " as b on ");
		for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = (String) action.getLeftJoinColumns()
					.get(i);
			final String fkColName = (String) action.getRightJoinColumns()
					.get(i);
			sb.append("a." + pkColName + "=b." + fkColName + "");
		}
		if (action.getRelationRestriction() != null) {
			sb.append(" and ");
			sb.append(action.getRelationRestriction().getSubstitutedExpression(
					action.isRelationRestrictionLeftIsFirst() ? "a" : "b",
					action.isRelationRestrictionLeftIsFirst() ? "b" : "a"));
		}
		if (action.getTableRestriction() != null) {
			sb.append(" and (");
			sb.append(action.getTableRestriction()
					.getSubstitutedExpression("b"));
			sb.append(')');
		}
		if (action.getPartitionColumn() != null) {
			sb.append(" and ");
			if (action.getPartitionRangeDef() != null)
				sb.append(action.getPartitionRangeDef()
						.getSubstitutedExpression(action.getPartitionValue(),
								"b", action.getPartitionColumn()));
			else {
				sb.append("b.");
				sb.append(action.getPartitionColumn());
				if (action.getPartitionValue() == null)
					sb.append(" is null");
				else {
					sb.append("='");
					sb
							.append(action.getPartitionValue().replaceAll("'",
									"\\'"));
					sb.append('\'');
				}
			}
		}

		statements.add(sb.toString());
	}

	public void doLeftJoin(final LeftJoin action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getDataSetSchemaName();
		final String srcTableName = action.getLeftTable();
		final String trgtSchemaName = action.getRightSchema();
		final String trgtTableName = action.getRightTable();
		final String mergeTableName = action.getResultTable();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + action.getDataSetSchemaName() + "."
				+ mergeTableName + " as select ");
		for (final Iterator i = action.getLeftSelectColumns().iterator(); i
				.hasNext();) {
			final String entry = (String) i.next();
			sb.append("a.");
			sb.append(entry);
			sb.append(',');
		}
		for (final Iterator i = action.getRightSelectColumns().iterator(); i
				.hasNext();) {
			final String entry = (String) i.next();
			sb.append("b.");
			sb.append(entry);
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName
				+ " as a left join " + trgtSchemaName + "." + trgtTableName
				+ " as b on ");
		for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = (String) action.getLeftJoinColumns()
					.get(i);
			final String fkColName = (String) action.getRightJoinColumns().get(
					i);
			sb.append("a." + pkColName + "=b." + fkColName + "");
		}

		statements.add(sb.toString());
	}

	public void doDropColumns(final DropColumns action, final List statements)
			throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String tableName = action.getTable();

		for (final Iterator i = action.getColumns().iterator(); i.hasNext();)
			statements.add("alter table " + schemaName + "." + tableName
					+ " drop column " + (String) i.next());
	}

	public void doDrop(final Drop action, final List statements)
			throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String tableName = action.getTable();

		statements.add("drop table " + schemaName + "." + tableName + "");
	}

	public void doCreateOptimiser(final CreateOptimiser action,
			final List statements) {
		final String schemaName = action.getDataSetSchemaName();
		final String sourceTableName = action.getDataSetTableName();
		final String optTableName = action.getOptTableName();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + schemaName + "." + optTableName
				+ " as select distinct ");
		for (final Iterator i = action.getKeyColumns().iterator(); i.hasNext();) {
			sb.append((String) i.next());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + schemaName + "." + sourceTableName);
		statements.add(sb.toString());
	}

	public void doUpdateOptimiser(final UpdateOptimiser action,
			final List statements) {
		final String schemaName = action.getDataSetSchemaName();
		final String sourceTableName = action.getSourceTableName();
		final String optTableName = action.getOptTableName();
		final String optColName = action.getOptColumnName();

		statements.add("alter table " + schemaName + "." + optTableName
				+ " add column (" + optColName + " integer default 0)");

		final String countStmt = action.isCountNotBool() ? "count(1)"
				: "case count(1) when 0 then 0 else 1 end";

		final StringBuffer sb = new StringBuffer();
		sb.append("update " + schemaName + "." + optTableName + " a set "
				+ optColName + "=(select " + countStmt + " from " + schemaName
				+ "." + sourceTableName + " b where ");
		for (final Iterator i = action.getKeyColumns().iterator(); i.hasNext();) {
			final String keyCol = (String) i.next();
			sb.append("a.");
			sb.append(keyCol);
			sb.append("=b.");
			sb.append(keyCol);
			sb.append(" and ");
		}
		sb.append("not (");
		for (final Iterator i = action.getNonNullColumns().iterator(); i
				.hasNext();) {
			sb.append("b.");
			sb.append((String) i.next());
			sb.append(" is null");
			if (i.hasNext())
				sb.append(" and ");
		}
		sb.append("))");
		statements.add(sb.toString());
	}

	public void doCopyOptimiserVia(final CopyOptimiserVia action,
			final List statements) {
		final String schemaName = action.getDataSetSchemaName();
		final String toOptTableName = action.getToOptTableName();
		final String fromOptTableName = action.getFromOptTableName();
		final String viaTableName = action.getViaTableName();
		final String fromOptColName = action.getFromOptColumnName();
		final String toOptColName = action.getToOptColumnName();

		statements.add("alter table " + schemaName + "." + toOptTableName
				+ " add column (" + toOptColName + " integer default 0)");

		final String function = action.isCountNotBool() ? "sum" : "max";

		final StringBuffer sb = new StringBuffer();
		sb.append("update " + schemaName + "." + toOptTableName + " a set "
				+ toOptColName + "=(select " + function + "(b."
				+ fromOptColName + ") from " + schemaName + "."
				+ fromOptTableName + " b inner join " + schemaName + "."
				+ viaTableName + " c on ");
		for (final Iterator i = action.getFromKeyColumns().iterator(); i
				.hasNext();) {
			final String keyCol = (String) i.next();
			sb.append("b.");
			sb.append(keyCol);
			sb.append("=c.");
			sb.append(keyCol);
			if (i.hasNext())
				sb.append(" and ");
		}
		sb.append(" where ");
		for (final Iterator i = action.getToKeyColumns().iterator(); i
				.hasNext();) {
			final String keyCol = (String) i.next();
			sb.append("a.");
			sb.append(keyCol);
			sb.append("=c.");
			sb.append(keyCol);
			if (i.hasNext())
				sb.append(" and ");
		}
		sb.append(')');
		statements.add(sb.toString());
	}

	public void doCopyOptimiserDirect(final CopyOptimiserDirect action,
			final List statements) {
		final String schemaName = action.getDataSetSchemaName();
		final String toOptTableName = action.getToOptTableName();
		final String fromOptTableName = action.getFromOptTableName();
		final String fromOptColName = action.getFromOptColumnName();
		final String toOptColName = action.getToOptColumnName();

		statements.add("alter table " + schemaName + "." + toOptTableName
				+ " add column (" + toOptColName + " integer default 0)");

		final String function = action.isCountNotBool() ? "sum" : "max";

		final StringBuffer sb = new StringBuffer();
		sb.append("update " + schemaName + "." + toOptTableName + " a set "
				+ toOptColName + "=(select " + function + "(b."
				+ fromOptColName + ") from " + schemaName + "."
				+ fromOptTableName + " b where ");
		for (final Iterator i = action.getToKeyColumns().iterator(); i
				.hasNext();) {
			final String keyCol = (String) i.next();
			sb.append("a.");
			sb.append(keyCol);
			sb.append("=b.");
			sb.append(keyCol);
			if (i.hasNext())
				sb.append(" and ");
		}
		sb.append(')');
		statements.add(sb.toString());
	}

	public Collection executeSelectDistinct(final String schemaName, final Column col)
			throws SQLException {
		final String colName = col.getName();
		final String tableName = col.getTable().getName();
		final Schema schema = col.getTable().getSchema();

		// The simple case where we actually do a select distinct.
		final Collection results = new ArrayList();
		final Connection conn = ((JDBCSchema) schema).getConnection();
		final ResultSet rs = conn.prepareStatement(
				"select distinct " + colName + " from " + schemaName + "."
						+ tableName).executeQuery();
		while (rs.next())
			results.add(rs.getString(1));
		rs.close();
		return results;
	}

	public Collection executeSelectRows(final Table table, final int offset,
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
		final Collection results = new ArrayList();
		final String schemaName = ((JDBCSchema) schema).getDatabaseSchema();
		final Connection conn = ((JDBCSchema) schema).getConnection();
		final ResultSet rs = conn.prepareStatement(
				"select " + colNames.toString() + " from " + schemaName + "."
						+ tableName + " limit " + count + " offset " + offset)
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

		if (includeComments)
			statements.add("# " + action.getStatusMessage());

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
		MySQLDialect.GROUP_CONCAT_SIZED = false;
	}

	public boolean understandsDataLink(final DataLink dataLink) {

		// Convert to JDBC version.
		if (!(dataLink instanceof JDBCDataLink))
			return false;
		final JDBCDataLink jddl = (JDBCDataLink) dataLink;

		try {
			return jddl.getConnection().getMetaData().getDatabaseProductName()
					.equals("MySQL");
		} catch (final SQLException e) {
			throw new BioMartError(e);
		}
	}
}
