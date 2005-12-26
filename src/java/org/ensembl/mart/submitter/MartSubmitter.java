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

package org.ensembl.mart.submitter;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.TextComponent;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.UIManager;

import org.ensembl.mart.builder.lib.DatabaseAdaptor;
import org.ensembl.mart.explorer.Feedback;
import org.ensembl.mart.guiutils.DatabaseSettingsDialog;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

// import com.mysql.jdbc.PreparedStatement;

/**
 * Class MartSubmitter extends JFrame..
 * 
 * <p>
 * This class contains the main function, it draws the external frame, toolsbar,
 * menus.
 * </p>
 * 
 * @author <a href="mailto:kasprzo3@man.ac.uk">Olga Kasprzyk</a>
 * 
 */

public class MartSubmitter extends JFrame implements ActionListener {

	private JDesktopPane desktop;

	static private String user;

	private String database;

	private String schema;

	private static String connection;

	private DatabaseAdaptor adaptor;

	private Preferences prefs = Preferences.userNodeForPackage(this.getClass());

	private DatabaseSettingsDialog databaseDialog = new DatabaseSettingsDialog(
			prefs);

	private static Connection dbConnection;

	protected Clipboard clipboardEditor;

	private static JTextField text;

	private static JLabel l;

	private static Button button;

	private JPanel pane;

	private Document doc;

	public MartSubmitter() {

		// autoconnect on startup
		super("MartSubmitter");

		JFrame.setDefaultLookAndFeelDecorated(true);

		// Make the big window be indented 50 pixels from each edge
		// of the screen.
		int inset = 100;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height
				- inset * 2);

		// Set up the GUI.
		this.getContentPane().setLayout(new BorderLayout());
		desktop = new JDesktopPane();
		this.getContentPane().add(desktop, BorderLayout.CENTER);
		setJMenuBar(createMenuBar());

		// Make dragging a little faster but perhaps uglier.
		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
		clipboardEditor = new Clipboard("editor_clipboard");

	}

	final static String BUTTONPANEL = "JPanel with JButtons";

	final static String TEXTPANEL = "JPanel with JTextField";

	protected JMenuBar createMenuBar() {
		JMenuBar menuBar;
		JMenu menu;
		JMenuItem menuItem;

		// Create the menu bar.
		menuBar = new JMenuBar();

		// Build the first menu.
		menu = new JMenu("File");

		// menu.setMnemonic(KeyEvent.VK_F);
		menu.getAccessibleContext().setAccessibleDescription(
				"the file related menu");
		menuBar.add(menu);

		menuItem = new JMenuItem("Database Connection ");
		MartSubmitter.MenuActionListener menuActionListener = new MartSubmitter.MenuActionListener();
		menuItem.addActionListener(menuActionListener);
		menuItem.setMnemonic(KeyEvent.VK_D);
		menu.add(menuItem);

		menu.addSeparator();
		menuItem = new JMenuItem("Import ");
		menuItem.addActionListener(menuActionListener);

		// menuItem.setMnemonic(KeyEvent.VK_I);
		menu.add(menuItem);

		// a group of check box menu items
		menu.addSeparator();
		menuItem = new JMenuItem("Exit");
		menuItem.addActionListener(menuActionListener);
		menuItem.setMnemonic(KeyEvent.VK_X);
		menu.add(menuItem);

		return menuBar;
	}

	// Create a new internal frame.
	protected SubmitFrame createFrame() {
		SubmitFrame frame = new SubmitFrame();
		frame.setVisible(true); // necessary as of 1.3
		desktop.add(frame);
		try {
			frame.setSelected(true);
		} catch (java.beans.PropertyVetoException e) {
		}

		return frame;
	}

	// Quit the application.
	protected void quit() {
		System.exit(0);
	}

	private static void createAndShowGUI() {
		// Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);

		// Create and set up the window.
		MartSubmitter frame = new MartSubmitter();
		frame.setTitle(connection);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

	public static void main(String[] args) {
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager
							.getCrossPlatformLookAndFeelClassName());
				} catch (Exception e) {
				}

				createAndShowGUI();

			}
		});
	}

	// Inner class that handles Menu Action Events
	protected class MenuActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			if (e.getActionCommand().startsWith("Database"))
				databaseConnection();

			else if (e.getActionCommand().startsWith("Import"))
				importDatasetConfig();
			else if (e.getActionCommand().equals("Exit"))
				exit();

		}
	}

	public void databaseConnection() {

		try {
			disableCursor();

			databaseDialog.showDialog(this);

			adaptor = new DatabaseAdaptor(databaseDialog.getDriver(),
					databaseDialog.getUser(), databaseDialog.getPassword(),
					databaseDialog.getHost(), databaseDialog.getPort(),
					databaseDialog.getDatabase(), databaseDialog.getSchema(),
					"");

			Connection conn = adaptor.getCon();

			setDbConnection(conn);

			connection = "MartSubmitter (CONNECTED TO "
					+ databaseDialog.getDatabase() + "/"
					+ databaseDialog.getSchema() + " AS "
					+ databaseDialog.getUser() + ")";

			user = databaseDialog.getUser();
			database = databaseDialog.getDatabase();

		} catch (Exception e) {
			connection = "MartSubmitter (NO DATABASE CONNECTION)";
			// warning dialog then retry
			Feedback f = new Feedback(this);
			f
					.warning("Could not connect to Database\nwith the given Connection Settings.\nPlease try again!");

		} finally {
			setTitle(connection);
			enableCursor();
		}
	}

	public void importDatasetConfig() {
		String[] datasets = getDatasets();

		try {
			disableCursor();

			if (datasets.length == 0) {
				JOptionPane.showMessageDialog(this,
						"No datasets in this database", "ERROR", 0);
				return;
			}

			String dataset = (String) JOptionPane.showInputDialog(null,
					"Choose one", "Dataset config",
					JOptionPane.INFORMATION_MESSAGE, null, datasets,
					datasets[0]);

			if (dataset == null)
				return;

			getXML(dataset);

			addComponents(doc);

		} catch (Exception e) {
			JOptionPane
					.showMessageDialog(
							this,
							"No datasets available for import - is this a BioMart compatible schema? Absent or empty meta_configuration table?",
							"ERROR", 0);

		} finally {
			enableCursor();
		}

	}

	
	private void addComponents(Document doc) {
		SubmitFrame frame = createFrame();

		Element root = doc.getRootElement();
		JPanel pane = new JPanel();
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
		setPane(pane);
		setDoc(doc);

		ArrayList tableNames = new ArrayList();
		String tableName = new String();
		tableName = "";

		JPanel buttonpanel = new JPanel();
		int attribute_counter = 0;
		JPanel panel_main = new JPanel();
		
		
		button = new Button("SUBMIT");
		buttonpanel.add(button);
		frame.getContentPane().add(buttonpanel, BorderLayout.PAGE_END);
	
		button.addActionListener(this);

		Component[] cp = pane.getComponents();
		//ArrayList tempAttributes = new ArrayList();
		String tempAttributes = new String();
		ArrayList selectedAttributes = new ArrayList();
		ArrayList checkAtt = new ArrayList();
		
		
		List PGElements = root.getChildren();

		for (int i = 0; i < PGElements.size(); i++) {

			Element pge = (Element) PGElements.get(i);
			
			if (pge.getName().equals("Key")) {
				System.out.println("key: " + pge.getText());
				String key = pge.getText();
			}

			if // (pge.getName().equals("MainTable")
			(pge.getName().equals("Key") || pge.getName().equals("FilterPage")
					|| pge.getName().equals("Importable")
					|| pge.getName().equals("Exportable"))
				continue;

			if (pge.getName().equals("MainTable")) {
				tableNames.add(pge.getText());
			}

			List AttributePageElements = ((Element) PGElements.get(i))
					.getChildren();

			for (int j = 0; j < AttributePageElements.size(); j++) {
				Element ape = (Element) AttributePageElements.get(j);

				List AttributGroupElements = ((Element) AttributePageElements
						.get(j)).getChildren();

				for (int k = 0; k < AttributGroupElements.size(); k++) {
					Element age = (Element) AttributGroupElements.get(k);

					List AttributeCollectionElements = ((Element) AttributGroupElements
							.get(k)).getChildren();
					pane.setBorder(BorderFactory.createLineBorder(Color.black));

					for (int m = 0; m < AttributeCollectionElements.size(); m++) {
						Element ace = (Element) AttributeCollectionElements
								.get(m);

						List Attribute = ((Element) AttributeCollectionElements
								.get(m)).getChildren();

						if (ace.getAttributeValue("tableConstraint").equals(
								"main")) {
							mainTable();
						}
						
						//if (ace.getAttributeValue("internalName").equals("cintestinalis_gene_ensembl_structure.exon_cds_end")) {
							//System.out.println("WWWWWWWWWWWwwww " + ace.getAttributeValue("internalName"));
							//continue;
						//}
						
						else {
							if (tableNames.contains(ace.getAttributeValue("tableConstraint")))
							{}
							else {
								tableNames.add(ace.getAttributeValue("tableConstraint"));
							}
							
							
						}
						
						
						String tableConstraint = ace.getAttributeValue("tableConstraint");
						String field = ace.getAttributeValue("field");
						tempAttributes = tableConstraint.concat(field);
						String checkAttCurrent = new String();
						checkAttCurrent = ace.getAttributeValue("internalName");
						
					    if (checkAtt.contains(tempAttributes))
					    {}
					    
					    if (checkAttCurrent.indexOf("_cds_")!=-1) {
					    	System.out.println("HHHH " + checkAttCurrent);
					    	continue;
					    }
					    
					    if (checkAttCurrent.indexOf(".")!=-1 || 
								checkAttCurrent.startsWith("5") || 
								checkAttCurrent.startsWith("3") 
					    		) {
					    	continue;
					    }
					    
					    
					    
					    else// {
					    	attribute_counter++;
					    	checkAtt.add(tempAttributes);
					    	tempAttributes = "";
					    	selectedAttributes.add(ace);
							
							ace.setName(ace.getAttributeValue("internalName"));
					    //}
						
	
					}

				}
			}
		}
			
		int r = 0;
		
		for (r = 0; r < tableNames.size(); r++) {
			String currentTable = (String) tableNames.get(r);
			int att_count = 0;
					
			if (tableNames.get(r).toString().endsWith("main")) {
				currentTable = "main";
			}

			JPanel tab_pane = new JPanel();
						
			for (int z = 0; z < attribute_counter; z++) {
				Element ace = (Element) selectedAttributes.get(z);

				if (ace.getAttributeValue("tableConstraint").equals(
						currentTable)) {
					JLabel l = new JLabel(ace.getAttributeValue("internalName"));
					tab_pane.add(l);
					JTextField text = new JTextField(10);
					text.setName(ace.getAttributeValue("internalName"));
					l.setLabelFor(text);
					tab_pane.add(text);
					att_count++;
					
				}

			}
						
			JScrollPane scrollPane = new JScrollPane(tab_pane);
			scrollPane.setPreferredSize( new Dimension( 400, 300 ) );
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			//int bottom = scrollPane.getVerticalScrollBar().getMaximum();
			//scrollPane.getVerticalScrollBar().setValue(bottom);
			tabbedPane.add(currentTable, scrollPane);
			
			tab_pane.setLayout(new SpringLayout());

			LayoutUtilities.makeCompactGrid(tab_pane, 
					att_count, 2, // rows, cols
					6, 6, // initX, initY
					6, 6); // xPad, yPad
			
			panel_main.add(tabbedPane, BorderLayout.CENTER);
					    
		    // Configure the tabs to scroll
		    tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		    //tab_pane.setAutoscrolls(true);
		    
		    
		    frame.getContentPane().add(panel_main);
			frame.pack();
		}

	}

	private void getXML(String dataset) {
		Document doc = null;

		String sql = "select compressed_xml from " + getAdaptor().getSchema()
				+ ".meta_configuration where dataset='" + dataset + "'";

		java.sql.PreparedStatement ps;

		try {
			ps = getDbConnection().prepareStatement(sql);
			ResultSet rs = ps.executeQuery();

			if (!rs.next()) {
			}

			byte[] cstream = rs.getBytes(1);
			rs.close();

			InputStream istream = new GZIPInputStream(new ByteArrayInputStream(
					cstream));
			InputSource is = new InputSource(istream);
			SAXBuilder builder = new SAXBuilder();
			doc = builder.build(is);

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}

		setDoc(doc);
	}

	private String[] getDatasets() {
		String sql = "SELECT dataset from " + getAdaptor().getSchema()
				+ ".meta_configuration";

		ArrayList datasets = new ArrayList();

		try {
			PreparedStatement ps = getDbConnection().prepareStatement(sql);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				datasets.add(rs.getString(1));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		String[] ds = new String[datasets.size()];

		datasets.toArray(ds);
		return ds;
	}

	public void exit() {
		System.exit(0);
	}

	private void enableCursor() {
		setCursor(Cursor.getDefaultCursor());
		getGlassPane().setVisible(false);
	}

	private void disableCursor() {
		getGlassPane().setVisible(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	private static Connection getDbConnection() {
		return dbConnection;
	}

	public void setDbConnection(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}
	
	public static void getKeyValue(String key, String keyQ) {
		Connection con = null;
		Statement sts = null;

		try {
			sts = getDbConnection().createStatement();
			ResultSet rs = sts.executeQuery(keyQ);
			
			//System.out.println("Q : " + keyQ);
			
			while (rs.next()) {
			    
			int keyVa = rs.getInt(key);
			    System.out.println("Max Key Value: " + keyVa);
			}
			
			System.out.println();
		}

		catch (Exception es) {
			System.err.println("Exception: " + es.getMessage());
		}

		finally {
			try {
				if (sts != null)
					if (sts != null)
						sts.close();
				if (con != null)
					con.close();
			}

			catch (SQLException es) {
			}
		}
	}

	public static void submitDatabase(String sql) {
		Connection con = null;
		Statement sts = null;

		try {
			sts = getDbConnection().createStatement();
			int rss = sts.executeUpdate(sql);
		}

		catch (Exception es) {
			System.err.println("Exception: " + es.getMessage());
		}

		finally {
			try {
				if (sts != null)
					if (sts != null)
						sts.close();
				if (con != null)
					con.close();
			}

			catch (SQLException es) {
			}
		}
	}

	
	public void getTableNames() {
		Element root = doc.getRootElement();
		setDoc(doc);

		Component[] cp = pane.getComponents();
		String tempAttributes = new String();
		ArrayList selectedAttributes = new ArrayList();
		ArrayList checkAtt = new ArrayList();
		ArrayList tableNames = new ArrayList();
		String tableName = new String();
		tableName = "";
		String key = new String();
		String keyQ = new String();

		List PGElements = root.getChildren();

		for (int i = 0; i < PGElements.size(); i++) {
			Element pge = (Element) PGElements.get(i);
			
			if (pge.getName().equals("Key")) {
				System.out.println("key: " + pge.getText());
				key = pge.getText();
				keyQ = "SELECT MAX(" + key + ") as " + key + " FROM vs__video__main";
				getKeyValue(key, keyQ);
			}

			if (pge.getName().equals("Key")
					|| pge.getName().equals("FilterPage")
					|| pge.getName().equals("Importable")
					|| pge.getName().equals("Exportable"))
				continue;

			if (pge.getName().equals("MainTable")) {
				tableNames.add(pge.getText());
			}

			List AttributePageElements = ((Element) PGElements.get(i))
					.getChildren();

			for (int j = 0; j < AttributePageElements.size(); j++) {
				Element ape = (Element) AttributePageElements.get(j);

				List AttributGroupElements = ((Element) AttributePageElements
						.get(j)).getChildren();

				for (int k = 0; k < AttributGroupElements.size(); k++) {
					
					//String = "" + key.toString();
					//System.out.println("!!: " + key + ":" + pge.getAttribute();
					
					Element age = (Element) AttributGroupElements.get(k);

					List AttributeCollectionElements = ((Element) AttributGroupElements
							.get(k)).getChildren();
					pane.setBorder(BorderFactory.createLineBorder(Color.black));
					for (int m = 0; m < AttributeCollectionElements.size(); m++) {

						Element ace = (Element) AttributeCollectionElements
								.get(m);

						
						
						if (ace.getAttributeValue("tableConstraint").equals(
								"main")) {
							mainTable();
						}

						else {
							if (tableNames.contains(ace.getAttributeValue("tableConstraint")))
							{}
							else {
								tableNames.add(ace.getAttributeValue("tableConstraint"));
							}
							
							
						}
						
						for (int x = 0; x < cp.length; x++) {
							TextField text = (TextField) cp[x];

						//	System.out.println("here: " + text.getName());

							if (ace.getName().equals(text.getName())) {
								ace.setAttribute("buttontext", text.getText());
								
								String tableConstraint = ace.getAttributeValue("tableConstraint");
								String field = ace.getAttributeValue("field");
								tempAttributes = tableConstraint.concat(field);
								
							    if (checkAtt.contains(tempAttributes))
							    {}
							    else// {
							    	checkAtt.add(tempAttributes);
							    	tempAttributes = "";
							    	selectedAttributes.add(ace);
							    	//System.out.println(checkAtt);
							    	
									ace.setName(ace.getAttributeValue("internalName"));
									
							}
						}
					}
				}
				 
			}
			
		}

		int p = 0;
		String array = new String();
		String array2 = new String();
		int li;

		for (p = 0; p < tableNames.size(); p++) {
			String currentTable = (String) tableNames.get(p);

			if (tableNames.get(p).toString().endsWith("main")) {
				currentTable = "main";
			}

			ArrayList attributeArray = new ArrayList();
			ArrayList attributeArray2 = new ArrayList();

			// go through all attributes and pick out the relevant to each table
			for (int z = 0; z < selectedAttributes.size(); z++) {
				Element ace = (Element) selectedAttributes.get(z);

				if (ace.getAttributeValue("tableConstraint").equals(
						currentTable)) {
					attributeArray.add(ace.getAttributeValue("buttontext"));
					attributeArray2.add(ace.getName());
				}
			}

			String sql = new String();
			array = attributeArray.toString();
			array2 = attributeArray2.toString();
			li = array.lastIndexOf("]");
			array = array.substring(1, li);
			array = array.replaceAll(", ", "\",\"");
			li = array2.lastIndexOf("]");
			array2 = array2.substring(1, li);
			
			

			// create Sql
			sql = "INSERT INTO " + tableNames.get(p) + " (" + array2 + ") "
					+ "VALUES (\"" + array + "\")";
			System.out.println(sql);

			//submitDatabase(sql);

		}
	}

	public void mainTable() {
		Element root = doc.getRootElement();
		setDoc(doc);

		ArrayList tableNames = new ArrayList();

		List PGElements = root.getChildren();

		for (int i = 0; i < PGElements.size(); i++) {
			Element pge = (Element) PGElements.get(i);

			if (pge.getName().equals("MainTable")) {
				tableNames.add(pge.getText());
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		getTableNames();
	}

	public JPanel getPane() {
		return pane;
	}

	public void setPane(JPanel pane) {
		this.pane = pane;
	}

	public Document getDoc() {
		return doc;
	}

	public void setDoc(Document doc) {
		this.doc = doc;
	}

	/**
	 * @return Returns the adaptor.
	 */
	public DatabaseAdaptor getAdaptor() {
		return adaptor;
	}

	/**
	 * @param adaptor
	 *            The adaptor to set.
	 */
	public void setAdaptor(DatabaseAdaptor adaptor) {
		this.adaptor = adaptor;
	}

}
