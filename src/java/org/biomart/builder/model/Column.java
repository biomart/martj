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

import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.resources.Resources;

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
 * @version 0.1.8, 2nd August 2006
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
	 * Use this to rename a column. The table will be informed of the change as
	 * well.
	 * 
	 * @param newName
	 *            the new name to give the column.
	 */
	public void setName(String newName);

	/**
	 * Use this to rename a column's original name. Use with extreme caution.
	 * The owning table doesn't need to know so won't be notified.
	 * 
	 * @param newName
	 *            the new original name to give the column.
	 */
	public void setOriginalName(String newName);

	/**
	 * Retrieve the parent table of this column.
	 * 
	 * @return the parent table of this column.
	 */
	public Table getTable();

	/**
	 * Sets whether this column is nullable or not.
	 * 
	 * @param nullable
	 *            <tt>true</tt> if it is nullable, <tt>false</tt> if not.
	 */
	public void setNullable(boolean nullable);

	/**
	 * Tests to see if this column is nullable or not.
	 * 
	 * @return <tt>true</tt> if it is nullable, <tt>false</tt> if not.
	 */
	public boolean getNullable();

	/**
	 * A generic implementation which provides the basic functionality required
	 * for a column to function.
	 */
	public class GenericColumn implements Column {
		private final Table table;

		private String originalName;

		private String name;

		private boolean nullable;

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
			this.nullable = false;
			// Make the name unique.
			String baseName = name;
			String suffix = Resources.get("pkSuffix");
			if (baseName.endsWith(suffix))
				baseName = baseName.substring(0, baseName.indexOf(suffix));
			else
				suffix = "";
			// Check there is no other column on this table with the same name.
			for (int i = 1; table.getColumnByName(name) != null; name = baseName
					+ "_" + i++ + suffix)
				;
			this.name = name;
			this.originalName = name;
			// Add it to the table - throws AssociationException.
			try {
				table.addColumn(this);
			} catch (final AssociationException e) {
				throw new MartBuilderInternalError(e);
			}
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

		public void setName(String newName) {
			// Don't duplicate effort.
			if (newName.equals(this.name))
				return;
			// Make the name unique.
			String baseName = newName;
			String suffix = Resources.get("pkSuffix");
			if (baseName.endsWith(suffix))
				baseName = baseName.substring(0, baseName.indexOf(suffix));
			else
				suffix = "";
			// Check there is no other column on this table with the same name.
			for (int i = 1; this.table.getColumnByName(newName) != null; newName = baseName
					+ "_" + i++ + suffix)
				;
			this.getTable().changeColumnMapKey(this.name, newName);
			this.name = newName;
		}

		public void setOriginalName(final String newName) {
			this.originalName = newName;
		}

		public boolean getNullable() {
			return this.nullable;
		}

		public void setNullable(final boolean nullable) {
			this.nullable = nullable;
		}

		public String toString() {
			return this.getTable().toString() + ":" + this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(final Object o) throws ClassCastException {
			final Column c = (Column) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(final Object o) {
			if (o == null || !(o instanceof Column))
				return false;
			final Column c = (Column) o;
			return c.toString().equals(this.toString());
		}
	}
}
