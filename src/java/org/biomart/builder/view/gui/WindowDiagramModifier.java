/*
 * WindowDiagramModifier.java
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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Adapts listener behaviour by adding in DataSet-specific stuff.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 2nd May 2006
 * @since 0.1
 */
public class WindowDiagramModifier extends SchemaDiagramModifier {
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
    public WindowDiagramModifier(DataSetTabSet datasetTabSet, DataSet dataset) {
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
    public void customiseContextMenu(JPopupMenu contextMenu, Object displayComponent) {
        // Add separator.
        contextMenu.addSeparator();
        // Add our own stuff.
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
