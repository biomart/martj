package org.ensembl.mart.explorer;

import java.io.*;
import java.sql.*;
import org.apache.log4j.*;

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
    private QueryRunnerFactory qrunnerfactory = new QueryRunnerFactory();

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

 /*   private Connection getServerConnection() {
        if (serverConnection == null)
            serverConnection = createConnection( false );
        return serverConnection;
    }
    removed by dml
    */
    
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

    public void execute(Query query, FormatSpec formatspec, OutputStream os) 
        throws SQLException, IOException, FormatException, InvalidQueryException {
    
          Connection conn = getDatabaseConnection();
	      QueryRunner qr = qrunnerfactory.createQueryRunner(query, formatspec);
          qr.execute(conn, os);
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
