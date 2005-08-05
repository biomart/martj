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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.Box;
import javax.swing.BoxLayout;

import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.ensembl.mart.explorer.Feedback;
import org.ensembl.mart.guiutils.DatabaseSettingsDialog;

import org.ensembl.mart.builder.lib.*;


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
  
  private ArrayList tableList; 

  private HashMap refColNames = new HashMap();
  private HashMap refColAliases = new HashMap();
  //private HashMap cenColNames = new HashMap();
  //private HashMap cenColAliases = new HashMap();
  private String cenColName;
  private String cenColAlias;	

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

	menu.addSeparator();

	menuItem = new JMenuItem("Create TransformationConfig");
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
        createConfig();
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

  private void createConfig(){
  	try{
		if (adaptor.rdbms.equals("mysql")) {
			resolver = new MetaDataAdaptorFKNotSupported(adaptor);
		} else if (adaptor.rdbms.equals("oracle")) {
			resolver = new MetaDataAdaptorFKSupported(adaptor);
		} else if (adaptor.rdbms.equals("postgresql")) {
			resolver = new MetaDataAdaptorFKSupported(adaptor);
		}
	
		String[] dialogOptions = new String[] {"Continue","Select columns","Cancel"};
		String[] standardOptions = new String[] {"OK","Cancel"};
		String[] includeCentralFilterOptions = new String[] {"N","Y"};
		
		cenColName = "";
		cenColAlias = "";
		
		Box initialSettings = new Box(BoxLayout.Y_AXIS);
		initialSettings.add(Box.createRigidArea(new Dimension(400,1)));		
		Box box1 = new Box(BoxLayout.X_AXIS);
		Box box2 = new Box(BoxLayout.X_AXIS);
		Box box3 = new Box(BoxLayout.X_AXIS);
		
		JLabel label1 = new JLabel("Transformation config name");		
		box1.add(label1);	
		JTextField tConfigNameField = new JTextField(10);
		box1.add(tConfigNameField);
		initialSettings.add(box1);
		JLabel label2 = new JLabel("Dataset name");
		box2.add(label2);
		JTextField datasetNameField = new JTextField(10);
		box2.add(datasetNameField);
		initialSettings.add(box2);
		JLabel label3 = new JLabel("Output file");
		box3.add(label3);
		JTextField outputFileField = new JTextField(10);
		box3.add(outputFileField);
		initialSettings.add(box3);
		
		int option = JOptionPane.showOptionDialog(
				this,
				initialSettings,
				"Initial Settings",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				standardOptions,
				null);
		
		String tConfigName = tConfigNameField.getText();
		String datasetName = datasetNameField.getText();
		String xmlFile     = outputFileField.getText();
	
		// create the config
		TransformationConfig tConfig = new TransformationConfig();		
		tConfig.getElement().setAttribute("internalName",tConfigName);		
		// add the dataset - ? more than one
		Dataset dataset = new Dataset();
		dataset.getElement().setAttribute("internalName",datasetName);
		
		
		// go through the tables 
		
		int transformationCount = 0;
		String[] potentialTables = resolver.getAllTableNames();
		
		Box centralSettings = new Box(BoxLayout.Y_AXIS);
		centralSettings.add(Box.createRigidArea(new Dimension(400,1)));		
		box1 = new Box(BoxLayout.X_AXIS);
		box2 = new Box(BoxLayout.X_AXIS);
		box3 = new Box(BoxLayout.X_AXIS);
				
		label1 = new JLabel("Main table name");
		box1.add(label1);
		JComboBox tableNameBox = new JComboBox(potentialTables);
		box1.add(tableNameBox);
		centralSettings.add(box1);
		label2 = new JLabel("Central projection/restriction (optional)");
		box2.add( label2);	
		JTextField extensionField = new JTextField();
		box2.add( extensionField );
		centralSettings.add(box2);
		label3 = new JLabel("Include central filters?");
		box3.add( label3);	
		JComboBox includeCentralBox = new JComboBox(includeCentralFilterOptions);
		box3.add( includeCentralBox );
	
		int option2 = JOptionPane.showOptionDialog(
				this,
				centralSettings,
				"Main Table Settings",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE,
				null,
				dialogOptions,
				null);
		
		String tableName = tableNameBox.getSelectedItem().toString();		
		
		if (option2 == 1){// choose central table cols
			 Box columnsBox = new Box(BoxLayout.Y_AXIS);

			 Table centralTable = resolver.getCentralTable(tableName);
			 Column[] cols = centralTable.getColumns();	
			 
			 JCheckBox[] colChecks = new JCheckBox[cols.length];	
			 String[] colNames = new String[cols.length];
			 JTextField[] colAliases = new JTextField[cols.length];
			  
			 for (int j=0;j<cols.length;j++){
				Box horizBox = new Box(BoxLayout.X_AXIS);
				JCheckBox check1 = new JCheckBox(cols[j].getName());
				check1.setSelected(true);
				horizBox.add(check1);
				colChecks[j] = check1;
				JTextField field1 = new JTextField(cols[j].getName());
				horizBox.add(field1);
				colNames[j] = cols[j].getName();
				colAliases[j] = field1;		 
				columnsBox.add(horizBox);
			 } 	
			 String[] newOptions = new String[] {"Ok","Cancel"}; 
			 int colsOption = JOptionPane.showOptionDialog(
									this,
									columnsBox,
									"Select columns for the final dataset ",
									JOptionPane.DEFAULT_OPTION,
									JOptionPane.PLAIN_MESSAGE,
									null,
									newOptions,
									null);
			 
			 String comma = "";	
			 if (colsOption == 0){// recover the aliases and names into cenColAliases and celColNames
				for (int i = 0; i < colChecks.length; i++){
					if (colChecks[i].getSelectedObjects() == null)
						continue;
					
					cenColName = cenColName+comma+colNames[i];
					cenColAlias = cenColAlias+comma+colAliases[i].getText();
					comma = ",";			
				}										
			 }
		}
		
		String userTableName = (datasetName+"__"+tableName+"__"+"main").toLowerCase();
		userTableName = JOptionPane.showInputDialog(null,"User Table Name:",userTableName);
			
		String extension = extensionField.getText();
		String includeCentralFilters = includeCentralBox.getSelectedItem().toString();
		String tableType = "m"; 
		dataset.getElement().setAttribute("mainTable",tableName);
				 
		Integer tCount = new Integer(transformationCount+1);

		Transformation transformation = new Transformation();
		transformation.getElement().setAttribute("internalName",tCount.toString());
		transformation.getElement().setAttribute("tableType",tableType);
		transformation.getElement().setAttribute("centralTable",tableName);
		transformation.getElement().setAttribute("userTableName",userTableName);
		transformation.getElement().setAttribute("includeCentralFilter",includeCentralFilters);

		
		String[] columnNames = {"%"};
		Table[] referencedTables = resolver.getReferencedTables(tableName);
		transformation = getCardinalities(referencedTables, tableName, tableType, datasetName, extension, transformationCount,transformation);
     
		dataset.insertChildObject(transformationCount,transformation);	
		transformationCount++;

		// now do the other dm tables
		do {
		  potentialTables = new String[tableList.size()];	
		  tableList.toArray(potentialTables);
		  		  
		  centralSettings = new Box(BoxLayout.Y_AXIS);
		  centralSettings.add(Box.createRigidArea(new Dimension(400,1)));		
		  box1 = new Box(BoxLayout.X_AXIS);
		  box2 = new Box(BoxLayout.X_AXIS);
		  box3 = new Box(BoxLayout.X_AXIS);
		  		
		  label1 = new JLabel("Central table name");
		  box1.add( label1);	
		  tableNameBox = new JComboBox(potentialTables);
		  box1.add( tableNameBox );
		  centralSettings.add(box1);
		  label2 = new JLabel("Central projection/restriction (optional)");
		  box2.add( label2);	
		  extensionField = new JTextField();
		  box2.add( extensionField );
		  centralSettings.add(box2);
		  label3 = new JLabel("Include central filters?");
		  box3.add( label3);	
		  includeCentralBox = new JComboBox(includeCentralFilterOptions);
		  box3.add( includeCentralBox );
		  centralSettings.add(box3);
		  
		  int option3 = JOptionPane.showOptionDialog(
				  this,
				  centralSettings,
				  "Central Table Settings",
				  JOptionPane.DEFAULT_OPTION,
				  JOptionPane.PLAIN_MESSAGE,
				  null,
				  dialogOptions,
				  null);
				
		  tableName = tableNameBox.getSelectedItem().toString();
		 
		  if (option3 == 1){// choose central table cols
			   Box columnsBox = new Box(BoxLayout.Y_AXIS);
		
			   Table centralTable = resolver.getCentralTable(tableName);
			   Column[] cols = centralTable.getColumns();	
			 
			   JCheckBox[] colChecks = new JCheckBox[cols.length];	
			   String[] colNames = new String[cols.length];
			   JTextField[] colAliases = new JTextField[cols.length];
			  
			   for (int j=0;j<cols.length;j++){
				  Box horizBox = new Box(BoxLayout.X_AXIS);
				  JCheckBox check1 = new JCheckBox(cols[j].getName());
				  check1.setSelected(true);
				  horizBox.add(check1);
				  colChecks[j] = check1;
				  JTextField field1 = new JTextField(cols[j].getName());
				  horizBox.add(field1);
				  colNames[j] = cols[j].getName();
				  colAliases[j] = field1;		 
				  columnsBox.add(horizBox);
			   } 	
			   String[] newOptions = new String[] {"Ok","Cancel"}; 
			   int colsOption = JOptionPane.showOptionDialog(
									  this,
									  columnsBox,
									  "Select columns for the final dataset ",
									  JOptionPane.DEFAULT_OPTION,
									  JOptionPane.PLAIN_MESSAGE,
									  null,
									  newOptions,
									  null);
			 
			   cenColName = "";
			   cenColAlias = "";
			   String comma = "";	
			   if (colsOption == 0){// recover the aliases and names into cenColAliases and celColNames
				  for (int i = 0; i < colChecks.length; i++){
					  if (colChecks[i].getSelectedObjects() == null)
						  continue;
					
					  cenColName = cenColName+comma+colNames[i];
					  cenColAlias = cenColAlias+comma+colAliases[i].getText();
					  comma = ",";			
				  }										
			   }
		  }
		 	  
		  userTableName = (datasetName+"__"+tableName+"__"+"dm").toLowerCase();
		  userTableName = JOptionPane.showInputDialog(null,"User Table Name:",userTableName);
		  extension = extensionField.getText();
		  includeCentralFilters = includeCentralBox.getSelectedItem().toString();	

		  tableList.remove(tableName);

		  if (tableName != null){
			tableType = "d"; 	
		 	tCount = new Integer(transformationCount+1);
			transformation = new Transformation();
			transformation.getElement().setAttribute("internalName",tCount.toString());
			transformation.getElement().setAttribute("tableType",tableType);
			transformation.getElement().setAttribute("centralTable",tableName);
			transformation.getElement().setAttribute("userTableName",userTableName);
			transformation.getElement().setAttribute("includeCentralFilter",includeCentralFilters);

			referencedTables = resolver.getReferencedTables(tableName);
			transformation = getCardinalities(referencedTables, tableName, tableType, datasetName, extension, transformationCount,transformation);

			dataset.insertChildObject(transformationCount,transformation);	
			transformationCount++;	
		  }		
		}
	    while (tableName != null && tableList.size() > 0);
		
		// add dataset to transformationConfig
		tConfig.insertChildObject(0,dataset);

		// save to file
		
		ConfigurationAdaptor configAdaptor = new ConfigurationAdaptor();
		configAdaptor.adaptor=adaptor;
		configAdaptor.resolver=resolver;
		configAdaptor.writeDocument(tConfig,xmlFile);
		
		System.out.println ("\nWritten XML to: "+xmlFile);
		
		// open it in a new frame
		
		TransformationConfigTreeWidget frame = new TransformationConfigTreeWidget(null, this, tConfig, null, null, null, null);
		frame.setVisible(true);
		desktop.add(frame);
		try {
		  frame.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {
		}

		
  	}
	catch(Exception e){
		System.out.println("Exception:" + e.toString());
	}
  }
  
  

  private Transformation getCardinalities(Table[] referencedTables,
                                String tableName,
                                String tableType,
                                String datasetName,
                                String centralExtension,
                                int transformationCount,
                                Transformation transformation){
                               	                            	 
	 int unitCount = 0;
    
     if (tableType.equals("m"))
     	tableList = new ArrayList();//create a new list of candidates for next central table selection
     
	 Box cardinalitySettings = new Box(BoxLayout.Y_AXIS);	
	 cardinalitySettings.add(Box.createRigidArea(new Dimension(400,1)));		
	 
	 JCheckBox[] checkboxs = new JCheckBox[referencedTables.length];
	 JComboBox[] comboBoxs = new JComboBox[referencedTables.length];
	 JTextField[] textFields = new JTextField[referencedTables.length];
     
	 String[] cardinalityOptions = new String[] {"11","1n","n1","0n","n1r"};
     
	 for (int i = 0; i < referencedTables.length; i++){
		if (referencedTables[i].getName().equals(tableName))
			continue;

		Box box2 = new Box(BoxLayout.X_AXIS);
		Box box3 = new Box(BoxLayout.X_AXIS);				
		checkboxs[i] = new JCheckBox("Include "+referencedTables[i].getName());
		checkboxs[i].setSelected(true);
		JLabel label1 = new JLabel("Cardinality for "+tableName+"."+referencedTables[i].PK+
									" => "+referencedTables[i].getName()+"."+referencedTables[i].FK);
		comboBoxs[i] = new JComboBox(cardinalityOptions);
		JLabel label2 = new JLabel("Referenced projection/restriction (optional)");
		textFields[i] = new JTextField();
				
		cardinalitySettings.add(checkboxs[i]);
		box2.add(label1);
		box2.add(comboBoxs[i]);
		cardinalitySettings.add(box2);
		box3.add(label2);
		box3.add(textFields[i]);
		cardinalitySettings.add(box3);
		
		cardinalitySettings.add(Box.createVerticalStrut(20));
     }
	 String[] dialogOptions = new String[] {"Continue","Select columns","Cancel"};
	 int option = JOptionPane.showOptionDialog(
					 this,
					 cardinalitySettings,
					 "Cardinality settings for tables referenced from "+tableName,
					 JOptionPane.DEFAULT_OPTION,
					 JOptionPane.PLAIN_MESSAGE,
					 null,
					 dialogOptions,
					 null);
     
     if (option == 2)
     	return transformation;
     else if (option == 1){// choose columns you want
     	Box columnsBox = new Box(BoxLayout.Y_AXIS);
		
		ArrayList colChecks = new ArrayList(); 
		ArrayList colAliases = new ArrayList();
		ArrayList colNames = new ArrayList();
		ArrayList colTable = new ArrayList();
		
		for (int i = 0; i < referencedTables.length; i++){
			Table refTab = referencedTables[i];
			if (refTab.getName().equals(tableName))
			   continue;
			String cardinality = comboBoxs[i].getSelectedItem().toString();
  		
			if (checkboxs[i].getSelectedObjects() == null  || cardinality.equals("1n"))
			   continue;
			 
			JLabel label1 = new JLabel(refTab.getName());
			columnsBox.add(label1);   
			Column[] cols = refTab.getColumns();
			
			for (int j=0;j<cols.length;j++){
				 Box horizBox = new Box(BoxLayout.X_AXIS);
				 JCheckBox check1 = new JCheckBox(cols[j].getName());
				 check1.setSelected(true);
				 horizBox.add(check1);
				 colChecks.add(check1);
				 JTextField field1 = new JTextField(cols[j].getName());
				 horizBox.add(field1);
				 colNames.add(cols[j].getName());
				 colAliases.add(field1);
				 colTable.add(refTab.getName());
				 columnsBox.add(horizBox);
			}
		}
     	
     	dialogOptions = new String[] {"Ok","Cancel"}; 
		int colsOption = JOptionPane.showOptionDialog(
						this,
						columnsBox,
						"Select columns for the final dataset ",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.PLAIN_MESSAGE,
						null,
						dialogOptions,
						null);
						
		if (colsOption == 0){// recover the aliases and names
			for (int i = 0; i < colChecks.size(); i++){
				if (((JCheckBox) colChecks.get(i)).getSelectedObjects() == null)
					continue;
							
				if (refColNames.get(colTable.get(i)) == null)		
					refColNames.put(colTable.get(i),colNames.get(i));
				else
					refColNames.put(colTable.get(i),refColNames.get(colTable.get(i))+","+colNames.get(i));
					
				if (refColAliases.get(colTable.get(i)) == null)		
					refColAliases.put(colTable.get(i),((JTextField) colAliases.get(i)).getText());
				else
					refColAliases.put(colTable.get(i),refColAliases.get(colTable.get(i))+","+((JTextField) colAliases.get(i)).getText());
												
			}
			
		}
			
     }
     
     for (int i = 0; i < referencedTables.length; i++){
		 Table refTab = referencedTables[i];
	  	 if (refTab.getName().equals(tableName))
			continue;
     	 String cardinality = comboBoxs[i].getSelectedItem().toString();
  		 String extension = textFields[i].getText();
  		
		 if (checkboxs[i].getSelectedObjects() == null  || cardinality.equals("1n")){
			if (tableType.equals("m"))
		 		tableList.add(refTab.getName());
		 	
		 	continue;
		 }
		 	
		 if (!tableType.equals("m"))
		 	tableList.remove(tableName);
	
		 Integer tunitCount = new Integer(unitCount+1);
		 
		 String refColName = "";
		 String refColAlias = "";
		 
		 if (refColNames.get(refTab.getName()) != null)
			refColName = (String) refColNames.get(refTab.getName());
		 if (refColAliases.get(refTab.getName()) != null)
			refColAlias = (String) refColAliases.get(refTab.getName());
		 
		 TransformationUnit transformationUnit = new TransformationUnit();
		 transformationUnit.getElement().setAttribute("internalName",tunitCount.toString());	
		 transformationUnit.getElement().setAttribute("referencingType",refTab.status);	
		 transformationUnit.getElement().setAttribute("primaryKey",refTab.PK);	
		 transformationUnit.getElement().setAttribute("referencedTable",refTab.getName().toUpperCase());
		 transformationUnit.getElement().setAttribute("cardinality",cardinality);
		 transformationUnit.getElement().setAttribute("centralProjection",centralExtension);			
		 transformationUnit.getElement().setAttribute("referencedProjection",extension);
		 transformationUnit.getElement().setAttribute("foreignKey",refTab.FK);		
		 transformationUnit.getElement().setAttribute("referenceColumnNames",refColName);	
		 transformationUnit.getElement().setAttribute("referenceColumnAliases",refColAlias);	
		 transformationUnit.getElement().setAttribute("centralColumnNames",cenColName);	
		 transformationUnit.getElement().setAttribute("centralColumnAliases",cenColAlias);	
     	 
     	 transformation.insertChildObject(unitCount,transformationUnit);
     	 unitCount++;                           	
     }
	 
     return transformation;
                                	
  }


  private void createDDL(){
  	
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
	configAdaptor.transformationPreparation(tConfig);
	configAdaptor.writeDDL(ddlFile,tConfig);
		
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
