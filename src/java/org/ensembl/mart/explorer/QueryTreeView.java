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
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
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
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.QueryListener;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.CompositeDSConfigAdaptor;
import org.ensembl.mart.lib.config.DSConfigAdaptor;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.Option;
import org.ensembl.mart.lib.config.QueryFilterSettings;

/**
 * Tree config showing the current state of the query. Allows the user
 * to select nodes and delete attributes and filters.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * 
 */
public class QueryTreeView extends JTree implements QueryListener {

  /**
   * Handles all DnD behaviour for the tree. Uses several call back
   * methods defined by the interfaces it implements to respond to user actions.
   */
  private class DnDHandler
    implements DragSourceListener, DragGestureListener, DropTargetListener {

    private JTree jTree;
    // We need to create and pass a transferable around even though we
    // don't use it so we need to create one.
    private Transferable dummyTransferable =
      new StringSelection("DUMMY DATA - NOT USED");
    private DefaultMutableTreeNode selected;
    private DragSource dragSource;

    /**
     * Initialises dnd source and target for the tree and registers
     * itself as a listener.
     */
    private DnDHandler(JTree jTree) {

      this.jTree = jTree;

      DropTarget target = new DropTarget(jTree, this);
      dragSource = new DragSource();
      dragSource.createDefaultDragGestureRecognizer(
        jTree,
        DnDConstants.ACTION_MOVE,
        this);

    }

    private TreeNode getNodeForLocation(Point p) {
      TreePath path = jTree.getClosestPathForLocation(p.x, p.y);
      return (TreeNode) path.getLastPathComponent();
    }

    public void dragEnter(DropTargetDragEvent dtde) {
      dragOver(dtde);
    }

    /**
     * Determine whether drop is allowed. Attributes can be dropped
     * on other "attribute" nodes or the "atributes" node. Filters can be
     * dropped on other "filter" nodes or the "filters" node.
     */
    public void dragOver(DropTargetDragEvent dtde) {

      TreeNode node = getNodeForLocation(dtde.getLocation());

      if (node == attributesNode
        || attributesNode.isNodeChild(node)
        && node != selected)
        dtde.acceptDrag(DnDConstants.ACTION_MOVE);
      else
        dtde.rejectDrag();

    }

    /**
     * Move the desired attribute or filter to it's new position. Called
     * in response to a drop action.
     */
    public void drop(DropTargetDropEvent dtde) {

      TreeNode target = getNodeForLocation(dtde.getLocation());

      // remove node from old position
      int oldIndex = attributesNode.getIndex(selected);
      Attribute attribute = query.getAttributes()[oldIndex];
      query.removeAttribute(attribute);

      // insert selected node into the tree by adding to the query in the
      // correct position.
      int newIndex = -1;
      if (target == attributesNode)
        newIndex = 0;
      else if (attributesNode.isNodeChild(target))
        newIndex = attributesNode.getIndex(target) + 1;
      query.addAttribute(newIndex, attribute);

      dtde.getDropTargetContext().dropComplete(true);
    }

    /**
     * Set cursor to show that a drop is allowed. Called after
     * dragOver(DropTargetDragEvent dtde) if 
     * dtde.acceptDrag(DnDConstants.ACTION_MOVE) was called.
     */
    public void dragOver(DragSourceDragEvent dsde) {
      dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }

    /**
     * Set cursor to show that the drop is not allowed.Called after
     * dragOver(DropTargetDragEvent dtde) if dtde.reject() was called.
     */
    public void dragExit(DragSourceEvent dse) {
      dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
    }

    /**
     * Start the drag if an attribute is selected, otherwise do nothing.
     */
    public void dragGestureRecognized(DragGestureEvent dge) {

      TreePath path = jTree.getSelectionPath();

      if (path == null || path.getPathCount() <= 1)
        return;

      selected = (DefaultMutableTreeNode) path.getLastPathComponent();

      // only allow attributes to be dragged
      if (!attributesNode.isNodeChild(selected))
        return;

      // And start the drag process. We start with a no-drop cursor, assuming that the
      // user won't want to drop the item right where she picked it up.
      dragSource.startDrag(
        dge,
        DragSource.DefaultMoveNoDrop,
        dummyTransferable,
        this);

    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragEnter(DragSourceDragEvent dsde) {
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
    }

  }

  /**
   * If an attribute or filter is currently selected delete it.
   */
  private final class DeleteAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {

      TreePath path = getSelectionModel().getSelectionPath();
      if (path == null)
        return;

      DefaultMutableTreeNode child =
        (DefaultMutableTreeNode) path.getLastPathComponent();
      TreeNode parent = child.getParent();
      int index = parent.getIndex(child);

      if (parent == attributesNode)
        query.removeAttribute(query.getAttributes()[index]);
      else if (parent == filtersNode)
        query.removeFilter(query.getFilters()[index]);

    }
  }

  private Feedback feedback = new Feedback(this);

  private String dsvInternalName;

  private DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();

  private DefaultMutableTreeNode dataSourceNode =
    new DefaultMutableTreeNode(TreeNodeData.createDataSourceNode());

  private DefaultMutableTreeNode datasetNode =
    new DefaultMutableTreeNode(TreeNodeData.createDatasetNode());

  private DefaultMutableTreeNode attributesNode =
    new DefaultMutableTreeNode(TreeNodeData.createAttributesNode());

  private DefaultMutableTreeNode filtersNode =
    new DefaultMutableTreeNode(TreeNodeData.createFilterNode());

  private DefaultMutableTreeNode formatNode =
    new DefaultMutableTreeNode(TreeNodeData.createFormatNode());

  private DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

  private final static Logger logger =
    Logger.getLogger(QueryTreeView.class.getName());

  private DSConfigAdaptor dsvAdaptor;

  private Query query;

  /**
   * Tree config showing the current state of the query. The current datasetConfig
   * is retrieved from the adaptor and this is used to determine how to render
   * the values stored in the query.
   * 
   * @param query Query represented by tree.
   * @param dsvAdaptor source of DatasetConfigs used to interpret query.
   */
  public QueryTreeView(Query query, DSConfigAdaptor dsvAdaptor) {

    super();

    this.query = query;
    this.dsvAdaptor = dsvAdaptor;
    query.addQueryChangeListener(this);

    setModel(treeModel);
    setRootVisible(false);

    rootNode.add(dataSourceNode);
    rootNode.add(datasetNode);
    rootNode.add(attributesNode);
    rootNode.add(filtersNode);
    rootNode.add(formatNode);

    getSelectionModel().setSelectionMode(
      TreeSelectionModel.SINGLE_TREE_SELECTION);

    // ensure the 1st level of nodes are visible
    TreePath path = new TreePath(rootNode).pathByAddingChild(dataSourceNode);
    makeVisible(path);

    getInputMap().put(
      KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
      "doDelete");
    getActionMap().put("doDelete", new DeleteAction());

    // handles dnd for this component
    new DnDHandler(this);

  }

  /**
   * Runs an interactive test program where the user can interact
   * with the QueryTreeView.
   */
  public static void main(String[] args) throws Exception {

    // default adaptor for retrieving datasetconfigs
    DSConfigAdaptor adaptor =
      QueryEditor.testDSConfigAdaptor(new CompositeDSConfigAdaptor());

    final Query query = new Query();
    final QueryTreeView qtv = new QueryTreeView(query, adaptor);
    Dimension d = new Dimension(500, 600);
    qtv.setPreferredSize(d);
    qtv.setMinimumSize(d);

    Box c = Box.createVerticalBox();

    qtv.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        if (e != null && e.getNewLeadSelectionPath() != null)
          logger.info(
            "Selected:" + e.getNewLeadSelectionPath().getLastPathComponent());
      }
    });

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

    box = Box.createHorizontalBox();
    c.add(box);
    b = new JButton("Print Query");
    box.add(b);
    b.addActionListener(new ActionListener() {
      private int count = 0;
      public void actionPerformed(ActionEvent e) {
        System.out.println(query.toString());
      }
    });

    c.add(new JScrollPane(qtv));

    JFrame f = new JFrame("QueryTreeView unit test");
    f.getContentPane().add(c);
    f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

    // preload some default settings
    //query.setDatasetConfig( adaptor.getDatasetConfigs()[0] );
    query.addAttribute(new FieldAttribute("ensembl_gene_id"));
    query.addFilter(new BasicFilter("ensembl_gene_id", "=", "ENSG001"));
    query.addFilter(new BasicFilter("chr_name", "=", "3"));

    f.setVisible(true);
    f.pack();
    int x = screen.width / 2 - f.getWidth() / 2;
    int y = screen.height / 2 - f.getHeight() / 2;
    f.setLocation(x, y);

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
   * Update the name of the dataset shown in the tree.
   * @see org.ensembl.mart.lib.QueryChangeListener#datasetChanged(org.ensembl.mart.lib.Query, java.lang.String, java.lang.String)
   */
  public void datasetChanged(
    Query source,
    String oldDataset,
    String newDataset) {

    String s = (newDataset != null) ? newDataset : "";
    ((TreeNodeData) datasetNode.getUserObject()).setRightText(s);
    treeModel.reload(datasetNode);
  }

  /**
   * Set the Datasource node with the newDatasource.
   * @see org.ensembl.mart.lib.QueryChangeListener#datasourceChanged(org.ensembl.mart.lib.Query, javax.sql.DataSource, javax.sql.DataSource)
   */
  public void datasourceChanged(
    Query sourceQuery,
    DataSource oldDatasource,
    DataSource newDatasource) {

    String s = (newDatasource != null) ? newDatasource.toString() : "";
    // TODO query.datasource:DataSource -> DetailedDataSource and propagate changes
    // through QueryListener.
    if (newDatasource != null && newDatasource instanceof DetailedDataSource) {
      DetailedDataSource ds = (DetailedDataSource) newDatasource;
      s = ds.getName();
    }

    ((TreeNodeData) dataSourceNode.getUserObject()).setRightText(s);
    treeModel.reload(dataSourceNode);

  }

  /**
   * Adds a child node to attributesNode at the specified index. 
   * If dsv->attributeDescription->displayName is available that is used for the
   * label, otherwise attribute.fieldName is used.
   * position in the list of attributes.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryAttributeAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void attributeAdded(
    Query sourceQuery,
    int index,
    Attribute attribute) {

    // TODO disambiguate: one attribute.getField()-> N*AttributeDescription

    // Try to get a user friendly labelName, 
    // otherwise use the raw one from attribute

    AttributeDescription ad = null;
    if (query.getDatasetConfig() != null)
      ad =
        query
          .getDatasetConfig()
          .getAttributeDescriptionByFieldNameTableConstraint(
          attribute.getField(),
          attribute.getTableConstraint());

    String nodeLabel =
      (ad != null) ? ad.getDisplayName() : attribute.getField();
    TreeNodeData userObject =
      new TreeNodeData(null, null, nodeLabel, attribute);
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
    scrollPathToVisible(path);
    if (select)
      setSelectionPath(path);

  }

  /**
   * Remove node from tree that corresponds to the attribute and select
   * the next attribute if available, otherwise the attributesNode.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryAttributeRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void attributeRemoved(
    Query sourceQuery,
    int index,
    Attribute attribute) {

    attributesNode.remove(index);
    treeModel.reload(attributesNode);
    select(attributesNode, index, true);

  }

  /**
   * Adds a child node to the filtersNode to represent the filter at the specified index.
   * The node label is partly derived from a corresponding FilterDescription retrieved
   * from the dsConfig, otherwise it is based solely on the filter.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryFilterAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void filterAdded(Query sourceQuery, int index, Filter filter) {

    // use rawfield as default for label
    String fieldName = filter.getField();

    // Try to get a user friendly fieldName, 
    // otherwise use the raw one from filter
    if (query.getDatasetConfig() != null) {

      FilterDescription fd =
        query
          .getDatasetConfig()
          .getFilterDescriptionByFieldNameTableConstraint(
          filter.getField(),
          filter.getTableConstraint());

      if (fd != null)
        fieldName =
          fd.getDisplayNameByFieldNameTableConstraint(
            filter.getField(),
            filter.getTableConstraint());

    }

    // Make the condition and value a little prettier in some cases.
    String condition = filter.getCondition().toLowerCase();
    if (condition.matches("\\s*is\\s+null\\s*"))
      condition = "excluded";
    else if (condition.matches("\\s*is\\s+not\\s+null\\s*"))
      condition = "required";
    String value = filter.getValue();
    if (value == null)
      value = "";

    String nodeLabel = fieldName + " " + condition + " " + value;
    
    TreeNodeData userObject = new TreeNodeData(null, null, nodeLabel, filter);
    DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(userObject);
    
    filtersNode.insert(treeNode, index);
    treeModel.reload(filtersNode);
    
    select(filtersNode, index, false);

  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryChangeListener#queryFilterRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void filterRemoved(Query sourceQuery, int index, Filter filter) {

    filtersNode.remove(index);
    treeModel.reload(filtersNode);
    select(filtersNode, index, true);

  }

  public void filterChanged(
    Query sourceQuery,
    Filter oldFilter,
    Filter newFilter) {
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#querySequenceDescriptionChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.SequenceDescription, org.ensembl.mart.lib.SequenceDescription)
   */
  public void sequenceDescriptionChanged(
    Query sourceQuery,
    SequenceDescription oldSequenceDescription,
    SequenceDescription newSequenceDescription) {
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryLimitChanged(org.ensembl.mart.lib.Query, int, int)
   */
  public void limitChanged(Query query, int oldLimit, int newLimit) {
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryStarBasesChanged(org.ensembl.mart.lib.Query, java.lang.String[], java.lang.String[])
   */
  public void starBasesChanged(
    Query sourceQuery,
    String[] oldStarBases,
    String[] newStarBases) {
  }

  /**
   * Do nothing.
   * @see org.ensembl.mart.lib.QueryChangeListener#queryPrimaryKeysChanged(org.ensembl.mart.lib.Query, java.lang.String[], java.lang.String[])
   */
  public void primaryKeysChanged(
    Query sourceQuery,
    String[] oldPrimaryKeys,
    String[] newPrimaryKeys) {
  }

  /**
   * Do nothing. 
   */
  public void datasetConfigChanged(
    Query query,
    DatasetConfig oldDatasetConfig,
    DatasetConfig newDatasetConfig) {

  }

}
