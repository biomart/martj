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

import org.ensembl.util.FormattedSequencePrintStream;
import org.ensembl.util.SequenceUtil;

/**
 * Writes out upstream or downstream sequences of transcripts in one of the supported formats.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported formats
 */
public final class TranscriptFlankSeqQueryRunner implements QueryRunner {

	/**
	  * Constructs a TranscriptFlankSeqQueryRunner object to print 
	  *  transcript upstream or downstream flanking sequences
	  * 
	  * @param query
	  * @param format
	  * @param os
	  */
	public TranscriptFlankSeqQueryRunner(Query query, FormatSpec format, OutputStream os) {
		this.query = query;
		this.format = format;
		this.osr = new FormattedSequencePrintStream(maxColumnLen, os, true); //autoflush true

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
		query.addAttribute(new FieldAttribute(TranscriptID, "_structure_dm"));
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

	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.QueryRunner#execute(int)
	 */
	public void execute(int limit) throws SequenceException, InvalidQueryException {
		SequenceDescription seqd = query.getSequenceDescription();
		boolean moreRows = true;
		int batchStart = 0;

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
		displayIDs.add("transcript_stable_id_v");
		displayIDs.add("gene_stable_id_v");
		updateQuery();

		Attribute[] attributes = query.getAttributes();

		String sql = null;
		try {
			Connection conn = query.getDataSource().getConnection();

			CompiledSQLQuery csql = new CompiledSQLQuery(query);
			String sqlbase = csql.toSQL();
			String structure_table = dataset + "_structure_dm";
			sqlbase += " order by  " + structure_table + ".gene_id, " + structure_table + ".transcript_id, " + structure_table + ".rank";

			while (moreRows) {
				sql = sqlbase;

				if (limit > 0) {
					sql += " limit " + limit;
					moreRows = false;
				} else {
					sql += " limit " + batchStart + " , " + batchLength;
					batchStart += batchLength;
				}

				logger.info("QUERY : " + query);
				logger.info("SQL : " + sql);

				PreparedStatement ps = conn.prepareStatement(sql);
				int p = 1;
				for (int i = 0; i < query.getFilters().length; ++i) {
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

				int rows = 0;
				Integer lastGene = new Integer(0);
				// will hold the previous geneID
				while (rs.next()) {
					rows++;
					Integer geneID = new Integer(rs.getInt(queryIDindex));
					Integer tranID = new Integer(rs.getInt(tranIDindex));
					Integer rank = new Integer(rs.getInt(rankIndex));

					// want everything ordered by gene_id, transcript_id
					if (!geneiDs.containsKey(geneID)) {
						if (geneiDs.size() > 0) {
							// write the previous genes data, and refresh the geneiDs TreeMap
							if (lastGene.intValue() > 0)
								seqWriter.writeSequences(lastGene, conn);
							geneiDs = new TreeMap();
						}
						lastGene = geneID;
						Hashtable atts = new Hashtable();
						atts.put(Assembly, rs.getString(assemblyIndex));
						atts.put(Transcripts, new TreeMap());
						geneiDs.put(geneID, atts);
					}
					Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
					TreeMap traniDs = (TreeMap) geneatts.get(Transcripts);

					if (!traniDs.containsKey(tranID))
						traniDs.put(tranID, new Hashtable());

					Hashtable tranatts = (Hashtable) traniDs.get(tranID);

					int start = rs.getInt(startIndex);
					if (start > 0) {
						// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
						String chr = rs.getString(chromIndex);
						int end = rs.getInt(endIndex);
						int strand = rs.getInt(strandIndex);

						// keep track of the lowest start and highest end for the gene, for caching
						if (!(geneatts.containsKey(Geneloc)))
							geneatts.put(Geneloc, new SequenceLocation(chr, start, end, strand));
						else {
							SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
							if (start < geneloc.getStart())
								geneatts.put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand));
							if (end > geneloc.getEnd())
								geneatts.put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand));
						}

						// keep track of the lowest start and highest end for the transcript
						if (!(tranatts.containsKey(Location)))
							tranatts.put(Location, new SequenceLocation(chr, start, end, strand));
						else {
							SequenceLocation tranloc = (SequenceLocation) tranatts.get(Location);
							if (start < tranloc.getStart())
								tranatts.put(Location, new SequenceLocation(chr, start, tranloc.getEnd(), strand));
							if (end > tranloc.getEnd())
								tranatts.put(Location, new SequenceLocation(chr, tranloc.getStart(), end, strand));
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
								ArrayList values = new ArrayList();
								values.add(value);
								tranatts.put(field, values);
							}
						}
					}

					// add the description, if necessary
					if (!tranatts.containsKey(Description))
						tranatts.put(Description, seqd.getDescription());
				}
				// write the last genes data, if present
				if (lastGene.intValue() > 0)
					seqWriter.writeSequences(lastGene, conn);

				if (rows < batchLength)
					moreRows = false;

				rs.close();
			}
			conn.close();
		} catch (IOException e) {
			throw new SequenceException(e);
		} catch (SQLException e) {
			throw new InvalidQueryException(e + ":" + sql);
		}
	}

	// SeqWriter object
	SeqWriter seqWriter;
	abstract class SeqWriter {
		abstract void writeSequences(Integer geneID, Connection conn) throws SequenceException;
	}

	private final SeqWriter tabulatedWriter = new SeqWriter() {
		void writeSequences(Integer geneID, Connection conn) throws SequenceException {
			try {
				DNAAdaptor dna = new DNAAdaptor(conn);

				Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
				SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
				String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				String assemblyout = (String) geneatts.get(Assembly);

				TreeMap traniDs = (TreeMap) geneatts.get(Transcripts);
				for (Iterator tranIDiter = traniDs.keySet().iterator(); tranIDiter.hasNext();) {
					Hashtable tranatts = (Hashtable) traniDs.get((Integer) tranIDiter.next());
					osr.print((String) tranatts.get(DisplayID));

					SequenceLocation tranloc = (SequenceLocation) tranatts.get(Location);
					SequenceDescription seqd = query.getSequenceDescription();

					osr.print(separator + "strand=" + strandout + separator + "chr=" + tranloc.getChr() + separator + "assembly=" + assemblyout);

					if (osr.checkError())
						throw new IOException();

					for (int j = 0, n = fields.size(); j < n; j++) {
						osr.print(separator);
						String field = (String) fields.get(j);
						if (tranatts.containsKey(field)) {
							ArrayList values = (ArrayList) tranatts.get(field);

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
					}

					osr.print(separator + (String) tranatts.get(Description));
					osr.print(separator);

					if (osr.checkError())
						throw new IOException();

					// modify transcript location coordinates depending on flank requested
					if (seqd.getLeftFlank() > 0)
						tranloc = tranloc.getLeftFlankOnly(seqd.getLeftFlank());
					else
						tranloc = tranloc.getRightFlankOnly(seqd.getRightFlank());

					if (tranloc.getStrand() < 0)
						osr.write(SequenceUtil.reverseComplement(dna.getSequence(species, tranloc.getChr(), tranloc.getStart(), tranloc.getEnd())));
					else
						osr.write(dna.getSequence(species, tranloc.getChr(), tranloc.getStart(), tranloc.getEnd()));

					osr.print("\n");

					if (osr.checkError())
						throw new IOException();
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
		void writeSequences(Integer geneID, Connection conn) throws SequenceException {
			try {
				DNAAdaptor dna = new DNAAdaptor(conn);

				Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
				SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
				String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				String assemblyout = (String) geneatts.get(Assembly);

				TreeMap traniDs = (TreeMap) geneatts.get(Transcripts);
				for (Iterator tranIDiter = traniDs.keySet().iterator(); tranIDiter.hasNext();) {
					Hashtable tranatts = (Hashtable) traniDs.get((Integer) tranIDiter.next());
					osr.print(">" + (String) tranatts.get(DisplayID));

					SequenceLocation tranloc = (SequenceLocation) tranatts.get(Location);
					SequenceDescription seqd = query.getSequenceDescription();

					osr.print("\tstrand=" + strandout + separator + "chr=" + tranloc.getChr() + separator + "assembly=" + assemblyout);

					if (osr.checkError())
						throw new IOException();

					for (int j = 0, n = fields.size(); j < n; j++) {
						osr.print(separator);
						String field = (String) fields.get(j);
						if (tranatts.containsKey(field)) {
							ArrayList values = (ArrayList) tranatts.get(field);

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
					}

					osr.print(separator + (String) tranatts.get(Description));
					osr.print("\n");

					if (osr.checkError())
						throw new IOException();

					// modify transcript location coordinates depending on flank requested
					if (seqd.getLeftFlank() > 0)
						tranloc = tranloc.getLeftFlankOnly(seqd.getLeftFlank());
					else
						tranloc = tranloc.getRightFlankOnly(seqd.getRightFlank());

					if (tranloc.getStrand() < 0)
						osr.writeSequence(SequenceUtil.reverseComplement(dna.getSequence(species, tranloc.getChr(), tranloc.getStart(), tranloc.getEnd())));
					else
						osr.writeSequence(dna.getSequence(species, tranloc.getChr(), tranloc.getStart(), tranloc.getEnd()));

					osr.print("\n");
					osr.resetColumnCount();

					if (osr.checkError())
						throw new IOException();

				}
			} catch (SequenceException e) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning(e.getMessage());
				throw e;
			} catch (IOException e) {
				logger.warning("Couldnt write to OutputStream\n" + e.getMessage());
				throw new SequenceException(e);
			}
		}
	};

	private final int maxColumnLen = 80;
	private int batchLength = 200000;
	// number of records to process in each batch
	private String separator;
	private Logger logger = Logger.getLogger(CodingSeqQueryRunner.class.getName());
	private Query query = null;
	private String dataset = null;
	private String species = null;
	private FormatSpec format = null;
	private FormattedSequencePrintStream osr = null;

	private TreeMap geneiDs = new TreeMap();
	// holds each objects information, in order
	private List fields = new ArrayList();
	// holds unique list of resultset description fields from the query

	// Used for colating required fields
	private String queryID;
	private String coordStart, coordEnd;
	private List displayIDs = new ArrayList();
	private final String Rank = "rank";
	private final String TranscriptID = "transcript_id";
	private final String Chr = "chr_name";
	private final String AssemblyColumn = "assembly_type";
	private final String StrandColumn = "exon_chrom_strand";

	// Strings for use in idattribute Hashtable keys
	private final String Location = "location";
	private final String Transcripts = "transcripts";
	private final String Assembly = "assembly";
	private final String Strand = "strand";
	private final String Geneloc = "geneloc";
	private final String DisplayID = "displayID";
	private final String Description = "description";
}
