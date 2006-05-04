/*
 * SchemaTabSet.java
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
import org.biomart.builder.controller.MartUtils;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of multiple {@link Schema}s in graphical form.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 27th April 2006
 * @since 0.1
 */
public class SchemaTabSet extends JTabbedPane {
    /**
     * Internal reference to the list of table providers, in order, mapped
     * to their schemaToDiagram.
     */
    private Map schemaToDiagram = new HashMap();
    
    /**
     * Internal reference to the diagramModifier for the providers we are viewing.
     */
    private DiagramModifier diagramModifier;
    
    /**
     * The window tab set we belong to.
     */
    private DataSetTabSet datasetTabSet;
    
    /**
     * Our overview tab.
     */
    private AllSchemasDiagram allSchemasDiagram;
    
    /**
     * Creates a new multiple table provider view over the given set of
     * of table providers.
     */
    public SchemaTabSet(DataSetTabSet datasetTabSet) {
        super();
        this.datasetTabSet = datasetTabSet;
        // Add the overview tab to ourselves.
        this.allSchemasDiagram = new AllSchemasDiagram(this.datasetTabSet);
        JScrollPane scroller = new JScrollPane(this.allSchemasDiagram);
        scroller.getViewport().setBackground(this.allSchemasDiagram.getBackground());
        this.addTab(BuilderBundle.getString("multiSchemaOverviewTab"), scroller);
        // Synchronise ourselves.
        this.synchroniseTabs();
    }
    
    /**
     * Who's our mummy?
     */
    public DataSetTabSet getDataSetTabSet() {
        return this.datasetTabSet;
    }
    
    /**
     * Makes sure we are displaying the correct set of table providers.
     */
    public void synchroniseTabs() {
        // Add all table providers that we don't have yet.
        List martSchemas = new ArrayList(datasetTabSet.getMart().getSchemas());
        for (Iterator i = martSchemas.iterator(); i.hasNext(); ) {
            Schema schema = (Schema)i.next();
            if (!schemaToDiagram.containsKey(schema)) addSchemaTab(schema);
        }
        // Remove all our table providers that are not in the schema.
        List ourSchemas = new ArrayList(schemaToDiagram.keySet());
        for (Iterator i = ourSchemas.iterator(); i.hasNext(); ) {
            Schema schema = (Schema)i.next();
            if (!martSchemas.contains(schema)) removeSchemaTab(schema);
        }
        // Synchronise our overview tab.
        allSchemasDiagram.synchroniseDiagram();
        // Synchronise our tab view contents.
        for (int i = 1; i < getTabCount(); i++) {
            JScrollPane scroller = (JScrollPane)getComponentAt(i);
            SchemaDiagram tableDiagram = (SchemaDiagram)scroller.getViewport().getView();
            tableDiagram.synchroniseDiagram();
        }
        // Redraw.
        validate();
    }
    
    /**
     * Confirms with user then adds a table provider.
     */
    public void requestAddSchema() {
        // Interpret the response.
        final Schema schema = SchemaManagementDialog.createSchema(this);
        LongProcess.run(this, new Runnable() {
            public void run() {
                // Add to schema.
                try {
                    if (schema != null) {
                        MartUtils.addSchemaToMart(datasetTabSet.getMart(), schema);
                        synchroniseSchema(schema);
                        synchroniseTabs();
                        datasetTabSet.getMartTabSet().setModifiedStatus(true);
                    }
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    public void requestAddSchemaToSchemaGroup(final Schema schema) {
        // Work out existing group names, if any.
        List groupSchemas = new ArrayList();
        String newGroupName = BuilderBundle.getString("newSchemaGroup");
        groupSchemas.add(newGroupName);
        for (Iterator i = this.datasetTabSet.getMart().getSchemas().iterator(); i.hasNext(); ) {
            Schema groupSchema = (Schema)i.next();
            if (groupSchema instanceof SchemaGroup) groupSchemas.add(groupSchema.getName());
        }
        // Obtain group name from user
        String groupName = (String)JOptionPane.showInputDialog(
                this.datasetTabSet.getMartTabSet().getMartBuilder(),
                BuilderBundle.getString("requestSchemaGroupName"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                groupSchemas.toArray(),
                newGroupName
                );
        // If they chose 'new group', get the new group name from a second dialog.
        if (groupName!=null && groupName.equals(newGroupName)) {
            groupName = (String)JOptionPane.showInputDialog(
                    this.datasetTabSet.getMartTabSet().getMartBuilder(),
                    BuilderBundle.getString("requestNewSchemaGroupName"),
                    BuilderBundle.getString("questionTitle"),
                    JOptionPane.QUESTION_MESSAGE
                    );
        }
        // Add schema to group
        try {
            if (groupName == null) return;
            else if (groupName.trim().length()==0) {
                throw new ValidationException(BuilderBundle.getString("schemaGroupNameIsNull"));
            } else {
                final String groupNameRef = groupName;
                LongProcess.run(this, new Runnable() {
                    public void run() {
                        try {
                            SchemaGroup group = MartUtils.addSchemaToSchemaGroup(datasetTabSet.getMart(), schema, groupNameRef);
                            datasetTabSet.synchroniseTabs(); // Some datasets may disappear.
                            datasetTabSet.getMartTabSet().setModifiedStatus(true);
                        } catch (Throwable t) {
                            datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Confirms with user then removes a table provider.
     */
    public void requestModifySchema(Schema schema) {
        // Add to schema.
        try {
            if (SchemaManagementDialog.modifySchema(this, schema))
                this.datasetTabSet.getMartTabSet().setModifiedStatus(true);
        } catch (Throwable t) {
            this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Adds a new table provider to our tabs.
     */
    private void addSchemaTab(Schema schema) {
        // Create and add the tab.
        SchemaDiagram schemaDiagram = new SchemaDiagram(this.datasetTabSet, schema);
        JScrollPane scroller = new JScrollPane(schemaDiagram);
        scroller.getViewport().setBackground(schemaDiagram.getBackground());
        this.addTab(schema.getName(), scroller);
        // Remember the view.
        this.schemaToDiagram.put(schema, schemaDiagram);
        // Set the diagramModifier on the view.
        schemaDiagram.setDiagramModifier(this.getDiagramModifier());
        this.allSchemasDiagram.synchroniseDiagram();
    }
    
    /**
     * Confirms with user then removes a table provider.
     */
    public void confirmRemoveSchema(final Schema schema) {
        // Must confirm action first.
        int choice = JOptionPane.showConfirmDialog(
                this,
                BuilderBundle.getString("confirmDelSchema"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.YES_NO_OPTION
                );
        if (choice == JOptionPane.YES_OPTION) {
            LongProcess.run(this, new Runnable() {
                public void run() {
                    try {
                        MartUtils.removeSchemaFromMart(datasetTabSet.getMart(), schema);
                        datasetTabSet.synchroniseTabs(); // Some datasets may disappear.
                        datasetTabSet.getMartTabSet().setModifiedStatus(true);
                    } catch (Throwable t) {
                        datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                    }
                }
            });
        }
    }
    
    public void confirmRemoveSchemaFromSchemaGroup(final Schema schema, final SchemaGroup schemaGroup) {
        // Must confirm action first.
        int choice = JOptionPane.showConfirmDialog(
                this,
                BuilderBundle.getString("confirmUngroupSchema"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.YES_NO_OPTION
                );
        if (choice == JOptionPane.YES_OPTION) {
            LongProcess.run(this, new Runnable() {
                public void run() {
                    try {
                        MartUtils.removeSchemaFromSchemaGroup(datasetTabSet.getMart(), schema, schemaGroup);
                        datasetTabSet.synchroniseTabs(); // Some datasets may disappear.
                        datasetTabSet.getMartTabSet().setModifiedStatus(true);
                    } catch (Throwable t) {
                        datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                    }
                }
            });
        }
    }
    
    /**
     * Removes a table provider from our tabs.
     */
    private void removeSchemaTab(Schema schema) {
        SchemaDiagram schemaDiagram = (SchemaDiagram)this.schemaToDiagram.get(schema);
        this.removeTabAt(this.indexOfTab(schema.getName()));
        this.schemaToDiagram.remove(schema);
    }
    
    /**
     * Prompt for a name for a window.
     */
    private String getSchemaName(String defaultResponse) {
        // Get one from user.
        String name = (String)JOptionPane.showInputDialog(
                this.datasetTabSet.getMartTabSet().getMartBuilder(),
                BuilderBundle.getString("requestSchemaName"),
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
    public void renameSchema(Schema schema, boolean isInGroup) {
        // Update the table provider name and the tab name.
        try {
            String newName = this.getSchemaName(schema.getName());
            if (newName != null && !newName.equals(schema.getName())) {
                if (isInGroup) {
                    MartUtils.renameSchemaInSchemaGroup(schema, newName);
                    this.allSchemasDiagram.synchroniseDiagram();
                } else {
                    int tabIndex = this.indexOfTab(schema.getName());
                    MartUtils.renameSchema(this.datasetTabSet.getMart(), schema, newName);
                    this.setTitleAt(tabIndex, newName);
                }
                this.datasetTabSet.getMartTabSet().setModifiedStatus(true);
            }
        } catch (Throwable t) {
            this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
        }
    }
    
    /**
     * Syncs this table provider individually against the database.
     */
    public void synchroniseSchema(final Schema schema) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.synchroniseSchema(schema);
                    datasetTabSet.synchroniseTabs(); // Some datasets may disappear.
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Synchronises the mart.
     */
    public void synchroniseAllSchemas() {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.synchroniseMartSchemas(datasetTabSet.getMart());
                    synchroniseTabs();
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Test this table provider individually against the database.
     */
    public void testSchema(Schema schema) {
        boolean passedTest = false;
        try {
            passedTest = MartUtils.testSchema(schema);
        } catch (Throwable t) {
            passedTest = false;
            this.datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
        }
        // Tell the user what happened.
        if (passedTest) {
            JOptionPane.showMessageDialog(
                    this,
                    BuilderBundle.getString("schemaTestPassed"),
                    BuilderBundle.getString("testTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    BuilderBundle.getString("schemaTestFailed"),
                    BuilderBundle.getString("testTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Construct a context menu for a given table provider view tab.
     *
     * @param schema the table provider to use when the context menu items are chosen.
     * @return the popup menu.
     */
    private JPopupMenu getSchemaTabContextMenu(final Schema schema) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem close = new JMenuItem(BuilderBundle.getString("removeSchemaTitle"));
        close.setMnemonic(BuilderBundle.getString("removeSchemaMnemonic").charAt(0));
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmRemoveSchema(schema);
            }
        });
        contextMenu.add(close);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                renameSchema(schema, false);
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
                    if (selectedDiagram instanceof SchemaDiagram) {
                        this.setSelectedIndex(selectedIndex);
                        Schema schema = ((SchemaDiagram)selectedDiagram).getSchema();
                        this.getSchemaTabContextMenu(schema).show(this, evt.getX(), evt.getY());
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
    public void setDiagramModifier(DiagramModifier diagramModifier) {
        this.diagramModifier = diagramModifier;
        for (int i = 0; i < this.getTabCount(); i++) {
            Diagram diagram = (Diagram)((JScrollPane)this.getComponentAt(i)).getViewport().getView();
            diagram.setDiagramModifier(diagramModifier);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public DiagramModifier getDiagramModifier() {
        return this.diagramModifier;
    }
}
