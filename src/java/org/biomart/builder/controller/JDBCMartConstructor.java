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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataLink.JDBCDataLink;
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
import org.biomart.builder.model.MartConstructor.GenericMartConstructor;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This implementation of the {@link MartConstructor} interface connects to a
 * JDBC data source in order to create a mart. It has options to output DDL to
 * file instead of running it, to run DDL directly against the database, or to
 * use JDBC to fetch/retrieve data between two databases.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 15th June 2006
 * @since 0.1
 */
public class JDBCMartConstructor extends GenericMartConstructor implements
		JDBCDataLink {
	private static final String DUMMY_KEY = "__jdbc_mc__dummy_map_key__";

	private Connection connection;

	private String driverClassName;

	private File driverClassLocation;

	private String url;

	private String username;

	private String password;

	private File outputDDLZipFile;

	private JDBCMartConstructorType type;

	/**
	 * <p>
	 * Establishes a JDBC connection from the information provided, and
	 * remembers it.
	 * 
	 * @param driverClassLocation
	 *            the location (filesystem path) of the class to load the JDBC
	 *            driver from. This will usually be a <tt>.jar</tt> file, or a
	 *            folder containing a Java <tt>.class</tt> file. If the path
	 *            does not exist, is null, or does not contain the class
	 *            specified, then the default system class loader is used
	 *            instead.
	 * @param driverClassName
	 *            the class name of the JDBC driver, eg.
	 *            <tt>com.mysql.jdbc.Driver</tt>.
	 * @param url
	 *            the JDBC URL of the database server to connect to.
	 * @param username
	 *            the username to connect as.
	 * @param password
	 *            the password to connect as. Defaults to the empty string if
	 *            null.
	 * @param name
	 *            the name to give this mart constructor after it has been
	 *            created.
	 */
	public JDBCMartConstructor(File driverClassLocation,
			String driverClassName, String url, String username,
			String password, String name, JDBCMartConstructorType type,
			File outputDDLFile) {
		// Call the GenericMartConstructor implementation first, to set up
		// our name.
		super(name);

		// Sensible defaults.
		if (driverClassLocation != null && !driverClassLocation.exists())
			driverClassLocation = null;

		// Remember the settings.
		this.driverClassLocation = driverClassLocation;
		this.driverClassName = driverClassName;
		this.url = url;
		this.username = username;
		this.password = password;
		this.type = type;
		this.outputDDLZipFile = outputDDLFile;
	}

	public boolean test() throws Exception {
		// Establish the JDBC connection. May throw an exception of its own,
		// which is fine, just let it go.
		Connection connection = this.getConnection();
		// If we have no connection, we can't test it!
		if (connection == null)
			return false;

		// Get the metadata.
		DatabaseMetaData dmd = connection.getMetaData();

		// By opening, executing, then closing a DMD query we will test
		// the connection fully without actually having to read anything from
		// it.
		String catalog = connection.getCatalog();
		String schema = dmd.getUserName();
		dmd.getTables(catalog, schema, "%", null).close();

		// If we get here, it worked.
		return true;
	}

	public Connection getConnection() throws SQLException {
		// If we have not connected before, we should attempt to connect now.
		if (this.connection == null) {
			// Start out with no driver at all.
			Class loadedDriverClass = null;

			// If a path was specified for the driver, and that path exists,
			// load the driver from that path.
			if (this.driverClassLocation != null
					&& this.driverClassLocation.exists())
				try {
					ClassLoader classLoader = URLClassLoader
							.newInstance(new URL[] { this.driverClassLocation
									.toURL() });
					loadedDriverClass = classLoader
							.loadClass(this.driverClassName);
				} catch (ClassNotFoundException e) {
					SQLException e2 = new SQLException();
					e2.initCause(e);
					throw e2;
				} catch (MalformedURLException e) {
					throw new MartBuilderInternalError(e);
				}

			// If we failed to load the driver from the custom path, try the
			// system class loader instead.
			if (loadedDriverClass == null)
				try {
					loadedDriverClass = Class.forName(this.driverClassName);
				} catch (ClassNotFoundException e) {
					SQLException e2 = new SQLException();
					e2.initCause(e);
					throw e2;
				}

			// Check it really is an instance of Driver.
			if (!Driver.class.isAssignableFrom(loadedDriverClass))
				throw new ClassCastException(BuilderBundle
						.getString("driverClassNotJDBCDriver"));

			// Connect!
			Properties properties = new Properties();
			properties.setProperty("user", this.username);
			if (this.password != null)
				properties.setProperty("password", this.password);
			this.connection = DriverManager.getConnection(this.url, properties);
		}

		// Return the connection.
		return this.connection;
	}

	public String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public File getDriverClassLocation() {
		return this.driverClassLocation;
	}

	public void setDriverClassLocation(File driverClassLocation) {
		this.driverClassLocation = driverClassLocation;
	}

	public String getJDBCURL() {
		return this.url;
	}

	public void setJDBCURL(String url) {
		this.url = url;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public File getOutputDDLZipFile() {
		return this.outputDDLZipFile;
	}

	public void setOutputDDLZipFile(File outputDDLZipFile) {
		this.outputDDLZipFile = outputDDLZipFile;
	}

	public JDBCMartConstructorType getType() {
		return this.type;
	}

	public void setType(JDBCMartConstructorType type) {
		this.type = type;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * In our case, cohabitation means that the partner link is also a
	 * {@link JDBCDataLink} and that its connection is connected to the same
	 * database server listening on the same port and connected with the same
	 * username.
	 */
	public boolean canCohabit(DataLink partner) {
		// We can't cohabit with non-JDBCDataLink partners.
		if (!(partner instanceof JDBCDataLink))
			return false;
		JDBCDataLink partnerLink = (JDBCDataLink) partner;

		// We can cohabit if the JDBC URLs and usernames are identical.
		return (partnerLink.getJDBCURL().equals(this.getJDBCURL()) && partnerLink
				.getUsername().equals(this.getUsername()));
	}

	public ConstructorRunnable getConstructorRunnable(final DataSet ds) {
		final JDBCMartConstructor constructor = this;
		return new ConstructorRunnable() {
			private String statusMessage = "";

			private double percentComplete = 0.0;

			private boolean cancelled = false;

			private Exception failure = null;

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
				// Work out what kind of helper to use.
				this.statusMessage = BuilderBundle.getString("mcMakingHelper");
				Helper helper;
				if (getType().equals(JDBCMartConstructorType.INTERNAL))
					helper = new InternalHelper(getConnection());
				else if (getType().equals(JDBCMartConstructorType.FILE))
					helper = new FileHelper(getOutputDDLZipFile());
				else if (getType().equals(JDBCMartConstructorType.EXTERNAL))
					throw new ConstructorException(BuilderBundle
							.getString("mcExternalNotImpl"));
				else
					throw new ConstructorException(BuilderBundle
							.getString("unknownJDBCMCType"));

				// Cancelled?
				this.checkCancelled();

				// Set the input and output dialects on the helper for each
				// schema.
				this.statusMessage = BuilderBundle
						.getString("mcGatheringSchemas");

				// Inputs first.
				Set inputSchemas = new HashSet();
				for (Iterator i = ds.getTables().iterator(); i.hasNext();) {
					Table t = (Table) i.next();
					for (Iterator j = t.getColumns().iterator(); j.hasNext();) {
						Column c = (Column) j.next();
						if (c instanceof WrappedColumn)
							inputSchemas.add(((WrappedColumn) c).getTable()
									.getSchema());
					}
				}
				for (Iterator i = inputSchemas.iterator(); i.hasNext();) {
					Schema s = (Schema) i.next();
					helper.setInputDialect(s, Dialect.getDialect(s));
				}

				// Then the output.
				helper.setOutputDialect(Dialect.getDialect(constructor));

				// Cancelled?
				this.checkCancelled();

				// Work out the partition values.
				this.statusMessage = BuilderBundle
						.getString("mcGettingPartitions");
				Map partitionValues = new HashMap();
				for (Iterator i = ds.getTables().iterator(); i.hasNext();) {
					Table t = (Table) i.next();
					List tabCols = new ArrayList(t.getColumns());
					tabCols.retainAll(ds.getPartitionedWrappedColumns());
					if (!tabCols.isEmpty()) {
						// There should be only one column left per table.
						// As we can only partition on wrapped columns,
						// we can assume it will be a wrapped column.
						WrappedColumn col = (WrappedColumn) tabCols.get(0);

						// What kind of partition is this?
						PartitionedColumnType type = ds
								.getPartitionedWrappedColumnType(col);

						// Unique values - one step per value.
						if (type instanceof UniqueValues) {
							List values = helper.listDistinctValues(col
									.getWrappedColumn());
							partitionValues.put(col, values);
						}

						// Single value - one step.
						else if (type instanceof SingleValue) {
							SingleValue sv = (SingleValue) type;
							Object value = sv.getIncludeNull() ? null : sv
									.getValue();
							partitionValues.put(col, Collections
									.singletonList(value));
						}

						// Select values - one step per value.
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
				}

				// Cancelled?
				this.checkCancelled();

				// Create a graph of actions.
				this.statusMessage = BuilderBundle.getString("mcCreatingGraph");
				MCActionGraph actionGraph = new MCActionGraph();

				// Find main table.
				DataSetTable mainTable = null;
				for (Iterator i = ds.getTables().iterator(); i.hasNext()
						&& mainTable == null;) {
					DataSetTable table = (DataSetTable) i.next();
					if (table.getType().equals(DataSetTableType.MAIN))
						mainTable = table;
				}

				// Find schemas to partition by.
				Collection schemas = null;
				if (ds.getPartitionOnSchema()
						&& (mainTable.getSchema() instanceof SchemaGroup))
					schemas = ((SchemaGroup) mainTable.getSchema())
							.getSchemas();
				else
					schemas = Collections.singletonList(mainTable.getSchema());

				// Check not cancelled.
				this.checkCancelled();

				// Repeat for each schema.
				for (Iterator i = schemas.iterator(); i.hasNext();) {
					Schema currSchema = (Schema) i.next();
					// Maintain chained map:
					// ds table ->
					// schema ->
					// parent partition value (optional) ->
					// partition value (optional) ->
					// actual table name
					Map dsTableNameNestedMap = new HashMap();

					// Maintain map:
					// ds table -> last action
					Map dsTableLastActionMap = new HashMap();

					// Process main table.
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
						Relation dimRel = (Relation) k.next();
						DataSetTable dimTableCandidate = (DataSetTable) dimRel
								.getManyKey().getTable();
						this.processTable(currSchema, actionGraph,
								scAndMainDimDependsOn, dimTableCandidate,
								dsTableNameNestedMap, dsTableLastActionMap,
								dimRel, partitionValues, helper);

						// Check not cancelled.
						this.checkCancelled();
					}

					// Process subclass tables.
					for (Iterator j = mainTable.getPrimaryKey().getRelations()
							.iterator(); j.hasNext();) {
						Relation scRel = (Relation) j.next();
						DataSetTable scTableCandidate = (DataSetTable) scRel
								.getManyKey().getTable();
						if (scTableCandidate.getType().equals(
								DataSetTableType.MAIN_SUBCLASS)) {
							// Process subclass table itself.
							this.processTable(currSchema, actionGraph,
									scAndMainDimDependsOn, scTableCandidate,
									dsTableNameNestedMap, dsTableLastActionMap,
									scRel, partitionValues, helper);

							// Check not cancelled.
							this.checkCancelled();

							// Subclass tables and main dimension tables are
							// dependent on last actions of main table.
							MCAction scDimDependsOn = (MCAction) dsTableLastActionMap
									.get(scTableCandidate);

							// Process all dimensions of subclass.
							for (Iterator k = scTableCandidate.getPrimaryKey()
									.getRelations().iterator(); k.hasNext();) {
								Relation dimRel = (Relation) k.next();
								DataSetTable dimTableCandidate = (DataSetTable) dimRel
										.getManyKey().getTable();
								this.processTable(currSchema, actionGraph,
										scDimDependsOn, dimTableCandidate,
										dsTableNameNestedMap,
										dsTableLastActionMap, dimRel,
										partitionValues, helper);

								// Check not cancelled.
								this.checkCancelled();
							}
						}
					}

					// TODO
					// 2. Optimiser nodes. Adding 'has' columns/tables, etc.
					// Optimiser nodes are dependent on last actions of all
					// tables within same schema. Use the dsTableNameNestedMap
					// to discover all the tables that need this doing.
				}

				// Check not cancelled.
				this.checkCancelled();

				// Carry out the action graph node-by-node, incrementing
				// the progress by one step per node.
				double stepPercent = 100.0 / (double) actionGraph.getActions()
						.size();
				helper.startActions();

				// Loop over all depths.
				int depth = 0;
				for (Collection actions = actionGraph.getActionsAtDepth(depth); !actions
						.isEmpty(); depth++) {

					// Loop over all actions at current depth.
					for (Iterator i = actions.iterator(); i.hasNext();) {
						MCAction action = (MCAction) i.next();

						// Execute the action.
						this.statusMessage = action.getStatusMessage();
						helper.executeAction(action, depth);
						this.percentComplete += stepPercent;

						// Check not cancelled.
						this.checkCancelled();
					}
				}

				// All done!
				helper.endActions();
			}

			private void processTable(Schema schema, MCActionGraph actionGraph,
					MCAction firstActionDependsOn, DataSetTable dsTable,
					Map dsTableNameNestedMap, Map dsTableLastActionMap,
					Relation parentRelation, Map partitionValues, Helper helper)
					throws Exception {
				// Work out if we have to do a multi-schema union, or a
				// single-schema select.
				Collection schemas = null;
				if (schema instanceof SchemaGroup)
					schemas = ((SchemaGroup) schema).getSchemas();
				else
					schemas = Collections.singletonList(schema);

				// If table has any column which is partitioned, identify
				// the underlying partitioned table.
				// Otherwise, identify the underlying base table of the
				// dataset table instead.
				WrappedColumn partitionColumn = null;
				for (Iterator i = dsTable.getColumns().iterator(); i.hasNext()
						&& partitionColumn == null;) {
					WrappedColumn c = (WrappedColumn) i.next();
					if (partitionValues.containsKey(c))
						partitionColumn = c;
				}
				Table startTable = null;
				Collection partColValues = null;
				if (partitionColumn != null) {
					startTable = partitionColumn.getWrappedColumn().getTable();
					partColValues = (Collection) partitionValues
							.get(partitionColumn);
				} else {
					startTable = dsTable.getUnderlyingTable();
					partColValues = Collections
							.singleton(JDBCMartConstructor.DUMMY_KEY);
				}

				// Holder for the last action performed.
				// By default, the last action is the first, but this
				// will be overridden by any actual actions.
				MCAction lastAction = firstActionDependsOn;

				// Set up a map to hold the temp table names we make,
				// and the last actions associated with each temp table.
				// The temp table names are keyed by partition value.
				Map tempTableNames = new HashMap();
				List lastActions = new ArrayList();

				// Placeholder for name of final temp table.
				String tempTableName = null;

				// Process table once per partition value.
				for (Iterator j = partColValues.iterator(); j.hasNext();) {
					Object partitionValue = j.next();

					// If schema is schema group, do all this once per schema.
					// If schema is a single schema, do all this only once.
					// Process table once per schema.
					for (Iterator i = schemas.iterator(); i.hasNext();) {
						Schema currSchema = (Schema) i.next();

						// Come up with a new temp table name for this
						// partition.
						tempTableName = helper.getNewTempTableName();
						tempTableNames.put(partitionValue, tempTableName);

						// Create the table for this partition.
						lastActions.add(this.constructTable(currSchema
								.getName(), actionGraph, firstActionDependsOn,
								dsTable, startTable, partitionColumn,
								partitionValue, tempTableName));
					}

					// At end, if dealing with multiple schemas, do a union on
					// the temporary tables to create the final table, one per
					// partition value. Last action is the union action.
					if (schemas.size() > 1) {
						// Come up with a temp table name for the union table.
						tempTableName = helper.getNewTempTableName();

						// TODO
						// Union depends on final actions of each temporary
						// table.
						// Last action is union action.

						// Drop all the old temp tables that were combined in
						// the union stage.
						for (Iterator i = tempTableNames.values().iterator(); i
								.hasNext();) {
							String tableName = (String) i.next();
							// TODO
							// Drop temp table. Don't remember as last action.
							// Each drop action depends on union action.
						}
					}
					// Otherwise, last action is the first (only) action in the
					// last action list.
					else
						lastAction = (MCAction) lastActions.get(0);

					// Maintain a map of parent partition values to temp table
					// names, in case we have to split them here.
					Map segmentTables = new HashMap();

					// If table is dimension or subclass, link it to its parent.
					// This may involve splitting it into segments.
					if (parentRelation != null) {

						// Work out parent PK.
						PrimaryKey parentPK = (PrimaryKey) parentRelation
								.getOneKey();

						// Work out child FK.
						ForeignKey childFK = (ForeignKey) parentRelation
								.getManyKey();

						// Work out parent table from relation.
						DataSetTable parentTable = (DataSetTable) parentPK
								.getTable();

						// Make a list to hold actions that final drop depends
						// on.
						List dropDependsOn = new ArrayList();

						// Use dsTableNameNestedMap to identify parent partition
						// values
						// of parent table, in case parent itself was segmented.
						Map parentVToV = (Map) dsTableNameNestedMap.get(schema);

						// For each parent segment, work out partition values.
						for (Iterator i = parentVToV.keySet().iterator(); i
								.hasNext();) {
							Object parentParentPartitionValue = i.next();
							Map vToName = (Map) parentVToV
									.get(parentParentPartitionValue);
							segmentTables.put(parentParentPartitionValue, new HashMap());

							for (Iterator k = vToName.keySet().iterator(); k
									.hasNext();) {
								Object parentPartitionValue = k.next();
								
								// Come up with a temp table name for the 
								// segment table.
								String segmentTableName = helper.getNewTempTableName();
								
								// TODO
								// (action depends on union action above)
								// createrestrict new temp table by using natural 
								// join with parent.
								
								// TODO
								// (action depends on create temp table action)
								// establish FK to PK relation on parent table.

								// TODO
								// add FK to PK action to dropDependsOn

								// Segments:add parent parent partition value ->
								// parent partition value -> new temp table.
								((Map)segmentTables.get(parentParentPartitionValue)).put(parentPartitionValue, segmentTableName);
							}
						}

						// TODO
						// (action depends on all dropDependsOn)
						// after last parent parent partition, drop original temp
						// table (tempTableName).
					}
					// If this is not a dimension or subclass, then there
					// is only one segment, which we have already made.
					else
						segmentTables.put(JDBCMartConstructor.DUMMY_KEY,
								(new HashMap()).put(
										JDBCMartConstructor.DUMMY_KEY,
										tempTableName));

					// For each segment table, construct the PK and rename to
					// their final name.
					for (Iterator i = segmentTables.keySet().iterator(); i
							.hasNext();) {
						Object parentParentPartitionValue = i.next();
						Map parentPartitionValueMap = (Map) segmentTables
								.get(parentParentPartitionValue);
						
						// Descend into second level of nesting.
						for (Iterator k = parentPartitionValueMap.keySet()
								.iterator(); k.hasNext();) {
							Object parentPartitionValue = k.next();
							String tableName = (String) parentPartitionValueMap
									.get(parentPartitionValue);
							
							// Find PK columns.
							PrimaryKey pk = dsTable.getPrimaryKey();
							if (pk != null) {
								// TODO
								// (action depends on drop action above, if DIM
								// or SC, or union action if not)
								// create PK.
							}

							// Make the final name for this table.
							String finalName = this.createFinalName(dsTable,
									schema, parentParentPartitionValue,
									parentPartitionValue, partitionValue);

							// TODO
							// (action depends on PK action, or union if null
							// PK)
							// Rename segment table to final name.
							// (tableName -> finalName)
							// (last rename action is the last action to pass to
							// next stage - don't care about the others)

							// Add final name to nested name map for this table.
							// ds table ->
							// schema ->
							// parent partition value (optional) ->
							// partition value (optional) ->
							// actual table name.
							Map schemaToParentV = (Map) dsTableNameNestedMap
									.get(dsTable);
							if (schemaToParentV == null) {
								schemaToParentV = new HashMap();
								dsTableNameNestedMap.put(dsTable,
										schemaToParentV);
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
								parentVToV
										.put(parentPartitionValue, parentVToV);
							}
							vToName.put(partitionValue, finalName);
						}
					}
				}

				// Last action for table is rename for last segment of
				// last partition value.
				dsTableLastActionMap.put(dsTable, lastAction);
			}

			private MCAction constructTable(String schemaName,
					MCActionGraph actionGraph, MCAction firstActionDependsOn,
					DataSetTable dsTable, Table startTable,
					DataSetColumn partitionColumn, Object partitionValue,
					String tempTableName)
					throws Exception {
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
				// has a chance of being filled.
				Table lastTable = startTable;
				do {
					// Get next relation (or if queue empty, use null)
					// Also work out which table we are merging.
					Relation relation = null;
					Table realTable = null;
					Key fromKey = null;
					Key toKey = null;
					if (!relationQueue.isEmpty()) {
						relation = (Relation) relationQueue.get(relQueuePos++);
						fromKey = relation.getFirstKey().getTable().equals(
								lastTable) ? relation.getSecondKey() : relation
								.getFirstKey();
						toKey = relation.getOtherKey(fromKey);
						realTable = toKey.getTable();
					} else {
						realTable = startTable;
					}

					// Identify cols to include from this table.
					// Whilst we're at it, translate fromKey into related
					// dataset columns.
					List dsColumns = new ArrayList();
					List fromKeyDSColumns = new ArrayList(fromKey.getColumns()
							.size());

					// Populate the from key translation with nulls to prevent
					// index-out-of-bounds exceptions later.
					for (int i = 0; i < fromKey.getColumns().size(); i++)
						fromKeyDSColumns.add(null);

					// Now do the loop for look up and translation.
					for (Iterator i = dsTable.getColumns().iterator(); i
							.hasNext();) {
						DataSetColumn dsCol = (DataSetColumn) i.next();

						// Do the translation of the fromKey first.
						// When searching for ds cols for a particular real col
						// (for translating real relations+keys), just take
						// first ds col found that mentions it, even if there
						// are several matches. This is because a relation can
						// only be followed once, so it doesn't matter about
						// the extra copies, as they won't have any relations
						// followed off them - only the first one will.
						if (dsCol instanceof WrappedColumn) {
							Column unwrappedCol = ((WrappedColumn) dsCol)
									.getWrappedColumn();
							if (fromKey.getColumns().contains(unwrappedCol))
								fromKeyDSColumns.set(fromKey.getColumns()
										.indexOf(unwrappedCol), dsCol);
						}

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

							// Add it if it is a wrapped column on this table.
							else if ((dsCol instanceof WrappedColumn)
									&& (((WrappedColumn) dsCol)
											.getWrappedColumn().getTable()
											.equals(realTable)))
								dsColumns.add(dsCol);
						}
					}

					// If fromKey and toKey not null, merge temp using them,
					// else create temp. Pass in the list of columns we want
					// from this step as a parameter. Pass in the schema name.
					// The Action will perform any concat columns required.
					// Also pass in the partition dataset column and value.
					if (fromKey == null) {
						// TODO
						// create temp action
					} else {
						// TODO
						// merge temp
					}

					// TODO
					// Add this action to the graph.

					// TODO
					// Make this action dependent on the last action performed.
					// Set this action as the last action performed.

					// Add new relations to queue if not already in queue.
					for (Iterator i = realTable.getRelations().iterator(); i
							.hasNext();) {
						Relation nextRel = (Relation) i.next();
						if (!relationQueue.contains(nextRel))
							relationQueue.add(nextRel);
					}

					// Remember which table we just came from.
					lastTable = realTable;
				} while (relQueuePos < relationQueue.size());

				// Return last action performed.
				return lastAction;
			}

			private String createFinalName(DataSetTable dsTable, Schema schema,
					Object parentParentPartitionValue,
					Object parentPartitionValue, Object partitionValue) {
				
				// TODO - come up with a better naming scheme
				// Currently the name is:
				//  schema_parentparentpartition_parentpartition__\
				//  table_partition__\
				//  type
				
				StringBuffer name = new StringBuffer();
				
				// Schema, parent parent partition, and parent partition.
				name.append(schema.getName());
				if (parentParentPartitionValue != null) {
					name.append("_");
					name.append(parentParentPartitionValue.toString());
				}
				if (parentPartitionValue != null) {
					name.append("_");
					name.append(parentPartitionValue.toString());
				}
				
				// Table name and partition.
				name.append("__");
				name.append(dsTable.getName());
				if (partitionValue != null) {
					name.append("_");
					name.append(partitionValue.toString());
				}
				
				// Type stuff.
				name.append("__");
				DataSetTableType type = dsTable.getType();
				if (type.equals(DataSetTableType.MAIN))
					name.append("main");
				else if (type.equals(DataSetTableType.MAIN_SUBCLASS))
					name.append("main");
				else if (type.equals(DataSetTableType.DIMENSION))
					name.append("dm");
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
		};
	}

	/**
	 * Helper provides methods to define large steps. Helper implementations
	 * perform those steps.
	 */
	public interface Helper {
		/**
		 * Sets the dialect to use on tables which come from the given schema.
		 * 
		 * @param schema
		 *            the schema to use the dialect on.
		 * @param dialect
		 *            the dialect to use for tables in that schema.
		 */
		public void setInputDialect(Schema schema, Dialect dialect);

		/**
		 * Sets the dialect to use to create the output tables with.
		 * 
		 * @param dialect
		 *            the dialect to use when creating output tables.
		 */
		public void setOutputDialect(Dialect dialect);

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
	 * DDLHelper generates DDL statements for each step.
	 */
	public abstract static class DDLHelper implements Helper {
		private Dialect dialect;

		private int tempTableSeq = 0;

		public void setInputDialect(Schema schema, Dialect dialect) {
			// Ignored as is not required - the dialect is the
			// same as the output dialect.
		}

		public void setOutputDialect(Dialect dialect) {
			this.dialect = dialect;
		}

		public String getNewTempTableName() {
			return "__JDBCMART_TEMP__" + this.tempTableSeq++;
		}

		public List listDistinctValues(Column col) throws SQLException {
			return this.dialect.executeSelectDistinct(col);
		}

		public String getCommandForAction(MCAction action) throws Exception {
			// TODO
			// Use the dialect to translate actions into words. We can't
			// use parameterised queries here unfortunately as they may
			// be written to file later.
			return "HELLO WORLD FROM ACTION#" + action.getSequence();
		}
	}

	/**
	 * InternalHelper extends DDLHelper, executes statements.
	 */
	public static class InternalHelper extends DDLHelper {
		private Connection outputConn;

		/**
		 * Constructs a helper which will execute all DDL across the specified
		 * connection.
		 * 
		 * @param outputConn
		 *            the connection to which DDL will be sent for execution.
		 */
		public InternalHelper(Connection outputConn) {
			super();
			this.outputConn = outputConn;
		}

		public void startActions() throws Exception {
			// We don't care.
		}

		public void executeAction(MCAction action, int level) throws Exception {
			// Executes the given action.
			String cmd = this.getCommandForAction(action);
			// As we're working internally, the input connection is the
			// same for all schemas, and is the same as the output one.
			Connection inputConn = this.outputConn;
			// Prepare and execute the command.
			inputConn.prepareStatement(cmd).execute();
		}

		public void endActions() throws Exception {
			// We don't care.
		}
	}

	/**
	 * FileHelper extends DDLHelper, saves statements. Statements are saved in
	 * folders. Folder 1 must be finished before folder 2, but files within
	 * folder 1 can be executed in any order. And so on.
	 */
	public static class FileHelper extends DDLHelper {
		private File outputFile;

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		/**
		 * Constructs a helper which will output all DDL into a structured
		 * directory tree inside the given zip file.
		 * 
		 * @param outputZippedDDLFile
		 *            the zip file to write the DDL structured tree into.
		 */
		public FileHelper(File outputZippedDDLFile) {
			super();
			this.outputFile = outputZippedDDLFile;
		}

		public void startActions() throws Exception {
			// Open the zip stream.
			this.outputFileStream = new FileOutputStream(this.outputFile);
			this.outputZipStream = new ZipOutputStream(this.outputFileStream);
		}

		public void executeAction(MCAction action, int level) throws Exception {
			// Writes the given action to file.
			// Put the next entry to the zip file.
			ZipEntry entry = new ZipEntry(level + "/" + action.getSequence()
					+ ".sql");
			this.outputZipStream.putNextEntry(entry);
			// Write the data.
			String cmd = this.getCommandForAction(action);
			this.outputZipStream.write(cmd.getBytes());
			// Close the entry.
			this.outputZipStream.closeEntry();
		}

		public void endActions() throws Exception {
			// Close the zip stream. Will also close the
			// file output stream by default.
			this.outputZipStream.close();
		}
	}

	/**
	 * Dialect provides methods which generate atomic DDL or SQL statements.
	 * Each implementation should register itself with
	 * {@link #registerDialect(Dialect)} in a static initializer so that it can
	 * be used.
	 */
	public abstract static class Dialect {
		private static final Set dialects = new HashSet();

		/**
		 * Registers a dialect for use with this system. Should be called from
		 * the static initializer of every implementing class.
		 * 
		 * @param dialect
		 *            the dialect to register.
		 */
		public static void registerDialect(Dialect dialect) {
			dialects.add(dialect);
		}

		/**
		 * Test to see whether this particular dialect implementation can
		 * understand the data link given, ie. it knows how to interact with it
		 * and speak the appropriate version of SQL or DDL.
		 * 
		 * @param dataLink
		 *            the data link to test compatibility with.
		 * @return <tt>true</tt> if it understands it, <tt>false</tt> if
		 *         not.
		 * @throws ConstructorException
		 *             if there was any problem trying to establish whether or
		 *             not the data link is compatible with this dialect.
		 */
		public abstract boolean understandsDataLink(DataLink dataLink)
				throws ConstructorException;

		/**
		 * Work out what kind of dialect to use for the given data link.
		 * 
		 * @param dataLink
		 *            the data link to work out the dialect for.
		 * @return the appropriate Dialect.
		 * @throws ConstructorException
		 *             if there is no appropriate dialect.
		 */
		public static Dialect getDialect(DataLink dataLink)
				throws ConstructorException {
			for (Iterator i = dialects.iterator(); i.hasNext();) {
				Dialect d = (Dialect) i.next();
				if (d.understandsDataLink(dataLink))
					return d;
			}
			throw new ConstructorException(BuilderBundle
					.getString("mcUnknownDataLink"));
		}

		/**
		 * Gets the distinct values in the given column. This must be a real
		 * column, not an instance of {@link DataSetColumn}. This method
		 * actually performs the select and returns the results as a list.
		 * 
		 * @param col
		 *            the column to get the distinct values from.
		 * @return the distinct values in the column.
		 * @throws SQLException
		 *             in case of problems.
		 */
		public abstract List executeSelectDistinct(Column col)
				throws SQLException;

		/**
		 * Escapes a string so that it may safely be contained as a quoted
		 * literal within a statement. The quotes themselves should not be added
		 * by this method.
		 * 
		 * @param string
		 *            the string to escape.
		 * @return the escaped string.
		 */
		public abstract String escapeQuotedString(String string);
	}

	/**
	 * Represents the name of various methods of constructing a JDBC mart.
	 */
	public static class JDBCMartConstructorType implements Comparable {
		private static final Map singletons = new HashMap();

		private final String name;

		/**
		 * Use this constant to refer to in-database DDL execution.
		 */
		public static final JDBCMartConstructorType INTERNAL = JDBCMartConstructorType
				.get("INTERNAL");

		/**
		 * Use this constant to refer to creation via JDBC import/export.
		 */
		public static final JDBCMartConstructorType EXTERNAL = JDBCMartConstructorType
				.get("EXTERNAL");

		/**
		 * Use this constant to refer to generation of DDL in a file.
		 */
		public static final JDBCMartConstructorType FILE = JDBCMartConstructorType
				.get("FILE");

		/**
		 * The static factory method creates and returns a type with the given
		 * name. It ensures the object returned is a singleton. Note that the
		 * names of type objects are case-insensitive.
		 * 
		 * @param name
		 *            the name of the type object.
		 * @return the type object.
		 */
		public static JDBCMartConstructorType get(String name) {
			// Convert to upper case.
			name = name.toUpperCase();

			// Do we already have this one?
			// If so, then return it.
			if (singletons.containsKey(name))
				return (JDBCMartConstructorType) singletons.get(name);

			// Otherwise, create it, remember it.
			JDBCMartConstructorType t = new JDBCMartConstructorType(name);
			singletons.put(name, t);

			// Return it.
			return t;
		}

		/**
		 * The private constructor defines the name this object will display
		 * when printed.
		 * 
		 * @param name
		 *            the name of the mart constructor type.
		 */
		private JDBCMartConstructorType(String name) {
			this.name = name;
		}

		/**
		 * Displays the name of this constructor type object.
		 * 
		 * @return the name of this constructor type object.
		 */
		public String getName() {
			return this.name;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			JDBCMartConstructorType t = (JDBCMartConstructorType) o;
			return this.toString().compareTo(t.toString());
		}

		public boolean equals(Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}

	// TODO - createTable(temptblname, targettbl, targetDSCols, schname, partDScols,
	// partvalue)
	// TODO - mergeTable extends createTable plus (fromDSCols, toCols,
	// left/natural join)
	// TODO - createRestrictedTable(targettblname, temptbl, parenttbl, tempFK, parentPK)
	// TODO - unionTables(tablelist)
	// TODO - dropTable(table)
	// TODO - renameTable(old, new)
	// TODO - createPK(table, dscols)
	// TODO - createFK(table, dscols, parenttable, parentdscols)
}
