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

package org.ensembl.mart.vieweditor;

import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.ensembl.mart.explorer.QueryEditor;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;

/**
 * Tree representation of a DatasetView object. 
 * 
 * <p>This widget implements the MVC design pattern.
 * It's datasetView attribute is the model and can be changed.
 * The widget provides a tree 
 * view representation of the model (not implemented) and offers these user control actions:
 * </p>
 * 
 * <ul>
 * <li> Select a node. (not implemented) </li>
 * <li> Change the order of some nodes. (not implemented) </li>
 * <li> Add a node, context sensitive. (not implemented) </li>
 * <li> Delete a node. (not implemented) </li>
 * </ul>
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see org.ensembl.mart.config.DatasetView
 */
public class DatasetViewTreeWidget extends JPanel {
  
  // NOTE see org.ensembl.mart.explorer.QueryEditor for examples of using JTree
  
  // TODO load tree when datasetView set
  // TODO check that resizing the widget works (manually resize via dragging "corners")
  // TODO remove tree when setDatasetView(null), need to make sure we can add and remove at will
  // TODO add DatasetView.setSelected(XXXX), this should propagate a change event. Katerina/Craig/Darin 
  // TODO Selecting a node in tree calls datasetView.setSelected(XXXX). 
  // TODO Support reordering nodes of the same type e.g.change the order of attributes. Preferably by drag and drop mechanism
  // TODO Support deleting node by "delete" key, undo?
  // TODO Support deleting node by "right click | delete"
  // TODO Support adding a node (context sensitive) by "right click | add XXXX"

	private DatasetView datasetView = null;

	public DatasetViewTreeWidget() {
	}

	/**
	 * Test purposes only. Creates a frame with a JTree containing
	 * a presepecified DatasetView.dtd compatible configuration file.
	 * @param args
	 * @throws ConfigurationException
	 */
	public static void main(String[] args) throws ConfigurationException {

		String file = "data/XML/homo_sapiens__ensembl_genes.xml";
		URL url = QueryEditor.class.getClassLoader().getResource(file);
		DSViewAdaptor adaptor = new URLDSViewAdaptor(url, true);
		// only view one in the file so get that one
		DatasetView view = adaptor.getDatasetViews()[0];

		DatasetViewTreeWidget w = new DatasetViewTreeWidget();
		w.setDatasetView(view);

		JFrame f = new JFrame("DatasetView Tree Widget");
		f.getContentPane().add( w );
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(950, 750);
		f.setVisible(true);
	}

	/**
	 * @return
	 */
	public DatasetView getDatasetView() {
		return datasetView;
	}

	/**
	 * @param view
	 */
	public void setDatasetView(DatasetView view) {
    clearDatasetView();
		datasetView = view;
    loadDatasetView();
	}

	/**
	 * Loads the datasetView by creating a tree to represent it
   * and displaying it.
	 */
	private void loadDatasetView() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Removes current dataset view if one is loaded, otherwise does nothing.
	 */
	private void clearDatasetView() {
		// TODO Auto-generated method stub
		
	}

}
