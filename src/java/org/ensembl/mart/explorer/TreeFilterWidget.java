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
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.FilterDescription;

/**
 * Represents a set of user options as a tree.
 * Component consists of a label, text area and button. 
 */
public class TreeFilterWidget extends FilterWidget {

	private JMenuBar menu = null;
	private JLabel label = null;
	private JTextField selected = new JTextField(30);
	private JButton button = new JButton("change");
	private String propertyName;
  private Map optionToName = new HashMap();
  private Option option = null;
  private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

	/**
   * 
	 * @param query
	 * @param filterDescription
	 */
	public TreeFilterWidget(FilterGroupWidget filterGroupWidget, Query query, FilterDescription filterDescription) {
		super(filterGroupWidget, query, filterDescription);

    // default property name.
    this.propertyName = filterDescription.getInternalName();

		label = new JLabel(filterDescription.getDisplayName());
		selected.setEditable(false);
		selected.setMaximumSize(new Dimension(400, 27));
		initMenu(filterDescription);

		Box box = Box.createHorizontalBox();
		box.add(menu);
		box.add(label);
    box.add(Box.createHorizontalStrut(5));
    box.add(button);		box.add(Box.createHorizontalStrut(5));
		box.add(selected);


		setLayout(new BorderLayout());
		add(box, BorderLayout.NORTH);

	}

	/**
	 * Initialise the menu with the menu "tree" of options. Make button
	 * pressses cause the menu to be displayed.
	 * @return
	 */
	private void initMenu(FilterDescription filterDescription) {

		// We use a JMenuBar to represent a "tree" of options.

		menu = new JMenuBar();

		// make the menu appear beneath the row of components 
		// containing the label, textField and button when displayed.
		menu.setMaximumSize(new Dimension(0, 100));

		final JMenu topLevelOptions = new JMenu();
		menu.add(topLevelOptions);

		// Clicking the button causes the top level options to be displayed.
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// show the options
				topLevelOptions.doClick();
			}

		});

		addOptions(topLevelOptions, filterDescription.getOptions(), "");

	}

    



	/**
   * Recursively add menus of menu items and submenus made up of options of options.
	 * @param menu
	 * @param options
	 */
	private void addOptions(JMenu menu, Option[] options, String baseName) {

		for (int i = 0, n = options.length; i < n; i++) {
			
      final Option option = options[i];
			
			if (option.getOptions().length == 0) {
				
        JMenuItem item = new JMenuItem(option.getDisplayName());
				menu.add(item);
				item.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          selectOption( option );
        }});
        
        optionToName.put( option, baseName+" "+ option.getDisplayName() );
        
			} else {
				String name = option.getDisplayName(); 
        JMenu subMenu = new JMenu( name );
				menu.add(subMenu);
				addOptions(subMenu, option.getOptions(), baseName + " " +name);
        
			}
		}

	}

  private void selectOption( Option option ) {
    
    String name = (String)optionToName.get( option );
    selected.setText( name );
    setNodeLabel( null, name );
    Option old = this.option;
    this.option = option;
    changeSupport.firePropertyChange( getPropertyName(), old, option);

  }

	/**
   * Listens for relevant changes on the Query.
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub

	}


  /**
   * 
   * @return selected option if one is selected, otherwise null.
   */
	public Option getOption() {
		return option;
	}

	/**
	 * @param currentDatasetName
	 */
	public void setOption(Option option) {
		this.option = option;
    selected.setText( (String)optionToName.get( option ) );
    System.out.println( "setting option " + option );
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
    // TODO Auto-generated method stub

  }

}
