package org.ensembl.mart.vieweditor;

/**
 * Created by IntelliJ IDEA.
 * User: katerina
 * Date: 19-Nov-2003
 * Time: 15:52:01
 * To change this template use Options | File Templates.
 */

import org.ensembl.mart.lib.config.*;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.dnd.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class DatasetViewTree extends JTree implements Autoscroll {

    public static final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
    protected Insets scrollInsets = defaultScrollInsets;
    protected DatasetView dsView = null;
    protected DatasetViewTreeNode lastSelectedNode = null;
    protected DatasetViewTreeNode editingNode = null;
    protected DatasetViewTreeNode clickedNode = null;
    protected DatasetViewTreeNode editingNodeParent = null;
    protected DatasetViewTreeNode rootNode = null;
    protected TreePath clickedPath = null;
    protected DatasetViewTreeModel treemodel = null;
    protected DatasetViewTreeWidget frame;
    protected int CUT_INITIATED, COPY_INITIATED, editingNodeIndex;
    protected DatasetViewAttributesTable attrTable = null;
    protected DatasetViewAttributeTableModel attrTableModel= null;

    public DatasetViewTree(DatasetView dsView, DatasetViewTreeWidget frame, DatasetViewAttributesTable attrTable) {
        super((TreeModel) null);
        this.dsView = dsView;
        this.frame = frame;
        this.attrTable = attrTable;
        addMouseListener(new DatasetViewTreeMouseListener());
        addTreeSelectionListener(new DatasetViewTreeSelectionListener());
        // Use horizontal and vertical lines
        putClientProperty("JTree.lineStyle", "Angled");
        setEditable(true);
        // Create the first node
        rootNode = new DatasetViewTreeNode(dsView.getDisplayName());
        rootNode.setUserObject(dsView);
        populateTree();
        treemodel = new DatasetViewTreeModel(rootNode, dsView);
        setModel(treemodel);
        this.setSelectionInterval(0, 0);
        DatasetViewTreeDnDListener dndListener = new DatasetViewTreeDnDListener(this);

    }

    private void populateTree() {
        AttributePage[] attributePages = dsView.getAttributePages();
        FilterPage[] filterPages = dsView.getFilterPages();

        populateFilterNodes(filterPages);
        populateAttributeNodes(attributePages);
    }

    private void populateFilterNodes(FilterPage[] filterPages) {
        for (int i = 0; i < filterPages.length; i++) {
            FilterPage fiPage = filterPages[i];
            String fiName = fiPage.getInternalName();
            DatasetViewTreeNode fiNode = new DatasetViewTreeNode("FilterPage:" + fiName);
            fiNode.setUserObject(fiPage);
            rootNode.add(fiNode);
            List groups = fiPage.getFilterGroups();
            for (int j = 0; j < groups.size(); j++) {
                if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.FilterGroup")) {
                    FilterGroup fiGroup = (FilterGroup) groups.get(j);
                    String grName = fiGroup.getInternalName();
                    DatasetViewTreeNode grNode = new DatasetViewTreeNode("FilterGroup:" + grName);
                    grNode.setUserObject(fiGroup);
                    fiNode.add(grNode);
                    FilterCollection[] collections = fiGroup.getFilterCollections();
                    for (int z = 0; z < collections.length; z++) {
                        FilterCollection fiCollection = collections[z];
                        String colName = fiCollection.getInternalName();
                        DatasetViewTreeNode colNode = new DatasetViewTreeNode("FilterCollection:" + colName);
                        colNode.setUserObject(fiCollection);
                        grNode.add(colNode);
                        List descriptions = fiCollection.getFilterDescriptions();
                        for (int y = 0; y < descriptions.size(); y++) {
                            FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
                            String desName = fiDescription.getInternalName();
                            DatasetViewTreeNode desNode = new DatasetViewTreeNode("FilterDescription:" + desName);
                            desNode.setUserObject(fiDescription);
                            colNode.add(desNode);
                        }
                    }
                }
            }
        }
    }

    private void populateAttributeNodes(AttributePage[] attributePages) {
        for (int i = 0; i < attributePages.length; i++) {
            AttributePage atPage = attributePages[i];
            String atName = atPage.getInternalName();
            DatasetViewTreeNode atNode = new DatasetViewTreeNode("AttributePage:" + atName);
            atNode.setUserObject(atPage);
            rootNode.add(atNode);
            List groups = atPage.getAttributeGroups();
            for (int j = 0; j < groups.size(); j++) {
                if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.AttributeGroup")) {
                    AttributeGroup atGroup = (AttributeGroup) groups.get(j);
                    String grName = atGroup.getInternalName();
                    DatasetViewTreeNode grNode = new DatasetViewTreeNode("AttributeGroup:" + grName);
                    grNode.setUserObject(atGroup);
                    atNode.add(grNode);
                    AttributeCollection[] collections = atGroup.getAttributeCollections();
                    for (int z = 0; z < collections.length; z++) {
                        AttributeCollection atCollection = collections[z];
                        String colName = atCollection.getInternalName();
                        DatasetViewTreeNode colNode = new DatasetViewTreeNode("AttributeCollection:" + colName);
                        grNode.add(colNode);
                        colNode.setUserObject(atCollection);
                        List descriptions = atCollection.getAttributeDescriptions();
                        for (int y = 0; y < descriptions.size(); y++) {
                            AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
                            String desName = atDescription.getInternalName();
                            DatasetViewTreeNode desNode = new DatasetViewTreeNode("AttributeDescription:" + desName);
                            desNode.setUserObject(atDescription);
                            colNode.add(desNode);
                        }
                    }
                }
            }
        }
    }

    public DatasetView getDatasetView() {
        dsView = (DatasetView) rootNode.getUserObject();
        AttributePage[] oldAttributePages = dsView.getAttributePages();
        FilterPage[] oldFilterPages = dsView.getFilterPages();
        int pagesCount = rootNode.getChildCount();

        return dsView;
    }

    // Autoscrolling support
    public void setScrollInsets(Insets insets) {
        this.scrollInsets = insets;
    }

    public Insets getScrollInsets() {
        return scrollInsets;
    }

    // Implementation of Autoscroll interface
    public Insets getAutoscrollInsets() {
        Rectangle r = getVisibleRect();
        Dimension size = getSize();
        Insets i = new Insets(r.y + scrollInsets.top, r.x + scrollInsets.left,
                size.height - r.y - r.height + scrollInsets.bottom,
                size.width - r.x - r.width + scrollInsets.right);
        return i;
    }

    public void autoscroll(Point location) {
        JScrollPane scroller =
                (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        if (scroller != null) {
            JScrollBar hBar = scroller.getHorizontalScrollBar();
            JScrollBar vBar = scroller.getVerticalScrollBar();
            Rectangle r = getVisibleRect();
            if (location.x <= r.x + scrollInsets.left) {
                // Need to scroll left
                hBar.setValue(hBar.getValue() - hBar.getUnitIncrement(-1));
            }
            if (location.y <= r.y + scrollInsets.top) {
                // Need to scroll up
                vBar.setValue(vBar.getValue() - vBar.getUnitIncrement(-1));
            }
            if (location.x >= r.x + r.width - scrollInsets.right) {
                // Need to scroll right
                hBar.setValue(hBar.getValue() + hBar.getUnitIncrement(1));
            }
            if (location.y >= r.y + r.height - scrollInsets.bottom) {
                // Need to scroll down
                vBar.setValue(vBar.getValue() + vBar.getUnitIncrement(1));
            }
        }

    }

    // Inner class that handles Tree Expansion Events
    protected class DatasetViewTreeExpansionHandler implements TreeExpansionListener {
        public void treeExpanded(TreeExpansionEvent evt) {
            TreePath path = evt.getPath();			// The expanded path
            JTree tree = (JTree) evt.getSource();	// The tree

            // Get the last component of the path and
            // arrange to have it fully populated.
            DatasetViewTreeNode node = (DatasetViewTreeNode) path.getLastPathComponent();
            /*if (node.populateFolders(true)) {
                 ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(node);
             }   */
        }

        public void treeCollapsed(TreeExpansionEvent evt) {
            // Nothing to do
        }
    }

    // Inner class that handles Tree Expansion Events
    protected class AttrTableModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent evt) {
            DatasetViewTreeNode child = (DatasetViewTreeNode) getLastSelectedPathComponent();
            DatasetViewTreeNode parent = (DatasetViewTreeNode)child.getParent();
            int index = parent.getIndex(child);
            treemodel.removeNodeFromParent(child);

            treemodel.insertNodeInto(attrTableModel.getNode(), parent, index);
        }

        public void treeCollapsed(TreeExpansionEvent evt) {
            // Nothing to do
        }
    }
    // Inner class that handles Menu Action Events
    protected class MenuActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                if (e.getActionCommand().equals("cut"))
                    cut();
                else if (e.getActionCommand().equals("copy"))
                    copy();
                else if (e.getActionCommand().equals("paste"))
                    paste();
                else if (e.getActionCommand().equals("insert filter page"))
                    insert(new FilterPage("new"), "FilterPage:");
                else if (e.getActionCommand().equals("insert attribute page"))
                    insert(new AttributePage("new"), "AttributePage:");
                else if (e.getActionCommand().equals("insert filter group"))
                    insert(new FilterGroup("new"), "FilterGroup:");
                else if (e.getActionCommand().equals("insert attribute group"))
                    insert(new AttributeGroup("new"), "AttributeGroup:");
                else if (e.getActionCommand().equals("insert filter collection"))
                    insert(new FilterCollection("new"), "FilterCollection:");
                else if (e.getActionCommand().equals("insert attribute collection"))
                    insert(new AttributeCollection("new"), "AttributeCollection:");
                /* else if (e.getActionCommand().equals("insert filter description"))
                     insert(new FilterDescription("mew"));
                 else if (e.getActionCommand().equals("insert attribute description"))
                     insert(new AttributeDescription("new")); */
                else if (e.getActionCommand().equals("delete"))
                    delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Inner class that handles Tree Selection Events
    protected class DatasetViewTreeSelectionListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            doOnSelection();
        }
    }

    private void doOnSelection() {
        lastSelectedNode = (DatasetViewTreeNode) this.getLastSelectedPathComponent();

        if (lastSelectedNode == null) return;
        String [] data = null;
        BaseConfigurationObject nodeObject = (BaseConfigurationObject)lastSelectedNode.getUserObject();
        String nodeObjectClass = nodeObject.getClass().getName();
        if (nodeObjectClass.equals("org.ensembl.mart.lib.config.DatasetView") ||
            nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterPage") ||
            nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributePage") ||
            nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeGroup") ||
            nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterGroup")||
            nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeGroup") ||
            nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterCollection") ||
            nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeCollection")  ) {
                data = new String [] { "Description","DisplayName","InternalName"};
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterDescription")) {
            data = new String[]{
                "Description", "DisplayName", "InternalName", "isSelectable", "Legal Qualifiers",
                "Qualifier", "Table Constraint", "Type"};
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeDescription")) {
            data = new String[]{
                "Description", "DisplayName", "InternalName", "Field",
                "Table Constraint", "Max Length", "Source","Homepage URL","Linkout URL"};
        }

        attrTableModel = new DatasetViewAttributeTableModel((DatasetViewTreeNode) this.getLastSelectedPathComponent(),data, nodeObjectClass);
        attrTableModel.addTableModelListener(new AttrTableModelListener());

       // model.setObject(nodeObject);
        attrTable.setModel(attrTableModel);

    }

    // Inner class that handles Tree Model Events
    protected class DatasetViewTreeModelListener implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {
            System.out.println("treeNodesChanged");

            /* if (nodeInfo.getClass().getName().equals("org.ensembl.mart.lib.config.AttributePage"))
              ((AttributePage)nodeInfo).setInternalName();
             else if nodeInfo.getClass().getName().equals("org.ensembl.mart.lib.config.AttributePage"))
              ((AttributePage)nodeInfo).setInternalName();
              */
            /*  Object[] children = e.getChildren();
              int[] childIndices = e.getChildIndices();
              for (int i = 0; i < children.length; i++) {
                  System.out.println("Index " + childIndices[i] + ",changed value: " + children[0]);
              }  */
        }


        public void treeStructureChanged(TreeModelEvent e) {
            System.out.println("tree structure changed");
        }

        public void treeNodesInserted(TreeModelEvent e) {
            TreePath tPath = e.getTreePath();
            System.out.println("tree nodes inserted");
            //tPath.getPathComponent();
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            System.out.println("tree nodes removed");
        }

    }

    // Inner class that handles Tree Expansion Events
    protected class DatasetViewTreeDnDListener implements DropTargetListener, DragSourceListener, DragGestureListener {
        protected DropTarget dropTarget = null;
        protected DragSource dragSource = null;
        protected DatasetViewTreeNode selnode = null;
        protected DatasetViewTreeNode dropnode = null;

        public DatasetViewTreeDnDListener(DatasetViewTree tree) {
            dropTarget = new DropTarget(tree, this);
            dragSource = new DragSource();
            dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_MOVE, this);
        }


        public void dragEnter(DropTargetDragEvent event) {
            event.acceptDrag(DnDConstants.ACTION_MOVE);
        }


        public void dragExit(DropTargetEvent event) {
        }


        public void dragOver(DropTargetDragEvent event) {
        }


        public void drop(DropTargetDropEvent event) {
            try {
                Transferable transferable = event.getTransferable();

                if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    event.acceptDrop(DnDConstants.ACTION_MOVE);
                    String s = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                    Object ob = event.getSource();
                    Point droppoint = event.getLocation();
                    TreePath droppath = getClosestPathForLocation(droppoint.x, droppoint.y);
                    dropnode = (DatasetViewTreeNode) droppath.getLastPathComponent();
                    event.getDropTargetContext().dropComplete(true);
                } else {
                    event.rejectDrop();
                }
            } catch (IOException exception) {
                event.rejectDrop();
            } catch (UnsupportedFlavorException ufException) {
                event.rejectDrop();
            }
        }


        public void dropActionChanged(DropTargetDragEvent event) {
        }


        public void dragGestureRecognized(DragGestureEvent event) {
            selnode = null;
            dropnode = null;
            Object selected = getSelectionPath();
            TreePath treepath = (TreePath) selected;
            selnode = (DatasetViewTreeNode) treepath.getLastPathComponent();
            if (selected != null) {
                StringSelection text = new StringSelection(selected.toString());
                dragSource.startDrag(event, DragSource.DefaultMoveDrop, text, this);
            } else {
            }
        }


        public void dragDropEnd(DragSourceDropEvent event) {
            if (event.getDropSuccess()) {
                try {
                    if (dropnode.equals(selnode)) {
                        String result = "Error, illegal action, drag==drop, the source is \nthe same as the destination";
                        JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    } else {

                        //dropnode.add(selnode);

                        editingNodeParent = (DatasetViewTreeNode)selnode.getParent();
                        editingNodeIndex = editingNodeParent.getIndex(selnode);

                        treemodel.removeNodeFromParent(selnode);

                        String result = treemodel.insertNodeInto(selnode, dropnode, dropnode.getChildCount());
                        if (result.startsWith("Error")) {
                            JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
                            //in case of error we need to insert back the deleted node...
                            treemodel.insertNodeInto(selnode, editingNodeParent, editingNodeIndex);
                            CUT_INITIATED = 0;

                            return;
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException(iae.toString());
                }
                //treemodel.reload(selnode.getParent());
            }
        }

        public void dragEnter(DragSourceDragEvent event) {
        }

        public void dragExit(DragSourceEvent event) {
        }

        public void dragOver(DragSourceDragEvent event) {
        }

        public void dropActionChanged(DragSourceDragEvent event) {
        }
    }

    protected class DatasetViewTreeMouseListener implements MouseListener {
        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == 3) {
                //Create the popup menu.
                loungePopupMenu(e);
            }
        }
    }

    private void loungePopupMenu(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        clickedPath = this.getClosestPathForLocation(e.getX(), e.getY());
        clickedNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
        String[] menuItems = null;
        String clickedNodeClass = clickedNode.getUserObject().getClass().getName();
        if (clickedNodeClass.equals("org.ensembl.mart.lib.config.DatasetView"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter page", "insert attribute page", "delete"};
        else if ((clickedNodeClass).equals("org.ensembl.mart.lib.config.FilterPage"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter group", "delete"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributePage"))
            menuItems = new String[]{"copy", "cut", "paste", "insert attribute group", "delete"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.FilterGroup"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter collection", "delete"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributeGroup"))
            menuItems = new String[]{"copy", "cut", "paste", "insert attribute collection", "delete"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.FilterCollection"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter description", "delete"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributeCollection"))
            menuItems = new String[]{"copy", "cut", "paste", "insert attribute description", "delete"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
            menuItems = new String[]{"copy", "cut", "paste", "delete"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
            menuItems = new String[]{"copy", "cut", "paste", "delete"};

        for (int i = 0; i < menuItems.length; i++) {
            JMenuItem menuItem = new JMenuItem(menuItems[i]);
            MenuActionListener menuActionListener = new MenuActionListener();
            menuItem.addActionListener(menuActionListener);
            popup.add(menuItem);
        }
        popup.show(e.getComponent(),
                e.getX(), e.getY());


    }

    private void cut() {
        CUT_INITIATED = 1;
        COPY_INITIATED = 0;
        editingNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();

        editingNodeParent = (DatasetViewTreeNode) editingNode.getParent();
        editingNodeIndex = editingNodeParent.getIndex(editingNode);
        treemodel.removeNodeFromParent(editingNode);
    }

    private void copy() {
        CUT_INITIATED = 0;
        COPY_INITIATED = 1;
        // clickedNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
        // editingNode = (DatasetViewTreeNode)clickedNode.clone();
        editingNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
    }

    public DatasetViewTreeNode getEditingNode() {
        return editingNode;
    }

    private void paste() {
        if (CUT_INITIATED == 0 && COPY_INITIATED == 0) {
            String result = "Sorry nothing to paste.\nPlease copy or cut before pasting";
            JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
       DatasetViewTreeNode parentNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
        String result = treemodel.insertNodeInto(editingNode, parentNode, parentNode.getChildCount());
        //String result ="";
        //parentNode.add(editingNode);
        //treemodel.reload(parentNode);
        if (result.startsWith("Error")) {
            JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
            if (CUT_INITIATED == 1) {
                //editingNodeParent.add(editingNode);
                treemodel.insertNodeInto(editingNode, editingNodeParent, editingNodeIndex);
                CUT_INITIATED = 0;
            }
            return;
        }

        CUT_INITIATED = 0;
        COPY_INITIATED = 0;

        DatasetView view = (DatasetView) rootNode.getUserObject();
        FilterPage fp [] = view.getFilterPages();
        List fg = fp[0].getFilterGroups();
        FilterGroup f = (FilterGroup) fg.get(0);
        FilterCollection fc [] = f.getFilterCollections();
    }

    private void insert(BaseConfigurationObject obj, String name) {

        DatasetViewTreeNode parentNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();

        DatasetViewTreeNode newNode = new DatasetViewTreeNode(name + "newNode", obj);

        String result = treemodel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
        if (result.startsWith("Error")) {
            JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void delete() {
        DatasetViewTreeNode node = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
        treemodel.removeNodeFromParent(node);
    }
}
