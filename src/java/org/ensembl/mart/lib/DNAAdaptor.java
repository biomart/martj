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
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Object for getting DNA Seqeuence Strings
 * from the Mart sgp_chunks table.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DNAAdaptor {

	private Logger logger = Logger.getLogger(DNAAdaptor.class.getName());
	private Connection conn;

	private final String SPECSQL = "select mart_species from meta_release_info";
	private int chunkSize = 100000; // Size of dna chunks in sgp_chunks table

	//some prepared statements for later use
	private PreparedStatement specStmt;
	private Hashtable specSQLFull = new Hashtable();
	private Hashtable specSQLSub = new Hashtable();

	/**
	 * DNAAdaptors require a database connection to get sequence from the mart database.
	 * 
	 * @param Connection
	 */
	public DNAAdaptor(Connection conn) throws SequenceException {
		this.conn = conn;

		try {
			specStmt = conn.prepareStatement(SPECSQL);
			populateSpeciesSQL();
		} catch (SQLException e) {
			throw new SequenceException("Could not initialize DNAAdaptor Species Statements: " + e.getMessage(), e);
		}
	}

	private void populateSpeciesSQL() throws SQLException {
		ResultSet rs = specStmt.executeQuery();

		while (rs.next()) {
			String species = rs.getString(1);
			String sqlFull = "select sequence from " + species + "__dna_chunks__supp where chr_start = ? and chr_name = ?";
			String sqlSub = "select substring(sequence, ?, ?) from " + species + "__dna_chunks__supp where chr_start = ? and chr_name = ?";

			specSQLFull.put(species, conn.prepareStatement(sqlFull));
			specSQLSub.put(species, conn.prepareStatement(sqlSub));
		}
		rs.close();
	}

  private byte[] fetchFullChunk(String species, String chr, int start) throws SequenceException {
    if (!specSQLFull.containsKey(species))
      throw new SequenceException("Species " + species + " is not a supported species\n");
      
    try {
      PreparedStatement ps = (PreparedStatement) specSQLFull.get(species);
      ps.setInt(1, start);
      ps.setString(2, chr);

      ResultSet rs = ps.executeQuery();
      rs.next();

      byte[] ret = rs.getBytes(1);
      rs.close();
      return ret;
    } catch (SQLException e) {
      throw new SequenceException("Could not fetch full sequence chunk " + e.getMessage(), e);
    }          
  }
  
  private byte[] fetchChunkSubstring(String species, String chr, int start, int chunkStart, int length) throws SequenceException {
    if (!specSQLSub.containsKey(species))
      throw new SequenceException("Species " + species + " is not a supported species\n");
    
    try {
      int coord = start - chunkStart + 1;
        
      PreparedStatement ps = (PreparedStatement) specSQLSub.get(species);
      ps.setInt(1, coord);
      ps.setInt(2, length);
      ps.setInt(3, chunkStart);
      ps.setString(4, chr);

      ResultSet rs = ps.executeQuery();
      rs.next();

      byte[] ret = rs.getBytes(1);
      rs.close();
      return ret;
    } catch (SQLException e) {
      throw new SequenceException("Could not fetch chunk substring " + e.getMessage(), e);
    }
  }
  
  private byte[] fetchSequence(String species, String chr, int start, int length) throws SequenceException {
    int chunkStart = start - ( ( start - 1 ) % chunkSize );
    
    if (start == chunkStart && length == chunkSize)
      return fetchFullChunk(species, chr, chunkStart);
    else
      return fetchChunkSubstring(species, chr, start, chunkStart, length);
  }

  private byte[] fetchResidualSequence(String species, String chr, int start, int length, byte[] initialSeq) throws SequenceException {
    List bytes = new ArrayList();
    bytes.add(initialSeq);
    int currentLength = initialSeq.length;
    int currentStart = start + currentLength;
    
    while (currentLength < length) {
      int residual = length - currentLength;
      byte[] currentBytes = fetchSequence(species, chr, currentStart, residual);
      
      if (currentBytes.length < 1)
        break;
      
      bytes.add(currentBytes);
      currentLength += currentBytes.length;
      currentStart = start + currentLength;
    }
    
    //iterate through bytes to fill sequence byte[]
    byte[] sequence = new byte[currentLength];
    int nextPos = 0;
    for (int i = 0, n = bytes.size(); i < n; i++) {
      byte[] thisChunk = (byte[]) bytes.get(i);
      System.arraycopy(thisChunk, 0, sequence, nextPos, thisChunk.length);
      nextPos += thisChunk.length;
    }

    bytes = null;
    return sequence;
  }
  
	/**
	 * Gets the Sequence for a given species, chr, start and end.
	 * Checks to see if there is cached sequence applicable for
	 * this query, and slices that sequence for its results.
	 * 
	 * @return String sequence
	 */
	public byte[] getSequence(String species, String chr, int start, int end) throws SequenceException {
		int len = (end - start) + 1;
  	byte[] retBytes = fetchSequence(species, chr, start, len);
    
    if (retBytes.length < 1) {
      if (logger.isLoggable(Level.INFO))
        logger.info("No Sequence Returned for request: species = " + species + ", chromosome = " + chr + ", start = " + start + " end = " + end + "\n");
      return Npad(len);
    }
    
    if (retBytes.length < len)
      retBytes = fetchResidualSequence(species, chr, start, len, retBytes);
    
		//user may ask for more sequence than is available, return as much as possible
		if (retBytes.length < len && logger.isLoggable(Level.INFO))
			logger.info("Warning, not enough sequence to satisfy request: requested " + len + " returning " + retBytes.length + "\n");

		return retBytes;
	}

	/**
	 * returns a byte[] of "N"s of a given length
	 * @param length
	 * @return
	 */
	private byte[] Npad(int length) {
		byte[] nseq = new byte[length];
		for (int i = 0; i < length; i++)
			nseq[i] = 'N';

		return nseq;
	}
}
