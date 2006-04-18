/*
 * MultiTableProviderView.java
 *
 * Created on 11 April 2006, 16:00
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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of multiple {@link TableProvider}s in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class MultiTableProviderView extends JTabbedPane {
    /**
     * Internal reference to the list of table providers, in order, mapped
     * to their views.
     */
    private final Map tblProvViews = new HashMap();
    
    /**
     * Internal reference to the listener for the providers we are viewing.
     */
    private TableProviderListener tblProvListener;
    
    /**
     * Creates a new multiple table provider view over the given set of
     * of table providers.
     * @param tblProvs the table providers to view.
     */
    public MultiTableProviderView(Collection tblProvs) {
        super();
        // Add the overview to ourselves.
        OverviewView overviewView = new OverviewView(this);
        JScrollPane overviewScroller = new JScrollPane(overviewView);
        overviewScroller.getViewport().setBackground(overviewView.getBackground());
        this.addTab(BuilderBundle.getString("multiTblProvOverviewTab"), overviewScroller);
        // Add the rest of the tabs.
        this.resyncTableProviderTabs(tblProvs);
    }
    
    /**
     * Resyncs the table providers with the contents of the set.
     * @param tblProvs the table providers to view.
     */
    public void resyncTableProviderTabs(Collection tblProvs) {
        // Form a list to remove.
        List removed = new ArrayList(this.tblProvViews.keySet());
        removed.removeAll(tblProvs);
        // Form a list to add.
        List added = new ArrayList(tblProvs);
        added.removeAll(this.tblProvViews.keySet());
        // Remove the removed ones.
        for (Iterator i = removed.iterator(); i.hasNext(); ) {
            TableProvider tblProv = (TableProvider)i.next();
            this.tblProvViews.remove(tblProv);
            this.removeTabAt(this.indexOfTab(tblProv.getName()));
        }
        // Add the added ones.
        for (Iterator i = added.iterator(); i.hasNext(); ) {
            TableProvider tblProv = (TableProvider)i.next();
            TableProviderView tblProvView = new TableProviderView(tblProv);
            this.tblProvViews.put(tblProv, tblProvView);
            JScrollPane tblProvScroller = new JScrollPane(tblProvView);
            tblProvScroller.getViewport().setBackground(tblProvView.getBackground());
            this.addTab(tblProv.getName(), tblProvScroller);
        }
        // Reset the listeners.
        this.setTableProviderListener(this.tblProvListener);
        // Select the overview tab for safety.
        this.setSelectedIndex(0);
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercepts tab selection in order to recalculate the visible view.</p>
     */
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        this.recalculateVisibleView();
    }
    
    /**
     * Recalculates the way to display what we are seeing at the moment.
     */
    public void recalculateVisibleView() {
        int index = this.getSelectedIndex();
        if (index > 0) {
            TableProviderView tblProvView =
                    (TableProviderView)((JScrollPane)this.getComponentAt(index)).getViewport().getView();
            tblProvView.recalculateView();
        } else if (index == 0) {
            OverviewView overviewView =
                    (OverviewView)((JScrollPane)this.getComponentAt(0)).getViewport().getView();
            overviewView.recalculateView();
        }
    }
    
    /**
     * Returns the map of table providers to table provider views.
     * @return the map of table providers to views.
     */
    public Map getTableProviders() {
        return this.tblProvViews;
    }
    
    /**
     * Sets the listener to use.
     * @param tblProvListener the listener that will be told when the view is interacted with.
     */
    public void setTableProviderListener(TableProviderListener tblProvListener) {
        this.tblProvListener = tblProvListener;
        for (Iterator i = this.tblProvViews.values().iterator(); i.hasNext(); ) {
            TableProviderView tblProvView = (TableProviderView)i.next();
            tblProvView.setTableProviderListener(this.tblProvListener);
        }
    }
    
    /**
     * Returns the listener to use.
     * @return tblProvListener the listener that will be told when the view is interacted with.
     */
    public TableProviderListener getTableProviderListener() {
        return this.tblProvListener;
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.getID() == MouseEvent.MOUSE_PRESSED && evt.getButton() == MouseEvent.BUTTON3) {
            // Where was the click?
            int index = this.indexAtLocation(evt.getX(), evt.getY());
            // Respond appropriately.
            if (index > 0) {
                TableProviderView tblProvView =
                        (TableProviderView)((JScrollPane)this.getComponentAt(index)).getViewport().getView();
                this.getTblProvTabContextMenu(tblProvView).show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            } 
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * Construct a context menu for a given table provider view tab.
     * @param tblProvView the table provider view to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getTblProvTabContextMenu(final TableProviderView tblProvView) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem close = new JMenuItem(BuilderBundle.getString("removeTblProvTitle", tblProvView.getTableProvider().getName()));
        close.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tblProvView.getTableProviderListener().removeTableProvider(tblProvView.getTableProvider());
            }
        });
        contextMenu.add(close);
        return contextMenu;
    }
    
    /**
     * This class deals with drawing an overview of all the table providers.
     */
    private static class OverviewView extends JComponent {
        /**
         * Static reference to the background colour to use for components.
         */
        public static final Color BACKGROUND_COLOUR = Color.YELLOW;
        
        /**
         * Internal reference to the parent view.
         */
        private MultiTableProviderView multiTblProvView;
        
        /**
         * The constructor rememembers the MultiTableProviderView that this
         * is part of.
         */
        public OverviewView(MultiTableProviderView multiTblProvView) {
            super();
            this.multiTblProvView = multiTblProvView;
            this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            this.setBackground(OverviewView.BACKGROUND_COLOUR);
        }
        
        /**
         * Construct a context menu for a given multi table provider view.
         * @param displayComponent the display component we wish to customise this menu to. It may
         * be null, so watch out for this.
         * @return the popup menu.
         */
        private JPopupMenu getContextMenu(Object displayComponent) {
            JPopupMenu contextMenu = new JPopupMenu();
            final JMenuItem redraw = new JMenuItem(BuilderBundle.getString("redrawTitle"));
            redraw.setMnemonic(BuilderBundle.getString("redrawMnemonic").charAt(0));
            redraw.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    multiTblProvView.getTableProviderListener().requestRecalculateVisibleView();
                }
            });
            contextMenu.add(redraw);
            contextMenu.addSeparator();
            final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseAllTitle"));
            sync.setMnemonic(BuilderBundle.getString("synchroniseAllMnemonic").charAt(0));
            sync.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (evt.getSource()==sync) multiTblProvView.getTableProviderListener().synchroniseAll();
                }
            });
            contextMenu.add(sync);
            // Extend and return.
            this.multiTblProvView.getTableProviderListener().customiseContextMenu(contextMenu, displayComponent);
            return contextMenu;
        }
        
        /**
         * {@inheritDoc}
         * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
         */
        protected void processMouseEvent(MouseEvent evt) {
            boolean eventProcessed = false;
            // Is it a right-click?
            if (evt.getID() == MouseEvent.MOUSE_PRESSED && evt.getButton() == MouseEvent.BUTTON3) {
                // Where was the click?
                Object component = this.getDisplayComponentAtLocation(evt.getPoint());
                // Only respond to individual table providers, not the overview tab.
                this.getContextMenu(component).show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            }
            // Pass it on up if we're not interested.
            if (!eventProcessed) super.processMouseEvent(evt);
        }
        
        /**
         * Works out what's at a given point.
         * @param location the point to look at.
         * @return the display component at that point, or null if nothing there.
         */
        private Object getDisplayComponentAtLocation(Point location) {
            return null;
        }
        
        /**
         * Recalculates the way to display what we see.
         */
        public void recalculateView() {
            // Nothing, yet.
        }
        
        /**
         * {@inheritDoc}
         */
        public Dimension getPreferredSize() {
            return new Dimension(200,200);
        }
        
        /**
         * {@inheritDoc}
         */
        protected void paintComponent(Graphics g) {
            // Paint background.
            if (this.isOpaque()) {
                g.setColor(this.getBackground());
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            Graphics2D g2d = (Graphics2D)g.create();
            // Do painting of this table provider overview.
            // Clean up.
            g2d.dispose();
        }
    }
}
