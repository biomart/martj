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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumn;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumn.SingleValue;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumn.UniqueValues;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumn.ValueCollection;
import org.biomart.builder.model.MartConstructorAction.CopyOptimiser;
import org.biomart.builder.model.MartConstructorAction.CreateOptimiser;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.DropColumns;
import org.biomart.builder.model.MartConstructorAction.Index;
import org.biomart.builder.model.MartConstructorAction.Join;
import org.biomart.builder.model.MartConstructorAction.LeftJoin;
import org.biomart.builder.model.MartConstructorAction.Rename;
import org.biomart.builder.model.MartConstructorAction.Select;
import org.biomart.builder.model.MartConstructorAction.UpdateOptimiser;
import org.biomart.builder.model.TransformationUnit.JoinTable;
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
		private static final String NO_PARTITION = "__NO__PARTITION__";

		private boolean cancelled = false;;

		private Collection datasets;

		private String datasetSchemaName;

		private Exception failure = null;

		private Helper helper;

		private Collection martConstructorListeners;

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
			// Check not cancelled.
			this.checkCancelled();

			// Work out the progress step size : 1 step = 1 table.
			double stepPercent = 100.0 / dataset.getTables().size();

			// Divide the progress step size by the number of datasets.
			stepPercent /= totalDataSetCount;

			// Process the tables.
			for (final Iterator i = this.getTablesToProcess(dataset).iterator(); i
					.hasNext();) {
				final DataSetTable dsTable = (DataSetTable) i.next();
				this.makeActionsForDatasetTable(dataset, dsTable);

				// Update the progress percentage once per table.
				this.percentComplete += stepPercent;

				// Check not cancelled.
				this.checkCancelled();
			}
		}

		private List getTablesToProcess(final DataSet dataset)
				throws ValidationException {
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
							.hasNext();)
						parentColNames.add(((DataSetColumn) j.next())
								.getModifiedName());
					final Collection ourColNames = new HashSet();
					for (final Iterator j = tbl.getColumns().iterator(); j
							.hasNext();)
						ourColNames.add(((DataSetColumn) j.next())
								.getModifiedName());
					// Make the check.
					if (!ourColNames.containsAll(parentColNames))
						throw new ValidationException(Resources.get(
								"subclassMissingCols", new String[] {
										tbl.getModifiedName(),
										parentTable.getModifiedName() }));
				}
				// Expand the table
				for (final Iterator j = tbl.getRelations().iterator(); j
						.hasNext();) {
					final Relation r = (Relation) j.next();
					final DataSetTable dsTab = (DataSetTable) r.getManyKey()
							.getTable();
					if (!tablesToProcess.contains(dsTab)
							&& !dataset.getDataSetModifications()
									.isMaskedTable(dsTab))
						tablesToProcess.add(dsTab);
				}
			}
			return tablesToProcess;
		}

		private void makeActionsForDatasetTable(final DataSet dataset,
				final DataSetTable dsTable) throws Exception {
			final String finalCombinedName = this.getFinalName(dsTable,
					GenericConstructorRunnable.NO_PARTITION);
			final String tempName = "TEMP";
			int tempNameCount = 0;
			final Map previousTempTables = new HashMap();
			final List partitionValues = new ArrayList();
			partitionValues.add(GenericConstructorRunnable.NO_PARTITION);
			previousTempTables.put(GenericConstructorRunnable.NO_PARTITION,
					null);

			// Use the transformation units to create the basic table.
			// TODO Cope with pseudo-partitioned schema - affects partition
			// values!
			// TODO Cope with relation restrictions.
			for (final Iterator j = dsTable.getTransformationUnits().iterator(); j
					.hasNext();) {
				final TransformationUnit tu = (TransformationUnit) j.next();

				// Partitioned column from this unit? Then partition it.
				final String delayedTempDrop;
				if (dataset.getDataSetModifications().isPartitionedTable(
						dsTable)) {
					final DataSetColumn partCol = (DataSetColumn) dsTable
							.getColumnByName(dataset.getDataSetModifications()
									.getPartitionedColumnName(dsTable));
					if (tu.getNewColumnNameMap().containsValue(partCol))
						delayedTempDrop = this.populatePartitionValues(dataset
								.getDataSetModifications()
								.getPartitionedColumn(partCol), partCol,
								partitionValues, previousTempTables);
					else
						delayedTempDrop = null;
				} else
					delayedTempDrop = null;

				// Do unit once per partition.
				for (final Iterator v = partitionValues.iterator(); v.hasNext();) {
					final String partitionValue = (String) v.next();
					final String tempTable = tempName + tempNameCount++;

					// Translate TU to Action.
					// TODO : Concat column TUNITs.
					// TODO : Expression column TUNITs.
					// Left-join?
					if (tu instanceof JoinTable)
						this.doJoinTable(dataset, dsTable, (JoinTable) tu,
								previousTempTables, partitionValue, tempTable);
					// Select-from?
					else if (tu instanceof SelectFromTable)
						this.doSelectFromTable(dataset, dsTable,
								(SelectFromTable) tu, previousTempTables,
								partitionValue, tempTable);
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
				final String finalName = this.getFinalName(dsTable,
						partitionValue);

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
							this.datasetSchemaName, finalCombinedName);
					action.setTable(finalName);
					action.setColumns(dropCols);
					this.issueAction(action);
				}

				// If was partitioned, do a final left join with the
				// parent table in order to reinstate missing rows dropped
				// by the inner join for the partition.
				if (!partitionValue
						.equals(GenericConstructorRunnable.NO_PARTITION)) {
					// Work out a temp table name.
					final String tempTable = tempName + tempNameCount++;
					// Work out the parent table.
					final DataSetTable parent = (DataSetTable) ((Relation) dsTable
							.getRelations().iterator().next()).getOneKey()
							.getTable();
					// Work out what columns to take from each side.
					final List leftJoinCols = new ArrayList();
					final List leftSelectCols = leftJoinCols;
					final List rightJoinCols = leftJoinCols;
					final List rightSelectCols = new ArrayList();
					for (final Iterator x = parent.getPrimaryKey().getColumns()
							.iterator(); x.hasNext();)
						leftJoinCols.add(((DataSetColumn) x.next())
								.getModifiedName());
					for (final Iterator x = dsTable.getColumns().iterator(); x
							.hasNext();)
						rightSelectCols.add(((DataSetColumn) x.next())
								.getModifiedName());
					rightSelectCols.removeAll(rightJoinCols);
					// Index the right-hand side of the join.
					final Index index = new Index(this.datasetSchemaName, this
							.getFinalName(dsTable,
									GenericConstructorRunnable.NO_PARTITION));
					index.setTable((String) previousTempTables
							.get(partitionValue));
					index.setColumns(leftJoinCols);
					this.issueAction(index);
					// Make the join.
					final LeftJoin action = new LeftJoin(
							this.datasetSchemaName, this.getFinalName(dsTable,
									GenericConstructorRunnable.NO_PARTITION));
					action.setLeftTable(this.getFinalName(parent,
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
					final Drop drop = new Drop(this.datasetSchemaName, this
							.getFinalName(dsTable,
									GenericConstructorRunnable.NO_PARTITION));
					drop.setTable((String) previousTempTables
							.get(partitionValue));
					this.issueAction(drop);
					// Update the previous temp table.
					previousTempTables.put(partitionValue, tempTable);
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
				final DataSetOptimiserType oType = dataset
						.getDataSetOptimiserType();
				if (!oType.equals(DataSetOptimiserType.NONE))
					this.doOptimiseTable(dataset, dsTable, oType,
							partitionValue);
			}
		}

		private void doOptimiseTable(final DataSet dataset,
				final DataSetTable dsTable, final DataSetOptimiserType oType,
				final String partitionValue) throws Exception {
			if (!dsTable.getType().equals(DataSetTableType.DIMENSION)) {
				// Tables are same name, but use 'bool' or 'count'
				// instead of 'main'
				final String optTable = this.getOptimiserTableName(dsTable,
						oType);
				// If main/subclass table type, create table.
				if (oType.equals(DataSetOptimiserType.TABLE)
						|| oType.equals(DataSetOptimiserType.TABLE_BOOL)
						|| oType.equals(DataSetOptimiserType.TABLE_INHERIT)
						|| oType
								.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT)) {
					// The key cols are those from the primary key.
					final List keyCols = new ArrayList();
					for (final Iterator y = dsTable.getPrimaryKey()
							.getColumns().iterator(); y.hasNext();)
						keyCols.add(((DataSetColumn) y.next())
								.getModifiedName());

					// Create the table by selecting the pk.
					final CreateOptimiser create = new CreateOptimiser(
							this.datasetSchemaName, this.getFinalName(dsTable,
									GenericConstructorRunnable.NO_PARTITION));
					create.setKeyColumns(keyCols);
					create.setOptTableName(optTable);
					this.issueAction(create);

					// Index the pk on the new table.
					final Index index = new Index(this.datasetSchemaName, this
							.getFinalName(dsTable,
									GenericConstructorRunnable.NO_PARTITION));
					index.setTable(optTable);
					index.setColumns(keyCols);
					this.issueAction(index);
				}
			} else {
				// Columns are dimension table names with '_bool' or
				// '_count' appended.
				final String optCol = this.getOptimiserColumnName(dsTable,
						partitionValue, oType);
				// Work out the dimension parent.
				final DataSetTable parent = (DataSetTable) ((Relation) dsTable
						.getRelations().iterator().next()).getOneKey()
						.getTable();
				// Set up the column on the dimension parent.
				final String optTable = this.getOptimiserTableName(parent,
						oType);
				// Key columns are primary key cols from parent.
				// Do a left-join update. We're looking for rows
				// where all child non-key cols are non-null.
				final List keyCols = new ArrayList();
				for (final Iterator y = parent.getPrimaryKey().getColumns()
						.iterator(); y.hasNext();)
					keyCols.add(((DataSetColumn) y.next()).getModifiedName());
				final List nonNullCols = new ArrayList();
				for (final Iterator y = dsTable.getColumns().iterator(); y
						.hasNext();) {
					final DataSetColumn col = (DataSetColumn) y.next();
					if (!dataset.getDataSetModifications().isMaskedColumn(col))
						nonNullCols.add(col.getModifiedName());
				}
				nonNullCols.removeAll(keyCols);

				// Do the bool/count update.
				final UpdateOptimiser update = new UpdateOptimiser(
						this.datasetSchemaName, this.getFinalName(dsTable,
								GenericConstructorRunnable.NO_PARTITION));
				update.setKeyColumns(keyCols);
				update.setNonNullColumns(nonNullCols);
				update.setSourceTableName(this.getFinalName(dsTable,
						partitionValue));
				update.setOptTableName(optTable);
				update.setOptColumnName(optCol);
				update.setCountNotBool(oType
						.equals(DataSetOptimiserType.COLUMN)
						|| oType.equals(DataSetOptimiserType.COLUMN_INHERIT)
						|| oType.equals(DataSetOptimiserType.TABLE)
						|| oType.equals(DataSetOptimiserType.TABLE_INHERIT));
				this.issueAction(update);

				// Work out the inherited parents.
				final List parents = new ArrayList();
				parents.add(parent);
				if (oType.equals(DataSetOptimiserType.COLUMN_BOOL_INHERIT)
						|| oType
								.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT)
						|| oType.equals(DataSetOptimiserType.COLUMN_INHERIT)
						|| oType.equals(DataSetOptimiserType.TABLE_INHERIT))
					for (int z = 0; z < parents.size(); z++) {
						final DataSetTable child = (DataSetTable) parents
								.get(z);
						final Iterator fks = child.getForeignKeys().iterator();
						if (fks.hasNext())
							parents.add(((Relation) ((Key) fks.next())
									.getRelations().iterator().next())
									.getOneKey().getTable());
					}

				// Copy the column to the inherited parents by following pairs.
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
					final CopyOptimiser copy = new CopyOptimiser(
							this.datasetSchemaName, this.getFinalName(dsTable,
									GenericConstructorRunnable.NO_PARTITION));
					copy.setFromOptTableName(this.getOptimiserTableName(
							fromParent, oType));
					copy.setFromKeyColumns(fromKeyCols);
					copy.setViaTableName(this.getFinalName(fromParent,
							GenericConstructorRunnable.NO_PARTITION));
					copy.setToKeyColumns(toKeyCols);
					copy.setToOptTableName(this.getOptimiserTableName(toParent,
							oType));
					copy.setOptColumnName(optCol);
					this.issueAction(copy);
				}
			}
		}

		private void doSelectFromTable(final DataSet dataset,
				final DataSetTable dsTable, final SelectFromTable stu,
				final Map previousTempTables, final String partitionValue,
				final String tempTable) throws Exception {

			final Table sourceTable = stu.getTable();
			final String schema = sourceTable instanceof DataSetTable ? this.datasetSchemaName
					: ((JDBCSchema) sourceTable.getSchema())
							.getDatabaseSchema();
			// Source tables are always main or subclass and
			// therefore are never partitioned.
			final String table = sourceTable instanceof DataSetTable ? this
					.getFinalName((DataSetTable) sourceTable,
							GenericConstructorRunnable.NO_PARTITION) : stu
					.getTable().getName();
			final Map selectCols = new HashMap();
			// Populate vars.
			for (final Iterator k = stu.getNewColumnNameMap().entrySet()
					.iterator(); k.hasNext();) {
				final Map.Entry entry = (Map.Entry) k.next();
				final DataSetColumn col = (DataSetColumn) entry.getValue();
				if (col.getDependency()
						|| !dataset.getDataSetModifications().isMaskedColumn(
								col))
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
			final Select action = new Select(this.datasetSchemaName, this
					.getFinalName(dsTable,
							GenericConstructorRunnable.NO_PARTITION));
			action.setSchema(schema);
			action.setTable(table);
			action.setSelectColumns(selectCols);
			action.setResultTable(tempTable);
			if (!partitionValue.equals(GenericConstructorRunnable.NO_PARTITION)) {
				action.setPartitionColumn((String) stu
						.getReverseNewColumnNameMap().get(
								dsTable.getColumnByName(dataset
										.getDataSetModifications()
										.getPartitionedColumnName(dsTable))));
				action.setPartitionValue(partitionValue);
			}
			if (dataset.getSchemaModifications().isRestrictedTable(dsTable,
					stu.getTable()))
				action.setTableRestriction(dataset.getSchemaModifications()
						.getRestrictedTable(dsTable, stu.getTable()));
			this.issueAction(action);
		}

		private void doJoinTable(final DataSet dataset,
				final DataSetTable dsTable, final JoinTable ljtu,
				final Map previousTempTables, final String partitionValue,
				final String tempTable) throws Exception {
			final String rightSchema = ((JDBCSchema) ljtu.getTable()
					.getSchema()).getDatabaseSchema();
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
				if (col.getDependency()
						|| !dataset.getDataSetModifications().isMaskedColumn(
								col))
					selectCols.put(entry.getKey(), col.getModifiedName());
			}
			// Index the left-hand side of the join.
			final Index index = new Index(this.datasetSchemaName, this
					.getFinalName(dsTable,
							GenericConstructorRunnable.NO_PARTITION));
			index.setTable((String) previousTempTables.get(partitionValue));
			index.setColumns(leftJoinCols);
			this.issueAction(index);
			// Make the join.
			final Join action = new Join(this.datasetSchemaName, this
					.getFinalName(dsTable,
							GenericConstructorRunnable.NO_PARTITION));
			action
					.setLeftTable((String) previousTempTables
							.get(partitionValue));
			action.setRightSchema(rightSchema);
			action.setRightTable(rightTable);
			action.setLeftJoinColumns(leftJoinCols);
			action.setRightJoinColumns(rightJoinCols);
			action.setSelectColumns(selectCols);
			action.setResultTable(tempTable);
			if (!partitionValue.equals(GenericConstructorRunnable.NO_PARTITION)) {
				action.setPartitionColumn((String) ljtu
						.getReverseNewColumnNameMap().get(
								dsTable.getColumnByName(dataset
										.getDataSetModifications()
										.getPartitionedColumnName(dsTable))));
				action.setPartitionValue(partitionValue);
			}
			if (dataset.getSchemaModifications().isRestrictedTable(dsTable,
					ljtu.getTable()))
				action.setTableRestriction(dataset.getSchemaModifications()
						.getRestrictedTable(dsTable, ljtu.getTable()));
			if (dataset.getSchemaModifications().isRestrictedRelation(dsTable,
					ljtu.getSchemaRelation(), ljtu.getSchemaRelationIteration())) {
				action.setRelationRestrictionLeftIsFirst(ljtu.getSchemaRelation().getFirstKey().equals(ljtu.getSchemaSourceKey()));
				action.setRelationRestriction(dataset.getSchemaModifications()
						.getRestrictedRelation(dsTable, ljtu.getSchemaRelation(), ljtu.getSchemaRelationIteration()));
			}
			this.issueAction(action);
		}

		private String populatePartitionValues(final PartitionedColumn pc,
				final DataSetColumn partCol, final List partitionValues,
				final Map previousTempTables) throws SQLException {
			partitionValues.clear();
			if (pc instanceof SingleValue) {
				if (((SingleValue) pc).getIncludeNull())
					partitionValues.add(null);
				else
					partitionValues.add(((SingleValue) pc).getValue());
			} else if (pc instanceof ValueCollection) {
				if (((ValueCollection) pc).getIncludeNull())
					partitionValues.add(null);
				partitionValues.addAll(((ValueCollection) pc).getValues());
			} else if (pc instanceof UniqueValues) {
				DataSetColumn col = partCol;
				while (col instanceof InheritedColumn)
					col = ((InheritedColumn) col).getInheritedColumn();
				partitionValues.addAll(this.helper
						.listDistinctValues(((WrappedColumn) col)
								.getWrappedColumn()));
			}
			final String delayedTempDrop = (String) previousTempTables
					.get(GenericConstructorRunnable.NO_PARTITION);
			for (final Iterator z = partitionValues.iterator(); z.hasNext();)
				previousTempTables.put(z.next(), delayedTempDrop);
			return delayedTempDrop;
		}

		private void issueAction(final MartConstructorAction action)
				throws Exception {
			// Execute the action.
			this.statusMessage = action.getStatusMessage();
			this.issueListenerEvent(MartConstructorListener.ACTION_EVENT,
					action);
		}

		private String getOptimiserTableName(final DataSetTable dsTable,
				final DataSetOptimiserType oType) {
			final StringBuffer finalName = new StringBuffer();
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

		private String getOptimiserColumnName(final DataSetTable dsTable,
				final String partitionValue, final DataSetOptimiserType oType) {
			final StringBuffer sb = new StringBuffer();
			sb.append(dsTable.getModifiedName());
			sb.append(Resources.get("tablenameSubSep"));
			if (!partitionValue.equals(GenericConstructorRunnable.NO_PARTITION)) {
				sb.append(this.getSanitizedPartitionValue(partitionValue));
				sb.append(Resources.get("tablenameSubSep"));
			}
			sb
					.append((oType.equals(DataSetOptimiserType.COLUMN_BOOL)
							|| oType
									.equals(DataSetOptimiserType.COLUMN_BOOL_INHERIT)
							|| oType.equals(DataSetOptimiserType.TABLE_BOOL) || oType
							.equals(DataSetOptimiserType.TABLE_BOOL_INHERIT)) ? Resources
							.get("boolColSuffix")
							: Resources.get("countColSuffix"));
			return sb.toString();
		}

		private String getSanitizedPartitionValue(final String partitionValue) {
			return partitionValue.replaceAll("\\W+", "_")
					.replaceAll("__+", "_");
		}

		private String getFinalName(final DataSetTable dsTable,
				final String partitionValue) {
			final StringBuffer finalName = new StringBuffer();
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
				if (!partitionValue
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
						martDataSets.put(mart, new HashSet());
					((Collection) martDataSets.get(mart)).add(ds);
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
						for (final Iterator i = ((Collection) j.next())
								.iterator(); i.hasNext();)
							try {
								this
										.issueListenerEvent(MartConstructorListener.DATASET_STARTED);
								this.makeActionsForDataset((DataSet) i.next(),
										totalDataSetCount);
							} catch (final Throwable t) {
								throw t;
							} finally {
								// Make sure the helper always gets a chance to
								// tidy up,
								// even if an exception is thrown.
								this
										.issueListenerEvent(MartConstructorListener.DATASET_ENDED);
							}
					} finally {
						this
								.issueListenerEvent(MartConstructorListener.MART_ENDED);
					}
				}
			} catch (final ConstructorException e) {
				this.failure = e;
			} catch (final ValidationException e) {
				// This is so the users see a nice message straight away.
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
		public Collection listDistinctValues(Column col) throws SQLException;
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
