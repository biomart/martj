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
import java.util.Collection;
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
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of multiple {@link Schema}s in graphical form.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 16th May 2006
 * @since 0.1
 */
public class SchemaTabSet extends JTabbedPane {
    /**
     * Internal reference to the list of table providers, in order, mapped
     * to their schemaToDiagram.
     */
    private Map schemaToDiagram = new HashMap();
    
    /**
     * Internal reference to the diagramContext for the providers we are viewing.
     */
    private DiagramContext diagramContext;
    
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
        this.recalculateSchemaTabs();
    }
    
    public void redrawDiagramComponent(Object object) {
        if (object instanceof Relation) {
            Relation relation = (Relation)object;
            Schema pkSchema = relation.getPrimaryKey().getTable().getSchema();
            Schema fkSchema = relation.getForeignKey().getTable().getSchema();
            if (pkSchema.equals(fkSchema)) ((Diagram)this.schemaToDiagram.get(pkSchema)).redrawDiagramComponent(relation);
            else this.allSchemasDiagram.redrawDiagramComponent(relation);
        } else {
            for (Iterator i = this.schemaToDiagram.values().iterator(); i.hasNext(); ) {
                ((Diagram)i.next()).redrawDiagramComponent(object);
            }
        }
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
    public void recalculateSchemaTabs() {
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
        // Synchronise our tab view contents.
        for (int i = 0; i < getTabCount(); i++) {
            JScrollPane scroller = (JScrollPane)getComponentAt(i);
            Diagram tableDiagram = (Diagram)scroller.getViewport().getView();
            tableDiagram.recalculateDiagram();
        }
        // Redraw.
        this.repaint();
    }
    
    /**
     * Confirms with user then adds a table provider.
     */
    public void requestAddSchema() {
        // Interpret the response.
        final Schema schema = SchemaManagerDialog.createSchema(this);
        LongProcess.run(this, new Runnable() {
            public void run() {
                // Add to schema.
                try {
                    if (schema != null) {
                        MartUtils.addSchemaToMart(datasetTabSet.getMart(), schema);
                        MartUtils.synchroniseSchema(schema);
                        if (schema.getInternalRelations().size()==0) {
                            MartUtils.enableKeyGuessing(schema);
                            MartUtils.synchroniseSchema(schema);
                        }
                        addSchemaTab(schema);
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
                            datasetTabSet.recalculateDataSetTabs(); // Some datasets may disappear. It'll call us.recalculateDataSetTabs() later.
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
            if (SchemaManagerDialog.modifySchema(this, schema))
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
        // Set the diagramContext on the view.
        schemaDiagram.setDiagramContext(this.getDiagramContext());
        this.allSchemasDiagram.recalculateDiagram();
    }
    
    /**
     * Confirms with user then removes a table provider.
     */
    public void requestRemoveSchema(final Schema schema) {
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
                        datasetTabSet.recalculateDataSetTabs(); // Some datasets may disappear. It'll call us.recalculateDataSetTabs() later.
                        datasetTabSet.getMartTabSet().setModifiedStatus(true);
                    } catch (Throwable t) {
                        datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                    }
                }
            });
        }
    }
    
    public void requestRemoveSchemaFromSchemaGroup(final Schema schema, final SchemaGroup schemaGroup) {
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
                        datasetTabSet.recalculateDataSetTabs(); // Some datasets may disappear. It'll call us.recalculateDataSetTabs() later.
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
    private String askUserForSchemaName(String defaultResponse) {
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
    public void requestRenameSchema(Schema schema, boolean isInGroup) {
        // Update the table provider name and the tab name.
        try {
            String newName = this.askUserForSchemaName(schema.getName());
            if (newName != null && !newName.equals(schema.getName())) {
                if (isInGroup) {
                    MartUtils.renameSchemaInSchemaGroup(schema, newName);
                    this.allSchemasDiagram.recalculateDiagram();
                } else {
                    int tabIndex = this.indexOfTab(schema.getName());
                    MartUtils.renameSchema(this.datasetTabSet.getMart(), schema, newName);
                    this.setTitleAt(tabIndex, newName);
                    datasetTabSet.recalculateDataSetTabs();
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
    public void requestSynchroniseSchema(final Schema schema) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.synchroniseSchema(schema);
                    datasetTabSet.recalculateDataSetTabs(); // Some datasets may disappear. It'll call us.recalculateDataSetTabs() later.
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
    public void requestSynchroniseAllSchemas() {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.synchroniseMartSchemas(datasetTabSet.getMart());
                    datasetTabSet.recalculateDataSetTabs(); // Some datasets may disappear. It'll call us.recalculateDataSetTabs() later.
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
    public void requestTestSchema(Schema schema) {
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
    
    public void requestEnableKeyGuessing(Schema schema) {
        MartUtils.enableKeyGuessing(schema);
        this.requestSynchroniseSchema(schema);
    }
    
    public void requestDisableKeyGuessing(Schema schema) {
        MartUtils.disableKeyGuessing(schema);
        this.requestSynchroniseSchema(schema);
    }
    
    /**
     * Update a relation cardinality.
     */
    public void requestChangeRelationCardinality(final Relation relation, final Cardinality cardinality) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.changeRelationCardinality(datasetTabSet.getMart(), relation, cardinality);
                    datasetTabSet.recalculateDataSetTabs(); // Regenerate all datasets
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Update a relation status.
     */
    public void requestChangeRelationStatus(final Relation relation, final ComponentStatus status) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.changeRelationStatus(datasetTabSet.getMart(), relation, status);
                    datasetTabSet.recalculateDataSetTabs(); // Regenerate all datasets
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Remove a relation.
     */
    public void requestRemoveRelation(final Relation relation) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.removeRelation(datasetTabSet.getMart(), relation);
                    datasetTabSet.recalculateDataSetTabs(); // Regenerate all datasets
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Remove a relation.
     */
    public void requestRemoveKey(final Key key) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.removeKey(datasetTabSet.getMart(), key);
                    datasetTabSet.recalculateDataSetTabs(); // Regenerate all datasets
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Update a relation status.
     */
    public void requestChangeKeyStatus(final Key key, final ComponentStatus status) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.changeKeyStatus(datasetTabSet.getMart(), key, status);
                    datasetTabSet.recalculateDataSetTabs(); // Regenerate all datasets
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Update a relation status.
     */
    public void requestCreatePrimaryKey(Table table) {
        List cols = KeyEditorDialog.createPrimaryKey(this, table);
        if (!cols.isEmpty()) this.requestCreatePrimaryKey(table, cols);
    }
    
    public void requestCreatePrimaryKey(final Table table, final List columns) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.createPrimaryKey(table, columns);
                    recalculateSchemaTabs();
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Update a relation status.
     */
    public void requestCreateForeignKey(Table table) {
        List cols = KeyEditorDialog.createForeignKey(this, table);
        if (!cols.isEmpty()) this.requestCreateForeignKey(table, cols);
    }
    
    public void requestCreateForeignKey(final Table table, final List columns) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.createForeignKey(table, columns);
                    recalculateSchemaTabs();
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    /**
     * Update a relation status.
     */
    public void requestEditKey(Key key) {
        List cols = KeyEditorDialog.editKey(this, key);
        if (!cols.isEmpty() && !cols.equals(key.getColumns())) this.requestEditKey(key, cols);
    }
    
    public void requestEditKey(final Key key, final List columns) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.editKeyColumns(datasetTabSet.getMart(), key, columns);
                    datasetTabSet.recalculateDataSetTabs(); // Regenerate all datasets
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
    }
    
    private Key askUserForTargetKey(Key from) {
        Collection candidates = new ArrayList();
        if (from instanceof PrimaryKey) {
            for (Iterator i = this.schemaToDiagram.keySet().iterator(); i.hasNext(); ) {
                for (Iterator j = ((Schema)i.next()).getTables().iterator(); j.hasNext(); ) {
                    for (Iterator k = ((Table)j.next()).getForeignKeys().iterator(); k.hasNext(); ) {
                        Key fk = (Key)k.next();
                        if (fk.countColumns()==from.countColumns()) candidates.add(fk);
                    }
                }
            }
        } else {
            for (Iterator i = this.schemaToDiagram.keySet().iterator(); i.hasNext(); ) {
                for (Iterator j = ((Schema)i.next()).getTables().iterator(); j.hasNext(); ) {
                    Key pk = ((Table)j.next()).getPrimaryKey();
                    if (pk!=null && pk.countColumns()==from.countColumns()) candidates.add(pk);
                }
            }
        }
        // Now put up the million-dollar-question box.
        return (Key)JOptionPane.showInputDialog(this,
                BuilderBundle.getString("whichKeyToLinkRelationTo"),
                BuilderBundle.getString("questionTitle"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                candidates.toArray(),
                null);
    }
    
    public void requestCreateRelation(Key from) {
        // Obtain the to relation from a question box.
        Key to = this.askUserForTargetKey(from);
        if (to!=null) this.requestCreateRelation(from, to);
    }
    
    public void requestCreateRelation(final Key from, final Key to) {
        LongProcess.run(this, new Runnable() {
            public void run() {
                try {
                    MartUtils.createRelation(datasetTabSet.getMart(), from, to);
                    datasetTabSet.recalculateDataSetTabs(); // Regenerate all datasets
                    datasetTabSet.getMartTabSet().setModifiedStatus(true);
                } catch (Throwable t) {
                    datasetTabSet.getMartTabSet().getMartBuilder().showStackTrace(t);
                }
            }
        });
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
                requestRemoveSchema(schema);
            }
        });
        contextMenu.add(close);
        
        JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
        rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                requestRenameSchema(schema, false);
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
    public void setDiagramContext(DiagramContext diagramContext) {
        this.diagramContext = diagramContext;
        for (int i = 0; i < this.getTabCount(); i++) {
            Diagram diagram = (Diagram)((JScrollPane)this.getComponentAt(i)).getViewport().getView();
            diagram.setDiagramContext(diagramContext);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public DiagramContext getDiagramContext() {
        return this.diagramContext;
    }
}
