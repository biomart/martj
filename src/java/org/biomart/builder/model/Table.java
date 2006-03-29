/*
 * Table.java
 *
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;

/**
 * <p>The {@link Table} interface provides the basic idea of what constitutes a database
 * table or an XML document entity. It has an optional {@link PrimaryKey}, zero or more
 * {@link ForeignKey}s, and one or more {@link Column}s.</p>
 *
 * <p>The {@link GenericTable} class is provided as a template from which to build up
 * more complex implementations. It is able to keep track of {@link Key}s and {@link Column}s
 * but it does not provide any methods that process or analyse these.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 28th March 2006
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
     * @param pk the new {@link PrimaryKey} of this {@link Table}.
     * @throws AssociationException if the {@link Table} parameter of the {@link ForeignKey}
     * does not match.
     */
    public void setPrimaryKey(PrimaryKey pk) throws AssociationException;
    
    /**
     * Returns a set of the {@link ForeignKey}s of this table. It may
     * be empty, indicating that the {@link Table} has no {@link ForeignKey}s.
     * It will never return null.
     * @return the set of {@link ForeignKey}s for this {@link Table}.
     */
    public Collection getForeignKeys();
    
    /**
     * Adds a {@link ForeignKey} to this table. It may
     * not be null. The {@link ForeignKey} must refer to this {@link Table} else
     * an {@link AssociationException} will be thrown. If it already exists, nothing
     * will happen and it will be quietly ignored.
     * @param fk the new {@link ForeignKey} to add to this {@link Table}.
     * @throws AssociationException if the {@link Table} parameter of the {@link ForeignKey}
     * does not match.
     * @throws NullPointerException if the {@link ForeignKey} object is null.
     */
    public void addForeignKey(ForeignKey fk) throws NullPointerException, AssociationException;
    
    /**
     * Removes a {@link ForeignKey} from this table. It may
     * not be null. If it doesn't exist, nothing
     * will happen and it will be quietly ignored.
     * @param fk the new {@link ForeignKey} to add to this {@link Table}.
     * @throws NullPointerException if the {@link ForeignKey} object is null.
     */
    public void removeForeignKey(ForeignKey fk) throws NullPointerException;
    
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
     * @param c the {@link Column} to add.
     * @throws AlreadyExistsException if the {@link Column} name has already been used on
     * this {@link Table}.
     * @throws AssociationException if the {@link Table} parameter of the {@link Column}
     * does not match.
     * @throws NullPointerException if the {@link Column} object is null.
     */
    public void addColumn(Column c) throws AlreadyExistsException, AssociationException, NullPointerException;
    
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
     * @param c the {@link Column} to remove.
     * @throws NullPointerException if the {@link Column} object is null.
     */
    public void removeColumn(Column c) throws NullPointerException;
    
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
        private final TableProvider prov;
        
        /**
         * Internal reference to the {@link PrimaryKey} of this {@link Table}.
         */
        private PrimaryKey pk;
        
        /**
         * Internal reference to the {@link ForeignKey}s of this {@link Table}.
         */
        private final Set fks = new HashSet();
        
        /**
         * Internal reference to the {@link Column}s of this {@link Table}.
         */
        private final Map cols = new HashMap();
        
        /**
         * The constructor sets up an empty {@link Table} representation with the given name
         * that lives within the given {@link TableProvider}.
         * @param name the table name.
         * @param prov the {@link TableProvider} this {@link Table} is associated with.
         * @throws NullPointerException if the name or provider are null.
         * @throws AlreadyExistsException if the provider, for whatever reason, refuses to
         * allow this {@link Table} to be added to it using {@link TableProvider#addTable(Table) addTable()}.
         */
        public GenericTable(String name, TableProvider prov) throws AlreadyExistsException, NullPointerException {
            // Sanity checks.
            if (name==null)
                throw new NullPointerException("Table name cannot be null.");
            if (prov==null)
                throw new NullPointerException("Table provider cannot be null.");
            // Remember the values.
            this.name = name;
            this.prov = prov;
            // Attempt to add to the provider - throws AssociationException and AlreadyExistsException..
            try {
                prov.addTable(this);
            } catch (AssociationException e) {
                throw new AssertionError("Table provider does not equal itself.");
            }
        }
        
        /**
         * Returns the name of this table.
         * @return the name of this {@link Table}.
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * Returns the {@link TableProvider} for this table.
         * @return the {@link TableProvider} for this {@link Table}.
         */
        public TableProvider getTableProvider() {
            return this.prov;
        }
        
        /**
         * Returns a reference to the {@link PrimaryKey} of this table. It may
         * be null, indicating that the {@link Table} has no {@link PrimaryKey}.
         * @return the {@link PrimaryKey} of this {@link Table}.
         */
        public PrimaryKey getPrimaryKey() {
            return this.pk;
        }
        
        /**
         * Sets the {@link PrimaryKey} of this table. It may
         * be null, indicating that the {@link Table} has no {@link PrimaryKey}.
         * @param pk the new {@link PrimaryKey} of this {@link Table}.
         * @throws AssociationException if the {@link Table} parameter of the {@link ForeignKey}
         * does not match.
         */
        public void setPrimaryKey(PrimaryKey pk) throws AssociationException {
            // Sanity check.
            if (pk!=null && !pk.getTable().equals(this))
                throw new AssociationException("Primary key must specify same table as the one it is assigned to.");
            // Ensure nobody points to the old primary key
            if (this.pk!=null) {
                for (Iterator i = this.pk.getRelations().iterator(); i.hasNext(); ) {
                    Relation r = (Relation)i.next();
                    r.destroy();
                }
            }
            // Update our primary key to the new one.
            this.pk = pk;
        }
        
        /**
         * Returns a set of the {@link ForeignKey}s of this table. It may
         * be empty, indicating that the {@link Table} has no {@link ForeignKey}s.
         * It will never return null.
         * @return the set of {@link ForeignKey}s for this {@link Table}.
         */
        public Collection getForeignKeys() {
            return this.fks;
        }
        
        /**
         * Adds a {@link ForeignKey} to this table. It may
         * not be null. The {@link ForeignKey} must refer to this {@link Table} else
         * an {@link AssociationException} will be thrown. If it already exists, nothing
         * will happen and it will be quietly ignored.
         * @param fk the new {@link ForeignKey} to add to this {@link Table}.
         * @throws AssociationException if the {@link Table} parameter of the {@link ForeignKey}
         * does not match.
         * @throws NullPointerException if the {@link ForeignKey} object is null.
         */
        public void addForeignKey(ForeignKey fk) throws NullPointerException, AssociationException {
            // Sanity checks.
            if (fk==null)
                throw new NullPointerException("Key to be added cannot be null.");
            // Do it.
            this.fks.add(fk);
        }
        
        /**
         * Removes a {@link ForeignKey} from this table. It may
         * not be null. If it doesn't exist, nothing
         * will happen and it will be quietly ignored.
         * @param fk the new {@link ForeignKey} to add to this {@link Table}.
         * @throws NullPointerException if the {@link ForeignKey} object is null.
         */
        public void removeForeignKey(ForeignKey fk) throws NullPointerException {
            // Sanity checks.
            if (fk==null)
                throw new NullPointerException("Key to be removed cannot be null.");
            // Do it.
            this.fks.remove(fk);
        }
        
        /**
         * Returns a set of the {@link Column}s of this table. It may
         * be empty, indicating that the {@link Table} has no {@link Column}s,
         * however this is highly unlikely! It will never return null.
         * @return the set of {@link Column}s for this {@link Table}.
         */
        public Collection getColumns() {
            return this.cols.values();
        }
        
        /**
         * Attempts to locate a {@link Column} in this {@link Table} by name. If
         * it finds it, it returns it. If it doesn't, it returns null.
         * @param name the name of the {@link Column} to look up.
         * @return the corresponding {@link Column}, or null if it couldn't be found.
         */
        public Column getColumnByName(String name) {
            // Do we know this column?
            if (this.cols.containsKey(name)) return (Column)this.cols.get(name);
            // Default case.
            return null;
        }
        
        /**
         * Attemps to add a {@link Column} to this table. The {@link Column} will already
         * have had it's {@link Table} parameter set to match, otherwise an
         * {@link IllegalArgumentException} will be thrown. That exception will also get thrown
         * if the {@link Column} has the same name as an existing one on this table.
         * @param c the {@link Column} to add.
         * @throws AlreadyExistsException if the {@link Column} name has already been used on
         * this {@link Table}.
         * @throws AssociationException if the {@link Table} parameter of the {@link Column}
         * does not match.
         * @throws NullPointerException if the {@link Column} object is null.
         */
        public void addColumn(Column c) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (c==null)
                throw new NullPointerException("Column cannot be null");
            if (c.getTable()!=this)
                throw new AssociationException("Column must be associated with this table before being added to it.");
            // Do the work.
            String name = c.getName();
            if (this.cols.containsKey(name))
                throw new AlreadyExistsException("Column already exists in this table", name);
            this.cols.put(name,c);
        }
        
        /**
         * Convenience method that creates and adds a {@link Column} to this {@link Table}.
         * If a {@link Column} with the same name already exists an exception will be thrown.
         * @param name the name of the {@link Column} to create and add.
         * @throws AlreadyExistsException if a {@link Column} with the same name already
         * exists in this {@link Table}.
         * @throws NullPointerException if the name argument is null.
         */
        public void createColumn(String name) throws AlreadyExistsException, NullPointerException {
            new GenericColumn(name, this);
            // By creating it we've already added it to ourselves! (Based on GenericColumn behaviour)
        }
        
        /**
         * Attemps to remove a {@link Column} from this table. If the {@link Column} does not exist on
         * this table the operation will be quietly ignored. Any {@link Key} involving that {@link Column}
         * will also be dropped along with all associated {@link Relation}s.
         * @param c the {@link Column} to remove.
         * @throws NullPointerException if the {@link Column} object is null.
         */
        public void removeColumn(Column c) throws NullPointerException {
            // Do we know this column?
            if (!this.cols.containsKey(c.getName())) return;
            // Remove primary key if if involves this column
            if (this.pk.getColumns().contains(c)) this.pk.destroy();
            // Remove all foreign keys involving this column
            for (Iterator i = this.fks.iterator(); i.hasNext(); ) {
                Key fk = (Key)i.next();
                if (fk.getColumns().contains(c)) fk.destroy();
            }
            // Remove the column itself
            this.cols.remove(c.getName());
        }
        
        /**
         * Displays the name of this {@link Table} object. The name is
         * in the form <tableProvider.toString()>:<table.getName()>
         * @return the name of this {@link Table} object.
         */
        public String toString() {
            return this.prov.toString()+":"+this.getName();
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
         * @throws ClassCastException if the object o is not a {@link Table}.
         */
        public int compareTo(Object o) throws ClassCastException {
            Table t = (Table)o;
            return this.toString().compareTo(t.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link Table}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof Table)) return false;
            Table t = (Table)o;
            return t.toString().equals(this.toString());
        }
    }
}
