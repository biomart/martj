/*
 * Created on Aug 4, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.explorer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.MartConfiguration;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DatasetSelectionPage extends InputPage implements ChangeListener{

  /**
   * @param query
   * @param config
   */
  public DatasetSelectionPage(Query query, QueryEditor editor ) {
    
    super("Dataset", query);
    
    this.editor = editor;
    
    initPage( editor.getMartConfiguration() );
  } 

  /**
	 * @param config
	 */
	private void initPage(MartConfiguration config ) {
    
    
    // Collect all datasets from the config
    Dataset[] datasets = config.getDatasets();
    
    // create a LabelledComboBox containing the datasets
    combo = new LabelledComboBox("Dataset");
    combo.setEditable( false );
    combo.addItem( "" );
    for (int i = 0, n = datasets.length; i < n; i++) {
      combo.addItem( datasets[i].getDisplayName() );
		}
    
    // Add Box to page
    add( combo );
		
    combo.addChangeListener( this );
	}
 
  /**
   * Listens for changes in the combo box. Resets query.dataet and causes the tree to 
   * redraw.
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void stateChanged(ChangeEvent e) {
    
    String selection = combo.getText();
    if ( selection!="" ) { 

      // update the nodes label      
      setNodeLabel( getName(), selection );
      // tell the tree to redraw the node with it's new label
      editor.nodeChanged( getNode() );
    
      // set new value on query
      query.setStarBases( new String[] {selection});
      
    }
  }

   
  private QueryEditor editor;
  private LabelledComboBox combo;
  private String nodeLabel;


	/* (non-Javadoc)
	 * @see org.ensembl.mart.explorer.InputPage#getNodeLabel()
	 */
	public String getNodeLabel() {
		return nodeLabel;
	}

}
