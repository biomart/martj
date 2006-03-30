/*
 * Window.java
 *
 * Created on 27 March 2006, 13:56
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.DataSet.GenericDataSet;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Relation.OneToMany;

/**
 * <p>Represents a window onto a {@link Table} in a {@link TableProvider}.
 * The window allows masking of unwanted {@link Relation}s and {@link Column}s.
 * It will automatically mask out permanently any {@link Relation}s forming more than
 * two levels of 1:m from the central table, and will flag any forming more than one such
 * level as 'Concat only'. 'Concat only' means that the remote primary key will be merged
 * as a single column with all unique values concatenated when followed in the 1:M
 * direction, but this is ignored in the M:1 direction. Additional relations beyond a
 * concat relation should be ignored when building the final mart.</p>
 *
 * <p>The name of the window is inherited by the {@link Dataset} so take care when
 * choosing it.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 30th March 2006
 * @since 0.1
 */
public class Window implements Comparable {
    /**
     * Internal reference to the name of this window.
     */
    private final String name;
    
    /**
     * Internal reference to the parent schema.
     */
    private final Schema schema;
    
    /**
     * Internal reference to the central table.
     */
    private final Table centralTable;
    
    /**
     * Internal reference to the transformation data set.
     */
    private final DataSet dataset;
    
    /**
     * Internal reference to masked relations.
     */
    private final Set maskedRelations = new HashSet();
    
    /**
     * Internal reference to masked columns.
     */
    private final Set maskedColumns = new HashSet();
    
    /**
     * Internal reference to partitioned columns (keys are columns,
     * values are {@link PartitionedColumnType}s).
     */
    private final Map partitionedColumns = new HashMap();
    
    /**
     * Internal reference to relations between subclassed tables.
     */
    private final Set subclassedRelations = new HashSet();
    
    /**
     * Internal reference to disabled relations.
     */
    private final Set disabledRelations = new HashSet();
    
    /**
     * Internal reference to concat-only relations. The keys of
     * the map are relations, the values are concat types.
     */
    private final Map concatOnlyRelations = new HashMap();
    
    /**
     * Internal reference to whether or not to partition by the {@link PartitionedTableProvider}.
     */
    private boolean partitionOnTableProvider = false;
    
    /**
     * Internal reference to the relations already seen when doing the prediction walk.
     */
    private Map relationsPredicted = new HashMap();
    
    /**
     * The constructor creates a {@link Window} around one central {@link Table} and
     * gives it a name. It also initiates a {@link DataSet} ready to contain the transformed
     * results.
     * @param s the {@link Schema} this {@link Window} will belong to.
     * @param t the {@link Table} to use as the central table.
     * @param name the name to give this {@link Window}.
     * @throws NullPointerException if any parameter is null.
     * @throws AssociationException if the {@link Table} does not belong to any of the
     * {@link TableProvider} objects in the {@link Schema} this {@link Window} belongs to.
     */
    public Window(Schema s, Table t, String name) throws NullPointerException, AssociationException {
        // Sanity check.
        if (s==null)
            throw new NullPointerException("Schema cannot be null.");
        if (t==null)
            throw new NullPointerException("Central table cannot be null.");
        if (name==null)
            throw new NullPointerException("Window name cannot be null.");
        if (!s.getTableProviders().contains(t.getTableProvider()))
            throw new AssociationException("Cannot use table that is not part of the schema supplied.");
        // Do it.
        this.schema = s;
        this.centralTable = t;
        this.name = name;
        this.dataset = new GenericDataSet(this);
        // Predict some sensible defaults.
        this.predictRelationTypes();
    }
    
    /**
     * This method identifies all candidate subset and concat relations, and masks out all relations
     * more than 2 1:M relations away from the main table.
     */
    public void predictRelationTypes() {
        this.relationsPredicted.clear();
        // Predict the subclass relations from the existing m:1 relations - simple guesser based
        // on finding foreign keys in the central table.
        for (Iterator i = this.getTable().getForeignKeys().iterator(); i.hasNext(); ) {
            Key k = (Key)i.next();
            for (Iterator j = k.getRelations().iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                // Only flag potential m:1 subclass relations if they don't refer back to ourselves.
                try {
                    if (!r.getPrimaryKey().getTable().equals(this.getTable())) this.flagSubclassRelation(r);
                } catch (AssociationException e) {
                    throw new AssertionError();
                }
            }
        }
        // Find the shortest 1:m paths (depths) of each relation we have.
        try {
            this.walkRelations(this.getTable(), 1);
        } catch (NullPointerException e) {
            throw new AssertionError("Found a null reference to a table.");
        }
        // Check on depths of relations.
        for (Iterator i = this.relationsPredicted.keySet().iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            int depth = ((Integer)this.relationsPredicted.get(r)).intValue();
            if (depth>2) {
                // Mask all relations that involve more than two levels of 1:m abstraction away
                // from the central main table.
                this.maskRelation(r);
            } else if (depth>1) {
                // Mark as concat-only (default to comma-separation) all 1:m relations that involve
                // more than one level of 1:m abstraction away from the central main table.
                this.flagConcatOnlyRelation(r, ConcatRelationType.COMMA);
            }
        }
    }
    
    /**
     * Internal method which works out the lowest number of 1:M relations between
     * the current table and all other {@link Table}s linked by as-yet-unvisited {@link Relation}s.
     * @param current the {@link Table} to start walking from.
     * @param currentDepth the number of 1:M relations it took to get this far.
     * @throws NullPointerException if the table parameter is null.
     */
    private void walkRelations(Table current, int currentDepth) throws NullPointerException {
        // Sanity check.
        if (current==null)
            throw new NullPointerException("Current table cannot be null.");
        // Find all relations from this table.
        Set relations = new HashSet();
        relations.addAll(current.getPrimaryKey().getRelations());
        for (Iterator i = current.getForeignKeys().iterator(); i.hasNext(); ) {
            Key k = (Key)i.next();
            relations.addAll(k.getRelations());
        }
        // See if we need to do anything to each one.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            // Seen before?
            if (this.relationsPredicted.containsKey(r)) {
                // Have we got there by a shorter path? If not, then we can skip it.
                int previousDepth = ((Integer)this.relationsPredicted.get(r)).intValue();
                if (previousDepth <= currentDepth) continue;
            }
            // Update the depth count on this relation.
            this.relationsPredicted.put(r, new Integer(currentDepth));
            // Work out where to go next.
            if (r.getPrimaryKey().getTable().equals(current) && (r instanceof OneToMany)) {
                // If current is at the one end of a one-to-many relation, then
                // up the count and recurse down it.
                this.walkRelations(r.getForeignKey().getTable(), currentDepth+1);
            } else {
                // Otherwise, recurse down it anyway but leave the count as-is.
                if (r.getPrimaryKey().getTable().equals(current))
                    this.walkRelations(r.getForeignKey().getTable(), currentDepth);
                else
                    this.walkRelations(r.getPrimaryKey().getTable(), currentDepth);
            }
        }
    }
    
    /**
     * Returns the name of this {@link Window}. The name will also be used for the
     * {@link DataSet} generated from this {@link Window}.
     * @return the name of this {@link Window}.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Returns the {@link Schema} of this {@link Window}.
     * @return the {@link Schema} of this {@link Window}.
     */
    public Schema getSchema() {
        return this.schema;
    }
    
    /**
     * Returns the central {@link Table} of this {@link Window}.
     * @return the central {@link Table} of this {@link Window}.
     */
    public Table getTable() {
        return this.centralTable;
    }
    
    /**
     * Returns the {@link DataSet} generated by this {@link Window}.
     * @return the {@link DataSet} generated by this {@link Window}.
     */
    public DataSet getDataSet() {
        return this.dataset;
    }
    
    /**
     * Mask a {@link Relation}. If it is already masked, ignore it.
     * An exception will be thrown if it is null.
     * @param r the {@link Relation} to mask.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void maskRelation(Relation r) throws NullPointerException {
        // Sanity check.
        if (r==null)
            throw new NullPointerException("Cannot mask a relation which is null.");
        // Do it.
        this.maskedRelations.add(r);
    }
    
    /**
     * Unmask a {@link Relation}. If it is already unmasked, ignore it.
     * An exception will be thrown if it is null.
     * @param r the {@link Relation} to unmask.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void unmaskRelation(Relation r) throws NullPointerException {
        // Sanity check.
        if (r==null)
            throw new NullPointerException("Cannot mask a relation which is null.");
        // Do it.
        this.maskedRelations.remove(r);
    }
    
    /**
     * Return the set of masked {@link Relation}s. It may be empty, but never null.
     * @return the set of masked {@link Relation}s.
     */
    public Collection getMaskedRelations() {
        return this.maskedRelations;
    }
    
    /**
     * Mask a {@link Column}. If it is already masked, ignore it.
     * An exception will be thrown if it is null. If the {@link Column} is
     * part of any {@link Key}, then all {@link Relation}s using that {@link Key}
     * will be masked as well.
     * @param c the {@link Column} to mask.
     * @throws NullPointerException if the {@link Column} is null.
     */
    public void maskColumn(Column c) throws NullPointerException {
        // Sanity check.
        if (c==null)
            throw new NullPointerException("Cannot mask a column which is null.");
        // Do it.
        this.maskedColumns.add(c);
        // Mask the associated relations.
        Table t = c.getTable();
        // Primary key first.
        Key pk = t.getPrimaryKey();
        if (pk!=null && pk.getColumns().contains(c)) {
            for (Iterator j= pk.getRelations().iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                this.maskRelation(r);
            }
        }
        // Then foreign keys.
        for (Iterator i = t.getForeignKeys().iterator(); i.hasNext(); ) {
            Key fk = (Key)i.next();
            if (fk.getColumns().contains(c)) {
                for (Iterator j= fk.getRelations().iterator(); j.hasNext(); ) {
                    Relation r = (Relation)j.next();
                    this.maskRelation(r);
                }
            }
        }
    }
    
    /**
     * Unmask a {@link Column}. If it is already unmasked, ignore it.
     * An exception will be thrown if it is null.
     * @param c the {@link Column} to unmask.
     * @throws NullPointerException if the {@link Column} is null.
     */
    public void unmaskColumn(Column c) throws NullPointerException {
        // Sanity check.
        if (c==null)
            throw new NullPointerException("Cannot mask a column which is null.");
        // Do it.
        this.maskedColumns.remove(c);
    }
    
    /**
     * Return the set of masked {@link Column}s. It may be empty, but never null.
     * @return the set of masked {@link Column}s.
     */
    public Collection getMaskedColumns() {
        return this.maskedColumns;
    }
    
    /**
     * Mark a {@link Table} as a subclass of another by marking the {@link Relation} between them.
     * The {@link ForeignKey} end of the relation is the subclass end, and the {@link PrimaryKey} end
     * is the parent table, but the parent table may not actually be the central table in this {@link Window}.
     * If it is already marked, ignore it. As subclasses can only apply to the central table, throw an AssociationException
     * if this is attempted on any table other than the central table. An exception will be thrown if any parameter is null.
     * @param subclass the {@link Relation} to mark as a subclass relation.
     * @throws AssociationException if one end of the {@link Relation} is not the central table for this window, or
     * if both ends of the {@link Relation} point to the same table.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void flagSubclassRelation(Relation subclass) throws NullPointerException, AssociationException {
        // Sanity check.
        if (subclass==null)
            throw new NullPointerException("Cannot mark a subclass relation which is null.");
        if (!(subclass.getPrimaryKey().getTable().equals(this.centralTable) ||
                subclass.getForeignKey().getTable().equals(this.centralTable)))
            throw new AssociationException("Subclassing can only take place on a relation from the central table.");
        if (subclass.getPrimaryKey().getTable().equals(subclass.getForeignKey().getTable()))
            throw new AssociationException("Subclassing can only take place between two distinct tables.");
        // Do it.
        this.subclassedRelations.add(subclass);
    }
    
    /**
     * Unmark a {@link Relation} as a subclass relation. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param subclass the {@link Relation} to unmark.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void unflagSubclassRelation(Relation subclass) throws NullPointerException {
        // Sanity check.
        if (subclass==null)
            throw new NullPointerException("Cannot unmark a relation which is null.");
        // Do it.
        this.subclassedRelations.remove(subclass);
    }
    
    /**
     * Return the set of subclassed {@link Relation}s. It may be empty, but never null.
     * @return the set of subclassed {@link Relation}s.
     */
    public Collection getSubclassedRelations() {
        return this.subclassedRelations;
    }
    
    /**
     * Mark a {@link Column} as partitioned. If it is already marked, it updates the partition type.
     * An exception will be thrown if any parameter is null.
     * @param c the {@link Column} to mark as partitioned.
     * @param pct the {@link PartitionedColumnType} to use for the partition.
     * @throws NullPointerException if either parameter is null.
     */
    public void flagPartitionedColumn(Column c, PartitionedColumnType pct) throws NullPointerException {
        // Sanity check.
        if (c==null)
            throw new NullPointerException("Cannot partition a column which is null.");
        if (pct==null)
            throw new NullPointerException("Cannot use a partition type which is null.");
        // Do it (the Map will replace the value with the new value if the key already exists)
        this.partitionedColumns.put(c, pct);
    }
    
    /**
     * Unmark a {@link Column} as partitioned. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param c the {@link Column} to unmark.
     * @throws NullPointerException if the {@link Column} is null.
     */
    public void unflagPartitionedColumn(Column c) throws NullPointerException {
        // Sanity check.
        if (c==null)
            throw new NullPointerException("Cannot unpartition a column which is null.");
        // Do it.
        this.partitionedColumns.remove(c);
    }
    
    /**
     * Return the set of partitioned {@link Column}s. It may be empty, but never null.
     * @return the set of partitioned {@link Column}s.
     */
    public Collection getPartitionedColumns() {
        return this.partitionedColumns.keySet();
    }
    
    /**
     * Return the partition type of partitioned {@link Column} c.
     * It will return null if there is no such partitioned column.
     * @return the partition type of the {@link Column}.
     * @throws NullPointerException if the parameter passed in was null.
     */
    public PartitionedColumnType getPartitionedColumnType(Column c) throws NullPointerException {
        // Sanity check.
        if (c==null)
            throw new NullPointerException("Partitioned column cannot be null.");
        // Do we have it?
        if (!this.partitionedColumns.containsKey(c)) return null;
        // Return it.
        return (PartitionedColumnType)this.partitionedColumns.get(c);
    }
    
    /**
     * Mark a {@link Relation} as concat-only. If it is already marked, it updates the concat type.
     * An exception will be thrown if any parameter is null.
     * @param r the {@link Relation} to mark as concat-only.
     * @param crt the {@link ConcatRelationType} to use for the relation.
     * @throws NullPointerException if either parameter is null.
     */
    public void flagConcatOnlyRelation(Relation r, ConcatRelationType crt) throws NullPointerException {
        // Sanity check.
        if (r==null)
            throw new NullPointerException("Cannot modify a relation which is null.");
        if (crt==null)
            throw new NullPointerException("Cannot use a concat relation type which is null.");
        // Do it (the Map will replace the value with the new value if the key already exists)
        this.concatOnlyRelations.put(r, crt);
    }
    
    /**
     * Unmark a {@link Relation} as concat-only. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param c the {@link Relation} to unmark.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void unflagConcatOnlyRelation(Relation r) throws NullPointerException {
        // Sanity check.
        if (r==null)
            throw new NullPointerException("Cannot modify a column which is null.");
        // Do it.
        this.concatOnlyRelations.remove(r);
    }
    
    /**
     * Return the set of concat-only {@link Relation}s. It may be empty, but never null.
     * @return the set of concat-only {@link Relation}s.
     */
    public Collection getConcatOnlyRelations() {
        return this.concatOnlyRelations.keySet();
    }
    
    /**
     * Return the concat type of concat-only {@link Relation} r.
     * It will return null if there is no such concat-only relation.
     * @return the concat type of the {@link Relation}.
     * @throws NullPointerException if the parameter passed in was null.
     */
    public ConcatRelationType getConcatRelationType(Relation r) throws NullPointerException {
        // Sanity check.
        if (r==null)
            throw new NullPointerException("Relation to check cannot be null.");
        // Do we have it?
        if (!this.concatOnlyRelations.containsKey(r)) return null;
        // Return it.
        return (ConcatRelationType)this.concatOnlyRelations.get(r);
    }
    
    /**
     * If the user wishes to partition the main table by the table provider
     * (only possible if the main table is from a {@link PartitionedTableProvider}) then set
     * this flag to true. Otherwise, set it to false, which is its default value.
     * @param f true if you want to turn this on, false if you want to turn it off.
     */
    public void setPartitionOnTableProvider(boolean f) {
        this.partitionOnTableProvider = f;
    }
    
    /**
     * If the user wishes to partition the main table by the table provider
     * (only possible if the main table is from a {@link PartitionedTableProvider} then this
     * will return true. Otherwise, false, which is its default value.
     * @return true if user wants to turn this on, false if they want to turn it off.
     */
    public boolean getPartitionOnTableProvider() {
        return this.partitionOnTableProvider;
    }
    
    /**
     * Synchronise this {@link Window} with the {@link TableProvider} that is
     * providing its tables. Synchronisation means checking the masked {@link Column}
     * and {@link Relation} objects and removing any that have disappeared.
     * The associated {@link DataSet}, if one exists yet, is then regenerated.
     * If one does not exist, it is generated now.
     *
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchronise() throws SQLException, BuilderException {
        // Start with all-dead.
        Set deadRelations = new HashSet(this.maskedRelations);
        deadRelations.addAll(this.subclassedRelations);
        deadRelations.addAll(this.concatOnlyRelations.keySet());
        Set deadColumns = new HashSet(this.maskedColumns);
        deadColumns.addAll(this.partitionedColumns.keySet());
        // Iterate through tables in each table provider.
        // Un-dead all columns and relations in the schema
        for (Iterator tpi = this.getSchema().getTableProviders().iterator(); tpi.hasNext(); ) {
            TableProvider tp = (TableProvider)tpi.next();
            for (Iterator ti = tp.getTables().iterator(); ti.hasNext(); ) {
                Table t = (Table)ti.next();
                deadColumns.removeAll(t.getColumns());
                // Only need primary key as all relations involve primary keys at some point.
                Key pk = t.getPrimaryKey();
                deadRelations.removeAll(pk.getRelations());
            }
        }
        // Remove any dead stuff remaining.
        for (Iterator i = deadRelations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            this.maskedRelations.remove(r);
            this.subclassedRelations.remove(r);
            this.concatOnlyRelations.remove(r);
        }
        for (Iterator i = deadColumns.iterator(); i.hasNext(); ) {
            Column c = (Column)i.next();
            this.maskedColumns.remove(c);
            this.partitionedColumns.remove(c);
        }
        // Regenerate the dataset
        this.dataset.regenerate();
    }
    
    /**
     * Request that the mart for this window be constructed now.
     * @throws SQLException if there was any data source error during
     * mart construction.
     * @throws BuilderException if there was any other kind of error in the
     * mart construction process.
     */
    public void constructMart() throws BuilderException, SQLException {
        this.dataset.constructMart();
    }
    
    /**
     * Displays the name of this {@link Window} object.
     * @return the name of this {@link Window} object.
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
     * @throws ClassCastException if the object o is not a {@link Window}.
     */
    public int compareTo(Object o) throws ClassCastException {
        Window w = (Window)o;
        return this.toString().compareTo(w.toString());
    }
    
    /**
     * Return true if the toString()s are identical.
     * @param o the object to compare to.
     * @return true if the toString()s match and both objects are {@link Window}s,
     * otherwise false.
     */
    public boolean equals(Object o) {
        if (o==null || !(o instanceof Window)) return false;
        Window w = (Window)o;
        return w.toString().equals(this.toString());
    }
}
