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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.common.model.PartitionTable.PartitionColumn.FixedColumn;
import org.biomart.common.model.Schema.JDBCSchema;
import org.biomart.common.resources.Resources;

/**
 * The partition table interface allows lists of values to be stored, with those
 * lists broken into sub-lists if required. Each entry in the list can consist
 * of multiple columns each labelled with a unique name. The partition table
 * itself also has a unique name.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.7
 */
public interface PartitionTable {

	/**
	 * What is our name?
	 * 
	 * @return the name.
	 */
	public String getName();

	/**
	 * Change our name.
	 * 
	 * @param name
	 *            the new name.
	 */
	public void setName(final String name);

	/**
	 * Get ready to iterate over the rows in this table. After calling this, a
	 * call to {@link #nextRow()} will return the first row.
	 * 
	 * @param schemaPartition
	 *            the partition of the schema we are getting rows from, if the
	 *            table needs it (<tt>null</tt> otherwise). This value is
	 *            used when establishing a connection to the schema. See
	 *            {@link JDBCSchema#getConnection(String)}
	 * @throws PartitionException
	 *             if anything went wrong.
	 */
	public void prepareRows(final String schemaPartition)
			throws PartitionException;

	/**
	 * Move to the next row, or the first row if not yet called.
	 * 
	 * @return <tt>true</tt> if it could, or <tt>false</tt> if there are no
	 *         more.
	 * @throws PartitionException
	 *             if anything went wrong.
	 */
	public boolean nextRow() throws PartitionException;

	/**
	 * Return the current row. If {@link #nextRow()} has not been called since
	 * {@link #prepareRows(String)} was called, or you are calling this after a
	 * failed call to {@link #nextRow()} then you will get an exception.
	 * 
	 * @return the current row.
	 * @throws PartitionException
	 *             if anything went wrong, or there is no current row.
	 */
	public PartitionRow currentRow() throws PartitionException;

	/**
	 * Can the columns be added to or removed by the user?
	 * 
	 * @return <tt>true</tt> if they can.
	 */
	public boolean isMutableColumns();

	/**
	 * Add a column to the table.
	 * 
	 * @param name
	 *            the name for the column.
	 * @param column
	 *            the column definition.
	 * @throws PartitionException
	 *             if the column cannot be added, e.g. duplicate name.
	 */
	public void addColumn(final String name, final PartitionColumn column)
			throws PartitionException;

	/**
	 * Rename a column.
	 * 
	 * @param oldName
	 *            the old name.
	 * @param newName
	 *            the new name.
	 * @throws PartitionException
	 *             if the new name cannot be used. If this is thrown, the rename
	 *             will not have been performed yet.
	 */
	public void renameColumn(final String oldName, final String newName)
			throws PartitionException;

	/**
	 * Removes the named column.
	 * 
	 * @param name
	 *            the name of the column to remove.
	 * @throws PartitionException
	 *             if it cannot be removed.
	 */
	public void removeColumn(final String name) throws PartitionException;

	/**
	 * Obtain the named column definition. If the name is dotted, it will
	 * recurse and return the final definition in the chain. This is useful for
	 * navigating subdivided columns to get to the bottom of the nest, from
	 * where you can obtain a nested table definition and proceed to use
	 * {@link #currentRow()} etc.
	 * 
	 * @param name
	 *            the column name.
	 * @return the definition.
	 * @throws PartitionException
	 *             if there is no such column, or the column has been 'adopted'
	 *             by a subdivision.
	 */
	public PartitionColumn getColumn(final String name)
			throws PartitionException;

	/**
	 * Set the columns to subdivide by, and the name to give the subdivision.
	 * This will replace any existing subdivision.
	 * 
	 * @param columns
	 *            the columns.
	 * @param name
	 *            the name to give the group when referenced using aliasing.
	 * @throws PartitionException
	 *             if the name clashes with any existing columns.
	 */
	public void setSubDivision(final List columns, final String name)
			throws PartitionException;

	/**
	 * Obtain the name used for the subdivision.
	 * 
	 * @return the name.
	 */
	public String getSubdivisionName();

	/**
	 * Obtain the columns used for the subdivision.
	 * 
	 * @return the columns.
	 */
	public List getSubdivisionCols();

	/**
	 * Obtain the current sub partition table.
	 * 
	 * @return the current sub table.
	 */
	public PartitionTable getSubdivision();

	/**
	 * Makes an exact copy of ourselves with a different name.
	 * 
	 * @param newName
	 *            the name to give the copy.
	 * @return the copy.
	 * @throws PartitionException
	 *             in case of emergency.
	 */
	public PartitionTable replicate(final String newName)
			throws PartitionException;

	/**
	 * An abstract implementation from which others will extend. Anonymous inner
	 * classes extending this will probably provide SQL-based data rows, for
	 * instance.
	 */
	public static abstract class AbstractPartitionTable implements
			PartitionTable {

		/**
		 * Internal use only. Cannot be private as it is referenced by an
		 * anonymous inner class.
		 */
		protected Map columnMap = new TreeMap(); // Sorted by column name.

		private int rowIterator = -1;

		private PartitionRow currentRow = null;

		private List rows = null;

		private List subdivisionCols = new ArrayList();

		private String subdivisionName = null;

		private PartitionTable subdivision = null;

		private String name;

		/**
		 * Create a new table with a given name.
		 * 
		 * @param name
		 *            the name.
		 */
		protected AbstractPartitionTable(final String name) {
			this.name = name;
		}

		public PartitionTable replicate(final String newName)
				throws PartitionException {
			final AbstractPartitionTable targetPartitionTable;
			try {
				targetPartitionTable = (AbstractPartitionTable) this.getClass()
						.getConstructor(new Class[] { String.class })
						.newInstance(new Object[] { newName });
			} catch (final Throwable t) {
				throw new PartitionException(t);
			}
			targetPartitionTable.columnMap = new TreeMap(this.columnMap);
			targetPartitionTable.subdivisionCols = new ArrayList(
					this.subdivisionCols);
			targetPartitionTable.subdivisionName = this.subdivisionName;
			targetPartitionTable.subdivision = this.subdivision
					.replicate(targetPartitionTable.getName());
			targetPartitionTable.currentRow = null;
			targetPartitionTable.rowIterator = -1;
			targetPartitionTable.rows = null;
			return targetPartitionTable;
		}

		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public void addColumn(String name, PartitionColumn column)
				throws PartitionException {
			// Throw exception if name already used by col or subdivision.
			if (this.columnMap.containsKey(name)
					|| name.equals(this.subdivisionName))
				throw new PartitionException(Resources.get(
						"partitionColumnNameTaken", name));
			this.columnMap.put(name, column);
		}

		public PartitionRow currentRow() throws PartitionException {
			// Exception if currentRow is null.
			if (currentRow == null)
				throw new PartitionException(Resources
						.get("partitionCurrentBeforeNext"));
			return this.currentRow;
		}

		public PartitionColumn getColumn(String name) throws PartitionException {
			// Throw exception if it does not exist or is subdivided.
			if (!this.columnMap.containsKey(name)
					|| this.subdivisionCols.contains(name))
				throw new PartitionException(Resources.get(
						"partitionNoSuchColumn", name));
			return (PartitionColumn) this.columnMap.get(name);
		}

		public boolean isMutableColumns() {
			// True for top-level, false for subdivided.
			return true;
		}

		public void setSubDivision(final List columns, final String name)
				throws PartitionException {
			// Throw exception if any cols are not in this table.
			for (final Iterator i = columns.iterator(); i.hasNext();)
				this.getColumn((String) i.next()); // Infers exception.
			// Exception if name clashes.
			if (this.columnMap.containsKey(name))
				throw new PartitionException(Resources.get(
						"partitionColumnNameTaken", name));
			this.subdivisionCols = new ArrayList(columns);
			this.subdivisionName = name;
		}

		public String getSubdivisionName() {
			return this.subdivisionName;
		}

		public List getSubdivisionCols() {
			return this.subdivisionCols;
		}

		public PartitionTable getSubdivision() {
			return this.subdivision;
		}

		public boolean nextRow() throws PartitionException {
			// If row iterator is negative, throw exception.
			if (this.rowIterator < 0)
				throw new PartitionException(Resources
						.get("partitionIterateBeforePopulate"));
			// Exception if doesn't have a next, and set currentRow to
			// null.
			if (this.rowIterator >= this.rows.size())
				return false;
			// Update current row.
			this.currentRow = (PartitionRow) this.rows.get(this.rowIterator++);
			// Set up the sub-division table.
			if (this.subdivisionCols != null) {
				final List subRows = new ArrayList();
				subRows.add(this.currentRow);
				// Keep adding rows till find one not same.
				boolean keepGoing = true;
				while (keepGoing && this.rowIterator < this.rows.size()) {
					final PartitionRow subRow = (PartitionRow) this.rows
							.get(this.rowIterator);
					for (final Iterator i = this.subdivisionCols.iterator(); i
							.hasNext()
							&& keepGoing;) {
						final String subColName = (String) i.next();
						keepGoing &= this.currentRow.getValue(subColName)
								.equals(subRow.getValue(subColName));
					}
					if (keepGoing) {
						subRows.add(subRow);
						this.rowIterator++;
					}
				}
				// Build a fake sub partition table.
				final Map columnMapPointer = this.columnMap;
				this.subdivision = new AbstractPartitionTable(
						this.subdivisionName) {
					// Make columns map identically across all tables.
					{
						this.columnMap = columnMapPointer;
					}

					public List getRows(final String ignoreMe)
							throws PartitionException {
						return subRows;
					}
				};
			}
			return true;
		}

		public void prepareRows(String schemaPartition)
				throws PartitionException {
			this.currentRow = null;
			this.rows = this.getRows(schemaPartition);
			this.rowIterator = 0;
		}

		/**
		 * Implementing methods should use this to build a list of rows and
		 * return it. Iteration will be handled by the parent.
		 * 
		 * @param schemaPartition
		 *            the partition to get rows for, or <tt>null</tt> if not
		 *            to bother.
		 * @return the rows. Never <tt>null</tt> but may be empty.
		 * @throws PartitionException
		 *             if the rows couldn't be obtained.
		 */
		protected abstract List getRows(String schemaPartition)
				throws PartitionException;

		public void removeColumn(String name) throws PartitionException {
			// Throw exception if subdivide references it.
			if (this.subdivisionCols.contains(name))
				throw new PartitionException(Resources
						.get("partitionRemoveDependentCol"));
			this.columnMap.remove(name);
		}

		public void renameColumn(String oldName, String newName)
				throws PartitionException {
			// Don't bother if the two are the same.
			if (oldName == newName || oldName.equals(newName))
				return;
			// Throw exception if new name is already used in col or
			// subdivide.
			if (this.columnMap.containsKey(newName)
					|| newName.equals(this.name + '.' + this.subdivisionName))
				throw new PartitionException(Resources.get(
						"partitionColumnNameTaken", newName));
			this.columnMap.put(newName, this.columnMap.remove(oldName));
		}

		public String toString() {
			return this.getName();
		}
	}

	/**
	 * A column knows its name.
	 */
	public interface PartitionColumn {

		/**
		 * Find out which table this column belongs to.
		 * 
		 * @return the table.
		 */
		public PartitionTable getPartitionTable();

		/**
		 * Get the value in this column for the specified row.
		 * 
		 * @param row
		 *            the row.
		 * @return the value.
		 * @throws PartitionException
		 *             if there were problems getting the value, or if the row
		 *             and column are not on the same table.
		 */
		public String getValueForRow(final PartitionRow row)
				throws PartitionException;

		/**
		 * A base implementation from which all others inherit.
		 */
		public static abstract class AbstractColumn implements PartitionColumn {
			private final PartitionTable table;

			/**
			 * Construct a new column that is going to be added to this table
			 * (but don't actually add it yet).
			 * 
			 * @param table
			 *            the table.
			 */
			protected AbstractColumn(final PartitionTable table) {
				this.table = table;
			}

			public PartitionTable getPartitionTable() {
				return this.table;
			}
		}

		/**
		 * A fixed column has unchangeable values.
		 */
		public static class FixedColumn extends AbstractColumn {

			private final String columnName;

			/**
			 * Construct a new column that is going to be added to this table
			 * (but don't actually add it yet).
			 * 
			 * @param table
			 *            the table.
			 * @param columnName
			 *            a name to pass to {@link PartitionRow} instances to
			 *            get data.
			 */
			public FixedColumn(final PartitionTable table,
					final String columnName) {
				super(table);
				this.columnName = columnName;
			}

			/**
			 * Which row column do we get values from?
			 * 
			 * @return the column name.
			 */
			public String getColumnName() {
				return this.columnName;
			}

			public String getValueForRow(final PartitionRow row)
					throws PartitionException {
				// Check row+col in same table, exception if not.
				if (!row.getPartitionTable().equals(this.getPartitionTable()))
					throw new PartitionException(Resources
							.get("partitionRowColMismatch"));
				return row.getValue(columnName);
			}
		}

		/**
		 * A regex column has values based on applying a regex to a value from
		 * another column.
		 */
		public static class RegexColumn extends FixedColumn {

			private String regexMatch = null;

			private String regexReplace = null;

			private Pattern compiled = null;

			/**
			 * Construct a new column that is going to be added to this table
			 * (but don't actually add it yet).
			 * 
			 * @param table
			 *            the table.
			 * @param sourceColumnName
			 *            the column that will provide data for this regex to
			 *            work with.
			 */
			public RegexColumn(final PartitionTable table,
					final String sourceColumnName) {
				super(table, sourceColumnName);
			}

			/**
			 * Set the regex to use to match values.
			 * 
			 * @param regexMatch
			 *            the regex.
			 */
			public void setRegexMatch(final String regexMatch) {
				this.regexMatch = regexMatch;
				this.compiled = Pattern.compile(regexMatch);
			}

			/**
			 * What regex are we using to match values?
			 * 
			 * @return the regex.
			 */
			public String getRegexMatch() {
				return this.regexMatch;
			}

			/**
			 * Set the regex to use to replace values.
			 * 
			 * @param regexReplace
			 *            the regex.
			 */
			public void setRegexReplace(final String regexReplace) {
				this.regexReplace = regexReplace;
			}

			/**
			 * What regex are we using to replace values?
			 * 
			 * @return the regex.
			 */
			public String getRegexReplace() {
				return this.regexReplace;
			}

			public String getValueForRow(final PartitionRow row)
					throws PartitionException {
				return this.compiled.matcher(super.getValueForRow(row))
						.replaceAll(this.regexReplace);
			}
		}
	}

	/**
	 * This class defines how rows of the table will behave.
	 */
	public static abstract class PartitionRow implements Comparable {
		private final int rowNumber;

		private final PartitionTable table;

		/**
		 * Use this constructor to make a new numbered row. The numbers are not
		 * checked so use with care.
		 * 
		 * @param table
		 *            the table this row belongs to.
		 * @param rowNumber
		 *            the row number.
		 */
		protected PartitionRow(final PartitionTable table, final int rowNumber) {
			this.table = table;
			this.rowNumber = rowNumber;
		}

		/**
		 * Find out which table this column belongs to.
		 * 
		 * @return the table.
		 */
		public PartitionTable getPartitionTable() {
			return this.table;
		}

		/**
		 * Return the number of the current row within the table.
		 * 
		 * @return the number.
		 */
		public int getRowNumber() {
			return this.rowNumber;
		}

		/**
		 * Return the value in the given column.
		 * 
		 * @param columnName
		 *            the column.
		 * @return the value.
		 * @throws PartitionException
		 *             if there was a problem, or the column does not exist.
		 */
		public abstract String getValue(final String columnName)
				throws PartitionException;

		public int compareTo(final Object obj) {
			return this.getRowNumber() - ((PartitionRow) obj).getRowNumber();
		}
	}

	/**
	 * Applies a partition table and specifies the column used to uniquely
	 * modify generated names in the target object to make them unique for each
	 * partition.
	 */
	public static class PartitionAppliedDefinition {
		private final PartitionTable table;

		private final String columnName;

		/**
		 * Create a definition.
		 * 
		 * @param table
		 *            the table to apply.
		 * @param columnName
		 *            the column to use for unique name generation.
		 */
		public PartitionAppliedDefinition(final PartitionTable table,
				final String columnName) {
			this.table = table;
			this.columnName = columnName;
		}

		/**
		 * Obtain a reference to the applied table.
		 * 
		 * @return the applied table.
		 */
		public PartitionTable getPartitionTable() {
			return this.table;
		}

		/**
		 * Obtain a reference to the column name for generating unique names
		 * with.
		 * 
		 * @return the column name. Can then be used to call
		 *         {@link PartitionTable#getColumn(String)} on the table
		 *         returned by {@link #getPartitionTable()}.
		 */
		public String getColumnName() {
			return this.columnName;
		}
	}

	/**
	 * Use this to substitute for no-partition whenever code requires a
	 * partition and uses a null check on column name to determine whether the
	 * partition it has received is to be used or not.
	 */
	public static class FakePartitionAppliedDefinition extends
			PartitionAppliedDefinition {
		/**
		 * Create a new fake partition that isn't really partitioned at all.
		 * 
		 * @throws PartitionException
		 *             if it can't be created.
		 */
		public FakePartitionAppliedDefinition() throws PartitionException {
			super(new FakeTable(), null);
		}

		private static class FakeTable extends AbstractPartitionTable {
			private FakeTable() throws PartitionException {
				super("__FAKE_EMPTY_PARTITION_TABLE__");
				this.addColumn("hello", new FixedColumn(this, "hello"));
				this.addColumn("world", new FixedColumn(this, "world"));
			}

			public PartitionTable replicate(final String newName)
					throws PartitionException {
				final FakeTable fake = new FakeTable();
				fake.setName(newName);
				return fake;
			}

			public List getRows(String name) {
				final List fakeList = new ArrayList();
				fakeList.add(new PartitionRow(this, 0) {
					public String getValue(final String columnName)
							throws PartitionException {
						return columnName + "0";
					}
				});
				fakeList.add(new PartitionRow(this, 1) {
					public String getValue(final String columnName)
							throws PartitionException {
						return columnName + "1";
					}
				});
				return fakeList;
			}
		}
	}
}
