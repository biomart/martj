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
 * DSConfigAdaptor implimentation that retrieves DatasetConfig objects from
 * a Mart Database.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSConfigAdaptor implements MultiDSConfigAdaptor, Comparable {

  //each dataset will have 2 name maps, and a Set of DatasetConfig objects associated with it in an ArrayList
  private final int INAME_INDEX = 0;
  private final int DNAME_INDEX = 1;
  private final int CONFIG_INDEX = 2;

  private final String DIGESTKEY = "MD5";
  private final String XMLKEY = "XML";
  Preferences xmlCache = null;

  private String dbpassword;
  private Logger logger = Logger.getLogger(DatabaseDSConfigAdaptor.class.getName());
  private List dsconfigs = new ArrayList();
  private HashMap datasetNameMap = new HashMap();

  private final DetailedDataSource dataSource;

  private final String user;
  private final int hashcode;
  private String adaptorName = null;

  private boolean clearCache = false; //developer hack to clear the cache
  //will be replaced soon with user supported clearing

  /**
   * Constructor for a DatabaseDSConfigAdaptor
   * @param ds -- DataSource for Mart RDBMS
   * @param user -- user for RDBMS connection, AND meta_configuration_user table
   * @throws ConfigurationException if DataSource or user is null
   */
  public DatabaseDSConfigAdaptor(DetailedDataSource ds, String user) throws ConfigurationException {
    if (ds == null || user == null)
      throw new ConfigurationException("DatabaseDSConfigAdaptor Objects must be instantiated with a DataSource and User\n");

    this.user = user;
    dataSource = ds;
    String host = ds.getHost();
    String port = ds.getPort();
    String databaseName = ds.getDatabaseName();

    adaptorName = ds.getName();

    try {
      //set up the preferences node with the datasource information as the root node
      if (clearCache) {
        BigPreferences.userNodeForPackage(DatabaseDSConfigAdaptor.class).node(adaptorName).node(user).removeNode();
        BigPreferences.userNodeForPackage(DatabaseDSConfigAdaptor.class).flush();
      }

      xmlCache = BigPreferences.userNodeForPackage(DatabaseDSConfigAdaptor.class).node(adaptorName).node(user);
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
   * @see org.ensembl.mart.lib.config.DatabaseDSConfigAdaptor#getMartLocations
   */
  public void setDatabasePassword(String password) {
    dbpassword = password;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigs()
   */
  public DatasetConfig[] getDatasetConfigs() throws ConfigurationException {
    DatasetConfig[] ret = new DatasetConfig[dsconfigs.size()];
    
    for (int i = 0, n = dsconfigs.size(); i < n; i++) {
      DatasetConfig dsvorig = (DatasetConfig) dsconfigs.get(i);
      ret[i] = new DatasetConfig(dsvorig); //return copy of datasetconfig, so that lazyLoad doesnt expand reference to original
    }
    return ret;
  }

  public void addDatasetConfig(DatasetConfig dsv) throws ConfigurationException {
    if (!(datasetNameMap.containsKey(dsv.getDataset()))) {
      dsv.setDSConfigAdaptor(this);
      dsconfigs.add(dsv); //add to the global dsconfigs list

      HashMap inameMap = new HashMap();
      HashMap dnameMap = new HashMap();
      ArrayList configs = new ArrayList();

      inameMap.put(dsv.getInternalName(), dsv);
      dnameMap.put(dsv.getDisplayName(), dsv);
      configs.add(dsv);

      Vector maps = new Vector();
      maps.add(INAME_INDEX, inameMap);
      maps.add(DNAME_INDEX, dnameMap);
      maps.add(CONFIG_INDEX, configs);

      datasetNameMap.put(dsv.getDataset(), maps);
    } else {
      Vector maps = (Vector) datasetNameMap.get(dsv.getDataset());
      HashMap inameMap = (HashMap) maps.get(INAME_INDEX);
      HashMap dnameMap = (HashMap) maps.get(DNAME_INDEX);
      ArrayList configs = (ArrayList) maps.get(CONFIG_INDEX);

      if (!(inameMap.containsKey(dsv.getInternalName()) && dnameMap.containsKey(dsv.getDisplayName()))) {
        dsv.setDSConfigAdaptor(this);
        dsconfigs.add(dsv); //add to the global dsconfigs list

        inameMap.put(dsv.getInternalName(), dsv);
        dnameMap.put(dsv.getDisplayName(), dsv);
        configs.add(dsv);

        maps.remove(INAME_INDEX);
        maps.add(INAME_INDEX, inameMap);
        maps.remove(DNAME_INDEX);
        maps.add(DNAME_INDEX, dnameMap);
        maps.remove(CONFIG_INDEX);
        maps.add(CONFIG_INDEX, configs);

        datasetNameMap.put(dsv.getDataset(), maps);
      }
    }
  }

  //  private void updateXMLCache() throws ConfigurationException {
  //    String iname = null;
  //    String dataset = null;
  //
  //    try {
  //      for (Iterator iter = dsconfigs.iterator(); iter.hasNext();) {
  //        DatasetConfig dsv = (DatasetConfig) iter.next();
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

  private void addToXMLCache(DatasetConfig dsv) throws ConfigurationException {
    String iname = dsv.getInternalName();
    String dataset = dsv.getDataset();

    if (logger.isLoggable(Level.INFO))
      logger.info("adding DatasetConfig " + dataset + " " + iname + " to cache\n");
    byte[] xml = DatasetConfigXMLUtils.DatasetConfigToByteArray(dsv);
    byte[] digest = dsv.getMessageDigest();
    
    if (logger.isLoggable(Level.INFO))
      logger.info("Storing " + xml.length + " bytes of XML to cache\n");
      
    if (digest == null)
      throw new ConfigurationException("Recieved DatasetConfig to lazyLoad without a stored MessageDigest\n");

    xmlCache.node(dataset).node(iname).putByteArray(DIGESTKEY, digest);
    xmlCache.node(dataset).node(iname).putByteArray(XMLKEY, xml);

    try {
      xmlCache.flush();
    } catch (BackingStoreException e) {
      throw new ConfigurationException(
        "Caught BackingStoreException adding new DatasetConfig to preferences node "
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
        "Caught BackingStoreException removing DatasetConfig from preferences node "
          + dataset
          + " internalName "
          + iname
          + " "
          + e.getMessage()
          + "\nAssuming it doesnt exist\n");
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.MultiDSConfigAdaptor#removeDatasetConfig(org.ensembl.mart.lib.config.DatasetConfig)
   */
  public boolean removeDatasetConfig(DatasetConfig dsv) throws ConfigurationException {
    if (datasetNameMap.containsKey(dsv.getDataset())) {
      Vector maps = (Vector) datasetNameMap.get(dsv.getDataset());
      HashMap inameMap = (HashMap) maps.get(INAME_INDEX);
      HashMap dnameMap = (HashMap) maps.get(DNAME_INDEX);
      ArrayList configs = (ArrayList) maps.get(CONFIG_INDEX);

      if (inameMap.containsKey(dsv.getInternalName())) {
        datasetNameMap.remove(dsv.getDataset());
        inameMap.remove(dsv.getInternalName());
        dnameMap.remove(dsv.getDisplayName());
        dsconfigs.remove(dsv);
        configs.remove(dsv);
        dsv.setDSConfigAdaptor(null);
        removeFromXMLCache(dsv.getDataset(), dsv.getInternalName());

        //if this dataset is completely removed from the adaptor, make sure its keys reflect its removal
        if (configs.size() > 0) {
          maps.remove(INAME_INDEX);
          maps.remove(DNAME_INDEX);
          maps.remove(CONFIG_INDEX);
          maps.add(INAME_INDEX, inameMap);
          maps.add(DNAME_INDEX, dnameMap);
          maps.add(CONFIG_INDEX, configs);
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
      DatabaseDatasetConfigUtils.getDSConfigMessageDigestByDatasetInternalName(dataSource, user, dataset, iname);
    byte[] oDigest = ((DatasetConfig) inameMap.get(iname)).getMessageDigest();

    if (!MessageDigest.isEqual(oDigest, nDigest)) {

      if (logger.isLoggable(Level.INFO))
        logger.info("Needs update\n");

      removeDatasetConfig((DatasetConfig) inameMap.get(iname));
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
        DatabaseDatasetConfigUtils.getDSConfigMessageDigestByDatasetInternalName(dataSource, user, dataset, iname);
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
          
        DatasetConfig newDSV = DatasetConfigXMLUtils.ByteArrayToDatasetConfig(cachedXML);
        newDSV.setMessageDigest(cacheDigest);
        addDatasetConfig(newDSV);
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

    DatasetConfig newDSV = DatabaseDatasetConfigUtils.getDatasetConfigByDatasetInternalName(dataSource, user, dataset, iname);
    addDatasetConfig(newDSV);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#update()
   */
  public void update() throws ConfigurationException {
    String[] datasets = DatabaseDatasetConfigUtils.getAllDatasetNames(dataSource, user);
    for (int i = 0, n = datasets.length; i < n; i++) {
      String dataset = datasets[i];
      String[] inms = DatabaseDatasetConfigUtils.getAllInternalNamesForDataset(dataSource, user, dataset);

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
        //dataset is loaded, check for update of its datasetconfig
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
        //not already loaded, check for its datasetconfigs in cache
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
            //load datasetconfig from database
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
   * Allows client to store a single DatasetConfig object as a DatasetConfig.dtd compliant XML document into a Mart Database.
   * Client can choose whether to compress (GZIP) the resulting XML before it is stored in the Database.
   * @param ds -- DataSource of the Mart Database where the DatasetConfig.dtd compliant XML is to be stored.
   * @param user -- RDBMS user for meta_configuration_[user] table to store the document.  If null, or if meta_configuration_[user] does not exist, meta_configuration will be the target of the document.
   * @param dsv -- DatasetConfig object to store
   * @param compress -- if true, the resulting XML will be gzip compressed before storing into the table.
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static void storeDatasetConfig(DetailedDataSource ds, String user, DatasetConfig dsv, boolean compress)
    throws ConfigurationException {
    DatabaseDatasetConfigUtils.storeConfiguration(
      ds,
      user,
      dsv.getInternalName(),
      dsv.getDisplayName(),
      dsv.getDataset(),
      dsv.getDescription(),
      DatasetConfigXMLUtils.DatasetConfigToDocument(dsv),
      compress);
  }

  private void lazyLoadWithDatabase(DatasetConfig dsv) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info("lazy loading from database\n");

    DatasetConfigXMLUtils.LoadDatasetConfigWithDocument(
      dsv,
      DatabaseDatasetConfigUtils.getDatasetConfigDocumentByDatasetInternalName(
        dataSource,
        user,
        dsv.getDataset(),
        dsv.getInternalName()));

    //cache this DatasetConfig, as, for some reason, it is needing to be cached
    removeFromXMLCache(dsv.getDataset(), dsv.getInternalName());
    addToXMLCache(dsv);
  }

  private void lazyLoadWithCache(DatasetConfig dsv) throws ConfigurationException {
    if (logger.isLoggable(Level.INFO))
      logger.info("Attempting to lazy load with cache\n");

    byte[] cachedXML = xmlCache.node(dsv.getDataset()).node(dsv.getInternalName()).getByteArray(XMLKEY, null);
    //  should return a null if it cant load for some reason

    if (cachedXML != null) {
      if (logger.isLoggable(Level.INFO))
        logger.info("lazyLoading from non null cache of length " +  cachedXML.length + "\n");

      DatasetConfigXMLUtils.LoadDatasetConfigWithDocument(dsv, DatasetConfigXMLUtils.ByteArrayToDocument(cachedXML));
    } else {
      if (logger.isLoggable(Level.INFO))
        logger.info("preferences returned null byte[], loading from database\n");
      lazyLoadWithDatabase(dsv);
    }
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#lazyLoad(org.ensembl.mart.lib.config.DatasetConfig)
   */
  public void lazyLoad(DatasetConfig dsv) throws ConfigurationException {
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
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getMartLocations()
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
   * Allows Equality Comparisons manipulation of DSConfigAdaptor objects.  Although
   * any DSConfigAdaptor object can be compared with any other DSConfigAdaptor object, to provide
   * consistency with the compareTo method, in practice, it is almost impossible for different DSVIewAdaptor
   * implimentations to equal.
   */
  public boolean equals(Object o) {
    return o instanceof DSConfigAdaptor && hashCode() == o.hashCode();
  }

  /**
   * Calculation is purely based on the DataSource and user hashCode.  Any
   * DatabaseDSConfigAdaptor based on these two inputs should represent the same
   * collection of DatasetConfig objects.
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
    return hashcode - ((DSConfigAdaptor) o).hashCode();
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

    for (int i = 0, n = dsconfigs.size(); i < n; i++) {
      DatasetConfig config = (DatasetConfig) dsconfigs.get(i);
      if (config.getDataset().equals(dataset)) {
        l.add(new DatasetConfig(config)); //return copy of datasetconfig, so that lazyLoad doesnt expand reference to original
      }
    }

    return (DatasetConfig[]) l.toArray(new DatasetConfig[l.size()]);

  }

  /**
   * @return datasource.toString() if datasource is not null, otherwise 
   * "No Database".
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDisplayName()
   */
  public String getDisplayName() {

    return (dataSource == null) ? "No Database" : dataSource.toString();
  }

  /**
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigByDatasetInternalName(java.lang.String, java.lang.String)
   */
  public DatasetConfig getDatasetConfigByDatasetInternalName(String dataset, String internalName)
    throws ConfigurationException {

    DatasetConfig config = null;
    for (int i = 0; i < dsconfigs.size(); ++i) {

      DatasetConfig dsv = (DatasetConfig) dsconfigs.get(i);
      if (StringUtil.compare(dataset, dsv.getDataset()) == 0
        && StringUtil.compare(internalName, dsv.getInternalName()) == 0)
        config = dsv;
    }

    return config;
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigByDatasetDisplayName(java.lang.String, java.lang.String)
   */
  public DatasetConfig getDatasetConfigByDatasetDisplayName(String dataset, String displayName)
    throws ConfigurationException {
    DatasetConfig config = null;
    for (int i = 0; i < dsconfigs.size(); ++i) {

      DatasetConfig dsv = (DatasetConfig) dsconfigs.get(i);
      if (StringUtil.compare(dataset, dsv.getDataset()) == 0
        && StringUtil.compare(displayName, dsv.getDisplayName()) == 0)
        config = dsv;
    }

    return config;
  }

  /**
   * DatabaseDSConfigAdaptor objects do not contain child adaptors
   * @return null
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getAdaptorByName(java.lang.String)
   */
  public DSConfigAdaptor getAdaptorByName(String adaptorName) throws ConfigurationException {
    // DatabaseDSConfigAdaptor objects do not contain child adaptors
    return null;
  }

  /**
   * DatabaseDSConfigAdaptor objects do not contain child adaptors
   * return empty String[]
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getAdaptorNames()
   */
  public String[] getAdaptorNames() throws ConfigurationException {
    return new String[0];
  }

  /**
   * DatabaseDSConfigAdaptor objects do not contain child adaptors
   * return empty DSConfigAdaptor[]
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getAdaptors()
   */
  public DSConfigAdaptor[] getAdaptors() throws ConfigurationException {
    // DatabaseDSConfigAdaptor objects do not contain child adaptors
    return new DSConfigAdaptor[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetNames()
   */
  public String[] getDatasetNames() throws ConfigurationException {
    return (String[]) datasetNameMap.keySet().toArray(new String[datasetNameMap.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetNames(java.lang.String)
   */
  public String[] getDatasetNames(String adaptorName) throws ConfigurationException {
    if (adaptorName.equals(this.adaptorName))
      return getDatasetNames();
    else
      return new String[0];
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigDisplayNamesByDataset(java.lang.String)
   */
  public String[] getDatasetConfigDisplayNamesByDataset(String dataset) throws ConfigurationException {
    List names = new ArrayList();
    DatasetConfig[] configs = getDatasetConfigsByDataset(dataset);

    for (int i = 0, n = configs.length; i < n; i++) {
      DatasetConfig dsv = (DatasetConfig) configs[i];

      if (dsv.getDataset().equals(dataset))
        names.add(dsv.getDisplayName());
    }

    return (String[]) names.toArray(new String[names.size()]);
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#getDatasetConfigInternalNamesByDataset(java.lang.String)
   */
  public String[] getDatasetConfigInternalNamesByDataset(String dataset) throws ConfigurationException {
    List names = new ArrayList();
    DatasetConfig[] configs = getDatasetConfigsByDataset(dataset);

    for (int i = 0, n = configs.length; i < n; i++) {
      DatasetConfig dsv = (DatasetConfig) configs[i];

      if (dsv.getDataset().equals(dataset))
        names.add(dsv.getInternalName());
    }

    return (String[]) names.toArray(new String[names.size()]);
  }

  /**
   * return name, defaults to "user@host:port/databaseName"
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
   * DatabaseDSConfigAdaptor objects do not contain child adaptors
   * return false
   * @see org.ensembl.mart.lib.config.DSConfigAdaptor#supportsAdaptor(java.lang.String)
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
