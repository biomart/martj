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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.biomart.builder.model.Window;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Set of tabs to display a schema and set of windows.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public class WindowTabSet extends JTabbedPane {
    /**
     * Internal reference to the {@link MartBuilder} parent.
     */
    private final MartBuilder martBuilder;
    
    /**
     * The constructor remembers who its daddy is.
     * @param martBuilder the parent MartBuilder to which this tabbed panel belongs.
     */
    public WindowTabSet(MartBuilder martBuilder) {
        super();
        this.martBuilder = martBuilder;
    }
    
    /**
     * Returns the mart builder.
     * @return the mart builder used in this instance.
     */
    protected MartBuilder getMartBuilder() {
        return this.martBuilder;
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercepts tab selection in order to recalculate the visible view.</p>
     */
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        this.recalculateView();
    }
    
    /**
     * Recalculates the way to display what we are seeing at the moment.
     */
    public void recalculateView() {
        int index = this.getSelectedIndex();
        if (index >= 0) {
            View view =
                    (View)((JScrollPane)this.getComponentAt(0)).getViewport().getView();
            view.recalculateView();
        }
    }
    
    /**
     * This function makes the tabs match up with the contents of the {@link Schema}
     * object in the parent {@link MartBuilder}.
     */
    public void recreateTabs() {
        // Remove the existing tabs.
        this.removeAll();
        // Schema tab first.
        MultiView schemaView = new TableProviderTabSet(this.getMartBuilder().getSchema().getTableProviders());
        JScrollPane scroller = new JScrollPane(schemaView.asJComponent());
        scroller.getViewport().setBackground(schemaView.asJComponent().getBackground());
        this.addTab(BuilderBundle.getString("schemaTabName"), scroller);
        // Set the listener.
        schemaView.setListener(new DefaultListener(this.getMartBuilder()));
        // Now the window tabs.
        for (Iterator i = this.martBuilder.getSchema().getWindows().iterator(); i.hasNext(); ) {
            Window window = (Window)i.next();
            this.createTab(window);
        }
    }
    
    /**
     * Creates and adds a new tab based around a given window.
     * @param window the window to base the new tab around.
     */
    public void createTab(Window window) {
        // Create the views.
        MultiView windowView = new TableProviderTabSet(this.getMartBuilder().getSchema().getTableProviders());
        View datasetView = new TableProviderView(window.getDataSet());
        // Set the listeners.
        windowView.setListener(new WindowListener(this.getMartBuilder(), window));
        datasetView.setListener(new DataSetListener(this.getMartBuilder(), window));
        // Create tabs themselves.
        WindowTab windowTab = new WindowTab(windowView, datasetView);
        this.addTab(window.getName(), windowTab);
    }
    
    /**
     * Resyncs the table providers within each multi table provider tab.
     */
    public void resyncTableProviderTabs() {
        // Synchronise the schema tab.
        MultiView schemaTab = (TableProviderTabSet)((JScrollPane)this.getComponentAt(0)).getViewport().getView();
        schemaTab.resyncTableProviders(this.getMartBuilder().getSchema().getTableProviders());
        // Start loop at 1 to skip the initial schema tab.
        for (int i = 1; i < this.getTabCount(); i++) {
            WindowTab windowTab = (WindowTab)this.getComponentAt(i);
            windowTab.resyncTableProviders(this.getMartBuilder().getSchema().getTableProviders());
        }
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
            int index = this.indexAtLocation(evt.getX(), evt.getY());
            // Respond appropriately.
            if (index > 0) {
                Window window = this.getMartBuilder().getSchema().getWindowByName(this.getTitleAt(index));
                this.getWindowTabContextMenu(window).show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            }
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * Construct a context menu for a given window view tab.
     * @param window the window to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getWindowTabContextMenu(final Window window) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem close = new JMenuItem(BuilderBundle.getString("removeWindowTitle", window.getName()));
        close.setMnemonic(BuilderBundle.getString("removeWindowMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getMartBuilder().removeWindow(window, true);
            }
        });
        contextMenu.add(close);
        return contextMenu;
    }
    
    /**
     * This is a custom JPanel which knows how to synchronise it's children.
     */
    private class WindowTab extends JPanel {
        /**
         * Internal reference to the display area.
         */
        private final JPanel displayArea;
        
        /**
         * Internal reference to the cards displayed in the display area.
         */
        private Map cardToView = new HashMap();
        
        /**
         * This constructor builds a pair of switcher-style buttons which alternate
         * between window and dataset view.
         * @param windowView the window view.
         * @param datasetView the dataset view.
         */
        public WindowTab(final MultiView windowView, final View datasetView) {
            super(new BorderLayout());
            // Create display part of the tab.
            this.displayArea = new JPanel(new CardLayout());
            // Dataset card first.
            JScrollPane scroller = new JScrollPane(datasetView.asJComponent());
            scroller.getViewport().setBackground(datasetView.asJComponent().getBackground());
            this.displayArea.add(scroller, "DATASET_CARD");
            this.cardToView.put("DATASET_CARD", datasetView);
            // Window card next.
            scroller = new JScrollPane(windowView.asJComponent());
            scroller.getViewport().setBackground(windowView.asJComponent().getBackground());
            this.displayArea.add(scroller, "WINDOW_CARD");
            this.cardToView.put("WINDOW_CARD", windowView);
            // Create switcher part of the tab.
            JPanel switcher = new JPanel();
            // Dataset button.
            final JRadioButton datasetButton = new JRadioButton(BuilderBundle.getString("datasetButtonName"));
            datasetButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == datasetButton) {
                        CardLayout cards = (CardLayout)displayArea.getLayout();
                        cards.show(displayArea, "DATASET_CARD");
                        datasetView.recalculateView();
                    }
                }
            });
            switcher.add(datasetButton);
            // Window button.
            final JRadioButton windowButton = new JRadioButton(BuilderBundle.getString("windowButtonName"));
            windowButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == windowButton) {
                        CardLayout cards = (CardLayout)displayArea.getLayout();
                        cards.show(displayArea, "WINDOW_CARD");
                        windowView.recalculateView();
                    }
                }
            });
            switcher.add(windowButton);
            // Make buttons mutually exclusive.
            ButtonGroup buttons = new ButtonGroup();
            buttons.add(windowButton);
            buttons.add(datasetButton);
            // Add the components to the panel.
            this.add(switcher, BorderLayout.NORTH);
            this.add(displayArea, BorderLayout.CENTER);
            // Set our preferred size to the dataset size plus a bit on top for the switcher buttons.
            Dimension preferredSize = datasetView.asJComponent().getPreferredSize();
            double extraHeight = datasetButton.getHeight();
            preferredSize.setSize(preferredSize.getWidth(), preferredSize.getHeight()+extraHeight);
            this.setPreferredSize(preferredSize);
            // Select the default one (dataset).
            datasetButton.setSelected(true);
            ((CardLayout)this.displayArea.getLayout()).show(displayArea, "DATASET_CARD");
        }
        
        /**
         * Recalculates the way to display what we are seeing at the moment.
         */
        public void recalculateView() {
            String visibleCard = ((CardLayout)this.displayArea.getLayout()).toString();
            View view = (View)this.cardToView.get(visibleCard);
            view.recalculateView();
        }
        
        /**
         * Resyncs the table providers within each multi table provider tab.
         * @param newTableProviders the new set of table providers to display.
         */
        public void resyncTableProviders(Collection newTableProviders) {
            ((MultiView)this.cardToView.get("WINDOW_CARD")).resyncTableProviders(newTableProviders);
        }
    }
}
