/*
 * Created on Apr 15, 2003
 *
 */
package org.ensembl.mart.explorer;

import java.sql.*;

/**
 * Sets up standard behavior for all SequenceAdaptor objects.  Sequence Adaptors
 * contain the logic for creating Sequences of a particular type.  They
 * make use of the DNAADaptor to fetch particular sequence Strings,
 * and build up a String sequence.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @see DNAAdaptor
 */
public interface SequenceAdaptor {
	/**
	 * returns a String sequence that can be printed out for a given QueryID.
	 * Uses a DNAAdaptor to fetch sequences
	 * 
	 * @param int QueryID - id used to query the gene structure table for the sequence coordinates
	 * @param String displayID - the list of id's displayed in the output
	 * @param String description - a string of descriptive information, which may be modified
	 * @param String separator - if the description is modified for the final output it should use this record separator
	 * @param int lflank - left flank length modifier
	 * @param int rflank - right flank length modifier
	 * 
	 * @return String printable sequence
	 */
	public Sequence[] getSequences(int QueryID, String displayID, String description, String separator, int lflank, int rflank) throws SQLException, SequenceException;
}
