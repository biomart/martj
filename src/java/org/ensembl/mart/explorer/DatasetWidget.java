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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.Option;

/**
 * Represents dataset views available to user as a tree. The tree is constructed from the 
 * dataset views display names. When a user selects
 * a dataset view the underlying query is updated. The widget also listens for
 * dataset  
 */
public class DatasetWidget
	extends InputPage
	implements PropertyChangeListener {

  private Logger logger = Logger.getLogger(DatasetWidget.class.getName());
  

  /** selected datasetView */
  private DatasetView datasetView = null;
  
  /** Available datasetViews */
  private DatasetView[] datasetViews = null;
  
  /** all options currently loaded */
  private HashSet allOptions;

  /** Null option */
	private Option nullOption;

  /** Menu item in tree for null option */
  private JMenuItem nullItem;
  

  /** last user selected option */
	private Option lastSelectedOption;

	

	/** represent a "tree" of options. */
	private JMenuBar treeMenu = new JMenuBar();
	private JMenu treeTopOptions = null;
	private JLabel label = null;
	private JTextField currentSelectedText = new JTextField(30);
	private JButton button = new JButton("change");
	private String propertyName;
	private Map valueToOption = new HashMap();
	private Option option = null;
	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

  

  
  
	/**
	 * 
	 * @param query
	 * @param filterDescription
	 */
	public DatasetWidget(Query query) {

		super(query, "Dataset");

		try {
			nullOption = new Option("No Filter", true);
		} catch (ConfigurationException e) {
			// shouldn't happen
			e.printStackTrace();
		}
		nullItem = new JMenuItem(nullOption.getInternalName());
		nullItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setOption(nullOption);
			}
		});

		// default property name.
		this.propertyName = "dataset";

		label = new JLabel("Dataset");
		currentSelectedText.setEditable(false);
		currentSelectedText.setMaximumSize(new Dimension(400, 27));

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				showTree();
			}
		});

		// make the menu appear beneath the row of components 
		// containing the label, textField and button when displayed.
		treeMenu.setMaximumSize(new Dimension(0, 100));

		Box box = Box.createHorizontalBox();
		box.add(treeMenu);
		box.add(label);
		box.add(Box.createHorizontalStrut(5));
		box.add(button);
		box.add(Box.createHorizontalStrut(5));
		box.add(currentSelectedText);

		setLayout(new BorderLayout());
		add(box, BorderLayout.NORTH);

		option = nullOption;
		lastSelectedOption = option;

	}

	/**
	 * Adds menu items and submenus to menu based on contents of _options_. A submenu is added 
	 * when an option contains
	 * sub options. If an option has no sub options it is added as a leaf node. This method calls
	 * itself recursively to build up the menu tree.
	 * @param menu menu to add options to
	 * @param options options to be added, method does nothing if null.
	 * @param prefix prepended to option.getDisplayName() to create internal name for menu item 
	 * created for each option.
	 */
	private void addOptions(JMenu menu, Option[] options, String prefix) {

		for (int i = 0; options != null && i < options.length; i++) {

			final Option option = options[i];

			String displayName = option.getDisplayName();
			String qualifiedName = prefix + " " + displayName;

			if (option.getOptions().length == 0) {

				// add menu item
				JMenuItem item = new JMenuItem(displayName);
				item.setName(qualifiedName);
				menu.add(item);
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						setOption(option);
					}
				});

				valueToOption.put(option.getValue(), option);

			} else {

				// Add sub menu
				JMenu subMenu = new JMenu(displayName);
				menu.add(subMenu);
				addOptions(subMenu, option.getOptions(), qualifiedName);

			}
		}

	}

	/**
	 * 
	 * @return selected option if one is selected, otherwise null.
	 */
	public Option getOption() {
		return option;
	}

	/**
	 * Default value is filterDescription.getInternalName().
	 * @return propertyName included in PropertyChangeEvents.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#addPropertyChangeListener(java.beans.PropertyChangeListener)
	 */
	public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#removePropertyChangeListener(java.beans.PropertyChangeListener)
	 */
	public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.FilterWidget#setOptions(org.ensembl.mart.lib.config.Option[])
	 */
	public void setOptions(Option[] options) {

		if (datasetName != null)
			removeDatasetFromQuery();

		setDatasetName(null);

		// reset the maps so we can can find things later
		valueToOption.clear();

		treeMenu.removeAll();

		treeTopOptions = new JMenu();
		treeMenu.add(treeTopOptions);

		//  add the nullItem to the top of the list, user selects this to clear
		// choice.
		treeTopOptions.add(nullItem);
		valueToOption.put(nullOption.getValue(), nullOption);

		addOptions(treeTopOptions, options, "");

		allOptions = new HashSet(valueToOption.values());
	}

	/**
	 * 
	 */
	private void removeDatasetFromQuery() {
		query.setDatasetName(null);

	}

	public void showTree() {
		treeTopOptions.doClick();
	}

	/**
	 * Sets datasetView. If datasetView is null then "No Filter" is selected
	 */
	protected void setDatasetView(DatasetView datasetView) {
		this.datasetView = datasetView;

		if (datasetView == null) {

			updateDisplay(null);

		} else {

			Option option = (Option) valueToOption.get(datasetName);
			updateDisplay(option);

		}
	}

	/**
	 * Sets the currentlySelectedText based on
	 * the option. If option is null these values are cleared.
	 * @param option option selected
	 */
	private void updateDisplay(Option option) {
		String name = "";
		if (option != null && option != nullOption)
			name = option.getDisplayName();
		currentSelectedText.setText(name);

	}

	/**
	 * 
	 * @param option should be one of the options currently available to this filter.
	 * @throws IllegalArgumentException if option unavailable in filter.
	 */
	public void setOption(Option option) {

		if (!allOptions.contains(option))
			throw new IllegalArgumentException(
				"Option is unailable in filter: " + option);

		if (option == lastSelectedOption)
			return;

		//  Confirm user really wants to change dataset
		if (query.getAttributes().length > 0 || query.getFilters().length > 0) {

			int o =
				JOptionPane.showConfirmDialog(
					this,
					new JLabel("Changing the dataset will cause the query settings to be cleared. Continue?"),
					"Change Attributes",
					JOptionPane.YES_NO_OPTION);

			// undo if user changes mind
			if (o != JOptionPane.OK_OPTION) {

				updateDisplay(lastSelectedOption);
				return;
			}
		}

		updateDisplay(option);

		Option old = this.option;
		this.option = option;
		changeSupport.firePropertyChange(getPropertyName(), old, option);

		String oldDataset = datasetName;

		if (option == nullOption) {

			removeDatasetFromQuery();
			datasetName = null;

		} else {

			String value = null;
			if (option != null) {
				String tmp = option.getValueFromContext();
				if (tmp != null && !"".equals(tmp)) {
					value = tmp;
				}

				if (value != null)
					datasetName = value;

			}
		}

		lastSelectedOption = option;
		updateQueryDatasetName(datasetName);

	}

	/**
	   * Updates the dataset on the query. Removes oldDataset if not null and
	   * sets newDataset if not null.
	   * @param newDatasetName name of dataset to be set, can be null
	   */
	private void updateQueryDatasetName(String newDatasetName) {
		query.removePropertyChangeListener(this);
		query.setDatasetName(newDatasetName);
		query.addPropertyChangeListener(this);
	}

	/**
	   * Responds to a change in dataset on the query. Updates the state of
	   * this widget by changing the currently selected item in the list.
	   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	   */
	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getSource() == query && evt.getPropertyName().equals("dataset")) {

			String newDataset = (String) evt.getNewValue();

			Option option = nullOption;
			if (newDataset != null)
				option = (Option) valueToOption.get(newDataset);
			assert option != null;
			setOption(nullOption);

		}

	}

	/**
	 * @return
	 */
	public DatasetView[] getDatasetViews() {
		return datasetViews;
	}

	/**
	 * @param datasetViews
	 */
	public void setDatasetViews(DatasetView[] datasetViews) {
		this.datasetViews = datasetViews;

		setOptions(convert(datasetViews));
	}

	/**
	 * @param datasetViews
	 * @return
	 */
	private Option[] convert(DatasetView[] datasetViews) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	public DatasetView getDatasetView() {
		return datasetView;
	}

	/**
	 * @param view
	 */
	public void setDatasetView(DatasetView view) {
		datasetView = view;
	}

}
