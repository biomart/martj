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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.editor;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.ensembl.mart.explorer.Feedback;
import org.ensembl.mart.guiutils.DatabaseSettingsDialog;
import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.config.AttributePage;
//import org.ensembl.mart.lib.config.Exportable;
//import org.ensembl.mart.lib.config.Importable;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatabaseDatasetConfigUtils;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.DSConfigAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSConfigAdaptor;
import org.ensembl.mart.lib.config.DatasetConfigIterator;
import org.ensembl.mart.lib.config.DatasetConfigXMLUtils;
import org.ensembl.mart.lib.config.URLDSConfigAdaptor;
//import org.jdom.Document;



/**
 * Class MartEditor extends JFrame..
 *
 * <p>This class contains the main function, it draws the external frame, toolsbar, menus.
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetConfig
 */

public class MartEditor extends JFrame implements ClipboardOwner {

  private JDesktopPane desktop;
  static private final String newline = "\n";
  private JFileChooser fc;
  final static String IMAGE_DIR = "data/image/";
  static final private String NEW = "New";
  static final private String OPEN = "Open";
  static final private String SAVE = "Save";
  static final private String COPY = "Copy";
  static final private String CUT = "Cut";
  static final private String PASTE = "Paste";
  static final private String DELETE = "Delete";
  static final private String UNDO = "Undo";
  static final private String REDO = "Redo";
  static final private String HELP = "Copy";
  private File file = null;

  static private DetailedDataSource ds;
  private static DatasetConfigXMLUtils dscutils = new DatasetConfigXMLUtils(true);
  //may want to turn validation on?
  private static DatabaseDatasetConfigUtils dbutils;
  private static Hashtable dbutilsHash = new Hashtable();
  

  static private String user;
  private String database;
  private String schema;
  private static String connection;
  

  /** Persistent preferences object used to hold user history. */
  private Preferences prefs = Preferences.userNodeForPackage(this.getClass());
  private DatabaseSettingsDialog databaseDialog = new DatabaseSettingsDialog(prefs);

  protected Clipboard clipboardEditor;

  public MartEditor() {
	
//	autoconnect on startup
	super("MartEditor");
	
		 String defaultSourceName = databaseDialog.getConnectionName();

				 if (defaultSourceName == null || defaultSourceName.length() < 1)
				   defaultSourceName =
					 defaultSourceName =
					   DetailedDataSource.defaultName(
						 databaseDialog.getHost(),
						 databaseDialog.getPort(),
						 databaseDialog.getDatabase(),
						 databaseDialog.getSchema(),
						 databaseDialog.getUser());

				 ds =
				   new DetailedDataSource(
					 databaseDialog.getDatabaseType(),
					 databaseDialog.getHost(),
					 databaseDialog.getPort(),
					 databaseDialog.getDatabase(),
					 databaseDialog.getSchema(),
					 databaseDialog.getUser(),
					 databaseDialog.getPassword(),
					 10,
					 databaseDialog.getDriver(),
					 defaultSourceName);
			
				 user = databaseDialog.getUser();
				 database = databaseDialog.getDatabase();
			
				 Connection conn = null;
				 try {
				   conn = ds.getConnection();
				   dbutils = new DatabaseDatasetConfigUtils(dscutils, ds);
					connection = "MartEditor (CONNECTED TO " + databaseDialog.getDatabase() + "/"+databaseDialog.getSchema()+" AS "+databaseDialog.getUser()+")";
				   //valid = true;
				   String[] schemas = databaseDialog.getSchema().split(";");
				   for (int i = 0; i < schemas.length; i++){
				   		DetailedDataSource ds1 = 
										new DetailedDataSource(
										 databaseDialog.getDatabaseType(),
										 databaseDialog.getHost(),
										 databaseDialog.getPort(),
										 schemas[i],
										 databaseDialog.getSchema(),
										 databaseDialog.getUser(),
										 databaseDialog.getPassword(),
										 10,
										 databaseDialog.getDriver(),
										 defaultSourceName);
					    DatabaseDatasetConfigUtils dbutils1 = new DatabaseDatasetConfigUtils(new DatasetConfigXMLUtils(true), ds1);
				   		dbutilsHash.put(schemas[i],dbutils1);
				   }
				   
				   
				   
				 } catch (SQLException e) {
				 	ds = null;
					connection = "MartEditor (NO DATABASE CONNECTION)";	
				   //System.out.println(e.toString()); 	
				   //warning dialog then retry
				   //Feedback f = new Feedback(this);
				   //f.warning("Could not connect to Database\nwith the given Connection Settings.\nPlease try again!");
				   //valid = false;
				 } finally {
				   DetailedDataSource.close(conn);
				 }
    JFrame.setDefaultLookAndFeelDecorated(true);
    fc = new JFileChooser();

    //Create the toolbar.
    JToolBar toolBar = new JToolBar("Still draggable");
    addButtons(toolBar);

    //Make the big window be indented 50 pixels from each edge
    //of the screen.
    int inset = 100;
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2);

    //Set up the GUI.
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(toolBar, BorderLayout.NORTH);

    desktop = new JDesktopPane();
    
    this.getContentPane().add(desktop, BorderLayout.CENTER);
    setJMenuBar(createMenuBar());
	
    //Make dragging a little faster but perhaps uglier.
    desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

    clipboardEditor = new Clipboard("editor_clipboard");

	


  }

  protected void addButtons(JToolBar toolBar) {
    JButton button = null;

    //first button
    button = makeNavigationButton("new", NEW, "Create a new dataset config", "new");
    toolBar.add(button);

    //second button
    button = makeNavigationButton("open", OPEN, "Open a dataset config", "open");
    toolBar.add(button);

    //third button
    button = makeNavigationButton("save", SAVE, "Save dataset config", "save");
    toolBar.add(button);

    button = makeNavigationButton("copy", COPY, "Copy a tree node", "copy");
    toolBar.add(button);

    button = makeNavigationButton("cut", CUT, "Cut a tree node", "cut");
    toolBar.add(button);

    button = makeNavigationButton("paste", PASTE, "Paste tree node", "paste");
    toolBar.add(button);

    button = makeNavigationButton("undo", UNDO, "Undo", "undo");
    toolBar.add(button);

    button = makeNavigationButton("redo", REDO, "Redo", "redo");
    toolBar.add(button);

  }

  protected JButton makeNavigationButton(String imageName, String actionCommand, String toolTipText, String altText) {
    //Look for the image.
    String imgLocation = IMAGE_DIR + imageName + ".gif";
    URL imageURL = DatasetConfigTree.class.getClassLoader().getResource(imgLocation);

    //Create and initialize the button.
    JButton button = new JButton();
    button.setBorderPainted(false);
    button.setActionCommand(actionCommand);
    button.setToolTipText(toolTipText);
    button.addActionListener(new MenuActionListener());

    if (imageURL != null) { //image found
      button.setIcon(new ImageIcon(imageURL, altText));
    } else { //no image found
      button.setText(altText);
      System.err.println("Resource not found: " + imgLocation);
    }

    return button;
  }

  protected JMenuBar createMenuBar() {
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem menuItem;

    //Create the menu bar.
    menuBar = new JMenuBar();

    //Build the first menu.
    menu = new JMenu("File");
    menu.setMnemonic(KeyEvent.VK_F);
    menu.getAccessibleContext().setAccessibleDescription("the file related menu");
    menuBar.add(menu);

    //a group of JMenuItems
    ImageIcon icon = createImageIcon(IMAGE_DIR + "new.gif");

    menuItem = new JMenuItem("Database Connection ");
    MartEditor.MenuActionListener menuActionListener = new MartEditor.MenuActionListener();
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_D);
    menu.add(menuItem);

    menu.addSeparator();

    menuItem = new JMenuItem("Import ");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_I);
    menu.add(menuItem);

    menuItem = new JMenuItem("Export ");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_I);
    menu.add(menuItem);

    menuItem = new JMenuItem("Delete ");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_I);
    menu.add(menuItem);

    menuItem = new JMenuItem("Naive ");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_M);
    menu.add(menuItem);

    menuItem = new JMenuItem("Update ");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_I);
    menu.add(menuItem);

    menu.addSeparator();
	menuItem = new JMenuItem("Update All");
	menuItem.addActionListener(menuActionListener);
	menu.add(menuItem);
	menuItem = new JMenuItem("Validate All");
	menuItem.addActionListener(menuActionListener);
	menu.add(menuItem);	
	menuItem = new JMenuItem("Save All");
	menuItem.addActionListener(menuActionListener);
	menu.add(menuItem);	
	menuItem = new JMenuItem("Upload All");
	menuItem.addActionListener(menuActionListener);
	menu.add(menuItem);	
	menuItem = new JMenuItem("Move All");
	menuItem.addActionListener(menuActionListener);
	menu.add(menuItem);		
	menu.addSeparator();
    
    menuItem = new JMenuItem("New", icon);

    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    // menuItem.setAccelerator(KeyStroke.getKeyStroke(
    // KeyEvent.VK_1, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription("Creates a new file");
    menu.add(menuItem);

    icon = createImageIcon(IMAGE_DIR + "open.gif");
    menuItem = new JMenuItem("Open", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_O);
    menu.add(menuItem);

    icon = createImageIcon(IMAGE_DIR + "save.gif");
    menuItem = new JMenuItem("Save", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_S);
    menu.add(menuItem);

    menuItem = new JMenuItem("Save as");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_A);
    menu.add(menuItem);

    //a group of radio button menu items
    menu.addSeparator();
    menuItem = new JMenuItem("Print");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_P);
    menu.add(menuItem);

    //a group of check box menu items
    menu.addSeparator();
    menuItem = new JMenuItem("Exit");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_X);
    menu.add(menuItem);

    //Build edit menu in the menu bar.
    menu = new JMenu("Edit");
    menu.setMnemonic(KeyEvent.VK_E);
    menu.getAccessibleContext().setAccessibleDescription("this is the edit menu");
    menuBar.add(menu);
    icon = createImageIcon(IMAGE_DIR + "undo.gif");
    menuItem = new JMenuItem("Undo", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_U); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("undo");
    menu.add(menuItem);
    icon = createImageIcon(IMAGE_DIR + "redo.gif");
    menuItem = new JMenuItem("Redo", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("redo");
    menu.add(menuItem);
    menu.addSeparator();
    icon = createImageIcon(IMAGE_DIR + "cut.gif");
    menuItem = new JMenuItem("Cut", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("cuts to clipboard");
    menu.add(menuItem);
    icon = createImageIcon(IMAGE_DIR + "copy.gif");
    menuItem = new JMenuItem("Copy", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("copies to clipboard");
    menu.add(menuItem);
    icon = createImageIcon(IMAGE_DIR + "paste.gif");
    menuItem = new JMenuItem("Paste", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("pastes from clipboard");
    menu.add(menuItem);

    menu.addSeparator();
    icon = createImageIcon(IMAGE_DIR + "remove.gif");
    menuItem = new JMenuItem("Delete", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("deletes");
    menu.add(menuItem);
    // insert does nothing at moment
    //icon = createImageIcon(IMAGE_DIR + "add.gif");
    //menuItem = new JMenuItem("Insert", icon);
    //menuItem.addActionListener(menuActionListener);
    //menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    //menuItem.getAccessibleContext().setAccessibleDescription("inserts");
    //menu.add(menuItem);

    /**
    menu = new JMenu("Settings");
    JMenuItem clear = new JMenuItem("Clear Cache");
    clear.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {



      }
    });
    menu.add(clear);
    menuBar.add(menu);
*/
    //Build help menu in the menu bar.
    icon = createImageIcon(IMAGE_DIR + "help.gif");
    menu = new JMenu("Help");
    menu.setMnemonic(KeyEvent.VK_H);
    menu.getAccessibleContext().setAccessibleDescription("this is the help menu");
    menuBar.add(menu);
    menuItem = new JMenuItem("Documentation", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_M); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("documentation");
    menu.add(menuItem);
    menuItem = new JMenuItem("About...");
    menuItem.addActionListener(menuActionListener);
    menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("inserts");
    menu.add(menuItem);

    return menuBar;
  }

  //Create a new internal frame.
  protected void createFrame(File file) {

    DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(file, this, null, null, null, null, null);
    frame.setVisible(true);
    desktop.add(frame);
    try {
      frame.setSelected(true);
    } catch (java.beans.PropertyVetoException e) {
    }

  }

  //Quit the application.
  protected void quit() {
    System.exit(0);
  }

  /**
   * Create the GUI and show it.  For thread safety,
   * this method should be invoked from the
   * event-dispatching thread.
   */
  private static void createAndShowGUI() {
    //Make sure we have nice window decorations.
    JFrame.setDefaultLookAndFeelDecorated(true);

    //Create and set up the window.
    MartEditor frame = new MartEditor();
          
    frame.setTitle(connection);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //ImageIcon icon = createImageIcon(IMAGE_DIR+"MartConfig_cube.gif");
    //frame.setIconImage(icon.getImage());
    //Display the window.
    frame.setVisible(true);
  }

  /** Returns an ImageIcon, or null if the path was invalid. */
  protected static ImageIcon createImageIcon(String path) {
    java.net.URL imgURL = DatasetConfigTreeWidget.class.getClassLoader().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }

  public static void main(String[] args) {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
        }
        createAndShowGUI();
      }
    });
  }

  // Inner class that handles Menu Action Events
  protected class MenuActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand().equals("Cut"))
        cut();
      else if (e.getActionCommand().equals("Copy"))
        copy();
      else if (e.getActionCommand().equals("Paste"))
        paste();
      else if (e.getActionCommand().equals("Insert"))
        insert();
      else if (e.getActionCommand().equals("Delete"))
        delete();
      else if (e.getActionCommand().startsWith("New"))
        newDatasetConfig();
      else if (e.getActionCommand().startsWith("Open"))
        openDatasetConfig();
      else if (e.getActionCommand().equals("Exit"))
        exit();
      else if (e.getActionCommand().equals("Save"))
        save();
      else if (e.getActionCommand().equals("Save as"))
        save_as();
      else if (e.getActionCommand().startsWith("Undo"))
        undo();
      else if (e.getActionCommand().startsWith("Redo"))
        redo();
      else if (e.getActionCommand().startsWith("Database"))
        databaseConnection();
      else if (e.getActionCommand().startsWith("Import"))
        importDatasetConfig();
      else if (e.getActionCommand().startsWith("Export"))
        exportDatasetConfig();
      else if (e.getActionCommand().startsWith("Naive"))
        naiveDatasetConfig();
	  else if (e.getActionCommand().startsWith("Update All"))
		updateAll();
	  else if (e.getActionCommand().startsWith("Validate All"))
		  validateAll();		
	  else if (e.getActionCommand().startsWith("Move All"))
		  moveAll();		
	  else if (e.getActionCommand().startsWith("Save All"))
		  saveAll();	
	  else if (e.getActionCommand().startsWith("Upload All"))
			uploadAll();			  	
      else if (e.getActionCommand().startsWith("Update"))
        updateDatasetConfig();
      else if (e.getActionCommand().startsWith("Delete"))
        deleteDatasetConfig();
      else if (e.getActionCommand().startsWith("hide"))
        makeHidden();

    }
  }

  public void cut() {
    ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).cut();
  }

  public void copy() {
    ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).copy();
  }

  public void paste() {
    ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).paste();
  }

  public void makeHidden() {
    ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).makeHidden();
  }

  public void insert() {
    //((DatasetConfigTreeWidget)desktop.getSelectedFrame()).insert();
  }

  public void delete() {
    ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).delete();
  }

  public void newDatasetConfig() {
    createFrame(null);
  }

  public void setFileChooserPath(File file) {
    this.file = file;
  }

  public File getFileChooserPath() {
    return file;
  }

  public void lostOwnership(Clipboard c, Transferable t) {

  }

  public static DetailedDataSource getDetailedDataSource() {
    return ds;
  }

  public static String getUser() {
    return user;
  }

  public static DatasetConfigXMLUtils getDatasetConfigXMLUtils() {
    return dscutils;
  }

  public static DatabaseDatasetConfigUtils getDatabaseDatasetConfigUtils() {
    return dbutils;
  }
  
  public static DatabaseDatasetConfigUtils getDatabaseDatasetConfigUtilsBySchema(String schema) {
	return (DatabaseDatasetConfigUtils) dbutilsHash.get(schema);
  }

  public void databaseConnection() {

    boolean valid = false;

    try {
      disableCursor();
      while (!valid) {
        if (!databaseDialog.showDialog(this))
          break;

        String defaultSourceName = databaseDialog.getConnectionName();

        if (defaultSourceName == null || defaultSourceName.length() < 1)
          defaultSourceName =
            defaultSourceName =
              DetailedDataSource.defaultName(
                databaseDialog.getHost(),
                databaseDialog.getPort(),
                databaseDialog.getDatabase(),
				databaseDialog.getSchema(),
                databaseDialog.getUser());

        ds =
          new DetailedDataSource(
            databaseDialog.getDatabaseType(),
            databaseDialog.getHost(),
            databaseDialog.getPort(),
            databaseDialog.getDatabase(),
			databaseDialog.getSchema(),
            databaseDialog.getUser(),
            databaseDialog.getPassword(),
            10,
            databaseDialog.getDriver(),
            defaultSourceName);
        user = databaseDialog.getUser();
        database = databaseDialog.getDatabase();
        
        Connection conn = null;
        try {
          conn = ds.getConnection();
          dbutils = new DatabaseDatasetConfigUtils(dscutils, ds);
          valid = true;
          connection = "MartEditor (CONNECTED TO " + databaseDialog.getDatabase() + "/"+databaseDialog.getSchema()+" AS "+databaseDialog.getUser()+")";		  
        } catch (SQLException e) {
          ds = null;	
          connection = "MartEditor (NO DATABASE CONNECTION)";	
          //warning dialog then retry
          Feedback f = new Feedback(this);
          f.warning("Could not connect to Database\nwith the given Connection Settings.\nPlease try again!");
          valid = false;
        } finally {
          DetailedDataSource.close(conn);
        }
      }
    } finally {
      setTitle(connection);
      enableCursor();
    }
  }



  public void openDatasetConfig() {

    XMLFileFilter filter = new XMLFileFilter();
    fc.addChoosableFileFilter(filter);
    int returnVal = fc.showOpenDialog(this.getContentPane());

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      file = fc.getSelectedFile();
      createFrame(file);
      //This is where a real application would open the file.
      System.out.println("Opening: " + file.getName() + "." + newline);
    } else {
      System.out.println("Open command cancelled by user." + newline);
    }

  }

  public void importDatasetConfig() {
    try {
      if (ds == null) {
        JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
        return;
      }

      disableCursor();

      String[] datasets = dbutils.getAllDatasetNames(user);
      String dataset =
        (String) JOptionPane.showInputDialog(
          null,
          "Choose one",
          "Dataset config",
          JOptionPane.INFORMATION_MESSAGE,
          null,
          datasets,
          datasets[0]);

      if (dataset == null)
        return;

      String[] internalNames = dbutils.getAllInternalNamesForDataset(user, dataset);
      String intName;
      if (internalNames.length == 1)
        intName = internalNames[0];
      else {
        intName =
          (String) JOptionPane.showInputDialog(
            null,
            "Choose one",
            "Internal name",
            JOptionPane.INFORMATION_MESSAGE,
            null,
            internalNames,
            internalNames[0]);
      }

      if (intName == null)
        return;

      DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, null, user, dataset, intName, databaseDialog.getSchema());
      frame.setVisible(true);
      desktop.add(frame);
      try {
        frame.setSelected(true);
      } catch (java.beans.PropertyVetoException e) {
      }
    } catch (ConfigurationException e) {
      JOptionPane.showMessageDialog(this, "No datasets available for import - is this a BioMart compatible schema? Absent or empty meta_configuration table?", "ERROR", 0);
    } finally {
      enableCursor();
    }
  }

  public void exportDatasetConfig() {
    if (ds == null) {
      JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
      return;
    }

    try {
      disableCursor();
      DatasetConfig dsConfig = ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).getDatasetConfig();
	  
      if (dsConfig.getAdaptor() != null && dsConfig.getAdaptor().getDataSource() != null && !dsConfig.getAdaptor().getDataSource().getSchema().equals(databaseDialog.getSchema())){
      	// NM the widget still has its adaptor - could switch connection
		int choice = JOptionPane.showConfirmDialog(this,"You are exporting this XML to a new schema: " + databaseDialog.getSchema() +"\nChange connection?", "", JOptionPane.YES_NO_OPTION);
      	if (choice == 0){databaseConnection();}
      }
      String dset = dsConfig.getDataset();
      String intName = dsConfig.getInternalName();

      String dataset =
        (String) JOptionPane.showInputDialog(
          null,
          "Choose one",
          "Dataset config",
          JOptionPane.INFORMATION_MESSAGE,
          null,
          null,
          dset);
      if (dataset == null)
        return;

      String internalName =
        (String) JOptionPane.showInputDialog(
          null,
          "Choose one",
          "Internal name",
          JOptionPane.INFORMATION_MESSAGE,
          null,
          null,
          intName);
      if (internalName == null)
        return;

      dsConfig.setInternalName(internalName);
      dsConfig.setDataset(dataset);


      ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).export();
    } catch (ConfigurationException e) {
      JOptionPane.showMessageDialog(this, "Problems exporting requested dataset. Check that you have write permission " +
      		"and the meta_configuration table is in required format", "ERROR", 0);
      e.printStackTrace();
    } finally {
      enableCursor();
    }
  }

  public void naiveDatasetConfig() {
    if (ds == null) {
      JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
      return;
    }

    
    String dbtype = databaseDialog.getDatabaseType();
    String schema = null;
    
    if(dbtype.equals("oracle")) schema = databaseDialog.getSchema().toUpperCase();
    else schema = databaseDialog.getSchema();
     
    
    try {
      disableCursor();
      String[] datasets = dbutils.getNaiveDatasetNamesFor(schema);
      if(datasets.length==0){
        JOptionPane.showMessageDialog(this, "No datasets available - Is this a BioMart comptatible schema?", "ERROR", 0);
        return;
      }
      
      String dataset =
        (String) JOptionPane.showInputDialog(
          null,
          "Choose one",
          "Dataset",
          JOptionPane.INFORMATION_MESSAGE,
          null,
          datasets,
          datasets[0]);
      if (dataset == null)
        return;

      disableCursor();
     
    
      DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, null, null, dataset, null, schema);

      frame.setVisible(true);
      desktop.add(frame);
      try {
        frame.setSelected(true);
      } catch (java.beans.PropertyVetoException e) {
      }
    } catch (SQLException e) {
    } finally {
      enableCursor();
    }
  }

  public void saveAll() {
  	
  	
	try {
			if (ds == null) {
			  JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
			  return;
			}

			try {
			  disableCursor();
		  	  // choose folder
			  JFileChooser fc = new JFileChooser(getFileChooserPath());
			  
			  fc.setSelectedFile(getFileChooserPath());
			  fc.setDialogTitle("Choose folder to save all XMLs");
			  fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			  fc.showSaveDialog(getContentPane());
		  
			  // cycle through all datasets for the database
			  String[] datasets = dbutils.getAllDatasetNames(user);
			  for (int i = 0; i < datasets.length; i++){
				String dataset = datasets[i];
				String[] internalNames = dbutils.getAllInternalNamesForDataset(user, dataset);
				for (int j = 0; j < internalNames.length; j++){
					String internalName = internalNames[j];
				
					DatasetConfig odsv = null;
					DSConfigAdaptor adaptor = new DatabaseDSConfigAdaptor(MartEditor.getDetailedDataSource(),user, true, false, true);
					DatasetConfigIterator configs = adaptor.getDatasetConfigs();
					while (configs.hasNext()){
						DatasetConfig lconfig = (DatasetConfig) configs.next();
						if (lconfig.getDataset().equals(dataset) && lconfig.getInternalName().equals(internalName)){
								odsv = lconfig;
								break;
						}
					}
				
					// save osdv each one to a separate file <internalname>.xml
					try {
						File newFile = new File(fc.getSelectedFile().getPath() + "/" + odsv.getDataset() + ".xml");
						URLDSConfigAdaptor.StoreDatasetConfig(odsv, newFile);
							setFileChooserPath(fc.getSelectedFile());
					} catch (Exception e) {
							e.printStackTrace();
					}
				  				
				}	
			  } 
			} catch (Exception e) {
			  e.printStackTrace();
			}
		  } finally {
			enableCursor();
		  }

  }


  public void uploadAll() {
  	
  	
	try {
			if (ds == null) {
			  JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
			  return;
			}

			try {
			  disableCursor();
			  // choose folder
			  JFileChooser fc = new JFileChooser(getFileChooserPath());
			  fc.setSelectedFile(getFileChooserPath());
			  fc.setDialogTitle("Choose file(s) to upload to database: WARNING - THIS WILL OVERWRITE EXISTING XMLS IN THE DATABASE");
		  	  fc.setMultiSelectionEnabled(true);			  
			  XMLFileFilter filter = new XMLFileFilter();
			  fc.addChoosableFileFilter(filter);
			  int returnVal = fc.showOpenDialog(getContentPane());
			  if (returnVal == JFileChooser.APPROVE_OPTION) {
			     
				 //file = fc.getSelectedFile();// works
				 	
			     // cycle through all dataset files	
			     File[] files = fc.getSelectedFiles();
			  
			     for (int i = 0; i < files.length; i++){
				  file = files[i];
				  			  
				  URL url = file.toURL();
				  //ignoreCache, includeHiddenMembers
				  DSConfigAdaptor adaptor = new URLDSConfigAdaptor(url,true, true);
				  DatasetConfig odsv  = (DatasetConfig) adaptor.getDatasetConfigs().next();
				  
				  // export osdv
				  try {
						dbutils.storeDatasetConfiguration(
									MartEditor.getUser(),
									odsv.getInternalName(),
									odsv.getDisplayName(),
									odsv.getDataset(),
									odsv.getDescription(),
									MartEditor.getDatasetConfigXMLUtils().getDocumentForDatasetConfig(odsv),
									true,
									odsv.getType(),
									odsv.getVisible(),
									odsv.getVersion(),
									odsv);
				   } catch (Exception e) {
							e.printStackTrace();
				   }
			     } 				
			  } 
			} catch (Exception e) {
			  e.printStackTrace();
			}
		  } finally {
			enableCursor();
		  }
  }


  public void moveAll() {  	
	try {
			if (ds == null) {
			  JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
			  return;
			}
			try {
			  disableCursor();
			  
			  DSConfigAdaptor adaptor = new DatabaseDSConfigAdaptor(MartEditor.getDetailedDataSource(),user, true, false, true);
			  DatasetConfigIterator configs = adaptor.getDatasetConfigs();
						   
			  
			 		  
			  
			  Set retSet = new HashSet();
			  //int k = 0;
			  while (configs.hasNext()){
					DatasetConfig lconfig = (DatasetConfig) configs.next();
					retSet.add(lconfig);
		      }
			  DatasetConfig[] dsConfigs = new DatasetConfig[retSet.size()];
			  retSet.toArray(dsConfigs);
		      
		      
			  // connect to database to export to
	    	  databaseConnection();
							
			  DatasetConfig dsv = null;
			  for (int k = 0; k < dsConfigs.length;k++){
					dsv = dsConfigs[k];
					// export it to new database	
					dbutils.storeDatasetConfiguration(
										MartEditor.getUser(),
										dsv.getInternalName(),
										dsv.getDisplayName(),
										dsv.getDataset(),
										dsv.getDescription(),
										MartEditor.getDatasetConfigXMLUtils().getDocumentForDatasetConfig(dsv),
										true,
										dsv.getType(),
										dsv.getVisible(),
										dsv.getVersion(),
										dsv);						   		
			  }	
			} 
			catch (Exception e) {
			  e.printStackTrace();
			}
     } finally {
			enableCursor();
     }
}

  public void updateAll() {
	  try {
		if (ds == null) {
		  JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
		  return;
		}

		try {
		  disableCursor();
		  
		  // cycle through all datasets for the database
		  String[] datasets = dbutils.getAllDatasetNames(user);
		  for (int i = 0; i < datasets.length; i++){
		  	String dataset = datasets[i];
			String[] internalNames = dbutils.getAllInternalNamesForDataset(user, dataset);
			for (int j = 0; j < internalNames.length; j++){
				String internalName = internalNames[j];
				
				DatasetConfig odsv = null;
				DSConfigAdaptor adaptor = new DatabaseDSConfigAdaptor(MartEditor.getDetailedDataSource(),user, true, false, true);
				DatasetConfigIterator configs = adaptor.getDatasetConfigs();
				while (configs.hasNext()){
					DatasetConfig lconfig = (DatasetConfig) configs.next();
					if (lconfig.getDataset().equals(dataset) && lconfig.getInternalName().equals(internalName)){
							odsv = lconfig;
							break;
					}
				}
				
				//DatasetConfig odsv = dbutils.getDatasetConfigByDatasetInternalName(user, dataset, internalName);
				// update it
				DatasetConfig dsv = dbutils.getValidatedDatasetConfig(odsv);
				
				// test if version need updating and newVersion++ if so
				String datasetVersion = dsv.getVersion();
				String newDatasetVersion = dbutils.getNewVersion(dsv.getDataset());
				if (datasetVersion != null && datasetVersion != "" && !datasetVersion.equals(newDatasetVersion)){
					dsv.setVersion(newDatasetVersion);
				}
				// repeat logic for linkVersions updating any not null or '' or equal to newLinkVersion
				dbutils.updateLinkVersions(dsv);					
				
				dsv = dbutils.getNewFiltsAtts(database, dsv);
				// export it	
				dbutils.storeDatasetConfiguration(
							MartEditor.getUser(),
							dsv.getInternalName(),
							dsv.getDisplayName(),
							dsv.getDataset(),
							dsv.getDescription(),
							MartEditor.getDatasetConfigXMLUtils().getDocumentForDatasetConfig(dsv),
							true,
							dsv.getType(),
							dsv.getVisible(),
							dsv.getVersion(),
							dsv);
					
				// display it if new atts or filts for further editing	
				if ((dsv.getAttributePageByInternalName("new_attributes") != null) ||
				    (dsv.getFilterPageByName("new_filters") != null)){
					DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, dsv, null, null, null, database);
					frame.setVisible(true);
					desktop.add(frame);
					try {
						frame.setSelected(true);
					} catch (java.beans.PropertyVetoException e) {
					}
				}			
			}	
		  } 
		} catch (Exception e) {
		  e.printStackTrace();
		}
	  } finally {
		enableCursor();
	  }
  }
  
  public void validateAll() {
	  try {
		if (ds == null) {
		  JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
		  return;
		}

		try {
		  disableCursor();
		  String duplicationString = "";
		  String brokenString = "";
		  String spaceErrors = "";
		  int newVersion;
		  // cycle through all datasets for the database
		  String[] datasets = dbutils.getAllDatasetNames(user);
		  DSConfigAdaptor adaptor;
		  DatasetConfig dsv, odsv, adsv;
		  for (int i = 0; i < datasets.length; i++){
			String dataset = datasets[i];
			System.out.println("VALIDATING " + dataset);
			String[] internalNames = dbutils.getAllInternalNamesForDataset(user, dataset);
			for (int j = 0; j < internalNames.length; j++){
				String internalName = internalNames[j];
				
				dsv = null;
				adaptor = new DatabaseDSConfigAdaptor(MartEditor.getDetailedDataSource(),user, true, false, true);
				DatasetConfigIterator configs = adaptor.getDatasetConfigs();
				while (configs.hasNext()){
					DatasetConfig lconfig = (DatasetConfig) configs.next();
					if (lconfig.getDataset().equals(dataset) && lconfig.getInternalName().equals(internalName)){
							dsv = lconfig;
							break;
					}
				}
			
				newVersion = 0;
				// test if version need updating and newVersion++ if so
				String datasetVersion = dsv.getVersion();
				String newDatasetVersion = dbutils.getNewVersion(dsv.getDataset());
				if (newDatasetVersion != null && datasetVersion != null && datasetVersion != "" && !datasetVersion.equals(newDatasetVersion)){
					dsv.setVersion(newDatasetVersion);
					newVersion++;
				}
				//System.out.println(newVersion + "\t" + newDatasetVersion);
				// repeat logic for linkVersions updating any not null or '' or equal to newLinkVersion
				if (dbutils.updateLinkVersions(dsv))					
					newVersion++;
				
				
				//System.out.println(newVersion + "\t" + newDatasetVersion);
				//BELOW MAKES NO SENSE
				if (dbutils.getBrokenElements(dsv) != "") 
					brokenString = brokenString + dbutils.getBrokenElements(dsv);
		
		
				dsv = dbutils.getNewFiltsAtts(database, dsv);
				
				// check uniqueness of internal names per page	  
				AttributePage[] apages = dsv.getAttributePages();
				AttributePage apage;
				String testInternalName;
				
	  
				for (int k = 0; k < apages.length; k++){
					  apage = apages[k];
					  Hashtable descriptionsMap = new Hashtable();
					  if ((apage.getHidden() != null) && (apage.getHidden().equals("true"))){
						  continue;
					  }
		    
					  List testAtts = new ArrayList();
					  testAtts = apage.getAllAttributeDescriptions();
					  for (Iterator iter = testAtts.iterator(); iter.hasNext();) {
						  Object testAtt = iter.next();
						  AttributeDescription testAD = (AttributeDescription) testAtt;
						  if ((testAD.getHidden() != null) && (testAD.getHidden().equals("true"))){
							  continue;
						  }
						  if (testAD.getInternalName().matches("\\w+\\.\\w+")){
							  continue;//placeholder atts can be duplicated	
						  }
						  if (testAD.getInternalName().matches("\\w+\\s+\\w+")){
							 spaceErrors = spaceErrors + testAD.getInternalName() + " in dataset " + dsv.getDataset() + "\n";
						  }					
						  if (descriptionsMap.containsKey(testAD.getInternalName())){
							  duplicationString = duplicationString + testAD.getInternalName() + " in dataset " + dsv.getDataset() + "\n";
						  }
						  descriptionsMap.put(testAD.getInternalName(),"1");
					  }
				}
				// repeat for filter pages
				FilterPage[] fpages = dsv.getFilterPages();
				FilterPage fpage;
				for (int k = 0; k < fpages.length; k++){
							fpage = fpages[k];
							Hashtable descriptionsMap = new Hashtable();
							if ((fpage.getHidden() != null) && (fpage.getHidden().equals("true"))){
								continue;
							}
					       
							List testAtts = new ArrayList();
							testAtts = fpage.getAllFilterDescriptions();// ? OPTIONS
				  
							for (Iterator iter = testAtts.iterator(); iter.hasNext();) {
								Object testAtt = iter.next();
								FilterDescription testAD = (FilterDescription) testAtt;
								if ((testAD.getHidden() != null) && (testAD.getHidden().equals("true"))){
									  continue;
								}
								if (testAD.getInternalName().matches("\\w+\\s+\\w+")){
									 spaceErrors = spaceErrors + testAD.getInternalName() + " in dataset " + dsv.getDataset() + "\n";
								}	
								if (descriptionsMap.containsKey(testAD.getInternalName())){
									duplicationString = duplicationString + testAD.getInternalName() + " in dataset " + dsv.getDataset() + "\n";
						  
									continue;//to stop options also being assessed
								}
								descriptionsMap.put(testAD.getInternalName(),"1");
					  
								// do options as well
								Option[] ops = testAD.getOptions();
								if (ops.length > 0 && ops[0].getType()!= null && !ops[0].getType().equals("")){
								  for (int l = 0; l < ops.length; l++){
									  Option op = ops[l];
									  if ((op.getHidden() != null) && (op.getHidden().equals("true"))){
											  continue;
									  }
									  if (descriptionsMap.containsKey(op.getInternalName())){
										  duplicationString = duplicationString + op.getInternalName() + " in dataset " + dsv.getDataset() + "\n";
								
									  }
									  descriptionsMap.put(op.getInternalName(),"1");
								  }
								}
							}
				}
	  
				
				// display it if new atts or filts for further editing	
				if (newVersion != 0 || (dsv.getAttributePageByInternalName("new_attributes") != null && (dsv.getAttributePageByInternalName("new_attributes").getHidden() == null || dsv.getAttributePageByInternalName("new_attributes").getHidden().equals("false"))) ||
					(dsv.getFilterPageByName("new_filters") != null && (dsv.getFilterPageByName("new_filters").getHidden() == null || dsv.getFilterPageByName("new_filters").getHidden().equals("false")))){
				
					DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, dsv, null, null, null, database);
					frame.setVisible(true);
					desktop.add(frame);
					try {
						frame.setSelected(true);
					} catch (java.beans.PropertyVetoException e) {
					}
				}
				//else{
					// make sure dsv memory freed
				//	DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, dsv, null, null, null, database);
				//	frame.dispose();
				//}
					
			}
		  }
		  
		    if (spaceErrors != ""){
			 	JOptionPane.showMessageDialog(null, "The following internal names contain spaces:\n"
									  + spaceErrors, "ERROR", 0);
				return;//no export performed
		  	}
			if (duplicationString != ""){
			  JOptionPane.showMessageDialog(this, "The following internal names are duplicated and will cause client problems:\n"
								  + duplicationString, "ERROR", 0);
				  
			}
			//if (brokenString.matches("\\w+")){
			if (brokenString != ""){
					JOptionPane.showMessageDialog(this, "The following internal names are broken\n"
											  + brokenString, "ERROR", 0);
				  
			} 
		} catch (Exception e) {
		  e.printStackTrace();
		}
	  } finally {
		enableCursor();
	  }
  }  
  
  
  public void updateDatasetConfig() {
    try {
      if (ds == null) {
        JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
        return;
      }

      try {
        disableCursor();
        // check whether existing filters and atts are still in database
        Object selectedFrame = desktop.getSelectedFrame();

        if (selectedFrame == null) {
          JOptionPane.showMessageDialog(this, "Nothing to Update, please Import a DatasetConfig", "ERROR", 0);
          return;
        }

        DatasetConfig odsv = ((DatasetConfigTreeWidget) selectedFrame).getDatasetConfig();

        if (odsv == null) {
          JOptionPane.showMessageDialog(this, "Nothing to Update, please Import a DatasetConfig", "ERROR", 0);
          return;
        }

        DatasetConfig dsv = dbutils.getValidatedDatasetConfig(odsv);
        // check for new tables and cols
        dsv = dbutils.getNewFiltsAtts(database, dsv);
		// test if version need updating
		String datasetVersion = dsv.getVersion();
		String newDatasetVersion = dbutils.getNewVersion(dsv.getDataset());
		if (datasetVersion != null && datasetVersion != "" && !datasetVersion.equals(newDatasetVersion))
			dsv.setVersion(newDatasetVersion);
							
		dbutils.updateLinkVersions(dsv);
		
		
        DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, dsv, null, null, null, database);
        frame.setVisible(true);
        desktop.add(frame);
        try {
          frame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } finally {
      enableCursor();
    }
  }

  public void deleteDatasetConfig() {

    try {
      if (ds == null) {
        JOptionPane.showMessageDialog(this, "Connect to database first", "ERROR", 0);
        return;
      }

      try {
        disableCursor();
        String[] datasets = dbutils.getAllDatasetNames(user);
        String dataset =
          (String) JOptionPane.showInputDialog(
            null,
            "Choose one",
            "Dataset Config",
            JOptionPane.INFORMATION_MESSAGE,
            null,
            datasets,
            datasets[0]);
        if (dataset == null)
          return;
        String[] internalNames = dbutils.getAllInternalNamesForDataset(user, dataset);
        String intName;
        if (internalNames.length == 1)
          intName = internalNames[0];
        else {
          intName =
            (String) JOptionPane.showInputDialog(
              null,
              "Choose one",
              "Internal name",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              internalNames,
              internalNames[0]);
        }
        if (intName == null)
          return;
        dbutils.deleteDatasetConfigsForDatasetIntName(dataset, intName, user);

      } catch (ConfigurationException e) {
      }
    } finally {
      enableCursor();
    }
  }

  public void save() {
    ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).save();
  }

  public void save_as() {
    ((DatasetConfigTreeWidget) desktop.getSelectedFrame()).save_as();
  }

  public void exit() {
    System.exit(0);
  }

  public void undo() {

  }

  public void redo() {

  }

  private void enableCursor() {
    setCursor(Cursor.getDefaultCursor());
    getGlassPane().setVisible(false);
  }

  private void disableCursor() {
    getGlassPane().setVisible(true);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }
}
