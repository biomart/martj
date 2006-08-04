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
import org.biomart.builder.exceptions.MartBuilderInternalError;
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
import org.biomart.builder.model.MartConstructorAction.FK;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.MartConstructorActionGraph;
import org.biomart.builder.model.MartConstructorAction.Merge;
import org.biomart.builder.model.MartConstructorAction.OptimiseAddColumn;
import org.biomart.builder.model.MartConstructorAction.OptimiseUpdateColumn;
import org.biomart.builder.model.MartConstructorAction.PK;
import org.biomart.builder.model.MartConstructorAction.Partition;
import org.biomart.builder.model.MartConstructorAction.PlaceHolder;
import org.biomart.builder.model.MartConstructorAction.Reduce;
import org.biomart.builder.model.MartConstructorAction.Rename;
import org.biomart.builder.model.MartConstructorAction.Union;
import org.biomart.builder.resources.Resources;

/**
 * This interface defines the behaviour expected from an object which can take a
 * dataset and actually construct a mart based on this information. Whether it
 * carries out the task or just writes some DDL to be run by the user later is
 * up to the implementor.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.24, 31st July 2006
 * @since 0.1
 */
public interface MartConstructor {
	/**
	 * This method takes a dataset and generates a script for the user to run
	 * later to construct a mart.
	 * <p>
	 * The work is done inside a thread, which is returned unstarted. The user
	 * should create a new {@link Thread} instance around this, and start it by
	 * calling {@link Thread#run()}. They can then monitor it using the methods
	 * provided by the {@link ConstructorRunnable} interface.
	 * 
	 * @param targetSchemaName
	 *            the name of the schema to create the dataset tables in.
	 * @param datasets
	 *            a set of datasets to construct. An empty set means nothing
	 *            will get constructed.
	 * @return the thread that will build it.
	 * @throws Exception
	 *             if there was any problem creating the builder thread.
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
		 * This method should return a message describing what the thread is
		 * currently doing.
		 * 
		 * @return a message describing current activity.
		 */
		public String getStatusMessage();

		/**
		 * This method should return a value between 0 and 100 indicating how
		 * the thread is getting along in the general scheme of things. 0
		 * indicates just starting, 100 indicates complete or very nearly
		 * complete.
		 * 
		 * @return a percentage indicating how far the thread has got.
		 */
		public int getPercentComplete();

		/**
		 * If the thread failed, this method should return an exception
		 * describing the failure. If it succeeded, or is still in progress and
		 * hasn't failed yet, it should return <tt>null</tt>.
		 * 
		 * @return the exception that caused the thread to fail, if any, or
		 *         <tt>null</tt> otherwise.
		 */
		public Exception getFailureException();

		/**
		 * This method will be called if the user wants the thread to stop work
		 * straight away. It should set an exception for
		 * {@link #getFailureException()} to return saying that it was
		 * cancelled, so that the user knows it was so, and doesn't think it
		 * just finished successfully without any warnings.
		 */
		public void cancel();

		/**
		 * This method adds a listener which will listen out for events emitted
		 * by the constructor.
		 * 
		 * @param listener
		 *            the listener to add.
		 */
		public void addMartConstructorListener(MartConstructorListener listener);
	}

	/**
	 * This interface defines a listener which hears events about mart
	 * construction. The events are defined as constants in this interface.
	 */
	public interface MartConstructorListener {
		/**
		 * This event will occur when mart construction begins.
		 */
		public static final int CONSTRUCTION_STARTED = 0;

		/**
		 * This event will occur when mart construction ends.
		 */
		public static final int CONSTRUCTION_ENDED = 1;

		/**
		 * This event will occur when an individual mart begins.
		 */
		public static final int MART_STARTED = 2;

		/**
		 * This event will occur when an individual mart ends.
		 */
		public static final int MART_ENDED = 3;

		/**
		 * This event will occur when an individual dataset begins.
		 */
		public static final int DATASET_STARTED = 4;

		/**
		 * This event will occur when an individual dataset ends.
		 */
		public static final int DATASET_ENDED = 5;

		/**
		 * This event will occur when an action needs performing, and will be
		 * accompanied by a {@link MartConstructorAction} object describing what
		 * needs doing.
		 */
		public static final int ACTION_EVENT = 6;

		/**
		 * This method will be called when an event occurs.
		 * 
		 * @param event
		 *            the event that occurred. See the constants defined
		 *            elsewhere in this interface for possible events.
		 * @param action
		 *            an action object that belongs to this event. Will be null
		 *            in all cases except where the event is
		 *            {@link #ACTION_EVENT}.
		 * @throws Exception
		 *             if anything goes wrong whilst handling the event.
		 */
		public void martConstructorEventOccurred(int event,
				MartConstructorAction action) throws Exception;
	}

	/**
	 * Helper provides methods to define large steps. Helper implementations
	 * perform those steps.
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
	 * Defines the generic way of constructing a mart.
	 */
	public static class GenericConstructorRunnable implements
			ConstructorRunnable {
		private String statusMessage = "";

		private double percentComplete = 0.0;

		private boolean cancelled = false;

		private Exception failure = null;

		private Collection datasets;

		private Helper helper;

		private List martConstructorListeners;

		private String datasetSchemaName;

		/**
		 * Constructs a mart builder that will build the mart in the given
		 * dataset, using the given helper.
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
			this.datasets = datasets;
			this.helper = helper;
			this.martConstructorListeners = new ArrayList();
			this.datasetSchemaName = datasetSchemaName;
		}

		public void addMartConstructorListener(
				final MartConstructorListener listener) {
			this.martConstructorListeners.add(listener);
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

		public void run() {
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
						// mart.
						for (final Iterator i = ((List) j.next()).iterator(); i
								.hasNext();)
							this.doIt((DataSet) i.next(), totalDataSetCount);
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
				}
			}
		}

		private void doIt(final DataSet dataset, final int totalDataSetCount)
				throws Exception {
			// Find the main table of the dataset.
			DataSetTable dsMainTable = null;
			for (final Iterator i = dataset.getTables().iterator(); i.hasNext()
					&& dsMainTable == null;) {
				final DataSetTable table = (DataSetTable) i.next();
				if (table.getType().equals(DataSetTableType.MAIN))
					dsMainTable = table;
			}

			// Work out if we are dealing with a main table that is in a group
			// schema. If it is, we make a list of all the schemas in the group,
			// otherwise the list is a singleton list containing the schema of
			// the main table. This list will be used later to merge schema
			// results together, unless partitioning by schema.
			final Schema rMainTableSchema = dsMainTable.getUnderlyingTable()
					.getSchema();
			Collection rSchemas = null;
			if (rMainTableSchema instanceof SchemaGroup)
				rSchemas = ((SchemaGroup) rMainTableSchema).getSchemas();
			else
				rSchemas = Collections.singletonList(rMainTableSchema);

			// Check not cancelled.
			this.checkCancelled();

			// We have to make space to store the graph of actions we
			// must carry out.
			this.statusMessage = Resources.get("mcCreatingGraph");
			final MartConstructorActionGraph actionGraph = new MartConstructorActionGraph();

			// Establish a root action for the graph.
			final MartConstructorAction rootAction = new PlaceHolder(
					this.datasetSchemaName);
			actionGraph.addAction(rootAction);

			// Make a list to hold the table representations.
			final List vTables = new ArrayList();

			// For each schema, we process all the tables.
			for (final Iterator i = rSchemas.iterator(); i.hasNext();) {
				final Schema rSchema = (Schema) i.next();

				// Make a list of tables for this schema.
				final List rSchemaVirtualTables = new ArrayList();

				// Process the main table.
				VirtualTable vMainTable = new VirtualTable(dsMainTable, null);
				this.processTable(null, rootAction, actionGraph,
						rMainTableSchema, rSchema, vMainTable);
				rSchemaVirtualTables.add(vMainTable);

				// Check not cancelled.
				this.checkCancelled();

				// Set up a map to hold the last actions for each source table
				// we encounter.
				final Map dsTableLastActions = new HashMap();
				dsTableLastActions.put(dsMainTable, vMainTable
						.getLastActionPerformed());

				// Set up a queue to hold all the subclass and dimension
				// relations we encounter.
				final List dsParentRelations = new ArrayList(dsMainTable
						.getPrimaryKey().getRelations());
				final List vParents = new ArrayList();
				for (int x = 0; x < dsParentRelations.size(); x++)
					vParents.add(vMainTable);

				// Process all main dimensions and subclasses.
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
					this.processTable(vParentTable,
							(MartConstructorAction) dsTableLastActions
									.get(dsParentTable), actionGraph,
							rMainTableSchema, rSchema, vChildTable);
					rSchemaVirtualTables.add(vChildTable);

					// Add target table to definitions list.
					dsTableLastActions.put(dsChildTable, vChildTable
							.getLastActionPerformed());

					// Add further subclasses and dimensions to queue.
					final Collection dsChildTableRelations = dsChildTable
							.getPrimaryKey().getRelations();
					dsParentRelations.addAll(dsChildTableRelations);
					for (int x = 0; x < dsChildTableRelations.size(); x++)
						vParents.add(vChildTable);

					// Check not cancelled.
					this.checkCancelled();
				}

				// Add tables for this schema to list of all tables.
				vTables.addAll(rSchemaVirtualTables);
			}

			// Set up a break-point last action that waits for all
			// tables to complete.
			MartConstructorAction prePartitionAction = new PlaceHolder(
					this.datasetSchemaName);
			for (final Iterator i = vTables.iterator(); i.hasNext();)
				prePartitionAction.addParent(((VirtualTable) i.next())
						.getLastActionPerformed());
			actionGraph.addAction(prePartitionAction);

			// If main table schema is a schema group, do a union on all schemas
			// for each table.
			if (rMainTableSchema instanceof SchemaGroup) {
				// Merge each set of tables.
				final List vUnionTables = new ArrayList();
				for (int i = 0; i < vTables.size() / rSchemas.size(); i++) {
					final List vTablesToUnion = new ArrayList();
					for (int j = i; j < vTables.size(); j += rSchemas.size())
						vTablesToUnion.add(vTables.get(j));
					// Create the VirtualTable entry for the union table.
					final VirtualTable vUnionTable = new VirtualTable(
							((VirtualTable) vTablesToUnion.get(0))
									.getDataSetTable(),
							((VirtualTable) vTablesToUnion.get(0))
									.getParentDataSetRelation());
					// Create a new union table based on tablesToUnion.
					// Action depends on prePartitionAction.
					final MartConstructorAction union = new Union(
							this.datasetSchemaName, vUnionTable
									.getDataSetTable().getName(), null,
							vUnionTable.getTempTableName(), null,
							vTablesToUnion);
					actionGraph.addActionWithParent(union, prePartitionAction);
					vUnionTable.setLastActionPerformed(union);
					// Add the union table to the list of unionTables.
					vUnionTables.add(vUnionTable);
					// Drop all the original tables that have been unioned now.
					for (int k = 0; k < vTablesToUnion.size(); k++) {
						final VirtualTable vTable = (VirtualTable) vTablesToUnion
								.get(k);
						// Drop table.
						final MartConstructorAction drop = new Drop(
								this.datasetSchemaName, vTable
										.getDataSetTable().getName(), null,
								vTable.getTempTableName());
						actionGraph.addActionWithParent(drop, union);
					}
				}
				vTables.clear();
				vTables.addAll(vUnionTables);

				// Set up a break-point last action that waits again for all
				// tables to complete.
				prePartitionAction = new PlaceHolder(this.datasetSchemaName);
				for (final Iterator i = vTables.iterator(); i.hasNext();)
					prePartitionAction.addParent(((VirtualTable) i.next())
							.getLastActionPerformed());
				actionGraph.addAction(prePartitionAction);
			}

			// Partition all partitioned columns.
			for (final Iterator i = dataset.getPartitionedDataSetColumns()
					.iterator(); i.hasNext();) {
				final List preDropChildActions = new ArrayList();
				final DataSetColumn dsPartCol = (DataSetColumn) i.next();

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
				final PartitionedColumnType dsPartColType = dataset
						.getPartitionedDataSetColumnType(dsPartCol);
				final Set partitionValues = new HashSet();
				if (dsPartColType instanceof SingleValue)
					partitionValues.add(((SingleValue) dsPartColType)
							.getValue());
				else if (dsPartColType instanceof ValueCollection) {
					partitionValues.addAll(((ValueCollection) dsPartColType)
							.getValues());
					if (((ValueCollection) dsPartColType).getIncludeNull())
						partitionValues.add(null);
				} else if (dsPartColType instanceof UniqueValues)
					if (dsPartCol instanceof WrappedColumn)
						partitionValues.addAll(this.helper
								.listDistinctValues(((WrappedColumn) dsPartCol)
										.getWrappedColumn()));
					else if (dsPartCol instanceof SchemaNameColumn)
						// Unique values for the schema column are the names
						// of all schemas involved in this dataset table. Ie.
						// the
						// schemas for each key in the underlying keys.
						for (final Iterator j = vParentTables.iterator(); j
								.hasNext();) {
							final VirtualTable vTable = (VirtualTable) j.next();
							for (final Iterator k = vTable.getDataSet()
									.getTables().iterator(); k.hasNext();)
								for (final Iterator l = ((DataSetTable) k
										.next()).getUnderlyingKeys().iterator(); l
										.hasNext();) {
									final Key key = (Key) l.next();
									final Schema keySchema = key.getTable()
											.getSchema();
									if (keySchema instanceof SchemaGroup)
										partitionValues
												.addAll(((SchemaGroup) keySchema)
														.getSchemas());
									else
										partitionValues.add(keySchema);
								}
						}
					else
						// Other column types not supported.
						throw new MartBuilderInternalError();

				// Do the partitioning. First, partition the table the column
				// belongs to. Then, partition every child, and every child
				// of every child, and so on recursively.

				// Construct the recursive list of child tables that will also
				// require partitioning.
				final List dsTablesToPartition = new ArrayList();
				dsTablesToPartition.add(dsParentTable);
				for (int j = 0; j < dsTablesToPartition.size(); j++)
					for (final Iterator l = ((DataSetTable) dsTablesToPartition
							.get(j)).getPrimaryKey().getRelations().iterator(); l
							.hasNext();)
						dsTablesToPartition.add(((Relation) l.next())
								.getManyKey().getTable());

				// Keep track of the last partition we do on this column.
				MartConstructorAction lastPartitionAction = prePartitionAction;

				// Find all child VirtualTables. Child VirtualTables are
				// those where the dataset table matches any of the
				// tablesToPartition. This will result in grouped schema
				// tables being partitioned for every value in the group,
				// regardless of the values in their individual schemas.
				final List vTablesToReduce = new ArrayList();
				for (final Iterator j = dsTablesToPartition.iterator(); j
						.hasNext();) {
					final DataSetTable childDSTable = (DataSetTable) j.next();
					if (childDSTable.equals(dsParentTable))
						continue;
					for (final Iterator l = vTables.iterator(); l.hasNext();) {
						final VirtualTable vChildTable = (VirtualTable) l
								.next();
						if (!vChildTable.getDataSetTable().equals(childDSTable))
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
				for (final Iterator j = vParentTables.iterator(); j.hasNext();) {
					final VirtualTable vParentTable = (VirtualTable) j.next();

					// Index the column to be partitioned.
					MartConstructorAction index = new Index(
							this.datasetSchemaName, vParentTable
									.getDataSetTable().getName(), null,
							vParentTable.getTempTableName(), vParentTable
									.getDataSetTable().getPrimaryKey()
									.getColumns());
					actionGraph.addActionWithParent(index, lastPartitionAction);
					lastPartitionAction = index;

					// Partition the parent table.
					// Do the partitioning, one table per value.
					final List reduceActions = new ArrayList();
					for (final Iterator k = partitionValues.iterator(); k
							.hasNext();) {
						final Object partitionValue = k.next();
						// Create an VirtualTable for the partitioned
						// value table. Add it to the list of all tables.
						final VirtualTable vPartitionTable = new VirtualTable(
								vParentTable.getDataSetTable(), vParentTable
										.getParentDataSetRelation());
						vPartitionTable.getPartitionValues().addAll(
								vParentTable.getPartitionValues());
						vPartitionTable.getPartitionValues()
								.add(partitionValue);
						vTables.add(vPartitionTable);
						// Partition the parent table for this value.
						final MartConstructorAction partition = new Partition(
								this.datasetSchemaName, vPartitionTable
										.getDataSetTable().getName(), null,
								vPartitionTable.getTempTableName(), null,
								vParentTable.getTempTableName(), dsPartCol
										.getName(), partitionValue);
						actionGraph.addActionWithParent(partition,
								lastPartitionAction);
						// Make the last action of each partitioned
						// table the partition action.
						vPartitionTable.setLastActionPerformed(partition);

						// Create an index over the primary key columns of
						// the newly partitioned table.
						index = new Index(this.datasetSchemaName,
								vPartitionTable.getDataSetTable().getName(),
								null, vPartitionTable.getTempTableName(),
								vParentTable.getDataSetTable().getPrimaryKey()
										.getColumns());
						actionGraph.addActionWithParent(index, partition);

						// For each child table, create a new VirtualTable
						// and generate it by doing a Reduce action. Add
						// the new VirtualTable to the list of all tables.
						for (final Iterator l = vTablesToReduce.iterator(); l
								.hasNext();) {
							final VirtualTable vChildTable = (VirtualTable) l
									.next();
							// Create an VirtualTable for the partitioned
							// child table. Add it to the list of all tables.
							final VirtualTable vReducedTable = new VirtualTable(
									vChildTable.getDataSetTable(), vChildTable
											.getParentDataSetRelation());
							vReducedTable.getPartitionValues().addAll(
									vChildTable.getPartitionValues());
							vReducedTable.getPartitionValues().add(
									partitionValue);
							vTables.add(vReducedTable);
							// Generate the table.
							final MartConstructorAction reduce = new Reduce(
									this.datasetSchemaName, vReducedTable
											.getDataSetTable().getName(), null,
									vReducedTable.getTempTableName(), null,
									vPartitionTable.getTempTableName(),
									vChildTable.getParentDataSetRelation()
											.getOneKey().getColumns(), null,
									vChildTable.getTempTableName(), vChildTable
											.getParentDataSetRelation()
											.getManyKey().getColumns());
							actionGraph.addActionWithParent(reduce, index);
							reduceActions.add(reduce);
							vReducedTable.setLastActionPerformed(reduce);
						}
					}
					// Drop previous table.
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
				for (final Iterator j = vTablesToReduce.iterator(); j.hasNext();) {
					final VirtualTable vTable = (VirtualTable) j.next();
					final MartConstructorAction drop = new Drop(
							this.datasetSchemaName, vTable.getDataSetTable()
									.getName(), null, vTable.getTempTableName());
					actionGraph.addActionWithParents(drop, preDropChildActions);
				}
				vTables.removeAll(vTablesToReduce);
			}

			// Set up a break-point last action that waits again for all
			// tables to complete.
			final MartConstructorAction prePKAction = new PlaceHolder(
					this.datasetSchemaName);
			for (final Iterator i = vTables.iterator(); i.hasNext();)
				prePKAction.addParent(((VirtualTable) i.next())
						.getLastActionPerformed());
			actionGraph.addAction(prePKAction);

			// Establish PKs and FKs.
			final List pkFkActions = new ArrayList();
			for (final Iterator i = vTables.iterator(); i.hasNext();) {
				final VirtualTable vParentTable = (VirtualTable) i.next();
				final List dsPKCols = vParentTable.getDataSetTable()
						.getPrimaryKey().getColumns();
				// Create a PK over pkKeyCols.
				final MartConstructorAction pk = new PK(this.datasetSchemaName,
						vParentTable.getDataSetTable().getName(), null,
						vParentTable.getTempTableName(), dsPKCols);
				actionGraph.addActionWithParent(pk, prePKAction);
				pkFkActions.add(pk);

				// Iterate over foreign keys from this PK.
				final Key dsPK = vParentTable.getDataSetTable().getPrimaryKey();
				for (final Iterator j = dsPK.getRelations().iterator(); j
						.hasNext();) {
					final Relation dsPKRelation = (Relation) j.next();
					final DataSetTable dsFKTable = (DataSetTable) dsPKRelation
							.getOtherKey(dsPK).getTable();
					// For each one, find all VirtualTables involved in
					// that relation, and index and establish FK.
					for (final Iterator k = vTables.iterator(); k.hasNext();) {
						final VirtualTable vChildTable = (VirtualTable) k
								.next();
						if (!vChildTable.getDataSetTable().equals(dsFKTable))
							continue;
						final List vParentPartitionValues = vParentTable
								.getPartitionValues();
						if (vChildTable.getPartitionValues().size() < vParentPartitionValues
								.size())
							continue;
						else if (!vChildTable.getPartitionValues().subList(0,
								vParentPartitionValues.size()).equals(
								vParentPartitionValues))
							continue;
						final Key dsFK = (Key) vChildTable.getDataSetTable()
								.getForeignKeys().iterator().next();
						final List dsFKCols = dsFK.getColumns();

						// Index fkKeyCols.
						final MartConstructorAction index = new Index(
								this.datasetSchemaName, vChildTable
										.getDataSetTable().getName(), null,
								vChildTable.getTempTableName(), dsFKCols);
						actionGraph.addActionWithParent(index, pk);
						// Create a FK over fkKeyCols.
						final MartConstructorAction fk = new FK(
								this.datasetSchemaName, vChildTable
										.getDataSetTable().getName(), null,
								vChildTable.getTempTableName(), dsFKCols, null,
								vParentTable.getTempTableName(), dsPKCols);
						actionGraph.addActionWithParent(fk, index);
						// Add action to list of pkFkActions.
						pkFkActions.add(fk);
					}

				}
			}

			// Set up a break-point last action that waits for all
			// foreign keys to complete.
			final MartConstructorAction prePCOAction = new PlaceHolder(
					this.datasetSchemaName);
			for (final Iterator i = pkFkActions.iterator(); i.hasNext();)
				prePCOAction.addParent((MartConstructorAction) i.next());
			actionGraph.addAction(prePCOAction);

			// Post-construction optimisation.
			final List optActions = new ArrayList();
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
					// But if we are using table join, create a 'has' table
					// and add columns to that instead.
					if (dsOptimiserType.equals(DataSetOptimiserType.TABLE)) {
						vHasTable = new VirtualHasTable(vParentTable
								.getDataSetTable());
						vHasTables.add(vHasTable);
						vHasTable.getPartitionValues().addAll(
								vParentTable.getPartitionValues());
						// Work out what columns to include from the
						// main/subclass table.
						final List dsPKCols = vParentTable.getDataSetTable()
								.getPrimaryKey().getColumns();
						// Create the new 'has' table with the columns
						// from the main/subclass table.
						final MartConstructorAction create = new Create(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), null,
								vParentTable.getTempTableName(), dsPKCols,
								false, null, false);
						actionGraph.addActionWithParent(create, preDOAction);
						// Create the PK on the 'has' table using the same
						// columns.
						final MartConstructorAction pk = new PK(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), dsPKCols);
						actionGraph.addActionWithParent(pk, create);
						// Create the FK on the same columns relating back
						// to the main/subclass table.
						final MartConstructorAction fk = new FK(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), dsPKCols, null,
								vParentTable.getTempTableName(), dsPKCols);
						actionGraph.addActionWithParent(fk, pk);
						preDOAction = fk;
					}
					// FIXME - Not adding any columns!
					// For each main or subclass table, identify all dimensions.
					for (final Iterator j = vTables.iterator(); j.hasNext();) {
						final VirtualTable vChildTable = (VirtualTable) j
								.next();
						if (!vChildTable.getDataSetTable().getType().equals(
								DataSetTableType.DIMENSION))
							continue;
						else if (!vChildTable.getParentDataSetRelation()
								.getOneKey().getTable().equals(vParentTable.getDataSetTable()))
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
						// Work out 'has' column name.
						final String vHasColumnName = vChildTable
								.createFinalName()
								+ Resources.get("tablenameSep")
								+ Resources.get("hasSuffix");
						// Add columns to the main or subclass table or
						// 'has' table.
						final MartConstructorAction optimiseAdd = new OptimiseAddColumn(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), vHasColumnName);
						actionGraph.addActionWithParent(optimiseAdd,
								preDOAction);
						// Update those columns using inner join
						// on has table PK to dimension table FK.
						final MartConstructorAction optimiseUpd = new OptimiseUpdateColumn(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vChildTable.getTempTableName(), vChildTable
										.getParentDataSetRelation()
										.getManyKey().getColumns(), null,
								vHasTable.getTempTableName(), vHasTable
										.getDataSetTable().getPrimaryKey()
										.getColumns(), vHasColumnName);
						actionGraph.addActionWithParent(optimiseUpd,
								optimiseAdd);
						// Index the has-column.
						final MartConstructorAction optimiseInd = new Index(
								this.datasetSchemaName, vHasTable
										.getDataSetTable().getName(), null,
								vHasTable.getTempTableName(), Collections
										.singletonList(vHasColumnName));
						actionGraph.addActionWithParent(optimiseInd,
								optimiseUpd);
						// Add final index to list of actions to wait for
						// before doing renames later.
						optActions.add(optimiseInd);
					}
				}
				vTables.addAll(vHasTables);
			}

			// Set up a break-point last action that waits for all
			// optimisations to complete.
			final MartConstructorAction preRenameAction = new PlaceHolder(
					this.datasetSchemaName);
			if (!optActions.isEmpty())
				for (final Iterator i = optActions.iterator(); i.hasNext();)
					preRenameAction.addParent((MartConstructorAction) i.next());
			else
				preRenameAction.addParent(prePCOAction);
			actionGraph.addAction(preRenameAction);

			// Rename all tables.
			for (final Iterator i = vTables.iterator(); i.hasNext();) {
				final VirtualTable vTable = (VirtualTable) i.next();
				final String vTableFinalName = vTable.createFinalName();
				final String vTableTempName = vTable.getTempTableName();
				// Rename the table.
				final MartConstructorAction rename = new Rename(
						this.datasetSchemaName, vTable.getDataSetTable()
								.getName(), null, vTableTempName,
						vTableFinalName);
				actionGraph.addActionWithParent(rename, preRenameAction);
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

		private void processTable(final VirtualTable vParentTable,
				final MartConstructorAction firstActionDependsOn,
				final MartConstructorActionGraph actionGraph,
				final Schema rMainTableSchema, final Schema rSchema,
				final VirtualTable vConstructionTable) throws Exception {
			// A placeholder for the last action performed on this table.
			MartConstructorAction lastActionPerformed = firstActionDependsOn;

			// Placeholder for name of the target temp table that will
			// contain the constructed table.
			String vTableTempName = vConstructionTable.getTempTableName();

			// If the table is a MAIN table, then we can select columns
			// directly from the underlying table.
			if (vConstructionTable.getDataSetTable().getType().equals(
					DataSetTableType.MAIN)) {
				// We now need to work out at which point to start the process
				// of transforming the table. That point is either the
				// underlying
				// table, or the first table in the list of underlying
				// relations,
				// if a list has been specified.
				final Table rFirstTable = vConstructionTable.getRealTable();

				// Work out what columns to include from the first table.
				final List dsFirstTableCols = vConstructionTable
						.getDataSetColumns(rFirstTable.getColumns());

				// Include the schema name column if there is one (don't need
				// to do this for dimension/subclass tables as it will be part
				// of the primary key inherited from the parent table).
				for (final Iterator i = vConstructionTable.getDataSetTable()
						.getColumns().iterator(); i.hasNext();) {
					final DataSetColumn dsCol = (DataSetColumn) i.next();
					if (dsCol instanceof SchemaNameColumn)
						dsFirstTableCols.add(dsCol);
				}

				// Create the table based on firstTable, and selecting
				// based on firstDSCols.
				final MartConstructorAction create = new Create(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vTableTempName, rSchema, rFirstTable.getName(),
						dsFirstTableCols, rFirstTable.getPrimaryKey() == null,
						vConstructionTable.getDataSet().getRestrictedTableType(
								rFirstTable), true);
				actionGraph.addActionWithParent(create, lastActionPerformed);
				// Update last action performed, in case there are no merges.
				lastActionPerformed = create;
			}

			// Else, if the table is NOT a MAIN table, then we should select
			// the inherited columns from the parent dataset table.
			else {
				// Work out what columns to include from the first table.
				final List dsTableFirstCols = new ArrayList();
				for (final Iterator i = vConstructionTable.getDataSetTable()
						.getColumns().iterator(); i.hasNext();) {
					final DataSetColumn dsCol = (DataSetColumn) i.next();
					if (dsCol instanceof InheritedColumn) {
						final InheritedColumn dsInheritedCol = (InheritedColumn) dsCol;
						if (!vConstructionTable.getDataSet()
								.getMaskedDataSetColumns().contains(
										dsInheritedCol))
							dsTableFirstCols.add(dsInheritedCol);
					}
				}
				
				final MartConstructorAction create = new Create(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vTableTempName, null, vParentTable.getTempTableName(),
						dsTableFirstCols, false, null, false);
				actionGraph.addActionWithParent(create, lastActionPerformed);
				// Update last action performed, in case there are no merges.
				lastActionPerformed = create;
			}

			// Merge subsequent tables based on underlying relations.
			for (int i = 0; i < vConstructionTable
					.getDataSetTable().getUnderlyingKeys().size(); i++) {
				// What key was followed from the previous table?
				final Key dsSourceKey = (Key) vConstructionTable
						.getDataSetTable().getUnderlyingKeys().get(i);
				// What relation was followed?
				final Relation rSourceRelation = (Relation) vConstructionTable
						.getDataSetTable().getUnderlyingRelations().get(i);
				// Left or inner join?
				final boolean useLeftJoin = rSourceRelation.isOptional();
				// What key did we reach by following the relation?
				final Key dsTargetKey = rSourceRelation
						.getOtherKey(dsSourceKey);
				// What table did we reach, and therefore need to merge now?
				final Table dsTargetTable = dsTargetKey.getTable();
				String dsTargetTableName = dsTargetTable.getName();
				// Is the target table going to need a union merge?
				final boolean useUnionMerge = dsTargetTable.getSchema() instanceof SchemaGroup
						&& !dsTargetTable.getSchema().equals(rMainTableSchema);
				// What are the equivalent columns on the existing temp table
				// that correspond to the key on the previous table?
				final List dsSourceKeyCols = vConstructionTable
						.getDataSetColumns(dsSourceKey.getColumns());
				// What are the columns we should merge from this new table?
				final List dsTargetIncludeCols = vConstructionTable
						.getDataSetColumns(rSourceRelation);
				// If targetTable is in a group schema that is not the
				// same group schema we started with, create a union table
				// containing all the targetTable copies, then merge with
				// that instead.
				Schema rTargetSchema;
				if (useUnionMerge) {
					// Build a list of schemas to union tables from.
					final List rUnionSchemas = new ArrayList();
					final List rUnionTableNames = new ArrayList();
					if (dsTargetTable.getSchema() instanceof SchemaGroup)
						for (final Iterator j = ((SchemaGroup) dsTargetTable
								.getSchema()).getSchemas().iterator(); j
								.hasNext();) {
							final Schema rUnionSchema = (Schema) j.next();
							rUnionSchemas.add(rUnionSchema);
							rUnionTableNames.add(dsTargetTable.getName());
						}
					else {
						rUnionSchemas.add(dsTargetTable.getSchema());
						rUnionTableNames.add(dsTargetTable.getName());
					}
					// Make name for union table, which is now the target table.
					dsTargetTableName = this.helper.getNewTempTableName();
					// Create union table.
					final MartConstructorAction union = new Union(
							this.datasetSchemaName, vConstructionTable
									.getDataSetTable().getName(), null,
							dsTargetTableName, rUnionSchemas, rUnionTableNames);
					actionGraph.addActionWithParent(union, lastActionPerformed);
					// Create index on targetKey on union table.
					final MartConstructorAction index = new Index(
							this.datasetSchemaName, vConstructionTable
									.getDataSetTable().getName(), null,
							dsTargetTableName, dsTargetKey.getColumns());
					actionGraph.addActionWithParent(index, union);
					// Update the last action performed to reflect the index
					// of the union table.
					lastActionPerformed = index;
					// Target schema is now the dataset schema. Null
					// represents this.
					rTargetSchema = null;
				} else
					// Target schema is same as source schema.
					rTargetSchema = rSchema;
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
						dsSourceKeyCols, useLeftJoin, rTargetSchema,
						dsTargetTableName, dsTargetKey.getColumns(),
						dsTargetIncludeCols, vConstructionTable.getDataSet()
								.getRestrictedRelationType(rSourceRelation),
						dsTargetTable.equals(rSourceRelation.getSecondKey()
								.getTable()), rSourceRelation.isManyToMany(),
						vConstructionTable.getDataSet().getRestrictedTableType(
								dsTargetTable), true);
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
				// If targetTable was a union table, drop it.
				if (useUnionMerge) {
					// Drop union target table. Dependent on
					// lastActionPerformed, but does not update it.
					drop = new Drop(this.datasetSchemaName, vConstructionTable
							.getDataSetTable().getName(), null,
							dsTargetTableName);
					actionGraph.addActionWithParent(drop, merge);
				}
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
				// Is the target table going to need a union merge?
				final boolean useUnionMerge = !rConcatTargetTable.getSchema()
						.equals(rMainTableSchema);
				// What columns on the table should be concatted?
				final Key rConcatTargetPK = rConcatTargetTable.getPrimaryKey();
				// What are the equivalent columns on the existing temp table
				// that correspond to the source key?
				final List vSourceKeyCols = vConstructionTable
						.getNthDataSetColumns(rConcatSourceKey.getColumns(),
								rConcatRelation);
				// If concatColumn is in a table in a group schema
				// that is not the same group schema we started with, create
				// a union table containing all the concatTable copies, then
				// concat that instead.
				Schema rConcatTargetSchema;
				if (useUnionMerge) {
					// Build a list of schemas to union tables from.
					final List rUnionTableSchemas = new ArrayList();
					final List rUnionTableNames = new ArrayList();
					if (rConcatTargetTable.getSchema() instanceof SchemaGroup)
						for (final Iterator j = ((SchemaGroup) rConcatTargetTable
								.getSchema()).getSchemas().iterator(); j
								.hasNext();) {
							final Schema rUnionSchema = (Schema) j.next();
							rUnionTableSchemas.add(rUnionSchema);
							rUnionTableNames.add(rConcatTargetTable.getName());
						}
					else {
						rUnionTableSchemas.add(rConcatTargetTable.getSchema());
						rUnionTableNames.add(rConcatTargetTable.getName());
					}
					// Make name for union table, which is now the target table.
					// Note how the target table is now a temp table.
					rConcatTargetTableName = this.helper.getNewTempTableName();
					// Create union table.
					final MartConstructorAction union = new Union(
							this.datasetSchemaName, vConstructionTable
									.getDataSetTable().getName(), null,
							rConcatTargetTableName, rUnionTableSchemas,
							rUnionTableNames);
					actionGraph.addActionWithParent(union, lastActionPerformed);
					// Create index on targetKey on union table. Dependent
					// on lastActionPerformed, and updates it.
					final MartConstructorAction index = new Index(
							this.datasetSchemaName, vConstructionTable
									.getDataSetTable().getName(), null,
							rConcatTargetTableName, rConcatTargetKey
									.getColumns());
					actionGraph.addActionWithParent(index, union);
					lastActionPerformed = index;
					// Target schema is now the dataset schema. Null
					// represents this.
					rConcatTargetSchema = null;
				} else
					// Target schema name is same as source schema.
					rConcatTargetSchema = rSchema;
				// Create the concat table by selecting sourceKey.
				String vConcatTableTempName = this.helper.getNewTempTableName();
				final MartConstructorAction createCon = new Create(
						this.datasetSchemaName, vConstructionTable
								.getDataSetTable().getName(), null,
						vConcatTableTempName, null, vTableTempName,
						vSourceKeyCols, false, null, false);
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
						vSourceKeyCols, rConcatTargetSchema,
						rConcatTargetTableName, rConcatTargetKey.getColumns(),
						rConcatTargetPK.getColumns(), dsConcatCol.getName(),
						vConstructionTable.getDataSet().getConcatRelationType(
								rConcatRelation), vConstructionTable
								.getDataSet().getRestrictedRelationType(
										rConcatRelation), rConcatTargetTable
								.equals(rConcatRelation.getSecondKey()
										.getTable()), vConstructionTable
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
						vSourceKeyCols, true, null, vConcatTableTempName,
						vSourceKeyCols, Collections.singletonList(dsConcatCol),
						null, false, false, null, false);
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
				// If concat table was a union table, drop it.
				if (useUnionMerge) {
					// Drop union concat table. Dependent on
					// lastActionPerformed, but does not update it.
					drop = new Drop(this.datasetSchemaName, vConstructionTable
							.getDataSetTable().getName(), null,
							rConcatTargetTableName);
					actionGraph.addActionWithParent(drop, merge);
				}
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
				final List vSelectedColumns = new ArrayList(vConstructionTable
						.getDataSetTable().getColumns());
				vSelectedColumns.removeAll(vConstructionTable.getDataSet()
						.getMaskedDataSetColumns());
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
						vTableTempName, null, vExpressionTableTempName,
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

			// Update the temp table name.
			vConstructionTable.setTempTableName(vTableTempName);

			// Return the last action performed to create this table.
			vConstructionTable.setLastActionPerformed(lastActionPerformed);
		}

		public String getStatusMessage() {
			return this.statusMessage;
		}

		public int getPercentComplete() {
			return (int) this.percentComplete;
		}

		public Exception getFailureException() {
			return this.failure;
		}

		public void cancel() {
			this.cancelled = true;
		}

		private void checkCancelled() throws ConstructorException {
			if (this.cancelled)
				throw new ConstructorException(Resources.get("mcCancelled"));
		}

		// Internal use only, serves as a convenient wrapper for tracking
		// info about a particular dataset table.
		private class VirtualTable {
			private DataSetTable datasetTable;

			private Relation parentDataSetRelation;

			private String tempTableName;

			private List partitionValues;

			private MartConstructorAction lastActionPerformed;

			private Map counter;

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
				this.counter = new HashMap();
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
			 * Obtain the list of values used to partition this table, in the
			 * order in which those partitions were made.
			 * 
			 * @return the list of values used to partition this table.
			 */
			public List getPartitionValues() {
				return this.partitionValues;
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
			 * Work out what schema the real table of this virtual table is in.
			 * 
			 * @return the schema that the real table of this virtual table is
			 *         in.
			 */
			public Schema getRealSchema() {
				return this.datasetTable.getUnderlyingTable().getSchema();
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

			private DataSetColumn getDataSetColumn(int n,
					final Column interestingColumn) {
				// We must look not only for this column, but all columns
				// involved in relations with it, because it could have been
				// introduced via another route.
				final List candidates = new ArrayList();
				candidates.add(interestingColumn);
				for (int j = 0; j < candidates.size(); j++) {
					final Column searchColumn = (Column) candidates.get(j);
					for (final Iterator i = this.datasetTable.getColumns()
							.iterator(); i.hasNext();) {
						final DataSetColumn candidate = (DataSetColumn) i
								.next();
						DataSetColumn test = candidate;
						if (test instanceof InheritedColumn)
							test = ((InheritedColumn) candidate)
									.getInheritedColumn();
						if (!(test instanceof WrappedColumn))
							continue;
						final WrappedColumn wc = (WrappedColumn) test;
						if (this.getDataSet().getMaskedDataSetColumns()
								.contains(wc)
								&& !wc.getDependency())
							continue;
						if (searchColumn.equals(wc.getWrappedColumn())
								&& n-- == 0)
							return candidate;
					}
					// If got here, didn't find this search candidate, so add
					// all the related columns in other keys.
					for (final Iterator i = searchColumn.getTable().getKeys()
							.iterator(); i.hasNext();) {
						final Key key = (Key) i.next();
						final int colIndex = key.getColumns().indexOf(
								searchColumn);
						if (colIndex < 0)
							continue;
						for (final Iterator k = key.getRelations().iterator(); k
								.hasNext();) {
							final Relation rel = (Relation) k.next();
							final Key otherKey = rel.getOtherKey(key);
							final Column nextCandidate = (Column) otherKey
									.getColumns().get(colIndex);
							if (!candidates.contains(nextCandidate))
								candidates.add(nextCandidate);
						}
					}
				}
				// If we get here, it means the specified column is not part
				// of the unmasked columns on this dataset table.
				return null;
			}

			/**
			 * Given a set of interesting columns from normal tables (not
			 * dataset tables), return the 0th set of matching dataset columns.
			 * This means that if the interesting column appears twice in the
			 * table as a dataset column, and n=1, then the 2nd instance is the
			 * one that gets returned. (n is 0-indexed).
			 * 
			 * @param interestingColumns
			 *            the set of normal columns to find matching dataset
			 *            columns for.
			 * @return the set of 0th matching dataset columns.
			 */
			public List getDataSetColumns(final Collection interestingColumns) {
				final List list = new ArrayList();
				for (final Iterator i = interestingColumns.iterator(); i
						.hasNext();) {
					final DataSetColumn col = this.getDataSetColumn(0,
							(Column) i.next());
					if (col != null)
						list.add(col);
				}
				return list;
			}

			/**
			 * Given a relation, find all columns in this dataset table that
			 * were added as a result of following this relation.
			 * 
			 * @param underlyingRelation
			 *            the relation to look for associated columns for.
			 * @return the columns added to this table by that relation. May be
			 *         empty but never null.
			 */
			public List getDataSetColumns(final Relation underlyingRelation) {
				final List list = new ArrayList();
				for (final Iterator i = this.datasetTable.getColumns()
						.iterator(); i.hasNext();) {
					final DataSetColumn candidate = (DataSetColumn) i.next();
					if (!(candidate instanceof WrappedColumn))
						continue;
					final WrappedColumn wc = (WrappedColumn) candidate;
					if (this.getDataSet().getMaskedDataSetColumns()
							.contains(wc)
							&& !wc.getDependency())
						continue;
					final Relation wcRel = wc.getUnderlyingRelation();
					if (wcRel == underlyingRelation)
						list.add(candidate);
					else if (wcRel != null
							&& underlyingRelation != null
							&& wc.getUnderlyingRelation().equals(
									underlyingRelation))
						list.add(candidate);
				}
				return list;
			}

			/**
			 * Given a set of interesting columns from normal tables (not
			 * dataset tables), return the nth set of matching dataset columns.
			 * This means that if the interesting column appears twice in the
			 * table as a dataset column, and n=1, then the 2nd instance is the
			 * one that gets returned. (n is 0-indexed).
			 * 
			 * @param interestingColumns
			 *            the set of normal columns to find matching dataset
			 *            columns for.
			 * @param counter
			 *            the counter object to retrieve the value n from. The
			 *            first time any object is used, n=0. The next time that
			 *            same object is seen, n=1, and so on.
			 * @return the set of nth matching dataset columns.
			 */
			public List getNthDataSetColumns(
					final Collection interestingColumns, final Object counter) {
				final int n = this.incrementCount(counter);
				final List list = new ArrayList();
				for (final Iterator i = interestingColumns.iterator(); i
						.hasNext();) {
					final DataSetColumn col = this.getDataSetColumn(n,
							(Column) i.next());
					if (col != null)
						list.add(col);
				}
				return list;
			}

			private int incrementCount(final Object counter) {
				int count;
				if (!this.counter.containsKey(counter)) {
					count = 0;
					this.counter.put(counter, new Integer(count));
				} else {
					count = ((Integer) this.counter.get(counter)).intValue();
					count++;
					this.counter.put(counter, new Integer(count));
				}
				return count;
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
			 * Constructs a string containing the recommended table name for
			 * this dataset table in the final schema.
			 * 
			 * @return the constructed name for this dataset table.
			 */
			public String createFinalName() {
				// TODO - come up with a better naming scheme
				// Currently the name is:
				// datasetname__\
				// {{partitionvalue{_partitionvalue}*}__}+\
				// tablename__\
				// type

				// Work out what dataset we are in.
				final DataSet dataset = this.getDataSet();
				final DataSetTable datasetTable = this.getDataSetTable();

				final StringBuffer name = new StringBuffer();

				// Dataset name and __ separator.
				name.append(dataset.getName());
				name.append(Resources.get("tablenameSep"));

				// Partition values and __ separator with _ separator between.
				final List partitionValues = this.getPartitionValues();
				if (!partitionValues.isEmpty()) {
					for (final Iterator j = partitionValues.iterator(); j
							.hasNext();) {
						// Partition values may be null, so cannot use
						// toString().
						final String partitionValue = "" + j.next();
						name.append(partitionValue);
						if (j.hasNext())
							name.append(Resources.get("tablenameSubSep"));
					}
					name.append(Resources.get("tablenameSep"));
				}

				// Table name and __ separator.
				name.append(datasetTable.getName());
				name.append(Resources.get("tablenameSep"));

				// Type.
				final DataSetTableType type = datasetTable.getType();
				if (type.equals(DataSetTableType.MAIN))
					name.append(Resources.get("mainSuffix"));
				else if (type.equals(DataSetTableType.MAIN_SUBCLASS))
					name.append(Resources.get("subclassSuffix"));
				else if (type.equals(DataSetTableType.DIMENSION))
					name.append(Resources.get("dimensionSuffix"));

				return name.toString();
			}
		}

		// Internal use only, serves as a convenient wrapper for tracking
		// info about a particular 'has' table.
		private class VirtualHasTable extends VirtualTable {
			/**
			 * Constructor starts tracking a 'has' table that contains
			 * optimisations for the given table.
			 * 
			 * @param datasetTable
			 *            the table to contain optimisations for.
			 */
			public VirtualHasTable(final DataSetTable datasetTable) {
				super(datasetTable, null);
			}

			public String createFinalName() {
				final StringBuffer sb = new StringBuffer();
				sb.append(super.createFinalName());
				sb.append(Resources.get("tablenameSep"));
				sb.append(Resources.get("hasSuffix"));
				return sb.toString();
			}
		}
	}
}
