/*
 * WindowContext.java
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Adapts listener behaviour by adding in DataSet-specific stuff.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 11th May 2006
 * @since 0.1
 */
public class ExplainDataSetContext extends WindowContext {
    private DataSetColumn selectedColumn;
    
    private static final Color ENROUTE_COLOUR = Color.ORANGE;
    private static final Color TARGET_COLOUR = Color.MAGENTA;
    private static final Color FADED_COLOUR = Color.LIGHT_GRAY;
    
    /**
     *
     * Creates a new instance of DataSetDiagramModifier over
     * a given dataset.
     *
     *
     * @param dataset the dataset we are attached to.
     */
    public ExplainDataSetContext(DataSetTabSet datasetTabSet, DataSet dataset) {
        super(datasetTabSet, dataset);
    }
    
    public void setSelectedColumn(DataSetColumn selectedColumn) {
        this.selectedColumn = selectedColumn;
    }
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object object) {
        if (object instanceof Relation) {
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
            
            JMenuItem mask = new JMenuItem(BuilderBundle.getString("maskRelationTitle"));
            mask.setMnemonic(BuilderBundle.getString("maskRelationMnemonic").charAt(0));
            mask.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestMaskRelation(ds, relation);
                }
            });
            contextMenu.add(mask);
            if (incorrect || relationMasked) mask.setEnabled(false);
            JMenuItem unmask = new JMenuItem(BuilderBundle.getString("unmaskRelationTitle"));
            unmask.setMnemonic(BuilderBundle.getString("unmaskRelationMnemonic").charAt(0));
            unmask.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestUnmaskRelation(ds, relation);
                }
            });
            contextMenu.add(unmask);
            if (incorrect || !relationMasked) unmask.setEnabled(false);
            
            JMenuItem subclass = new JMenuItem(BuilderBundle.getString("subclassRelationTitle"));
            subclass.setMnemonic(BuilderBundle.getString("subclassRelationMnemonic").charAt(0));
            subclass.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestSubclassRelation(ds, relation);
                }
            });
            contextMenu.add(subclass);
            if (incorrect || relationSubclassed || relationOneToOne || relationMasked || relationConcated) subclass.setEnabled(false);
            JMenuItem unsubclass = new JMenuItem(BuilderBundle.getString("unsubclassRelationTitle"));
            unsubclass.setMnemonic(BuilderBundle.getString("unsubclassRelationMnemonic").charAt(0));
            unsubclass.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestUnsubclassRelation(ds, relation);
                }
            });
            contextMenu.add(unsubclass);
            if (incorrect || !relationSubclassed || relationOneToOne || relationMasked || relationConcated) unsubclass.setEnabled(false);
            
            JMenuItem concat = new JMenuItem(BuilderBundle.getString("concatOnlyRelationTitle"));
            concat.setMnemonic(BuilderBundle.getString("concatOnlyRelationMnemonic").charAt(0));
            concat.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestConcatOnlyRelation(ds, relation);
                }
            });
            contextMenu.add(concat);
            if (incorrect || relationConcated || relationOneToOne || relationMasked || relationSubclassed) concat.setEnabled(false);
            JMenuItem changeConcat = new JMenuItem(BuilderBundle.getString("changeConcatOnlyRelationTitle"));
            changeConcat.setMnemonic(BuilderBundle.getString("changeConcatOnlyRelationMnemonic").charAt(0));
            changeConcat.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestConcatOnlyRelation(ds, relation);
                }
            });
            contextMenu.add(changeConcat);
            if (incorrect || !relationConcated || relationOneToOne || relationMasked || relationSubclassed) changeConcat.setEnabled(false);
            JMenuItem unconcat = new JMenuItem(BuilderBundle.getString("unconcatOnlyRelationTitle"));
            unconcat.setMnemonic(BuilderBundle.getString("unconcatOnlyRelationMnemonic").charAt(0));
            unconcat.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestUnconcatOnlyRelation(ds, relation);
                }
            });
            contextMenu.add(unconcat);
            if (incorrect || !relationConcated || relationOneToOne || relationMasked || relationSubclassed) unconcat.setEnabled(false);
        }
        
        else if (object instanceof Key) {
            // Keys just show the parent table stuff.
            Table table = ((Key)object).getTable();
            this.customiseContextMenu(contextMenu, table);
        }
        
        else if (object instanceof Column) {
            // Columns show the parent table stuff.
            Table table = ((Column)object).getTable();
            this.customiseContextMenu(contextMenu, table);
            
            // AND they show Column stuff
            final Column column = (Column)object;
            final DataSet ds = this.getDataSetTabSet().getSelectedDataSetTab().getDataSet();
            boolean isMasked = ds.getMaskedColumns().contains(column);
            
            // Add separator.
            contextMenu.addSeparator();
            
            // Add column stuff.
            JMenuItem mask = new JMenuItem(BuilderBundle.getString("maskColumnTitle"));
            mask.setMnemonic(BuilderBundle.getString("maskColumnMnemonic").charAt(0));
            mask.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestMaskColumn(ds, column);
                }
            });
            contextMenu.add(mask);
            if (isMasked) mask.setEnabled(false);
            
            JMenuItem unmask = new JMenuItem(BuilderBundle.getString("unmaskColumnTitle"));
            unmask.setMnemonic(BuilderBundle.getString("unmaskColumnMnemonic").charAt(0));
            unmask.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().requestUnmaskColumn(ds, column);
                }
            });
            contextMenu.add(unmask);
            if (!isMasked) unmask.setEnabled(false);
        }
    }
    
    public void customiseAppearance(JComponent component, Object object) {
        if (selectedColumn==null) {
            super.customiseAppearance(component, object);
        } else {
            if (object instanceof Relation) {
                Relation relation = (Relation)object;
                if (selectedColumn.getUnderlyingRelation()!=null && selectedColumn.getUnderlyingRelation().equals(relation)) {
                    if (selectedColumn instanceof ConcatRelationColumn) {
                        // Highlight relation as target.
                        component.setForeground(ExplainDataSetContext.TARGET_COLOUR);
                    } else {
                        // Highlight relation en route.
                        component.setForeground(ExplainDataSetContext.ENROUTE_COLOUR);
                    }
                } else {
                    // Normal relation. Faded.
                    component.setForeground(ExplainDataSetContext.FADED_COLOUR);
                }
            } else if (object instanceof Table) {
                Table table = (Table)object;
                if ((selectedColumn instanceof WrappedColumn) && table.getColumns().contains(((WrappedColumn)selectedColumn).getWrappedColumn())) {
                    // Highlight relation as target.
                    component.setForeground(ExplainDataSetContext.TARGET_COLOUR);
                } else if (selectedColumn instanceof SchemaNameColumn && table.equals(((DataSetTable)this.selectedColumn.getTable()).getUnderlyingTable())) {
                    // Highlight relation as target.
                    component.setForeground(ExplainDataSetContext.TARGET_COLOUR);
                } else if (selectedColumn.getUnderlyingRelation()!=null && table.getRelations().contains(selectedColumn.getUnderlyingRelation())) {
                    // Highlight relation en route.
                    component.setForeground(ExplainDataSetContext.ENROUTE_COLOUR);
                } else {
                    // Normal relation. Faded.
                    component.setForeground(ExplainDataSetContext.FADED_COLOUR);
                }
            } else if (object instanceof Column) {
                Column column = (Column)object;
                if ((selectedColumn instanceof WrappedColumn) && column.equals(((WrappedColumn)selectedColumn).getWrappedColumn())) {
                    // Highlight relation as target.
                    component.setForeground(ExplainDataSetContext.TARGET_COLOUR);
                } else {
                    // Normal relation. Faded.
                    component.setForeground(ExplainDataSetContext.FADED_COLOUR);
                }
            }
        }
    }
}
