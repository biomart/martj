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
        connStr.append( query.getHost() );
        String p = query.getPort();
        if ( p != null && !"".equals(p) )
            connStr.append(":").append( p );
        connStr.append("/");
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
    
    
    ResultSet rs = null;
    try {
      ps.executeQuery();
    } catch (SQLException e) {
      logger.warn(e.getMessage()+ " : " + sql);
      throw e;
    }
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

    public String[] databases(Query q) throws SQLException {
        ArrayList tables = new ArrayList();
        Connection conn = getDatabaseConnection( q );
        ResultSet rs = conn.createStatement().executeQuery("show databases");
        while (rs.next()) {
            tables.add(rs.getString(1));
        }
        return toStringArray( tables );
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


    public String[] columns(Query q, String table) throws SQLException {
        ArrayList columns = new ArrayList();
        Connection conn = getDatabaseConnection( q );
        ResultSet rs = conn.createStatement().executeQuery("describe "+table);
        while (rs.next()) {
          columns.add( rs.getString(1) );
        }
        return toStringArray( columns );
    }



  /**
   * Creates mappers from database. First mappers correspond to single
   * tables, the last one to all tables (useful when joins needed).
   */
  public ColumnMapper[] mappers( Query query )  throws SQLException {

    List tables = new ArrayList();
    List mappers = new ArrayList();

    String[] tableNames = tables( query );
    Arrays.sort( tableNames ); // this makes the focus table first.
    
    String baseName = query.getSpecies() + "_core_" + query.getFocus();
    logger.debug("Filtering tables beginning with " + baseName 
                 + "(total num tables = "+tableNames.length+")");

    Connection conn = createConnection( query );

    // We create a mapper for each table. These can be used when no joins are
    // necessary.
    for (int i=0; i<tableNames.length; ++i) {
      
      String tableName = tableNames[i];
      List cols = new ArrayList();
      
      if ( tableName.startsWith( baseName ) ) { // ignore irrelevant tables.
        
        ResultSet rs = conn.createStatement().executeQuery("describe " + tableName);

        while ( rs.next() ) 
          cols.add( rs.getString(1) );
        
        String[] colArray = toStringArray( cols );
        Table table = new Table( tableName
                                 ,colArray
                                 ,baseName);
        tables.add( table );
        // create mapper for single table;
        mappers.add( new ColumnMapper( new Table[]{ table }) );
        
      }
      
    }

    // Create join mapper.
    Table[] tableArr = new Table[ tables.size() ];
    tables.toArray( tableArr );
    ColumnMapper joinMapper = new ColumnMapper( tableArr );
    mappers.add( joinMapper );

    ColumnMapper[] mapperArr 
      = (ColumnMapper[])mappers.toArray( new ColumnMapper[ mappers.size() ] );

    return mapperArr;
  }

  private String[] toStringArray( List list ) {
    return (String[])list.toArray(new String[ list.size() ]);
  }
}
