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
import org.biomart.builder.model.Relation;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Adapts listener events suitable for datasets.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 4th May 2006
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
                    getDataSetTabSet().confirmRemoveDataSet(dataset);
                }
            });
            contextMenu.add(remove);
            
            JMenuItem optimise = new JMenuItem(BuilderBundle.getString("optimiseDataSetTitle"));
            optimise.setMnemonic(BuilderBundle.getString("optimiseDataSetMnemonic").charAt(0));
            optimise.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().optimiseDataSet(dataset);
                }
            });
            contextMenu.add(optimise);
            
            JMenuItem rename = new JMenuItem(BuilderBundle.getString("renameDataSetTitle"));
            rename.setMnemonic(BuilderBundle.getString("renameDataSetMnemonic").charAt(0));
            rename.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    getDataSetTabSet().renameDataSet(dataset);
                }
            });
            contextMenu.add(rename);
        }
    }
    
    public void customiseColours(JComponent component, Object object) {
        if (object instanceof Relation) {
            Relation relation = (Relation)object;
            // All are normal.
            component.setForeground(RelationComponent.NORMAL_COLOUR);
        }
    }
}
