/*
 * DiagramComponent.java
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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 12th May 2006
 * @since 0.1
 */
public class RelationComponent extends JComponent implements DiagramComponent {
    /**
     * Constant referring to the width of a relation shape.
     */
    public static final float RELATION_LINEWIDTH = 1.0f; // 72 = 1 inch at 72 dpi
    
    /**
     * Constant referring to normal relation colour.
     */
    public static final Color NORMAL_COLOUR = Color.DARK_GRAY;
    
    /**
     * Constant referring to faded relation colour.
     */
    public static final Color FADED_COLOUR = Color.LIGHT_GRAY;
    
    /**
     * Constant referring to handmade relation colour.
     */
    public static final Color HANDMADE_COLOUR = Color.GREEN;
    
    /**
     * Constant referring to handmade relation colour.
     */
    public static final Color CONCAT_COLOUR = Color.BLUE;
    
    /**
     * Constant referring to handmade relation colour.
     */
    public static final Color SUBCLASS_COLOUR = Color.RED;
    
    /**
     * Constant defining our 1:M stroke.
     */
    public static final Stroke ONE_MANY = new BasicStroke(RelationComponent.RELATION_LINEWIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    /**
     * Constant defining our 1:1 stroke.
     */
    public static final Stroke ONE_ONE = new BasicStroke(RelationComponent.RELATION_LINEWIDTH * 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    /**
     * What do we look like?
     */
    private Shape shape;
    
    /**
     * Internal reference to our display parent.
     */
    private Diagram diagram;
    
    /**
     * Internal reference to the object we represent.
     */
    private Relation relation;
    
    /**
     * The keys we link.
     */
    private KeyComponent primaryKey;
    private KeyComponent foreignKey;
    
    private Object state;
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public RelationComponent(Relation relation, Diagram diagram, KeyComponent primaryKey, KeyComponent foreignKey) {
        super();
        this.relation = relation;
        this.diagram = diagram;
        this.primaryKey = primaryKey;
        this.foreignKey = foreignKey;
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.updateAppearance();
    }
    
    /**
     * Updates the tooltip.
     */
    public void updateAppearance() {
        this.setToolTipText(this.relation.getName());
        DiagramContext mod = this.getDiagram().getDiagramContext();
        if (mod != null) mod.customiseAppearance(this, this.getObject());
        this.setBackground(this.getForeground());
    }
    
    public Object getState() {
        return this.state;
    }
    
    public void setState(Object state) {
        this.state = state;
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
    public Diagram getDiagram() {
        return this.diagram;
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
        Stroke stroke = this.getStroke();
        this.shape = stroke.createStrokedShape(shape);
    }
    
    /**
     * Intercept clicks to see if they're in our shape's outline.
     */
    public boolean contains(int x, int y) {
        return this.shape != null && this.shape.intersects(
                new Rectangle2D.Double(
                x - RelationComponent.RELATION_LINEWIDTH,
                y - RelationComponent.RELATION_LINEWIDTH,
                RelationComponent.RELATION_LINEWIDTH * 2,
                RelationComponent.RELATION_LINEWIDTH * 2
                ));
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        // No additional entries for us yet.
        // Return it.
        return contextMenu;
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.isPopupTrigger() && this.diagram.getDiagramContext().isRightClickAllowed()) {
            // Build the basic menu.
            JPopupMenu contextMenu = this.getContextMenu();
            if (contextMenu.getComponentCount()>0) contextMenu.addSeparator();
            // Extend.
            this.getDiagram().getDiagramContext().populateContextMenu(contextMenu, this.getObject());
            // Display.
            if (contextMenu.getComponentCount()>0) {
                contextMenu.show(this, evt.getX(), evt.getY());
                eventProcessed = true;
            }
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * Work out what stroke to use.
     */
    private Stroke getStroke() {
        if (this.getRelation().getFKCardinality().equals(Cardinality.MANY)) return RelationComponent.ONE_MANY;
        else return RelationComponent.ONE_ONE;
    }
    
    /**
     * {@inheritDoc}
     */
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g.create();
        // Do painting of this component.
        g2d.setColor(this.getForeground());
        g2d.setStroke(this.getStroke());
        g2d.draw(this.shape);
        g2d.setColor(this.getBackground());
        g2d.fill(this.shape);
        // Clean up.
        g2d.dispose();
    }
}
