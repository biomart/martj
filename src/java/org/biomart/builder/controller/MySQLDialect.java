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
package org.biomart.builder.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.MartConstructor.CreateFK;
import org.biomart.builder.model.MartConstructor.CreatePK;
import org.biomart.builder.model.MartConstructor.CreateTable;
import org.biomart.builder.model.MartConstructor.DropTable;
import org.biomart.builder.model.MartConstructor.MCAction;
import org.biomart.builder.model.MartConstructor.MergeTable;
import org.biomart.builder.model.MartConstructor.RenameTable;
import org.biomart.builder.model.MartConstructor.RestrictTable;
import org.biomart.builder.model.MartConstructor.UnionTables;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Understands how to create SQL and DDL for a MySQL database.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 23rd June 2006
 * @since 0.1
 */
public class MySQLDialect extends DatabaseDialect {

	public boolean understandsDataLink(DataLink dataLink)
			throws ConstructorException {

		// Convert to JDBC version.
		if (!(dataLink instanceof JDBCDataLink))
			return false;
		JDBCDataLink jddl = (JDBCDataLink) dataLink;

		try {
			return jddl.getConnection().getMetaData().getDatabaseProductName()
					.equals("MySQL");
		} catch (SQLException e) {
			throw new ConstructorException(e);
		}
	}
	
	public void reset() {
		
	}

	public List executeSelectDistinct(Column col) throws SQLException {
		String colName = col.getName();
		String tableName = col.getTable().getName();
		Schema schema = col.getTable().getSchema();

		// The complex case - where we need to do a union of
		// select distincts.
		if (schema instanceof SchemaGroup) {
			Set results = new HashSet();
			for (Iterator i = ((SchemaGroup) schema).getSchemas().iterator(); i
					.hasNext();) {
				Schema member = (Schema) i.next();
				results.addAll(this.executeSelectDistinct(member
						.getTableByName(tableName).getColumnByName(colName)));
			}
			return new ArrayList(results);
		}

		// The simple case where we actually do a select distinct.
		List results = new ArrayList();
		String schemaName = ((JDBCSchema) schema).getDatabaseSchema();
		Connection conn = ((JDBCSchema) schema).getConnection();
		ResultSet rs = conn.prepareStatement(
				"select distinct " + colName + " from " + schemaName + "."
						+ tableName).executeQuery();
		while (rs.next())
			results.add(rs.getString(0));
		rs.close();
		return results;
	}

	public String[] getStatementsForAction(MCAction action,
			boolean includeComments) throws ConstructorException {
		
		// FIXME: This is not very nice code. When the graph nodes change
		// if/when the stuff in MartConstructor.GenericConstructorRunnable
		// is ever rewritten nicely, then this method and all the various
		// methods it calls will also have to be rewritten, but I hope 
		// in a much nicer way.
		
		try {
			List statements = new ArrayList();

			if (includeComments)
				statements.add("#" + action.getStatusMessage());

			if (action instanceof CreatePK)
				this.createPKStatements((CreatePK) action, statements);
			else if (action instanceof CreateFK)
				this.createFKStatements((CreateFK) action, statements);
			else if (action instanceof MergeTable)
				this.mergeTableStatements((MergeTable) action, statements);
			else if (action instanceof CreateTable)
				this.createTableStatements((CreateTable) action, statements);
			else if (action instanceof DropTable)
				this.dropTableStatements((DropTable) action, statements);
			else if (action instanceof RenameTable)
				this.renameTableStatements((RenameTable) action, statements);
			else if (action instanceof RestrictTable)
				this
						.restrictTableStatements((RestrictTable) action,
								statements);
			else if (action instanceof UnionTables)
				this.unionTablesStatements((UnionTables) action, statements);
			else
				throw new ConstructorException(BuilderBundle
						.getString("mcUnknownAction"));

			return (String[]) statements.toArray(new String[0]);
		} catch (Throwable t) {
			throw new ConstructorException(t);
		}
	}

	private void createPKStatements(CreatePK action, List statements)
			throws Exception {
		String schemaName = action.datasetSchemaName;
		String tableName = action.tableName;
		StringBuffer sb = new StringBuffer();
		for (Iterator i = action.dsColumns.iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			sb.append(colName);
			if (i.hasNext())
				sb.append(",");
		}
		statements.add("alter table " + schemaName + "." + tableName
				+ " add primary key (" + sb.toString() + ")");
	}

	private void createFKStatements(CreateFK action, List statements)
			throws Exception {
		String schemaName = action.datasetSchemaName;
		String fkTableName = action.tableName;
		String pkTableName = action.parentTableName;
		StringBuffer sbFK = new StringBuffer();
		for (Iterator i = action.dsColumns.iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			sbFK.append(colName);
			if (i.hasNext())
				sbFK.append(",");
		}
		StringBuffer sbPK = new StringBuffer();
		for (Iterator i = action.parentDSColumns.iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			sbPK.append(colName);
			if (i.hasNext())
				sbPK.append(",");
		}
		statements
				.add("alter table " + schemaName + "." + fkTableName
						+ " add foreign key (" + sbFK.toString()
						+ ") references " + schemaName + "." + pkTableName
						+ " (" + sbPK.toString() + ")");
	}

	private void dropTableStatements(DropTable action, List statements)
			throws Exception {
		String schemaName = action.datasetSchemaName;
		String tableName = action.tableName;
		statements.add("drop table " + schemaName + "." + tableName);
	}

	private void renameTableStatements(RenameTable action, List statements)
			throws Exception {
		String schemaName = action.datasetSchemaName;
		String oldTableName = action.oldName;
		String newTableName = action.newName;
		statements.add("rename table " + schemaName + "." + oldTableName
				+ " to " + schemaName + "." + newTableName);
	}

	private void createTableStatements(CreateTable action, List statements)
			throws Exception {
		this.processTableStatements(action, action.datasetSchemaName,
				((JDBCSchema) action.schema).getDatabaseSchema(),
				action.tableName, null, action.realTable.getName(),
				action.dsColumns, null, null, false, action.partitionColumn,
				action.partitionValue, statements);
	}

	private void mergeTableStatements(MergeTable action, List statements)
			throws Exception {
		this.processTableStatements(action, action.datasetSchemaName,
				((JDBCSchema) action.schema).getDatabaseSchema(),
				action.tempTable, action.tableName, action.realTable.getName(),
				action.dsColumns, action.fromDSColumns, action.toRealColumns,
				action.leftJoin, action.partitionColumn, action.partitionValue,
				statements);
	}

	private void restrictTableStatements(RestrictTable action, List statements)
			throws Exception {
		String schemaName = action.datasetSchemaName;
		String newTableName = action.newTableName;
		String childTableName = action.oldTableName;
		String parentTableName = action.parentTableName;

		// Create the necessary index on the child table.
		StringBuffer isb = new StringBuffer();
		for (Iterator i = action.childFKCols.iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			isb.append(colName);
			if (i.hasNext())
				isb.append(",");
		}
		statements.add("create index " + schemaName + "." + childTableName
				+ "_I on " + schemaName + "." + childTableName + "("
				+ isb.toString() + ")");

		// Restrict the table.
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < action.parentPKCols.size(); i++) {
			if (i > 0)
				sb.append(" and ");
			String parentColName = ((Column) action.parentPKCols.get(i))
					.getName();
			String childColName = ((Column) action.childFKCols.get(i))
					.getName();
			sb.append("a." + parentColName + "=b." + childColName);
		}
		statements.add("create table " + schemaName + "." + newTableName
				+ " as select b.* " + "from " + schemaName + "."
				+ parentTableName + " as a, " + schemaName + "."
				+ childTableName + " as b where " + sb.toString());
	}

	private void unionTablesStatements(UnionTables action, List statements)
			throws Exception {
		String schemaName = action.datasetSchemaName;
		String tableName = action.tableName;
		StringBuffer sb = new StringBuffer();
		sb.append("create table " + schemaName + "." + tableName
				+ " as select * from ");
		for (Iterator i = action.tableNames.iterator(); i.hasNext();) {
			sb.append((String) i.next());
			if (i.hasNext())
				sb.append(" union select * from ");
		}
		statements.add(sb.toString());
	}

	private void processTableStatements(MCAction action, String dsSchemaName,
			String targetSchemaName, String tempTableName,
			String existingTableName, String targetTableName, List dsColumns,
			List fromDSColumns, List toRealColumns, boolean leftJoin,
			DataSetColumn partitionColumn, Object partitionValue,
			List statements) throws Exception {

		// We must assume the source schema is read-only, therefore
		// we cannot make any temporary indexes on it even though
		// we'd probably quite like to.

		// Restrict to a particular partition.
		String sbWhere = "";
		if (partitionColumn != null) {
			if (partitionValue == null)
				sbWhere = "where b."
						+ ((WrappedColumn) partitionColumn).getWrappedColumn()
								.getName() + " is null";
			else
				sbWhere = "where b."
						+ ((WrappedColumn) partitionColumn).getWrappedColumn()
								.getName() + "='" + partitionValue + "'";
		}

		// Join the table to the parent, if parent table not null.
		StringBuffer sbJoin = new StringBuffer();
		if (existingTableName != null) {

			// Create an index on the existing table to use for the join.
			StringBuffer sbIndCols = new StringBuffer();
			for (Iterator i = fromDSColumns.iterator(); i.hasNext();) {
				sbIndCols.append(((Column) i.next()).getName());
				if (i.hasNext())
					sbIndCols.append(",");
			}
			statements.add("create index " + dsSchemaName + "."
					+ existingTableName + "_I on " + dsSchemaName + "."
					+ existingTableName + "(" + sbIndCols.toString() + ")");

			// Work out what kind of join to use, if any.
			String joinKW = (leftJoin && partitionColumn == null) ? "left"
					: "inner";

			// Do the join.
			sbJoin.append(dsSchemaName);
			sbJoin.append('.');
			sbJoin.append(existingTableName);
			sbJoin.append(" as a ");
			sbJoin.append(joinKW);
			sbJoin.append(" join ");
			sbJoin.append(targetSchemaName);
			sbJoin.append('.');
			sbJoin.append(targetTableName);
			sbJoin.append(" as b on (");
			for (int i = 0; i < fromDSColumns.size(); i++) {
				if (i > 0)
					sbJoin.append(" and ");
				String parentColName = ((Column) fromDSColumns.get(i))
						.getName();
				String childColName = ((Column) toRealColumns.get(i)).getName();
				sbJoin.append("a.");
				sbJoin.append(parentColName);
				sbJoin.append("=b.");
				sbJoin.append(childColName);
			}
			sbJoin.append(')');
		}
		// Otherwise, just select from target table.
		else {
			sbJoin.append(targetSchemaName);
			sbJoin.append('.');
			sbJoin.append(targetTableName);
			sbJoin.append(" as b");
		}

		// Make a list to contain temporary concat table names.
		List tempConcatTableNames = new ArrayList();

		// Work out what columns to include.
		StringBuffer sbCols = new StringBuffer();

		// Include all columns from the existing table, if present.
		if (existingTableName != null)
			sbCols.append("a.*,");

		// Set up the alias for the first concat table, if any.
		char nextAlias = 'c';

		// Add all the columns from the new table.
		for (Iterator i = dsColumns.iterator(); i.hasNext();) {
			DataSetColumn dsCol = (DataSetColumn) i.next();
			if (dsCol instanceof WrappedColumn) {
				WrappedColumn wc = (WrappedColumn) dsCol;
				sbCols.append("b.");
				sbCols.append(wc.getWrappedColumn().getName());
				sbCols.append(" as ");
				sbCols.append(wc.getName());
			} else if (dsCol instanceof SchemaNameColumn) {
				SchemaNameColumn sn = (SchemaNameColumn) dsCol;
				sbCols.append('\'');
				sbCols.append(targetSchemaName);
				sbCols.append("' as ");
				sbCols.append(sn.getName());
			} else if (dsCol instanceof ConcatRelationColumn) {
				ConcatRelationColumn crCol = (ConcatRelationColumn) dsCol;
				ConcatRelationType crType = ((DataSet) dsCol.getTable()
						.getSchema()).getConcatRelationType(crCol
						.getUnderlyingRelation());
				Key fromKey = crCol.getUnderlyingRelation().getOneKey();
				Key toKey = crCol.getUnderlyingRelation().getManyKey();
				Key concatPrimaryKey = toKey.getTable().getPrimaryKey();

				// Make a name for our temporary concat table.
				String tempConcTabName = tempTableName + "_C" + nextAlias;
				tempConcatTableNames.add(tempConcTabName);

				// Create a list of columns to include in the concat, using
				// nextAlias.toKey, and using ifNull on each to prevent
				// skipped columns (replace with '').
				StringBuffer sbConcatCols = new StringBuffer();
				for (Iterator j = concatPrimaryKey.getColumns().iterator(); j
						.hasNext();) {
					sbConcatCols.append("ifnull(");
					sbConcatCols.append(nextAlias);
					sbConcatCols.append('.');
					sbConcatCols.append(((Column) j.next()).getName());
					sbConcatCols.append(",'')");
					if (j.hasNext())
						sbConcatCols.append(',');
				}

				// 1. Create a list of columns to select for the concat, using
				// fromKey.
				// 2. Create a list of columns to join for the concat, using
				// fromKey and toKey.
				// 3. Create a list of columns to index in the concat, using
				// fromKey.
				// 4. Create a list of columns to link the concat to the
				// table we are constructing, using fromKey.
				StringBuffer sbConcatJoinCols = new StringBuffer();
				StringBuffer sbConcatFromCols = new StringBuffer();
				StringBuffer sbConcatIndCols = new StringBuffer();
				StringBuffer sbConcatLinkCols = new StringBuffer();
				for (int j = 0; j < fromKey.getColumns().size(); j++) {
					if (j > 0) {
						sbConcatJoinCols.append(',');
						sbConcatFromCols.append(',');
						sbConcatIndCols.append(',');
						sbConcatLinkCols.append(" and ");
					}
					// join cols
					sbConcatJoinCols.append("b.");
					sbConcatJoinCols.append(((Column) fromKey.getColumns().get(
							j)).getName());
					sbConcatJoinCols.append('=');
					sbConcatJoinCols.append(nextAlias);
					sbConcatJoinCols.append('.');
					sbConcatJoinCols
							.append(((Column) toKey.getColumns().get(j))
									.getName());
					// from cols
					sbConcatFromCols.append("b.");
					sbConcatFromCols.append(((Column) fromKey.getColumns().get(
							j)).getName());
					// index cols
					sbConcatIndCols.append(((Column) fromKey.getColumns()
							.get(j)).getName());
					// link cols
					sbConcatLinkCols.append("b.");
					sbConcatLinkCols.append(((Column) fromKey.getColumns().get(
							j)).getName());
					sbConcatLinkCols.append('=');
					sbConcatLinkCols.append(nextAlias);
					sbConcatLinkCols.append('.');
					sbConcatLinkCols.append(((Column) fromKey.getColumns().get(
							j)).getName());
				}

				// Work out what schema the other end of the concat relation
				// is in.
				// FIXME: what to do if target real schema is a group, but
				// not our group?
				Table realTable = toKey.getTable();
				String realSchemaName = 
					(realTable.getSchema() instanceof SchemaGroup) ? targetSchemaName : realTable.getSchema().getName();
				
				// Construct the temporary concat table.
				StringBuffer sbTempConc = new StringBuffer();
				sbTempConc.append("create table ");
				sbTempConc.append(dsSchemaName);
				sbTempConc.append('.');
				sbTempConc.append(tempConcTabName);
				sbTempConc.append(" as select ");
				sbTempConc.append(sbConcatFromCols.toString());
				sbTempConc.append(", ");
				sbTempConc.append("group_concat(distinct concat_ws('");
				sbTempConc.append(crType.getValueSeparator());
				sbTempConc.append("', ");
				sbTempConc.append(sbConcatCols.toString());
				sbTempConc.append(") separator '");
				sbTempConc.append(crType.getRecordSeparator());
				sbTempConc.append("'))");
				sbTempConc.append(" from ");
				sbTempConc.append(targetSchemaName);
				sbTempConc.append('.');
				sbTempConc.append(targetTableName);
				sbTempConc.append(" as b inner join ");
				sbTempConc.append(realSchemaName);
				sbTempConc.append('.');
				sbTempConc.append(toKey.getTable().getName());
				sbTempConc.append(" as ");
				sbTempConc.append(nextAlias);
				sbTempConc.append(" on (");
				sbTempConc.append(sbConcatJoinCols.toString());
				sbTempConc.append(") group by ");
				sbTempConc.append(sbConcatFromCols.toString());
				statements.add(sbTempConc.toString());

				// Create an index on b.fromKey in the temporary concat table.
				statements.add("create index " + dsSchemaName + "."
						+ tempConcTabName + "_I on " + dsSchemaName + "."
						+ tempConcTabName + "(" + sbConcatIndCols.toString()
						+ ")");

				// Add the 'concat' column from the temporary concat table to
				// the list of columns to create for table b.
				sbCols.append(nextAlias);
				sbCols.append(".xconc as ");
				sbCols.append(crCol.getName());

				// Add an inner join from b.fromKey to equivalent b.fromKey in
				// the temporary concat table.
				sbJoin.append("left join ");
				sbJoin.append(dsSchemaName);
				sbJoin.append('.');
				sbJoin.append(tempConcTabName);
				sbJoin.append(" as ");
				sbJoin.append(nextAlias);
				sbJoin.append(" on (");
				sbJoin.append(sbConcatLinkCols.toString());
				sbJoin.append(')');

				// Increment the alias for the next one, if any.
				nextAlias++;
			} else
				throw new ConstructorException(BuilderBundle
						.getString("mcUnknownDSCol"));
			if (i.hasNext())
				sbCols.append(',');
		}

		// Write the command.
		statements.add("create table " + dsSchemaName + "." + tempTableName
				+ " as select " + sbCols.toString() + " from "
				+ sbJoin.toString() + " " + sbWhere);

		// If it has a parent table...
		if (existingTableName != null) {
			// Drop the parent table.
			this.dropTableStatements(new DropTable(dsSchemaName,
					existingTableName), statements);

			// Rename the child table.
			this.renameTableStatements(new RenameTable(dsSchemaName,
					tempTableName, existingTableName), statements);
		}

		// Drop any temporary concat tables.
		for (Iterator i = tempConcatTableNames.iterator(); i.hasNext();)
			// Drop the parent table.
			this.dropTableStatements(new DropTable(dsSchemaName, (String) i
					.next()), statements);
	}
}
