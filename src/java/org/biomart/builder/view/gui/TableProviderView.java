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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Displays the contents of a {@link TableProvider} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class TableProviderView extends JComponent {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.WHITE;
    
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
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.setBackground(TableProviderView.BACKGROUND_COLOUR);
    }
    
    /**
     * Obtains the table provider this view displays.
     * @return the table provider this view displays.
     */
    public TableProvider getTableProvider() {
        return this.tblProv;
    }
    
    /**
     * Sets the listener to use.
     * @param tblProvListener the listener that will be told when the view is interacted with.
     */
    public void setTableProviderListener(TableProviderListener tblProvListener) {
        this.tblProvListener = tblProvListener;
    }
    
    /**
     * Returns the listener to use.
     * @return tblProvListener the listener that will be told when the view is interacted with.
     */
    public TableProviderListener getTableProviderListener() {
        return this.tblProvListener;
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @param displayComponent the display component we wish to customise this menu to. It may
     * be null, so watch out for this.
     * @return the popup menu.
     */
    protected JPopupMenu getContextMenu(Object displayComponent) {
        JPopupMenu contextMenu = new JPopupMenu();
        // The following are applicable to all table provider views.
        final JMenuItem redraw = new JMenuItem(BuilderBundle.getString("redrawTitle"));
        redraw.setMnemonic(BuilderBundle.getString("redrawMnemonic").charAt(0));
        redraw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                tblProvListener.requestRecalculateVisibleView();
            }
        });
        contextMenu.add(redraw);
        // The following are not applicable to DataSetViews.
        if (!(this instanceof DataSetView)) {
            contextMenu.addSeparator();
            final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseTblProvTitle", this.tblProv.getName()));
            sync.setMnemonic(BuilderBundle.getString("synchroniseTblProvMnemonic").charAt(0));
            sync.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    tblProvListener.synchroniseTableProvider(tblProv);
                }
            });
            contextMenu.add(sync);
            final JMenuItem test = new JMenuItem(BuilderBundle.getString("testTblProvTitle", this.tblProv.getName()));
            test.setMnemonic(BuilderBundle.getString("testTblProvMnemonic").charAt(0));
            test.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    tblProvListener.testTableProvider(tblProv);
                }
            });
            contextMenu.add(test);
            final JMenuItem remove = new JMenuItem(BuilderBundle.getString("removeTblProvTitle", this.tblProv.getName()));
            remove.setMnemonic(BuilderBundle.getString("removeTblProvMnemonic").charAt(0));
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    tblProvListener.removeTableProvider(tblProv);
                }
            });
            contextMenu.add(remove);
        }
        // Extend and return.
        this.tblProvListener.customiseContextMenu(contextMenu, displayComponent);
        return contextMenu;
        
    }
    
    /**
     * {@inheritDoc}
     * <p>Intercept mouse events on the tabs to override right-clicks and provide context menus.</p>
     */
    protected void processMouseEvent(MouseEvent evt) {
        boolean eventProcessed = false;
        // Is it a right-click?
        if (evt.getID() == MouseEvent.MOUSE_PRESSED && evt.getButton() == MouseEvent.BUTTON3) {
            // Where was the click?
            Object component = this.getDisplayComponentAtLocation(evt.getPoint());
            // Only respond to individual table providers, not the overview tab.
            this.getContextMenu(component).show(this, evt.getX(), evt.getY());
            eventProcessed = true;
        }
        // Pass it on up if we're not interested.
        if (!eventProcessed) super.processMouseEvent(evt);
    }
    
    /**
     * Works out what's at a given point.
     * @param location the point to look at.
     * @return the display component at that point, or null if nothing there.
     */
    private Object getDisplayComponentAtLocation(Point location) {
        return null;
    }
    
    /**
     * Recalculates the way to display what we see.
     */
    public void recalculateView() {
        // Nothing, yet.
    }
    
    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize() {
        return new Dimension(200,200);
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
