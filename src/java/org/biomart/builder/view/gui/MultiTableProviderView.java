/*
 * MultiTableProviderView.java
 *
 * Created on 19 April 2006, 09:28
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
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This class deals with drawing an overview of all the table providers.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.0, 19th April 2006
 * @since 0.1
 */
public class MultiTableProviderView extends JComponent implements MultiView {
    /**
     * Static reference to the background colour to use for components.
     */
    public static final Color BACKGROUND_COLOUR = Color.YELLOW;
    
    /**
     * Internal reference to the parent view.
     */
    private Collection tableProviders;
    
    /**
     * Internal reference to our event listener.
     */
    private Listener listener;
    
    /**
     * The constructor rememembers the Collection that this
     * displays.
     * @param tableProviders the providers collection to display.
     */
    public MultiTableProviderView(Collection tableProviders) {
        super();
        this.tableProviders = tableProviders;
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.setBackground(MultiTableProviderView.BACKGROUND_COLOUR);
    }
    
    /**
     * Obtains the table providers this view displays.
     * @return the table providers this view displays.
     */
    public Collection getTableProviders() {
        return this.tableProviders;
    }
    
    /**
     * Resyncs the table providers with the contents of the set.
     * 
     * @param newTableProviders the table providers to view.
     */
    public void resyncTableProviders(Collection newTableProviders) {
        this.tableProviders = newTableProviders;
    }
    
    /**
     * Construct a context menu for a given multi table provider view.
     * @param displayComponent the display component we wish to customise this menu to. It may
     * be null, so watch out for this.
     * @return the popup menu.
     */
    private JPopupMenu getContextMenu(Object displayComponent) {
        JPopupMenu contextMenu = new JPopupMenu();
        final JMenuItem redraw = new JMenuItem(BuilderBundle.getString("redrawTitle"));
        redraw.setMnemonic(BuilderBundle.getString("redrawMnemonic").charAt(0));
        redraw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                recalculateView();
            }
        });
        contextMenu.add(redraw);
        contextMenu.addSeparator();
        final JMenuItem sync = new JMenuItem(BuilderBundle.getString("synchroniseAllTitle"));
        sync.setMnemonic(BuilderBundle.getString("synchroniseAllMnemonic").charAt(0));
        sync.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getListener().requestSynchroniseAll();
            }
        });
        contextMenu.add(sync);
        // Extend.
        this.getListener().requestCustomiseContextMenu(contextMenu, displayComponent);
        // Return.
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
     * {@inheritDoc}
     */
    public void setListener(Listener listener) throws NullPointerException {
        if (listener==null) 
            throw new NullPointerException(BuilderBundle.getString("listenerIsNull"));
        this.listener = listener;
    }
    
    /**
     * {@inheritDoc}
     */
    public Listener getListener() {
        return this.listener;
    }
    
    /**
     * {@inheritDoc}
     */
    public JComponent asJComponent() {
        return this;
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
     * {@inheritDoc}
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
        // Do painting of this table provider overview.
        // Clean up.
        g2d.dispose();
    }
}
