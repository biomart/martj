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
import java.beans.PropertyChangeSupport;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * Represents a list of user options. Some options cause the
 * options available in other widgets to be modified.
 */
public class ListFilterWidget extends FilterWidget 
implements ActionListener {

  private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

  private JComboBox list;

  /**
   * Holds an Option and returns option.getDisplayName() from
   * toString(). This class is used to add Options to the
   * combo box.
   */
  private class OptionWrapper {
    private Option option;
    
    private OptionWrapper(Option option ){
      this.option = option;
    }
    
    public String toString() {
      return (option==null) ? "" : option.getDisplayName();
    }
  }

  /**
   * @param query model to bv synchronised
   * @param filterDescription parameters for this widget
   */
  public ListFilterWidget(Query query, UIFilterDescription filterDescription) {
    super(query, filterDescription);
    
    list = new JComboBox(  );
    list.addActionListener( this );
    
    configureList( list, filterDescription );
    
    setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
    add( new JLabel( filterDescription.getDisplayName() ) );
    add( Box.createHorizontalStrut( 5 ) );
    add( list );
  }

  /**
   * @param list
   * @param filterDescription
   */
  private void configureList(JComboBox list, UIFilterDescription filterDescription) {
    
    // make first option be empty
    list.addItem( new OptionWrapper(null) );
    
    // add each option, via a surrogate, to the list.     
    Option[] options = filterDescription.getOptions();
    for (int i = 0; i < options.length; i++) {
      Option option = options[i];
      list.addItem( new OptionWrapper( option )  );
    }
    
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    // TODO remove item currently set on query if there is one
    
    // TODO add item to query
    
    // TODO propagate OptionPushes if necessary
    
    System.out.println("item selected");
  }

}
