/*
 * Created on Aug 2, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.lib;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
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
import org.ewin.common.util.Log;
import org.ewin.javax.sql.DefaultPoolingAlgorithm;
import org.ewin.javax.sql.DriverManagerDataSource;
import org.ewin.javax.sql.PoolingAlgorithmDataSource;

/**
 * Utility class for working with JDBC databases.
 */
public class DatabaseUtil {

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



  /**
   * Default settings for DataSource creation parameters
   */
  public static final int DEFAULTPOOLSIZE = 10;
  public static final String DEFAULTDATABASETYPE = "mysql";
  public static final String DEFAULTDRIVER = "com.mysql.jdbc.Driver";
  public static final String DEFAULTPORT = "3306";

  private static final String ORACLETHIN = "oracle:thin";
  private static final String ORACLEAT = "@";
  private static final String SYBASE = "sybase:Tds";

  /*
   * oracle:thin URL jdbc:oracle:thin:@host:port:dbname
   * sybase:Tds URL jdbc:sybase:Tds:host:port/dbname
   * postgresSQL/mySQL URL jdbc:x://host:port/dbname
   */
  public static final Connection getConnection(
    String databaseType,
    String host,
    String port,
    String databaseName,
    String user,
    String password)
    throws SQLException {

    return getConnection(getConnectionURL(databaseType, host, port, databaseName, user, password), user, password);
  }

  public static final Connection getConnection(String dbURL, String user, String password) throws SQLException {

    return DriverManager.getConnection(dbURL, user, password);
  }

  public static final String[] databaseNames(Connection conn) throws SQLException {
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


		/**
		 * @return databaseName@host:port
		 */
		public String simpleRepresentation() {
			return databaseName + "@" + host + ":" + port;
		}
  };


  /**
   * Simple wrapper for a datasource that implements a version
   * of toString() which prints something user friendly. Ideally
   * we would extend PoollingAlgorithmDatasource but that is final.
   */
  private static class PrettyPrintDataSource implements DataSource {
    
    private DataSource datasource;
    private String displayName;

		private PrettyPrintDataSource(DataSource ds, String displayName) {
      this.datasource = ds;
      this.displayName = displayName;
    }

    public String toString() {
      return displayName;
    }
    
    
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return datasource.equals(obj);
		}

		/**
		 * @return
		 * @throws java.sql.SQLException
		 */
		public Connection getConnection() throws SQLException {
			return datasource.getConnection();
		}

		/**
		 * @param username
		 * @param password
		 * @return
		 * @throws java.sql.SQLException
		 */
		public Connection getConnection(String username, String password)
			throws SQLException {
			return datasource.getConnection(username, password);
		}

		/**
		 * @return
		 * @throws java.sql.SQLException
		 */
		public int getLoginTimeout() throws SQLException {
			return datasource.getLoginTimeout();
		}

		/**
		 * @return
		 * @throws java.sql.SQLException
		 */
		public PrintWriter getLogWriter() throws SQLException {
			return datasource.getLogWriter();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return datasource.hashCode();
		}

		/**
		 * @param seconds
		 * @throws java.sql.SQLException
		 */
		public void setLoginTimeout(int seconds) throws SQLException {
			datasource.setLoginTimeout(seconds);
		}

		/**
		 * @param out
		 * @throws java.sql.SQLException
		 */
		public void setLogWriter(PrintWriter out) throws SQLException {
			datasource.setLogWriter(out);
		}

  }


  /**
   * Decomposes a databaseURL into it's constituent parts.
   * 
   * <p>EXAMPLE :
   * <code>jdbc:mysql://kaka.sanger.ac.uk:3306/ensembl_mart_15_1</code>
   * </p > 
   * @param URL database connection string. 
   * @throws IllegalArgumentException if the DriverManager cannot use the URL to get the hdbcDriverClassName, or databaseURL has the wrong format.
   */
  public static final DatabaseURLElements decompose(String databaseURL) throws IllegalArgumentException {

    DatabaseURLElements elements = new DatabaseURLElements();

    try {
      //long winded way of finding the JDBC Driver class name from the databaseURL
      Driver jdbcDriver = DriverManager.getDriver(databaseURL); // gets a driver for this DB URL
      elements.jdbcDriverClassName = jdbcDriver.getClass().getName(); // get the classname from the driver class

    } catch (SQLException e) {
      throw new IllegalArgumentException("Invalid database URL: " + databaseURL);
    }

    elements.databaseURL = databaseURL;

    //Note: mySQLP also matches db2 and postGreSQL, others
    Pattern mySQLP = Pattern.compile("^(\\w+:(\\w+):?//([^:/]+)(:(\\d+))?)(/([^?]*))?$");
    Pattern sybaseP = Pattern.compile("^(\\w+:(\\w+:\\w+):([^:/]+)(:(\\d+))?)(/([^?]*))?$");
    Pattern oracleThinP = Pattern.compile("^(\\w+:(\\w+:\\w+):@([^:/]+)(:(\\d+))?)(:([^?]*))?$");

    Matcher mysqlM = mySQLP.matcher(databaseURL);
    Matcher sybaseM = sybaseP.matcher(databaseURL);
    Matcher oracleThinM = oracleThinP.matcher(databaseURL);

    if (mysqlM.matches()) {
      elements.databaseType = mysqlM.group(2);
      elements.host = mysqlM.group(3);
      elements.port = mysqlM.group(5);
      elements.databaseName = mysqlM.group(7);
    } else if (sybaseM.matches()) {
      elements.databaseType = sybaseM.group(2);
      elements.host = sybaseM.group(3);
      elements.port = sybaseM.group(5);
      elements.databaseName = sybaseM.group(7);
    } else if (oracleThinM.matches()) {
      elements.databaseType = oracleThinM.group(2);
      elements.host = oracleThinM.group(3);
      elements.port = oracleThinM.group(5);
      elements.databaseName = oracleThinM.group(7);
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
  public static DataSource createDataSource(String host, String database, String user, String password)
    throws ConfigurationException {
    return createDataSource(
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

    String connString = getConnectionURL(dbType, host, port, database, user, password);

    return createDataSource(connString, user, password, maxPoolSize, jdbcDriverClassName);
  }

  /**
   * Returns a connection URL for jdbc.  This could differ from RDBMS to RDBMS.
   * Currently supports oracle:thin, sybase:Tbs, db2, mySql, and postGreSql, and any other
   * database whose connnection URL syntax matches one of these.
   * @param databaseType database type e.g. mysql.
   * @param host host name e.g. ensembldb.ensembl.org
   * @param port port number. e.g. 3306.
   * @param databaseName of database on database server  
   * @param user username
   * @param password password, can be null
   * @return String dbURL
   */
  public static String getConnectionURL(
    String databaseType,
    String host,
    String port,
    String databaseName,
    String user,
    String password) {
    if (databaseType.equals(ORACLETHIN)) {
      host = ORACLEAT + host;
      databaseName = ":" + databaseName;
    } else if (databaseType.equals(SYBASE)) {
      databaseName = "/" + databaseName;
    } else {
      host = "//" + host;
      databaseName = "/" + databaseName;
    }

    StringBuffer dbURL = new StringBuffer();
    dbURL.append("jdbc:").append(databaseType).append(":");
    dbURL.append(host);
    if (port != null && !"".equals(port))
      dbURL.append(":").append(port);

    if (databaseName != null && !databaseName.equals(""))
      dbURL.append(databaseName);

    return dbURL.toString();
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
    if (jdbcDriverClassName == null)
      throw new ConfigurationException("Recieved Null jdbcDriverClassName\n");

    try {
      // load driver
      Class.forName(jdbcDriverClassName).newInstance();

      DataSource dataSource = new DriverManagerDataSource(jdbcDriverClassName, connectionString, user, password);
      
      // Wrap data source in connection pool
      PoolingAlgorithmDataSource tmp = new PoolingAlgorithmDataSource(dataSource);
      DefaultPoolingAlgorithm poolAlgorithm = new DefaultPoolingAlgorithm();
      poolAlgorithm.setPoolMax(maxPoolSize);
      tmp.setPoolingAlgorithm(poolAlgorithm);

      String displayName = decompose(connectionString).simpleRepresentation();
      dataSource = new PrettyPrintDataSource(tmp, displayName);

      return dataSource;
    } catch (InstantiationException e) {
      throw new ConfigurationException(
        "Failed to initialise database connection pool " + "(is the connection pool jar available?) : ",
        e);
    } catch (ClassNotFoundException e) {
      throw new ConfigurationException(
        "Failed to initialise database connection pool for "
          + jdbcDriverClassName
          + " (is the connection pool jar available?) : ",
        e);
    } catch (IllegalAccessException e) {
      throw new ConfigurationException(
        "Failed to initialise database connection pool " + "(is the connection pool jar available?) : ",
        e);
    } catch (NoClassDefFoundError e) {
      e.printStackTrace();
      // can't pass it to ConfigurationExcpeption so print stack trace here       
      throw new ConfigurationException(
        "Failed to initialise database connection pool " + "(is the connection pool jar available?) : ");

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
