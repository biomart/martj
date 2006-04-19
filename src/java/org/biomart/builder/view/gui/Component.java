/*
 * Component.java
 *
 * Created on 19 April 2006, 15:36
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

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Comparator;
import java.util.Map;

/**
 * An element that can be drawn on a ComponentDisplay. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public interface Component {
    /**
     * Retrieves the real object this component is a representation of.
     * @return the real object.
     */
    public Object getObject();    
    
    /**
     * Gets the width of this component.
     * @return the width.
     */
    public double getWidth();

    /**
     * Gets the height of this component.
     * @return the height.
     */
    public double getHeight();
    
    /**
     * Paints this component at the given coordinates on the given canvas.
     * @param g2d the Graphics2D canvas to paint ourselves on.
     * @param topLeft the top-left corner to paint ourselves at.
     * @param the flags controlling our display, from ComponentDisplay.
     */
    public void paint(Graphics2D g2d, Point topLeft, int flags);
    
    /**
     * Returns any relations that link keys within us to other components. These 
     * are actual Relation objects. The Key objects they refer to can be found 
     * as child components. The keys of this map are boxes outlining the coordinates
     * of the key within us, relative to our own top-left corner. The values are
     * the relations themselves. 
     * @return a map (never null but may be empty) of relation objects.
     */
    public Map getRelations();
    
    /**
     * This Comparator sorts by descending width. 
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public class DescendingWidth implements Comparator {
        public int compare(Object a, Object b) throws ClassCastException {
            return (int)(((Component)a).getWidth() - ((Component)b).getWidth());
        }
    }
    
    /**
     * This Comparator sorts by descending height. 
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public class DescendingHeight implements Comparator {
        public int compare(Object a, Object b) throws ClassCastException {
            return (int)(((Component)a).getHeight() - ((Component)b).getHeight());
        }
    }
}
