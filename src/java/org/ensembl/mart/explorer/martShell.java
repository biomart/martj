package org.ensembl.mart.explorer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ensembl.mart.explorer.config.ConfigurationException;
import org.ensembl.mart.explorer.config.Dataset;
import org.ensembl.mart.explorer.config.MartConfiguration;

public class martShell {

	// main variables
	private static final String defaultConf =
		System.getProperty("user.home") + "/.martexplorer";
	private static String COMMAND_LINE_SWITCHES =
		"hC:M:H:P:U:p:d:vl:e:O:F:R:E:";
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
	private static Logger mainLogger =
		Logger.getLogger(martShell.class.getName());

	/**
	 *  @return application usage instructions
	 * 
	 */
	public static String usage() {
		return "martShell <OPTIONS>"
			+ "\n"
			+ "\n-h                             - this screen"
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
			mainLogger.warn(
				"Could not load connection file "
					+ connfile
					+ " MalformedURLException: "
					+ e);
		} catch (java.io.IOException e) {
			mainLogger.warn(
				"Could not load connection file "
					+ connfile
					+ " IOException: "
					+ e);
		}
		confinUse = connfile;
	}

	public static void main(String[] args) {
		String loggingURL = null;
		boolean help = false;
		boolean verbose = false;

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
			mainLogger.info(
				"Using commandline options only for connection configuration");
		}

		// check for help
		if (help) {
			System.out.println(usage());
			return;
		}

		martShell ms = new martShell();
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
				System.out.println(
					"Must supply either a Query command or a query script\n"
						+ usage());
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
				System.out.println(
					"Invalid Batch c:" + ms.getBatchError() + "\n" + usage());
				System.exit(0);
			}
		} else
			ms.runInteractive();

	}

	public martShell() {
	}

	public void runInteractive() {
		reader = new BufferedReader(new InputStreamReader(System.in));
		String thisline = null;
		
		try {
			if (martHost == null || martHost.length() < 5)
				setConnectionSettings(setConnectionSettings);
			initEngine();
		} catch (Exception e1) {
			System.out.println(
				"Could not initialize connection: " + e1.getMessage());
			e1.printStackTrace();
			System.exit(1);
		}

		while (true) {
			try {
				Prompt();
				thisline = reader.readLine();
				if (thisline.equals(exit) || thisline.equals(quit))
					break;

				if (thisline.length() != 0) {
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
			reader =
				new BufferedReader(
					new InputStreamReader(
						new URL(batchScriptURL).openStream()));

			for (String line = reader.readLine();
				line != null;
				line = reader.readLine())
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

	private void initEngine()
		throws MalformedURLException, ConfigurationException {
		engine =
			new Engine(martHost, martPort, martUser, martPass, martDatabase);

		if (altConfigurationFileURL != null)
			martconf =
				engine.getMartConfiguration(new URL(altConfigurationFileURL));
		else
			martconf = engine.getMartConfiguration();
	}

	private void parse(String line) throws IOException, InvalidQueryException {
		if (line.equals(help)) {
			if (continueQuery)
				System.out.println(
					"help not applicable in the middle of a query");
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
		reader.close();
		System.exit(0);
	}

	private void Prompt() {
		if (continueQuery)
			subPrompt();
		else
			mainPrompt();
	}

	private void mainPrompt() {
		String prompt = null;

		if (martUser != null && martDatabase != null)
			prompt = martUser + "@" + martHost + " : "+ martDatabase + ">";
		else
			prompt = ">";

		System.out.print(prompt);
	}

	private void subPrompt() {
		System.out.print("%");
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
			StringTokenizer tokens =
				new StringTokenizer(fTokens.nextToken(), "=");
			if (tokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Recieved invalid setOutputFormat request: "
						+ command
						+ "\nmust be of format: setOutputFormat x=y(,x=y)* where x  can be one of : "
						+ format
						+ ", "
						+ separator
						+ "\n");

			String key = tokens.nextToken();
			String value = tokens.nextToken();

			if (key.equals(format))
				outputFormat = value;
			else if (key.equals(separator))
				outputSeparator = value;
			else
				throw new InvalidQueryException(
					"Recieved invalid setOutputFormat request: "
						+ command
						+ "\nmust be of format: setOutputFormat x=y(,x=y)* where x  can be one of : "
						+ format
						+ ", "
						+ separator
						+ "\n");
		}
	}

	private void showOutputSettings() {
		String file = "stdout";
		if (outputFile != null)
			file = outputFile;

		System.out.println(
			"Output Format: "
				+ format
				+ " = "
				+ outputFormat
				+ " "
				+ separator
				+ " = "
				+ "'"
				+ outputSeparator
				+ "'"
				+ " file = "
				+ file);
	}

	private void setConnectionSettings(String command)
		throws InvalidQueryException {
		if (command.endsWith(lineEnd))
			command = command.substring(0, command.length() - 1);

		StringTokenizer ctokens = new StringTokenizer(command, " ");
		if (ctokens.countTokens() > 1) {
			// parse command
			ctokens.nextToken(); // throw away
			String connSettings = ctokens.nextToken();

			StringTokenizer sTokens = new StringTokenizer(connSettings, ",");
			while (sTokens.hasMoreTokens()) {
				StringTokenizer tokens =
					new StringTokenizer(sTokens.nextToken(), "=");
				if (tokens.countTokens() < 2)
					throw new InvalidQueryException(
						martConnectionUsage(command));

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
					throw new InvalidQueryException(
						martConnectionUsage(command));
			}
		} else {
			if (mainBatchMode)
				throw new InvalidQueryException(martConnectionUsage(command));

			//interactive
			if (reader == null)
				throw new InvalidQueryException("Interactive request to set mart connection parameters recieved without valid input reader\n");

			String thisLine = null;

			try {
				System.out.println(
					"\nPlease enter the host address of the mart database (press enter to leave unchaged): ");
				thisLine = reader.readLine();
				if (thisLine.length() >= 5)
					martHost = thisLine;

				System.out.println(
					"\nPlease enter the port on which the mart database is running (press enter to leave unchaged): ");
				thisLine = reader.readLine();
				if (thisLine.length() > 1)
					martPort = thisLine;

				System.out.println(
					"\nPlease enter the user name used to connect to the mart database (press enter to leave unchaged): ");
				thisLine = reader.readLine();
				if (thisLine.length() > 1)
					martUser = thisLine;

				System.out.println(
					"\nPlease enter the password used to connect to the mart database (press enter to leave unchaged): ");
				thisLine = reader.readLine();
				if (thisLine.length() > 1)
					martPass = thisLine;

				System.out.println(
					"\nPlease enter the name of the mart database you wish to query (press enter to leave unchaged): ");
				thisLine = reader.readLine();
				if (thisLine.length() > 1)
					martDatabase = thisLine;

				System.out.println(
					"\nPlease enter the URL for the XML Configuration File for the new mart (press enter to leave unchaged,\n enter '-' to use configuration provided by "+martDatabase+"):");
				thisLine = reader.readLine();
				if (thisLine.length() >= 1) {
				    if (thisLine.equals("-"))
				       altConfigurationFileURL = null;
				    else
					  altConfigurationFileURL = thisLine;
				}

			} catch (IOException e) {
				throw new InvalidQueryException(
					"Problem reading input for mart connection settings: "
						+ e.getMessage());
			}
		}
		try {
		  initEngine();
		} catch (Exception e) {
		  throw new InvalidQueryException("Could not initialize connection: " +e.getMessage());
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
	private void parseCommand(String command)
		throws IOException, InvalidQueryException {
		int cLen = command.length();

		if (cLen == 0) {
			return;
		} else if (command.startsWith(qStart)) {
			List atts = new ArrayList();
			Hashtable filts = new Hashtable();

			String seqd = null;
			String dataset = null;
			String where = null;
			String limit = null;
			String into = null;

			int fIndex = command.indexOf(qFrom);
			int wIndex = command.indexOf(qWith);

			if (fIndex < 0)
				throw new InvalidQueryException(
					"\nYou must supply a from statement with a query: "
						+ command
						+ "\n");

			String attList =
				command.substring(qStart.length(), fIndex - 1).trim();

			if (wIndex >= 0) {
				if (wIndex > fIndex)
					throw new InvalidQueryException("\nInvalid Query: with sequence statements must precede from statements\n");

				int atWindex = attList.indexOf(qWith);
				seqd = attList.substring(atWindex);
				attList = attList.substring(0, atWindex - 1).trim();
			}

			StringTokenizer att = new StringTokenizer(attList, ",", false);
			if (att.countTokens() > 1) {
				while (att.hasMoreTokens()) {
					atts.add(att.nextToken().trim());
				}
			} else
				atts.add(attList);

			int lineEndIndex = command.indexOf(lineEnd);
			int whereIndex = command.indexOf(qWhere);
			int limitIndex = command.lastIndexOf(qLimit);
			int intoIndex = command.lastIndexOf(qInto);

			if (limitIndex > -1 && intoIndex > -1 && limitIndex > intoIndex)
				throw new InvalidQueryException(
					"\nInvalid Query, limit must precede into: "
						+ command
						+ "\n");

			if (whereIndex > -1) {
				dataset =
					command
						.substring(fIndex + qFrom.length(), whereIndex - 1)
						.trim();

				if (limitIndex > -1) {
					if (whereIndex > limitIndex)
						throw new InvalidQueryException(
							"\nInvalid Query: where must precede the limit clause: "
								+ command
								+ "\n");
					else
						where =
							command
								.substring(
									whereIndex + qWhere.length(),
									limitIndex)
								.trim();

					if (intoIndex > -1) {
						limit =
							command
								.substring(
									limitIndex + qLimit.length(),
									intoIndex)
								.trim();
						into =
							command
								.substring(
									intoIndex + qInto.length(),
									lineEndIndex)
								.trim();
					} else
						limit =
							command
								.substring(
									limitIndex + qLimit.length(),
									lineEndIndex)
								.trim();

				} else if (intoIndex > -1) {
					if (whereIndex > intoIndex)
						throw new InvalidQueryException(
							"\nInvalid Query: Where must precede the into clause: "
								+ command
								+ "\n");
					else {
						where =
							command
								.substring(
									whereIndex + qWhere.length(),
									intoIndex)
								.trim();
						into =
							command
								.substring(
									intoIndex + qInto.length(),
									lineEndIndex)
								.trim();
					}
				} else
					where =
						command
							.substring(
								whereIndex + qWhere.length(),
								lineEndIndex)
							.trim();

				StringTokenizer whereTokens =
					new StringTokenizer(where, " ", false);

				String filterType = null;
				String filtCond = null;
				boolean islist = false;
				boolean issubr = false;
				StringBuffer sub = new StringBuffer();
				StringBuffer thisList = new StringBuffer();
				int liststart = 0;
				int listend = 0;
				int open = 0;

				while (whereTokens.hasMoreTokens()) {
					String nextWhere = whereTokens.nextToken();

					if (nextWhere.endsWith(","))
						nextWhere =
							nextWhere.substring(0, nextWhere.length() - 1);

					if (filterType == null)
						filterType = nextWhere;
					else {
						if (nextWhere.equals("excluded"))
							filts.put(filterType, nextWhere);
						else if (nextWhere.equals("exclusive"))
							filts.put(filterType, nextWhere);
						else if (islist) {
							if (nextWhere.startsWith(lStart)) {
								open++;

								nextWhere = nextWhere.substring(1);

								if (nextWhere.startsWith(qStart)) {
									//if (sublevel > 0) {
									//System.out.println("Can only have one nested subroutine");
									//return;
									//}
									islist = false;
									issubr = true;
									sub.append(nextWhere);
								} else {
									// single token surrounded by perenthesis
									if (nextWhere.endsWith(lEnd)) {
										islist = false;
										filts.put(
											filterType,
											filtCond
												+ " LIST "
												+ thisList.append(
													nextWhere
														.substring(
															0,
															nextWhere.indexOf(
																lEnd))
														.toString()));
										thisList = new StringBuffer();
										filterType = null;
										open = 0;
									} else
										thisList.append(nextWhere + ",");
								}
							} else if (nextWhere.endsWith(lEnd)) {
								// last token in a series within perenthesis

								islist = false;
								filts.put(
									filterType,
									filtCond
										+ " LIST "
										+ thisList.append(
											nextWhere
												.substring(
													0,
													nextWhere.indexOf(lEnd))
												.toString()));
								thisList = new StringBuffer();
								filterType = null;
								open = 0;
							} else if (nextWhere.indexOf("file:") > -1) {
								//URL
								filts.put(
									filterType,
									filtCond + " URL " + nextWhere);
								filterType = null;
							} else
								thisList.append(nextWhere + ",");
						} else if (issubr) {
							char operen = '(';
							char cperen = ')';
							int lastindex = 0;
							for (int i = 0, n = nextWhere.length();
								i < n;
								i++) {
								char c = nextWhere.charAt(i);
								if (c == cperen) {
									open--;
									if (open > 0)
										lastindex++;
								} else {
									if (c == operen)
										open++;
									lastindex++;
								}
							}

							nextWhere = nextWhere.substring(0, lastindex);
							if (open == 0) {
								issubr = false;
								sub.append(" " + nextWhere);
								filts.put(
									filterType,
									filtCond + " SubQuery " + sub.toString());
								filterType = null;
								sub = new StringBuffer();
							} else
								sub.append(" " + nextWhere);
						} else if (nextWhere.equals("in")) {
							islist = true;
							filtCond = nextWhere;
						} else {
							String filtValue = whereTokens.nextToken();

							if (filtValue.endsWith(","))
								filtValue =
									filtValue.substring(
										0,
										filtValue.indexOf(","));

							filts.put(filterType, nextWhere + " " + filtValue);
							filterType = null;
						}
					}
				}
			} else if (limitIndex > -1) {
				dataset =
					command
						.substring(fIndex + qFrom.length(), limitIndex - 1)
						.trim();
				if (intoIndex > -1) {
					limit =
						command
							.substring(limitIndex + qLimit.length(), intoIndex)
							.trim();
					into =
						command
							.substring(intoIndex + qInto.length(), lineEndIndex)
							.trim();
				} else
					limit =
						command
							.substring(
								limitIndex + qLimit.length(),
								lineEndIndex)
							.trim();
			} else if (intoIndex > -1) {
				dataset =
					command
						.substring(fIndex + qFrom.length(), intoIndex - 1)
						.trim();
				into =
					command
						.substring(intoIndex + qInto.length(), lineEndIndex)
						.trim();
			} else {
				dataset =
					command
						.substring(fIndex + qFrom.length(), lineEndIndex)
						.trim();
			}
			// process it all
			System.out.println("Requested query from " + dataset);

			int n = atts.size();
			System.out.println("\nwith " + n + " Attributes\n");
			for (int i = 0; i < n; i++) {
				System.out.println((String) atts.get(i) + "\n");
			}

			if (seqd != null) {
				StringTokenizer stokens = new StringTokenizer(seqd, " ");
				stokens.nextToken(); //skip with
				String sequenceDescription = stokens.nextToken();
				System.out.println(
					"With sequence description = "
						+ sequenceDescription
						+ "\n");
			}

			if (where != null) {
				System.out.println("\nand " + filts.size() + " Filters:\n");

				for (Iterator iter = filts.keySet().iterator();
					iter.hasNext();
					) {
					String filterType = (String) iter.next();
					String filtValue = (String) filts.get(filterType);
					System.out.println(filterType + " " + filtValue + "\n");
				}
			}

			if (limit != null)
				System.out.println("Limit " + limit + "\n");

			if (into != null)
				System.out.println("into " + into);
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
		else if (command.equals(exit) || command.equals(quit))
			ExitShell();
		else {
			throw new InvalidQueryException(
				"\nInvalid Query: please try again " + command + "\n");
		}
	}

	// martShell instance variables
	private Engine engine;
	private MartConfiguration martconf;
	private BufferedReader reader;
	private String martHost = null;
	private String martPort = null;
	private String martUser = null;
	private String martPass = null;
	private String martDatabase = null;

	private String altConfigurationFileURL = null;
	private String outputFile = null;
	private String outputFormat = "tabulated"; // default to tabulated output
	private String outputSeparator = "\t"; // default to tab separated
	private String batchErrorMessage = null;

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
	private final String id = "id";

	// strings used to show/set output format settings
	private final String setOutputSettings = "setOutputSettings";
	private final String showOutputSettings = "showOutputSettings";
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
	private final String alternateConfigurationFile =
		"alternateConfigurationFile";

	private final int sublevel = 0;
	private List qualifiers =
		Arrays.asList(
			new String[] {
				"=",
				"<",
				">",
				"<=",
				">=",
				"exclusive",
				"excluded",
				"in" });

	private boolean continueQuery = false;
	private StringBuffer conline = new StringBuffer();
}
