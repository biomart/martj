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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Schema.GenericSchema;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.Resources;

/**
 * <p>
 * Represents a window onto a schema, and the transformation of that window into
 * a dataset.
 * <p>
 * The window allows masking of unwanted relations and columns, and flagging of
 * relations as concat-only or subclassed.
 * <p>
 * The central table of the dataset is the table which will be used to derive
 * the main table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.40, 28th July 2006
 * @since 0.1
 */
public class DataSet extends GenericSchema {
	private final Mart mart;

	private final Table centralTable;

	// Use List to avoid problems with hashcodes changing.
	private final List maskedRelations = new ArrayList();

	// Use List to avoid problems with hashcodes changing.
	private final List maskedDataSetColumns = new ArrayList();

	// Use double-List to avoid problems with hashcodes changing.
	private final List[] partitionedDataSetColumns = new List[] {
			new ArrayList(), new ArrayList() };

	// Use List to avoid problems with hashcodes changing.
	private final List subclassedRelations = new ArrayList();

	// Use double-List to avoid problems with hashcodes changing.
	private final List[] concatOnlyRelations = new List[] { new ArrayList(),
			new ArrayList() };

	// Use double-List to avoid problems with hashcodes changing.
	private final List[] restrictedRelations = new List[] { new ArrayList(),
			new ArrayList() };

	// Use double-List to avoid problems with hashcodes changing.
	private final List[] restrictedTables = new List[] { new ArrayList(),
			new ArrayList() };

	private DataSetOptimiserType optimiser;

	private boolean invisible;

	/**
	 * The constructor creates a dataset around one central table and gives the
	 * dataset a name. It adds itself to the specified mart automatically.
	 * <p>
	 * If the name already exists, an underscore and a sequence number will be
	 * appended until the name is unique.
	 * 
	 * @param mart
	 *            the mart this dataset will belong to.
	 * @param centralTable
	 *            the table to use as the central table for this dataset.
	 * @param name
	 *            the name to give this dataset.
	 * @throws AssociationException
	 *             if the central table does not belong to any of the schema
	 *             objects in the mart.
	 * @throws AlreadyExistsException
	 *             Should never happen. If it does, it was thrown by
	 *             {@link Mart#addDataSet(DataSet));
	 */
	public DataSet(Mart mart, Table centralTable, String name)
			throws AssociationException, AlreadyExistsException {
		// Super first, to set the name.
		super(name);

		// Sanity check to see if the table is in this mart or not.
		if (!mart.getSchemas().contains(centralTable.getSchema()))
			throw new AssociationException(Resources.get("tableMartMismatch"));

		// Remember the settings and make some defaults.
		this.invisible = false;
		this.mart = mart;
		this.centralTable = centralTable;
		this.optimiser = DataSetOptimiserType.NONE;

		// Rename ourselves if the name clashes.
		String newName = name;
		int suffix = 1;
		while (mart.getDataSetByName(newName) != null)
			newName = name + "_" + (suffix++);
		if (!newName.equals(name))
			this.setName(newName);

		// Add ourselves to the mart.
		mart.addDataSet(this);
	}

	public Schema replicate(String newName) {
		try {
			// Create the copy.
			DataSet newDataSet = new DataSet(this.mart, this.centralTable,
					newName);

			// Copy everything. It doesn't matter that we're using the
			// objects for columns from the original dataset, as during
			// synchronisation these will be replaced with objects
			// representing columns in the new one automatically.
			newDataSet.setDataSetOptimiserType(this.optimiser);
			for (Iterator i = this.maskedRelations.iterator(); i.hasNext();)
				newDataSet.maskRelation((Relation) i.next());
			for (Iterator i = this.subclassedRelations.iterator(); i.hasNext();)
				newDataSet.flagSubclassRelation((Relation) i.next());
			for (Iterator i = this.maskedDataSetColumns.iterator(); i.hasNext();)
				newDataSet.maskDataSetColumn((DataSetColumn) i.next());
			for (int i = 0; i < this.concatOnlyRelations[0].size(); i++) {
				Relation rel = (Relation) this.concatOnlyRelations[0].get(i);
				ConcatRelationType type = (ConcatRelationType) this.concatOnlyRelations[1]
						.get(i);
				newDataSet.flagConcatOnlyRelation(rel, type);
			}
			for (int i = 0; i < this.partitionedDataSetColumns[0].size(); i++) {
				WrappedColumn col = (WrappedColumn) this.partitionedDataSetColumns[0]
						.get(i);
				PartitionedColumnType type = (PartitionedColumnType) this.partitionedDataSetColumns[1]
						.get(i);
				newDataSet.flagPartitionedDataSetColumn(col, type);
			}
			for (int i = 0; i < this.restrictedRelations[0].size(); i++) {
				Relation rel = (Relation) this.restrictedRelations[0].get(i);
				DataSetRelationRestriction restriction = (DataSetRelationRestriction) this.restrictedRelations[1]
						.get(i);
				newDataSet.flagRestrictedRelation(rel, restriction);
			}
			for (int i = 0; i < this.restrictedTables[0].size(); i++) {
				Table tbl = (Table) this.restrictedTables[0].get(i);
				DataSetTableRestriction restriction = (DataSetTableRestriction) this.restrictedTables[1]
						.get(i);
				newDataSet.flagRestrictedTable(tbl, restriction);
			}

			// Synchronise it.
			newDataSet.synchronise();

			// Return the copy.
			return newDataSet;
		} catch (Throwable t) {
			throw new MartBuilderInternalError(t);
		}
	}

	/**
	 * Returns the mart of this dataset.
	 * 
	 * @return the mart containing this dataset.
	 */
	public Mart getMart() {
		return this.mart;
	}

	/**
	 * Returns the central table of this dataset.
	 * 
	 * @return the central table of this dataset.
	 */
	public Table getCentralTable() {
		return this.centralTable;
	}

	/**
	 * Test to see if this dataset is invisible.
	 * 
	 * @return <tt>true</tt> if it is invisible, <tt>false</tt> otherwise.
	 */
	public boolean getInvisible() {
		return this.invisible;
	}

	/**
	 * Sets the inivisibility of this dataset.
	 * 
	 * @param invisible
	 *            <tt>true</tt> if it is invisible, <tt>false</tt>
	 *            otherwise.
	 */
	public void setInvisible(boolean invisible) {
		this.invisible = invisible;
	}

	/**
	 * Mask a relation.
	 * 
	 * @param relation
	 *            the relation to mask.
	 */
	public void maskRelation(Relation relation) {
		// Skip if already masked.
		if (!this.maskedRelations.contains(relation))
			this.maskedRelations.add(relation);
	}

	/**
	 * Unmask a relation.
	 * 
	 * @param relation
	 *            the relation to unmask.
	 */
	public void unmaskRelation(Relation relation) {
		this.maskedRelations.remove(relation);
	}

	/**
	 * Return the set of masked relations. It may be empty, but never null.
	 * 
	 * @return the set of masked relations.
	 */
	public Collection getMaskedRelations() {
		return this.maskedRelations;
	}

	/**
	 * <p>
	 * Mask a column. If the column is an essential part of an underlying key,
	 * then all relations using that key are also masked.
	 * <p>
	 * Columns that are part of the primary key on the table they are from, or
	 * are part of any foreign key on that table, cannot be masked.
	 * 
	 * @param column
	 *            the {@link Column} to mask.
	 */
	public void maskDataSetColumn(DataSetColumn dsColumn)
			throws AssociationException {
		// Work out where this column is coming from.
		DataSetTable dsTable = (DataSetTable) dsColumn.getTable();
		Table centralTable = ((DataSet) dsTable.getSchema()).getCentralTable();

		// Make a list of columns that are OK to mask.
		List okToMask = new ArrayList(dsTable.getColumns());

		// Can mask PK cols only if not from original central table
		Key dsTablePK = dsTable.getPrimaryKey();
		if (dsTablePK != null) {
			for (Iterator i = dsTablePK.getColumns().iterator(); i.hasNext();) {
				DataSetColumn col = (DataSetColumn) i.next();
				Table underlyingTable = ((DataSetTable) col.getTable())
						.getUnderlyingTable();
				if (underlyingTable != null
						&& underlyingTable.equals(centralTable))
					okToMask.remove(col);
			}
		}

		// Can't mask any FK cols.
		for (Iterator i = dsTable.getForeignKeys().iterator(); i.hasNext();)
			okToMask.removeAll(((Key) i.next()).getColumns());

		// Refuse to mask the column if it does not appear in the list of OK
		// columns to mask.
		if (!okToMask.contains(dsColumn))
			throw new AssociationException(Resources
					.get("cannotMaskNecessaryColumn"));

		// Refuse to mask schema name columns.
		else if (dsColumn instanceof SchemaNameColumn)
			throw new AssociationException(Resources
					.get("cannotMaskSchemaNameColumn"));

		// Refuse to mask inherited columns.
		else if (dsColumn instanceof InheritedColumn)
			throw new AssociationException(Resources
					.get("cannotMaskInheritedColumn"));

		// Refuse to mask inherited columns.
		else if (dsColumn instanceof ExpressionColumn)
			((DataSetTable) dsColumn.getTable()).removeColumn(dsColumn);

		// If concat-only, mask concat-only relation instead
		else if (dsColumn instanceof ConcatRelationColumn)
			this.maskRelation(dsColumn.getUnderlyingRelation());

		// If wrapped, mask wrapped column
		else if (dsColumn instanceof WrappedColumn) {
			WrappedColumn wcol = (WrappedColumn) dsColumn;

			// Skip if already masked.
			if (this.maskedDataSetColumns.contains(wcol))
				return;

			// Mask it.
			this.maskedDataSetColumns.add(wcol);

			// Mask the associated relations from the real underlying column.
			for (Iterator i = wcol.getWrappedColumn().getTable().getKeys()
					.iterator(); i.hasNext();) {
				Key k = (Key) i.next();
				if (k.getColumns().contains(wcol.getWrappedColumn()))
					for (Iterator j = k.getRelations().iterator(); j.hasNext();) {
						Relation r = (Relation) j.next();
						this.maskRelation(r);
					}
			}
		}

		// Eh?
		else
			throw new MartBuilderInternalError();
	}

	/**
	 * Unmask a column.
	 * 
	 * @param column
	 *            the column to unmask.
	 */
	public void unmaskDataSetColumn(DataSetColumn column) {
		this.maskedDataSetColumns.remove(column);
	}

	/**
	 * Return the set of masked columns. It may be empty, but never null.
	 * 
	 * @return the set of masked columns.
	 */
	public Collection getMaskedDataSetColumns() {
		return this.maskedDataSetColumns;
	}

	/**
	 * Mark a table as a subclass of another by marking the relation between
	 * them. The M end of the relation is the child table, and the 1 end is the
	 * parent table, but the parent table may not actually be the central table
	 * in this dataset - instead it may be linked to that central table by an
	 * existing chain of M:1 relations. The central table of the dataset may
	 * have at most one M:1 relation, and at most one 1:M relations.
	 * 
	 * @param relation
	 *            the relation to mark as a subclass relation, where the PK end
	 *            is the parent table and the FK end is the subclass table.
	 * @throws AssociationException
	 *             if one end of the relation is not the central table for this
	 *             window, or is not another subclass table, or if both ends of
	 *             the relation point to the same table, or if the rule about
	 *             maximum numbers of subclass relations of certain types
	 *             applies (see above).
	 */
	public void flagSubclassRelation(Relation relation)
			throws AssociationException {
		// Skip if already subclassed.
		if (this.subclassedRelations.contains(relation))
			return;

		// Check that the relation is a 1:M relation and isn't a loop-back.
		if (!relation.isOneToMany())
			throw new AssociationException(Resources.get("subclassNotOneMany"));
		if (relation.getFirstKey().getTable().equals(
				relation.getSecondKey().getTable()))
			throw new AssociationException(Resources
					.get("subclassNotBetweenTwoTables"));

		// Work out the child end of the relation - the M end. The parent is
		// the 1 end.
		Table parentTable = relation.getOneKey().getTable();
		Table childTable = relation.getManyKey().getTable();

		// If there are no existing subclass relations, then we only
		// need to test that either the parent or the child is the central
		// table.
		if (this.subclassedRelations.isEmpty()) {
			if (!(parentTable.equals(this.centralTable) || childTable
					.equals(this.centralTable)))
				throw new AssociationException(Resources
						.get("subclassNotOnCentralTable"));
		}

		// We have existing subclass relations.
		else {
			boolean parentIsCentral = parentTable.equals(this.centralTable);
			boolean childIsCentral = parentTable.equals(this.centralTable);
			boolean parentHasM1 = false;
			boolean childHasM1 = false;
			boolean parentHas1M = false;
			boolean childHas1M = false;
			// Check that child has no M:1s and parent has no 1:Ms already.
			// Check that parent has M:1, and child has 1:M.
			for (Iterator i = this.subclassedRelations.iterator(); i.hasNext();) {
				Relation rel = (Relation) i.next();
				if (rel.getOneKey().getTable().equals(parentTable))
					parentHas1M = true;
				else if (rel.getOneKey().getTable().equals(childTable))
					childHas1M = true;
				if (rel.getManyKey().getTable().equals(parentTable))
					parentHasM1 = true;
				else if (rel.getManyKey().getTable().equals(childTable))
					childHasM1 = true;
			}

			// If child has M:1 or parent has 1:M, we cannot do this.
			if (childHasM1 || parentHas1M)
				throw new AssociationException(Resources
						.get("mixedCardinalitySubclasses"));

			// If parent is not central or doesn't have M:1, and
			// child is not central or doesn't have 1:M, we cannot do this.
			if (!(parentIsCentral || childIsCentral)
					&& !(parentHasM1 || childHas1M))
				throw new AssociationException(Resources
						.get("subclassNotOnCentralTable"));
		}

		// Mark the relation.
		this.subclassedRelations.add(relation);
	}

	/**
	 * Unmark a relation as a subclass relation. If this breaks a chain, then
	 * all subsequent subclass relations having a 1:M from the M end of this
	 * relation are also unmarked.
	 * 
	 * @param relation
	 *            the relation to unmark.
	 */
	public void unflagSubclassRelation(Relation relation) {
		// Break the chain first.
		Table target = relation.getManyKey().getTable();
		if (!target.equals(this.centralTable)) {
			if (target.getPrimaryKey() != null)
				for (Iterator i = target.getPrimaryKey().getRelations()
						.iterator(); i.hasNext();) {
					Relation rel = (Relation) i.next();
					if (rel.isOneToMany())
						this.unflagSubclassRelation(rel);
				}
		}
		// Then remove the head of the chain.
		this.subclassedRelations.remove(relation);
	}

	/**
	 * Return the set of subclassed relations. It may be empty, but never null.
	 * 
	 * @return the set of subclassed relations.
	 */
	public Collection getSubclassedRelations() {
		return this.subclassedRelations;
	}

	/**
	 * Mark a column as partitioned. If previously marked as partitioned, this
	 * call will override the previous request.
	 * 
	 * @param column
	 *            the columnto mark as partitioned.
	 * @param type
	 *            the partition type to use for the partition.
	 * @throws AssociationException
	 *             if there is already a partitioned column on this table.
	 */
	public void flagPartitionedDataSetColumn(DataSetColumn column,
			PartitionedColumnType type) throws AssociationException {
		// If we do already have a partitioned column on this table, throw a
		// wobbly.
		if (!(column instanceof WrappedColumn || column instanceof SchemaNameColumn))
			throw new AssociationException(Resources
					.get("cannotPartitionNonWrapSchColumns"));

		// Check to see if we already have a partitioned column in this table,
		// that is not the same column as this one.
		boolean alreadyHave = false;
		for (Iterator i = this.partitionedDataSetColumns[0].iterator(); i
				.hasNext()
				&& !alreadyHave;) {
			DataSetColumn testCol = (DataSetColumn) i.next();
			if (testCol.getTable().equals(column.getTable())
					&& !testCol.equals(column))
				alreadyHave = true;
		}

		// If we do already have a partitioned column on this table, throw a
		// wobbly.
		if (alreadyHave)
			throw new AssociationException(Resources
					.get("cannotPartitionMultiColumns"));

		// Partition the colum. If the column has already been partitioned, this
		// will override the type.
		int index = this.partitionedDataSetColumns[0].indexOf(column);
		if (index >= 0) {
			this.partitionedDataSetColumns[0].set(index, column);
			this.partitionedDataSetColumns[1].set(index, type);
		} else {
			this.partitionedDataSetColumns[0].add(column);
			this.partitionedDataSetColumns[1].add(type);
		}
	}

	/**
	 * Unmark a column as partitioned.
	 * 
	 * @param column
	 *            the column to unmark.
	 */
	public void unflagPartitionedDataSetColumn(DataSetColumn column) {
		int index = this.partitionedDataSetColumns[0].indexOf(column);
		if (index >= 0) {
			this.partitionedDataSetColumns[0].remove(index);
			this.partitionedDataSetColumns[1].remove(index);
		}
	}

	/**
	 * Return the set of partitioned columns. It may be empty, but never null.
	 * 
	 * @return the set of partitioned columns.
	 */
	public Collection getPartitionedDataSetColumns() {
		return this.partitionedDataSetColumns[0];
	}

	/**
	 * Return the partition type of a partitioned column. It will return null if
	 * there is no such partitioned column.
	 * 
	 * @param column
	 *            the column to check the partitioning type for.
	 * @return the partition type of the column, or null if it is not
	 *         partitioned.
	 */
	public PartitionedColumnType getPartitionedDataSetColumnType(
			DataSetColumn column) {
		int index = this.partitionedDataSetColumns[0].indexOf(column);
		if (index >= 0)
			return (PartitionedColumnType) this.partitionedDataSetColumns[1]
					.get(index);
		else
			return null;
	}

	/**
	 * Mark a relation as restricted. If previously marked as restricted, this
	 * call will override the previous request.
	 * 
	 * @param relation
	 *            the relation to mark as restricted.
	 * @param restriction
	 *            the restriction to use for the relation.
	 */
	public void flagRestrictedRelation(Relation relation,
			DataSetRelationRestriction restriction) {
		// Restrict the relation. If the relation has already been restricted,
		// this will override the type.
		int index = this.restrictedRelations[0].indexOf(relation);
		if (index >= 0) {
			this.restrictedRelations[0].set(index, relation);
			this.restrictedRelations[1].set(index, restriction);
		} else {
			this.restrictedRelations[0].add(relation);
			this.restrictedRelations[1].add(restriction);
		}
	}

	/**
	 * Unmark a relation as restricted.
	 * 
	 * @param relation
	 *            the relation to unmark.
	 */
	public void unflagRestrictedRelation(Relation relation) {
		int index = this.restrictedRelations[0].indexOf(relation);
		if (index >= 0) {
			this.restrictedRelations[0].remove(index);
			this.restrictedRelations[1].remove(index);
		}
	}

	/**
	 * Return the set of restricted relations. It may be empty, but never null.
	 * 
	 * @return the set of restricted relations.
	 */
	public Collection getRestrictedRelations() {
		return this.restrictedRelations[0];
	}

	/**
	 * Return the restriction for a restricted relation. It will return null if
	 * there is no such restricted relation.
	 * 
	 * @param relation
	 *            the relation to check the restriction type for.
	 * @return the restriction type of the relation, or null if it is not
	 *         restricted.
	 */
	public DataSetRelationRestriction getRestrictedRelationType(
			Relation relation) {
		int index = this.restrictedRelations[0].indexOf(relation);
		if (index >= 0)
			return (DataSetRelationRestriction) this.restrictedRelations[1]
					.get(index);
		else
			return null;
	}

	/**
	 * Mark a table as restricted. If previously marked as restricted, this call
	 * will override the previous request.
	 * 
	 * @param table
	 *            the table to mark as restricted.
	 * @param restriction
	 *            the restriction to use for the table.
	 */
	public void flagRestrictedTable(Table table,
			DataSetTableRestriction restriction) {
		// Restrict the table. If the table has already been restricted,
		// this will override the type.
		int index = this.restrictedTables[0].indexOf(table);
		if (index >= 0) {
			this.restrictedTables[0].set(index, table);
			this.restrictedTables[1].set(index, restriction);
		} else {
			this.restrictedTables[0].add(table);
			this.restrictedTables[1].add(restriction);
		}
	}

	/**
	 * Unmark a table as restricted.
	 * 
	 * @param table
	 *            the table to unmark.
	 */
	public void unflagRestrictedTable(Table table) {
		int index = this.restrictedTables[0].indexOf(table);
		if (index >= 0) {
			this.restrictedTables[0].remove(index);
			this.restrictedTables[1].remove(index);
		}
	}

	/**
	 * Return the set of restricted tables. It may be empty, but never null.
	 * 
	 * @return the set of restricted tables.
	 */
	public Collection getRestrictedTables() {
		return this.restrictedTables[0];
	}

	/**
	 * Return the restriction for a restricted table. It will return null if
	 * there is no such restricted table.
	 * 
	 * @param table
	 *            the table to check the restriction type for.
	 * @return the restriction type of the table, or null if it is not
	 *         restricted.
	 */
	public DataSetTableRestriction getRestrictedTableType(Table table) {
		int index = this.restrictedTables[0].indexOf(table);
		if (index >= 0)
			return (DataSetTableRestriction) this.restrictedTables[1]
					.get(index);
		else
			return null;
	}

	/**
	 * Mark a relation as concat-only. If previously marked concat-only, this
	 * new call will override the previous request. If the relation is not 1:M,
	 * or the M end does not have a primary key, the call will fail.
	 * 
	 * @param relation
	 *            the relation to mark as concat-only.
	 * @param type
	 *            the concat type to use for the relation.
	 */
	public void flagConcatOnlyRelation(Relation relation,
			ConcatRelationType type) throws AssociationException {

		// Sanity check.
		if (!relation.isOneToMany())
			throw new AssociationException(Resources
					.get("cannotConcatNonOneMany"));
		if (relation.getManyKey().getTable().getPrimaryKey() == null)
			throw new AssociationException(Resources
					.get("cannotConcatManyWithoutPK"));

		// Do it.
		int index = this.concatOnlyRelations[0].indexOf(relation);
		if (index >= 0) {
			this.concatOnlyRelations[0].set(index, relation);
			this.concatOnlyRelations[1].set(index, type);
		} else {
			this.concatOnlyRelations[0].add(relation);
			this.concatOnlyRelations[1].add(type);
		}
	}

	/**
	 * Unmark a relation as concat-only.
	 * 
	 * @param relation
	 *            the relation to unmark.
	 */
	public void unflagConcatOnlyRelation(Relation relation) {
		int index = this.concatOnlyRelations[0].indexOf(relation);
		if (index >= 0) {
			this.concatOnlyRelations[0].remove(index);
			this.concatOnlyRelations[1].remove(index);
		}
	}

	/**
	 * Return the set of concat-only relations. It may be empty, but never null.
	 * 
	 * @return the set of concat-only relations.
	 */
	public Collection getConcatOnlyRelations() {
		return this.concatOnlyRelations[0];
	}

	/**
	 * Return the concat type of concat-only relation relation. It will return
	 * null if there is no such concat-only relation.
	 * 
	 * @param relation
	 *            the relation to return the concat type for.
	 * @return the concat type of the relation, or null if it is not flagged as
	 *         concat-only.
	 */
	public ConcatRelationType getConcatRelationType(Relation relation) {
		int index = this.concatOnlyRelations[0].indexOf(relation);
		if (index >= 0)
			return (ConcatRelationType) this.concatOnlyRelations[1].get(index);
		else
			return null;
	}

	/**
	 * Synchronise this dataset with the schema that is providing its tables.
	 * Synchronisation means checking the columns and relations and removing any
	 * that have disappeared. The dataset is then regenerated.
	 * 
	 * @throws SQLException
	 *             never thrown - this is inherited from {@link Schema} but does
	 *             not apply here because we are not doing any database
	 *             communications.
	 * @throws BuilderException
	 *             never thrown - this is inherited from {@link Schema} but does
	 *             not apply here because we are not doing any logical work with
	 *             the schema.
	 */
	public void synchronise() throws SQLException, BuilderException {
		// Start off by marking all existing flagged relations as having been
		// removed.
		Set deadRelations = new HashSet(this.maskedRelations);
		deadRelations.addAll(this.subclassedRelations);
		deadRelations.addAll(this.concatOnlyRelations[0]);
		deadRelations.addAll(this.restrictedRelations[0]);

		// Iterate through tables in each schema. For each table,
		// find all the relations on that table and remove them
		// from the set of dead relations we started with.
		for (Iterator i = this.getMart().getSchemas().iterator(); i.hasNext();) {
			Schema s = (Schema) i.next();
			for (Iterator j = s.getTables().iterator(); j.hasNext();) {
				Table t = (Table) j.next();
				deadRelations.removeAll(t.getRelations());
			}
		}

		// All that is left in the initial set now is dead, and no longer exists
		// in the underlying schemas. Therefore, we can unmark them.
		for (Iterator i = deadRelations.iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();
			this.maskedRelations.remove(r);
			this.unflagSubclassRelation(r);
			this.unflagConcatOnlyRelation(r);
			this.unflagRestrictedRelation(r);
		}

		// Remember the names for renamed tables and columns.
		List renamedColumns = new ArrayList();
		List renamedTables = new ArrayList();
		for (Iterator i = this.getTables().iterator(); i.hasNext();) {
			Table tbl = (Table) i.next();
			if (!tbl.getOriginalName().equals(tbl.getName()))
				renamedTables.add(tbl);
			for (Iterator j = tbl.getColumns().iterator(); j.hasNext();) {
				Column col = (Column) j.next();
				if (!col.getOriginalName().equals(col.getName()))
					renamedColumns.add(col);
			}
		}

		// Remember all the ExpressionColumns
		List expressionColumns = new ArrayList();
		for (Iterator i = this.getTables().iterator(); i.hasNext();)
			for (Iterator j = ((Table) i.next()).getColumns().iterator(); j
					.hasNext();) {
				Column col = (Column) j.next();
				if (col instanceof ExpressionColumn)
					expressionColumns.add(col);
			}

		// Regenerate the dataset
		this.regenerate();

		// Reapply the names for renamed tables and columns. Look up using
		// original names else they won't be found. Do columns first before
		// table names have changed completely.
		for (Iterator i = renamedColumns.iterator(); i.hasNext();) {
			Column col = (Column) i.next();
			Column newCol = this.getTableByName(
					col.getTable().getOriginalName()).getColumnByName(
					col.getOriginalName());
			try {
				newCol.setName(col.getName());
			} catch (Throwable t) {
				// Ignore, and leave the name as it is.
			}
		}
		for (Iterator i = renamedTables.iterator(); i.hasNext();) {
			Table tbl = (Table) i.next();
			Table newTbl = this.getTableByName(tbl.getOriginalName());
			try {
				newTbl.setName(tbl.getName());
			} catch (Throwable t) {
				// Ignore, and leave the name as it is.
			}
		}

		// Regenerate all the ExpressionColumns and mark all
		// their dependencies again. Drop any that refer to columns
		// that no longer exist.
		for (Iterator i = expressionColumns.iterator(); i.hasNext();) {
			ExpressionColumn oldExpCol = (ExpressionColumn) i.next();
			// Work out what table it came from. Gone? Drop.
			DataSetTable newExpColTable = (DataSetTable) this
					.getTableByName(oldExpCol.getTable().getName());
			if (newExpColTable == null)
				continue;
			// Get all the dependent columns. Any gone? Drop.
			boolean allFound = true;
			Map newDependencies = new TreeMap();
			for (Iterator j = oldExpCol.getAliases().keySet().iterator(); j
					.hasNext();) {
				DataSetColumn dependency = (DataSetColumn) newExpColTable
						.getColumnByName(((DataSetColumn) j.next()).getName());
				String alias = (String) oldExpCol.getAliases().get(dependency);
				if (dependency == null)
					allFound = false;
				else
					newDependencies.put(dependency, alias);
			}
			if (!allFound)
				continue;
			// Create a new ExpressionColumn based on the new columns.
			try {
				ExpressionColumn newExpCol = new ExpressionColumn(oldExpCol
						.getName(), newExpColTable);
				newExpCol.getAliases().putAll(newDependencies);
				newExpCol.setExpression(oldExpCol.getExpression());
				newExpCol.setGroupBy(oldExpCol.getGroupBy());
				for (Iterator j = newDependencies.keySet().iterator(); j
						.hasNext();)
					((DataSetColumn) j.next()).setDependency(true);
			} catch (Throwable t) {
				// Should never happen!
				throw new MartBuilderInternalError(t);
			}
		}

		// Regenerate all the columns in the restricted relations,
		// and remove any restrictions that reference columns that
		// no longer exist.
		List deadRelationRestrictions = new ArrayList();
		for (int i = 0; i < this.restrictedRelations[0].size(); i++) {
			Relation rel = (Relation) this.restrictedRelations[0].get(i);
			DataSetRelationRestriction restriction = (DataSetRelationRestriction) this.restrictedRelations[1]
					.get(i);
			// First table.
			List oldAliases = new ArrayList(restriction.getFirstTableAliases()
					.keySet());
			boolean destroy = false;
			for (Iterator j = oldAliases.iterator(); j.hasNext() && !destroy;) {
				Column oldCol = (Column) j.next();
				Table newTbl = oldCol.getTable().getSchema().getTableByName(
						oldCol.getTable().getName());
				if (newTbl == null)
					destroy = true;
				else {
					String alias = (String) restriction.getFirstTableAliases()
							.get(oldCol);
					Column newCol = (Column) newTbl.getColumnByName(oldCol
							.getName());
					if (newCol == null)
						destroy = true;
					else {
						restriction.getFirstTableAliases().remove(oldCol);
						restriction.getFirstTableAliases().put(newCol, alias);
					}
				}
			}
			if (destroy)
				deadRelationRestrictions.add(rel);
			// Second table.
			oldAliases = new ArrayList(restriction.getSecondTableAliases()
					.keySet());
			destroy = false;
			for (Iterator j = oldAliases.iterator(); j.hasNext() && !destroy;) {
				Column oldCol = (Column) j.next();
				Table newTbl = oldCol.getTable().getSchema().getTableByName(
						oldCol.getTable().getName());
				if (newTbl == null)
					destroy = true;
				else {
					String alias = (String) restriction.getSecondTableAliases()
							.get(oldCol);
					Column newCol = (Column) newTbl.getColumnByName(oldCol
							.getName());
					if (newCol == null)
						destroy = true;
					else {
						restriction.getSecondTableAliases().remove(oldCol);
						restriction.getSecondTableAliases().put(newCol, alias);
					}
				}
			}
			if (destroy)
				deadRelationRestrictions.add(rel);
		}
		// Remove the dead ones that reference columns that no longer exist.
		for (Iterator i = deadRelationRestrictions.iterator(); i.hasNext();)
			this.unflagRestrictedRelation((Relation) i.next());
	}

	/**
	 * This method recreates all the tables in the dataset, using the relation
	 * flags as a guide as to how to treat each table it comes across in the
	 * schema.
	 * <p>
	 * Columns that were masked or partitioned are preserved only if after
	 * regeneration a column with the same name still exists in the same table.
	 */
	private void regenerate() {
		// Clear all our tables out as they will all be rebuilt.
		this.tables.clear();

		// Identify main table.
		Table centralTable = this.getCentralTable();
		// If central table has subclass relations and is at the M key
		// end, then follow them to the real central table.
		boolean found;
		do {
			found = false;
			for (Iterator i = centralTable.getRelations().iterator(); i
					.hasNext()
					&& !found;) {
				Relation r = (Relation) i.next();
				if (this.getSubclassedRelations().contains(r)
						&& r.getManyKey().getTable().equals(centralTable)) {
					centralTable = r.getOneKey().getTable();
					found = true;
				}
			}
		} while (found);

		// Generate the main table. It will recursively generate all the others.
		this.generateDataSetTable(DataSetTableType.MAIN, null, centralTable,
				null);

		// Attempt to reuse existing masked columns.
		// Do this by first creating a set of columns from the new tables
		// with identical table and column names to the old set of masked
		// columns. Once the set is constructed, use it to replace the old
		// set of masked columns.
		Collection newMaskedWrappedCols = new ArrayList();
		for (Iterator i = this.maskedDataSetColumns.iterator(); i.hasNext();) {
			DataSetColumn col = (DataSetColumn) i.next();
			if (this.getTables().contains(col.getTable())) {
				// Find the new column with the same name.
				Table newTable = this.getTableByName(col.getTable().getName());
				Column newCol = newTable.getColumnByName(col.getName());

				// If found, mask the column.
				if (newCol != null)
					newMaskedWrappedCols.add(newCol);
			}
		}
		this.maskedDataSetColumns.retainAll(newMaskedWrappedCols);

		// Attempt to reuse existing partitioned columns.
		// Do this by first creating a set of columns from the new tables
		// with identical table and column names to the old set of masked
		// columns. Once the set is constructed, use it to replace the old
		// set of partitioned columns.
		Collection newPartDSCols = new ArrayList();
		for (Iterator i = this.partitionedDataSetColumns[0].iterator(); i
				.hasNext();) {
			WrappedColumn col = (WrappedColumn) i.next();
			if (this.getTables().contains(col.getTable())) {
				// Find the new column with the same name.
				Table newTable = this.getTableByName(col.getTable().getName());
				Column newCol = newTable.getColumnByName(col.getName());

				// If found, partition the column.
				if (newCol != null)
					newPartDSCols.add(newCol);
			}
		}
		for (Iterator i = this.partitionedDataSetColumns[0].iterator(); i
				.hasNext();)
			if (!newPartDSCols.contains(i.next()))
				i.remove();
	}

	private void generateDataSetTable(DataSetTableType type,
			DataSetTable parentDSTable, Table realTable, Relation sourceRelation) {
		// Create the dataset table.
		DataSetTable dsTable = null;
		try {
			dsTable = new DataSetTable(realTable.getName(), this, type,
					realTable, sourceRelation);
		} catch (Throwable t) {
			throw new MartBuilderInternalError(t);
		}

		// Create the three relation-table pair queues we will work with. The
		// normal queue holds pairs of relations and tables. The other two hold
		// a list of relations only, the tables being the FK ends of each
		// relation. The normal queue has a third object associated with each
		// entry, which specifies whether to treat the 1:M relations from
		// the merged table as dimensions or not.
		List normalQ = new ArrayList();
		List subclassQ = new ArrayList();
		List dimensionQ = new ArrayList();

		// Set up a set of relations that have been followed so far.
		Set relationsFollowed = new HashSet();

		// Set up a list to hold columns for this table's primary key.
		List dsTablePKCols = new ArrayList();

		// If the parent dataset table is not null, add columns from it
		// as appropriate. Dimension tables get just the PK, and an
		// FK linking them back. Subclass tables get all columns, plus
		// the PK with FK link. Also add the relations we followed to
		// get all these columns.
		if (parentDSTable != null) {
			// Make a list to hold the child table's FK cols.
			List dsTableFKCols = new ArrayList();

			// Get the primary key of the parent DS table.
			PrimaryKey parentDSTablePK = parentDSTable.getPrimaryKey();

			// Don't follow the parent's relations again.
			relationsFollowed.addAll(parentDSTable.getUnderlyingRelations());

			// Loop over each column in the parent table. If this is
			// a subclass table, add it. If it is a dimension table,
			// only add it if it is in the PK. In either case, if it
			// is in the PK, add it both to the child PK and the child FK.
			for (Iterator i = parentDSTable.getColumns().iterator(); i
					.hasNext();) {
				DataSetColumn parentDSCol = (DataSetColumn) i.next();
				// Skip columns that are not in the primary key, if
				// we are not making a subclass table.
				if (!parentDSTablePK.getColumns().contains(parentDSCol)
						&& !type.equals(DataSetTableType.MAIN_SUBCLASS))
					continue;
				// Otherwise, create a copy of the column.
				try {
					DataSetColumn dsTableCol;
					if (parentDSCol instanceof InheritedColumn)
						dsTableCol = new InheritedColumn(dsTable,
								((InheritedColumn) parentDSCol)
										.getInheritedColumn());
					else
						dsTableCol = new InheritedColumn(dsTable, parentDSCol);
					// Add the column to the child's PK and FK, if it was in
					// the parent PK only.
					if (parentDSTablePK.getColumns().contains(parentDSCol)) {
						dsTablePKCols.add(dsTableCol);
						dsTableFKCols.add(dsTableCol);
					}
				} catch (AlreadyExistsException e) {
					throw new MartBuilderInternalError(e);
				}
			}

			try {
				// Create the child FK.
				ForeignKey dsTableFK = new GenericForeignKey(dsTableFKCols);
				dsTable.addForeignKey(dsTableFK);
				// Link the child FK to the dataset
				new GenericRelation(parentDSTablePK, dsTableFK,
						Cardinality.MANY);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}
		}

		// Process the table. If a source relation was specified,
		// ignore the cols in the key in the table that is part of that
		// source relation, else they'll get duplicated. This will always
		// be the FK or many end, as that's the only way we can get subclass and
		// dimension tables. This operation will populate the initial
		// pairs in the normal, subclass and dimension queues. We only
		// want dimensions constructed if we are not already constructing
		// a dimension ourselves.
		this.processTable(dsTable, dsTablePKCols, realTable, normalQ,
				subclassQ, dimensionQ, sourceRelation, relationsFollowed, !type
						.equals(DataSetTableType.DIMENSION));

		// Process the normal queue. This merges tables into the dataset
		// table using the relation specified in each pair in the queue.
		for (int i = 0; i < normalQ.size(); i++) {
			Object[] triple = (Object[]) normalQ.get(i);
			Relation mergeSourceRelation = (Relation) triple[0];
			Table mergeTable = (Table) triple[1];
			boolean makeDimensions = ((Boolean) triple[2]).booleanValue();
			this.processTable(dsTable, dsTablePKCols, mergeTable, normalQ,
					subclassQ, dimensionQ, mergeSourceRelation,
					relationsFollowed, makeDimensions);
		}

		// If the primary key is empty, then we must create one, else we
		// will run into trouble later. Therefore, all tables must contain
		// unique rows.
		if (dsTablePKCols.isEmpty()) {
			// Create the PK by adding every wrapped column in the
			// dataset table. We don't care about other columns
			// as that would create primary key update hell.
			for (Iterator i = dsTable.getColumns().iterator(); i.hasNext();) {
				DataSetColumn dsCol = (DataSetColumn) i.next();
				if (dsCol instanceof WrappedColumn)
					dsTablePKCols.add(dsCol);
			}
		}

		// Add a schema name column, if we are partitioning by schema name.
		// If the table has a PK by this stage, add the schema name column
		// to it, otherwise don't bother as it'll introduce unnecessary
		// restrictions if we do. We only need do this to main tables, as
		// others will inherit it through their foreign key.
		if (type.equals(DataSetTableType.MAIN)
				&& (realTable.getSchema() instanceof SchemaGroup))
			try {
				DataSetColumn schemaNameCol = new SchemaNameColumn(Resources
						.get("schemaColumnName"), dsTable);
				if (!dsTablePKCols.isEmpty())
					dsTablePKCols.add(schemaNameCol);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}

		// Create the primary key on this table. First check that
		// all columns end in '_key'. If they don't make it so.
		try {
			for (Iterator i = dsTablePKCols.iterator(); i.hasNext(); ) {
				DataSetColumn col = (DataSetColumn)i.next();
				if (!col.getName().endsWith(Resources.get("pkSuffix")))
					col.setName(col.getName()+Resources.get("pkSuffix"));
			}
			dsTable.setPrimaryKey(new GenericPrimaryKey(dsTablePKCols));
		} catch (Throwable t) {
			throw new MartBuilderInternalError(t);
		}

		// Process the subclass relations of this table.
		for (int i = 0; i < subclassQ.size(); i++) {
			Relation subclassRelation = (Relation) subclassQ.get(i);
			this.generateDataSetTable(DataSetTableType.MAIN_SUBCLASS, dsTable,
					subclassRelation.getManyKey().getTable(), subclassRelation);
		}

		// Process the dimension relations of this table. For 1:M it's easy.
		// For M:M, we have to work out which end is connected to the real
		// table, then process the table at the other end of the relation.
		for (int i = 0; i < dimensionQ.size(); i++) {
			Relation dimensionRelation = (Relation) dimensionQ.get(i);
			if (dimensionRelation.isOneToMany())
				this.generateDataSetTable(DataSetTableType.DIMENSION, dsTable,
						dimensionRelation.getManyKey().getTable(),
						dimensionRelation);
			else
				this.generateDataSetTable(DataSetTableType.DIMENSION, dsTable,
						dimensionRelation.getFirstKey().getTable().equals(
								realTable) ? dimensionRelation.getSecondKey()
								.getTable() : dimensionRelation.getFirstKey()
								.getTable(), dimensionRelation);
		}
	}

	private void processTable(DataSetTable dsTable, List dsTablePKCols,
			Table mergeTable, List normalQ, List subclassQ, List dimensionQ,
			Relation sourceRelation, Set relationsFollowed,
			boolean makeDimensions) {

		// Don't ignore any keys by default.
		Key ignoreKey = null;

		// Did we get here via somewhere else?
		if (sourceRelation != null) {
			// Work out what key to ignore.
			ignoreKey = sourceRelation.getFirstKey().getTable().equals(
					mergeTable) ? sourceRelation.getFirstKey() : sourceRelation
					.getSecondKey();

			// Add the relation and key to the list followed for the table.
			dsTable.getUnderlyingRelations().add(sourceRelation);
			dsTable.getUnderlyingKeys().add(
					sourceRelation.getOtherKey(ignoreKey));

			// Mark the source relation as followed.
			relationsFollowed.add(sourceRelation);
		}

		// Work out the merge table's PK.
		PrimaryKey mergeTablePK = mergeTable.getPrimaryKey();

		// Add all columns from merge table to dataset table, except those in
		// the ignore key.
		for (Iterator i = mergeTable.getColumns().iterator(); i.hasNext();) {
			Column c = (Column) i.next();

			// Ignore those in the key followed to get here.
			if (ignoreKey != null && ignoreKey.getColumns().contains(c))
				continue;

			// Create a wrapped column for this column.
			try {
				WrappedColumn wc = new WrappedColumn(c, dsTable, sourceRelation);

				// If the column was in the merge table's PK, add it to the ds
				// tables's PK too.
				if (mergeTablePK != null
						&& mergeTablePK.getColumns().contains(c))
					dsTablePKCols.add(wc);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}
		}

		// Update the three queues with relations.
		for (Iterator i = mergeTable.getRelations().iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();

			// Don't repeat relations.
			if (relationsFollowed.contains(r))
				continue;
			else
				relationsFollowed.add(r);

			// Don't follow masked or incorrect relations, or relations
			// between incorrect keys.
			if (this.maskedRelations.contains(r)
					|| r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
					|| r.getFirstKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT)
					|| r.getSecondKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
				continue;

			// Are we at the 1 end of a 1:M, or at either end of a M:M?
			else if (r.isManyToMany()
					|| (r.isOneToMany() && r.getOneKey().getTable().equals(
							mergeTable))) {

				// Concatenate concat-only relations.
				if (this.concatOnlyRelations[0].contains(r))
					try {
						new ConcatRelationColumn(Resources
								.get("concatColumnPrefix")
								+ mergeTable.getName(), dsTable, r);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}

				// Subclass subclassed relations, if we are currently
				// not building a dimension table.
				else if (this.subclassedRelations.contains(r)
						&& !dsTable.getType()
								.equals(DataSetTableType.DIMENSION))
					subclassQ.add(r);

				// Dimensionize dimension relations, which are all other 1:M
				// or M:M relations, if we are not constructing a dimension
				// table, and are currently intending to construct dimensions.
				else if (makeDimensions
						&& !dsTable.getType()
								.equals(DataSetTableType.DIMENSION))
					dimensionQ.add(r);
			}

			// Follow all others. If we follow a 1:1, and we are currently
			// including dimensions, include them from the 1:1 as well.
			// Otherwise, stop including dimensions on subsequent tables.
			else
				normalQ.add(new Object[] {
						r,
						(r.getFirstKey().getTable().equals(mergeTable) ? r
								.getSecondKey().getTable() : r.getFirstKey()
								.getTable()),
						Boolean.valueOf(makeDimensions && r.isOneToOne()) });
		}
	}

	public String toString() {
		return this.getName();
	}

	public int hashCode() {
		return this.toString().hashCode();
	}

	public int compareTo(Object o) throws ClassCastException {
		DataSet w = (DataSet) o;
		return this.toString().compareTo(w.toString());
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof DataSet))
			return false;
		DataSet w = (DataSet) o;
		return w.toString().equals(this.toString());
	}

	/**
	 * Sets the post-creation optimiser type this dataset will use.
	 * 
	 * @param optimiser
	 *            the optimiser type to use.
	 */
	public void setDataSetOptimiserType(DataSetOptimiserType optimiser) {
		// Do it.
		this.optimiser = optimiser;
	}

	/**
	 * Returns the post-creation optimiser type this dataset will use.
	 * 
	 * @return the optimiser type that will be used.
	 */
	public DataSetOptimiserType getDataSetOptimiserType() {
		return this.optimiser;
	}

	public void addTable(Table table) throws AlreadyExistsException,
			AssociationException {
		// We only accept dataset tables.
		if (!(table instanceof DataSetTable))
			throw new AssociationException(Resources.get("tableNotDSTable"));

		// Call the super version to do the work.
		super.addTable(table);
	}

	/**
	 * Represents a method of partitioning by column. There are no methods.
	 * Actual logic to divide up by column is left to the mart constructor to
	 * decide.
	 */
	public interface PartitionedColumnType {
		/**
		 * Use this class to refer to a column partitioned by every unique
		 * value.
		 */
		public class UniqueValues implements PartitionedColumnType {
			public String toString() {
				return "UniqueValues";
			}
		}

		/**
		 * Use this class to partition on a set of values - ie. only columns
		 * with one of these values will be returned.
		 */
		public class ValueCollection implements PartitionedColumnType {
			private Set values = new HashSet();

			private boolean includeNull;

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
			public ValueCollection(Collection values, boolean includeNull) {
				this.values = new HashSet();
				this.values.addAll(values);
				this.includeNull = includeNull;
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

			public String toString() {
				return "ValueCollection:" + this.values.toString();
			}

			public boolean equals(Object o) {
				if (o == null || !(o instanceof ValueCollection))
					return false;
				ValueCollection vc = (ValueCollection) o;
				return (vc.getValues().equals(this.values) && vc
						.getIncludeNull() == this.includeNull);
			}
		}

		/**
		 * Use this class to partition on a single value - ie. only rows
		 * matching this value will be returned.
		 */
		public class SingleValue extends ValueCollection {
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
			public SingleValue(String value, boolean useNull) {
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

			public String toString() {
				return "SingleValue:" + this.value;
			}
		}
	}

	/**
	 * Represents a method of concatenating values in a key referenced by a
	 * concat-only {@link Relation}. It simply represents the separator to use.
	 */
	public static class ConcatRelationType implements Comparable {
		private final String name;

		private final String valueSeparator;

		private final String recordSeparator;

		/**
		 * Use this constant to refer to value-separation by commas, and
		 * record-separation by commas.
		 */
		public static final ConcatRelationType COMMA_COMMA = new ConcatRelationType(
				"COMMA_COMMA", ",", ",");

		/**
		 * Use this constant to refer to value-separation by commas, and
		 * record-separation by spaces.
		 */
		public static final ConcatRelationType COMMA_SPACE = new ConcatRelationType(
				"COMMA_SPACE", ",", " ");

		/**
		 * Use this constant to refer to value-separation by commas, and
		 * record-separation by tabs.
		 */
		public static final ConcatRelationType COMMA_TAB = new ConcatRelationType(
				"COMMA_TAB", ",", "\t");

		/**
		 * Use this constant to refer to value-separation by spaces, and
		 * record-separation by commas.
		 */
		public static final ConcatRelationType SPACE_COMMA = new ConcatRelationType(
				"SPACE_COMMA", " ", ",");

		/**
		 * Use this constant to refer to value-separation by spaces, and
		 * record-separation by spaces.
		 */
		public static final ConcatRelationType SPACE_SPACE = new ConcatRelationType(
				"SPACE_SPACE", " ", " ");

		/**
		 * Use this constant to refer to value-separation by spaces, and
		 * record-separation by tabs.
		 */
		public static final ConcatRelationType SPACE_TAB = new ConcatRelationType(
				"SPACE_TAB", " ", "\t");

		/**
		 * Use this constant to refer to value-separation by tabs, and
		 * record-separation by commas.
		 */
		public static final ConcatRelationType TAB_COMMA = new ConcatRelationType(
				"TAB_COMMA", "\t", ",");

		/**
		 * Use this constant to refer to value-separation by tabs, and
		 * record-separation by spaces.
		 */
		public static final ConcatRelationType TAB_SPACE = new ConcatRelationType(
				"TAB_SPACE", "\t", " ");

		/**
		 * Use this constant to refer to value-separation by tabs, and
		 * record-separation by tabs.
		 */
		public static final ConcatRelationType TAB_TAB = new ConcatRelationType(
				"TAB_TAB", "\t", "\t");

		/**
		 * The private constructor takes parameters which define the name this
		 * concat type object will display when printed, and the separator to
		 * use between values and records that have been concatenated.
		 * 
		 * @param name
		 *            the name of the concat type.
		 * @param valueSeparator
		 *            the separator for values in this concat type.
		 * @param recordSeparator
		 *            the separator for records in this concat type.
		 */
		private ConcatRelationType(String name, String valueSeparator,
				String recordSeparator) {
			this.name = name;
			this.valueSeparator = valueSeparator;
			this.recordSeparator = recordSeparator;
		}

		/**
		 * Displays the name of this concat type object.
		 * 
		 * @return the name of this concat type object.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Displays the value separator for this concat type object.
		 * 
		 * @return the value separator for this concat type object.
		 */
		public String getValueSeparator() {
			return this.valueSeparator;
		}

		/**
		 * Displays the record separator for this concat type object.
		 * 
		 * @return the record separator for this concat type object.
		 */
		public String getRecordSeparator() {
			return this.recordSeparator;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			ConcatRelationType pct = (ConcatRelationType) o;
			return this.toString().compareTo(pct.toString());
		}

		public boolean equals(Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}

	/**
	 * This special table represents the merge of one or more other tables by
	 * following a series of relations rooted in a similar series of keys. As
	 * such it has no real columns of its own, so every column is from another
	 * table and is given an alias.
	 */
	public static class DataSetTable extends GenericTable {
		private final Relation sourceRelation;

		private final List underlyingRelations;

		private final List underlyingKeys;

		private final DataSetTableType type;

		private final Table underlyingTable;

		/**
		 * The constructor calls the parent table constructor. It uses a dataset
		 * as a parent schema for itself. You must also supply a type that
		 * describes this as a main table, dimension table, etc.
		 * 
		 * @param name
		 *            the table name.
		 * @param ds
		 *            the dataset to hold this table in.
		 * @param type
		 *            the type that best describes this table.
		 * @param relation
		 *            the relation that controls the existence of this table.
		 *            Can be null.
		 * @throws AlreadyExistsException
		 *             this should never actually happen, but is required as the
		 *             super constructor throws it.
		 */
		public DataSetTable(String name, DataSet ds, DataSetTableType type,
				Table underlyingTable, Relation sourceRelation)
				throws AlreadyExistsException {
			// Super constructor first, using an alias to prevent duplicates.
			super(generateAlias(name, ds), ds);

			// Remember the other settings.
			this.underlyingTable = underlyingTable;
			this.type = type;
			this.sourceRelation = sourceRelation;
			this.underlyingRelations = new ArrayList();
			this.underlyingKeys = new ArrayList();
		}

		/**
		 * Returns the real table upon which this dataset table is based.
		 * 
		 * @return the real table providing the basis for this dataset table.
		 */
		public Table getUnderlyingTable() {
			return this.underlyingTable;
		}

		/**
		 * Internal method that generates a safe alias/name for a table. The
		 * first try is always the original table name, followed by attempts
		 * with an underscore and a sequence number appended.
		 * 
		 * @param name
		 *            the first name to try.
		 * @param ds
		 *            the dataset that this table is being added to.
		 * @return the resulting safe alias/name.
		 */
		protected static String generateAlias(String name, DataSet ds) {
			String alias = name;
			int aliasNumber = 2;
			while (ds.getTableByName(alias) != null) {
				// Alias is original name appended with _2, _3, _4 etc.
				alias = name + "_" + (aliasNumber++);
			}
			return alias;
		}

		/**
		 * Returns the type of this table specified at construction time.
		 * 
		 * @return the type of this table.
		 */
		public DataSetTableType getType() {
			return this.type;
		}

		/**
		 * Sets the list of relations used to construct this table.
		 * 
		 * @param relations
		 *            the list of relations of this table. May be empty but
		 *            never null.
		 */
		public void setUnderlyingRelations(List relations) {
			// Check the relations and save them.
			this.underlyingRelations.clear();
			this.underlyingRelations.addAll(relations);
		}

		/**
		 * Sets the list of keys used to construct this table.
		 * 
		 * @param keys
		 *            the list of keys of this table. May be empty but never
		 *            null.
		 */
		public void setUnderlyingKeys(List keys) {
			// Check the keys and save them.
			this.underlyingKeys.clear();
			this.underlyingKeys.addAll(keys);
		}

		/**
		 * Returns the list of relations used to construct this table.
		 * 
		 * @return the list of relations of this table. May be empty but never
		 *         null.
		 */
		public List getUnderlyingRelations() {
			return this.underlyingRelations;
		}

		/**
		 * Returns the list of keys used to construct this table. These keys are
		 * in the same order as the relations, and indicate which key to root
		 * each relation at.
		 * 
		 * @return the list of keys of this table. May be empty but never null.
		 */
		public List getUnderlyingKeys() {
			return this.underlyingKeys;
		}

		/**
		 * Gets the relation which controls the existince of this table.
		 * Modifying that relation will directly affect the structure or
		 * existence of this table. It can be <tt>null</tt>.
		 * 
		 * @return the source relation.
		 */
		public Relation getSourceRelation() {
			return this.sourceRelation;
		}

		public void addColumn(Column column) throws AlreadyExistsException,
				AssociationException {
			// We only accept dataset columns.
			if (!(column instanceof DataSetColumn))
				throw new AssociationException(Resources
						.get("columnNotDatasetColumn"));

			// Call the super to add it.
			super.addColumn(column);
		}
	}

	/**
	 * A column on a dataset table has to be one of the types of dataset column
	 * available from this class. All types can be renamed safely as they will
	 * delegate the necessary calls to the dataset table on the caller's behalf.
	 */
	public static class DataSetColumn extends GenericColumn {
		private final Relation underlyingRelation;

		private boolean dependency;

		/**
		 * This constructor gives the column a name.
		 * 
		 * @param name
		 *            the name to give this column.
		 * @param dsTable
		 *            the parent dataset table.
		 * @param underlyingRelation
		 *            the relation that provided this column. The underlying
		 *            relation can be null in only one case - when the table is
		 *            a {@link DataSetTableType#MAIN} table. It is also null for
		 *            {@link SchemaNameColumn} instances.
		 * @throws AlreadyExistsException
		 *             inherited from the super constructor, but should never
		 *             get thrown.
		 */
		public DataSetColumn(String name, DataSetTable dsTable,
				Relation underlyingRelation) throws AlreadyExistsException {
			// Call the super constructor using the alias generator to
			// ensure we have a unique name.
			super(generateAlias(name, dsTable), dsTable);

			// Remember the rest.
			this.underlyingRelation = underlyingRelation;

			// Set up default dependency.
			this.dependency = false;
		}

		/**
		 * Internal method that generates a safe alias/name for a column. The
		 * first try is always the original column name, followed by attempts
		 * with an underscore and a sequence number appended.
		 * 
		 * @param name
		 *            the first name to try.
		 * @param dsTable
		 *            the dataset table that this column is being added to.
		 * @return the result.
		 */
		protected static String generateAlias(String name, DataSetTable dsTable) {
			String alias = name;
			int aliasNumber = 2;
			while (dsTable.getColumnByName(alias) != null) {
				// Alias is original name appended with _2, _3, _4 etc.
				alias = name + "_" + (aliasNumber++);
			}
			return alias;
		}

		/**
		 * Returns the underlying relation that provided this column. If it
		 * returns null and this table is a {@link DataSetTableType#MAIN} table,
		 * then that means the column came from the table's real table.
		 * 
		 * @return the relation that underpins this column.
		 */
		public Relation getUnderlyingRelation() {
			return this.underlyingRelation;
		}

		/**
		 * Changes the dependency flag on this column.
		 * 
		 * @param the
		 *            new dependency flag. <tt>true</tt> indicates that this
		 *            column is a dependency for an expression column. If this
		 *            is the case, then the column will get selected regardless
		 *            of it's masking flag. However, if it is masked, it will be
		 *            removed again after the dependency is satisified.
		 */
		public void setDependency(boolean dependency) {
			this.dependency = dependency;
		}

		/**
		 * Retrieves the current dependency flag on this column.
		 * 
		 * @param <tt>true</tt> if this column is a dependency for another one.
		 */
		public boolean getDependency() {
			return this.dependency;
		}

		/**
		 * A column on a dataset table that wraps an existing column but is
		 * otherwise identical to a normal column. It assigns itself an alias if
		 * the original name is already used in the dataset table.
		 */
		public static class WrappedColumn extends DataSetColumn {
			private final Column column;

			/**
			 * This constructor wraps an existing column. It also assigns an
			 * alias to the wrapped column if another one with the same name
			 * already exists on this table.
			 * 
			 * @param column
			 *            the column to wrap.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param underlyingRelation
			 *            the relation that provided this column. The underlying
			 *            relation can be null in only one case - when the table
			 *            is a {@link DataSetTableType#MAIN} table.
			 * @throws AlreadyExistsException
			 *             inherited from the super constructor, but should
			 *             never get thrown.
			 */
			public WrappedColumn(Column column, DataSetTable dsTable,
					Relation underlyingRelation) throws AlreadyExistsException {
				// Call the parent which will use the alias generator for us.
				super(column.getName(), dsTable, underlyingRelation);

				// Remember the wrapped column.
				this.column = column;
			}

			/**
			 * Returns the wrapped column.
			 * 
			 * @return the wrapped {@link Column}.
			 */
			public Column getWrappedColumn() {
				return this.column;
			}
		}

		/**
		 * A column on a dataset table that indicates the concatenation of the
		 * primary key values of a record in some table beyond a concat-only
		 * relation. They take a reference to the concat-only relation.
		 */
		public static class ConcatRelationColumn extends DataSetColumn {
			/**
			 * The constructor takes a name for this column-to-be, and the
			 * dataset table on which it is to be constructed, and the relation
			 * it represents.
			 * 
			 * @param name
			 *            the name to give this column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param underlyingRelation
			 *            the concat-only relation that provided this column. *
			 * @throws AlreadyExistsException
			 *             inherited from the super constructor, but should
			 *             never get thrown.
			 */
			public ConcatRelationColumn(String name, DataSetTable dsTable,
					Relation concatRelation) throws AlreadyExistsException,
					AssociationException {
				// Super first, which will do the alias generation for us.
				super(name, dsTable, concatRelation);

				// Make sure it really is a concat relation.
				if (!((DataSet) dsTable.getSchema()).getConcatOnlyRelations()
						.contains(concatRelation))
					throw new AssociationException(Resources
							.get("relationNotConcatRelation"));
			}
		}

		/**
		 * A column on a dataset table that should be populated with the name of
		 * the table provider providing the data in this row.
		 */
		public static class SchemaNameColumn extends DataSetColumn {
			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here.
			 * 
			 * @param name
			 *            the name to give this column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @throws AlreadyExistsException
			 *             inherited from the super constructor, but should
			 *             never get thrown.
			 */
			public SchemaNameColumn(String name, DataSetTable dsTable)
					throws AlreadyExistsException {
				// The super constructor will make the alias for us.
				super(name, dsTable, null);
			}
		}

		/**
		 * A column on a dataset table that is inherited from a parent dataset
		 * table.
		 */
		public static class InheritedColumn extends DataSetColumn {
			private DataSetColumn dsColumn;

			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here. The name is inherited from the column too.
			 * 
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param dsColumn
			 *            the column to inherit.
			 * @throws AlreadyExistsException
			 *             inherited from the super constructor, but should
			 *             never get thrown.
			 */
			public InheritedColumn(DataSetTable dsTable, DataSetColumn dsColumn)
					throws AlreadyExistsException {
				// The super constructor will make the alias for us.
				super(dsColumn.getName(), dsTable, null);
				// Remember the inherited column.
				this.dsColumn = dsColumn;
			}

			/**
			 * Returns the column that has been inherited by this column.
			 * 
			 * @return the inherited column.
			 */
			public DataSetColumn getInheritedColumn() {
				return this.dsColumn;
			}
		}

		/**
		 * A column on a dataset table that is an expression bringing together
		 * values from other columns. Those columns should be marked with a
		 * dependency flag to indicate that they are still needed even if
		 * otherwise masked. If this is the case, they can be dropped after the
		 * dependent expression column has been added.
		 * <p>
		 * Note that all expression columns should be added in a single step.
		 */
		public static class ExpressionColumn extends DataSetColumn {

			private String expr;

			private Map aliases;

			private boolean groupBy;

			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here.
			 * 
			 * @param name
			 *            the name to give this column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @throws AlreadyExistsException
			 *             inherited from the super constructor, but should
			 *             never get thrown.
			 */
			public ExpressionColumn(String name, DataSetTable dsTable)
					throws AlreadyExistsException {
				// The super constructor will make the alias for us.
				super(name, dsTable, null);

				// Set some defaults.
				this.aliases = new TreeMap();
				this.expr = "";
				this.groupBy = false;
			}

			/**
			 * The actual expression. The values from the alias map will be used
			 * to refer to various columns. This value is RDBMS-specific.
			 * 
			 * @param expr
			 *            the actual expression to use.
			 */
			public void setExpression(String expr) {
				this.expr = expr;
			}

			/**
			 * Returns the expression, <i>without</i> substitution. This value
			 * is RDBMS-specific.
			 * 
			 * @return the unsubstituted expression.
			 */
			public String getExpression() {
				return this.expr;
			}

			/**
			 * Drops all the unused aliases.
			 */
			public void dropUnusedAliases() {
				List usedColumns = new ArrayList();
				for (Iterator i = this.aliases.keySet().iterator(); i.hasNext();) {
					DataSetColumn col = (DataSetColumn) i.next();
					String alias = (String) this.aliases.get(col);
					if (this.expr.indexOf(":" + alias) >= 0)
						usedColumns.add(col);
				}
				// Drop the unused aliases.
				this.aliases.keySet().retainAll(usedColumns);
			}

			/**
			 * Returns the expression, <i>with</i> substitution. This value is
			 * RDBMS-specific.
			 * 
			 * @return the substituted expression.
			 */
			public String getSubstitutedExpression() {
				String sub = this.expr;
				for (Iterator i = this.aliases.keySet().iterator(); i.hasNext();) {
					DataSetColumn wrapped = (DataSetColumn) i.next();
					String alias = ":" + (String) this.aliases.get(wrapped);
					sub = sub.replaceAll(alias, wrapped.getName());
				}
				return sub;
			}

			/**
			 * Returns the set of dependent columns.
			 * 
			 * @param the
			 *            collection of dependent columns.
			 */
			public Collection getDependentColumns() {
				return this.aliases.keySet();
			}

			/**
			 * Retrieves the map used for setting up aliases.
			 * 
			 * @return the aliases map. Keys must be {@link DataSetColumn}
			 *         instances, and values are aliases used in the expression.
			 */
			public Map getAliases() {
				return this.aliases;
			}

			/**
			 * Sets the group-by requirement for this column. If set to
			 * <tt>true</tt>, then the expression is an RDBMS call that
			 * requires group-by to be set in the select statement. The group-by
			 * should include all columns, except other expression columns and
			 * those columns that any group-by expression column depends on.
			 * 
			 * @param groupBy
			 *            the flag indicating the group-by requirement.
			 */
			public void setGroupBy(boolean groupBy) {
				this.groupBy = groupBy;
			}

			/**
			 * Returns the group-by requirement for this column. See
			 * {@link #setGroupBy(boolean)} for details.
			 * 
			 * @return the flag indicating the group-by requirement.
			 */
			public boolean getGroupBy() {
				return this.groupBy;
			}
		}
	}

	/**
	 * This class defines the various different types of DataSetTable there are.
	 */
	public static class DataSetTableType implements Comparable {
		private final String name;

		/**
		 * Use this constant to refer to a main table.
		 */
		public static final DataSetTableType MAIN = new DataSetTableType("MAIN");

		/**
		 * Use this constant to refer to a subclass of a main table.
		 */
		public static final DataSetTableType MAIN_SUBCLASS = new DataSetTableType(
				"MAIN_SUBCLASS");

		/**
		 * Use this constant to refer to a dimension table.
		 */
		public static final DataSetTableType DIMENSION = new DataSetTableType(
				"DIMENSION");

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this dataset table type object will display when printed.
		 * 
		 * @param name
		 *            the name of the dataset table type.
		 */
		private DataSetTableType(String name) {
			this.name = name;
		}

		/**
		 * Displays the name of this dataset table type object.
		 * 
		 * @return the name of this dataset table type object.
		 */
		public String getName() {
			return this.name;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			DataSetTableType c = (DataSetTableType) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}

	/**
	 * This class defines the various different ways of optimising a dataset
	 * after it has been constructed, eg. adding 'hasDimension' columns, or
	 * doing left joins on the dimensions.
	 */
	public static class DataSetOptimiserType implements Comparable {

		private final String name;

		/**
		 * Use this constant to refer to no optimisation.
		 */
		public static final DataSetOptimiserType NONE = new DataSetOptimiserType(
				"NONE");

		/**
		 * Use this constant to refer to optimising by including an extra column
		 * on the main table for each dimension and populating it with true or
		 * false..
		 */
		public static final DataSetOptimiserType COLUMN = new DataSetOptimiserType(
				"COLUMN");

		/**
		 * Use this constant to refer to no optimising by creating a separate
		 * table linked on a 1:1 basis with the main table, with one column per
		 * dimension populated with true or false.
		 */
		public static final DataSetOptimiserType TABLE = new DataSetOptimiserType(
				"TABLE");

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this optimiser type object will display when printed.
		 * 
		 * @param name
		 *            the name of the optimiser type.
		 */
		private DataSetOptimiserType(String name) {
			this.name = name;
		}

		/**
		 * Displays the name of this optimiser type object.
		 * 
		 * @return the name of this optimiser type object.
		 */
		public String getName() {
			return this.name;
		}

		public String toString() {
			return this.getName();
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		public int compareTo(Object o) throws ClassCastException {
			DataSetOptimiserType c = (DataSetOptimiserType) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}

	/**
	 * Defines the restriction on a relation, ie. a where-clause.
	 */
	public static class DataSetRelationRestriction {

		private String expr;

		private Map firstTableAliases;

		private Map secondTableAliases;

		/**
		 * This constructor gives the restriction an initial expression and two
		 * sets of aliases. The expression may not be empty, and neither of the
		 * two alias maps may be either.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param firstTableAliases
		 *            the aliases to use for columns in the first table of the
		 *            relation this restriction refers to.
		 * @param secondTableAliases
		 *            the aliases to use for columns in the second table of the
		 *            relation this restriction refers to.
		 */
		public DataSetRelationRestriction(String expr, Map firstTableAliases,
				Map secondTableAliases) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("relRestrictMissingExpression"));
			if (firstTableAliases == null || firstTableAliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("relRestrictMissingFirstAliases"));
			if (secondTableAliases == null || secondTableAliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("relRestrictMissingSecondAliases"));

			// Remember the settings.
			this.firstTableAliases = new TreeMap();
			this.firstTableAliases.putAll(firstTableAliases);
			this.secondTableAliases = new TreeMap();
			this.secondTableAliases.putAll(secondTableAliases);
			this.expr = expr;
		}

		/**
		 * The actual expression. The values from the alias maps will be used to
		 * refer to various columns. This value is RDBMS-specific.
		 * 
		 * @param expr
		 *            the actual expression to use.
		 */
		public void setExpression(String expr) {
			this.expr = expr;
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
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(String firstTablePrefix,
				String secondTablePrefix) {
			String sub = this.expr;
			// First table first.
			for (Iterator i = this.firstTableAliases.keySet().iterator(); i
					.hasNext();) {
				Column col = (Column) i.next();
				String alias = ":" + (String) this.firstTableAliases.get(col);
				sub = sub.replaceAll(alias, firstTablePrefix + "."
						+ col.getName());
			}
			// Second table second.
			for (Iterator i = this.secondTableAliases.keySet().iterator(); i
					.hasNext();) {
				Column col = (Column) i.next();
				String alias = ":" + (String) this.secondTableAliases.get(col);
				sub = sub.replaceAll(alias, secondTablePrefix + "."
						+ col.getName());
			}
			// Return the substituted expression.
			return sub;
		}

		/**
		 * Retrieves the map used for setting up aliases in the first table.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getFirstTableAliases() {
			return this.firstTableAliases;
		}

		/**
		 * Retrieves the map used for setting up aliases in the second table.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getSecondTableAliases() {
			return this.secondTableAliases;
		}
	}

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class DataSetTableRestriction {

		private String expr;

		private Map aliases;

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
		public DataSetTableRestriction(String expr, Map aliases) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("tblRestrictMissingExpression"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("tblRestrictMissingAliases"));

			// Remember the settings.
			this.aliases = new TreeMap();
			this.aliases.putAll(aliases);
			this.expr = expr;
		}

		/**
		 * The actual expression. The values from the alias maps will be used to
		 * refer to various columns. This value is RDBMS-specific.
		 * 
		 * @param expr
		 *            the actual expression to use.
		 */
		public void setExpression(String expr) {
			this.expr = expr;
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
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(String tablePrefix) {
			String sub = this.expr;
			for (Iterator i = this.aliases.keySet().iterator(); i.hasNext();) {
				Column col = (Column) i.next();
				String alias = ":" + (String) this.aliases.get(col);
				sub = sub.replaceAll(alias, tablePrefix + "." + col.getName());
			}
			return sub;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getAliases() {
			return this.aliases;
		}
	}
}
