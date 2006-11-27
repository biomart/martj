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
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.DataSet.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.DataSet.PartitionedColumnType.ValueCollection;
import org.biomart.builder.model.MartConstructorAction.Concat;
import org.biomart.builder.model.MartConstructorAction.Create;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.ExpressionAddColumns;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.MartConstructorActionGraph;
import org.biomart.builder.model.MartConstructorAction.Merge;
import org.biomart.builder.model.MartConstructorAction.OptimiseAddColumn;
import org.biomart.builder.model.MartConstructorAction.OptimiseCopyColumn;
import org.biomart.builder.model.MartConstructorAction.OptimiseUpdateColumn;
import org.biomart.builder.model.MartConstructorAction.Partition;
import org.biomart.builder.model.MartConstructorAction.PlaceHolder;
import org.biomart.builder.model.MartConstructorAction.Reduce;
import org.biomart.builder.model.MartConstructorAction.RenameTable;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Key.PrimaryKey;
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
 * @since 0.1
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

		private Helper helper;

		private List martConstructorListeners;

		private double percentComplete = 0.0;

		private String statusMessage = Resources.get("mcCreatingGraph");

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
		 * @param helper
		 *            the helper to use in the transformation.
		 */
		public GenericConstructorRunnable(final String datasetSchemaName,
				final Collection datasets, final Helper helper) {
			super();
			Log.debug("Created generic constructor runnable");
			this.datasets = datasets;
			this.helper = helper;
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
			// Find the main table of the dataset.
			DataSetTable dsMainTable = null;
			for (final Iterator i = dataset.getTables().iterator(); i.hasNext()
					&& dsMainTable == null;) {
				final DataSetTable table = (DataSetTable) i.next();
				if (table.getType().equals(DataSetTableType.MAIN))
					dsMainTable = table;
			}
			
			// Check not cancelled.
			this.checkCancelled();

			// We have to make space to store the graph of actions we
			// must carry out.
			final MartConstructorActionGraph actionGraph = new MartConstructorActionGraph();

			// Establish a root action for the graph.
			final MartConstructorAction rootAction = new PlaceHolder(
					this.datasetSchemaName);
			actionGraph.addAction(rootAction);

			// Make a list to hold the virtual (temp) table representations.
			final List vTables = new ArrayList();

			// We process all the tables into actions.
				final Schema rSchema = dsMainTable.getUnderlyingTable()
				.getSchema();

				// Make a list of tables for this schema.
				final List rSchemaVirtualTables = new ArrayList();

				// Process the main table into actions.
				VirtualTable vMainTable = new VirtualTable(dsMainTable, null);
				this.makeActionsForDatasetTable(null, rootAction, actionGraph,
						rSchema, rSchema, vMainTable);
				rSchemaVirtualTables.add(vMainTable);

				// Check not cancelled.
				this.checkCancelled();

				// Set up a map to hold the last actions for each table
				// we encounter. Start off by putting the last action
				// for the main table into the map.
				final Map dsTableLastActions = new HashMap();
				dsTableLastActions.put(dsMainTable, vMainTable
						.getLastActionPerformed());

				// Set up a queue to hold all the subclass and dimension
				// relations we encounter.
				final PrimaryKey dsMainTablePK = dsMainTable.getPrimaryKey();
				final List dsParentRelations = dsMainTablePK == null ? Collections.EMPTY_LIST
						: new ArrayList(dsMainTablePK.getRelations());
				final List vParents = new ArrayList();
				for (int x = 0; x < dsParentRelations.size(); x++)
					vParents.add(vMainTable);

				// Process all main dimensions and subclasses into actions.
				for (int j = 0; j < dsParentRelations.size(); j++) {
					final VirtualTable vParentTable = (VirtualTable) vParents
							.get(j);
					final Relation dsParentRelation = (Relation) dsParentRelations
							.get(j);
					final DataSetTable dsParentTable = (DataSetTable) dsParentRelation
							.getOneKey().getTable();
					final DataSetTable dsChildTable = (DataSetTable) dsParentRelation
							.getManyKey().getTable();

					// Create the subclass or dimension table.
					VirtualTable vChildTable = new VirtualTable(dsChildTable,
							dsParentRelation);
					this.makeActionsForDatasetTable(vParentTable,
							(MartConstructorAction) dsTableLastActions
									.get(dsParentTable), actionGraph,
							rSchema, rSchema, vChildTable);
					rSchemaVirtualTables.add(vChildTable);

					// Add last action for newly created table to the last
					// actions list.
					dsTableLastActions.put(dsChildTable, vChildTable
							.getLastActionPerformed());

					// Add further subclasses and dimensions to queue.
					if (dsChildTable.getPrimaryKey() != null) {
						final Collection dsChildTableRelations = dsChildTable
								.getPrimaryKey().getRelations();
						dsParentRelations.addAll(dsChildTableRelations);
						for (int x = 0; x < dsChildTableRelations.size(); x++)
							vParents.add(vChildTable);
					}

					// Check not cancelled.
					this.checkCancelled();
				}

				// Add all tables for this schema to list of all tables.
				vTables.addAll(rSchemaVirtualTables);

			// Set up a break-point last action that waits for all
			// tables to complete being turned into actions.
			MartConstructorAction prePartitionAction = new PlaceHolder(
					this.datasetSchemaName);
			for (final Iterator i = vTables.iterator(); i.hasNext();)
				prePartitionAction.addParent(((VirtualTable) i.next())
						.getLastActionPerformed());
			actionGraph.addAction(prePartitionAction);

			// Partition all partitioned columns.
			for (final Iterator x = dataset.getTables().iterator(); x.hasNext();)
				for (final Iterator i = ((Table) x.next()).getColumns()
						.iterator(); i.hasNext();) {
					final DataSetColumn dsPartCol = (DataSetColumn) i.next();

					// Skip if not partitioned.
					if (dsPartCol.getPartitionType() == null)
						continue;

					// Make a list to hold actions that dropping the
					// original table depends on.
					final List preDropChildActions = new ArrayList();

					// Look up the VirtualTable(s) containing the parent table.
					final DataSetTable dsParentTable = (DataSetTable) dsPartCol
							.getTable();
					final List vParentTables = new ArrayList();
					for (final Iterator j = vTables.iterator(); j.hasNext();) {
						final VirtualTable vTable = (VirtualTable) j.next();
						if (vTable.getDataSetTable().equals(dsParentTable))
							vParentTables.add(vTable);
					}

					// Work out what partition values we will use.
					final PartitionedColumnType dsPartColType = dsPartCol
							.getPartitionType();
					final Set partitionValues = new HashSet();
					if (dsPartColType instanceof SingleValue)
						partitionValues.add(((SingleValue) dsPartColType)
								.getValue());
					else if (dsPartColType instanceof ValueCollection) {
						partitionValues
								.addAll(((ValueCollection) dsPartColType)
										.getValues());
						if (((ValueCollection) dsPartColType).getIncludeNull())
							partitionValues.add(null);
					} else if (dsPartColType instanceof UniqueValues)
						if (dsPartCol instanceof WrappedColumn)
							partitionValues
									.addAll(this.helper
											.listDistinctValues(((WrappedColumn) dsPartCol)
													.getWrappedColumn()));
						else if (dsPartCol instanceof SchemaNameColumn)
							// Unique values for the schema column are the names
							// of all schemas involved in this dataset table.
							// ie. the schemas for each key in the underlying
							// keys.
							for (final Iterator j = vParentTables.iterator(); j
									.hasNext();) {
								final VirtualTable vTable = (VirtualTable) j
										.next();
								for (final Iterator k = vTable.getDataSet()
										.getTables().iterator(); k.hasNext();)
									for (final Iterator l = ((DataSetTable) k
											.next()).getUnderlyingKeys()
											.iterator(); l.hasNext();) {
										final Key key = (Key) l.next();
										final Schema keySchema = key.getTable()
												.getSchema();
										partitionValues.add(keySchema);
									}
							}
						else
							// Other column types not supported.
							throw new BioMartError();

					// Do the partitioning. First, partition the table the
					// column belongs to. Then, partition every child, and
					// every child of every child, and so on recursively.

					// Construct the recursive list of child tables that will
					// also require partitioning.
					final List dsTablesToPartition = new ArrayList();
					dsTablesToPartition.add(dsParentTable);
					for (int j = 0; j < dsTablesToPartition.size(); j++) {
						final DataSetTable partTable = (DataSetTable) dsTablesToPartition
								.get(j);
						if (partTable.getPrimaryKey() != null)
							for (final Iterator l = partTable.getPrimaryKey()
									.getRelations().iterator(); l.hasNext();)
								dsTablesToPartition.add(((Relation) l.next())
										.getManyKey().getTable());
					}

					// Keep track of the last partition we do on this column.
					MartConstructorAction lastPartitionAction = prePartitionAction;

					// Find all child VirtualTables. Child VirtualTables are
					// those where the dataset table matches any of the
					// tablesToPartition. This will result in grouped schema
					// tables being partitioned for every value in the group,
					// regardless of the values in their individual schemas.
					// Therefore you may end up with some empty partition
					// tables.
					final List vTablesToReduce = new ArrayList();
					for (final Iterator j = dsTablesToPartition.iterator(); j
							.hasNext();) {
						final DataSetTable childDSTable = (DataSetTable) j
								.next();
						if (childDSTable.equals(dsParentTable))
							continue;
						for (final Iterator l = vTables.iterator(); l.hasNext();) {
							final VirtualTable vChildTable = (VirtualTable) l
									.next();
							if (!vChildTable.getDataSetTable().equals(
									childDSTable))
								continue;
							vTablesToReduce.add(vChildTable);
							// Create an index over the foreign key columns of
							// each child table.
							final MartConstructorAction index = new Index(
									this.datasetSchemaName, vChildTable
											.getDataSetTable().getName(), null,
									vChildTable.getTempTableName(), vChildTable
											.getParentDataSetRelation()
											.getManyKey().getColumns());
							actionGraph.addActionWithParent(index,
									lastPartitionAction);
							lastPartitionAction = index;
						}
					}

					// For each parent table to be partitioned...
					for (final Iterator j = vParentTables.iterator(); j
							.hasNext();) {
						final VirtualTable vParentTable = (VirtualTable) j
								.next();

						// Index the column to be partitioned.
						MartConstructorAction index = new Index(
								this.datasetSchemaName, vParentTable
										.getDataSetTable().getName(), null,
								vParentTable.getTempTableName(), Collections
										.singletonList(dsPartCol));
						actionGraph.addActionWithParent(index,
								lastPartitionAction);
						lastPartitionAction = index;

						// Partition the parent table.
						final List reduceActions = new ArrayList();
						// Do the partitioning, one table per value.
						for (final Iterator k = partitionValues.iterator(); k
								.hasNext();) {
							final Object partitionValue = k.next();
							// Create an VirtualTable for the partitioned
							// value table. Add it to the list of all tables.
							final VirtualTable vPartitionTable = new VirtualTable(
									vParentTable.getDataSetTable(),
									vParentTable.getParentDataSetRelation());
							vPartitionTable.getPartitionValues().addAll(
									vParentTable.getPartitionValues());
							vPartitionTable.getPartitionValues().add(
									partitionValue);
							vTables.add(vPartitionTable);
							final Key dsPartTablePK = vParentTable
									.getDataSetTable().getPrimaryKey();
							final List dsPartTablePKCols = dsPartTablePK == null ? Collections.EMPTY_LIST
									: dsPartTablePK.getColumns();
							final Iterator dsPartTableFK = vParentTable
									.getDataSetTable().getForeignKeys()
									.iterator();
							final List dsPartTableFKCols = dsPartTableFK
									.hasNext() ? ((Key) dsPartTableFK.next())
									.getColumns() : Collections.EMPTY_LIST;
							final List dsPartTableCols = vParentTable
									.getDataSetTable()
									.getUnmaskedDataSetColumns();
							// Partition the parent table for this value.
							final MartConstructorAction partition = new Partition(
									this.datasetSchemaName, vPartitionTable
											.getDataSetTable().getName(), null,
									vPartitionTable.getTempTableName(), null,
									vParentTable.getTempTableName(), dsPartCol
											.getName(), partitionValue,
									dsPartTablePKCols, dsPartTableFKCols,
									dsPartTableCols);
							actionGraph.addActionWithParent(partition,
									lastPartitionAction);
							// Make the last action of each partitioned
							// table the partition action.
							vPartitionTable.setLastActionPerformed(partition);
							reduceActions.add(partition);

							// Create an index over the primary key columns of
							// the newly partitioned table, if it has a primary
							// key. Also, if it doesn't have a primary key, then
							// we can't recurse to children, so don't bother.
							if (vParentTable.getDataSetTable().getPrimaryKey() != null) {
								index = new Index(this.datasetSchemaName,
										vPartitionTable.getDataSetTable()
												.getName(), null,
										vPartitionTable.getTempTableName(),
										vParentTable.getDataSetTable()
												.getPrimaryKey().getColumns());
								actionGraph.addActionWithParent(index,
										partition);
								// For each child table, create a new
								// VirtualTable
								// and generate it by doing a Reduce action. Add
								// the new VirtualTable to the list of all
								// tables.
								for (final Iterator l = vTablesToReduce
										.iterator(); l.hasNext();) {
									final VirtualTable vChildTable = (VirtualTable) l
											.next();
									// Create an VirtualTable for the
									// partitioned
									// child table. Add it to the list of all
									// tables.
									final VirtualTable vReducedTable = new VirtualTable(
											vChildTable.getDataSetTable(),
											vChildTable
													.getParentDataSetRelation());
									vReducedTable.getPartitionValues().addAll(
											vChildTable.getPartitionValues());
									vReducedTable.getPartitionValues().add(
											partitionValue);
									vTables.add(vReducedTable);
									// Generate the table.
									final MartConstructorAction reduce = new Reduce(
											this.datasetSchemaName,
											vReducedTable.getDataSetTable()
													.getName(),
											null,
											vReducedTable.getTempTableName(),
											null,
											vPartitionTable.getTempTableName(),
											vChildTable
													.getParentDataSetRelation()
													.getOneKey().getColumns(),
											null,
											vChildTable.getTempTableName(),
											vChildTable
													.getParentDataSetRelation()
													.getManyKey().getColumns(),
											vChildTable
													.getDataSetTable()
													.getUnmaskedDataSetColumns());
									actionGraph.addActionWithParent(reduce,
											index);
									reduceActions.add(reduce);
									vReducedTable
											.setLastActionPerformed(reduce);
								}
							}
						}
						// Drop original parent table.
						final MartConstructorAction drop = new Drop(
								this.datasetSchemaName, vParentTable
										.getDataSetTable().getName(), null,
								vParentTable.getTempTableName());
						actionGraph.addActionWithParents(drop, reduceActions);
						preDropChildActions.addAll(reduceActions);
					}
					vTables.removeAll(vParentTables);

					// Drop original child tables. Reduced ones will have
					// taken their place now.
					for (final Iterator j = vTablesToReduce.iterator(); j
							.hasNext();) {
						final VirtualTable vTable = (VirtualTable) j.next();
						final MartConstructorAction drop = new Drop(
								this.datasetSchemaName, vTable
										.getDataSetTable().getName(), null,
								vTable.getTempTableName());
						actionGraph.addActionWithParents(drop,
								preDropChildActions);
					}
					vTables.removeAll(vTablesToReduce);
				}

			// Set up a break-point last action that waits again for all
			// tables to complete.
			final MartConstructorAction preIndexAction = new PlaceHolder(
					this.datasetSchemaName);
			for (final Iterator i = vTables.iterator(); i.hasNext();)
				preIndexAction.addParent(((VirtualTable) i.next())
						.getLastActionPerformed());
			actionGraph.addAction(preIndexAction);

			// Establish PKs and FKs, but only as indexes.
			final List pkFkActions = new ArrayList();
			pkFkActions.add(preIndexAction);
			for (final Iterator i = vTables.iterator(); i.hasNext();) {
				final VirtualTable vParentTable = (VirtualTable) i.next();
				final PrimaryKey dsParentTablePK = vParentTable
						.getDataSetTable().getPrimaryKey();
				if (dsParentTablePK == null)
					continue;
				final List dsPKCols = dsParentTablePK.getColumns();
				// Create a PK index over pkKeyCols.
				final MartConstructorAction pkIndex = new Index(
						this.datasetSchemaName, vParentTable.getDataSetTable()
								.getName(), null, vParentTable
								.getTempTableName(), dsPKCols);
				actionGraph.addActionWithParent(pkIndex, preIndexAction);
				pkFkActions.add(pkIndex);

				// Iterate over foreign keys from this PK.
				for (final Iterator j = dsParentTablePK.getRelations()
						.iterator(); j.hasNext();) {
					final Relation dsPKRelation = (Relation) j.next();
					final DataSetTable dsFKTable = (DataSetTable) dsPKRelation
							.getOtherKey(dsParentTablePK).getTable();
					// For each one, find all VirtualTables involved in
					// that relation, and index and establish FK.
					for (final Iterator k = vTables.iterator(); k.hasNext();) {
						final VirtualTable vChildTable = (VirtualTable) k
								.next();
						// Virtual table not one we're looking for?
						if (!vChildTable.getDataSetTable().equals(dsFKTable))
							continue;
						// Virtual table not same partition values as
						// parent?
						final List vParentPartitionValues = vParentTable
								.getPartitionValues();
						if (vChildTable.getPartitionValues().size() < vParentPartitionValues
								.size())
							continue;
						else if (!vChildTable.getPartitionValues().subList(0,
								vParentPartitionValues.size()).equals(
								vParentPartitionValues))
							continue;
						// Work out the FK.
						final Key dsChildPK = vChildTable.getDataSetTable()
								.getPrimaryKey();
						final Key dsChildFK = (Key) vChildTable
								.getDataSetTable().getForeignKeys().iterator()
								.next();
						final List dsChildFKCols = dsChildFK.getColumns();
						// FK identical to PK on child table?
						if (dsChildPK != null
								&& dsChildFKCols.equals(dsChildPK.getColumns()))
							continue;
						// Index fkKeyCols.
						final MartConstructorAction fkIndex = new Index(
								this.datasetSchemaName, vChildTable
										.getDataSetTable().getName(), null,
								vChildTable.getTempTableName(), dsChildFKCols);
						actionGraph.addActionWithParent(fkIndex, pkIndex);
						// Add action to list of pkFkActions.
						pkFkActions.add(fkIndex);
					}
				}
			}

			// Set up a break-point last action that waits for all
			// foreign keys to complete.
			final MartConstructorAction preRenameAction = new PlaceHolder(
					this.datasetSchemaName);
			for (final Iterator i = pkFkActions.iterator(); i.hasNext();)
				preRenameAction.addParent((MartConstructorAction) i.next());
			actionGraph.addAction(preRenameAction);
			final List renameActions = new ArrayList();

			// Rename all tables.
			for (final Iterator i = vTables.iterator(); i.hasNext();) {
				final VirtualTable vTable = (VirtualTable) i.next();
				final String vTableFinalName = vTable.createFinalName();
				final String vTableTempName = vTable.getTempTableName();
				// Rename the table.
				final MartConstructorAction renameTable = new RenameTable(
						this.datasetSchemaName, vTable.getDataSetTable()
								.getName(), null, vTableFinalName,
						vTableTempName);
				actionGraph.addActionWithParent(renameTable, preRenameAction);
				renameActions.add(renameTable);
				vTable.setTempTableName(vTableFinalName);
			}

			// Post-construction optimisation.
			final MartConstructorAction prePCOAction = new PlaceHolder(
					this.datasetSchemaName);
			for (final Iterator i = renameActions.iterator(); i.hasNext();)
				prePCOAction.addParent((MartConstructorAction) i.next());
			actionGraph.addAction(prePCOAction);
			final List optimiseActions = new ArrayList();
			final DataSetOptimiserType dsOptimiserType = dataset
					.getDataSetOptimiserType();
			if (!dsOptimiserType.equals(DataSetOptimiserType.NONE)) {
				final List vHasTables = new ArrayList();
				// We only need optimise all main and subclass tables.
				for (final Iterator i = vTables.iterator(); i.hasNext();) {
					final VirtualTable vParentTable = (VirtualTable) i.next();
					if (vParentTable.getDataSetTable().getType().equals(
							DataSetTableType.DIMENSION))
						continue;
					// Make the dimension optimisations dependent on the
					// prePCOAction.
					MartConstructorAction preDOAction = prePCOAction;
					// If we are using column join, add columns to the
					// main or subclass table directly.
					VirtualTable vHasTable = vParentTable;
					// But if we are using table join, create a 'boolean'
					// table and add columns to that instead.
					if (dsOptimiserType.getTable()) {
						vHasTable = new VirtualHasTable(vParentTable
								.getDataSetTable(), dsOptimiserType);
						vHasTables.add(vHasTable);
						vHasTable.getPartitionValues().addAll(
								vParentTable.getPartitionValues());
						// Use the final name from the start.
						vHasTable.setTempTableName(vHasTable.createFinalName());
						// Work out what columns to include from the
						// main/subclass table.
						final List dsPKCols = vParentTable.getDataSetTable()
								.getPrimaryKey().getColumns();
						// Create the new 'boolean' table with the columns
						// from the main/subclass table.
						final MartConstructorAction create = new Create(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), null,
								vParentTable.getTempTableName(), dsPKCols,
								false, null, false, false);
						actionGraph.addActionWithParent(create, preDOAction);
						// Create the PK on the 'boolean' table using the same
						// columns, as an index.
						final MartConstructorAction pkIndex = new Index(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), dsPKCols);
						actionGraph.addActionWithParent(pkIndex, create);
						preDOAction = pkIndex;
					}

					// Add the has table to the parent table for
					// future reference.
					vParentTable.setHasTable(vHasTable);

					// Identify all dimensions in dataset.
					for (final Iterator j = vTables.iterator(); j.hasNext();) {
						final VirtualTable vChildTable = (VirtualTable) j
								.next();
						if (!vChildTable.getDataSetTable().getType().equals(
								DataSetTableType.DIMENSION))
							continue;
						// Dimensions are only useful if they hang off
						// acceptable tables.
						if (!vChildTable.getParentDataSetRelation().getOneKey()
								.getTable().equals(
										vParentTable.getDataSetTable()))
							continue;
						// Child tables are tables that have the same child
						// dataset table, and start with the same sequence
						// of partition values as the parent table, possibly
						// with extra ones on the end.
						final List vParentPartitionValues = vParentTable
								.getPartitionValues();
						if (vChildTable.getPartitionValues().size() < vParentPartitionValues
								.size())
							continue;
						else if (!vChildTable.getPartitionValues().subList(0,
								vParentPartitionValues.size()).equals(
								vParentPartitionValues))
							continue;
						// Work out 'boolean' column name.
						final String vHasColumnName = vChildTable
								.createContentName()
								+ Resources
										.get((dsOptimiserType.getBool() ? "bool"
												: "count")
												+ "ColSuffix");
						// Add columns to the main or subclass table or
						// 'has' table.
						final MartConstructorAction optimiseAdd = new OptimiseAddColumn(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), vHasColumnName);
						actionGraph.addActionWithParent(optimiseAdd,
								preDOAction);
						vHasTable.addHasColumn(vHasColumnName);
						// Work out what the non-null columns to check are.
						// By default it's the primary key, but if not, then
						// it is all columns except the foreign key.
						// If that leaves it with no columns, then it is
						// simply all columns.
						List vChildTableNonNullCols;
						if (vChildTable.getDataSetTable().getPrimaryKey() != null)
							vChildTableNonNullCols = vChildTable
									.getDataSetTable().getPrimaryKey()
									.getColumns();
						else {
							vChildTableNonNullCols = new ArrayList(vChildTable
									.getDataSetTable()
									.getUnmaskedDataSetColumns());
							// Remove all dependencies as they will not be
							// there in the actual table.
							for (final Iterator l = vChildTableNonNullCols
									.iterator(); l.hasNext();)
								if (((DataSetColumn) l.next()).getDependency())
									l.remove();
							if (vChildTable.getParentDataSetRelation()
									.getManyKey().getColumns().size() < vChildTableNonNullCols
									.size())
								vChildTableNonNullCols.removeAll(vChildTable
										.getParentDataSetRelation()
										.getManyKey().getColumns());
						}
						// Update those columns using inner join
						// on has table PK to dimension table FK.
						final MartConstructorAction optimiseUpd = new OptimiseUpdateColumn(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), null, vChildTable
										.getTempTableName(), vChildTable
										.getParentDataSetRelation()
										.getManyKey().getColumns(),
								vChildTableNonNullCols, vHasTable
										.getDataSetTable().getPrimaryKey()
										.getColumns(), vHasColumnName,
								dsOptimiserType.getBool());
						actionGraph.addActionWithParent(optimiseUpd,
								optimiseAdd);
						optimiseActions.add(optimiseUpd);
					}
				}

				// Inheriting anything?
				if (dsOptimiserType.getInherit()) {
					// Pause and wait for optimisation to finish.
					final MartConstructorAction postPCOAction = new PlaceHolder(
							this.datasetSchemaName);
					for (final Iterator i = optimiseActions.iterator(); i
							.hasNext();)
						postPCOAction.addParent((MartConstructorAction) i
								.next());
					actionGraph.addAction(postPCOAction);

					// Inherit optimiser columns from child subclasses.
					for (final Iterator i = vTables.iterator(); i.hasNext();) {
						final VirtualTable vParentTable = (VirtualTable) i
								.next();
						if (vParentTable.getDataSetTable().getType().equals(
								DataSetTableType.DIMENSION))
							continue;

						// Work out what the child subclass tables are by
						// iterating over all virtual tables and adding in any
						// that refer to a dataset table that has a subclass
						// relation from any dataset table already in the list
						// and shares the exact same partition values.
						final List inheritTables = new ArrayList();
						inheritTables.add(vParentTable); // Temporarily.
						for (int k = 0; k < inheritTables.size(); k++) {
							final VirtualTable vPrevTable = (VirtualTable) inheritTables
									.get(k);
							for (final Iterator j = vTables.iterator(); j
									.hasNext();) {
								final VirtualTable vNextTable = (VirtualTable) j
										.next();
								if (vNextTable.getDataSetTable().getType()
										.equals(DataSetTableType.MAIN_SUBCLASS)
										&& vNextTable
												.getParentDataSetRelation()
												.getOneKey().getTable() == vPrevTable
												.getDataSetTable()
										&& vNextTable
												.getPartitionValues()
												.equals(
														vPrevTable
																.getPartitionValues()))
									inheritTables.add(vNextTable);
							}
						}
						// Remove parent table as is finished with now.
						inheritTables.remove(vParentTable);

						// The parent has a 'has' table.
						final VirtualTable vParentHasTable = vParentTable
								.getHasTable();

						// Find out the 'has' table for each of the subclass
						// tables and copy over all 'has' columns for it.
						for (final Iterator k = inheritTables.iterator(); k
								.hasNext();) {
							final VirtualTable vInheritTable = (VirtualTable) k
									.next();
							final VirtualTable vHasTable = vInheritTable
									.getHasTable();
							final Collection vHasColumnNames = vHasTable
									.getHasColumns();
							// Add all the columns to the parent 'has' table.
							for (final Iterator l = vHasColumnNames.iterator(); l
									.hasNext();) {
								final String vHasColumnName = (String) l.next();
								final MartConstructorAction optimiseAdd = new OptimiseAddColumn(
										this.datasetSchemaName, vParentHasTable
												.getDataSetTable().getName(),
										null, vParentHasTable
												.getTempTableName(),
										vHasColumnName);
								actionGraph.addActionWithParent(optimiseAdd,
										postPCOAction);
								// Copy the column value over using the PK of
								// the parent 'has' table.
								final MartConstructorAction optimiseCopy = new OptimiseCopyColumn(
										this.datasetSchemaName, vParentHasTable
												.getDataSetTable().getName(),
										null, vParentHasTable
												.getTempTableName(), null,
										vHasTable.getTempTableName(), null,
										vInheritTable.getTempTableName(),
										vHasTable.getDataSetTable()
												.getPrimaryKey().getColumns(),
										vParentHasTable.getDataSetTable()
												.getPrimaryKey().getColumns(),
										vHasColumnName, dsOptimiserType
												.getBool());
								actionGraph.addActionWithParent(optimiseCopy,
										optimiseAdd);
							}
						}
					}
				}

				// Remember 'has' tables that we made. Must do this
				// last else they'll get included in the inheritance loop.
				vTables.addAll(vHasTables);
			}

			// Now we have constructed our action graph, issue events to
			// get the work done!

			// Work out the progress step size.
			double stepPercent = 100.0 / actionGraph.getActions().size();

			// Divide the progress step size by the number of datasets.
			stepPercent /= totalDataSetCount;

			// Initialize the helper, telling it we are about to start
			// sending it actions to process.
			this.issueListenerEvent(MartConstructorListener.DATASET_STARTED);

			try {
				// The action graph is tiered into several levels, called
				// depths. These levels are assigned automatically according
				// to the dependency of actions on other actions. We want
				// to start at the first depth (0), process all actions
				// there, then move on the next depth, and so on. Each depth
				// is reliant on the previous depth completing before it can
				// begin processing.
				int depth = 0;
				Collection actions = actionGraph.getActionsAtDepth(depth);
				while (!actions.isEmpty()) {
					// Loop over all actions at current depth.
					for (final Iterator i = actions.iterator(); i.hasNext();) {
						final MartConstructorAction action = (MartConstructorAction) i
								.next();

						// Execute the action.
						this.statusMessage = action.getStatusMessage();
						this.issueListenerEvent(
								MartConstructorListener.ACTION_EVENT, action);

						// Update the progress percentage.
						this.percentComplete += stepPercent;

						// Check not cancelled.
						this.checkCancelled();
					}

					// Get next depth.
					actions = actionGraph.getActionsAtDepth(++depth);
				}
			} catch (final Exception e) {
				// Pass any exceptions on up.
				throw e;
			} finally {
				// Make sure the helper always gets a chance to tidy up,
				// even if an exception is thrown.
				this.issueListenerEvent(MartConstructorListener.DATASET_ENDED);
			}
		}

		private void issueListenerEvent(final int event) throws Exception {
			this.issueListenerEvent(event, null);
		}

		private void issueListenerEvent(final int event,
				final MartConstructorAction action) throws Exception {
			for (final Iterator i = this.martConstructorListeners.iterator(); i
					.hasNext();)
				((MartConstructorListener) i.next())
						.martConstructorEventOccurred(event, action);
		}

		/**
		 * This takes a dataset table and converts it into a set of actions that
		 * will create that dataset table, before partioning etc. occurs
		 * elsewhere, eg. in {@link #makeActionsForDataset(DataSet, int)}.
		 * 
		 * @param vParentTable
		 *            if the dataset table is going to be linked to another one,
		 *            eg. if it is a dimension of a main table, then this
		 *            parameter contains the temp table name that holds the
		 *            parent table.
		 * @param firstActionDependsOn
		 *            the action that must be completed before actions for this
		 *            table can begin being processed.
		 * @param actionGraph
		 *            the graph of actions for the dataset this table lives in.
		 *            This method will update the graph with actions for this
		 *            table.
		 * @param rMainTableSchema
		 *            the schema that the original central table
		 *            {@link DataSet#getCentralTable()} lives in.
		 * @param rSchema
		 *            the schema that this table lives in, from
		 *            {@link Table#getSchema()}.
		 * @param vConstructionTable
		 *            the temp table object to create the definition for this
		 *            dataset table inside.
		 * @throws Exception
		 *             if anythign goes wrong.
		 */
		private void makeActionsForDatasetTable(
				final VirtualTable vParentTable,
				final MartConstructorAction firstActionDependsOn,
				final MartConstructorActionGraph actionGraph,
				final Schema rMainTableSchema, final Schema rSchema,
				final VirtualTable vConstructionTable) throws Exception {
			// A placeholder for the last action performed on this table.
			MartConstructorAction lastActionPerformed = firstActionDependsOn;

			// Mark temp table from dependent action as an interim table.
			// Interim tables are not dropped until everything is complete,
			// so that things like dimension tables can be restarted if
			// they fail, without having to recreate the temp table they
			// sourced their inherited columns from.
			lastActionPerformed.setInterim(true);

			// Placeholder for name of the target temp table that will
			// contain the constructed table.
			String vTableTempName = vConstructionTable.getTempTableName();

			// First table to select from is the underlying real table if
			// MAIN, or the parent DS table if not MAIN.
			Schema replacementFirstSchema;
			Table replacementFirstTable;
			String replacementFirstTableName;
			final List replacementFirstTableCols = new ArrayList();
			if (vConstructionTable.getDataSetTable().getType().equals(
					DataSetTableType.MAIN)) {
				replacementFirstSchema = rSchema;
				replacementFirstTable = vConstructionTable.getRealTable();
				replacementFirstTableName = replacementFirstTable.getName();

				// Work out what columns to include from the first table.
				replacementFirstTableCols
						.addAll(vConstructionTable.getDataSetTable()
								.getUnmaskedDataSetColumns(
										replacementFirstTable.getColumns(),
										null, null));

				// Include the schema name column if there is one (don't need
				// to do this for dimension/subclass tables as it will be part
				// of the primary key inherited from the parent table).
				for (final Iterator i = vConstructionTable.getDataSetTable()
						.getColumns().iterator(); i.hasNext();) {
					final DataSetColumn dsCol = (DataSetColumn) i.next();
					if (dsCol instanceof SchemaNameColumn)
						replacementFirstTableCols.add(dsCol);
				}
			} else {
				replacementFirstSchema = null;
				replacementFirstTable = null;
				replacementFirstTableName = vParentTable.getTempTableName();

				// Work out what columns to include from the first table.
				for (final Iterator i = vConstructionTable.getDataSetTable()
						.getColumns().iterator(); i.hasNext();) {
					final DataSetColumn dsCol = (DataSetColumn) i.next();
					if (dsCol instanceof InheritedColumn) {
						final InheritedColumn dsInheritedCol = (InheritedColumn) dsCol;
						replacementFirstTableCols.add(dsInheritedCol);
					}
				}
			}

			// Make a list to keep track of columns used so far,
			// and populate it with the columns from the first table.
			final List colsSoFar = new ArrayList(replacementFirstTableCols);

			// If no underlying relations, CREATE from replacement first.
			if (vConstructionTable.getDataSetTable().getUnderlyingKeys()
					.isEmpty()) {
				final MartConstructorAction create = new Create(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vTableTempName, replacementFirstSchema,
						replacementFirstTableName, replacementFirstTableCols,
						replacementFirstTable.getPrimaryKey() == null,
						vConstructionTable.getDataSet().getRestrictedTableType(
								replacementFirstTable), true, false);
				actionGraph.addActionWithParent(create, lastActionPerformed);
				// Update last action performed, in case there are no merges.
				lastActionPerformed = create;
			}
			// Otherwise, MERGE from replacement first plus second in line.
			else {
				// What key was followed from the previous table?
				final Key rSourceKey = (Key) vConstructionTable
						.getDataSetTable().getUnderlyingKeys().get(0);
				// What relation was followed?
				final Relation rSourceRelation = (Relation) vConstructionTable
						.getDataSetTable().getUnderlyingRelations().get(0);
				// What key did we reach by following the relation?
				final Key rTargetKey = rSourceRelation.getOtherKey(rSourceKey);
				// What table did we reach, and therefore need to merge now?
				final Table rTargetTable = rTargetKey.getTable();
				String rTargetTableName = rTargetTable.getName();
				Schema rTargetSchema = rTargetTable.getSchema();
				// What are the equivalent columns on the existing temp table
				// that correspond to the key on the previous table?
				final List dsSourceKeyCols = vConstructionTable
						.getDataSetTable().getUnmaskedDataSetColumns(
								rSourceKey.getColumns(), rSourceRelation,
								colsSoFar);
				// What are the columns we should merge from this new table?
				final List dsTargetIncludeCols = vConstructionTable
						.getDataSetTable().getUnmaskedDataSetColumns(
								rSourceRelation);
				colsSoFar.addAll(dsTargetIncludeCols);				
				// Generate new temp table name for merged table.
				final String vMergedTableTempName = this.helper
						.getNewTempTableName();
				// Merge tables based on sourceDSKey -> targetKey,
				// and selecting based on targetDSCols.
				final MartConstructorAction merge = new Merge(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vMergedTableTempName, replacementFirstSchema,
						replacementFirstTableName, dsSourceKeyCols,
						replacementFirstTableCols, rTargetSchema,
						rTargetTableName, rTargetKey.getColumns(),
						dsTargetIncludeCols, rTargetTable
								.equals(rSourceRelation.getSecondKey()
										.getTable()), rSourceRelation
								.isManyToMany(), vConstructionTable
								.getDataSet().getRestrictedTableType(
										rTargetTable),
						replacementFirstSchema != null, true);
				actionGraph.addActionWithParent(merge, lastActionPerformed);
				// Last action performed for this table is the merge, as
				// the drop can be carried out later at any time.
				lastActionPerformed = merge;
				// Update temp table name to point to newly merged table.
				vTableTempName = vMergedTableTempName;
			}

			// Merge subsequent tables based on underlying relations,
			// starting from second in line.
			for (int i = 1; i < vConstructionTable.getDataSetTable()
					.getUnderlyingKeys().size(); i++) {
				// What key was followed from the previous table?
				final Key rSourceKey = (Key) vConstructionTable
						.getDataSetTable().getUnderlyingKeys().get(i);
				// What relation was followed?
				final Relation rSourceRelation = (Relation) vConstructionTable
						.getDataSetTable().getUnderlyingRelations().get(i);
				// What key did we reach by following the relation?
				final Key rTargetKey = rSourceRelation.getOtherKey(rSourceKey);
				// What table did we reach, and therefore need to merge now?
				final Table rTargetTable = rTargetKey.getTable();
				String rTargetTableName = rTargetTable.getName();
				Schema rTargetSchema = rTargetTable.getSchema();
				// What are the equivalent columns on the existing temp table
				// that correspond to the key on the previous table?
				final List dsSourceKeyCols = vConstructionTable
						.getDataSetTable().getUnmaskedDataSetColumns(
								rSourceKey.getColumns(), rSourceRelation,
								colsSoFar);
				// What are the columns we should merge from this new table?
				final List dsTargetIncludeCols = vConstructionTable
						.getDataSetTable().getUnmaskedDataSetColumns(
								rSourceRelation);
				colsSoFar.addAll(dsTargetIncludeCols);
				// Skip the merge if we won't gain anything from it.
				if (dsTargetIncludeCols.isEmpty())
					continue;
				// Index sourceDSKey columns.
				final MartConstructorAction index = new Index(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vTableTempName, dsSourceKeyCols);
				actionGraph.addActionWithParent(index, lastActionPerformed);
				// Generate new temp table name for merged table.
				final String vMergedTableTempName = this.helper
						.getNewTempTableName();
				// Merge tables based on sourceDSKey -> targetKey,
				// and selecting based on targetDSCols.
				final MartConstructorAction merge = new Merge(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vMergedTableTempName, null, vTableTempName,
						dsSourceKeyCols, null, rTargetSchema, rTargetTableName,
						rTargetKey.getColumns(), dsTargetIncludeCols,
						rTargetTable.equals(rSourceRelation.getSecondKey()
								.getTable()), rSourceRelation.isManyToMany(),
						vConstructionTable.getDataSet().getRestrictedTableType(
								rTargetTable), false, true);
				actionGraph.addActionWithParent(merge, index);
				// Last action performed for this table is the merge, as
				// the drop can be carried out later at any time.
				lastActionPerformed = merge;
				// Drop old temp table.
				MartConstructorAction drop = new Drop(this.datasetSchemaName,
						vConstructionTable.getDataSetTable().getName(), null,
						vTableTempName);
				actionGraph.addActionWithParent(drop, merge);
				// Update temp table name to point to newly merged table.
				vTableTempName = vMergedTableTempName;
			}

			// Add in all the concat-relation columns.
			for (final Iterator i = vConstructionTable.getDataSetTable()
					.getColumns().iterator(); i.hasNext();) {
				final DataSetColumn dsCol = (DataSetColumn) i.next();
				if (!(dsCol instanceof ConcatRelationColumn))
					continue;
				final ConcatRelationColumn dsConcatCol = (ConcatRelationColumn) dsCol;
				// Where does the concat column point?
				final Relation rConcatRelation = dsConcatCol
						.getUnderlyingRelation();
				// What key did it lead from?
				final Key rConcatSourceKey = rConcatRelation.getOneKey();
				// What key does it go to?
				final Key rConcatTargetKey = rConcatRelation.getManyKey();
				// What is the table to be concatted?
				final Table rConcatTargetTable = rConcatTargetKey.getTable();
				String rConcatTargetTableName = rConcatTargetTable.getName();
				// What columns on the table should be concatted?
				final List rConcatTargetCols = vConstructionTable.getDataSet()
						.getConcatRelationType(rConcatRelation)
						.getConcatColumns();
				// What are the equivalent columns on the existing temp table
				// that correspond to the source key?
				final List vSourceKeyCols = vConstructionTable
						.getDataSetTable().getUnmaskedDataSetColumns(
								rConcatSourceKey.getColumns(), rConcatRelation,
								null);
				// Create the concat table by selecting sourceKey.
				String vConcatTableTempName = this.helper.getNewTempTableName();
				final MartConstructorAction createCon = new Create(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vConcatTableTempName, null, vTableTempName,
						vSourceKeyCols, false, null, false, false);
				actionGraph.addActionWithParent(createCon, lastActionPerformed);
				// Generate new temp table name for populated concat table.
				final String vPopulatedConcatTableTempName = this.helper
						.getNewTempTableName();
				// Populate the concat table by selecting targetKey and
				// concatting targetConcatKey into a column with the same
				// name as concatColumn. Dependent on lastActionPerformed, and
				// updates it.
				final MartConstructorAction concat = new Concat(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vPopulatedConcatTableTempName, null, vTableTempName,
						vSourceKeyCols, rSchema,
						rConcatTargetTableName, rConcatTargetKey.getColumns(),
						rConcatTargetCols, dsConcatCol.getName(),
						vConstructionTable.getDataSet().getConcatRelationType(
								rConcatRelation).getColumnSeparator(),
						vConstructionTable.getDataSet().getConcatRelationType(
								rConcatRelation).getRecordSeparator(),
						rConcatTargetTable.equals(rConcatRelation
								.getSecondKey().getTable()), vConstructionTable
								.getDataSet().getRestrictedTableType(
										rConcatTargetTable));
				actionGraph.addActionWithParent(concat, createCon);
				// Drop old concat temp table and replace with new one.
				MartConstructorAction drop = new Drop(this.datasetSchemaName,
						vConstructionTable.getDataSetTable().getName(), null,
						vConcatTableTempName);
				actionGraph.addActionWithParent(drop, concat);
				vConcatTableTempName = vPopulatedConcatTableTempName;
				// Index sourceDSKey columns.
				final MartConstructorAction indexCon = new Index(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vConcatTableTempName, vSourceKeyCols);
				actionGraph.addActionWithParent(indexCon, concat);
				// Generate new temp table name for merged table.
				final String vConcatTableFinalTempName = this.helper
						.getNewTempTableName();
				// Merge with the concat table based on sourceDSKey ->
				// sourceDSKey and selecting the column with the same name as
				// concatColumn.
				final MartConstructorAction merge = new Merge(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vConcatTableFinalTempName, null, vTableTempName,
						vSourceKeyCols, null, null, vConcatTableTempName,
						vSourceKeyCols, Collections.singletonList(dsConcatCol),
						false, false, null, false, false);
				actionGraph.addActionWithParent(merge, indexCon);
				lastActionPerformed = merge;
				// Drop old temp table and replace with new one.
				drop = new Drop(this.datasetSchemaName, vConstructionTable
						.getDataSetTable().getName(), null, vTableTempName);
				actionGraph.addActionWithParent(drop, merge);
				vTableTempName = vConcatTableFinalTempName;
				// Drop concat table. Dependent on lastActionPerformed, but
				// does not update it.
				final MartConstructorAction dropCon = new Drop(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vConcatTableTempName);
				actionGraph.addActionWithParent(dropCon, merge);
			}

			// Add all the expression columns.
			final List dsExpressionColumns = new ArrayList();
			final List dsGroupByDependentColumns = new ArrayList();
			for (final Iterator i = vConstructionTable.getDataSetTable()
					.getColumns().iterator(); i.hasNext();) {
				final Column dsCol = (Column) i.next();
				if (dsCol instanceof ExpressionColumn) {
					final ExpressionColumn dsExpressionCol = (ExpressionColumn) dsCol;
					dsExpressionColumns.add(dsExpressionCol);
					dsExpressionCol.dropUnusedAliases();
					if (dsExpressionCol.getGroupBy())
						dsGroupByDependentColumns.addAll(dsExpressionCol
								.getDependentColumns());
				}
			}
			if (!dsExpressionColumns.isEmpty()) {
				// Construct list of columns to select (and optionally
				// group by). List contains all unmasked columns on table,
				// minus expression columns, minus dependency columns from
				// group-by expression columns.
				final List vSelectedColumns = vConstructionTable
						.getDataSetTable().getUnmaskedDataSetColumns();
				vSelectedColumns.removeAll(dsExpressionColumns);
				vSelectedColumns.removeAll(dsGroupByDependentColumns);
				// Generate new temp table name for merged table.
				final String vExpressionTableTempName = this.helper
						.getNewTempTableName();
				// Create new table as select all from list
				// plus all expression columns using literal expression,
				// with optional group-by.
				final ExpressionAddColumns expr = new ExpressionAddColumns(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vExpressionTableTempName, null, vTableTempName,
						vSelectedColumns, dsExpressionColumns,
						!dsGroupByDependentColumns.isEmpty());
				actionGraph.addActionWithParent(expr, lastActionPerformed);
				lastActionPerformed = expr;
				// Drop previous table.
				final Drop drop = new Drop(this.datasetSchemaName,
						vConstructionTable.getDataSetTable().getName(), null,
						vTableTempName);
				actionGraph.addActionWithParent(drop, expr);
				vTableTempName = vExpressionTableTempName;
			}

			// Get all the columns for this table. Remove all
			// the masked dependency columns, if there are any left
			// that weren't already removed by the expression group-by.
			final List dsFinalColumns = vConstructionTable.getDataSetTable()
					.getUnmaskedDataSetColumns();
			final int beforeDependentRemoved = dsFinalColumns.size();
			for (final Iterator i = dsFinalColumns.iterator(); i.hasNext();) {
				DataSetColumn dsCol = (DataSetColumn) i.next();
				if (dsCol.getDependency() && dsCol.getMasked())
					i.remove();
			}
			final int afterDependentRemoved = dsFinalColumns.size();
			dsFinalColumns.removeAll(dsGroupByDependentColumns);
			final int afterGroupByRemoved = dsFinalColumns.size();
			if (beforeDependentRemoved != afterDependentRemoved
					&& (dsGroupByDependentColumns.size() == 0 || afterDependentRemoved != afterGroupByRemoved)) {
				// We need to do the trim as there were extra columns
				// that need to be removed that were not already removed
				// by the group-by.
				final String vTrimmedTableTempName = this.helper
						.getNewTempTableName();
				final Create trim = new Create(this.datasetSchemaName,
						vConstructionTable.getDataSetTable().getName(), null,
						vTrimmedTableTempName, null, vTableTempName,
						dsFinalColumns, false, null, false, false);
				actionGraph.addActionWithParent(trim, lastActionPerformed);
				lastActionPerformed = trim;
				// Drop previous table.
				final Drop drop = new Drop(this.datasetSchemaName,
						vConstructionTable.getDataSetTable().getName(), null,
						vTableTempName);
				actionGraph.addActionWithParent(drop, trim);
				vTableTempName = vTrimmedTableTempName;
			}

			// Update the temp table name.
			vConstructionTable.setTempTableName(vTableTempName);

			// Return the last action performed to create this table.
			vConstructionTable.setLastActionPerformed(lastActionPerformed);
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
				// Split the datasets into groups by mart.
				final Map martDataSets = new HashMap();
				for (final Iterator i = this.datasets.iterator(); i.hasNext();) {
					final DataSet ds = (DataSet) i.next();
					final Mart mart = ds.getMart();
					if (!martDataSets.containsKey(mart))
						martDataSets.put(mart, new ArrayList());
					((List) martDataSets.get(mart)).add(ds);
				}

				// Begin.
				this
						.issueListenerEvent(MartConstructorListener.CONSTRUCTION_STARTED);

				// Work out how many datasets we have.
				final int totalDataSetCount = this.datasets.size();

				// Loop over each mart.
				for (final Iterator j = martDataSets.values().iterator(); j
						.hasNext();) {
					this
							.issueListenerEvent(MartConstructorListener.MART_STARTED);

					try {
						// Loop over all the datasets we want included from this
						// mart. Build actions for each one.
						for (final Iterator i = ((List) j.next()).iterator(); i
								.hasNext();)
							this.makeActionsForDataset((DataSet) i.next(),
									totalDataSetCount);
					} finally {
						this
								.issueListenerEvent(MartConstructorListener.MART_ENDED);
					}
				}
			} catch (final ConstructorException e) {
				this.failure = e;
			} catch (final Throwable t) {
				this.failure = new ConstructorException(t);
			} finally {
				try {
					this
							.issueListenerEvent(MartConstructorListener.CONSTRUCTION_ENDED);
				} catch (final ConstructorException e) {
					this.failure = e;
				} catch (final Throwable t) {
					this.failure = new ConstructorException(t);
				} finally {
					Log.info(Resources.get("logConstructorEnded"));
				}
			}
		}

		/**
		 * Internal use only, serves as a convenient wrapper for tracking info
		 * about a particular 'boolean' table.
		 */
		private class VirtualHasTable extends VirtualTable {
			private final DataSetOptimiserType dsOptimiserType;

			/**
			 * Constructor starts tracking a 'boolean' table that contains
			 * optimisations for the given table.
			 * 
			 * @param datasetTable
			 *            the table to contain optimisations for.
			 * @param dsOptimiserType
			 *            the optimiser type for this table.
			 */
			public VirtualHasTable(final DataSetTable datasetTable,
					final DataSetOptimiserType dsOptimiserType) {
				super(datasetTable, null);
				this.dsOptimiserType = dsOptimiserType;
			}

			public String createFinalName() {
				// TODO - come up with a better naming scheme
				// Currently the name is:
				// datasetname__{content}_bool__dm

				// Work out what dataset we are in.
				final DataSet dataset = this.getDataSet();

				final StringBuffer name = new StringBuffer();

				// Dataset name and __ separator.
				name.append(dataset.getName());
				name.append(Resources.get("tablenameSep"));

				// Content name.
				name.append(this.createContentName());

				// _ separator, 'bool', and __ separator.
				name.append(Resources.get("tablenameSubSep"));
				name
						.append(Resources
								.get((this.dsOptimiserType.getBool() ? "bool"
										: "count")
										+ "TblSuffix"));
				name.append(Resources.get("tablenameSep"));

				// Type.
				name.append(Resources.get("dimensionSuffix"));

				return name.toString().toLowerCase();
			}
		}

		/**
		 * Internal use only, serves as a convenient wrapper for tracking info
		 * about a particular dataset table.
		 */
		private class VirtualTable {

			private DataSetTable datasetTable;

			private MartConstructorAction lastActionPerformed;

			private Relation parentDataSetRelation;

			private List partitionValues;

			private String tempTableName;

			private VirtualTable hasTable;

			private Collection hasColumns;

			/**
			 * Constructor starts tracking a dataset table.
			 * 
			 * @param datasetTable
			 *            the table to track.
			 * @param parentDataSetRelation
			 *            the relation between this dataset table and any parent
			 *            table it belongs to, e.g. a main or a subclass.
			 */
			public VirtualTable(final DataSetTable datasetTable,
					final Relation parentDataSetRelation) {
				this.tempTableName = GenericConstructorRunnable.this.helper
						.getNewTempTableName();
				this.datasetTable = datasetTable;
				this.parentDataSetRelation = parentDataSetRelation;
				this.partitionValues = new ArrayList();
				this.hasTable = this;
				this.hasColumns = new ArrayList();
			}

			/**
			 * Constructs a string containing the recommended content name for
			 * this dataset table in the final schema.
			 * 
			 * @return the constructed name for this dataset table.
			 */
			public String createContentName() {
				// TODO - come up with a better naming scheme
				// Currently the name is:
				// tablename\
				// {_partitionvalue}*}\

				// Work out what dataset we are in.
				final DataSetTable datasetTable = this.getDataSetTable();

				final StringBuffer name = new StringBuffer();

				// Table name.
				name.append(datasetTable.getName());

				// Partition values with _ separator between.
				final List partitionValues = this.getPartitionValues();
				if (!partitionValues.isEmpty())
					for (final Iterator j = partitionValues.iterator(); j
							.hasNext();) {
						name.append(Resources.get("tablenameSubSep"));
						// Partition values may be null, so cannot use
						// toString().
						String partitionValue = "" + j.next();
						// Replace all unusable bits with single underscores.
						partitionValue = partitionValue.replaceAll("\\W", "_");
						// Replace multiple underscores with single ones.
						partitionValue = partitionValue.replaceAll("_+", "_");
						// Wrap with 'p's if start/end with _.
						if (partitionValue.startsWith("_"))
							partitionValue = 'p' + partitionValue;
						if (partitionValue.endsWith("_"))
							partitionValue += 'p';
						// Append the value.
						name.append(partitionValue);
					}

				return name.toString().toLowerCase();
			}

			/**
			 * Constructs a string containing the recommended table name for
			 * this dataset table in the final schema.
			 * 
			 * @return the constructed name for this dataset table.
			 */
			public String createFinalName() {
				// TODO - come up with a better naming scheme
				// Currently the name is:
				// datasetname__{content}__type

				// Work out what dataset we are in.
				final DataSet dataset = this.getDataSet();

				final StringBuffer name = new StringBuffer();

				// Dataset name and __ separator.
				name.append(dataset.getName());
				name.append(Resources.get("tablenameSep"));

				// Content name.
				name.append(this.createContentName());

				// __ separator.
				name.append(Resources.get("tablenameSep"));

				// Type.
				final DataSetTableType type = this.datasetTable.getType();
				if (type.equals(DataSetTableType.MAIN))
					name.append(Resources.get("mainSuffix"));
				else if (type.equals(DataSetTableType.MAIN_SUBCLASS))
					name.append(Resources.get("subclassSuffix"));
				else if (type.equals(DataSetTableType.DIMENSION))
					name.append(Resources.get("dimensionSuffix"));

				return name.toString().toLowerCase();
			}

			/**
			 * Work out what dataset this virtual table represents.
			 * 
			 * @return the dataset for this virtual table.
			 */
			public DataSet getDataSet() {
				return (DataSet) this.datasetTable.getSchema();
			}

			/**
			 * Work out what dataset table this virtual table represents.
			 * 
			 * @return the dataset table for this virtual table.
			 */
			public DataSetTable getDataSetTable() {
				return this.datasetTable;
			}

			/**
			 * Gets the last action performed for this table, so that other
			 * methods can work out which action to wait for in order for this
			 * table to be complete.
			 * 
			 * @return the last action performed in the creation of this table.
			 */
			public MartConstructorAction getLastActionPerformed() {
				return this.lastActionPerformed;
			}

			/**
			 * Find out the relation that links the dataset table of this
			 * virtual table to its parent dataset table, if any.
			 * 
			 * @return the relation between this virtual table's dataset table
			 *         and its parent, if any.
			 */
			public Relation getParentDataSetRelation() {
				return this.parentDataSetRelation;
			}

			/**
			 * Find out the parent dataset table of the dataset table inside
			 * this virtual table.
			 * 
			 * @return the parent dataset table of this virtual table's dataset
			 *         table.
			 */
			public DataSetTable getParentDataSetTable() {
				return (DataSetTable) this.parentDataSetRelation.getOneKey()
						.getTable();
			}

			/**
			 * Obtain the list of values used to partition this table, in the
			 * order in which those partitions were made.
			 * 
			 * @return the list of values used to partition this table.
			 */
			public List getPartitionValues() {
				return this.partitionValues;
			}

			/**
			 * Work out what schema the real table of this virtual table is in.
			 * 
			 * @return the schema that the real table of this virtual table is
			 *         in.
			 */
			public Schema getRealSchema() {
				return this.datasetTable.getUnderlyingTable().getSchema();
			}

			/**
			 * Work out what real table, ie. the underlying table of the dataset
			 * table, for this virtual table.
			 * 
			 * @return the underlying table of the dataset table for this
			 *         virtual table.
			 */
			public Table getRealTable() {
				return this.datasetTable.getUnderlyingTable();
			}

			/**
			 * The name of the physical temporary table that contains the data
			 * for this representation.
			 * 
			 * @return the temp table name.
			 */
			public String getTempTableName() {
				return this.tempTableName;
			}

			/**
			 * Sets the last action performed for this table, so that other
			 * methods can work out which action to wait for in order for this
			 * table to be complete.
			 * 
			 * @param lastActionPerformed
			 *            the last action performed in the creation of this
			 *            table.
			 */
			public void setLastActionPerformed(
					final MartConstructorAction lastActionPerformed) {
				this.lastActionPerformed = lastActionPerformed;
			}

			/**
			 * Sets the name to use for the physical temporary table that
			 * contains the data for this representation.
			 * 
			 * @param tempTableName
			 *            the temp table name.
			 */
			public void setTempTableName(final String tempTableName) {
				this.tempTableName = tempTableName;
			}

			/**
			 * Set the 'has' table for this parent table, which will either be
			 * the same as itself, or an actual has table.
			 * 
			 * @param hasTable
			 *            the 'has' table for this table.
			 */
			public void setHasTable(VirtualTable hasTable) {
				this.hasTable = hasTable;
			}

			/**
			 * Retrieve the 'has' table for this parent table.
			 * 
			 * @return the 'has' table.
			 */
			public VirtualTable getHasTable() {
				return this.hasTable;
			}

			/**
			 * Add a 'has' column to the table.
			 * 
			 * @param hasColumn
			 *            the name of the column to add.
			 */
			public void addHasColumn(String hasColumn) {
				this.hasColumns.add(hasColumn);
			}

			/**
			 * Retrieve the 'has' columns for the table.
			 * 
			 * @return the list, in no particular order, of 'has' columns.
			 */
			public Collection getHasColumns() {
				return this.hasColumns;
			}
		}
	}

	/**
	 * Helpers provide methods to give useful information to the constructor as
	 * it builds the action graph. Helper instances are usually database aware
	 * and are able to connect to a database to get this information.
	 */
	public interface Helper {

		/**
		 * Generate a new temporary table name.
		 * 
		 * @return a new temporary table name.
		 */
		public String getNewTempTableName();

		/**
		 * Lists the distinct values in the given column. This must be a real
		 * column, not an instance of {@link DataSetColumn}.
		 * 
		 * @param col
		 *            the column to get the distinct values from.
		 * @return the distinct values in the column.
		 * @throws SQLException
		 *             in case of problems.
		 */
		public List listDistinctValues(Column col) throws SQLException;
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
		 * This event will occur when an individual mart ends.
		 */
		public static final int MART_ENDED = 5;

		/**
		 * This event will occur when an individual mart begins.
		 */
		public static final int MART_STARTED = 6;

		/**
		 * This method will be called when an event occurs.
		 * 
		 * @param event
		 *            the event that occurred. See the constants defined
		 *            elsewhere in this interface for possible events.
		 * @param action
		 *            an action object that belongs to this event. Will be
		 *            <tt>null</tt> in all cases except where the event is
		 *            {@link #ACTION_EVENT}.
		 * @throws Exception
		 *             if anything goes wrong whilst handling the event.
		 */
		public void martConstructorEventOccurred(int event,
				MartConstructorAction action) throws Exception;
	}
}
