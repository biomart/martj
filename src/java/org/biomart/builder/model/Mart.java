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
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.resources.Resources;

/**
 * The mart contains the set of all schemas that are providing data to this
 * mart. It also has zero or more datasets based around these.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.20, 26th July 2006
 * @since 0.1
 */
public class Mart {
	// OK to use map, as keys are strings and never change.
	// Use tree map to keep them in alphabetical order.
	private final Map schemas = new TreeMap();

	// OK to use map, as keys are strings and never change.
	// Use tree map to keep them in alphabetical order.
	private final Map datasets = new TreeMap();

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
	 * Given a set of tables, produce the minimal set of datasets which include
	 * all the specified tables. Tables can be included in the same dataset if
	 * they are linked by 1:M relations (1:M, 1:M in a chain), or if the table
	 * is the last in the chain and is linked to the previous table by a pair of
	 * 1:M and M:1 relations via a third table, simulating a M:M relation.
	 * <p>
	 * If the chains of tables fork, then one dataset is generated for each
	 * branch of the fork.
	 * <p>
	 * Every suggested dataset is synchronised before being returned.
	 * <p>
	 * Datasets will be named after their central tables. If a dataset with that
	 * name already exists, a '_' and sequence number will be appended to make
	 * the new dataset name unique.
	 * 
	 * @param includeTables
	 *            the tables that must appear in the final set of datasets.
	 * @return the collection of datasets generated.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws AssociationException
	 *             if any of the tables do not belong to this mart.
	 * @throws AlreadyExistsException
	 *             if a dataset already exists in this schema with the same name
	 *             or any of the suffixed versions.
	 * @throws BuilderException
	 *             if synchronisation fails.
	 */
	public Collection suggestDataSets(Collection includeTables)
			throws SQLException, AssociationException, AlreadyExistsException,
			BuilderException {
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
		Set suggestedDataSets = new HashSet();
		for (Iterator i = rootTables.iterator(); i.hasNext();) {
			Table rootTable = (Table) i.next();
			DataSet dataset = new DataSet(this, rootTable, rootTable.getName());
			// Process it.
			List tablesIncluded = new ArrayList();
			tablesIncluded.add(rootTable);
			suggestedDataSets.addAll(this.continueSubclassing(includeTables,
					tablesIncluded, dataset, rootTable));
		}

		// Remove all datasets that have all their subclassed relations
		// also subclassed in some other dataset.
		for (Iterator i = suggestedDataSets.iterator(); i.hasNext();) {
			DataSet suggestedDataSet = (DataSet) i.next();
			if (suggestedDataSet.getSubclassedRelations().isEmpty())
				continue;
			boolean subsumed = false;
			for (Iterator j = suggestedDataSets.iterator(); j.hasNext()
					&& !subsumed;) {
				DataSet candidate = (DataSet) j.next();
				if (candidate.equals(suggestedDataSet))
					continue;
				subsumed = candidate.getSubclassedRelations().containsAll(
						suggestedDataSet.getSubclassedRelations());
			}
			if (subsumed) {
				this.removeDataSet(suggestedDataSet);
				i.remove();
			}
		}

		// Synchronise them all.
		for (Iterator i = suggestedDataSets.iterator(); i.hasNext();)
			((DataSet) i.next()).synchronise();

		// Return the final set of suggested datasets.
		return suggestedDataSets;
	}

	private Collection continueSubclassing(Collection includeTables,
			Collection tablesIncluded, DataSet dataset, Table table)
			throws AssociationException {
		// Check table has a primary key.
		Key pk = table.getPrimaryKey();

		// Make a unique set to hold all the resulting datasets. It
		// is initially empty.
		Set suggestedDataSets = new HashSet();
		// Make a set to contain relations to subclass.
		Set subclassedRelations = new HashSet();
		// Make a map to hold tables included for each relation.
		Map relationTablesIncluded = new HashMap();
		// Make a list to hold all tables included at this level.
		Set localTablesIncluded = new HashSet(tablesIncluded);

		// Find all 1:M relations starting from the given table that point
		// to another interesting table.
		if (pk != null)
			for (Iterator i = pk.getRelations().iterator(); i.hasNext();) {
				Relation r = (Relation) i.next();
				if (!r.isOneToMany())
					continue;
				else if (r.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT))
					continue;

				// For each relation, if it points to another included
				// table via 1:M we should subclass the relation.
				Table target = r.getManyKey().getTable();
				if (includeTables.contains(target)
						&& !localTablesIncluded.contains(target)) {
					subclassedRelations.add(r);
					List newRelationTablesIncluded = new ArrayList(tablesIncluded);
					relationTablesIncluded.put(r,newRelationTablesIncluded);
					newRelationTablesIncluded.add(target);
					localTablesIncluded.add(target);
				}
			}

		// Find all 1:M:1 relations starting from the given table that point
		// to another interesting table.
		if (pk != null)
			for (Iterator i = pk.getRelations().iterator(); i.hasNext();) {
				Relation firstRel = (Relation) i.next();
				if (!firstRel.isOneToMany())
					continue;
				else if (firstRel.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT))
					continue;

				Table intermediate = firstRel.getManyKey().getTable();
				for (Iterator j = intermediate.getForeignKeys().iterator(); j
						.hasNext();) {
					Key fk = (Key) j.next();
					if (fk.getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
						continue;
					for (Iterator k = fk.getRelations().iterator(); k.hasNext();) {
						Relation secondRel = (Relation) k.next();
						if (secondRel.equals(firstRel))
							continue;
						else if (!secondRel.isOneToMany())
							continue;
						else if (secondRel.getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT))
							continue;
						// For each relation, if it points to another included
						// table via M:1 we should subclass the relation.
						Table target = secondRel.getOneKey().getTable();
						if (includeTables.contains(target)
								&& !localTablesIncluded.contains(target)) {
							subclassedRelations.add(firstRel);
							List newRelationTablesIncluded = new ArrayList(tablesIncluded);
							relationTablesIncluded.put(firstRel,newRelationTablesIncluded);
							newRelationTablesIncluded.add(target);
							localTablesIncluded.add(target);
						}
					}
				}
			}

		// No subclassing? Return a singleton.
		if (subclassedRelations.isEmpty())
			return Collections.singleton(dataset);

		// Iterate through the relations we found and recurse.
		// If not the last one, we copy the original dataset and
		// work on the copy, otherwise we work on the original.
		for (Iterator i = subclassedRelations.iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();
			DataSet suggestedDataSet = dataset;
			if (i.hasNext())
				suggestedDataSet = (DataSet) dataset.replicate(dataset
						.getName());
			suggestedDataSet.flagSubclassRelation(r);
			suggestedDataSets.addAll(this
					.continueSubclassing(includeTables, (List)relationTablesIncluded.get(r),
							suggestedDataSet, r.getManyKey().getTable()));
		}

		// Return the resulting datasets.
		return suggestedDataSets;
	}

	/**
	 * Given a dataset and a set of columns from one table upon which a table of
	 * that dataset is based, find all other tables which have similar columns,
	 * and create a new dataset for each one.
	 * <p>
	 * This method will not create datasets around tables which have already
	 * been used as the underlying table in any dataset table in the existing
	 * dataset. Neither will it create a dataset around the table from which the
	 * original columns came.
	 * <p>
	 * There may be no datasets resulting from this, if the columns do not
	 * appear elsewhere.
	 * <p>
	 * Datasets are synchronised before being returned.
	 * <p>
	 * Datasets will be named after their central tables. If a dataset with that
	 * name already exists, a '_' and sequence number will be appended to make
	 * the new dataset name unique.
	 * 
	 * @param dataset
	 *            the dataset the columns were selected from.
	 * @param columns
	 *            the columns to search across.
	 * @return the resulting set of datasets.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws AssociationException
	 *             if any of the tables do not belong to this mart.
	 * @throws AlreadyExistsException
	 *             if a dataset already exists in this schema with the same name
	 *             or any of the suffixed versions.
	 * @throws BuilderException
	 *             if synchronisation fails.
	 */
	public Collection suggestInvisibleDataSets(DataSet dataset,
			Collection columns) throws AssociationException,
			AlreadyExistsException, SQLException, BuilderException {
		List invisibleDataSets = new ArrayList();
		// Check the dataset belongs to us.
		if (!this.datasets.values().contains(dataset))
			throw new AssociationException(Resources
					.get("datasetSchemaMismatch"));
		// Check all the columns are from the same table.
		Table sourceTable = ((Column) columns.iterator().next()).getTable();
		for (Iterator i = columns.iterator(); i.hasNext();)
			if (!((Column) i.next()).getTable().equals(sourceTable))
				throw new AssociationException(Resources
						.get("invisibleNotAllSameTable"));
		// Find all tables which mention them.
		List candidates = new ArrayList();
		for (Iterator i = this.schemas.values().iterator(); i.hasNext();)
			for (Iterator j = ((Schema) i.next()).getTables().iterator(); j
					.hasNext();) {
				Table table = (Table) j.next();
				int matchingColCount = 0;
				for (Iterator k = columns.iterator(); k.hasNext();) {
					Column col = (Column) k.next();
					if (table.getColumnByName(col.getName()) != null
							|| table.getColumnByName(col.getName()
									+ Resources.get("foreignKeySuffix")) != null)
						matchingColCount++;
				}
				if (matchingColCount == columns.size())
					candidates.add(table);
			}
		// Remove from the found tables all those which are already
		// used, and the one from which the original columns came.
		candidates.remove(sourceTable);
		for (Iterator i = dataset.getTables().iterator(); i.hasNext();)
			candidates.remove(((DataSetTable) i.next()).getUnderlyingTable());
		// Generate the dataset for each.
		for (Iterator i = candidates.iterator(); i.hasNext();) {
			Table table = (Table) i.next();
			invisibleDataSets.add(new DataSet(this, table, table.getName()));
		}
		// Synchronise them all.
		for (Iterator i = invisibleDataSets.iterator(); i.hasNext();)
			((DataSet) i.next()).synchronise();
		// Return the results.
		return invisibleDataSets;
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
}
