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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.ensembl.util.SequenceUtil;

/**
 * This object prints out Transcripts )Exons and Introns) in fasta format.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public final class FastaTranscriptEISeqQueryRunner implements QueryRunner {

    /**
     * Constructs a FastaTranscriptEISeqQueryRunner object to print 
     *  Transcripts (exons and introns) in fasta format.
     * 
     * @param query
     * @param format
     * @param conn
     * @param os
     */
    public FastaTranscriptEISeqQueryRunner(Query query, FormatSpec format, Connection conn, OutputStream os) {
		this.query = query;
		this.format = format;
		this.conn = conn;
		this.os = os;
	}
 
	private void updateQuery() {
		query.addAttribute(new FieldAttribute(queryID));
		query.addAttribute(new FieldAttribute(Rank));
		query.addAttribute(new FieldAttribute(AssemblyColumn));
		query.addAttribute(new FieldAttribute(coordStart));
		query.addAttribute(new FieldAttribute(coordEnd));
		query.addAttribute(new FieldAttribute(Chr));
		query.addAttribute(new FieldAttribute(StrandColumn));
		
		for (int i=0; i< displayIDs.size(); i++) {
			query.addAttribute( new FieldAttribute( (String) displayIDs.get(i) ) );
		}
	}
    
	public void execute(int limit)
		throws SQLException, IOException, InvalidQueryException {
			OutputStreamWriter osr =  new OutputStreamWriter(os);
			SequenceDescription seqd = query.getSequenceDescription();
			dna = new DNAAdaptor(conn);

			// need to know these indexes specifically
			int queryIDindex = 0;
			int rankIndex = 0;
			int assemblyIndex = 0;
			int startIndex = 0;
			int endIndex = 0;
			int chromIndex = 0;
			int strandIndex = 0;
			List displayIDindices = new ArrayList();
			List otherIndices = new ArrayList();
        
			queryID = "transcript_id";
			coordStart = "exon_chrom_start";
			coordEnd = "exon_chrom_end";
			displayIDs.add("transcript_stable_id_v");
			displayIDs.add("gene_stable_id_v");
			updateQuery();
				
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
				ResultSetMetaData rmeta = rs.getMetaData();
						
				// process columnNames for required attribute indices
				for (int i = 1, nColumns = rmeta.getColumnCount(); i <= nColumns; ++i) {
					String column = rmeta.getColumnName(i);
					if (column.equals(queryID))
						queryIDindex = i;
					else if (column.equals(Rank))
						rankIndex = i;
					else if (column.equals(AssemblyColumn))
						assemblyIndex = i;
					else if (column.equals(coordStart))
						startIndex = i;
					else if (column.equals(coordEnd))
						endIndex = i;
					else if (column.equals(Chr))
						chromIndex = i;
					else if (column.equals(StrandColumn))
						strandIndex = i;
					else if (displayIDs.contains(column))
						displayIDindices.add(new Integer(i));
					else
						otherIndices.add(new Integer(i));
				}
			
				while ( rs.next() ) {
					Integer tranID = new Integer(rs.getInt(queryIDindex));
					Integer rank = new Integer(rs.getInt(rankIndex));
				
					if (! traniDs.containsKey(tranID)) { 
						Hashtable atts = new Hashtable();
						atts.put(Assembly, rs.getString(assemblyIndex));
					    traniDs.put(tranID, atts);
					}
				
					Hashtable atts = (Hashtable) traniDs.get(tranID);
				
					int start = rs.getInt(startIndex);
					if (start > 0) {
						// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
						String chr = rs.getString(chromIndex);
						int end = rs.getInt(endIndex);
						int strand = rs.getInt(strandIndex);
						
						// keep track of the lowest start and highest end for the gene, for caching
						if (! (atts.containsKey(Geneloc)) )
						    atts.put(Geneloc, new SequenceLocation(chr, start, end, strand));
						else {
							SequenceLocation geneloc = (SequenceLocation) atts.get(Geneloc);
							if (start < geneloc.getStart())
							    atts.put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand));
							if (end > geneloc.getEnd())
							    atts.put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand));
						}
						
                        // keep track of the lowest start and highest end for the transcript
						if (! (atts.containsKey(Location) ) )
						    atts.put(Location , new SequenceLocation(chr, start, end, strand));
						else {
							SequenceLocation tranloc = (SequenceLocation) atts.get(Location);
							if (start < tranloc.getStart())
							    atts.put(Location , new SequenceLocation(chr, start, tranloc.getEnd(), strand));
							if (end > tranloc.getEnd())
							atts.put(Location , new SequenceLocation(chr, tranloc.getStart(), end, strand));
						}
					}

					//	process displayID, if necessary
					if (! (atts.containsKey(DisplayID)  ) ) {								
						StringBuffer displayID = new StringBuffer();
                
						for (int i = 0, n = displayIDindices.size(); i < n; i++) {
							if (i>0) displayID.append( separator );
							int currindex = ((Integer) displayIDindices.get(i)).intValue();
							if ( rs.getString(currindex) != null )
								displayID.append( rs.getString(currindex) );
						}
				
						atts.put(DisplayID, displayID.toString());
					}
                   
					// Rest can be duplicates, or novel values for a given field, collect lists of values for each field
					// currindex is now the last index of the DisplayIDs.  Increment it, and iterate over the rest of the ResultSet to print the description
				
					for (int i = 0, n = otherIndices.size(); i < n; i++) {
						int currindex = ((Integer) otherIndices.get(i)).intValue();
						if (rs.getString(currindex) != null ) { 					    
							String field = attributes[currindex-1].getName();
							if (! fields.contains(field)) 
								fields.add(field);
						
							String value = rs.getString(currindex);
							
							if ( atts.containsKey(field) ) {
								if (! ((ArrayList) atts.get(field)).contains(value))
									((ArrayList) atts.get(field)).add(value);
							}
							else {
								ArrayList values = new ArrayList();
								values.add(value); 
								atts.put(field, values);
							}
						}
					}
				
					// add the description, if necessary
					if (! atts.containsKey(Description) )
						atts.put( Description, separator+seqd.getDescription() );
				}
			
				// run through the idS list, make and print the header, then get and print the sequences from the locations
			
				for ( Iterator tranIDiter = traniDs.keySet().iterator(); tranIDiter.hasNext(); ) {
					Hashtable atts = (Hashtable) traniDs.get((Integer) tranIDiter.next());
				
					// write the header, starting with the displayID
					String displayIDout = (String) atts.get(DisplayID);
					osr.write(">"+displayIDout);
					
					// cache the gene seq, if necessary (note, this doesnt do anything if the location has already been cached)
					SequenceLocation geneloc = (SequenceLocation) atts.get(Geneloc);
					dna.CacheSequence(query.getSpecies(), geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());
					
					SequenceLocation tranloc = (SequenceLocation) atts.get(Location);
					String strandout = tranloc.getStrand() > 0 ? "forward" : "revearse";
					String assemblyout = (String) atts.get(Assembly);
					osr.write("\tstrand="+strandout+separator+"chr="+tranloc.getChr()+separator+"assembly="+assemblyout);
					osr.flush();
					
					for (int j = 0, n = fields.size(); j < n; j++) {
						osr.write(separator);
						String field = (String) fields.get(j);
						if (atts.containsKey(field)) {
							ArrayList values = (ArrayList) atts.get(field);
							
							if (values.size() > 1)
								osr.write(field+" in ");
							else
								osr.write(field+"=");
							    
							for (int vi = 0; vi < values.size(); vi++) {
								if (vi > 0) osr.write(",");
								osr.write((String) values.get(vi));
							}
						}
						else
							osr.write(field+"= ");
						osr.flush();
					}
					
					osr.write(separator+(String) atts.get(Description));
					osr.write("\n");
					osr.flush();
        
                    //extend flanks, if necessary, and write sequence
                    if (seqd.getLeftFlank() > 0)
                        tranloc = tranloc.extendLeftFlank(seqd.getLeftFlank());
                    if (seqd.getRightFlank() > 0)
                        tranloc = tranloc.extendRightFlank(seqd.getRightFlank());
                        
			        if (tranloc.getStrand() < 0)
						osr.write( SequenceUtil.reverseComplement( dna.getSequence(query.getSpecies(), tranloc.getChr(), tranloc.getStart(), tranloc.getEnd()) ) );
					else
						osr.write( dna.getSequence(query.getSpecies(), tranloc.getChr(), tranloc.getStart(), tranloc.getEnd()) );
					osr.flush();
					osr.write("\n");
					osr.flush();
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

	 private final String separator = "|";		
	 private Logger logger = Logger.getLogger(FastaCodingSeqQueryRunner.class.getName());
	 private Query query = null;
	 private FormatSpec format = null;
	 private OutputStream os = null;
	 private Connection conn = null;
	
	 private TreeMap traniDs = new TreeMap(); // holds each objects information, in order
	 private List fields = new ArrayList(); // holds unique list of resultset description fields from the query
	 private DNAAdaptor dna;
    
	 // Used for colating required fields
	 private String queryID;
	 private String coordStart, coordEnd;
	 private List displayIDs = new ArrayList();
	 private final String Rank = "rank";
	 private final String Chr =  "chr_name";
	 private final String AssemblyColumn = "assembly_type";
	 private final String StrandColumn = "exon_chrom_strand";
    
	 // Strings for use in idattribute Hashtable keys
	 private final String Location = "location";
	 private final String Assembly = "assembly";
	 private final String Strand = "strand";	
	 private final String Geneloc = "geneloc";
	 private final String DisplayID = "displayID";
	 private final String Description = "description";	
}
