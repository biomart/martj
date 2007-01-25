/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.jdbc.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.biomart.jdbc.exceptions.RegistryException;
import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class DataSet {	
	private String datasetName;
	
	private Map attributes = new HashMap();
	private Map filters = new HashMap();
	
	/**
	 * Construct a dataset with the given name.
	 * @param datasetName the name of this dataset.
	 */
	public DataSet(final String datasetName) {
		this.datasetName = datasetName;
	}
	
	/**
	 * Get the name.
	 * @return the name.
	 */
	public String getName() {
		return this.datasetName;
	}
		
	/**
	 * Find out what attributes are in this dataset.
	 * @return the list of attributes. May be empty but never <tt>null</tt>.
	 */
	public Collection getAttributeNames() {
		return this.attributes.keySet();
	}

	/**
	 * Obtain the named attribute.
	 * @param attributeName the name of the attribute.
	 * @return the attribute.
	 * @throws RegistryException if the attribute could not be found.
	 */
	public Attribute getAttribute(String attributeName) throws RegistryException {
		if (!this.attributes.containsKey(attributeName))
			throw new RegistryException(Resources.get("noSuchAttribute",attributeName));
		return (Attribute)this.attributes.get(attributeName);
	}
		
	/**
	 * Find out what filters are in this dataset.
	 * @return the list of filters. May be empty but never <tt>null</tt>.
	 */
	public Collection getFilterNames() {
		return this.filters.keySet();
	}

	/**
	 * Obtain the named filter.
	 * @param filterName the name of the filter.
	 * @return the filter.
	 * @throws RegistryException if the filter could not be found.
	 */
	public Filter getFilter(String filterName) throws RegistryException {
		if (!this.filters.containsKey(filterName))
			throw new RegistryException(Resources.get("noSuchFilter",filterName));
		return (Filter)this.filters.get(filterName);
	}
}
