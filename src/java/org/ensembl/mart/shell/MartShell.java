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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FormatException;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.InputSourceUtil;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.LoggingUtils;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.SequenceException;
import org.ensembl.mart.lib.DatabaseUtil.DatabaseURLElements;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributeGroup;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.CompositeDSViewAdaptor;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.RegistryDSViewAdaptor;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

/**
 * <p>Interface to a Mart Database implimentation that provides commandline access using a SQL-like query language (see MartShellLib for a 
 *  description of the Mart Query Language). The system can be used to run script files containing valid Mart Query Language commands, 
 *  or individual queries from the commandline.
 *  It has an interactive shell as well.  Script files can include comment lines beginning with #, which are ignored by the system.</p>  
 * 
 * <p>The interactive shell makes use of the <a href="http://java-readline.sourceforge.net/">Java Readline Library</a>
 * to allow commandline editing, history, and tab completion for those users working on Linux/Unix operating systems.  Unfortunately, there is no way
 * to provide this functionality in a portable way across OS platforms.  For windows users, there is a Getline c library which is provided with the Java Readline source.
 * By following the instructions to build a windows version of this library, you will get some (but not all) of this functionality.</p>
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
	private static final String DEFAULTREGISTRY = "defaultRegistry";
	private static final String INITSCRIPT = "initScript";

	private static final String defaultConf = System.getProperty("user.home") + "/.martshell";
	private static String COMMAND_LINE_SWITCHES = "h:AR:I:M:d:vl:e:O:F:S:E:";
	private static String confinUse = null;
	private static String mainRegistry = null;
	private static String mainInitScript = null;

	private static String mainDefaultDataset = null;
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
			+ "\n-R MARTREGISTRY_FILE_URL                - URL or path to MartRegistry (Bookmark) document"
			+ "\n-M SHELL_CONFIGURATION_FILE_URL         - URL or path to shell configuration file"
			+ "\n-I INITIALIZATION_SCRIPT                - URL or path to Shell initialization MQL script"
			+ "\n-d DATASETVIEW                          - DatasetViewname"
			+ "\n-v                                      - verbose logging output"
			+ "\n-l LOGGING_CONFIGURATION_URL            - URL to Java logging system configuration file (example file:data/exampleLoggingConfig.properties)"
			+ "\n-e MARTQUERY                            - a well formatted Mart Query to run in Batch Mode"
			+ "\n\nThe following are used in combination with the -e flag:"
			+ "\n-O OUTPUT_FILE                          - output file, default is standard out"
			+ "\n-F OUTPUT_FORMAT                        - output format, either tabulated or fasta"
			+ "\n-S OUTPUT_SEPARATOR                     - if OUTPUT_FORMAT is tabulated, can define a separator, defaults to tab separated"
			+ "\n\n-E QUERY_FILE_FILE_URL                - URL or path to file with valid Mart Query Commands"
			+ "\n\nThe application searches for a .martshell file in the user home directory for shell configuration information."
			+ "\nif present, this file will be loaded. If the -M, -R or -I options are given, these over-ride those values provided in the .martshell file"
			+ "\nUsers specifying a shell configuration file with -M,"
			+ "\nor using a .martshell file, can use -R, or -I to specify"
			+ "\nparameters not specified in the configuration file, or over-ride those that are specified."
			+ "\n\nAn Inititialization script can contain any MQL statements, but is best suited to statements concerning"
			+ "\nMart management, such as initializing the Mart to query for the session, or the various DatasetViews"
			+ "\nbeing querried."
			+ "\n";
	}

	/**
	 * Parses java properties file to get mysql shell configuration parameters.
	 * 
	 * @param connfile -- String name of the configuration file containing shell
	 * configuration properties.
	 */
	public static void getConnProperties(String connfile) {
		URL confInfo;
		Properties p = new Properties();

		try {
			p.load(InputSourceUtil.getStreamForString(connfile));

			String tmp = p.getProperty(INITSCRIPT);
			if (tmp != null && tmp.length() > 1 && mainInitScript == null)
				mainInitScript = tmp.trim();

			tmp = p.getProperty(DEFAULTREGISTRY);
			if (tmp != null && tmp.length() > 1 && mainRegistry == null) {
				mainRegistry = tmp.trim();
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

					if (value != null) {
						//strip leading and trailing quotes
						if (value.startsWith("'"))
							value = value.substring(1);
						if (value.startsWith("\""))
							value = value.substring(1);

						if (value.endsWith("'"))
							value = value.substring(0, value.lastIndexOf("'"));
						if (value.endsWith("\""))
							value = value.substring(0, value.lastIndexOf("\""));

						buf.append(value);
					}

					argtable.put(key, buf);
				}
			} else {
				if (key == null)
					throw new Exception("Invalid Arguments Passed to MartShell\n");
				StringBuffer value = (StringBuffer) argtable.get(key);
				if (value.length() > 0)
					value.append(" ");

				//strip leading and trailing quotes
				if (arg.startsWith("'"))
					arg = arg.substring(1);
				if (arg.startsWith("\""))
					arg = arg.substring(1);

				if (arg.endsWith("'"))
					arg = arg.substring(0, arg.lastIndexOf("'"));
				if (arg.endsWith("\""))
					arg = arg.substring(0, arg.lastIndexOf("\""));

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

					case 'R' :
						mainRegistry = g.getOptarg();
						break;

					case 'I' :
						mainInitScript = g.getOptarg();
						break;

					case 'A' :
						commandComp = false;
						break;

						// get everything that is specified in the provided configuration file, then fill in rest with other options, if provided
					case 'M' :
						getConnProperties(g.getOptarg());
						break;

					case 'd' :
						mainDefaultDataset = g.getOptarg();
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

					case 'S' :
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
				LoggingUtils.setLoggingConfiguration(InputSourceUtil.getStreamForString(loggingURL));
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

		if (mainRegistry != null)
			try {
				ms.addMartRegistry(mainRegistry);
			} catch (MalformedURLException e1) {
				System.out.println("Could not set default Registry file " + e1.getMessage());
				e1.printStackTrace();
			} catch (ConfigurationException e1) {
				System.out.println("Could not set default Registry file " + e1.getMessage());
				e1.printStackTrace();
			}

		if (mainInitScript != null)
			try {
				ms.initializeWithScript(mainInitScript);
			} catch (Exception e2) {
				System.out.println("Could not initialize MartShell with initScript " + e2.getMessage());
				e2.printStackTrace();
			}

		if (mainDefaultDataset != null)
			ms.setDefaultDatasetName(mainDefaultDataset);

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
				completionOn = true;
				readlineLoaded = true;
			} catch (UnsatisfiedLinkError ignore_me2) {
				try {
					Readline.load(ReadlineLibrary.GnuReadline);
					historyOn = true;
					completionOn = true;
					readlineLoaded = true;
				} catch (UnsatisfiedLinkError ignore_me3) {
					mainLogger.warning(
						"Could not load Readline Library, commandline editing, completion will not be available"
							+ "\nConsult MartShell documentation for methods to resolve this error.");
					historyOn = false;
					completionOn = false;
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
      System.out.println();
      System.out.println(supportHelp.getProperty(STARTUP));
      System.out.println();
    } catch (InvalidQueryException e2) {
      System.out.println("Couldnt display startup information\n" + e2.getMessage());

      StackTraceElement[] stacks = e2.getStackTrace();
      StringBuffer stackout = new StringBuffer();

      for (int i = 0, n = stacks.length; i < n; i++) {
        StackTraceElement element = stacks[i];
        stackout.append(element.toString()).append("\n");
      }

      if (mainLogger.isLoggable(Level.INFO))
        mainLogger.info("\n\nStackTrace:\n" + stackout.toString());
    }
    
		try {
			initializeMartShellLib();

			if (completionOn) {
				mcl = new MartCompleter(adaptorManager);

				// add commands
				List allCommands = new ArrayList();
				allCommands.addAll(availableCommands);
				allCommands.addAll(msl.availableCommands);
				mcl.setBaseCommands(allCommands);

				mcl.setAddCommands(addRequests);
				mcl.setRemoveBaseCommands(removeRequests);
				mcl.setListCommands(listRequests);
				mcl.setUpdateBaseCommands(updateRequests);
				mcl.setSetBaseCommands(setRequests);
				mcl.setDescribeBaseCommands(describeRequests);
				mcl.setEnvironmentBaseCommands(envRequests);
				mcl.setExecuteBaseCommands(executeRequests);

				// add sequences
				mcl.setDomainSpecificCommands(SequenceDescription.SEQS); // will need to modify this if others are added

				if (helpLoaded)
					mcl.setHelpCommands(commandHelp.keySet());

				if (envDatasetIName != null)
					mcl.setEnvDataset(envDatasetIName);

				mcl.setMartNames(martMap.keySet()); // sets to any initial Marts from initialization

				mcl.setCommandMode();

				Readline.setCompleter(mcl);
			}

			if (mainLogger.isLoggable(Level.INFO))
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

			if (mainLogger.isLoggable(Level.INFO))
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

				if (mainLogger.isLoggable(Level.INFO))
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

			if (mainLogger.isLoggable(Level.INFO))
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
			initializeMartShellLib();
			InputStream input = InputSourceUtil.getStreamForString(batchScriptFile);

			ExecScriptFromStream(input);
			input.close();
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

		try {
			initializeMartShellLib();
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

			if (mainLogger.isLoggable(Level.INFO))
				mainLogger.info("\n\nStackTrace:\n" + stackout.toString());

			validQuery = false;
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
	 * Method allowing client scripts to specify a default MartRegistry.dtd compliant
	 * document specifying the location of Mart DatasetViews.
	 * 
	 * @param confFile - String path or URL to MartRegistry file
	 * 
	 */
	public void addMartRegistry(String confFile) throws ConfigurationException, MalformedURLException {
		URL confURL = InputSourceUtil.getURLForString(confFile);

		if (confURL == null)
			throw new ConfigurationException("Could not parse " + confFile + " into a URL\n");

		RegistryDSViewAdaptor adaptor = new RegistryDSViewAdaptor(confURL);
		adaptorManager.add(adaptor);
	}

	/**
	 * Takes a script with MQL commands (typically, add Mart, set Mart, add DatasetView(s), use DatasetView ,etc).
	 * This will ignore lines commented with #.
	 * @param initScript -- either path or URL to MQL initialization script.
	 */
	public void initializeWithScript(String initScript)
		throws ConfigurationException, SequenceException, FormatException, InvalidQueryException, IOException, SQLException {

		initializeMartShellLib();

		InputStream input = InputSourceUtil.getStreamForString(initScript);
		ExecScriptFromStream(input);
		input.close();
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

			sessionOutput = new FileOutputStream(batchFile);
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
	public void setDefaultDatasetName(String datasetName) {
		this.envDatasetIName = datasetName;
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

	private void initializeMartShellLib() {
		if (msl == null)
			msl = new MartShellLib(adaptorManager);
		else
			msl.setDSViewAdaptor(adaptorManager);
	}

	private void ExitShell() throws IOException {
		Readline.cleanup();

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
			mcl.setCommandMode();

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

		} else if (command.startsWith(HELPC))
			return Help(command.substring(command.indexOf(HELPC) + HELPC.length() + 1).trim());
		else {
			buf.append("\n");
			StringTokenizer hToks = new StringTokenizer(command, " ");
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
				commandHelp.remove(HISTORYC);
				availableCommands.remove(HISTORYC);
				commandHelp.remove(LOADSCRPTC);
				availableCommands.remove(LOADSCRPTC);
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

	private void ListRequest(String command) throws InvalidQueryException, ConfigurationException {
		System.out.println();
		String[] toks = command.split("\\s+");

		if (toks.length == 2) {
			String request = toks[1];
			String[] lines = null;

			if (request.equalsIgnoreCase(DATASETVIEWSREQ))
				lines = listDatasetViews();
			else if (request.equalsIgnoreCase(FILTERSREQ))
				lines = listFilters();
			else if (request.equalsIgnoreCase(ATTRIBUTESREQ))
				lines = listAttributes();
			else if (request.equalsIgnoreCase(PROCSREQ))
				lines = listProcedures();
			else if (request.equalsIgnoreCase(MARTSREQ))
				lines = listMarts();
			else
				throw new InvalidQueryException("Invalid list command recieved: " + command + "\n");

			if (lines != null) {
				PageOutput(lines);
			}
		} else
			throw new InvalidQueryException("Invalid list command recieved: " + command + "\n");
		System.out.println();
	}

	private String[] listDatasetViews() throws ConfigurationException {
		if (adaptorManager.getDatasetViews().length == 0)
			return new String[] { "No DatasetViews Loaded\n" };

		DatasetView[] ds = adaptorManager.getDatasetViews();

		String[] ret = new String[ds.length];

		for (int i = 0, n = ds.length; i < n; i++)
			ret[i] = ds[i].getInternalName() + "\n";

		Arrays.sort(ret);
		return ret;
	}

	private String[] listProcedures() {
    if (mainLogger.isLoggable(Level.INFO))
      mainLogger.info("Listing Procedures\n");
      
		if (msl.getStoredMQLCommandKeys().size() == 0)
			return new String[] { "No Procedures Stored\n" };

		Set names = msl.getStoredMQLCommandKeys();
		String[] ret = new String[names.size()];

		int i = 0;
		for (Iterator iter = names.iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			ret[i] = name + "\n";
			i++;
		}
		Arrays.sort(ret);
		return ret;
	}

	private String[] listMarts() {
    if (mainLogger.isLoggable(Level.INFO))
      mainLogger.info("Listing Marts\n");
      
		if (martMap.keySet().size() == 0)
			return new String[] { "No Marts have been loaded\n" };

		Set names = martMap.keySet();
		String[] ret = new String[names.size()];

		int i = 0;
		for (Iterator iter = names.iterator(); iter.hasNext();) {
			String sourceName = (String) iter.next();
			ret[i] = sourceName + "\n";
			i++;
		}

		Arrays.sort(ret);
		return ret;
	}

	private String[] listFilters() throws InvalidQueryException, ConfigurationException {
		if (envDatasetIName != null) {
			if (!adaptorManager.supportsInternalName(envDatasetIName))
				throw new InvalidQueryException("This mart does not support DatasetView" + envDatasetIName + "\n");

			int blen = 3; //3 filters/line
			DatasetView dset = adaptorManager.getDatasetViewByInternalName(envDatasetIName);
			List columns = new ArrayList();
			String[] buffer = new String[blen];

			int[] maxlengths = new int[] { 0, 0, 0 };

			List names = dset.getFilterCompleterNames();
			Collections.sort(names);

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
			throw new InvalidQueryException("Must set a DatasetView with the use command to list filters\n");
	}

	private String[] listAttributes() throws ConfigurationException, InvalidQueryException {
		if (envDatasetIName != null) {
			if (!adaptorManager.supportsInternalName(envDatasetIName))
				throw new InvalidQueryException("This mart does not support DatasetView" + envDatasetIName + "\n");

			int blen = 3; //3 atts/line
			DatasetView dset = adaptorManager.getDatasetViewByInternalName(envDatasetIName);
			List columns = new ArrayList();
			String[] buffer = new String[blen];

			int[] maxlengths = new int[] { 0, 0, 0 };

			List names = dset.getAttributeCompleterNames();
			Collections.sort(names);

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
			throw new InvalidQueryException("Must set a DatasetView with the use command to list attributes\n");
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

	private void DescribeRequest(String command) throws InvalidQueryException, ConfigurationException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		int tokCount = toks.countTokens();
		toks.nextToken(); // skip describe

		System.out.println();

		if (tokCount < 3)
			throw new InvalidQueryException("Invalid Describe request " + command + "\n" + Help(DESCC));
		else {
			String request = toks.nextToken();
			String name = toks.nextToken();

			if (request.equalsIgnoreCase(MARTREQ)) {
				String tmp = DescribeMart(name);
				System.out.println(tmp + "\n");
			} else if (request.equalsIgnoreCase(DATASETVIEWREQ)) {
				String[] lines = DescribeDataset(name);

				PageOutput(lines);
			} else if (request.equalsIgnoreCase(FILTERREQ)) {
				if (envDatasetIName == null)
					throw new InvalidQueryException("Must set a DatasetView with a use command for describe filter to work\n");

				DatasetView dset = adaptorManager.getDatasetViewByInternalName(envDatasetIName);
				if (!(dset.containsFilterDescription(name)))
					throw new InvalidQueryException("Filter " + name + " is not supported by DatasetView" + envDatasetIName + "\n");

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
			} else if (request.equalsIgnoreCase(ATTRIBUTEREQ)) {
				if (envDatasetIName == null)
					throw new InvalidQueryException("Must set a DatasetView with a use command for describe attribute to work\n");

				DatasetView dset = adaptorManager.getDatasetViewByInternalName(envDatasetIName);
				if (!dset.containsAttributeDescription(name))
					throw new InvalidQueryException("Attribute " + name + " is not supported by DatasetView" + envDatasetIName + "\n");

				String tmp = DescribeAttribute(dset.getAttributeDescriptionByInternalName(name));
				System.out.println(tmp + "\n");
			} else if (request.equalsIgnoreCase(PROCREQ)) {
				String out = msl.describeStoredMQLCommand(name);
				if (out == null)
					throw new InvalidQueryException("Procedure " + name + " has not been defined\n");
				else
					System.out.println(out);
			} else
				throw new InvalidQueryException("Invalid Request key in describe command, see help describe. " + command + "\n");
		}
	}

	private String DescribeMart(String name) throws InvalidQueryException {
		if (!martMap.containsKey(name))
			throw new InvalidQueryException(MARTREQ + " " + name + " has not been stored\n");

		String ret = null;
		try {
			String user = envMart.getConnection().getMetaData().getUserName();
			DatabaseURLElements els = DatabaseUtil.decompose(envMart.getConnection().getMetaData().getURL());
			ret = "Mart: " + name + " HOST: " + els.host + " USER: " + user + " MART NAME: " + els.databaseName;
		} catch (Exception e) {
			throw new InvalidQueryException("Could not parse Mart for Information " + e.getMessage(), e);
		}

		if (ret == null)
			throw new InvalidQueryException("Could not parse Mart for information");

		return ret;
	}

	private String[] DescribeDataset(String dsetname) throws ConfigurationException, InvalidQueryException {
		if (!adaptorManager.supportsInternalName(dsetname))
			throw new InvalidQueryException("This mart does not support DatasetView" + dsetname + "\n");

		List lines = new ArrayList();

		DatasetView dset = adaptorManager.getDatasetViewByInternalName(dsetname);

		if (dset.getDatasource() != null) {
			try {
				DatabaseURLElements els = DatabaseUtil.decompose(dset.getDatasource().getConnection().getMetaData().getURL());
				lines.add("Currently connected to RDBMS " + els.host);
				lines.add("\n");
			} catch (Exception e) {
				if (mainLogger.isLoggable(Level.INFO))
					mainLogger.info("Could not parse Mart for DatasetView into Connection info\n");
			}
		}

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
			lines.add("numbers in perentheses denote groups of attributes that have limits on the number that can be queried together\n");
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

	private void addRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); // skip add

		if (toks.hasMoreTokens()) {
			String addreq = toks.nextToken();
			if (addreq.equalsIgnoreCase(MARTREQ))
				addMart(toks);
			else if (addreq.equalsIgnoreCase(DATASETVIEWSREQ))
				addDatasetViews(toks);
			else if (addreq.equalsIgnoreCase(DATASETVIEWREQ))
				addDatasetView(toks);
			else
				throw new InvalidQueryException("Invalid Add request recieved " + command + "\n");
		} else
			throw new InvalidQueryException("Invalid Add request recieved " + command + "\n");

		if (completionOn) {
			mcl.setMartNames(martMap.keySet());
			mcl.setAdaptorLocations(adaptorMap.keySet());

			try {
				mcl.setDatasetViewInternalNames(Arrays.asList(adaptorManager.getDatasetInternalNames()));
			} catch (ConfigurationException e) {
				throw new InvalidQueryException("Could not get internalNames from adaptorManager " + e.getMessage(), e);
			}
		}
	}

	private void addMart(StringTokenizer toks) throws InvalidQueryException {
		String martHost = null;
		String martDatabaseType = null;
		String martPort = null;
		String martUser = null;
		String martPass = null;
		String martDatabase = null;
		String martDriver = null;
		String sourceKey = null;

		if (toks.countTokens() > 0) {

			String connSettings = toks.nextToken();

			while (toks.hasMoreTokens())
				connSettings += toks.nextToken();

			if (connSettings.indexOf("as") > 0) {
				String[] vals = connSettings.split("as");
				connSettings = vals[0].trim();
				sourceKey = vals[1].trim();
			}

			//pattern to find and parse all occurances of x=y, or x = y in a string
			Pattern pat = Pattern.compile("(\\w+)\\s*\\=+\\s*([^\\,]+)\\,*\\s*");
			Matcher mat = pat.matcher(connSettings);
			boolean matchFound = false;

			while (mat.find()) {
				matchFound = true;

				String key = mat.group(1);
				String value = mat.group(2);

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
				else if (key.equals(INSTANCENAME))
					martDatabase = value;
				else if (key.equals(DBDRIVER))
					martDriver = value;
				else
					throw new InvalidQueryException("Recieved invalid add Mart command.\n" + Help(ADDC) + "\n");
			}

			if (!matchFound)
				throw new InvalidQueryException("Invalid set Output request " + connSettings + "\n" + Help(ADDC));

		} else {
			if (mainBatchMode)
				throw new InvalidQueryException("Recieved invalid add Mart command.\n" + Help(ADDC) + "\n");

			String thisLine = null;

			try {
				thisLine = Readline.readline("\nPlease enter the host address of the mart database: ", false);
				if (thisLine != null)
					martHost = thisLine;

				thisLine =
					Readline.readline(
						"\nPlease enter the type of RDBMS hosting the mart database (if both type and jdbc driver are left blank, defaults to "
							+ DatabaseUtil.DEFAULTDATABASETYPE
							+ " and "
							+ DatabaseUtil.DEFAULTDRIVER
							+ " respectively): ",
						false);
				if (thisLine != null)
					martDatabaseType = thisLine;

				thisLine =
					Readline.readline(
						"\nPlease enter the Driver Class Name of the RDBMS hosting the mart database (if both type and jdbc driver are left blank, defaults to "
							+ DatabaseUtil.DEFAULTDATABASETYPE
							+ " and "
							+ DatabaseUtil.DEFAULTDRIVER
							+ " respectively): ",
						false);
				if (thisLine != null)
					martDriver = thisLine;

				thisLine =
					Readline.readline(
						"\nPlease enter the port on which the mart database is running (defaults to "
							+ DatabaseUtil.DEFAULTPORT
							+ " for "
							+ DatabaseUtil.DEFAULTDATABASETYPE
							+ " database systems): ",
						false);
				if (thisLine != null)
					martPort = thisLine;

				thisLine = Readline.readline("\nPlease enter the user name used to connect to the mart database: ", false);
				if (thisLine != null)
					martUser = thisLine;

				thisLine = Readline.readline("\nPlease enter the password used to connect to the mart database: ", false);
				if (thisLine != null)
					martPass = thisLine;

				thisLine = Readline.readline("\nPlease enter the name of the mart database you wish to query: ", false);
				if (thisLine != null)
					martDatabase = thisLine;

				thisLine =
					Readline.readline("\nPlease enter a name to refer to this Mart in Shell commands (defaults to " + martDatabase + "@" + martHost + "): ", false);
				if (thisLine != null)
					sourceKey = thisLine;

			} catch (Exception e) {
				throw new InvalidQueryException("Problem reading input for mart connection settings: " + e.getMessage());
			}
		}

		if (martDatabaseType == null && martDriver == null) {
			martDatabaseType = DatabaseUtil.DEFAULTDATABASETYPE;
			martDriver = DatabaseUtil.DEFAULTDRIVER;
		}

		if (martDatabaseType.equals(DatabaseUtil.DEFAULTDATABASETYPE) && martPort == null)
			martPort = DatabaseUtil.DEFAULTPORT;

		try {
			DataSource ds =
				DatabaseUtil.createDataSource(martDatabaseType, martHost, martPort, martDatabase, martUser, martPass, DatabaseUtil.DEFAULTPOOLSIZE, martDriver);

			if (sourceKey != null)
				martMap.put(sourceKey, ds);
			else
				martMap.put(martDatabase + "@" + martHost, ds); //TODO: May need to bind more inforamation to a particular datasource name

			msl.setMartMap(martMap);
		} catch (ConfigurationException e) {
			throw new InvalidQueryException("Could not create Mart with given connection parameters " + e.getMessage(), e);
		}
	}

	private void addDatasetViews(StringTokenizer toks) throws InvalidQueryException {
		if (toks.countTokens() == 2) {
			toks.nextToken(); // ignore from

			String source = toks.nextToken();

			if (martMap.containsKey(source)) {
				//add DatabaseDSViewAdaptor with Mart
				DataSource ds = (DataSource) martMap.get(source);

				try {
					String user = ds.getConnection().getMetaData().getUserName();

					//remove any @host if present
					if (user.indexOf("@") >= 0)
						user = user.substring(0, user.indexOf("@"));

					DatabaseDSViewAdaptor adaptor = new DatabaseDSViewAdaptor(ds, user);
					adaptorManager.add(adaptor);

					adaptorMap.put(source, adaptor);
				} catch (Exception e) {
					throw new InvalidQueryException("Could not create DatabaseDSViewAdaptor with Mart " + source + " " + e.getMessage() + "\n", e);
				}
			} else {
				try {
					URL regURL = InputSourceUtil.getURLForString(source);
					RegistryDSViewAdaptor adaptor = new RegistryDSViewAdaptor(regURL);
					adaptorManager.add(adaptor);
					adaptorMap.put(source, adaptor);
				} catch (MalformedURLException e) {
					throw new InvalidQueryException("Recieved MalformedURLException parsing " + source + " into a URL " + e.getMessage() + "\n", e);
				} catch (ConfigurationException e) {
					throw new InvalidQueryException("Recieved ConfigurationException loading DatasetViews from " + source + " " + e.getMessage() + "\n", e);
				}
			}
		} else
			throw new InvalidQueryException("Recieved invalid add DatasetViews command. " + Help(ADDC) + "\n");
	}

	private void addDatasetView(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			String source = toks.nextToken();

			try {
				URL dsvURL = InputSourceUtil.getURLForString(source);
				URLDSViewAdaptor adaptor = new URLDSViewAdaptor(dsvURL);
				adaptorManager.add(adaptor);
				adaptorMap.put(source, adaptor);
			} catch (MalformedURLException e) {
				throw new InvalidQueryException("Recieved MalformedURLException parsing " + source + " into a URL " + e.getMessage() + "\n", e);
			} catch (ConfigurationException e) {
				throw new InvalidQueryException("Recieved ConfigurationException loading DatasetView from " + source + " " + e.getMessage() + "\n", e);
			}

		} else
			throw new InvalidQueryException("Recieved invalid add DatasetView command. " + Help(ADDC) + "\n");
	}

	private void removeRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); // skip remove

		if (toks.hasMoreTokens()) {
			String removereq = toks.nextToken();
			if (removereq.equalsIgnoreCase(MARTREQ))
				removeMart(toks);
			else if (removereq.equalsIgnoreCase(DATASETVIEWSREQ))
				removeDatasetViews(toks);
			else if (removereq.equalsIgnoreCase(DATASETVIEWREQ))
				removeDatasetView(toks);
			else if (removereq.equalsIgnoreCase(PROCREQ))
				removeProcedure(toks);
			else
				throw new InvalidQueryException("Invalid remove request recieved " + command + "\n");
		} else
			throw new InvalidQueryException("Invalid remove request recieved " + command + "\n");

		if (completionOn) {
			mcl.setMartNames(martMap.keySet());
			mcl.setAdaptorLocations(adaptorMap.keySet());

			try {
				mcl.setDatasetViewInternalNames(Arrays.asList(adaptorManager.getDatasetInternalNames()));
			} catch (ConfigurationException e) {
				throw new InvalidQueryException("Caught ConfigurationException updating DatasetView internalNames " + e.getMessage(), e);
			}
		}
	}

	private void removeProcedure(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			String name = toks.nextToken();
			msl.removeStoredMQLCommand(name);

			if (completionOn)
				mcl.setProcedureNames(msl.getStoredMQLCommandKeys());
		} else
			throw new InvalidQueryException("Recieved invalid remove Procedure command. " + Help(REMOVEC) + "\n");
	}

	private void removeMart(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			String name = toks.nextToken();

			//If a DSViewAdaptor has been created with this Mart, remove it
			if (adaptorMap.containsKey(name)) {
				adaptorManager.remove((DSViewAdaptor) adaptorMap.get(name));
				adaptorMap.remove(name);
			}

			martMap.remove(name);

			msl.setMartMap(martMap);
		} else
			throw new InvalidQueryException("Recieved invalid remove Mart command. " + Help(REMOVEC) + "\n");
	}

	private void removeDatasetViews(StringTokenizer toks) throws InvalidQueryException {
		if (toks.countTokens() == 2) {
			toks.nextToken(); // skip from
			String source = toks.nextToken();

			if (adaptorMap.containsKey(source)) {
				adaptorManager.remove((DSViewAdaptor) adaptorMap.get(source));
				adaptorMap.remove(source);
			}
		} else if (toks.countTokens() == 1)
			adaptorManager.clear();
		else
			throw new InvalidQueryException("Recieved invalid remove DatasetViews comand. " + Help(REMOVEC) + "\n");
	}

	private void removeDatasetView(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			String dsvIname = toks.nextToken();

			try {
				if (adaptorManager.supportsInternalName(dsvIname))
					adaptorManager.removeDatasetView(adaptorManager.getDatasetViewByInternalName(dsvIname));
			} catch (ConfigurationException e) {
				throw new InvalidQueryException("Could not remove DatasetView " + dsvIname + " " + e.getMessage(), e);
			}
		} else
			throw new InvalidQueryException("Recieved invalid remove DatasetView command. " + Help(REMOVEC) + "\n");
	}

	private void updateRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); // skip update

		if (toks.hasMoreTokens()) {
			String updatereq = toks.nextToken();
			if (updatereq.equalsIgnoreCase(DATASETVIEWSREQ))
				updateDatasetViews(toks);
			else if (updatereq.equalsIgnoreCase(DATASETVIEWREQ))
				updateDatasetView(toks);
			else
				throw new InvalidQueryException("Invalid update request recieved " + command + "\n");
		} else
			throw new InvalidQueryException("Invalid update request recieved " + command + "\n");
	}

	private void updateDatasetViews(StringTokenizer toks) throws InvalidQueryException {
		try {
			if (!toks.hasMoreTokens())
				adaptorManager.update();
			else {
				toks.nextToken(); // skip from
				String source = toks.nextToken();

				if (adaptorMap.containsKey(source))
					 ((DSViewAdaptor) adaptorMap.get(source)).update();
			}

			if (completionOn)
				mcl.setDatasetViewInternalNames(Arrays.asList(adaptorManager.getDatasetInternalNames()));

		} catch (ConfigurationException e) {
			throw new InvalidQueryException("Could not update DatasetViews, " + e.getMessage() + "\n", e);
		}
	}

	private void updateDatasetView(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			String dsIname = toks.nextToken();

			try {
				if (!adaptorManager.supportsInternalName(dsIname))
					throw new ConfigurationException("DatasetView " + dsIname + " has not been loaded");

				DSViewAdaptor[] adaptors = adaptorManager.getAdaptors();
				for (int i = 0, n = adaptors.length; i < n; i++) {
					DSViewAdaptor adaptor = adaptors[i];

					if (adaptor.supportsInternalName(dsIname)) {
						adaptor.update();
						break;
					}
				}
			} catch (ConfigurationException e) {
				throw new InvalidQueryException("Could not update DatasetView " + dsIname + " " + e.getMessage(), e);
			}
		} else
			throw new InvalidQueryException("Recieved invalid update DatasetView command. " + Help(REMOVEC) + "\n");
	}

	private void unsetRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); // skip set

		if (toks.hasMoreTokens()) {
			String request = toks.nextToken();

			if (request.equalsIgnoreCase(PROMPTREQ))
				unsetPrompt();
			else if (request.equalsIgnoreCase(MARTREQ))
				unsetMart(toks);
			else if (request.equalsIgnoreCase(OUTPUTREQ))
				unsetOutputSettings(toks);
			else if (request.equalsIgnoreCase(VERBOSEREQ))
				unsetVerbose();
		} else
			throw new InvalidQueryException("Recieved invalid set command " + command + "\n" + Help(SETC));
	}

	private void unsetMart(StringTokenizer toks) throws InvalidQueryException {
		if (!toks.hasMoreTokens()) {
			envMartSetBySet = false;
			envMart = null;
		} else {
			String dsIname = toks.nextToken();

			try {
				if (!adaptorManager.supportsInternalName(dsIname))
					throw new InvalidQueryException("Recieved invalid DatasetView name " + dsIname + " in unset Mart command\n" + Help(UNSETC));

				adaptorManager.getDatasetViewByInternalName(dsIname).setDatasource(null);

			} catch (ConfigurationException e) {
				throw new InvalidQueryException("Recieved ConfigurationException unsetting Mart for DatasetView name " + dsIname + e.getMessage(), e);
			} catch (InvalidQueryException e) {
				throw e;
			}
		}
	}

	private void unsetPrompt() throws InvalidQueryException {
		userPrompt = null;
	}

	private void unsetVerbose() throws InvalidQueryException {
		if (loggingConfURL == null) {
			verbose = false;

			LoggingUtils.setVerbose(verbose);

			if (mainLogger.isLoggable(Level.INFO))
				mainLogger.info("Logging now off\n");
		} else
			throw new InvalidQueryException("Cannot change logging properties when a logging configuration URL is supplied\n");
	}

	private void unsetOutputSettings(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			try {
				while (toks.hasMoreTokens()) {
					String key = toks.nextToken(",").trim();

					if (key.equals(FILE)) {
						sessionOutput = DEFOUTPUT;
						appendToFile = false;
						sessionOutputFileName = null;
					} else if (key.equals(FORMAT))
						sessionOutputFormat = DEFOUTPUTFORMAT;
					else if (key.equals(SEPARATOR))
						sessionOutputSeparator = DEFOUTPUTSEPARATOR;
					else
						throw new InvalidQueryException("Recieved invalid unset Output request " + key + "\n" + Help(UNSETC));
				}
			} catch (Exception e) {
				throw new InvalidQueryException("Could not unset output settings: " + e.getMessage() + "\n", e);
			}
		} else {
			sessionOutput = DEFOUTPUT;
			sessionOutputFileName = null;
			sessionOutputFormat = DEFOUTPUTFORMAT;
			appendToFile = false;
			sessionOutputSeparator = DEFOUTPUTSEPARATOR;
		}
	}

	private void setRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); // skip set

		if (toks.hasMoreTokens()) {
			String request = toks.nextToken();

			if (request.equalsIgnoreCase(PROMPTREQ))
				setPrompt(toks);
			else if (request.equalsIgnoreCase(MARTREQ))
				setMart(toks);
			else if (request.equalsIgnoreCase(OUTPUTREQ))
				setOutputSettings(toks);
			else if (request.equalsIgnoreCase(VERBOSEREQ))
				setVerbose(toks);
		} else
			throw new InvalidQueryException("Recieved invalid set command " + command + "\n" + Help(SETC));
	}

	private void setMart(StringTokenizer toks) throws InvalidQueryException {
		if (!toks.hasMoreTokens())
			throw new InvalidQueryException("Invalid set Mart command\n" + Help(SETC));

		String name = toks.nextToken();

		if (!martMap.containsKey(name))
			throw new InvalidQueryException("Invalid Mart name " + name + " recieved in set Mart command\n" + Help(SETC));

		if (toks.hasMoreTokens()) {
			String dsIname = toks.nextToken();

			try {
				if (!adaptorManager.supportsInternalName(dsIname))
					throw new InvalidQueryException("Recieved invalid DatasetView name " + dsIname + " in set Mart command\n" + Help(SETC));

				adaptorManager.getDatasetViewByInternalName(dsIname).setDatasource((DataSource) martMap.get(name));

			} catch (ConfigurationException e) {
				throw new InvalidQueryException("Recieved ConfigurationException setting Mart " + name + " for DatasetView name " + dsIname + e.getMessage(), e);
			} catch (InvalidQueryException e) {
				throw e;
			}
		} else {
			envMartSetBySet = true;
			envMart = (DataSource) martMap.get(name);
		}
	}

	private void setPrompt(StringTokenizer toks) throws InvalidQueryException {
		if (!toks.hasMoreTokens())
			throw new InvalidQueryException("Invalid set Prompt Command Recieved\n" + Help(SETC));

		String prompt = toks.nextToken();

		if (prompt.equals("-"))
			userPrompt = null;
		else
			userPrompt = prompt + " >";
	}

	private void setVerbose(StringTokenizer toks) throws InvalidQueryException {
		if (!toks.hasMoreTokens())
			throw new InvalidQueryException("Invalid set Verbose Command Recieved\n" + Help(SETC));

		String command = toks.nextToken();
		if (loggingConfURL == null) {
			if (command.equals("on"))
				verbose = true;
			else if (command.equals("off"))
				verbose = false;
			else
				throw new InvalidQueryException("Invalid set Verbose command recieved: \n" + Help(SETC));

			LoggingUtils.setVerbose(verbose);

			if (mainLogger.isLoggable(Level.INFO))
				mainLogger.info("Logging now " + command + "\n");
		} else
			throw new InvalidQueryException("Cannot change logging properties when a logging configuration URL is supplied\n");
	}

	private void setOutputSettings(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			try {
				String fSettings = toks.nextToken();

				//pattern to find all occurances of x='y' in a string
				Pattern pat = Pattern.compile("(\\w+\\=\\'[^\\']+\\')");
				Matcher mat = pat.matcher(fSettings);
				boolean matchFound = false;

				while (mat.find()) {
					matchFound = true;
					String setReq = mat.group();

					String[] setkv = setReq.split("\\=");

					String key = setkv[0];
					String value = setkv[1];

					value = value.substring(value.indexOf("'") + 1, value.lastIndexOf("'")); // strip off leading and trailing quotes

					if (key.equals(FILE)) {
						if (value.equals("-")) {
							sessionOutputFileName = null;
							appendToFile = false;
							sessionOutput = DEFOUTPUT;
						} else {
							if (value.startsWith(">>")) {
								appendToFile = true;
								value = value.substring(2);
							} else
								appendToFile = false;
							sessionOutputFileName = value;
						}
					} else if (key.equals(FORMAT))
						sessionOutputFormat = value;
					else if (key.equals(SEPARATOR))
						sessionOutputSeparator = value;
					else
						throw new InvalidQueryException("Recieved invalid set Output request " + fSettings + "\n" + Help(SETC));
				}

				if (!matchFound)
					throw new InvalidQueryException("Invalid set Output request " + fSettings + "\n" + Help(SETC));

			} catch (Exception e) {
				throw new InvalidQueryException("Could not set output settings: " + e.getMessage() + "\n", e);
			}
		} else {
			//interactive
			if (mainBatchMode)
				throw new InvalidQueryException("Recieved invalid add Mart command.\n" + Help(ADDC) + "\n");

			String thisLine = null;

			try {
				String out = (sessionOutputFormat != null) ? sessionOutputFormat : DEFOUTPUTFORMAT;
				thisLine =
					Readline.readline(
						"\nPlease enter the format of the output (either 'tabulated' or 'fasta', enter '-' to use "
							+ DEFOUTPUTFORMAT
							+ ", hit enter to leave as "
							+ out
							+ "): ",
						false);
				if (thisLine != null) {
					if (thisLine.equals("-"))
						sessionOutputFormat = DEFOUTPUTFORMAT;
					else
						sessionOutputFormat = thisLine;
				}

				out = (sessionOutputFileName != null) ? sessionOutputFileName : DEFOUTPUTFILE;
				out = (appendToFile) ? ">>" + out : out;
				thisLine =
					Readline.readline(
						"\nPlease enter the File to output all MQL commands (use '-' for "
							+ DEFOUTPUTFILE
							+ ", hit enter to leave as "
							+ out
							+ ", prepend path with '>>' to append to an existing file): ",
						false);
				if (thisLine != null) {
					if (thisLine.equals("-")) {
						sessionOutput = DEFOUTPUT;
						appendToFile = false;
						sessionOutputFileName = null;
					} else {
						if (thisLine.startsWith(">>")) {
							appendToFile = true;
							thisLine = thisLine.substring(2);
						} else
							appendToFile = false;
						sessionOutputFileName = thisLine;
					}
				}

				out = (sessionOutputSeparator != null) ? sessionOutputSeparator : DEFOUTPUTSEPARATOR;

				thisLine =
					Readline.readline("\nPlease enter the record separator to use (use '-' for " + DEFOUTPUTSEPARATOR + ", hit enter to leave as " + out + "): ", false);

				if (thisLine != null) {
					if (thisLine.equals("-"))
						sessionOutputSeparator = DEFOUTPUTSEPARATOR;
					else
						sessionOutputSeparator = thisLine;
				}

			} catch (Exception e) {
				throw new InvalidQueryException("Problem reading input for mart connection settings: " + e.getMessage(), e);
			}
		}

	}

	private void envRequest(String command) throws InvalidQueryException {
		System.out.println();

		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); //skip environment

		if (!toks.hasMoreTokens())
			showAllEnvironment();
		else {
			String req = toks.nextToken();
			if (req.equalsIgnoreCase(MARTREQ))
				showEnvMart();
			else if (req.equalsIgnoreCase(DATASETVIEWREQ))
				showEnvDataSetView();
			else if (req.equalsIgnoreCase(OUTPUTREQ))
				showEnvOutput(toks);
			else
				throw new InvalidQueryException("Recieved invalid environment request " + command + "\n" + Help(ENVC));

			System.out.println();
		}
	}

	private void showAllEnvironment() throws InvalidQueryException {
		showEnvMart();
		showEnvDataSetView();
		showAllOutputSettings();
	}

	private void showEnvMart() throws InvalidQueryException {
		if (envMart == null)
			System.out.println(" Mart not set");
		else {
			try {
				String user = envMart.getConnection().getMetaData().getUserName();
				DatabaseURLElements els = DatabaseUtil.decompose(envMart.getConnection().getMetaData().getURL());
				System.out.println(" Mart HOST: " + els.host + " USER: " + user + " MART NAME: " + els.databaseName);
			} catch (Exception e) {
				throw new InvalidQueryException("Could not parse Mart for Information " + e.getMessage(), e);
			}
		}

		System.out.println();
	}

	private void showEnvDataSetView() {
		if (envDatasetIName == null)
			System.out.println(" DataSetView not set");
		else
			System.out.println(" DatasetView " + envDatasetIName);
		System.out.println();
	}

	private void showEnvOutput(StringTokenizer toks) throws InvalidQueryException {
		if (toks.hasMoreTokens()) {
			String subreq = toks.nextToken();
			if (subreq.equalsIgnoreCase(FORMAT)) {
				String out = (sessionOutputFormat != null) ? sessionOutputFormat : DEFOUTPUTFORMAT;
				System.out.println(" " + FORMAT + " = " + out);
			} else if (subreq.equalsIgnoreCase(FILE)) {
				String out = (sessionOutputFileName != null) ? sessionOutputFileName : DEFOUTPUTFILE;
				System.out.println(" " + FILE + " = " + out);
			} else if (subreq.equalsIgnoreCase(SEPARATOR)) {
				String out = (sessionOutputSeparator != null) ? sessionOutputSeparator : DEFOUTPUTSEPARATOR;
				System.out.println(" " + SEPARATOR + " = " + out);
			} else
				throw new InvalidQueryException("Recieved invalid Environment Output commmand " + subreq + "\n" + Help(ENVC));
		} else
			showAllOutputSettings();

		System.out.println();
	}

	private void showAllOutputSettings() {
		String thisFormat = (sessionOutputFormat != null) ? sessionOutputFormat : DEFOUTPUTFORMAT;
		String thisFile = (sessionOutputFileName != null) ? sessionOutputFileName : DEFOUTPUTFILE;
		String thisSeparator = (sessionOutputSeparator != null) ? sessionOutputSeparator : DEFOUTPUTSEPARATOR;

		System.out.println(
			" Output Format: " + FORMAT + " = " + thisFormat + ", " + SEPARATOR + " = " + "'" + thisSeparator + "'" + ", " + FILE + " = " + thisFile);

		System.out.println();
	}

	private void useRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); //skip use
		String datasourcereq = null;
		String datasetviewreq = null;

		String dsourceDelimiter = ">";
		if (command.indexOf(dsourceDelimiter) > 0) {
			datasetviewreq = toks.nextToken(dsourceDelimiter).trim();
			datasourcereq = toks.nextToken(dsourceDelimiter).trim();
		} else {
			datasourcereq = null;
			datasetviewreq = toks.nextToken();
		}

		try {

			if (mainLogger.isLoggable(Level.INFO)) {
				mainLogger.info("Attempting to set Environment DatasetView to " + datasetviewreq + "\n");
				if (datasourcereq != null && mainLogger.isLoggable(Level.INFO))
					mainLogger.info("Attempting to set Environment Mart to " + datasourcereq + "\n");
			}

			if (!adaptorManager.supportsInternalName(datasetviewreq))
				throw new InvalidQueryException("DatasetView '" + datasetviewreq + "' has not been loaded\n");
		} catch (ConfigurationException e) {
			throw new InvalidQueryException("Recieved ConfigurationException when determining support for " + datasetviewreq + " " + e.getMessage(), e);
		} catch (InvalidQueryException e) {
			throw e;
		}

		if (datasourcereq != null) {
			if (!martMap.containsKey(datasourcereq))
				throw new InvalidQueryException("Mart " + datasourcereq + " has not been loaded\n");
			envMart = (DataSource) martMap.get(datasourcereq);
		} else if (!envMartSetBySet)
			envMart = null;

		envDatasetIName = datasetviewreq;

		if (completionOn) {
			mcl.setEnvDataset(envDatasetIName);
		}
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

	private void executeRequest(String command) throws InvalidQueryException {
		StringTokenizer toks = new StringTokenizer(command, " ");
		toks.nextToken(); //skip execute

		if (!toks.hasMoreTokens())
			throw new InvalidQueryException("Invalid execute command recieved " + command + "\n" + Help(EXECC));

		String request = toks.nextToken();
		if (request.equalsIgnoreCase(HISTORYC))
			executeHistory(toks);
		else if (request.equalsIgnoreCase(PROCREQ))
			executeProcedure(toks);
		else if (request.equalsIgnoreCase(SCRIPTREQ))
			ExecuteScript(toks);
		else
			throw new InvalidQueryException("Invalid execute command recieved " + command + "\n" + Help(EXECC));
	}

	private void executeHistory(StringTokenizer toks) throws InvalidQueryException {
		if (!toks.hasMoreTokens())
			throw new InvalidQueryException("Invalid execute history command recieved\n" + Help(EXECC));

		String req = null;
		try {
			req = toks.nextToken();

			String[] lines = GetHistoryLines(req);
			// will throw an exception if GetHistoryLines requirements are not satisfied
			for (int i = 0, n = lines.length; i < n; i++) {
				String thisline = lines[i];

				if (historyOn)
					Readline.addToHistory(thisline);

				while (thisline != null)
					thisline = parseForCommands(thisline);
			}
		} catch (Exception e) {
			throw new InvalidQueryException("Could not execute history " + req + " " + e.getMessage(), e);
		}
	}

	private void executeProcedure(StringTokenizer toks) throws InvalidQueryException {
		if (!toks.hasMoreTokens())
			throw new InvalidQueryException("Invalid execute procedure command recieved\n" + Help(EXECC));

		String storedCommandName = toks.nextToken();

		String bindValues = null;
		if (storedCommandName.indexOf(MartShellLib.LISTSTARTCHR) > 0) {
			bindValues = storedCommandName.substring(storedCommandName.indexOf(MartShellLib.LISTSTARTCHR) + 1, storedCommandName.indexOf(MartShellLib.LISTENDCHR));
			storedCommandName = storedCommandName.substring(0, storedCommandName.indexOf(MartShellLib.LISTSTARTCHR));
		}

		String nestedQuery = msl.describeStoredMQLCommand(storedCommandName);

		if (nestedQuery != null) {
			try {
				if ((bindValues != null) && (bindValues.length() > 0)) {
					List bindVariables = new ArrayList();
					StringTokenizer vtokens = new StringTokenizer(bindValues, ",");
					while (vtokens.hasMoreTokens())
						bindVariables.add(vtokens.nextToken().trim());

					Pattern bindp = Pattern.compile("\\?");
					Matcher bindm = bindp.matcher(nestedQuery);

					StringBuffer qbuf = new StringBuffer();
					int bindIter = 0;
					while (bindm.find()) {
						bindm.appendReplacement(qbuf, (String) bindVariables.get(bindIter));
						bindIter++;
					}
					bindm.appendTail(qbuf);
					nestedQuery = qbuf.toString();
				}

				executeCommand(nestedQuery);
			} catch (Exception e) {
				throw new InvalidQueryException("Recieved Exception executing Stored Procedure " + e.getMessage() + "\n", e);
			}
		} else
			throw new InvalidQueryException("Procedure for " + storedCommandName + " has not been defined\n");
	}

	private void ExecuteScript(StringTokenizer toks) throws InvalidQueryException {
		if (!(toks.hasMoreTokens()))
			throw new InvalidQueryException("Recieved invalid execute Script command, must supply a URL or path\n");

		String source = toks.nextToken();

		try {
			InputStream inStream = InputSourceUtil.getStreamForString(source);
			ExecScriptFromStream(inStream);
			inStream.close();
		} catch (Exception e) {
			throw new InvalidQueryException("Could not execute script: " + source + " " + e.getMessage(), e);
		}
	}

	private void ExecScriptFromStream(InputStream input) throws InvalidQueryException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));

			String line = null;
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("#")) {
					if (historyOn)
						Readline.addToHistory(line);

					parse(line);
				}
			}
		} catch (Exception e) {
			throw new InvalidQueryException(e.getMessage(), e);
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
							throw new InvalidQueryException(nfe.getMessage(), nfe);
						}
					}
				} else //n
					try {
						start = Integer.parseInt(req);
					} catch (NumberFormatException nfe) {
						throw new InvalidQueryException(nfe.getMessage(), nfe);
					}
			}

			String[] lines = GetHistoryLines(req);
			// will throw an exception if GetHistoryLines requirements are not satisfied
			for (int i = 0, n = lines.length; i < n; i++) {
				System.out.print(start + " " + lines[i] + "\n");
				start++;
			}
		} catch (Exception e) {
			throw new InvalidQueryException("Could not show history " + e.getMessage(), e);
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
						throw new InvalidQueryException(nfe.getMessage(), nfe);
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
						throw new InvalidQueryException(nfe.getMessage(), nfe);
					}
				}

				for (int i = start; i < end; i++)
					lines.add(Readline.getHistoryLine(i));
			} else {
				try {
					start = Integer.parseInt(req) - 1;
				} catch (NumberFormatException nfe) {
					throw new InvalidQueryException(nfe.getMessage(), nfe);
				}
				lines.add(Readline.getHistoryLine(start));
			}
		}
		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	/**
	 * Takes a String MQL line, and parses it for commands to execute.  If any
	 * complete commands are found, they are executed.
	 * If any incomplete commands are encountered (either after a complete command, separated by semicolon, 
	 * or in the middle of a single command), the line is cached for later addition by subsequent
	 * calls to parse. If the current line completes a command built up over successive previous 
	 * calls to parse, this command is executed.
	 * @param line -- String line to parse
	 * @throws SequenceException
	 * @throws FormatException
	 * @throws IOException
	 * @throws SQLException
	 * @throws InvalidQueryException
	 * @throws ConfigurationException
	 */
	public void parse(String line) throws SequenceException, FormatException, IOException, SQLException, InvalidQueryException, ConfigurationException {
		if (line.indexOf(LINEEND) >= 0) {
			String currentCommand = conline.append(" ").append(line).toString().trim();
			conline = new StringBuffer(); // may be reinitialized with residual

			String residual = parseForCommands(currentCommand);
			if (residual != null) {
				continueQuery = true;
				conline = new StringBuffer(residual);

				if (completionOn)
					mcl.setModeForLine(residual);
			} else
				continueQuery = false;
		} else {
			conline.append(" ").append(line);
			continueQuery = true;

			//MartCompleter Mode
			if (completionOn)
				mcl.setModeForLine(line);
		}
	}

	private String parseForCommands(String line)
		throws SequenceException, FormatException, IOException, SQLException, InvalidQueryException, ConfigurationException {
		StringBuffer residual = new StringBuffer();

		StringTokenizer commandtokens = new StringTokenizer(line, LINEEND, true);

		while (commandtokens.hasMoreTokens()) {
			String thisCommand = commandtokens.nextToken().trim();
			if (thisCommand.equals(LINEEND)) {
				if (residual.length() > 1) {
					executeCommand(residual.toString());
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

	/**
	 * Takes a complete MQL command, and executes it.
	 * @param command -- MQL to execute
	 * @throws SequenceException
	 * @throws FormatException
	 * @throws IOException
	 * @throws SQLException
	 * @throws InvalidQueryException
	 * @throws ConfigurationException
	 */
	public void executeCommand(String command)
		throws SequenceException, FormatException, IOException, SQLException, InvalidQueryException, ConfigurationException {
		int cLen = command.length();

		command = command.replaceAll("\\s;$", ";");
		// removes any whitespace before the ; character

		if (cLen == 0)
			return;
		else if (command.startsWith(USEC))
			useRequest(NormalizeCommand(command));
		else if (command.startsWith(ENVC))
			envRequest(NormalizeCommand(command));
		else if (command.startsWith(HELPC))
			System.out.print(Help(NormalizeCommand(command)));
		else if (command.startsWith(DESCC))
			DescribeRequest(NormalizeCommand(command));
		else if (command.startsWith(LISTC))
			ListRequest(NormalizeCommand(command));
		else if (command.startsWith(ADDC))
			addRequest(NormalizeCommand(command));
		else if (command.startsWith(REMOVEC))
			removeRequest(NormalizeCommand(command));
		else if (command.startsWith(SETC))
			setRequest(NormalizeCommand(command));
		else if (command.startsWith(UNSETC))
			unsetRequest(NormalizeCommand(command));
		else if (command.startsWith(UPDATEC))
			updateRequest(NormalizeCommand(command));
		else if (command.startsWith(HISTORYC))
			History(NormalizeCommand(command));
		else if (command.startsWith(EXECC))
			executeRequest(NormalizeCommand(command));
		else if (command.startsWith(LOADSCRPTC))
			LoadScript(NormalizeCommand(command));
		else if (command.startsWith(SAVETOSCRIPTC))
			WriteHistory(NormalizeCommand(command));
		else if (NormalizeCommand(command).equals(EXITC) || NormalizeCommand(command).equals(QUITC))
			ExitShell();
		else if (command.startsWith(MartShellLib.GETQSTART) || command.startsWith(MartShellLib.USINGQSTART)) {
			msl.setDataset(envDatasetIName);
			msl.setEnvMart(envMart);
			//above might set these to null

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

				if (completionOn)
					mcl.setProcedureNames(msl.getStoredMQLCommandKeys());
			} else {
				Query query = msl.MQLtoQuery(command);

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

				Engine engine = new Engine();

				if (sessionOutputFileName != null)
					sessionOutput = new FileOutputStream(sessionOutputFileName, appendToFile);

				engine.execute(query, fspec, sessionOutput);

				if (sessionOutputFileName != null)
					sessionOutput.close();
			}
		} else {
			throw new InvalidQueryException("\nInvalid Command: please try again " + command + "\n");
		}
	}

	/**
	 * Special Method for MartShellTest, allows it to set the
	 * session OutputStream to an OutputStream that it can
	 * manipulate.  If out is null, defaults to System.out
	 */
	public void setTestOutput(OutputStream out) {
		if (out != null)
			sessionOutput = out;
		else
			sessionOutput = DEFOUTPUT;
	}

	// MartShell instance variables
	private CompositeDSViewAdaptor adaptorManager = new CompositeDSViewAdaptor();
	private MartShellLib msl = null;
	private boolean verbose = false;
	private URL loggingConfURL = null;

	private final String history_file = System.getProperty("user.home") + "/.martshell_history";

	private MartCompleter mcl;
	// will hold the MartCompleter, if Readline is loaded and completion turned on
	private boolean helpLoaded = false;
	// first time Help function is called, loads the help properties file and sets this to true
	private boolean historyOn = false; // commandline history, default to off
	private boolean completionOn = false; // commannd completion, default to off
	private boolean readlineLoaded = false;
	// true only if functional Readline library was loaded, false if PureJava
	private String userPrompt = null;
	private final String DEFAULTPROMPT = "MartShell";

	//these are set using the set Output command.
	private final OutputStream DEFOUTPUT = System.out;
	private OutputStream sessionOutput = DEFOUTPUT; // defaults to System.out, can be chaged
	private String sessionOutputFileName = null;
	private String sessionOutputFormat = null;
	private String sessionOutputSeparator = null;

	//defaults for output settings tabulated, tab separated, STDOUT
	private final String DEFOUTPUTFORMAT = "tabulated";
	private final String DEFOUTPUTSEPARATOR = "\t";
	private final String DEFOUTPUTFILE = "STDOUT";
	private final String DEFOUTPUTMODE = "overwrite";
	private boolean appendToFile = false;

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
	private final String SETC = "set";
	private final String UNSETC = "unset";
	private final String ENVC = "environment";
	private final String ADDC = "add";
	private final String REMOVEC = "remove";
	private final String UPDATEC = "update";
	private final String EXECC = "execute";
	private final String LOADSCRPTC = "loadScript";
	private final String SAVETOSCRIPTC = "saveToScript";
	private final String HISTORYC = "history";
	private final String LISTC = "list";
	private final String SCRIPTREQ = "Script";
	private final String MARTREQ = "Mart";
	private final String MARTSREQ = "Marts";
	private final String DATASETVIEWSREQ = "DatasetViews";
	private final String DATASETVIEWREQ = "DatasetView";
	private final String FILTERSREQ = "filters";
	private final String FILTERREQ = "Filter";
	private final String ATTRIBUTESREQ = "attributes";
	private final String ATTRIBUTEREQ = "Attribute";
	private final String PROCREQ = "procedure";
	private final String PROCSREQ = "procedures";
	private final String PROMPTREQ = "prompt";
	private final String OUTPUTREQ = "output";
	private final String VERBOSEREQ = "verbose";

	//lists to set for completion of add, remove, list, set, update, describe, environment, execute
	private final List addRequests = Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { MARTREQ, DATASETVIEWREQ, DATASETVIEWSREQ })));

	private final List removeRequests =
		Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { MARTREQ, DATASETVIEWREQ, DATASETVIEWSREQ, PROCREQ })));

	private final List listRequests =
		Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { MARTSREQ, DATASETVIEWSREQ, FILTERSREQ, ATTRIBUTESREQ, PROCSREQ })));

	private final List setRequests = Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { MARTREQ, PROMPTREQ, OUTPUTREQ, VERBOSEREQ })));

	private final List updateRequests = Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { DATASETVIEWREQ, DATASETVIEWSREQ })));

	private final List describeRequests =
		Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { MARTREQ, DATASETVIEWREQ, FILTERREQ, ATTRIBUTEREQ, PROCREQ })));

	private final List envRequests = Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { DATASETVIEWREQ, MARTREQ, OUTPUTREQ })));

	private final List executeRequests = Collections.unmodifiableList(new ArrayList(Arrays.asList(new String[] { PROCREQ, HISTORYC, SCRIPTREQ })));

	protected List availableCommands =
		new ArrayList(
			Arrays.asList(new String[] { EXECC, LOADSCRPTC, SAVETOSCRIPTC, EXECC, EXITC, QUITC, HELPC, SETC, UNSETC, ENVC, ADDC, REMOVEC, UPDATEC, DESCC, LISTC, USEC }));

	// strings used to show/set output format settings
	private final String FILE = "file";
	private final String FORMAT = "format";
	private final String SEPARATOR = "separator";

	//environment dataset
	private String envDatasetIName = null;

	//environment Mart
	private DataSource envMart = null;

	private boolean envMartSetBySet = false;

	// maps user keys to DataSource
	private Hashtable martMap = new Hashtable();

	// maps source (url|path|MartMap key to DSViewAdaptors
	private Hashtable adaptorMap = new Hashtable();

	// strings used to show/set mart connection settings
	private final String DBHOST = "host";
	private final String DBUSER = "user";
	private final String DBPASSWORD = "password";
	private final String DBPORT = "port";
	private final String INSTANCENAME = "instanceName";
	private final String DATABASETYPE = "databaseType";
	private final String DBDRIVER = "jdbcDriver";

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
