/*
 * Created on Aug 2, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.lib;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with JDBC databases.
 */
public class DatabaseUtil {

  public static final Connection getConnection(
    String databaseType,
    String host,
    String port,
    String databaseName,
    String user,
    String password)
    throws SQLException {

    StringBuffer dbURL = new StringBuffer();
    dbURL.append("jdbc:").append(databaseType).append("://");
    dbURL.append(host);
    if (port != null && !"".equals(port))
      dbURL.append(":").append(port);
    dbURL.append("/");
    if (databaseName != null && !databaseName.equals(""))
      dbURL.append(databaseName);

    return getConnection(dbURL.toString(), user, password);
  }

  public static final Connection getConnection(
    String dbURL,
    String user,
    String password)
    throws SQLException {

    return DriverManager.getConnection(dbURL, user, password);
  }

  public static final String[] databaseNames(Connection conn)
    throws SQLException {
    List databases = new ArrayList();

    ResultSet rs = conn.createStatement().executeQuery("show databases");
    while (rs.next()) {
      databases.add(rs.getString(1));
    }

    return (String[]) databases.toArray(new String[databases.size()]);
  }

  public static class DatabaseURLElements {

    public String databaseURL;
    public String databaseType;
    public String host;
    public String port;
    public String databaseName;
  };

  /**
   * Decomposes a databaseURL into it's constituent parts.
   * 
   * <p>EXAMPLE :
   * <code>jdbc:mysql://kaka.sanger.ac.uk:3306/ensembl_mart_15_1</code>
   * </p > 
   * @param URL database connection string. 
   * @throws IllegalArgumentException if databaseURL has the wrong format.
   */
  public static final DatabaseURLElements decompose(String databaseURL)
    throws IllegalArgumentException {

    DatabaseURLElements elements = new DatabaseURLElements();
    elements.databaseURL = databaseURL;
    Pattern p =
      Pattern.compile("^(\\w+:(\\w+)://([^:/]+)(:(\\d+))?)(/([^?]*))?$");
    Matcher m = p.matcher(databaseURL);
    if (m.matches()) {
      elements.databaseType = m.group(2);
      elements.host = m.group(3);
      elements.port = m.group(5);
      elements.databaseName = m.group(7);
    } else
      throw new IllegalArgumentException("Invalid database URL:" + databaseURL);
    
    return elements;
  }
  
}