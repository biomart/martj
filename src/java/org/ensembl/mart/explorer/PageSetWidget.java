/*
 * Created on Aug 15, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PageSetWidget extends InputPage implements ChangeListener {

  /**
   * @param name Name of this widget
   * @param query model this leafWidgets listens to and manipulates
   */
  public PageSetWidget(Query query, String name) {
    super(query, name);
    
    tabbedPane = new JTabbedPane();
    tabbedPane.setForeground( SELECTED_FOREGROUND );
    tabbedPane.setBackground( SELECTED_BACKGROUND );
    tabbedPane.addChangeListener( this );
    tabbedPane.setUI(new ConfigurableTabbedPaneUI( SELECTED_BACKGROUND ));
    lastSelectedIndex = 0;
    add( tabbedPane );
  }

  protected final Color SELECTED_FOREGROUND = Color.WHITE;

  protected final Color SELECTED_BACKGROUND = Color.BLACK;

  protected final Color UNSELECTED_FOREGROUND = Color.BLACK;

  protected final Color UNSELECTED_BACKGROUND = Color.LIGHT_GRAY;

  protected JTabbedPane tabbedPane;

  protected int lastSelectedIndex;

  /**
     * Listens for tab changes. When a tab change is attempted and attributes are currently
     * set the user is prompted to decide whether to change AttributePage (and loose currently selected attributes)
     * or change anyway. This method also sets the colors of the selected and non-selected tabs.
  	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
  	 */
  public void stateChanged(ChangeEvent e) {
  
  	// Present user with an "Are you sure?" option.
  	if (query.getAttributes().length > 0) {
  
  		int option =
  			JOptionPane.showConfirmDialog(
  				this,
  				new JLabel(
  					"All currently attributes will be removed from the query "
  						+ "\nif you change this page. Continue?"),
  				"Change Attributes",
  				JOptionPane.YES_NO_OPTION);
  
  		if (option != JOptionPane.OK_OPTION) {
  			// change selected tab back to the selected one 
  			tabbedPane.removeChangeListener(this);
  			tabbedPane.setSelectedIndex(lastSelectedIndex);
  			tabbedPane.addChangeListener(this);
  			return;
  		}
  	}
  
  	// Remove attributes from model
  	Attribute[] attributes = query.getAttributes();
  	for (int i = 0; i < attributes.length; i++) {
  		query.removeAttribute(attributes[i]);
  	}
  
  	resetTabColors();
  	lastSelectedIndex = tabbedPane.getSelectedIndex();
  
  }

  /**
  	 * 
  	 */
  protected void resetTabColors() {
    
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      
      Color foreground = UNSELECTED_FOREGROUND;
      Color background = UNSELECTED_BACKGROUND;
      
      if ( i==tabbedPane.getSelectedIndex() ) {
        foreground = SELECTED_FOREGROUND;
        background = SELECTED_BACKGROUND;
      }
      
      tabbedPane.setForegroundAt(i, foreground );
      tabbedPane.setBackgroundAt(i, background );
    }
  }

}
