package org.ensembl.mart.explorer;

public class QueryRunnerFactory {

    /* creates the right QueryRunner object based upon query and formatspec
     *     Query.ATTRIBUTE + FormatSpec.TABULATED -> TabulatedQueryRunner
     *  TODO, implement SEQUENCE switching
     */
    public QueryRunner createQueryRunner(Query q, FormatSpec f) throws FormatException {
		/*		switch (q.getType) {
 
		case q.ATTRIBUTE:
            if (f.getFormat == FormatSpec.TABULATED)
				return new TabulatedQueryRunner(q,f);
            else 
               throw new FormatException("Cannot create Fasta output on attributes");
            break;

        case q.SEQUENCE:
            if (f.getFormat == FormatSpec.TABULATED)
				return new TabulatedSeqQueryRunner(q,f);
            else 
               return new FastaSeqQueryRunner(q,f);
            break;
			}*/
        return new TabulatedQueryRunner(q,f);
	}
}
