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

import java.util.List;
import java.util.StringTokenizer;

/**
 * This DSFilterHandler implementing object resolves requests
 * for Genes/Snps located in regions selected by the NCBI
 * ENCODE project into a chromosome BasicFilter, a chromomsomal
 * start coordinate BasicFilter, and a chromosomal end coordinate
 * BasicFilter.
 * The String parameter should match the following format:
 * chromosome:start_coordinate:end_coordinate (eg., 13:29450016:29950015) 
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class EncodeQtlFilterHandler implements UnprocessedFilterHandler {
	private String validPattern = "^.*:.*:.*";
	private String chrname = "chr_name";
	private String chrcoord = "_chrom_start"; // append focus to this to get filter name for coordinate filters

	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.DSFilterHandler#ModifyQuery(java.sql.Connection, java.lang.String, org.ensembl.mart.explorer.Query)
	 */
	public Query ModifyQuery(Engine engine, List filters, Query query) throws InvalidQueryException {
		Query newQuery = new Query(query);
		for (int i = 0, n = filters.size(); i < n; i++) {
			Filter element = (Filter) filters.get(i);
		  newQuery.removeFilter(element);
		  
		  String parameter = element.getValue();
		  if (!parameter.matches(validPattern))
		    throw new InvalidQueryException("Recieved invalid value for Encode/Qtl Filter " + parameter + "\n");
		    
			StringTokenizer paramTokens = new StringTokenizer(parameter, ":");
			String chr = paramTokens.nextToken();

			String bpstart = paramTokens.nextToken();
			String bpend = paramTokens.nextToken();


			// must get focus for coordinate filter names
			String focus = null;
			String joinKey;
			String starBase = newQuery.getStarBases()[0];
			StringTokenizer starBaseTokens = new StringTokenizer(starBase, "__");
			String dset = starBaseTokens.nextToken();
			if (dset.endsWith("snp")){
				focus = "snp";
			    joinKey = "snp_id_key";	
			}
			else{
				joinKey = "gene_id_key";
				focus = "gene";
			}
			Filter startFilter = new BasicFilter(focus + chrcoord, "main", joinKey, ">=", bpstart);
			Filter endFilter = new BasicFilter(focus + chrcoord, "main", joinKey, "<=", bpend);

			Filter chrFilter = new BasicFilter(chrname, "main", joinKey, "=", chr);

			newQuery.addFilter(chrFilter);
			newQuery.addFilter(startFilter);
			newQuery.addFilter(endFilter);	
		}
		
		return newQuery;
	}

}
