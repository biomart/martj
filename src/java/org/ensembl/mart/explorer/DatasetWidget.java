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
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryChangeListener;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * Widget representing the currently selected dataset 
 * and enabling the user to select another.
 */
public class DatasetWidget
  extends InputPage
  implements QueryChangeListener, ChangeListener {

  private Logger logger = Logger.getLogger(DatasetWidget.class.getName());

  private Feedback feedback = new Feedback(this);

  private LabelledComboBox combo = new LabelledComboBox("Dataset ", this);

  private JButton defaultButton = new JButton("Reset from dataset view");

  /**
   * @param query underlying model for this widget.
   */
  public DatasetWidget(Query query) {

    super(query, "Dataset ");

    defaultButton.setEnabled(query.getDatasetView() != null);
    defaultButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doLoadDefaultDatasetName();
      }
    });

    Box b = Box.createVerticalBox();
    b.add(combo, BorderLayout.NORTH);
    b.add(defaultButton);
    b.add(Box.createVerticalGlue());
    add(b);
  }


  private void doLoadDefaultDatasetName() {
    DatasetView dsv = query.getDatasetView(); 
    if ( dsv!=null )
      combo.setSelectedItem( dsv.getDataset() );
  }


  /**
   * Runs a test; an instance of this class is shown in a Frame.
   */
  public static void main(String[] args) throws Exception {
    Query q = new Query();
    DatasetWidget dw = new DatasetWidget(q);
    dw.setSize(950, 750);

    JFrame f = new JFrame(dw.getClass().getName() + " - test");
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.getContentPane().add(dw);
    f.pack();
    f.setVisible(true);

  }

  /**
   * Responds to a change in query.dataset. Updates the state of
   * this widget by changing the label.
   */
  public void datasetChanged(
    Query query,
    String oldDataset,
    String newDataset) {

    combo.setSelectedItem(newDataset);
  }

  /**
   * Responds to user changing the selected dataset by updating
   * query.dataset.
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void stateChanged(ChangeEvent e) {

    if (combo.getSelectedItem() != query.getDataset())
      query.setDataset((String) combo.getSelectedItem());
  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryChangeListener#datasetViewChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.config.DatasetView, org.ensembl.mart.lib.config.DatasetView)
   */
  public void datasetViewChanged(
    Query query,
    DatasetView oldDatasetView,
    DatasetView newDatasetView) {

    defaultButton.setEnabled(newDatasetView != null );
  }

}
