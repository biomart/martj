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

import java.net.URL;
import java.util.logging.Logger;

/**
 * DSViewAdaptor implimenting object designed to provide a DatasetView object from
 * from an URL.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class URLDSViewAdaptor implements DSViewAdaptor {
  private final URL dsvurl;
  private final boolean validate;
  
  private DatasetView dsv;
  private String[] inames;
  private String[] dnames;
  private Logger logger = Logger.getLogger(URLDSViewAdaptor.class.getName());
  
  public URLDSViewAdaptor(URL url, boolean validate) throws ConfigurationException {
     if (url == null)
       throw new ConfigurationException("DSViewURLAdaptors must be instantiated with a URL\n");
    dsvurl = url;
    this.validate = validate;   
    update();   
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
    try {
       if (dsvurl.getProtocol().matches("file"))
         dsv = ConfigurationUtils.getDatasetView(dsvurl.openStream(), validate);
       else
         throw new ConfigurationException("Non-file URLS are not currently Supported: " + dsvurl + "\n");
    } catch (Exception e) {
       throw new ConfigurationException("Could not load DatasetView from URL: " + dsvurl + " " + e.getMessage(),e);  
    }
    
    inames = new String[] { dsv.getInternalName()};
    dnames = new String[] { dsv.getDisplayName()}; 
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
    return o instanceof URLDSViewAdaptor && hashCode() == o.hashCode();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    int hshcode = 0;
    if ( dsvurl!=null ) hshcode = ( 31 * hshcode ) + dsvurl.hashCode();
    hshcode = ( 31 * hshcode ) + ( validate ? 1 : 2 );
    if ( dsv!=null ) hshcode = ( 31 * hshcode ) + dsv.hashCode();
    return hshcode;
  }

}
