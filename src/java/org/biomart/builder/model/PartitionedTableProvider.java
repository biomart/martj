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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;

/**
 * <p>A {@link PartitionedTableProvider} represents a collection of {@link TableProvider} objects
 * which all have exactly the same table names and column names. It assigns each of them
 * a name by which they can be referred to later.</p>
 *
 * <p>When generating a mart from this later, the mart will act as though the fact table has an
 * extra column containing the names of these partitions, and has been set to partition itself on
 * those values.</p>
 *
 * <p>As the {@link PartitionedTableProvider} is a {@link TableProvider} itself, all operations on it
 * which modify the structure of the tables are passed on to each of its member {@link TableProvider}
 * objects in turn.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 27th March 2006
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
     * is actually identical to the base one in terms of structure.
     * @param label the label for the partition this {@link TableProvider} represents.
     * @param tp the {@link TableProvider} to add as a new partition.
     * @throws NullPointerException if the label or the provider are null.
     * @throws AlreadyExistsException if the provider has already been set as a partition here.
     */
    public void addTableProvider(String label, TableProvider tp) throws AlreadyExistsException, NullPointerException;
    
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
        private final Map members = new HashMap();
        
        /**
         * The constructor creates a partitioned provider with the given name.
         * @param name the name for this new partitioned provider.
         * @throws NullPointerException if the name is null.
         */
        public GenericPartitionedTableProvider(String name) {
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
            for (Iterator i = this.members.values().iterator(); i.hasNext(); ) {
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
         * <p>The partitioned provider simply delegates this call to each of its members in turn.</p>
         *
         * @throws SQLException if there was a problem connecting to the data source.
         * @throws BuilderException if there was any other kind of problem.
         */
        public void synchronise() throws SQLException, BuilderException {
            for (Iterator i = this.members.values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                tp.synchronise();
            }
        }
        
        /**
         * Adds a {@link Table} to this provider. If the {@link Table} already exists
         * an exception will be thrown. As this provider is partitioned, the call is delegated
         * to every member in turn.
         * @param t the {@link Table} to add.
         * @throws AlreadyExistsException if a {@link Table} with the same name already
         * exists in this {@link TableProvider}.
         * @throws NullPointerException if the {@link Table} argument is null.
         * @throws AssociationException if the {@link TableProvider} provided by the
         * {@link Table} object is not this same provider.
         */
        public void addTable(Table t) throws AlreadyExistsException, AssociationException, NullPointerException {
            for (Iterator i = this.members.values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                tp.addTable(t);
            }
        }
        
        /**
         * Convenience method that creates and adds a {@link Table} to this provider.
         * If a {@link Table} with the same name already exists an exception will be thrown.
         * As this provider is partitioned, the call is delegated to every member in turn.
         * @param name the name of the {@link Table} to create and add.
         * @throws AlreadyExistsException if a {@link Table} with the same name already
         * exists in this {@link TableProvider}.
         * @throws NullPointerException if the name argument is null.
         */
        public void createTable(String name) throws AlreadyExistsException, NullPointerException {
            for (Iterator i = this.members.values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                tp.createTable(name);
            }
            
        }
        
        /**
         * Returns all the {@link Table}s this provider provides. The set returned may be
         * empty but it will never be null. As this provider is partitioned, the call is delegated to the
         * first member of the set as all the others are identical. If there are no members, an empty
         * set is returned.
         * @return the set of all {@link Table}s in this provider.
         */
        public Collection getTables() {
            if (this.members.isEmpty()) return Collections.EMPTY_SET;
            else return this.getFirstTableProvider().getTables();
        }
        
        /**
         * Returns the {@link Table}s from this provider with the given name. If there is
         * no such table, the method will return null. As this provider is partitioned, the call is delegated
         * to the first member of the set as all the others are identical.
         * @param name the name of the {@link Table} to retrieve.
         * @return the matching {@link Table}s from this provider.
         */
        public Table getTableByName(String name) {
            if (this.members.isEmpty()) return null;
            else return this.getFirstTableProvider().getTableByName(name);
        }
        
        /**
         * Internal function that returns the first member partition {@link TableProvider}.
         * @return the first partition's provider.
         */
        protected TableProvider getFirstTableProvider() {
            return (TableProvider)this.members.values().toArray()[0];
        }
        
        /**
         * Returns the label->provider map of {@link TableProvider} members of this partition. It will
         * never return null but may return an empty map.
         * @return the map of {@link TableProvider} options living in the partitions. Each key of the map
         * is the label for the partition, with the values being the providers themselves..
         */
        public Map getTableProviders() {
            return this.members;
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
            if (!this.members.containsKey(label))
                throw new AssociationException("No provider has been registered with that label.");
            // Do it.
            return (TableProvider)this.members.get(label);
        }
        
        /**
         * Adds a {@link TableProvider} to this partition with the given label. If it is already here,
         * it throws an exception to say so. No check is made to see if the new {@link TableProvider}
         * is actually identical to the base one in terms of structure.
         * @param label the label for the partition this {@link TableProvider} represents.
         * @param tp the {@link TableProvider} to add as a new partition.
         * @throws NullPointerException if the label or the provider are null.
         * @throws AlreadyExistsException if the provider has already been set as a partition here.
         */
        public void addTableProvider(String label, TableProvider tp) throws AlreadyExistsException, NullPointerException {
            // Sanity check.
            if (label==null)
                throw new NullPointerException("Label cannot be null.");
            if (tp==null)
                throw new NullPointerException("Table provider cannot be null.");
            if (this.members.containsKey(label))
                throw new AlreadyExistsException("A provider has already been registered with that label.", label);
            // Do it.
            this.members.put(label,tp);
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
            if (!this.members.containsKey(label)) return;
            // Do it.
            this.members.remove(label);
        }
        
        /**
         * Displays the name of this {@link PartitionedTableProvider} object. The name is the concatenation
         * of all the member {@link TableProvider}s, contained in square brackets and comma separated.
         * @return the name of this {@link PartitionedTableProvider} object.
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            for (Iterator i = this.members.values().iterator(); i.hasNext(); ) {
                TableProvider tp = (TableProvider)i.next();
                sb.append(tp.toString());
                if (i.hasNext()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
