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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.util.LoggingUtil;

/**
 * Widget shows the currently selected datasource and enables the 
 * user to change it.
 */
public class DatasourceWidget extends InputPage implements ChangeListener {

  private Feedback feedback = new Feedback(this);

  private static Logger logger =
    Logger.getLogger(DatasourceWidget.class.getName());

  private AdaptorManager adaptorManager;
  private LabelledComboBox chooser = new LabelledComboBox("DataSource");
  private String none = "None";

  /**
   * @param query listens to changes in query.datasource, updates widget in response
   * @param datasources list of available datasources. A reference to this list
   * is kept so that the widget is always up to date.
   */
  public DatasourceWidget(Query query, AdaptorManager adaptorManager) {

    super(query);

    this.adaptorManager = adaptorManager;
    createUI();
    updateChooser();
    chooser.addChangeListener(this);
  }

  private void createUI() {

    chooser.setEditable(false);
    add(chooser, BorderLayout.NORTH);
  }


  private void updateChooser() {
    
    List items = new ArrayList();
    
    try {
      DSViewAdaptor as[] = adaptorManager.getRootAdaptor().getAdaptors();
      for (int i = 0; i < as.length; i++) {
        DSViewAdaptor a = as[i];
        if ( a.getDataSource()!=null ) {
          items.add(a.getName());
          logger.warning( "Adding adaptor to chooser : "+a.getName() );
        }
      }
    } catch (ConfigurationException e) {
      feedback.warning(e);
    }

    Collections.sort( items );
    items.add(0, none);
    chooser.removeAllItems();
    chooser.addAll( items );

  }

  /**
   * Opens MartSettings dialog and if the user selects a new datasource
   * that is set on the query.
   */
  public void doChange() {

  }

  /**
   * Update UI in respose to change in query.datasource.
   * @see org.ensembl.mart.lib.QueryChangeListener#datasourceChanged(org.ensembl.mart.lib.Query, javax.sql.DataSource, javax.sql.DataSource)
   */
  public void datasourceChanged(
    Query sourceQuery,
    DataSource oldDatasource,
    DataSource newDatasource) {

    DetailedDataSource ds = (DetailedDataSource) newDatasource;

    if (ds == null) {
      chooser.setSelectedItem(none);
    } else {
      String item = ds.getDisplayName();
      if (!chooser.hasItem(item))
        chooser.addItem(item);
      chooser.setSelectedItem(item);
    }
  }

  /**
   * Test purposes only; shows widget in  at est frame.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    // enable logging messages
    LoggingUtil.setAllRootHandlerLevelsToFinest();
    logger.setLevel(Level.FINEST);
    Logger.getLogger(Query.class.getName()).setLevel(Level.FINEST);

    AdaptorManager am = QueryEditor.testDatasetViewSettings();
    Query q = new Query();
    DatasourceWidget dw = new DatasourceWidget(q, am);

    JFrame f = new JFrame("Datasource Widget Editor (Test Frame)");
    Box p = Box.createVerticalBox();
    p.add(dw);
    f.getContentPane().add(p);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setSize(450, 150);
    f.setVisible(true);

  }

  /**
   * Updates query in response to user selecting a different datasource.
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void stateChanged(ChangeEvent e) {
    String selected = (String) chooser.getSelectedItem();

    logger.warning("selected  = " + selected);

    DataSource ds = null;
    if (selected !=null && selected!=none)
      try {
        ds =
          adaptorManager
            .getRootAdaptor()
            .getAdaptorByName(selected)
            .getDataSource();
      } catch (ConfigurationException e1) {
        feedback.warning(e1);
      }
    query.setDataSource(ds);

  }

}
