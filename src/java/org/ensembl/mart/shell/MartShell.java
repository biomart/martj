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
import java.io.ByteArrayOutputStream;
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FormatException;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.SequenceException;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSAttributeGroup;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterCollection;
import org.ensembl.mart.lib.config.FilterGroup;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSet;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;
import org.ensembl.mart.lib.config.UIAttributeDescription;
import org.ensembl.mart.lib.config.UIDSFilterDescription;
import org.ensembl.mart.lib.config.UIFilterDescription;
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
			+ "\n-l LOGGING_FILE_URL                     - logging file, defaults to console if none specified"
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
	 * Initialise logging system to print to logging messages of level >= WARN
	 * to console. Does nothing if system property log4j.configuration is set.
	 */
	public static void defaultLoggingConfiguration(boolean verbose) {
		if (System.getProperty("log4j.configuration") == null) {

			BasicConfigurator.configure();
			if (verbose)
				Logger.getRoot().setLevel(Level.INFO);
			else
				Logger.getRoot().setLevel(Level.WARN);
		}
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

			mainHost = p.getProperty("host");
			mainPort = p.getProperty("port");
			mainDatabase = p.getProperty("databaseName");
			mainUser = p.getProperty("user");
			mainPassword = p.getProperty("password");
			mainDatabaseType = p.getProperty("databaseType");
			mainConfiguration = p.getProperty("alternateConfigurationFile");

		} catch (java.net.MalformedURLException e) {
			mainLogger.warn("Could not load connection file " + connfile + " MalformedURLException: " + e);
		} catch (java.io.IOException e) {
			mainLogger.warn("Could not load connection file " + connfile + " IOException: " + e);
		}
		confinUse = connfile;
	}

	private static String[] harvestArguments(String[] oargs) throws Exception {
		Hashtable argtable = new Hashtable();
		String key = null;

		for (int i = 0, n = oargs.length; i < n; i++) {
			String arg = oargs[i];

			if (arg.startsWith("-")) {
				key = arg;

				if (!argtable.containsKey(key))
					argtable.put(key, new StringBuffer());
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

		String[] ret = new String[argtable.size() * 2]; // one slot for each key, and one slot for each non null or non empty value
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

		// check for the defaultConf file, and use it, if present.  Some values may be overridden with a user specified file with -g
		if (new File(defaultConf).exists())
			getConnProperties(defaultConf);

		// Initialise logging system
		if (loggingURL != null) {
			PropertyConfigurator.configure(loggingURL);
		} else {
			defaultLoggingConfiguration(verbose);
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
					mainLogger.warn(
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
			LoadHelpFile();
			  
			//display startup information
			String startupMessage = Help(STARTUP);
			  
			if (historyOn)
			  startupMessage = startupMessage.replaceAll(HISTORYQ, Help(HISTORYQ));
      else
        startupMessage = startupMessage.replaceAll(HISTORYQ, "");
			  
      if (completionOn)
			  startupMessage = startupMessage.replaceAll(COMPLETIONQ, Help(COMPLETIONQ));
			else
        startupMessage = startupMessage.replaceAll(COMPLETIONQ, "");
          
			System.out.println(startupMessage);
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

				// add sequences
				mcl.AddAvailableCommandsTo(MartShellLib.QSEQUENCE, SequenceDescription.SEQS);

				// add describe
				HashMap describeCommands = new HashMap();

				//      set up FilterPage keys
				HashMap FilterMap = new HashMap();
				FilterMap.put(FILTERKEY, new HashMap());

				describeCommands.put(FILTERKEY, FilterMap);

				HashMap FpageMap = new HashMap();
				FpageMap.put(FILTERKEY, FilterMap);

				HashMap FgroupMap = new HashMap();
				FgroupMap.put(FILTERKEY, FilterMap);

				HashMap FColMap = new HashMap();
				FColMap.put(FILTERKEY, FilterMap);

				HashMap FDescMap = new HashMap();
				FDescMap.put(FILTERKEY, FilterMap);

				HashMap FsetMap = new HashMap();
				FsetMap.put(FILTERSETDESCRIPTIONKEY, new HashMap());

				FColMap.put(FILTERSETKEY, FsetMap);
				FColMap.put(FILTERKEY, FDescMap);

				FgroupMap.put(FILTERCOLLECTIONKEY, FColMap);

				FpageMap.put(FILTERGROUPKEY, FgroupMap);

				describeCommands.put(FILTERPAGEKEY, FpageMap);

				// set up AttributePage Keys
				HashMap AttMap = new HashMap();
				AttMap.put(ATTRIBUTEKEY, new HashMap());
				describeCommands.put(ATTRIBUTEKEY, AttMap);

				HashMap ApageMap = new HashMap();
				ApageMap.put(ATTRIBUTEKEY, AttMap);

				HashMap AgroupMap = new HashMap();
				AgroupMap.put(ATTRIBUTEKEY, AttMap);

				HashMap AColMap = new HashMap();
				AColMap.put(ATTRIBUTEKEY, AttMap);

				HashMap AdescMap = new HashMap();
				AdescMap.put(ATTRIBUTEKEY, AttMap);

				AColMap.put(ATTRIBUTEKEY, AdescMap);
				AgroupMap.put(ATTRIBUTECOLLECTIONKEY, AColMap);
				ApageMap.put(ATTRIBUTEGROUPKEY, AgroupMap);

				describeCommands.put(FILTERPAGEKEY, FpageMap);
				describeCommands.put(ATTRIBUTEPAGEKEY, ApageMap);

				mcl.AddAvailableCommandsTo(DESCC, describeCommands);

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
		} else if (martUser.length() < 1) {
			validQuery = false;
			setBatchError("Must set a User");
		} else if (martPass.length() < 1) {
			validQuery = false;
			setBatchError("Must set a Password");
		} else if (martDatabase.length() < 5) {
			validQuery = false;
			setBatchError("Must set a Mart Database");
		} else {
			try {
				Initialize();
				if (!querystring.endsWith(LINEEND))
					querystring = querystring + LINEEND;

				parseCommand(querystring);
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
		else if (martUser != null && martDatabase != null)
			prompt = martUser + "@" + martHost + " : " + martDatabase + "> ";
		else
			prompt = "> ";

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
			LoadHelpFile();

		StringBuffer buf = new StringBuffer();

		if (command.equals(HELPC)) {
			buf.append("\nAvailable items:\n");

			for (Iterator iter = commandHelp.keySet().iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				buf.append("\t\t" + element).append("\n");
			}
			buf.append("\n");
			return buf.toString();

		} else if (command.startsWith(HELPC)) {
			buf.append("\n");
			StringTokenizer hToks = new StringTokenizer(command, " ");
			hToks.nextToken(); // skip help
			String comm = hToks.nextToken();

			return Help(comm);
		} else {
			if (commandHelp.containsKey(command)) {
				// if this is a help call, make sure and check for domain specific bits in the help input to substitute
				String output = commandHelp.getProperty(command);
				for (int i = 0, n = dsCommands.length; i < n; i++) {
					Pattern pat = dsCommands[i];
					Matcher m = pat.matcher(output);

					if (m.matches())
						buf.append(m.group(1)).append(Help(m.group(2))).append(m.group(3)).append("\n");
					else
						buf.append(output).append("\n");
				}
			} else
				buf.append("Sorry, no information available for item: ").append(command).append("\n");
			return buf.toString();
		}
	}

	private void LoadHelpFile() throws InvalidQueryException {
		URL help = ClassLoader.getSystemResource(HELPFILE);
		URL dshelp = ClassLoader.getSystemResource(DSHELPFILE);
		try {
			commandHelp.load(help.openStream());
			commandHelp.load(dshelp.openStream());
      
      if (! historyOn)
        commandHelp.remove(HISTORYQ);
      
      if (! completionOn)
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

	private void DescribeRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		int tokCount = toks.countTokens();
		toks.nextToken(); // skip describe

		System.out.println();

		if (tokCount == 1) {
			String output = "This mart contains the following datasets:\n";
			Dataset[] dsets = martconf.getDatasets();
			for (int i = 0, n = dsets.length; i < n; i++) {
				Dataset dset = dsets[i];
				output += "\t" + dset.getInternalName() + "   (" + dset.getDisplayName() + ")\n";
			}
			System.out.println(output);
		} else if (tokCount == 2) {
			String dsetName = toks.nextToken();

			if (!martconf.containsDataset(dsetName))
				throw new InvalidQueryException("Dataset " + dsetName + " Not found in mart configuration for " + martconf.getInternalName() + "\n");

			DescribeDataset(martconf.getDatasetByName(dsetName));
		} else if (tokCount > 2) {

			int mod = tokCount % 2; // must be even number of elements
			if (mod > 0)
				throw new InvalidQueryException("Recieved invalid describe command: " + command + "\n");

			List args = new ArrayList();

			String dsetName = toks.nextToken();
			if (!martconf.containsDataset(dsetName))
				throw new InvalidQueryException("Dataset " + dsetName + " Not found in mart configuration for " + martconf.getInternalName() + "\n");

			while (toks.hasMoreTokens()) {
				args.add(new String[] { toks.nextToken(), toks.nextToken()});
			}

			DescribeDataset(martconf.getDatasetByName(dsetName), args);
		}
	}

	private void DescribeDataset(Dataset dset) throws InvalidQueryException {
		System.out.print("Dataset " + dset.getInternalName() + " - " + dset.getDisplayName() + "\n");

		String[] dlines = ColumnIze(dset.getDescription());
		for (int i = 0, n = dlines.length; i < n; i++)
			System.out.println(dlines[i]);

		System.out.println("\nFilterPages:");
		System.out.println(DASHES);

		FilterPage[] fpages = dset.getFilterPages();
		for (int i = 0, n = fpages.length; i < n; i++) {
			if (i > 0)
				System.out.println(DASHES);

			FilterPage page = fpages[i];

			String filtPageDesc = page.getInternalName() + " - " + page.getDisplayName();
			if (!page.getDescription().equals(""))
				filtPageDesc += ": " + page.getDescription();

			String[] lines = ColumnIze(filtPageDesc);
			for (int j = 0, k = lines.length; j < k; j++)
				System.out.print("\t" + lines[j] + "\n");
		}

		System.out.println(DASHES);
		System.out.println(DASHES);
		System.out.println();

		System.out.print("AttributePages:\n");
		System.out.println(DASHES);

		AttributePage[] apages = dset.getAttributePages();
		for (int i = 0, n = apages.length; i < n; i++) {
			if (i > 0)
				System.out.println(DASHES);

			AttributePage page = apages[i];

			String atPageDesc = page.getInternalName() + " - " + page.getDisplayName();
			if (!page.getDescription().equals(""))
				atPageDesc += ": " + page.getDescription();

			String[] alines = ColumnIze(atPageDesc);
			for (int j = 0, k = alines.length; j < k; j++)
				System.out.print("\t" + alines[j] + "\n");
		}
		System.out.println(DASHES);
	}

	private void DescribeDataset(Dataset dset, List args) throws InvalidQueryException {
		int argCount = args.size();
		String[] arg1 = null;
		String arg1key = null;
		String arg1value = null;

		switch (argCount) {
			case 1 :
				arg1 = (String[]) args.get(0);
				arg1key = arg1[0];
				arg1value = arg1[1];

				if (arg1key.equals(FILTERPAGEKEY)) {
					if (!dset.containsFilterPage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain FilterPage " + arg1value + "\n");

					System.out.print("Dataset " + dset.getInternalName() + " - " + dset.getDisplayName() + "\n\nFilterPage: ");

					DescribeFilterPage(dset.getFilterPageByName(arg1value));
				} else if (arg1key.equals(FILTERKEY)) {
					if (!dset.containsUIFilterDescription(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain Filter " + arg1value + "\n");

					System.out.print("Dataset " + dset.getInternalName() + " - " + dset.getDisplayName() + "\n\n");

					String[] lines = DescribeFilter(dset.getUIFilterDescriptionByName(arg1value));
					for (int i = 0, n = lines.length; i < n; i++)
						System.out.println(lines[i]);

					System.out.println();
				} else if (arg1key.equals(ATTRIBUTEPAGEKEY)) {
					if (!dset.containsAttributePage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain AttributePage " + arg1value + "\n");

					System.out.print("Dataset " + dset.getInternalName() + " - " + dset.getDisplayName() + "\n\nAttributePage: ");
					DescribeAttributePage(dset.getAttributePageByName(arg1value));

				} else if (arg1key.equals(ATTRIBUTEKEY)) {
					if (!dset.containsUIAttributeDescription(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain Attribute " + arg1value + "\n");

					System.out.print("Dataset " + dset.getInternalName() + " - " + dset.getDisplayName() + "\n\n");
					String[] lines = DescribeAttribute(dset.getUIAttributeDescriptionByName(arg1value));
					for (int i = 0, n = lines.length; i < n; i++)
						System.out.println(lines[i]);

					System.out.println();
				} else {
					throw new InvalidQueryException("Recieved describe command with invalid request key: " + arg1key + "\n");
				}
				break;

			case 2 :
				arg1 = (String[]) args.get(0);
				arg1key = arg1[0];
				arg1value = arg1[1];

				String[] arg2 = null;
				if (arg1key.equals(FILTERPAGEKEY)) {
					if (!dset.containsFilterPage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain FilterPage " + arg1value + "\n");

					arg2 = (String[]) args.get(1);
					FilterPage fpage = dset.getFilterPageByName(arg1value);
					String arg2key = arg2[0];
					String arg2value = arg2[1];

					if (arg2key.equals(FILTERGROUPKEY)) {
						if (!fpage.containsFilterGroup(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " FilterPage " + fpage.getInternalName() + " does not contain FilterGroup " + arg2value + "\n");

						System.out.print(
							"Dataset "
								+ dset.getInternalName()
								+ " - "
								+ dset.getDisplayName()
								+ "\nFilterPage: "
								+ fpage.getInternalName()
								+ " - "
								+ fpage.getDisplayName()
								+ "\n\n");

						String[] lines = DescribeFilterGroup(fpage.getFilterGroupByName(arg2value));
						for (int i = 0, n = lines.length; i < n; i++)
							System.out.println("\t\t" + lines[i]);

						System.out.println();

					} else if (arg2key.equals(FILTERKEY)) {
						if (!fpage.containsUIFilterDescription(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " FilterPage " + fpage.getInternalName() + " does not contain Filter " + arg2value + "\n");

						System.out.print(
							"Dataset "
								+ dset.getInternalName()
								+ " - "
								+ dset.getDisplayName()
								+ "\nFilterPage: "
								+ fpage.getInternalName()
								+ " - "
								+ fpage.getDisplayName()
								+ "\n");

						System.out.println();
						String[] lines = DescribeFilter(fpage.getUIFilterDescriptionByName(arg2value));
						for (int i = 0, n = lines.length; i < n; i++)
							System.out.println("\t\t" + lines[i]);

						System.out.println();

					} else {
						throw new InvalidQueryException("Recieved describe command with request key: " + arg1key + " and invalid second request: " + arg2key + "\n");
					}

				} else if (arg1key.equals(ATTRIBUTEPAGEKEY)) {
					if (!dset.containsAttributePage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain AttributePage " + arg1value + "\n");

					arg2 = (String[]) args.get(1);
					AttributePage apage = dset.getAttributePageByName(arg1value);
					String arg2key = arg2[0];
					String arg2value = arg2[1];

					if (arg2key.equals(ATTRIBUTEGROUPKEY)) {
						if (!apage.containsAttributeGroup(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " AttributePage: " + apage.getInternalName() + " does not contain AttributeGroup " + arg2value + "\n");

						System.out.print(
							"Dataset "
								+ dset.getInternalName()
								+ " - "
								+ dset.getDisplayName()
								+ "\nAttributePage: "
								+ apage.getInternalName()
								+ " - "
								+ apage.getDisplayName()
								+ "\n\n");

						String[] lines = DescribeAttributeGroup(apage.getAttributeGroupByName(arg2value));
						for (int i = 0, n = lines.length; i < n; i++)
							System.out.println("\t\t" + lines[i]);

					} else if (arg2key.equals(ATTRIBUTEKEY)) {
						if (!apage.containsUIAttributeDescription(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " AttributePage: " + apage.getInternalName() + " does not contain Attribute " + arg2value + "\n");

						System.out.print(
							"Dataset "
								+ dset.getInternalName()
								+ " - "
								+ dset.getDisplayName()
								+ "\nAttributePage: "
								+ apage.getInternalName()
								+ " - "
								+ apage.getDisplayName()
								+ "\n");

						System.out.println();
						String[] lines = DescribeAttribute(apage.getUIAttributeDescriptionByName(arg2value));
						for (int i = 0, n = lines.length; i < n; i++)
							System.out.println("\t\t" + lines[i]);

						System.out.println();

					} else {
						throw new InvalidQueryException("Recieved describe command with request key: " + arg1key + " and invalid second request: " + arg2key + "\n");
					}

				} else {
					throw new InvalidQueryException("Recieved describe command with invalid request key: " + arg1key + "\n");
				}
				break;

			case 3 :
				arg1 = (String[]) args.get(0);
				arg1key = arg1[0];
				arg1value = arg1[1];

				arg2 = null;
				String[] arg3 = null;
				if (arg1key.equals(FILTERPAGEKEY)) {
					if (!dset.containsFilterPage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain FilterPage " + arg1value + "\n");

					arg2 = (String[]) args.get(1);
					FilterPage fpage = dset.getFilterPageByName(arg1value);
					String arg2key = arg2[0];
					String arg2value = arg2[1];

					if (arg2key.equals(FILTERGROUPKEY)) {
						if (!fpage.containsFilterGroup(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " FilterPage " + fpage.getInternalName() + " does not contain FilterGroup " + arg2value + "\n");

						if (fpage.getFilterGroupByName(arg2value) instanceof FilterGroup) {
							FilterGroup group = (FilterGroup) fpage.getFilterGroupByName(arg2value);

							arg3 = (String[]) args.get(2);
							String arg3key = arg3[0];
							String arg3value = arg3[1];

							if (arg3key.equals(FILTERCOLLECTIONKEY)) {
								if (!group.containsFilterCollection(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " FilterPage "
											+ fpage.getInternalName()
											+ " FilterGroup "
											+ group.getInternalName()
											+ " does not contain FilterCollection "
											+ arg3value
											+ "\n");

								System.out.print(
									"Dataset "
										+ dset.getInternalName()
										+ " - "
										+ dset.getDisplayName()
										+ "\nFilterPage: "
										+ fpage.getInternalName()
										+ " - "
										+ fpage.getDisplayName()
										+ "\n\tFilterGroup: "
										+ group.getInternalName()
										+ " - "
										+ group.getDisplayName()
										+ "\n\n");

								String[] lines = DescribeFilterCollection(group.getFilterCollectionByName(arg3value));
								for (int i = 0, n = lines.length; i < n; i++)
									System.out.println("\t\t" + lines[i]);

								System.out.println();

							} else if (arg3key.equals(FILTERSETKEY)) {
								if (!group.containsFilterSet(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " FilterPage "
											+ fpage.getInternalName()
											+ " FilterGroup "
											+ group.getInternalName()
											+ " does not contain FilterSet "
											+ arg3value
											+ "\n");

								System.out.print(
									"Dataset "
										+ dset.getInternalName()
										+ " - "
										+ dset.getDisplayName()
										+ "\nFilterPage: "
										+ fpage.getInternalName()
										+ " - "
										+ fpage.getDisplayName()
										+ "\n\tFilterGroup: "
										+ group.getInternalName()
										+ " - "
										+ group.getDisplayName()
										+ "\n\n");

								String[] lines = DescribeFilterSet(group.getFilterSetByName(arg3value));
								for (int i = 0, n = lines.length; i < n; i++)
									System.out.println("\t\t" + lines[i]);

								System.out.println();

							} else if (arg3key.equals(FILTERKEY)) {
								if (!group.containsUIFilterDescription(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " FilterPage "
											+ fpage.getInternalName()
											+ " FilterGroup "
											+ group.getInternalName()
											+ " does not contain Filter "
											+ arg3value
											+ "\n");

								System.out.print(
									"Dataset "
										+ dset.getInternalName()
										+ " - "
										+ dset.getDisplayName()
										+ "\nFilterPage: "
										+ fpage.getInternalName()
										+ " - "
										+ fpage.getDisplayName()
										+ "\n\tFilterGroup: "
										+ group.getInternalName()
										+ " - "
										+ group.getDisplayName()
										+ "\n\n");

								String[] lines = DescribeFilter(group.getUIFilterDescriptionByName(arg3value));
								for (int i = 0, n = lines.length; i < n; i++)
									System.out.println("\t\t" + lines[i]);

								System.out.println();

							} else {
								throw new InvalidQueryException(
									"Recieved describe command with request key: "
										+ arg1key
										+ " and second request key: "
										+ arg2key
										+ " with invalid third request key: "
										+ arg3key
										+ "\n");
							}

						} //else
						// DSFilterGroup code goes here, if needed

					} else {
						throw new InvalidQueryException("Recieved describe command with request key: " + arg1key + " and invalid second request key: " + arg2key + "\n");
					}

				} else if (arg1key.equals(ATTRIBUTEPAGEKEY)) {
					if (!dset.containsAttributePage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain AttributePage " + arg1value + "\n");

					arg2 = (String[]) args.get(1);
					AttributePage apage = dset.getAttributePageByName(arg1value);
					String arg2key = arg2[0];
					String arg2value = arg2[1];

					if (arg2key.equals(ATTRIBUTEGROUPKEY)) {
						if (!apage.containsAttributeGroup(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " AttributePage " + apage.getInternalName() + " does not contain AttributeGroup " + arg2value + "\n");

						if (apage.getAttributeGroupByName(arg2value) instanceof AttributeGroup) {
							AttributeGroup group = (AttributeGroup) apage.getAttributeGroupByName(arg2value);
							arg3 = (String[]) args.get(2);
							String arg3key = arg3[0];
							String arg3value = arg3[1];

							if (arg3key.equals(ATTRIBUTECOLLECTIONKEY)) {
								if (!group.containsAttributeCollection(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " AttributePage "
											+ apage.getInternalName()
											+ " AttributeGroup "
											+ group.getInternalName()
											+ " does not contain AttributeCollection "
											+ arg3value
											+ "\n");

								System.out.print(
									"Dataset "
										+ dset.getInternalName()
										+ " - "
										+ dset.getDisplayName()
										+ "\nAttributePage: "
										+ apage.getInternalName()
										+ " - "
										+ apage.getDisplayName()
										+ "\n\tAttributeGroup: "
										+ group.getInternalName()
										+ " - "
										+ group.getDisplayName()
										+ "\n\n");

								String[] lines = DescribeAttributeCollection(group.getAttributeCollectionByName(arg3value));
								for (int i = 0, n = lines.length; i < n; i++)
									System.out.println("\t\t" + lines[i]);

								System.out.println();

							} else if (arg3key.equals(ATTRIBUTEKEY)) {
								if (!group.containsUIAttributeDescription(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " AttributePage "
											+ apage.getInternalName()
											+ " AttributeGroup "
											+ group.getInternalName()
											+ " does not contain Attribute "
											+ arg2value
											+ "\n");

								System.out.print(
									"Dataset "
										+ dset.getInternalName()
										+ " - "
										+ dset.getDisplayName()
										+ "\nAttributePage: "
										+ apage.getInternalName()
										+ " - "
										+ apage.getDisplayName()
										+ "\n\tAttributeGroup: "
										+ group.getInternalName()
										+ " - "
										+ group.getDisplayName()
										+ "\n\n");

								String[] lines = DescribeAttribute(group.getUIAttributeDescriptionByName(arg3value));
								for (int i = 0, n = lines.length; i < n; i++)
									System.out.println("\t\t" + lines[i]);

								System.out.println();
							} else {
								throw new InvalidQueryException(
									"Recieved describe command with request key: "
										+ arg1key
										+ " and second request key: "
										+ arg2key
										+ " with invalid third request key: "
										+ arg3key
										+ "\n");
							}
						} //else
						// describe individual sequences or other DSAttributeGroup things?

					} else {
						throw new InvalidQueryException("Recieved describe command with request key: " + arg1key + " and invalid second request: " + arg2key + "\n");
					}

				} else {
					throw new InvalidQueryException("Recieved describe command with invalid request key: " + arg1key + "\n");
				}
				break;

			case 4 :
				arg1 = (String[]) args.get(0);
				arg1key = arg1[0];
				arg1value = arg1[1];

				arg2 = null;
				arg3 = null;
				String[] arg4 = null;
				if (arg1key.equals(FILTERPAGEKEY)) {
					if (!dset.containsFilterPage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain FilterPage " + arg1value + "\n");

					arg2 = (String[]) args.get(1);
					FilterPage fpage = dset.getFilterPageByName(arg1value);
					String arg2key = arg2[0];
					String arg2value = arg2[1];

					if (arg2key.equals(FILTERGROUPKEY)) {
						if (!fpage.containsFilterGroup(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " FilterPage " + fpage.getInternalName() + " does not contain FilterGroup " + arg2value + "\n");

						if (fpage.getFilterGroupByName(arg2value) instanceof FilterGroup) {
							FilterGroup group = (FilterGroup) fpage.getFilterGroupByName(arg2value);

							arg3 = (String[]) args.get(2);
							String arg3key = arg3[0];
							String arg3value = arg3[1];

							if (arg3key.equals(FILTERCOLLECTIONKEY)) {
								if (!group.containsFilterCollection(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " FilterPage "
											+ fpage.getInternalName()
											+ " FilterGroup "
											+ group.getInternalName()
											+ " does not contain FilterCollection "
											+ arg3value
											+ "\n");

								arg4 = (String[]) args.get(3);
								FilterCollection collection = group.getFilterCollectionByName(arg3value);
								String arg4key = arg4[0];
								String arg4value = arg4[1];

								if (arg4key.equals(FILTERKEY)) {
									if (!collection.containsUIFilterDescription(arg4value))
										throw new InvalidQueryException(
											"Dataset "
												+ dset.getInternalName()
												+ " FilterPage "
												+ fpage.getInternalName()
												+ " FilterGroup "
												+ group.getInternalName()
												+ " FilterCollection "
												+ collection.getInternalName()
												+ " does not contain Filter "
												+ arg4value
												+ "\n");

									System.out.print(
										"Dataset "
											+ dset.getInternalName()
											+ " - "
											+ dset.getDisplayName()
											+ "\nFilterPage: "
											+ fpage.getInternalName()
											+ " - "
											+ fpage.getDisplayName()
											+ "\n\tFilterGroup: "
											+ group.getInternalName()
											+ " - "
											+ group.getDisplayName()
											+ "\n\t\tFilterCollection: "
											+ collection.getInternalName()
											+ " - "
											+ collection.getDisplayName());

									if (collection.inFilterSet())
										System.out.print("\n\t\tMust be qualified with a FilterSetDescription from FilterSet: " + collection.getFilterSetName());

									System.out.print("\n\n");

									String[] lines = DescribeFilter(collection.getUIFilterDescriptionByName(arg4value));
									for (int i = 0, n = lines.length; i < n; i++)
										System.out.println("\t\t\t" + lines[i]);

									System.out.println();

								} else {
									throw new InvalidQueryException(
										"Recieved describe command with request key: "
											+ arg1key
											+ " and second request key: "
											+ arg2key
											+ " with third request key: "
											+ arg3key
											+ " and invalid fourth request key: "
											+ arg4key
											+ "\n");
								}

							} else if (arg3key.equals(FILTERSETKEY)) {
								if (!group.containsFilterSet(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " FilterPage "
											+ fpage.getInternalName()
											+ " FilterGroup "
											+ group.getInternalName()
											+ " does not contain FilterSet "
											+ arg3value
											+ "\n");

								arg4 = (String[]) args.get(3);
								FilterSet fset = group.getFilterSetByName(arg3value);
								String arg4key = arg4[0];
								String arg4value = arg4[1];

								if (arg4key.equals(FILTERSETDESCRIPTIONKEY)) {
									if (!fset.containsFilterSetDescription(arg4value))
										throw new InvalidQueryException(
											"Dataset "
												+ dset.getInternalName()
												+ " FilterPage "
												+ fpage.getInternalName()
												+ " FilterGroup "
												+ group.getInternalName()
												+ " FilterSet "
												+ fset.getInternalName()
												+ " does not contain FilterSetDescription "
												+ arg4value
												+ "\n");

									System.out.print(
										"Dataset "
											+ dset.getInternalName()
											+ " - "
											+ dset.getDisplayName()
											+ "\nFilterPage: "
											+ fpage.getInternalName()
											+ " - "
											+ fpage.getDisplayName()
											+ "\n\tFilterGroup: "
											+ group.getInternalName()
											+ " - "
											+ group.getDisplayName()
											+ "\n\t\tFilterSet: "
											+ fset.getInternalName()
											+ " - "
											+ fset.getDisplayName()
											+ "\n\n");

									String[] lines = DescribeFilterSetDescription(fset.getFilterSetDescriptionByName(arg4value));
									for (int i = 0, n = lines.length; i < n; i++)
										System.out.println("\t\t\t" + lines[i]);

									System.out.println();

								} else {
									throw new InvalidQueryException(
										"Recieved describe command with request key: "
											+ arg1key
											+ " and second request key: "
											+ arg2key
											+ " with third request key: "
											+ arg3key
											+ " and invalid fourth request key: "
											+ arg4key
											+ "\n");
								}

							} else {
								throw new InvalidQueryException(
									"Recieved describe command with request key: "
										+ arg1key
										+ " and second request key: "
										+ arg2key
										+ " with invalid third request key: "
										+ arg3key
										+ "\n");
							}
						} //else
						// DSFilterGroup code goes here, if needed

					} else {
						throw new InvalidQueryException("Recieved describe command with request key: " + arg1key + " and invalid second request: " + arg2key + "\n");
					}

				} else if (arg1key.equals(ATTRIBUTEPAGEKEY)) {
					if (!dset.containsAttributePage(arg1value))
						throw new InvalidQueryException("Dataset " + dset.getInternalName() + " does not contain AttributePage " + arg1value + "\n");

					arg2 = (String[]) args.get(1);
					AttributePage apage = dset.getAttributePageByName(arg1value);
					String arg2key = arg2[0];
					String arg2value = arg2[1];

					if (arg2key.equals(ATTRIBUTEGROUPKEY)) {
						if (!apage.containsAttributeGroup(arg2value))
							throw new InvalidQueryException(
								"Dataset " + dset.getInternalName() + " AttributePage " + apage.getInternalName() + " does not contain AttributeGroup " + arg2value + "\n");

						if (apage.getAttributeGroupByName(arg2value) instanceof AttributeGroup) {
							AttributeGroup group = (AttributeGroup) apage.getAttributeGroupByName(arg2value);
							arg3 = (String[]) args.get(2);
							String arg3key = arg3[0];
							String arg3value = arg3[1];

							if (arg3key.equals(ATTRIBUTECOLLECTIONKEY)) {
								if (!group.containsAttributeCollection(arg3value))
									throw new InvalidQueryException(
										"Dataset "
											+ dset.getInternalName()
											+ " AttributePage "
											+ apage.getInternalName()
											+ " AttributeGroup "
											+ group.getInternalName()
											+ " does not contain AttributeCollection "
											+ arg2value
											+ "\n");

								arg4 = (String[]) args.get(3);
								AttributeCollection collection = group.getAttributeCollectionByName(arg3value);
								String arg4key = arg4[0];
								String arg4value = arg4[1];

								if (arg4key.equals(ATTRIBUTEKEY)) {
									if (!collection.containsUIAttributeDescription(arg4value))
										throw new InvalidQueryException(
											"Dataset "
												+ dset.getInternalName()
												+ " AttributePage "
												+ apage.getInternalName()
												+ " AttributeGroup "
												+ group.getInternalName()
												+ " AttributeCollection "
												+ collection.getInternalName()
												+ " does not contain Attribute "
												+ arg4value
												+ "\n");

									System.out.print(
										"Dataset "
											+ dset.getInternalName()
											+ " - "
											+ dset.getDisplayName()
											+ "\nAttributePage: "
											+ apage.getInternalName()
											+ " - "
											+ apage.getDisplayName()
											+ "\n\tAttributeGroup: "
											+ group.getInternalName()
											+ " - "
											+ group.getDisplayName()
											+ "\n\t\tAttributeCollection: "
											+ collection.getInternalName()
											+ " - "
											+ collection.getDisplayName()
											+ "\n\n");

									String[] lines = DescribeAttribute(collection.getUIAttributeDescriptionByName(arg4value));
									for (int i = 0, n = lines.length; i < n; i++)
										System.out.println("\t\t\t\t" + lines[i]);

									System.out.println();

								} else {
									throw new InvalidQueryException(
										"Recieved describe command with request key: "
											+ arg1key
											+ " and second request key: "
											+ arg2key
											+ " with third request key: "
											+ arg3key
											+ " and invalid fourth request key: "
											+ arg4key
											+ "\n");
								}

							} else {
								throw new InvalidQueryException(
									"Recieved describe command with request key: "
										+ arg1key
										+ " and second request key: "
										+ arg2key
										+ " with invalid third request key: "
										+ arg3key
										+ "\n");
							}
						} //else
						//DSAttributeGroup Code goes here

					} else {
						throw new InvalidQueryException("Recieved describe command with request key: " + arg1key + " and invalid second request: " + arg2key + "\n");
					}

				} else {
					throw new InvalidQueryException("Recieved describe command with invalid request key: " + arg1key + "\n");
				}
				break;

			default :
				throw new InvalidQueryException("Recieved invalid describe command: wrong number of arguments after dateset\n");
		}
	}

	private void DescribeFilterPage(FilterPage page) throws InvalidQueryException {
		System.out.print(page.getInternalName() + " contains the following FilterGroups:\n\n");

		List groups = page.getFilterGroups();
		for (int i = 0, n = groups.size(); i < n; i++) {
			if (i > 0) {
				try {
					String quit = Readline.readline("\n\nHit Enter to continue with next group, q to return to prompt: ", false);
					if (quit.equals("q"))
						break;

				} catch (Exception e) {
					// do nothing
				}
				System.out.println();
			}

			Object group = groups.get(i);
			String[] lines = DescribeFilterGroup(group);
			for (int j = 0, n2 = lines.length; j < n2; j++) {
				String string = lines[j];
				System.out.print("\t" + string + "\n");
			}
		}
	}

	private void DescribeAttributePage(AttributePage page) throws InvalidQueryException {
		System.out.print(page.getInternalName() + " contains the following AttributeGroups:\n\n");

		List groups = page.getAttributeGroups();
		for (int i = 0, n = groups.size(); i < n; i++) {
			if (i > 0) {
				try {
					String quit = Readline.readline("\n\nHit Enter to continue with next group, q to return to prompt: ", false);
					if (quit.equals("q"))
						break;

				} catch (Exception e) {
					// do nothing
				}
				System.out.println();
			}

			Object groupo = groups.get(i);
			String[] lines = DescribeAttributeGroup(groupo);
			for (int j = 0, n2 = lines.length; j < n2; j++) {
				String string = lines[j];
				System.out.print("\t" + string + "\n");
			}
		}
	}

	private String[] DescribeFilterGroup(Object groupo) throws InvalidQueryException {
		List lines = new ArrayList();

		if (groupo instanceof FilterGroup) {
			FilterGroup group = (FilterGroup) groupo;
			lines.add("Group: " + group.getInternalName() + " - " + group.getDisplayName());
			lines.add(DASHES);

			if (group.hasFilterSets()) {
				FilterSet[] fsets = group.getFilterSets();
				for (int i = 0, n = fsets.length; i < n; i++) {
					if (i > 0)
						lines.add("");

					lines.addAll(Arrays.asList(DescribeFilterSet(fsets[i])));
				}
				lines.add("");
				lines.add(DASHES);
			}

			FilterCollection[] fcs = group.getFilterCollections();

			for (int i = 0, n = fcs.length; i < n; i++) {
				if (i > 0)
					lines.add(DASHES);

				lines.addAll(Arrays.asList(DescribeFilterCollection(fcs[i])));
			}
			lines.add(DASHES);
		} else {
			//DSFilterGroup group = (DSFilterGroup) groupo;
			//do nothing, but add hooks for DSFilterGroups here
		}
		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String[] DescribeAttributeGroup(Object groupo) {
		List lines = new ArrayList();

		if (groupo instanceof DSAttributeGroup) {
			DSAttributeGroup group = (DSAttributeGroup) groupo;

			if (group.getInternalName().equals("sequences")) {
				lines.add("\t" + group.getInternalName() + " - " + group.getDisplayName());
				lines.add("");

				for (int i = 0, n = SequenceDescription.SEQS.size(); i < n; i++) {
					String seqname = (String) SequenceDescription.SEQS.get(i);
					String seqdesc = (String) SequenceDescription.SEQDESCRIPTIONS.get(i);
					lines.add("\t\t" + seqname + " - " + seqdesc);
				}
			}
			// add new DSAttributeGroup hooks here, with else if
			else {
				//do nothing
			}
		} else {
			AttributeGroup group = (AttributeGroup) groupo;
			lines.add("Group: " + group.getInternalName() + " - " + group.getDisplayName());
			lines.add(DASHES);

			AttributeCollection[] attcs = group.getAttributeCollections();
			for (int i = 0, n = attcs.length; i < n; i++) {
				if (i > 0)
					lines.add(DASHES);

				lines.addAll(Arrays.asList(DescribeAttributeCollection(attcs[i])));
			}
			lines.add(DASHES);
		}

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String[] DescribeFilterSet(FilterSet fset) {
		List lines = new ArrayList();

		String setheader = "\tFilterSet: " + fset.getInternalName();
		if (!fset.getDisplayName().equals(""))
			setheader += " - " + fset.getDisplayName();

		setheader += " Contains the following FilterSetDescriptions:\n";
		lines.add(setheader);

		FilterSetDescription[] fsetdescs = fset.getFilterSetDescriptions();
		for (int i = 0, n = fsetdescs.length; i < n; i++)
			lines.addAll(Arrays.asList(DescribeFilterSetDescription(fsetdescs[i])));

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String[] DescribeFilterCollection(FilterCollection collection) {
		List lines = new ArrayList();

		String colheader = "\tCollection: " + collection.getInternalName();
		if (!collection.getDisplayName().equals(""))
			colheader += " - " + collection.getDisplayName();

		lines.add(colheader);

		if (collection.inFilterSet()) {
			String[] clines =
				ColumnIze(
					"(Note: Filters from this collection must be qualified with the internalName of one of the FilterSetsDescriptions from FilterSet '"
						+ collection.getFilterSetName()
						+ "' above)");
			for (int j = 0, k = clines.length; j < k; j++)
				lines.add("\t" + clines[j]);

			lines.add("");
		}

		List fdescs = collection.getUIFilterDescriptions();
		for (int j = 0, n2 = fdescs.size(); j < n2; j++)
			lines.addAll(Arrays.asList(DescribeFilter(fdescs.get(j))));

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String[] DescribeAttributeCollection(AttributeCollection collection) {
		List lines = new ArrayList();

		String colheader = "\tCollection: " + collection.getInternalName();
		if (!collection.getDisplayName().equals(""))
			colheader += " - " + collection.getDisplayName();

		lines.add(colheader);

		if (collection.getMaxSelect() > 0) {
			String[] clines = ColumnIze("(Note: Only " + collection.getMaxSelect() + " of the following attributes can be selected in the same query)");
			for (int j = 0, k = clines.length; j < k; j++)
				lines.add("\t" + clines[j]);
			lines.add("");
		}

		List adescs = collection.getUIAttributeDescriptions();
		for (int i = 0, n = adescs.size(); i < n; i++)
			lines.addAll(Arrays.asList(DescribeAttribute(adescs.get(i))));

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String[] DescribeFilterSetDescription(FilterSetDescription desc) {
		List lines = new ArrayList();

		String disp = "\t\t" + desc.getInternalName();
		if (desc.getDisplayName().length() > 0)
			disp += " - " + desc.getDisplayName();

		lines.add(disp);
		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String[] DescribeFilter(Object filtero) {
		List lines = new ArrayList();

		if (filtero instanceof UIFilterDescription) {
			UIFilterDescription desc = (UIFilterDescription) filtero;
			lines.add("\t\t" + desc.getInternalName() + " - " + desc.getDisplayName() + " (Type " + desc.getType() + ")");
		} else {
			UIDSFilterDescription desc = (UIDSFilterDescription) filtero;
			String disp = "\t\t" + desc.getInternalName();
			if (desc.getDisplayName().length() > 0)
				disp += " - " + desc.getDisplayName();
			disp += " (see 'help " + desc.getHandler() + "' for further information)";
			lines.add(disp);
		}

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	private String[] DescribeAttribute(Object attributeo) {
		List lines = new ArrayList();

		if (attributeo instanceof UIAttributeDescription) {
			UIAttributeDescription desc = (UIAttributeDescription) attributeo;
			lines.add("\t\t" + desc.getInternalName() + " - " + desc.getDisplayName());
		} else {
			// for now, do nothing.  If we add UIDSAttributeDescriptions to the config, add hooks here
		}

		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
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
					throw new InvalidQueryException("Recieved invalid setConnectionSettings command.\n" + Help(SETCONSETSC) + "\n");

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
					throw new InvalidQueryException("Recieved invalid setConnectionSettings command.\n" + Help(SETCONSETSC) + "\n");
			}
		} else {
			if (mainBatchMode)
				throw new InvalidQueryException("Recieved invalid setConnectionSettings command.\n" + Help(SETCONSETSC) + "\n");

			String thisLine = null;

			try {

				String myHost = (martHost == null) ? "" : martHost;
				thisLine = Readline.readline("\nPlease enter the host address of the mart database (press enter to leave as '" + myHost + "'): ", false);
				if (thisLine != null)
					martHost = thisLine;

				String myDBType = (martDatabaseType == null) ? "" : martDatabaseType;
				thisLine = Readline.readline("\nPlease enter the type of RDBMS hosting the mart database (press enter to leave as '" + myDBType + "'): ", false);
				if (thisLine != null)
					martDatabaseType = thisLine;

				String myPort = (martPort == null) ? "" : martPort;
				thisLine = Readline.readline("\nPlease enter the port on which the mart database is running (press enter to leave as '" + myPort + "'): ", false);
				if (thisLine != null)
					martPort = thisLine;

				String myUser = (martUser == null) ? "" : martUser;
				thisLine = Readline.readline("\nPlease enter the user name used to connect to the mart database (press enter to leave as '" + myUser + "'): ", false);
				if (thisLine != null)
					martUser = thisLine;

				String myPass = "";
				if (martPass != null) {
					for (int i = 0, n = martPass.length(); i < n; i++)
						myPass += "*";
				}

				thisLine = Readline.readline("\nPlease enter the password used to connect to the mart database (press enter to leave as '" + myPass + "'): ", false);
				if (thisLine != null)
					martPass = thisLine;

				String myDb = (martDatabase == null) ? "" : martDatabase;
				thisLine = Readline.readline("\nPlease enter the name of the mart database you wish to query (press enter to leave as '" + myDb + "'): ", false);
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
		if (!command.matches("\\w+\\s(on|off)"))
			throw new InvalidQueryException("Invalid setVerbose command recieved: " + command + "\n");

		String val = command.split("\\s")[1];
		verbose = (val.equals("on")) ? true : false;

		System.out.println("Logging now " + val + "\n");
		defaultLoggingConfiguration(verbose);
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
					throw new InvalidQueryException("Recieved invalid setOutputFormat request: " + command + "\n" + Help(SETOUTSETSC) + "\n");

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
					throw new InvalidQueryException("Recieved invalid setOutputFormat request: " + command + "\n" + Help(SETOUTSETSC) + "\n");
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
				throw new InvalidQueryException("invalid use command: does not contain a dataset to use: " + command + " has " + tokens.countTokens() + " tokens\n");

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
		System.out.println("Current Dataset " + dsetName);
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
		String[] lines = GetHistoryLines(req); // will throw an exception if GetHistoryLines requirements are not satisfied

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

				String[] lines = GetHistoryLines(req); // will throw an exception if GetHistoryLines requirements are not satisfied
				for (int i = 0, n = lines.length; i < n; i++) {
					String thisline = lines[i];

					if (historyOn)
						Readline.addToHistory(thisline);
					parse(thisline);
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

			String[] lines = GetHistoryLines(req); // will throw an exception if GetHistoryLines requirements are not satisfied
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

	private void parse(String line) throws SequenceException, FormatException, InvalidQueryException, IOException, SQLException {
		if (line.equals(LINEEND)) {
			String command = conline.append(line).toString().trim();
			continueQuery = false;
			conline = new StringBuffer();

			parseCommand(command);
		} else if (line.endsWith(LINEEND)) {
			String command = conline.append(" " + line).toString().trim();
			continueQuery = false;
			conline = new StringBuffer();

			parseCommand(command);
		} else {
			conline.append(" " + line);
			continueQuery = true;

			//MartCompleter Mode
			if (completionOn)
        mcl.SetModeForLine(line);
		}
	}

	private void parseCommand(String command) throws SequenceException, FormatException, InvalidQueryException, IOException, SQLException {
		int cLen = command.length();

		command = command.replaceAll("\\s;$", ";"); // removes any whitespace before the ; character

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

	private final String history_file = System.getProperty("user.home") + "/.martshell_history";

	private String martHost = null;
	private String martPort = null;
	private String martUser = null;
	private String martPass = null;
	private String martDatabase = null;
	private String martDatabaseType = "mysql"; //default
	private MartCompleter mcl; // will hold the MartCompleter, if Readline is loaded and completion turned on
	private boolean helpLoaded = false; // first time Help function is called, loads the help properties file and sets this to true
	private boolean historyOn = false; // commandline history, default to off
	private boolean completionOn = true; // command completion, default to on
	private boolean readlineLoaded = false; // true only if functional Readline library was loaded, false if PureJava
	private String userPrompt = null;

	private String altConfigurationFile = null;
	private FileOutputStream sessionOutputFile = null; // this is set using the setOutputSettings command.
	private final String DEFOUTPUTFORMAT = "tabulated"; // default to tabulated output
	private String sessionOutputFormat = null; // this is set using the setOutputSettings command.
	private final String DEFOUTPUTSEPARATOR = "\t"; // default to tab separated
	private String sessionOutputSeparator = null; // this is set using the setOutputSettings command.

	private String batchErrorMessage = null;
	private Properties commandHelp = new Properties();
	private final String HELPFILE = "data/help.properties"; //contains help general to the shell
	private final String DSHELPFILE = "data/dshelp.properties"; // contains help for domain specific aspects

	private final Pattern[] dsCommands = new Pattern[] { Pattern.compile("(.*)DOMAINSPECIFIC\\s(\\w+)(.+)", Pattern.DOTALL)};

	private final List dsHelpItems = Collections.unmodifiableList(Arrays.asList(new String[] { "selectSequence" }));

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

	protected final List availableCommands =
		Collections.unmodifiableList(
			Arrays.asList(
				new String[] {
					EXITC,
					QUITC,
					HELPC,
					DESCC,
					SETCONSETSC,
					SETOUTSETSC,
					SHOWOUTSETSC,
					SHOWCONSETSC,
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
	private final String FILTERPAGEKEY = "FilterPage";
	private final String FILTERGROUPKEY = "FilterGroup";
	private final String FILTERCOLLECTIONKEY = "FilterCollection";
	private final String FILTERSETKEY = "FilterSet";
	private final String FILTERSETDESCRIPTIONKEY = "FilterSetDescription";
	private final String FILTERKEY = "Filter";
	private final String ATTRIBUTEPAGEKEY = "AttributePage";
	private final String ATTRIBUTEGROUPKEY = "AttributeGroup";
	private final String ATTRIBUTECOLLECTIONKEY = "AttributeCollection";
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
  
	// variables for subquery
	private ByteArrayOutputStream subqueryOutput = null;
	private int nestedLevel = 0;
	private final int MAXNESTING = 1; // change this to allow deeper nesting of queries inside queries
	private final int MAXLINECOUNT = 60; // page prompt describe output line limit
	private final int MAXCHARCOUNT = 80; // describe group output limit

	private final String DASHES = "--------------------------------------------------------------------------------"; // describe output separator

	private List qualifiers = Arrays.asList(new String[] { "=", "!=", "<", ">", "<=", ">=", "exclusive", "excluded", "in" });

	private boolean continueQuery = false;
	private StringBuffer conline = new StringBuffer();

	//other strings needed
	private final String LINEEND = ";";
}
