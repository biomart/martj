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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.model.SchemaModificationSet.CompoundRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedTableDefinition;
import org.biomart.common.controller.CommonUtils;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Relation.Cardinality;
import org.biomart.common.model.Relation.GenericRelation;

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
		dataset.getDataSetModifications().unsetDistinctTable(table);
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
		dataset.getSchemaModifications().setMaskedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Loopback a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to loopback the relation in.
	 * @param relation
	 *            the relation to loopback.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if the operation is not allowed.
	 */
	public static void loopbackRelation(final DataSetTable datasetTable,
			final Relation relation) throws SQLException, DataModelException,
			ValidationException {
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.setLoopbackRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Loopback a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to loopback the relation in.
	 * @param relation
	 *            the relation to loopback.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 * @throws ValidationException
	 *             if the operation is not allowed.
	 */
	public static void loopbackRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException,
			ValidationException {
		dataset.getSchemaModifications().setLoopbackRelation(relation);
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
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();)
			dataset.getSchemaModifications().setMaskedRelation(
					(Relation) i.next());
		dataset.synchronise();
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
	 * @throws SQLException
	 *             if something went wrong.
	 * @throws DataModelException
	 *             if something went wrong.
	 */
	public static void setExpressionColumn(final DataSetTable dsTable,
			final ExpressionColumnDefinition def, final Map aliases,
			final String expression, final boolean groupBy)
			throws SQLException, DataModelException {
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
		mart.removeDataSet(dataset);
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
		dataset.getSchemaModifications().unsetRestrictedRelation(relation,
				index);
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
		dataset.getSchemaModifications().unsetMaskedRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Unloopbacks a relation within a dataset table. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param datasetTable
	 *            the dataset table to unloopback the relation in.
	 * @param relation
	 *            the relation to unloopback.
	 * @throws ValidationException
	 *             if the relation could not be unforced.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unloopbackRelation(final DataSetTable datasetTable,
			final Relation relation) throws ValidationException, SQLException,
			DataModelException {
		((DataSet) datasetTable.getSchema()).getSchemaModifications()
				.unsetLoopbackRelation(datasetTable, relation);
		((DataSet) datasetTable.getSchema()).synchronise();
	}

	/**
	 * Unloopbacks a relation within a dataset. The dataset is regenerated
	 * afterwards.
	 * 
	 * @param dataset
	 *            the dataset to unloopback the relation in.
	 * @param relation
	 *            the relation to unloopback.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unloopbackRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		dataset.getSchemaModifications().unsetLoopbackRelation(relation);
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
		dataset.getSchemaModifications().unsetForceIncludeRelation(relation);
		dataset.synchronise();
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
		dataset.setInvisible(false);
	}

	/**
	 * Turns index optimiser off in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to disable index optimiser in.
	 */
	public static void noIndexOptimiserDataSet(final DataSet dataset) {
		dataset.setIndexOptimiser(false);
	}

	/**
	 * Turns index optimiser on in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to enable index optimiser in.
	 */
	public static void indexOptimiserDataSet(final DataSet dataset) {
		dataset.setIndexOptimiser(true);
	}

	// The tools are static and not intended to be instantiated.
	private MartBuilderUtils() {
	}
}
