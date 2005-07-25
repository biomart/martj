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

package org.ensembl.mart.builder;

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
import java.io.IOException;
import java.sql.Connection;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.ensembl.mart.explorer.Feedback;
import org.ensembl.mart.guiutils.DatabaseSettingsDialog;

import org.ensembl.mart.builder.lib.ConfigurationAdaptor;
import org.ensembl.mart.builder.lib.DatabaseAdaptor;
import org.ensembl.mart.builder.lib.MetaDataAdaptor;
import org.ensembl.mart.builder.lib.MetaDataAdaptorFKNotSupported;
import org.ensembl.mart.builder.lib.MetaDataAdaptorFKSupported;
import org.ensembl.mart.builder.lib.TransformationConfig;


/**
 * Class MartBuilder extends JFrame..
 *
 * <p>This class contains the main function, it draws the external frame, toolsbar, menus.
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.builder.lib.TransformationConfig
 */

public class MartBuilder extends JFrame implements ClipboardOwner {

  private JDesktopPane desktop;
  static final private String data_dir = "data/builder/";
  
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
 
  //private static Hashtable dbutilsHash = new Hashtable();
  

  static private String user;
  private String database;
  private String schema;
  private static String connection;
  private static MetaDataAdaptor resolver;  

  /** Persistent preferences object used to hold user history. */
  private Preferences prefs = Preferences.userNodeForPackage(this.getClass());
  private DatabaseSettingsDialog databaseDialog = new DatabaseSettingsDialog(prefs);
  private DatabaseAdaptor adaptor;
  protected Clipboard clipboardEditor;

  public MartBuilder() {
	
  	//	autoconnect on startup
  	super("MartBuilder");
			     
  	adaptor = new DatabaseAdaptor(databaseDialog.getDriver(),
	                              databaseDialog.getUser(),
							      databaseDialog.getPassword(),
								  databaseDialog.getHost(),
								  databaseDialog.getPort(),
								  databaseDialog.getDatabase(),
								  databaseDialog.getSchema(),
							      "");
    Connection conn = adaptor.getCon();
	connection = "MartBuilder (CONNECTED TO " + databaseDialog.getDatabase() + "/"+databaseDialog.getSchema()+" AS "+databaseDialog.getUser()+")";
				   
	user = databaseDialog.getUser();
	database = databaseDialog.getDatabase();  
				   
    JFrame.setDefaultLookAndFeelDecorated(true);
    fc = new JFileChooser();

    //Make the big window be indented 50 pixels from each edge
    //of the screen.
    int inset = 100;
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2);

    //Set up the GUI.
    this.getContentPane().setLayout(new BorderLayout());

    desktop = new JDesktopPane();
    
    this.getContentPane().add(desktop, BorderLayout.CENTER);
    setJMenuBar(createMenuBar());
	
    //Make dragging a little faster but perhaps uglier.
    desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    
    clipboardEditor = new Clipboard("editor_clipboard");
  }

  protected JMenuBar createMenuBar() {
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem menuItem;

    //Create the menu bar.
    menuBar = new JMenuBar();

    //Build the first menu.
    menu = new JMenu("File");
    menu.getAccessibleContext().setAccessibleDescription("the file related menu");
    menuBar.add(menu);

    //a group of JMenuItems
    ImageIcon icon = createImageIcon(IMAGE_DIR + "new.gif");

    menuItem = new JMenuItem("Database Connection ");
    MartBuilder.MenuActionListener menuActionListener = new MartBuilder.MenuActionListener();
    menuItem.addActionListener(menuActionListener);
    menu.add(menuItem);

    menu.addSeparator();

    menuItem = new JMenuItem("Create TransformationConfig");
    menuItem.addActionListener(menuActionListener);
    menu.add(menuItem);

    icon = createImageIcon(IMAGE_DIR + "open.gif");
    menuItem = new JMenuItem("Open", icon);
    menuItem.addActionListener(menuActionListener);
    menu.add(menuItem);

    icon = createImageIcon(IMAGE_DIR + "save.gif");
    menuItem = new JMenuItem("Save", icon);
    menuItem.addActionListener(menuActionListener);
    menu.add(menuItem);

    menuItem = new JMenuItem("Save as");
    menuItem.addActionListener(menuActionListener);
    menu.add(menuItem);

	menuItem = new JMenuItem("Create DDL");
	menuItem.addActionListener(menuActionListener);
	menu.add(menuItem);

    //a group of check box menu items
    menu.addSeparator();
    menuItem = new JMenuItem("Exit");
    menuItem.addActionListener(menuActionListener);
    //menuItem.setMnemonic(KeyEvent.VK_X);
    menu.add(menuItem);

    //Build edit menu in the menu bar.
    menu = new JMenu("Edit");
    //menu.setMnemonic(KeyEvent.VK_E);
    menu.getAccessibleContext().setAccessibleDescription("this is the edit menu");
    menuBar.add(menu);
    icon = createImageIcon(IMAGE_DIR + "undo.gif");
    menuItem = new JMenuItem("Undo", icon);
    menuItem.addActionListener(menuActionListener);
    //menuItem.setMnemonic(KeyEvent.VK_U); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("undo");
    //menu.add(menuItem);
    icon = createImageIcon(IMAGE_DIR + "redo.gif");
    menuItem = new JMenuItem("Redo", icon);
    menuItem.addActionListener(menuActionListener);
    //menuItem.setMnemonic(KeyEvent.VK_N); //used constructor instead
    menuItem.getAccessibleContext().setAccessibleDescription("redo");
    //menu.add(menuItem);
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

    //Build help menu in the menu bar.
    icon = createImageIcon(IMAGE_DIR + "help.gif");
    menu = new JMenu("Help");
    //menu.setMnemonic(KeyEvent.VK_H);
    menu.getAccessibleContext().setAccessibleDescription("this is the help menu");
    menuBar.add(menu);
    menuItem = new JMenuItem("Documentation", icon);
    menuItem.addActionListener(menuActionListener);
    menuItem.getAccessibleContext().setAccessibleDescription("documentation");
    menu.add(menuItem);
    menuItem = new JMenuItem("About...");
    menuItem.addActionListener(menuActionListener);
    menuItem.getAccessibleContext().setAccessibleDescription("inserts");
    menu.add(menuItem);

    return menuBar;
  }

  //Create a new internal frame.
  protected void createFrame(String file) {

    TransformationConfigTreeWidget frame = new TransformationConfigTreeWidget(file, this, null, null, null, null, null);
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
    MartBuilder frame = new MartBuilder();
    frame.setTitle(connection);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }

  /** Returns an ImageIcon, or null if the path was invalid. */
  protected static ImageIcon createImageIcon(String path) {
    java.net.URL imgURL = TransformationConfigTreeWidget.class.getClassLoader().getResource(path);
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
  protected class MenuActionListener implements ActionListener{
    public void actionPerformed(ActionEvent e){
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
      else if (e.getActionCommand().startsWith("Open"))
        openTransformationConfig();
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
      else if (e.getActionCommand().startsWith("Create TransformationConfig")){
        //naiveTransformationConfig();
      }
	  else if (e.getActionCommand().startsWith("Create DDL")){
		createDDL();
	  }     
    }
  }

  public void cut() {
    ((TransformationConfigTreeWidget) desktop.getSelectedFrame()).cut();
  }

  public void copy() {
    ((TransformationConfigTreeWidget) desktop.getSelectedFrame()).copy();
  }

  public void paste() {
    ((TransformationConfigTreeWidget) desktop.getSelectedFrame()).paste();
  }


  public void insert() {
    //((TransformationConfigTreeWidget)desktop.getSelectedFrame()).insert();
  }

  public void delete() {
    ((TransformationConfigTreeWidget) desktop.getSelectedFrame()).delete();
  }

  public void setFileChooserPath(File file) {
    this.file = file;
  }

  public File getFileChooserPath() {
    return file;
  }

  public void lostOwnership(Clipboard c, Transferable t) {

  }

  public void databaseConnection() {

    boolean valid = false;

    try {
      disableCursor();
      while (!valid) {
        if (!databaseDialog.showDialog(this))
          break;

        try {
			adaptor = new DatabaseAdaptor(databaseDialog.getDriver(),
										  databaseDialog.getUser(),
									      databaseDialog.getPassword(),
								          databaseDialog.getHost(),
										  databaseDialog.getPort(),
										  databaseDialog.getDatabase(),
										  databaseDialog.getSchema(),
										  ""	                                                            
										  );
			Connection conn = adaptor.getCon();
			connection = "MartBuilder (CONNECTED TO " + databaseDialog.getDatabase() + "/"+databaseDialog.getSchema()+" AS "+databaseDialog.getUser()+")";
            valid = true;	
			user = databaseDialog.getUser();
	    	database = databaseDialog.getDatabase();

	  	} catch (Exception e) {
				adaptor = null;	
				connection = "MartBuilder (NO DATABASE CONNECTION)";	
				//warning dialog then retry
				Feedback f = new Feedback(this);
				f.warning("Could not connect to Database\nwith the given Connection Settings.\nPlease try again!");
				valid = false;
		}

      }
    } finally {
      setTitle(connection);
      enableCursor();
    }
  }



  public void openTransformationConfig() {

    XMLFileFilter filter = new XMLFileFilter();
    fc.addChoosableFileFilter(filter);
    int returnVal = fc.showOpenDialog(this.getContentPane());

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      file = fc.getSelectedFile();
      createFrame(file.toString());
      //This is where a real application would open the file.
      System.out.println("Opening: " + file.getName() + ".\n");
    } else {
      System.out.println("Open command cancelled by user.\n");
    }

  }

  public void createDDL(){
  	
   try{
	String config_info = "";
	TransformationConfig tConfig = null;
	String ddlFile = null;
	String tSchemaName = null;    

	if (adaptor.rdbms.equals("mysql")) {
		resolver = new MetaDataAdaptorFKNotSupported(adaptor);
	} else if (adaptor.rdbms.equals("oracle")) {
		resolver = new MetaDataAdaptorFKSupported(adaptor);
	} else if (adaptor.rdbms.equals("postgresql")) {
		resolver = new MetaDataAdaptorFKSupported(adaptor);
	}

	System.out.println("TARG SCHEMA");
	tSchemaName = JOptionPane.showInputDialog(null,"INPUT TARGET SCHEMA");
	
	// read the XML from the open frame rather than inputing a file
	disableCursor();
    Object selectedFrame = desktop.getSelectedFrame();

	if (selectedFrame == null) {
		JOptionPane.showMessageDialog(this, "Nothing to read, please import a TransformationConfig", "ERROR", 0);
		return;
	}

	tConfig = ((TransformationConfigTreeWidget) selectedFrame).getTransformationConfig();
	
	ddlFile = JOptionPane.showInputDialog(null,"OUTPUT DDL>> FILE:");
	File sFile = new File(ddlFile);
	sFile.delete();// does not seem to be working
		
	ConfigurationAdaptor configAdaptor = new ConfigurationAdaptor();
	configAdaptor.adaptor=adaptor;
	configAdaptor.resolver=resolver;
	configAdaptor.targetSchemaName=tSchemaName;	
	configAdaptor.readXMLConfiguration(tConfig);
	configAdaptor.writeDDL(ddlFile);
		
	System.out.println ("\nWritten DDLs to: "+ddlFile);
  	}
  	catch(IOException e){
  		System.out.println("IO Exception:" + e.toString());
  	}
  }
  
  public void save() {
    ((TransformationConfigTreeWidget) desktop.getSelectedFrame()).save();
  }

  public void save_as() {
    ((TransformationConfigTreeWidget) desktop.getSelectedFrame()).save_as();
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
