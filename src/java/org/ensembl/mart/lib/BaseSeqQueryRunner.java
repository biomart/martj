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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ensembl.util.FormattedSequencePrintStream;

/** 
* @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
* @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
*/
public abstract class BaseSeqQueryRunner implements QueryRunner {

  protected int maxColumnLen = 80;

  //batching numbers
  protected final int[] batchModifiers = { 5, 2 };
  protected int modIter = 0; //start at 0 
  protected int batchLength = 1000;
  protected final int maxBatchLength = 50000;

  protected String separator;
  private Logger logger = Logger.getLogger(BaseSeqQueryRunner.class.getName());

  protected Query query = null;
  protected Attribute[] attributes = null;
  protected Filter[] filters = null;
  protected SequenceDescription seqd = null;
  protected String dataset = null;
  protected String species = null;
  protected String focus = null;
  protected String dset = null;
  protected String structureTable = null;
  protected FormatSpec format = null;
  protected FormattedSequencePrintStream osr;
  protected SeqWriter seqWriter;

  protected int totalRows = 0;
  protected int resultSetRowsProcessed = 0; // will count rows processed for a given ResultSet batch
  protected int lastID = -1;
  protected int lastIDRowsProcessed = 0;
  // will allow process to skip rows already processed in previous batch, for a given ID

  protected TreeMap iDs = new TreeMap();
  // holds each objects information, in order
  protected List fields = new ArrayList();
  // holds unique list of resultset description fields from the query

  // Used for colating required fields
  protected String queryID;
  protected String coordStart, coordEnd;
  protected List displayIDs = new ArrayList();
  protected final String GENEID = "gene_id_key";
  protected final String TRANID = "transcript_id_key";
  protected final String RANK = "rank";
  protected final String CHR = "chr_name";
  protected final String ASSEMBLYCOLUMN = "assembly_type";
  protected final String STRANDCOLUMN = "exon_chrom_strand";

  // Strings for use in idattribute Hashtable keys
  protected final String ASSEMBLY = "assembly";
  protected final String DISPLAYID = "displayID";
  protected final String DESCRIPTION = "description";

  // need to know these indexes specifically
  protected int queryIDindex = 0;
  protected int tranIDindex = 0;
  protected int rankIndex = 0;
  protected int assemblyIndex = 0;
  protected int startIndex = 0;
  protected int endIndex = 0;
  protected int chromIndex = 0;
  protected int strandIndex = 0;
  protected List displayIDindices = new ArrayList();
  protected List otherIndices = new ArrayList();

  /**
   * This method should set the required variables queryID, coordStart, and coordEnd to
   * the values necessary for the type of sequence being processed, then 
   * add any other displayID fields required for output, etc. to the displayIDs List, 
   * and finally update the Query object with attributes that are necessary to get 
   * the sequence data.
   */
  protected abstract void updateQuery();

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryRunner#execute(int)
   */
  public void execute(int limit) throws SequenceException, InvalidQueryException {
    execute(limit, false);
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

  /**
   * This Method should first calculate the indices of the various fields that it requires,
   * using the ResultSet Object ResultSetMetaData.  It should then iterate through each of the
   * results in the ResultSet, processing them.  While processing, if it should encounter a new 
   * keyID, it should write out the results from the lastID, and reset lastIDRowsProcessed to zero.
   * It should end this loop by incrementing the totalRows, resultSetRowsProcessed and lastIDRowsProcessed
   * integers, and setting the lastID to the current keyID.intValue.
   * 
   * @param conn
   * @param rs
   * @throws IOException
   * @throws SQLException
   */
  protected abstract void processResultSet(Connection conn, ResultSet rs) throws IOException, SQLException;

  protected void writeLastEntry(Connection conn) throws SequenceException {
    // write the last transcripts data, if present
    if (lastID > -1)
      seqWriter.writeSequences(new Integer(lastID), conn);
  }

  protected abstract class SeqWriter {
    abstract void writeSequences(Integer tranID, Connection conn) throws SequenceException;
  }
  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryRunner#execute(int, boolean)
   */
  public void execute(int limit, boolean isSubQuery) throws SequenceException, InvalidQueryException {
     if (isSubQuery)
       throw new SequenceException("SubQuerys cannot return sequences\n");
     
    boolean moreRows = true;
    boolean userLimit = false;

    updateQuery();

    attributes = query.getAttributes();
    filters = query.getFilters();
    seqd = query.getSequenceDescription();

    Connection conn = null;
    String sql = null;
    try {
      conn = query.getDataSource().getConnection();

      CompiledSQLQuery csql = new CompiledSQLQuery(query);
      String sqlbase = csql.toSQL();

      String structure_table = dset + "__structure__dm";

      while (moreRows) {
        sql = sqlbase;

        if (lastID > -1) {
          if (sqlbase.indexOf("WHERE") >= 0)
            sql += " and " + structure_table + "." + queryID + " >= " + lastID;
          else
            sql += " WHERE " + structure_table + "." + queryID + " >= " + lastID;
        }

        sql += " order by  "
          + structure_table
          + "."
          + GENEID
          + ", "
          + structure_table
          + "."
          + TRANID
          + ", "
          + structure_table
          + "."
          + RANK;

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
        for (int i = 0; i < filters.length; ++i) {
          Filter f = filters[i];
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
          batchLength *= batchModifiers[modIter];
          modIter = (modIter == 0) ? 1 : 0;
        }

        rs.close();
      }

      writeLastEntry(conn);
      conn.close();
    } catch (IOException e) {
      throw new SequenceException(e);
    } catch (SQLException e) {
      throw new InvalidQueryException(e + " :" + sql);
    } finally {
      DetailedDataSource.close(conn);
    }  
  }

}
