/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.ewin.common.util.Log;
import org.ewin.javax.sql.DefaultPoolingAlgorithm;
import org.ewin.javax.sql.DriverManagerDataSource;
import org.ewin.javax.sql.PoolingAlgorithmDataSource;

/**
 * Datasource with extra functionality:
 * 
 * <ul>
 * <li> parameters are available via getters.
 * <li> lazy loads connection (useful when used offline and connection not needed)
 * <li> offers connection pooling
 * <li> implements toString() which prints something user friendly. 
 * </ul>
 * 
 * Users should <code>Connection conn = dataSource.getConnection()</code> 
 * to retrieve a connection from the pool and <code>conn.close()</code> to return it to the pool.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class DetailedDataSource implements DataSource {

	// Disable logging from ewin connection pool package
	static {

		//temporarily disable stdout
		PrintStream n = new PrintStream(new ByteArrayOutputStream());
		PrintStream o = new PrintStream(System.out);

		System.setOut(n);

		// Configure Ewin logging - remove all defined loggers
		Iterator ewinLoggers = Log.loggers();
		while (ewinLoggers.hasNext()) {
			Log.removeLogger((Log.Logger) ewinLoggers.next());
		}

		//reset stdout
		System.setOut(o);
	}

	public static final String DEFAULTDATABASETYPE = "mysql";
	public static final String DEFAULTDRIVER = "com.mysql.jdbc.Driver";
	public static final int DEFAULTPOOLSIZE = 10;
	public static final String DEFAULTPORT = "3306";
	
  private static final String ORACLEAT = "@";
	private static final String ORACLETHIN = "oracle:thin";
	private static final String SYBASE = "sybase:Tds";

	private String databaseType;
	private String host;
	private String port;
	private String databaseName;
	private int maxPoolSize;
	private String password;
	private String user;
	private String jdbcDriverClassName;
	private String displayName;
	private DataSource dataSource;
	private String connectionString;

	/**
	 * Creates a datasource backed by a connection pool. connectionString should 
	 * match the host, port, and dbType. 
	 * @param dbType database type e.g. mysql.
	 * @param host host name e.g. ensembldb.ensembl.org
	 * @param port port number. e.g. 3306.
	 * @param database name of database on database server, can be null for "meta" queries e.g. what databasea are available  
	 * @param connectionString database connectionString, e.g. jdbc:mysql://ensembldb.ensembl.org:3036
	 * @param user username
	 * @param password password, can be null
	 * @param maxPoolSize maximum poolsize
	 * @param jdbcDriverClassName name of jdbc driver to back the datasource.
	 * @return connection pool capable datasource
	 **/
	public DetailedDataSource(
		String dbType,
		String host,
		String port,
		String databaseName,
		String connectionString,
		String user,
		String password,
		int maxPoolSize,
		String jdbcDriverClassName) {

		assert dbType != null;
		assert host != null;
		assert port != null;
    assert connectionString != null;
    assert databaseName == null || connectionString.indexOf( databaseName )!=-1;
		assert user != null;
		assert maxPoolSize >= 0;
		assert jdbcDriverClassName != null;

		this.databaseType = dbType;
		this.host = host;
		this.port = port;
		this.databaseName = databaseName;
		this.connectionString = connectionString;
		this.user = user;
		this.password = password;
		this.maxPoolSize = maxPoolSize;
		this.jdbcDriverClassName = jdbcDriverClassName;

	}

	/**
	 * Convenience method which calls createDataSource(DEFAULTDATABASETYPE, host, DEFAULTPORT, database, user, password, DEFAULTPOOLSIZE, DEFAULTDRIVER);
	 * @param host host name e.g. ensembldb.ensembl.org
	 * @param database name of database on database server  
	 * @param user username
	 * @param password password, can be null
	 * @return connection pool capable datasource
	 * @throws ConfigurationException thrown if a problem occurs creating the datasource
	 */
	public DetailedDataSource(
		String host,
		String database,
		String user,
		String password) {
		this(
			DEFAULTDATABASETYPE,
			host,
			DEFAULTPORT,
			database,
			user,
			password,
			DEFAULTPOOLSIZE,
			DEFAULTDRIVER);
	}

	/**
	 * Datasource constructed by specific parameters. The connection is automatically
   * derived from the host, port and databaseType.
	 * 
	 * @param databaseType database type e.g. mysql.
	 * @param host host name e.g. ensembldb.ensembl.org
	 * @param port port number. e.g. 3306.
	 * @param database name of database on database server  
	 * @param user username
	 * @param password password, can be null
	 * @param maxPoolSize maximum poolsize.
	 * @param jdbcDriverClassName name of jdbc driver to back the datasource.
	 */
	public DetailedDataSource(
		String databaseType,
		String host,
		String port,
		String database,
		String user,
		String password,
		int maxPoolSize,
		String jdbcDriverClassName) {

		this(
			databaseType,
			host,
			port,
			database,
			getConnectionURL(databaseType, host, port, database),
			user,
			password,
			maxPoolSize,
			jdbcDriverClassName);

	}

	/**
	 * Returns a connection URL for jdbc.  This could differ from RDBMS to RDBMS.
	 * Currently supports oracle:thin, sybase:Tbs, db2, mySql, and postGreSql, and any other
	 * database whose connnection URL syntax matches one of these. The dbType will produce
	 * different connection strings:
	 * <ul>
	 * <li>oracle:thin --> jdbc:oracle:thin:@host:port:dbname
	 * <li>sybase:Tds --> jdbc:sybase:Tds:host:port/dbname
	 * <li>postgresSQL/mySQL --> jdbc:x://host:port/dbname
	 * </ul>
	 * @param databaseType database type e.g. mysql.
	 * @param host host name e.g. ensembldb.ensembl.org
	 * @param port port number. e.g. 3306.
	 * @param databaseName of database on database server  
	 * @return String connectionURL
	 */
	public static String getConnectionURL(
		String dbType,
		String host,
		String port,
		String databaseName) {

		if (dbType.equals(ORACLETHIN)) {
			host = ORACLEAT + host;
			databaseName = ":" + databaseName;
		} else if (dbType.equals(SYBASE)) {
			databaseName = "/" + databaseName;
		} else {
			host = "//" + host;
			databaseName = "/" + databaseName;
		}

		StringBuffer dbURL = new StringBuffer();
		dbURL.append("jdbc:").append(dbType).append(":");
		dbURL.append(host);
		if (port != null && !"".equals(port))
			dbURL.append(":").append(port);

		if (databaseName != null && !databaseName.equals(""))
			dbURL.append(databaseName);

		return dbURL.toString();
	}

  /**
   * Convenience method for closing a connection and handling any SQLException
   * by printing a stack trace.
   * @param conn connection to be closed, method does nothing if conn=null.
   */
  public static void close(Connection conn) {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }



  public String[] databaseNames() throws SQLException {
    List databases = new ArrayList();

    Connection conn = getConnection();
    // TODO check this works with non-mysql databases
    ResultSet rs = conn.createStatement().executeQuery("show databases");
    while (rs.next()) {
      databases.add(rs.getString(1));
    }
    close( conn );
    
    return (String[]) databases.toArray(new String[databases.size()]);
  }

	/**
	 * @return databaseName@host:port
	 */
	public String simpleRepresentation() {
		return databaseName + "@" + host + ":" + port;
	}

	public String toString() {
		return displayName;
	}

	/**
	 * A connection pool is created when this merthod is first called
	 * and then connections are returned from it.
	 * @return Connection to the database specified.
	 * @throws java.sql.SQLException if any problem occurs making the connection.
	 */
	public Connection getConnection() throws SQLException {

		if (dataSource == null) {
			try {
				// load driver
				Class.forName(jdbcDriverClassName).newInstance();

				dataSource =
					new DriverManagerDataSource(
						jdbcDriverClassName,
						connectionString,
						user,
						password);

				// Wrap data source in connection pool
				PoolingAlgorithmDataSource tmp =
					new PoolingAlgorithmDataSource(dataSource);
				DefaultPoolingAlgorithm poolAlgorithm = new DefaultPoolingAlgorithm();
				poolAlgorithm.setPoolMax(maxPoolSize);
				tmp.setPoolingAlgorithm(poolAlgorithm);

				dataSource = tmp;

			} catch (InstantiationException e) {
				e.printStackTrace();
				throw new SQLException(
					"Failed to initialise database connection pool "
						+ "(is the connection pool jar available?) : ");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new SQLException(
					"Failed to initialise database connection pool for "
						+ jdbcDriverClassName
						+ " (is the connection pool jar available?) : ");
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new SQLException(
					"Failed to initialise database connection pool "
						+ "(is the connection pool jar available?) : ");
			} catch (NoClassDefFoundError e) {
				e.printStackTrace();
				throw new SQLException(
					"Failed to initialise database connection pool "
						+ "(is the connection pool jar available?) : ");
			}

		}
		return dataSource.getConnection();
	}

	/**
	 * @param username
	 * @param password
	 * @return
	 * @throws java.sql.SQLException
	 */
	public Connection getConnection(String username, String password)
		throws SQLException {
		return dataSource.getConnection(username, password);
	}

	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public int getLoginTimeout() throws SQLException {
		return dataSource.getLoginTimeout();
	}

	/**
	 * @return
	 * @throws java.sql.SQLException
	 */
	public PrintWriter getLogWriter() throws SQLException {
		return dataSource.getLogWriter();
	}

	/**
	 * @param seconds
	 * @throws java.sql.SQLException
	 */
	public void setLoginTimeout(int seconds) throws SQLException {
		dataSource.setLoginTimeout(seconds);
	}

	/**
	 * @param out
	 * @throws java.sql.SQLException
	 */
	public void setLogWriter(PrintWriter out) throws SQLException {
		dataSource.setLogWriter(out);
	}

	/**
	 * @return
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @return
	 */
	public String getDatabaseType() {
		return databaseType;
	}

	/**
	 * @return
	 */
	public DataSource getDatasource() {
		return dataSource;
	}

	/**
	 * @return
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return
	 */
	public String getJdbcDriverClassName() {
		return jdbcDriverClassName;
	}

	/**
	 * @return
	 */
	public String getPort() {
		return port;
	}

}