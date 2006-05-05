/*
 * MartTabSet.java
 *
 * Created on 21 April 2006, 08:28
 */

/*
        Copyright (C) 2006 EBI
 
        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Lesser General Public
        License as published by the Free Software Foundation; either
        version 2.1 of the License, or (at your option) any later version.
 
        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the itmplied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Lesser General Public License for more details.
 
        You should have received a copy of the GNU Lesser General Public
        License along with this library; if not, write to the Free Software
        Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.builder.view.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;
import org.biomart.builder.controller.MartBuilderXML;
import org.biomart.builder.model.Mart;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays a schema.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 5th May 2006
 * @since 0.1
 */
public class MartTabSet extends JTabbedPane {
    /**
     * Reference to the parent MartBuilder.
     */
    private MartBuilder martBuilder;
    
    /**
     * A file chooser for XML files.
     */
    private JFileChooser xmlFileChooser;
    
    /**
     * The modified status of the schema.
     */
    private Map martModifiedStatus;
    
    /**
     * The map of schemas to files.
     */
    private Map martXMLFile;
    
    /**
     * Creates a new instance of MartTabSet
     */
    public MartTabSet(MartBuilder martBuilder) {
        // GUI stuff first.
        super();
        // Create the file chooser.
        this.xmlFileChooser = new JFileChooser();
        this.xmlFileChooser.setFileFilter(new FileFilter(){
            /**
             * {@inheritDoc}
             * <p>Accepts only files ending in ".xml".</p>
             */
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().endsWith(".xml"));
            }
            
            /**
             * {@inheritDoc}
             */
            public String getDescription() {
                return BuilderBundle.getString("XMLFileFilterDescription");
            }
        });
        this.xmlFileChooser.setMultiSelectionEnabled(true);
        
        // Now the application logic stuff.
        this.martBuilder = martBuilder;
        this.martModifiedStatus = new HashMap();
        this.martXMLFile = new HashMap();
    }
    
    /**
     * Retrieves the parent MartBuilder.
     */
    public MartBuilder getMartBuilder() {
        return this.martBuilder;
    }
    
    /**
     * Returns the current schema's window tabset.
     */
    public DataSetTabSet getSelectedDataSetTabSet() {
        if (this.getSelectedComponent()!=null) return (DataSetTabSet)this.getSelectedComponent();
        else return null;
    }
    
    /**
     * If any schema is modified, confirm they really want to do it.
     */
    public boolean confirmCloseAllMarts() {
        for (Iterator i =  this.martModifiedStatus.values().iterator(); i.hasNext(); ) {
            if (i.next().equals(Boolean.TRUE)) {
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        BuilderBundle.getString("okToCloseAll"),
                        BuilderBundle.getString("questionTitle"),
                        JOptionPane.YES_NO_OPTION
                        );
                return choice == JOptionPane.YES_OPTION;
            }
        }
        return true;
    }
    
    /**
     * If the schema is modified, asks the user for confirmation whether to
     * close it without saving or not.
     */
    public void confirmCloseMart() {
        boolean canClose = true;
        if (this.getSelectedDataSetTabSet() == null) return;
        Mart currentMart = this.getSelectedDataSetTabSet().getMart();
        if (this.martModifiedStatus.get(currentMart).equals(Boolean.TRUE)) {
            // Modified, so must confirm action first.
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    BuilderBundle.getString("okToClose"),
                    BuilderBundle.getString("questionTitle"),
                    JOptionPane.YES_NO_OPTION
                    );
            canClose = (choice == JOptionPane.YES_OPTION);
        }
        if (canClose) {
            this.removeTabAt(this.indexOfComponent(this.getSelectedComponent()));
            this.martModifiedStatus.remove(currentMart);
            this.martXMLFile.remove(currentMart);
        }
    }
    
    /**
     * Sets the current modified status.
     * @param status true for modified, false for unmodified.
     */
    public void setModifiedStatus(boolean status) {
        if (this.getSelectedDataSetTabSet() == null) return;
        Mart currentMart = this.getSelectedDataSetTabSet().getMart();
        this.martModifiedStatus.put(currentMart, Boolean.valueOf(status));
        this.setTitleAt(this.getSelectedIndex(), this.suggestTabName(currentMart));
    }
    
    /**
     * Gets a tab name based on a filename.
     */
    private String suggestTabName(Mart mart) {
        File filename = (File)this.martXMLFile.get(mart);
        String basename = BuilderBundle.getString("unsavedMart");
        if (filename!=null) basename = filename.getName();
        return basename + (this.martModifiedStatus.get(mart).equals(Boolean.TRUE) ? " *" : "");
    }
    
    /**
     * Sets up a schema.
     */
    private void addMartTab(Mart mart, File martXMLFile, boolean initialState) {
        this.martXMLFile.put(mart, martXMLFile);
        this.martModifiedStatus.put(mart, Boolean.valueOf(initialState));
        this.addTab(this.suggestTabName(mart), new DataSetTabSet(this, mart));
        this.setSelectedIndex(this.getTabCount()-1); // Select the newest tab.
    }
    
    /**
     * Creates a new schema.
     */
    public void requestNewMart() {
        this.addMartTab(new Mart(), null, true);
    }
    
    /**
     * Saves the schema to the current file.
     */
    public void saveMart() {
        if (this.getSelectedDataSetTabSet() == null) return;
        final Mart currentMart = this.getSelectedDataSetTabSet().getMart();
        if (this.martXMLFile.get(currentMart) == null) this.saveMartAs();
        else {
            LongProcess.run(this, new Runnable() {
                public void run() {
                    try {
                        MartBuilderXML.save(currentMart, (File)martXMLFile.get(currentMart));
                        setModifiedStatus(false);
                    } catch (Throwable t) {
                        martBuilder.showStackTrace(t);
                    }
                }
            });
        }
    }
    
    /**
     * Saves the schema to a user-specified file.
     */
    public void saveMartAs() {
        if (this.getSelectedDataSetTabSet() == null) return;
        Mart currentMart = this.getSelectedDataSetTabSet().getMart();
        if (this.xmlFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveAsFile = this.xmlFileChooser.getSelectedFile();
            // Skip the rest if they cancelled the save box.
            if (saveAsFile != null) {
                this.martXMLFile.put(currentMart, saveAsFile);
                this.saveMart();
            }
        }
    }
    
    /**
     * Loads a schema from a user-specified file.
     */
    public void loadMart() {
        if (this.xmlFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File[] loadFiles = this.xmlFileChooser.getSelectedFiles();
            if (loadFiles != null) {
                LongProcess.run(this, new Runnable() {
                    public void run() {
                        try {
                            for (int i = 0; i < loadFiles.length; i++) {
                                addMartTab(MartBuilderXML.load(loadFiles[i]), loadFiles[i], false);
                            }
                        } catch (Throwable t) {
                            martBuilder.showStackTrace(t);
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Construct a context menu for a given window view tab.
     * @param window the window to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getMartTabContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem close = new JMenuItem(BuilderBundle.getString("closeMartTitle"));
        close.setMnemonic(BuilderBundle.getString("closeMartMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmCloseMart();
            }
        });
        contextMenu.add(close);
        
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.isPopupTrigger()) {
            // Where was the click?
            int selectedIndex = this.indexAtLocation(evt.getX(), evt.getY());
            if (selectedIndex >= 0) {
                // Respond appropriately.
                this.setSelectedIndex(selectedIndex);
                this.getMartTabContextMenu().show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            }
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
}