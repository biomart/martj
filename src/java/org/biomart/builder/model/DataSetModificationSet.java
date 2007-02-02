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

import java.util.Collection;
import java.util.Collections;
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
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.common.model.Key;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * This interface defines a set of modifications to a schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public class DataSetModificationSet {

	// NOTE: Using Collections/Strings avoids problems with changing hashcodes.

	private Map renamedTables = new HashMap();

	private Map renamedColumns = new HashMap();

	private Collection maskedTables = new HashSet();

	private Map maskedColumns = new HashMap();

	private Map partitionedColumns = new HashMap();

	private Map nonInheritedColumns = new HashMap();

	private Map expressionColumns = new HashMap();

	public void setMaskedColumn(final DataSetColumn column)
			throws ValidationException {
		final String tableKey = column.getTable().getName();
		if (!this.isMaskedColumn(column)) {
			if (column instanceof InheritedColumn)
				throw new ValidationException(Resources
						.get("cannotMaskInheritedColumn"));
			for (final Iterator i = column.getTable().getKeys().iterator(); i
					.hasNext();)
				if (((Key) i.next()).getColumns().contains(column))
					throw new ValidationException(Resources
							.get("cannotMaskNecessaryColumn"));
			if (!this.maskedColumns.containsKey(tableKey))
				this.maskedColumns.put(tableKey, new HashSet());
			((Collection) this.maskedColumns.get(tableKey)).add(column
					.getName());
		}
	}

	public void unsetMaskedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (this.maskedColumns.containsKey(tableKey)) {
			((Collection) this.maskedColumns.get(tableKey)).remove(column
					.getName());
			if (((Collection) this.maskedColumns.get(tableKey)).isEmpty())
				this.maskedColumns.remove(tableKey);
		}
	}

	public boolean isMaskedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		return this.maskedColumns.containsKey(tableKey)
				&& ((Collection) this.maskedColumns.get(tableKey))
						.contains(column.getName());
	}

	public Map getMaskedColumns() {
		return this.maskedColumns;
	}

	public void setNonInheritedColumn(final DataSetColumn column)
			throws ValidationException {
		final String tableKey = column.getTable().getName();
		if (!this.isNonInheritedColumn(column)) {
			for (final Iterator i = column.getTable().getKeys().iterator(); i
					.hasNext();)
				if (((Key) i.next()).getColumns().contains(column))
					throw new ValidationException(Resources
							.get("cannotNonInheritNecessaryColumn"));
			if (!this.nonInheritedColumns.containsKey(tableKey))
				this.nonInheritedColumns.put(tableKey, new HashSet());
			((Collection) this.nonInheritedColumns.get(tableKey)).add(column
					.getName());
		}
	}

	public void unsetNonInheritedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (this.nonInheritedColumns.containsKey(tableKey)) {
			((Collection) this.nonInheritedColumns.get(tableKey)).remove(column
					.getName());
			if (((Collection) this.nonInheritedColumns.get(tableKey)).isEmpty())
				this.nonInheritedColumns.remove(tableKey);
		}
	}

	public boolean isNonInheritedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		return this.nonInheritedColumns.containsKey(tableKey)
				&& ((Collection) this.nonInheritedColumns.get(tableKey))
						.contains(column.getName());
	}

	public Map getNonInheritedColumns() {
		return this.nonInheritedColumns;
	}

	public void setTableRename(final DataSetTable table, String name) {
		if (name.equals(table.getName()))
			this.renamedTables.remove(table.getName());
		else if (!name.equals(table.getModifiedName())) {
			// Make the name unique.
			final String baseName = name;
			for (int i = 1; this.renamedTables.containsValue(name); name = baseName
					+ "_" + i++)
				;
			this.renamedTables.put(table.getName(), name);
		}
	}

	public boolean isTableRename(final DataSetTable table) {
		return this.renamedTables.containsKey(table.getName());
	}

	public String getTableRename(final DataSetTable table) {
		return (String) this.renamedTables.get(table.getName());
	}

	public Map getTableRenames() {
		return this.renamedTables;
	}

	public void setMaskedTable(final DataSetTable table) {
		this.maskedTables.add(table.getName());
	}

	public void unsetMaskedTable(final DataSetTable table) {
		this.maskedTables.remove(table.getName());
	}

	public boolean isMaskedTable(final DataSetTable table) {
		return this.maskedTables.contains(table.getName());
	}

	public Collection getMaskedTables() {
		return this.maskedTables;
	}

	public void setColumnRename(final DataSetColumn col, String name)
			throws ValidationException {
		if (col instanceof InheritedColumn)
			throw new ValidationException(Resources
					.get("cannotRenameInheritedColumn"));
		final String tableKey = col.getTable().getName();
		if (name.equals(col.getName())) {
			if (this.renamedColumns.containsKey(tableKey))
				((Map) this.renamedColumns.get(tableKey)).remove(col.getName());
		} else if (!name.equals(col.getModifiedName())) {
			if (!this.renamedColumns.containsKey(tableKey))
				this.renamedColumns.put(tableKey, new HashMap());
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
			for (int i = 1; ((Map) this.renamedColumns.get(tableKey))
					.containsValue(name); name = baseName + "_" + i++ + suffix)
				;
			((Map) this.renamedColumns.get(tableKey)).put(col.getName(), name);
		}
	}

	public boolean isColumnRename(final DataSetColumn col) {
		final String tableKey = col.getTable().getName();
		return this.renamedColumns.containsKey(tableKey)
				&& ((Map) this.renamedColumns.get(tableKey)).containsKey(col
						.getName());
	}

	public String getColumnRename(final DataSetColumn col) {
		final String tableKey = col.getTable().getName();
		return this.renamedColumns.containsKey(tableKey) ? (String) ((Map) this.renamedColumns
				.get(tableKey)).get(col.getName())
				: null;
	}

	public Map getColumnRenames() {
		return this.renamedColumns;
	}

	public void setPartitionedColumn(final DataSetColumn column,
			final PartitionedColumnDefinition restriction)
			throws ValidationException {
		final String tableKey = column.getTable().getName();
		// Refuse to partition subclass tables.
		if (!((DataSetTable) column.getTable()).getType().equals(
				DataSetTableType.DIMENSION))
			throw new ValidationException(Resources
					.get("partitionOnlyDimensionTables"));
		// Check to see if we already have a partitioned column in this
		// table, that is not the same column as this one.
		if (this.partitionedColumns.containsKey(tableKey))
			throw new ValidationException(Resources
					.get("cannotPartitionMultiColumns"));
		// Check type of column.
		// TODO Also exclude concat and recursive columns.
		if (column instanceof ExpressionColumn)
			throw new ValidationException(Resources
					.get("cannotPartitionNonWrapSchColumns"));
		// Do it.
		this.partitionedColumns.put(tableKey, new HashMap());
		final Map restrictions = (Map) this.partitionedColumns.get(tableKey);
		restrictions.put(column.getName(), restriction);
	}

	public void unsetPartitionedColumn(final DataSetColumn column) {
		// Skip already-unmasked relations.
		if (!this.isPartitionedColumn(column))
			return;
		final String tableKey = column.getTable().getName();
		this.partitionedColumns.remove(tableKey);
	}

	public boolean isPartitionedTable(final DataSetTable table) {
		return this.partitionedColumns.containsKey(table.getName());
	}

	public boolean isPartitionedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		final Map pcs = (Map) this.partitionedColumns.get(tableKey);
		return pcs != null && pcs.containsKey(column.getName());
	}

	public String getPartitionedColumnName(final DataSetTable table) {
		if (!this.isPartitionedTable(table))
			return null;
		final String tableKey = table.getName();
		final Map pcs = (Map) this.partitionedColumns.get(tableKey);
		return (String) (((Map.Entry) pcs.entrySet().iterator().next())
				.getKey());
	}

	public PartitionedColumnDefinition getPartitionedColumn(
			final DataSetColumn column) {
		if (!this.isPartitionedColumn(column))
			return null;
		final String tableKey = column.getTable().getName();
		final Map pcs = (Map) this.partitionedColumns.get(tableKey);
		return (PartitionedColumnDefinition) pcs.get(column.getName());
	}

	public Map getPartitionedColumns() {
		return this.partitionedColumns;
	}

	public String nextExpressionColumn(final DataSetTable table) {
		final String tableKey = table.getName();
		int i = 1;
		if (this.expressionColumns.containsKey(tableKey))
			while (((Map) this.expressionColumns.get(tableKey))
					.containsKey(Resources.get("expressionColumnPrefix") + i)) {
				i++;
			}
		return Resources.get("expressionColumnPrefix") + i;
	}

	public void setExpressionColumn(final DataSetTable table,
			final ExpressionColumnDefinition expr) {
		final String tableKey = table.getName();
		if (!this.expressionColumns.containsKey(tableKey))
			this.expressionColumns.put(tableKey, new HashSet());
		((Collection) this.expressionColumns.get(tableKey)).add(expr);
	}

	public void unsetExpressionColumn(final DataSetTable table,
			final ExpressionColumnDefinition expr) {
		final String tableKey = table.getName();
		if (this.expressionColumns.containsKey(tableKey)) {
			((Collection) this.expressionColumns.get(tableKey)).remove(expr);
			if (((Collection) this.expressionColumns.get(tableKey)).isEmpty())
				this.expressionColumns.remove(tableKey);
		}
	}

	public boolean hasExpressionColumn(final DataSetTable table) {
		final String tableKey = table.getName();
		return this.expressionColumns.containsKey(tableKey);
	}

	public Map getExpressionColumns() {
		return this.expressionColumns;
	}

	public void replicate(final DataSetModificationSet target) {
		target.renamedTables.clear();
		target.renamedTables.putAll(this.renamedTables);
		target.renamedColumns.clear();
		target.renamedColumns.putAll(this.renamedColumns);
		target.maskedColumns.clear();
		target.maskedColumns.putAll(this.maskedColumns);
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
	public interface PartitionedColumnDefinition {
		/**
		 * Use this class to partition on a single value - ie. only rows
		 * matching this value will be returned.
		 */
		public static class SingleValue extends ValueCollection {
			private String value;

			/**
			 * The constructor specifies the value to partition on.
			 * 
			 * @param value
			 *            the value to partition on.
			 * @param useNull
			 *            if set to <tt>true</tt>, the value will be ignored,
			 *            and null will be used instead.
			 */
			public SingleValue(final String value, final boolean useNull) {
				super(Collections.singleton(value), useNull);
				this.value = value;
			}

			/**
			 * Returns the value we will partition on.
			 * 
			 * @return the value we will partition on.
			 */
			public String getValue() {
				return this.value;
			}

			/**
			 * {@inheritDoc}
			 * <p>
			 * This will return "SingleValue:" suffixed with the output of
			 * {@link #getValue()}.
			 */
			public String toString() {
				return "SingleValue:" + this.value;
			}
		}

		/**
		 * Use this class to refer to a column partitioned by every unique
		 * value.
		 */
		public static class UniqueValues implements PartitionedColumnDefinition {

			/**
			 * {@inheritDoc}
			 * <p>
			 * This will return "UniqueValues".
			 */
			public String toString() {
				return "UniqueValues";
			}
		}

		/**
		 * Use this class to partition on a set of values - ie. only columns
		 * with one of these values will be returned.
		 */
		public static class ValueCollection implements
				PartitionedColumnDefinition {
			private boolean includeNull;

			private Set values = new HashSet();

			/**
			 * The constructor specifies the values to partition on. Duplicate
			 * values will be ignored.
			 * 
			 * @param values
			 *            the set of unique values to partition on.
			 * @param includeNull
			 *            whether to include <tt>null</tt> as a partitionable
			 *            value.
			 */
			public ValueCollection(final Collection values,
					final boolean includeNull) {
				this.values = new HashSet();
				this.values.addAll(values);
				this.includeNull = includeNull;
			}

			public boolean equals(final Object o) {
				if (o == null || !(o instanceof ValueCollection))
					return false;
				final ValueCollection vc = (ValueCollection) o;
				return vc.getValues().equals(this.values)
						&& vc.getIncludeNull() == this.includeNull;
			}

			/**
			 * Returns <tt>true</tt> or <tt>false</tt> depending on whether
			 * <tt>null</tt> is considered a partitionable value or not.
			 * 
			 * @return <tt>true</tt> if <tt>null</tt> is included as a
			 *         partitioned value.
			 */
			public boolean getIncludeNull() {
				return this.includeNull;
			}

			/**
			 * Returns the set of values we will partition on. May be empty but
			 * never null.
			 * 
			 * @return the values we will partition on.
			 */
			public Set getValues() {
				return this.values;
			}

			/**
			 * {@inheritDoc}
			 * <p>
			 * This will return "ValueCollection:" suffixed with the output of
			 * {@link #getValues()}.
			 */
			public String toString() {
				return "ValueCollection:"
						+ (this.values == null ? "<undef>" : this.values
								.toString());
			}
		}
	}

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class ExpressionColumnDefinition {

		private Map aliases;

		private String expr;

		private boolean groupBy;

		private String colKey;

		/**
		 * This constructor gives the restriction an initial expression and a
		 * set of aliases. The expression may not be empty, and neither can the
		 * alias map.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
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

		public String getColKey() {
			return this.colKey;
		}

		public boolean isGroupBy() {
			return this.groupBy;
		}

		/**
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @param tablePrefix
		 *            the prefix to use for the table in the expression.
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
}
