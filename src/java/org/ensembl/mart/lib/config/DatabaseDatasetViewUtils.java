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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.util.ColumnDescription;
import org.ensembl.mart.util.TableDescription;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDatasetViewUtils {

  private static final String DIGESTTYPE = "MD5";

  private static final String BASEMETATABLE = "_meta_DatasetView"; // append user if necessary

  /*
   * _meta_DatasetView<_username>
   * -----------------------
   * internalName   varchar(100)
   * displayName    varchar(100)
   * dataset        varchar(100)
   * description    varchar(200)
   * xml            longblob
   * compressed_xml longblob
   * MessageDigest  blob
   */  


  private static final String GETALLNAMESQL =
    "select internalname, displayName, dataset, description, MessageDigest from ";
  private static final String GETALLDATASETSQL = "select dataset from ";
  private static final String GETINTNAMESQL = "select internalName from "; //append table after user test
  private static final String GETDNAMESQL = "select displayName from "; //append table after user test
  private static final String GETANYNAMESWHEREDATASET = " where dataset = ?";
  private static final String GETANYNAMESWHEREDNAME = " where displayName = ? and dataset = ?";
  // append to GETINTNAMESQL when wanting internalName by displayName
  private static final String GETANYNAMESWHERINAME = " where internalName = ? and dataset = ?";
  private static final String GETDOCBYDNAMESELECT = "select xml, compressed_xml from "; //append table after user test
  private static final String GETDOCBYDNAMEWHERE = " where displayName = ?";
  private static final String GETDOCBYINAMESELECT = "select xml, compressed_xml from "; //append table after user test
  private static final String GETDOCBYINAMEWHERE = " where internalName = ? and dataset = ?";
  private static final String GETDIGBYNAMESELECT = "select MessageDigest from ";
  private static final String GETDIGBYINAMEWHERE = " where internalName = ? and dataset = ?";
  private static final String GETDIGBYDNAMEWHERE = " where displayName = ? and dataset = ?";
  private static final String EXISTSELECT = "select count(*) from "; //append table after user test
  private static final String EXISTWHERE = " where internalName = ? and displayName = ? and dataset = ?";
  private static final String DELETEOLDXML = "delete from "; //append table after user test
  private static final String DELETEOLDXMLWHERE = " where internalName = ? and displayName = ? and dataset = ?";
  private static final String INSERTXMLSQLA = "insert into "; //append table after user test
  private static final String INSERTXMLSQLB =
    " (internalName, displayName, dataset, description, xml, MessageDigest) values (?, ?, ?, ?, ?, ?)";
  private static final String INSERTCOMPRESSEDXMLA = "insert into "; //append table after user test
  private static final String INSERTCOMPRESSEDXMLB =
    " (internalName, displayName, dataset, description, compressed_xml, MessageDigest) values (?, ?, ?, ?, ?, ?)";
  private static final String MAINTABLESUFFIX = "main";
  private static final String DIMENSIONTABLESUFFIX = "dm";

  private static final String DOESNTEXISTSUFFIX = "**DOES_NOT_EXIST**";

  private static String DEFAULTLEGALQUALIFIERS = "in,=,>,<,>=,<=";
  private static String DEFAULTQUALIFIER = "in";
  private static String DEFAULTTYPE = "list";
  private static final String DEFAULTGROUP = "defaultGroup";
  private static final String DEFAULTPAGE = "defaultPage";

  private static Logger logger = Logger.getLogger(DatabaseDatasetViewUtils.class.getName());

  /**
   * Verify if a _meta_DatasetView_[user] table exists.  Returns false if user is null, or
   * if the table does not exist. 
   * @param dsource - DetailedDataSource containing connection to Mart Database
   * @param user - user to query
   * @return true if _meta_DatasetView_[user] exists, false otherwise
   * @throws ConfigurationException for SQLExceptions
   */
  public static boolean DSViewUserTableExists(DetailedDataSource dsource, String user) throws ConfigurationException {
    if (user == null)
      return false;
    else {
      String table = BASEMETATABLE + "_" + user;
      String tcheck = null;

      try {
        Connection conn = dsource.getConnection();
        String catalog = conn.getCatalog();

        ResultSet vr = conn.getMetaData().getTables(catalog, null, table, null);

        //expect at most one result, if no results, tcheck will remain null
        if (vr.next())
          tcheck = vr.getString(3);

        vr.close();
        conn.close();
      } catch (SQLException e) {
        throw new ConfigurationException("Recieved SQL exception attempting to verify table " + table + "\n", e);
      }

      if (tcheck == null) {
          logger.info("Table " + table + " does not exist, using " + BASEMETATABLE + " instead\n");
        return false;
      }

      if (!(table.equals(tcheck))) {
        if (logger.isLoggable(Level.WARNING))
          logger.warning("Returned Wrong table for verifyTable: wanted " + table + " got " + tcheck + "\n");
        return false;
      }
      return true;
    }
  }

  /**
   * Determine if _meta_DatasetView exists in a Mart Database defined by the given DetailedDataSource.
   * @param dsource -- DetailedDataSource for the Mart Database being querried.
   * @return true if _meta_DatasetView exists, false if it does not exist
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static boolean BaseDSViewTableExists(DetailedDataSource dsource) throws ConfigurationException {
    String table = BASEMETATABLE;
    String tcheck = null;

    try {
      Connection conn = dsource.getConnection();
      String catalog = conn.getCatalog();

      ResultSet vr = conn.getMetaData().getTables(catalog, null, table, null);
      //expect at most one result, if no results, tcheck will remain null
      if (vr.next())
        tcheck = vr.getString(3);

      vr.close();
      conn.close();
    } catch (SQLException e) {
      throw new ConfigurationException("Recieved SQL exception attempting to verify table " + table + "\n", e);
    }

    if (tcheck == null) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning("BASE META TABLE " + table + " does not exist\n");
      return false;
    }

    if (!(table.equals(tcheck))) {
      if (logger.isLoggable(Level.WARNING))
        logger.warning("Returned Wrong table for verifyTable: wanted " + table + " got " + tcheck + "\n");
      return false;
    }
    return true;
  }

  /**
   * Store a DatesetView.dtd compliant (compressed or uncompressed) XML Document in the Mart Database with a given internalName and displayName.  
   * If user is not null and _meta_DatsetView_[user] exists, this table is the target, otherwise, _meta_DatasetView is the target.
   * Along with the internalName and displayName of the XML, an MD5 messageDigest of the xml is computed, and stored as well. 
   * @param dsource -- DetailedDataSource object containing connection information for the Mart Database
   * @param user -- Specific User to look for _meta_DatasetView_[user] table, if null, or non-existent, uses _meta_DatasetView
   * @param internalName -- internalName of the DatasetViewXML being stored.
   * @param displayName -- displayName of the DatasetView XML being stored.
   * @param dataset -- dataset of the DatasetView XML being stored
   * @param doc - JDOM Document object representing the XML for the DatasetView   
   * @param compress -- if true, the XML is compressed using GZIP.
   * @throws ConfigurationException when no _meta_DatasetView table exists, and for all underlying Exceptions
   */
  public static void storeConfiguration(
    DetailedDataSource dsource,
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc,
    boolean compress)
    throws ConfigurationException {

    int rowsupdated = 0;

    if (compress)
      rowsupdated = storeCompressedXML(dsource, user, internalName, displayName, dataset, description, doc);
    else
      rowsupdated = storeUncompressedXML(dsource, user, internalName, displayName, dataset, description, doc);

    if (rowsupdated < 1)
      if (logger.isLoggable(Level.WARNING))
        logger.warning("Warning, xml for " + internalName + ", " + displayName + " not stored"); //throw an exception?	
  }

  private static int storeUncompressedXML(
    DetailedDataSource dsource,
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String insertSQL = INSERTXMLSQLA + metatable + INSERTXMLSQLB;

      if (logger.isLoggable(Level.INFO))
        logger.info("\ninserting with SQL " + insertSQL + "\n");

      Connection conn = dsource.getConnection();
      MessageDigest md5digest = MessageDigest.getInstance(DIGESTTYPE);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      DigestOutputStream dout = new DigestOutputStream(bout, md5digest);
      XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

      xout.output(doc, dout);

      byte[] xml = bout.toByteArray();
      byte[] md5 = md5digest.digest();

      bout.close();
      dout.close();

      int rowstodelete = getDSViewEntryCountFor(dsource, metatable, dataset, internalName, displayName);

      if (rowstodelete > 0)
        DeleteOldDSViewEntriesFor(dsource, metatable, dataset, internalName, displayName);

      PreparedStatement ps = conn.prepareStatement(insertSQL);
      ps.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);
      ps.setString(4, description);
      ps.setBytes(5, xml);
      ps.setBytes(6, md5);

      int ret = ps.executeUpdate();
      ps.close();
      conn.close();

      return ret;
    } catch (IOException e) {
      throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage(), e);
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQLException updating xml for " + internalName + ", " + displayName + ": " + e.getMessage(),
        e);
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException(
        "Caught NoSuchAlgorithmException updating xml for " + internalName + ", " + displayName + ": " + e.getMessage(),
        e);
    }
  }

  private static int storeCompressedXML(
    DetailedDataSource dsource,
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String insertSQL = INSERTCOMPRESSEDXMLA + metatable + INSERTCOMPRESSEDXMLB;

      if (logger.isLoggable(Level.INFO))
        logger.info("\ninserting with SQL " + insertSQL + "\n");

      Connection conn = dsource.getConnection();
      MessageDigest md5digest = MessageDigest.getInstance(DIGESTTYPE);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      GZIPOutputStream gout = new GZIPOutputStream(bout);
      DigestOutputStream out = new DigestOutputStream(gout, md5digest);
      // should calculate digest on unzipped data, eg, bytes before they are sent to gout.write

      XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

      xout.output(doc, out);
      gout.finish();

      byte[] xml = bout.toByteArray();
      byte[] md5 = md5digest.digest();
      bout.close();
      gout.close();
      out.close();

      int rowstodelete = getDSViewEntryCountFor(dsource, metatable, dataset, internalName, displayName);

      if (rowstodelete > 0)
        DeleteOldDSViewEntriesFor(dsource, metatable, dataset, internalName, displayName);

      PreparedStatement ps = conn.prepareStatement(insertSQL);
      ps.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);
      ps.setString(4, description);
      ps.setBytes(5, xml);
      ps.setBytes(6, md5);

      int ret = ps.executeUpdate();
      ps.close();
      conn.close();

      return ret;
    } catch (IOException e) {
      throw new ConfigurationException("Caught IOException writing out xml to OutputStream: " + e.getMessage());
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQLException updating xml for " + internalName + ", " + displayName + ": " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new ConfigurationException(
        "Caught NoSuchAlgorithmException updating xml for " + internalName + ", " + displayName + ": " + e.getMessage(),
        e);
    }
  }

  /**
   * Returns all dataset names from the _meta_DatasetView table for the given user.
   * @param ds -- DetailedDataSource for mart database
   * @param user -- user for _meta_DatasetView table, if _meta_DatasetView_user does not exist, _meta_DatasetView is attempted.
   * @return String[] dataset names
   * @throws ConfigurationException when valid _meta_DatasetView table does not exist, and for all underlying SQL Exceptions
   */
  public static String[] getAllDatasetNames(DetailedDataSource ds, String user) throws ConfigurationException {
    SortedSet names = new TreeSet();
    String metatable = getDSViewTableFor(ds, user);
    String sql = GETALLDATASETSQL + metatable;
    
    if (logger.isLoggable(Level.INFO))
      logger.info("Getting all dataset names with sql: " + sql + "\n");

    try {
      Connection conn = ds.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      
      ResultSet rs = ps.executeQuery();

      while (rs.next()) {
        String name = rs.getString(1);
        names.add(name);
      }
      rs.close();
      conn.close();
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQLException during attempt to fetch InternalNames for " + sql + "\n", e);
    }
    
    String[] ret = new String[names.size()];
    names.toArray(ret);
    return ret;
  }
  
  /**
   * Returns all of the internalNames for the given dataset, as stored in the _meta_DatasetView table for
   * the Mart Database for the given user.
   * @param ds -- DetailedDataSource for Mart database
   * @param user -- user for _meta_DatasetView table, if _meta_DatasetView_user does not exist, _meta_DatasetView is attempted.
   * @param dataset -- dataset for which internalNames are requested
   * @return String[] containing all of the internalNames for the requested dataset.
   * @throws ConfigurationException when valid _meta_DatasetView tables do not exist, and for all underlying Exceptons.
   */
  public static String[] getAllInternalNamesForDataset(DetailedDataSource ds, String user, String dataset) throws ConfigurationException {
    List names = new ArrayList();
    String metatable = getDSViewTableFor(ds, user);
    String sql = GETINTNAMESQL + metatable + GETANYNAMESWHEREDATASET;

    if (logger.isLoggable(Level.INFO))
      logger.info("Getting all InternalNames with sql: " + sql + "\n");

    try {
      Connection conn = ds.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, dataset);
      
      ResultSet rs = ps.executeQuery();

      while (rs.next()) {
        String name = rs.getString(1);
        if (!names.contains(name))
          names.add(name);
      }
      rs.close();
      conn.close();
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQLException during attempt to fetch InternalNames for " + sql + "\n", e);
    }

    String[] ret = new String[names.size()];
    names.toArray(ret);
    return ret;
  }

  /**
   * Returns all of the displayNames for the requested dataset, as stored in the _meta_DatasetView table for
   * the Mart Database for the given user.
   * @param ds -- DetailedDataSource for Mart database
   * @param user -- user for _meta_DatasetView table, if _meta_DatasetView_user does not exist, _meta_DatasetView is attempted.
   * @param dataset -- dataset for which displayNames are requested
   * @return String[] containing all of the displayNames for the requested dataset
   * @throws ConfigurationException when valid _meta_DatasetView tables do not exist, and for all underlying Exceptons.
   */
  public static String[] getAllDisplayNamesForDataset(DetailedDataSource ds, String user, String dataset) throws ConfigurationException {
    List names = new ArrayList();
    String metatable = getDSViewTableFor(ds, user);
    String sql = GETDNAMESQL + metatable+ GETANYNAMESWHEREDATASET;

    if (logger.isLoggable(Level.INFO))
      logger.info("Getting all displayNames with sql: " + sql + "\n");

    try {
      Connection conn = ds.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, dataset);
      
      ResultSet rs = ps.executeQuery();

      while (rs.next()) {
        String name = rs.getString(1);
        if (!names.contains(name))
          names.add(name);
      }
      rs.close();
      conn.close();
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQLException during attempt to fetch InternalNames for " + sql + "\n", e);
    }

    String[] ret = new String[names.size()];
    names.toArray(ret);
    return ret;
  }

  /**
   * Returns a DatasetView object from the Mart Database using a supplied DetailedDataSource for a given user, defined with the
   * given internalName and dataset.
   * @param dsource -- DetailedDataSource object containing connection information for the Mart Database
   * @param user -- Specific User to look for _meta_DatasetView_[user] table, if null, or non-existent, uses _meta_DatasetView
   * @param dataset -- dataset for which DatasetView is requested
   * @param internalName -- internalName of desired DatasetView object
   * @return DatasetView defined by given internalName
   * @throws ConfigurationException when valid _meta_DatasetView tables are absent, and for all underlying Exceptions
   */
  public static DatasetView getDatasetViewByDatasetInternalName(DetailedDataSource dsource, String user, String dataset, String internalName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETALLNAMESQL + metatable + GETANYNAMESWHERINAME;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get displayName for internalName " + internalName + " and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, internalName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      String iname = rs.getString(1);
      String dname = rs.getString(2);
      String dprefix = rs.getString(3);
      String description = rs.getString(4);
      byte[] digest = rs.getBytes(5);
      rs.close();
      conn.close();

      DatasetView dsv = new DatasetView(iname, dname, dprefix, description);
      dsv.setMessageDigest(digest);
      return dsv;
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
    }
  }

  /**
   * Returns a DatasetView JDOM Document from the Mart Database using a supplied DetailedDataSource for a given user, defined with the
   * given internalName and dataset.
   * @param dsource -- DetailedDataSource object containing connection information for the Mart Database
   * @param user -- Specific User to look for _meta_DatasetView_[user] table, if null, or non-existent, uses _meta_DatasetView
   * @param dataset -- dataset for which DatasetView document is requested
   * @param internalName -- internalName of desired DatasetView document
   * @return DatasetView JDOM Document defined by given displayName and dataset
   * @throws ConfigurationException when valid _meta_DatasetView tables are absent, and for all underlying Exceptions
   */
  public static Document getDatasetViewDocumentByDatasetInternalName(DetailedDataSource dsource, String user, String dataset, String internalName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETDOCBYINAMESELECT + metatable + GETDOCBYINAMEWHERE;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get DatasetView for internalName " + internalName + "and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, internalName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      byte[] stream = rs.getBytes(1);
      byte[] cstream = rs.getBytes(2);
      rs.close();
      conn.close();

      InputStream rstream = null;
      if (cstream != null)
        rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
      else
        rstream = new ByteArrayInputStream(stream);

      return DatasetViewXMLUtils.XMLStreamToDocument(rstream, false);
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQL Exception during fetch of requested DatasetView: " + e.getMessage(),
        e);
    } catch (IOException e) {
      throw new ConfigurationException("Caught IOException during fetch of requested DatasetView: " + e.getMessage(), e);
    }
  }

  /**
   * Returns a DatasetView object from the Mart Database using a supplied DetailedDataSource for a given user, defined with the
   * given dataset and displayName
   * @param dsource -- DetailedDataSource object containing connection information for the Mart Database
   * @param dataset -- dataset for which DatsetView is requested
   * @param user -- Specific User to look for _meta_DatasetView_[user] table, if null, or non-existent, uses _meta_DatasetView
   * @param displayName -- String displayName for requested DatasetView
   * @return DatasetView with given displayName and dataset
   * @throws ConfigurationException when valid _meta_DatasetView tables are absent, and for all underlying Exceptions
   */
  public static DatasetView getDatasetViewByDatasetDisplayName(DetailedDataSource dsource, String user, String dataset, String displayName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETALLNAMESQL + metatable + GETANYNAMESWHEREDNAME;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get DatasetView for displayName " + displayName + "and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, displayName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      String iname = rs.getString(1);
      String dname = rs.getString(2);
      String dprefix = rs.getString(3);
      String description = rs.getString(4);
      byte[] digest = rs.getBytes(5);
      rs.close();
      conn.close();

      DatasetView dsv = new DatasetView(iname, dname, dprefix, description);
      dsv.setMessageDigest(digest);
      return dsv;
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
    }
  }

  public static Document getDatasetViewDocumentByDatasetDisplayName(DetailedDataSource dsource, String user, String dataset, String displayName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETDOCBYDNAMESELECT + metatable + GETDOCBYDNAMEWHERE;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get DatasetView Document for displayName " + displayName + " and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, displayName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      byte[] stream = rs.getBytes(1);
      byte[] cstream = rs.getBytes(2);

      rs.close();
      conn.close();

      InputStream rstream = null;
      if (cstream != null)
        rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
      else
        rstream = new ByteArrayInputStream(stream);

      return DatasetViewXMLUtils.XMLStreamToDocument(rstream, false);
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQL Exception during fetch of requested DatasetView: " + e.getMessage(),
        e);
    } catch (IOException e) {
      throw new ConfigurationException("Caught IOException during fetch of requested DatasetView: " + e.getMessage(), e);
    } catch (ConfigurationException e) {
      throw e;
    }
  }

  /**
   * Get a message digest for a given DatasetView, given by dataset and internalName
   * @param dsource -- connection to mart database
   * @param user -- user for _meta_DatasetView_[user] table, if null, _meta_DatasetView is attempted
   * @param dataset -- dataset for which digest is requested
   * @param internalName -- internalName for DatasetView digest desired.
   * @return byte[] digest for given dataset and displayName
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static byte[] getDSViewMessageDigestByDatasetInternalName(DetailedDataSource dsource, String user, String dataset, String internalName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETDIGBYNAMESELECT + metatable + GETDIGBYINAMEWHERE;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get Digest for internalName " + internalName + " and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, internalName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      byte[] digest = rs.getBytes(1);
      rs.close();
      conn.close();

      return digest;
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
    }
  }

  /**
   * Get the displayName for a given dataset and internalName.
   * @param dsource -- connection to mart database
   * @param user -- user for _meta_DatasetView_[user] table, if null, _meta_DatasetView is attempted
   * @param dataset -- dataset for which displayName is requested
   * @param internalName -- internalName for DatasetView internalName desired.
   * @return String displayName for given dataset and internalName
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static String getDSViewDisplayNameByDatasetInternalName(DetailedDataSource dsource, String user, String dataset, String internalName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETDNAMESQL + metatable + GETANYNAMESWHERINAME;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get displayName for internalName " + internalName + "and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, internalName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      String dname = rs.getString(1);
      rs.close();
      conn.close();

      return dname;
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
    }
  }
  /**
   * Get a message digest for a given DatasetView, given by dataset and displayName
   * @param dsource -- connection to mart database
   * @param user -- user for _meta_DatasetView_[user] table, if null, _meta_DatasetView is attempted
   * @param dataset -- dataset for which digest is requested
   * @param displayName -- displayName for DatasetView digest desired.
   * @return byte[] digest for given displayName and dataset
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static byte[] getDSViewMessageDigestByDatasetDisplayName(DetailedDataSource dsource, String user, String dataset, String displayName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETDIGBYNAMESELECT + metatable + GETDIGBYDNAMEWHERE;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get Digest for displayName " + displayName + "and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, displayName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      byte[] digest = rs.getBytes(1);
      rs.close();
      conn.close();

      return digest;
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
    }
  }

  /**
   * Get the internalName for a given dataset and displayName.
   * @param dsource -- connection to mart database
   * @param user -- user for _meta_DatasetView_[user] table, if null, _meta_DatasetView is attempted
   * @param dataset -- dataset for which internalName is requested
   * @param displayName -- displayName for DatasetView internalName desired.
   * @return String internalName for given displayName and dataset
   * @throws ConfigurationException for all underlying Exceptions
   */
  public static String getDSViewInternalNameByDatasetDisplayName(DetailedDataSource dsource, String user, String dataset, String displayName)
    throws ConfigurationException {
    try {
      String metatable = getDSViewTableFor(dsource, user);
      String sql = GETINTNAMESQL + metatable + GETANYNAMESWHEREDNAME;

      if (logger.isLoggable(Level.INFO))
        logger.info("Using " + sql + " to get Digest for displayName " + displayName + "and dataset " + dataset + "\n");

      Connection conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setString(1, displayName);
      ps.setString(2, dataset);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        // will only get one result
        rs.close();
        conn.close();
        return null;
      }

      String iname = rs.getString(1);
      rs.close();
      conn.close();

      return iname;
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
    }
  }

  public static int getDSViewEntryCountFor(DetailedDataSource ds, String metatable, String dataset, String internalName, String displayName)
    throws ConfigurationException {
    String existSQL = EXISTSELECT + metatable + EXISTWHERE;
    if (logger.isLoggable(Level.INFO))
      logger.info("Getting DSViewEntryCount with SQL " + existSQL + "\n");

    int ret;
    try {
      Connection conn = ds.getConnection();
      PreparedStatement ps = conn.prepareStatement(existSQL);
      ps.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);

      ResultSet rs = ps.executeQuery();
      rs.next();
      ret = rs.getInt(1);
      rs.close();
      conn.close();
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQL exception attempting to determing count of rows for "
          + internalName
          + ", "
          + displayName
          + " from "
          + metatable
          + "\n");
    }

    return ret;
  }

  /**
   * Removes all records in a given metatable for the given dataset, internalName and displayName.  
   * Throws an error if the rows deleted do not equal the number of rows obtained using DatabaseDatasetViewAdaptor.getDSViewEntryCountFor(). 
   * @param dsrc - DetailedDataSource for Mart Database
   * @param metatable - _meta_DatasetView table to use to delete entries
   * @param dataset - dataset for DatasetView entries to delete from metatable
   * @param internalName - internalName of DatasetView entries to delete from metatable
   * @param displayName - displayName of DatasetView entries to delete from metatable
   * @throws ConfigurationException if number of rows to delete doesnt match number returned by getDSViewEntryCountFor()
   */
  public static void DeleteOldDSViewEntriesFor(
    DetailedDataSource dsrc,
    String metatable,
    String dataset,
    String internalName,
    String displayName)
    throws ConfigurationException {
    String deleteSQL = DELETEOLDXML + metatable + DELETEOLDXMLWHERE;

    int rowstodelete = getDSViewEntryCountFor(dsrc, metatable, dataset, internalName, displayName);
    if (logger.isLoggable(Level.INFO))
      logger.info("Deleting old DSViewEntries with SQL " + deleteSQL + "\n");

    int rowsdeleted;
    try {
      Connection conn = dsrc.getConnection();
      PreparedStatement ds = conn.prepareStatement(deleteSQL);
      ds.setString(1, internalName);
      ds.setString(2, displayName);
      ds.setString(3, dataset);

      rowsdeleted = ds.executeUpdate();
      ds.close();
      conn.close();
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQLException during delete of old Entries for "
          + internalName
          + ", "
          + displayName
          + " in table "
          + metatable
          + "\n");
    }

    if (!(rowsdeleted == rowstodelete))
      throw new ConfigurationException(
        "Did not delete old XML data rows for " + internalName + ", " + displayName + "\n");
  }

  /**
   * Get the correct DatasetView table for a given user in the Mart Database
   * stored in the given DetailedDataSource.
   * @param ds -- DetailedDataSource for Mart Database.
   * @param user -- user to retrieve a DatasetView table.  If user is null, or if _meta_DatasetView_[user] does not exist
   *                returns DatabaseDatasetViewUtils.BASEMETATABLE.
   * @return String meta table name
   * @throws ConfigurationException if both _meta_DatasetView_[user] and DatabaseDatasetViewUtils.BASEMETATABLE are absent, and for all underlying exceptions.
   */
  public static String getDSViewTableFor(DetailedDataSource ds, String user) throws ConfigurationException {
    String metatable = BASEMETATABLE;

    //override if user not null
    if (DSViewUserTableExists(ds, user))
      metatable += "_" + user;
    else {
      //if BASEMETATABLE doesnt exist, throw an exception
      if (!BaseDSViewTableExists(ds))
        throw new ConfigurationException(
          "Neither " + BASEMETATABLE + " or " + BASEMETATABLE + "_" + user + " exists in the Mart Database\n");
    }

    return metatable;
  }

  public static DatasetView getValidatedDatasetView(DetailedDataSource dsource, DatasetView dsv) throws SQLException {
    String schema = null;
    String catalog = null;

    ResultSet schemas = dsource.getConnection().getMetaData().getSchemas();
    while (schemas.next()) {
      schema = schemas.getString(1);
      catalog = schemas.getString(2);

      if (logger.isLoggable(Level.INFO))
        logger.info("schema: " + schema + " - catalog: " + catalog + "\n");
    }

    DatasetView validatedDatasetView = new DatasetView(dsv);

    boolean hasBrokenStars = false;
    String[] starbases = dsv.getStarBases();
    String[] validatedStars = new String[starbases.length];

    for (int i = 0, n = starbases.length; i < n; i++) {
      String starbase = starbases[i];
      String validatedStar = getValidatedStarBase(dsource, schema, catalog, starbase);

      if (!validatedStar.equals(starbase)) {
        hasBrokenStars = true;
        validatedDatasetView.removeStarBase(starbase);
      }

      validatedStars[i] = validatedStar;
    }

    if (hasBrokenStars) {
      validatedDatasetView.setStarsBroken();
      validatedDatasetView.addStarBases(validatedStars);
    }

    boolean hasBrokenPKeys = false;
    String[] pkeys = dsv.getPrimaryKeys();
    String[] validatedKeys = new String[pkeys.length];

    for (int i = 0, n = pkeys.length; i < n; i++) {
      String pkey = pkeys[i];
      String validatedKey = getValidatedPrimaryKey(dsource, schema, catalog, pkey);

      if (!validatedKey.equals(pkey)) {
        hasBrokenPKeys = true;
        validatedDatasetView.removePrimaryKey(pkey);
      }

      validatedKeys[i] = validatedKey;
    }

    if (hasBrokenPKeys) {
      validatedDatasetView.setPrimaryKeysBroken();
      validatedDatasetView.addPrimaryKeys(validatedKeys);
    }

    boolean hasBrokenDefaultFilters = false;
    DefaultFilter[] defaultFilters = dsv.getDefaultFilters();
    List brokenFilters = new ArrayList();

    //defaultFilter objects are not position sensitive
    for (int i = 0, n = defaultFilters.length; i < n; i++) {
      DefaultFilter dfilter = defaultFilters[i];
      DefaultFilter validatedDefaultFilter = getValidatedDefaultFilter(dsource, schema, catalog, dfilter);

      if (validatedDefaultFilter.isBroken()) {
        hasBrokenDefaultFilters = true;
        validatedDatasetView.removeDefaultFilter(dfilter);
        brokenFilters.add(validatedDefaultFilter);
      }
    }

    if (hasBrokenDefaultFilters) {
      validatedDatasetView.setDefaultFiltersBroken();

      for (int i = 0, n = brokenFilters.size(); i < n; i++) {
        DefaultFilter brokenFilter = (DefaultFilter) brokenFilters.get(i);
        validatedDatasetView.addDefaultFilter(brokenFilter);
      }
    }

    boolean hasBrokenOptions = false;
    Option[] options = dsv.getOptions();
    HashMap brokenOptions = new HashMap();

    for (int i = 0, n = options.length; i < n; i++) {
      Option validatedOption = getValidatedOption(dsource, schema, catalog, options[i]);

      if (validatedOption.isBroken()) {
        hasBrokenOptions = true;
        brokenOptions.put(new Integer(i), validatedOption);
      }
    }

    if (hasBrokenOptions) {
      validatedDatasetView.setOptionsBroken();

      for (Iterator iter = brokenOptions.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        Option brokenOption = (Option) brokenOptions.get(position);

        validatedDatasetView.removeOption(options[position.intValue()]);
        validatedDatasetView.insertOption(position.intValue(), brokenOption);
      }
    }

    boolean hasBrokenAttributePages = false;
    AttributePage[] apages = dsv.getAttributePages();
    HashMap brokenAPages = new HashMap();

    for (int i = 0, n = apages.length; i < n; i++) {
      AttributePage validatedPage = getValidatedAttributePage(dsource, apages[i]);

      if (validatedPage.isBroken()) {
        hasBrokenAttributePages = true;
        brokenAPages.put(new Integer(i), validatedPage);
      }
    }

    if (hasBrokenAttributePages) {
      validatedDatasetView.setAttributePagesBroken();

      for (Iterator iter = brokenAPages.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        AttributePage brokenAPage = (AttributePage) brokenAPages.get(position);

        validatedDatasetView.removeAttributePage(apages[position.intValue()]);
        validatedDatasetView.insertAttributePage(position.intValue(), brokenAPage);
      }
    }

    boolean hasBrokenFilterPages = false;
    HashMap brokenFPages = new HashMap();
    FilterPage[] allPages = dsv.getFilterPages();
    for (int i = 0, n = allPages.length; i < n; i++) {
      FilterPage validatedPage = getValidatedFilterPage(dsource, allPages[i]);

      if (validatedPage.isBroken()) {
        hasBrokenFilterPages = true;
        brokenFPages.put(new Integer(i), validatedPage);
      }
    }

    if (hasBrokenFilterPages) {
      validatedDatasetView.setFilterPagesBroken();

      for (Iterator iter = brokenFPages.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        FilterPage brokenPage = (FilterPage) brokenFPages.get(position);

        validatedDatasetView.removeFilterPage(allPages[position.intValue()]);
        validatedDatasetView.insertFilterPage(position.intValue(), brokenPage);
      }
    }

    return validatedDatasetView;
  }

  private static DefaultFilter getValidatedDefaultFilter(
    DetailedDataSource dsource,
    String schema,
    String catalog,
    DefaultFilter dfilter)
    throws SQLException {
    DefaultFilter validatedDefaultFilter = new DefaultFilter(dfilter);

    FilterDescription validatedFilterDescription =
      getValidatedFilterDescription(dsource, schema, catalog, dfilter.getFilterDescription());

    if (validatedFilterDescription.isBroken()) {
      validatedDefaultFilter.setFilterBroken();

      validatedDefaultFilter.setFilterDescription(validatedFilterDescription);
    }

    return validatedDefaultFilter;
  }

  public static String getValidatedStarBase(DetailedDataSource dsource, String schema, String catalog, String starbase)
    throws SQLException {
    String validatedStarBase = new String(starbase);

    String table = starbase + "%" + MAINTABLESUFFIX;
    boolean isBroken = true;

    ResultSet rs = dsource.getConnection().getMetaData().getTables(catalog, schema, table, null);
    while (rs.next()) {
      String thisTable = rs.getString(3);
      if (thisTable.toLowerCase().startsWith(starbase.toLowerCase())) {
        isBroken = false;
        break;
      } else {
        if (logger.isLoggable(Level.INFO))
          logger.info("Recieved table " + thisTable + " when querying for " + table + "\n");
      }
    }

    if (isBroken)
      validatedStarBase += DOESNTEXISTSUFFIX;

    return validatedStarBase;
  }

  public static String getValidatedPrimaryKey(DetailedDataSource dsource, String schema, String catalog, String primaryKey)
    throws SQLException {
    String validatedPrimaryKey = new String(primaryKey);

    String tablePattern = "%" + MAINTABLESUFFIX;
    boolean isBroken = true;

    ResultSet columns = dsource.getConnection().getMetaData().getColumns(catalog, schema, tablePattern, primaryKey);
    while (columns.next()) {
      String thisColumn = columns.getString(4);

      if (thisColumn.toLowerCase().equals(primaryKey.toLowerCase())) {
        isBroken = false;
        break;
      } else {
        if (logger.isLoggable(Level.INFO))
          logger.info("Recieved column " + thisColumn + " during query for primary key " + primaryKey + "\n");
      }
    }

    if (isBroken)
      validatedPrimaryKey += DOESNTEXISTSUFFIX;

    return validatedPrimaryKey;
  }

  public static FilterPage getValidatedFilterPage(DetailedDataSource dsource, FilterPage page) throws SQLException {
    FilterPage validatedPage = new FilterPage(page);

    boolean hasBrokenGroups = false;
    HashMap brokenGroups = new HashMap();

    List allGroups = page.getFilterGroups();
    for (int i = 0, n = allGroups.size(); i < n; i++) {
      Object group = allGroups.get(i);

      if (group instanceof FilterGroup) {
        FilterGroup validatedGroup = getValidatedFilterGroup(dsource, (FilterGroup) group);

        if (validatedGroup.isBroken()) {
          hasBrokenGroups = true;
          brokenGroups.put(new Integer(i), validatedGroup);
        }
      } // else not needed yet

      if (hasBrokenGroups) {
        validatedPage.setGroupsBroken();

        for (Iterator iter = brokenGroups.keySet().iterator(); iter.hasNext();) {
          Integer position = (Integer) iter.next();
          Object brokenGroup = brokenGroups.get(position);

          if (brokenGroup instanceof FilterGroup) {
            validatedPage.removeFilterGroup((FilterGroup) allGroups.get(position.intValue()));
            validatedPage.insertFilterGroup(position.intValue(), (FilterGroup) brokenGroup);
          } //else not needed yet
        }
      }
    }

    return validatedPage;
  }

  public static FilterGroup getValidatedFilterGroup(DetailedDataSource dsource, FilterGroup group) throws SQLException {
    FilterGroup validatedGroup = new FilterGroup(group);

    FilterCollection[] collections = group.getFilterCollections();

    boolean hasBrokenCollections = false;
    HashMap brokenCollections = new HashMap();

    for (int i = 0, n = collections.length; i < n; i++) {
      FilterCollection validatedCollection = getValidatedFilterCollection(dsource, collections[i]);

      if (validatedCollection.isBroken()) {
        hasBrokenCollections = true;
        brokenCollections.put(new Integer(i), validatedCollection);
      }
    }

    if (hasBrokenCollections) {
      validatedGroup.setCollectionsBroken();

      for (Iterator iter = brokenCollections.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        FilterCollection brokenCollection = (FilterCollection) brokenCollections.get(position);

        validatedGroup.removeFilterCollection(collections[position.intValue()]);
        validatedGroup.insertFilterCollection(position.intValue(), brokenCollection);
      }
    }

    return validatedGroup;
  }

  /**
   * Runs through the FilterDescription objects within a FilterCollection and checks whether the field and,
   * if present, tableConstraint is present in the given mart hosted by the DetailedDataSource provided, or, for FilterDescription
   * Objects containing child Options/PushAction combinations, whether these are valid in the same manner.
   * @param dsource - DetailedDataSource containing a Connection to a Mart Database
   * @param collection - FilterCollection to validate
   * @return Copy of given FilterCollection with validated FilterDescription Objects 
   */
  public static FilterCollection getValidatedFilterCollection(DetailedDataSource dsource, FilterCollection collection)
    throws SQLException {
    String schema = null;
    String catalog = null;

    ResultSet schemas = dsource.getConnection().getMetaData().getSchemas();
    while (schemas.next()) {
      schema = schemas.getString(1);
      catalog = schemas.getString(2);

      if (logger.isLoggable(Level.INFO))
        logger.info("schema: " + schema + " - catalog: " + catalog + "\n");
    }

    FilterCollection validatedFilterCollection = new FilterCollection(collection);

    List allFilts = collection.getFilterDescriptions();

    boolean filtersValid = true;
    HashMap brokenFilts = new HashMap();

    for (int i = 0, n = allFilts.size(); i < n; i++) {
      Object element = allFilts.get(i);

      if (element instanceof FilterDescription) {
        FilterDescription validatedFilter =
          getValidatedFilterDescription(dsource, schema, catalog, (FilterDescription) element);
        if (validatedFilter.isBroken())
          brokenFilts.put(new Integer(i), validatedFilter);
      } //else not needed yet
    }

    if (!filtersValid) {
      validatedFilterCollection.setFiltersBroken();

      for (Iterator iter = brokenFilts.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        Object brokenFilter = brokenFilts.get(position);

        if (brokenFilter instanceof FilterDescription) {
          validatedFilterCollection.removeFilterDescription((FilterDescription) allFilts.get(position.intValue()));
          validatedFilterCollection.insertFilterDescription(position.intValue(), (FilterDescription) brokenFilter);
        } //else not needed yet
      }
    }

    return validatedFilterCollection;
  }

  public static FilterDescription getValidatedFilterDescription(
    DetailedDataSource dsource,
    String schema,
    String catalog,
    FilterDescription filter)
    throws SQLException {
    FilterDescription validatedFilter = new FilterDescription(filter);

    if (filter.getField() != null) {
      //test
      boolean fieldValid = false;
      boolean tableValid = false;

      String field = filter.getField();
      String tableConstraint = filter.getTableConstraint();

      // if the tableConstraint is null, this field must be available in one of the main tables
      String table = (tableConstraint != null) ? "%" + tableConstraint + "%" : "%" + MAINTABLESUFFIX;

      ResultSet rs = dsource.getConnection().getMetaData().getColumns(catalog, schema, table, field);
      while (rs.next()) {
        String columnName = rs.getString(4);
        String tableName = rs.getString(3);

        boolean[] valid = isValidDescription(columnName, field, tableName, tableConstraint);
        fieldValid = valid[0];
        tableValid = valid[1];

        if (valid[0] && valid[1])
          break;
      }

      if (!(fieldValid && tableValid)) {
        validatedFilter.setFieldBroken();
        validatedFilter.setTableConstraintBroken();
      }
    } else {
      //check Options/PushAction Options
      boolean optionsValid = true;
      HashMap brokenOptions = new HashMap();

      Option[] options = filter.getOptions();
      for (int j = 0, m = options.length; j < m; j++) {
        Option validatedOption = getValidatedOption(dsource, schema, catalog, options[j]);
        if (validatedOption.isBroken()) {
          optionsValid = false;
          brokenOptions.put(new Integer(j), validatedOption);
        }
      }

      if (!optionsValid) {
        validatedFilter.setOptionsBroken();

        for (Iterator iter = brokenOptions.keySet().iterator(); iter.hasNext();) {
          Integer position = (Integer) iter.next();
          Option brokenOption = (Option) brokenOptions.get(position);

          //remove the old version of the broken option
          validatedFilter.removeOption(options[position.intValue()]);
          //insert the validated version of the broken option in its place
          validatedFilter.insertOption(position.intValue(), brokenOption);
        }
      }
    }

    return validatedFilter;
  }

  public static Option getValidatedOption(DetailedDataSource dsource, String schema, String catalog, Option option)
    throws SQLException {
    Option validatedOption = new Option(option);

    if (option.getField() != null) {
      //test
      boolean fieldValid = false;
      boolean tableValid = false;

      String field = option.getField();
      String tableConstraint = option.getTableConstraint();

      // if the tableConstraint is null, this field must be available in one of the main tables
      String table = (tableConstraint != null) ? "%" + tableConstraint + "%" : "%" + MAINTABLESUFFIX;

      ResultSet rs = dsource.getConnection().getMetaData().getColumns(catalog, schema, table, field);
      while (rs.next()) {
        String columnName = rs.getString(4);
        String tableName = rs.getString(3);

        boolean[] valid = isValidDescription(columnName, field, tableName, tableConstraint);
        fieldValid = valid[0];
        tableValid = valid[1];

        if (valid[0] && valid[1])
          break;
      }

      if (!(fieldValid && tableValid)) {
        validatedOption.setFieldBroken(); //eg. if field is valid, Option.hasBrokenField will return false
        validatedOption.setTableConstraintBroken();
        //eg. if table is valid, Option.hasBrokenTableConstraint will return false
      }
    } else {
      //check Options/PushAction Options
      boolean optionsValid = true;
      HashMap brokenOptions = new HashMap();

      Option[] options = option.getOptions();
      for (int j = 0, m = options.length; j < m; j++) {
        Option validatedSubOption = getValidatedOption(dsource, schema, catalog, options[j]);
        if (validatedSubOption.isBroken()) {
          optionsValid = false;
          brokenOptions.put(new Integer(j), validatedSubOption);
        }
      }

      if (!optionsValid) {
        validatedOption.setOptionsBroken();
        //if optionsValid is false, option.hasBrokenOptions would be true

        for (Iterator iter = brokenOptions.keySet().iterator(); iter.hasNext();) {
          Integer position = (Integer) iter.next();
          Option brokenOption = (Option) brokenOptions.get(position);

          //remove the old version of the broken option
          validatedOption.removeOption(options[position.intValue()]);
          //insert the validated version of the broken option in its place
          validatedOption.insertOption(position.intValue(), brokenOption);
        }
      }

      boolean pushActionsValid = true;
      HashMap brokenPushActions = new HashMap();
      PushAction[] pas = option.getPushActions();
      for (int j = 0, m = pas.length; j < m; j++) {
        PushAction validatedAction = getValidatedPushAction(dsource, schema, catalog, pas[j]);
        if (validatedAction.isBroken()) {
          pushActionsValid = false;
          brokenPushActions.put(new Integer(j), validatedAction);
        }
      }

      if (!pushActionsValid) {
        validatedOption.setPushActionsBroken();
        //if pushActionsValid is false, option.hasBrokenPushActions will be true

        for (Iterator iter = brokenPushActions.keySet().iterator(); iter.hasNext();) {
          Integer position = (Integer) iter.next();
          PushAction brokenPushAction = (PushAction) brokenPushActions.get(position);

          validatedOption.removePushAction(pas[position.intValue()]);
          validatedOption.addPushAction(brokenPushAction); //PushActions are not sensitive to position
        }
      }
    }

    return validatedOption;
  }

  public static PushAction getValidatedPushAction(DetailedDataSource dsource, String schema, String catalog, PushAction action)
    throws SQLException {
    PushAction validatedPushAction = new PushAction(action);

    boolean optionsValid = true;
    HashMap brokenOptions = new HashMap();

    Option[] options = action.getOptions();
    for (int j = 0, m = options.length; j < m; j++) {
      Option validatedSubOption = getValidatedOption(dsource, schema, catalog, options[j]);
      if (validatedSubOption.isBroken()) {
        optionsValid = false;
        brokenOptions.put(new Integer(j), validatedSubOption);
      }
    }

    if (!optionsValid) {
      validatedPushAction.setOptionsBroken(); //if optionsValid is false, option.hasBrokenOptions would be true

      for (Iterator iter = brokenOptions.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        Option brokenOption = (Option) brokenOptions.get(position);

        validatedPushAction.removeOption(options[position.intValue()]);
        validatedPushAction.insertOption(position.intValue(), brokenOption);
      }
    }

    return validatedPushAction;
  }

  public static AttributePage getValidatedAttributePage(DetailedDataSource dsource, AttributePage page) throws SQLException {
    AttributePage validatedPage = new AttributePage(page);

    boolean hasBrokenGroups = false;
    HashMap brokenGroups = new HashMap();

    List allGroups = page.getAttributeGroups();
    for (int i = 0, n = allGroups.size(); i < n; i++) {
      Object group = allGroups.get(i);

      if (group instanceof AttributeGroup) {
        AttributeGroup validatedGroup = getValidatedAttributeGroup(dsource, (AttributeGroup) group);

        if (validatedGroup.isBroken()) {
          hasBrokenGroups = true;
          brokenGroups.put(new Integer(i), group);
        }
      } //else not needed yet      
    }

    if (hasBrokenGroups) {
      validatedPage.setGroupsBroken();

      for (Iterator iter = brokenGroups.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();

        Object brokenGroup = brokenGroups.get(position);
        if (brokenGroup instanceof AttributeGroup) {
          validatedPage.removeAttributeGroup((AttributeGroup) allGroups.get(position.intValue()));
          validatedPage.insertAttributeGroup(position.intValue(), (AttributeGroup) brokenGroup);
        } //else not needed
      }
    }

    return validatedPage;
  }

  public static AttributeGroup getValidatedAttributeGroup(DetailedDataSource dsource, AttributeGroup group) throws SQLException {
    AttributeGroup validatedGroup = new AttributeGroup(group);

    boolean hasBrokenCollections = false;
    HashMap brokenCollections = new HashMap();

    AttributeCollection[] collections = group.getAttributeCollections();
    for (int i = 0, n = collections.length; i < n; i++) {
      AttributeCollection validatedCollection = getValidatedAttributeCollection(dsource, collections[i]);

      if (validatedCollection.isBroken()) {
        hasBrokenCollections = true;
        brokenCollections.put(new Integer(i), validatedCollection);
      }
    }

    if (hasBrokenCollections) {
      validatedGroup.setCollectionsBroken();

      for (Iterator iter = brokenCollections.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        AttributeCollection brokenCollection = (AttributeCollection) brokenCollections.get(position);

        validatedGroup.removeAttributeCollection(collections[position.intValue()]);
        validatedGroup.insertAttributeCollection(position.intValue(), brokenCollection);
      }
    }

    return validatedGroup;
  }

  public static AttributeCollection getValidatedAttributeCollection(DetailedDataSource dsource, AttributeCollection collection)
    throws SQLException {
    String schema = null;
    String catalog = null;

    ResultSet schemas = dsource.getConnection().getMetaData().getSchemas();
    while (schemas.next()) {
      schema = schemas.getString(1);
      catalog = schemas.getString(2);

      if (logger.isLoggable(Level.INFO))
        logger.info("schema: " + schema + " - catalog: " + catalog + "\n");
    }

    AttributeCollection validatedAttributeCollection = new AttributeCollection(collection);
    boolean hasBrokenAttributes = false;
    HashMap brokenAtts = new HashMap();

    List allAtts = collection.getAttributeDescriptions();
    for (int i = 0, n = allAtts.size(); i < n; i++) {
      Object attribute = allAtts.get(i);
      if (attribute instanceof AttributeDescription) {
        AttributeDescription validatedAttributeDescription =
          getValidatedAttributeDescription(dsource, schema, catalog, (AttributeDescription) attribute);

        if (validatedAttributeDescription.isBroken()) {
          hasBrokenAttributes = true;
          brokenAtts.put(new Integer(i), validatedAttributeDescription);
        }
      } //else not needed yet
    }

    if (hasBrokenAttributes) {
      validatedAttributeCollection.setAttributesBroken();

      for (Iterator iter = brokenAtts.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        Object brokenAtt = brokenAtts.get(position);

        if (brokenAtt instanceof AttributeDescription) {
          validatedAttributeCollection.removeAttributeDescription(
            (AttributeDescription) allAtts.get(position.intValue()));
          validatedAttributeCollection.insertAttributeDescription(position.intValue(), (AttributeDescription) brokenAtt);
        } //else not needed yet
      }
    }

    return validatedAttributeCollection;
  }

  public static AttributeDescription getValidatedAttributeDescription(
    DetailedDataSource dsource,
    String schema,
    String catalog,
    AttributeDescription description)
    throws SQLException {
    AttributeDescription validatedAttribute = new AttributeDescription(description);

    boolean fieldValid = false;
    boolean tableValid = false;

    String field = description.getField();
    String tableConstraint = description.getTableConstraint();

    // if the tableConstraint is null, this field must be available in one of the main tables
    String table = (tableConstraint != null) ? "%" + tableConstraint + "%" : "%" + MAINTABLESUFFIX;

    ResultSet rs = dsource.getConnection().getMetaData().getColumns(catalog, schema, table, field);
    while (rs.next()) {
      String columnName = rs.getString(4);
      String tableName = rs.getString(3);

      boolean[] valid = isValidDescription(columnName, field, tableName, tableConstraint);
      fieldValid = valid[0];
      tableValid = valid[1];

      if (valid[0] && valid[1])
        break;
    }

    if (!(fieldValid && tableValid)) {
      validatedAttribute.setFieldBroken();
      validatedAttribute.setTableConstraintBroken();
    }

    return validatedAttribute;
  }

  private static boolean[] isValidDescription(
    String columnName,
    String descriptionField,
    String tableName,
    String descriptionTableConstraint) {
    boolean[] validFlags = new boolean[] { false, false };

    if (columnName.equals(descriptionField)) {
      validFlags[0] = true;

      if (descriptionTableConstraint != null) {
        if (tableName.toLowerCase().indexOf(descriptionTableConstraint.toLowerCase()) > -1) {
          validFlags[1] = true;
        } else {
          if (logger.isLoggable(Level.INFO))
            logger.info(
              "Recieved correct field, but tableName "
                + tableName
                + " does not contain "
                + descriptionTableConstraint
                + "\n");
        }
      } else {
        validFlags[1] = true;
      }
    } else {
      if (logger.isLoggable(Level.INFO))
        logger.info(
          "RECIEVED "
            + columnName
            + " WHEN EXPECTING "
            + descriptionField
            + " from table "
            + tableName
            + " ( = "
            + descriptionTableConstraint
            + " ?)\n");
    }

    return validFlags;
  }

  /**
   * Returns a list of potential (nieve) dataset names from a given Mart compliant database hosted on a given DetailedDataSource.
   * These names can be used as an argument to getNieveMainTablesFor, getNieveDimensionTablesFor and getNieveDatasetViewFor.
   * @param dsource -- DetailedDataSource housing a connection to an RDBMS
   * @param databaseName -- name of the RDBMS instance to search for potential datasets
   * @return String[] of potential dataset names
   * @throws SQLException
   */
  public static String[] getNieveDatasetNamesFor(DetailedDataSource dsource, String databaseName) throws SQLException {
    String[] potentials = getNieveMainTablesFor(dsource, databaseName, null);
    
    //now weed them to a subset, attempting to unionize conformed dimension names
    List retList = new ArrayList();

    for (int i = 0, n = potentials.length; i < n; i++) {
      String curval = potentials[i];

      retList.add( curval.replaceFirst("_[Mm][Aa][Ii][Nn]", "") );
    }

    String[] dsList = new String[retList.size()];
    retList.toArray(dsList);
    return dsList;
  }

  /**
   * Retruns a String[] of possible main tables for a given Mart Compliant database, hosted on a given 
   * RDBMS, with an (optional) datasetName to key upon.  With no datasetName, all possible main tables from 
   * the database are returned.
   * @param dsource -- DetailedDataSource housing a connection to an RDBMS
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNieveDatasetNamesFor, or null)
   * @return String[] of potential main table names
   * @throws SQLException
   */
  public static String[] getNieveMainTablesFor(DetailedDataSource dsource, String databaseName, String datasetName)
    throws SQLException {
    //want sorted entries, dont need to worry about duplicates
    Set potentials = new TreeSet();

    Connection conn = dsource.getConnection();

    DatabaseMetaData dmd = conn.getMetaData();

    //Note: currently this isnt cross platform,
    //as some RDBMS capitalize all names of tables
    //Either need to find a capitalization scheme general
    //to all RDBMS, or search for both MAIN and main
    //and force intermart consistency of capitalization for
    //those RDBMS which allow users freedom to capitalize as
    //they see fit. Currently does the latter.
    String tablePattern = (datasetName != null) ? datasetName + "%" : "%";
    tablePattern += MAINTABLESUFFIX;
    String capTablePattern = tablePattern.toUpperCase();

    //get all main tables
    //first search for tablePattern    
    ResultSet rsTab = dmd.getTables(null, databaseName, tablePattern, null);

    while (rsTab.next()) {
      String tableName = rsTab.getString(3);
      potentials.add(tableName);
    }
    rsTab.close();

    //now try capitals, should NOT get mixed results
    rsTab = dmd.getTables(null, databaseName, capTablePattern, null);
    while (rsTab.next()) {
      String tableName = rsTab.getString(3);

      if (!potentials.contains(tableName))
        potentials.add(tableName);
    }
    rsTab.close();
    conn.close();
    
    String[] retList = new String[potentials.size()];
    potentials.toArray(retList);
    return retList;
  }

  /**
   * Returns a String[] of potential dimension tables from a given Mart Compliant database, hosted on a
   * given RDBMS, constrained to an (optional) dataset.
   * @param dsource -- DetailedDataSource housing a connection to an RDBMS
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNieveDatasetNamesFor, or null)
   * @return String[] of potential dimension table names
   * @throws SQLException
   */
  public static String[] getNieveDimensionTablesFor(DetailedDataSource dsource, String databaseName, String datasetName)
    throws SQLException {
    //want sorted entries, dont need to worry about duplicates
    Set potentials = new TreeSet();

    Connection conn = dsource.getConnection();

    DatabaseMetaData dmd = conn.getMetaData();

    //Note: currently this isnt cross platform,
    //as some RDBMS capitalize all names of tables
    //Either need to find a capitalization scheme general
    //to all RDBMS, or search for both MAIN and main
    //and force intermart consistency of capitalization for
    //those RDBMS which allow users freedom to capitalize as
    //they see fit. Currently does the latter.
    String tablePattern = (datasetName != null) ? datasetName + "%" : "%";
    tablePattern += DIMENSIONTABLESUFFIX;
    String capTablePattern = tablePattern.toUpperCase();

    //get all dimension tables
    //first search for tablePattern    
    ResultSet rsTab = dmd.getTables(null, databaseName, tablePattern, null);

    while (rsTab.next()) {
      String tableName = rsTab.getString(3);
      potentials.add(tableName);
    }
    rsTab.close();

    //now try capitals, should NOT get mixed results
    rsTab = dmd.getTables(null, databaseName, capTablePattern, null);
    while (rsTab.next()) {
      String tableName = rsTab.getString(3);

      if (!potentials.contains(tableName))
        potentials.add(tableName);
    }
    rsTab.close();
    conn.close();
    
    String[] retList = new String[potentials.size()];
    potentials.toArray(retList);
    return retList;
  }

  /**
   * Returns a TableDescription object describing a particular table in a given database,
   * hosted on a given RDBMS.
   * @param dsource -- DetailedDataSource housing a connection to an RDBMS
   * @param databaseName -- name of the RDBMS instance housing the requested table
   * @param tableName -- name of the desired table, as might be returned by a call to getXXXTablesFor
   * @return TableDescription object describing the table
   * @throws SQLException
   */
  public static TableDescription getTableDescriptionFor(DetailedDataSource dsource, String databaseName, String tableName)
    throws SQLException {
    Connection conn = dsource.getConnection();
    DatabaseMetaData dmd = conn.getMetaData();

    List columns = new ArrayList();
    ResultSet rset = dmd.getColumns(null, databaseName, tableName, null);
    while (rset.next()) {
      if (rset.getString(3).toLowerCase().equals(tableName.toLowerCase())) {
        String cname = rset.getString(4);
        int javaType = rset.getInt(5);
        String dbType = rset.getString(6);
        int maxLength = rset.getInt(7);

        ColumnDescription column = new ColumnDescription(cname, dbType, javaType, maxLength);
        columns.add(column);
      }
    }
    rset.close();
    conn.close();
    
    ColumnDescription[] cols = new ColumnDescription[columns.size()];
    columns.toArray(cols);

    TableDescription table = new TableDescription(tableName, cols);
    return table;
  }

  //TODO: change this when Mart Compliant Schema is fully optimized
  /**
   * Returns a nieve DatasetView for a given dataset within a given Mart Compliant database, hosted on a given
   * RDBMS.  This will consist of an unordered set of starbases, no primary keys, and one FilterPage and AttributePage
   * containing one group with Collections for each table containing descriptions for each field.  Filters are only
   * described for main tables, and no Option grouping is attempted.  All Descriptions are fully constrained to
   * a tableConstraint.  Note, this method is likely to undergo extensive revisions as we optimize the Mart Compliant
   * Schema.
   * @param dsource -- DetailedDataSource housing a connection to an RDBMS
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNieveDatasetNamesFor, or null)
   * @return
   * @throws ConfigurationException
   * @throws SQLException
   */
  public static DatasetView getNieveDatasetViewFor(DetailedDataSource dsource, String databaseName, String datasetName)
    throws ConfigurationException, SQLException {
    DatasetView dsv = new DatasetView();

    List starbases = new ArrayList();
    String[] primaryKeys = new String[] { "!!! MUST ADD PRIMARY KEYS MANUALLY !!!"};

    dsv.setInternalName(datasetName);
    dsv.setDisplayName(datasetName + " ( " + databaseName + " )");
    dsv.setDataset(datasetName);
    dsv.addPrimaryKeys(primaryKeys);
    
    AttributePage ap = new AttributePage();
    ap.setInternalName(DEFAULTPAGE);
    ap.setDisplayName(DEFAULTPAGE);

    FilterPage fp = new FilterPage();
    fp.setInternalName(DEFAULTPAGE);
    fp.setDisplayName(DEFAULTPAGE);

    AttributeGroup ag = new AttributeGroup();
    ag.setInternalName(DEFAULTGROUP);
    ag.setDisplayName(DEFAULTGROUP);

    FilterGroup fg = new FilterGroup();
    fg.setInternalName(DEFAULTGROUP);
    fg.setDisplayName(DEFAULTGROUP);

    List allTables = new ArrayList();
    allTables.addAll(Arrays.asList(getNieveMainTablesFor(dsource, databaseName, datasetName)));
    allTables.addAll(Arrays.asList(getNieveDimensionTablesFor(dsource, databaseName, datasetName)));

    for (int i = 0, n = allTables.size(); i < n; i++) {
      String tableName = (String) allTables.get(i);

      AttributeCollection ac = new AttributeCollection();
      ac.setInternalName(tableName);
      ac.setDisplayName(tableName);

      FilterCollection fc = null;
      if (isMainTable(tableName)) {
        String starbase = tableName;
        starbase = starbase.replaceAll("_[Mm][Aa][Ii][Nn]", "");
        // replace _MAIN or _main or _MaIn, etc with the empty string

        if (logger.isLoggable(Level.INFO))
          logger.info("Starbase " + starbase + "\n");

        if (!starbases.contains(starbase))
          starbases.add(starbase);

        fc = new FilterCollection();
        fc.setInternalName(tableName);
        fc.setDisplayName(tableName);
      }

      TableDescription table = getTableDescriptionFor(dsource, databaseName, tableName);

      for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
        ColumnDescription column = table.columnDescriptions[j];

        String cname = column.name;
        int ctype = column.javaType; // java generalized type across all JDBC instances
        //String ctype = column.dbType; //as in RDBMS table definition
        int csize = column.maxLength;

        if (logger.isLoggable(Level.INFO))
          logger.info(tableName + ": " + cname + "-- type : " + ctype + "\n");

        if (isMainTable(tableName) || isDimensionTable(tableName)) {
          ac.addAttributeDescription(getAttributeDescription(cname, tableName, csize));

          if (isMainTable(tableName)) {
            fc.addFilterDescription(getFilterDescription(cname, tableName, ctype));
          }
        } else {
          if (logger.isLoggable(Level.INFO))
            logger.info("Skipping " + tableName + "\n");
        }
      }

      ag.addAttributeCollection(ac);

      if (fc != null)
        fg.addFilterCollection(fc);
    }

    ap.addAttributeGroup(ag);
    fp.addFilterGroup(fg);

    dsv.addAttributePage(ap);
    dsv.addFilterPage(fp);

    String[] sbases = new String[starbases.size()];
    starbases.toArray(sbases);

    dsv.addStarBases(sbases);
    return dsv;
  }

  private static boolean isDimensionTable(String tableName) {
    if (tableName.toLowerCase().endsWith(DIMENSIONTABLESUFFIX))
      return true;
    return false;
  }

  private static boolean isMainTable(String tableName) {
    if (tableName.toLowerCase().endsWith(MAINTABLESUFFIX.toLowerCase()))
      return true;
    return false;
  }

  private static AttributeDescription getAttributeDescription(String columnName, String tableName, int maxSize)
    throws ConfigurationException {
    AttributeDescription att = new AttributeDescription();
    att.setInternalName(columnName.toLowerCase());
    att.setDisplayName(columnName);
    att.setField(columnName);
    att.setTableConstraint(tableName);
    att.setMaxLength(String.valueOf(maxSize));

    return att;
  }

  private static FilterDescription getFilterDescription(String columnName, String tableName, int columnType) {
    FilterDescription filt = new FilterDescription();
    filt.setInternalName(columnName.toLowerCase());
    filt.setDisplayName(columnName);
    filt.setField(columnName);
    filt.setTableConstraint(tableName);

    //this is just to debug possibility of using java.sql.Types values to determine suitable filter types
    if (logger.isLoggable(Level.INFO)) {
      if (columnType == java.sql.Types.TINYINT) {
        logger.info("Recieved TINYINT type, probably boolean or boolean_num\n");
      } else {
        logger.info("Recieved type " + columnType + " defaulting to list\n");
      }
    }

    filt.setType(DEFAULTTYPE);
    filt.setQualifier(DEFAULTQUALIFIER);
    filt.setLegalQualifiers(DEFAULTLEGALQUALIFIERS);
    return filt;
  }
}