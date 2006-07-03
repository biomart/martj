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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.DataSet.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.DataSet.PartitionedColumnType.ValueCollection;
import org.biomart.builder.model.MartConstructorAction.Concat;
import org.biomart.builder.model.MartConstructorAction.Create;
import org.biomart.builder.model.MartConstructorAction.Drop;
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
import org.biomart.builder.resources.BuilderBundle;

/**
 * This interface defines the behaviour expected from an object which can take a
 * dataset and actually construct a mart based on this information. Whether it
 * carries out the task or just writes some DDL to be run by the user later is
 * up to the implementor.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.16, 29th June 2006
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
	 * @param datasetSchemaName
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
		 * @param ds
		 *            the dataset(s) to transform into a mart.
		 * @param helper
		 *            the helper to use in the transformation.
		 */
		public GenericConstructorRunnable(String datasetSchemaName,
				Collection datasets, Helper helper) {
			super();
			this.datasets = datasets;
			this.helper = helper;
			this.martConstructorListeners = new ArrayList();
			this.datasetSchemaName = datasetSchemaName;
		}

		public void addMartConstructorListener(MartConstructorListener listener) {
			this.martConstructorListeners.add(listener);
		}

		private void issueListenerEvent(int event) throws Exception {
			this.issueListenerEvent(event, null);
		}

		private void issueListenerEvent(int event, MartConstructorAction action)
				throws Exception {
			for (Iterator i = this.martConstructorListeners.iterator(); i
					.hasNext();)
				((MartConstructorListener) i.next())
						.martConstructorEventOccurred(event, action);
		}

		public void run() {
			try {
				// Split the datasets into groups by mart.
				Map martDataSets = new HashMap();
				for (Iterator i = this.datasets.iterator(); i.hasNext();) {
					DataSet ds = (DataSet) i.next();
					Mart mart = (Mart) ds.getMart();
					if (!martDataSets.containsKey(mart))
						martDataSets.put(mart, new ArrayList());
					((List) martDataSets.get(mart)).add(ds);
				}

				// Begin.
				this
						.issueListenerEvent(MartConstructorListener.CONSTRUCTION_STARTED);

				// Work out how many datasets we have.
				int totalDataSetCount = datasets.size();

				// Loop over each mart.
				for (Iterator j = martDataSets.keySet().iterator(); j.hasNext();) {
					Mart mart = (Mart) j.next();
					this
							.issueListenerEvent(MartConstructorListener.MART_STARTED);

					try {
						// Loop over all the datasets we want included from this
						// mart.
						for (Iterator i = ((List) martDataSets.get(mart))
								.iterator(); i.hasNext();)
							this.doIt((DataSet) i.next(), totalDataSetCount);
					} finally {
						this
								.issueListenerEvent(MartConstructorListener.MART_ENDED);
					}
				}
			} catch (ConstructorException e) {
				this.failure = e;
			} catch (Throwable t) {
				this.failure = new ConstructorException(t);
			} finally {
				try {
					this
							.issueListenerEvent(MartConstructorListener.CONSTRUCTION_ENDED);
				} catch (ConstructorException e) {
					this.failure = e;
				} catch (Throwable t) {
					this.failure = new ConstructorException(t);
				}
			}
		}

		private void doIt(DataSet ds, int totalDataSetCount) throws Exception {
			// Find the main table of the dataset.
			DataSetTable mainTable = null;
			for (Iterator i = ds.getTables().iterator(); i.hasNext()
					&& mainTable == null;) {
				DataSetTable table = (DataSetTable) i.next();
				if (table.getType().equals(DataSetTableType.MAIN))
					mainTable = table;
			}

			// Work out if we are dealing with a main table that is in a group
			// schema. If it is, we make a list of all the schemas in the group,
			// otherwise the list is a singleton list containing the schema of
			// the main table. This list will be used later to merge schema
			// results together, unless partitioning by schema.
			Schema mainTableSchema = mainTable.getUnderlyingTable().getSchema();
			Collection schemas = null;
			if (mainTableSchema instanceof SchemaGroup)
				schemas = ((SchemaGroup) mainTableSchema).getSchemas();
			else
				schemas = Collections.singletonList(mainTableSchema);

			// Check not cancelled.
			this.checkCancelled();

			// We have to make space to store the graph of actions we
			// must carry out.
			this.statusMessage = BuilderBundle.getString("mcCreatingGraph");
			MartConstructorActionGraph actionGraph = new MartConstructorActionGraph();

			// Establish a root action.
			MartConstructorAction rootAction = new PlaceHolder(
					this.datasetSchemaName);
			actionGraph.addAction(rootAction);

			// Make a list to hold the table representations.
			List tables = new ArrayList();

			// For each schema, we process all the tables.
			for (Iterator i = schemas.iterator(); i.hasNext();) {
				Schema schema = (Schema) i.next();

				// Make a list of tables for this schema.
				List schemaTables = new ArrayList();

				// Process the main table.
				MCDSTable table = new MCDSTable(mainTable, null);
				this.processTable(rootAction, actionGraph, mainTableSchema,
						schema, table);
				schemaTables.add(table);
				MartConstructorAction lastMainAction = table
						.getLastActionPerformed();

				// Check not cancelled.
				this.checkCancelled();

				// Process all main dimensions and subclasses.
				for (Iterator j = mainTable.getPrimaryKey().getRelations()
						.iterator(); j.hasNext();) {
					Relation relation = (Relation) j.next();
					DataSetTable targetTable = (DataSetTable) relation
							.getManyKey().getTable();
					table = new MCDSTable(targetTable, relation);
					this.processTable(lastMainAction, actionGraph,
							mainTableSchema, schema, table);
					schemaTables.add(table);

					// Check not cancelled.
					this.checkCancelled();

					// Process all subclass dimensions.
					if (targetTable.getType().equals(
							DataSetTableType.MAIN_SUBCLASS)) {
						MartConstructorAction lastSCAction = table
								.getLastActionPerformed();
						for (Iterator k = targetTable.getPrimaryKey()
								.getRelations().iterator(); k.hasNext();) {
							Relation scRelation = (Relation) k.next();
							DataSetTable scTargetTable = (DataSetTable) scRelation
									.getManyKey().getTable();
							table = new MCDSTable(scTargetTable, scRelation);
							this.processTable(lastSCAction, actionGraph,
									mainTableSchema, schema, table);
							schemaTables.add(table);

							// Check not cancelled.
							this.checkCancelled();
						}
					}
				}

				// If partitioning by schema, add schema to partition
				// values on each table representation in schemaTables.
				if (ds.getPartitionOnSchema())
					for (Iterator j = schemaTables.iterator(); j.hasNext();)
						((MCDSTable) j.next()).getPartitionValues().add(
								schema.getName());

				// Add tables for this schema to list of all tables.
				tables.addAll(schemaTables);
			}

			// Set up a break-point last action that waits for all
			// tables to complete.
			MartConstructorAction prePartitionAction = new PlaceHolder(
					this.datasetSchemaName);
			for (Iterator i = tables.iterator(); i.hasNext();)
				prePartitionAction.addParent(((MCDSTable) i.next())
						.getLastActionPerformed());
			actionGraph.addAction(prePartitionAction);

			// If not partitioning by schema, and main table schema is
			// a schema group, do a union on all schemas for each table.
			if (!ds.getPartitionOnSchema()
					&& mainTableSchema instanceof SchemaGroup) {
				// Merge each set of tables.
				List unionTables = new ArrayList();
				for (int i = 0; i < tables.size() / schemas.size(); i++) {
					List tableSchemasToUnion = new ArrayList();
					List tablesToUnion = new ArrayList();
					for (int j = i; j < tables.size(); j += schemas.size()) {
						tableSchemasToUnion.add(null);
						tablesToUnion.add(tables.get(j));
					}
					// Create the MCDSTable entry for the union table.
					MCDSTable unionTable = new MCDSTable(
							((MCDSTable) tablesToUnion.get(0))
									.getDataSetTable(),
							((MCDSTable) tablesToUnion.get(0))
									.getParentDataSetRelation());
					// Create a new union table based on tablesToUnion.
					// Action depends on prePartitionAction.
					MartConstructorAction union = new Union(
							this.datasetSchemaName, null, unionTable
									.getTempTableName(), tableSchemasToUnion,
							tablesToUnion);
					actionGraph.addActionWithParent(union, prePartitionAction);
					// Make the last action of the union table the
					// union action.
					unionTable.setLastActionPerformed(union);
					// Add the union table to the list of unionTables.
					unionTables.add(unionTable);
					// Drop all previous tables in tablesToUnion.
					for (int k = 0; k < tablesToUnion.size(); k++) {
						Schema schema = (Schema) tableSchemasToUnion.get(k);
						MCDSTable table = (MCDSTable) tablesToUnion.get(k);
						// Drop table.
						// Action depends on union action.
						MartConstructorAction drop = new Drop(
								this.datasetSchemaName, schema, table
										.getTempTableName());
						actionGraph.addActionWithParent(drop, union);
					}
				}
				tables.clear();
				tables.addAll(unionTables);

				// Set up a break-point last action that waits again for all
				// tables to complete.
				prePartitionAction = new PlaceHolder(this.datasetSchemaName);
				for (Iterator i = tables.iterator(); i.hasNext();)
					prePartitionAction.addParent(((MCDSTable) i.next())
							.getLastActionPerformed());
				actionGraph.addAction(prePartitionAction);
			}

			// Partition all partitioned columns.
			for (Iterator i = ds.getPartitionedWrappedColumns().iterator(); i
					.hasNext();) {
				WrappedColumn partCol = (WrappedColumn) i.next();

				// Work out what partition values we will use.
				PartitionedColumnType partColType = ds
						.getPartitionedWrappedColumnType(partCol);
				List partValues = new ArrayList();
				if (partColType instanceof SingleValue) {
					partValues.add(((SingleValue) partColType).getValue());
				} else if (partColType instanceof ValueCollection) {
					partValues.addAll(((ValueCollection) partColType)
							.getValues());
					if (((ValueCollection) partColType).getIncludeNull())
						partValues.add(null);
				} else if (partColType instanceof UniqueValues) {
					partValues.addAll(this.helper.listDistinctValues(partCol
							.getWrappedColumn()));
				}

				// Do the partitioning. First, partition the table the column
				// belongs to. Then, partition every child, and every child
				// of every child, and so on recursively.

				// Look up the MCDSTable(s) containing the parent table.
				DataSetTable parentDSTable = (DataSetTable) partCol.getTable();
				List parentTables = new ArrayList();
				for (Iterator j = tables.iterator(); j.hasNext();) {
					MCDSTable table = (MCDSTable) j.next();
					if (table.getDataSetTable().equals(parentDSTable))
						parentTables.add(table);
				}

				// Construct the recursive list of child tables that will also
				// require partitioning.
				List tablesToPartition = new ArrayList();
				tablesToPartition.add(parentDSTable);
				for (int j = 0; j < tablesToPartition.size(); j++)
					for (Iterator l = ((DataSetTable) tablesToPartition.get(j))
							.getPrimaryKey().getRelations().iterator(); l
							.hasNext();)
						tablesToPartition.add((DataSetTable) ((Relation) l
								.next()).getManyKey().getTable());

				// Keep track of the last partition we do on this column.
				MartConstructorAction lastPartitionAction = prePartitionAction;

				// Find all child MCDSTables. Child MCDSTables are
				// those where the dataset table matches any of the
				// tablesToPartition. This will result in grouped schema
				// tables being partitioned for every value in the group,
				// regardless of the values in their individual schemas.
				List tablesToReduce = new ArrayList();
				for (Iterator j = tablesToPartition.iterator(); j.hasNext();) {
					DataSetTable childDSTable = (DataSetTable) j.next();
					for (Iterator l = tables.iterator(); l.hasNext();) {
						MCDSTable childTable = (MCDSTable) l.next();
						if (!childTable.getDataSetTable().equals(childDSTable))
							continue;
						tablesToReduce.add(childTable);
						// Create an index over the foreign key columns of
						// each child table.
						MartConstructorAction index = new Index(
								this.datasetSchemaName, null, childTable
										.getTempTableName(), childTable
										.getParentDataSetRelation()
										.getManyKey().getColumns());
						actionGraph.addActionWithParent(index,
								lastPartitionAction);
						lastPartitionAction = index;
					}
				}

				// For each parent table to be partitioned...
				for (Iterator j = parentTables.iterator(); j.hasNext();) {
					MCDSTable parentTable = (MCDSTable) j.next();

					// Index the column to be partitioned.
					MartConstructorAction index = new Index(
							this.datasetSchemaName, null, parentTable
									.getTempTableName(), parentTable
									.getDataSetTable().getPrimaryKey()
									.getColumns());
					actionGraph.addActionWithParent(index, lastPartitionAction);
					lastPartitionAction = index;

					// Partition the parent table.
					// Do the partitioning, one table per value.
					for (Iterator k = partValues.iterator(); k.hasNext();) {
						Object partValue = k.next();
						// Create an MCDSTable for the partitioned
						// value table. Add it to the list of all tables.
						MCDSTable partitionedTable = new MCDSTable(parentTable
								.getDataSetTable(), parentTable
								.getParentDataSetRelation());
						partitionedTable.getPartitionValues().addAll(
								parentTable.getPartitionValues());
						partitionedTable.getPartitionValues().add(partValue);
						tables.add(partitionedTable);
						// Partition the parent table for this value.
						// Make action depend on lastPartitionAction.
						MartConstructorAction partition = new Partition(
								this.datasetSchemaName, null, partitionedTable
								.getTempTableName(), null, parentTable
										.getTempTableName(), partCol.getName(),
								partValue);
						actionGraph.addActionWithParent(partition,
								lastPartitionAction);
						lastPartitionAction = partition;
						// Make the last action of each partitioned
						// table the partition action.
						partitionedTable.setLastActionPerformed(partition);

						// Create an index over the primary key columns of
						// the newly partitioned table.
						index = new Index(this.datasetSchemaName, null,
								partitionedTable.getTempTableName(),
								parentTable.getDataSetTable().getPrimaryKey()
										.getColumns());
						actionGraph.addActionWithParent(index,
								lastPartitionAction);
						lastPartitionAction = index;

						// For each child table, create a new MCDSTable
						// and generate it by doing a Reduce action. Add
						// the new MCDSTable to the list of all tables.
						// Each reduce action depends on and updates the
						// lastPartitionAction.
						for (Iterator l = tablesToReduce.iterator(); l
								.hasNext();) {
							MCDSTable childTable = (MCDSTable) l.next();
							// Create an MCDSTable for the partitioned
							// child table. Add it to the list of all tables.
							MCDSTable reducedTable = new MCDSTable(childTable
									.getDataSetTable(), childTable
									.getParentDataSetRelation());
							reducedTable.getPartitionValues().addAll(
									childTable.getPartitionValues());
							reducedTable.getPartitionValues().add(partValue);
							tables.add(reducedTable);
							// Generate the table.
							MartConstructorAction reduce = new Reduce(
									this.datasetSchemaName, null, reducedTable
											.getTempTableName(), null,
									parentTable.getTempTableName(), parentTable
											.getDataSetTable().getPrimaryKey()
											.getColumns(), null, childTable
											.getTempTableName(), childTable
											.getParentDataSetRelation()
											.getManyKey().getColumns());
							actionGraph.addActionWithParent(reduce,
									lastPartitionAction);
							lastPartitionAction = reduce;
						}
					}
					// Drop previous table. Action depends on all
					// partition actions.
					MartConstructorAction drop = new Drop(
							this.datasetSchemaName, null, parentTable
									.getTempTableName());
					actionGraph.addActionWithParent(drop, lastPartitionAction);
				}
				tables.removeAll(parentTables);

				// Drop original child tables. Reduced ones will have
				// taken their place now.
				for (Iterator j = tablesToReduce.iterator(); j.hasNext();) {
					MCDSTable table = (MCDSTable) j.next();
					MartConstructorAction drop = new Drop(
							this.datasetSchemaName, null, table
									.getTempTableName());
					actionGraph.addActionWithParent(drop, lastPartitionAction);
				}
				tables.removeAll(tablesToReduce);
			}

			// Set up a break-point last action that waits again for all
			// tables to complete.
			MartConstructorAction prePKAction = new PlaceHolder(
					this.datasetSchemaName);
			for (Iterator i = tables.iterator(); i.hasNext();)
				prePKAction.addParent(((MCDSTable) i.next())
						.getLastActionPerformed());
			actionGraph.addAction(prePKAction);

			// Establish PKs and FKs.
			List pkFkActions = new ArrayList();
			for (Iterator i = tables.iterator(); i.hasNext();) {
				MCDSTable parentTable = (MCDSTable) i.next();
				List pkKeyCols = parentTable.getDataSetTable().getPrimaryKey()
						.getColumns();
				// Create a PK over pkKeyCols. Action depends on
				// prePKAction.
				MartConstructorAction pk = new PK(this.datasetSchemaName, null,
						parentTable.getTempTableName(), pkKeyCols);
				actionGraph.addActionWithParent(pk, prePKAction);
				pkFkActions.add(pk);

				// Iterate over foreign keys from this PK.
				Key parentTablePK = parentTable.getDataSetTable()
						.getPrimaryKey();
				for (Iterator j = parentTablePK.getRelations().iterator(); j
						.hasNext();) {
					Relation pkRel = (Relation) j.next();
					DataSetTable fkTable = (DataSetTable) pkRel.getOtherKey(
							parentTablePK).getTable();
					// For each one, find all MCDSTables involved in
					// that relation, and index and establish FK.
					for (Iterator k = tables.iterator(); k.hasNext();) {
						MCDSTable childTable = (MCDSTable) k.next();
						if (!childTable.getDataSetTable().equals(fkTable))
							continue;
						List parentPartitionValues = parentTable
								.getPartitionValues();
						if (childTable.getPartitionValues().size() < parentPartitionValues
								.size())
							continue;
						else if (!childTable.getPartitionValues().subList(0,
								parentPartitionValues.size()).equals(
								parentPartitionValues))
							continue;
						Key foreignKey = (Key) childTable.getDataSetTable()
								.getForeignKeys().iterator().next();
						List fkKeyCols = foreignKey.getColumns();

						// Index fkKeyCols. Depends on pk action.
						MartConstructorAction index = new Index(
								this.datasetSchemaName, null, childTable
										.getTempTableName(), fkKeyCols);
						actionGraph.addActionWithParent(index, pk);
						// Create a FK over fkKeyCols. Action depends on
						// index action.
						MartConstructorAction fk = new FK(
								this.datasetSchemaName, null, childTable
										.getTempTableName(), fkKeyCols, null,
								parentTable.getTempTableName(), pkKeyCols);
						actionGraph.addActionWithParent(fk, index);
						// Add action to list of pkFkActions.
						pkFkActions.add(fk);
					}

				}
			}

			// Set up a break-point last action that waits for all
			// foreign keys to complete.
			MartConstructorAction prePCOAction = new PlaceHolder(
					this.datasetSchemaName);
			for (Iterator i = pkFkActions.iterator(); i.hasNext();)
				prePCOAction.addParent((MartConstructorAction) i.next());
			actionGraph.addAction(prePCOAction);

			// Post-construction optimisation, dependent on prePCOAction.
			List optActions = new ArrayList();
			DataSetOptimiserType optType = ds.getDataSetOptimiserType();
			if (!optType.equals(DataSetOptimiserType.NONE)) {
				List hasTables = new ArrayList();
				// We only need optimise all main and subclass tables.
				for (Iterator i = tables.iterator(); i.hasNext();) {
					MCDSTable parentTable = (MCDSTable) i.next();
					if (parentTable.getDataSetTable().getType().equals(
							DataSetTableType.DIMENSION))
						continue;
					// Make the dimension optimisations dependent on the
					// prePCOAction.
					MartConstructorAction preDOAction = prePCOAction;
					// If we are using column join, add columns to the
					// main or subclass table directly.
					MCDSTable hasTable = parentTable;
					// But if we are using table join, create a 'has' table
					// and add columns to that instead.
					if (optType.equals(DataSetOptimiserType.TABLE)) {
						hasTable = new MCDSHasTable(parentTable
								.getDataSetTable(), parentTable
								.getParentDataSetRelation());
						hasTables.add(hasTable);
						hasTable.getPartitionValues().addAll(
								parentTable.getPartitionValues());
						// Work out what columns to include from the
						// main/subclass table.
						List pkKeyCols = parentTable.getDataSetTable()
								.getPrimaryKey().getColumns();
						// Create the new 'has' table with the columns
						// from the main/subclass table.
						// Depends on preDOAction action.
						MartConstructorAction create = new Create(
								this.datasetSchemaName, null, hasTable
										.getTempTableName(), null, parentTable
										.getTempTableName(), pkKeyCols, false);
						actionGraph.addActionWithParent(create, preDOAction);
						// Create the PK on the 'has' table using the same
						// columns. Depends on create action.
						MartConstructorAction pk = new PK(
								this.datasetSchemaName, null, hasTable
										.getTempTableName(), pkKeyCols);
						actionGraph.addActionWithParent(pk, create);
						// Create the FK on the same columns relating back
						// to the main/subclass table. Depends on pk action.
						// Updates preDOAction.
						MartConstructorAction fk = new FK(
								this.datasetSchemaName, null, hasTable
										.getTempTableName(), pkKeyCols, null,
								parentTable.getTempTableName(), pkKeyCols);
						actionGraph.addActionWithParent(fk, pk);
						preDOAction = fk;
					}
					// For each main or subclass table, identify all dimensions.
					for (Iterator j = tables.iterator(); j.hasNext();) {
						MCDSTable childTable = (MCDSTable) j.next();
						if (!childTable.getDataSetTable().getType().equals(
								DataSetTableType.DIMENSION))
							continue;
						else if (!childTable.getParentDataSetRelation()
								.getOneKey().getTable().equals(parentTable))
							continue;
						List parentPartitionValues = parentTable
								.getPartitionValues();
						if (childTable.getPartitionValues().size() < parentPartitionValues
								.size())
							continue;
						else if (!childTable.getPartitionValues().subList(0,
								parentPartitionValues.size()).equals(
								parentPartitionValues))
							continue;
						// Work out has column name.
						StringBuffer sb = new StringBuffer();
						sb.append(childTable.createFinalName());
						sb.append(BuilderBundle.getString("tablenameSep"));
						sb.append(BuilderBundle.getString("hasSuffix"));
						String hasColumnName = sb.toString();
						// Add columns to the main or subclass table or
						// 'has' table.
						// Dependent on preDOAction.
						MartConstructorAction optimiseAdd = new OptimiseAddColumn(
								this.datasetSchemaName, null, childTable
										.getTempTableName(), hasColumnName);
						actionGraph.addActionWithParent(optimiseAdd,
								preDOAction);
						// Update those columns using inner join
						// on has table PK to dimension table FK.
						// Dependent on optimise action.
						// Updates preDOAction.
						MartConstructorAction optimiseUpd = new OptimiseUpdateColumn(
								this.datasetSchemaName, null, childTable
										.getTempTableName(), childTable
										.getParentDataSetRelation()
										.getManyKey().getColumns(), null,
								parentTable.getTempTableName(), parentTable
										.getDataSetTable().getPrimaryKey()
										.getColumns(), hasColumnName);
						actionGraph.addActionWithParent(optimiseUpd,
								optimiseAdd);
						preDOAction = optimiseUpd;
					}
					// Add final preDOAction to list of actions to wait for
					// before doing renames later.
					optActions.add(preDOAction);
				}
				tables.addAll(hasTables);
			}

			// Set up a break-point last action that waits for all
			// optimisations to complete.
			MartConstructorAction preRenameAction = new PlaceHolder(
					this.datasetSchemaName);
			if (!optActions.isEmpty())
				for (Iterator i = optActions.iterator(); i.hasNext();)
					preRenameAction.addParent((MartConstructorAction) i.next());
			else
				preRenameAction.addParent(prePCOAction);
			actionGraph.addAction(preRenameAction);

			// Rename all tables.
			for (Iterator i = tables.iterator(); i.hasNext();) {
				MCDSTable table = (MCDSTable) i.next();
				String realName = table.createFinalName();
				String tempTableName = table.getTempTableName();
				// Rename the table, dependent on preRenameAction.
				MartConstructorAction rename = new Rename(
						this.datasetSchemaName, null, tempTableName, realName);
				actionGraph.addActionWithParent(rename, preRenameAction);
			}

			// Now we have constructed our action graph, issue events to
			// get the work done!

			// Work out the progress step size.
			double stepPercent = 100.0 / (double) actionGraph.getActions()
					.size();

			// Divide the progress step size by the number of datasets.
			stepPercent /= (double) totalDataSetCount;

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
					for (Iterator i = actions.iterator(); i.hasNext();) {
						MartConstructorAction action = (MartConstructorAction) i
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
			} catch (Exception e) {
				// Pass any exceptions on up.
				throw e;
			} finally {
				// Make sure the helper always gets a chance to tidy up,
				// even if an exception is thrown.
				this.issueListenerEvent(MartConstructorListener.DATASET_ENDED);
			}
		}

		private void processTable(MartConstructorAction firstActionDependsOn,
				MartConstructorActionGraph actionGraph, Schema mainTableSchema,
				Schema schema, MCDSTable table) throws Exception {
			// A placeholder for the last action performed on this table.
			MartConstructorAction lastActionPerformed = firstActionDependsOn;

			// Placeholder for name of the target temp table that will
			// contain the constructed table.
			String tempTableName = table.getTempTableName();

			// We now need to work out at which point to start the process
			// of transforming the table.
			Table firstTable = (table.getDataSetTable()
					.getUnderlyingRelations().isEmpty()) ? table.getRealTable()
					: ((Key) table.getDataSetTable().getUnderlyingKeys().get(0))
							.getTable();

			// Work out what columns to include from the first table.
			List firstDSCols = table.getDataSetColumns(firstTable.getColumns());

			// Include the schema name column if there is one (don't need
			// to do this for dimension/subclass tables as it will be part
			// of the primary key inherited from the parent table).
			for (Iterator i = table.getDataSetTable().getColumns().iterator(); i
					.hasNext();) {
				DataSetColumn dsCol = (DataSetColumn) i.next();
				if (dsCol instanceof SchemaNameColumn)
					firstDSCols.add(dsCol);
			}

			// Create the table based on firstTable, and selecting
			// based on firstDSCols. Dependent on lastActionPerformed, and
			// updates it.
			MartConstructorAction create = new Create(this.datasetSchemaName,
					null, tempTableName, schema, firstTable.getName(),
					firstDSCols, true);
			actionGraph.addActionWithParent(create, lastActionPerformed);
			lastActionPerformed = create;

			// Merge subsequent tables based on underlying relations.
			for (int i = 0; i < table.getDataSetTable().getUnderlyingKeys()
					.size(); i++) {
				// What key was followed from the previous table?
				Key sourceKey = (Key) table.getDataSetTable()
						.getUnderlyingKeys().get(i);
				// What relation was followed?
				Relation relation = (Relation) table.getDataSetTable()
						.getUnderlyingRelations().get(i);
				// Left or inner join?
				boolean useLeftJoin = relation.isOptional();
				// What key did we reach by following the relation?
				Key targetKey = relation.getOtherKey(sourceKey);
				// What table did we reach, and therefore need to merge now?
				Table targetTable = targetKey.getTable();
				String targetTableName = targetTable.getName();
				// Is the target table going to need a union merge?
				boolean unionMerge = (targetTable.getSchema() instanceof SchemaGroup)
						&& !targetTable.getSchema().equals(mainTableSchema);
				// What are the equivalent columns on the existing temp table
				// that correspond to the key on the previous table?
				List sourceDSKey = table.getDataSetColumns(sourceKey
						.getColumns());
				// If no columns are selected in the source key, we don't
				// need to merge it, so we can skip it!
				if (sourceDSKey.isEmpty())
					continue;
				// What are the columns we should merge from this new table?
				List targetDSCols = table.getDataSetColumns(relation);
				// If no columns are selected in the target table, we don't
				// need to merge it, so we can skip it!
				if (targetDSCols.isEmpty())
					continue;
				// If targetTable is in a group schema that is not the
				// same group schema we started with, create a union table
				// containing all the targetTable copies, then merge with
				// that instead.
				Schema targetSchema;
				if (unionMerge) {
					// Build a list of schemas to union tables from.
					List unionTableSchemas = new ArrayList();
					List unionTableNames = new ArrayList();
					if (targetTable.getSchema() instanceof SchemaGroup) {
						for (Iterator j = ((SchemaGroup) targetTable
								.getSchema()).getSchemas().iterator(); j
								.hasNext();) {
							Schema mergeSchema = (Schema) j.next();
							unionTableSchemas.add(mergeSchema);
							unionTableNames.add(targetTable.getName());
						}
					} else {
						unionTableSchemas.add(targetTable.getSchema());
						unionTableNames.add(targetTable.getName());
					}
					// Make name for union table, which is now the target table.
					targetTableName = this.helper.getNewTempTableName();
					// Create union table. Each table comes
					// from one of the schemas in the group on the RHS.
					// If the RHS is not a group, just use the single RHS
					// schema instead. Dependent on lastActionPerformed, and
					// updates it.
					MartConstructorAction union = new Union(
							this.datasetSchemaName, null, targetTableName,
							unionTableSchemas, unionTableNames);
					actionGraph.addActionWithParent(union, lastActionPerformed);
					// Create index on targetKey on union table. Dependent
					// on lastActionPerformed, and updates it.
					MartConstructorAction index = new Index(
							this.datasetSchemaName, null, targetTableName,
							targetKey.getColumns());
					actionGraph.addActionWithParent(index, union);
					lastActionPerformed = index;
					// Target schema is now the dataset schema. Null
					// represents this.
					targetSchema = null;
				} else {
					// Target schema is same as source schema.
					targetSchema = schema;
				}
				// Index sourceDSKey columns. Dependent on
				// lastActionPerformed, and updates it.
				MartConstructorAction index = new Index(this.datasetSchemaName,
						null, tempTableName, sourceDSKey);
				actionGraph.addActionWithParent(index, lastActionPerformed);
				lastActionPerformed = index;
				// Generate new temp table name for merged table.
				String mergedTableName = this.helper.getNewTempTableName();
				// Merge tables based on sourceDSKey -> targetKey,
				// and selecting based on targetDSCols. Dependent on
				// lastActionPerformed, and updates it.
				MartConstructorAction merge = new Merge(this.datasetSchemaName,
						null, mergedTableName, null, tempTableName,
						sourceDSKey, useLeftJoin, targetSchema,
						targetTableName, targetKey.getColumns(), targetDSCols,
						true);
				actionGraph.addActionWithParent(merge, lastActionPerformed);
				lastActionPerformed = merge;
				// Drop old temp table.
				MartConstructorAction drop = new Drop(this.datasetSchemaName,
						null, tempTableName);
				actionGraph.addActionWithParent(drop, lastActionPerformed);
				// Update temp table name to point to newly merged table.
				tempTableName = mergedTableName;
				// If targetTable was a union table, drop it.
				if (unionMerge) {
					// Drop union target table. Dependent on
					// lastActionPerformed, but does not update it.
					drop = new Drop(this.datasetSchemaName, null,
							targetTableName);
					actionGraph.addActionWithParent(drop, lastActionPerformed);
				}
			}

			// Add in all the concat-relation columns.
			for (Iterator i = table.getDataSetTable().getColumns().iterator(); i
					.hasNext();) {
				DataSetColumn datasetColumn = (DataSetColumn) i.next();
				if (!(datasetColumn instanceof ConcatRelationColumn))
					continue;
				ConcatRelationColumn concatColumn = (ConcatRelationColumn) datasetColumn;
				// Where does the concat column point?
				Relation concatRelation = concatColumn.getUnderlyingRelation();
				// What key did it lead from?
				Key sourceKey = concatRelation.getOneKey();
				// What key does it go to?
				Key targetKey = concatRelation.getManyKey();
				// What is the table to be concatted?
				Table targetTable = targetKey.getTable();
				String targetTableName = targetTable.getName();
				// Is the target table going to need a union merge?
				boolean unionMerge = !targetTable.getSchema().equals(
						mainTableSchema);
				// What columns on the table should be concatted?
				Key targetConcatKey = targetTable.getPrimaryKey();
				// What are the equivalent columns on the existing temp table
				// that correspond to the source key?
				List sourceDSKey = table.getNthDataSetColumns(sourceKey
						.getColumns(), concatRelation);
				// If concatColumn is in a table in a group schema
				// that is not the same group schema we started with, create
				// a union table containing all the concatTable copies, then
				// concat that instead.
				Schema targetSchema;
				if (unionMerge) {
					// Build a list of schemas to union tables from.
					List unionTableSchemas = new ArrayList();
					List unionTableNames = new ArrayList();
					if (targetTable.getSchema() instanceof SchemaGroup) {
						for (Iterator j = ((SchemaGroup) targetTable
								.getSchema()).getSchemas().iterator(); j
								.hasNext();) {
							Schema mergeSchema = (Schema) j.next();
							unionTableSchemas.add(mergeSchema);
							unionTableNames.add(targetTable.getName());
						}
					} else {
						unionTableSchemas.add(targetTable.getSchema());
						unionTableNames.add(targetTable.getName());
					}
					// Make name for union table, which is now the target table.
					targetTableName = this.helper.getNewTempTableName();
					// Create union table. Each table comes
					// from one of the schemas in the group on the RHS.
					// If the RHS is not a group, just use the single RHS
					// schema instead. Dependent on lastActionPerformed, and
					// updates it.
					MartConstructorAction union = new Union(
							this.datasetSchemaName, null, targetTableName,
							unionTableSchemas, unionTableNames);
					actionGraph.addActionWithParent(union, lastActionPerformed);
					// Create index on targetKey on union table. Dependent
					// on lastActionPerformed, and updates it.
					MartConstructorAction index = new Index(
							this.datasetSchemaName, null, targetTableName,
							targetKey.getColumns());
					actionGraph.addActionWithParent(index, union);
					lastActionPerformed = index;
					// Target schema is now the dataset schema. Null
					// represents this.
					targetSchema = null;
				} else {
					// Target schema name is same as source schema.
					targetSchema = schema;
				}
				// Create the concat table by selecting sourceKey.
				String concatTableName = this.helper.getNewTempTableName();
				MartConstructorAction createCon = new Create(
						this.datasetSchemaName, null, concatTableName, null,
						tempTableName, sourceDSKey, false);
				actionGraph.addActionWithParent(createCon, lastActionPerformed);
				lastActionPerformed = createCon;
				// Generate new temp table name for merged concat table.
				String mergedConcatTableName = this.helper
						.getNewTempTableName();
				// Populate the concat table by selecting targetKey and
				// concatting targetConcatKey into a column with the same
				// name as concatColumn. Dependent on lastActionPerformed, and
				// updates it.
				MartConstructorAction concat = new Concat(
						this.datasetSchemaName, null, mergedConcatTableName,
						null, tempTableName, sourceDSKey, targetSchema,
						targetTableName, targetKey.getColumns(),
						targetConcatKey.getColumns(), concatColumn.getName(),
						table.getDataSet()
								.getConcatRelationType(concatRelation));
				actionGraph.addActionWithParent(concat, lastActionPerformed);
				lastActionPerformed = concat;
				// Drop old concat temp table and replace with new one.
				MartConstructorAction drop = new Drop(this.datasetSchemaName,
						null, concatTableName);
				actionGraph.addActionWithParent(drop, lastActionPerformed);
				concatTableName = mergedConcatTableName;
				// Index sourceDSKey columns. Dependent on lastActionPerformed,
				// and updates it.
				MartConstructorAction indexCon = new Index(
						this.datasetSchemaName, null, concatTableName,
						sourceDSKey);
				actionGraph.addActionWithParent(indexCon, lastActionPerformed);
				lastActionPerformed = indexCon;
				// Generate new temp table name for merged table.
				String mergedTableName = this.helper.getNewTempTableName();
				// Merge with the concat table based on sourceDSKey ->
				// sourceDSKey and selecting the column with the same name as
				// concatColumn.
				// Dependent on lastActionPerformed, and updates it.
				MartConstructorAction merge = new Merge(this.datasetSchemaName,
						null, mergedTableName, null, tempTableName,
						sourceDSKey, true, null, concatTableName, sourceDSKey,
						Collections.singletonList(concatColumn), false);
				actionGraph.addActionWithParent(merge, lastActionPerformed);
				lastActionPerformed = merge;
				// Drop old temp table and replace with new one.
				drop = new Drop(this.datasetSchemaName, null, tempTableName);
				actionGraph.addActionWithParent(drop, lastActionPerformed);
				tempTableName = mergedTableName;
				// Drop concat table. Dependent on lastActionPerformed, but
				// does not update it.
				MartConstructorAction dropCon = new Drop(
						this.datasetSchemaName, null, concatTableName);
				actionGraph.addActionWithParent(dropCon, lastActionPerformed);
				// If concat table was a union table, drop it.
				if (unionMerge) {
					// Drop union concat table. Dependent on
					// lastActionPerformed, but does not update it.
					drop = new Drop(this.datasetSchemaName, null,
							targetTableName);
					actionGraph.addActionWithParent(drop, lastActionPerformed);
				}
			}

			// Update the temp table name.
			table.setTempTableName(tempTableName);
			
			// Return the last action performed to create this table.
			table.setLastActionPerformed(lastActionPerformed);
		}

		public String getStatusMessage() {
			return this.statusMessage;
		}

		public int getPercentComplete() {
			return (int) this.percentComplete;
		}

		public Exception getFailureException() {
			return failure;
		}

		public void cancel() {
			this.cancelled = true;
		}

		private void checkCancelled() throws ConstructorException {
			if (this.cancelled)
				throw new ConstructorException(BuilderBundle
						.getString("mcCancelled"));
		}

		// Internal use only, serves as a convenient wrapper for tracking
		// info about a particular dataset table.
		private class MCDSTable {
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
			public MCDSTable(DataSetTable datasetTable,
					Relation parentDataSetRelation) {
				this.tempTableName = helper.getNewTempTableName();
				this.datasetTable = datasetTable;
				this.parentDataSetRelation = parentDataSetRelation;
				this.partitionValues = new ArrayList();
				this.counter = new HashMap();
			}

			public String getTempTableName() {
				return this.tempTableName;
			}

			public void setTempTableName(String tempTableName) {
				this.tempTableName = tempTableName;
			}

			public List getPartitionValues() {
				return this.partitionValues;
			}

			public DataSet getDataSet() {
				return (DataSet) this.datasetTable.getSchema();
			}

			public DataSetTable getDataSetTable() {
				return this.datasetTable;
			}

			public Table getRealTable() {
				return this.datasetTable.getUnderlyingTable();
			}

			public Schema getRealSchema() {
				return this.datasetTable.getUnderlyingTable().getSchema();
			}

			public Relation getParentDataSetRelation() {
				return this.parentDataSetRelation;
			}

			public DataSetTable getParentDataSetTable() {
				return (DataSetTable) this.parentDataSetRelation.getOneKey()
						.getTable();
			}

			private DataSetColumn getDataSetColumn(int n, Column interestingColumn) {
				// We must look not only for this column, but all columns
				// involved in relations with it, because it could have been
				// introduced via another route.
				List candidates = new ArrayList();
				candidates.add(interestingColumn);
				for (int j = 0; j < candidates.size(); j++) {
					Column searchColumn = (Column) candidates.get(j);
					for (Iterator i = this.datasetTable.getColumns().iterator(); i
							.hasNext();) {
						DataSetColumn candidate = (DataSetColumn) i.next();
						if (this.getDataSet().getMaskedDataSetColumns()
								.contains(candidate))
							continue;
						if (!(candidate instanceof WrappedColumn))
							continue;
						WrappedColumn wc = (WrappedColumn) candidate;
						if (searchColumn.equals(wc.getWrappedColumn()) && n--==0)
							return candidate;
					}
					// If got here, didn't find this search candidate, so add
					// all the related columns in other keys.
					for (Iterator i = searchColumn.getTable().getKeys()
							.iterator(); i.hasNext();) {
						Key key = (Key) i.next();
						int colIndex = key.getColumns().indexOf(searchColumn);
						if (colIndex < 0)
							continue;
						for (Iterator k = key.getRelations().iterator(); k
								.hasNext();) {
							Relation rel = (Relation) k.next();
							if (!this.getDataSetTable()
									.getUnderlyingRelations().contains(rel))
								continue;
							Key otherKey = rel.getOtherKey(key);
							Column nextCandidate = (Column) otherKey
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

			public List getDataSetColumns(Collection interestingColumns) {
				List list = new ArrayList();
				for (Iterator i = interestingColumns.iterator(); i.hasNext();) {
					DataSetColumn col = this
							.getDataSetColumn(0, (Column) i.next());
					if (col != null)
						list.add(col);
				}
				return list;
			}

			public List getDataSetColumns(int n, Collection interestingColumns) {
				List list = new ArrayList();
				for (Iterator i = interestingColumns.iterator(); i.hasNext();) {
					DataSetColumn col = this
							.getDataSetColumn(n, (Column) i.next());
					if (col != null)
						list.add(col);
				}
				return list;
			}

			public List getDataSetColumns(Relation underlyingRelation) {
				List list = new ArrayList();
				for (Iterator i = this.datasetTable.getColumns().iterator(); i
						.hasNext();) {
					DataSetColumn candidate = (DataSetColumn) i.next();
					if (this.getDataSet().getMaskedDataSetColumns().contains(
							candidate))
						continue;
					if (!(candidate instanceof WrappedColumn))
						continue;
					WrappedColumn wc = (WrappedColumn) candidate;
					Relation wcRel = wc.getUnderlyingRelation();
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
			
			public List getNthDataSetColumns(Collection interestingColumns, Object counter) {
				int n = this.incrementCount(counter);
				return this.getDataSetColumns(n, interestingColumns);
			}
			
			private int incrementCount(Object counter) {
				int count;
				if (!this.counter.containsKey(counter)) {
					count = 0;
					this.counter.put(counter, new Integer(count));
				} else {
					count = ((Integer)this.counter.get(counter)).intValue();
					count++;
					this.counter.put(counter, new Integer(count));
				}
				return count;					
			}

			public void setLastActionPerformed(
					MartConstructorAction lastActionPerformed) {
				this.lastActionPerformed = lastActionPerformed;
			}

			public MartConstructorAction getLastActionPerformed() {
				return this.lastActionPerformed;
			}

			public String createFinalName() {
				// TODO - come up with a better naming scheme
				// Currently the name is:
				// datasetname__\
				// {partitionvalue{_partitionvalue}*}*__\
				// tablename__\
				// type

				// Work out what dataset we are in.
				DataSet dataset = this.getDataSet();
				DataSetTable datasetTable = this.getDataSetTable();

				StringBuffer name = new StringBuffer();

				// Dataset name and __ separator.
				name.append(dataset.getName());
				name.append(BuilderBundle.getString("tablenameSep"));

				// Partition values and __ separator with _ separator between.
				List partitionValues = this.getPartitionValues();
				for (Iterator j = partitionValues.iterator(); j.hasNext();) {
					// Partition values may be null, so cannot use toString().
					String partitionValue = "" + j.next();
					name.append(partitionValue);
					if (j.hasNext())
						name.append(BuilderBundle.getString("tablenameSubSep"));
				}
				name.append(BuilderBundle.getString("tablenameSep"));

				// Table name and __ separator.
				name.append(datasetTable.getName());
				name.append(BuilderBundle.getString("tablenameSep"));

				// Type.
				DataSetTableType type = datasetTable.getType();
				if (type.equals(DataSetTableType.MAIN))
					name.append(BuilderBundle.getString("mainSuffix"));
				else if (type.equals(DataSetTableType.MAIN_SUBCLASS))
					name.append(BuilderBundle.getString("subclassSuffix"));
				else if (type.equals(DataSetTableType.DIMENSION))
					name.append(BuilderBundle.getString("dimensionSuffix"));

				return name.toString();
			}
		}

		// Internal use only, serves as a convenient wrapper for tracking
		// info about a particular 'has' table.
		private class MCDSHasTable extends MCDSTable {
			/**
			 * Constructor starts tracking a 'has' table that contains
			 * optimisations for the given table.
			 * 
			 * @param datasetTable
			 *            the table to contain optimisations for.
			 * @param parentDataSetRelation
			 *            the relation between the dataset table and any parent
			 *            table it belongs to, e.g. a main or a subclass.
			 */
			public MCDSHasTable(DataSetTable datasetTable,
					Relation parentDataSetRelation) {
				super(datasetTable, parentDataSetRelation);
			}

			public String createFinalName() {
				StringBuffer sb = new StringBuffer();
				sb.append(super.createFinalName());
				sb.append(BuilderBundle.getString("tablenameSep"));
				sb.append(BuilderBundle.getString("hasSuffix"));
				return sb.toString();
			}
		}
	}
}
