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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.MartConstructorAction.AddExpression;
import org.biomart.builder.model.MartConstructorAction.CreateOptimiser;
import org.biomart.builder.model.MartConstructorAction.Distinct;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.DropColumns;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.Join;
import org.biomart.builder.model.MartConstructorAction.LeftJoin;
import org.biomart.builder.model.MartConstructorAction.Rename;
import org.biomart.builder.model.MartConstructorAction.Select;
import org.biomart.builder.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.DataLink;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.model.DataLink.JDBCDataLink;

/**
 * Understands how to create SQL and DDL for an Oracle database.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class OracleDialect extends DatabaseDialect {

	private int indexCount;

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doRename(final Rename action, final List statements)
			throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String oldTableName = action.getFrom();
		final String newTableName = action.getTo();

		this.checkTableName(newTableName);

		statements.add("alter table " + schemaName + "." + oldTableName
				+ " rename to " + newTableName + "");
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doSelect(final Select action, final List statements)
			throws Exception {
		final String createTableSchema = action.getDataSetSchemaName();
		final String createTableName = action.getResultTable();
		final String fromTableSchema = action.getSchema();
		final String fromTableName = action.getTable();

		// Work out additional tables to include in this.
		char additionalTable = 'f';
		final Map additionalRels = new HashMap();
		if (action.getTableRestriction() != null)
			for (final Iterator i = action.getTableRestriction()
					.getAdditionalRelations().iterator(); i.hasNext();)
				additionalRels.put((Relation) i.next(), "" + additionalTable++);

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + createTableSchema + "." + createTableName
				+ " as select ");
		for (final Iterator i = action.getSelectColumns().entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append("a.");
			sb.append(entry.getKey());
			if (!entry.getKey().equals(entry.getValue())) {
				this.checkColumnName((String) entry.getValue());
				sb.append(" as ");
				sb.append(entry.getValue());
			}
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + fromTableSchema + "." + fromTableName + " a");
		for (final Iterator k = additionalRels.entrySet().iterator(); k
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) k.next();
			final Relation rel = (Relation) entry.getKey();
			final Table tbl = (Table) rel.getOneKey().getTable();
			sb.append(" inner join ");
			sb.append(fromTableSchema);
			sb.append(".");
			sb.append(tbl.getName());
			sb.append(' ');
			sb.append((String) entry.getValue());
			sb.append(" on ");
			final List aCols = rel.getManyKey().getColumns();
			final List joinCols = rel.getOneKey().getColumns();
			for (int i = 0; i < aCols.size(); i++) {
				if (i > 0)
					sb.append(" and ");
				sb.append("a.");
				sb.append(((Column) aCols.get(i)).getName());
				sb.append('=');
				sb.append((String) entry.getValue());
				sb.append('.');
				sb.append(((Column) joinCols.get(i)).getName());
			}
		}
		if (action.getTableRestriction() != null) {
			sb.append(" where ");
			sb.append(action.getTableRestriction().getSubstitutedExpression(
					additionalRels, "a"));
		}

		statements.add(sb.toString());
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doDistinct(final Distinct action, final List statements)
			throws Exception {
		final String createTableSchema = action.getDataSetSchemaName();
		final String createTableName = action.getResultTable();
		final String fromTableSchema = action.getSchema();
		final String fromTableName = action.getTable();

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + createTableSchema + "." + createTableName
				+ " as select distinct * from " + fromTableSchema + "."
				+ fromTableName);

		statements.add(sb.toString());
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doAddExpression(final AddExpression action,
			final List statements) throws Exception {
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
			if (i.hasNext())
				sb.append(',');
		}
		for (final Iterator i = action.getExpressionColumns().entrySet()
				.iterator(); i.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append(',');
			this.checkColumnName((String) entry.getKey());
			sb.append((String) entry.getValue());
			sb.append(" as ");
			sb.append((String) entry.getKey());
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

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doIndex(final Index action, final List statements)
			throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String tableName = action.getTable();
		final StringBuffer sb = new StringBuffer();

		sb.append("create index I_" + this.indexCount++ + " on " + schemaName
				+ "." + tableName + "(");
		for (final Iterator i = action.getColumns().iterator(); i.hasNext();) {
			sb.append(i.next());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(")");

		statements.add(sb.toString());
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doJoin(final Join action, final List statements)
			throws Exception {
		final String srcSchemaName = action.getDataSetSchemaName();
		final String srcTableName = action.getLeftTable();
		final String trgtSchemaName = action.getRightSchema();
		final String trgtTableName = action.getRightTable();
		final String mergeTableName = action.getResultTable();

		// Work out additional tables to include in this.
		char additionalTable = 'f';
		final Map additionalRels = new HashMap();
		if (action.getTableRestriction() != null)
			for (final Iterator i = action.getTableRestriction()
					.getAdditionalRelations().iterator(); i.hasNext();)
				additionalRels.put((Relation) i.next(), "" + additionalTable++);

		final String joinType = action.getRelationRestriction() != null
				&& action.getRelationRestriction().isHard()
				|| action.getTableRestriction() != null
				&& action.getTableRestriction().isHard() ? "inner" : "left";

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + action.getDataSetSchemaName() + "."
				+ mergeTableName + " as select a.*");
		for (final Iterator i = action.getSelectColumns().entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			sb.append(",b.");
			sb.append(entry.getKey());
			if (!entry.getKey().equals(entry.getValue())) {
				this.checkColumnName((String) entry.getValue());
				sb.append(" as ");
				sb.append(entry.getValue());
			}
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName + " a "
				+ joinType + " join " + trgtSchemaName + "." + trgtTableName
				+ " b on ");
		for (int i = 0; i < action.getLeftJoinColumns().size(); i++) {
			if (i > 0)
				sb.append(" and ");
			final String pkColName = (String) action.getLeftJoinColumns()
					.get(i);
			final String fkColName = (String) action.getRightJoinColumns().get(
					i);
			sb.append("a." + pkColName + "=b." + fkColName + "");
		}
		if (action.getRelationRestriction() != null) {
			sb.append(" and ");
			sb.append(action.getRelationRestriction().getSubstitutedExpression(
					action.isRelationRestrictionLeftIsFirst() ? "a" : "b",
					action.isRelationRestrictionLeftIsFirst() ? "b" : "a",
					action.isRelationRestrictionLeftIsFirst(),
					action.getRelationRestrictionPreviousUnit()));
		}
		if (action.getTableRestriction() != null && additionalRels.isEmpty()) {
			sb.append(" and (");
			sb.append(action.getTableRestriction().getSubstitutedExpression(
					additionalRels, "b"));
			sb.append(')');
		}
		for (final Iterator k = additionalRels.entrySet().iterator(); k
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) k.next();
			final Relation rel = (Relation) entry.getKey();
			final Table tbl = (Table) rel.getOneKey().getTable();
			sb.append(" " + joinType + " join ");
			sb.append(trgtSchemaName);
			sb.append(".");
			sb.append(tbl.getName());
			sb.append(' ');
			sb.append((String) entry.getValue());
			sb.append(" on ");
			final List aCols = rel.getManyKey().getColumns();
			final List joinCols = rel.getOneKey().getColumns();
			for (int i = 0; i < aCols.size(); i++) {
				if (i > 0)
					sb.append(" and ");
				sb.append("b.");
				sb.append(((Column) aCols.get(i)).getName());
				sb.append('=');
				sb.append((String) entry.getValue());
				sb.append('.');
				sb.append(((Column) joinCols.get(i)).getName());
			}
		}
		if (action.getTableRestriction() != null && !additionalRels.isEmpty()) {
			sb.append(" where ");
			sb.append(action.getTableRestriction().getSubstitutedExpression(
					additionalRels, "b"));
		}

		statements.add(sb.toString());
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
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
				+ " a left join " + trgtSchemaName + "." + trgtTableName
				+ " b on ");
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

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doDropColumns(final DropColumns action, final List statements)
			throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String tableName = action.getTable();

		statements.add("set search_path=" + schemaName + ",pg_catalog");

		for (final Iterator i = action.getColumns().iterator(); i.hasNext();)
			statements.add("alter table " + schemaName + "." + tableName
					+ " set unused (" + (String) i.next() + ")");
		statements.add("alter table " + schemaName + "." + tableName
				+ " drop unused columns");
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doDrop(final Drop action, final List statements)
			throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String tableName = action.getTable();

		statements.add("drop table " + schemaName + "." + tableName + "");
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doCreateOptimiser(final CreateOptimiser action,
			final List statements) throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String sourceTableName = action.getDataSetTableName();
		final String optTableName = action.getOptTableName();

		this.checkTableName(optTableName);

		final StringBuffer sb = new StringBuffer();
		sb.append("create table " + schemaName + "." + optTableName
				+ " as select distinct ");
		for (final Iterator i = action.getKeyColumns().iterator(); i.hasNext();) {
			sb.append("a.");
			sb.append((String) i.next());
			if (i.hasNext())
				sb.append(',');
		}
		if (action.getCopyTable() != null)
			sb.append(",b.*");
		sb.append(" from " + schemaName + "." + sourceTableName + " a");
		if (action.getCopyTable() != null) {
			sb.append(" inner join " + schemaName + "." + action.getCopyTable()
					+ " b on ");
			for (final Iterator i = action.getCopyKey().iterator(); i.hasNext();) {
				final String col = (String) i.next();
				sb.append("a." + col + "=b." + col);
				if (i.hasNext())
					sb.append(" and ");
			}
		}
		statements.add(sb.toString());
		if (action.getCopyTable() != null) {
			for (final Iterator i = action.getCopyKey().iterator(); i.hasNext();)
				statements.add("alter table " + schemaName + "." + optTableName
						+ " set unused (" + (String) i.next() + ")");
			statements.add("alter table " + schemaName + "." + optTableName
					+ " drop unused columns");
		}
	}

	/**
	 * Performs an action.
	 * 
	 * @param action
	 *            the action to perform.
	 * @param statements
	 *            the list into which statements will be added.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public void doUpdateOptimiser(final UpdateOptimiser action,
			final List statements) throws Exception {
		final String schemaName = action.getDataSetSchemaName();
		final String sourceTableName = action.getSourceTableName();
		final String optTableName = action.getOptTableName();
		final String optColName = action.getOptColumnName();

		this.checkColumnName(optColName);

		statements.add("alter table " + schemaName + "." + optTableName
				+ " add (" + optColName + " number default 0)");

		final String countStmt = action.isCountNotBool() ? "count(1)"
				: "decode(count(1),0,"
						+ (action.isNullNotZero() ? "null" : "0") + ",1)";

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
		sb.append("not(");
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

	public String[] getStatementsForAction(final MartConstructorAction action)
			throws ConstructorException {

		final List statements = new ArrayList();

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
		this.indexCount = 0;
	}

	public boolean understandsDataLink(final DataLink dataLink) {
		// Convert to JDBC version.
		if (!(dataLink instanceof JDBCDataLink))
			return false;
		final JDBCDataLink jddl = (JDBCDataLink) dataLink;

		try {
			return jddl.getConnection(null).getMetaData()
					.getDatabaseProductName().equals("Oracle");
		} catch (final SQLException e) {
			throw new BioMartError(e);
		}
	}
}
