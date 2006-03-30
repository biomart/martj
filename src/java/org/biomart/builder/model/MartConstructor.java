/*
 * MartConstructor.java
 *
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

/**
 * This interface defines the behaviour expected from an object which can take
 * a {@link DataSet} and actually construct a mart based on this information. Whether it carries out the
 * task or just writes some DDL to be run by the user later is up to the implementor.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.3, 30th March 2006
 * @since 0.1
 */
public interface MartConstructor extends DataLink {
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
        public GenericMartConstructor(String name) {
            // Sanity check.
            if (name==null)
                throw new NullPointerException("Mart constructor name cannot be null.");
            // Remember the values.
            this.name = name;
        }
        
        /**
         * Returns the name of this {@link MartConstructor}.
         * @return the name of this constructor.
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * <p>This method takes a {@link DataSet} and either generates a script for the
         * user to run later to construct a mart, or does the work right now. The end result
         * should be a completely finished and populated mart, or the script to make one.</p>
         *
         * <p>This simple generic implementation tests for a non-null {@link DataSet}.
         * It doesn't actually generate any tables or DDL.</p>
         * @param ds the {@link DataSet} to build the mart for.
         * @throws NullPointerException if the {@link DataSet} parameter is null.
         * @throws BuilderException if anything went wrong during the building process.
         * @throws SQLException if it needed to talk to a database and couldn't.
         */
        public void constructMart(DataSet ds) throws NullPointerException, BuilderException, SQLException {
            // Sanity check.
            if (ds==null)
                throw new NullPointerException("Schema cannot be null.");
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
        }
        
        /**
         * <p>This method tests the database connection (if one is required) that will be used to
         * construct the mart inside. It returns nothing if successful, but if an error is encountered
         * then an exception is thrown.</p>
         *
         * <p>This being a generic implementation, nothing actually happens in this method.</p>
         *
         * @throws SQLException if it needed to talk to a database and couldn't.
         */
        public void testConnection() throws SQLException {}
        
        /**
         * <p>Checks to see if this {@link DataLink} 'cohabits' with another one. Cohabitation means
         * that it would be possible to write a single SQL statement that could read data from
         * both {@link DataLink}s simultaneously.</p>
         *
         * <p>The generic provider has no data source, so it will always return false.</p>
         *
         * @param partner the other {@link DataLink} to test for cohabitation.
         * @return true if the two can cohabit, false if not.
         * @throws NullPointerException if the partner is null.
         */
        public boolean canCohabit(DataLink partner) {
            return false;
        }
        
        /**
         * Displays the name of this {@link MartConstructor} object.
         * @return the name of this {@link MartConstructor} object.
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
         * @throws ClassCastException if the object o is not a {@link MartConstructor}.
         */
        public int compareTo(Object o) throws ClassCastException {
            MartConstructor c = (MartConstructor)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link MartConstructor}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof MartConstructor)) return false;
            MartConstructor c = (MartConstructor)o;
            return c.toString().equals(this.toString());
        }
    }
}
