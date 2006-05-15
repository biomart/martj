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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.TransferHandler;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.PrimaryKey;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 15th May 2006
 * @since 0.1
 */
public class KeyComponent extends BoxShapedComponent {
    /**
     * Constant referring to normal relation colour.
     */
    public static final Color NORMAL_COLOUR = Color.DARK_GRAY;
    
    /**
     * Constant referring to faded relation colour.
     */
    public static final Color MASKED_COLOUR = Color.LIGHT_GRAY;
    
    /**
     * Constant referring to faded relation colour.
     */
    public static final Color INCORRECT_COLOUR = Color.RED;
    
    /**
     * Constant referring to handmade relation colour.
     */
    public static final Color HANDMADE_COLOUR = Color.GREEN;
    /**
     * The component representing our parent box.
     */
    private BoxShapedComponent parentComponent;
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public KeyComponent(Key key, Diagram diagram, BoxShapedComponent parentComponent) {
        super(key, diagram);
        this.parentComponent = parentComponent;
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.recalculateDiagramComponent();
    }
    
    public void recalculateDiagramComponent() {
        this.removeAll();
        Key key = this.getKey();
        // Create the border and set up the colors and fonts.
        if (key instanceof PrimaryKey) this.setBackground(Color.CYAN);
        else this.setBackground(Color.GREEN);
        // Add the label for each column.
        for (Iterator i = key.getColumns().iterator(); i.hasNext(); ) {
            JLabel label = new JLabel(((Column)i.next()).getName());
            label.setFont(Font.decode("Serif-ITALIC-10"));
            this.add(label);
        }
        // Add drag-and-drop.
        this.setTransferHandler(new TransferHandler("draggedKey"));
        this.addMouseListener(new DragMouseAdapter());
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
    
    public Key getDraggedKey() {
        return this.getKey();
    }
    
    public void setDraggedKey(Key key) {
        if (!key.equals(this)) {
            this.getDiagram().getDataSetTabSet().getSchemaTabSet().requestCreateRelation(key, this.getKey());
        }
    }
    
    public class DragMouseAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            JComponent c = (JComponent)e.getSource();
            TransferHandler handler = c.getTransferHandler();
            handler.exportAsDrag(c, e, TransferHandler.COPY);
        }
    }
}
