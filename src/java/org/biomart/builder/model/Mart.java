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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.BuilderBundle;

/**
 * The mart contains the set of all schemas that are providing data to this
 * mart. It also has zero or more datasets based around these.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 2nd June 2006
 * @since 0.1
 */
public class Mart {
	// OK to use map, as keys are strings and never change.
	private final Map schemas = new HashMap();

	// OK to use map, as keys are strings and never change.
	private final Map datasets = new HashMap();

	/**
	 * Returns the set of schema objects which this mart includes when building
	 * a mart. The set may be empty but it is never null.
	 * 
	 * @return a set of schema objects.
	 */
	public Collection getSchemas() {
		return this.schemas.values();
	}

	/**
	 * Returns the schema object with the given name. If it doesn't exist, null
	 * is returned. If the name was null, you'll get an exception.
	 * 
	 * @param name
	 *            the name to look for.
	 * @return a schema object matching the specified name.
	 */
	public Schema getSchemaByName(String name) {
		return (Schema) this.schemas.get(name);
	}

	/**
	 * Adds a schema to the set which this mart includes. An exception is thrown
	 * if it already is in this set, or if it is null.
	 * 
	 * 
	 * @param schema
	 *            the schema to add.
	 * @throws AlreadyExistsException
	 *             if the schema is already in this mart.
	 */
	public void addSchema(Schema schema) throws AlreadyExistsException {
		// Check we don't have one by this name already.
		if (this.schemas.containsKey(schema.getName()))
			throw new AlreadyExistsException(BuilderBundle
					.getString("schemaExists"), schema.getName());

		// Add it.
		this.schemas.put(schema.getName(), schema);
	}

	/**
	 * Renames a schema. An exception is thrown if that names has already been
	 * used, or if it is null. This call cascades to the schema and renames that
	 * as well.
	 * 
	 * @param schema
	 *            the schema to rename.
	 * @param name
	 *            the new name for it.
	 * @throws AlreadyExistsException
	 *             if the schema name is already in this mart.
	 * @throws AssociationException
	 *             if the schema does not belong to us.
	 */
	public void renameSchema(Schema schema, String name)
			throws AlreadyExistsException, AssociationException {
		// Check the schema belongs to us, and the new name has not
		// already been used.
		if (this.schemas.containsKey(name))
			throw new AlreadyExistsException(BuilderBundle
					.getString("schemaExists"), schema.getName());
		if (!this.schemas.containsValue(schema))
			throw new AssociationException(BuilderBundle
					.getString("schemaMartMismatch"));

		// Rename it.
		this.schemas.remove(schema.getName());
		schema.setName(name);
		this.schemas.put(name, schema);
	}

	/**
	 * Removes a schema from the set which this mart includes. Any datasets
	 * centred on this schema are also removed.
	 * 
	 * @param schema
	 *            the schema to remove.
	 */
	public void removeSchema(Schema schema) {
		List datasets = new ArrayList(this.getDataSets());
		for (Iterator i = datasets.iterator(); i.hasNext();) {
			DataSet ds = (DataSet) i.next();
			if (ds.getCentralTable().getSchema().equals(schema))
				this.removeDataSet(ds);
		}
		this.schemas.remove(schema);
	}

	/**
	 * Returns the set of dataset objects which this mart includes. The set may
	 * be empty but it is never null.
	 * 
	 * @return a set of dataset objects.
	 */
	public Collection getDataSets() {
		return this.datasets.values();
	}

	/**
	 * Returns the dataset object with the given name.
	 * 
	 * @param name
	 *            the name to look for.
	 * @return a dataset object matching the specified name.
	 */
	public DataSet getDataSetByName(String name) {
		return (DataSet) this.datasets.get(name);
	}

	/**
	 * Adds a dataset to the mart.
	 * 
	 * @param dataset
	 *            the dataset to add.
	 * @throws AlreadyExistsException
	 *             if the dataset is already in this schema with the same name.
	 */
	public void addDataSet(DataSet dataset) throws AlreadyExistsException {
		// Check the dataset name has not already been used.
		if (this.datasets.containsKey(dataset.getName()))
			throw new AlreadyExistsException(BuilderBundle
					.getString("datasetExists"), dataset.getName());

		// Add it.
		this.datasets.put(dataset.getName(), dataset);
	}

	/**
	 * Given a particular table, automatically create a number of datasets based
	 * around that table that represent the various possible subclassing
	 * scenarios. It will always create one dataset (with the name given) that
	 * doesn't subclass anything. For every M:1 relation leading off the table,
	 * another dataset will be created containing that subclass relation. Each
	 * subclass dataset choice will have a number appended to it after an
	 * underscore, eg. '_SC1' ,'_SC2' etc. Each table created will be optimised
	 * automatically.
	 * 
	 * @param centralTable
	 *            the table to build predicted datasets around.
	 * @param name
	 *            the name to use for the datasets.
	 * @return the newly created datasets, which will already have been added to
	 *         the mart.
	 * @throws AlreadyExistsException
	 *             if a dataset already exists in this schema with the same name
	 *             or any of the suffixed versions.
	 */
	public Collection suggestDataSets(Table centralTable, String name)
			throws SQLException, AssociationException, AlreadyExistsException {
		// Make a list to hold the new datasets.
		List newDataSets = new ArrayList();

		// Create the base-case no-subclass version.
		DataSet basicDS = new DataSet(this, centralTable, name);
		basicDS.optimiseDataSet();
		newDataSets.add(basicDS);

		// Predict the subclass relations from the existing m:1 relations -
		// simple guesser based
		// on finding foreign keys in the central table.
		int suffix = 1;
		for (Iterator i = centralTable.getForeignKeys().iterator(); i.hasNext();) {
			Key k = (Key) i.next();
			for (Iterator j = k.getRelations().iterator(); j.hasNext();) {
				Relation r = (Relation) j.next();
				// Only flag potential M:1 subclass relations if they don't
				// refer back to ourselves.
				if (r.getFKCardinality().equals(Cardinality.MANY)
						&& !r.getPrimaryKey().getTable().equals(centralTable)) {
					DataSet subclassedDS = new DataSet(this, centralTable, name
							+ BuilderBundle.getString("subclassDataSetSuffix")
							+ (suffix++));
					subclassedDS.flagSubclassRelation(r);
					subclassedDS.optimiseDataSet();
					newDataSets.add(subclassedDS);
				}
			}
		}

		// Return the set we just created.
		return newDataSets;
	}

	/**
	 * Renames a dataset. This call cascades to the dataset itself and renames
	 * that too.
	 * 
	 * @param dataset
	 *            the dataset to rename.
	 * @param name
	 *            the new name for it.
	 * @throws AlreadyExistsException
	 *             if the dataset name is already in this mart.
	 * @throws AssociationException
	 *             if the dataset does not belong to us.
	 */
	public void renameDataSet(DataSet dataset, String name)
			throws AlreadyExistsException, AssociationException {
		// Check the dataset belongs to us and the new name is unique.
		if (this.datasets.containsKey(name))
			throw new AlreadyExistsException(BuilderBundle
					.getString("datasetExists"), dataset.getName());
		if (!this.datasets.containsValue(dataset))
			throw new AssociationException(BuilderBundle
					.getString("datasetMartMismatch"));

		// Rename it.
		this.datasets.remove(dataset.getName());
		dataset.setName(name);
		this.datasets.put(name, dataset);
	}

	/**
	 * Removes a dataset from the set which this mart includes.
	 * 
	 * @param dataset
	 *            the dataset to remove.
	 */
	public void removeDataSet(DataSet dataset) {
		this.datasets.remove(dataset);
	}

	/**
	 * Synchronise all datasets in this mart so that they match up with the
	 * mart's schema(s). Any datasets that are based on now-missing tables are
	 * dropped. This is all simply a matter of delegating calls and the routine
	 * does no real work itself.
	 */
	public void synchroniseDataSets() {
		for (Iterator i = this.datasets.values().iterator(); i.hasNext();) {
			DataSet ds = (DataSet) i.next();

			// Has the table gone?
			if (!this.getSchemas().contains(ds.getCentralTable().getSchema()))
				i.remove();

			// If not, synchronise it.
			else
				try {
					ds.synchronise();
				} catch (Throwable t) {
					throw new MartBuilderInternalError(t);
				}
		}
	}

	/**
	 * Synchronise this mart with the schema(s) that is(are) providing its
	 * tables, then synchronises its datasets too. This is all simply a matter
	 * of delegating calls and the routine does no real work itself.
	 * 
	 * @throws SQLException
	 *             if there was a problem connecting to the data source.
	 * @throws BuilderException
	 *             if there was any other kind of problem.
	 */
	public void synchroniseSchemas() throws SQLException, BuilderException {
		// Schemas first
		for (Iterator i = this.schemas.values().iterator(); i.hasNext();) {
			Schema s = (Schema) i.next();
			s.synchronise();
		}
		// Then, synchronise datasets.
		this.synchroniseDataSets();
	}

	/**
	 * Request that all datasets be constructed into actual marts now.
	 * 
	 * @throws SQLException
	 *             if there was any data source error during mart construction.
	 * @throws BuilderException
	 *             if there was any other kind of error in the mart construction
	 *             process.
	 */
	public void constructMart() throws BuilderException, SQLException {
		for (Iterator i = this.datasets.values().iterator(); i.hasNext();) {
			DataSet ds = (DataSet) i.next();
			ds.constructMart();
		}
	}
}
