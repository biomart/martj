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
 
package org.ensembl.mart.explorer;

import gnu.getopt.Getopt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * CommandLine Tool for extraction of data from a mart database.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 */
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
  private static boolean validQuery = true;
  private static FormatSpec formatspec = new FormatSpec();
  private static OutputStream os = null;
  private static SequenceDescription seqDescription = null;

  private static Logger logger = Logger.getLogger(MartExplorerTool.class.getName());
  private static List attributes = new ArrayList();
  private static List filters = new ArrayList();

  private static IDListFilter idFilter = null;

  private static String COMMAND_LINE_SWITCHES = "l:H:P:U:p:d:a:f:O:F:i:I:S:t:hvs:c:M:R:";
  private static final String seqdelimiter = "+";

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

/**
 *  @return application usage instructions
 * 
 */
  public static String usage() {
    return 
      "MartExplorerTool <OPTIONS>"
      + "\n"
      + "\n-h                             - this screen"
      + "\n-M                             - mysql connection configuration file"
      + "\n-H HOST                        - database host"
      + "\n-P PORT                        - database port" 
      + "\n-U USER                        - database user name"
      + "\n-p PASSWORD                    - database password"
      + "\n-d DATABASE                    - database name"
      + "\n-s SPECIES                     - species name"
      + "\n-c FOCUS                       - focus of query"
      + "\n-a ATTRIBUTE                   - one or more attributes"
      + "\n-f FILTER                      - zero or more filters"
      + "\n-O OUTPUT_FILE                 - output file, default is standard out"
      + "\n-F OUTPUT_FORMAT               - output format, either tabulated or fasta"
      + "\n-R OUTPUT_SEPARATOR            - if OUTPUT_FORMAT is tabulated, can define a separator, defaults to tab separated" 
      + "\n-i IDENTIFIER_FILTER           - zero or more identifiers "
      + "\n-I URL_CONTAINING_IDENTIFIERS  - url with one or more identifiers, use - to pipe in from STDIN (newline separated)"
      + "\n-S SEQUENCE_TYPE               - sequnce request description <left-flank length+>seqtype<+right-flank length>"
      + "\n-t IDENTIFIER_TYPE             - type of identifiers (necessary if -i, -I, or -S)"
      + "\n-v                             - verbose logging output"
      + "\n-l LOGGING_FILE_URL            - logging file, defaults to console if none specified"
      + "\n"
      + "\nThe application searches for a .martexplorer file in the user home directory for mysql connection configuration"
      + "\nif present, this file will be loaded. If the -g option is given, or any of the commandline connection"
      + "\nparameters are passed, these over-ride those values provided in the .martexplorer file"
      + "\nUsers specifying a mysql connection configuration file with -g,"
      + "\nor using a .martexplorer file, can use -H, -P, -p, -u, or -d to specify"
      + "\nparameters not specified in the configuration file, or over-ride those that are specified."
      + "\n\nSequences:"
      + "\nrequest descriptions can be of the form intb+type+intb, where inta+ and +intb are optional flank length modifiers."
      + "\nThe following sequence types are supported:"
      + "\n"
      + SequenceDescription.getAvailableSequences()
      + "\n"
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
        case 'M':
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

        case 'U':
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

        case 'O':
          nameFile( g.getOptarg() );
          break;

        case 'F':
          format( g.getOptarg() ); 
          break;

	    case 'R':
			formatspec.setSeparator( g.getOptarg() );
            break;

        case 'I':
              addIdFilterURL( g.getOptarg() ); 
          break;

        case 'i':
          addIdFilter( g.getOptarg() ); 
          break;

	    case 'S':
		  addSequenceDescription( g.getOptarg());
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
      return;
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

    else if ( seqDescription == null && attributes.size()==0 )
      validationError("If not requesting Sequences, at least one attributes must be chosen (use -a).");

    else if ( idFilter!=null && idFilter.getType()==null ) 
      validationError("You must set id filter type if you use an id filter (use -t).");

    else if (formatspec.getFormat() == -1 )
		validationError("You must set a format for the output (use -F).");

    else {
	    // all required attributes set, may need to set some defaults
		if (formatspec.getFormat() == FormatSpec.TABULATED && formatspec.getSeparator() == null) {
            logger.info("No separator specified for tabulated output, defaulting to tab separated");
            formatspec.setSeparator("\t");
		}
	}

    run();
  }

/**
 * Prints an Error message, and sets flag to stop procesing of the Query
 * @param message -- String to print in Error: message
 */
  public static void validationError( String message ) {
    System.err.println( "Error: " + message );
    validQuery = false;
  }

  /**
   * Constructs a Query based on the command line parameters and executes it.
   */
  public static void run() {

    if ( !validQuery ) {
      System.err.println( "Run with -h for help." );
      return;
    }

    // default output is stdout
    if (os == null)
        os = System.out;

    if ( idFilter!=null ) filters.add( idFilter );

    Query q = new Query();
    q.setSpecies( species );
    q.setFocus( focus );
    q.setAttributes( attributes );

    q.setFilters( (Filter[])filters.toArray( new Filter[]{}) );
    
    if (seqDescription!=null ) q.setSequenceDescription(seqDescription);

    Engine e = new Engine(host, port, user, password, database);
    try {
      e.execute( q, formatspec, os );
    } catch ( Exception ex) {
      ex.printStackTrace();
    }
  }

/**
 * Parses java properties file to get mysql database connection parameters.
 * 
 * @param connfile -- String name of the configuration file containing mysql
 *  database configuration properties.
 */
  public static void getConnProperties( String connfile ) {
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

/**
 * Adds an attribute to the Query
 * 
 * @param attribute -- String attribute name
 * @see Query
 * @see Attribute
 */
  public static void addAttribute( String attribute ) {
    attributes.add( new FieldAttribute( attribute ) );
  }

/**
 * Adds a filter to the Query
 * 
 * @param filterCondition -- String filter condition. Consists of 
 *  a filter type, the condition to include in the where clause (=<>), and the
 *  filter value (example "chr_name=3" for chromosome equal to three).
 *  Currently, only BasicFilters are supported.
 * @see Query
 * @see BasicFilter
 */
  public static void addFilter( String filterCondition ) {
    
    // currently only support BasicFilters e.g. a=3
    StringTokenizer tokens = new StringTokenizer( filterCondition, "=<>", true);
    int n = tokens.countTokens();
    if (n!=3 ) 
      validationError("Currently unsupported filter: " + filterCondition);
    else 
      filters.add( new BasicFilter( tokens.nextToken(), tokens.nextToken(), tokens.nextToken() ) );
  }

  /**
   * Sets the Output to a user defined output file by
   * creating a FileOutputStream from the file.
   * If the File cannot be created, it defaults to STDOUT.
   * 
   * @param fileName -- String path to file for output
   */
  public static void nameFile( String fileName ) {
    try {
        os = new FileOutputStream( fileName );
	}
    catch (FileNotFoundException e) {
        logger.warn("Could not open file "+fileName+e+"\ndefaulting to stdout\n");
        os = System.out;
	}
  }

/**
 * Sets the format for the output. Format can be "tabulated" or "fasta"
 * 
 * @param format -- String format
 * @see FormatSpec
 */
  public static void format( String format ) {
    if ( "tabulated".equals( format) ) 
		formatspec.setFormat(FormatSpec.TABULATED);

    else if ( "fasta".equals( format ) )
	    formatspec.setFormat(FormatSpec.FASTA);
     
    else
      validationError("Unkown format: " + format + "\n must be tabulated or fasta");
  }

/**
 * Sets up an IDListFilter, which is later added to a Query to filter on
 * certain types of known identifiers
 * 
 * @param identifier -- type of identifier
 * @see Query
 * @see IDListFilter
 */
  public static void addIdFilter( String identifier ) {
    if ( idFilter==null ) idFilter = new IDListFilter();
    idFilter.addIdentifier( identifier );
  }
  
  /**
   * If url is '-', calls addIDFilterStream(System.in)
   * else Gets identifiers from a URL, and creates an 
   * IDListFilter from them
   * 
   * @param url
   * @see Query
   * @see IDListFilter
   */
  public static void addIdFilterURL( String url ) {
    
        if (url.equals("-")) {
			addIdFilterStream(System.in);
        }
        else {
            try {

                // retain current filterType if set
                String filterType =  ( idFilter!=null ) ? idFilter.getType() : null;

                idFilter = new IDListFilter(filterType, new URL( url ));

            } catch ( Exception e ){
                validationError("Problem loading from url: " + url + " : " + e.getMessage());
            }
        }
  }

  /**
   * Gets identifiers from STDIN, and creates an IDListFilter from them.
   * 
   * @param instream -- InputStream containing newline separated identifiers
   * @see Query
   * @see IDListFilter
   */
  public static void addIdFilterStream( InputStream instream ) {
    
    try {

      // retain current filterType if set
      String filterType =  ( idFilter!=null ) ? idFilter.getType() : null;

      idFilter = new IDListFilter(filterType, new InputStreamReader( instream ));

    } catch ( Exception e ){
      validationError("Problem loading from STDIN: " + e.getMessage());
    }
  }

/**
 * Sets the filter type to use for IDFilters set by any of the addIdFilter methods.
 * 
 * @param type -- String name of identifier
 * @see Query
 * @see IDListFilter
 */
  public static void identifierType(String type) {
    if ( idFilter==null )
      idFilter = new IDListFilter();
    
    if ( idFilter.getType()==null )
      idFilter.setType( type );
    else
      validationError("ID Filter type already set, can't set it again: " + type);
  }

  public static void addSequenceDescription(String seqrequest) {
	  int typecode = 0;
  	  int left = 0;
  	  int right = 0;
  	  
  	  StringTokenizer tokens = new StringTokenizer(seqrequest, seqdelimiter, true);
	  int n = tokens.countTokens();
	  switch (n) {
	  	 case 5:
	  	     // left+type+right
	  	     left = Integer.parseInt(tokens.nextToken());
	  	     tokens.nextToken(); // skip plus
	  	     typecode = SequenceDescription.SEQS.indexOf(tokens.nextToken());
	  	     tokens.nextToken();
	  	     right = Integer.parseInt(tokens.nextToken());
	  	     break;
	  	 case 3:
	  	     // left+type || type+right
	  	     String tmpl = tokens.nextToken();
	  	     tokens.nextToken();
	  	     String tmpr = tokens.nextToken();
	  	     
	  	     if (SequenceDescription.SEQS.contains(tmpl)) {
				 typecode = SequenceDescription.SEQS.indexOf(tmpl);
	  	         right = Integer.parseInt(tmpr);
	  	     }
	  	     else if (SequenceDescription.SEQS.contains(tmpr)) {
	  	         left = Integer.parseInt(tmpl);
				typecode = SequenceDescription.SEQS.indexOf(tmpr);
	  	     }
	  	     else {
	  	        throw new RuntimeException("Couldnt parse sequence request "+seqrequest );
	  	     }
	  	     break;
	  	 case 1:
	  	     // type
		     typecode = SequenceDescription.SEQS.indexOf(seqrequest);
	  	     break;
	  }
	          
  	  try {
  	  	seqDescription = new SequenceDescription(typecode, left, right);
  	  } catch (InvalidQueryException e) {
  	  	throw new RuntimeException("Couldnt add Sequence Description "+e.getMessage());
  	  }
  }
}
