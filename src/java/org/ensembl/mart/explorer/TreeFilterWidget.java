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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * Represents a set of user selectable options arranged as a tree.
 * Components consists of a label, text area and button. The text area
 * contains the current selection and the tree causes the option
 * tree to be displayed.
 */
public class TreeFilterWidget extends FilterWidget {

  private JTextField selected = new JTextField( 30 );
  private JButton button = new JButton("change");
  private JPopupMenu menu;

  /**
   * @param query
   * @param name
   */
  public TreeFilterWidget(Query query, UIFilterDescription filterDescription) {
    super(query, filterDescription);
    
    selected.setEditable( false );
    button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				showMenu();		
			}
		});
    
    initMenu( filterDescription );
    
    add( new JLabel( filterDescription.getDisplayName() ) );
    add( selected );
    add( button );
    
    
  }

  /**
	 * @return
	 */
	private void initMenu( UIFilterDescription filterDescription ) {
		menu = new JPopupMenu(); 
		
	}


  private void showMenu() {
    // TODO Auto-generated method stub
        
  }


	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}

}
