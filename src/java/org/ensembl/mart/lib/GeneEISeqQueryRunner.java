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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.ensembl.util.FormattedSequencePrintStream;
import org.ensembl.util.SequenceUtil;

/**
 * Writes out Gene Sequences Exons and Introns) in one of the supported formats.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported formats
 */
public final class GeneEISeqQueryRunner extends BaseSeqQueryRunner {

	private final String GENELOC = "geneloc";
  private Logger logger = Logger.getLogger(GeneEISeqQueryRunner.class.getName());
  
	/**
	 * Constructs a GeneEISeqQueryRunner object to execute a Query
	 * and print Gene Sequences (Exons and Introns).
	 * 
	 * @param query a Query Object
	 * @param format a FormatSpec object
	 * @param os an OutputStream object
	 */
	public GeneEISeqQueryRunner(Query query, FormatSpec format, OutputStream os) {
		this.query = query;
		this.format = format;
		this.osr = new FormattedSequencePrintStream(maxColumnLen, os, true); // autoflush true

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
			if (Pattern.matches(".*gene__main", mainTables[i]))
				dataset = mainTables[i];
		}

		StringTokenizer tokens = new StringTokenizer(dataset, "_", false);
		species = tokens.nextToken();
		focus = tokens.nextToken();
		dset = species + "_" + focus;
		structureTable = dset + "__structure__dm";
	}

	protected void updateQuery() {
		queryID = GENEID;	
		coordStart = "exon_chrom_start";
		coordEnd = "exon_chrom_end";
		displayIDs.add("gene_stable_id_v");
		
		query.addAttribute(new FieldAttribute(queryID, structureTable,"transcript_id_key"));
		query.addAttribute(new FieldAttribute(ASSEMBLYCOLUMN, structureTable,"transcript_id_key"));
		query.addAttribute(new FieldAttribute(coordStart, structureTable,"transcript_id_key"));
		query.addAttribute(new FieldAttribute(coordEnd, structureTable,"transcript_id_key"));
		query.addAttribute(new FieldAttribute(CHR, structureTable,"transcript_id_key"));
		query.addAttribute(new FieldAttribute(STRANDCOLUMN, structureTable,"transcript_id_key"));

		for (int i = 0; i < displayIDs.size(); i++) {
			query.addAttribute(new FieldAttribute((String) displayIDs.get(i), structureTable,"transcript_id_key"));
		}
	}
  
	protected void processResultSet(Connection conn, ResultSet rs) throws IOException, SQLException {
		ResultSetMetaData rmeta = rs.getMetaData();

		// process columnNames for required attribute indices
		for (int i = 1, nColumns = rmeta.getColumnCount(); i <= nColumns; ++i) {
			String column = rmeta.getColumnName(i);
			if (column.equals(queryID))
				queryIDindex = i;
			else if (column.equals(ASSEMBLYCOLUMN))
				assemblyIndex = i;
			else if (column.equals(coordStart))
				startIndex = i;
			else if (column.equals(coordEnd))
				endIndex = i;
			else if (column.equals(CHR))
				chromIndex = i;
			else if (column.equals(STRANDCOLUMN))
				strandIndex = i;
			else if (displayIDs.contains(column))
				displayIDindices.add(new Integer(i));
			else
				otherIndices.add(new Integer(i));
		}

		while (rs.next()) {
			Integer keyID = new Integer(rs.getInt(queryIDindex));

			if ( keyID.intValue() != lastID  ) {
				if ( lastID > -1  ) {
					//This is not the first ID in a batch, process the previous ID sequences
					seqWriter.writeSequences(new Integer(lastID), conn);
				}
       
				//refresh the iDs TreeMap  
				iDs = new TreeMap();
				lastIDRowsProcessed = 0; // refresh for the new ID

				Hashtable atts = new Hashtable();
				atts.put(ASSEMBLY, rs.getString(assemblyIndex));
				iDs.put(keyID, atts);
			}
			
			Hashtable atts = (Hashtable) iDs.get(keyID);

			int start = rs.getInt(startIndex);
			if (start > 0) {
				// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
				String chr = rs.getString(chromIndex);
				int end = rs.getInt(endIndex);
				int strand = rs.getInt(strandIndex);

				// keep track of the lowest start and highest end for the gene
				if (!(atts.containsKey(GENELOC))) {
					atts.put(GENELOC, new SequenceLocation(chr, start, end, strand));
				} else {
					SequenceLocation geneloc = (SequenceLocation) atts.get(GENELOC);
					if (start < geneloc.getStart())
						atts.put(GENELOC, new SequenceLocation(chr, start, geneloc.getEnd(), strand));
					// overwrite the previous copy
					if (end > geneloc.getEnd())
						atts.put(GENELOC, new SequenceLocation(chr, geneloc.getStart(), end, strand));
					// overwrite the previous copy
				}
			}

			//	process displayID, if necessary
			if (!(atts.containsKey(DISPLAYID))) {
				StringBuffer displayID = new StringBuffer();

				for (int i = 0, n = displayIDindices.size(); i < n; i++) {

					int currindex = ((Integer) displayIDindices.get(i)).intValue();
					if (rs.getString(currindex) != null) {
						String thisID = rs.getString(currindex);
						if (displayID.indexOf(thisID) < 0) {
							if (i > 0)
								displayID.append(separator);
							displayID.append(thisID);
						}
					}
				}

				atts.put(DISPLAYID, displayID.toString());
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

					if (atts.containsKey(field)) {
						if (!((ArrayList) atts.get(field)).contains(value))
							 ((ArrayList) atts.get(field)).add(value);
					} else {
						List values = new ArrayList();
						values.add(value);
						atts.put(field, values);
					}
				}
			}

			// add the description, if necessary
			if (!atts.containsKey(DESCRIPTION))
				atts.put(DESCRIPTION, seqd.getDescription());
								
			totalRows++;
			resultSetRowsProcessed++;
			lastID = keyID.intValue();
			lastIDRowsProcessed++;	
		}
	}

	private final SeqWriter tabulatedWriter = new SeqWriter() {
		void writeSequences(Integer geneID, Connection conn) throws SequenceException {
			try {
				DNAAdaptor dna = new DNAAdaptor(conn);

				Hashtable geneatts = (Hashtable) iDs.get(geneID);
				SequenceLocation geneloc = (SequenceLocation) geneatts.get(GENELOC);

				osr.print((String) geneatts.get(DISPLAYID));
				String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				osr.print(separator + "strand=" + strandout + separator + "chr=" + geneloc.getChr() + separator + "assembly=" + (String) geneatts.get(ASSEMBLY));

				if (osr.checkError())
					throw new IOException();

				for (int j = 0, n = fields.size(); j < n; j++) {
					osr.print(separator);
					String field = (String) fields.get(j);
					if (geneatts.containsKey(field)) {
						List values = (ArrayList) geneatts.get(field);

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
				osr.print(separator + (String) geneatts.get(DESCRIPTION));
				osr.print(separator);

				if (osr.checkError())
					throw new IOException();

				//modify flanks, if necessary, and write sequence
				if (query.getSequenceDescription().getLeftFlank() > 0)
					geneloc = geneloc.extendLeftFlank(query.getSequenceDescription().getLeftFlank());
				if (query.getSequenceDescription().getRightFlank() > 0)
					geneloc = geneloc.extendRightFlank(query.getSequenceDescription().getRightFlank());

				if (geneloc.getStrand() < 0)
					osr.write(SequenceUtil.reverseComplement(dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd())));
				else
					osr.write(dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd()));

				osr.print("\n");

				if (osr.checkError())
					throw new IOException();
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

				Hashtable geneatts = (Hashtable) iDs.get(geneID);
				SequenceLocation geneloc = (SequenceLocation) geneatts.get(GENELOC);

				osr.print(">" + (String) geneatts.get(DISPLAYID));
				String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";
				osr.print("\tstrand=" + strandout + separator + "chr=" + geneloc.getChr() + separator + "assembly=" + (String) geneatts.get(ASSEMBLY));

				if (osr.checkError())
					throw new IOException();

				for (int j = 0, n = fields.size(); j < n; j++) {
					osr.print(separator);
					String field = (String) fields.get(j);
					if (geneatts.containsKey(field)) {
						List values = (ArrayList) geneatts.get(field);

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
				osr.print(separator + (String) geneatts.get(DESCRIPTION));
				osr.print("\n");

				if (osr.checkError())
					throw new IOException();

				//modify flanks, if necessary, and write sequence
				if (query.getSequenceDescription().getLeftFlank() > 0)
					geneloc = geneloc.extendLeftFlank(query.getSequenceDescription().getLeftFlank());
				if (query.getSequenceDescription().getRightFlank() > 0)
					geneloc = geneloc.extendRightFlank(query.getSequenceDescription().getRightFlank());

				if (geneloc.getStrand() < 0)
					osr.writeSequence(SequenceUtil.reverseComplement(dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd())));
				else
					osr.writeSequence(dna.getSequence(species, geneloc.getChr(), geneloc.getStart(), geneloc.getEnd()));
				osr.print("\n");
				osr.resetColumnCount();

				if (osr.checkError())
					throw new IOException();
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
}
