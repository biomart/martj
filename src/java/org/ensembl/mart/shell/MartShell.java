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
	private static String COMMAND_LINE_SWITCHES = "hAC:M:H:P:U:p:d:vl:e:O:F:R:E:";
	private static String confinUse = null;
	private static String mainConfiguration = null;
	private static String mainHost = null;
	private static String mainPort = null;
	private static String mainDatabase = null;
	private static String mainUser = null;
	private static String mainPassword = null;
	private static boolean mainBatchMode = false; // if -e is passed, go true
	private static String mainBatchSQL = null;
	private static String mainBatchScriptFile = null;
	// can hold the URL to a mart script
	private static String mainBatchFile = null;
	private static String mainBatchFormat = null;
	private static String mainBatchSeparator = null;
	private static Logger mainLogger = Logger.getLogger(MartShell.class.getName());

	/**
	 *  @return application usage instructions
	 * 
	 */
	public static String usage() {
		return "MartShell <OPTIONS>"
			+ "\n"
			+ "\n-h                             - this screen"
			+ "\n-A                                          Turn off Commandline Completion (faster startup, less helpful)"
			+ "\n-C  MART_CONFIGURATION_FILE_URL                           -  URL to Alternate Mart XML Configuration File"
			+ "\n-M  CONNECTION_CONFIGURATION_FILE_URL            - URL to mysql connection configuration file"
			+ "\n-H HOST                        - database host"
			+ "\n-P PORT                        - database port"
			+ "\n-U USER                        - database user name"
			+ "\n-p PASSWORD                    - database password"
			+ "\n-d DATABASE                    - database name"
			+ "\n-v                             - verbose logging output"
			+ "\n-l LOGGING_FILE_URL            - logging file, defaults to console if none specified"
			+ "\n-e MARTQUERY                   - a well formatted Mart Query to run in Batch Mode"
			+ "\nThe following are used in combination with the -e flag:"
			+ "\n-O OUTPUT_FILE                 - output file, default is standard out"
			+ "\n-F OUTPUT_FORMAT               - output format, either tabulated or fasta"
			+ "\n-R OUTPUT_SEPARATOR            - if OUTPUT_FORMAT is tabulated, can define a separator, defaults to tab separated"
			+ "\n\n-E QUERY_FILE_URL            - URL to file with valid Mart Query Commands"
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

			mainHost = p.getProperty("mysqlhost");
			mainPort = p.getProperty("mysqlport");
			mainDatabase = p.getProperty("mysqldbase");
			mainUser = p.getProperty("mysqluser");
			mainPassword = p.getProperty("mysqlpass");
			mainConfiguration = p.getProperty("alternateConfigurationFile");

		} catch (java.net.MalformedURLException e) {
			mainLogger.warn("Could not load connection file " + connfile + " MalformedURLException: " + e);
		} catch (java.io.IOException e) {
			mainLogger.warn("Could not load connection file " + connfile + " IOException: " + e);
		}
		confinUse = connfile;
	}

	public static void main(String[] args) {
		String loggingURL = null;
		boolean help = false;
		boolean verbose = false;
		boolean commandComp = true;

		// check for the defaultConf file, and use it, if present.  Some values may be overridden with a user specified file with -g
		if (new File(defaultConf).exists())
			getConnProperties(defaultConf);

		Getopt g = new Getopt("MartExplorerTool", args, COMMAND_LINE_SWITCHES);
		int c;

		while ((c = g.getopt()) != -1) {

			switch (c) {

				case 'h' :
					help = true;
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

				case 'P' :
					mainPort = g.getOptarg();
					break;

				case 'U' :
					mainUser = g.getOptarg();
					break;

				case 'p' :
					mainPassword = g.getOptarg();
					break;

				case 'd' :
					mainDatabase = g.getOptarg();
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
			System.out.println(usage());
			return;
		}

		MartShell ms = new MartShell();
		if (mainHost != null)
			ms.setDBHost(mainHost);
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

		if (mainBatchMode) {
			boolean validQuery = true;

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
			}
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

		String thisline = null;

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
				mcl.AddAvailableCommandsTo(QSEQUENCE, SequenceDescription.SEQS);

				// add describe
				mcl.AddAvailableCommandsTo(DESCC, describeCommands);

				mcl.SetCommandMode();

				Readline.setCompleter(mcl);
			}
			
			if (readlineLoaded && historyOn) {
				File histFile = new File( history_file );
				if ( ! histFile.exists() )
          histFile.createNewFile();
        else
          LoadScriptFromFile(history_file);          			   			
			}
			
			// load help, this loads the help, and, if completionOn, adds them to Help Mode in the MartCompleter
			LoadHelpFile();
			
		} catch (Exception e1) {
			System.out.println("Could not initialize connection: " + e1.getMessage());
			
			System.exit(1);
		}
		
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
				e.printStackTrace();
				conline = new StringBuffer();
				continueQuery = false;
				thisline = null;
			}
		}

		try {
			ExitShell();
		} catch (IOException e) {
			System.err.println("Warning, could not close Buffered Reader\n");
			e.printStackTrace();
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
				if (! line.startsWith("#") )
				  parse(line);
			}
		} catch (Exception e) {
			setBatchError(e.getMessage());
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
		} else if (martPort.length() < 1) {
			validQuery = false;
			setBatchError("Must set a Port");
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
			File batchFile = new File( batchFileName );
			if (! batchFile.exists())
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
		engine = new Engine();
    engine.setConnectionString(  "mysql", martHost, martPort, martDatabase);
    engine.setUser(martUser);
    engine.setPassword(martPass);

		if (altConfigurationFile != null)
			martconf = engine.getMartConfiguration(new URL(altConfigurationFile));
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
		  Readline.writeHistoryFile( history_file );

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

	private String Help(String command) throws InvalidQueryException {
		StringBuffer buf = new StringBuffer();

		if (command.equals(HELPC)) {
			buf.append("\nAvailable items:\n");

			for (Iterator iter = commandHelp.keySet().iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				buf.append("\t\t" + element).append("\n");
			}
			buf.append("\n");
		} else if (command.startsWith(HELPC)) {
			buf.append("\n");
			StringTokenizer hToks = new StringTokenizer(command, " ");
			hToks.nextToken(); // skip help
			String comm = hToks.nextToken();

			if (commandHelp.containsKey(comm)) {
				// if this is a help call, make sure and check for domain specific bits in the help input to substitute
				String output = commandHelp.getProperty(comm);
				for (int i = 0, n = dsCommands.length; i < n; i++) {
					Pattern pat = dsCommands[i];
					Matcher m = pat.matcher(output);
					if (m.matches())
						buf.append(m.group(1)).append(Help(m.group(2))).append(m.group(3)).append("\n");
					else
						buf.append(output).append("\n");
				}
			} else
				buf.append("Sorry, no help is available for item: ").append(comm).append("\n");
		} else {
			if (commandHelp.containsKey(command))
				buf.append(commandHelp.getProperty(command));
			else
				buf.append("Sorry, no information available for item: ").append(command).append("\n");
		}
		return buf.toString();
	}

	private void LoadHelpFile() throws InvalidQueryException {
		URL help = ClassLoader.getSystemResource(HELPFILE);
		URL dshelp = ClassLoader.getSystemResource(DSHELPFILE);
		try {
			commandHelp.load(help.openStream());
			commandHelp.load(dshelp.openStream());
		} catch (IOException e) {
			helpLoaded = false;
			throw new InvalidQueryException("Could not load Help File " + e.getMessage());
		}
		helpLoaded = true;

		if (completionOn)
			mcl.AddAvailableCommandsTo(HELPC, commandHelp.keySet());
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
		String[] toks = command.split(" ");
		String dataset = null, pagetype = null, pagename = null, groupname = null;
		System.out.println();

		if (toks.length == 1) {
			String output = "This mart contains the following datasets:\n";
			Dataset[] dsets = martconf.getDatasets();
			for (int i = 0, n = dsets.length; i < n; i++) {
				Dataset dset = dsets[i];
				output += "\t" + dset.getInternalName() + "   (" + dset.getDisplayName() + ")\n";
			}
			System.out.println(output);
		} else if (toks.length == 2) {
			dataset = toks[1].trim();
			DescribeDataset(dataset);
		} else if (toks.length == 4) {
			//describe datasetname page pagename
			dataset = toks[1].trim();
			pagetype = toks[2].trim();
			pagename = toks[3].trim();

			if (pagetype.equals(FILTERPAGE))
				DescribeFilterPage(dataset, pagename);
			else if (pagetype.equals(ATTRIBUTEPAGE))
				DescribeAttributePage(dataset, pagename);
			else
				throw new InvalidQueryException("Invalid describe command " + command + "\n");
		} else if (toks.length == 5) {
			//describe datasetname page pagename group groupname
			dataset = toks[1].trim();
			if (!martconf.containsDataset(dataset))
				throw new InvalidQueryException("Dataset " + dataset + " Not found in mart configuration for " + martconf.getInternalName() + "\n");

			Dataset dset = martconf.getDatasetByName(dataset);

			pagetype = toks[2].trim();
			pagename = toks[3].trim();
			groupname = toks[4].trim();

			if (pagetype.equals(FILTERPAGE)) {
				if (!dset.containsFilterPage(pagename))
					throw new InvalidQueryException("Dataset " + dataset + " does not contain FilterPage " + pagename + "\n");

				FilterPage page = dset.getFilterPageByName(pagename);
				if (!page.containsFilterGroup(groupname))
					throw new InvalidQueryException("Dataset " + dataset + " FilterPage " + pagename + "does not contain FilterGroup " + groupname + "\n");

				Object group = page.getFilterGroupByName(groupname);
				String[] lines = DescribeFilterGroup(group);
				for (int i = 0, n = lines.length; i < n; i++)
					System.out.println("\t" + lines[i]);
				System.out.println();
			} else if (pagetype.equals(ATTRIBUTEPAGE)) {
				if (!dset.containsAttributePage(pagename))
					throw new InvalidQueryException("Dataset " + dataset + " does not contain AttributePage " + pagename + "\n");

				AttributePage page = dset.getAttributePageByName(pagename);
				if (!page.containsAttributeGroup(groupname))
					throw new InvalidQueryException("Dataset " + dataset + " AttributePage " + pagename + "does not contain AttributeGroup " + groupname + "\n");

				Object group = page.getAttributeGroupByName(groupname);
				String[] lines = DescribeAttributeGroup(group);
				for (int i = 0, n = lines.length; i < n; i++)
					System.out.println("\t" + lines[i]);
				System.out.println();
			} else
				throw new InvalidQueryException("Recieved Invalid describe command " + command + "\n");
		} else
			throw new InvalidQueryException("Recieved invalid describe command: " + command + "\n");
	}

	private void DescribeDataset(String datasetName) throws InvalidQueryException {
		if (!martconf.containsDataset(datasetName))
			throw new InvalidQueryException("Dataset " + datasetName + " Not found in mart configuration for " + martconf.getInternalName() + "\n");

		Dataset dset = martconf.getDatasetByName(datasetName);
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

			String[] lines = ColumnIze(page.getInternalName() + " - " + page.getDisplayName());
			for (int j = 0, k = lines.length; j < k; j++)
				System.out.print("\t" + lines[j] + "\n");

			System.out.print("\n\tFilterGroups:\n");

			List groups = page.getFilterGroups();
			for (int j = 0, k = groups.size(); j < k; j++) {
				Object groupo = groups.get(j);
				if (groupo instanceof FilterGroup) {
					FilterGroup group = (FilterGroup) groupo;

					String[] glines = ColumnIze(group.getInternalName() + " - " + group.getDisplayName());
					for (int y = 0, g = glines.length; y < g; y++)
						System.out.print("\t\t" + glines[y] + "\n");
				} else {
					// do nothing, but could do something special for DSFilterGroups.  add hooks here
				}
			}
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

			String[] alines = ColumnIze(page.getInternalName() + " - " + page.getDisplayName());
			for (int j = 0, k = alines.length; j < k; j++)
				System.out.print("\t" + alines[j] + "\n");

			System.out.print("\n\tAttributeGroups:\n");

			List agroups = page.getAttributeGroups();
			for (int j = 0, k = agroups.size(); j < k; j++) {
				if (j > 0)
					System.out.println();

				Object groupo = agroups.get(j);
				if (groupo instanceof AttributeGroup) {
					AttributeGroup group = (AttributeGroup) groupo;
					String[] glines = ColumnIze(group.getInternalName() + " - " + group.getDisplayName() + " " + group.getDescription());
					for (int l = 0, m = glines.length; l < m; l++)
						System.out.print("\t\t" + glines[l] + "\n");
				} else {
					DSAttributeGroup group = (DSAttributeGroup) groupo;
					String[] glines = ColumnIze(group.getInternalName() + " - " + group.getDisplayName() + " " + group.getDescription());
					for (int l = 0, m = glines.length; l < m; l++)
						System.out.print("\t\t" + glines[l] + "\n");
				}
			}
		}
		System.out.println(DASHES);
	}

	private void DescribeFilterPage(String datasetName, String pageName) throws InvalidQueryException {
		if (!martconf.containsDataset(datasetName))
			throw new InvalidQueryException("Dataset " + datasetName + " Not found in mart configuration for " + martconf.getInternalName() + "\n");

		Dataset dset = martconf.getDatasetByName(datasetName);
		if (!dset.containsFilterPage(pageName))
			throw new InvalidQueryException("Dataset " + datasetName + " does not contain FilterPage " + pageName + "\n");

		FilterPage page = dset.getFilterPageByName(pageName);
		System.out.print("Dataset: " + dset.getInternalName() + " - FilterPage: " + pageName + " contains the following FilterGroups\n");

		List groups = page.getFilterGroups();
		for (int i = 0, n = groups.size(); i < n; i++) {
			if (i > 0) {
				try {
					String quit = Readline.readline("\nHit Enter to continue with next group, q to return to prompt: ", false);
					if (quit.equals("q"))
						break;

				} catch (Exception e) {
					// do nothing
				}
			}

			for (int j = 0; j < MAXCHARCOUNT; j++)
				System.out.print("-");
			System.out.println();

			Object groupo = groups.get(i);
			if (groupo instanceof FilterGroup) {
				FilterGroup group = (FilterGroup) groupo;
				String[] lines = DescribeFilterGroup(group);
				for (int j = 0, n2 = lines.length; j < n2; j++) {
					String string = lines[j];
					System.out.print("\t" + string + "\n");
				}
			}
		}
	}

	private void DescribeAttributePage(String datasetName, String pageName) throws InvalidQueryException {
		if (!martconf.containsDataset(datasetName))
			throw new InvalidQueryException("Dataset " + datasetName + " Not found in mart configuration for " + martconf.getInternalName() + "\n");

		Dataset dset = martconf.getDatasetByName(datasetName);
		if (!dset.containsAttributePage(pageName))
			throw new InvalidQueryException("Dataset " + datasetName + " does not contain AttributePage " + pageName + "\n");

		AttributePage page = dset.getAttributePageByName(pageName);
		System.out.print("Dataset: " + dset.getInternalName() + " - AttributePage: " + pageName + " contains the following AttributeGroups\n");

		List groups = page.getAttributeGroups();
		for (int i = 0, n = groups.size(); i < n; i++) {
			if (i > 0) {
				try {
					String quit = Readline.readline("\nHit Enter to continue with next group, q to return to prompt: ", false);
					if (quit.equals("q"))
						break;

				} catch (Exception e) {
					// do nothing
				}
			}

			for (int j = 0; j < MAXCHARCOUNT; j++)
				System.out.print("-");

			System.out.println();

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
			lines.add("FilterGroup: " + group.getInternalName() + " - " + group.getDisplayName());
			lines.add(DASHES);

			if (group.hasFilterSets()) {
				lines.add("Contains FilterSets:");
				FilterSet[] fsets = group.getFilterSets();
				for (int i = 0, n = fsets.length; i < n; i++) {
					if (i > 0)
						lines.add("");

					FilterSet set = fsets[i];
					String thisSetInfo = "\tFilterSet: " + set.getInternalName();
					if (set.getDisplayName().length() > 0)
						thisSetInfo += " - " + set.getDisplayName();
					lines.add(thisSetInfo);
					lines.add("\tContains the following FilterSetDescriptions:");

					FilterSetDescription[] fsds = set.getFilterSetDescriptions();
					for (int j = 0, n2 = fsds.length; j < n2; j++) {
						FilterSetDescription fsd = fsds[j];
						lines.add("\t\t" + fsd.getInternalName() + " - " + fsd.getDisplayName());
					}
				}
				lines.add("");
			}

			FilterCollection[] fcs = group.getFilterCollections();
			lines.add(DASHES);

			for (int i = 0, n = fcs.length; i < n; i++) {
				if (i > 0)
					lines.add(DASHES);

				FilterCollection collection = fcs[i];
				lines.add("\tCollection: " + collection.getDisplayName());
				lines.add("");

				if (collection.inFilterSet()) {
					String[] clines =
						ColumnIze(
							"Filters from this collection must be qualified with the internalName of one of the FilterSetsDescriptions from FilterSet '"
								+ collection.getFilterSetName()
								+ "' above");
					for (int j = 0, k = clines.length; j < k; j++)
						lines.add("\t" + clines[j]);

					lines.add("");
				}

				List fdescs = collection.getUIFilterDescriptions();
				for (int j = 0, n2 = fdescs.size(); j < n2; j++) {
					Object desco = fdescs.get(j);
					if (desco instanceof UIFilterDescription) {
						UIFilterDescription desc = (UIFilterDescription) desco;
						lines.add("\t\t" + desc.getInternalName() + " - " + desc.getDisplayName() + " (Type " + desc.getType() + ")");
					} else {
						UIDSFilterDescription desc = (UIDSFilterDescription) desco;
						String disp = "\t\t" + desc.getInternalName();
						if (desc.getDisplayName().length() > 0)
							disp += " - " + desc.getDisplayName();
						disp += " (see 'help " + desc.getObjectCode() + "' for further information)";
						lines.add(disp);
					}
				}
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
				lines.add("AttributeGroup: " + group.getInternalName() + " - " + group.getDisplayName());
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
			lines.add("AttributeGroup: " + group.getInternalName() + " - " + group.getDisplayName());

			AttributeCollection[] attcs = group.getAttributeCollections();
			for (int i = 0, n = attcs.length; i < n; i++) {
				if (i > 0)
					lines.add("");

				AttributeCollection collection = attcs[i];
				if (collection.getDisplayName() != null) {
					lines.add("\t" + collection.getDisplayName());
					lines.add("");
				}

				if (collection.getMaxSelect() > 0) {
					lines.add("\tOnly " + collection.getMaxSelect() + " of the following attributes can be selected in the same query");
					lines.add("");
				}

				List adescs = collection.getUIAttributeDescriptions();
				for (int j = 0, k = adescs.size(); i < k; i++) {
					if (j > 0)
						lines.add("");

					Object desco = adescs.get(i);
					if (desco instanceof UIAttributeDescription) {
						UIAttributeDescription desc = (UIAttributeDescription) desco;
						lines.add("\t\t" + desc.getInternalName() + " - " + desc.getDisplayName());
					} else {
						// for now, do nothing.  If we add UIDSAttributeDescriptions to the config, add hooks here
					}
				}
			}
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

				if (key.equals(MYSQLHOST))
					martHost = value;
				else if (key.equals(MYSQLPORT))
					martPort = value;
				else if (key.equals(MYSQLUSER))
					martUser = value;
				else if (key.equals(MYSQLPASS))
					martPass = value;
				else if (key.equals(MYSQLBASE))
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
        	for(int i = 0, n = martPass.length(); i < n; i++)
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
						"\nPlease enter the URL for the XML Configuration File for the new mart (press enter to leave as '" + myAltFile + "',\n enter '-' to use configuration provided by "
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
				outPutFile = new File( outPutFileName );
			} else if (tokCount == 3) {
				req = com.nextToken();
				outPutFileName = com.nextToken();
				outPutFile = new File( outPutFileName );
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
			OutputStreamWriter hisout = new OutputStreamWriter( new FileOutputStream( outPutFile ) );
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
			scriptFile = new File( scriptFileName );
			ExecScriptFromFile(scriptFile);
		} catch (Exception e) {
			throw new InvalidQueryException("Could not execute script: " + scriptFileName + " " + e.getMessage());
		}
	}

	private void ExecScriptFromFile(File scriptFile) throws InvalidQueryException {
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream( scriptFile ) ) );

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
			if (completionOn)
				mcl.SetCommandMode();
				
			parseCommand(command);
		} else if (line.endsWith(LINEEND)) {
			String command = conline.append(" " + line).toString().trim();
			continueQuery = false;
			conline = new StringBuffer();

			if (completionOn)
				mcl.SetCommandMode();

			parseCommand(command);
		} else {
			conline.append(" " + line);
			continueQuery = true;

			//MartCompleter Mode
			if (completionOn) {
				if (line.indexOf(QSTART) >= 0)
					mcl.SetSelectMode();
				else if (line.indexOf(QFROM) >= 0)
					mcl.SetFromMode();
				else if (line.indexOf(QWHERE) >= 0)
					mcl.SetWhereMode();
				//else not needed
			}
		}
	}

	private void parseCommand(String command) throws SequenceException, FormatException, InvalidQueryException, IOException, SQLException {
		int cLen = command.length();

		command = command.replaceAll("\\s;$", ";");

		if (cLen == 0)
			return;
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
		else if (command.startsWith(QSTART)) {
			Query query = msl.MQLtoQuery(command);
			
			OutputStream os = null;
			if (sessionOutputFile != null)
				os = sessionOutputFile;
		  else
		    os = System.out;

			FormatSpec fspec = null;
			
			if (sessionOutputFormat != null) {
				if ( sessionOutputFormat.equals("fasta") )
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

	private final Pattern[] dsCommands = new Pattern[] { Pattern.compile("(.+)DOMAINSPECIFIC\\s(\\w+)(.+)", Pattern.DOTALL)};

	private final List dsHelpItems = Collections.unmodifiableList(Arrays.asList(new String[] { "selectSequence" }));

	// available commands
	private final String EXITC = "exit";
	private final String QUITC = "quit";
	private final String HELPC = "help";
	private final String DESCC = "describe";
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
	private final String QSTART = "select";

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
					QSTART }));

	// describe instructions
	private final String FILTERPAGE = "FilterPage";
	private final String ATTRIBUTEPAGE = "AttributePage";

	private final List describeCommands = Collections.unmodifiableList(Arrays.asList(new String[] { FILTERPAGE, ATTRIBUTEPAGE }));

	// strings used to show/set output format settings
	private final String FILE = "file";
	private final String FORMAT = "format";
	private final String SEPARATOR = "separator";

	// strings used to show/set mart connection settings
	private final String MYSQLHOST = "mysqlhost";
	private final String MYSQLUSER = "mysqluser";
	private final String MYSQLPASS = "mysqlpass";
	private final String MYSQLPORT = "mysqlport";
	private final String MYSQLBASE = "mysqldbase";
	private final String ALTCONFFILE = "alternateConfigurationFile";

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
	private final String QFROM = "from";
	private final String QWHERE = "where";
	private final String LINEEND = ";";
	private final String QSEQUENCE = "sequence";
}
