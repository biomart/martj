/*
 * WindowDiagramContext.java
 *
 * Created on 19 April 2006, 09:43
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
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Adapts listener behaviour by adding in DataSet-specific stuff.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 10th May 2006
 * @since 0.1
 */
public class WindowDiagramContext extends SchemaDiagramContext {
    /**
     * Internal reference to our dataset.
     */
    private DataSet dataset;
    
    /**
     *
     * Creates a new instance of DataSetDiagramModifier over
     * a given dataset.
     *
     *
     * @param dataset the dataset we are attached to.
     */
    public WindowDiagramContext(DataSetTabSet datasetTabSet, DataSet dataset) {
        super(datasetTabSet);
        this.dataset = dataset;
    }
    
    /**
     * Retrieve our dataset.
     *
     * @return our dataset.
     */
    protected DataSet getDataSet() {
        return this.dataset;
    }
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object object) {
        
        if (object instanceof Schema) {
            // Add schema stuff
            final Schema schema = (Schema)object;
            contextMenu.addSeparator();
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameSchemaTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameSchemaMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().getSchemaTabSet().requestRenameSchema(schema, false);
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
                        getDataSetTabSet().getSchemaTabSet().requestModifySchema(schema);
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
                        getDataSetTabSet().getSchemaTabSet().requestRemoveSchema(schema);
                    }
                });
                contextMenu.add(remove);
                
                JMenuItem addToGroup = new JMenuItem(BuilderBundle.getString("addToGroupTitle"));
                addToGroup.setMnemonic(BuilderBundle.getString("addToGroupMnemonic").charAt(0));
                addToGroup.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().getSchemaTabSet().requestAddSchemaToSchemaGroup(schema);
                    }
                });
                contextMenu.add(addToGroup);
            }
        }
        
        else if (object instanceof Relation) {
            // Relation stuff
            final Relation relation = (Relation)object;
            final DataSet ds = this.getDataSetTabSet().getSelectedDataSetTab().getDataSet();
            
            // Add separator.
            contextMenu.addSeparator();
            
            boolean incorrect = relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT);
            boolean relationOneToOne = relation.getFKCardinality().equals(Cardinality.ONE);
            boolean relationMasked = ds.getMaskedRelations().contains(relation);
            boolean relationConcated = ds.getConcatOnlyRelations().contains(relation);
            boolean relationSubclassed = ds.getSubclassedRelations().contains(relation);
            
            if (relationMasked) {
                JMenuItem unmask = new JMenuItem(BuilderBundle.getString("unmaskRelationTitle"));
                unmask.setMnemonic(BuilderBundle.getString("unmaskRelationMnemonic").charAt(0));
                unmask.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestUnmaskRelation(ds, relation);
                    }
                });
                contextMenu.add(unmask);
                if (incorrect) unmask.setEnabled(false);
            } else {
                JMenuItem mask = new JMenuItem(BuilderBundle.getString("maskRelationTitle"));
                mask.setMnemonic(BuilderBundle.getString("maskRelationMnemonic").charAt(0));
                mask.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestMaskRelation(ds, relation);
                    }
                });
                contextMenu.add(mask);
                if (incorrect) mask.setEnabled(false);
            }
            
            if (relationSubclassed) {
                JMenuItem unsubclass = new JMenuItem(BuilderBundle.getString("unsubclassRelationTitle"));
                unsubclass.setMnemonic(BuilderBundle.getString("unsubclassRelationMnemonic").charAt(0));
                unsubclass.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestUnsubclassRelation(ds, relation);
                    }
                });
                contextMenu.add(unsubclass);
                if (incorrect || relationOneToOne || relationMasked || relationConcated) unsubclass.setEnabled(false);
            } else {
                JMenuItem subclass = new JMenuItem(BuilderBundle.getString("subclassRelationTitle"));
                subclass.setMnemonic(BuilderBundle.getString("subclassRelationMnemonic").charAt(0));
                subclass.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestSubclassRelation(ds, relation);
                    }
                });
                contextMenu.add(subclass);
                if (incorrect || relationOneToOne || relationMasked || relationConcated) subclass.setEnabled(false);
            }
            
            if (relationConcated) {
                JMenuItem unconcat = new JMenuItem(BuilderBundle.getString("unconcatOnlyRelationTitle"));
                unconcat.setMnemonic(BuilderBundle.getString("unconcatOnlyRelationMnemonic").charAt(0));
                unconcat.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestUnconcatOnlyRelation(ds, relation);
                    }
                });
                contextMenu.add(unconcat);
                if (incorrect || relationOneToOne || relationMasked || relationSubclassed) unconcat.setEnabled(false);
            } else {
                JMenu concatMenu = new JMenu(BuilderBundle.getString("concatOnlyRelationTitle"));
                concatMenu.setMnemonic(BuilderBundle.getString("concatOnlyRelationMnemonic").charAt(0));
                
                JMenuItem comma = new JMenuItem(BuilderBundle.getString("commaConcatTitle"));
                comma.setMnemonic(BuilderBundle.getString("commaConcatMnemonic").charAt(0));
                comma.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestConcatOnlyRelation(ds, relation, ConcatRelationType.COMMA);
                    }
                });
                concatMenu.add(comma);
                
                JMenuItem space = new JMenuItem(BuilderBundle.getString("spaceConcatTitle"));
                space.setMnemonic(BuilderBundle.getString("spaceConcatMnemonic").charAt(0));
                space.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestConcatOnlyRelation(ds, relation, ConcatRelationType.SPACE);
                    }
                });
                concatMenu.add(space);
                
                JMenuItem tab = new JMenuItem(BuilderBundle.getString("tabConcatTitle"));
                tab.setMnemonic(BuilderBundle.getString("tabConcatMnemonic").charAt(0));
                tab.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        getDataSetTabSet().requestConcatOnlyRelation(ds, relation, ConcatRelationType.TAB);
                    }
                });
                concatMenu.add(tab);
                
                contextMenu.add(concatMenu);
                if (incorrect || relationOneToOne || relationMasked || relationSubclassed) concatMenu.setEnabled(false);
            }
        }
        
        else if (object instanceof Key) {
            Table table = ((Key)object).getTable();
            this.customiseContextMenu(contextMenu, table);
        }
        
        else if (object == null) {
            // Common stuff.
            
            // Add separator.
            contextMenu.addSeparator();
            
            JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeDataSetTitle"));
            remove.setMnemonic(BuilderBundle.getString("removeDataSetMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestRemoveDataSet(dataset);
                }
            });
            contextMenu.add(remove);
            
            JMenuItem optimise = new JMenuItem(BuilderBundle.getString("optimiseDataSetTitle"));
            optimise.setMnemonic(BuilderBundle.getString("optimiseDataSetMnemonic").charAt(0));
            optimise.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestOptimiseDataSet(dataset);
                }
            });
            contextMenu.add(optimise);
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameDataSetTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameDataSetMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestRenameDataSet(dataset);
                }
            });
            contextMenu.add(rename);
        }
    }
    
    public void customiseAppearance(JComponent component, Object object) {
        if (object instanceof Relation) {
            
            DataSet ds = this.getDataSetTabSet().getSelectedDataSetTab().getDataSet();
            
            Relation relation = (Relation)object;
            // Fade out all INFERRED_INCORRECT and MASKED relations.
            if (relation.getStatus().equals(ComponentStatus.INFERRED_INCORRECT) ||
                    ds.getMaskedRelations().contains(relation)) {
                component.setForeground(RelationComponent.FADED_COLOUR);
            }
            // Highlight CONCAT-ONLY relations.
            else if (ds.getConcatOnlyRelations().contains(relation)) {
                component.setForeground(RelationComponent.CONCAT_COLOUR);
            }
            // Highlight SUBCLASS relations.
            else if (ds.getSubclassedRelations().contains(relation)) {
                component.setForeground(RelationComponent.SUBCLASS_COLOUR);
            }
            // All others are normal.
            else {
                component.setForeground(RelationComponent.NORMAL_COLOUR);
            }
        }
    }
    
    private DefaultListModel maskedColumns;
    
    public JComponent getTableManagerContextPane(final TableManagerDialog manager) {
        // Create a big-box list of things.
        Box panel = Box.createVerticalBox();
        
        // Create a sub-pane including the remove column button, and a label for the diagram.
        Box maskPane = Box.createHorizontalBox();
        // Create a sub-sub pane for the buttons and label.
        Box maskButtonPane = Box.createVerticalBox();
        JLabel label = new JLabel(BuilderBundle.getString("maskedColumnsLabel"));
        label.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        maskButtonPane.add(label);
        final JButton unmask = new JButton(BuilderBundle.getString("deselectColumnButton"));
        unmask.setEnabled(false); // default off.
        maskButtonPane.add(unmask);
        final JButton mask = new JButton(BuilderBundle.getString("selectColumnButton"));
        mask.setEnabled(false); // default off.
        maskButtonPane.add(mask);
        // Add the buttons and label to the main mask pane.
        maskPane.add(maskButtonPane);
        // Create a pane listing the masked columns.
        // Set up the empty list.
        this.maskedColumns = new DefaultListModel();
        // Add any existing masked columns to it.
        for (Iterator i = this.getDataSet().getMaskedColumns().iterator(); i.hasNext(); ) {
            Column col = (Column)i.next();
            if (col.getTable().equals(manager.getTable())) this.maskedColumns.addElement(col.getName());
        }
        // Add the list to the pane.
        final JList maskedColumnsList = new JList(this.maskedColumns);
        JScrollPane scroller = new JScrollPane(maskedColumnsList);
        scroller.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        maskPane.add(scroller);
        
        /*
        // Create a sub-pane including the remove column button, and a label for the diagram.
        Box partitionPane = Box.createHorizontalBox();
        final JButton partition = new JButton(BuilderBundle.getString("deselectColumnButton"));
        partition.setEnabled(false); // default off.
        partitionPane.add(partition);
        final JButton unpartition = new JButton(BuilderBundle.getString("selectColumnButton"));
        unpartition.setEnabled(false); // default off.
        partitionPane.add(unpartition);
        label = new JLabel(BuilderBundle.getString("partitionColumnLabel"));
        label.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        partitionPane.add(label);
        final JTextField partitionedColumn = new JTextField();
        partitionedColumn.setEnabled(false);
        partitionPane.add(partitionedColumn);
        final JComboBox partitionedColumnType = new JComboBox();
        partitionedColumnType.setEnabled(false);
        partitionPane.add(partitionedColumnType);
        // Look up partition types.
            -- 'SingleValue' takes one string params,
            -- 'UniqueValues' takes no params,
            -- 'ValueCollection' takes unlimited string params.
        // Can partition only one column, or at most two if exactly one of them is a SchemaNameColumn
        // Look up default partitioned column.
        if (!this.dataset.getPartitionedColumns().isEmpty()) {
            boolean found = false;
            for (Iterator i = this.dataset.getPartitionedColumns().iterator(); i.hasNext() && !found; ) {
                Column c = (Column)i.next();
                if (c.getTable().equals(manager.getTable())) {
                    found = true;
                    partitionedColumn.setText(c.getName());
                    unpartition.setEnabled(true);
                }
            }
        }
         */
        
        // Add the panes.
        panel.add(maskPane);
        //panel.add(partitionPane);
        
        // Add a column listener which redraws the diagram each time it changes.
        final JList columnsList = manager.getColumnsList();
        columnsList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedCol = (String)columnsList.getSelectedValue();
                    if (selectedCol==null) {
                        mask.setEnabled(false);
                        //              partition.setEnabled(false);
                    } else {
                        mask.setEnabled(true);
                        //              partition.setEnabled(true);
                    }
                }
            }
        });
        
        // Add a column listener which redraws the diagram each time it changes.
        maskedColumnsList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedCol = (String)maskedColumnsList.getSelectedValue();
                    if (selectedCol==null) {
                        unmask.setEnabled(false);
                    } else {
                        unmask.setEnabled(true);
                    }
                }
            }
        });
        
        // Add action to the 'mask column' button.
        mask.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedCol = (String)manager.getColumnsList().getSelectedValue();
                if (selectedCol != null && !maskedColumns.contains(selectedCol)) {
                    Column col = manager.getTable().getColumnByName(selectedCol);
                    getDataSetTabSet().requestMaskColumn(getDataSet(), col);
                    maskedColumns.addElement(col.getName());
                }
            }
        });
        
        // Add action to the 'unmask column' button.
        unmask.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedCol = (String)maskedColumnsList.getSelectedValue();
                if (selectedCol != null) {
                    Column col = manager.getTable().getColumnByName(selectedCol);
                    getDataSetTabSet().requestUnmaskColumn(getDataSet(), col);
                    maskedColumns.removeElement(col.getName());
                }
            }
        });
        
        /*
        // Add action to the 'mask column' button.
        partition.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedCol = (String)manager.getColumnsList().getSelectedValue();
                if (selectedCol != null) {
                    Column col = manager.getTable().getColumnByName(selectedCol);
                    getDataSetTabSet().requestPartitionColumn(getDataSet(), col);
                    partitionedColumn.setText(col.getName());
                }
            }
        });
         
        // Add action to the 'unmask column' button.
        unpartition.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedCol = (String)maskedColumnsList.getSelectedValue();
                if (selectedCol != null) {
                    Column col = manager.getTable().getColumnByName(selectedCol);
                    getDataSetTabSet().requestUnpartitionColumn(getDataSet(), col);
                    partitionedColumn.setText(BuilderBundle.getString("none"));
                    unpartition.setEnabled(false);
                }
            }
        });
         */
        
        // Return the panel
        return panel;
    }
}
