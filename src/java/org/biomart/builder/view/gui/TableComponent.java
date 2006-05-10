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
import java.util.Iterator;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Table;
import org.biomart.builder.resources.BuilderBundle;

/**
 * An element that can be drawn on a Diagram. Two Comparators
 * are provided for sorting them, as they are not comparable within themselves.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 10th May 2006
 * @since 0.1
 */
public class TableComponent extends BoxShapedComponent {
    /**
     * Colours
     */
    public static final Color SUBCLASS_COLOUR = Color.RED;
    public static final Color DIMENSION_COLOUR = Color.BLUE;
    public static final Color NORMAL_COLOUR = Color.BLACK;
    
    private JButton showHide;
    private JComponent colsPanel;
    
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
            this.addSubComponent(key, keyComponent);
            this.add(keyComponent);
        }
        // Now the columns, in a panel of their own.
        this.colsPanel = Box.createVerticalBox();
        for (Iterator i = table.getColumns().iterator(); i.hasNext(); ) {
            Column col = (Column)i.next();
            ColumnComponent colComponent = new ColumnComponent(col, diagram, this);
            this.addSubComponent(col, colComponent);
            this.colsPanel.add(colComponent);
        }
        // Show/hide the columns panel with a button.
        this.showHide = new JButton(BuilderBundle.getString("showColumnsButton"));
        this.showHide.setFont(Font.decode("Serif-BOLD-10"));
        this.add(showHide);
        showHide.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getState().equals(Boolean.TRUE)) {
                    setState(Boolean.FALSE);
                } else {
                    setState(Boolean.TRUE);
                }
            }
        });
        this.setState(Boolean.FALSE);
    }
    
    public void setState(Object state) {
        if (state!=null && state.equals(Boolean.TRUE)) {
            if (this.getState()==null || !this.getState().equals(Boolean.TRUE)) this.add(this.colsPanel);
            this.showHide.setText(BuilderBundle.getString("hideColumnsButton"));
        } else {
            if (this.getState()!=null && this.getState().equals(Boolean.TRUE)) this.remove(this.colsPanel);
            this.showHide.setText(BuilderBundle.getString("showColumnsButton"));
        }
        this.validate();
        super.setState(state);
    }
    
    /**
     * Gets our table.
     */
    private Table getTable() {
        return (Table)this.getObject();
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
        
        // Nothing to add.
        
        // Return it. Will be further adapted by a listener elsewhere.
        return contextMenu;
    }
}
