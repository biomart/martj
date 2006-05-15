/*
 * Key.java
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
import java.util.Iterator;
import java.util.List;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>The {@link Key} interface is core to the way {@link Table}s get associated. They
 * are involved in {@link Relation}s which link {@link Table}s together in various ways, and
 * provide information about which {@link Column}s at each end correspond.</p>
 * <p>The {@link GenericSimpleKey} and {@link GenericCompoundKey} implementations are
 * there to provide the basics for more complex {@link Key}s. They keep track of which
 * {@link Column}s are involved, and which {@link Relation}s refer to this {@link Key}, but
 * not much more. {@link GenericUnionKey} is much the same.</p>
 * <p>Unless otherwise specified, all {@link Key}s are created with a default
 * {@link ComponentStatus} of INFERRED.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 15th May 2006
 * @since 0.1
 */
public interface Key extends Comparable {
    /**
     * Returns the name of this key.
     * @return the name of this {@link Key}.
     */
    public String getName();
    
    public Collection getColumnNames();
    
    /**
     * Returns the {@link ComponentStatus} of this {@link Key}. The default value,
     * unless otherwise specified, is INFERRED.
     * @return the {@link ComponentStatus} of this {@link Key}.
     */
    public ComponentStatus getStatus();
    
    /**
     * Sets the {@link ComponentStatus} of this {@link Key}. The default value,
     * unless otherwise specified, is INFERRED.
     * @param status the new {@link ComponentStatus} of this {@link Key}.
     */
    public void setStatus(ComponentStatus status);
    
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
     * @param relation the {@link Relation} to add to this {@link Key}.
     * @throws AssociationException if it is not actually involved in the given
     * {@link Relation} in any way.
     */
    public void addRelation(Relation relation) throws AssociationException;
    
    /**
     * Removes a particular {@link Relation} from the set this {@link Key} is involved in.
     * It quietly ignores it if it is not involved or doesn't know about this {@link Relation}.
     * @param relation the {@link Relation} to remove from this {@link Key}.
     */
    public void removeRelation(Relation relation);
    
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
     * return values > = 1.
     * @return the number of {@link Column}s this {@link Key} involves.
     */
    public int countColumns();
    
    public void setColumns(List columns) throws AssociationException;
    
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
     * The generic implementation provides the basics for more complex key types to extend.
     * It doesn't know anything except which {@link Column}s and {@link Relation}s are involved.
     */
    public class GenericKey implements Key {
        /**
         * Internal reference to the set of {@link Column}s this {@link Key} is over.
         */
        private final List columns = new ArrayList();
        
        /**
         * Internal reference to the set of {@link Relations}s this {@link Key} is involved in.
         * Keys are relation names.
         */
        private final List relations = new ArrayList();
        
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
         * objects in the list an exception will be thrown. The list must contain at least one {@link Column}.
         * @param columns the {@link List} of {@link Column}s to form the key over.
         * @throws IllegalArgumentException if the input list contains any non-null non-{@link Column}
         * objects or contains less than 1 {@link Column}s.
         * @throws AssociationException if any of the {@link Column}s do not belong to
         * the {@link Table} specified.
         */
        public GenericKey(List columns) throws IllegalArgumentException, AssociationException {
            // Call the default constructor first.
            this();
            // Do the work.
            this.setColumns(columns);
        }
        
        /**
         * The constructor constructs a {@link Key} over a single {@link Column}. The {@link Column}
         * cannot be changed outside this constructor.
         * @param column the {@link Column} to form the key over.
         */
        public GenericKey(Column column) {
            // Call the default constructor first.
            this();
            // Do the work.
            this.columns.add(column);
            this.table = column.getTable();
        }
        
        public void setColumns(List columns) throws AssociationException {
            this.columns.clear();
            for (Iterator i = columns.iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o == null) continue;
                if (!(o instanceof Column))
                    throw new IllegalArgumentException(BuilderBundle.getString("columnNotColumn"));
                Column column = (Column)o;
                if (this.table == null) this.table = column.getTable();
                if (!column.getTable().equals(this.table))
                    throw new AssociationException(BuilderBundle.getString("multiTableColumns"));
                if (this.columns.contains(column))
                    throw new AssociationException(BuilderBundle.getString("duplicateColumnsInKey"));
                this.columns.add(column);
            }
            // Final sanity check.
            if (this.columns.size() < 1)
                throw new IllegalArgumentException(BuilderBundle.getString("columnsIsEmpty"));
            // Invalidate all relations associated with this key.
            List deadRels = new ArrayList();
            for (Iterator i = this.relations.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                if (r.getStatus().equals(ComponentStatus.HANDMADE)) deadRels.add(r);
                else r.setStatus(ComponentStatus.INFERRED_INCORRECT);
            }
            for (Iterator i = deadRels.iterator(); i.hasNext(); ) ((Relation)i.next()).destroy();
        }
        
        /**
         * {@inheritDoc}
         */
        public ComponentStatus getStatus() {
            return this.status;
        }
        
        /**
         * {@inheritDoc}
         */
        public void setStatus(ComponentStatus status) {
            this.status = status;
            // Invalidate all relations associated with this key.
            List deadRels = new ArrayList();
            for (Iterator i = this.relations.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                if (r.getStatus().equals(ComponentStatus.HANDMADE)) deadRels.add(r);
                else r.setStatus(ComponentStatus.INFERRED_INCORRECT);
            }
            for (Iterator i = deadRels.iterator(); i.hasNext(); ) ((Relation)i.next()).destroy();
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getRelations() {
            return this.relations;
        }
        
        /**
         * {@inheritDoc}
         */
        public void addRelation(Relation relation) throws AssociationException {
            // Does it refer to us?
            if (!(relation.getForeignKey() == this || relation.getPrimaryKey() == this))
                throw new AssociationException(BuilderBundle.getString("relationNotOfThisKey"));
            // Quietly ignore if we've already got it.
            if (this.relations.contains(relation)) return;
            // Otherwise, do the work.
            this.relations.add(relation);
        }
        
        /**
         * {@inheritDoc}
         */
        public void removeRelation(Relation relation) {
            // Quietly ignore if we dont' know about iit.
            if (!this.relations.contains(relation)) return;
            // Otherwise, do the work.
            this.relations.remove(relation);
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
        public List getColumns() {
            return this.columns;
        }
        
        /**
         * {@inheritDoc}
         */
        public int countColumns() {
            return this.getColumns().size();
        }
        
        /**
         * {@inheritDoc}
         */
        public void destroy() {
            // Remove all the relations.
            for (Iterator i = this.relations.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                r.destroy();
            }
            // Remove references from tables.
            if (this instanceof PrimaryKey) {
                try {
                    this.getTable().setPrimaryKey(null);
                } catch (AssociationException e) {
                    AssertionError ae = new AssertionError(BuilderBundle.getString("pkNotNullable"));
                    ae.initCause(e);
                    throw ae;
                }
            } else if (this instanceof ForeignKey) {
                this.getTable().removeForeignKey((ForeignKey)this);
            } else {
                throw new AssertionError(BuilderBundle.getString("unknownKey", this.getClass().getName()));
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getColumnNames() {
            List names = new ArrayList();
            for (Iterator i = this.columns.iterator(); i.hasNext(); ) {
                Column c = (Column)i.next();
                names.add(c.getName());
            }
            return names;
        }
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getTable().toString());
            sb.append(this.getColumnNames().toString());
            return sb.toString();
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
            Key k = (Key)o;
            return this.toString().compareTo(k.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Key)) return false;
            Key k = (Key)o;
            return k.toString().equals(this.toString());
        }
    }
    
    /**
     * This implementation is a simple primary key.
     */
    public class GenericPrimaryKey extends GenericKey implements PrimaryKey {
        /**
         * The constructor passes on all its work to the {@link GenericKey} constructor.
         * @param columns the {@link List} of {@link Column}s to form the key over.
         * @throws IllegalArgumentException if the input list contains any non-null non-{@link Column}
         * objects or contains less than 2 {@link Column}s.
         * @throws AssociationException if any of the {@link Column}s do not belong to
         * the {@link Table} specified.
         */
        public GenericPrimaryKey(List columns) throws AssociationException {
            super(columns);
        }
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            String supername = super.getName();
            return BuilderBundle.getString("pkPrefix") + supername;
        }
    }
    
    /**
     * This implementation is a simple foreign key.
     */
    public class GenericForeignKey extends GenericKey implements ForeignKey {
        /**
         * The constructor passes on all its work to the {@link GenericKey} constructor. It then
         * adds itself to the set of {@link ForeignKey}s on the parent {@link Table}.
         * @param columns the {@link List} of {@link Column}s to form the key over.
         * @throws IllegalArgumentException if the input list contains any non-null non-{@link Column}
         * objects or contains less than 2 {@link Column}s.
         * @throws AssociationException if any of the {@link Column}s do not belong to
         * the {@link Table} specified.
         */
        public GenericForeignKey(List columns) throws AssociationException {
            super(columns);
        }
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            String supername = super.getName();
            return BuilderBundle.getString("fkPrefix") + supername;
        }
    }
}
