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
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
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
				validQuery = ms.RunBatchScript(mainBatchScriptURL);
			} else {
				if (mainBatchFile != null)
					ms.setBatchOutputFile(mainBatchFile);
				if (mainBatchFormat != null)
					ms.setBatchOutputFormat(mainBatchFormat);
				if (mainBatchSeparator == null)
					ms.setBatchOutputSeparator("\t"); //default
				else
					ms.setBatchOutputSeparator(mainBatchSeparator);

				validQuery = ms.RunBatch(mainBatchSQL);
			}
			if (!validQuery) {
				System.out.println("Invalid Batch c:" + ms.getBatchError() + "\n" + usage());
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

	public void RunInteractive() {
		try {
			Readline.load(ReadlineLibrary.Getline);
			//		Getline doesnt support completion, or history manipulation/files
			completionOn = false;
			historyOn = false;
			readlineLoaded = true;
		} catch (UnsatisfiedLinkError ignore_me) {
			try {
				Readline.load(ReadlineLibrary.GnuReadline);
				historyOn = true;
				readlineLoaded = true;
			} catch (UnsatisfiedLinkError ignore_me2) {
				mainLogger.warn(
					"Could not load Readline Library, commandline editing, completion will not be available"
						+ "\nConsult MartShell documentation for methods to resolve this error.");
				historyOn = false;
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
				setConnectionSettings(SETCONSETSC);
			initEngine();
			if (completionOn) {
				mcl = new MartCompleter(martconf);
				Readline.setCompleter(mcl);
			}
		} catch (Exception e1) {
			System.out.println("Could not initialize connection: " + e1.getMessage());
			e1.printStackTrace();
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

	public boolean RunBatchScript(String batchScriptURL) {
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

	public boolean RunBatch(String querystring) {
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

	private Filter getIDFilterForSubQuery(String fieldName, String tableConstraint, String command) throws InvalidQueryException {
		command = command.trim();

		nestedLevel++;

		mainLogger.info("Recieved nested query at nestedLevel " + nestedLevel + "\n");

		if (nestedLevel > MAXNESTING)
			throw new InvalidQueryException("Only " + MAXNESTING + " levels of nested Query are allowed\n");

		//validate, then call parseQuery on the subcommand
		String[] tokens = command.split("\\s");
		if (!tokens[0].trim().equals(QSTART))
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

	private String Help(String command) throws InvalidQueryException {
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

			if (pagetype.equals("FilterPage"))
				DescribeFilterPage(dataset, pagename);
			else if (pagetype.equals("AttributePage"))
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

			if (pagetype.equals("FilterPage")) {
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
			} else if (pagetype.equals("AttributePage")) {
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
					throw new InvalidQueryException(martConnectionUsage(command));

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

				thisLine =
					Readline.readline(
						"\nPlease enter the URL for the XML Configuration File for the new mart (press enter to leave unchaged,\n enter '-' to use configuration provided by "
							+ martDatabase
							+ "):",
						false);
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
			+ MYSQLHOST
			+ ", "
			+ MYSQLPORT
			+ ", "
			+ MYSQLUSER
			+ ", "
			+ MYSQLPASS
			+ ", "
			+ MYSQLBASE
			+ ", "
			+ ALTCONFFILE
			+ "\n";
	}

	private void setVerbose(String command) throws InvalidQueryException {
		if (!command.matches("\\w+\\s(on|off)"))
			throw new InvalidQueryException("Invalid setVerbose command recieved: " + command + "\n");

		String val = command.split("\\s")[1];
		boolean verbose = (val.equals("on")) ? true : false;

		System.out.println("Logging now " + val + "\n");
		defaultLoggingConfiguration(verbose);
	}

	private void setOutputFormat(String command) throws InvalidQueryException {
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
						+ "\nusage: setOutputFormat x=y(,x=y)* where x  can be one of : "
						+ FILE
						+ " (note, use '-' for stdout, or specify a valid URL), "
						+ FORMAT
						+ " (note, use 'comma' for comma separated), "
						+ SEPARATOR
						+ "\n");

			String key = tokens.nextToken();
			String value = tokens.nextToken();

			if (key.equals(FILE)) {
				if (value.equals("-"))
					outputFile = null;
				else
					outputFile = value;
			} else if (key.equals(FORMAT))
				outputFormat = value;
			else if (key.equals(SEPARATOR))
				if (key.equals("comma"))
					outputSeparator = ",";
				else
					outputSeparator = value;
			else
				throw new InvalidQueryException(
					"Recieved invalid setOutputFormat request: "
						+ command
						+ "\nmust be of format: setOutputFormat x=y(,x=y)* where x  can be one of : "
						+ FILE
						+ " (note, use '-' for stdout, specify a valid URL), "
						+ FORMAT
						+ " (note, use 'comma' for comma separated), "
						+ SEPARATOR
						+ "\n");
		}
	}

	private void showOutputSettings() {
		String thisFile = "stdout";
		if (outputFile != null)
			thisFile = outputFile;

		System.out.println(
			"Output Format: " + FORMAT + " = " + outputFormat + ", " + SEPARATOR + " = " + "'" + outputSeparator + "'" + ", " + FILE + " = " + thisFile);
	}

	private void WriteHistory(String command) throws InvalidQueryException {
		try {
			StringTokenizer com = new StringTokenizer(command, " ");
			int tokCount = com.countTokens();
			com.nextToken(); // skip commmand start

			String req = null;
			URL url = null;

			if (tokCount < 2)
				throw new InvalidQueryException("WriteHistory command must be provided a valid URL: " + command + "\n");
			else if (tokCount == 2) {
				//url
				url = new URL(com.nextToken());
			} else if (tokCount == 3) {
				req = com.nextToken();
				url = new URL(com.nextToken());
			} else
				throw new InvalidQueryException("Recieved invalid WriteHistory request " + command + "\n");

			WriteHistoryLinesToURL(req, url);

		} catch (Exception e) {
			throw new InvalidQueryException("Could not write history " + e.getMessage());
		}
	}

	private void WriteHistoryLinesToURL(String req, URL url) throws InvalidQueryException {
		String[] lines = GetHistoryLines(req); // will throw an exception if GetHistoryLines requirements are not satisfied

		try {
			OutputStreamWriter hisout = new OutputStreamWriter(new FileOutputStream(url.getFile()));
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
		URL url = null;
		try {
			url = new URL(com.nextToken());
			ExecScriptFromURL(url);
		} catch (Exception e) {
			throw new InvalidQueryException("Could not execute script: " + url.getFile() + " " + e.getMessage());
		}
	}

	private void ExecScriptFromURL(URL url) throws InvalidQueryException {
		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream()));

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
		URL url = null;
		try {
			url = new URL(com.nextToken());
			LoadScriptFromURL(url);
		} catch (Exception e) {
			throw new InvalidQueryException("Could not load script: " + url.getFile() + " " + e.getMessage());
		}
	}

	private void LoadScriptFromURL(URL url) throws InvalidQueryException {
		if (!readlineLoaded)
			throw new InvalidQueryException("Sorry, histrory functions are not available on your terminal.\n");
		if (!historyOn)
			throw new InvalidQueryException("Sorry, histrory is not activated.\n");

		try {
			Readline.readHistoryFile(url.getFile());
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

	private void parse(String line) throws IOException, InvalidQueryException {
		if (line.equals(LINEEND)) {
			String command = conline.append(line).toString().trim();
			continueQuery = false;
			conline = new StringBuffer();
			mcl.SetDefaultMode();
			parseCommand(command);
		} else if (line.endsWith(LINEEND)) {
			String command = conline.append(" " + line).toString().trim();
			continueQuery = false;
			conline = new StringBuffer();
			mcl.SetDefaultMode();
			
			parseCommand(command);
		} else {
			conline.append(" " + line);
			continueQuery = true;
			
			//MartCompleter Mode
			if (line.indexOf(QSTART) >= 0)
			  mcl.SetSelectMode();
			else if (line.indexOf(QFROM) >= 0)
			  mcl.SetFromMode();
			else if (line.indexOf(QWHERE) >= 0)
			  mcl.SetWhereMode();
			//else not needed
		}
	}

	private void parseCommand(String command) throws IOException, InvalidQueryException {
		int cLen = command.length();

    command = command.replaceAll("\\s;$", ";");
    
		if (cLen == 0)
			return;
		else if (command.startsWith(HELPC))
			System.out.print(Help(NormalizeCommand(command)));
		else if (command.startsWith(QSTART))
			parseQuery(command);
		else if (command.startsWith(DESCC))
			DescribeRequest(NormalizeCommand(command));
		else if (command.startsWith(SETCONSETSC))
			setConnectionSettings(NormalizeCommand(command));
		else if (command.startsWith(SHOWCONSETSC))
			showConnectionSettings();
		else if (command.startsWith(SETPROMPT))
			setPrompt(NormalizeCommand(command));
		else if (command.startsWith(SETOUTSETSC))
			setOutputFormat(NormalizeCommand(command));
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
		else {
			throw new InvalidQueryException("\nInvalid Command: please try again " + command + "\n");
		}
	}

	private void parseQuery(String command) throws IOException, InvalidQueryException {
		boolean start = true;
		boolean selectClause = false;
		boolean sequenceClause = false;
		boolean fromClause = false;
		boolean whereClause = false;
		boolean limitClause = false;
		boolean intoClause = false;
		int listLevel = 0; // level of subquery/list

		StringBuffer attString = new StringBuffer();
		StringBuffer sequenceString = new StringBuffer();
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
				if (!(thisToken.equalsIgnoreCase(QSTART)))
					throw new InvalidQueryException("Invalid Query Recieved, should begin with select: " + command + "\n");
				else {
					start = false;
					selectClause = true;
				}
			} else if (selectClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement in the middle of a select statement: " + command + "\n");
				if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement before from statement: " + command + "\n");
				if (thisToken.equalsIgnoreCase(QINTO))
					throw new InvalidQueryException("Invalid Query Recieved, into statement before from statement: " + command + "\n");
				if (thisToken.equalsIgnoreCase(QLIMIT))
					throw new InvalidQueryException("Invalid Query Recieved, limit statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE)) {
					selectClause = false;
					sequenceClause = true;
				} else if (thisToken.equalsIgnoreCase(QFROM)) {
					selectClause = false;
					fromClause = true;
				} else
					attString.append(thisToken);
			} else if (sequenceClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement in the middle of an into statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QINTO))
					throw new InvalidQueryException("Invalid Query Recieved, into statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT))
					throw new InvalidQueryException("Invalid Query Recieved, limit statement before from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM)) {
					sequenceClause = false;
					fromClause = true;
				} else
					sequenceString.append(thisToken);
			} else if (fromClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after from statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE)) {
					fromClause = false;
					whereClause = true;
				} else if (thisToken.equalsIgnoreCase(QINTO)) {
					fromClause = false;
					intoClause = true;
				} else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					fromClause = false;
					limitClause = true;
				} else {
					if (dataset != null)
						throw new InvalidQueryException("Invalid Query Recieved, dataset already set, attempted to set again: " + command + "\n");
					else
						dataset = thisToken;
				}
			} else if (whereClause) {
				if (listLevel < 1 && thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after where statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(LSTART) || thisToken.startsWith(LSTART)) {
					listLevel++;

					if (thisToken.endsWith(LEND) || thisToken.endsWith(LEND + ",") || thisToken.endsWith(LEND + LINEEND)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == LENDCHAR)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(LINEEND) || thisToken.endsWith(LINEEND)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where statement: " + command + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (listLevel > 0) {
					if (thisToken.equalsIgnoreCase(LEND)) {
						listLevel--;
					} else if (thisToken.endsWith(LEND) || thisToken.endsWith(LEND + ",") || thisToken.endsWith(LEND + LINEEND)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == LENDCHAR)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(LINEEND) || thisToken.endsWith(LINEEND)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where statement: " + command + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after where statement, not in subquery: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM))
					throw new InvalidQueryException("Invalid Query Recieved, from statement after where statement, not in subquery: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement after where statement, not in subquery: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QINTO)) {
					whereClause = false;
					intoClause = true;
				} else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					whereClause = false;
					limitClause = true;
				} else
					whereString.append(" ").append(thisToken);
			} else if (intoClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after into statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after into statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM))
					throw new InvalidQueryException("Invalid Query Recieved, from statement into where statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement into where statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					intoClause = false;
					limitClause = true;
				} else {
					if (thisToken.endsWith(LINEEND))
						thisToken = thisToken.substring(0, thisToken.length() - 1);
					outformat = thisToken;
				}
			} else if (limitClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM))
					throw new InvalidQueryException("Invalid Query Recieved, from statement into limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement into limit statement: " + command + "\n");
				else if (thisToken.equalsIgnoreCase(QINTO))
					throw new InvalidQueryException("Invalid Query Recieved, into statement into limit statement: " + command + "\n");
				else {
					if (limit > 0)
						throw new InvalidQueryException("Invalid Query Recieved, attempt to set limit twice: " + command + "\n");
					else {
						if (thisToken.endsWith(LINEEND))
							thisToken = thisToken.substring(0, thisToken.length() - 1);
						limit = Integer.parseInt(thisToken);
					}
				}
			}
			// else not needed, as these are the only states present
		}

		if (dataset == null)
			throw new InvalidQueryException("Invalid Query Recieved, did not set dataset: " + command + "\n");

		if (attString.length() == 0 && sequenceString.length() == 0)
			throw new InvalidQueryException("Invalid Query Recieved, no attributes or sequence request found: " + command + "\n");

		if (dataset.endsWith(LINEEND))
			dataset = dataset.substring(0, dataset.length() - 1);

		if (!martconf.containsDataset(dataset))
			throw new InvalidQueryException("Dataset " + dataset + " is not found in this mart\n");

		Dataset dset = martconf.getDatasetByName(dataset);
		Query query = new Query();
		FilterPage currentFpage = null;
		AttributePage currentApage = null;

		query.setStarBases(dset.getStarBases());
		query.setPrimaryKeys(dset.getPrimaryKeys());

		if (sequenceString.length() > 0) {
			String seqrequest = sequenceString.toString().trim();

			int typecode = 0;
			int left = 0;
			int right = 0;

			StringTokenizer tokens = new StringTokenizer(seqrequest, SEQDELIMITER, true);
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
			if (whereString.toString().endsWith(LINEEND))
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
					if (!wTokens.hasMoreTokens()) {
						if (!(thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")))
							throw new InvalidQueryException("Invalid Query Recieved, filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
					}

					if (thisToken.endsWith(",")) {
						thisToken = thisToken.substring(0, thisToken.length() - 1);
						if (!(thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")))
							throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
					}

					if (thisToken.endsWith(LINEEND)) {
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
						if (thisToken.startsWith(LSTART)) {
							listLevel++;
							thisToken = thisToken.substring(1);
						}

						if (thisToken.startsWith("file:")) {
							if (thisToken.endsWith(LINEEND))
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
						} else if (thisToken.equals(QSTART)) {
							isList = false;
							isNested = true;
							subquery.append(" ").append(thisToken);
						} else {
							if (thisToken.endsWith(","))
								thisToken = thisToken.substring(0, thisToken.length() - 1);

							if (thisToken.endsWith(LEND)) {
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
						if (thisToken.equals(LSTART) || thisToken.startsWith(LSTART))
							listLevel++;

						if (thisToken.indexOf(LEND) >= 0) {
							subquery.append(" ");
							for (int i = 0, n = thisToken.length(); i < n; i++) {
								if (thisToken.charAt(i) == LENDCHAR)
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
							+ FILE
							+ "(note, use '-' for stdout, or specify a valid URL), "
							+ FORMAT
							+ "(note, use 'tab' for tab separated, 'space' for space separated, and 'comma' for comma separated), "
							+ SEPARATOR
							+ "\n");

				String key = tok.nextToken();
				String value = tok.nextToken();
				if (key.equals(FILE))
					thisFile = value;
				else if (key.equals(FORMAT))
					thisFormat = value;
				else if (key.equals(SEPARATOR)) {
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
							+ FILE
							+ "(note, use '-' for stdout, specify a valid URL), "
							+ FORMAT
							+ "(note, use 'tab' for tab separated, 'space' for space separated, and 'comma' for comma separated), "
							+ SEPARATOR
							+ "\n");
			}
		}

		if (thisFormat == null) {
			if (outputFormat != null)
				thisFormat = outputFormat;
			else
				thisFormat = DEFOUTPUTFORMAT;
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
				thisSeparator = DEFOUTPUTSEPARATOR;
		}

		FormatSpec formatspec = new FormatSpec();
		if (TABULATED.equalsIgnoreCase(thisFormat))
			formatspec.setFormat(FormatSpec.TABULATED);
		else if (FASTA.equalsIgnoreCase(thisFormat))
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

	// MartShell instance variables
	private Engine engine;
	private MartConfiguration martconf;
	private BufferedReader reader;
	private String martHost = null;
	private String martPort = null;
	private String martUser = null;
	private String martPass = null;
	private String martDatabase = null;
	private MartCompleter mcl; // will hold the MartCompleter, if Readline is loaded and completion turned on
	private boolean helpLoaded = false; // first time Help function is called, loads the help properties file and sets this to true
	private boolean historyOn = true; // commandline history, default to on
	private boolean completionOn = true; // command completion, default to on
	private boolean readlineLoaded = false; // true only if functional Readline library was loaded, false if PureJava
	private String userPrompt = null;

	private String altConfigurationFileURL = null;
	private String outputFile = null;
	private final String DEFOUTPUTFORMAT = "tabulated"; // default to tabulated output
	private String outputFormat = null;
	private final String DEFOUTPUTSEPARATOR = "\t"; // default to tab separated
	private String outputSeparator = null;
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

	// query instructions
	private final String QSTART = "select";
	private final String QSEQUENCE = "sequence";
	private final String QFROM = "from";
	private final String QWHERE = "where";
	private final String QLIMIT = "limit";
	private final String QINTO = "into";
	private final String LSTART = "(";
	private final String LEND = ")";
	private final char LENDCHAR = LEND.charAt(0);
	private final String LINEEND = ";";
	private final String ID = "id";
	private final String SEQDELIMITER = "+";
	private final String EXCLUSIVE = "exclusive";
	private final String TABULATED = "tabulated";
	private final String FASTA = "fasta";

	protected final List availableCommands =
		Collections.unmodifiableList(
			Arrays.asList(
				new String[] {
					EXITC,
					QUITC,
					HELPC,
					DESCC,
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
					QSTART,
					QSEQUENCE,
					QFROM,
					QWHERE,
					QLIMIT,
					QINTO,
					FASTA }));

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
}
