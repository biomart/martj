/*
 * MartBuilder.java
 *
 * Created on 11 April 2006, 10:00
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.biomart.builder.controller.SchemaSaver;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.model.Window;
import org.biomart.builder.resources.BuilderBundle;

/**
 * The main window housing the MartBuilder GUI.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 19th April 2006
 * @since 0.1
 */
public class MartBuilder extends JFrame {
    /**
     * Internal reference to the modified status of this application.
     */
    private boolean modifiedStatus;
    
    /**
     * Internal reference to the {@link Schema} we are viewing.
     */
    private Schema schema;
    
    /**
     * Internal reference to the file the current schema was loaded from.
     */
    private File schemaFile;
    
    /**
     * Internal reference to the tabs containing the schema and windows.
     */
    private WindowTabSet windowTabSet;
    
    /**
     * Internal reference to the hint bar.
     */
    private JLabel hintBar = new JLabel("");
    
    /**
     * Internal reference to a JFileChooser for opening/saving XML files with.
     */
    private JFileChooser xmlFileChooser;
    
    /**
     * Creates a new instance of MartBuilder. Calls initComponents
     * to set up the UI.
     */
    public MartBuilder() {
        // Create the window.
        super();
        // Set the look and feel to the one specified by the user, or the system
        // default if not specified by the user.
        String lookAndFeelClass = System.getProperty("martbuilder.laf"); // null if not set
        try {
            UIManager.setLookAndFeel(lookAndFeelClass);
        } catch (Exception e) {
            // Ignore, as we'll end up with the system one if this one doesn't work.
            if (lookAndFeelClass != null) // only worry if we were actually given one.
                System.err.println(BuilderBundle.getString("badLookAndFeel", lookAndFeelClass));
            // Use system default.
            lookAndFeelClass = UIManager.getSystemLookAndFeelClassName();
            try {
                UIManager.setLookAndFeel(lookAndFeelClass);
            } catch (Exception e2) {
                // Ignore, as we'll end up with the cross-platform one if there is no system one.
                System.err.println(BuilderBundle.getString("badLookAndFeel", lookAndFeelClass));
            }
        }
        // Start work.
        this.initComponents();
        this.newSchema();
    }
    
    /**
     * Creates all the components that are part of the main window.
     */
    private void initComponents() {
        // Set up window listener and use it to handle windows closing.
        final MartBuilder mb = this;
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (e.getWindow() == mb) {
                    mb.exitNow();
                }
            }
        });
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Make a menu bar and add it.
        this.setJMenuBar(new MartBuilderMenuBar(this));
        // Set up the hint bar.
        this.hintBar.setBorder(BorderFactory.createLoweredBevelBorder());
        this.getContentPane().add(this.hintBar, BorderLayout.SOUTH);
        // Set up the schema tabs.
        this.windowTabSet = new WindowTabSet(this);
        this.getContentPane().add(this.windowTabSet, BorderLayout.CENTER);
        // Pack the window.
        this.pack();
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
    }
    
    /**
     * Display a nice friendly stack trace window.
     * @param t the throwable to display the stack trace for.
     */
    public void showStackTrace(Throwable t) {
        // Create the main message.
        final int messageClass = (t instanceof Error) ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        String mainMessage = t.getLocalizedMessage();
        // Extract the full stack trace.
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        final String stackTraceText = sw.toString();
        int choice = JOptionPane.showConfirmDialog(
                this,
                new Object[]{mainMessage, BuilderBundle.getString("stackTracePrompt")},
                BuilderBundle.getString("stackTraceTitle"),
                JOptionPane.YES_NO_OPTION);
        // Create and show the dialog.
        if (choice == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(
                    this,
                    stackTraceText,
                    BuilderBundle.getString("stackTraceTitle"),
                    messageClass);
        }
    }
    
    /**
     * Tells the application to exit.
     */
    public void exitNow() {
        if (this.confirmCloseSchema()) System.exit(0);
    }
    
    /**
     * Sets a hint.
     * @param hint the hint to display on the hint bar.
     */
    public void setHint(String hint) {
        this.hintBar.setText(hint);
    }
    
    /**
     * Resets the hint to its default value.
     */
    public void resetHint() {
        if (this.schemaFile == null) this.setHint(BuilderBundle.getString("noFileLoaded"));
        else this.setHint(BuilderBundle.getString("editingFile", this.schemaFile.toString()));
    }
    
    /**
     * Sets the current schema to be a new one. If the current schema has been modified
     * then it will confirm that the user wants to close the existing one first.
     */
    public void newSchema() {
        this.setSchema(new Schema());
        this.schemaFile = null;
        this.resetHint();
    }
    
    /**
     * Opens a file chooser, uses it to select a file, then loads the file.
     */
    public void loadSchema() {
        if (this.xmlFileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File fileChosen = this.xmlFileChooser.getSelectedFile();
        if (fileChosen != null) {
            try {
                this.loadSchema(fileChosen);
            } catch (Throwable t) {
                this.showStackTrace(t);
            }
        }
    }
    
    /**
     * Loads a schema file and attempts to parse it.
     * @param file the schema file to load.
     */
    public void loadSchema(final File file) {
        // Set the hint to say we're loading.
        this.setHint(BuilderBundle.getString("loadingFile", file.toString()));
        // Load the file in the background.
        final MartBuilder mb = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    mb.setSchema(SchemaSaver.load(file));
                    mb.schemaFile = file;
                } catch (Throwable t) {
                    mb.showStackTrace(t);
                } finally {
                    mb.resetHint();
                }
            }
        });
    }
    
    /**
     * Saves the current schema.
     * @param chooseLocation true if the user wants to choose the location, false if
     * they want to reuse the previous location. If the file did not have a previous
     * location, then this parameter is treated as true anyway.
     */
    public void saveSchema(boolean chooseLocation) {
        if (chooseLocation || this.schemaFile == null) {
            // Let the user choose a new file.
            if (this.xmlFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File fileChosen = this.xmlFileChooser.getSelectedFile();
            // Skip the rest if they cancelled the save box.
            if (fileChosen == null) return;
            // Remember the file chosen.
            else this.schemaFile = fileChosen;
        }
        // Save the schema.
        try {
            this.saveSchema(this.schemaFile);
        } catch (Throwable t) {
            this.showStackTrace(t);
        }
    }
    
    /**
     * Saves the current schema to the given file and updates the modified status.
     * @param file the file to save the current schema to.
     */
    public void saveSchema(final File file) {
        // Set the hint to say we're saving.
        this.setHint(BuilderBundle.getString("savingFile", file.toString()));
        // Save the file in the background.
        final MartBuilder mb = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    SchemaSaver.save(mb.schema, file);
                    mb.setModifiedStatus(false);
                } catch (Throwable t) {
                    mb.showStackTrace(t);
                } finally {
                    mb.resetHint();
                }
            }
        });
    }
    
    /**
     * If the schema is modified, asks the user for confirmation whether to
     * close it without saving or not. If the schema is unmodified, no action
     * is taken.
     * @return true if the schema can be closed without saving first, false if
     * the user wishes to cancel the close action.
     */
    public boolean confirmCloseSchema() {
        if (this.getModifiedStatus() == true) {
            // Modified, so must confirm action first.
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    BuilderBundle.getString("okToClose"),
                    BuilderBundle.getString("questionTitle"),
                    JOptionPane.YES_NO_OPTION
                    );
            return choice == JOptionPane.YES_OPTION;
        } else {
            // Not modified, so can close OK.
            return true;
        }
    }
    
    /**
     * Tests the specified table provider to see if it can connect.
     * It shows a dialog to tell the user the results.
     * @param tblProv the table provider to test.
     */
    public void testTableProvider(TableProvider tblProv) {
        boolean passedTest = false;
        try {
            passedTest = tblProv.test();
        } catch (Throwable t) {
            passedTest = false;
            this.showStackTrace(t);
        }
        // Tell the user what happened.
        if (passedTest) {
            JOptionPane.showMessageDialog(
                    this,
                    BuilderBundle.getString("tblProvTestPassed"),
                    BuilderBundle.getString("testTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    BuilderBundle.getString("tblProvTestFailed"),
                    BuilderBundle.getString("testTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Adds the specified table provider to the underlying schema.
     * @param tblProv the table provider to add.
     */
    public void addTableProvider(TableProvider tblProv) {
        try {
            this.schema.addTableProvider(tblProv);
            // Update the TableProviderView tabs.
            this.windowTabSet.resyncTableProviderTabs();
            // Update modified status.
            this.setModifiedStatus(true);
        } catch (Throwable t) {
            this.showStackTrace(t);
        }
    }
    
    /**
     * Removes the specified table provider from the underlying schema.
     * @param tblProv the table provider to remove.
     */
    public void removeTableProvider(final TableProvider tblProv) {
        // Must confirm action first.
        int choice = JOptionPane.showConfirmDialog(
                this,
                BuilderBundle.getString("confirmDelTblProv"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.YES_NO_OPTION
                );
        if (choice != JOptionPane.YES_OPTION) return;
        // Set hint to removing table provider
        this.setHint(BuilderBundle.getString("removingTblProv", tblProv.getName()));
        // Do this later in the background
        final MartBuilder mb = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    // Table provider may contain windows, so remove the windows first.
                    List windows = new ArrayList(mb.schema.getWindows());
                    for (Iterator i = windows.iterator(); i.hasNext(); ) {
                        Window w = (Window)i.next();
                        if (w.getCentralTable().getTableProvider().equals(tblProv)) mb.removeWindow(w, false);
                    }
                    // Now remove the table provider itself.
                    mb.schema.removeTableProvider(tblProv);
                    // Update the TableProviderView tabs.
                    mb.windowTabSet.resyncTableProviderTabs();
                    // Update modified status.
                    mb.setModifiedStatus(true);
                } catch (Throwable t) {
                    mb.showStackTrace(t);
                } finally {
                    mb.resetHint();
                }
            }
        });
    }
    
    /**
     * Creates a number of suggested windows based around a given table.
     * @param table the table to create the windows around.
     */
    public void createWindows(final Table table) {
        // Set hint to creating windows
        this.setHint(BuilderBundle.getString("creatingWindows", table.getName()));
        // Do this later in the background
        final MartBuilder mb = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Set suggestedWindows = mb.schema.suggestWindows(table);
                    // Create a new tab for each of the new windows.
                    for (Iterator i = suggestedWindows.iterator(); i.hasNext(); )
                        mb.windowTabSet.createTab((Window)i.next());
                    // Update modified status.
                    mb.setModifiedStatus(true);
                } catch (Throwable t) {
                    mb.showStackTrace(t);
                } finally {
                    mb.resetHint();
                }
            }
        });
    }
    
    /**
     * Remvoes the specified window from the underlying schema.
     * @param window the window to remove.
     * @param confirmRemove true if the user should be prompted, false if it
     * should just remove it anyway (eg. if called from another routine where
     * confirmation has already been obtained).
     */
    public void removeWindow(Window window, boolean confirmRemove) {
        if (confirmRemove) {
            // Must confirm action first.
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    BuilderBundle.getString("confirmDelDataset"),
                    BuilderBundle.getString("questionTitle"),
                    JOptionPane.YES_NO_OPTION
                    );
            if (choice != JOptionPane.YES_OPTION) return;
        }
        // Do it.
        try {
            this.schema.removeWindow(window);
            // Remove the corresponding tab.
            this.windowTabSet.removeTabAt(this.windowTabSet.indexOfTab(window.getName()));
            // Update modified status.
            this.setModifiedStatus(true);
        } catch (Throwable t) {
            this.showStackTrace(t);
        }
    }
    
    /**
     * Synchronises the schema with the underlying table providers.
     */
    public void synchroniseAll() {
        // Set hint to creating windows
        this.setHint(BuilderBundle.getString("synchronisingSchema"));
        // Do this later in the background
        final MartBuilder mb = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    mb.getSchema().synchronise();
                    // Update modified status.
                    mb.setModifiedStatus(true);
                } catch (Throwable t) {
                    mb.showStackTrace(t);
                } finally {
                    mb.resetHint();
                }
            }
        });
    }
    
    /**
     * Synchronises the table provider with the underlying data structures.
     * @param tblProv the table provider to synchronise.
     */
    public void synchroniseTableProvider(final TableProvider tblProv) {
        // Set hint to creating windows
        this.setHint(BuilderBundle.getString("synchronisingTblProv", tblProv.getName()));
        // Do this later in the background
        final MartBuilder mb = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    tblProv.synchronise();
                    // Update modified status.
                    mb.setModifiedStatus(true);
                } catch (Throwable t) {
                    mb.showStackTrace(t);
                } finally {
                    mb.resetHint();
                }
            }
        });
    }
    
    /**
     * Synchronises the window with the underlying table provider.
     * @param window the window to synchronise.
     */
    public void synchroniseWindow(final Window window) {
        // Set hint to creating windows
        this.setHint(BuilderBundle.getString("synchronisingWindow", window.getName()));
        // Do this later in the background
        final MartBuilder mb = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    window.synchronise();
                    // Update modified status.
                    mb.setModifiedStatus(true);
                } catch (Throwable t) {
                    mb.showStackTrace(t);
                } finally {
                    mb.resetHint();
                }
            }
        });
    }
    
    /**
     * Returns the current modified status.
     * @return the current modified status, true for modified, false for unmodified.
     */
    public boolean getModifiedStatus() {
        return this.modifiedStatus;
    }
    
    /**
     * Sets the current modified status.
     * @param status true for modified, false for unmodified.
     */
    public void setModifiedStatus(boolean status) {
        this.modifiedStatus = status;
        // Update window title to match.
        this.setTitle(
                BuilderBundle.getString("GUITitle",SchemaSaver.DTD_VERSION) +
                (status?" *":"")
                );
        // Recalculate visible contents.
        this.windowTabSet.recalculateView();
    }
    
    /**
     * Returns the current schema being viewed.
     * @return the current schema.
     */
    public Schema getSchema() {
        return this.schema;
    }
    
    /**
     * Sets the current schema to be viewed. If the current schema has been modified
     * then it will confirm that the user wants to close the existing one first.
     * If the new schema value is null, a NullPointerException will be thrown.
     * @param schema to use.
     * @throws NullPointerException if the new schema is null.
     */
    public void setSchema(Schema schema) throws NullPointerException {
        // Sanity check.
        if (schema == null)
            throw new NullPointerException(BuilderBundle.getString("schemaIsNull"));
        // Do it.
        if (this.confirmCloseSchema()) {
            this.schema = schema;
            this.setModifiedStatus(false); // New schema is in non-modified state.
            this.windowTabSet.recreateTabs();
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>The preferred size is simply the preferred size of our schema tabs.</p>
     */
    public Dimension getPreferredSize() {
        return this.windowTabSet.getPreferredSize();
    }
    
    /**
     * {@inheritDoc}
     * <p>The minimum size is the 400x400, for no real reason.</p>
     */
    public Dimension getMinimumSize() {
        return new Dimension(400,400);
    }
    
    /**
     * The main method starts the application and opens the main window.
     * @param args the command line arguments. The first argument, if present,
     * is taken as the name of a schema XML file to load when the application
     * has started.
     */
    public static void main(String[] args) {
        // Do we have an input file on the command line?
        final File inputFile;
        if (args.length > 0) inputFile = new File(args[0]);
        else inputFile = null;
        // Start the application.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Create it.
                final MartBuilder mb = new MartBuilder();
                // Do we have an input file to load?
                if (inputFile != null) {
                    try {
                        mb.loadSchema(inputFile);
                    } catch (Throwable t) {
                        mb.showStackTrace(t);
                    }
                    // Reset the size to the new file's best options.
                    mb.setSize(mb.getPreferredSize());
                } else {
                    // Set a sensible size.
                    mb.setSize(mb.getMinimumSize());
                }
                // Open it.
                mb.setVisible(true);
            }
        });
    }
    
    /**
     * The internal window listener listens for events on the main {@link MartBuilder} window only.
     */
    private class MartBuilderWindowListener extends WindowAdapter {
        /**
         * Internal reference to the {@link MartBuilder} we're listening on.
         */
        private final MartBuilder martBuilder;
        
        /**
         * The constructor remembers the {@link MartBuilder} we're supposed to listen
         * to. {@link MartBuilder} is a {@link JFrame}, so this is OK to use as a window.
         * @param martBuilder the {@link MartBuilder} to listen to.
         */
        public MartBuilderWindowListener(MartBuilder martBuilder) {
            this.martBuilder = martBuilder;
        }
        
        /**
         * {@inheritDoc}
         * <p>Closing this window means we should check to see if our work
         * has been saved first. If there is unsaved work, confirm with the
         * user first. If not, exit straight away.</p>
         */
        public void windowClosing(WindowEvent e) {
            if (e.getWindow() == this.martBuilder) {
                this.martBuilder.exitNow();
            }
        }
    }
    
    /**
     * This is the main menu bar.
     */
    private class MartBuilderMenuBar extends JMenuBar implements ActionListener {
        /**
         * Internal reference to the {@link MartBuilder} parent.
         */
        private final MartBuilder martBuilder;
        
        /**
         * Internal references to the various menu options.
         */
        private JMenuItem newSchema;
        private JMenuItem openSchema;
        private JMenuItem saveSchema;
        private JMenuItem saveSchemaAs;
        private JMenuItem exit;
        private JMenuItem synchroniseAll;
        
        /**
         * Constructor calls super then sets up our menu items.
         * @param martBuilder the {@link MartBuilder} to which we are attached.
         */
        public MartBuilderMenuBar(MartBuilder martBuilder) {
            super();
            this.martBuilder = martBuilder;
            
            // File menu.
            
            JMenu fileMenu = new JMenu(BuilderBundle.getString("fileMenuTitle"));
            fileMenu.setMnemonic(BuilderBundle.getString("fileMenuMnemonic").charAt(0));
            
            this.newSchema = new JMenuItem(BuilderBundle.getString("newSchemaTitle"));
            this.newSchema.setMnemonic(BuilderBundle.getString("newSchemaMnemonic").charAt(0));
            this.newSchema.addActionListener(this);
            
            this.openSchema = new JMenuItem(BuilderBundle.getString("openSchemaTitle"));
            this.openSchema.setMnemonic(BuilderBundle.getString("openSchemaMnemonic").charAt(0));
            this.openSchema.addActionListener(this);
            
            this.saveSchema = new JMenuItem(BuilderBundle.getString("saveSchemaTitle"));
            this.saveSchema.setMnemonic(BuilderBundle.getString("saveSchemaMnemonic").charAt(0));
            this.saveSchema.addActionListener(this);
            
            this.saveSchemaAs = new JMenuItem(BuilderBundle.getString("saveSchemaAsTitle"));
            this.saveSchemaAs.setMnemonic(BuilderBundle.getString("saveSchemaAsMnemonic").charAt(0));
            this.saveSchemaAs.addActionListener(this);
            
            this.exit = new JMenuItem(BuilderBundle.getString("exitTitle"));
            this.exit.setMnemonic(BuilderBundle.getString("exitMnemonic").charAt(0));
            this.exit.addActionListener(this);
            
            fileMenu.add(this.newSchema);
            fileMenu.addSeparator();
            fileMenu.add(this.openSchema);
            fileMenu.add(this.saveSchema);
            fileMenu.add(this.saveSchemaAs);
            fileMenu.addSeparator();
            fileMenu.add(this.exit);
            
            this.add(fileMenu);
            
            // Schema menu.
            
            JMenu schemaMenu = new JMenu(BuilderBundle.getString("schemaMenuTitle"));
            schemaMenu.setMnemonic(BuilderBundle.getString("schemaMenuMnemonic").charAt(0));
            
            this.synchroniseAll = new JMenuItem(BuilderBundle.getString("synchroniseAllTitle"));
            this.synchroniseAll.setMnemonic(BuilderBundle.getString("synchroniseAllMnemonic").charAt(0));
            this.synchroniseAll.addActionListener(this);
            
            schemaMenu.add(this.synchroniseAll);
            
            this.add(schemaMenu);
        }
        
        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
            // File menu.
            
            if (e.getSource() == this.newSchema) this.martBuilder.newSchema();
            else if (e.getSource() == this.openSchema) this.martBuilder.loadSchema();
            else if (e.getSource() == this.saveSchema) this.martBuilder.saveSchema(false);
            else if (e.getSource() == this.saveSchemaAs) this.martBuilder.saveSchema(true);
            else if (e.getSource() == this.exit) this.martBuilder.exitNow();
            
            // Schema menu.
            
            else if (e.getSource() == this.synchroniseAll) this.martBuilder.synchroniseAll();
        }
    }
}
