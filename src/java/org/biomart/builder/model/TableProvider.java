/*
 * TableProvider.java
 *
 * Created on 23 March 2006, 15:07
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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Table.GenericTable;

/**
 * <p>A {@link TableProvider} provides one or more {@link Table} objects with
 * unique names for the user to use. It could be a relational database, or an XML
 * document, or any other source of potentially tabular information.</p>
 *
 * <p>The generic implementation provided should suffice for most tasks involved with
 * keeping track of the {@link Table}s a {@link TableProvider} provides.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 27th March 2006
 * @since 0.1
 */
public interface TableProvider extends Comparable {
    /**
     * This constant specifies the dummy {@link TableProvider} which represents the
     * target {@link DataSet}'s tables. It does not exist, and does not connect to anything.
     * It is here purely so that we can re-use the {@link Table} framework to represent
     * the transformed tables in the mart as well.
     */
    public static final TableProvider DATASET = new GenericTableProvider("DATASET");
    
    /**
     * Returns the name of this {@link TableProvider}.
     * @return the name of this provider.
     */
    public String getName();
    
    /**
     * Tests the connection between this {@link TableProvider} and the data source that is
     * providing its tables. It will return without throwing any exceptions if the connection
     * is OK. If there is a problem with the connection, a SQLException will be thrown
     * detailing the problems.
     * @throws SQLException if there is a problem connecting to the data source..
     */
    public void testConnection() throws SQLException;
    
    /**
     * Synchronise this {@link TableProvider} with the data source that is
     * providing its tables. Synchronisation means checking the list of {@link Table}s
     * available and drop/add any that have changed, then check each {@link Column}.
     * and {@link Key} and {@link Relation} and update those too.
     * Any {@link Key} or {@link Relation} that was created by the user and is still valid,
     * ie. the underlying columns still exist, will not be affected by this operation.
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchronise() throws SQLException, BuilderException;
    
    /**
     * Adds a {@link Table} to this provider. If the {@link Table} already exists
     * an exception will be thrown.
     * @param t the {@link Table} to add.
     * @throws AlreadyExistsException if a {@link Table} with the same name already
     * exists in this {@link TableProvider}.
     * @throws NullPointerException if the {@link Table} argument is null.
     * @throws AssociationException if the {@link TableProvider} provided by the
     * {@link Table} object is not this same provider.
     */
    public void addTable(Table t) throws AlreadyExistsException, AssociationException, NullPointerException;
    
    /**
     * Convenience method that creates and adds a {@link Table} to this provider.
     * If a {@link Table} with the same name already exists an exception will be thrown.
     * @param name the name of the {@link Table} to create and add.
     * @throws AlreadyExistsException if a {@link Table} with the same name already
     * exists in this {@link TableProvider}.
     * @throws NullPointerException if the name argument is null.
     */
    public void createTable(String name) throws AlreadyExistsException, NullPointerException;
    
    /**
     * Returns all the {@link Table}s this provider provides. The set returned may be
     * empty but it will never be null.
     * @return the set of all {@link Table}s in this provider.
     */
    public Collection getTables();
    
    /**
     * Returns the {@link Table}s from this provider with the given name. If there is
     * no such table, the method will return null.
     * @param name the name of the {@link Table} to retrieve.
     * @return the matching {@link Table}s from this provider.
     */
    public Table getTableByName(String name);
    
    /**
     * The generic implementation should suffice as the ground for most
     * complex implementations. It keeps track of {@link Table}s it has already seen, and
     * performs simple lookups for them.
     */
    public class GenericTableProvider implements TableProvider {
        /**
         * Internal reference to the name of this provider.
         */
        private final String name;
        
        /**
         * Internal reference to the set of {@link Table}s in this provider.
         */
        private final Map tables = new HashMap();
        
        /**
         * The constructor creates a provider with the given name.
         * @param name the name for this new provider.
         * @throws NullPointerException if the name is null.
         */
        public GenericTableProvider(String name) {
            // Sanity check.
            if (name==null)
                throw new NullPointerException("Table provider name cannot be null.");
            // Remember the values.
            this.name = name;
        }
        
        /**
         * Returns the name of this {@link TableProvider}.
         * @return the name of this provider.
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * <p>Tests the connection between this {@link TableProvider} and the data source that is
         * providing its tables. It will return without throwing any exceptions if the connection
         * is OK. If there is a problem with the connection, a SQLException will be thrown
         * detailing the problems.</p>
         *
         * <p>The generic provider has no data source, so this method will
         * always return successfully.</p>
         * @throws SQLException if there is a problem connecting to the data source..
         */
        public void testConnection() throws SQLException {}
        
        /**
         * <p>Synchronise this {@link TableProvider} with the data source that is
         * providing its tables. Synchronisation means checking the list of {@link Table}s
         * available and drop/add any that have changed, then check each {@link Column}.
         * and {@link Key} and {@link Relation} and update those too.
         * Any {@link Key} or {@link Relation} that was created by the user and is still valid,
         * ie. the underlying columns still exist, will not be affected by this operation.</p>
         *
         * <p>As this is a generic implementation, nothing actually happens here.</p>
         *
         * @throws SQLException if there was a problem connecting to the data source.
         * @throws BuilderException if there was any other kind of problem.
         */
        public void synchronise() throws SQLException, BuilderException {}
        
        /**
         * Adds a {@link Table} to this provider. If the {@link Table} already exists
         * an exception will be thrown.
         * @param t the {@link Table} to add.
         * @throws AlreadyExistsException if a {@link Table} with the same name already
         * exists in this {@link TableProvider}.
         * @throws NullPointerException if the {@link Table} argument is null.
         * @throws AssociationException if the {@link TableProvider} provided by the
         * {@link Table} object is not this same provider.
         */
        public void addTable(Table t) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (t==null)
                throw new NullPointerException("Table cannot be null");
            if (t.getTableProvider()!=this)
                throw new AssociationException("Table must be associated with this provider before being added to it.");
            // Do the work.
            String name = t.getName();
            if (this.tables.containsKey(name))
                throw new AlreadyExistsException("Table already exists in this provider", name);
            this.tables.put(name,t);
        }
        
        /**
         * Convenience method that creates and adds a {@link Table} to this provider.
         * If a {@link Table} with the same name already exists an exception will be thrown.
         * @param name the name of the {@link Table} to create and add.
         * @throws AlreadyExistsException if a {@link Table} with the same name already
         * exists in this {@link TableProvider}.
         * @throws NullPointerException if the name argument is null.
         */
        public void createTable(String name) throws AlreadyExistsException, NullPointerException {
            Table t = new GenericTable(name, this);
            try {
                this.addTable(t);
            } catch (AssociationException e) {
                throw new AssertionError("TableProvider does not equal itself.");
            }
        }
        
        /**
         * Returns all the {@link Table}s this provider provides. The set returned may be
         * empty but it will never be null.
         * @return the set of all {@link Table}s in this provider.
         */
        public Collection getTables() {
            return this.tables.values();
        }
        
        /**
         * Returns the {@link Table}s from this provider with the given name. If there is
         * no such table, the method will return null.
         * @param name the name of the {@link Table} to retrieve.
         * @return the matching {@link Table}s from this provider.
         */
        public Table getTableByName(String name) {
            // Do we know about it?
            if (this.tables.containsKey(name)) return (Table)this.tables.get(name);
            // Default case.
            return null;
        }
        
        /**
         * Displays the name of this {@link TableProvider} object.
         * @return the name of this {@link TableProvider} object.
         */
        public String toString() {
            return this.getName();
        }
        
        /**
         * Displays the hashcode of this object.
         * @return the hashcode of this object.
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * Sorts by comparing the toString() output.
         * @param o the object to compare to.
         * @return -1 if we are smaller, +1 if we are larger, 0 if we are equal.
         * @throws ClassCastException if the object o is not a {@link TableProvider}.
         */
        public int compareTo(Object o) throws ClassCastException {
            TableProvider t = (TableProvider)o;
            return this.toString().compareTo(t.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link TableProvider}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof TableProvider)) return false;
            TableProvider t = (TableProvider)o;
            return t.toString().equals(this.toString());
        }
    }
}
