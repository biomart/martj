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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.biomart.builder.exceptions.PartitionException;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.TransformationUnit.JoinTable;
import org.biomart.common.model.Relation;
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
	 * Use this marker to indicate that the partitoin is applied to the whole
	 * dataset, not just a dimension in it.
	 */
	public static final String NO_DIMENSION = "";

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
	 * Move to the next row, or the first row if not yet called. This will skip
	 * over all rows with an identical set of values used to define any
	 * subdivision.
	 * 
	 * @return <tt>true</tt> if it could, or <tt>false</tt> if there are no
	 *         more.
	 * @throws PartitionException
	 *             if anything went wrong.
	 */
	public boolean nextRow() throws PartitionException;

	/**
	 * Move to the next row, or the first row if not yet called. This will not
	 * skip over multiple rows with the same subdivision-defining values.
	 * 
	 * @return <tt>true</tt> if it could, or <tt>false</tt> if there are no
	 *         more.
	 * @throws PartitionException
	 *             if anything went wrong.
	 */
	public boolean nudgeRow() throws PartitionException;

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
	 * Make a copy of ourselves in the target.
	 * 
	 * @param target
	 *            the target to copy ourselves to.
	 * @throws PartitionException
	 *             if it went wrong.
	 */
	public void replicate(final PartitionTable target)
			throws PartitionException;

	/**
	 * Apply this partition table to the given dimension using the given
	 * definition. If the definition is null, apply using defaults.
	 * 
	 * @param ds
	 *            the dataset.
	 * @param dimension
	 *            the dimension.
	 * @param appl
	 *            the application definition (null for default).
	 */
	public void applyTo(final DataSet ds, final String dimension,
			final PartitionTableApplication appl);

	/**
	 * Gets the current definition of how this partition table is applied to the
	 * dimension.
	 * 
	 * @param ds
	 *            the dataset.
	 * @param dimension
	 *            the dimension.
	 * @return the definition.
	 */
	public PartitionTableApplication getApplication(final DataSet ds,
			final String dimension);

	/**
	 * Remove this partition table from a dimension.
	 * 
	 * @param ds
	 *            the dataset.
	 * @param dimension
	 *            the dimension.
	 */
	public void removeFrom(final DataSet ds, final String dimension);

	/**
	 * Get a map of all instances where this is applied to a dimension (or if
	 * dimension is the empty string, to a dataset).
	 * 
	 * @return keys are datasets, values are nested maps where keys are
	 *         dimension names and values are application definitions.
	 */
	public Map getAllApplications();

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

		/**
		 * Internal use only, for subdivision tables only.
		 */
		protected final List subRows = new ArrayList();

		/**
		 * Internal use only, by anonymous subclass.
		 */
		protected List selectedCols = new ArrayList();

		/**
		 * Internal use only, by anonymous subclass.
		 */
		protected List groupCols = new ArrayList();

		private AbstractPartitionTable subdivision = null;

		private final Map dmApplications = new HashMap();

		public void applyTo(final DataSet ds, String dimension,
				final PartitionTableApplication appl) {
			if (dimension == null)
				dimension = PartitionTable.NO_DIMENSION;
			if (!this.dmApplications.containsKey(ds))
				this.dmApplications.put(ds, new HashMap());
			((Map) this.dmApplications.get(ds)).put(dimension,
					appl == null ? PartitionTableApplication.createDefault(
							this, ds, dimension) : appl);
		}

		public PartitionTableApplication getApplication(final DataSet ds,
				String dimension) {
			if (dimension == null)
				dimension = PartitionTable.NO_DIMENSION;
			if (!this.dmApplications.containsKey(ds))
				return null;
			return (PartitionTableApplication) ((Map) this.dmApplications
					.get(ds)).get(dimension);
		}

		public void removeFrom(final DataSet ds, String dimension) {
			if (dimension == null)
				dimension = PartitionTable.NO_DIMENSION;
			if (!this.dmApplications.containsKey(ds))
				return;
			((Map) this.dmApplications.get(ds)).remove(dimension);
			if (((Map) this.dmApplications.get(ds)).isEmpty())
				this.dmApplications.remove(ds);
		}

		public Map getAllApplications() {
			return this.dmApplications;
		}

		public PartitionRow currentRow() throws PartitionException {
			// Exception if currentRow is null.
			if (this.currentRow == null)
				throw new PartitionException(Resources
						.get("partitionCurrentBeforeNext"));
			return this.currentRow;
		}

		public void replicate(final PartitionTable target)
				throws PartitionException {
			target.setSelectedColumnNames(this.getSelectedColumnNames());
			for (final Iterator i = this.getSelectedColumnNames().iterator(); i
					.hasNext();) {
				final String colName = (String) i.next();
				final PartitionColumn ourPcol = this.getSelectedColumn(colName);
				final PartitionColumn theirPcol = target
						.getSelectedColumn(colName);
				theirPcol.setRegexMatch(ourPcol.getRegexMatch());
				theirPcol.setRegexReplace(ourPcol.getRegexReplace());
			}
			// Note that we do NOT replicate applications. That would
			// not make sense - cannot apply two partition tables to
			// a single target!
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
				if (col.equals(PartitionTable.DIV_COLUMN))
					// Don't allow back-to-back divs.
					if (previous.equals(col))
						continue;
					// Don't allow div-at-end.
					else if (!i.hasNext())
						continue;
					// Don't allow div-at-start.
					else if ("".equals(previous))
						continue;
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
						return this.subRows;
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

		public boolean nudgeRow() throws PartitionException {
			return this.getNextRow(true);
		}

		public boolean nextRow() throws PartitionException {
			return this.getNextRow(false);
		}

		private boolean getNextRow(final boolean nudge)
				throws PartitionException {
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
				this.subdivision.subRows.clear();
				this.subdivision.subRows.add(this.currentRow);
				// Keep adding rows till find one not same.
				boolean keepGoing = !nudge;
				while (keepGoing && this.rowIterator < this.rows.size()) {
					final PartitionRow subRow = (PartitionRow) this.rows
							.get(this.rowIterator);
					for (final Iterator i = this.groupCols.iterator(); i
							.hasNext()
							&& keepGoing;) {
						final String subColName = (String) i.next();
						final PartitionColumn pcol = this
								.getSelectedColumn(subColName);
						final String parentValue = pcol
								.getValueForRow(this.currentRow);
						final String subValue = pcol.getValueForRow(subRow);
						keepGoing &= parentValue.equals(subValue);
					}
					if (keepGoing) {
						this.subdivision.subRows.add(subRow);
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
			this.rows = new ArrayList(this.getRows(schemaPartition, limit));
			// Iterate over rows, apply transforms, drop duplicates.
			final Set seen = new HashSet();
			for (final Iterator i = this.rows.iterator(); i.hasNext();) {
				final PartitionRow row = (PartitionRow) i.next();
				final StringBuffer buf = new StringBuffer();
				for (final Iterator j = this.columnMap.values().iterator(); j
						.hasNext();) {
					final PartitionColumn pcol = (PartitionColumn) j.next();
					buf.append(pcol.getValueForRow(row));
					buf.append(',');
				}
				final String result = buf.toString();
				if (!seen.contains(result))
					seen.add(result);
				else
					i.remove();
			}
			this.rowIterator = 0;
		}

		/**
		 * Implementing methods should use this to build a list of rows and
		 * return it. Iteration will be handled by the parent. Duplicated rows,
		 * if any, will be handled by the parent, as will any regexing or
		 * special row manipulation.
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
			if (this.regexMatch != null)
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

	/**
	 * Defines how a partition table is applied in real life.
	 */
	public static class PartitionTableApplication {
		private final PartitionTable pt;

		private final List partitions = new ArrayList();

		/**
		 * Construct a new, empty, partition table application.
		 * 
		 * @param pt
		 *            the partition table.
		 */
		public PartitionTableApplication(final PartitionTable pt) {
			this.pt = pt;
		}

		/**
		 * What table is this applying?
		 * 
		 * @return the table.
		 */
		public PartitionTable getPartitionTable() {
			return this.pt;
		}

		/**
		 * Obtain a list of all partition rows.
		 * 
		 * @return the list.
		 */
		public List getPartitionAppliedRows() {
			return this.partitions;
		}

		/**
		 * Given a relation, find out which row of our partition table rows
		 * applies to it.
		 * 
		 * @param rel
		 *            the relation to find.
		 * @return the applied row, or null if none of them match.
		 */
		public PartitionAppliedRow getAppliedRowForRelation(final Relation rel) {
			for (final Iterator i = this.partitions.iterator(); i.hasNext();) {
				final PartitionAppliedRow row = (PartitionAppliedRow) i.next();
				if (row.getRelation() != null && row.getRelation().equals(rel))
					return row;
			}
			return null;
		}

		/**
		 * Convenience method to get the column to use to provide the name for
		 * the first entry in the applied rows.
		 * 
		 * @return the real naming column.
		 * @throws PartitionException
		 *             if it cannot.
		 */
		public PartitionColumn getNamePartitionCol() throws PartitionException {
			return this.getPartitionTable().getSelectedColumn(
					((PartitionAppliedRow) this.partitions.get(0))
							.getNamePartitionCol());
		}

		/**
		 * Set the ordered list of partition applied rows.
		 * 
		 * @param partitions
		 *            the list.
		 */
		public void setPartitionAppliedRows(final List partitions) {
			this.partitions.clear();
			this.partitions.addAll(partitions);
		}

		/**
		 * Update the compound relation counts internally.
		 * 
		 * @param dsTable
		 *            the dataset table being built when sync is called.
		 * @param rel
		 *            the relation currently being looked at when sync is
		 *            called.
		 * @throws PartitionException
		 *             if it goes wrong.
		 */
		public void syncCounts(final DataSetTable dsTable, final Relation rel)
				throws PartitionException {
			// Get real partition table for each alias and count rows.
			for (int i = 0; i < this.partitions.size(); i++) {
				final PartitionAppliedRow prow = (PartitionAppliedRow) this.partitions
						.get(i);
				final String partitionCol = prow.getPartitionCol();
				final PartitionTable realPT = this.pt.getSelectedColumn(
						partitionCol).getPartitionTable();
				int compound = 0;
				realPT.prepareRows(null, PartitionTable.UNLIMITED_ROWS);
				while (realPT.nextRow())
					compound++;
				// Work out the source relation for this dataset column.
				prow.setCompound(compound);
				// Set default relation of null.
				Relation relToUse = null;
				// Iterate over TU in dsTable.
				for (final Iterator j = dsTable.getTransformationUnits()
						.iterator(); relToUse == null && j.hasNext();) {
					final TransformationUnit tu = (TransformationUnit) j.next();
					// If TU is Join TU, iterate over cols.
					if (tu instanceof JoinTable) {
						final JoinTable jtu = (JoinTable) tu;
						for (final Iterator k = jtu.getNewColumnNameMap()
								.values().iterator(); relToUse == null
								&& k.hasNext();) {
							final DataSetColumn dsCol = (DataSetColumn) k
									.next();
							// If col matches ds col from row, update relation.
							if (dsCol.getName()
									.equals(prow.getRootDataSetCol())
									|| dsCol.getName().endsWith(
											Resources.get("columnnameSep")
													+ prow.getRootDataSetCol()))
								relToUse = jtu.getSchemaRelation();
						}
					}
				}
				// If relation is still null, and previous was not null,
				// set to default relation.
				if (relToUse == null
						&& i == 1
						|| i > 1
						&& ((PartitionAppliedRow) this.partitions.get(i - 1))
								.getRelation() != null)
					relToUse = rel;
				prow.setRelation(relToUse);
			}
		}

		/**
		 * Create a default application based on the given dataset.
		 * 
		 * @param pt
		 *            the partition table.
		 * @param ds
		 *            the dataset.
		 * @return the default application.
		 */
		public static PartitionTableApplication createDefault(
				final PartitionTable pt, final DataSet ds) {
			final PartitionTableApplication pa = new PartitionTableApplication(
					pt);
			final String ptCol = (String) pt.getSelectedColumnNames()
					.iterator().next();
			pa.setPartitionAppliedRows(Collections
					.singletonList(new PartitionAppliedRow(ptCol,
							((DataSetColumn) ds.getMainTable().getColumns()
									.iterator().next()).getName(), ptCol)));
			return pa;
		}

		/**
		 * Create a default application based on the given dimension.
		 * 
		 * @param pt
		 *            the partition table.
		 * @param ds
		 *            the dataset.
		 * @param dimension
		 *            the dimension.
		 * @return the default application.
		 */
		public static PartitionTableApplication createDefault(
				final PartitionTable pt, final DataSet ds,
				final String dimension) {
			final PartitionTableApplication pa = new PartitionTableApplication(
					pt);
			final String ptCol = (String) pt.getSelectedColumnNames()
					.iterator().next();
			pa.setPartitionAppliedRows(Collections
					.singletonList(new PartitionAppliedRow(ptCol,
							((DataSetColumn) ds.getTableByName(dimension)
									.getColumns().iterator().next()).getName(),
							ptCol)));
			return pa;
		}

		/**
		 * Details of how a partition table is broken down into a particular
		 * row.
		 */
		public static class PartitionAppliedRow {
			private int compound;

			private Relation relation;

			private final String partitionCol;

			private final String rootDataSetCol;

			private final String namePartitionCol;

			/**
			 * Construct a row of data from a single partition table.
			 * 
			 * @param partitionCol
			 *            the column providing unique values.
			 * @param rootDataSetCol
			 *            the data set column the values are applied to. This is
			 *            a root name (not including the {0}*__ prefix).
			 * @param namePartitionCol
			 *            the column providing data to be used in the prefix.
			 */
			public PartitionAppliedRow(final String partitionCol,
					final String rootDataSetCol, final String namePartitionCol) {
				this.compound = 1;
				this.relation = null;
				this.partitionCol = partitionCol;
				this.rootDataSetCol = rootDataSetCol;
				this.namePartitionCol = namePartitionCol;
			}

			/**
			 * @return the compound
			 */
			public int getCompound() {
				return this.compound;
			}

			/**
			 * @param compound
			 *            the compound to set
			 */
			public void setCompound(final int compound) {
				this.compound = compound;
			}

			/**
			 * @return the relation
			 */
			public Relation getRelation() {
				return this.relation;
			}

			/**
			 * @param relation
			 *            the relation to set
			 */
			public void setRelation(final Relation relation) {
				this.relation = relation;
			}

			/**
			 * @return the namePartitionCol
			 */
			public String getNamePartitionCol() {
				return this.namePartitionCol;
			}

			/**
			 * @return the partitionCol
			 */
			public String getPartitionCol() {
				return this.partitionCol;
			}

			/**
			 * @return the rootDataSetCol
			 */
			public String getRootDataSetCol() {
				return this.rootDataSetCol;
			}
		}
	}
}
