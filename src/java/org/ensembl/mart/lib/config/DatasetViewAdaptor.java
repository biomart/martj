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
 * Interface for Objects providing access to one or more DatasetView Objects via
 * implimentation specific methods for accessing and parsing DatasetView.dtd compliant
 * documents from a target source.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public interface DatasetViewAdaptor {
  
  /**
   * Returns a String[] consisting of the displayNames of all DatasetView objects provided by
   * a particular DatasetViewAdaptor object. 
   * @return String[] displayNames
   * @throws ConfigurationException for all underlying exceptions
   */
  public String[] getDatasetDisplayNames() throws ConfigurationException;
  
  /**
   * Returns a String[] consisting of the internalNames of all DatasetView objects provided by
   * a particular DatasetViewAdaptor object.
   * @return String[] internalNames
   * @throws ConfigurationException
   */
  public String[] getDatasetInternalNames() throws ConfigurationException;
  /**
   * Returns a DatasetView[] consisting of all DatasetView objects provided by a particular
   * DatasetViewAdaptor object.
   * @return DatasetView[] dsetviews
   * @throws ConfigurationException  for all underlying exceptions
   */
  public DatasetView[] getDatasetViews() throws ConfigurationException;
  
  /**
   * Returns a specific DatasetView object, named by the given displayName 
   * @param name -- String displayName
   * @return DatasetView named by the given displayName
   * @throws ConfigurationException for all underlying Exceptions
   */
  public DatasetView getDatasetViewByDisplayName(String name) throws ConfigurationException;
  
  /**
   * Returns a specific DatasetView object, named by the given internalName.
   * @param name -- String internalName
   * @return DatasetView named by the given internalName
   * @throws ConfigurationException for all underlying Exceptions
   */
  public DatasetView getDatasetViewByInternalName(String name) throws ConfigurationException;
  
  /**
   * If a DatasetViewAdaptor implimenting object caches names and DatasetView objects, this method updates the cache contents
   * based on a comparison with the information stored in the object's target source.  May not actually do anything for some implimentations.
   * 
   * @throws ConfigurationException for all underlying Exceptions
   */
  public void update() throws ConfigurationException;

}
