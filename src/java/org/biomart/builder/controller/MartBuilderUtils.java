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

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableRestriction;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.SchemaGroup.GenericSchemaGroup;

/**
 * Tools for working with the mart from a GUI or CLI. These wrapper methods
 * exist to prevent the GUI or CLI having to know about the exact details of
 * manipulating the various objects in the data model. If the GUI or CLI are the
 * View of an MVC system, then this is a Controller, and the data model is
 * obviously the Model.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.29, 24th July 2006
 * @since 0.1
 */
public class MartBuilderUtils {

	/**
	 * The tools are static and not intended to be instantiated.
	 */
	private MartBuilderUtils() {
	}

	/**
	 * This method asks to remove a particular expression column.
	 * 
	 * @param column
	 *            the expression column to remove.
	 */
	public static void removeExpressionColumn(ExpressionColumn column) {
		column.getTable().removeColumn(column);
	}

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
	public static void addExpressionColumn(DataSetTable table,
			String columnName, Map columnAliases, String expression,
			boolean groupBy) throws AlreadyExistsException {
		ExpressionColumn column = new ExpressionColumn(columnName, table);
		column.getAliases().putAll(columnAliases);
		column.setExpression(expression);
		column.setGroupBy(groupBy);
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
	public static void modifyExpressionColumn(ExpressionColumn column,
			Map columnAliases, String expression, boolean groupBy)
			throws AlreadyExistsException {
		column.getAliases().clear();
		column.getAliases().putAll(columnAliases);
		column.setExpression(expression);
		column.setGroupBy(groupBy);
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
	 * @throws BuilderException
	 *             if any of them find logical problems during the
	 *             synchronisation process.
	 */
	public static void synchroniseMartSchemas(Mart mart) throws SQLException,
			BuilderException {
		mart.synchroniseSchemas();
	}

	/**
	 * This method asks the mart to synchronise all its datasets against the
	 * schemas they are formed from.
	 * 
	 * @param mart
	 *            the mart you wish to synchronise all the datasets in.
	 * @throws SQLException
	 *             if any of them have problems communicating with their
	 *             schemas. Technically this should never happen, but the
	 *             exception is part of the interface specification for datasets
	 *             (which are types of schema themselves), so must be thrown
	 *             here too.
	 * @throws BuilderException
	 *             if any logical problems are found within the schemas or
	 *             datasets.
	 */
	public static void synchroniseMartDataSets(Mart mart) {
		mart.synchroniseDataSets();
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
	public static void removeDataSetFromMart(Mart mart, DataSet dataset) {
		mart.removeDataSet(dataset);
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
	 * @throws AlreadyExistsException
	 *             if the new name has already been used by another dataset in
	 *             this mart.
	 * @throws AssociationException
	 *             if the dataset is not part of this mart.
	 */
	public static void renameDataSet(Mart mart, DataSet dataset, String newName)
			throws AlreadyExistsException, AssociationException {
		mart.renameDataSet(dataset, newName);
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
	 * @throws AlreadyExistsException
	 *             Should never happen.
	 * @throws AssociationException
	 *             if the specified table is not part of the mart given.
	 * @throws SQLException
	 *             if there is any trouble communicating with the data source or
	 *             database during schema synchronisation.
	 * @throws BuilderException
	 *             if synchronisation fails.
	 */
	public static Collection suggestDataSets(Mart mart, Collection tables)
			throws SQLException, AssociationException, AlreadyExistsException,
			BuilderException {
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
	 * @throws AlreadyExistsException
	 *             Should never happen.
	 * @throws AssociationException
	 *             if the specified table is not part of the mart given.
	 * @throws SQLException
	 *             if there is any trouble communicating with the data source or
	 *             database during schema synchronisation.
	 * @throws BuilderException
	 *             if synchronisation fails.
	 */
	public static Collection suggestInvisibleDataSets(Mart mart,
			DataSet dataset, Collection columns) throws AssociationException,
			AlreadyExistsException, BuilderException, SQLException {
		return mart.suggestInvisibleDataSets(dataset, columns);
	}

	/**
	 * Adds a schema to a mart.
	 * 
	 * @param mart
	 *            the mart to add the schema to.
	 * @param schema
	 *            the schema to add to the mart.
	 * @throws AlreadyExistsException
	 *             if a schema with that name already exists in this mart.
	 */
	public static void addSchemaToMart(Mart mart, Schema schema)
			throws AlreadyExistsException {
		mart.addSchema(schema);
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
	public static void removeSchemaFromMart(Mart mart, Schema schema) {
		mart.removeSchema(schema);
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
	 * @throws AlreadyExistsException
	 *             if another schema in this mart already has the new name.
	 * @throws AssociationException
	 *             if the schema is not part of this mart.
	 */
	public static void renameSchema(Mart mart, Schema schema, String newName)
			throws AlreadyExistsException, AssociationException {
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
	public static void renameSchemaInSchemaGroup(Schema schema, String newName) {
		schema.setName(newName);
	}

	/**
	 * Synchronises an individual schema against the data source or database it
	 * represents. Datasets using this schema will also be synchronised.
	 * 
	 * @param schema
	 *            the schema to synchronise.
	 * @throws SQLException
	 *             if there was any problem communicating with the data source
	 *             or database.
	 * @throws BuilderException
	 *             if there were any logical problems with synchronisation.
	 */
	public static void synchroniseSchema(Schema schema) throws SQLException,
			BuilderException {
		schema.synchronise();
	}

	/**
	 * Tests a schema. In most cases, this simply tests the connection between
	 * the schema and the data source or database it represents. The connection
	 * between test failure and throwing an exception describing the failure
	 * means that this routine will (probably) never return <tt>false</tt>,
	 * only ever <tt>true</tt> or an exception.
	 * 
	 * @param schema
	 *            the schema to test.
	 * @return <tt>true</tt> if it passed the test, <tt>false</tt>
	 *         otherwise.
	 * @throws Exception
	 *             if it failed the test. The exception will describe the reason
	 *             for failure.
	 */
	public static boolean testSchema(Schema schema) throws Exception {
		return schema.test();
	}

	/**
	 * Creates a JDBC schema and returns it. No further action is taken - the
	 * schema will not synchronise itself.
	 * 
	 * @param driverClassLocation
	 *            the location, optional, of the JDBC driver class. This can
	 *            either be a JAR file or a path to a class file. If null, the
	 *            default system class loader is used to load the class. The
	 *            system class loader is also used if the class could not be
	 *            found in the location specified here.
	 * @param driverClassName
	 *            the name of the JDBC driver class, eg.
	 *            <tt>com.mysql.jdbc.Driver</tt>
	 * @param url
	 *            the JDBC url to connect to.
	 * @param schemaName
	 *            the target schema to connect to.
	 * @param username
	 *            the username to connect with.
	 * @param password
	 *            the password to connect with. If the empty string is passed
	 *            in, no password is given to the connection at all.
	 * @param name
	 *            the name to give the created schema.
	 * @param keyGuessing
	 *            a flag indicating whether to enable key-guessing on this JDBC
	 *            schema or not. This can always be changed later.
	 * @return the created schema.
	 */
	public static JDBCSchema createJDBCSchema(File driverClassLocation,
			String driverClassName, String url, String schemaName,
			String username, String password, String name, boolean keyGuessing) {
		if (password != null && password.equals(""))
			password = null;
		return new JDBCSchema(driverClassLocation, driverClassName, url,
				schemaName, username, password, name, keyGuessing);
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
	 * @throws AlreadyExistsException
	 *             if a schema already exists in that group with the same name,
	 *             or if a schema already exists in the mart with the same name
	 *             as the group.
	 * @throws BuilderException
	 *             if there was any logical problem constructing a new group to
	 *             place the schema in.
	 * @throws SQLException
	 *             if during synchronisation of the group there was any problem
	 *             communicating with the data source or database of the newly
	 *             added schema.
	 */
	public static SchemaGroup addSchemaToSchemaGroup(Mart mart, Schema schema,
			String groupName) throws AssociationException,
			AlreadyExistsException, BuilderException, SQLException {
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
	 * @throws AlreadyExistsException
	 *             if another schema with the same name as the extracted one
	 *             already exists in the mart.
	 * @throws BuilderException
	 *             if there was any logical problem during synchronisation of
	 *             the group after removal of the schema.
	 * @throws SQLException
	 *             if there was any problem connecting to a data source or
	 *             database during synchronisation of the group.
	 */
	public static void removeSchemaFromSchemaGroup(Mart mart, Schema schema,
			SchemaGroup schemaGroup) throws AssociationException,
			AlreadyExistsException, BuilderException, SQLException {
		schemaGroup.removeSchema(schema);
		if (schemaGroup.getSchemas().size() == 0) {
			schemaGroup.replicateContents(schema);
			mart.removeSchema(schemaGroup);
		} else
			schemaGroup.synchronise();
		mart.addSchema(schema);
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
	public static void changeRelationCardinality(Mart mart, Relation relation,
			Cardinality cardinality) {
		// Change the cardinality.
		relation.setCardinality(cardinality);

		// If 1:1, make sure it isn't used as a subclass or concat-only relation
		// in any dataset.
		if (relation.isOneToOne())
			for (Iterator i = mart.getDataSets().iterator(); i.hasNext();) {
				DataSet ds = (DataSet) i.next();
				ds.unflagSubclassRelation(relation);
				ds.unflagConcatOnlyRelation(relation);
			}

		// Synchronise the datasets.
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
	public static void removeRelation(Mart mart, Relation relation) {
		relation.destroy();
		mart.synchroniseDataSets();
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
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskRelation(DataSet dataset, Relation relation)
			throws SQLException, BuilderException {
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
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskTable(DataSet dataset, Table table)
			throws SQLException, BuilderException {
		for (Iterator i = table.getRelations().iterator(); i.hasNext();)
			dataset.maskRelation((Relation) i.next());
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
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskRelation(DataSet dataset, Relation relation)
			throws SQLException, BuilderException {
		dataset.unmaskRelation(relation);
		dataset.synchronise();
	}

	/**
	 * Flags a relation as subclassed within a dataset. The dataset is
	 * regenerated afterwards.
	 * 
	 * @param dataset
	 *            the dataset to flag the relation within.
	 * @param relation
	 *            the subclassed relation.
	 * @throws AssociationException
	 *             if the relation is not permitted to be a subclass relation in
	 *             this dataset, for whatever reason.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void subclassRelation(DataSet dataset, Relation relation)
			throws AssociationException, SQLException, BuilderException {
		dataset.flagSubclassRelation(relation);
		dataset.unflagConcatOnlyRelation(relation);
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
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void unsubclassRelation(DataSet dataset, Relation relation)
			throws SQLException, BuilderException {
		dataset.unflagSubclassRelation(relation);
		dataset.synchronise();
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
	 * @throws AssociationException
	 *             if the relation was not concat-able.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void concatOnlyRelation(DataSet dataset, Relation relation,
			ConcatRelationType type) throws AssociationException, SQLException,
			BuilderException {
		dataset.flagConcatOnlyRelation(relation, type);
		dataset.unflagSubclassRelation(relation);
		dataset.synchronise();
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
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void unconcatOnlyRelation(DataSet dataset, Relation relation)
			throws SQLException, BuilderException {
		dataset.unflagConcatOnlyRelation(relation);
		dataset.synchronise();
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
	public static void restrictRelation(DataSet dataset, Relation relation,
			String expression, Map firstTableAliases, Map secondTableAliases) {
		DataSetRelationRestriction restriction = new DataSetRelationRestriction(
				expression, firstTableAliases, secondTableAliases);
		dataset.flagRestrictedRelation(relation, restriction);
	}

	/**
	 * Unflags a relation as restricted within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unflag the relation within.
	 * @param relation
	 *            the relation to unflag.
	 */
	public static void unrestrictRelation(DataSet dataset, Relation relation)
			throws SQLException, BuilderException {
		dataset.unflagRestrictedRelation(relation);
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
	public static void restrictTable(DataSet dataset, Table table,
			String expression, Map aliases) {
		DataSetTableRestriction restriction = new DataSetTableRestriction(
				expression, aliases);
		dataset.flagRestrictedTable(table, restriction);
	}

	/**
	 * Unflags a table as restricted within a dataset.
	 * 
	 * @param dataset
	 *            the dataset to unflag the table within.
	 * @param table
	 *            the table to unflag.
	 */
	public static void unrestrictTable(DataSet dataset, Table table)
			throws SQLException, BuilderException {
		dataset.unflagRestrictedTable(table);
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
	public static void changeRelationStatus(Mart mart, Relation relation,
			ComponentStatus status) throws AssociationException {
		relation.setStatus(status);
		mart.synchroniseDataSets();
	}

	/**
	 * Renames a table within a dataset.
	 * 
	 * @param tbl
	 *            the table to rename.
	 * @param newName
	 *            the new name to give it. If the name is the same as the
	 *            existing one, no action is taken.
	 * @throws AlreadyExistsException
	 *             if the dataset already has another table with that name.
	 */
	public static void renameDataSetTable(DataSetTable tbl, String newName)
			throws AlreadyExistsException {
		tbl.setName(newName);
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
	 * @throws AlreadyExistsException
	 *             if the dataset table already has another column with that
	 *             name.
	 */
	public static void renameDataSetColumn(DataSetColumn col, String newName)
			throws AlreadyExistsException {
		// If the new name is the same as the old, ignore the request.
		if (newName.equals(col.getName()))
			return;

		// Rename the column.
		col.setName(newName);

		// Make a list of relations involving this column. It
		// is empty to start with.
		List relations = new ArrayList();

		// Is it in any of the keys on this table?
		for (Iterator i = col.getTable().getKeys().iterator(); i.hasNext();) {
			Key k = (Key) i.next();

			// Is the column in this key?
			if (k.getColumns().contains(col))
				// Add the relations on the key to the list to process.
				// The first half of the object in the list is the position
				// of the column in the key. The second half is all relations
				// involving that object.
				relations.add(new Object[] {
						new Integer(k.getColumns().indexOf(col)),
						k.getRelations() });
		}

		// For each group of relations involving this column, iterate over
		// each relation and rename the column in the keys at both ends.
		for (Iterator i = relations.iterator(); i.hasNext();) {
			Object[] obj = (Object[]) i.next();

			// What index in the key is the column to rename?
			int colIndex = ((Integer) obj[0]).intValue();

			// Loop over all the relations using that key.
			for (Iterator j = ((Collection) obj[1]).iterator(); j.hasNext();) {
				Relation r = (Relation) j.next();

				// Rename both ends using recursion. This works because this
				// method skips the rename process if the names already match.
				renameDataSetColumn((DataSetColumn) r.getFirstKey()
						.getColumns().get(colIndex), newName);
				renameDataSetColumn((DataSetColumn) r.getSecondKey()
						.getColumns().get(colIndex), newName);
			}
		}
	}

	/**
	 * Masks a column within a dataset, then regenerates the dataset.
	 * 
	 * @param dataset
	 *            the dataset to mask the column in.
	 * @param column
	 *            the column to mask.
	 * @throws AssociationException
	 *             if the column is not maskable.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void maskColumn(DataSet dataset, DataSetColumn column)
			throws AssociationException, SQLException, BuilderException {
		dataset.maskDataSetColumn(column);
		dataset.synchronise();
	}

	/**
	 * Unmasks a column within a dataset, then regenerates the dataset.
	 * 
	 * @param dataset
	 *            the dataset to unmask the column in.
	 * @param column
	 *            the column to unmask.
	 * @throws SQLException
	 *             if the dataset could not be synchronised.
	 * @throws BuilderException
	 *             if the dataset could not be synchronised.
	 */
	public static void unmaskColumn(DataSet dataset, DataSetColumn column)
			throws SQLException, BuilderException {
		dataset.unmaskDataSetColumn(column);
		dataset.synchronise();
	}

	/**
	 * Sets the partition-by-schema flag to on in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to turn the partition-by-schema flag on for.
	 */
	public static void partitionBySchema(DataSet dataset) {
		dataset.setPartitionOnSchema(true);
	}

	/**
	 * Sets the partition-by-schema flag to off in a given dataset.
	 * 
	 * @param dataset
	 *            the dataset to turn the partition-by-schema flag off for.
	 */
	public static void unpartitionBySchema(DataSet dataset) {
		dataset.setPartitionOnSchema(false);
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
	 * @throws AssociationException
	 *             if the column could not be used for partitioning, for
	 *             whatever reason.
	 */
	public static void partitionByColumn(DataSet dataset, WrappedColumn column,
			PartitionedColumnType type) throws AssociationException {
		dataset.flagPartitionedWrappedColumn(column, type);
	}

	/**
	 * Turns off partitioning on a given dataset column.
	 * 
	 * @param dataset
	 *            the dataset to turn off partitioning for on this column.
	 * @param column
	 *            the column to turn off partitioning for.
	 */
	public static void unpartitionByColumn(DataSet dataset, WrappedColumn column) {
		dataset.unflagPartitionedWrappedColumn(column);
	}

	/**
	 * Turns key-guessing on in a given schema.
	 * 
	 * @param schema
	 *            the schema to enable key-guessing in.
	 * @throws SQLException
	 *             if after keyguessing the key sync went wrong.
	 * @throws BuilderException
	 *             if after keyguessing the key sync went wrong.
	 */
	public static void enableKeyGuessing(Schema schema)
			throws BuilderException, SQLException {
		schema.setKeyGuessing(true);
	}

	/**
	 * Turns key-guessing off in a given schema.
	 * 
	 * @param schema
	 *            the schema to disable key-guessing in.
	 * @throws SQLException
	 *             if after disabling keyguessing the key sync went wrong.
	 * @throws BuilderException
	 *             if after disabling keyguessing the key sync went wrong.
	 */
	public static void disableKeyGuessing(Schema schema) throws SQLException,
			BuilderException {
		schema.setKeyGuessing(false);
	}

	/**
	 * Attempts to create a primary key on a table given a set of columns. If
	 * the table already has a primary key, then this one will replace it. The
	 * new key will have a status of {@link ComponentStatus#HANDMADE}.
	 * 
	 * @param table
	 *            the table to create the key on.
	 * @param columns
	 *            the colums, in order, to create the key over.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void createPrimaryKey(Table table, List columns)
			throws AssociationException {
		PrimaryKey pk = new GenericPrimaryKey(columns);
		pk.setStatus(ComponentStatus.HANDMADE);
		table.setPrimaryKey(pk);
	}

	/**
	 * Attempts to create a foreign key on a table given a set of columns. The
	 * new key will have a status of {@link ComponentStatus#HANDMADE}.
	 * 
	 * @param table
	 *            the table to create the key on.
	 * @param columns
	 *            the colums, in order, to create the key over.
	 * @throws AssociationException
	 *             if any of the columns in the key are not part of the
	 *             specified table.
	 */
	public static void createForeignKey(Table table, List columns)
			throws AssociationException {
		ForeignKey fk = new GenericForeignKey(columns);
		fk.setStatus(ComponentStatus.HANDMADE);
		table.addForeignKey(fk);
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
	public static void editKeyColumns(Mart mart, Key key, List columns)
			throws AssociationException {
		key.setColumns(columns);
		changeKeyStatus(mart, key, ComponentStatus.HANDMADE);
		mart.synchroniseDataSets();
	}

	/**
	 * Removes a key. All datasets will subsequently be synchronised.
	 * 
	 * @param mart
	 *            the mart to remove the key from.
	 * @param key
	 *            the key to remove.
	 */
	public static void removeKey(Mart mart, Key key) {
		key.destroy();
		mart.synchroniseDataSets();
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
	public static void changeKeyStatus(Mart mart, Key key,
			ComponentStatus status) {
		key.setStatus(status);
		mart.synchroniseDataSets();
	}

	/**
	 * Changes the nullability of a foreign key.
	 * 
	 * @param key
	 *            the foreign key to change.
	 * @param nullable
	 *            the new nullable status to give the key. <tt>true</tt> means
	 *            it is nullable, <tt>false</tt> means it is not.
	 */
	public static void changeForeignKeyNullability(ForeignKey key,
			boolean nullable) {
		key.setNullable(nullable);
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
	public static void createRelation(Mart mart, Key from, Key to)
			throws AssociationException {

		// Create the relation.
		Relation r = new GenericRelation(from, to, Cardinality.MANY);
		r.setStatus(ComponentStatus.HANDMADE);

		// Synchronise the datasets.
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
	public static void changeOptimiserType(DataSet dataset,
			DataSetOptimiserType type) {
		dataset.setDataSetOptimiserType(type);
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
	 * @throws AlreadyExistsException
	 *             if another schema with that name already exists.
	 */
	public static Schema replicateSchema(Mart mart, Schema schema,
			String newName) throws AlreadyExistsException {
		Schema newSchema = schema.replicate(newName);
		mart.addSchema(newSchema);
		return newSchema;
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
	 * @throws AlreadyExistsException
	 *             if another dataset with that name already exists.
	 */
	public static DataSet replicateDataSet(Mart mart, DataSet dataset,
			String newName) throws AlreadyExistsException {
		DataSet newDataSet = (DataSet) dataset.replicate(newName);
		return newDataSet;
	}
}
