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

import java.sql.Connection;
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
public class DSEncodeQtlFilterHandler implements DSFilterHandler {
  private String validPattern = "^.*:.*:.*";
	private String chrname = "chr_name";
	private String chrcoord = "_chrom_start";  // append focus to this to get filter name for coordinate filters
	   
	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.DSFilterHandler#ModifyQuery(java.sql.Connection, java.lang.String, org.ensembl.mart.explorer.Query)
	 */
	public Query ModifyQuery(Connection conn, String parameter, Query query) throws InvalidQueryException {
		if (! parameter.matches( validPattern ) )
					throw new InvalidQueryException("Supplied parameter does not match the required format for DSEncodeQtlFilterHandler: recieved "+parameter+" expected String matching Regex "+validPattern);
					
		StringTokenizer paramTokens = new StringTokenizer(parameter, ":");
		String chr = paramTokens.nextToken();
		Filter chrFilter = new BasicFilter(chrname, "=", chr);
		
		String bpstart = paramTokens.nextToken();
		String bpend = paramTokens.nextToken();
									
		Query newQuery = new Query(query);
		newQuery.addFilter(chrFilter);
		
		// must get focus for coordinate filter names
		String focus = null;
		String starBase = newQuery.getStarBases()[0];
		if (starBase.endsWith("snp"))
			focus = "snp";
		else
			focus = "gene";
		
		Filter startFilter = new BasicFilter(focus+chrcoord, ">=", bpstart);
		Filter endFilter = new BasicFilter(focus+chrcoord, "<=", bpend);
		
		newQuery.addFilter(startFilter);
		newQuery.addFilter(endFilter);
		return newQuery;
	}

}
