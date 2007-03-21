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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * This class defines a set of modifications to a dataset.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class DataSetModificationSet implements Serializable {
	private static final long serialVersionUID = 1L;

	private final DataSet ds;

	// NOTE: Using Collections/Strings avoids problems with changing hashcodes.

	private final Map renamedTables = new HashMap();

	private final Map renamedColumns = new HashMap();

	private final Collection maskedTables = new HashSet();

	private final Map maskedColumns = new HashMap();

	private final Map indexedColumns = new HashMap();

	private final Map partitionedColumns = new HashMap();

	private final Map nonInheritedColumns = new HashMap();

	private final Map expressionColumns = new HashMap();

	/**
	 * Constructs an empty set of modifications that apply to the given dataset.
	 * 
	 * @param ds
	 *            the dataset these modifications apply to.
	 */
	public DataSetModificationSet(final DataSet ds) {
		this.ds = ds;
	}

	/**
	 * Masks a dataset column.
	 * 
	 * @param column
	 *            the column.
	 * @throws ValidationException
	 *             if it cannot logically be masked.
	 */
	public void setMaskedColumn(final DataSetColumn column)
			throws ValidationException {
		final String tableKey = column.getTable().getName();
		if (!this.isMaskedColumn(column)) {
			if (column instanceof InheritedColumn)
				throw new ValidationException(Resources
						.get("cannotMaskInheritedColumn"));
			if (column.isInAnyKey())
				throw new ValidationException(Resources
						.get("cannotMaskNecessaryColumn"));
			if (!this.maskedColumns.containsKey(tableKey))
				this.maskedColumns.put(tableKey, new HashSet());
			((Collection) this.maskedColumns.get(tableKey)).add(column
					.getName());
		}
	}

	/**
	 * Unasks a dataset column.
	 * 
	 * @param column
	 *            the column.
	 */
	public void unsetMaskedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (this.maskedColumns.containsKey(tableKey)) {
			((Collection) this.maskedColumns.get(tableKey)).remove(column
					.getName());
			if (((Collection) this.maskedColumns.get(tableKey)).isEmpty())
				this.maskedColumns.remove(tableKey);
		}
	}

	/**
	 * Is the column masked?
	 * 
	 * @param column
	 *            the column.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isMaskedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		return this.maskedColumns.containsKey(tableKey)
				&& ((Collection) this.maskedColumns.get(tableKey))
						.contains(column.getName());
	}

	/**
	 * Get a map of all masked columns. Keys are table names, values are masked
	 * column names within those tables.
	 * 
	 * @return the map.
	 */
	public Map getMaskedColumns() {
		return this.maskedColumns;
	}

	/**
	 * Index the given column.
	 * 
	 * @param column
	 *            the column.
	 */
	public void setIndexedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (!this.isIndexedColumn(column)) {
			if (!this.indexedColumns.containsKey(tableKey))
				this.indexedColumns.put(tableKey, new HashSet());
			((Collection) this.indexedColumns.get(tableKey)).add(column
					.getName());
		}
	}

	/**
	 * Unindex the given column.
	 * 
	 * @param column
	 *            the column.
	 */
	public void unsetIndexedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (this.indexedColumns.containsKey(tableKey)) {
			((Collection) this.indexedColumns.get(tableKey)).remove(column
					.getName());
			if (((Collection) this.indexedColumns.get(tableKey)).isEmpty())
				this.maskedColumns.remove(tableKey);
		}
	}

	/**
	 * Is this column indexed?
	 * 
	 * @param column
	 *            the column.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isIndexedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		return this.indexedColumns.containsKey(tableKey)
				&& ((Collection) this.indexedColumns.get(tableKey))
						.contains(column.getName());
	}

	/**
	 * Get a map of all indexed columns. Keys are table names, values are
	 * indexed column names within those tables.
	 * 
	 * @return the map.
	 */
	public Map getIndexedColumns() {
		return this.indexedColumns;
	}

	/**
	 * Non-inherit a column.
	 * 
	 * @param column
	 *            the column.
	 * @throws ValidationException
	 *             if logically it cannot be done.
	 */
	public void setNonInheritedColumn(final DataSetColumn column)
			throws ValidationException {
		final String tableKey = column.getTable().getName();
		if (((DataSetTable) column.getTable()).getType().equals(
				DataSetTableType.DIMENSION))
			throw new ValidationException(Resources
					.get("cannotNonInheritDimensionColumn"));
		if (!this.isNonInheritedColumn(column)) {
			if (column.getModifiedName().endsWith(Resources.get("keySuffix")))
				throw new ValidationException(Resources
						.get("cannotNonInheritNecessaryColumn"));
			if (!this.nonInheritedColumns.containsKey(tableKey))
				this.nonInheritedColumns.put(tableKey, new HashSet());
			((Collection) this.nonInheritedColumns.get(tableKey)).add(column
					.getName());
		}
	}

	/**
	 * Un-non-inherit a column.
	 * 
	 * @param column
	 *            the column.
	 */
	public void unsetNonInheritedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (this.nonInheritedColumns.containsKey(tableKey)) {
			((Collection) this.nonInheritedColumns.get(tableKey)).remove(column
					.getName());
			if (((Collection) this.nonInheritedColumns.get(tableKey)).isEmpty())
				this.nonInheritedColumns.remove(tableKey);
		}
	}

	/**
	 * Is the column non-inherited?
	 * 
	 * @param column
	 *            the column.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isNonInheritedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		return this.nonInheritedColumns.containsKey(tableKey)
				&& ((Collection) this.nonInheritedColumns.get(tableKey))
						.contains(column.getName());
	}

	/**
	 * Get all non-inherited columns. The keys of the map are table names, and
	 * the values are non-inherited column names from those tables.
	 * 
	 * @return the map.
	 */
	public Map getNonInheritedColumns() {
		return this.nonInheritedColumns;
	}

	/**
	 * Rename a table.
	 * 
	 * @param table
	 *            the table.
	 * @param name
	 *            the new name to give it. If it is not unique it will be made
	 *            so.
	 */
	public void setTableRename(final DataSetTable table, String name) {
		this.renamedTables.remove(table.getName());
		if (!name.equals(table.getName())
				&& !name.equals(table.getModifiedName())) {
			// Make the name unique.
			final String baseName = name;
			for (int i = 1; this.renamedTables.containsValue(name); name = baseName
					+ "_" + i++)
				;
			this.renamedTables.put(table.getName(), name);
		}
	}

	/**
	 * Has this table been renamed?
	 * 
	 * @param table
	 *            the table.
	 * @return <tt>true</tt> if it has.
	 */
	public boolean isTableRename(final DataSetTable table) {
		return this.renamedTables.containsKey(table.getName());
	}

	/**
	 * Get the new name for the renamed table.
	 * 
	 * @param table
	 *            the table.
	 * @return the name.
	 */
	public String getTableRename(final DataSetTable table) {
		return (String) this.renamedTables.get(table.getName());
	}

	/**
	 * Get a map of all renamed tables. Keys are original table names and values
	 * are the new ones.
	 * 
	 * @return the map.
	 */
	public Map getTableRenames() {
		return this.renamedTables;
	}

	/**
	 * Mask the table.
	 * 
	 * @param table
	 *            the table.
	 * @throws ValidationException
	 *             if logically it cannot be masked.
	 */
	public void setMaskedTable(final DataSetTable table)
			throws ValidationException {
		if (!table.getType().equals(DataSetTableType.DIMENSION))
			throw new ValidationException(Resources
					.get("cannotMaskNonDimension"));
		this.maskedTables.add(table.getName());
	}

	/**
	 * Unmask the table.
	 * 
	 * @param table
	 *            the table.
	 */
	public void unsetMaskedTable(final DataSetTable table) {
		this.maskedTables.remove(table.getName());
	}

	/**
	 * Is the table masked?
	 * 
	 * @param table
	 *            the table.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isMaskedTable(final DataSetTable table) {
		return this.maskedTables.contains(table.getName());
	}

	/**
	 * Get a collection of all masked table names.
	 * 
	 * @return the collection.
	 */
	public Collection getMaskedTables() {
		return this.maskedTables;
	}

	/**
	 * Renames the column.
	 * 
	 * @param col
	 *            the column.
	 * @param name
	 *            the new name to give it, which will be made unique if it is
	 *            not already so.
	 * @throws ValidationException
	 *             if it cannot be renamed for logical reasons.
	 */
	public void setColumnRename(final DataSetColumn col, String name)
			throws ValidationException {
		if (col instanceof InheritedColumn)
			throw new ValidationException(Resources
					.get("cannotRenameInheritedColumn"));
		final String tableKey = col.getTable().getName();
		if (this.renamedColumns.containsKey(tableKey))
			((Map) this.renamedColumns.get(tableKey)).remove(col.getName());
		if (!name.equals(col.getName()) && !name.equals(col.getModifiedName())) {
			if (!this.renamedColumns.containsKey(tableKey))
				this.renamedColumns.put(tableKey, new HashMap());
			// First we need to find out the base name, ie. the bit
			// we append numbers to make it unique, but before any
			// key suffix. If we appended numbers after the key
			// suffix then it would confuse MartEditor.
			String keySuffix = Resources.get("keySuffix");
			String baseName = name;
			if (name.endsWith(keySuffix))
				baseName = name.substring(0, name.indexOf(keySuffix));
			else if (!col.getName().endsWith(keySuffix))
				keySuffix = "";
			// Now simply check to see if the name is used, and
			// then add an incrementing number to it until it is unique.
			name = baseName + keySuffix;
			for (int i = 1; ((Map) this.renamedColumns.get(tableKey))
					.containsValue(name)
					|| col.getTable().getColumnByName(name) != null; name = baseName
					+ "_" + i++ + keySuffix)
				;
			((Map) this.renamedColumns.get(tableKey)).put(col.getName(), name);
		}
	}

	/**
	 * Has this column been renamed?
	 * 
	 * @param col
	 *            the column.
	 * @return <tt>true</tt> if it has.
	 */
	public boolean isColumnRename(final DataSetColumn col) {
		final String tableKey = col.getTable().getName();
		return this.renamedColumns.containsKey(tableKey)
				&& ((Map) this.renamedColumns.get(tableKey)).containsKey(col
						.getName());
	}

	/**
	 * Find the new name for the column.
	 * 
	 * @param col
	 *            the column.
	 * @return the new name for it.
	 */
	public String getColumnRename(final DataSetColumn col) {
		final String tableKey = col.getTable().getName();
		return this.renamedColumns.containsKey(tableKey) ? (String) ((Map) this.renamedColumns
				.get(tableKey)).get(col.getName())
				: null;
	}

	/**
	 * Get the map of renamed columns. Keys are table names. Values are maps,
	 * where the keys are old column names and the values are the new names.
	 * 
	 * @return the map.
	 */
	public Map getColumnRenames() {
		return this.renamedColumns;
	}

	/**
	 * Mark the column as partitioned.
	 * 
	 * @param column
	 *            the column.
	 * @param restriction
	 *            the partition definition.
	 * @throws ValidationException
	 *             if it logically cannot be partitioned.
	 */
	public void setPartitionedColumn(final DataSetColumn column,
			final PartitionedColumnDefinition restriction)
			throws ValidationException {
		final String tableKey = column.getTable().getName();
		// Refuse to partition subclass tables.
		if (!((DataSetTable) column.getTable()).getType().equals(
				DataSetTableType.DIMENSION))
			throw new ValidationException(Resources
					.get("partitionOnlyDimensionTables"));
		// Check type of column.
		if (!(column instanceof WrappedColumn))
			throw new ValidationException(Resources
					.get("cannotPartitionNonWrapSchColumns"));
		// Do it. This will overwrite any existing partitioned column.
		this.partitionedColumns.put(tableKey, new HashMap());
		final Map restrictions = (Map) this.partitionedColumns.get(tableKey);
		restrictions.put(column.getName(), restriction);
	}

	/**
	 * Remove the partitioned column from the given table.
	 * 
	 * @param table
	 *            the table to unpartition.
	 */
	public void unsetPartitionedColumn(final DataSetTable table) {
		final String tableKey = table.getName();
		this.partitionedColumns.remove(tableKey);
	}

	/**
	 * Does this table have a partitioned column?
	 * 
	 * @param table
	 *            the table.
	 * @return <tt>true</tt> if it has.
	 */
	public boolean isPartitionedTable(final DataSetTable table) {
		return this.partitionedColumns.containsKey(table.getName());
	}

	/**
	 * Is this column partitioned?
	 * 
	 * @param column
	 *            the column.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isPartitionedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		final Map pcs = (Map) this.partitionedColumns.get(tableKey);
		return pcs != null && pcs.containsKey(column.getName());
	}

	/**
	 * Get the name of the partitioned column in the given table.
	 * 
	 * @param table
	 *            the table.
	 * @return the name.
	 */
	public String getPartitionedColumnName(final DataSetTable table) {
		final String tableKey = table.getName();
		final Map pcs = (Map) this.partitionedColumns.get(tableKey);
		return (String) ((Map.Entry) pcs.entrySet().iterator().next()).getKey();
	}

	/**
	 * Get the partitioned column definition for the given table.
	 * 
	 * @param dsTable
	 *            the table.
	 * @return the definition.
	 */
	public PartitionedColumnDefinition getPartitionedColumnDef(
			final DataSetTable dsTable) {
		final String tableKey = dsTable.getName();
		final Map pcs = (Map) this.partitionedColumns.get(tableKey);
		return (PartitionedColumnDefinition) pcs.get(this
				.getPartitionedColumnName(dsTable));
	}

	/**
	 * Get a map of all partitioned columns. Keys are table names. Values are
	 * maps, where the keys are column names and the values are the definitions.
	 * 
	 * @return the map.
	 */
	public Map getPartitionedColumns() {
		return this.partitionedColumns;
	}

	/**
	 * Get a unique name for the next expression column to be added.
	 * 
	 * @return the name.
	 */
	public String nextExpressionColumn() {
		final Set used = new HashSet();
		for (final Iterator j = this.expressionColumns.values().iterator(); j
				.hasNext();)
			for (final Iterator i = ((Collection) j.next()).iterator(); i
					.hasNext();)
				used.add(((ExpressionColumnDefinition) i.next()).getColKey());
		int i = 1;
		while (used.contains(Resources.get("expressionColumnPrefix") + i))
			i++;
		return Resources.get("expressionColumnPrefix") + i;
	}

	/**
	 * Add an expression column to the given table. This is in addition to
	 * adding an actual column to the actual table object.
	 * 
	 * @param table
	 *            the table.
	 * @param expr
	 *            the definition of the expression.
	 */
	public void setExpressionColumn(final DataSetTable table,
			final ExpressionColumnDefinition expr) {
		final String tableKey = table.getName();
		if (!this.expressionColumns.containsKey(tableKey))
			this.expressionColumns.put(tableKey, new HashSet());
		((Collection) this.expressionColumns.get(tableKey)).add(expr);
	}

	/**
	 * Remove the expression definition for the given table. This is in addition
	 * to removing the actual column from the actual table.
	 * 
	 * @param table
	 *            the table.
	 * @param expr
	 *            the definition.
	 */
	public void unsetExpressionColumn(final DataSetTable table,
			final ExpressionColumnDefinition expr) {
		final String tableKey = table.getName();
		if (this.expressionColumns.containsKey(tableKey)) {
			((Collection) this.expressionColumns.get(tableKey)).remove(expr);
			if (((Collection) this.expressionColumns.get(tableKey)).isEmpty())
				this.expressionColumns.remove(tableKey);
		}
	}

	/**
	 * Does this table have any expression columns?
	 * 
	 * @param table
	 *            the table.
	 * @return <tt>true</tt> if it does.
	 */
	public boolean hasExpressionColumn(final DataSetTable table) {
		final String tableKey = table.getName();
		return this.expressionColumns.containsKey(tableKey);
	}

	/**
	 * Get all the expression columns for the table. The map has keys which are
	 * table names, and values which are collections of expression column
	 * definitions.
	 * 
	 * @return the map.
	 */
	public Map getExpressionColumns() {
		return this.expressionColumns;
	}

	/**
	 * Replicates this set of modifications so that the target contains the same
	 * set.
	 * 
	 * @param target
	 *            the target to receive the replicated copies.
	 */
	public void replicate(final DataSetModificationSet target) {
		target.renamedTables.clear();
		target.renamedTables.putAll(this.renamedTables);
		target.renamedColumns.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.renamedColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.renamedColumns.put(entry.getKey(), new HashMap());
			((Map) target.renamedColumns.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
		target.maskedColumns.clear();
		target.maskedColumns.putAll(this.maskedColumns);
		target.indexedColumns.clear();
		target.indexedColumns.putAll(this.indexedColumns);
		target.maskedTables.clear();
		target.maskedTables.addAll(this.maskedTables);
		target.expressionColumns.clear();
		target.expressionColumns.putAll(this.expressionColumns);
		target.nonInheritedColumns.clear();
		target.nonInheritedColumns.putAll(this.nonInheritedColumns);
		target.partitionedColumns.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.partitionedColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.partitionedColumns.put(entry.getKey(), new HashMap());
			((Map) target.partitionedColumns.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
	}

	/**
	 * Represents a method of partitioning by column. There are no methods.
	 * Actual logic to divide up by column is left to the mart constructor to
	 * decide.
	 */
	public interface PartitionedColumnDefinition extends Serializable {
		/**
		 * Use this class to partition on a range of values - ie. only columns
		 * which fit one of these ranges will be returned.
		 */
		public static class ValueRange implements PartitionedColumnDefinition {
			private static final long serialVersionUID = 1L;
			
			private Map ranges = new HashMap();

			/**
			 * The constructor specifies the ranges to partition on. Duplicate
			 * ranges will be ignored. Keys of the range are names for the
			 * ranges. Values are range expressions where :col represents the
			 * name of the column.
			 * 
			 * @param ranges
			 *            the set of unique ranges to partition on.
			 */
			public ValueRange(final Map ranges) {
				this.ranges = new TreeMap();
				this.ranges.putAll(ranges);
			}

			public boolean equals(final Object o) {
				if (o == null || !(o instanceof ValueRange))
					return false;
				final ValueRange vc = (ValueRange) o;
				return vc.getRanges().equals(this.ranges);
			}

			/**
			 * Returns the set of ranges we will partition on. May be empty but
			 * never <tt>null</tt>.
			 * 
			 * @return the ranges we will partition on.
			 */
			public Map getRanges() {
				return this.ranges;
			}

			/**
			 * For the given range name, return the range definition after
			 * substituing column names for aliases.
			 * 
			 * @param name
			 *            the name of the range to obtain.
			 * @param alias
			 *            the table prefix to use.
			 * @param colName
			 *            the column name to use.
			 * @return the substituted expression.
			 */
			public String getSubstitutedExpression(final String name,
					final String alias, final String colName) {
				return ((String) this.ranges.get(name)).replaceAll(":col",
						alias + "." + colName);
			}

			/**
			 * {@inheritDoc}
			 * <p>
			 * This will return "ValueRange:" suffixed with the output of
			 * {@link #getRanges()}.
			 */
			public String toString() {
				return "ValueRange:"
						+ (this.ranges == null ? "<undef>" : this.ranges
								.toString());
			}
		}

		/**
		 * Use this class to partition on a list of values - ie. only columns
		 * which match one of these values will be returned.
		 */
		public static class ValueList implements PartitionedColumnDefinition {
			private static final long serialVersionUID = 1L;
			
			private Map values = new HashMap();

			/**
			 * The constructor specifies the values to partition on. Duplicate
			 * values will be ignored. Keys of the map are short names for the
			 * values
			 * 
			 * @param values
			 *            the set of unique values to partition on.
			 */
			public ValueList(final Map values) {
				this.values = new TreeMap();
				this.values.putAll(values);
			}

			public boolean equals(final Object o) {
				if (o == null || !(o instanceof ValueList))
					return false;
				final ValueList vc = (ValueList) o;
				return vc.getValues().equals(this.values);
			}

			/**
			 * Returns the set of values we will partition on. May be empty but
			 * never <tt>null</tt>.
			 * 
			 * @return the values we will partition on.
			 */
			public Map getValues() {
				return this.values;
			}

			/**
			 * {@inheritDoc}
			 * <p>
			 * This will return "ValueList:" suffixed with the output of
			 * {@link #getValues()}.
			 */
			public String toString() {
				return "ValueList:"
						+ (this.values == null ? "<undef>" : this.values
								.toString());
			}
		}
	}

	/**
	 * Defines an expression column for a table.
	 */
	public static class ExpressionColumnDefinition implements Serializable {
		private static final long serialVersionUID = 1L;

		private Map aliases;

		private String expr;

		private boolean groupBy;

		private String colKey;

		/**
		 * This constructor makes a new expression definition based on the given
		 * expression and a set of column aliases.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
		 * @param groupBy
		 *            if this expression requires a group-by statement to be
		 *            used on all columns not included in the expression.
		 * @param colKey
		 *            the name of the expression column that will be created.
		 */
		public ExpressionColumnDefinition(final String expr, final Map aliases,
				final boolean groupBy, final String colKey) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("expColMissingExpression"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("expColMissingAliases"));

			// Remember the settings.
			this.aliases = new TreeMap();
			this.aliases.putAll(aliases);
			this.expr = expr;
			this.groupBy = groupBy;
			this.colKey = colKey;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link String} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getAliases() {
			return this.aliases;
		}

		/**
		 * Returns the expression, <i>without</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @return the unsubstituted expression.
		 */
		public String getExpression() {
			return this.expr;
		}

		/**
		 * Get the name of the expression column.
		 * 
		 * @return the name.
		 */
		public String getColKey() {
			return this.colKey;
		}

		/**
		 * Does this expression require a group-by on all columns other than
		 * those included in the expression?
		 * 
		 * @return <tt>true</tt> if it does.
		 */
		public boolean isGroupBy() {
			return this.groupBy;
		}

		/**
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @param dsTable
		 *            the table to use to look up column names from.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final DataSetTable dsTable) {
			Log.debug("Calculating expression column expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final String col = (String) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, dsTable.getModifiedName(col));
			}
			Log.debug("Expression is: " + sub);
			return sub;
		}

		/**
		 * The actual expression. The values from the alias maps will be used to
		 * refer to various columns. This value is RDBMS-specific.
		 * 
		 * @param expr
		 *            the actual expression to use.
		 */
		public void setExpression(final String expr) {
			this.expr = expr;
		}
	}

	/**
	 * Remove any references to non-existent objects.
	 */
	public void synchronise() {
		for (final Iterator i = this.renamedTables.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (this.ds.getTableByName((String) entry.getKey()) == null)
				i.remove();
		}
		for (final Iterator i = this.expressionColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (this.ds.getTableByName((String) entry.getKey()) == null)
				i.remove();
		}
		for (final Iterator i = this.maskedTables.iterator(); i.hasNext();) {
			final String tbl = (String) i.next();
			if (this.ds.getTableByName(tbl) == null)
				i.remove();
		}
		for (final Iterator i = this.renamedColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (this.ds.getTableByName((String) entry.getKey()) == null)
				i.remove();
			else {
				final DataSetTable tbl = (DataSetTable) this.ds
						.getTableByName((String) entry.getKey());
				final Map cols = (Map) entry.getValue();
				for (final Iterator j = cols.entrySet().iterator(); j.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) j.next();
					if (tbl.getColumnByName((String) entry2.getKey()) == null)
						j.remove();
				}
				if (cols.isEmpty())
					i.remove();
			}
		}
		for (final Iterator i = this.maskedColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (this.ds.getTableByName((String) entry.getKey()) == null)
				i.remove();
			else {
				final DataSetTable tbl = (DataSetTable) this.ds
						.getTableByName((String) entry.getKey());
				final Collection cols = (Collection) entry.getValue();
				for (final Iterator j = cols.iterator(); j.hasNext();)
					if (tbl.getColumnByName((String) j.next()) == null)
						j.remove();
				if (cols.isEmpty())
					i.remove();
			}
		}
		for (final Iterator i = this.indexedColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (this.ds.getTableByName((String) entry.getKey()) == null)
				i.remove();
			else {
				final DataSetTable tbl = (DataSetTable) this.ds
						.getTableByName((String) entry.getKey());
				final Collection cols = (Collection) entry.getValue();
				for (final Iterator j = cols.iterator(); j.hasNext();)
					if (tbl.getColumnByName((String) j.next()) == null)
						j.remove();
				if (cols.isEmpty())
					i.remove();
			}
		}
		for (final Iterator i = this.nonInheritedColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (this.ds.getTableByName((String) entry.getKey()) == null)
				i.remove();
			else {
				final DataSetTable tbl = (DataSetTable) this.ds
						.getTableByName((String) entry.getKey());
				final Collection cols = (Collection) entry.getValue();
				for (final Iterator j = cols.iterator(); j.hasNext();)
					if (tbl.getColumnByName((String) j.next()) == null)
						j.remove();
				if (cols.isEmpty())
					i.remove();
			}
		}
		for (final Iterator i = this.partitionedColumns.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			if (this.ds.getTableByName((String) entry.getKey()) == null)
				i.remove();
			else {
				final DataSetTable tbl = (DataSetTable) this.ds
						.getTableByName((String) entry.getKey());
				final Map cols = (Map) entry.getValue();
				for (final Iterator j = cols.entrySet().iterator(); j.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) j.next();
					if (tbl.getColumnByName((String) entry2.getKey()) == null)
						j.remove();
				}
				if (cols.isEmpty())
					i.remove();
			}
		}
	}
}
