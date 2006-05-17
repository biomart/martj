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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.TransferHandler;
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
 * @version 0.1.15, 17th May 2006
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
        if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
        
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
        
        else if (object instanceof Table) {
        if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
        
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
            
            contextMenu.addSeparator();
            
            JMenuItem pk = new JMenuItem(BuilderBundle.getString("createPrimaryKeyTitle"));
            pk.setMnemonic(BuilderBundle.getString("createPrimaryKeyMnemonic").charAt(0));
            pk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestCreatePrimaryKey(table);
                }
            });
            if (table.getPrimaryKey()!=null) pk.setEnabled(false);
            contextMenu.add(pk);
            
            JMenuItem fk = new JMenuItem(BuilderBundle.getString("createForeignKeyTitle"));
            fk.setMnemonic(BuilderBundle.getString("createForeignKeyMnemonic").charAt(0));
            fk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestCreateForeignKey(table);
                }
            });
            contextMenu.add(fk);
        }
        
        else if (object instanceof Schema) {
        if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
        
            // Add schema stuff
            final Schema schema = (Schema)object;
            
            final JCheckBoxMenuItem keyguess = new JCheckBoxMenuItem(BuilderBundle.getString("enableKeyGuessingTitle"));
            keyguess.setMnemonic(BuilderBundle.getString("enableKeyGuessingMnemonic").charAt(0));
            keyguess.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (keyguess.isSelected()) datasetTabSet.getSchemaTabSet().requestEnableKeyGuessing(schema);
                    else datasetTabSet.getSchemaTabSet().requestDisableKeyGuessing(schema);
                }
            });
            contextMenu.add(keyguess);
            if (schema.getKeyGuessing()) keyguess.setSelected(true);
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestRenameSchema(schema, null);
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
                
                JMenuItem replicate = new JMenuItem(BuilderBundle.getString("replicateSchemaTitle"));
                replicate.setMnemonic(BuilderBundle.getString("replicateSchemaMnemonic").charAt(0));
                replicate.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        datasetTabSet.getSchemaTabSet().requestReplicateSchema(schema);
                    }
                });
                contextMenu.add(replicate);
                
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
        if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
        
            // Add relation stuff
            final Relation relation = (Relation)object;
            
            boolean relationIncorrect = relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT);
            
            
            ButtonGroup cardGroup = new ButtonGroup();
            // one:one
            JRadioButtonMenuItem oneToOne = new JRadioButtonMenuItem(BuilderBundle.getString("oneToOneTitle"));
            oneToOne.setMnemonic(BuilderBundle.getString("oneToOneMnemonic").charAt(0));
            oneToOne.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestChangeRelationCardinality(relation, Cardinality.ONE);
                }
            });
            cardGroup.add(oneToOne);
            contextMenu.add(oneToOne);
            if (relationIncorrect) oneToOne.setEnabled(false);
            if (relation.getFKCardinality().equals(Cardinality.ONE)) oneToOne.setSelected(true);
            
            // one:many
            JRadioButtonMenuItem oneToMany = new JRadioButtonMenuItem(BuilderBundle.getString("oneToManyTitle"));
            oneToMany.setMnemonic(BuilderBundle.getString("oneToManyMnemonic").charAt(0));
            oneToMany.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestChangeRelationCardinality(relation, Cardinality.MANY);
                }
            });
            cardGroup.add(oneToMany);
            contextMenu.add(oneToMany);
            if (relationIncorrect) oneToMany.setEnabled(true);
            if (relation.getFKCardinality().equals(Cardinality.MANY)) oneToMany.setSelected(true);
            
        contextMenu.addSeparator();
        
            
            ButtonGroup correctGroup = new ButtonGroup();
            // correct
            JRadioButtonMenuItem correct = new JRadioButtonMenuItem(BuilderBundle.getString("correctRelationTitle"));
            correct.setMnemonic(BuilderBundle.getString("correctRelationMnemonic").charAt(0));
            correct.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestChangeRelationStatus(relation, ComponentStatus.INFERRED);
                }
            });
            correctGroup.add(correct);
            contextMenu.add(correct);
            if (relation.getStatus().equals(ComponentStatus.INFERRED)) correct.setSelected(true);
            else if (relation.getStatus().equals(ComponentStatus.HANDMADE)) correct.setEnabled(false);
            
            // incorrect
            JRadioButtonMenuItem incorrect = new JRadioButtonMenuItem(BuilderBundle.getString("incorrectRelationTitle"));
            incorrect.setMnemonic(BuilderBundle.getString("incorrectRelationMnemonic").charAt(0));
            incorrect.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestChangeRelationStatus(relation, ComponentStatus.INFERRED_INCORRECT);
                }
            });
            correctGroup.add(incorrect);
            contextMenu.add(incorrect);
            if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) incorrect.setSelected(true);
            else if (relation.getStatus().equals(ComponentStatus.HANDMADE)) incorrect.setEnabled(false);
            
            
        contextMenu.addSeparator();
        
            
            // remove
            JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeRelationTitle"));
            remove.setMnemonic(BuilderBundle.getString("removeRelationMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestRemoveRelation(relation);
                }
            });
            contextMenu.add(remove);
            if (!relation.getStatus().equals(ComponentStatus.HANDMADE)) remove.setEnabled(false);
        }
        
        else if (object instanceof Key) {
            // Keys just show their table menus first.
            final Table table = ((Key)object).getTable();
            this.populateContextMenu(contextMenu, table);
            
            // Then their own stuff.
            final Key key = (Key)object;
            
            // Separators only if their is stuff to be separated from.
            if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
            
            // Primary/Foreign/edit keys
            
            JMenuItem editkey = new JMenuItem(BuilderBundle.getString("editKeyTitle"));
            editkey.setMnemonic(BuilderBundle.getString("editKeyMnemonic").charAt(0));
            editkey.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestEditKey(key);
                }
            });
            contextMenu.add(editkey);
            
            // Create relation
            
            JMenuItem createrel = new JMenuItem(BuilderBundle.getString("createRelationTitle"));
            createrel.setMnemonic(BuilderBundle.getString("createRelationMnemonic").charAt(0));
            createrel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestCreateRelation(key);
                }
            });
            contextMenu.add(createrel);
            if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) createrel.setEnabled(false);
            
            // Incorrect/correct/remove keys.
            
            contextMenu.addSeparator();
            
            ButtonGroup correctGroup = new ButtonGroup();
            // correct
            JRadioButtonMenuItem correct = new JRadioButtonMenuItem(BuilderBundle.getString("correctKeyTitle"));
            correct.setMnemonic(BuilderBundle.getString("correctKeyMnemonic").charAt(0));
            correct.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestChangeKeyStatus(key, ComponentStatus.INFERRED);
                }
            });
            correctGroup.add(correct);
            contextMenu.add(correct);
            if (key.getStatus().equals(ComponentStatus.INFERRED)) correct.setSelected(true);
            else if (key.getStatus().equals(ComponentStatus.HANDMADE)) correct.setEnabled(false);
            
            // incorrect
            JRadioButtonMenuItem incorrect = new JRadioButtonMenuItem(BuilderBundle.getString("incorrectKeyTitle"));
            incorrect.setMnemonic(BuilderBundle.getString("incorrectKeyMnemonic").charAt(0));
            incorrect.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestChangeKeyStatus(key, ComponentStatus.INFERRED_INCORRECT);
                }
            });
            correctGroup.add(incorrect);
            contextMenu.add(incorrect);
            if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) incorrect.setSelected(true);
            else if (key.getStatus().equals(ComponentStatus.HANDMADE)) incorrect.setEnabled(false);
            
            contextMenu.addSeparator();
            
            // remove
            JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeKeyTitle"));
            remove.setMnemonic(BuilderBundle.getString("removeKeyMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.getSchemaTabSet().requestRemoveKey(key);
                }
            });
            contextMenu.add(remove);
            if (!key.getStatus().equals(ComponentStatus.HANDMADE)) remove.setEnabled(false);
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
                component.setForeground(RelationComponent.INCORRECT_COLOUR);
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
        
        else if (object instanceof Key) {
            Key key = (Key)object;
            // Fade out all INFERRED_INCORRECT relations.
            if (key.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) {
                component.setForeground(KeyComponent.INCORRECT_COLOUR);
            }
            // Highlight all HANDMADE relations.
            else if (key.getStatus().equals(ComponentStatus.HANDMADE)) {
                component.setForeground(KeyComponent.HANDMADE_COLOUR);
            }
            // All others are normal.
            else {
                component.setForeground(KeyComponent.NORMAL_COLOUR);
            }
            
            // Add drag-and-drop.
            component.addMouseListener(SchemaContext.dragAdapter);
        }
    }
    
    public static MouseAdapter dragAdapter = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            JComponent c = (JComponent)e.getSource();
            TransferHandler handler = c.getTransferHandler();
            handler.exportAsDrag(c, e, TransferHandler.COPY);
        }
    };
}
