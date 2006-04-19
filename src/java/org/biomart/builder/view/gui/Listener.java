/*
 * Listener.java
 *
 * Created on 11 April 2006, 16:52
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
 * Listener for events of interest to classes displaying graphical views
 * of {@link TableProvider}s using {@link TableProviderView}.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public interface Listener {
    /**
     * Add items to a context menu for a given component. Must add separator
     * if required.
     * @param contextMenu the context menu to add parameters to.
     * @param displayComponent the display component we wish to customise this menu to.
     * @return the popup menu.
     */
    public void requestCustomiseContextMenu(JPopupMenu contextMenu, Object displayComponent);
    
    /**
     * Causes the entire schema to be resynchronised. Should be passed back up to
     * {@link MartBuilder#requestSynchroniseAll()}. No other action should be necessary.
     */
    public void requestSynchroniseAll();
    
    /**
     * A signal to synchronise some table provider. Should be passed back up to
     * {@link MartBuilder#requestSynchroniseTableProvider(TableProvider)}. No other
     * action should be necessary.
     * 
     * @param tblProv the table provider to sync.
     */
    public void requestSynchroniseTableProvider(TableProvider tblProv);
    
    /**
     * A signal to test some table provider. Should be passed back up to
     * {@link MartBuilder#requestTestTableProvider(TableProvider)}. No other
     * action should be necessary.
     * 
     * @param tblProv the table provider to test.
     */
    public void requestTestTableProvider(TableProvider tblProv);
    
    /**
     * A signal to delete some table provider. Should be passed back up to
     * {@link MartBuilder#requestRemoveTableProvider(TableProvider)}. No other
     * action should be necessary.
     * 
     * @param tblProv the table provider to delete.
     */
    public void requestRemoveTableProvider(TableProvider tblProv);
}
