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
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ensembl.mart.util.FormattedSequencePrintStream;
import org.ensembl.util.SequenceUtil;

/**
 * Outputs peptide sequence in one of the supported output format
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see FormatSpec for supported output formats
 */
public final class DownStreamUTRSeqQueryRunner extends BaseSeqQueryRunner {

	private final String TRANSCRIPTS = "transcripts";
	private final String LOCATIONS = "locations";
	private final String GENELOC = "geneloc";
  private Logger logger = Logger.getLogger(DownStreamUTRSeqQueryRunner.class.getName());
  
  /**
   * Constructs a PeptideSeqQueryRunner object to execute a Query
   * and print Peptide Sequences
   * 
   * @param query a Query Object
   * @param format a FormatSpec object
   * @param os an OutputStream object
   */
  public DownStreamUTRSeqQueryRunner(Query query, FormatSpec format, OutputStream os) {
    super(query);
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
  }

  protected void updateQuery() {
    queryID = GENEID;
    coordStart = "3utr_start";
    coordEnd = "3utr_end";
    displayIDs.add("transcript_stable_id_v");
    displayIDs.add("gene_stable_id_v");

    query.addAttribute(new FieldAttribute(TRANID, structureTable, "transcript_id_key"));
    query.addAttribute(new FieldAttribute(queryID, structureTable, "transcript_id_key"));
    query.addAttribute(new FieldAttribute(RANK, structureTable, "transcript_id_key"));
    query.addAttribute(new FieldAttribute(ASSEMBLYCOLUMN, structureTable, "transcript_id_key"));
    query.addAttribute(new FieldAttribute(coordStart, structureTable, "transcript_id_key"));
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
      if (column.equals(TRANID))
        tranIDindex = i;
      else if (column.equals(queryID))
        queryIDindex = i;
      else if (column.equals(RANK))
        rankIndex = i;
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
			Integer tranID = new Integer(rs.getInt(tranIDindex));
			Integer rank = new Integer(rs.getInt(rankIndex));

			if ( keyID.intValue() != lastID ) {
				if ( lastID > -1  ) {
					//This is not the first ID in a batch, process the previous ID sequences
					seqWriter.writeSequences(new Integer(lastID), conn);
				}
       
				//refresh the iDs TreeMap  
				iDs = new TreeMap();
				lastIDRowsProcessed = 0; // refresh for the new ID
				
				
				Hashtable atts = new Hashtable();

				atts.put(ASSEMBLY, rs.getString(assemblyIndex));
				atts.put(TRANSCRIPTS, new TreeMap());
				iDs.put(keyID, atts);
			}

			Hashtable geneAtts = (Hashtable) iDs.get(keyID);
			TreeMap traniDs = (TreeMap) geneAtts.get(TRANSCRIPTS);

			if (!(traniDs.containsKey(tranID))) {
				Hashtable atts = new Hashtable();
				atts.put(LOCATIONS, new TreeMap());
				atts.put(HASUTR, Boolean.FALSE);
				// initialize to false, over ride if necessary
				traniDs.put(tranID, atts);
			}
			
			Hashtable atts = (Hashtable) traniDs.get(tranID);

			int start = rs.getInt(startIndex);
			if (start > 0) {
				// if start is not null, create a new SequenceLocation object from the chr, start, end, and strand
				atts.put(HASUTR, Boolean.TRUE);

				String chr = rs.getString(chromIndex);
				int end = rs.getInt(endIndex);
				int strand = rs.getInt(strandIndex);

				//	order the locations by their rank in ascending order
				 ((TreeMap) atts.get(LOCATIONS)).put(rank, new SequenceLocation(chr, start, end, strand));

				// keep track of the lowest start and highest end for the gene	
				if (!(geneAtts.containsKey(GENELOC))) {
					geneAtts.put(GENELOC, new SequenceLocation(chr, start, end, strand));
				} else {
					SequenceLocation geneloc = (SequenceLocation) geneAtts.get(GENELOC);
					if (start < geneloc.getStart()) {
						geneAtts.put(GENELOC, new SequenceLocation(chr, start, geneloc.getEnd(), strand));
						// overwrite the previous copy
						if (end > geneloc.getEnd())
							geneAtts.put(GENELOC, new SequenceLocation(chr, geneloc.getStart(), end, strand));
						// overwrite the previous copy
					}
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
			if (!(atts.containsKey(DESCRIPTION)))
				atts.put(DESCRIPTION, seqd.getDescription());
				
			totalRows++;
			resultSetRowsProcessed++;
			lastID = keyID.intValue();
			lastIDRowsProcessed++;
		}
  }

  private final SeqWriter tabulatedWriter = new SeqWriter() {
    void writeSequences(Integer geneID, Connection conn) throws SequenceException {
      Hashtable geneatts = (Hashtable) iDs.get(geneID);
      TreeMap traniDs = (TreeMap) geneatts.get(TRANSCRIPTS);
      SequenceLocation geneloc = (SequenceLocation) geneatts.get(GENELOC);

      try {
        DNAAdaptor dna = new DNAAdaptor(conn);

        for (Iterator tranIter = traniDs.keySet().iterator(); tranIter.hasNext();) {
          Hashtable tranatts = (Hashtable) traniDs.get((Integer) tranIter.next());

          if (((Boolean) tranatts.get(HASUTR)).booleanValue()) {
            String assemblyout = (String) geneatts.get(ASSEMBLY);

            String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";

            osr.print((String) tranatts.get(DISPLAYID));

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

            osr.print(separator + (String) tranatts.get(DESCRIPTION));
            osr.print(separator);
            if (osr.checkError())
              throw new IOException();

            TreeMap locations = (TreeMap) tranatts.get(LOCATIONS);

            ArrayList seqout = new ArrayList();
            int seqLen = 0;

            // to collect all sequence before appending flanks
            for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext();) {
              SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
              byte[] theseBytes = null;

              if (loc.getStrand() < 0)
                theseBytes =
                  SequenceUtil.reverseComplement(dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()));
              else
                theseBytes = dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd());

              seqLen += theseBytes.length;
              seqout.add(theseBytes);
            }

            if (query.getSequenceDescription().getRightFlank() > 0) {
              // extend flanking sequence
              SequenceLocation first_loc = (SequenceLocation) locations.get((Integer) locations.firstKey());
              SequenceLocation last_loc = (SequenceLocation) locations.get((Integer) locations.lastKey());

              //both use last location
              SequenceLocation flank_loc = last_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank());
              byte[] theseBytes = null;

              if (first_loc.getStrand() < 0) {
                // right flank of last location reverse complimented

                theseBytes =
                  SequenceUtil.reverseComplement(
                    dna.getSequence(species, flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()));
              } else {

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
            osr.print((String) tranatts.get(DISPLAYID));
            osr.print(separator + (String) tranatts.get(DESCRIPTION));
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
    void writeSequences(Integer geneID, Connection conn) throws SequenceException {
      Hashtable geneatts = (Hashtable) iDs.get(geneID);
      TreeMap traniDs = (TreeMap) geneatts.get(TRANSCRIPTS);
      SequenceLocation geneloc = (SequenceLocation) geneatts.get(GENELOC);

      try {
        DNAAdaptor dna = new DNAAdaptor(conn);

        for (Iterator tranIter = traniDs.keySet().iterator(); tranIter.hasNext();) {
          Hashtable tranatts = (Hashtable) traniDs.get((Integer) tranIter.next());

          if (((Boolean) tranatts.get(HASUTR)).booleanValue()) {
            String assemblyout = (String) geneatts.get(ASSEMBLY);
            String strandout = geneloc.getStrand() > 0 ? "forward" : "revearse";

            // write the header, starting with the displayID
            osr.print(">" + (String) tranatts.get(DISPLAYID));

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

            osr.print(separator + (String) tranatts.get(DESCRIPTION));
            osr.print("\n");
            if (osr.checkError())
              throw new IOException();

            TreeMap locations = (TreeMap) tranatts.get(LOCATIONS);

            ArrayList seqout = new ArrayList();
            int seqLen = 0;

            // to collect all sequence before appending flanks
            for (Iterator lociter = locations.keySet().iterator(); lociter.hasNext();) {
              SequenceLocation loc = (SequenceLocation) locations.get((Integer) lociter.next());
              byte[] theseBytes = null;

              if (loc.getStrand() < 0)
                theseBytes =
                  SequenceUtil.reverseComplement(dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd()));
              else
                theseBytes = dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd());

              seqLen += theseBytes.length;
              seqout.add(theseBytes);
            }

            if (query.getSequenceDescription().getRightFlank() > 0) {
              // extend flanking sequence
              SequenceLocation first_loc = (SequenceLocation) locations.get((Integer) locations.firstKey());
              SequenceLocation last_loc = (SequenceLocation) locations.get((Integer) locations.lastKey());

              //both use last location
              SequenceLocation flank_loc = last_loc.getRightFlankOnly(query.getSequenceDescription().getRightFlank());
              byte[] theseBytes = null;

              if (first_loc.getStrand() < 0) {
                // right flank of first location

                theseBytes =
                  SequenceUtil.reverseComplement(
                    dna.getSequence(species, flank_loc.getChr(), flank_loc.getStart(), flank_loc.getEnd()));
              } else {                

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
            osr.print(">" + (String) tranatts.get(DISPLAYID));
            osr.print("\t" + (String) tranatts.get(DESCRIPTION));
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

  //flag for boolean
	private final String HASUTR = "hasUTR";
  //message to write when no UTR is available 
  private final String noUTRmessage = "No UTR is annotated for this transcript";
}
