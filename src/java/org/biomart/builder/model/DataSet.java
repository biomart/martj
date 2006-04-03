/*
 * DataSet.java
 *
 * Created on 27 March 2006, 14:24
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
import org.biomart.builder.model.DataSet.DataSetColumn.HasDimensionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.TableProviderNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Key.CompoundForeignKey;
import org.biomart.builder.model.Key.CompoundPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Key.SimpleForeignKey;
import org.biomart.builder.model.Key.SimplePrimaryKey;
import org.biomart.builder.model.Relation.OneToMany;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.model.TableProvider.GenericTableProvider;

/**
 * This is the heart of the whole system, and represents a single data set in a mart.
 * The generic implementation includes the algorithm which flattens tables down into
 * a set of mart tables based on the contents of a {@link Window}.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 31st March 2006
 * @since 0.1
 */
public interface DataSet extends Comparable {
    /**
     * Constant representing the separator between bits of table names.
     */
    public static final String SEPARATOR = "__";
    
    /**
     * Constant representing the suffix added to main table names.
     */
    public static final String MAIN_SUFFIX = "main";
    
    /**
     * Constant representing the separator between main and subclass table names.
     */
    public static final String SUBCLASS_SEPARATOR = "__";
    
    /**
     * Constant representing the prefix for a table provider partition column.
     */
    public static final String TBLPROVCOL_PREFIX = "__tblprov_";
    
    /**
     * Constant representing the prefix for a concat column.
     */
    public static final String CONCATCOL_PREFIX = "__concat_";
    
    /**
     * Returns the {@link Window} this {@link DataSet} is constructed from.
     * @return the {@link Window} for this {@link DataSet}.
     */
    public Window getWindow();
    
    /**
     * Returns the {@link DataSetTable} representing the central main table for this {@link DataSet}.
     * @return the {@link DataSetTable} representing the main table for this {@link DataSet}.
     * @throws NullPointerException if it could not muster up a main table.
     */
    public DataSetTable getMainTable() throws NullPointerException;
    
    /**
     * Rebuild this {@link DataSet} based on the contents of its parent {@link Window}.
     * It will attempt to keep any custom names etc. for tables and columns in the regenerated
     * version where they had been previously specified by the user.
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void regenerate() throws SQLException, BuilderException;
    
    /**
     * Sets the {@link MartConstructor} this {@link DataSet} will be built by.
     * @param mc the {@link MartConstructor} for this {@link DataSet}.
     * @throws NullPointerException if the parameter is null.
     */
    public void setMartConstructor(MartConstructor mc) throws NullPointerException;
    
    /**
     * Returns the {@link MartConstructor} this {@link DataSet} will be built by.
     * @return the {@link MartConstructor} for this {@link DataSet}.
     */
    public MartConstructor getMartConstructor();
    
    /**
     * Request that the mart for this dataset be constructed now.
     * @throws SQLException if there was any data source error during
     * mart construction.
     * @throws BuilderException if there was any other kind of error in the
     * mart construction process.
     */
    public void constructMart() throws BuilderException, SQLException;
    
    /**
     * The generic version can construct itself from any given Window.
     */
    public class GenericDataSet implements DataSet {
        /**
         * Internal reference to the mart constructor that will build us later.
         */
        private MartConstructor mc;
        
        /**
         * Internal reference to the parent window.
         */
        private Window window;
        
        /**
         * Internal reference to the generated main table.
         */
        private DataSetTable mainTable;
        
        /**
         * The constructor links this {@link DataSet} with a specific {@link Window}.
         * @throws NullPointerException if the window is null.
         */
        public GenericDataSet(Window window) throws NullPointerException {
            // Sanity check.
            if (window==null)
                throw new NullPointerException("Parent window cannot be null.");
            // Do it.
            this.window = window;
        }
        
        /**
         * Returns the {@link Window} this {@link DataSet} is constructed from.
         * @return the {@link Window} for this {@link DataSet}.
         */
        public Window getWindow() {
            return this.window;
        }
        
        /**
         * Returns the {@link DataSetTable} representing the central main table for this {@link DataSet}.
         * @return the {@link DataSetTable} representing the main table for this {@link DataSet}.
         * @throws NullPointerException if it could not muster up a main table.
         */
        public DataSetTable getMainTable() throws NullPointerException {
            // Sanity check.
            try {
                if (this.mainTable==null) this.regenerate();
            } catch (Exception e) {
                NullPointerException npe = new NullPointerException("Unable to regenerate DataSet.");
                npe.initCause(e);
                throw npe;
            }
            if (this.mainTable==null)
                throw new NullPointerException("Unable to construct a main table. Does the parent Window have a central table?");
            // Do it.
            return this.mainTable;
        }
        
        /**
         * Rebuild this {@link DataSet} based on the contents of its parent {@link Window}.
         * It will attempt to keep any custom names etc. for tables and columns in the regenerated
         * version where they had been previously specified by the user.
         * @throws SQLException if there was a problem connecting to the data source.
         * @throws BuilderException if there was any other kind of problem.
         */
        public void regenerate() throws SQLException, BuilderException {
            // Build a dummy table provider.
            String dataSetName = this.getWindow().getName();
            DataSetTableProvider tp = new DataSetTableProvider(this.getWindow().getName());
            
            // Set up maps of interesting relations
            // ignored relations - keys are tables, values are sets of relations
            Map ignoredRelations = new HashMap();
            // subclass and dimension relations - a simple set for each
            Set dimensionRelations = new HashSet();
            Set subclassRelations = new HashSet();
            
            // Identify main table.
            Table centralTable = this.getWindow().getTable();
            // if central table has subclass relations and is at the foreign key end, then follow
            // them to the real central table.
            boolean found = false;
            for (Iterator i = centralTable.getForeignKeys().iterator(); i.hasNext() && !found; ) {
                Key k = (Key)i.next();
                for (Iterator j = k.getRelations().iterator(); j.hasNext() && !found; ) {
                    Relation r = (Relation)j.next();
                    if (this.getWindow().getSubclassedRelations().contains(r)) {
                        centralTable = r.getPrimaryKey().getTable();
                        found = true;
                    }
                }
            }
            
            // Identify all subclass and dimension relations, and set up the ignorable
            // relations for main and dimension tables.
            ignoredRelations.put(centralTable, new HashSet());
            for (Iterator i = centralTable.getPrimaryKey().getRelations().iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                // Skip masked relations.
                if (this.getWindow().getMaskedRelations().contains(r)) continue;
                // See what kind of relation we have.
                if (this.getWindow().getSubclassedRelations().contains(r)) {
                    // Subclass relation from main table? Ignore it at main table but add to subclass set.
                    ((Set)ignoredRelations.get(centralTable)).add(r);
                    subclassRelations.add(r);
                    Table subclassTable = r.getForeignKey().getTable();
                    ignoredRelations.put(subclassTable, new HashSet());
                    if (subclassTable.getPrimaryKey()!=null) {
                        // Mark all OneToManys from the subclass table as dimensions.
                        for (Iterator j = subclassTable.getPrimaryKey().getRelations().iterator(); j.hasNext(); ) {
                            Relation sr = (Relation)j.next();
                            // OneToMany from subclass table? Ignore it at subclass table but add to dimension set.
                            // Also add the relation to the ignore set of the dimension.
                            if (sr instanceof OneToMany) {
                                ((Set)ignoredRelations.get(subclassTable)).add(sr);
                                dimensionRelations.add(sr);
                                Table dimTable = sr.getForeignKey().getTable();
                                if (!ignoredRelations.containsKey(dimTable)) ignoredRelations.put(dimTable, new HashSet());
                                ((Set)ignoredRelations.get(dimTable)).add(sr);
                            }
                        }
                    }
                } else if (r instanceof OneToMany) {
                    // OneToMany from main table? Ignore it at main table but add to dimension set.
                    // Also add the relation to the ignore set of the dimension.
                    // Note that subclass OneToMany will already have been picked up by previous test.
                    ((Set)ignoredRelations.get(centralTable)).add(r);
                    dimensionRelations.add(r);
                    Table dimTable = r.getForeignKey().getTable();
                    if (!ignoredRelations.containsKey(dimTable)) ignoredRelations.put(dimTable, new HashSet());
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
            this.mainTable = this.constructDataSetTable(tp, DataSetTableType.MAIN, centralTable, (Set)ignoredRelations.get(centralTable), null);
            
            // Build the subclass tables.
            for (Iterator i = subclassRelations.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                Table sct = r.getForeignKey().getTable();
                DataSetTable subclass = this.constructDataSetTable(tp, DataSetTableType.MAIN_SUBCLASS, sct, (Set)ignoredRelations.get(sct), r);
            }
            
            // Build the dimension tables.
            for (Iterator i = dimensionRelations.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                Table dt = r.getForeignKey().getTable();
                DataSetTable dim = this.constructDataSetTable(tp, DataSetTableType.DIMENSION, dt, (Set)ignoredRelations.get(dt), r);
                // Add 'has' column to parent table (main table or subclass table) for this dimension.
                DataSetTable dimParent = (DataSetTable)tp.getTableByName(r.getPrimaryKey().getTable().getName());
                new HasDimensionColumn("__has_"+dt.getName(), dimParent, dim);
            }
        }
        
        /**
         * Internal function that constructs a data set table based on a real table, and the relations
         * linking to/from it. If a primary key is supplied, then a foreign key is created on the data set
         * table linking it to that primary key. Relations in the set supplied are ignored.
         * @param prov the place to create the table in.
         * @param type the type of table to create.
         * @param sourceTable the original table to transform.
         * @param ignoreRels the set of relations to ignore during transformation. If null, taken as empty.
         * @param parentRel the relation between this and the 'real' parent table to link to if required.
         * Null indicates no linking required. Linking takes place to the dataset table version of the 'real' table.
         * @return a fully constructed and keyed up data set table.
         * @throws BuilderException if there was any trouble.
         * @throws NullPointerException if any required parameter was null.
         */
        private DataSetTable constructDataSetTable(DataSetTableProvider prov, DataSetTableType type, Table sourceTable, Set ignoreRels, Relation parentRel) throws BuilderException, NullPointerException {
            // Sanity check.
            if (prov==null)
                throw new NullPointerException("Table provider cannot be null.");
            if (type==null)
                throw new NullPointerException("Table type cannot be null.");
            if (sourceTable==null)
                throw new NullPointerException("Source table cannot be null.");
            // Sensible defaults.
            if (ignoreRels==null)
                ignoreRels = new HashSet();
            // Do it!
            List keyRelations = new ArrayList();
            List pkCols = new ArrayList();
            Set cols = new HashSet();
            // Add all masked columns to the ignore set.
            ignoreRels.addAll(this.getWindow().getMaskedRelations());
            // Create the DataSetTable
            DataSetTable dt = new DataSetTable(sourceTable.getName(), this, prov, type);
            this.transformTable(dt, sourceTable, ignoreRels, keyRelations, pkCols, cols);
            dt.setUnderlyingRelations(keyRelations);
            // Add table provider partition column if required - must be part of primary key.
            DataSetColumn childTblProvCol=null;
            if (sourceTable.getTableProvider() instanceof PartitionedTableProvider) {
                childTblProvCol = new TableProviderNameColumn("__tblprov", dt);
                pkCols.add(childTblProvCol);
            }
            // Work out the foreign key only if the parent relation is not null.
            if (parentRel != null) {
                // Get real table for dataset parent table
                Table parentRealTable = parentRel.getPrimaryKey().getTable();
                DataSetTable parentDSTable = (DataSetTable)prov.getTableByName(parentRealTable.getName());
                // Create new, empty child FK list.
                List fkCols = new ArrayList();
                // For each column in dataset parent table PK
                for (Iterator i = parentDSTable.getPrimaryKey().getColumns().iterator(); i.hasNext(); ) {
                    DataSetColumn c = (DataSetColumn)i.next();
                    DataSetColumn childCol;
                    if (c instanceof WrappedColumn) {
                        // If column is a wrapped column
                        // Find associated real column
                        Column rc = ((WrappedColumn)c).getWrappedColumn();
                        if (!rc.getTable().equals(sourceTable)) {
                            // If real column not on original main table, create child column for it add it to child table PK
                            childCol = new WrappedColumn(rc, dt);
                            pkCols.add(childCol);
                        } else {
                            // Else follow original relation FK end and find real child column and associated child column
                            int parentRealPKColPos = parentRealTable.getPrimaryKey().getColumns().indexOf(rc);
                            Column childRealFKCol = (Column)parentRel.getForeignKey().getColumns().get(parentRealPKColPos);
                            childCol = (DataSetColumn)dt.getColumnByName(childRealFKCol.getName());
                        }
                    } else if (c instanceof TableProviderNameColumn) {
                        // Else if its a tblprov column
                        // Find child tblprov column
                        childCol = childTblProvCol;
                    } else {
                        // Else, can't handle.
                        throw new AssertionError("Cannot handle non-Wrapped non-TableProviderName columns in foreign keys.");
                    }
                    // add child column to child FK
                    fkCols.add(childCol);
                }
                // Create the foreign key.
                if (fkCols.size()>1) dt.addForeignKey(new CompoundForeignKey(fkCols));
                else if (fkCols.size()==1) dt.addForeignKey(new SimpleForeignKey((Column)fkCols.get(0)));
                else throw new AssertionError("Foreign key expected but has no columns.");
            }
            // Create the primary key on it.
            if (pkCols.size()>1) dt.setPrimaryKey(new CompoundPrimaryKey(pkCols));
            else if (pkCols.size()==1) dt.setPrimaryKey(new SimplePrimaryKey((Column)pkCols.get(0)));
            // Return.
            return dt;
        }
        
        /**
         * Simple recursive internal function that walks through relations, either noting that its seen them,
         * marking them as concat relations, and adding their columns to the table.
         * @param dsTable the table to construct as we go along.
         * @param sourceTable the table we are adding columns to dsTable from next.
         * @param ignoreRels relations to ignore.
         * @param keyRelations the relations used as we went along.
         * @param pkCols the primary key columns to be added to the new table.
         * @param cols the columns to be added to the new table.
         * @throws NullPointerException if any of the params were null.
         * @throws AlreadyExistsException if any attempt to add a column resulted in a duplicate name.
         */
        private void transformTable(DataSetTable dsTable, Table sourceTable, Set ignoreRels, List keyRelations, List pkCols, Set cols) throws NullPointerException, AlreadyExistsException {
            // Sanity check.
            if (dsTable==null)
                throw new NullPointerException("Table cannot be null.");
            if (sourceTable==null)
                throw new NullPointerException("Source table cannot be null.");
            if (ignoreRels==null)
                throw new NullPointerException("Ignore relations cannot be null.");
            if (keyRelations==null)
                throw new NullPointerException("Relations pairs cannot be null.");
            if (pkCols==null)
                throw new NullPointerException("Primary key columns cannot be null.");
            if (cols==null)
                throw new NullPointerException("Columns cannot be null.");
            // Find all relations from source table.
            // Additionally, find all columns in foreign keys with non-ignored relations. Exclude them.
            Set sourceRels = new HashSet();
            Set excludedColumns = new HashSet();
            if (sourceTable.getPrimaryKey()!=null) sourceRels.addAll(sourceTable.getPrimaryKey().getRelations());
            for (Iterator i = sourceTable.getForeignKeys().iterator(); i.hasNext(); ) {
                Key k = (Key)i.next();
                for (Iterator j = k.getRelations().iterator(); j.hasNext(); ) {
                    Relation r = (Relation)j.next();
                    sourceRels.add(r);
                    if (!ignoreRels.contains(r))
                        excludedColumns.addAll(k.getColumns());
                }
            }
            // Also exclude all masked columns.
            excludedColumns.addAll(this.getWindow().getMaskedColumns());
            // Add all non-excluded columns from source table to dataset table.
            for (Iterator i = sourceTable.getColumns().iterator(); i.hasNext(); ) {
                Column c = (Column)i.next();
                // If column is excluded, ignore it.
                if (excludedColumns.contains(c)) continue;
                // Otherwise add the column to our table.
                Column wc = new WrappedColumn(c, dsTable);
                // If column is part of primary key, add the wrapped version to the primary key column set.
                if (sourceTable.getPrimaryKey().getColumns().contains(c)) pkCols.add(wc);
            }
            // For all non-ignored rels in sourceTable
            for (Iterator i = sourceRels.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                // Need to recheck ignoreRefs in case has become ignored since we built our set of relations.
                if (ignoreRels.contains(r)) continue;
                // Find key in relation that refers to this table.
                Key sourceKey = r.getPrimaryKey();
                Table targetTable = r.getForeignKey().getTable();
                if (!sourceKey.getTable().equals(sourceTable)) {
                    sourceKey = r.getForeignKey();
                    targetTable = r.getPrimaryKey().getTable();
                }
                // Add relation to path followed so far.
                keyRelations.add(r);
                // Add to ignore so don't revisit later
                ignoreRels.add(r);
                if (sourceKey instanceof PrimaryKey && this.getWindow().getConcatOnlyRelations().contains(r)) {
                    // If concat-only and sourceTable is at primary key end of relation, concat it
                    try {
                        new ConcatRelationColumn("__concat_"+targetTable.getName(), dsTable, r);
                    } catch (AssociationException e) {
                        throw new AssertionError("Table did not match itself.");
                    }
                } else {
                    // Otherwise, recurse down to target table.
                    this.transformTable(dsTable, targetTable, ignoreRels, keyRelations, pkCols, cols);
                }
            }
        }
        
        /**
         * Sets the {@link MartConstructor} this {@link DataSet} will be built by.
         * @param mc the {@link MartConstructor} for this {@link DataSet}.
         * @throws NullPointerException if the parameter is null.
         */
        public void setMartConstructor(MartConstructor mc) throws NullPointerException {
            // Sanity check.
            if (mc==null)
                throw new NullPointerException("Mart constructor cannot be null.");
            // Do it.
            this.mc = mc;
        }
        
        /**
         * Returns the {@link MartConstructor} this {@link DataSet} will be built by.
         * @return the {@link MartConstructor} for this {@link DataSet}.
         */
        public MartConstructor getMartConstructor() {
            return this.mc;
        }
        
        /**
         * Request that the mart for this dataset be constructed now.
         * @throws SQLException if there was any data source error during
         * mart construction.
         * @throws BuilderException if there was any other kind of error in the
         * mart construction process.
         */
        public void constructMart() throws BuilderException, SQLException {
            // Sanity check.
            if (this.mc==null)
                throw new BuilderException("Cannot construct mart as no mart constructor has been specified.");
            // Do it.
            this.mc.constructMart(this);
        }
        
        /**
         * Displays the name of this {@link DataSet} object. The name is the same as the
         * parent {@link Window}.
         * @return the name of this {@link DataSet} object.
         */
        public String toString() {
            return this.getWindow().getName();
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
         * @throws ClassCastException if the object o is not a {@link DataSet}.
         */
        public int compareTo(Object o) throws ClassCastException {
            DataSet d = (DataSet)o;
            return this.toString().compareTo(d.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link DataSet}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof DataSet)) return false;
            DataSet d = (DataSet)o;
            return d.toString().equals(this.toString());
        }
    }
    
    /**
     * This special {@link TableProvider} allows tables to be added to it, but only accepts
     * {@link DataSetTable}s..
     */
    public class DataSetTableProvider extends GenericTableProvider {
        /**
         * The constructor delegates directly upwards to {@link GenericTableProvider}.
         * @param name the name to give this provider.
         * @throws NullPointerException if the name is null.
         */
        public DataSetTableProvider(String name) throws NullPointerException {
            super(name);
        }
        
        /**
         * Adds a {@link DataSetTable} to this provider. The table must not be null, and
         * must not already exist (ie. with the same name).
         * @param t the {@link DataSetTable} to add.
         * @throws AlreadyExistsException if another one with the same name already exists.
         * @throws AssociationException if the table doesn't belong to this provider.
         * @throws NullPointerException if the table is null.
         */
        public void addTable(DataSetTable t) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (t==null)
                throw new NullPointerException("Table cannot be null.");
            if (!t.getTableProvider().equals(this))
                throw new AssociationException("Table does not belong to this provider.");
            if (this.tables.containsKey(t.getName()))
                throw new AlreadyExistsException("Table with that name already exists in this provider.", t.getName());
            // Do it.
            this.tables.put(t.getName(), t);
        }
    }
    
    /**
     * This special {@link Table} represents the merge of one or more other {@link Table}s
     * by following a series of {@link Relation}s. As such it has no real columns of its own,
     * so every column is from another table and is given an alias.
     */
    public class DataSetTable extends GenericTable {
        /**
         * Internal reference to the list of {@link Relation}s to follow to construct this table.
         * The list contains alternate references to Key (first) then Relation (second), in pairs,
         * until the final pair. The key specifies which end of the relation to start from.
         */
        private final List relations = new ArrayList();
        
        /**
         * Internal reference to the type of this table.
         */
        private final DataSetTableType type;
        
        /**
         * Internal reference to the dataset of this table.
         */
        private final DataSet ds;
        
        /**
         * The constructor calls the parent {@link GenericTable} constructor. It uses a
         * {@link DataSetTableProvider} as a parent for itself. You must also supply a type that
         * describes this as a main table, dimension table, etc.
         * @param name the table name.
         * @param ds the {@link DataSet} this table belongs in.
         * @param prov the {@link DataSetTableProvider} to hold this table in.
         * @param type the {@link DataSetTableType} that best describes this table.
         * @throws NullPointerException if any parameter is null.
         * @throws AssociationException if the relations didn't contain the right number of elements for
         * the table type.
         * @throws IllegalArgumentException if the relations list contained any null or non-Relation elements.
         * @throws AlreadyExistsException if the provider, for whatever reason, refuses to
         * allow this {@link Table} to be added to it using {@link DataSetTableProvider#addTable(Table)}.
         */
        public DataSetTable(String name, DataSet ds, DataSetTableProvider prov, DataSetTableType type) throws AlreadyExistsException, NullPointerException, AssociationException, IllegalArgumentException {
            // Super call first.
            super(name, prov);
            // Sanity check.
            if (type==null)
                throw new NullPointerException("Table type cannot be null.");
            if (ds==null)
                throw new NullPointerException("Dataset cannot be null.");
            // Do the work.
            this.type = type;
            this.ds = ds;
            // Add the table to the provider.
            try {
                prov.addTable(this);
            } catch (AssociationException e) {
                throw new AssertionError("Table provider does not match itself.");
            }
        }
        
        /**
         * Returns the type of this table specified at construction time.
         * @return the type of this table.
         */
        public DataSetTableType getType() {
            return this.type;
        }
        
        /**
         * Returns the dataset of this table specified at construction time.
         * @return the dataset of this table.
         */
        public DataSet getDataSet() {
            return this.ds;
        }
        
        /**
         * Sets the list of relations used to construct this table.
         * @param the list of relations of this table. May be empty but never null.
         * @throws NullPointerException if the list was null.
         * @throws IllegalArgumentException if the list wasn't a list of Key/Relation pairs.
         */
        public void setUnderlyingRelations(List relations) throws NullPointerException, IllegalArgumentException {
            // Sanity check.
            if (relations==null)
                throw new NullPointerException("Relations list cannot be null.");
            // Check the relations and save them.
            this.relations.clear();
            for (Iterator i = relations.iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o==null || !(o instanceof Relation))
                    throw new IllegalArgumentException("Relations must be only Relation instances, they cannot be null or anything else.");
                this.relations.add((Relation)o);
            }
        }
        
        /**
         * Returns the list of relations used to construct this table.
         * @return the list of relations of this table. May be empty but never null.
         */
        public List getUnderlyingRelations() {
            return this.relations;
        }
        
        /**
         * <p>Attemps to add a {@link Column} to this table. The {@link Column} will already
         * have had it's {@link Table} parameter set to match, otherwise an
         * {@link IllegalArgumentException} will be thrown. That exception will also get thrown
         * if the {@link Column} has the same name as an existing one on this table.</p>
         *
         * <p>{@link DataSetTable}s insist that all columns are {@link DataSetColumn}s.</p>
         *
         * @param c the {@link Column} to add, which must be a {@link DataSetColumn}..
         * @throws AlreadyExistsException if the {@link Column} name has already been used on
         * this {@link Table}.
         * @throws AssociationException if the {@link Table} parameter of the {@link Column}
         * does not match.
         * @throws NullPointerException if the {@link Column} object is null.
         */
        public void addColumn(Column c) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (!(c instanceof DataSetColumn))
                throw new AssociationException("Column must be a DataSetColumn to be added to a DataSetTable.");
            // Do the work.
            super.addColumn(c);
        }
        
        /**
         * <p>Convenience method that creates and adds a {@link Column} to this {@link Table}.
         * If a {@link Column} with the same name already exists an exception will be thrown.</p>
         *
         * <p>This implementation does not allow columns to be directly created. An
         * AssertionError will be thrown if this method is called.</p>
         *
         * @param name the name of the {@link Column} to create and add.
         * @throws AlreadyExistsException if a {@link Column} with the same name already
         * exists in this {@link Table}.
         * @throws NullPointerException if the name argument is null.
         */
        public void createColumn(String name) throws AlreadyExistsException, NullPointerException {
            throw new AssertionError("Columns cannot be created on a DataSetTable.");
        }
        
        /**
         * Convenience method that wraps a {@link Column} and adds it to this {@link Table}.
         * @param c the {@link Column} to wrap and add.
         * @throws NullPointerException if the argument is null.
         */
        public void createColumn(Column c) throws NullPointerException {
            try {
                new WrappedColumn(c, this);
                // By creating it we've already added it to ourselves! (Based on DataSetColumn behaviour)
            } catch (AlreadyExistsException e) {
                throw new AssertionError("Alias generator failed to generate a unique alias for this column.");
            }
        }
        
        /**
         * {@link DataSetTable}s can be renamed by the user if the names don't make
         * any sense to them. Use this method to do just that. It will check first to see if the proposed
         * name has already been used in the data set {@link TableProvider}. If it has, an AlreadyExistsException
         * will be thrown, otherwise the change will be made. The new name must not be null.
         * @param name the new name for the table.
         * @throws AlreadyExistsException if a {@link DataSetTable} already exists with
         * that name.
         * @throws NullPointerException if the new name is null.
         */
        public void setName(String name) throws NullPointerException, AlreadyExistsException {
            // Sanity check.
            if (name==null)
                throw new NullPointerException("New name cannot be null.");
            if (this.getTableProvider().getTableByName(name)!=null)
                throw new AlreadyExistsException("A table with that name already exists in this dataset.", name);
            // Do it.
            this.name = name;
        }
        
        /**
         * If this table is partitioned, returns the partitioning column(s). Otherwise, returns an empty set.
         * @return a set, never null, of {@link Column}s this table is partitioned on.
         */
        public Collection getPartitionedColumns() {
            Set cols = new HashSet();
            for (Iterator i = this.getColumns().iterator(); i.hasNext(); ) {
                DataSetColumn c = (DataSetColumn)i.next();
                if (c instanceof TableProviderNameColumn && this.getDataSet().getWindow().getPartitionOnTableProvider()) {
                    cols.add(c);
                } else if (c instanceof WrappedColumn) {
                    WrappedColumn wc = (WrappedColumn)c;
                    if (this.getDataSet().getWindow().getPartitionedColumns().contains(wc)) cols.add(wc);
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
         * This constructor gives the column a name and checks that the
         * parent {@link Table} is not null.
         * @param name the name to give this column.
         * @param t the parent {@link Table}
         * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
         * that name.
         * @throws NullPointerException if either parameter is null.
         */
        public DataSetColumn(String name, DataSetTable t) throws NullPointerException, AlreadyExistsException {
            super(generateAlias(name, t), t);
        }
        
        /**
         * Columns in {@link DataSetTable}s can be renamed by the user if the names don't make
         * any sense to them. Use this method to do just that. It will check first to see if the proposed
         * name has already been used on this {@link DataSetTable}. If it has, an AlreadyExistsException
         * will be thrown, otherwise the change will be made. The new name must not be null.
         * @param name the new name for the column.
         * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
         * that name.
         * @throws NullPointerException if the new name is null.
         */
        public void setName(String name) throws NullPointerException, AlreadyExistsException {
            // Sanity check.
            if (name==null)
                throw new NullPointerException("New name cannot be null.");
            if (t.getColumnByName(name)!=null)
                throw new AlreadyExistsException("A column with that name already exists on this table.", name);
            // Do it.
            this.name = name;
        }
        
        /**
         * Internal method that generates a safe alias/name for a column. The first try is
         * always the original column name, followed by attempts with an underscore and a
         * sequence number appended.
         * @param name the first name to try.
         * @param t the {@link DataSetTable} that this column is being added to.
         * @return the result.
         * @throws NullPointerException if any parameter is null.
         */
        protected static String generateAlias(String name, DataSetTable t) throws NullPointerException {
            // Sanity check.
            if (name==null)
                throw new NullPointerException("Column name cannot be null.");
            if (t==null)
                throw new NullPointerException("Parent table cannot be null.");
            // Do it.
            String alias = name;
            int aliasNumber = 2;
            while (t.getColumnByName(alias)!=null) {
                // Alias is original name appended with _2, _3, _4 etc.
                alias = name+"_"+(aliasNumber++);
            }
            return alias;
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
            private final Column c;
            
            /**
             * This constructor wraps an existing {@link Column} and checks that the
             * parent {@link Table} is not null. It also assigns an alias to the wrapped {@link Column}
             * if another one with the same name already exists on this table.
             * @param c the {@link Column} to wrap.
             * @param t the parent {@link Table}
             * @throws NullPointerException if either parameter is null.
             * @throws AlreadyExistsException if the world has stopped turning. (Should never happen.)
             */
            public WrappedColumn(Column c, DataSetTable t) throws NullPointerException, AlreadyExistsException {
                // Call the parent with the alias.
                super(c.getName(), t);
                // Remember the wrapped column.
                this.c = c;
            }
            
            /**
             * Returns the wrapped column.
             * @return the wrapped {@link Column}.
             */
            public Column getWrappedColumn() {
                return this.c;
            }
        }
        
        /**
         * A column on a {@link DataSetTable} that indicates the presence of a record in some dimension table.
         * These only appear on main tables. They take a reference to a dimension table.
         */
        public static class HasDimensionColumn extends DataSetColumn {
            /**
             * Internal reference to the dimension this column refers to.
             */
            private final DataSetTable dim;
            
            /**
             * The constructor takes a name for this column-to-be, and the {@link DataSetTable}
             * on which it is to be constructed. If that table is not a MAIN or MAIN_SUBCLASS table
             * an exception will be thrown, likewise if the reference {@link DataSetTable} which
             * the column refers to is not a DIMENSION.
             * @param name the name to give this column.
             * @param main the parent {@link Table}
             * @param dim the dimension {@link Table}
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws AssociationException if the parent is not MAIN or MAIN_SUBCLASS or the
             * dimension is not DIMENSION.
             * @throws NullPointerException if any parameter is null.
             */
            public HasDimensionColumn(String name, DataSetTable parent, DataSetTable dim) throws NullPointerException, AlreadyExistsException, AssociationException {
                // Super first.
                super(name, parent);
                // Sanity check.
                if (dim==null)
                    throw new NullPointerException("Dimension table cannot be null.");
                if (!dim.getType().equals(DataSetTableType.DIMENSION))
                    throw new AssociationException("Dimension table is not of DIMENSION type.");
                if (!(parent.getType().equals(DataSetTableType.MAIN) || parent.getType().equals(DataSetTableType.MAIN)))
                    throw new AssociationException("Parent table is not of MAIN or MAIN_SUBCLASS type.");
                // Do it.
                this.dim = dim;
            }
            
            /**
             * Returns the dimension this column refers to.
             * @return the {@link DataSetTable} representing the dimension.
             */
            public DataSetTable getDimension() {
                return this.dim;
            }
        }
        
        /**
         * A column on a {@link DataSetTable} that indicates the concatenation of the primary key values of a record
         * in some table beyond a concat-only relation. They take a reference to the concat-only relation.
         */
        public static class ConcatRelationColumn extends DataSetColumn {
            /**
             * Internal reference to the relation this column refers to.
             */
            private final Relation rel;
            
            /**
             * The constructor takes a name for this column-to-be, and the {@link DataSetTable}
             * on which it is to be constructed, and the {@link Relation} it represents.
             * @param name the name to give this column.
             * @param parent the parent {@link Table}.
             * @param rel the concat only {@link Relation} it refers to.
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws AssociationException if the {@link Relation} is not a concat relation.
             * @throws NullPointerException if any parameter is null.
             */
            public ConcatRelationColumn(String name, DataSetTable parent, Relation rel) throws NullPointerException, AlreadyExistsException, AssociationException {
                // Super first.
                super(name, parent);
                // Sanity check.
                if (rel==null)
                    throw new NullPointerException("Relation cannot be null.");
                if (!parent.getDataSet().getWindow().getConcatOnlyRelations().contains(rel))
                    throw new AssociationException("This relation is not a concat relation.");
                // Do it.
                this.rel = rel;
            }
            
            /**
             * Returns the {@link Relation} this column refers to.
             * @return the {@link Relation} for this column.
             */
            public Relation getRelation() {
                return this.rel;
            }
        }
        
        /**
         * A column on a {@link DataSetTable} that should be populated with the name
         * of the table provider providing the data in this row.
         */
        public static class TableProviderNameColumn extends DataSetColumn {
            /**
             * This constructor gives the column a name and checks that the
             * parent {@link Table} is not null.
             * @param name the name to give this column.
             * @param t the parent {@link Table}
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws NullPointerException if either parameter is null.
             */
            public TableProviderNameColumn(String name, DataSetTable t) throws NullPointerException, AlreadyExistsException {
                super(name, t);
            }
        }
    }
    
    /**
     * This class defines the various different types of DataSetTable there are.
     */
    public class DataSetTableType implements Comparable {
        /**
         * Use this constant to refer to a main table.
         */
        public static final DataSetTableType MAIN = DataSetTableType.get("MAIN");
        
        /**
         * Use this constant to refer to a subclass of a main table.
         */
        public static final DataSetTableType MAIN_SUBCLASS = DataSetTableType.get("MAIN_SUBCLASS");
        
        /**
         * Use this constant to refer to a dimension table.
         */
        public static final DataSetTableType DIMENSION = DataSetTableType.get("DIMENSION");
        
        /**
         * Internal reference to the name of this {@link DataSetTableType}.
         */
        private final String name;
        
        /**
         * Internal reference to the set of {@link DataSetTableType} singletons.
         */
        private static final Map singletons = new HashMap();
        
        /**
         * The static factory method creates and returns a {@link DataSetTableType}
         * with the given name. It ensures the object returned is a singleton.
         * Note that the names of {@link DataSetTableType} objects are case-insensitive.
         * @param name the name of the {@link DataSetTableType} object.
         * @return the {@link DataSetTableType} object.
         */
        public static DataSetTableType get(String name) {
            // Convert to upper case.
            name = name.toUpperCase();
            // Do we already have this one?
            // If so, then return it.
            if (singletons.containsKey(name)) return (DataSetTableType)singletons.get(name);
            // Otherwise, create it, remember it, then return it.
            DataSetTableType s = new DataSetTableType(name);
            singletons.put(name,s);
            return s;
        }
        
        /**
         * The private constructor takes a single parameter, which defines the name
         * this {@link DataSetTableType} object will display when printed.
         * @param name the name of the {@link DataSetTableType}.
         */
        private DataSetTableType(String name) {
            this.name=name;
        }
        
        /**
         * Displays the name of this {@link DataSetTableType} object.
         * @return the name of this {@link DataSetTableType} object.
         */
        public String toString() {
            return this.name;
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
         * @throws ClassCastException if the object o is not a {@link DataSetTableType}.
         */
        public int compareTo(Object o) throws ClassCastException {
            DataSetTableType c = (DataSetTableType)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * Return true if the objects are identical.
         * @param o the object to compare to.
         * @return true if the names are the same and both are {@link DataSetTableType} instances,
         * otherwise false.
         */
        public boolean equals(Object o) {
            // We are dealing with singletons so can use == happily.
            return o==this;
        }
    }
}
