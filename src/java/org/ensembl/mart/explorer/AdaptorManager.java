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

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.MartRegistry;
import org.ensembl.mart.lib.config.MartRegistryXMLUtils;
import org.ensembl.mart.lib.config.RegistryDSViewAdaptor;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;
import org.ensembl.mart.util.LoggingUtil;


/**
 * Widget representing the availabled adaptors.
 * Enables the user to select add and delete 
 * adaptors.
 *
 * TODO add file
 * TODO delete
 * TODO delete all
 * TODO add database
 * TODO adaptor.toString() -> adaptor.getName()
 */
public class AdaptorManager extends Box {

	private DataSourceManager martSettings = new DataSourceManager(this);
	private static final Logger logger = Logger.getLogger(AdaptorManager.class.getName());
	private Feedback feedback = new Feedback(this);
	private static final String CONFIG_FILE_KEY = "CONFIG_FILE_KEY";
	private static final String REGISTRY_KEY = "REGISTRY_KEY";
	private String none = "None";

	private RegistryDSViewAdaptor rootAdaptor = new RegistryDSViewAdaptor();
	private Map optionToView = new HashMap();
	/** Persistent preferences object used to hold user history. */
	private Preferences prefs = Preferences.userNodeForPackage(this.getClass());

	private LabelledComboBox combo = new LabelledComboBox("Adaptor");

  private Cursor waitCursor = new Cursor( Cursor.WAIT_CURSOR);
  private Cursor defaultCursor = new Cursor( Cursor.DEFAULT_CURSOR);

	private JFileChooser configFileChooser;

	/**
	 * This widget is part of a system based on the MVC design pattern. 
	 * From this perspective the widget is a View and a Controller
	 * and the query is the Model.
	 * @param query underlying model for this widget.
	 */
	public AdaptorManager() {
		super(BoxLayout.Y_AXIS);

		loadPrefs();

		combo.setEditable(false);
		//combo.setSelectedItem(none);
		initConfigFileChooser();

		JButton addDB = new JButton("Add Database");
		addDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doAddDatabase();
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
		bottom.add(addFile);
		bottom.add(delete);
		bottom.add(deleteAll);

		add(top);
		add(bottom);

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
		} catch (ConfigurationException e) {
			feedback.warning(e);
		}
	}

	/**
	 * Updates state of widget after a user action.
	 * @param views
	 */
	private void updateWidget(DSViewAdaptor[] adaptors)
		throws ConfigurationException {

		optionToView.clear();

		// map string -> adaptor
		for (int i = 0; i < adaptors.length; i++) {
			DSViewAdaptor a = adaptors[i];
			optionToView.put(a.getName(), a);
      logger.fine("Added" + a.getName()+":"+a );
		}

		// sort
		List l = new ArrayList(optionToView.keySet());
		Collections.sort(l);

		// Add "none"
		//    optionToView.put(none, null);
		//    l.add(0, none);

		// set on combo
		combo.removeAllItems();
		combo.addAll(l);

		storePrefs();
	}

	/**
	 * 
	 */
	private void doAddDatabase() {
		martSettings.showDialog(this);
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
				for (int i = 0; i < adaptors.length; i++)
					add(adaptors[i]);

			}
		} catch (ConfigurationException e1) {
			e1.printStackTrace();
		}

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

		} catch (ConfigurationException e) {
			feedback.warning(
				"Failed to delete adaptor " + combo.getSelectedItem(),
				e);
		}

	}

	/**
	 * Presents the user with a file chooser dialog with which she can 
	 * choose a configuration file. 
	 */
	protected void doAddFile() {

    setCursor( waitCursor );

		// user chooses file
		int action = configFileChooser.showOpenDialog(this);

		// convert file contents into string
		if (action == JFileChooser.APPROVE_OPTION) {
			File f = configFileChooser.getSelectedFile().getAbsoluteFile();
			prefs.put(CONFIG_FILE_KEY, f.toString());

			try {
				URLDSViewAdaptor adaptor = new URLDSViewAdaptor(f.toURL(), false);
				// TODO resolve any name clashes, i.e. existing dsv with same name
				//				this.adaptor.add(adaptor);
				add(adaptor);
				combo.setSelectedItem(adaptor.getName());

			} catch (MalformedURLException e) {
				JOptionPane.showMessageDialog(
					this,
					"File " + f.toString() + " not found: " + e.getMessage());
			} catch (ConfigurationException e) {
				JOptionPane.showMessageDialog(
					this,
					"Problem loading the Failed to load file: "
						+ f.toString()
						+ ": "
						+ e.getMessage());

			}

		}
  
    setCursor( defaultCursor );
	}

	/**
	 * Runs a test; an instance of this class is shown in a Frame.
	 */
	public static void main(String[] args) throws Exception {
    //Preferences.userNodeForPackage(AdaptorManager.class).remove( REGISTRY_KEY );
    
    LoggingUtil.setAllRootHandlerLevelsToFinest();
    logger.setLevel( Level.FINE); 
    
		AdaptorManager dvm = new AdaptorManager();
		dvm.setSize(950, 750);
		dvm.showDialog(null);
		System.exit(0);
	}
	/**
	 * Convenience method which opens this instance in a dialog box.
   * @param parent parent for the dialog box.
	 */
	public void showDialog(Component parent) {

		int option =
			JOptionPane.showOptionDialog(
				parent,
				this,
				"Adaptors",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.DEFAULT_OPTION,
				null,
				null,
				null);

	}

	/**
	 * Initialises _configFileChooser_. Sets the last loaded 
	 * config file if available and makes the 
	 * chooser only show XML files.
	 */
	private void initConfigFileChooser() {
		configFileChooser = new JFileChooser();
		FileFilter xmlFilter = new FileFilter() {
			public boolean accept(File f) {
				return f != null
					&& (f.isDirectory() || f.getName().toLowerCase().endsWith(".xml"));
			}
			public String getDescription() {
				return "XML Files";
			}
		};

		String lastChosenFile = prefs.get(CONFIG_FILE_KEY, null);

		if (lastChosenFile != null) {
			configFileChooser.setSelectedFile(new File(lastChosenFile));
		}
		configFileChooser.addChoosableFileFilter(xmlFilter);

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
	}

	
	private void storePrefs() throws ConfigurationException {

		MartRegistry reg = rootAdaptor.getMartRegistry();

		byte[] b = MartRegistryXMLUtils.MartRegistryToByteArray(reg);
		prefs.putByteArray(REGISTRY_KEY, b);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			feedback.warning(e);
		}
	}

	public DSViewAdaptor getRootAdaptor() {
		return rootAdaptor;

	}

	/**
	 * MartManager handles the management of marts such as adding and removing
	 * datasources.
	 * @return
	 */
	public DataSourceManager getMartSettings() {
		return martSettings;
	}

}
