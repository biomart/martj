package org.ensembl.mart.lib;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;

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
 * IDListFilterHandler implementing object designed to process
 * URL type IDListFilter objects into STRING type IDListFilter objects.
 * Currently only supports file: URLs with one or more ids, one per line.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class URLIDListFilterHandler extends IDListFilterHandlerIMPL {

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.IDListFilterHandler#ModifyQuery(org.ensembl.mart.lib.Engine, org.ensembl.mart.lib.IDListFilter, org.ensembl.mart.lib.Query)
	 */
	public Query ModifyQuery(Engine engine, IDListFilter idfilter, Query query) throws InvalidQueryException {
		Query newQuery = new Query(query);
		
		URL idURL = idfilter.getUrl();
		String[] unversionedIds = null;
		
		if (idURL.getProtocol().equals("file")) {
		  try {
			  unversionedIds = HarvestStream(engine.getConnection(), query, new InputStreamReader( idURL.openStream() ) );
		  } catch (SQLException e) {
			  throw new InvalidQueryException("Could not parse URL IDListFilter: " + e.getMessage(), e);
      } catch (IOException e) {
        throw new InvalidQueryException( "Problem reading from file", e );
      }
		}
		else 
		//impliment HTML parser here
		throw new InvalidQueryException("Non File URLs are not currently supported\n"); 
		
		if (unversionedIds.length > 0) {
			Filter newFilter = new IDListFilter(idfilter.getName(), idfilter.getTableConstraint(), unversionedIds);
			newQuery.addFilter(newFilter);
		}
		return newQuery;
	}

}
