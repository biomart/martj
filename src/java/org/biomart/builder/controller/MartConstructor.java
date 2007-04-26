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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.TransformationUnit;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueList;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueRange;
import org.biomart.builder.model.MartConstructorAction.AddExpression;
import org.biomart.builder.model.MartConstructorAction.ConcatJoin;
import org.biomart.builder.model.MartConstructorAction.CopyOptimiserDirect;
import org.biomart.builder.model.MartConstructorAction.CopyOptimiserVia;
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
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedTableDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition.RecursionType;
import org.biomart.builder.model.TransformationUnit.Concat;
import org.biomart.builder.model.TransformationUnit.Expression;
import org.biomart.builder.model.TransformationUnit.JoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Schema.JDBCSchema;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * This interface defines the behaviour expected from an object which can take a
 * dataset and actually construct a mart based on this information. Whether it
 * carries out the task or just writes some DDL to be run by the user later is
 * up to the implementor.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public interface MartConstructor {

	/**
	 * This method takes a dataset and generates a {@link Runnable} which when
	 * run will construct a graph of actions describing how to construct the
	 * mart, then emit those actions as events to whatever may be listening.
	 * <p>
	 * The {@link Runnable} can be started by calling {@link Runnable#run()} on
	 * it. Ideally this should be done within it's own {@link Thread}, so that
	 * the thread can do the work in the background.
	 * <p>
	 * Once started, the {@link Runnable} can be monitored using the methods
	 * available in the {@link ConstructorRunnable} interface.
	 * 
	 * @param targetSchemaName
	 *            the name of the schema to create the dataset tables in.
	 * @param datasets
	 *            a set of datasets to construct. An empty set means nothing
	 *            will get constructed.
	 * @return the {@link Runnable} object that when run will construct the
	 *         action graph and start emitting action events.
	 * @throws Exception
	 *             if there was any problem creating the {@link Runnable}
	 *             object.
	 */
	public ConstructorRunnable getConstructorRunnable(String targetSchemaName,
			Collection datasets) throws Exception;

	/**
	 * This interface defines a class which does the actual construction work.
	 * It should keep its status up-to-date, as these will be displayed
	 * regularly to the user. You should probably provide a constructor which
	 * takes a dataset as a parameter.
	 */
	public interface ConstructorRunnable extends Runnable {

		/**
		 * This method adds a listener which will listen out for events emitted
		 * by the constructor.
		 * 
		 * @param listener
		 *            the listener to add.
		 */
		public void addMartConstructorListener(MartConstructorListener listener);

		/**
		 * This method will be called if the user wants the thread to stop work
		 * straight away. It should set an exception for
		 * {@link #getFailureException()} to return saying that it was
		 * cancelled, so that the user knows it was so, and doesn't think it
		 * just finished successfully without any warnings.
		 */
		public void cancel();

		/**
		 * If the thread failed or was cancelled, this method should return an
		 * exception describing the failure. If it succeeded, or is still in
		 * progress and hasn't failed yet, it should return <tt>null</tt>.
		 * 
		 * @return the exception that caused the thread to fail, if any, or
		 *         <tt>null</tt> otherwise.
		 */
		public Exception getFailureException();

		/**
		 * This method should return a value between 0 and 100 indicating how
		 * the thread is getting along in the general scheme of things. 0
		 * indicates just starting, 100 indicates complete.
		 * 
		 * @return a percentage indicating how far the thread has got.
		 */
		public int getPercentComplete();

		/**
		 * This method should return a message describing what the thread is
		 * currently doing.
		 * 
		 * @return a message describing current activity.
		 */
		public String getStatusMessage();
	}

	/**
	 * Defines the generic way of constructing a mart. Generates a graph of
	 * actions then iterates through that graph in an ordered manner, ensuring
	 * that no action is reached before all actions it depends on have been
	 * reached. Each action it iterates over fires an action event to all
	 * listeners registered with it.
	 */
	public static class GenericConstructorRunnable implements
			ConstructorRunnable {
		private static final String NO_PARTITION = "__NO__PARTITION__";

		private boolean cancelled = false;;

		private Collection datasets;

		private String datasetSchemaName;

		private Exception failure = null;

		private Collection martConstructorListeners;

		private double percentComplete = 0.0;

		private final Map uniqueOptCols = new HashMap();

		private String statusMessage = Resources.get("mcCreatingGraph");

		private int tempNameCount = 0;

		/**
		 * Constructs a builder object that will construct an action graph
		 * containing all actions necessary to build the given dataset, then
		 * emit events related to those actions.
		 * <p>
		 * The helper specified will interface between the builder object and
		 * the data source, providing it with bits of data it may need in order
		 * to construct the graph.
		 * 
		 * @param datasetSchemaName
		 *            the name of the database schema into which the transformed
		 *            dataset should be put.
		 * @param datasets
		 *            the dataset(s) to transform into a mart.
		 */
		public GenericConstructorRunnable(final String datasetSchemaName,
				final Collection datasets) {
			super();
			Log.debug("Created generic constructor runnable");
			this.datasets = datasets;
			this.martConstructorListeners = new ArrayList();
			this.datasetSchemaName = datasetSchemaName;
		}

		private void checkCancelled() throws ConstructorException {
			if (this.cancelled)
				throw new ConstructorException(Resources.get("mcCancelled"));
		}

		/**
		 * This is the starting point for the conversion of a dataset into a set
		 * of actions. Internally, it constructs a graph of actions specific to
		 * this dataset, populates the graph, then iterates over the graph at
		 * the end emitting those actions as events in the correct order, so
		 * that any action that depends on another action is guaranteed to be
		 * emitted after the action it depends on.
		 * 
		 * @param dataset
		 *            the dataset to build an action graph for and then emit
		 *            actions from that graph.
		 * @param totalDataSetCount
		 *            a counter informing this method how many datasets in total
		 *            there are to process. It is used to work out percentage
		 *            process.
		 * @throws Exception
		 *             if anything goes wrong at all during the transformation
		 *             process.
		 */
		private void makeActionsForDataset(final DataSet dataset,
				final int totalDataSetCount) throws Exception {
			Log.debug("Making actions for dataset " + dataset);
			// Check not cancelled.
			this.checkCancelled();

			// Find out the main table source schema.
			final Schema templateSchema = dataset.getCentralTable().getSchema();

			// Is it partitioned?
			Collection partitions = templateSchema.getPartitions().entrySet();
			if (partitions.isEmpty()) {
				Log.debug("Using dummy empty partition");
				partitions = new ArrayList();
				partitions.add(new Map.Entry() {
					public Object getKey() {
						return ((JDBCSchema) templateSchema)
								.getDatabaseSchema();
					}

					public Object getValue() {
						return null;
					}

					public Object setValue(final Object value) {
						return null;
					}
				});
			}

			// Work out the progress step size : 1 step = 1 table per source
			// schema partition.
			double stepPercent = 100.0 / dataset.getTables().size()
					/ partitions.size();

			// Divide the progress step size by the number of datasets.
			stepPercent /= totalDataSetCount;

			// Process the tables.
			for (final Iterator s = partitions.iterator(); s.hasNext();) {
				final Map.Entry partition = (Map.Entry) s.next();
				// Clear out optimiser col names so that they start
				// again on this partition.
				this.uniqueOptCols.clear();
				Log.debug("Starting partition " + partition);
				this.issueListenerEvent(
						MartConstructorListener.PARTITION_STARTED, partition
								.getKey());
				for (final Iterator i = this.getTablesToProcess(dataset)
						.iterator(); i.hasNext();) {
					final DataSetTable dsTable = (DataSetTable) i.next();
					this.makeActionsForDatasetTable(templateSchema,
							(String) partition.getKey(), (String) partition
									.getValue(), dataset, dsTable);

					// Update the progress percentage once per table.
					this.percentComplete += stepPercent;

					// Check not cancelled.
					this.checkCancelled();
				}
				this.issueListenerEvent(
						MartConstructorListener.PARTITION_ENDED, partition
								.getKey());
			}
			Log.debug("Finished dataset " + dataset);
		}

		private List getTablesToProcess(final DataSet dataset)
				throws ValidationException {
			Log.debug("Creating ordered list of tables for dataset " + dataset);
			// Create a list in the order by which we want to process tables.
			final List tablesToProcess = new ArrayList();
			// Main table first.
			tablesToProcess.add(dataset.getMainTable());
			// Now recursively expand the table list.
			for (int i = 0; i < tablesToProcess.size(); i++) {
				final DataSetTable tbl = (DataSetTable) tablesToProcess.get(i);
				// Check that, if it is subclassed, it has cols
				// with all the same modified names as the parent table.
				if (tbl.getType().equals(DataSetTableType.MAIN_SUBCLASS)) {
					// Find parent table.
					Relation parentRelation = null;
					for (final Iterator j = tbl.getRelations().iterator(); parentRelation == null
							&& j.hasNext();) {
						final Relation rel = (Relation) j.next();
						if (rel.getManyKey().getTable().equals(tbl))
							parentRelation = rel;
					}
					final DataSetTable parentTable = (DataSetTable) parentRelation
							.getOneKey().getTable();
					// Get the two sets of column names.
					final Collection parentColNames = new HashSet();
					for (final Iterator j = parentTable.getColumns().iterator(); j
							.hasNext();) {
						final DataSetColumn col = (DataSetColumn) j.next();
						if (!dataset.getDataSetModifications().isMaskedColumn(
								col))
							parentColNames.add(col.getModifiedName());
					}
					final Collection ourColNames = new HashSet();
					for (final Iterator j = tbl.getColumns().iterator(); j
							.hasNext();)
						ourColNames.add(((DataSetColumn) j.next())
								.getModifiedName());
					// Make the check.
					if (!ourColNames.containsAll(parentColNames)) {
						final StringBuffer missingCols = new StringBuffer();
						final Collection cols = new HashSet(parentColNames);
						cols.removeAll(ourColNames);
						for (final Iterator j = cols.iterator(); j.hasNext();) {
							missingCols.append(j.next());
							if (j.hasNext())
								missingCols.append(", ");
						}
						throw new ValidationException(Resources.get(
								"subclassMissingCols", new String[] {
										tbl.getModifiedName(),
										parentTable.getModifiedName(),
										missingCols.toString() }));
					}
				}
				// Expand the table.
				final Collection nextSCs = new HashSet();
				final Collection nextDims = new HashSet();
				for (final Iterator j = tbl.getRelations().iterator(); j
						.hasNext();) {
					final Relation r = (Relation) j.next();
					final DataSetTable dsTab = (DataSetTable) r.getManyKey()
							.getTable();
					if (!tablesToProcess.contains(dsTab)
							&& !dataset.getDataSetModifications()
									.isMaskedTable(dsTab))
						if (dsTab.getType().equals(DataSetTableType.DIMENSION))
							nextDims.add(dsTab);
						else
							nextSCs.add(dsTab);
				}
				// We need to insert each dimension directly
				// after its parent table and before any subsequent
				// subclass table. This ensures that by the time the subclass
				// table is created, the parent table will have all its
				// columns in place and complete already.
				tablesToProcess.addAll(i + 1, nextSCs);
				tablesToProcess.addAll(i + 1, nextDims);
			}
			return tablesToProcess;
		}

		private void makeActionsForDatasetTable(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable)
				throws Exception {
			Log.debug("Creating actions for table " + dsTable);
			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsTable, GenericConstructorRunnable.NO_PARTITION);
			final String tempName = "TEMP";
			final Map previousTempTables = new HashMap();
			final Map previousIndexes = new HashMap();
			final List partitionValues = new ArrayList();
			final List optimisers = new ArrayList();
			optimisers.add(null); // No-expression default column.
			partitionValues.add(GenericConstructorRunnable.NO_PARTITION);
			previousTempTables.put(GenericConstructorRunnable.NO_PARTITION,
					null);
			boolean requiresFinalLeftJoin = dataset.getDataSetModifications()
					.isPartitionedTable(dsTable);

			// Use the transformation units to create the basic table.
			for (final Iterator j = dsTable.getTransformationUnits().iterator(); j
					.hasNext();) {
				final TransformationUnit tu = (TransformationUnit) j.next();

				// Partitioned column from this unit? Then partition it.
				String delayedTempDrop = null;
				PartitionedColumnDefinition pc = null;
				if (dataset.getDataSetModifications().isPartitionedTable(
						dsTable)) {
					pc = dataset.getDataSetModifications()
							.getPartitionedColumnDef(dsTable);
					final DataSetColumn partCol = (DataSetColumn) dsTable
							.getColumnByName(dataset.getDataSetModifications()
									.getPartitionedColumnName(dsTable));
					if (partCol != null
							&& tu.getNewColumnNameMap().containsValue(partCol))
						delayedTempDrop = this.populatePartitionValues(
								schemaPartition, pc, partCol, partitionValues,
								previousTempTables, previousIndexes);
					else
						pc = null;
				}

				// Do unit once per partition.
				for (final Iterator v = partitionValues.iterator(); v.hasNext();) {
					final String partitionValue = (String) v.next();
					final String tempTable = tempName + this.tempNameCount++;
					previousIndexes.put(tempTable, new HashSet());

					// Translate TU to Action.
					// Expression?
					if (tu instanceof Expression) {
						if (!this.doExpression(templateSchema, schemaPartition,
								schemaPrefix, dataset, dsTable,
								(Expression) tu, previousTempTables,
								previousIndexes, partitionValue, tempTable,
								optimisers)) {
							// Skip to next action to prevent non-existent
							// new temp table from getting dropped.
							continue;
						}
					}
					// Concat?
					else if (tu instanceof Concat)
						this.doConcat(templateSchema, schemaPartition,
								schemaPrefix, dataset, dsTable, (Concat) tu,
								previousTempTables, previousIndexes,
								partitionValue, tempTable);
					// Left-join?
					else if (tu instanceof JoinTable)
						requiresFinalLeftJoin |= this.doJoinTable(
								templateSchema, schemaPartition, schemaPrefix,
								dataset, dsTable, (JoinTable) tu,
								previousTempTables, previousIndexes, pc,
								partitionValue, tempTable);
					// Select-from?
					else if (tu instanceof SelectFromTable)
						this.doSelectFromTable(templateSchema, schemaPartition,
								schemaPrefix, dataset, dsTable,
								(SelectFromTable) tu, previousTempTables,
								previousIndexes, pc, partitionValue, tempTable);
					else
						throw new BioMartError();

					// Drop the previous table if we're finished with it.
					if (delayedTempDrop == null
							&& (String) previousTempTables.get(partitionValue) != null) {
						final Drop action = new Drop(this.datasetSchemaName,
								finalCombinedName);
						action.setTable((String) previousTempTables
								.get(partitionValue));
						this.issueAction(action);
					}

					// Update the previous table.
					previousTempTables.put(partitionValue, tempTable);
				}

				// Drop the temp table finally, if not already done.
				if (delayedTempDrop != null) {
					final Drop action = new Drop(this.datasetSchemaName,
							finalCombinedName);
					action.setTable(delayedTempDrop);
					this.issueAction(action);
				}
			}

			// Do final set of actions for table once per partition.
			for (final Iterator v = partitionValues.iterator(); v.hasNext();) {
				final String partitionValue = (String) v.next();
				final String finalName = this.getFinalName(schemaPrefix,
						dsTable, partitionValue);

				// Do a final left-join against the parent to reinstate
				// any potentially missing rows.
				if (requiresFinalLeftJoin
						&& !dsTable.getType().equals(DataSetTableType.MAIN))
					this.doParentLeftJoin(templateSchema, schemaPartition,
							schemaPrefix, dataset, dsTable, finalCombinedName,
							partitionValue, previousTempTables,
							previousIndexes, tempName + this.tempNameCount++);

				// Does it need a final distinct?
				if (dataset.getDataSetModifications().isDistinctTable(dsTable))
					this.doDistinct(templateSchema, schemaPartition,
							schemaPrefix, dataset, dsTable, finalCombinedName,
							partitionValue, previousTempTables,
							previousIndexes, tempName + this.tempNameCount++);

				// Drop masked dependencies and create column indices.
				final List dropCols = new ArrayList();
				for (final Iterator x = dsTable.getColumns().iterator(); x
						.hasNext();) {
					final DataSetColumn col = (DataSetColumn) x.next();
					if (col.isRequiredInterim() && !col.isRequiredFinal())
						dropCols.add(col.getModifiedName());
					else // Create index if required.
					if (dataset.getDataSetModifications().isIndexedColumn(col)) {
						final Index index = new Index(this.datasetSchemaName,
								finalCombinedName);
						index.setTable((String) previousTempTables
								.get(partitionValue));
						index.setColumns(Collections.singletonList(col
								.getModifiedName()));
						this.issueAction(index);
					}
				}
				if (!dropCols.isEmpty()) {
					final DropColumns dropcol = new DropColumns(
							this.datasetSchemaName, finalCombinedName);
					dropcol.setTable((String) previousTempTables
							.get(partitionValue));
					dropcol.setColumns(dropCols);
					this.issueAction(dropcol);
				}

				// Add a rename action to produce the final table.
				final Rename action = new Rename(this.datasetSchemaName,
						finalCombinedName);
				action.setFrom((String) previousTempTables.get(partitionValue));
				action.setTo(finalName);
				this.issueAction(action);

				// Create indexes on all keys on the final table.
				for (final Iterator j = dsTable.getKeys().iterator(); j
						.hasNext();) {
					final Key key = (Key) j.next();
					final List keyCols = new ArrayList();
					for (final Iterator k = key.getColumns().iterator(); k
							.hasNext();)
						keyCols.add(((DataSetColumn) k.next())
								.getModifiedName());
					final Index index = new Index(this.datasetSchemaName,
							finalCombinedName);
					index.setTable(finalName);
					index.setColumns(keyCols);
					this.issueAction(index);
				}

				// Create optimiser columns - either count or bool,
				// or none if not required.
				// If this is a subclass table, then the optimiser
				// type is always COUNT_INHERIT.
				final DataSetOptimiserType oType = dataset
						.getDataSetModifications().isNoOptimiserTable(dsTable) ? DataSetOptimiserType.NONE
						: dsTable.getType().equals(
								DataSetTableType.MAIN_SUBCLASS) ? ((DataSet) dsTable
								.getSchema()).isSubclassOptimiser() ? DataSetOptimiserType.COLUMN_INHERIT
								: DataSetOptimiserType.NONE
								: dataset.getDataSetOptimiserType();
				if (!oType.equals(DataSetOptimiserType.NONE))
					this.doOptimiseTable(templateSchema, schemaPartition,
							schemaPrefix, dataset, dsTable, oType,
							partitionValue, optimisers);
			}
		}

		private void doParentLeftJoin(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable,
				final String finalCombinedName, final String partitionValue,
				final Map previousTempTables, final Map previousIndexes,
				final String tempTable) throws Exception {
			// Work out the parent table.
			final DataSetTable parent = (DataSetTable) ((Relation) ((Key) dsTable
					.getForeignKeys().iterator().next()).getRelations()
					.iterator().next()).getOneKey().getTable();
			// Work out what columns to take from each side.
			final List leftJoinCols = new ArrayList();
			final List leftSelectCols = leftJoinCols;
			final List rightJoinCols = leftJoinCols;
			final List rightSelectCols = new ArrayList();
			for (final Iterator x = parent.getPrimaryKey().getColumns()
					.iterator(); x.hasNext();)
				leftJoinCols.add(((DataSetColumn) x.next()).getModifiedName());
			for (final Iterator x = dsTable.getColumns().iterator(); x
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) x.next();
				if (col.isRequiredInterim())
					rightSelectCols.add(col.getModifiedName());
			}
			rightSelectCols.removeAll(rightJoinCols);
			// Index the left-hand side of the join.
			if (!((Collection) previousIndexes.get(previousTempTables
					.get(partitionValue))).contains(leftJoinCols)) {
				final String indTbl = (String) previousTempTables
						.get(partitionValue);
				final Index index = new Index(this.datasetSchemaName,
						finalCombinedName);
				index.setTable(indTbl);
				index.setColumns(leftJoinCols);
				this.issueAction(index);
				((Collection) previousIndexes.get(indTbl)).add(leftJoinCols);
			}
			// Make the join.
			final LeftJoin action = new LeftJoin(this.datasetSchemaName,
					finalCombinedName);
			action.setLeftTable(this.getFinalName(schemaPrefix, parent,
					GenericConstructorRunnable.NO_PARTITION));
			action.setRightSchema(this.datasetSchemaName);
			action.setRightTable((String) previousTempTables
					.get(partitionValue));
			action.setLeftJoinColumns(leftJoinCols);
			action.setRightJoinColumns(rightJoinCols);
			action.setLeftSelectColumns(leftSelectCols);
			action.setRightSelectColumns(rightSelectCols);
			action.setResultTable(tempTable);
			this.issueAction(action);
			// Drop the old one.
			final Drop drop = new Drop(this.datasetSchemaName,
					finalCombinedName);
			drop.setTable((String) previousTempTables.get(partitionValue));
			this.issueAction(drop);
			// Update the previous temp table.
			previousTempTables.put(partitionValue, tempTable);
		}

		private void doDistinct(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable,
				final String finalCombinedName, final String partitionValue,
				final Map previousTempTables, final Map previousIndexes,
				final String tempTable) throws Exception {
			// Make the join.
			final Distinct action = new Distinct(this.datasetSchemaName,
					finalCombinedName);
			action.setSchema(this.datasetSchemaName);
			action.setTable((String) previousTempTables.get(partitionValue));
			action.setResultTable(tempTable);
			this.issueAction(action);
			// Drop the old one.
			final Drop drop = new Drop(this.datasetSchemaName,
					finalCombinedName);
			drop.setTable((String) previousTempTables.get(partitionValue));
			this.issueAction(drop);
			// Update the previous temp table.
			previousTempTables.put(partitionValue, tempTable);
		}

		private void doOptimiseTable(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable,
				final DataSetOptimiserType oType, final String partitionValue,
				final Collection optimisers) throws Exception {
			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsTable, GenericConstructorRunnable.NO_PARTITION);
			if (!dsTable.getType().equals(DataSetTableType.DIMENSION)) {
				// Tables are same name, but use 'bool' or 'count'
				// instead of 'main'
				final String optTable = this.getOptimiserTableName(
						schemaPrefix, dsTable, oType);
				// If main/subclass table type, create table.
				if (oType.isTable()) {
					// The key cols are those from the primary key.
					final List keyCols = new ArrayList();
					for (final Iterator y = dsTable.getPrimaryKey()
							.getColumns().iterator(); y.hasNext();)
						keyCols.add(((DataSetColumn) y.next())
								.getModifiedName());

					// Create the table by selecting the pk.
					final CreateOptimiser create = new CreateOptimiser(
							this.datasetSchemaName, finalCombinedName);
					create.setKeyColumns(keyCols);
					create.setOptTableName(optTable);
					this.issueAction(create);

					// Index the pk on the new table.
					final Index index = new Index(this.datasetSchemaName,
							finalCombinedName);
					index.setTable(optTable);
					index.setColumns(keyCols);
					this.issueAction(index);
				}
			}
			if (!dsTable.getType().equals(DataSetTableType.MAIN)) {
				// Work out the dimension/subclass parent.
				final DataSetTable parent = (DataSetTable) ((Relation) ((Key) dsTable
						.getForeignKeys().iterator().next()).getRelations()
						.iterator().next()).getOneKey().getTable();
				// Set up the column on the dimension parent.
				final String optTable = this.getOptimiserTableName(
						schemaPrefix, parent, oType);
				// Key columns are primary key cols from parent.
				// Do a left-join update. We're looking for rows
				// where at least one child non-key col is non-null.
				final List keyCols = new ArrayList();
				for (final Iterator y = parent.getPrimaryKey().getColumns()
						.iterator(); y.hasNext();)
					keyCols.add(((DataSetColumn) y.next()).getModifiedName());
				final List nonNullCols = new ArrayList();
				for (final Iterator y = dsTable.getColumns().iterator(); y
						.hasNext();) {
					final DataSetColumn col = (DataSetColumn) y.next();
					// We won't select masked cols as they won't be in
					// the final table, and we won't select expression
					// columns as they can genuinely be null.
					if (!dataset.getDataSetModifications().isMaskedColumn(col)
							&& !(col instanceof ExpressionColumn))
						nonNullCols.add(col.getModifiedName());
				}
				nonNullCols.removeAll(keyCols);
				// Create all optimiser cols.
				for (final Iterator x = optimisers.iterator(); x.hasNext();) {
					final ExpressionColumn optExprCol = (ExpressionColumn) x
							.next();
					// Columns are dimension table names with '_bool' or
					// '_count' appended.
					final String optCol = this.getOptimiserColumnName(parent,
							dsTable, partitionValue, oType, optExprCol);

					// Do the bool/count update.
					final UpdateOptimiser update = new UpdateOptimiser(
							this.datasetSchemaName, finalCombinedName);
					update.setKeyColumns(keyCols);
					update.setNonNullColumns(nonNullCols);
					update.setSourceTableName(this.getFinalName(schemaPrefix,
							dsTable, partitionValue));
					update.setOptTableName(optTable);
					update.setOptColumnName(optCol);
					update.setCountNotBool(!oType.isBool());
					update.setNullNotZero(!oType.isUseNull());
					if (optExprCol != null) {
						update.setExpressionDSTable((DataSetTable) optExprCol
								.getTable());
						update.setExpression(optExprCol.getDefinition());
					}
					this.issueAction(update);

					// Index the column if required.
					if (dataset.isIndexOptimiser()) {
						final Index index = new Index(this.datasetSchemaName,
								finalCombinedName);
						index.setTable(optTable);
						index.setColumns(Collections.singletonList(optCol));
						this.issueAction(index);
					}

					// Work out the inherited parents.
					final List parents = new ArrayList();
					parents.add(parent);
					if (oType.isInherit())
						for (int z = 0; z < parents.size(); z++) {
							final DataSetTable child = (DataSetTable) parents
									.get(z);
							final Iterator fks = child.getForeignKeys()
									.iterator();
							if (fks.hasNext())
								parents.add(((Relation) ((Key) fks.next())
										.getRelations().iterator().next())
										.getOneKey().getTable());
						}

					// Copy the column to the inherited parents by following
					// pairs.
					for (int i = 0; i < parents.size() - 1; i++) {
						final DataSetTable fromParent = (DataSetTable) parents
								.get(i);
						final DataSetTable toParent = (DataSetTable) parents
								.get(i + 1);
						final List fromKeyCols = new ArrayList();
						for (final Iterator y = fromParent.getPrimaryKey()
								.getColumns().iterator(); y.hasNext();)
							fromKeyCols.add(((DataSetColumn) y.next())
									.getModifiedName());
						final List toKeyCols = new ArrayList();
						for (final Iterator y = toParent.getPrimaryKey()
								.getColumns().iterator(); y.hasNext();)
							toKeyCols.add(((DataSetColumn) y.next())
									.getModifiedName());

						// Copy the column.
						final String fromTableName = this
								.getOptimiserTableName(schemaPrefix,
										fromParent, oType);
						final String viaTableName = this.getFinalName(
								schemaPrefix, fromParent,
								GenericConstructorRunnable.NO_PARTITION);
						final CopyOptimiserDirect copy;
						if (fromTableName.equals(viaTableName))
							copy = new CopyOptimiserDirect(
									this.datasetSchemaName, finalCombinedName);
						else {
							copy = new CopyOptimiserVia(this.datasetSchemaName,
									finalCombinedName);
							((CopyOptimiserVia) copy)
									.setFromKeyColumns(fromKeyCols);
							((CopyOptimiserVia) copy)
									.setViaTableName(viaTableName);
						}
						copy.setFromOptTableName(fromTableName);
						copy.setToKeyColumns(toKeyCols);
						copy.setToOptTableName(this.getOptimiserTableName(
								schemaPrefix, toParent, oType));
						copy.setFromOptColumnName(optCol);
						copy.setToOptColumnName(this.getOptimiserColumnName(
								toParent, dsTable, partitionValue, oType,
								optExprCol));
						copy.setCountNotBool(!oType.isBool());
						this.issueAction(copy);

						// Index the copied column if required.
						if (dataset.isIndexOptimiser()) {
							final Index index = new Index(
									this.datasetSchemaName, finalCombinedName);
							index.setTable(this.getOptimiserTableName(
									schemaPrefix, toParent, oType));
							index.setColumns(Collections.singletonList(optCol));
							this.issueAction(index);
						}
					}
				}
			}
		}

		private void doSelectFromTable(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable,
				final SelectFromTable stu, final Map previousTempTables,
				final Map previousIndexes,
				final PartitionedColumnDefinition pc,
				final String partitionValue, final String tempTable)
				throws Exception {

			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsTable, GenericConstructorRunnable.NO_PARTITION);

			final Table sourceTable = stu.getTable();
			// Make sure that we use the same partition on the RHS
			// if it exists, otherwise use the default partition.
			String schema = null;
			if (sourceTable instanceof DataSetTable)
				schema = this.datasetSchemaName;
			else if (sourceTable.getSchema() == templateSchema)
				schema = schemaPartition;
			else {
				for (final Iterator i = ((JDBCSchema) sourceTable.getSchema())
						.getPartitions().entrySet().iterator(); i.hasNext()
						&& schema == null;) {
					final Map.Entry entry = (Map.Entry) i.next();
					if (entry.getValue().equals(schemaPrefix))
						schema = (String) entry.getKey();
				}
				if (schema == null)
					schema = ((JDBCSchema) sourceTable.getSchema())
							.getDatabaseSchema();
			}
			// Source tables are always main or subclass and
			// therefore are never partitioned.
			final String table = sourceTable instanceof DataSetTable ? this
					.getFinalName(schemaPrefix, (DataSetTable) sourceTable,
							GenericConstructorRunnable.NO_PARTITION) : stu
					.getTable().getName();
			final Map selectCols = new HashMap();
			// Select columns from parent table.
			for (final Iterator k = stu.getNewColumnNameMap().entrySet()
					.iterator(); k.hasNext();) {
				final Map.Entry entry = (Map.Entry) k.next();
				final DataSetColumn col = (DataSetColumn) entry.getValue();
				if (col.isRequiredInterim())
					selectCols
							.put(
									sourceTable instanceof DataSetTable ? ((DataSetColumn) sourceTable
											.getColumnByName((String) entry
													.getKey()))
											.getModifiedName()
											: entry.getKey(), col
											.getModifiedName());
			}
			// Add to selectCols all the inherited has columns, if
			// this is not a dimension table.
			if (sourceTable instanceof DataSetTable
					&& !dsTable.getType().equals(DataSetTableType.DIMENSION)) {
				final Collection hasCols = (Collection) this.uniqueOptCols
						.get(sourceTable);
				if (hasCols != null)
					for (final Iterator k = hasCols.iterator(); k.hasNext();) {
						final String hasCol = (String) k.next();
						selectCols.put(hasCol, hasCol);
					}
			}
			// Do the select.
			final Select action = new Select(this.datasetSchemaName,
					finalCombinedName);
			action.setSchema(schema);
			action.setTable(table);
			action.setSelectColumns(selectCols);
			action.setResultTable(tempTable);
			if (pc != null) {
				action.setPartitionColumn((String) stu
						.getReverseNewColumnNameMap().get(
								dsTable.getColumnByName(dataset
										.getDataSetModifications()
										.getPartitionedColumnName(dsTable))));
				if (pc instanceof ValueRange)
					action.setPartitionRangeDef((ValueRange) pc);
				else if (pc instanceof ValueList)
					action.setPartitionListDef((ValueList) pc);
				else
					throw new BioMartError();
				action.setPartitionValue(partitionValue);
			}
			if (dataset.getSchemaModifications().isRestrictedTable(dsTable,
					stu.getTable()))
				action.setTableRestriction(dataset.getSchemaModifications()
						.getRestrictedTable(dsTable, stu.getTable()));
			this.issueAction(action);
		}

		private void doConcat(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable,
				final Concat ljtu, final Map previousTempTables,
				final Map previousIndexes, final String partitionValue,
				final String tempTable) throws Exception {
			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsTable, GenericConstructorRunnable.NO_PARTITION);
			final String tempJoinTable = tempTable + "C";

			// Make sure that we use the same partition on the RHS
			// if it exists, otherwise use the default partition.
			String rightSchema = null;
			if (ljtu.getTable().getSchema() == templateSchema)
				rightSchema = schemaPartition;
			else {
				for (final Iterator i = ((JDBCSchema) ljtu.getTable()
						.getSchema()).getPartitions().entrySet().iterator(); i
						.hasNext()
						&& rightSchema == null;) {
					final Map.Entry entry = (Map.Entry) i.next();
					if (entry.getValue().equals(schemaPrefix))
						rightSchema = (String) entry.getKey();
				}
				if (rightSchema == null)
					rightSchema = ((JDBCSchema) ljtu.getTable().getSchema())
							.getDatabaseSchema();
			}
			final String rightTable = ljtu.getTable().getName();
			final List leftJoinCols = new ArrayList();
			final List rightJoinCols = ljtu.getSchemaRelation().getOtherKey(
					ljtu.getSchemaSourceKey()).getColumnNames();

			// Populate vars.
			for (final Iterator k = ljtu.getSourceDataSetColumns().iterator(); k
					.hasNext();)
				leftJoinCols.add(((DataSetColumn) k.next()).getModifiedName());
			final ConcatColumn concCol = (ConcatColumn) ljtu
					.getNewColumnNameMap().values().iterator().next();
			final ConcatRelationDefinition concDef = concCol.getDefinition();

			// Index the left-hand side of the join.
			if (!((Collection) previousIndexes.get(previousTempTables
					.get(partitionValue))).contains(leftJoinCols)) {
				final Index index = new Index(this.datasetSchemaName,
						finalCombinedName);
				index.setTable((String) previousTempTables.get(partitionValue));
				index.setColumns(leftJoinCols);
				this.issueAction(index);
				((Collection) previousIndexes.get(previousTempTables
						.get(partitionValue))).add(leftJoinCols);
			}

			// Create the temp RHS table. The concat column will
			// be called "conc_col".
			final ConcatJoin action = new ConcatJoin(this.datasetSchemaName,
					finalCombinedName);
			action
					.setLeftTable((String) previousTempTables
							.get(partitionValue));
			action.setRightSchema(rightSchema);
			action.setRightTable(rightTable);
			action.setLeftJoinColumns(leftJoinCols);
			action.setRightJoinColumns(rightJoinCols);
			action.setConcatColumnName("conc_col");
			action.setConcatColumnDefinition(concDef);
			if (dataset.getSchemaModifications().isRestrictedTable(dsTable,
					ljtu.getTable()))
				action.setTableRestriction(dataset.getSchemaModifications()
						.getRestrictedTable(dsTable, ljtu.getTable()));
			if (dataset.getSchemaModifications()
					.isRestrictedRelation(dsTable, ljtu.getSchemaRelation(),
							ljtu.getSchemaRelationIteration())) {
				action.setRelationRestrictionPreviousUnit(ljtu
						.getPreviousUnit());
				action.setRelationRestrictionLeftIsFirst(ljtu
						.getSchemaRelation().getFirstKey().equals(
								ljtu.getSchemaSourceKey()));
				action.setRelationRestriction(dataset.getSchemaModifications()
						.getRestrictedRelation(dsTable,
								ljtu.getSchemaRelation(),
								ljtu.getSchemaRelationIteration()));
			}
			action.setResultTable(tempJoinTable);
			action.setRecursionType(concDef.getRecursionType());
			if (concDef.getRecursionType() != RecursionType.NONE) {
				final Key viaKey = concDef.getFirstRelation().getOtherKey(
						concDef.getRecursionKey());
				final Table viaTable = viaKey.getTable();
				action.setRecursionFromColumns(concDef.getRecursionKey()
						.getColumnNames());
				action.setRecursionToColumns(viaKey.getColumnNames());
				action.setRecursionTable(viaTable.getName());
				if (concDef.getSecondRelation() != null) {
					action
							.setRecursionSecondFromColumns((concDef
									.getSecondRelation().getFirstKey().equals(
											viaTable) ? concDef
									.getSecondRelation().getFirstKey()
									: concDef.getSecondRelation()
											.getSecondKey()).getColumnNames());
					action.setRecursionSecondToColumns((concDef
							.getSecondRelation().getSecondKey()
							.equals(viaTable) ? concDef.getSecondRelation()
							.getFirstKey() : concDef.getSecondRelation()
							.getSecondKey()).getColumnNames());
				}
			}
			this.issueAction(action);

			// Index the temp RHS table.
			final Index index = new Index(this.datasetSchemaName,
					finalCombinedName);
			index.setTable(tempJoinTable);
			// Concat table will have left join cols as key cols.
			index.setColumns(leftJoinCols);
			this.issueAction(index);

			// Make the join between LHS and temp RHS.
			final Map selectCols = new HashMap();
			selectCols.put("conc_col", concCol.getModifiedName());
			final Join jaction = new Join(this.datasetSchemaName,
					finalCombinedName);
			jaction.setLeftTable((String) previousTempTables
					.get(partitionValue));
			jaction.setRightSchema(this.datasetSchemaName);
			jaction.setRightTable(tempJoinTable);
			jaction.setLeftJoinColumns(leftJoinCols);
			// Concat table will have left join cols as key cols.
			jaction.setRightJoinColumns(leftJoinCols);
			jaction.setSelectColumns(selectCols);
			jaction.setResultTable(tempTable);
			this.issueAction(jaction);

			// Drop temp RHS.
			final Drop drop = new Drop(this.datasetSchemaName,
					finalCombinedName);
			drop.setTable(tempJoinTable);
			this.issueAction(drop);
		}

		private boolean doJoinTable(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable,
				final JoinTable ljtu, final Map previousTempTables,
				final Map previousIndexes,
				final PartitionedColumnDefinition pc,
				final String partitionValue, final String tempTable)
				throws Exception {
			boolean requiresFinalLeftJoin = false;
			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsTable, GenericConstructorRunnable.NO_PARTITION);

			// Make sure that we use the same partition on the RHS
			// if it exists, otherwise use the default partition.
			String rightSchema = null;
			if (ljtu.getTable().getSchema() == templateSchema)
				rightSchema = schemaPartition;
			else {
				for (final Iterator i = ((JDBCSchema) ljtu.getTable()
						.getSchema()).getPartitions().entrySet().iterator(); i
						.hasNext()
						&& rightSchema == null;) {
					final Map.Entry entry = (Map.Entry) i.next();
					if (entry.getValue().equals(schemaPrefix))
						rightSchema = (String) entry.getKey();
				}
				if (rightSchema == null)
					rightSchema = ((JDBCSchema) ljtu.getTable().getSchema())
							.getDatabaseSchema();
			}
			final String rightTable = ljtu.getTable().getName();
			final List leftJoinCols = new ArrayList();
			final List rightJoinCols = ljtu.getSchemaRelation().getOtherKey(
					ljtu.getSchemaSourceKey()).getColumnNames();
			final Map selectCols = new HashMap();
			// Populate vars.
			for (final Iterator k = ljtu.getSourceDataSetColumns().iterator(); k
					.hasNext();)
				leftJoinCols.add(((DataSetColumn) k.next()).getModifiedName());
			for (final Iterator k = ljtu.getNewColumnNameMap().entrySet()
					.iterator(); k.hasNext();) {
				final Map.Entry entry = (Map.Entry) k.next();
				final DataSetColumn col = (DataSetColumn) entry.getValue();
				if (col.isRequiredInterim())
					selectCols.put(entry.getKey(), col.getModifiedName());
			}
			// Index the left-hand side of the join.
			if (!((Collection) previousIndexes.get(previousTempTables
					.get(partitionValue))).contains(leftJoinCols)) {
				final Index index = new Index(this.datasetSchemaName,
						finalCombinedName);
				index.setTable((String) previousTempTables.get(partitionValue));
				index.setColumns(leftJoinCols);
				this.issueAction(index);
				((Collection) previousIndexes.get(previousTempTables
						.get(partitionValue))).add(leftJoinCols);
			}
			// Make the join.
			final Join action = new Join(this.datasetSchemaName,
					finalCombinedName);
			action
					.setLeftTable((String) previousTempTables
							.get(partitionValue));
			action.setRightSchema(rightSchema);
			action.setRightTable(rightTable);
			action.setLeftJoinColumns(leftJoinCols);
			action.setRightJoinColumns(rightJoinCols);
			action.setSelectColumns(selectCols);
			action.setResultTable(tempTable);
			if (pc != null) {
				final DataSetColumn dsCol = (DataSetColumn) dsTable
						.getColumnByName(dataset.getDataSetModifications()
								.getPartitionedColumnName(dsTable));
				// We do a null check in case the partition refers to a
				// non-existent column.
				if (dsCol != null) {
					action
							.setPartitionColumn((String) ljtu
									.getReverseNewColumnNameMap().get(
											dsTable.getColumnByName(dataset
													.getDataSetModifications()
													.getPartitionedColumnName(
															dsTable))));
					if (pc instanceof ValueRange)
						action.setPartitionRangeDef((ValueRange) pc);
					else if (pc instanceof ValueList)
						action.setPartitionListDef((ValueList) pc);
					else
						throw new BioMartError();
					action.setPartitionValue(partitionValue);
				}
			}
			if (dataset.getSchemaModifications().isRestrictedTable(dsTable,
					ljtu.getTable())) {
				final RestrictedTableDefinition def = dataset
						.getSchemaModifications().getRestrictedTable(dsTable,
								ljtu.getTable());
				action.setTableRestriction(def);
				requiresFinalLeftJoin |= def.isHard();
			}
			if (dataset.getSchemaModifications()
					.isRestrictedRelation(dsTable, ljtu.getSchemaRelation(),
							ljtu.getSchemaRelationIteration())) {
				final RestrictedRelationDefinition def = dataset
						.getSchemaModifications().getRestrictedRelation(
								dsTable, ljtu.getSchemaRelation(),
								ljtu.getSchemaRelationIteration());
				action.setRelationRestrictionPreviousUnit(ljtu
						.getPreviousUnit());
				action.setRelationRestrictionLeftIsFirst(ljtu
						.getSchemaRelation().getFirstKey().equals(
								ljtu.getSchemaSourceKey()));
				action.setRelationRestriction(def);
				requiresFinalLeftJoin |= def.isHard();
			}
			this.issueAction(action);
			return requiresFinalLeftJoin;
		}

		private boolean doExpression(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final DataSet dataset, final DataSetTable dsTable,
				final Expression etu, final Map previousTempTables,
				final Map previousIndexes, final String partitionValue,
				final String tempTable, final Collection optimisers)
				throws Exception {

			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsTable, GenericConstructorRunnable.NO_PARTITION);
			// Some useful stuff.
			boolean useXTable = false;
			final String xTableName = tempTable + "X";
			final String firstTable = (String) previousTempTables
					.get(partitionValue);

			// Work out what columns we can select in the first group.
			final Collection selectCols = new HashSet();
			for (final Iterator z = dsTable.getColumns().iterator(); z
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) z.next();
				if (col.isRequiredInterim()
						&& !(col instanceof ExpressionColumn))
					selectCols.add(col.getModifiedName());
			}

			// Do each group of similar expressions in a single statement.
			for (final Iterator i = etu.getOrderedExpressionGroups().iterator(); i
					.hasNext();) {
				final Collection expGroup = (Collection) i.next();

				// Work out all group by and expression columns.
				final Collection groupByCols = new HashSet();
				final Map exprCols = new HashMap();
				for (final Iterator j = expGroup.iterator(); j.hasNext();) {
					final ExpressionColumn expCol = (ExpressionColumn) j.next();
					final ExpressionColumnDefinition expr = expCol
							.getDefinition();
					// We don't actually calculate optimiser columns.
					if (expr.isOptimiser()) {
						optimisers.add(expCol);
						continue;
					}
					// Otherwise, work out group-by stuff.
					if (expr.isGroupBy())
						for (final Iterator x = dsTable.getColumns().iterator(); x
								.hasNext();) {
							final DataSetColumn col = (DataSetColumn) x.next();
							if (expr.getAliases().keySet().contains(
									col.getName())
									|| !selectCols.contains(col
											.getModifiedName()))
								continue;
							groupByCols.add(col.getModifiedName());
						}
					// Add the column to the list to be generated.
					exprCols.put(expCol.getModifiedName(), expr
							.getSubstitutedExpression(dsTable, null));
				}

				// None left to do here? Don't do any then!
				if (exprCols.isEmpty())
					continue;

				// Rename temp to X table if required.
				if (useXTable) {
					final Rename rename = new Rename(this.datasetSchemaName,
							finalCombinedName);
					rename.setFrom(tempTable);
					rename.setTo(xTableName);
					this.issueAction(rename);
				}

				// Select only columns from all group bys in this group.
				if (!groupByCols.isEmpty())
					selectCols.retainAll(groupByCols);

				// Issue an AddExpression for the group.
				final AddExpression action = new AddExpression(
						this.datasetSchemaName, finalCombinedName);
				action.setTable(useXTable ? xTableName : firstTable);
				// We use a set to prevent post-modification problems.
				action.setSelectColumns(new HashSet(selectCols));
				action.setExpressionColumns(exprCols);
				if (!groupByCols.isEmpty())
					action.setGroupByColumns(groupByCols);
				action.setResultTable(tempTable);
				this.issueAction(action);

				// Update select cols for next time.
				for (final Iterator j = expGroup.iterator(); j.hasNext();)
					selectCols.add(((ExpressionColumn) j.next())
							.getModifiedName());

				// Drop the X table if it was used.
				if (useXTable) {
					final Drop drop = new Drop(this.datasetSchemaName,
							finalCombinedName);
					drop.setTable(xTableName);
					this.issueAction(drop);
				} else
					useXTable = true;
			}

			return useXTable;
		}

		private String populatePartitionValues(final String schemaPartition,
				final PartitionedColumnDefinition pc,
				final DataSetColumn partCol, final List partitionValues,
				final Map previousTempTables, final Map previousIndexes)
				throws SQLException {
			partitionValues.clear();
			if (pc instanceof ValueRange)
				partitionValues.addAll(((ValueRange) pc).getRanges().keySet());
			else if (pc instanceof ValueList)
				partitionValues.addAll(((ValueList) pc).getValues().keySet());
			else
				throw new BioMartError();
			final String delayedTempDrop = (String) previousTempTables
					.get(GenericConstructorRunnable.NO_PARTITION);
			for (final Iterator z = partitionValues.iterator(); z.hasNext();) {
				final Object partitionValue = z.next();
				previousTempTables.put(partitionValue, delayedTempDrop);
				previousIndexes.put(delayedTempDrop, new HashSet());
			}
			return delayedTempDrop;
		}

		private void issueAction(final MartConstructorAction action)
				throws Exception {
			// Execute the action.
			this.statusMessage = action.getStatusMessage();
			this.issueListenerEvent(MartConstructorListener.ACTION_EVENT, null,
					action);
		}

		private String getOptimiserTableName(
				final String schemaPartitionPrefix, final DataSetTable dsTable,
				final DataSetOptimiserType oType) {
			final StringBuffer finalName = new StringBuffer();
			if (schemaPartitionPrefix != null) {
				finalName.append(schemaPartitionPrefix);
				finalName.append(Resources.get("tablenameSubSep"));
			}
			finalName.append(dsTable.getSchema().getName());
			finalName.append(Resources.get("tablenameSep"));
			finalName.append(dsTable.getModifiedName());
			if (oType.equals(DataSetOptimiserType.TABLE)
					|| oType.equals(DataSetOptimiserType.TABLE_INHERIT)) {
				finalName.append(Resources.get("tablenameSubSep"));
				finalName.append(Resources.get("countTblPartition"));
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("dimensionSuffix"));
			} else if (oType.equals(DataSetOptimiserType.TABLE_BOOL)
					|| oType.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT)) {
				finalName.append(Resources.get("tablenameSubSep"));
				finalName.append(Resources.get("boolTblPartition"));
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("dimensionSuffix"));
			} else if (dsTable.getType().equals(DataSetTableType.MAIN)) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("mainSuffix"));
			} else if (dsTable.getType().equals(DataSetTableType.MAIN_SUBCLASS)) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("subclassSuffix"));
			} else if (dsTable.getType().equals(DataSetTableType.DIMENSION)) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("dimensionSuffix"));
			} else
				throw new BioMartError();
			return finalName.toString().replaceAll("\\W", "");
		}

		private String getOptimiserColumnName(final DataSetTable parent,
				final DataSetTable dsTable, final String partitionValue,
				final DataSetOptimiserType oType,
				final ExpressionColumn optExprCol) {
			// Set up storage for unique names if required.
			if (!this.uniqueOptCols.containsKey(parent))
				this.uniqueOptCols.put(parent, new HashSet());
			// Make a unique name.
			int counter = -1;
			String name;
			do {
				final StringBuffer sb = new StringBuffer();
				sb.append(dsTable.getModifiedName());
				sb.append(Resources.get("tablenameSubSep"));
				if (partitionValue == null
						|| !partitionValue
								.equals(GenericConstructorRunnable.NO_PARTITION)) {
					sb.append(this.getSanitizedPartitionValue(partitionValue));
					sb.append(Resources.get("tablenameSubSep"));
				}
				if (optExprCol != null) {
					sb.append(optExprCol.getModifiedName());
					sb.append(Resources.get("tablenameSubSep"));
				}
				if (++counter > 0) {
					sb.append("" + counter);
					sb.append(Resources.get("tablenameSubSep"));
				}
				sb
						.append(oType.equals(DataSetOptimiserType.COLUMN_BOOL)
								|| oType
										.equals(DataSetOptimiserType.COLUMN_BOOL_INHERIT)
								|| oType
										.equals(DataSetOptimiserType.TABLE_BOOL)
								|| oType
										.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT) ? Resources
								.get("boolColSuffix")
								: Resources.get("countColSuffix"));
				name = sb.toString();
			} while (((Collection) this.uniqueOptCols.get(parent))
					.contains(name));
			// Store the name above in the unique list for the parent.
			((Collection) this.uniqueOptCols.get(parent)).add(name);
			return name;
		}

		private String getSanitizedPartitionValue(final String partitionValue) {
			return ("" + partitionValue).replaceAll("\\W+", "_").replaceAll(
					"__+", "_").replaceAll("(^_+|_+$)", "");
		}

		private String getFinalName(final String schemaPartitionPrefix,
				final DataSetTable dsTable, final String partitionValue) {
			final StringBuffer finalName = new StringBuffer();
			if (schemaPartitionPrefix != null) {
				finalName.append(schemaPartitionPrefix);
				finalName.append(Resources.get("tablenameSubSep"));
			}
			finalName.append(dsTable.getSchema().getName());
			finalName.append(Resources.get("tablenameSep"));
			finalName.append(dsTable.getModifiedName());
			if (dsTable.getType().equals(DataSetTableType.MAIN)) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("mainSuffix"));
			} else if (dsTable.getType().equals(DataSetTableType.MAIN_SUBCLASS)) {
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("subclassSuffix"));
			} else if (dsTable.getType().equals(DataSetTableType.DIMENSION)) {
				if (partitionValue == null
						|| !partitionValue
								.equals(GenericConstructorRunnable.NO_PARTITION)) {
					finalName.append(Resources.get("tablenameSubSep"));
					// Substitute non-[char/number/underscore] symbols with
					// underscores, and replace multiple underscores with
					// single ones.
					finalName.append(this
							.getSanitizedPartitionValue(partitionValue));
				}
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("dimensionSuffix"));
			} else
				throw new BioMartError();
			// Remove any non-[char/number/underscore] symbols.
			return finalName.toString().replaceAll("\\W+", "");
		}

		private void issueListenerEvent(final int event) throws Exception {
			this.issueListenerEvent(event, null);
		}

		private void issueListenerEvent(final int event, final Object data)
				throws Exception {
			this.issueListenerEvent(event, data, null);
		}

		private void issueListenerEvent(final int event, final Object data,
				final MartConstructorAction action) throws Exception {
			Log.debug("Event issued: event:" + event + " data:" + data
					+ " action:" + action);
			for (final Iterator i = this.martConstructorListeners.iterator(); i
					.hasNext();)
				((MartConstructorListener) i.next())
						.martConstructorEventOccurred(event, data, action);
		}

		public void addMartConstructorListener(
				final MartConstructorListener listener) {
			Log.debug("Listener added to constructor runnable");
			this.martConstructorListeners.add(listener);
		}

		public void cancel() {
			Log.debug("Constructor runnable cancelled");
			this.cancelled = true;
		}

		public Exception getFailureException() {
			return this.failure;
		}

		public int getPercentComplete() {
			return (int) this.percentComplete;
		}

		public String getStatusMessage() {
			return this.statusMessage;
		}

		public void run() {
			Log.info(Resources.get("logConstructorStarted"));
			try {
				// Begin.
				Log.debug("Construction started");
				this
						.issueListenerEvent(MartConstructorListener.CONSTRUCTION_STARTED);

				// Work out how many datasets we have.
				final int totalDataSetCount = this.datasets.size();

				for (final Iterator j = this.datasets.iterator(); j.hasNext();) {
					// Loop over all the datasets we want included from this
					// mart. Build actions for each one.
					final DataSet ds = (DataSet) j.next();
					try {
						this.issueListenerEvent(
								MartConstructorListener.DATASET_STARTED, ds
										.getName());
						this.makeActionsForDataset(ds, totalDataSetCount);
					} catch (final Throwable t) {
						throw t;
					} finally {
						// Make sure the helper always gets a chance to
						// tidy up, even if an exception is thrown.
						this.issueListenerEvent(
								MartConstructorListener.DATASET_ENDED, ds
										.getName());
					}
				}
				this
				.issueListenerEvent(MartConstructorListener.CONSTRUCTION_ENDED);
				Log.info(Resources.get("logConstructorEnded"));
			} catch (final ConstructorException e) {
				// This is so the users see a nice message straight away.
				this.failure = e;
			} catch (final Throwable t) {
				this.failure = new ConstructorException(t);
			}
		}
	}

	/**
	 * This interface defines a listener which hears events about mart
	 * construction. The events are defined as constants in this interface. The
	 * listener will take these events and either build scripts for later
	 * execution, or will execute them directly in order to physically construct
	 * the mart.
	 */
	public interface MartConstructorListener {

		/**
		 * This event will occur when an action needs performing, and will be
		 * accompanied by a {@link MartConstructorAction} object describing what
		 * needs doing.
		 */
		public static final int ACTION_EVENT = 0;

		/**
		 * This event will occur when mart construction ends.
		 */
		public static final int CONSTRUCTION_ENDED = 1;

		/**
		 * This event will occur when mart construction begins.
		 */
		public static final int CONSTRUCTION_STARTED = 2;

		/**
		 * This event will occur when an individual dataset ends.
		 */
		public static final int DATASET_ENDED = 3;

		/**
		 * This event will occur when an individual dataset begins.
		 */
		public static final int DATASET_STARTED = 4;

		/**
		 * This event will occur when an individual schema partition ends.
		 */
		public static final int PARTITION_ENDED = 5;

		/**
		 * This event will occur when an individual schema partition begins.
		 */
		public static final int PARTITION_STARTED = 6;

		/**
		 * This method will be called when an event occurs.
		 * 
		 * @param event
		 *            the event that occurred. See the constants defined
		 *            elsewhere in this interface for possible events.
		 * @param data
		 *            ancilliary data, may be null.
		 * @param action
		 *            an action object that belongs to this event. Will be
		 *            <tt>null</tt> in all cases except where the event is
		 *            {@link #ACTION_EVENT}.
		 * @throws Exception
		 *             if anything goes wrong whilst handling the event.
		 */
		public void martConstructorEventOccurred(int event, Object data,
				MartConstructorAction action) throws Exception;
	}
}
