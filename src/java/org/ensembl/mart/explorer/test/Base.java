/* Generated by Together */

package org.ensembl.mart.explorer.test;

import java.net.URL;
import java.sql.Connection;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.ensembl.driver.ConfigurationException;
import org.ensembl.driver.Driver;
import org.ensembl.driver.DriverManager;
import org.ensembl.mart.explorer.Engine;
import org.ensembl.mart.explorer.Query;

/**
 * Base class for tests that sets up the logging system if necessary. 
 */
public abstract class Base extends TestCase {

	private final static Logger logger = Logger.getLogger(Base.class.getClass());
	private final static String connprops = "data/testconnection.conf";
	private final static String connpropsEnsj = "data/testconnection_ensj.conf";

	private String host = null;
	private String port = null;
	private String database = null;
	private String user = null;
	private String password = null;
	private Properties p = new Properties();
	private URL connectionconf;

	protected Driver ensjDriver = null;
	protected Engine engine;
	protected Query genequery = new Query();
	protected Query snpquery = new Query();

	public void init() {
		connectionconf = org.apache.log4j.helpers.Loader.getResource(connprops);

		if (connectionconf != null) {
			try {
				p.load(connectionconf.openStream());

				host = p.getProperty("mysqlhost");
				port = p.getProperty("mysqlport");
				database = p.getProperty("mysqldbase");
				user = p.getProperty("mysqluser");
				password = p.getProperty("mysqlpass");
			} catch (java.io.IOException e) {
				System.out.println(
					"Caught IOException when trying to open connection configuration file " + connprops + "\n" + e + "\n\nusing default connection parameters");
			}
		} else {
			System.out.println("Failed to find connection configuration file " + connprops + " using default connection parameters");

			host = "kaka.sanger.ac.uk";
			database = "ensembl_mart_11_1";
			user = "anonymous";
		}

		try {
			ensjDriver = DriverManager.load(connpropsEnsj);
		} catch (ConfigurationException e) {
			logger.warn("", e);
		}

	}

	public void setUp() {
		init();

		engine = new Engine(host, port, user, password, database);
		genequery.setStarBases(new String[] { "hsapiens_ensemblgene", "hsapiens_ensembltranscript" });
		genequery.setPrimaryKeys(new String[] { "gene_id", "transcript_id" });
		snpquery.setStarBases(new String[] { "hsapiens_snp" });
		snpquery.setPrimaryKeys(new String[] { "snp_id" });
	}

	public Base(String name) {
		super(name);
		if (System.getProperty("log4j.configuration") == null) {
			BasicConfigurator.configure();
			Logger.getRoot().setLevel(Level.WARN);
		}
	}

  public Connection getDBConnection() throws Exception {
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
