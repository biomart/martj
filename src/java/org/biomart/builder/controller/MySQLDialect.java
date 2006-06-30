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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.MartConstructorAction.Concat;
import org.biomart.builder.model.MartConstructorAction.Create;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.FK;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.Merge;
import org.biomart.builder.model.MartConstructorAction.OptimiseAddColumn;
import org.biomart.builder.model.MartConstructorAction.OptimiseUpdateColumn;
import org.biomart.builder.model.MartConstructorAction.PK;
import org.biomart.builder.model.MartConstructorAction.PlaceHolder;
import org.biomart.builder.model.MartConstructorAction.Reduce;
import org.biomart.builder.model.MartConstructorAction.Rename;
import org.biomart.builder.model.MartConstructorAction.Union;

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

	public String[] getStatementsForAction(MartConstructorAction action,
			boolean includeComments) throws ConstructorException {

		List statements = new ArrayList();

		if (includeComments)
			statements.add("#" + action.getStatusMessage());

		try {
			String className = action.getClass().getName();
			String methodName = "do"
					+ className.substring(className.lastIndexOf('$') + 1);
			Method method = this.getClass().getMethod(methodName,
					new Class[] { action.getClass(), List.class });
			method.invoke(this, new Object[] { action, statements });
		} catch (InvocationTargetException ite) {
			Throwable t = ite.getCause();
			if (t instanceof ConstructorException)
				throw (ConstructorException) t;
			else
				throw new ConstructorException(t);
		} catch (Throwable t) {
			if (t instanceof ConstructorException)
				throw (ConstructorException) t;
			else
				throw new ConstructorException(t);
		}

		return (String[]) statements.toArray(new String[0]);
	}

	public void doPK(PK action, List statements) throws Exception {
		String schemaName = action.getPkTableSchema() == null ? action
				.getDataSetSchemaName() : action.getPkTableSchema().getName();
		String tableName = action.getPkTableName();
		StringBuffer sb = new StringBuffer();
		for (Iterator i = action.getPkColumns().iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			sb.append(colName);
			if (i.hasNext())
				sb.append(",");
		}
		statements.add("alter table " + schemaName + "." + tableName
				+ " add primary key (" + sb.toString() + ")");
	}

	public void doFK(FK action, List statements) throws Exception {
		String pkSchemaName = action.getPkTableSchema() == null ? action
				.getDataSetSchemaName() : action.getPkTableSchema().getName();
		String pkTableName = action.getPkTableName();
		String fkSchemaName = action.getFkTableSchema() == null ? action
				.getDataSetSchemaName() : action.getFkTableSchema().getName();
		String fkTableName = action.getFkTableName();

		StringBuffer sbFK = new StringBuffer();
		for (Iterator i = action.getFkColumns().iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			sbFK.append(colName);
			if (i.hasNext())
				sbFK.append(",");
		}

		StringBuffer sbPK = new StringBuffer();
		for (Iterator i = action.getPkColumns().iterator(); i.hasNext();) {
			String colName = ((Column) i.next()).getName();
			sbPK.append(colName);
			if (i.hasNext())
				sbPK.append(",");
		}

		statements.add("alter table " + fkSchemaName + "." + fkTableName
				+ " add foreign key (" + sbFK.toString() + ") references "
				+ pkSchemaName + "." + pkTableName + " (" + sbPK.toString()
				+ ")");
	}

	public void doReduce(Reduce action, List statements) throws Exception {
		String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName() : action.getSourceTableSchema()
				.getName();
		String srcTableName = action.getSourceTableName();
		String trgtSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName() : action.getTargetTableSchema()
				.getName();
		String trgtTableName = action.getTargetTableName();
		String reduceSchemaName = action.getReducedTableSchema() == null ? action
				.getDataSetSchemaName()
				: action.getReducedTableSchema().getName();
		String reduceTableName = action.getReducedTableName();

		StringBuffer sb = new StringBuffer();
		sb.append("create table " + reduceSchemaName + "." + reduceTableName
				+ " as select b.* from " + srcSchemaName + "." + srcTableName
				+ " as a inner join " + trgtSchemaName + "." + trgtTableName
				+ " as b using ");
		for (int i = 0; i < action.getTargetTableKeyColumns().size(); i++) {
			if (i > 0)
				sb.append(',');
			String pkColName = ((Column) action.getSourceTableKeyColumns().get(
					i)).getName();
			String fkColName = ((Column) action.getTargetTableKeyColumns().get(
					i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName);
		}

		statements.add(sb.toString());
	}

	public void doDrop(Drop action, List statements) throws Exception {
		String schemaName = action.getDropTableSchema() == null ? action
				.getDataSetSchemaName() : action.getDropTableSchema().getName();
		String tableName = action.getDropTableName();
		statements.add("drop table " + schemaName + "." + tableName);
	}

	public void doRename(Rename action, List statements) throws Exception {
		String schemaName = action.getRenameTableSchema() == null ? action
				.getDataSetSchemaName() : action.getRenameTableSchema()
				.getName();
		String oldTableName = action.getRenameTableOldName();
		String newTableName = action.getRenameTableNewName();
		statements.add("rename table " + schemaName + "." + oldTableName
				+ " to " + schemaName + "." + newTableName);
	}

	public void doUnion(Union action, List statements) throws Exception {
		String schemaName = action.getUnionTableSchema() == null ? action
				.getDataSetSchemaName() : action.getUnionTableSchema()
				.getName();
		String tableName = action.getUnionTableName();
		StringBuffer sb = new StringBuffer();
		sb.append("create table " + schemaName + "." + tableName
				+ " as select * from ");
		for (int i = 0; i < action.getTargetTableSchemas().size(); i++) {
			if (i > 0)
				sb.append(" union select * from ");
			String targetSchemaName = action.getTargetTableSchemas().get(i) == null ? action
					.getDataSetSchemaName()
					: ((Schema) action.getTargetTableSchemas().get(i))
							.getName();
			String targetTableName = (String) action.getTargetTableNames().get(
					i);
			sb.append(targetSchemaName);
			sb.append('.');
			sb.append(targetTableName);
		}
		statements.add(sb.toString());
	}

	public void doPlaceHolder(PlaceHolder action, List statements) {
		statements.add("#");
	}

	public void doIndex(Index action, List statements) {
		String schemaName = action.getIndexTableSchema() == null ? action
				.getDataSetSchemaName() : action.getIndexTableSchema()
				.getName();
		String tableName = action.getIndexTableName();
		StringBuffer sb = new StringBuffer();

		sb.append("create index " + schemaName + "." + tableName + "_I on "
				+ schemaName + "." + tableName + "(");
		for (Iterator i = action.getIndexColumns().iterator(); i.hasNext();) {
			Column col = (Column) i.next();
			sb.append(col.getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(")");

		statements.add(sb.toString());
	}

	public void doOptimiseAddColumn(OptimiseAddColumn action, List statements) {
		String schemaName = action.getTableSchema() == null ? action
				.getDataSetSchemaName() : action.getTableSchema().getName();
		String tableName = action.getTableName();
		String colName = action.getColumnName();
		statements.add("alter table " + schemaName + "." + tableName
				+ " add column (" + colName + " smallint default value 0)");
	}

	public void doOptimiseUpdateColumn(OptimiseUpdateColumn action,
			List statements) throws Exception {
		String pkSchemaName = action.getPkTableSchema() == null ? action
				.getDataSetSchemaName() : action.getPkTableSchema().getName();
		String pkTableName = action.getPkTableName();
		String fkSchemaName = action.getFkTableSchema() == null ? action
				.getDataSetSchemaName() : action.getFkTableSchema().getName();
		String fkTableName = action.getFkTableName();
		String colName = action.getOptimiseColumnName();

		StringBuffer sb = new StringBuffer();
		sb.append("update table " + pkSchemaName + "." + pkTableName + " set "
				+ colName + "=1 where (");
		for (Iterator i = action.getPkTableColumns().iterator(); i.hasNext();) {
			sb.append(((Column) i.next()).getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(") in (select ");
		for (Iterator i = action.getFkTableColumns().iterator(); i.hasNext();) {
			sb.append(((Column) i.next()).getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + fkSchemaName + "." + fkTableName + ")");

		statements.add(sb.toString());
	}

	public void doCreate(Create action, List statements) {
		String createTableSchema = action.getNewTableSchema() == null ? action
				.getDataSetSchemaName() : action.getNewTableSchema().getName();
		String createTableName = action.getNewTableName();
		String fromTableSchema = action.getSelectFromTableSchema() == null ? action
				.getDataSetSchemaName()
				: action.getSelectFromTableSchema().getName();
		String fromTableName = action.getSelectFromTableName();

		StringBuffer sb = new StringBuffer();
		sb.append("create table " + createTableSchema + "." + createTableName
				+ " as select ");
		for (Iterator i = action.getSelectFromColumns().iterator(); i.hasNext();) {
			Column col = (Column) i.next();
			if (action.isUseAliases()) {
				DataSetColumn dsCol = (DataSetColumn) col;
				if (dsCol instanceof WrappedColumn) {
					sb.append(((WrappedColumn) dsCol).getWrappedColumn()
							.getName());
					sb.append(" as ");
				} else if (dsCol instanceof SchemaNameColumn) {
					sb.append('\'');
					sb.append(fromTableSchema);
					sb.append("' as ");
				} else {
					// Ouch!
					throw new MartBuilderInternalError();
				}
			}
			sb.append(col.getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + fromTableSchema + "." + fromTableName);

		statements.add(sb.toString());
	}

	public void doMerge(Merge action, List statements) throws Exception {
		String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName() : action.getSourceTableSchema()
				.getName();
		String srcTableName = action.getSourceTableName();
		String trgtSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName() : action.getTargetTableSchema()
				.getName();
		String trgtTableName = action.getTargetTableName();
		String mergeSchemaName = action.getMergedTableSchema() == null ? action
				.getDataSetSchemaName() : action.getMergedTableSchema()
				.getName();
		String mergeTableName = action.getMergedTableName();

		StringBuffer sb = new StringBuffer();
		sb.append("create table " + mergeSchemaName + "." + mergeTableName
				+ " as select a.*");
		for (Iterator i = action.getTargetTableColumns().iterator(); i
				.hasNext();) {
			sb.append(",b.");
			Column col = (Column) i.next();
			if (action.isUseAliases()) {
				DataSetColumn dsCol = (DataSetColumn) col;
				if (dsCol instanceof WrappedColumn) {
					sb.append(((WrappedColumn) dsCol).getWrappedColumn()
							.getName());
					sb.append(" as ");
				} else if (dsCol instanceof SchemaNameColumn) {
					sb.append('\'');
					sb.append(trgtSchemaName);
					sb.append("' as ");
				} else {
					// Ouch!
					throw new MartBuilderInternalError();
				}
			}
			sb.append(col.getName());
			if (i.hasNext())
				sb.append(',');
		}
		sb.append(" from " + srcSchemaName + "." + srcTableName + " as a "
				+ (action.isUseLeftJoin() ? "left" : "inner") + " join "
				+ trgtSchemaName + "." + trgtTableName + " as b using ");
		for (int i = 0; i < action.getTargetTableKeyColumns().size(); i++) {
			if (i > 0)
				sb.append(',');
			String pkColName = ((Column) action.getSourceTableKeyColumns().get(
					i)).getName();
			String fkColName = ((Column) action.getTargetTableKeyColumns().get(
					i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName);
		}

		statements.add(sb.toString());
	}

	public void doConcat(Concat action, List statements) {
		String srcSchemaName = action.getSourceTableSchema() == null ? action
				.getDataSetSchemaName() : action.getSourceTableSchema()
				.getName();
		String srcTableName = action.getSourceTableName();
		String trgtSchemaName = action.getTargetTableSchema() == null ? action
				.getDataSetSchemaName() : action.getTargetTableSchema()
				.getName();
		String trgtTableName = action.getTargetTableName();
		String concatSchemaName = action.getConcatTableSchema() == null ? action
				.getDataSetSchemaName()
				: action.getConcatTableSchema().getName();
		String concatTableName = action.getConcatTableName();
		ConcatRelationType crType = action.getConcatRelationType();

		StringBuffer sb = new StringBuffer();
		sb.append("create table " + concatSchemaName + "." + concatTableName
				+ " as select a.*, group_concat(distinct concat_ws('");
		sb.append(crType.getValueSeparator());
		sb.append("'");
		for (Iterator i = action.getTargetTableConcatColumns().iterator(); i
				.hasNext();) {
			sb.append(',');
			Column col = (Column) i.next();
			sb.append(col.getName());
		}
		sb.append(") separator '");
		sb.append(crType.getRecordSeparator());
		sb.append("'))");

		sb.append(" from " + srcSchemaName + "." + srcTableName
				+ " as a inner join " + trgtSchemaName + "." + trgtTableName
				+ " as b using ");
		for (int i = 0; i < action.getTargetTableKeyColumns().size(); i++) {
			if (i > 0)
				sb.append(',');
			String pkColName = ((Column) action.getSourceTableKeyColumns().get(
					i)).getName();
			String fkColName = ((Column) action.getTargetTableKeyColumns().get(
					i)).getName();
			sb.append("a." + pkColName + "=b." + fkColName);
		}
		// group by all a columns

		statements.add(sb.toString());
	}
}
