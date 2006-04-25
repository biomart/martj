/*
 * TableProvider.java
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>A {@link TableProvider} provides one or more {@link Table} objects with
 * unique names for the user to use. It could be a relational database, or an XML
 * document, or any other source of potentially tabular information.</p>
 * <p>The generic implementation provided should suffice for most tasks involved with
 * keeping track of the {@link Table}s a {@link TableProvider} provides.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 18th April 2006
 * @since 0.1
 */
public interface TableProvider extends Comparable, DataLink {
    /**
     * Returns the name of this {@link TableProvider}.
     * @return the name of this provider.
     */
    public String getName();

    /**
     * Sets the name of this {@link TableProvider}.
     * @param name the new name of this provider.
     * @throws NullPointerException if the name is null.
     */
    public void setName(String name);
    
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
     * Adds a {@link Table} to this provider. The table must not be null, and
     * must not already exist (ie. with the same name).
     * @param table the {@link Table} to add.
     * @throws AlreadyExistsException if another one with the same name already exists.
     * @throws AssociationException if the table doesn'table belong to this provider.
     * @throws NullPointerException if the table is null.
     */
    public void addTable(Table table) throws AlreadyExistsException, AssociationException, NullPointerException;
    
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
     * Returns a set of unique values in a given column, which may include null. The
     * set returned will never be null itself.
     * @param column the {@link Column} to get unique values for.
     * @return a set of unique values in a given column.
     * @throws AssociationException if the column doesn't belong to us.
     * @throws SQLException if there was any problem loading the values.
     * @throws NullPointerException if the column was null.
     */
    public Collection getUniqueValues(Column column) throws AssociationException, NullPointerException, SQLException;
    
    /**
     * Counts the unique values in a given column, which may include null.
     * @param column the {@link Column} to get unique values for.
     * @return a count of the unique values in a given column.
     * @throws AssociationException if the column doesn't belong to us.
     * @throws SQLException if there was any problem counting the values.
     * @throws NullPointerException if the column was null.
     */
    public int countUniqueValues(Column column) throws AssociationException, NullPointerException, SQLException;
    
    /**
     * Returns a collection of all the keys from all the tables in this provider which
     * have relations referring to tables in other table providers.
     * @return a set of keys with relations linking to tables in other table providers.
     */
    public Collection getExternalKeys();
    
    /**
     * Returns a collection of all the relations from all the tables in this provider which
     * refer to tables in other table providers.
     * @return a set of relations linking to tables in other table providers.
     */
    public Collection getExternalRelations();
    
    /**
     * The generic implementation should suffice as the ground for most
     * complex implementations. It keeps track of {@link Table}s it has already seen, and
     * performs simple lookups for them.
     */
    public class GenericTableProvider implements TableProvider {
        /**
         * Internal reference to the name of this provider.
         */
        private String name;
        
        /**
         * Internal reference to the set of {@link Table}s in this provider.
         */
        protected final Map tables = new TreeMap();
        
        /**
         * The constructor creates a provider with the given name.
         * @param name the name for this new provider.
         * @throws NullPointerException if the name is null.
         */
        public GenericTableProvider(String name) throws NullPointerException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            // Remember the values.
            this.name = name;
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
        public void setName(String name) {
            // Sanity check.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            // Remember the values.
            this.name = name;
        }
        
        /**
         * {@inheritDoc}
         * <p>The generic provider has no data source, so it will always return false.</p>
         */
        public boolean canCohabit(DataLink partner) throws NullPointerException {
            return false;
        }
        
        /**
         * {@inheritDoc}
         * <p>As this is a generic implementation, with nothing to connect to, it always returns true.</p>
         */
        public boolean test() throws Exception {
            return true;
        }
        
        /**
         * {@inheritDoc}
         * <p>As this is a generic implementation, nothing actually happens here.</p>
         */
        public void synchronise() throws SQLException, BuilderException {}
        
        /**
         * {@inheritDoc}
         */
        public void addTable(Table table) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (table == null)
                throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
            if (!table.getTableProvider().equals(this))
                throw new AssociationException(BuilderBundle.getString("tableTblprovMismatch"));
            if (this.tables.containsKey(table.getName()))
                throw new AlreadyExistsException(BuilderBundle.getString("tableExists"), table.getName());
            // Do it.
            this.tables.put(table.getName(), table);
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getTables() {
            return this.tables.values();
        }
        
        /**
         * {@inheritDoc}
         */
        public Table getTableByName(String name) {
            // Do we know about it?
            if (this.tables.containsKey(name)) return (Table)this.tables.get(name);
            // Default case.
            return null;
        }
        
        /**
         * {@inheritDoc}
         * <p>This being the generic implementation, it always returns an empty set.</p>
         */
        public Collection getUniqueValues(Column column) throws AssociationException, NullPointerException, SQLException {
            // Sanity check.
            if (column == null)
                throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
            if (!column.getTable().getTableProvider().equals(this))
                throw new AssociationException(BuilderBundle.getString("columnTblprovMismatch"));
            // Do it.
            return Collections.EMPTY_SET;
        }
        
        /**
         * {@inheritDoc}
         * <p>This being the generic implementation, it always returns 0.</p>
         */
        public int countUniqueValues(Column column) throws AssociationException, NullPointerException, SQLException {
            // Sanity check.
            if (column == null)
                throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
            if (!column.getTable().getTableProvider().equals(this))
                throw new AssociationException(BuilderBundle.getString("columnTblprovMismatch"));
            // Do it.
            return 0;
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getExternalKeys() {
            List keys = new ArrayList();
            Collection relations = this.getExternalRelations();
            for (Iterator i = relations.iterator(); i.hasNext(); ) {
                Relation relation = (Relation)i.next();
                if (relation.getPrimaryKey().getTable().getTableProvider().equals(this)) keys.add(relation.getPrimaryKey());
                else keys.add(relation.getForeignKey());
            }
            return keys;
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getExternalRelations() {
            List relations = new ArrayList();
            for (Iterator i = this.getTables().iterator(); i.hasNext(); ) {
                Table table = (Table)i.next();
                for (Iterator j = table.getKeys().iterator(); j.hasNext(); ) {
                    Key key = (Key)j.next();
                    for (Iterator l = key.getRelations().iterator(); l.hasNext(); ) {
                        Relation relation = (Relation)l.next();
                        if (!(relation.getPrimaryKey().getTable().getTableProvider().equals(this) &&
                                relation.getForeignKey().getTable().getTableProvider().equals(this))) {
                            relations.add(relation);
                        }
                    }
                }
            }
            return relations;
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return this.getName();
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
            TableProvider t = (TableProvider)o;
            return this.toString().compareTo(t.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof TableProvider)) return false;
            TableProvider t = (TableProvider)o;
            return t.toString().equals(this.toString());
        }
    }
}
