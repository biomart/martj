/*
 * Created on Aug 2, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.lib;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ewin.javax.sql.DefaultPoolingAlgorithm;
import org.ewin.javax.sql.DriverManagerDataSource;
import org.ewin.javax.sql.PoolingAlgorithmDataSource;
import org.ewin.common.util.Log;

/**
 * Utility class for working with JDBC databases.
 */
public class DatabaseUtil {

  static {
    // Configure Ewin logging - remove all defined loggers
    Iterator ewinLoggers = Log.loggers();
    while (ewinLoggers.hasNext()) {
      Log.removeLogger((Log.Logger) ewinLoggers.next());
    }
  }



  /**
   * Default settings for DataSource creation parameters
   */
  public static int DEFAULTPOOLSIZE = 10;
  public static String DEFAULTDATABASETYPE = "mysql";
  public static String DEFAULTDRIVER = "com.mysql.jdbc.Driver";
  public static String DEFAULTPORT = "3306";


	public static final Connection getConnection(
		String databaseType,
		String host,
		String port,
		String databaseName,
		String user,
		String password)
		throws SQLException {

		StringBuffer dbURL = new StringBuffer();
		dbURL.append("jdbc:").append(databaseType).append("://");
		dbURL.append(host);
		if (port != null && !"".equals(port))
			dbURL.append(":").append(port);
		dbURL.append("/");
		if (databaseName != null && !databaseName.equals(""))
			dbURL.append(databaseName);

		return getConnection(dbURL.toString(), user, password);
	}

	public static final Connection getConnection(
		String dbURL,
		String user,
		String password)
		throws SQLException {

		return DriverManager.getConnection(dbURL, user, password);
	}

	public static final String[] databaseNames(Connection conn)
		throws SQLException {
		List databases = new ArrayList();

		ResultSet rs = conn.createStatement().executeQuery("show databases");
		while (rs.next()) {
			databases.add(rs.getString(1));
		}

		return (String[]) databases.toArray(new String[databases.size()]);
	}

	public static class DatabaseURLElements {

		public String databaseURL;
		public String databaseType;
		public String host;
		public String port;
		public String databaseName;
    public String jdbcDriverClassName;
	};

	/**
	 * Decomposes a databaseURL into it's constituent parts.
	 * 
	 * <p>EXAMPLE :
	 * <code>jdbc:mysql://kaka.sanger.ac.uk:3306/ensembl_mart_15_1</code>
	 * </p > 
	 * @param URL database connection string. 
	 * @throws IllegalArgumentException if the DriverManager cannot use the URL to get the hdbcDriverClassName, or databaseURL has the wrong format.
	 */
	public static final DatabaseURLElements decompose(String databaseURL)
		throws IllegalArgumentException {

    DatabaseURLElements elements = new DatabaseURLElements();
      
		try {
			//long winded way of finding the JDBC Driver class name from the databaseURL
			Driver jdbcDriver = DriverManager.getDriver(databaseURL); // gets a driver for this DB URL
			elements.jdbcDriverClassName = jdbcDriver.getClass().getName(); // get the classname from the driver class
			
		} catch (SQLException e) {
			 throw new IllegalArgumentException("Invalid database URL: " + databaseURL);
		}    
    
		elements.databaseURL = databaseURL;
		Pattern p =
			Pattern.compile("^(\\w+:(\\w+)://([^:/]+)(:(\\d+))?)(/([^?]*))?$");
		Matcher m = p.matcher(databaseURL);
		if (m.matches()) {
			elements.databaseType = m.group(2);
			elements.host = m.group(3);
			elements.port = m.group(5);
			elements.databaseName = m.group(7);
		} else
			throw new IllegalArgumentException("Invalid database URL:" + databaseURL);

		return elements;
	}

	private static Logger logger = Logger.getLogger(DatabaseUtil.class.getName());

  /**
   * Convenience method which calls createDataSource(DEFAULTDATABASETYPE, host, DEFAULTPORT, database, user, password, DEFAULTPOOLSIZE, DEFAULTDRIVER);
   * @param host host name e.g. ensembldb.ensembl.org
   * @param database name of database on database server  
   * @param user username
   * @param password password, can be null
   * @return connection pool capable datasource
   * @throws ConfigurationException thrown if a problem occurs creating the datasource
   */
  public static DataSource createDataSource(String host, String database, String user, String password) throws ConfigurationException {
    return createDataSource(DEFAULTDATABASETYPE, host, DEFAULTPORT, database, user, password, DEFAULTPOOLSIZE, DEFAULTDRIVER);
  }
  
	/**
	 * Convenience method which constructs a connection URL and calls createDataSource(String user, String password, int maxPoolSize, String jdbcDriverClassName).
	 * @see #createDataSource(String user, String password, int maxPoolSize, String jdbcDriverClassName)
	 * @param dbType database type e.g. mysql.
	 * @param host host name e.g. ensembldb.ensembl.org
	 * @param port port number. e.g. 3306.
	 * @param database name of database on database server  
	 * @param user username
	 * @param password password, can be null
	 * @param maxPoolSize maximum poolsize.
	 * @param jdbcDriverClassName name of jdbc driver to back the datasource.
	 * @return connection pool capable datasource
	 * @throws ConfigurationException thrown if a problem occurs creating the datasource
	 */
	public static DataSource createDataSource(
		String dbType,
		String host,
		String port,
		String database,
		String user,
		String password,
		int maxPoolSize,
		String jdbcDriverClassName)
		throws ConfigurationException {
      
		StringBuffer connString = new StringBuffer();
		connString.append("jdbc:").append(dbType).append("://");
		connString.append(host);
		if (port != null)
			connString.append(":").append(port);
		connString.append("/");
		if (database != null)
			connString.append(database);

		return createDataSource(
			connString.toString(),
			user,
			password,
			maxPoolSize,
			jdbcDriverClassName);
	}

	/**
	 * Creates a datasource backed by a connection pool. Users should <code>Connection conn = dataSource.getConnection()</code> 
	 * to retrieve a connection from the pool and <code>conn.close()</code> to return it to the pool.
	 * @param connectionString database connectionString, e.g. jdbc:mysql://ensembldb.ensembl.org:3036
	 * @param user username
	 * @param password password, can be null
	 * @param maxPoolSize maximum poolsize
	 * @param jdbcDriverClassName name of jdbc driver to back the datasource.
	 * @return connection pool capable datasource
	 * @throws ConfigurationException thrown if a problem occurs creating the datasource
	 **/
	public static DataSource createDataSource(
		String connectionString,
		String user,
		String password,
		int maxPoolSize,
		String jdbcDriverClassName)
		throws ConfigurationException {

    if (connectionString == null)
      throw new ConfigurationException("Recieved Null connectionString\n");
    if ( jdbcDriverClassName == null)
      throw new ConfigurationException("Recieved Null jdbcDriverClassName\n");
      
		try {

			// load driver
			Class.forName(jdbcDriverClassName).newInstance();

			DataSource dataSource =
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

			return dataSource;

		} catch (InstantiationException e) {
			throw new ConfigurationException(
				"Failed to initialise database connection pool "
					+ "(is the connection pool jar available?) : ",
				e);
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(
				"Failed to initialise database connection pool for "
					+ jdbcDriverClassName + " (is the connection pool jar available?) : ",
				e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(
				"Failed to initialise database connection pool "
					+ "(is the connection pool jar available?) : ",
				e);
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			// can't pass it to ConfigurationExcpeption so print stack trace here       
			throw new ConfigurationException(
				"Failed to initialise database connection pool "
					+ "(is the connection pool jar available?) : ");

		}
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
}
