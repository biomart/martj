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
import java.io.OutputStream;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.JOptionPane;

import oracle.sql.BLOB;
import oracle.sql.CLOB;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.util.ColumnDescription;
import org.ensembl.mart.util.TableDescription;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDatasetConfigUtils {

  private final String BASEMETATABLE = "meta_configuration"; // append user if necessary

  /*
   * meta_configuration<_username>
   * -----------------------
   * internalName   varchar(100)
   * displayName    varchar(100)
   * dataset        varchar(100)
   * description    varchar(200)
   * xml            longblob
   * compressed_xml longblob
   * MessageDigest  blob
   */

  private HashMap configInfo = new HashMap();
  
  private final String VISIBLESQL = " where visible = 1";
  private final String GETALLNAMESQL = "select internalname, displayName, dataset, description, MessageDigest, type, visible, version from ";
  private final String GETANYNAMESWHERINAME = " where internalName = ? and dataset = ?";
  private final String GETDOCBYINAMESELECT = "select xml, compressed_xml from "; //append table after user test
  private final String GETDOCBYINAMEWHERE = " where internalName = ? and dataset = ?";
  private final String EXISTSELECT = "select count(*) from "; //append table after user test
  private final String EXISTWHERE = " where internalName = ? and displayName = ? and dataset = ?";
  private final String DELETEOLDXML = "delete from "; //append table after user test
  private final String DELETEOLDXMLWHERE = " where internalName = ? and displayName = ? and dataset = ?";
  private final String DELETEDATASETCONFIG = " where dataset = ?";
  private final String DELETEINTERNALNAME = " and internalName = ?";
  private final String INSERTXMLSQLA = "insert into "; //append table after user test
  private final String INSERTXMLSQLB =
    " (internalName, displayName, dataset, description, xml, MessageDigest,type, visible, version) values (?, ?, ?, ?, ?, ?,?,?,?)";
  private final String SELECTXMLFORUPDATE = "select xml from ";
  private final String SELECTCOMPRESSEDXMLFORUPDATE = "select compressed_xml from ";
  private final String INSERTCOMPRESSEDXMLA = "insert into "; //append table after user test
  private final String INSERTCOMPRESSEDXMLB =
    " (internalName, displayName, dataset, description, compressed_xml, MessageDigest, type, visible, version) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
  private final String CREATEMETATABLESQL = "create table meta_configuration (internalName varchar(100), displayName varchar(100), dataset varchar(100), description varchar(200), xml longblob, compressed_xml longblob, MessageDigest blob, type varchar(20), visible int(1) unsigned, version varchar(25))";
  private final String ORACLE_CREATETABLESQL = "create table meta_configuration (internalname varchar2(100), displayname varchar2(100), dataset varchar2(100), description varchar2(200), xml clob, compressed_xml blob, messagedigest blob, type varchar2(100), visible number(1), version varchar2(25))";
  private final String MAINTABLESUFFIX = "main";
  private final String DIMENSIONTABLESUFFIX = "dm";
  private final String LOOKUPTABLESUFFIX = "look";

  private final String DOESNTEXISTSUFFIX = "**DOES_NOT_EXIST**";

  //private String DEFAULTLEGALQUALIFIERS = "in,=,>,<,>=,<=";
  //private String DEFAULTQUALIFIER = "in";
  //private String DEFAULTTYPE = "list";
  private String DEFAULTLEGALQUALIFIERS = "=";
  private String DEFAULTQUALIFIER = "=";
  private String DEFAULTTYPE = "text";

  private Logger logger = Logger.getLogger(DatabaseDatasetConfigUtils.class.getName());

  private DatasetConfigXMLUtils dscutils = null;
  private DetailedDataSource dsource = null;

  /**
   * Constructor for a DatabaseDatasetConfigUtils object to obtain DatasetConfig related information
   * from a Mart Database host.
   * @param dscutils - DatasetConfigXMLUtils object for parsing XML
   * @param dsource - DetailedDataSource object with connection to a Mart Database host.
   */
  public DatabaseDatasetConfigUtils(DatasetConfigXMLUtils dscutils, DetailedDataSource dsource) {
    this.dscutils = dscutils;
    this.dsource = dsource;
    
  }

  /**
   * Verify if a meta_configuration_[user] table exists.  Returns false if user is null, or
   * if the table does not exist. 
   * @param user - user to query
   * @return true if meta_configuration_[user] exists, false otherwise
   */
  public boolean datasetConfigUserTableExists(String user) {
    boolean exists = true;
    String table = BASEMETATABLE + "_" + user;

    if (user == null)
      return false;

    try {
      exists = tableExists(table);

      if (!exists) {
        //try upper casing the name
        exists = tableExists(table.toUpperCase());
      }

      if (!exists) {
        //try lower casing the name
        exists = tableExists(table.toLowerCase());
      }
    } catch (SQLException e) {
      exists = false;
      if (logger.isLoggable(Level.INFO))
        logger.info("Recieved SQL Exception checking for user table: " + e.getMessage() + "\ncontinuing!\n");
    }

    return exists;
  }

  private boolean tableExists(String table) throws SQLException {
    String tcheck = null;
    Connection conn = null;
    boolean ret = true;

    try {
      conn = dsource.getConnection();
      String catalog = conn.getCatalog();

      ResultSet vr = conn.getMetaData().getTables(catalog, null, table, null);

      //expect at most one result, if no results, tcheck will remain null
      if (vr.next())
        tcheck = vr.getString(3);

      vr.close();

      if (tcheck == null) {
        if (logger.isLoggable(Level.FINE))
          logger.fine("Table " + table + " does not exist, using " + BASEMETATABLE + " instead\n");
        ret = false;
      }

      if (tcheck == null || !(table.equals(tcheck))) {
        if (logger.isLoggable(Level.FINE))
          logger.fine("Returned Wrong table for verifyTable: wanted " + table + " got " + tcheck + "\n");
        ret = false;
      }
    } finally {
      //this throws any SQLException, but always closes the connection
      DetailedDataSource.close(conn);
    }

    return ret;
  }

  /**
   * Determine if meta_configuration exists in a Mart Database defined by the given DetailedDataSource.
   * @return true if meta_configuration exists, false if it does not exist
   * @throws ConfigurationException for all underlying Exceptions
   */
  public boolean baseDSConfigTableExists() throws ConfigurationException {
    String table = BASEMETATABLE;
    try {
      boolean exists = true;

      exists = tableExists(table);
      if (!exists)
        exists = tableExists(table.toUpperCase());
      if (!exists)
        exists = tableExists(table.toLowerCase());

      return exists;
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Could not find base Configuration table "
          + table
          + " in DataSource: "
          + dsource
          + "\nRecieved SQL Exception: "
          + e.getMessage()
          + "\n");
    }
  }

  /**
   * Store a DatesetConfig.dtd compliant (compressed or uncompressed) XML Document in the Mart Database with a given internalName and displayName.  
   * If user is not null and meta_DatsetConfig_[user] exists, this table is the target, otherwise, meta_configuration is the target.
   * Along with the internalName and displayName of the XML, an MD5 messageDigest of the xml is computed, and stored as well. 
   * @param user -- Specific User to look for meta_configuration_[user] table, if null, or non-existent, uses meta_configuration
   * @param internalName -- internalName of the DatasetConfigXML being stored.
   * @param displayName -- displayName of the DatasetConfig XML being stored.
   * @param dataset -- dataset of the DatasetConfig XML being stored
   * @param doc - JDOM Document object representing the XML for the DatasetConfig   
   * @param compress -- if true, the XML is compressed using GZIP.
   * @throws ConfigurationException when no meta_configuration table exists, and for all underlying Exceptions
   */
  public void storeDatasetConfiguration(
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc,
    boolean compress,
    String type,
    String visible,
    String version)
    throws ConfigurationException {

    int rowsupdated = 0;

    if (compress)
      rowsupdated = storeCompressedXML(user, internalName, displayName, dataset, description, doc, type, visible, version);
    else
      rowsupdated = storeUncompressedXML(user, internalName, displayName, dataset, description, doc);

    updateMartConfigForUser(user);
    if (rowsupdated < 1)
      if (logger.isLoggable(Level.WARNING))
        logger.warning("Warning, xml for " + internalName + ", " + displayName + " not stored"); //throw an exception?	
  }

  private int storeUncompressedXML(
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc)
    throws ConfigurationException {
    if (dsource.getJdbcDriverClassName().indexOf("oracle") >= 0)
      return storeUncompressedXMLOracle(user, internalName, displayName, dataset, description, doc);

    Connection conn = null;
    try {
      String metatable = getDSConfigTableFor(user);
      String insertSQL = INSERTXMLSQLA + metatable + INSERTXMLSQLB;

      if (logger.isLoggable(Level.FINE))
        logger.fine("\ninserting with SQL " + insertSQL + "\n");

      conn = dsource.getConnection();
      MessageDigest md5digest = MessageDigest.getInstance(DatasetConfigXMLUtils.DEFAULTDIGESTALGORITHM);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      DigestOutputStream dout = new DigestOutputStream(bout, md5digest);
      XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

      xout.output(doc, dout);

      byte[] xml = bout.toByteArray();
      byte[] md5 = md5digest.digest();

      bout.close();
      dout.close();

      int rowstodelete = getDSConfigEntryCountFor(metatable, dataset, internalName, displayName);

      if (rowstodelete > 0)
        deleteOldDSConfigEntriesFor(metatable, dataset, internalName, displayName);

      PreparedStatement ps = conn.prepareStatement(insertSQL);
      ps.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);
      ps.setString(4, description);
      ps.setBinaryStream(5, new ByteArrayInputStream(xml), xml.length);
      ps.setBytes(6, md5);

      int ret = ps.executeUpdate();
      ps.close();

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
    } finally {
      DetailedDataSource.close(conn);
    }
  }

  private int storeUncompressedXMLOracle(
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc)
    throws ConfigurationException {

    Connection conn = null;
    try {
      String metatable = getDSConfigTableFor(user);
      String insertSQL = INSERTXMLSQLA + metatable + INSERTXMLSQLB;
      String oraclehackSQL = SELECTXMLFORUPDATE + metatable + GETANYNAMESWHERINAME + " FOR UPDATE";

      if (logger.isLoggable(Level.FINE))
        logger.fine("\ninserting with SQL " + insertSQL + "\n");

      conn = dsource.getConnection();
      conn.setAutoCommit(false);

      MessageDigest md5digest = MessageDigest.getInstance(DatasetConfigXMLUtils.DEFAULTDIGESTALGORITHM);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      DigestOutputStream dout = new DigestOutputStream(bout, md5digest);
      XMLOutputter xout = new XMLOutputter(org.jdom.output.Format.getRawFormat());

      xout.output(doc, dout);

      byte[] xml = bout.toByteArray();
      byte[] md5 = md5digest.digest();

      bout.close();
      dout.close();

      int rowstodelete = getDSConfigEntryCountFor(metatable, dataset, internalName, displayName);

      if (rowstodelete > 0)
        deleteOldDSConfigEntriesFor(metatable, dataset, internalName, displayName);

      PreparedStatement ps = conn.prepareStatement(insertSQL);
      PreparedStatement ohack = conn.prepareStatement(oraclehackSQL);

      ps.setString(1, internalName);
      ohack.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);
      ohack.setString(2, dataset);
      ps.setString(4, description);
      ps.setClob(5, CLOB.empty_lob());
      ps.setBytes(6, md5);

      int ret = ps.executeUpdate();

      ResultSet rs = ohack.executeQuery();

      if (rs.next()) {
        CLOB clob = (CLOB) rs.getClob(1);

        OutputStream clobout = clob.getAsciiOutputStream();
        clobout.write(xml);
        clobout.close();
      }

      conn.commit();
      rs.close();
      ohack.close();
      ps.close();

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
    } finally {
      DetailedDataSource.close(conn);
    }
  }

  private int storeCompressedXML(
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc,
    String type,
    String visible,
    String version)
    throws ConfigurationException {
    if (dsource.getJdbcDriverClassName().indexOf("oracle") >= 0)
      return storeCompressedXMLOracle(user, internalName, displayName, dataset, description, doc, type, visible, version);

    Connection conn = null;
    try {
      String metatable = getDSConfigTableFor(user);
      String insertSQL = INSERTCOMPRESSEDXMLA + metatable + INSERTCOMPRESSEDXMLB;

      if (logger.isLoggable(Level.FINE))
        logger.fine("\ninserting with SQL " + insertSQL + "\n");

      conn = dsource.getConnection();
      MessageDigest md5digest = MessageDigest.getInstance(DatasetConfigXMLUtils.DEFAULTDIGESTALGORITHM);
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

      int rowstodelete = getDSConfigEntryCountFor(metatable, dataset, internalName, displayName);

      if (rowstodelete > 0)
        deleteOldDSConfigEntriesFor(metatable, dataset, internalName, displayName);

      PreparedStatement ps = conn.prepareStatement(insertSQL);
      ps.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);
      ps.setString(4, description);
      ps.setBinaryStream(5, new ByteArrayInputStream(xml), xml.length);
      ps.setBytes(6, md5);
	  ps.setString(7, type);
	  ps.setString(8, visible);
	  ps.setString(9,version);

      int ret = ps.executeUpdate();
      ps.close();

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
    } finally {
      DetailedDataSource.close(conn);
    }
  }

  private int storeCompressedXMLOracle(
    String user,
    String internalName,
    String displayName,
    String dataset,
    String description,
    Document doc,
    String type,
    String visible,
	String version)
    throws ConfigurationException {

    Connection conn = null;
    try {
      String metatable = getDSConfigTableFor(user);
      String insertSQL = INSERTCOMPRESSEDXMLA + metatable + INSERTCOMPRESSEDXMLB;
      String oraclehackSQL = SELECTCOMPRESSEDXMLFORUPDATE + metatable + GETANYNAMESWHERINAME + " FOR UPDATE";

      if (logger.isLoggable(Level.FINE))
        logger.fine("\ninserting with SQL " + insertSQL + "\nOracle: " + oraclehackSQL + "\n");

      conn = dsource.getConnection();
      conn.setAutoCommit(false);

      MessageDigest md5digest = MessageDigest.getInstance(DatasetConfigXMLUtils.DEFAULTDIGESTALGORITHM);
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

      int rowstodelete = getDSConfigEntryCountFor(metatable, dataset, internalName, displayName);

      if (rowstodelete > 0)
        deleteOldDSConfigEntriesFor(metatable, dataset, internalName, displayName);

      PreparedStatement ps = conn.prepareStatement(insertSQL);
      PreparedStatement ohack = conn.prepareStatement(oraclehackSQL);

      ps.setString(1, internalName);
      ohack.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);
      ohack.setString(2, dataset);
      ps.setString(4, description);
      ps.setBlob(5, BLOB.empty_lob());
      ps.setBytes(6, md5);
	  ps.setString(7,type);
	  ps.setString(8,visible);
	  ps.setString(9,version);
	  
      int ret = ps.executeUpdate();

      ResultSet rs = ohack.executeQuery();

      if (rs.next()) {
        BLOB blob = (BLOB) rs.getBlob(1);

        OutputStream blobout = blob.getBinaryOutputStream();
        blobout.write(xml);
        blobout.close();
      }

      conn.commit();
      rs.close();
      ohack.close();
      ps.close();

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
    } finally {
      DetailedDataSource.close(conn);
    }
  }

  private void updateMartConfigForUser(String user) throws ConfigurationException {
	configInfo = new HashMap();
	initMartConfigForUser(user);
  }
  
  private void initMartConfigForUser(String user) throws ConfigurationException {
    if (!configInfo.containsKey(user)) {
      HashMap userMap = new HashMap();
      configInfo.put(user, userMap);
    }
    
    Connection conn = null;
    try {
      String metatable = getDSConfigTableFor(user);
      String sql = GETALLNAMESQL + metatable;
      
      if (!dscutils.includeHiddenMembers) {
        sql += VISIBLESQL;
      }
      
      if (logger.isLoggable(Level.FINE))
        logger.fine(
          "Using " + sql + " to get unloaded DatasetConfigs for user " + user + "\n");

      conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);

      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        String iname = rs.getString(1);
        String dname = rs.getString(2);
        String dset = rs.getString(3);
        String description = rs.getString(4);
        String type = rs.getString(6);
        String visible = rs.getString(7);
		String version = rs.getString(8);
        byte[] digest = rs.getBytes(5);
        DatasetConfig dsv = new DatasetConfig(iname, dname, dset, description, type, visible,version);
        dsv.setMessageDigest(digest);
        
        HashMap userMap = (HashMap) configInfo.get(user);
        
        if (!userMap.containsKey(dset)) {
          HashMap dsetMap = new HashMap();
          userMap.put(dset, dsetMap);
        }
        
        HashMap dsetMap = (HashMap) userMap.get(dset);
        dsetMap.put(iname, dsv);
      }
      rs.close();
      
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
    } finally {
      DetailedDataSource.close(conn);
    }
  }
  
  /**
   * Returns all dataset names from the meta_configuration table for the given user.
   * @param user -- user for meta_configuration table, if meta_configuration_user does not exist, meta_configuration is attempted.
   * @return String[] dataset names
   * @throws ConfigurationException when valid meta_configuration table does not exist, and for all underlying SQL Exceptions
   */
  public String[] getAllDatasetNames(String user) throws ConfigurationException {
    if (!configInfo.containsKey(user))
      initMartConfigForUser(user);
    
    HashMap userMap = (HashMap) configInfo.get(user);
    
    //sort the names alphabetically  
    SortedSet names = new TreeSet(userMap.keySet());       

    String[] ret = new String[names.size()];
    names.toArray(ret);
    return ret;
  }

  /**
   * Returns all of the internalNames for the given dataset, as stored in the meta_configuration table for
   * the Mart Database for the given user.
   * @param user -- user for meta_configuration table, if meta_configuration_user does not exist, meta_configuration is attempted.
   * @param dataset -- dataset for which internalNames are requested
   * @return String[] containing all of the internalNames for the requested dataset.
   * @throws ConfigurationException when valid meta_configuration tables do not exist, and for all underlying Exceptons.
   */
  public String[] getAllInternalNamesForDataset(String user, String dataset) throws ConfigurationException {
    if (!configInfo.containsKey(user))
      initMartConfigForUser(user);
    
    HashMap userMap = (HashMap) configInfo.get(user);
    
    if (!userMap.containsKey(dataset))
      initMartConfigForUser(user);
    
    if (!userMap.containsKey(dataset))
      return new String[0];
    
    HashMap dsetMap = (HashMap) userMap.get(dataset);
      
    //sorted alphabetically  
    SortedSet names = new TreeSet(dsetMap.keySet());

    String[] ret = new String[names.size()];
    names.toArray(ret);
    return ret;
  }

  /**
   * Returns a DatasetConfig object from the Mart Database using a supplied DetailedDataSource for a given user, defined with the
   * given internalName and dataset.
   * @param user -- Specific User to look for meta_configuration_[user] table, if null, or non-existent, uses meta_configuration
   * @param dataset -- dataset for which DatasetConfig is requested
   * @param internalName -- internalName of desired DatasetConfig object
   * @return DatasetConfig defined by given internalName
   * @throws ConfigurationException when valid meta_configuration tables are absent, and for all underlying Exceptions
   */
  public DatasetConfig getDatasetConfigByDatasetInternalName(String user, String dataset, String internalName)
    throws ConfigurationException {

    if (!configInfo.containsKey(user))
      initMartConfigForUser(user);
    
    HashMap userMap = (HashMap) configInfo.get(user);
    
    if (!userMap.containsKey(dataset))
      initMartConfigForUser(user);
    
    if (!userMap.containsKey(dataset))
      return null;
      
    HashMap dsetMap = (HashMap) userMap.get(dataset);
    
    if (!dsetMap.containsKey(internalName))
      initMartConfigForUser(user);
    if (!dsetMap.containsKey(internalName))
      return null;
    
    DatasetConfig dsv = (DatasetConfig) dsetMap.get(internalName);
    return dsv;      
  }

  /**
   * Returns a DatasetConfig JDOM Document from the Mart Database using a supplied DetailedDataSource for a given user, defined with the
   * given internalName and dataset.
   * @param user -- Specific User to look for meta_configuration_[user] table, if null, or non-existent, uses meta_configuration
   * @param dataset -- dataset for which DatasetConfig document is requested
   * @param internalName -- internalName of desired DatasetConfig document
   * @return DatasetConfig JDOM Document defined by given displayName and dataset
   * @throws ConfigurationException when valid meta_configuration tables are absent, and for all underlying Exceptions
   */
  public Document getDatasetConfigDocumentByDatasetInternalName(String user, String dataset, String internalName)
    throws ConfigurationException {
    if (dsource.getJdbcDriverClassName().indexOf("oracle") >= 0)
      return getDatasetConfigDocumentByDatasetInternalNameOracle(user, dataset, internalName);

    Connection conn = null;
    try {
      String metatable = getDSConfigTableFor(user);
      String sql = GETDOCBYINAMESELECT + metatable + GETDOCBYINAMEWHERE;

      if (logger.isLoggable(Level.FINE))
        logger.fine(
          "Using " + sql + " to get DatasetConfig for internalName " + internalName + "and dataset " + dataset + "\n");

      conn = dsource.getConnection();
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

      InputStream rstream = null;
      if (cstream != null)
        rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
      else
        rstream = new ByteArrayInputStream(stream);

      return dscutils.getDocumentForXMLStream(rstream);
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQL Exception during fetch of requested DatasetConfig: " + e.getMessage(),
        e);
    } catch (IOException e) {
      throw new ConfigurationException(
        "Caught IOException during fetch of requested DatasetConfig: " + e.getMessage(),
        e);
    } finally {
      DetailedDataSource.close(conn);
    }
  }

  private Document getDatasetConfigDocumentByDatasetInternalNameOracle(String user, String dataset, String internalName)
    throws ConfigurationException {
    Connection conn = null;
    try {
      String metatable = getDSConfigTableFor(user);
      String sql = GETDOCBYINAMESELECT + metatable + GETDOCBYINAMEWHERE;

      if (logger.isLoggable(Level.FINE))
        logger.fine(
          "Using " + sql + " to get DatasetConfig for internalName " + internalName + "and dataset " + dataset + "\n");

      conn = dsource.getConnection();
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

      CLOB stream = (CLOB) rs.getClob(1);
      BLOB cstream = (BLOB) rs.getBlob(2);

      InputStream rstream = null;
      if (cstream != null) {
        rstream = new GZIPInputStream(cstream.getBinaryStream());
      } else
        rstream = stream.getAsciiStream();

      Document ret = dscutils.getDocumentForXMLStream(rstream);
      rstream.close();
      rs.close();
      return ret;
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQL Exception during fetch of requested DatasetConfig: " + e.getMessage(),
        e);
    } catch (IOException e) {
      throw new ConfigurationException(
        "Caught IOException during fetch of requested DatasetConfig: " + e.getMessage(),
        e);
    } finally {
      DetailedDataSource.close(conn);
    }
  }

  /**
   * Returns a DatasetConfig object from the Mart Database using a supplied DetailedDataSource for a given user, defined with the
   * given dataset and displayName
   * @param dataset -- dataset for which DatsetConfig is requested
   * @param user -- Specific User to look for meta_configuration_[user] table, if null, or non-existent, uses meta_configuration
   * @param displayName -- String displayName for requested DatasetConfig
   * @return DatasetConfig with given displayName and dataset
   * @throws ConfigurationException when valid meta_configuration tables are absent, and for all underlying Exceptions
   */
//  public DatasetConfig getDatasetConfigByDatasetDisplayName(String user, String dataset, String displayName)
//    throws ConfigurationException {
//    Connection conn = null;
//    try {
//      String metatable = getDSConfigTableFor(user);
//      String sql = GETALLNAMESQL + metatable + GETANYNAMESWHEREDNAME;
//
//      if (logger.isLoggable(Level.FINE))
//        logger.fine(
//          "Using " + sql + " to get DatasetConfig for displayName " + displayName + "and dataset " + dataset + "\n");
//
//      conn = dsource.getConnection();
//      PreparedStatement ps = conn.prepareStatement(sql);
//      ps.setString(1, displayName);
//      ps.setString(2, dataset);
//
//      ResultSet rs = ps.executeQuery();
//      if (!rs.next()) {
//        // will only get one result
//        rs.close();
//        conn.close();
//        return null;
//      }
//
//      String iname = rs.getString(1);
//      String dname = rs.getString(2);
//      String dprefix = rs.getString(3);
//      String description = rs.getString(4);
//      byte[] digest = rs.getBytes(5);
//      rs.close();
//
//      DatasetConfig dsv = new DatasetConfig(iname, dname, dprefix, description);
//      dsv.setMessageDigest(digest);
//      return dsv;
//    } catch (SQLException e) {
//      throw new ConfigurationException("Caught SQL Exception during fetch of requested digest: " + e.getMessage(), e);
//    } finally {
//      DetailedDataSource.close(conn);
//    }
//  }

  /**
   * Returns a DatasetConfig JDOM Document for a given user, dataset, and displayName
   * @param user -- mart user. Determines which meta table to extract DatasetConfig information from.
   * @param dataset -- dataset of required DatasetConfig
   * @param displayName -- displayName of required DatasetConfig
   * @return JDOM Document for required DatasetConfig XML
   * @throws ConfigurationException for all underlying Exceptions
   */
//  public Document getDatasetConfigDocumentByDatasetDisplayName(String user, String dataset, String displayName)
//    throws ConfigurationException {
//    if (dsource.getJdbcDriverClassName().indexOf("oracle") >= 0)
//      return getDatasetConfigDocumentByDatasetDisplayNameOracle(user, dataset, displayName);
//
//    Connection conn = null;
//    try {
//      String metatable = getDSConfigTableFor(user);
//      String sql = GETDOCBYDNAMESELECT + metatable + GETDOCBYDNAMEWHERE;
//
//      if (logger.isLoggable(Level.FINE))
//        logger.fine(
//          "Using "
//            + sql
//            + " to get DatasetConfig Document for displayName "
//            + displayName
//            + " and dataset "
//            + dataset
//            + "\n");
//
//      conn = dsource.getConnection();
//      PreparedStatement ps = conn.prepareStatement(sql);
//      ps.setString(1, displayName);
//      ps.setString(2, dataset);
//
//      ResultSet rs = ps.executeQuery();
//      if (!rs.next()) {
//        // will only get one result
//        rs.close();
//        conn.close();
//        return null;
//      }
//
//      byte[] stream = rs.getBytes(1);
//      byte[] cstream = rs.getBytes(2);
//
//      rs.close();
//
//      InputStream rstream = null;
//      if (cstream != null)
//        rstream = new GZIPInputStream(new ByteArrayInputStream(cstream));
//      else
//        rstream = new ByteArrayInputStream(stream);
//
//      return dscutils.getDocumentForXMLStream(rstream);
//    } catch (SQLException e) {
//      throw new ConfigurationException(
//        "Caught SQL Exception during fetch of requested DatasetConfig: " + e.getMessage(),
//        e);
//    } catch (IOException e) {
//      throw new ConfigurationException(
//        "Caught IOException during fetch of requested DatasetConfig: " + e.getMessage(),
//        e);
//    } catch (ConfigurationException e) {
//      throw e;
//    } finally {
//      DetailedDataSource.close(conn);
//    }
//  }

//  private Document getDatasetConfigDocumentByDatasetDisplayNameOracle(String user, String dataset, String displayName)
//    throws ConfigurationException {
//
//    Connection conn = null;
//    try {
//      String metatable = getDSConfigTableFor(user);
//      String sql = GETDOCBYDNAMESELECT + metatable + GETDOCBYDNAMEWHERE;
//
//      if (logger.isLoggable(Level.FINE))
//        logger.fine(
//          "Using " + sql + " to get DatasetConfig for displayName " + displayName + "and dataset " + dataset + "\n");
//
//      conn = dsource.getConnection();
//      PreparedStatement ps = conn.prepareStatement(sql);
//      ps.setString(1, displayName);
//      ps.setString(2, dataset);
//
//      ResultSet rs = ps.executeQuery();
//      if (!rs.next()) {
//        // will only get one result
//        rs.close();
//        conn.close();
//        return null;
//      }
//
//      CLOB stream = (CLOB) rs.getClob(1);
//      BLOB cstream = (BLOB) rs.getBlob(2);
//
//      InputStream rstream = null;
//      if (cstream != null) {
//        rstream = new GZIPInputStream(cstream.getBinaryStream());
//      } else
//        rstream = stream.getAsciiStream();
//
//      Document ret = dscutils.getDocumentForXMLStream(rstream);
//      rstream.close();
//      rs.close();
//      return ret;
//    } catch (SQLException e) {
//      throw new ConfigurationException(
//        "Caught SQL Exception during fetch of requested DatasetConfig: " + e.getMessage(),
//        e);
//    } catch (IOException e) {
//      throw new ConfigurationException(
//        "Caught IOException during fetch of requested DatasetConfig: " + e.getMessage(),
//        e);
//    } finally {
//      DetailedDataSource.close(conn);
//    }
//  }

  /**
   * Get a message digest for a given DatasetConfig, given by dataset and internalName
   * @param user -- user for meta_configuration_[user] table, if null, meta_configuration is attempted
   * @param dataset -- dataset for which digest is requested
   * @param internalName -- internalName for DatasetConfig digest desired.
   * @return byte[] digest for given dataset and displayName
   * @throws ConfigurationException for all underlying Exceptions
   */
  public byte[] getDSConfigMessageDigestByDatasetInternalName(String user, String dataset, String internalName)
    throws ConfigurationException {
      
      DatasetConfig dsv = getDatasetConfigByDatasetInternalName(user, dataset, internalName);
      if (dsv == null)
        return null;
      
      return dsv.getMessageDigest();
  }

  private int getDSConfigEntryCountFor(String metatable, String dataset, String internalName, String displayName)
    throws ConfigurationException {
    String existSQL = EXISTSELECT + metatable + EXISTWHERE;
    if (logger.isLoggable(Level.FINE))
      logger.fine("Getting DSConfigEntryCount with SQL " + existSQL + "\n");

    int ret = 0;

    Connection conn = null;
    try {
      conn = dsource.getConnection();
      PreparedStatement ps = conn.prepareStatement(existSQL);
      ps.setString(1, internalName);
      ps.setString(2, displayName);
      ps.setString(3, dataset);

      ResultSet rs = ps.executeQuery();
      rs.next();
      ret = rs.getInt(1);
      rs.close();
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQL exception attempting to determing count of rows for "
          + internalName
          + ", "
          + displayName
          + " from "
          + metatable
          + "\n");
    } finally {
      DetailedDataSource.close(conn);
    }

    return ret;
  }

  /**
   * Removes all records in a given metatable for the given dataset, internalName and displayName.  
   * Throws an error if the rows deleted do not equal the number of rows obtained using DatabaseDatasetConfigAdaptor.getDSConfigEntryCountFor(). 
   * @param metatable - meta_configuration table to use to delete entries
   * @param dataset - dataset for DatasetConfig entries to delete from metatable
   * @param internalName - internalName of DatasetConfig entries to delete from metatable
   * @param displayName - displayName of DatasetConfig entries to delete from metatable
   * @throws ConfigurationException if number of rows to delete doesnt match number returned by getDSConfigEntryCountFor()
   */
  public void deleteOldDSConfigEntriesFor(String metatable, String dataset, String internalName, String displayName)
    throws ConfigurationException {

    String deleteSQL = DELETEOLDXML + metatable + DELETEOLDXMLWHERE;

    int rowstodelete = getDSConfigEntryCountFor(metatable, dataset, internalName, displayName);
    if (logger.isLoggable(Level.FINE))
      logger.fine("Deleting old DSConfigEntries with SQL " + deleteSQL + "\n");

    int rowsdeleted;

    Connection conn = null;
    try {
      conn = dsource.getConnection();
      PreparedStatement ds = conn.prepareStatement(deleteSQL);
      ds.setString(1, internalName);
      ds.setString(2, displayName);
      ds.setString(3, dataset);

      rowsdeleted = ds.executeUpdate();
      ds.close();
    } catch (SQLException e) {
      throw new ConfigurationException(
        "Caught SQLException during delete of old Entries for "
          + internalName
          + ", "
          + displayName
          + " in table "
          + metatable
          + "\n");
    } finally {
      DetailedDataSource.close(conn);
    }

    if (!(rowsdeleted == rowstodelete))
      throw new ConfigurationException(
        "Did not delete old XML data rows for " + internalName + ", " + displayName + "\n");
  }

  /**
    * Removes all records in a given metatable for the given dataset   
    * @param dataset - dataset for DatasetConfig entries to delete from metatable
    * @throws ConfigurationException if number of rows to delete doesnt match number returned by getDSConfigEntryCountFor()
    */

  public void deleteDatasetConfigsForDataset(String dataset) throws ConfigurationException {
    String deleteSQL = "delete from " + BASEMETATABLE + DELETEDATASETCONFIG;

    Connection conn = null;
    try {
      conn = dsource.getConnection();
      PreparedStatement ds = conn.prepareStatement(deleteSQL);
      ds.setString(1, dataset);
      ds.executeUpdate();
      ds.close();
    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQLException during delete\n");
    } finally {
      DetailedDataSource.close(conn);
    }
  }

  /**
	* Removes all records in a given metatable for the given dataset and internal name
	* @param dataset - dataset for DatasetConfig entries to delete from metatable
	* @param internalName - internal name for DatasetConfig entry to delete from metatable
	* @throws ConfigurationException if number of rows to delete doesnt match number returned by getDSConfigEntryCountFor()
	*/

  public void deleteDatasetConfigsForDatasetIntName(String dataset, String internalName, String user) throws ConfigurationException {
	String deleteSQL = "delete from " + BASEMETATABLE + DELETEDATASETCONFIG + DELETEINTERNALNAME;

	Connection conn = null;
	try {
	  conn = dsource.getConnection();
	  PreparedStatement ds = conn.prepareStatement(deleteSQL);
	  ds.setString(1, dataset);
	  ds.setString(2,internalName);
	  ds.executeUpdate();
	  ds.close();

      updateMartConfigForUser(user);
	  
	} catch (SQLException e) {
	  throw new ConfigurationException("Caught SQLException during delete\n");
	} finally {
	  DetailedDataSource.close(conn);
	}
  }

  /**
   * Get the correct DatasetConfig table for a given user in the Mart Database
   * stored in the given DetailedDataSource.
   * @param user -- user to retrieve a DatasetConfig table.  If user is null, or if meta_configuration_[user] does not exist
   *                returns DatabaseDatasetConfigUtils.BASEMETATABLE.
   * @return String meta table name
   * @throws ConfigurationException if both meta_configuration_[user] and DatabaseDatasetConfigUtils.BASEMETATABLE are absent, and for all underlying exceptions.
   */
  public String getDSConfigTableFor(String user) throws ConfigurationException {
    String metatable = BASEMETATABLE;
    //override if user not null
    if (datasetConfigUserTableExists(user))
      metatable += "_" + user;
    else {
      //if BASEMETATABLE doesnt exist, throw an exception
      if (!baseDSConfigTableExists()){
		Connection conn = null;
			try {
			  conn = dsource.getConnection();
			  String CREATE_SQL = new String();
			  if(dsource.getDatabaseType().equals("oracle")) {CREATE_SQL=ORACLE_CREATETABLESQL;}
			  else {CREATE_SQL = CREATEMETATABLESQL;}
			  
			  PreparedStatement ps = conn.prepareStatement(CREATE_SQL);
			  System.out.println ("create statement: "+CREATE_SQL);
			  ps.executeUpdate();	
			  conn.close();
			} catch (SQLException e) {
			  throw new ConfigurationException("Caught SQLException during create meta_configuration table\n" +e);
			}
      	
      }
    }

    return metatable;
  }

  public DatasetConfig getValidatedDatasetConfig(DatasetConfig dsv) throws SQLException, ConfigurationException {
    
  	
  	String catalog="";
    String schema = getSchema();
  	
  	

    DatasetConfig validatedDatasetConfig = new DatasetConfig(dsv, true, false);
    //want to copy existing Elements to the new Object as is
    String dset = validatedDatasetConfig.getDataset();
    boolean hasBrokenStars = false;
    String[] starbases = dsv.getStarBases();
    String[] validatedStars = new String[starbases.length];

    for (int i = 0, n = starbases.length; i < n; i++) {
      String starbase = starbases[i];
      String validatedStar = getValidatedStarBase(schema, catalog, starbase);

      if (!validatedStar.equals(starbase)) {
        hasBrokenStars = true;
        validatedDatasetConfig.removeMainTable(starbase);
      }

      validatedStars[i] = validatedStar;
    }

    if (hasBrokenStars) {
      validatedDatasetConfig.setStarsBroken();
      validatedDatasetConfig.addMainTables(validatedStars);
    }

    boolean hasBrokenPKeys = false;
    String[] pkeys = dsv.getPrimaryKeys();
    String[] validatedKeys = new String[pkeys.length];

    for (int i = 0, n = pkeys.length; i < n; i++) {
      String pkey = pkeys[i];
      String validatedKey = getValidatedPrimaryKey(schema, catalog, pkey);

      if (!validatedKey.equals(pkey)) {
        hasBrokenPKeys = true;
        validatedDatasetConfig.removePrimaryKey(pkey);
      }

      validatedKeys[i] = validatedKey;
    }

    if (hasBrokenPKeys) {
      validatedDatasetConfig.setPrimaryKeysBroken();
      validatedDatasetConfig.addPrimaryKeys(validatedKeys);
    }

    boolean hasBrokenDefaultFilters = false;
    
    List brokenFilters = new ArrayList();   

    boolean hasBrokenOptions = false;
    Option[] options = dsv.getOptions();
    HashMap brokenOptions = new HashMap();

    for (int i = 0, n = options.length; i < n; i++) {
      Option validatedOption = getValidatedOption(schema, catalog, options[i], dset);

      if (validatedOption.isBroken()) {
        hasBrokenOptions = true;
        brokenOptions.put(new Integer(i), validatedOption);
      }
    }

    if (hasBrokenOptions) {
      validatedDatasetConfig.setOptionsBroken();

      for (Iterator iter = brokenOptions.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        Option brokenOption = (Option) brokenOptions.get(position);

        validatedDatasetConfig.removeOption(options[position.intValue()]);
        validatedDatasetConfig.insertOption(position.intValue(), brokenOption);
      }
    }

    boolean hasBrokenAttributePages = false;
    AttributePage[] apages = dsv.getAttributePages();
    HashMap brokenAPages = new HashMap();

    for (int i = 0, n = apages.length; i < n; i++) {
      AttributePage validatedPage = getValidatedAttributePage(apages[i], dset);

      if (validatedPage.isBroken()) {
        hasBrokenAttributePages = true;
        brokenAPages.put(new Integer(i), validatedPage);
      }
    }

    if (hasBrokenAttributePages) {
      validatedDatasetConfig.setAttributePagesBroken();

      for (Iterator iter = brokenAPages.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        AttributePage brokenAPage = (AttributePage) brokenAPages.get(position);

        validatedDatasetConfig.removeAttributePage(apages[position.intValue()]);
        validatedDatasetConfig.insertAttributePage(position.intValue(), brokenAPage);
      }
    }

    boolean hasBrokenFilterPages = false;
    HashMap brokenFPages = new HashMap();
    FilterPage[] allPages = dsv.getFilterPages();
    for (int i = 0, n = allPages.length; i < n; i++) {
      FilterPage validatedPage = getValidatedFilterPage(allPages[i], dset);

      if (validatedPage.isBroken()) {
        hasBrokenFilterPages = true;
        brokenFPages.put(new Integer(i), validatedPage);
      }
    }

    if (hasBrokenFilterPages) {
      validatedDatasetConfig.setFilterPagesBroken();

      for (Iterator iter = brokenFPages.keySet().iterator(); iter.hasNext();) {
        Integer position = (Integer) iter.next();
        FilterPage brokenPage = (FilterPage) brokenFPages.get(position);

        validatedDatasetConfig.removeFilterPage(allPages[position.intValue()]);
        validatedDatasetConfig.insertFilterPage(position.intValue(), brokenPage);
      }
    }

    return validatedDatasetConfig;
  }

  

  private String getValidatedStarBase(String schema, String catalog, String starbase) throws SQLException {
    String validatedStarBase = new String(starbase);

    //String table = starbase + "%" + MAINTABLESUFFIX;
    String table = starbase;
    boolean isBroken = true;

    Connection conn = null;

    try {
      conn = dsource.getConnection();
      ResultSet rs = conn.getMetaData().getTables(catalog, schema, table, null);
      while (rs.next()) {
        String thisTable = rs.getString(3);
        if (thisTable.toLowerCase().startsWith(starbase.toLowerCase())) {
          isBroken = false;
          break;
        } else {
          if (logger.isLoggable(Level.FINE))
            logger.fine("Recieved table " + thisTable + " when querying for " + table + "\n");
        }
      }
    } finally {
      DetailedDataSource.close(conn);
    }

    if (isBroken) {
      validatedStarBase += DOESNTEXISTSUFFIX;
    }
    return validatedStarBase;
  }

  private String getValidatedPrimaryKey(String schema, String catalog, String primaryKey) throws SQLException {
    String validatedPrimaryKey = new String(primaryKey);

    String tablePattern = "%" + MAINTABLESUFFIX;
    
	if(dsource.getDatabaseType().equals("oracle")) tablePattern=tablePattern.toUpperCase();
        //System.out.println("databaseType() "+dsource.getDatabaseType());        
 
    boolean isBroken = true;

    Connection conn = null;

    try {
      conn = dsource.getConnection();
      ResultSet columns = conn.getMetaData().getColumns(catalog, schema, tablePattern, primaryKey);
      while (columns.next()) {
        String thisColumn = columns.getString(4);
        if (thisColumn.equalsIgnoreCase(primaryKey)) {
          isBroken = false;
          break;
        } else {
          if (logger.isLoggable(Level.FINE))
            logger.fine("Recieved column " + thisColumn + " during query for primary key " + primaryKey + "\n");
        }
      }
    } finally {
      DetailedDataSource.close(conn);
    }
    if (isBroken)
      validatedPrimaryKey += DOESNTEXISTSUFFIX;

    return validatedPrimaryKey;
  }

  private FilterPage getValidatedFilterPage(FilterPage page, String dset) throws SQLException {
    FilterPage validatedPage = new FilterPage(page);

    boolean hasBrokenGroups = false;
    HashMap brokenGroups = new HashMap();

    List allGroups = page.getFilterGroups();
    for (int i = 0, n = allGroups.size(); i < n; i++) {
      Object group = allGroups.get(i);

      if (group instanceof FilterGroup) {
        //FilterGroup gr = (FilterGroup) group;
        //if ((gr.getInternalName().equals("expression")))
        //	continue;// hack for expression - breaks current code - needs fixing
        FilterGroup validatedGroup = getValidatedFilterGroup((FilterGroup) group, dset);

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
            allGroups.remove(position.intValue());
            allGroups.add(position.intValue(), brokenGroup);
          } //else not needed yet
        }
      }
    }

    return validatedPage;
  }

  private FilterGroup getValidatedFilterGroup(FilterGroup group, String dset) throws SQLException {
    FilterGroup validatedGroup = new FilterGroup(group);

    FilterCollection[] collections = group.getFilterCollections();

    boolean hasBrokenCollections = false;
    HashMap brokenCollections = new HashMap();

    for (int i = 0, n = collections.length; i < n; i++) {
      FilterCollection validatedCollection = getValidatedFilterCollection(collections[i], dset);

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

  private FilterCollection getValidatedFilterCollection(FilterCollection collection, String dset) throws SQLException {
    
    String catalog="";
    String schema = getSchema();
    
    
    
    FilterCollection validatedFilterCollection = new FilterCollection(collection);

    List allFilts = collection.getFilterDescriptions();

    boolean filtersValid = true;
    HashMap brokenFilts = new HashMap();

    for (int i = 0, n = allFilts.size(); i < n; i++) {
      Object element = allFilts.get(i);

      if (element instanceof FilterDescription) {
        FilterDescription validatedFilter =
          getValidatedFilterDescription(schema, catalog, (FilterDescription) element, dset);
        if (validatedFilter.isBroken()) {

          filtersValid = false;
          brokenFilts.put(new Integer(i), validatedFilter);
        }
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
          allFilts.remove(position.intValue());
          allFilts.add(position.intValue(), brokenFilter);
        } //else not needed yet
      }
    }

    return validatedFilterCollection;
  }

  private FilterDescription getValidatedFilterDescription(
    String schema,
    String catalog,
    FilterDescription filter,
    String dset)
    throws SQLException {
    FilterDescription validatedFilter = new FilterDescription(filter);
    //System.out.println("CHecking FILTER\t" + validatedFilter);
    if (filter.getField() != null) {
      //test
      boolean fieldValid = false;
      boolean tableValid = false;

      String field = filter.getField();
      String tableConstraint = filter.getTableConstraint();

      // if the tableConstraint is null, this field must be available in one of the main tables
      String table = (!tableConstraint.equals("main")) ? tableConstraint : dset + "%" + MAINTABLESUFFIX;
      
	  if(dsource.getDatabaseType().equals("oracle")) table=table.toUpperCase();
          //System.out.println("databaseType() "+dsource.getDatabaseType());          
 
      //String table = (tableConstraint != null) ? "%" + tableConstraint + "%" : "%" + MAINTABLESUFFIX;
      Connection conn = dsource.getConnection();
      
      ResultSet rs = conn.getMetaData().getColumns(catalog, schema, table, field);
      while (rs.next()) {
        String columnName = rs.getString(4);
        String tableName = rs.getString(3);
        boolean[] valid = isValidDescription(columnName, field, tableName, tableConstraint);
        fieldValid = valid[0];
        tableValid = valid[1];

        if (valid[0] && valid[1]) {
          //System.out.println(columnName + "\t" + tableName + "\t" + field);
          break;
        }
      }
      conn.close();
      if (!(fieldValid) || !(tableValid)) {
        validatedFilter.setHidden("true");

      } else if (validatedFilter.getHidden() != null && validatedFilter.getHidden().equals("true")) {

        validatedFilter.setHidden("false");
        validatedFilter.setFieldBroken(); //so gets changed in the update
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
        Option validatedOption = getValidatedOption(schema, catalog, options[j], dset);
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

  private Option getValidatedOption(String schema, String catalog, Option option, String dset) throws SQLException {
    Option validatedOption = new Option(option);
    // hack to ignore the expression drop down menu
    if (option.getType().equals("tree"))
      return validatedOption;
    if (option.getField() != null) {
      //test
      boolean fieldValid = false;
      boolean tableValid = false;

      String field = option.getField();
      String tableConstraint = option.getTableConstraint();

      // if the tableConstraint is null, this field must be available in one of the main tables
      String table = (!tableConstraint.equals("main")) ? tableConstraint : dset + "%" + MAINTABLESUFFIX;
      //String table = (tableConstraint != null) ? "%" + tableConstraint + "%" : "%" + MAINTABLESUFFIX;
      
	  if(dsource.getDatabaseType().equals("oracle")) table=table.toUpperCase();
          //System.out.println("databaseType() "+dsource.getDatabaseType());          

     
      Connection conn = dsource.getConnection();
      ResultSet rs = conn.getMetaData().getColumns(catalog, schema, table, field);
      while (rs.next()) {
        String columnName = rs.getString(4);
        String tableName = rs.getString(3);

        boolean[] valid = isValidDescription(columnName, field, tableName, tableConstraint);
        fieldValid = valid[0];
        tableValid = valid[1];

        if (valid[0] && valid[1])
          break;
      }
      conn.close();

      if (!(fieldValid) || !(tableValid)) {
        //System.out.println("CHNAGING OPTION\t" + validatedOption);
        validatedOption.setHidden("true");

      } else if (validatedOption.getHidden() != null && validatedOption.getHidden().equals("true")) {
        validatedOption.setHidden("false");
        validatedOption.setFieldBroken(); //so gets changed in the update
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
        Option validatedSubOption = getValidatedOption(schema, catalog, options[j], dset);
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
        PushAction validatedAction = getValidatedPushAction(schema, catalog, pas[j], dset);
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

  private PushAction getValidatedPushAction(String schema, String catalog, PushAction action, String dset)
    throws SQLException {
    PushAction validatedPushAction = new PushAction(action);

    boolean optionsValid = true;
    HashMap brokenOptions = new HashMap();

    Option[] options = action.getOptions();
    for (int j = 0, m = options.length; j < m; j++) {
      Option validatedSubOption = getValidatedOption(schema, catalog, options[j], dset);
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

  private AttributePage getValidatedAttributePage(AttributePage page, String dset) throws SQLException {
    AttributePage validatedPage = new AttributePage(page);

    boolean hasBrokenGroups = false;
    HashMap brokenGroups = new HashMap();

    List allGroups = page.getAttributeGroups();
    for (int i = 0, n = allGroups.size(); i < n; i++) {
      Object group = allGroups.get(i);

      if (group instanceof AttributeGroup) {
        AttributeGroup validatedGroup = getValidatedAttributeGroup((AttributeGroup) group, dset);

        if (validatedGroup.isBroken()) {
          hasBrokenGroups = true;
          //brokenGroups.put(new Integer(i), group);
          brokenGroups.put(new Integer(i), validatedGroup);
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
          allGroups.remove(position.intValue());
          allGroups.add(position.intValue(), brokenGroup);

        } //else not needed
      }
    }

    return validatedPage;
  }

  private AttributeGroup getValidatedAttributeGroup(AttributeGroup group, String dset) throws SQLException {
    AttributeGroup validatedGroup = new AttributeGroup(group);

    boolean hasBrokenCollections = false;
    HashMap brokenCollections = new HashMap();

    AttributeCollection[] collections = group.getAttributeCollections();
    for (int i = 0, n = collections.length; i < n; i++) {
      AttributeCollection validatedCollection = getValidatedAttributeCollection(collections[i], dset);

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

  private AttributeCollection getValidatedAttributeCollection(AttributeCollection collection, String dset)
    throws SQLException {
    
    String catalog = "";
    String schema=getSchema();
    
   
    AttributeCollection validatedAttributeCollection = new AttributeCollection(collection);
    boolean hasBrokenAttributes = false;
    HashMap brokenAtts = new HashMap();

    List allAtts = collection.getAttributeDescriptions();
    for (int i = 0, n = allAtts.size(); i < n; i++) {
      Object attribute = allAtts.get(i);

      if (attribute instanceof AttributeDescription) {
        AttributeDescription validatedAttributeDescription =
          getValidatedAttributeDescription(schema, catalog, (AttributeDescription) attribute, dset);

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
          allAtts.remove(position.intValue());
          allAtts.add(position.intValue(), brokenAtt);
        } //else not needed yet
      }
    }

    return validatedAttributeCollection;
  }

  private AttributeDescription getValidatedAttributeDescription(
    String schema,
    String catalog,
    AttributeDescription description,
    String dset)
    throws SQLException {
    AttributeDescription validatedAttribute = new AttributeDescription(description);

    boolean fieldValid = false;
    boolean tableValid = false;

    String field = description.getField();
    // oracle case sensitive
    if(dsource.getDatabaseType().equals("oracle")) field=field.toUpperCase();
   //System.out.println("databaseType() "+dsource.getDatabaseType()); 
   
    String tableConstraint = description.getTableConstraint();

    
    
	// have placeholders for attributes in XML now with no info other than internal_name
	if (tableConstraint == null){
		return validatedAttribute;
		
	}


    // if the tableConstraint is null, this field must be available in one of the main tables
    //String table = (!tableConstraint.equals("main")) ? tableConstraint : dset + "%" + MAINTABLESUFFIX;
    String table = (!tableConstraint.equals("main")) ? tableConstraint : dset + "%" + MAINTABLESUFFIX;
    
	if(dsource.getDatabaseType().equals("oracle")) table=table.toUpperCase();
        //System.out.println("databaseType() "+dsource.getDatabaseType());   


 
    Connection conn = dsource.getConnection();
    catalog=null;
    //schema=null;
    //System.out.println("schema "+schema + " catalog: "+catalog+ " table "+ table+ " field "+field);
    ResultSet rs = conn.getMetaData().getColumns(catalog, schema, table, field);
    
    
    
    
    while (rs.next()) {
      String columnName = rs.getString(4);
      String tableName = rs.getString(3);
      
      
      boolean[] valid = isValidDescription(columnName, field, tableName, tableConstraint);
      fieldValid = valid[0];
      tableValid = valid[1];
      if (valid[0] && valid[1]) {

        break;
      }
    }

    conn.close();
    if (!(fieldValid) || !(tableValid)) {
      validatedAttribute.setHidden("true");

    } else if (validatedAttribute.getHidden() != null && validatedAttribute.getHidden().equals("true")) {
      validatedAttribute.setHidden("false");
      validatedAttribute.setFieldBroken(); //so gets changed in the update
    }

    if (!(fieldValid && tableValid)) {

      validatedAttribute.setFieldBroken();
      validatedAttribute.setTableConstraintBroken();
    }

    return validatedAttribute;
  }

  private boolean[] isValidDescription(
    String columnName,
    String descriptionField,
    String tableName,
    String descriptionTableConstraint) {
    boolean[] validFlags = new boolean[] { false, false };

    // oracle case mismatch
    if (columnName.toLowerCase().equals(descriptionField.toLowerCase())) {
      validFlags[0] = true;

      if (descriptionTableConstraint != null) {
        if (tableName.toLowerCase().indexOf(descriptionTableConstraint.toLowerCase()) > -1) {
          validFlags[1] = true;
        } else {
        	        	
        	
          if (logger.isLoggable(Level.FINE))
            logger.fine(
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
      if (logger.isLoggable(Level.FINE))
        logger.fine(
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
   * Returns a list of potential (Naive) dataset names from a given Mart compliant database hosted on a given DetailedDataSource.
   * These names can be used as an argument to getNaiveMainTablesFor, getNaiveDimensionTablesFor and getNaiveDatasetConfigFor.
   * @param databaseName -- name of the RDBMS instance to search for potential datasets
   * @return String[] of potential dataset names
   * @throws SQLException
   */
  public String[] getNaiveDatasetNamesFor(String databaseName) throws SQLException {
    String[] potentials = getNaiveMainTablesFor(databaseName, null);

    //now weed them to a subset, attempting to unionize conformed dimension names
    //List retList = new ArrayList();
    Set retSet = new HashSet();

    for (int i = 0, n = potentials.length; i < n; i++) {
      String curval = potentials[i];

      retSet.add(curval.replaceFirst("__.+__[Mm][Aa][Ii][Nn]", ""));
    }

    String[] dsList = new String[retSet.size()];
    retSet.toArray(dsList);
    Arrays.sort(dsList);
    return dsList;
  }

  /**
   * Retruns a String[] of possible main tables for a given Mart Compliant database, hosted on a given 
   * RDBMS, with an (optional) datasetName to key upon.  With no datasetName, all possible main tables from 
   * the database are returned.
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNaiveDatasetNamesFor, or null)
   * @return String[] of potential main table names
   * @throws SQLException
   */
  public String[] getNaiveMainTablesFor(String databaseName, String datasetName) throws SQLException {
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
	//String tablePattern = (datasetName != null) ? datasetName + "__%": "%";
    
    tablePattern += MAINTABLESUFFIX;
    String capTablePattern = tablePattern.toUpperCase();

    //get all main tables

    if (dsource.getDatabaseType().equals("oracle")) {
    //System.out.println("databaseType() "+dsource.getDatabaseType());


      //System.out.println("database type: " + dsource.getDatabaseType());

      //ResultSet rsSch = 
      //while (rsSch.next()) {
        String databaseName2 = getSchema();

        //first search for tablePattern    
        ResultSet rsTab = dmd.getTables(null, databaseName2, tablePattern, null);

        while (rsTab.next()) {
          String tableName = rsTab.getString(3);
          potentials.add(tableName);
        }
        rsTab.close();

        //now try capitals, should NOT get mixed results
        rsTab = dmd.getTables(null, databaseName2, capTablePattern, null);
        while (rsTab.next()) {
          String tableName = rsTab.getString(3);
          //NN
          //System.out.println(tableName);

          if (!potentials.contains(tableName))
            potentials.add(tableName);
        }
        rsTab.close();
      //}
      //rsSch.close();
    } else {


	   
      //====
      //first search for tablePattern    
      ResultSet rsTab = dmd.getTables(null, databaseName, tablePattern, null);

      while (rsTab.next()) {
        String tableName = rsTab.getString(3);
		String tableDataset = tableName.split("__")[0];
		if (datasetName == null || tableDataset.equals(datasetName)){
			potentials.add(tableName);
		}
      }
      rsTab.close();

      //now try capitals, should NOT get mixed results
      rsTab = dmd.getTables(null, databaseName, capTablePattern, null);
      while (rsTab.next()) {
        String tableName = rsTab.getString(3);

        if (!potentials.contains(tableName)){	
			String tableDataset = tableName.split("__")[0];
			if (datasetName == null || tableDataset.equals(datasetName)){
				potentials.add(tableName);
			}    	
        }
      }
      rsTab.close();

    }
    conn.close();

    String[] retList = new String[potentials.size()];
    potentials.toArray(retList);
    //Arrays.sort(retList);
    return retList;
  }

  /**
   * Retruns a String[] of main tables sorted on the number of join keys they contain 
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNaiveDatasetNamesFor, or null)
   * @return String[] of potential main table names
   * @throws SQLException
   */
  public String[] sortNaiveMainTables(String[] mainTables, String databaseName) throws SQLException {
    
    if (mainTables.length == 1){// no need to sort a single table
    	return mainTables;
    }
    List sortedMainTables = new ArrayList();
    int resolution = 1;
    int numberKeys;
    while (sortedMainTables.size() < mainTables.length) {
      for (int i = 0, n = mainTables.length; i < n; i++) {
        numberKeys = 0;
        String tableName = mainTables[i];
        
        
        
        TableDescription table = getTableDescriptionFor(databaseName, tableName);
        //System.out.println("DATABASE NAME "+databaseName);
        
        for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
          ColumnDescription column = table.columnDescriptions[j];
          String cname = column.name;
          
          //System.out.println("COLUMN "+cname);
          
          //NN
          if (cname.endsWith("_KEY") || cname.endsWith("_key"))
            //if (cname.endsWith("_key"))
            numberKeys++;
        }
        if (numberKeys == resolution) {
          sortedMainTables.add(tableName);
          resolution++;
          break;
        }
      
      
        //System.out.println("before tttable descripiton table "+tableName+ " sorted length "+ 
        //		sortedMainTables.size() +"< "+mainTables.length+ " resolution "+resolution +
        //		"number  of keys"+ numberKeys);
      
      
      
      }
      // incase first main table has 2 keys
      if (sortedMainTables.size() < 1)
        resolution++;
    
    
    
    
    }
    String[] retList = new String[sortedMainTables.size()];
    sortedMainTables.toArray(retList);
    return retList;
  }

  /**
   * Returns a String[] of potential dimension tables from a given Mart Compliant database, hosted on a
   * given RDBMS, constrained to an (optional) dataset.
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNaiveDatasetNamesFor, or null)
   * @return String[] of potential dimension table names
   * @throws SQLException
   */
  public String[] getNaiveDimensionTablesFor(String databaseName, String datasetName) throws SQLException {
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
	//String tablePattern = (datasetName != null) ? datasetName : "%";
    
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
   * Returns a String[] of potential lookup tables from a given Mart Compliant database, hosted on a
   * given RDBMS, constrained to an (optional) dataset.
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNaiveDatasetNamesFor, or null)
   * @return String[] of potential lookup table names
   * @throws SQLException
   */
  public String[] getNaiveLookupTablesFor(String databaseName, String datasetName) throws SQLException {
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
    //String tablePattern = (datasetName != null) ? datasetName + "%" : "%";

    String tablePattern = "%";

    tablePattern += LOOKUPTABLESUFFIX;
    String capTablePattern = tablePattern.toUpperCase();

    //get all lookup tables
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

    // String tablePattern = (datasetName != null) ? datasetName + "%" : "%"; 

    String[] potList = new String[potentials.size()];
    potentials.toArray(potList);

    Set finals = new TreeSet();

    for (int k = 0; k < potList.length; k++) {

      String pat = potList[k].split("__")[0];
      if (pat.equals("global") || datasetName.matches(pat + ".*")) {
        finals.add(potList[k]);
      }

    }

    String[] retList = new String[finals.size()];
    finals.toArray(retList);
    return retList;
  }

  /**
   * Returns a TableDescription object describing a particular table in a given database,
   * hosted on a given RDBMS.
   * @param databaseName -- name of the RDBMS instance housing the requested table
   * @param tableName -- name of the desired table, as might be returned by a call to getXXXTablesFor
   * @return TableDescription object describing the table
   * @throws SQLException
   */
  public TableDescription getTableDescriptionFor(String databaseName, String tableName) throws SQLException {
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
   * Returns a Naive DatasetConfig for a given dataset within a given Mart Compliant database, hosted on a given
   * RDBMS.  This will consist of an unordered set of starbases, no primary keys, and one FilterPage and AttributePage
   * containing one group with Collections for each table containing descriptions for each field.  Filters are only
   * described for main tables, and no Option grouping is attempted.  All Descriptions are fully constrained to
   * a tableConstraint.  Note, this method is likely to undergo extensive revisions as we optimize the Mart Compliant
   * Schema.
   * @param databaseName -- name of the RDBMS instance to search for potential tables
   * @param datasetName -- name of the dataset to constrain the search (can be a result of getNaiveDatasetNamesFor, or null)
   * @return DatasetConfig nievely generated
   * @throws ConfigurationException
   * @throws SQLException
   */
  public DatasetConfig getNaiveDatasetConfigFor(String databaseName, String datasetName)
    throws ConfigurationException, SQLException {
    DatasetConfig dsv = new DatasetConfig();

    //dsv.setInternalName(datasetName);
    dsv.setInternalName("default");
    dsv.setDisplayName(datasetName + " ( " + databaseName + " )");
    dsv.setDataset(datasetName);
    dsv.setType("TableSet");
    dsv.setVisible("1");
    

    AttributePage ap = new AttributePage();
    ap.setInternalName("feature_page");
    ap.setDisplayName("ATTRIBUTES");

    FilterPage fp = new FilterPage();
    fp.setInternalName("filters");
    fp.setDisplayName("FILTERS");

    AttributeGroup ag = new AttributeGroup();
    ag.setInternalName("features");
    ag.setDisplayName("FEATURES");

    FilterGroup fg = new FilterGroup();
    fg.setInternalName("filters");
    fg.setDisplayName("FILTERS");

    //need to sort starbases in order of the number of keys they contain
    //primaryKeys should be in this same order

    List starbases = new ArrayList();
    List finalStarbases = new ArrayList(); 
    starbases.addAll(Arrays.asList(sortNaiveMainTables(getNaiveMainTablesFor(databaseName, datasetName), databaseName)));
    List primaryKeys = new ArrayList();
    for (int i = 0, n = starbases.size(); i < n; i++) {
      String tableName = (String) starbases.get(i);
      //System.out.println("getting table name "+tableName);
      // can have single main tables with no keys so add here now
	  finalStarbases.add(starbases.get(i));
      
      TableDescription table = getTableDescriptionFor(databaseName, tableName);
      for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
        ColumnDescription column = table.columnDescriptions[j];
        String cname = column.name;
        //NN added uppercase   
        //if (cname.endsWith("_key") && (!primaryKeys.contains(cname)))
        if ((cname.endsWith("_key") || (cname.endsWith("_KEY"))) && (!primaryKeys.contains(cname))){
          primaryKeys.add(cname);
          // fix for Star schema - multiple keys per l main table
          //finalStarbases.add(starbases.get(i));
        }
      }
    }

    String[] sbases = new String[finalStarbases.size()];
    finalStarbases.toArray(sbases);
    dsv.addMainTables(sbases);

    String[] pkeys = new String[primaryKeys.size()];
    if (pkeys.length > 0){
    	primaryKeys.toArray(pkeys);
    	dsv.addPrimaryKeys(pkeys);
    }
    
    List allTables = new ArrayList();
    allTables.addAll(starbases);
    allTables.addAll(Arrays.asList(getNaiveDimensionTablesFor(databaseName, datasetName)));
    allTables.addAll(Arrays.asList(getNaiveLookupTablesFor(databaseName, datasetName)));
    List allCols = new ArrayList();

    // ID LIST FC and FDs
    FilterCollection fcList = new FilterCollection();
    fcList.setInternalName("id_list");
    fcList.setDisplayName("ID LIST");

    FilterDescription fdBools = new FilterDescription();
    fdBools.setInternalName("id_list_filters");
    fdBools.setType("boolean_list");
    FilterDescription fdLists = new FilterDescription();
    fdLists.setInternalName("id_list_limit_filters");
    fdLists.setType("id_list");

    for (int i = 0, n = allTables.size(); i < n; i++) {
      String tableName = (String) allTables.get(i);
      //System.out.println ("Second time tablename "+ tableName);
      String content = null;
      String fullTableName = tableName;
      String[] tableTokenizer = tableName.split("__");
      content = tableTokenizer[1];

      AttributeCollection ac = null;
      if (!isLookupTable(tableName)) {
        ac = new AttributeCollection();
        ac.setInternalName(content);
        ac.setDisplayName(content.replaceAll("_", " "));
      }

      FilterCollection fc = null;
      if (isMainTable(tableName)) {

        fc = new FilterCollection();
        fc.setInternalName(content);
        fc.setDisplayName(content.replaceAll("_", " "));
      }

      TableDescription table = getTableDescriptionFor(databaseName, tableName);

      // need to find the lowest joinKey for table first;
      String joinKey = null;
      outer : for (int k = pkeys.length - 1; k > -1; k--) {
        for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
          ColumnDescription column = table.columnDescriptions[j];
          String cname = column.name;
          if (cname.equals(pkeys[k])) {
            joinKey = cname;
            break outer;
          }
        }
      }

      for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
        ColumnDescription column = table.columnDescriptions[j];

        //NN 
        String cname = column.name.toLowerCase();
        //String cname = column.name;

        // ignore the key columns as atts and filters
        if (cname.endsWith("_key"))
          continue;

        // if the column already seen in a higher resolution
        // main table ignore

        if (isMainTable(tableName) && allCols.contains(cname))
          continue;

        int ctype = column.javaType; // java generalized type across all JDBC instances
        //String ctype = column.dbType; //as in RDBMS table definition
        int csize = column.maxLength;

        if (logger.isLoggable(Level.FINE))
          logger.fine(tableName + ": " + cname + "-- type : " + ctype + "\n");

        if (isMainTable(tableName) || isDimensionTable(tableName)) {
//System.out.println ("tableName before AllNULL "+tableName);
          if (isAllNull(cname, fullTableName))
            continue;

          if (isMainTable(tableName)) {
            tableName = "main";
             //System.out.println("Resetting table name to: "+ tableName);

            allCols.add(cname);
            if (!cname.endsWith("_bool"))
              fc.addFilterDescription(getFilterDescription(cname, tableName, ctype, joinKey, dsv));
            else {
              FilterDescription fdBool = getFilterDescription(cname, tableName, ctype, joinKey, dsv);

              Option opBool = new Option(fdBool);
              fdBools.addOption(opBool);

            }
          }
          if (!cname.endsWith("_bool")) {
            AttributeDescription ad = getAttributeDescription(cname, tableName, csize, joinKey);
            //ad.setHidden("false");
            ac.addAttributeDescription(ad);
            if (cname.endsWith("_list")) {

              FilterDescription fdList = getFilterDescription(cname, tableName, ctype, joinKey, dsv);
              Option op = new Option(fdList);
              fdLists.addOption(op);

            }
          }

        } else if (isLookupTable(tableName)) {
          if (cname.startsWith("glook_") || cname.startsWith("silent_")) {
            if (fc == null) {
              fc = new FilterCollection();
              fc.setInternalName(content);
              fc.setDisplayName(content.replaceAll("_", " "));
            }
            fc.addFilterDescription(getFilterDescription(cname, tableName, ctype, joinKey, dsv));

          }
        } else {
          if (logger.isLoggable(Level.FINE))
            logger.fine("Skipping " + tableName + "\n");
        }
      }
	  if (ac != null && ac.getAttributeDescriptions().size() > 0)
        ag.addAttributeCollection(ac);

      if (fc != null && fc.getFilterDescriptions().size() > 0)
        fg.addFilterCollection(fc);
    }
    if (fdBools != null && fdBools.getOptions().length > 0)
       fcList.addFilterDescription(fdBools);
    if (fdLists != null && fdLists.getOptions().length > 0)
        fcList.addFilterDescription(fdLists);
    if (fcList != null && fcList.getFilterDescriptions().size() > 0)
        fg.addFilterCollection(fcList);
    if (ag != null && ag.getAttributeCollections().length > 0)
      ap.addAttributeGroup(ag);
    if (fg != null && fg.getFilterCollections().length > 0)
      fp.addFilterGroup(fg);
    if (ap != null && ap.getAttributeGroups().size() > 0)
      dsv.addAttributePage(ap);
    if (fp != null && fp.getFilterGroups().size() > 0)
      dsv.addFilterPage(fp);

    return dsv;
  }

  public DatasetConfig getNewFiltsAtts(String databaseName, DatasetConfig dsv)
    throws ConfigurationException, SQLException {

  	if (dsource.getDatabaseType().equals("oracle")) databaseName=getSchema();
  //System.out.println("databaseType() "+dsource.getDatabaseType());	
  	
  	
    String datasetName = dsv.getDataset();

    AttributePage ap = new AttributePage();
    ap.setInternalName("new_attributes");
    ap.setDisplayName("NEW_ATTRIBUTES");

    FilterPage fp = new FilterPage();
    fp.setInternalName("new_filters");
    fp.setDisplayName("NEW_FILTERS");

    AttributeGroup ag = new AttributeGroup();
    ag.setInternalName("new_attributes");
    ag.setDisplayName("NEW_ATTRIBUTES");

    FilterGroup fg = new FilterGroup();
    fg.setInternalName("new_filters");
    fg.setDisplayName("NEW_FILTERS");

    //need to sort starbases in order of the number of keys they contain
    //primaryKeys should be in this same order

    List starbases = new ArrayList();
    starbases.addAll(Arrays.asList(sortNaiveMainTables(getNaiveMainTablesFor(databaseName, datasetName), databaseName)));
    
    List primaryKeys = new ArrayList();

    for (int i = 0, n = starbases.size(); i < n; i++) {
      String tableName = (String) starbases.get(i);

     TableDescription table = getTableDescriptionFor(databaseName, tableName);

      for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
        ColumnDescription column = table.columnDescriptions[j];
        String cname = column.name;
        if (cname.endsWith("_key") && (!primaryKeys.contains(cname)))
          primaryKeys.add(cname);
      }
    }

    String[] sbases = new String[starbases.size()];
    starbases.toArray(sbases);
    dsv.addMainTables(sbases);

    String[] pkeys = new String[primaryKeys.size()];
    primaryKeys.toArray(pkeys);
    dsv.addPrimaryKeys(pkeys);

    List allTables = new ArrayList();
    allTables.addAll(starbases);
    allTables.addAll(Arrays.asList(getNaiveDimensionTablesFor(databaseName, datasetName)));
    allTables.addAll(Arrays.asList(getNaiveLookupTablesFor(databaseName, datasetName)));
    List allCols = new ArrayList();

    // ID LIST FC and FDs
    FilterCollection fcList = new FilterCollection();
    fcList.setInternalName("id_list");
    fcList.setDisplayName("ID LIST");

    FilterDescription fdBools = new FilterDescription();
    fdBools.setInternalName("id_list_filters");
    fdBools.setType("boolean_list");
    FilterDescription fdLists = new FilterDescription();
    fdLists.setInternalName("id_list_limit_filters");
    fdLists.setType("id_list");

    for (int i = 0, n = allTables.size(); i < n; i++) {
      String tableName = (String) allTables.get(i);
      String content = null;
      String fullTableName = tableName;
      String[] tableTokenizer = tableName.split("__");
      content = tableTokenizer[1];

      AttributeCollection ac = null;
      if (!isLookupTable(tableName)) {
        ac = new AttributeCollection();
        ac.setInternalName(content);
        ac.setDisplayName(content.replaceAll("_", " "));
      }

      FilterCollection fc = null;
      if (isMainTable(tableName)) {

        fc = new FilterCollection();
        fc.setInternalName(content);
        fc.setDisplayName(content.replaceAll("_", " "));
      }

      TableDescription table = getTableDescriptionFor(databaseName, tableName);

      // need to find the lowest joinKey for table first;
      String joinKey = null;
      outer : for (int k = pkeys.length - 1; k > -1; k--) {
        for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
          ColumnDescription column = table.columnDescriptions[j];
          String cname = column.name;
          if (cname.equals(pkeys[k])) {
            joinKey = cname;
            break outer;
          }
        }
      }
    
      for (int j = 0, m = table.columnDescriptions.length; j < m; j++) {
        ColumnDescription column = table.columnDescriptions[j];

        //String cname = column.name;
		String cname = column.name.toLowerCase();
        // ignore the key columns as atts and filters
        if (cname.endsWith("_key"))
          continue;

        // if the column already seen in a higher resolution
        // main table ignore

        if (isMainTable(tableName) && allCols.contains(cname))
          continue;

        int ctype = column.javaType; // java generalized type across all JDBC instances
        //String ctype = column.dbType; //as in RDBMS table definition
        int csize = column.maxLength;
        if (logger.isLoggable(Level.FINE))
          logger.fine(tableName + ": " + cname + "-- type : " + ctype + "\n");

        if (isMainTable(tableName) || isDimensionTable(tableName)) {
          if (isAllNull(cname, fullTableName))
            continue;

          if (isMainTable(tableName)){
            tableName = "main";
            allCols.add(cname);
            if (!cname.endsWith("_bool")) {
              FilterDescription currFilt = null;
              if (dsv.getFilterDescriptionByFieldNameTableConstraint(cname, tableName, null) != null)

              	//System.out.println("cname "+ cname+ " tableName " + tableName);
              	currFilt = dsv.getFilterDescriptionByFieldNameTableConstraint(cname, tableName,null);

              if (currFilt == null) {
                fc.addFilterDescription(getFilterDescription(cname, tableName, ctype, joinKey, dsv));
//System.out.println("Going to null ");
              }
              else { // update options if has any

              if (currFilt.hasOptions())
                  updateDropDown(dsv, currFilt); 
              }

            } else { // is a main table bool filter
              FilterDescription fdBool = getFilterDescription(cname, tableName, ctype, joinKey, dsv);

              Option opBool = new Option(fdBool);
              boolean newOption = true;

              // cycle through all options looking for a match
              FilterPage[] fps = dsv.getFilterPages();
              outer : for (int k = 0; k < fps.length; k++) {
                List fds = new ArrayList();
                fds = fps[k].getAllFilterDescriptions();
                for (int l = 0; l < fds.size(); l++) {
                  FilterDescription fdCurrent = (FilterDescription) fds.get(l);
                  Option[] ops = fdCurrent.getOptions();
                  for (int p = 0, q = ops.length; p < q; p++) {
                    if ((ops[p].getField() != null && ops[p].getField().equals(cname))
                      && (ops[p].getTableConstraint() != null && ops[p].getTableConstraint().equals(tableName))) {
                      newOption = false;
                      break outer;
                    }
                  }
                }
              }

              // could be present as a FD as well
              FilterDescription currFilt = null;
              if (dsv.getFilterDescriptionByFieldNameTableConstraint(cname, tableName,null) != null)
                currFilt = dsv.getFilterDescriptionByFieldNameTableConstraint(cname, tableName,null);
              if (currFilt != null)
                newOption = false;

              if (newOption) // option with this field and table name doesn't already exist
                fdBools.addOption(opBool);

            }
          }
          if (!cname.endsWith("_bool")) {
            AttributeDescription ad = getAttributeDescription(cname, tableName, csize, joinKey);

            if (dsv.getAttributeDescriptionByFieldNameTableConstraint(cname, tableName) == null) {
              ac.addAttributeDescription(ad);
            }
            if (cname.endsWith("_list")) {

              FilterDescription fdList = getFilterDescription(cname, tableName, ctype, joinKey, dsv);
              Option op = new Option(fdList);

              boolean newOption = true;

              // cycle through all options looking for a match
              FilterPage[] fps = dsv.getFilterPages();
              outer : for (int k = 0; k < fps.length; k++) {
                List fds = new ArrayList();
                fds = fps[k].getAllFilterDescriptions();
                for (int l = 0; l < fds.size(); l++) {
                  FilterDescription fdCurrent = (FilterDescription) fds.get(l);
                  Option[] ops = fdCurrent.getOptions();
                  for (int p = 0, q = ops.length; p < q; p++) {
                    if ((ops[p].getField() != null && ops[p].getField().equals(cname))
                      && (ops[p].getTableConstraint() != null && ops[p].getTableConstraint().equals(tableName))) {
                      newOption = false;
                      break outer;
                    }
                  }
                }
              }

              // could be present as a FD as well
              FilterDescription currFilt = null;
              if (dsv.getFilterDescriptionByFieldNameTableConstraint(cname, tableName,null) != null)
                currFilt = dsv.getFilterDescriptionByFieldNameTableConstraint(cname, tableName,null);
              if (currFilt != null)
                newOption = false;

              if (newOption)
                fdLists.addOption(op);

            }
          }

        } else if (isLookupTable(tableName)) {
          if (cname.startsWith("glook_") || cname.startsWith("silent_")) {
            if (fc == null) {
              fc = new FilterCollection();
              fc.setInternalName(content);
              fc.setDisplayName(content.replaceAll("_", " "));
            }

            FilterDescription currFilt = dsv.getFilterDescriptionByFieldNameTableConstraint(cname, tableName,null);
            if (currFilt == null)
              fc.addFilterDescription(getFilterDescription(cname, tableName, ctype, joinKey, dsv));

            else { // update options if has any
              if (currFilt.hasOptions())
                updateDropDown(dsv, currFilt);
            }

          }
        } else {
          if (logger.isLoggable(Level.FINE))
            logger.fine("Skipping " + tableName + "\n");
        }
      }

      if (ac != null && ac.getAttributeDescriptions().size() > 0)
        ag.addAttributeCollection(ac);

      if (fc != null && fc.getFilterDescriptions().size() > 0)
        fg.addFilterCollection(fc);
    }

    if (fdBools != null && fdBools.getOptions().length > 0)
      fcList.addFilterDescription(fdBools);
    if (fdLists != null && fdLists.getOptions().length > 0)
      fcList.addFilterDescription(fdLists);
    if (fcList != null && fcList.getFilterDescriptions().size() > 0)
      fg.addFilterCollection(fcList);

    if (ag != null && ag.getAttributeCollections().length > 0)
      ap.addAttributeGroup(ag);
    if (fg != null && fg.getFilterCollections().length > 0)
      fp.addFilterGroup(fg);
    if (ap != null && ap.getAttributeGroups().size() > 0) {
      dsv.addAttributePage(ap);
    }
    if (fp != null && fp.getFilterGroups().size() > 0)
      dsv.addFilterPage(fp);

    return dsv;
  }

  private void updateDropDown(DatasetConfig dsConfig, FilterDescription fd1)
    throws ConfigurationException, SQLException {

    Option[] ops = fd1.getOptions();
    
    //System.out.println("options size " + ops.length);
    
    if (ops[0].getTableConstraint() != null)
      return;
    // drop down lists of options shouldn't be updated

    PushAction[] pas = ops[0].getPushActions();

    PushAction[] pas2 = null;
    if (pas.length > 0){
      Option[] paOps = pas[0].getOptions();
      if (paOps.length > 0)
        pas2 = paOps[0].getPushActions();
    }

    
    for (int i = 0; i < ops.length; i++) {
      fd1.removeOption(ops[i]);
    }
    String field = fd1.getField();
    String tableName = fd1.getTableConstraint();
    String joinKey = fd1.getKey();
    fd1.setType("list");
    fd1.setQualifier("=");
    fd1.setLegalQualifiers("=");

    Option[] options = getOptions(field, tableName, joinKey, dsConfig);

    fd1.addOptions(options);

    // update push actions if any

    for (int k = 0; k < pas.length; k++) {

      String ref = pas[k].getRef();
      
      
      
      FilterDescription fd2 = dsConfig.getFilterDescriptionByInternalName(ref);
      
      if (fd2 != null){// because of martp placeholders
		updatePushAction(dsConfig, fd1, fd2);
      }
      
    }
    Option[] newOps = fd1.getOptions();
    for (int k = 0; k < newOps.length; k++) {
      for (int l = 0; l < pas.length; l++) {
        PushAction[] newPas = newOps[k].getPushActions();
        PushAction newPa = null;
        for (int z = 0; z < newPas.length; z++) {
          if (newPas[z].getRef().equals(pas[l].getRef())) {
            newPa = newPas[z];
            break;
          }
        }

        for (int m = 0; m < pas2.length; m++) {
          String ref2 = pas2[m].getRef();
          FilterDescription fd3 = dsConfig.getFilterDescriptionByInternalName(ref2);
          updatePushAction(dsConfig, newPa, fd3);
        }
      }
    }
  }

  private void updatePushAction(DatasetConfig dsConfig, BaseConfigurationObject bo, FilterDescription fd2)
    throws ConfigurationException, SQLException {
    fd2.setType("drop_down_basic_filter");

    String pushField = fd2.getField();
    String pushInternalName = fd2.getInternalName();
    String pushTableName = fd2.getTableConstraint();

	if (pushTableName.equals("main")){
    	String[] mains = dsConfig.getStarBases();
		pushTableName = mains[0];
	}

	String orderSQL = JOptionPane.showInputDialog("Optional column name to order " + pushInternalName + " :");		
	

    // can add push actions to existing push actions so need to know the class of the node
    String className = bo.getClass().getName();
    String field;
    Option[] options;

    if (className.equals("org.ensembl.mart.lib.config.FilterDescription")) {
      FilterDescription fd1 = (FilterDescription) bo;
      field = fd1.getField();
      //if (!fd1.getTableConstraint().equals(pushTableName))
      //  field = "olook_" + field;
      options = fd1.getOptions();

    } else {
      PushAction pa1 = (PushAction) bo;
      String intName = pa1.getInternalName();
      field = intName.split("_push")[0];
      //if (field.startsWith("glook_")) {
      //  field = field.replaceFirst("glook_", "");
      //}
      options = pa1.getOptions();
    }


    for (int i = 0; i < options.length; i++) {
      Option op = options[i];
      String opName = op.getInternalName();
      PushAction pa = new PushAction(pushInternalName + "_push_" + opName, null, null, pushInternalName);

      pa.addOptions(getLookupOptions(pushField, pushTableName, field, opName, orderSQL));

      if (pa.getOptions().length > 0) {
        //System.out.println("ADDING PA\t" + op.getInternalName());
        op.addPushAction(pa);
      }
    }

  }

  private boolean isDimensionTable(String tableName) {
    if (tableName.toLowerCase().endsWith(DIMENSIONTABLESUFFIX))
      return true;
    return false;
  }

  private boolean isMainTable(String tableName) {
    if (tableName.toLowerCase().endsWith(MAINTABLESUFFIX.toLowerCase()))
      return true;
    return false;
  }

  private boolean isLookupTable(String tableName) {
    if (tableName.toLowerCase().endsWith(LOOKUPTABLESUFFIX.toLowerCase()))
      return true;
    return false;
  }

  private AttributeDescription getAttributeDescription(String columnName, String tableName, int maxSize, String joinKey)
    throws ConfigurationException {
    AttributeDescription att = new AttributeDescription();
    att.setField(columnName);
    att.setInternalName(columnName.toLowerCase());
    att.setDisplayName(columnName.replaceAll("_", " "));
    att.setKey(joinKey);
    att.setTableConstraint(tableName);
    
    if (maxSize > 255){
    	maxSize = 255;
    }
    
    att.setMaxLength(String.valueOf(maxSize));

    return att;
  }

  private FilterDescription getFilterDescription(
    String columnName,
    String tableName,
    int columnType,
    String joinKey,
    DatasetConfig dsv)
    throws SQLException, ConfigurationException {
    FilterDescription filt = new FilterDescription();
    filt.setField(columnName);
    String descriptiveName = columnName;
    // lookup table fds
    if (tableName.endsWith("look")) {
      descriptiveName = descriptiveName.replaceFirst("glook_", "");
      descriptiveName = descriptiveName.replaceFirst("silent_", "");
      filt.setInternalName(descriptiveName.toLowerCase());
      filt.setDisplayName(descriptiveName.replaceAll("_", " "));
      filt.setTableConstraint(tableName);
      if (!columnName.startsWith("silent_")) {
        //filt.setHandler("org.ensembl.mart.lib.GenericHandler");//handlers now removed from XML
        filt.setType("text");
      } else {
        filt.setType("list");
        filt.setQualifier("=");
        filt.setLegalQualifiers("=");
        Option[] options = getOptions(columnName, tableName, null, dsv);
        filt.addOptions(options);
      }
    }
    // main table fds
    else {
      if (columnName.endsWith("_bool")) {
		filt.setInternalName(descriptiveName.toLowerCase());
        descriptiveName = columnName.replaceFirst("_bool", "");
        filt.setType("boolean");
        filt.setQualifier("only");
        filt.setLegalQualifiers("only,excluded");
      } else if (columnName.endsWith("_list")) {
        descriptiveName = columnName.replaceFirst("_list", "");
        filt.setType("list");
        filt.setQualifier("=");
        filt.setLegalQualifiers("=,in");
        // hack for multiple display_id in ensembl xref tables
        if (descriptiveName.equals("display_id")) {
          descriptiveName = tableName.split("__")[1].replaceFirst("xref_", "");
        }
		filt.setInternalName(descriptiveName.toLowerCase());
      } else {
		filt.setInternalName(descriptiveName.toLowerCase());
        filt.setType(DEFAULTTYPE);
        filt.setQualifier(DEFAULTQUALIFIER);
        filt.setLegalQualifiers(DEFAULTLEGALQUALIFIERS);
      }
      
      filt.setDisplayName(descriptiveName.replaceAll("_", " "));
      filt.setTableConstraint(tableName);
      filt.setKey(joinKey);

    }
    return filt;
  }

  public Option[] getOptions(String columnName, String tableName, String joinKey, DatasetConfig dsConfig)
    throws SQLException, ConfigurationException {

    List options = new ArrayList();
	
    if (tableName.equalsIgnoreCase("main")) {
      String[] starNames = dsConfig.getStarBases();
      String[] primaryKeys = dsConfig.getPrimaryKeys();
      tableName = starNames[0];// in case no keys for a lookup type dataset
      for (int k = 0; k < primaryKeys.length; k++) {
        if (primaryKeys[k].equalsIgnoreCase(joinKey))
          tableName = starNames[k];
      }
    }

    Connection conn = dsource.getConnection();
    String sql =
      "SELECT DISTINCT "
        + columnName
        + " FROM "
        + tableName
        + " WHERE "
        + columnName
        + " IS NOT NULL ORDER BY "
        + columnName;
        
        
    PreparedStatement ps = conn.prepareStatement(sql);
    ResultSet rs = ps.executeQuery();
    String value;
    Option op;
    while (rs.next()) {
      value = rs.getString(1);
      
      // fix for an empty string
      	if (value.length() == 0)
      	{	
      		System.out.println("MAKE DROP DOWN WARNING: Detected empty string(s) in "+ tableName+"."+columnName);
      continue;
      }  
      
      	op = new Option();
      op.setDisplayName(value);
      String intName = value.replaceAll(" ", "_");
      op.setInternalName(intName.toLowerCase());
      //NN
      //if (!(columnName.startsWith("silent_") || columnName.startsWith("SILENT_"))) //prob. not needed, to check
        //if (!columnName.startsWith("silent_"))
      op.setValue(value);
	  //op.setValue(intName);
      op.setSelectable("true");
      options.add(op);
    }

    Option[] retOptions = new Option[options.size()];
    options.toArray(retOptions);
    return retOptions;
  }

  public Option[] getLookupOptions(String columnName, String tableName, String whereName, String whereValue, String orderSQL)
    throws SQLException, ConfigurationException {

    List options = new ArrayList();
    Connection conn = dsource.getConnection();
    if (orderSQL.equals(""))
      orderSQL = "\" ORDER BY " + columnName;
    else
      orderSQL = "\" ORDER BY " + orderSQL;
        
    String sql =
      "SELECT DISTINCT "
        + columnName
        + " FROM "
        + tableName
        + " WHERE "
        + whereName
        + "=\""
        + whereValue
        + orderSQL;
        
        
    PreparedStatement ps = conn.prepareStatement(sql);
    ResultSet rs = ps.executeQuery();
    String value;
    Option op;

    while (rs.next()) {
      value = rs.getString(1);
      op = new Option();
      op.setDisplayName(value);
      op.setInternalName(value);
      op.setValue(value);
      op.setSelectable("true");
      options.add(op);
    }
    conn.close();
    Option[] retOptions = new Option[options.size()];
    options.toArray(retOptions);
    return retOptions;
  }

  private boolean isAllNull(String cname, String tableName) throws SQLException, ConfigurationException {

    Connection conn = null;
    try {
      conn = dsource.getConnection();
       
      StringBuffer sql = new StringBuffer("SELECT " + cname+ " FROM " + tableName + " WHERE " + cname + " IS NOT NULL");
      
      if (dsource.getDatabaseType().equals("oracle")){
     //System.out.println("databaseType() "+dsource.getDatabaseType());

      sql.append (" and rownum <=1");
      } 
      if (dsource.getDatabaseType().equals("mysql")) {
      sql.append (" limit 1");
      }

      PreparedStatement ps = conn.prepareStatement(sql.toString());
      ResultSet rs = ps.executeQuery();
      rs.next();

      if (rs.isFirst()) {
        rs.close();
        conn.close();
        return false;
      } else {
        //System.out.println("ALL NULLS\t" + cname + "\t" + tableName);
        rs.close();
        conn.close();
        return true;
      }

    } catch (SQLException e) {
      throw new ConfigurationException("Caught SQLException during attempt to count non-null values\n", e);
    } finally {
      DetailedDataSource.close(conn);
    }
  }

private String getSchema(){
	
	String schema = null;
   
    try {
		Connection conn = dsource.getConnection();
		ResultSet schemas = conn.getMetaData().getSchemas();
		  // for oracle, mysql does not care
		// schema, catalog, user, db and instance handling between oracle and mysql needs to be reorganised
		  schema=dsource.getUser().toUpperCase();
		conn.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	

    return schema;
    
}


}
