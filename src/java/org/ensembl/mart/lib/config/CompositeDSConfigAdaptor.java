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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.util.StringUtil;

/**
 * A composite DSConfigAdaptor that combines the datasets from all contained 
 * DSConfigAdaptors.
 */
public class CompositeDSConfigAdaptor implements MultiDSConfigAdaptor, Comparable {

  //instanceCount for default adaptorName
  private static int count = 0;

  private final String DEFAULT_ADAPTOR_NAME = "Composite";

  protected Set adaptors = new HashSet();
  protected Set adaptorNameMap = new HashSet();
  protected String adaptorName = null;

  /**
   * Creates instance of CompositeDSConfigAdaptor.
   */
  public CompositeDSConfigAdaptor() {
    adaptorName = DEFAULT_ADAPTOR_NAME + count++;
  }

  /**
   * Adds adaptor.  
   * @param adaptor adaptor to be added. Do not add an ancestor CompositeDSConfigAdaptor
   * to this instance or you will cause circular references when the getXXX() methods are called.
   */
  public void add(DSConfigAdaptor adaptor) {
    if (adaptor.getName() != null)
      adaptorNameMap.add(adaptor.getName());
    adaptors.add(adaptor);
  }

  /**
   * Remove adaptor if present.
   * @param adaptor adaptor to be removed
   * @return true if adaptor was removed, otherwise false.
   */
  public boolean remove(DSConfigAdaptor adaptor) {
    if (adaptorNameMap.contains(adaptor.getName()))
      adaptorNameMap.remove(adaptor.getName());
    return adaptors.remove(adaptor);
  }

  /**
   * Removes all adaptors.
   */
  public void clear() {
    adaptorNameMap.clear();
    adaptors.clear();
  }

  /**
   * Gets currently available adaptors.
   * @return all adaptors currently managed by this instance. Empty array 
   * if non available.
   */
  public DSConfigAdaptor[] getAdaptors() {
    return (DSConfigAdaptor[]) adaptors.toArray(new DSConfigAdaptor[adaptors.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigs()
   */
  public DatasetConfig[] getDatasetConfigs() throws ConfigurationException {
    List configs = new ArrayList();
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSConfigAdaptor adaptor = (DSConfigAdaptor) iter.next();
      configs.addAll(Arrays.asList(adaptor.getDatasetConfigs()));
    }
    return (DatasetConfig[]) configs.toArray(new DatasetConfig[configs.size()]);
  }

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#supportsDisplayName(java.lang.String)
   */
  public boolean supportsDisplayName(String name) throws ConfigurationException {
    return null != getDatasetConfigByDisplayName(name);
  }

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigByDisplayName(java.lang.String)
   */
  public DatasetConfig getDatasetConfigByDisplayName(String name) throws ConfigurationException {

    DatasetConfig result = null;
    DatasetConfig[] configs = getDatasetConfigs();
    for (int i = 0, n = configs.length; i < n; i++) {
      DatasetConfig config = configs[i];
      if (config.getDisplayName().equals(name)) {
        result = config;
        break;
      }
    }
    return result;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#supportsInternalName(java.lang.String)
   */
  public boolean supportsInternalName(String name) throws ConfigurationException {
    return getDatasetConfigByInternalName(name) != null;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigByInternalName(java.lang.String)
   */
  public DatasetConfig getDatasetConfigByInternalName(String name) throws ConfigurationException {

    DatasetConfig result = null;

    DatasetConfig[] configs = getDatasetConfigs();
    for (int i = 0, n = configs.length; i < n; i++) {
      DatasetConfig config = configs[i];
      if (config.getInternalName().equals(name)) {
        result = config;
        break;
      }
    }
    return result;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#update()
   */
  public void update() throws ConfigurationException {
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSConfigAdaptor adaptor = (DSConfigAdaptor) iter.next();
      adaptor.update();
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#lazyLoad(org.ensembl.mart.lib.config.DatasetConfig)
   */
  public void lazyLoad(DatasetConfig dsv) throws ConfigurationException {
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSConfigAdaptor adaptor = (DSConfigAdaptor) iter.next();
      if (adaptor.supportsDataset(dsv.getDataset())) {
        adaptor.lazyLoad(dsv);
        break;
      }
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.MultiDSConfigAdaptor#removeDatasetConfig(org.ensembl.mart.lib.config.DatasetConfig)
   */
  public boolean removeDatasetConfig(DatasetConfig dsv) throws ConfigurationException {
    boolean removed = false;

    for (Iterator iter = adaptors.iterator(); !removed && iter.hasNext();) {
      DSConfigAdaptor adaptor = (DSConfigAdaptor) iter.next();

      if (adaptor.supportsDataset(dsv.getDataset())) {
        if (adaptor instanceof MultiDSConfigAdaptor) {
          removed =
            ((MultiDSConfigAdaptor) adaptor).removeDatasetConfig(
              adaptor.getDatasetConfigByDatasetInternalName(dsv.getDataset(), dsv.getInternalName()));
        } else {
          DatasetConfig thisDSV = adaptor.getDatasetConfigByDatasetInternalName(dsv.getDataset(), dsv.getInternalName());
          if (thisDSV.equals(dsv))
            removed = adaptors.remove(adaptor);
        }
      }
    }

    return removed;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getMartLocations()
   */
  public MartLocation[] getMartLocations() throws ConfigurationException {
    List locations = new ArrayList();

    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSConfigAdaptor adaptor = (DSConfigAdaptor) iter.next();

      locations.addAll(Arrays.asList(adaptor.getMartLocations()));
    }

    MartLocation[] retlocs = new MartLocation[locations.size()];
    locations.toArray(retlocs);
    return retlocs;
  }

  /**
   * Allows Equality Comparisons manipulation of DSConfigAdaptor objects.  Although
   * any DSConfigAdaptor object can be compared with any other DSConfigAdaptor object, to provide
   * consistency with the compareTo method, in practice, it is almost impossible for different DSVIewAdaptor
   * implimentations to equal.
   */
  public boolean equals(Object o) {
    return o instanceof DSConfigAdaptor && hashCode() == o.hashCode();
  }

  /**
   * Calculated from all included adaptor hashCodes.
   */
  public int hashCode() {
    int hsh = (adaptorName != null) ? adaptorName.hashCode() : 0;

    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSConfigAdaptor adaptor = (DSConfigAdaptor) iter.next();
      int h = adaptor.hashCode();
      hsh += h;
    }

    return hsh;
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
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#supportsDataset(java.lang.String)
   */
  public boolean supportsDataset(String dataset) throws ConfigurationException {
    return getDatasetConfigsByDataset(dataset).length > 0;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigByDataset(java.lang.String)
   */
  public DatasetConfig[] getDatasetConfigsByDataset(String dataset) throws ConfigurationException {

    ArrayList l = new ArrayList();
    DatasetConfig[] configs = getDatasetConfigs();
    for (int i = 0, n = configs.length; i < n; i++) {
      DatasetConfig config = configs[i];
      if (config.getDataset().equals(dataset)) {
        l.add(config);
      }
    }

    return (DatasetConfig[]) l.toArray(new DatasetConfig[l.size()]);

  }

  /**
   * @return "Composite"
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDisplayName()
   */
  public String getDisplayName() {
    return "Composite";
  }

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigByDatasetInternalName(java.lang.String, java.lang.String)
   */
  public DatasetConfig getDatasetConfigByDatasetInternalName(String dataset, String internalName)
    throws ConfigurationException {

    DatasetConfig config = null;
    DatasetConfig[] dsconfigs = getDatasetConfigs();
    for (int i = 0; i < dsconfigs.length; ++i) {

      DatasetConfig dsv = dsconfigs[i];
      if (StringUtil.compare(dataset, dsv.getDataset()) == 0
        && StringUtil.compare(internalName, dsv.getInternalName()) == 0) {
        config = dsv;
        break;
      }

    }

    return config;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigByDatasetDisplayName(java.lang.String, java.lang.String)
   */
  public DatasetConfig getDatasetConfigByDatasetDisplayName(String dataset, String displayName)
    throws ConfigurationException {
      DatasetConfig config = null;
      DatasetConfig[] dsconfigs = getDatasetConfigs();
      for (int i = 0; i < dsconfigs.length; ++i) {

        DatasetConfig dsv = dsconfigs[i];
        if (StringUtil.compare(dataset, dsv.getDataset()) == 0
          && StringUtil.compare(displayName, dsv.getDisplayName()) == 0) {
          config = dsv;
          break;
        }

      }

      return config;
  }
  
  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetNames()
   */
  public String[] getDatasetNames() throws ConfigurationException {
    List l = new ArrayList();
    DatasetConfig[] configs = getDatasetConfigs();
    for (int i = 0, n = configs.length; i < n; i++) {
      l.add(configs[i].getDataset());
    }

    return (String[]) l.toArray(new String[l.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getAdaptorByName(java.lang.String)
   */
  public DSConfigAdaptor getAdaptorByName(String adaptorName) throws ConfigurationException {
    DSConfigAdaptor dsva = null;

    if (adaptorNameMap.contains(adaptorName)) {
      for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
        DSConfigAdaptor element = (DSConfigAdaptor) iter.next();
        if (element.getName().equals(adaptorName)) {
          dsva = element;
          break;
        } else if (element.supportsAdaptor(adaptorName))
          dsva = element.getAdaptorByName(adaptorName);
      }

    }

    return dsva;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getAdaptorNames()
   */
  public String[] getAdaptorNames() throws ConfigurationException {
    return (String[]) adaptorNameMap.toArray(new String[adaptorNameMap.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetNames(java.lang.String)
   */
  public String[] getDatasetNames(String adaptorName) throws ConfigurationException {
    List l = new ArrayList();

    if (adaptorNameMap.contains(adaptorName)) {
      for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
        DSConfigAdaptor element = (DSConfigAdaptor) iter.next();

        if (element.getName().equals(adaptorName)) {
          l.addAll(Arrays.asList(element.getDatasetNames()));
          break;
        } else if (element.supportsAdaptor(adaptorName)) {
          l.addAll(Arrays.asList(element.getDatasetNames(adaptorName)));
          break;
        }
      }
    }

    return (String[]) l.toArray(new String[l.size()]);
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

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigDisplayNamesByDataset(java.lang.String)
   */
  public String[] getDatasetConfigDisplayNamesByDataset(String dataset) throws ConfigurationException {
    List l = new ArrayList();
    DatasetConfig[] configs = getDatasetConfigsByDataset(dataset);
    for (int i = 0, n = configs.length; i < n; i++) {
      l.add(configs[i].getDisplayName());
    }

    return (String[]) l.toArray(new String[l.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigInternalNamesByDataset(java.lang.String)
   */
  public String[] getDatasetConfigInternalNamesByDataset(String dataset) throws ConfigurationException {
    List l = new ArrayList();
    DatasetConfig[] configs = getDatasetConfigsByDataset(dataset);
    for (int i = 0, n = configs.length; i < n; i++) {
      l.add(configs[i].getInternalName());
    }

    return (String[]) l.toArray(new String[l.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#supportsAdaptor(java.lang.String)
   */
  public boolean supportsAdaptor(String adaptorName) throws ConfigurationException {
    boolean supports = adaptorNameMap.contains(adaptorName);

    if (!supports) {
      for (Iterator iter = adaptors.iterator(); !supports && iter.hasNext();) {
        DSConfigAdaptor element = (DSConfigAdaptor) iter.next();

        supports = element.supportsAdaptor(adaptorName);
      }
    }

    return supports;
  }

  /**
   * This adapytor is not associated with a data source so it returns null.
   * @return null.
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDataSource()
   */
  public DetailedDataSource getDataSource() {
    return null;
  }
}
