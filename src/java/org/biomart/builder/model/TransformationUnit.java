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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.utils.InverseMap;

/**
 * This interface defines a unit of transformation for mart construction.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.1
 */
public abstract class TransformationUnit {
	/**
	 * A map of source schema column names to dataset column objects.
	 */
	private final Map newColumnNameMap;

	private final TransformationUnit previousUnit;

	public TransformationUnit(final TransformationUnit previousUnit) {
		this.newColumnNameMap = new HashMap();
		this.previousUnit = previousUnit;
	}

	public TransformationUnit getPreviousUnit() {
		return this.previousUnit;
	}

	public Map getNewColumnNameMap() {
		return this.newColumnNameMap;
	}

	public Map getReverseNewColumnNameMap() {
		return new InverseMap(this.newColumnNameMap);
	}

	public abstract DataSetColumn getDataSetColumnFor(final String columnName);

	public static class SelectFromTable extends TransformationUnit {
		private final Table table;

		private SelectFromTable(final TransformationUnit previousUnit,
				final Table table) {
			super(previousUnit);
			this.table = table;
		}

		public SelectFromTable(final Table table) {
			this(null, table);
		}

		public Table getTable() {
			return this.table;
		}

		public DataSetColumn getDataSetColumnFor(final String columnName) {
			return (DataSetColumn) this.getNewColumnNameMap().get(columnName);
		}
	}

	public static class JoinTable extends SelectFromTable {
		private List sourceDataSetColumns;

		private Key schemaSourceKey;

		private Relation schemaRelation;

		private int schemaRelationIteration;

		public JoinTable(final TransformationUnit previousUnit,
				final Table table, final List sourceDataSetColumns,
				final Key schemaSourceKey, final Relation schemaRelation,
				final int schemaRelationIteration) {
			super(previousUnit, table);
			this.sourceDataSetColumns = sourceDataSetColumns;
			this.schemaSourceKey = schemaSourceKey;
			this.schemaRelation = schemaRelation;
			this.schemaRelationIteration = schemaRelationIteration;
		}

		public List getSourceDataSetColumns() {
			return this.sourceDataSetColumns;
		}

		public Key getSchemaSourceKey() {
			return this.schemaSourceKey;
		}

		public Relation getSchemaRelation() {
			return this.schemaRelation;
		}

		public int getSchemaRelationIteration() {
			return this.schemaRelationIteration;
		}

		public DataSetColumn getDataSetColumnFor(final String columnName) {
			final DataSetColumn candidate = (DataSetColumn) this
					.getNewColumnNameMap().get(columnName);
			if (candidate == null && this.getPreviousUnit() != null) {
				final Key ourKey = this.schemaRelation.getFirstKey()
						.getColumnNames().contains(columnName) ? this.schemaRelation
						.getFirstKey()
						: this.schemaRelation.getSecondKey();
				final Key parentKey = this.schemaRelation.getOtherKey(ourKey);
				final int pos = ourKey.getColumnNames().indexOf(columnName);
				return this.getPreviousUnit().getDataSetColumnFor(
						(String) parentKey.getColumnNames().get(pos));
			} else
				return candidate;
		}
	}

	public static class Expression extends TransformationUnit {

		private DataSetTable dsTable;
		
		public Expression(final TransformationUnit previousUnit,
				final DataSetTable dsTable) {
			super(previousUnit);
			this.dsTable = dsTable;
		}

		public DataSetColumn getDataSetColumnFor(final String columnName) {
			return (DataSetColumn) this.getNewColumnNameMap().get(columnName);
		}

		public DataSetTable getDataSetTable() {
			return this.dsTable;
		}
		
		public Collection getOrderedExpressionGroups() {
			final List groups = new ArrayList();
			final Collection entries = new TreeSet(new Comparator() {
				public int compare(Object a, Object b) {
					final Map.Entry entryA = (Map.Entry) a;
					final Map.Entry entryB = (Map.Entry) b;
					final String colNameA = (String) entryA.getKey();
					final String colNameB = (String) entryB.getKey();
					final ExpressionColumnDefinition exprA = ((DataSet) Expression.this.dsTable.getSchema())
					.getDataSetModifications().getExpressionColumn(
							dsTable, colNameA);
					final ExpressionColumnDefinition exprB = ((DataSet) Expression.this.dsTable.getSchema())
					.getDataSetModifications().getExpressionColumn(
							dsTable, colNameB);
					return exprB.getAliases().keySet().contains(colNameA) ? -1 : ((exprA.isGroupBy() == exprB.isGroupBy()) ? 1 : -1);
				}
			});
			entries.addAll(this.getNewColumnNameMap().entrySet());
			// Iterator over entries and sort into groups.
			Map.Entry previousEntry = null;
			Map currentGroup = new HashMap();
			groups.add(currentGroup);
			for (final Iterator i = entries.iterator(); i.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				if (previousEntry != null) {
					final String colNameA = (String) entry.getKey();
					final String colNameB = (String) previousEntry.getKey();
					final ExpressionColumnDefinition exprA = ((DataSet) Expression.this.dsTable.getSchema())
					.getDataSetModifications().getExpressionColumn(
							dsTable, colNameA);
					final ExpressionColumnDefinition exprB = ((DataSet) Expression.this.dsTable.getSchema())
					.getDataSetModifications().getExpressionColumn(
							dsTable, colNameB);
					if (exprB.getAliases().keySet().contains(colNameA) || !(exprA.isGroupBy() == exprB.isGroupBy())) {
						currentGroup = new HashMap();
						groups.add(currentGroup);
					}
				}
				currentGroup.put(entry.getKey(), entry.getValue());
				previousEntry = entry;
			}
			return groups;
		}
	}
	// FIXME: Reinstate.
	/*
	 * public static class ConcatSchemaTable extends LeftJoinSchemaTable {
	 * public static final String CONCAT_COLNAME = "__CONCAT";
	 * 
	 * public ConcatSchemaTable(final Table schemaTable, final List
	 * sourceDataSetColumnNames, final Key schemaSourceKey, final Relation
	 * schemaRelation) { super(schemaTable, sourceDataSetColumnNames,
	 * schemaSourceKey, schemaRelation); } }
	 */
}
