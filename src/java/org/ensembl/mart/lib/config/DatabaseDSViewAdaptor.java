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

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.sql.DataSource;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.util.BigPreferences;
import org.ensembl.util.StringUtil;

/**
 * DSViewAdaptor implimentation that retrieves DatasetView objects from
 * a Mart Database.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSViewAdaptor implements MultiDSViewAdaptor, Comparable {

  private final String DIGESTKEY = "MD5";
  private final String XMLKEY = "XML";
  Preferences xmlCache = null;

  private String dbpassword;
  private Logger logger = Logger.getLogger(DatabaseDSViewAdaptor.class.getName());
  private List dsviews = new ArrayList();
  private SortedSet datasetNameMap = new TreeSet();
  private HashMap inameMap = new HashMap();
  private HashMap dnameMap = new HashMap();

  private final DetailedDataSource dataSource;

  private final String user;
  private final int hashcode;
  private String adaptorName = null;

  /**
   * Constructor for a DatabaseDSViewAdaptor
   * @param ds -- DataSource for Mart RDBMS
   * @param user -- user for RDBMS connection, AND _meta_DatasetView_user table
   * @throws ConfigurationException if DataSource or user is null
   */
  public DatabaseDSViewAdaptor(DetailedDataSource ds, String user) throws ConfigurationException {
    if (ds == null || user == null)
      throw new ConfigurationException("DatabaseDSViewAdaptor Objects must be instantiated with a DataSource and User\n");

    this.user = user;
    dataSource = ds;
    String host = ds.getHost();
    String port = ds.getPort();
    String databaseName = ds.getDatabaseName();
    try {
      //set up the preferences node with the datasource information as the root node
      xmlCache =
        BigPreferences.userNodeForPackage(DatabaseDSViewAdaptor.class).node(
          host + "/" + port + "/" + databaseName);
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException(
        "Caught IllegalArgumentException during parse of Connection for Connection Parameters " + e.getMessage(),
        e);
    }

    int tmp = user.hashCode();
    tmp = (31 * tmp) + host.hashCode();
    tmp = (port != null) ? (31 * tmp) + port.hashCode() : tmp;
    tmp = (ds.getDatabaseType() != null) ? (31 * tmp) + ds.getDatabaseType().hashCode() : tmp;
    tmp = (databaseName != null) ? (31 * tmp) + databaseName.hashCode() : tmp;
    tmp = (31 * tmp) + ds.getJdbcDriverClassName().hashCode();
    hashcode = tmp;
    
    adaptorName = user + "@" + host + ":" + port + "/" + databaseName;
    
    update();
  }

  /**
   * This method should ONLY be used if the user is not concerned with network password snooping, as it does
   * not do anything to encrypt the password provided.  It is really a convenience method for users wishing to
   * create MartRegistry files with their database password attribute filled in.
   * @param password -- String password for underlying DataSource
   * @see org.ensembl.mart.lib.config.DatabaseDSViewAdaptor#getMartLocations
   */
  public void setDatabasePassword(String password) {
    dbpassword = password;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetDisplayNames()
   */
  public String[] getDatasetDisplayNames() throws ConfigurationException {
    String[] ret = new String[dnameMap.size()];
    dnameMap.keySet().toArray(ret);
    return ret;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetInternalNames()
   */
  public String[] getDatasetInternalNames() throws ConfigurationException {
    String[] ret = new String[inameMap.size()];
    inameMap.keySet().toArray(ret);
    return ret;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViews()
   */
  public DatasetView[] getDatasetViews() throws ConfigurationException {
    DatasetView[] ret = new DatasetView[dsviews.size()];
    dsviews.toArray(ret);
    return ret;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDisplayName(java.lang.String)
   */
  public boolean supportsDisplayName(String name) {
    return dnameMap.containsKey(name);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDisplayName(java.lang.String)
   */
  public DatasetView getDatasetViewByDisplayName(String name) throws ConfigurationException {
    return (DatasetView) dnameMap.get(name);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsInternalName(java.lang.String)
   */
  public boolean supportsInternalName(String name) {
    return inameMap.containsKey(name);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByInternalName(java.lang.String)
   */
  public DatasetView getDatasetViewByInternalName(String name) throws ConfigurationException {
    return (DatasetView) inameMap.get(name);
  }

  public void addDatasetView(DatasetView dsv) throws ConfigurationException {
    if (!(inameMap.containsKey(dsv.getInternalName()) && dnameMap.containsKey(dsv.getDisplayName()))) {
      datasetNameMap.add( dsv.getDataset() );
      dsv.setDatasource(dataSource);
      dsv.setDSViewAdaptor(this);

      inameMap.put(dsv.getInternalName(), dsv);
      dnameMap.put(dsv.getDisplayName(), dsv);
      dsv.setDSViewAdaptor(this);
      dsviews.add(dsv);
    }

    updateXMLCache();
  }

  private void updateXMLCache() throws ConfigurationException {
    String iname = null;

    try {
      for (Iterator iter = inameMap.keySet().iterator(); iter.hasNext();) {
        iname = (String) iter.next();
        DatasetView dsv = (DatasetView) inameMap.get(iname);

        if (xmlCache.nodeExists(iname)) {
          byte[] md5 = xmlCache.node(iname).getByteArray(DIGESTKEY, new byte[0]);

          if (!MessageDigest.isEqual(md5, dsv.getMessageDigest()))
            addToXMLCache(dsv);
        } else
          addToXMLCache(dsv);
      }
      xmlCache.flush();
    } catch (BackingStoreException e) {
      throw new ConfigurationException(
        "Caught BackingStoreException checking for Preferences node "
          + iname
          + " "
          + e.getMessage()
          + "\nAssuming it doesnt exist\n",
        e);
    } catch (ConfigurationException e) {
      throw e;
    }
  }

  private void addToXMLCache(DatasetView dsv) throws ConfigurationException {
    String iname = dsv.getInternalName();
    xmlCache.node(iname).putByteArray(DIGESTKEY, dsv.getMessageDigest());
    xmlCache.node(iname).putByteArray(XMLKEY, DatasetViewXMLUtils.DatasetViewToByteArray(dsv));

    try {
      xmlCache.flush();
    } catch (BackingStoreException e) {
      throw new ConfigurationException(
        "Caught BackingStoreException adding new DatasetView to preferences node "
          + iname
          + " "
          + e.getMessage()
          + "\nAssuming it doesnt exist\n");
    }
  }

  private void removeFromXMLCache(DatasetView dsv) throws ConfigurationException {
    String iname = dsv.getInternalName();
    xmlCache.node(iname).remove(iname); //removes this node entirely
    try {
      xmlCache.flush();
    } catch (BackingStoreException e) {
      throw new ConfigurationException(
        "Caught BackingStoreException removing DatasetView from preferences node "
          + iname
          + " "
          + e.getMessage()
          + "\nAssuming it doesnt exist\n");
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.MultiDSViewAdaptor#removeDatasetView(org.ensembl.mart.lib.config.DatasetView)
   */
  public boolean removeDatasetView(DatasetView dsv) throws ConfigurationException {
    if (inameMap.containsKey(dsv.getInternalName())) {
      datasetNameMap.remove( dsv.getDataset() );
      inameMap.remove(dsv.getInternalName());
      dnameMap.remove(dsv.getDisplayName());
      dsviews.remove(dsv);
      dsv.setDSViewAdaptor(null);
      removeFromXMLCache(dsv);      
      return true;
    } else
      return false;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
   */
  public void update() throws ConfigurationException {
    String[] inms = DatabaseDatasetViewUtils.getAllInternalNames(dataSource, user);
    for (int i = 0, n = inms.length; i < n; i++) {
      String iname = inms[i];
      boolean cacheExists = false;

      try {
        cacheExists = xmlCache.nodeExists(iname);
      } catch (BackingStoreException e) {
        if (logger.isLoggable(Level.INFO))
          logger.info(
            "Caught BackingStoreException checking for Preferences node "
              + iname
              + " "
              + e.getMessage()
              + "\nAssuming it doesnt exist\n");
        cacheExists = false;
      }

      if (inameMap.containsKey(iname)) {
        byte[] nDigest = DatabaseDatasetViewUtils.getDSViewMessageDigestByInternalName(dataSource, user, iname);
        byte[] oDigest = ((DatasetView) inameMap.get(iname)).getMessageDigest();

        if (!MessageDigest.isEqual(oDigest, nDigest)) {
          removeDatasetView((DatasetView) inameMap.get(iname));
          addDatasetView(DatabaseDatasetViewUtils.getDatasetViewByInternalName(dataSource, user, iname));
        }
      } else if (cacheExists) {
        byte[] nDigest = DatabaseDatasetViewUtils.getDSViewMessageDigestByInternalName(dataSource, user, iname);
        byte[] oDigest = xmlCache.node(iname).getByteArray(DIGESTKEY, new byte[0]);
        //if the cache cannot return the digest for some reason, it should return null

        if (MessageDigest.isEqual(oDigest, nDigest)) {
          byte[] cachedXML = xmlCache.node(iname).getByteArray(XMLKEY, null);
          // should return a null if it cant load for some reason

          if (cachedXML != null) {
            DatasetView newDSV = DatasetViewXMLUtils.ByteArrayToDatasetView(cachedXML);
            newDSV.setMessageDigest(oDigest);
            addDatasetView(newDSV);
          } else {
            //get it from the database
            DatasetView newDSV = DatabaseDatasetViewUtils.getDatasetViewByInternalName(dataSource, user, iname);
            addDatasetView(newDSV);
          }
        } else {
          DatasetView newDSV = DatabaseDatasetViewUtils.getDatasetViewByInternalName(dataSource, user, iname);
          addDatasetView(newDSV);
        }
      } else {
        DatasetView newDSV = DatabaseDatasetViewUtils.getDatasetViewByInternalName(dataSource, user, iname);
        addDatasetView(newDSV);
      }
    }
  }

  /**
   * Allows client to store a single DatasetView object as a DatasetView.dtd compliant XML document into a Mart Database.
   * Client can choose whether to compress (GZIP) the resulting XML before it is stored in the Database.
   * @param ds -- DataSource of the Mart Database where the DatasetView.dtd compliant XML is to be stored.
   * @param user -- RDBMS user for _meta_DatasetView_[user] table to store the document.  If null, or if _meta_DatasetView_[user] does not exist, _meta_DatasetView will be the target of the document.
   * @param dsv -- DatasetView object to store
   * @param compress -- if true, the resulting XML will be gzip compressed before storing into the table.
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static void storeDatasetView(DataSource ds, String user, DatasetView dsv, boolean compress)
    throws ConfigurationException {
    DatabaseDatasetViewUtils.storeConfiguration(
      ds,
      user,
      dsv.getInternalName(),
      dsv.getDisplayName(),
      dsv.getDataset(),
      dsv.getDescription(),
      DatasetViewXMLUtils.DatasetViewToDocument(dsv),
      compress);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#lazyLoad(org.ensembl.mart.lib.config.DatasetView)
   */
  public void lazyLoad(DatasetView dsv) throws ConfigurationException {
    DatasetViewXMLUtils.LoadDatasetViewWithDocument(
      dsv,
      DatabaseDatasetViewUtils.getDatasetViewDocumentByInternalName(dataSource, user, dsv.getInternalName()));
  }

  /**
   * Note, this method will only include the DataSource password in the resulting MartLocation object
   * if the user set the password using the setDatabasePassword method of this adaptor. Otherwise, 
   * regardless of whether the underlying DataSource was created
   * with a password, the resulting DatabaseLocation element will not have
   * a password attribute.  Users may need to hand modify any MartRegistry documents
   * that they create in these cases.  Users are encouraged to use
   * passwordless, readonly access users.
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getMartLocations()
   */
  public MartLocation[] getMartLocations() throws ConfigurationException {
    MartLocation dbloc =
      new DatabaseLocation(
        dataSource.getHost(),
        dataSource.getPort(),
        dataSource.getDatabaseType(),
        dataSource.getDatabaseName(),
        user,
        dbpassword,
        dataSource.getJdbcDriverClassName(), adaptorName);
    return new MartLocation[] { dbloc };
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
   * Calculation is purely based on the DataSource and user hashCode.  Any
   * DatabaseDSViewAdaptor based on these two inputs should represent the same
   * collection of DatasetView objects.
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
    return hashcode - ((DSViewAdaptor) o).hashCode();
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDataset(java.lang.String)
   */
  public boolean supportsDataset(String dataset) throws ConfigurationException {
    return getDatasetViewByDataset(dataset).length > 0;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDataset(java.lang.String)
   */
  public DatasetView[] getDatasetViewByDataset(String dataset) throws ConfigurationException {

    ArrayList l = new ArrayList();

    for (int i = 0, n = dsviews.size(); i < n; i++) {
      DatasetView view = (DatasetView) dsviews.get(i);
      if (view.getDataset().equals(dataset)) {
        l.add(view);
      }
    }

    return (DatasetView[]) l.toArray(new DatasetView[l.size()]);

  }

  /**
   * @return datasource.toString() if datasource is not null, otherwise 
   * "No Database".
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDisplayName()
   */
  public String getDisplayName() {

    return (dataSource == null) ? "No Database" : dataSource.toString();
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDatasetInternalName(java.lang.String, java.lang.String)
   */
  public DatasetView getDatasetViewByDatasetInternalName(String dataset, String internalName)
    throws ConfigurationException {

    DatasetView view = null;
    for (int i = 0; i < dsviews.size(); ++i) {

      DatasetView dsv = (DatasetView) dsviews.get(i);
      if (StringUtil.compare(dataset, dsv.getDataset()) == 0
        && StringUtil.compare(internalName, dsv.getInternalName()) == 0)
        view = dsv;
    }

    return view;
  }

  /**
   * DatabaseDSViewAdaptor objects do not contain child adaptors
   * @return null
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getAdaptorByName(java.lang.String)
   */
  public DSViewAdaptor getAdaptorByName(String adaptorName) throws ConfigurationException {
    // DatabaseDSViewAdaptor objects do not contain child adaptors
    return null;
  }

  /**
   * DatabaseDSViewAdaptor objects do not contain child adaptors
   * return empty String[]
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getAdaptorNames()
   */
  public String[] getAdaptorNames() throws ConfigurationException {
    return new String[0];
  }

  /**
   * DatabaseDSViewAdaptor objects do not contain child adaptors
   * return empty DSViewAdaptor[]
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getAdaptors()
   */
  public DSViewAdaptor[] getAdaptors() throws ConfigurationException {
    // DatabaseDSViewAdaptor objects do not contain child adaptors
    return new DSViewAdaptor[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetNames()
   */
  public String[] getDatasetNames() throws ConfigurationException {
    return (String[]) datasetNameMap.toArray(new String[datasetNameMap.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetNames(java.lang.String)
   */
  public String[] getDatasetNames(String adaptorName) throws ConfigurationException {
    if (adaptorName.equals(this.adaptorName))
      return getDatasetNames();
    else
      return new String[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewDisplayNamesByDataset(java.lang.String)
   */
  public String[] getDatasetViewDisplayNamesByDataset(String dataset) throws ConfigurationException {
    List names = new ArrayList();
    DatasetView[] views = getDatasetViewByDataset(dataset);

    for (int i = 0, n = views.length; i < n; i++) {
      DatasetView dsv = (DatasetView) views[i];

      if (dsv.getDataset().equals(dataset))
        names.add(dsv.getDisplayName());
    }

    return (String[]) names.toArray(new String[names.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewInternalNamesByDataset(java.lang.String)
   */
  public String[] getDatasetViewInternalNamesByDataset(String dataset) throws ConfigurationException {
    List names = new ArrayList();
    DatasetView[] views = getDatasetViewByDataset(dataset);

    for (int i = 0, n = views.length; i < n; i++) {
      DatasetView dsv = (DatasetView) views[i];

      if (dsv.getDataset().equals(dataset))
        names.add(dsv.getInternalName());
    }

    return (String[]) names.toArray(new String[names.size()]);
  }

  /**
   * return name, defaults to "user@host:port/databaseName"
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
   * DatabaseDSViewAdaptor objects do not contain child adaptors
   * return false
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsAdaptor(java.lang.String)
   */
  public boolean supportsAdaptor(String adaptorName) throws ConfigurationException {
    return false;
  }

}
