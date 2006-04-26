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

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Window;

/**
 * Adapts listener events suitable for datasets.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 21st April 2006
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
    public DataSetDiagramModifier(WindowTabSet windowTabSet, Window window) {
        super(windowTabSet, window);
    }
 
    /**
     * Retrieves our dataset.
     * @return our dataset.
     */
    protected DataSet getDataSet() {
        return this.getWindow().getDataSet();
    }
}
