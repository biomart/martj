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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Object for getting DNA Seqeuence Strings
 * from the Mart sgp_chunks table.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DNAAdaptor {

	/**
	 * DNAAdaptors require a database connection to get sequence from the mart database.
	 * 
	 * @param Connection
	 */
	public DNAAdaptor(Connection conn) {
		this.conn = conn;
	}
	
	/**
	 * For sequence operations working with smaller subsets of a sequence
	 * (usually portions of a single gene) this method allows an entire sequence
	 * to be fetched and cached.  Subsequen calls to getSequence will attempt 
	 * to work on this sequence, before fetching new sequence from the database.
	 *   
	 * @param species
	 * @param chr
	 * @param start
	 * @param end
	 */
	public void CacheSequence(String species, String chr, int start, int end) throws SequenceException {
		// if first time called, or required DNA is not from same big seg get a new big seg
	    if( (sql == null) || (! lastChr.equals(chr)) || (start < cachedSeqStart) || (start > cachedSeqEnd) || (end > cachedSeqEnd)) {
        List bytes = new ArrayList();
        
			  int tmp =  start - 1;
			  // exact coord of a chunk start
			  cachedSeqStart = tmp - (tmp % chunkSize) + 1;

        try {
			    sql = "select sequence from "+species+"_dna_chunks_support where chr_start between ? and ? and chr_name = ?";
		 	    PreparedStatement ps = conn.prepareStatement( sql );
			    ps.setInt(1, cachedSeqStart);
			    ps.setInt(2, end);
			    ps.setString(3, chr);

          //logger.info("SQL: "+sql+"\nparameter1="+cachedSeqStart+"\nparameter2="+end+"\nparameter3="+chr);
          
				  ResultSet rs = ps.executeQuery();
				  while ( rs.next() ) {
				     int nColumns = rs.getMetaData().getColumnCount();
				
				     if (nColumns > 0) {
				       for (int i = 1; i <= nColumns; ++i)
				         bytes.add(rs.getBytes(i));
				     }
				      else {
				        logger.error("No Sequence Returned for chromosome "+chr+"\n");
				      }              
				  }
					
          int seqLength = 0;
          for (int i = 0, n = bytes.size(); i < n; i++)
						seqLength += ( (byte[]) bytes.get(i)).length;
          
          cachedSeq = new byte[seqLength];
          int nextPos = 0;
          for (int i = 0, n = bytes.size(); i < n; i++) {
            byte[] theseBytes = (byte[]) bytes.get(i);
            System.arraycopy(theseBytes, 0, cachedSeq, nextPos, theseBytes.length);
            nextPos += theseBytes.length;
          }          
          
          bytes = null;
          System.gc(); // try to garbage collect some time after this method runs
          
				  lastChr = chr;
				  cachedSeqEnd = cachedSeqStart + cachedSeq.length - 1;
        } catch (SQLException e) {
            	throw new SequenceException("Could not cache Sequence for chromosome "+chr+" "+e.getMessage());
        }
	    }
	}
	
	/**
	 * Gets the Sequence for a given species, chr, start and end.
	 * Checks to see if there is cached sequence applicable for
	 * this query, and slices that sequence for its results.
	 * 
	 * @return String sequence
	 */
	public String getSequence(String species, String chr, int start, int end) throws SequenceException{
	  CacheSequence(species, chr, start, end); // may not do anything if cachedSeq is sufficient
	    
		int len = (end - start) + 1;
		if (cachedSeq.length < 1) {
			logger.warn("failed to get DNA for chr "+chr+"\n");
			return Npad(len);
		}
		
	  // cut out the requested section from the big segment
		int seqstart = start - cachedSeqStart;
		int seqend = seqstart + len;
		int cacheLen = cachedSeq.length;
		
    byte[] retBytes = null;
		//user may ask for more sequence than is available, return as much as possible
		if (len > cacheLen - seqstart -1) {
      
      logger.info("Warning, not enough sequence to satisfy request, returning as much as possible\n");
		  
      retBytes = new byte[cacheLen - seqstart + 1];
      System.arraycopy(cachedSeq, seqstart, retBytes, 0, retBytes.length);		  
    } else {
      retBytes = new byte[len];
      System.arraycopy(cachedSeq, seqstart, retBytes, 0, len);
    }
    
    return new String(retBytes);   
	}
	
	/**
	 * returns a string of "N"s of a given length
	 * @param length
	 * @return
	 */
	private String Npad(int length) {
		StringBuffer nseq = new StringBuffer();
		for (int i = 0; i < length; i++)
		   nseq.append("N");
		   
		 return nseq.toString();
	}

	// variables to determine if we need to fetch more sequence for a given request
	private String lastChr = null;
	private int cachedSeqStart = 0;
	private int cachedSeqEnd = 0;
	private byte[] cachedSeq = null; // will cache a sequence after calls to CacheSequence for use by getSequence
	
	private Logger logger = Logger.getLogger(DNAAdaptor.class.getName());	
    private Connection conn;
    private String sql = null;
    private int chunkSize = 100000; // Size of dna chunks in sgp_chunks table
}
