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
import java.util.Collections;
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
				this.seqtype = (String) SEQS.get(TRANSCRIPTCODING);
				this.description = "coding sequence of transcript";
				break;
				
			case TRANSCRIPTPEPTIDE:
				if (lflank > 0 || rflank > 0)
					throw new InvalidQueryException("Cannot extend flanks on a peptide sequence\n");
		       
				this.seqt = type;
				this.seqtype = (String) SEQS.get(TRANSCRIPTPEPTIDE);
				this.description = "peptide sequence";
				break;
			    
			case TRANSCRIPTCDNA:
				if (lflank > 0 || rflank > 0)
					throw new InvalidQueryException("Cannot extend flanks on a cdna sequence\n");
		       
				this.seqt = type;
				this.seqtype = (String) SEQS.get(TRANSCRIPTCDNA);
				this.description = "cdna sequence";
				break;
				
			case TRANSCRIPTEXONS:
			    this.seqt = type;
			    this.seqtype = (String) SEQS.get(TRANSCRIPTEXONS);
			    this.description = "exon";
			    this.leftflank = lflank;
			    this.rightflank = rflank;
			    if (lflank > 0)
			        this.description = "upstream flanking sequence plus "+this.description;
			    if (rflank > 0)
			        this.description += " plus downstream flanking sequence";
			    break;
			
		    case TRANSCRIPTEXONINTRON:
		        this.seqt = type;
		        this.seqtype = (String) SEQS.get(TRANSCRIPTEXONINTRON);
		        this.description = "exon and intron sequence for transcript";
		        this.leftflank = lflank;
		        this.rightflank = rflank;
			      if (lflank > 0)
				      this.description = "upstream flanking sequence plus "+this.description;
			      if (rflank > 0)
				      this.description += " plus downstream flanking sequence";		        
		        break;
		        
			case TRANSCRIPTFLANKS:
			    if (lflank > 0 && rflank > 0)
				    throw new InvalidQueryException("Cannot create both 3' and 5' transcript flanking sequence in one sequence\n");
				  if (! (lflank >0 || rflank > 0) )
				    throw new InvalidQueryException("Transcript flanking requires either 3' or 5' flanking length\n");
				    
				  this.seqt = type;
				  this.seqtype = (String) SEQS.get(TRANSCRIPTFLANKS);
				  this.description = "flanking sequence of transcript only";
				  this.leftflank = lflank;
				  this.rightflank = rflank;
				  if (lflank > 0)
				    this.description = "upstream "+this.description;
				  else
				    this.description = "downstream "+this.description;
				  break;

        case GENEEXONS:
		      this.seqt = type;
		      this.seqtype = (String) SEQS.get(TRANSCRIPTEXONS);
		      this.description = "exon";
		      this.leftflank = lflank;
		      this.rightflank = rflank;
		      if (lflank > 0)
			      this.description = "upstream flanking sequence plus "+this.description;
		      if (rflank > 0)
			      this.description += " plus downstream flanking sequence";
		      break;
		
				case GENEEXONINTRON:
				  this.seqt = type;
				  this.seqtype = (String) SEQS.get(GENEEXONINTRON);
				  this.description = "exon and intron sequence for gene";
				  this.leftflank = lflank;
				  this.rightflank = rflank;		        
			    if (lflank > 0)
				    this.description = "upstream flanking sequence plus "+this.description;
			    if (rflank > 0)
				    this.description += " plus downstream flanking sequence";
		      break;

			case GENEFLANKS:
				if (lflank > 0 && rflank > 0)
					throw new InvalidQueryException("Cannot create both 3' and 5' gene flanking sequence in one sequence\n");
				  if (! (lflank >0 || rflank > 0) )
					throw new InvalidQueryException("Gene flanking requires either 3' or 5' flanking length\n");
				    
				  this.seqt = type;
				  this.seqtype = (String) SEQS.get(GENEFLANKS);
				  this.description = "flanking sequence of gene only";
				  this.leftflank = lflank;
				  this.rightflank = rflank;
				  if (lflank > 0)
					this.description = "upstream "+this.description;
				  else
					this.description = "downstream "+this.description;
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
    public String getTypeAsString() {
    	return seqtype;
    }
    
    /**
     * Returns the intiger code for the sequence type,
     * which can be tested against the static sequence type
     * enums.
     * 
     * @return int seqt
     */
    public int getType() {
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
	public static final List SEQS = Collections.unmodifiableList(Arrays.asList( new String[]{"coding", 
                                                                                                                                                    "peptide" , 
                                                                                                                                                    "cdna", 
                                                                                                                                                    "transcript_exons", 
                                                                                                                                                    "transcript_exon_intron",
                                                                                                                                                    "transcript_flanks",
                                                                                                                                                    "gene_exon_intron",
                                                                                                                                                    "gene_exons",
                                                                                                                                                    "gene_flanks"
                                                                                                                                                    }));
	
	/**
	 * enums over sequence type that objects can use to test the type, and SequenceDescription can use to set seqtype
	 */
	public static final int TRANSCRIPTCODING = 0;
	public static final int TRANSCRIPTPEPTIDE = 1;
	public static final int TRANSCRIPTCDNA = 2;
	public static final int TRANSCRIPTEXONS = 3;
	public static final int TRANSCRIPTEXONINTRON = 4;
	public static final int TRANSCRIPTFLANKS = 5;
	public static final int GENEEXONINTRON = 6;
	public static final int GENEEXONS = 7;
	public static final int GENEFLANKS = 8;
}
