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
package org.ensembl.mart.explorer;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatabaseDSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.MartRegistry;
import org.ensembl.mart.lib.config.MartRegistryXMLUtils;
import org.ensembl.mart.lib.config.RegistryDSViewAdaptor;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.util.LoggingUtil;
/**
 * Widget representing availabled adaptors and enabling user to add, and delete
 * them.
 *  
 */
public class AdaptorManager extends Box {
	private boolean advancedOptionsEnabled;
	private static final Logger logger = Logger.getLogger(AdaptorManager.class
			.getName());
	private Feedback feedback = new Feedback(this);
	private static final String DS_VIEW_FILE_KEY = "CONFIG_FILE_KEY";
	private static final String REGISTRY_FILE_KEY = "REGISTRY_FILE_KEY";
	private static final String REGISTRY_KEY = "REGISTRY_KEY";
	private static final String OPTIONAL_ENABLED_KEY = "OPTIONAL_ENABLED";
	private String none = "None";
	private RegistryDSViewAdaptor rootAdaptor = new RegistryDSViewAdaptor();
	private Map optionToView = new HashMap();
	/** Persistent preferences object used to hold user history. */
	private Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	private LabelledComboBox combo = new LabelledComboBox("Adaptor");
	private JFileChooser dsViewFileChooser = new JFileChooser();
	private JFileChooser registryFileChooser = new JFileChooser();
	private FileFilter xmlFilter = new FileFilter() {
		public boolean accept(File f) {
			return f != null
					&& (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
		}
		public String getDescription() {
			return "XML Files";
		}
	};
	private DatabaseSettingsDialog databaseDialog = new DatabaseSettingsDialog();
	/**
	 * This widget is part of a system based on the MVC design pattern. From this
	 * perspective the widget is a View and a Controller and the query is the
	 * Model.
	 * 
	 * @param query
	 *          underlying model for this widget.
	 */
	public AdaptorManager() {
		super(BoxLayout.Y_AXIS);
		databaseDialog.addDatabaseType("mysql");
		databaseDialog.addDriver("com.mysql.jdbc.Driver");
		databaseDialog.setPrefs(Preferences.userNodeForPackage(this.getClass()));
		loadPrefs();
		createUI();
	}
	private void createUI() {
		combo.setEditable(false);
		//combo.setSelectedItem(none);
		initFileChoosers();
		JButton addDB = new JButton("Add Database");
		addDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doAddDatabase();
			}
		});
		JButton importRegistry = new JButton("Import Registry");
		importRegistry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doImportRegistry();
			}
		});
		JButton addFile = new JButton("Add File");
		addFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doAddFile();
			}
		});
		JButton delete = new JButton("Delete");
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doDelete();
			}
		});
		JButton deleteAll = new JButton("Delete All");
		deleteAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doDeleteAll();
			}
		});
		Box top = Box.createHorizontalBox();
		top.add(combo);
		Box bottom = Box.createHorizontalBox();
		bottom.add(Box.createHorizontalGlue());
		bottom.add(addDB);
		bottom.add(importRegistry);
		bottom.add(addFile);
		bottom.add(delete);
		bottom.add(deleteAll);
		add(top);
		add(bottom);
	}
	/**
	 * Presents the user with a file chooser dialog with which she can choose a
	 * registry file to import.
	 */
	protected void doImportRegistry() {
		// user chooses file
		int action = registryFileChooser.showOpenDialog(this);
		// convert file contents into string
		if (action == JFileChooser.APPROVE_OPTION) {
			File f = registryFileChooser.getSelectedFile().getAbsoluteFile();
			prefs.put(REGISTRY_FILE_KEY, f.toString());
			try {
				importRegistry(f.toURL());
			} catch (MalformedURLException e) {
				feedback.warning(e);
			}
		}
	}
	/**
	 * @param url
	 */
	public void importRegistry(URL url) {
		try {
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
 			RegistryDSViewAdaptor ra = new RegistryDSViewAdaptor(url);
			DSViewAdaptor[] as = ra.getAdaptors();
			for (int i = 0; i < as.length; i++) {
				// TODO only add "leaf" node adaptors
				add(as[i]);
			}
		} catch (ConfigurationException e) {
			JOptionPane.showMessageDialog(this, "Problem loading the url: " + url
					+ ": " + e.getMessage());
		} finally {
      setCursor(Cursor.getDefaultCursor());
		}
	}
	/**
	 * Removes all adaptors.
	 *  
	 */
	private void doDeleteAll() {
		DSViewAdaptor as[] = rootAdaptor.getAdaptors();
		for (int i = 0; i < as.length; i++)
			rootAdaptor.remove(as[i]);
		try {
			updateWidget(rootAdaptor.getAdaptors());
			storePrefs();
		} catch (ConfigurationException e) {
			feedback.warning(e);
		}
	}
	/**
	 * Updates state of widget after a user action.
	 * 
	 * @param views
	 */
	private void updateWidget(DSViewAdaptor[] adaptors)
			throws ConfigurationException {
		optionToView.clear();
		// map string -> adaptor
		for (int i = 0; i < adaptors.length; i++) {
			DSViewAdaptor a = adaptors[i];
			optionToView.put(a.getName(), a);
			logger.fine("Added" + a.getName() + ":" + a);
		}
		// sort
		List l = new ArrayList(optionToView.keySet());
		Collections.sort(l);
		combo.removeAllItems();
		combo.addAll(l);
	}
	/**
	 *  
	 */
	private void doAddDatabase() {
		if (databaseDialog.showDialog(this)) {

      String defaultSourceName = DetailedDataSource.defaultName(databaseDialog.getHost(), databaseDialog.getPort(), databaseDialog.getDatabase(), databaseDialog.getUser());
			DetailedDataSource ds =
				new DetailedDataSource(
					databaseDialog.getDatabaseType(),
					databaseDialog.getHost(),
					databaseDialog.getPort(),
					databaseDialog.getDatabase(),
					databaseDialog.getUser(),
					databaseDialog.getPassword(),
					10,
					databaseDialog.getDriver(), defaultSourceName);

			try {
				DSViewAdaptor a = new DatabaseDSViewAdaptor(ds, databaseDialog
						.getUser());
				// TODO bind a and ds so that can recreate the link after persistence
				add(a);
			} catch (ConfigurationException e1) {
				feedback
						.warning(
								"Couldn not load DatasetViews from \""
										+ ds
										+ "\". It might be possible to execute queries against this database.",
								e1, false);
			}
		}
	}
	private void loadPrefs() {
		byte[] b = prefs.getByteArray(REGISTRY_KEY, new byte[0]);
		try {
			MartRegistry reg = null;
			if (b.length > 0) {
				reg = MartRegistryXMLUtils.ByteArrayToMartRegistry(b);
			}
			if (reg != null) {
				RegistryDSViewAdaptor tmp = new RegistryDSViewAdaptor(reg);
				DSViewAdaptor[] adaptors = tmp.getAdaptors();
				for (int i = 0; i < adaptors.length; i++) {
					add(adaptors[i]);
					logger.fine("Loaded Adaptor:" + adaptors[i].getName()
							+ ", num datasetViews=" + adaptors[i].getDatasetViews().length);
				}
			}
		} catch (ConfigurationException e1) {
			e1.printStackTrace();
		}
		advancedOptionsEnabled = prefs.getBoolean(OPTIONAL_ENABLED_KEY, false);
	}
	/**
	 * Delete selected datasetview.
	 */
	public void doDelete() {
		if (combo.getSelectedItem() == null)
			return;
		try {
			Object selected = combo.getSelectedItem();
			int newIndex = -1;
			int index = combo.indexOfItem(selected);
			int max = combo.getItemCount();
			if (index + 1 < max)
				newIndex = index + 1;
			else if (index - 1 <= max)
				newIndex = index - 1;
			// remove the selected item
			rootAdaptor.remove((DSViewAdaptor) optionToView.get(selected));
			updateWidget(rootAdaptor.getAdaptors());
			// select the "next" item
			if (newIndex > -1)
				combo.setSelectedItem(combo.getItemAt(newIndex));
			storePrefs();
		} catch (ConfigurationException e) {
			feedback
					.warning("Failed to delete adaptor " + combo.getSelectedItem(), e);
		}
	}
	/**
	 * Presents the user with a file chooser dialog with which she can choose a
	 * configuration file.
	 */
	protected void doAddFile() {
		// user chooses file
		int action = dsViewFileChooser.showOpenDialog(this);
		// convert file contents into string
		if (action == JFileChooser.APPROVE_OPTION) {
			File f = dsViewFileChooser.getSelectedFile().getAbsoluteFile();
			prefs.put(DS_VIEW_FILE_KEY, f.toString());
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				URLDSViewAdaptor adaptor = new URLDSViewAdaptor(f.toURL(), false);
				// TODO resolve any name clashes, i.e. existing dsv with same name
				//				this.adaptor.add(adaptor);
				add(adaptor);
				combo.setSelectedItem(adaptor.getName());
			} catch (MalformedURLException e) {
				JOptionPane.showMessageDialog(this, "File " + f.toString()
						+ " not found: " + e.getMessage());
			} catch (ConfigurationException e) {
				JOptionPane.showMessageDialog(this,
						"Problem loading the Failed to load file: " + f.toString() + ": "
								+ e.getMessage());
			} finally {
				setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	/**
	 * Runs a test; an instance of this class is shown in a Frame.
	 */
	public static void main(String[] args) throws Exception {
		//Preferences.userNodeForPackage(AdaptorManager.class).remove(
		// REGISTRY_KEY );
		LoggingUtil.setAllRootHandlerLevelsToFinest();
		logger.setLevel(Level.FINE);
		AdaptorManager dvm = new AdaptorManager();
		dvm.setSize(950, 750);
		dvm.showDialog(null);
		System.exit(0);
	}
	/**
	 * Convenience method which opens this instance in a dialog box.
	 * 
	 * @param parent
	 *          parent for the dialog box.
	 */
	public void showDialog(Component parent) {
		int option = JOptionPane.showOptionDialog(parent, this, "Adaptors",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.DEFAULT_OPTION, null, null,
				null);
	}
	private void initFileChoosers() {
		String lastChosenFile = prefs.get(DS_VIEW_FILE_KEY, null);
		if (lastChosenFile != null) {
			dsViewFileChooser.setSelectedFile(new File(lastChosenFile));
		}
		dsViewFileChooser.addChoosableFileFilter(xmlFilter);
		lastChosenFile = prefs.get(REGISTRY_FILE_KEY, null);
		if (lastChosenFile != null) {
			registryFileChooser.setSelectedFile(new File(lastChosenFile));
		}
		registryFileChooser.addChoosableFileFilter(xmlFilter);
	}
	public boolean contains(DatasetView dsv) {
		try {
			return dsv != null
					&& rootAdaptor.supportsInternalName(dsv.getInternalName());
		} catch (ConfigurationException e) {
			// Shouldn't happen
			feedback.warning(e);
		}
		return false;
	}
	public void add(DSViewAdaptor a) throws ConfigurationException {
		rootAdaptor.add(a);
		updateWidget(rootAdaptor.getAdaptors());
		storePrefs();
	}
	private void storePrefs() throws ConfigurationException {
		MartRegistry reg = rootAdaptor.getMartRegistry();
		byte[] b = MartRegistryXMLUtils.MartRegistryToByteArray(reg);
		prefs.putByteArray(REGISTRY_KEY, b);
		prefs.putBoolean(OPTIONAL_ENABLED_KEY, advancedOptionsEnabled);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			feedback.warning(e);
		}
	}
	public RegistryDSViewAdaptor getRootAdaptor() {
		return rootAdaptor;
	}
	public void setAdvancedOptionsEnabled(boolean b) {
		this.advancedOptionsEnabled = b;
		// just store this pref, rather than using storePrefs(), because that
		// method will
		// be expensive if many adaptors are loaded.
		prefs.putBoolean(OPTIONAL_ENABLED_KEY, advancedOptionsEnabled);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			feedback.warning(e);
		}
	}
	public boolean isAdvancedOptionsEnabled() {
		return advancedOptionsEnabled;
	}
	/**
	 * Resets the state of the manager. Removes all adaptors and sets
	 * advancedOptionsEnabled=false;
	 */
	public void reset() {
		rootAdaptor = new RegistryDSViewAdaptor();
		try {
			updateWidget(rootAdaptor.getAdaptors());
			storePrefs();
		} catch (ConfigurationException e) {
			feedback.warning(e);
		}
		setAdvancedOptionsEnabled(false);
	}
}
