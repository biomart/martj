/*
 * SchemaContext.java
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Column;
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
 * @version 0.1.13, 12th May 2006
 * @since 0.1
 */
public class SchemaContext implements DiagramContext {
    /**
     * The window set we belong to.
     */
    private DataSetTabSet datasetTabSet;
    
    /**
     * Creates a new instance of SchemaContext and binds it to a given
     * MartBuilder instance.
     */
    public SchemaContext(DataSetTabSet datasetTabSet) {
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
    public void populateContextMenu(JPopupMenu contextMenu, Object object) {
        
        if (object == null) {
            // Constant parts of schema diagram menus, for background areas only.
            
            JMenuItem syncAll = new JMenuItem(BuilderBundle.getString("synchroniseAllSchemasTitle"));
            syncAll.setMnemonic(BuilderBundle.getString("synchroniseAllSchemasMnemonic").charAt(0));
            syncAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().getSchemaTabSet().requestSynchroniseAllSchemas();
                }
            });
            contextMenu.add(syncAll);
            
            JMenuItem add = new JMenuItem(BuilderBundle.getString("addSchemaTitle"));
            add.setMnemonic(BuilderBundle.getString("addSchemaMnemonic").charAt(0));
            add.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().getSchemaTabSet().requestAddSchema();
                }
            });
            contextMenu.add(add);
            
        }
        
        else if (object instanceof Table || object instanceof Key) {
            // Add the dataset generation options.
            final Table table;
            if (object instanceof Key) table = ((Key)object).getTable();
            else table = (Table)object;
            
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
            
            JMenuItem keyguess = new JMenuItem(BuilderBundle.getString("enableKeyGuessingTitle"));
            keyguess.setMnemonic(BuilderBundle.getString("enableKeyGuessingMnemonic").charAt(0));
            keyguess.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestEnableKeyGuessing(schema);
                }
            });
            contextMenu.add(keyguess);
            if (schema.getKeyGuessing()) keyguess.setEnabled(false);
            
            JMenuItem unkeyguess = new JMenuItem(BuilderBundle.getString("disableKeyGuessingTitle"));
            unkeyguess.setMnemonic(BuilderBundle.getString("disableKeyGuessingMnemonic").charAt(0));
            unkeyguess.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestDisableKeyGuessing(schema);
                }
            });
            contextMenu.add(unkeyguess);
            if (!schema.getKeyGuessing()) unkeyguess.setEnabled(false);
            
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
            
            boolean relationIncorrect = relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT);
            
            // one:one
            JMenuItem oneToOne = new JMenuItem(BuilderBundle.getString("oneToOneTitle"));
            oneToOne.setMnemonic(BuilderBundle.getString("oneToOneMnemonic").charAt(0));
            oneToOne.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.requestChangeRelationCardinality(relation, Cardinality.ONE);
                }
            });
            contextMenu.add(oneToOne);
            if (relationIncorrect || relation.getFKCardinality().equals(Cardinality.ONE)) oneToOne.setEnabled(false);
            
            // one:many
            JMenuItem oneToMany = new JMenuItem(BuilderBundle.getString("oneToManyTitle"));
            oneToMany.setMnemonic(BuilderBundle.getString("oneToManyMnemonic").charAt(0));
            oneToMany.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.requestChangeRelationCardinality(relation, Cardinality.MANY);
                }
            });
            contextMenu.add(oneToMany);
            if (relationIncorrect || relation.getFKCardinality().equals(Cardinality.MANY)) oneToMany.setEnabled(true);
            
            // correct
            JMenuItem correct = new JMenuItem(BuilderBundle.getString("correctRelationTitle"));
            correct.setMnemonic(BuilderBundle.getString("correctRelationMnemonic").charAt(0));
            correct.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.requestChangeRelationStatus(relation, ComponentStatus.INFERRED);
                }
            });
            if (!relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) correct.setEnabled(false);
            contextMenu.add(correct);
            
            // incorrect
            JMenuItem incorrect = new JMenuItem(BuilderBundle.getString("incorrectRelationTitle"));
            incorrect.setMnemonic(BuilderBundle.getString("incorrectRelationMnemonic").charAt(0));
            incorrect.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.requestChangeRelationStatus(relation, ComponentStatus.INFERRED_INCORRECT);
                }
            });
            if (!relation.getStatus().equals(ComponentStatus.INFERRED)) incorrect.setEnabled(false);
            contextMenu.add(incorrect);
            
            // remove
            JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeRelationTitle"));
            remove.setMnemonic(BuilderBundle.getString("removeRelationMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.requestRemoveRelation(relation);
                }
            });
            if (!relation.getStatus().equals(ComponentStatus.HANDMADE)) remove.setEnabled(false);
            contextMenu.add(remove);
        }
        
        else if (object instanceof Key) {
            // Keys just show their table menus.
            Table table = ((Key)object).getTable();
            this.populateContextMenu(contextMenu, table);
        }
        
        else if (object instanceof Column) {
            // Columns just show their table menus.
            Table table = ((Column)object).getTable();
            this.populateContextMenu(contextMenu, table);
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
}
