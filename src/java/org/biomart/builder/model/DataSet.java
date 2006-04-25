/*
 * DataSet.java
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
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.model.TableProvider.GenericTableProvider;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This is the heart of the whole system, and represents a single data set in a mart.
 * The generic implementation includes the algorithm which flattens tables down into
 * a set of mart tables based on the contents of a {@link Window}.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 25th April 2006
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
     * @param martConstructor the {@link MartConstructor} for this {@link DataSet}.
     * @throws NullPointerException if the parameter is null.
     */
    public void setMartConstructor(MartConstructor martConstructor) throws NullPointerException;
    
    /**
     * Returns the {@link MartConstructor} this {@link DataSet} will be built by.
     * Defaults to NONE and is never null.
     * @return the {@link MartConstructor} for this {@link DataSet}.
     */
    public MartConstructor getMartConstructor();
    
    /**
     * Sets the {@link DataSetOptimiserType} this {@link DataSet} will be optimised with.
     * Defaults to NONE and is never null.
     * @param optimiser the {@link DataSetOptimiserType} for this {@link DataSet}.
     * @throws NullPointerException if the parameter is null.
     */
    public void setDataSetOptimiserType(DataSetOptimiserType optimiser) throws NullPointerException;
    
    /**
     * Returns the {@link DataSetOptimiserType} this {@link DataSet} will be optimised with.
     * @return the {@link DataSetOptimiserType} for this {@link DataSet}.
     */
    public DataSetOptimiserType getDataSetOptimiserType();
    
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
         * Internal reference to the dataset optimiser that will optimise us later.
         * Defaults to none.
         */
        private DataSetOptimiserType optimiser = DataSetOptimiserType.NONE;
        
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
         * {@inheritDoc}
         */
        public Window getWindow() {
            return this.window;
        }
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            return this.getWindow().getName();
        }
        
        /**
         * {@inheritDoc}
         */
        public DataSetTable getMainTable() throws NullPointerException {
            // Sanity check.
            try {
                if (this.mainTable == null) this.regenerate();
            } catch (Exception e) {
                NullPointerException npe = new NullPointerException(BuilderBundle.getString("datasetRegenerationFailed"));
                npe.initCause(e);
                throw npe;
            }
            if (this.mainTable == null)
                throw new NullPointerException(BuilderBundle.getString("mainTableFailed"));
            // Do it.
            return this.mainTable;
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
            
            // Identify all subclass and dimension relations.
            ignoredRelations.put(centralTable, new HashSet());
            if (centralTable.getPrimaryKey()!=null) for (Iterator i = centralTable.getPrimaryKey().getRelations().iterator(); i.hasNext(); ) {
                Relation r = (Relation)i.next();
                // Skip masked relations.
                if (this.getWindow().getMaskedRelations().contains(r)) continue;
                // Skip inferred-incorrect relations.
                if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)) continue;
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
                        if (sr.getFKCardinality().equals(Cardinality.MANY)) {
                            ((Set)ignoredRelations.get(subclassTable)).add(sr);
                            dimensionRelations.add(sr);
                            Table dimTable = sr.getForeignKey().getTable();
                            if (!ignoredRelations.containsKey(dimTable)) ignoredRelations.put(dimTable, new HashSet());
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
                throw new NullPointerException(BuilderBundle.getString("tableTypeIsNull"));
            if (realTable == null)
                throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
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
            
            // Add table provider partition column if required.
            DataSetColumn tblProvCol=null;
            if (realTable.getTableProvider() instanceof PartitionedTableProvider) {
                tblProvCol = new TableProviderNameColumn(BuilderBundle.getString("tblprovColumnName"), datasetTable);
                // Only add to PK if PK is not empty, as otherwise will introduce a PK where none previously existed.
                if (!constructedPKColumns.isEmpty()) constructedPKColumns.add(tblProvCol);
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
                            // relation - if null, then it was on the original table or subclass table, if not null, 
                            // then it came from somewhere else.
                            constructedFKColumn = new WrappedColumn(parentRealTableColumn, datasetTable, linkbackRelation);
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
                        constructedFKColumn = tblProvCol;
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
         * @throws NullPointerException if any of the params were null.
         * @throws AlreadyExistsException if any attempt to add a column resulted in a duplicate name.
         */
        private void transformTable(DataSetTable datasetTable, Table realTable, Set ignoredRelations, List relationsFollowed, List constructedPKColumns, Set constructedColumns) throws NullPointerException, AlreadyExistsException {
            // Sanity check.
            if (datasetTable == null)
                throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
            if (realTable == null)
                throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
            if (ignoredRelations == null)
                throw new NullPointerException(BuilderBundle.getString("ignoreRelationsIsNull"));
            if (relationsFollowed == null)
                throw new NullPointerException(BuilderBundle.getString("relationsIsNull"));
            if (constructedPKColumns == null)
                throw new NullPointerException(BuilderBundle.getString("columnsIsNull"));
            if (constructedColumns == null)
                throw new NullPointerException(BuilderBundle.getString("columnsIsNull"));
            
            // Find all relations from source table.
            // Additionally, find all columns in all keys with non-ignored relations. Exclude them,
            // except if they're in a concat-only relation, in which case keep them.
            Collection realTableRelations = realTable.getRelations();
            Set excludedColumns = new HashSet();
            for (Iterator i = realTable.getKeys().iterator(); i.hasNext(); ) {
                Key k = (Key)i.next();
                for (Iterator j = k.getRelations().iterator(); j.hasNext(); ) {
                    Relation r = (Relation)j.next();
                    if (!ignoredRelations.contains(r) && !this.getWindow().getConcatOnlyRelations().contains(r))
                        excludedColumns.addAll(k.getColumns());
                }
            }
            
            // Also exclude all masked columns.
            excludedColumns.addAll(this.getWindow().getMaskedColumns());
            
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
                
                if (realTableRelSourceKey instanceof PrimaryKey && this.getWindow().getConcatOnlyRelations().contains(r)) {
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
        public void setMartConstructor(MartConstructor martConstructor) throws NullPointerException {
            // Sanity check.
            if (martConstructor == null)
                throw new NullPointerException(BuilderBundle.getString("martConstructorIsNull"));
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
        public void setDataSetOptimiserType(DataSetOptimiserType optimiser) throws NullPointerException {
            // Sanity check.
            if (optimiser == null)
                throw new NullPointerException(BuilderBundle.getString("optimiserIsNull"));
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
        public void addTable(Table table) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (!(table instanceof DataSetTable))
                throw new AssociationException(BuilderBundle.getString("tableTblProvMismatch"));
            // Do it.
            super.addTable(table);
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
                throw new NullPointerException(BuilderBundle.getString("tableTypeIsNull"));
            // Do the work.
            this.type = type;
        }
        
        /**
         * Internal method that generates a safe alias/name for a table. The first try is
         * always the original table name, followed by attempts with an underscore and a
         * sequence number appended.
         * @param name the first name to try.
         * @param ds the {@link DataSet} that this table is being added to.
         * @return the result.
         * @throws NullPointerException if any parameter is null.
         */
        protected static String generateAlias(String name, DataSet ds) throws NullPointerException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            if (ds == null)
                throw new NullPointerException(BuilderBundle.getString("datasetIsNull"));
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
         * @throws NullPointerException if the list was null.
         * @throws IllegalArgumentException if the list wasn'table a list of Key/Relation pairs.
         */
        public void setUnderlyingRelations(List relations) throws NullPointerException, IllegalArgumentException {
            // Sanity check.
            if (relations == null)
                throw new NullPointerException(BuilderBundle.getString("relationsIsNull"));
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
        public void addColumn(Column column) throws AlreadyExistsException, AssociationException, NullPointerException {
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
        public void createColumn(String name) throws AlreadyExistsException, NullPointerException {
            throw new AssertionError(BuilderBundle.getString("datasetTableCreateColumnReject"));
        }
        
        /**
         * {@link DataSetTable}s can be renamed by the user if the names don'table make
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
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            if (name.equals(this.name)) return; // Skip unnecessary change.
            if (this.getTableProvider().getTableByName(name)!=null)
                throw new AlreadyExistsException(BuilderBundle.getString("nameAExists"), name);
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
         * Internal reference to the {@link Relation} that produces this column.
         */
        private final Relation underlyingRelation;
        
        /**
         * This constructor gives the column a name and checks that the
         * parent {@link Table} is not null.
         * @param name the name to give this column.
         * @param dsTable the parent {@link Table}
         * @param underlyingRelation the {@link Relation} that provided this column. The
         * underlying relation can be null in two instances - when the {@link DataSetTable} is
         * a MAIN table, in which case the column came from the parent table, and all
         * other instances, in which case the column came from the MAIN table in this
         * {@link DataSet}.
         * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
         * that name.
         * @throws NullPointerException if either parameter is null.
         */
        public DataSetColumn(String name, DataSetTable dsTable, Relation underlyingRelation) throws NullPointerException, AlreadyExistsException {
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
         * @throws NullPointerException if the new name is null.
         */
        public void setName(String name) throws NullPointerException, AlreadyExistsException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            if (name.equals(this.name)) return; // Skip unnecessary change.
            if (table.getColumnByName(name) != null)
                throw new AlreadyExistsException(BuilderBundle.getString("nameExists"), name);
            // Do it.
            this.name = name;
        }
        
        /**
         * Internal method that generates a safe alias/name for a column. The first try is
         * always the original column name, followed by attempts with an underscore and a
         * sequence number appended.
         * @param name the first name to try.
         * @param dsTable the {@link DataSetTable} that this column is being added to.
         * @return the result.
         * @throws NullPointerException if any parameter is null.
         */
        protected static String generateAlias(String name, DataSetTable dsTable) throws NullPointerException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            if (dsTable == null)
                throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
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
             * @param column the {@link Column} to wrap.
             * @param dsTable the parent {@link Table}
             * @param underlyingRelation the {@link Relation} that provided this column. The
             * underlying relation can be null in two instances - when the {@link DataSetTable} is
             * a MAIN table, in which case the column came from the parent table, and all
             * other instances, in which case the column came from the MAIN table in this
             * {@link DataSet}.
             * @throws NullPointerException if either parameter is null.
             * @throws AlreadyExistsException if the world has stopped turning. (Should never happen.)
             */
            public WrappedColumn(Column column, DataSetTable dsTable, Relation underlyingRelation) throws NullPointerException, AlreadyExistsException {
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
             * @param name the name to give this column.
             * @param dsTable the dsTable {@link Table}.
             * @param underlyingRelation the {@link Relation} that provided this column. The
             * underlying relation can be null in two instances - when the {@link DataSetTable} is
             * a MAIN table, in which case the column came from the parent table, and all
             * other instances, in which case the column came from the MAIN table in this
             * {@link DataSet}.
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws AssociationException if the {@link Relation} is not a concat relation.
             * @throws NullPointerException if any parameter is null.
             */
            public ConcatRelationColumn(String name, DataSetTable dsTable, Relation concatRelation) throws NullPointerException, AlreadyExistsException, AssociationException {
                // Super first.
                super(name, dsTable, concatRelation);
                // Sanity check.
                if (!((DataSet)dsTable.getTableProvider()).getWindow().getConcatOnlyRelations().contains(concatRelation))
                    throw new AssociationException(BuilderBundle.getString("relationNotConcatRelation"));
            }
        }
        
        /**
         * A column on a {@link DataSetTable} that should be populated with the name
         * of the table provider providing the data in this row.
         */
        public static class TableProviderNameColumn extends DataSetColumn {
            /**
             * This constructor gives the column a name and checks that the
             * parent {@link Table} is not null. The underlying relation is not required.
             * @param name the name to give this column.
             * @param dsTable the parent {@link Table}
             * @throws AlreadyExistsException if the {@link DataSetTable} already has a column with
             * that name.
             * @throws NullPointerException if either parameter is null.
             */
            public TableProviderNameColumn(String name, DataSetTable dsTable) throws NullPointerException, AlreadyExistsException {
                super(name, dsTable, null);
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
    public class DataSetOptimiserType implements Comparable {
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
