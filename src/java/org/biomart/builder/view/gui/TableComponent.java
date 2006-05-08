/*
 * TableComponent.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 8th May 2006
 * @since 0.1
 */
public class TableComponent extends BoxShapedComponent {
    /**
     * Colours
     */
    public static final Color SUBCLASS_COLOUR = Color.RED;
    public static final Color DIMENSION_COLOUR = Color.BLUE;
    public static final Color NORMAL_COLOUR = Color.BLACK;
    
    /**
     * A map of keys to key components.
     */
    private Map keyToKeyComponent = new HashMap();
    
    /**
     * The constructor constructs an object around a given
     * object, and associates with a given display.
     */
    public TableComponent(Table table, Diagram diagram) {
        super(table, diagram);
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        // Create the border and set up the colors and fonts.
        this.setBackground(Color.PINK);
        // Add the label.
        JLabel label = new JLabel(table.getName());
        label.setFont(Font.decode("Serif-BOLD-10"));
        this.add(label);
        // Now the keys.
        for (Iterator i = table.getKeys().iterator(); i.hasNext(); ) {
            Key key = (Key)i.next();
            KeyComponent keyComponent = new KeyComponent(key, diagram, this);
            this.keyToKeyComponent.put(key, keyComponent);
            this.add(keyComponent);
        }
    }
    
    /**
     * Gets our table.
     */
    private Table getTable() {
        return (Table)this.getObject();
    }
    
    /**
     * Gets a key component.
     */
    public Map getKeyComponents() {
        return this.keyToKeyComponent;
    }
    
    /**
     * Count the relations attached to our inner object.
     */
    public int countRelations() {
        return this.getTable().getRelations().size();
    }
    
    /**
     * Construct a context menu for a given view.
     * @return the popup menu.
     */
    public JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
       
        JMenuItem manager = new JMenuItem(BuilderBundle.getString("tableManagerTitle"));
        manager.setMnemonic(BuilderBundle.getString("tableManagerMnemonic").charAt(0));
        manager.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getDiagram().getDataSetTabSet().getSchemaTabSet().requestTableManager(getTable(), getDiagram().getDiagramContext());
            }
        });
        contextMenu.add(manager);
        
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
}
