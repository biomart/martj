package org.ensembl.mart.explorer;

import java.sql.*;
import org.apache.log4j.*;
import java.util.*;
import gnu.getopt.*;
import java.io.*;
import java.net.*;

public class MartExplorerTool {

  private static final String defaultConf = System.getProperty("user.home")+"/.martexplorer";

  private static String confinUse = null;
  private static String host = null;
  private static String port = null;
  private static String database = null;
  private static String user = null;
  private static String password = null;
  private static String species = null;
  private static String focus = null;
  private static ResultFile resultFile = new ResultFile();
  private static boolean validQuery = true;

  private static Logger logger = Logger.getLogger(MartExplorerTool.class.getName());
  private static List attributes = new ArrayList();
  private static List filters = new ArrayList();

  private static IDListFilter idFilter = null;

  private static String COMMAND_LINE_SWITCHES = "l:H:P:u:p:d:a:f:o:F:i:I:t:hvs:c:g:";

  public MartExplorerTool() {
  }
  
  
  /**
   * Initialise logging system to print to logging messages of level >= WARN
   * to console. Does nothing if system property log4j.configuration is set.
   */
  public static void defaultLoggingConfiguration( boolean verbose) {
    if (System.getProperty("log4j.configuration") == null) {
      
      BasicConfigurator.configure();
      if ( verbose ) 
        Logger.getRoot().setLevel(Level.INFO);
      else 
        Logger.getRoot().setLevel(Level.WARN);
    }
  }

  public static String usage() {
    return 
      "MartExplorerTool <OPTIONS>"
      + "\n"
      + "\n-h                             - this screen"
      + "\n-g                             - mysql connection configuration file"
      + "\n-H HOST                        - database host"
      + "\n-P PORT                        - database port" 
      + "\n-u USER                        - database user name"
      + "\n-p PASSWORD                    - database password"
      + "\n-d DATABASE                    - database name"
      + "\n-s SPECIES                     - species name"
      + "\n-c FOCUS                       - focus of query"
      + "\n-a ATTRIBUTE                   - one or more attributes"
      + "\n-f FILTER                      - zero or more filters"
      + "\n-o OUTPUT_FILE                 - output file, default is standard out"
      + "\n-F OUTPUT_FORMAT               - output format, default is tab separated values"
      + "\n-i IDENTIFIER_FILTER           - zero or more identifiers "
      + "\n-I URL_CONTAINING_IDENTIFIERS  - url with one or more identifiers"
      + "\n-t IDENTIFIER_TYPE             - type of identifiers (necessary if -i or -I)"
      + "\n-v                             - verbose logging output"
      + "\n-l LOGGING_FILE_URL            - logging file, defaults to console if none specified"
      + "\n"
      + "\nThe application searches for a .martexplorer file in the user home directory for mysql connection configuration"
      + "\nif present, this file will be loaded. If the -g option is given, or any of the commandline connection"
      + "\nparameters are passed, these over-ride those values provided in the .martexplorer file"
      + "\nUsers specifying a mysql connection configuration file with -g,"
      + "\nor using a .martexplorer file, can use -H, -P, -p, -u, or -d to specify"
      + "\nparameters not specified in the configuration file, or over-ride those that are specified."
	  + "\n";
  }

  /**
   * Parses command line parameters. 
   */
  public static void main(String[] args) {
    String loggingURL = null;
    boolean help = false;
    boolean verbose = false;

    // check for the defaultConf file, and use it, if present.  Some values may be overridden with a user specified file with -g
    if (new File( defaultConf ).exists()) 
        getConnProperties(defaultConf);

    Getopt g = new Getopt("MartExplorerApplication", args, COMMAND_LINE_SWITCHES);
    int c;
    String arg;
    int argnum = 0;

    while ((c = g.getopt()) != -1) {
	  argnum++;

      switch (c) {

        case 'l':
            loggingURL = g.getOptarg();
            break;

        case 'h':
            help = true;
            break;

        case 'v':
            verbose = true;
            break;

 	    // get everything that is specified in the provided configuration file, then fill in rest with other options, if provided
        case 'g':
		  getConnProperties(g.getOptarg());
          break;

        case 'H':
          host = g.getOptarg();
          break;

        case 'P':
          port = g.getOptarg();
          break;

        case 'd':
          database = g.getOptarg();
          break;

        case 'u':
          user = g.getOptarg();
          break;

        case 'p':
          password = g.getOptarg();
          break;

        case 's':
          species = g.getOptarg() ;
      	  break;

        case 'c':
          focus = g.getOptarg() ;
      	  break;

        case 'a':
          addAttribute( g.getOptarg() );
          break;

        case 'f':
          addFilter( g.getOptarg() );
          break;

        case 'o':
          nameFile( g.getOptarg() );
          break;

        case 'F':
          format( g.getOptarg() ); 
          break;

        case 'I':
          addIdFilterURL( g.getOptarg() ); 
          break;

        case 'i':
          addIdFilter( g.getOptarg() ); 
          break;
        
        case 't':
          identifierType( g.getOptarg() );
          break;

	  }
    }            


    // Initialise logging system
    if (loggingURL != null) {
        PropertyConfigurator.configure(loggingURL);
    }
    else {
      defaultLoggingConfiguration( verbose );
    }

    if (confinUse != null) {
       logger.info("Using configuration file: "+confinUse+"\n");
	}
    else {
		logger.info("Using commandline options only for connection configuration");
	}

    // check for help or no args
    if ( help || argnum == 0) {
      System.out.println( usage() );
    }

    else if (host == null) 
      validationError("Host must be set\n"+usage());

    else if (user == null) 
      validationError("User must be set\n"+usage());

    else if (database == null) 
      validationError("Database must be set\n"+usage());

    else if ( species == null)
      validationError("Species must be set\n"+usage());

    else if ( focus == null)
      validationError("Focus must be set\n"+usage());

    else if ( attributes.size()==0 )
      validationError("At least one attributes must be chosen (use -a).");

    else if ( idFilter!=null && idFilter.getType()==null ) 
      validationError("You must set id filter type if you use an id filter (use -t).");

    else {
      // Default to writing output to stdout.
      if ( resultFile.getName()==null )
        resultFile.setWriter( new BufferedWriter(new OutputStreamWriter(System.out) ) );

      // Default to writing output format as tab separated values.
      if ( resultFile.getFormatter()==null )
        resultFile.setFormatter( new SeparatedValueFormatter("\t") );

	  run();
	}
  }

  private static void validationError( String message ) {
    System.err.println( "Error: " + message );
    validQuery = false;
  }

  /**
   * Constructs a Query based on the command line parameters and executes it.
   */
  private static void run() {

    if ( !validQuery ) {
      System.err.println( "Run with -h for help." );
      return;
    }

    if ( idFilter!=null ) filters.add( idFilter );

    Query q = new Query();
    q.setHost( host );
    q.setPort( port );
    q.setDatabase( database );
    q.setUser( user );
    q.setPassword( password );
    q.setSpecies( species );
    q.setFocus( focus );
    q.setAttributes( attributes );

    q.setFilters( (Filter[])filters.toArray( new Filter[]{}) );
    q.setResultTarget( resultFile );

    Engine e = new Engine();
    try {
      e.execute( q );
    } catch ( Exception ex) {
      ex.printStackTrace();
    }
  }



  private static void getConnProperties( String connfile ) {
      URL confInfo;
      Properties p = new Properties();

      try {
          confInfo = new File( connfile ).toURL();
          p.load(confInfo.openStream());
 
          host = p.getProperty("mysqlhost");
          port = p.getProperty("mysqlport");
          database = p.getProperty("mysqldbase");
          user = p.getProperty("mysqluser");
          password = p.getProperty("mysqlpass");
	  }
      catch (java.net.MalformedURLException e) {
		  logger.warn("Could not load connection file "+connfile+" MalformedURLException: "+e);
	  }
      catch (java.io.IOException e) {
		  logger.warn("Could not load connection file "+connfile+" IOException: "+e);
	  }
      confinUse = connfile;
  }

  private static void addAttribute( String attribute ) {
    attributes.add( new FieldAttribute( attribute ) );
  }

  private static void addFilter( String filterCondition ) {
    
    // currently only support BasicFilters e.g. a=3
    StringTokenizer tokens = new StringTokenizer( filterCondition, "=<>", true);
    int n = tokens.countTokens();
    if (n!=3 ) 
      validationError("Currently unsupported filter: " + filterCondition);
    else 
      filters.add( new BasicFilter( tokens.nextToken(), tokens.nextToken(), tokens.nextToken() ) );
  }

  
  private static void nameFile( String fileName ) {
    resultFile.setName( fileName );
  }

  private static void format( String format ) {
    if ( "tsv".equals( format) ) 
      resultFile.setFormatter( new SeparatedValueFormatter("\t") );
    else
      validationError("Unkown format: " + format);
  }


  private static void addIdFilter( String identifier ) {
    if ( idFilter==null ) idFilter = new IDListFilter();
    idFilter.addIdentifier( identifier );
  }
  
  private static void addIdFilterURL( String url ) {
    
    try {

      // retain current filterType if set
      String filterType =  ( idFilter!=null ) ? idFilter.getType() : null;

      idFilter = new IDListFilter(filterType, new URL( url ));

    } catch ( Exception e ){
      validationError("Problem loading from url: " + url + " : " + e.getMessage());
    }
  }

  private static void identifierType(String type) {
    if ( idFilter==null )
      idFilter = new IDListFilter();
    
    if ( idFilter.getType()==null )
      idFilter.setType( type );
    else
      validationError("ID Filter type already set, can't set it again: " + type);
  }

}
