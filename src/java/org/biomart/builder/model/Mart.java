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
import java.util.TreeSet;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.common.exceptions.BioMartError;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.Column;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Resources;

/**
 * The mart contains the set of all schemas that are providing data to this
 * mart. It also has zero or more datasets based around these.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class Mart {
	// OK to use map, as keys are strings and never change.
	// Use tree map to keep them in alphabetical order.
	private final Map datasets = new TreeMap();

	// OK to use map, as keys are strings and never change.
	// Use tree map to keep them in alphabetical order.
	private final Map schemas = new TreeMap();

	/**
	 * This internal method takes a bunch of tables that the user would like to
	 * see as subclass or main tables in a single dataset, and attempts to find
	 * a subclass path between them. For each subclass path it can build, it
	 * produces one dataset based on that path. Each path contains as many
	 * tables as possible. The paths do not overlap. If there is a choice, the
	 * one chosen is arbitrary.
	 * 
	 * @param includeTables
	 *            the tables we want to include as main or subclass tables.
	 * @param tablesIncluded
	 *            the tables we have managed to include in a path so far.
	 * @param dataset
	 *            the dataset we started out from which contains just the main
	 *            table on its own with no subclassing.
	 * @param table
	 *            the real table we are looking at to see if there is a subclass
	 *            path between any of the include tables and any of the existing
	 *            subclassed or main tables via this real table.
	 * @return the datasets we have created - one per subclass path, or if there
	 *         were none, then a singleton collection containing the dataset
	 *         originally passed in.
	 */
	private Collection continueSubclassing(final Collection includeTables,
			final Collection tablesIncluded, final DataSet dataset,
			final Table table) {
		// Check table has a primary key.
		final Key pk = table.getPrimaryKey();

		// Make a unique set to hold all the resulting datasets. It
		// is initially empty.
		final Set suggestedDataSets = new HashSet();
		// Make a set to contain relations to subclass.
		final Set subclassedRelations = new HashSet();
		// Make a map to hold tables included for each relation.
		final Map relationTablesIncluded = new HashMap();
		// Make a list to hold all tables included at this level.
		final Set localTablesIncluded = new HashSet(tablesIncluded);

		// Find all 1:M relations starting from the given table that point
		// to another interesting table.
		if (pk != null)
			for (final Iterator i = pk.getRelations().iterator(); i.hasNext();) {
				final Relation r = (Relation) i.next();
				if (!r.isOneToMany())
					continue;
				else if (r.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT))
					continue;

				// For each relation, if it points to another included
				// table via 1:M we should subclass the relation.
				final Table target = r.getManyKey().getTable();
				if (includeTables.contains(target)
						&& !localTablesIncluded.contains(target)) {
					subclassedRelations.add(r);
					final List newRelationTablesIncluded = new ArrayList(
							tablesIncluded);
					relationTablesIncluded.put(r, newRelationTablesIncluded);
					newRelationTablesIncluded.add(target);
					localTablesIncluded.add(target);
				}
			}

		// Find all 1:M:1 relations starting from the given table that point
		// to another interesting table.
		if (pk != null)
			for (final Iterator i = pk.getRelations().iterator(); i.hasNext();) {
				final Relation firstRel = (Relation) i.next();
				if (!firstRel.isOneToMany())
					continue;
				else if (firstRel.getStatus().equals(
						ComponentStatus.INFERRED_INCORRECT))
					continue;

				final Table intermediate = firstRel.getManyKey().getTable();
				for (final Iterator j = intermediate.getForeignKeys()
						.iterator(); j.hasNext();) {
					final Key fk = (Key) j.next();
					if (fk.getStatus().equals(
							ComponentStatus.INFERRED_INCORRECT))
						continue;
					for (final Iterator k = fk.getRelations().iterator(); k
							.hasNext();) {
						final Relation secondRel = (Relation) k.next();
						if (secondRel.equals(firstRel))
							continue;
						else if (!secondRel.isOneToMany())
							continue;
						else if (secondRel.getStatus().equals(
								ComponentStatus.INFERRED_INCORRECT))
							continue;
						// For each relation, if it points to another included
						// table via M:1 we should subclass the relation.
						final Table target = secondRel.getOneKey().getTable();
						if (includeTables.contains(target)
								&& !localTablesIncluded.contains(target)) {
							subclassedRelations.add(firstRel);
							final List newRelationTablesIncluded = new ArrayList(
									tablesIncluded);
							relationTablesIncluded.put(firstRel,
									newRelationTablesIncluded);
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
		for (final Iterator i = subclassedRelations.iterator(); i.hasNext();) {
			final Relation r = (Relation) i.next();
			DataSet suggestedDataSet = dataset;
			if (i.hasNext())
				suggestedDataSet = (DataSet) dataset.replicate(dataset
						.getName());
			try {
				suggestedDataSet.flagSubclassRelation(r);
			} catch (ValidationException e) {
				// Eh? We asked for it, dammit!
				throw new BioMartError(e);
			}
			suggestedDataSets.addAll(this.continueSubclassing(includeTables,
					(List) relationTablesIncluded.get(r), suggestedDataSet, r
							.getManyKey().getTable()));
		}

		// Return the resulting datasets.
		return suggestedDataSets;
	}

	/**
	 * Adds a dataset to the mart.
	 * 
	 * @param dataset
	 *            the dataset to add.
	 */
	public void addDataSet(final DataSet dataset) {
		String name = dataset.getName();
		final String baseName = dataset.getName();
		// Check we don't have one by this name already. Alias if we do.
		for (int i = 1; this.datasets.containsKey(name); name = baseName + "_"
				+ i++)
			;
		dataset.setName(name);
		// Add it.
		this.datasets.put(dataset.getName(), dataset);
	}

	/**
	 * Adds a schema to the set which this mart includes. It is renamed if one
	 * with that name already exists.
	 * 
	 * @param schema
	 *            the schema to add.
	 */
	public void addSchema(final Schema schema) {
		String name = schema.getName();
		final String baseName = schema.getName();
		// Check we don't have one by this name already. Alias if we do.
		for (int i = 1; this.schemas.containsKey(name); name = baseName + "_"
				+ i++)
			;
		schema.setName(name);
		// Add it.
		this.schemas.put(name, schema);
	}

	/**
	 * Returns the dataset object with the given name.
	 * 
	 * @param name
	 *            the name to look for.
	 * @return a dataset object matching the specified name.
	 */
	public DataSet getDataSetByName(final String name) {
		return (DataSet) this.datasets.get(name);
	}

	/**
	 * Returns the set of dataset objects which this mart includes. The set may
	 * be empty but it is never <tt>null</tt>.
	 * 
	 * @return a set of dataset objects.
	 */
	public Collection getDataSets() {
		return this.datasets.values();
	}

	/**
	 * Returns the schema object with the given name. If it doesn't exist,
	 * <tt>null</tt> is returned.
	 * 
	 * @param name
	 *            the name to look for.
	 * @return a schema object matching the specified name.
	 */
	public Schema getSchemaByName(final String name) {
		return (Schema) this.schemas.get(name);
	}

	/**
	 * Returns the set of schema objects which this mart includes. The set may
	 * be empty but it is never <tt>null</tt>.
	 * 
	 * @return a set of schema objects.
	 */
	public Collection getSchemas() {
		return this.schemas.values();
	}

	/**
	 * Removes a dataset from the set which this mart includes.
	 * 
	 * @param dataset
	 *            the dataset to remove.
	 */
	public void removeDataSet(final DataSet dataset) {
		this.datasets.remove(dataset.getName());
	}

	/**
	 * Removes a schema from the set which this mart includes. Any datasets
	 * centred on this schema are also removed, and any external relations
	 * referring to it also.
	 * 
	 * @param schema
	 *            the schema to remove.
	 */
	public void removeSchema(final Schema schema) {
		final List datasets = new ArrayList(this.getDataSets());
		for (final Iterator i = datasets.iterator(); i.hasNext();) {
			final DataSet ds = (DataSet) i.next();
			if (ds.getCentralTable().getSchema().equals(schema))
				this.removeDataSet(ds);
		}
		for (final Iterator i = schema.getExternalRelations().iterator(); i
				.hasNext();)
			((Relation) i.next()).destroy();
		this.schemas.remove(schema.getName());
	}

	/**
	 * Renames a dataset. This call cascades to the dataset itself and renames
	 * that too.
	 * 
	 * @param dataset
	 *            the dataset to rename.
	 * @param name
	 *            the new name for it.
	 */
	public void renameDataSet(final DataSet dataset, String name) {
		final String baseName = name;
		// Check we don't have one by this name already. Alias if we do.
		for (int i = 1; this.datasets.containsKey(name)
				&& !name.equals(dataset.getName()); name = baseName + "_" + i++)
			;
		// Rename it.
		this.datasets.remove(dataset.getName());
		dataset.setName(name);
		this.datasets.put(name, dataset);
	}

	/**
	 * Renames a schema. This call cascades to the schema and renames that as
	 * well. If the name clashes, it is altered until it does not.
	 * 
	 * @param schema
	 *            the schema to rename.
	 * @param name
	 *            the new name for it.
	 */
	public void renameSchema(final Schema schema, String name) {
		final String baseName = name;
		// Check we don't have one by this name already. Alias if we do.
		for (int i = 1; this.schemas.containsKey(name)
				&& !name.equals(schema.getName()); name = baseName + "_" + i++)
			;
		// Rename it.
		this.schemas.remove(schema.getName());
		schema.setName(name);
		this.schemas.put(name, schema);
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
	 * <p>
	 * See also
	 * {@link #continueSubclassing(Collection, Collection, DataSet, Table)}.
	 * 
	 * @param includeTables
	 *            the tables that must appear in the final set of datasets.
	 * @return the collection of datasets generated.
	 * @throws SQLException
	 *             if there is any problem talking to the source database whilst
	 *             generating the dataset.
	 * @throws DataModelException
	 *             if synchronisation fails.
	 */
	public Collection suggestDataSets(final Collection includeTables)
			throws SQLException, DataModelException {
		// The root tables are all those which do not have a M:1 relation
		// to another one of the initial set of tables. This means that
		// extra datasets will be created for each table at the end of
		// 1:M:1 relation, so that any further tables past it will still
		// be included.
		final List rootTables = new ArrayList(includeTables);
		for (final Iterator i = includeTables.iterator(); i.hasNext();) {
			final Table candidate = (Table) i.next();
			for (final Iterator j = candidate.getRelations().iterator(); j
					.hasNext();) {
				final Relation rel = (Relation) j.next();
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
		final Set suggestedDataSets = new TreeSet();
		for (final Iterator i = rootTables.iterator(); i.hasNext();) {
			final Table rootTable = (Table) i.next();
			final DataSet dataset = new DataSet(this, rootTable, rootTable
					.getName());
			// Process it.
			final List tablesIncluded = new ArrayList();
			tablesIncluded.add(rootTable);
			suggestedDataSets.addAll(this.continueSubclassing(includeTables,
					tablesIncluded, dataset, rootTable));
		}

		// Synchronise them all.
		for (final Iterator i = suggestedDataSets.iterator(); i.hasNext();)
			((DataSet) i.next()).synchronise();

		// Do any of the resulting datasets contain all the tables
		// exactly with subclass relations between each?
		// If so, just use that one dataset and forget the rest.
		DataSet perfectDS = null;
		for (final Iterator i = suggestedDataSets.iterator(); i.hasNext()
				&& perfectDS == null;) {
			final DataSet candidate = (DataSet) i.next();

			// A candidate is a perfect match if the set of tables
			// covered by the subclass relations is the same as the
			// original set of tables requested.
			final Set scTables = new HashSet();
			for (final Iterator j = candidate.getSubclassedRelations()
					.iterator(); j.hasNext();) {
				final Relation r = (Relation) j.next();
				scTables.add(r.getFirstKey().getTable());
				scTables.add(r.getSecondKey().getTable());
			}
			if (scTables.size() == includeTables.size()
					&& scTables.containsAll(includeTables))
				perfectDS = candidate;
		}
		if (perfectDS != null) {
			// Drop the others.
			for (final Iterator i = suggestedDataSets.iterator(); i.hasNext();) {
				final DataSet candidate = (DataSet) i.next();
				if (!candidate.equals(perfectDS)) {
					this.removeDataSet(candidate);
					i.remove();
				}
			}
			// Rename it to lose any extension it may have gained.
			this
					.renameDataSet(perfectDS, perfectDS.getCentralTable()
							.getName());
		}

		// Return the final set of suggested datasets.
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
	 * @throws DataModelException
	 *             if synchronisation fails.
	 */
	public Collection suggestInvisibleDataSets(final DataSet dataset,
			final Collection columns) throws SQLException, DataModelException {
		final List invisibleDataSets = new ArrayList();
		final Table sourceTable = ((Column) columns.iterator().next())
				.getTable();
		// Find all tables which mention the columns specified.
		final List candidates = new ArrayList();
		for (final Iterator i = this.schemas.values().iterator(); i.hasNext();)
			for (final Iterator j = ((Schema) i.next()).getTables().iterator(); j
					.hasNext();) {
				final Table table = (Table) j.next();
				int matchingColCount = 0;
				for (final Iterator k = columns.iterator(); k.hasNext();) {
					final Column col = (Column) k.next();
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
		for (final Iterator i = dataset.getTables().iterator(); i.hasNext();)
			candidates.remove(((DataSetTable) i.next()).getUnderlyingTable());
		// Generate the dataset for each.
		for (final Iterator i = candidates.iterator(); i.hasNext();) {
			final Table table = (Table) i.next();
			invisibleDataSets.add(new DataSet(this, table, table.getName()));
		}
		// Synchronise them all and make them all invisible.
		for (final Iterator i = invisibleDataSets.iterator(); i.hasNext();) {
			final DataSet ds = (DataSet) i.next();
			ds.setInvisible(true);
			ds.synchronise();
		}
		// Return the results.
		return invisibleDataSets;
	}

	/**
	 * Synchronise all datasets in this mart so that they match up with the
	 * mart's schema(s). Any datasets that are based on now-missing tables are
	 * dropped. This is all simply a matter of delegating calls and the routine
	 * does no real work itself.
	 */
	public void synchroniseDataSets() {
		for (final Iterator i = this.datasets.values().iterator(); i.hasNext();) {
			final DataSet ds = (DataSet) i.next();

			// Has the table gone?
			if (!this.getSchemas().contains(ds.getCentralTable().getSchema()))
				i.remove();

			// If not, synchronise it.
			else
				try {
					ds.synchronise();
				} catch (final Throwable t) {
					throw new BioMartError(t);
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
	 * @throws DataModelException
	 *             if there was any other kind of problem.
	 */
	public void synchroniseSchemas() throws SQLException, DataModelException {
		// Schemas first
		for (final Iterator i = this.schemas.values().iterator(); i.hasNext();) {
			final Schema s = (Schema) i.next();
			s.synchronise();
		}
		// Then, synchronise datasets.
		this.synchroniseDataSets();
	}
}
