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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DSViewAdaptor;

/**
 * Tree view showing the current state of the query.
 * 
 * <p>TODO extract QueryChangeListener interface
 * <p>TODO extract propertyChange to a base class shared by this and QuerySettingsContainer
  * <p>TODO add,remove support
 * <p>TODO support deleting nodes
 * <p>TODO support dnd reordering of attribute nodes
 */
public class QueryTreeView extends JPanel implements PropertyChangeListener {

  private DefaultTreeModel treeModel;

  private DefaultMutableTreeNode rootNode;

  private final static Logger logger =
    Logger.getLogger(QueryTreeView.class.getName());

  private DSViewAdaptor dsvAdaptor;

  private Query query;

  private JTree tree;

  /**
   * Tree view showing the current state of the query. The current datasetView
   * is retrieved from the adaptor and this is used to determine how to render
   * the values stored in the query.
   * 
   * @param query Query represented by tree.
   * @param dsvAdaptor source of DatasetViews used to interpret query.
   */
  public QueryTreeView(Query query, DSViewAdaptor dsvAdaptor) {
    super();
    this.query = query;
    this.dsvAdaptor = dsvAdaptor;
    query.addPropertyChangeListener(this);
  }

  /*
   *  <p>TODO main() test case 
   */
  public static void main(String[] args) {
  }

  public void addTreeSelectionListener(TreeSelectionListener tsl) {
    tree.addTreeSelectionListener(tsl);
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    String propertyName = evt.getPropertyName();
    Object newValue = evt.getNewValue();
    Object oldValue = evt.getOldValue();

    if (evt.getSource() == query) {

      logger.fine("propertyName=" + propertyName);
      logger.fine("oldValue=" + oldValue);
      logger.fine("newValue=" + newValue);

      if ("datasetInternalName".equals(propertyName)) {

        changedDatasetInternalName((String) newValue);

      } else if ("dataSource".equals(propertyName)) {

        changedDatasetInternalName(newValue);

      } else if ("attribute".equals(propertyName)) {

        if (newValue != null && oldValue == null)
          addAttribute((Attribute) newValue);

        else if (newValue == null && oldValue != null)
          removeAttribute((Attribute) oldValue);

      } else if ("filter".equals(propertyName)) {

        if (newValue != null && oldValue == null)
          addFilter((Filter) newValue);
        else if (newValue == null && oldValue != null)
          removeFilter((Filter) oldValue);

      }

      Enumeration enum = rootNode.breadthFirstEnumeration();
      while (enum.hasMoreElements())
        treeModel.nodeChanged((TreeNode) enum.nextElement());

    }
  }

  /**
   * @param filter
   */
  public void removeFilter(Filter filter) {
    // TODO Auto-generated method stub
    //  treeModel.removeNodeFromParent(
    //    ((InputPageAware) oldValue).getInputPage().getNode());

  }

  /**
   * @param filter
   */
  public void addFilter(Filter filter) {
    // TODO Auto-generated method stub
    //  {
    //    insertNode(
    //      filtersPage.getNode(),
    //      ((InputPageAware) newValue).getInputPage().getNode());
    //  } 

  }

  /**
   * @param attribute
   */
  public void removeAttribute(Attribute attribute) {
    // TODO Auto-generated method stub
    //  {
    //    treeModel.removeNodeFromParent(
    //      ((InputPageAware) oldValue).getInputPage().getNode());
    //  }
  }

  /**
   * @param attribute
   */
  public void addAttribute(Attribute attribute) {
    // TODO Auto-generated method stub
    //  insertNode(
    //    attributesPage.getNode(),
    //    ((InputPageAware) newValue).getInputPage().getNode());
  }

  /**
   * @param newValue
   */
  public void changedDatasetInternalName(Object newValue) {
    // TODO Auto-generated method stub

  }

  /**
   * @param string
   */
  public void changedDatasetInternalName(String string) {
    // TODO Auto-generated method stub

  }

}
