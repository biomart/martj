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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package org.ensembl.mart.lib;

import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;



/**
 * Class for interaction between UI and Mart Database.  Manages mySQL database
 * connections, and executes Querys.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */

//TODO: implement broad(transcript based) versus narrow(gene based) filtering of resultsets

public class Engine {
  private static Logger logger = Logger.getLogger(Engine.class.getName());

  /**
   * Attempts to load the database drivers normally shipped with 
   * martlib distribution.
   */
  private static void loadFallbackDatabaseDrivers(String[] driverNames) {
    for (int i = 0; i < driverNames.length; i++) {
      try {
        Class.forName(driverNames[i]).newInstance();
      } catch (Exception e) {
        logger.warn("Failed to load driver" + driverNames[i], e);
        throw new RuntimeException(e.getMessage());
      }
    }
  }

  /** 
   * Load drivers normally distributed with mart lib. These will be
   * available if no other drivers are previously loaded.
   */
  {
    loadFallbackDatabaseDrivers(new String[] { "org.gjt.mm.mysql.Driver" });
  }

  private String baseConnectionString;
  private Connection connection = null;
  private String connectionString;
  private String databaseName;
  private String password;
  private String user;

  /**
   * Creates an engine instance.
   *
   */
  public Engine() {
  }

  /**
   * Creates an Engine object for a connection to a specific mart
   * database running on a specific mySQL database instance. Also loads 
   * specific mySQL driver implementation "org.gjt.mm.mysql.Driver".
   * 
   * @param host - the mySQL host
   * @param port - the mySQL port
   * @param user - the mySQL user
   * @param password - the mySQL password (can be null)
   * @param database - the name of the mart database
   * @deprecated use Engine() followed by setDatabaseURL(), 
   * setUser() and setPassword() instead.
   * @see #Engine()
   * @see #setDatabaseURL()
   * @see #setUser()
   * @see #setPassword()
   */
  public Engine(
    String host,
    String port,
    String user,
    String password,
    String database) {

    this.user = user;
    this.password = password;

    setConnectionString( "mysql", host, port, database );

    
  }

  public int countFocus(Query query) {
    throw new RuntimeException();
  }

  public int countRows(Query query) {
    throw new RuntimeException();
  }

 

  /**
   * Checks for DomainSpecificFilters in the Query, and uses the DSFilterHandler
   * system to modify the Query accordingly, if present.
   * Constructs a QueryRunner object for the given Query, and format using 
   * a QueryRunnerFactory.  Uses the QueryRunner to execute the Query
   * with the mySQL connection of this Engine, and write the results to 
   * a specified OutputStream.
   * 
   * @param query - A Query Object
   * @param formatspec - A FormatSpec Object
   * @param os - An OutputStream
   * @throws FormatException - unsupported Format supplied to the QueryRunnerFactory
   * @throws SequenceException - general Exception thrown for a variety of reasons that the SeqQueryRunners cannot write out sequence data
   * @throws InvalidQueryException - general Exception thrown when invalid query parameters have been presented, and the resulting SQL will not work.
   * @see Query
   * @see FormatSpec
   * @see QueryRunnerFactory
   * @see QueryRunner
   * @see DSFilterHandler
   * @see DSFilterHandlerFactory
   */
  public void execute(Query query, FormatSpec formatspec, OutputStream os)
    throws SequenceException, FormatException, InvalidQueryException, SQLException {

    execute(query, formatspec, os, 0);

  }

  /**
   * Checks for DomainSpecificFilters in the Query, and uses the DSFilterHandler
   * system to modify the Query accordingly, if present.
   * Constructs a QueryRunner object for the given Query, and format using 
   * a QueryRunnerFactory.  Applies a limit clause to the SQL.
   * Uses the QueryRunner to execute the Query with the mySQL connection of 
   * this Engine, and write the results to a specified OutputStream.
   * 
   * @param query A Query Object
   * @param formatspec A FormatSpec Object
   * @param os An OutputStream
   * @param limit limits the number of records returned by the query
   * @throws SequenceException
   * @throws FormatException
   * @throws InvalidQueryException
   * @see Query
   * @see FormatSpec
   * @see QueryRunnerFactory
   * @see QueryRunner
   * @see DSFilterHandler
   * @see DSFilterHandlerFactory
   */
  public void execute(
    Query query,
    FormatSpec formatspec,
    OutputStream os,
    int limit)
    throws SequenceException, FormatException, InvalidQueryException, SQLException {

    Connection conn = getConnection();
    if (query.hasDomainSpecificFilters()) {
      DomainSpecificFilter[] dsfilters = query.getDomainSpecificFilters();
      for (int i = 0, n = dsfilters.length; i < n; i++) {
        DomainSpecificFilter dsf = dsfilters[i];
        DSFilterHandler dsfh =
          DSFilterHandlerFactory.getInstance(dsf.getObjectCode());
        query = dsfh.ModifyQuery(conn, dsf.getHandlerParameter(), query);
      }
    }

    logger.info(query);
    QueryRunner qr =
      QueryRunnerFactory.getInstance(query, formatspec, conn, os);
    qr.execute(limit);
  }


  public void connectToDatabase( String connectionString, String user, String password) throws SQLException {
    setConnectionString( connectionString );
    this.user= user;
    this.password = password;
    getConnection();
  }
  
  public String[] getDatabaseNames() throws SQLException {
    
    List databases = new ArrayList();
    
    ResultSet rs =
      getConnection().createStatement().executeQuery("show databases");
    while( rs.next() ) {
      databases.add( rs.getString( 1 ) );
    }
    
    return (String[]) databases.toArray(new String[databases.size()]);
  }

  /**
   * @return
   */
  public String getBaseConnectionString() {
    return baseConnectionString;
  }

  public Connection getConnection() throws SQLException {

    if (connection == null) {

      logger.info("Connecting to database: " + connectionString);
      connection = DriverManager.getConnection(connectionString, user, password);
    }
    return connection;
  }

  public String getDatabaseName() {
    return databaseName;
  }
  /**
   * @return
   */
  public String getDatabaseURL() {
    return connectionString;
  }

  /**
   * Returns a MartConfiguration object with all of the information needed to interact with
   * the mart defined by the connection parameters provided to this Engine.
   * 
   * @return MartConfiguration object
   */
  public MartConfiguration getMartConfiguration()
    throws ConfigurationException, SQLException {
    Connection conn = getConnection();
    // use default table for connection if database unset.
    // need this for the case where we connect to database server
    // but don't know what databases it contains.
    String db = (databaseName != null) ? databaseName : "mysql";
    return new MartConfigurationFactory().getInstance(conn, db);
  }

  /**
   * Overloaded method allowing user to supply an alternate xml configuration to use.  This configuration
   * must exist in the database, and must conform to the MartConfiguration.dtd.  Intended mostly for use by the Unit Test
   * ConfigurationTest.testMartConfiguration
   * 
   * @param system_id -- system_id of the alternate Configuration document
   * @return MartConfiguration object
   * @throws ConfigurationException
   */
  public MartConfiguration getMartConfiguration(String system_id)
    throws ConfigurationException, SQLException {
    Connection conn = getConnection();
    return new MartConfigurationFactory().getInstance(
      conn,
      databaseName,
      system_id);
  }

  /**
   * Overloaded method allowing user to supply an alternate xml configuration stored on the file system, as a URL.
   * This requires that the DTD for the document is also available from the file system.  Users should make sure that
   * the DOCTYPE declaration correctly locates the DTD for the document supplied.
   * 
   * @param martConfURL - URL for the supplied MartConfiguration xml document
   * @return MartConfiguration object for the mart defined by this document.
   * @throws ConfigurationException for all underlying Exceptions encountered during the attempt to process the request.
   */
  public MartConfiguration getMartConfiguration(URL martConfURL)
    throws ConfigurationException {
    return new MartConfigurationFactory().getInstance(martConfURL);
  }

  /**
   * @return
   */
  public String getUser() {
    return user;
  }

  /**
   * @param string
   */
  public void setBaseConnectionString(String string) {
    baseConnectionString = string;
  }

  /**
   * Convenience method for setting the database server URL and the database
   * The URL should match the standard jdbc connection string format.
   * Here are two examples:
   * <p>
   * EXAMPLE 1: Host URL only, no database specified.
   * <code>jdbc:mysql://kaka.sanger.ac.uk:3306</code>
   * </p>
   * <p>EXAMPLE 2: Database is set to "ensembl_15_1". 
   * <code>jdbc:mysql://kaka.sanger.ac.uk:3306/ensembl_mart_15_1</code>
   * </p>
   * @param URL database connection string.
   * @throws IllegalArgumentException if databaseURL has the wrong format.
   */
  public void setConnectionString(String connectionString)
    throws IllegalArgumentException {

    Pattern p = Pattern.compile("^(\\w+:\\w+://[^:/]+(:\\d+)?)(/(.*))?$");
    Matcher m = p.matcher(connectionString);
    if (m.matches()) {
      baseConnectionString = m.group(1);
      String tmpDatabase = m.group(4);
      if (tmpDatabase != null && tmpDatabase.length() > 0)
        databaseName = tmpDatabase;
    } else
      throw new IllegalArgumentException(
        "Invalid database URL:" + connectionString);

    this.connectionString = connectionString;

  }

  public void setDatabaseName(String database) {
    this.databaseName = database;
  }

  /**
   * @param string
   */
  public void setUser(String string) {
    user = string;
  }

  public String sql(Query query) {
    throw new RuntimeException();
  }

  /**
   * @return
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param string
   */
  public void setPassword(String string) {
    password = string;
  }

  /**
   * @param databaseType
   * @param host
   * @param port
   * @param databaseName
   */
  public void setConnectionString(
    String databaseType,
    String host,
    String port,
    String databaseName) {
    StringBuffer connStr = new StringBuffer();
    connStr.append("jdbc:").append( databaseType ).append("://");
    connStr.append(host);
    if (port != null && !"".equals(port))
      connStr.append(":").append(port);
    connStr.append("/");
    if (databaseName != null && !databaseName.equals(""))
      connStr.append( databaseName );

    setConnectionString(connStr.toString());

  }

}
