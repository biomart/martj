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
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryChangeListener;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * Widget representing the currently selected dataset view
 * and enabling the user to select another.
 */
public class DatasetViewWidget
  extends InputPage
  implements QueryChangeListener {

  private DatasetViewSettings datasetViewSettings;

  private Logger logger = Logger.getLogger(DatasetViewWidget.class.getName());

  private Feedback feedback = new Feedback(this);

  private JTextField datasetViewName = new JTextField(30);
  private JButton button = new JButton("change");

  /**
   * @param query underlying model for this widget.
   */
  public DatasetViewWidget(
    Query query,
    DatasetViewSettings datasetViewSettings) {

    super(query, "Dataset View");

    this.datasetViewSettings = datasetViewSettings;
    datasetViewName.setEditable(false);
    setDatasetView(null);

    JButton cb = new JButton("Change");
    cb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doChange();

      }
    });

    Box b = Box.createHorizontalBox();
    b.add(new JLabel("DatasetView "));
    b.add(cb);
    b.add(datasetViewName);
    add(b, BorderLayout.NORTH);

  }

  /**
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
        feedback.warn(e);
      }
    setDatasetView(newDatasetView);
  }

  /**
   * Update the label to show which dataset view is currently selected.
   * @param object
   */
  private void setDatasetView(DatasetView datasetView) {
    String s = "";
    if (datasetView != null)
      s = datasetView.getDisplayName();
    datasetViewName.setText(s);
  }

}
