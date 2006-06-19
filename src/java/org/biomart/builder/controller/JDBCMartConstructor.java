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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
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
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.MartConstructor.GenericMartConstructor;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This implementation of the {@link MartConstructor} interface connects to a
 * JDBC data source in order to create a mart. It has options to output DDL to
 * file instead of running it, to run DDL directly against the database, or to
 * use JDBC to fetch/retrieve data between two databases.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 16th June 2006
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
	 * remembers it. Also calls {@link DatabaseDialect#registerDialects()} to
	 * ensure all known dialects are covered.
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

		// Register dialects.
		DatabaseDialect.registerDialects();
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

	/**
	 * Gets the file to use as output when creating the zipped DDL files.
	 * 
	 * @return the file that will be created.
	 */
	public File getOutputDDLZipFile() {
		return this.outputDDLZipFile;
	}

	/**
	 * Sets the file to use as output when creating the zipped DDL files.
	 * 
	 * @param outputDDLZipFile
	 *            the file that will be created.
	 */
	public void setOutputDDLZipFile(File outputDDLZipFile) {
		this.outputDDLZipFile = outputDDLZipFile;
	}

	/**
	 * Gets the type to use for this mart construction.
	 * 
	 * @return the type used for this mart construction.
	 */
	public JDBCMartConstructorType getType() {
		return this.type;
	}

	/**
	 * Sets the type to use for this mart construction.
	 * 
	 * @param type
	 *            the type used for this mart construction.
	 */
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

	public ConstructorRunnable getConstructorRunnable(DataSet ds)
			throws Exception {
		// Work out what kind of helper to use.
		DDLHelper helper;
		if (getType().equals(JDBCMartConstructorType.INTERNAL))
			helper = new InternalHelper(getConnection());
		else if (getType().equals(JDBCMartConstructorType.SINGLEFILE))
			helper = new SingleFileHelper(getOutputDDLZipFile());
		else if (getType().equals(JDBCMartConstructorType.MULTIFILE))
			helper = new MultiFileHelper(getOutputDDLZipFile());
		else if (getType().equals(JDBCMartConstructorType.EXTERNAL))
			throw new Error(BuilderBundle.getString("mcExternalNotImpl"));
		else
			throw new ConstructorException(BuilderBundle
					.getString("unknownJDBCMCType"));

		// Set the input and output dialects on the helper for each
		// schema.

		// Inputs first.
		Set inputSchemas = new HashSet();
		for (Iterator i = ds.getTables().iterator(); i.hasNext();) {
			Table t = (Table) i.next();
			for (Iterator j = t.getColumns().iterator(); j.hasNext();) {
				Column c = (Column) j.next();
				if (c instanceof WrappedColumn)
					inputSchemas.add(((WrappedColumn) c).getWrappedColumn()
							.getTable().getSchema());
			}
		}
		for (Iterator i = inputSchemas.iterator(); i.hasNext();) {
			Schema s = (Schema) i.next();
			helper.setInputDialect(s, DatabaseDialect.getDialect(s));
		}

		// Then the output.
		helper.setOutputDialect(DatabaseDialect.getDialect(this));

		// Construct and return the runnable that uses the helper.
		return new GenericConstructorRunnable(ds, helper);
	}

	/**
	 * JDBCHelper provides extra methods for understanding different SQL and DDL
	 * dialects from various JDBC connections.
	 */
	public interface JDBCHelper extends Helper {
		/**
		 * Sets the dialect to use on tables which come from the given schema.
		 * 
		 * @param schema
		 *            the schema to use the dialect on.
		 * @param dialect
		 *            the dialect to use for tables in that schema.
		 */
		public void setInputDialect(Schema schema, DatabaseDialect dialect);

		/**
		 * Sets the dialect to use to create the output tables with.
		 * 
		 * @param dialect
		 *            the dialect to use when creating output tables.
		 */
		public void setOutputDialect(DatabaseDialect dialect);
	}

	/**
	 * DDLHelper generates DDL statements for each step.
	 */
	public abstract static class DDLHelper implements JDBCHelper {
		private DatabaseDialect dialect;

		private int tempTableSeq = 0;

		public void setInputDialect(Schema schema, DatabaseDialect dialect) {
			// Ignored as is not required - the input dialect is the
			// same as the output dialect if we are to run DDL statements.
		}

		public void setOutputDialect(DatabaseDialect dialect) {
			this.dialect = dialect;
		}

		public String getNewTempTableName() {
			return "__JDBCMART_TEMP__" + this.tempTableSeq++;
		}

		public List listDistinctValues(Column col) throws SQLException {
			return this.dialect.executeSelectDistinct(col);
		}

		/**
		 * Translates an action into commands, using
		 * {@link DatabaseDialect#getStatementsForAction(MCAction)}
		 * 
		 * @param action
		 *            the action to translate.
		 * @return the translated action.
		 * @throws Exception
		 *             if anything went wrong.
		 */
		public String[] getStatementsForAction(MCAction action)
				throws Exception {
			return dialect.getStatementsForAction(action);
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
			String[] cmd = this.getStatementsForAction(action);
			// As we're working internally, the input connection is the
			// same for all schemas, and is the same as the output one.
			// Prepare and execute the command.
			for (int i = 0; i < cmd.length; i++)
				this.outputConn.prepareStatement(cmd[i]).execute();
		}

		public void endActions() throws Exception {
			// We don't care.
		}
	}

	/**
	 * SingleFileHelper extends DDLHelper, saves statements. Statements are
	 * saved as a single SQL file inside a Zip file.
	 */
	public static class SingleFileHelper extends DDLHelper {
		private File outputFile;

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		private ZipEntry entry;

		/**
		 * Constructs a helper which will output all DDL into a single file
		 * inside the given zip file.
		 * 
		 * @param outputZippedDDLFile
		 *            the zip file to write the DDL into.
		 */
		public SingleFileHelper(File outputZippedDDLFile) {
			super();
			this.outputFile = outputZippedDDLFile;
		}

		public void startActions() throws Exception {
			// Open the zip stream.
			this.outputFileStream = new FileOutputStream(this.outputFile);
			this.outputZipStream = new ZipOutputStream(this.outputFileStream);
			this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
			this.entry = new ZipEntry("ddl.sql");
			entry.setTime(System.currentTimeMillis());
			this.outputZipStream.putNextEntry(entry);
		}

		public void executeAction(MCAction action, int level) throws Exception {
			// Convert the action to some DDL.
			String[] cmd = this.getStatementsForAction(action);
			// Write the data.
			for (int i = 0; i < cmd.length; i++) {
				this.outputZipStream.write(cmd[i].getBytes());
				this.outputZipStream.write(';');
				this.outputZipStream.write(System.getProperty("line.separator")
						.getBytes());
			}
		}

		public void endActions() throws Exception {
			// Close the zip stream. Will also close the
			// file output stream by default.
			this.outputZipStream.closeEntry();
			this.outputZipStream.finish();
			this.outputFileStream.flush();
			this.outputFileStream.close();
		}
	}

	/**
	 * MultiFileHelper extends DDLHelper, saves statements. Statements are saved
	 * in folders. Folder 1 must be finished before folder 2, but files within
	 * folder 1 can be executed in any order. And so on.
	 */
	public static class MultiFileHelper extends DDLHelper {
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
		public MultiFileHelper(File outputZippedDDLFile) {
			super();
			this.outputFile = outputZippedDDLFile;
		}

		public void startActions() throws Exception {
			// Open the zip stream.
			this.outputFileStream = new FileOutputStream(this.outputFile);
			this.outputZipStream = new ZipOutputStream(this.outputFileStream);
			this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
		}

		public void executeAction(MCAction action, int level) throws Exception {
			try {
				// Writes the given action to file.
				// Put the next entry to the zip file.
				ZipEntry entry = new ZipEntry(level + "/" + level + "-"
						+ action.getSequence() + ".sql");
				entry.setTime(System.currentTimeMillis());
				this.outputZipStream.putNextEntry(entry);
				// Convert the action to some DDL.
				String[] cmd = this.getStatementsForAction(action);
				// Write the data.
				for (int i = 0; i < cmd.length; i++) {
					this.outputZipStream.write(cmd[i].getBytes());
					this.outputZipStream.write(';');
					this.outputZipStream.write(System.getProperty(
							"line.separator").getBytes());
				}
				// Close the entry.
				this.outputZipStream.closeEntry();
			} catch (Exception e) {
				// Make sure we don't leave open entries lying around
				// if exceptions get thrown.
				this.outputZipStream.closeEntry();
				throw e;
			}
		}

		public void endActions() throws Exception {
			// Close the zip stream. Will also close the
			// file output stream by default.
			this.outputZipStream.finish();
			this.outputFileStream.flush();
			this.outputFileStream.close();
		}
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
				.get(BuilderBundle.getString("jdbcMCTypeInternal"));

		/**
		 * Use this constant to refer to creation via JDBC import/export.
		 */
		public static final JDBCMartConstructorType EXTERNAL = JDBCMartConstructorType
				.get(BuilderBundle.getString("jdbcMCTypeExternal"));

		/**
		 * Use this constant to refer to generation of DDL in a file.
		 */
		public static final JDBCMartConstructorType SINGLEFILE = JDBCMartConstructorType
				.get(BuilderBundle.getString("jdbcMCTypeSingleFile"));

		/**
		 * Use this constant to refer to generation of DDL in a file.
		 */
		public static final JDBCMartConstructorType MULTIFILE = JDBCMartConstructorType
				.get(BuilderBundle.getString("jdbcMCTypeMultiFile"));

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
