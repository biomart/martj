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
 * Outputs peptide sequence in one of the supported output format
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported output formats
 */
public final class UpStreamUTRSeqQueryRunner implements QueryRunner {
	
	/**
	 * Constructs a PeptideSeqQueryRunner object to execute a Query
	 * and print Peptide Sequences
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param conn a java.sql.Connection object
	 * @param os an OutputStream object
	 */	
	public UpStreamUTRSeqQueryRunner(Query query, FormatSpec format, Connection conn, OutputStream os) {
		this.query = query;
		this.format = format;
		this.conn = conn;
		this.osr = new OutputStreamWriter(os);
		this.dna = new DNAAdaptor(conn);
	    
		switch (format.getFormat()) {
		  case FormatSpec.TABULATED:
				this.separator = format.getSeparator();
			  this.seqWriter = tabulatedWriter;
			  break;
	    	    
		  case FormatSpec.FASTA:
			  this.separator = "|";
		    this.seqWriter = fastaWriter;
		    break;
		}
	}
	
	private void updateQuery() {
		query.addAttribute(new FieldAttribute(queryID));
		query.addAttribute(new FieldAttribute(GeneID));
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
	
	public void execute(int limit) throws SequenceException, InvalidQueryException {
		SequenceDescription seqd = query.getSequenceDescription();

		// need to know these indexes specifically
		int queryIDindex = 0;
		int geneIDindex = 0;
		int rankIndex = 0;
		int assemblyIndex = 0;
		int startIndex = 0;
		int endIndex = 0;
		int chromIndex = 0;
		int strandIndex = 0;
		List displayIDindices = new ArrayList();
		List otherIndices = new ArrayList();
        
		queryID = "transcript_id";
		coordStart = "5utr_start";
		coordEnd = "5utr_end";
		displayIDs.add("transcript_stable_id_v");
		displayIDs.add("gene_stable_id_v");
		updateQuery();
				
		Attribute[] attributes = query.getAttributes();

	try {
		  CompiledSQLQuery csql = new CompiledSQLQuery( conn, query );
		  String sql = csql.toSQL();
		  
		  String structure_table = query.getSpecies()+"_core_"+query.getFocus()+"_structure";
		  sql += " order by  "+structure_table+".gene_id, "+structure_table+".transcript_id, "+structure_table+".rank";
		
		  if (limit > 0)
			  sql += " limit "+limit;

		  logger.info( "QUERY : " + query );
		  logger.info( "SQL : " +sql );
		  
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
			  else if (column.equals(GeneID))
				geneIDindex = i;
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

	  Integer lastGene = new Integer(0); // will hold last gene, when a new one is encountered
			while ( rs.next() ) {
				Integer geneID = new Integer(rs.getInt(geneIDindex));
				Integer tranID = new Integer(rs.getInt(queryIDindex));
				Integer rank = new Integer(rs.getInt(rankIndex));

				if (! ( geneiDs.containsKey(geneID) ) ) {
					if (geneiDs.size() > 0) { 
//					process the previous tranID, if this isnt the first time through, then refresh the traniDs TreeMap
					  seqWriter.writeSequences(lastGene);
					  geneiDs = new TreeMap();
					}
					lastGene = geneID;					 
					Hashtable geneatts = new Hashtable();
					
					geneatts.put(Assembly, rs.getString(assemblyIndex));
					geneatts.put(Transcripts, new TreeMap());
					geneiDs.put(geneID, geneatts);
				}
				
				Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
				
				TreeMap traniDs = (TreeMap) geneatts.get(Transcripts);

				if (! ( traniDs.containsKey(tranID) ) ) {
		  Hashtable tranatts = new Hashtable();
		  tranatts.put(Locations, new TreeMap());
		  tranatts.put(hasUTR, Boolean.FALSE); // initialize to falst, over ride if necessary
		  traniDs.put(tranID, tranatts);					
				}
				Hashtable tranatts = (Hashtable) traniDs.get(tranID);
				
				int start = rs.getInt(startIndex);
				if (start > 0) {
					// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
					tranatts.put(hasUTR, Boolean.TRUE);
					
					String chr = rs.getString(chromIndex);
					int end = rs.getInt(endIndex);
					int strand = rs.getInt(strandIndex);
						
					//	order the locations by their rank in ascending order
					((TreeMap) tranatts.get(Locations)).put(rank, new SequenceLocation(chr, start, end, strand));
						
					// keep track of the lowest start and highest end for the gene	
					if (! ( geneatts.containsKey(Geneloc) ) ) {
						geneatts.put(Geneloc, new SequenceLocation(chr, start, end, strand));
					}
					else {
						SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
						if (start < geneloc.getStart()) {
							geneatts.put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand)); // overwrite the previous copy
						if (end > geneloc.getEnd())
							geneatts.put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand)); // overwrite the previous copy
						}
					}
				}

				//	process displayID, if necessary
				if (! ( tranatts.containsKey(DisplayID) ) ) {								
					StringBuffer displayID = new StringBuffer();
                
					for (int i = 0, n = displayIDindices.size(); i < n; i++) {
						if (i>0) displayID.append( separator );
						int currindex = ((Integer) displayIDindices.get(i)).intValue();
						if ( rs.getString(currindex) != null )
							displayID.append( rs.getString(currindex) );
					}
				
					tranatts.put(DisplayID, displayID.toString());
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
						
						if ( tranatts.containsKey(field) ) {
							if (! ((ArrayList) tranatts.get(field)).contains(value))
								((ArrayList) tranatts.get(field)).add(value);
						}
						else {
							List values = new ArrayList();
							values.add(value); 
							tranatts.put(field, values);
						}
					}
				}
				
				// add the description, if necessary
				if (! ( tranatts.containsKey(Description) ) )
					tranatts.put( Description, seqd.getDescription() );
			}
		  // write the last transcripts data
		seqWriter.writeSequences(lastGene);
		osr.close();
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new InvalidQueryException(e);
	} catch (SQLException e) {
			logger.warn(e.getMessage());
			throw new SequenceException(e);
		}
	}

	// SeqWriter object
	SeqWriter seqWriter; 
	abstract class SeqWriter {
		abstract void writeSequences(Integer geneID) throws SequenceException;
	}
	
	private final SeqWriter tabulatedWriter = new SeqWriter() {
		  void writeSequences(Integer geneID) throws SequenceException {
			Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
			TreeMap traniDs = (TreeMap) geneatts.get(Transcripts);
			  SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
			  String assemblyout = (String) geneatts.get(Assembly);
			
			try {
			  for (Iterator tranIter = traniDs.keySet().iterator(); tranIter.hasNext();) {
					Hashtable tranatts = (Hashtable) traniDs.get( (Integer) tranIter.next() );

					osr.write((String) tranatts.get(DisplayID));
					
					String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
					osr.write(separator+"strand="+strandout+separator+"chr="+geneloc.getChr()+separator+"assembly="+assemblyout);
					osr.flush();
					
					for (int j = 0, n = fields.size(); j < n; j++) {
						osr.write(separator);
						String field = (String) fields.get(j);
						if (tranatts.containsKey(field)) {
							List values = (ArrayList) tranatts.get(field);
							
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
					
					osr.write(separator+(String) tranatts.get(Description));
					osr.write(separator);
					osr.flush();

			if ( ( (Boolean) tranatts.get(hasUTR) ).booleanValue() ) {
					  TreeMap locations = (TreeMap) tranatts.get(Locations);				    					
					  dna.CacheSequence(query.getSpecies(), geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());
					StringBuffer sequence = new StringBuffer(); // to collect all sequence before appending flanks
					        
				for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext(); ) {
							SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
							if (loc.getStrand() < 0)
								sequence.append( SequenceUtil.reverseComplement( dna.getSequence(query.getSpecies(), loc.getChr(), loc.getStart(), loc.getEnd()) ) );
							else
								sequence.append( dna.getSequence(query.getSpecies(), loc.getChr(), loc.getStart(), loc.getEnd()) );
				}
				    
					  if (query.getSequenceDescription().getRightFlank() > 0) {
						// extend flanking sequence
						SequenceLocation first_loc = (SequenceLocation) locations.get((Integer) locations.firstKey());
						SequenceLocation last_loc = (SequenceLocation) locations.get( (Integer) locations.lastKey());
				      
						SequenceLocation flank_loc;
						if (first_loc.getStrand() < 0) {
						  flank_loc = first_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank()); // right flank of first location
							  sequence.append( SequenceUtil.reverseComplement( dna.getSequence(query.getSpecies(), flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()) ) );
						}
						else {
						  flank_loc = last_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank()); // right flank of last location
							  sequence.append( dna.getSequence(query.getSpecies(), flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()) );
						} 
					  }				    
					  osr.write( sequence.toString() );				
					  osr.write("\n");
					  osr.flush();		
			}
			else {
			  osr.write(noUTRmessage);
			  osr.write("\n");
			  osr.flush();
			}
				}
		   } catch (SequenceException e) {
			   logger.warn(e.getMessage());
			   throw e;	
		   } catch (IOException e) {
			   logger.warn("Couldnt write to OutputStream\n"+e.getMessage());
			   throw new SequenceException(e);	
		   }		  	
	   }				  	
	};
	
	private final SeqWriter fastaWriter = new SeqWriter() {
		void writeSequences(Integer geneID) throws SequenceException {
			Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
			TreeMap traniDs = (TreeMap) geneatts.get(Transcripts);
			SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
			String assemblyout = (String) geneatts.get(Assembly);
						
			 try {
				for (Iterator tranIter = traniDs.keySet().iterator(); tranIter.hasNext();) {
				  Hashtable tranatts = (Hashtable) traniDs.get( (Integer) tranIter.next());
				
				  // write the header, starting with the displayID
				  String displayIDout = (String) tranatts.get(DisplayID);
				  osr.write(">"+displayIDout);
					
				  String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				  osr.write("\tstrand="+strandout+separator+"chr="+geneloc.getChr()+separator+"assembly="+assemblyout);
				  osr.flush();
					
				  for (int j = 0, n = fields.size(); j < n; j++) {
					  osr.write(separator);
					  String field = (String) fields.get(j);
					  if (tranatts.containsKey(field)) {
						  List values = (ArrayList) tranatts.get(field);
							
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
					
				  osr.write(separator+(String) tranatts.get(Description));
				  osr.write("\n");
				  osr.flush();

				  if ( ( (Boolean) tranatts.get(hasUTR) ).booleanValue() ) {
					  TreeMap locations = (TreeMap) tranatts.get(Locations);				    					
					  dna.CacheSequence(query.getSpecies(), geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());
 
					 StringBuffer sequence = new StringBuffer(); // to collect all sequence before appending flanks
					        
					 for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext(); ) {
						 SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
						 if (loc.getStrand() < 0)
							 sequence.append( SequenceUtil.reverseComplement( dna.getSequence(query.getSpecies(), loc.getChr(), loc.getStart(), loc.getEnd()) ) );
						 else
							 sequence.append( dna.getSequence(query.getSpecies(), loc.getChr(), loc.getStart(), loc.getEnd()) );
					 }
				    
					 if (query.getSequenceDescription().getLeftFlank() > 0) {
					   // extend flanking sequence
					   SequenceLocation first_loc = (SequenceLocation) locations.get((Integer) locations.firstKey());
					   SequenceLocation last_loc = (SequenceLocation) locations.get( (Integer) locations.lastKey());
				      
					   SequenceLocation flank_loc;
					   if (first_loc.getStrand() < 0) {
						   flank_loc = last_loc.getLeftFlankOnly(query.getSequenceDescription().getLeftFlank()); // left flank of last location
							 sequence.insert( 0, SequenceUtil.reverseComplement( dna.getSequence(query.getSpecies(), flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()) ) );
					   }
					   else {
						    flank_loc = first_loc.getLeftFlankOnly(query.getSequenceDescription().getLeftFlank()); // left flank of first location
							  sequence.insert( 0, dna.getSequence(query.getSpecies(), flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()) );
					   } 
					 }				    
					 osr.write( sequence.toString() );				
					 osr.write("\n");
					 osr.flush();
				  }
			  else {
				osr.write(noUTRmessage);
				osr.write("\n");
				osr.flush();
			  }
				}		
		  } catch (SequenceException e) {
			  logger.warn(e.getMessage());
			  throw e;	
		  } catch (IOException e) {
			  logger.warn("Couldnt write to OutputStream\n"+e.getMessage());
			  throw new SequenceException(e);	
		  }		  	
		}		
	};	

	private String separator;		
	private Logger logger = Logger.getLogger(DownStreamUTRSeqQueryRunner.class.getName());
	private Query query = null;
	private FormatSpec format = null;
	private OutputStreamWriter osr = null;
	private Connection conn = null;
	
	private TreeMap geneiDs = new TreeMap(); // holds each objects information, in order
	private List fields = new ArrayList(); // holds unique list of resultset description fields from the query
	private DNAAdaptor dna;
    
	// Used for colating required fields
	private String queryID;
	private String GeneID = "gene_id";
	private String coordStart, coordEnd;
	private List displayIDs = new ArrayList();
	private final String Rank = "rank";
	private final String Chr =  "chr_name";
	private final String AssemblyColumn = "assembly_type";
	private final String StrandColumn = "exon_chrom_strand";
    
	// Strings for use in idattribute Hashtable keys
	private final String Locations = "locations";
	private final String Transcripts = "transcripts";
	private final String Assembly = "assembly";
	private final String Strand = "strand";	
	private final String Geneloc = "geneloc";
	private final String DisplayID = "displayID";
	private final String Description = "description";
	private final String hasUTR = "hasUTR";	
	private final String noUTRmessage = "No UTR is annotated for this transcript"; // message to write when no UTR is available
}
