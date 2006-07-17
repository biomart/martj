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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.resources.Resources;

/**
 * Represents one task in the grand scheme of constructing a mart.
 * Implementations of this abstract class will provide specific methods for
 * working with the various different stages of mart construction.
 * <p>
 * In all actions, if any schema parameter is null, it means to use the dataset
 * schema instead, as specified by the datasetSchemaName parameter.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 17th July 2006
 * @since 0.1
 */
public abstract class MartConstructorAction {
	private Set parents;

	private Set children;

	private int depth;

	private int sequence;

	private String datasetSchemaName;

	private static int nextSequence = 0;

	private static final String nextSequenceLock = "__SEQ_LOCK";

	/**
	 * Sets up a node.
	 */
	public MartConstructorAction(String datasetSchemaName) {
		this.depth = 0;
		this.datasetSchemaName = datasetSchemaName;
		synchronized (nextSequenceLock) {
			this.sequence = nextSequence++;
		}
		this.children = new HashSet();
		this.parents = new HashSet();
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
	 * Adds a child to this node. The child will have this node added as a
	 * parent.
	 * 
	 * @param child
	 *            the child to add to this node.
	 */
	public void addChild(MartConstructorAction child) {
		this.children.add(child);
		child.parents.add(this);
		child.ensureDepth(this.depth + 1);
	}

	/**
	 * Adds children to this node. Each child will have this node added as a
	 * parent.
	 * 
	 * @param children
	 *            the children to add to this node.
	 */
	public void addChildren(Collection children) {
		for (Iterator i = children.iterator(); i.hasNext();)
			this.addChild((MartConstructorAction) i.next());
	}

	/**
	 * Adds a parent to this node. The parent will have this node added as a
	 * child.
	 * 
	 * @param parent
	 *            the parent to add to this node.
	 */
	public void addParent(MartConstructorAction parent) {
		this.parents.add(parent);
		parent.children.add(this);
		this.ensureDepth(parent.depth + 1);
	}

	/**
	 * Adds parents to this node. Each parent will have this node added as a
	 * child.
	 * 
	 * @param parents
	 *            the parents to add to this node.
	 */
	public void addParents(Collection parents) {
		for (Iterator i = parents.iterator(); i.hasNext();)
			this.addParent((MartConstructorAction) i.next());
	}

	/**
	 * Returns the children of this node.
	 * 
	 * @return the children of this node.
	 */
	public Collection getChildren() {
		return this.children;
	}

	/**
	 * Returns the parents of this node.
	 * 
	 * @return the parents of this node.
	 */
	public Collection getParents() {
		return this.parents;
	}

	private void ensureDepth(int newDepth) {
		// Is the new depth less than our current
		// depth? If so, need do nothing.
		if (this.depth >= newDepth)
			return;

		// Remember the new depth.
		this.depth = newDepth;

		// Ensure the child depths are at least one greater
		// than our own new depth.
		for (Iterator i = this.children.iterator(); i.hasNext();) {
			MartConstructorAction child = (MartConstructorAction) i.next();
			child.ensureDepth(this.depth + 1);
		}
	}

	/**
	 * Returns the current depth (level) of the graph at which this action lies.
	 * 
	 * @retun the current depth, 0-indexed.
	 */
	public int getDepth() {
		return this.depth;
	}

	/**
	 * Override this method to produce a message describing what this node of
	 * the graph will do.
	 * 
	 * @return a description of what this node will do.
	 */
	public abstract String getStatusMessage();

	/**
	 * Returns the order in which this action was created.
	 * 
	 * @return the order in which this action was created. 0 is the first.
	 */
	public int getSequence() {
		return this.sequence;
	}

	public int hashCode() {
		return this.sequence;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		else
			return (obj != null && (obj instanceof MartConstructorAction) && this.sequence == ((MartConstructorAction) obj).sequence);
	}

	/**
	 * Represents the various tasks in the mart construction process and how
	 * they should fit together.
	 */
	public static class MartConstructorActionGraph {
		private Collection actions = new HashSet();

		/**
		 * Adds an action to the graph.
		 * 
		 * @param action
		 *            the action to add.
		 */
		public void addAction(MartConstructorAction action) {
			this.actions.add(action);
		}

		/**
		 * Adds an action to the graph and attaches it to the given parent.
		 * 
		 * @param action
		 *            the action to add.
		 * @param parent
		 *            the parent to attach it to.
		 */
		public void addActionWithParent(MartConstructorAction action,
				MartConstructorAction parent) {
			action.addParent(parent);
			this.actions.add(action);
		}

		/**
		 * Adds an action to the graph and attaches it to the given parents.
		 * 
		 * @param action
		 *            the action to add.
		 * @param parents
		 *            the parents to attach it to.
		 */
		public void addActionWithParents(MartConstructorAction action,
				Collection parents) {
			for (Iterator i = parents.iterator(); i.hasNext();)
				action.addParent((MartConstructorAction) i.next());
			this.actions.add(action);
		}

		/**
		 * Returns a set of all actions in this graph.
		 * 
		 * @return all the actions in this graph.
		 */
		public Collection getActions() {
			return this.actions;
		}

		/**
		 * Returns all the actions in this graph which are at a particular
		 * depth.
		 * 
		 * @param depth
		 *            the depth to search.
		 * @return all the actions at that depth. It may return an empty set if
		 *         there are none, but it will never return <tt>null</tt>.
		 */
		public Collection getActionsAtDepth(int depth) {
			Collection matches = new HashSet();
			for (Iterator i = this.actions.iterator(); i.hasNext();) {
				MartConstructorAction action = (MartConstructorAction) i.next();
				if (action.depth == depth)
					matches.add(action);
			}
			return matches;
		}
	}

	/**
	 * Placeholder actions do nothing except wait until other actions have
	 * caught up.
	 */
	public static class PlaceHolder extends MartConstructorAction {
		public PlaceHolder(String datasetSchemaName) {
			super(datasetSchemaName);
		}

		public String getStatusMessage() {
			return Resources.get("mcPlaceHolder");
		}
	}

	/**
	 * Union actions create a new table out of a bunch of others by performing a
	 * union statement over them.
	 */
	public static class Union extends MartConstructorAction {
		private Schema unionTableSchema;

		private String unionTableName;

		private List targetTableSchemas;

		private List targetTableNames;

		public Union(String datasetSchemaName, Schema unionTableSchema,
				String unionTableName, List targetTableSchemas,
				List targetTableNames) {
			super(datasetSchemaName);
			this.unionTableSchema = unionTableSchema;
			this.unionTableName = unionTableName;
			this.targetTableSchemas = targetTableSchemas;
			this.targetTableNames = targetTableNames;
		}

		public List getTargetTableNames() {
			return targetTableNames;
		}

		public List getTargetTableSchemas() {
			return targetTableSchemas;
		}

		public String getUnionTableName() {
			return unionTableName;
		}

		public Schema getUnionTableSchema() {
			return unionTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcUnion");
		}
	}

	/**
	 * Drop actions drop tables.
	 */
	public static class Drop extends MartConstructorAction {
		private Schema dropTableSchema;

		private String dropTableName;

		public Drop(String datasetSchemaName, Schema dropTableSchema,
				String dropTableName) {
			super(datasetSchemaName);
			this.dropTableSchema = dropTableSchema;
			this.dropTableName = dropTableName;
		}

		public String getDropTableName() {
			return dropTableName;
		}

		public Schema getDropTableSchema() {
			return dropTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcDrop");
		}
	}

	/**
	 * Partition actions create a second table that represents all the rows from
	 * the first table where a particular column has a given value.
	 */
	public static class Partition extends MartConstructorAction {
		private Schema targetTableSchema;

		private String targetTableName;

		private Schema partitionTableSchema;

		private String partitionTableName;

		private String partitionColumnName;

		private Object partitionColumnValue;

		public Partition(String datasetSchemaName, Schema targetTableSchema,
				String targetTableName, Schema partitionTableSchema,
				String partitionTableName, String partitionColumnName,
				Object partitionColumnValue) {
			super(datasetSchemaName);
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.partitionTableSchema = partitionTableSchema;
			this.partitionTableName = partitionTableName;
			this.partitionColumnName = partitionColumnName;
			this.partitionColumnValue = partitionColumnValue;
		}

		public String getPartitionColumnName() {
			return partitionColumnName;
		}

		public void setPartitionColumnName(String partitionColumnName) {
			this.partitionColumnName = partitionColumnName;
		}

		public Object getPartitionColumnValue() {
			return partitionColumnValue;
		}

		public void setPartitionColumnValue(Object partitionColumnValue) {
			this.partitionColumnValue = partitionColumnValue;
		}

		public String getPartitionTableName() {
			return partitionTableName;
		}

		public void setPartitionTableName(String partitionTableName) {
			this.partitionTableName = partitionTableName;
		}

		public Schema getPartitionTableSchema() {
			return partitionTableSchema;
		}

		public void setPartitionTableSchema(Schema partitionTableSchema) {
			this.partitionTableSchema = partitionTableSchema;
		}

		public String getTargetTableName() {
			return targetTableName;
		}

		public void setTargetTableName(String targetTableName) {
			this.targetTableName = targetTableName;
		}

		public Schema getTargetTableSchema() {
			return targetTableSchema;
		}

		public void setTargetTableSchema(Schema targetTableSchema) {
			this.targetTableSchema = targetTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcPartition");
		}
	}

	/**
	 * Primary key actions create a primary key over the given set of columns on
	 * the given table.
	 */
	public static class PK extends MartConstructorAction {

		private Schema pkTableSchema;

		private String pkTableName;

		private List pkColumns;

		public PK(String datasetSchemaName, Schema pkTableSchema,
				String pkTableName, List pkColumns) {
			super(datasetSchemaName);
			this.pkTableSchema = pkTableSchema;
			this.pkTableName = pkTableName;
			this.pkColumns = pkColumns;
		}

		public List getPkColumns() {
			return pkColumns;
		}

		public String getPkTableName() {
			return pkTableName;
		}

		public Schema getPkTableSchema() {
			return pkTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcPK");
		}
	}

	/**
	 * Index actions create indexes over the given set of columns on the given
	 * table.
	 */
	public static class Index extends MartConstructorAction {

		private Schema indexTableSchema;

		private String indexTableName;

		private List indexColumns;

		public Index(String datasetSchemaName, Schema indexTableSchema,
				String indexTableName, List indexColumns) {
			super(datasetSchemaName);
			this.indexTableSchema = indexTableSchema;
			this.indexTableName = indexTableName;
			this.indexColumns = indexColumns;
		}

		public List getIndexColumns() {
			return indexColumns;
		}

		public String getIndexTableName() {
			return indexTableName;
		}

		public Schema getIndexTableSchema() {
			return indexTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcIndex");
		}
	}

	/**
	 * Foreign key actions create a foreign key over the given set of columns on
	 * the given table, then establishes a relation to the given primary key
	 * columns an the given table.
	 */
	public static class FK extends MartConstructorAction {

		private Schema fkTableSchema;

		private String fkTableName;

		private List fkColumns;

		private Schema pkTableSchema;

		private String pkTableName;

		private List pkColumns;

		public FK(String datasetSchemaName, Schema fkTableSchema,
				String fkTableName, List fkColumns, Schema pkTableSchema,
				String pkTableName, List pkColumns) {
			super(datasetSchemaName);
			this.fkTableSchema = fkTableSchema;
			this.fkTableName = fkTableName;
			this.fkColumns = fkColumns;
			this.pkTableSchema = pkTableSchema;
			this.pkTableName = pkTableName;
			this.pkColumns = pkColumns;
		}

		public List getPkColumns() {
			return pkColumns;
		}

		public String getPkTableName() {
			return pkTableName;
		}

		public Schema getPkTableSchema() {
			return pkTableSchema;
		}

		public List getFkColumns() {
			return fkColumns;
		}

		public String getFkTableName() {
			return fkTableName;
		}

		public Schema getFkTableSchema() {
			return fkTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcFK");
		}
	}

	/**
	 * Rename table actions rename tables.
	 */
	public static class Rename extends MartConstructorAction {
		private Schema renameTableSchema;

		private String renameTableOldName;

		private String renameTableNewName;

		public Rename(String datasetSchemaName, Schema renameTableSchema,
				String renameTableOldName, String renameTableNewName) {
			super(datasetSchemaName);
			this.renameTableSchema = renameTableSchema;
			this.renameTableOldName = renameTableOldName;
			this.renameTableNewName = renameTableNewName;
		}

		public String getRenameTableNewName() {
			return renameTableNewName;
		}

		public String getRenameTableOldName() {
			return renameTableOldName;
		}

		public Schema getRenameTableSchema() {
			return renameTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcRename");
		}
	}

	/**
	 * Creates tables based on selected columns. If the useAliases flag is
	 * turned on, it expects instances of {@link DataSetColumn} and will select
	 * columns and alias them. If not, then it expects {@link Column} instances
	 * and will select columns by name alone.
	 */
	public static class Create extends MartConstructorAction {
		private Schema newTableSchema;

		private String newTableName;

		private Schema selectFromTableSchema;

		private String selectFromTableName;

		private List selectFromColumns;

		private boolean useAliases;

		public Create(String datasetSchemaName, Schema newTableSchema,
				String newTableName, Schema selectFromTableSchema,
				String selectFromTableName, List selectFromColumns,
				boolean useAliases) {
			super(datasetSchemaName);
			this.newTableSchema = newTableSchema;
			this.newTableName = newTableName;
			this.selectFromTableSchema = selectFromTableSchema;
			this.selectFromTableName = selectFromTableName;
			this.selectFromColumns = selectFromColumns;
			this.useAliases = useAliases;
		}

		public String getNewTableName() {
			return newTableName;
		}

		public Schema getNewTableSchema() {
			return newTableSchema;
		}

		public List getSelectFromColumns() {
			return selectFromColumns;
		}

		public String getSelectFromTableName() {
			return selectFromTableName;
		}

		public Schema getSelectFromTableSchema() {
			return selectFromTableSchema;
		}

		public boolean isUseAliases() {
			return useAliases;
		}

		public String getStatusMessage() {
			return Resources.get("mcCreate");
		}
	}

	/**
	 * This action creates a column on a table ready for use as an optimisation
	 * ('has') column.
	 */
	public static class OptimiseAddColumn extends MartConstructorAction {
		private Schema tableSchema;

		private String tableName;

		private String columnName;

		public OptimiseAddColumn(String datasetSchemaName, Schema tableSchema,
				String tableName, String columnName) {
			super(datasetSchemaName);
			this.tableSchema = tableSchema;
			this.tableName = tableName;
			this.columnName = columnName;
		}

		public String getColumnName() {
			return columnName;
		}

		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public Schema getTableSchema() {
			return tableSchema;
		}

		public void setTableSchema(Schema tableSchema) {
			this.tableSchema = tableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcOptimiseAdd");
		}
	}

	/**
	 * This class performs an in-place update that changes the value of every
	 * row in the PK table which has a corresponding row in the FK table. e
	 * left-join on a table.
	 */
	public static class OptimiseUpdateColumn extends MartConstructorAction {
		private Schema fkTableSchema;

		private String fkTableName;

		private List fkTableColumns;

		private Schema pkTableSchema;

		private String pkTableName;

		private List pkTableColumns;

		private String optimiseColumnName;

		public OptimiseUpdateColumn(String datasetSchemaName,
				Schema fkTableSchema, String fkTableName, List fkTableColumns,
				Schema pkTableSchema, String pkTableName, List pkTableColumns,
				String optimiseColumnName) {
			super(datasetSchemaName);
			this.fkTableSchema = fkTableSchema;
			this.fkTableName = fkTableName;
			this.fkTableColumns = fkTableColumns;
			this.pkTableSchema = pkTableSchema;
			this.pkTableName = pkTableName;
			this.pkTableColumns = pkTableColumns;
			this.optimiseColumnName = optimiseColumnName;
		}

		public List getFkTableColumns() {
			return fkTableColumns;
		}

		public void setFkTableColumns(List fkTableColumns) {
			this.fkTableColumns = fkTableColumns;
		}

		public String getFkTableName() {
			return fkTableName;
		}

		public void setFkTableName(String fkTableName) {
			this.fkTableName = fkTableName;
		}

		public Schema getFkTableSchema() {
			return fkTableSchema;
		}

		public void setFkTableSchema(Schema fkTableSchema) {
			this.fkTableSchema = fkTableSchema;
		}

		public List getPkTableColumns() {
			return pkTableColumns;
		}

		public void setPkTableColumns(List pkTableColumns) {
			this.pkTableColumns = pkTableColumns;
		}

		public String getPkTableName() {
			return pkTableName;
		}

		public void setPkTableName(String pkTableName) {
			this.pkTableName = pkTableName;
		}

		public Schema getPkTableSchema() {
			return pkTableSchema;
		}

		public void setPkTableSchema(Schema pkTableSchema) {
			this.pkTableSchema = pkTableSchema;
		}

		public String getOptimiseColumnName() {
			return optimiseColumnName;
		}

		public void setOptimiseColumnName(String optimiseColumnName) {
			this.optimiseColumnName = optimiseColumnName;
		}

		public String getStatusMessage() {
			return Resources.get("mcOptimiseUpdate");
		}
	}

	/**
	 * This action creates a new table containing the selected columns from the
	 * old table plus all the expression columns, optionally grouping if any of
	 * them are group-by expression columns.
	 * 
	 */
	public static class ExpressionAddColumns extends MartConstructorAction {
		public ExpressionAddColumns(String datasetSchemaName,
				Schema sourceTableSchema, String sourceTableName,
				Schema targetTableSchema, String targetTableName,
				List sourceTableColumns, List expressionColumns,
				boolean useGroupBy) {
			super(datasetSchemaName);
			// TODO: The rest!
		}

		public String getStatusMessage() {
			return Resources.get("mcExpressionAdd");
		}
	}

	/**
	 * This action creates a new table based on the merge between two existing
	 * tables. If the useAliases flag is turned on, it expects instances of
	 * {@link DataSetColumn} and will select columns and alias them. If not,
	 * then it expects {@link Column} instances and will select columns by name
	 * alone.
	 */
	public static class Merge extends MartConstructorAction {

		private Schema mergedTableSchema;

		private String mergedTableName;

		private Schema sourceTableSchema;

		private String sourceTableName;

		private List sourceTableKeyColumns;

		private boolean useLeftJoin;

		private Schema targetTableSchema;

		private String targetTableName;

		private List targetTableKeyColumns;

		private List targetTableColumns;

		private boolean useAliases;

		public Merge(String datasetSchemaName, Schema mergedTableSchema,
				String mergedTableName, Schema sourceTableSchema,
				String sourceTableName, List sourceTableKeyColumns,
				boolean useLeftJoin, Schema targetTableSchema,
				String targetTableName, List targetTableKeyColumns,
				List targetTableColumns, boolean useAliases) {
			super(datasetSchemaName);
			this.mergedTableSchema = mergedTableSchema;
			this.mergedTableName = mergedTableName;
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTableKeyColumns = sourceTableKeyColumns;
			this.useLeftJoin = useLeftJoin;
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.targetTableKeyColumns = targetTableKeyColumns;
			this.targetTableColumns = targetTableColumns;
			this.useAliases = useAliases;
		}

		public String getMergedTableName() {
			return mergedTableName;
		}

		public Schema getMergedTableSchema() {
			return mergedTableSchema;
		}

		public List getSourceTableKeyColumns() {
			return sourceTableKeyColumns;
		}

		public String getSourceTableName() {
			return sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return sourceTableSchema;
		}

		public List getTargetTableColumns() {
			return targetTableColumns;
		}

		public List getTargetTableKeyColumns() {
			return targetTableKeyColumns;
		}

		public String getTargetTableName() {
			return targetTableName;
		}

		public Schema getTargetTableSchema() {
			return targetTableSchema;
		}

		public boolean isUseAliases() {
			return useAliases;
		}

		public boolean isUseLeftJoin() {
			return useLeftJoin;
		}

		public String getStatusMessage() {
			return Resources.get("mcMerge");
		}
	}

	/**
	 * This action creates a new table based on the concatenated columns of a
	 * relation between two existing tables. If the useAliases flag is turned
	 * on, it expects instances of {@link DataSetColumn} and will select columns
	 * and alias them. If not, then it expects {@link Column} instances and will
	 * select columns by name alone.
	 */
	public static class Concat extends MartConstructorAction {

		private Schema concatTableSchema;

		private String concatTableName;

		private Schema sourceTableSchema;

		private String sourceTableName;

		private List sourceTableKeyColumns;

		private Schema targetTableSchema;

		private String targetTableName;

		private List targetTableKeyColumns;

		private List targetTableConcatColumns;

		private String targetConcatColumnName;

		private ConcatRelationType concatRelationType;

		public Concat(String datasetSchemaName, Schema concatTableSchema,
				String concatTableName, Schema sourceTableSchema,
				String sourceTableName, List sourceTableKeyColumns,
				Schema targetTableSchema, String targetTableName,
				List targetTableKeyColumns, List targetTableConcatColumns,
				String targetConcatColumnName,
				ConcatRelationType concatRelationType) {
			super(datasetSchemaName);
			this.concatTableSchema = concatTableSchema;
			this.concatTableName = concatTableName;
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTableKeyColumns = sourceTableKeyColumns;
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.targetTableKeyColumns = targetTableKeyColumns;
			this.targetTableConcatColumns = targetTableConcatColumns;
			this.targetConcatColumnName = targetConcatColumnName;
			this.concatRelationType = concatRelationType;
		}

		public String getConcatTableName() {
			return concatTableName;
		}

		public Schema getConcatTableSchema() {
			return concatTableSchema;
		}

		public List getSourceTableKeyColumns() {
			return sourceTableKeyColumns;
		}

		public String getSourceTableName() {
			return sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return sourceTableSchema;
		}

		public List getTargetTableConcatColumns() {
			return targetTableConcatColumns;
		}

		public List getTargetTableKeyColumns() {
			return targetTableKeyColumns;
		}

		public String getTargetTableName() {
			return targetTableName;
		}

		public Schema getTargetTableSchema() {
			return targetTableSchema;
		}

		public String getTargetConcatColumnName() {
			return targetConcatColumnName;
		}

		public ConcatRelationType getConcatRelationType() {
			return concatRelationType;
		}

		public String getStatusMessage() {
			return Resources.get("mcConcat");
		}
	}

	/**
	 * This action creates a new table based on all the columns of a target
	 * table when inner joined with some source table.
	 */
	public static class Reduce extends MartConstructorAction {

		private Schema reducedTableSchema;

		private String reducedTableName;

		private Schema sourceTableSchema;

		private String sourceTableName;

		private List sourceTableKeyColumns;

		private Schema targetTableSchema;

		private String targetTableName;

		private List targetTableKeyColumns;

		public Reduce(String datasetSchemaName, Schema reducedTableSchema,
				String reducedTableName, Schema sourceTableSchema,
				String sourceTableName, List sourceTableKeyColumns,
				Schema targetTableSchema, String targetTableName,
				List targetTableKeyColumns) {
			super(datasetSchemaName);
			this.reducedTableSchema = reducedTableSchema;
			this.reducedTableName = reducedTableName;
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTableKeyColumns = sourceTableKeyColumns;
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.targetTableKeyColumns = targetTableKeyColumns;
		}

		public String getReducedTableName() {
			return reducedTableName;
		}

		public Schema getReducedTableSchema() {
			return reducedTableSchema;
		}

		public List getSourceTableKeyColumns() {
			return sourceTableKeyColumns;
		}

		public String getSourceTableName() {
			return sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return sourceTableSchema;
		}

		public List getTargetTableKeyColumns() {
			return targetTableKeyColumns;
		}

		public String getTargetTableName() {
			return targetTableName;
		}

		public Schema getTargetTableSchema() {
			return targetTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcReduce");
		}
	}
}
