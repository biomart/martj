/*
 * MartConstructor.java
 * Created on 28 March 2006, 13:15
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
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This interface defines the behaviour expected from an object which can take
 * a {@link DataSet} and actually construct a mart based on this information. Whether it carries out the
 * task or just writes some DDL to be run by the user later is up to the implementor.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 30th March 2006
 * @since 0.1
 */
public interface MartConstructor extends DataLink, Comparable {
    /**
     * This method takes a {@link DataSet} and either generates a script for the
     * user to run later to construct a mart, or does the work right now. The end result
     * should be a completely finished and populated mart, or the script to make one.
     * @param ds the {@link DataSet} to build the mart for.
     * @throws NullPointerException if the {@link Schema} parameter is null.
     * @throws BuilderException if anything went wrong during the building process.
     * @throws SQLException if it needed to talk to a database and couldn't.
     */
    public void constructMart(DataSet ds) throws NullPointerException, BuilderException, SQLException;
    
    /**
     * Returns the name of this {@link MartConstructor}.
     * @return the name of this constructor.
     */
    public String getName();
    
    /**
     * The base implementation simply does the bare minimum, ie. tests for the presence of
     * one or more {@link Window}s in the {@link Schema}, then synchronises them. It doesn't actually
     * generate any tables or DDL.
     */
    public class GenericMartConstructor implements MartConstructor {
        /**
         * Internal reference to the name of this constructor.
         */
        private final String name;
        
        /**
         * The constructor creates a mart constructor with the given name.
         * @param name the name for this new constructor.
         * @throws NullPointerException if the name is null.
         */
        public GenericMartConstructor(String name) throws NullPointerException {
            // Sanity check.
            if (name == null)
                throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
            // Remember the values.
            this.name = name;
        }
        
        /**
         * {@inheritDoc}
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * {@inheritDoc}
         * <p>This simple generic implementation tests for a non-null {@link DataSet}.
         * It doesn't actually generate any tables or DDL.</p>
         */
        public void constructMart(DataSet ds) throws NullPointerException, BuilderException, SQLException {
            // Sanity check.
            if (ds == null)
                throw new NullPointerException(BuilderBundle.getString("schemaIsNull"));
            // Do the work.
            // TODO: Subclasses actually generate DDL or access JDBC/XML/whatever and do the transformation.
            // Don't forget to include the 'hasXYZDimension' columns in the main table and subclassed main tables.
            // Also don't forget to left-join tables when joining so we get nulls in appropriate places.
            // Plus, check partitionOnTableProvider when dealing with PartitionedTableProvider on main table.
            // Use pseudo-column if PartitionedTableProvider and off, use partition-prefix if PartitionedTableProvider
            // and on, ignore if not PartitionedTableProvider. Applies only when partitioning main table, otherwise
            // normal rules apply. (Partition suffix on table name).
            // Check for masked columns, masked relations, concat only relations, and subclass relations.
            
            // Can partition to separate databases by being a wrapper around one or more DataSource objects per
            // partition name! This is for each implementation to decide for itself.
            
            // Use abstract delegate methods (create table as table, merge tables, etc.) which will do the work
            // and know how to be specific to a certain database..
        }
        
        /**
         * {@inheritDoc}
         * <p>The generic constructor has no data source, so it will always return false.</p>
         */
        public boolean canCohabit(DataLink partner) throws NullPointerException {
            return false;
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
            MartConstructor c = (MartConstructor)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o == null || !(o instanceof MartConstructor)) return false;
            MartConstructor c = (MartConstructor)o;
            return c.toString().equals(this.toString());
        }
    }
}
