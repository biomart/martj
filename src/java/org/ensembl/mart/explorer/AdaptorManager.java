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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.MartRegistry;
import org.ensembl.mart.lib.config.MartRegistryXMLUtils;
import org.ensembl.mart.lib.config.RegistryDSViewAdaptor;
import org.ensembl.mart.lib.config.SimpleDSViewAdaptor;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;

/**
 * Widget representing the availabled adaptors.
 * Enables the user to select add and delete 
 * adaptors.
 * 
 * 
 * 
 * <p>
 * The datasets are represented hierarchically in a tree.  
  * The tree is constructed from all of the datasetNames 
 * displayNames. They are arranged as a tree of options.
 * Where "__" appears in the displayName it is used as a
 * branch point in a tree menu.
 * For example an array of datasetViews with these datasetNames:
 * </p>
 *
 *  <pre>
 * AAA
 * BBB
 * CCCCC__DDDDD
 * CCCCC__DDDDD__EEEE
 * CCCCC__DDDDD__FFFF
 * 
 * </pre>
 * Would be rendered as this tree:
 * <pre>
 * -AAA
 * -BBB
 * -CCCCC
 *       -DDDDD-EEEE
 *             -FFFF
 * </pre>
 *             
 * 
 * A single dataset could be represented by multiple datasetViews.
 * 
 * TODO import from database
 */
public class AdaptorManager extends Box {

	private DataSourceManager martSettings = new DataSourceManager(this);
	private Logger logger = Logger.getLogger(AdaptorManager.class.getName());
	private Feedback feedback = new Feedback(this);
	// Used to find and remove separators in datasetView.displayNames
	private Matcher separatorMatcher = Pattern.compile("__").matcher("");
	private static final String CONFIG_FILE_KEY = "CONFIG_FILE_KEY";
	private static final String REGISTRY_KEY = "REGISTRY_KEY";
	private String none = "None";

	// --- state
	private DatasetView selected = null;
	private RegistryDSViewAdaptor adaptor = new RegistryDSViewAdaptor();
	private Map datasetNameToDatasetView = new HashMap();
	/** Persistent preferences object used to hold user history. */
	private Preferences prefs;

	// --- GUI components
	private JMenuBar treeMenu = new JMenuBar();
	private JMenu treeTopMenu = new JMenu();
	private JTextField selectedTextField = new JTextField(30);
	private JButton button = new JButton("change");
	private JMenuItem noneMenuItem = new JMenuItem("None");
	private JFileChooser configFileChooser;

	/**
	 * This widget is part of a system based on the MVC design pattern. 
	 * From this perspective the widget is a View and a Controller
	 * and the query is the Model.
	 * @param query underlying model for this widget.
	 */
	public AdaptorManager() {
		super(BoxLayout.Y_AXIS);

		prefs = Preferences.userNodeForPackage(this.getClass());
    //prefs.remove( REGISTRY_KEY );

    loadPrefs();
    
		selectedTextField.setText(none);
		initConfigFileChooser();

		noneMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				doSelect(none);
			}
		});

		selectedTextField.setEditable(false);
		selectedTextField.setMaximumSize(new Dimension(400, 27));

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				showTree();
			}
		});

		// make the menu appear beneath the row of components 
		// containing the label, textField and button when displayed.
		treeMenu.setMaximumSize(new Dimension(0, 100));
		treeMenu.add(treeTopMenu);


    JButton addDB = new JButton("Add Database");
    addDB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doAddDatabase();
      }
    });

		JButton addFile = new JButton("Add File");
		addFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doLoadtFromFile();
			}
		});
		JButton delete = new JButton("Delete");
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doDelete();
			}
		});

		Box top = Box.createHorizontalBox();
		top.add(treeMenu);
		top.add(new JLabel("Dataset"));
		top.add(Box.createHorizontalStrut(5));
		top.add(button);
		top.add(Box.createHorizontalStrut(5));
		top.add(selectedTextField);

		Box bottom = Box.createHorizontalBox();
		bottom.add(Box.createHorizontalGlue());
    bottom.add(addDB);
    bottom.add(addFile);
		bottom.add(delete);

		add(top);
		add(bottom);

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
      if ( b.length>0 ) {
        reg = MartRegistryXMLUtils.ByteArrayToMartRegistry(b);
      }
        
      if ( reg!=null ) {
        RegistryDSViewAdaptor tmp = new RegistryDSViewAdaptor(reg);
        DSViewAdaptor[] adaptors = tmp.getAdaptors();
        for (int i = 0; i < adaptors.length; i++) 
          add(adaptors[i], false);

      }
    } catch (ConfigurationException e1) {
      e1.printStackTrace();
    }

	}

	/**
	 * Delete selected datasetview.
	 */
	public void doDelete() {

		if (selected == null) return ;
			try {

				// decide which will be the "next" datasetView
				List names = new ArrayList(datasetNameToDatasetView.keySet());
				Collections.sort(names);
				String name = selected.getDisplayName();

				int newIndex = -1;
				int index = names.indexOf(name);
				int max = names.size();
				if (index + 1 < max)
					newIndex = index + 1;
				else if (index - 1 <= max)
					newIndex = index - 1;

				String newSelectedName =
					(newIndex == -1) ? none : (String) names.get(newIndex);

				// remove the selected item
				adaptor.removeDatasetView(selected);
				datasetNameToDatasetView.remove(name);

				// select the "next" item
				doSelect(newSelectedName);

			} catch (ConfigurationException e) {
				feedback.warning(
					"Failed to delete dataset view " + selected.getDisplayName(),
					e);
			}
		
    
    try {
			storePrefs();
		} catch (ConfigurationException e1) {
			feedback.warning(e1);
		}
	}

	/**
	 * Presents the user with a file chooser dialog with which she can 
	 * choose a configuration file. 
	 */
	protected void doLoadtFromFile() {

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
				DatasetView dsv = adaptor.getDatasetViews()[0];
				String name = dsv.getDisplayName();
//				datasetNameToDatasetView.put(name, dsv);
        add( adaptor );        
				doSelect(name);

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

	}

	public void showTree() {
		try {
			if (adaptor != null) {

				updateMenu(adaptor.getDatasetViews());
				treeTopMenu.doClick();
			}
		} catch (ConfigurationException e) {
			feedback.warning(e);
		}

	}

	/**
	 * @param displayName display name of the selected datasetView.
	 */
	private void doSelect(String displayName) {

		selectedTextField.setText(displayName);

		if (displayName == none)
			selected = null;
		else
			selected = (DatasetView) datasetNameToDatasetView.get(displayName);

	}

	/**
	 * Unpacks the datasetViews into several sets and maps that enable
	 * easy lookup of information.
	 * 
	 * displayName -> shortName
	 * datasetName -> datasetView | List-of-datasetViews
	 * displayName -> datasetView | List-of-datasetViews
	 * 
	 * @param datasetViews dataset views, should be sorted by displayNames.
	 */
	private void unpack(DatasetView[] datasetViews) {

		Set availableDisplayNames = new HashSet();
		Set availableDatasetNames = new HashSet();
		Map displayNameToDatasetView = new HashMap();
		Map displayNameToShortName = new HashMap();

		if (datasetViews == null)
			return;

		Set clashingDisplayNames = new HashSet();
		Set clashingDatasetNames = new HashSet();

		for (int i = 0; i < datasetViews.length; i++) {

			DatasetView view = datasetViews[i];

			String displayName = view.getDisplayName();
			if (availableDisplayNames.contains(displayName))
				clashingDisplayNames.add(view);
			else
				availableDisplayNames.add(displayName);

			String datasetName = view.getInternalName();
			if (availableDatasetNames.contains(datasetName))
				clashingDatasetNames.add(view);
			else
				availableDatasetNames.add(datasetName);

			String[] elements = displayName.split("__");
			String shortName = elements[elements.length - 1];
			displayNameToShortName.put(displayName, shortName);
		}

		for (int i = 0; i < datasetViews.length; i++) {

			DatasetView view = datasetViews[i];

			String displayName = view.getDisplayName();
			if (clashingDisplayNames.contains(view)) {
				List list = (List) displayNameToDatasetView.get(displayName);
				if (list == null) {
					list = new LinkedList();
					displayNameToDatasetView.put(displayName, list);
				}
				list.add(view);
			} else {
				displayNameToDatasetView.put(displayName, view);
			}

			String datasetName = view.getInternalName();
			if (clashingDatasetNames.contains(view)) {
				List list = (List) datasetNameToDatasetView.get(datasetName);
				if (list == null) {
					list = new LinkedList();
					datasetNameToDatasetView.put(datasetName, list);
				}
				list.add(view);
			} else {
				datasetNameToDatasetView.put(datasetName, view);
			}

		}

	}

	/**
	 * Update the menu to reflect the datasetViews. 
	 * Menu item names and position in the menu tree are
	 * derived from the displayNames of the datasetViews. 
	 * @param datasetViews
	 */
	private void updateMenu(DatasetView[] datasetViews) {

		treeTopMenu.removeAll();
		treeTopMenu.add(noneMenuItem);

		if (datasetViews == null || datasetViews.length == 0)
			return;

		//  we need the dsvs sorted so we can construct the menu tree
		// by parsing the array once
		Arrays.sort(datasetViews, new Comparator() {
			public int compare(Object o1, Object o2) {
				DatasetView d1 = (DatasetView) o1;
				DatasetView d2 = (DatasetView) o2;
				return d1.getDisplayName().compareTo(d2.getDisplayName());
			}
		});

		String[][] tree = new String[][] {
		};
		Map menus = new HashMap();

		for (int i = 0; i < datasetViews.length; i++) {
			final DatasetView view = datasetViews[i];

			final String datasetName = view.getDisplayName();

			String[] elements = datasetName.split("__");

			for (int j = 0; j < elements.length; j++) {

				String substring = elements[j];

				JMenu parent = treeTopMenu;
				if (j > 0)
					parent = (JMenu) menus.get(elements[j - 1]);

				if (j + 1 == elements.length) {

					// user selectable leaf node
					JMenuItem item = new JMenuItem(substring);
					item.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							doSelect(view.getDisplayName());
						}
					});
					parent.add(item);

				} else {

					// intermediate menu node
					JMenu menu = (JMenu) menus.get(elements[j]);
					if (menu == null) {
						menu = new JMenu(substring);
						menus.put(substring, menu);
						parent.add(menu);
					}

				}

			}
		}

	}

	/**
	 * Runs a test; an instance of this class is shown in a Frame.
	 */
	public static void main(String[] args) throws Exception {
		AdaptorManager dvm = new AdaptorManager();
		dvm.setSize(950, 750);
		dvm.showDialog(null);
		System.exit(0);
	}
	/**
	 * @return
	 */
	public DatasetView getSelected() {
		return selected;
	}

	/**
	 * Responds to a change in dataset view on the query. Updates the state of
	 * this widget by changing the currently selected item in the list.
	 */
	public void datasetViewChanged(
		Query query,
		DatasetView oldDatasetView,
		DatasetView newDatasetView) {
		String s = "";
		if (newDatasetView != null)
			s = newDatasetView.getDisplayName();
		selectedTextField.setText(s);
	}

	/**
	 * Opens this DatasetViewManager as a dialog box. User can then
	 * select, add and remove Marts.
	 * @param parent parent for the dialog box.
	 * @return true if user selected ok, otherwise false.
	 */
	public boolean showDialog(Component parent) {

		int option =
			JOptionPane.showOptionDialog(
				parent,
				this,
				"DatasetViews",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.DEFAULT_OPTION,
				null,
				null,
				null);

		return option == JOptionPane.OK_OPTION;

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
			return dsv != null && adaptor.supportsInternalName(dsv.getInternalName());
		} catch (ConfigurationException e) {
			// Shouldn't happen
			feedback.warning(e);
		}

		return false;
	}

	/**
	 * Adds dataset.
	 * @param datasetView
	 */
	public void add(DatasetView datasetView) throws ConfigurationException {
		add(new SimpleDSViewAdaptor(datasetView));
	}

	public void setSelected(DatasetView selected) {
		this.selected = selected;
    String name = (selected!=null) ? selected.getDisplayName() : null; 
    doSelect(name);

	}

	public void add(DSViewAdaptor a) throws ConfigurationException {
    add( a, true );    
	}

  private void add( DSViewAdaptor a, boolean storePrefs) throws ConfigurationException{

    DatasetView[] dvs = a.getDatasetViews();
    for (int i = 0; i < dvs.length; i++) {
      DatasetView dv = dvs[i];
      datasetNameToDatasetView.put(dv.getDisplayName(), dv);
    }
    this.adaptor.add(a);

    if ( storePrefs ) 
      storePrefs();
    
  }


	private void storePrefs() throws ConfigurationException {
    
    MartRegistry reg = adaptor.getMartRegistry();
    
    byte[] b =
      MartRegistryXMLUtils.MartRegistryToByteArray(reg);
    prefs.putByteArray(REGISTRY_KEY, b);
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      feedback.warning(e);
    }
	}

	public DSViewAdaptor getAdaptor() {
		return adaptor;

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
