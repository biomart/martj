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
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ensembl.util.SequenceUtil;

/**
 * Writes out Gene Sequences Exons and Introns) in one of the supported formats.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported formats
 */
public final class GeneEISeqQueryRunner implements QueryRunner {

	/**
	 * Constructs a GeneEISeqQueryRunner object to execute a Query
	 * and print Gene Sequences (Exons and Introns).
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param conn a java.sql.Connection object
	 * @param os an OutputStream object
	 */
	  public GeneEISeqQueryRunner(Query query, FormatSpec format, Connection conn, OutputStream os) {
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
		query.addAttribute(new FieldAttribute(queryID, "_structure_dm"));
		query.addAttribute(new FieldAttribute(AssemblyColumn, "_structure_dm"));
		query.addAttribute(new FieldAttribute(coordStart, "_structure_dm"));
		query.addAttribute(new FieldAttribute(coordEnd, "_structure_dm"));
		query.addAttribute(new FieldAttribute(Chr, "_structure_dm"));
		query.addAttribute(new FieldAttribute(StrandColumn, "_structure_dm"));
		
		for (int i=0; i< displayIDs.size(); i++) {
			query.addAttribute( new FieldAttribute( (String) displayIDs.get(i) , "_structure_dm") );
		}
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.QueryRunner#execute(int)
	 */
	public void execute(int limit)
		throws SequenceException, InvalidQueryException {
			SequenceDescription seqd = query.getSequenceDescription();
      boolean moreRows = true; // will go false on last batch of SQL
      int batchStart = 0;
      
			// need to know these indexes specifically
			int queryIDindex = 0;
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
			displayIDs.add("gene_stable_id_v");
			updateQuery();
				
			Attribute[] attributes = query.getAttributes();

    String sql = null;
		try {
			  CompiledSQLQuery csql = new CompiledSQLQuery( conn, query );
			  String sqlbase = csql.toSQL();
			  String structure_table = dataset+"_structure_dm";
			  sqlbase += " order by  "+structure_table+".gene_id, "+structure_table+".transcript_id, "+structure_table+".rank";


			while (moreRows) {
				  sql = sqlbase;
				  			  
			    if (limit > 0) {
				    sql += " limit "+limit;
				    moreRows = false; // run entire query for client supplied limit
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
				  for (int i = 1, nColumns = rmeta.getColumnCount(); i <= nColumns; ++i) {
					  String column = rmeta.getColumnName(i);
					  if (column.equals(queryID))
						  queryIDindex = i;
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
			
			    int rows = 0; //rowcount
				  Integer lastGene = new Integer(0); // will nold last geneID when new one is encountered
			 	  while ( rs.next() ) {
			 	  	rows++;
					  Integer geneID = new Integer(rs.getInt(queryIDindex));
				
					  if (! geneiDs.containsKey(geneID)) {
					    if (geneiDs.size() > 0) {
						    // write the previous genes data, and refresh the geneiDs TreeMap
						    if (lastGene.intValue() > 0)
							    seqWriter.writeSequences(lastGene);
							  geneiDs = new TreeMap();
					    }
							lastGene = geneID;					 
						  Hashtable atts = new Hashtable();
						  atts.put(Assembly, rs.getString(assemblyIndex));
						  geneiDs.put(geneID, atts);
					  }
				
					  Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
	
					  int start = rs.getInt(startIndex);
					  if (start > 0) {
						  // if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
						  String chr = rs.getString(chromIndex);
						  int end = rs.getInt(endIndex);
						  int strand = rs.getInt(strandIndex);
						                    						
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
					  }

					  //	process displayID, if necessary
					  if (! ( geneatts.containsKey(DisplayID) )) {								
						  StringBuffer displayID = new StringBuffer();
                
						  for (int i = 0, n = displayIDindices.size(); i < n; i++) {
							  if (i>0) displayID.append( separator );
							  int currindex = ((Integer) displayIDindices.get(i)).intValue();
							  if ( rs.getString(currindex) != null )
								  displayID.append( rs.getString(currindex) );
						  }
				
						  geneatts.put(DisplayID, displayID.toString());
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
						
							  if (geneatts.containsKey(field)) {
								  if (! ( (ArrayList) geneatts.get(field) ).contains(value))
									  ( (ArrayList) geneatts.get(field) ).add(value);
							  }
							  else {
								  List values = new ArrayList();
								  values.add(value); 
								  geneatts.put(field, values);
							  }
						  }
					  }
				
					  // add the description, if necessary
					  if (! geneatts.containsKey(Description))
						  geneatts.put( Description, seqd.getDescription() );
			 	  }
			    // write the last genes data, if present
			    if (lastGene.intValue() > 0)
			      seqWriter.writeSequences(lastGene);

			    if (rows < batchLength)
				    moreRows = false; // on the odd chance that the last result set is equal in size to the batchLength, it will need to make an extra attempt.	      	      
        }  
			} catch (SQLException e) {
				throw new InvalidQueryException(e + " :" + sql);
			} catch (IOException e) {
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
		  	try {
				  Hashtable geneatts = (Hashtable) geneiDs.get( geneID );
				  SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
				  
				  osr.write((String) geneatts.get(DisplayID));
				  String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				  osr.write(separator+"strand="+strandout+separator+"chr="+geneloc.getChr()+separator+"assembly="+(String) geneatts.get(Assembly));
				  osr.flush();
									  
				  for (int j = 0, n = fields.size(); j < n; j++) {
					  osr.write(separator);
					  String field = (String) fields.get(j);
					  if (geneatts.containsKey(field)) {
						  List values = (ArrayList) geneatts.get(field);
							
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
				  osr.write(separator+(String) geneatts.get(Description));
				  osr.write(separator);
				  osr.flush();
				  
				  //modify flanks, if necessary, and write sequence
				  if (query.getSequenceDescription().getLeftFlank() > 0)
				    geneloc = geneloc.extendLeftFlank(query.getSequenceDescription().getLeftFlank());
				  if (query.getSequenceDescription().getRightFlank() > 0)
				    geneloc = geneloc.extendRightFlank(query.getSequenceDescription().getRightFlank());
				  
				  if (geneloc.getStrand() < 0)
				  	osr.write( SequenceUtil.reverseComplement( dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd()) ) );
				  else
					  osr.write( dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd()) );
				  osr.flush();
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
		  void writeSequences(Integer geneID) throws SequenceException {
			  try {
				  Hashtable geneatts = (Hashtable) geneiDs.get( geneID );
				  SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
				  
				  osr.write(">"+(String) geneatts.get(DisplayID));
				  String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				  osr.write("\tstrand="+strandout+separator+"chr="+geneloc.getChr()+separator+"assembly="+(String) geneatts.get(Assembly));
					osr.flush();
									  
				  for (int j = 0, n = fields.size(); j < n; j++) {
					  osr.write(separator);
					  String field = (String) fields.get(j);
					  if (geneatts.containsKey(field)) {
						  List values = (ArrayList) geneatts.get(field);
							
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
				  osr.write(separator+(String) geneatts.get(Description));
				  osr.write("\n");
				  osr.flush();
				  
				  //modify flanks, if necessary, and write sequence
				  if (query.getSequenceDescription().getLeftFlank() > 0)
				    geneloc = geneloc.extendLeftFlank(query.getSequenceDescription().getLeftFlank());
				  if (query.getSequenceDescription().getRightFlank() > 0)
				    geneloc = geneloc.extendRightFlank(query.getSequenceDescription().getRightFlank());
				  
				  if (geneloc.getStrand() < 0)
					  osr.write( SequenceUtil.reverseComplement( dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd()) ) );
				  else
					  osr.write( dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd()) );
				  osr.flush();
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
	private Logger logger = Logger.getLogger(GeneEISeqQueryRunner.class.getName());
	private Query query = null;
	private String dataset = null;
	private String species = null;
	private FormatSpec format = null;
	private OutputStreamWriter osr = null;
	private Connection conn = null;
	
	private TreeMap geneiDs = new TreeMap(); // holds each objects information, in order
	private List fields = new ArrayList(); // holds unique list of resultset description fields from the query
	private DNAAdaptor dna;
	
	private String queryID;
	private String AssemblyColumn = "assembly_type";
	private String coordStart = "exon_chrom_start";
	private String coordEnd  = "exon_chrom_end";
	private String Chr = "chr_name";
	private String StrandColumn = "exon_chrom_strand";
	private List displayIDs = new ArrayList();

  // strings for keys of Hashtable
  private String Geneloc = "gene_location";
  private String Assembly = "assembly";
  private String DisplayID = "display_id";
  private String Description = "description";
}
