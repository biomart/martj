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

package org.ensembl.mart.shell;

import gnu.getopt.Getopt;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FormatException;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.LoggingUtils;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.SequenceException;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

/**
 * <p>Interface to a Mart Database implimentation that provides commandline access using a SQL-like query language (see MartShellLib for a description of the Mart Query Language).
 * The system can be used to run script files containing valid Mart Query Language commands, or individual queries from the commandline.
 * It has an interactive shell as well.  Script files can include comment lines beginning with #, which are ignored by the system.</p>  
 * 
 * <p>The interactive shell makes use of the <a href="http://java-readline.sourceforge.net/">Java Readline Library</a>
 * to allow commandline editing, history, and tab completion for those users working on Linux/Unix operating systems.  Unfortunately, there is no way
 * to provide this functionality in a portable way across OS platforms.  For windows users, there is a Getline c library which is provided with the Java Readline source.
 * By following the instructions to build a windows version of this library, and you will get some (but not all) of this functionality.</p>
 * <p> One other side effect of the use of this library is that, because it uses GNU Readline, which is GPL, it makes MartShell GPL as well (despite the LPGL license that it and the rest
 * of Mart-Explorer are released under).  If you are serious about extending/using the MartShell class in your own code for distribution, and are worried about
 * the effects of the GPL, then you might consider rebuilding the Java Readline Library using the LGPL EditLine library, which is available on some Linux platforms.</p>
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see MartShellLib
 * @see MartCompleter
 */
public class MartShell {

	// main variables
	private static final String defaultConf = System.getProperty("user.home") + "/.martshell";
	private static String COMMAND_LINE_SWITCHES = "h:AC:M:H:T:P:U:p:D:d:vl:e:O:F:R:E:";
	private static String confinUse = null;
	private static String mainConfiguration = null;
	private static String mainHost = null;
	private static String mainPort = null;
	private static String mainDatabase = null;
	private static String mainDatabaseType = null;
	private static String mainUser = null;
	private static String mainPassword = null;
	private static String mainDataset = null;
	private static boolean mainBatchMode = false; // if -e is passed, true
	private static String mainBatchSQL = null;
	private static String mainBatchScriptFile = null;
	// can hold the URL to a mart script
	private static String mainBatchFile = null;
	private static String mainBatchFormat = null;
	private static String mainBatchSeparator = null;
	private static String helpCommand = null;
	private static Logger mainLogger = Logger.getLogger(MartShell.class.getName());

	/**
	 *  @return application usage instructions
	 * 
	 */
	public static String usage() {
		return "MartShell <OPTIONS>"
			+ "\n"
			+ "\n-h <command>                            - this screen, or, if a command is provided, help for that command"
			+ "\n-A                                      - Turn off Commandline Completion (faster startup, less helpful)"
			+ "\n-C MART_CONFIGURATION_FILE_URL         - URL to Alternate Mart XML Configuration File"
			+ "\n-M CONNECTION_CONFIGURATION_FILE_URL   - URL to mysql connection configuration file"
			+ "\n-H HOST                                 - database host"
			+ "\n-T DATABASE_TYPE                        - type of Relational Database Management System holing the mart (default mysql)"
			+ "\n-P PORT                                 - database port"
			+ "\n-U USER                                 - database user name"
			+ "\n-p PASSWORD                             - database password"
			+ "\n-D DATABASE                             - database name"
			+ "\n-d DATASET                              - dataset name"
			+ "\n-v                                      - verbose logging output"
			+ "\n-l LOGGING_CONFIGURATION_URL    - Java logging system configuration file (example file:data/exampleLoggingConfig.properties)"
			+ "\n-e MARTQUERY                            - a well formatted Mart Query to run in Batch Mode"
			+ "\n\nThe following are used in combination with the -e flag:"
			+ "\n-O OUTPUT_FILE                          - output file, default is standard out"
			+ "\n-F OUTPUT_FORMAT                        - output format, either tabulated or fasta"
			+ "\n-R OUTPUT_SEPARATOR                     - if OUTPUT_FORMAT is tabulated, can define a separator, defaults to tab separated"
			+ "\n\n-E QUERY_FILE_URL                     - URL to file with valid Mart Query Commands"
			+ "\n\nThe application searches for a .martshell file in the user home directory for mysql connection configuration"
			+ "\nif present, this file will be loaded. If the -g option is given, or any of the commandline connection"
			+ "\nparameters are passed, these over-ride those values provided in the .martshell file"
			+ "\nUsers specifying a mysql connection configuration file with -g,"
			+ "\nor using a .martshell file, can use -H, -P, -p, -u, or -d to specify"
			+ "\nparameters not specified in the configuration file, or over-ride those that are specified."
			+ "\n";
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

			String tmp = p.getProperty("host");
			if (tmp != null && tmp.length() > 1 && mainHost == null)
				mainHost = tmp.trim();

			tmp = p.getProperty("port");
			if (tmp != null && tmp.length() > 1 && mainPort == null)
				mainPort = tmp.trim();

			tmp = p.getProperty("databaseName");
			if (tmp != null && tmp.length() > 1 && mainDatabase == null)
				mainDatabase = tmp.trim();

			tmp = p.getProperty("user");
			if (tmp != null && tmp.length() > 1 && mainUser == null)
				mainUser = tmp.trim();

			tmp = p.getProperty("password");
			if (tmp != null && tmp.length() > 1 && mainPassword == null)
				mainPassword = tmp.trim();

			tmp = p.getProperty("databaseType");
			if (tmp != null && tmp.length() > 1 && mainDatabaseType == null)
				mainDatabaseType = tmp.trim();

			tmp = p.getProperty("alternateConfigurationFile");
			if (tmp != null && tmp.length() > 1 && mainConfiguration == null) {
				mainConfiguration = tmp.trim();
			}
		} catch (java.net.MalformedURLException e) {
			mainLogger.warning("Could not load connection file " + connfile + " MalformedURLException: " + e);
		} catch (java.io.IOException e) {
			mainLogger.warning("Could not load connection file " + connfile + " IOException: " + e);
		}
		confinUse = connfile;
	}

	private static String[] harvestArguments(String[] oargs) throws Exception {
		Hashtable argtable = new Hashtable();
		String key = null;

		for (int i = 0, n = oargs.length; i < n; i++) {
			String arg = oargs[i];

			if (arg.startsWith("-")) {
				String thisArg = arg;
				key = null;
				String value = null;

				if (thisArg.length() > 2) {
					key = thisArg.substring(0, 2);
					value = thisArg.substring(2);
				} else
					key = thisArg;

				if (!argtable.containsKey(key)) {
					StringBuffer buf = new StringBuffer();

					if (value != null)
						buf.append(value);

					argtable.put(key, buf);
				}
			} else {
				if (key == null)
					throw new Exception("Invalid Arguments Passed to MartShell\n");
				StringBuffer value = (StringBuffer) argtable.get(key);
				if (value.length() > 0)
					value.append(" ");
				value.append(arg);
				argtable.put(key, value);
			}
		}

		String[] ret = new String[argtable.size() * 2];
		// one slot for each key, and one slot for each non null or non empty value
		int argnum = 0;

		for (Iterator iter = argtable.keySet().iterator(); iter.hasNext();) {
			String thiskey = (String) iter.next();
			String thisvalue = ((StringBuffer) argtable.get(thiskey)).toString();

			ret[argnum] = thiskey;
			argnum++;

			// getOpt wants an empty string for switches
			if (thisvalue.length() < 1)
				thisvalue = "";

			ret[argnum] = thisvalue;
			argnum++;
		}

		return ret;
	}

	public static void main(String[] oargs) {

		String loggingURL = null;
		boolean help = false;
		boolean verbose = false;
		boolean commandComp = true;

		// check for the defaultConf file, and use it, if present.  Some values may be overridden with a user specified file with -g
		if (new File(defaultConf).exists())
			getConnProperties(defaultConf);

		String[] args = null;
		if (oargs.length > 0) {
			try {
				args = harvestArguments(oargs);
			} catch (Exception e1) {
				System.out.println(e1.getMessage());
				e1.printStackTrace();
				System.exit(1);
			}

			Getopt g = new Getopt("MartShell", args, COMMAND_LINE_SWITCHES);
			int c;

			while ((c = g.getopt()) != -1) {

				switch (c) {

					case 'h' :
						help = true;
						helpCommand = g.getOptarg();
						break;

					case 'C' :
						mainConfiguration = g.getOptarg();
						break;

					case 'A' :
						commandComp = false;
						break;

						// get everything that is specified in the provided configuration file, then fill in rest with other options, if provided
					case 'M' :
						getConnProperties(g.getOptarg());
						break;

					case 'H' :
						mainHost = g.getOptarg();
						break;

					case 'T' :
						mainDatabaseType = g.getOptarg();
						break;

					case 'P' :
						mainPort = g.getOptarg();
						break;

					case 'U' :
						mainUser = g.getOptarg();
						break;

					case 'p' :
						mainPassword = g.getOptarg();
						break;

					case 'D' :
						mainDatabase = g.getOptarg();
						break;

					case 'd' :
						mainDataset = g.getOptarg();
						break;

					case 'v' :
						verbose = true;
						break;

					case 'l' :
						loggingURL = g.getOptarg();
						break;

					case 'e' :
						mainBatchSQL = g.getOptarg();
						mainBatchMode = true;
						break;

					case 'O' :
						mainBatchFile = g.getOptarg();
						break;

					case 'F' :
						mainBatchFormat = g.getOptarg();
						break;

					case 'R' :
						mainBatchSeparator = g.getOptarg();
						break;

					case 'E' :
						mainBatchScriptFile = g.getOptarg();
						mainBatchMode = true;
						break;
				}
			}
		} else {
			args = new String[0];
		}

		// Initialise logging system
		if (loggingURL != null) {
			try {
				LoggingUtils.setLoggingConfiguration(new URL(loggingURL).openStream());
			} catch (SecurityException e) {
				System.out.println("Caught Security Exception when adding logger configuration URL");
				e.printStackTrace();
			} catch (MalformedURLException e) {
				System.out.println("User supplied URL " + loggingURL + " is not well formed");
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Could not read input from URL " + loggingURL + "\n");
				e.printStackTrace();
			}
		} else {
			LoggingUtils.setVerbose(verbose);
		}

		if (confinUse != null) {
			mainLogger.info("Using configuration file: " + confinUse + "\n");
		} else {
			mainLogger.info("Using commandline options only for connection configuration");
		}

		// check for help
		if (help) {
			if (helpCommand.equals(""))
				System.out.println(usage());
			else {
				MartShell ms = new MartShell();
				ms.UnsetCommandCompletion();
				try {
					System.out.println(ms.Help(helpCommand));
				} catch (InvalidQueryException e) {
					System.out.println("Couldnt provide Help for " + helpCommand + e.getMessage());
					e.printStackTrace();
				}
			}
			return;
		}

		MartShell ms = new MartShell();

		if (mainHost != null)
			ms.setDBHost(mainHost);

		if (mainDatabaseType != null)
			ms.setDBType(mainDatabaseType);

		if (mainPort != null)
			ms.setDBPort(mainPort);

		if (mainUser != null)
			ms.setDBUser(mainUser);

		if (mainPassword != null)
			ms.setDBPass(mainPassword);

		if (mainDatabase != null)
			ms.setDBDatabase(mainDatabase);

		if (mainConfiguration != null)
			ms.setAlternateMartConfiguration(mainConfiguration);

		if (mainDataset != null)
			ms.setDatasetName(mainDataset);

		if (mainBatchMode) {
			boolean validQuery = true;
			ms.UnsetCommandCompletion();

			if (mainBatchSQL == null && mainBatchScriptFile == null) {
				System.out.println("Must supply either a Query command or a query script\n" + usage());
				System.exit(0);
			} else if (mainBatchScriptFile != null) {
				validQuery = ms.RunBatchScript(mainBatchScriptFile);
			} else {
				if (mainBatchFile != null) {
					try {
						ms.setBatchOutputFile(mainBatchFile);
					} catch (Exception e) {
						validQuery = false;
					}
				}
				if (mainBatchFormat != null)
					ms.setBatchOutputFormat(mainBatchFormat);
				if (mainBatchSeparator == null)
					ms.setBatchOutputSeparator("\t"); //default
				else
					ms.setBatchOutputSeparator(mainBatchSeparator);

				validQuery = ms.RunBatch(mainBatchSQL);
			}
			if (!validQuery) {
				System.out.println("Invalid Batch command:" + ms.getBatchError() + "\n" + usage());
				System.exit(0);
			} else
				System.exit(0);
		} else {
			if (!commandComp)
				ms.UnsetCommandCompletion();
			ms.RunInteractive();
		}
	}

	public MartShell() {
	}

	/**
	 * Method for creating an interactive MartShell session.  This system
	 * will attempt to load the Java Readline library, first using Getline (Windows),
	 * then attempting EditLine (linux/Unix LGPL), then GnuReadline (Linux GPL), 
	 * and finally, given that all of these attempts have failed, loads the PureJava 
	 * library, which is merely a wrapper around standard out/standard in, and provides 
	 * no history/completion functionality.  The user is warned if PureJava is loaded.
	 * It sets state depending on which library sucessfully loaded.  If the user
	 * hasnt turned completion off with the -A flag, and a compatible library was loaded,
	 * a MartCompleter is initialized, and the Readline is linked to that to allow
	 * context sensitive command completion.  It then enters into the shell loop,
	 * which attempts to capture all Exceptions and report them to the user at
	 * the martshell prompt without exiting. 
	 *
	 */
	public void RunInteractive() {
		try {
			Readline.load(ReadlineLibrary.Getline);
			//		Getline doesnt support completion, or history manipulation/files
			completionOn = false;
			historyOn = true;
			readlineLoaded = true;
		} catch (UnsatisfiedLinkError ignore_me) {
			try {
				Readline.load(ReadlineLibrary.Editline);
				historyOn = true;
				readlineLoaded = true;
			} catch (UnsatisfiedLinkError ignore_me2) {
				try {
					Readline.load(ReadlineLibrary.GnuReadline);
					historyOn = true;
					readlineLoaded = true;
				} catch (UnsatisfiedLinkError ignore_me3) {
					mainLogger.warning(
						"Could not load Readline Library, commandline editing, completion will not be available"
							+ "\nConsult MartShell documentation for methods to resolve this error.");
					historyOn = false;
					readlineLoaded = false;
					Readline.load(ReadlineLibrary.PureJava);
				}
			}
		}

		Readline.initReadline("MartShell");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Readline.cleanup();
			}
		});

		try {
			// load help file
			LoadHelpFiles();

			//display startup information
			System.out.println(supportHelp.getProperty(STARTUP));
			System.out.println("connected to " + martDatabase + " on " + martHost + ":" + martPort + "\n");
		} catch (InvalidQueryException e2) {
			System.out.println("Couldnt display startup information\n" + e2.getMessage());

			StackTraceElement[] stacks = e2.getStackTrace();
			StringBuffer stackout = new StringBuffer();

			for (int i = 0, n = stacks.length; i < n; i++) {
				StackTraceElement element = stacks[i];
				stackout.append(element.toString()).append("\n");
			}
			mainLogger.info("\n\nStackTrace:\n" + stackout.toString());
		}

		try {
			if (martHost == null || martHost.length() < 5)
				setConnectionSettings(SETCONSETSC);
			Initialize();

			if (completionOn) {
				mcl = new MartCompleter(martconf);

				// add commands
				mcl.AddAvailableCommandsTo("commands", availableCommands);
				mcl.AddAvailableCommandsTo("commands", msl.availableCommands);

        mcl.AddAvailableCommandsTo(LISTC, listRequests);
        
				// add sequences
				mcl.AddAvailableCommandsTo(MartShellLib.QSEQUENCE, SequenceDescription.SEQS);

				if (helpLoaded)
					mcl.AddAvailableCommandsTo(HELPC, commandHelp.keySet());

				if (envDataset != null)
					mcl.setEnvDataset(envDataset);

				mcl.SetCommandMode();

				Readline.setCompleter(mcl);
			}

			mainLogger.info("Completer set\n");

			if (readlineLoaded && historyOn) {
				File histFile = new File(history_file);
				if (!histFile.exists())
					histFile.createNewFile();
				else
					LoadScriptFromFile(history_file);
			}

		} catch (Exception e1) {
			System.out.println("Could not initialize connection: " + e1.getMessage());

			StackTraceElement[] stacks = e1.getStackTrace();
			StringBuffer stackout = new StringBuffer();

			for (int i = 0, n = stacks.length; i < n; i++) {
				StackTraceElement element = stacks[i];
				stackout.append(element.toString()).append("\n");
			}
			mainLogger.info("\n\nStackTrace:\n" + stackout.toString());

			System.exit(1);
		}

		String thisline = null;
		while (true) {
			try {
				thisline = Prompt();

				if (thisline != null) {
					if (thisline.equals(EXITC) || thisline.equals(QUITC))
						break;
					parse(thisline);
					thisline = null;
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());

				StackTraceElement[] stacks = e.getStackTrace();
				StringBuffer stackout = new StringBuffer();

				for (int i = 0, n = stacks.length; i < n; i++) {
					StackTraceElement element = stacks[i];
					stackout.append(element.toString()).append("\n");
				}
				mainLogger.info("\n\nStackTrace:\n" + stackout.toString());

				conline = new StringBuffer();
				continueQuery = false;
				thisline = null;
			}
		}

		try {
			ExitShell();
		} catch (IOException e) {
			System.err.println("Warning, could not close Buffered Reader\n");
			StackTraceElement[] stacks = e.getStackTrace();
			StringBuffer stackout = new StringBuffer();

			for (int i = 0, n = stacks.length; i < n; i++) {
				StackTraceElement element = stacks[i];
				stackout.append(element.toString()).append("\n");
			}
			mainLogger.info("\n\nStackTrace:\n" + stackout.toString());

			System.exit(1);
		}
	}

	/**
	 * Method for running a batchScript file non-interactively.  This will attempt
	 * to parse/run all of the Mart Query Languge commands in the file in succession.
	 * It Returns true if the commands were executed successfully.  If there is an error,
	 * it sets the BatchError message, and returns false.
	 *  
	 * @param batchScriptFile - String path to file to be loaded and evaluated
	 * @return boolean true if all commands are executed successfully, false if not.
	 */
	public boolean RunBatchScript(String batchScriptFile) {
		historyOn = false;
		completionOn = false;
		readlineLoaded = false;

		boolean valid = true;
		try {
			Initialize();
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(batchScriptFile)));

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (!line.startsWith("#"))
					parse(line);
			}
		} catch (Exception e) {
			setBatchError(e.getMessage());
			StackTraceElement[] stacks = e.getStackTrace();
			StringBuffer stackout = new StringBuffer();

			for (int i = 0, n = stacks.length; i < n; i++) {
				StackTraceElement element = stacks[i];
				stackout.append(element.toString()).append("\n");
			}
			mainLogger.info("\n\nStackTrace:\n" + stackout.toString());
			valid = false;
		}
		return valid;
	}

	/**
	 * Method for running a single command non-interactively.  This will
	 * attempt to parse/run the entire queryString provided.  It returns
	 * true if the command was executed successfully, false if exceptions
	 * occur (including connection exceptions, IO Exceptions, etc.).  When
	 * Exceptions occur, the message is stored in the BatchError message.
	 * 
	 * @param querystring - String Mart Query Languate String to be evaluated
	 * @return boolean true if querystring is executed successfully, false if not.
	 */
	public boolean RunBatch(String querystring) {
		historyOn = false;
		completionOn = false;
		readlineLoaded = false;

		boolean validQuery = true;

		if (martHost == null || martHost.length() < 5) {
			validQuery = false;
			setBatchError("Must set Host");
		} else if (martUser == null || martUser.length() < 1) {
			validQuery = false;
			setBatchError("Must set a User");
		} else if (martDatabase == null || martDatabase.length() < 5) {
			validQuery = false;
			setBatchError("Must set a Mart Database");
		} else {
			try {
				Initialize();
				if (!querystring.endsWith(LINEEND))
					querystring = querystring + LINEEND;

				parseForCommands(querystring);
			} catch (Exception e) {
				setBatchError(e.getMessage());
				StackTraceElement[] stacks = e.getStackTrace();
				StringBuffer stackout = new StringBuffer();

				for (int i = 0, n = stacks.length; i < n; i++) {
					StackTraceElement element = stacks[i];
					stackout.append(element.toString()).append("\n");
				}
				mainLogger.info("\n\nStackTrace:\n" + stackout.toString());

				validQuery = false;
			}
		}
		return validQuery;
	}

	/**
	 * Method allowing client scripts to specifically turn off command completion
	 *
	 */
	public void UnsetCommandCompletion() {
		completionOn = false;
	}

	public void setLoggingConfigurationURL(URL conf) {
		loggingConfURL = conf;
	}

	/**
	 * Method allowing client scripts to specify an alternate MartConfiguration.xml
	 * document to use in place of that provided by the Mart database.
	 * 
	 * @param confFile - String path to alternate MartConfiguration.xml file
	 */
	public void setAlternateMartConfiguration(String confFile) {
		altConfigurationFile = confFile;
	}

	/**
	 * Set the Database Host name of the RDBMS.
	 * 
	 * @param dbhost - String host name of the RDBMS.
	 */
	public void setDBHost(String dbhost) {
		this.martHost = dbhost;
	}

	/**
	 * Set the Type of the RDBMS.  Defaults to mysql
	 * @param dbtype - String type of rdbms, to pass in a Connection URL
	 */
	public void setDBType(String dbtype) {
		this.martDatabaseType = dbtype;
	}

	/**
	 * Set the port for the RDBMS.
	 * 
	 * @param dbport - String Port that the RDBMS is running on
	 */
	public void setDBPort(String dbport) {
		this.martPort = dbport;
	}

	/**
	 * Set the username to use to connect to the specified RDBMS.
	 * 
	 * @param dbuser - String user name for RDBMS connection
	 */
	public void setDBUser(String dbuser) {
		this.martUser = dbuser;
	}

	/**
	 * Set the password to use to connect to the specified RDBMS.
	 * 
	 * @param dbpass - String password for RDBMS connection.
	 */
	public void setDBPass(String dbpass) {
		martPass = dbpass;
	}

	/**
	 * Set the name of the mart database to query.
	 * 
	 * @param db - String name of the mart database
	 */
	public void setDBDatabase(String db) {
		martDatabase = db;
	}

	/**
	 * Set the name of a file to output a batch Mart Query command using the -e flag
	 * 
	 * @param batchFile - String path to output file.
	 * @throws IOException when the specified file cannot be created.
	 */
	public void setBatchOutputFile(String batchFileName) throws IOException {
		try {
			File batchFile = new File(batchFileName);
			if (!batchFile.exists())
				batchFile.createNewFile();

			sessionOutputFile = new FileOutputStream(batchFile);
		} catch (FileNotFoundException e) {
			setBatchError("Could not open file " + batchFileName + "\n" + e.getMessage());
			throw e;
		}
	}

	/**
	 * Set the format for output of a batch Mart Query command using the -e flag
	 * 
	 * @param outputFormat - String format, must be either tabulated or fasta, or an exception will be thrown when the query executes.
	 */
	public void setBatchOutputFormat(String outputFormat) {
		this.sessionOutputFormat = outputFormat;
	}

	/**
	 * Set the output separator for tabulated output of a batch Mart Query command
	 * using the -e flag.
	 * 
	 * @param outputSeparator - String field separator (defaults to tab separated if none specified)
	 */
	public void setBatchOutputSeparator(String outputSeparator) {
		this.sessionOutputSeparator = outputSeparator;
	}

	/**
	 * sets the DatasetName to the provided String
	 * @param datasetName - string internalName of the dataset
	 */
	public void setDatasetName(String datasetName) {
		this.envDataset = datasetName;
	}

	/**
	 * Get any error message, if the runBatch or runBatchScript command returns
	 * false.
	 * 
	 * @return String error message
	 */
	public String getBatchError() {
		return batchErrorMessage;
	}

	private void setBatchError(String message) {
		batchErrorMessage = message;
	}

	private void Initialize() throws MalformedURLException, ConfigurationException, SQLException {
		engine = new Engine("mysql", martHost, martPort, martDatabase, martUser, martPass);

		if (altConfigurationFile != null)
			martconf = new MartConfigurationFactory().getInstance(new URL(altConfigurationFile));
		else
			martconf = engine.getMartConfiguration();

		if (msl == null)
			msl = new MartShellLib(martconf);
		else
			msl.setMartConfiguration(martconf);
	}

	private void ExitShell() throws IOException {
		Readline.cleanup();

		// close the sessionwide FileOutputStream, if it isnt null
		if (sessionOutputFile != null)
			sessionOutputFile.close();

		// if history and completion are on, save the history file
		if (readlineLoaded && historyOn)
			Readline.writeHistoryFile(history_file);

		System.exit(0);
	}

	private String Prompt() throws EOFException, UnsupportedEncodingException, IOException {
		String line = null;
		if (continueQuery)
			line = subPrompt();
		else
			line = mainPrompt();

		return line;
	}

	private String mainPrompt() throws EOFException, UnsupportedEncodingException, IOException {
		String prompt = null;
		String line = null;

		if (userPrompt != null)
			prompt = userPrompt;
		else
			prompt = DEFAULTPROMPT + "> ";

		if (completionOn)
			mcl.SetCommandMode();

		line = Readline.readline(prompt, historyOn);
		return line;
	}

	private String subPrompt() throws EOFException, UnsupportedEncodingException, IOException {
		return Readline.readline("% ", historyOn);
	}

	private String NormalizeCommand(String command) {
		String normalizedcommand = null;

		if (command.endsWith(LINEEND))
			normalizedcommand = command.substring(0, command.indexOf(LINEEND));
		else
			normalizedcommand = command;

		return normalizedcommand;
	}

	public String Help(String command) throws InvalidQueryException {
		if (!helpLoaded)
			LoadHelpFiles();

		StringBuffer buf = new StringBuffer();

		if (command.equals(HELPC)) {
			buf.append("\nAvailable items:\n");

			for (Iterator iter = commandHelp.keySet().iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				//mainLogger.info("ELEMENT IS <" + element + ">\nVALUE IS " + commandHelp.getProperty(element) + "\n" );
				buf.append("\t\t" + element).append("\n");
			}
			return buf.toString();

		} else {
			buf.append("\n");
			StringTokenizer hToks = new StringTokenizer(command, " ");
			hToks.nextToken(); // skip help
			command = hToks.nextToken();

			if (commandHelp.containsKey(command)) {
				String output = commandHelp.getProperty(command);

				//				check for domain specific bits in the help input to substitute
				Matcher m = DOMAINSPP.matcher(output);

				while (m.find()) {
					String replacement = m.group(1);
					if (supportHelp.containsKey(replacement))
						m.appendReplacement(buf, supportHelp.getProperty(replacement));
					else
						m.appendReplacement(buf, "");
				}
				m.appendTail(buf);

				output = buf.toString();
				buf = new StringBuffer();

				// check for INSERT
				m = INSERTP.matcher(output);
				while (m.find()) {
					String replacement = m.group(1);
					if (supportHelp.containsKey(replacement))
						m.appendReplacement(buf, supportHelp.getProperty(replacement));
					else
						m.appendReplacement(buf, "");
				}
				m.appendTail(buf);
			} else
				buf.append("Sorry, no information available for item: ").append(command).append("\n");

			return buf.toString();
		}
	}

	private void LoadHelpFiles() throws InvalidQueryException {
		URL help = ClassLoader.getSystemResource(HELPFILE);
		URL dshelp = ClassLoader.getSystemResource(DSHELPFILE);
		URL helpsupport = ClassLoader.getSystemResource(HELPSUPPORT);
		URL dshelpsupport = ClassLoader.getSystemResource(DSHELPSUPPORT);
		try {
			commandHelp.load(help.openStream());
			commandHelp.load(dshelp.openStream());

			supportHelp.load(helpsupport.openStream());
			supportHelp.load(dshelpsupport.openStream());

			if (!historyOn) {
				commandHelp.remove(HISTORYQ);
				commandHelp.remove(EXECC);
				availableCommands.remove(EXECC);
				commandHelp.remove(HISTORYC);
				availableCommands.remove(HISTORYC);
				commandHelp.remove(LOADSCRIPTC);
				availableCommands.remove(LOADSCRIPTC);
				commandHelp.remove(SAVETOSCRIPTC);
				availableCommands.remove(SAVETOSCRIPTC);
			}

			if (!completionOn)
				commandHelp.remove(COMPLETIONQ);
		} catch (IOException e) {
			helpLoaded = false;
			throw new InvalidQueryException("Could not load Help File " + e.getMessage());
		}
		helpLoaded = true;
	}

	private String[] ColumnIze(String input) {
		int chrCount = 0;

		List lines = new ArrayList();
		StringBuffer buf = new StringBuffer();

		StringTokenizer words = new StringTokenizer(input, " ");
		while (words.hasMoreTokens()) {
			String thisWord = words.nextToken();

			int cnt = chrCount + thisWord.length() + 1;
			if (cnt >= MAXCHARCOUNT) {
				lines.add(buf.toString());
				buf = new StringBuffer(thisWord);
				buf.append(" ");
				chrCount = 0;
			} else {
				buf.append(thisWord).append(" ");
				chrCount += thisWord.length();
			}
		}

		if (buf.length() > 0)
			lines.add(buf.toString()); // add the last bit of the buffer

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private void PageOutput(String[] lines) {
		int linesout = 0;
		for (int i = 0, n = lines.length; i < n; i++) {
			if (linesout > MAXLINECOUNT) {
				linesout = 0;
				try {
					String quit = Readline.readline("\n\nHit Enter to continue, q to return to prompt: ", false);
					if (quit.equals("q")) {
						System.out.println();
						break;
					}

					System.out.println("\n");
				} catch (Exception e) {
					// do nothing
				}
			}
			String line = lines[i];
			System.out.print(line);
			linesout++;
		}
	}

	private void ListRequest(String command) throws InvalidQueryException {
		System.out.println();
		String[] toks = command.split("\\s+");

		if (toks.length == 2) {
			String request = toks[1];
			String[] lines = null;

			if (request.equalsIgnoreCase("datasets"))
				lines = ListDatasets();
			else if (request.equalsIgnoreCase("filters"))
				lines = ListFilters();
			else if (request.equalsIgnoreCase("attributes"))
				lines = ListAttributes();
			else
				throw new InvalidQueryException("Invalid list command recieved: " + command + "\n");

			if (lines != null) {
				PageOutput(lines);
			}
		} else
			throw new InvalidQueryException("Invalid list command recieved: " + command + "\n");
		System.out.println();
	}

	private String[] ListDatasets() {
		DatasetView[] ds = martconf.getDatasets();
		String[] ret = new String[ds.length];

		for (int i = 0, n = ds.length; i < n; i++)
			ret[i] = ds[i].getInternalName() + "\n";

		return ret;
	}

	private String[] ListFilters() throws InvalidQueryException {
		if (envDataset != null) {
			if (!martconf.containsDataset(envDataset))
				throw new InvalidQueryException("This mart does not support dataset " + envDataset + "\n");

			int blen = 3; //3 filters/line
			DatasetView dset = martconf.getDatasetByName(envDataset);
			List columns = new ArrayList();
			String[] buffer = new String[blen];

			int[] maxlengths = new int[] { 0, 0, 0 };

			List names = dset.getFilterCompleterNames();
			int pos = 0;
			for (Iterator iter = names.iterator(); iter.hasNext();) {
				String name = (String) iter.next();

				if (pos == blen) {
					columns.add(buffer);
					buffer = new String[blen];
					pos = 0;
				}
				buffer[pos] = name;
				if (name.length() > maxlengths[pos])
					maxlengths[pos] = name.length();
				pos++;
			}

			if (pos > 0)
				columns.add(buffer);

			return formatColumns(columns, maxlengths);
		} else
			throw new InvalidQueryException("Must set a dataset with the use command to list filters\n");
	}

	private String[] ListAttributes() throws InvalidQueryException {
		if (envDataset != null) {
			if (!martconf.containsDataset(envDataset))
				throw new InvalidQueryException("This mart does not support dataset " + envDataset + "\n");

			int blen = 3; //3 atts/line
			DatasetView dset = martconf.getDatasetByName(envDataset);
			List columns = new ArrayList();
			String[] buffer = new String[blen];

			int[] maxlengths = new int[] { 0, 0, 0 };

			List names = dset.getAttributeCompleterNames();
			int pos = 0;
			for (Iterator iter = names.iterator(); iter.hasNext();) {
				String name = (String) iter.next();

				if (pos == blen) {
					columns.add(buffer);
					buffer = new String[blen];
					pos = 0;
				}
				buffer[pos] = name;

				if (name.length() > maxlengths[pos])
					maxlengths[pos] = name.length();
				pos++;
			}

			if (pos > 0)
				columns.add(buffer);

			return formatColumns(columns, maxlengths);
		} else
			throw new InvalidQueryException("Must set a dataset with the use command to list attributes\n");
	}

	private String[] formatColumns(List columns, int[] maxlengths) {

		int[] pos = new int[] { 0, 0, 0 }; // position matrix, change pos[0] to increase leftmost padding

		int maxtotal = 0;
		for (int i = 0, n = maxlengths.length; i < n; i++) {
			maxtotal += maxlengths[i];
		}

		int minSpace = 5; //default
		if (maxtotal < MAXCHARCOUNT)
			minSpace = (MAXCHARCOUNT - maxtotal) / (maxlengths.length - 1);

		//calculate positions 2 onward
		for (int i = 1, n = maxlengths.length; i < n; i++) {
			pos[i] = pos[i - 1] + maxlengths[i - 1] + minSpace;
		}

		List lines = new ArrayList();

		for (Iterator iter = columns.iterator(); iter.hasNext();) {
			String[] lc = (String[]) iter.next();
			StringBuffer thisLine = new StringBuffer();
			int len = thisLine.length();

			for (int i = 0, n = lc.length; i < n; i++) {
				if (lc[i] != null) {
					while (len < pos[i]) {
						thisLine.append(" ");
						len++;
					}
					thisLine.append(lc[i]);
					len = thisLine.length();
				}
			}
			thisLine.append("\n");
			lines.add(thisLine.toString());
		}

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private void DescribeRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		int tokCount = toks.countTokens();
		toks.nextToken(); // skip describe

		System.out.println();

		if (tokCount < 3)
			throw new InvalidQueryException("Invalid Describe request " + command + "\n");
		else {
			String request = toks.nextToken();
			String name = toks.nextToken();

			if (request.equalsIgnoreCase(DATASETKEY)) {
				String[] lines = DescribeDataset(name);

				PageOutput(lines);
			} else if (request.equalsIgnoreCase(FILTERKEY)) {
				if (envDataset == null)
					throw new InvalidQueryException("Must set a dataset with a use command for describe filter to work\n");

				DatasetView dset = martconf.getDatasetByName(envDataset);
				if (!(dset.containsFilterDescription(name)))
					throw new InvalidQueryException("Filter " + name + " is not supported by dataset " + envDataset + "\n");

				List quals = dset.getFilterCompleterQualifiersByInternalName(name);
				StringBuffer qual = new StringBuffer();
				for (int k = 0, l = quals.size(); k < l; k++) {
					if (k > 0)
						qual.append(", ");
					String element = (String) quals.get(k);
					qual.append(element);
				}

				String tmp = DescribeFilter(name, dset.getFilterDescriptionByInternalName(name), qual.toString());
				System.out.println(tmp + "\n");
			} else if (request.equalsIgnoreCase(ATTRIBUTEKEY)) {
				if (envDataset == null)
					throw new InvalidQueryException("Must set a dataset with a use command for describe filter to work\n");

				DatasetView dset = martconf.getDatasetByName(envDataset);
				if (!dset.containsAttributeDescription(name))
					throw new InvalidQueryException("Attribute " + name + " is not supported by dataset " + envDataset + "\n");

				String tmp = DescribeAttribute(dset.getAttributeDescriptionByInternalName(name));
				System.out.println(tmp + "\n");
			} else
				throw new InvalidQueryException(
					"Invalid Request key in describe command, see help describe. " + command + "\n");
		}
	}

	private String[] DescribeDataset(String dsetname) throws InvalidQueryException {
		if (!martconf.containsDataset(dsetname))
			throw new InvalidQueryException("This mart does not support dataset " + dsetname + "\n");

		List lines = new ArrayList();

		DatasetView dset = martconf.getDatasetByName(dsetname);
		//filters first
		FilterPage[] fpages = dset.getFilterPages();
		for (int i = 0, n = fpages.length; i < n; i++) {
			FilterPage page = fpages[i];
			lines.add("The following filters can be applied in the same query\n");
			lines.add("\n");

			List names = page.getCompleterNames();
			for (int j = 0, m = names.size(); j < m; j++) {
				String name = (String) names.get(j);

				List quals = page.getFilterCompleterQualifiersByInternalName(name);
				StringBuffer qual = new StringBuffer();
				for (int k = 0, l = quals.size(); k < l; k++) {
					if (k > 0)
						qual.append(", ");
					String element = (String) quals.get(k);
					qual.append(element);
				}

				lines.add(DescribeFilter(name, page.getFilterDescriptionByInternalName(name), qual.toString()));
				lines.add("\n");
			}
		}

		//attributes
		AttributePage[] apages = dset.getAttributePages();
		for (int i = 0, n = apages.length; i < n; i++) {
			AttributePage page = apages[i];
			lines.add("\n");
			lines.add("\n");
			lines.add("The following Attributes can be querried together\n");
			lines.add(
				"numbers in perentheses denote groups of attributes that have limits on the number that can be queried together\n");
			lines.add("\n");

			List groups = page.getAttributeGroups();
			for (Iterator iter = groups.iterator(); iter.hasNext();) {
				Object obj = iter.next();

				if (obj instanceof AttributeGroup) {
					AttributeGroup group = (AttributeGroup) obj;
					AttributeCollection[] cols = group.getAttributeCollections();

					for (int j = 0, m = cols.length; j < m; j++) {
						AttributeCollection collection = cols[j];

						List atts = collection.getAttributeDescriptions();
						int maxSelect = collection.getMaxSelect();
						for (Iterator iterator = atts.iterator(); iterator.hasNext();) {
							Object element = iterator.next();
							String tmp = DescribeAttribute(element);

							if (maxSelect > 0)
								lines.add(tmp + " (" + maxSelect + ")");
							else
								lines.add(tmp);
							lines.add("\n");
						}
					}
				}
			}
		}

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String DescribeFilter(String iname, FilterDescription desc, String qualifiers) throws InvalidQueryException {
		String displayName = desc.getDisplayname(iname);

		return iname + " - " + displayName + " (" + qualifiers + ")";
	}

	private String DescribeAttribute(Object attributeo) {
		if (attributeo instanceof AttributeDescription) {
			AttributeDescription desc = (AttributeDescription) attributeo;

			String iname = desc.getInternalName();
			String displayName = desc.getDisplayName();
			return iname + " - " + displayName;
		} else
			//dsattributedescription, if ever implimented
			return null;
	}

	private void setPrompt(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		if (toks.countTokens() < 2)
			throw new InvalidQueryException("Invalid setPrompt Command Recieved: " + command + "\n" + Help(SETPROMPT));
		toks.nextToken(); // skip command
		String tmp = toks.nextToken();

		if (tmp.equals("-"))
			userPrompt = null;
		else
			userPrompt = tmp;
	}

	private void setConnectionSettings(String command) throws InvalidQueryException {
		StringTokenizer ctokens = new StringTokenizer(command, " ");
		if (ctokens.countTokens() > 1) {
			// parse command
			ctokens.nextToken(); // throw away
			String connSettings = ctokens.nextToken();

			StringTokenizer sTokens = new StringTokenizer(connSettings, ",");
			while (sTokens.hasMoreTokens()) {
				StringTokenizer tokens = new StringTokenizer(sTokens.nextToken(), "=");
				if (tokens.countTokens() < 2)
					throw new InvalidQueryException(
						"Recieved invalid setConnectionSettings command.\n" + Help(SETCONSETSC) + "\n");

				String key = tokens.nextToken();
				String value = tokens.nextToken();

				if (key.equals(DBHOST))
					martHost = value;
				else if (key.equals(DATABASETYPE))
					martDatabaseType = value;
				else if (key.equals(DBPORT))
					martPort = value;
				else if (key.equals(DBUSER))
					martUser = value;
				else if (key.equals(DBPASSWORD))
					martPass = value;
				else if (key.equals(DATABASENAME))
					martDatabase = value;
				else if (key.equals(ALTCONFFILE))
					altConfigurationFile = value;
				else
					throw new InvalidQueryException(
						"Recieved invalid setConnectionSettings command.\n" + Help(SETCONSETSC) + "\n");
			}
		} else {
			if (mainBatchMode)
				throw new InvalidQueryException("Recieved invalid setConnectionSettings command.\n" + Help(SETCONSETSC) + "\n");

			String thisLine = null;

			try {

				String myHost = (martHost == null) ? "" : martHost;
				thisLine =
					Readline.readline(
						"\nPlease enter the host address of the mart database (press enter to leave as '" + myHost + "'): ",
						false);
				if (thisLine != null)
					martHost = thisLine;

				String myDBType = (martDatabaseType == null) ? "" : martDatabaseType;
				thisLine =
					Readline.readline(
						"\nPlease enter the type of RDBMS hosting the mart database (press enter to leave as '" + myDBType + "'): ",
						false);
				if (thisLine != null)
					martDatabaseType = thisLine;

				String myPort = (martPort == null) ? "" : martPort;
				thisLine =
					Readline.readline(
						"\nPlease enter the port on which the mart database is running (press enter to leave as '"
							+ myPort
							+ "'): ",
						false);
				if (thisLine != null)
					martPort = thisLine;

				String myUser = (martUser == null) ? "" : martUser;
				thisLine =
					Readline.readline(
						"\nPlease enter the user name used to connect to the mart database (press enter to leave as '"
							+ myUser
							+ "'): ",
						false);
				if (thisLine != null)
					martUser = thisLine;

				String myPass = "";
				if (martPass != null) {
					for (int i = 0, n = martPass.length(); i < n; i++)
						myPass += "*";
				}

				thisLine =
					Readline.readline(
						"\nPlease enter the password used to connect to the mart database (press enter to leave as '"
							+ myPass
							+ "'): ",
						false);
				if (thisLine != null)
					martPass = thisLine;

				String myDb = (martDatabase == null) ? "" : martDatabase;
				thisLine =
					Readline.readline(
						"\nPlease enter the name of the mart database you wish to query (press enter to leave as '" + myDb + "'): ",
						false);
				if (thisLine != null)
					martDatabase = thisLine;

				String myAltFile = (altConfigurationFile == null) ? "-" : altConfigurationFile;
				thisLine =
					Readline.readline(
						"\nPlease enter the URL for the XML Configuration File for the new mart (press enter to leave as '"
							+ myAltFile
							+ "',\n enter '-' to use configuration provided by "
							+ martDatabase
							+ "):",
						false);
				if (thisLine != null) {
					if (thisLine.equals("-"))
						altConfigurationFile = null;
					else
						altConfigurationFile = thisLine;
				}

			} catch (Exception e) {
				throw new InvalidQueryException("Problem reading input for mart connection settings: " + e.getMessage());
			}
		}
		try {
			Initialize();
		} catch (Exception e) {
			throw new InvalidQueryException("Could not initialize connection: " + e.getMessage());
		}
	}

	private void showConnectionSettings() {
		String conf = martDatabase;
		if (altConfigurationFile != null)
			conf = altConfigurationFile;

		System.out.println(
			"\nMart connections now:"
				+ "\nHost "
				+ martHost
				+ "\nDatabaseType "
				+ martDatabaseType
				+ "\nPort "
				+ martPort
				+ "\nUser "
				+ martUser
				+ "\nDatabase "
				+ martDatabase
				+ "\nusing mart configuration from: "
				+ conf
				+ "\n");
	}

	private void setVerbose(String command) throws InvalidQueryException {
		if (loggingConfURL == null) {
			if (!command.matches("\\w+\\s(on|off)"))
				throw new InvalidQueryException("Invalid setVerbose command recieved: " + command + "\n");

			String val = command.split("\\s")[1];
			verbose = (val.equals("on")) ? true : false;

			System.out.println("Logging now " + val + "\n");
			LoggingUtils.setVerbose(verbose);
		} else
			throw new InvalidQueryException("Cannot change logging properties when a logging configuration URL is supplied\n");
	}

	private void setOutputSettings(String command) throws InvalidQueryException {
		try {
			StringTokenizer ctokens = new StringTokenizer(command, " ");
			ctokens.nextToken();
			String fSettings = ctokens.nextToken();

			StringTokenizer fTokens = new StringTokenizer(fSettings, ",");
			while (fTokens.hasMoreTokens()) {
				StringTokenizer tokens = new StringTokenizer(fTokens.nextToken(), "=");
				if (tokens.countTokens() < 2)
					throw new InvalidQueryException(
						"Recieved invalid setOutputFormat request: " + command + "\n" + Help(SETOUTSETSC) + "\n");

				String key = tokens.nextToken();
				String value = tokens.nextToken();

				if (key.equals(FILE)) {
					if (value.equals("-")) {
						if (sessionOutputFile != null)
							sessionOutputFile.close();
						sessionOutputFile = null;
					} else {
						if (sessionOutputFile != null)
							sessionOutputFile.close();
						sessionOutputFile = new FileOutputStream(value);
					}
				} else if (key.equals(FORMAT))
					sessionOutputFormat = value;
				else if (key.equals(SEPARATOR))
					if (key.equals("comma"))
						sessionOutputSeparator = ",";
					else
						sessionOutputSeparator = value;
				else
					throw new InvalidQueryException(
						"Recieved invalid setOutputFormat request: " + command + "\n" + Help(SETOUTSETSC) + "\n");
			}
		} catch (Exception e) {
			throw new InvalidQueryException("Could not set output settings: " + e.getMessage() + "\n");
		}
	}

	private void showOutputSettings() {
		String thisFile = "stdout";
		if (sessionOutputFile != null)
			thisFile = sessionOutputFile.toString();

		System.out.println(
			"Output Format: "
				+ FORMAT
				+ " = "
				+ sessionOutputFormat
				+ ", "
				+ SEPARATOR
				+ " = "
				+ "'"
				+ sessionOutputSeparator
				+ "'"
				+ ", "
				+ FILE
				+ " = "
				+ thisFile);
	}

	private void useCommand(String command) throws InvalidQueryException {
		if (command.startsWith(USEC)) {
			StringTokenizer tokens = new StringTokenizer(command, " ");
			if (tokens.countTokens() == 2) {
				tokens.nextToken();
				useCommand(tokens.nextToken());
			} else
				throw new InvalidQueryException(
					"invalid use command: does not contain a dataset to use: "
						+ command
						+ " has "
						+ tokens.countTokens()
						+ " tokens\n");

		} else {
			if (martconf.containsDataset(command))
				envDataset = command;
			else
				throw new InvalidQueryException("MartConfiguration does not contain dataset " + command + "\n");
		}

		if (completionOn)
			mcl.setEnvDataset(envDataset);
	}

	private void showDataset() {
		String dsetName = (envDataset != null) ? envDataset : "not set";
		System.out.println("Current DatasetView " + dsetName);
	}

	private void WriteHistory(String command) throws InvalidQueryException {
		try {
			StringTokenizer com = new StringTokenizer(command, " ");
			int tokCount = com.countTokens();
			com.nextToken(); // skip commmand start

			String req = null;

			String outPutFileName = null;
			File outPutFile = null;

			if (tokCount < 2)
				throw new InvalidQueryException("WriteHistory command must be provided a valid URL: " + command + "\n");
			else if (tokCount == 2) {
				//file
				outPutFileName = com.nextToken();
				outPutFile = new File(outPutFileName);
			} else if (tokCount == 3) {
				req = com.nextToken();
				outPutFileName = com.nextToken();
				outPutFile = new File(outPutFileName);
			} else
				throw new InvalidQueryException("Recieved invalid WriteHistory request " + command + "\n");

			WriteHistoryLinesToFile(req, outPutFile);

		} catch (Exception e) {
			throw new InvalidQueryException("Could not write history " + e.getMessage());
		}
	}

	private void WriteHistoryLinesToFile(String req, File outPutFile) throws InvalidQueryException {
		String[] lines = GetHistoryLines(req);
		// will throw an exception if GetHistoryLines requirements are not satisfied

		try {
			OutputStreamWriter hisout = new OutputStreamWriter(new FileOutputStream(outPutFile));
			for (int i = 0, n = lines.length; i < n; i++) {
				String thisline = lines[i];
				if (!thisline.startsWith(SAVETOSCRIPTC))
					hisout.write(thisline + "\n");
			}
			hisout.close();
		} catch (Exception e) {
			throw new InvalidQueryException(e.getMessage());
		}
	}

	private void ExecuteScript(String command) throws InvalidQueryException {
		StringTokenizer com = new StringTokenizer(command, " ");
		if (!(com.countTokens() > 1))
			throw new InvalidQueryException("Recieved invalid LoadScript command, must supply a URL\n");

		com.nextToken(); // skip command start

		String scriptFileName = null;
		File scriptFile = null;
		try {
			scriptFileName = com.nextToken();
			scriptFile = new File(scriptFileName);
			ExecScriptFromFile(scriptFile);
		} catch (Exception e) {
			throw new InvalidQueryException("Could not execute script: " + scriptFileName + " " + e.getMessage());
		}
	}

	private void ExecScriptFromFile(File scriptFile) throws InvalidQueryException {
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile)));

			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (historyOn)
					Readline.addToHistory(line);
				parse(line);
			}
		} catch (Exception e) {
			throw new InvalidQueryException(e.getMessage());
		}
	}

	private void LoadScript(String command) throws InvalidQueryException {
		StringTokenizer com = new StringTokenizer(command, " ");
		if (!(com.countTokens() > 1))
			throw new InvalidQueryException("Recieved invalid LoadScript command, must supply a URL\n");

		com.nextToken(); // skip command start

		String scriptFile = null;
		try {
			scriptFile = com.nextToken();
			LoadScriptFromFile(scriptFile);
		} catch (Exception e) {
			throw new InvalidQueryException("Could not load script: " + scriptFile + " " + e.getMessage());
		}
	}

	private void LoadScriptFromFile(String scriptFile) throws InvalidQueryException {
		if (!readlineLoaded)
			throw new InvalidQueryException("Sorry, histrory functions are not available on your terminal.\n");
		if (!historyOn)
			throw new InvalidQueryException("Sorry, histrory is not activated.\n");

		try {
			Readline.readHistoryFile(scriptFile);
		} catch (Exception e) {
			throw new InvalidQueryException(e.getMessage());
		}
	}

	private void Execute(String command) throws InvalidQueryException {
		try {
			StringTokenizer com = new StringTokenizer(command, " ");
			int tokCount = com.countTokens();

			if (tokCount < 2)
				throw new InvalidQueryException(Help(EXECC));
			else {
				com.nextToken(); // skip commmand start
				String req = com.nextToken();

				String[] lines = GetHistoryLines(req);
				// will throw an exception if GetHistoryLines requirements are not satisfied
				for (int i = 0, n = lines.length; i < n; i++) {
					String thisline = lines[i];

					if (historyOn)
						Readline.addToHistory(thisline);

					while (thisline != null)
						thisline = parseForCommands(thisline);
				}
			}
		} catch (Exception e) {
			throw new InvalidQueryException("Could not execute command " + command + " " + e.getMessage());
		}
	}

	private void History(String command) throws InvalidQueryException {
		try {
			StringTokenizer com = new StringTokenizer(command, " ");
			int start = 1;

			String req = null;

			if (com.countTokens() > 1) {
				com.nextToken(); // skip commmand start
				req = com.nextToken();

				int compos = req.indexOf(",");
				if (compos >= 0) {
					if (compos > 0) {
						//n, or n,y
						try {
							start = Integer.parseInt(req.substring(0, compos));
						} catch (NumberFormatException nfe) {
							throw new InvalidQueryException(nfe.getMessage());
						}
					}
				} else //n
					try {
						start = Integer.parseInt(req);
					} catch (NumberFormatException nfe) {
						throw new InvalidQueryException(nfe.getMessage());
					}
			}

			String[] lines = GetHistoryLines(req);
			// will throw an exception if GetHistoryLines requirements are not satisfied
			for (int i = 0, n = lines.length; i < n; i++) {
				System.out.print(start + " " + lines[i] + "\n");
				start++;
			}
		} catch (Exception e) {
			throw new InvalidQueryException("Could not show history " + e.getMessage());
		}
	}

	private String[] GetHistoryLines(String req) throws InvalidQueryException {
		if (!readlineLoaded)
			throw new InvalidQueryException("Sorry, histrory functions are not available on your terminal.\n");
		if (!historyOn)
			throw new InvalidQueryException("Sorry, histrory is not activated.\n");

		List lines = new ArrayList();
		if (req == null)
			Readline.getHistory(lines);
		else {
			int start = 0;
			int end = 0;

			if (req.indexOf(",") > -1) {
				StringTokenizer pos = new StringTokenizer(req, ",", true);

				if (pos.countTokens() > 2) {
					//n,y
					try {
						start = Integer.parseInt(pos.nextToken()) - 1;
						pos.nextToken(); // skip ,
						end = Integer.parseInt(pos.nextToken());
					} catch (NumberFormatException nfe) {
						throw new InvalidQueryException(nfe.getMessage());
					}
				} else {
					//either n, or ,y
					try {
						String tmp = pos.nextToken();
						if (tmp.equals(",")) {
							start = 0;
							end = Integer.parseInt(pos.nextToken());
						} else {
							start = Integer.parseInt(tmp) - 1;
							end = Readline.getHistorySize();
						}
					} catch (NumberFormatException nfe) {
						throw new InvalidQueryException(nfe.getMessage());
					}
				}

				for (int i = start; i < end; i++)
					lines.add(Readline.getHistoryLine(i));
			} else {
				try {
					start = Integer.parseInt(req) - 1;
				} catch (NumberFormatException nfe) {
					throw new InvalidQueryException(nfe.getMessage());
				}
				lines.add(Readline.getHistoryLine(start));
			}
		}
		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private void parse(String line)
		throws SequenceException, FormatException, InvalidQueryException, IOException, SQLException {
		if (line.indexOf(LINEEND) >= 0) {
			String residual = parseForCommands(conline.append(" ").append(line).toString().trim());
			if (residual != null) {
				continueQuery = true;
				conline = new StringBuffer(residual);

				if (completionOn)
					mcl.SetModeForLine(residual);
			} else {
				continueQuery = false;
				conline = new StringBuffer();
			}
		} else {
			conline.append(" ").append(line);
			continueQuery = true;

			//MartCompleter Mode
			if (completionOn)
				mcl.SetModeForLine(line);
		}
	}

	private String parseForCommands(String line)
		throws SequenceException, FormatException, InvalidQueryException, IOException, SQLException {
		StringBuffer residual = new StringBuffer();

		StringTokenizer commandtokens = new StringTokenizer(line, LINEEND, true);

		while (commandtokens.hasMoreTokens()) {
			String thisCommand = commandtokens.nextToken().trim();
			if (thisCommand.equals(LINEEND)) {
				if (residual.length() > 1) {
					parseCommand(residual.toString());
					residual = new StringBuffer();
				}
			} else
				residual = new StringBuffer(thisCommand);
		}

		if (residual.length() > 0)
			return residual.toString();
		else
			return null;
	}

	private void parseCommand(String command)
		throws SequenceException, FormatException, InvalidQueryException, IOException, SQLException {
		int cLen = command.length();

		command = command.replaceAll("\\s;$", ";");
		// removes any whitespace before the ; character

		if (cLen == 0)
			return;
		else if (command.startsWith(USEC))
			useCommand(NormalizeCommand(command));
		else if (command.startsWith(SHOWDATASETC))
			showDataset();
		else if (command.startsWith(HELPC))
			System.out.print(Help(NormalizeCommand(command)));
		else if (command.startsWith(DESCC))
			DescribeRequest(NormalizeCommand(command));
		else if (command.startsWith(LISTC))
			ListRequest(NormalizeCommand(command));
		else if (command.startsWith(SETCONSETSC))
			setConnectionSettings(NormalizeCommand(command));
		else if (command.startsWith(SHOWCONSETSC))
			showConnectionSettings();
		else if (command.startsWith(SETPROMPT))
			setPrompt(NormalizeCommand(command));
		else if (command.startsWith(SETOUTSETSC))
			setOutputSettings(NormalizeCommand(command));
		else if (command.startsWith(SHOWOUTSETSC))
			showOutputSettings();
		else if (command.startsWith(SETVERBOSEC))
			setVerbose(NormalizeCommand(command));
		else if (command.startsWith(HISTORYC))
			History(NormalizeCommand(command));
		else if (command.startsWith(EXECC))
			Execute(NormalizeCommand(command));
		else if (command.startsWith(LOADSCRIPTC))
			LoadScript(NormalizeCommand(command));
		else if (command.startsWith(RUNSCRIPTC))
			ExecuteScript(NormalizeCommand(command));
		else if (command.startsWith(SAVETOSCRIPTC))
			WriteHistory(NormalizeCommand(command));
		else if (NormalizeCommand(command).equals(EXITC) || NormalizeCommand(command).equals(QUITC))
			ExitShell();
		else if (command.startsWith(MartShellLib.GETQSTART) || command.startsWith(MartShellLib.USINGQSTART)) {
			if (envDataset != null)
				msl.setDataset(envDataset);

			//is it a store command
			Matcher storeMatcher = MartShellLib.STOREPAT.matcher(command);
			if (storeMatcher.matches()) {

				mainLogger.info("Recieved storeCommand " + command + "\n");

				String storedCommand = storeMatcher.group(1);
				String key = storeMatcher.group(4);

				mainLogger.info("as command " + command + "\n\nkey = " + key + "\nstoredCommand = " + storedCommand + "\n");

				if (key.indexOf(LINEEND) > 0)
					key = key.substring(0, key.indexOf(LINEEND));

				msl.addStoredMQLCommand(key, storedCommand.toString());
			} else {
				Query query = msl.MQLtoQuery(command);

				OutputStream os = null;
				if (sessionOutputFile != null)
					os = sessionOutputFile;
				else
					os = System.out;

				FormatSpec fspec = null;

				if (sessionOutputFormat != null) {
					if (sessionOutputFormat.equals("fasta"))
						fspec = FormatSpec.FASTAFORMAT;
					else {
						fspec = new FormatSpec(FormatSpec.TABULATED);

						if (sessionOutputSeparator != null)
							fspec.setSeparator(sessionOutputSeparator);
						else
							fspec.setSeparator(DEFOUTPUTSEPARATOR);
					}

				} else
					fspec = FormatSpec.TABSEPARATEDFORMAT;

				engine.execute(query, fspec, os);
			}
		} else {
			throw new InvalidQueryException("\nInvalid Command: please try again " + command + "\n");
		}
	}

	// MartShell instance variables
	private Engine engine;
	private MartConfiguration martconf;
	private MartShellLib msl = null;
	private BufferedReader reader;
	private boolean verbose = false;
	private URL loggingConfURL = null;

	private final String history_file = System.getProperty("user.home") + "/.martshell_history";

	private String martHost = null;
	private String martPort = null;
	private String martUser = null;
	private String martPass = null;
	private String martDatabase = null;
	private String martDatabaseType = "mysql"; //default
	private MartCompleter mcl;
	// will hold the MartCompleter, if Readline is loaded and completion turned on
	private boolean helpLoaded = false;
	// first time Help function is called, loads the help properties file and sets this to true
	private boolean historyOn = false; // commandline history, default to off
	private boolean completionOn = true; // command completion, default to on
	private boolean readlineLoaded = false;
	// true only if functional Readline library was loaded, false if PureJava
	private String userPrompt = null;
	private final String DEFAULTPROMPT = "MartShell";

	private String altConfigurationFile = null;
	private FileOutputStream sessionOutputFile = null;
	// this is set using the setOutputSettings command.
	private final String DEFOUTPUTFORMAT = "tabulated";
	// default to tabulated output
	private String sessionOutputFormat = null;
	// this is set using the setOutputSettings command.
	private final String DEFOUTPUTSEPARATOR = "\t"; // default to tab separated
	private String sessionOutputSeparator = null;
	// this is set using the setOutputSettings command.

	private String batchErrorMessage = null;
	private Properties commandHelp = new Properties();
	private Properties supportHelp = new Properties();

	private final String HELPFILE = "data/help.properties";
	//contains help general to the shell
	private final String DSHELPFILE = "data/dshelp.properties";
	// contains help for domain specific aspects
	private final String HELPSUPPORT = "data/helpSupport.properties";
	private final String DSHELPSUPPORT = "data/dshelpSupport.properties";

	private final Pattern DOMAINSPP = Pattern.compile("DOMAINSPECIFIC:(\\w+)", Pattern.DOTALL);
	private Pattern INSERTP = Pattern.compile("INSERT:(\\w+)", Pattern.DOTALL);

	// available commands
	private final String EXITC = "exit";
	private final String QUITC = "quit";
	private final String HELPC = "help";
	private final String DESCC = "describe";
	private final String USEC = "use";
	private final String SHOWDATASETC = "showDataset";
	private final String SETOUTSETSC = "setOutputSettings";
	private final String SHOWOUTSETSC = "showOutputSettings";
	private final String SETCONSETSC = "setConnectionSettings";
	private final String SHOWCONSETSC = "showConnectionSettings";
	private final String SETPROMPT = "setPrompt";
	private final String SETVERBOSEC = "setVerbose";
	private final String SHOWVERBOSEC = "showVerbose";
	private final String EXECC = "exec";
	private final String RUNSCRIPTC = "runScript";
	private final String LOADSCRIPTC = "loadScript";
	private final String SAVETOSCRIPTC = "saveToScript";
	private final String HISTORYC = "history";
	private final String LISTC = "list";
  
  private final List listRequests = Collections.unmodifiableList( 
                                                           new ArrayList( 
                                                                 Arrays.asList ( 
                                                                       new String[] { "datasets",
                                                                       	                      "filters",
                                                                       	                      "attributes"
                                                                       } 
  	)));
  
	protected List availableCommands = 
		new ArrayList(
			Arrays.asList(
				new String[] {
					EXITC,
					QUITC,
					HELPC,
					DESCC,
					LISTC,
					SETCONSETSC,
					SETOUTSETSC,
					SHOWOUTSETSC,
					SHOWCONSETSC,
					SHOWDATASETC,
					SETVERBOSEC,
					SHOWVERBOSEC,
					SETPROMPT,
					EXECC,
					RUNSCRIPTC,
					LOADSCRIPTC,
					SAVETOSCRIPTC,
					HISTORYC,
					USEC,
					MartShellLib.GETQSTART,
					MartShellLib.USINGQSTART }));

	// describe instructions
	private final String DATASETKEY = "DatasetView";
	private final String FILTERKEY = "Filter";
	private final String ATTRIBUTEKEY = "Attribute";

	// strings used to show/set output format settings
	private final String FILE = "file";
	private final String FORMAT = "format";
	private final String SEPARATOR = "separator";

	//environment dataset
	private String envDataset = null;

	// strings used to show/set mart connection settings
	private final String DBHOST = "host";
	private final String DBUSER = "user";
	private final String DBPASSWORD = "password";
	private final String DBPORT = "port";
	private final String DATABASENAME = "databaseName";
	private final String DATABASETYPE = "databaseType";
	private final String ALTCONFFILE = "alternateConfigurationFile";

	//startup message variables
	private final String STARTUP = "startUp";
	private final String HISTORYQ = "CommandHistoryHelp";
	private final String COMPLETIONQ = "CommandCompletionHelp";
	private final String ASC = "as";

	private final int MAXLINECOUNT = 60;
	// page prompt describe output line limit
	private final int MAXCHARCOUNT = 80; // line length limit

	private boolean continueQuery = false;
	private StringBuffer conline = new StringBuffer();

	//other strings needed
	private final String LINEEND = ";";
}
