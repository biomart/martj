package org.ensembl.mart.vieweditor;

/**
 * Created by Katerina Tzouvara
 * Date: 12-Nov-2003
 * Time: 12:02:50
 */

import org.ensembl.mart.lib.config.DatasetView;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.*;
import java.util.*;

public class DatasetTree extends JTree implements Autoscroll {
	public static final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
	protected Insets scrollInsets = defaultScrollInsets;

	public DatasetTree(DatasetView dsView) throws FileNotFoundException, SecurityException {
		super((TreeModel)null);			// Create the JTree itself

		// Use horizontal and vertical lines
		putClientProperty("JTree.lineStyle", "Angled");

		// Create the first node
		DatasetTreeNode rootNode = new DatasetTreeNode(null, dsView);

		// Populate the root node with its subfolders
		boolean addedNodes = rootNode.populateFolders(true);
		setModel(new DefaultTreeModel(rootNode));

		// Listen for Tree Selection Events
		addTreeExpansionListener(new TreeExpansionHandler());
	}

	// Returns the full pathname for a path, or null if not a known path
	public String getPathName(TreePath path) {
		Object o = path.getLastPathComponent();
		if (o instanceof DatasetTreeNode) {
			return ((DatasetTreeNode)o).fullName;
		}
		return null;
	}

	// Adds a new node to the tree after construction.
	// Returns the inserted node, or null if the parent
	// directory has not been expanded.
	public DatasetTreeNode addNode(DatasetTreeNode parent, String name) {
		int index = parent.addNode(name);
		if (index != -1) {
			((DefaultTreeModel)getModel()).nodesWereInserted(
							parent, new int[] { index });
			return (DatasetTreeNode)parent.getChildAt(index);
		}

		// No node was created
		return null;
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
				(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
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

	// Inner class that represents a node in this file system tree
	public static class DatasetTreeNode extends DefaultMutableTreeNode {

        protected String name;		// Name of this component
		protected String internalName;	// Full name
		protected boolean populated;// true if we have been populated
		protected boolean interim;	// true if we are in interim state
		protected boolean isFolder;	// true if this is a directory

		public DatasetTreeNode(String parent, String name) throws SecurityException,
															FileNotFoundException {
			this.name = name;

			// See if this node exists and whether it is a folder
			fullName = parent == null ? name : parent + File.separator + name;

			File f = new File(fullName);
			if (f.exists() == false) {
				throw new FileNotFoundException("File " + fullName + " does not exist");
			}

			isFolder = f.isFolder();

			// Hack for Windows which doesn't consider a drive to be a directory!
			if (isDir == false && f.isFile() == false) {
				isDir = true;
			}
		}

		// Override isLeaf to check whether this is a directory
		public boolean isLeaf() {
			return !isFolder;
		}

		// Override getAllowsChildren to check whether this is a directory
		public boolean getAllowsChildren() {
			return isFolder;
		}

		// Return whether this is a directory
		public boolean isFolder() {
			return isFolder;
		}

		// Get full path
		public String getFullName() {
			return fullName;
		}

		// For display purposes, we return our own name
		public String toString() {
			return name;
		}

		// If we are a directory, scan our contents and populate
		// with children. In addition, populate those children
		// if the "descend" flag is true. We only descend once,
		// to avoid recursing the whole subtree.
		// Returns true if some nodes were added
		boolean populateFolders(boolean descend) {
			boolean addedNodes = false;

			// Do this only once
			if (populated == false) {
				File f;
				try {
					f = new File(fullName);
				} catch (SecurityException e) {
					populated = true;
					return false;
				}

				if (interim == true) {
					// We have had a quick look here before:
					// remove the dummy node that we added last time
					removeAllChildren();
					interim = false;
				}

				String[] names = f.list();		// Get list of contents

				// Process the contents
				ArrayList list = new ArrayList();
				for (int i = 0; i < names.length; i++) {
					String name = names[i];
					File d = new File(fullName, name);
					try {
						DatasetTreeNode node = new DatasetTreeNode(fullName, name);
						list.add(node);
						if (descend && d.isDirectory()) {
							node.populateFolders(false);
						}
						addedNodes = true;
						if (descend == false) {
							// Only add one node if not descending
							break;
						}
					} catch (Throwable t) {
						// Ignore phantoms or access problems
					}
				}

				if (addedNodes == true) {
					// Now sort the list of contained files and directories
					Object[] nodes = list.toArray();
					Arrays.sort(nodes, new Comparator() {
						public boolean equals(Object o) {
							return false;
						}

						public int compare(Object o1, Object o2) {
							DatasetTreeNode node1 = (DatasetTreeNode)o1;
							DatasetTreeNode node2 = (DatasetTreeNode)o2;

							// Directories come first
							if (node1.isDir != node2.isDir) {
								return node1.isDir ? -1 : +1;
							}

							// Both directories or both files -
							// compare based on pathname
							return node1.fullName.compareTo(node2.fullName);
						}
					});

					// Add sorted items as children of this node
					for (int j = 0; j < nodes.length; j++) {
						this.add((DatasetTreeNode)nodes[j]);
					}
				}

				// If we were scanning to get all subdirectories,
				// or if we found no content, there is no
				// reason to look at this directory again, so
				// set populated to true. Otherwise, we set interim
				// so that we look again in the future if we need to
				if (descend == true || addedNodes == false) {
					populated = true;
				} else {
					// Just set interim state
					interim = true;
				}
			}
			return addedNodes;
		}

		// Adding a new file or directory after
		// constructing the FileTree. Returns
		// the index of the inserted node.
		public int addNode(String name) {
			// If not populated yet, do nothing
			if (populated == true) {
				// Do not add a new node if
				// the required node is already there
				int childCount = getChildCount();
				for (int i = 0; i < childCount; i++) {
					DatasetTreeNode node = (DatasetTreeNode)getChildAt(i);
					if (node.name.equals(name)) {
						// Already exists - ensure
						// we repopulate
						if (node.isDir()) {
							node.interim = true;
							node.populated = false;
						}
						return -1;
					}
				}

				// Add a new node
				try {
					DatasetTreeNode node = new DatasetTreeNode(fullName, name);
					add(node);
					return childCount;
				} catch (Exception e) {
				}
			}
			return -1;
		}


	}

	// Inner class that handles Tree Expansion Events
	protected class TreeExpansionHandler implements TreeExpansionListener {
		public void treeExpanded(TreeExpansionEvent evt) {
			TreePath path = evt.getPath();			// The expanded path
			JTree tree = (JTree)evt.getSource();	// The tree

			// Get the last component of the path and
			// arrange to have it fully populated.
			DatasetTreeNode node = (DatasetTreeNode)path.getLastPathComponent();
			if (node.populateFolders(true)) {
				((DefaultTreeModel)tree.getModel()).nodeStructureChanged(node);
			}
		}

		public void treeCollapsed(TreeExpansionEvent evt) {
			// Nothing to do
		}
	}
}