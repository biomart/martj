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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Defines standard behavior for SequenceDescription objects
 * used by all mart-explorer objects.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class SequenceDescription {
	
	/**
	 * default constructor.
	 */
	public SequenceDescription() {
	}
	
	/**
	 * Construct a SequenceDescription of a specified type.
	 * type can be one of the static sequence types.
	 * must explicily call setLeftFlank or setRightFlank
	 * to set one or both flank.
	 * 
	 * @param type String
	 */
	public SequenceDescription(String type) throws SequenceException {
		setType(type);
	}
	
	/**
	 * Construct a fully qualified SequenceDescription with all
	 * necessary information.
	 * 
	 * @param type
	 * @param leftflank
	 * @param rightflank
	 */
	public SequenceDescription(String type, int leftflank, int rightflank) throws SequenceException {
		setType(type);
		setLeftFlank(leftflank);
		setRightFlank(rightflank);
	}
	
	/**
	 * Sets the type, and sets all type specific information.
	 * @param type
	 */
	public void setType(String type) throws SequenceException {
		if (type.equals(SequenceDescription.CODINGSEQ)) {
			seqtype = type.toLowerCase();
			seqt = SequenceDescription.CODING;
			description = "coding sequence of transcript";
			canFlank = false; // cant flank coding sequence
			queryIDAttribute = new FieldAttribute("transcript_id");
			// add all of the DisplayIDs
			displayIDAttributes.add(new FieldAttribute("transcript_stable_id_v"));
			displayIDAttributes.add(new FieldAttribute("gene_stable_id_v"));
			descriptionAttributes.add(new FieldAttribute("assembly_type"));	    			    
	    } 
	    else {
		    throw new SequenceException("Supplied sequence type "+type+" is not a known sequence type");
		}
	}

    /**
     * Static type test.  Returns true if the specified type matches a known sequence type.
     * 
     * @param String type
     * @return boolean isValid
     */
    public static boolean isValidType(String type) {
    	boolean isValid = true;
    	if (type.equals(SequenceDescription.CODINGSEQ))
    	  isValid = true;
    	else
    	  isValid = false;
    	return isValid;
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
     *  sets, or resets, the LeftFlank length.
     * 
     * @param int lflank - length of the left flank required
     */
    public void setLeftFlank(int lflank) {
    	if (canFlank) {
    	    this.leftflank = lflank;
    	}
    	else {
    		if (lflank > 0)
			    logger.info("Ignoring flanking sequence length, "+seqtype+" sequences cannot be extended with flanking sequence");
    	}
    }
    
    /**
     * sets, or resets, the LeftFlank length.
     * 
     * @param int rflank
     */
    public void setRightFlank(int rflank) {
    	if (canFlank) {
    	    this.rightflank = rflank;
    	}
    	else {
    		if (rflank > 0)
    		    logger.info("Ignoring flanking sequence length, "+seqtype+" sequences cannot be extended with flanking sequence");
    	}
    }
    
    /**
     * Returns the length of the right flank
     * 
     * @return int rightflank
     */
    public int getRightFlank() {
    	return rightflank;
    }
    
    /**
     * returns an Attribute object containing the ID required
     * for a particular SequenceDescription implimentation.
     * This is the ID necessary for queries against the gene
     * structure table for this type of Sequence.
     * 
     * @return Attribute attribute
     */
    public Attribute getQueryIDAttribute() {
    	return queryIDAttribute;
    }
    
	/**
	 * gets the index of the QueryID in the Attributes
	 * returned from a SQL ResultSet. Used by the QueryRunner
	 * to get the QueryID to send to the SequenceAdaptor.
	 * 
	 * @return int index
	 * @see SequenceAdaptor
	 */
	public int getQueryIDIndex() {
		return queryIDindex;
	}
	
    /**
     * Returns a List of Attribute objects pertaining to
     * the IDs that a particular SequenceDescription should 
     * display along with the sequence in the output. Used 
     * by the Query object to add attributes to its attributes 
     * List.
     * 
     * @return List attributes
     */
    public List getDisplayIDAttributes() {
    	return displayIDAttributes;
    }
        
	/**
	 * Used by the Query Object to set the indices of the DisplayIDAttributes
	 * in its attributes List.
	 * 
	 * @param int[] indices
	 */
	public void setDisplayIDAttributeIndices(int[] indices) {
		displayIDAttributeindices = new int[indices.length];
		for (int i = 0; i < indices.length; i++)
		    displayIDAttributeindices[i] = indices[i];
	}
	
	/**
	 * gets the idices of the DisplayIDAttributes in
	 * the SQL ResultSet.  Used by the QueryRunner to get the
	 * DisplayIDAttributes to add to the output.
	 * 
	 * @return int[] indices
	 */
	public int[] getDisplayIDAttributeIndices() {
		return displayIDAttributeindices;
	}
	
	/**
	 * Returns a List of Attribute objects pertaining
	 * to the required description information for a sequence.
	 * These should be printed last, before the sequence itself.
	 * Used by the Query object to add attributes to its attributes
	 * List.
	 * 
	 * @return List descriptionAttributes
	 */
	public List getDescriptionAttributes() {
		return descriptionAttributes;
	}
	
	
	/**
	 * gets the String description to add to the output.
	 * 
	 * @return String description
	 */
	public String getDescription() {
		return description;
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
		//TODO: append new types as they are added
		buf.append("\ncoding");
		
		return buf.toString();
	}
	
	private String seqtype = null;
	private int seqt = 0; // will hold one of the enums below
	private String description = null;
	private int leftflank = 0;
	private int rightflank = 0;
	private boolean canFlank = true; // default to true, as most can flank
	
	private Attribute queryIDAttribute;
	private List displayIDAttributes = new ArrayList();
	private List descriptionAttributes = new ArrayList();
	
	private final int queryIDindex = 1; // the queryIDAttribute is always first in the list of Attributes
	private int[] displayIDAttributeindices;
	private Logger logger = Logger.getLogger(SequenceDescription.class.getName());
	
	//TODO: add new enums as new types of sequences are implimented
	/**
	 * enums over sequence type that objects can use to test the type, and SequenceDescription can use to set seqtype
	 */
	public final static int CODING = 1;
	private final static String CODINGSEQ = "coding";
}
