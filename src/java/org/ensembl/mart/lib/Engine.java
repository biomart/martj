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
    private Logger logger = Logger.getLogger(Engine.class.getName());
    private Connection databaseConnection = null;
    //private Connection serverConnection = null;
    //private Engine engine;
    private String host;
    private String port;
    private String user;
    private String password;
    private String database;
    
    /**
     * Creates an Engine object for a connection to a specific mart
     * database running on a specific mySQL database instance.
     * 
     * @param host - the mySQL host
     * @param port - the mySQL port
     * @param user - the mySQL user
     * @param password - the mySQL password (can be null)
     * @param database - the name of the mart database
     */
    public Engine (String host, String port, String user, String password, String database) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
    }

    private Connection getDatabaseConnection() {
        if (databaseConnection == null) {
            if (database == null)
                throw new IllegalStateException("database not set.");

			databaseConnection = createConnection( true );
		}
        return databaseConnection;
    }
   
    /**
     * Creates connection to database server. If getdb is true, resulting connection
     * is to the specific database on the server, otherwise it is to the server itself.
     */
    private Connection createConnection(boolean getdb) {
        StringBuffer connStr = new StringBuffer();
        connStr.append("jdbc:mysql://");
        connStr.append( host );

        if ( port != null && !"".equals(port) )
            connStr.append(":").append( port );
        connStr.append("/");

        if ( getdb && database != null && !database.equals(""))
            connStr.append( database );
        else
            connStr.append("mysql"); // default table - we have to connect to one table
        connStr.append("?autoReconnect=true");
        Connection conn = null;
        try {
            Class.forName("org.gjt.mm.mysql.Driver").newInstance();
            logger.info(connStr.toString());
            conn = DriverManager.getConnection(connStr.toString(), user, password );
        } catch (Exception e) {
            logger.error("failed to connect to " + connStr.toString(), e);
            throw new RuntimeException(e.getMessage());
        }
        return conn;
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
        throws FormatException, SequenceException, InvalidQueryException {
    
          execute( query, formatspec, os, 0 );
          
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
	public void execute(Query query, FormatSpec formatspec, OutputStream os, int limit) 
		throws SequenceException, FormatException, InvalidQueryException {
    
			Connection conn = getDatabaseConnection();
      if (query.hasDomainSpecificFilters()) {
      	DomainSpecificFilter[] dsfilters = query.getDomainSpecificFilters();
      	for (int i = 0, n = dsfilters.length; i < n; i++) {
					DomainSpecificFilter dsf = dsfilters[i];
					DSFilterHandler dsfh = DSFilterHandlerFactory.getInstance(dsf.getObjectCode());
					query = dsfh.ModifyQuery(conn, dsf.getHandlerParameter(), query);
				}
      }
        
    	logger.info(query);
		  QueryRunner qr = QueryRunnerFactory.getInstance(query, formatspec, conn, os);
		  qr.execute(limit);
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
	public MartConfiguration getMartConfiguration(String system_id) throws ConfigurationException {
		Connection conn = getDatabaseConnection();
		return new MartConfigurationFactory().getInstance(conn, database, system_id);
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
	public MartConfiguration getMartConfiguration(URL martConfURL) throws ConfigurationException {
	    return new MartConfigurationFactory().getInstance(martConfURL);
	}
	
	/**
	 * Returns a MartConfiguration object with all of the information needed to interact with
	 * the mart defined by the connection parameters provided to this Engine.
	 * 
	 * @return MartConfiguration object
	 */
	public MartConfiguration getMartConfiguration() throws ConfigurationException {
		Connection conn = getDatabaseConnection();
		return new MartConfigurationFactory().getInstance(conn, database);
	}

    public int countRows(Query query) {
        throw new RuntimeException();
    }

    public int countFocus(Query query) {
        throw new RuntimeException();
    }

    public String sql(Query query) {
        throw new RuntimeException();
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
