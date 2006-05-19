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
 * This interface defines the behaviour expected from an object which can take a
 * dataset and actually construct a mart based on this information. Whether it
 * carries out the task or just writes some DDL to be run by the user later is
 * up to the implementor.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 5th May 2006
 * @since 0.1
 */
public interface MartConstructor extends DataLink, Comparable {
	/**
	 * This constant refers to a placeholder mart constructor which does nothing
	 * except prevent null pointer exceptions.
	 */
	public static final MartConstructor DUMMY_MART_CONSTRUCTOR = new GenericMartConstructor(
			"__DUMMY_MC");

	/**
	 * This method takes a dataset and either generates a script for the user to
	 * run later to construct a mart, or does the work right now. The end result
	 * should be a completely finished and populated mart, or the script to make
	 * one.
	 * 
	 * @param ds
	 *            the dataset to build the mart for.
	 * @throws BuilderException
	 *             if anything went wrong during the building process.
	 * @throws SQLException
	 *             if it needed to talk to a database and couldn't.
	 */
	public void constructMart(DataSet ds) throws BuilderException, SQLException;

	/**
	 * Returns the name of this constructor.
	 * 
	 * @return the name of this constructor.
	 */
	public String getName();

	/**
	 * The base implementation simply does the bare minimum, ie. synchronises
	 * the dataset before starting work. It doesn't actually generate any tables
	 * or DDL.
	 */
	public class GenericMartConstructor implements MartConstructor {
		private final String name;

		/**
		 * The constructor creates a mart constructor with the given name.
		 * 
		 * @param name
		 *            the name for this new constructor.
		 */
		public GenericMartConstructor(String name) {
			// Remember the values.
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void constructMart(DataSet ds) throws BuilderException,
				SQLException {
			/*
			 * TODO: Subclasses actually generate DDL or access
			 * JDBC/XML/whatever and do the transformation. Don't forget to
			 * include the 'hasXYZDimension' columns in the main table and
			 * subclassed main tables. Also don't forget to left-join tables
			 * when joining so we get nulls in appropriate places. Plus, check
			 * whether paritioning on any SchemaColumn instances. Use
			 * pseudo-column if PartitionedTableProvider and off, use
			 * partition-prefix if PartitionedTableProvider and on, ignore if
			 * not PartitionedTableProvider. Applies only when partitioning main
			 * table, otherwise normal rules apply. (Partition suffix on table
			 * name). Check for masked columns, masked relations, concat only
			 * relations, and subclass relations.
			 * 
			 * Can partition to separate databases by being a wrapper around one
			 * or more DataSource objects per partition name! This is for each
			 * implementation to decide for itself.
			 * 
			 * Use abstract delegate methods (create table as table, merge
			 * tables, etc.) which will do the work and know how to be specific
			 * to a certain database..
			 */
		}

		public boolean test() throws Exception {
			return true;
		}

		public boolean canCohabit(DataLink partner) {
			return false;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			MartConstructor c = (MartConstructor) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			if (o == null || !(o instanceof MartConstructor))
				return false;
			MartConstructor c = (MartConstructor) o;
			return c.toString().equals(this.toString());
		}
	}
}
