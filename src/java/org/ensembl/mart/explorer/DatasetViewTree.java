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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JFrame;

import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.util.LoggingUtil;

/**
 * Widget showing available dataset views represented as a menu tree. The user can select one of
 * these. 
 * 
 * <p>The first tier of the tree contains
 * adaptors, the second the datasets, and the optional third tier the internalNames. 
 * If adaptorManager.isOptionalDatasetViewsEnabled()==true then the optional 
 * third tier is included. Otherwise only dataset views with internalName=="default"
 * are displayed and they are shown as adaptor -> dataset. In this case the internalName
 * is not shown.
 * </p>
 */
public class DatasetViewTree extends PopUpTreeCombo {

	private final static Logger logger =
		Logger.getLogger(DatasetViewTree.class.getName());

	private Feedback feedback = new Feedback(this);

	private AdaptorManager manager;

	public DatasetViewTree(AdaptorManager manager) {
		super("DatasetView");
		this.manager = manager;
	}

	/**
	 * Update the tree's rootNode to reflect the currently available datasetViews.
	 * Structure: adaptor -> dataset [ -> internalName ]
	 * @see org.ensembl.mart.explorer.PopUpTreeCombo#update()
	 */
	public void update() {

		boolean optional = manager.isAdvancedOptionsEnabled();
		rootNode.removeAllChildren();
		logger.fine("optional=" + optional);

		try {
			DSViewAdaptor[] adaptors = manager.getRootAdaptor().getAdaptors();
			// TODO sort adaptors by name

			for (int i = 0; i < adaptors.length; i++) {

				DSViewAdaptor adaptor = adaptors[i];

				// Skip composite adaptors
				if (adaptor.getAdaptors().length > 0)
					continue;

				// skip adaptors which lack a "default" view
				// if we are only showing default views.
				if (!optional && !containsDefaultView(adaptor))
					continue;
        
        if (adaptor.getDatasetViews().length==0 )
          continue;

				LabelledTreeNode adaptorNode =
					new LabelledTreeNode(adaptor.getName(), null);

				rootNode.add(adaptorNode);

				try {

					String[] datasetNames = adaptor.getDatasetNames();
					Arrays.sort(datasetNames);

					for (int j = 0; j < datasetNames.length; j++) {

						String dataset = datasetNames[j];
						DatasetView[] views = adaptor.getDatasetViewsByDataset(dataset);

						LabelledTreeNode datasetNode = null;

						for (int k = 0; k < views.length; k++) {

							DatasetView view = views[k];

							if (optional) {

								if (datasetNode == null) {
									datasetNode = new LabelledTreeNode(dataset, null);

									if (datasetNode != null)
										adaptorNode.add(datasetNode);
								}

								// adaptor -> dataset -> internalName
								datasetNode.add(
									new LabelledTreeNode(view.getInternalName(), view));

							} else {
								// adaptor -> dataset (using default datasetview only)
								if (isDefault(view)) {
									adaptorNode.add(
										new LabelledTreeNode(view.getDataset(), view));
									break;
								}
							}
						}
					}
				} catch (ConfigurationException e) {
					// do this try ... catch so that a problem with one adaptor won't prevent dataset views from
					// others being loaded 
					feedback.warning(e);
				}

			}
		} catch (ConfigurationException e) {

			feedback.warning(e);
		}
	}

	public boolean isDefault(DatasetView view) {
		return "default".equals(view.getInternalName().toLowerCase());
	}

	/**
	 * @param adaptor
	 * @return
	 */
	private boolean containsDefaultView(DSViewAdaptor adaptor) {
		boolean r = false;
		try {
			DatasetView[] views = adaptor.getDatasetViews();
			for (int i = 0; !r && i < views.length; i++)
				if (isDefault(views[i]))
					r = true;

		} catch (ConfigurationException e) {
			feedback.warning(e);
		}
		return r;
	}

	public static void main(String[] args) {

		LoggingUtil.setAllRootHandlerLevelsToFinest();
		logger.setLevel(Level.FINE);

		AdaptorManager am = new AdaptorManager();
		//QueryEditor.testDatasetViewSettings();
		am.setAdvancedOptionsEnabled(true);

		final DatasetViewTree pu = new DatasetViewTree(am);
		// test the listener support
		pu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Selection changed to : " + pu.getSelectedLabel());
			}
		});
		Box p = Box.createVerticalBox();
		p.add(pu);
		JFrame f = new JFrame(DatasetViewTree.class.getName() + " (Test Frame)");
		f.getContentPane().add(p);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//f.setSize(250, 100);
		f.pack();
		f.setVisible(true);
	}

	/**
   * Sets the selected node to the node where node.userObject==datasetView
	 * @param newDatasetView
	 */
	public void setSelectedUserObject(DatasetView datasetView) {
		Enumeration enum = rootNode.breadthFirstEnumeration();

		while (enum.hasMoreElements()) {
			LabelledTreeNode next = (LabelledTreeNode) enum.nextElement();
      if ( next.getUserObject()==datasetView) {
        setSelected(next);
        break;
        } 
		}
	}

  
  
}
