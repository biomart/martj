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
 
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryChangeListener;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.DSViewAdaptor;

/**
 * Tree view showing the current state of the query.
 * 
 * <p>TODO use DSV to correctly render nodes 
 * <p>TODO support dnd reordering of attribute nodes
 */
public class QueryTreeView extends JPanel implements QueryChangeListener {

  /**
   * If an attribute or filter is currently selected delete it.
   */
  private final class DeleteAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {

      TreePath path = jTree.getSelectionModel().getSelectionPath();
      if ( path==null ) return;
      
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) path.getLastPathComponent();
      TreeNode parent = child.getParent();
      int index = parent.getIndex( child );      

      if ( parent==attributesNode )
        query.removeAttribute( query.getAttributes()[index] );
      else if ( parent==filtersNode )
        query.removeFilter( query.getFilters()[index] );

    }
  }

  /**
   * Object with a toString() implementation that generates a small
   * piece of html based on the instances optional attributes.
   * This is used to create the "labels" for tree nodes.
   */
  private class NodeUserObject {

    private String label = null;
    private String separator = null;
    private String rightText = null;

    private NodeUserObject(String label, String separator, String rightText) {
      this.label = label;
      this.separator = separator;
      this.rightText = rightText;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("<html>");
      buf.append("<b>");
      if (label != null)
        buf.append(label);
      if (separator != null)
        buf.append(separator);

      buf.append("</b> ");
      if (rightText != null)
        buf.append(rightText);
      buf.append("</html>");
      return buf.toString();
    }

  }

  private DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();

  private DefaultMutableTreeNode datasetViewNode =
    new DefaultMutableTreeNode(new NodeUserObject("DatasetView", ":", null));

  private DefaultMutableTreeNode dataSourceNode =
    new DefaultMutableTreeNode(new NodeUserObject("DataSource", ":", null));

  private DefaultMutableTreeNode attributesNode =
    new DefaultMutableTreeNode(new NodeUserObject("Attributes", null, null));

  private DefaultMutableTreeNode filtersNode =
    new DefaultMutableTreeNode(new NodeUserObject("Filters", null, null));

  private DefaultMutableTreeNode formatNode =
    new DefaultMutableTreeNode(new NodeUserObject("Format", null, null));

  private DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

  private JTree jTree = new JTree(treeModel);

  private final static Logger logger =
    Logger.getLogger(QueryTreeView.class.getName());

  private DSViewAdaptor dsvAdaptor;

  private Query query;

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
    query.addQueryChangeListener(this);

    jTree.setRootVisible(false);

    rootNode.add(datasetViewNode);
    rootNode.add(dataSourceNode);
    rootNode.add(attributesNode);
    rootNode.add(filtersNode);
    rootNode.add(formatNode);

    // ensure the 1st level of nodes are visible
    TreePath path = new TreePath(rootNode).pathByAddingChild(datasetViewNode);
    jTree.makeVisible(path);

    jTree.getInputMap().put(
      KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
      "doDelete");
    jTree.getActionMap().put("doDelete", new DeleteAction());

    add(new JScrollPane(jTree) );
  }

  /**
   * Runs an interactive test program where the user can interact
   * with the QueryTreeView.
   */
  public static void main(String[] args) throws Exception {

    final Query query = new Query();
    DSViewAdaptor adaptor = QueryEditor.testDSViewAdaptor();
    final QueryTreeView qtv = new QueryTreeView(query, adaptor);
    qtv.setSize(300, 500);

    Box c = Box.createVerticalBox();

    qtv.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        if (e != null && e.getNewLeadSelectionPath() != null)
          logger.info(
            "Selected:" + e.getNewLeadSelectionPath().getLastPathComponent());
      }
    });

    //    qtv.addKeyListener(new KeyAdapter() {
    //      public void keyPressed(KeyEvent e) {
    //        if (e.getKeyCode() == KeyEvent.VK_DELETE)
    //          qtv.delete();
    //      }
    //    });

    c.add(new JLabel("Delete key should work for attributes+filters"));

    JButton b;
    Box box;

    box = Box.createHorizontalBox();
    c.add(box);
    b = new JButton("Add attribute");
    box.add(b);
    b.addActionListener(new ActionListener() {
      private int count = 0;
      public void actionPerformed(ActionEvent e) {
        int index = (int) (query.getAttributes().length * Math.random());
        Attribute a = new FieldAttribute("attribute" + count++);
        query.addAttribute(index, a);
      }
    });
    b = new JButton("Remove attribute");
    box.add(b);
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Attribute[] a = query.getAttributes();
        if (a.length > 0) {
          int index = (int) (Math.random() * a.length);
          logger.info("Removing attribute " + index + " " + a[index]);
          query.removeAttribute(a[index]);
        }
      }
    });

    // ---------
    box = Box.createHorizontalBox();
    c.add(box);
    b = new JButton("Add filter");
    box.add(b);
    b.addActionListener(new ActionListener() {
      private int count = 0;
      public void actionPerformed(ActionEvent e) {
        int index = (int) (query.getFilters().length * Math.random());
        Filter f = new BasicFilter("filterField" + count++, "=", "value");
        query.addFilter(index, f);
      }
    });
    b = new JButton("Remove random filter");
    box.add(b);
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Filter[] f = query.getFilters();
        if (f.length > 0) {
          int index = (int) (Math.random() * f.length);
          logger.info("Removing filter " + index + " " + f[index]);
          query.removeFilter(f[index]);
        }
      }
    });

    c.add(qtv);

    query.setDatasetInternalName("Some dataset");
    query.addAttribute(new FieldAttribute("ensembl_gene_id"));

    JFrame f = new JFrame("QueryTreeView unit test");
    f.getContentPane().add(c);
    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screen.width / 2 - f.getWidth() / 2;
    int y = screen.height / 2 - f.getHeight() / 2;
    f.setLocation(x, y);
    f.setVisible(true);
    f.pack();

  }

  public void addTreeSelectionListener(TreeSelectionListener tsl) {
    jTree.addTreeSelectionListener(tsl);
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryNameChanged(org.ensembl.mart.lib.Query, java.lang.String, java.lang.String)
   */
  public void queryNameChanged(
    Query sourceQuery,
    String oldName,
    String newName) {
  }

  /**
   * Update the name of the datasetview shown in the tree.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryDatasetInternalNameChanged(org.ensembl.mart.lib.Query, java.lang.String, java.lang.String)
   */
  public void queryDatasetInternalNameChanged(
    Query sourceQuery,
    String oldDatasetInternalName,
    String newDatasetInternalName) {

    ((NodeUserObject) datasetViewNode.getUserObject()).rightText =
      newDatasetInternalName;
    treeModel.reload();
  }

  /**
   * TODO Set the Datasource node with the newDatasource.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryDatasourceChanged(org.ensembl.mart.lib.Query, javax.sql.DataSource, javax.sql.DataSource)
   */
  public void queryDatasourceChanged(
    Query sourceQuery,
    DataSource oldDatasource,
    DataSource newDatasource) {

  }

  /**
   * Adds a representation of the attribute to the tree in the correct
   * position in the list of attributes.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryAttributeAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void queryAttributeAdded(
    Query sourceQuery,
    int index,
    Attribute attribute) {

    NodeUserObject userObject =
      new NodeUserObject(null, null, attribute.getField());
    DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(userObject);
    attributesNode.insert(treeNode, index);
    treeModel.reload(attributesNode);
    select(attributesNode, index, false);

  }

  /**
   * Select a node. 
   * @param treeNode
   */
  private void select(
    DefaultMutableTreeNode parentNode,
    int selectedChildIndex,
    boolean select) {

    DefaultMutableTreeNode next = parentNode;
    int nChildren = parentNode.getChildCount();
    if (nChildren > 0)
      if (selectedChildIndex < nChildren)
        next =
          (DefaultMutableTreeNode) parentNode.getChildAt(selectedChildIndex);
      else
        next = (DefaultMutableTreeNode) parentNode.getChildAt(nChildren - 1);

    TreePath path = new TreePath(next.getPath());
    jTree.scrollPathToVisible(path);
    if ( select ) jTree.setSelectionPath(path);

  }

  /**
   * Remove node from tree that corresponds to the attribute and select
   * the next attribute if available, otherwise the attributesNode.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryAttributeRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void queryAttributeRemoved(
    Query sourceQuery,
    int index,
    Attribute attribute) {

    attributesNode.remove(index);
    treeModel.reload(attributesNode);
    select(attributesNode, index, true);

  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryChangeListener#queryFilterAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void queryFilterAdded(Query sourceQuery, int index, Filter filter) {
    NodeUserObject userObject =
      new NodeUserObject(
        null,
        null,
        filter.getField() + filter.getRightHandClause());
    DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(userObject);
    filtersNode.insert(treeNode, index);
    treeModel.reload(filtersNode);
    select(filtersNode, index, false);

  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryChangeListener#queryFilterRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void queryFilterRemoved(Query sourceQuery, int index, Filter filter) {

    filtersNode.remove(index);
    treeModel.reload(filtersNode);
    select(filtersNode, index, true);

  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryChangeListener#queryFilterChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.Filter, org.ensembl.mart.lib.Filter)
   */
  public void queryFilterChanged(
    Query sourceQuery,
    Filter oldFilter,
    Filter newFilter) {
    // TODO Auto-generated method stub

  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#querySequenceDescriptionChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.SequenceDescription, org.ensembl.mart.lib.SequenceDescription)
   */
  public void querySequenceDescriptionChanged(
    Query sourceQuery,
    SequenceDescription oldSequenceDescription,
    SequenceDescription newSequenceDescription) {
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryLimitChanged(org.ensembl.mart.lib.Query, int, int)
   */
  public void queryLimitChanged(Query query, int oldLimit, int newLimit) {
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryStarBasesChanged(org.ensembl.mart.lib.Query, java.lang.String[], java.lang.String[])
   */
  public void queryStarBasesChanged(
    Query sourceQuery,
    String[] oldStarBases,
    String[] newStarBases) {
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryPrimaryKeysChanged(org.ensembl.mart.lib.Query, java.lang.String[], java.lang.String[])
   */
  public void queryPrimaryKeysChanged(
    Query sourceQuery,
    String[] oldPrimaryKeys,
    String[] newPrimaryKeys) {
  }

}
