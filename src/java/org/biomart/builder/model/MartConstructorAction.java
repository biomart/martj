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

package org.biomart.builder.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.biomart.builder.model.SchemaModificationSet.TableRestriction;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * Represents one task in the grand scheme of constructing a mart.
 * Implementations of this abstract class will provide specific methods for
 * working with the various different stages of mart construction.
 * <p>
 * In all actions, if any schema parameter is null, it means to use the dataset
 * schema instead, as specified by the datasetSchemaName parameter.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public abstract class MartConstructorAction {

	private String datasetTableName;

	private String datasetSchemaName;

	/**
	 * Sets up a node.
	 * 
	 * @param datasetSchemaName
	 *            the name of the schema within which the dataset will be
	 *            constructed. Wherever other schemas in actions are specified
	 *            as null, this schema will be used in place.
	 * @param datasetTableName
	 *            the name of the table this action is associated with.
	 */
	public MartConstructorAction(final String datasetSchemaName,
			final String datasetTableName) {
		this.datasetSchemaName = datasetSchemaName;
		this.datasetTableName = datasetTableName;
		Log.debug("Constructor action created: " + this.getClass().getName());
	}

	/**
	 * Returns the dataset table name for this action.
	 * 
	 * @return the dataset table name.
	 */
	public String getDataSetTableName() {
		return this.datasetTableName;
	}

	/**
	 * Returns the dataset schema name for this action.
	 * 
	 * @return the dataset schema name.
	 */
	public String getDataSetSchemaName() {
		return this.datasetSchemaName;
	}

	/**
	 * Override this method to produce a message describing what this node of
	 * the graph will do.
	 * 
	 * @return a description of what this node will do.
	 */
	public abstract String getStatusMessage();

	/**
	 * Update optimiser table actions.
	 */
	public static class UpdateOptimiser extends MartConstructorAction {

		private Collection keyColumns;
		private String optTableName;
		private Collection nonNullColumns;
		private String optColumnName;
		private boolean countNotBool;
		
		/**
		 * Creates a new UpdateOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public UpdateOptimiser(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcUpdateOpt");
		}

		/**
		 * @return the countNotBool
		 */
		public boolean isCountNotBool() {
			return countNotBool;
		}

		/**
		 * @param countNotBool the countNotBool to set
		 */
		public void setCountNotBool(boolean countNotBool) {
			this.countNotBool = countNotBool;
		}

		/**
		 * @return the keyColumns
		 */
		public Collection getKeyColumns() {
			return keyColumns;
		}

		/**
		 * @param keyColumns the keyColumns to set
		 */
		public void setKeyColumns(Collection keyColumns) {
			this.keyColumns = keyColumns;
		}

		/**
		 * @return the nonNullColumns
		 */
		public Collection getNonNullColumns() {
			return nonNullColumns;
		}

		/**
		 * @param nonNullColumns the nonNullColumns to set
		 */
		public void setNonNullColumns(Collection nonNullColumns) {
			this.nonNullColumns = nonNullColumns;
		}

		/**
		 * @return the optColumnName
		 */
		public String getOptColumnName() {
			return optColumnName;
		}

		/**
		 * @param optColumnName the optColumnName to set
		 */
		public void setOptColumnName(String optColumnName) {
			this.optColumnName = optColumnName;
		}

		/**
		 * @return the optTableName
		 */
		public String getOptTableName() {
			return optTableName;
		}

		/**
		 * @param optTableName the optTableName to set
		 */
		public void setOptTableName(String optTableName) {
			this.optTableName = optTableName;
		}
	}
	
	/**
	 * Create optimiser table actions.
	 */
	public static class CreateOptimiser extends MartConstructorAction {

		private Collection keyColumns;
		private String optTableName;
		
		/**
		 * Creates a new CreateOptimiser action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public CreateOptimiser(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCreateOpt");
		}

		/**
		 * @return the keyColumns
		 */
		public Collection getKeyColumns() {
			return keyColumns;
		}

		/**
		 * @param keyColumns the keyColumns to set
		 */
		public void setKeyColumns(Collection keyColumns) {
			this.keyColumns = keyColumns;
		}

		/**
		 * @return the optTableName
		 */
		public String getOptTableName() {
			return optTableName;
		}

		/**
		 * @param optTableName the optTableName to set
		 */
		public void setOptTableName(String optTableName) {
			this.optTableName = optTableName;
		}
	}
	
	/**
	 * Left-join actions.
	 */
	public static class LeftJoin extends MartConstructorAction {

		private String leftTable;

		private String rightSchema;

		private String rightTable;

		private List leftJoinColumns;

		private List rightJoinColumns;

		private Map selectColumns;

		private String resultTable;
		
		private TableRestriction tableRestriction;
		
		private String partitionColumn;
		
		private String partitionValue;

		/**
		 * Creates a new LeftJoin action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public LeftJoin(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcMerge");
		}

		/**
		 * @return the leftJoinColumns
		 */
		public List getLeftJoinColumns() {
			return leftJoinColumns;
		}

		/**
		 * @param leftJoinColumns
		 *            the leftJoinColumns to set
		 */
		public void setLeftJoinColumns(List leftJoinColumns) {
			this.leftJoinColumns = leftJoinColumns;
		}

		/**
		 * @return the leftTable
		 */
		public String getLeftTable() {
			return leftTable;
		}

		/**
		 * @param leftTable
		 *            the leftTable to set
		 */
		public void setLeftTable(String leftTable) {
			this.leftTable = leftTable;
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the rightJoinColumns
		 */
		public List getRightJoinColumns() {
			return rightJoinColumns;
		}

		/**
		 * @param rightJoinColumns
		 *            the rightJoinColumns to set
		 */
		public void setRightJoinColumns(List rightJoinColumns) {
			this.rightJoinColumns = rightJoinColumns;
		}

		/**
		 * @return the rightSchema
		 */
		public String getRightSchema() {
			return rightSchema;
		}

		/**
		 * @param rightSchema
		 *            the rightSchema to set
		 */
		public void setRightSchema(String rightSchema) {
			this.rightSchema = rightSchema;
		}

		/**
		 * @return the rightTable
		 */
		public String getRightTable() {
			return rightTable;
		}

		/**
		 * @param rightTable
		 *            the rightTable to set
		 */
		public void setRightTable(String rightTable) {
			this.rightTable = rightTable;
		}

		/**
		 * @return the selectColumns
		 */
		public Map getSelectColumns() {
			return selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(Map selectColumns) {
			this.selectColumns = selectColumns;
		}

		/**
		 * @return the tableRestriction
		 */
		public TableRestriction getTableRestriction() {
			return tableRestriction;
		}

		/**
		 * @param tableRestriction the tableRestriction to set
		 */
		public void setTableRestriction(TableRestriction tableRestriction) {
			this.tableRestriction = tableRestriction;
		}

		/**
		 * @return the partitionColumn
		 */
		public String getPartitionColumn() {
			return partitionColumn;
		}

		/**
		 * @param partitionColumn the partitionColumn to set
		 */
		public void setPartitionColumn(String partitionColumn) {
			this.partitionColumn = partitionColumn;
		}

		/**
		 * @return the partitionValue
		 */
		public String getPartitionValue() {
			return partitionValue;
		}

		/**
		 * @param partitionValue the partitionValue to set
		 */
		public void setPartitionValue(String partitionValue) {
			this.partitionValue = partitionValue;
		}
	}

	/**
	 * Select actions.
	 */
	public static class Select extends MartConstructorAction {

		private String schema;

		private String table;

		private Map selectColumns;

		private String resultTable;
		
		private TableRestriction tableRestriction;
		
		private String partitionColumn;
		
		private String partitionValue;

		/**
		 * Creates a new Select action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Select(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcCreate");
		}

		/**
		 * @return the resultTable
		 */
		public String getResultTable() {
			return resultTable;
		}

		/**
		 * @param resultTable
		 *            the resultTable to set
		 */
		public void setResultTable(String resultTable) {
			this.resultTable = resultTable;
		}

		/**
		 * @return the schema
		 */
		public String getSchema() {
			return schema;
		}

		/**
		 * @param schema
		 *            the schema to set
		 */
		public void setSchema(String schema) {
			this.schema = schema;
		}

		/**
		 * @return the selectColumns
		 */
		public Map getSelectColumns() {
			return selectColumns;
		}

		/**
		 * @param selectColumns
		 *            the selectColumns to set
		 */
		public void setSelectColumns(Map selectColumns) {
			this.selectColumns = selectColumns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}

		/**
		 * @return the tableRestriction
		 */
		public TableRestriction getTableRestriction() {
			return tableRestriction;
		}

		/**
		 * @param tableRestriction the tableRestriction to set
		 */
		public void setTableRestriction(TableRestriction tableRestriction) {
			this.tableRestriction = tableRestriction;
		}

		/**
		 * @return the partitionColumn
		 */
		public String getPartitionColumn() {
			return partitionColumn;
		}

		/**
		 * @param partitionColumn the partitionColumn to set
		 */
		public void setPartitionColumn(String partitionColumn) {
			this.partitionColumn = partitionColumn;
		}

		/**
		 * @return the partitionValue
		 */
		public String getPartitionValue() {
			return partitionValue;
		}

		/**
		 * @param partitionValue the partitionValue to set
		 */
		public void setPartitionValue(String partitionValue) {
			this.partitionValue = partitionValue;
		}
	}

	/**
	 * Drop column actions.
	 */
	public static class DropColumns extends MartConstructorAction {

		private Collection columns;
		
		private String table;

		/**
		 * Creates a new DropColumns action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public DropColumns(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDropCols");
		}

		/**
		 * @return the columns
		 */
		public Collection getColumns() {
			return columns;
		}

		/**
		 * @param columns the columns to set
		 */
		public void setColumns(Collection columns) {
			this.columns = columns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return table;
		}

		/**
		 * @param table the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Drop actions.
	 */
	public static class Drop extends MartConstructorAction {

		private String table;

		/**
		 * Creates a new Drop action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Drop(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDrop");
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return table;
		}

		/**
		 * @param table the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Index actions.
	 */
	public static class Index extends MartConstructorAction {

		private String table;

		private List columns;

		/**
		 * Creates a new Index action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Index(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcIndex");
		}

		/**
		 * @return the columns
		 */
		public List getColumns() {
			return columns;
		}

		/**
		 * @param columns
		 *            the columns to set
		 */
		public void setColumns(List columns) {
			this.columns = columns;
		}

		/**
		 * @return the table
		 */
		public String getTable() {
			return table;
		}

		/**
		 * @param table
		 *            the table to set
		 */
		public void setTable(String table) {
			this.table = table;
		}
	}

	/**
	 * Rename actions.
	 */
	public static class Rename extends MartConstructorAction {

		private String from;

		private String to;

		/**
		 * Creates a new Rename action.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema we are working in.
		 * @param datasetTableName
		 *            the dataset table we are working on.
		 */
		public Rename(String datasetSchemaName, String datasetTableName) {
			super(datasetSchemaName, datasetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcRename");
		}

		/**
		 * @return the from
		 */
		public String getFrom() {
			return from;
		}

		/**
		 * @param from
		 *            the from to set
		 */
		public void setFrom(String from) {
			this.from = from;
		}

		/**
		 * @return the to
		 */
		public String getTo() {
			return to;
		}

		/**
		 * @param to
		 *            the to to set
		 */
		public void setTo(String to) {
			this.to = to;
		}

	}
}
