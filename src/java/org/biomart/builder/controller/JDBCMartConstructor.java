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
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.DataSet.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.DataSet.PartitionedColumnType.ValueCollection;
import org.biomart.builder.model.MartConstructor.GenericMartConstructor;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This implementation of the {@link MartConstructor} interface connects to a
 * JDBC data source in order to create a mart. It has options to output DDL to
 * file instead of running it, to run DDL directly against the database, or to
 * use JDBC to fetch/retrieve data between two databases.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 12th June 2006
 * @since 0.1
 */
public class JDBCMartConstructor extends GenericMartConstructor implements
		JDBCDataLink {
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

				// TODO - add each of these steps as MCAction objects to graph.

				/**
				 
				// For each schema partition...

				// For each table...

				// Find partition values for partition column on
				// table, if any.

				// For each partition value, which may be null if not
				// partitioned, call A once for each value, specifying the
				// dataset table's underlying table.

				// (A)
				// (run in parallel for each partition value)
				// drop temp
				// if value not null
				// call step B on table with partition column,
				// specifying current partition value
				// else
				// call step B on table, specifying partition value
				// of null.
				// add schema name column if source is schema group
				// call step C on table

				// (B)
				// if temp exists
				// merge table with temp using left join with given
				// relation. create indexes on temp table relation
				// key first, and drop them after.
				// else
				// create temp as select * from table, restrict
				// by partition column value if on this table and
				// value is not null.
				// add all relations from merged table to queue and
				// call step B on each one specifying partition value
				// and relation.

				// (C)
				// rename temp table to final name
				// call step D

				// (D)
				// create primary key on temp table
				// call step E if dimension/subclass, step F if main.

				// (E)
				// (depends on parent table for this partition value
				// being complete)
				// if parent table is also partitioned, split this
				// table into one chunk per parent table partition.
				// delete records in each chunk that foreign key to
				// parent table does not allow
				// create foreign key to parent table.
				// if subclass table, call step F.

				// (F)
				// (depends on all tables for this partition value being
				// complete)
				// Run the optimiser nodes.
				
				*/

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
	public abstract class DDLHelper implements Helper {
		private Dialect dialect;

		public void setInputDialect(Schema schema, Dialect dialect) {
			// Ignored as is not required - the dialect is the
			// same as the output dialect.
		}

		public void setOutputDialect(Dialect dialect) {
			this.dialect = dialect;
		}

		public List listDistinctValues(Column col) throws SQLException {
			return this.dialect.executeSelectDistinct(col);
		}

		public String getCommandForAction(MCAction action) throws Exception {
			// TODO
			// Use the dialect to translate actions into words.
			return "";
		}
	}

	/**
	 * InternalHelper extends DDLHelper, executes statements.
	 */
	public class InternalHelper extends DDLHelper {
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
	public class FileHelper extends DDLHelper {
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
			ZipEntry entry = new ZipEntry(level + "/" + action.getSequence());
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
}
