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
 * DSViewAdaptor implimenting object designed to store a single
 * DatasetView object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class SimpleDSViewAdaptor implements DSViewAdaptor, Comparable {

	private final DatasetView dsv;
	private final String[] inames;
	private final String[] dnames;
  private final int hashcode;

	/**
	 * Constructor for an immutable SimpleDSViewAdaptor object.
	 * @param dset -- DatasetView object
	 * @throws ConfigurationException when the DatasetView is null
	 */
	public SimpleDSViewAdaptor(DatasetView dset) throws ConfigurationException {
		if (dset == null)
			throw new ConfigurationException("SimpleDatasetView objects must be instantiated with a DatasetView object");
		inames = new String[] { dset.getInternalName()};
		dnames = new String[] { dset.getDisplayName()};
		dsv = dset;
    
    dsv.setDSViewAdaptor(this);
    hashcode = dsv.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetDisplayNames()
	 */
	public String[] getDatasetDisplayNames() throws ConfigurationException {
		return dnames;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetInternalNames()
	 */
	public String[] getDatasetInternalNames() throws ConfigurationException {
		return inames;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViews()
	 */
	public DatasetView[] getDatasetViews() throws ConfigurationException {
		return new DatasetView[] { dsv };
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDisplayName(java.lang.String)
	 */
	public boolean supportsDisplayName(String name) {
		if (dnames[0].equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDisplayName(java.lang.String)
	 */
	public DatasetView getDatasetViewByDisplayName(String name) throws ConfigurationException {
		if (!supportsDisplayName(name))
			throw new ConfigurationException(name + " does not match the displayName of this SimpleDatasetView object\n");
		return dsv;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsInternalName(java.lang.String)
	 */
	public boolean supportsInternalName(String name) {
		if (inames[0].equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByInternalName(java.lang.String)
	 */
	public DatasetView getDatasetViewByInternalName(String name) throws ConfigurationException {
		if (!supportsInternalName(name))
			throw new ConfigurationException(name + " does not match the internalName of this SimpleDatasetView object\n");
		return dsv;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
	 */
	public void update() throws ConfigurationException {
		//immutable object, cannot be updated.
	}
  
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append(" dataset DisplayName=").append(dsv.getDisplayName());
		buf.append("]");

		return buf.toString();
	}
  
  /**
	 * Allows Equality Comparisons manipulation of SimpleDSViewAdaptor objects
	 */
	public boolean equals(Object o) {
		return o instanceof SimpleDSViewAdaptor && hashCode() == o.hashCode();
	}
  
  /**
   * Calculated from the underlying DataSetView hashCode.
   */
	public int hashCode() {
		return hashcode;
	}

  /**
   * allows any DSViewAdaptor implimenting object to be compared to any other
   * DSViewAdaptor implimenting object, based on their hashCode.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    return hashCode() - ( (DSViewAdaptor) o).hashCode();
  }
  
/**
 * Currently doesnt do anything, as Simple DatasetView objects are fully loaded
 * at instantiation.  Could change in the future.
 * @see org.ensembl.mart.lib.config.DSViewAdaptor#lazyLoad()
 */
	public void lazyLoad(DatasetView dsv) throws ConfigurationException {
		// Doesnt do anything, should be fully instantiated
	}

  /**
   * Throws a ConfigurationException, as this doesnt have a compatible MartLocation element.
   * Client code should create one of the supported Adaptors from the DatasetView for this adaptor,
   * and use that one to create the MartRegistry object instead.
   */
	public MartLocation[] getMartLocations() throws ConfigurationException {
		  throw new ConfigurationException("Cannot create a MartLocation from a SimpleDatasetViewAdaptor\n");
	}

}
