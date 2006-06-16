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

import java.io.IOException;
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
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.DataSet.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.DataSet.PartitionedColumnType.ValueCollection;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This interface defines the behaviour expected from an object which can take a
 * dataset and actually construct a mart based on this information. Whether it
 * carries out the task or just writes some DDL to be run by the user later is
 * up to the implementor.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 16th June 2006
 * @since 0.1
 */
public interface MartConstructor extends DataLink, Comparable {
	/**
	 * This method takes a dataset and either generates a script for the user to
	 * run later to construct a mart, or does the work right now. The end result
	 * should be a completely finished and populated mart, or the script to make
	 * one. The work is done inside a thread, which is returned unstarted. The
	 * user should create a new {@link Thread} instance around this, and start
	 * it by calling {@link Thread#run()}. They can then monitor it using the
	 * methods provided by the {@link ConstructorRunnable} interface.
	 * 
	 * @param ds
	 *            the dataset to build the mart for.
	 * @return the thread that will build it.
	 * @throws Exception
	 *             if there was any problem creating the builder thread.
	 */
	public ConstructorRunnable getConstructorRunnable(DataSet ds)
			throws Exception;

	/**
	 * Returns the name of this constructor.
	 * 
	 * @return the name of this constructor.
	 */
	public String getName();

	/**
	 * The base implementation simply does the bare minimum, ie. synchronises
	 * the dataset before starting work. It doesn't actually generate any tables
	 * or DDL. You should override the {@link #getConstructorRunnable(DataSet)}
	 * method to actually create a construction thread that does some useful
	 * work.
	 */
	public abstract class GenericMartConstructor implements MartConstructor {
		private final String name;

		/**
		 * The constructor creates a mart constructor with the given name.
		 * 
		 * @param name
		 *            the name for this new constructor.
		 */
		public GenericMartConstructor(String name) {
			// Remember the values.
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public abstract ConstructorRunnable getConstructorRunnable(DataSet ds)
				throws Exception;

		public boolean test() throws Exception {
			return true;
		}

		public boolean canCohabit(DataLink partner) {
			return false;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			MartConstructor c = (MartConstructor) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			if (o == null || !(o instanceof MartConstructor))
				return false;
			MartConstructor c = (MartConstructor) o;
			return c.toString().equals(this.toString());
		}
	}

	/**
	 * This class refers to a placeholder mart constructor which does nothing
	 * except prevent null pointer exceptions.
	 */
	public static class DummyMartConstructor extends GenericMartConstructor {
		/**
		 * The constructor passes everything on up to the parent.
		 * 
		 * @param name
		 *            the name to give this mart constructor.
		 */
		public DummyMartConstructor(String name) {
			super(name);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This runnable will always fail immediately without attempting to do
		 * anything, throwing a constructor explanation saying that it does not
		 * implement any kind of SQL generation.
		 */
		public ConstructorRunnable getConstructorRunnable(DataSet ds) {
			return new ConstructorRunnable() {
				public void run() {
				}

				public String getStatusMessage() {
					return "";
				}

				public int getPercentComplete() {
					return 100;
				}

				public Exception getFailureException() {
					return new ConstructorException(BuilderBundle
							.getString("defaultMartConstNotImplemented"));
				}

				public void cancel() {
				}
			};
		}
	}

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

		/**
		 * Flag that we are about to begin.
		 * 
		 * @throws Exception
		 *             if anything went wrong. Usually this will be a
		 *             {@link SQLException} or {@link IOException}, but may be
		 *             others too.
		 */
		public void startActions() throws Exception;

		/**
		 * Given an action, executes it. The action level specifies how far down
		 * the tree of actions we are. This may or may not be important.
		 * 
		 * @param action
		 *            the action to handle.
		 * @param level
		 *            the level the action is at. Once a particular level is
		 *            reached, this method should never be passed actions from
		 *            preceding levels again.
		 * @throws Exception
		 *             if anything went wrong. Usually this will be a
		 *             {@link SQLException} or {@link IOException}, but may be
		 *             others too.
		 */
		public void executeAction(MCAction action, int level) throws Exception;

		/**
		 * Flag that we are about to end.
		 * 
		 * @throws Exception
		 *             if anything went wrong. Usually this will be a
		 *             {@link SQLException} or {@link IOException}, but may be
		 *             others too.
		 */
		public void endActions() throws Exception;
	}

	/**
	 * Defines the generic way of constructing a mart.
	 */
	public static class GenericConstructorRunnable implements
			ConstructorRunnable {
		private static final String DUMMY_KEY = "__generic_mc__dummy_map_key__";

		private String statusMessage = "";

		private double percentComplete = 0.0;

		private boolean cancelled = false;

		private Exception failure = null;

		private DataSet ds;

		private Helper helper;

		/**
		 * Constructs a mart builder that will build the mart in the given
		 * dataset, using the given helper.
		 * 
		 * @param ds
		 *            the dataset to transform into a mart.
		 * @param helper
		 *            the helper to use in the transformation.
		 */
		public GenericConstructorRunnable(DataSet ds, Helper helper) {
			super();
			this.ds = ds;
			this.helper = helper;
		}

		public void run() {
			try {
				this.doIt();
			} catch (ConstructorException e) {
				this.failure = e;
			} catch (Throwable t) {
				this.failure = new ConstructorException(t);
			}
		}

		private void doIt() throws Exception {
			// Mart construction is done by processing the main table first,
			// then the main dimension(s), then the subclass table(s), with
			// subclass dimensions after the subclass table they belong to.

			// Before work starts, we need to construct a map of partition
			// columns to the values we are going to partition those columns by.
			this.statusMessage = BuilderBundle.getString("mcGettingPartitions");
			Map partitionValues = new HashMap();

			// Iterate over the tables. If the table has a partition column,
			// add it to the map along with the values to use to partition
			// that column.
			for (Iterator i = ds.getTables().iterator(); i.hasNext();) {
				Table t = (Table) i.next();

				// Does it have any partition columns? The intersection
				// of its columns with the set of partition columns
				// will tell us all.
				List tabCols = new ArrayList(t.getColumns());
				tabCols.retainAll(ds.getPartitionedWrappedColumns());

				// No columns left = no partition columns on this table.
				if (tabCols.isEmpty())
					continue;

				// There should be only one column left per table.
				// As we can only partition on wrapped columns,
				// we can assume it will be a wrapped column.
				WrappedColumn col = (WrappedColumn) tabCols.get(0);

				// What kind of partition is this?
				PartitionedColumnType type = ds
						.getPartitionedWrappedColumnType(col);

				// Unique values - one partition per value.
				if (type instanceof UniqueValues) {
					List values = helper.listDistinctValues(col
							.getWrappedColumn());
					partitionValues.put(col, values);
				}

				// Single value - one partition.
				else if (type instanceof SingleValue) {
					SingleValue sv = (SingleValue) type;
					Object value = sv.getIncludeNull() ? null : sv.getValue();
					partitionValues.put(col, Collections.singletonList(value));
				}

				// Select values - one partition per value.
				else if (type instanceof ValueCollection) {
					ValueCollection vc = (ValueCollection) type;
					List values = new ArrayList(vc.getValues());
					if (vc.getIncludeNull())
						values.add(null);
					partitionValues.put(col, values);
				}
			}

			// Cancelled?
			this.checkCancelled();

			// Now we have to make space to store the graph of actions we
			// must carry out.
			this.statusMessage = BuilderBundle.getString("mcCreatingGraph");
			MCActionGraph actionGraph = new MCActionGraph();

			// Find the main table of the dataset.
			DataSetTable mainTable = null;
			for (Iterator i = ds.getTables().iterator(); i.hasNext()
					&& mainTable == null;) {
				DataSetTable table = (DataSetTable) i.next();
				if (table.getType().equals(DataSetTableType.MAIN))
					mainTable = table;
			}

			// Work out if we are going to partition by schema. If we are,
			// then we make a list of the individual schema partitions. If
			// not then we make a list containing just the original schema
			// on its own. By making a list with just one entry we simplify
			// the code later.
			Collection schemas = null;
			if (ds.getPartitionOnSchema()
					&& (mainTable.getSchema() instanceof SchemaGroup))
				schemas = ((SchemaGroup) mainTable.getSchema()).getSchemas();
			else
				schemas = Collections.singletonList(mainTable.getSchema());

			// Check not cancelled.
			this.checkCancelled();

			// For each schema partition, we process all the tables. If
			// we are partitioning by schema, then this will result in
			// multiple identical datasets based on data from different
			// schemas. If not, then we will get a single dataset based
			// on the results from the single source schema. If that
			// single source schema is a group (but not partitioned), then
			// the tables created will contain the combined results of all
			// the members of the schema group.
			for (Iterator i = schemas.iterator(); i.hasNext();) {
				Schema currSchema = (Schema) i.next();
				// Maintain a map that links each dataset table through
				// the path of partition values affecting it to the actual
				// final table names that represent that dataset table. There
				// are many levels of partitioning, so the map is nested.
				// Where values are not available, the dummy key is used
				// to replace them and maintain the structure of the map.
				// ds table ->
				// schema ->
				// parent table partition value ->
				// partition value ->
				// actual table name
				Map dsTableNameNestedMap = new HashMap();

				// Maintain a map that records the last action in the
				// graph that is associated with each dataset table. This
				// way we can make steps that occur after the creation of
				// the dataset tables reliant on the completion of all
				// the dataset tables before execution.
				// ds table -> last action
				Map dsTableLastActionMap = new HashMap();

				// Process the main table.
				this.processTable(currSchema, actionGraph, null, mainTable,
						dsTableNameNestedMap, dsTableLastActionMap, null,
						partitionValues, helper);

				// Check not cancelled.
				this.checkCancelled();

				// Subclass tables and main dimension tables are dependent
				// on last actions of main table.
				MCAction scAndMainDimDependsOn = (MCAction) dsTableLastActionMap
						.get(mainTable);

				// Process all dimensions of main.
				for (Iterator k = mainTable.getPrimaryKey().getRelations()
						.iterator(); k.hasNext();) {

					// Get the dimension relation.
					Relation dimRel = (Relation) k.next();

					// Work out the table it points to.
					DataSetTable dimTableCandidate = (DataSetTable) dimRel
							.getManyKey().getTable();

					// Process the dimension table.
					this.processTable(currSchema, actionGraph,
							scAndMainDimDependsOn, dimTableCandidate,
							dsTableNameNestedMap, dsTableLastActionMap, dimRel,
							partitionValues, helper);

					// Check not cancelled.
					this.checkCancelled();
				}

				// Process all subclass tables.
				for (Iterator j = mainTable.getPrimaryKey().getRelations()
						.iterator(); j.hasNext();) {

					// Get the subclass relation.
					Relation scRel = (Relation) j.next();

					// Work out the table it points to.
					DataSetTable scTableCandidate = (DataSetTable) scRel
							.getManyKey().getTable();

					// Check that it is a subclass relation.
					if (!scTableCandidate.getType().equals(
							DataSetTableType.MAIN_SUBCLASS))
						continue;

					// Process subclass table.
					this.processTable(currSchema, actionGraph,
							scAndMainDimDependsOn, scTableCandidate,
							dsTableNameNestedMap, dsTableLastActionMap, scRel,
							partitionValues, helper);

					// Check not cancelled.
					this.checkCancelled();

					// Subclass dimensions are dependent on last actions of
					// subclass table.
					MCAction scDimDependsOn = (MCAction) dsTableLastActionMap
							.get(scTableCandidate);

					// Process all dimensions of subclass.
					for (Iterator k = scTableCandidate.getPrimaryKey()
							.getRelations().iterator(); k.hasNext();) {

						// Get the dimension relation.
						Relation dimRel = (Relation) k.next();

						// Work out the table it points to.
						DataSetTable dimTableCandidate = (DataSetTable) dimRel
								.getManyKey().getTable();

						// Process the subclass dimension.
						this.processTable(currSchema, actionGraph,
								scDimDependsOn, dimTableCandidate,
								dsTableNameNestedMap, dsTableLastActionMap,
								dimRel, partitionValues, helper);

						// Check not cancelled.
						this.checkCancelled();
					}
				}

				// TODO
				// 2. Optimiser nodes. Adding 'has' columns/tables, etc.
				// Optimiser nodes are dependent on last actions of all
				// tables within same schema, using dsTableLastActionMap.
				// Use the dsTableNameNestedMap to discover all the tables
				// that need this doing.
			}

			// Check not cancelled.
			this.checkCancelled();

			// Work out the progress step size.
			double stepPercent = 100.0 / (double) actionGraph.getActions()
					.size();

			// Initialize the helper, telling it we are about to start
			// sending it actions to process.
			helper.startActions();

			// The action graph is tiered into several levels, called
			// depths. These levels are assigned automatically according
			// to the dependency of actions on other actions. We want
			// to start at the first depth (0), process all actions there,
			// then move on the next depth, and so on. Each depth is
			// reliant on the previous depth completing before it can
			// begin processing.
			int depth = 0;
			Collection actions = actionGraph.getActionsAtDepth(depth);
			while (!actions.isEmpty()) {
				// Loop over all actions at current depth.
				for (Iterator i = actions.iterator(); i.hasNext();) {
					MCAction action = (MCAction) i.next();

					// Execute the action.
					this.statusMessage = action.getStatusMessage();
					helper.executeAction(action, depth);

					// Update the progress percentage.
					this.percentComplete += stepPercent;

					// Check not cancelled.
					this.checkCancelled();
				}

				// Get next depth.
				actions = actionGraph.getActionsAtDepth(++depth);
			}

			// All done!
			helper.endActions();
		}

		private void processTable(Schema schema, MCActionGraph actionGraph,
				MCAction firstActionDependsOn, DataSetTable dsTable,
				Map dsTableNameNestedMap, Map dsTableLastActionMap,
				Relation parentRelation, Map partitionValues, Helper helper)
				throws Exception {
			// This method constructs one or more tables based on the
			// dataset table. If the schema it is asked to retrieve data
			// from is a group schema, then it does this once per schema
			// then merges the results using a union statement at the end.
			// Otherwise, it does this just once.
			// If the table is partitioned, then one table is created for
			// each partition value in the partitioned column.
			// If the table is related to a parent dataset table, then
			// that parent dataset table may have been split up itself
			// depending on partition values and possibly even the partition
			// values of the parent of the parent table (ie. the main table).
			// If so, then after creation the table we just created is split
			// into chunks, one per individual parent table we found.

			// Work out if we have to do a multi-schema union, or a
			// single-schema select. Store the result as a list where
			// each member of the list represents one complete separate
			// set of tables that need to be constructed.
			Collection schemas = null;
			if (schema instanceof SchemaGroup)
				schemas = ((SchemaGroup) schema).getSchemas();
			else
				schemas = Collections.singletonList(schema);

			// We now need to work out at which point to start the process
			// of transforming the table. If the table is partitioned, we
			// should start from the table in which the partitioned column
			// lies, in order to restrict the data as early as possible.
			// Otherwise, we should start from the base table around which
			// the dataset table is constructed.
			Table startTable = null;
			Collection partColValues = null;

			// To do this, we check to see if there is a partition column
			// on this dataset table.
			WrappedColumn partitionColumn = null;
			for (Iterator i = dsTable.getColumns().iterator(); i.hasNext()
					&& partitionColumn == null;) {
				WrappedColumn c = (WrappedColumn) i.next();
				if (partitionValues.containsKey(c))
					partitionColumn = c;
			}

			// If there was a partition column, find out what table it
			// is on, and use that as the start point. At the same time,
			// store the values we should use for that column.
			if (partitionColumn != null) {
				startTable = partitionColumn.getWrappedColumn().getTable();
				partColValues = (Collection) partitionValues
						.get(partitionColumn);
			}

			// Otherwise, we start at the base table at the centre of
			// this dataset table, and we specify the partition values
			// as a single-value set containing the dummy key.
			else {
				startTable = dsTable.getUnderlyingTable();
				partColValues = Collections
						.singleton(GenericConstructorRunnable.DUMMY_KEY);
			}

			// We need to keep track of the final action performed in
			// the processing of this table, so that we can store it
			// in the map indicating the end-point of each table processed.
			// By default, the last action is the one this table
			// depends on itself, but this will be overridden by any actual
			// actions.
			MCAction lastAction = firstActionDependsOn;

			// Set up a map to hold the temp table names we make,
			// and a list for the last actions associated with each temp table.
			// The temp table names in the map are keyed by partition value.
			Map tempTableNames = new HashMap();
			List lastActions = new ArrayList();

			// Placeholder for name of the target temp table that will
			// contain the constructed table.
			String targetTempTableName = null;

			// Process dataset table once per partition value.
			for (Iterator j = partColValues.iterator(); j.hasNext();) {
				Object partitionValue = j.next();

				// Process table once per schema. After this loop,
				// if the schema was a single schema, targetTempTableName
				// will contain the name of the temp table that is
				// the constructed table. However, if the schema was
				// a group schema, this will not be the case.
				for (Iterator i = schemas.iterator(); i.hasNext();) {
					Schema currSchema = (Schema) i.next();

					// Come up with a new temp table name for this
					// schema.
					targetTempTableName = helper.getNewTempTableName();
					tempTableNames.put(schema, targetTempTableName);

					// Create the table for this partition.
					lastActions.add(this.constructTable(currSchema.getName(),
							actionGraph, firstActionDependsOn, dsTable,
							startTable, partitionColumn, partitionValue,
							targetTempTableName));
				}

				// At end, if dealing with multiple schemas, do a union on
				// the temporary tables to create the final table, one per
				// partition value. Last action is the union action. The
				// target temp table name will be updated to contain the
				// name of the temp table that contains the merged tables.
				if (schemas.size() > 1) {
					// Come up with a temp table name for the union table.
					targetTempTableName = helper.getNewTempTableName();

					// Union depends on final actions of each temporary
					// table.
					UnionTables union = new UnionTables(targetTempTableName,
							tempTableNames.values());
					for (Iterator i = lastActions.iterator(); i.hasNext();)
						union.addParent((MCAction) i.next());
					actionGraph.addAction(union);
					lastAction = union;

					// Drop all the old temp tables that were combined in
					// the union stage.
					for (Iterator i = tempTableNames.values().iterator(); i
							.hasNext();) {
						String tableName = (String) i.next();
						MCAction dropTable = new DropTable(tableName);
						dropTable.addParent(lastAction);
						actionGraph.addAction(dropTable);
					}
				}

				// Otherwise, for a sinlge source schema, last action is
				// the first (only) action in the last action list.
				else
					lastAction = (MCAction) lastActions.get(0);

				// Maintain a map that will store each parent table partition,
				// and parent table's parent table partition values. This
				// is in order to keep track of how we split this target table
				// into pieces, one for each parent table piece. The
				// map is nested. If the parent table is a single table, then
				// this will contain only one entry keyed by two levels of the
				// dummy key. Where a parent table's parent is not partitioned,
				// the first level key will be the dummy key. Where a parent
				// table is not partitioned, but it's parent is, then the
				// second level key will be the dummy key.
				// parent table's parent partition value ->
				// parent partition value ->
				// temp table name.
				Map segmentTables = new HashMap();

				// If dataset table is dimension or subclass, link it to its
				// parent. This may involve splitting it into segments.
				if (parentRelation != null) {

					// Work out parent PK.
					PrimaryKey parentPK = (PrimaryKey) parentRelation
							.getOneKey();

					// Work out child FK.
					ForeignKey childFK = (ForeignKey) parentRelation
							.getManyKey();

					// Work out parent dataset table.
					DataSetTable parentDSTable = (DataSetTable) parentPK
							.getTable();

					// Make a list to hold actions that final drop depends
					// on.
					List dropDependsOn = new ArrayList();

					// Use dsTableNameNestedMap to identify parent partition
					// values of parent table, in case parent itself was
					// segmented.
					Map parentVToV = (Map) ((Map) dsTableNameNestedMap
							.get(parentDSTable)).get(schema);

					// For each parent table's parent partition value, loop.
					for (Iterator i = parentVToV.keySet().iterator(); i
							.hasNext();) {
						Object parentParentPartitionValue = i.next();

						// Get the partition values for this parent
						// table based on the current value of it's
						// parent's partition.
						Map vToName = (Map) parentVToV
								.get(parentParentPartitionValue);
						segmentTables.put(parentParentPartitionValue,
								new HashMap());

						// Loop over each parent's partition value and
						// obtain actual parent table names.
						for (Iterator k = vToName.keySet().iterator(); k
								.hasNext();) {
							Object parentPartitionValue = k.next();
							String parentTableName = (String) vToName
									.get(parentPartitionValue);

							// Come up with a temp table name for the
							// segment table.
							String segmentTableName = helper
									.getNewTempTableName();

							// Create a new temp table restricted by
							// the parent relation.
							RestrictTable restrict = new RestrictTable(
									segmentTableName, targetTempTableName,
									parentTableName, parentPK.getColumns(),
									childFK.getColumns());
							restrict.addParent(lastAction);
							actionGraph.addAction(restrict);

							// Establish FK to PK relation on parent table.
							CreateFK createFK = new CreateFK(segmentTableName,
									childFK.getColumns(), parentTableName,
									parentPK.getColumns());
							createFK.addParent(restrict);
							actionGraph.addAction(createFK);

							// Add FK to PK action to dropDependsOn.
							dropDependsOn.add(createFK);

							// Update segment map.
							((Map) segmentTables
									.get(parentParentPartitionValue)).put(
									parentPartitionValue, segmentTableName);
						}
					}

					// After last segment, drop original temp table
					// (tempTableName) as it has now been split into pieces.
					MCAction dropTable = new DropTable(targetTempTableName);
					for (Iterator i = dropDependsOn.iterator(); i.hasNext();)
						dropTable.addParent((MCAction) i.next());
					actionGraph.addAction(dropTable);

					// Subsequent actions depend on the drop table action.
					lastAction = dropTable;
				}

				// If this is not a dimension or subclass, then there
				// is only one segment, which we have already made.
				// Add it to the map using two levels of dummy keys.
				else {
					Map parentPartitionValueMap = new HashMap();
					parentPartitionValueMap.put(
							GenericConstructorRunnable.DUMMY_KEY,
							targetTempTableName);
					segmentTables.put(GenericConstructorRunnable.DUMMY_KEY,
							parentPartitionValueMap);
				}

				// Remember the last action we did above, and create a
				// place to remember the last rename action we are about
				// to carry out below.
				MCAction prePKAction = lastAction;
				MCAction lastRenameAction = null;

				// For each segment table, construct the PK and rename
				// to the final name.

				// Start by looping over the top-level keys in the segment
				// map, ie. the parent's parent partition values.
				for (Iterator i = segmentTables.keySet().iterator(); i
						.hasNext();) {
					Object parentParentPartitionValue = i.next();
					Map parentPartitionValueMap = (Map) segmentTables
							.get(parentParentPartitionValue);

					// Descend into second level of nesting, the
					// individual parent's partition values. Get the
					// actual table names from this.
					for (Iterator k = parentPartitionValueMap.keySet()
							.iterator(); k.hasNext();) {
						Object parentPartitionValue = k.next();
						String tableName = (String) parentPartitionValueMap
								.get(parentPartitionValue);

						// Reset the last action to the one that happened
						// just before this loop. This means that each
						// PK action, or each rename action if no PK, can
						// be run in parallel.
						lastAction = prePKAction;

						// Find PK columns and create the PK if present.
						PrimaryKey pk = dsTable.getPrimaryKey();
						if (pk != null) {
							CreatePK createPK = new CreatePK(tableName, pk
									.getColumns());
							createPK.addParent(lastAction);
							actionGraph.addAction(createPK);
							lastAction = createPK;
						}

						// Make the final name for this table.
						String finalName = this.createFinalName(dsTable,
								schema, parentParentPartitionValue,
								parentPartitionValue, partitionValue);

						// Rename segment table to final name. Make this
						// action dependent on the last action - which will
						// either be the action before this loop, or the
						// create PK action above, depending on whether a PK
						// exists or not.
						MCAction rename = new RenameTable(tableName, finalName);
						rename.addParent(lastAction);
						actionGraph.addAction(rename);
						lastRenameAction = rename;

						// Add final name to nested name map for this table.
						// ds table ->
						// schema ->
						// parent partition value ->
						// partition value ->
						// actual table name.
						Map schemaToParentV = (Map) dsTableNameNestedMap
								.get(dsTable);
						if (schemaToParentV == null) {
							schemaToParentV = new HashMap();
							dsTableNameNestedMap.put(dsTable, schemaToParentV);
						}
						Map parentVToV = (Map) schemaToParentV.get(schema);
						if (parentVToV == null) {
							parentVToV = new HashMap();
							schemaToParentV.put(schema, parentVToV);
						}
						Map vToName = (Map) parentVToV
								.get(parentPartitionValue);
						if (vToName == null) {
							vToName = new HashMap();
							parentVToV.put(parentPartitionValue, vToName);
						}
						vToName.put(partitionValue, finalName);
					}
				}

				// Set the last action for this table to be the
				// last rename action we carry out.
				lastAction = lastRenameAction;
			}

			// Remember the last action that was carried out for this
			// table by adding it to the appropriate map.
			dsTableLastActionMap.put(dsTable, lastAction);
		}

		private MCAction constructTable(String schemaName,
				MCActionGraph actionGraph, MCAction firstActionDependsOn,
				DataSetTable dsTable, Table startTable,
				DataSetColumn partitionColumn, Object partitionValue,
				String tempTableName) throws Exception {
			// Work out the relation that led us here.
			Relation initialRelation = null;
			if (!dsTable.getType().equals(DataSetTableType.MAIN))
				initialRelation = (Relation) dsTable.getUnderlyingRelations()
						.get(0);

			// Holder for the last action performed on this table.
			// By default, the last action is the first, but this
			// will be overridden by any actual actions.
			MCAction lastAction = firstActionDependsOn;

			// The relation queue stores relations we need to merge,
			// in the order we need to merge them.
			List relationQueue = new ArrayList();
			int relQueuePos = 0;

			// Loop over all relations in queue. Initially the queue will
			// be empty, so make sure we loop at least once so the queue
			// has a chance of being filled. Keep track of tables we have
			// visited so that we know which ends of relations we need
			// to follow.
			Set mergedTables = new HashSet();
			do {
				// Get next relation (or if queue empty, use null)
				// Also work out which table we are merging.
				Relation relation = null;
				Table realTable = null;
				Key fromKey = null;
				Key toKey = null;
				if (!relationQueue.isEmpty()) {
					relation = (Relation) relationQueue.get(relQueuePos++);
					fromKey = mergedTables.contains(relation.getFirstKey()
							.getTable()) ? relation.getFirstKey() : relation
							.getSecondKey();
					toKey = relation.getOtherKey(fromKey);
					realTable = toKey.getTable();
				} else {
					realTable = startTable;
				}

				// Translate fromKey into related dataset columns.
				List fromKeyDSColumns = new ArrayList();

				// Populate the from key translation with nulls to prevent
				// index-out-of-bounds exceptions later.
				if (fromKey != null) {
					for (int i = 0; i < fromKey.getColumns().size(); i++)
						fromKeyDSColumns.add(null);

					// TODO
					// We can't guarantee that this exact key will be in the
					// dataset
					// table, because it may have used other columns which refer
					// to this key instead. So, we have to find all keys in the
					// already-merged tables which refer via a relation to the
					// same
					// columns as this key, and make a list of them. We run
					// through that list until we find a key that we have
					// already
					// merged, and then we use that one instead. We can use the
					// first one because it doesn't matter about second ones, as
					// relations can only get followed once and so we will never
					// be asked to do this for two different relations.
					List potentialFromKeys = new ArrayList();
					potentialFromKeys.add(fromKey);

					for (Iterator j = mergedTables.iterator(); j.hasNext();) {
						Table table = (Table) j.next();
						for (Iterator k = table.getKeys().iterator(); k
								.hasNext();) {
							Key key = (Key) k.next();
							for (Iterator r = key.getRelations().iterator(); r
									.hasNext();) {
								Relation rel = (Relation) r.next();
								Key targetKey = rel.getOtherKey(key);
								if (targetKey.getColumns().equals(
										fromKey.getColumns())
										&& !potentialFromKeys.contains(key))
									potentialFromKeys.add(key);
							}
						}
					}

					// Loop over the candidates till we find one that matches.
					for (Iterator j = potentialFromKeys.iterator(); j.hasNext()
							&& fromKeyDSColumns.get(0) == null;) {
						Key candidateFromKey = (Key) j.next();

						// Do the key translation loop.
						for (Iterator i = dsTable.getColumns().iterator(); i
								.hasNext();) {
							DataSetColumn dsCol = (DataSetColumn) i.next();

							// When searching for ds cols for a particular real
							// col
							// (for translating real relations+keys), just take
							// first ds col found that mentions it, even if
							// there
							// are several matches. This is because a relation
							// can
							// only be followed once, so it doesn't matter about
							// the extra copies, as they won't have any
							// relations
							// followed off them - only the first one will.
							if (dsCol instanceof WrappedColumn) {
								Column unwrappedCol = ((WrappedColumn) dsCol)
										.getWrappedColumn();

								// We want this column if it matches the fromKey
								// column.
								if (candidateFromKey.getColumns().contains(
										unwrappedCol))
									fromKeyDSColumns.set(
											candidateFromKey.getColumns()
													.indexOf(unwrappedCol),
											dsCol);
							}
						}
					}
				}

				// Identify cols to include from this table.
				List dsColumns = new ArrayList();

				// Now do the loop to find them.
				for (Iterator i = dsTable.getColumns().iterator(); i.hasNext();) {
					DataSetColumn dsCol = (DataSetColumn) i.next();
					// Only select non-masked columns for adding to the
					// table this time round.
					if (!ds.getMaskedDataSetColumns().contains(dsCol)) {

						// Add it if it is a schema name column and this is
						// the first table.
						if (realTable == startTable
								&& (dsCol instanceof SchemaNameColumn))
							dsColumns.add(dsCol);

						// Add it if it is a concat column on this relation.
						else if ((dsCol instanceof ConcatRelationColumn)
								&& (((ConcatRelationColumn) dsCol)
										.getUnderlyingRelation()
										.equals(relation)))
							dsColumns.add(dsCol);

						// Add it if it is a wrapped column on this table, or
						// is a wrapped column on the parent table if this is
						// a dimension or subclass.
						else if (dsCol instanceof WrappedColumn) {
							Table unwrappedColTbl = ((WrappedColumn) dsCol)
									.getWrappedColumn().getTable();
							if (unwrappedColTbl.equals(realTable)
									|| (initialRelation != null && unwrappedColTbl
											.equals(initialRelation.getOneKey()
													.getTable())))
								dsColumns.add(dsCol);
						}
					}
				}

				// If fromKey not null, merge temp using it and toKey,
				// else create temp. Pass in the list of columns we want
				// from this step as a parameter. Pass in the schema name.
				// The Action will perform any concat columns required.
				// Also pass in the partition dataset column and value.
				MCAction tableAction = null;
				if (fromKey == null)
					tableAction = new CreateTable(tempTableName, realTable,
							dsColumns, schemaName, partitionColumn,
							partitionValue);
				else
					tableAction = new MergeTable(tempTableName, realTable,
							dsColumns, schemaName, partitionColumn,
							partitionValue, fromKeyDSColumns, toKey
									.getColumns(), relation.isOptional());

				// Add this action to the graph.
				actionGraph.addAction(tableAction);

				// Make this action dependent on the last action performed
				// (if there was one).
				// Set this action as the last action performed.
				if (lastAction != null)
					tableAction.addParent(lastAction);
				lastAction = tableAction;

				// Add new relations to queue if not already in queue. We
				// only need bother with relations that are in the
				// underlying set of the dataset table in question.
				for (Iterator i = dsTable.getUnderlyingRelations().iterator(); i
						.hasNext();) {
					Relation nextRel = (Relation) i.next();
					// Skip the relation that led us here in the first place.
					if (nextRel.equals(initialRelation))
						continue;
					// Only add relations that we haven't seen before, and
					// that have one end attached to the current real table.
					if (relationQueue.contains(nextRel)
							|| !(nextRel.getFirstKey().getTable().equals(
									realTable) || nextRel.getSecondKey()
									.getTable().equals(realTable)))
						continue;
					else
						relationQueue.add(nextRel);
				}

				// Remember which table we just came from.
				mergedTables.add(realTable);
			} while (relQueuePos < relationQueue.size());

			// Return last action performed.
			return lastAction;
		}

		private String createFinalName(DataSetTable dsTable, Schema schema,
				Object parentParentPartitionValue, Object parentPartitionValue,
				Object partitionValue) {

			// TODO - come up with a better naming scheme
			// Currently the name is:
			// datasetname__\
			// schema_parentparentpartitionvalue_parentpartitionvalue__\
			// table_partitionvalue__\
			// type

			StringBuffer name = new StringBuffer();

			// Dataset name.
			name.append(this.ds.getName());

			// Schema, parent parent partition, and parent partition.
			name.append(BuilderBundle.getString("tablenameSep"));
			name.append(schema.getName());
			if (parentParentPartitionValue != GenericConstructorRunnable.DUMMY_KEY) {
				name.append(BuilderBundle.getString("tablenameSubSep"));
				name.append(parentParentPartitionValue.toString());
			}
			if (parentPartitionValue != GenericConstructorRunnable.DUMMY_KEY) {
				name.append(BuilderBundle.getString("tablenameSubSep"));
				name.append(parentPartitionValue.toString());
			}

			// Table name and partition.
			name.append(BuilderBundle.getString("tablenameSep"));
			name.append(dsTable.getName());
			if (partitionValue != GenericConstructorRunnable.DUMMY_KEY) {
				name.append(BuilderBundle.getString("tablenameSubSep"));
				name.append(partitionValue.toString());
			}

			// Type stuff.
			name.append(BuilderBundle.getString("tablenameSep"));
			DataSetTableType type = dsTable.getType();
			if (type.equals(DataSetTableType.MAIN))
				name.append(BuilderBundle.getString("mainSuffix"));
			else if (type.equals(DataSetTableType.MAIN_SUBCLASS))
				name.append(BuilderBundle.getString("subclassSuffix"));
			else if (type.equals(DataSetTableType.DIMENSION))
				name.append(BuilderBundle.getString("dimensionSuffix"));
			return name.toString();
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
	}

	/**
	 * Represents the various tasks in the mart construction process and how
	 * they should fit together.
	 */
	public class MCActionGraph {
		private Collection actions = new HashSet();

		/**
		 * Adds an action to the graph.
		 * 
		 * @param action
		 *            the action to add.
		 */
		public void addAction(MCAction action) {
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
				MCAction action = (MCAction) i.next();
				if (action.depth == depth)
					matches.add(action);
			}
			return matches;
		}
	}

	/**
	 * Represents one task in the grand scheme of constructing a mart.
	 * Implementations of this abstract class will provide specific methods for
	 * working with the various different stages of mart construction.
	 */
	public abstract class MCAction {
		private Set parents;

		private Set children;

		private int depth;

		private int sequence;

		private static int nextSequence = 0;

		private static final String nextSequenceLock = "__SEQ_LOCK";

		/**
		 * Sets up a node.
		 */
		public MCAction() {
			this.depth = 0;
			synchronized (nextSequenceLock) {
				this.sequence = nextSequence++;
			}
			this.children = new HashSet();
			this.parents = new HashSet();
		}

		/**
		 * Adds a child to this node. The child will have this node added as a
		 * parent.
		 * 
		 * @param child
		 *            the child to add to this node.
		 */
		public void addChild(MCAction child) {
			this.children.add(child);
			child.parents.add(this);
			child.ensureDepth(this.depth + 1);
		}

		/**
		 * Adds a parent to this node. The parent will have this node added as a
		 * child.
		 * 
		 * @param parent
		 *            the parent to add to this node.
		 */
		public void addParent(MCAction parent) {
			this.parents.add(parent);
			parent.children.add(this);
			this.ensureDepth(parent.depth + 1);
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
				MCAction child = (MCAction) i.next();
				child.ensureDepth(this.depth + 1);
			}
		}

		/**
		 * Override this method to produce a message describing what this node
		 * of the graph will do.
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
				return (obj != null && (obj instanceof MCAction) && this.sequence == ((MCAction) obj).sequence);
		}
	}

	/**
	 * This action drops a table.
	 */
	public class DropTable extends MCAction {
		/**
		 * The table name to drop.
		 */
		public final String tableName;

		/**
		 * Constructs an action which represents the dropping of a table.
		 * 
		 * @param tableName
		 *            the name of the table to drop.
		 */
		public DropTable(String tableName) {
			super();
			this.tableName = tableName;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcDropTable", this.tableName);
		}
	}

	/**
	 * This action renames a table.
	 */
	public class RenameTable extends MCAction {
		/**
		 * The existing name of the table.
		 */
		public final String oldName;

		/**
		 * The new name for the table.
		 */
		public final String newName;

		/**
		 * Constructs an action which represents the renaming of a table.
		 * 
		 * @param oldName
		 *            the existing table name.
		 * @param newName
		 *            the new name to give it.
		 */
		public RenameTable(String oldName, String newName) {
			super();
			this.oldName = oldName;
			this.newName = newName;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcRenameTable", new String[] {
					this.oldName, this.newName });
		}
	}

	/**
	 * This action creates a new table by performing a union on a number of
	 * other tables.
	 */
	public class UnionTables extends MCAction {
		/**
		 * The name to give the new table resulting from the union.
		 */
		public final String tableName;

		/**
		 * The collection of table names to involve in the union.
		 */
		public final Collection tableNames;

		/**
		 * Constructs an action which represents the performing of a union over
		 * a number of tables.
		 * 
		 * @param tableName
		 *            the name of the table to create.
		 * @param tableNames
		 *            the names of the tables to use in the union.
		 */
		public UnionTables(String tableName, Collection tableNames) {
			super();
			this.tableName = tableName;
			this.tableNames = tableNames;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcUnionTables", new String[] {
					this.tableName, "" + this.tableNames });
		}
	}

	/**
	 * This action represents the creation of a primary key.
	 */
	public class CreatePK extends MCAction {
		/**
		 * The name of the table to create the key over.
		 */
		public final String tableName;

		/**
		 * The list of dataset columns to include in the key.
		 */
		public final List dsColumns;

		/**
		 * Constructs an action which represents the creation of a primary key.
		 * 
		 * @param tableName
		 *            the name fo the table to create the key for.
		 * @param dsColumns
		 *            the dataset columns, in order, which represent the colums
		 *            to include in the key.
		 */
		public CreatePK(String tableName, List dsColumns) {
			super();
			this.tableName = tableName;
			this.dsColumns = dsColumns;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcCreatePK", new String[] {
					this.tableName, "" + this.dsColumns });
		}
	}

	/**
	 * This action represents the creation of a foreign key.
	 */
	public class CreateFK extends MCAction {
		/**
		 * The table to create the foreign key on.
		 */
		public final String tableName;

		/**
		 * The dataset columns (in order) representing the foreign key columns.
		 */
		public final List dsColumns;

		/**
		 * The foreign key refers to a primary key, which lives in this table.
		 */
		public final String parentTableName;

		/**
		 * The dataset columns representing the columns in the primary key that
		 * this foreign key refers to.
		 */
		public final List parentDSColumns;

		/**
		 * Creates an action that represents the creation of a foreign key.
		 * 
		 * @param tableName
		 *            the table the foreign key is to be created on.
		 * @param dsColumns
		 *            the dataset columns, in order, to create the key with.
		 * @param parentTableName
		 *            the table that owns the primary key to which the foreign
		 *            key refers.
		 * @param parentDSColumns
		 *            the dataset columns, in order, of the primary key that the
		 *            foreign key refers to.
		 */
		public CreateFK(String tableName, List dsColumns,
				String parentTableName, List parentDSColumns) {
			super();
			this.tableName = tableName;
			this.dsColumns = dsColumns;
			this.parentTableName = parentTableName;
			this.parentDSColumns = parentDSColumns;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcCreateFK", new String[] {
					this.tableName, "" + this.dsColumns, this.parentTableName,
					"" + this.parentDSColumns });
		}
	}

	/**
	 * Represents the creation of a new table.
	 */
	public class CreateTable extends MCAction {
		/**
		 * The name of the table to create.
		 */
		public final String tableName;

		/**
		 * The table from which columns should be selected to create this table.
		 */
		public final Table realTable;

		/**
		 * The list of dataset columns that this table should include.
		 */
		public final List dsColumns;

		/**
		 * The name of the schema in which the table to select data from lives.
		 */
		public final String schemaName;

		/**
		 * The column this table should use when restricting data for the
		 * purposes of creating a single partition. If null, then no restriction
		 * is required.
		 */
		public final DataSetColumn partitionColumn;

		/**
		 * The value to restrict by when using a partition column
		 */
		public final Object partitionValue;

		/**
		 * Creates an action that represents the creation of a new table.
		 * 
		 * @param tableName
		 *            the name of the table to create.
		 * @param realTable
		 *            the table it is based on.
		 * @param dsColumns
		 *            the columns to include.
		 * @param schemaName
		 *            the name of the schema in which the source table to select
		 *            from lives.
		 * @param partitionColumn
		 *            the column to restrict on for partitioning, or null if not
		 *            required.
		 * @param partitionValue
		 *            the value to restrict the partition column by.
		 */
		public CreateTable(String tableName, Table realTable, List dsColumns,
				String schemaName, DataSetColumn partitionColumn,
				Object partitionValue) {
			super();
			this.tableName = tableName;
			this.realTable = realTable;
			this.dsColumns = dsColumns;
			this.schemaName = schemaName;
			this.partitionColumn = partitionColumn;
			this.partitionValue = partitionValue;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcCreateTable", new String[] {
					this.tableName, "" + this.realTable, "" + this.dsColumns,
					this.schemaName, "" + this.partitionColumn,
					"" + this.partitionValue });
		}
	}

	/**
	 * Represents the merging of another table into an existing table by joining
	 * keys. May or may not also create and drop indexes.
	 */
	public class MergeTable extends CreateTable {
		/**
		 * The columns to use in the left-hand-side of the join.
		 */
		public final List fromDSColumns;

		/**
		 * The columns to use in the right-hand-side of the join.
		 */
		public final List toRealColumns;

		/**
		 * <tt>true</tt> if the join should be a left-join, <tt>false</tt>
		 * if it should be a natural-join.
		 */
		public final boolean leftJoin;

		/**
		 * Creates an action that represents the merging of a table with an
		 * existing one.
		 * 
		 * @param tableName
		 *            the name of the table to create.
		 * @param realTable
		 *            the table it is based on.
		 * @param dsColumns
		 *            the columns to include.
		 * @param schemaName
		 *            the name of the schema in which the source table to select
		 *            from lives.
		 * @param partitionColumn
		 *            the column to restrict on for partitioning, or null if not
		 *            required.
		 * @param partitionValue
		 *            the value to restrict the partition column by.
		 * @param fromDSColumns
		 *            the columns to use in the existing table on the
		 *            left-hand-side of the join.
		 * @param toRealColumns
		 *            the columns to use in the realTable on the right-hand-side
		 *            of the join.
		 * @param leftJoin
		 *            <tt>true</tt> if the join should be a left-join,
		 *            <tt>false</tt> if it should be a natural-join.
		 */
		public MergeTable(String tableName, Table realTable, List dsColumns,
				String schemaName, DataSetColumn partitionColumn,
				Object partitionValue, List fromDSColumns, List toRealColumns,
				boolean leftJoin) {
			super(tableName, realTable, dsColumns, schemaName, partitionColumn,
					partitionValue);
			this.fromDSColumns = fromDSColumns;
			this.toRealColumns = toRealColumns;
			this.leftJoin = leftJoin;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcMergeTable", new String[] {
					this.tableName, "" + this.realTable, "" + this.dsColumns,
					this.schemaName, "" + this.partitionColumn,
					"" + this.partitionValue, "" + this.fromDSColumns,
					"" + this.toRealColumns, "" + this.leftJoin });
		}
	}

	/**
	 * This action represents the creation of a new table based on an existing
	 * table joined (restricted) to some other table which provides no extra
	 * columns, just a restrictive where clause. Use it to narrow dimension
	 * tables or subclass tables after creation.
	 */
	public class RestrictTable extends MCAction {
		/**
		 * The name of the new table to create.
		 */
		public final String newTableName;

		/**
		 * The name of the table to copy and restrict.
		 */
		public final String oldTableName;

		/**
		 * The name of the table that is providing the restrictive where clause.
		 */
		public final String parentTableName;

		/**
		 * The primary key of the restrictive table.
		 */
		public final List parentPKCols;

		/**
		 * The foreign key of the table to be restricted which refers to the
		 * primary key of the restrictive table.
		 */
		public final List childFKCols;

		/**
		 * Creates an action which represents the restriction of a table.
		 * 
		 * @param newTableName
		 *            the name of the new table to create.
		 * @param oldTableName
		 *            the name of the table to copy and restrict.
		 * @param parentTableName
		 *            the name of the table that is providing the restrictive
		 *            where clause.
		 * @param parentPKCols
		 *            the primary key of the restrictive table.
		 * @param childFKCols
		 *            the foreign key of the table to be restricted which refers
		 *            to the primary key of the restrictive table.
		 */
		public RestrictTable(String newTableName, String oldTableName,
				String parentTableName, List parentPKCols, List childFKCols) {
			super();
			this.newTableName = newTableName;
			this.oldTableName = oldTableName;
			this.parentTableName = parentTableName;
			this.parentPKCols = parentPKCols;
			this.childFKCols = childFKCols;
		}

		public String getStatusMessage() {
			return BuilderBundle.getString("mcRestrictTable", new String[] {
					this.newTableName, this.oldTableName, this.parentTableName,
					"" + this.parentPKCols, "" + this.childFKCols });
		}
	}
}
