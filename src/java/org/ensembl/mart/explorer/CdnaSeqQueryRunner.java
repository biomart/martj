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
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ensembl.util.SequenceUtil;

/**
 * Outputs Cdna sequence in one of the supported output formats
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported output formats
 */
public final class CdnaSeqQueryRunner implements QueryRunner {
	
	/**
	 * Constructs a CdnaSeqQueryRunner object to execute a Query
	 * and print output of cdna Sequences
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param conn a java.sql.Connection object
	 * @param os an OutputStream object
	 */	
	public CdnaSeqQueryRunner(Query query, FormatSpec format, Connection conn, OutputStream os) {
	    this.query = query;
	    this.format = format;
	    this.conn = conn;
	    this.osr  = new OutputStreamWriter(os);
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
	    
	    //resolve dataset, species, and focus
		  String[] mainTables = query.getStarBases();

		  for (int i = 0; i < mainTables.length; i++) {
			  if (Pattern.matches(".*gene", mainTables[i]))
				  dataset = mainTables[i];
		  }

		  StringTokenizer tokens = new StringTokenizer( dataset, "_", false);
		  species = tokens.nextToken();
    }
 
	private void updateQuery() {
		query.addAttribute(new FieldAttribute(queryID));
		query.addAttribute(new FieldAttribute(Rank));
		query.addAttribute(new FieldAttribute(AssemblyColumn));
		query.addAttribute(new FieldAttribute(coordStart));
		query.addAttribute(new FieldAttribute(coordEnd));
		query.addAttribute(new FieldAttribute(Chr));
		query.addAttribute(new FieldAttribute(StrandColumn));
		
		for (int i=0, n = displayIDs.size(); i< n; i++) {
			query.addAttribute( new FieldAttribute( (String) displayIDs.get(i) ) );
		}
	}
    
	public void execute(int limit) throws SequenceException, InvalidQueryException {
		SequenceDescription seqd = query.getSequenceDescription();
		boolean moreRows = true;
		int batchStart = 0;

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
				
		try {
		  CompiledSQLQuery csql = new CompiledSQLQuery( conn, query );
		  String sqlbase = csql.toSQL();		
		  
		  String structure_table = dataset+"_structure_dm";
		  sqlbase += " order by  "+structure_table+".transcript_id, "+structure_table+".rank";

		  while (moreRows) {
		  	String sql = sqlbase;
		  			
		    if (limit > 0) {
			    sql += " limit "+limit;
			    moreRows = false; // if client supplied limit, run the entire query
		    }
		    else {
		    	sql += " limit "+batchStart+" , "+batchLength;
		    	batchStart += batchLength;
		    }

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
			  for (int i = 1,  nColumns = rmeta.getColumnCount(); i <= nColumns; ++i) {
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

        int rows = 0; // count rows
        Integer lastTran = new Integer(0); // will hold previous transcript id, when a new one is encountered
			  while ( rs.next() ) {
			  	rows++;
				  Integer tranID = new Integer(rs.getInt(queryIDindex));
				  Integer rank = new Integer(rs.getInt(rankIndex));

				  if (! traniDs.containsKey(tranID)) {
					  if (traniDs.size() > 0) { 
//						process the previous tranID, if this isnt the first time through, then refresh the traniDs TreeMap
              seqWriter.writeSequences(lastTran);
              traniDs = new TreeMap();
					  }
					  lastTran = tranID;
					  Hashtable atts = new Hashtable();
					  atts.put(Locations, new TreeMap());
					  atts.put(Assembly, rs.getString(assemblyIndex));
					  traniDs.put(tranID, atts);
				  }
				
				  Hashtable tranatts = (Hashtable) traniDs.get(tranID);
				
				  int start = rs.getInt(startIndex);
				  if (start > 0) {
					  // if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
					  String chr = rs.getString(chromIndex);
					  int end = rs.getInt(endIndex);
					  int strand = rs.getInt(strandIndex);
						
					  //	order the locations by their rank in ascending order
					  ((TreeMap) tranatts.get(Locations)).put(rank, new SequenceLocation(chr, start, end, strand));
					
					  // keep track of the lowest start and highest end for the gene	
					  if (! tranatts.containsKey(Geneloc)) {
						  tranatts.put(Geneloc, new SequenceLocation(chr, start, end, strand));
					  }
					  else {
						  SequenceLocation geneloc = (SequenceLocation) ((Hashtable) traniDs.get(tranID)).get(Geneloc);
						  if (start < geneloc.getStart()) {
							  tranatts.put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand)); // overwrite the previous copy
					      if (end > geneloc.getEnd())
						      tranatts.put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand)); // overwrite the previous copy
						  }
					  }
				  }

				  //	process displayID, if necessary
				  if (! tranatts.containsKey(DisplayID)) {								
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
				  if (! tranatts.containsKey(Description))
					  tranatts.put( Description, seqd.getDescription() );
			  }
			  // write the last transcripts data, if present
			  if (lastTran.intValue() > 0)
	        seqWriter.writeSequences(lastTran);
	      
		    if (rows < batchLength)
		      moreRows = false; // on the odd chance that the last result set is equal in size to the batchLength, it will need to make an extra attempt.	      	      
       }
		} catch (IOException e) {
		  logger.warn("Couldnt write to OutputStream\n"+e.getMessage());
		  throw new SequenceException(e);
    } catch (SQLException e) {
			logger.warn(e.getMessage());
			throw new InvalidQueryException(e);
		}
	}
	
	// SeqWriter object
	SeqWriter seqWriter; 
	abstract class SeqWriter {
		abstract void writeSequences(Integer tranID) throws SequenceException;
	}
	
	private final SeqWriter tabulatedWriter = new SeqWriter() {
	  void writeSequences(Integer tranID) throws SequenceException {
		  try {
				Hashtable tranatts = (Hashtable) traniDs.get(tranID);
					
				// write the header, starting with the displayID
				String displayIDout = (String) tranatts.get(DisplayID);
				osr.write(displayIDout);
						
				SequenceLocation geneloc = (SequenceLocation) tranatts.get(Geneloc);
				String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				String assemblyout = (String) tranatts.get(Assembly);
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
			
				TreeMap locations = (TreeMap) tranatts.get(Locations);					    					
				dna.CacheSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());
			
				for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext(); ) {
					 SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
					 if (loc.getStrand() < 0)
			  	   osr.write( SequenceUtil.reverseComplement( dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()) ) );
					 else
						 osr.write( dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()) );
					 osr.flush();
				}
				osr.write("\n");
				osr.flush();
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
						  void writeSequences(Integer tranID) throws SequenceException {
									try {
											Hashtable tranatts = (Hashtable) traniDs.get(tranID);
					
											// write the header, starting with the displayID
											String displayIDout = (String) tranatts.get(DisplayID);
											osr.write(">"+displayIDout);
						
											SequenceLocation geneloc = (SequenceLocation) tranatts.get(Geneloc);
											String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
											String assemblyout = (String) tranatts.get(Assembly);
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
			
											TreeMap locations = (TreeMap) tranatts.get(Locations);
											dna.CacheSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());
			
											for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext(); ) {
												SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
												if (loc.getStrand() < 0)
													osr.write( SequenceUtil.reverseComplement( dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()) ) );
												else
													 osr.write( dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()) );
												osr.flush();
											}
											osr.write("\n");
											osr.flush();
									} catch (SequenceException e) {
										 logger.warn(e.getMessage());
										 throw e;
									} catch (IOException e) {
										 logger.warn("Couldnt write to OutputStream\n"+e.getMessage());
										 throw new SequenceException(e);
									}	    		 	
						  }
					};
					
	private int batchLength = 200000; // number of records to process in each batch
  private String separator;	
	private Logger logger = Logger.getLogger(CdnaSeqQueryRunner.class.getName());
	private Query query = null;
	private String dataset = null;
	private String species = null;
	private FormatSpec format = null;
	private OutputStreamWriter osr;
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
	private final String Locations = "locations";
	private final String Assembly = "assembly";
	private final String Strand = "strand";	
	private final String Geneloc = "geneloc";
	private final String DisplayID = "displayID";
	private final String Description = "description";
}