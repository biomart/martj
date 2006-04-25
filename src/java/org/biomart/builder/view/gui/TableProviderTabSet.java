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
    private Map tableProviderToView = new HashMap();
    
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
    private TableProviderView tableProviderView;
    
    /**
     * Our new provider dialog.
     */
    private NewTableProviderDialog newTableProviderDialog;
    
    /**
     * Creates a new multiple table provider view over the given set of
     * of table providers.
     */
    public TableProviderTabSet(WindowTabSet windowTabSet) {
        super();
        this.windowTabSet = windowTabSet;
        // Add the overview tab to ourselves.
        this.tableProviderView = new TableProviderView(this.windowTabSet);
        JScrollPane scroller = new JScrollPane(this.tableProviderView);
        scroller.getViewport().setBackground(this.tableProviderView.getBackground());
        this.addTab(BuilderBundle.getString("multiTblProvOverviewTab"), scroller);
        // Make our own table provider dialog.
        this.newTableProviderDialog = new NewTableProviderDialog(this);
        // Synchronise ourselves.
        this.synchroniseTabs();
    }
    
    /**
     * Who's our mummy?
     */
    public WindowTabSet getWindowTabSet() {
        return this.windowTabSet;
    }
    
    /**
     * Makes sure we are displaying the correct set of table providers.
     */
    public void synchroniseTabs() {
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
        this.tableProviderView.synchroniseView();
        // Synchronise our tab view contents.
        for (int i = 1; i < this.getTabCount(); i++) {
            JScrollPane scroller = (JScrollPane)this.getComponentAt(i);
            TableView tableView = (TableView)scroller.getViewport().getView();
            tableView.synchroniseView();
        }
        // Redraw.
        this.validate();
    }
    
    /**
     * Confirms with user then removes a table provider.
     */
    public void requestAddTableProvider() {
        // Pop up a box to get the details of the new provider.
        this.newTableProviderDialog.show();
        TableProvider tableProvider = this.newTableProviderDialog.getTableProvider();
        // Add to schema.
        try {
            if (tableProvider != null) {
                this.windowTabSet.getSchema().addTableProvider(tableProvider);
                this.addTableProviderTab(tableProvider);
                this.windowTabSet.getSchemaTabSet().setModifiedStatus(true);
            }
        } catch (Throwable t) {
            this.windowTabSet.getSchemaTabSet().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Adds a new table provider to our tabs.
     */
    private void addTableProviderTab(TableProvider tableProvider) {
        // Create and add the tab.
        TableView tableView = new TableView(this.windowTabSet, tableProvider);
        JScrollPane scroller = new JScrollPane(tableView);
        scroller.getViewport().setBackground(tableView.getBackground());
        this.addTab(tableProvider.getName(), scroller);
        // Remember the view.
        this.tableProviderToView.put(tableProvider, tableView);
        // Set the adaptor on the view.
        tableView.setAdaptor(this.getAdaptor());
        this.tableProviderView.synchroniseView();
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
                this.windowTabSet.synchroniseTabs();
                this.windowTabSet.getSchemaTabSet().setModifiedStatus(true);
            } catch (Throwable t) {
                this.windowTabSet.getSchemaTabSet().getMartBuilder().showStackTrace(t);
            }
        }
    }
    
    /**
     * Removes a table provider from our tabs.
     */
    private void removeTableProviderTab(TableProvider tableProvider) {
        TableView tableView = (TableView)this.tableProviderToView.get(tableProvider);
        this.removeTabAt(this.indexOfTab(tableProvider.getName()));
        this.tableProviderToView.remove(tableProvider);
    }
    
    /**
     * Prompt for a name for a window.
     */
    private String getTableProviderName(String defaultResponse) {
        // Get one from user.
        String name = (String)JOptionPane.showInputDialog(
                this.windowTabSet.getSchemaTabSet().getMartBuilder(),
                BuilderBundle.getString("requestTblProvName"),
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
    public void renameTableProvider(TableProvider tableProvider) {
        // Update the table provider name and the tab name.
        try {
            String newName = this.getTableProviderName(tableProvider.getName());
            if (newName != null && !newName.equals(tableProvider.getName())) {
                this.removeTableProviderTab(tableProvider);
                this.windowTabSet.getSchema().renameTableProvider(tableProvider, newName);
                this.addTableProviderTab(tableProvider);
                this.setSelectedIndex(this.indexOfTab(newName));
                this.windowTabSet.getSchemaTabSet().setModifiedStatus(true);
            }
        } catch (Throwable t) {
            this.windowTabSet.getSchemaTabSet().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Syncs this table provider individually against the database.
     */
    public void synchroniseTableProvider(TableProvider tableProvider) {
        try {
            tableProvider.synchronise();
            this.windowTabSet.synchroniseTabs();
        } catch (Throwable t) {
            this.windowTabSet.getSchemaTabSet().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Test this table provider individually against the database.
     */
    public void testTableProvider(TableProvider tableProvider) {
        boolean passedTest = false;
        try {
            passedTest = tableProvider.test();
        } catch (Throwable t) {
            passedTest = false;
            this.windowTabSet.getSchemaTabSet().getMartBuilder().showStackTrace(t);
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
     * Construct a context menu for a given table provider view tab.
     * @param tableProvider the table provider to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getTblProvTabContextMenu(final TableProvider tableProvider) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem close = new JMenuItem(BuilderBundle.getString("removeTblProvTitle"));
        close.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmRemoveTableProvider(tableProvider);
            }
        });
        contextMenu.add(close);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameTblProvTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameTblProvMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                renameTableProvider(tableProvider);
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
                if (selectedComponent instanceof JScrollPane) {
                    Component selectedView = ((JScrollPane)selectedComponent).getViewport().getView();
                    if (selectedView instanceof TableView) {
                        this.setSelectedIndex(selectedIndex);
                        TableProvider tableProvider = ((TableView)selectedView).getTableProvider();
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
