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

import java.sql.*;
import java.io.*;

import org.ensembl.util.NotImplementedYetException;

/**
 * Factory class for generating QueryRunner implimenting objects 
 * based upon the specified Query and FormatSpec.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class QueryRunnerFactory {

    /**
     *  Creates a QueryRunner implimenting object for a given Query and 
     *  FormatSpec.
     *  Query.ATTRIBUTE + FormatSpec.TABULATED -> TabulatedQueryRunner
     *
     *  @param Query q
     *  @param FormatSpec f
     *  @throws FormatException
     *  @see Query
     *  @see FormatSpec
     */
    public static QueryRunner getInstance(Query q, FormatSpec f, Connection conn, OutputStream out) throws FormatException, NotImplementedYetException {
    	QueryRunner thisQueryRunner = null;
		switch (q.getType()) {
 
		case Query.ATTRIBUTE:
            if (f.getFormat() == FormatSpec.TABULATED) {
				thisQueryRunner = new TabulatedAttributeQueryRunner(q,f,conn,out);
				break;
            }
            else 
               throw new FormatException("Fasta format can only be applied to Sequence output");

        case Query.SEQUENCE:
            if (f.getFormat() == FormatSpec.TABULATED) {
				switch (q.getSequenceDescription().getSeqCode()) {
					case SequenceDescription.TRANSCRIPTCODING:
					    thisQueryRunner = new TabulatedCodingSeqQueryRunner(q,f,conn,out);
					    break;
					    
					case SequenceDescription.TRANSCRIPTPEPTIDE:
                       thisQueryRunner = new TabulatedPeptideSeqQueryRunner(q,f,conn,out);
					   break;
					    
					case SequenceDescription.TRANSCRIPTCDNA:
                        thisQueryRunner = new TabulatedCdnaSeqQueryRunner(q,f,conn,out);
					    break;
					    
					case SequenceDescription.TRANSCRIPTEXONS:
					    thisQueryRunner = new TabulatedTExonSeqQueryRunner(q,f,conn,out);
					    break;
					    
					case SequenceDescription.TRANSCRIPTEXONINTRON:
					     thisQueryRunner = new TabulatedTranscriptEISeqQueryRunner(q,f,conn,out);
					     break;
					    
					case SequenceDescription.GENEEXONINTRON:
					    throw new NotImplementedYetException(q.getSequenceDescription().getType()+" not implimented yet\n");
//					thisQueryRunner = new TabulatedGeneEISeqQueryRunner(q,f,conn,out);
					    //break;
					    					
					case SequenceDescription.GENEEXONS:
					    throw new NotImplementedYetException(q.getSequenceDescription().getType()+" not implimented yet\n");
//					thisQueryRunner = new TabulatedGeneExonSeqQueryRunner(q,f,conn,out);
					    //break;					
				}
				break;
            }
            else {
				switch (q.getSequenceDescription().getSeqCode()) {
					case SequenceDescription.TRANSCRIPTCODING:
					    thisQueryRunner = new FastaCodingSeqQueryRunner(q,f,conn,out);
					    break;
					    
					case SequenceDescription.TRANSCRIPTPEPTIDE:
					    thisQueryRunner = new FastaPeptideSeqQueryRunner(q,f,conn,out);
					    break;
					    
				    case SequenceDescription.TRANSCRIPTCDNA:
					    thisQueryRunner = new FastaCdnaSeqQueryRunner(q,f,conn,out);
					    break;
					    
				    case SequenceDescription.TRANSCRIPTEXONS:
					    thisQueryRunner = new FastaTExonSeqQueryRunner(q,f,conn,out);
					    break;
					    
				    case SequenceDescription.TRANSCRIPTEXONINTRON:
					    thisQueryRunner = new FastaTranscriptEISeqQueryRunner(q,f,conn,out);
					    break;
					    
				    case SequenceDescription.GENEEXONINTRON:
					    throw new NotImplementedYetException(q.getSequenceDescription().getType()+" not implimented yet\n");
					    //thisQueryRunner = new FastaGeneEISeqQueryRunner(q,f,conn,out);
					    //break;
					    					
				    case SequenceDescription.GENEEXONS:
					    throw new NotImplementedYetException(q.getSequenceDescription().getType()+" not implimented yet\n");
					    //thisQueryRunner = new FastaGeneExonSeqQueryRunner(q,f,conn,out);
					    //break;
				}            	
            }   
            break;
		}
		return thisQueryRunner;
	}
}
