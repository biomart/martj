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
				    throw new InvalidQueryException("Cannot create both upstream and downstream transcript flanking sequence in one sequence\n");
				  if (! (lflank >0 || rflank > 0) )
				    throw new InvalidQueryException("Transcript flanking requires either upstream or downstream flanking length\n");
				    
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
					throw new InvalidQueryException("Cannot create both upstream and downstream gene flanking sequence in one sequence\n");
				  if (! (lflank >0 || rflank > 0) )
					throw new InvalidQueryException("Gene flanking requires either upstream or downstream flanking length\n");
				    
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
				  
			case DOWNSTREAMUTR:
			    if (lflank > 0)
			      throw new InvalidQueryException("Cannot get upstream flanking sequence of downstream UTR");
			      
			    this.seqt = type;
			    this.seqtype = (String)	SEQS.get(DOWNSTREAMUTR);
			    this.description = "downstream UTR";
			    this.leftflank = lflank;
			    this.rightflank = rflank;
			    
			    if (rflank > 0)
			      this.description += " plus downstream flanking region"; 
			    break;
			    
			case UPSTREAMUTR:
				if (rflank > 0)
				  throw new InvalidQueryException("Cannot get downstream flanking sequence of upstream UTR");
			      
				this.seqt = type;
				this.seqtype = (String)	SEQS.get(UPSTREAMUTR);
				this.description = "upstream UTR";
				this.leftflank = lflank;
				this.rightflank = rflank;
			    
				if (lflank > 0)
				  this.description = "upstream flanking region "+this.description; 
				break;
			       		      		              
			default:
				throw new InvalidQueryException("Unknown sequence type"+type);
		}
	}
	
	/**
	 * Copy constructor.
	 * @param o - a SequenceDescription object
	 */
	public SequenceDescription(SequenceDescription o) {
		// since we are dealing with a previously constructed object, we dont need to do parameter checking.
		// so we wont need to throw any exceptions.
		
		int type = o.getType();
		int lflank = o.getLeftFlank();
		int rflank = o.getRightFlank();
		
		switch(type) {
			case TRANSCRIPTCODING:
				this.seqt = type;
				this.seqtype = (String) SEQS.get(TRANSCRIPTCODING);
				this.description = "coding sequence of transcript";
				break;
				
			case TRANSCRIPTPEPTIDE:
				this.seqt = type;
				this.seqtype = (String) SEQS.get(TRANSCRIPTPEPTIDE);
				this.description = "peptide sequence";
				break;
			    
			case TRANSCRIPTCDNA:
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
				  
			case DOWNSTREAMUTR:
					this.seqt = type;
					this.seqtype = (String)	SEQS.get(DOWNSTREAMUTR);
					this.description = "downstream UTR";
					this.leftflank = lflank;
					this.rightflank = rflank;
			    
					if (rflank > 0)
						this.description += " plus downstream flanking region"; 
					break;
			    
			case UPSTREAMUTR:
				this.seqt = type;
				this.seqtype = (String)	SEQS.get(UPSTREAMUTR);
				this.description = "upstream UTR";
				this.leftflank = lflank;
				this.rightflank = rflank;
			    
				if (lflank > 0)
					this.description = "upstream flanking region "+this.description; 
				break;
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
        buf.append(" type=").append(seqt);
        buf.append(", seqtype=").append(seqtype);
        buf.append(", leftflank=").append(leftflank);
        buf.append(", rightflank=").append(rightflank);
        buf.append(", description=").append(description);
        buf.append("]");
        
        return buf.toString();		
	}
	
	/**
	 * Allows Equality Comparison manipulation of SequenceDescription objects
	 */
	public boolean equals(Object o) {
		return o instanceof SequenceDescription && hashCode() == ((SequenceDescription) o).hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int tmp = seqt;
		tmp = (31 * tmp) + seqtype.hashCode();
		tmp = (31 * tmp) + leftflank;
		tmp = (31 * tmp) + rightflank;
		tmp = (31 * tmp) + description.hashCode();
	  return tmp;	
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
	
	// add new enums as new types of sequences are implimented
	public static final List SEQS = Collections.unmodifiableList(Arrays.asList( new String[]{"coding", 
                                                                                           "peptide" , 
                                                                                           "cdna", 
                                                                                           "transcript_exons", 
                                                                                           "transcript_exon_intron",
                                                                                           "transcript_flanks",                                                                                           
                                                                                           "gene_exon_intron",
                                                                                           "gene_exons",                                                                                           
                                                                                           "gene_flanks",
                                                                                           "downstream_utr",
                                                                                           "upstream_utr"
                                                                                          }
                                                                             )
                                                                );
  
  // descriptions for SEQS.  The order of the description must match the order of the SEQ                                                                                                                                                  
  public static final List SEQDESCRIPTIONS = Collections.unmodifiableList(Arrays.asList( new String[] {
                                                                                                        "Coding Sequences",
                                                                                                        "Protein Translations",
                                                                                                        "Cdna Sequences",
                                                                                                        "Transcript Exons (with optional flanking sequences)",
                                                                                                        "Transcripts with Exons and Intronic Sequence (with optional flanking sequences)",
                                                                                                        "Left or Right Flanking Sequence of Transcripts (must specify a left or right flanking length)",
		                                                                                                    "Genes with Exons and Intronic Sequence (with optional flanking sequences)",
                                                                                                        "Gene Exons (with optional flanking sequences)",                                                                                                        
		                                                                                                    "Left or Right Flanking Sequence of Genes (must specify a left or right flanking length)",
		                                                                                                    "Downstream Untranslated Region (must specify a right flank length. Note, some genes do not have UTR categorized)",
		                                                                                                    "Upstream Untranslated Region (must specify a left flank length. Note, some genes do not have UTR categorized)"                                                              
                                                                                                      }
                                                                                        )
                                                                          );                                                                                                                                                   
	
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
	public static final int DOWNSTREAMUTR = 9;
	public static final int UPSTREAMUTR = 10;
}
