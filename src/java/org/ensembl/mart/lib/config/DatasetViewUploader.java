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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.LoggingUtils;

/**
 * Loads Mart XML Configuration files into the mart database.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
public class DatasetViewUploader {

	private String dbType = "mysql"; // default
	private String driver = "com.mysql.jdbc.Driver"; //default
	private boolean compress = true; // default

	private String host = null;
	private String port = null;
	private String database = null;
	private String user = null;
	private String password = null; //optional
	private boolean help = false;
	private String datasetUser = null; // optional

	private DataSource dataSource = null;
	private String[] files = new String[] {
	};

	private final static Logger logger =
		Logger.getLogger(DatasetViewUploader.class.getName());

	public static void main(String[] args) {
		LoggingUtils.setVerbose(false);

		DatasetViewUploader u = new DatasetViewUploader();
		u.parse(args);
		u.initDataSource();
		u.upload();

	}

	/**
	 * 
	 */
	private void initDataSource() {

		try {
			dataSource =
				DatabaseUtil.createDataSource(
					dbType,
					host,
					port,
					database,
					user,
					password,
					10,
					driver);

			// make a test connection to ensure connection corrent
			dataSource.getConnection().close();
		} catch (Exception e) {
			System.out.println("Failed to connect to database: " + e.getMessage());
			System.exit(0);
		}

	}

	/**
	 * Attempt to upload each file to the database.
	 */
	private void upload() {

		// load xml each file and upload to database
		for (int i = 0, n = files.length; i < n; i++) {

			try {

				System.out.println("Uploading file: " + files[i] + " ... ");

        // if files[i] is a file then load that, otherwise load
        // it as a URL
				URL url = null;
        //e.g. data/myfile.xml
        File file = new File(files[i]);
        // e.g. file:data/XML/myfile.xml
        if ( file.exists() ) url = file.toURL();
        if ( url==null ) url = new URL(files[i]);
        
				URLDSViewAdaptor fileLoader = new URLDSViewAdaptor(url, true);

				// upload xml
				DatabaseDSViewAdaptor.storeDatasetView(
					dataSource,
					datasetUser,
					fileLoader.getDatasetViews()[0],
					compress);

				System.out.println("Finished uploading file: " + files[i]);

			} catch (Exception e) {
				System.out.println(
					"Failed to upload file: " + files[i] + " (" + e.getMessage() + ")");
			}

		}
	}

	/**
	 * Parses array of arguments extracting parameters which are set as 
	 * member attributes for later use.
	 * @param args
	 */
	private void parse(String[] args) {

		LongOpt[] longopts =
			new LongOpt[] {
				new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'h'),
				new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 'P'),
				new LongOpt("database", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
				new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
				new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
				new LongOpt("dataset-user", LongOpt.REQUIRED_ARGUMENT, null, 'U'),
				new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'H'),
        new LongOpt("no-compress", LongOpt.NO_ARGUMENT, null, 'n'),
        new LongOpt("db-type", LongOpt.NO_ARGUMENT, null, 't'),
        new LongOpt("driver", LongOpt.NO_ARGUMENT, null, 'D'),
        };

		Getopt g =
			new Getopt("DatasetViewUploader", args, "h:P:d:u:p:U:Hn", longopts);
		int c;
		String arg;
		while ((c = g.getopt()) != -1) {

			switch (c) {

				case 'h' :
					host = g.getOptarg();
					break;

				case 'P' :
					port = g.getOptarg();
					break;

				case 'd' :
					database = g.getOptarg();
					break;

				case 'u' :
					user = g.getOptarg();
					break;

				case 'p' :
					password = g.getOptarg();
					break;

        case 't' :
          dbType = g.getOptarg();
          break;

        case 'D' :
          driver = g.getOptarg();
          break;

				case 'H' :
					help = true;
					break;

				case 'n' :
					compress = false;
					break;

				case '?' :
					// getopt will print an outor message
					help = true;
					break;

			}
		}

		if (args.length == 0)
			help = true;

		if (!help) {

			help = missing("host", host) || help;
			help = missing("database", database) || help;
			help = missing("user", user) || help;

      files = new String[args.length - g.getOptind()];
      System.arraycopy(args, g.getOptind(), files, 0, files.length);

      if (files.length == 0) {
        help = true;
        System.out.println("WARNING: No files to upload specified.");
      }

		}


		if (help) {
			System.out.println(usage());
			System.exit(0);
		}

	}

	/**
	 * Prints a warning message if value is null.
	 * @param parameterName name of command line parameter
	 * @param value command line option for parameter, null if not set.
	 * @return true if value is null, otherwise true.
	 */
	private boolean missing(String parameterName, String value) {

		if (value != null)
			return false;

		System.err.println("WARNING: Parameter " + parameterName + " is required");
		return true;

	}

	public String usage() {
    return
    "DatasetViewUploader <OPTIONS> FILE_1 FILE_2 FILE_3 ... FILE_N"
    +"\nOPTIONS:" 
    +"\n--host, -h HOST"
    +"\n--port, -P PORT                       - optional port, default will be used"
    +"\n--database, -d DATABASE"
    +"\n--user, -u USERNAME"
    +"\n--password, -p PASSWORD               - optional database password"
    +"\n--dataset-user -U DATASET_VIEW_USER   - username prefix for meta table on database."
    +"\n--help, -H"
    +"\n--no-compress, -n                     - files are compressed by default."
    +"\n--db-type DATABASE_TYPE               - optional database type, default is mysql"
    +"\n--driver DRIVER                       - optional jdbc driver, default is com.mysql.jdbc.Driver";
    
	}

}
