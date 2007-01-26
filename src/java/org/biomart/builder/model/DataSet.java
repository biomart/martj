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
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.TransformationUnit.LeftJoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.Column;
import org.biomart.common.model.Column.GenericColumn;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Table.GenericTable;
import org.biomart.common.model.Key.ForeignKey;
import org.biomart.common.model.Key.GenericForeignKey;
import org.biomart.common.model.Key.GenericPrimaryKey;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.model.Relation.Cardinality;
import org.biomart.common.model.Relation.GenericRelation;
import org.biomart.common.model.Schema.GenericSchema;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * A {@link DataSet} instance serves two purposes. First, it contains lists of
 * settings that are specific to this dataset and affect the way in which tables
 * and relations in the schemas it draws data from behave. Secondly, it is a
 * {@link Schema} itself, containing definitions of all the tables in the
 * dataset it represents and how they relate to each other.
 * <p>
 * The settings that customise the way in which schemas it uses behave include
 * masking of unwanted relations and columns, and flagging of relations as
 * concat-only or subclassed. These settings are specific to this dataset and do
 * not affect other datasets.
 * <p>
 * The central table of the dataset is a reference to a real table, from which
 * the main table of the dataset will be derived and all other transformations
 * in the dataset to produce dimensions and subclasses will begin.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class DataSet extends GenericSchema {
	private final Table centralTable;

	private boolean invisible;

	private final Mart mart;
	
	private final Collection includedRelations;

	// TODO SchemaModMaps for source schema changes.
	// Restricted tables.
	// Replicated relations - CREATE COMPOUND RELATION.
	// Restricted relations - INDEX INTO COMPOUND RELATION.
	// Concat-only relations with concat definitions incl. 
	//   expressions inside concat definition (DST only).
	//   INDEX INTO COMPOUND RELATION.
	//   ADDS A CONCAT TUNIT TO THE CHAIN WITH ONE CONCAT DS COLUMN
	//   IN IT - COLUMN HAS DEFAULT NAME AND CAN BE RENAMED BY
	//   USER AS PER ANY OTHER COLUMN - need synch() after this.
	// Recursion.
	private final SchemaModificationSet schemaMods;

	// TODO DataSetModMaps for renames/masking.
	// Partition columns.
	// Expression column definitions with nesting resolution.
	//   ADDS AN EXPRESSION TUNIT(S) TO THE CHAIN WITH EACH NESTED
	//   EXPRESSION CLUSTER OF DS COLUMNS - COLUMNS HAVE DEFAULT NAME
	//   AND CAN BE RENAMED BY USER - need synch() after this.
	private final DataSetModificationSet dsMods;

	private DataSetOptimiserType optimiser;

	/**
	 * The constructor creates a dataset around one central table and gives the
	 * dataset a name. It adds itself to the specified mart automatically.
	 * <p>
	 * If the name already exists, an underscore and a sequence number will be
	 * appended until the name is unique, as per the constructor in
	 * {@link GenericSchema}, which it inherits from.
	 * 
	 * @param mart
	 *            the mart this dataset will belong to.
	 * @param centralTable
	 *            the table to use as the central table for this dataset.
	 * @param name
	 *            the name to give this dataset.
	 */
	public DataSet(final Mart mart, final Table centralTable, final String name) {
		// Super first, to set the name.
		super(name);

		Log.info(Resources.get("logNewDataset", name));

		// Remember the settings and make some defaults.
		this.invisible = false;
		this.mart = mart;
		this.centralTable = centralTable;
		this.optimiser = DataSetOptimiserType.NONE;
		this.schemaMods = new SchemaModificationSet(this);
		this.dsMods = new DataSetModificationSet();
		this.includedRelations = new HashSet();
	}
	
	/**
	 * Obtain all relations used by this dataset.
	 * @return all relations.
	 */
	public Collection getIncludedRelations() {
		return this.includedRelations;
	}
	
	/**
	 * Obtain the set of schema modifiers for this dataset.
	 * @return the set of modifiers.
	 */
	public SchemaModificationSet getSchemaModifications() {
		return this.schemaMods;
	}
	
	/**
	 * Obtain the set of dataset modifiers for this dataset.
	 * @return the set of modifiers.
	 */
	public DataSetModificationSet getDataSetModifications() {
		return this.dsMods;
	}

	/**
	 * This internal method builds a dataset table based around a real table. It
	 * works out what dimensions and subclasses are required then recurses to
	 * create those too.
	 * 
	 * @param type
	 *            the type of table to build.
	 * @param parentDSTable
	 *            the table which this dataset table creates a foreign key to.
	 *            If this is to be a subclass table, it will inherit all columns
	 *            from this parent table.
	 * @param realTable
	 *            the real table in a schema from where the transformation to
	 *            create this dataset table will begin.
	 * @param sourceRelation
	 *            the real relation in a schema which was followed in order to
	 *            discover that this dataset table should be created. For
	 *            instance, it could be the 1:M relation between the realTable
	 *            parameter of this call, and the realTable parameter of the
	 *            main table call to this method.
	 */
	private void generateDataSetTable(final DataSetTableType type,
			final DataSetTable parentDSTable, final Table realTable,
			final List sourceDSCols, final Relation sourceRelation) {
		Log.debug("Creating dataset table for " + realTable
				+ " with parent relation " + sourceRelation + " as a " + type);
		// Create the empty dataset table.
		final DataSetTable dsTable = new DataSetTable(realTable.getName(),
				this, type, realTable, sourceRelation);
		this.addTable(dsTable);

		// Create the three relation-table pair queues we will work with. The
		// normal queue holds pairs of relations and tables. The other two hold
		// a list of relations only, the tables being the FK ends of each
		// relation. The normal queue has a third object associated with each
		// entry, which specifies whether to treat the 1:M relations from
		// the merged table as dimensions or not.
		final List normalQ = new ArrayList();
		final List subclassQ = new ArrayList();
		final List dimensionQ = new ArrayList();

		// Set up a list to hold columns for this table's primary key.
		final List dsTablePKCols = new ArrayList();

		// If the parent dataset table is not null, add columns from it
		// as appropriate. Dimension tables get just the PK, and an
		// FK linking them back. Subclass tables get all columns, plus
		// the PK with FK link, plus all the relations we followed to
		// get these columns.
		if (parentDSTable != null) {
			final TransformationUnit tu = new SelectFromTable(
					parentDSTable);
			dsTable.addTransformationUnit(tu);

			// Make a list to hold the child table's FK cols.
			final List dsTableFKCols = new ArrayList();

			// Get the primary key of the parent DS table.
			final PrimaryKey parentDSTablePK = parentDSTable.getPrimaryKey();

			// Loop over each column in the parent table. If this is
			// a subclass table, add it. If it is a dimension table,
			// only add it if it is in the PK or is in the first underlying
			// key. In either case, if it is in the PK, add it both to the
			// child PK and the child FK.
			for (final Iterator i = parentDSTable.getColumns().iterator(); i
					.hasNext();) {
				final DataSetColumn parentDSCol = (DataSetColumn) i.next();
				// If this is not a subclass table, we need to filter columns.
				if (!type.equals(DataSetTableType.MAIN_SUBCLASS)) {
					// Skip columns that are not in the primary key.
					boolean inPK = parentDSTablePK.getColumns().contains(
							parentDSCol);
					// Skip columns that are not in the source key.
					boolean inSourceKey = parentDSCol instanceof WrappedColumn
							&& (sourceRelation.getFirstKey().getColumns()
									.contains(
											((WrappedColumn) parentDSCol)
													.getWrappedColumn()) || sourceRelation
									.getSecondKey().getColumns().contains(
											((WrappedColumn) parentDSCol)
													.getWrappedColumn()));
					if (!inPK && !inSourceKey)
						continue;
				}
				// If column is masked, don't inherit it.
				if (this.dsMods.isMaskedColumn(parentDSCol)
						|| this.dsMods.isNonInheritedColumn(parentDSCol))
					continue;
				// Only unfiltered columns reach this point. Create a copy of
				// the column.
				InheritedColumn dsCol = new InheritedColumn(dsTable,
						parentDSCol);
				dsTable.addColumn(dsCol);
				tu.getNewColumnNameMap().put(parentDSCol.getName(), dsCol);
				// Add the column to the child's FK, but only if it was in
				// the parent PK.
				if (parentDSTablePK.getColumns().contains(parentDSCol))
					dsTableFKCols.add(dsCol);
			}

			try {
				// Create the child FK.
				final ForeignKey dsTableFK = new GenericForeignKey(
						dsTableFKCols);
				dsTable.addForeignKey(dsTableFK);
				// Link the child FK to the parent PK.
				final Relation rel = new GenericRelation(parentDSTablePK,
						dsTableFK, Cardinality.MANY);
				parentDSTablePK.addRelation(rel);
				dsTableFK.addRelation(rel);
			} catch (final Throwable t) {
				throw new BioMartError(t);
			}
		}

		// How many times are allowed to iterate over each relation?
		final Map relationCount = new HashMap();
		final Set allRelations = new HashSet();
		for (final Iterator i = this.getMart().getSchemas().iterator(); i.hasNext(); ) 
			allRelations.addAll(((Schema)i.next()).getRelations());
		for (final Iterator i = allRelations.iterator(); i.hasNext();) {
			final Relation rel = (Relation)i.next();
			final int compounded = 
				this.schemaMods.isCompoundRelation(dsTable, rel)
				? this.schemaMods.getCompoundRelation(dsTable, rel)
						: 1;
			relationCount.put(rel, new Integer(compounded));		
		}
		
		// Don't follow the source relation again.
		relationCount.put(sourceRelation, new Integer(0));
		// Don't follow the parent's relations again, if this is
		// a subclass table. Otherwise, follow everything as normal.
		if (type.equals(DataSetTableType.MAIN_SUBCLASS))
			for (final Iterator i = parentDSTable.getTransformationUnits()
					.iterator(); i.hasNext();) {
				final TransformationUnit u = (TransformationUnit) i.next();
				if (u instanceof LeftJoinTable)
					relationCount.put(((LeftJoinTable) u)
							.getSchemaRelation(), new Integer(0));
			}
		
		// Process the table. This operation will populate the initial
		// values in the normal, subclass and dimension queues. We only
		// want dimensions constructed if we are not already constructing
		// a dimension ourselves.
		this.processTable(null, dsTable, dsTablePKCols, realTable, normalQ,
				subclassQ, dimensionQ, sourceDSCols, sourceRelation,
				relationCount, !type.equals(DataSetTableType.DIMENSION));

		// Process the normal queue. This merges tables into the dataset
		// table using the relation specified in each pair in the queue.
		// The third value is the dataset parent table columns to link from.
		// The fourth value of each entry in the queue determines whether or
		// not to continue making dimensions off each table in the queue.
		for (int i = 0; i < normalQ.size(); i++) {
			final Object[] triple = (Object[]) normalQ.get(i);
			final Relation mergeSourceRelation = (Relation) triple[0];
			final List newSourceDSCols = (List) triple[1];
			final Table mergeTable = (Table) triple[2];
			final TransformationUnit previousUnit = (TransformationUnit)triple[3];
			final boolean makeDimensions = ((Boolean) triple[4]).booleanValue();
			this.processTable(previousUnit, dsTable, dsTablePKCols, mergeTable, normalQ,
				subclassQ, dimensionQ, newSourceDSCols,
				mergeSourceRelation, relationCount, makeDimensions);
		}

		// Create the primary key on this table, but only if it has one.
		// Don't bother for dimensions.
		if (!dsTablePKCols.isEmpty()
				&& !dsTable.getType().equals(DataSetTableType.DIMENSION)) 
			// Create the key.
			dsTable.setPrimaryKey(new GenericPrimaryKey(dsTablePKCols));

		// Process the subclass relations of this table.
		for (int i = 0; i < subclassQ.size(); i++) {
			final Object[] triple = (Object[]) subclassQ.get(i);
			final List newSourceDSCols = (List) triple[0];
			final Relation subclassRelation = (Relation) triple[1];
			this.generateDataSetTable(DataSetTableType.MAIN_SUBCLASS, dsTable,
					subclassRelation.getManyKey().getTable(), newSourceDSCols,
					subclassRelation);
		}

		// Process the dimension relations of this table. For 1:M it's easy.
		// For M:M, we have to work out which end is connected to the real
		// table, then process the table at the other end of the relation.
		for (int i = 0; i < dimensionQ.size(); i++) {
			final Object[] triple = (Object[]) dimensionQ.get(i);
			final List newSourceDSCols = (List) triple[0];
			final Relation dimensionRelation = (Relation) triple[1];
			if (dimensionRelation.isOneToMany())
				this.generateDataSetTable(DataSetTableType.DIMENSION, dsTable,
						dimensionRelation.getManyKey().getTable(),
						newSourceDSCols, dimensionRelation);
			else
				this
						.generateDataSetTable(DataSetTableType.DIMENSION,
								dsTable,
								dimensionRelation.getFirstKey().getTable()
										.equals(realTable) ? dimensionRelation
										.getSecondKey().getTable()
										: dimensionRelation.getFirstKey()
												.getTable(), newSourceDSCols,
								dimensionRelation);
		}
	}

	/**
	 * This method takes a real table and merges it into a dataset table. It
	 * does this by creating {@link WrappedColumn} instances for each new column
	 * it finds in the table.
	 * <p>
	 * If a source relation was specified, columns in the key in the table that
	 * is part of that source relation are ignored, else they'll get duplicated.
	 * 
	 * @param dsTable
	 *            the dataset table we are constructing and should merge the
	 *            columns into.
	 * @param dsTablePKCols
	 *            the primary key columns of that table. If we find we need to
	 *            add to these, we should add to this list directly.
	 * @param mergeTable
	 *            the real table we are about to merge columns from.
	 * @param normalQ
	 *            the queue to add further real tables into that we find need
	 *            merging into this same dataset table.
	 * @param subclassQ
	 *            the queue to add starting points for subclass tables that we
	 *            find.
	 * @param dimensionQ
	 *            the queue to add starting points for dimension tables we find.
	 * @param sourceRelation
	 *            the real relation we followed to reach this table.
	 * @param relationCount
	 *            how many times we have left to follow each relation, so that 
	 *            we don't follow them too often.
	 * @param makeDimensions
	 *            <tt>true</tt> if we should add potential dimension tables to
	 *            the dimension queue, <tt>false</tt> if we should just ignore
	 *            them. This is useful for preventing dimensions from gaining
	 *            dimensions of their own.
	 */
	private void processTable(final TransformationUnit previousUnit, final DataSetTable dsTable,
			final List dsTablePKCols, final Table mergeTable,
			final List normalQ, final List subclassQ, final List dimensionQ,
			final List sourceDataSetCols, final Relation sourceRelation,
			final Map relationCount, final boolean makeDimensions) {
		Log.debug("Processing table " + mergeTable);

		// Don't ignore any keys by default.
		Key ignoreKey = null;

		// By default we didn't land on any key to get here.
		Key mergeKey = null;

		final TransformationUnit tu;

		// Did we get here via somewhere else?
		if (sourceRelation != null) {
			// Work out what key to ignore by working out at which end
			// of the relation we are.
			ignoreKey = sourceRelation.getFirstKey().getTable().equals(
					mergeTable) ? sourceRelation.getFirstKey() : sourceRelation
					.getSecondKey();
			mergeKey = sourceRelation.getOtherKey(ignoreKey);

			// Add the relation and key to the list that the table depends on.
			// This list is what defines the path required to construct
			// the DDL for this table.
			// TODO Don't bother if this is a concat relation. Just add
			// a concat unit instead.
			tu = new LeftJoinTable(previousUnit, mergeTable, sourceDataSetCols,
					mergeKey, sourceRelation);

			// Remember we've been here.
			this.includedRelations.add(sourceRelation);
		} else {
			tu = new SelectFromTable(mergeTable);
		}
		dsTable.addTransformationUnit(tu);

		// Work out the merge table's PK.
		final PrimaryKey mergeTablePK = mergeTable.getPrimaryKey();

		// We must merge only the first PK we come across, if this is
		// a main table, or the first PK we come across after the
		// inherited PK, if this is any other kind of table.
		boolean includeMergeTablePK = mergeTablePK != null;
		if (includeMergeTablePK && sourceRelation != null)
			// Only add further PK columns if the relation did NOT
			// involve our PK and was NOT 1:1.
			includeMergeTablePK = !sourceRelation.isOneToOne()
					&& !sourceRelation.getFirstKey().equals(mergeTablePK)
					&& !sourceRelation.getSecondKey().equals(mergeTablePK)
					&& dsTablePKCols.isEmpty();

		// Add all columns from merge table to dataset table, except those in
		// the ignore key.
		// TODO Don't bother if this is a concat relation. Just add
		// a concat column instead.
		for (final Iterator i = mergeTable.getColumns().iterator(); i.hasNext();) {
			final Column c = (Column) i.next();

			// Ignore those in the key used to get here.
			if (ignoreKey != null && ignoreKey.getColumns().contains(c))
				continue;

			// Create a wrapped column for this column.
			String colName = c.getName();
			// Rename all PK columns to have the '_key' suffix.
			if (includeMergeTablePK && mergeTablePK.getColumns().contains(c)
					&& !colName.endsWith(Resources.get("keySuffix")))
						colName = colName + Resources.get("keySuffix");
			final WrappedColumn wc = new WrappedColumn(c, colName, dsTable,
					sourceRelation);
			tu.getNewColumnNameMap().put(c.getName(), wc);
			dsTable.addColumn(wc);

			// If the column is in any key on this table then it is a
			// dependency for possible future linking, which must be flagged.
			for (final Iterator j = mergeTable.getKeys().iterator(); j
					.hasNext();)
				if (((Key) j.next()).getColumns().contains(c))
					wc.setDependency(true);

			// If the column was in the merge table's PK, and we are
			// expecting to add the PK to the generated table's PK, then
			// add it to the generated table's PK.
			if (includeMergeTablePK && mergeTablePK.getColumns().contains(c))
				dsTablePKCols.add(wc);
		}

		// Update the three queues with relations that lead away from this
		// table.
		for (final Iterator i = mergeTable.getRelations().iterator(); i
				.hasNext();) {
			final Relation r = (Relation) i.next();

			// Don't repeat relations.
			// Note that it won't loop back up to parent, because 
			// to do that would mean having two 1:1 relations,
			// or a 1:M parent:child and separate M:1 child:parent, which 
			// would be silly.
			if (((Integer)relationCount.get(r)).intValue()<=0
					|| r.equals(sourceRelation))
				continue;

			// Don't follow masked relations.
			if (this.schemaMods.isMaskedRelation(dsTable, r))
				continue;
			
			// Don't follow incorrect relations, or relations
			// between incorrect keys.
			if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
					|| r.getFirstKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT)
					|| r.getSecondKey().getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT)) {
				continue;
			}
			
			// Decrement the relation counter.
			relationCount.put(r, new Integer(((Integer)relationCount.get(r)).intValue() - 1));
			
			// Set up a holder to indicate whether or not to follow
			// the relation.
			boolean followRelation = false;
			boolean forceFollowRelation = false;

			// Are we at the 1 end of a 1:M, or at either end of a M:M?
			// If so, we may need to make a dimension, a subclass, or
			// a concat column.
			if (r.isManyToMany() || r.isOneToMany()
					&& r.getOneKey().getTable().equals(mergeTable)) {
				// Subclass subclassed relations, if we are currently
				// not building a dimension table.
				if (this.schemaMods.isSubclassedRelation(r)
						&& !dsTable.getType()
								.equals(DataSetTableType.DIMENSION)) {
					final Key sourceKey = r.getFirstKey().getTable().equals(
							mergeTable) ? r.getFirstKey() : r.getSecondKey();
					final List newSourceDSCols = new ArrayList();
					for (final Iterator j = sourceKey.getColumnNames()
							.iterator(); j.hasNext();)
						newSourceDSCols.add(tu.getDataSetColumnFor((String)
								j.next()));
					subclassQ.add(new Object[] { newSourceDSCols, r });
				}

				// Dimensionize dimension relations, which are all other 1:M
				// or M:M relations, if we are not constructing a dimension
				// table, and are currently intending to construct dimensions.
				else if (makeDimensions
						&& !dsTable.getType()
								.equals(DataSetTableType.DIMENSION)) {
					final Key sourceKey = r.getFirstKey().getTable().equals(
							mergeTable) ? r.getFirstKey() : r.getSecondKey();
					final List newSourceDSCols = new ArrayList();
					for (final Iterator j = sourceKey.getColumnNames()
							.iterator(); j.hasNext();)
						newSourceDSCols.add(tu.getDataSetColumnFor((String)
								j.next()));
					dimensionQ.add(new Object[] { newSourceDSCols, r });
					if (this.schemaMods.isMergedRelation(r))
						forceFollowRelation = true;
				}
				
				// TODO Forcibly follow concat relations.
				//else if (this.schemaMods.isForceIncludeRelation(dsTable, r)) 
				//	forceFollowRelation = true;
				
				// Forcibly follow forced relations.
				else if (this.schemaMods.isForceIncludeRelation(dsTable, r)) 
					forceFollowRelation = true;
			}

			// We're at the M end of a 1:M. Don't follow it if it has
			// multiple relations, as it is likely to be an artifact
			// of a non-normalised schema (e.g. Ensembl) and is best
			// included from the opposite direction, if at all.
			else if (r.isOneToMany()
					&& r.getManyKey().getTable().equals(mergeTable)
					&& r.getManyKey().getRelations().size() > 1) {
				// Do nothing.
			}
			
			// Follow all others.
			else 
				followRelation = true;

			// If we follow a 1:1, and we are currently
			// including dimensions, include them from the 1:1 as well.
			// Otherwise, stop including dimensions on subsequent tables.
			if (followRelation || forceFollowRelation) {
				this.includedRelations.add(r);
				dsTable.includedRelations.add(r);
				final Key targetKey = r.getFirstKey().getTable().equals(
						mergeTable) ? r.getSecondKey() : r.getFirstKey();
				final Key sourceKey = r.getOtherKey(targetKey);
				final List newSourceDSCols = new ArrayList();
				for (final Iterator j = sourceKey
						.getColumnNames().iterator(); j.hasNext();) 
					newSourceDSCols.add(tu.getDataSetColumnFor((String)j.next()));
				// Repeat queueing of relation N times if compounded.
				// Note that we only spin off one child per parent if
				// the parent was compounded. If the child compound value
				// is less than the parent, this results in only some
				// nested compounding. If it is greater, only the parent
				// value number of compounds appears.
				final int parentCompounded = sourceRelation==null ? 1 : (this.schemaMods.isCompoundRelation(dsTable, sourceRelation)
				? this.schemaMods.getCompoundRelation(dsTable, sourceRelation)
						: 1);
				final int childCompounded = parentCompounded>1 ? 1 :
					(this.schemaMods.isCompoundRelation(dsTable, r)
					? this.schemaMods.getCompoundRelation(dsTable, r)
							: 1);
				for (int k = 0; k < childCompounded; k++)
					normalQ.add(new Object[] { r, newSourceDSCols,
						targetKey.getTable(), tu,
						Boolean.valueOf(makeDimensions && (r.isOneToOne()) || forceFollowRelation) });
			}
		}
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
	 * @throws ValidationException
	 *             if it is not possible to concat this relation.
	 *
	 * FIXME: Reinstate.
	public void flagConcatOnlyRelation(final Relation relation,
			final DataSetConcatRelationType type) throws ValidationException {
		Log.debug("Flagging concat-only relation " + relation + " in "
				+ this.getName());

		// Sanity check.
		if (!relation.isOneToMany())
			throw new ValidationException(Resources
					.get("cannotConcatNonOneMany"));
		if (relation.getManyKey().getTable().getPrimaryKey() == null)
			throw new ValidationException(Resources
					.get("cannotConcatManyWithoutPK"));

		// Do it.
		final int index = this.concatOnlyRelations[0].indexOf(relation);
		if (index >= 0) {
			this.concatOnlyRelations[0].set(index, relation);
			this.concatOnlyRelations[1].set(index, type);
		} else {
			this.concatOnlyRelations[0].add(relation);
			this.concatOnlyRelations[1].add(type);
		}
	}
	*/

	/**
	 * Returns the central table of this dataset.
	 * 
	 * @return the central table of this dataset.
	 */
	public Table getCentralTable() {
		return this.centralTable;
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
	 * Test to see if this dataset is invisible.
	 * 
	 * @return <tt>true</tt> if it is invisible, <tt>false</tt> otherwise.
	 */
	public boolean getInvisible() {
		return this.invisible;
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
	 * {@inheritDoc}
	 * <p>
	 */
	public Schema replicate(final String newName) {
		Log.debug("Replicating dataset " + this.getName() + " as " + newName);
		try {
			// Create the copy.
			final DataSet newDataSet = new DataSet(this.mart,
					this.centralTable, newName);
			this.getSchemaModifications().replicate(newDataSet.getSchemaModifications());
			// FIXME: Copy dsmods too.
			this.mart.addDataSet(newDataSet);

			// Synchronise it.
			newDataSet.synchronise();

			// Return the copy.
			return newDataSet;
		} catch (final Throwable t) {
			throw new BioMartError(t);
		}
	}

	/**
	 * Sets the post-creation optimiser type this dataset will use.
	 * 
	 * @param optimiser
	 *            the optimiser type to use.
	 */
	public void setDataSetOptimiserType(final DataSetOptimiserType optimiser) {
		Log.debug("Setting optimiser " + optimiser + " in " + this.getName());
		// Do it.
		this.optimiser = optimiser;
	}

	/**
	 * Sets the inivisibility of this dataset.
	 * 
	 * @param invisible
	 *            <tt>true</tt> if it is invisible, <tt>false</tt>
	 *            otherwise.
	 */
	public void setInvisible(final boolean invisible) {
		Log.debug("Setting invisible flag in " + this.getName());
		this.invisible = invisible;
	}

	/**
	 * Synchronise this dataset with the schema that is providing its tables.
	 * Synchronisation means checking the columns and relations and removing any
	 * that have disappeared. The dataset is then regenerated. After
	 * regeneration, any customisations to the dataset such as partitioning are
	 * reapplied to columns which match the original names of the columns from
	 * before regeneration.
	 * 
	 * @throws SQLException
	 *             never thrown - this is inherited from {@link Schema} but does
	 *             not apply here because we are not doing any database
	 *             communications.
	 * @throws DataModelException
	 *             never thrown - this is inherited from {@link Schema} but does
	 *             not apply here because we are not attempting any new logic
	 *             with the schema.
	 */
	public void synchronise() throws SQLException, DataModelException {
		Log.debug("Regenerating dataset " + this.getName());
		// Clear all our tables out as they will all be rebuilt.
		this.removeAllTables();
		this.includedRelations.clear();

		Log.debug("Finding actual central table");
		// Identify main table.
		Table centralTable = this.getCentralTable();
		// If central table has subclass relations and is at the M key
		// end, then follow them to the real central table.
		boolean found;
		do {
			found = false;
			for (final Iterator i = centralTable.getRelations().iterator(); i
					.hasNext()
					&& !found;) {
				final Relation r = (Relation) i.next();
				if (this.schemaMods.isSubclassedRelation(r)
						&& r.getManyKey().getTable().equals(centralTable)) {
					centralTable = r.getOneKey().getTable();
					found = true;
				}
			}
		} while (found);
		Log.debug("Actual central table is " + centralTable);

		// Generate the main table. It will recursively generate all the others.
		this.generateDataSetTable(DataSetTableType.MAIN, null, centralTable,
				null, null);
	}

	/**
	 * A column on a dataset table has to be one of the types of dataset column
	 * available from this class.
	 */
	public static class DataSetColumn extends GenericColumn {
		private boolean dependency;

		// FIXME: Reinstate.
		// private PartitionedColumnType partitionType;

		/**
		 * This constructor gives the column a name.
		 * 
		 * @param name
		 *            the name to give this column.
		 * @param dsTable
		 *            the parent dataset table.
		 */
		public DataSetColumn(final String name, final DataSetTable dsTable) {
			// Call the super constructor using the alias generator to
			// ensure we have a unique name.
			super(name, dsTable);

			Log.debug("Creating dataset column " + name + " of type "
					+ this.getClass().getName());

			// Set up default mask/partition values.
			this.dependency = false;
			// FIXME: Reinstate
			// this.masked = false;
			// this.partitionType = null;
		}

		/**
		 * Retrieves the current dependency flag on this column.
		 * 
		 * @return <tt>true</tt> if this column is a dependency for another
		 *         one.
		 */
		public boolean getDependency() {
			return this.dependency;
		}
		
		/**
		 * Return this modified name including any renames etc.
		 * @return the modified name.
		 */
		public String getModifiedName() {		
			final DataSetModificationSet mods = 
			((DataSet)this.getTable().getSchema()).getDataSetModifications();
			return mods.isColumnRename(this) ? mods.getColumnRename(this) : this.getName();
		}

		/**
		 * Changes the dependency flag on this column.
		 * 
		 * @param dependency
		 *            the new dependency flag. <tt>true</tt> indicates that
		 *            this column is required for the fundamental structure
		 *            of the dataset table to exist. The column will get selected
		 *            regardless of it's masking flag. However, if it is masked,
		 *            it will be removed again after the dependency is
		 *            satisified.
		 */
		public void setDependency(final boolean dependency) {
			this.dependency = dependency;
		}

		/**
		 * Partition/unpartition this column.
		 * 
		 * @param partitionType
		 *            <tt>null</tt> if the column should not be partitioned,
		 *            or a type if it should be.
		 * @throws ValidationException
		 *             if partitioning is not allowed on this column.
		 * FIXME: Reinstate.
		public void setPartitionType(PartitionedColumnType partitionType)
				throws ValidationException {
			Log.debug("Setting partition type on column " + this.getName());
			if (partitionType != null) {
				// Refuse to partition subclass tables.
				if (((DataSetTable) this.getTable()).getType().equals(
						DataSetTableType.MAIN_SUBCLASS))
					throw new ValidationException(Resources
							.get("cannotPartitionSubclassTables"));
				// Check to see if we already have a partitioned column in this
				// table, that is not the same column as this one.
				for (final Iterator i = this.getTable().getColumns().iterator(); i
						.hasNext();) {
					final DataSetColumn testCol = (DataSetColumn) i.next();
					if (!testCol.equals(this)
							&& testCol.getPartitionType() != null)
						throw new ValidationException(Resources
								.get("cannotPartitionMultiColumns"));
				}
			}

			// Do it.
			this.partitionType = partitionType;
		}
		*/

		/**
		 * A column on a dataset table that indicates the concatenation of the
		 * columns of a record in some table beyond a concat-only relation. They
		 * take a reference to the concat-only relation.
		 * FIXME: Reinstate.
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
			 * @param concatRelation
			 *            the concat-only relation that provided this column.
			 * @throws ValidationException
			 *             if the relation is not a concat relation.
			 *
			public ConcatRelationColumn(final String name,
					final DataSetTable dsTable, final Relation concatRelation)
					throws ValidationException {
				// Super first, which will do the alias generation for us.
				super(name, dsTable, concatRelation);

				// Make sure it really is a concat relation.
				if (!((DataSet) dsTable.getSchema()).getConcatOnlyRelations()
						.contains(concatRelation))
					throw new ValidationException(Resources
							.get("relationNotConcatRelation"));
			}

			public void setMasked(boolean masked) throws ValidationException {
				if (masked)
					((DataSet) this.getTable().getSchema()).maskRelation(this
							.getUnderlyingRelation());
			}

			public void setPartitionType(PartitionedColumnType partitionType)
					throws ValidationException {
				if (partitionType != null)
					throw new ValidationException(Resources
							.get("cannotPartitionNonWrapSchColumns"));
			}

		}
		*/

		/**
		 * A column on a dataset table that is an expression bringing together
		 * values from other columns. Those columns should be marked with a
		 * dependency flag to indicate that they are still needed even if
		 * otherwise masked. If this is the case, they can be dropped after the
		 * dependent expression column has been added.
		 * <p>
		 * Note that all expression columns should be added in a single step.
		 * FIXME: Reinstate.
		public static class ExpressionColumn extends DataSetColumn {

			private Map aliases;

			private String expr;

			private boolean groupBy;

			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here.
			 * 
			 * @param name
			 *            the name to give this column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 *
			public ExpressionColumn(final String name,
					final DataSetTable dsTable) {
				// The super constructor will make the alias for us.
				super(name, dsTable);

				// Set some defaults.
				this.aliases = new TreeMap();
				this.expr = "";
				this.groupBy = false;
			}

			/**
			 * Drops all the unused aliases.
			 *
			public void dropUnusedAliases() {
				Log.debug("Trimming unused aliases from " + this.getName());
				final List usedColumns = new ArrayList();
				for (final Iterator i = this.aliases.entrySet().iterator(); i
						.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final DataSetColumn col = (DataSetColumn) entry.getKey();
					final String alias = (String) entry.getValue();
					if (this.expr.indexOf(":" + alias) >= 0)
						usedColumns.add(col);
				}
				// Drop the unused aliases.
				this.aliases.keySet().retainAll(usedColumns);
			}

			/**
			 * Retrieves the map used for setting up aliases.
			 * 
			 * @return the aliases map. Keys must be {@link DataSetColumn}
			 *         instances, and values are aliases used in the expression.
			 *
			public Map getAliases() {
				return this.aliases;
			}

			/**
			 * Returns the set of dependent columns.
			 * 
			 * @return the collection of dependent columns.
			 *
			public Collection getDependentColumns() {
				return this.aliases.keySet();
			}

			/**
			 * Returns the expression, <i>without</i> substitution. This value
			 * is RDBMS-specific.
			 * 
			 * @return the unsubstituted expression.
			 *
			public String getExpression() {
				return this.expr;
			}

			/**
			 * Returns the group-by requirement for this column. See
			 * {@link #setGroupBy(boolean)} for details.
			 * 
			 * @return the flag indicating the group-by requirement.
			 *
			public boolean getGroupBy() {
				return this.groupBy;
			}

			/**
			 * Returns the expression, <i>with</i> substitution. This value is
			 * RDBMS-specific.
			 * 
			 * @return the substituted expression.
			 *
			public String getSubstitutedExpression() {
				Log.debug("Constructing expression for " + this.getName());
				String sub = this.expr;
				for (final Iterator i = this.aliases.entrySet().iterator(); i
						.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final DataSetColumn wrapped = (DataSetColumn) entry
							.getKey();
					final String alias = ":" + (String) entry.getValue();
					sub = sub.replaceAll(alias, wrapped.getName());
				}
				Log.debug("Expression is: " + sub);
				return sub;
			}

			/**
			 * The actual expression. The values from the alias map will be used
			 * to refer to various columns. This value is RDBMS-specific.
			 * 
			 * @param expr
			 *            the actual expression to use.
			 *
			public void setExpression(final String expr) {
				this.expr = expr;
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
			 *
			public void setGroupBy(final boolean groupBy) {
				this.groupBy = groupBy;
			}

			/*
			 * FIXME: Reinstate.
			public void setMasked(boolean masked) throws ValidationException {
				if (masked)
					((DataSetTable) this.getTable()).removeColumn(this);
			}
			*

			/*
			 * FIXME: Reinstate.
			public void setPartitionType(PartitionedColumnType partitionType)
					throws ValidationException {
				if (partitionType != null)
					throw new ValidationException(Resources
							.get("cannotPartitionNonWrapSchColumns"));
			}
			*

		}
	    */

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
			 */
			public InheritedColumn(final DataSetTable dsTable,
					final DataSetColumn dsColumn) {
				// The super constructor will make the alias for us.
				super(dsColumn.getName(), dsTable);
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

			public String getName() {
				return this.dsColumn == null ? super.getName() : this.dsColumn
						.getName();
			}
			
			public String getModifiedName() {
				return this.dsColumn == null ? super.getModifiedName() : this.dsColumn
						.getModifiedName();
			}

			/*
			 * FIXME: Reinstate.
			public void setPartitionType(PartitionedColumnType partitionType)
					throws ValidationException {
				if (partitionType != null)
					throw new ValidationException(Resources
							.get("cannotPartitionNonWrapSchColumns"));
			}
		    */
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
			 *            @param colName the name to give the wrapped column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param underlyingRelation
			 *            the relation that provided this column. The underlying
			 *            relation can be null in only one case - when the table
			 *            is a {@link DataSetTableType#MAIN} table.
			 */
			public WrappedColumn(final Column column,
					final String colName,
					final DataSetTable dsTable,
					final Relation underlyingRelation) {
				// Call the parent which will use the alias generator for us.
				super(
						dsTable.getColumnByName(colName) != null ? colName
								+ "_" + column.getTable().getName()
								: colName, dsTable);

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
	}

	/**
	 * Represents a method of concatenating values in a key referenced by a
	 * concat-only {@link Relation}. It simply represents the separator to use
	 * and the columns to include.
	 * FIXME: Reinstate.
	public static class DataSetConcatRelationType {
		private final String columnSeparator;

		private final List concatColumns;

		private final String recordSeparator;

		/**
		 * The constructor takes parameters which define the columns this object
		 * will concatenate, and the separator to use between values and records
		 * that have been concatenated.
		 * 
		 * @param columnSeparator
		 *            the separator for values in this concat type.
		 * @param recordSeparator
		 *            the separator for records in this concat type.
		 * @param concatColumns
		 *            the columns to concatenate.
		 *
		public DataSetConcatRelationType(final String columnSeparator,
				final String recordSeparator, final List concatColumns) {
			this.concatColumns = concatColumns;
			this.columnSeparator = columnSeparator;
			this.recordSeparator = recordSeparator;
		}

		/**
		 * Displays the value separator for this concat type object.
		 * 
		 * @return the value separator for this concat type object.
		 *
		public String getColumnSeparator() {
			return this.columnSeparator;
		}

		/**
		 * Displays the columns concatenated by this concat type object.
		 * 
		 * @return the columns concatenated by this concat type object.
		 *
		public List getConcatColumns() {
			return this.concatColumns;
		}

		/**
		 * Displays the record separator for this concat type object.
		 * 
		 * @return the record separator for this concat type object.
		 *
		public String getRecordSeparator() {
			return this.recordSeparator;
		}
	}
    */

	/**
	 * This class defines the various different ways of optimising a dataset
	 * after it has been constructed, eg. adding boolean columns.
	 */
	public static class DataSetOptimiserType implements Comparable {

		/**
		 * Use this constant to refer to no optimisation.
		 */
		public static final DataSetOptimiserType NONE = new DataSetOptimiserType(
				"NONE", false, false, false);

		/**
		 * Use this constant to refer to optimising by including an extra column
		 * on the main table for each dimension and populating it with the
		 * number of matching rows in that dimension.
		 */
		public static final DataSetOptimiserType COLUMN = new DataSetOptimiserType(
				"COLUMN", false, false, false);

		/**
		 * Use this constant to refer to no optimising by creating a separate
		 * table linked on a 1:1 basis with the main table, with one column per
		 * dimension populated with the number of matching rows in that
		 * dimension.
		 */
		public static final DataSetOptimiserType TABLE = new DataSetOptimiserType(
				"TABLE", false, true, false);

		/**
		 * Use this constant to refer to optimising by including an extra column
		 * on the main table for each dimension and populating it with 1 or 0
		 * depending whether matching rows exist in that dimension.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL = new DataSetOptimiserType(
				"COLUMN_BOOL", true, false, false);

		/**
		 * Use this constant to refer to no optimising by creating a separate
		 * table linked on a 1:1 basis with the main table, with one column per
		 * dimension populated with 1 or 0 depending whether matching rows exist
		 * in that dimension.
		 */
		public static final DataSetOptimiserType TABLE_BOOL = new DataSetOptimiserType(
				"TABLE_BOOL", true, true, false);

		/**
		 * See {@link #COLUMN} but parent tables will inherit copies of count
		 * columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_INHERIT = new DataSetOptimiserType(
				"COLUMN_INHERIT", false, false, true);

		/**
		 * See {@link #TABLE} but parent tables will inherit copies of count
		 * tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_INHERIT = new DataSetOptimiserType(
				"TABLE_INHERIT", false, true, true);

		/**
		 * See {@link #COLUMN_BOOL} but parent tables will inherit copies of
		 * bool columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL_INHERIT = new DataSetOptimiserType(
				"COLUMN_BOOL_INHERIT", true, false, true);

		/**
		 * See {@link #TABLE_BOOL} but parent tables will inherit copies of bool
		 * tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_BOOL_INHERIT = new DataSetOptimiserType(
				"TABLE_BOOL_INHERIT", true, true, true);

		private final String name;

		private final boolean bool;

		private final boolean table;

		private final boolean inherit;

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this optimiser type object will display when printed.
		 * 
		 * @param name
		 *            the name of the optimiser type.
		 * @param bool
		 *            <tt>true</tt> if bool values (0,1) should be used
		 *            instead of counts.
		 * @param table
		 *            <tt>true</tt> if columns should live in their own
		 *            tables.
		 * @param inherit
		 *            <tt>true</tt> if parent main tables of a subclass table
		 *            should also inherit the column and/or table of this
		 *            optimiser type.
		 */
		private DataSetOptimiserType(final String name, final boolean bool,
				final boolean table, final boolean inherit) {
			this.name = name;
			this.bool = bool;
			this.table = table;
			this.inherit = inherit;
		}

		public int compareTo(final Object o) throws ClassCastException {
			final DataSetOptimiserType c = (DataSetOptimiserType) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(final Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}

		/**
		 * Displays the name of this optimiser type object.
		 * 
		 * @return the name of this optimiser type object.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return <tt>true</tt> if columns counts should be replaced by 0/1
		 * boolean-style values.
		 * 
		 * @return <tt>true</tt> if if columns counts should be replaced by
		 *         0/1 boolean-style values.
		 */
		public boolean getBool() {
			return this.bool;
		}

		/**
		 * Return <tt>true</tt> if columns should live in their own table.
		 * 
		 * @return <tt>true</tt> if columns should live in their own table.
		 */
		public boolean getTable() {
			return this.table;
		}

		/**
		 * Return <tt>true</tt> if parent tables should inherit columns/tables
		 * generated by this optimise.
		 * 
		 * @return <tt>true</tt> if parent tables should inherit,
		 *         <tt>false</tt> otherwise.
		 */
		public boolean getInherit() {
			return this.inherit;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The method simply returns the name of the optimiser type.
		 */
		public String toString() {
			return this.getName();
		}
	}

	/**
	 * This special table represents the merge of one or more other tables by
	 * following a series of relations rooted in a similar series of keys. As
	 * such it has no real columns of its own, so every column is from another
	 * table and is given an alias.
	 */
	public static class DataSetTable extends GenericTable {
		private final List transformationUnits;

		private final DataSetTableType type;

		private final Table focusTable;

		private final Relation focusRelation;
		
		private final Collection includedRelations;

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
		 * @param focusTable
		 *            the schema table this dataset table starts from.
		 * @param focusRelation
		 *            the schema relation used to reach the focus table.
		 *            Can be <tt>null</tt>.
		 */
		public DataSetTable(final String name, final DataSet ds,
				final DataSetTableType type, final Table focusTable,
				final Relation focusRelation) {
			// Super constructor first, using an alias to prevent duplicates.
			super(name, ds);

			Log.debug("Creating dataset table " + name);

			// Remember the other settings.
			this.type = type;
			this.focusTable = focusTable;
			this.focusRelation = focusRelation;
			this.transformationUnits = new ArrayList();
			this.includedRelations = new HashSet();
		}
		
		/**
		 * Obtain all relations used by this dataset table.
		 * @return all relations.
		 */
		public Collection getIncludedRelations() {
			return this.includedRelations;
		}
		
		/**
		 * Return this modified name including any renames etc.
		 * @return the modified name.
		 */
		public String getModifiedName() {
			final DataSetModificationSet mods = 
				((DataSet)this.getSchema()).getDataSetModifications();
			return mods.isTableRename(this) ? mods.getTableRename(this) : this.getName();
		}
		
		/**
		 * Obtain the focus relation for this dataset table. The focus relation
		 * is the one which the transformation uses to reach the focus table.
		 * @return the focus relation.
		 */
		public Relation getFocusRelation() {
			return this.focusRelation;
		}
		
		/**
		 * Obtain the focus table for this dataset table. The focus table
		 * is the one which the transformation starts from.
		 * @return the focus table.
		 */
		public Table getFocusTable() {
			return this.focusTable;
		}

		/**
		 * Adds a transformation unit to the end of the chain.
		 * 
		 * @param tu
		 *            the unit to add.
		 */
		void addTransformationUnit(final TransformationUnit tu) {
			this.transformationUnits.add(tu);
		}

		/**
		 * Gets the ordered list of transformation units.
		 * 
		 * @return the list of units.
		 */
		public List getTransformationUnits() {
			return this.transformationUnits;
		}

		/**
		 * Returns the type of this table specified at construction time.
		 * 
		 * @return the type of this table.
		 */
		public DataSetTableType getType() {
			return this.type;
		}
	}

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class DataSetTableRestriction {

		private Map aliases;

		private String expr;

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
		public DataSetTableRestriction(final String expr, final Map aliases) {
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
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
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
		public String getSubstitutedExpression(final String tablePrefix) {
			Log.debug("Calculating restricted table expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Column col = (Column) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, tablePrefix + "." + col.getName());
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
	 * This class defines the various different types of DataSetTable there are.
	 */
	public static class DataSetTableType implements Comparable {
		/**
		 * Use this constant to refer to a dimension table.
		 */
		public static final DataSetTableType DIMENSION = new DataSetTableType(
				"DIMENSION");

		/**
		 * Use this constant to refer to a main table.
		 */
		public static final DataSetTableType MAIN = new DataSetTableType("MAIN");

		/**
		 * Use this constant to refer to a subclass of a main table.
		 */
		public static final DataSetTableType MAIN_SUBCLASS = new DataSetTableType(
				"MAIN_SUBCLASS");

		private final String name;

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this dataset table type object will display when printed.
		 * 
		 * @param name
		 *            the name of the dataset table type.
		 */
		private DataSetTableType(final String name) {
			this.name = name;
		}

		public int compareTo(final Object o) throws ClassCastException {
			final DataSetTableType c = (DataSetTableType) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(final Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}

		/**
		 * Displays the name of this dataset table type object.
		 * 
		 * @return the name of this dataset table type object.
		 */
		public String getName() {
			return this.name;
		}

		public int hashCode() {
			return this.toString().hashCode();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * This will return the output of {@link #getName()}.
		 */
		public String toString() {
			return this.getName();
		}
	}

	/**
	 * Represents a method of partitioning by column. There are no methods.
	 * Actual logic to divide up by column is left to the mart constructor to
	 * decide.
	 */
	public interface PartitionedColumnType {
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
		public class UniqueValues implements PartitionedColumnType {

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
		public class ValueCollection implements PartitionedColumnType {
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
}
