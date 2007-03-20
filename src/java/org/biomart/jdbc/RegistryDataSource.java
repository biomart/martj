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

package org.biomart.jdbc;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.sql.DataSource;

import org.biomart.jdbc.exceptions.RegistryException;
import org.biomart.jdbc.model.Registry;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class RegistryDataSource implements DataSource {

	/**
	 * The major version for this driver.
	 */
	public static final int MAJOR_VERSION = 0;

	/**
	 * The minor version for this driver.
	 */
	public static final int MINOR_VERSION = 6;

	// Holds the URL of this registry.
	private URL registryURL;

	// Our LogWriter.
	private PrintWriter logWriter;

	// Our login timeout.
	private int loginTimeout = 0;

	// Our set of open connections.
	private Collection openConnections = new HashSet();

	// Are we initialised?
	private Registry registry;

	/**
	 * Loads the registry XML and parses it into {@link Registry} objects.
	 * 
	 * @throws RegistryException
	 *             if there was a problem talking to the registry XML URL, or a
	 *             problem parsing the response.
	 */
	void initialise() throws RegistryException {
		if (this.registry != null)
			return;
		// We can assume that URL has already been set, as we
		// will only get called via a connection, which will
		// already have checked via getConnection() on this object.
		this.registry = new Registry(this.registryURL);
	}

	/**
	 * Obtains the registry from this data source.
	 * 
	 * @return the registry.
	 * @throws RegistryException
	 *             if there was a problem constructing it.
	 */
	Registry getRegistry() throws RegistryException {
		this.initialise();
		return this.registry;
	}

	public Connection getConnection() throws SQLException {
		return this.getRegistryConnection();
	}

	public Connection getConnection(final String username, final String password)
			throws SQLException {
		// We do not support usernames and passwords yet.
		return this.getConnection();
	}

	/**
	 * Returns a {@link RegistryConnection} that will allow queries against
	 * marts in this registry.
	 * 
	 * @return a connection.
	 * @throws SQLException
	 *             if the connection could not be created.
	 */
	protected RegistryConnection getRegistryConnection() throws SQLException {
		final DriverPropertyInfo[] missingProperties = this
				.getMissingProperties();
		if (missingProperties.length > 0)
			throw new SQLException(Resources.get("missingProperties", ""
					+ missingProperties));
		RegistryConnection conn = new RegistryConnection(this);
		synchronized (this.openConnections) {
			this.openConnections.add(conn);
		}
		return conn;
	}

	/**
	 * Inform the data source that a connection has been closed.
	 * 
	 * @param conn
	 *            the connection that has been closed.
	 */
	void connectionClosed(final RegistryConnection conn) {
		synchronized (this.openConnections) {
			this.openConnections.remove(conn);
		}
	}

	/**
	 * Checks to see if any properties we would need to establish a connection
	 * are missing. If they are, it returns their names in an array.
	 * 
	 * @return an array of missing properties. It will be empty if none are
	 *         missing, but never <tt>null</tt>.
	 */
	public DriverPropertyInfo[] getMissingProperties() {
		Collection missingProperties = new HashSet();
		if (this.registryURL == null) {
			final DriverPropertyInfo missingProp = new DriverPropertyInfo(
					"registryURL", null);
			missingProp.required = true;
			missingProp.description = Resources.get("registryURLDescription");
			missingProperties.add(missingProp);
		}
		return (DriverPropertyInfo[])missingProperties.toArray(new DriverPropertyInfo[0]);
	}

	/**
	 * Sets the URL for this registry.
	 * 
	 * @param registryURL
	 *            the URL to load this registry from.
	 */
	public void setRegistryURL(final URL registryURL) {
		this.registryURL = registryURL;
	}

	/**
	 * Sets the URL for this registry.
	 * 
	 * @param registryURL
	 *            the URL to load this registry from.
	 * @throws MalformedURLException
	 *             if the URL could not be parsed using the constructor
	 *             {@link URL#URL(String)}.
	 */
	public void setURL(final String registryURL) throws MalformedURLException {
		this.registryURL = new URL(registryURL);
	}

	/**
	 * Gets the URL this registry is loaded from.
	 * 
	 * @return the URL the registry came from.
	 */
	public URL getRegistryURL() {
		return this.registryURL;
	}

	public void setLogWriter(final PrintWriter out) throws SQLException {
		this.logWriter = out;
	}

	public PrintWriter getLogWriter() throws SQLException {
		return this.logWriter;
	}

	public void setLoginTimeout(final int seconds) throws SQLException {
		this.loginTimeout = seconds;
	}

	public int getLoginTimeout() throws SQLException {
		return this.loginTimeout;
	}

	/**
	 * Use this method to log something to the logfile, if one has been set at
	 * all.
	 * 
	 * @param message
	 *            the message to log. It can be any recognised object, as per
	 *            {@link PrintWriter#println(Object)}.
	 */
	public void logMessage(final Object message) {
		if (this.logWriter != null)
			this.logWriter.println(message);
	}

	public void finalize() {
		// We need to track and close all the connections we issued.
		for (final Iterator i = this.openConnections.iterator(); i.hasNext(); )
			try {
				((Connection)i.next()).close();
			} catch (SQLException e) {
				// We don't care. Get rid of it anyway.
			}
		this.openConnections.clear();
	}
}
