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

import javax.swing.JPanel;
import javax.swing.event.TreeSelectionEvent;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * Contains all the input pages.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * 
 */
public class InputPageContainer extends JPanel {


  

	private DatasetView datasetView;


  public InputPageContainer(Query query) {
		super();
    setLayout( new CardLayout() );    
    addPage(new DatasetWidget(query));
	}


  /**
   * Adds page to input panel and tree view.
   * @param page page to be added
   */
  public void addPage(InputPage page) {
    add(page.getName(), page);
  }


  public void setDatasetView(DatasetView datasetView) {
    removeAllPages();
    this.datasetView = datasetView;
    addDefaultPages();
    addPages( datasetView );
    
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

//    if (e.getNewLeadSelectionPath() != null
//      && e.getNewLeadSelectionPath().getLastPathComponent() != null) {
//
//      DefaultMutableTreeNode node =
//        (DefaultMutableTreeNode) e
//          .getNewLeadSelectionPath()
//          .getLastPathComponent();
//
//      if (node.getUserObject() instanceof InputPage) {
//
//        InputPage page = (InputPage) node.getUserObject();
//        showInputPage(page);
//
//      }
//    }
  }




}
