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

/**
 * Interface for Objects providing access to one or more DatasetView Objects via
 * implimentation specific methods for accessing and parsing DatasetView.dtd compliant
 * documents from a target source.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public interface DSViewAdaptor {

  /**
   * sets the name of this DSViewAdptor
   * @param adaptorName - name of the adaptor
   */
  public void setName(String adaptorName);
  
  /**
   * Returns the name of this DSViewAdaptor
   * @return String name of DSViewAdaptor
   */
  public String getName();
  
  /**
   * Returns a String[] consisting of the displayNames of all DatasetView objects provided by
   * a particular DSViewAdaptor object. 
   * @return String[] displayNames
   * @throws ConfigurationException for all underlying exceptions
   */
  public String[] getDatasetDisplayNames() throws ConfigurationException;

  /**
   * Returns a String[] consisting of the internalNames of all DatasetView objects provided by
   * a particular DSViewAdaptor object.
   * @return String[] internalNames
   * @throws ConfigurationException
   */
  public String[] getDatasetInternalNames() throws ConfigurationException;

  /**
   * Returns a DatasetView[] consisting of all DatasetView objects provided by a particular
   * DSViewAdaptor object.
   * @return DatasetView[] dsetviews
   * @throws ConfigurationException  for all underlying exceptions
   */
  public DatasetView[] getDatasetViews() throws ConfigurationException;

  /**
   * Determine if a DSViewAdaptor object contains a DatasetView with the given
   * displayName.
   * @param name -- String displayName of requested DatasetView
   * @return true if supported, false otherwise
   * @throws ConfigurationException for all underlying Exceptions
   */
  public boolean supportsDisplayName(String name)
    throws ConfigurationException;

  /**
   * Returns a specific DatasetView object, named by the given displayName 
   * @param name -- String displayName
   * @return DatasetView named by the given displayName
   * @throws ConfigurationException for all underlying Exceptions
   */
  public DatasetView getDatasetViewByDisplayName(String name)
    throws ConfigurationException;

  /**
   * Determine if a DSViewAdaptor object contains a DatasetView with the given
   * internalName.
   * @param name -- String internalName of requested DatasetView.
   * @return true if supported, false otherwise
   * @throws ConfigurationException for all underlying Exceptions
   */
  public boolean supportsInternalName(String name)
    throws ConfigurationException;

  /**
   * Returns a specific DatasetView object, named by the given internalName.
   * @param name -- String internalName
   * @return DatasetView named by the given internalName
   * @throws ConfigurationException for all underlying Exceptions
   */
  public DatasetView getDatasetViewByInternalName(String name)
    throws ConfigurationException;


  /**
   * Returns a specific DatasetView object, named by the given dataset
   * and internalName.
   * @param dataset
   * @param internalName
   * @return DatasetView named by the given dataset
   * and internalName.
   * @throws ConfigurationException for all underlying Exceptions
   */
  public DatasetView getDatasetViewByDatasetInternalName(
    String dataset,
    String internalName)
    throws ConfigurationException;


  /**
   * Determine if the DSViewAdaptor contains a DatasetView with the given
   * dataset name.
   * @param dataset -- dataset name
   * @return true if supported, false otherwise
   * @throws ConfigurationException for all underlying Exceptions
   */
  public boolean supportsDataset(String dataset) throws ConfigurationException;

  /**
   * Returns specific DatasetViews with the given dataset name 
   * @param dataset -- dataset name
   * @return DatasetViews with the given dataset name, empty array if non found
   * @throws ConfigurationException for all underlying Exceptions
   */
  public DatasetView[] getDatasetViewByDataset(String dataset)
    throws ConfigurationException;

  /**
   * Returns a list of the DatasetView internalNames for a particular dataset.
   * @param dataset - name of the dataset for which internalNames are required.
   * @return String[] list of datasetview internalNames associated with the dataset.
   * @throws ConfigurationException
   */
  public String[] getDatasetViewInternalNamesByDataset(String dataset) throws ConfigurationException;
  
  /**
   * Returns a list of the DatasetView displayNames for a particular dataset.
   * @param dataset - name of the dataset for which displayNames are required.
   * @return String[] list of datasetview displayNames associated with the dataset.
   * @throws ConfigurationException
   */
  public String[] getDatasetViewDisplayNamesByDataset(String dataset) throws ConfigurationException;
  
  /**
   * Returns a list of the names of the Datasets for which DatasetViews are held
   * for an Adaptor.
   * @return String[] list of dataset names
   * @throws ConfigurationException
   */
  public String[] getDatasetNames() throws ConfigurationException;
  
  /**
   * Returns a list of the names of Datasets for which DatasetViews are held, given a particular
   * adaptor name.  May return an empty list for adaptors with zero child adaptors.
   * @param adaptorName - name of DSViewAdaptor object for which DatasetNames are required
   * @return String[] list of datatset names
   * @throws ConfigurationException
   */
  public String[] getDatasetNames(String adaptorName) throws ConfigurationException;
  
  /**
   * Returns all DSViewAdaptor objects contained with this Object (which may be a zero length list
   * for some implimentations). Note, this only returns the adaptors contained by this adaptor, and does
   * not return child adaptors of children to this Adaptor.
   * @return Array of DSViewAdaptor objects
   * @throws ConfigurationException
   */
  public DSViewAdaptor[] getAdaptors() throws ConfigurationException;
  
  /**
   * Determine if a DSViewAdaptor supports (contains somewhere in its adaptors, or its adaptors child adaptors)
   * a given DSViewAdaptor named by adaptorName
   * @param adaptorName - name of requested DSViewAdaptor
   * @return boolean true if the requested DSViewAdaptor is supported by this DSViewAdaptor, false otherwise
   * @throws ConfigurationException
   */
  public boolean supportsAdaptor(String adaptorName) throws ConfigurationException;
  
  /**
   * Returns a DSViewAdaptor object named by the given adaptor name
   * @param adaptorName - name of DSViewAdaptor required 
   * @return DSViewAdaptor named by adaptorName
   * @throws ConfigurationException
   */
  public DSViewAdaptor getAdaptorByName(String adaptorName) throws ConfigurationException;
  
  /**
   * Returns the names of all DSViewAdaptor objects contained within this Object (which may be a zero
   * length list for some implimentations). Note, this only returns the names of Adaptors held by this Adaptor,
   * and does not return names of child adaptors of children to this Adaptor.
   * @return String[] names of all adaptors in this Object.
   * @throws ConfigurationException
   */
  public String[] getAdaptorNames() throws ConfigurationException;
  
  /**
    * If a DSViewAdaptor implimenting object caches names and DatasetView objects, this method updates the cache contents
    * based on a comparison with the information stored in the object's target source.  May not actually do anything for some implimentations.
    * 
    * @throws ConfigurationException for all underlying Exceptions
    */
  public void update() throws ConfigurationException;

  /**
   * Method to allow DatasetView objects to be instantiated with a minimum of information, but then be lazy loaded with the rest of their
   * XML data when needed.  This method is intended primarily to be used by the DatasetView object itself, which automatically lazy loads itself
   * using the adaptor that it was instantiated with.
   * @param dsv -- DatasetView Object to be lazy loaded.  Input reference is modified by the method.
   * @throws ConfigurationException for all underlying Exceptions
   */
  public void lazyLoad(DatasetView dsv) throws ConfigurationException;

  /**
   * All implimentations should be able to create MartLocation objects which can be added to 
   * a MartRegistry object when RegistryDSViewAdaptor.getMartRegistry method is called.
   * @return MartLocation[] array
   * @throws ConfigurationException for any underlying Exceptions.
   */
  public MartLocation[] getMartLocations() throws ConfigurationException;

  /**
   * All implementations should provide a display name.
   * @return display name for this adaptor.
   */
  public String getDisplayName();

  /**
   * All implementations should either return a datasource if available,
   * otherwise null.
   * @return datasource if available, otherwise null.
   */
  public DetailedDataSource getDataSource();


}
