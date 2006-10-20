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
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableRestriction;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.SchemaGroup.GenericSchemaGroup;
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
 * @since 0.1
 */
public class MartBuilderUtils {

	/**
	 * This method asks to create a new expression column.
	 * 
	 * @param table
	 *            the table to add the column to.
	 * @param columnName
	 *            the name to give the column.
	 * @param columnAliases
	 *            the map of columns on the table to labels in the expression.
	 * @param expression
	 *            the expression for the column.
	 * @param groupBy
	 *            whether this column requires a group-by statement. If it does,
	 *            the group-by columns required will be worked out
	 *            automatically.
	 */
	public static void addExpressionColumn(final DataSetTable table,
			final String columnName, final Map columnAliases,
			final String expression, final boolean groupBy) {
		final ExpressionColumn column = new ExpressionColumn(columnName, table);
		column.getAliases().putAll(columnAliases);
		column.setExpression(expression);
		column.setGroupBy(groupBy);
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
	 * This method takes a schema, removes it as an individual schema from a
	 * mart, and adds it to a group instead. If the group with that name does
	 * not exist yet, it is created first and added to the mart. The group will
	 * be synchronised immediately after adding the schema to it. External
	 * relations will be copied into the group.
	 * 
	 * @param mart
	 *            the mart to remove the schema from, and which contains the
	 *            group the schema will be added to.
	 * @param schema
	 *            the schema to remove from the mart and add to the group.
	 * @param groupName
	 *            the name of the group to use. If a group already exists with
	 *            this name, the schema will be added to it. If not, then the
	 *            group will be created first.
	 * @return the group that the schema was added to.
	 * @throws AssociationException
	 *             if the schema is not part of the mart specified.
	 * @throws DataModelException
	 *             if there was any logical problem constructing a new group to
	 *             place the schema in.
	 * @throws SQLException
	 *             if during synchronisation of the group there was any problem
	 *             communicating with the data source or database of the newly
	 *             added schema.
	 */
	public static SchemaGroup addSchemaToSchemaGroup(final Mart mart,
			final Schema schema, final String groupName)
			throws AssociationException, DataModelException, SQLException {
		SchemaGroup schemaGroup = (SchemaGroup) mart.getSchemaByName(groupName);
		if (schemaGroup == null || !(schemaGroup instanceof SchemaGroup)) {
			schemaGroup = new GenericSchemaGroup(groupName);
			mart.addSchema(schemaGroup);
		}
		schemaGroup.addSchema(schema);
		schemaGroup.synchronise();
		mart.removeSchema(schema);
		return schemaGroup;
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
		// Change the cardinality.
		relation.setCardinality(cardinality);

		// If 1:1, make sure it isn't used as a subclass or concat-only relation
		// in any dataset.
		if (relation.isOneToOne())
			for (final Iterator i = mart.getDataSets().iterator(); i.hasNext();) {
				final DataSet ds = (DataSet) i.next();
				ds.unflagSubclassRelation(relation);
				ds.unflagConcatOnlyRelation(relation);
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
		relation.setStatus(status);
		mart.synchroniseDataSets();
	}

	/**
	 * Flags a relation as concat-only within a dataset, then synchronise the
	 * dataset.
	 * 
	 * @param dataset
	 *            the dataset to flag the relation in.
	 * @param relation
	 *            the relation to flag as concat-only.
	 * @param type
	 *            the type of concat-only relation this should be.
	 * @throws ValidationException
	 *             if the dataset could not be synchronised.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void concatOnlyRelation(final DataSet dataset,
			final Relation relation, final DataSetConcatRelationType type)
			throws SQLException, DataModelException, ValidationException {
		dataset.flagConcatOnlyRelation(relation, type);
		dataset.unflagSubclassRelation(relation);
		dataset.synchronise();
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

		// Create the relation.
		final Relation r = new GenericRelation(from, to, Cardinality.MANY);
		r.setStatus(ComponentStatus.HANDMADE);

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
		dataset.setInvisible(true);
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
	 */
	public static void maskColumn(final DataSet dataset,
			final DataSetColumn column) throws ValidationException {
		column.setMasked(true);
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
		dataset.maskRelation(relation);
		dataset.synchronise();
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
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();)
			dataset.maskRelation((Relation) i.next());
		dataset.synchronise();
	}

	/**
	 * This method asks to modify an expression column.
	 * 
	 * @param column
	 *            the column to modify.
	 * @param columnAliases
	 *            the map of columns on the table to labels in the expression.
	 * @param expression
	 *            the expression for the column.
	 * @param groupBy
	 *            whether this column requires a group-by statement. If it does,
	 *            the group-by columns required will be worked out
	 *            automatically.
	 */
	public static void modifyExpressionColumn(final ExpressionColumn column,
			final Map columnAliases, final String expression,
			final boolean groupBy) {
		column.getAliases().clear();
		column.getAliases().putAll(columnAliases);
		column.setExpression(expression);
		column.setGroupBy(groupBy);
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
			final DataSetColumn column, final PartitionedColumnType type)
			throws ValidationException {
		column.setPartitionType(type);
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
	 * @param column
	 *            the expression column to remove.
	 */
	public static void removeExpressionColumn(final ExpressionColumn column) {
		column.getTable().removeColumn(column);
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
		mart.removeSchema(schema);
	}

	/**
	 * Removes a schema from a group and places it back into the mart as an
	 * individual schema. If that was the last schema in the group, the group
	 * itself is disbanded. Otherwise, the group is synchronised but the schema
	 * itself is not. External relations will not be exported to the schema
	 * unless it is the last schema in the group and the group is now going to
	 * be disbanded.
	 * 
	 * @param mart
	 *            the mart to add the schema to once it has been removed from
	 *            the group.
	 * @param schema
	 *            the schema to remove from the group.
	 * @param schemaGroup
	 *            the group to remove the schema from.
	 * @throws AssociationException
	 *             if the schema is not part of the group, or the group is not
	 *             part of the mart.
	 * @throws DataModelException
	 *             if there was any logical problem during synchronisation of
	 *             the group after removal of the schema.
	 * @throws SQLException
	 *             if there was any problem connecting to a data source or
	 *             database during synchronisation of the group.
	 */
	public static void removeSchemaFromSchemaGroup(final Mart mart,
			final Schema schema, final SchemaGroup schemaGroup)
			throws AssociationException, DataModelException, SQLException {
		schemaGroup.removeSchema(schema);
		if (schemaGroup.getSchemas().size() == 0) {
			schemaGroup.replicateContents(schema);
			mart.removeSchema(schemaGroup);
		} else
			schemaGroup.synchronise();
		mart.addSchema(schema);
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
	 */
	public static void renameDataSetColumn(final DataSetColumn col,
			final String newName) {
		col.setName(newName);
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
		tbl.setName(newName);
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
	 * Renames a schema within a group of schemas. Schema groups do not care
	 * what their members are called, so we don't need to pass in a reference.
	 * Likewise, marts don't care because they're only aware of the groups, not
	 * the individual schemas within them. However, take care when extracting a
	 * schema from a group that it doesn't share a name with some other
	 * individual schema.
	 * 
	 * @param schema
	 *            the schema to rename.
	 * @param newName
	 *            the new name to give the schema.
	 */
	public static void renameSchemaInSchemaGroup(final Schema schema,
			final String newName) {
		schema.setName(newName);
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
	 * Flags a relation as restricted within a dataset. If already flagged, this
	 * updates the existing settings.
	 * 
	 * @param dataset
	 *            the dataset to flag the relation in.
	 * @param relation
	 *            the relation to flag as restricted.
	 * @param expression
	 *            the expression to use for the restriction for the relation.
	 * @param firstTableAliases
	 *            the aliases to use for columns from the first table of the
	 *            relation.
	 * @param secondTableAliases
	 *            the aliases to use for columns from the second table of the
	 *            relation.
	 */
	public static void restrictRelation(final DataSet dataset,
			final Relation relation, final String expression,
			final Map firstTableAliases, final Map secondTableAliases) {
		final DataSetRelationRestriction restriction = new DataSetRelationRestriction(
				expression, firstTableAliases, secondTableAliases);
		dataset.flagRestrictedRelation(relation, restriction);
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
	 */
	public static void restrictTable(final DataSet dataset, final Table table,
			final String expression, final Map aliases) {
		final DataSetTableRestriction restriction = new DataSetTableRestriction(
				expression, aliases);
		dataset.flagRestrictedTable(table, restriction);
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
	public static List selectRows(final Table table, final int offset,
			final int count) throws SQLException {
		final Schema schema = table.getSchema();
		final List results = new ArrayList();
		if (schema instanceof SchemaGroup)
			for (final Iterator i = ((SchemaGroup) schema).getSchemas()
					.iterator(); i.hasNext();) {
				final Schema internalSchema = (Schema) i.next();
				final DatabaseDialect dd = DatabaseDialect
						.getDialect(internalSchema);
				if (dd != null)
					results.addAll(dd.executeSelectRows(internalSchema
							.getTableByName(table.getName()), offset, count));
			}
		else {
			final DatabaseDialect dd = DatabaseDialect.getDialect(schema);
			if (dd != null)
				results.addAll(dd.executeSelectRows(table, offset, count));
		}
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
		dataset.flagSubclassRelation(relation);
		dataset.unflagConcatOnlyRelation(relation);
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
	 */
	public static void synchroniseMartDataSets(final Mart mart) {
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
	 * Unflags a relation as concat-only within a dataset, then regenerates the
	 * dataset.
	 * 
	 * @param dataset
	 *            the dataset to unflag the relation within.
	 * @param relation
	 *            the relation to unflag.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws DataModelException
	 *             if the dataset could not be synchronised.
	 */
	public static void unconcatOnlyRelation(final DataSet dataset,
			final Relation relation) throws SQLException, DataModelException {
		dataset.unflagConcatOnlyRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Unmasks a column within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unmask the column in.
	 * @param column
	 *            the column to unmask.
	 * @throws ValidationException
	 *             if the column could not be used for masking, for whatever
	 *             reason.
	 */
	public static void unmaskColumn(final DataSet dataset,
			final DataSetColumn column) throws ValidationException {
		column.setMasked(false);
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
		for (final Iterator i = table.getRelations().iterator(); i.hasNext();)
			dataset.unmaskRelation((Relation) i.next());
		dataset.synchronise();
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
		dataset.unmaskRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Turns off partitioning on a given dataset column.
	 * 
	 * @param dataset
	 *            the dataset to turn off partitioning for on this column.
	 * @param column
	 *            the column to turn off partitioning for.
	 * @throws ValidationException
	 *             if the column could not be used for partitioning, for
	 *             whatever reason.
	 */
	public static void unpartitionByColumn(final DataSet dataset,
			final DataSetColumn column) throws ValidationException {
		column.setPartitionType(null);
	}

	/**
	 * Unflags a relation as restricted within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unflag the relation within.
	 * @param relation
	 *            the relation to unflag.
	 */
	public static void unrestrictRelation(final DataSet dataset,
			final Relation relation) {
		dataset.unflagRestrictedRelation(relation);
	}

	/**
	 * Unflags a table as restricted within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unflag the table within.
	 * @param table
	 *            the table to unflag.
	 */
	public static void unrestrictTable(final DataSet dataset, final Table table) {
		dataset.unflagRestrictedTable(table);
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
		dataset.unflagSubclassRelation(relation);
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
	 * The tools are static and not intended to be instantiated.
	 */
	private MartBuilderUtils() {
	}
}
