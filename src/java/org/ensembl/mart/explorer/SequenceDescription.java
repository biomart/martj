/**
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
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Contains the knowledge for creating the different sequences
 * that Mart is able to generate.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public final class SequenceDescription {
	
	/**
	 * default constructor private, so SequeceDescription cannot be subclassed
	 */
	private SequenceDescription() {
	}
	
	/**
	 * Construct a SequenceDescription of a specified type.
	 * type can be one of the static sequence types.
	 * This is useful for types that cannot have
	 * their flanking sequences extended.
	 * 
	 * @param type int
	 */
	public SequenceDescription(int type) throws InvalidQueryException {
		this(type, 0, 0);
	}
	
	/**
	 * Construct a fully qualified SequenceDescription with all
	 * necessary information.
	 * 
	 * @param type Must be one of the static enums
	 * @param leftflank length of left flanking sequence to append to the resulting sequences
	 * @param rightflank length of right flanking sequence to append to the resulting sequences
	 * @throws InvalidQueryException if the client requests an unknown sequence type,
	 *         or requests flanking sequence for a sequence which is not applicable for flanking sequence.
	 */
	public SequenceDescription(int type, int lflank, int rflank) throws InvalidQueryException {
		switch(type) {
			case TRANSCRIPTCODING:
				if (lflank > 0 || rflank > 0)
					throw new InvalidQueryException("Cannot extend flanks on a coding sequence\n");
		       
				this.seqt = type;
				this.seqtype = CODINGSEQ;
				this.description = "coding sequence of transcript";
				break;
				
			case TRANSCRIPTPEPTIDE:
				if (lflank > 0 || rflank > 0)
					throw new InvalidQueryException("Cannot extend flanks on a peptide sequence\n");
		       
				this.seqt = type;
				this.seqtype = PEPTIDESEQ;
				this.description = "peptide sequence";
				break;
			    
			case TRANSCRIPTCDNA:
				if (lflank > 0 || rflank > 0)
					throw new InvalidQueryException("Cannot extend flanks on a cdna sequence\n");
		       
				this.seqt = type;
				this.seqtype = CDNASEQ;
				this.description = "cdna sequence";
				break;
				
			case TRANSCRIPTEXONS:
			    this.seqt = type;
			    this.seqtype = TRANSCRIPTEXONSEQ;
			    this.description = "exon";
			    break;
			      
			default:
				throw new InvalidQueryException("Unknown sequence type"+type);
		}
	}
	
    /**
	 * Returns the string representation of the sequence type, 
	 *  
	 * @return String type
	 */
    public String getType() {
    	return seqtype;
    }
    
    /**
     * Returns the intiger code for the sequence type,
     * which can be tested against the static sequence type
     * enums.
     * 
     * @return int seqt
     */
    public int getSeqCode() {
    	return seqt;
    }
    /**
     * Returns the length of the left flank
     * 
     * @return int leftflank
     */
    public int getLeftFlank() {
    	return leftflank;
    }
    
    /**
     * Returns the length of the right flank
     * 
     * @return int rightflank
     */
    public int getRightFlank() {
    	return rightflank;
    }
	
	protected String getDescription() {
		return description;
	}
	
	public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        buf.append(" type=").append(seqtype);
        buf.append(", leftflank=").append(leftflank);
        buf.append(", rightflank=").append(rightflank);
        buf.append("]");
        
        return buf.toString();		
	}
	/**
	 * class method that returns a string with all implemented
	 * sequences.  Used by the UI to print information on
	 * implemented sequences.
	 * 
	 * @return String Printable List of implemented Sequences
	 */
	public static String getAvailableSequences(){
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < SEQS.size(); i++) {
			buf.append("\n"+(String) SEQS.get(i));
		}
		
		return buf.toString();
	}
	
	private String seqtype;
	private int seqt = 0; // will hold one of the enums below
	private String description;
	private int leftflank = 0;
	private int rightflank = 0;
	
	private Logger logger = Logger.getLogger(SequenceDescription.class.getName());
	
	//TODO: add new enums as new types of sequences are implimented
	public static final List SEQS = Arrays.asList( new String[]{"coding", "peptide" , "cdna", "transcript_exons"});
	
	/**
	 * enums over sequence type that objects can use to test the type, and SequenceDescription can use to set seqtype
	 */
	public static final int TRANSCRIPTCODING = 1;
	public static final String CODINGSEQ = "coding";
	public static final int TRANSCRIPTPEPTIDE = 2;
	public static final String PEPTIDESEQ = "peptide";
	public static final int TRANSCRIPTCDNA = 3;
	public static final String CDNASEQ = "cdna";
	public static final int TRANSCRIPTEXONS = 4;
	public static final String TRANSCRIPTEXONSEQ = "transcript_exons";
	public static final int TRANSCRIPTEXONINTRON = 5;
	public static final String TRANSCRIPTEISEQ = "transcript_exon_intron";
	public static final int GENEEXONINTRON = 6;
	public static final String GENEEISEQ = "gene_exon_intron";
	public static final int GENEEXONS = 7;
	public static final String GENEEXONSEQ = "gene_exons";
}
