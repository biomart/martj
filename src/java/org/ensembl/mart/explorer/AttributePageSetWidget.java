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

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.Dataset;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AttributePageSetWidget extends InputPage implements ChangeListener{

  final Color SELECTED_FOREGROUND = Color.WHITE;
  final Color SELECTED_BACKGROUND = Color.BLACK;
  
  final Color UNSELECTED_FOREGROUND = Color.BLACK;
  final Color UNSELECTED_BACKGROUND = Color.LIGHT_GRAY;

  private JTabbedPane tabbedPane;
  private int lastSelectedIndex;
  
  /** Whether the widget is in the middle of reverting a user tab change action. */
  private boolean reverting;

	/**
	 * @param query
	 */
	public AttributePageSetWidget(Query query, Dataset dataset) {
		super("Attributes", query);

    tabbedPane = new JTabbedPane();
    tabbedPane.setForeground( SELECTED_FOREGROUND );
    tabbedPane.setBackground( SELECTED_BACKGROUND );
    tabbedPane.addChangeListener( this );
    tabbedPane.setUI(new ConfigurableTabbedPaneUI( SELECTED_BACKGROUND ));
    lastSelectedIndex = 0;
    reverting = false;
    
		AttributePage[] attributePages = dataset.getAttributePages();
		for (int i = 0, n = attributePages.length; i < n; i++) {
			AttributePage page = attributePages[i];
      String name = page.getDisplayName();
			tabbedPane.add( name, new AttributePageWidget(query, name, page) );
		}
    resetTabColors(); 
    
    add( tabbedPane );
    
	}

	/**
   * Listens for tab changes. When a tab change is attempted and attributes are currently
   * set the user is prompted to decide whether to change AttributePage (and loose currently selected attributes)
   * or change anyway. This method also sets the colors of the selected and non-selected tabs.
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e) {

		// Present user with an "Are you sure?" option.
		if (!reverting && query.getAttributes().length > 0) {

			int option =
				JOptionPane.showConfirmDialog(
					this,
					new JLabel(
						"Changing this page will cause all currently selected"
							+ "attributes to be removed from the query. Continue?"),
					"Change Attributes",
					JOptionPane.YES_NO_OPTION);

			if (option != JOptionPane.OK_OPTION) {
				// revert to last selected attribute page
				reverting = true;
				tabbedPane.setSelectedIndex(lastSelectedIndex);
				return;
			}
		}

		if (!reverting) {

			// Remove attributes from model
			Attribute[] attributes = query.getAttributes();
			for (int i = 0; i < attributes.length; i++) {
				query.removeAttribute(attributes[i]);
			}

			resetTabColors();
			lastSelectedIndex = tabbedPane.getSelectedIndex();
		}

		reverting = false;

	}

	/**
	 * 
	 */
	private void resetTabColors() {
    
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      
      Color foreground = UNSELECTED_FOREGROUND;
      Color background = UNSELECTED_BACKGROUND;
      
      if ( i==tabbedPane.getSelectedIndex() ) {
        System.out.println("tab change to " + i);
        foreground = SELECTED_FOREGROUND;
        background = SELECTED_BACKGROUND;
      }
      
      tabbedPane.setForegroundAt(i, foreground );
      tabbedPane.setBackgroundAt(i, background );
    }
	}


}
