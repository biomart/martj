package org.ensembl.mart.vieweditor;

/**
 * Created by Katerina Tzouvara
 * Date: 12-Nov-2003
 * Time: 14:04:08
 */


import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;

public class DatasetTransferable implements Transferable {
	public DatasetTransferable(File[] files) {
		fileList = new ArrayList();
		for (int i = 0; i < files.length; i++) {
			fileList.add(files[i]);
		}
	}

	// Implementation of the Transferable interface
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.javaFileListFlavor };
	}

	public boolean isDataFlavorSupported(DataFlavor fl) {
		return fl.equals(DataFlavor.javaFileListFlavor);
	}

	public Object getTransferData(DataFlavor fl) {
		if (!isDataFlavorSupported(fl)) {
			return null;
		}

		return fileList;
	}

	List fileList;		// The list of files
}
