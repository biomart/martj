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

package org.ensembl.mart.builder.config;

import java.util.ArrayList;
import java.util.List;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.util.StringUtil;

/**
 * DSConfigAdaptor implimenting object designed to store a single
 * TransformationConfig object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class SimpleDSConfigAdaptor implements DSConfigAdaptor, Comparable {


  private final TransformationConfig dsv;
  private final String[] inames;
  //private final String[] dnames;
  private final int hashcode;
  private String adaptorName = null;

  /**
   * Constructor for an immutable SimpleDSConfigAdaptor object.
   * Really only for development purposes.  If you do use this, make sure you pass in either
   * a fully instantiated TransformationConfig object (all FilterPage and AttributePage objects loaded) or
   * a TransformationConfig object with a different underlying TransformationConfigAdaptor object (this DSConfigAdaptor implementation
   * doesnt insert itself as the adaptor for a given TransformationConfig object, and its lazyLoad() is never called.
   * @param dset -- TransformationConfig object
   * @throws ConfigurationException when the TransformationConfig is null
   */
  public SimpleDSConfigAdaptor(TransformationConfig dset) throws ConfigurationException {
    if (dset == null)
      throw new ConfigurationException("SimpleTransformationConfig objects must be instantiated with a TransformationConfig object");
    inames = new String[] { dset.getInternalName()};
    //dnames = new String[] { dset.getDisplayName()};
    dsv = dset;

    hashcode = dsv.hashCode();
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getTransformationConfigs()
   */
  public TransformationConfigIterator getTransformationConfigs() throws ConfigurationException {
    List l = new ArrayList();
       l.add(dsv);
       return new TransformationConfigIterator(l.iterator());
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#update()
   */
  public void update() throws ConfigurationException {
    //immutable object, cannot be updated.
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[");
    //buf.append(" dataset DisplayName=").append(dsv.getDisplayName());
    buf.append("]");

    return buf.toString();
  }

  /**
   * Allows Equality Comparisons manipulation of SimpleDSConfigAdaptor objects
   */
  public boolean equals(Object o) {
    return o instanceof SimpleDSConfigAdaptor && hashCode() == o.hashCode();
  }

  /**
   * Calculated from the underlying DataSetView hashCode.
   */
  public int hashCode() {
    return hashcode;
  }

  /**
   * allows any DSConfigAdaptor implimenting object to be compared to any other
   * DSConfigAdaptor implimenting object, based on their hashCode.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    return hashCode() - ((DSConfigAdaptor) o).hashCode();
  }

  /**
   * Currently doesnt do anything, as Simple TransformationConfig objects are fully loaded
   * at instantiation.  Could change in the future.
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#lazyLoad(TransformationConfig)
   */
  public void lazyLoad(TransformationConfig dsv) throws ConfigurationException {
    // Doesnt do anything, should be fully instantiated
  }

  /**
   * Throws a ConfigurationException, as this doesnt have a compatible MartLocation element.
   * Client code should create one of the supported Adaptors from the TransformationConfig for this adaptor,
   * and use that one to create the MartRegistry object instead.
   */
  //public MartLocation[] getMartLocations() throws ConfigurationException {
  //  throw new ConfigurationException("Cannot create a MartLocation from a SimpleTransformationConfigAdaptor\n");
  //}

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#supportsDataset(java.lang.String)
   */


  /**
   * @return "Simple" 
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDisplayName()
   */
  public String getDisplayName() {
    return "Simple";
  }




    
  /**
   * SimpleDSConfigAdaptor Objects do not contain child DSConfigAdaptor Objects
   * @return null
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getAdaptorByName(java.lang.String)
   */
  public DSConfigAdaptor getAdaptorByName(String adaptorName) throws ConfigurationException {
      return null;
  }

  /**
   * SimpleDSConfigAdaptor objects do not contain child DSConfigAdaptor Objects
   * @return Empty String[]
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getAdaptorNames()
   */
  public String[] getAdaptorNames() throws ConfigurationException {
    return new String[0];
  }

  /**
   * SimpleDSConfigAdaptor objects do not contain child DSConfigAdaptor Objects
   * @return Empty DSConfigAdaptor[]
   * @see org.ensembl.mart.lib.config.LeafDSConfigAdaptor#getLeafAdaptors()
   */
  public DSConfigAdaptor[] getLeafAdaptors() throws ConfigurationException {
    return new DSConfigAdaptor[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getName()
   */
  public String getName() {
    return adaptorName;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#setName(java.lang.String)
   */
  public void setName(String adaptorName) {
    this.adaptorName = adaptorName;
  }

  /**
   * SimpleDSConfigAdaptor objects do not contain child DSConfigAdaptor Objects
   * @return false
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#supportsAdaptor(java.lang.String)
   */
  public boolean supportsAdaptor(String adaptorName) throws ConfigurationException {
    return false;
  }


  /**
   * This adapytor is not associated with a data source so it returns null.
   * @return null.
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDataSource()
   */
  public DetailedDataSource getDataSource() {
    return null;
  }



  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#containsTransformationConfig(org.ensembl.mart.lib.config.TransformationConfig)
   */
  public boolean containsTransformationConfig(TransformationConfig dsvc) throws ConfigurationException {
    return dsv != null && dsv.equals(dsvc);
  }
  
  /**
   * Do nothing.
   */
  public void clearCache() {
    
  }
}
