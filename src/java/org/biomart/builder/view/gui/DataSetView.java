/*
 * DataSetView.java
 *
 * Created on 11 April 2006, 16:00
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

import javax.swing.JLabel;
import org.biomart.builder.model.DataSet;

/**
 * Displays the contents of a {@link DataSet} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class DataSetView extends TableProviderView implements TableProviderListener  {
    /**
     * Internal reference to the provider we are viewing.
     */
    private final DataSet dataset;
    
    /**
     * Creates a new instance of TableProviderView over a given dataset.
     * @param dataset the dataset to display.
     */
    public DataSetView(DataSet dataset) {
        super(dataset);
        this.setTableProviderListener(this);
        this.dataset = dataset;
    }
}
