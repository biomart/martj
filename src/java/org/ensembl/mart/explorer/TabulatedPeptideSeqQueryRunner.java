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
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.ensembl.util.SequenceUtil;

/**
 * Outputs peptide sequence in tabulated output format, with the sequence as the last field in the output.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class TabulatedPeptideSeqQueryRunner implements QueryRunner {

	/**
	 * Constructs a TabulatedPeptideSeqQueryRunner object to execute a Query
	 * and print tabulated output of Peptide Sequences
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param conn a java.sql.Connection object
	 * @param os an OutputStream object
	 */	
	public TabulatedPeptideSeqQueryRunner(Query query, FormatSpec format, Connection conn, OutputStream os) {
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
	    
	/**
	 * Impliments the execute method of the interface.  For tabulated 
	 * sequence queries, the SQL is executed, and the ResultSet is written 
	 * to the OutputStream via a OutputStreamWriter.  Then the Sequence
	 * is printed as the last field in the tabulated output. Each field 
	 * of a ResultSet is separated by the separator defined in the FormatSpec 
	 * object in the output.
	 */
	public void execute(int limit) throws SQLException, IOException, InvalidQueryException {
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
		ArrayList displayIDindices = new ArrayList();
		ArrayList otherIndices = new ArrayList();
        
		queryID = "transcript_id";
		coordStart = "coding_start";
		coordEnd = "coding_end";
		displayIDs.add("transcript_stable_id_v");
		displayIDs.add("gene_stable_id_v");
		updateQuery();
				
		Attribute[] attributes = query.getAttributes();
		String separator = format.getSeparator();

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
            
			int nColumns = rmeta.getColumnCount();
				
			// process columnNames for required attribute indices
			for (int i = 1; i <= nColumns; ++i) {
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
				int queryID = rs.getInt(queryIDindex);
				int rank = rs.getInt(rankIndex);
				
				Integer idI = new Integer(queryID);
				
				if (! idS.containsKey(idI)) { 
					Hashtable atts = new Hashtable();
					atts.put(Locations, new TreeMap());
					atts.put(Assembly, rs.getString(assemblyIndex));
					idS.put(idI, atts);
				}
				
				Hashtable atts = (Hashtable) idS.get(idI);
				
				int start = rs.getInt(startIndex);
				if (start > 0) {
					// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
					String chr = rs.getString(chromIndex);
					int end = rs.getInt(endIndex);
					int strand = rs.getInt(strandIndex);
						
					//	order the locations by their rank in ascending order
					((TreeMap) ((Hashtable) idS.get(idI)).get(Locations)).put(new Integer(rank), new SequenceLocation(chr, start, end, strand));
						
					// keep track of the lowest start and highest end for the gene	
					if (! ((Hashtable) idS.get(idI)).containsKey(Geneloc)) {
						((Hashtable) idS.get(idI)).put(Geneloc, new SequenceLocation(chr, start, end, strand));
					}
					else {
						SequenceLocation geneloc = (SequenceLocation) ((Hashtable) idS.get(idI)).get(Geneloc);
						if (start < geneloc.getStart()) {
							((Hashtable) idS.get(idI)).put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand)); // overwrite the previous copy
						if (end > geneloc.getEnd())
							((Hashtable) idS.get(idI)).put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand)); // overwrite the previous copy
						}
					}    
				}

				//	process displayID, if necessary
				if (!((Hashtable) idS.get(idI)).containsKey(DisplayID)) {								
					StringBuffer displayID = new StringBuffer();
                
					for (int i = 0, n = displayIDindices.size(); i < n; i++) {
						if (i>0) displayID.append( separator );
						int currindex = ((Integer) displayIDindices.get(i)).intValue();
						if ( rs.getString(currindex) != null )
							displayID.append( rs.getString(currindex) );
					}
				
					((Hashtable) idS.get(idI)).put(DisplayID, displayID.toString());
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
						
						if (((Hashtable) idS.get(idI)).containsKey(field)) {
							if (! ((ArrayList) atts.get(field)).contains(value))
								((ArrayList) atts.get(field)).add(value);
						}
						else {
							ArrayList values = new ArrayList();
							values.add(value); 
							((Hashtable) idS.get(idI)).put(field, values);
						}
					}
				}
				
				// add the description, if necessary
				if (! ((Hashtable) idS.get(idI)).containsKey(Description))
					((Hashtable) idS.get(idI)).put( Description, separator+seqd.getDescription() );
			}
			
			// run through the idS list, make and print the header, then get and print the sequences from the locations
			Iterator iDiter = idS.keySet().iterator();
			
			while (iDiter.hasNext()) {
				Hashtable atts = (Hashtable) idS.get((Integer) iDiter.next());
				
				// write the header, starting with the displayID
				String displayIDout = (String) atts.get(DisplayID);
				osr.write(displayIDout);
					
				SequenceLocation geneloc = (SequenceLocation) atts.get(Geneloc);
				String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				String assemblyout = (String) atts.get(Assembly);
				osr.write(separator+"strand="+strandout+separator+"chr="+geneloc.getChr()+separator+"assembly="+assemblyout);
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
					
				osr.write(separator+(String) atts.get(Description)+separator);
				osr.flush();

				TreeMap locations = (TreeMap) atts.get(Locations);
				Iterator lociter = locations.keySet().iterator();
								    					
				dna.CacheSequence(query.getSpecies(), geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());
        
				StringBuffer sequence = new StringBuffer(); // to collect all sequence before translation
					        
				while (lociter.hasNext()) {
				    SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
					if (loc.getStrand() < 0)
					    sequence.append( SequenceUtil.reverseComplement( dna.getSequence(query.getSpecies(), loc.getChr(), loc.getStart(), loc.getEnd()) ) );
					else
					    sequence.append( dna.getSequence(query.getSpecies(), loc.getChr(), loc.getStart(), loc.getEnd()) );
				}
				osr.write( SequenceUtil.dna2protein( sequence.toString() )+"\n" );
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
		
	private Logger logger = Logger.getLogger(TabulatedSeqQueryRunner.class.getName());
	private Query query = null;
	private FormatSpec format = null;
	private OutputStream os = null;
	private Connection conn = null;
	
	private TreeMap idS = new TreeMap(); // holds each objects information, in order
	private ArrayList fields = new ArrayList(); // holds unique list of resultset description fields from the query
	private DNAAdaptor dna;
    
	// Used for colating required fields
	private String queryID;
	private String coordStart, coordEnd;
	private ArrayList displayIDs = new ArrayList();
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
