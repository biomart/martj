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
import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.ensembl.util.SequenceUtil;

/**
 * Outputs Transcript Exon sequences in fasta format
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public final class FastaTExonSeqQueryRunner implements QueryRunner {
	/**
	 * Constructs a FastaTExonSeqQueryRunner object to execute a Query
	 * and print fasta output of Exon Sequences for each transcript.
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param conn a java.sql.Connection object
	 * @param os an OutputStream object
	 */
	public FastaTExonSeqQueryRunner(Query query, FormatSpec format, Connection conn, OutputStream os) {
		this.query = query;
		this.format = format;
		this.conn = conn;
		this.os = os;		
	}

	private void updateQuery() {
		query.addAttribute(new FieldAttribute(queryID));
		query.addAttribute(new FieldAttribute(TranscriptID));
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

	private void writeSequence() throws SequenceException, IOException {
		OutputStreamWriter osr =  new OutputStreamWriter(os);
		
		try {
			// run through the geneiDs list, for each transcript-exon, make and print output
			for (Iterator geneIter = geneiDs.keySet().iterator(); geneIter.hasNext();) {
				Hashtable geneatts = (Hashtable) geneiDs.get( (Integer) geneIter.next() );
				SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
				String assemblyout = (String) geneatts.get(Assembly);
				
				// cache the gene sequence
				dna.CacheSequence(query.getSpecies(), geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());
				
				for (Iterator tranIter = ( (TreeMap) geneatts.get(Transcripts) ).keySet().iterator(); tranIter.hasNext();) {
					Hashtable tranatts = (Hashtable) ( (TreeMap) geneatts.get(Transcripts) ).get(tranIter.next());
					
					for (Iterator exonIter = ( (TreeMap) tranatts.get(Exons) ).keySet().iterator(); exonIter.hasNext();) {
						Hashtable exonatts = (Hashtable) ( (TreeMap) (tranatts.get(Exons) )).get(exonIter.next());
						SequenceLocation exonloc = (SequenceLocation) exonatts.get(Location);
						
						// write the header, starting with the displayID
						osr.write(">"+(String) exonatts.get(DisplayID));
					
						String strandout = exonloc.getStrand() > 0 ? "forward" : "revearse";
						osr.write("\tstrand="+strandout+separator+"chr="+exonloc.getChr()+separator+"assembly="+assemblyout);
						osr.flush();
					
						for (int j = 0, n = fields.size(); j < n; j++) {
							osr.write(separator);
							String field = (String) fields.get(j);
							if (exonatts.containsKey(field)) {
								List values = (ArrayList) exonatts.get(field);
							
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
					
						// write the description
						osr.write(separator+(String) exonatts.get(Description));
						osr.write("\n");
						osr.flush();
					
						//extend flanking sequence if necessary
						int lflank = query.getSequenceDescription().getLeftFlank();
						int rflank = query.getSequenceDescription().getRightFlank();
					
						if (lflank > 0)
						   exonloc = exonloc.extendLeftFlank(lflank);
						if (rflank > 0)
						   exonloc = exonloc.extendRightFlank(rflank);
					
						// write out the sequence
						if (exonloc.getStrand() < 0)
							osr.write( SequenceUtil.reverseComplement( dna.getSequence(query.getSpecies(), exonloc.getChr(), exonloc.getStart(), exonloc.getEnd()) ) );
						else
							osr.write( dna.getSequence(query.getSpecies(), exonloc.getChr(), exonloc.getStart(), exonloc.getEnd()) );
						osr.flush();
						osr.write("\n");
						osr.flush();
					}
				}
			}
			osr.close();			
		} catch (SequenceException e) {
		    logger.warn(e.getMessage());
		    throw e;	
	    } catch (IOException e) {
		    logger.warn("Couldnt write to OutputStream\n"+e.getMessage());
		    throw e;	
	    }
	}
		    
	public void execute(int limit) throws SQLException, IOException, InvalidQueryException {
		SequenceDescription seqd = query.getSequenceDescription();
		dna = new DNAAdaptor(conn);

		// need to know these indexes specifically
		int queryIDindex = 0;
		int tranIDindex = 0;
		int rankIndex = 0;
		int assemblyIndex = 0;
		int startIndex = 0;
		int endIndex = 0;
		int chromIndex = 0;
		int strandIndex = 0;
		List displayIDindices = new ArrayList();
		List otherIndices = new ArrayList();
        
		queryID = "gene_id";
		coordStart = "exon_chrom_start";
		coordEnd = "exon_chrom_end";
		displayIDs.add("exon_stable_id_v");
		displayIDs.add("gene_stable_id_v");			    
		displayIDs.add("transcript_stable_id_v");
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
			for( int i=0, n=query.getFilters().length; i<n; ++i) {
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
			    else if (column.equals(TranscriptID))
				    tranIDindex = i;
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
				Integer geneID = new Integer(rs.getInt(queryIDindex));
				Integer tranID = new Integer(rs.getInt(tranIDindex));
				Integer rank = new Integer(rs.getInt(rankIndex));
				
				if (! geneiDs.containsKey(geneID)) { 
					Hashtable atts = new Hashtable();

//					order the transcripts by their rank in ascending order					
					atts.put(Transcripts, new TreeMap());
					atts.put(Assembly, rs.getString(assemblyIndex));
					geneiDs.put(geneID, atts);
				}
				
				Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
				TreeMap ordtrans = (TreeMap) geneatts.get(Transcripts);
				if (! (ordtrans.containsKey(tranID)))
				  ordtrans.put(tranID, new Hashtable());
				
				int start = rs.getInt(startIndex);
				if (start > 0) {
					// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
					String chr = rs.getString(chromIndex);
					int end = rs.getInt(endIndex);
					int strand = rs.getInt(strandIndex);

					Hashtable tranatts = (Hashtable) ordtrans.get(tranID);
                    						
					// keep track of the lowest start and highest end for the gene
					if (! ( geneatts.containsKey(Geneloc) ) ) {
						geneatts.put(Geneloc, new SequenceLocation(chr, start, end, strand));
					}
					else {
						SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
						if (start < geneloc.getStart())
							geneatts.put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand)); // overwrite the previous copy
						if (end > geneloc.getEnd())
							geneatts.put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand)); // overwrite the previous copy
					}
					
					// store each exon in rank order
					if (!( tranatts.containsKey(Exons))) {
						TreeMap exonMap = new TreeMap();
					    tranatts.put(Exons, new TreeMap());
					}
					
					// need to add this exon to the Exons
					Hashtable exonatts = new Hashtable();
					exonatts.put(Location, new SequenceLocation(chr, start, end, strand));
					( (TreeMap) ( tranatts.get(Exons) ) ).put(rank, exonatts);  
				}

				Hashtable exonatts = (Hashtable) ( (TreeMap) ((Hashtable) ordtrans.get(tranID)).get(Exons) ).get(rank); // just need the information for this exon
				
				//	process displayID, if necessary
				if (! ( exonatts.containsKey(DisplayID) )) {								
					StringBuffer displayID = new StringBuffer();
                
					for (int i = 0, n = displayIDindices.size(); i < n; i++) {
						if (i>0) displayID.append( separator );
						int currindex = ((Integer) displayIDindices.get(i)).intValue();
						if ( rs.getString(currindex) != null )
							displayID.append( rs.getString(currindex) );
					}
				
					exonatts.put(DisplayID, displayID.toString());
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
						
						if (exonatts.containsKey(field)) {
							if (! ( (ArrayList) exonatts.get(field) ).contains(value))
								( (ArrayList) exonatts.get(field) ).add(value);
						}
						else {
							List values = new ArrayList();
							values.add(value); 
							exonatts.put(field, values);
						}
					}
				}
				
				// add the description, if necessary
				if (! exonatts.containsKey(Description))
					exonatts.put( Description, separator+seqd.getDescription() );
			}
		    writeSequence();
		} catch (SQLException e) {
			logger.warn(e.getMessage()+ " : " + sql);
			throw e;
		}
	}
	
	private final String separator = "|";
	private Logger logger = Logger.getLogger(FastaTExonSeqQueryRunner.class.getName());
	private Query query = null;
	private FormatSpec format = null;
	private OutputStream os = null;
	private Connection conn = null;
	
	private TreeMap geneiDs = new TreeMap(); // holds each objects information, in order
	private List fields = new ArrayList(); // holds unique list of resultset description fields from the query
	private DNAAdaptor dna;
    
	// Used for colating required fields
	private String queryID;
	private String coordStart, coordEnd;
	private List displayIDs = new ArrayList();
	private final String TranscriptID = "transcript_id";
	private final String Rank = "rank";
	private final String Chr =  "chr_name";
	private final String AssemblyColumn = "assembly_type";
	private final String StrandColumn = "exon_chrom_strand";
    
	// Strings for use in idattribute Hashtable keys
	private final String Exons = "exons";
	private final String Transcripts = "transcripts";
	private final String Assembly = "assembly";
	private final String Strand = "strand";	
	private final String Geneloc = "geneloc";
	private final String Location = "location";
	private final String DisplayID = "displayID";
	private final String Description = "description";
}

