/* Generated by Together */

package org.ensembl.mart.explorer.gui;

import javax.swing.*;

public class Tool {
    /**
     * Prepends value to the front of the list of items in the combo box.
     * Does nothing if value is null.
     */
  public static final void prepend( String value, JComboBox combo ) {
    Object selected = combo.getSelectedItem();

    if ( value!=null ) {
			combo.getModel().setSelectedItem( value );
			ComboBoxModel m = combo.getModel();
			int n = m.getSize();
      boolean found = false;
      for( int i=0; i<n && !found; i++ )
				found = value.equals(m.getElementAt( i ));
      if ( !found )
   			combo.insertItemAt( value, 0 );
    }

    if ( selected!=null
         && !"".equals( selected )
         && combo.getModel().getElementAt(1)!=selected)
      combo.insertItemAt( selected, 1);
	}

    /**
     * Returns the selected item, null if none selected.
     */
	static final String selected( JComboBox combo ) {
    Object item = combo.getModel().getSelectedItem();
  	if ( item==null ) return null;
    else return item.toString();
	}


  /**
   * Makes the empty string "" the first item in the list.
   * The previous item is pushed down to the second position.
   */
  public static final void clear( JComboBox combo ) {
    prepend( "", combo );
  }
}
