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

/**
 * Class for holding the location of a portion of sequence in the dna chunks
 * table.  A Sequence Location is specified by chromosome, start, end, and strand.
 * Start and end can be modified by extendRightFlank and extendLeftFlank.
 * Start and end can be updated to new values.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class SequenceLocation {

     private String chr = null;
     private int start = 0;
     private int end = 0;
     private int strand = 0; // -1 is revearse, 1 is forward

	public SequenceLocation(String chr, int start, int end, int strand) {
     	this.chr = chr;
     	this.start = start;
     	this.end = end;
     	this.strand = strand;
     }


	public void updateStart(int start) {
		this.start = start;
	}
	
	/**
	 * Returns the start position.
	 * 
	 * @return int start
	 */
	public int getStart() {
		return start;
	}
    
	public void updateEnd(int end) {
	    this.end = end;
	}
	    
	/**
	 * Returns the end position.
	 * 
	 * @return int end
	 */
	public int getEnd() {
		return end;
	}
    
	/**
	 * Returns the strand
	 * 
	 * @return int strand
	 */
	public int getStrand() {
		return strand;
	}
    
	/**
	 * Returns the Chromosome name.
	 * 
	 * @return int chromosome
	 */
	public String getChr() {
		return chr;
	}
    
	/**
	 * Extends the RightFlank coordinate by length, according 
	 * to the strand.
	 * 
	 * @param int length
	 */
	public void extendRightFlank(int length) {
		if (strand == -1) {
			start = start - length;
			if (start < 1)
				start = 1; // sometimes requested flank length exceeds available sequence.
		}
		else
			end = end + length;
	}
    
	/**
	 * Extends the LeftFlank coordinate by length, according
	 * to the strand.
	 * 
	 * @param int length
	 */
	public void extendLeftFlank(int length) {
		if (strand == -1)
			end = end + length;
		else {
			start = start - length;
			if (start < 1)
				start = 1;
		}
	}     
}
