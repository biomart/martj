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

import java.util.logging.Logger;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryAdaptor;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * Represents the currently selected adaptot.dataset.viewname view and enables
 * the user to select another from the current adaptor.dataset.
 * 
 * NOTE viewname = datasetview.internalName
 * TODO add datasetview.datasetDisplayName and datasetview.viewDisplayName
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatasetViewChooser extends LabelledComboBox {

  private Query query;

  private final static Logger logger =
    Logger.getLogger(DatasetViewChooser.class.getName());

  private Feedback feedback = new Feedback(this);

  /**
   * @param label
   */
  public DatasetViewChooser(Query query) {
    super("DatasetView");

    this.query = query;

    //  set up handler to listen to change in user selection in widget
    addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {

        updateQuery();
      }
    });

    // set up handler to listen to change in query
    query.addQueryChangeListener(new QueryAdaptor() {
      public void datasetViewChanged(
        Query query,
        DatasetView oldDatasetView,
        DatasetView newDatasetView) {

        updateWidget(newDatasetView);
      }
    });
  }

  /**
   * Updates available options and currently selected item in response
   * to a change in the query.datasetView value.
   * @param datasetView
   */
  private void updateWidget(DatasetView datasetView) {

    if (datasetView == null) {

      removeAllItems();

    } else {

      // we use the string dataset.internalName as the option that represents
      // the datasetview in the list of options.
      removeAllItems();
      try {
        DSViewAdaptor a = datasetView.getAdaptor();
        DatasetView[] views =
          a.getDatasetViewByDataset(datasetView.getDataset());
        for (int i = 0; i < views.length; i++) {
          DatasetView view = views[i];
          addItem(view.getInternalName());
        }
        setSelectedItem(datasetView.getInternalName());
      } catch (ConfigurationException e) {
        feedback.warning(e);
      }

    }
  }

  /**
   * Called in response to a change in the options in the combo, e.g. user selecting a new item.
   * 
   * Changes query.datasetView to correspond to the the users selection.
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  private void updateQuery() {

    DatasetView view = query.getDatasetView();

    if (view == null
      || (view != null && view.getInternalName().equals(getSelectedItem())))
      return;

    try {

      view =
        view.getAdaptor().getDatasetViewByDatasetInternalName(
          view.getDataset(),
          (String) getSelectedItem());
      query.setDatasetView(view);

    } catch (ConfigurationException e) {
      feedback.warning("Problem loading new dataset view: ", e);
    }

  }

}
