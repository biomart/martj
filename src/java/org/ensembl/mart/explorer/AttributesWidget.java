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

import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class AttributesWidget extends InputPage {

  private JTabbedPane tabbedPane = new JTabbedPane();
  private JLabel unavailableLabel =
    new JLabel("Unavailable. Choose DatasetView first.");
  
  
  /**
   * Displays the attributes grouped according to query.datasetView.
   * If none are available if displays a message to that effect. 
   * @param query
   */
  public AttributesWidget(Query query, DSViewAdaptor datasetViewAdaptor, QueryTreeView tree) {
    super(query, null, tree);
    clearAttributes();
    
  }

  private void clearAttributes() {
    remove(tabbedPane);
    add(unavailableLabel);
    validate();
  }

  /**
   * Loads attributes from datasetView when a new datasetView is set on
   * the query.
   * @see org.ensembl.mart.lib.QueryChangeListener#datasetViewChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.config.DatasetView, org.ensembl.mart.lib.config.DatasetView)
   */
  public void datasetViewChanged(
    Query query,
    DatasetView oldDatasetView,
    DatasetView newDatasetView) {

    if (newDatasetView == null) {
      clearAttributes();
    } else {
      remove( unavailableLabel );
      tabbedPane.removeAll();
      AttributePage[] aps = newDatasetView.getAttributePages();
      for (int i = 0; i < aps.length; i++)
        tabbedPane.add(
          new AttributePageWidget(query, aps[i].getDisplayName(), aps[i], tree));
      add(tabbedPane);
      validate();
    }
  }

}
