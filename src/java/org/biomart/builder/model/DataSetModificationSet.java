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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.common.model.Key;
import org.biomart.common.resources.Resources;

/**
 * This interface defines a set of modifications to a schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class DataSetModificationSet {

	// NOTE: Using Collections/Strings avoids problems with changing hashcodes.

	private Map renamedTables = new HashMap();

	private Map renamedColumns = new HashMap();
	
	private Collection maskedTables = new HashSet();
	
	private Map maskedColumns = new HashMap();
	
	private Map nonInheritedColumns = new HashMap();

	public void setMaskedColumn(final DataSetColumn column) throws ValidationException {
		final String tableKey = column.getTable().getName();
		if (!this.isMaskedColumn(column)) {
			if (column instanceof InheritedColumn) 
				throw new ValidationException(Resources.get("cannotMaskInheritedColumn"));
			for (final Iterator i = column.getTable().getKeys().iterator(); i.hasNext(); ) 
				if (((Key)i.next()).getColumns().contains(column))
					throw new ValidationException(Resources.get("cannotMaskNecessaryColumn"));
			if (!this.maskedColumns.containsKey(tableKey))
				this.maskedColumns.put(tableKey, new HashSet());
			((Collection)this.maskedColumns.get(tableKey)).add(column.getName());
		}
	}

	public void unsetMaskedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (this.maskedColumns.containsKey(tableKey)) {
			((Collection)this.maskedColumns.get(tableKey)).remove(column.getName());
			if (((Collection)this.maskedColumns.get(tableKey)).isEmpty())
				this.maskedColumns.remove(tableKey);
		}		
	}

	public boolean isMaskedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		return this.maskedColumns.containsKey(tableKey)
		 && ((Collection)this.maskedColumns.get(tableKey)).contains(column.getName());
	}

	public Map getMaskedColumns() {
		return this.maskedColumns;
	}

	public void setNonInheritedColumn(final DataSetColumn column) throws ValidationException {
		final String tableKey = column.getTable().getName();
		if (!this.isNonInheritedColumn(column)) {
			for (final Iterator i = column.getTable().getKeys().iterator(); i.hasNext(); ) 
				if (((Key)i.next()).getColumns().contains(column))
					throw new ValidationException(Resources.get("cannotNonInheritNecessaryColumn"));
			if (!this.nonInheritedColumns.containsKey(tableKey))
				this.nonInheritedColumns.put(tableKey, new HashSet());
			((Collection)this.nonInheritedColumns.get(tableKey)).add(column.getName());
		}
	}

	public void unsetNonInheritedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		if (this.nonInheritedColumns.containsKey(tableKey)) {
			((Collection)this.nonInheritedColumns.get(tableKey)).remove(column.getName());
			if (((Collection)this.nonInheritedColumns.get(tableKey)).isEmpty())
				this.nonInheritedColumns.remove(tableKey);
		}		
	}

	public boolean isNonInheritedColumn(final DataSetColumn column) {
		final String tableKey = column.getTable().getName();
		return this.nonInheritedColumns.containsKey(tableKey)
		 && ((Collection)this.nonInheritedColumns.get(tableKey)).contains(column.getName());
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

	public void replicate(final DataSetModificationSet target) {
		target.renamedTables.clear();
		target.renamedTables.putAll(this.renamedTables);
		target.renamedColumns.clear();
		target.renamedColumns.putAll(this.renamedColumns);
		target.maskedColumns.clear();
		target.maskedColumns.putAll(this.maskedColumns);
		target.maskedTables.clear();
		target.maskedTables.addAll(this.maskedTables);
	}
}
