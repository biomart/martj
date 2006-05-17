/*
 * SchemaGroup.java
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>A {@link SchemaGroup} represents a collection of {@link Schema} objects
 * which all have exactly the same table names and column names. It assigns each of them
 * a name by which they can be referred to later.</p>
 * <p>When generating a mart from this later, the mart will act as though the main table has an
 * extra column containing the names of these partitions, and has been set to partition itself on
 * those values.</p>
 * <p>As the {@link SchemaGroup} is a {@link Schema} itself, all operations on it
 * which modify the structure of the tables are passed on to each of its member {@link Schema}
 * objects in turn.</p>
 *
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 12th May 2006
 * @since 0.1
 */
public interface SchemaGroup extends Schema {
    /**
     * Returns the label->provider map of {@link Schema} members of this partition. It will
     * never return null but may return an empty map.
     *
     * @return the map of {@link Schema} options living in the partitions. Each key of the map
     * is the label for the partition, with the values being the providers themselves..
     */
    public Collection getSchemas();
    
    /**
     * Adds a {@link Schema} to this partition with the given label. If it is already here,
     * it throws an exception to say so. No check is made to see if the new {@link Schema}
     * is actually identical to the base one in terms of structure. An exception will be thrown if
     * you try to nest {@link SchemaGroup}s inside other ones.
     *
     * @param label the label for the partition this {@link Schema} represents.
     * @param schema the {@link Schema to add as a new partition.
     * @throws AlreadyExistsException if the provider has already been set as a partition here.
     * @throws AssociationException if the provider to be added is a {@link PartSchemaGroup
     */
    public void addSchema(Schema schema) throws AlreadyExistsException, AssociationException;
    
    /**
     * Removes the {@link Schema} with the given label from this partition. If it is not recognised,
     * it is quietly ignored.
     *
     * @param label the label for the partition to remove.
     */
    public void removeSchema(Schema schema);
    
    /**
     * The generic implementation uses a simple map to do the work.
     */
    public class GenericSchemaGroup extends GenericSchema implements SchemaGroup {
        /**
         * Internal reference to the map of member providers. Keys are the partition labels.
         * Must be linked because the first one to be added must remain the same.
         */
        private final List schemas = new ArrayList();
        
        /**
         * The constructor creates a partitioned provider with the given name.
         * @param name the name for this new partitioned provider.
         */
        public GenericSchemaGroup(String name) {
            super(name);
        }
        
        public Schema replicate(String newName) {
            SchemaGroup newGroup = new GenericSchemaGroup(newName);
            try {
                for (Iterator i = this.schemas.iterator(); i.hasNext(); ) newGroup.addSchema((Schema)i.next());
            } catch (Exception e) {
                throw new AssertionError(e);
            }        
            this.replicateContents(newGroup);
            return newGroup;
        }
        
        /**
         * {@inheritDoc}
         * <p>The partitioned provider simply delegates this call to each of its members in turn.
         * Then the partitioned provider's own list of {@link Table}s is updated to match the contents of the first
         * provider in the list.</p>
         */
        public void synchronise() throws SQLException, BuilderException {
            // Synchronise our members.
            for (Iterator i = this.schemas.iterator(); i.hasNext(); ) {
                ((Schema)i.next()).synchronise();
            }
            // Update our own list.
            // Clear it first.
            this.tables.clear();
            // Then create new tables based on first providers.
            for (Iterator i = this.getFirstSchema().getTables().iterator(); i.hasNext(); ) {
                Table sourceTable = (Table)i.next();
                Table targetTable = new GenericTable(sourceTable.getName(), this);
                // Columns.
                for (Iterator j = sourceTable.getColumns().iterator(); j.hasNext(); ) {
                    Column sourceColumn = (Column)j.next();
                    Column targetColumn = new GenericColumn(sourceColumn.getName(), targetTable);
                }
                // Primary key.
                PrimaryKey sourcePK = sourceTable.getPrimaryKey();
                if (sourcePK!=null) {
                    List targetKeyCols = new ArrayList();
                    for (Iterator n = sourcePK.getColumns().iterator(); n.hasNext(); ) {
                        Column c = (Column)n.next();
                        targetKeyCols.add(targetTable.getColumnByName(c.getName()));
                    }
                    PrimaryKey targetPK = new GenericPrimaryKey(targetKeyCols);
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
                    ForeignKey targetFK = new GenericForeignKey(targetKeyCols);
                    targetTable.addForeignKey(targetFK);
                }
            }
            // Now do the relations.
            for (Iterator i = this.getFirstSchema().getTables().iterator(); i.hasNext(); ) {
                Table sourceTable = (Table)i.next();
                Table targetTable = this.getTableByName(sourceTable.getName());
                // PKs only - everything involves a PK somewhere.
                if (sourceTable.getPrimaryKey()!=null) for (Iterator j = sourceTable.getPrimaryKey().getRelations().iterator(); j.hasNext(); ) {
                    Relation sourceRelation = (Relation)j.next();
                    ForeignKey sourceFK = sourceRelation.getForeignKey();
                    Table sourceFKTable = sourceFK.getTable();
                    Table targetFKTable = this.getTableByName(sourceFKTable.getName());
                    // Locate the equivalent target foreign key.
                    ForeignKey targetFK = null;
                    for (Iterator l = targetFKTable.getForeignKeys().iterator(); l.hasNext() && targetFK==null; ) {
                        ForeignKey k = (ForeignKey)l.next();
                        if (k.getColumnNames().equals(sourceFK.getColumnNames())) targetFK = k;
                    }
                    // Create the relation.
                    new GenericRelation(targetTable.getPrimaryKey(), targetFK, sourceRelation.getFKCardinality());
                }
            }
        }
        
        /**
         * {@inheritDoc}
         * <p>This implementation returns the combination of unique values resulting from
         * delegating the call to all its subordinates.</p>
         */
        public Collection getUniqueValues(Column column) throws SQLException, AssociationException {
            // Do it.
            Set values = new HashSet();
            for (Iterator i = this.getSchemas().iterator(); i.hasNext(); ) {
                Schema s = (Schema)i.next();
                // Associate the column directly with the table when asking subordinate.
                Table t = s.getTableByName(column.getTable().getName());
                // Look up the values with the disassociated column.
                values.addAll(s.getUniqueValues(t.getColumnByName(column.getName())));
            }
            return values;
        }
        
        /**
         * {@inheritDoc}
         * <p>This implementation unfortunately has to read all the data from all the providers
         * before it can work out which ones are unique, which may be very slow.</p>
         */
        public int countUniqueValues(Column column) throws AssociationException, SQLException {
            return this.getUniqueValues(column).size();
        }
        
        /**
         * Internal function that returns the first member partition {@link Schema}.
         *
         * @return the first partition's provider.
         */
        protected Schema getFirstSchema() {
            return (Schema)this.schemas.toArray()[0];
        }
        
        /**
         * {@inheritDoc}
         */
        public Collection getSchemas() {
            return this.schemas;
        }
        
        /**
         * {@inheritDoc}
         */
        public void addSchema(Schema schema) throws AlreadyExistsException, AssociationException {
            // Sanity check.
            if (this.schemas.contains(schema))
                throw new AlreadyExistsException(BuilderBundle.getString("schemaExists"), schema.getName());
            if (schema instanceof SchemaGroup)
                throw new AssociationException(BuilderBundle.getString("nestedSchema"));
            // Do it.
            this.schemas.add(schema);
        }
        
        /**
         * {@inheritDoc}
         */
        public void removeSchema(Schema schema) {
            // Do we need to do it?
            if (!this.schemas.contains(schema)) return;
            // Do it.
            this.schemas.remove(schema);
        }
    }
}
