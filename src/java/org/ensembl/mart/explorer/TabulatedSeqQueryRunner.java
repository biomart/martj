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
public class TabulatedSeqQueryRunner implements QueryRunner {

	/**
	 * Constructs a TabulatedSeqQueryRunner object to execute a Query
	 * and print tabulated output specified by the given FormatSpec
	 * 
	 * @param query - a Query Object
	 * @param format - a FormatSpec object
     */
	public TabulatedSeqQueryRunner(Query query, FormatSpec format) {
	   this.query = query;
	   this.format = format;
	}

	/**
	 * Impliments the execute method of the interface.  For tabulated 
	 * sequence queries, the SQL is executed, and the ResultSet is written 
	 * to the OutputStream via a OutputStreamWriter.  Then the Sequence
	 * is printed as the last field in the tabulated output. Each field 
	 * of a ResultSet is separated by the separator defined in the FormatSpec 
	 * object in the output.
	 */
	public void execute(Connection conn, OutputStream os, int limit) throws SQLException, IOException, InvalidQueryException {
		OutputStreamWriter osr =  new OutputStreamWriter(os);
		SequenceDescription seqd = query.getSequenceDescription();
		SequenceAdaptor seqa = SequenceAdaptorFactory.createSequenceAdaptor( seqd.getSeqCode(), query.getSpecies(), query.getFocus(), conn );
		int[] displayIDindices = seqd.getDisplayIDAttributeIndices();
		Attribute[] attributes = query.getAttributes();

		CompiledSQLQuery csql = new CompiledSQLQuery( conn, query );
		String sql = csql.toSQL();
		if (limit > 0)
		    sql = sql+" limit "+limit;

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
             *     indices, and store them in a string.
             * - store the ResultSet data after the DisplayIDindices, present.
             * - get Sequence[] from Seqadaptor, and print each Sequence
             *   in tabulated form.
             * 
             */
			while ( rs.next() ) {
				int queryID = rs.getInt(seqd.getQueryIDIndex());
				if (idSeen.contains(new Integer(queryID))) continue; // skip non unique ids
				
				idSeen.add(new Integer(queryID));
				StringBuffer description = new StringBuffer();
				StringBuffer displayID = new StringBuffer();
				int nColumns = rs.getMetaData().getColumnCount();

                int currindex = 0;
                String separator = format.getSeparator();
                
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
					osr.write(sequences[i].getDisplayID()+separator+sequences[i].getDescription()+separator+sequences[i].getSequence()+"\n");
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
	
	private Logger logger = Logger.getLogger(TabulatedSeqQueryRunner.class.getName());
    private Query query = null;
    private FormatSpec format = null;
    private ArrayList idSeen = new ArrayList();
    private Sequence[] sequences;
}
