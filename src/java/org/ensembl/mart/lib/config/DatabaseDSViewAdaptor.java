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
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.util.BigPreferences;
import org.ensembl.util.StringUtil;

import com.sun.rsasign.l;

/**
 * DSViewAdaptor implimentation that retrieves DatasetView objects from
 * a Mart Database.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSViewAdaptor implements MultiDSViewAdaptor, Comparable {

  //each dataset will have 2 name maps, and a Set of DatasetView objects associated with it in an ArrayList
  private final int INAME_INDEX = 0;
  private final int DNAME_INDEX = 1;
  private final int VIEW_INDEX = 2;

  private final String DIGESTKEY = "MD5";
  private final String XMLKEY = "XML";
  Preferences xmlCache = null;

  private String dbpassword;
  private Logger logger = Logger.getLogger(DatabaseDSViewAdaptor.class.getName());
  private List dsviews = new ArrayList();
  private HashMap datasetNameMap = new HashMap();

  private final DetailedDataSource dataSource;

  private final String user;
  private final int hashcode;
  private String adaptorName = null;

  private boolean clearCache = false; //developer hack to clear the cache
  //will be replaced soon with user supported clearing

  /**
   * Constructor for a DatabaseDSViewAdaptor
   * @param ds -- DataSource for Mart RDBMS
   * @param user -- user for RDBMS connection, AND meta_DatasetView_user table
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

    adaptorName = ds.getName();

    try {
      //set up the preferences node with the datasource information as the root node
      if (clearCache) {
        BigPreferences.userNodeForPackage(DatabaseDSViewAdaptor.class).node(adaptorName).node(user).removeNode();
        BigPreferences.userNodeForPackage(DatabaseDSViewAdaptor.class).flush();
      }

      xmlCache = BigPreferences.userNodeForPackage(DatabaseDSViewAdaptor.class).node(adaptorName).node(user);
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException(
        "Caught IllegalArgumentException during parse of Connection for Connection Parameters " + e.getMessage(),
        e);
    } catch (BackingStoreException e) {
      throw new ConfigurationException("Caught BackingStoreException clearing cache: " + e.getMessage(), e);
    }

    int tmp = user.hashCode();
    tmp = (31 * tmp) + host.hashCode();
    tmp = (port != null) ? (31 * tmp) + port.hashCode() : tmp;
    tmp = (ds.getDatabaseType() != null) ? (31 * tmp) + ds.getDatabaseType().hashCode() : tmp;
    tmp = (databaseName != null) ? (31 * tmp) + databaseName.hashCode() : tmp;
    tmp = (31 * tmp) + ds.getJdbcDriverClassName().hashCode();
    tmp = (31 * tmp) + adaptorName.hashCode();
    hashcode = tmp;

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
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViews()
   */
  public DatasetView[] getDatasetViews() throws ConfigurationException {
    DatasetView[] ret = new DatasetView[dsviews.size()];
    
    for (int i = 0, n = dsviews.size(); i < n; i++) {
      DatasetView dsvorig = (DatasetView) dsviews.get(i);
      ret[i] = new DatasetView(dsvorig); //return copy of datasetview, so that lazyLoad doesnt expand reference to original
    }
    return ret;
  }

  public void addDatasetView(DatasetView dsv) throws ConfigurationException {
    if (!(datasetNameMap.containsKey(dsv.getDataset()))) {
      dsv.setDSViewAdaptor(this);
      dsviews.add(dsv); //add to the global dsviews list

      HashMap inameMap = new HashMap();
      HashMap dnameMap = new HashMap();
      ArrayList views = new ArrayList();

      inameMap.put(dsv.getInternalName(), dsv);
      dnameMap.put(dsv.getDisplayName(), dsv);
      views.add(dsv);

      Vector maps = new Vector();
      maps.add(INAME_INDEX, inameMap);
      maps.add(DNAME_INDEX, dnameMap);
      maps.add(VIEW_INDEX, views);

      datasetNameMap.put(dsv.getDataset(), maps);
    } else {
      Vector maps = (Vector) datasetNameMap.get(dsv.getDataset());
      HashMap inameMap = (HashMap) maps.get(INAME_INDEX);
      HashMap dnameMap = (HashMap) maps.get(DNAME_INDEX);
      ArrayList views = (ArrayList) maps.get(VIEW_INDEX);

      if (!(inameMap.containsKey(dsv.getInternalName()) && dnameMap.containsKey(dsv.getDisplayName()))) {
        dsv.setDSViewAdaptor(this);
        dsviews.add(dsv); //add to the global dsviews list

        inameMap.put(dsv.getInternalName(), dsv);
        dnameMap.put(dsv.getDisplayName(), dsv);
        views.add(dsv);

        maps.remove(INAME_INDEX);
        maps.add(INAME_INDEX, inameMap);
        maps.remove(DNAME_INDEX);
        maps.add(DNAME_INDEX, dnameMap);
        maps.remove(VIEW_INDEX);
        maps.add(VIEW_INDEX, views);

        datasetNameMap.put(dsv.getDataset(), maps);
      }
    }
  }

  //  private void updateXMLCache() throws ConfigurationException {
  //    String iname = null;
  //    String dataset = null;
  //
  //    try {
  //      for (Iterator iter = dsviews.iterator(); iter.hasNext();) {
  //        DatasetView dsv = (DatasetView) iter.next();
  //        iname = dsv.getInternalName();
  //        dataset = dsv.getDataset();
  //
  //        if (xmlCache.nodeExists(dataset) && xmlCache.node(dataset).nodeExists(iname)) {
  //          byte[] md5 = xmlCache.node(dataset).node(iname).getByteArray(DIGESTKEY, new byte[0]);
  //
  //          if (!MessageDigest.isEqual(md5, dsv.getMessageDigest()))
  //            addToXMLCache(dsv);
  //        } else
  //          addToXMLCache(dsv);
  //      }
  //      xmlCache.flush();
  //    } catch (BackingStoreException e) {
  //      throw new ConfigurationException(
  //        "Caught BackingStoreException checking for Preferences node "
  //          + dataset
  //          + " internalname "
  //          + iname
  //          + " "
  //          + e.getMessage()
  //          + "\nAssuming it doesnt exist\n",
  //        e);
  //    } catch (ConfigurationException e) {
  //      throw e;
  //    }
  //  }

  private void addToXMLCache(DatasetView dsv) throws ConfigurationException {
    String iname = dsv.getInternalName();
    String dataset = dsv.getDataset();

    if (logger.isLoggable(Level.INFO))
      logger.info("adding DatasetView " + dataset + " " + iname + " to cache\n");
    byte[] xml = DatasetViewXMLUtils.DatasetViewToByteArray(dsv);
    byte[] digest = dsv.getMessageDigest();
    
    if (logger.isLoggable(Level.INFO))
      logger.info("Storing " + xml.length + " bytes of XML to cache\n");
      
    if (digest == null)
      throw new ConfigurationException("Recieved DatasetView to lazyLoad without a stored MessageDigest\n");

    xmlCache.node(dataset).node(iname).putByteArray(DIGESTKEY, digest);
    xmlCache.node(dataset).node(iname).putByteArray(XMLKEY, xml);

    try {
      xmlCache.flush();
    } catch (BackingStoreException e) {
      throw new ConfigurationException(
        "Caught BackingStoreException adding new DatasetView to preferences node "
          + dataset
          + " internalName "
          + iname
          + " "
          + e.getMessage()
          + "\nAssuming it doesnt exist\n");
    }
  }

  private void removeFromXMLCache(String dataset, String iname) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info("Removing old cache for " + dataset + " " + iname + " if any\n");

    try {
      xmlCache.node(dataset).remove(iname); //removes this node entirely
      if (xmlCache.node(dataset).childrenNames().length == 0)
        xmlCache.remove(dataset); //removes the dataset node, if empty
      xmlCache.flush();
    } catch (BackingStoreException e) {
      throw new ConfigurationException(
        "Caught BackingStoreException removing DatasetView from preferences node "
          + dataset
          + " internalName "
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
    if (datasetNameMap.containsKey(dsv.getDataset())) {
      Vector maps = (Vector) datasetNameMap.get(dsv.getDataset());
      HashMap inameMap = (HashMap) maps.get(INAME_INDEX);
      HashMap dnameMap = (HashMap) maps.get(DNAME_INDEX);
      ArrayList views = (ArrayList) maps.get(VIEW_INDEX);

      if (inameMap.containsKey(dsv.getInternalName())) {
        datasetNameMap.remove(dsv.getDataset());
        inameMap.remove(dsv.getInternalName());
        dnameMap.remove(dsv.getDisplayName());
        dsviews.remove(dsv);
        views.remove(dsv);
        dsv.setDSViewAdaptor(null);
        removeFromXMLCache(dsv.getDataset(), dsv.getInternalName());

        //if this dataset is completely removed from the adaptor, make sure its keys reflect its removal
        if (views.size() > 0) {
          maps.remove(INAME_INDEX);
          maps.remove(DNAME_INDEX);
          maps.remove(VIEW_INDEX);
          maps.add(INAME_INDEX, inameMap);
          maps.add(DNAME_INDEX, dnameMap);
          maps.add(VIEW_INDEX, views);
          datasetNameMap.put(dsv.getDataset(), maps);
        } else
          datasetNameMap.remove(dsv.getDataset());

        return true;
      } else
        return false;
    } else
      return false;
  }

  private void checkMemoryForUpdate(String dataset, HashMap inameMap, String iname) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info(" Already loaded, check for update\n");

    byte[] nDigest =
      DatabaseDatasetViewUtils.getDSViewMessageDigestByDatasetInternalName(dataSource, user, dataset, iname);
    byte[] oDigest = ((DatasetView) inameMap.get(iname)).getMessageDigest();

    if (!MessageDigest.isEqual(oDigest, nDigest)) {

      if (logger.isLoggable(Level.INFO))
        logger.info("Needs update\n");

      removeDatasetView((DatasetView) inameMap.get(iname));
      loadFromDatabase(dataset, iname);
    }
  }

  private boolean cacheUpToDate(String dataset, String iname) throws ConfigurationException {
    boolean ret = false;
    try {
      ret = xmlCache.nodeExists(dataset);

      if (logger.isLoggable(Level.INFO))
        logger.info("cache node Dataset: " + dataset + " exists: " + ret + "\n");
      if (ret) {
        ret = xmlCache.node(dataset).nodeExists(iname);

        if (logger.isLoggable(Level.INFO))
          logger.info("cache node Dataset: " + dataset + " internalName: " + iname + " exists: " + ret + "\n");
      }
    } catch (BackingStoreException e) {
      if (logger.isLoggable(Level.INFO))
        logger.info(
          "Caught BackingStoreException checking for Preferences node "
            + dataset
            + " "
            + e.getMessage()
            + "\nAssuming it doesnt exist\n");
      ret = false;
    }

    if (ret) {
      byte[] nDigest =
        DatabaseDatasetViewUtils.getDSViewMessageDigestByDatasetInternalName(dataSource, user, dataset, iname);
      byte[] cacheDigest = xmlCache.node(dataset).node(iname).getByteArray(DIGESTKEY, new byte[0]);

      //if the cache cannot return the digest for some reason, it should return an empty byte[]
      
      ret = MessageDigest.isEqual(cacheDigest, nDigest);

      if (logger.isLoggable(Level.INFO))
        logger.info("Message Digests equal: " + ret + "\n");
    }

    if (!ret) {
      if (logger.isLoggable(Level.INFO))
        logger.info("Cache is not up to date for " + dataset + " " + iname + ", removing cache\n");
      removeFromXMLCache(dataset, iname);
    }
    return ret;
  }

  private void loadCacheOrUpdate(String dataset, String iname) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info("Not loaded yet, is it in cache and is cache up to date?");

    if (cacheUpToDate(dataset, iname)) {
      if (logger.isLoggable(Level.INFO))
        logger.info("getting from cache\n");

      byte[] cacheDigest = xmlCache.node(dataset).node(iname).getByteArray(DIGESTKEY, new byte[0]);
      byte[] cachedXML = xmlCache.node(dataset).node(iname).getByteArray(XMLKEY, null);
        
      // should return a null if it cant load for some reason

      if (cachedXML != null) {
        if (logger.isLoggable(Level.INFO))
          logger.info("Recieved " + cachedXML.length + " bytes of xml from cache\n");
          
        DatasetView newDSV = DatasetViewXMLUtils.ByteArrayToDatasetView(cachedXML);
        newDSV.setMessageDigest(cacheDigest);
        addDatasetView(newDSV);
      } else {
        if (logger.isLoggable(Level.INFO))
          logger.info("Recieved null xml from cache\n");
          
        loadFromDatabase(dataset, iname); 
      }
    } else {
      if (logger.isLoggable(Level.INFO))
        logger.info("Need to update cache\n");

      loadFromDatabase(dataset, iname);
    }
  }

  private void loadFromDatabase(String dataset, String iname) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info("Dataset " + dataset + " internalName " + iname + " Not in cache, loading from database\n");

    DatasetView newDSV = DatabaseDatasetViewUtils.getDatasetViewByDatasetInternalName(dataSource, user, dataset, iname);
    addDatasetView(newDSV);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
   */
  public void update() throws ConfigurationException {
    String[] datasets = DatabaseDatasetViewUtils.getAllDatasetNames(dataSource, user);
    for (int i = 0, n = datasets.length; i < n; i++) {
      String dataset = datasets[i];
      String[] inms = DatabaseDatasetViewUtils.getAllInternalNamesForDataset(dataSource, user, dataset);

      boolean datasetCacheExists = false;
      try {
        datasetCacheExists = xmlCache.nodeExists(dataset);
      } catch (BackingStoreException e) {
        if (logger.isLoggable(Level.INFO))
          logger.info(
            "Caught BackingStoreException checking for Preferences node "
              + dataset
              + " "
              + e.getMessage()
              + "\nAssuming it doesnt exist\n");
        datasetCacheExists = false;
      }

      if (datasetNameMap.containsKey(dataset)) {
        //dataset is loaded, check for update of its datasetview
        Vector maps = (Vector) datasetNameMap.get(dataset);
        HashMap inameMap = (HashMap) maps.get(INAME_INDEX);

        for (int k = 0, m = inms.length; k < m; k++) {
          String iname = inms[k];

          if (logger.isLoggable(Level.INFO))
            logger.info("Checking for dataset " + dataset + " internamName " + iname + "\n");

          boolean internalnameCacheExists = false;

          try {
            internalnameCacheExists = xmlCache.node(dataset).nodeExists(iname);
          } catch (BackingStoreException e) {
            if (logger.isLoggable(Level.INFO))
              logger.info(
                "Caught BackingStoreException checking for dataset "
                  + dataset
                  + " Preferences node "
                  + iname
                  + " "
                  + e.getMessage()
                  + "\nAssuming it doesnt exist\n");
            internalnameCacheExists = false;
          }

          if (inameMap.containsKey(iname))
            checkMemoryForUpdate(dataset, inameMap, iname);
          else if (internalnameCacheExists)
            loadCacheOrUpdate(dataset, iname);
          else
            loadFromDatabase(dataset, iname);
        }
      } else if (datasetCacheExists) {
        //not already loaded, check for its datasetviews in cache
        for (int k = 0, m = inms.length; k < m; k++) {
          String iname = inms[k];

          if (logger.isLoggable(Level.INFO))
            logger.info("Checking for dataset " + dataset + " internamName " + iname + "\n");

          boolean internalnameCacheExists = false;

          try {
            internalnameCacheExists = xmlCache.node(dataset).nodeExists(iname);
          } catch (BackingStoreException e) {
            if (logger.isLoggable(Level.INFO))
              logger.info(
                "Caught BackingStoreException checking for dataset "
                  + dataset
                  + " Preferences node "
                  + iname
                  + " "
                  + e.getMessage()
                  + "\nAssuming it doesnt exist\n");
            internalnameCacheExists = false;
          }

          if (internalnameCacheExists)
            loadCacheOrUpdate(dataset, iname);
          else {
            //load datasetview from database
            if (logger.isLoggable(Level.INFO))
              logger.info("Dataset " + dataset + " internalName " + iname + " not in cache, loading from database\n");

            loadFromDatabase(dataset, iname);
          }
        }
      } else {
        //load dataset from database
        if (logger.isLoggable(Level.INFO))
          logger.info("Dataset " + dataset + " not in cache, loading from database\n");

        for (int k = 0, m = inms.length; k < m; k++) {
          String iname = inms[k];
          loadFromDatabase(dataset, iname);
        }
      }
    }
  }

  /**
   * Allows client to store a single DatasetView object as a DatasetView.dtd compliant XML document into a Mart Database.
   * Client can choose whether to compress (GZIP) the resulting XML before it is stored in the Database.
   * @param ds -- DataSource of the Mart Database where the DatasetView.dtd compliant XML is to be stored.
   * @param user -- RDBMS user for meta_DatasetView_[user] table to store the document.  If null, or if meta_DatasetView_[user] does not exist, meta_DatasetView will be the target of the document.
   * @param dsv -- DatasetView object to store
   * @param compress -- if true, the resulting XML will be gzip compressed before storing into the table.
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static void storeDatasetView(DetailedDataSource ds, String user, DatasetView dsv, boolean compress)
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

  private void lazyLoadWithDatabase(DatasetView dsv) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info("lazy loading from database\n");

    DatasetViewXMLUtils.LoadDatasetViewWithDocument(
      dsv,
      DatabaseDatasetViewUtils.getDatasetViewDocumentByDatasetInternalName(
        dataSource,
        user,
        dsv.getDataset(),
        dsv.getInternalName()));

    //cache this DatasetView, as, for some reason, it is needing to be cached
    removeFromXMLCache(dsv.getDataset(), dsv.getInternalName());
    addToXMLCache(dsv);
  }

  private void lazyLoadWithCache(DatasetView dsv) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info("Attempting to lazy load with cache\n");

    byte[] cachedXML = xmlCache.node(dsv.getDataset()).node(dsv.getInternalName()).getByteArray(XMLKEY, null);
    //  should return a null if it cant load for some reason

    if (cachedXML != null) {
      if (logger.isLoggable(Level.INFO))
        logger.info("lazyLoading from non null cache of length " +  cachedXML.length + "\n");

      DatasetViewXMLUtils.LoadDatasetViewWithDocument(dsv, DatasetViewXMLUtils.ByteArrayToDocument(cachedXML));
    } else {
      if (logger.isLoggable(Level.INFO))
        logger.info("preferences returned null byte[], loading from database\n");
      lazyLoadWithDatabase(dsv);
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#lazyLoad(org.ensembl.mart.lib.config.DatasetView)
   */
  public void lazyLoad(DatasetView dsv) throws ConfigurationException {
    String dataset = dsv.getDataset();
    String iname = dsv.getInternalName();

    if (cacheUpToDate(dataset, iname))
      lazyLoadWithCache(dsv);
    else
      lazyLoadWithDatabase(dsv);
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
        dataSource.getJdbcDriverClassName(),
        adaptorName);
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
    return getDatasetViewsByDataset(dataset).length > 0;
  }

  /**
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDataset(java.lang.String)
   */
  public DatasetView[] getDatasetViewsByDataset(String dataset) throws ConfigurationException {

    ArrayList l = new ArrayList();

    for (int i = 0, n = dsviews.size(); i < n; i++) {
      DatasetView view = (DatasetView) dsviews.get(i);
      if (view.getDataset().equals(dataset)) {
        l.add(new DatasetView(view)); //return copy of datasetview, so that lazyLoad doesnt expand reference to original
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

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDatasetDisplayName(java.lang.String, java.lang.String)
   */
  public DatasetView getDatasetViewByDatasetDisplayName(String dataset, String displayName)
    throws ConfigurationException {
    DatasetView view = null;
    for (int i = 0; i < dsviews.size(); ++i) {

      DatasetView dsv = (DatasetView) dsviews.get(i);
      if (StringUtil.compare(dataset, dsv.getDataset()) == 0
        && StringUtil.compare(displayName, dsv.getDisplayName()) == 0)
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
    return (String[]) datasetNameMap.keySet().toArray(new String[datasetNameMap.size()]);
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
    DatasetView[] views = getDatasetViewsByDataset(dataset);

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
    DatasetView[] views = getDatasetViewsByDataset(dataset);

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

  /**
   * @return
   */
  public DetailedDataSource getDataSource() {
    return dataSource;
  }
}
