/*
 * TableProviderTabSet.java
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
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public class TableProviderTabSet extends JTabbedPane implements MultiView {
    /**
     * Internal reference to the list of table providers, in order, mapped
     * to their tableProviderToView.
     */
    private final Map tableProviderToView = new HashMap();
    
    /**
     * Internal reference to the list of table providers, in order, mapped
     * from their tableProviderToView.
     */
    private final Map viewToTableProvider = new HashMap();
    
    /**
     * Internal reference to the listener for the providers we are viewing.
     */
    private Listener listener;
    
    /**
     * Creates a new multiple table provider view over the given set of
     * of table providers.
     * 
     * @param tableProviders the table providers to view.
     */
    public TableProviderTabSet(Collection tableProviders) {
        super();
        // Add the overview tab to ourselves.
        MultiView overviewView = new MultiTableProviderView(tableProviders);
        JScrollPane scroller = new JScrollPane(overviewView.asJComponent());
        scroller.getViewport().setBackground(overviewView.asJComponent().getBackground());
        this.addTab(BuilderBundle.getString("multiTblProvOverviewTab"), scroller);
        // Add the rest of the tabs.
        this.resyncTableProviders(tableProviders);
    }
    
    /**
     * Resyncs the table providers with the contents of the set.
     * 
     * @param newTableProviders the table providers to view.
     */
    public void resyncTableProviders(Collection newTableProviders) {
        // Update the overview tab first.
        ((MultiView)((JScrollPane)this.getComponentAt(0)).getViewport().getView()).resyncTableProviders(newTableProviders);
        // Form a list to remove.
        List removedTableProviders = new ArrayList(this.tableProviderToView.keySet());
        removedTableProviders.removeAll(newTableProviders);
        // Form a list to add.
        List addedTableProviders = new ArrayList(newTableProviders);
        addedTableProviders.removeAll(this.tableProviderToView.keySet());
        // Remove the removedTableProviders ones.
        for (Iterator i = removedTableProviders.iterator(); i.hasNext(); ) {
            TableProvider tableProvider = (TableProvider)i.next();
            View tableProviderView = (View)this.tableProviderToView.get(tableProvider);
            this.tableProviderToView.remove(tableProvider);
            this.viewToTableProvider.remove(tableProviderView);
            this.removeTabAt(this.indexOfTab(tableProvider.getName()));
        }
        // Add the addedTableProviders ones.
        for (Iterator i = addedTableProviders.iterator(); i.hasNext(); ) {
            TableProvider tableProvider = (TableProvider)i.next();
            // Create and add the tab.
            View tableProviderView = new TableProviderView(tableProvider);
            JScrollPane scroller = new JScrollPane(tableProviderView.asJComponent());
            scroller.getViewport().setBackground(tableProviderView.asJComponent().getBackground());
            this.addTab(tableProvider.getName(), scroller);
            // Remember the view.
            this.tableProviderToView.put(tableProvider, tableProviderView);
            this.viewToTableProvider.put(tableProviderView, tableProvider);
            // Set the listener on the view (if it's been set yet, which it won't be from the constructor)
            if (this.getListener() != null) tableProviderView.setListener(this.getListener());
        }
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
                View view =
                        (View)((JScrollPane)this.getComponentAt(index)).getViewport().getView();
                TableProvider tableProvider = (TableProvider)viewToTableProvider.get(view);
                this.getTblProvTabContextMenu(tableProvider).show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            } 
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * Construct a context menu for a given table provider view tab.
     * @param tableProvider the table provider to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getTblProvTabContextMenu(final TableProvider tableProvider) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem close = new JMenuItem(BuilderBundle.getString("removeTblProvTitle", tableProvider.getName()));
        close.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getListener().requestRemoveTableProvider(tableProvider);
            }
        });
        contextMenu.add(close);
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setListener(Listener listener) throws NullPointerException {
        if (listener==null) 
            throw new NullPointerException(BuilderBundle.getString("listenerIsNull"));
        this.listener = listener;
        for (int i = 0; i < this.getTabCount(); i++) {
            View view = (View)((JScrollPane)this.getComponentAt(i)).getViewport().getView();
            view.setListener(listener);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Listener getListener() {
        return this.listener;
    }
    
    /**
     * {@inheritDoc}
     */
    public JComponent asJComponent() {
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    public void recalculateView() {
        int index = this.getSelectedIndex();
        if (index >= 0) {
            View view = (View)((JScrollPane)this.getComponentAt(index)).getViewport().getView();
            view.recalculateView();
        } 
    }
    
}
