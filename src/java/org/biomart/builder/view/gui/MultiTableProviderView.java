/*
 * MultiTableProviderView.java
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

import java.awt.FlowLayout;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JPanel;
import org.biomart.builder.model.TableProvider;

/**
 * Displays the contents of multiple {@link TableProvider}s in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class MultiTableProviderView extends JPanel {
    /**
     * Internal reference to the map of table provider to table provider view.
     */
    private Map tblProvViews = new HashMap();
    
    /**
     * Internal reference to the listener for the providers we are viewing.
     */
    private TableProviderListener tblProvListener;
    
    /**
     * Creates a new multiple table provider view over the given set of
     * of table providers. They are displayed in FlowLayout form.
     * @param tblProvs the table providers to view.
     */
    public MultiTableProviderView(Collection tblProvs) {
        super(new FlowLayout());
        this.resyncTableProviders(tblProvs);
    }
    
    /**
     * Sets the listener to use.
     * @param tblProvListener the listener that will be told when the view is interacted with.
     */
    public void setTableProviderListener(TableProviderListener tblProvListener) {
        this.tblProvListener = tblProvListener;
        for (Iterator i = this.tblProvViews.values().iterator(); i.hasNext(); ) {
            TableProviderView tblProvView = (TableProviderView)i.next();
            tblProvView.setTableProviderListener(this.tblProvListener);
        }
    }
    
    /**
     * Resyncs the table providers with the contents of the set.
     * @param tblProvs the table providers to view.
     */
    public void resyncTableProviders(Collection tblProvs) {
        // Remove all the previous ones.
        this.tblProvViews.clear();
        // Add all the providers to ourselves.
        for (Iterator i = tblProvs.iterator(); i.hasNext(); ) {
            TableProvider tblProv = (TableProvider)i.next();
            TableProviderView tblProvView = new TableProviderView(tblProv);
            tblProvView.setTableProviderListener(this.tblProvListener);
            this.tblProvViews.put(tblProv, tblProvView);
            this.add(tblProvView);
        }
    }
    
    /**
     * Returns the panel containing a particular table provider.
     * @param tblProv the table provider to look up.
     * @return the TableProviderView containing that provider.
     */
    public TableProviderView getTableProviderView(TableProvider tblProv) {
        return (TableProviderView)this.tblProvViews.get(tblProv);
    }
}
