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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * Represents a set of user selectable options arranged as a tree.
 * Components consists of a label, text area and button. The text area
 * contains the current selection and the tree causes the option
 * tree to be displayed.
 */
public class TreeFilterWidget extends FilterWidget {

  private JMenuBar menu = null;
	private JLabel label = null;
	private JTextField selected = new JTextField(30);
	private JButton button = new JButton("change");
	private String propertyName;

	/**
	 * @param query
	 * @param name
	 */
	public TreeFilterWidget(Query query, UIFilterDescription filterDescription) {
		super(query, filterDescription);

		label = new JLabel(filterDescription.getDisplayName());
		selected.setEditable(false);
		selected.setMaximumSize(new Dimension(100, 30));
    initMenu( filterDescription );
    
		Box box = Box.createHorizontalBox();
    box.add( menu ); 
    box.add(label);
    box.add(Box.createHorizontalStrut(5));
		box.add(selected);
		box.add(Box.createHorizontalStrut(5));
    box.add( button );
  
		setLayout(new BorderLayout());
		add(box, BorderLayout.NORTH);
    
	}

	/**
   * Initialise the menu with the menu "tree" of options. Make button
   * pressses cause the menu to be displayed.
	 * @return
	 */
	private void initMenu(UIFilterDescription filterDescription) {
		
    // We use a JMenuBar to represent a "tree" of options.
    
    menu = new JMenuBar();
    
    // make the menu appear beneath the row of components 
    // containing the label, textField and button when displayed.
    menu.setMaximumSize( new Dimension(0,100) );
		
    final JMenu topLevelOptions = new JMenu();
    menu.add( topLevelOptions );

		// Clicking the button causes the top level options to be displayed.
    button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				// show the options
        topLevelOptions.doClick();
			}

		});

    addOptions(topLevelOptions, filterDescription.getOptions());

	}

	/**
	 * @param menu
	 * @param options
	 */
	private void addOptions(JMenu menu, Option[] options) {

		for (int i = 0, n = options.length; i < n; i++) {
			Option option = options[i];
			if (option.getOptions().length == 0)
				menu.add(new JMenuItem(option.getDisplayName()));
			else {
        JMenu subMenu = new JMenu(option.getDisplayName());
        menu.add( subMenu );
				addOptions( subMenu, option.getOptions());
			}
		}

	}

	private void showMenu() {
		// TODO Auto-generated method stub
		//menu.show(label, label.getSize().width / 2, label.getSize().height);
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub

	}

	public String getSelected() {
		return selected.getText();
	}

	/**
	 * @param currentDatasetName
	 */
	public void setSelected(String currentDatasetName) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @param string
	 */
	public void setPropertyName(String string) {
		propertyName = string;
	}

}
