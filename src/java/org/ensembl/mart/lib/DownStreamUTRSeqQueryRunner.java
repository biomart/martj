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

import java.io.IOException;
import java.io.OutputStream;
import org.ensembl.util.FormattedSequencePrintStream;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.ensembl.util.SequenceUtil;

/**
 * Outputs peptide sequence in one of the supported output format
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported output formats
 */
public final class DownStreamUTRSeqQueryRunner implements QueryRunner {

	/**
	 * Constructs a PeptideSeqQueryRunner object to execute a Query
	 * and print Peptide Sequences
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param os an OutputStream object
	 */
	public DownStreamUTRSeqQueryRunner(Query query, FormatSpec format, OutputStream os) {
		this.query = query;
		this.format = format;
		this.osr = new FormattedSequencePrintStream(maxColumnLen, os, true); // autoflush true
		this.dna = new DNAAdaptor(conn);

		switch (format.getFormat()) {
			case FormatSpec.TABULATED :
				this.separator = format.getSeparator();
				this.seqWriter = tabulatedWriter;
				break;

			case FormatSpec.FASTA :
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

		StringTokenizer tokens = new StringTokenizer(dataset, "_", false);
		species = tokens.nextToken();
	}

	private void updateQuery() {
		query.addAttribute(new FieldAttribute(queryID, "_structure_dm"));
		query.addAttribute(new FieldAttribute(GeneID, "_structure_dm"));
		query.addAttribute(new FieldAttribute(Rank, "_structure_dm"));
		query.addAttribute(new FieldAttribute(AssemblyColumn, "_structure_dm"));
		query.addAttribute(new FieldAttribute(coordStart, "_structure_dm"));
		query.addAttribute(new FieldAttribute(coordEnd, "_structure_dm"));
		query.addAttribute(new FieldAttribute(Chr, "_structure_dm"));
		query.addAttribute(new FieldAttribute(StrandColumn, "_structure_dm"));

		for (int i = 0; i < displayIDs.size(); i++) {
			query.addAttribute(new FieldAttribute((String) displayIDs.get(i), "_structure_dm"));
		}
	}

	public void execute(int limit) throws SequenceException, InvalidQueryException {
		SequenceDescription seqd = query.getSequenceDescription();
		boolean moreRows = true;
		// will be switched to false on the last batch of SQL
		int batchStart = 0;

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
		coordStart = "3utr_start";
		coordEnd = "3utr_end";
		displayIDs.add("transcript_stable_id_v");
		displayIDs.add("gene_stable_id_v");
		updateQuery();

		Attribute[] attributes = query.getAttributes();

		String sql = null;
		try {
			CompiledSQLQuery csql = new CompiledSQLQuery(query);
			String sqlbase = csql.toSQL();
			String structure_table = dataset + "_structure_dm";
			sqlbase += " order by  "
				+ structure_table
				+ ".gene_id, "
				+ structure_table
				+ ".transcript_id, "
				+ structure_table
				+ ".rank";

			while (moreRows) {
				sql = sqlbase;

				if (limit > 0) {
					sql += " limit " + limit;
					moreRows = false;
					// for client requested limit, run the entire query
				} else {
					sql += " limit " + batchStart + " , " + batchLength;
					batchStart += batchLength;
				}

				logger.info("QUERY : " + query);
				logger.info("SQL : " + sql);

				PreparedStatement ps = conn.prepareStatement(sql);
				int p = 1;
				for (int i = 0, n = query.getFilters().length; i < n; ++i) {
					Filter f = query.getFilters()[i];
					String value = f.getValue();
					if (value != null) {
						logger.info("SQL (prepared statement value) : " + p + " = " + value);
						ps.setString(p++, value);
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

				int rows = 0; //row count
				Integer lastGene = new Integer(0);
				// will hold last gene, when a new one is encountered
				while (rs.next()) {
					rows++;
					Integer geneID = new Integer(rs.getInt(geneIDindex));
					Integer tranID = new Integer(rs.getInt(queryIDindex));
					Integer rank = new Integer(rs.getInt(rankIndex));

					if (!(geneiDs.containsKey(geneID))) {
						if (geneiDs.size() > 0) {
							//				   process the previous tranID, if this isnt the first time through, then refresh the traniDs TreeMap
							if (lastGene.intValue() > 0)
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

					if (!(traniDs.containsKey(tranID))) {
						Hashtable tranatts = new Hashtable();
						tranatts.put(Locations, new TreeMap());
						tranatts.put(hasUTR, Boolean.FALSE);
						// initialize to false, over ride if necessary
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
						if (!(geneatts.containsKey(Geneloc))) {
							geneatts.put(Geneloc, new SequenceLocation(chr, start, end, strand));
						} else {
							SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
							if (start < geneloc.getStart()) {
								geneatts.put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand));
								// overwrite the previous copy
								if (end > geneloc.getEnd())
									geneatts.put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand));
								// overwrite the previous copy
							}
						}
					}

					//	process displayID, if necessary
					if (!(tranatts.containsKey(DisplayID))) {
						StringBuffer displayID = new StringBuffer();

						for (int i = 0, n = displayIDindices.size(); i < n; i++) {
							if (i > 0)
								displayID.append(separator);
							int currindex = ((Integer) displayIDindices.get(i)).intValue();
							if (rs.getString(currindex) != null)
								displayID.append(rs.getString(currindex));
						}

						tranatts.put(DisplayID, displayID.toString());
					}

					// Rest can be duplicates, or novel values for a given field, collect lists of values for each field
					// currindex is now the last index of the DisplayIDs.  Increment it, and iterate over the rest of the ResultSet to print the description

					for (int i = 0, n = otherIndices.size(); i < n; i++) {
						int currindex = ((Integer) otherIndices.get(i)).intValue();
						if (rs.getString(currindex) != null) {
							String field = attributes[currindex - 1].getField();
							if (!fields.contains(field))
								fields.add(field);
							String value = rs.getString(currindex);

							if (tranatts.containsKey(field)) {
								if (!((ArrayList) tranatts.get(field)).contains(value))
									 ((ArrayList) tranatts.get(field)).add(value);
							} else {
								List values = new ArrayList();
								values.add(value);
								tranatts.put(field, values);
							}
						}
					}

					// add the description, if necessary
					if (!(tranatts.containsKey(Description)))
						tranatts.put(Description, seqd.getDescription());
				}
				// write the last transcripts data, if present
				if (lastGene.intValue() > 0)
					seqWriter.writeSequences(lastGene);

				if (rows < batchLength)
					moreRows = false;
				// on the odd chance that the last result set is equal in size to the batchLength, it will need to make an extra attempt.	      	     
			}
		} catch (IOException e) {
			throw new InvalidQueryException(e);
		} catch (SQLException e) {
			throw new SequenceException(e + " :" + sql);
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

			try {
				for (Iterator tranIter = traniDs.keySet().iterator(); tranIter.hasNext();) {
					Hashtable tranatts = (Hashtable) traniDs.get((Integer) tranIter.next());

					if (((Boolean) tranatts.get(hasUTR)).booleanValue()) {
						String assemblyout = (String) geneatts.get(Assembly);

						int gstrand = geneloc.getStrand();
						String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";

						osr.print((String) tranatts.get(DisplayID));

						osr.print(
							separator
								+ "strand="
								+ strandout
								+ separator
								+ "chr="
								+ geneloc.getChr()
								+ separator
								+ "assembly="
								+ assemblyout);
            if (osr.checkError())
              throw new IOException();

						for (int j = 0, n = fields.size(); j < n; j++) {
							osr.print(separator);
							String field = (String) fields.get(j);
							if (tranatts.containsKey(field)) {
								List values = (ArrayList) tranatts.get(field);

								if (values.size() > 1)
									osr.print(field + " in ");
								else
									osr.print(field + "=");

								for (int vi = 0; vi < values.size(); vi++) {
									if (vi > 0)
										osr.print(",");
									osr.print((String) values.get(vi));
								}
							} else
								osr.print(field + "= ");
              if (osr.checkError())
                throw new IOException();
						}

						osr.print(separator + (String) tranatts.get(Description));
						osr.print(separator);
            if (osr.checkError())
              throw new IOException();

						TreeMap locations = (TreeMap) tranatts.get(Locations);
						dna.CacheSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());

            ArrayList seqout = new ArrayList();
            int seqLen = 0;
            
						// to collect all sequence before appending flanks
						for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext();) {
							SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
              byte[] theseBytes = null;
              
							if (loc.getStrand() < 0)
								theseBytes = SequenceUtil.reverseComplement(dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()));
							else
								theseBytes = dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd());
              
              seqLen += theseBytes.length;
              seqout.add(theseBytes);
						}

						if (query.getSequenceDescription().getRightFlank() > 0) {
							// extend flanking sequence
							SequenceLocation first_loc = (SequenceLocation) locations.get((Integer) locations.firstKey());
							SequenceLocation last_loc = (SequenceLocation) locations.get((Integer) locations.lastKey());

							SequenceLocation flank_loc;
              byte[] theseBytes = null;
              
							if (first_loc.getStrand() < 0) {
								flank_loc = first_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank());
								// right flank of first location
							
              	theseBytes = SequenceUtil.reverseComplement(dna.getSequence(species, flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()));
							} else {
								flank_loc = last_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank());
								
                // right flank of last location
								theseBytes = dna.getSequence(species, flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd());
							}
              
              seqLen += theseBytes.length;
              seqout.add(theseBytes);
						}
            
            //iterate through sequence bytes to fill seqout byte[]
            byte[] sequence = new byte[seqLen];
            int nextPos = 0;
            for (int i = 0, n = seqout.size(); i < n; i++) {
              byte[] thisChunk = (byte[]) seqout.get(i);
              System.arraycopy(thisChunk, 0, sequence, nextPos, thisChunk.length);
              nextPos += thisChunk.length;
            }

            seqout = null;
						osr.write(sequence);

						osr.print("\n");
            if (osr.checkError())
              throw new IOException();
					} else {
						osr.print((String) tranatts.get(DisplayID));
						osr.print(separator + (String) tranatts.get(Description));
						osr.print(separator);
						osr.print(noUTRmessage);
						osr.print("\n");
            if (osr.checkError())
              throw new IOException();
					}
				}
			} catch (SequenceException e) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning(e.getMessage());
				throw e;
			} catch (IOException e) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning("Couldnt write to OutputStream\n" + e.getMessage());
				throw new SequenceException(e);
			}
		}
	};

	private final SeqWriter fastaWriter = new SeqWriter() {
		void writeSequences(Integer geneID) throws SequenceException {
			Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
			TreeMap traniDs = (TreeMap) geneatts.get(Transcripts);
			SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);

			try {
				for (Iterator tranIter = traniDs.keySet().iterator(); tranIter.hasNext();) {
					Hashtable tranatts = (Hashtable) traniDs.get((Integer) tranIter.next());

					if (((Boolean) tranatts.get(hasUTR)).booleanValue()) {
						String assemblyout = (String) geneatts.get(Assembly);
						String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";

						// write the header, starting with the displayID
						osr.print(">" + (String) tranatts.get(DisplayID));

						osr.print(
							"\tstrand=" + strandout + separator + "chr=" + geneloc.getChr() + separator + "assembly=" + assemblyout);
            if (osr.checkError())
              throw new IOException();

						for (int j = 0, n = fields.size(); j < n; j++) {
							osr.print(separator);
							String field = (String) fields.get(j);
							if (tranatts.containsKey(field)) {
								List values = (ArrayList) tranatts.get(field);

								if (values.size() > 1)
									osr.print(field + " in ");
								else
									osr.print(field + "=");

								for (int vi = 0; vi < values.size(); vi++) {
									if (vi > 0)
										osr.print(",");
									osr.print((String) values.get(vi));
								}
							} else
								osr.print(field + "= ");
              if (osr.checkError())
                throw new IOException();
						}

						osr.print(separator + (String) tranatts.get(Description));
						osr.print("\n");
            if (osr.checkError())
              throw new IOException();

						TreeMap locations = (TreeMap) tranatts.get(Locations);
						dna.CacheSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());

            ArrayList seqout = new ArrayList();
            int seqLen = 0;
            
            // to collect all sequence before appending flanks
            for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext();) {
              SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
              byte[] theseBytes = null;
              
              if (loc.getStrand() < 0)
                theseBytes = SequenceUtil.reverseComplement(dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()));
              else
                theseBytes = dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd());
              
              seqLen += theseBytes.length;
              seqout.add(theseBytes);
            }

            if (query.getSequenceDescription().getRightFlank() > 0) {
              // extend flanking sequence
              SequenceLocation first_loc = (SequenceLocation) locations.get((Integer) locations.firstKey());
              SequenceLocation last_loc = (SequenceLocation) locations.get((Integer) locations.lastKey());

              SequenceLocation flank_loc;
              byte[] theseBytes = null;
              
              if (first_loc.getStrand() < 0) {
                flank_loc = first_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank());
                // right flank of first location
              
                theseBytes = SequenceUtil.reverseComplement(dna.getSequence(species, flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()));
              } else {
                flank_loc = last_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank());
                
                // right flank of last location
                theseBytes = dna.getSequence(species, flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd());
              }
              
              seqLen += theseBytes.length;
              seqout.add(theseBytes);
            }
            
            //iterate through sequence bytes to fill seqout byte[]
            byte[] sequence = new byte[seqLen];
            int nextPos = 0;
            for (int i = 0, n = seqout.size(); i < n; i++) {
              byte[] thisChunk = (byte[]) seqout.get(i);
              System.arraycopy(thisChunk, 0, sequence, nextPos, thisChunk.length);
              nextPos += thisChunk.length;
            }

            seqout = null;
            osr.writeSequence(sequence);
						osr.print("\n");
            if (osr.checkError())
              throw new IOException();
					} else {
						osr.print(">" + (String) tranatts.get(DisplayID));
						osr.print("\t" + (String) tranatts.get(Description));
						osr.print("\n");
						osr.print(noUTRmessage);
						osr.print("\n");
            if (osr.checkError())
              throw new IOException();
					}
				}
			} catch (SequenceException e) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning(e.getMessage());
				throw e;
			} catch (IOException e) {
				if (logger.isLoggable(Level.WARNING))
				  logger.warning("Couldnt write to OutputStream\n" + e.getMessage());
				throw new SequenceException(e);
			}
		}
	};

  private final int maxColumnLen = 80;
	private int batchLength = 200000;
	// number of records to process in each batch
	private String separator;
	private Logger logger = Logger.getLogger(DownStreamUTRSeqQueryRunner.class.getName());
	private Query query = null;
	private String dataset = null;
	private String species = null;
	private FormatSpec format = null;
	private FormattedSequencePrintStream osr = null;
	private Connection conn = null;

	private TreeMap geneiDs = new TreeMap();
	// holds each objects information, in order
	private List fields = new ArrayList();
	// holds unique list of resultset description fields from the query
	private DNAAdaptor dna;

	// Used for colating required fields
	private String queryID;
	private String GeneID = "gene_id";
	private String coordStart, coordEnd;
	private List displayIDs = new ArrayList();
	private final String Rank = "rank";
	private final String Chr = "chr_name";
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
	private final String noUTRmessage = "No UTR is annotated for this transcript";
	// message to write when no UTR is available
}
