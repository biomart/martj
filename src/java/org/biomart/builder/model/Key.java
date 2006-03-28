/*
 * Key.java
 *
 * Created on 23 March 2006, 15:03
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.biomart.builder.exceptions.AssociationException;

/**
 * <p>The {@link Key} interface is core to the way {@link Table}s get associated. They
 * are involved in {@link Relation}s which link {@link Table}s together in various ways, and
 * provide information about which {@link Column}s at each end correspond.</p>
 *
 * <p>The {@link GenericSimpleKey} and {@link GenericCompoundKey} implementations are
 * there to provide the basics for more complex {@link Key}s. They keep track of which
 * {@link Column}s are involved, and which {@link Relation}s refer to this {@link Key}, but
 * not much more. {@link GenericUnionKey} is much the same.</p>
 *
 * <p>Unless otherwise specified, all {@link Key}s are created with a default
 * {@link ComponentStatus} of INFERRED.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 28th March 2006
 * @since 0.1
 */
public interface Key extends Comparable {
    /**
     * Returns the {@link ComponentStatus} of this {@link Key}. The default value,
     * unless otherwise specified, is INFERRED.
     * @return the {@link ComponentStatus} of this {@link Key}.
     */
    public ComponentStatus getStatus();
    
    /**
     * Sets the {@link ComponentStatus} of this {@link Key}. The default value,
     * unless otherwise specified, is INFERRED.
     * @param s the new {@link ComponentStatus} of this {@link Key}.
     */
    public void setStatus(ComponentStatus s);
    
    /**
     * Returns all {@link Relation}s this {@link Key} is involved in. The set may be
     * empty but it will never be null.
     * @return the set of all {@link Relation}s this {@link Key} is involved in.
     */
    public Collection getRelations();
    
    /**
     * Adds a particular {@link Relation} to the set this {@link Key} is involved in.
     * It checks first to make sure it is actually involved. It quietly ignores it if
     * it already knows about this {@link Relation}.
     * @param rel the {@link Relation} to add to this {@link Key}.
     * @throws AssociationException if it is not actually involved in the given
     * {@link Relation} in any way.
     * @throws NullPointerException if the {@link Relation} argument was null.
     */
    public void addRelation(Relation rel) throws AssociationException, NullPointerException;
    
    /**
     * Removes a particular {@link Relation} from the set this {@link Key} is involved in.
     * It quietly ignores it if it is not involved or doesn't know about this {@link Relation}.
     * @param rel the {@link Relation} to remove from this {@link Key}.
     * @throws NullPointerException if the {@link Relation} argument was null.
     */
    public void removeRelation(Relation rel) throws NullPointerException;
    
    /**
     * Returns the {@link Table} this {@link Key} is formed over.
     * @return the {@link Table} this {@link Key} involves.
     */
    public Table getTable();
    
    /**
     * Returns the list of {@link Column}s this {@link Key} is formed over. It will always
     * return a list with at least one entry in it.
     * @return the list of {@link Column}s this {@link Key} involves.
     */
    public List getColumns();
    
    /**
     * Counts the {@link Column}s this {@link Key} is formed over. It will always
     * return values >= 1.
     * @return the number of {@link Column}s this {@link Key} involves.
     */
    public int countColumns();
    
    /**
     * Deletes this {@link Key}, and also deletes all {@link Relation}s that use it.
     */
    public void destroy();
    
    /**
     * This interface is designed to mark {@link Key} instances as primary keys.
     */
    public interface PrimaryKey extends Key {
    }
    
    /**
     * This interface is designed to mark {@link Key} instances as foreign keys.
     */
    public interface ForeignKey extends Key {
    }
    
    /**
     * This interface is designed to mark {@link Key} instances as single-column keys.
     */
    public interface SimpleKey extends Key {
    }
    
    /**
     * This interface is designed to mark {@link Key} instances as compound keys.
     */
    public interface CompoundKey extends Key {
    }
    
    /**
     * The generic implementation provides the basics for more complex key types to extend.
     * It doesn't know anything except which {@link Column}s and {@link Relation}s are involved.
     */
    public class GenericKey implements Key {
        /**
         * Internal reference to the set of {@link Column}s this {@link Key} is over.
         */
        private final List cols = new ArrayList();
        
        /**
         * Internal reference to the set of {@link Relations}s this {@link Key} is involved in.
         */
        private final Set rels = new HashSet();
        
        /**
         * Internal reference to the {@link Table} of this {@link Key}.
         */
        private Table table;
        
        /**
         * Internal reference to the {@link ComponentStatus} of this {@link Key}.
         */
        private ComponentStatus status;
        
        /**
         * The default constructor sets the status to INFERRED.
         */
        public GenericKey() {
            this.status = ComponentStatus.INFERRED;
        }
        
        /**
         * The constructor constructs a {@link Key} over a set of {@link Column}s. It doesn't check
         * to make sure they all come from the same {@link Table}, nor does it check to see if
         * they are in a sensible order. The order they are specified in here is the order in which
         * the {@link Key} will refer to them in future. The list of {@link Column}s cannot be changed
         * outside this constructor. Nulls inside the list are ignored, but if it finds any non-{@link Column}
         * objects in the list an exception will be thrown. The list must contain at least two {@link Column}s.
         * @param cols the {@link List} of {@link Column}s to form the key over.
         * @throws NullPointerException if the input list or table is null.
         * @throws IllegalArgumentException if the input list contains any non-null non-{@link Column}
         * objects or contains less than 2 {@link Column}s.
         * @throws AssociationException if any of the {@link Column}s do not belong to
         * the {@link Table} specified.
         */
        public GenericKey(List cols) throws IllegalArgumentException, NullPointerException, AssociationException {
            // Call the default constructor first.
            this();
            // Sanity check.
            if (cols==null)
                throw new NullPointerException("Key must be formed over a non-null set of columns.");
            // Do the work.
            for (Iterator i = cols.iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o==null) continue;
                if (!(o instanceof Column))
                    throw new IllegalArgumentException("List of columns must only contain Column instances.");
                Column c = (Column)o;
                if (this.table==null) this.table = c.getTable();
                if (!c.getTable().equals(this.table))
                    throw new AssociationException("All columns must belong to the same table.");
                this.cols.add(c);
            }
            // Final sanity check.
            if (this.cols.size()<2)
                throw new IllegalArgumentException("List of columns must contain at least two Column instances.");
        }
        
        /**
         * The constructor constructs a {@link Key} over a single {@link Column}. The {@link Column}
         * cannot be changed outside this constructor.
         * @param col the {@link Column} to form the key over.
         * @throws NullPointerException if the input {@link Column} is null.
         */
        public GenericKey(Column col) throws NullPointerException {
            // Call the default constructor first.
            this();
            // Sanity check.
            if (col==null)
                throw new NullPointerException("Column cannot be null.");
            // Do the work.
            this.cols.add(col);
            this.table = col.getTable();
        }
        
        /**
         * Returns the {@link ComponentStatus} of this {@link Key}. The default value,
         * unless otherwise specified, is INFERRED.
         * @return the {@link ComponentStatus} of this {@link Key}.
         */
        public ComponentStatus getStatus() {
            return this.status;
        }
        
        /**
         * Sets the {@link ComponentStatus} of this {@link Key}. The default value,
         * unless otherwise specified, is INFERRED.
         * @param s the new {@link ComponentStatus} of this {@link Key}.
         */
        public void setStatus(ComponentStatus s) {
            this.status = s;
        }
        
        /**
         * Returns all {@link Relation}s this {@link Key} is involved in. The set may be
         * empty but it will never be null.
         * @return the set of all {@link Relation}s this {@link Key} is involved in.
         */
        public Collection getRelations() {
            return this.rels;
        }
        
        /**
         * Adds a particular {@link Relation} to the set this {@link Key} is involved in.
         * It checks first to make sure it is actually involved. It quietly ignores it if
         * it already knows about this {@link Relation}.
         * @param rel the {@link Relation} to add to this {@link Key}.
         * @throws AssociationException if it is not actually involved in the given
         * {@link Relation} in any way.
         * @throws NullPointerException if the {@link Relation} argument was null.
         */
        public void addRelation(Relation rel) throws AssociationException, NullPointerException {
            // Sanity check.
            if (rel==null)
                throw new NullPointerException("Relation to be added cannot be null.");
            // Does it refer to us?
            if (!(rel.getForeignKey()==this || rel.getPrimaryKey()==this))
                throw new AssociationException("Relation does not refer to this key.");
            // Quietly ignore if we've already got it.
            if (this.rels.contains(rel)) return;
            // Otherwise, do the work.
            this.rels.add(rel);
        }
        
        /**
         * Removes a particular {@link Relation} from the set this {@link Key} is involved in.
         * It quietly ignores it if it is not involved or doesn't know about this {@link Relation}.
         * @param rel the {@link Relation} to remove from this {@link Key}.
         * @throws NullPointerException if the {@link Relation} argument was null.
         */
        public void removeRelation(Relation rel) throws NullPointerException {
            // Sanity check.
            if (rel==null)
                throw new NullPointerException("Relation to be removed cannot be null.");
            // Quietly ignore if we dont' know about iit.
            if (!this.rels.contains(rel)) return;
            // Otherwise, do the work.
            this.rels.remove(rel);
        }
        
        /**
         * Returns the {@link Table} this {@link Key} is formed over.
         * @return the {@link Table} this {@link Key} involves.
         */
        public Table getTable() {
            return this.table;
        }
        
        /**
         * Returns the list of {@link Column}s this {@link Key} is formed over. It will always
         * return a list with at least one entry in it.
         * @return the list of {@link Column}s this {@link Key} involves.
         */
        public List getColumns() {
            return this.cols;
        }
        
        /**
         * Counts the {@link Column}s this {@link Key} is formed over. It will always
         * return values >= 1.
         * @return the number of {@link Column}s this {@link Key} involves.
         */
        public int countColumns() {
            return this.getColumns().size();
        }
        
        /**
         * Deletes this {@link Key}, and also deletes all {@link Relation}s that use it.
         * If it was a {@link PrimaryKey} it will remove itself from the associated 
         * {@link Table}, and likewise if it was a {@link ForeignKey}.
         */
        public void destroy() {
            // Remove all the relations.
            for (Iterator i = this.rels.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                r.destroy();
            }
            // Remove references from tables.
            if (this instanceof PrimaryKey) {
                try {
                    this.getTable().setPrimaryKey(null);
                } catch (AssociationException e) {
                    throw new AssertionError("Primary key could not be set to null.");
                }
            } else if (this instanceof ForeignKey) {
                this.getTable().removeForeignKey((ForeignKey)this);
            } else {
                // Do nothing. Sure there may only be primary and foreign keys now,
                // but what about the future...?
            }
        }
        
        /**
         * Displays the name of this {@link Key} object. The name is the concatenation
         * of all the columns, contained in curly brackets and comma separated.
         * @return the name of this {@link Key} object.
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("{");
            for (Iterator i = this.cols.iterator(); i.hasNext(); ) {
                Column c = (Column)i.next();
                sb.append(c.toString());
                if (i.hasNext()) sb.append(",");
            }
            sb.append("}");
            return sb.toString();
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
         * @throws ClassCastException if the object o is not a {@link Key}.
         */
        public int compareTo(Object o) throws ClassCastException {
            Key k = (Key)o;
            return this.toString().compareTo(k.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link Key}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof Key)) return false;
            Key k = (Key)o;
            return k.toString().equals(this.toString());
        }
    }
    
    /**
     * This implementation is the building block for a single-{@link Column} {@link Key}.
     */
    public class GenericSimpleKey extends GenericKey implements SimpleKey {
        /**
         * The constructor passes on all its work to the {@link GenericKey} constructor.
         * @param col the {@link Column} to form the key over.
         * @throws NullPointerException if the input {@link Column} is null.
         */
        public GenericSimpleKey(Column col) throws NullPointerException {
            super(col);
        }
    }
    
    /**
     * This implementation is a single-column primary key.
     */
    public class SimplePrimaryKey extends GenericSimpleKey implements PrimaryKey {
        /**
         * The constructor passes on all its work to the {@link GenericSimpleKey} constructor. It then
         * sets itself as the {@link PrimaryKey} on the parent {@link Table}.
         * @param col the {@link Column} to form the key over.
         * @throws NullPointerException if the input {@link Column} is null.
         */
        public SimplePrimaryKey(Column col) throws NullPointerException {
            super(col);
            try {
                this.getTable().setPrimaryKey(this);
            } catch (AssociationException e) {
                throw new AssertionError("Primary key table does not match itself.");
            }
        }
    }
    
    /**
     * This implementation is a single-column foreign key.
     */
    public class SimpleForeignKey extends GenericSimpleKey implements ForeignKey {
        /**
         * The constructor passes on all its work to the {@link GenericSimpleKey} constructor. It then
         * adds itself to the set of {@link ForeignKey}s on the parent {@link Table}.
         * @param col the {@link Column} to form the key over.
         * @throws NullPointerException if the input {@link Column} is null.
         */
        public SimpleForeignKey(Column col) throws NullPointerException {
            super(col);
            try {
                this.getTable().addForeignKey(this);
            } catch (AssociationException e) {
                throw new AssertionError("Foreign key table does not match itself.");
            }
        }
    }
    
    /**
     * This implementation is the building block for a multi-{@link Column} {@link Key}.
     */
    public class GenericCompoundKey extends GenericKey implements CompoundKey {
        /**
         * The constructor passes on all its work to the {@link GenericKey} constructor. It then
         * sets itself as the {@link PrimaryKey} on the parent {@link Table}.
         * @param cols the {@link List} of {@link Column}s to form the key over.
         * @throws NullPointerException if the input list is null.
         * @throws IllegalArgumentException if the input list contains any non-null non-{@link Column}
         * objects or contains less than 2 {@link Column}s.
         * @throws AssociationException if any of the {@link Column}s do not belong to
         * the {@link Table} specified.
         */
        public GenericCompoundKey(List cols) throws NullPointerException, IllegalArgumentException, AssociationException {
            super(cols);
        }
    }
    
    /**
     * This implementation is a multi-column primary key.
     */
    public class CompoundPrimaryKey extends GenericCompoundKey implements PrimaryKey {
        /**
         * The constructor passes on all its work to the {@link GenericCompoundKey} constructor.
         * @param cols the {@link List} of {@link Column}s to form the key over.
         * @throws NullPointerException if the input list is null.
         * @throws IllegalArgumentException if the input list contains any non-null non-{@link Column}
         * objects or contains less than 2 {@link Column}s.
         * @throws AssociationException if any of the {@link Column}s do not belong to
         * the {@link Table} specified.
         */
        public CompoundPrimaryKey(List cols) throws NullPointerException, IllegalArgumentException, AssociationException {
            super(cols);
            try {
                this.getTable().setPrimaryKey(this);
            } catch (AssociationException e) {
                throw new AssertionError("Primary key table does not match itself.");
            }
            
        }
    }
    
    /**
     * This implementation is a multi-column foreign key.
     */
    public class CompoundForeignKey extends GenericCompoundKey implements ForeignKey {
        /**
         * The constructor passes on all its work to the {@link GenericCompoundKey} constructor. It then
         * adds itself to the set of {@link ForeignKey}s on the parent {@link Table}.
         * @param cols the {@link List} of {@link Column}s to form the key over.
         * @throws NullPointerException if the input list is null.
         * @throws IllegalArgumentException if the input list contains any non-null non-{@link Column}
         * objects or contains less than 2 {@link Column}s.
         * @throws AssociationException if any of the {@link Column}s do not belong to
         * the {@link Table} specified.
         */
        public CompoundForeignKey(List cols) throws NullPointerException, IllegalArgumentException, AssociationException {
            super(cols);
            try {
                this.getTable().addForeignKey(this);
            } catch (AssociationException e) {
                throw new AssertionError("Foreign key table does not match itself.");
            }
        }
    }
}
