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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.MartConstructor.GenericMartConstructor;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This implementation of the {@link MartConstructor} interface connects to a
 * JDBC data source in order to create a mart. It has options to output DDL to
 * file instead of running it, to run DDL directly against the database, or to
 * use JDBC to fetch/retrieve data between two databases.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 7th June 2006
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

	private File outputDDLFile;

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
		this.outputDDLFile = outputDDLFile;
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

	public File getOutputDDLFile() {
		return this.outputDDLFile;
	}

	public void setOutputDDLFile(File outputDDLFile) {
		this.outputDDLFile = outputDDLFile;
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

	public ConstructorRunnable getConstructorRunnable(DataSet ds) {
		return new ConstructorRunnable() {
			private Exception failure = null;

			public void run() {
				
				// TODO
				
				// Define Helper interface. Helper provides methods
				// to define large steps.
				// Helper implementations perform those steps.
				// DDLHelper generates DDL statements for each step.
				// InternalHelper extends DDLHelper, executes stmts.
				// FileHelper extends DDLHelper, saves stmts. Stmts
				//  are saved in folders. Folder 1 must be finished
				//  before folder 2, but files within folder 1 can
				//  be executed in any order. And so on.
				// ExternalHelper performs JDBC select/inserts.
				
				// TODO
				
				// Define Dialect interface. Dialect provides methods
				// which generate atomic DDL or SQL statements.
				// Oracle/MySQL/PostgreSQLDialect classes implement it.
				// DDLHelper has one Dialect for input/output.
				// ExternalHelper has one Dialect each for input/output.
				
				// TODO
				
				// Check to see if input and output DDL types are
				// understandable.

				// TODO

				// Number of steps = number of partition values times
				// the number of tables in the dataset, plus one for
				// the optimiser step. Each table completed per partition 
				// is one step forward, and the last step is the 
				// optimiser step.
				
				// TODO

				// For each table...
				
				// Find partition values for partition column on
				// table, if any.
								
				// For each partition value, which may be null if not
				// partitioned, call A with value for each table in
				// dataset.

				// (A)
				//  (run in parallel for each partition value)
				//  drop temp
				//  if value not null
				//    call step B on table with partition column,
				//    specifying current partition value
				//  else
				//    call step B on table, specifying partition value 
				//    of null.
				//  add schema name column if source is schema group
				//  call step C on table
				
				// (B)
				//  if temp exists
				//    merge table with temp using left join with given
				//     relation. create indexes on temp table relation 
				//     key first, and drop them after.
				//  else
				//    create temp as select * from table, restrict
				//     by partition column value if on this table and
				//	   value is not null.
				//  add all relations from merged table to queue and
				//   call step B on each one specifying partition value
				//   and relation.

				// (C)
				//  rename temp table to final name
				//  call step D
				
				// (D)
				//  create primary key on temp table
				//  call step E if dimension/subclass, step F if main.
				
				// (E)
				//  (depends on parent table for this partition value 
				//   being complete)
				//  if parent table is also partitioned, split this
				//   table into one chunk per parent table partition.
				//  delete records in each chunk that foreign key to
				//   parent table does not allow
				//  create foreign key to parent table.
				//  if subclass table, call step F.
				
				// (F)
				//  (depends on all tables for this partition value being
				//   complete)
				//  Run the optimiser nodes.
				
				
				if (getType().equals(JDBCMartConstructorType.INTERNAL)) {
					// Do it using DDL internally.
				} else if (getType().equals(JDBCMartConstructorType.EXTERNAL)) {
					// Do it using JDBC import/export.
				} else if (getType().equals(JDBCMartConstructorType.FILE)) {
					// Do it using DDL written to file.
				} else {
					failure = new ConstructorException(BuilderBundle
							.getString("unknownJDBCMCType"));
				}
			}

			public String getStatusMessage() {
				return "";
			}

			public int getPercentComplete() {
				return 100;
			}

			public Exception getFailureException() {
				return failure;
			}

			public void cancel() {
			}
		};
	}
}
