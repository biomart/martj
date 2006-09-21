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

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.model.DataSet.DataSetTableRestriction;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
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
 * @version 0.1.14, 21st September 2006
 * @since 0.1
 */
public abstract class MartConstructorAction {
	private static int nextSequence = 0;

	private static final String nextSequenceLock = "__SEQ_LOCK";

	private Set children;

	private String datasetSchemaName;

	private String datasetTableName;

	private int depth;

	private boolean interim;

	private Set parents;

	private int sequence;

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
		this.interim = false;
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

	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		else
			return obj != null && obj instanceof MartConstructorAction
					&& this.sequence == ((MartConstructorAction) obj).sequence;
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
	 * Returns the current depth (level) of the graph at which this action lies.
	 * 
	 * @return the current depth, 0-indexed.
	 */
	public int getDepth() {
		return this.depth;
	}

	/**
	 * Returns <tt>true</tt> if the temp table created by this step is
	 * depended on by the first steps of other dataset tables.
	 * 
	 * @return <tt>true</tt> if the temp table is a dependency for other
	 *         tables, <tt>false</tt> if not.
	 */
	public boolean getInterim() {
		return this.interim;
	}

	/**
	 * Returns the parents of this node.
	 * 
	 * @return the parents of this node.
	 */
	public Collection getParents() {
		return this.parents;
	}

	/**
	 * Returns the order in which this action was created.
	 * 
	 * @return the order in which this action was created. 0 is the first.
	 */
	public int getSequence() {
		return this.sequence;
	}

	/**
	 * Override this method to produce a message describing what this node of
	 * the graph will do.
	 * 
	 * @return a description of what this node will do.
	 */
	public abstract String getStatusMessage();

	public int hashCode() {
		return this.sequence;
	}

	/**
	 * Set to <tt>true</tt> if the temp table created by this step is depended
	 * on by the first steps of other dataset tables.
	 * 
	 * @param interim
	 *            <tt>true</tt> if the temp table is a dependency for other
	 *            tables, <tt>false</tt> if not.
	 */
	public void setInterim(boolean interim) {
		this.interim = interim;
	}

	/**
	 * This action creates a new table based on the concatenated columns of a
	 * relation between two existing tables. If the useAliases flag is turned
	 * on, it expects instances of {@link DataSetColumn} and will select columns
	 * and alias them. If not, then it expects {@link Column} instances and will
	 * select columns by name alone.
	 */
	public static class Concat extends MartConstructorTableAction {

		private String columnSeparator;

		private DataSetRelationRestriction concatRelationRestriction;

		private List concatTableConcatColumns;

		private List concatTableFKColumns;

		private String concatTableName;

		private DataSetTableRestriction concatTableRestriction;

		private Schema concatTableSchema;

		private boolean firstTableSourceTable;

		private String recordSeparator;

		private String sourceTableName;

		private List sourceTablePKColumns;

		private Schema sourceTableSchema;

		private String targetTableConcatColumnName;

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
		 * @param concatTableFKColumns
		 *            the columns to join on on the RHS (concat) side.
		 * @param concatTableConcatColumns
		 *            the columns to concatenate together from the RHS (concat)
		 *            side.
		 * @param targetTableConcatColumnName
		 *            the name of the column which will hold the concatenated
		 *            values.
		 * @param columnSeparator
		 *            the separator to use between columns.
		 * @param recordSeparator
		 *            the separator to use between records.
		 * @param concatRelationRestriction
		 *            a restriction to place on the join.
		 * @param firstTableSourceTable
		 *            <tt>true</tt> if the first table of the relation
		 *            restriction is the LHS side, <tt>false</tt> if it is the
		 *            RHS side.
		 * @param concatTableRestriction
		 *            a restriction to place on the RHS table.
		 */
		public Concat(final String dsSchemaName, final String dsTableName,
				final Schema targetTableSchema, final String targetTableName,
				final Schema sourceTableSchema, final String sourceTableName,
				final List sourceTableKeyColumns,
				final Schema concatTableSchema, final String concatTableName,
				final List concatTableFKColumns,
				final List concatTableConcatColumns,
				final String targetTableConcatColumnName,
				final String columnSeparator, final String recordSeparator,
				final DataSetRelationRestriction concatRelationRestriction,
				final boolean firstTableSourceTable,
				final DataSetTableRestriction concatTableRestriction) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTablePKColumns = sourceTableKeyColumns;
			this.concatTableSchema = concatTableSchema;
			this.concatTableName = concatTableName;
			this.concatTableFKColumns = concatTableFKColumns;
			this.concatTableConcatColumns = concatTableConcatColumns;
			this.targetTableConcatColumnName = targetTableConcatColumnName;
			this.columnSeparator = columnSeparator;
			this.recordSeparator = recordSeparator;
			this.concatRelationRestriction = concatRelationRestriction;
			this.firstTableSourceTable = firstTableSourceTable;
			this.concatTableRestriction = concatTableRestriction;
		}

		public String getColumnSeparator() {
			return this.columnSeparator;
		}

		public DataSetRelationRestriction getConcatRelationRestriction() {
			return this.concatRelationRestriction;
		}

		public List getConcatTableConcatColumns() {
			return this.concatTableConcatColumns;
		}

		public List getConcatTableFKColumns() {
			return this.concatTableFKColumns;
		}

		public String getConcatTableName() {
			return this.concatTableName;
		}

		public DataSetTableRestriction getConcatTableRestriction() {
			return this.concatTableRestriction;
		}

		public Schema getConcatTableSchema() {
			return this.concatTableSchema;
		}

		public String getRecordSeparator() {
			return this.recordSeparator;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public List getSourceTablePKColumns() {
			return this.sourceTablePKColumns;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcConcat");
		}

		public String getTargetTableConcatColumnName() {
			return this.targetTableConcatColumnName;
		}

		public boolean isFirstTableSourceTable() {
			return this.firstTableSourceTable;
		}
	}

	/**
	 * Creates tables based on selected columns. If the useAliases flag is
	 * turned on, it expects instances of {@link DataSetColumn} and will select
	 * columns and alias them. If not, then it expects {@link Column} instances
	 * and will select columns by name alone.
	 */
	public static class Create extends MartConstructorTableAction {

		private List selectFromColumns;

		private String sourceTableName;

		private DataSetTableRestriction sourceTableRestriction;

		private Schema sourceTableSchema;

		private boolean useAliases;

		private boolean useDistinct;

		private boolean useInheritedAliases;

		/**
		 * Creates a new table by selecting from an existing table.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema to create the table in.
		 * @param targetTableName
		 *            the name of the new table to create.
		 * @param sourceTableSchema
		 *            the schema to select data for the table from.
		 * @param sourceTableName
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
		 * @param useInheritedAliases
		 *            if <tt>true</tt>, then any {@link InheritedColumn}
		 *            items selectFromColumns will be aliased to select the
		 *            referenced column aliased to the inherited column name.
		 *            Otherwise, the name of the referenced column is used
		 *            verbatim.
		 */
		public Create(final String dsSchemaName, final String dsTableName,
				final Schema targetTableSchema, final String targetTableName,
				final Schema sourceTableSchema, final String sourceTableName,
				final List selectFromColumns, final boolean useDistinct,
				final DataSetTableRestriction tableRestriction,
				final boolean useAliases, final boolean useInheritedAliases) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.selectFromColumns = selectFromColumns;
			this.useDistinct = useDistinct;
			this.sourceTableRestriction = tableRestriction;
			this.useAliases = useAliases;
			this.useInheritedAliases = useInheritedAliases;
		}

		public List getSelectFromColumns() {
			return this.selectFromColumns;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public DataSetTableRestriction getSourceTableRestriction() {
			return this.sourceTableRestriction;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcCreate");
		}

		public boolean isUseAliases() {
			return this.useAliases;
		}

		public boolean isUseDistinct() {
			return this.useDistinct;
		}

		public boolean isUseInheritedAliases() {
			return this.useInheritedAliases;
		}
	}

	/**
	 * Drop actions drop tables.
	 */
	public static class Drop extends MartConstructorTableAction {

		/**
		 * Drops a table.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema to drop the table from.
		 * @param targetTableName
		 *            the name of the table to drop.
		 */
		public Drop(final String dsSchemaName, final String dsTableName,
				final Schema targetTableSchema, final String targetTableName) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
		}

		public String getStatusMessage() {
			return Resources.get("mcDrop");
		}
	}

	/**
	 * This action creates a new table containing the selected columns from the
	 * old table plus all the expression columns, optionally grouping if any of
	 * them are group-by expression columns.
	 */
	public static class ExpressionAddColumns extends MartConstructorTableAction {
		private List sourceTableColumns;

		private String sourceTableName;

		private Schema sourceTableSchema;

		private List targetExpressionColumns;

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
		 * @param targetExpressionColumns
		 *            the list of {@link ExpressionColumn} instances describing
		 *            the expressions to add.
		 * @param useGroupBy
		 *            <tt>true</tt> if a group by clause should be included.
		 *            The group by clause will include all columns in
		 *            sourceTableColumns.
		 */
		public ExpressionAddColumns(final String dsSchemaName,
				final String dsTableName, final Schema targetTableSchema,
				final String targetTableName, final Schema sourceTableSchema,
				final String sourceTableName, final List sourceTableColumns,
				final List targetExpressionColumns, final boolean useGroupBy) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTableColumns = sourceTableColumns;
			this.targetExpressionColumns = targetExpressionColumns;
			this.useGroupBy = useGroupBy;
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

		public String getStatusMessage() {
			return Resources.get("mcExpressionAdd");
		}

		public List getTargetExpressionColumns() {
			return this.targetExpressionColumns;
		}

		public boolean getUseGroupBy() {
			return this.useGroupBy;
		}
	}

	/**
	 * Index actions create indexes over the given set of columns on the given
	 * table.
	 */
	public static class Index extends MartConstructorTableAction {

		private List indexColumns;

		/**
		 * Creates an index.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema holding the table to create the index for.
		 * @param targetTableName
		 *            the table to create the index for.
		 * @param indexColumns
		 *            the column names to include in the index . Items in this
		 *            list are either {@link Column} instances, or plain
		 *            strings, or a mix of both.
		 */
		public Index(final String dsSchemaName, final String dsTableName,
				final Schema targetTableSchema, final String targetTableName,
				final List indexColumns) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.indexColumns = indexColumns;
		}

		public List getIndexColumns() {
			return this.indexColumns;
		}

		public String getStatusMessage() {
			return Resources.get("mcIndex");
		}
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
	 * This subclass represents all actions which modify a target table.
	 */
	public static abstract class MartConstructorTableAction extends
			MartConstructorAction {

		private String targetTableName;

		private Schema targetTableSchema;

		/**
		 * Sets up a node.
		 * 
		 * @param datasetSchemaName
		 *            the name of the schema within which the dataset will be
		 *            constructed. Wherever other schemas in actions are
		 *            specified as null, this schema will be used in place.
		 * @param datasetTableName
		 *            the name of the table with which this action is associated
		 *            as part of the transformation process.
		 * @param targetTableSchema
		 *            the schema in which the target table will be created.
		 * @param targetTableName
		 *            the name of the target table.
		 */
		public MartConstructorTableAction(final String datasetSchemaName,
				final String datasetTableName, final Schema targetTableSchema,
				final String targetTableName) {
			super(datasetSchemaName, datasetTableName);
			this.targetTableSchema = targetTableSchema;
			this.targetTableName = targetTableName;
		}

		public abstract String getStatusMessage();

		public String getTargetTableName() {
			return this.targetTableName;
		}

		public Schema getTargetTableSchema() {
			return this.targetTableSchema;
		}
	}

	/**
	 * This action creates a new table based on the merge between two existing
	 * tables. If the useAliases flag is turned on, it expects instances of
	 * {@link DataSetColumn} and will select columns and alias them. If not,
	 * then it expects {@link Column} instances and will select columns by name
	 * alone.
	 */
	public static class Merge extends MartConstructorTableAction {

		private boolean firstTableSourceTable;

		private DataSetRelationRestriction mergeRelationRestriction;

		private List mergeTableJoinColumns;

		private String mergeTableName;

		private Schema mergeTableSchema;

		private List mergeTableSelectColumns;

		private List sourceTableJoinColumns;

		private String sourceTableName;

		private Schema sourceTableSchema;

		private DataSetTableRestriction targetTableRestriction;

		private boolean useAliases;

		private boolean useDistinct;

		/**
		 * Creates a new table by merging two existing ones together. The merge
		 * should be done using left joins.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema to create the merged table in.
		 * @param targetTableName
		 *            the name to give the merged table.
		 * @param sourceTableSchema
		 *            the schema the LHS of the join lives in.
		 * @param sourceTableName
		 *            the table on the LHS of the join.
		 * @param sourceTableJoinColumns
		 *            the columns on the LHS to use to make the join.
		 * @param mergeTableSchema
		 *            the schema the RHS of the join lives in.
		 * @param mergeTableName
		 *            the table on the RHS of the join.
		 * @param mergeTableJoinColumns
		 *            the columns on the RHS to use to make the join.
		 * @param mergeTableSelectColumns
		 *            the columns to select from the RHS of the join.
		 * @param mergeRelationRestriction
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
				final Schema targetTableSchema, final String targetTableName,
				final Schema sourceTableSchema, final String sourceTableName,
				final List sourceTableJoinColumns,
				final Schema mergeTableSchema, final String mergeTableName,
				final List mergeTableJoinColumns,
				final List mergeTableSelectColumns,
				final DataSetRelationRestriction mergeRelationRestriction,
				final boolean firstTableSourceTable, final boolean useDistinct,
				final DataSetTableRestriction targetTableRestriction,
				final boolean useAliases) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTableJoinColumns = sourceTableJoinColumns;
			this.mergeTableSchema = mergeTableSchema;
			this.mergeTableName = mergeTableName;
			this.mergeTableJoinColumns = mergeTableJoinColumns;
			this.mergeTableSelectColumns = mergeTableSelectColumns;
			this.mergeRelationRestriction = mergeRelationRestriction;
			this.firstTableSourceTable = firstTableSourceTable;
			this.useDistinct = useDistinct;
			this.targetTableRestriction = targetTableRestriction;
			this.useAliases = useAliases;
		}

		public DataSetRelationRestriction getMergeRelationRestriction() {
			return this.mergeRelationRestriction;
		}

		public List getMergeTableJoinColumns() {
			return this.mergeTableJoinColumns;
		}

		public String getMergeTableName() {
			return this.mergeTableName;
		}

		public Schema getMergeTableSchema() {
			return this.mergeTableSchema;
		}

		public List getMergeTableSelectColumns() {
			return this.mergeTableSelectColumns;
		}

		public List getSourceTableJoinColumns() {
			return this.sourceTableJoinColumns;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcMerge");
		}

		public DataSetTableRestriction getTargetTableRestriction() {
			return this.targetTableRestriction;
		}

		public boolean isFirstTableSourceTable() {
			return this.firstTableSourceTable;
		}

		public boolean isUseAliases() {
			return this.useAliases;
		}

		public boolean isUseDistinct() {
			return this.useDistinct;
		}
	}

	/**
	 * This action creates a column on a table ready for use as an optimisation
	 * ('has') column.
	 */
	public static class OptimiseAddColumn extends MartConstructorTableAction {

		private String columnName;

		/**
		 * Adds a column for the purposes of optimisation (a 'has' column).
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema the table lives in.
		 * @param targetTableName
		 *            the name of the table to add the column to.
		 * @param targetColumnName
		 *            the name of the column to add.
		 */
		public OptimiseAddColumn(final String dsSchemaName,
				final String dsTableName, final Schema targetTableSchema,
				final String targetTableName, final String targetColumnName) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.columnName = targetColumnName;
		}

		public String getColumnName() {
			return this.columnName;
		}

		public String getStatusMessage() {
			return Resources.get("mcOptimiseAdd");
		}
	}

	/**
	 * This class performs an in-place update that changes the value of every
	 * row in the PK table which has a corresponding row in the FK table.
	 */
	public static class OptimiseUpdateColumn extends MartConstructorTableAction {
		private List countTableFKColumns;

		private String countTableName;

		private List countTableNotNullColumns;

		private Schema countTableSchema;

		private String optimiseColumnName;

		private List targetTablePKColumns;

		/**
		 * Updates an optimisation column ('has' column) on the pkTable based on
		 * whether or not rows exist in the fkTable that match the rows in the
		 * pkTable.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param countTableSchema
		 *            the schema the fkTable lives in.
		 * @param countTableName
		 *            the name of the fkTable. This table is the one which
		 *            contains child rows to the parent pkTable.
		 * @param countTableFKColumns
		 *            the columns of the foreign key. Items are {@link Column}
		 *            instances.
		 * @param countTableNotNullColumns
		 *            the columns to check for null values. The optimiser counts
		 *            a row as existing in the FK only if all the columns in
		 *            this collection are not null.
		 * @param targetTableSchema
		 *            the schema the pkTable lives in.
		 * @param targetTableName
		 *            the name of the pkTable. This is the one that has the
		 *            optimiser column attached which needs updating based on
		 *            the presence or absence of corresponding fkTable entries.
		 * @param targetTablePKColumns
		 *            the columns of the primary key. Items are {@link Column}
		 *            instances.
		 * @param optimiseColumnName
		 *            the name of the optimiser column which needs to be
		 *            updated.
		 */
		public OptimiseUpdateColumn(final String dsSchemaName,
				final String dsTableName, final Schema targetTableSchema,
				final String targetTableName, final Schema countTableSchema,
				final String countTableName, final List countTableFKColumns,
				List countTableNotNullColumns, final List targetTablePKColumns,
				final String optimiseColumnName) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.countTableSchema = countTableSchema;
			this.countTableName = countTableName;
			this.countTableFKColumns = countTableFKColumns;
			this.countTableNotNullColumns = countTableNotNullColumns;
			this.targetTablePKColumns = targetTablePKColumns;
			this.optimiseColumnName = optimiseColumnName;
		}

		public List getCountTableFKColumns() {
			return this.countTableFKColumns;
		}

		public String getCountTableName() {
			return this.countTableName;
		}

		public List getCountTableNotNullColumns() {
			return this.countTableNotNullColumns;
		}

		public Schema getCountTableSchema() {
			return this.countTableSchema;
		}

		public String getOptimiseColumnName() {
			return this.optimiseColumnName;
		}

		public String getStatusMessage() {
			return Resources.get("mcOptimiseUpdate");
		}

		public List getTargetTablePKColumns() {
			return this.targetTablePKColumns;
		}
	}

	/**
	 * Partition actions create a second table that represents all the rows from
	 * the first table where a particular column has a given value.
	 */
	public static class Partition extends MartConstructorTableAction {
		private String partitionColumnName;

		private Object partitionColumnValue;

		private Collection sourceTableAllColumns;

		private List sourceTableFKColumns;

		private String sourceTableName;

		private List sourceTablePKColumns;

		private Schema sourceTableSchema;

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
		 *            the schema in which the newly partitioned table will be
		 *            created.
		 * @param targetTableName
		 *            the name of the newly partitioned table to create.
		 * @param sourceTableSchema
		 *            the schema from which to select data.
		 * @param sourceTableName
		 *            the table from which to select data.
		 * @param partitionColumnName
		 *            the name of the column to use for the partitioning.
		 * @param partitionColumnValue
		 *            the value to restrict the partitioned column by.
		 * @param sourceTablePKColumns
		 *            the PK columns of the table we are selecting from.
		 * @param sourceTableFKColumns
		 *            the FK columns of the table we are selecting from.
		 * @param sourceTableAllColumns
		 *            all columns on the table we are selecting from.
		 */
		public Partition(final String dsSchemaName, final String dsTableName,
				final Schema targetTableSchema, final String targetTableName,
				final Schema sourceTableSchema, final String sourceTableName,
				final String partitionColumnName,
				final Object partitionColumnValue,
				final List sourceTablePKColumns,
				final List sourceTableFKColumns,
				final Collection sourceTableAllColumns) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.partitionColumnName = partitionColumnName;
			this.partitionColumnValue = partitionColumnValue;
			this.sourceTablePKColumns = sourceTablePKColumns;
			this.sourceTableFKColumns = sourceTableFKColumns;
			this.sourceTableAllColumns = sourceTableAllColumns;
		}

		public String getPartitionColumnName() {
			return this.partitionColumnName;
		}

		public Object getPartitionColumnValue() {
			return this.partitionColumnValue;
		}

		public Collection getSourceTableAllColumns() {
			return this.sourceTableAllColumns;
		}

		public List getSourceTableFKColumns() {
			return this.sourceTableFKColumns;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public List getSourceTablePKColumns() {
			return this.sourceTablePKColumns;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcPartition");
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
	 * This action creates a new table based on all the columns of a target
	 * table when inner joined with some source table.
	 */
	public static class Reduce extends MartConstructorTableAction {

		private Collection reduceTableAllColumns;

		private List reduceTableFKColumns;

		private String reduceTableName;

		private Schema reduceTableSchema;

		private String sourceTableName;

		private List sourceTablePKColumns;

		private Schema sourceTableSchema;

		/**
		 * Creates a new table that contains everything from the old table which
		 * is associated with some other table. The join is an inner join.
		 * 
		 * @param datasetSchemaName
		 *            the dataset schema name to use by default.
		 * @param datasetTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema to create the new table in.
		 * @param targetTableName
		 *            the name of the new table to create.
		 * @param sourceTableSchema
		 *            the schema on the LHS of the join.
		 * @param sourceTableName
		 *            the table on the LHS of the join.
		 * @param sourceTablePKColumns
		 *            the columns to use as a key on the LHS side. Items in this
		 *            list are {@link Column} instances.
		 * @param reduceTableSchema
		 *            the schema on the RHS of the join.
		 * @param reduceTableName
		 *            the table from which data will be selected on the RHS
		 *            side. Only rows where the targetTableKeyColumns match the
		 *            sourceTableKeyColumns will be included.
		 * @param reduceTableFKColumns
		 *            the columns to use as a key on the RHS side. Items in this
		 *            list are {@link Column} instances.
		 * @param reduceTableAllColumns
		 *            all columns in the target table. Items in this list are
		 *            {@link Column} instances.
		 */
		public Reduce(final String datasetSchemaName,
				final String datasetTableName, final Schema targetTableSchema,
				final String targetTableName, final Schema sourceTableSchema,
				final String sourceTableName, final List sourceTablePKColumns,
				final Schema reduceTableSchema, final String reduceTableName,
				final List reduceTableFKColumns,
				final Collection reduceTableAllColumns) {
			super(datasetSchemaName, datasetTableName, targetTableSchema,
					targetTableName);
			this.sourceTableSchema = sourceTableSchema;
			this.sourceTableName = sourceTableName;
			this.sourceTablePKColumns = sourceTablePKColumns;
			this.reduceTableSchema = reduceTableSchema;
			this.reduceTableName = reduceTableName;
			this.reduceTableFKColumns = reduceTableFKColumns;
			this.reduceTableAllColumns = reduceTableAllColumns;
		}

		public Collection getReduceTableAllColumns() {
			return this.reduceTableAllColumns;
		}

		public List getReduceTableFKColumns() {
			return this.reduceTableFKColumns;
		}

		public String getReduceTableName() {
			return this.reduceTableName;
		}

		public Schema getReduceTableSchema() {
			return this.reduceTableSchema;
		}

		public String getSourceTableName() {
			return this.sourceTableName;
		}

		public List getSourceTablePKColumns() {
			return this.sourceTablePKColumns;
		}

		public Schema getSourceTableSchema() {
			return this.sourceTableSchema;
		}

		public String getStatusMessage() {
			return Resources.get("mcReduce");
		}
	}

	/**
	 * Rename table actions rename tables.
	 */
	public static class RenameTable extends MartConstructorTableAction {
		private String targetTableOldName;

		/**
		 * Renames a table.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema in which the table lives.
		 * @param targetTableOldName
		 *            the old name of the table.
		 * @param targetTableName
		 *            the new name of the table.
		 */
		public RenameTable(final String dsSchemaName, final String dsTableName,
				final Schema targetTableSchema, final String targetTableName,
				final String targetTableOldName) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.targetTableOldName = targetTableOldName;
		}

		public String getStatusMessage() {
			return Resources.get("mcRename");
		}

		public String getTargetTableOldName() {
			return this.targetTableOldName;
		}
	}

	/**
	 * Union actions create a new table out of a bunch of others by performing a
	 * union statement over them.
	 */
	public static class Union extends MartConstructorTableAction {

		private List sourceTableNames;

		private List sourceTableSchemas;

		/**
		 * Construct an action which creates a new table in the specified schema
		 * based on the union of all the target tables in all the target
		 * schemas.
		 * 
		 * @param dsSchemaName
		 *            the dataset schema name to use by default.
		 * @param dsTableName
		 *            the dataset table this action is associated with.
		 * @param targetTableSchema
		 *            the schema in which to create the union table.
		 * @param targetTableName
		 *            the name of the union table to create.
		 * @param sourceTableSchemas
		 *            the schemas from which to obtain the tables to perform a
		 *            union on (in a 1:1 correlation with targetTableNames).
		 *            Contents must be {@link Schema} objects.
		 * @param sourceTableNames
		 *            the tables to union (in a 1:1 correlation with
		 *            targetTableSchemas). Contents must be strings.
		 */
		public Union(final String dsSchemaName, final String dsTableName,
				final Schema targetTableSchema, final String targetTableName,
				final List sourceTableSchemas, final List sourceTableNames) {
			super(dsSchemaName, dsTableName, targetTableSchema, targetTableName);
			this.sourceTableSchemas = sourceTableSchemas;
			this.sourceTableNames = sourceTableNames;
		}

		public List getSourceTableNames() {
			return this.sourceTableNames;
		}

		public List getSourceTableSchemas() {
			return this.sourceTableSchemas;
		}

		public String getStatusMessage() {
			return Resources.get("mcUnion");
		}
	}
}
