package org.ensembl.mart.vieweditor;

import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;


public class XMLFileFilter extends FileFilter {

    //Accept all directories and all xml files.
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = ".xml";
        if (extension != null) {
            if (f.getName().endsWith(extension) ) {
                    return true;
            } else {
                return false;
            }
        }

        return false;
    }

    //The description of this filter
    public String getDescription() {
        return ".xml";
    }
}


