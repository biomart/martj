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
 * @version 0.1.5, 12th May 2006
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
    
    public void customiseAppearance(JComponent component, Object object) {
        if (this.selectedColumn==null) {
            super.customiseAppearance(component, object);
        } else {
            if (object instanceof Relation) {
                Relation relation = (Relation)object;
                Relation underlyingRelation = this.selectedColumn.getUnderlyingRelation();
                if (underlyingRelation!=null && underlyingRelation.equals(relation)) {
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
