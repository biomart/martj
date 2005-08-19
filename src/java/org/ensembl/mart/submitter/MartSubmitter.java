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
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.ensembl.mart.builder.lib.DatabaseAdaptor;
import org.ensembl.mart.builder.lib.Dataset;
import org.ensembl.mart.builder.lib.Transformation;
import org.ensembl.mart.builder.lib.TransformationConfig;
import org.ensembl.mart.builder.lib.TransformationUnit;
import org.ensembl.mart.editor.DatasetConfigTreeWidget;
import org.ensembl.mart.explorer.Feedback;
import org.ensembl.mart.guiutils.DatabaseSettingsDialog;
import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatabaseDatasetConfigUtils;
import org.ensembl.mart.lib.config.DatasetConfigXMLUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import com.mysql.jdbc.PreparedStatement;

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

public class MartSubmitter extends JFrame implements 
ActionListener { 

	private JDesktopPane desktop;
	static private String user;
	private String database;
	private String schema;
	private static String connection;
	private DatabaseAdaptor adaptor;
	private Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	private DatabaseSettingsDialog databaseDialog = new DatabaseSettingsDialog(
			prefs);
	private Connection dbConnection;
	protected Clipboard clipboardEditor;
	private static TextField textField1;
	private static Button button;
	//private TextField text;
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
		// setContentPane(frame);

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

		// boolean valid = false;

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
			// ds = null;
			connection = "MartSubmitter (NO DATABASE CONNECTION)";
			// warning dialog then retry
			Feedback f = new Feedback(this);
			f
					.warning("Could not connect to Database\nwith the given Connection Settings.\nPlease try again!");
			// valid = false;

		} finally {
			setTitle(connection);
			enableCursor();
		}
	}

	public void importDatasetConfig() {
		try {
			// if (ds == null) {
			// JOptionPane.showMessageDialog(this, "Connect to database first",
			// "ERROR", 0);
			// return;
			// }

			disableCursor();

			String[] datasets = getDatasets();

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

			createCheckBoxes(doc);

			// DatasetConfigTreeWidget frame = new DatasetConfigTreeWidget(null,
			// this, null, user, dataset, intName, databaseDialog.getSchema());
			// frame.setVisible(true);
			// desktop.add(frame);
			// try {
			// frame.setSelected(true);
			// } catch (java.beans.PropertyVetoException e) {
			// }
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

	private void createCheckBoxes(Document doc) {

		SubmitFrame frame = createFrame();

		System.out.println("getting xml"
				+ doc.getRootElement().getAttributeValue("dataset"));
		System.out.println("FRAME DONE");
		Element root = doc.getRootElement();
		JPanel pane = new JPanel();
		setPane(pane);
		//setText(text);
		setDoc(doc);
		
		JPanel buttonpanel = new JPanel();
		
		button = new Button("SUBMIT");
		buttonpanel.add(button);
		frame.getContentPane().add(buttonpanel, BorderLayout.PAGE_END);
		pane.setLayout(new FlowLayout());
		button.addActionListener(this);
		
		List PGElements = root.getChildren();
		
		for (int i = 0; i < PGElements.size(); i++) {
			
			Element pge = (Element) PGElements.get(i);
			
			if (pge.getName().equals("MainTable")
					|| pge.getName().equals("Key")
					|| pge.getName().equals("FilterPage")
					|| pge.getName().equals("Importable")
					|| pge.getName().equals("Exportable"))
				continue;

			System.out.println("printing counting " + i + " name "
					+ pge.getName());

			System.out.println("Page: *******"
					+ pge.getAttributeValue("internalName"));
			List AttributePageElements = ((Element) PGElements.get(i))
					.getChildren();
			//JTabbedPane tabbedPane = new JTabbedPane();
			//JPanel panel1 = new JPanel();
			//tabbedPane.addTab()
			//tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
			//frame.getContentPane().add(tabbedPane);

			for (int j = 0; j < AttributePageElements.size(); j++) {
				
				Element ape = (Element) AttributePageElements.get(j);

				System.out.println("Attribute Group: "
						+ ape.getAttributeValue("internalName"));

				List AttributGroupElements = ((Element) AttributePageElements
						.get(j)).getChildren();

				for (int k = 0; k < AttributGroupElements.size(); k++) {
					
					Element age = (Element) AttributGroupElements.get(k);

					System.out.println("Attribute Collection: "
							+ age.getAttributeValue("internalName"));

					List AttributeCollectionElements = ((Element) AttributGroupElements
							.get(k)).getChildren();
					pane.setBorder(BorderFactory.createLineBorder(Color.black));
					for (int m = 0; m < AttributeCollectionElements.size(); m++) {
						
						Element ace = (Element) AttributeCollectionElements
								.get(m);
						
						System.out.println("Attribute: "
								+ ace.getAttributeValue("internalName"));
						
						List Attribute = ((Element) AttributeCollectionElements
								.get(m)).getChildren();

						TextField text = new TextField(ace
								.getAttributeValue("internalName"), 10);
						text.setName(ace
								.getAttributeValue("internalName"));
						pane.add(text);
						//pane.addBorder(BorderFactory.createLineBorder(Color.black));
						//Border blackline = BorderFactory.createLineBorder(Color.black);
						
						ace.setName(ace.getAttributeValue("internalName"));
						
						}
				}
			}
		}
		frame.getContentPane().add(pane);
		
		
		System.out.println("before");
		
		
		
	
		
		
	}

	private void getXML(String dataset) {
		Document doc = null;

		String sql = "select compressed_xml from meta_configuration where dataset='"
				+ dataset + "'";

		// Connection conn = initialiseConnection(driver, host, port,
		// instance, user,password);

		java.sql.PreparedStatement ps;
		try {
			ps = getDbConnection().prepareStatement(sql);
			ResultSet rs = ps.executeQuery();

			if (!rs.next()) {
				// will only get one result
				// rs.close();
				// getDbConnection().close();
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

		String sql = "SELECT dataset from meta_configuration";

		ArrayList datasets = new ArrayList();

		try {
			PreparedStatement ps = (PreparedStatement) getDbConnection()
					.prepareStatement(sql);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				datasets.add(rs.getString(1));

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
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

	public Connection getDbConnection() {
		return dbConnection;
	}

	public void setDbConnection(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}
	/*
	 * public MyInternalFrame() { super("Document #" + (++openFrameCount), true,
	 * //resizable true, //closable true, //maximizable true);//iconifiable
	 * //...Create the GUI and put it in the window... //...Then set the window
	 * size or call pack... //Set the window's location.
	 * setLocation(xOffset*openFrameCount, yOffset*openFrameCount); }
	 */

	public static void submitDatabase(String sql) {
		Connection con = null;
		Statement sts = null;

		//String sqls = "INSERT INTO vs__customer__dm VALUES('" + customerIdS + "', '" + firstNameS
			//	+ "', '" + surnameS + "', '" + dateOfBirthS + "', '12', '"
			//	+ genderS + "')";
		System.out.println("second sql");
		System.out.println(sql);
		
		//String sqls2 = "INSERT INTO vs__video__main VALUES('13', " + titleS + "', " + ageCertS +"')";
 
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String url = "jdbc:mysql://localhost.localdomain:3306/video_store";
			con = DriverManager.getConnection(url, "root", "Monika222");

			sts = con.createStatement();
			int rss = sts.executeUpdate(sql);
			//int rss2 = sts.executeUpdate(sqls2);
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
	
	
		
		
		
		
	


	public void actionPerformed(ActionEvent e) {
				
		Element root = doc.getRootElement();
		setDoc(doc);
		
		Component[] cp = pane.getComponents();
		ArrayList valid = new ArrayList();
		
		
		List PGElements = root.getChildren();
		
		for (int i = 0; i < PGElements.size(); i++) {
			
			Element pge = (Element) PGElements.get(i);
			
			if (pge.getName().equals("MainTable")
					|| pge.getName().equals("Key")
					|| pge.getName().equals("FilterPage")
					|| pge.getName().equals("Importable")
					|| pge.getName().equals("Exportable"))
				continue;

			System.out.println("printing counting " + i + " name "
					+ pge.getName());

			System.out.println("Page: *******"
					+ pge.getAttributeValue("internalName"));
			List AttributePageElements = ((Element) PGElements.get(i))
					.getChildren();
			
			for (int j = 0; j < AttributePageElements.size(); j++) {
				
				Element ape = (Element) AttributePageElements.get(j);

				System.out.println("Attribute Group: "
						+ ape.getAttributeValue("internalName"));

				List AttributGroupElements = ((Element) AttributePageElements
						.get(j)).getChildren();

				for (int k = 0; k < AttributGroupElements.size(); k++) {
					
					Element age = (Element) AttributGroupElements.get(k);

					System.out.println("Attribute Collection: "
							+ age.getAttributeValue("internalName"));

					List AttributeCollectionElements = ((Element) AttributGroupElements
							.get(k)).getChildren();
					pane.setBorder(BorderFactory.createLineBorder(Color.black));
					for (int m = 0; m < AttributeCollectionElements.size(); m++) {
						
						Element ace = (Element) AttributeCollectionElements
								.get(m);
						
						System.out.println("Attribute: "
								+ ace.getAttributeValue("internalName"));
						
						List Attribute = ((Element) AttributeCollectionElements
								.get(m)).getChildren();

						System.out.println("ACE NAME: " + ace.getName());
						
						
						for (int x = 0; x < cp.length; x++) {

							TextField text = (TextField) cp[x];
							
	
							if (ace.getName().equals(text.getName()))
							{ ace.setAttribute("buttontext", text.getText()); 
							
							System.out.println("FOUND: " + text.getName());
							
							valid.add(ace);
							
							}
							
						
						}
						
						
					}	
				}
					
			}
			
				
			
		
		
		
				
		System.out.println("after");
		String sql = ("INSERT INTO vs__customer__dm VALUES('"); 
		
		
		for (int z = 0; z < valid.size(); z++) {
	        //tf[i] = i;
	    Element ace = (Element) valid.get(z);
	    System.out.println(ace.getAttributeValue("buttontext"));
	    System.out.println();
	    sql = sql + ace.getAttributeValue("buttontext");
	    
		}
	    
	    
		System.out.println(sql);
		}
		
	}
		/*
			TextField txt = (TextField) tf[i];
			
			System.out.print(txt.getText());
	        System.out.println();
	        sql = sql + txt.getName() + "', '";
	      */ 
		
					/*
		int l = sql.length();
		l = l - 3;
		sql = sql.substring(0, l) + ")";
		System.out.println(sql);}}}}
		//submitDatabase(sql);
		
		/*
		String titleS = text.getText();
		String ageCertS = text.getText();
		String customerIdS = text.getText();
		String genderS = text.getText();
		String firstNameS = text.getText();
		String surnameS = text.getText();
		String dateOfBirthS = text.getText();
		submitDatabase(titleS, ageCertS, customerIdS, genderS, firstNameS, surnameS, dateOfBirthS);
	*/
	

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
/*
	public TextField getText() {
		return text;
	}

	public void setText(TextField text) {
		this.text = text;
	}	
*/
		
		
}
