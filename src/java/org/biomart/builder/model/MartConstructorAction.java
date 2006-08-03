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
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.model.DataSet.DataSetTableRestriction;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
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
 * @version 0.1.7, 3rd August 2006
 * @since 0.1
 */
public abstract class MartConstructorAction {
	private Set parents;

	private Set children;

	private int depth;

	private int sequence;

	private String datasetSchemaName;

	private String datasetTableName;

	private static int nextSequence = 0;

	private static final String nextSequenceLock = "__SEQ_LOCK";

	/**
	 * Sets up a node.
	 * 
	 * @param datasetSchemaName
	 *            the name of the schema within which the dataset will be
	 *            constructed. Wherever other schemas in actions are specified
	 *            as null, this schema will be used in place.
	 * @param datasetTableName
	 *            the name of the table with which this action is associated as
	 *            part of the transformation process.
	 */
	public MartConstructorAction(final String datasetSchemaName,
			final String datasetTableName) {
		this.depth = 0;
		this.datasetSchemaName = datasetSchemaName;
		this.datasetTableName = datasetTableName;
		synchronized (MartConstructorAction.nextSequenceLock) {
			this.sequence = MartConstructorAction.nextSequence++;
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
	 * Returns the dataset table name for this action.
	 * 
	 * @return the dataset table name.
	 */
	public String getDataSetTableName() {
		return this.datasetTableName;
	}

	/**
	 * Adds a child to this node. The child will have this node added as a
	 * parent.
	 * 
	 * @param child
	 *            the child to add to this node.
	 */
	public void addChild(final MartConstructorAction child) {
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
	public void addChildren(final Collection children) {
		for (final Iterator i = children.iterator(); i.hasNext();)
			this.addChild((MartConstructorAction) i.next());
	}

	/**
	 * Adds a parent to this node. The parent will have this node added as a
	 * child.
	 * 
	 * @param parent
	 *            the parent to add to this node.
	 */
	public void addParent(final MartConstructorAction parent) {
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
	public void addParents(final Collection parents) {
		for (final Iterator i = parents.iterator(); i.hasNext();)
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

	private void ensureDepth(final int newDepth) {
		// Is the new depth less than our current
		// depth? If so, need do nothing.
		if (this.depth >= newDepth)
			return;

		// Remember the new depth.
		this.depth = newDepth;

		// Ensure the child depths are at least one greater
		// than our own new depth.
		for (final Iterator i = this.children.iterator(); i.hasNext();) {
			final MartConstructorAction child = (MartConstructorAction) i
					.next();
			child.ensureDepth(this.depth + 1);
		}
	}

	/**
	 * Returns the current depth (level) of the graph at which this action lies.
	 * 
	 * @return the current depth, 0-indexed.
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

	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		else
			return obj != null && obj instanceof MartConstructorAction
					&& this.sequence == ((MartConstructorAction) obj).sequence;
	}

	/**
	 * Represents the various tasks in the mart construction process and how
	 * they should fit together.
	 */
	public static class MartConstructorActionGraph {
		private final Collection actions = new HashSet();

		/**
		 * Adds an action to the graph.
		 * 
		 * @param action
		 *            the action to add.
		 */
		public void addAction(final MartConstructorAction action) {
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
		public void addActionWithParent(final MartConstructorAction action,
				final MartConstructorAction parent) {
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
		public void addActionWithParents(final MartConstructorAction action,
				final Collection parents) {
			for (final Iterator i = parents.iterator(); i.hasNext();)
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
		public Collection getActionsAtDepth(final int depth) {
			final Collection matches = new HashSet();
			for (final Iterator i = this.actions.iterator(); i.hasNext();) {
				final MartConstructorAction action = (MartConstructorAction) i
						.next();
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
		/**
		 * The placeholder action requires only the name of the dataset schema
		 * in order to pass it up to the parent constructor.
		 * 
		 * @param dsSchemaName
		 *            the name of the dataset schema to use by default.
		 */
		public PlaceHolder(final String dsSchemaName) {
			super(dsSchemaName, Resources.get("placeholderActionTableName"));
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

		/**
		 * Construct an action which creates a new table in the specified schema
		 * based on the union of all the target tables in all the target
		 * schemas.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param unionTableSchema
		 *            the schema in which to create the union table.
		 * @param unionTableName
		 *            the name of the union table to create.
		 * @param targetTableSchemas
		 *            the schemas from which to obtain the tables to perform a
		 *            union on (in a 1:1 correlation with targetTableNames).
		 *            Contents must be {@link Schema} objects.
		 * @param targetTableNames
		 *            the tables to union (in a 1:1 correlation with
		 *            targetTableSchemas). Contents must be strings.
		 */
		public Union(final String dsSchemaName, final String dsTableName,
				final Schema unionTableSchema, final String unionTableName,
				final List targetTableSchemas, final List targetTableNames) {
			super(dsSchemaName, dsTableName);
			this.unionTableSchema = unionTableSchema;
			this.unionTableName = unionTableName;
			this.targetTableSchemas = targetTableSchemas;
			this.targetTableNames = targetTableNames;
		}

		public List getTargetTableNames() {
			return this.targetTableNames;
		}

		public List getTargetTableSchemas() {
			return this.targetTableSchemas;
		}

		public String getUnionTableName() {
			return this.unionTableName;
		}

		public Schema getUnionTableSchema() {
			return this.unionTableSchema;
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

		/**
		 * Drops a table.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param dropTableSchema
		 *            the schema to drop the table from.
		 * @param dropTableName
		 *            the name of the table to drop.
		 */
		public Drop(final String dsSchemaName, final String dsTableName,
				final Schema dropTableSchema, final String dropTableName) {
			super(dsSchemaName, dsTableName);
			this.dropTableSchema = dropTableSchema;
			this.dropTableName = dropTableName;
		}

		public String getDropTableName() {
			return this.dropTableName;
		}

		public Schema getDropTableSchema() {
			return this.dropTableSchema;
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
		private Schema partitionTableSchema;

		private String partitionTableName;

		private Schema targetTableSchema;

		private String targetTableName;

		private String partitionColumnName;

		private Object partitionColumnValue;

		/**
		 * Partitions a table. The partition table and schema define the table
		 * which will be partitioned. The target table and schema define the
		 * table which will be created as a result of the partitioning. The
		 * partition column name and value define the column from which to
		 * restrict values for the partitioning, and the value to restrict that
		 * column by.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema from which to select data.
		 * @param targetTableName
		 *            the table from which to select data.
		 * @param partitionTableSchema
		 *            the schema in which the newly partitioned table will be
		 *            created.
		 * @param partitionTableName
		 *            the name of the newly partitioned table to create.
		 * @param partitionColumnName
		 *            the name of the column to use for the partitioning.
		 * @param partitionColumnValue
		 *            the value to restrict the partitioned column by.
		 */
		public Partition(final String dsSchemaName, final String dsTableName,
				final Schema partitionTableSchema,
				final String partitionTableName,
				final Schema targetTableSchema, final String targetTableName,
				final String partitionColumnName,
				final Object partitionColumnValue) {
			super(dsSchemaName, dsTableName);
			this.partitionTableSchema = partitionTableSchema;
			this.partitionTableName = partitionTableName;
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.partitionColumnName = partitionColumnName;
			this.partitionColumnValue = partitionColumnValue;
		}

		public String getPartitionColumnName() {
			return this.partitionColumnName;
		}

		public Object getPartitionColumnValue() {
			return this.partitionColumnValue;
		}

		public String getTargetTableName() {
			return this.targetTableName;
		}

		public Schema getTargetTableSchema() {
			return this.targetTableSchema;
		}

		public String getPartitionTableName() {
			return this.partitionTableName;
		}

		public Schema getPartitionTableSchema() {
			return this.partitionTableSchema;
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

		/**
		 * Creates a primary key.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param pkTableSchema
		 *            the schema holding the table to create the key for.
		 * @param pkTableName
		 *            the table to create the key for.
		 * @param pkColumns
		 *            the column names to include in the key. Items in this list
		 *            are {@link Column} instances.
		 */
		public PK(final String dsSchemaName, final String dsTableName,
				final Schema pkTableSchema, final String pkTableName,
				final List pkColumns) {
			super(dsSchemaName, dsTableName);
			this.pkTableSchema = pkTableSchema;
			this.pkTableName = pkTableName;
			this.pkColumns = pkColumns;
		}

		public List getPkColumns() {
			return this.pkColumns;
		}

		public String getPkTableName() {
			return this.pkTableName;
		}

		public Schema getPkTableSchema() {
			return this.pkTableSchema;
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

		/**
		 * Creates an index.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param indexTableSchema
		 *            the schema holding the table to create the index for.
		 * @param indexTableName
		 *            the table to create the index for.
		 * @param indexColumns
		 *            the column names to include in the index . Items in this
		 *            list are either {@link Column} instances, or plain
		 *            strings, or a mix of both.
		 */
		public Index(final String dsSchemaName, final String dsTableName,
				final Schema indexTableSchema, final String indexTableName,
				final List indexColumns) {
			super(dsSchemaName, dsTableName);
			this.indexTableSchema = indexTableSchema;
			this.indexTableName = indexTableName;
			this.indexColumns = indexColumns;
		}

		public List getIndexColumns() {
			return this.indexColumns;
		}

		public String getIndexTableName() {
			return this.indexTableName;
		}

		public Schema getIndexTableSchema() {
			return this.indexTableSchema;
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

		/**
		 * Establishes a foreign key.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param fkTableSchema
		 *            the schema holding the table that will have the key.
		 * @param fkTableName
		 *            the table that will have the key.
		 * @param fkColumns
		 *            the columns to include in that key. Items in this list are
		 *            {@link Column} instances.
		 * @param pkTableSchema
		 *            the schema in which the table whose primary key this
		 *            foreign key refers to lives.
		 * @param pkTableName
		 *            the table whose primary key this foreign key refers to.
		 * @param pkColumns
		 *            the columns in the primary key that this foreign key
		 *            refers to. Items in this list are {@link Column}
		 *            instances.
		 */
		public FK(final String dsSchemaName, final String dsTableName,
				final Schema fkTableSchema, final String fkTableName,
				final List fkColumns, final Schema pkTableSchema,
				final String pkTableName, final List pkColumns) {
			super(dsSchemaName, dsTableName);
			this.fkTableSchema = fkTableSchema;
			this.fkTableName = fkTableName;
			this.fkColumns = fkColumns;
			this.pkTableSchema = pkTableSchema;
			this.pkTableName = pkTableName;
			this.pkColumns = pkColumns;
		}

		public List getPkColumns() {
			return this.pkColumns;
		}

		public String getPkTableName() {
			return this.pkTableName;
		}

		public Schema getPkTableSchema() {
			return this.pkTableSchema;
		}

		public List getFkColumns() {
			return this.fkColumns;
		}

		public String getFkTableName() {
			return this.fkTableName;
		}

		public Schema getFkTableSchema() {
			return this.fkTableSchema;
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

		/**
		 * Renames a table.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param renameTableSchema
		 *            the schema in which the table lives.
		 * @param renameTableOldName
		 *            the old name of the table.
		 * @param renameTableNewName
		 *            the new name of the table.
		 */
		public Rename(final String dsSchemaName, final String dsTableName,
				final Schema renameTableSchema,
				final String renameTableOldName, final String renameTableNewName) {
			super(dsSchemaName, dsTableName);
			this.renameTableSchema = renameTableSchema;
			this.renameTableOldName = renameTableOldName;
			this.renameTableNewName = renameTableNewName;
		}

		public String getRenameTableNewName() {
			return this.renameTableNewName;
		}

		public String getRenameTableOldName() {
			return this.renameTableOldName;
		}

		public Schema getRenameTableSchema() {
			return this.renameTableSchema;
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

		private boolean useDistinct;

		private DataSetTableRestriction tableRestriction;

		private boolean useAliases;

		/**
		 * Creates a new table by selecting from an existing table.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param newTableSchema
		 *            the schema to create the table in.
		 * @param newTableName
		 *            the name of the new table to create.
		 * @param selectFromTableSchema
		 *            the schema to select data for the table from.
		 * @param selectFromTableName
		 *            the table to select data from.
		 * @param selectFromColumns
		 *            the columns to select data from. Items in this list are
		 *            {@link Column} instances.
		 * @param useDistinct
		 *            if <tt>true</tt>, then a select distinct is performed.
		 * @param tableRestriction
		 *            any restrictions to place upon the selection from the
		 *            table.
		 * @param useAliases
		 *            if <tt>true</tt>, then the items in selectFromColumns
		 *            are expected to be {@link WrappedColumn} or
		 *            {@link SchemaNameColumn} instances, and the names used are
		 *            the names of the real {@link Column} instances which these
		 *            columns wrap. Otherwise, the names used are those returned
		 *            by the {@link Column#getName()} function on each column in
		 *            the list.
		 */
		public Create(final String dsSchemaName, final String dsTableName,
				final Schema newTableSchema, final String newTableName,
				final Schema selectFromTableSchema,
				final String selectFromTableName, final List selectFromColumns,
				final boolean useDistinct,
				final DataSetTableRestriction tableRestriction,
				final boolean useAliases) {
			super(dsSchemaName, dsTableName);
			this.newTableSchema = newTableSchema;
			this.newTableName = newTableName;
			this.selectFromTableSchema = selectFromTableSchema;
			this.selectFromTableName = selectFromTableName;
			this.selectFromColumns = selectFromColumns;
			this.useDistinct = useDistinct;
			this.tableRestriction = tableRestriction;
			this.useAliases = useAliases;
		}

		public String getNewTableName() {
			return this.newTableName;
		}

		public Schema getNewTableSchema() {
			return this.newTableSchema;
		}

		public List getSelectFromColumns() {
			return this.selectFromColumns;
		}

		public String getSelectFromTableName() {
			return this.selectFromTableName;
		}

		public Schema getSelectFromTableSchema() {
			return this.selectFromTableSchema;
		}

		public boolean isUseDistinct() {
			return this.useDistinct;
		}

		public DataSetTableRestriction getTableRestriction() {
			return this.tableRestriction;
		}

		public boolean isUseAliases() {
			return this.useAliases;
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

		/**
		 * Adds a column for the purposes of optimisation (a 'has' column).
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param tableSchema
		 *            the schema the table lives in.
		 * @param tableName
		 *            the name of the table to add the column to.
		 * @param columnName
		 *            the name of the column to add.
		 */
		public OptimiseAddColumn(final String dsSchemaName,
				final String dsTableName, final Schema tableSchema,
				final String tableName, final String columnName) {
			super(dsSchemaName, dsTableName);
			this.tableSchema = tableSchema;
			this.tableName = tableName;
			this.columnName = columnName;
		}

		public String getColumnName() {
			return this.columnName;
		}

		public String getTableName() {
			return this.tableName;
		}

		public Schema getTableSchema() {
			return this.tableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcOptimiseAdd");
		}
	}

	/**
	 * This class performs an in-place update that changes the value of every
	 * row in the PK table which has a corresponding row in the FK table.
	 */
	public static class OptimiseUpdateColumn extends MartConstructorAction {
		private Schema fkTableSchema;

		private String fkTableName;

		private List fkColumns;

		private Schema pkTableSchema;

		private String pkTableName;

		private List pkColumns;

		private String optimiseColumnName;

		/**
		 * Updates an optimisation column ('has' column) based on whether or not
		 * rows exist in the fkTable that match the rows in the pkTable.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param fkTableSchema
		 *            the schema the fkTable lives in.
		 * @param fkTableName
		 *            the name of the fkTable. This table is the one which
		 *            contains child rows to the parent pkTable.
		 * @param fkColumns
		 *            the columns of the foreign key. Items are {@link Column}
		 *            instances.
		 * @param pkTableSchema
		 *            the schema the pkTable lives in.
		 * @param pkTableName
		 *            the name of the pkTable. This is the one that has the
		 *            optimiser column attached which needs updating based on
		 *            the presence or absence of corresponding fkTable entries.
		 * @param pkColumns
		 *            the columns of the primary key. Items are {@link Column}
		 *            instances.
		 * @param optimiseColumnName
		 *            the name of the optimiser column which needs to be
		 *            updated.
		 */
		public OptimiseUpdateColumn(final String dsSchemaName,
				final String dsTableName, final Schema fkTableSchema,
				final String fkTableName, final List fkColumns,
				final Schema pkTableSchema, final String pkTableName,
				final List pkColumns, final String optimiseColumnName) {
			super(dsSchemaName, dsTableName);
			this.fkTableSchema = fkTableSchema;
			this.fkTableName = fkTableName;
			this.fkColumns = fkColumns;
			this.pkTableSchema = pkTableSchema;
			this.pkTableName = pkTableName;
			this.pkColumns = pkColumns;
			this.optimiseColumnName = optimiseColumnName;
		}

		public List getFkColumns() {
			return this.fkColumns;
		}

		public String getFkTableName() {
			return this.fkTableName;
		}

		public Schema getFkTableSchema() {
			return this.fkTableSchema;
		}

		public List getPkColumns() {
			return this.pkColumns;
		}

		public String getPkTableName() {
			return this.pkTableName;
		}

		public Schema getPkTableSchema() {
			return this.pkTableSchema;
		}

		public String getOptimiseColumnName() {
			return this.optimiseColumnName;
		}

		public String getStatusMessage() {
			return Resources.get("mcOptimiseUpdate");
		}
	}

	/**
	 * This action creates a new table containing the selected columns from the
	 * old table plus all the expression columns, optionally grouping if any of
	 * them are group-by expression columns.
	 */
	public static class ExpressionAddColumns extends MartConstructorAction {
		private Schema sourceTableSchema;

		private String sourceTableName;

		private Schema targetTableSchema;

		private String targetTableName;

		private List sourceTableColumns;

		private List expressionColumns;

		private boolean useGroupBy;

		/**
		 * Adds a set of expressions to the table by creating a new table with
		 * all the old table plus the columns for the expressions.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param sourceTableSchema
		 *            the schema containing the table to select data from.
		 * @param sourceTableName
		 *            the table to select data from.
		 * @param targetTableSchema
		 *            the schema to create the new table in.
		 * @param targetTableName
		 *            the name of the new table.
		 * @param sourceTableColumns
		 *            a list of {@link Column} instances describing which of the
		 *            columns from the source table should be kept in the new
		 *            table after the expressions are added.
		 * @param expressionColumns
		 *            the list of {@link ExpressionColumn} instances describing
		 *            the expressions to add.
		 * @param useGroupBy
		 *            <tt>true</tt> if a group by clause should be included.
		 *            The group by clause will include all columns in
		 *            sourceTableColumns.
		 */
		public ExpressionAddColumns(final String dsSchemaName,
				final String dsTableName, final Schema sourceTableSchema,
				final String sourceTableName, final Schema targetTableSchema,
				final String targetTableName, final List sourceTableColumns,
				final List expressionColumns, final boolean useGroupBy) {
			super(dsSchemaName, dsTableName);
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.sourceTableColumns = sourceTableColumns;
			this.expressionColumns = expressionColumns;
			this.useGroupBy = useGroupBy;
		}

		public List getExpressionColumns() {
			return this.expressionColumns;
		}

		public List getSourceTableColumns() {
			return this.sourceTableColumns;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public String getTargetTableName() {
			return this.targetTableName;
		}

		public Schema getTargetTableSchema() {
			return this.targetTableSchema;
		}

		public boolean getUseGroupBy() {
			return this.useGroupBy;
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

		private DataSetRelationRestriction relationRestriction;

		private boolean firstTableSourceTable;

		private boolean useDistinct;

		private DataSetTableRestriction targetTableRestriction;

		private boolean useAliases;

		/**
		 * Creates a new table by merging two existing ones together.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param mergedTableSchema
		 *            the schema to create the merged table in.
		 * @param mergedTableName
		 *            the name to give the merged table.
		 * @param sourceTableSchema
		 *            the schema the LHS of the join lives in.
		 * @param sourceTableName
		 *            the table on the LHS of the join.
		 * @param sourceTableKeyColumns
		 *            the columns on the LHS to use to make the join.
		 * @param useLeftJoin
		 *            <tt>true</tt> if the join should be a left join,
		 *            <tt>false</tt> for an inner join.
		 * @param targetTableSchema
		 *            the schema the RHS of the join lives in.
		 * @param targetTableName
		 *            the table on the RHS of the join.
		 * @param targetTableKeyColumns
		 *            the columns on the RHS to use to make the join.
		 * @param targetTableColumns
		 *            the columns to select from the RHS of the join.
		 * @param relationRestriction
		 *            a restriction to place on the join.
		 * @param firstTableSourceTable
		 *            <tt>true</tt> if the first table of the relation
		 *            restriction is the LHS side, <tt>false</tt> if it is the
		 *            RHS side.
		 * @param useDistinct
		 *            <tt>true</tt> if a select distinct should be used.
		 * @param targetTableRestriction
		 *            a restriction to place on the RHS table.
		 * @param useAliases
		 *            if <tt>true</tt>, then the items in selectFromColumns
		 *            are expected to be {@link WrappedColumn} or
		 *            {@link SchemaNameColumn} instances, and the names used are
		 *            the names of the real {@link Column} instances which these
		 *            columns wrap. Otherwise, the names used are those returned
		 *            by the {@link Column#getName()} function on each column in
		 *            the list.
		 */
		public Merge(final String dsSchemaName, final String dsTableName,
				final Schema mergedTableSchema, final String mergedTableName,
				final Schema sourceTableSchema, final String sourceTableName,
				final List sourceTableKeyColumns, final boolean useLeftJoin,
				final Schema targetTableSchema, final String targetTableName,
				final List targetTableKeyColumns,
				final List targetTableColumns,
				final DataSetRelationRestriction relationRestriction,
				final boolean firstTableSourceTable, final boolean useDistinct,
				final DataSetTableRestriction targetTableRestriction,
				final boolean useAliases) {
			super(dsSchemaName, dsTableName);
			this.mergedTableSchema = mergedTableSchema;
			this.mergedTableName = mergedTableName;
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTableKeyColumns = sourceTableKeyColumns;
			this.useLeftJoin = relationRestriction != null ? true : useLeftJoin;
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.targetTableKeyColumns = targetTableKeyColumns;
			this.targetTableColumns = targetTableColumns;
			this.relationRestriction = relationRestriction;
			this.firstTableSourceTable = firstTableSourceTable;
			this.useDistinct = useDistinct;
			this.targetTableRestriction = targetTableRestriction;
			this.useAliases = useAliases;
		}

		public String getMergedTableName() {
			return this.mergedTableName;
		}

		public Schema getMergedTableSchema() {
			return this.mergedTableSchema;
		}

		public List getSourceTableKeyColumns() {
			return this.sourceTableKeyColumns;
		}
		
		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public List getTargetTableColumns() {
			return this.targetTableColumns;
		}

		public List getTargetTableKeyColumns() {
			return this.targetTableKeyColumns;
		}

		public String getTargetTableName() {
			return this.targetTableName;
		}

		public Schema getTargetTableSchema() {
			return this.targetTableSchema;
		}

		public DataSetRelationRestriction getRelationRestriction() {
			return this.relationRestriction;
		}

		public boolean isUseAliases() {
			return this.useAliases;
		}

		public boolean isUseLeftJoin() {
			return this.useLeftJoin;
		}

		public boolean isFirstTableSourceTable() {
			return this.firstTableSourceTable;
		}

		public boolean isUseDistinct() {
			return this.useDistinct;
		}

		public DataSetTableRestriction getTargetTableRestriction() {
			return this.targetTableRestriction;
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

		private String concatColumnName;

		private ConcatRelationType concatRelationType;

		private DataSetRelationRestriction relationRestriction;

		private boolean firstTableSourceTable;

		private DataSetTableRestriction targetTableRestriction;

		/**
		 * Creates a new table by concatting together all rows in the target
		 * table which match each entry in the source table.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param concatTableSchema
		 *            the schema to create the new table in.
		 * @param concatTableName
		 *            the name of the table to create.
		 * @param sourceTableSchema
		 *            the schema on the LHS side of the join.
		 * @param sourceTableName
		 *            the table on the LHS side of the join.
		 * @param sourceTableKeyColumns
		 *            the columns to use to make the LHS side of the join.
		 * @param targetTableSchema
		 *            the schema on the RHS (concat) side of the join.
		 * @param targetTableName
		 *            the name of the table on the RHS (concat) side.
		 * @param targetTableKeyColumns
		 *            the columns to join on on the RHS (concat) side.
		 * @param targetTableConcatColumns
		 *            the columns to concatenate together from the RHS (concat)
		 *            side.
		 * @param concatColumnName
		 *            the name of the column which will hold the concatenated
		 *            values.
		 * @param concatRelationType
		 *            the type of concatenation to perform.
		 * @param relationRestriction
		 *            a restriction to place on the join.
		 * @param firstTableSourceTable
		 *            <tt>true</tt> if the first table of the relation
		 *            restriction is the LHS side, <tt>false</tt> if it is the
		 *            RHS side.
		 * @param targetTableRestriction
		 *            a restriction to place on the RHS table.
		 */
		public Concat(final String dsSchemaName, final String dsTableName,
				final Schema concatTableSchema, final String concatTableName,
				final Schema sourceTableSchema, final String sourceTableName,
				final List sourceTableKeyColumns,
				final Schema targetTableSchema, final String targetTableName,
				final List targetTableKeyColumns,
				final List targetTableConcatColumns,
				final String concatColumnName,
				final ConcatRelationType concatRelationType,
				final DataSetRelationRestriction relationRestriction,
				final boolean firstTableSourceTable,
				final DataSetTableRestriction targetTableRestriction) {
			super(dsSchemaName, dsTableName);
			this.concatTableSchema = concatTableSchema;
			this.concatTableName = concatTableName;
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTableKeyColumns = sourceTableKeyColumns;
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
			this.targetTableKeyColumns = targetTableKeyColumns;
			this.targetTableConcatColumns = targetTableConcatColumns;
			this.concatColumnName = concatColumnName;
			this.concatRelationType = concatRelationType;
			this.relationRestriction = relationRestriction;
			this.firstTableSourceTable = firstTableSourceTable;
			this.targetTableRestriction = targetTableRestriction;
		}

		public String getConcatTableName() {
			return this.concatTableName;
		}

		public Schema getConcatTableSchema() {
			return this.concatTableSchema;
		}

		public List getSourceTableKeyColumns() {
			return this.sourceTableKeyColumns;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public List getTargetTableConcatColumns() {
			return this.targetTableConcatColumns;
		}

		public List getTargetTableKeyColumns() {
			return this.targetTableKeyColumns;
		}

		public String getTargetTableName() {
			return this.targetTableName;
		}

		public Schema getTargetTableSchema() {
			return this.targetTableSchema;
		}

		public String getConcatColumnName() {
			return this.concatColumnName;
		}

		public ConcatRelationType getConcatRelationType() {
			return this.concatRelationType;
		}

		public DataSetRelationRestriction getRelationRestriction() {
			return this.relationRestriction;
		}

		public boolean isFirstTableSourceTable() {
			return this.firstTableSourceTable;
		}

		public DataSetTableRestriction getTargetTableRestriction() {
			return this.targetTableRestriction;
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

		/**
		 * Creates a new table that contains everything from the old table which
		 * is associated with some other table. The join is an inner join.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param reducedTableSchema
		 *            the schema to create the new table in.
		 * @param reducedTableName
		 *            the name of the new table to create.
		 * @param sourceTableSchema
		 *            the schema on the LHS of the join.
		 * @param sourceTableName
		 *            the table on the LHS of the join.
		 * @param sourceTableKeyColumns
		 *            the columns to use as a key on the LHS side. Items in this
		 *            list are {@link Column} instances.
		 * @param targetTableSchema
		 *            the schema on the RHS of the join.
		 * @param targetTableName
		 *            the table from which data will be selected on the RHS
		 *            side. Only rows where the targetTableKeyColumns match the
		 *            sourceTableKeyColumns will be included.
		 * @param targetTableKeyColumns
		 *            the columns to use as a key on the RHS side. Items in this
		 *            list are {@link Column} instances.
		 */
		public Reduce(final String datasetSchemaName,
				final String datasetTableName, final Schema reducedTableSchema,
				final String reducedTableName, final Schema sourceTableSchema,
				final String sourceTableName, final List sourceTableKeyColumns,
				final Schema targetTableSchema, final String targetTableName,
				final List targetTableKeyColumns) {
			super(datasetSchemaName, datasetTableName);
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
			return this.reducedTableName;
		}

		public Schema getReducedTableSchema() {
			return this.reducedTableSchema;
		}

		public List getSourceTableKeyColumns() {
			return this.sourceTableKeyColumns;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public List getTargetTableKeyColumns() {
			return this.targetTableKeyColumns;
		}

		public String getTargetTableName() {
			return this.targetTableName;
		}

		public Schema getTargetTableSchema() {
			return this.targetTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcReduce");
		}
	}
}
