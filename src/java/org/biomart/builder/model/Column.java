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

import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.MartBuilderInternalError;

/**
 * <p>
 * A column is a simple representation of a column in some table. It has a name,
 * and knows which table it belongs to, but apart from that knows nothing much
 * else.
 * <p>
 * A {@link GenericColumn} class is provided for ease of implementation. It
 * provides a simple storage/retrieval mechanism for the parent table and column
 * name.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 19th May 2006
 * @since 0.1
 */
public interface Column extends Comparable {
	/**
	 * Retrieve the name of this column.
	 * 
	 * @return the name of this column.
	 */
	public String getName();

	/**
	 * Retrieve the parent table of this column.
	 * 
	 * @return the parent table of this column.
	 */
	public Table getTable();

	/**
	 * A generic implementation which provides the basic functionality required
	 * for a column to function.
	 */
	public class GenericColumn implements Column {
		private final Table table;

		private String name;

		/**
		 * This constructor creates a column and remembers the name and parent
		 * table.
		 * 
		 * @param name
		 *            the name of the column to create.
		 * @param table
		 *            the parent table.
		 * @throws AlreadyExistsException
		 *             if it was unable to add the column to the parent table
		 *             because a column with that name already exists.
		 */
		public GenericColumn(String name, Table table)
				throws AlreadyExistsException {
			// Remember the values.
			this.name = name;
			this.table = table;

			// Add it to the table - throws AssociationException and
			// AlreadyExistsException
			try {
				table.addColumn(this);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}
		}

		public String getName() {
			return this.name;
		}

		public Table getTable() {
			return this.table;
		}

		/**
		 * Use this to rename a column from within a subclass. Use with care as
		 * the table will no longer recognise this column unless explicitly told
		 * about it.
		 * 
		 * @param newName
		 *            the new name to give the column.
		 */
		protected void setName(String newName) {
			this.name = newName;
		}

		public String toString() {
			return this.getTable().toString() + ":" + this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			Column c = (Column) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			if (o == null || !(o instanceof Column))
				return false;
			Column c = (Column) o;
			return c.toString().equals(this.toString());
		}
	}
}
