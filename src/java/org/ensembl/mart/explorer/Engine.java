package org.ensembl.mart.explorer;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.log4j.*;

public class Engine {
    private Logger logger = Logger.getLogger(Engine.class.getName());
    private Connection databaseConnection = null;
    private Connection serverConnection = null;
    private Engine engine;
    private String host;
    private String port;
    private String user;
    private String password;
    private String database;

    public void init(String host, String port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public Connection getDatabaseConnection( Query query) {
/*
        if (databaseConnection == null) {
            if (database == null)
                throw new IllegalStateException("database not set.");
            databaseConnection = createConnection(database);
        }
        return databaseConnection;
*/
			return createConnection( query );
    }

    public Connection getServerConnection() {
        if (serverConnection == null)
            serverConnection = createConnection(null);
        return serverConnection;
    }

    /**
     * Creates connection to database server. If database is set the connection
     * is to the specific database on the server, otherwise it is to the server itself.
     */
    public Connection createConnection(Query query) {
        StringBuffer connStr = new StringBuffer();
        connStr.append("jdbc:mysql://");
        connStr.append( query.getHost() ).append("/");
        String p = query.getPort();
        if ( p != null && !"".equals(p) )
            connStr.append(":").append( p );
        String db = query.getDatabase();
        if ( db != null && !db.equals(""))
            connStr.append( db );
        else
            connStr.append("mysql"); // default table - we have to connect to one table
        connStr.append("?autoReconnect=true");
        Connection conn = null;
        try {
            Class.forName("org.gjt.mm.mysql.Driver").newInstance();
            logger.info(connStr.toString());
            conn = DriverManager.getConnection(connStr.toString(), query.getUser(), query.getPassword() );
        } catch (Exception e) {
            logger.error("failed to connect to " + connStr.toString(), e);
            throw new RuntimeException(e.getMessage());
        }
        return conn;
    }

    public void execute(Query query)
      throws SQLException, FormatterException, InvalidQueryException {

      logger.warn( "Executing query : " + query );


      init( query.getHost(), query.getPort(), query.getUser(), query.getPassword() );
      CompiledSQLQuery csql = new CompiledSQLQuery( query );
      String sql = csql.toSQL();
      logger.warn( "SQL = " +sql );
      Connection conn = getDatabaseConnection( query );
      ResultSet rs = conn.createStatement().executeQuery( sql );
      query.getResultTarget().output( rs );
    }

    public OutputStream execute(Query query, Formatter formatter) {
        throw new RuntimeException();
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

    public List tables(Query q) throws SQLException {
        ArrayList tables = new ArrayList();
        Connection conn = getDatabaseConnection( q );
        ResultSet rs = conn.createStatement().executeQuery("show tables");
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        return tables;
    }

    public List databases() throws SQLException {
        ArrayList tables = new ArrayList();
        Connection conn = getServerConnection();
        ResultSet rs = conn.createStatement().executeQuery("show databases");
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        return tables;
    }
}
