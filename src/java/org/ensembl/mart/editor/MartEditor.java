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
  private static DatasetConfigXMLUtils dscutils = new DatasetConfigXMLUtils(false, true);
  //may want to turn validation on?
  private static DatabaseDatasetConfigUtils dbutils;

  static private String user;
  private String database;
  /** Persistent preferences object used to hold user history. */
  private Preferences prefs = Preferences.userNodeForPackage(this.getClass());
  private DatabaseSettingsDialog databaseDialog = new DatabaseSettingsDialog(prefs);

  protected Clipboard clipboardEditor;

  public MartEditor() {
    super("MartEditor");
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

	// autoconnect on startup
	String defaultSourceName = databaseDialog.getConnectionName();

			if (defaultSourceName == null || defaultSourceName.length() < 1)
			  defaultSourceName =
				defaultSourceName =
				  DetailedDataSource.defaultName(
					databaseDialog.getHost(),
					databaseDialog.getPort(),
					databaseDialog.getDatabase(),
					databaseDialog.getUser());

			ds =
			  new DetailedDataSource(
				databaseDialog.getDatabaseType(),
				databaseDialog.getHost(),
				databaseDialog.getPort(),
				databaseDialog.getDatabase(),
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
			  //valid = true;
			} catch (SQLException e) {
			  System.out.println(e.toString()); 	
			  //warning dialog then retry
			  //Feedback f = new Feedback(this);
			  //f.warning("Could not connect to Database\nwith the given Connection Settings.\nPlease try again!");
			  //valid = false;
			} finally {
			  DetailedDataSource.close(conn);
			}


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
                databaseDialog.getUser());

        ds =
          new DetailedDataSource(
            databaseDialog.getDatabaseType(),
            databaseDialog.getHost(),
            databaseDialog.getPort(),
            databaseDialog.getDatabase(),
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
        } catch (SQLException e) {
          //warning dialog then retry
          Feedback f = new Feedback(this);
          f.warning("Could not connect to Database\nwith the given Connection Settings.\nPlease try again!");
          valid = false;
        } finally {
          DetailedDataSource.close(conn);
        }
      }
    } finally {
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

      DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, null, user, dataset, intName, null);
      frame.setVisible(true);
      desktop.add(frame);
      try {
        frame.setSelected(true);
      } catch (java.beans.PropertyVetoException e) {
      }
    } catch (ConfigurationException e) {
      JOptionPane.showMessageDialog(this, "Could not import requested Dataset", "ERROR", 0);
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

	  // check uniqueness of internal names per page	  
	  AttributePage[] apages = dsConfig.getAttributePages();
	  AttributePage apage;
	  String testInternalName;
	  String duplicationString = "";
	  
	  for (int i = 0; i < apages.length; i++){
	  		apage = apages[i];
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
				
				if (descriptionsMap.containsKey(testAD.getInternalName())){
					//System.out.println("DUPLICATION " + testAD.getInternalName());	
					duplicationString = duplicationString + testAD.getInternalName() + " in page " + apage.getInternalName() + "\n";
					
				}
				descriptionsMap.put(testAD.getInternalName(),"1");
		    }
	  }
	  // repeat for filter pages
	  FilterPage[] fpages = dsConfig.getFilterPages();
	  FilterPage fpage;
	  for (int i = 0; i < fpages.length; i++){
				  fpage = fpages[i];
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
					  if (descriptionsMap.containsKey(testAD.getInternalName())){
						  //System.out.println("DUPLICATION " + testAD.getInternalName());
						  duplicationString = duplicationString + testAD.getInternalName() + " in page " + fpage.getInternalName() + "\n";
						  
						  continue;//to stop options also being assessed
					  }
					  descriptionsMap.put(testAD.getInternalName(),"1");
					  
					  // do options as well
					  Option[] ops = testAD.getOptions();
					  if (ops.length > 0 && ops[0].getType()!= null ){
						for (int j = 0; j < ops.length; j++){
							Option op = ops[j];
							if ((op.getHidden() != null) && (op.getHidden().equals("true"))){
									continue;
							}
							if (descriptionsMap.containsKey(op.getInternalName())){
								//System.out.println("DUPLICATION " + op.getInternalName());
								duplicationString = duplicationString + op.getInternalName() + " in page " + fpage.getInternalName() + "\n";
								
							}
						    descriptionsMap.put(op.getInternalName(),"1");
						}
					  }
				  }
	  }
	  
	  if (duplicationString != ""){
		JOptionPane.showMessageDialog(this, "The following internal names are duplicated and will cause client problems:\n"
							+ duplicationString, "ERROR", 0);
	  	//return;//leave as a warning for now otherwise naive doesn't work
	  }

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

    try {
      disableCursor();
      String[] datasets = dbutils.getNaiveDatasetNamesFor(database);
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
      // for oracle. arrayexpress: needs user, not instance
      String dbtype = databaseDialog.getDatabaseType();
      String qdb= null;

      if (dbtype.startsWith("oracle"))
        qdb = user.toUpperCase(); // not sure why needs uppercase
      if (dbtype.equals("mysql")) qdb = database;
      
      // There is some confusion wrt to schema/db names across platforms.
      // For postgres we set it to an unrestricted search ei null.
      //  DatasetConfigTreeWidget relies on some db null, user null etc logic
      // the type is temporarily set to pgsql until sorted.
      
      if (dbtype.equals("postgresql")) qdb = "pgsql";
      
      DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null, this, null, null, dataset, null, qdb);

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
							dsv.getVersion());
					
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
