package org.ensembl.mart.shell;

import gnu.getopt.Getopt;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.DomainSpecificFilter;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.NullableFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.UIAttributeDescription;
import org.ensembl.mart.lib.config.UIDSFilterDescription;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * Interface to a Mart Database implimentation that provides commandline access using a SQL-like query language (referred to below as the Mart Query Language).
 * The system can be used to run script files containing valid Mart Query Language commands, or individual queries from the commandline.
 * It has an interactive shell as well.  The interactive shell makes use of the <a href="http://java-readline.sourceforge.net/">Java Readline Library</a>
 * to allow commandline editing, history, and tab completion for those users working on Linux/Unix operating systems.  Unfortunately, there is no way
 * to provide this functionality in a portable way across OS platforms.  For windows users, there is a Getline c library which is provided with the Java Readline source.
 * By following the instructions to build a windows version of this library, you can overwrite the libreadline-java.jar and libJavaGetline.so file in the 
 * mart-explorer/lib directory with your own versions, and you will gain some (but not all) of this functionality.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartShell {

	// main variables
	private static final String defaultConf = System.getProperty("user.home") + "/.martexplorer";
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
	private static String mainBatchScriptURL = null;
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
			+ "\n\nThe application searches for a .martexplorer file in the user home directory for mysql connection configuration"
			+ "\nif present, this file will be loaded. If the -g option is given, or any of the commandline connection"
			+ "\nparameters are passed, these over-ride those values provided in the .martexplorer file"
			+ "\nUsers specifying a mysql connection configuration file with -g,"
			+ "\nor using a .martexplorer file, can use -H, -P, -p, -u, or -d to specify"
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
					mainBatchScriptURL = g.getOptarg();
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

			if (mainBatchSQL == null && mainBatchScriptURL == null) {
				System.out.println("Must supply either a Query command or a query script\n" + usage());
				System.exit(0);
			} else if (mainBatchScriptURL != null) {
				validQuery = ms.runBatchScript(mainBatchScriptURL);
			} else {
				if (mainBatchFile != null)
					ms.setBatchOutputFile(mainBatchFile);
				if (mainBatchFormat != null)
					ms.setBatchOutputFormat(mainBatchFormat);
				if (mainBatchSeparator == null)
					ms.setBatchOutputSeparator("\t"); //default
				else
					ms.setBatchOutputSeparator(mainBatchSeparator);

				validQuery = ms.runBatch(mainBatchSQL);
			}
			if (!validQuery) {
				System.out.println("Invalid Batch c:" + ms.getBatchError() + "\n" + usage());
				System.exit(0);
			}
		} else
		  if (! commandComp)
		    ms.UnsetCommandCompletion();
			ms.runInteractive();
	}

	public MartShell() {
	}

	public void runInteractive() {
		try {
			Readline.load(ReadlineLibrary.Getline);
//		Getline doesnt support completion, or history manipulation/files
			completionOn = false;
			historyOn = false;
			readlineLoaded = true;
		}
		catch (UnsatisfiedLinkError ignore_me) {
			try {
				Readline.load(ReadlineLibrary.GnuReadline);
				historyOn = true;
				readlineLoaded = true;
			}
			catch (UnsatisfiedLinkError ignore_me2) {
				mainLogger.warn("Could not load Readline Library, commandline editing, completion will not be available"
				+ "\nConsult MartShell documentation for methods to resolve this error.");
				readlineLoaded = false;
				Readline.load(ReadlineLibrary.PureJava);
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
				setConnectionSettings(setConnectionSettings);
			initEngine();
			if (completionOn)
				Readline.setCompleter(new MartCompleter(martconf));
		} catch (Exception e1) {
			System.out.println("Could not initialize connection: " + e1.getMessage());
			e1.printStackTrace();
			System.exit(1);
		}

		while (true) {
			try {
				thisline = Prompt();
				if (thisline.equals(exit) || thisline.equals(quit))
					break;

				if (thisline != null) {
					parse(thisline);
					thisline = null;
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
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

	public boolean runBatchScript(String batchScriptURL) {
		boolean valid = true;
		try {
			initEngine();
			reader = new BufferedReader(new InputStreamReader(new URL(batchScriptURL).openStream()));

			for (String line = reader.readLine(); line != null; line = reader.readLine())
				parse(line);
		} catch (Exception e) {
			setBatchError(e.getMessage());
			valid = false;
		}
		return valid;
	}

	public boolean runBatch(String querystring) {
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
				initEngine();
				if (!querystring.endsWith(lineEnd))
					querystring = querystring + lineEnd;

				parseCommand(querystring);
			} catch (Exception e) {
				setBatchError(e.getMessage());
				validQuery = false;
			}
		}
		return validQuery;
	}

  public void UnsetCommandCompletion() {
		completionOn = false;
  }
  
	public void setAlternateMartConfiguration(String confFile) {
		altConfigurationFileURL = confFile;
	}
	
	public void setDBHost(String dbhost) {
		this.martHost = dbhost;
	}

	public void setDBPort(String dbport) {
		this.martPort = dbport;
	}

	public void setDBUser(String dbuser) {
		this.martUser = dbuser;
	}

	public void setDBPass(String dbpass) {
		martPass = dbpass;
	}

	public void setDBDatabase(String db) {
		martDatabase = db;
	}

	public void setBatchOutputFile(String batchFile) {
		outputFile = batchFile;
	}

	public void setBatchOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setBatchOutputSeparator(String outputSeparator) {
		this.outputSeparator = outputSeparator;
	}

	public String getBatchError() {
		return batchErrorMessage;
	}

	private void setBatchError(String message) {
		batchErrorMessage = message;
	}

	private void initEngine() throws MalformedURLException, ConfigurationException {
		engine = new Engine(martHost, martPort, martUser, martPass, martDatabase);

		if (altConfigurationFileURL != null)
			martconf = engine.getMartConfiguration(new URL(altConfigurationFileURL));
		else
			martconf = engine.getMartConfiguration();
	}

	private void parse(String line) throws IOException, InvalidQueryException {
		if (line.equals(help)) {
			if (continueQuery)
				System.out.println("help not applicable in the middle of a query");
			else
				help(null);
		} else if (line.equals(lineEnd)) {
			String command = conline.append(line).toString().trim();
			parseCommand(command);
			continueQuery = false;
			conline = new StringBuffer();
		} else if (line.endsWith(lineEnd)) {
			String command = conline.append(" " + line).toString().trim();
			parseCommand(command);
			continueQuery = false;
			conline = new StringBuffer();
		} else {
			conline.append(" " + line);
			continueQuery = true;
		}
	}

	private void ExitShell() throws IOException {
		Readline.cleanup();
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
    
		if (martUser != null && martDatabase != null)
			prompt = martUser + "@" + martHost + " : " + martDatabase + "> ";
		else
			prompt = "> ";

		line = Readline.readline(prompt, historyOn);
		return line;
	}

	private String subPrompt() throws EOFException, UnsupportedEncodingException, IOException {
		return Readline.readline("% ", historyOn);
	}

	private void help(String command) {
		if (command != null)
			System.out.println("Recieved help request: " + command);
		else
			System.out.println("Recieved help request");
	}

	private void listRequest(String command) {
		if (command != null)
			System.out.println("Recieved list request: " + command);
		else
			System.out.println("Recieved list request");
	}

	private void setOutputFormat(String command) throws InvalidQueryException {
		if (command.endsWith(lineEnd))
			command = command.substring(0, command.length() - 1);

		StringTokenizer ctokens = new StringTokenizer(command, " ");
		ctokens.nextToken();
		String fSettings = ctokens.nextToken();

		StringTokenizer fTokens = new StringTokenizer(fSettings, ",");
		while (fTokens.hasMoreTokens()) {
			StringTokenizer tokens = new StringTokenizer(fTokens.nextToken(), "=");
			if (tokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Recieved invalid setOutputFormat request: "
						+ command
						+ "\nmust be of format: setOutputFormat x=y(,x=y)* where x  can be one of : "
						+ file
						+ "(note, use '-' for stdout, specify a valid URL), "
						+ format
						+ " (note, use 'comma' for comma separated), "
						+ separator
						+ "\n");

			String key = tokens.nextToken();
			String value = tokens.nextToken();

			if (key.equals(file)) {
				if (value.equals("-"))
					outputFile = null;
				else
					outputFile = value;
			} else if (key.equals(format))
				outputFormat = value;
			else if (key.equals(separator))
				if (key.equals("comma"))
					outputSeparator = ",";
				else
					outputSeparator = value;
			else
				throw new InvalidQueryException(
					"Recieved invalid setOutputFormat request: "
						+ command
						+ "\nmust be of format: setOutputFormat x=y(,x=y)* where x  can be one of : "
						+ file
						+ " (note, use '-' for stdout, specify a valid URL), "
						+ format
						+ " (note, use 'comma' for comma separated), "
						+ separator
						+ "\n");
		}
	}

	private void showOutputSettings() {
		String thisFile = "stdout";
		if (outputFile != null)
			thisFile = outputFile;

		System.out.println(
			"Output Format: " + format + " = " + outputFormat + ", " + separator + " = " + "'" + outputSeparator + "'" + ", " + file + " = " + thisFile);
	}

	private void setConnectionSettings(String command) throws InvalidQueryException {
		if (command.endsWith(lineEnd))
			command = command.substring(0, command.length() - 1);

		StringTokenizer ctokens = new StringTokenizer(command, " ");
		if (ctokens.countTokens() > 1) {
			// parse command
			ctokens.nextToken(); // throw away
			String connSettings = ctokens.nextToken();

			StringTokenizer sTokens = new StringTokenizer(connSettings, ",");
			while (sTokens.hasMoreTokens()) {
				StringTokenizer tokens = new StringTokenizer(sTokens.nextToken(), "=");
				if (tokens.countTokens() < 2)
					throw new InvalidQueryException(martConnectionUsage(command));

				String key = tokens.nextToken();
				String value = tokens.nextToken();

				if (key.equals(mysqlhost))
					martHost = value;
				else if (key.equals(mysqlport))
					martPort = value;
				else if (key.equals(mysqluser))
					martUser = value;
				else if (key.equals(mysqlpass))
					martPass = value;
				else if (key.equals(mysqldbase))
					martDatabase = value;
				else if (key.equals(alternateConfigurationFile))
					altConfigurationFileURL = value;
				else
					throw new InvalidQueryException(martConnectionUsage(command));
			}
		} else {
			if (mainBatchMode)
				throw new InvalidQueryException(martConnectionUsage(command));

			String thisLine = null;

			try {
				thisLine = Readline.readline("\nPlease enter the host address of the mart database (press enter to leave unchaged): ", false);
				if (thisLine != null)
					martHost = thisLine;

				thisLine = Readline.readline("\nPlease enter the port on which the mart database is running (press enter to leave unchaged): ", false);
				if (thisLine != null)
					martPort = thisLine;

				thisLine = Readline.readline("\nPlease enter the user name used to connect to the mart database (press enter to leave unchaged): ", false);
				if (thisLine != null)
					martUser = thisLine;

				thisLine = Readline.readline("\nPlease enter the password used to connect to the mart database (press enter to leave unchaged): ", false);
				if (thisLine != null)
					martPass = thisLine;

				thisLine = Readline.readline("\nPlease enter the name of the mart database you wish to query (press enter to leave unchaged): ", false);
				if (thisLine != null)
					martDatabase = thisLine;

				thisLine = Readline.readline("\nPlease enter the URL for the XML Configuration File for the new mart (press enter to leave unchaged,\n enter '-' to use configuration provided by "
				+ martDatabase
				+ "):", false);
				if (thisLine != null) {
					if (thisLine.equals("-"))
						altConfigurationFileURL = null;
					else
						altConfigurationFileURL = thisLine;
				}

			} catch (Exception e) {
				throw new InvalidQueryException("Problem reading input for mart connection settings: " + e.getMessage());
			}
		}
		try {
			initEngine();
		} catch (Exception e) {
			throw new InvalidQueryException("Could not initialize connection: " + e.getMessage());
		}
	}

	private void showConnectionSettings() {
		String conf = martDatabase;
		if (altConfigurationFileURL != null)
			conf = altConfigurationFileURL;

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

	private String martConnectionUsage(String command) {
		return "Recieved invalid setMart request: "
			+ command
			+ "\nmust be of format: setMart x=y(,x=y)* where x can be one of: "
			+ mysqlhost
			+ ", "
			+ mysqlport
			+ ", "
			+ mysqluser
			+ ", "
			+ mysqlpass
			+ ", "
			+ mysqldbase
			+ ", "
			+ alternateConfigurationFile
			+ "\n";
	}

	private void setVerbose(String command) throws InvalidQueryException {
		if (command.endsWith(lineEnd))
			command = command.substring(0, command.length() - 1);

		if (!command.matches("\\w+\\s(true|false)"))
			throw new InvalidQueryException("Invalid setVerbose command recieved: " + command + "\n");

		boolean verbose = Boolean.valueOf(command.split("\\s")[1]).booleanValue();
		String val = (verbose) ? "on" : "off";
		System.out.println("Logging now " + val + "\n");
		defaultLoggingConfiguration(verbose);
	}

	private void parseCommand(String command) throws IOException, InvalidQueryException {
		int cLen = command.length();

		if (cLen == 0) {
			return;
		} else if (command.startsWith(qStart)) {
			parseQuery(command);
		} else if (command.startsWith(setConnectionSettings))
			setConnectionSettings(command);
		else if (command.startsWith(showConnectionSettings))
			showConnectionSettings();
		else if (command.startsWith(list))
			listRequest(command);
		else if (command.startsWith(setOutputSettings))
			setOutputFormat(command);
		else if (command.startsWith(showOutputSettings))
			showOutputSettings();
		else if (command.startsWith(setVerbose))
			setVerbose(command);
		else if (command.equals(exit) || command.equals(quit))
			ExitShell();
		else {
			throw new InvalidQueryException("\nInvalid Command: please try again " + command + "\n");
		}
	}

	private void parseQuery(String command) throws IOException, InvalidQueryException {
		boolean start = true;
		boolean selectClause = false;
		boolean withClause = false;
		boolean fromClause = false;
		boolean whereClause = false;
		boolean limitClause = false;
		boolean intoClause = false;
		int listLevel = 0; // level of subquery/list

		StringBuffer attString = new StringBuffer();
		StringBuffer withString = new StringBuffer();
		String dataset = null;
		StringBuffer whereString = new StringBuffer();
		String outformat = null;
		int limit = 0;

		StringTokenizer cTokens = new StringTokenizer(command, " ");

		if (cTokens.countTokens() < 2)
			throw new InvalidQueryException("\nInvalid Query Recieved " + command + "\n");

		while (cTokens.hasMoreTokens()) {
			String thisToken = cTokens.nextToken();
			if (start) {
				if (!(thisToken.equalsIgnoreCase(qStart)))
					throw new InvalidQueryException("Invalid Query Recieved, should begin with select: " + command + "\n");
				else {
					start = false;
					selectClause = true;
				}
			} else if (selectClause) {
				if (thisToken.equalsIgnoreCase(qStart))
					throw new InvalidQueryException("Invalid Query Recieved, select statement in the middle of a select statement: " + command + "\n");
				if (thisToken.equalsIgnoreCase(qWhere))
					throw new InvalidQueryException("Invalid Query Recieved, where statement before from statement: " + command + "\n");
				if (thisToken.equalsIgnoreCase(qInto))
					throw new InvalidQueryException("Invalid Query Recieved, into statement before from statement: " + command + "\n");
				if (thisToken.equalsIgnoreCase(qLimit))
					throw new InvalidQueryException("Invalid Query Recieved, limit statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWith)) {
					selectClause = false;
					withClause = true;
				} else if (thisToken.equalsIgnoreCase(qFrom)) {
					selectClause = false;
					fromClause = true;
				} else if (!thisToken.equalsIgnoreCase("nothing"))
					attString.append(thisToken);
			} else if (withClause) {
				if (thisToken.equalsIgnoreCase(qStart))
					throw new InvalidQueryException("Invalid Query Recieved, select statement in the middle of an into statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWhere))
					throw new InvalidQueryException("Invalid Query Recieved, where statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qInto))
					throw new InvalidQueryException("Invalid Query Recieved, into statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qLimit))
					throw new InvalidQueryException("Invalid Query Recieved, limit statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qFrom)) {
					withClause = false;
					fromClause = true;
				} else
					withString.append(thisToken);
			} else if (fromClause) {
				if (thisToken.equalsIgnoreCase(qStart))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWith))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWhere)) {
					fromClause = false;
					whereClause = true;
				} else if (thisToken.equalsIgnoreCase(qInto)) {
					fromClause = false;
					intoClause = true;
				} else if (thisToken.equalsIgnoreCase(qLimit)) {
					fromClause = false;
					limitClause = true;
				} else {
					if (dataset != null)
						throw new InvalidQueryException("Invalid Query Recieved, dataset already set, attempted to set again: " + command + "\n");
					else
						dataset = thisToken;
				}
			} else if (whereClause) {
				if (listLevel < 1 && thisToken.equalsIgnoreCase(qWith))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after where statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(lStart) || thisToken.startsWith(lStart)) {
					listLevel++;

					if (thisToken.endsWith(lEnd) || thisToken.endsWith(lEnd + ",") || thisToken.endsWith(lEnd + lineEnd)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == lEndChar)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(lineEnd) || thisToken.endsWith(lineEnd)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where statement: " + command + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (listLevel > 0) {
					if (thisToken.equalsIgnoreCase(lEnd)) {
						listLevel--;
					} else if (thisToken.endsWith(lEnd) || thisToken.endsWith(lEnd + ",") || thisToken.endsWith(lEnd + lineEnd)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == lEndChar)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(lineEnd) || thisToken.endsWith(lineEnd)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where statement: " + command + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (thisToken.equalsIgnoreCase(qStart))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after where statement, not in subquery: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qFrom))
					throw new InvalidQueryException("Invalid Query Recieved, from statement after where statement, not in subquery: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWhere))
					throw new InvalidQueryException("Invalid Query Recieved, where statement after where statement, not in subquery: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qInto)) {
					whereClause = false;
					intoClause = true;
				} else if (thisToken.equalsIgnoreCase(qLimit)) {
					whereClause = false;
					limitClause = true;
				} else
					whereString.append(" ").append(thisToken);
			} else if (intoClause) {
				if (thisToken.equalsIgnoreCase(qStart))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after into statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWith))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after into statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qFrom))
					throw new InvalidQueryException("Invalid Query Recieved, from statement into where statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWhere))
					throw new InvalidQueryException("Invalid Query Recieved, where statement into where statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qLimit)) {
					intoClause = false;
					limitClause = true;
				} else {
					if (thisToken.endsWith(lineEnd))
						thisToken = thisToken.substring(0, thisToken.length() - 1);
					outformat = thisToken;
				}
			} else if (limitClause) {
				if (thisToken.equalsIgnoreCase(qStart))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWith))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qFrom))
					throw new InvalidQueryException("Invalid Query Recieved, from statement into limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qWhere))
					throw new InvalidQueryException("Invalid Query Recieved, where statement into limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(qInto))
					throw new InvalidQueryException("Invalid Query Recieved, into statement into limit statement: " + command + "\n");
				else {
					if (limit > 0)
						throw new InvalidQueryException("Invalid Query Recieved, attempt to set limit twice: " + command + "\n");
					else {
						if (thisToken.endsWith(lineEnd))
							thisToken = thisToken.substring(0, thisToken.length() - 1);
						limit = Integer.parseInt(thisToken);
					}
				}
			}
			// else not needed, as these are the only states present
		}

		if (dataset == null)
			throw new InvalidQueryException("Invalid Query Recieved, did not set dataset: " + command + "\n");

		if (attString.length() == 0 && withString.length() == 0)
			throw new InvalidQueryException("Invalid Query Recieved, no attributes or sequence request found: " + command + "\n");

		if (dataset.endsWith(lineEnd))
			dataset = dataset.substring(0, dataset.length() - 1);

		if (!martconf.containsDataset(dataset))
			throw new InvalidQueryException("Dataset " + dataset + " is not found in this mart\n");

		Dataset dset = martconf.getDatasetByName(dataset);
		Query query = new Query();
		FilterPage currentFpage = null;
		AttributePage currentApage = null;

		query.setStarBases(dset.getStarBases());
		query.setPrimaryKeys(dset.getPrimaryKeys());

		if (withString.length() > 0) {
			String seqrequest = withString.toString().trim();

			int typecode = 0;
			int left = 0;
			int right = 0;

			StringTokenizer tokens = new StringTokenizer(seqrequest, seqdelimiter, true);
			int n = tokens.countTokens();
			switch (n) {
				case 5 :
					// left+type+right
					left = Integer.parseInt(tokens.nextToken());
					tokens.nextToken(); // skip plus
					typecode = SequenceDescription.SEQS.indexOf(tokens.nextToken());
					tokens.nextToken();
					right = Integer.parseInt(tokens.nextToken());
					break;
				case 3 :
					// left+type || type+right
					String tmpl = tokens.nextToken();
					tokens.nextToken();
					String tmpr = tokens.nextToken();

					if (SequenceDescription.SEQS.contains(tmpl)) {
						typecode = SequenceDescription.SEQS.indexOf(tmpl);
						right = Integer.parseInt(tmpr);
					} else if (SequenceDescription.SEQS.contains(tmpr)) {
						left = Integer.parseInt(tmpl);
						typecode = SequenceDescription.SEQS.indexOf(tmpr);
					} else {
						throw new InvalidQueryException("Invalid sequence request recieved: " + seqrequest + "\n");
					}
					break;
				case 1 :
					// type
					typecode = SequenceDescription.SEQS.indexOf(seqrequest);
					break;
			}
			currentApage = dset.getAttributePageByName("sequences");
			query.setSequenceDescription(new SequenceDescription(typecode, left, right));
		}

		//parse attributes, if present
		if (attString.length() > 1) {
			List atts = new ArrayList();
			StringTokenizer attTokens = new StringTokenizer(attString.toString(), ",");

			while (attTokens.hasMoreTokens()) {
				String attname = attTokens.nextToken().trim(); // remove leading and trailing whitespace
				if (!dset.containsUIAttributeDescription(attname))
					throw new InvalidQueryException("Attribute " + attname + " is not found in this mart for dataset " + dataset + "\n");

				if (currentApage == null) {
					currentApage = dset.getPageForUIAttributeDescription(attname);
					atts.add(dset.getUIAttributeDescriptionByName(attname));
				} else {
					if (!currentApage.containsUIAttributeDescription(attname)) {
						if (currentApage.getInternalName().equals("sequences"))
							throw new InvalidQueryException("Cannot request attribute " + attname + " with a sequence request\n");

						currentApage = dset.getPageForUIAttributeDescription(attname);

						for (int i = 0, n = atts.size(); i < n; i++) {
							UIAttributeDescription element = (UIAttributeDescription) atts.get(i);

							if (!currentApage.containsUIAttributeDescription(element.getInternalName()))
								throw new InvalidQueryException(
									"Cannot request attributes from different Attribute Pages " + attname + " in " + currentApage + " intName is not\n");
						}
					}
					atts.add(dset.getUIAttributeDescriptionByName(attname));
				}
			}

			for (int i = 0, n = atts.size(); i < n; i++) {
				UIAttributeDescription attd = (UIAttributeDescription) atts.get(i);
				Attribute attr = new FieldAttribute(attd.getFieldName(), attd.getTableConstraint());
				query.addAttribute(attr);
			}
		}

		//parse filters, if present
		List filts = new ArrayList();

		if (whereString.length() > 0) {
			if (whereString.toString().endsWith(lineEnd))
				whereString.deleteCharAt(whereString.length() - 1);

			List filtNames = new ArrayList();
			String filterName = null;
			String cond = null;
			String val = null;
			String filterSetName = null;
			FilterSetDescription fset = null;

			start = true;
			listLevel = 0;

			boolean condition = false;
			boolean value = false;

			boolean isList = false;
			boolean isNested = false;

			List idlist = null; // will hold ids from a list
			StringBuffer subquery = null; // will build up a subquery

			StringTokenizer wTokens = new StringTokenizer(whereString.toString(), " ");

			while (wTokens.hasMoreTokens()) {
				String thisToken = wTokens.nextToken().trim();

				if (start) {
					//reset all values
					filterName = null;
					cond = null;
					val = null;
					filterSetName = null;
					fset = null;
					idlist = new ArrayList();
					subquery = new StringBuffer();
					isNested = false;
					isList = false;

					if (thisToken.indexOf(".") > 0) {
						StringTokenizer dtokens = new StringTokenizer(thisToken, ".");
						if (dtokens.countTokens() < 2)
							throw new InvalidQueryException("Invalid FilterSet Request, must be filtersetname.filtername: " + thisToken + "\n");
						filterSetName = dtokens.nextToken();
						filterName = dtokens.nextToken();
					} else
						filterName = thisToken;

					if (!dset.containsUIFilterDescription(filterName))
						throw new InvalidQueryException("Filter " + filterName + " not supported by mart dataset " + dataset + "\n");
					else {
						if (currentFpage == null)
							currentFpage = dset.getPageForUIFilterDescription(filterName);
						else {
							if (!currentFpage.containsUIFilterDescription(filterName)) {
								currentFpage = dset.getPageForUIFilterDescription(filterName);

								for (int i = 0, n = filtNames.size(); i < n; i++) {
									String element = (String) filtNames.get(i);
									if (!currentFpage.containsUIFilterDescription(element))
										throw new InvalidQueryException(
											"Cannot use filters from different FilterPages: filter " + filterName + " in page " + currentFpage + "filter " + element + "is not\n");
								}
							}
						}

						if (filterSetName != null) {
							if (!currentFpage.containsFilterSetDescription(filterSetName))
								throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
							else
								fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
						}
					}

					start = false;
					condition = true;
				} else if (condition) {
					if ( ! wTokens.hasMoreTokens() ) {
						if (! ( thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded") ) )
						  throw new InvalidQueryException("Invalid Query Recieved, filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
					}
					
					if (thisToken.endsWith(",")) {
						thisToken = thisToken.substring(0, thisToken.length() - 1);
						if (!(thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")))
							throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
					}

					if (thisToken.endsWith(lineEnd)) {
						thisToken = thisToken.substring(0, thisToken.length() - 1);
						if (!(thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")))
							throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
					}

					if (thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")) {
						//process exclusive/excluded filter
						String thisFieldName = null;
						String thisTableConstraint = null;

						UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);

						if (fds.inFilterSet()) {
							if (fset == null)
								throw new InvalidQueryException("Request for this filter must be specified with a filterset " + filterName + "\n");
							else {
								if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
									thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
									thisTableConstraint = fds.getTableConstraint();
								} else {
									thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
									thisFieldName = fds.getFieldName();
								}
							}
						} else {
							thisFieldName = fds.getFieldName();
							thisTableConstraint = fds.getTableConstraint();
						}

						Filter thisFilter = null;

						if (fds.getType().equals("boolean")) {
							String thisCondition = null;
							if (thisToken.equalsIgnoreCase("exclusive"))
								thisCondition = NullableFilter.isNotNULL;
							else if (thisToken.equalsIgnoreCase("excluded"))
								thisCondition = NullableFilter.isNULL;
							else
								throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");

							thisFilter = new NullableFilter(thisFieldName, thisTableConstraint, thisCondition);
						} else if (fds.getType().equals("boolean_num")) {
							String thisCondition;
							if (cond.equalsIgnoreCase("exclusive"))
								thisCondition = "=";
							else if (cond.equalsIgnoreCase("excluded"))
								thisCondition = "!=";
							else
								throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");

							thisFilter = new BasicFilter(thisFieldName, thisTableConstraint, thisCondition, "1");
						} else
							throw new InvalidQueryException("Recieved invalid exclusive/excluded query: " + command + "\n");

						query.addFilter(thisFilter);
						condition = false;
						start = true;
					} else {
						cond = thisToken;
						if (cond.equals("in"))
							isList = true;

						condition = false;
						value = true;
					}
				} else if (value) {
					if (isList) {
						//just get rid of the beginning peren if present
						if (thisToken.startsWith(lStart)) {
							listLevel++;
							thisToken = thisToken.substring(1);
						}

						if (thisToken.startsWith("file:")) {
							if (thisToken.endsWith(lineEnd))
								thisToken = thisToken.substring(0, thisToken.length() - 1);
							if (thisToken.endsWith(","))
								thisToken = thisToken.substring(0, thisToken.length() - 1);

							String thisFieldName = null;
							String thisTableConstraint = null;

							UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
							if (!fds.getType().equals("list"))
								throw new InvalidQueryException("Cannot query this filter with a list input using in qualifier: " + filterName + "\n");

							if (fds.inFilterSet()) {
								if (fset == null)
									throw new InvalidQueryException(
										"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
								else {
									if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
										thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
										thisTableConstraint = fds.getTableConstraint();
									} else {
										thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
										thisFieldName = fds.getFieldName();
									}
								}
							} else {
								thisFieldName = fds.getFieldName();
								thisTableConstraint = fds.getTableConstraint();
							}

							Filter thisFilter = new IDListFilter(thisFieldName, new URL(thisToken));
							((IDListFilter) thisFilter).setTableConstraint(thisTableConstraint);
							query.addFilter(thisFilter);
							start = true;
							value = false;
						} else if (thisToken.equals(qStart)) {
							isList = false;
							isNested = true;
							subquery.append(" ").append(thisToken);
						} else {
							if (thisToken.endsWith(","))
								thisToken = thisToken.substring(0, thisToken.length() - 1);

							if (thisToken.endsWith(lEnd)) {
								value = false;
								start = true;
								listLevel--;
								thisToken = thisToken.substring(0, thisToken.length() - 1);

								//process list
								StringTokenizer idtokens = new StringTokenizer(thisToken, ",");
								while (idtokens.hasMoreTokens()) {
									idlist.add(idtokens.nextToken());
								}
								String thisFieldName = null;
								String thisTableConstraint = null;

								UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
								if (!fds.getType().equals("list"))
									throw new InvalidQueryException("Cannot query this filter with a list input using in qualifier: " + filterName + "\n");

								if (fds.inFilterSet()) {
									if (fset == null)
										throw new InvalidQueryException(
											"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
									else {
										if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
											thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
											thisTableConstraint = fds.getTableConstraint();
										} else {
											thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
											thisFieldName = fds.getFieldName();
										}
									}
								} else {
									thisFieldName = fds.getFieldName();
									thisTableConstraint = fds.getTableConstraint();
								}

								String[] ids = new String[idlist.size()];
								idlist.toArray(ids);
								Filter thisFilter = new IDListFilter(thisFieldName, ids);
								((IDListFilter) thisFilter).setTableConstraint(thisTableConstraint);
								query.addFilter(thisFilter);
								start = true;
								value = false;
							} else
								idlist.add(thisToken);
						}
					} else if (isNested) {
						if (thisToken.equals(lStart) || thisToken.startsWith(lStart))
						  listLevel++;
						  
						if (thisToken.indexOf(lEnd) >= 0) {
							subquery.append(" ");
							for (int i = 0, n = thisToken.length(); i < n; i++) {
								if (thisToken.charAt(i) == lEndChar)
									listLevel--;
								if (listLevel > 0)
									subquery.append(thisToken.charAt(i));
							}

							if (listLevel < 1) {
								//process subquery
								String thisFieldName = null;
								String thisTableConstraint = null;

								UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
								if (!fds.getType().equals("list"))
									throw new InvalidQueryException(
										"Cannot query this filter with a list input using in qualifier: " + filterName + "in command: " + command + "\n");

								if (fds.inFilterSet()) {
									if (fset == null)
										throw new InvalidQueryException(
											"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
									else {
										if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
											thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
											thisTableConstraint = fds.getTableConstraint();
										} else {
											thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
											thisFieldName = fds.getFieldName();
										}
									}
								} else {
									thisFieldName = fds.getFieldName();
									thisTableConstraint = fds.getTableConstraint();
								}
								Filter thisFilter = getIDFilterForSubQuery(thisFieldName, thisTableConstraint, subquery.toString());
								query.addFilter(thisFilter);
								start = true;
								value = false;
							}
						} else
							subquery.append(" ").append(thisToken);
					} else {
						if (thisToken.endsWith(","))
							thisToken = thisToken.substring(0, thisToken.length() - 1);

						if (dset.getUIFilterDescriptionByName(filterName) instanceof UIFilterDescription) {
							String thisFieldName = null;
							String thisTableConstraint = null;

							UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
							if (!fds.getType().equals("list"))
								throw new InvalidQueryException("Cannot query this filter with a list input using in: " + filterName + "in command: " + command + "\n");

							if (fds.inFilterSet()) {
								if (fset == null)
									throw new InvalidQueryException(
										"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
								else {
									if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
										thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
										thisTableConstraint = fds.getTableConstraint();
									} else {
										thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
										thisFieldName = fds.getFieldName();
									}
								}
							} else {
								thisFieldName = fds.getFieldName();
								thisTableConstraint = fds.getTableConstraint();
							}

							query.addFilter(new BasicFilter(thisFieldName, thisTableConstraint, cond, thisToken));
							start = true;
							value = false;
						} else {
							String thisHandlerParam = null;

							UIDSFilterDescription fds = (UIDSFilterDescription) dset.getUIFilterDescriptionByName(filterName);

							if (fds.IsInFilterSet()) {
								if (fset == null)
									throw new InvalidQueryException(
										"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
								else {
									if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME))
										thisHandlerParam = fset.getFieldNameModifier() + ":" + thisToken;
									else
										thisHandlerParam = fset.getTableConstraintModifier() + ":" + thisToken;
								}
							} else
								thisHandlerParam = thisToken;

							DomainSpecificFilter thisFilter = new DomainSpecificFilter(fds.getObjectCode(), thisHandlerParam);
							query.addDomainSpecificFilter(thisFilter);
							start = true;
							value = false;
						}
					}
				}
				//dont need else
			}
		}

		OutputStream os = null;
		String thisFormat = null;
		String thisSeparator = null;
		String thisFile = null;

		if (outformat != null) {
			StringTokenizer fTokens = new StringTokenizer(outformat, ",");
			while (fTokens.hasMoreTokens()) {
				StringTokenizer tok = new StringTokenizer(fTokens.nextToken(), "=");
				if (tok.countTokens() < 2)
					throw new InvalidQueryException(
						"Recieved invalid into request: "
							+ outformat
							+ "\nmust be of format: x=y(,x=y)* where x  can be one of : "
							+ file
							+ "(note, use '-' for stdout, specify a valid URL), "
							+ format
							+ "(note, use 'tab' for tab separated, 'space' for space separated, and 'comma' for comma separated), "
							+ separator
							+ "\n");

				String key = tok.nextToken();
				String value = tok.nextToken();
				if (key.equals(file))
					thisFile = value;
				else if (key.equals(format))
					thisFormat = value;
				else if (key.equals(separator)) {
					if (value.equals("tab"))
						thisSeparator = "\t";
					else if (value.equals("space"))
						thisSeparator = " ";
					else if (value.equals("comma"))
						thisSeparator = ",";
					else
						thisSeparator = value;
				} else
					throw new InvalidQueryException(
						"Recieved invalid into request: "
							+ outformat
							+ "\nmust be of format: x=y(,x=y)* where x  can be one of : "
							+ file
							+ "(note, use '-' for stdout, specify a valid URL), "
							+ format
							+ "(note, use 'tab' for tab separated, 'space' for space separated, and 'comma' for comma separated), "
							+ separator
							+ "\n");
			}
		}

		if (thisFormat == null) {
			if (outputFormat != null)
				thisFormat = outputFormat;
			else
				thisFormat = defOutputFormat;
		}

		if (thisFile == null) {
			if (subqueryOutput != null) {
				thisFile = "subquery";
				os = subqueryOutput;
			} else if (outputFile != null) {
				thisFile = outputFile;
				os = new FileOutputStream(new URL(outputFile).getFile());
			} else {
				thisFile = "stdout";
				os = System.out;
			}
		} else if (thisFile.equals("-")) {
			thisFile = "stdout";
			os = System.out;
		} else
			os = new FileOutputStream(new URL(thisFile).getFile());

		if (thisSeparator == null) {
			if (outputSeparator != null)
				thisSeparator = outputSeparator;
			else
				thisSeparator = defOutputSeparator;
		}

		FormatSpec formatspec = new FormatSpec();
		if (tabulated.equalsIgnoreCase(thisFormat))
			formatspec.setFormat(FormatSpec.TABULATED);
		else if (fasta.equalsIgnoreCase(thisFormat))
			formatspec.setFormat(FormatSpec.FASTA);
		else
			throw new InvalidQueryException("Invalid Format Request Recieved, must be either tabulated or fasta\n" + outputFormat + "\n");

		formatspec.setSeparator(thisSeparator);

		mainLogger.info("Processed request for Query: \n" + query + "\n");
		mainLogger.info("with format " + formatspec + "\n");
		mainLogger.info("into file " + thisFile);
		mainLogger.info("limit " + limit);

		engine.execute(query, formatspec, os, limit);
		if (!(thisFile.equals("stdout") || thisFile.equals("subquery")))
			os.close();
	}

	private Filter getIDFilterForSubQuery(String fieldName, String tableConstraint, String command) throws InvalidQueryException {
		command = command.trim();
		
		nestedLevel++;
		
		mainLogger.info("Recieved nested query at nestedLevel " + nestedLevel + "\n");
		
		if (nestedLevel > maxNesting)
		  throw new InvalidQueryException("Only " + maxNesting + " levels of nested Query are allowed\n");
		  
		//validate, then call parseQuery on the subcommand
		String[] tokens = command.split("\\s");
		if (!tokens[0].trim().equals(qStart))
			throw new InvalidQueryException("Invalid Nested Query Recieved: no select statement " + "recieved " + tokens[0].trim() + " in " + command + "\n");

		for (int i = 1, n = tokens.length; i < n; i++) {
			String tok = tokens[i];
			if (tok.equals("with"))
				throw new InvalidQueryException("Invalid Nested Query Recieved: with statement not allowed " + command + "\n");
			else if (tok.equals("into"))
				throw new InvalidQueryException("Invalid Nested Query Recieved: into statement not allowed " + command + "\n");
			//else not needed
		}

		mainLogger.info("Recieved request for Nested Query\n:" + command + "\n");

		subqueryOutput = new ByteArrayOutputStream();

		FormatSpec thisFormatspec = new FormatSpec(FormatSpec.TABULATED);

		thisFormatspec.setSeparator(",");
		String results = null;

		try {
			parseQuery(command);
			results = subqueryOutput.toString();
			subqueryOutput.close();
			subqueryOutput = null;
		} catch (Exception e) {
			try {
				subqueryOutput.close();
			} catch (Exception ex) {
				subqueryOutput = null;
				throw new InvalidQueryException("Could not execute Nested Query: " + ex.getMessage());
			}
			subqueryOutput = null;
			throw new InvalidQueryException("Could not execute Nested Query: " + e.getMessage());
		}

		StringTokenizer lines = new StringTokenizer(results, "\n");
		List idlist = new ArrayList();

		while (lines.hasMoreTokens()) {
			String id = lines.nextToken();

			if (id.indexOf(".") >= 0)
				id = id.substring(0, id.lastIndexOf("."));
			if (!idlist.contains(id))
				idlist.add(id);
		}

		String[] ids = new String[idlist.size()];
		idlist.toArray(ids);
		Filter f = new IDListFilter(fieldName, ids);
		((IDListFilter) f).setTableConstraint(tableConstraint);
		
		nestedLevel--;
		return f;
	}

	// MartShell instance variables
	private Engine engine;
	private MartConfiguration martconf;
	private BufferedReader reader;
	private String martHost = null;
	private String martPort = null;
	private String martUser = null;
	private String martPass = null;
	private String martDatabase = null;
  private boolean historyOn = true; // commandline history, default to on
  private boolean completionOn = true; // command completion, default to on
  private boolean readlineLoaded = false; // true only if functional Readline library was loaded, false if PureJava
  
	private String altConfigurationFileURL = null;
	private String outputFile = null;
	private final String defOutputFormat = "tabulated"; // default to tabulated output
	private String outputFormat = null;
	private final String defOutputSeparator = "\t"; // default to tab separated
	private String outputSeparator = null;
	private String batchErrorMessage = null;

	private final String exclusive = "exclusive";
	private final String tabulated = "tabulated";
	private final String fasta = "fasta";
	private final String exit = "exit;";
	private final String quit = "quit;";
	private final String help = "help;";
	private final String list = "list";
	private final String lineEnd = ";";
	private final String listStart = "list";
	private final String qStart = "select";
	private final String qWith = "with";
	private final String qFrom = "from";
	private final String qWhere = "where";
	private final String qLimit = "limit";
	private final String qInto = "into";
	private final String lStart = "(";
	private final String lEnd = ")";
	private final char lEndChar = lEnd.charAt(0);
	private final String id = "id";
	private final String seqdelimiter = "+";

	// strings used to show/set output format settings
	private final String setOutputSettings = "setOutputSettings";
	private final String showOutputSettings = "showOutputSettings";
	private final String file = "file";
	private final String format = "format";
	private final String separator = "separator";

	// strings used to show/set mart connection settings
	private final String setConnectionSettings = "setConnectionSettings";
	private final String showConnectionSettings = "showConnectionSettings";
	private final String mysqlhost = "mysqlhost";
	private final String mysqluser = "mysqluser";
	private final String mysqlpass = "mysqlpass";
	private final String mysqlport = "mysqlport";
	private final String mysqldbase = "mysqldbase";
	private final String alternateConfigurationFile = "alternateConfigurationFile";

	// strings used to show/set logging verbosity
	private final String setVerbose = "setVerbose";
	private final String showVerbose = "showVerbose";

	// variables for subquery
	private ByteArrayOutputStream subqueryOutput = null;
	private int nestedLevel = 0;
	private final int maxNesting = 1; // change this to allow deeper nesting of queries inside queries

	private List qualifiers = Arrays.asList(new String[] { "=", "!=", "<", ">", "<=", ">=", "exclusive", "excluded", "in" });

	private boolean continueQuery = false;
	private StringBuffer conline = new StringBuffer();
}
