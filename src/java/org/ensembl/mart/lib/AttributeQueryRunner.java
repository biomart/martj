//TODO: test, and if necessary, revert to cvs version 1.7
package org.ensembl.mart.lib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Implimentation of the QueryRunner for executing a Query and 
 * generating Tabulated output.
 * Tabulated output is separated by a field separator specified by 
 * a FormatSpec object.  Any Query can generate tabulated output.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see Query
 * @see FormatSpec
 */
public final class AttributeQueryRunner implements QueryRunner {

  /**
   * Constructs a TabulatedQueryRunner object to execute a Query
   * and print tabulated output specified by the given FormatSpec
   * 
   * @param query - a Query Object
   * @param format - a FormatSpec object
   */
  public AttributeQueryRunner(Query query, FormatSpec format, OutputStream os) {
    this.query = query;
    this.format = format;
    this.osr = new PrintStream(os, true); // autoflush true
  }

  public void executeNEW(int limit) throws SequenceException, InvalidQueryException {
    //TODO: this should be moved into the Query object, so you can just do if (query.hasBigList())
    Filter[] filters = query.getFilters();

    Filter bigListFilter = null;
    String[] biglist = null;
    int numBigLists = 0;
    for (int i = 0, n = filters.length; i < n; i++) {
      Filter filter = filters[i];
      if (filter instanceof IDListFilter) {
        if (((IDListFilter) filter).getIdentifiers().length > listSizeMax) {
          if (numBigLists > maxBigListCount)
            throw new InvalidQueryException("Too many in list filters attached, only one per query supported.\n");

          bigListFilter = filter;
          biglist = ((IDListFilter) filter).getIdentifiers();
          numBigLists++;
        }
      }
    }

    if (limit < 1 && numBigLists > 0) {
      String[] idBatch = new String[listSizeMax];
      int batchIter = 0;

      for (int i = 0, n = biglist.length; i < n; i++) {
        String element = biglist[i];

        if ((i > 0) && ((i % listSizeMax) == 0)) {
          Query newQuery = new Query(query);
          newQuery.removeFilter(bigListFilter);

          IDListFilter newFilter =
            new IDListFilter(bigListFilter.getField(), bigListFilter.getTableConstraint(), idBatch);
          newQuery.addFilter(newFilter);

          executeQueryBatch(newQuery);

          idBatch = new String[listSizeMax];
          batchIter = 0;
        }
        idBatch[batchIter] = element;
        batchIter++;
      }

      //last batch is either empty, or less than idBatch.length
      if (idBatch[0] != null) {
        
        List lastBatch = new ArrayList();
        for (int i = 0, n = idBatch.length; i < n; i++) {
          String element = idBatch[i];
          if (element != null)
            lastBatch.add(element);
        }

        String[] lbatch = new String[lastBatch.size()];
        lastBatch.toArray(lbatch);
                  
        Query newQuery = new Query(query);
        newQuery.removeFilter(bigListFilter);

        IDListFilter newFilter = new IDListFilter(bigListFilter.getField(), bigListFilter.getTableConstraint(), lbatch);
        newQuery.addFilter(newFilter);

        executeQueryBatch(newQuery);
      }
    } else {
      executeSQLLimit(limit);
    }
  }

  protected void executeQueryBatch(Query newQuery) throws SequenceException, InvalidQueryException {
    attributes = newQuery.getAttributes();
    filters = newQuery.getFilters();

    Connection conn = null;
    String sql = null;
    try {
      csql = new CompiledSQLQuery(newQuery);
      sql = csql.toSQLWithKey();

      queryID = csql.getPrimaryKey();

      DataSource ds = newQuery.getDataSource();
      if (ds == null)
        throw new RuntimeException("newQuery.DataSource is null");
      conn = ds.getConnection();

      if (logger.isLoggable(Level.INFO)) {
        logger.info("QUERY : " + newQuery);
        logger.info("SQL : " + sql);
      }

      PreparedStatement ps = conn.prepareStatement(sql);

      int p = 1;
      for (int i = 0, n = filters.length; i < n; ++i) {
        Filter f = filters[i];
        String value = f.getValue();
        if (value != null) {
          logger.info("SQL (prepared statement value) : " + p + " = " + value);
          ps.setString(p++, value);
        }
      }

      ResultSet rs = ps.executeQuery();
      resultSetRowsProcessed = 0;

      processResultSet(conn, rs);

      rs.close();
    } catch (IOException e) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning("Couldnt write to OutputStream\n" + e.getMessage());
      throw new InvalidQueryException(e);
    } catch (SQLException e) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning(e.getMessage());
      throw new InvalidQueryException(e);
    } finally {
      DatabaseUtil.close(conn);
    }
  }

  protected void executeSQLLimit(int limit) throws SequenceException, InvalidQueryException {
    boolean moreRows = true;
    boolean userLimit = false;

    attributes = query.getAttributes();
    filters = query.getFilters();

    Connection conn = null;
    String sql = null;
    try {
      csql = new CompiledSQLQuery(query);
      String sqlbase = csql.toSQLWithKey();
      String primaryKey = csql.getQualifiedPrimaryKey();
      queryID = csql.getPrimaryKey();

      DataSource ds = query.getDataSource();
      if (ds == null)
        throw new RuntimeException("query.DataSource is null");
      conn = ds.getConnection();

      while (moreRows) {
        sql = sqlbase;

        if (sqlbase.indexOf("WHERE") >= 0)
          sql += " AND " + primaryKey + " >= " + lastID;
        else
          sql += " WHERE " + primaryKey + " >= " + lastID;

        sql += " ORDER BY " + primaryKey;

        if (logger.isLoggable(Level.INFO)) {
          logger.info("QUERY : " + query);
          logger.info("SQL : " + sql);
        }

        PreparedStatement ps = conn.prepareStatement(sql);

        if (limit > 0) {
          userLimit = true;
          ps.setMaxRows(limit);
          moreRows = false;
        } else
          ps.setMaxRows(batchLength);

        int p = 1;
        for (int i = 0, n = filters.length; i < n; ++i) {
          Filter f = query.getFilters()[i];
          String value = f.getValue();
          if (value != null) {
            logger.info("SQL (prepared statement value) : " + p + " = " + value);
            ps.setString(p++, value);
          }
        }

        ResultSet rs = ps.executeQuery();
        resultSetRowsProcessed = 0;

        processResultSet(conn, skipNewBatchRedundantRecords(rs));

        // on the odd chance that the last result set is equal in size to the batchLength, it will need to make an extra attempt.
        if ((!userLimit) && (resultSetRowsProcessed < batchLength))
          moreRows = false;

        if (batchLength < maxBatchLength) {
          batchLength =
            (batchLength * batchModifiers[modIter] < maxBatchLength)
              ? batchLength * batchModifiers[modIter]
              : maxBatchLength;
          modIter = (modIter == 0) ? 1 : 0;
        }

        rs.close();
      }
    } catch (IOException e) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning("Couldnt write to OutputStream\n" + e.getMessage());
      throw new InvalidQueryException(e);
    } catch (SQLException e) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning(e.getMessage());
      throw new InvalidQueryException(e);
    } finally {
      DatabaseUtil.close(conn);
    }
  }

  public void execute(int limit) throws SequenceException, InvalidQueryException {
    boolean moreRows = true;
    boolean userLimit = false;

    attributes = query.getAttributes();
    filters = query.getFilters();

    Connection conn = null;
    String sql = null;
    try {
      csql = new CompiledSQLQuery(query);
      String sqlbase = csql.toSQLWithKey();
      String primaryKey = csql.getQualifiedPrimaryKey();
      queryID = csql.getPrimaryKey();

      DataSource ds = query.getDataSource();
      if (ds == null)
        throw new RuntimeException("query.DataSource is null");
      conn = ds.getConnection();

      while (moreRows) {
        sql = sqlbase;

        if (sqlbase.indexOf("WHERE") >= 0)
          sql += " AND " + primaryKey + " >= " + lastID;
        else
          sql += " WHERE " + primaryKey + " >= " + lastID;

        sql += " ORDER BY " + primaryKey;

        if (logger.isLoggable(Level.INFO)) {
          logger.info("QUERY : " + query);
          logger.info("SQL : " + sql);
        }

        PreparedStatement ps = conn.prepareStatement(sql);

        if (limit > 0) {
          userLimit = true;
          ps.setMaxRows(limit);
          moreRows = false;
        } else
          ps.setMaxRows(batchLength);

        int p = 1;
        for (int i = 0, n = filters.length; i < n; ++i) {
          Filter f = query.getFilters()[i];
          String value = f.getValue();
          if (value != null) {
            logger.info("SQL (prepared statement value) : " + p + " = " + value);
            ps.setString(p++, value);
          }
        }

        ResultSet rs = ps.executeQuery();
        resultSetRowsProcessed = 0;

        processResultSet(conn, skipNewBatchRedundantRecords(rs));

        // on the odd chance that the last result set is equal in size to the batchLength, it will need to make an extra attempt.
        if ((!userLimit) && (resultSetRowsProcessed < batchLength))
          moreRows = false;

        if (batchLength < maxBatchLength) {
          batchLength =
            (batchLength * batchModifiers[modIter] < maxBatchLength)
              ? batchLength * batchModifiers[modIter]
              : maxBatchLength;
          modIter = (modIter == 0) ? 1 : 0;
        }

        rs.close();
      }
    } catch (IOException e) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning("Couldnt write to OutputStream\n" + e.getMessage());
      throw new InvalidQueryException(e);
    } catch (SQLException e) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning(e.getMessage());
      throw new InvalidQueryException(e);
    } finally {
      DatabaseUtil.close(conn);
    }
  }

  protected ResultSet skipNewBatchRedundantRecords(ResultSet rs) throws SQLException {
    if (lastID > -1) {
      //If lastID > -1, we know that there are 1 or more rows to skip before beginning to process again

      while ((resultSetRowsProcessed < lastIDRowsProcessed) && rs.next()) {
        //skip through rows already processed for a the last id, incrementing only resultSetRowsprocessed
        //This will only occur at the beginning of a new ResultSet batch
        resultSetRowsProcessed++;
      }
    }

    return rs;
  }

  private void processResultSet(Connection conn, ResultSet rs) throws IOException, SQLException {
    ResultSetMetaData rmeta = rs.getMetaData();

    int queryIDindex = 0;

    // process columnNames for required attribute indices
    for (int i = 1, nColumns = rmeta.getColumnCount(); i <= nColumns; ++i) {
      String column = rmeta.getColumnName(i);

      if (column.toLowerCase().equals(queryID.toLowerCase()))
        queryIDindex = i;
    }

    while (rs.next()) {
      int currID = rs.getInt(queryIDindex);

      if (lastID > -1 && lastID != currID) {
        lastIDRowsProcessed = 0;
      }

      for (int i = 1, nColumns = rs.getMetaData().getColumnCount(); i <= nColumns; ++i) {
        //skip the queryID
        if (i != queryIDindex) {
          if (i > 1)
            osr.print(format.getSeparator());
          String v = rs.getString(i);
          if (v != null)
            osr.print(v);
          //          else
          //            osr.print("NULL");
        }
      }
      osr.print("\n");

      if (osr.checkError())
        throw new IOException();

      lastID = currID;
      totalRows++;
      resultSetRowsProcessed++;
      lastIDRowsProcessed++;
    }
  }

  //batching 
  private final int[] batchModifiers = { 2, 2 };
  private int modIter = 0; //start at 0 
  private int batchLength = 50000;
  private final int maxBatchLength = 200000;
  //private int batchLength = 200000;

  //big list batching
  private final int listSizeMax = 1000;
  private final int maxBigListCount = 1;

  private String queryID = null;
  private int lastID = -1;
  private int totalRows = 0;
  private int resultSetRowsProcessed = 0; // will count rows processed for a given ResultSet batch
  private int lastIDRowsProcessed = 0;
  // will allow process to skip rows already processed in previous batch, for a given ID

  private Logger logger = Logger.getLogger(AttributeQueryRunner.class.getName());
  private Query query = null;
  private CompiledSQLQuery csql;
  private Attribute[] attributes = null;
  private Filter[] filters = null;
  private FormatSpec format = null;
  private PrintStream osr;
}
