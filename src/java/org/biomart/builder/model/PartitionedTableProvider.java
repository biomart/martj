/*
 * PartitionedTableProvider.java
 *
 * Created on 27 March 2006, 11:49
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
import org.biomart.builder.model.Key.CompoundForeignKey;
import org.biomart.builder.model.Key.CompoundKey;
import org.biomart.builder.model.Key.CompoundPrimaryKey;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Key.SimpleForeignKey;
import org.biomart.builder.model.Key.SimpleKey;
import org.biomart.builder.model.Key.SimplePrimaryKey;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Table.GenericTable;

/**
 * <p>A {@link PartitionedTableProvider} represents a collection of {@link TableProvider} objects
 * which all have exactly the same table names and column names. It assigns each of them
 * a name by which they can be referred to later.</p>
 *
 * <p>When generating a mart from this later, the mart will act as though the main table has an
 * extra column containing the names of these partitions, and has been set to partition itself on
 * those values.</p>
 *
 * <p>As the {@link PartitionedTableProvider} is a {@link TableProvider} itself, all operations on it
 * which modify the structure of the tables are passed on to each of its member {@link TableProvider}
 * objects in turn.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 30th March 2006
 * @since 0.1
 */
public interface PartitionedTableProvider extends TableProvider {
    /**
     * Returns the label->provider map of {@link TableProvider} members of this partition. It will
     * never return null but may return an empty map.
     * @return the map of {@link TableProvider} options living in the partitions. Each key of the map
     * is the label for the partition, with the values being the providers themselves..
     */
    public Map getTableProviders();
    
    /**
     * Retrieves the {@link TableProvider} with the given label from this partition. If it is not recognised,
     * an exception is thrown.
     * @param label the label for the partition provider to retrieve.
     * @return the matching provider.
     * @throws NullPointerException if the label is null.
     * @throws AssociationException if the label is not recognised.
     */
    public TableProvider getTableProvider(String label) throws NullPointerException, AssociationException;
    
    /**
     * Adds a {@link TableProvider} to this partition with the given label. If it is already here,
     * it throws an exception to say so. No check is made to see if the new {@link TableProvider}
     * is actually identical to the base one in terms of structure. An exception will be thrown if
     * you try to nest {@link PartitionedTableProvider}s inside other ones.
     * @param label the label for the partition this {@link TableProvider} represents.
     * @param tp the {@link TableProvider} to add as a new partition.
     * @throws NullPointerException if the label or the provider are null.
     * @throws AlreadyExistsException if the provider has already been set as a partition here.
     * @throws AssociationException if the provider to be added is a {@link PartitionedTableProvider}.
     */
    public void addTableProvider(String label, TableProvider tp) throws AlreadyExistsException, NullPointerException, AssociationException;
    
    /**
     * Removes the {@link TableProvider} with the given label from this partition. If it is not recognised,
     * it is quietly ignored.
     * @param label the label for the partition to remove.
     * @throws NullPointerException if the label is null.
     */
    public void removeTableProvider(String label) throws NullPointerException;
    
    /**
     * The generic implementation uses a simple map to do the work.
     */
    public class GenericPartitionedTableProvider extends GenericTableProvider implements PartitionedTableProvider {
        /**
         * Internal reference to the map of member providers. Keys are the partition labels.
         */
        private final Map memberProviders = new HashMap();
        
        /**
         * The constructor creates a partitioned provider with the given name.
         * @param name the name for this new partitioned provider.
         * @throws NullPointerException if the name is null.
         */
        public GenericPartitionedTableProvider(String name) throws NullPointerException {
            super(name);
        }
        
        /**
         * <p>Tests the connection between this {@link TableProvider} and the data source that is
         * providing its tables. It will return without throwing any exceptions if the connection
         * is OK. If there is a problem with the connection, a SQLException will be thrown
         * detailing the problems.</p>
         *
         * <p>The partitioned provider simply delegates this call to each of its members in turn.</p>
         *
         * @throws SQLException if there is a problem connecting to the data source..
         */
        public void testConnection() throws SQLException {
            for (Iterator i = this.memberProviders.values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                tp.testConnection();
            }
        }
        
        /**
         * <p>Synchronise this {@link TableProvider} with the data source that is
         * providing its tables. Synchronisation means checking the list of {@link Table}s
         * available and drop/add any that have changed, then check each {@link Column}
         * and {@link Key} and {@link Relation} and update those too.</p>
         *
         * <p>The partitioned provider simply delegates this call to each of its members in turn.
         * Then the partitioned provider's own list of {@link Table}s is updated to match the contents of the first
         * provider in the list.</p>
         *
         * @throws SQLException if there was a problem connecting to the data source.
         * @throws BuilderException if there was any other kind of problem.
         */
        public void synchronise() throws SQLException, BuilderException {
            // Synchronise.
            for (Iterator i = this.memberProviders.values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                tp.synchronise();
            }
            // Update our own list.
            // Clear it first.
            this.tables.clear();
            // Then create new tables based on first providers.
            for (Iterator i = this.getFirstTableProvider().getTables().iterator(); i.hasNext(); ) {
                Table sourceTable = (Table)i.next();
                Table targetTable = new GenericTable(sourceTable.getName(), this);
                // Columns.
                for (Iterator j = sourceTable.getColumns().iterator(); j.hasNext(); ) {
                    Column sourceColumn = (Column)j.next();
                    Column targetColumn = new GenericColumn(sourceColumn.getName(), targetTable);
                }
                // Primary key.
                PrimaryKey sourcePK = targetTable.getPrimaryKey();
                if (sourcePK!=null) {
                    List targetKeyCols = new ArrayList();
                    for (Iterator n = sourcePK.getColumns().iterator(); n.hasNext(); ) {
                        Column c = (Column)n.next();
                        targetKeyCols.add(targetTable.getColumnByName(c.getName()));
                    }
                    PrimaryKey targetPK;
                    if (sourcePK instanceof SimpleKey) targetPK = new SimplePrimaryKey((Column)targetKeyCols.toArray()[0]);
                    else if (sourcePK instanceof CompoundKey) targetPK = new CompoundPrimaryKey(targetKeyCols);
                    else throw new AssertionError("Unknown type of key.");
                    targetTable.setPrimaryKey(targetPK);
                }
                // Foreign keys.
                for (Iterator j = sourceTable.getForeignKeys().iterator(); j.hasNext(); ) {
                    ForeignKey sourceFK = (ForeignKey)j.next();
                    List targetKeyCols = new ArrayList();
                    for (Iterator n = sourceFK.getColumns().iterator(); n.hasNext(); ) {
                        Column c = (Column)n.next();
                        targetKeyCols.add(targetTable.getColumnByName(c.getName()));
                    }
                    ForeignKey targetFK;
                    if (sourceFK instanceof SimpleKey) targetFK = new SimpleForeignKey((Column)targetKeyCols.toArray()[0]);
                    else if (sourceFK instanceof CompoundKey) targetFK = new CompoundForeignKey(targetKeyCols);
                    else throw new AssertionError("Unknown type of key.");
                    targetTable.addForeignKey(targetFK);
                }
                // Save it.
                this.tables.put(targetTable.getName(), targetTable);
            }
            // Now do the relations.
            for (Iterator i = this.getFirstTableProvider().getTables().iterator(); i.hasNext(); ) {
                Table sourceTable = (Table)i.next();
                Table targetTable = this.getTableByName(sourceTable.getName());
                // PKs only - everything involves a PK somewhere.
                if (sourceTable.getPrimaryKey()!=null) for (Iterator j = sourceTable.getPrimaryKey().getRelations().iterator(); j.hasNext(); ) {
                    Relation sourceRelation = (Relation)j.next();
                    ForeignKey sourceFK = sourceRelation.getForeignKey();
                    Table sourceFKTable = sourceFK.getTable();
                    Table targetFKTable = this.getTableByName(sourceFKTable.getName());
                    // Locate the equivalent target foreign key.
                    ForeignKey targetFK = targetFKTable.getForeignKeyByName(sourceFK.getName());
                    // Create the relation.
                    Relation targetRelation = new GenericRelation(targetTable.getPrimaryKey(), targetFK, sourceRelation.getFKCardinality());
                    targetTable.getPrimaryKey().addRelation(targetRelation);
                    targetFK.addRelation(targetRelation);
                }
            }
        }
        
        /**
         * <p>Returns a set of unique values in a given column, which may include null. The
         * set returned will never be null itself.</p>
         *
         * <p>This implementation returns the combination of unique values resulting from
         * delegating the call to all its subordinates.</p>
         *
         * @param c the {@link Column} to get unique values for.
         * @return a set of unique values in a given column.
         * @throws SQLException if there was any problem loading the values.
         * @throws NullPointerException if the column was null.
         */
        public Collection getUniqueValues(Column c) throws NullPointerException, SQLException {
            // Sanity check.
            if (c==null)
                throw new NullPointerException("Column cannot be null.");
            // Do it.
            Set values = new HashSet();
            for (Iterator i = this.getTableProviders().values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                // Associate the column directly with the table when asking subordinate.
                Table t = tp.getTableByName(c.getTable().getName());
                // Look up the values with the disassociated column.
                values.addAll(tp.getUniqueValues(t.getColumnByName(c.getName())));
            }
            return values;
        }
        
        /**
         * Internal function that returns the first member partition {@link TableProvider}.
         * @return the first partition's provider.
         */
        protected TableProvider getFirstTableProvider() {
            return (TableProvider)this.memberProviders.values().toArray()[0];
        }
        
        /**
         * Returns the label->provider map of {@link TableProvider} members of this partition. It will
         * never return null but may return an empty map.
         * @return the map of {@link TableProvider} options living in the partitions. Each key of the map
         * is the label for the partition, with the values being the providers themselves..
         */
        public Map getTableProviders() {
            return this.memberProviders;
        }
        
        /**
         * Retrieves the {@link TableProvider} with the given label from this partition. If it is not recognised,
         * an exception is thrown.
         * @param label the label for the partition provider to retrieve.
         * @return the matching provider.
         * @throws NullPointerException if the label is null.
         * @throws AssociationException if the label is not recognised.
         */
        public TableProvider getTableProvider(String label) throws NullPointerException, AssociationException {
            // Sanity check.
            if (label==null)
                throw new NullPointerException("Label cannot be null.");
            if (!this.memberProviders.containsKey(label))
                throw new AssociationException("No provider has been registered with that label.");
            // Do it.
            return (TableProvider)this.memberProviders.get(label);
        }
        
        /**
         * Adds a {@link TableProvider} to this partition with the given label. If it is already here,
         * it throws an exception to say so. No check is made to see if the new {@link TableProvider}
         * is actually identical to the base one in terms of structure. An exception will be thrown if
         * you try to nest {@link PartitionedTableProvider}s inside other ones.
         * @param label the label for the partition this {@link TableProvider} represents.
         * @param tp the {@link TableProvider} to add as a new partition.
         * @throws NullPointerException if the label or the provider are null.
         * @throws AlreadyExistsException if the provider has already been set as a partition here.
         * @throws AssociationException if the provider to be added is a {@link PartitionedTableProvider}.
         */
        public void addTableProvider(String label, TableProvider tp) throws AlreadyExistsException, NullPointerException, AssociationException {
            // Sanity check.
            if (label==null)
                throw new NullPointerException("Label cannot be null.");
            if (tp==null)
                throw new NullPointerException("Table provider cannot be null.");
            if (this.memberProviders.containsKey(label))
                throw new AlreadyExistsException("A provider has already been registered with that label.", label);
            if (tp instanceof PartitionedTableProvider)
                throw new AssociationException("You cannot nest partitioned table providers within other ones.");
            // Do it.
            this.memberProviders.put(label,tp);
        }
        
        /**
         * Removes the {@link TableProvider} with the given label from this partition. If it is not recognised,
         * it is quietly ignored.
         * @param label the label for the partition to remove.
         * @throws NullPointerException if the label is null.
         */
        public void removeTableProvider(String label) throws NullPointerException {
            // Sanity check.
            if (label==null)
                throw new NullPointerException("Label cannot be null.");
            // Do we need to do it?
            if (!this.memberProviders.containsKey(label)) return;
            // Do it.
            this.memberProviders.remove(label);
        }
        
        /**
         * Displays the name of this {@link PartitionedTableProvider} object. The name is the concatenation
         * of all the member {@link TableProvider}s, contained in square brackets and comma separated.
         * @return the name of this {@link PartitionedTableProvider} object.
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            for (Iterator i = this.memberProviders.values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                sb.append(tp.toString());
                if (i.hasNext()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
