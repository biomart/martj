package org.ensembl.mart.lib;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

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

/**
 * IDListFilterHandler implementing object designed to process File type
 * IDListFilter objects into STRING type IDListFilter objects. Expects that 
 * files contain one or more ids, one per line.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class FileIDListFilterHandler extends IDListFilterHandlerBase {

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.IDListFilterHandler#ModifyQuery(org.ensembl.mart.lib.Engine, org.ensembl.mart.lib.IDListFilter, org.ensembl.mart.lib.Query)
	 */
	public Query ModifyQuery(Engine engine, IDListFilter idfilter, Query query) throws InvalidQueryException {
		Query newQuery = new Query(query);
		
		File idFile = idfilter.getFile();
		String[] unversionedIds = null;
		
		try {
			unversionedIds = HarvestStream(engine.getConnection(), query, new InputStreamReader(new FileInputStream(idFile)));
		} catch (Exception e) {
      throw new InvalidQueryException("Could not parse File IDListFilter: " + e.getMessage(), e);
		} 
		
		if (unversionedIds.length > 0) {
			Filter newFilter = new IDListFilter(idfilter.getName(), idfilter.getTableConstraint(), unversionedIds);
			newQuery.addFilter(newFilter);
		}
		return newQuery;
	}

}
