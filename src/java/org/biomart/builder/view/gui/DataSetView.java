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

import javax.swing.JPopupMenu;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.model.Window;

/**
 * Displays the contents of a {@link DataSet} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class DataSetView extends TableProviderView implements TableProviderListener  {
    /**
     * Internal reference to the WindowView we are viewing the dataset for.
     */
    private final WindowView windowView;
    
    /**
     * Creates a new instance of TableProviderView over a given dataset.
     * @param windowView the WindowView owning dataset to display.
     */
    public DataSetView(WindowView windowView) {
        super(windowView.getWindow().getDataSet());
        this.setTableProviderListener(this);
        this.windowView= windowView;
    }
    
    /**
     * Returns the parent window view.
     * @return the parent WindowView.
     */
    public WindowView getWindowView() {
        return this.windowView;
    }
    
    /**     
     * {@inheritDoc}
     */
    public void synchroniseAll() {
        this.windowView.synchroniseAll();
    }
    
    /**
     * {@inheritDoc}
     */
    public void synchroniseTableProvider(TableProvider tblProv) {
        this.windowView.synchroniseTableProvider(tblProv);
    }
    
    /**
     * {@inheritDoc}
     */
    public void testTableProvider(TableProvider tblProv) {
        this.windowView.testTableProvider(tblProv);
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeTableProvider(TableProvider tblProv) {
        this.windowView.removeTableProvider(tblProv);
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeWindow(Window window) {
        this.windowView.removeWindow(window);
    }
    
    /**
     * {@inheritDoc}
     */
    public void synchroniseWindow(Window window) {
        this.windowView.synchroniseWindow(window);
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestRecalculateVisibleView() {
        this.windowView.requestRecalculateVisibleView();
    }
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object displayComponent) {
        this.windowView.customiseContextMenu(contextMenu, displayComponent);
    }
}
