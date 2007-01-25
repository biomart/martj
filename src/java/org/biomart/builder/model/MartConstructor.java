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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.MartConstructorAction.CreateOptimiser;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.DropColumns;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.LeftJoin;
import org.biomart.builder.model.MartConstructorAction.Rename;
import org.biomart.builder.model.MartConstructorAction.Select;
import org.biomart.builder.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.builder.model.TransformationUnit.LeftJoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.common.controller.JDBCSchema;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
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
			// Create a list in the order by which we want to process tables.
			final List tablesToProcess = new ArrayList();
			// Main table first.
			for (final Iterator i = dataset.getTables().iterator(); i.hasNext()
					&& tablesToProcess.isEmpty();) {
				final DataSetTable table = (DataSetTable) i.next();
				if (table.getType().equals(DataSetTableType.MAIN))
					tablesToProcess.add(table);
			}
			// Now recursively expand the table list.
			for (int i = 0; i < tablesToProcess.size(); i++)
				for (final Iterator j = ((DataSetTable) tablesToProcess.get(i))
						.getRelations().iterator(); j.hasNext();) {
					final Relation r = (Relation) j.next();
					final DataSetTable dsTab = (DataSetTable) r.getManyKey()
							.getTable();
					if (!tablesToProcess.contains(dsTab)
							&& !dataset.getDataSetModifications()
									.isMaskedTable(dsTab))
						tablesToProcess.add(dsTab);
				}

			// Check not cancelled.
			this.checkCancelled();

			// Work out the progress step size : 1 step = 1 table.
			double stepPercent = 100.0 / dataset.getTables().size();

			// Divide the progress step size by the number of datasets.
			stepPercent /= totalDataSetCount;

			// Process the tables.
			for (final Iterator i = tablesToProcess.iterator(); i.hasNext();) {
				final DataSetTable dsTable = (DataSetTable) i.next();
				final String finalName = this.getFinalName(dsTable);
				final String tempName = "TEMP";
				int tempNameCount = 0;
				String previousTempTable = null;

				// Use the transformation units to create the basic table.
				// TODO Cope with partitions.
				// TODO Cope with pseudo-partitioned schema.
				// TODO Cope with table and relation restrictions.
				// TODO Cope with non-inherited cols - check alternatives
				// have been specified.
				for (final Iterator j = dsTable.getTransformationUnits()
						.iterator(); j.hasNext();) {
					final TransformationUnit tu = (TransformationUnit) j.next();
					final String tempTable = tempName + tempNameCount++;

					// Translate TU to Action.
					// Left-join?
					if (tu instanceof LeftJoinTable) {
						final LeftJoinTable ljtu = (LeftJoinTable) tu;
						final String rightSchema = ((JDBCSchema) ljtu
								.getTable().getSchema()).getDatabaseSchema();
						final String rightTable = ljtu.getTable().getName();
						final List leftJoinCols = new ArrayList();
						final List rightJoinCols = ljtu.getSchemaRelation()
								.getOtherKey(ljtu.getSchemaSourceKey())
								.getColumnNames();
						final Map selectCols = new HashMap();
						// Populate vars.
						for (final Iterator k = ljtu.getSourceDataSetColumns()
								.iterator(); k.hasNext();)
							leftJoinCols.add(((DataSetColumn) k.next())
									.getModifiedName());
						for (final Iterator k = ljtu.getNewColumnNameMap()
								.entrySet().iterator(); k.hasNext();) {
							final Map.Entry entry = (Map.Entry) k.next();
							final DataSetColumn col = (DataSetColumn) entry
									.getValue();
							if (col.getDependency()
									|| !dataset.getDataSetModifications()
											.isMaskedColumn(col))
								selectCols.put(entry.getKey(), col
										.getModifiedName());
						}
						// Index the left-hand side of the join.
						final Index index = new Index(this.datasetSchemaName,
								finalName);
						index.setTable(previousTempTable);
						index.setColumns(leftJoinCols);
						this.issueAction(index);
						// Make the join.
						final LeftJoin action = new LeftJoin(
								this.datasetSchemaName, finalName);
						action.setLeftTable(previousTempTable);
						action.setRightSchema(rightSchema);
						action.setRightTable(rightTable);
						action.setLeftJoinColumns(leftJoinCols);
						action.setRightJoinColumns(rightJoinCols);
						action.setSelectColumns(selectCols);
						action.setResultTable(tempTable);
						this.issueAction(action);
					}
					// Select-from?
					else if (tu instanceof SelectFromTable) {
						final SelectFromTable stu = (SelectFromTable) tu;
						final Table sourceTable = stu.getTable();
						final String schema = sourceTable instanceof DataSetTable ? this.datasetSchemaName
								: ((JDBCSchema) sourceTable.getSchema())
										.getDatabaseSchema();
						final String table = sourceTable instanceof DataSetTable ? this
								.getFinalName((DataSetTable) sourceTable)
								: stu.getTable().getName();
						final Map selectCols = new HashMap();
						// Populate vars.
						for (final Iterator k = stu.getNewColumnNameMap()
								.entrySet().iterator(); k.hasNext();) {
							final Map.Entry entry = (Map.Entry) k.next();
							final DataSetColumn col = (DataSetColumn) entry
									.getValue();
							if (col.getDependency()
									|| !dataset.getDataSetModifications()
											.isMaskedColumn(col))
								selectCols
										.put(
												sourceTable instanceof DataSetTable ? ((DataSetColumn) sourceTable
														.getColumnByName((String) entry
																.getKey()))
														.getModifiedName()
														: entry.getKey(), col
														.getModifiedName());
						}
						// Do the select.
						final Select action = new Select(
								this.datasetSchemaName, finalName);
						action.setSchema(schema);
						action.setTable(table);
						action.setSelectColumns(selectCols);
						action.setResultTable(tempTable);
						this.issueAction(action);
					}
					// TODO : Concat column TUNITs.
					// TODO : Expression column TUNITs.
					// Others?
					else
						throw new BioMartError();
					// Drop the previous table now we're finished with it.
					if (previousTempTable != null) {
						final Drop action = new Drop(this.datasetSchemaName,
								finalName);
						action.setTable(previousTempTable);
						this.issueAction(action);
					}
					// Update the previous table.
					previousTempTable = tempTable;
				}
				// Drop masked dependencies.
				final List dropCols = new ArrayList();
				for (final Iterator x = dsTable.getColumns().iterator(); x
						.hasNext();) {
					final DataSetColumn col = (DataSetColumn) x.next();
					if (!col.getDependency())
						continue;
					if (dataset.getDataSetModifications().isMaskedColumn(col))
						dropCols.add(col.getModifiedName());
				}
				if (!dropCols.isEmpty()) {
					final DropColumns action = new DropColumns(
							this.datasetSchemaName, finalName);
					action.setColumns(dropCols);
					this.issueAction(action);
				}
				// Add a rename action to produce the final table.
				final Rename action = new Rename(this.datasetSchemaName,
						finalName);
				action.setFrom(previousTempTable);
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
							finalName);
					index.setTable(finalName);
					index.setColumns(keyCols);
					this.issueAction(index);
				}
				// Create optimiser columns - either count or bool,
				// or none if not required.
				final DataSetOptimiserType oType = dataset
						.getDataSetOptimiserType();
				if (!oType.equals(DataSetOptimiserType.NONE)) {
					// Tables are same name, but use 'bool' or 'count' instead
					// of 'main'
					final String optTable = this.getOptimiserName(dsTable,
							oType);
					if (!dsTable.getType().equals(DataSetTableType.DIMENSION)) {
						// If main/subclass table type, create table.
						if (oType.equals(DataSetOptimiserType.TABLE)
								|| oType
										.equals(DataSetOptimiserType.TABLE_BOOL)
								|| oType
										.equals(DataSetOptimiserType.TABLE_INHERIT)
								|| oType
										.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT)) {
							// The key cols are those from the parent table.
							final List keyCols = new ArrayList();
							for (final Iterator y = dsTable.getPrimaryKey()
									.getColumns().iterator(); y.hasNext();)
								keyCols.add(((DataSetColumn) y.next())
										.getModifiedName());

							// Create the table by selecting the pk.
							final CreateOptimiser create = new CreateOptimiser(this.datasetSchemaName,
									finalName);
							create.setKeyColumns(keyCols);
							create.setOptTableName(optTable);
							this.issueAction(create);
						}
					} else {
						// Columns are dimension table names with '_bool' or
						// '_count' appended.
						final String optCol = dsTable.getModifiedName()
								+ ((oType
										.equals(DataSetOptimiserType.COLUMN_BOOL)
										|| oType
												.equals(DataSetOptimiserType.COLUMN_BOOL_INHERIT)
										|| oType
												.equals(DataSetOptimiserType.TABLE_BOOL) || oType
										.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT)) ? Resources
										.get("boolColSuffix")
										: Resources.get("countColSuffix"));
						// Work out which parents are getting this column.
						final List parents = new ArrayList();
						parents.add(((Relation) dsTable.getRelations()
								.iterator().next()).getOneKey().getTable());
						if (oType
								.equals(DataSetOptimiserType.COLUMN_BOOL_INHERIT)
								|| oType
										.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT))
							for (int z = 0; z < parents.size(); z++)
								parents
										.add(((Relation) ((Table) parents
												.get(z)).getRelations()
												.iterator().next()).getOneKey()
												.getTable());
						for (final Iterator z = parents.iterator(); z.hasNext();) {
							final DataSetTable parent = (DataSetTable) z.next();
							// For each parent, add a column to it or its table.
							// Key columns are primary key cols from parent.
							// Do a left-join update. We're looking for rows
							// where all _child_
							// non-key cols are non-null.
							final List keyCols = new ArrayList();
							for (final Iterator y = parent.getPrimaryKey()
									.getColumns().iterator(); y.hasNext();)
								keyCols.add(((DataSetColumn) y.next())
										.getModifiedName());
							final List nonNullCols = new ArrayList();
							for (final Iterator y = dsTable.getColumns()
									.iterator(); y.hasNext();) {
								final DataSetColumn col = (DataSetColumn) y
										.next();
								if (!dsTable.getPrimaryKey().getColumns()
										.contains(col))
									nonNullCols.add(col.getModifiedName());
							}

							// Do the bool/count update.
							final UpdateOptimiser update = new UpdateOptimiser(this.datasetSchemaName,
									finalName);
							update.setKeyColumns(keyCols);
							update.setNonNullColumns(nonNullCols);
							update.setOptTableName(optTable);
							update.setOptColumnName(optCol);
							update.setCountNotBool(oType.equals(DataSetOptimiserType.COLUMN)
									|| oType.equals(DataSetOptimiserType.COLUMN_INHERIT)
									|| oType.equals(DataSetOptimiserType.TABLE)
									|| oType.equals(DataSetOptimiserType.TABLE_INHERIT));
							this.issueAction(update);
						}
					}
				}

				// Update the progress percentage once per table.
				this.percentComplete += stepPercent;

				// Check not cancelled.
				this.checkCancelled();
			}
		}

		private void issueAction(final MartConstructorAction action)
				throws Exception {
			// Execute the action.
			this.statusMessage = action.getStatusMessage();
			this.issueListenerEvent(MartConstructorListener.ACTION_EVENT,
					action);
		}

		private String getOptimiserName(final DataSetTable dsTable,
				final DataSetOptimiserType oType) {
			final StringBuffer finalName = new StringBuffer();
			finalName.append(dsTable.getSchema().getName());
			finalName.append(Resources.get("tablenameSep"));
			finalName.append(dsTable.getModifiedName());
			finalName.append(Resources.get("tablenameSep"));
			if (oType.equals(DataSetOptimiserType.TABLE)
					|| oType.equals(DataSetOptimiserType.TABLE_INHERIT))
				finalName.append(Resources.get("countTblSuffix"));
			else if (oType.equals(DataSetOptimiserType.TABLE_BOOL)
					|| oType.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT))
				finalName.append(Resources.get("boolTblSuffix"));
			else if (dsTable.getType().equals(DataSetTableType.MAIN))
				finalName.append(Resources.get("mainSuffix"));
			else if (dsTable.getType().equals(DataSetTableType.MAIN_SUBCLASS))
				finalName.append(Resources.get("subclassSuffix"));
			else if (dsTable.getType().equals(DataSetTableType.DIMENSION))
				finalName.append(Resources.get("dimensionSuffix"));
			else
				throw new BioMartError();
			return finalName.toString().replaceAll("\\W", "");
		}

		private String getFinalName(final DataSetTable dsTable) {
			final StringBuffer finalName = new StringBuffer();
			finalName.append(dsTable.getSchema().getName());
			finalName.append(Resources.get("tablenameSep"));
			finalName.append(dsTable.getModifiedName());
			finalName.append(Resources.get("tablenameSep"));
			if (dsTable.getType().equals(DataSetTableType.MAIN))
				finalName.append(Resources.get("mainSuffix"));
			else if (dsTable.getType().equals(DataSetTableType.MAIN_SUBCLASS))
				finalName.append(Resources.get("subclassSuffix"));
			else if (dsTable.getType().equals(DataSetTableType.DIMENSION))
				finalName.append(Resources.get("dimensionSuffix"));
			else
				throw new BioMartError();
			return finalName.toString().replaceAll("\\W", "");
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
							try {
								this.issueListenerEvent(MartConstructorListener.DATASET_STARTED);
								this.makeActionsForDataset((DataSet) i.next(),
									totalDataSetCount);
							} catch (final Throwable t) {
								throw t;
							} finally {
								// Make sure the helper always gets a chance to tidy up,
								// even if an exception is thrown.
								this.issueListenerEvent(MartConstructorListener.DATASET_ENDED);
							}
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
