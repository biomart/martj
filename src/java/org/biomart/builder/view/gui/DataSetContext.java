/*
 * DataSetContext.java
 *
 * Created on 19 April 2006, 09:46
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
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Adapts listener events suitable for datasets.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.12, 16th May 2006
 * @since 0.1
 */
public class DataSetContext extends WindowContext {
    /**
     *
     * Creates a new instance of DataSetContext over
     * a given window.
     *
     * @param window the window whose dataset we are attached to.
     */
    public DataSetContext(DataSetTabSet datasetTabSet, DataSet dataset) {
        super(datasetTabSet, dataset);
    }
    
    /**
     * {@inheritDoc}
     */
    public void populateContextMenu(JPopupMenu contextMenu, Object object) {
        if (object instanceof DataSet) {
            if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();        
            
            // Common DataSet stuff.
            
            JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeDataSetTitle"));
            remove.setMnemonic(BuilderBundle.getString("removeDataSetMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestRemoveDataSet(getDataSet());
                }
            });
            contextMenu.add(remove);
            
            JMenuItem optimise = new JMenuItem(BuilderBundle.getString("optimiseDataSetTitle"));
            optimise.setMnemonic(BuilderBundle.getString("optimiseDataSetMnemonic").charAt(0));
            optimise.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestOptimiseDataSet(getDataSet());
                }
            });
            contextMenu.add(optimise);
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameDataSetTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameDataSetMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestRenameDataSet(getDataSet());
                }
            });
            contextMenu.add(rename);
            
            // Optimiser stuff.
            JMenu optimiserMenu = new JMenu(BuilderBundle.getString("optimiserTypeTitle"));
            optimiserMenu.setMnemonic(BuilderBundle.getString("optimiserTypeMnemonic").charAt(0));
            ButtonGroup optGroup = new ButtonGroup();

            JRadioButtonMenuItem optNone = new JRadioButtonMenuItem(BuilderBundle.getString("optimiserNoneTitle"));
            optNone.setMnemonic(BuilderBundle.getString("optimiserNoneMnemonic").charAt(0));
            optNone.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestChangeOptimiserType(getDataSet(), DataSetOptimiserType.NONE);
                }
            });
            optGroup.add(optNone);
            optimiserMenu.add(optNone);
            if (this.getDataSet().getDataSetOptimiserType().equals(DataSetOptimiserType.NONE)) optNone.setSelected(true);
            
            JRadioButtonMenuItem optLJ = new JRadioButtonMenuItem(BuilderBundle.getString("optimiserLeftJoinTitle"));
            optLJ.setMnemonic(BuilderBundle.getString("optimiserLeftJoinMnemonic").charAt(0));
            optLJ.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestChangeOptimiserType(getDataSet(), DataSetOptimiserType.LEFTJOIN);
                }
            });
            optGroup.add(optLJ);
            optimiserMenu.add(optLJ);
            if (this.getDataSet().getDataSetOptimiserType().equals(DataSetOptimiserType.LEFTJOIN)) optLJ.setSelected(true);

            JRadioButtonMenuItem optCol= new JRadioButtonMenuItem(BuilderBundle.getString("optimiserColumnTitle"));
            optCol.setMnemonic(BuilderBundle.getString("optimiserColumnMnemonic").charAt(0));
            optCol.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestChangeOptimiserType(getDataSet(), DataSetOptimiserType.COLUMN);
                }
            });
            optGroup.add(optCol);
            optimiserMenu.add(optCol);
            if (this.getDataSet().getDataSetOptimiserType().equals(DataSetOptimiserType.COLUMN)) optCol.setSelected(true);

            JRadioButtonMenuItem optTbl = new JRadioButtonMenuItem(BuilderBundle.getString("optimiserTableTitle"));
            optTbl.setMnemonic(BuilderBundle.getString("optimiserTableMnemonic").charAt(0));
            optTbl.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestChangeOptimiserType(getDataSet(), DataSetOptimiserType.TABLE);
                }
            });
            optGroup.add(optTbl);
            optimiserMenu.add(optTbl);
            if (this.getDataSet().getDataSetOptimiserType().equals(DataSetOptimiserType.TABLE)) optTbl.setSelected(true);
            
            contextMenu.add(optimiserMenu);
        }
        
        if (object instanceof DataSetTable) {
        if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
        
            // DataSet table stuff.
            final DataSetTable table = (DataSetTable)object;
            DataSetTableType tableType = table.getType();
            
            JMenuItem explain = new JMenuItem(BuilderBundle.getString("explainTableTitle"));
            explain.setMnemonic(BuilderBundle.getString("explainTableMnemonic").charAt(0));
            explain.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestExplainTable(table);
                }
            });
            contextMenu.add(explain);
            
            if (tableType.equals(DataSetTableType.DIMENSION)) {
                JMenuItem removeDM = new JMenuItem(BuilderBundle.getString("removeDimensionTitle"));
                removeDM.setMnemonic(BuilderBundle.getString("removeDimensionMnemonic").charAt(0));
                removeDM.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        Relation relation = (Relation)table.getUnderlyingRelations().toArray(new Relation[0])[0];
                        getDataSetTabSet().requestMaskRelation((DataSet)table.getSchema(), relation);
                    }
                });
                contextMenu.add(removeDM);
            } else if (tableType.equals(DataSetTableType.MAIN_SUBCLASS)) {
                JMenuItem removeDM = new JMenuItem(BuilderBundle.getString("removeSubclassTitle"));
                removeDM.setMnemonic(BuilderBundle.getString("removeSubclassMnemonic").charAt(0));
                removeDM.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        Relation relation = (Relation)table.getUnderlyingRelations().toArray(new Relation[0])[0];
                        getDataSetTabSet().requestUnsubclassRelation((DataSet)table.getSchema(), relation);
                    }
                });
                contextMenu.add(removeDM);
            }
        }
        
        else if (object instanceof Key) {
            // Show parent table stuff for keys.
            Table table = ((Key)object).getTable();
            this.populateContextMenu(contextMenu, table);
        }
        
        else if (object instanceof DataSetColumn) {
            // Show parent table stuff first.
            Table table = ((DataSetColumn)object).getTable();
            this.populateContextMenu(contextMenu, table);
            
            // Add separator.
            if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
            
            // AND they show Column stuff
            final DataSetColumn column = (DataSetColumn)object;
            final DataSet ds = this.getDataSetTabSet().getSelectedDataSetTab().getDataSet();
            
            JMenuItem explain = new JMenuItem(BuilderBundle.getString("explainColumnTitle"));
            explain.setMnemonic(BuilderBundle.getString("explainColumnMnemonic").charAt(0));
            explain.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestExplainColumn(column);
                }
            });
            contextMenu.add(explain);
            
            // Add column stuff.
            final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(BuilderBundle.getString("maskColumnTitle"));
            mask.setMnemonic(BuilderBundle.getString("maskColumnMnemonic").charAt(0));
            mask.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (mask.isSelected()) getDataSetTabSet().requestMaskColumn(ds, column);
                    else getDataSetTabSet().requestUnmaskColumn(ds, column);
                }
            });
            contextMenu.add(mask);
            if (ds.getMaskedDataSetColumns().contains(column)) mask.setSelected(true);
            
            // If it's a schema name column...
            if (column instanceof SchemaNameColumn) {
                final JCheckBoxMenuItem partition = new JCheckBoxMenuItem(BuilderBundle.getString("partitionOnSchemaTitle"));
                partition.setMnemonic(BuilderBundle.getString("partitionOnSchemaMnemonic").charAt(0));
                partition.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        if (partition.isSelected()) getDataSetTabSet().requestPartitionBySchema(ds);
                        else getDataSetTabSet().requestUnpartitionBySchema(ds);
                    }
                });
                contextMenu.add(partition);
                if (ds.getPartitionOnSchema()) partition.setSelected(false);
            } else if (column instanceof WrappedColumn) {
                // Partition stuff.
                final WrappedColumn wrappedCol = (WrappedColumn)column;
                boolean isPartitioned = ds.getPartitionedWrappedColumns().contains(column);
                
                if (isPartitioned) {
                    // Submenu with change option.
                    JMenu partitionSubmenu = new JMenu(BuilderBundle.getString("partitionColumnSMTitle"));
                    partitionSubmenu.setMnemonic(BuilderBundle.getString("partitionColumnSMMnemonic").charAt(0));
                    
                    JMenuItem changepartition = new JMenuItem(BuilderBundle.getString("changePartitionColumnTitle"));
                    changepartition.setMnemonic(BuilderBundle.getString("changePartitionColumnMnemonic").charAt(0));
                    changepartition.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            getDataSetTabSet().requestPartitionByColumn(ds, wrappedCol);
                        }
                    });
                    partitionSubmenu.add(changepartition);
                    
                    JMenuItem unpartition = new JMenuItem(BuilderBundle.getString("unpartitionColumnTitle"));
                    unpartition.setMnemonic(BuilderBundle.getString("unpartitionColumnMnemonic").charAt(0));
                    unpartition.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            getDataSetTabSet().requestUnpartitionByColumn(ds, wrappedCol);
                        }
                    });
                    partitionSubmenu.add(unpartition);
                    
                    contextMenu.add(partitionSubmenu);
                } else {
                    // Partition option.
                    JMenuItem partition = new JMenuItem(BuilderBundle.getString("partitionColumnTitle"));
                    partition.setMnemonic(BuilderBundle.getString("partitionColumnMnemonic").charAt(0));
                    partition.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            getDataSetTabSet().requestPartitionByColumn(ds, wrappedCol);
                        }
                    });
                    contextMenu.add(partition);
                }
            }
        }
    }
    
    public void customiseAppearance(JComponent component, Object object) {
        if (object instanceof Relation) {
            
            Relation relation = (Relation)object;
            // Highlight SUBCLASS relations.
            if (((DataSetTable)relation.getForeignKey().getTable()).getType().equals(DataSetTableType.MAIN_SUBCLASS)) {
                component.setForeground(RelationComponent.SUBCLASS_COLOUR);
            }
            // All the rest are normal.
            else {
                component.setForeground(RelationComponent.NORMAL_COLOUR);
            }
        }
        
        else if (object instanceof DataSetTable) {
            DataSetTableType tableType = ((DataSetTable)object).getType();
            if (tableType.equals(DataSetTableType.MAIN_SUBCLASS)) {
                component.setForeground(TableComponent.SUBCLASS_COLOUR);
            } else if (tableType.equals(DataSetTableType.DIMENSION)) {
                component.setForeground(TableComponent.DIMENSION_COLOUR);
            } else {
                component.setForeground(TableComponent.NORMAL_COLOUR);
            }
        }
        
        // Columns.
        else if (object instanceof Column) {
            
            Column column = (Column)object;
            
            // Fade out all MASKED columns.
            if (this.getDataSet().getMaskedDataSetColumns().contains(column)) {
                component.setForeground(ColumnComponent.FADED_COLOUR);
            }
            // Blue PARTITIONED columns and the schema name if partition on dataset.
            else if (this.getDataSet().getPartitionedWrappedColumns().contains(column) || ((column instanceof SchemaNameColumn) && this.getDataSet().getPartitionOnSchema())) {
                component.setForeground(ColumnComponent.PARTITIONED_COLOUR);
            }
            // All others are normal.
            else {
                component.setForeground(ColumnComponent.NORMAL_COLOUR);
            }
        }
    }
}
