/*
 * Schema.java
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>A {@link Schema} provides one or more {@link Table} objects with
 * unique names for the user to use. It could be a relational database, or an XML
 * document, or any other source of potentially tabular information.</p>
 * <p>The generic implementation provided should suffice for most tasks involved with
 * keeping track of the {@link Table}s a {@link Schema} provides.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.9, 12th May 2006
 * @since 0.1
 */
public interface Schema extends Comparable, DataLink {
    /**
     * Returns the name of this {@link Schema}.
     *
     * @return the name of this provider.
     */
    public String getName();
    
    /**
     * Sets the name of this {@link Schema}.
     *
     * @param name the new name of this provider.
     */
    public void setName(String name);
    
    /**
     * Synchronise this {@link Schema} with the data source that is
     * providing its tables. Synchronisation means checking the list of {@link Table}s
     * available and drop/add any that have changed, then check each {@link Column}.
     * and {@link Key} and {@link Relation} and update those too.
     * Any {@link Key} or {@link Relation} that was created by the user and is still valid,
     * ie. the underlying columns still exist, will not be affected by this operation.
     *
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
     */
    public void addTable(Table table) throws AlreadyExistsException, AssociationException;
    
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
     */
    public Collection getUniqueValues(Column column) throws AssociationException, SQLException;
    
    /**
     * Counts the unique values in a given column, which may include null.
     * @param column the {@link Column} to get unique values for.
     * @return a count of the unique values in a given column.
     * @throws AssociationException if the column doesn't belong to us.
     * @throws SQLException if there was any problem counting the values.
     */
    public int countUniqueValues(Column column) throws AssociationException, SQLException;
    
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
    
    public Collection getInternalRelations();
    
    public void setKeyGuessing(boolean keyguessing);
    
    public boolean getKeyGuessing();
    
    public Schema replicate(String newName);
    
    public void replicateContents(Schema targetSchema);
    
    /**
     * The generic implementation should suffice as the ground for most
     * complex implementations. It keeps track of {@link Table}s it has already seen, and
     * performs simple lookups for them.
     */
    public class GenericSchema implements Schema {
        /**
         * Internal reference to the name of this provider.
         */
        private String name;
        
        /**
         * Internal reference to the set of {@link Table}s in this provider.
         */
        protected final Map tables = new TreeMap();
        
        private boolean keyguessing;
        
        /**
         * The constructor creates a provider with the given name.
         * @param name the name for this new provider.
         */
        public GenericSchema(String name) {
            // Remember the values.
            this.name = name;
        }
        
        public void replicateContents(Schema targetSchema) {
            try {
                targetSchema.getTables().clear();
                Set relations = new HashSet();
                // copy all tables
                for (Iterator i = this.tables.values().iterator(); i.hasNext(); ) {
                    Table table = (Table)i.next();
                    Table newTable = new GenericTable(table.getName(), targetSchema);
                    // copy all columns on the tables
                    for (Iterator j = table.getColumns().iterator(); j.hasNext(); ) {
                        Column col = (Column)j.next();
                        Column newCol = new GenericColumn(col.getName(), newTable);
                    }
                    // copy all keys on the tables
                    PrimaryKey pk = table.getPrimaryKey();
                    if (pk!=null) {
                        List columns = new ArrayList();
                        for (Iterator k = pk.getColumnNames().iterator(); k.hasNext(); ) {
                            columns.add(newTable.getColumnByName((String)k.next()));
                        }
                        PrimaryKey newPK = new GenericPrimaryKey(columns);
                        newPK.setStatus(pk.getStatus());
                        newTable.setPrimaryKey(newPK);
                    }
                    for (Iterator j = table.getForeignKeys().iterator(); j.hasNext(); ) {
                        ForeignKey fk = (ForeignKey)j.next();
                        List columns = new ArrayList();
                        for (Iterator k = fk.getColumnNames().iterator(); k.hasNext(); ) {
                            columns.add(newTable.getColumnByName((String)k.next()));
                        }
                        ForeignKey newFK = new GenericForeignKey(columns);
                        newFK.setStatus(fk.getStatus());
                        newTable.addForeignKey(newFK);
                    }
                    // remember the relations.
                    relations.addAll(table.getRelations());
                }
                // copy all relations.
                for (Iterator i = relations.iterator(); i.hasNext(); ) {
                    Relation r = (Relation)i.next();
                    PrimaryKey pk = r.getPrimaryKey();
                    ForeignKey fk = r.getForeignKey();
                    Cardinality card = r.getFKCardinality();
                    PrimaryKey newPK = targetSchema.getTableByName(pk.getTable().getName()).getPrimaryKey();
                    ForeignKey newFK = null;
                    for (Iterator j = targetSchema.getTableByName(fk.getTable().getName()).getForeignKeys().iterator(); j.hasNext() && newFK==null; ) {
                        ForeignKey candidate = (ForeignKey)j.next();
                        if (candidate.getColumnNames().equals(fk.getColumnNames())) newFK = candidate;
                    }
                    Relation newRel;
                    try {
                        newRel = new GenericRelation(newPK, newFK, card);
                        newRel.setStatus(r.getStatus());
                    } catch (Exception e) {
                        // Ignore. This can only happen if incorrect relations are copied across
                        // which have different-arity keys at each end.
                    }
                }
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
        
        public Schema replicate(String newName) {
            return new GenericSchema(newName);
        }
        
        public void setKeyGuessing(boolean keyguessing) {
            this.keyguessing = keyguessing;
        }
        
        public boolean getKeyGuessing() {
            return this.keyguessing;
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
            // Remember the values.
            this.name = name;
        }
        
        /**
         * {@inheritDoc}
         * <p>The generic provider has no data source, so it will always return false.</p>
         */
        public boolean canCohabit(DataLink partner) {
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
        public void addTable(Table table) throws AlreadyExistsException, AssociationException {
            // Sanity check.
            if (!table.getSchema().equals(this))
                throw new AssociationException(BuilderBundle.getString("tableSchemaMismatch"));
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
        public Collection getUniqueValues(Column column) throws AssociationException, SQLException {
            // Sanity check.
            if (!column.getTable().getSchema().equals(this))
                throw new AssociationException(BuilderBundle.getString("columnSchemaMismatch"));
            // Do it.
            return Collections.EMPTY_SET;
        }
        
        /**
         * {@inheritDoc}
         * <p>This being the generic implementation, it always returns 0.</p>
         */
        public int countUniqueValues(Column column) throws AssociationException, SQLException {
            // Sanity check.
            if (!column.getTable().getSchema().equals(this))
                throw new AssociationException(BuilderBundle.getString("columnSchemaMismatch"));
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
                if (relation.getPrimaryKey().getTable().getSchema().equals(this)) keys.add(relation.getPrimaryKey());
                else keys.add(relation.getForeignKey());
            }
            return keys;
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getInternalRelations() {
            List relations = new ArrayList();
            for (Iterator i = this.getTables().iterator(); i.hasNext(); ) {
                Table table = (Table)i.next();
                if (table.getPrimaryKey()==null) continue;
                for (Iterator j = table.getPrimaryKey().getRelations().iterator(); j.hasNext(); ) {
                    Relation relation = (Relation)j.next();
                    if (relation.getForeignKey().getTable().getSchema().equals(this)) relations.add(relation);
                }
            }
            return relations;
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
                        if (!(relation.getPrimaryKey().getTable().getSchema().equals(this) &&
                                relation.getForeignKey().getTable().getSchema().equals(this))) {
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
            Schema t = (Schema)o;
            return this.toString().compareTo(t.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Schema)) return false;
            Schema t = (Schema)o;
            return t.toString().equals(this.toString());
        }
    }
}
