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

package org.ensembl.mart.lib.config;

/**
 * DatasetViewAdaptor implimenting object designed to store a single
 * DatasetView object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class SimpleDatasetViewAdaptor implements DatasetViewAdaptor {

	private final DatasetView dsv;
	private final String[] inames;
	private final String[] dnames;

	/**
	 * Constructor for an immutable SimpleDatasetViewAdaptor object.
	 * @param dset -- DatasetView object
	 * @throws ConfigurationException when the DatasetView is null
	 */
	public SimpleDatasetViewAdaptor(DatasetView dset) throws ConfigurationException {
		if (dset == null)
			throw new ConfigurationException("SimpleDatasetView objects must be instantiated with a DatasetView object");
		inames = new String[] { dset.getInternalName()};
		dnames = new String[] { dset.getDisplayName()};
		dsv = dset;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#getDatasetDisplayNames()
	 */
	public String[] getDatasetDisplayNames() throws ConfigurationException {
		return dnames;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#getDatasetInternalNames()
	 */
	public String[] getDatasetInternalNames() throws ConfigurationException {
		return inames;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#getDatasetViews()
	 */
	public DatasetView[] getDatasetViews() throws ConfigurationException {
		return new DatasetView[] { dsv };
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#supportsDisplayName(java.lang.String)
	 */
	public boolean supportsDisplayName(String name) {
		if (dnames[0].equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#getDatasetViewByDisplayName(java.lang.String)
	 */
	public DatasetView getDatasetViewByDisplayName(String name) throws ConfigurationException {
		if (!supportsDisplayName(name))
			throw new ConfigurationException(name + " does not match the displayName of this SimpleDatasetView object\n");
		return dsv;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#supportsInternalName(java.lang.String)
	 */
	public boolean supportsInternalName(String name) {
		if (inames[0].equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#getDatasetViewByInternalName(java.lang.String)
	 */
	public DatasetView getDatasetViewByInternalName(String name) throws ConfigurationException {
		if (!supportsInternalName(name))
			throw new ConfigurationException(name + " does not match the internalName of this SimpleDatasetView object\n");
		return dsv;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DatasetViewAdaptor#update()
	 */
	public void update() throws ConfigurationException {
		//immutable object, cannot be updated.
	}
}
