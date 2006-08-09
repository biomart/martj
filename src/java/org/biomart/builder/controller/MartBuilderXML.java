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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetRelationRestriction;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableRestriction;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.InheritedColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.DataSet.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.DataSet.PartitionedColumnType.ValueCollection;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Schema.GenericSchema;
import org.biomart.builder.model.SchemaGroup.GenericSchemaGroup;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.Resources;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>
 * The MartBuilderXML class provides two static methods which serialize and
 * deserialize {@link Mart} objects to/from a basic XML format.
 * <p>
 * Writing is done by building up a map of objects to unique IDs. Where objects
 * cross-reference each other, they look up the unique ID in the map and
 * reference that instead. When reading, the reverse map is built up to achieve
 * the same effect. This system relies on objects being written out before they
 * are cross-referenced by other objects, so circular references are not
 * possible, and the file structure has been carefully planned to avoid other
 * situations where this may arise.
 * <p>
 * NOTE: The XML is version-specific. A formal DTD will be included with each
 * official release of MartBuilder, and subsequent releases will include new
 * DTDs (if any aspects have changed) and converter tools to translate your old
 * files.
 * <p>
 * TODO: Generate an initial DTD.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.33, 9th August 2006
 * @since 0.1
 */
public class MartBuilderXML extends DefaultHandler {
	/**
	 * Version number of MartBuilder XML DTD this class will read/write.
	 */
	public static final String DTD_VERSION = "0.1";

	private Mart constructedMart;

	private Map mappedObjects;

	private Map reverseMappedObjects;

	private Stack objectStack;

	private String currentOutputElement;

	private int currentOutputIndent;

	private int currentElementID;

	/**
	 * This class is intended to be used only in a static context.
	 */
	private MartBuilderXML() {
		this.constructedMart = null;
		this.currentOutputElement = null;
		this.currentOutputIndent = 0;
		this.currentElementID = 1;
	}

	public void startDocument() throws SAXException {
		this.mappedObjects = new HashMap();
		this.reverseMappedObjects = new HashMap();
		this.objectStack = new Stack();
	}

	public void endDocument() throws SAXException {
		// No action required.
	}

	public void startElement(final String namespaceURI, final String sName,
			final String qName, final Attributes attrs) throws SAXException {
		// Work out the name of the tag we are being asked to process.
		String eName = sName;
		if ("".equals(eName))
			eName = qName;

		// Construct a set of attributes from the tag.
		final Map attributes = new HashMap();
		if (attrs != null)
			for (int i = 0; i < attrs.getLength(); i++) {
				// Work out the name of the attribute.
				String aName = attrs.getLocalName(i);
				if ("".equals(aName))
					aName = attrs.getQName(i);

				// Store the attribute and value.
				final String aValue = attrs.getValue(i);
				attributes.put(aName, aValue.replaceAll("&quot;", "\"")
						.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
						.replaceAll("&amp;", "&"));
			}

		// Start by assuming the tag is not recognised.
		Object element = null;

		// Now, attempt to recognise the tag by checking its name
		// against a set of names known to us.

		// Mart (top-level only).
		if ("mart".equals(eName))
			// Start building a new mart. There can only be one mart tag
			// per file, as if more than one is found, the later tags
			// will override the earlier ones.
			element = this.constructedMart = new Mart();
		else if ("schemaGroup".equals(eName)) {
			// Start a new group of schemas.
			final String name = (String) attributes.get("name");
			try {
				final SchemaGroup schemaGroup = new GenericSchemaGroup(name);
				this.constructedMart.addSchema(schemaGroup);
				element = schemaGroup;
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// JDBC schema (anywhere, optionally inside schema group).
		else if ("jdbcSchema".equals(eName)) {
			// Start a new JDBC schema.

			// Does it have a driver class location? (optional)
			File driverClassLocation = null;
			if (attributes.containsKey("driverClassLocation"))
				driverClassLocation = new File((String) attributes
						.get("driverClassLocation"));

			// Does it have a password? (optional)
			String password = null;
			if (attributes.containsKey("password"))
				password = (String) attributes.get("password");

			// Load the compulsory attributes.
			final String driverClassName = (String) attributes
					.get("driverClassName");
			final String url = (String) attributes.get("url");
			final String schemaName = (String) attributes.get("schemaName");
			final String username = (String) attributes.get("username");
			final String name = (String) attributes.get("name");
			final boolean keyguessing = Boolean.valueOf(
					(String) attributes.get("keyguessing")).booleanValue();

			// Construct the JDBC schema.
			try {
				final Schema schema = new JDBCSchema(driverClassLocation,
						driverClassName, url, schemaName, username, password,
						name, keyguessing);
				// Are we inside a schema group?
				if (!this.objectStack.empty()
						&& this.objectStack.peek() instanceof SchemaGroup) {
					// Add the schema to the group if we are in a group.
					final SchemaGroup group = (SchemaGroup) this.objectStack
							.peek();
					group.addSchema(schema);
				} else
					// Add the schema directly to the mart if outside a group.
					this.constructedMart.addSchema(schema);
				element = schema;
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Table (inside table provider).
		else if ("table".equals(eName)) {
			// Start a new table.

			// What schema does it belong to? Throw a wobbly if not
			// currently inside a schema.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof Schema))
				throw new SAXException(Resources.get("tableOutsideSchema"));
			final Schema schema = (Schema) this.objectStack.peek();

			// Get the name and id as these are common features.
			final String id = (String) attributes.get("id");
			final String name = (String) attributes.get("name");
			final String originalName = (String) attributes.get("originalName");

			// DataSet table provider?
			if (schema instanceof DataSet) {
				// Work out what type of dataset table it is.
				final String type = (String) attributes.get("type");
				DataSetTableType dsType = null;
				if (type.equals("MAIN"))
					dsType = DataSetTableType.MAIN;
				else if (type.equals("MAIN_SUBCLASS"))
					dsType = DataSetTableType.MAIN_SUBCLASS;
				else if (type.equals("DIMENSION"))
					dsType = DataSetTableType.DIMENSION;
				else
					throw new SAXException(Resources.get(
							"unknownDatasetTableType", type));

				// Work out the underlying table (if has one).
				final String underlyingTableId = (String) attributes
						.get("underlyingTableId");
				Table underlyingTable = null;
				if (underlyingTableId != null
						&& underlyingTableId.trim().length() != 0)
					underlyingTable = (Table) this.mappedObjects
							.get(underlyingTableId);

				// Work out the source relation (if has one).
				final String sourceRelationId = (String) attributes
						.get("sourceRelationId");
				Relation sourceRelation = null;
				if (sourceRelationId != null
						&& sourceRelationId.trim().length() != 0)
					sourceRelation = (Relation) this.mappedObjects
							.get(sourceRelationId);

				try {
					// Construct the dataset table.
					final DataSetTable dst = new DataSetTable(name,
							(DataSet) schema, dsType, underlyingTable,
							sourceRelation);
					dst.setOriginalName(originalName);
					element = dst;

					// Read and set the underlying relations.
					final String[] underlyingRelationIds = ((String) attributes
							.get("underlyingRelationIds")).split("\\s*,\\s*");
					final List underRels = new ArrayList();
					for (int i = 0; i < underlyingRelationIds.length; i++)
						underRels.add(this.mappedObjects
								.get(underlyingRelationIds[i]));
					dst.setUnderlyingRelations(underRels);

					// Read and set the underlying keys.
					final String[] underlyingKeyIds = ((String) attributes
							.get("underlyingKeyIds")).split("\\s*,\\s*");
					final List underKeys = new ArrayList();
					for (int i = 0; i < underlyingKeyIds.length; i++)
						underKeys.add(this.mappedObjects
								.get(underlyingKeyIds[i]));
					dst.setUnderlyingKeys(underKeys);
				} catch (final Exception e) {
					throw new SAXException(e);
				}
			}

			// Generic schema?
			else if (schema instanceof GenericSchema)
				try {
					final Table table = new GenericTable(name, schema);
					table.setOriginalName(originalName);
					element = table;
				} catch (final Exception e) {
					throw new SAXException(e);
				}
			else
				throw new SAXException(Resources.get("unknownSchemaType",
						schema.getClass().getName()));

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Column (inside table).
		else if ("column".equals(eName)) {
			// What table does it belong to? Throw a wobbly if not inside one.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof Table))
				throw new SAXException(Resources.get("columnOutsideTable"));
			final Table tbl = (Table) this.objectStack.peek();

			// Get the id and name as these are common features.
			final String id = (String) attributes.get("id");
			final String name = (String) attributes.get("name");
			final String originalName = (String) attributes.get("originalName");

			try {
				// DataSet table column?
				if (tbl instanceof DataSetTable) {
					// Work out underlying relation, if any.
					final String underlyingRelationId = (String) attributes
							.get("underlyingRelationId");
					Relation underlyingRelation = null;
					if (!"null".equals(underlyingRelationId))
						underlyingRelation = (Relation) this.mappedObjects
								.get(underlyingRelationId);
					final boolean dependency = Boolean.valueOf(
							(String) attributes.get("dependency"))
							.booleanValue();
					final boolean masked = Boolean.valueOf(
							(String) attributes.get("masked")).booleanValue();

					// Work out type and construct appropriate column.
					final String type = (String) attributes.get("type");
					DataSetColumn column = null;
					if ("concatRelation".equals(type))
						column = new ConcatRelationColumn(name,
								(DataSetTable) tbl, underlyingRelation);
					else if ("schemaName".equals(type))
						column = new SchemaNameColumn(name, (DataSetTable) tbl);
					else if ("wrapped".equals(type)) {
						final Column wrappedCol = (Column) this.mappedObjects
								.get(attributes.get("wrappedColumnId"));
						column = new WrappedColumn(wrappedCol,
								(DataSetTable) tbl, underlyingRelation);
						column.setName(name);
					} else if ("inherited".equals(type)) {
						final DataSetColumn inheritedCol = (DataSetColumn) this.mappedObjects
								.get(attributes.get("inheritedColumnId"));
						column = new InheritedColumn((DataSetTable) tbl,
								inheritedCol);
					} else if ("expression".equals(type)) {
						column = new ExpressionColumn(name, (DataSetTable) tbl);
						// AliasCols, AliasNames - wrapped obj to string map
						final String[] aliasColumnIds = ((String) attributes
								.get("aliasColumnIds")).split(",");
						final String[] aliasNames = ((String) attributes
								.get("aliasNames")).split(",");
						for (int i = 0; i < aliasColumnIds.length; i++) {
							final WrappedColumn wrapped = (WrappedColumn) this.mappedObjects
									.get(aliasColumnIds[i]);
							((ExpressionColumn) column).getAliases().put(
									wrapped, aliasNames[i]);
						}
						// Other properties.
						((ExpressionColumn) column)
								.setExpression((String) attributes
										.get("expression"));
						((ExpressionColumn) column).setGroupBy(Boolean.valueOf(
								(String) attributes.get("groupBy"))
								.booleanValue());
					} else
						throw new SAXException(Resources.get(
								"unknownColumnType", type));

					// Partitioning.
					final String partitionType = (String) attributes
							.get("partitionType");
					PartitionedColumnType resolvedPartitionType;
					if (partitionType==null || "null".equals(partitionType)) {
						resolvedPartitionType = null;
					} else if ("singleValue".equals(partitionType)) {
						String value = null;
						boolean useNull = Boolean.valueOf(
								(String) attributes.get("partitionUseNull"))
								.booleanValue();
						if (!useNull)
							value = (String) attributes.get("partitionValue");
						resolvedPartitionType = new SingleValue(value, useNull);
					} else if ("valueCollection".equals(partitionType)) {
						// Values are comma-separated.
						final List valueList = new ArrayList();
						if (attributes.containsKey("partitionValues"))
							valueList.addAll(Arrays
									.asList(((String) attributes
											.get("partitionValues"))
											.split("\\s*,\\s*")));
						final boolean includeNull = Boolean.valueOf(
								(String) attributes.get("partitionUseNull"))
								.booleanValue();
						// Make the collection.
						resolvedPartitionType = new ValueCollection(valueList,
								includeNull);
					} else if ("uniqueValues".equals(partitionType))
						resolvedPartitionType = new UniqueValues();
					else
						throw new SAXException(Resources.get(
								"unknownPartitionColumnType", partitionType));

					// Flag the column as partitioned.
					column.setPartitionType(resolvedPartitionType);

					// Update remaining settings.
					column.setOriginalName(originalName);
					column.setDependency(dependency);
					column.setMasked(masked);
					element = column;
				}

				// Generic column?
				else if (tbl instanceof GenericTable) {
					final String nullable = (String) attributes.get("nullable");
					final Column column = new GenericColumn(name, tbl);
					column.setOriginalName(originalName);
					column
							.setNullable(Boolean.valueOf(nullable)
									.booleanValue());
					element = column;
				}

				// Others
				else
					throw new SAXException(Resources.get("unknownTableType",
							tbl.getClass().getName()));

			} catch (final Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Primary key (inside table).
		else if ("primaryKey".equals(eName)) {
			// What table does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof Table))
				throw new SAXException("pkOutsideTable");
			final Table tbl = (Table) this.objectStack.peek();

			// Get the ID.
			final String id = (String) attributes.get("id");

			try {
				// Work out what status the key is.
				final ComponentStatus status = ComponentStatus
						.get((String) attributes.get("status"));

				// Decode the column IDs from the comma-separated list.
				final String[] pkColIds = ((String) attributes.get("columnIds"))
						.split("\\s*,\\s*");
				final List pkCols = new ArrayList();
				for (int i = 0; i < pkColIds.length; i++)
					pkCols.add(this.mappedObjects.get(pkColIds[i]));

				// Make the key.
				final PrimaryKey pk = new GenericPrimaryKey(pkCols);
				pk.setStatus(status);

				// Assign it to the table.
				tbl.setPrimaryKey(pk);
				element = pk;
			} catch (final Exception e) {
				throw new SAXException(e);
			}

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Foreign key (inside table).
		else if ("foreignKey".equals(eName)) {
			// What table does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof Table))
				throw new SAXException(Resources.get("fkOutsideTable"));
			final Table tbl = (Table) this.objectStack.peek();

			// Get the ID and nullability.
			final String id = (String) attributes.get("id");

			try {
				// Work out what status it is.
				final ComponentStatus status = ComponentStatus
						.get((String) attributes.get("status"));

				// Decode the column IDs from the comma-separated list.
				final String[] fkColIds = ((String) attributes.get("columnIds"))
						.split("\\s*,\\s*");
				final List fkCols = new ArrayList();
				for (int i = 0; i < fkColIds.length; i++)
					fkCols.add(this.mappedObjects.get(fkColIds[i]));

				// Make the key.
				final ForeignKey fk = new GenericForeignKey(fkCols);
				fk.setStatus(status);

				// Add it to the table.
				tbl.addForeignKey(fk);
				element = fk;
			} catch (final Exception e) {
				throw new SAXException(e);
			}

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Relation (anywhere).
		else if ("relation".equals(eName)) {
			// Get the ID.
			final String id = (String) attributes.get("id");
			try {
				// Work out status, cardinality, and look up the keys
				// at either end.
				final ComponentStatus status = ComponentStatus
						.get((String) attributes.get("status"));
				final Cardinality card = Cardinality.get((String) attributes
						.get("cardinality"));
				final Key firstKey = (Key) this.mappedObjects.get(attributes
						.get("firstKeyId"));
				final Key secondKey = (Key) this.mappedObjects.get(attributes
						.get("secondKeyId"));

				// Make it
				final Relation rel = new GenericRelation(firstKey, secondKey,
						card);

				// Set its status.
				rel.setStatus(status);
				element = rel;
			} catch (final Exception e) {
				throw new SAXException(e);
			}

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Masked Relation (inside dataset).
		else if ("maskedRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("maskedRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));

				// Mask it.
				w.maskRelation(rel);
				element = rel;
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Subclass Relation (inside dataset).
		else if ("subclassRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("subclassRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));

				// Subclass it.
				w.flagSubclassRelation(rel);
				element = rel;
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Concat Relation (inside dataset).
		else if ("concatRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("concatRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));

				// Work out what concat-only type to use.
				final String type = (String) attributes
						.get("concatRelationType");
				ConcatRelationType crType = null;
				if (type.equals("COMMA_COMMA"))
					crType = ConcatRelationType.COMMA_COMMA;
				else if (type.equals("COMMA_SPACE"))
					crType = ConcatRelationType.COMMA_SPACE;
				else if (type.equals("COMMA_TAB"))
					crType = ConcatRelationType.COMMA_TAB;
				else if (type.equals("SPACE_COMMA"))
					crType = ConcatRelationType.SPACE_COMMA;
				else if (type.equals("SPACE_SPACE"))
					crType = ConcatRelationType.SPACE_SPACE;
				else if (type.equals("SPACE_TAB"))
					crType = ConcatRelationType.SPACE_TAB;
				else if (type.equals("TAB_COMMA"))
					crType = ConcatRelationType.TAB_COMMA;
				else if (type.equals("TAB_SPACE"))
					crType = ConcatRelationType.TAB_SPACE;
				else if (type.equals("TAB_TAB"))
					crType = ConcatRelationType.TAB_TAB;
				else
					throw new SAXException(Resources.get(
							"unknownConcatRelationType", type));

				// Flag it as concat-only.
				w.flagConcatOnlyRelation(rel, crType);
				element = rel;
			} catch (final Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		}

		// Restricted Relation (inside dataset).
		else if ("restrictedRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("restrictedRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));

				// Get the expression to use.
				final String expr = (String) attributes.get("expression");

				// Get the aliases to use for the first table.
				final Map first = new HashMap();
				final String[] firstTableAliasColumnIds = ((String) attributes
						.get("firstTableAliasColumnIds")).split(",");
				final String[] firstTableAliasNames = ((String) attributes
						.get("firstTableAliasNames")).split(",");
				for (int i = 0; i < firstTableAliasColumnIds.length; i++) {
					final Column wrapped = (Column) this.mappedObjects
							.get(firstTableAliasColumnIds[i]);
					first.put(wrapped, firstTableAliasNames[i]);
				}

				// Get the aliases to use for the second table.
				final Map second = new HashMap();
				final String[] secondTableAliasColumnIds = ((String) attributes
						.get("secondTableAliasColumnIds")).split(",");
				final String[] secondTableAliasNames = ((String) attributes
						.get("secondTableAliasNames")).split(",");
				for (int i = 0; i < secondTableAliasColumnIds.length; i++) {
					final Column wrapped = (Column) this.mappedObjects
							.get(secondTableAliasColumnIds[i]);
					second.put(wrapped, secondTableAliasNames[i]);
				}

				// Flag it as restricted
				final DataSetRelationRestriction restrict = new DataSetRelationRestriction(
						expr, first, second);
				w.flagRestrictedRelation(rel, restrict);
				element = rel;
			} catch (final Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		}

		// Restricted Table (inside dataset).
		else if ("restrictedTable".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("restrictedRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Table tbl = (Table) this.mappedObjects.get(attributes
						.get("tableId"));

				// Get the expression to use.
				final String expr = (String) attributes.get("expression");

				// Get the aliases to use for the first table.
				final Map aliases = new HashMap();
				final String[] aliasColumnIds = ((String) attributes
						.get("aliasColumnIds")).split(",");
				final String[] aliasNames = ((String) attributes
						.get("aliasNames")).split(",");
				for (int i = 0; i < aliasColumnIds.length; i++) {
					final Column wrapped = (Column) this.mappedObjects
							.get(aliasColumnIds[i]);
					aliases.put(wrapped, aliasNames[i]);
				}

				// Flag it as restricted
				final DataSetTableRestriction restrict = new DataSetTableRestriction(
						expr, aliases);
				w.flagRestrictedTable(tbl, restrict);
				element = tbl;
			} catch (final Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		}

		// DataSet (anywhere).
		else if ("dataset".equals(eName))
			try {
				// Look up the name, optimiser type, partition on schema flag,
				// central table reference, and mart constructor reference.
				// Resolve them all.
				final String name = (String) attributes.get("name");
				final boolean invisible = Boolean.valueOf(
						(String) attributes.get("invisible")).booleanValue();
				final Table centralTable = (Table) this.mappedObjects
						.get(attributes.get("centralTableId"));
				final String optType = (String) attributes.get("optimiser");

				// Construct the dataset.
				final DataSet ds = new DataSet(this.constructedMart,
						centralTable, name);

				// Work out the optimiser.
				DataSetOptimiserType opt = null;
				if ("NONE".equals(optType))
					opt = DataSetOptimiserType.NONE;
				else if ("COLUMN".equals(optType))
					opt = DataSetOptimiserType.COLUMN;
				else if ("TABLE".equals(optType))
					opt = DataSetOptimiserType.TABLE;
				else
					throw new SAXException(Resources.get(
							"unknownOptimiserType", optType));

				// Assign the mart constructor, optimiser, and partition on
				// schema settings.
				ds.setDataSetOptimiserType(opt);
				ds.setInvisible(invisible);
				element = ds;
			} catch (final Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		else
			throw new SAXException(Resources.get("unknownTag", eName));

		// Stick the element on the stack.
		if (element != null)
			this.objectStack.push(element);
	}

	public void endElement(final String namespaceURI, final String sName,
			final String qName) throws SAXException {
		// Work out what element it is we are closing.
		String eName = sName;
		if ("".equals(eName))
			eName = qName;

		// Pop the element off the stack.
		this.objectStack.pop();
	}

	/**
	 * Internal method which opens a tag in the output stream.
	 * 
	 * @param name
	 *            the tag to open.
	 * @param xmlWriter
	 *            the writer we are writing to.
	 * @throws IOException
	 *             if it failed to write it.
	 */
	private void openElement(final String name, final Writer xmlWriter)
			throws IOException {
		// Are we already partway through one?
		if (this.currentOutputElement != null) {
			// Yes, so put closing angle bracket and newline on it.
			xmlWriter.write(">");
			xmlWriter.write(System.getProperty("line.separator"));
			// Increase the indent.
			this.currentOutputIndent++;
		}

		// Write any indent required.
		for (int i = this.currentOutputIndent; i > 0; i--)
			xmlWriter.write("\t");
		// Open the tag.
		xmlWriter.write("<");
		// Write the tag.
		xmlWriter.write(name);

		// Update are note of what we are currently writing.
		this.currentOutputElement = name;
	}

	/**
	 * Internal method which closes a tag in the output stream.
	 * 
	 * @param name
	 *            the tag to close.
	 * @param xmlWriter
	 *            the writer we are writing to.
	 * @throws IOException
	 *             if it failed to write it.
	 */
	private void closeElement(final String name, final Writer xmlWriter)
			throws IOException {
		// Can we use the simple /> method?
		if (this.currentOutputElement != null
				&& name.equals(this.currentOutputElement)) {
			// Yes, so put closing angle bracket and newline on it.
			xmlWriter.write("/>");
			xmlWriter.write(System.getProperty("line.separator"));
		} else {
			// No, so use the full technique.
			// Decrease the indent.
			this.currentOutputIndent--;

			// Output any indent required.
			for (int i = this.currentOutputIndent; i > 0; i--)
				xmlWriter.write("\t");
			// Output the tag.
			xmlWriter.write("</");
			xmlWriter.write(name);
			xmlWriter.write(">\n");
		}
		this.currentOutputElement = null;
	}

	/**
	 * Internal method which writes an attribute in the output stream.
	 * 
	 * @param name
	 *            the name of the attribute.
	 * @param value
	 *            the value of the attribute.
	 * @param xmlWriter
	 *            the writer we are writing to.
	 * @throws IOException
	 *             if it failed to write it.
	 */
	private void writeAttribute(final String name, final String value,
			final Writer xmlWriter) throws IOException {
		// Write it.
		xmlWriter.write(" ");
		xmlWriter.write(name);
		xmlWriter.write("=\"");
		xmlWriter.write(value.replaceAll("&", "&amp;").replaceAll("\"",
				"&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
		xmlWriter.write("\"");
	}

	/**
	 * Internal method which writes a comma-separated list of attributes in the
	 * output stream.
	 * 
	 * @param name
	 *            the name of the attribute.
	 * @param values
	 *            the values of the attribute.
	 * @param xmlWriter
	 *            the writer we are writing to.
	 * @throws IOException
	 *             if it failed to write it.
	 */
	private void writeAttribute(final String name, final String[] values,
			final Writer xmlWriter) throws IOException {
		// Write it.
		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(values[i]);
		}
		this.writeAttribute(name, sb.toString(), xmlWriter);
	}

	/**
	 * Internal method which writes an entire schema out to file.
	 * 
	 * @param schema
	 *            the schema to write.
	 * @param xmlWriter
	 *            the writer to write to.
	 * @throws IOException
	 *             in case there was any problem writing the file.
	 * @throws BuilderException
	 *             if there were any logical problems with the schema.
	 */
	private void writeSchema(final Schema schema, final Writer xmlWriter)
			throws IOException, BuilderException {
		// What kind of schema is it?
		if (schema instanceof JDBCSchema) {
			// It's a JDBC schema.
			this.openElement("jdbcSchema", xmlWriter);
			final JDBCSchema jdbcSchema = (JDBCSchema) schema;

			if (jdbcSchema.getDriverClassLocation() != null)
				this.writeAttribute("driverClassLocation", jdbcSchema
						.getDriverClassLocation().getPath(), xmlWriter);
			this.writeAttribute("driverClassName", jdbcSchema
					.getDriverClassName(), xmlWriter);
			this.writeAttribute("url", jdbcSchema.getJDBCURL(), xmlWriter);
			this.writeAttribute("schemaName", jdbcSchema.getDatabaseSchema(),
					xmlWriter);
			this
					.writeAttribute("username", jdbcSchema.getUsername(),
							xmlWriter);
			if (jdbcSchema.getPassword() != null)
				this.writeAttribute("password", jdbcSchema.getPassword(),
						xmlWriter);
			this.writeAttribute("name", jdbcSchema.getName(), xmlWriter);
			this.writeAttribute("keyguessing", Boolean.toString(jdbcSchema
					.getKeyGuessing()), xmlWriter);
		}
		// Other schema types are not recognised.
		else
			throw new BuilderException(Resources.get("unknownSchemaType",
					schema.getClass().getName()));

		// Write out the contents, and note the relations.
		this.writeSchemaContents(schema, xmlWriter);

		// What kind of schema was it?
		// JDBC?
		if (schema instanceof JDBCSchema)
			this.closeElement("jdbcSchema", xmlWriter);
		// Others?
		else
			throw new BuilderException(Resources.get("unknownSchemaType",
					schema.getClass().getName()));
	}

	/**
	 * Internal method which writes out the tables of a schema and remembers the
	 * relations it saw as it goes along.
	 * 
	 * 
	 * @param schema
	 *            the {@link Schema} to write out the tables of.
	 * @param relations
	 *            the set of {@link Relation}s we found on the way.
	 * @param xmlWriter
	 *            the writer to write to.
	 * @throws IOException
	 *             if there was a problem writing to file.
	 * @throws AssociationException
	 *             if an unwritable kind of object was found.
	 */
	private void writeSchemaContents(final Schema schema, final Writer xmlWriter)
			throws IOException, BuilderException {
		// What tables to write? The order is important for datasets
		// only. This is to allow inherited columns to reference
		// earlier columns.
		final List tables = new ArrayList();
		if (schema instanceof DataSet) {
			// Add the main table first.
			for (final Iterator i = schema.getTables().iterator(); i.hasNext();) {
				final DataSetTable dsTab = (DataSetTable) i.next();
				if (dsTab.getType().equals(DataSetTableType.MAIN))
					tables.add(dsTab);
			}
			// Recursively add child tables.
			for (int i = 0; i < tables.size(); i++) {
				final DataSetTable dsTab = (DataSetTable) tables.get(i);
				if (dsTab.getPrimaryKey() != null)
					for (final Iterator j = dsTab.getPrimaryKey()
							.getRelations().iterator(); j.hasNext();)
						tables.add(((Relation) j.next()).getManyKey()
								.getTable());
			}
		} else
			tables.addAll(schema.getTables());
		// Write out tables inside each schema.
		for (final Iterator ti = tables.iterator(); ti.hasNext();) {
			final Table table = (Table) ti.next();
			final String tableMappedID = "" + this.currentElementID++;
			this.reverseMappedObjects.put(table, tableMappedID);

			// Start table.
			this.openElement("table", xmlWriter);
			this.writeAttribute("id", tableMappedID, xmlWriter);
			this.writeAttribute("name", table.getName(), xmlWriter);
			this.writeAttribute("originalName", table.getOriginalName(),
					xmlWriter);

			// A dataset table?
			if (table instanceof DataSetTable) {
				// Write the type.
				this.writeAttribute("type", ((DataSetTable) table).getType()
						.getName(), xmlWriter);
				final Table underlyingTable = ((DataSetTable) table)
						.getUnderlyingTable();
				if (underlyingTable != null)
					this.writeAttribute("underlyingTableId",
							(String) this.reverseMappedObjects
									.get(underlyingTable), xmlWriter);

				// Write out the underlying relations.
				final List underRelIds = new ArrayList();
				for (final Iterator i = ((DataSetTable) table)
						.getUnderlyingRelations().iterator(); i.hasNext();)
					underRelIds.add(this.reverseMappedObjects.get(i.next()));
				this.writeAttribute("underlyingRelationIds",
						(String[]) underRelIds.toArray(new String[0]),
						xmlWriter);

				// Write out the underlying keys.
				final List underKeyIds = new ArrayList();
				for (final Iterator i = ((DataSetTable) table)
						.getUnderlyingKeys().iterator(); i.hasNext();)
					underKeyIds.add(this.reverseMappedObjects.get(i.next()));
				this.writeAttribute("underlyingKeyIds", (String[]) underKeyIds
						.toArray(new String[0]), xmlWriter);

				// Write out the source relation.
				final Relation sourceRelation = ((DataSetTable) table)
						.getSourceRelation();
				if (sourceRelation != null)
					this.writeAttribute("sourceRelationId",
							(String) this.reverseMappedObjects
									.get(sourceRelation), xmlWriter);
			}

			// Make a place to store expression columns, which must come last
			// in case they reference columns that come after them.
			List expressionColumns = new ArrayList();

			// Write out columns inside each table.
			for (final Iterator ci = table.getColumns().iterator(); ci
					.hasNext();) {
				final Column col = (Column) ci.next();
				// Skip expression columns till later.
				if (col instanceof ExpressionColumn) {
					expressionColumns.add(col);
					continue;
				}

				// Otherwise continue as normal.
				final String colMappedID = "" + this.currentElementID++;
				this.reverseMappedObjects.put(col, colMappedID);

				// Start column.
				this.openElement("column", xmlWriter);
				this.writeAttribute("id", colMappedID, xmlWriter);
				this.writeAttribute("name", col.getName(), xmlWriter);
				this.writeAttribute("originalName", col.getOriginalName(),
						xmlWriter);
				this.writeAttribute("nullable", Boolean.toString(col
						.getNullable()), xmlWriter);

				// Dataset column?
				if (col instanceof DataSetColumn) {
					final DataSetColumn dcol = (DataSetColumn) col;
					final Relation underlyingRelation = dcol
							.getUnderlyingRelation();
					String underlyingRelationId = "null";
					if (underlyingRelation != null)
						underlyingRelationId = (String) this.reverseMappedObjects
								.get(underlyingRelation);
					this.writeAttribute("underlyingRelationId",
							underlyingRelationId, xmlWriter);
					this.writeAttribute("dependency", Boolean.toString(dcol
							.getDependency()), xmlWriter);
					this.writeAttribute("masked", Boolean.toString(dcol
							.getMasked()), xmlWriter);
					this.writeAttribute("alt",
							underlyingRelation == null ? "null"
									: underlyingRelation.toString(), xmlWriter);

					// Concat relation column?
					if (dcol instanceof ConcatRelationColumn)
						this
								.writeAttribute("type", "concatRelation",
										xmlWriter);
					// Schema name column?
					else if (dcol instanceof SchemaNameColumn)
						this.writeAttribute("type", "schemaName", xmlWriter);

					// Wrapped column?
					else if (dcol instanceof WrappedColumn) {
						this.writeAttribute("type", "wrapped", xmlWriter);
						this
								.writeAttribute("wrappedColumnId",
										(String) this.reverseMappedObjects
												.get(((WrappedColumn) dcol)
														.getWrappedColumn()),
										xmlWriter);
						this.writeAttribute("wrappedColumnAlt",
								((WrappedColumn) dcol).getWrappedColumn()
										.toString(), xmlWriter);
					}

					// Inherited column?
					else if (dcol instanceof InheritedColumn) {
						this.writeAttribute("type", "inherited", xmlWriter);
						this.writeAttribute("inheritedColumnId",
								(String) this.reverseMappedObjects
										.get(((InheritedColumn) dcol)
												.getInheritedColumn()),
								xmlWriter);
						this.writeAttribute("inheritedColumnAlt",
								((InheritedColumn) dcol).getInheritedColumn()
										.toString(), xmlWriter);
					}

					// Others
					else
						throw new BuilderException(Resources.get(
								"unknownDatasetColumnType", dcol.getClass()
										.getName()));

					// What kind of partition is it?
					PartitionedColumnType ptc = dcol.getPartitionType();
					if (ptc == null) {
						this.writeAttribute("partitionType", "null", xmlWriter);
					}
					// Single value partition?
					else if (ptc instanceof SingleValue) {
						final SingleValue sv = (SingleValue) ptc;
						this.writeAttribute("partitionType", "singleValue",
								xmlWriter);
						final String value = sv.getValue();
						this.writeAttribute("partitionUseNull", Boolean
								.toString(sv.getIncludeNull()), xmlWriter);
						if (value != null)
							this.writeAttribute("partitionValue", value,
									xmlWriter);
					}
					// Unique values partition?
					else if (ptc instanceof UniqueValues)
						this.writeAttribute("partitionType",
								"uniqueValues", xmlWriter);
					// Values collection partition?
					else if (ptc instanceof ValueCollection) {
						final ValueCollection vc = (ValueCollection) ptc;
						this.writeAttribute("partitionType",
								"valueCollection", xmlWriter);
						// Values are comma-separated.
						final List valueList = new ArrayList();
						valueList.addAll(vc.getValues());
						this.writeAttribute("partitionUseNull", Boolean
								.toString(vc.getIncludeNull()), xmlWriter);
						if (!valueList.isEmpty())
							this
									.writeAttribute("partitionValues",
											(String[]) valueList
													.toArray(new String[0]),
											xmlWriter);
					}
					// Others.
					else
						throw new BuilderException(Resources.get(
								"unknownPartitionColumnType", ptc.getClass()
										.getName()));
				}
				// Generic column?
				else if (col instanceof GenericColumn) {
					// Nothing extra required here.
				}
				// Others
				else
					throw new BuilderException(Resources.get(
							"unknownColumnType", col.getClass().getName()));

				// Close off column element.
				this.closeElement("column", xmlWriter);
			}

			// Write out expression columns, if any.
			for (Iterator i = expressionColumns.iterator(); i.hasNext();) {
				ExpressionColumn col = (ExpressionColumn) i.next();
				final String colMappedID = "" + this.currentElementID++;
				this.reverseMappedObjects.put(col, colMappedID);

				// Start column.
				this.openElement("column", xmlWriter);
				this.writeAttribute("id", colMappedID, xmlWriter);
				this.writeAttribute("name", col.getName(), xmlWriter);
				this.writeAttribute("originalName", col.getOriginalName(),
						xmlWriter);
				this.writeAttribute("nullable", Boolean.toString(col
						.getNullable()), xmlWriter);

				final Relation underlyingRelation = col.getUnderlyingRelation();
				String underlyingRelationId = "null";
				if (underlyingRelation != null)
					underlyingRelationId = (String) this.reverseMappedObjects
							.get(underlyingRelation);
				this.writeAttribute("underlyingRelationId",
						underlyingRelationId, xmlWriter);
				this.writeAttribute("dependency", Boolean
						.toString(((DataSetColumn) col).getDependency()),
						xmlWriter);
				this.writeAttribute("alt", underlyingRelation == null ? "null"
						: underlyingRelation.toString(), xmlWriter);

				this.writeAttribute("type", "expression", xmlWriter);

				// AliasCols, AliasNames - wrapped obj to string map
				final StringBuffer aliasColumnIds = new StringBuffer();
				final StringBuffer aliasNames = new StringBuffer();
				for (final Iterator j = col.getAliases().entrySet().iterator(); j
						.hasNext();) {
					final Map.Entry entry = (Map.Entry) j.next();
					final DataSetColumn wrapped = (DataSetColumn) entry
							.getKey();
					final String alias = (String) entry.getValue();
					aliasColumnIds.append((String) this.reverseMappedObjects
							.get(wrapped));
					aliasNames.append(alias);
					if (j.hasNext()) {
						aliasColumnIds.append(',');
						aliasNames.append(',');
					}
				}
				this.writeAttribute("aliasColumnIds",
						aliasColumnIds.toString(), xmlWriter);
				this.writeAttribute("aliasNames", aliasNames.toString(),
						xmlWriter);

				// Other properties.
				this.writeAttribute("expression", col.getExpression(),
						xmlWriter);
				this.writeAttribute("groupBy", Boolean.toString(col
						.getGroupBy()), xmlWriter);

				// Close off column element.
				this.closeElement("column", xmlWriter);
			}

			// Write out keys inside each table. Remember relations as
			// we go along.
			for (final Iterator ki = table.getKeys().iterator(); ki.hasNext();) {
				final Key key = (Key) ki.next();
				final String keyMappedID = "" + this.currentElementID++;
				this.reverseMappedObjects.put(key, keyMappedID);

				String elem = null;
				if (key instanceof PrimaryKey)
					elem = "primaryKey";
				else if (key instanceof ForeignKey)
					elem = "foreignKey";
				else
					throw new BuilderException(Resources.get("unknownKey", key
							.getClass().getName()));

				this.openElement(elem, xmlWriter);
				this.writeAttribute("id", keyMappedID, xmlWriter);
				final List columnIds = new ArrayList();
				for (final Iterator kci = key.getColumns().iterator(); kci
						.hasNext();)
					columnIds.add(this.reverseMappedObjects.get(kci.next()));
				this.writeAttribute("columnIds", (String[]) columnIds
						.toArray(new String[0]), xmlWriter);
				this.writeAttribute("status", key.getStatus().getName(),
						xmlWriter);
				this.writeAttribute("alt", key.toString(), xmlWriter);
				this.closeElement(elem, xmlWriter);
			}

			// Finish table.
			this.closeElement("table", xmlWriter);
		}

		// Write relations.
		this.writeRelations(schema.getInternalRelations(), xmlWriter);
	}

	/**
	 * Internal method which writes out a set of relations.
	 * 
	 * @param relations
	 *            the set of {@link Relation}s to write.
	 * @param xmlWriter
	 *            the writer to write to.
	 * @throws IOException
	 *             if there was a problem writing to file.
	 */
	private void writeRelations(final Collection relations,
			final Writer xmlWriter) throws IOException {
		// Write out relations.
		for (final Iterator i = relations.iterator(); i.hasNext();) {
			final Relation r = (Relation) i.next();
			final String relMappedID = "" + this.currentElementID++;
			this.reverseMappedObjects.put(r, relMappedID);
			this.openElement("relation", xmlWriter);
			this.writeAttribute("id", relMappedID, xmlWriter);
			this.writeAttribute("cardinality", r.getCardinality().getName(),
					xmlWriter);
			this.writeAttribute("firstKeyId",
					(String) this.reverseMappedObjects.get(r.getFirstKey()),
					xmlWriter);
			this.writeAttribute("secondKeyId",
					(String) this.reverseMappedObjects.get(r.getSecondKey()),
					xmlWriter);
			this.writeAttribute("status", r.getStatus().toString(), xmlWriter);
			this.writeAttribute("alt", r.toString(), xmlWriter);
			this.closeElement("relation", xmlWriter);
		}
	}

	/**
	 * Internal method which does the work of writing out XML files and
	 * generating those funky ID tags you see in them.
	 * 
	 * @param mart
	 *            the mart to write.
	 * @param xmlWriter
	 *            the Writer to write the XML to.
	 * @throws IOException
	 *             if a write error occurs.
	 * @throws BuilderException
	 *             if it encounters an object not writable under the current
	 *             DTD.
	 */
	private void writeXML(final Mart mart, final Writer xmlWriter)
			throws IOException, BuilderException {
		// Write the headers.
		xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		// xmlWriter.write("<\!DOCTYPE mart PUBLIC \"-//EBI//DTD
		// MartBuilder "+MartBuilderXML.DTD_VERSION+"//EN\"
		// \"http\://www.biomart.org/TR/MartBuilder-"+MartBuilderXML.DTD_VERSION+"/DTD/mart.dtd\">\n");

		// Initialise the ID counter.
		this.reverseMappedObjects = new HashMap();

		// Start by enclosing the whole lot in a <mart> tag.
		this.openElement("mart", xmlWriter);

		// Write out each schema.
		final Set externalRelations = new HashSet();
		for (final Iterator i = mart.getSchemas().iterator(); i.hasNext();) {
			final Schema schema = (Schema) i.next();
			if (schema instanceof SchemaGroup) {
				this.openElement("schemaGroup", xmlWriter);
				this.writeAttribute("name", schema.getName(), xmlWriter);
				// Write group itself.
				this.writeSchemaContents(schema, xmlWriter);
				// Write member schemas.
				for (final Iterator j = ((SchemaGroup) schema).getSchemas()
						.iterator(); j.hasNext();)
					this.writeSchema((Schema) j.next(), xmlWriter);
				this.closeElement("schemaGroup", xmlWriter);
			} else
				this.writeSchema(schema, xmlWriter);
			externalRelations.addAll(schema.getExternalRelations());
		}

		// Write out relations.
		this.writeRelations(externalRelations, xmlWriter);

		// Write out windows.
		for (final Iterator dsi = mart.getDataSets().iterator(); dsi.hasNext();) {
			final DataSet ds = (DataSet) dsi.next();
			this.openElement("dataset", xmlWriter);
			this.writeAttribute("name", ds.getName(), xmlWriter);
			this.writeAttribute("centralTableId",
					(String) this.reverseMappedObjects
							.get(ds.getCentralTable()), xmlWriter);
			this.writeAttribute("alt", ds.getCentralTable().toString(),
					xmlWriter);
			this.writeAttribute("optimiser", ds.getDataSetOptimiserType()
					.getName(), xmlWriter);
			this.writeAttribute("invisible", Boolean
					.toString(ds.getInvisible()), xmlWriter);

			// Write out the contents of the dataset, and note the relations.
			this.writeSchemaContents(ds, xmlWriter);

			// Write out concat relations inside window. MUST come first else
			// the dataset concat-only cols will complain about not having a
			// relation to refer to.
			for (final Iterator x = ds.getConcatOnlyRelations().iterator(); x
					.hasNext();) {
				final Relation r = (Relation) x.next();
				this.openElement("concatRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.writeAttribute("concatRelationType", ds
						.getConcatRelationType(r).getName(), xmlWriter);
				this.writeAttribute("alt", r.toString(), xmlWriter);
				this.closeElement("concatRelation", xmlWriter);
			}

			// Write out restricted tables inside window.
			for (final Iterator x = ds.getRestrictedTables().iterator(); x
					.hasNext();) {
				final Table t = (Table) x.next();
				final DataSetTableRestriction restrict = ds
						.getRestrictedTableType(t);
				this.openElement("restrictedTable", xmlWriter);
				this.writeAttribute("tableId",
						(String) this.reverseMappedObjects.get(t), xmlWriter);
				this.writeAttribute("expression", restrict.getExpression(),
						xmlWriter);

				// AliasCols, AliasNames - wrapped obj to string map
				final StringBuffer aliasColumnIds = new StringBuffer();
				final StringBuffer aliasNames = new StringBuffer();
				for (final Iterator i = restrict.getAliases().entrySet()
						.iterator(); i.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final Column col = (Column) entry.getKey();
					final String alias = (String) entry.getValue();
					aliasColumnIds.append((String) this.reverseMappedObjects
							.get(col));
					aliasNames.append(alias);
					if (i.hasNext()) {
						aliasColumnIds.append(',');
						aliasNames.append(',');
					}
				}
				this.writeAttribute("aliasColumnIds",
						aliasColumnIds.toString(), xmlWriter);
				this.writeAttribute("aliasNames", aliasNames.toString(),
						xmlWriter);

				this.writeAttribute("alt", t.toString(), xmlWriter);
				this.closeElement("restrictedTable", xmlWriter);
			}

			// Write out restricted relations inside window.
			for (final Iterator x = ds.getRestrictedRelations().iterator(); x
					.hasNext();) {
				final Relation r = (Relation) x.next();
				final DataSetRelationRestriction restrict = ds
						.getRestrictedRelationType(r);
				this.openElement("restrictedRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.writeAttribute("expression", restrict.getExpression(),
						xmlWriter);

				// First table aliases.
				// AliasCols, AliasNames - wrapped obj to string map
				final StringBuffer firstAliasColumnIds = new StringBuffer();
				final StringBuffer firstAliasNames = new StringBuffer();
				for (final Iterator i = restrict.getFirstTableAliases()
						.entrySet().iterator(); i.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final Column col = (Column) entry.getKey();
					final String alias = (String) entry.getValue();
					firstAliasColumnIds
							.append((String) this.reverseMappedObjects.get(col));
					firstAliasNames.append(alias);
					if (i.hasNext()) {
						firstAliasColumnIds.append(',');
						firstAliasNames.append(',');
					}
				}
				this.writeAttribute("firstTableAliasColumnIds",
						firstAliasColumnIds.toString(), xmlWriter);
				this.writeAttribute("firstTableAliasNames", firstAliasNames
						.toString(), xmlWriter);

				// Second table aliases.
				// AliasCols, AliasNames - wrapped obj to string map
				final StringBuffer secondAliasColumnIds = new StringBuffer();
				final StringBuffer secondAliasNames = new StringBuffer();
				for (final Iterator i = restrict.getSecondTableAliases()
						.entrySet().iterator(); i.hasNext();) {
					final Map.Entry entry = (Map.Entry) i.next();
					final Column col = (Column) entry.getKey();
					final String alias = (String) entry.getValue();
					secondAliasColumnIds
							.append((String) this.reverseMappedObjects.get(col));
					secondAliasNames.append(alias);
					if (i.hasNext()) {
						secondAliasColumnIds.append(',');
						secondAliasNames.append(',');
					}
				}
				this.writeAttribute("secondTableAliasColumnIds",
						secondAliasColumnIds.toString(), xmlWriter);
				this.writeAttribute("secondTableAliasNames", secondAliasNames
						.toString(), xmlWriter);

				this.writeAttribute("alt", r.toString(), xmlWriter);
				this.closeElement("restrictedRelation", xmlWriter);
			}

			// Write out masked relations inside window. Can go before or after.
			for (final Iterator x = ds.getMaskedRelations().iterator(); x
					.hasNext();) {
				final Relation r = (Relation) x.next();
				this.openElement("maskedRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.writeAttribute("alt", r.toString(), xmlWriter);
				this.closeElement("maskedRelation", xmlWriter);
			}

			// Write out subclass relations inside window. Can go before or
			// after.
			for (final Iterator x = ds.getSubclassedRelations().iterator(); x
					.hasNext();) {
				final Relation r = (Relation) x.next();
				this.openElement("subclassRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.writeAttribute("alt", r.toString(), xmlWriter);
				this.closeElement("subclassRelation", xmlWriter);
			}

			// Write out relations inside dataset.
			this.closeElement("dataset", xmlWriter);
		}

		// Finished! Close the mart tag.
		this.closeElement("mart", xmlWriter);

		// Flush.
		xmlWriter.flush();
	}

	/**
	 * The load method takes a {@link File} and loads up a {@link Mart} object
	 * based on the XML contents of the file. This XML is usually generated by
	 * the {@link MartBuilderXML#save(Mart,File)} method.
	 * 
	 * @param file
	 *            the {@link File} to load the data from.
	 * @return a {@link Mart} object containing the data from the file.
	 * @throws IOException
	 *             if there was any problem reading the file.
	 * @throws BuilderException
	 *             if the content of the file is not valid {@link Mart} XML, or
	 *             has any logical problems.
	 */
	public static Mart load(final File file) throws IOException,
			BuilderException {
		// Use the default (non-validating) parser
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		// Parse the input
		final MartBuilderXML loader = new MartBuilderXML();
		try {
			final SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(file, loader);
		} catch (final ParserConfigurationException e) {
			throw new BuilderException(Resources.get("XMLConfigFailed"), e);
		} catch (final SAXException e) {
			throw new BuilderException(Resources.get("XMLUnparseable"), e);
		}
		// Get the constructed object.
		final Mart s = loader.constructedMart;
		// Check that it is a schema.
		if (s == null)
			throw new BuilderException(Resources.get("fileNotSchemaVersion",
					MartBuilderXML.DTD_VERSION));
		// Return.
		return s;
	}

	/**
	 * The save method takes a {@link Mart} object and writes out XML describing
	 * it to the given {@link File}. This XML can be read by the
	 * {@link MartBuilderXML#load(File)} method.
	 * 
	 * @param schema
	 *            {@link Mart} object containing the data for the file.
	 * @param file
	 *            the {@link File} to save the data to.
	 * @throws IOException
	 *             if there was any problem writing the file.
	 * @throws BuilderException
	 *             if it encounters an object not writable under the current
	 *             DTD.
	 */
	public static void save(final Mart schema, final File file)
			throws IOException, BuilderException {
		// Open the file.
		final FileWriter fw = new FileWriter(file);
		try {
			// Write it out.
			(new MartBuilderXML()).writeXML(schema, fw);
		} catch (final IOException e) {
			throw e;
		} catch (final BuilderException e) {
			throw e;
		} finally {
			// Close the output stream.
			fw.close();
		}
	}
}
