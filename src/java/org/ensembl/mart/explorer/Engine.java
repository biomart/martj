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

    public Connection getDatabaseConnection() {
        if (databaseConnection == null) {
            if (database == null)
                throw new IllegalStateException("database not set.");
            databaseConnection = createConnection(database);
        }
        return databaseConnection;
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
    public Connection createConnection(String database) {
        StringBuffer connStr = new StringBuffer();
        connStr.append("jdbc:mysql://");
        connStr.append(host).append("/");
        if (port != null)
            connStr.append(":").append(port);
        if (database != null && !database.equals(""))
            connStr.append(database);
        else
            connStr.append("mysql"); // default table - we have to connect to one table
        connStr.append("?autoReconnect=true");
        Connection conn = null;
        try {
            Class.forName("org.gjt.mm.mysql.Driver").newInstance();
            logger.info(connStr.toString());
            conn = DriverManager.getConnection(connStr.toString(), user, password);
        } catch (Exception e) {
            logger.error("failed to connect to " + connStr.toString(), e);
            throw new RuntimeException(e.getMessage());
        }
        return conn;
    }

    public void execute(Query query) {
      logger.warn( "Pretending to execute query : " + query );

    }

    public OutputStream execute(Query query, ResultRenderer renderer) {
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

    public List tables() throws SQLException {
        ArrayList tables = new ArrayList();
        Connection conn = getDatabaseConnection();
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
