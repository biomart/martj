/*
 * SchemaDiagramContext.java
 *
 * Created on 19 April 2006, 09:36
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Provides the default behaviour for table provider listeners.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.9, 8th May 2006
 * @since 0.1
 */
public class SchemaDiagramContext implements DiagramContext {
    /**
     * The window set we belong to.
     */
    protected DataSetTabSet datasetTabSet;
    
    /**
     * Creates a new instance of SchemaDiagramContext and binds it to a given
     * MartBuilder instance.
     */
    public SchemaDiagramContext(DataSetTabSet datasetTabSet) {
        this.datasetTabSet = datasetTabSet;
    }
    
    /**
     * Get the window tab set.
     */
    protected DataSetTabSet getDataSetTabSet() {
        return this.datasetTabSet;
    }
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object object) {
        
        if (object instanceof Table || object instanceof Key) {
            // Add the dataset generation options.
            final Table table;
            if (object instanceof Key) table = ((Key)object).getTable();
            else table = (Table)object;
            
            contextMenu.addSeparator();
            
            JMenuItem create = new JMenuItem(BuilderBundle.getString("createDataSetTitle", table.getName()));
            create.setMnemonic(BuilderBundle.getString("createDataSetMnemonic").charAt(0));
            create.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.requestCreateDataSet(table);
                }
            });
            contextMenu.add(create);
            
            JMenuItem suggest = new JMenuItem(BuilderBundle.getString("suggestDataSetsTitle", table.getName()));
            suggest.setMnemonic(BuilderBundle.getString("suggestDataSetsMnemonic").charAt(0));
            suggest.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.requestSuggestDataSets(table);
                }
            });
            contextMenu.add(suggest);
        }
        
        else if (object instanceof Schema) {
            // Add schema stuff
            final Schema schema = (Schema)object;
            contextMenu.addSeparator();
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestRenameSchema(schema, false);
                }
            });
            contextMenu.add(rename);
            
            JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseSchemaTitle"));
            sync.setMnemonic(BuilderBundle.getString("synchroniseSchemaMnemonic").charAt(0));
            sync.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().getSchemaTabSet().requestSynchroniseSchema(schema);
                }
            });
            contextMenu.add(sync);
            
            if (!(schema instanceof SchemaGroup)) {
                JMenuItem modify = new JMenuItem(BuilderBundle.getString("modifySchemaTitle"));
                modify.setMnemonic(BuilderBundle.getString("modifySchemaMnemonic").charAt(0));
                modify.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.getSchemaTabSet().requestModifySchema(schema);
                    }
                });
                contextMenu.add(modify);
                
                JMenuItem test = new JMenuItem(BuilderBundle.getString("testSchemaTitle"));
                test.setMnemonic(BuilderBundle.getString("testSchemaMnemonic").charAt(0));
                test.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().getSchemaTabSet().requestTestSchema(schema);
                    }
                });
                contextMenu.add(test);
                
                JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeSchemaTitle"));
                remove.setMnemonic(BuilderBundle.getString("removeSchemaMnemonic").charAt(0));
                remove.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.getSchemaTabSet().requestRemoveSchema(schema);
                    }
                });
                contextMenu.add(remove);
                
                JMenuItem addToGroup = new JMenuItem(BuilderBundle.getString("addToGroupTitle"));
                addToGroup.setMnemonic(BuilderBundle.getString("addToGroupMnemonic").charAt(0));
                addToGroup.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.getSchemaTabSet().requestAddSchemaToSchemaGroup(schema);
                    }
                });
                contextMenu.add(addToGroup);
            }
        }
        
        else if (object instanceof Relation) {
            // Add relation stuff
            final Relation relation = (Relation)object;
            contextMenu.addSeparator();
            
            boolean relationIncorrect = relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT);
            
            if (relation.getFKCardinality().equals(Cardinality.MANY)) {
                // one:one
                JMenuItem oneToOne = new JMenuItem(BuilderBundle.getString("oneToOneTitle"));
                oneToOne.setMnemonic(BuilderBundle.getString("oneToOneMnemonic").charAt(0));
                oneToOne.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.requestChangeRelationCardinality(relation, Cardinality.ONE);
                    }
                });
                contextMenu.add(oneToOne);
                if (relationIncorrect) oneToOne.setEnabled(false);
            } else if (relation.getFKCardinality().equals(Cardinality.ONE)) {
                // one:many
                JMenuItem oneToMany = new JMenuItem(BuilderBundle.getString("oneToManyTitle"));
                oneToMany.setMnemonic(BuilderBundle.getString("oneToManyMnemonic").charAt(0));
                oneToMany.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.requestChangeRelationCardinality(relation, Cardinality.MANY);
                    }
                });
                contextMenu.add(oneToMany);
                if (relationIncorrect) oneToMany.setEnabled(true);
            }
            
            if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) {
                // correct
                JMenuItem correct = new JMenuItem(BuilderBundle.getString("correctRelationTitle"));
                correct.setMnemonic(BuilderBundle.getString("correctRelationMnemonic").charAt(0));
                correct.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.requestChangeRelationStatus(relation, ComponentStatus.INFERRED);
                    }
                });
                contextMenu.add(correct);
            } else if (relation.getStatus().equals(ComponentStatus.INFERRED)) {
                // incorrect
                JMenuItem incorrect = new JMenuItem(BuilderBundle.getString("incorrectRelationTitle"));
                incorrect.setMnemonic(BuilderBundle.getString("incorrectRelationMnemonic").charAt(0));
                incorrect.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.requestChangeRelationStatus(relation, ComponentStatus.INFERRED_INCORRECT);
                    }
                });
                contextMenu.add(incorrect);
            } else if (relation.getStatus().equals(ComponentStatus.HANDMADE)) {
                // remove
                JMenuItem incorrect = new JMenuItem(BuilderBundle.getString("removeRelationTitle"));
                incorrect.setMnemonic(BuilderBundle.getString("removeRelationMnemonic").charAt(0));
                incorrect.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.requestRemoveRelation(relation);
                    }
                });
                contextMenu.add(incorrect);
            }
        }
        
        else if (object instanceof Key) {
            Table table = ((Key)object).getTable();
            this.customiseContextMenu(contextMenu, table);
        }
    }
    
    public void customiseAppearance(JComponent component, Object object) {
        if (object instanceof Relation) {
            Relation relation = (Relation)object;
            // Fade out all INFERRED_INCORRECT relations.
            if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) {
                component.setForeground(RelationComponent.FADED_COLOUR);
            }
            // Highlight all HANDMADE relations.
            else if (relation.getStatus().equals(ComponentStatus.HANDMADE)) {
                component.setForeground(RelationComponent.HANDMADE_COLOUR);
            }
            // All others are normal.
            else {
                component.setForeground(RelationComponent.NORMAL_COLOUR);
            }
        }
    }
    
    public boolean isRightClickAllowed() {
        return true;
    }
    
    public JComponent getTableManagerContextPane(Table table, JList columnsList) {
        // Create a pane explaining the underlying relations.
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Schema context"), BorderLayout.PAGE_START);
        return panel;
    }
}
