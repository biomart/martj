/*
 * DataSet.java
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Schema.GenericSchema;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>Represents a window onto a {@link Table} in a {@link Schema}.
 * The window allows masking of unwanted {@link Relation}s and {@link Column}s.
 * It will automatically mask out permanently any {@link Relation}s forming more than
 * two levels of 1:m from the central table, and will flag any forming more than one such
 * level as 'Concat only'. 'Concat only' means that the remote primary key will be merged
 * as a single column with all unique values concatenated when followed in the 1:M
 * direction, but this is ignored in the M:1 direction. Additional relations beyond a
 * concat relation should be ignored when building the final mart.</p>
 * <p>The name of the window is inherited by the {@link Dataset} so take care when
 * choosing it.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.13, 10th May 2006
 * @since 0.1
 */
public class DataSet extends GenericSchema {
    /**
     * Internal reference to the parent mart.
     */
    private final Mart mart;
    
    /**
     * Internal reference to the central table.
     */
    private final Table centralTable;
    
    /**
     * Internal reference to masked relations.
     */
    private final List maskedRelations = new ArrayList();
    
    /**
     * Internal reference to masked columns.
     */
    private final List maskedColumns = new ArrayList();
    
    /**
     * Internal reference to partitioned columns (keys are columns,
     * values are {@link PartitionedColumnType}s).
     */
    private final Map partitionedColumns = new HashMap();
    
    /**
     * Internal reference to relations between subclassed tables.
     */
    private final List subclassedRelations = new ArrayList();
    
    /**
     * Internal reference to concat-only relations. The keys of
     * the map are relations, the values are concat types.
     */
    private final Map concatOnlyRelations = new HashMap();
    
    /**
     * Internal reference to the relations already seen when doing the prediction walk.
     */
    private Map relationsPredicted = new HashMap();
    
    /**
     * Internal reference to the mart constructor that will build us later.
     */
    private MartConstructor martConstructor;
    
    /**
     * Internal reference to the dataset optimiser that will optimise us later.
     * Defaults to none.
     */
    private DataSetOptimiserType optimiser = DataSetOptimiserType.NONE;
    
    /**
     * The constructor creates a {@link DataSet} around one central {@link Table} and
     * gives it a name. It also initiates a {@link OLDDataSet} ready to contain the transformed
     * results. It adds itself to the specified mart automatically.
     *
     * @param mart the {@link SMart this {@link DataSet} will belong to.
     * @param centralTable the {@link Table} to use as the central centralTable.
     * @param name the name to give this {@link DataSet}.
     * @throws AssociationException if the {@link Table} does not belong to any of the
     * {@link Schema} objects in the {@link SchemMarts {@link DataSet} belongs to.
     * @throws AlreadyExistsException if another {@link DataSet} with exactly the same name already exists
     * in the specified mart.
     */
    public DataSet(Mart mart, Table centralTable, String name) throws AssociationException, AlreadyExistsException {
        // Super first.
        super(name);
        // Sanity check.
        if (!mart.getSchemas().contains(centralTable.getSchema()))
            throw new AssociationException(BuilderBundle.getString("tableMartMismatch"));
        // Do it.
        this.mart = mart;
        this.centralTable = centralTable;
        this.setMartConstructor(MartConstructor.DUMMY_MART_CONSTRUCTOR);
        // Add ourselves.
        mart.addDataSet(this);
    }
    
    /**
     * This method identifies all candidate concat relations, and masks out all relations
     * more than 2 1:M relations away from the main or subclass table.
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void optimiseDataSet() throws SQLException, BuilderException {
        // Clear out our previous predictions.
        this.relationsPredicted.clear();
        this.maskedRelations.clear();
        this.concatOnlyRelations.clear();
        
        // Find the shortest 1:m paths (depths) of each relation we have.
        this.walkRelations(this.getCentralTable(), 0);
        
        // Regenerate the dataset.
        this.regenerate();
    }
    
    /**
     * Internal method which works out the lowest number of 1:M relations between
     * the currentTable table and all other {@link Table}s linked by as-yet-unvisited {@link Relation}s.
     * @param currentTable the {@link Table} to start walking from.
     * @param currentDepth the number of 1:M relations it took to get this far.
     */
    private void walkRelations(Table currentTable, int currentDepth) {
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
            // Update the flags.
            if (currentDepth >= 2) {
                // Mask all relations that involve two or more levels of 1:m abstraction away
                // from the central main table.
                this.maskRelation(r);
            } else {
                this.unmaskRelation(r);
            }
            // Work out where to go next.
            if (r.getPrimaryKey().getTable().equals(currentTable) && (r.getFKCardinality().equals(Cardinality.MANY))) {
                // If currentTable is at the one end of a one-to-many relation, then
                // up the count (if it's not a subclass relation) and recurse down it.
                if (currentDepth >= 1) {
                    // Mark as concat-only (default to comma-separation) all 1:m relations that involve
                    // a second or further level of 1:m abstraction away from the central main table.
                    this.flagConcatOnlyRelation(r, ConcatRelationType.COMMA);
                }
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
     * Returns the {@link Mart} of this {@link DataSet}.
     *
     * @return the {@link Mart} of this {@link DataSet}.
     */
    public Mart getMart() {
        return this.mart;
    }
    
    /**
     * Returns the central {@link Table} of this {@link DataSet}.
     *
     * @return the central {@link Table} of this {@link DataSet}.
     */
    public Table getCentralTable() {
        return this.centralTable;
    }
    
    /**
     * Mask a {@link Relation}. If it is already masked, ignore it.
     * An exception will be thrown if it is null.
     * @param relation the {@link Relation} to mask.
     */
    public void maskRelation(Relation relation) {
        // Do it.
        this.maskedRelations.add(relation);
    }
    
    /**
     * Unmask a {@link Relation}. If it is already unmasked, ignore it.
     * An exception will be thrown if it is null.
     * @param relation the {@link Relation} to unmask.
     */
    public void unmaskRelation(Relation relation) {
        // Do it.
        this.maskedColumns.removeAll(relation.getPrimaryKey().getColumns());
        this.maskedColumns.removeAll(relation.getForeignKey().getColumns());
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
     */
    public void maskColumn(Column column) throws AssociationException {
        if (column instanceof DataSetColumn) {
            DataSetColumn dsColumn = (DataSetColumn)column;
            DataSetTable dsTable = (DataSetTable)dsColumn.getTable();
            Table centralTable = ((DataSet)dsTable.getSchema()).getCentralTable();
            List okToMask = new ArrayList(dsTable.getColumns());
            // Can mask PK cols only if not from original central table
            Key dsTablePK = dsTable.getPrimaryKey();
            if (dsTablePK!=null) {
                for (Iterator i = dsTablePK.getColumns().iterator(); i.hasNext(); ) {
                    DataSetColumn col = (DataSetColumn)i.next();
                    Table underlyingTable = ((DataSetTable)col.getTable()).getUnderlyingTable();
                    if (underlyingTable!=null && underlyingTable.equals(centralTable)) okToMask.remove(col);
                }
            }
            // Can't mask any FK cols.
            for (Iterator i = dsTable.getForeignKeys().iterator(); i.hasNext(); )
                okToMask.removeAll(((Key)i.next()).getColumns());
            // Refuse to mask necessary columns
            if (!okToMask.contains(dsColumn))
                throw new AssociationException(BuilderBundle.getString("cannotMaskNecessaryColumn"));
            // Refuse to mask schema name columns.
            else if (dsColumn instanceof SchemaNameColumn)
                throw new AssociationException(BuilderBundle.getString("cannotMaskSchemaNameColumn"));
            // If wrapped, mask wrapped column
            else if (dsColumn instanceof WrappedColumn)
                this.maskColumn(((WrappedColumn)dsColumn).getWrappedColumn());
            // If concat-only, mask concat-only part
            else if (dsColumn instanceof ConcatRelationColumn)
                this.maskRelation(dsColumn.getUnderlyingRelation());
            // Eh?
            else
                throw new AssertionError(BuilderBundle.getString("cannotMaskUnknownColumn"));
        } else {
            // Do it.
            this.maskedColumns.add(column);
            // Mask the associated relations.
            for (Iterator i = column.getTable().getKeys().iterator(); i.hasNext(); ) {
                Key k = (Key)i.next();
                if (k.getColumns().contains(column)) for (Iterator j = k.getRelations().iterator(); j.hasNext(); ) {
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
     */
    public void unmaskColumn(Column column) {
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
     * is the parent table, but the parent table may not actually be the central table in this {@link DataSet}.
     * If it is already marked, ignore it. As subclasses can only apply to the central table, throw an AssociationException
     * if this is attempted on any table other than the central table. An exception will be thrown if any parameter is null.</p>
     * <p>One further restriction is that a {@link Table} can only have a single M:1 subclass {@link Relation},
     * or multiple 1:M ones. It cannot have a mix of both, nor can it have more than one M:1 subclass {@link Relation}.
     * In either case if this is attempted an AssociationException will be thrown.</p>
     *
     * @param relation the {@link Relation} to mark as a relation relation.
     * @throws AssociationException if one end of the {@link Relation} is not the central table for this window, or
     * if both ends of the {@link Relation} point to the same table.
     */
    public void flagSubclassRelation(Relation relation) throws AssociationException {
        // Sanity check.
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
     */
    public void unflagSubclassRelation(Relation relation) {
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
     */
    public void flagPartitionedColumn(Column column, PartitionedColumnType type) {
        // Do it (the Map will replace the value with the new value if the key already exists)
        this.partitionedColumns.put(column, type);
    }
    
    /**
     * Unmark a {@link Column} as partitioned. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param column the {@link Column} to unmark.
     */
    public void unflagPartitionedColumn(Column column) {
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
     */
    public PartitionedColumnType getPartitionedColumnType(Column column) {
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
     */
    public void flagConcatOnlyRelation(Relation relation, ConcatRelationType type) {
        // Do it (the Map will replace the value with the new value if the key already exists)
        this.concatOnlyRelations.put(relation, type);
    }
    
    /**
     * Unmark a {@link Relation} as concat-only. If it is already unmarked, ignore it.
     * An exception will be thrown if it is null.
     * @param relation the {@link Relation} to unmark.
     */
    public void unflagConcatOnlyRelation(Relation relation) {
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
     */
    public ConcatRelationType getConcatRelationType(Relation relation) {
        // Do we have it?
        if (!this.concatOnlyRelations.containsKey(relation)) return null;
        // Return it.
        return (ConcatRelationType)this.concatOnlyRelations.get(relation);
    }
    
    /**
     * Synchronise this {@link DataSet} with the {@link Schema} that is
     * providing its tables. Synchronisation means checking the masked {@link Column}
     * and {@link Relation} objects and removing any that have disappeared.
     * The associated {@link OLDDataSet}, if one exists yet, is then regenerated.
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
        // Un-dead all columns and relations in the mart
        for (Iterator i = this.getMart().getSchemas().iterator(); i.hasNext(); ) {
            Schema tp = (Schema)i.next();
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
        this.regenerate();
    }
    
    /**
     * {@inheritDoc}
     */
    public void regenerate() throws SQLException, BuilderException {
        // Clear our set of tables.
        this.tables.clear();
        
        // Set up maps of interesting relations
        // ignored relations - keys are tables, values are sets of relations
        Map ignoredRelations = new HashMap();
        // subclass and dimension relations - a simple set for each
        Set dimensionRelations = new HashSet();
        Set subclassRelations = new HashSet();
        
        // Identify main table.
        Table centralTable = this.getCentralTable();
        // if central table has subclass relations and is at the foreign key end, then follow
        // them to the real central table.
        boolean found = false;
        for (Iterator i = centralTable.getForeignKeys().iterator(); i.hasNext() && !found; ) {
            Key k = (Key)i.next();
            for (Iterator j = k.getRelations().iterator(); j.hasNext() && !found; ) {
                Relation r = (Relation)j.next();
                if (this.getSubclassedRelations().contains(r)) {
                    centralTable = r.getPrimaryKey().getTable();
                    found = true;
                }
            }
        }
        
        // Identify all subclass and dimension relations.
        ignoredRelations.put(centralTable, new HashSet(this.getMaskedRelations()));
        if (centralTable.getPrimaryKey()!=null) for (Iterator i = centralTable.getPrimaryKey().getRelations().iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            // Skip masked and concat-only relations.
            if (this.getMaskedRelations().contains(r) || this.getConcatOnlyRelations().contains(r)) continue;
            // Skip inferred-incorrect relations.
            if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) continue;
            // See what kind of relation we have.
            if (this.getSubclassedRelations().contains(r)) {
                // Subclass relation from main table? Ignore it at main table but add to subclass set.
                ((Set)ignoredRelations.get(centralTable)).add(r);
                subclassRelations.add(r);
                Table subclassTable = r.getForeignKey().getTable();
                ignoredRelations.put(subclassTable, new HashSet(this.getMaskedRelations()));
                // Mark all OneToManys from the subclass table as dimensions.
                if (subclassTable.getPrimaryKey()!=null) for (Iterator j = subclassTable.getPrimaryKey().getRelations().iterator(); j.hasNext(); ) {
                    Relation sr = (Relation)j.next();
                    // Skip masked and concat-only relations.
                    if (this.getMaskedRelations().contains(sr) || this.getConcatOnlyRelations().contains(sr)) continue;
                    // Skip inferred-incorrect relations.
                    if (sr.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) continue;
                    // OneToMany from subclass table? Ignore it at subclass table but add to dimension set.
                    // Also add the relation to the ignore set of the dimension.
                    if (sr.getFKCardinality().equals(Cardinality.MANY)) {
                        ((Set)ignoredRelations.get(subclassTable)).add(sr);
                        dimensionRelations.add(sr);
                        Table dimTable = sr.getForeignKey().getTable();
                        if (!ignoredRelations.containsKey(dimTable)) ignoredRelations.put(dimTable, new HashSet(this.getMaskedRelations()));
                        ((Set)ignoredRelations.get(dimTable)).add(sr);
                    }
                }
            } else if (r.getFKCardinality().equals(Cardinality.MANY)) {
                // OneToMany from main table? Ignore it at main table but add to dimension set.
                // Also add the relation to the ignore set of the dimension.
                // Note that subclass OneToMany will already have been picked up by previous test.
                ((Set)ignoredRelations.get(centralTable)).add(r);
                dimensionRelations.add(r);
                Table dimTable = r.getForeignKey().getTable();
                if (!ignoredRelations.containsKey(dimTable)) ignoredRelations.put(dimTable, new HashSet(this.getMaskedRelations()));
                ((Set)ignoredRelations.get(dimTable)).add(r);
            }
        }
        // Set up the ignorable relations for subclass tables.
        for (Iterator i = subclassRelations.iterator(); i.hasNext(); ) {
            Relation subclassRel = (Relation)i.next();
            for (Iterator j = subclassRelations.iterator(); j.hasNext(); ) {
                Relation otherSubclassRel = (Relation)j.next();
                if (!subclassRel.equals(otherSubclassRel))
                    ((Set)ignoredRelations.get(subclassRel.getPrimaryKey().getTable())).add(otherSubclassRel);
            }
        }
        
        // Build the main table.
        this.constructTable(DataSetTableType.MAIN, centralTable, (Set)ignoredRelations.get(centralTable), null);
        
        // Build the subclass tables.
        for (Iterator i = subclassRelations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            Table sct = r.getForeignKey().getTable();
            DataSetTable subclass = this.constructTable(DataSetTableType.MAIN_SUBCLASS, sct, (Set)ignoredRelations.get(sct), r);
        }
        
        // Build the dimension tables.
        for (Iterator i = dimensionRelations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            Table dt = r.getForeignKey().getTable();
            DataSetTable dim = this.constructTable(DataSetTableType.DIMENSION, dt, (Set)ignoredRelations.get(dt), r);
        }
    }
    
    /**
     * Internal function that constructs a data set table based on a real table, and the relations
     * linking to/from it. If a linkback relation is supplied between the real table and some parent real table,
     * then a foreign key is created on the data set table linking it to the primary key of the dataset equivalent
     * of the real parent table in the relation.
     * @param dsTableType the dsTableType of table to create.
     * @param realTable the original table to transform.
     * @param ignoredRelations the set of relations to ignore during transformation. If null, taken as empty.
     * @param linkbackRelation the relation between the realTable and the real parent table to link to if required.
     * Null indicates no linking required. Linking takes place to the dataset version of the real parent table.
     * @return a fully constructed and keyed up data set table.
     * @throws BuilderException if there was any trouble.
     */
    private DataSetTable constructTable(DataSetTableType dsTableType, Table realTable, Set ignoredRelations, Relation linkbackRelation) throws BuilderException {
        // Sensible defaults.
        if (ignoredRelations == null)
            ignoredRelations = new HashSet();
        
        // Do it!
        List relationsFollowed = new ArrayList(); // relations used to construct this table
        if (linkbackRelation != null) relationsFollowed.add(linkbackRelation); // don't forget the parent relation
        Set constructedColumns = new HashSet(); // constructedColumns to include in the constructed table
        List constructedPKColumns = new ArrayList(); // constructedColumns to include in the constructed table's PK
        
        // Create the DataSetTable
        DataSetTable datasetTable = new DataSetTable(realTable.getName(), this, dsTableType, realTable);
        this.transformTable(datasetTable, realTable, ignoredRelations, relationsFollowed, constructedPKColumns, constructedColumns);
        datasetTable.setUnderlyingRelations(relationsFollowed);
        
        // Add table provider partition column if required.
        DataSetColumn schemaCol=null;
        if (realTable.getSchema() instanceof SchemaGroup) {
            schemaCol = new SchemaNameColumn(BuilderBundle.getString("schemaColumnName"), datasetTable);
            // Only add to PK if PK is not empty, as otherwise will introduce a PK where none previously existed.
            if (!constructedPKColumns.isEmpty()) constructedPKColumns.add(schemaCol);
        }
        
        // Work out the foreign key only if the parent relation is not null.
        if (linkbackRelation != null) {
            // Create new, empty child FK list.
            List constructedFKColumns = new ArrayList();
            
            // Get real table for dataset parent table
            Table parentRealTable = linkbackRelation.getPrimaryKey().getTable();
            DataSetTable parentDatasetTable = (DataSetTable)this.getTableByName(parentRealTable.getName());
            
            // For each column in dataset parent table PK
            for (Iterator i = parentDatasetTable.getPrimaryKey().getColumns().iterator(); i.hasNext(); ) {
                DataSetColumn parentDatasetTableColumn = (DataSetColumn)i.next();
                DataSetColumn constructedFKColumn;
                
                if (parentDatasetTableColumn instanceof WrappedColumn) {
                    // If column is a wrapped column
                    // Find associated real column
                    Column parentRealTableColumn = ((WrappedColumn)parentDatasetTableColumn).getWrappedColumn();
                    if (parentDatasetTableColumn.getUnderlyingRelation()!=null) {
                        // If real column not obtained directly from original main table, ie. is from another table or a copy
                        // of the main table obtained by linking to itself via some other route, create child column for it
                        // add it to child table PK. We test for where the column came from by looking at its providing
                        // relation - if null, then it was on the original table, if not null then it is a column inherited from
                        // the parent of the subclass or dimension table. So, we look the column up by name to see if it is
                        // already defined. If it is, we reuse it. If not, we create a new wrapped column for it.
                        constructedFKColumn = (DataSetColumn)datasetTable.getColumnByName(parentDatasetTableColumn.getName());
                        if (constructedFKColumn == null) {
                            constructedFKColumn = new WrappedColumn(parentRealTableColumn, datasetTable, linkbackRelation);
                            constructedPKColumns.add(constructedFKColumn);
                        }
                    } else {
                        // Else follow original relation FK end and find real child column and associated child column
                        int parentRealPKColPos = parentRealTable.getPrimaryKey().getColumns().indexOf(parentRealTableColumn);
                        Column childRealFKCol = (Column)linkbackRelation.getForeignKey().getColumns().get(parentRealPKColPos);
                        constructedFKColumn = (DataSetColumn)datasetTable.getColumnByName(childRealFKCol.getName());
                    }
                } else if (parentDatasetTableColumn instanceof SchemaNameColumn) {
                    // Else if its a tblprov column
                    // Find child tblprov column
                    constructedFKColumn = schemaCol;
                } else {
                    // Else, can'table handle.
                    throw new AssertionError(BuilderBundle.getString("unknownColumnTypeInFK", parentDatasetTableColumn.getClass().getName()));
                }
                
                // Set the FK column name to match the parent PK column name with "_key" appended
                // (naming convention requirement), but only append that extra bit if parent doesn't already have it.
                String candidateFKColName = parentDatasetTableColumn.getName();
                if (!candidateFKColName.endsWith(BuilderBundle.getString("fkSuffix"))) candidateFKColName += BuilderBundle.getString("fkSuffix");
                constructedFKColumn.setName(candidateFKColName);
                
                // Add child column to child FK
                constructedFKColumns.add(constructedFKColumn);
            }
            
            // Create the foreign key.
            if (constructedFKColumns.size()>=1) {
                ForeignKey newFK = new GenericForeignKey(constructedFKColumns);
                datasetTable.addForeignKey(newFK);
                // Create the relation.
                new GenericRelation(parentDatasetTable.getPrimaryKey(), newFK, Cardinality.MANY);
            } else throw new AssertionError(BuilderBundle.getString("emptyFK"));
        }
        
        // Create the primary key on it.
        if (constructedPKColumns.size()>=1) datasetTable.setPrimaryKey(new GenericPrimaryKey(constructedPKColumns));
        
        // Return.
        return datasetTable;
    }
    
    /**
     * Simple recursive internal function that walks through relations, either noting that its seen them,
     * marking them as concat relations, and adding their columns to the table.
     * @param datasetTable the table to construct as we go along.
     * @param realTable the table we are adding columns to datasetTable from next.
     * @param ignoredRelations relations to ignore.
     * @param relationsFollowed the relations used as we went along.
     * @param constructedPKColumns the primary key columns to be added to the new table.
     * @param constructedColumns the columns to be added to the new table.
     * @throws AlreadyExistsException if any attempt to add a column resulted in a duplicate name.
     */
    private void transformTable(DataSetTable datasetTable, Table realTable, Set ignoredRelations, List relationsFollowed, List constructedPKColumns, Set constructedColumns) throws AlreadyExistsException {
        // Find all relations from source table.
        Collection realTableRelations = realTable.getRelations();
        
        // Find all columns in all keys with non-ignored non-concat-only relations. Exclude them.
        List excludedColumns = new ArrayList();
        for (Iterator i = realTable.getKeys().iterator(); i.hasNext(); ) {
            Key k = (Key)i.next();
            boolean hasUnignoredRelation = false;
            for (Iterator j = k.getRelations().iterator(); j.hasNext() && !hasUnignoredRelation; ) {
                Relation r = (Relation)j.next();
                if (!r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT) && 
                        !ignoredRelations.contains(r) &&
                        !this.getConcatOnlyRelations().contains(r)) { 
                    hasUnignoredRelation = true;
                }
            }
            if (hasUnignoredRelation) excludedColumns.addAll(k.getColumns());
        }
        
        // Also exclude all masked columns.
        excludedColumns.addAll(this.getMaskedColumns());
        
        // Work out the underlying relation (the last one visited) for all columns on this table.
        Relation underlyingRelation = null;
        if (relationsFollowed.size()>0) underlyingRelation = (Relation)relationsFollowed.get(relationsFollowed.size()-1);
        
        // Add all non-excluded columns from source table to dataset table.
        for (Iterator i = realTable.getColumns().iterator(); i.hasNext(); ) {
            Column c = (Column)i.next();
            // If column is excluded, ignore it.
            if (excludedColumns.contains(c)) continue;
            // Otherwise add the column to our table.
            Column wc = new WrappedColumn(c, datasetTable, underlyingRelation);
            // If column is part of primary key, add the wrapped version to the primary key column set.
            if (realTable.getPrimaryKey()!=null && realTable.getPrimaryKey().getColumns().contains(c)) constructedPKColumns.add(wc);
        }
        
        // For all non-ignored rels in realTable
        for (Iterator i = realTableRelations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            
            // Need to recheck ignoreRefs in case has become ignored since we built our set of relations.
            if (ignoredRelations.contains(r)) continue;
            // Ignore incorrect relations too.
            if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) continue;
            
            // Find key in relation that refers to this table.
            Key realTableRelSourceKey = r.getPrimaryKey();
            Table realTableRelTargetTable = r.getForeignKey().getTable();
            if (!realTableRelSourceKey.getTable().equals(realTable)) {
                realTableRelSourceKey = r.getForeignKey();
                realTableRelTargetTable = r.getPrimaryKey().getTable();
            }
            
            // Add relation to path followed so far.
            relationsFollowed.add(r);
            // Add to ignore so don'table revisit later
            ignoredRelations.add(r);
            
            if (realTableRelSourceKey instanceof PrimaryKey && this.getConcatOnlyRelations().contains(r)) {
                // If concat-only and realTable is at primary key end of relation, concat it
                try {
                    new ConcatRelationColumn(BuilderBundle.getString("concatColumnPrefix") + realTableRelTargetTable.getName(), datasetTable, r);
                } catch (AssociationException e) {
                    AssertionError ae = new AssertionError(BuilderBundle.getString("tableMismatch"));
                    ae.initCause(e);
                    throw ae;
                }
            } else {
                // Otherwise, recurse down to target table.
                this.transformTable(datasetTable, realTableRelTargetTable, ignoredRelations, relationsFollowed, constructedPKColumns, constructedColumns);
            }
        }
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
        DataSet w = (DataSet)o;
        return this.toString().compareTo(w.toString());
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DataSet)) return false;
        DataSet w = (DataSet)o;
        return w.toString().equals(this.toString());
    }
    
    /**
     * {@inheritDoc}
     */
    public void setMartConstructor(MartConstructor martConstructor) {
        // Do it.
        this.martConstructor = martConstructor;
    }
    
    /**
     * {@inheritDoc}
     */
    public MartConstructor getMartConstructor() {
        return this.martConstructor;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDataSetOptimiserType(DataSetOptimiserType optimiser) {
        // Do it.
        this.optimiser = optimiser;
    }
    
    /**
     * {@inheritDoc}
     */
    public DataSetOptimiserType getDataSetOptimiserType() {
        return this.optimiser;
    }
    
    /**
     * {@inheritDoc}
     */
    public void constructMart() throws BuilderException, SQLException {
        // Sanity check.
        if (this.martConstructor == null)
            throw new BuilderException(BuilderBundle.getString("martConstructorMissing"));
        // Do it.
        this.martConstructor.constructMart(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public void addTable(Table table) throws AlreadyExistsException, AssociationException {
        // Sanity check.
        if (!(table instanceof DataSetTable))
            throw new AssociationException(BuilderBundle.getString("tableSchemaMismatch"));
        // Do it.
        super.addTable(table);
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
             */
            public ValueCollection(Collection values) throws IllegalArgumentException {
                // Sanity check.
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
    
    /**
     * This special {@link Table} represents the merge of one or more other {@link Table}s
     * by following a series of {@link Relation}s. As such it has no real columns of its own,
     * so every column is from another table and is given an alias.
     */
    public static class DataSetTable extends GenericTable {
        /**
         * Internal reference to the list of {@link Relation}s to follow to construct this table.
         * The list contains alternate references to Key (first) then Relation (second), in pairs,
         * until the final pair. The key specifies which end of the relation to start from.
         */
        private final List underlyingRelations = new ArrayList();
        
        /**
         * Internal reference to the type of this table.
         */
        private final DataSetTableType type;
        
        private final Table underlyingTable;
        
        /**
         * The constructor calls the parent {@link GenericTable} constructor. It uses a
         * {@link DataSetTableProvider} as a parent for itself. You must also supply a type that
         * describes this as a main table, dimension table, etc.
         *
         * @param name the table name.
         * @param ds the {@link OLDDataSet} to hold this table in.
         * @param type the {@link DataSetTableType} that best describes this table.
         * @throws AlreadyExistsException if the provider, for whatever reason, refuses to
         * allow this {@link Table} to be added to it using {@link DataSetTableProvider#addTable(Table)}.
         */
        public DataSetTable(String name, DataSet ds, DataSetTableType type, Table underlyingTable) throws AlreadyExistsException {
            // Super call first.
            super(generateAlias(name, ds), ds);
            // Do the work.
            this.underlyingTable = underlyingTable;
            this.type = type;
        }
        
        public Table getUnderlyingTable() {
            return this.underlyingTable;
        }
        
        /**
         * Internal method that generates a safe alias/name for a table. The first try is
         * always the original table name, followed by attempts with an underscore and a
         * sequence number appended.
         *
         * @param name the first name to try.
         * @param ds the {@link OLDDataSet} that this table is being added to.
         * @return the result.
         */
        protected static String generateAlias(String name, DataSet ds) {
            // Do it.
            String alias = name;
            int aliasNumber = 2;
            while (ds.getTableByName(alias)!=null) {
                // Alias is original name appended with _2, _3, _4 etc.
                alias = name + "_" + (aliasNumber++);
            }
            return alias;
        }
        
        /**
         * Returns the type of this table specified at construction time.
         * @return the type of this table.
         */
        public DataSetTableType getType() {
            return this.type;
        }
        
        /**
         * Sets the list of relations used to construct this table.
         * @param relations the list of relations of this table. May be empty but never null.
         * @throws IllegalArgumentException if the list wasn'table a list of Key/Relation pairs.
         */
        public void setUnderlyingRelations(List relations) throws IllegalArgumentException {
            // Check the relations and save them.
            this.underlyingRelations.clear();
            for (Iterator i = relations.iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o == null) continue; // Skip nulls.
                if (!(o instanceof Relation))
                    throw new IllegalArgumentException(BuilderBundle.getString("relationNotRelation"));
                this.underlyingRelations.add((Relation)o);
            }
        }
        
        /**
         * Returns the list of relations used to construct this table.
         * @return the list of relations of this table. May be empty but never null.
         */
        public List getUnderlyingRelations() {
            return this.underlyingRelations;
        }
        
        /**
         * {@inheritDoc}
         * <p>{@link DataSetTable}s insist that all columns are {@link DataSetColumn}s.</p>
         */
        public void addColumn(Column column) throws AlreadyExistsException, AssociationException {
            // Sanity check.
            if (!(column instanceof DataSetColumn))
                throw new AssociationException(BuilderBundle.getString("columnNotDatasetColumn"));
            // Do the work.
            super.addColumn(column);
        }
        
        /**
         * {@inheritDoc}
         * <p>This implementation does not allow columns to be directly created. An
         * AssertionError will be thrown if this method is called.</p>
         */
        public void createColumn(String name) throws AlreadyExistsException {
            throw new AssertionError(BuilderBundle.getString("datasetTableCreateColumnReject"));
        }
        
        /**
         * If this table is partitioned, returns the partitioning column(s). Otherwise, returns an empty set.
         * @return a set, never null, of {@link Column}s this table is partitioned on.
         */
        public Collection getPartitionedColumns() {
            Set cols = new HashSet();
            for (Iterator i = this.getColumns().iterator(); i.hasNext(); ) {
                DataSetColumn c = (DataSetColumn)i.next();
                if (c instanceof SchemaNameColumn) {
                    cols.add(c);
                } else if (c instanceof WrappedColumn) {
                    WrappedColumn wc = (WrappedColumn)c;
                    if (((DataSet)this.getSchema()).getPartitionedColumns().contains(wc)) cols.add(wc);
                }
            }
            return cols;
        }
    }
    
    /**
     * A column on a {@link DataSetTable} has to be one of the types of {@link DataSetColumn}
     * available from this class. All types can be renamed.
     */
    public static class DataSetColumn extends GenericColumn {
        /**
         * Internal reference to the {@link Relation} that produces this column.
         */
        private final Relation underlyingRelation;
        
        /**
         * This constructor gives the column a name and checks that the
         * parent {@link Table} is not null.
         *
         * @param name the name to give this column.
         * @param dsTable the parent {@link Table}
         * @param underlyingRelation the {@link Relation} that provided this column. The
         * underlying relation can be null in two instances - when the {@link DataSetTable} is
         * a MAIN table, in which case the column came from the parent table, and all
         * other instances, in which case the column came from the MAIN table in this
         * {@link OLDDataSet}.
         * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
         * that name.
         */
        public DataSetColumn(String name, DataSetTable dsTable, Relation underlyingRelation) throws AlreadyExistsException {
            super(generateAlias(name, dsTable), dsTable);
            this.underlyingRelation = underlyingRelation;
        }
        
        /**
         * Columns in {@link DataSetTable}s can be renamed by the user if the names don'table make
         * any sense to them. Use this method to do just that. It will check first to see if the proposed
         * name has already been used on this {@link DataSetTable}. If it has, an AlreadyExistsException
         * will be thrown, otherwise the change will be made. The new name must not be null.
         * @param name the new name for the column.
         * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
         * that name.
         */
        public void setName(String name) throws AlreadyExistsException {
            // Sanity check.
            if (name.equals(this.name)) return; // Skip unnecessary change.
            // Do it.
            String oldName = this.name;
            this.name = name;
            this.table.changeColumnMapKey(oldName, this.name);
        }
        
        /**
         * Internal method that generates a safe alias/name for a column. The first try is
         * always the original column name, followed by attempts with an underscore and a
         * sequence number appended.
         * @param name the first name to try.
         * @param dsTable the {@link DataSetTable} that this column is being added to.
         * @return the result.
         */
        protected static String generateAlias(String name, DataSetTable dsTable) {
            // Do it.
            String alias = name;
            int aliasNumber = 2;
            while (dsTable.getColumnByName(alias)!=null) {
                // Alias is original name appended with _2, _3, _4 etc.
                alias = name + "_" + (aliasNumber++);
            }
            return alias;
        }
        
        /**
         * Returns the underlying {@link Relation} that provided this column. If it returns null and
         * this table is a MAIN table, then that means the column came from the table's real table.
         * If it returns null in any other circumstances, ie. DIMENSION and MAIN_SUBCLASS, then
         * the column is part of the parent {@link DataSetTable}'s primary key.
         * @return the {@link Relation} that underpins this column.
         */
        public Relation getUnderlyingRelation() {
            return this.underlyingRelation;
        }
        
        /**
         * A column on a {@link DataSetTable} wraps an existing column but is otherwise identical to
         * a normal column. It assigns itself an alias if the original name is already used in the target table.
         * Can be used in keys on dataset tables.
         */
        public static class WrappedColumn extends DataSetColumn {
            /**
             * Internal reference to the wrapped {@link Column}.
             */
            private final Column column;
            
            /**
             * This constructor wraps an existing {@link Column} and checks that the
             * parent {@link Table} is not null. It also assigns an alias to the wrapped {@link Column}
             * if another one with the same name already exists on this table.
             *
             * @param column the {@link Column} to wrap.
             * @param dsTable the parent {@link Table}
             * @param underlyingRelation the {@link Relation} that provided this column. The
             * underlying relation can be null in two instances - when the {@link DataSetTable} is
             * a MAIN table, in which case the column came from the parent table, and all
             * other instances, in which case the column came from the MAIN table in this
             * {@link DataSet}.
             * @throws AlreadyExistsException if the world has stopped turning. (Should never happen.)
             */
            public WrappedColumn(Column column, DataSetTable dsTable, Relation underlyingRelation) throws AlreadyExistsException {
                // Call the parent with the alias.
                super(column.getName(), dsTable, underlyingRelation);
                // Remember the wrapped column.
                this.column = column;
            }
            
            /**
             * Returns the wrapped column.
             * @return the wrapped {@link Column}.
             */
            public Column getWrappedColumn() {
                return this.column;
            }
        }
        
        /**
         * A column on a {@link DataSetTable} that indicates the concatenation of the primary key values of a record
         * in some table beyond a concat-only relation. They take a reference to the concat-only relation.
         */
        public static class ConcatRelationColumn extends DataSetColumn {
            /**
             * The constructor takes a name for this column-to-be, and the {@link DataSetTable}
             * on which it is to be constructed, and the {@link Relation} it represents.
             *
             * @param name the name to give this column.
             * @param dsTable the dsTable {@link Table}.
             * @param underlyingRelation the {@link Relation} that provided this column. The
             * underlying relation can be null in two instances - when the {@link DataSetTable} is
             * a MAIN table, in which case the column came from the parent table, and all
             * other instances, in which case the column came from the MAIN table in this
             * {@link OLDDataSet}.
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws AssociationException if the {@link Relation} is not a concat relation.
             */
            public ConcatRelationColumn(String name, DataSetTable dsTable, Relation concatRelation) throws AlreadyExistsException, AssociationException {
                // Super first.
                super(name, dsTable, concatRelation);
                // Sanity check.
                if (!((DataSet)dsTable.getSchema()).getConcatOnlyRelations().contains(concatRelation))
                    throw new AssociationException(BuilderBundle.getString("relationNotConcatRelation"));
            }
        }
        
        /**
         * A column on a {@link DataSetTable} that should be populated with the name
         * of the table provider providing the data in this row.
         */
        public static class SchemaNameColumn extends DataSetColumn {
            /**
             * This constructor gives the column a name and checks that the
             * parent {@link Table} is not null. The underlying relation is not required.
             * @param name the name to give this column.
             * @param dsTable the parent {@link Table}
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             */
            public SchemaNameColumn(String name, DataSetTable dsTable) throws AlreadyExistsException {
                super(name, dsTable, null);
            }
        }
    }
    
    /**
     * This class defines the various different types of DataSetTable there are.
     */
    public static class DataSetTableType implements Comparable {
        /**
         * Internal reference to the set of {@link DataSetTableType} singletons.
         */
        private static final Map singletons = new HashMap();
        
        /**
         * Use this constant to refer to a main table.
         */
        public static final DataSetTableType MAIN = new DataSetTableType("MAIN");
        
        /**
         * Use this constant to refer to a subclass of a main table.
         */
        public static final DataSetTableType MAIN_SUBCLASS = new DataSetTableType("MAIN_SUBCLASS");
        
        /**
         * Use this constant to refer to a dimension table.
         */
        public static final DataSetTableType DIMENSION = new DataSetTableType("DIMENSION");
        
        /**
         * Internal reference to the name of this {@link DataSetTableType}.
         */
        private final String name;
        
        /**
         * The private constructor takes a single parameter, which defines the name
         * this {@link DataSetTableType} object will display when printed.
         * @param name the name of the {@link DataSetTableType}.
         */
        private DataSetTableType(String name) {
            this.name = name;
        }
        
        /**
         * Displays the name of this {@link DataSetTableType} object.
         * @return the name of this {@link DataSetTableType} object.
         */
        public String getName() {
            return this.name;
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
            DataSetTableType c = (DataSetTableType)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            // We are dealing with singletons so can use == happily.
            return o == this;
        }
    }
    
    /**
     * This class defines the various different ways of optimising a dataset after it has
     * been constructed, eg. adding 'hasDimension' columns, or doing left joins on the dimensions.
     */
    public static class DataSetOptimiserType implements Comparable {
        /**
         * Internal reference to the set of {@link DataSetOptimiserType} singletons.
         */
        private static final Map singletons = new HashMap();
        
        /**
         * Use this constant to refer to no optimisation.
         */
        public static final DataSetOptimiserType NONE = new DataSetOptimiserType("NONE");
        
        /**
         * Use this constant to refer to optimising by running a left join on each dimension..
         */
        public static final DataSetOptimiserType LEFTJOIN = new DataSetOptimiserType("LEFTJOIN");
        
        /**
         * Use this constant to refer to optimising by including an extra column on the main
         * table for each dimension and populating it with true or false..
         */
        public static final DataSetOptimiserType COLUMN = new DataSetOptimiserType("COLUMN");
        
        /**
         * Use this constant to refer to no optimising by creating a separate table linked on a 1:1
         * basis with the main table, with one column per dimension populated with true or false.
         */
        public static final DataSetOptimiserType TABLE = new DataSetOptimiserType("TABLE");
        
        /**
         * Internal reference to the name of this {@link DataSetTableType}.
         */
        private final String name;
        
        /**
         * The private constructor takes a single parameter, which defines the name
         * this {@link DataSetOptimiserType} object will display when printed.
         * @param name the name of the {@link DataSetOptimiserType}.
         */
        private DataSetOptimiserType(String name) {
            this.name = name;
        }
        
        /**
         * Displays the name of this {@link DataSetOptimiserType} object.
         * @return the name of this {@link DataSetOptimiserType} object.
         */
        public String getName() {
            return this.name;
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
            DataSetOptimiserType c = (DataSetOptimiserType)o;
            return this.toString().compareTo(c.toString());
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
