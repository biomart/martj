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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.InputSourceUtil;
import org.ensembl.util.StringUtil;

/**
 * DSConfigAdaptor implimenting object designed to provide a TransformationConfig object from
 * from an URL.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class URLDSConfigAdaptor extends LeafDSConfigAdaptor implements DSConfigAdaptor, Comparable {
  private final URL dsvurl;
  private final int hashcode;

  private TransformationConfig dsv;
  private String[] inames;
  private String[] dnames;
  private Logger logger = Logger.getLogger(URLDSConfigAdaptor.class.getName());
  private String adaptorName;
  private boolean ignoreCache = false;
  private TransformationConfigXMLUtils dscutils = null;
  
  /**
   * Construct a DSConfigAdaptor from a url containing a TransformationConfig.dtd compliant XML Document,
   * with optional JDOM validation.
   * @param url -- url containing a TransformationConfig.dtd compliant XML document
   * @param ignoreCache -- if true, never caches and always parses the source URL for its TransformationConfig objects
   * @param includeHiddenMembers -- if true, hidden members will be included in TransformationConfig objects, if false they will be skipped
   * @throws ConfigurationException for all underlying Exceptions.
   */
  public URLDSConfigAdaptor(URL url, boolean ignoreCache, boolean includeHiddenMembers) throws ConfigurationException {
    if (url == null)
      throw new ConfigurationException("DSConfigURLAdaptors must be instantiated with a URL\n");
    dsvurl = url;
    this.ignoreCache = ignoreCache;
      
    setName(dsvurl.toString());
    
    dscutils = new TransformationConfigXMLUtils(includeHiddenMembers);

    hashcode = dsvurl.hashCode();
    update();
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
    try {
      dsv = dscutils.getTransformationConfigForXMLStream(InputSourceUtil.getStreamForURL(dsvurl));
    } catch (Exception e) {
      throw new ConfigurationException(
        "Could not load TransformationConfig from URL: " + dsvurl.toString() + " " + e.getMessage(),
        e);
    }

    inames = new String[] { dsv.getInternalName()};
    //dnames = new String[] { dsv.getDisplayName()};

    dsv.setDSConfigAdaptor(this);
  }

  /**
   * Useful debug output
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("[");
    buf.append(" url=").append(dsvurl.toString());
    //buf.append(", dataset DisplayName=").append(dsv.getDisplayName());
    buf.append("]");

    return buf.toString();
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
   * Based solely on the underlying URL.
   * Two URLDSConfigAdaptors should return the same hashCode if they
   * are based on the same underlying URL. If the underlying TransformationConfig
   * has changed between instantiation of the two URLDSConfigAdaptor objects,
   * a call to update() should resolve this.
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

  /*
   * (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#lazyLoad(org.ensembl.mart.lib.config.TransformationConfig)
   */
  public void lazyLoad(TransformationConfig dsv) throws ConfigurationException {
    try {
      dscutils.loadTransformationConfigWithDocument( dsv, dscutils.getDocumentForXMLStream( InputSourceUtil.getStreamForURL( dsvurl ) ) );
    } catch (IOException e) {
      throw new ConfigurationException("Recieved IOException lazyLoading TransformationConfig: " + e.getMessage(), e);
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getMartLocations()
   */
  //public MartLocation[] getMartLocations() throws ConfigurationException {
  //  return new MartLocation[] { new URLLocation(dsvurl, adaptorName, "true")};
  //}

  /**
   * Writes a TransformationConfig object as TransformationConfig.dtd compliant XML to a File.
   * @param dsv -- TransformationConfig object to store to the file system
   * @param file -- File to write XML
   * @throws ConfigurationException for underlying Exceptions
   */
  public static void StoreTransformationConfig(TransformationConfig dsv, File file) throws ConfigurationException {
    TransformationConfigXMLUtils dscutils = new TransformationConfigXMLUtils(false); //hidden members are only applicable to incoming XML streams
    dscutils.writeTransformationConfigToFile(dsv, file);
  }

  
  /**
   * @return "URL"
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDisplayName()
   */
  public String getDisplayName() {
    return "URL";
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
   * URLDSConfigAdaptor Objects do not contain child DSConfigAdaptor Objects.
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
  public boolean containsTransformationConfig(TransformationConfig dsv) throws ConfigurationException {
    return this.dsv != null && this.dsv.equals(dsv);
  }
  
  /**
   * Do nothing.
   */
  public void clearCache() {
    
  }
}
