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
 * Outputs Transcript Exon sequences in one of the supported formats
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported output formats
 */
public final class TranscriptExonSeqQueryRunner implements QueryRunner {
	/**
	 * Constructs a TranscriptExonSeqQueryRunner object to execute a Query
	 * and print Exon Sequences for each transcript.
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param os an OutputStream object
	 */
	public TranscriptExonSeqQueryRunner(Query query, FormatSpec format, OutputStream os) {
		this.query = query;
		this.format = format;
		this.osr = new FormattedSequencePrintStream(maxColumnLen, os, true); //autoflush true
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
		displayIDs.add("exon_stable_id_v");
		displayIDs.add("gene_stable_id_v");
		displayIDs.add("transcript_stable_id_v");
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

				int rows = 0; //rowcount			
				Integer lastGene = new Integer(0);
				// will nold last geneID when new one is encountered
				while (rs.next()) {
					rows++;
					Integer geneID = new Integer(rs.getInt(queryIDindex));
					Integer tranID = new Integer(rs.getInt(tranIDindex));
					Integer rank = new Integer(rs.getInt(rankIndex));

					if (!geneiDs.containsKey(geneID)) {
						if (geneiDs.size() > 0) {
							// write the previous genes data, and refresh the geneiDs TreeMap
							if (lastGene.intValue() > 0)
								seqWriter.writeSequences(lastGene);
							geneiDs = new TreeMap();
						}
						lastGene = geneID;
						Hashtable atts = new Hashtable();

						//order the transcripts by their rank in ascending order					
						atts.put(Transcripts, new TreeMap());
						atts.put(Assembly, rs.getString(assemblyIndex));
						geneiDs.put(geneID, atts);
					}

					Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
					TreeMap ordtrans = (TreeMap) geneatts.get(Transcripts);
					if (!(ordtrans.containsKey(tranID)))
						ordtrans.put(tranID, new Hashtable());

					int start = rs.getInt(startIndex);
					if (start > 0) {
						// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
						String chr = rs.getString(chromIndex);
						int end = rs.getInt(endIndex);
						int strand = rs.getInt(strandIndex);

						Hashtable tranatts = (Hashtable) ordtrans.get(tranID);

						// keep track of the lowest start and highest end for the gene
						if (!(geneatts.containsKey(Geneloc))) {
							geneatts.put(Geneloc, new SequenceLocation(chr, start, end, strand));
						} else {
							SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
							if (start < geneloc.getStart())
								geneatts.put(Geneloc, new SequenceLocation(chr, start, geneloc.getEnd(), strand));
							// overwrite the previous copy
							if (end > geneloc.getEnd())
								geneatts.put(Geneloc, new SequenceLocation(chr, geneloc.getStart(), end, strand));
							// overwrite the previous copy
						}

						// store each exon in rank order
						if (!(tranatts.containsKey(Exons))) {
							TreeMap exonMap = new TreeMap();
							tranatts.put(Exons, new TreeMap());
						}

						// need to add this exon to the Exons
						Hashtable exonatts = new Hashtable();
						exonatts.put(Location, new SequenceLocation(chr, start, end, strand));
						((TreeMap) (tranatts.get(Exons))).put(rank, exonatts);
					}

					Hashtable exonatts = (Hashtable) ((TreeMap) ((Hashtable) ordtrans.get(tranID)).get(Exons)).get(rank);
					// just need the information for this exon

					//	process displayID, if necessary
					if (!(exonatts.containsKey(DisplayID))) {
						StringBuffer displayID = new StringBuffer();

						for (int i = 0, n = displayIDindices.size(); i < n; i++) {
							if (i > 0)
								displayID.append(separator);
							int currindex = ((Integer) displayIDindices.get(i)).intValue();
							if (rs.getString(currindex) != null)
								displayID.append(rs.getString(currindex));
						}

						exonatts.put(DisplayID, displayID.toString());
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

							if (exonatts.containsKey(field)) {
								if (!((ArrayList) exonatts.get(field)).contains(value))
									 ((ArrayList) exonatts.get(field)).add(value);
							} else {
								List values = new ArrayList();
								values.add(value);
								exonatts.put(field, values);
							}
						}
					}

					// add the description, if necessary
					if (!exonatts.containsKey(Description))
						exonatts.put(Description, seqd.getDescription());
				}
				// write the last genes data, if present
				if (lastGene.intValue() > 0)
					seqWriter.writeSequences(lastGene);

				if (rows < batchLength)
					moreRows = false;
			}
		} catch (SQLException e) {
			throw new InvalidQueryException(e + ":" + sql);
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
				Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
				SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
				String assemblyout = (String) geneatts.get(Assembly);

				// cache the gene sequence
				dna.CacheSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());

				for (Iterator tranIter = ((TreeMap) geneatts.get(Transcripts)).keySet().iterator(); tranIter.hasNext();) {
					Hashtable tranatts = (Hashtable) ((TreeMap) geneatts.get(Transcripts)).get(tranIter.next());

					for (Iterator exonIter = ((TreeMap) tranatts.get(Exons)).keySet().iterator(); exonIter.hasNext();) {
						Hashtable exonatts = (Hashtable) ((TreeMap) (tranatts.get(Exons))).get(exonIter.next());
						SequenceLocation exonloc = (SequenceLocation) exonatts.get(Location);

						// write the header, starting with the displayID
						osr.print((String) exonatts.get(DisplayID));

						String strandout = exonloc.getStrand() > 0 ? "forward" : "revearse";
						osr.print(
							separator
								+ "strand="
								+ strandout
								+ separator
								+ "chr="
								+ exonloc.getChr()
								+ separator
								+ "assembly="
								+ assemblyout);

						if (osr.checkError())
							throw new IOException();

						for (int j = 0, n = fields.size(); j < n; j++) {
							osr.print(separator);
							String field = (String) fields.get(j);
							if (exonatts.containsKey(field)) {
								List values = (ArrayList) exonatts.get(field);

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

						// write the description
						osr.print(separator + (String) exonatts.get(Description));
						osr.print(separator);

						if (osr.checkError())
							throw new IOException();

						//extend flanking sequence if necessary
						int lflank = query.getSequenceDescription().getLeftFlank();
						int rflank = query.getSequenceDescription().getRightFlank();

						if (lflank > 0)
							exonloc = exonloc.extendLeftFlank(lflank);
						if (rflank > 0)
							exonloc = exonloc.extendRightFlank(rflank);

						// write out the sequence
						if (exonloc.getStrand() < 0)
							osr.write(
								SequenceUtil.reverseComplement(
									dna.getSequence(species, exonloc.getChr(), exonloc.getStart(), exonloc.getEnd())));
						else
							osr.write(dna.getSequence(species, exonloc.getChr(), exonloc.getStart(), exonloc.getEnd()));

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
			try {
				Hashtable geneatts = (Hashtable) geneiDs.get(geneID);
				SequenceLocation geneloc = (SequenceLocation) geneatts.get(Geneloc);
				String assemblyout = (String) geneatts.get(Assembly);

				// cache the gene sequence
				dna.CacheSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd());

				for (Iterator tranIter = ((TreeMap) geneatts.get(Transcripts)).keySet().iterator(); tranIter.hasNext();) {
					Hashtable tranatts = (Hashtable) ((TreeMap) geneatts.get(Transcripts)).get(tranIter.next());

					for (Iterator exonIter = ((TreeMap) tranatts.get(Exons)).keySet().iterator(); exonIter.hasNext();) {
						Hashtable exonatts = (Hashtable) ((TreeMap) (tranatts.get(Exons))).get(exonIter.next());
						SequenceLocation exonloc = (SequenceLocation) exonatts.get(Location);

						// write the header, starting with the displayID
						osr.print(">" + (String) exonatts.get(DisplayID));

						String strandout = exonloc.getStrand() > 0 ? "forward" : "revearse";
						osr.print(
							"\tstrand=" + strandout + separator + "chr=" + exonloc.getChr() + separator + "assembly=" + assemblyout);

						if (osr.checkError())
							throw new IOException();

						for (int j = 0, n = fields.size(); j < n; j++) {
							osr.print(separator);
							String field = (String) fields.get(j);
							if (exonatts.containsKey(field)) {
								List values = (ArrayList) exonatts.get(field);

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

						// write the description
						osr.print(separator + (String) exonatts.get(Description));
						osr.print("\n");

						if (osr.checkError())
							throw new IOException();

						//extend flanking sequence if necessary
						int lflank = query.getSequenceDescription().getLeftFlank();
						int rflank = query.getSequenceDescription().getRightFlank();

						if (lflank > 0)
							exonloc = exonloc.extendLeftFlank(lflank);
						if (rflank > 0)
							exonloc = exonloc.extendRightFlank(rflank);

						// write out the sequence
						if (exonloc.getStrand() < 0)
							osr.writeSequence(
								SequenceUtil.reverseComplement(
									dna.getSequence(species, exonloc.getChr(), exonloc.getStart(), exonloc.getEnd())));
						else
							osr.writeSequence(dna.getSequence(species, exonloc.getChr(), exonloc.getStart(), exonloc.getEnd()));

						osr.print("\n");
						osr.resetColumnCount();

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
	private Logger logger = Logger.getLogger(TranscriptExonSeqQueryRunner.class.getName());
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
	private String coordStart, coordEnd;
	private List displayIDs = new ArrayList();
	private final String TranscriptID = "transcript_id";
	private final String Rank = "rank";
	private final String Chr = "chr_name";
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
