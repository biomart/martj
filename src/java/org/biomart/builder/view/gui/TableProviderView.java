/*
 * TableProviderView.java
 *
 * Created on 11 April 2006, 16:00
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

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import org.biomart.builder.model.TableProvider;

/**
 * Displays the contents of a {@link TableProvider} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class TableProviderView extends JComponent {
    /**
     * Internal reference to the provider we are viewing.
     */
    private final TableProvider tblProv;
    
    /**
     * Internal reference to the listener for the provider we are viewing.
     */
    private TableProviderListener tblProvListener;
    
    /**
     * Creates a new instance of TableProviderView over a given provider.
     * @param tblProv the given table provider.
     */
    public TableProviderView(TableProvider tblProv) {
        super();
        this.tblProv = tblProv;
    }
    
    /**
     * Sets the listener to use.
     * @param tblProvListener the listener that will be told when the view is interacted with.
     */
    public void setTableProviderListener(TableProviderListener tblProvListener) {
        this.tblProvListener = tblProvListener;
    }
    
    /**
     * {@inheritDoc}
     */
    protected void paintComponent(Graphics g) {
        // Paint background.
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        Graphics2D g2d = (Graphics2D)g.create();
        // Do painting of this table provider.
        // Clean up.
        g2d.dispose();
    }
}
