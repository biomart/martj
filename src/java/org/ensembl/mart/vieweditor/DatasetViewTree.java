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

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.Autoscroll;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.ensembl.mart.lib.config.*;
import org.ensembl.mart.lib.DetailedDataSource;

/**
 * Class DatasetViewTree extends JTree.
 *
 * <p>This is the main class of the view editor that creates and populates the tree etc
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetView
 */

public class DatasetViewTree extends JTree implements Autoscroll, ClipboardOwner {

    public static final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
    protected Insets scrollInsets = defaultScrollInsets;
    protected DatasetView dsView = null;
    protected DatasetViewTreeNode lastSelectedNode = null;
    protected DatasetViewTreeNode editingNode = null;
    protected DatasetViewTreeNode editingNodeParent = null;
    protected DatasetViewTreeNode rootNode = null;
    protected TreePath clickedPath = null;
    protected DatasetViewTreeModel treemodel = null;
    protected DatasetViewTreeWidget frame;
    protected DatasetViewAttributesTable attrTable = null;
    protected DatasetViewAttributeTableModel attrTableModel = null;
    protected Clipboard clipboard;
    protected boolean cut = false;
    protected int editingNodeIndex;
    protected File file = null;

    public DatasetViewTree(DatasetView dsView, DatasetViewTreeWidget frame, DatasetViewAttributesTable attrTable) {
        super((TreeModel) null);
        this.dsView = dsView;
        this.frame = frame;
        this.attrTable = attrTable;
        file = frame.getFileChooserPath();
        addMouseListener(new DatasetViewTreeMouseListener());
        addTreeSelectionListener(new DatasetViewTreeSelectionListener());
        // Use horizontal and vertical lines
        putClientProperty("JTree.lineStyle", "Angled");
        setEditable(true);
        // Create the first node
        rootNode = new DatasetViewTreeNode(dsView.getDisplayName());
        rootNode.setUserObject(dsView);
        treemodel = new DatasetViewTreeModel(rootNode, dsView);
        setModel(treemodel);
        this.setSelectionInterval(0, 0);
        DatasetViewTreeDnDListener dndListener = new DatasetViewTreeDnDListener(this);
        clipboard = new Clipboard("tree_clipboard");

    }

    public DatasetView getDatasetView() {
        dsView = (DatasetView) rootNode.getUserObject();
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

            // treemodel.reload(attrTableModel.getNode(),(DatasetViewTreeNode)attrTableModel.getNode().getParent());
            treemodel.reload(attrTableModel.getParentNode());
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
                else if (e.getActionCommand().equals("insert filter description")) {
                    FilterDescription fd = new FilterDescription();
                    fd.setAttribute("internalName", "new");
                    insert(fd, "FilterDescription");
                } else if (e.getActionCommand().equals("insert attribute description")) {
                    AttributeDescription ad = new AttributeDescription();
                    ad.setAttribute("internalName", "new");
                    insert(ad, "AttributeDescription");
                } else if (e.getActionCommand().equals("insert option")) {
                    Option option = new Option();
                    option.setAttribute("internalName", "new");
                    insert(option, "Option");
                }else if (e.getActionCommand().equals("insert enable")) {
				    Enable enable = new Enable();
				    enable.setAttribute("ref", "new");
				    insert(enable, "Enable");
				}else if (e.getActionCommand().equals("add push action")) {
				    addPushAction();  
				}else if (e.getActionCommand().equals("make drop down")) {
				    makeDropDown();       
                }else if (e.getActionCommand().equals("delete"))
                    delete();
                else if (e.getActionCommand().equals("save"))
                    save();
                else if (e.getActionCommand().equals("save as"))
                    save_as();
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
        if (attrTable != null)
            if (attrTable.getEditorComponent() != null) {
                TableCellEditor attrTableEditor = attrTable.getCellEditor();
                attrTableEditor.stopCellEditing();
            }
        lastSelectedNode = (DatasetViewTreeNode) this.getLastSelectedPathComponent();
        if (lastSelectedNode == null) return;
        BaseConfigurationObject nodeObject = (BaseConfigurationObject) lastSelectedNode.getUserObject();
        String nodeObjectClass = nodeObject.getClass().getName();
        String[] data = nodeObject.getXmlAttributeTitles();

        attrTableModel = new DatasetViewAttributeTableModel((DatasetViewTreeNode) this.getLastSelectedPathComponent(), data, nodeObjectClass);
        attrTableModel.addTableModelListener(new AttrTableModelListener());

        // model.setObject(nodeObject);
        attrTable.setModel(attrTableModel);

    }

    // Inner class that handles Tree Model Events
    protected class DatasetViewTreeModelListener implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {
            System.out.println("treeNodesChanged");
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
                        String result = new String();
                        DatasetViewTreeNode selnodeParent;
                        int selnodeIndex;
                        if (selnode.getUserObject().getClass().equals(dropnode.getUserObject().getClass())) {
                            selnodeParent = (DatasetViewTreeNode) selnode.getParent();
                            selnodeIndex = selnodeParent.getIndex(selnode);
                            treemodel.removeNodeFromParent(selnode);
                            result = treemodel.insertNodeInto(selnode, (DatasetViewTreeNode) dropnode.getParent(), dropnode.getParent().getIndex(dropnode) + 1);
                        } else {
                            selnodeParent = (DatasetViewTreeNode) selnode.getParent();
                            selnodeIndex = selnodeParent.getIndex(selnode);
                            treemodel.removeNodeFromParent(selnode);
                            result = treemodel.insertNodeInto(selnode, dropnode, 0);
                        }
                        if (result.startsWith("Error")) {
                            JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
                            treemodel.insertNodeInto(selnode, selnodeParent, selnodeIndex);
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }

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
            if (attrTable != null)
                if (attrTable.getEditorComponent() != null) {
                    TableCellEditor attrTableEditor = attrTable.getCellEditor();
                    attrTableEditor.stopCellEditing();
                }
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
        editingNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
        String[] menuItems = null;
        String clickedNodeClass = editingNode.getUserObject().getClass().getName();
        if (clickedNodeClass.equals("org.ensembl.mart.lib.config.DatasetView"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter page", "insert attribute page", "delete", "save","save as"};
        else if ((clickedNodeClass).equals("org.ensembl.mart.lib.config.FilterPage"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter group", "delete", "save","save as"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributePage"))
            menuItems = new String[]{"copy", "cut", "paste", "insert attribute group", "delete", "save","save as"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.FilterGroup"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter collection", "delete", "save","save as"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributeGroup"))
            menuItems = new String[]{"copy", "cut", "paste", "insert attribute collection", "delete", "save","save as"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.FilterCollection"))
            menuItems = new String[]{"copy", "cut", "paste", "insert filter description", "delete", "save","save as"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributeCollection"))
            menuItems = new String[]{"copy", "cut", "paste", "insert attribute description", "delete", "save","save as"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
            menuItems = new String[]{"copy", "cut", "paste", "insert option", "make drop down", "insert enable", "add push action", "delete", "save","save as"};
		else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.Option"))
			menuItems = new String[]{"copy", "cut", "paste", "insert option", "make drop down", "insert enable", "add push action", "delete", "save","save as"};
        else if (clickedNodeClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
            menuItems = new String[]{"copy", "cut", "paste", "delete", "save","save as"};

        for (int i = 0; i < menuItems.length; i++) {
            JMenuItem menuItem = new JMenuItem(menuItems[i]);
            MenuActionListener menuActionListener = new MenuActionListener();
            menuItem.addActionListener(menuActionListener);
            popup.add(menuItem);
        }
        popup.show(e.getComponent(),
                e.getX(), e.getY());


    }

    public void cut() {
        cut = true;
        editingNodeParent = (DatasetViewTreeNode) editingNode.getParent();
        editingNodeIndex = editingNode.getParent().getIndex(editingNode);
        treemodel.removeNodeFromParent(editingNode);
        copy();
    }

    public void copy() {

        String editingNodeClass = editingNode.getUserObject().getClass().getName();
        DatasetViewTreeNode copiedNode = new DatasetViewTreeNode("");

        if ((editingNodeClass).equals("org.ensembl.mart.lib.config.FilterPage"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new FilterPage((FilterPage) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.AttributePage"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new AttributePage((AttributePage) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.FilterGroup"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new FilterGroup((FilterGroup) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.AttributeGroup"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new AttributeGroup((AttributeGroup) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.FilterCollection"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new FilterCollection((FilterCollection) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.AttributeCollection"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new AttributeCollection((AttributeCollection) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new FilterDescription((FilterDescription) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new AttributeDescription((AttributeDescription) editingNode.getUserObject()));
        else if (editingNodeClass.equals("org.ensembl.mart.lib.config.Option"))
            copiedNode = new DatasetViewTreeNode(editingNode.toString(), new Option((Option) editingNode.getUserObject()));
        DatasetViewTreeNodeSelection ss = new DatasetViewTreeNodeSelection(copiedNode);
        clipboard.setContents(ss, this);
    }

    public DatasetViewTreeNode getEditingNode() {
        return editingNode;
    }

    public void paste() {
        Transferable t = clipboard.getContents(this);
        try {

            DatasetViewTreeNode selnode = (DatasetViewTreeNode) t.getTransferData(new DataFlavor(Class.forName("org.ensembl.mart.vieweditor.DatasetViewTreeNode"), "treeNode"));
            DatasetViewTreeNode dropnode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
            String result = new String();
            if (selnode.getUserObject().getClass().equals(dropnode.getUserObject().getClass())) {
                result = treemodel.insertNodeInto(selnode, (DatasetViewTreeNode) dropnode.getParent(), dropnode.getParent().getIndex(dropnode) + 1);
            } else {
                if (selnode.getUserObject() instanceof org.ensembl.mart.lib.config.AttributePage) {
                    result = treemodel.insertNodeInto(selnode, dropnode, ((DatasetView) dropnode.getUserObject()).getFilterPages().length);
                } else
                    result = treemodel.insertNodeInto(selnode, dropnode, 0);
            }
            if (result.startsWith("Error")) {
                JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
                if (cut)
                    treemodel.insertNodeInto(selnode, editingNodeParent, editingNodeIndex);
            }
            cut = false;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void insert(BaseConfigurationObject obj, String name) {
		
		DatasetViewTreeNode parentNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();

        DatasetViewTreeNode newNode = new DatasetViewTreeNode(name + "newNode", obj);
        String result = new String();
        if (newNode.getUserObject() instanceof org.ensembl.mart.lib.config.AttributePage) {
            result = treemodel.insertNodeInto(newNode, parentNode, ((DatasetView) parentNode.getUserObject()).getFilterPages().length);
        } else
            result = treemodel.insertNodeInto(newNode, parentNode, 0);
        if (result.startsWith("Error")) {
            JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

	public void addPushAction()
	    throws ConfigurationException, SQLException {
	    	
		String filter2 = JOptionPane.showInputDialog("Filter Description to set:");	
	    	
		dsView = (DatasetView) ((DatasetViewTreeNode) this.getModel().getRoot()).getUserObject();
		FilterDescription fd2 = dsView.getFilterDescriptionByInternalName(filter2);
        
        // set FilterDescription fd1 = to current node
	    DatasetViewTreeNode node = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
		FilterDescription fd1 = (FilterDescription) node.getUserObject();

        String pushField = fd2.getField();
        String pushInternalName = fd2.getInternalName();
        String pushTableName = fd2.getTableConstraint();
        String field = fd1.getField();
        
        // eventually will have dsource open already
        DetailedDataSource dsource = new DetailedDataSource(
            "mysql",
            "web27.ebi.ac.uk",
            "3327",
            "ensembl_mart_test",
            "admin",
            ">fc9i9m92",
            DetailedDataSource.DEFAULTPOOLSIZE,
            "com.mysql.jdbc.Driver");
	            
        
		Option[] options = fd1.getOptions();
		DatasetViewTreeNode parentNode = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
		
	    for (int i = 0; i < options.length; i++ ){
			Option op = options[i];
			String opName = op.getInternalName();
			PushAction pa = new PushAction(pushInternalName + "_push_" + opName, null, null, pushInternalName );
			
			pa.addOptions(DatabaseDatasetViewUtils.getLookupOptions(pushField,pushTableName,field,opName,dsource));
			
			if (pa.getOptions().length > 0){  
			  Enumeration children = parentNode.children();
			  DatasetViewTreeNode childNode = null;
			  while (children.hasMoreElements()){
			  	childNode = (DatasetViewTreeNode) children.nextElement();
			  	if (op.equals(childNode.getUserObject()))
			  	  break;
			  }
			  DatasetViewTreeNode newNode = new DatasetViewTreeNode("PushAction:newNode", pa);
			  String result = treemodel.insertNodeInto(newNode, childNode, 0);
			  if (result.startsWith("Error")) {
				JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			  }
			}
	    }
        
	}

	public void makeDropDown()
		throws ConfigurationException, SQLException {
		
		DatasetViewTreeNode node = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
		FilterDescription fd1 = (FilterDescription) node.getUserObject();
        
		dsView = (DatasetView) ((DatasetViewTreeNode) this.getModel().getRoot()).getUserObject();
        
		// eventually will have dsource open already
		DetailedDataSource dsource = new DetailedDataSource(
				"mysql",
				"web27.ebi.ac.uk",
				"3327",
				"ensembl_mart_test",
				"admin",
				">fc9i9m92",
				DetailedDataSource.DEFAULTPOOLSIZE,
				"com.mysql.jdbc.Driver");
        
        String field = fd1.getField();
        String tableName = fd1.getTableConstraint();
        String joinKey = fd1.getKey();
		fd1.setType("list");
		fd1.setQualifier("=");
		fd1.setLegalQualifiers("=");

		Option[] options = DatabaseDatasetViewUtils.getOptions(field, tableName, joinKey, dsource, dsView);
		for (int k = options.length - 1; k > -1; k-- ){
			insert(options[k], "Option");
		}
	}

    public void delete() {
        DatasetViewTreeNode node = (DatasetViewTreeNode) clickedPath.getLastPathComponent();
        treemodel.removeNodeFromParent(node);
    }

    public void save_as() {
        dsView = (DatasetView) ((DatasetViewTreeNode) this.getModel().getRoot()).getUserObject();
        JFileChooser fc;
        if (frame.getFileChooserPath() != null)  {
            fc = new JFileChooser(frame.getFileChooserPath());
            fc.setDragEnabled(true);
            fc.setSelectedFile(frame.getFileChooserPath());
            fc.setDialogTitle("Save as");
        }
        else
            fc = new JFileChooser();
        XMLFileFilter filter = new XMLFileFilter();
        fc.addChoosableFileFilter(filter);
        int returnVal = fc.showSaveDialog(frame.getContentPane());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                DatasetViewXMLUtils.DatasetViewToFile(dsView, fc.getSelectedFile());
                frame.setFileChooserPath(fc.getSelectedFile());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        dsView = (DatasetView) ((DatasetViewTreeNode) this.getModel().getRoot()).getUserObject();
        try {
            if(frame.getFileChooserPath() != null)
                DatasetViewXMLUtils.DatasetViewToFile(dsView, frame.getFileChooserPath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lostOwnership(Clipboard c, Transferable t) {

    }
}
