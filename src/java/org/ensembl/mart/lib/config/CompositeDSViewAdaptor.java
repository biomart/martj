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
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ensembl.util.StringUtil;

/**
 * A composite DSViewAdaptor that combines the datasets from all contained 
 * DSViewAdaptors.
 */
public class CompositeDSViewAdaptor implements MultiDSViewAdaptor, Comparable {

  protected SortedSet adaptors = new TreeSet();

  /**
   * Creates instance of CompositeDSViewAdaptor.
   */
  public CompositeDSViewAdaptor() {
  }

  /**
   * Adds adaptor.  
   * @param adaptor adaptor to be added. Do not add an ancestor CompositeDSViewAdaptor
   * to this instance or you will cause circular references when the getXXX() methods are called.
   */
  public void add(DSViewAdaptor adaptor) {
    adaptors.add(adaptor);
  }

  /**
   * Remove adaptor if present.
   * @param adaptor adaptor to be removed
   * @return true if adaptor was removed, otherwise false.
   */
  public boolean remove(DSViewAdaptor adaptor) {
    return adaptors.remove(adaptor);
  }

  /**
   * Removes all adaptors.
   */
  public void clear() {
    adaptors.clear();
  }

  /**
   * Gets currently available adaptors.
   * @return all adaptors currently managed by this instance. Empty array 
   * if non available.
   */
  public DSViewAdaptor[] getAdaptors() {
    //TODO: return adaptors sorted by hashCode after implimenting comparable
    return (DSViewAdaptor[]) adaptors.toArray(
      new DSViewAdaptor[adaptors.size()]);
  }

  /** 
   * @return dataset display names for all managed adaptors.
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetDisplayNames()
   */
  public String[] getDatasetDisplayNames() throws ConfigurationException {
    List names = new ArrayList();
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();
      names.addAll(Arrays.asList(adaptor.getDatasetDisplayNames()));
    }
    return (String[]) names.toArray(new String[names.size()]);
  }

  /**
   * @return dataset internal names for all managed adaptors.
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetInternalNames()
   */
  public String[] getDatasetInternalNames() throws ConfigurationException {
    List names = new ArrayList();
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();
      names.addAll(Arrays.asList(adaptor.getDatasetInternalNames()));
    }

    return (String[]) names.toArray(new String[names.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViews()
   */
  public DatasetView[] getDatasetViews() throws ConfigurationException {
    List views = new ArrayList();
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();
      views.addAll(Arrays.asList(adaptor.getDatasetViews()));
    }
    return (DatasetView[]) views.toArray(new DatasetView[views.size()]);
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDisplayName(java.lang.String)
   */
  public boolean supportsDisplayName(String name)
    throws ConfigurationException {
    return null != getDatasetViewByDisplayName(name);
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDisplayName(java.lang.String)
   */
  public DatasetView getDatasetViewByDisplayName(String name)
    throws ConfigurationException {

    DatasetView result = null;
    DatasetView[] views = getDatasetViews();
    for (int i = 0, n = views.length; i < n; i++) {
      DatasetView view = views[i];
      if (view.getDisplayName().equals(name)) {
        result = view;
        break;
      }
    }
    return result;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsInternalName(java.lang.String)
   */
  public boolean supportsInternalName(String name)
    throws ConfigurationException {
    return getDatasetViewByInternalName(name) != null;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByInternalName(java.lang.String)
   */
  public DatasetView getDatasetViewByInternalName(String name)
    throws ConfigurationException {

    DatasetView result = null;

    DatasetView[] views = getDatasetViews();
    for (int i = 0, n = views.length; i < n; i++) {
      DatasetView view = views[i];
      if (view.getInternalName().equals(name)) {
        result = view;
        break;
      }
    }
    return result;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
   */
  public void update() throws ConfigurationException {
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();
      adaptor.update();
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#lazyLoad(org.ensembl.mart.lib.config.DatasetView)
   */
  public void lazyLoad(DatasetView dsv) throws ConfigurationException {
    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();
      if (adaptor.supportsInternalName(dsv.getInternalName())) {
        adaptor.lazyLoad(dsv);
        break;
      }
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.MultiDSViewAdaptor#removeDatasetView(org.ensembl.mart.lib.config.DatasetView)
   */
  public boolean removeDatasetView(DatasetView dsv)
    throws ConfigurationException {
    boolean removed = false;

    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();

      if (adaptor.supportsInternalName(dsv.getInternalName())) {
        if (adaptor instanceof MultiDSViewAdaptor) {
          String[] inames = adaptor.getDatasetInternalNames();
          if (inames.length == 1 && inames[0].equals(dsv.getInternalName()))
            adaptors.remove(adaptor);
          else
             ((MultiDSViewAdaptor) adaptor).removeDatasetView(dsv);
          removed = true;
        } else
          removed = adaptors.remove(adaptor);
        break;
      }
    }

    return removed;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getMartLocations()
   */
  public MartLocation[] getMartLocations() throws ConfigurationException {
    List locations = new ArrayList();

    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();

      locations.addAll(Arrays.asList(adaptor.getMartLocations()));
    }

    MartLocation[] retlocs = new MartLocation[locations.size()];
    locations.toArray(retlocs);
    return retlocs;
  }

  /**
   * Allows Equality Comparisons manipulation of DSViewAdaptor objects.  Although
   * any DSViewAdaptor object can be compared with any other DSViewAdaptor object, to provide
   * consistency with the compareTo method, in practice, it is almost impossible for different DSVIewAdaptor
   * implimentations to equal.
   */
  public boolean equals(Object o) {
    return o instanceof DSViewAdaptor && hashCode() == o.hashCode();
  }

  /**
   * Calculated from all included adaptor hashCodes.
   */
  public int hashCode() {
    int hsh = 0;

    for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
      DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();
      int h = adaptor.hashCode();
      hsh += h;
    }

    return hsh;
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
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDataset(java.lang.String)
   */
  public boolean supportsDataset(String dataset)
    throws ConfigurationException {
    return getDatasetViewByDataset(dataset).length>0;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDataset(java.lang.String)
   */
  public DatasetView[] getDatasetViewByDataset(String dataset)
    throws ConfigurationException {

    ArrayList l = new ArrayList();
    DatasetView[] views = getDatasetViews();
    for (int i = 0, n = views.length; i < n; i++) {
      DatasetView view = views[i];
      if (view.getDataset().equals( dataset )) {
        l.add( view );
      }
    }
    
    return (DatasetView[]) l.toArray(new DatasetView[l.size()]);

  }

	/**
   * @return "Composite"
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDisplayName()
	 */
	public String getDisplayName() {
		return "Composite";
	}


  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDatasetInternalName(java.lang.String, java.lang.String)
   */
  public DatasetView getDatasetViewByDatasetInternalName(
    String dataset,
    String internalName)
    throws ConfigurationException {

    DatasetView view = null;
    DatasetView[] dsviews = getDatasetViews();
    for (int i = 0; i < dsviews.length; ++i) {

      DatasetView dsv = dsviews[i];
      if (StringUtil.compare(dataset, dsv.getDataset()) == 0
        && StringUtil.compare(internalName, dsv.getInternalName()) == 0)
        view = dsv;
    }

    return view;
  }
      

}
