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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ensembl.mart.lib.LoggingUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Loads Mart XML Configuration files into the mart database.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class XMLLoader {
	
	private static final String conf = System.getProperty("user.home") + "/.martshell";
	private static String confinUse = null;
	private static Connection conn = null;
	private static String host = null;
  private static String dbtype = null;
	private static String port = null;
	private static String database = null;
	private static String user = null;
	private static String password = null;
	private static String martdb = null;
	private static String system_id = null;
	private static URL sourceURL = null;
	private static boolean compress = false; 
	private static Logger logger = Logger.getLogger(XMLLoader.class.getName());
			
	public static void main(String[] args) {
		LoggingUtils.setVerbose(false);
		
		try {
			if (new File(conf).exists())
						getConnProperties(conf);
			else {
				System.err.println("Please create a .martshell file in your home directory.\nIt should match the file pathTomart-explorerDistributeDirectory/data/testconnection.conf\n");
				System.exit(1);			
			}
			
			if (args.length == 3) {
				database = args[0];
				system_id = args[1];
				sourceURL = new URL(args[2]);
			} else if (args.length == 4) {
				database = args[0];
				system_id = args[1];
				sourceURL = new URL(args[2]);
				compress = true;
			}
			else {
				System.err.println(usage());
				System.exit(1);
			}
			
			conn = getDBConnection();
			
			if (system_id.endsWith("xml")) {
			  SAXBuilder builder = new SAXBuilder();
			  builder.setValidation(true); // validate against the DTD
			  builder.setEntityResolver(new MartDTDEntityResolver()); // set the EntityResolver to allow it to get the DTDs from the classpath.
			
			  Document doc = builder.build(sourceURL);
			
			  MartXMLutils.storeConfiguration(conn, system_id, doc, compress);
			}
			else {
				// dtd
				MartXMLutils.storeDTD(conn, system_id, sourceURL, compress);
			}
			
			conn.close();
			System.err.println("XML File Stored Successfully.");
			System.exit(0);		
		} catch (MalformedURLException e) {
			System.err.println("Could not get xml file "+sourceURL+" "+e.getMessage()+"\n");
			e.printStackTrace();
		} catch (JDOMException e) {
			System.err.println("Could not parse xml file "+sourceURL+" "+e.getMessage()+"\n");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Caught IOException "+e.getMessage());
			e.printStackTrace();
		} catch (ConfigurationException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static String usage() {
		String out = "Usage: XMLLoader <martdb> <system_id> <URL for xml file location> <[compress] (if left off, the file is not compressed)>";
		return out;
	}
	
	/**
	 * Parses java properties file to get mysql database connection parameters.
	 * 
	 * @param connfile -- String name of the configuration file containing mysql
	 *  database configuration properties.
	 */
	public static void getConnProperties(String connfile) {
		URL confInfo;
		Properties p = new Properties();

		try {
			confInfo = new File(connfile).toURL();
			p.load(confInfo.openStream());

			host = p.getProperty("host");
			port = p.getProperty("port");
			database = p.getProperty("databaseName");
			user = p.getProperty("user");
			password = p.getProperty("password");
      dbtype = p.getProperty("databaseType");
		} catch (java.net.MalformedURLException e) {
			if (logger.isLoggable( Level.WARNING ))
			logger.warning(
				"Could not load connection file "
					+ connfile
					+ " MalformedURLException: "
					+ e);
		} catch (java.io.IOException e) {
			if (logger.isLoggable( Level.WARNING ))
			logger.warning(
				"Could not load connection file " + connfile + " IOException: " + e);
		}
		confinUse = connfile;
	}
	
	public static Connection getDBConnection() throws Exception {
		StringBuffer connStr = new StringBuffer();
		connStr.append("jdbc:mysql://");
		connStr.append( host );

		if ( port != null && !"".equals(port) )
				connStr.append(":").append( port );
				
		connStr.append("/").append( database ); // default table - we have to connect to one table
		connStr.append("?autoReconnect=true");
		Connection conn = null;
		
		Class.forName("org.gjt.mm.mysql.Driver").newInstance();
		logger.info(connStr.toString());
		conn = java.sql.DriverManager.getConnection(connStr.toString(), user, password );
		
		return conn;
	}
}
