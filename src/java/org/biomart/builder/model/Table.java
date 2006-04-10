/*
 * Table.java
 * Created on 23 March 2006, 14:34
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>The {@link Table} interface provides the basic idea of what constitutes a database
 * table or an XML document entity. It has an optional {@link PrimaryKey}, zero or more
 * {@link ForeignKey}s, and one or more {@link Column}s.</p>
 * <p>The {@link GenericTable} class is provided as a template from which to build up
 * more complex implementations. It is able to keep track of {@link Key}s and {@link Column}s
 * but it does not provide any methods that process or analyse these.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.6, 6th April 2006
 * @since 0.1
 */
public interface Table extends Comparable {
    /**
     * Returns the name of this table.
     * @return the name of this {@link Table}.
     */
    public String getName();
    
    /**
     * Returns the {@link TableProvider} for this table.
     * @return the {@link TableProvider} for this {@link Table}.
     */
    public TableProvider getTableProvider();
    
    /**
     * Returns a reference to the {@link PrimaryKey} of this table. It may
     * be null, indicating that the {@link Table} has no {@link PrimaryKey}.
     * @return the {@link PrimaryKey} of this {@link Table}.
     */
    public PrimaryKey getPrimaryKey();
    
    /**
     * Sets the {@link PrimaryKey} of this table. It may
     * be null, indicating that the {@link Table} has no {@link PrimaryKey}.
     * @param primaryKey the new {@link PrimaryKey} of this {@link Table}.
     * @throws AssociationException if the {@link Table} parameter of the {@link ForeignKey}
     * does not match.
     */
    public void setPrimaryKey(PrimaryKey primaryKey) throws AssociationException;
    
    /**
     * Returns a set of the {@link ForeignKey}s of this table. It may
     * be empty, indicating that the {@link Table} has no {@link ForeignKey}s.
     * It will never return null.
     * @return the set of {@link ForeignKey}s for this {@link Table}.
     */
    public Collection getForeignKeys();
    
    /**
     * Returns the {@link ForeignKey} on this {@link Table} with the given name. It may return
     * null if not found.
     * @param name the name to look for.
     * @return the named {@link ForeignKey} on this {@link Table} if found, otherwise null.
     * @throws NullPointerException if the name given was null.
     */
    public ForeignKey getForeignKeyByName(String name) throws NullPointerException;
    
    /**
     * Adds a {@link ForeignKey} to this table. It may
     * not be null. The {@link ForeignKey} must refer to this {@link Table} else
     * an {@link AssociationException} will be thrown. If it already exists, nothing
     * will happen and it will be quietly ignored.
     * @param foreignKey the new {@link ForeignKey} to add to this {@link Table}.
     * @throws AssociationException if the {@link Table} parameter of the {@link ForeignKey}
     * does not match.
     * @throws NullPointerException if the {@link ForeignKey} object is null.
     */
    public void addForeignKey(ForeignKey foreignKey) throws NullPointerException, AssociationException;
    
    /**
     * Removes a {@link ForeignKey} from this table. It may
     * not be null. If it doesn't exist, nothing
     * will happen and it will be quietly ignored.
     * @param foreignKey the new {@link ForeignKey} to add to this {@link Table}.
     * @throws NullPointerException if the {@link ForeignKey} object is null.
     */
    public void removeForeignKey(ForeignKey foreignKey) throws NullPointerException;
    
    /**
     * Returns a set of the {@link Key}s on all {@link Column}s in this table. It may
     * be empty, indicating that the {@link Table} has no {@link Key}s.
     * It will never return null.
     * @return the set of {@link Key}s for this {@link Table}.
     */
    public Collection getKeys();
    
    /**
     * Returns a set of the {@link Relation}s on all {@link Key}s in this table. It may
     * be empty, indicating that the {@link Table} has no {@link Relation}s.
     * It will never return null.
     * @return the set of {@link Relation}s for this {@link Table}.
     */
    public Collection getRelations();
    
    /**
     * Returns a set of the {@link Column}s of this table. It may
     * be empty, indicating that the {@link Table} has no {@link Column}s,
     * however this is highly unlikely! It will never return null.
     * @return the set of {@link Column}s for this {@link Table}.
     */
    public Collection getColumns();
    
    /**
     * Attempts to locate a {@link Column} in this {@link Table} by name. If
     * it finds it, it returns it. If it doesn't, it returns null.
     * @param name the name of the {@link Column} to look up.
     * @return the corresponding {@link Column}, or null if it couldn't be found.
     */
    public Column getColumnByName(String name);
    
    /**
     * Attemps to add a {@link Column} to this table. The {@link Column} will already
     * have had it's {@link Table} parameter set to match, otherwise an
     * {@link IllegalArgumentException} will be thrown. That exception will also get thrown
     * if the {@link Column} has the same name as an existing one on this table.
     * @param column the {@link Column} to add.
     * @throws AlreadyExistsException if the {@link Column} name has already been used on
     * this {@link Table}.
     * @throws AssociationException if the {@link Table} parameter of the {@link Column}
     * does not match.
     * @throws NullPointerException if the {@link Column} object is null.
     */
    public void addColumn(Column column) throws AlreadyExistsException, AssociationException, NullPointerException;
    
    /**
     * Convenience method that creates and adds a {@link Column} to this {@link Table}.
     * If a {@link Column} with the same name already exists an exception will be thrown.
     * @param name the name of the {@link Column} to create and add.
     * @throws AlreadyExistsException if a {@link Column} with the same name already
     * exists in this {@link Table}.
     * @throws NullPointerException if the name argument is null.
     */
    public void createColumn(String name) throws AlreadyExistsException, NullPointerException;
    
    /**
     * Attemps to remove a {@link Column} from this table. If the {@link Column} does not exist on
     * this table the operation will be quietly ignored. Any {@link Key} involving that {@link Column}
     * will also be dropped along with all associated {@link Relation}s.
     * @param column the {@link Column} to remove.
     * @throws NullPointerException if the {@link Column} object is null.
     */
    public void removeColumn(Column column) throws NullPointerException;
    
    /**
     * Attemps to remove all columns on a table so that it can safely be dropped.
     */
    public void destroy();
    
    /**
     * The generic implementation of {@link Table} provides basic methods for working with
     * database or XML document tables, including the ability to add a new {@link Column} and
     * check for conflicts with existing {@link Column}s.
     */
    public class GenericTable implements Table {
        /**
         * Internal reference to the name of this {@link Table}.
         */
        protected String name;
        
        /**
         * Internal reference to the provider of this {@link Table}.
         */
        private final TableProvider tableProvider;
        
        /**
         * Internal reference to the {@link PrimaryKey} of this {@link Table}.
         */
        private PrimaryKey primaryKey;
        
        /**
         * Internal reference to the {@link ForeignKey}s of this {@link Table}.
         */
        private final Map foreignKeys = new TreeMap();
        
        /**
         * Internal reference to the {@link Column}s of this {@link Table}.
         */
        private final Map columns = new TreeMap();
        
        /**
         * The constructor sets up an empty {@link Table} representation with the given name
         * that lives within the given {@link TableProvider}.
         * @param name the table name.
         * @param prov the {@link TableProvider} this {@link Table} is associated with.
         * @throws NullPointerException if the name or provider are null.
         * @throws AlreadyExistsException if a table with that name already exists in the provider.
         */
        public GenericTable(String name, TableProvider prov) throws NullPointerException, AlreadyExistsException {
            // Sanity checks.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            if (prov == null)
                throw new NullPointerException(BuilderBundle.getString("tblprovIsNull"));
            // Remember the values.
            this.name = name;
            this.tableProvider = prov;
            // Add it to our provider.
            try {
                prov.addTable(this);
            } catch (AssociationException e) {
                AssertionError ae = new AssertionError(BuilderBundle.getString("tblprovMismatch"));
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
        public TableProvider getTableProvider() {
            return this.tableProvider;
        }
        
        /**
         * {@inheritDoc}
         */
        public PrimaryKey getPrimaryKey() {
            return this.primaryKey;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setPrimaryKey(PrimaryKey primaryKey) throws AssociationException {
            // Sanity check.
            if (primaryKey != null && !primaryKey.getTable().equals(this))
                throw new AssociationException(BuilderBundle.getString("pkTableMismatch"));
            // Ensure nobody points to the old primary key
            if (this.primaryKey != null) {
                for (Iterator i = this.primaryKey.getRelations().iterator(); i.hasNext(); ) {
                    Relation r = (Relation)i.next();
                    r.destroy();
                }
            }
            // Update our primary key to the new one.
            this.primaryKey = primaryKey;
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getForeignKeys() {
            return this.foreignKeys.values();
        }
        
        /**
         * {@inheritDoc}
         */
        public ForeignKey getForeignKeyByName(String name) throws NullPointerException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            // Do we have it?
            if (!this.foreignKeys.containsKey(name)) return null;
            // Return it.
            return (ForeignKey)this.foreignKeys.get(name);
        }
        
        /**
         * {@inheritDoc}
         */
        public void addForeignKey(ForeignKey foreignKey) throws NullPointerException, AssociationException {
            // Sanity checks.
            if (foreignKey == null)
                throw new NullPointerException(BuilderBundle.getString("keyIsNull"));
            // Work out its name.
            String name = foreignKey.getName();
            // Quietly ignore if we've already got it.
            if (this.foreignKeys.containsKey(name)) return;
            // Do it.
            this.foreignKeys.put(name, foreignKey);
        }
        
        /**
         * {@inheritDoc}
         */
        public void removeForeignKey(ForeignKey foreignKey) throws NullPointerException {
            // Sanity checks.
            if (foreignKey == null)
                throw new NullPointerException(BuilderBundle.getString("keyIsNull"));
            // Do it.
            this.foreignKeys.remove(foreignKey);
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getKeys() {
            Set allKeys = new TreeSet();
            if (this.primaryKey!=null) allKeys.add(this.primaryKey);
            allKeys.addAll(this.foreignKeys.values());
            return allKeys;
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getRelations() {
            Set allRels = new TreeSet();
            for (Iterator i = this.getKeys().iterator(); i.hasNext(); ) {
                Key k = (Key)i.next();
                allRels.addAll(k.getRelations());
            }
            return allRels;
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getColumns() {
            return this.columns.values();
        }
        
        /**
         * {@inheritDoc}
         */
        public Column getColumnByName(String name) {
            // Do we know this column?
            if (this.columns.containsKey(name)) return (Column)this.columns.get(name);
            // Default case.
            return null;
        }
        
        /**
         * {@inheritDoc}
         */
        public void addColumn(Column column) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (column == null)
                throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
            if (column.getTable() != this)
                throw new AssociationException(BuilderBundle.getString("columnTableMismatch"));
            // Do the work.
            String name = column.getName();
            if (this.columns.containsKey(name))
                throw new AlreadyExistsException(BuilderBundle.getString("columnExists"), name);
            this.columns.put(name,column);
        }
        
        /**
         * {@inheritDoc}
         */
        public void createColumn(String name) throws AlreadyExistsException, NullPointerException {
            new GenericColumn(name, this);
            // By creating it we've already added it to ourselves! (Based on GenericColumn behaviour)
        }
        
        /**
         * {@inheritDoc}
         */
        public void removeColumn(Column column) throws NullPointerException {
            // Do we know this column?
            if (!this.columns.containsKey(column.getName())) return;
            // Remove all keys involving this column
            for (Iterator i = this.getKeys().iterator(); i.hasNext(); ) {
                Key k = (Key)i.next();
                if (k.getColumns().contains(column)) k.destroy();
            }
            // Remove the column itself
            this.columns.remove(column.getName());
        }
        
        /**
         * {@inheritDoc}
         */
        public void destroy() {
            Set allCols = new HashSet();
            allCols.addAll(this.columns.values());
            // Remove each column we have. This will recursively cause
            // keys etc. to be removed.
            for (Iterator i = allCols.iterator(); i.hasNext(); ) {
                Column c = (Column)i.next();
                try {
                    this.removeColumn(c);
                } catch (NullPointerException e) {
                    AssertionError ae = new AssertionError(BuilderBundle.getString("columnIsNull"));
                    ae.initCause(e);
                    throw ae;
                }
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return this.tableProvider.toString() + ":" + this.getName();
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
            Table t = (Table)o;
            return this.toString().compareTo(t.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Table))
                return false;
            Table t = (Table)o;
            return t.toString().equals(this.toString());
        }
    }
}
