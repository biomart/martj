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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.util.LoggingUtil;

/**
 * Widget representing currently available datasource.dataset options.
 * Once user selects a datasource.dataset the default datasource.dataset.datasetView
 * is selected.
 * 
 * TODO upgrade to drop down tree
 */
public class DatasetViewWidget
	extends InputPage
	implements ActionListener {

	private InputPageContainer container;

	private DatasetView[] oldViews;

	private Map optionToView = new HashMap();

	private AdaptorManager adaptorManager;

	private static final Logger logger =
		Logger.getLogger(DatasetViewWidget.class.getName());

	private Feedback feedback = new Feedback(this);

	private DatasetViewTree chooser;

	private String noneOption = "None";

	private DatasetView lastDSV;


	/**
	 * @param query underlying model for this widget.
	 */
	public DatasetViewWidget(
		Query query,
		AdaptorManager adaptorManager,
    InputPageContainer container) {

		super(query, "Dataset View");

		this.adaptorManager = adaptorManager;
    this.container = container;
    this.chooser = new DatasetViewTree(adaptorManager);
    
		chooser.addActionListener(this);
		add(chooser, BorderLayout.NORTH);
	}

	/**
	 * Runs a test; an instance of this class is shown in a Frame.
	 */
	public static void main(String[] args) throws Exception {

		LoggingUtil.setAllRootHandlerLevelsToFinest();
		logger.setLevel(Level.FINE);
		//Logger.getLogger(Query.class.getName()).setLevel( Level.FINE );

    AdaptorManager am = QueryEditor.testDatasetViewSettings();
    am.setAdvancedOptionsEnabled( true );
		Query q = new Query();
		DatasetViewWidget dvm =
			new DatasetViewWidget(q, am, null);
		dvm.setSize(950, 750);

		JFrame f = new JFrame(dvm.getClass().getName() + " - test");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add(dvm);
		f.pack();
		f.setVisible(true);

	}

	/**
	 * Responds to a change in dataset view on the query. Updates the state of
	 * this widget by changing the currently selected item in the list.
	 */
	public void datasetViewChanged(
		Query query,
		DatasetView oldDatasetView,
		DatasetView newDatasetView) {

		if (newDatasetView != null
			&& !adaptorManager.contains(newDatasetView)) 
			try {
				adaptorManager.add(newDatasetView.getAdaptor());
			} catch (ConfigurationException e) {
				feedback.warning(e);
			}

		if (newDatasetView != null) {
			chooser.setSelectedUserObject(newDatasetView);
			// set these to default values
			query.setPrimaryKeys(newDatasetView.getPrimaryKeys());
			query.setStarBases(newDatasetView.getStarBases());
		} else {
			chooser.setSelectedUserObject(null);
		}

	}


	/**
	 * Handles user selection of an adaptor->dataset by setting the datasetView,
   * and datasource if vailable.
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
  public void actionPerformed(ActionEvent e) {

    DatasetView dsv = (DatasetView) chooser.getSelectedUserObject();
    
    if ( dsv==null || dsv == lastDSV) return;
		lastDSV = dsv;
    
		query.clear();
		query.setDatasetView(dsv);

		if (dsv != null) {

			query.setPrimaryKeys(dsv.getPrimaryKeys());
			query.setStarBases(dsv.getStarBases());
			query.setDataset(dsv.getDataset());
      
      if ( dsv.getDSViewAdaptor()!=null && dsv.getDSViewAdaptor().getDataSource()!=null )
        query.setDataSource( dsv.getDSViewAdaptor().getDataSource() );
		}

    container.toFront( TreeNodeData.ATTRIBUTES );

	}

	/**
	 * 
	 */
	public void openDatasetViewMenu() {
		chooser.showTree();
	}


}
