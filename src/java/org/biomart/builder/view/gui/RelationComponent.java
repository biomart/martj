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

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Relation;

/**
 * An element that can be drawn on a View. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 24th April 2006
 * @since 0.1
 */
public class RelationComponent extends JComponent implements ViewComponent {
    /**
     * Constant referring to the width of a relation shape.
     */
    public static final float RELATION_LINEWIDTH = 2.0f; // 72 = 1 inch at 72 dpi
    
    /**
     * What do we look like?
     */
    private Shape shape;
    
    /**
     * Internal reference to our display parent.
     */
    private View parentDisplay;
    
    /**
     * Internal reference to the object we represent.
     */
    private Relation relation;
    
    /**
     * The keys we link.
     */
    private KeyComponent primaryKey;
    private KeyComponent foreignKey;
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public RelationComponent(Relation relation, View parentDisplay, KeyComponent primaryKey, KeyComponent foreignKey) {
        this.relation = relation;
        this.parentDisplay = parentDisplay;
        this.primaryKey = primaryKey;
        this.foreignKey = foreignKey;
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }
    
    /**
     * Retrieves the primary key component.
     */
    public KeyComponent getPrimaryKeyComponent() {
        return this.primaryKey;
    }
    
    /**
     * Retrieves the foreign key component.
     */
    public KeyComponent getForeignKeyComponent() {
        return this.foreignKey;
    }
    
    /**
     * Retrieves the parent this component belongs to.
     * @return the parent.
     */
    public View getView() {
        return this.parentDisplay;
    }
    
    /**
     * Retrieves the real object this component is a representation of.
     * @return the real object.
     */
    public Object getObject() {
        return this.relation;
    }
    
    /**
     * Retrieves the real object this component is a representation of.
     * @return the real object.
     */
    private Relation getRelation() {
        return this.relation;
    }
    
    /**
     * Sets the shape for us to display the outline of.
     */
    public void setShape(Shape shape) {
        Stroke stroke = this.getStroke(this.getFlags());
        this.shape = stroke.createStrokedShape(shape);
    }
    
    /**
     * Intercept clicks to see if they're in our shape's outline.
     */
    public boolean contains(int x, int y) {
        return this.shape != null && this.shape.contains(x, y);
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = this.getView().getContextMenu();
        // Extend it for this table here.
        contextMenu.addSeparator();
        contextMenu.add(new JMenuItem("Hello from "+this.getRelation()));
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.isPopupTrigger()) {
            // Build the basic menu.
            JPopupMenu contextMenu = this.getContextMenu();
            // Extend.
            this.getView().getAdaptor().customiseContextMenu(contextMenu, this.getRelation());
            // Display.
            contextMenu.show(this, evt.getX(), evt.getY());
            eventProcessed = true;
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void paintComponent(Graphics g) {
        // Paint background, if required.
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        Graphics2D g2d = (Graphics2D)g.create();
        // Do painting of this component.
        this.getView().clearFlags();
        this.getView().getAdaptor().aboutToDraw(this.getRelation());
        this.paintComponent(g2d, this.getFlags());
        // Clean up.
        g2d.dispose();
    }
    
    /**
     * Work out what stroke to use.
     */
    private int getFlags() {
        this.getView().clearFlags();
        this.getView().getAdaptor().aboutToDraw(this.getRelation());
        return this.getView().getFlags();
    }
    
    /**
     * Work out what stroke to use.
     */
    private Stroke getStroke(int flags) {
        return new BasicStroke(RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
    
    /**
     * Override this to do your painting. The background has already
     * been done for you if you are opaque. Flags are already set.
     */
    private void paintComponent(Graphics2D g2d, int flags) {
        // Do the drawing here!
        g2d.setStroke(this.getStroke(flags));
        g2d.draw(this.shape);
        g2d.fill(this.shape);
    }
}
