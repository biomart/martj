/*
 * DefaultAdaptor.java
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

import javax.swing.JPopupMenu;

/**
 * Provides the default behaviour for table provider listeners.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 21st April 2006
 * @since 0.1
 */
public class DefaultAdaptor implements Adaptor {   
    /**
     * The window set we belong to.
     */
    protected WindowTabSet windowTabSet;
    
    /**
     * Creates a new instance of DefaultAdaptor and binds it to a given
     * MartBuilder instance.
     */
    public DefaultAdaptor(WindowTabSet windowTabSet) {
        this.windowTabSet = windowTabSet;
    }
    
    /**
     * {@inheritDoc}
     */
    public void customiseContextMenu(JPopupMenu contextMenu, Object object) {
        // Do nothing.
    }
        
    /**
     * {@inheritDoc}
     */
    public void aboutToDraw(Object object) {
        // Nothing special required here. Only datasets and windows
        // may care - masked, concat, etc.
    }
}
