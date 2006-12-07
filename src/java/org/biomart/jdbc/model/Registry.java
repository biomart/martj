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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biomart.common.resources.Resources;
import org.biomart.jdbc.exceptions.RegistryException;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class Registry {
	private URL registryURL;
	
	private Map marts = new HashMap();
	
	/**
	 * Establish a registry around a given URL.
	 * @param registryURL the URL to find the registry at.
	 * @throws RegistryException if the registry could not be parsed.
	 */
	public Registry(final URL registryURL) throws RegistryException {
		this.registryURL = registryURL;
		this.populate();
	}
	
	/**
	 * Obtain the URL.
	 * @return the URL.
	 */
	public URL getURL() {
		return this.registryURL;
	}
	
	private void populate() throws RegistryException {
		// TODO - read registry, read dataset configs, 
		// construct Mart/Dataset/Attribute/Filter objects,
		// remember stuff.
	}
	
	/**
	 * Find out what marts are in this registry.
	 * @return the list of marts. May be empty but never <tt>null</tt>.
	 */
	public List getMartNames() {
		return new ArrayList(this.marts.keySet());
	}

	/**
	 * Obtain the named mart.
	 * @param martName the name of the mart.
	 * @return the mart.
	 * @throws RegistryException if the mart could not be found.
	 */
	public Mart getMart(String martName) throws RegistryException {
		if (!this.marts.containsKey(martName))
			throw new RegistryException(Resources.get("noSuchMart",martName));
		return (Mart)this.marts.get(martName);
	}
}
