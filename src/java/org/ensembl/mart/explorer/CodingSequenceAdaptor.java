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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.ensembl.util.SequenceUtil;

/**
 * SequenceAdaptor implimenting object for coding sequences.
 * The printSequence method will warn and ignore any
 * lflank or rflank provided.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class CodingSequenceAdaptor implements SequenceAdaptor {
	
	/**
	 * Constuctor for a CodingSequenceAdaptor for a particular
	 * species and focus.
	 * 
	 * @param species
	 * @param focus
	 */
	public CodingSequenceAdaptor(String species, String focus, Connection conn) {
		this.species = species;
		this.focus = focus;
		this.conn = conn;
		
		sql = "select chr_name, exon_chrom_strand, coding_start, coding_end from "+species+"_core_gene_structure where transcript_id = ? order by transcript_id, rank";
		//TODO: table renaming: sql = "select chr_name, exon_chrom_strand, coding_start, coding_end from "+species+"_"+focus+"gene_structure_dm where transcript_id = ? order by transcript_id, rank";
		dna = new DNAAdaptor(conn);
	}

	public Sequence[] getSequences(int QueryID, String displayID, String description, String separator, int lflank, int rflank) throws SQLException, SequenceException {
		if (lflank > 0 || rflank > 0)
		    logger.info("Ignoring flank modifiers in coding sequence");

		Sequence[] s;        
        int genelow = 500000000; // guarantees that first comparison with an actual value will be lower than maximum chromosome start in the database
        int genehi = -1; // guarantees that first comparison with an actual value will be higher
        int tstrand = 0;        
        String genechr = null; // gene chr_name
        ArrayList locations = new ArrayList();
        
        /*
         * Build up an array of Sequence Objects, with the correct coordinates for fetching the sequence
         * from the DNAAdaptor.  After a cachSequence call on the hi and low coordinates of the gene,
         * iterate through the sequences ArrayList, setting each Sequence object sequence with a call 
         * to DNAAdpaptor.getSequence.
         */
	    PreparedStatement ps = conn.prepareStatement( sql );
		ps.setInt(1, QueryID);
			
		ResultSet rs = ps.executeQuery();
		
		while (rs.next()) {
			int start = rs.getInt(START);
			
			if (start == 0) continue; // some starts are null, skip these
			
			String chr = rs.getString(CHR);
			int end = rs.getInt(END);
			int strand = rs.getInt(STRAND);
			
			if (genechr == null)
			    genechr = chr;
			
			if (start < genelow)
			    genelow = start;
			
			if (end > genehi)
			    genehi = end;
			
			if (tstrand == 0)
			    tstrand = strand;
			         
            locations.add(new SequenceLocation(chr, start, end, strand));
		}
		
		if (locations.isEmpty()) {
		    logger.info("Did not get any Gene_Structure Data for gene"+QueryID);
		    s = new Sequence[0];
		}
		else {    
		    dna.CacheSequence(species, genechr, genelow, genehi);

			ArrayList sequences = new ArrayList();
			StringBuffer sequence = new StringBuffer();
					
		    for (int i = 0; i < locations.size(); i++) {
			    SequenceLocation loc = (SequenceLocation) locations.get(i);
			    String seqchunk = dna.getSequence(species, loc.getChr(), loc.getStart(), loc.getEnd());
			    if (loc.getStrand() < 0) seqchunk = SequenceUtil.reverseComplement(seqchunk);
			    sequence.append(seqchunk);
		    }
		    
			if (tstrand == -1)
				description = "strand=revearse"+separator+description;
		    else
                description = "strand=forward"+separator+description;
                
			Sequence seq = new Sequence(displayID, description, sequence.toString());
		    sequences.add(seq);

			s = new Sequence[sequences.size()];
			sequences.toArray( s );
		}
		return s;
	}
	
	private Logger logger = Logger.getLogger(CodingSequenceAdaptor.class.getName());
	private DNAAdaptor dna;
	private String species = null;
	private String focus = null;
	private String sql = null;
	private Connection conn = null;
	
	// positions of data in ResultSet from Gene Structure Table
	private final int CHR = 1;
	private final int STRAND = 2;
	private final int START = 3;
	private final int END = 4;
}
