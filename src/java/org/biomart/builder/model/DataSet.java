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
import java.util.HashMap;
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
import org.biomart.builder.resources.BuilderBundle;

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
 * @version 0.1.17, 19th May 2006
 * @since 0.1
 */
public class DataSet extends GenericSchema {
	private final Mart mart;

	private final Table centralTable;

	private final Set maskedRelations = new HashSet();

	private final Set maskedDataSetColumns = new HashSet();

	private final Map partitionedWrappedColumns = new HashMap();

	private final Set subclassedRelations = new HashSet();

	private final Map concatOnlyRelations = new HashMap();

	private MartConstructor martConstructor;

	private DataSetOptimiserType optimiser;

	private boolean partitionOnSchema;

	/**
	 * The constructor creates a dataset around one central table and gives the
	 * dataset a name. It adds itself to the specified mart automatically.
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
	 *             if another dataset with exactly the same name already exists
	 *             in the specified mart.
	 */
	public DataSet(Mart mart, Table centralTable, String name)
			throws AssociationException, AlreadyExistsException {
		// Super first, to set the name.
		super(name);

		// Sanity check to see if the table is in this mart or not.
		if (!mart.getSchemas().contains(centralTable.getSchema()))
			throw new AssociationException(BuilderBundle
					.getString("tableMartMismatch"));

		// Remember the settings and make some defaults.
		this.mart = mart;
		this.centralTable = centralTable;
		this.martConstructor = MartConstructor.DUMMY_MART_CONSTRUCTOR;
		this.optimiser = DataSetOptimiserType.NONE;
		this.partitionOnSchema = false;

		// Add ourselves to the mart.
		mart.addDataSet(this);
	}

	/**
	 * <p>
	 * This method removes all existing flags for masked and concat-only
	 * relations.
	 * <p>
	 * It then works out the 'real' central table - if the central table is a
	 * subclass of something else, then the something else is used as the
	 * central table for these purposes.
	 * <p>
	 * It then calls another method to walk through the relations and calculate
	 * how far they are from the central table, in terms of the number of 1:M
	 * relations traversed from the 1 end first. Those relations that are the
	 * second 1:M relation away are marked concat-only. Those that are the third
	 * or more are masked.
	 * <p>
	 * After all this, the dataset is regenerated so that it correctly reflects
	 * the optimised relations.
	 */
	public void optimiseDataSet() {
		// Clear out our previous predictions.
		this.maskedRelations.clear();
		this.concatOnlyRelations.clear();

		// Identify main table.
		Table centralTable = this.getCentralTable();
		// If central table has subclass relations and is at the foreign key
		// end, then follow them to the real central table.
		boolean found = false;
		for (Iterator i = centralTable.getRelations().iterator(); i.hasNext()
				&& !found;) {
			Relation r = (Relation) i.next();
			if (this.getSubclassedRelations().contains(r)
					&& r.getForeignKey().getTable().equals(centralTable)) {
				centralTable = r.getPrimaryKey().getTable();
				found = true;
			}
		}

		// Walk the relations and mark them as necessary.
		this.walkRelations(centralTable, 0, new TreeMap());

		// Regenerate the dataset.
		this.regenerate();
	}

	/**
	 * Internal method which works out the lowest number of 1:M relations
	 * between a given table and all other tables linked to it by
	 * as-yet-unvisited relations.
	 * 
	 * @param currentTable
	 *            the table to start walking from.
	 * @param pathLength
	 *            the number of 1:M relations it took by following from the 1
	 *            end to the M end to get this far.
	 */
	private void walkRelations(Table currentTable, int pathLength,
			Map relationsPredicted) {
		// Iterate through all the relations on the current table.
		for (Iterator i = currentTable.getRelations().iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();

			// Skip all incorrect relations.
			if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				continue;

			// Seen before?
			else if (relationsPredicted.containsKey(r)) {
				// Have we got there before by a path shorter than the current
				// path? If so, we can skip it.
				int previousShortestPathLength = ((Integer) relationsPredicted
						.get(r)).intValue();
				if (previousShortestPathLength <= pathLength)
					continue;
			}

			// If we get here, this is a new shortest path to the relation.

			// Update the path length on this relation.
			relationsPredicted.put(r, new Integer(pathLength));

			// Update the masked/unmasked flags.
			if (pathLength >= 2) {
				// Mask all relations that involve two or more levels of 1:m
				// abstraction away from the central main table.
				this.maskRelation(r);
			} else {
				// Unmask it. This is because the relation may have previously
				// been visited by a longer path, in which case it could have
				// been masked by that path when it should be unmasked for this
				// path.
				this.unmaskRelation(r);
			}

			// Work out where to go next.

			// If we're at the 1 end of a 1:M relation, increment the path
			// length
			// and follow it.
			if (r.getPrimaryKey().getTable().equals(currentTable)
					&& r.getFKCardinality().equals(Cardinality.MANY)) {

				// If we've already followed one 1:M relation to get here, then
				// this relation, also being a 1:M relation, should be flagged
				// as concat-only, but only if it is not a subclass relation.
				if (pathLength == 1
						&& !this.getSubclassedRelations().contains(r)) {
					this.flagConcatOnlyRelation(r, ConcatRelationType.COMMA);
				}

				// Recurse down to the foreign key end of the relation. Only
				// increment the path length if it is not a subclass relation.
				this.walkRelations(r.getForeignKey().getTable(), this
						.getSubclassedRelations().contains(r) ? pathLength
						: pathLength + 1, relationsPredicted);
			}

			// We're at the M end of a 1:M, or on a 1:1. So, recurse down the
			// appropriate end of the relation without incrementing the path
			// length.
			else {
				Key targetKey = r.getPrimaryKey().getTable().equals(
						currentTable) ? (Key) r.getForeignKey() : (Key) r
						.getPrimaryKey();
				this.walkRelations(targetKey.getTable(), pathLength,
						relationsPredicted);
			}
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
	 * Mask a relation.
	 * 
	 * @param relation
	 *            the relation to mask.
	 */
	public void maskRelation(Relation relation) {
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
			throw new AssociationException(BuilderBundle
					.getString("cannotMaskNecessaryColumn"));

		// Refuse to mask schema name columns.
		else if (dsColumn instanceof SchemaNameColumn)
			throw new AssociationException(BuilderBundle
					.getString("cannotMaskSchemaNameColumn"));

		// If concat-only, mask concat-only relation instead
		else if (dsColumn instanceof ConcatRelationColumn)
			this.maskRelation(dsColumn.getUnderlyingRelation());

		// If wrapped, mask wrapped column
		else if (dsColumn instanceof WrappedColumn) {
			WrappedColumn wcol = (WrappedColumn) dsColumn;
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
	 * <p>
	 * Mark a table as a subclass of another by marking the relation between
	 * them. The FK end of the relation is the subclass table, and the PK end is
	 * the parent table, but the parent table may not actually be the central
	 * table in this dataset.
	 * <p>
	 * As subclasses can only apply to the central table, throw an
	 * AssociationException if this is attempted on any table other than the
	 * central table.
	 * <p>
	 * One further restriction is that a table can only have a single M:1
	 * subclass relation, or multiple 1:M ones. It cannot have a mix of both,
	 * nor can it have more than one M:1 subclass relation. In either case if
	 * this is attempted an AssociationException will be thrown.
	 * 
	 * @param relation
	 *            the relation to mark as a subclass relation, where the PK end
	 *            is the parent table and the FK end is the subclass table.
	 * @throws AssociationException
	 *             if one end of the relation is not the central table for this
	 *             window, or if both ends of the relation point to the same
	 *             table, or if the rule about maximum numbers of subclass
	 *             relations of certain types applies (see above).
	 */
	public void flagSubclassRelation(Relation relation)
			throws AssociationException {
		// Check that the relation is a 1:M relation and that it is between
		// the central table and some other table.
		if (!(relation.getPrimaryKey().getTable().equals(this.centralTable) || relation
				.getForeignKey().getTable().equals(this.centralTable)))
			throw new AssociationException(BuilderBundle
					.getString("subclassNotOnCentralTable"));
		if (relation.getPrimaryKey().getTable().equals(
				relation.getForeignKey().getTable()))
			throw new AssociationException(BuilderBundle
					.getString("subclassNotBetweenTwoTables"));
		if (relation.getFKCardinality().equals(Cardinality.ONE))
			throw new AssociationException(BuilderBundle
					.getString("subclassNotOneMany"));

		// Check to see if there is already a M:1 subclass relation on the
		// central table.
		boolean containsM1 = false;
		for (Iterator i = this.subclassedRelations.iterator(); i.hasNext()
				&& !containsM1;) {
			Relation r = (Relation) i.next();
			if (r.getForeignKey().getTable().equals(this.centralTable))
				containsM1 = true;
		}

		// If an M:1 relation already exists, or any relation already exists and
		// this new relation is M:1, throw a wobbly.
		if (relation.getForeignKey().getTable().equals(this.centralTable)
				&& (containsM1 || this.subclassedRelations.size() != 0))
			throw new AssociationException(BuilderBundle
					.getString("mixedCardinalitySubclasses"));

		// Mark the relation.
		this.subclassedRelations.add(relation);
	}

	/**
	 * Unmark a relation as a subclass relation.
	 * 
	 * @param relation
	 *            the relation to unmark.
	 */
	public void unflagSubclassRelation(Relation relation) {
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
	 * Check to see if this dataset is set to partition by schema or not.
	 * 
	 * @return <tt>true</tt> if partition by schema is enabled, <tt>false</tt>
	 *         if not.
	 */
	public boolean getPartitionOnSchema() {
		return this.partitionOnSchema;
	}

	/**
	 * Sets the partition-on-schema flag for this dataset.
	 * 
	 * @param partitionOnSchema
	 *            <tt>true</tt> to enable partitioning by schema,
	 *            <tt>false</tt> to disable it.
	 */
	public void setPartitionOnSchema(boolean partitionOnSchema) {
		this.partitionOnSchema = partitionOnSchema;
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
	public void flagPartitionedWrappedColumn(WrappedColumn column,
			PartitionedColumnType type) throws AssociationException {
		// Check to see if we already have a partitioned column in this table,
		// that is not the same column as this one.
		boolean alreadyHave = false;
		for (Iterator i = this.partitionedWrappedColumns.keySet().iterator(); i
				.hasNext()
				&& !alreadyHave;) {
			WrappedColumn testCol = (WrappedColumn) i.next();
			if (testCol.getTable().equals(column.getTable())
					&& !testCol.equals(column))
				alreadyHave = true;
		}

		// If we do already have a partitioned column on this table, throw a
		// wobbly.
		if (alreadyHave)
			throw new AssociationException(BuilderBundle
					.getString("cannotPartitionMultiColumns"));

		// Partition the colum. If the column has already been partitioned, this
		// will override the type.
		this.partitionedWrappedColumns.put(column, type);
	}

	/**
	 * Unmark a column as partitioned.
	 * 
	 * @param column
	 *            the column to unmark.
	 */
	public void unflagPartitionedWrappedColumn(WrappedColumn column) {
		this.partitionedWrappedColumns.remove(column);
	}

	/**
	 * Return the set of partitioned columns. It may be empty, but never null.
	 * 
	 * @return the set of partitioned columns.
	 */
	public Collection getPartitionedWrappedColumns() {
		return this.partitionedWrappedColumns.keySet();
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
	public PartitionedColumnType getPartitionedWrappedColumnType(
			WrappedColumn column) {
		// Do we have partitioning on this column?
		if (!this.partitionedWrappedColumns.containsKey(column))
			return null;

		// Return the partitioning type.
		return (PartitionedColumnType) this.partitionedWrappedColumns
				.get(column);
	}

	/**
	 * Mark a relation as concat-only. If previously marked concat-only, this
	 * new call will override the previous request.
	 * 
	 * @param relation
	 *            the relation to mark as concat-only.
	 * @param type
	 *            the concat type to use for the relation.
	 */
	public void flagConcatOnlyRelation(Relation relation,
			ConcatRelationType type) {
		this.concatOnlyRelations.put(relation, type);
	}

	/**
	 * Unmark a relation as concat-only.
	 * 
	 * @param relation
	 *            the relation to unmark.
	 */
	public void unflagConcatOnlyRelation(Relation relation) {
		this.concatOnlyRelations.remove(relation);
	}

	/**
	 * Return the set of concat-only relations. It may be empty, but never null.
	 * 
	 * @return the set of concat-only relations.
	 */
	public Collection getConcatOnlyRelations() {
		return this.concatOnlyRelations.keySet();
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
		// Is this a concat-only relation?
		if (!this.concatOnlyRelations.containsKey(relation))
			return null;

		// Return the concat type.
		return (ConcatRelationType) this.concatOnlyRelations.get(relation);
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
		deadRelations.addAll(this.concatOnlyRelations.keySet());

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
			this.subclassedRelations.remove(r);
			this.concatOnlyRelations.remove(r);
		}

		// Regenerate the dataset
		this.regenerate();
	}

	/**
	 * <p>
	 * This method recreates all the tables in the dataset, using the relation
	 * flags as a guide as to how to treat each table it comes across in the
	 * schema.
	 * <p>
	 * Columns that were masked or partitioned are preserved only if after
	 * regeneration a column with the same name still exists in the same table.
	 */
	public void regenerate() {
		// Clear all our tables out as they will all be rebuilt.
		this.tables.clear();

		// Set up a map to contain relations which can be ignored during
		// the transformation process. Keys are tables, values are sets
		// of relations from those tables which can be ignored.
		Map ignoredRelations = new HashMap();

		// Set up a pair of sets to represent all subclass relations, and
		// all relations which represent the beginning of a dimension table.
		Set dimensionRelations = new HashSet();
		Set subclassRelations = new HashSet();

		// Identify main table.
		Table centralTable = this.getCentralTable();
		// If central table has subclass relations and is at the foreign key
		// end, then follow them to the real central table.
		boolean found = false;
		for (Iterator i = centralTable.getRelations().iterator(); i.hasNext()
				&& !found;) {
			Relation r = (Relation) i.next();
			if (this.getSubclassedRelations().contains(r)
					&& r.getForeignKey().getTable().equals(centralTable)) {
				centralTable = r.getPrimaryKey().getTable();
				found = true;
			}
		}

		// Start out by ignoring all masked relations on the central table.
		ignoredRelations.put(centralTable, new HashSet(this
				.getMaskedRelations()));

		// Work out the primary key on the central table.
		PrimaryKey pk = centralTable.getPrimaryKey();

		// If it has a primary key, and that key is OK, then loop over all the
		// relations leading from that key.
		if (pk != null
				&& !pk.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
			for (Iterator i = centralTable.getPrimaryKey().getRelations()
					.iterator(); i.hasNext();) {
				Relation r = (Relation) i.next();

				// Skip masked and concat-only relations.
				// Skip inferred-incorrect relations.
				// Skip incorrect-FK relations.
				if (this.getMaskedRelations().contains(r)
						|| this.getConcatOnlyRelations().contains(r)
						|| r.getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT)
						|| r.getForeignKey().getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT))
					continue;

				// Is this a subclass relation?
				if (this.getSubclassedRelations().contains(r)) {
					// Ignore the subclass relation at the main table.
					((Set) ignoredRelations.get(centralTable)).add(r);

					// Remember the relation as a subclass relation.
					subclassRelations.add(r);

					// Work out the parent of the subclass, and create a
					// new set of ignored relations for that table, containing
					// the set of masked relations.
					Table parentTable = r.getForeignKey().getTable();
					ignoredRelations.put(parentTable, new HashSet(this
							.getMaskedRelations()));

					// Work out the primary key on the parent table.
					PrimaryKey parentPK = parentTable.getPrimaryKey();

					// If it has a PK, and that PK is correct, iterate over the
					// relations on that PK.
					if (parentPK != null
							&& !parentPK.getStatus().equals(
									ComponentStatus.INFERRED_INCORRECT))
						for (Iterator j = parentTable.getPrimaryKey()
								.getRelations().iterator(); j.hasNext();) {
							Relation sr = (Relation) j.next();

							// Skip masked and concat-only relations.
							// Skip inferred-incorrect relations.
							// Skip incorrect-FK relations.
							if (this.getMaskedRelations().contains(sr)
									|| this.getConcatOnlyRelations().contains(
											sr)
									|| sr.getStatus().equals(
											ComponentStatus.INFERRED_INCORRECT)
									|| sr.getForeignKey().getStatus().equals(
											ComponentStatus.INFERRED_INCORRECT))
								continue;

							// If it is a 1:M relation with the 1 end at the
							// parent table, then set it up as a dimension, but
							// only if the relation is not ignored.
							if (sr.getFKCardinality().equals(Cardinality.MANY)) {

								// Ignore the relation from the parent table.
								((Set) ignoredRelations.get(parentTable))
										.add(sr);

								// Remember it as a dimension relation.
								dimensionRelations.add(sr);

								// Work out the target table for the dimension
								// and set up a set of ignored relations on it
								// containing all masked relations plus the
								// dimension relation.
								Table dimTable = sr.getForeignKey().getTable();
								if (!ignoredRelations.containsKey(dimTable))
									ignoredRelations.put(dimTable, new HashSet(
											this.getMaskedRelations()));
								((Set) ignoredRelations.get(dimTable)).add(sr);
							}
						}
				}

				// Is it a potential dimension relation, ie. 1:M with the
				// 1 end at the main table?
				else if (r.getFKCardinality().equals(Cardinality.MANY)) {

					// Ignore the relation from the central table.
					((Set) ignoredRelations.get(centralTable)).add(r);

					// Remember it as a dimension relation.
					dimensionRelations.add(r);

					// Work out the target table for the dimension
					// and set up a set of ignored relations on it
					// containing all masked relations plus the
					// dimension relation.
					Table dimTable = r.getForeignKey().getTable();
					if (!ignoredRelations.containsKey(dimTable))
						ignoredRelations.put(dimTable, new HashSet(this
								.getMaskedRelations()));
					((Set) ignoredRelations.get(dimTable)).add(r);
				}
			}

		// Subclass tables should ignore all subclass relations except
		// the one they depend on.
		for (Iterator i = subclassRelations.iterator(); i.hasNext();) {
			Relation subclassRel = (Relation) i.next();
			for (Iterator j = subclassRelations.iterator(); j.hasNext();) {
				Relation otherSubclassRel = (Relation) j.next();
				if (!subclassRel.equals(otherSubclassRel))
					((Set) ignoredRelations.get(subclassRel.getPrimaryKey()
							.getTable())).add(otherSubclassRel);
			}
		}

		// Build the main table.
		this.constructTable(DataSetTableType.MAIN, centralTable,
				(Set) ignoredRelations.get(centralTable), null);

		// Build the subclass tables.
		for (Iterator i = subclassRelations.iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();
			Table sct = r.getForeignKey().getTable();
			this.constructTable(DataSetTableType.MAIN_SUBCLASS, sct,
					(Set) ignoredRelations.get(sct), r);
		}

		// Build the dimension tables.
		for (Iterator i = dimensionRelations.iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();
			Table dt = r.getForeignKey().getTable();
			this.constructTable(DataSetTableType.DIMENSION, dt,
					(Set) ignoredRelations.get(dt), r);
		}

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
		for (Iterator i = this.partitionedWrappedColumns.keySet().iterator(); i
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
		for (Iterator i = this.partitionedWrappedColumns.keySet().iterator(); i
				.hasNext();)
			if (!newPartDSCols.contains(i.next()))
				i.remove();
	}

	/**
	 * Internal function that constructs a data set table based on a real table,
	 * and the relations linking to/from it. If a linkback relation is supplied
	 * between the real table and some parent real table, then a foreign key is
	 * created on the data set table linking it to the primary key of the
	 * dataset equivalent of the real parent table in the relation.
	 * 
	 * @param dsTableType
	 *            the dsTableType of table to create.
	 * @param realTable
	 *            the original table to transform.
	 * @param ignoredRelations
	 *            the set of relations to ignore during transformation. If null,
	 *            taken as empty.
	 * @param linkbackRelation
	 *            the relation between the realTable and the real parent table
	 *            to link to if required. Null indicates no linking required.
	 *            Linking takes place to the dataset version of the real parent
	 *            table.
	 * @return a fully constructed and keyed up data set table.
	 */
	private void constructTable(DataSetTableType dsTableType, Table realTable,
			Set ignoredRelations, Relation linkbackRelation) {
		// Prevents null-pointer exceptions in the case where there are no
		// relations to ignore.
		if (ignoredRelations == null)
			ignoredRelations = new HashSet();

		// Make a list to hold the list of relations followed when constructing
		// this table.
		List relationsFollowed = new ArrayList();

		// Make a list to hold the columns created on this table.
		List constructedColumns = new ArrayList();

		// Make a list to hold the primary key columns for this table.
		List constructedPKColumns = new ArrayList();

		// Make a list to hold the foreign key columns for this table.
		List constructedFKColumns = new ArrayList();

		// Create the empty DataSetTable into which columns will be added.
		// Give the table the same name as the real table upon which it
		// is based.
		DataSetTable datasetTable;
		try {
			datasetTable = new DataSetTable(realTable.getName(), this,
					dsTableType, realTable);
		} catch (Throwable t) {
			throw new MartBuilderInternalError(t);
		}

		// Add partition-on-schema schema name column if required. This
		// is only required when the real table is part of a schema group,
		// and happens regardless of whether partitioning by schema
		// group is enabled or not as it is vital to the uniqueness of
		// the ultimate primary key on this table.
		DataSetColumn schemaNameCol = null;
		if (realTable.getSchema() instanceof SchemaGroup) {
			try {
				schemaNameCol = new SchemaNameColumn(BuilderBundle
						.getString("schemaColumnName"), datasetTable);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}
		}

		// If there is a linkback relation, then that relation describes the
		// path between two real tables. The parent real table has some
		// other dataset table representing it, usually a main table or a
		// subclass table. The child real table is the one we are constructing a
		// dataset table for here. What we need to do is construct an FK on the
		// child dataset table which contains all the columns necessary to link
		// back
		// to the PK on the parent dataset table.
		if (linkbackRelation != null) {

			// Mark linkback relation as followed.
			relationsFollowed.add(linkbackRelation);

			// Get the parent real table.
			Table parentRealTable = linkbackRelation.getPrimaryKey().getTable();

			// Get the parent dataset table.
			DataSetTable parentDatasetTable = (DataSetTable) this
					.getTableByName(parentRealTable.getName());

			// Loop over the columns of the PK of the parent dataset table.
			for (Iterator i = parentDatasetTable.getPrimaryKey().getColumns()
					.iterator(); i.hasNext();) {
				DataSetColumn parentDatasetTableColumn = (DataSetColumn) i
						.next();
				DataSetColumn childDatasetTableColumn;

				// If the parent dataset column is a wrapped column, then
				// create a wrapped column on the child dataset table that
				// points to the same real table column. Use the linkback
				// relation to indicate that the column was obtained via
				// this method.
				if (parentDatasetTableColumn instanceof WrappedColumn) {
					// Find parent real table column.
					Column parentRealTableColumn = ((WrappedColumn) parentDatasetTableColumn)
							.getWrappedColumn();

					// Create the child dataset column.
					try {
						childDatasetTableColumn = new WrappedColumn(
								parentRealTableColumn, datasetTable,
								linkbackRelation);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}
				}

				// If the parent dataset column is a schema name column, we
				// just reuse the one we already created for the child dataset
				// table.
				else if (parentDatasetTableColumn instanceof SchemaNameColumn)
					childDatasetTableColumn = schemaNameCol;

				// We can't handle anything else.
				else
					throw new MartBuilderInternalError();

				// Add child dataset column to child dataset table FK.
				constructedFKColumns.add(childDatasetTableColumn);

				// Because the newly created column is part of the unique
				// PK columns on the parent dataset table, it must be
				// part of the child's too to prevent duplicate row problems.
				constructedPKColumns.add(childDatasetTableColumn);
			}

			// Create the foreign key on the child dataset table now that we
			// have all its columns.
			try {
				// Make the key.
				ForeignKey newFK = new GenericForeignKey(constructedFKColumns);

				// Add it to the child dataset table.
				datasetTable.addForeignKey(newFK);

				// Create the relation between the parent and child
				// dataset tables.
				new GenericRelation(parentDatasetTable.getPrimaryKey(), newFK,
						Cardinality.MANY);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}

			// Remember that we've made them.
			constructedColumns.addAll(constructedFKColumns);
		}

		// Add the rest of the columns by running the table transformation.
		// This transformation will populate the list of relations followed,
		// and potentially expand the list of PK columns too.
		this.transformTable(datasetTable, realTable, ignoredRelations,
				relationsFollowed, constructedPKColumns, constructedColumns);

		// Set the underlying relations list on the child dataset table.
		datasetTable.setUnderlyingRelations(relationsFollowed);

		// After we've gathered all our colums together, and duplicate names
		// have been ironed out, rename our foreign key columns with '_key'
		// suffixes.
		for (Iterator i = constructedFKColumns.iterator(); i.hasNext();) {
			DataSetColumn col = (DataSetColumn) i.next();
			if (!col.getName().endsWith(BuilderBundle.getString("fkSuffix")))
				try {
					col.setName(col.getName()
							+ BuilderBundle.getString("fkSuffix"));
				} catch (Throwable t) {
					throw new MartBuilderInternalError(t);
				}
		}

		// Add schema name column to PK, if not already there. However, this
		// should only be done if there is already a PK, because if it doesn't
		// already exist, this would create ridiculous constraints.
		if (schemaNameCol != null && !constructedPKColumns.isEmpty()
				&& !constructedPKColumns.contains(schemaNameCol))
			constructedPKColumns.add(schemaNameCol);

		// Create the primary key.
		if (!constructedPKColumns.isEmpty())
			try {
				datasetTable.setPrimaryKey(new GenericPrimaryKey(
						constructedPKColumns));
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}
	}

	/**
	 * <p>
	 * Simple recursive internal function that takes a real table, and adds all
	 * its columns to some dataset table. It then takes all the relations from
	 * that table, and recurses down them to do the same on each target table.
	 * <p>
	 * Masked relations are ignored, as are those that are marked as
	 * {@link ComponentStatus#INFERRED_INCORRECT}.
	 * <p>
	 * Along the way, it constructs a list of columns that are required to be in
	 * the eventual dataset table's primary key.
	 * 
	 * @param datasetTable
	 *            the table to construct as we go along.
	 * @param realTable
	 *            the table we are adding columns to the dataset table from
	 *            next.
	 * @param ignoredRelations
	 *            relations to ignore.
	 * @param relationsFollowed
	 *            the relations used as we went along.
	 * @param constructedPKColumns
	 *            the primary key columns to be added to the new dataset table.
	 * @param constructedColumns
	 *            the columns to be added to the new dataset table.
	 */
	private void transformTable(DataSetTable datasetTable, Table realTable,
			Set ignoredRelations, List relationsFollowed,
			List constructedPKColumns, List constructedColumns) {
		// Set up a list to hold columns which should not be added to this
		// dataset table.
		List excludedColumns = new ArrayList();

		// Add all columns from good foreign keys to the list of columns to
		// be excluded. This is because these columns will be added from the
		// primary key end of the relation pointing to the foreign key instead.
		// Good foreign keys are those that are not incorrect, and have at least
		// one non-incorrect non-masked non-concat-only relation.
		for (Iterator i = realTable.getForeignKeys().iterator(); i.hasNext();) {
			Key k = (Key) i.next();

			// Skip bad keys.
			if (k.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				continue;

			// See if we have a good relation.
			boolean hasGoodRelation = false;
			for (Iterator j = k.getRelations().iterator(); j.hasNext()
					&& !hasGoodRelation;) {
				Relation r = (Relation) j.next();
				hasGoodRelation = (!r.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT)
						&& !this.getMaskedRelations().contains(r) && !this
						.getConcatOnlyRelations().contains(r));
			}

			// If we have a good relation, skip all columns from this key.
			if (hasGoodRelation)
				excludedColumns.addAll(k.getColumns());
		}

		// Also exclude all masked columns.
		excludedColumns.addAll(this.getMaskedDataSetColumns());

		// Work out the last relation visited. This relation is the one
		// that was followed to reach this table, and will be used
		// as the underlying relation for all columns added from this table.
		Relation underlyingRelation = null;
		if (!relationsFollowed.isEmpty())
			underlyingRelation = (Relation) relationsFollowed
					.get(relationsFollowed.size() - 1);

		// Add all non-excluded columns from real table to dataset table
		// by wrapping them up.
		for (Iterator i = realTable.getColumns().iterator(); i.hasNext();) {
			Column c = (Column) i.next();

			// If column is excluded, ignore it.
			if (excludedColumns.contains(c))
				continue;

			// Otherwise add the column to our table.
			Column wc;
			try {
				wc = new WrappedColumn(c, datasetTable, underlyingRelation);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}

			// If column is part of the primary key on the real table, then
			// add the wrapped version to the primary key column set on the
			// dataset table. Only do this if the real table primary key is
			// not incorrect.
			PrimaryKey pk = realTable.getPrimaryKey();
			if (pk != null
					&& !pk.getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT)
					&& pk.getColumns().contains(c))
				constructedPKColumns.add(wc);
		}

		// Loop over the relations leading from the real table.
		for (Iterator i = realTable.getRelations().iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();

			// Skip all ignored relations.
			// Ignore incorrect relations too.
			// Ignore relations with incorrect keys at either end.
			if (ignoredRelations.contains(r)
					|| r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
					|| r.getPrimaryKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT)
					|| r.getForeignKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
				continue;

			// Add relation to the path of relations followed so far.
			relationsFollowed.add(r);

			// Add the relation to the ignore set so that we don't
			// revisit it later.
			ignoredRelations.add(r);

			// Find which end of the relation links to the current table,
			// and which table the other end links to.
			Key realTableRelSourceKey = r.getPrimaryKey();
			Table realTableRelTargetTable = r.getForeignKey().getTable();
			if (!realTableRelSourceKey.getTable().equals(realTable)) {
				realTableRelSourceKey = r.getForeignKey();
				realTableRelTargetTable = r.getPrimaryKey().getTable();
			}

			// If the primary key end of the relation links to the
			// current table, and the relation is a concat-only relation,
			// then create a concat-only-relation column on the dataset table.
			if (realTableRelSourceKey instanceof PrimaryKey
					&& this.getConcatOnlyRelations().contains(r))
				try {
					new ConcatRelationColumn(BuilderBundle
							.getString("concatColumnPrefix")
							+ realTableRelTargetTable.getName(), datasetTable,
							r);
				} catch (Throwable t) {
					throw new MartBuilderInternalError(t);
				}

			// Otherwise, recurse down to the target table and continue
			// from there.
			else {
				this.transformTable(datasetTable, realTableRelTargetTable,
						ignoredRelations, relationsFollowed,
						constructedPKColumns, constructedColumns);
			}
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
	 * Sets the constructor to use to build the final mart with.
	 * 
	 * @param martConstructor
	 *            the constructor to use.
	 */
	public void setMartConstructor(MartConstructor martConstructor) {
		// Do it.
		this.martConstructor = martConstructor;
	}

	/**
	 * Returns the constructor that will be used to build the final mart with.
	 * 
	 * @return the constructor that will be used.
	 */
	public MartConstructor getMartConstructor() {
		return this.martConstructor;
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

	/**
	 * Uses the mart constructor to construct the final mart.
	 * 
	 * @throws BuilderException
	 *             if there was a logical problem constructing the mart.
	 * @throws SQLException
	 *             if there was a problem connecting to the database within
	 *             which the mart will be constructed.
	 */
	public void constructMart() throws BuilderException, SQLException {
		this.martConstructor.constructMart(this);
	}

	public void addTable(Table table) throws AlreadyExistsException,
			AssociationException {
		// We only accept dataset tables.
		if (!(table instanceof DataSetTable))
			throw new AssociationException(BuilderBundle
					.getString("tableNotDSTable"));

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

		private final String separator;

		/**
		 * Use this constant to refer to value-separation by commas.
		 */
		public static final ConcatRelationType COMMA = new ConcatRelationType(
				"COMMA", ",");

		/**
		 * Use this constant to refer to value-separation by spaces.
		 */
		public static final ConcatRelationType SPACE = new ConcatRelationType(
				"SPACE", " ");

		/**
		 * Use this constant to refer to value-separation by tabs.
		 */
		public static final ConcatRelationType TAB = new ConcatRelationType(
				"TAB", "\t");

		/**
		 * The private constructor takes two parameters, which define the name
		 * this concat type object will display when printed, and the separator
		 * to use between values that have been concatenated.
		 * 
		 * @param name
		 *            the name of the concat type.
		 * @param separator
		 *            the separator for this concat type.
		 */
		private ConcatRelationType(String name, String separator) {
			this.name = name;
			this.separator = separator;
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
		 * Displays the separator for this concat type object.
		 * 
		 * @return the separator for this concat type object.
		 */
		public String getSeparator() {
			return this.separator;
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
	 * following a series of relations. As such it has no real columns of its
	 * own, so every column is from another table and is given an alias.
	 */
	public static class DataSetTable extends GenericTable {
		private final List underlyingRelations = new ArrayList();

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
		 * @throws AlreadyExistsException
		 *             this should never actually happen, but is required as the
		 *             super constructor throws it.
		 */
		public DataSetTable(String name, DataSet ds, DataSetTableType type,
				Table underlyingTable) throws AlreadyExistsException {
			// Super constructor first, using an alias to prevent duplicates.
			super(generateAlias(name, ds), ds);

			// Remember the other settings.
			this.underlyingTable = underlyingTable;
			this.type = type;
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
		 * Returns the list of relations used to construct this table.
		 * 
		 * @return the list of relations of this table. May be empty but never
		 *         null.
		 */
		public List getUnderlyingRelations() {
			return this.underlyingRelations;
		}

		public void addColumn(Column column) throws AlreadyExistsException,
				AssociationException {
			// We only accept dataset columns.
			if (!(column instanceof DataSetColumn))
				throw new AssociationException(BuilderBundle
						.getString("columnNotDatasetColumn"));

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
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This implementation also calls the appropriate renaming functions in
		 * the table as well, saving the caller the effort of doing so.
		 */
		public void setName(String newName) {
			// Sanity check.
			if (newName.equals(this.getName()))
				return; // Skip unnecessary change.
			// Do it.
			String oldName = this.getName();
			super.setName(newName);
			try {
				this.getTable().changeColumnMapKey(oldName, newName);
			} catch (Throwable t) {
				throw new MartBuilderInternalError(t);
			}
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
					throw new AssociationException(BuilderBundle
							.getString("relationNotConcatRelation"));
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
		 * Use this constant to refer to optimising by running a left join on
		 * each dimension..
		 */
		public static final DataSetOptimiserType LEFTJOIN = new DataSetOptimiserType(
				"LEFTJOIN");

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
}
