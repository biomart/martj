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

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.util.StringUtil;

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
  private String adaptorName = null;

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
  public DatasetView getDatasetViewByDisplayName(String name)
    throws ConfigurationException {
    if (!supportsDisplayName(name))
      throw new ConfigurationException(
        name
          + " does not match the displayName of this SimpleDatasetView object\n");
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
  public DatasetView getDatasetViewByInternalName(String name)
    throws ConfigurationException {
    if (!supportsInternalName(name))
      throw new ConfigurationException(
        name
          + " does not match the internalName of this SimpleDatasetView object\n");
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
    return hashCode() - ((DSViewAdaptor) o).hashCode();
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

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDataset(java.lang.String)
   */
  public boolean supportsDataset(String dataset)
    throws ConfigurationException {
    return dsv.getDataset().equals(dataset);
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDataset(java.lang.String)
   */
  public DatasetView[] getDatasetViewsByDataset(String dataset)
    throws ConfigurationException {

    if (supportsDataset(dataset))
      return new DatasetView[] { dsv };
    else
      return new DatasetView[0];
  }

  /**
   * @return "Simple" 
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDisplayName()
   */
  public String getDisplayName() {
    return "Simple";
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDatasetInternalName(java.lang.String, java.lang.String)
   */
  public DatasetView getDatasetViewByDatasetInternalName(
    String dataset,
    String internalName)
    throws ConfigurationException {
    
    boolean same = StringUtil.compare(dataset, dsv.getDataset()) == 0;
    same = same && StringUtil.compare(internalName, dsv.getInternalName()) == 0;

    if (same)
      return dsv;
    else
      return null;
    }

  /**
   * SimpleDSViewAdaptor Objects do not contain child DSViewAdaptor Objects
   * @returns null
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getAdaptorByName(java.lang.String)
   */
  public DSViewAdaptor getAdaptorByName(String adaptorName) throws ConfigurationException {
      return null;
  }

  /**
   * SimpleDSViewAdaptor objects do not contain child DSViewAdaptor Objects
   * @return Empty String[]
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getAdaptorNames()
   */
  public String[] getAdaptorNames() throws ConfigurationException {
    return new String[0];
  }

  /**
   * SimpleDSViewAdaptor objects do not contain child DSViewAdaptor Objects
   * @return Empty DSViewAdaptor[]
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getAdaptors()
   */
  public DSViewAdaptor[] getAdaptors() throws ConfigurationException {
    return new DSViewAdaptor[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetNames()
   */
  public String[] getDatasetNames() throws ConfigurationException {
    return new String[] { dsv.getDataset() };
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetNames(java.lang.String)
   */
  public String[] getDatasetNames(String adaptorName) throws ConfigurationException {
    if (this.adaptorName.equals(adaptorName))
      return getDatasetNames();
    else
      return new String[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewDisplayNamesByDataset(java.lang.String)
   */
  public String[] getDatasetViewDisplayNamesByDataset(String dataset) throws ConfigurationException {
    if (dsv.getDataset().equals(dataset))
      return new String[] { dsv.getDisplayName() };
    else
      return new String[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewInternalNamesByDataset(java.lang.String)
   */
  public String[] getDatasetViewInternalNamesByDataset(String dataset) throws ConfigurationException {
    if (dsv.getDataset().equals(dataset))
      return new String[] { dsv.getInternalName() };
    else
      return new String[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getName()
   */
  public String getName() {
    return adaptorName;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#setName(java.lang.String)
   */
  public void setName(String adaptorName) {
    this.adaptorName = adaptorName;
  }

  /**
   * SimpleDSViewAdaptor objects do not contain child DSViewAdaptor Objects
   * @return false
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsAdaptor(java.lang.String)
   */
  public boolean supportsAdaptor(String adaptorName) throws ConfigurationException {
    return false;
  }


  /**
   * This adapytor is not associated with a data source so it returns null.
   * @return null.
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDataSource()
   */
  public DetailedDataSource getDataSource() {
    return null;
  }

}
