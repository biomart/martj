package org.ensembl.mart.vieweditor;

/**
 * Created Katerina Tzouvara.
 * Date: 12-Nov-2003
 * Time: 13:25:51
 */


import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class DatasetTreeDropTarget implements DropTargetListener,
									PropertyChangeListener {
	public DatasetTreeDropTarget(DatasetViewTree tree) {
		this.tree = tree;

		// Listen for changes in the enabled property
		tree.addPropertyChangeListener(this);

		// Create the DropTarget and register
		// it with the FileTree.
		dropTarget = new DropTarget(tree,
									DnDConstants.ACTION_COPY_OR_MOVE,
									this,
									tree.isEnabled(), null);
	}

	// Implementation of the DropTargetListener interface
	public void dragEnter(DropTargetDragEvent dtde) {
		DnDUtils.debugPrintln("dragEnter, drop action = "
						+ DnDUtils.showActions(dtde.getDropAction()));

		// Save the list of selected items
		saveTreeSelection();

		// Get the type of object being transferred and determine
		// whether it is appropriate.
		checkTransferType(dtde);

		// Accept or reject the drag.
		boolean acceptedDrag = acceptOrRejectDrag(dtde);

		// Do drag-under feedback
		dragUnderFeedback(dtde, acceptedDrag);
	}

	public void dragExit(DropTargetEvent dte) {
		DnDUtils.debugPrintln("DropTarget dragExit");

		// Do drag-under feedback
		dragUnderFeedback(null, false);

		// Restore the original selections
		restoreTreeSelection();
	}

	public void dragOver(DropTargetDragEvent dtde) {
		DnDUtils.debugPrintln("DropTarget dragOver, drop action = "
						+ DnDUtils.showActions(dtde.getDropAction()));

		// Accept or reject the drag
		boolean acceptedDrag = acceptOrRejectDrag(dtde);

		// Do drag-under feedback
		dragUnderFeedback(dtde, acceptedDrag);
	}

	public void dropActionChanged(DropTargetDragEvent dtde) {
		DnDUtils.debugPrintln("DropTarget dropActionChanged, drop action = "
						+ DnDUtils.showActions(dtde.getDropAction()));

		// Accept or reject the drag
		boolean acceptedDrag = acceptOrRejectDrag(dtde);

		// Do drag-under feedback
		dragUnderFeedback(dtde, acceptedDrag);
	}

	public void drop(DropTargetDropEvent dtde) {
		DnDUtils.debugPrintln("DropTarget drop, drop action = "
						+ DnDUtils.showActions(dtde.getDropAction()));

		// Check the drop action
		if ((dtde.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0) {
			// Accept the drop and get the transfer data
			dtde.acceptDrop(dtde.getDropAction());
			Transferable transferable = dtde.getTransferable();
			boolean dropSucceeded = false;

			try {
				tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				// Save the user's selections
				saveTreeSelection();

				dropSucceeded = dropFile(dtde.getDropAction(),
								transferable, dtde.getLocation());

				DnDUtils.debugPrintln("Drop completed, success: "
									  + dropSucceeded);
			} catch (Exception e) {
				DnDUtils.debugPrintln("Exception while handling drop " + e);
			} finally {
				tree.setCursor(Cursor.getDefaultCursor());

				// Restore the user's selections
				restoreTreeSelection();
				dtde.dropComplete(dropSucceeded);
			}
		} else {
			DnDUtils.debugPrintln("Drop target rejected drop");
			dtde.dropComplete(false);
		}
	}

	// PropertyChangeListener interface
	public void propertyChange(PropertyChangeEvent evt) {
		String propertyName = evt.getPropertyName();
		if (propertyName.equals("enabled")) {
			// Enable the drop target if the FileTree is enabled
			// and vice versa.
			dropTarget.setActive(tree.isEnabled());
		}
	}

	// Internal methods start here

	protected boolean acceptOrRejectDrag(DropTargetDragEvent dtde) {
		int dropAction = dtde.getDropAction();
		int sourceActions = dtde.getSourceActions();
		boolean acceptedDrag = false;

		DnDUtils.debugPrintln("\tSource actions are " +
							DnDUtils.showActions(sourceActions) +
							", drop action is " +
							DnDUtils.showActions(dropAction));

		Point location = dtde.getLocation();
		boolean acceptableDropLocation = isAcceptableDropLocation(location);

		// Reject if the object being transferred
		// or the operations available are not acceptable.
		if (!acceptableType ||
			(sourceActions & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
			DnDUtils.debugPrintln("Drop target rejecting drag");
			dtde.rejectDrag();
		} else if (!tree.isEditable()) {
			// Can't drag to a read-only FileTree
			DnDUtils.debugPrintln("Drop target rejecting drag");
			dtde.rejectDrag();
		} else if (!acceptableDropLocation) {
			// Can only drag to writable directory
			DnDUtils.debugPrintln("Drop target rejecting drag");
			dtde.rejectDrag();
		} else if ((dropAction & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
			// Not offering copy or move - suggest a copy
			DnDUtils.debugPrintln("Drop target offering COPY");
			dtde.acceptDrag(DnDConstants.ACTION_COPY);
			acceptedDrag = true;
		} else {
			// Offering an acceptable operation: accept
			DnDUtils.debugPrintln("Drop target accepting drag");
			dtde.acceptDrag(dropAction);
			acceptedDrag = true;
		}

		return acceptedDrag;
	}

	protected void dragUnderFeedback(DropTargetDragEvent dtde,
									boolean acceptedDrag) {
		if (dtde != null && acceptedDrag) {
			Point location = dtde.getLocation();
			if (isAcceptableDropLocation(location)) {
				tree.setSelectionRow(
					tree.getRowForLocation(location.x, location.y));
			} else {
				tree.clearSelection();
			}
		} else {
			tree.clearSelection();
		}
	}

	protected void checkTransferType(DropTargetDragEvent dtde) {
		// Accept a list of files
		acceptableType = false;
		if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			acceptableType = true;
		}
		DnDUtils.debugPrintln("Data type acceptable - " + acceptableType);
	}

	// This method handles a drop for a list of files
	protected boolean dropFile(int action,
							   Transferable transferable, Point location)
							throws IOException, UnsupportedFlavorException,
								MalformedURLException {
		List files = (List)transferable.getTransferData(
								DataFlavor.javaFileListFlavor);
		TreePath treePath = tree.getPathForLocation(
								location.x, location.y);
		File targetDirectory = findTargetDirectory(location);
		if (treePath == null || targetDirectory == null) {
			return false;
		}
		DatasetViewTree.DatasetTreeNode node = (DatasetViewTree.DatasetTreeNode)treePath.getLastPathComponent();

		// Highlight the drop location while we perform the drop
		tree.setSelectionPath(treePath);

		// Get File objects for all files being
		// transferred, eliminating duplicates.
		File[] fileList = getFileList(files);

		// Don't overwrite files by default
		copyOverExistingFiles = false;

		// Copy or move each source object to the target
		for (int i = 0; i < fileList.length; i++) {
			File f = fileList[i];
			if (f.isDirectory()) {
				transferDirectory(action, f, targetDirectory, node);
			} else {
				try {
					transferFile(action, fileList[i],
								 targetDirectory, node);
				} catch (IllegalStateException e) {
					// Cancelled by user
					return false;
				}
			}
		}

		return true;
	}

	protected File findTargetDirectory(Point location) {
		TreePath treePath = tree.getPathForLocation(location.x, location.y);
		if(treePath != null) {
			DatasetViewTree.DatasetTreeNode node =
					(DatasetViewTree.DatasetTreeNode)treePath.getLastPathComponent();
			// Only allow a drop on a writable directory
			if (node.isFolder()) {
				try {
					File f = new File(node.getFullName());
					if (f.canWrite()) {
						return f;
					}
				} catch (Exception e) {
				}
			}
		}
		return null;
	}

	protected boolean isAcceptableDropLocation(Point location) {
		return findTargetDirectory(location) != null;
	}

	protected void saveTreeSelection() {
		selections = tree.getSelectionPaths();
		leadSelection = tree.getLeadSelectionPath();
		tree.clearSelection();
	}

	protected void restoreTreeSelection() {
		tree.setSelectionPaths(selections);

		// Restore the lead selection
		if (leadSelection != null) {
			tree.removeSelectionPath(leadSelection);
			tree.addSelectionPath(leadSelection);
		}
	}

	// Get the list of files being transferred and
	// remove any duplicates. For example, if the
	// list contains /a/b/c and /a/b/c/d, the
	// second entry is removed.
	protected File[] getFileList(List files) {
		int size = files.size();

		// Get the files into an array for sorting
		File[] f = new File[size];
		Iterator iter = files.iterator();
		int count = 0;
		while (iter.hasNext()) {
			f[count++] = (File)iter.next();
		}

		// Sort the files into alphabetical order
		// based on pathnames.
		Arrays.sort(f, new Comparator() {
			public boolean equals(Object o1) {
				return false;
			}

			public int compare(Object o1, Object o2) {
				return ((File)o1).getAbsolutePath().compareTo(
								((File)o2).getAbsolutePath());
			}
		});

		// Remove duplicates, retaining the results in a Vector
		Vector v = new Vector();
		char separator = System.getProperty("file.separator").charAt(0);
outer:
		for (int i = f.length - 1 ; i >= 0; i--) {
			String secondPath = f[i].getAbsolutePath();
			int secondLength = secondPath.length();
			for (int j = i - 1 ; j >= 0; j--) {
				String firstPath = f[j].getAbsolutePath();
				int firstLength = firstPath.length();
				if (secondPath.startsWith(firstPath)
					&& firstLength != secondLength
					&& secondPath.charAt(firstLength) == separator) {
					continue outer;
				}
			}
			v.add(f[i]);
		}

		// Copy the retained files into an array
		f = new File[v.size()];
		v.copyInto(f);

		return f;
	}

	// Copy or move a file
	protected void transferFile(int action, File srcFile,
								File targetDirectory,
								DatasetViewTree.DatasetTreeNode targetNode) {
		DnDUtils.debugPrintln(
						(action == DnDConstants.ACTION_COPY ? "Copy" : "Move") +
						" file " + srcFile.getAbsolutePath() +
						" to " + targetDirectory.getAbsolutePath());

		// Create a File entry for the target
		String name = srcFile.getName();
		File newFile = new File(targetDirectory, name);
		if (newFile.exists()) {
			// Already exists - is it the same file?
			if (newFile.equals(srcFile)) {
				// Exactly the same file - ignore
				return;
			}
			// File of this name exists in this directory
			if (copyOverExistingFiles == false) {
				int res = JOptionPane.showOptionDialog(tree,
							"A file called\n   " + name +
							"\nalready exists in the directory\n   " +
							targetDirectory.getAbsolutePath() +
							"\nOverwrite it?",
							"File Exists",
							JOptionPane.DEFAULT_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null, new String[] {
								"Yes", "Yes to All", "No", "Cancel"
							},
							"No");
				switch (res) {
				case 1:	// Yes to all
					copyOverExistingFiles = true;
				case 0:	// Yes
					break;
				case 2:	// No
					return;
				default: // Cancel
					throw new IllegalStateException("Cancelled");
				}
			}
		} else {
			// New file - create it
			try {
				newFile.createNewFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(tree,
					"Failed to create new file\n  " +
					newFile.getAbsolutePath(),
					"File Creation Failed",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// Copy the data and close file.
		BufferedInputStream is = null;
		BufferedOutputStream os = null;

		try {
			is = new BufferedInputStream(
						new FileInputStream(srcFile));
			os = new BufferedOutputStream(
						new FileOutputStream(newFile));
			int size = 4096;
			byte[] buffer = new byte[size];
			int len;
			while ((len = is.read(buffer, 0, size)) > 0) {
				os.write(buffer, 0, len);
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(tree,
					"Failed to copy file\n  " +
					name + "\nto directory\n  " +
					targetDirectory.getAbsolutePath(),
					"File Copy Failed",
					JOptionPane.ERROR_MESSAGE);
			return;
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
			}
		}

		// Remove the source if this is a move operation.
		if (action == DnDConstants.ACTION_MOVE &&
			System.getProperty("DnDExamples.allowRemove") != null) {
			srcFile.delete();
		}

		// Update the tree display
		if (targetNode != null) {
			tree.addNode(targetNode, name);
		}
	}

	protected void transferDirectory(int action, File srcDir,
								File targetDirectory,
								DatasetViewTree.DatasetTreeNode targetNode) {
		DnDUtils.debugPrintln(
						(action == DnDConstants.ACTION_COPY ? "Copy" : "Move") +
						" directory " + srcDir.getAbsolutePath() +
						" to " + targetDirectory.getAbsolutePath());

		// Do not copy a directory into itself or
		// a subdirectory of itself.
		File parentDir = targetDirectory;
		while (parentDir != null) {
			if (parentDir.equals(srcDir)) {
				DnDUtils.debugPrintln("-- SUPPRESSED");
				return;
			}
			parentDir = parentDir.getParentFile();
		}

		// Copy the directory itself, then its contents

		// Create a File entry for the target
		String name = srcDir.getName();
		File newDir = new File(targetDirectory, name);
		if (newDir.exists()) {
			// Already exists - is it the same directory?
			if (newDir.equals(srcDir)) {
				// Exactly the same file - ignore
				return;
			}
		} else {
			// Directory does not exist - create it
			if (newDir.mkdir() == false) {
				// Failed to create - abandon this directory
				JOptionPane.showMessageDialog(tree,
					"Failed to create target directory\n  " +
					newDir.getAbsolutePath(),
					"Directory creation Failed",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// Add a node for the new directory
		if (targetNode != null) {
			targetNode = tree.addNode(targetNode, name);
		}

		// Now copy the directory content.
		File[] files = srcDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isFile()) {
				transferFile(action, f, newDir, targetNode);
			} else if (f.isDirectory()) {
				transferDirectory(action, f, newDir, targetNode);
			}
		}

		// Remove the source directory after moving
		if (action == DnDConstants.ACTION_MOVE &&
			System.getProperty("DnDExamples.allowRemove") != null) {
			srcDir.delete();
		}
	}

	/*public static void main(String[] args) {
		final JFrame f = new JFrame("FileTree Drop Target Example");

		try {

			final DatasetViewTree tree = new DatasetViewTree(args[0]);
			 DatasetTreeDragSource dragSource = new DatasetTreeDragSource(tree);
			// Add a drop target to the FileTree
			DatasetTreeDropTarget target = new DatasetTreeDropTarget(tree);

			tree.setEditable(true);

			f.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent evt) {
					System.exit(0);
				}
			});

			JPanel panel = new JPanel();
			final JCheckBox editable = new JCheckBox("Editable");
			editable.setSelected(true);
			panel.add(editable);
			editable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					tree.setEditable(editable.isSelected());
				}
			});


			final JCheckBox enabled = new JCheckBox("Enabled");
			enabled.setSelected(true);
			panel.add(enabled);
			enabled.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					tree.setEnabled(enabled.isSelected());
				}
			});

			f.getContentPane().add(new JScrollPane(tree), BorderLayout.CENTER);
			f.getContentPane().add(panel, BorderLayout.SOUTH);
			f.setSize(500, 400);
			f.setVisible(true);
		} catch (Exception e) {
			System.out.println("Failed to build GUI: " + e);
		}
	}                         */

	protected DatasetViewTree tree;
	protected DropTarget dropTarget;
	protected boolean acceptableType;	// Indicates whether data is acceptable
	TreePath[] selections;				// Initially selected rows
	TreePath leadSelection;				// Initial lead selection
	boolean copyOverExistingFiles;
}

