/*
 * DataSetTabSet.java
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
import org.biomart.builder.controller.MartUtils;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Set of tabs to display a mart and set of windows.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 2nd May 2006
 * @since 0.1
 */
public class DataSetTabSet extends JTabbedPane {
    /**
     * Internal reference to the table provider tabs.
     */
    private SchemaTabSet schemaTabSet;
    
    /**
     * Internal reference to the list of windows, in order, mapped
     * to their view.
     */
    private Map datasetToTab = new HashMap();
    
    /**
     * The mart manager managing our mart.
     */
    private MartTabSet martTabSet;
    
    /**
     * The mart we are viewing.
     */
    private Mart mart;
    
    /**
     * The constructor remembers who its daddy is.
     * @param martBuilder the parent MartBuilder to which this tabbed panel belongs.
     */
    public DataSetTabSet(MartTabSet martTabSet, Mart mart) {
        super();
        this.martTabSet = martTabSet;
        this.mart = mart;
        // Load the table providers.
        this.schemaTabSet = new SchemaTabSet(this);
        // Set up the mart tab dummy placeholder.
        this.addTab(BuilderBundle.getString("schemaTabName"), new JLabel());
        // Set up the dataset tabs.
        this.synchroniseTabs();
    }
    
    /**
     * Return our mart.
     */
    public Mart getMart() {
        return this.mart;
    }
    
    /**
     * Return our mart manager.
     */
    public MartTabSet getMartTabSet() {
        return this.martTabSet;
    }
    
    /**
     * Return our table provider tab set.
     */
    public SchemaTabSet getSchemaTabSet() {
        return this.schemaTabSet;
    }
    
    /**
     * Switch between listeners and attach the table provider set to the
     * appropriate place.
     */
    public void setSelectedIndex(int selectedIndex) {
        int schemaTabIndex = this.indexOfTab(BuilderBundle.getString("schemaTabName"));
        Component selectedComponent = this.getComponentAt(selectedIndex);
        if (selectedComponent instanceof DataSetTab) {
            this.setComponentAt(schemaTabIndex, new JLabel()); // Dummy placeholder
            DataSetTab datasetTab = (DataSetTab)selectedComponent;
            datasetTab.attachSchemaTabSet(this, this.schemaTabSet);
        } else {
            this.schemaTabSet.setDiagramModifier(new SchemaDiagramModifier(this));
            this.setComponentAt(schemaTabIndex, this.schemaTabSet);
        }
        super.setSelectedIndex(selectedIndex);
    }
    
    /**
     * Syncs our windows with our mart.
     */
    public void synchroniseTabs() {
        // Synchronise our windows.
        try {
            MartUtils.synchroniseMartDataSets(mart);
        } catch (Throwable t) {
            martTabSet.getMartBuilder().showStackTrace(t);
        }
        // Add all mart windows that we don't have yet.
        List martDataSets = new ArrayList(mart.getDataSets());
        for (Iterator i = martDataSets.iterator(); i.hasNext(); ) {
            DataSet dataset = (DataSet)i.next();
            if (!datasetToTab.containsKey(dataset)) addDataSetTab(dataset);
        }
        // Remove all our windows that are not in the mart.
        List ourDataSets = new ArrayList(datasetToTab.keySet());
        for (Iterator i = ourDataSets.iterator(); i.hasNext(); ) {
            DataSet dataset = (DataSet)i.next();
            if (!martDataSets.contains(dataset)) removeDataSetTab(dataset);
        }
        // Synchronise our tab view contents.
        for (int i = 1; i < getTabCount(); i++) {
            DataSetTab datasetTab = (DataSetTab)getComponentAt(i);
            datasetTab.synchroniseDatasetDiagram();
        }
        // Synchronise the table provider views.
        schemaTabSet.synchroniseTabs();
        // Redraw.
        validate();
    }
    
    /**
     * Confirms with user whether they really want to remove this dataset.
     */
    public void confirmRemoveDataSet(DataSet dataset) {
        // Must confirm action first.
        int choice = JOptionPane.showConfirmDialog(
                this,
                BuilderBundle.getString("confirmDelDataset"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.YES_NO_OPTION
                );
        if (choice == JOptionPane.YES_OPTION) {
            try {
                MartUtils.removeDataSetFromSchema(this.mart, dataset);
                this.synchroniseTabs();
                this.martTabSet.setModifiedStatus(true);
                // Nasty hack to force redraw.
                int componentIndex = this.indexOfTab(BuilderBundle.getString("schemaTabName"));
                this.setSelectedIndex(componentIndex);
            } catch (Throwable t) {
                this.martTabSet.getMartBuilder().showStackTrace(t);
            }
        }
    }
    
    /**
     * Removes a dataset and tab.
     */
    private void removeDataSetTab(DataSet dataset) {
        DataSetTab datasetTab = (DataSetTab)this.datasetToTab.get(dataset);
        this.remove(datasetTab);
        this.datasetToTab.remove(dataset);
    }
    
    /**
     * Creates a new tab based around a given table.
     */
    private void addDataSetTab(DataSet dataset) {
        // Create tabs themselves.
        DataSetTab datasetTab = new DataSetTab(this, dataset);
        this.addTab(dataset.getName(), datasetTab);
        // Remember them.
        this.datasetToTab.put(dataset, datasetTab);
    }
    
    /**
     * Prompt for a name for a dataset.
     */
    private String getDataSetName(String defaultResponse) {
        // Get one from user.
        String name = (String)JOptionPane.showInputDialog(
                this.martTabSet.getMartBuilder(),
                BuilderBundle.getString("requestDataSetName"),
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
    public void renameDataSet(DataSet dataset) {
        // Update the table provider name and the tab name.
        try {
            String newName = this.getDataSetName(dataset.getName());
            if (newName != null && !newName.equals(dataset.getName())) {
                int tabIndex = this.indexOfTab(dataset.getName());
                MartUtils.renameDataSet(this.mart, dataset, newName);
                this.setTitleAt(tabIndex, newName);
                this.martTabSet.setModifiedStatus(true);
            }
        } catch (Throwable t) {
            this.martTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Create a single dataset.
     */
    public void createDataSet(Table table) {
        try {
            MartUtils.createDataSet(this.mart, table, this.getDataSetName(table.getName()));
            this.synchroniseTabs();
            this.martTabSet.setModifiedStatus(true);
        } catch (Throwable t) {
            this.martTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Suggest windows.
     */
    public void suggestDataSets(Table table) {
        try {
            MartUtils.suggestDataSets(this.mart, table, this.getDataSetName(table.getName()));
            this.synchroniseTabs();
            this.martTabSet.setModifiedStatus(true);
        } catch (Throwable t) {
            this.martTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Optimise stuff.
     */
    public void optimiseDataSet(DataSet dataset) {
        try {
            MartUtils.optimiseDataSet(dataset);
            this.synchroniseTabs();
            this.martTabSet.setModifiedStatus(true);
        } catch (Throwable t) {
            this.martTabSet.getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Construct a context menu for a given dataset view tab.
     *
     * @param dataset the dataset to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getDataSetTabContextMenu(final DataSet dataset) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem close = new JMenuItem(BuilderBundle.getString("removeDataSetTitle"));
        close.setMnemonic(BuilderBundle.getString("removeDataSetMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmRemoveDataSet(dataset);
            }
        });
        contextMenu.add(close);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameDataSetTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameDataSetMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                renameDataSet(dataset);
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
                if (selectedComponent instanceof DataSetTab) {
                    this.setSelectedIndex(selectedIndex);
                    DataSet dataset = ((DataSetTab)selectedComponent).getDataSet();
                    this.getDataSetTabContextMenu(dataset).show(this, evt.getX(), evt.getY());
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
    private class DataSetTab extends JPanel {
        /**
         * The dataset we are viewing.
         */
        private DataSet dataset;
        
        /**
         * The display area.
         */
        private JPanel displayArea;
        
        /**
         * Our dataset button.
         */
        private JRadioButton windowButton;
        
        /**
         * The dataset view.
         */
        private SchemaDiagram datasetDiagram;
        
        /**
         * This constructor builds a pair of switcher-style buttons which alternate
         * between dataset and dataset view.
         *
         * @param windowView the dataset view.
         * @param datasetDiagram the dataset view.
         */
        public DataSetTab(DataSetTabSet datasetTabSet, DataSet dataset) {
            super(new BorderLayout());
            this.dataset = dataset;
            // Create display part of the tab.
            this.displayArea = new JPanel(new CardLayout());
            // Dataset card first.
            this.datasetDiagram = new SchemaDiagram(datasetTabSet, dataset);
            this.datasetDiagram.setDiagramModifier(new DataSetDiagramModifier(datasetTabSet, dataset));
            JScrollPane scroller = new JScrollPane(this.datasetDiagram);
            scroller.getViewport().setBackground(this.datasetDiagram.getBackground());
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
            // DataSet button.
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
            Dimension preferredSize = datasetDiagram.getPreferredSize();
            double extraHeight = datasetButton.getHeight();
            preferredSize.setSize(preferredSize.getWidth(), preferredSize.getHeight()+extraHeight);
            this.setPreferredSize(preferredSize);
            // Select the default one (dataset).
            datasetButton.doClick();
        }
        
        /**
         * Returns our dataset.
         */
        public DataSet getDataSet() {
            return this.dataset;
        }
        
        /**
         * Attach the table provider tab set.
         */
        public void attachSchemaTabSet(DataSetTabSet datasetTabSet, SchemaTabSet schemaTabSet) {
            schemaTabSet.setDiagramModifier(new WindowDiagramModifier(datasetTabSet, this.dataset));
            this.displayArea.add(schemaTabSet, "WINDOW_CARD");
            // Nasty hack to force table provider set to redisplay.
            if (this.windowButton.isSelected()) this.windowButton.doClick();
        }
        
        /**
         * Resync the dataset view.
         */
        public void synchroniseDatasetDiagram() {
            this.datasetDiagram.synchroniseDiagram();
        }
    }
}
