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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.common.model.Key;
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
public class DataSetModificationSet {
	private static final long serialVersionUID = 1L;

	private final DataSet ds;

	// NOTE: Using Collections/Strings avoids problems with changing hashcodes.

	private final Map renamedTables = new HashMap();

	private final Map renamedColumns = new HashMap();

	private final Collection distinctTables = new HashSet();

	private final Collection maskedTables = new HashSet();

	private final Map maskedColumns = new HashMap();

	private final Map indexedColumns = new HashMap();

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
			boolean inKey = false;
			for (final Iterator i = column.getTable().getKeys().iterator(); i.hasNext() && !inKey; )
				inKey = ((Key)i.next()).getColumns().contains(column);
			if (inKey)
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
			final List entries = Arrays.asList(this.renamedTables.entrySet()
					.toArray());
			// Iterate over renamedTables entries.
			// If find an entry with same name, find ds table it refers to.
			// If entry ds table parent = table parent then increment and
			// restart search.
			int suffix = 1;
			for (int i = 0; i < entries.size(); i++) {
				final Map.Entry entry = (Map.Entry) entries.get(i);
				final DataSetTable checkTable = (DataSetTable) table
						.getSchema().getTableByName((String) entry.getKey());
				final String checkName = (String) entry.getValue();
				// Can use straight == as will be null or same object.
				if (checkName.equals(name) && checkTable != null
						&& checkTable.getParent() == table.getParent()) {
					name = baseName + "_" + suffix++;
					i = -1;
				}
			}
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
	 * Distinct the table.
	 * 
	 * @param table
	 *            the table.
	 */
	public void setDistinctTable(final DataSetTable table) {
		this.distinctTables.add(table.getName());
	}

	/**
	 * Undistinct the table.
	 * 
	 * @param table
	 *            the table.
	 */
	public void unsetDistinctTable(final DataSetTable table) {
		this.distinctTables.remove(table.getName());
	}

	/**
	 * Is the table distinct?
	 * 
	 * @param table
	 *            the table.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isDistinctTable(final DataSetTable table) {
		return this.distinctTables.contains(table.getName());
	}

	/**
	 * Get a collection of all distinct table names.
	 * 
	 * @return the collection.
	 */
	public Collection getDistinctTables() {
		return this.distinctTables;
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
			name = baseName + keySuffix;
			// Now, if the old name has a partition prefix, and the
			// new one doesn't, reinstate or replace it.
			if (col.getName().indexOf("__") >= 0) {
				System.err.println("Col: " + col.getName());
				if (name.indexOf("__") >= 0)
					name = name.substring(name.lastIndexOf("__") + 2);
				name = col.getName().substring(0,
						col.getName().lastIndexOf("__") + 2)
						+ name;
			}
			// Now simply check to see if the name is used, and
			// then add an incrementing number to it until it is unique.
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
		target.distinctTables.clear();
		target.distinctTables.addAll(this.distinctTables);
		;
		target.expressionColumns.clear();
		target.expressionColumns.putAll(this.expressionColumns);
	}

	/**
	 * Defines an expression column for a table.
	 */
	public static class ExpressionColumnDefinition {
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
		 * @param prefix
		 *            the prefix to use for each column. If <tt>null</tt>, no
		 *            prefix is used.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final DataSetTable dsTable,
				final String prefix) {
			Log.debug("Calculating expression column expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final String col = (String) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				final DataSetColumn dsCol = (DataSetColumn) dsTable
						.getColumnByName(col);
				sub = sub.replaceAll(alias, prefix != null ? prefix + "."
						+ dsCol.getModifiedName() : dsCol.getModifiedName());
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
		for (final Iterator i = this.distinctTables.iterator(); i.hasNext();) {
			final String tbl = (String) i.next();
			if (this.ds.getTableByName(tbl) == null)
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
	}
}
