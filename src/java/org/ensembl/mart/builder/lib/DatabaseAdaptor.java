/*
 * Created on Jun 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */

public class DatabaseAdaptor {
	
	public String username;
	public String catalog;
	public String rdbms;
	public String schema;
	
	private String host;
	private String port;
    private String instance;
	private String password;
	private String driver;
	
	private Properties p = new Properties();
	private URL connectionconf;
	private String config;
	private String url;
	private Connection con;
	
	/**
	 * 
	 */
	public DatabaseAdaptor(String config) {
		super();
		setConfig(config);
		initialiseFromFile();
	}
	
	public DatabaseAdaptor(String driverString,String usernameString, String passwordString, String hostString,
	                       String portString, String instanceString, String schemaString, String catalogString) {
		super();
		driver = driverString;
		username = usernameString;
		password = passwordString;
		host = hostString;
		port = portString;
		instance = instanceString;
		schema = schemaString;
		catalog= catalogString;

		if (driver.indexOf("oracle") >= 0)
			rdbms = "oracle";
		else if (driver.indexOf("mysql") >= 0)
			rdbms = "mysql";	
		else if (driver.indexOf("postgresql") >= 0)
			rdbms = "postgresql";
		else{
			System.out.println("CONNECTION NOT POSSIBLE");
			return;
		}
		
		initialiseConnection();			
					
	}
	
	
	private Connection initialiseConnection (){
				
	  try {
		  Class.forName(driver);
	  }
	  catch (java.lang.ClassNotFoundException e) {
		  System.err.print("ClassNotFoundException: ");
		  System.err.println(e.getMessage());
	  }	
			
	  try {
		  if (rdbms.equals("oracle"))
		  url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + instance;
		  else if (rdbms.equals("mysql")) 
		  url = "jdbc:mysql://" + host + ":" + port + "/" + instance;
		  else if (rdbms.equals("postgresql")) 
			  url = "jdbc:postgresql://" + host + ":" + port + "/" + instance;
			
		  else System.err.println("not supported rdbms type: "+ rdbms);
		  Connection con = DriverManager.getConnection (url, username,password);
		  setCon(con);
	  }
	  catch(SQLException ex) {
		  System.err.print("SQLException: ");
		  System.err.println(ex.getMessage());
	  }
		
	  return con;
		
	}
	
	private Connection initialiseFromFile (){
		
		
		try {
			connectionconf = ClassLoader.getSystemResource(getConfig());
			p.load(connectionconf.openStream());
			
			rdbms =p.getProperty("rdbms");
			username = p.getProperty("username");
		    password = p.getProperty("password");
			host = p.getProperty("host");
			port = p.getProperty("port");
		    //driver = p.getProperty("driver");
		    instance = p.getProperty("instance");
		    schema = p.getProperty("schema");
		    catalog= p.getProperty("catalog");
		    
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if (rdbms.equals("oracle"))
			driver = "oracle.jdbc.driver.OracleDriver";
			else if (rdbms.equals("mysql")) 
			driver = "com.mysql.jdbc.Driver";
			else if (rdbms.equals("postgresql")) 
				driver="org.postgresql.Driver";
			else System.err.println("not supported rdbms type: "+ rdbms);
			
		Connection con = initialiseConnection();
		return con;
	}	
	
	
	/**
	 * @return Returns the con.
	 */
	public Connection getCon() {
		return con;
	}
	/**
	 * @param con The con to set.
	 */
	public void setCon(Connection con) {
		this.con = con;
	}
	
	/**
	 * @return Returns the config.
	 */
	public String getConfig() {
		return config;
	}
	/**
	 * @param config The config to set.
	 */
	public void setConfig(String config) {
		this.config = config;
	}




}


	
	
	
	
	
	

