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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of multiple {@link TableProvider}s in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 21st April 2006
 * @since 0.1
 */
public class TableProviderTabSet extends JTabbedPane {
    /**
     * Internal reference to the list of table providers, in order, mapped
     * to their tableProviderToView.
     */
    private final Map tableProviderToView = new HashMap();
    
    /**
     * Internal reference to the adaptor for the providers we are viewing.
     */
    private Adaptor adaptor;
    
    /**
     * The window tab set we belong to.
     */
    private WindowTabSet windowTabSet;
    
    /**
     * Our overview tab.
     */
    private MultiTableProviderView multiTableProviderView;
    
    /**
     * Creates a new multiple table provider view over the given set of
     * of table providers.
     */
    public TableProviderTabSet(WindowTabSet windowTabSet) {
        super();
        this.windowTabSet = windowTabSet;
        // Add the overview tab to ourselves.
        this.multiTableProviderView = new MultiTableProviderView(this.windowTabSet);
        JScrollPane scroller = new JScrollPane(this.multiTableProviderView);
        scroller.getViewport().setBackground(this.multiTableProviderView.getBackground());
        this.addTab(BuilderBundle.getString("multiTblProvOverviewTab"), scroller);
        // Synchronise ourselves.
        this.synchronise();
    }
    
    /**
     * Makes sure we are displaying the correct set of table providers.
     */
    public void synchronise() {
        // Add all table providers that we don't have yet.
        List schemaTableProviders = new ArrayList(this.windowTabSet.getSchema().getTableProviders());
        for (Iterator i = schemaTableProviders.iterator(); i.hasNext(); ) {
            TableProvider tableProvider = (TableProvider)i.next();
            if (!this.tableProviderToView.containsKey(tableProvider)) this.addTableProviderTab(tableProvider);
        }
        // Remove all our table providers that are not in the schema.
        List candidates = new ArrayList(this.tableProviderToView.keySet());
        for (Iterator i = candidates.iterator(); i.hasNext(); ) {
            TableProvider tableProvider = (TableProvider)i.next();
            if (!schemaTableProviders.contains(tableProvider)) this.removeTableProviderTab(tableProvider);
        }
        // Synchronise our overview tab.
        this.multiTableProviderView.synchronise();
        // Synchronise our tab view contents.
        for (int i = 1; i < this.getTabCount(); i++) {
            JScrollPane scroller = (JScrollPane)this.getComponentAt(i);
            TableProviderView tableProviderView = (TableProviderView)scroller.getViewport().getView();
            tableProviderView.synchronise();
        }
        // Redraw.
        this.validate();
    }
    
    /**
     * Adds a new table provider to our tabs.
     */
    private void addTableProviderTab(TableProvider tableProvider) {
        // Create and add the tab.
        TableProviderView tableProviderView = new TableProviderView(this.windowTabSet, tableProvider);
        JScrollPane scroller = new JScrollPane(tableProviderView);
        scroller.getViewport().setBackground(tableProviderView.getBackground());
        this.addTab(tableProvider.getName(), scroller);
        // Remember the view.
        this.tableProviderToView.put(tableProvider, tableProviderView);
        // Set the adaptor on the view.
        tableProviderView.setAdaptor(this.getAdaptor());
        this.multiTableProviderView.synchronise();
    }
    
    /**
     * Confirms with user then removes a table provider.
     */
    public void confirmRemoveTableProvider(TableProvider tableProvider) {
        // Must confirm action first.
        int choice = JOptionPane.showConfirmDialog(
                this,
                BuilderBundle.getString("confirmDelTblProv"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.YES_NO_OPTION
                );
        if (choice == JOptionPane.YES_OPTION) {
            try {
                this.windowTabSet.getSchema().removeTableProvider(tableProvider);
                this.removeTableProviderTab(tableProvider);
                this.windowTabSet.synchronise();
                this.windowTabSet.getSchemaManager().setModifiedStatus(true);
            } catch (Throwable t) {
                this.windowTabSet.getSchemaManager().getMartBuilder().showStackTrace(t);
            }
        }
    }
    
    /**
     * Removes a table provider from our tabs.
     */
    private void removeTableProviderTab(TableProvider tableProvider) {
        TableProviderView tableProviderView = (TableProviderView)this.tableProviderToView.get(tableProvider);
        this.removeTabAt(this.indexOfTab(tableProvider.getName()));
        this.tableProviderToView.remove(tableProvider);
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
                confirmRemoveTableProvider(tableProvider);
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
                Component selectedComponent = this.getComponentAt(selectedIndex);
                // Respond appropriately.
                if (selectedComponent instanceof JScrollPane) {
                    Component selectedView = ((JScrollPane)selectedComponent).getViewport().getView();
                    if (selectedView instanceof TableProviderView) {
                        this.setSelectedIndex(selectedIndex);
                        TableProvider tableProvider = ((TableProviderView)selectedView).getTableProvider();
                        this.getTblProvTabContextMenu(tableProvider).show(this, evt.getX(), evt.getY());
                        eventProcessed = true;
                    }
                }
            }
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setAdaptor(Adaptor adaptor) throws NullPointerException {
        if (adaptor==null)
            throw new NullPointerException(BuilderBundle.getString("adaptorIsNull"));
        this.adaptor = adaptor;
        for (int i = 0; i < this.getTabCount(); i++) {
            View view = (View)((JScrollPane)this.getComponentAt(i)).getViewport().getView();
            view.setAdaptor(adaptor);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Adaptor getAdaptor() {
        return this.adaptor;
    }
}
