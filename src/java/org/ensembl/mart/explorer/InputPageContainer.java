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

import java.awt.CardLayout;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * Contains all the input pages.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * 
 */
public class InputPageContainer
  extends JPanel
  implements TreeSelectionListener {

  private final static Logger logger =
    Logger.getLogger(InputPageContainer.class.getName());

  private DatasetView datasetView;
  private CardLayout cardLayout = new CardLayout();

  public InputPageContainer(Query query, DSViewAdaptor datasetViewAdaptor) {
    super();
    setLayout(cardLayout);
    add(new DatasetWidget(query, datasetViewAdaptor), TreeNodeData.DATASET_VIEW.getLabel());
    add(new DatasourceWidget(query), TreeNodeData.DATASOURCE.getLabel());
    add(new AttributesWidget(query, datasetViewAdaptor), TreeNodeData.ATTRIBUTES.getLabel());
    add(new FiltersWidget(query, datasetViewAdaptor), TreeNodeData.FILTERS.getLabel());
    add(new OutputSettingsPage(), TreeNodeData.FORMAT.getLabel());
  }

  public void setDatasetView(DatasetView datasetView) {
    removeAllPages();
    this.datasetView = datasetView;
    addDefaultPages();
    addPages(datasetView);

  }

  /**
   * TODO add all input pages for the specified datasetView.
   * @param datasetView
   */
  private void addPages(DatasetView datasetView) {

  }

  /**
   * TODO Add default input pages: dataset, db/mart_target
   */
  private void addDefaultPages() {

    //  outputSettingsPage = new OutputSettingsPage();
    //  outputSettingsPage.addPropertyChangeListener(this);
    //  addPage(outputSettingsPage);

  }

  /**
   * TODO Remove all input pages.
   */
  private void removeAllPages() {

  }

  /**
   * 
   */
  private void addFilterPages(DatasetView dataset) {
    //    filtersPage = new FilterPageSetWidget(query, dataset);
    //    addPage(filtersPage);
  }

  /**
   * Creates the attribute pages and various maps that are useful 
   * for relating nodes, pages and attributes.
   */
  private void addAttributePages(DatasetView dataset) {

    //    attributesPage = new AttributePageSetWidget(query, dataset);

    //    List list = attributesPage.getLeafWidgets();
    //    AttributeDescriptionWidget[] attributePages = (AttributeDescriptionWidget[]) list.toArray(new AttributeDescriptionWidget[list.size()]);
    //    for (int i = 0; i < attributePages.length; i++) {
    //      AttributeDescriptionWidget w = attributePages[i];
    //      Attribute a = w.getAttribute();
    //      attributeFieldNameToPage.put( a.getField(), w );
    //      attributeToWidget.put( a, w );
    //    }

    //    addPage(attributesPage);

  }

  /**
   * TODO Show input page corresponding to selected tree node. 
   */
  public void valueChanged(TreeSelectionEvent e) {

    logger.info(e.toString());

    if (e.getNewLeadSelectionPath() != null
      && e.getNewLeadSelectionPath().getLastPathComponent() != null) {

      DefaultMutableTreeNode node =
        (DefaultMutableTreeNode) e
          .getNewLeadSelectionPath()
          .getLastPathComponent();

      if (node != null) {

        TreeNodeData tnd = (TreeNodeData) node.getUserObject();

        if (tnd.getAttribute() != null)
          toFront(tnd.getAttribute());
        else if (tnd.getFilter() != null)
          toFront(tnd.getFilter());
        else
          toFront(tnd);
      }
    }
  }

  /**
   * @param tnd
   */
  private void toFront(TreeNodeData tnd) {
    // TODO Auto-generated method stub
    cardLayout.show(this, tnd.getLabel());
  }

  /**
   * @param filter
   */
  private void toFront(Filter filter) {
    // TODO Auto-generated method stub

  }

  /**
   * @param attribute
   */
  private void toFront(Attribute attribute) {
    // TODO Auto-generated method stub

  }

}
