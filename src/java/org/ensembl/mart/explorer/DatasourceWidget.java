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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.util.LoggingUtil;

/**
 * Widget shows the currently selected datasource and enables the 
 * user to change it.
 */
public class DatasourceWidget extends InputPage {

  private static int MAX_CONNECTION_POOL_SIZE = 10;


  private MartSettings martManager;
  private JTextField martName = new JTextField(30);
  private String none = "None";
  private static Logger logger =
    Logger.getLogger(DatasourceWidget.class.getName());

  /**
   * @param query listens to changes in query.datasource, updates widget in response
   * @param datasources list of available datasources. A reference to this list
   * is kept so that the widget is always up to date.
   */
  public DatasourceWidget(Query query, MartSettings martManager) {

    super(query);

    this.martManager = martManager;
    martName.setEditable(false);
    setDatasource(null);

    JButton cb = new JButton("Change");
    cb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doChange();

      }
    });

    Box b = Box.createHorizontalBox();
    b.add(new JLabel("Datasource "));
    b.add(cb);
    b.add(martName);
    add(b, BorderLayout.NORTH);
  }

  /**
   * Opens MartSettings dialog and if the user selects a new datasource
   * that is set on the query.
   */
  public void doChange() {
    
    martManager.setSelected(query.getDataSource());
      
    if (martManager.showDialog(this)) {

      setDatasource(martManager.getSelected());
      query.setDataSource( martManager.getSelected() );
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

    MartSettings mm = QueryEditor.testDatasetViewSettings().getMartSettings();
    Query q = new Query();
    DatasourceWidget dw = new DatasourceWidget(q, mm);

    JFrame f = new JFrame("Datasource Widget Editor (Test Frame)");
    Box p = Box.createVerticalBox();
    p.add(dw);
    f.getContentPane().add(p);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setSize(450, 150);
    f.setVisible(true);

  }

  /**
   * Listen to query.datasource changes
   * @see org.ensembl.mart.lib.QueryChangeListener#datasourceChanged(org.ensembl.mart.lib.Query, javax.sql.DataSource, javax.sql.DataSource)
   */
  public void datasourceChanged(
    Query sourceQuery,
    DataSource oldDatasource,
    DataSource newDatasource) {

    if ( newDatasource!=null && !martManager.contains( newDatasource ))
      martManager.add( newDatasource );
    setDatasource(newDatasource);
  }

  /**
   * Update the label to correspond to the datasource.
   * @param newDatasource
   */
  private void setDatasource(DataSource datasource) {
    if (datasource == null)
      martName.setText(none);
    else
      martName.setText(datasource.toString());

  }


}
