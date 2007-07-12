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
import java.util.Set;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.exceptions.ListenerException;
import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.PartitionTable;
import org.biomart.builder.model.TransformationUnit;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
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
import org.biomart.builder.model.PartitionTable.PartitionColumn;
import org.biomart.builder.model.PartitionTable.PartitionTableApplication;
import org.biomart.builder.model.PartitionTable.PartitionTableApplication.PartitionAppliedRow;
import org.biomart.builder.model.SchemaModificationSet.RestrictedRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedTableDefinition;
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
				final int totalDataSetCount) throws ListenerException,
				ValidationException, ConstructorException, SQLException,
				PartitionException {
			Log.debug("Making actions for dataset " + dataset);
			// Check not cancelled.
			this.checkCancelled();

			// Find out the main table source schema.
			final Schema templateSchema = dataset.getCentralTable().getSchema();
			final PartitionTableApplication dsPta = dataset.getMart()
					.getPartitionTableForDataSet(dataset);

			// Is it partitioned?
			Collection schemaPartitions = templateSchema.getPartitions()
					.entrySet();
			if (schemaPartitions.isEmpty()) {
				Log.debug("Using dummy empty partition");
				schemaPartitions = new ArrayList();
				schemaPartitions.add(new Map.Entry() {
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
					/ schemaPartitions.size();
			if (dsPta != null)
				stepPercent /= ((PartitionAppliedRow) dsPta
						.getPartitionAppliedRows().get(0)).getCompound();

			// Divide the progress step size by the number of datasets.
			stepPercent /= totalDataSetCount;

			// Process the tables.
			for (final Iterator s = schemaPartitions.iterator(); s.hasNext();) {
				final Map.Entry schemaPartition = (Map.Entry) s.next();
				final Set droppedTables = new HashSet();
				// Clear out optimiser col names so that they start
				// again on this partition.
				this.uniqueOptCols.clear();

				Log.debug("Starting schema partition " + schemaPartition);
				this.issueListenerEvent(
						MartConstructorListener.PARTITION_STARTED,
						schemaPartition.getKey());

				// Loop over dataset partitions.
				boolean fakeDSPartition = dsPta == null;
				if (!fakeDSPartition)
					dsPta.getNamePartitionCol().getPartitionTable()
							.prepareRows((String) schemaPartition.getKey(),
									PartitionTable.UNLIMITED_ROWS);
				while (fakeDSPartition ? true : dsPta != null
						&& dsPta.getPartitionTable().nextRow()) {
					fakeDSPartition = false;
					// Make more specific.
					String partitionedDataSetName = dataset.getName();
					if (dsPta != null)
						partitionedDataSetName = dsPta.getNamePartitionCol()
								.getValueForRow(
										dsPta.getPartitionTable().currentRow())
								+ Resources.get("tablenameSubSep")
								+ partitionedDataSetName;
					this.issueListenerEvent(
							MartConstructorListener.DATASET_STARTED,
							partitionedDataSetName);
					for (final Iterator i = this.getTablesToProcess(dataset)
							.iterator(); i.hasNext();) {
						final DataSetTable dsTable = (DataSetTable) i.next();
						if (!droppedTables.contains(dsTable.getParent())) {
							// Loop over dataset table partitions.
							final PartitionTableApplication dmPta = dataset
									.getMart().getPartitionTableForDimension(
											dsTable);
							boolean fakeDMPartition = dmPta == null;
							if (!fakeDMPartition)
								dmPta.getNamePartitionCol().getPartitionTable()
										.prepareRows(
												(String) schemaPartition
														.getKey(),
												PartitionTable.UNLIMITED_ROWS);
							while (fakeDMPartition ? true : dmPta != null
									&& dmPta.getPartitionTable().nextRow()) {
								fakeDMPartition = false;
								if (!this.makeActionsForDatasetTable(
										templateSchema,
										(String) schemaPartition.getKey(),
										(String) schemaPartition.getValue(),
										dsPta, dmPta, dataset, dsTable))
									droppedTables.add(dsTable);
							}
						}

						// Update the progress percentage once per table.
						this.percentComplete += stepPercent;

						// Check not cancelled.
						this.checkCancelled();
					}
					this.issueListenerEvent(
							MartConstructorListener.DATASET_ENDED,
							partitionedDataSetName);
				}

				this.issueListenerEvent(
						MartConstructorListener.PARTITION_ENDED,
						schemaPartition.getKey());
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
									.isMaskedTable(dsTab)
							&& (dsTab.getFocusRelation() != null && !dataset
									.getSchemaModifications().isMergedRelation(
											dsTab.getFocusRelation())))
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

		private boolean makeActionsForDatasetTable(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta, final DataSet dataset,
				final DataSetTable dsTable) throws ListenerException,
				SQLException, PartitionException {
			Log.debug("Creating actions for table " + dsTable);
			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsPta, dmPta, dsTable);
			final String tempName = "TEMP";
			String previousTempTable = null;
			boolean firstJoin = true;
			boolean requiresFinalLeftJoin = false;
			final Set droppedCols = new HashSet();

			// Use the transformation units to create the basic table.
			Relation firstJoinRel = null;
			for (final Iterator j = dsTable.getTransformationUnits().iterator(); j
					.hasNext();) {
				final TransformationUnit tu = (TransformationUnit) j.next();
				final String tempTable = tempName + this.tempNameCount++;

				// Translate TU to Action.
				// Expression?
				if (tu instanceof Expression) {
					if (!this.doExpression(schemaPrefix, dsPta, dmPta, dataset,
							dsTable, (Expression) tu, previousTempTable,
							tempTable, droppedCols))
						// Skip to next action to prevent non-existent
						// new temp table from getting dropped.
						continue;
				}
				// Left-join?
				else if (tu instanceof JoinTable) {
					if (firstJoinRel == null)
						firstJoinRel = ((JoinTable) tu).getSchemaRelation();
					requiresFinalLeftJoin |= this.doJoinTable(templateSchema,
							schemaPartition, schemaPrefix, dsPta, dmPta,
							dataset, dsTable, (JoinTable) tu, firstJoinRel,
							previousTempTable, tempTable, droppedCols);
				}

				// Select-from?
				else if (tu instanceof SelectFromTable)
					this.doSelectFromTable(templateSchema, schemaPartition,
							schemaPrefix, dsPta, dmPta, dataset, dsTable,
							(SelectFromTable) tu, tempTable);

				else
					throw new BioMartError();

				if (previousTempTable != null) {
					final Drop action = new Drop(this.datasetSchemaName,
							finalCombinedName);
					action.setTable(previousTempTable);
					this.issueAction(action);
				}

				if (tu instanceof JoinTable && !firstJoin) {
					if (!droppedCols.isEmpty()
							&& !dsTable.getType().equals(DataSetTableType.MAIN)) {
						// If first join of non-MAIN table produced droppedCols
						// then the target table does not exist and the entire
						// non-MAIN table can be dropped. This also means that
						// if this is SUBCLASS then all its DMS and further
						// SUBCLASS tables can be ignored.
						final Drop action = new Drop(this.datasetSchemaName,
								finalCombinedName);
						action.setTable(tempTable);
						this.issueAction(action);
						return false;
					}
					// Don't repeat this check.
					firstJoin = false;
				}

				// Update the previous table.
				previousTempTable = tempTable;
			}

			// Do final set of actions for table once per partition.
			final String finalName = this.getFinalName(schemaPrefix, dsPta,
					dmPta, dsTable);

			// Do a final left-join against the parent to reinstate
			// any potentially missing rows.
			if (requiresFinalLeftJoin
					&& !dsTable.getType().equals(DataSetTableType.MAIN)) {
				final String tempTable = tempName + this.tempNameCount++;
				this.doParentLeftJoin(schemaPrefix, dsPta, dmPta, dataset,
						dsTable, finalCombinedName, previousTempTable,
						tempTable, droppedCols);
				previousTempTable = tempTable;
			}

			// Does it need a final distinct?
			if (dataset.getDataSetModifications().isDistinctTable(dsTable)) {
				final String tempTable = tempName + this.tempNameCount++;
				this
						.doDistinct(finalCombinedName, previousTempTable,
								tempTable);
				previousTempTable = tempTable;
			}

			// Drop masked dependencies and create column indices.
			final List dropCols = new ArrayList();
			for (final Iterator x = dsTable.getColumns().iterator(); x
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) x.next();
				if (col.isRequiredInterim() && !col.isRequiredFinal())
					dropCols.add(col.getPartitionedName());
				// Create index if required.
				else if (!droppedCols.contains(col.getPartitionedName())
						&& dataset.getDataSetModifications().isIndexedColumn(
								col)) {
					final Index index = new Index(this.datasetSchemaName,
							finalCombinedName);
					index.setTable(previousTempTable);
					index.setColumns(Collections.singletonList(col
							.getPartitionedName()));
					this.issueAction(index);
				}
			}
			if (!dropCols.isEmpty()) {
				final DropColumns dropcol = new DropColumns(
						this.datasetSchemaName, finalCombinedName);
				dropcol.setTable(previousTempTable);
				dropcol.setColumns(dropCols);
				this.issueAction(dropcol);
			}

			// Add a rename action to produce the final table.
			final Rename action = new Rename(this.datasetSchemaName,
					finalCombinedName);
			action.setFrom(previousTempTable);
			action.setTo(finalName);
			this.issueAction(action);

			// Create indexes on all keys on the final table.
			for (final Iterator j = dsTable.getKeys().iterator(); j.hasNext();) {
				final Key key = (Key) j.next();
				final List keyCols = new ArrayList();
				for (final Iterator k = key.getColumns().iterator(); k
						.hasNext();)
					keyCols
							.add(((DataSetColumn) k.next())
									.getPartitionedName());
				final Index index = new Index(this.datasetSchemaName,
						finalCombinedName);
				index.setTable(finalName);
				index.setColumns(keyCols);
				this.issueAction(index);
			}

			// Create optimiser columns - either count or bool,
			// or none if not required.
			// If this is a subclass table, then the optimiser
			// type is always COUNT_INHERIT or COUNT_INHERIT_TABLE.
			final DataSetOptimiserType oType = dsTable.getType().equals(
					DataSetTableType.MAIN_SUBCLASS) ? dataset
					.getDataSetOptimiserType().isTable() ? DataSetOptimiserType.TABLE_INHERIT
					: DataSetOptimiserType.COLUMN_INHERIT
					: dataset.getDataSetOptimiserType();
			this.doOptimiseTable(schemaPrefix, dsPta, dmPta, dataset, dsTable,
					oType, !dsTable.getType()
							.equals(DataSetTableType.DIMENSION)
							&& dataset.getDataSetOptimiserType().isTable());
			return true;
		}

		private void doParentLeftJoin(final String schemaPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta, final DataSet dataset,
				final DataSetTable dsTable, final String finalCombinedName,
				final String previousTempTable, final String tempTable,
				final Set droppedCols) throws ListenerException,
				PartitionException {
			// Work out the parent table.
			final DataSetTable parent = dsTable.getParent();
			// Work out what columns to take from each side.
			final List leftJoinCols = new ArrayList();
			final List leftSelectCols = leftJoinCols;
			final List rightJoinCols = leftJoinCols;
			final List rightSelectCols = new ArrayList();
			for (final Iterator x = parent.getPrimaryKey().getColumns()
					.iterator(); x.hasNext();)
				leftJoinCols.add(((DataSetColumn) x.next())
						.getPartitionedName());
			for (final Iterator x = dsTable.getColumns().iterator(); x
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) x.next();
				if (col.isRequiredInterim())
					rightSelectCols.add(col.getPartitionedName());
			}
			rightSelectCols.removeAll(rightJoinCols);
			rightSelectCols.removeAll(droppedCols);
			// Add to rightSelectCols all the has columns for this table.
			final Collection hasCols = dataset.getDataSetOptimiserType()
					.isTable() ? null : (Collection) this.uniqueOptCols
					.get(dsTable);
			if (hasCols != null)
				rightSelectCols.addAll(hasCols);
			// Index the left-hand side of the join.
			final Index index = new Index(this.datasetSchemaName,
					finalCombinedName);
			index.setTable(previousTempTable);
			index.setColumns(leftJoinCols);
			this.issueAction(index);
			// Make the join.
			final LeftJoin action = new LeftJoin(this.datasetSchemaName,
					finalCombinedName);
			action.setLeftTable(this.getFinalName(schemaPrefix, dsPta, dmPta,
					parent));
			action.setRightSchema(this.datasetSchemaName);
			action.setRightTable(previousTempTable);
			action.setLeftJoinColumns(leftJoinCols);
			action.setRightJoinColumns(rightJoinCols);
			action.setLeftSelectColumns(leftSelectCols);
			action.setRightSelectColumns(rightSelectCols);
			action.setResultTable(tempTable);
			this.issueAction(action);
			// Drop the old one.
			final Drop drop = new Drop(this.datasetSchemaName,
					finalCombinedName);
			drop.setTable(previousTempTable);
			this.issueAction(drop);
		}

		private void doDistinct(final String finalCombinedName,
				final String previousTempTable, final String tempTable)
				throws ListenerException {
			// Make the join.
			final Distinct action = new Distinct(this.datasetSchemaName,
					finalCombinedName);
			action.setSchema(this.datasetSchemaName);
			action.setTable(previousTempTable);
			action.setResultTable(tempTable);
			this.issueAction(action);
			// Drop the old one.
			final Drop drop = new Drop(this.datasetSchemaName,
					finalCombinedName);
			drop.setTable(previousTempTable);
			this.issueAction(drop);
		}

		private void doOptimiseTable(final String schemaPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta, final DataSet dataset,
				final DataSetTable dsTable, final DataSetOptimiserType oType,
				final boolean createTable) throws ListenerException,
				PartitionException {
			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsPta, dmPta, dsTable);
			if (createTable) {
				// Tables are same name, but use 'bool' or 'count'
				// instead of 'main'
				final String optTable = this.getOptimiserTableName(
						schemaPrefix, dsPta, dmPta, dsTable, dataset
								.getDataSetOptimiserType());
				// The key cols are those from the primary key.
				final List keyCols = new ArrayList();
				for (final Iterator y = dsTable.getPrimaryKey().getColumns()
						.iterator(); y.hasNext();)
					keyCols
							.add(((DataSetColumn) y.next())
									.getPartitionedName());

				// Work out the parent.
				final DataSetTable parent = dsTable.getType().equals(
						DataSetTableType.MAIN_SUBCLASS) ? dsTable.getParent()
						: null;

				// Create the table by selecting the pk.
				final CreateOptimiser create = new CreateOptimiser(
						this.datasetSchemaName, finalCombinedName);
				create.setKeyColumns(keyCols);
				create.setOptTableName(optTable);
				if (parent != null) {
					// The key cols are those from the primary key.
					final List parentKeyCols = new ArrayList();
					for (final Iterator y = parent.getPrimaryKey().getColumns()
							.iterator(); y.hasNext();)
						parentKeyCols.add(((DataSetColumn) y.next())
								.getPartitionedName());
					create.setCopyKey(parentKeyCols);
					create.setCopyTable(this.getOptimiserTableName(
							schemaPrefix, dsPta, dmPta, parent, dataset
									.getDataSetOptimiserType()));
				}
				this.issueAction(create);

				// Index the pk on the new table.
				final Index index = new Index(this.datasetSchemaName,
						finalCombinedName);
				index.setTable(optTable);
				index.setColumns(keyCols);
				this.issueAction(index);
			}
			if (!dsTable.getType().equals(DataSetTableType.MAIN)) {
				// Work out the dimension/subclass parent.
				final DataSetTable parent = dsTable.getParent();
				// Set up the column on the dimension parent.
				final String optTable = this.getOptimiserTableName(
						schemaPrefix, dsPta, dmPta, parent, dataset
								.getDataSetOptimiserType());
				// Key columns are primary key cols from parent.
				// Do a left-join update. We're looking for rows
				// where at least one child non-key col is non-null.
				final List keyCols = new ArrayList();
				for (final Iterator y = parent.getPrimaryKey().getColumns()
						.iterator(); y.hasNext();)
					keyCols
							.add(((DataSetColumn) y.next())
									.getPartitionedName());
				final List nonNullCols = new ArrayList();
				for (final Iterator y = dsTable.getColumns().iterator(); y
						.hasNext();) {
					final DataSetColumn col = (DataSetColumn) y.next();
					// We won't select masked cols as they won't be in
					// the final table, and we won't select expression
					// columns as they can genuinely be null.
					if (!dataset.getDataSetModifications().isMaskedColumn(col)
							&& !(col instanceof ExpressionColumn))
						nonNullCols.add(col.getPartitionedName());
				}
				nonNullCols.removeAll(keyCols);

				// Columns are dimension table names with '_bool' or
				// '_count' appended.
				final String optCol = this.getOptimiserColumnName(dsPta, dmPta,
						parent, dsTable, oType);

				// Do the bool/count update.
				final UpdateOptimiser update = new UpdateOptimiser(
						this.datasetSchemaName, finalCombinedName);
				update.setKeyColumns(keyCols);
				update.setNonNullColumns(nonNullCols);
				update.setSourceTableName(this.getFinalName(schemaPrefix,
						dsPta, dmPta, dsTable));
				update.setOptTableName(optTable);
				update.setOptColumnName(optCol);
				update.setCountNotBool(!oType.isBool());
				update.setNullNotZero(!oType.isUseNull());
				this.issueAction(update);

				// Index the column if required.
				if (dataset.isIndexOptimiser()) {
					final Index index = new Index(this.datasetSchemaName,
							finalCombinedName);
					index.setTable(optTable);
					index.setColumns(Collections.singletonList(optCol));
					this.issueAction(index);
				}
			}
		}

		private void doSelectFromTable(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta, final DataSet dataset,
				final DataSetTable dsTable, final SelectFromTable stu,
				final String tempTable) throws SQLException, ListenerException,
				PartitionException {

			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsPta, dmPta, dsTable);
			final Select action = new Select(this.datasetSchemaName,
					finalCombinedName);

			// If this is a dimension, look up DM PT,
			// otherwise if this is the main table, look up DS PT,
			// otherwise don't do it at all.
			PartitionTableApplication pta = null;
			if (dsTable.getType().equals(DataSetTableType.DIMENSION)
					&& dmPta != null)
				pta = dmPta;
			else if (dsTable.getType().equals(DataSetTableType.MAIN)
					&& dsPta != null)
				pta = dsPta;
			if (pta != null) {
				// This is a select, so we are dealing with the first row
				// only.
				final PartitionAppliedRow prow = (PartitionAppliedRow) pta
						.getPartitionAppliedRows().get(0);
				// The naming column will also always be the first row,
				// which will be on pta itself, so we don't need to
				// initialise the table as it has already been done.
				final PartitionColumn pcol = pta.getPartitionTable()
						.getSelectedColumn(prow.getPartitionCol());
				// For each of the getNewColumnNameMap cols that are in the
				// current ptable application, add a restriction for that col
				// using current ptable column value.
				for (final Iterator i = stu.getNewColumnNameMap().entrySet()
						.iterator(); i.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final DataSetColumn dsCol = (DataSetColumn) entry
							.getValue();
					// Only apply this to the dsCol which matches
					// the partition row's ds col.
					if (dsCol.getName().equals(prow.getRootDataSetCol())
							|| dsCol.getName().endsWith(
									Resources.get("columnnameSep")
											+ prow.getRootDataSetCol()))
						// Apply restriction.
						action.getPartitionRestrictions().put(
								entry.getKey(),
								pcol.getValueForRow(pcol.getPartitionTable()
										.currentRow()));
				}
				// PrepareRow on subdivision, if any.
				if (pta.getPartitionAppliedRows().size() > 1) {
					final PartitionAppliedRow subprow = (PartitionAppliedRow) pta
							.getPartitionAppliedRows().get(1);
					pta.getPartitionTable().getSelectedColumn(
							subprow.getNamePartitionCol()).getPartitionTable()
							.prepareRows(schemaPartition,
									PartitionTable.UNLIMITED_ROWS);
				}
			}

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
				if (schema == null) // Can never happen.
					throw new BioMartError();
			}
			// Source tables are always main or subclass and
			// therefore are never partitioned.
			final String table = sourceTable instanceof DataSetTable ? this
					.getFinalName(schemaPrefix, dsPta, dmPta,
							(DataSetTable) sourceTable) : stu.getTable()
					.getName();
			final Map selectCols = new HashMap();
			// Select columns from parent table.
			for (final Iterator k = stu.getNewColumnNameMap().entrySet()
					.iterator(); k.hasNext();) {
				final Map.Entry entry = (Map.Entry) k.next();
				final DataSetColumn col = (DataSetColumn) entry.getValue();
				col.fixPartitionedName();
				if (col.isRequiredInterim())
					selectCols
							.put(
									sourceTable instanceof DataSetTable ? ((DataSetColumn) sourceTable
											.getColumnByName((String) entry
													.getKey()))
											.getPartitionedName()
											: entry.getKey(), col
											.getPartitionedName());

			}
			// Add to selectCols all the inherited has columns, if
			// this is not a dimension table and the optimiser type is not a
			// table one.
			if (!dataset.getDataSetOptimiserType().isTable()
					&& sourceTable instanceof DataSetTable
					&& !dsTable.getType().equals(DataSetTableType.DIMENSION)) {
				final Collection hasCols = (Collection) this.uniqueOptCols
						.get(sourceTable);
				if (hasCols != null) {
					for (final Iterator k = hasCols.iterator(); k.hasNext();) {
						final String hasCol = (String) k.next();
						selectCols.put(hasCol, hasCol);
					}
					// Make inherited copies.
					this.uniqueOptCols.put(dsTable, new HashSet(hasCols));
				}
			}
			// Do the select.
			action.setSchema(schema);
			action.setTable(table);
			action.setSelectColumns(selectCols);
			action.setResultTable(tempTable);

			// Table restriction.
			if (dataset.getSchemaModifications().isRestrictedTable(dsTable,
					stu.getTable())) {
				final RestrictedTableDefinition def = dataset
						.getSchemaModifications().getRestrictedTable(dsTable,
								stu.getTable());
				action.setTableRestriction(def);
			}
			this.issueAction(action);
		}

		private boolean doJoinTable(final Schema templateSchema,
				final String schemaPartition, final String schemaPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta, final DataSet dataset,
				final DataSetTable dsTable, final JoinTable ljtu,
				final Relation firstJoinRel, final String previousTempTable,
				final String tempTable, final Set droppedCols)
				throws SQLException, ListenerException, PartitionException {

			boolean requiresFinalLeftJoin = false;
			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsPta, dmPta, dsTable);
			final Join action = new Join(this.datasetSchemaName,
					finalCombinedName);

			PartitionTableApplication pta = null;
			if (dsTable.getType().equals(DataSetTableType.DIMENSION)
					&& dmPta != null)
				pta = dmPta;
			else if (dsTable.getType().equals(DataSetTableType.MAIN)
					&& dsPta != null)
				pta = dsPta;
			if (pta != null) {
				// If this is first relation after select table
				// (note first relation, not first join) then apply
				// next row to any subdiv table present.
				if (pta.getPartitionAppliedRows().size() > 1) {
					final PartitionAppliedRow prow = (PartitionAppliedRow) pta
							.getPartitionAppliedRows().get(1);
					// A test to see if this is the first relation
					// after the select (regardless of how many times this
					// relation has been seen).
					boolean nudgeRow = firstJoinRel.equals(ljtu
							.getSchemaRelation());
					if (nudgeRow)
						pta.getPartitionTable().getSelectedColumn(
								prow.getNamePartitionCol()).getPartitionTable()
								.nextRow();
				}
				// For all relations, if this is the one
				// that some subdiv partition applies to, then apply it.
				// This is a join, so we look up row by relation.
				final PartitionAppliedRow prow = pta
						.getAppliedRowForRelation(ljtu.getSchemaRelation());
				// It might not have one after all.
				if (prow != null) {
					// Look up the table that the naming column is on. It
					// will be a subtable which needs initialising on the
					// first pass, and next rowing on all passes.
					final PartitionColumn pcol = pta.getPartitionTable()
							.getSelectedColumn(prow.getPartitionCol());
					final PartitionTable ptbl = pcol.getPartitionTable();
					// For each of the getNewColumnNameMap cols that are in the
					// current ptable application, add a restriction for that
					// col using current ptable column value.
					for (final Iterator i = ljtu.getNewColumnNameMap()
							.entrySet().iterator(); i.hasNext();) {
						final Map.Entry entry = (Map.Entry) i.next();
						final DataSetColumn dsCol = (DataSetColumn) entry
								.getValue();
						// Only apply this to the dsCol which matches
						// the partition row's ds col.
						if (dsCol.getName().equals(prow.getRootDataSetCol())
								|| dsCol.getName().endsWith(
										Resources.get("columnnameSep")
												+ prow.getRootDataSetCol()))
							// Apply restriction.
							action.getPartitionRestrictions().put(
									entry.getKey(),
									pcol.getValueForRow(ptbl.currentRow()));
					}
				}
			}

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
				if (rightSchema == null) {
					droppedCols.addAll(ljtu.getNewColumnNameMap().values());
					return false;
				}
			}
			final String rightTable = ljtu.getTable().getName();
			final List leftJoinCols = new ArrayList();
			final List rightJoinCols = ljtu.getSchemaRelation().getOtherKey(
					ljtu.getSchemaSourceKey()).getColumnNames();
			final Map selectCols = new HashMap();
			// Populate vars.
			for (final Iterator k = ljtu.getSourceDataSetColumns().iterator(); k
					.hasNext();) {
				final String joinCol = ((DataSetColumn) k.next())
						.getPartitionedName();
				if (droppedCols.contains(joinCol)) {
					droppedCols.addAll(ljtu.getNewColumnNameMap().values());
					return false;
				} else
					leftJoinCols.add(joinCol);
			}
			for (final Iterator k = ljtu.getNewColumnNameMap().entrySet()
					.iterator(); k.hasNext();) {
				final Map.Entry entry = (Map.Entry) k.next();
				final DataSetColumn col = (DataSetColumn) entry.getValue();
				col.fixPartitionedName();
				if (col.isRequiredInterim())
					selectCols.put(entry.getKey(), col.getPartitionedName());
			}
			// Index the left-hand side of the join.
			final Index index = new Index(this.datasetSchemaName,
					finalCombinedName);
			index.setTable(previousTempTable);
			index.setColumns(leftJoinCols);
			this.issueAction(index);
			// Make the join.
			action.setLeftTable(previousTempTable);
			action.setRightSchema(rightSchema);
			action.setRightTable(rightTable);
			action.setLeftJoinColumns(leftJoinCols);
			action.setRightJoinColumns(rightJoinCols);
			action.setSelectColumns(selectCols);
			action.setResultTable(tempTable);

			// Table restriction.
			if (dataset.getSchemaModifications().isRestrictedTable(dsTable,
					ljtu.getTable())) {
				final RestrictedTableDefinition def = dataset
						.getSchemaModifications().getRestrictedTable(dsTable,
								ljtu.getTable());
				action.setTableRestriction(def);
				requiresFinalLeftJoin |= def.isHard();
			}
			// Don't add restriction if loopback relation from M end.
			final boolean loopback = ljtu.getSchemaRelation().isOneToMany()
					&& ljtu.getSchemaSourceKey().equals(
							ljtu.getSchemaRelation().getOneKey());
			if (!loopback
					&& dataset.getSchemaModifications().isRestrictedRelation(
							dsTable, ljtu.getSchemaRelation(),
							ljtu.getSchemaRelationIteration())) {
				final RestrictedRelationDefinition def = dataset
						.getSchemaModifications().getRestrictedRelation(
								dsTable, ljtu.getSchemaRelation(),
								ljtu.getSchemaRelationIteration());
				// Add the restriction.
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

		private boolean doExpression(final String schemaPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta, final DataSet dataset,
				final DataSetTable dsTable, final Expression etu,
				final String previousTempTable, final String tempTable,
				final Set droppedCols) throws ListenerException,
				PartitionException {

			final String finalCombinedName = this.getFinalName(schemaPrefix,
					dsPta, dmPta, dsTable);
			// Some useful stuff.
			boolean useXTable = false;
			final String xTableName = tempTable + "X";

			// Work out what columns we can select in the first group.
			final Collection selectCols = new HashSet();
			for (final Iterator z = dsTable.getColumns().iterator(); z
					.hasNext();) {
				final DataSetColumn col = (DataSetColumn) z.next();
				final String colName = col.getPartitionedName();
				if (col.isRequiredInterim() && !droppedCols.contains(colName)
						&& !(col instanceof ExpressionColumn))
					selectCols.add(colName);
			}
			// Add to selectCols all the has columns for this table.
			final Collection hasCols = dataset.getDataSetOptimiserType()
					.isTable() ? null : (Collection) this.uniqueOptCols
					.get(dsTable);
			if (hasCols != null)
				selectCols.addAll(hasCols);

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
					// If it refers to any dropped columns, drop the expression
					// column too.
					boolean usesDroppedCols = false;
					for (final Iterator k = expr.getAliases().keySet()
							.iterator(); k.hasNext() && !usesDroppedCols;) {
						final String exprAlias = (String) k.next();
						if (droppedCols.contains(exprAlias)) {
							droppedCols.add(expCol.getPartitionedName());
							usesDroppedCols = true;
						}
					}
					if (usesDroppedCols)
						continue;
					// Otherwise, work out group-by stuff.
					if (expr.isGroupBy()) {
						for (final Iterator x = dsTable.getColumns().iterator(); x
								.hasNext();) {
							final DataSetColumn col = (DataSetColumn) x.next();
							final String colName = col.getPartitionedName();
							if (expr.getAliases().keySet().contains(
									col.getName())
									|| !selectCols.contains(colName))
								continue;
							groupByCols.add(colName);
						}
						// Make sure group-by doesn't drop the has cols.
						if (hasCols != null)
							groupByCols.addAll(hasCols);
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
				action.setTable(useXTable ? xTableName : previousTempTable);
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

		private void issueAction(final MartConstructorAction action)
				throws ListenerException {
			// Execute the action.
			this.statusMessage = action.getStatusMessage();
			this.issueListenerEvent(MartConstructorListener.ACTION_EVENT, null,
					action);
		}

		private String getOptimiserTableName(
				final String schemaPartitionPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta,
				final DataSetTable dsTable, final DataSetOptimiserType oType)
				throws PartitionException {
			final StringBuffer finalName = new StringBuffer();
			if (schemaPartitionPrefix != null) {
				finalName.append(schemaPartitionPrefix);
				finalName.append(Resources.get("tablenameSubSep"));
			}
			if (dsPta != null) {
				final PartitionColumn pcol = dsPta.getNamePartitionCol();
				finalName.append(pcol.getValueForRow(pcol.getPartitionTable()
						.currentRow()));
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
				if (dmPta != null) {
					finalName.append(Resources.get("tablenameSubSep"));
					final PartitionColumn pcol = dmPta.getNamePartitionCol();
					finalName.append(pcol.getValueForRow(pcol
							.getPartitionTable().currentRow()));
				}
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("dimensionSuffix"));
			} else
				throw new BioMartError();
			final String name = finalName.toString().replaceAll("\\W+", "");
			// UC/LC/Mixed?
			switch (((DataSet) dsTable.getSchema()).getMart().getCase()) {
			case Mart.USE_LOWER_CASE:
				return name.toLowerCase();
			case Mart.USE_UPPER_CASE:
				return name.toUpperCase();
			default:
				return name;
			}
		}

		private String getOptimiserColumnName(
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta,
				final DataSetTable parent, final DataSetTable dsTable,
				final DataSetOptimiserType oType) throws PartitionException {
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
				if (dsPta != null) {
					final PartitionColumn pcol = dsPta.getNamePartitionCol();
					sb.append(pcol.getValueForRow(pcol.getPartitionTable()
							.currentRow()));
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
			name = name.replaceAll("\\W+", "");
			// UC/LC/Mixed?
			switch (((DataSet) dsTable.getSchema()).getMart().getCase()) {
			case Mart.USE_LOWER_CASE:
				name = name.toLowerCase();
				break;
			case Mart.USE_UPPER_CASE:
				name = name.toUpperCase();
				break;
			}
			// Store the name above in the unique list for the parent.
			((Collection) this.uniqueOptCols.get(parent)).add(name);
			return name;
		}

		private String getFinalName(final String schemaPartitionPrefix,
				final PartitionTableApplication dsPta,
				final PartitionTableApplication dmPta,
				final DataSetTable dsTable) throws PartitionException {
			final StringBuffer finalName = new StringBuffer();
			if (schemaPartitionPrefix != null) {
				finalName.append(schemaPartitionPrefix);
				finalName.append(Resources.get("tablenameSubSep"));
			}
			if (dsPta != null) {
				final PartitionColumn pcol = dsPta.getNamePartitionCol();
				finalName.append(pcol.getValueForRow(pcol.getPartitionTable()
						.currentRow()));
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
				if (dmPta != null) {
					finalName.append(Resources.get("tablenameSubSep"));
					final PartitionColumn pcol = dmPta.getNamePartitionCol();
					finalName.append(pcol.getValueForRow(pcol
							.getPartitionTable().currentRow()));
				}
				finalName.append(Resources.get("tablenameSep"));
				finalName.append(Resources.get("dimensionSuffix"));
			} else
				throw new BioMartError();
			// Remove any non-[char/number/underscore] symbols.
			final String name = finalName.toString().replaceAll("\\W+", "");
			// UC/LC/Mixed?
			switch (((DataSet) dsTable.getSchema()).getMart().getCase()) {
			case Mart.USE_LOWER_CASE:
				return name.toLowerCase();
			case Mart.USE_UPPER_CASE:
				return name.toUpperCase();
			default:
				return name;
			}
		}

		private void issueListenerEvent(final int event)
				throws ListenerException {
			this.issueListenerEvent(event, null);
		}

		private void issueListenerEvent(final int event, final Object data)
				throws ListenerException {
			this.issueListenerEvent(event, data, null);
		}

		private void issueListenerEvent(final int event, final Object data,
				final MartConstructorAction action) throws ListenerException {
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
						this.makeActionsForDataset(ds, totalDataSetCount);
					} catch (final Throwable t) {
						throw t;
					}
				}
				this
						.issueListenerEvent(MartConstructorListener.CONSTRUCTION_ENDED);
				Log.info("Construction ended");
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
		 * @throws ListenerException
		 *             if anything goes wrong whilst handling the event.
		 */
		public void martConstructorEventOccurred(int event, Object data,
				MartConstructorAction action) throws ListenerException;
	}
}
