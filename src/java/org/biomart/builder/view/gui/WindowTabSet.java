/*
 * WindowTabSet.java
 *
 * Created on 19 April 2006, 10:58
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Window;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Set of tabs to display a schema and set of windows.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 25th April 2006
 * @since 0.1
 */
public class WindowTabSet extends JTabbedPane {
    /**
     * Internal reference to the table provider tabs.
     */
    private TableProviderTabSet tableProviderTabSet;
    
    /**
     * Internal reference to the list of windows, in order, mapped
     * to their view.
     */
    private Map windowToTab = new HashMap();
    
    /**
     * The schema manager managing our schema.
     */
    private SchemaTabSet schemaTabSet;
    
    /**
     * The schema we are viewing.
     */
    private Schema schema;
    
    /**
     * The constructor remembers who its daddy is.
     * @param martBuilder the parent MartBuilder to which this tabbed panel belongs.
     */
    public WindowTabSet(SchemaTabSet schemaTabSet, Schema schema) {
        super();
        this.schemaTabSet = schemaTabSet;
        this.schema = schema;
        // Load the table providers.
        this.tableProviderTabSet = new TableProviderTabSet(this);
        // Set up the schema tab dummy placeholder.
        this.addTab(BuilderBundle.getString("schemaTabName"), new JLabel());
        // Set up the window tabs.
        this.synchroniseTabs();
    }
    
    /**
     * Return our schema.
     */
    public Schema getSchema() {
        return this.schema;
    }
    
    /**
     * Return our schema manager.
     */
    public SchemaTabSet getSchemaTabSet() {
        return this.schemaTabSet;
    }
    
    /**
     * Return our table provider tab set.
     */
    public TableProviderTabSet getTableProviderTabSet() {
        return this.tableProviderTabSet;
    }
    
    /**
     * Switch between listeners and attach the table provider set to the
     * appropriate place.
     */
    public void setSelectedIndex(int selectedIndex) {
        int schemaTabIndex = this.indexOfTab(BuilderBundle.getString("schemaTabName"));
        Component selectedComponent = this.getComponentAt(selectedIndex);
        if (selectedComponent instanceof WindowTab) {
            this.setComponentAt(schemaTabIndex, new JLabel()); // Dummy placeholder
            WindowTab windowTab = (WindowTab)selectedComponent;
            windowTab.attachTableProviderTabSet(this, this.tableProviderTabSet);
        } else {
            this.tableProviderTabSet.setAdaptor(new SchemaAdaptor(this));
            this.setComponentAt(schemaTabIndex, this.tableProviderTabSet);
        }
        super.setSelectedIndex(selectedIndex);
    }
    
    /**
     * Syncs our windows with our schema.
     */
    public void synchroniseTabs() {
        // Synchronise our windows.
        try {
            this.schema.synchroniseWindows();
        } catch (Throwable t) {
            this.schemaTabSet.getMartBuilder().showStackTrace(t);
        }
        // Add all schema windows that we don't have yet.
        List schemaWindows = new ArrayList(this.schema.getWindows());
        for (Iterator i = schemaWindows.iterator(); i.hasNext(); ) {
            Window window = (Window)i.next();
            if (!this.windowToTab.containsKey(window)) this.addWindowTab(window);
        }
        // Remove all our windows that are not in the schema.
        List candidates = new ArrayList(this.windowToTab.keySet());
        for (Iterator i = candidates.iterator(); i.hasNext(); ) {
            Window window = (Window)i.next();
            if (!schemaWindows.contains(window)) this.removeWindowTab(window);
        }
        // Synchronise our tab view contents.
        for (int i = 1; i < this.getTabCount(); i++) {
            WindowTab windowTab = (WindowTab)this.getComponentAt(i);
            windowTab.synchroniseDatasetView();
        }
        // Synchronise the table provider views.
        this.tableProviderTabSet.synchroniseTabs();
        // Redraw.
        this.validate();
    }
    
    /**
     * Confirms with user whether they really want to remove this window.
     */
    public void confirmRemoveWindow(Window window) {
        // Must confirm action first.
        int choice = JOptionPane.showConfirmDialog(
                this,
                BuilderBundle.getString("confirmDelDataset"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.YES_NO_OPTION
                );
        if (choice == JOptionPane.YES_OPTION) {
            try {
                this.schema.removeWindow(window);
                this.removeWindowTab(window);
                this.schemaTabSet.setModifiedStatus(true);
                // Nasty hack to force redraw.
                int componentIndex = this.indexOfTab(BuilderBundle.getString("schemaTabName"));
                this.setSelectedIndex(componentIndex);
            } catch (Throwable t) {
                this.schemaTabSet.getMartBuilder().showStackTrace(t);
            }
        }
    }
    
    /**
     * Removes a window and tab.
     */
    private void removeWindowTab(Window window) {
        WindowTab windowTab = (WindowTab)this.windowToTab.get(window);
        this.remove(windowTab);
        this.windowToTab.remove(window);
    }
    
    /**
     * Creates a new tab based around a given table.
     */
    private void addWindowTab(Window window) {
        // Create tabs themselves.
        WindowTab windowTab = new WindowTab(this, window);
        this.addTab(window.getName(), windowTab);
        // Remember them.
        this.windowToTab.put(window, windowTab);
    }
    
    /**
     * Prompt for a name for a window.
     */
    private String getWindowName(String defaultResponse) {
        // Get one from user.
        String name = (String)JOptionPane.showInputDialog(
                this.schemaTabSet.getMartBuilder(),
                BuilderBundle.getString("requestWindowName"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defaultResponse
                );
        // If empty, use table name.
        if (name == null) return null;
        else if (name.trim().length()==0) name = defaultResponse;
        // Return.
        return name;
    }
    
    /**
     * Renames a table provider (and tab).
     */
    public void renameWindow(Window window) {
        // Update the table provider name and the tab name.
        try {
            String newName = this.getWindowName(window.getName());
            if (newName != null && !newName.equals(window.getName())) {
                this.removeWindowTab(window);
                this.schema.renameWindow(window, newName);
                this.addWindowTab(window);
                this.setSelectedIndex(this.indexOfTab(newName));
                this.schemaTabSet.setModifiedStatus(true);
            }
        } catch (Throwable t) {
            this.schemaTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Create a single window.
     */
    public void createWindow(Table table) {
        try {
            Window window = new Window(this.schema, table, this.getWindowName(table.getName()));
            this.synchroniseTabs();
            this.schemaTabSet.setModifiedStatus(true);
        } catch (Throwable t) {
            this.schemaTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Suggest windows.
     */
    public void suggestWindows(Table table) {
        try {
            this.schema.suggestWindows(table, this.getWindowName(table.getName()));
            this.synchroniseTabs();
            this.schemaTabSet.setModifiedStatus(true);
        } catch (Throwable t) {
            this.schemaTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Optimise stuff.
     */
    public void optimiseRelations(Window window) {
        try {
            window.optimiseRelations();
            this.synchroniseTabs();
            this.schemaTabSet.setModifiedStatus(true);
        } catch (Throwable t) {
            this.schemaTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Construct a context menu for a given window view tab.
     * @param window the window to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getWindowTabContextMenu(final Window window) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem close = new JMenuItem(BuilderBundle.getString("removeWindowTitle"));
        close.setMnemonic(BuilderBundle.getString("removeWindowMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmRemoveWindow(window);
            }
        });
        contextMenu.add(close);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameWindowTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameWindowMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                renameWindow(window);
            }
        });
        contextMenu.add(rename);
        
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
                Component selectedComponent = this.getComponentAt(selectedIndex);
                // Respond appropriately.
                if (selectedComponent instanceof WindowTab) {
                    this.setSelectedIndex(selectedIndex);
                    Window window = ((WindowTab)selectedComponent).getWindow();
                    this.getWindowTabContextMenu(window).show(this, evt.getX(), evt.getY());
                    eventProcessed = true;
                }
            }
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * This is a custom JPanel which knows how to synchroniseTabs it's children.
     */
    private class WindowTab extends JPanel {
        /**
         * The window we are viewing.
         */
        private Window window;
        
        /**
         * The display area.
         */
        private JPanel displayArea;
        
        /**
         * Our window button.
         */
        private JRadioButton windowButton;
        
        /**
         * The dataset view.
         */
        private TableView datasetView;
        
        /**
         * This constructor builds a pair of switcher-style buttons which alternate
         * between window and dataset view.
         * @param windowView the window view.
         * @param datasetView the dataset view.
         */
        public WindowTab(WindowTabSet windowTabSet, Window window) {
            super(new BorderLayout());
            this.window = window;
            // Create display part of the tab.
            this.displayArea = new JPanel(new CardLayout());
            // Dataset card first.
            this.datasetView = new TableView(windowTabSet, window.getDataSet());
            this.datasetView.setAdaptor(new DataSetAdaptor(windowTabSet, window));
            JScrollPane scroller = new JScrollPane(this.datasetView);
            scroller.getViewport().setBackground(this.datasetView.getBackground());
            displayArea.add(scroller, "DATASET_CARD");
            // Create switcher part of the tab.
            JPanel switcher = new JPanel();
            // Dataset button.
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
            // Window button.
            this.windowButton = new JRadioButton(BuilderBundle.getString("windowButtonName"));
            windowButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == windowButton) {
                        CardLayout cards = (CardLayout)displayArea.getLayout();
                        cards.show(displayArea, "WINDOW_CARD");
                    }
                }
            });
            switcher.add(windowButton);
            // Make buttons mutually exclusive.
            ButtonGroup buttons = new ButtonGroup();
            buttons.add(this.windowButton);
            buttons.add(datasetButton);
            // Add the components to the panel.
            this.add(switcher, BorderLayout.NORTH);
            this.add(displayArea, BorderLayout.CENTER);
            // Set our preferred size to the dataset size plus a bit on top for the switcher buttons.
            Dimension preferredSize = datasetView.getPreferredSize();
            double extraHeight = datasetButton.getHeight();
            preferredSize.setSize(preferredSize.getWidth(), preferredSize.getHeight()+extraHeight);
            this.setPreferredSize(preferredSize);
            // Select the default one (dataset).
            datasetButton.doClick();
        }
        
        /**
         * Returns our window.
         */
        public Window getWindow() {
            return this.window;
        }
        
        /**
         * Attach the table provider tab set.
         */
        public void attachTableProviderTabSet(WindowTabSet windowTabSet, TableProviderTabSet tableProviderTabSet) {
            tableProviderTabSet.setAdaptor(new WindowAdaptor(windowTabSet, this.window));
            this.displayArea.add(tableProviderTabSet, "WINDOW_CARD");
            // Nasty hack to force table provider set to redisplay.
            if (this.windowButton.isSelected()) this.windowButton.doClick();
        }
        
        /**
         * Resync the dataset view.
         */
        public void synchroniseDatasetView() {
            this.datasetView.synchroniseView();
        }
    }
}
