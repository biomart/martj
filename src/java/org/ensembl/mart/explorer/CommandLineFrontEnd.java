package org.ensembl.mart.explorer;

import java.sql.*;
import org.apache.log4j.*;
import java.util.*;
import gnu.getopt.*;
import java.io.*;
import java.net.*;

public class CommandLineFrontEnd {

  private Logger logger = Logger.getLogger(CommandLineFrontEnd.class.getName());

  private List attributes = new ArrayList();
  private List filters = new ArrayList();
  private String host = null;
  private String port = null;
  private String database = null;
  private String user = null;
  private String password = null;
  private String species = null;
  private String focus = null;
  private boolean verbose = false;
  private ResultFile resultFile = new ResultFile();
  private Formatter formatter = null;
  private boolean validQuery = true;
  private IDListFilter idFilter = null;

  public static String COMMAND_LINE_SWITCHES = "l:H:P:u:p:d:a:f:o:F:i:I:t:hvs:c:";

  public CommandLineFrontEnd() {
		System.out.println(COMMAND_LINE_SWITCHES);
  }
  

  public static String usage() {
    return 
      "MartExplorerApplication <OPTIONS>"
      + "\n"
      + "\nIf host (-H) is specified then the program runs in command line mode,"
      + "\notherwise the program starts with the Graphical User Interface."
      + "\n"
      + "\n-h                             - this screen"
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
      + "\n-l LOGGING_FILE_URL            - logging file, defaults to console if non specified";
  }

  /**
   * Parses command line parameters. 
   */
  public void init(String[] args) {

    Getopt g = new Getopt("MartExplorerApplication", args, COMMAND_LINE_SWITCHES);
    int c;
    String arg;
    while ((c = g.getopt()) != -1) {
      System.out.println( "c=" +(char)c);
      switch (c) {

			case 'v':
        verbose = true;
      	break;

      case 'l':
        // do nothing, should have been handled by MartExplorerApplication
        break;

      case 'h':
        // do nothing, should have been handled by MartExplorerApplication
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
      	System.out.println("user="+user);
        break;

      case 'p':
        password = g.getOptarg();
        break;

      case 's':
        species = g.getOptarg() ;
      	System.out.println("species="+species);
      	break;

			case 'c':
        focus = g.getOptarg() ;
      	System.out.println("focus="+focus);
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

    // Default to writing output to stdout.
    if ( resultFile.getName()==null )
      resultFile.setWriter( new BufferedWriter(new OutputStreamWriter(System.out) ) );

    // Default to writing output format as tab separated values.
    if ( resultFile.getFormatter()==null )
      resultFile.setFormatter( new SeparatedValueFormatter("\t") );

    if (host == null) 
      validationError("Host must be set (use -h).");
    else if (user == null) 
      validationError("User must be set (use -u).");
    else if (database == null) 
      validationError("Database must be set (use -d).");

    else if ( species == null)
      validationError("Species must be set (use -s).");

    else if ( focus == null)
      validationError("Focus must be set (use -c).");

    else if ( attributes.size()==0 )
      validationError("At least one attributes must be chosen (use -a).");
    else if ( idFilter!=null && idFilter.getType()==null ) 
      validationError("You must set id filter type if you use an id filter (use -t).");

  }


  private void validationError( String message ) {
    System.err.println( "Error: " + message );
    validQuery = false;
  }

  /**
   * Constructs a Query based on the command line parameters and executes it.
   */
  public void run() {

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
    q.setFilters( filters );
    q.setResultTarget( resultFile );

		if ( verbose )
      System.out.println( "QUERY : "+ q );

    Engine e = new Engine();
    try {
      e.execute( q );
    } catch ( Exception ex) {
      ex.printStackTrace();
    }
  }




  private void addAttribute( String attribute ) {
    attributes.add( new FieldAttribute( attribute ) );
  }

  private void addFilter( String filterCondition ) {
    
    // currently only support BasicFilters e.g. a=3
    StringTokenizer tokens = new StringTokenizer( filterCondition, "=<>", true);
    int n = tokens.countTokens();
    if (n!=3 ) 
      validationError("Currently unsupported filter: " + filterCondition);
    else 
      filters.add( new BasicFilter( tokens.nextToken(), tokens.nextToken(), tokens.nextToken() ) );
  }

  
  private void nameFile( String fileName ) {
    resultFile.setName( fileName );
  }

  private void format( String format ) {
    if ( "tsv".equals( format) ) 
      resultFile.setFormatter( new SeparatedValueFormatter("\t") );
    else
      validationError("Unkown format: " + format);
  }


  private void addIdFilter( String identifier ) {
    if ( idFilter==null ) idFilter = new IDListFilter();
    idFilter.addIdentifier( identifier );
  }
  
  private void addIdFilterURL( String url ) {
    
    try {

      // retain current filterType if set
      String filterType =  ( idFilter!=null ) ? idFilter.getType() : null;

      idFilter = new IDListFilter(filterType, new URL( url ));

    } catch ( Exception e ){
      validationError("Problem loading from url: " + url + " : " + e.getMessage());
    }
  }

  private void identifierType(String type) {
    if ( idFilter==null )
      idFilter = new IDListFilter();
    
    if ( idFilter.getType()==null )
      idFilter.setType( type );
    else
      validationError("ID Filter type already set, can't set it again: " + type);
  }

}
