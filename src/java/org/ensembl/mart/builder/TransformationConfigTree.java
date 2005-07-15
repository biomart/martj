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

package org.ensembl.mart.builder;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
//import java.awt.datatransfer.Clipboard;
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
import java.awt.Color;

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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.ensembl.mart.builder.config.*;

/**
 * Class TransformationConfigTree extends JTree.
 *
 * <p>This is the main class of the builder that creates and populates the tree etc
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.config.TransformationConfig
 */

public class TransformationConfigTree extends JTree implements Autoscroll { //, ClipboardOwner {

	public static final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
	protected Insets scrollInsets = defaultScrollInsets;
	protected TransformationConfig dsConfig = null;
	protected TransformationConfigTreeNode lastSelectedNode = null;
	protected TransformationConfigTreeNode editingNode = null;
	protected TransformationConfigTreeNode editingNodeParent = null;
	protected TransformationConfigTreeNode rootNode = null;
	protected TreePath clickedPath = null;
	protected TransformationConfigTreeModel treemodel = null;
	protected TransformationConfigTreeWidget frame;
	protected TransformationConfigAttributesTable attrTable = null;
	protected TransformationConfigAttributeTableModel attrTableModel = null;
	//protected Clipboard clipboard;
	protected boolean cut = false;
	protected int editingNodeIndex;
	protected File file = null;

	public TransformationConfigTree(
		TransformationConfig dsConfig,
		TransformationConfigTreeWidget frame,
		TransformationConfigAttributesTable attrTable) {
		super((TreeModel) null);
		this.dsConfig = dsConfig;
		this.frame = frame;
		this.attrTable = attrTable;
		file = frame.getFileChooserPath();
		addMouseListener(new TransformationConfigTreeMouseListener());
		addTreeSelectionListener(new TransformationConfigTreeSelectionListener());
		// Use horizontal and vertical lines
		putClientProperty("JTree.lineStyle", "Angled");
		setEditable(true);
		// Create the first node
		rootNode = new TransformationConfigTreeNode("new");
		rootNode.setUserObject(dsConfig);
		treemodel = new TransformationConfigTreeModel(rootNode, dsConfig);
		setModel(treemodel);
		this.setSelectionInterval(0, 0);
		TransformationConfigTreeDnDListener dndListener = new TransformationConfigTreeDnDListener(this);
		//clipboard = new Clipboard("tree_clipboard");

	}

	public TransformationConfig getTransformationConfig() {
		dsConfig = (TransformationConfig) rootNode.getUserObject();
		return dsConfig;
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
		Insets i =
			new Insets(
				r.y + scrollInsets.top,
				r.x + scrollInsets.left,
				size.height - r.y - r.height + scrollInsets.bottom,
				size.width - r.x - r.width + scrollInsets.right);
		return i;
	}

	public void autoscroll(Point location) {
		JScrollPane scroller = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
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
	protected class TransformationConfigTreeExpansionHandler implements TreeExpansionListener {
		public void treeExpanded(TreeExpansionEvent evt) {
			TreePath path = evt.getPath(); // The expanded path
			JTree tree = (JTree) evt.getSource(); // The tree

			// Get the last component of the path and
			// arrange to have it fully populated.
			TransformationConfigTreeNode node = (TransformationConfigTreeNode) path.getLastPathComponent();
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

			// treemodel.reload(attrTableModel.getNode(),(TransformationConfigTreeNode)attrTableModel.getNode().getParent());
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
				else if (e.getActionCommand().equals("insert transformation"))
					insert(new Transformation("new"), "Transformation:");
				else if (e.getActionCommand().equals("insert transformation line"))
					insert(new TransformationUnit("new"), "TransformationLine:");					
				else if (e.getActionCommand().equals("delete"))
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
	protected class TransformationConfigTreeSelectionListener implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			doOnSelection();
		}
	}

	private void doOnSelection() {			
		if (attrTable != null)
			if (attrTable.getEditorComponent() != null) {
				TableCellEditor attrTableEditor = attrTable.getCellEditor();
			}
		lastSelectedNode = (TransformationConfigTreeNode) this.getLastSelectedPathComponent();
		
		if (lastSelectedNode == null)
			return;
		BaseConfigurationObject nodeObject = (BaseConfigurationObject) lastSelectedNode.getUserObject();
		String nodeObjectClass = nodeObject.getClass().getName();
		String[] data = nodeObject.getXmlAttributeTitles();

		attrTableModel =
			new TransformationConfigAttributeTableModel(
				(TransformationConfigTreeNode) this.getLastSelectedPathComponent(),
				data,
				nodeObjectClass);
		attrTableModel.addTableModelListener(new AttrTableModelListener());

		attrTable.setModel(attrTableModel);

	}

	// Inner class that handles Tree Model Events
	protected class TransformationConfigTreeModelListener implements TreeModelListener {
		public void treeNodesChanged(TreeModelEvent e) {
		}
		public void treeStructureChanged(TreeModelEvent e) {
		}
		public void treeNodesInserted(TreeModelEvent e) {
			TreePath tPath = e.getTreePath();
		}
		public void treeNodesRemoved(TreeModelEvent e) {
		}
	}

	// Inner class that handles Tree Expansion Events
	protected class TransformationConfigTreeDnDListener implements DropTargetListener, DragSourceListener, DragGestureListener{
		protected DropTarget dropTarget = null;
		protected DragSource dragSource = null;
		protected TransformationConfigTreeNode selnode = null;
		protected TransformationConfigTreeNode dropnode = null;

		public TransformationConfigTreeDnDListener(TransformationConfigTree tree) {
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
					dropnode = (TransformationConfigTreeNode) droppath.getLastPathComponent();
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
			selnode = (TransformationConfigTreeNode) treepath.getLastPathComponent();
			if (selected != null) {
				StringSelection text = new StringSelection(selected.toString());
				dragSource.startDrag(event, DragSource.DefaultMoveDrop, text, this);
			} else {
			}
		}

		public void dragDropEnd(DragSourceDropEvent event){
			if (event.getDropSuccess()) {
				try {
					if (dropnode.equals(selnode)) {
						String result = "Error, illegal action, drag==drop, the source is \nthe same as the destination";
						JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
						return;
					} else {
						String result = new String();
						TransformationConfigTreeNode selnodeParent;
						int selnodeIndex;
						if (selnode.getUserObject().getClass().equals(dropnode.getUserObject().getClass())) {
							selnodeParent = (TransformationConfigTreeNode) selnode.getParent();
							selnodeIndex = selnodeParent.getIndex(selnode);
							treemodel.removeNodeFromParent(selnode);

							result =
									treemodel.insertNodeInto(
										selnode,
										(TransformationConfigTreeNode) dropnode.getParent(),
										dropnode.getParent().getIndex(dropnode) + 1);
						} else {
							selnodeParent = (TransformationConfigTreeNode) selnode.getParent();
							selnodeIndex = selnodeParent.getIndex(selnode);
							treemodel.removeNodeFromParent(selnode);
							result =
								treemodel.insertNodeInto(
									selnode,
									dropnode,
									TransformationConfigTreeNode.getHeterogenousOffset(
										((TransformationConfigTreeNode) dropnode).getUserObject(),
										((TransformationConfigTreeNode) selnode).getUserObject()));
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

	protected class TransformationConfigTreeMouseListener implements MouseListener {
		public void mousePressed(MouseEvent e) {
//			if (attrTable != null)
//				if (attrTable.getEditorComponent() != null) {
//					TableCellEditor attrTableEditor = attrTable.getCellEditor();
//					attrTableEditor.stopCellEditing();
//				}
			//need to evaluate here as well as mouseReleased, for cross platform portability
			if (e.isPopupTrigger()) {
				//Create the popup menu.
				loungePopupMenu(e);
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				//Create the popup menu.
				loungePopupMenu(e);
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
//			if (e.isPopupTrigger()) {
//				//Create the popup menu.
//				loungePopupMenu(e);
//			}
		}
	}

	private void loungePopupMenu(MouseEvent e) {
		JPopupMenu popup = new JPopupMenu();
		clickedPath = this.getClosestPathForLocation(e.getX(), e.getY());
		editingNode = (TransformationConfigTreeNode) clickedPath.getLastPathComponent();
		setSelectionPath(clickedPath);
		String[] menuItems = null;
		String clickedNodeClass = editingNode.getUserObject().getClass().getName();
		if (clickedNodeClass.equals("org.ensembl.mart.builder.config.TransformationConfig"))
			menuItems = new String[] { "copy", "cut", "paste", "delete", "insert transformation"};
		else if ((clickedNodeClass).equals("org.ensembl.mart.builder.config.TransformationUnit"))
			menuItems = new String[] { "copy", "cut", "paste", "delete", "insert transformation line" };
		else if (clickedNodeClass.equals("org.ensembl.mart.builder.config.TransformationLine"))
			menuItems = new String[] { "copy", "cut", "paste", "delete", "hide toggle"};

		for (int i = 0; i < menuItems.length; i++) {
			JMenuItem menuItem = new JMenuItem(menuItems[i]);
			MenuActionListener menuActionListener = new MenuActionListener();
			menuItem.addActionListener(menuActionListener);
			popup.add(menuItem);
		}
		popup.show(e.getComponent(), e.getX(), e.getY());

	}

	public void cut() {
		cut = true;
		editingNode = setEditingNode();

		if (editingNode == null)
			return;
		editingNodeParent = (TransformationConfigTreeNode) editingNode.getParent();
		editingNodeIndex = editingNode.getParent().getIndex(editingNode);
		treemodel.removeNodeFromParent(editingNode);

		copy();
	}

	private TransformationConfigTreeNode setEditingNode() {

		TreePath path = getSelectionPath();
		return ((TransformationConfigTreeNode) path.getLastPathComponent());
	}

	public void copy() {

		//TreePath path = getSelectionPath();
		//if (path==null) return;
		//editingNode= ((TransformationConfigTreeNode)path.getLastPathComponent());     

		if (!cut)
			editingNode = setEditingNode();

		if (editingNode == null)
			return;
		String editingNodeClass = editingNode.getUserObject().getClass().getName();
		TransformationConfigTreeNode copiedNode = new TransformationConfigTreeNode("");

		if ((editingNodeClass).equals("org.ensembl.mart.builder.config.Transformation"))
			copiedNode =
				new TransformationConfigTreeNode(editingNode.toString(), new Transformation((Transformation) editingNode.getUserObject()));
		else if (editingNodeClass.equals("org.ensembl.mart.builder.config.TransformationUnit"))
			copiedNode =
				new TransformationConfigTreeNode(editingNode.toString(), new TransformationUnit((TransformationUnit) editingNode.getUserObject()));
		else if (editingNodeClass.equals("org.ensembl.mart.builder.config.Dataset"))
			copiedNode =
				new TransformationConfigTreeNode(editingNode.toString(), new Dataset((Dataset) editingNode.getUserObject()));

		TransformationConfigTreeNodeSelection ss = new TransformationConfigTreeNodeSelection(copiedNode);
		//clipboard.setContents(ss, this);
		//try to set owner as the MartEditor object so can copy and paste between trees
		frame.getBuilder().clipboardEditor.setContents(ss, (ClipboardOwner) frame.getBuilder());
	}



	public TransformationConfigTreeNode getEditingNode() {
		return editingNode;
	}

	public void paste() {
		Transferable t = frame.getBuilder().clipboardEditor.getContents(this);
		try {
			TransformationConfigTreeNode selnode =
				(TransformationConfigTreeNode) t.getTransferData(
					new DataFlavor(Class.forName("org.ensembl.mart.builder.TransformationConfigTreeNode"), "treeNode"));				
			
			
			BaseNamedConfigurationObject test = (BaseNamedConfigurationObject) selnode.getUserObject();
			TransformationConfigTreeNode dropnode = setEditingNode();
			if (dropnode == null)
				return;
			
			String result = new String();
			int insertIndex = -1;
			if (selnode.getUserObject().getClass().equals(dropnode.getUserObject().getClass())) {
				insertIndex = dropnode.getParent().getIndex(dropnode) + 1;
				dropnode = (TransformationConfigTreeNode) dropnode.getParent();
			} else {
				insertIndex = TransformationConfigTreeNode.getHeterogenousOffset(dropnode.getUserObject(), selnode.getUserObject());
			}
			//    make sure internalName is unique within its parent group
			TransformationConfigTreeNode childNode = null;
			Enumeration children = dropnode.children();
			while (children.hasMoreElements()) {
				childNode = (TransformationConfigTreeNode) children.nextElement();
				BaseNamedConfigurationObject ch = (BaseNamedConfigurationObject) childNode.getUserObject();

				BaseNamedConfigurationObject sel = (BaseNamedConfigurationObject) selnode.getUserObject();
				if (sel.getInternalName().equals(ch.getInternalName())) {
					
					//sel.setInternalName(sel.getInternalName() + "_copy");
					
					String selnodeName = selnode.getUserObject().getClass().getName();
					
					BaseNamedConfigurationObject newSel = null;// no copy constructor for abstract class
					if (selnodeName.equals("org.ensembl.mart.builder.config.TransformationUnit"))
						newSel = new Transformation((Transformation)sel);
					else if (selnodeName.equals("org.ensembl.mart.lib.config.TransformationLine"))
						newSel = new TransformationUnit((TransformationUnit)sel);
					
					newSel.setInternalName(sel.getInternalName() + "_copy");
					// need to make sure refers to a different object for multiple pastes
					selnode = new TransformationConfigTreeNode(selnode.name + "_copy",newSel);
					TransformationConfigTreeNodeSelection ss = new TransformationConfigTreeNodeSelection(selnode);
					frame.getBuilder().clipboardEditor.setContents(ss, (ClipboardOwner) frame.getBuilder());
					
					
					break;
				}
			}

			result = treemodel.insertNodeInto(selnode, dropnode, insertIndex);
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

	public void insert(BaseConfigurationObject obj, String name) throws ConfigurationException{

		TransformationConfigTreeNode parentNode = (TransformationConfigTreeNode) clickedPath.getLastPathComponent();
		TransformationConfigTreeNode newNode = new TransformationConfigTreeNode(name + "newNode", obj);

		String result =
			treemodel.insertNodeInto(
				newNode,
				parentNode,
				TransformationConfigTreeNode.getHeterogenousOffset(parentNode.getUserObject(), newNode.getUserObject()));

		if (result.startsWith("Error")) {
			JOptionPane.showMessageDialog(frame, result, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}


	public void delete() {
		TransformationConfigTreeNode node = setEditingNode();
		if (node == null)
			return;
		//TransformationConfigTreeNode node = (TransformationConfigTreeNode) clickedPath.getLastPathComponent();
		treemodel.removeNodeFromParent(node);
	}

	public void save_as() {
		dsConfig = (TransformationConfig) ((TransformationConfigTreeNode) this.getModel().getRoot()).getUserObject();

		JFileChooser fc;
		if (frame.getFileChooserPath() != null) {
			fc = new JFileChooser(frame.getFileChooserPath());
			fc.setDragEnabled(true);
			fc.setSelectedFile(frame.getFileChooserPath());
			fc.setDialogTitle("Save as");
		} else
			fc = new JFileChooser();
		XMLFileFilter filter = new XMLFileFilter();
		fc.addChoosableFileFilter(filter);
		int returnVal = fc.showSaveDialog(frame.getContentPane());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				//find a non-adaptor way
				//URLDSConfigAdaptor.StoreTransformationConfig(dsConfig, fc.getSelectedFile());
				frame.setFileChooserPath(fc.getSelectedFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void save() {
		dsConfig = (TransformationConfig) ((TransformationConfigTreeNode) this.getModel().getRoot()).getUserObject();
		try {
			if (frame.getFileChooserPath() != null){
				// find a non-adaptor way
				//URLDSConfigAdaptor.StoreTransformationConfig(dsConfig, frame.getFileChooserPath());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*	public void export() throws ConfigurationException {
		dsConfig = (TransformationConfig) ((TransformationConfigTreeNode) this.getModel().getRoot()).getUserObject();
		
		MartEditor.getDatabaseTransformationConfigUtils().storeTransformationConfiguration(
			MartEditor.getUser(),
			dsConfig.getInternalName(),
			dsConfig.getDisplayName(),
			dsConfig.getDataset(),
			dsConfig.getDescription(),
			MartEditor.getTransformationConfigXMLUtils().getDocumentForTransformationConfig(dsConfig),
			true,
			dsConfig.getType(),
			dsConfig.getVisible(),
			dsConfig.getVersion(),
			dsConfig);
	}*/

}
