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
    
    init( query.getHost(), query.getPort(), query.getUser(), query.getPassword() );
    CompiledSQLQuery csql = new CompiledSQLQuery( query, this );
    String sql = csql.toSQL();
    Connection conn = getDatabaseConnection( query );

    logger.info( "QUERY : " + query );
    logger.info( "SQL : " +sql );

    PreparedStatement ps = conn.prepareStatement( sql );
    int p=1;
    for( int i=0; i<query.getFilters().length; ++i) {
      Filter f = query.getFilters()[i];
      String value = f.getValue();
      if ( value!=null ) {
        logger.info("SQL (prepared statement value) : "+p+" = " + value);
        ps.setString( p++, value);
      }
    }
    
    
    ResultSet rs = ps.executeQuery();
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

    public String[] tables(Query q) throws SQLException {
        ArrayList tables = new ArrayList();
        Connection conn = getDatabaseConnection( q );
        ResultSet rs = conn.createStatement().executeQuery("show tables");
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        return toStringArray( tables );
    }

    public String[] databases(Query q) throws SQLException {
        ArrayList tables = new ArrayList();
        Connection conn = getDatabaseConnection( q );
        ResultSet rs = conn.createStatement().executeQuery("show databases");
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        return toStringArray( tables );
    }


  public Properties columnToTable( Query query ) throws SQLException {

    Properties col2Table = new Properties();

    Connection conn = getDatabaseConnection( query );
    
    String[] tables = tables( query );

    // sort by length so that we have central tables before satelite
    // tables. This means that for columns that appear in multiple tables
    // (e.g. gene_stable_id) we use always use the one from the central
    // table.
    Arrays.sort( tables );

    String base = query.getSpecies() + "_core_" + query.getFocus();

    logger.debug("Filtering tables beginning with " + base + "(total num tables = "+tables.length+")");

    for (int i=0; i<tables.length; ++i) {

      String table = tables[i];

      if ( table.startsWith( base ) ) {

        logger.debug("loading table : " + table);
        ResultSet rs = conn.createStatement().executeQuery("describe " + table);
        while ( rs.next() ) {
          String col = rs.getString(1);
          if ( !col2Table.containsKey( col ) ) 
            col2Table.put( col, table);
        }
      }
      else {
        logger.debug("ignoring table : " + table);
      }
    }

    return col2Table;
  }


  private String[] toStringArray( List list ) {
    return (String[])list.toArray(new String[ list.size() ]);
  }
}
