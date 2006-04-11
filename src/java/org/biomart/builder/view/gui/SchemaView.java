/*
 * SchemaView.java
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

import javax.swing.JLabel;
import org.biomart.builder.model.Schema;

/**
 * Displays the contents of a {@link Schema} in graphical form.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 11th April 2006
 * @since 0.1
 */
public class SchemaView extends MultiTableProviderView implements TableProviderListener {
    /**
     * Internal reference to the provider we are viewing.
     */
    private final Schema schema;
    
    /**
     * Creates a new instance of TableProviderView over a given schema.
     * @param schema the schema to display.
     */
    public SchemaView(Schema schema) {
        super(schema.getTableProviders());
        this.setTableProviderListener(this);
        this.schema = schema;
    }
}
