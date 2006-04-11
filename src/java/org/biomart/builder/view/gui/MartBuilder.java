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
import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.biomart.builder.controller.SchemaSaver;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Window;
import org.biomart.builder.resources.BuilderBundle;

/**
 * The main window housing the MartBuilder GUI.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
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
     * Internal reference to the tabs containing the schema.
     */
    private MartBuilderSchemaTabs schemaTabs;
    
    /**
     * Internal reference to a JFileChooser for opening/saving XML files with.
     */
    private XMLFileChooser xmlFileChooser = new XMLFileChooser();
    
    /**
     * Internal reference to the hint bar.
     */
    private JLabel hintBar = new JLabel("");
    
    /**
     * Creates a new instance of MartBuilder. Calls initComponents
     * to set up the UI.
     */
    public MartBuilder() {
        // Create the window.
        super(BuilderBundle.getString("GUITitle",SchemaSaver.DTD_VERSION));
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
        this.addWindowListener(new MartBuilderWindowListener(this));
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Make a menu bar and add it.
        this.setJMenuBar(new MartBuilderMenuBar(this));
        // Set up the hint bar.
        this.hintBar.setBorder(BorderFactory.createLoweredBevelBorder());
        this.getContentPane().add(this.hintBar, BorderLayout.SOUTH);
        // Set up the schema tabs.
        this.schemaTabs = new MartBuilderSchemaTabs(this);
        this.getContentPane().add(this.schemaTabs, BorderLayout.CENTER);
        // Pack the window.
        this.pack();
    }
    
    /**
     * Display a nice friendly stack trace window.
     * @param t the throwable to display the stack trace for.
     */
    public void showStackTrace(Throwable t) {
        String mainMessage = t.getLocalizedMessage();
        int messageClass = (t instanceof Error) ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, mainMessage, BuilderBundle.getString("stackTraceTitle"), messageClass);
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
     * If the schema is modified, asks the user for confirmation whether to
     * close it without saving or not. If the schema is unmodified, no action
     * is taken.
     * @return true if the schema can be closed without saving first, false if
     * the user wishes to cancel the close action.
     */
    public boolean confirmCloseSchema() {
        if (this.getModifiedStatus() == true) {
            // Modified, so must confirm action first.
            JOptionPane confirm = new JOptionPane(
                    "Schema has been altered since the last time it was saved. " +
                    "Is it OK to close it without saving the changes first?",
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_NO_OPTION
                    );
            confirm.setVisible(true);
            Object choice = confirm.getValue();
            if (choice == null) return false; // Closed the window without clicking anything.
            else return (((Integer)choice).intValue() == JOptionPane.YES_OPTION);
        } else {
            // Not modified, so can close OK.
            return true;
        }
    }
    
    /**
     * Tells the application to exit.
     */
    public void exitNow() {
        if (this.confirmCloseSchema()) System.exit(0);
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
        this.xmlFileChooser.showOpenDialog(this);
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
     * @throws Throwable if there was any problem.
     */
    public void loadSchema(final File file) throws Throwable {
        if (file == null)
            throw new NullPointerException(BuilderBundle.getString("fileIsNull"));
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
            this.xmlFileChooser.showSaveDialog(this);
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
     * @throws Throwable if there was any problem.
     */
    public void saveSchema(final File file) throws Throwable {
        if (file == null)
            throw new NullPointerException(BuilderBundle.getString("fileIsNull"));
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
    
    
    // add/remove table provider (causes schemaTabs.resyncTableProviders)
    // create/create-predict/remove window (causes schemaTabs.recreateTabs)    
    
    
    
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
            this.schemaTabs.recreateTabs();
        }
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
                // Set a sensible minimum size then maximize it.
                mb.setSize(400,400);
                mb.setExtendedState(JFrame.MAXIMIZED_BOTH);
                // Open it.
                mb.setVisible(true);
                // Do we have an input file to load?
                if (inputFile != null) {
                    try {
                        mb.loadSchema(inputFile);
                    } catch (Throwable t) {
                        mb.showStackTrace(t);
                    }
                }
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
        private MartBuilder martBuilder;
        
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
     * This JFileChooser only opens XML files.
     */
    private class XMLFileChooser extends JFileChooser {
        /**
         * The default constructor calls the super, then sets the filter
         * to XML files only.
         */
        public XMLFileChooser() {
            super();
            // Create the filter.
            this.setFileFilter(new FileFilter(){
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
    }
    
    /**
     * This is the main menu bar.
     */
    private class MartBuilderMenuBar extends JMenuBar implements ActionListener {
        /**
         * Internal reference to the {@link MartBuilder} parent.
         */
        private MartBuilder martBuilder;
        
        /**
         * Internal references to the various menu options.
         */
        private JMenuItem newSchema;
        private JMenuItem openSchema;
        private JMenuItem saveSchema;
        private JMenuItem saveSchemaAs;
        private JMenuItem exit;
        
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
        }
    }
    
    /**
     * This class represents the tabbed layout that draws Schemas.
     */
    private class MartBuilderSchemaTabs extends JTabbedPane {
        /**
         * Internal reference to the {@link MartBuilder} parent.
         */
        private MartBuilder martBuilder;
        
        /**
         * Internal reference to the set of multi-table-providers which might
         * need resyncing once in a while.
         */
        private Set multiTableProviders = new HashSet();
        
        /**
         * The constructor remembers who its daddy is.
         * @param martBuilder the parent MartBuilder to which this tabbed panel belongs.
         */
        public MartBuilderSchemaTabs(MartBuilder martBuilder) {
            super();
            this.martBuilder = martBuilder;
        }
        
        /**
         * This function makes the tabs match up with the contents of the {@link Schema}
         * object in the parent {@link MartBuilder}.
         */
        public void recreateTabs() {
            // Remove the existing tabs.
            this.removeAll();
            this.multiTableProviders.clear();
            
            // Schema tab first.
            JPanel schemaTab = new JPanel(new BorderLayout());
            SchemaView schemaView = new SchemaView(this.martBuilder.getSchema());
            schemaTab.add(schemaView, BorderLayout.CENTER);
            this.multiTableProviders.add(schemaView);
            this.addTab(BuilderBundle.getString("schemaTabName"), schemaTab);
            
            // Now the window tabs.
            for (Iterator i = this.martBuilder.getSchema().getWindows().iterator(); i.hasNext(); ) {
                Window w = (Window)i.next();
                // Create display part of the tab.
                final JPanel displayArea = new JPanel(new CardLayout());
                displayArea.add(new DataSetView(w.getDataSet()), "DATASET_CARD");
                WindowView windowView = new WindowView(w);
                displayArea.add(windowView, "WINDOW_CARD");
                this.multiTableProviders.add(windowView);
                // Create switcher part of the tab.
                JPanel switcher = new JPanel();
                final JRadioButton datasetButton = new JRadioButton(BuilderBundle.getString("datasetButtonName"));
                datasetButton.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        if (e.getSource() == datasetButton) {
                            CardLayout cards = (CardLayout)displayArea.getLayout();
                            cards.show(displayArea, "DATASET_CARD");
                        }
                    }
                });
                switcher.add(datasetButton);
                final JRadioButton windowButton = new JRadioButton(BuilderBundle.getString("windowButtonName"));
                windowButton.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        if (e.getSource() == windowButton) {
                            CardLayout cards = (CardLayout)displayArea.getLayout();
                            cards.show(displayArea, "WINDOW_CARD");
                        }
                    }
                });
                switcher.add(windowButton);
                // Make buttons mutually exclusive, and select a default.
                ButtonGroup buttons = new ButtonGroup();
                buttons.add(windowButton);
                buttons.add(datasetButton);
                datasetButton.setSelected(true);
                // Create tab itself.
                JPanel windowTab = new JPanel(new BorderLayout());
                windowTab.add(switcher, BorderLayout.NORTH);
                windowTab.add(displayArea, BorderLayout.CENTER);
                this.addTab(w.getName(), windowTab);
            }
            
            // Select the schema tab by default.
            this.setSelectedComponent(schemaTab);
        }
        
        /**
         * Resyncs the table providers within each multi table provider tab.
         */
        public void resyncTableProviders() {
            for (Iterator i = this.multiTableProviders.iterator(); i.hasNext(); ) {
                MultiTableProviderView multi = (MultiTableProviderView)i.next();
                multi.resyncTableProviders(this.martBuilder.getSchema().getTableProviders());
            }
        }
    }
}
