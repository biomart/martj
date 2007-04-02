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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.biomart.builder.model.DataSet.DataSetColumn.ConcatColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition;
import org.biomart.builder.model.TransformationUnit.Concat;
import org.biomart.builder.model.TransformationUnit.Expression;
import org.biomart.builder.model.TransformationUnit.JoinTable;
import org.biomart.builder.model.TransformationUnit.SelectFromTable;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.Column;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
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
 * @since 0.5
 */
public class DataSet extends GenericSchema {
	private static final long serialVersionUID = 1L;

	private final Table centralTable;

	private boolean invisible;

	private final Mart mart;

	private final Collection includedRelations;

	private final Collection includedSchemas;

	private final SchemaModificationSet schemaMods;

	private final DataSetModificationSet dsMods;

	private DataSetOptimiserType optimiser;

	private boolean indexOptimiser;

	private boolean subclassOptimiser;

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
		this.dsMods = new DataSetModificationSet(this);
		this.includedRelations = new HashSet();
		this.includedSchemas = new HashSet();
	}

	/**
	 * Obtain all relations used by this dataset.
	 * 
	 * @return all relations.
	 */
	public Collection getIncludedRelations() {
		return this.includedRelations;
	}

	/**
	 * Obtain the set of schema modifiers for this dataset.
	 * 
	 * @return the set of modifiers.
	 */
	public SchemaModificationSet getSchemaModifications() {
		return this.schemaMods;
	}

	/**
	 * Obtain the set of dataset modifiers for this dataset.
	 * 
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
			final List sourceDSCols, final Relation sourceRelation,
			final Map subclassCount, final int relationIteration) {
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
		TransformationUnit parentTU = null;
		if (parentDSTable != null) {
			parentTU = new SelectFromTable(parentDSTable);
			dsTable.addTransformationUnit(parentTU);

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
					final boolean inPK = parentDSTablePK.getColumns().contains(
							parentDSCol);
					final boolean inSourceKey = sourceDSCols != null
							&& sourceDSCols.contains(parentDSCol);
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
				parentTU.getNewColumnNameMap()
						.put(parentDSCol.getName(), dsCol);
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
		for (final Iterator i = this.getMart().getSchemas().iterator(); i
				.hasNext();)
			for (final Iterator j = ((Schema) i.next()).getRelations()
					.iterator(); j.hasNext();) {
				final Relation rel = (Relation) j.next();
				final int compounded = this.schemaMods.isCompoundRelation(
						dsTable, rel) ? this.schemaMods.getCompoundRelation(
						dsTable, rel).getN() : 1;
				relationCount.put(rel, new Integer(compounded));
			}

		// Process the table. This operation will populate the initial
		// values in the normal, subclass and dimension queues. We only
		// want dimensions constructed if we are not already constructing
		// a dimension ourselves.
		this.processTable(parentTU, dsTable, dsTablePKCols, realTable, normalQ,
				subclassQ, dimensionQ, sourceDSCols, sourceRelation,
				relationCount, subclassCount, !type
						.equals(DataSetTableType.DIMENSION), relationIteration);

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
			final TransformationUnit previousUnit = (TransformationUnit) triple[3];
			final boolean makeDimensions = ((Boolean) triple[4]).booleanValue();
			final int iteration = ((Integer) triple[5]).intValue();
			this.processTable(previousUnit, dsTable, dsTablePKCols, mergeTable,
					normalQ, subclassQ, dimensionQ, newSourceDSCols,
					mergeSourceRelation, relationCount, subclassCount,
					makeDimensions, iteration);
		}

		// Create the primary key on this table, but only if it has one.
		// Don't bother for dimensions.
		if (!dsTablePKCols.isEmpty()
				&& !dsTable.getType().equals(DataSetTableType.DIMENSION))
			// Create the key.
			dsTable.setPrimaryKey(new GenericPrimaryKey(dsTablePKCols));

		// Insert Expression Column Transformation Unit
		// containing all expression columns defined on this table.
		if (this.dsMods.hasExpressionColumn(dsTable)) {
			final Collection aliases = new HashSet();
			final Expression tu = new Expression((TransformationUnit) dsTable
					.getTransformationUnits().get(
							dsTable.getTransformationUnits().size() - 1),
					dsTable);
			dsTable.addTransformationUnit(tu);
			for (final Iterator i = ((Collection) this.dsMods
					.getExpressionColumns().get(dsTable.getName())).iterator(); i
					.hasNext();) {
				final ExpressionColumnDefinition expr = (ExpressionColumnDefinition) i
						.next();
				// Save up the aliases to make dependents later.
				aliases.addAll(expr.getAliases().keySet());
				final String expColName = expr.getColKey();
				final ExpressionColumn expCol = new ExpressionColumn(
						expColName, dsTable, expr);
				dsTable.addColumn(expCol);
				tu.getNewColumnNameMap().put(expColName, expCol);
			}
			// Mark all aliased columns as dependents
			for (final Iterator j = aliases.iterator(); j.hasNext();)
				((DataSetColumn) dsTable.getColumnByName((String) j.next()))
						.setExpressionDependency(true);
		}

		// Only dataset tables with primary keys can have subclasses
		// or dimensions.
		if (dsTable.getPrimaryKey() != null) {
			// Process the subclass relations of this table.
			for (int i = 0; i < subclassQ.size(); i++) {
				final Object[] triple = (Object[]) subclassQ.get(i);
				final List newSourceDSCols = (List) triple[0];
				final Relation subclassRelation = (Relation) triple[1];
				final int iteration = ((Integer) triple[2]).intValue();
				this.generateDataSetTable(DataSetTableType.MAIN_SUBCLASS,
						dsTable, subclassRelation.getManyKey().getTable(),
						newSourceDSCols, subclassRelation, subclassCount,
						iteration);
			}

			// Process the dimension relations of this table. For 1:M it's easy.
			// For M:M, we have to work out which end is connected to the real
			// table, then process the table at the other end of the relation.
			for (int i = 0; i < dimensionQ.size(); i++) {
				final Object[] triple = (Object[]) dimensionQ.get(i);
				final List newSourceDSCols = (List) triple[0];
				final Relation dimensionRelation = (Relation) triple[1];
				final int iteration = ((Integer) triple[2]).intValue();
				if (dimensionRelation.isOneToMany())
					this.generateDataSetTable(DataSetTableType.DIMENSION,
							dsTable, dimensionRelation.getManyKey().getTable(),
							newSourceDSCols, dimensionRelation, subclassCount,
							iteration);
				else
					this.generateDataSetTable(DataSetTableType.DIMENSION,
							dsTable, dimensionRelation.getFirstKey().getTable()
									.equals(realTable) ? dimensionRelation
									.getSecondKey().getTable()
									: dimensionRelation.getFirstKey()
											.getTable(), newSourceDSCols,
							dimensionRelation, subclassCount, iteration);
			}
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
	 * @param subclassCount
	 *            how many times we have followed a particular subclass
	 *            relation.
	 * @param makeDimensions
	 *            <tt>true</tt> if we should add potential dimension tables to
	 *            the dimension queue, <tt>false</tt> if we should just ignore
	 *            them. This is useful for preventing dimensions from gaining
	 *            dimensions of their own.
	 */
	private void processTable(final TransformationUnit previousUnit,
			final DataSetTable dsTable, final List dsTablePKCols,
			final Table mergeTable, final List normalQ, final List subclassQ,
			final List dimensionQ, final List sourceDataSetCols,
			final Relation sourceRelation, final Map relationCount,
			final Map subclassCount, final boolean makeDimensions,
			final int relationIteration) {
		Log.debug("Processing table " + mergeTable);

		// Remember the schema.
		this.includedSchemas.add(mergeTable.getSchema());

		// Don't ignore any keys by default.
		final Set ignoreCols = new HashSet();

		final boolean isConcat = this.schemaMods.isConcatRelation(dsTable,
				sourceRelation, relationIteration);

		final TransformationUnit tu;

		// Did we get here via somewhere else?
		if (sourceRelation != null) {
			// Work out what key to ignore by working out at which end
			// of the relation we are.
			final Key ignoreKey = sourceRelation.getKeyForTable(mergeTable);
			ignoreCols.addAll(ignoreKey.getColumns());
			final Key mergeKey = sourceRelation.getOtherKey(ignoreKey);

			// Add the relation and key to the list that the table depends on.
			// This list is what defines the path required to construct
			// the DDL for this table.
			if (isConcat)
				tu = new Concat(previousUnit, mergeTable, sourceDataSetCols,
						mergeKey, sourceRelation, relationIteration);
			else
				tu = new JoinTable(previousUnit, mergeTable, sourceDataSetCols,
						mergeKey, sourceRelation, relationIteration);

			// Remember we've been here.
			this.includedRelations.add(sourceRelation);
		} else
			tu = new SelectFromTable(mergeTable);

		dsTable.addTransformationUnit(tu);

		// Work out the merge table's PK.
		final PrimaryKey mergeTablePK = mergeTable.getPrimaryKey();

		// We must merge only the first PK we come across, if this is
		// a main table, or the first PK we come across after the
		// inherited PK, if this is a subclass. Dimensions dont get
		// merged at all.
		boolean includeMergeTablePK = mergeTablePK != null
				&& !mergeTablePK.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT)
				&& !dsTable.getType().equals(DataSetTableType.DIMENSION);
		if (includeMergeTablePK && sourceRelation != null)
			// Only add further PK columns if the relation did NOT
			// involve our PK and was NOT 1:1.
			includeMergeTablePK = dsTablePKCols.isEmpty()
					&& !sourceRelation.isOneToOne()
					&& !sourceRelation.getFirstKey().equals(mergeTablePK)
					&& !sourceRelation.getSecondKey().equals(mergeTablePK);

		// Add all columns from merge table to dataset table, except those in
		// the ignore key.
		if (isConcat) {
			final ConcatRelationDefinition expr = this.schemaMods
					.getConcatRelation(dsTable, sourceRelation,
							relationIteration);
			final String expColName = expr.getColKey();
			final ConcatColumn expCol = new ConcatColumn(expColName, dsTable,
					expr);
			dsTable.addColumn(expCol);
			tu.getNewColumnNameMap().put(expColName, expCol);
		} else
			for (final Iterator i = mergeTable.getColumns().iterator(); i
					.hasNext();) {
				final Column c = (Column) i.next();

				// Ignore those in the key used to get here.
				if (ignoreCols.contains(c))
					continue;

				// Create a wrapped column for this column.
				String colName = c.getName();
				// Rename all PK columns to have the '_key' suffix.
				if (includeMergeTablePK
						&& mergeTablePK.getColumns().contains(c)
						&& !colName.endsWith(Resources.get("keySuffix")))
					colName = colName + Resources.get("keySuffix");
				final WrappedColumn wc = new WrappedColumn(c, colName, dsTable,
						sourceRelation);
				tu.getNewColumnNameMap().put(c.getName(), wc);
				dsTable.addColumn(wc);

				// If the column is in any key on this table then it is a
				// dependency for possible future linking, which must be
				// flagged.
				wc.setKeyDependency(c.isInAnyKey());

				// If the column was in the merge table's PK, and we are
				// expecting to add the PK to the generated table's PK, then
				// add it to the generated table's PK.
				if (includeMergeTablePK
						&& mergeTablePK.getColumns().contains(c))
					dsTablePKCols.add(wc);
			}

		// Update the three queues with relations that lead away from this
		// table.
		if (!isConcat)
			for (final Iterator i = mergeTable.getRelations().iterator(); i
					.hasNext();) {
				final Relation r = (Relation) i.next();

				// Don't repeat relations or go back up relation just followed.
				if (r.equals(sourceRelation)
						|| ((Integer) relationCount.get(r)).intValue() <= 0)
					continue;

				// Don't follow masked relations.
				if (this.schemaMods.isMaskedRelation(dsTable, r))
					continue;

				// Don't follow directional relations from the wrong end.
				if (this.schemaMods.isDirectionalRelation(dsTable, r)
						&& !r.getFirstKey().getTable().equals(
								r.getSecondKey().getTable())
						&& !this.schemaMods.getDirectionalRelation(dsTable, r)
								.equals(r.getKeyForTable(mergeTable)))
					continue;

				// Don't follow incorrect relations, or relations
				// between incorrect keys.
				if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT)
						|| r.getFirstKey().getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT)
						|| r.getSecondKey().getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT))
					continue;

				// Decrement the relation counter.
				relationCount.put(r, new Integer(((Integer) relationCount
						.get(r)).intValue() - 1));

				// Set up a holder to indicate whether or not to follow
				// the relation.
				boolean followRelation = false;
				boolean forceFollowRelation = false;

				// Are we at the 1 end of a 1:M?
				// If so, we may need to make a dimension, a subclass, or
				// a concat column.
				if (r.isOneToMany()
						&& r.getOneKey().getTable().equals(mergeTable)
						&& (!this.schemaMods.isDirectionalRelation(dsTable, r) || this.schemaMods
								.getDirectionalRelation(dsTable, r).equals(
										r.getOneKey()))) {

					// Forcibly follow concat relations.
					if (this.schemaMods.isConcatRelation(dsTable, r))
						forceFollowRelation = true;

					// Subclass subclassed relations, if we are currently
					// not building a dimension table.
					else if (this.schemaMods.isSubclassedRelation(r)
							&& !dsTable.getType().equals(
									DataSetTableType.DIMENSION)) {
						final List newSourceDSCols = new ArrayList();
						for (final Iterator j = r.getOneKey().getColumns()
								.iterator(); j.hasNext();)
							newSourceDSCols.add(tu
									.getDataSetColumnFor((Column) j.next()));
						// Deal with recursive subclasses.
						final int nextSC = subclassCount.containsKey(r) ? ((Integer) subclassCount
								.get(r)).intValue() + 1
								: 0;
						subclassCount.put(r, new Integer(nextSC));
						// Only do this if the subclassCount is less than
						// the maximum allowed.
						final int childCompounded = this.schemaMods
								.isCompoundRelation(dsTable, r) ? this.schemaMods
								.getCompoundRelation(dsTable, r).getN()
								: 1;
						if (nextSC < childCompounded)
							subclassQ.add(new Object[] { newSourceDSCols, r,
									new Integer(nextSC) });
					}

					// Dimensionize dimension relations, which are all other 1:M
					// relations, if we are not constructing a dimension
					// table, and are currently intending to construct
					// dimensions.
					else if (makeDimensions
							&& !dsTable.getType().equals(
									DataSetTableType.DIMENSION)) {
						final List newSourceDSCols = new ArrayList();
						for (final Iterator j = r.getOneKey().getColumns()
								.iterator(); j.hasNext();) {
							final DataSetColumn newCol = tu
									.getDataSetColumnFor((Column) j.next());
							newSourceDSCols.add(newCol);
						}
						int childCompounded = 1;
						if (this.schemaMods.isCompoundRelation(dsTable, r)
								&& this.schemaMods.getCompoundRelation(dsTable,
										r).isParallel())
							childCompounded = this.schemaMods
									.getCompoundRelation(dsTable, r).getN();
						// Follow the relation.
						for (int k = 0; k < childCompounded; k++)
							dimensionQ.add(new Object[] { newSourceDSCols, r,
									new Integer(k) });
						if (this.schemaMods.isMergedRelation(r))
							forceFollowRelation = true;
					}

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

					final Key sourceKey = (this.schemaMods
							.isDirectionalRelation(dsTable, r) && r
							.getFirstKey().getTable().equals(
									r.getSecondKey().getTable())) ? this.schemaMods
							.getDirectionalRelation(dsTable, r)
							: r.getKeyForTable(mergeTable);
					final Key targetKey = r.getOtherKey(sourceKey);
					final List newSourceDSCols = new ArrayList();
					for (final Iterator j = sourceKey.getColumns().iterator(); j
							.hasNext();)
						newSourceDSCols.add(tu.getDataSetColumnFor((Column) j
								.next()));
					// Repeat queueing of relation N times if compounded.
					int childCompounded = 1;
					if (this.schemaMods.isCompoundRelation(dsTable, r)
							&& this.schemaMods.getCompoundRelation(dsTable, r)
									.isParallel())
						childCompounded = this.schemaMods.getCompoundRelation(
								dsTable, r).getN();
					for (int k = 0; k < childCompounded; k++)
						normalQ
								.add(new Object[] {
										r,
										newSourceDSCols,
										targetKey.getTable(),
										tu,
										Boolean.valueOf(makeDimensions
												&& r.isOneToOne()
												|| forceFollowRelation),
										new Integer(k) });
				}
			}
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
	 * Follows subclassed relations to find where transformation should really
	 * start for this dataset.
	 * 
	 * @return the real central table.
	 */
	public Table getRealCentralTable() {
		Log.debug("Finding actual central table");
		// Identify main table.
		final Table realCentralTable = this.getCentralTable();
		Table centralTable = realCentralTable;
		// If central table has subclass relations and is at the M key
		// end, then follow them to the real central table.
		boolean found;
		do {
			found = false;
			final Set rels = new HashSet(centralTable.getRelations());
			rels.retainAll(this.schemaMods.getSubclassedRelations());
			if (rels.size() > 1) {
				centralTable = ((Relation) rels.iterator().next()).getOneKey()
						.getTable();
				found = true;
			}
		} while (found && centralTable != realCentralTable);
		Log.debug("Actual central table is " + centralTable);
		return centralTable;
	}

	/**
	 * Returns the central table of this dataset.
	 * 
	 * @return the central table of this dataset.
	 */
	public DataSetTable getMainTable() {
		for (final Iterator i = this.getTables().iterator(); i.hasNext();) {
			final DataSetTable dst = (DataSetTable) i.next();
			if (dst.getType().equals(DataSetTableType.MAIN))
				return dst;
		}
		// Should never happen.
		throw new BioMartError();
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
	 * Sees if the optimiser will index its columns.
	 * 
	 * @return <tt>true</tt> if it will.
	 */
	public boolean isIndexOptimiser() {
		return this.indexOptimiser;
	}

	/**
	 * Sees if the optimiser will count subclasses.
	 * 
	 * @return <tt>true</tt> if it will.
	 */
	public boolean isSubclassOptimiser() {
		return this.subclassOptimiser;
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
			this.getSchemaModifications().replicate(
					newDataSet.getSchemaModifications());
			this.getDataSetModifications().replicate(
					newDataSet.getDataSetModifications());
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
	 * Sets the optimiser index type.
	 * 
	 * @param index
	 *            the optimiser index if <tt>true</tt>.
	 */
	public void setIndexOptimiser(final boolean index) {
		Log.debug("Setting optimiser index in " + this.getName());
		// Do it.
		this.indexOptimiser = index;
	}

	/**
	 * Sets the subclass count optimiser type.
	 * 
	 * @param subclassCount
	 *            the subclass optimiser if <tt>true</tt>.
	 */
	public void setSubclassOptimiser(final boolean subclassCount) {
		Log.debug("Setting subclass optimiser in " + this.getName());
		// Do it.
		this.subclassOptimiser = subclassCount;
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
		this.includedSchemas.clear();

		// Generate the main table. It will recursively generate all the others.
		this.generateDataSetTable(DataSetTableType.MAIN, null, this
				.getRealCentralTable(), null, null, new HashMap(), 0);

		// Update the modification sets.
		this.dsMods.synchronise();
	}

	/**
	 * Find out what schemas are used in this dataset.
	 * 
	 * @return the set of schemas used.
	 */
	public Collection getIncludedSchemas() {
		return this.includedSchemas;
	}

	/**
	 * See if the given schema is used in this dataset.
	 * 
	 * @param schema
	 *            the schema to check for.
	 * @return <tt>true</tt> if this dataset uses the given schema, or if it
	 *         is unsure.
	 */
	public boolean usesSchema(final Schema schema) {
		return this.includedSchemas.isEmpty()
				|| this.includedSchemas.contains(schema);
	}

	/**
	 * A column on a dataset table has to be one of the types of dataset column
	 * available from this class.
	 */
	public static class DataSetColumn extends
			org.biomart.common.model.Column.GenericColumn {
		private static final long serialVersionUID = 1L;

		private boolean keyDependency;

		private boolean expressionDependency;

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
			this.keyDependency = false;
			this.expressionDependency = false;
		}

		/**
		 * Test to see if this column is required during intermediate
		 * construction phases.
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isRequiredInterim() {
			final DataSetModificationSet mods = ((DataSet) this.getTable()
					.getSchema()).getDataSetModifications();
			return this.keyDependency || this.expressionDependency
					|| !mods.isMaskedColumn(this);
		}

		/**
		 * Test to see if this column is required in the final completed dataset
		 * table.
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isRequiredFinal() {
			if (this.keyDependency)
				return true;
			final DataSetModificationSet mods = ((DataSet) this.getTable()
					.getSchema()).getDataSetModifications();
			// If appears in aliases on any group-by expression column
			// then is not required final.
			final Collection exprCols = (Collection) mods
					.getExpressionColumns().get(this.getTable().getName());
			if (exprCols != null)
				for (final Iterator i = exprCols.iterator(); i.hasNext();) {
					final ExpressionColumnDefinition entry = (ExpressionColumnDefinition) i
							.next();
					if (entry.isGroupBy()
							&& entry.getAliases().containsKey(this.getName()))
						return false;
				}
			return !mods.isMaskedColumn(this);
		}

		/**
		 * Return this modified name including any renames etc.
		 * 
		 * @return the modified name.
		 */
		public String getModifiedName() {
			final DataSetModificationSet mods = ((DataSet) this.getTable()
					.getSchema()).getDataSetModifications();
			return mods.isColumnRename(this) ? mods.getColumnRename(this)
					: this.getName();
		}

		/**
		 * Changes the dependency flag on this column.
		 * 
		 * @param dependency
		 *            the new dependency flag. <tt>true</tt> indicates that
		 *            this column is required for the fundamental structure of
		 *            the dataset table to exist. The column will get selected
		 *            regardless of it's masking flag. However, if it is masked,
		 *            it will be removed again after the dependency is
		 *            satisified.
		 */
		public void setKeyDependency(final boolean dependency) {
			this.keyDependency = dependency;
		}

		/**
		 * Changes the dependency flag on this column.
		 * 
		 * @param dependency
		 *            the new dependency flag. <tt>true</tt> indicates that
		 *            this column is required for the fundamental structure of
		 *            the dataset table to exist. The column will get selected
		 *            regardless of it's masking flag. However, if it is masked,
		 *            it will be removed again after the dependency is
		 *            satisified.
		 */
		public void setExpressionDependency(final boolean dependency) {
			this.expressionDependency = dependency;
		}

		/**
		 * Is this column required as a dependency?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isKeyDependency() {
			return this.keyDependency;
		}

		/**
		 * Is this column required as a dependency?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isExpressionDependency() {
			return this.expressionDependency;
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
			private static final long serialVersionUID = 1L;

			private final ExpressionColumnDefinition definition;

			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here.
			 * 
			 * @param name
			 *            the name to give this column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param definition
			 *            the definition of this column's expression.
			 */
			public ExpressionColumn(final String name,
					final DataSetTable dsTable,
					final ExpressionColumnDefinition definition) {
				// The super constructor will make the alias for us.
				super(name, dsTable);
				this.definition = definition;
			}

			/**
			 * Obtain the expression behind this column.
			 * 
			 * @return the expression.
			 */
			public ExpressionColumnDefinition getDefinition() {
				return this.definition;
			}

			public boolean isRequiredInterim() {
				return !this.definition.isOptimiser()
						|| super.isRequiredInterim();
			}

			public boolean isRequiredFinal() {
				return !this.definition.isOptimiser()
						|| super.isRequiredFinal();
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
		public static class ConcatColumn extends DataSetColumn {
			private static final long serialVersionUID = 1L;

			private final ConcatRelationDefinition definition;

			/**
			 * This constructor gives the column a name. The underlying relation
			 * is not required here.
			 * 
			 * @param name
			 *            the name to give this column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param definition
			 *            the definition of the concat expression.
			 */
			public ConcatColumn(final String name, final DataSetTable dsTable,
					final ConcatRelationDefinition definition) {
				// The super constructor will make the alias for us.
				super(name, dsTable);
				this.definition = definition;
			}

			/**
			 * Obtain the expression used in each concatenated row inside this
			 * column.
			 * 
			 * @return the concat expression used.
			 */
			public ConcatRelationDefinition getDefinition() {
				return this.definition;
			}
		}

		/**
		 * A column on a dataset table that is inherited from a parent dataset
		 * table.
		 */
		public static class InheritedColumn extends DataSetColumn {
			private static final long serialVersionUID = 1L;

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
				super(dsColumn.getModifiedName(), dsTable);
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

			public String getModifiedName() {
				return this.getName();
			}
		}

		/**
		 * A column on a dataset table that wraps an existing column but is
		 * otherwise identical to a normal column. It assigns itself an alias if
		 * the original name is already used in the dataset table.
		 */
		public static class WrappedColumn extends DataSetColumn {
			private static final long serialVersionUID = 1L;

			private final Column column;

			/**
			 * This constructor wraps an existing column. It also assigns an
			 * alias to the wrapped column if another one with the same name
			 * already exists on this table.
			 * 
			 * @param column
			 *            the column to wrap.
			 * @param colName
			 *            the name to give the wrapped column.
			 * @param dsTable
			 *            the dataset table to add the wrapped column to.
			 * @param underlyingRelation
			 *            the relation that provided this column. The underlying
			 *            relation can be null in only one case - when the table
			 *            is a {@link DataSetTableType#MAIN} table.
			 */
			public WrappedColumn(final Column column, final String colName,
					final DataSetTable dsTable,
					final Relation underlyingRelation) {
				// Call the parent which will use the alias generator for us.
				super(dsTable.getColumnByName(colName) != null ? colName
						.endsWith(Resources.get("keySuffix")) ? colName
						.substring(0, colName.indexOf(Resources
								.get("keySuffix")))
						+ "_"
						+ column.getTable().getName()
						+ Resources.get("keySuffix") : colName + "_"
						+ column.getTable().getName() : colName, dsTable);

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
	 * This class defines the various different ways of optimising a dataset
	 * after it has been constructed, eg. adding boolean columns.
	 */
	public static class DataSetOptimiserType implements Comparable {
		private static final long serialVersionUID = 1L;

		/**
		 * Use this constant to refer to no optimisation.
		 */
		public static final DataSetOptimiserType NONE = new DataSetOptimiserType(
				"NONE", false, false, false, false);

		/**
		 * Use this constant to refer to optimising by including an extra column
		 * on the main table for each dimension and populating it with the
		 * number of matching rows in that dimension.
		 */
		public static final DataSetOptimiserType COLUMN = new DataSetOptimiserType(
				"COLUMN", false, false, false, false);

		/**
		 * Use this constant to refer to no optimising by creating a separate
		 * table linked on a 1:1 basis with the main table, with one column per
		 * dimension populated with the number of matching rows in that
		 * dimension.
		 */
		public static final DataSetOptimiserType TABLE = new DataSetOptimiserType(
				"TABLE", false, true, false, false);

		/**
		 * Use this constant to refer to optimising by including an extra column
		 * on the main table for each dimension and populating it with 1 or 0
		 * depending whether matching rows exist in that dimension.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL = new DataSetOptimiserType(
				"COLUMN_BOOL", true, false, false, false);

		/**
		 * Use this constant to refer to no optimising by creating a separate
		 * table linked on a 1:1 basis with the main table, with one column per
		 * dimension populated with 1 or 0 depending whether matching rows exist
		 * in that dimension.
		 */
		public static final DataSetOptimiserType TABLE_BOOL = new DataSetOptimiserType(
				"TABLE_BOOL", true, true, false, false);

		/**
		 * Use this constant to refer to optimising by including an extra column
		 * on the main table for each dimension and populating it with 1 or null
		 * depending whether matching rows exist in that dimension.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL_NULL = new DataSetOptimiserType(
				"COLUMN_BOOL_NULL", true, false, false, true);

		/**
		 * Use this constant to refer to no optimising by creating a separate
		 * table linked on a 1:1 basis with the main table, with one column per
		 * dimension populated with 1 or null depending whether matching rows
		 * exist in that dimension.
		 */
		public static final DataSetOptimiserType TABLE_BOOL_NULL = new DataSetOptimiserType(
				"TABLE_BOOL_NULL", true, true, false, true);

		/**
		 * See {@link #COLUMN} but parent tables will inherit copies of count
		 * columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_INHERIT = new DataSetOptimiserType(
				"COLUMN_INHERIT", false, false, true, false);

		/**
		 * See {@link #TABLE} but parent tables will inherit copies of count
		 * tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_INHERIT = new DataSetOptimiserType(
				"TABLE_INHERIT", false, true, true, false);

		/**
		 * See {@link #COLUMN_BOOL} but parent tables will inherit copies of
		 * bool columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL_INHERIT = new DataSetOptimiserType(
				"COLUMN_BOOL_INHERIT", true, false, true, false);

		/**
		 * See {@link #TABLE_BOOL} but parent tables will inherit copies of bool
		 * tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_BOOL_INHERIT = new DataSetOptimiserType(
				"TABLE_BOOL_INHERIT", true, true, true, false);

		/**
		 * See {@link #COLUMN_BOOL_NULL} but parent tables will inherit copies
		 * of bool columns from child tables.
		 */
		public static final DataSetOptimiserType COLUMN_BOOL_NULL_INHERIT = new DataSetOptimiserType(
				"COLUMN_BOOL_NULL_INHERIT", true, false, true, true);

		/**
		 * See {@link #TABLE_BOOL_NULL} but parent tables will inherit copies of
		 * bool tables from child tables.
		 */
		public static final DataSetOptimiserType TABLE_BOOL_NULL_INHERIT = new DataSetOptimiserType(
				"TABLE_BOOL_NULL_INHERIT", true, true, true, true);

		private final String name;

		private final boolean bool;

		private final boolean table;

		private final boolean inherit;

		private final boolean useNull;

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
		 * @param useNull
		 *            if this is a bool column, use null/1 instead of 0/1.
		 */
		private DataSetOptimiserType(final String name, final boolean bool,
				final boolean table, final boolean inherit,
				final boolean useNull) {
			this.name = name;
			this.bool = bool;
			this.table = table;
			this.inherit = inherit;
			this.useNull = useNull;
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
		 * @return <tt>true</tt> if columns counts should be replaced by 0/1
		 *         boolean-style values.
		 */
		public boolean isBool() {
			return this.bool;
		}

		/**
		 * Return <tt>true</tt> if columns 0/1 values should be replaced by
		 * null/1 equivalents.
		 * 
		 * @return <tt>true</tt> if columns 0/1 values should be replaced by
		 *         null/1 equivalents.
		 */
		public boolean isUseNull() {
			return this.useNull;
		}

		/**
		 * Return <tt>true</tt> if columns should live in their own table.
		 * 
		 * @return <tt>true</tt> if columns should live in their own table.
		 */
		public boolean isTable() {
			return this.table;
		}

		/**
		 * Return <tt>true</tt> if parent tables should inherit columns/tables
		 * generated by this optimise.
		 * 
		 * @return <tt>true</tt> if parent tables should inherit,
		 *         <tt>false</tt> otherwise.
		 */
		public boolean isInherit() {
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

		/**
		 * Return the types of optimiser column available.
		 * 
		 * @return the types available. Keys are internal names for the types,
		 *         values are the actual type instances.
		 */
		public static Map getTypes() {
			final Map optimiserTypes = new LinkedHashMap();
			optimiserTypes.put("None", DataSetOptimiserType.NONE);
			optimiserTypes.put("Column", DataSetOptimiserType.COLUMN);
			optimiserTypes.put("ColumnInherit",
					DataSetOptimiserType.COLUMN_INHERIT);
			optimiserTypes.put("ColumnBool", DataSetOptimiserType.COLUMN_BOOL);
			optimiserTypes.put("ColumnBoolInherit",
					DataSetOptimiserType.COLUMN_BOOL_INHERIT);
			optimiserTypes.put("ColumnBoolNull",
					DataSetOptimiserType.COLUMN_BOOL_NULL);
			optimiserTypes.put("ColumnBoolNullInherit",
					DataSetOptimiserType.COLUMN_BOOL_NULL_INHERIT);
			optimiserTypes.put("Table", DataSetOptimiserType.TABLE);
			optimiserTypes.put("TableInherit",
					DataSetOptimiserType.TABLE_INHERIT);
			optimiserTypes.put("TableBool", DataSetOptimiserType.TABLE_BOOL);
			optimiserTypes.put("TableBoolInherit",
					DataSetOptimiserType.TABLE_BOOL_INHERIT);
			optimiserTypes.put("TableBoolNull",
					DataSetOptimiserType.TABLE_BOOL_NULL);
			optimiserTypes.put("TableBoolNullInherit",
					DataSetOptimiserType.TABLE_BOOL_NULL_INHERIT);
			return optimiserTypes;
		}
	}

	/**
	 * This special table represents the merge of one or more other tables by
	 * following a series of relations rooted in a similar series of keys. As
	 * such it has no real columns of its own, so every column is from another
	 * table and is given an alias.
	 */
	public static class DataSetTable extends
			org.biomart.common.model.Table.GenericTable {
		private static final long serialVersionUID = 1L;

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
		 *            the schema relation used to reach the focus table. Can be
		 *            <tt>null</tt>.
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
		 * 
		 * @return all relations.
		 */
		public Collection getIncludedRelations() {
			return this.includedRelations;
		}

		/**
		 * Return this modified name including any renames etc.
		 * 
		 * @return the modified name.
		 */
		public String getModifiedName() {
			final DataSetModificationSet mods = ((DataSet) this.getSchema())
					.getDataSetModifications();
			return mods.isTableRename(this) ? mods.getTableRename(this) : this
					.getName();
		}

		/**
		 * Return this modified name including any renames etc.
		 * 
		 * @param columnName
		 *            the column name to look up the modified name for.
		 * @return the modified name.
		 */
		public String getModifiedName(String columnName) {
			return ((DataSetColumn) this.getColumnByName(columnName))
					.getModifiedName();
		}

		/**
		 * Obtain the focus relation for this dataset table. The focus relation
		 * is the one which the transformation uses to reach the focus table.
		 * 
		 * @return the focus relation.
		 */
		public Relation getFocusRelation() {
			return this.focusRelation;
		}

		/**
		 * Obtain the focus table for this dataset table. The focus table is the
		 * one which the transformation starts from.
		 * 
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
	 * This class defines the various different types of DataSetTable there are.
	 */
	public static class DataSetTableType implements Comparable {
		private static final long serialVersionUID = 1L;

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
}
