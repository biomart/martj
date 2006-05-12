/*
 * KeyComponent.java
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

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.PrimaryKey;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 12th May 2006
 * @since 0.1
 */
public class ColumnComponent extends BoxShapedComponent {
    
    /**
     * Constant referring to normal relation colour.
     */
    public static final Color NORMAL_COLOUR = Color.DARK_GRAY;
    
    /**
     * Constant referring to faded relation colour.
     */
    public static final Color FADED_COLOUR = Color.LIGHT_GRAY;
    
    /**
     * Constant referring to faded relation colour.
     */
    public static final Color PARTITIONED_COLOUR = Color.BLUE;
    
    /**
     * The component representing our parent box.
     */
    private BoxShapedComponent parentComponent;
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public ColumnComponent(Column column, Diagram diagram, BoxShapedComponent parentComponent) {
        super(column, diagram);
        this.parentComponent = parentComponent;
        // Create the border and set up the colors and fonts.
        this.setBackground(Color.ORANGE);
        // Add the label for each column.
        JLabel label = new JLabel(column.getName());
        label.setFont(Font.decode("Serif-ITALIC-10"));
        this.add(label);
    }
    
    /**
     * Gets our table.
     */
    private Key getKey() {
        return (Key)this.getObject();
    }
    
    /**
     * Gets our parent component.
     */
    public BoxShapedComponent getParentComponent() {
        return this.parentComponent;
    }
}
