/*
 * SchemaTabSet.java
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

import java.awt.Cursor;
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
import org.biomart.builder.controller.SchemaSaver;
import org.biomart.builder.model.Schema;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays a schema.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 25th April 2006
 * @since 0.1
 */
public class SchemaTabSet extends JTabbedPane {
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
    private Map schemaModifiedStatus;
    
    /**
     * The map of schemas to files.
     */
    private Map schemaFile;
    
    /**
     * Creates a new instance of SchemaTabSet
     */
    public SchemaTabSet(MartBuilder martBuilder) {
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
        
        // Now the application logic stuff.
        this.martBuilder = martBuilder;
        this.schemaModifiedStatus = new HashMap();
        this.schemaFile = new HashMap();
    }
    
    /**
     * Retrieves the parent MartBuilder.
     */
    public MartBuilder getMartBuilder() {
        return this.martBuilder;
    }
    
    /**
     * Returns the current schema.
     */
    public Schema getCurrentSchema() {
        if (this.getSelectedComponent()!=null) return ((WindowTabSet)this.getSelectedComponent()).getSchema();
        else return null;
    }
    
    /**
     * If any schema is modified, confirm they really want to do it.
     */
    public boolean confirmCloseAllSchemas() {
        for (Iterator i =  this.schemaModifiedStatus.values().iterator(); i.hasNext(); ) {
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
    public void confirmCloseSchema() {
        boolean canClose = true;
        Schema currentSchema = this.getCurrentSchema();
        if (currentSchema == null) return;
        if (this.schemaModifiedStatus.get(currentSchema).equals(Boolean.TRUE)) {
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
            this.schemaModifiedStatus.remove(currentSchema);
            this.schemaFile.remove(currentSchema);
        }
    }
    
    /**
     * Sets the current modified status.
     * @param status true for modified, false for unmodified.
     */
    public void setModifiedStatus(boolean status) {
        Schema currentSchema = this.getCurrentSchema();
        this.schemaModifiedStatus.put(this.getCurrentSchema(), Boolean.valueOf(status));
        this.setTitleAt(this.getSelectedIndex(), this.getTabName(currentSchema));
    }
    
    /**
     * Gets a tab name based on a filename.
     */
    private String getTabName(Schema schema) {
        File filename = (File)this.schemaFile.get(schema);
        String basename = BuilderBundle.getString("unsavedSchema");
        if (filename!=null) basename = filename.getName();
        return basename + (this.schemaModifiedStatus.get(schema).equals(Boolean.TRUE) ? " *" : "");
    }
    
    /**
     * Sets up a schema.
     */
    private void addSchemaTab(Schema newSchema, File schemaFile, boolean initialState) {
        this.schemaFile.put(newSchema, schemaFile);
        this.schemaModifiedStatus.put(newSchema, Boolean.valueOf(initialState));
        this.addTab(this.getTabName(newSchema), new WindowTabSet(this, newSchema));
        this.setSelectedIndex(this.getTabCount()-1); // Select the newest tab.
    }
    
    /**
     * Creates a new schema.
     */
    public void newSchema() {
        this.addSchemaTab(new Schema(), null, true);
    }
    
    /**
     * Saves the schema to the current file.
     */
    public void saveSchema() {
        Schema currentSchema = this.getCurrentSchema();
        if (currentSchema == null) return;
        if (this.schemaFile.get(currentSchema) == null) this.saveSchemaAs();
        else {
            try {
                SchemaSaver.save(currentSchema, (File)this.schemaFile.get(currentSchema));
                this.setModifiedStatus(false);
            } catch (Throwable t) {
                this.martBuilder.showStackTrace(t);
            }
        }
    }
    
    /**
     * Saves the schema to a user-specified file.
     */
    public void saveSchemaAs() {
        Schema currentSchema = this.getCurrentSchema();
        if (currentSchema == null) return;
        if (this.xmlFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveAsFile = this.xmlFileChooser.getSelectedFile();
            // Skip the rest if they cancelled the save box.
            if (saveAsFile != null) {
                this.schemaFile.put(currentSchema, saveAsFile);
                this.saveSchema();
            }
        }
    }
    
    /**
     * Loads a schema from a user-specified file.
     */
    public void loadSchema() {
        if (this.xmlFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File loadFile = this.xmlFileChooser.getSelectedFile();
            if (loadFile != null) {
                try {
                    this.addSchemaTab(SchemaSaver.load(loadFile), loadFile, false);
                } catch (Throwable t) {
                    this.martBuilder.showStackTrace(t);
                }
            }
        }
    }
    
    /**
     * Synchronises the schema.
     */
    public void synchroniseSchema() {
        Schema currentSchema = this.getCurrentSchema();
        if (currentSchema == null) return;
        try {
            this.getCurrentSchema().synchroniseTableProviders();
            ((WindowTabSet)this.getSelectedComponent()).synchroniseTabs();
            this.setModifiedStatus(true);
        } catch (Throwable t) {
            this.martBuilder.showStackTrace(t);
        }
    }
    
    /**
     * Construct a context menu for a given window view tab.
     * @param window the window to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getSchemaTabContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem close = new JMenuItem(BuilderBundle.getString("closeSchemaTitle"));
        close.setMnemonic(BuilderBundle.getString("closeSchemaMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmCloseSchema();
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
                this.getSchemaTabContextMenu().show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            }
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
}