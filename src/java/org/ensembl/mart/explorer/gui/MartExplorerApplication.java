/* Generated by Together */

package org.ensembl.mart.explorer.gui;

import org.apache.log4j.*;
import java.util.*;
import gnu.getopt.*;
import java.awt.event.WindowEvent;
import javax.swing.*;
import java.awt.event.*;
import org.ensembl.mart.explorer.*;
import java.awt.*;
import java.sql.*;
import java.io.*;
import java.net.*;


public class MartExplorerGUI extends JFrame {
  private final static int WIDTH = 600;
  private final static int HEIGHT = 600;

  private static String COMMAND_LINE_SWITCHES = "l:H:P:u:p:d:a:f:o:F:i:I:t:hvs:c:g:";

  /** Creates new form JFrame */
  public MartExplorerGUI() {
    initGUI();
    setSize( new Dimension(WIDTH, HEIGHT) );
	queryPanel.setMartExplorerGUI( this );
  }

  public static void main (String[] args) {
    String loggingURL = null;
    boolean help = false;
    boolean verbose = false;

    Getopt g = new Getopt("MartExplorerApplication", args, COMMAND_LINE_SWITCHES);
    int c;
    String arg;

    while ((c = g.getopt()) != -1) {

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
	  }
	}

      // Initialise logging system
      if (loggingURL != null) {
          PropertyConfigurator.configure(loggingURL);
      }
      else {
        defaultLoggingConfiguration( verbose );
      }

     // check for help or no args
     if ( help ) {
       System.out.println( usage() );
       System.exit(0);
     }
     else {
	    new MartExplorerGUI().run();
	 }
  }

  /** This method is called from within the constructor to initialize the form. */
  private void initGUI() {
    clearToolBarButton.setText("Clear");
    clearToolBarButton.setText("Clear");
    clearToolBarButton.setActionCommand("Clear");
    toolBar.add(clearToolBarButton);
    toolBar.add(exportToolBarButton);
    exportToolBarButton.setText("jButton1");
    exportToolBarButton.setText("Export");
    exportToolBarButton.setActionCommand("export");
    addWindowListener(
                      new java.awt.event.WindowAdapter() {
                          public void windowClosing(java.awt.event.WindowEvent evt) {
                            exitForm(evt);
                          }
                        });
    setTitle("MartExplorer");
    getContentPane().getPreferredSize().setSize(new java.awt.Dimension(600, 600));
    //setResizable(false);
    getContentPane().setLayout(new javax.swing.BoxLayout(this.getContentPane(), javax.swing.BoxLayout.Y_AXIS));
    getContentPane().add(toolBar);
    getContentPane().add(queryPanel);
    getContentPane().add(summaryPanel);
    summaryPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
                                                                                                                   new java.awt.Color(153, 153, 153), 1), "Summary", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                                                                        new java.awt.Font("SansSerif", 0, 11), new java.awt.Color(60, 60, 60)));
    fileMenu.setText("File");
    fileMenu.add(exitMenuItem);
    menuBar.add(fileMenu);
    menuBar.add(queryMenu);
    menuBar.add(helpMenu);
    exitMenuItem.setText("jMenuItem1");
    exitMenuItem.setActionCommand("exitMenuItem");
    exitMenuItem.setText("Exit");
    exitMenuItem.setToolTipText("Exits Application");
    exitMenuItem.addActionListener(
                                   new ActionListener() {
                                       public void actionPerformed(ActionEvent e) { exitMenuItemActionPerformed(e); }
                                     });
    helpMenu.setText("Help");
    helpMenu.add(aboutMenuItem);
    aboutMenuItem.setText("About");
    aboutMenuItem.addActionListener(
                                    new ActionListener() {
                                        public void actionPerformed(ActionEvent e) { aboutActionPerformed(e); }
                                      });
    setJMenuBar(menuBar);
    setSize(new java.awt.Dimension(500,402));
    exportToolBarButton.addActionListener(
                                          new ActionListener() {
                                              public void actionPerformed(ActionEvent e) { exportToolBarButtonActionPerformed(e); }
                                            });
    queryMenu.setText("Query");
    queryMenu.add(kakaMenuItem);
    queryMenu.add(executeMenuItem);
    queryMenu.add(clearMenuItem);
    kakaMenuItem.setText("Kaka partial query");
    executeMenuItem.setText("Execute");
    clearMenuItem.setText("New");
    clearMenuItem.setActionCommand("Clear");
    clearMenuItem.setText("Clear");
    clearMenuItem.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){newMenuItemActionPerformed(e);}});
    executeMenuItem.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){executeMenuItemActionPerformed(e);}});
    kakaMenuItem.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){kakaMenuItemActionPerformed(e);}});
    clearToolBarButton.addActionListener(
        new ActionListener() {
            public void actionPerformed(ActionEvent e) { clearToolBarButtonActionPerformed(e); }
        });
  }

  /** Exit the Application */
  private void exitForm(WindowEvent evt) {
    exit();
  }

  public void run() {
    setVisible(true);
  }

  public void exitMenuItemActionPerformed(ActionEvent e) {
    exit();
  }

  private void exit() {
    System.exit(0);
  }

  public void aboutActionPerformed(ActionEvent e) {
    new AboutDialog().setVisible(true);
  }

  public void exportToolBarButtonActionPerformed(ActionEvent e) {
    executeQuery();
  }

  private void newQuery() {
    queryPanel.clear();
  }

  private void executeQuery() {

		final Query q = new Query();
    final Component parent = this;

    try {
      queryPanel.updateQuery( q );
    } catch( Exception e ) {
      logger.warn( "Failed to execute query", e );
      JOptionPane.showMessageDialog( this, "Failed to execute query: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

    }

    // execute the query on a separate thread so we don't
    // slow down the main AWT thread if it takes a while to run.
    new Thread() {
      public void run() {
        try {
          engine.execute( q );
        } catch( Exception e ) {
          logger.warn( "Failed to execute query", e );
          JOptionPane.showMessageDialog( parent, "Failed to execute query: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
     }
   }.start();
  }



  /**
   * This method creates a dummy query then uses it to populate the GUI with
   * values. Included for test purposes during development.  */
  private void kakaPartialQuery() {
    Query q = new Query();
    q.setHost( "kaka.sanger.ac.uk" );
    q.setUser( "anonymous" );
    q.setDatabase( "ensembl_mart_10_1" );
    q.setSpecies( "homo_sapiens" );
    q.setFocus( "gene" );
    //q.addFilter( new IDListFilter("gene_stable_id", new String[]{"ENSG00000170057"}) );
    try {
      // Add some test filters (user must manually select wich to use
      // unless she wants the default one.
      q.addFilter( new IDListFilter("gene_stable_id", new File( System.getProperty("user.home")+"/dev/mart-explorer/data/gene_stable_id.test") ) );

      q.addFilter( new IDListFilter("gene_stable_id", new URL( "file://" +System.getProperty("user.home")+"/dev/mart-explorer/data/gene_stable_id.test") ) );
      q.addFilter( new IDListFilter("gene_stable_id", new
        String[]{"ENSG00000170057"} ) );
    }catch( IOException e ) {
      logger.warn("Failed to construct partial kaka query", e );
    }
    q.addAttribute( new FieldAttribute("gene_stable_id") );
    //query.addFilter( new IDListFilter("gene_stable_id", new File( STABLE_ID_FILE).toURL() ) );
    //q.setResultTarget( new ResultFile( "/tmp/kaka.txt", new SeparatedValueFormatter("\t") ) );
    q.setResultTarget( new ResultWindow( "Results_1", new SeparatedValueFormatter ("\t") ) );
    queryPanel.updatePage( q );
  }

  public void newMenuItemActionPerformed(ActionEvent e) {
    newQuery();
  }


  public void executeMenuItemActionPerformed(ActionEvent e) {
    executeQuery();
  }

  public void kakaMenuItemActionPerformed(ActionEvent e) {
    kakaPartialQuery();
  }

  public void clearToolBarButtonActionPerformed(ActionEvent e) {
    newQuery();
  }

  /**
   * If a ResultWindow
   * with the same name already exists that is returned, otherwise a new one
   * is created.
   * @return ResultWindow with specified name and formatter.
   */
  public ResultWindow createResultWindow(String name, Formatter formatter) {

    ResultWindow rw = null;
    logger.debug( "name="+name );
    logger.debug( "resultWindows="+resultWindows );
    if ( name!=null && resultWindows.containsKey( name ) ) {
      rw = (ResultWindow) resultWindows.get( name );
      rw.setFormatter( formatter );
    }
    else {
      rw = new ResultWindow( name, formatter );
      resultWindows.put( name, rw );
    }

    return rw;
  }

  public static String usage() {
    return 
      "MartExplorerGUI <OPTIONS>"
      + "\n"
      + "\n-h                             - this screen"
      + "\n-v                             - verbose logging output"
      + "\n-l LOGGING_FILE_URL            - logging file, defaults to console if none specified"
  	  + "\n";
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

  private Map resultWindows = new TreeMap();
  private static final Logger logger = Logger.getLogger(MartExplorerGUI.class.getName());
  private QueryPanel queryPanel = new QueryPanel();
  private JMenuBar menuBar = new JMenuBar();
  private JMenu fileMenu = new JMenu();
  private JMenuItem exitMenuItem = new JMenuItem();
  private JMenu helpMenu = new JMenu();
  private JMenuItem aboutMenuItem = new JMenuItem();
  private JToolBar toolBar = new JToolBar();
  private JButton clearToolBarButton = new JButton();
  private JButton exportToolBarButton = new JButton();
  private SummaryPanel summaryPanel = new SummaryPanel();
  private JMenu queryMenu = new JMenu();
  private JMenuItem kakaMenuItem = new JMenuItem();
  private JMenuItem executeMenuItem = new JMenuItem();
  private JMenuItem clearMenuItem = new JMenuItem();
  private Engine engine = new Engine();
}
