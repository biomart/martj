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
import java.util.List;

/**
 * UnprocessedFilterHandler implimenting object that checks ids in a String[] IDListFilter Handler for
 * versions, and, if applicable to the dataset, strips the versions off, creating a new String[]
 * IDListFilterHandler with the ids, sans versions. 
* @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
* @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
*/
public class StringIDListFilterHandler extends IDListFilterHandlerBase {

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.UnprocessedFilterHandler#ModifyQuery(org.ensembl.mart.lib.Engine, java.util.List, org.ensembl.mart.lib.Query)
	 */
	public Query ModifyQuery(Engine engine, List filters, Query query) throws InvalidQueryException {
    Query newQuery = new Query(query);
    
    for (int i = 0, n = filters.size(); i < n; i++) {
			IDListFilter idfilter = (IDListFilter) filters.get(i);
			newQuery.removeFilter(idfilter);
			
      Connection conn = null;
			try {
        conn = query.getDataSource().getConnection();
				newQuery.addFilter(new IDListFilter(idfilter.getField(), idfilter.getTableConstraint(), idfilter.getKey(),ModifyVersionedIDs( conn, newQuery, idfilter.getIdentifiers())));
			} catch (Exception e) {
				throw new InvalidQueryException("Could not process Versions from IDListFilter " + e.getMessage(), e);
			} finally {
        DetailedDataSource.close( conn );			
			}
		}
        
    return newQuery;
	}
}
