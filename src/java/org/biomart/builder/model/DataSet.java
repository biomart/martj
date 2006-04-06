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
import org.biomart.builder.model.DataSet.DataSetColumn.TableProviderNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.OneToMany;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.model.TableProvider.GenericTableProvider;

/**
 * This is the heart of the whole system, and represents a single data set in a mart.
 * The generic implementation includes the algorithm which flattens tables down into
 * a set of mart tables based on the contents of a {@link Window}.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 6th April 2006
 * @since 0.1
 */
public interface DataSet extends Comparable, TableProvider {
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
     *
     * @param martConstructor the {@link MartConstructor} for this {@link DataSet}.
     * @throws NullPointerException if the parameter is null.
     */
    public void setMartConstructor(MartConstructor martConstructor) throws NullPointerException;
    
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
    public class GenericDataSet extends GenericTableProvider implements DataSet {
        /**
         * Internal reference to the mart constructor that will build us later.
         */
        private MartConstructor martConstructor;
        
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
         * @param window the {@link Window} to link this {@link DataSet} with.
         * @throws NullPointerException if the window is null.
         */
        public GenericDataSet(Window window) throws NullPointerException {
            super(window.getName());
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
                if (this.mainTable == null) this.regenerate();
            } catch (Exception e) {
                NullPointerException npe = new NullPointerException("Unable to regenerate DataSet.");
                npe.initCause(e);
                throw npe;
            }
            if (this.mainTable == null)
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
            // Clear our set of tables.
            this.tables.clear();
            
            // Set up maps of interesting relations
            // ignored relations - keys are tables, values are sets of relations
            Map ignoredRelations = new HashMap();
            // subclass and dimension relations - a simple set for each
            Set dimensionRelations = new HashSet();
            Set subclassRelations = new HashSet();
            
            // Identify main table.
            Table centralTable = this.getWindow().getCentralTable();
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
            if (centralTable.getPrimaryKey()!=null) for (Iterator i = centralTable.getPrimaryKey().getRelations().iterator(); i.hasNext(); ) {
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
                    // Mark all OneToManys from the subclass table as dimensions.
                    if (subclassTable.getPrimaryKey()!=null) for (Iterator j = subclassTable.getPrimaryKey().getRelations().iterator(); j.hasNext(); ) {
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
            this.mainTable = this.constructTable(DataSetTableType.MAIN, centralTable, (Set)ignoredRelations.get(centralTable), null);
            
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
         *
         * @param dsTableType the dsTableType of table to create.
         * @param realTable the original table to transform.
         * @param ignoredRelations the set of relations to ignore during transformation. If null, taken as empty.
         * @param linkbackRelation the relation between the realTable and the real parent table to link to if required.
         * Null indicates no linking required. Linking takes place to the dataset version of the real parent table.
         * @return a fully constructed and keyed up data set table.
         * @throws BuilderException if there was any trouble.
         * @throws NullPointerException if any required parameter was null.
         */
        private DataSetTable constructTable(DataSetTableType dsTableType, Table realTable, Set ignoredRelations, Relation linkbackRelation) throws BuilderException, NullPointerException {
            // Sanity check.
            if (dsTableType == null)
                throw new NullPointerException("Table type cannot be null.");
            if (realTable == null)
                throw new NullPointerException("Source table cannot be null.");
            // Sensible defaults.
            if (ignoredRelations == null)
                ignoredRelations = new HashSet();
            
            // Do it!
            List relationsFollowed = new ArrayList(); // relations used to construct this table
            if (linkbackRelation != null) relationsFollowed.add(linkbackRelation); // don't forget the parent relation
            Set constructedColumns = new HashSet(); // constructedColumns to include in the constructed table
            List constructedPKColumns = new ArrayList(); // constructedColumns to include in the constructed table's PK
            
            // Add all masked constructedColumns to the ignore set.
            ignoredRelations.addAll(this.getWindow().getMaskedRelations());
            
            // Create the DataSetTable
            DataSetTable datasetTable = new DataSetTable(realTable.getName(), this, dsTableType);
            this.transformTable(datasetTable, realTable, ignoredRelations, relationsFollowed, constructedPKColumns, constructedColumns);
            datasetTable.setUnderlyingRelations(relationsFollowed);
            
            // Add table provider partition column if required - must be part of primary key.
            DataSetColumn childTblProvCol=null;
            if (realTable.getTableProvider() instanceof PartitionedTableProvider) {
                childTblProvCol = new TableProviderNameColumn("__tblprov", datasetTable);
                constructedPKColumns.add(childTblProvCol);
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
                        if (!parentRealTableColumn.getTable().equals(parentRealTable)) {
                            // If real column not on original main table, create child column for it add it to child table PK
                            constructedFKColumn = new WrappedColumn(parentRealTableColumn, datasetTable);
                            constructedPKColumns.add(constructedFKColumn);
                        } else {
                            // Else follow original relation FK end and find real child column and associated child column
                            int parentRealPKColPos = parentRealTable.getPrimaryKey().getColumns().indexOf(parentRealTableColumn);
                            Column childRealFKCol = (Column)linkbackRelation.getForeignKey().getColumns().get(parentRealPKColPos);
                            constructedFKColumn = (DataSetColumn)datasetTable.getColumnByName(childRealFKCol.getName());
                        }
                    } else if (parentDatasetTableColumn instanceof TableProviderNameColumn) {
                        // Else if its a tblprov column
                        // Find child tblprov column
                        constructedFKColumn = childTblProvCol;
                    } else {
                        // Else, can'table handle.
                        throw new AssertionError("Cannot handle non-Wrapped non-TableProviderName columns in foreign keys.");
                    }
                    
                    // Add child column to child FK
                    constructedFKColumns.add(constructedFKColumn);
                }
                
                // Create the foreign key.
                if (constructedFKColumns.size()>=1) {
                    ForeignKey newFK = new GenericForeignKey(constructedFKColumns);
                    datasetTable.addForeignKey(newFK);
                    // Create the relation.
                    new OneToMany(parentDatasetTable.getPrimaryKey(), newFK);
                } else throw new AssertionError("Foreign key expected but has no columns.");
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
         * @throws NullPointerException if any of the params were null.
         * @throws AlreadyExistsException if any attempt to add a column resulted in a duplicate name.
         */
        private void transformTable(DataSetTable datasetTable, Table realTable, Set ignoredRelations, List relationsFollowed, List constructedPKColumns, Set constructedColumns) throws NullPointerException, AlreadyExistsException {
            // Sanity check.
            if (datasetTable == null)
                throw new NullPointerException("Table cannot be null.");
            if (realTable == null)
                throw new NullPointerException("Source table cannot be null.");
            if (ignoredRelations == null)
                throw new NullPointerException("Ignore relations cannot be null.");
            if (relationsFollowed == null)
                throw new NullPointerException("Relations pairs cannot be null.");
            if (constructedPKColumns == null)
                throw new NullPointerException("Primary key columns cannot be null.");
            if (constructedColumns == null)
                throw new NullPointerException("Columns cannot be null.");
            
            // Find all relations from source table.
            // Additionally, find all columns in foreign keys with non-ignored relations. Exclude them.
            Collection realTableRelations = realTable.getRelations();
            Set excludedColumns = new HashSet();
            for (Iterator i = realTable.getForeignKeys().iterator(); i.hasNext(); ) {
                Key k = (Key)i.next();
                for (Iterator j = k.getRelations().iterator(); j.hasNext(); ) {
                    Relation r = (Relation)j.next();
                    if (!ignoredRelations.contains(r))
                        excludedColumns.addAll(k.getColumns());
                }
            }
            
            // Also exclude all masked columns.
            excludedColumns.addAll(this.getWindow().getMaskedColumns());
            
            // Add all non-excluded columns from source table to dataset table.
            for (Iterator i = realTable.getColumns().iterator(); i.hasNext(); ) {
                Column c = (Column)i.next();
                // If column is excluded, ignore it.
                if (excludedColumns.contains(c)) continue;
                // Otherwise add the column to our table.
                Column wc = new WrappedColumn(c, datasetTable);
                // If column is part of primary key, add the wrapped version to the primary key column set.
                if (realTable.getPrimaryKey()!=null && realTable.getPrimaryKey().getColumns().contains(c)) constructedPKColumns.add(wc);
            }
            
            // For all non-ignored rels in realTable
            for (Iterator i = realTableRelations.iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                
                // Need to recheck ignoreRefs in case has become ignored since we built our set of relations.
                if (ignoredRelations.contains(r)) continue;
                
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
                
                if (realTableRelSourceKey instanceof PrimaryKey && this.getWindow().getConcatOnlyRelations().contains(r)) {
                    // If concat-only and realTable is at primary key end of relation, concat it
                    try {
                        new ConcatRelationColumn("__concat_" + realTableRelTargetTable.getName(), datasetTable, r);
                    } catch (AssociationException e) {
                        AssertionError ae = new AssertionError("Table does not equal itself.");
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
         * Sets the {@link MartConstructor} this {@link DataSet} will be built by.
         *
         * @param martConstructor the {@link MartConstructor} for this {@link DataSet}.
         * @throws NullPointerException if the parameter is null.
         */
        public void setMartConstructor(MartConstructor martConstructor) throws NullPointerException {
            // Sanity check.
            if (martConstructor == null)
                throw new NullPointerException("Mart constructor cannot be null.");
            // Do it.
            this.martConstructor = martConstructor;
        }
        
        /**
         * Returns the {@link MartConstructor} this {@link DataSet} will be built by.
         * @return the {@link MartConstructor} for this {@link DataSet}.
         */
        public MartConstructor getMartConstructor() {
            return this.martConstructor;
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
            if (this.martConstructor == null)
                throw new BuilderException("Cannot construct mart as no mart constructor has been specified.");
            // Do it.
            this.martConstructor.constructMart(this);
        }
        
        /**
         * Adds a {@link Table} to this provider. The table must not be null, and
         * must not already exist (ie. with the same name). The table must be an
         * instance of {@link DataSetTable}.
         *
         * @param table the {@link Table} to add.
         * @throws AlreadyExistsException if another one with the same name already exists.
         * @throws AssociationException if the table doesn'table belong to this provider or is
         * not a {@link DataSetTable}.
         * @throws NullPointerException if the table is null.
         */
        public void addTable(Table table) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (!(table instanceof DataSetTable))
                throw new AssociationException("Table does not belong to this provider.");
            // Do it.
            super.addTable(table);
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
        
        /**
         * The constructor calls the parent {@link GenericTable} constructor. It uses a
         * {@link DataSetTableProvider} as a parent for itself. You must also supply a type that
         * describes this as a main table, dimension table, etc.
         *
         * @param name the table name.
         * @param ds the {@link DataSet} to hold this table in.
         * @param type the {@link DataSetTableType} that best describes this table.
         * @throws NullPointerException if any parameter is null.
         * @throws AlreadyExistsException if the provider, for whatever reason, refuses to
         * allow this {@link Table} to be added to it using {@link DataSetTableProvider#addTable(Table)}.
         */
        public DataSetTable(String name, DataSet ds, DataSetTableType type) throws AlreadyExistsException, NullPointerException {
            // Super call first.
            super(generateAlias(name, ds), ds);
            // Sanity check.
            if (type == null)
                throw new NullPointerException("Table type cannot be null.");
            // Do the work.
            this.type = type;
        }
        
        /**
         * Internal method that generates a safe alias/name for a table. The first try is
         * always the original table name, followed by attempts with an underscore and a
         * sequence number appended.
         *
         * @param name the first name to try.
         * @param ds the {@link DataSet} that this table is being added to.
         * @return the result.
         * @throws NullPointerException if any parameter is null.
         */
        protected static String generateAlias(String name, DataSet ds) throws NullPointerException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException("Table name cannot be null.");
            if (ds == null)
                throw new NullPointerException("Parent dataset cannot be null.");
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
         *
         * @param relations the list of relations of this table. May be empty but never null.
         * @throws NullPointerException if the list was null.
         * @throws IllegalArgumentException if the list wasn'table a list of Key/Relation pairs.
         */
        public void setUnderlyingRelations(List relations) throws NullPointerException, IllegalArgumentException {
            // Sanity check.
            if (relations == null)
                throw new NullPointerException("Relations list cannot be null.");
            // Check the relations and save them.
            this.underlyingRelations.clear();
            for (Iterator i = relations.iterator(); i.hasNext(); ) {
                Object o = i.next();
                if (o == null || !(o instanceof Relation))
                    throw new IllegalArgumentException("Relations must be only Relation instances, they cannot be null or anything else.");
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
         * <p>Attemps to add a {@link Column} to this table. The {@link Column} will already
         * have had it's {@link Table} parameter set to match, otherwise an
         * {@link IllegalArgumentException} will be thrown. That exception will also get thrown
         * if the {@link Column} has the same name as an existing one on this table.</p>
         *
         * <p>{@link DataSetTable}s insist that all columns are {@link DataSetColumn}s.</p>
         *
         * @param column the {@link Column} to add, which must be a {@link DataSetColumn}..
         * @throws AlreadyExistsException if the {@link Column} name has already been used on
         * this {@link Table}.
         * @throws AssociationException if the {@link Table} parameter of the {@link Column}
         * does not match.
         * @throws NullPointerException if the {@link Column} object is null.
         */
        public void addColumn(Column column) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (!(column instanceof DataSetColumn))
                throw new AssociationException("Column must be a DataSetColumn to be added to a DataSetTable.");
            // Do the work.
            super.addColumn(column);
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
         *
         * @param column the {@link Column} to wrap and add.
         * @throws NullPointerException if the argument is null.
         */
        public void createColumn(Column column) throws NullPointerException {
            try {
                new WrappedColumn(column, this);
                // By creating it we've already added it to ourselves! (Based on DataSetColumn behaviour)
            } catch (AlreadyExistsException e) {
                AssertionError ae = new AssertionError("Alias generator failed to generate a unique alias for this column.");
                ae.initCause(e);
                throw ae;
            }
        }
        
        /**
         * {@link DataSetTable}s can be renamed by the user if the names don'table make
         * any sense to them. Use this method to do just that. It will check first to see if the proposed
         * name has already been used in the data set {@link TableProvider}. If it has, an AlreadyExistsException
         * will be thrown, otherwise the change will be made. The new name must not be null.
         *
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
                if (c instanceof TableProviderNameColumn && ((DataSet)this.getTableProvider()).getWindow().getPartitionOnTableProvider()) {
                    cols.add(c);
                } else if (c instanceof WrappedColumn) {
                    WrappedColumn wc = (WrappedColumn)c;
                    if (((DataSet)this.getTableProvider()).getWindow().getPartitionedColumns().contains(wc)) cols.add(wc);
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
         *
         * @param name the name to give this column.
         * @param dsTable the parent {@link Table}
         * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
         * that name.
         * @throws NullPointerException if either parameter is null.
         */
        public DataSetColumn(String name, DataSetTable dsTable) throws NullPointerException, AlreadyExistsException {
            super(generateAlias(name, dsTable), dsTable);
        }
        
        /**
         * Columns in {@link DataSetTable}s can be renamed by the user if the names don'table make
         * any sense to them. Use this method to do just that. It will check first to see if the proposed
         * name has already been used on this {@link DataSetTable}. If it has, an AlreadyExistsException
         * will be thrown, otherwise the change will be made. The new name must not be null.
         *
         * @param name the new name for the column.
         * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
         * that name.
         * @throws NullPointerException if the new name is null.
         */
        public void setName(String name) throws NullPointerException, AlreadyExistsException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException("New name cannot be null.");
            if (table.getColumnByName(name) != null)
                throw new AlreadyExistsException("A column with that name already exists on this table.", name);
            // Do it.
            this.name = name;
        }
        
        /**
         * Internal method that generates a safe alias/name for a column. The first try is
         * always the original column name, followed by attempts with an underscore and a
         * sequence number appended.
         *
         * @param name the first name to try.
         * @param dsTable the {@link DataSetTable} that this column is being added to.
         * @return the result.
         * @throws NullPointerException if any parameter is null.
         */
        protected static String generateAlias(String name, DataSetTable dsTable) throws NullPointerException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException("Column name cannot be null.");
            if (dsTable == null)
                throw new NullPointerException("Parent table cannot be null.");
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
             * @throws NullPointerException if either parameter is null.
             * @throws AlreadyExistsException if the world has stopped turning. (Should never happen.)
             */
            public WrappedColumn(Column column, DataSetTable dsTable) throws NullPointerException, AlreadyExistsException {
                // Call the parent with the alias.
                super(column.getName(), dsTable);
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
             * Internal reference to the relation this column refers to.
             */
            private final Relation concatRelation;
            
            /**
             * The constructor takes a name for this column-to-be, and the {@link DataSetTable}
             * on which it is to be constructed, and the {@link Relation} it represents.
             *
             * @param name the name to give this column.
             * @param dsTable the dsTable {@link Table}.
             * @param concatRelation the concat only {@link Relation} it refers to.
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws AssociationException if the {@link Relation} is not a concat relation.
             * @throws NullPointerException if any parameter is null.
             */
            public ConcatRelationColumn(String name, DataSetTable dsTable, Relation concatRelation) throws NullPointerException, AlreadyExistsException, AssociationException {
                // Super first.
                super(name, dsTable);
                // Sanity check.
                if (concatRelation == null)
                    throw new NullPointerException("Relation cannot be null.");
                if (!((DataSet)dsTable.getTableProvider()).getWindow().getConcatOnlyRelations().contains(concatRelation))
                    throw new AssociationException("This relation is not a concat relation.");
                // Do it.
                this.concatRelation = concatRelation;
            }
            
            /**
             * Returns the {@link Relation} this column refers to.
             * @return the {@link Relation} for this column.
             */
            public Relation getRelation() {
                return this.concatRelation;
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
             *
             * @param name the name to give this column.
             * @param dsTable the parent {@link Table}
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws NullPointerException if either parameter is null.
             */
            public TableProviderNameColumn(String name, DataSetTable dsTable) throws NullPointerException, AlreadyExistsException {
                super(name, dsTable);
            }
        }
    }
    
    /**
     * This class defines the various different types of DataSetTable there are.
     */
    public class DataSetTableType implements Comparable {
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
         * Displays the name of this {@link DataSetTableType} object.
         * @return the name of this {@link DataSetTableType} object.
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
            return o == this;
        }
    }
}
