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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * DSFilterHandler implementing object designed to process requests for Genes fitting
 * defined expression profiles defined by expression datasets provided by the Mart.
 * Requests are resolved into an IDListFilter of transcript ids added to the query.
 * The String parameter must match the following format:
 * expression_dataset:List of Term=Value pairs separated by commas
 * examples:
 *   est:anatomical_site=ovary -- returns all genes expressed in the ovary Anatomical Site, based on data inn the eGenetics/SANBI dataset,
 *   gnf:anatomical_site=ovary,development_stage=adult -- returns all genes expressed in the ovary Anatomical Site, in Adult Developmental Stage, based on data in the GNF dataset.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DSExpressionFilterHandler implements DSFilterHandler {

   private String validPattern = "\\w+:(\\w+=\\w+,*)+";
   private String validSQL = "select count(*) from evoc_vocabulary_lookup where term = ?";
   
	 private Logger logger = Logger.getLogger(DSExpressionFilterHandler.class.getName());
	
	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.DSFilterHandler#ModifyQuery(java.sql.Connection, java.lang.String, org.ensembl.mart.explorer.Query)
	 */
	public Query ModifyQuery(Connection conn, String parameter, Query query) throws InvalidQueryException {
		if (! parameter.matches(validPattern))
		  throw new InvalidQueryException("Parameter not valid for DSExpressionFilterHandler request: expected regex "+validPattern+" recieved "+parameter);
		
		Query newQuery = new Query(query);
		
		//must get species and dataset, from the first starBase
		String species = null; 
		String dataset = null;
			 
		//resolve dataset, species, and focus
		String[] mainTables = newQuery.getStarBases();

		for (int i = 0; i < mainTables.length; i++) {
			if (mainTables[i].matches(".*gene"))
				dataset = mainTables[i];
		}
					 
		StringTokenizer tokens = new StringTokenizer(dataset, "_", false);
		species = tokens.nextToken();
		
		if (dataset == null)
		  throw new InvalidQueryException("Could not determine dataset for query, perhaps it is a snp query "+newQuery);
		   		  
		StringTokenizer ptokens = new StringTokenizer(parameter, ":");
		String edataset = ptokens.nextToken();
		String equery = ptokens.nextToken();

		String trans_lib_table = dataset+"_expression_"+edataset+"_map";
		StringBuffer idSQL = new StringBuffer("select transcript_stable_id from "+trans_lib_table); //append libBuf later
		StringBuffer lidBuf = new StringBuffer(" where lib_id in (");
				
		StringTokenizer qtokens = new StringTokenizer(equery, ",");
		int terms = 0;
		String firstTable = null;
		
		//	will build up SQL
		StringBuffer selectBuf = new StringBuffer("select ");
		StringBuffer fromBuf = new StringBuffer(" from ");
		StringBuffer whereBuf = new StringBuffer(" where ");
		
		List values = new ArrayList();
				
		try {
			while ( qtokens.hasMoreTokens() ){
				String tv = qtokens.nextToken();
				StringTokenizer tvtokens = new StringTokenizer(tv, "=");
				
				String term = tvtokens.nextToken("=");
				String value = tvtokens.nextToken("=");
				values.add(value);
				
				if (! IsValidTerm(conn, value))
				  throw new InvalidQueryException("One or more of the following terms does not exist in the Mart Database:"+equery);
				
				String table = species+"_expression_"+edataset+"_"+term+"_support";  
				if (terms < 1) {
          firstTable = table;
 				  selectBuf.append(firstTable+".lib_id"); // will only get the lib_id for the first term, but mapped across all support tables in the from and where clause
				}
				         
        if (terms > 0)
          fromBuf.append(" , ");
				fromBuf.append(table);
        
				if (terms >0) {
				  whereBuf.append(" AND "+firstTable+".lib_id = "+table+".lib_id AND ");
				}  
        whereBuf.append(table+".term = ?");
        
        terms++;
			}
			
			String sql = selectBuf.toString()+fromBuf.toString()+whereBuf.toString();
			
			logger.info("Getting lib_ids with "+sql);
			
			PreparedStatement lps = conn.prepareStatement(sql);
			
			for (int i = 0, n = values.size(); i < n; i++) {
				String element = (String) values.get(i);
				lps.setString(i+1, element);
			}
			ResultSet lrs = lps.executeQuery();
			
			int lidcount = 0;
			while (lrs.next()) {
				if (lidcount > 0)
				  lidBuf.append(", ");
				  
				lidBuf.append("\"").append( lrs.getString(1) ).append("\"");
				lidcount++;
			}
			lps.close();
			lrs.close();
			lidBuf.append(")");
			
			idSQL.append(lidBuf);
			sql = idSQL.toString();
			
			logger.info("Getting ids with "+sql);
			
			List tranIds = new ArrayList();
			 
			PreparedStatement tps = conn.prepareStatement(sql);
			ResultSet trs = tps.executeQuery();
			
			while (trs.next())
			  tranIds.add(trs.getString(1));
			
			tps.close();
			trs.close();
			
			String[] tids = new String[tranIds.size()];
			tranIds.toArray(tids);
			  
			Filter tranidFilter = new IDListFilter("transcript_stable_id", tids);
			
			newQuery.addFilter(tranidFilter); 
			return newQuery;
		} catch (SQLException e) {
			throw new InvalidQueryException("Recieved SQL Exception processing request for Expression Filter "+e.getMessage());
		}
	}

  private boolean IsValidTerm(Connection conn, String term) throws SQLException {
  	boolean valid = true;
  	PreparedStatement ps = conn.prepareStatement(validSQL);
  	ps.setString(1, term);
  	
    ResultSet rs = ps.executeQuery();
    rs.next();
    int count = rs.getInt(1);
    rs.close();
    ps.close();
    
    if (! (count > 0 ))
      valid = false;
      
  	return valid;
  }
}
