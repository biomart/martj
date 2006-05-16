/*
 * DiagramContext.java
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

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * DiagramContext for events of interest to classes displaying graphical views
 * of {@link TableProvider}s using {@link TableProviderView}.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.9, 16th May 2006
 * @since 0.1
 */
public interface DiagramContext {
    /**
     * Add items to a context menu for a given component. Must add separator
     * if required.
     * @param contextMenu the context menu to add parameters to.
     * @param displayComponent the display component we wish to customise this menu to.
     * @return the popup menu.
     */
    public void populateContextMenu(JPopupMenu contextMenu, Object object);
    
    public void customiseAppearance(JComponent component, Object object);
}
