/*
 * Created on Aug 4, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.explorer;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
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


  private Object currentSeletion;

  /**
   * Proxy for a Dataset where toString() returns dataset.displayName(). Used for printing in ComboName.
   */
  private class DatasetWrapper{
    private final Dataset dataset;
    
    private DatasetWrapper( Dataset dataset ) {
      this.dataset = dataset;
    }
    
    public String toString() {
      return dataset.getDisplayName();
    }
  }



  /**
   * @param query
   * @param config
   */
  public DatasetSelectionPage(Query query, MartConfiguration config) {
    
    super(query, "Dataset");
    
    
    initPage( config );
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
      combo.addItem( new DatasetWrapper( datasets[i])  );
		}
    
    setLayout( new FlowLayout() );
    
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

		if ( combo.getSelectedItem() == currentSeletion )
			return;

		boolean update = true;

		if (query.getAttributes().length > 0 || query.getFilters().length > 0) {

			int option =
				JOptionPane.showConfirmDialog(
					this,
					new JLabel("Changing the dataset will cause the query settings to be cleared. Continue?"),
					"Change Attributes",
					JOptionPane.YES_NO_OPTION);

			if (option != JOptionPane.OK_OPTION) {
				update = false;
				combo.removeChangeListener(this);
				combo.setSelectedItem(currentSeletion);
				combo.addChangeListener(this);
			}
		}

    Dataset dataset = getSelectedDataset();
		if (update 
        && combo.getText()!=null 
        && dataset!=null  ) {
        
			// update the nodes label      
			setNodeLabel(getName(), combo.getText());

			// set new value on query
			query.setStarBases(dataset.getStarBases());
			query.setPrimaryKeys(dataset.getPrimaryKeys());
		}

		currentSeletion = combo.getSelectedItem();

	}

  
  public Dataset getSelectedDataset() {
    Dataset selected = null;
    String selection = combo.getText();
    if ( selection!="" )  
      selected  = ((DatasetWrapper) combo.getSelectedItem()).dataset;
    return selected;
  }
   
  private LabelledComboBox combo;


}
