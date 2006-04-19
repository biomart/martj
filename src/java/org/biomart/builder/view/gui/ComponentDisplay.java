/*
 * ComponentDisplay.java
 *
 * Created on 19 April 2006, 13:35
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

/**
 * An interface for things that actually draw things.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public interface ComponentDisplay {
    /**
     * Reset the current display mask.
     */
    public void clearFlags();
    
    /**
     * Set a particular flag on the display mask.
     * @param flag the flag to set.
     */
    public void setFlag(int flag);

    /**
     * Test for a particular display mask flag.
     * @param flag the flag to test for.
     * @return true if it is set, false if not.
     */
    public boolean getFlag(int flag);

    /**
     * Get the current display mask.
     * @return the current display mask.
     */
    public int getFlags();
}
