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
import org.biomart.builder.controller.SchemaTools;
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
     * to their tableProviderToDiagram.
     */
    private Map tableProviderToDiagram = new HashMap();
    
    /**
     * Internal reference to the adaptor for the providers we are viewing.
     */
    private DiagramModifier adaptor;
    
    /**
     * The window tab set we belong to.
     */
    private WindowTabSet windowTabSet;
    
    /**
     * Our overview tab.
     */
    private TableProviderDiagram tableProviderDiagram;
    
    /**
     * Creates a new multiple table provider view over the given set of
     * of table providers.
     */
    public TableProviderTabSet(WindowTabSet windowTabSet) {
        super();
        this.windowTabSet = windowTabSet;
        // Add the overview tab to ourselves.
        this.tableProviderDiagram = new TableProviderDiagram(this.windowTabSet);
        JScrollPane scroller = new JScrollPane(this.tableProviderDiagram);
        scroller.getViewport().setBackground(this.tableProviderDiagram.getBackground());
        this.addTab(BuilderBundle.getString("multiTblProvOverviewTab"), scroller);
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
            if (!this.tableProviderToDiagram.containsKey(tableProvider)) this.addTableProviderTab(tableProvider);
        }
        // Remove all our table providers that are not in the schema.
        List candidates = new ArrayList(this.tableProviderToDiagram.keySet());
        for (Iterator i = candidates.iterator(); i.hasNext(); ) {
            TableProvider tableProvider = (TableProvider)i.next();
            if (!schemaTableProviders.contains(tableProvider)) this.removeTableProviderTab(tableProvider);
        }
        // Synchronise our overview tab.
        this.tableProviderDiagram.synchroniseDiagram();
        // Synchronise our tab view contents.
        for (int i = 1; i < this.getTabCount(); i++) {
            JScrollPane scroller = (JScrollPane)this.getComponentAt(i);
            TableDiagram tableDiagram = (TableDiagram)scroller.getViewport().getView();
            tableDiagram.synchroniseDiagram();
        }
        // Redraw.
        this.validate();
    }
    
    /**
     * Confirms with user then removes a table provider.
     */
    public void requestAddTableProvider() {
        // Interpret the response.
        TableProvider tableProvider = TableProviderDialog.createTableProvider(this);
        // Add to schema.
        try {
            if (tableProvider != null) {
                SchemaTools.addTableProviderToSchema(this.windowTabSet.getSchema(), tableProvider);
                this.synchroniseTableProvider(tableProvider);
                this.synchroniseTabs();
                this.windowTabSet.getSchemaTabSet().setModifiedStatus(true);
            }
        } catch (Throwable t) {
            this.windowTabSet.getSchemaTabSet().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Confirms with user then removes a table provider.
     */
    public void requestModifyTableProvider(TableProvider tableProvider) {
        // Add to schema.
        try {
            if (TableProviderDialog.modifyTableProvider(this, tableProvider)) {
                this.synchroniseTableProvider(tableProvider);
                this.synchroniseTabs();
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
        TableDiagram tableDiagram = new TableDiagram(this.windowTabSet, tableProvider);
        JScrollPane scroller = new JScrollPane(tableDiagram);
        scroller.getViewport().setBackground(tableDiagram.getBackground());
        this.addTab(tableProvider.getName(), scroller);
        // Remember the view.
        this.tableProviderToDiagram.put(tableProvider, tableDiagram);
        // Set the adaptor on the view.
        tableDiagram.setAdaptor(this.getAdaptor());
        this.tableProviderDiagram.synchroniseDiagram();
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
                SchemaTools.removeTableProviderFromSchema(this.windowTabSet.getSchema(), tableProvider);
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
        TableDiagram tableDiagram = (TableDiagram)this.tableProviderToDiagram.get(tableProvider);
        this.removeTabAt(this.indexOfTab(tableProvider.getName()));
        this.tableProviderToDiagram.remove(tableProvider);
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
                SchemaTools.renameTableProvider(this.windowTabSet.getSchema(), tableProvider, newName);
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
            SchemaTools.synchroniseTableProvider(tableProvider);
            this.windowTabSet.synchroniseTabs();
            this.windowTabSet.getSchemaTabSet().setModifiedStatus(true);
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
            passedTest = SchemaTools.testTableProvider(tableProvider);
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
                    Component selectedDiagram = ((JScrollPane)selectedComponent).getViewport().getView();
                    if (selectedDiagram instanceof TableDiagram) {
                        this.setSelectedIndex(selectedIndex);
                        TableProvider tableProvider = ((TableDiagram)selectedDiagram).getTableProvider();
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
    public void setAdaptor(DiagramModifier adaptor) throws NullPointerException {
        if (adaptor==null)
            throw new NullPointerException(BuilderBundle.getString("adaptorIsNull"));
        this.adaptor = adaptor;
        for (int i = 0; i < this.getTabCount(); i++) {
            Diagram diagram = (Diagram)((JScrollPane)this.getComponentAt(i)).getViewport().getView();
            diagram.setAdaptor(adaptor);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public DiagramModifier getAdaptor() {
        return this.adaptor;
    }
}
