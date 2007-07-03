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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.common.model.Schema.JDBCSchema;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * The partition table interface allows lists of values to be stored, with those
 * lists broken into sub-lists if required. Each entry in the list can consist
 * of multiple columns each labelled with a unique name. The partition table
 * itself also has a unique name.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.7
 */
public interface PartitionTable {

	/**
	 * Use this constant to pass to methods which require a number of rows as a
	 * parameter.
	 */
	public static final int UNLIMITED_ROWS = -1;

	/**
	 * Use this marker in the selected column list to indicate the start of a
	 * subdivision.
	 */
	public static final String DIV_COLUMN = "__SUBDIVISION_BOUNDARY__";

	/**
	 * What is our name?
	 * 
	 * @return the name.
	 */
	public String getName();

	/**
	 * Get ready to iterate over the rows in this table. After calling this, a
	 * call to {@link #nextRow()} will return the first row.
	 * 
	 * @param schemaPartition
	 *            the partition of the schema we are getting rows from, if the
	 *            table needs it (<tt>null</tt> otherwise). This value is
	 *            used when establishing a connection to the schema. See
	 *            {@link JDBCSchema#getConnection(String)}. If <tt>null</tt>
	 *            is passed when it needs a non-null value then it should use a
	 *            sensible default.
	 * @param limit
	 *            the maximum number of rows to return, or
	 *            {@link #UNLIMITED_ROWS} for no limit.
	 * @throws PartitionException
	 *             if anything went wrong.
	 */
	public void prepareRows(final String schemaPartition, final int limit)
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
	 * {@link #prepareRows(String, int)} was called, or you are calling this
	 * after a failed call to {@link #nextRow()} then you will get an exception.
	 * 
	 * @return the current row.
	 * @throws PartitionException
	 *             if anything went wrong, or there is no current row.
	 */
	public PartitionRow currentRow() throws PartitionException;

	/**
	 * Obtain the named column definition.
	 * 
	 * @param name
	 *            the column name.
	 * @return the definition.
	 * @throws PartitionException
	 *             if there is no such column.
	 */
	public PartitionColumn getSelectedColumn(final String name)
			throws PartitionException;

	/**
	 * What columns do we have?
	 * 
	 * @return the column names.
	 */
	public Collection getAllColumnNames();

	/**
	 * What columns did the user select? (This may contain entries which are
	 * equal to {@link #DIV_COLUMN} which indicate the location of subdivision
	 * boundaries.)
	 * 
	 * @return the ordered list of selected columns.
	 */
	public List getSelectedColumnNames();

	/**
	 * What columns did the user select? (This may contain entries which are
	 * equal to {@link #DIV_COLUMN} which indicate the location of subdivision
	 * boundaries.)
	 * 
	 * @param selectedColumns
	 *            the ordered list of selected columns.
	 * @throws PartitionException
	 *             if any of them are invalid.
	 */
	public void setSelectedColumnNames(final List selectedColumns)
			throws PartitionException;

	/**
	 * An abstract implementation from which others will extend. Anonymous inner
	 * classes extending this will probably provide SQL-based data rows, for
	 * instance.
	 */
	public static abstract class AbstractPartitionTable implements
			PartitionTable {

		/**
		 * Internal use only, by anonymous subclass.
		 */
		protected Map columnMap = new TreeMap(); // Sorted by column name.

		private int rowIterator = -1;

		private PartitionRow currentRow = null;

		private List rows = null;

		private final List currentSubRows = new ArrayList();

		/**
		 * Internal use only, by anonymous subclass.
		 */
		protected List selectedCols = new ArrayList();

		/**
		 * Internal use only, by anonymous subclass.
		 */
		protected List groupCols = new ArrayList();

		private AbstractPartitionTable subdivision = null;

		public PartitionRow currentRow() throws PartitionException {
			// Exception if currentRow is null.
			if (this.currentRow == null)
				throw new PartitionException(Resources
						.get("partitionCurrentBeforeNext"));
			return this.currentRow;
		}

		public PartitionColumn getSelectedColumn(final String name)
				throws PartitionException {
			// Throw exception if it does not exist or is subdivided.
			if (!this.columnMap.containsKey(name))
				throw new PartitionException(Resources.get(
						"partitionNoSuchColumn", name));
			return (PartitionColumn) this.columnMap.get(name);
		}

		public List getSelectedColumnNames() {
			return this.selectedCols;
		}

		public void setSelectedColumnNames(final List selectedColumns)
				throws PartitionException {
			// Preserve any existing regexes.
			final Map regexStore = new HashMap(this.columnMap);

			// Clear-out.
			this.selectedCols.clear();
			this.groupCols.clear();
			this.columnMap.clear();

			// Construct new table hierarchy.
			String previous = "";
			for (final Iterator i = selectedColumns.iterator(); i.hasNext();) {
				final String col = (String) i.next();
				if (col.equals(PartitionTable.DIV_COLUMN)) {
					// Don't allow back-to-back divs.
					if (previous.equals(col)) continue;
					// Don't allow div-at-end.
					else if (!i.hasNext()) continue;
					// Don't allow div-at-start.
					else if ("".equals(previous)) continue;
				}
				this.selectedCols.add(col);
				previous = col;
			}

			// Column groupings into subdivisions.
			final List currentGroupCols = new ArrayList();
			int groupPos = 0;
			while (groupPos < this.selectedCols.size()
					&& !this.selectedCols.get(groupPos).equals(
							PartitionTable.DIV_COLUMN))
				currentGroupCols.add(this.selectedCols.get(groupPos++));
			// Set up initial table.
			for (final Iterator i = currentGroupCols.iterator(); i.hasNext();) {
				final String col = (String) i.next();
				this.groupCols.add(col);
				final PartitionColumn pcol = new PartitionColumn(this, col);
				final PartitionColumn regexStored = (PartitionColumn) regexStore
						.get(pcol.getName());
				if (regexStored != null) {
					pcol.setRegexMatch(regexStored.getRegexMatch());
					pcol.setRegexReplace(regexStored.getRegexReplace());
				}
				this.columnMap.put(col, pcol);
			}
			AbstractPartitionTable currentPT = this;
			while (groupPos < this.selectedCols.size()) {
				// Skip DIV itself.
				if (groupPos < this.selectedCols.size())
					groupPos++;
				// Extend group cols to next DIV
				final List newGroupCols = new ArrayList();
				while (groupPos < this.selectedCols.size()
						&& !this.selectedCols.get(groupPos).equals(
								PartitionTable.DIV_COLUMN)) {
					final String col = (String) this.selectedCols
							.get(groupPos++);
					currentGroupCols.add(col);
					newGroupCols.add(col);
				}
				// Create subdiv PT with extended group cols
				final AbstractPartitionTable parent = this;
				final AbstractPartitionTable subdiv = new AbstractPartitionTable() {
					{
						// Subdiv column map = pointer this column map.
						this.columnMap = parent.columnMap;
						// Subdiv selected cols = pointer this selected cols.
						this.selectedCols = parent.selectedCols;
						// Subdiv group cols = copy currentGroupCols
						this.groupCols = new ArrayList(currentGroupCols);
					}

					protected List getRows(String schemaPartition, int limit)
							throws PartitionException {
						return currentSubRows;
					}

					public Collection getAllColumnNames() {
						return parent.getAllColumnNames();
					}

					public String getName() {
						return parent.getName();
					}

				};
				// Assign subdiv to current PT
				currentPT.subdivision = subdiv;
				// Create column objects for each new group col
				for (final Iterator i = newGroupCols.iterator(); i.hasNext();) {
					final String col = (String) i.next();
					// Assign column objects to new subdiv
					final PartitionColumn pcol = new PartitionColumn(subdiv,
							col);
					final PartitionColumn regexStored = (PartitionColumn) regexStore
							.get(pcol.getName());
					if (regexStored != null) {
						pcol.setRegexMatch(regexStored.getRegexMatch());
						pcol.setRegexReplace(regexStored.getRegexReplace());
					}
					this.columnMap.put(col, pcol);
				}
				// Set current PT = new subdiv
				currentPT = subdiv;
			}
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
			// Set up the sub-division tables.
			if (this.subdivision != null) {
				this.currentSubRows.clear();
				this.currentSubRows.add(this.currentRow);
				// Keep adding rows till find one not same.
				boolean keepGoing = true;
				while (keepGoing && this.rowIterator < this.rows.size()) {
					final PartitionRow subRow = (PartitionRow) this.rows
							.get(this.rowIterator);
					for (final Iterator i = this.groupCols.iterator(); i
							.hasNext()
							&& keepGoing;) {
						final String subColName = (String) i.next();
						keepGoing &= this.columnMap.containsKey(subColName)
								&& this.currentRow.getValue(subColName).equals(
										subRow.getValue(subColName));
					}
					if (keepGoing) {
						this.currentSubRows.add(subRow);
						this.rowIterator++;
					}
				}
			}
			return true;
		}

		public void prepareRows(final String schemaPartition, final int limit)
				throws PartitionException {
			Log.debug("Preparing rows");
			this.currentRow = null;
			this.rows = this.getRows(schemaPartition, limit);
			this.rowIterator = 0;
		}

		/**
		 * Implementing methods should use this to build a list of rows and
		 * return it. Iteration will be handled by the parent.
		 * 
		 * @param schemaPartition
		 *            the partition to get rows for, or <tt>null</tt> if not
		 *            to bother.
		 * @param limit
		 *            the maximum number of rows, or {@link #UNLIMITED_ROWS} for
		 *            unlimited.
		 * @return the rows. Never <tt>null</tt> but may be empty.
		 * @throws PartitionException
		 *             if the rows couldn't be obtained.
		 */
		protected abstract List getRows(final String schemaPartition,
				final int limit) throws PartitionException;

		public String toString() {
			return this.getName();
		}
	}

	/**
	 * A column knows its name.
	 */
	public static class PartitionColumn {
		private final PartitionTable table;

		private String name;

		private String regexMatch = null;

		private String regexReplace = null;

		private Pattern compiled = null;

		/**
		 * Construct a new column that is going to be added to this table (but
		 * don't actually add it yet).
		 * 
		 * @param table
		 *            the table.
		 * @param name
		 *            the name.
		 */
		public PartitionColumn(final PartitionTable table, final String name) {
			this.table = table;
			this.name = name;
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
		 * Find out the column name.
		 * 
		 * @return the column name.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Get the value in this column for the specified row.
		 * 
		 * @param row
		 *            the row.
		 * @return the value.
		 * @throws PartitionException
		 *             if there were problems getting the value.
		 */
		public String getValueForRow(final PartitionRow row)
				throws PartitionException {
			if (this.compiled != null)
				return this.compiled.matcher(row.getValue(this.getName()))
						.replaceAll(this.regexReplace);
			else
				return row.getValue(this.getName());
		}

		/**
		 * Set the regex to use to match values.
		 * 
		 * @param regexMatch
		 *            the regex.
		 */
		public void setRegexMatch(final String regexMatch) {
			this.regexMatch = regexMatch;
			if (this.regexMatch!=null)
				try {
					this.compiled = Pattern.compile(this.regexMatch);
				} catch (final PatternSyntaxException pe) {
					this.compiled = null;
				}
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

		/**
		 * A fake column for use when partitioning is not applied, but the
		 * algorithm requires a partition column object.
		 */
		public static class FakeColumn extends PartitionColumn {
			/**
			 * Construct a fake column that belongs to a fake table which only
			 * has one row, with no columns.
			 */
			public FakeColumn() {
				super(new AbstractPartitionTable() {
					public String getName() {
						return "__FAKE__TABLE__";
					}

					public Collection getAllColumnNames() {
						return Collections.EMPTY_LIST;
					}

					protected List getRows(final String schemaPartition,
							final int ignoreLimit) throws PartitionException {
						final List rows = new ArrayList();
						rows.add(new PartitionRow(this, 0) {
							public String getValue(final String columnName)
									throws PartitionException {
								// Should never get called. If it does,
								// then the empty string should suffice.
								return "";
							}
						});
						return rows;
					}
				}, null);
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
		 * Return the value in the given column. If null, returns "null".
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
}
