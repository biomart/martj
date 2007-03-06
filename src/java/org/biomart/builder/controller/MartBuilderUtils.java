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

package org.biomart.builder.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.controller.dialects.DatabaseDialect;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedTableDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition.RecursionType;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Relation.Cardinality;
import org.biomart.common.model.Relation.GenericRelation;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * Tools for working with the mart from a GUI or CLI. These wrapper methods
 * exist to prevent the GUI or CLI having to know about the exact details of
 * manipulating the various objects in the data model.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class MartBuilderUtils {

	/**
	 * Sets the output schema on a mart.
	 * 
	 * @param mart
	 *            the mart to change.
	 * @param outputSchema
	 *            the new output schema value.
	 */
	public static void setOutputSchema(final Mart mart,
			final String outputSchema) {
		Log.info(Resources.get("logReqOutputSchema"));
		mart.setOutputSchema(outputSchema);
	}

	/**
	 * Adds a schema to a mart.
	 * 
	 * @param mart
	 *            the mart to add the schema to.
	 * @param schema
	 *            the schema to add to the mart.
	 */
	public static void addSchemaToMart(final Mart mart, final Schema schema) {
		Log.info(Resources.get("logReqAddSchemaToMart"));
		mart.addSchema(schema);
	}

	/**
	 * Changes the status of a key. All datasets will subsequently be
	 * synchronised.
	 * 
	 * @param mart
	 *            the mart to change the key in.
	 * @param key
	 *            the key to change.
	 * @param status
	 *            the new status to give the key.
	 */
	public static void changeKeyStatus(final Mart mart, final Key key,
			final ComponentStatus status) {
		Log.info(Resources.get("logReqChangeKeyStatus"));
		key.setStatus(status);
		mart.synchroniseDataSets();
	}

	/**
	 * Changes the post-creation optimiser type for a dataset.
	 * 
	 * @param dataset
	 *            the dataset to change the optimiser for.
	 * @param type
	 *            the new optimiser type to use.
	 */
	public static void changeOptimiserType(final DataSet dataset,
			final DataSetOptimiserType type) {
		Log.info(Resources.get("logReqChangeOptimiser"));
		dataset.setDataSetOptimiserType(type);
	}

	/**
	 * Changes the cardinality of a relation. If the relation is going to become
	 * 1:1, it will also remove any flags from the relation that indicate
	 * subclassing or concat-only. This is because 1:1 relations cannot be used
	 * for those purposes.
	 * 
	 * @param mart
	 *            the mart to change the relation in.
	 * @param relation
	 *            the relation to change.
	 * @param cardinality
	 *            the new cardinality to give the relation.
	 */
	public static void changeRelationCardinality(final Mart mart,
			final Relation relation, final Cardinality cardinality) {
		Log.info(Resources.get("logReqChangeCardinality"));
		// Change the cardinality.
		relation.setCardinality(cardinality);

		// If 1:1, make sure it isn't used as a subclass relation
		// in any dataset.
		if (relation.isOneToOne())
			for (final Iterator i = mart.getDataSets().iterator(); i.hasNext();) {
				final DataSet ds = (DataSet) i.next();
				ds.getSchemaModifications().unsetSubclassedRelation(relation);
			}

		// Synchronise the datasets.
		mart.synchroniseDataSets();
	}

	/**
	 * Changes the status of a relation within a mart.
	 * 
	 * @param mart
	 *            the mart to change the relation within.
	 * @param relation
	 *            the relation to change.
	 * @param status
	 *            the new status to give it.
	 * @throws AssociationException
	 *             if the relation was previously
	 *             {@link ComponentStatus#INFERRED_INCORRECT} and the keys at
	 *             either end do not have the same number of columns as each
	 *             other, then this exception will be thrown to tell you so.
	 */
	public static void changeRelationStatus(final Mart mart,
			final Relation relation, final ComponentStatus status)
			throws AssociationException {
		Log.info(Resources.get("logReqChangeRelStatus"));
		relation.setStatus(status);
		mart.synchroniseDataSets();
	}

	/**
	 * Attempts to establish a relation between two keys in a mart. The relation
	 * will be a 1:M relation. All datasets will subsequently be synchronised.
	 * 
	 * @param mart
	 *            the mart the keys live in. The new relation will be marked as
	 *            {@link ComponentStatus#HANDMADE}.
	 * @param from
	 *            the first key of the relation (the 1 end of a 1:M relation).
	 * @param to
	 *            the second key of the relation (the M end of a 1:M relation).
	 * @throws AssociationException
	 *             if the relation could not be established.
	 */
	public static void createRelation(final Mart mart, final Key from,
			final Key to) throws AssociationException {
		Log.info(Resources.get("logReqRelation"));

		// Create the relation.
		final Relation r = new GenericRelation(from, to, Cardinality.MANY);
		r.setStatus(ComponentStatus.HANDMADE);
		from.addRelation(r);
		to.addRelation(r);

		// Synchronise the datasets.
		mart.synchroniseDataSets();
	}

	/**
	 * Attempts to alter the columns of the given key so that they match the set
	 * of columns provided, in order. The altered key will have a status of
	 * {@link ComponentStatus#HANDMADE}. All datasets will be synchronised
	 * afterwards as they may have changed.
	 * 
	 * @param mart
	 *            the mart the key lives in.
	 * @param key
	 *            the key to alter.
	 * @param columns
	 *            the new columns to give the key.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void editKeyColumns(final Mart mart, final Key key,
			final List columns) throws AssociationException {
		Log.info(Resources.get("logReqEditKey"));
		key.setColumns(columns);
		MartBuilderUtils.changeKeyStatus(mart, key, ComponentStatus.HANDMADE);
		mart.synchroniseDataSets();
	}

	/**
	 * Turns invisibility on in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to enable invisibility in.
	 */
	public static void invisibleDataSet(final DataSet dataset) {
		Log.info(Resources.get("logReqInvisibleDataset"));
		dataset.setInvisible(true);
	}

	/**
	 * Masks a dataset table within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to mask the column in.
	 * @param table
	 *            the table to mask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskTable(final DataSet dataset, final DataSetTable table)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqMaskTable"));
		dataset.getDataSetModifications().setMaskedTable(table);
		dataset.synchronise();
	}

	/**
	 * Unmasks a dataset table within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to mask the column in.
	 * @param table
	 *            the table to unmask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskTable(final DataSet dataset,
			final DataSetTable table) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnmaskTable"));
		dataset.getDataSetModifications().unsetMaskedTable(table);
		dataset.synchronise();
	}

	/**
	 * Non-inherits all column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to uninherit the column in.
	 * @param table
	 *            the table to uninherit.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void nonInheritAllColumns(final DataSet dataset,
			final DataSetTable table) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqNonInheritColumn"));
		for (final Iterator i = table.getColumns().iterator(); i.hasNext();) {
			final DataSetColumn column = (DataSetColumn) i.next();
			try {
				dataset.getDataSetModifications().setNonInheritedColumn(column);
			} catch (ValidationException e) {
				// We don't care.
			}
		}
		dataset.synchronise();
	}

	/**
	 * Non-inherits a column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to uninherit the column in.
	 * @param column
	 *            the column to uninherit.
	 * @throws ValidationException
	 *             if the column is not uninheritable.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void nonInheritColumn(final DataSet dataset,
			final DataSetColumn column) throws ValidationException,
			SQLException, DataModelException {
		Log.info(Resources.get("logReqNonInheritColumn"));
		dataset.getDataSetModifications().setNonInheritedColumn(column);
		dataset.synchronise();
	}

	/**
	 * Un-non-inherits a column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to un-uninherit the column in.
	 * @param column
	 *            the column to un-uninherit.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unNonInheritColumn(final DataSet dataset,
			final DataSetColumn column) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnNonInheritColumn"));
		dataset.getDataSetModifications().unsetNonInheritedColumn(column);
		dataset.synchronise();
	}

	/**
	 * Un-Non-inherits all column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to uninherit the column in.
	 * @param table
	 *            the table to uninherit.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unNonInheritAllColumns(final DataSet dataset,
			final DataSetTable table) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqNonInheritColumn"));
		for (final Iterator i = table.getColumns().iterator(); i.hasNext();)
			dataset.getDataSetModifications().unsetNonInheritedColumn(
					(DataSetColumn) i.next());
		dataset.synchronise();
	}

	/**
	 * Indexes a column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to index the column in.
	 * @param column
	 *            the column to index.
	 */
	public static void indexColumn(final DataSet dataset,
			final DataSetColumn column) {
		Log.info(Resources.get("logReqIndexColumn"));
		dataset.getDataSetModifications().setIndexedColumn(column);
	}

	/**
	 * Masks a column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to mask the column in.
	 * @param column
	 *            the column to mask.
	 * @throws ValidationException
	 *             if the column is not maskable.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskColumn(final DataSet dataset,
			final DataSetColumn column) throws ValidationException,
			SQLException, DataModelException {
		Log.info(Resources.get("logReqMaskColumn"));
		dataset.getDataSetModifications().setMaskedColumn(column);
		dataset.synchronise();
	}

	/**
	 * Masks a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to mask the relation in.
	 * @param relation
	 *            the relation to mask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskRelation(final DataSetTable datasetTable,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqMaskRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setMaskedRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Merges a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to merge the relation in.
	 * @param relation
	 *            the relation to merge.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void mergeRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqMergeRelation"));
		dataset.getSchemaModifications().setMergedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Unmerges a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to unmerge the relation in.
	 * @param relation
	 *            the relation to unmerge.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmergeRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnmergeRelation"));
		dataset.getSchemaModifications().unsetMergedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Uncompounds a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to mask the relation in.
	 * @param relation
	 *            the relation to mask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void uncompoundRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUncompoundRelation"));
		dataset.getSchemaModifications().unsetCompoundRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Uncompounds a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to mask the relation in.
	 * @param relation
	 *            the relation to mask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if it could not be uncompounded.
	 */
	public static void uncompoundRelation(final DataSetTable datasetTable,
			final Relation relation) throws SQLException, DataModelException,
			ValidationException {
		Log.info(Resources.get("logReqUncompoundRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.unsetCompoundRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Compounds a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to mask the relation in.
	 * @param n
	 *            the compound arity.
	 * @param relation
	 *            the relation to mask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void compoundRelation(final DataSet dataset,
			final Relation relation, final int n) throws SQLException,
			DataModelException {
		Log.info(Resources.get("logReqCompoundRelation"));
		dataset.getSchemaModifications().setCompoundRelation(relation, n);
		dataset.synchronise();
	}

	/**
	 * Compounds a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to mask the relation in.
	 * @param relation
	 *            the relation to mask.
	 * @param n
	 *            the compound arity.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void compoundRelation(final DataSetTable datasetTable,
			final Relation relation, final int n) throws SQLException,
			DataModelException {
		Log.info(Resources.get("logReqCompoundRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setCompoundRelation(datasetTable, relation, n);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Masks a relation within a dataset. The dataset is regenerated afterwards.
	 * 
	 * @param dataset
	 *            the dataset to mask the relation in.
	 * @param relation
	 *            the relation to mask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqMaskRelation"));
		dataset.getSchemaModifications().setMaskedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Forces a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to force the relation in.
	 * @param relation
	 *            the relation to force.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void forceRelation(final DataSetTable datasetTable,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqForceRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setForceIncludeRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Forces a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to mask the relation in.
	 * @param relation
	 *            the relation to fprce.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void forceRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqForceRelation"));
		dataset.getSchemaModifications().setForceIncludeRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Masks all relations on a table within a dataset table. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to mask the table in.
	 * @param table
	 *            the table to mask all relations for.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskTable(final DataSetTable datasetTable,
			final Table table) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqMaskTable"));
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();)
			((DataSet) datasetTable.getSchema()).getSchemaModifications()
					.setMaskedRelation(datasetTable, (Relation) i.next());
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Masks all relations on a table within a dataset. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param dataset
	 *            the dataset to mask the table in.
	 * @param table
	 *            the table to mask all relations for.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskTable(final DataSet dataset, final Table table)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqMaskTable"));
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();)
			dataset.getSchemaModifications().setMaskedRelation(
					(Relation) i.next());
		dataset.synchronise();
	}

	/**
	 * This method asks to modify a concat column.
	 * 
	 * @param dsTable
	 *            the table the concat relation is associated with.
	 * @param rel
	 *            the relation to concat.
	 * @param index
	 *            the index of the relation to concat.
	 * @param column
	 *            the name to give the concat column.
	 * @param aliases
	 *            the map of columns on the table to labels in the expression.
	 * @param expression
	 *            the expression for the column.
	 * @param rowSep
	 *            the separator to use between concated rows.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 * @throws ValidationException
	 *             if something went wrong.
	 */
	public static void concatRelation(final DataSetTable dsTable,
			final Relation rel, final int index, final String column,
			final Map aliases, final String expression, final String rowSep,
			final RecursionType recursionType, final Key recursionKey,
			final Relation firstRelation, final Relation secondRelation,
			final String concSep)
			throws SQLException, DataModelException, ValidationException {
		Log.info(Resources.get("logReqConcatRelation"));
		final ConcatRelationDefinition expr = new ConcatRelationDefinition(
				expression, aliases, rowSep, column,
				recursionType, recursionKey, firstRelation, secondRelation, concSep);
		((DataSet) dsTable.getSchema()).getSchemaModifications()
				.setConcatRelation(dsTable, rel, index, expr);
		((DataSet) dsTable.getSchema()).synchronise();
	}

	/**
	 * This method asks to modify an expression column.
	 * 
	 * @param ds
	 *            the dataset the concat relation is associated with.
	 * @param rel
	 *            the relation to concat.
	 * @param index
	 *            the index of the relation to concat.
	 * @param column
	 *            the name to give the concat column.
	 * @param aliases
	 *            the map of columns on the table to labels in the expression.
	 * @param expression
	 *            the expression for the column.
	 * @param rowSep
	 *            the separator to use between concated rows.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 * @throws ValidationException
	 *             if something went wrong.
	 */
	public static void concatRelation(final DataSet ds, final Relation rel,
			final int index, final String column, final Map aliases,
			final String expression, final String rowSep,
			final RecursionType recursionType, final Key recursionKey,
			final Relation firstRelation, final Relation secondRelation,
			final String concSep) throws SQLException,
			DataModelException, ValidationException {
		Log.info(Resources.get("logReqConcatRelation"));
		final ConcatRelationDefinition expr = new ConcatRelationDefinition(
				expression, aliases, rowSep, column,
				recursionType, recursionKey, firstRelation, secondRelation, concSep);
		ds.getSchemaModifications().setConcatRelation(rel, index, expr);
		ds.synchronise();
	}

	/**
	 * This method asks to modify an expression column.
	 * 
	 * @param dsTable
	 *            the table the expression is part of.
	 * @param def
	 *            the column to modify.
	 * @param aliases
	 *            the map of columns on the table to labels in the expression.
	 * @param expression
	 *            the expression for the column.
	 * @param groupBy
	 *            whether this column requires a group-by statement. If it does,
	 *            the group-by columns required will be worked out
	 *            automatically.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 */
	public static void setExpressionColumn(final DataSetTable dsTable,
			final ExpressionColumnDefinition def, final Map aliases,
			final String expression, final boolean groupBy)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqChangeExprCol"));
		((DataSet) dsTable.getSchema()).getDataSetModifications()
				.unsetExpressionColumn(dsTable, def);
		final ExpressionColumnDefinition expr = new ExpressionColumnDefinition(
				expression, aliases, groupBy, def == null ? ((DataSet) dsTable
						.getSchema()).getDataSetModifications()
						.nextExpressionColumn() : def.getColKey());
		((DataSet) dsTable.getSchema()).getDataSetModifications()
				.setExpressionColumn(dsTable, expr);
		((DataSet) dsTable.getSchema()).synchronise();
	}

	/**
	 * Asks a dataset to partition tables by the values in the specified column.
	 * 
	 * @param dataset
	 *            the dataset to turn partitioning on for.
	 * @param column
	 *            the column to partition on.
	 * @param type
	 *            the type of partitioning to use for this column.
	 * @throws ValidationException
	 *             if the column could not be used for partitioning, for
	 *             whatever reason.
	 */
	public static void partitionByColumn(final DataSet dataset,
			final DataSetColumn column, final PartitionedColumnDefinition type)
			throws ValidationException {
		Log.info(Resources.get("logReqPartitionCol"));
		dataset.getDataSetModifications().setPartitionedColumn(column, type);
	}

	/**
	 * Drops a dataset from a mart. Normally you'd expect this kind of routine
	 * to throw {@link AssociationException}s, but not here, because we don't
	 * care if the dataset never existed in the first place.
	 * 
	 * @param mart
	 *            the mart to remove the dataset from.
	 * @param dataset
	 *            the dataset to drop.
	 */
	public static void removeDataSetFromMart(final Mart mart,
			final DataSet dataset) {
		Log.info(Resources.get("logReqRemoveDSFromMart"));
		mart.removeDataSet(dataset);
	}

	/**
	 * This method asks to remove a particular expression column.
	 * 
	 * @param ds
	 *            the dataset to remove the concat from.
	 * @param relation
	 *            the relation to unconcat.
	 * @param index
	 *            the index of the relation to unconcat.
	 * @throws ValidationException
	 *             if something went wrong.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 */
	public static void unconcatRelation(final DataSet ds,
			final Relation relation, final int index)
			throws ValidationException, SQLException, DataModelException {
		Log.info(Resources.get("logReqUnconcatRelation"));
		ds.getSchemaModifications().unsetConcatRelation(relation, index);
		ds.synchronise();
	}

	/**
	 * This method asks to remove a particular expression column.
	 * 
	 * @param dsTable
	 *            the table to remove the concat from.
	 * @param relation
	 *            the relation to unconcat.
	 * @param index
	 *            the index of the relation to unconcat.
	 * @throws ValidationException
	 *             if something went wrong.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 */
	public static void unconcatRelation(final DataSetTable dsTable,
			final Relation relation, final int index)
			throws ValidationException, SQLException, DataModelException {
		Log.info(Resources.get("logReqUnconcatRelation"));
		((DataSet) dsTable.getSchema()).getSchemaModifications()
				.unsetConcatRelation(dsTable, relation, index);
		((DataSet) dsTable.getSchema()).synchronise();
	}

	/**
	 * This method asks to remove a particular expression column.
	 * 
	 * @param dsTable
	 *            the table to remove the concat from.
	 * @param column
	 *            the expression to remove.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 */
	public static void removeExpressionColumn(final DataSetTable dsTable,
			final ExpressionColumnDefinition column) throws SQLException,
			DataModelException {
		Log.info(Resources.get("logReqRemoveExprCol"));
		((DataSet) dsTable.getSchema()).getDataSetModifications()
				.unsetExpressionColumn(dsTable, column);
		((DataSet) dsTable.getSchema()).synchronise();
	}

	/**
	 * Removes a key. All datasets will subsequently be synchronised.
	 * 
	 * @param mart
	 *            the mart to remove the key from.
	 * @param key
	 *            the key to remove.
	 */
	public static void removeKey(final Mart mart, final Key key) {
		Log.info(Resources.get("logReqRemoveKey"));
		key.destroy();
		mart.synchroniseDataSets();
	}

	/**
	 * Removes a relation from a mart. This doesn't care if it is handmade or
	 * not, it removes it anyway, so use with caution. It synchronises all
	 * datasets afterwards.
	 * 
	 * @param mart
	 *            the mart to remove the relation from.
	 * @param relation
	 *            the relation to remove.
	 */
	public static void removeRelation(final Mart mart, final Relation relation) {
		Log.info(Resources.get("logReqRemoveRelation"));
		relation.destroy();
		mart.synchroniseDataSets();
	}

	/**
	 * Removes a schema from a mart. It doesn't throw any
	 * {@link AssociationException} as we don't care if the schema never existed
	 * in the first place.
	 * 
	 * @param mart
	 *            the mart to remove the schema from.
	 * @param schema
	 *            the schema to remove from the mart.
	 */
	public static void removeSchemaFromMart(final Mart mart, final Schema schema) {
		Log.info(Resources.get("logReqRemoveSchemaFromMart"));
		mart.removeSchema(schema);
	}

	/**
	 * Renames a dataset within a mart. The new name is checked to see if it
	 * already exists, in which case an exception will be thrown.
	 * 
	 * @param mart
	 *            the mart to rename the dataset in.
	 * @param dataset
	 *            the dataset to rename.
	 * @param newName
	 *            the new name to give the dataset. If it is the same as the old
	 *            name, no action is taken.
	 */
	public static void renameDataSet(final Mart mart, final DataSet dataset,
			final String newName) {
		Log.info(Resources.get("logReqRenameDataset"));
		mart.renameDataSet(dataset, newName);
	}

	/**
	 * Renames a column within a dataset table. If the column is in a key, then
	 * also renames the columns in keys in other tables which correspond to it.
	 * 
	 * @param col
	 *            the column to rename.
	 * @param newName
	 *            the new name to give it. If the name is the same as the
	 *            existing one, no action is taken.
	 * @throws ValidationException
	 *             if the column is not allowed to be renamed.
	 */
	public static void renameDataSetColumn(final DataSetColumn col,
			final String newName) throws ValidationException, SQLException,
			DataModelException {
		Log.info(Resources.get("logReqRenameDSColumn"));
		((DataSet) col.getTable().getSchema()).getDataSetModifications()
				.setColumnRename(col, newName);
		((DataSet) col.getTable().getSchema()).synchronise();
	}

	/**
	 * Renames a table within a dataset.
	 * 
	 * @param tbl
	 *            the table to rename.
	 * @param newName
	 *            the new name to give it. If the name is the same as the
	 *            existing one, no action is taken.
	 */
	public static void renameDataSetTable(final DataSetTable tbl,
			final String newName) {
		Log.info(Resources.get("logReqRenameDSTable"));
		((DataSet) tbl.getSchema()).getDataSetModifications().setTableRename(
				tbl, newName);
	}

	/**
	 * Renames a schema within a mart.
	 * 
	 * @param mart
	 *            the mart to rename the schema in.
	 * @param schema
	 *            the schema to rename.
	 * @param newName
	 *            the new name to give the schema.
	 */
	public static void renameSchema(final Mart mart, final Schema schema,
			final String newName) {
		Log.info(Resources.get("logReqRenameSchema"));
		mart.renameSchema(schema, newName);
	}

	/**
	 * Creates an exact copy of a dataset within the mart, giving it the name
	 * specified. All tables, keys and relations will be copied over, and have
	 * the same statuses. Masked columns, partitions etc. will also be copied.
	 * 
	 * @param mart
	 *            the mart to create the copy of the dataset in.
	 * @param dataset
	 *            the dataset to copy.
	 * @param newName
	 *            the name to give the copy of the dataset.
	 * @return the copy of the dataset.
	 */
	public static DataSet replicateDataSet(final Mart mart,
			final DataSet dataset, final String newName) {
		Log.info(Resources.get("logReqReplicateDataset"));
		final DataSet newDataSet = (DataSet) dataset.replicate(newName);
		return newDataSet;
	}

	/**
	 * Creates an exact copy of a schema within the mart, giving it the name
	 * specified. All tables, keys and relations will be copied over, and have
	 * the same statuses. However, relations that are
	 * {@link ComponentStatus#INFERRED_INCORRECT} and have incompatible PKs and
	 * FKs will <u>not</u> be copied.
	 * 
	 * @param mart
	 *            the mart to create the copy of the schema in.
	 * @param schema
	 *            the schema to copy.
	 * @param newName
	 *            the name to give the copy of the schema.
	 * @return the copy of the schema.
	 */
	public static Schema replicateSchema(final Mart mart, final Schema schema,
			final String newName) {
		Log.info(Resources.get("logReqReplicateSchema"));
		final Schema newSchema = schema.replicate(newName);
		mart.addSchema(newSchema);
		return newSchema;
	}

	/**
	 * Flags a table as restricted within a dataset table. If already flagged,
	 * this updates the existing settings.
	 * 
	 * @param datasetTable
	 *            the dataset table to flag the relation in.
	 * @param table
	 *            the table to flag as restricted.
	 * @param expression
	 *            the expression to use for the restriction for the relation.
	 * @param aliases
	 *            the aliases to use for columns.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void restrictTable(final DataSetTable datasetTable,
			final Table table, final String expression, final Map aliases,
			final boolean hard)
			throws ValidationException {
		Log.info(Resources.get("logReqRestrictTable"));
		final RestrictedTableDefinition restriction = new RestrictedTableDefinition(
				expression, aliases, hard);
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setRestrictedTable(datasetTable, table, restriction);
	}

	/**
	 * Flags a table as restricted within a dataset. If already flagged, this
	 * updates the existing settings.
	 * 
	 * @param dataset
	 *            the dataset to flag the relation in.
	 * @param table
	 *            the table to flag as restricted.
	 * @param expression
	 *            the expression to use for the restriction for the relation.
	 * @param aliases
	 *            the aliases to use for columns.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void restrictTable(final DataSet dataset, final Table table,
			final String expression, final Map aliases, final boolean hard)
			throws ValidationException {
		Log.info(Resources.get("logReqRestrictTable"));
		final RestrictedTableDefinition restriction = new RestrictedTableDefinition(
				expression, aliases, hard);
		dataset.getSchemaModifications().setRestrictedTable(table, restriction);
	}

	/**
	 * UnFlags a table as restricted within a dataset table. If already flagged,
	 * this updates the existing settings.
	 * 
	 * @param datasetTable
	 *            the dataset table to unflag the relation in.
	 * @param table
	 *            the table to unflag as restricted.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void unrestrictTable(final DataSetTable datasetTable,
			final Table table) throws ValidationException {
		Log.info(Resources.get("logReqUnrestrictTable"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.unsetRestrictedTable(datasetTable, table);
	}

	/**
	 * UnFlags a table as restricted within a dataset. If already flagged, this
	 * updates the existing settings.
	 * 
	 * @param dataset
	 *            the dataset to unflag the relation in.
	 * @param table
	 *            the table to unflag as restricted.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void unrestrictTable(final DataSet dataset, final Table table)
			throws ValidationException {
		Log.info(Resources.get("logReqUnrestrictTable"));
		dataset.getSchemaModifications().unsetRestrictedTable(table);
	}

	/**
	 * Flags a table as restricted within a dataset table. If already flagged,
	 * this updates the existing settings.
	 * 
	 * @param datasetTable
	 *            the dataset table to flag the relation in.
	 * @param relation
	 *            the relation to flag as restricted.
	 * @param index
	 *            the index of the restricted relation.
	 * @param expression
	 *            the expression to use for the restriction for the relation.
	 * @param lhsAliases
	 *            the aliases to use for columns on the from end.
	 * @param rhsAliases
	 *            the aliases to use for columns on the to end.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void restrictRelation(final DataSetTable datasetTable,
			final Relation relation, final int index, final String expression,
			final Map lhsAliases, final Map rhsAliases, final boolean hard)
			throws ValidationException {
		Log.info(Resources.get("logReqRestrictRelation"));
		final RestrictedRelationDefinition restriction = new RestrictedRelationDefinition(
				expression, lhsAliases, rhsAliases, hard);
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setRestrictedRelation(datasetTable, relation, index,
						restriction);
	}

	/**
	 * Flags a table as restricted within a dataset. If already flagged, this
	 * updates the existing settings.
	 * 
	 * @param dataset
	 *            the dataset to flag the relation in.
	 * @param relation
	 *            the relation to flag as restricted.
	 * @param index
	 *            the index of the restricted relation.
	 * @param expression
	 *            the expression to use for the restriction for the relation.
	 * @param lhsAliases
	 *            the aliases to use for columns on the from end.
	 * @param rhsAliases
	 *            the aliases to use for columns on the to end.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void restrictRelation(final DataSet dataset,
			final Relation relation, final int index, final String expression,
			final Map lhsAliases, final Map rhsAliases, final boolean hard)
			throws ValidationException {
		Log.info(Resources.get("logReqRestrictRelation"));
		final RestrictedRelationDefinition restriction = new RestrictedRelationDefinition(
				expression, lhsAliases, rhsAliases, hard);
		dataset.getSchemaModifications().setRestrictedRelation(relation, index,
				restriction);
	}

	/**
	 * UnFlags a table as restricted within a dataset table. If already flagged,
	 * this updates the existing settings.
	 * 
	 * @param datasetTable
	 *            the dataset table to unflag the relation in.
	 * @param relation
	 *            the relation to unflag as restricted.
	 * @param index
	 *            the index of the relation to unrestrict.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void unrestrictRelation(final DataSetTable datasetTable,
			final Relation relation, final int index)
			throws ValidationException {
		Log.info(Resources.get("logReqUnrestrictRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.unsetRestrictedRelation(datasetTable, relation, index);
	}

	/**
	 * UnFlags a table as restricted within a dataset. If already flagged, this
	 * updates the existing settings.
	 * 
	 * @param dataset
	 *            the dataset to unflag the relation in.
	 * @param relation
	 *            the relation to unflag as restricted.
	 * @param index
	 *            the index of the relation to unrestrict.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void unrestrictRelation(final DataSet dataset,
			final Relation relation, final int index)
			throws ValidationException {
		Log.info(Resources.get("logReqUnrestrictRelation"));
		dataset.getSchemaModifications().unsetRestrictedRelation(relation,
				index);
	}

	/**
	 * This method returns the first few rows from a given offset in the given
	 * table. The results are a nested list, where each item in the main list is
	 * another list containing all the column values in the same order as they
	 * appear in the method {@link Table#getColumns()}.
	 * <p>
	 * If the schema is a grouped schema, this method will return <i>n*m</i>
	 * rows, where <i>n</i> is the number of rows requested and <i>m</i> is
	 * the number of schemas in the group.
	 * <p>
	 * If any schema is unrecognised by
	 * {@link DatabaseDialect#getDialect(DataLink)} then no rows are returned
	 * for that schema.
	 * 
	 * @param table
	 *            the table to get the rows for.
	 * @param offset
	 *            the offset to start at.
	 * @param count
	 *            the number of rows to get.
	 * @return the rows.
	 * @throws SQLException
	 *             if anything goes wrong whilst fetching the rows.
	 */
	public static Collection selectRows(final Table table, final int offset,
			final int count) throws SQLException {
		Log.info(Resources.get("logReqSelectRows"));
		final Schema schema = table.getSchema();
		final Collection results = new ArrayList();
		final DatabaseDialect dd = DatabaseDialect.getDialect(schema);
		if (dd != null)
			results.addAll(dd.executeSelectRows(table, offset, count));
		return results;
	}

	/**
	 * Flags a relation as subclassed within a dataset. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param dataset
	 *            the dataset to flag the relation within.
	 * @param relation
	 *            the subclassed relation.
	 * @throws ValidationException
	 *             if the relation is not permitted to be a subclass relation in
	 *             this dataset, for whatever reason.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void subclassRelation(final DataSet dataset,
			final Relation relation) throws ValidationException, SQLException,
			DataModelException {
		Log.info(Resources.get("logReqSubclassRel"));
		dataset.getSchemaModifications().setSubclassedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Given a particular set of tables, this method suggests a bunch of
	 * datasets which could be sensibly created. Each suggestion is synchronised
	 * before being returned.
	 * 
	 * @param mart
	 *            the mart to create the datasets in.
	 * @param tables
	 *            the tables to include in the set of suggested datasets.
	 * @return the set of datasets, which will always have at least one member.
	 * @throws AssociationException
	 *             if the specified table is not part of the mart given.
	 * @throws SQLException
	 *             if there is any trouble communicating with the data source or
	 *             database during schema synchronisation.
	 * @throws DataModelException
	 *             if synchronisation fails.
	 */
	public static Collection suggestDataSets(final Mart mart,
			final Collection tables) throws SQLException, AssociationException,
			DataModelException {
		Log.info(Resources.get("logReqSuggestDatasets"));
		return mart.suggestDataSets(tables);
	}

	/**
	 * Given a dataset and a set of columns from one table upon which the main
	 * table of that dataset is based, find all other tables which have similar
	 * columns, and create a new dataset for each one.
	 * <p>
	 * This method will not create datasets around tables which have already
	 * been used as the underlying table in any dataset table in the existing
	 * dataset. Neither will it create a dataset around the table from which the
	 * original columns came.
	 * <p>
	 * There may be no datasets resulting from this, if the columns do not
	 * appear elsewhere.
	 * 
	 * @param mart
	 *            the mart to create the datasets in.
	 * @param dataset
	 *            the dataset the columns were selected from.
	 * @param columns
	 *            the columns to search across.
	 * @return the resulting set of datasets.
	 * @throws AssociationException
	 *             if the specified table is not part of the mart given.
	 * @throws SQLException
	 *             if there is any trouble communicating with the data source or
	 *             database during schema synchronisation.
	 * @throws DataModelException
	 *             if synchronisation fails.
	 */
	public static Collection suggestInvisibleDataSets(final Mart mart,
			final DataSet dataset, final Collection columns)
			throws AssociationException, DataModelException, SQLException {
		Log.info(Resources.get("logReqSuggestInvDatasets"));
		return mart.suggestInvisibleDataSets(dataset, columns);
	}

	/**
	 * This method asks the mart to synchronise all its datasets against the
	 * schemas they are formed from.
	 * 
	 * @param mart
	 *            the mart you wish to synchronise all the datasets in.
	 */
	public static void synchroniseMartDataSets(final Mart mart) {
		Log.info(Resources.get("logReqSyncMartDS"));
		mart.synchroniseDataSets();
	}

	/**
	 * This method asks the mart to synchronise all its schemas against the data
	 * sources or databases they represent.
	 * 
	 * @param mart
	 *            the mart you wish to synchronise all the schemas in.
	 * @throws SQLException
	 *             if any of them have problems communicating with their data
	 *             sources or databases.
	 * @throws DataModelException
	 *             if any of them find logical problems during the
	 *             synchronisation process.
	 */
	public static void synchroniseMartSchemas(final Mart mart)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqSyncMartSchema"));
		mart.synchroniseSchemas();
	}

	/**
	 * Unindexes a column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unindex the column in.
	 * @param column
	 *            the column to unindex.
	 */
	public static void unindexColumn(final DataSet dataset,
			final DataSetColumn column) {
		Log.info(Resources.get("logReqUnindexColumn"));
		dataset.getDataSetModifications().unsetIndexedColumn(column);
	}

	/**
	 * Unmasks a column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unmask the column in.
	 * @param column
	 *            the column to unmask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskColumn(final DataSet dataset,
			final DataSetColumn column) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnmaskColumn"));
		dataset.getDataSetModifications().unsetMaskedColumn(column);
		dataset.synchronise();
	}

	/**
	 * Unmasks all relations on a table within a dataset table. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to unmask the table in.
	 * @param table
	 *            the table to unmask all relations for.
	 * @throws ValidationException
	 *             if the table could not be unmasked.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskTable(final DataSetTable datasetTable,
			final Table table) throws ValidationException, SQLException,
			DataModelException {
		Log.info(Resources.get("logReqUnmaskTable"));
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();)
			((DataSet) datasetTable.getSchema()).getSchemaModifications()
					.unsetMaskedRelation(datasetTable, (Relation) i.next());
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Unmasks all relations on a table within a dataset. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param dataset
	 *            the dataset to unmask the table in.
	 * @param table
	 *            the table to unmask all relations for.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskTable(final DataSet dataset, final Table table)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnmaskTable"));
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();)
			dataset.getSchemaModifications().unsetMaskedRelation(
					(Relation) i.next());
		dataset.synchronise();
	}

	/**
	 * Unmasks a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to unmask the relation in.
	 * @param relation
	 *            the relation to unmask.
	 * @throws ValidationException
	 *             if the relation could not be unmasked.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskRelation(final DataSetTable datasetTable,
			final Relation relation) throws ValidationException, SQLException,
			DataModelException {
		Log.info(Resources.get("logReqUnmaskRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.unsetMaskedRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Unmasks a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to unmask the relation in.
	 * @param relation
	 *            the relation to unmask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnmaskRelation"));
		dataset.getSchemaModifications().unsetMaskedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Unforces a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to unforce the relation in.
	 * @param relation
	 *            the relation to unforce.
	 * @throws ValidationException
	 *             if the relation could not be unforced.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unforceRelation(final DataSetTable datasetTable,
			final Relation relation) throws ValidationException, SQLException,
			DataModelException {
		Log.info(Resources.get("logReqUnforceRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.unsetForceIncludeRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Unforces a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to unforce the relation in.
	 * @param relation
	 *            the relation to unforce.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unforceRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnforceRelation"));
		dataset.getSchemaModifications().unsetForceIncludeRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Turns off partitioning on a given dataset column.
	 * 
	 * @param dataset
	 *            the dataset to turn off partitioning for on this column.
	 * @param table
	 *            the table to turn off partitioning for.
	 * @throws ValidationException
	 *             if the column could not be used for partitioning, for
	 *             whatever reason.
	 */
	public static void unpartitionByColumn(final DataSet dataset,
			final DataSetTable table) throws ValidationException {
		Log.info(Resources.get("logReqUnpartitionCol"));
		dataset.getDataSetModifications().unsetPartitionedColumn(table);
	}

	/**
	 * Unflags a relation as subclassed, and regenerates the dataset.
	 * 
	 * @param dataset
	 *            the dataset to unflag the relation in.
	 * @param relation
	 *            the relation to unflag.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unsubclassRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUnsubclassRel"));
		dataset.getSchemaModifications().unsetSubclassedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Turns invisibility off in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to disable invisibility in.
	 */
	public static void visibleDataSet(final DataSet dataset) {
		Log.info(Resources.get("logReqVisibleDataset"));
		dataset.setInvisible(false);
	}

	/**
	 * Turns index optimiser off in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to disable index optimiser in.
	 */
	public static void noIndexOptimiserDataSet(final DataSet dataset) {
		Log.info(Resources.get("logReqNoIndOptDataset"));
		dataset.setIndexOptimiser(false);
	}

	/**
	 * Turns index optimiser on in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to enable index optimiser in.
	 */
	public static void indexOptimiserDataSet(final DataSet dataset) {
		Log.info(Resources.get("logReqIndOptDataset"));
		dataset.setIndexOptimiser(true);
	}

	/**
	 * The tools are static and not intended to be instantiated.
	 */
	private MartBuilderUtils() {
	}
}
