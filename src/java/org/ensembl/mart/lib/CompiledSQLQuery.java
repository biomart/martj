/* Generated by Together */

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ensembl.util.StringUtil;

/**
 * Compiles a Query object into SQL.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class CompiledSQLQuery {

  /**
   * Constructs a CompiledSQLQuery object with a specified mySQL
   * database Connection, and a Query object
   * 
   * @param conn
   * @param query
   * @throws SQLException
   * @see Query
   */
  public CompiledSQLQuery(Query query) throws SQLException {
    this.query = query;
    createMappers();
  }

  /**
   * 
   * @return Query query
   */
  public Query getQuery() {
    return query;
  }

  /**
   * creates a String SQL statement suitable for preparing in
   * a PreparedStatement using a mySQL Connection.
   * 
   * @return String SQL - SQL to be executed
   * @throws InvalidQueryException
   */
  public String toSQL() throws InvalidQueryException {
    // select gene_stable_id from homo_sapiens_core_gene where
    // chromosome_id="3" limit 3;
    if (sql == null)
      compileSQL();
    return sql;
  }

  /**
   * creates a String SQL statement suitable for preparing in
   * a PreparedStatement using a mySQL Connection, whereby the
   * last item in the select clause is the primaryKey for the 
   * entire SQL query (eg, used in any join fields, or determined from
   * the single table beign queried).
   * 
   * @return String SQL - SQL to be executed
   * @throws InvalidQueryException
   */
  public String toSQLWithKey() throws InvalidQueryException {
    // select gene_stable_id from homo_sapiens_core_gene where
    // chromosome_id="3" limit 3;
    if (pksql == null)
      compileSQL();
    return pksql;
  }

  /**
   * Returns the primary key determined during SQL generation.
   * @return String primary key used in join clauses
   * @throws InvalidQueryException when the toSQL() has not been called,
   * this method will call it, which might result in an InvalidQueryException
   */
  public String getPrimaryKey() throws InvalidQueryException {
    if (sql == null)
      compileSQL();
    return primaryKey;
  }

  /**
   * Returns the primary key, qualified with the mainTable.
   * @return String mainTable+"."+primaryKey
   * @throws InvalidQueryException same as getPrimaryKey
   */
  public String getQualifiedPrimaryKey() throws InvalidQueryException {
    if (sql == null)
      compileSQL();
    return qualifiedPrimaryKey;
  }

  /**
   * Returns the table used in the SQL query, which may be a single
   * dimension table, if the system optimized to this, or the main
   * table in any main - dimension table combination. 
   * @return String Main Table Name
   * @throws InvalidQueryException when the toSQL() method has not been called,
   * this method will call it, which might result in an InvalidQueryException
   */
  public String getMainTable() throws InvalidQueryException {
    if (sql == null)
      compileSQL();
    return mainTable;
  }

  private void compileSQL() throws InvalidQueryException {

    boolean success = false;
    StringBuffer buf = new StringBuffer();

    for (int m = 0; m < mappers.length && !success; ++m) {
      buf.delete(0, buf.length());
      mainTable = null;
      primaryKey = null;

      success = selectClause(buf, mappers[m]);

      if (success) {
        if (logger.isLoggable(Level.FINE))
          logger.fine("select clause:" + buf.toString());

        success = fromClause(buf, mappers[m]);
      }

      if (success) {
        if (logger.isLoggable(Level.FINE))
          logger.fine("select + from clauses:" + buf.toString());

        success = whereClause(buf, mappers[m]);
      }

      if (success && logger.isLoggable(Level.FINE))
        logger.fine("select + from + where clauses:" + buf.toString());

    }
    if (!success)
      throw new InvalidQueryException("Failed to compile query :" + query);

    sql = buf.toString();

    //generate pksql
    StringBuffer pkbuf = new StringBuffer(sql);
    pkbuf.insert(sql.indexOf(FROM), ", " + qualifiedPrimaryKey);
    pksql = pkbuf.toString();

    if (logger.isLoggable(Level.INFO)) {
      logger.info("SQL: " + sql + "\n");
      logger.info("PKSQL: " + pksql + "\n");
      logger.info("MainTable: " + mainTable + "\n");
      logger.info("PrimaryKey: " + primaryKey + "\n");
    }
  }

  /**
   * @return true if all attributes in the query could be mapped to tables by
   * the mapper, otherwise false.
   */
  private boolean selectClause(StringBuffer buf, FieldMapper mapper) throws InvalidQueryException {

    final int nAttributes = query.getAttributes().length;

    if (nAttributes == 0)
      throw new InvalidQueryException("No attributes selected.");

    buf.append(SELECT).append(" ");

    for (int i = 0; i < nAttributes; ++i) {

      Attribute a = query.getAttributes()[i];
      if (!mapper.canMap(a))
        return false;
      buf.append(mapper.qualifiedName(a));

      if (i + 1 < nAttributes)
        buf.append(", ");
    }

    return true;
  }

  /**
   * Builds array of "from" tables by looking at all the columns mentioned in
   * the queries attributes and filters.
   *
   * @return true if all attributes and filter 'columns' in the query could
   * be mapped to tables by the mapper, otherwise false.
   */
  private boolean fromClause(StringBuffer buf, FieldMapper mapper) throws InvalidQueryException {

    Set relevantTables = new HashSet();

    for (int i = 0; i < query.getAttributes().length; ++i) {
      Attribute attribute = query.getAttributes()[i];
      //			String colName = attribute.getField();
      if (!mapper.canMap(attribute))
        return false;

      relevantTables.add(mapper.tableName(attribute));
    }

    for (int i = 0; i < query.getFilters().length; ++i) {
      Filter filter = query.getFilters()[i];
      //			String colName = filter.getField();
      if (!mapper.canMap(filter))
        return false;

      relevantTables.add(mapper.tableName(filter));
    }

    fromTables = new String[relevantTables.size()];
    relevantTables.toArray(fromTables);

    buf.append(FROM);
    for (int i = 0; i < fromTables.length; ++i) {
      if (i > 0)
        buf.append(" , ");

      buf.append(fromTables[i]);

      if (mainTable == null || (!(mainTable.endsWith("_main")) && fromTables[i].endsWith("_main")))
        mainTable = fromTables[i];
    }

    return true;
  }

  /**
   * @return true if all filter condition 'columns' in the query could be mapped to tables by
   * the mapper, otherwise false.
   */
  private boolean whereClause(StringBuffer buf, FieldMapper mapper) throws InvalidQueryException {

    final int nFilters = query.getFilters().length;

    if (nFilters > 0 || fromTables.length > 1)
      buf.append(WHERE);

    boolean and = false;

    // Add user defined filters to where clause
    if (nFilters != 0) {

      for (int i = 0; i < nFilters; ++i) {

        Filter f = query.getFilters()[i];
        // don't need this next check because already checked in "fromClause"
        // but leave incase this method is ever called without calling that
        // method previously.
        if (!mapper.canMap(f))
          return false;

        if (and)
          buf.append(" AND ");
        buf.append(mapper.qualifiedName(f)).append(f.getRightHandClause()).append(" ");
        and = true;
      }
    }

    // get mainTable and primaryKey
    primaryKey = mapper.getPrimaryKey();
    qualifiedPrimaryKey = mainTable + "." + primaryKey;

    // Add joins to where clause
    if (fromTables.length > 1) {

      // Join from (first) central table to dimenstion tables.
      for (int i = 0; i < fromTables.length; i++) {
        if (!(fromTables[i].equals(mainTable))) {
          if (and)
            buf.append(" AND ");
          and = true;

          buf.append(mainTable).append(".").append(primaryKey).append("=").append(fromTables[i]).append(".").append(
            primaryKey);
        }
      }
    }

    return true;
  }

  /**
   * Determines if value is in array.
   * @param array values to search
   * @param value value to search for
   * @return true if value in array, otherwise false.
   */
  //	private boolean contains(Object[] array, Object value) {
  //		return value.equals(array[Arrays.binarySearch(array, value)]);
  //	}

  /**
    * Creates mappers from database. First mappers correspond to single
    * tables, the last one to all tables (useful when joins needed).
    */
  private void createMappers() throws SQLException {

    mappers = FieldMapperCache.instance.cachedMappers(query);
    if (mappers != null)
      return;

    List dimensionMappers = new ArrayList();
    List mainMappers = new ArrayList();
    List joinMappers = new ArrayList();

    List dimensionTables = new ArrayList();
    List mainTables = new ArrayList();

    Table[] tables = TableCache.instance.get(query);
    String[] starNames = query.getStarBases();
		String[] primaryKeys = query.getPrimaryKeys();
		
    // Get all relevant dimension tables and create a mapper for each
    for (int i = 0; i < tables.length; i++) {
      Table table = tables[i];
      String tableName = table.name;
      for (int j = 0; j < starNames.length; j++) {
        String starName = starNames[j];
        if (tableName.toLowerCase().startsWith(starName.toLowerCase()) && tableName.toLowerCase().endsWith("_dm")) {
        	String thisPrimaryKey = null;
        	
        	//find the first primary key that maps to this table
        	for (int k = 0, n = primaryKeys.length; k < n && (thisPrimaryKey == null); k++) {
            for (int l = 0, m = table.columns.length; l < m && (thisPrimaryKey == null); l++) {
              if (primaryKeys[k].toLowerCase().equals(table.columns[l].toLowerCase())) thisPrimaryKey = table.columns[l];
            }
          }
        	
          dimensionMappers.add(new FieldMapper(new Table[] { table }, thisPrimaryKey));
          dimensionTables.add(table);
          break;
        }
      }
    }

    //	sort so that we can use Arrays.binarySearch() later	
    Arrays.sort(tables);

    for (int i = 0; i < primaryKeys.length; i++) {
      String primaryKey = primaryKeys[i];
      for (int j = 0; j < starNames.length; j++) {
        String starName = starNames[j];
        String tableName = starName + "_main";

        // Find a mapper for each star's "main" table
        Table table = Table.findTable(tableName, tables);
        if (table == null)
          throw new RuntimeException(
            "Failed to find a table in database called: "
              + tableName
              + ". Known databases are: ["
              + StringUtil.toString(tables)
              + "]");
        mainMappers.add(new FieldMapper(new Table[] { table }, primaryKey));
        mainTables.add(table);

        // Create a mapper for each main containing main + all dm tables
        // store them separately in _joinMappers_ so that we can add them
        // to the end of _mappersList_ 		
        ArrayList mainAndDimensionTables = new ArrayList();
        mainAndDimensionTables.add(table);
        mainAndDimensionTables.addAll(dimensionTables);
        Table[] tableArray = new Table[mainAndDimensionTables.size()];
        mainAndDimensionTables.toArray(tableArray);
        joinMappers.add(new FieldMapper(tableArray, primaryKey));

      }
    }

    List mappersList = new ArrayList();
    mappersList.addAll(mainMappers);
    mappersList.addAll(dimensionMappers);
    mappersList.addAll(joinMappers);

    mappers = (FieldMapper[]) mappersList.toArray(new FieldMapper[mappersList.size()]);

    FieldMapperCache.instance.cacheMappers(query, mappers);

    //		StringBuffer buf = new StringBuffer();
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Num mappers = " + mappers.length);
    }
  }

  //	private String[] toStringArray(List list) {
  //		return (String[]) list.toArray(new String[list.size()]);
  //	}

  private String sql = null;
  private String pksql = null;
  private Query query = null;
  private Logger logger = Logger.getLogger(CompiledSQLQuery.class.getName());
  private String mainTable = null; // either the _main table, or the single dimension table when that is chosen
  private String primaryKey = null; // whichever primary_key supplied by the query is used in the SQL
  private String qualifiedPrimaryKey = null; // mainTable + "." + primaryKey
  private String[] fromTables = null;
  private FieldMapper[] mappers = null;

  //SQL keywords
  private final String SELECT = "SELECT ";
  private final String FROM = " FROM ";
  private final String WHERE = " WHERE ";
}
