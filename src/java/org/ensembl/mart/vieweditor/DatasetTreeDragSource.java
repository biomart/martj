package org.ensembl.mart.vieweditor;

/**
 * Created by Katerina Tzouvara.
 * Date: 12-Nov-2003
 * Time: 13:28:42
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.*;

public class DatasetTreeDragSource implements DragGestureListener,
								DragSourceListener {
	public DatasetTreeDragSource(DatasetViewTree2 tree) {
		this.tree = tree;

		// Use the default DragSource
		DragSource dragSource = DragSource.getDefaultDragSource();

		// Create a DragGestureRecognizer and
		// register as the listener
		dragSource.createDefaultDragGestureRecognizer(
						tree, DnDConstants.ACTION_COPY_OR_MOVE,
						this);
	}

	// Implementation of DragGestureListener interface.
	public void dragGestureRecognized(DragGestureEvent dge) {
		// Get the mouse location and convert it to
		// a location within the tree.
		Point location = dge.getDragOrigin();
		TreePath dragPath = tree.getPathForLocation(location.x, location.y);
		if (dragPath != null && tree.isPathSelected(dragPath)) {
			// Get the list of selected files and create a Transferable
			// The list of files and the is saved for use when
			// the drop completes.
			paths = tree.getSelectionPaths();
			if (paths != null && paths.length > 0) {
				dragFiles = new File[paths.length];
				for (int i = 0; i < paths.length; i++) {
					String pathName = tree.getPathName(paths[i]);
					dragFiles[i] = new File(pathName);
				}

				Transferable transferable =
							new DatasetTransferable(dragFiles);
				dge.startDrag(null, transferable, this);
			}
		}
	}

	// Implementation of DragSourceListener interface
	public void dragEnter(DragSourceDragEvent dsde) {
		DnDUtils.debugPrintln("Drag Source: dragEnter, drop action = "
						+ DnDUtils.showActions(dsde.getDropAction()));
	}

	public void dragOver(DragSourceDragEvent dsde) {
		DnDUtils.debugPrintln("Drag Source: dragOver, drop action = "
						+ DnDUtils.showActions(dsde.getDropAction()));
	}

	public void dragExit(DragSourceEvent dse) {
		DnDUtils.debugPrintln("Drag Source: dragExit");
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
		DnDUtils.debugPrintln("Drag Source: dropActionChanged, drop action = "
						+ DnDUtils.showActions(dsde.getDropAction()));
	}

	public void dragDropEnd(DragSourceDropEvent dsde) {
		DnDUtils.debugPrintln("Drag Source: drop completed, drop action = "
						+ DnDUtils.showActions(dsde.getDropAction())
						+ ", success: " + dsde.getDropSuccess());
		// If the drop action was ACTION_MOVE,
		// the tree might need to be updated.
		if (dsde.getDropAction() == DnDConstants.ACTION_MOVE) {
			final File[] draggedFiles = dragFiles;
			final TreePath[] draggedPaths = paths;

			Timer tm = new Timer(200, new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					// Check whether each of the dragged files exists.
					// If it does not, we need to remove the node
					// that represents it from the tree.
					for (int i = 0; i < draggedFiles.length; i++) {
						if (draggedFiles[i].exists() == false) {
							// Remove this node
							DefaultMutableTreeNode node =
								(DefaultMutableTreeNode)draggedPaths[i].
										getLastPathComponent();
							((DefaultTreeModel)tree.getModel()).
											removeNodeFromParent(node);
						}
					}
				}
			});
			tm.setRepeats(false);
			tm.start();
		}
	}



	protected DatasetViewTree2 tree;			// The associated tree
	protected File[] dragFiles;			// Dragged files
	protected TreePath[] paths;			// Dragged paths
}


