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

import org.biomart.builder.resources.Resources;

/**
 * A column is a simple representation of a column in some table. It has a name,
 * and knows which table it belongs to, but apart from that knows nothing much
 * else.
 * <p>
 * A {@link GenericColumn} class is provided for ease of implementation. It
 * provides a simple storage/retrieval mechanism for the parent table and column
 * name.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
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
	 * Retrieve the original name of this column, as it was first created. This
	 * is useful when the user has renamed a column but you need to know what it
	 * started out as for purposes of comparison.
	 * 
	 * @return the original name of this column.
	 */
	public String getOriginalName();

	/**
	 * Retrieve the parent table of this column.
	 * 
	 * @return the parent table of this column.
	 */
	public Table getTable();

	/**
	 * Use this to rename a column. The table will be informed of the change as
	 * well.
	 * 
	 * @param newName
	 *            the new name to give the column.
	 */
	public void setName(String newName);

	/**
	 * Use this to change a column's original name. Use with extreme caution.
	 * The owning table doesn't need to know so won't be notified.
	 * 
	 * @param newName
	 *            the new original name to give the column.
	 */
	public void setOriginalName(String newName);

	/**
	 * A generic implementation which provides the basic functionality required
	 * for a column to function.
	 */
	public class GenericColumn implements Column {
		private String name;

		private String originalName;

		private final Table table;

		/**
		 * This constructor creates a column and remembers the name and parent
		 * table.
		 * 
		 * @param name
		 *            the name of the column to create.
		 * @param table
		 *            the parent table.
		 */
		public GenericColumn(String name, final Table table) {
			// Remember the values.
			this.table = table;
			// Remember the name, and set it as the original name for
			// this column.
			name = this.makeUniqueName(name);
			this.name = name;
			this.originalName = name;
			// Add it to the table.
			table.addColumn(this);
		}

		private String makeUniqueName(String name) {
			// Is it the same as our existing name? Reuse it.
			if (this.name != null && this.name.equals(name))
				return name;
			// First we need to find out the base name, ie. the bit
			// we append numbers to make it unique, but before any
			// key suffix. If we appended numbers after the key
			// suffix then it would confuse MartEditor.
			String suffix = "";
			String baseName = name;
			if (name.endsWith(Resources.get("keySuffix"))) {
				suffix = Resources.get("keySuffix");
				baseName = name.substring(0, name.indexOf(Resources
						.get("keySuffix")));
			}
			// Now simply check to see if the name is used, and
			// then add an incrementing number to it until it is unique.
			for (int i = 1; table.getColumnByName(name) != null; name = baseName
					+ "_" + i++ + suffix)
				;
			// Return it.
			return name;
		}

		public int compareTo(final Object o) {
			final Column c = (Column) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Column))
				return false;
			final Column c = (Column) o;
			return c.toString().equals(this.toString());
		}

		public String getName() {
			return this.name;
		}

		public String getOriginalName() {
			return this.originalName;
		}

		public Table getTable() {
			return this.table;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public void setName(String newName) {
			// Make the name unique.
			newName = this.makeUniqueName(newName);
			// Rename it.
			this.getTable().changeColumnMapKey(this.name, newName);
			this.name = newName;
		}

		public void setOriginalName(final String newName) {
			this.originalName = newName;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The format of the returned string is the output of
		 * {@link Table#toString()} for the parent table, followed by a colon
		 * and the output of {@link #getName()}.
		 */
		public String toString() {
			return this.getTable().toString() + ":" + this.getName();
		}
	}
}
