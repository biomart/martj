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
    protected DatasetViewTreeNode rootNode = null;
    protected TreePath clickedPath = null;
    protected DatasetViewTreeModel treemodel = null;

    public DatasetViewTree(DatasetView dsView) {
        super((TreeModel) null);
        this.dsView = dsView;
        addMouseListener(new DatasetViewTreeMouseListener());
        addTreeSelectionListener(new DatasetViewTreeSelectionListener());
        // Use horizontal and vertical lines
        putClientProperty("JTree.lineStyle", "Angled");
        setEditable(true);
        // Create the first node
        rootNode = new DatasetViewTreeNode(dsView.getDisplayName());
        rootNode.setUserObject(dsView);
        populateTree();
        treemodel = new DatasetViewTreeModel(rootNode);
        setModel(treemodel);
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
                        AttributeCollection atCollection = (AttributeCollection) collections[z];
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

    // Inner class that handles Menu Action Events
    protected class MenuActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("cut"))
                cut();
            else if (e.getActionCommand().equals("copy"))
                copy();
            else if (e.getActionCommand().equals("paste"))
                paste();
            else if (e.getActionCommand().equals("insert"))
                insert();
            else if (e.getActionCommand().equals("delete"))
                delete();
        }
    }

    // Inner class that handles Tree Selection Events
    protected class DatasetViewTreeSelectionListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            doOnSelection();
        }
    }

    private void doOnSelection(){
         lastSelectedNode = (DatasetViewTreeNode)
                    this.getLastSelectedPathComponent();
            if (lastSelectedNode == null) return;

            Object nodeInfo = lastSelectedNode.getUserObject();

            System.out.println(nodeInfo.getClass());
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

        public DatasetViewTreeDnDListener(DatasetViewTree tree){
            dropTarget = new DropTarget(tree, this);
            dragSource = new DragSource();
            dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_MOVE, this);
        }

        /** Internally implemented, Do not override!*/
        public void dragEnter(DropTargetDragEvent event) {
            event.acceptDrag(DnDConstants.ACTION_MOVE);
        }

        /** Internally implemented, Do not override!*/
        public void dragExit(DropTargetEvent event) {
        }

        /** Internally implemented, Do not override!*/
        public void dragOver(DropTargetDragEvent event) {
        }

        /** Internally implemented, Do not override!*/
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

        /** Internally implemented, Do not override!*/
        public void dropActionChanged(DropTargetDragEvent event) {
        }

        /** Internally implemented, Do not override!*/
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

        /** Internally implemented, Do not override!.
         * throws IllegalArgumentException.
         */
        public void dragDropEnd(DragSourceDropEvent event) {
            if (event.getDropSuccess()) {
                try {
                    if (dropnode.equals(selnode)) {
                        System.out.println("drag==drop");
                        throw new IllegalArgumentException("the source is the same as the destination");
                    } else {
                        dropnode.add(selnode);
                    }
                } catch (IllegalArgumentException iae) {
                    throw new IllegalArgumentException(iae.toString());
                }
                treemodel.reload();
            }
        }

        /** Internally implemented, Do not override!*/
        public void dragEnter(DragSourceDragEvent event) {
        }

        /** Internally implemented, Do not override!*/
        public void dragExit(DragSourceEvent event) {
        }

        /** Internally implemented, Do not override!*/
        public void dragOver(DragSourceDragEvent event) {
        }

        /** Internally implemented, Do not override!*/
        public void dropActionChanged(DragSourceDragEvent event) {
        }
    }

    // Inner class that handles Mouse events
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
        String[] menuItems = { "copy", "cut", "paste", "insert", "delete" };
        for( int i =0; i<menuItems.length; i++ ) {
           JMenuItem menuItem = new JMenuItem( menuItems[i] );
            MenuActionListener menuActionListener = new MenuActionListener();
            menuItem.addActionListener(menuActionListener);
            popup.add(menuItem);
        }
        popup.show(e.getComponent(),
                e.getX(), e.getY());

        clickedPath = this.getClosestPathForLocation(e.getX(), e.getY());
        System.out.println("here" + ((DatasetViewTreeNode) clickedPath.getLastPathComponent()).getUserObject().getClass());
    }

    private void cut() {
    }

    private void copy() {
    }

    private void paste() {
    }

    private void insert() {
        System.out.println("I'm inserting...");
        DatasetViewTreeNode node = new DatasetViewTreeNode("newNode");
        treemodel.insertNodeInto(node, (DatasetViewTreeNode) clickedPath.getLastPathComponent(), clickedPath.getPathCount() + 1);
    }

    private void delete() {
        System.out.println("I'm deleting...");
        DatasetViewTreeNode node = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
        treemodel.removeNodeFromParent(node);
        //node.removeFromParent();
    }


}
