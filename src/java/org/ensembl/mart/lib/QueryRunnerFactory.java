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
package org.ensembl.mart.lib;

import java.sql.*;
import java.io.*;

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
    public static QueryRunner getInstance(Query q, FormatSpec f, Connection conn, OutputStream out) throws FormatException {
    	QueryRunner thisQueryRunner = null;
		   switch (q.getType()) {
 
		      case Query.ATTRIBUTE:
              if (f.getFormat() == FormatSpec.FASTA)
                 throw new FormatException("Fasta format can only be applied to Sequence output");            
				      
				      thisQueryRunner = new AttributeQueryRunner(q,f,conn,out);
				      break;
            
          case Query.SEQUENCE:
      		  switch (q.getSequenceDescription().getType()) {
					    case SequenceDescription.TRANSCRIPTCODING:
					      thisQueryRunner = new CodingSeqQueryRunner(q,f,conn,out);
					      break;
					    
					    case SequenceDescription.TRANSCRIPTPEPTIDE:
                thisQueryRunner = new PeptideSeqQueryRunner(q,f,conn,out);
					      break;
					    
					    case SequenceDescription.TRANSCRIPTCDNA:
                thisQueryRunner = new CdnaSeqQueryRunner(q,f,conn,out);
					      break;
					    
					    case SequenceDescription.TRANSCRIPTEXONS:
					      thisQueryRunner = new TranscriptExonSeqQueryRunner(q,f,conn,out);
					      break;
					    
					    case SequenceDescription.TRANSCRIPTEXONINTRON:
					      thisQueryRunner = new TranscriptEISeqQueryRunner(q,f,conn,out);
					      break;
					     
					    case SequenceDescription.TRANSCRIPTFLANKS:
					      thisQueryRunner = new TranscriptFlankSeqQueryRunner(q,f,conn,out);
					      break;
					    
					    case SequenceDescription.GENEEXONINTRON:
					      thisQueryRunner = new GeneEISeqQueryRunner(q,f,conn,out);
					      break;
					    					
					    case SequenceDescription.GENEEXONS:
  					    thisQueryRunner = new GeneExonSeqQueryRunner(q,f,conn,out);
					      break;
					      
					    case SequenceDescription.GENEFLANKS:
					      thisQueryRunner = new GeneFlankSeqQueryRunner(q,f,conn,out);
					      break;
					      
					    case SequenceDescription.DOWNSTREAMUTR:
					      thisQueryRunner = new DownStreamUTRSeqQueryRunner(q,f,conn,out);
					      break;
					      
				      case SequenceDescription.UPSTREAMUTR:
				        thisQueryRunner = new UpStreamUTRSeqQueryRunner(q,f,conn,out);
				        break;
      		  }
      		  break;
		   }
		   return thisQueryRunner;
	}
}
