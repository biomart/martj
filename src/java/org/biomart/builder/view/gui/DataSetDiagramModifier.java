/*
 * DataSetDiagramModifier.java
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
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Adapts listener events suitable for datasets.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 5th May 2006
 * @since 0.1
 */
public class DataSetDiagramModifier extends WindowDiagramModifier {
    /**
     *
     * Creates a new instance of DataSetDiagramModifier over
     * a given window.
     *
     *
     * @param window the window whose dataset we are attached to.
     */
    public DataSetDiagramModifier(DataSetTabSet datasetTabSet, DataSet dataset) {
        super(datasetTabSet, dataset);
    }
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object object) {
        if (object instanceof DataSet) {
            // DataSet stuff.
            final DataSet dataset = (DataSet)object;
            
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
        
        else if (object instanceof DataSetTable) {
            // DataSet table stuff.
            final DataSetTable table = (DataSetTable)object;
            DataSetTableType tableType = table.getType();
            
            // Add separator.
            contextMenu.addSeparator();
            
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
            Table table = ((Key)object).getTable();
            this.customiseContextMenu(contextMenu, table);
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
    }
}
