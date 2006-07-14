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
import org.biomart.builder.resources.Resources;

/**
 * The mart contains the set of all schemas that are providing data to this
 * mart. It also has zero or more datasets based around these.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.14, 13th July 2006
 * @since 0.1
 */
public class Mart {
	// OK to use map, as keys are strings and never change.
	// Use tree map to keep them in alphabetical order.
	private final Map schemas = new TreeMap();

	// OK to use map, as keys are strings and never change.
	// Use tree map to keep them in alphabetical order.
	private final Map datasets = new TreeMap();

	// The suffix iterator generates suffix values for datasets.
	// Care must be taken not to allow two datasets to be generated
	// in parallel else the suffix iterator may get altered in odd ways.
	private int suffix;

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
			throw new AlreadyExistsException(Resources.get("schemaExists"),
					schema.getName());

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
			throw new AlreadyExistsException(Resources.get("schemaExists"),
					schema.getName());
		if (!this.schemas.containsValue(schema))
			throw new AssociationException(Resources.get("schemaMartMismatch"));

		// Rename it.
		this.schemas.remove(schema.getName());
		schema.setName(name);
		this.schemas.put(name, schema);
	}

	/**
	 * Removes a schema from the set which this mart includes. Any datasets
	 * centred on this schema are also removed, and any external relations
	 * referring to it also.
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
		for (Iterator i = schema.getExternalRelations().iterator(); i.hasNext();)
			((Relation) i.next()).destroy();
		this.schemas.remove(schema.getName());
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
			throw new AlreadyExistsException(Resources.get("datasetExists"),
					dataset.getName());

		// Add it.
		this.datasets.put(dataset.getName(), dataset);
	}

	/**
	 * Given some tables and a mode, suggest some datasets for those tables. The
	 * method is synchronized so that the static suffix counter is protected
	 * over each iteration.
	 * 
	 * @param mode
	 *            the mode for suggestion.
	 * @param tables
	 *            the tables to suggest datasets for.
	 * @param name
	 *            the name to give the suggested datasets.
	 * @return the collection of datasets generated.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws AssociationException
	 *             if any of the tables do not belong to this mart.
	 * @throws AlreadyExistsException
	 *             if a dataset already exists in this schema with the same name
	 *             or any of the suffixed versions.
	 */
	public synchronized Collection suggestDataSets(DataSetSuggestionMode mode,
			Collection tables, String name) throws SQLException,
			AssociationException, AlreadyExistsException {
		this.suffix = 1;
		if (mode.equals(DataSetSuggestionMode.IDENTITY))
			return this.suggestIdentityDataSets(tables, name);
		else if (mode.equals(DataSetSuggestionMode.SIMPLE))
			return this.suggestSimpleDataSets(tables, name);
		else if (mode.equals(DataSetSuggestionMode.COMBINED))
			return this.suggestCompoundDataSets(tables, name);
		return Collections.EMPTY_LIST;
	}

	/**
	 * Given a particular table, automatically create a number of datasets based
	 * around that table. Each table created will be optimised automatically.
	 * 
	 * @param tables
	 *            the tables to build predicted datasets around.
	 * @param name
	 *            the name to use for the datasets.
	 * @return the newly created datasets, which will already have been added to
	 *         the mart.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws AssociationException
	 *             if any of the tables do not belong to this mart.
	 * @throws AlreadyExistsException
	 *             if a dataset already exists in this schema with the same name
	 *             or any of the suffixed versions.
	 */
	private Collection suggestIdentityDataSets(Collection tables, String name)
			throws SQLException, AssociationException, AlreadyExistsException {
		// Make a list to hold the new datasets.
		List newDataSets = new ArrayList();

		// Do once for each table.
		for (Iterator t = tables.iterator(); t.hasNext();) {
			Table centralTable = (Table) t.next();

			// Create the base-case no-subclass version.
			DataSet basicDS = new DataSet(this, centralTable, name + "_"
					+ (this.suffix++));
			basicDS.optimiseDataSet();
			newDataSets.add(basicDS);
		}

		// Return the set we just created.
		return newDataSets;
	}

	/**
	 * Given a particular table, automatically create a number of datasets based
	 * around that table that represent the various possible subclassing
	 * scenarios. It will always create one dataset (with the name given) that
	 * doesn't subclass anything. For every M:1 relation leading off the table,
	 * another dataset will be created containing that subclass relation. Each
	 * subclass dataset choice will have a number appended to it after an
	 * underscore, eg. '_1' ,'_2' etc. Each table created will be optimised
	 * automatically.
	 * 
	 * @param tables
	 *            the tables to build predicted datasets around.
	 * @param name
	 *            the name to use for the datasets.
	 * @return the newly created datasets, which will already have been added to
	 *         the mart.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws AssociationException
	 *             if any of the tables do not belong to this mart.
	 * @throws AlreadyExistsException
	 *             if a dataset already exists in this schema with the same name
	 *             or any of the suffixed versions.
	 */
	private Collection suggestSimpleDataSets(Collection tables, String name)
			throws SQLException, AssociationException, AlreadyExistsException {
		// Make a list to hold the new datasets.
		List newDataSets = new ArrayList();

		// Do once for each table.
		for (Iterator t = tables.iterator(); t.hasNext();) {
			Table centralTable = (Table) t.next();

			// Create the base-case no-subclass version.
			DataSet basicDS = new DataSet(this, centralTable, name + "_"
					+ (this.suffix++));
			basicDS.optimiseDataSet();
			newDataSets.add(basicDS);

			// Predict the subclass relations from the existing m:1 relations -
			// simple guesser based on finding foreign keys in the central
			// table.
			for (Iterator i = centralTable.getForeignKeys().iterator(); i
					.hasNext();) {
				Key k = (Key) i.next();
				for (Iterator j = k.getRelations().iterator(); j.hasNext();) {
					Relation r = (Relation) j.next();
					// Only flag potential M:1 subclass relations if they don't
					// refer back to ourselves.
					if (r.isOneToMany()
							&& !r.getOtherKey(k).getTable()
									.equals(centralTable)) {
						DataSet subclassedDS = new DataSet(this, centralTable,
								name + "_" + (this.suffix++));
						subclassedDS.flagSubclassRelation(r);
						subclassedDS.optimiseDataSet();
						newDataSets.add(subclassedDS);
					}
				}
			}
		}

		// Return the set we just created.
		return newDataSets;
	}

	/**
	 * Given a set of tables, produce the minimal set of datasets which include
	 * all the specified tables. Tables can be included in the same dataset if
	 * they are linked by 1:M relations (1:M, 1:M in a chain), or if the table
	 * is the last in the chain and is linked to the previous table by a pair of
	 * 1:M and M:1 relations via a third table, simulating a M:M relation.
	 * <p>
	 * If the chains of tables the datasets are built from fork, then one
	 * dataset is generated for each branch of the fork.
	 * 
	 * @param includeTables
	 *            the tables that must appear in the final set of datasets.
	 * @param name
	 *            the name to give each dataset. As each dataset is generated
	 *            beyond the first one, it will gain an '_x' suffix where 'x' is
	 *            the sequence number of the additional dataset.
	 * @return the collection of datasets generated.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws AssociationException
	 *             if any of the tables do not belong to this mart.
	 * @throws AlreadyExistsException
	 *             if a dataset already exists in this schema with the same name
	 *             or any of the suffixed versions.
	 */
	private Collection suggestCompoundDataSets(Collection includeTables,
			String name) throws SQLException, AssociationException,
			AlreadyExistsException {
		// The root tables are all those which do not have a M:1 relation
		// to another one of the initial set of tables. This means that
		// extra datasets will be created for each table at the end of
		// 1:M:1 relation, so that any further tables past it will still
		// be included.
		List rootTables = new ArrayList(includeTables);
		for (Iterator i = includeTables.iterator(); i.hasNext();) {
			Table candidate = (Table) i.next();
			for (Iterator j = candidate.getRelations().iterator(); j.hasNext();) {
				Relation rel = (Relation) j.next();
				if (rel.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
					continue;
				if (!rel.isOneToMany())
					continue;
				if (!rel.getManyKey().getTable().equals(candidate))
					continue;
				if (includeTables.contains(rel.getOneKey().getTable()))
					rootTables.remove(candidate);
			}
		}
		// We construct one dataset per root table.
		List suggestedDataSets = new ArrayList();
		for (Iterator i = rootTables.iterator(); i.hasNext();) {
			Table rootTable = (Table) i.next();
			DataSet dataset = new DataSet(this, rootTable, name + "_"
					+ (this.suffix++));
			// Process it.
			suggestedDataSets.add(this.continueSubclassing(includeTables,
					name, dataset, rootTable));
		}
		// Optimise all the results.
		for (Iterator i = suggestedDataSets.iterator(); i.hasNext();)
			((DataSet) i.next()).optimiseDataSet();
		// Return the final set of suggested datasets.
		return suggestedDataSets;
	}

	private DataSet continueSubclassing(Collection includeTables,
			String name, DataSet dataset, Table table)
			throws AssociationException {
		// Make a list to hold the recursion tables, and the potential 1:M:1
		// table/relation pairs.
		Set recursion = new HashSet();
		List potential = new ArrayList();
		// Find all 1:M relations starting from the given table.
		Key pk = table.getPrimaryKey();
		if (pk == null)
			return dataset;
		for (Iterator i = pk.getRelations().iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();
			if (!r.isOneToMany())
				continue;
			else if (r.getStatus().equals(ComponentStatus.INFERRED_INCORRECT))
				continue;
			// If any of them point to a table that should be subclassed,
			// them mark the relation. Recurse to the next table, and continue.
			Table target = r.getManyKey().getTable();
			if (includeTables.contains(target)) {
				dataset.flagSubclassRelation(r);
				recursion.add(target);
			}
			// If any of them point to a table that is not subclassed but has
			// an M:1 to one that should be, mark the relation as potential 
			// but do not recurse down that path. This allows us to
			// remove potential 1:M:1 relations if we find a direct 1:M that
			// would do the job better.
			else {
				Table potential1M1Table = null;
				for (Iterator j = target.getForeignKeys().iterator(); j
						.hasNext()
						&& potential1M1Table==null;) {
					Key fk = (Key) j.next();
					if (fk.getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
						continue;
					for (Iterator k = fk.getRelations().iterator(); k.hasNext()
							&& potential1M1Table==null;) {
						Relation fkr = (Relation) k.next();
						if (fkr.equals(r)) 
							continue;
						else if (!fkr.isOneToMany())
							continue;
						else if (fkr.getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT))
							continue;
						// This bit avoids 1:M:1 back to same table.
						else if (fkr.getOneKey().getTable().equals(r.getOneKey().getTable()))
							continue;
						else if (includeTables.contains(fkr.getOneKey().getTable()))
							potential1M1Table = fkr.getOneKey().getTable();
					}
				}
				if (potential1M1Table!=null) 
					potential.add(new Object[]{potential1M1Table, r});
			}
		}
		// Check the potential 1:M:1 relations to make sure the target
		// tables have not already been included (if they are, they'll be
		// in the recursion set).
		for (Iterator i = potential.iterator(); i.hasNext(); ) {
			Object[] obj = (Object[])i.next();
			Table potentialTable = (Table)obj[0];
			Relation potentialRel = (Relation)obj[1];
			if (!recursion.contains(potentialTable))
				dataset.flagSubclassRelation(potentialRel);
		}
		// Do the recursion.
		for (Iterator i = recursion.iterator(); i.hasNext();) 
			this.continueSubclassing(includeTables, name, dataset, (Table) i.next());
		// Return the resulting datasets.
		return dataset;
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
			throw new AlreadyExistsException(Resources.get("datasetExists"),
					dataset.getName());
		if (!this.datasets.containsValue(dataset))
			throw new AssociationException(Resources.get("datasetMartMismatch"));

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
		this.datasets.remove(dataset.getName());
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
	 * This class defines the various different ways of suggesting a dataset.
	 */
	public static class DataSetSuggestionMode implements Comparable {

		private final String name;

		/**
		 * Use this constant to refer to no optimisation.
		 */
		public static final DataSetSuggestionMode IDENTITY = new DataSetSuggestionMode(
				"SIMPLE");

		/**
		 * Use this constant to refer to optimising by including an extra column
		 * on the main table for each dimension and populating it with true or
		 * false..
		 */
		public static final DataSetSuggestionMode SIMPLE = new DataSetSuggestionMode(
				"SIMPLE");

		/**
		 * Use this constant to refer to no optimising by creating a separate
		 * table linked on a 1:1 basis with the main table, with one column per
		 * dimension populated with true or false.
		 */
		public static final DataSetSuggestionMode COMBINED = new DataSetSuggestionMode(
				"COMBINED");

		/**
		 * The private constructor takes a single parameter, which defines the
		 * name this suggestion mode object will display when printed.
		 * 
		 * @param name
		 *            the name of the suggestion mode.
		 */
		private DataSetSuggestionMode(String name) {
			this.name = name;
		}

		/**
		 * Displays the name of this suggestion mode object.
		 * 
		 * @return the name of this suggestion mode object.
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
			DataSetSuggestionMode c = (DataSetSuggestionMode) o;
			return this.toString().compareTo(c.toString());
		}

		public boolean equals(Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}
}
