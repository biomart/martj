/*
 * View.java
 *
 * Created on 19 April 2006, 09:48
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

/**
 * All the methods required for table provider views to interact with the mothership.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public interface View {
    /**
     * Returns the component of this view.
     * @return the component of this view.
     */
    public JComponent asJComponent();
    
    /**
     * Set the listener to send events to. If null, an exception is thrown.
     * @param listener the listener to send events to.
     * @throws NullPointerException if the listener is null.
     */
    public void setListener(Listener listener) throws NullPointerException;
    
    /**
     * Get the listener which events are being sent to.
     * @return the listener events are going to.
     */
    public Listener getListener();
    
    /**
     * Recalculate the display.
     */
    public void recalculateView();
}
