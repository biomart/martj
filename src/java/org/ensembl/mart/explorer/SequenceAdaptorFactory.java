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

import java.sql.*;

/**
 * Factory class to generate SequenceAdaptor objects for specific
 * sequence types.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * 
 * @see SequenceAdaptor
 */
public class SequenceAdaptorFactory {
	
	/**
	 * Factory method.  Returns the correct SequenceAdaptor implimenting object for the given seqtype.
	 * 
	 * @param int seqtype - one of the SequenceDescription enums
	 * @param String species - the species of this query
	 * @param String focus - the focus of the query
	 *  
	 * @return SequenceAdaptor
	 * @see SequenceAdaptor
	 * @see SequenceDescription
	 */
	public static SequenceAdaptor createSequenceAdaptor(int seqcode, String species, String focus, Connection conn) throws SequenceException {
		SequenceAdaptor thisSeqAdaptor = null;
		//TODO: impliment new cases as new sequence types are added to the SequenceDescription
		switch (seqcode) {
			case SequenceDescription.CODING:
			    thisSeqAdaptor = new CodingSequenceAdaptor(species, focus, conn);
			    break;
			    
		    default:
			    throw new SequenceException("Supplied sequence type is not a known sequence type");		     
		}
		return thisSeqAdaptor;
	}
}
