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
import javax.swing.JTextField;

import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * Represents a set of user options as a tree.
 * Component consists of a label, text area and button. 
 */
public class TreeFilterWidget extends FilterWidget {

	private HashSet allOptions;

	private JMenuItem nullItem;

	private Option nullOption;

	private Option lastSelectedOption;

	private Logger logger = Logger.getLogger(TreeFilterWidget.class.getName());

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

	private Filter filter = null;

	/**
	 * 
	 * @param query
	 * @param filterDescription
	 */
	public TreeFilterWidget(
		FilterGroupWidget filterGroupWidget,
		Query query,
		FilterDescription filterDescription) {
		super(filterGroupWidget, query, filterDescription);

		try {
			nullOption = new Option("No Filter", "true");
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
		this.propertyName = filterDescription.getInternalName();

		label = new JLabel(filterDescription.getDisplayName());
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
		setOptions(filterDescription.getOptions());

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
	 * Sets the currentlySelectedText and node label based on
	 * the option. If option is null these values are cleared.
	 * @param option
	 */
	private void updateDisplay(Option option) {
		String name = "";
		if (option != null && option != nullOption)
			name = option.getDisplayName();
		currentSelectedText.setText(name);
		setNodeLabel(fieldName, name);
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

	/**
	 * Set the propertyName to some specific value.
	 * @param string
	 */
	public void setPropertyName(String string) {
		propertyName = string;
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

		if (filter != null)
			removeFilterFromQuery(filter);

		setFilter(null);

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

	public void showTree() {
		treeTopOptions.doClick();
	}

	/**
	 * Sets filter and also causes the appropriate item in the tree to be selected and any relevant
	 * PushOption to be assigned. If filter is null then "No Filter" is selected
	 * and and PushOption are unassigned.
	 * @see org.ensembl.mart.explorer.FilterWidget#setFilter(org.ensembl.mart.lib.Filter)
	 */
	protected void setFilter(Filter filter) {
		this.filter = filter;

		if (filter == null) {

			updateDisplay(null);
			unassignPushOptions();

		} else {

			Option option = (Option) valueToOption.get(filter.getValue());
			updateDisplay(option);
			assignPushOptions(option.getPushActions());

		}
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

		updateDisplay(option);

		Option old = this.option;
		this.option = option;
		changeSupport.firePropertyChange(getPropertyName(), old, option);

		unassignPushOptions();

		Filter oldFilter = filter;

		if (option == nullOption) {

			removeFilterFromQuery(filter);
			filter = null;

		} else {

			String value = null;
			if (option != null) {
				String tmp = option.getValueFromContext();
				if (tmp != null && !"".equals(tmp)) {
					value = tmp;
				}

				if (value != null) 
					filter = new InputPageAwareBasicFilter(option, this);
				

				setNodeLabel(fieldName, option.getDisplayName());

				assignPushOptions(option.getPushActions());
			}
		}

		lastSelectedOption = option;
		// query=null when used for dataset so must precheck
		if (query != null)
			updateQueryFilters(oldFilter, filter);

	}
}
