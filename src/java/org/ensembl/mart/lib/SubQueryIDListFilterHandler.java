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

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * IDListFilterHandler implementing object designed to process SUBQUERY
 * type IDListFilter objects into STRING type IDListFilter objects.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class SubQueryIDListFilterHandler extends IDListFilterHandlerIMPL {

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.IDListFilterHandler#ModifyQuery(org.ensembl.mart.lib.Engine, org.ensembl.mart.lib.IDListFilter, org.ensembl.mart.lib.Query)
	 */
	public Query ModifyQuery(Engine engine, IDListFilter idfilter, Query query) throws InvalidQueryException {
		Query newQuery = new Query(query);
		
		Query subq = idfilter.getSubQuery();

    ByteArrayOutputStream idstream = new ByteArrayOutputStream();
    String results = null;
    
    try {
    	engine.execute(subq, FormatSpec.TABSEPARATEDFORMAT, idstream);

      results = idstream.toString();
      idstream.close();
		} catch (Exception e) {
			throw new InvalidQueryException("Could not execute subquery: "+ e.getMessage());
		}
    
		StringTokenizer lines = new StringTokenizer(results, "\n");
		List idlist = new ArrayList();

		while (lines.hasMoreTokens()) {
		  String id = lines.nextToken();
			if (! idlist.contains(id))
		    idlist.add(id);
		}

		String[] ids = new String[idlist.size()];
		idlist.toArray(ids);
		
		String[] unversionedIds = null;
    
    try {
      unversionedIds = ModifyVersionedIDs(engine.getConnection(), query, ids);
    } catch (SQLException e) {
      throw new InvalidQueryException( "Problem with db connection: ",e );
    }
		
		if (unversionedIds.length > 0) {
		  Filter newFilter = new IDListFilter(idfilter.getName(), idfilter.getTableConstraint(), unversionedIds);
		  newQuery.addFilter(newFilter);
		}
				
		return newQuery;
	}

}
