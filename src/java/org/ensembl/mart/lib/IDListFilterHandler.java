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

/**
 * Interface for Handlers for IDListFilter objects.  This system allows
 * IDListFilters of different types to be added to a query without actually
 * processing them into proper String[] filters.  The IDListFilterHandler
 * for a particular type of IDListFilter will then process the object underlying
 * it (URL, Query, etc.) and create a new IDListFilter object consisting only
 * of String[] as its underlying object.  This IDListFilter will then be added to the
 * Query as a proper Filter.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public interface IDListFilterHandler {
	
	/**
	 * Method to modify the Query by resolving a given IDListFilter object into
	 * a String[] based IDListFilter.  It will have an engine to execute Mart Query
	 * Objects, if necessary.  The Method should use the Query copy constructor to make
	 * a new Copy of the Query, modify that, and return the new Query.
	 * The method also handles versioned ids.  If the versioned_ids column in the _meta_release_info entry
	 * for the given species is true, it will strip off any versions that it encounters.
	 * 
	 * @param engine - Engine object
	 * @param idfilter - IDListFilter object of a special Type
	 * @param query - Query object to be modified.
	 * @return Query Object with String[] based IDListFilter Filter
	 * @throws InvalidQueryException -- chains all underlying Exceptions as InvalidQueryExceptions
	 */
	public Query ModifyQuery(Engine engine, IDListFilter idfilter, Query query) throws InvalidQueryException;

}
