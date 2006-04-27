/*
 * SchemaDiagramModifier.java
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Provides the default behaviour for table provider listeners.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 27th April 2006
 * @since 0.1
 */
public class SchemaDiagramModifier implements DiagramModifier {
    /**
     * The window set we belong to.
     */
    protected DataSetTabSet datasetTabSet;
    
    /**
     * Creates a new instance of SchemaDiagramModifier and binds it to a given
     * MartBuilder instance.
     */
    public SchemaDiagramModifier(DataSetTabSet datasetTabSet) {
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
        Table table = null;
        if (object instanceof Table) table = (Table)object;
        else if (object instanceof Key) table = ((Key)object).getTable();
        
        if (table != null) {
            final Table tableRef = table;
            // Add the dataset generation options.
            contextMenu.addSeparator();
            
            JMenuItem create = new JMenuItem(BuilderBundle.getString("createDataSetTitle", table.getName()));
            create.setMnemonic(BuilderBundle.getString("createDataSetMnemonic").charAt(0));
            create.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.createDataSet(tableRef);
                }
            });
            contextMenu.add(create);
            
            JMenuItem suggest = new JMenuItem(BuilderBundle.getString("suggestDataSetsTitle", table.getName()));
            suggest.setMnemonic(BuilderBundle.getString("suggestDataSetsMnemonic").charAt(0));
            suggest.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    datasetTabSet.suggestDataSets(tableRef);
                }
            });
            contextMenu.add(suggest);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void aboutToDraw(Object object) {
        // Nothing special required here. Only datasets and windows
        // may care - masked, concat, etc.
    }
}
