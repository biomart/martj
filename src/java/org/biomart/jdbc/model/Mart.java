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

package org.biomart.jdbc.model;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.biomart.jdbc.exceptions.RegistryException;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 	
 * 			$Author$
 * @since 0.6
 */
public abstract class Mart {
	private String martName;

	private Map dataSets = new HashMap();

	/**
	 * Construct a mart with the given name.
	 * 
	 * @param martName
	 *            the name of this mart.
	 */
	public Mart(final String martName) {
		this.martName = martName;
	}

	/**
	 * Get the name.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return this.martName;
	}

	/**
	 * Find out what datasets are in this mart.
	 * 
	 * @return the list of datasets. May be empty but never <tt>null</tt>.
	 */
	public Collection getDatasetNames() {
		return this.dataSets.keySet();
	}

	/**
	 * Obtain the named dataset.
	 * 
	 * @param datasetName
	 *            the name of the dataset.
	 * @return the dataset.
	 * @throws RegistryException
	 *             if the dataset could not be found.
	 */
	public DataSet getDataset(String datasetName) throws RegistryException {
		if (!this.dataSets.containsKey(datasetName))
			throw new RegistryException(Resources.get("noSuchDataset",
					datasetName));
		return (DataSet) this.dataSets.get(datasetName);
	}

	/**
	 * This represents a JDBC connection to a database containing a mart.
	 */
	public class JDBCMart extends Mart {

		private DataSource dataSource;

		private List connections = new ArrayList();

		/**
		 * Calls {@link Mart#Mart(String)}, and remembers the data source.
		 * 
		 * @param martName
		 *            the name of the mart.
		 * @param dataSource
		 *            the data source the mart lives in.
		 */
		public JDBCMart(String martName, DataSource dataSource) {
			super(martName);
			this.dataSource = dataSource;
		}

		/**
		 * Establish a JDBC connection to the mart.
		 * 
		 * @return the connection.
		 * @throws SQLException
		 *             if the connection could not be established.
		 */
		public Connection getConnection() throws SQLException {
			final Connection conn = this.dataSource.getConnection();
			this.connections.add(conn);
			return conn;
		}

		/**
		 * Inform us that the connection is finished with.
		 * 
		 * @param conn
		 *            the connection that is no longer required.
		 */
		public void connectionClosed(final Connection conn) {
			try {
				if (conn != null && !conn.isClosed())
					conn.close();
			} catch (SQLException e) {
				// We don't care.
			} finally {
				this.connections.remove(conn);
			}
		}

		public void finalize() {
			for (final Iterator i = this.connections.iterator(); i.hasNext();) {
				final Connection conn = (Connection) i.next();
				try {
					if (conn != null && !conn.isClosed())
						conn.close();
				} catch (SQLException e) {
					// We don't care.
				}
			}
			this.connections.clear();
		}
	}

	/**
	 * This represents an XML webservices connection to a mart.
	 */
	public class XMLMart extends Mart {

		private URL serverURL;

		/**
		 * Calls {@link Mart#Mart(String)}, and remembers the data source.
		 * 
		 * @param martName
		 *            the name of the mart.
		 * @param serverURL
		 *            the server the mart lives in.
		 */
		public XMLMart(String martName, URL serverURL) {
			super(martName);
			this.serverURL = serverURL;
		}

		/**
		 * Get the server to send queries to.
		 * 
		 * @return the server to send queries to.
		 */
		public URL getServerURL() {
			return this.serverURL;
		}
	}
}
