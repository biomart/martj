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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryChangeListener;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.util.LoggingUtil;

/**
 * Widget representing currently available datasource.dataset options.
 * Once user selects a datasource.dataset the default datasource.dataset.datasetView
 * is selected.
 */
public class DatasetViewWidget
	extends InputPage
	implements QueryChangeListener, ChangeListener {

	private DatasetView[] oldViews;

	private Map optionToView = new HashMap();

	private DatasetViewSettings datasetViewSettings;

	private static final Logger logger =
		Logger.getLogger(DatasetViewWidget.class.getName());

	private Feedback feedback = new Feedback(this);

	private LabelledComboBox chooser = new LabelledComboBox("Dataset");

	private String noneOption = "None";

	//	private JTextField datasetViewName = new JTextField(30);
	//	private JButton button = new JButton("change");

	/**
	 * @param query underlying model for this widget.
	 */
	public DatasetViewWidget(
		Query query,
		DatasetViewSettings datasetViewSettings) {

		super(query, "Dataset View");

		this.datasetViewSettings = datasetViewSettings;

		chooser.setEditable(false);
		chooser.addChangeListener(this);
		add(chooser, BorderLayout.NORTH);

    initOptions();
	}

	/**
	 * Construct list of options from the available datasetviews and add them to the chooser 
	 */
	private void initOptions() {

		try {
			DatasetView[] views = datasetViewSettings.getAdaptor().getDatasetViews();

			// only update if datasetViews changed.
			if (Arrays.equals(views, oldViews))
				return;
			oldViews = views;

			// Collect all dataset views and key by adaptor->dataset
			optionToView.clear();
			for (int i = 0; i < views.length; i++) {
				DatasetView view = views[i];
				String option = toOption(view);

				// add novel options
				if (!optionToView.containsKey(option)) {
					optionToView.put(option, view);
				}

			}

			// Sort options before adding to chooser
			List options = new ArrayList();
			options.addAll(optionToView.keySet());
			Collections.sort(options);

			// Add the "none" option at the beginning.
			optionToView.put(noneOption, null);
			options.add(0, noneOption);

			chooser.removeAllItems();
			chooser.addAll(options);

		} catch (ConfigurationException e1) {
			feedback.warning(e1);
		}

	}

	/**
	 * TODO move this test to drop down dsv chooser in QueryEditor.
   * Opens DatasetViewSettings dialog.
	 */
	public void doChange() {

		DatasetView oldDsv = query.getDatasetView();

		datasetViewSettings.setSelected(oldDsv);

		DatasetView dsv = null;
		if (datasetViewSettings.showDialog(this)) {

			dsv = datasetViewSettings.getSelected();

			if (oldDsv != dsv
				&& (query.getAttributes().length > 0 || query.getFilters().length > 0)) {

				int o =
					JOptionPane.showConfirmDialog(
						this,
						new JLabel("Changing the dataset will cause the query settings to be cleared. Continue?"),
						"Delete current Change Attributes",
						JOptionPane.YES_NO_OPTION);

				// undo if user changes mind
				if (o != JOptionPane.OK_OPTION)
					return;

			}

			query.clear();
			query.setDatasetView(dsv);

			if (dsv != null) {

				query.setPrimaryKeys(dsv.getPrimaryKeys());
				query.setStarBases(dsv.getStarBases());
				query.setDataset(dsv.getDataset());
			}
		}

	}

	/**
	 * Runs a test; an instance of this class is shown in a Frame.
	 */
	public static void main(String[] args) throws Exception {

		LoggingUtil.setAllRootHandlerLevelsToFinest();
		logger.setLevel(Level.FINE);
		//Logger.getLogger(Query.class.getName()).setLevel( Level.FINE );

		Query q = new Query();
		DatasetViewWidget dvm =
			new DatasetViewWidget(q, QueryEditor.testDatasetViewSettings());
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
			&& !datasetViewSettings.contains(newDatasetView))
			try {
				datasetViewSettings.add(newDatasetView);
			} catch (ConfigurationException e) {
				feedback.warning(e);
			}

		// add dsv to datasetViewSettings if not present
		try {
			if (newDatasetView != null
				&& !datasetViewSettings.contains(newDatasetView))
				datasetViewSettings.add(newDatasetView);
		} catch (ConfigurationException e1) {
			feedback.warning(e1);
		}

		initOptions();

		if (newDatasetView != null) {
			chooser.setSelectedItem(toOption(newDatasetView));
			// set these to default values
			query.setPrimaryKeys(newDatasetView.getPrimaryKeys());
			query.setStarBases(newDatasetView.getStarBases());
		} else {
			chooser.setSelectedItem(noneOption);
		}

	}

	/**
	 * @param view DatasetView to convert to a string option name. 
	 * @return option name for the view
	 */
	private String toOption(DatasetView view) {

		DSViewAdaptor a = view.getAdaptor();
		String aName = (a != null) ? a.getDisplayName() : "Unkown";
		return aName + " -> " + view.getDataset();
	}

	/**
	 * Update datasetview if user selects a new one.
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e) {

		DatasetView dsv = (DatasetView)optionToView.get(chooser.getSelectedItem());

		query.clear();
		query.setDatasetView(dsv);

		if (dsv != null) {

			query.setPrimaryKeys(dsv.getPrimaryKeys());
			query.setStarBases(dsv.getStarBases());
			query.setDataset(dsv.getDataset());
		}

	}


}
