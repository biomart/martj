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
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.biomart.builder.controller.SchemaSaver;
import org.biomart.builder.resources.BuilderBundle;

/**
 * The main window housing the MartBuilder GUI.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 21st April 2006
 * @since 0.1
 */
public class MartBuilder extends JFrame {
    /**
     * Our schema manager.
     */
    private SchemaManager schemaManager;
    
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
        // Set up our GUI components.
        this.initComponents();
    }
    
    /**
     * Creates all the components that are part of the main window.
     */
    private void initComponents() {
        // Set up window listener and use it to handle windows closing.
        final MartBuilder mb = this;
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (e.getWindow() == mb) exitApp();
            }
        });
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Make a menu bar and add it.
        this.setJMenuBar(new MartBuilderMenuBar(this));
        // Set up the schema manager.
        this.schemaManager = new SchemaManager(this);
        this.getContentPane().add(this.schemaManager, BorderLayout.CENTER);
        // Pack the window.
        this.pack();
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
     * Confirms the user wants to exit, then exits.
     */
    public void exitApp() {
        if (this.schemaManager.confirmCloseAllSchemas()) System.exit(0);
    }
    
    /**
     * {@inheritDoc}
     * <p>The minimum size is arbitrary.</p>
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
        // Start the application.
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Create it.
                MartBuilder mb = new MartBuilder();
                // Set a sensible size.
                mb.setSize(mb.getMinimumSize());
                // Open it.
                mb.setVisible(true);
            }
        });
    }
    
    /**
     * This is the main menu bar.
     */
    private class MartBuilderMenuBar extends JMenuBar implements ActionListener {
        /**
         * The MartBuilder which will receive events.
         */
        private final MartBuilder martBuilder;
        
        /**
         * Internal references to the various menu options.
         */
        private JMenuItem newSchema;
        private JMenuItem openSchema;
        private JMenuItem saveSchema;
        private JMenuItem saveSchemaAs;
        private JMenuItem closeSchema;
        private JMenuItem exit;
        private JMenuItem synchroniseSchema;
        
        /**
         * Constructor calls super then sets up our menu items.
         * @param martBuilder the {@link MartBuilder} to which we are attached.
         */
        public MartBuilderMenuBar(final MartBuilder martBuilder) {
            super();
            
            // Remember our parent.
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

            this.closeSchema = new JMenuItem(BuilderBundle.getString("closeSchemaTitle"));
            this.closeSchema.setMnemonic(BuilderBundle.getString("closeSchemaMnemonic").charAt(0));
            this.closeSchema.addActionListener(this);
            
            this.exit = new JMenuItem(BuilderBundle.getString("exitTitle"));
            this.exit.setMnemonic(BuilderBundle.getString("exitMnemonic").charAt(0));
            this.exit.addActionListener(this);
            
            fileMenu.add(this.newSchema);
            fileMenu.addSeparator();
            fileMenu.add(this.openSchema);
            fileMenu.add(this.saveSchema);
            fileMenu.add(this.saveSchemaAs);
            fileMenu.add(this.closeSchema);
            fileMenu.addSeparator();
            fileMenu.add(this.exit);
            
            fileMenu.addMenuListener(new MenuListener() {
                public void menuSelected(MenuEvent e) {
                    boolean hasSchema = true;
                    if (martBuilder.schemaManager.getCurrentSchema()==null) hasSchema = false;
                    saveSchema.setEnabled(hasSchema);
                    saveSchemaAs.setEnabled(hasSchema);
                    closeSchema.setEnabled(hasSchema);
                }
                public void menuDeselected(MenuEvent e) {}
                public void menuCanceled(MenuEvent e) {}
            });
            
            this.add(fileMenu);
            
            // Schema menu.
            
            JMenu schemaMenu = new JMenu(BuilderBundle.getString("schemaMenuTitle"));
            schemaMenu.setMnemonic(BuilderBundle.getString("schemaMenuMnemonic").charAt(0));
            
            this.synchroniseSchema = new JMenuItem(BuilderBundle.getString("synchroniseSchemaTitle"));
            this.synchroniseSchema.setMnemonic(BuilderBundle.getString("synchroniseSchemaMnemonic").charAt(0));
            this.synchroniseSchema.addActionListener(this);
            
            schemaMenu.add(this.synchroniseSchema);
            
            schemaMenu.addMenuListener(new MenuListener() {
                public void menuSelected(MenuEvent e) {
                    boolean hasSchema = true;
                    if (martBuilder.schemaManager.getCurrentSchema()==null) hasSchema = false;
                    synchroniseSchema.setEnabled(hasSchema);
                }
                public void menuDeselected(MenuEvent e) {}
                public void menuCanceled(MenuEvent e) {}
            });
            
            this.add(schemaMenu);
        }
        
        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {

            // File menu.
            
            if (e.getSource() == this.newSchema) this.martBuilder.schemaManager.newSchema();
            else if (e.getSource() == this.openSchema) this.martBuilder.schemaManager.loadSchema();
            else if (e.getSource() == this.saveSchema) this.martBuilder.schemaManager.saveSchema();
            else if (e.getSource() == this.saveSchemaAs) this.martBuilder.schemaManager.saveSchemaAs();
            else if (e.getSource() == this.closeSchema) this.martBuilder.schemaManager.confirmCloseSchema();
            else if (e.getSource() == this.exit) this.martBuilder.exitApp();
            
            // Schema menu.
            
            else if (e.getSource() == this.synchroniseSchema) this.martBuilder.schemaManager.synchroniseSchema();
        }
    }
}
