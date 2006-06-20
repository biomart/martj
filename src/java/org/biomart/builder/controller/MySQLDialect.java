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
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.DataLink.JDBCDataLink;
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
 * @version 0.1.2, 19th June 2006
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
		String schemaName = schema.getName();
		Connection conn = ((JDBCSchema) schema).getConnection();
		ResultSet rs = conn.prepareStatement(
				"select distinct " + colName + " from " + schemaName + "."
						+ tableName).executeQuery();
		while (rs.next())
			results.add(rs.getString(0));
		rs.close();
		return results;
	}

	public String[] getStatementsForAction(MCAction action)
			throws ConstructorException {
		try {
			if (action instanceof CreatePK)
				return this.createPKStatements((CreatePK) action);
			else if (action instanceof CreateFK)
				return this.createFKStatements((CreateFK) action);
			else if (action instanceof MergeTable)
				return this.mergeTableStatements((MergeTable) action);
			else if (action instanceof CreateTable)
				return this.createTableStatements((CreateTable) action);
			else if (action instanceof DropTable)
				return this.dropTableStatements((DropTable) action);
			else if (action instanceof RenameTable)
				return this.renameTableStatements((RenameTable) action);
			else if (action instanceof RestrictTable)
				return this.restrictTableStatements((RestrictTable) action);
			else if (action instanceof UnionTables)
				return this.unionTablesStatements((UnionTables) action);
		} catch (Throwable t) {
			throw new ConstructorException(t);
		}

		throw new ConstructorException(BuilderBundle
				.getString("mcUnknownAction"));
	}

	private String[] createPKStatements(CreatePK action) throws Exception {
		String schemaName = ((JDBCDataLink) action.dataLink).getConnection()
				.getCatalog();
		String tableName = action.tableName;
		StringBuffer sb = new StringBuffer();
		for (Iterator i = action.dsColumns.iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			sb.append(colName);
			if (i.hasNext())
				sb.append(",");
		}
		return new String[] {
				"#" + action.getStatusMessage(),
				"alter table " + schemaName + "." + tableName
						+ " add primary key (" + sb.toString() + ")" };
	}

	private String[] createFKStatements(CreateFK action) throws Exception {
		String schemaName = ((JDBCDataLink) action.dataLink).getConnection()
				.getCatalog();
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
		return new String[] {
				"#" + action.getStatusMessage(),
				"alter table " + schemaName + "." + fkTableName
						+ " add foreign key (" + sbFK.toString()
						+ ") references " + schemaName + "." + pkTableName
						+ " (" + sbPK.toString() + ")" };
	}

	private String[] dropTableStatements(DropTable action) throws Exception {
		String schemaName = ((JDBCDataLink) action.dataLink).getConnection()
				.getCatalog();
		String tableName = action.tableName;
		return new String[] { "#" + action.getStatusMessage(),
				"drop table " + schemaName + "." + tableName };
	}

	private String[] renameTableStatements(RenameTable action) throws Exception {
		String schemaName = ((JDBCDataLink) action.dataLink).getConnection()
				.getCatalog();
		String oldTableName = action.oldName;
		String newTableName = action.newName;
		return new String[] {
				"#" + action.getStatusMessage(),
				"rename table " + schemaName + "." + oldTableName + " to "
						+ schemaName + "." + newTableName };
	}

	private String[] createTableStatements(CreateTable action) throws Exception {
		return processTableStatements(action, ((JDBCDataLink) action.dataLink)
				.getConnection().getCatalog(), ((JDBCSchema) action.schema)
				.getConnection().getCatalog(), action.tableName, null,
				action.realTable.getName(), action.dsColumns, null, null,
				false, action.partitionColumn, action.partitionValue);
	}

	private String[] mergeTableStatements(MergeTable action) throws Exception {
		return this.processTableStatements(action,
				((JDBCDataLink) action.dataLink).getConnection().getCatalog(),
				((JDBCSchema) action.schema).getConnection().getCatalog(),
				action.tempTable, action.tableName, action.realTable.getName(),
				action.dsColumns, action.fromDSColumns, action.toRealColumns,
				action.leftJoin, action.partitionColumn, action.partitionValue);
	}

	private String[] restrictTableStatements(RestrictTable action)
			throws Exception {
		List commands = new ArrayList();
		String schemaName = ((JDBCDataLink) action.dataLink).getConnection()
				.getCatalog();
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
		commands.add("#" + action.getStatusMessage());
		commands.add("create index " + childTableName + "_I on " + schemaName
				+ "." + childTableName + "(" + isb.toString() + ")");

		// Restrict the table.
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < action.parentPKCols.size(); i++) {
			if (i > 0)
				sb.append(" and ");
			String parentColName = ((Column) action.parentPKCols.get(i))
					.getName();
			String childColName = ((Column) action.childFKCols.get(i))
					.getName();
			sb.append("a." + parentColName + " = b." + childColName);
		}
		commands.add("create table " + schemaName + "." + newTableName
				+ " as select b.* " + "from " + schemaName + "."
				+ parentTableName + " as a, " + schemaName + "."
				+ childTableName + " as b where " + sb.toString());

		// Return.
		return (String[]) commands.toArray(new String[0]);
	}

	private String[] unionTablesStatements(UnionTables action) throws Exception {
		String schemaName = ((JDBCDataLink) action.dataLink).getConnection()
				.getCatalog();
		String tableName = action.tableName;
		StringBuffer sb = new StringBuffer();
		sb.append("create table " + schemaName + "." + tableName
				+ " as select * from ");
		for (Iterator i = action.tableNames.iterator(); i.hasNext();) {
			sb.append((String) i.next());
			if (i.hasNext())
				sb.append(" union select * from ");
		}
		return new String[] { "#" + action.getStatusMessage(), sb.toString() };
	}

	private String[] processTableStatements(MCAction action,
			String dsSchemaName, String targetSchemaName, String tempTableName,
			String existingTableName, String targetTableName, List dsColumns,
			List fromDSColumns, List toRealColumns, boolean leftJoin,
			DataSetColumn partitionColumn, Object partitionValue)
			throws Exception {

		List commands = new ArrayList();

		// We must assume the source schema is read-only, therefore
		// we cannot make any temporary indexes on it even though
		// we'd probably quite like to.

		// Join the table to the parent, if parent table not null.
		StringBuffer sbJoin = new StringBuffer();
		if (existingTableName != null) {

			// Work out what kind of join to use, if any.
			String joinKW = (leftJoin && partitionColumn == null) ? "left"
					: "inner";

			// Do the join.
			sbJoin.append(dsSchemaName);
			sbJoin.append(".");
			sbJoin.append(existingTableName);
			sbJoin.append(" ");
			sbJoin.append(joinKW);
			sbJoin.append(" as a join ");
			sbJoin.append(targetSchemaName);
			sbJoin.append(".");
			sbJoin.append(targetTableName);
			sbJoin.append(" as b on (");
			for (int i = 0; i < fromDSColumns.size(); i++) {
				if (i > 0)
					sbJoin.append(" and ");
				String parentColName = ((Column) fromDSColumns.get(i))
						.getName();
				String childColName = ((Column) toRealColumns.get(i)).getName();
				sbJoin.append("a." + parentColName + " = b." + childColName);
			}
			sbJoin.append(")");
		}
		// Otherwise, just select from target table.
		else {
			sbJoin.append(targetSchemaName);
			sbJoin.append(".");
			sbJoin.append(targetTableName);
			sbJoin.append(" as b ");
		}

		// Work out what columns to include.
		StringBuffer sbCols = new StringBuffer();
		if (existingTableName != null)
			sbCols.append("a.*,");
		for (Iterator i = dsColumns.iterator(); i.hasNext();) {
			DataSetColumn dsCol = (DataSetColumn) i.next();
			if (dsCol instanceof WrappedColumn) {
				WrappedColumn wc = (WrappedColumn) dsCol;
				sbCols.append("b." + wc.getWrappedColumn().getName() + " as "
						+ wc.getName());
			} else if (dsCol instanceof SchemaNameColumn) {
				SchemaNameColumn sn = (SchemaNameColumn) dsCol;
				sbCols.append("'" + targetSchemaName + "' as " + sn.getName());
			} else if (dsCol instanceof ConcatRelationColumn) {
				ConcatRelationColumn cr = (ConcatRelationColumn) dsCol;
				// TODO
				sbCols.append("'concat' as " + cr.getName());
			} else
				throw new ConstructorException(BuilderBundle
						.getString("mcUnknownDSCol"));
			if (i.hasNext())
				sbCols.append(",");
		}

		// TODO : concat-relations. Do these by group-by all the columns
		// on the original table, then adding an additional LEFT/INNER join
		// per concat relation, and adding a GROUP_CONCAT(DISTINCT CONCAT_WS(
		// valsep, ifnull(col1,''), ifnull(col2,''), ..., ifnull(colN,''))
		// SEPARATOR rowsep) column as the concat column for that relation.
		// The cols selected are the PK cols of the remote table. The join
		// cols are obvious.
		// Watch out for the MySQL property 'group_concat_max_len'. Also
		// watch out for null being replaced with empty string in CONCAT_WS -
		// alternative is to leave as null, then it will be skipped altogether
		// and output will be missing one entire value including separator.

		// Restrict to a particular partition.
		StringBuffer sbWhere = new StringBuffer();
		if (partitionColumn != null) {
			if (partitionValue == null)
				sbWhere.append(" where b."
						+ ((WrappedColumn) partitionColumn).getWrappedColumn()
								.getName() + " is null");
			else
				sbWhere.append(" where b."
						+ ((WrappedColumn) partitionColumn).getWrappedColumn()
								.getName() + " = '" + partitionValue + "'");
		}

		// Write the command.
		commands.add("#" + action.getStatusMessage());
		commands.add("create table " + dsSchemaName + "." + tempTableName
				+ " as select " + sbCols.toString() + " from "
				+ sbJoin.toString() + sbWhere.toString());

		// If it has a parent table...
		if (existingTableName != null) {
			// Drop the parent table.
			String[] dropBits = this.dropTableStatements(new DropTable(
					action.dataLink, existingTableName));
			for (int i = 0; i < dropBits.length; i++)
				commands.add(dropBits[i]);

			// Rename the child table.
			String[] renameBits = this.renameTableStatements(new RenameTable(
					action.dataLink, tempTableName, existingTableName));
			for (int i = 0; i < renameBits.length; i++)
				commands.add(renameBits[i]);
		}

		// Return.
		return (String[]) commands.toArray(new String[0]);
	}
}
