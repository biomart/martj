/*
 * ViewComponent.java
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
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.PrimaryKey;

/**
 * An element that can be drawn on a View. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 24th April 2006
 * @since 0.1
 */
public class KeyComponent extends BoxComponent {
    /**
     * The component representing our parent box.
     */
    private BoxComponent parentComponent;
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public KeyComponent(Key key, View parentDisplay, BoxComponent parentComponent) {
        super(key, parentDisplay);
        this.parentComponent = parentComponent;   
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        // Create the border and set up the colors and fonts.
        this.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        this.setForeground(Color.DARK_GRAY);
        if (key instanceof PrimaryKey) this.setBackground(Color.CYAN);
        else this.setBackground(Color.GREEN);
        this.setFont(Font.decode("serif-ITALIC-8"));
        // Add the label for each column.
        for (Iterator i = key.getColumns().iterator(); i.hasNext(); ) {
            this.add(new JLabel(((Column)i.next()).getName()));
        }
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
    public BoxComponent getParentComponent() {
        return this.parentComponent;
    }
        
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = this.getParentComponent().getContextMenu();
        // Extend it for this table here.
        contextMenu.addSeparator();
        contextMenu.add(new JMenuItem("Hello from "+this.getKey()));
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
        
    /**
     * Set up the colours etc. for this component. Flags have already been set.
     */
    protected void setComponentColours() {
        // boolean xyFlagSet = this.getParentDisplay().getFlag(ComponentView.XYFLAG);
    }
}
