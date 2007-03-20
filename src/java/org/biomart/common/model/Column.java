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

package org.biomart.common.model;

import java.util.HashSet;
import java.util.Set;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

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
 * 			$Author$
 * @since 0.5
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
	 * If this column is involved in a key, say so.
	 * 
	 * @return <tt>true</tt> if it is in a key.
	 */
	public boolean isInAnyKey();

	/**
	 * If this column is involved in the given key, say so.
	 * 
	 * @param inKey
	 *            the key to check.
	 * @return <tt>true</tt> if it is in the key.
	 */
	public boolean isInKey(final Key inKey);

	/**
	 * Specify that this column is involved in the given key.
	 * 
	 * @param inKey
	 *            the key that this column is part of.
	 */
	public void setInKey(final Key inKey);

	/**
	 * Specify that this column is not involved in the given key.
	 * 
	 * @param inKey
	 *            the key that this column is not part of.
	 */
	public void setNotInKey(final Key inKey);

	/**
	 * A generic implementation which provides the basic functionality required
	 * for a column to function.
	 */
	public class GenericColumn implements Column {
		private String name;

		private final Table table;

		private final Set keySet = new HashSet();

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
			Log.debug("Creating column " + name + " on table " + table);
			// Remember the values.
			this.table = table;
			// First we need to find out the base name, ie. the bit
			// we append numbers to make it unique, but before any
			// key suffix. If we appended numbers after the key
			// suffix then it would confuse MartEditor.
			String suffix = "";
			String baseName = name;
			if (name.endsWith(Resources.get("keySuffix"))) {
				suffix = Resources.get("keySuffix");
				baseName = name.substring(0, name.indexOf(suffix));
			}
			// Now simply check to see if the name is used, and
			// then add an incrementing number to it until it is unique.
			for (int i = 1; table.getColumnByName(name) != null; name = baseName
					+ "_" + i++ + suffix)
				;
			// Return it.
			Log.debug("Unique name is " + name);
			this.name = name;
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

		public Table getTable() {
			return this.table;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public String toString() {
			return this.getName() + " ["+this.getTable().toString()+"]";
		}

		public boolean isInAnyKey() {
			return !this.keySet.isEmpty();
		}

		public boolean isInKey(final Key inKey) {
			return this.keySet.contains(inKey);
		}

		public void setInKey(final Key inKey) {
			this.keySet.add(inKey);
		}

		public void setNotInKey(final Key inKey) {
			this.keySet.remove(inKey);
		}
	}
}
