/*
 * Column.java
 * Created on 23 March 2006, 14:10
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

package org.biomart.builder.model;

import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>A {@link Column} is a simple representation of a column in some {@link Table}.
 * It has a name, and knows which {@link Table} it belongs to, but apart from that knows
 * nothing much else.</p>
 * <p>A {@link GenericColumn} class is provided for ease of implementation. It provides
 * a simple storage/retrieval mechanism for the parent {@link Table} and name.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 4th April 2006
 * @since 0.1
 */
public interface Column extends Comparable {
    /**
     * Retrieve the name of this {@link Column}.
     * @return the name of this {@link Column}.
     */
    public String getName();
    
    /**
     * Retrieve the parent {@link Table} of this {@link Column}.
     * @return the parent {@link Table} of this {@link Column}.
     */
    public Table getTable();
    
    /**
     * A generic implementation which provides the basic functionality required for
     * a {@link Column} to function.
     */
    public class GenericColumn implements Column {
        /**
         * Internal reference to the parent {@link Table}.
         */
        protected Table table;
        
        /**
         * Internal reference to the name of the {@link Column}.
         */
        protected String name;
        
        /**
         * Dummy constructor, does nothing.
         */
        protected GenericColumn() {}
        
        /**
         * This constructor creates a {@link Column} and checks that neither the
         * name nor the parent {@link Table} are null.
         * @param name the name of the {@link Column} to create.
         * @param table the parent {@link Table}
         * @throws NullPointerException if either parameter is null.
         * @throws AlreadyExistsException if it was unable to add the {@link Column}
         * to the parent {@link Table} using {@link Table#addColumn(Column) addColumn()}.
         */
        public GenericColumn(String name, Table table) throws AlreadyExistsException, NullPointerException {
            // Sanity checks
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
            if (table == null)
                throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
            // Remember the values.
            this.name = name;
            this.table = table;
            // Add it to the table - throws AssociationException and AlreadyExistsException
            try {
                table.addColumn(this);
            } catch (AssociationException e) {
                AssertionError ae = new AssertionError(BuilderBundle.getString("tableMismatch"));
                ae.initCause(e);
                throw ae;
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * {@inheritDoc}
         */
        public Table getTable() {
            return this.table;
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return this.getTable().toString() + ":" + this.getName();
        }
        
        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * {@inheritDoc}
         */
        public int compareTo(Object o) throws ClassCastException {
            Column c = (Column)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Column)) return false;
            Column c = (Column)o;
            return c.toString().equals(this.toString());
        }
    }
}
