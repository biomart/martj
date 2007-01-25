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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;

/**
 * This interface defines a unit of transformation for mart construction.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
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
	
	public abstract DataSetColumn getDataSetColumnFor(final String columnName);

	public static class SelectFromTable extends TransformationUnit {
		private final Table table;

		private SelectFromTable(final TransformationUnit previousUnit, final Table table) {
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
			return (DataSetColumn)this.getNewColumnNameMap().get(columnName);
		}
	}

	public static class LeftJoinTable extends SelectFromTable {
		private List sourceDataSetColumns;

		private Key schemaSourceKey;

		private Relation schemaRelation;

		public LeftJoinTable(final TransformationUnit previousUnit, final Table table,
				final List sourceDataSetColumns, final Key schemaSourceKey,
				final Relation schemaRelation) {
			super(previousUnit, table);
			this.sourceDataSetColumns = sourceDataSetColumns;
			this.schemaSourceKey = schemaSourceKey;
			this.schemaRelation = schemaRelation;
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
		
		public DataSetColumn getDataSetColumnFor(final String columnName) {
			final DataSetColumn candidate = (DataSetColumn)this.getNewColumnNameMap().get(columnName);
			if (candidate==null && this.getPreviousUnit()!=null) {
				final Key ourKey = this.schemaRelation.getFirstKey().getColumnNames().contains(columnName)
				? this.schemaRelation.getFirstKey() : this.schemaRelation.getSecondKey();
				final Key parentKey = this.schemaRelation.getOtherKey(ourKey);
				final int pos = ourKey.getColumnNames().indexOf(columnName);
				return this.getPreviousUnit().getDataSetColumnFor((String)parentKey.getColumnNames().get(pos));
			}
			else
				return candidate;
		}
	}

	// FIXME: Reinstate.
	/*
	public static class ConcatSchemaTable extends LeftJoinSchemaTable {
		public static final String CONCAT_COLNAME = "__CONCAT";

		public ConcatSchemaTable(final Table schemaTable,
				final List sourceDataSetColumnNames, final Key schemaSourceKey,
				final Relation schemaRelation) {
			super(schemaTable, sourceDataSetColumnNames, schemaSourceKey,
					schemaRelation);
		}
	}
	*/
}
