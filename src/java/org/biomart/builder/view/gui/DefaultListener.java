/*
 * DefaultListener.java
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
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.model.Window;

/**
 * Provides the default behaviour for table provider listeners.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.0.1, 19th April 2006
 * @since 0.1
 */
public class DefaultListener implements Listener {
    /**
     * Internal reference to the MartBuilder this listener refers to.
     */
    private MartBuilder martBuilder;
    
    /** 
     * Creates a new instance of DefaultListener and binds it to a given
     * MartBuilder instance.
     * @param martBuilder the MartBuilder to bind this listener to.
     */
    public DefaultListener(MartBuilder martBuilder) {
        this.martBuilder = martBuilder;
    }

    /**
     * Gets the MartBuilder this listener is attached to.
     * @return the MartBuilder.
     */
    protected MartBuilder getMartBuilder() {
        return this.martBuilder;
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestCustomiseContextMenu(JPopupMenu contextMenu, Object displayComponent) {
        // Do nothing by default.
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestSynchroniseAll() {
        this.getMartBuilder().synchroniseAll();
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestSynchroniseTableProvider(TableProvider tblProv) {
        this.getMartBuilder().synchroniseTableProvider(tblProv);
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestTestTableProvider(TableProvider tblProv) {
        this.getMartBuilder().testTableProvider(tblProv);
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestRemoveTableProvider(TableProvider tblProv) {
        this.getMartBuilder().removeTableProvider(tblProv);
    }
    
    /**
     * {@inheritDoc}
     */
    public void requestObjectFlags(Object displayComponent) {
        // Nothing special required here. Only datasets and windows
        // may care - masked, concat, etc.
    }
}
