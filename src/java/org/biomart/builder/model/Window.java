/*
 * Window.java
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.DataSet.GenericDataSet;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.MartConstructor.GenericMartConstructor;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>Represents a window onto a {@link Table} in a {@link TableProvider}.
 * The window allows masking of unwanted {@link Relation}s and {@link Column}s.
 * It will automatically mask out permanently any {@link Relation}s forming more than
 * two levels of 1:m from the central table, and will flag any forming more than one such
 * level as 'Concat only'. 'Concat only' means that the remote primary key will be merged
 * as a single column with all unique values concatenated when followed in the 1:M
 * direction, but this is ignored in the M:1 direction. Additional relations beyond a
 * concat relation should be ignored when building the final mart.</p>
 * <p>The name of the window is inherited by the {@link Dataset} so take care when
 * choosing it.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 7th April 2006
 * @since 0.1
 */
public class Window implements Comparable {
    /**
     * Internal reference to the name of this window.
     */
    private String name;
    
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
    private final Set maskedRelations = new TreeSet();
    
    /**
     * Internal reference to masked columns.
     */
    private final Set maskedColumns = new TreeSet();
    
    /**
     * Internal reference to partitioned columns (keys are columns,
     * values are {@link PartitionedColumnType}s).
     */
    private final Map partitionedColumns = new TreeMap();
    
    /**
     * Internal reference to relations between subclassed tables.
     */
    private final Set subclassedRelations = new TreeSet();
    
    /**
     * Internal reference to concat-only relations. The keys of
     * the map are relations, the values are concat types.
     */
    private final Map concatOnlyRelations = new TreeMap();
    
    /**
     * Internal reference to whether or not to partition by the {@link PartitionedTableProvider}.
     */
    private boolean partitionOnTableProvider = false;
    
    /**
     * Internal reference to the relations already seen when doing the prediction walk.
     */
    private Map relationsPredicted = new HashMap();
    
    /**
     * This constant refers to a placeholder mart constructor which does
     * nothing except prevent null pointer exceptions.
     */
    public static final MartConstructor DUMMY_MART_CONSTRUCTOR = new GenericMartConstructor("__DUMMY_MC");
    
    /**
     * The constructor creates a {@link Window} around one central {@link Table} and
     * gives it a name. It also initiates a {@link DataSet} ready to contain the transformed
     * results. It adds itself to the specified schema automatically.
     *
     * @param schema the {@link Schema} this {@link Window} will belong to.
     * @param centralTable the {@link Table} to use as the central centralTable.
     * @param name the name to give this {@link Window}.
     * @throws NullPointerException if any parameter is null.
     * @throws AssociationException if the {@link Table} does not belong to any of the
     * {@link TableProvider} objects in the {@link Schema} this {@link Window} belongs to.
     * @throws AlreadyExistsException if another {@link Window} with exactly the same name already exists
     * in the specified schema.
     */
    public Window(Schema schema, Table centralTable, String name) throws NullPointerException, AssociationException, AlreadyExistsException {
        // Sanity check.
        if (schema == null)
            throw new NullPointerException(BuilderBundle.getString("schemaIsNull"));
        if (centralTable == null)
            throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
        if (name == null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        if (!schema.getTableProviders().contains(centralTable.getTableProvider()))
            throw new AssociationException(BuilderBundle.getString("tableSchemaMismatch"));
        // Do it.
        this.schema = schema;
        this.centralTable = centralTable;
        this.name = name;
        this.dataset = new GenericDataSet(this);
        this.dataset.setMartConstructor(DUMMY_MART_CONSTRUCTOR);
        // Add ourselves.
        schema.addWindow(this);
    }
    
    /**
     * This method identifies all candidate concat relations, and masks out all relations
     * more than 2 1:M relations away from the main or subclass table.
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void optimiseRelations() throws SQLException, BuilderException {
        // Clear out our previous predictions.
        this.relationsPredicted.clear();
        this.maskedRelations.clear();
        this.concatOnlyRelations.clear();
        
        // Find the shortest 1:m paths (depths) of each relation we have.
        try {
            this.walkRelations(this.getCentralTable(), 1);
        } catch (NullPointerException e) {
            AssertionError ae = new AssertionError(BuilderBundle.getString("tableIsNull"));
            ae.initCause(e);
            throw ae;
        }
        
        // Check on depths of relations.
        for (Iterator i = this.relationsPredicted.keySet().iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            int depth = ((Integer)this.relationsPredicted.get(r)).intValue();
            if (depth > 2) {
                // Mask all relations that involve more than two levels of 1:m abstraction away
                // from the central main table.
                this.maskRelation(r);
            } else if (depth > 1) {
                // Mark as concat-only (default to comma-separation) all 1:m relations that involve
                // more than one level of 1:m abstraction away from the central main table.
                this.flagConcatOnlyRelation(r, ConcatRelationType.COMMA);
            }
        }
        
        // Regenerate the dataset.
        this.dataset.regenerate();
    }
    
    /**
     * Internal method which works out the lowest number of 1:M relations between
     * the currentTable table and all other {@link Table}s linked by as-yet-unvisited {@link Relation}s.
     * @param currentTable the {@link Table} to start walking from.
     * @param currentDepth the number of 1:M relations it took to get this far.
     * @throws NullPointerException if the table parameter is null.
     */
    private void walkRelations(Table currentTable, int currentDepth) throws NullPointerException {
        // Sanity check.
        if (currentTable == null)
            throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
        // Find all relations from this table.
        Collection relations = currentTable.getRelations();
        // See if we need to do anything to each one.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            // Dodge relation?
            if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) continue;
            // Seen before?
            else if (this.relationsPredicted.containsKey(r)) {
                // Have we got there by a shorter path? If not, then we can skip it.
                int previousDepth = ((Integer)this.relationsPredicted.get(r)).intValue();
                if (previousDepth <= currentDepth) continue;
            }
            // Update the depth count on this relation.
            this.relationsPredicted.put(r, new Integer(currentDepth));
            // Work out where to go next.
            if (r.getPrimaryKey().getTable().equals(currentTable) && (r.getFKCardinality().equals(Cardinality.MANY))) {
                // If currentTable is at the one end of a one-to-many relation, then
                // up the count (if it's not a subclass relation) and recurse down it.
                int nextDepth = currentDepth;
                if (!this.getSubclassedRelations().contains(r)) nextDepth++;
                this.walkRelations(r.getForeignKey().getTable(), nextDepth);
            } else {
                // Otherwise, recurse down it anyway but leave the count as-is.
                if (r.getPrimaryKey().getTable().equals(currentTable)) {
                    this.walkRelations(r.getForeignKey().getTable(), currentDepth);
                } else {
                    this.walkRelations(r.getPrimaryKey().getTable(), currentDepth);
                }
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
     * Sets the name of this {@link Window}. The name will also be used for the
     * {@link DataSet} generated from this {@link Window}.
     * @param name the new name of this {@link Window}.
     */
    public void setName(String name) {
        // Sanity check.
        if (name == null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        // Do it.
        this.name = name;
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
    public Table getCentralTable() {
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
     * @param relation the {@link Relation} to mask.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void maskRelation(Relation relation) throws NullPointerException {
        // Sanity check.
        if (relation == null)
            throw new NullPointerException(BuilderBundle.getString("relationIsNull"));
        // Do it.
        this.maskedRelations.add(relation);
    }
    
    /**
     * Unmask a {@link Relation}. If it is already unmasked, ignore it.
     * An exception will be thrown if it is null.
     * @param relation the {@link Relation} to unmask.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void unmaskRelation(Relation relation) throws NullPointerException {
        // Sanity check.
        if (relation == null)
            throw new NullPointerException(BuilderBundle.getString("relationIsNull"));
        // Do it.
        this.maskedRelations.remove(relation);
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
     * @param column the {@link Column} to mask.
     * @throws NullPointerException if the {@link Column} is null.
     */
    public void maskColumn(Column column) throws NullPointerException {
        // Sanity check.
        if (column == null)
            throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
        // Do it.
        this.maskedColumns.add(column);
        // Mask the associated relations.
        Table t = column.getTable();
        // Primary key first.
        Key pk = t.getPrimaryKey();
        if (pk != null && pk.getColumns().contains(column)) {
            for (Iterator j = pk.getRelations().iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                this.maskRelation(r);
            }
        }
        // Then foreign keys.
        for (Iterator i = t.getForeignKeys().iterator(); i.hasNext(); ) {
            Key fk = (Key)i.next();
            if (fk.getColumns().contains(column)) {
                for (Iterator j = fk.getRelations().iterator(); j.hasNext(); ) {
                    Relation r = (Relation)j.next();
                    this.maskRelation(r);
                }
            }
        }
    }
    
    /**
     * Unmask a {@link Column}. If it is already unmasked, ignore it.
     * An exception will be thrown if it is null.
     * @param column the {@link Column} to unmask.
     * @throws NullPointerException if the {@link Column} is null.
     */
    public void unmaskColumn(Column column) throws NullPointerException {
        // Sanity check.
        if (column == null)
            throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
        // Do it.
        this.maskedColumns.remove(column);
    }
    
    /**
     * Return the set of masked {@link Column}s. It may be empty, but never null.
     * @return the set of masked {@link Column}s.
     */
    public Collection getMaskedColumns() {
        return this.maskedColumns;
    }
    
    /**
     * <p>Mark a {@link Table} as a relation of another by marking the {@link Relation} between them.
     * The {@link ForeignKey} end of the relation is the relation end, and the {@link PrimaryKey} end
     * is the parent table, but the parent table may not actually be the central table in this {@link Window}.
     * If it is already marked, ignore it. As subclasses can only apply to the central table, throw an AssociationException
     * if this is attempted on any table other than the central table. An exception will be thrown if any parameter is null.</p>
     * <p>One further restriction is that a {@link Table} can only have a single M:1 subclass {@link Relation},
     * or multiple 1:M ones. It cannot have a mix of both, nor can it have more than one M:1 subclass {@link Relation}.
     * In either case if this is attempted an AssociationException will be thrown.</p>
     * @param relation the {@link Relation} to mark as a relation relation.
     * @throws AssociationException if one end of the {@link Relation} is not the central table for this window, or
     * if both ends of the {@link Relation} point to the same table.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void flagSubclassRelation(Relation relation) throws NullPointerException, AssociationException {
        // Sanity check.
        if (relation == null)
            throw new NullPointerException(BuilderBundle.getString("relationIsNull"));
        if (!(relation.getPrimaryKey().getTable().equals(this.centralTable) ||
                relation.getForeignKey().getTable().equals(this.centralTable)))
            throw new AssociationException(BuilderBundle.getString("subclassNotOnCentralTable"));
        if (relation.getPrimaryKey().getTable().equals(relation.getForeignKey().getTable()))
            throw new AssociationException(BuilderBundle.getString("subclassNotBetweenTwoTables"));
        // Check validity.
        boolean containsM1 = false;
        for (Iterator i = this.subclassedRelations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            if (r.getForeignKey().getTable().equals(this.centralTable)) {
                containsM1 = true;
                break; // no need to look any further.
            }
        }
        if (containsM1 && (relation.getPrimaryKey().getTable().equals(this.centralTable) || this.subclassedRelations.size()!=0))
            throw new AssociationException(BuilderBundle.getString("mixedCardinalitySubclasses"));
        // Do it.
        this.subclassedRelations.add(relation);
    }
    
    /**
     * Unmark a {@link Relation} as a relation relation. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param relation the {@link Relation} to unmark.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void unflagSubclassRelation(Relation relation) throws NullPointerException {
        // Sanity check.
        if (relation == null)
            throw new NullPointerException(BuilderBundle.getString("relationIsNull"));
        // Do it.
        this.subclassedRelations.remove(relation);
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
     *
     * @param column the {@link Column} to mark as partitioned.
     * @param type the {@link PartitionedColumnType} to use for the partition.
     * @throws NullPointerException if either parameter is null.
     */
    public void flagPartitionedColumn(Column column, PartitionedColumnType type) throws NullPointerException {
        // Sanity check.
        if (column == null)
            throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
        if (type == null)
            throw new NullPointerException(BuilderBundle.getString("partitionTypeIsNull"));
        // Do it (the Map will replace the value with the new value if the key already exists)
        this.partitionedColumns.put(column, type);
    }
    
    /**
     * Unmark a {@link Column} as partitioned. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param column the {@link Column} to unmark.
     * @throws NullPointerException if the {@link Column} is null.
     */
    public void unflagPartitionedColumn(Column column) throws NullPointerException {
        // Sanity check.
        if (column == null)
            throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
        // Do it.
        this.partitionedColumns.remove(column);
    }
    
    /**
     * Return the set of partitioned {@link Column}s. It may be empty, but never null.
     * @return the set of partitioned {@link Column}s.
     */
    public Collection getPartitionedColumns() {
        return this.partitionedColumns.keySet();
    }
    
    /**
     * Return the partition type of partitioned {@link Column} column.
     * It will return null if there is no such partitioned column.
     * @param column the {@link Column} to check the partitioning type for.
     * @return the partition type of the {@link Column}.
     * @throws NullPointerException if the parameter passed in was null.
     */
    public PartitionedColumnType getPartitionedColumnType(Column column) throws NullPointerException {
        // Sanity check.
        if (column == null)
            throw new NullPointerException(BuilderBundle.getString("columnIsNull"));
        // Do we have it?
        if (!this.partitionedColumns.containsKey(column)) return null;
        // Return it.
        return (PartitionedColumnType)this.partitionedColumns.get(column);
    }
    
    /**
     * Mark a {@link Relation} as concat-only. If it is already marked, it updates the concat type.
     * An exception will be thrown if any parameter is null.
     *
     * @param relation the {@link Relation} to mark as concat-only.
     * @param type the {@link ConcatRelationType} to use for the relation.
     * @throws NullPointerException if either parameter is null.
     */
    public void flagConcatOnlyRelation(Relation relation, ConcatRelationType type) throws NullPointerException {
        // Sanity check.
        if (relation == null)
            throw new NullPointerException(BuilderBundle.getString("relationIsNull"));
        if (type == null)
            throw new NullPointerException(BuilderBundle.getString("concatRelationTypeIsNull"));
        // Do it (the Map will replace the value with the new value if the key already exists)
        this.concatOnlyRelations.put(relation, type);
    }
    
    /**
     * Unmark a {@link Relation} as concat-only. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param relation the {@link Relation} to unmark.
     * @throws NullPointerException if the {@link Relation} is null.
     */
    public void unflagConcatOnlyRelation(Relation relation) throws NullPointerException {
        // Sanity check.
        if (relation == null)
            throw new NullPointerException(BuilderBundle.getString("relationIsNull"));
        // Do it.
        this.concatOnlyRelations.remove(relation);
    }
    
    /**
     * Return the set of concat-only {@link Relation}s. It may be empty, but never null.
     * @return the set of concat-only {@link Relation}s.
     */
    public Collection getConcatOnlyRelations() {
        return this.concatOnlyRelations.keySet();
    }
    
    /**
     * Return the concat type of concat-only {@link Relation} relation.
     * It will return null if there is no such concat-only relation.
     * @param relation the {@link Relation} to return the concat type for.
     * @return the concat type of the {@link Relation}.
     * @throws NullPointerException if the parameter passed in was null.
     */
    public ConcatRelationType getConcatRelationType(Relation relation) throws NullPointerException {
        // Sanity check.
        if (relation == null)
            throw new NullPointerException(BuilderBundle.getString("relationIsNull"));
        // Do we have it?
        if (!this.concatOnlyRelations.containsKey(relation)) return null;
        // Return it.
        return (ConcatRelationType)this.concatOnlyRelations.get(relation);
    }
    
    /**
     * If the user wishes to partition the main table by the table provider
     * (only possible if the main table is from a {@link PartitionedTableProvider}) then set
     * this flag to true. Otherwise, set it to false, which is its default value.
     * @param partitionOnTableProvider true if you want to turn this on, false if you want to turn it off.
     */
    public void setPartitionOnTableProvider(boolean partitionOnTableProvider) {
        this.partitionOnTableProvider = partitionOnTableProvider;
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
        for (Iterator i = this.getSchema().getTableProviders().iterator(); i.hasNext(); ) {
            TableProvider tp = (TableProvider)i.next();
            for (Iterator j = tp.getTables().iterator(); j.hasNext(); ) {
                Table t = (Table)j.next();
                deadColumns.removeAll(t.getColumns());
                // Only need primary key as all relations involve primary keys at some point.
                Key pk = t.getPrimaryKey();
                if (pk != null) deadRelations.removeAll(pk.getRelations());
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
        Window w = (Window)o;
        return this.toString().compareTo(w.toString());
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Window)) return false;
        Window w = (Window)o;
        return w.toString().equals(this.toString());
    }
    
    /**
     * Represents a method of partitioning by column. There are no methods.
     * Actual logic to divide up by column is left to the DDL generator elsewhere
     * to decide by looking at the class used.
     */
    public interface PartitionedColumnType {
        /**
         * Use this class to refer to a column partitioned by every unique value.
         */
        public class UniqueValues implements PartitionedColumnType {
            /**
             * {@inheritDoc}
             */
            public String toString() {
                return "UniqueValues";
            }
        }
        
        /**
         * Use this class to partition on a set of values - ie. only columns with
         * one of these values will be returned.
         */
        public class ValueCollection implements PartitionedColumnType {
            /**
             * Internal reference to the values to select rows on.
             */
            private final Set values = new HashSet();
            
            /**
             * The constructor specifies the values to partition on. If any value is null,
             * then only rows with null in this column will be selected for that value.
             * Duplicate values will be ignored.
             * @param values the set of unique values to partition on.
             * @throws IllegalArgumentException if any of the values are not-null
             * and not Strings, or if the input set is empty.
             * @throws NullPointerException if the input set is null.
             */
            public ValueCollection(Collection values) throws IllegalArgumentException, NullPointerException {
                // Sanity check.
                if (values==null)
                    throw new NullPointerException(BuilderBundle.getString("valuesIsNull"));
                if (values.size()<1)
                    throw new IllegalArgumentException(BuilderBundle.getString("valuesEmpty"));
                // Do it.
                for (Iterator i = values.iterator(); i.hasNext(); ) {
                    Object o = i.next();
                    // Sanity check.
                    if (o != null && !(o instanceof String))
                        throw new IllegalArgumentException(BuilderBundle.getString("valueNotString"));
                    // Add the value.
                    this.values.add((String)o);
                }
            }
            
            /**
             * Returns the values we will partition on. May be empty but never null.
             * @return the values we will partition on.
             */
            public Set getValues() {
                return this.values;
            }
            
            /**
             * {@inheritDoc}
             */
            public String toString() {
                return "ValueCollection:" + this.values.toString();
            }
        }
        
        /**
         * Use this class to partition on a single value - ie. only rows matching this
         * value will be returned.
         */
        public class SingleValue extends ValueCollection {
            /**
             * Internal reference to the single value to select rows on.
             */
            private String value;
            
            /**
             * The constructor specifies the value to partition on. If the value is null,
             * then only rows with null in this column will be selected.
             * @param value the value to partition on.
             */
            public SingleValue(String value) {
                super(Collections.singleton(value));
                this.value = value;
            }
            
            /**
             * Returns the value we will partition on.
             * @return the value we will partition on.
             */
            public String getValue() {
                return this.value;
            }
            
            /**
             * {@inheritDoc}
             */
            public String toString() {
                return "SingleValue:" + this.value;
            }
        }
    }
    
    /**
     * Represents a method of concatenating values in a key referenced by a
     * concat-only {@link Relation}. It simply represents the separator to use.
     */
    public static class ConcatRelationType implements Comparable {
        
        /**
         * Internal reference to the set of {@link ConcatRelationType} singletons.
         */
        private static final Map singletons = new HashMap();
        /**
         * Use this constant to refer to value-separation by commas.
         */
        public static final ConcatRelationType COMMA = new ConcatRelationType("COMMA", ",");
        
        /**
         * Use this constant to refer to value-separation by spaces.
         */
        public static final ConcatRelationType SPACE = new ConcatRelationType("SPACE", " ");
        
        /**
         * Use this constant to refer to value-separation by tabs.
         */
        public static final ConcatRelationType TAB = new ConcatRelationType("TAB", "\t");
        
        /**
         * Internal reference to the name of this {@link ConcatRelationType}.
         */
        private final String name;
        
        /**
         * Internal reference to the separator for this {@link ConcatRelationType}.
         */
        private final String separator;
        
        /**
         * The private constructor takes two parameters, which define the name
         * this {@link ConcatRelationType} object will display when printed, and the
         * separator to use between values that have been concatenated.
         * @param name the name of the {@link ConcatRelationType}.
         * @param separator the separator for this {@link ConcatRelationType}.
         */
        private ConcatRelationType(String name, String separator) {
            this.name = name;
            this.separator = separator;
        }
        
        /**
         * Displays the name of this {@link ConcatRelationType} object.
         * @return the name of this {@link ConcatRelationType} object.
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * Displays the separator for this {@link ConcatRelationType} object.
         * @return the separator for this {@link ConcatRelationType} object.
         */
        public String getSeparator() {
            return this.separator;
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
            ConcatRelationType pct = (ConcatRelationType)o;
            return this.toString().compareTo(pct.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            // We are dealing with singletons so can use == happily.
            return o == this;
        }
    }
}
