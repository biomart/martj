/*
 * Created on Apr 16, 2003
 *
 */
package org.ensembl.mart.explorer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">dlondon</a>
 */
public class FastaSeqQueryRunner implements QueryRunner {

	/**
	 * Constructs a FastaSeqQueryRunner object to execute a Query
	 * and print FASTA sequence output
	 * 
	 * @param query - a Query Object
	 * @param format - a FormatSpec object
     */
	public FastaSeqQueryRunner(Query query, FormatSpec format) {
		this.query = query;
		this.format = format;
	}

	/**
	 * Impliments the execute method of the interface.  For FASTA 
	 * sequence queries, the SQL is executed, and the ResultSet is parsed
	 * for the header information, which is written to the OutputStream
	 * as a Fasta header line.  The sequence is then created, and written
	 * to the OutputStream.  All OutputStream writes are mediated
	 * by an OutputStreamWriter object.
	 */
	public void execute(Connection conn, OutputStream os) throws SQLException, IOException, InvalidQueryException {
		OutputStreamWriter osr =  new OutputStreamWriter(os);
		SequenceDescription seqd = query.getSequenceDescription();
		SequenceAdaptor seqa = SequenceAdaptorFactory.createSequenceAdaptor( seqd.getSeqCode(), query.getSpecies(), query.getFocus(), conn );
		int[] displayIDindices = seqd.getDisplayIDAttributeIndices();
		Attribute[] attributes = query.getAttributes();

		CompiledSQLQuery csql = new CompiledSQLQuery( conn, query );
		String sql = csql.toSQL();

		logger.info( "QUERY : " + query );
		logger.info( "SQL : " +sql );

		try {
			
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

			/*
			 * For each ResultSet:
			 * - get the QueryID using the SequenceDescription QueryIDindex. 
			 * - get the DisplayIDindices from the SequenceDescription, 
			 *     iterate over the display Ids in the ResultSet using these 
			 *     indices, and print them to the OutputStreamWriter.
			 * - print the ResultSet data after the DisplayIDindices, if present.
			 * - print the sequence description last
			 * - get and print the sequence using the SequenceAdaptor.
			 */
			while ( rs.next() ) {
                int queryID = rs.getInt(seqd.getQueryIDIndex());
	            if (idSeen.contains(new Integer(queryID))) continue; // skip non unique ids
				
	            idSeen.add(new Integer(queryID));
	            StringBuffer description = new StringBuffer();
	            StringBuffer displayID = new StringBuffer();
	            int nColumns = rs.getMetaData().getColumnCount();
	            int currindex = 0;
                
	            for (int i = 0; i < displayIDindices.length; i++) {
	            	if (i>0) displayID.append( separator );
		            currindex = displayIDindices[i];
		            if ( rs.getString(currindex) != null )
			            displayID.append( rs.getString(currindex) );
	            }
				
	           // currindex is now the last index of the DisplayIDs.  Increment it, and iterate over the rest of the ResultSet to print the description
	           int septest = currindex;
	           for (currindex++; currindex <= nColumns; ++currindex) {
	           	   if (currindex - septest > 1) description.append( separator );
		           if (rs.getString(currindex) != null )
			           description.append( attributes[currindex-1].getName()+"="+rs.getString(currindex) );
	           }
				
			   description.append( separator+seqd.getDescription());
	           // get the sequences
	           sequences = seqa.getSequences(queryID, displayID.toString(), description.toString(), separator, seqd.getLeftFlank(), seqd.getRightFlank());
				
	           for (int i = 0; i < sequences.length; i++) {
		           osr.write(sequences[i].toString()+"\n");
		           osr.flush();
	           }
            }
            osr.close();
		}
		catch (IOException e) {
			logger.warn("Couldnt write to OutputStream\n"+e.getMessage());
			throw e;
		} 
		catch (SQLException e) {
			logger.warn(e.getMessage()+ " : " + sql);
			throw e;
		}		
    }

	private Logger logger = Logger.getLogger(FastaSeqQueryRunner.class.getName());    
    private Query query = null;
    private FormatSpec format = null;
	private Sequence[] sequences;
	private ArrayList idSeen = new ArrayList();
	private final String separator = "|"; 
}
