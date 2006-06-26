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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This interface defines the methods required to connect to and test a data
 * source. It doesn't define any data source specific methods, only those that
 * are required to make the rest of the system work without worrying about where
 * the data is coming from.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 21st June 2006
 * @since 0.1
 */
public interface DataLink {
	/**
	 * Checks to see if this datalink 'cohabits' with another one. Cohabitation
	 * means that it would be possible to write a single SQL statement that
	 * could read data from both datalinks simultaneously.
	 * 
	 * @param partner
	 *            the other datalink to test for cohabitation.
	 * @return true if the two can cohabit, false if not.
	 */
	public boolean canCohabit(DataLink partner);

	/**
	 * Checks to see if this datalink is working properly. Returns <tt>true</tt>
	 * if it is, otherwise throws an exception describing the problem. Should
	 * never return <tt>false</tt>.
	 * 
	 * @return <tt>true</tt> if the link is working. Should never return
	 *         <tt>false</tt>, as an exception will always be thrown if there
	 *         is a problem.
	 * @throws Exception
	 *             if there is a problem.
	 */
	public boolean test() throws Exception;

	/**
	 * This inner interface defines methods required for JDBC connections only.
	 */
	public interface JDBCDataLink extends DataLink {
		/**
		 * Returns a JDBC connection connected to this database using the data
		 * supplied to all the other methods in this interface.
		 * 
		 * @return the connection for this database.
		 * @throws SQLException
		 *             if there was any problem connecting.
		 */
		public Connection getConnection() throws SQLException;

		/**
		 * Getter for the name of the driver class, eg.
		 * <tt>com.mysql.jdbc.Driver</tt>
		 * 
		 * @return the name of the driver class.
		 */
		public String getDriverClassName();

		/**
		 * Sets the name of the driver class, eg. <tt>com.mysql.jdbc.Driver</tt>
		 * 
		 * @param driverClassName
		 *            the name of the driver class.
		 */
		public void setDriverClassName(String driverClassName);

		/**
		 * Gets the location of the driver class, if specified. May return null.
		 * 
		 * @return the location of the driver class.
		 */
		public File getDriverClassLocation();

		/**
		 * Sets the location of the driver class. If the class is not found at
		 * that location, or the location is null, the default system class
		 * loader is used instead.
		 * 
		 * @param driverClassLocation
		 *            the location of the driver class.
		 */
		public void setDriverClassLocation(File driverClassLocation);

		/**
		 * Gets the JDBC URL.
		 * 
		 * @return the JDBC url.
		 */
		public String getJDBCURL();

		/**
		 * Sets the JDBC URL.
		 * 
		 * @param url
		 *            the JDBC url.
		 */
		public void setJDBCURL(String url);

		/**
		 * Gets the database schema name.
		 * 
		 * @return the database schema name.
		 */
		public String getDatabaseSchema();

		/**
		 * Sets the database schema name.
		 * 
		 * @param schemaName
		 *            the database schema name.
		 */
		public void setDatabaseSchema(String schemaName);

		/**
		 * Gets the username.
		 * 
		 * @return the username.
		 */
		public String getUsername();

		/**
		 * Sets the username.
		 * 
		 * @param username
		 *            the username.
		 */
		public void setUsername(String username);

		/**
		 * Gets the password.
		 * 
		 * @return the password.
		 */
		public String getPassword();

		/**
		 * Sets the password. If null, then no password will be used.
		 * 
		 * @param password
		 *            the password.
		 */
		public void setPassword(String password);
	}

	/**
	 * This inner interface defines methods required for XML files only.
	 * <p>
	 * TODO: Work out how it's going to work, then define it.
	 */
	public interface XMLDataLink extends DataLink {
	}
}