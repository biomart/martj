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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.biomart.builder.controller.dialects.DatabaseDialect;
import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueList;
import org.biomart.builder.model.SchemaModificationSet.CompoundRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedTableDefinition;
import org.biomart.common.controller.CommonUtils;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.Column;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.DataLink;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Relation.Cardinality;
import org.biomart.common.model.Relation.GenericRelation;
import org.biomart.common.model.Schema.JDBCSchema;
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
 * @since 0.6
 */
public class MartBuilderUtils {
	/**
	 * Attempts to create a foreign key on a table given a set of columns. The
	 * new key will have a status of {@link ComponentStatus#HANDMADE}.
	 * 
	 * @param mart
	 *            the mart we are working with.
	 * @param table
	 *            the table to create the key on.
	 * @param columns
	 *            the colums, in order, to create the key over.
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void createForeignKey(final Mart mart, final Table table,
			final List columns) throws SQLException, DataModelException,
			AssociationException {
		CommonUtils.createForeignKey(table, columns);
		mart.synchroniseDataSets(table.getSchema());
	}

	/**
	 * Attempts to create a primary key on a table given a set of columns. If
	 * the table already has a primary key, then this one will replace it. The
	 * new key will have a status of {@link ComponentStatus#HANDMADE}.
	 * 
	 * @param mart
	 *            the mart we are working with.
	 * @param table
	 *            the table to create the key on.
	 * @param columns
	 *            the colums, in order, to create the key over.
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void createPrimaryKey(final Mart mart, final Table table,
			final List columns) throws SQLException, DataModelException,
			AssociationException {
		CommonUtils.createPrimaryKey(table, columns);
		mart.synchroniseDataSets(table.getSchema());
	}

	/**
	 * Turns key-guessing off in a given schema.
	 * 
	 * @param mart
	 *            the mart we are working with.
	 * @param schema
	 *            the schema to disable key-guessing in.
	 * @throws SQLException
	 *             if after disabling keyguessing the key sync went wrong.
	 * @throws DataModelException
	 *             if after disabling keyguessing the key sync went wrong.
	 */
	public static void disableKeyGuessing(final Mart mart, final Schema schema)
			throws SQLException, DataModelException {
		CommonUtils.disableKeyGuessing(schema);
		mart.synchroniseDataSets(schema);
	}

	/**
	 * Turns key-guessing on in a given schema.
	 * 
	 * @param mart
	 *            the mart we are working with.
	 * @param schema
	 *            the schema to enable key-guessing in.
	 * @throws SQLException
	 *             if after keyguessing the key sync went wrong.
	 * @throws DataModelException
	 *             if after keyguessing the key sync went wrong.
	 */
	public static void enableKeyGuessing(final Mart mart, final Schema schema)
			throws DataModelException, SQLException {
		CommonUtils.enableKeyGuessing(schema);
		mart.synchroniseDataSets(schema);
	}

	/**
	 * Synchronises an individual schema against the data source or database it
	 * represents. Datasets using this schema will also be synchronised.
	 * 
	 * @param mart
	 *            the mart we are working with.
	 * @param schema
	 *            the schema to synchronise.
	 * @throws SQLException
	 *             if there was any problem communicating with the data source
	 *             or database.
	 * @throws DataModelException
	 *             if there were any logical problems with synchronisation.
	 */
	public static void synchroniseSchema(final Mart mart, final Schema schema)
			throws SQLException, DataModelException {
		CommonUtils.synchroniseSchema(schema);
		mart.synchroniseDataSets(schema);
	}

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
	 * Sets the output host on a mart.
	 * 
	 * @param mart
	 *            the mart to change.
	 * @param outputHost
	 *            the new output host value.
	 */
	public static void setOutputHost(final Mart mart, final String outputHost) {
		Log.info(Resources.get("logReqOutputHost"));
		mart.setOutputHost(outputHost);
	}

	/**
	 * Sets the output port on a mart.
	 * 
	 * @param mart
	 *            the mart to change.
	 * @param outputPort
	 *            the new output port value.
	 */
	public static void setOutputPort(final Mart mart, final String outputPort) {
		Log.info(Resources.get("logReqOutputPort"));
		mart.setOutputPort(outputPort);
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
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 */
	public static void changeKeyStatus(final Mart mart, final Key key,
			final ComponentStatus status) throws SQLException,
			DataModelException {
		Log.info(Resources.get("logReqChangeKeyStatus"));
		key.setStatus(status);
		mart.synchroniseDataSets(key.getTable().getSchema());
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
	 * for those purposes. Also this method changes it to HANDMADE.
	 * 
	 * @param mart
	 *            the mart to change the relation in.
	 * @param relation
	 *            the relation to change.
	 * @param cardinality
	 *            the new cardinality to give the relation.
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws AssociationException
	 *             if it cannot make the change.
	 */
	public static void changeRelationCardinality(final Mart mart,
			final Relation relation, final Cardinality cardinality)
			throws SQLException, DataModelException, AssociationException {
		Log.info(Resources.get("logReqChangeCardinality"));
		// Change the cardinality.
		relation.setCardinality(cardinality);
		// Change it to handmade to make it obvious.
		relation.setStatus(ComponentStatus.HANDMADE);

		// If 1:1, make sure it isn't used as a subclass relation
		// in any dataset.
		if (relation.isOneToOne())
			for (final Iterator i = mart.getDataSets().iterator(); i.hasNext();) {
				final DataSet ds = (DataSet) i.next();
				ds.getSchemaModifications().unsetSubclassedRelation(relation);
			}

		// Synchronise the datasets.
		mart.synchroniseDataSets(relation.getFirstKey().getTable().getSchema(),
				relation.getSecondKey().getTable().getSchema());
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
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws AssociationException
	 *             if the relation was previously
	 *             {@link ComponentStatus#INFERRED_INCORRECT} and the keys at
	 *             either end do not have the same number of columns as each
	 *             other, then this exception will be thrown to tell you so.
	 */
	public static void changeRelationStatus(final Mart mart,
			final Relation relation, final ComponentStatus status)
			throws SQLException, DataModelException, AssociationException {
		Log.info(Resources.get("logReqChangeRelStatus"));
		relation.setStatus(status);
		mart.synchroniseDataSets(relation.getFirstKey().getTable().getSchema(),
				relation.getSecondKey().getTable().getSchema());
	}

	/**
	 * Attempts to establish a relation between two keys in a mart. The relation
	 * will be a 1:M or M:M relation. All datasets will subsequently be
	 * synchronised.
	 * 
	 * @param mart
	 *            the mart the keys live in. The new relation will be marked as
	 *            {@link ComponentStatus#HANDMADE}.
	 * @param from
	 *            the first key of the relation.
	 * @param to
	 *            the second key of the relation.
	 * @throws AssociationException
	 *             if the relation could not be established.
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 */
	public static void createRelation(final Mart mart, final Key from,
			final Key to) throws SQLException, DataModelException,
			AssociationException {
		Log.info(Resources.get("logReqRelation"));

		// Create the relation.
		final Relation r = new GenericRelation(from, to, Cardinality.MANY);
		r.setStatus(ComponentStatus.HANDMADE);
		from.addRelation(r);
		to.addRelation(r);

		// Synchronise the datasets.
		mart.synchroniseDataSets(from.getTable().getSchema(), to.getTable()
				.getSchema());
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
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void editKeyColumns(final Mart mart, final Key key,
			final List columns) throws SQLException, DataModelException,
			AssociationException {
		Log.info(Resources.get("logReqEditKey"));
		key.setColumns(columns);
		MartBuilderUtils.changeKeyStatus(mart, key, ComponentStatus.HANDMADE);
		mart.synchroniseDataSets(key.getTable().getSchema());
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
	 * Masks a dimension within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to mask the dimension in.
	 * @param table
	 *            the dimension to mask.
	 * @throws ValidationException
	 *             if the dimension could not be masked.
	 */
	public static void maskDimension(final DataSet dataset,
			final DataSetTable table) throws ValidationException {
		Log.info(Resources.get("logReqMaskTable"));
		dataset.getDataSetModifications().setMaskedTable(table);
	}

	/**
	 * Unmasks a dimension within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unmask the dimension in.
	 * @param table
	 *            the dimension to unmask.
	 */
	public static void unmaskDimension(final DataSet dataset,
			final DataSetTable table) {
		Log.info(Resources.get("logReqUnmaskTable"));
		dataset.getDataSetModifications().unsetMaskedTable(table);
	}

	/**
	 * Distincts a table within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to distinct the table in.
	 * @param table
	 *            the table to make distinct.
	 * @throws ValidationException
	 *             if the table could not be made distinct.
	 */
	public static void distinctTable(final DataSet dataset,
			final DataSetTable table) throws ValidationException {
		Log.info(Resources.get("logReqDistinctTable"));
		dataset.getDataSetModifications().setDistinctTable(table);
	}

	/**
	 * Undistincts a table within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to undistinct the table in.
	 * @param table
	 *            the table to undistinct.
	 */
	public static void undistinctTable(final DataSet dataset,
			final DataSetTable table) {
		Log.info(Resources.get("logReqUndistinctTable"));
		dataset.getDataSetModifications().unsetDistinctTable(table);
	}

	/**
	 * Removes optimisers for a given table within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unoptimise the table in.
	 * @param dst
	 *            the table to unoptimise.
	 * @throws ValidationException
	 *             if the table could not be unoptimise.
	 */
	public static void unoptimiseTable(final DataSet dataset,
			final DataSetTable dst) throws ValidationException {
		Log.info(Resources.get("logReqUnoptimiseTable"));
		dataset.getDataSetModifications().setNoOptimiserTable(dst);
	}

	/**
	 * Re-optimises a table within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to undistinct the table in.
	 * @param dst
	 *            the table to undistinct.
	 */
	public static void reoptimiseTable(final DataSet dataset,
			final DataSetTable dst) {
		Log.info(Resources.get("logReqReoptimiseTable"));
		dataset.getDataSetModifications().unsetNoOptimiserTable(dst);
	}

	/**
	 * Non-inherits all columns within a dataset. Any that cannot be
	 * non-inherited are ignored.
	 * 
	 * @param dataset
	 *            the dataset to uninherit the columns in.
	 * @param table
	 *            the table to uninherit columns from.
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
	 * Un-Non-inherits all columns within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to uninherit the columns in.
	 * @param table
	 *            the table to uninherit columns from.
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
	 * Masks a set of columns within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to mask the column in.
	 * @param columns
	 *            the columns to mask.
	 * @throws ValidationException
	 *             if the column is not maskable.
	 */
	public static void maskColumns(final DataSet dataset,
			final Collection columns) throws ValidationException {
		Log.info(Resources.get("logReqMaskColumn"));
		for (final Iterator i = columns.iterator(); i.hasNext();)
			dataset.getDataSetModifications().setMaskedColumn(
					(DataSetColumn) i.next());
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
	 *            the dataset to uncompound the relation in.
	 * @param relation
	 *            the relation to uncompound.
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
	 *            the dataset table to uncompound the relation in.
	 * @param relation
	 *            the relation to uncompound.
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
	 * Un-directionalises a relation within a dataset. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param dataset
	 *            the dataset to un-directionalise the relation in.
	 * @param relation
	 *            the relation to un-directionalise.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void undirectionalRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqUndirectionalRelation"));
		dataset.getSchemaModifications().unsetDirectionalRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Un-directionalises a relation within a dataset table. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to un-directionalise the relation in.
	 * @param relation
	 *            the relation to un-directionalise.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if it could not be un-directionalised.
	 */
	public static void undirectionalRelation(final DataSetTable datasetTable,
			final Relation relation) throws SQLException, DataModelException,
			ValidationException {
		Log.info(Resources.get("logReqUndirectionalRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.unsetDirectionalRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Compounds a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to mask the relation in.
	 * @param def
	 *            the compound definition.
	 * @param relation
	 *            the relation to compound.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if it could not be compounded.
	 */
	public static void compoundRelation(final DataSet dataset,
			final Relation relation, final CompoundRelationDefinition def)
			throws SQLException, DataModelException, ValidationException {
		Log.info(Resources.get("logReqCompoundRelation"));
		dataset.getSchemaModifications().setCompoundRelation(relation, def);
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
	 * @param def
	 *            the compound definition.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if it could not be compounded.
	 */
	public static void compoundRelation(final DataSetTable datasetTable,
			final Relation relation, final CompoundRelationDefinition def)
			throws SQLException, DataModelException, ValidationException {
		Log.info(Resources.get("logReqCompoundRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setCompoundRelation(datasetTable, relation, def);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Directionalises a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to directionalise the relation in.
	 * @param relation
	 *            the relation to directionalise.
	 * @param def
	 *            the key to mark as the starting point of the relation.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if the directionalisation is rejected.
	 */
	public static void directionalRelation(final DataSet dataset,
			final Relation relation, final Key def) throws SQLException,
			DataModelException, ValidationException {
		Log.info(Resources.get("logReqDirectionalRelation"));
		dataset.getSchemaModifications().setDirectionalRelation(relation, def);
		dataset.synchronise();
	}

	/**
	 * Directionalises a relation within a dataset table. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to directionalise the relation in.
	 * @param relation
	 *            the relation to directionalise.
	 * @param def
	 *            the key to mark as the starting point of the relation.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if the directionalisation is rejected.
	 */
	public static void directionalRelation(final DataSetTable datasetTable,
			final Relation relation, final Key def) throws SQLException,
			DataModelException, ValidationException {
		Log.info(Resources.get("logReqDirectionalRelation"));
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setDirectionalRelation(datasetTable, relation, def);
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
	 *            the dataset to force the relation in.
	 * @param relation
	 *            the relation to force.
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
	public static void maskAllRelations(final DataSetTable datasetTable,
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
	public static void maskAllRelations(final DataSet dataset, final Table table)
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
	 * @param expr
	 *            the concat definition.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 * @throws ValidationException
	 *             if something went wrong.
	 */
	public static void concatRelation(final DataSetTable dsTable,
			final Relation rel, final int index,
			final ConcatRelationDefinition expr) throws SQLException,
			DataModelException, ValidationException {
		Log.info(Resources.get("logReqConcatRelation"));
		((DataSet) dsTable.getSchema()).getSchemaModifications()
				.setConcatRelation(dsTable, rel, index, expr);
		((DataSet) dsTable.getSchema()).synchronise();
	}

	/**
	 * This method asks to modify a concat column.
	 * 
	 * @param ds
	 *            the dataset the concat relation is associated with.
	 * @param rel
	 *            the relation to concat.
	 * @param index
	 *            the index of the relation to concat.
	 * @param expr
	 *            the concat definition.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 * @throws ValidationException
	 *             if something went wrong.
	 */
	public static void concatRelation(final DataSet ds, final Relation rel,
			final int index, final ConcatRelationDefinition expr)
			throws SQLException, DataModelException, ValidationException {
		Log.info(Resources.get("logReqConcatRelation"));
		ds.getSchemaModifications().setConcatRelation(rel, index, expr);
		ds.synchronise();
	}

	/**
	 * This method asks to modify an expression column.
	 * 
	 * @param dsTable
	 *            the table the expression is part of.
	 * @param def
	 *            the expression definition.
	 * @param aliases
	 *            the map of columns on the table to labels in the expression.
	 * @param expression
	 *            the expression for the column.
	 * @param groupBy
	 *            whether this column requires a group-by statement. If it does,
	 *            the group-by columns required will be worked out
	 *            automatically.
	 * @param optimiser
	 *            whether this column is to be used as an optimiser column
	 *            instead.
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 */
	public static void setExpressionColumn(final DataSetTable dsTable,
			final ExpressionColumnDefinition def, final Map aliases,
			final String expression, final boolean groupBy,
			final boolean optimiser) throws SQLException, DataModelException {
		Log.info(Resources.get("logReqChangeExprCol"));
		((DataSet) dsTable.getSchema()).getDataSetModifications()
				.unsetExpressionColumn(dsTable, def);
		final ExpressionColumnDefinition expr = new ExpressionColumnDefinition(
				expression, aliases, groupBy, optimiser,
				def == null ? ((DataSet) dsTable.getSchema())
						.getDataSetModifications().nextExpressionColumn() : def
						.getColKey());
		((DataSet) dsTable.getSchema()).getDataSetModifications()
				.setExpressionColumn(dsTable, expr);
		((DataSet) dsTable.getSchema()).synchronise();
	}

	/**
	 * Asks a dataset to partition tables on the specified column.
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
	 * This method asks to remove a particular concat column.
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
	 * This method asks to remove a particular concat column.
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
	 *            the table to remove the expression from.
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
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 */
	public static void removeKey(final Mart mart, final Key key)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqRemoveKey"));
		key.destroy();
		mart.synchroniseDataSets(key.getTable().getSchema());
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
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 */
	public static void removeRelation(final Mart mart, final Relation relation)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqRemoveRelation"));
		relation.destroy();
		mart.synchroniseDataSets(relation.getFirstKey().getTable().getSchema(),
				relation.getSecondKey().getTable().getSchema());
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
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 */
	public static void removeSchemaFromMart(final Mart mart, final Schema schema)
			throws SQLException, DataModelException {
		Log.info(Resources.get("logReqRemoveSchemaFromMart"));
		mart.removeSchema(schema);
	}

	/**
	 * Renames a dataset within a mart.
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
	 * @throws SQLException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
	 * @throws DataModelException
	 *             if anything goes wrong when trying to resync datasets after
	 *             the change.
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
	 *            the dataset table to flag the table in.
	 * @param table
	 *            the table to flag as restricted.
	 * @param expression
	 *            the expression to use for the restriction for the table.
	 * @param aliases
	 *            the aliases to use for columns.
	 * @param hard
	 *            if this is to be a hard restriction.
	 * @throws ValidationException
	 *             if this could not be done.
	 */
	public static void restrictTable(final DataSetTable datasetTable,
			final Table table, final String expression, final Map aliases,
			final boolean hard) throws ValidationException {
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
	 *            the dataset to flag the table in.
	 * @param table
	 *            the table to flag as restricted.
	 * @param expression
	 *            the expression to use for the restriction for the table.
	 * @param aliases
	 *            the aliases to use for columns.
	 * @param hard
	 *            if this is to be a hard restriction.
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
	 * UnFlags a table as restricted within a dataset table.
	 * 
	 * @param datasetTable
	 *            the dataset table to unflag the table in.
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
	 * UnFlags a table as restricted within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unflag the table in.
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
	 * Flags a relation as restricted within a dataset table. If already
	 * flagged, this updates the existing settings.
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
	 * @param hard
	 *            if this is to be a hard restriction.
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
	 * Flags a relation as restricted within a dataset. If already flagged, this
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
	 * @param hard
	 *            if this is to be a hard restriction.
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
	 * UnFlags a relation as restricted within a dataset table.
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
	 * UnFlags a relation as restricted within a dataset.
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
	 * If the schema is unrecognised by
	 * {@link DatabaseDialect#getDialect(DataLink)} then no rows are returned.
	 * 
	 * @param table
	 *            the table to get the rows for.
	 * @param offset
	 *            the offset to start at, zero-indexed.
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
	 * @throws SQLException
	 *             if syncing failed.
	 * @throws DataModelException
	 *             if syncing failed.
	 */
	public static void synchroniseMartDataSets(final Mart mart)
			throws SQLException, DataModelException {
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
	 * Unmasks a group of columns within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unmask the column in.
	 * @param columns
	 *            the columns to unmask.
	 */
	public static void unmaskColumns(final DataSet dataset,
			final Collection columns) {
		Log.info(Resources.get("logReqUnmaskColumn"));
		for (final Iterator i = columns.iterator(); i.hasNext();)
			dataset.getDataSetModifications().unsetMaskedColumn(
					(DataSetColumn) i.next());
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
	public static void unmaskAllRelations(final DataSetTable datasetTable,
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
	public static void unmaskAllRelations(final DataSet dataset,
			final Table table) throws SQLException, DataModelException {
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
	 * Turns off partitioning on a given dataset table.
	 * 
	 * @param dataset
	 *            the dataset to turn off partitioning for on this table.
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
	 * Works out which datasets reference the given schema, then updates any
	 * list-based partition values in those datasets to reflect the correct set
	 * of distinct values.
	 * 
	 * @param mart
	 *            the mart we are working in.
	 * @param schema
	 *            the schema that recently changed.
	 * @throws SQLException
	 *             if it could not connect to the db to do the lookup.
	 */
	public static void updatePartitionColumns(final Mart mart,
			final Schema schema) throws SQLException {
		for (final Iterator i = mart.getDataSets().iterator(); i.hasNext();) {
			final DataSet ds = (DataSet) i.next();
			if (!ds.usesSchema(schema))
				continue;
			for (final Iterator pi = ds.getDataSetModifications()
					.getPartitionedColumns().entrySet().iterator(); pi
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) pi.next();
				final DataSetTable dsTable = (DataSetTable) ds
						.getTableByName((String) entry.getKey());
				final Map colDef = (Map) entry.getValue();
				for (final Iterator ci = colDef.entrySet().iterator(); ci
						.hasNext();) {
					final Map.Entry subEntry = (Map.Entry) ci.next();
					if (!(subEntry.getValue() instanceof ValueList))
						continue;
					// Work out what we've already got.
					final Map existingValues = ((ValueList) subEntry.getValue())
							.getValues();
					// Read the values from the database.
					final Set dbValues = new HashSet();
					// First, make a set of all input schemas. We use a set to
					// prevent duplicates.
					DataSetColumn dsCol = (DataSetColumn) dsTable
							.getColumnByName((String) subEntry.getKey());
					while (dsCol instanceof InheritedColumn)
						dsCol = ((InheritedColumn) dsCol).getInheritedColumn();
					final Column col = ((WrappedColumn) dsCol)
							.getWrappedColumn();
					if (!col.getTable().getSchema().equals(schema))
						continue;
					final DatabaseDialect dd = DatabaseDialect
							.getDialect(schema);
					if (dd != null)
						if (schema.getPartitions().isEmpty())
							dbValues.addAll(dd.executeSelectDistinct(
									((JDBCSchema) schema).getDatabaseSchema(),
									col));
						else
							for (final Iterator si = schema.getPartitions()
									.keySet().iterator(); si.hasNext();)
								dbValues.addAll(dd.executeSelectDistinct(
										(String) si.next(), col));
					// Combine the two to create an updated list.
					final Map newValues = new TreeMap(existingValues);
					for (final Iterator vi = newValues.entrySet().iterator(); vi
							.hasNext();) {
						final Map.Entry oldEntry = (Map.Entry) vi.next();
						if (!dbValues.contains(oldEntry.getValue()))
							i.remove();
					}
					for (final Iterator vi = dbValues.iterator(); vi.hasNext();) {
						final String value = (String) vi.next();
						if (!newValues.containsValue(value))
							newValues.put(value, value);
					}
					// Update the table contents.
					existingValues.clear();
					existingValues.putAll(newValues);
				}
			}
		}
	}

	/**
	 * Turns subclass optimiser off in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to disable subclass optimiser in.
	 */
	public static void noSubclassOptimiserDataSet(final DataSet dataset) {
		Log.info(Resources.get("logReqNoSCOptDataset"));
		dataset.setSubclassOptimiser(false);
	}

	/**
	 * Turns subclass optimiser on in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to enable subclass optimiser in.
	 */
	public static void subclassOptimiserDataSet(final DataSet dataset) {
		Log.info(Resources.get("logReqSCOptDataset"));
		dataset.setSubclassOptimiser(true);
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

	// The tools are static and not intended to be instantiated.
	private MartBuilderUtils() {
	}
}
