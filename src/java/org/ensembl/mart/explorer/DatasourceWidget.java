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
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.util.LoggingUtil;

/**
 * Widget containing a list of possible datasources the user can choose from.
 * Choosing a datasource will cause it to be set on the query.
 */
public class DatasourceWidget extends InputPage implements ActionListener {

	private Object selectedItem = null;

	private Vector datasourcesCopy = new Vector();

	private String noneItem = "None";

	private static Logger logger =
		Logger.getLogger(DatasourceWidget.class.getName());

	private List datasources;
	private JComboBox combo;

	/**
	 * @param query listens to changes in query.datasource, updates widget in response
	 * @param datasources list of available datasources. A reference to this list
	 * is kept so that the widget is always up to date.
	 */
	public DatasourceWidget(Query query, List datasources) {

		super(query);

		this.datasources = datasources;

		combo = new JComboBox();
		combo.addActionListener(this);

    initialiseDropDownList(datasources);
    initialiseDropDownList(datasources);
    initialiseDropDownList(datasources);

		DataSource ds = query.getDataSource();
		if (ds != null) {
			// TODO handle case where dsv not in list already
			combo.setSelectedItem(ds.toString());
		}

		Box b = Box.createHorizontalBox();
		b.add(new JLabel("Datasource"));
		b.add(combo);
		add(b, BorderLayout.NORTH);
	}

	/**
   * TODO only update the state of the drop down list if the datasources
   * has changed; if so change the options and reset the same selected
   * item if it is available
	 * @param datasources
	 */
	private void initialiseDropDownList(List datasources) {

    // TODO try to get vector.equals() to work.

//    Vector copy = new Vector(datasources);
//    System.out.println( ""+datasourcesCopy.equals( copy ));
//    boolean same = ;
//    for (int i = 0, n = datasources.size(); i < n; i++) {
//			System.out.println( datasources.get(i).toString() + ", " + copy.get(i).toString());
//			if ( datasources.get(i).equals(copy.get(i) )) same = false;
//		}
//
//    if ( datasourcesCopy.equals( new Vector(datasources) ))
//      return;

    logger.info("reloading model");
      
    datasourcesCopy = new Vector( datasources );

    // Reload drop down with list strings from each element in 
    // datasources plus a "none" option.
    Vector tmp = new Vector();
    for (int i = 0; i < datasources.size(); i++)
      tmp.add(datasources.get(i).toString());
    Collections.sort(tmp);
    tmp.insertElementAt(noneItem, 0);

		combo.setModel(new DefaultComboBoxModel(tmp));

	}

	/**
	 * 
	 */
	private void refresh() {

		//combo.removeAllItems();
		//combo.setMoListData( datasourcesCopy );
		//    for (int i = 0, n = datasources.size(); i < n; i++) 
		//      combo.addItem( datasources.get(i) );
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

		List dss = QueryEditor.testDatasources();
		Query q = new Query();
		DatasourceWidget dw = new DatasourceWidget(q, dss);

		JFrame f = new JFrame("Datasource Widget Editor (Test Frame)");
		Box p = Box.createVerticalBox();
		p.add(dw);
		f.getContentPane().add(p);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(450, 150);
		f.setVisible(true);

	}

	//  /**
	//   * Set or unset query.datasource if the selected item has changed.
	//   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	//   */
	//	public void valueChanged(ListSelectionEvent e) {
	//		
	//    logger.info("");
	//    
	//    Object item = combo.getSelectedValue(); 
	//    if ( item==selectedItem ) return;
	//    
	//    if ( item==noneItem )
	//      query.setDataSource(null);
	//    else
	//      query.setDataSource( (DataSource)item );
	//    
	//    selectedItem = item; 		
	//	}

	/**
	 * Set or unset query.datasource if the selected item has changed.
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */

	public void actionPerformed(ActionEvent e) {
		logger.info("");
		//    
		//    Object item = combo.getSelectedItem(); 
		//    if ( item==selectedItem ) return;
		//    
		//    if ( item==noneItem )
		//      query.setDataSource(null);
		//    else
		//      query.setDataSource( (DataSource)item );
		//    
		//    selectedItem = item;
	}
}
