/*
 * NewTableProviderDialog.java
 *
 * Created on 25 April 2006, 16:09
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

import javax.swing.JDialog;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Construct a new table provider based on user input.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 25th April 2006
 * @since 0.1
 */
public class NewTableProviderDialog extends JDialog {
    /**
     * Our parent schema.
     */
    private TableProviderTabSet tableProviderTabSet;

    /**
     * The provider we created.
     */
    private TableProvider tableProvider;
    
    /** 
     * Creates a new instance of NewTableProviderDialog.
     */
    public NewTableProviderDialog(TableProviderTabSet tableProviderTabSet) {
        super(tableProviderTabSet.getWindowTabSet().getSchemaTabSet().getMartBuilder(),
                BuilderBundle.getString("newTblProvDialogTitle"),
                true);
        this.tableProviderTabSet = tableProviderTabSet;
        
        // create dialog
        
        // set size of window
        
        // centre window
        
        // intercept window closing to determine action
        
        // intercept buttons to determine action
    }    
    
    /**
     * Retrieve the provider we created.
     */
    public TableProvider getTableProvider() {
        return this.tableProvider;
    }
}
