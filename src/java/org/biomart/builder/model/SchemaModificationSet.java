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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.biomart.builder.exceptions.ValidationException;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition.RecursionType;
import org.biomart.common.model.Column;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * This class defines a set of modifications to a schema.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class SchemaModificationSet {
	private static final long serialVersionUID = 1L;

	/**
	 * The name to use for dataset-wide modifications.
	 */
	public static final String DATASET = "__DATASET_WIDE__";

	private final DataSet ds;

	// Linked because they must be kept in order for XML read/write.
	private final Collection subclassedRelations = new LinkedHashSet();

	private final Collection mergedRelations = new HashSet();

	private final Map maskedRelations = new HashMap();

	private final Map forceIncludeRelations = new HashMap();

	private final Map compoundRelations = new HashMap();

	private final Map directionalRelations = new HashMap();

	private final Map restrictedTables = new HashMap();

	private final Map restrictedRelations = new HashMap();

	private final Map concatRelations = new HashMap();

	/**
	 * Create a new empty set of modifications related to the given dataset.
	 * 
	 * @param ds
	 *            the dataset these modifications are for.
	 */
	public SchemaModificationSet(final DataSet ds) {
		this.ds = ds;
	}

	/**
	 * Mask a relation across the whole dataset.
	 * 
	 * @param relation
	 *            the relation.
	 */
	public void setMaskedRelation(final Relation relation) {
		this.setMaskedRelation(SchemaModificationSet.DATASET, relation);
	}

	/**
	 * Mask a relation for this dataset table only.
	 * 
	 * @param table
	 *            the table this affects.
	 * @param relation
	 *            the relation.
	 */
	public void setMaskedRelation(final DataSetTable table,
			final Relation relation) {
		this.setMaskedRelation(table.getName(), relation);
	}

	private void setMaskedRelation(final String tableName,
			final Relation relation) {
		if (!this.maskedRelations.containsKey(tableName))
			this.maskedRelations.put(tableName, new HashSet());
		final Collection masks = (Collection) this.maskedRelations
				.get(tableName);
		masks.add(relation);
	}

	/**
	 * Unmask a relation across the whole dataset.
	 * 
	 * @param relation
	 *            the relation.
	 */
	public void unsetMaskedRelation(final Relation relation) {
		this.unsetMaskedRelation(SchemaModificationSet.DATASET, relation);
	}

	/**
	 * Unmasks a relation for this dataset table only.
	 * 
	 * @param table
	 *            the table this affects.
	 * @param relation
	 *            the relation.
	 * @throws ValidationException
	 *             if it cannot be done for logical reasons.
	 */
	public void unsetMaskedRelation(final DataSetTable table,
			final Relation relation) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Collection globalMasks = (Collection) this.maskedRelations
				.get(SchemaModificationSet.DATASET);
		if (globalMasks != null && globalMasks.contains(relation))
			throw new ValidationException(Resources
					.get("relationMaskedGlobally"));
		this.unsetMaskedRelation(table.getName(), relation);
	}

	private void unsetMaskedRelation(final String tableName,
			final Relation relation) {
		// Skip already-unmasked relations.
		if (!this.isMaskedRelation(tableName, relation))
			return;
		if (this.maskedRelations.containsKey(tableName)) {
			final Collection masks = (Collection) this.maskedRelations
					.get(tableName);
			masks.remove(relation);
			if (masks.isEmpty())
				this.maskedRelations.remove(tableName);
		}
	}

	/**
	 * Check to see if the relation is masked for the given table.
	 * 
	 * @param table
	 *            the table to check for.
	 * @param relation
	 *            the relation to check.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isMaskedRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.isMaskedRelation(table == null ? SchemaModificationSet.DATASET
						: table.getName(), relation);
	}

	private boolean isMaskedRelation(final String tableName,
			final Relation relation) {
		final Collection globalMasks = (Collection) this.maskedRelations
				.get(SchemaModificationSet.DATASET);
		final Collection masks = (Collection) this.maskedRelations
				.get(tableName);
		return masks != null && masks.contains(relation) || globalMasks != null
				&& globalMasks.contains(relation);
	}

	/**
	 * Get a map of masked relations, where the keys are table names or the
	 * {@link #DATASET} constant, and the values are collections of relations.
	 * 
	 * @return the map.
	 */
	public Map getMaskedRelations() {
		return this.maskedRelations;
	}

	/**
	 * Mark the given relation as merged (ie. treat as 1:1) across the whole
	 * dataset.
	 * 
	 * @param rel
	 *            the relation.
	 */
	public void setMergedRelation(final Relation rel) {
		this.mergedRelations.add(rel);
	}

	/**
	 * Unmark the given relation as merged (ie. treat as 1:1) across the whole
	 * dataset.
	 * 
	 * @param rel
	 *            the relation.
	 */
	public void unsetMergedRelation(final Relation rel) {
		this.mergedRelations.remove(rel);
	}

	/**
	 * Check to see if the given relation has been merged.
	 * 
	 * @param rel
	 *            the relation.
	 * @return <tt>true</tt> if it has.
	 */
	public boolean isMergedRelation(final Relation rel) {
		return this.mergedRelations.contains(rel);
	}

	/**
	 * Return the collection of merged relations.
	 * 
	 * @return the collection.
	 */
	public Collection getMergedRelations() {
		return this.mergedRelations;
	}

	/**
	 * Restrict the given table across the whole dataset.
	 * 
	 * @param table
	 *            the table.
	 * @param restriction
	 *            the restriction.
	 */
	public void setRestrictedTable(final Table table,
			final RestrictedTableDefinition restriction) {
		this.setRestrictedTable(SchemaModificationSet.DATASET, table,
				restriction);
	}

	/**
	 * Restrict the given table for the specified dataset table only.
	 * 
	 * @param dsTable
	 *            the dataset table this affects.
	 * @param table
	 *            the table to restrict.
	 * @param restriction
	 *            the restriction.
	 */
	public void setRestrictedTable(final DataSetTable dsTable,
			final Table table, final RestrictedTableDefinition restriction) {
		this.setRestrictedTable(dsTable.getName(), table, restriction);
	}

	private void setRestrictedTable(final String dsTableName,
			final Table table, final RestrictedTableDefinition restriction) {
		if (!this.restrictedTables.containsKey(dsTableName))
			this.restrictedTables.put(dsTableName, new HashMap());
		final Map restrictions = (Map) this.restrictedTables.get(dsTableName);
		restrictions.put(table, restriction);
	}

	/**
	 * Unrestrict the given table across the whole dataset.
	 * 
	 * @param table
	 *            the table.
	 */
	public void unsetRestrictedTable(final Table table) {
		this.unsetRestrictedTable(SchemaModificationSet.DATASET, table);
	}

	/**
	 * Unrestrict the given table for the specified dataset table only.
	 * 
	 * @param dsTable
	 *            the dataset table this affects.
	 * @param table
	 *            the table.
	 * @throws ValidationException
	 *             if it cannot be done for logical reasons.
	 */
	public void unsetRestrictedTable(final DataSetTable dsTable,
			final Table table) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalRests = (Map) this.restrictedTables
				.get(SchemaModificationSet.DATASET);
		if (globalRests != null && globalRests.containsKey(table))
			throw new ValidationException(Resources
					.get("tableRestrictedGlobally"));
		this.unsetRestrictedTable(dsTable.getName(), table);
	}

	private void unsetRestrictedTable(final String dsTableName,
			final Table table) {
		// Skip already-unmasked relations.
		if (!this.isRestrictedTable(dsTableName, table))
			return;
		if (this.restrictedTables.containsKey(dsTableName)) {
			final Map rests = (Map) this.restrictedTables.get(dsTableName);
			rests.remove(table);
			if (rests.isEmpty())
				this.restrictedTables.remove(dsTableName);
		}
	}

	/**
	 * See if the table is restricted for the given dataset table.
	 * 
	 * @param dsTable
	 *            the dataset table to check in.
	 * @param table
	 *            the table to check.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isRestrictedTable(final DataSetTable dsTable,
			final Table table) {
		return this.isRestrictedTable(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), table);
	}

	private boolean isRestrictedTable(final String dsTableName,
			final Table table) {
		final Map globalRests = (Map) this.restrictedTables
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedTables.get(dsTableName);
		return rests != null && rests.containsKey(table) || globalRests != null
				&& globalRests.containsKey(table);
	}

	/**
	 * Obtain the definition for the given restricted table within the given
	 * dataset table.
	 * 
	 * @param dsTable
	 *            the dataset table to look in.
	 * @param table
	 *            the restricted table.
	 * @return the definition of the restriction.
	 */
	public RestrictedTableDefinition getRestrictedTable(
			final DataSetTable dsTable, final Table table) {
		return this.getRestrictedTable(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), table);
	}

	private RestrictedTableDefinition getRestrictedTable(
			final String dsTableName, final Table table) {
		if (!this.isRestrictedTable(dsTableName, table))
			return null;
		final Map globalRests = (Map) this.restrictedTables
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedTables.get(dsTableName);
		return rests != null && rests.containsKey(table) ? (RestrictedTableDefinition) rests
				.get(table)
				: (RestrictedTableDefinition) globalRests.get(table);
	}

	/**
	 * Return a map containing all table restrictions. Keys are dataset table
	 * names or the {@link #DATASET} constant. Values are also maps where the
	 * keys are table names and the values are restriction definitions.
	 * 
	 * @return the map.
	 */
	public Map getRestrictedTables() {
		return this.restrictedTables;
	}

	/**
	 * Mark the relation as restricted for the whole dataset.
	 * 
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to mark.
	 * @param restriction
	 *            the restriction definition.
	 * @throws ValidationException
	 *             if it could not be marked.
	 */
	public void setRestrictedRelation(final Relation relation, final int index,
			final RestrictedRelationDefinition restriction)
			throws ValidationException {
		this.setRestrictedRelation(SchemaModificationSet.DATASET, relation,
				index, restriction);
	}

	/**
	 * Mark the relation as restricted for the given dataset table only.
	 * 
	 * @param dsTable
	 *            the dataset table this affects.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to mark.
	 * @param restriction
	 *            the restriction definition.
	 * @throws ValidationException
	 *             if it could not be marked.
	 */
	public void setRestrictedRelation(final DataSetTable dsTable,
			final Relation relation, final int index,
			final RestrictedRelationDefinition restriction)
			throws ValidationException {
		this.setRestrictedRelation(dsTable.getName(), relation, index,
				restriction);
	}

	private void setRestrictedRelation(final String dsTableName,
			final Relation relation, final int index,
			final RestrictedRelationDefinition restriction)
			throws ValidationException {
		if (this.isConcatRelation(dsTableName, relation, index)
				&& this.getConcatRelation(dsTableName, relation, index)
						.getRecursionType() != RecursionType.NONE)
			throw new ValidationException(Resources
					.get("cannotConcatRecurseRestricted"));
		if (!this.restrictedRelations.containsKey(dsTableName))
			this.restrictedRelations.put(dsTableName, new HashMap());
		final Map restrictions = (Map) this.restrictedRelations
				.get(dsTableName);
		if (!restrictions.containsKey(relation))
			restrictions.put(relation, new HashMap());
		((Map) restrictions.get(relation)).put(new Integer(index), restriction);
	}

	/**
	 * Unmark the relation as restricted for the whole dataset.
	 * 
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to mark.
	 */
	public void unsetRestrictedRelation(final Relation relation, final int index) {
		this.unsetRestrictedRelation(SchemaModificationSet.DATASET, relation,
				index);
	}

	/**
	 * Unmark the relation as restricted for the given dataset table only.
	 * 
	 * @param dsTable
	 *            the dataset table this affects.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to unmark.
	 * @throws ValidationException
	 *             if it could not be unmarked.
	 */
	public void unsetRestrictedRelation(final DataSetTable dsTable,
			final Relation relation, final int index)
			throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		if (globalRests != null
				&& globalRests.containsKey(relation)
				&& ((Map) globalRests.get(relation)).containsKey(new Integer(
						index)))
			throw new ValidationException(Resources
					.get("relationRestrictedGlobally"));
		this.unsetRestrictedRelation(dsTable.getName(), relation, index);
	}

	private void unsetRestrictedRelation(final String dsTableName,
			final Relation relation, final int index) {
		if (!this.isRestrictedRelation(dsTableName, relation, index))
			return;
		if (this.restrictedRelations.containsKey(dsTableName)) {
			final Map rests = (Map) this.restrictedRelations.get(dsTableName);
			((Map) rests.get(relation)).remove(new Integer(index));
			if (((Map) rests.get(relation)).isEmpty())
				rests.remove(relation);
			if (rests.isEmpty())
				this.restrictedRelations.remove(dsTableName);
		}
	}

	/**
	 * Is this relation restricted in the given dataset table, on any of the
	 * compounded parts of the relation?
	 * 
	 * @param dsTable
	 *            the dataset table.
	 * @param relation
	 *            the relation.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isRestrictedRelation(final DataSetTable dsTable,
			final Relation relation) {
		return this.isRestrictedRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation);
	}

	private boolean isRestrictedRelation(final String dsTableName,
			final Relation relation) {
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedRelations.get(dsTableName);
		return rests != null && rests.containsKey(relation)
				|| globalRests != null && globalRests.containsKey(relation);
	}

	/**
	 * Is this relation restricted in the given dataset table on the given
	 * compound iteration?
	 * 
	 * @param dsTable
	 *            the dataset table.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the compound index.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isRestrictedRelation(final DataSetTable dsTable,
			final Relation relation, final int index) {
		return this.isRestrictedRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private boolean isRestrictedRelation(final String dsTableName,
			final Relation relation, final int index) {
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedRelations.get(dsTableName);
		return rests != null
				&& rests.containsKey(relation)
				&& ((Map) rests.get(relation)).containsKey(new Integer(index))
				|| globalRests != null
				&& globalRests.containsKey(relation)
				&& ((Map) globalRests.get(relation)).containsKey(new Integer(
						index));
	}

	/**
	 * Get the restriction definition.
	 * 
	 * @param dsTable
	 *            the dataset table to look in.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the compound relation index to check for.
	 * @return the definition.
	 */
	public RestrictedRelationDefinition getRestrictedRelation(
			final DataSetTable dsTable, final Relation relation, final int index) {
		return this.getRestrictedRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private RestrictedRelationDefinition getRestrictedRelation(
			final String dsTableName, final Relation relation, final int index) {
		if (!this.isRestrictedRelation(dsTableName, relation, index))
			return null;
		final Map globalRests = (Map) this.restrictedRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.restrictedRelations.get(dsTableName);
		return rests != null && rests.containsKey(relation)
				&& ((Map) rests.get(relation)).containsKey(new Integer(index)) ? (RestrictedRelationDefinition) ((Map) rests
				.get(relation)).get(new Integer(index))
				: (RestrictedRelationDefinition) ((Map) globalRests
						.get(relation)).get(new Integer(index));
	}

	/**
	 * Obtain all the restricted relation definitions. The keys of the map are
	 * dataset table names or the {@link #DATASET} constant. Values are also
	 * maps, where the keys are relations, and the values are more maps. These
	 * last maps have keys of compound relation index integers, and values of
	 * the restriction definitions.
	 * 
	 * @return the map.
	 */
	public Map getRestrictedRelations() {
		return this.restrictedRelations;
	}

	/**
	 * Get a unique name to use for the next concat column to be created.
	 * 
	 * @return the next concat column name.
	 */
	public String nextConcatColumn() {
		final Set used = new HashSet();
		for (final Iterator j = this.concatRelations.values().iterator(); j
				.hasNext();)
			for (final Iterator k = ((Map) j.next()).values().iterator(); k
					.hasNext();)
				for (final Iterator i = ((Map) k.next()).values().iterator(); i
						.hasNext();)
					used.add(((ConcatRelationDefinition) i.next()).getColKey());
		int i = 1;
		while (used.contains(Resources.get("concatColumnPrefix") + i))
			i++;
		return Resources.get("concatColumnPrefix") + i;
	}

	/**
	 * Mark the relation as concated for the whole dataset.
	 * 
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to mark.
	 * @param restriction
	 *            the concat definition.
	 * @throws ValidationException
	 *             if it could not be marked.
	 */
	public void setConcatRelation(final Relation relation, final int index,
			final ConcatRelationDefinition restriction)
			throws ValidationException {
		this.setConcatRelation(SchemaModificationSet.DATASET, relation, index,
				restriction);
	}

	/**
	 * Mark the relation as concated for the given dataset table only.
	 * 
	 * @param dsTable
	 *            the dataset table this affects.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to mark.
	 * @param restriction
	 *            the concat definition.
	 * @throws ValidationException
	 *             if it could not be marked.
	 */
	public void setConcatRelation(final DataSetTable dsTable,
			final Relation relation, final int index,
			final ConcatRelationDefinition restriction)
			throws ValidationException {
		this.setConcatRelation(dsTable.getName(), relation, index, restriction);
	}

	private void setConcatRelation(final String dsTableName,
			final Relation relation, final int index,
			final ConcatRelationDefinition restriction)
			throws ValidationException {
		if (!relation.isOneToMany())
			throw new ValidationException(Resources
					.get("cannotConcatNonOneMany"));
		if (this.isRestrictedRelation(dsTableName, relation)
				&& restriction.getRecursionType() != RecursionType.NONE)
			throw new ValidationException(Resources
					.get("cannotConcatRecurseRestricted"));
		if (this.isCompoundRelation(dsTableName, relation))
			throw new ValidationException(Resources.get("cannotConcatCompound"));
		if (!this.concatRelations.containsKey(dsTableName))
			this.concatRelations.put(dsTableName, new HashMap());
		final Map restrictions = (Map) this.concatRelations.get(dsTableName);
		if (!restrictions.containsKey(relation))
			restrictions.put(relation, new HashMap());
		((Map) restrictions.get(relation)).put(new Integer(index), restriction);
	}

	/**
	 * Unmark the relation as concated for the whole dataset.
	 * 
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to unmark.
	 */
	public void unsetConcatRelation(final Relation relation, final int index) {
		this
				.unsetConcatRelation(SchemaModificationSet.DATASET, relation,
						index);
	}

	/**
	 * Unmark the relation as concated for the given dataset table only.
	 * 
	 * @param dsTable
	 *            the dataset table this affects.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the index of a compounded relation to unmark.
	 * @throws ValidationException
	 *             if it could not be unmarked.
	 */
	public void unsetConcatRelation(final DataSetTable dsTable,
			final Relation relation, final int index)
			throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		if (globalRests != null
				&& globalRests.containsKey(relation)
				&& ((Map) globalRests.get(relation)).containsKey(new Integer(
						index)))
			throw new ValidationException(Resources
					.get("relationConcatedGlobally"));
		this.unsetConcatRelation(dsTable.getName(), relation, index);
	}

	private void unsetConcatRelation(final String dsTableName,
			final Relation relation, final int index) {
		if (!this.isConcatRelation(dsTableName, relation, index))
			return;
		if (this.concatRelations.containsKey(dsTableName)) {
			final Map rests = (Map) this.concatRelations.get(dsTableName);
			((Map) rests.get(relation)).remove(new Integer(index));
			if (((Map) rests.get(relation)).isEmpty())
				rests.remove(relation);
			if (rests.isEmpty())
				this.concatRelations.remove(dsTableName);
		}
	}

	/**
	 * Is this relation restricted in the given dataset table on any compound
	 * iteration?
	 * 
	 * @param dsTable
	 *            the dataset table.
	 * @param relation
	 *            the relation.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isConcatRelation(final DataSetTable dsTable,
			final Relation relation) {
		return this.isConcatRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation);
	}

	private boolean isConcatRelation(final String dsTableName,
			final Relation relation) {
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.concatRelations.get(dsTableName);
		return rests != null && rests.containsKey(relation)
				|| globalRests != null && globalRests.containsKey(relation);
	}

	/**
	 * Is this relation concated in the given dataset table on the given
	 * compound iteration?
	 * 
	 * @param dsTable
	 *            the dataset table.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the compound index.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isConcatRelation(final DataSetTable dsTable,
			final Relation relation, final int index) {
		return this.isConcatRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private boolean isConcatRelation(final String dsTableName,
			final Relation relation, final int index) {
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.concatRelations.get(dsTableName);
		return rests != null
				&& rests.containsKey(relation)
				&& ((Map) rests.get(relation)).containsKey(new Integer(index))
				|| globalRests != null
				&& globalRests.containsKey(relation)
				&& ((Map) globalRests.get(relation)).containsKey(new Integer(
						index));
	}

	/**
	 * Get the concat definition.
	 * 
	 * @param dsTable
	 *            the dataset table to look in.
	 * @param relation
	 *            the relation.
	 * @param index
	 *            the compound relation index to check for.
	 * @return the definition.
	 */
	public ConcatRelationDefinition getConcatRelation(
			final DataSetTable dsTable, final Relation relation, final int index) {
		return this.getConcatRelation(
				dsTable == null ? SchemaModificationSet.DATASET : dsTable
						.getName(), relation, index);
	}

	private ConcatRelationDefinition getConcatRelation(
			final String dsTableName, final Relation relation, final int index) {
		if (!this.isConcatRelation(dsTableName, relation, index))
			return null;
		final Map globalRests = (Map) this.concatRelations
				.get(SchemaModificationSet.DATASET);
		final Map rests = (Map) this.concatRelations.get(dsTableName);
		return rests != null && rests.containsKey(relation)
				&& ((Map) rests.get(relation)).containsKey(new Integer(index)) ? (ConcatRelationDefinition) ((Map) rests
				.get(relation)).get(new Integer(index))
				: (ConcatRelationDefinition) ((Map) globalRests.get(relation))
						.get(new Integer(index));
	}

	/**
	 * Obtain all the concated relation definitions. The keys of the map are
	 * dataset table names or the {@link #DATASET} constant. Values are also
	 * maps, where the keys are relations, and the values are more maps. These
	 * last maps have keys of compound relation index integers, and values of
	 * the restriction definitions.
	 * 
	 * @return the map.
	 */
	public Map getConcatRelations() {
		return this.concatRelations;
	}

	/**
	 * Force the transformation to include the given relation.
	 * 
	 * @param relation
	 *            the relation.
	 */
	public void setForceIncludeRelation(final Relation relation) {
		this.setForceIncludeRelation(SchemaModificationSet.DATASET, relation);
	}

	/**
	 * Force the transformation to include the given relation, but only as part
	 * of the given dataset table.
	 * 
	 * @param table
	 *            the dataset table.
	 * @param relation
	 *            the relation.
	 */
	public void setForceIncludeRelation(final DataSetTable table,
			final Relation relation) {
		this.setForceIncludeRelation(table.getName(), relation);
	}

	private void setForceIncludeRelation(final String tableName,
			final Relation relation) {
		if (!this.forceIncludeRelations.containsKey(tableName))
			this.forceIncludeRelations.put(tableName, new HashSet());
		final Collection masks = (Collection) this.forceIncludeRelations
				.get(tableName);
		masks.add(relation);
	}

	/**
	 * Unforce the relation dataset-wide.
	 * 
	 * @param relation
	 *            the relation to unforce.
	 */
	public void unsetForceIncludeRelation(final Relation relation) {
		this.unsetForceIncludeRelation(SchemaModificationSet.DATASET, relation);
	}

	/**
	 * Unforce the relation for the specific dataset table.
	 * 
	 * @param table
	 *            the dataset table.
	 * @param relation
	 *            the relation to unforce.
	 * @throws ValidationException
	 *             if it cannot do it for logical reasons.
	 */
	public void unsetForceIncludeRelation(final DataSetTable table,
			final Relation relation) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Collection globalIncs = (Collection) this.forceIncludeRelations
				.get(SchemaModificationSet.DATASET);
		if (globalIncs != null && globalIncs.contains(relation))
			throw new ValidationException(Resources
					.get("relationForcedGlobally"));
		this.unsetForceIncludeRelation(table.getName(), relation);
	}

	private void unsetForceIncludeRelation(final String tableName,
			final Relation relation) {
		// Skip already-unmasked relations.
		if (!this.isForceIncludeRelation(tableName, relation))
			return;
		if (this.forceIncludeRelations.containsKey(tableName)) {
			final Collection incs = (Collection) this.forceIncludeRelations
					.get(tableName);
			incs.remove(relation);
			if (incs.isEmpty())
				this.forceIncludeRelations.remove(tableName);
		}
	}

	/**
	 * Is ths relation forced for the given dataset table?
	 * 
	 * @param table
	 *            the dataset table to check.
	 * @param relation
	 *            the relation.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isForceIncludeRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.isForceIncludeRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private boolean isForceIncludeRelation(final String tableName,
			final Relation relation) {
		final Collection globalIncs = (Collection) this.forceIncludeRelations
				.get(SchemaModificationSet.DATASET);
		final Collection incs = (Collection) this.forceIncludeRelations
				.get(tableName);
		return incs != null && incs.contains(relation) || globalIncs != null
				&& globalIncs.contains(relation);
	}

	/**
	 * Get the map of forced relations. Keys are dataset table names or the
	 * {@link #DATASET} constant. Values are relations.
	 * 
	 * @return the map.
	 */
	public Map getForceIncludeRelations() {
		return this.forceIncludeRelations;
	}

	/**
	 * Subclass the given relation.
	 * 
	 * @param relation
	 *            the relation.
	 * @throws ValidationException
	 *             if it cannot be subclassed.
	 */
	public void setSubclassedRelation(final Relation relation)
			throws ValidationException {
		Log.debug("Flagging subclassed relation " + relation + " in "
				+ this.ds.getName());

		// Skip if already subclassed.
		if (this.isSubclassedRelation(relation))
			return;

		// Check that the relation is a 1:M relation and isn't a loop-back.
		if (!relation.isOneToMany())
			throw new ValidationException(Resources.get("subclassNotOneMany"));

		// Work out the child end of the relation - the M end. The parent is
		// the 1 end.
		final Table parentTable = relation.getOneKey().getTable();
		final Table childTable = relation.getManyKey().getTable();
		if (parentTable.equals(childTable))
			throw new ValidationException(Resources
					.get("subclassNotBetweenTwoTables"));
		if (parentTable.getPrimaryKey() == null
				|| childTable.getPrimaryKey() == null)
			throw new ValidationException(Resources.get("subclassTargetNoPK"));

		// We have existing subclass relations.
		if (!this.subclassedRelations.isEmpty()) {
			// We need to test if the selected relation links to
			// a table which itself has subclass relations, or
			// is the central table, and has not got an
			// existing subclass relation in the direction we
			// are working in.

			boolean childHasM1 = false;
			boolean parentHas1M = false;
			for (final Iterator i = this.subclassedRelations.iterator(); i
					.hasNext();) {
				final Relation rel = (Relation) i.next();
				if (rel.getOneKey().getTable().equals(parentTable))
					parentHas1M = true;
				else if (rel.getOneKey().getTable().equals(childTable)) {
				}
				if (rel.getManyKey().getTable().equals(parentTable)) {
				} else if (rel.getManyKey().getTable().equals(childTable))
					childHasM1 = true;
			}

			// If child has M:1 or parent has 1:M, we cannot do this.
			if (childHasM1 || parentHas1M)
				throw new ValidationException(Resources
						.get("mixedCardinalitySubclasses"));
		}

		// Mark the relation.
		this.subclassedRelations.add(relation);
	}

	/**
	 * Check to see if the relation is subclassed.
	 * 
	 * @param r
	 *            the relation.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isSubclassedRelation(final Relation r) {
		return this.subclassedRelations.contains(r);
	}

	/**
	 * Get a collection of all subclassed relations.
	 * 
	 * @return the collection.
	 */
	public Collection getSubclassedRelations() {
		return this.subclassedRelations;
	}

	/**
	 * Unsubclass a relation.
	 * 
	 * @param relation
	 *            the relation.
	 */
	public void unsetSubclassedRelation(final Relation relation) {
		Log.debug("Unflagging subclass relation " + relation + " in "
				+ this.ds.getName());
		if (!this.isSubclassedRelation(relation))
			return;
		// Break the chain first.
		final Key key = relation.getManyKey();
		if (key != null) {
			final Table target = key.getTable();
			if (!target.equals(this.ds.getCentralTable()))
				if (target.getPrimaryKey() != null)
					for (final Iterator i = target.getPrimaryKey()
							.getRelations().iterator(); i.hasNext();) {
						final Relation rel = (Relation) i.next();
						if (rel.isOneToMany())
							this.unsetSubclassedRelation(rel);
					}
		}
		// Then remove the head of the chain.
		this.subclassedRelations.remove(relation);
	}

	/**
	 * Set the relation to only be followed from a given key dataset-wide.
	 * 
	 * @param relation
	 *            the relation.
	 * @param def
	 *            the key to follow from.
	 * @throws ValidationException
	 *             if this cannot be done for logical reasons.
	 */
	public void setDirectionalRelation(final Relation relation, final Key def)
			throws ValidationException {
		this.setDirectionalRelation(SchemaModificationSet.DATASET, relation,
				def);
	}

	/**
	 * Set the relation to only be followed from a given key for the dataset
	 * table shown.
	 * 
	 * @param table
	 *            the table this affects.
	 * @param relation
	 *            the relation.
	 * @param def
	 *            the key to follow from.
	 * @throws ValidationException
	 *             if this cannot be done for logical reasons.
	 */
	public void setDirectionalRelation(final DataSetTable table,
			final Relation relation, final Key def) throws ValidationException {
		this.setDirectionalRelation(table.getName(), relation, def);
	}

	private void setDirectionalRelation(final String tableName,
			final Relation relation, final Key def) throws ValidationException {
		if (!this.directionalRelations.containsKey(tableName))
			this.directionalRelations.put(tableName, new HashMap());
		final Map masks = (Map) this.directionalRelations.get(tableName);
		masks.put(relation, def);
	}

	/**
	 * Make the relation bidirectional again dataset-wide.
	 * 
	 * @param relation
	 *            the relation.
	 */
	public void unsetDirectionalRelation(final Relation relation) {
		this.unsetDirectionalRelation(SchemaModificationSet.DATASET, relation);
	}

	/**
	 * Make the relation bidirectional for the given dataset table.
	 * 
	 * @param table
	 *            the table.
	 * @param relation
	 *            the relation.
	 * @throws ValidationException
	 *             if logically this cannot be done.
	 */
	public void unsetDirectionalRelation(final DataSetTable table,
			final Relation relation) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalComps = (Map) this.directionalRelations
				.get(SchemaModificationSet.DATASET);
		if (globalComps != null && globalComps.containsKey(relation))
			throw new ValidationException(Resources
					.get("relationDirectionaledGlobally"));
		this.unsetDirectionalRelation(table.getName(), relation);
	}

	private void unsetDirectionalRelation(final String tableName,
			final Relation relation) {
		// Skip already-unmasked relations.
		if (!this.isDirectionalRelation(tableName, relation))
			return;
		if (this.directionalRelations.containsKey(tableName)) {
			final Map comps = (Map) this.directionalRelations.get(tableName);
			comps.remove(relation);
			if (comps.isEmpty())
				this.directionalRelations.remove(tableName);
		}
	}

	/**
	 * Check to see if the relation is directional in the given table.
	 * 
	 * @param table
	 *            the table.
	 * @param relation
	 *            the relation.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isDirectionalRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.isDirectionalRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private boolean isDirectionalRelation(final String tableName,
			final Relation relation) {
		final Map globalComps = (Map) this.directionalRelations
				.get(SchemaModificationSet.DATASET);
		final Map comps = (Map) this.directionalRelations.get(tableName);
		return comps != null && comps.containsKey(relation)
				|| globalComps != null && globalComps.containsKey(relation);
	}

	/**
	 * Get the starting key for the directional relation in the given dataset
	 * table.
	 * 
	 * @param table
	 *            the table.
	 * @param relation
	 *            the relation.
	 * @return the starting key.
	 */
	public Key getDirectionalRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.getDirectionalRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private Key getDirectionalRelation(final String tableName,
			final Relation relation) {
		final Map globalComps = (Map) this.directionalRelations
				.get(SchemaModificationSet.DATASET);
		final Map comps = (Map) this.directionalRelations.get(tableName);
		return comps != null && comps.containsKey(relation) ? (Key) comps
				.get(relation) : (Key) globalComps.get(relation);
	}

	/**
	 * Get the map of all directional relations. Keys of the map are dataset
	 * table names or the {@link #DATASET} constant. Values are maps too, where
	 * the keys are relations, and the values are the starting keys.
	 * 
	 * @return the map.
	 */
	public Map getDirectionalRelations() {
		return this.directionalRelations;
	}

	/**
	 * Mark the relation as compound dataset-wide.
	 * 
	 * @param relation
	 *            the relation.
	 * @param def
	 *            the compound definition.
	 * @throws ValidationException
	 *             if it cannot be done logically.
	 */
	public void setCompoundRelation(final Relation relation,
			final CompoundRelationDefinition def) throws ValidationException {
		this.setCompoundRelation(SchemaModificationSet.DATASET, relation, def);
	}

	/**
	 * Mark the relation as compound for the dataset table given.
	 * 
	 * @param table
	 *            the table.
	 * @param relation
	 *            the relation.
	 * @param def
	 *            the compound definition.
	 * @throws ValidationException
	 *             if it cannot be done logically.
	 */
	public void setCompoundRelation(final DataSetTable table,
			final Relation relation, final CompoundRelationDefinition def)
			throws ValidationException {
		this.setCompoundRelation(table.getName(), relation, def);
	}

	private void setCompoundRelation(final String tableName,
			final Relation relation, final CompoundRelationDefinition def)
			throws ValidationException {
		if (this.isConcatRelation(tableName, relation))
			throw new ValidationException(Resources.get("cannotConcatCompound"));
		if (!this.compoundRelations.containsKey(tableName))
			this.compoundRelations.put(tableName, new HashMap());
		final Map masks = (Map) this.compoundRelations.get(tableName);
		masks.put(relation, def);
	}

	/**
	 * Unmark the relation as compound dataset-wide.
	 * 
	 * @param relation
	 *            the relation.
	 */
	public void unsetCompoundRelation(final Relation relation) {
		this.unsetCompoundRelation(SchemaModificationSet.DATASET, relation);
	}

	/**
	 * Unmark the relation as compound for the given table.
	 * 
	 * @param table
	 *            the table.
	 * @param relation
	 *            the relation.
	 * @throws ValidationException
	 *             if logically it cannot be done.
	 */
	public void unsetCompoundRelation(final DataSetTable table,
			final Relation relation) throws ValidationException {
		// Complain if asked to unmask globally masked relation.
		final Map globalComps = (Map) this.compoundRelations
				.get(SchemaModificationSet.DATASET);
		if (globalComps != null && globalComps.containsKey(relation))
			throw new ValidationException(Resources
					.get("relationCompoundedGlobally"));
		this.unsetCompoundRelation(table.getName(), relation);
	}

	private void unsetCompoundRelation(final String tableName,
			final Relation relation) {
		// Skip already-unmasked relations.
		if (!this.isCompoundRelation(tableName, relation))
			return;
		if (this.compoundRelations.containsKey(tableName)) {
			final Map comps = (Map) this.compoundRelations.get(tableName);
			comps.remove(relation);
			if (comps.isEmpty())
				this.compoundRelations.remove(tableName);
		}
	}

	/**
	 * Is the relation compound for the given table?
	 * 
	 * @param table
	 *            the table.
	 * @param relation
	 *            the relation.
	 * @return <tt>true</tt> if it is.
	 */
	public boolean isCompoundRelation(final DataSetTable table,
			final Relation relation) {
		return this
				.isCompoundRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private boolean isCompoundRelation(final String tableName,
			final Relation relation) {
		final Map globalComps = (Map) this.compoundRelations
				.get(SchemaModificationSet.DATASET);
		final Map comps = (Map) this.compoundRelations.get(tableName);
		return comps != null && comps.containsKey(relation)
				|| globalComps != null && globalComps.containsKey(relation);
	}

	/**
	 * Get the compound definition for this relation.
	 * 
	 * @param table
	 *            the table.
	 * @param relation
	 *            the relation.
	 * @return the definition.
	 */
	public CompoundRelationDefinition getCompoundRelation(
			final DataSetTable table, final Relation relation) {
		return this
				.getCompoundRelation(
						table == null ? SchemaModificationSet.DATASET : table
								.getName(), relation);
	}

	private CompoundRelationDefinition getCompoundRelation(
			final String tableName, final Relation relation) {
		final Map globalComps = (Map) this.compoundRelations
				.get(SchemaModificationSet.DATASET);
		final Map comps = (Map) this.compoundRelations.get(tableName);
		return comps != null && comps.containsKey(relation) ? (CompoundRelationDefinition) comps
				.get(relation)
				: (CompoundRelationDefinition) globalComps.get(relation);
	}

	/**
	 * Get a map of all compound relations. Keys are dataset table names or the
	 * {@link #DATASET} constant. Values are maps, where the keys are relations
	 * and the values are the definitions.
	 * 
	 * @return the map.
	 */
	public Map getCompoundRelations() {
		return this.compoundRelations;
	}

	/**
	 * Replicate this set of modifications into the target set.
	 * 
	 * @param target
	 *            the set to receive copies of all this set's modifications.
	 */
	public void replicate(final SchemaModificationSet target) {
		target.subclassedRelations.clear();
		target.subclassedRelations.addAll(this.subclassedRelations);
		target.maskedRelations.clear();
		target.maskedRelations.putAll(this.maskedRelations);
		target.forceIncludeRelations.clear();
		target.forceIncludeRelations.putAll(this.forceIncludeRelations);
		target.mergedRelations.clear();
		target.mergedRelations.addAll(this.mergedRelations);
		target.compoundRelations.clear();
		target.compoundRelations.putAll(this.compoundRelations);
		target.directionalRelations.clear();
		target.directionalRelations.putAll(this.directionalRelations);
		target.restrictedTables.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.restrictedTables.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.restrictedTables.put(entry.getKey(), new HashMap());
			((Map) target.restrictedTables.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
		target.restrictedRelations.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.restrictedRelations.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.restrictedRelations.put(entry.getKey(), new HashMap());
			for (final Iterator j = ((Map) entry.getValue()).entrySet()
					.iterator(); j.hasNext();) {
				final Map.Entry entry2 = (Map.Entry) j.next();
				((Map) target.restrictedRelations.get(entry.getKey())).put(
						entry2.getKey(), new HashMap());
				((Map) ((Map) target.restrictedRelations.get(entry.getKey()))
						.get(entry2.getKey())).putAll((Map) entry2.getValue());
			}
			((Map) target.restrictedRelations.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
		target.concatRelations.clear();
		// We have to use an iterator because of nested maps.
		for (final Iterator i = this.concatRelations.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			target.concatRelations.put(entry.getKey(), new HashMap());
			for (final Iterator j = ((Map) entry.getValue()).entrySet()
					.iterator(); j.hasNext();) {
				final Map.Entry entry2 = (Map.Entry) j.next();
				((Map) target.concatRelations.get(entry.getKey())).put(entry2
						.getKey(), new HashMap());
				((Map) ((Map) target.concatRelations.get(entry.getKey()))
						.get(entry2.getKey())).putAll((Map) entry2.getValue());
			}
			((Map) target.concatRelations.get(entry.getKey()))
					.putAll((Map) entry.getValue());
		}
	}

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class RestrictedTableDefinition {
		private static final long serialVersionUID = 1L;

		private Map aliases;

		private String expr;

		private boolean hard;

		/**
		 * This constructor gives the restriction an initial expression and a
		 * set of aliases. The expression may not be empty, and neither can the
		 * alias map.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
		 * @param hard
		 *            if this restriction is a hard restriction (inner join) as
		 *            opposed to a soft one (left join).
		 */
		public RestrictedTableDefinition(final String expr, final Map aliases,
				final boolean hard) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("tblRestrictMissingExpression"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("tblRestrictMissingAliases"));

			// Remember the settings.
			this.aliases = new HashMap();
			this.aliases.putAll(aliases);
			this.expr = expr;
			this.hard = hard;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getAliases() {
			return this.aliases;
		}

		/**
		 * Is this a hard restriction?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isHard() {
			return this.hard;
		}

		/**
		 * Returns the expression, <i>without</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @return the unsubstituted expression.
		 */
		public String getExpression() {
			return this.expr;
		}

		/**
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @param additionalRels
		 *            contains the map of table prefixes to use for tables
		 *            joined by relations off the primary table involved.
		 * @param tablePrefix
		 *            the prefix to use for the table in the expression.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final Map additionalRels,
				final String tablePrefix) {
			Log.debug("Calculating restricted table expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Object[] crPair = (Object[]) entry.getKey();
				final Relation rel = (Relation) crPair[0];
				final Column col = (Column) crPair[1];
				final String tp;
				if (rel == null)
					tp = tablePrefix;
				else
					tp = (String) additionalRels.get(rel);
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, tp + "." + col.getName());
			}
			Log.debug("Expression is: " + sub);
			return sub;
		}

		/**
		 * Return the set of additional relations involved in this expression.
		 * 
		 * @return the set of relations.
		 */
		public Collection getAdditionalRelations() {
			final Collection pairs = new HashSet();
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Object[] crPair = (Object[]) entry.getKey();
				if (crPair[0] == null)
					continue;
				pairs.add(crPair[0]);
			}
			return pairs;
		}

		/**
		 * The actual expression. The values from the alias maps will be used to
		 * refer to various columns. This value is RDBMS-specific.
		 * 
		 * @param expr
		 *            the actual expression to use.
		 */
		public void setExpression(final String expr) {
			this.expr = expr;
		}
	}

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class RestrictedRelationDefinition {
		private static final long serialVersionUID = 1L;

		private Map leftAliases;

		private Map rightAliases;

		private String expr;

		private boolean hard;

		/**
		 * This constructor gives the restriction an initial expression and a
		 * set of aliases. The expression may not be empty, and neither can the
		 * alias map.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param leftAliases
		 *            the aliases to use for columns on the LHS of the join.
		 * @param rightAliases
		 *            the aliases to use for columns on the RHS of the join.
		 * @param hard
		 *            <tt>true</tt> if this is a hard (inner) join as opposed
		 *            to a soft (left) join.
		 */
		public RestrictedRelationDefinition(final String expr,
				final Map leftAliases, final Map rightAliases,
				final boolean hard) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("relRestrictMissingExpression"));
			if (leftAliases == null || leftAliases.isEmpty()
					|| rightAliases == null || rightAliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("relRestrictMissingAliases"));

			// Remember the settings.
			this.leftAliases = new HashMap();
			this.leftAliases.putAll(leftAliases);
			this.rightAliases = new HashMap();
			this.rightAliases.putAll(rightAliases);
			this.expr = expr;
			this.hard = hard;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getLeftAliases() {
			return this.leftAliases;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getRightAliases() {
			return this.rightAliases;
		}

		/**
		 * Returns the expression, <i>without</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @return the unsubstituted expression.
		 */
		public String getExpression() {
			return this.expr;
		}

		/**
		 * Is this a hard-restriction?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isHard() {
			return this.hard;
		}

		/**
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @param leftTablePrefix
		 *            the prefix to use for the LHS table in the expression.
		 * @param rightTablePrefix
		 *            the prefix to use for the LHS table in the expression.
		 * @param leftIsDataSet
		 *            if the LHS side is a dataset table.
		 * @param mappingUnit
		 *            the transformation unit this restriction will use to
		 *            translate columns into dataset column equivalents.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final String leftTablePrefix,
				final String rightTablePrefix, final boolean leftIsDataSet,
				final TransformationUnit mappingUnit) {
			Log.debug("Calculating restricted table expression");
			String sub = this.expr;
			for (final Iterator i = this.leftAliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Column col = (Column) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, leftTablePrefix
						+ "."
						+ (leftIsDataSet ? mappingUnit.getDataSetColumnFor(col)
								.getModifiedName() : col.getName()));
			}
			for (final Iterator i = this.rightAliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Column col = (Column) entry.getKey();
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, rightTablePrefix
						+ "."
						+ (!leftIsDataSet ? mappingUnit
								.getDataSetColumnFor(col).getModifiedName()
								: col.getName()));
			}
			Log.debug("Expression is: " + sub);
			return sub;
		}

		/**
		 * The actual expression. The values from the alias maps will be used to
		 * refer to various columns. This value is RDBMS-specific.
		 * 
		 * @param expr
		 *            the actual expression to use.
		 */
		public void setExpression(final String expr) {
			this.expr = expr;
		}
	}

	/**
	 * Defines the restriction on a table, ie. a where-clause.
	 */
	public static class ConcatRelationDefinition {
		private static final long serialVersionUID = 1L;

		private Map aliases;

		private String expr;

		private String rowSep;

		private String colKey;

		private RecursionType recursionType;

		private Key recursionKey;

		private Relation firstRelation;

		private Relation secondRelation;

		private String concSep;

		/**
		 * This constructor gives the restriction an initial expression and a
		 * set of aliases. The expression may not be empty, and neither can the
		 * alias map.
		 * 
		 * @param expr
		 *            the expression to define for this restriction.
		 * @param aliases
		 *            the aliases to use for columns.
		 * @param rowSep
		 *            the row separator to put between concated rows.
		 * @param colKey
		 *            the name of the concated column.
		 * @param recursionType
		 *            the type of recursion to use, if any.
		 * @param recursionKey
		 *            the key to start recursing from.
		 * @param firstRelation
		 *            the first relation to recurse.
		 * @param secondRelation
		 *            the second relation to recurse (optional).
		 * @param concSep
		 *            the separator to put between recursed values.
		 */
		public ConcatRelationDefinition(final String expr, final Map aliases,
				final String rowSep, final String colKey,
				RecursionType recursionType, final Key recursionKey,
				final Relation firstRelation, final Relation secondRelation,
				final String concSep) {
			// Test for good arguments.
			if (expr == null || expr.trim().length() == 0)
				throw new IllegalArgumentException(Resources
						.get("concatRelMissingExpression"));
			if (aliases == null || aliases.isEmpty())
				throw new IllegalArgumentException(Resources
						.get("concatRelMissingAliases"));
			if (rowSep == null || rowSep.length() == 0)
				throw new IllegalArgumentException(Resources
						.get("concatRelMissingRowSep"));
			if (recursionType == null)
				recursionType = RecursionType.NONE;
			if (recursionType != RecursionType.NONE) {
				if (recursionKey == null)
					throw new IllegalArgumentException(Resources
							.get("concatRelMissingRecursionKey"));
				if (firstRelation == null)
					throw new IllegalArgumentException(Resources
							.get("concatRelMissingFirstRelation"));
				if (secondRelation == null
						&& !firstRelation.getFirstKey().getTable().equals(
								firstRelation.getSecondKey().getTable()))
					throw new IllegalArgumentException(Resources
							.get("concatRelMissingSecondRelation"));
				if (concSep == null || concSep.length() == 0)
					throw new IllegalArgumentException(Resources
							.get("concatRelMissingConcSep"));
			}

			// Remember the settings.
			this.aliases = new HashMap();
			this.aliases.putAll(aliases);
			this.expr = expr;
			this.rowSep = rowSep;
			this.colKey = colKey;
			this.recursionType = recursionType;
			this.recursionKey = recursionKey;
			this.firstRelation = firstRelation;
			this.secondRelation = secondRelation;
			this.concSep = concSep;
		}

		/**
		 * Retrieves the map used for setting up aliases.
		 * 
		 * @return the aliases map. Keys must be {@link Column} instances, and
		 *         values are aliases used in the expression.
		 */
		public Map getAliases() {
			return this.aliases;
		}

		/**
		 * Returns the expression, <i>without</i> substitution. This value is
		 * RDBMS-specific.
		 * 
		 * @return the unsubstituted expression.
		 */
		public String getExpression() {
			return this.expr;
		}

		/**
		 * @return the column name.
		 */
		public String getColKey() {
			return this.colKey;
		}

		/**
		 * @return the expression.
		 */
		public String getExpr() {
			return this.expr;
		}

		/**
		 * @return the first recursion relation.
		 */
		public Relation getFirstRelation() {
			return this.firstRelation;
		}

		/**
		 * @return the recursion key.
		 */
		public Key getRecursionKey() {
			return this.recursionKey;
		}

		/**
		 * @return the recursion type.
		 */
		public RecursionType getRecursionType() {
			return this.recursionType;
		}

		/**
		 * @return the second recursion relation.
		 */
		public Relation getSecondRelation() {
			return this.secondRelation;
		}

		/**
		 * @return the row separator.
		 */
		public String getRowSep() {
			return this.rowSep;
		}

		/**
		 * @return the separator to use between recursed rows.
		 */
		public String getConcSep() {
			return this.concSep;
		}

		/**
		 * Returns the expression, <i>with</i> substitution. This value is
		 * RDBMS-specific. The prefix map must contain two entries. Each entry
		 * relates to one of the keys of a relation. The key of the map is the
		 * key of the relation, and the value is the prefix to use in the
		 * substituion, eg. "a" if columns for the table for that key should be
		 * prefixed as "a.mycolumn".
		 * 
		 * @param additionalRels
		 *            the prefixes to use for each table involved through an
		 *            additional relation.
		 * @param tablePrefix
		 *            the prefix to use for the table in the expression.
		 * @return the substituted expression.
		 */
		public String getSubstitutedExpression(final Map additionalRels,
				final String tablePrefix) {
			Log.debug("Calculating restricted table expression");
			String sub = this.expr;
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Object[] crPair = (Object[]) entry.getKey();
				final Relation rel = (Relation) crPair[0];
				final Column col = (Column) crPair[1];
				final String tp;
				if (rel == null)
					tp = tablePrefix;
				else
					tp = (String) additionalRels.get(rel);
				final String alias = ":" + (String) entry.getValue();
				sub = sub.replaceAll(alias, tp + "." + col.getName());
			}
			Log.debug("Expression is: " + sub);
			return sub;
		}

		/**
		 * Get all the additional relations involved with this.
		 * 
		 * @return the additional relations.
		 */
		public Collection getAdditionalRelations() {
			final Collection pairs = new HashSet();
			for (final Iterator i = this.aliases.entrySet().iterator(); i
					.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Object[] crPair = (Object[]) entry.getKey();
				if (crPair[0] == null)
					continue;
				pairs.add(crPair[0]);
			}
			return pairs;
		}

		/**
		 * Defines types of concat relation recursion.
		 */
		public static class RecursionType implements Comparable {
			private static final long serialVersionUID = 1L;

			private static final Map singletons = new HashMap();

			/**
			 * Use this constant to refer to a key with many values.
			 */
			public static final RecursionType APPEND = RecursionType
					.get("APPEND");

			/**
			 * Use this constant to refer to a key with one value.
			 */
			public static final RecursionType PREPEND = RecursionType
					.get("PREPEND");

			/**
			 * Use this constant to refer to a key with one value.
			 */
			public static final RecursionType NONE = RecursionType.get("NONE");

			/**
			 * The static factory method creates and returns a recursion type
			 * with the given name. It ensures the object returned is a
			 * singleton. Note that the names of recursion type objects are
			 * case-sensitive.
			 * 
			 * @param name
			 *            the name of the recursion type object.
			 * @return the recursion type object.
			 */
			public static RecursionType get(final String name) {
				// Do we already have this one?
				// If so, then return it.
				if (RecursionType.singletons.containsKey(name))
					return (RecursionType) RecursionType.singletons.get(name);

				// Otherwise, create it, remember it.
				final RecursionType c = new RecursionType(name);
				RecursionType.singletons.put(name, c);

				// Return it.
				return c;
			}

			private final String name;

			/**
			 * The private constructor takes a single parameter, which defines
			 * the name this recursion type object will display when printed.
			 * 
			 * @param name
			 *            the name of the recursion type.
			 */
			private RecursionType(final String name) {
				this.name = name;
			}

			public int compareTo(final Object o) throws ClassCastException {
				final RecursionType c = (RecursionType) o;
				return this.toString().compareTo(c.toString());
			}

			public boolean equals(final Object o) {
				// We are dealing with singletons so can use == happily.
				return o == this;
			}

			/**
			 * Displays the name of this recursion type object.
			 * 
			 * @return the name of this recursion type object.
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
			 * Always returns the name of this recursion type.
			 */
			public String toString() {
				return this.getName();
			}
		}
	}

	/**
	 * Defines a compound relation.
	 */
	public static class CompoundRelationDefinition {
		private static final long serialVersionUID = 1L;

		private int n;

		private boolean parallel;

		/**
		 * This constructor gives the compound relation an arity and a flag
		 * indicating whether to follow the multiple copies in parallel or
		 * serial.
		 * 
		 * @param n
		 *            the number of times this relation has been compounded (the
		 *            arity).
		 * @param parallel
		 *            whether this is a parallel (<tt>true</tt>) or serial (<tt>false</tt>)
		 *            compounding.
		 */
		public CompoundRelationDefinition(final int n, final boolean parallel) {
			// Remember the settings.
			this.n = n;
			this.parallel = parallel;
		}

		/**
		 * Get the arity of this compound relation.
		 * 
		 * @return the arity.
		 */
		public int getN() {
			return this.n;
		}

		/**
		 * Is this compound relation parallel?
		 * 
		 * @return <tt>true</tt> if it is.
		 */
		public boolean isParallel() {
			return this.parallel;
		}
	}
}
