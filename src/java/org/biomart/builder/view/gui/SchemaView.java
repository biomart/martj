/*
 * SchemaView.java
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
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.model.Window;

/**
 * Displays the contents of a {@link Schema} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class SchemaView extends MultiTableProviderView implements TableProviderListener {
    /**
     * Internal reference to the parent mart builder.
     */
    private final MartBuilder martBuilder;
    
    /**
     * Creates a new instance of TableProviderView over a given MartBuilder schema.
     * @param martBuilder the MartBuilder to display the schema for.
     */
    public SchemaView(MartBuilder martBuilder) {
        super(martBuilder.getSchema().getTableProviders());
        this.setTableProviderListener(this);
        this.martBuilder = martBuilder;
    }
    
    /**
     * Returns the parent mart builder.
     * @return the parent mart builder.
     */
    public MartBuilder getMartBuilder() {
        return this.martBuilder;
    }  
    
    /**     
     * {@inheritDoc}
     */
    public void synchroniseAll() {
        this.martBuilder.synchroniseAll();
    }
    
    /**
     * {@inheritDoc}
     */
    public void synchroniseTableProvider(TableProvider tblProv) {
        this.martBuilder.synchroniseTableProvider(tblProv);
    }  
    
    /**
     * {@inheritDoc}
     */
    public void testTableProvider(TableProvider tblProv) {
        this.martBuilder.testTableProvider(tblProv);
    }  
    
    /**
     * {@inheritDoc}
     */
    public void removeTableProvider(TableProvider tblProv) {
        this.martBuilder.removeTableProvider(tblProv);
    }
    
    /**
     * {@inheritDoc}
     */
    public void synchroniseWindow(Window window) {
        this.martBuilder.synchroniseWindow(window);
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeWindow(Window window) {
        this.martBuilder.removeWindow(window, true);
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestRecalculateVisibleView() {
        this.recalculateVisibleView();
    }
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object displayComponent) {
        // Nothing extra needed here.
    }
}
