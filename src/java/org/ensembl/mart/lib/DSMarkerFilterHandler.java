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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

/**
 * This DSFilterHandler implementing object resolves Requests for
 * Genes/Snps located between known chromosomal markers into
 * a chromosomal start coordinate BasicFilter, or a chromosomal 
 * end coordinate BasicFilter.
 * The String paremeter should match one of the following formats:
 *  - markerid:start -- adds chr_start coordinates for the marker identified by markerid
 *  - markerid:end -- adds chr_end coordinates for the marker identified by markerid
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DSMarkerFilterHandler implements DSFilterHandler {

  private String startPattern = "^.*\\:start$";
  private String endPattern = "^.*\\:end$";
  private String posstart = "start";
  private String posend = "end";
  private String chrname = "chr_name";
  
	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.DSFilterHandler#ModifyQuery(java.sql.Connection, java.lang.String, org.ensembl.mart.explorer.Query)
	 */
	public Query ModifyQuery(Connection conn, String parameter, Query query) throws InvalidQueryException {
		if (! ( ( parameter.matches( startPattern ) ) || ( parameter.matches( endPattern ) ) ) )
		  throw new InvalidQueryException("Supplied parameter does not match the required format for DSMarkerFilterHandler: recieved "+parameter+" expected String matching Regex "+startPattern+" or Regex "+endPattern);
		  
		String sql, filterName, filterCondition, filterValue;
		PreparedStatement ps;
		Filter chrFilter;
		
		StringTokenizer paramtokens = new StringTokenizer(parameter, ":");
		String markerID = paramtokens.nextToken();
		String pos = paramtokens.nextToken();
		
		Query newQuery = new Query(query);
		
		// must get species, focus, and chromosome.  If a chromosome filter has not been set, then throw an exception
		String species, focus, chrvalue, lookupTable;
		
		// species can be parsed from the beginning of the first starBase
		String starBase = newQuery.getStarBases()[0];
		StringTokenizer sbtokens = new StringTokenizer(starBase, "_");
		species = sbtokens.nextToken();
		String tmp = sbtokens.nextToken();
		
		//focus is snp if tmp ends with snp
		if (tmp.endsWith("snp"))
		  focus = "snp";
		else
		  focus = "gene";
		filterName = focus+"_chrom_start";
		
		if (species == null || species.equals(""))
		  throw new InvalidQueryException("Species is required for a Marker Filter, check the MartConfiguration for the correct starBases for this Dataset.");
		lookupTable = species+"_marker_lookup";
		  
		chrFilter = newQuery.getFilterByName(chrname);
		if (chrFilter == null)
		  throw new InvalidQueryException("Marker Filters require a Chromosome Filter to have already been added to the Query.");
		  
		chrvalue = chrFilter.getValue();
		
		if (pos.equals(posstart)) {
			sql = "SELECT chr_start FROM "+lookupTable+" WHERE  chr_name = ? AND id = ?";
			filterCondition = ">=";
		} 
		else {
		  sql = "SELECT chr_end FROM "+lookupTable+" WHERE  chr_name = ? AND id = ?";
		  filterCondition = "<=";
		}
		
		try {
			ps = conn.prepareStatement(sql);
			ps.setString(1, chrvalue);
			ps.setString(2, markerID);
			
			ResultSet rs = ps.executeQuery();
			rs.next(); // will only be one result
			filterValue = rs.getString(1);
		} catch (SQLException e) {
			throw new InvalidQueryException("Recieved SQLException "+e.getMessage());
		}
		
		Filter posFilter = new BasicFilter(filterName, filterCondition, filterValue);
		newQuery.addFilter(posFilter);
		return newQuery;
	}

}
