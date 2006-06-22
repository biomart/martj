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
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
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
import org.biomart.builder.resources.BuilderBundle;
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
 * @version 0.1.19, 22nd June 2006
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

	public void startElement(String namespaceURI, String sName, String qName,
			Attributes attrs) throws SAXException {
		// Work out the name of the tag we are being asked to process.
		String eName = sName;
		if ("".equals(eName))
			eName = qName;

		// Construct a set of attributes from the tag.
		Map attributes = new HashMap();
		if (attrs != null) {
			for (int i = 0; i < attrs.getLength(); i++) {
				// Work out the name of the attribute.
				String aName = attrs.getLocalName(i);
				if ("".equals(aName))
					aName = attrs.getQName(i);

				// Store the attribute and value.
				String aValue = attrs.getValue(i);
				attributes.put(aName, aValue);
			}
		}

		// Start by assuming the tag is not recognised.
		Object element = null;

		// Now, attempt to recognise the tag by checking its name
		// against a set of names known to us.

		// Mart (top-level only).
		if ("mart".equals(eName)) {
			// Start building a new mart. There can only be one mart tag
			// per file, as if more than one is found, the later tags
			// will override the earlier ones.
			element = this.constructedMart = new Mart();
		}

		// Schema group (anywhere).
		else if ("schemaGroup".equals(eName)) {
			// Start a new group of schemas.
			String name = (String) attributes.get("name");
			try {
				SchemaGroup schemaGroup = new GenericSchemaGroup(name);
				this.constructedMart.addSchema(schemaGroup);
				element = schemaGroup;
			} catch (Exception e) {
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
			String driverClassName = (String) attributes.get("driverClassName");
			String url = (String) attributes.get("url");
			String schemaName = (String) attributes.get("schemaName");
			String username = (String) attributes.get("username");
			String name = (String) attributes.get("name");
			boolean keyguessing = ((String) attributes.get("keyguessing"))
					.equals("true");

			// Construct the JDBC schema.
			try {
				Schema schema = new JDBCSchema(driverClassLocation,
						driverClassName, url, schemaName, username, password,
						name, keyguessing);
				// Are we inside a schema group?
				if (!this.objectStack.empty()
						&& (this.objectStack.peek() instanceof SchemaGroup)) {
					// Add the schema to the group if we are in a group.
					SchemaGroup group = (SchemaGroup) this.objectStack.peek();
					group.addSchema(schema);
				} else
					// Add the schema directly to the mart if outside a group.
					this.constructedMart.addSchema(schema);
				element = schema;
			} catch (Exception e) {
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
				throw new SAXException(BuilderBundle
						.getString("tableOutsideSchema"));
			Schema schema = (Schema) this.objectStack.peek();

			// Get the name and id as these are common features.
			String id = (String) attributes.get("id");
			String name = (String) attributes.get("name");
			String originalName = (String) attributes.get("originalName");
			
			// DataSet table provider?
			if (schema instanceof DataSet) {
				// Work out what type of dataset table it is.
				String type = (String) attributes.get("type");
				DataSetTableType dsType = null;
				if (type.equals("MAIN"))
					dsType = DataSetTableType.MAIN;
				else if (type.equals("MAIN_SUBCLASS"))
					dsType = DataSetTableType.MAIN_SUBCLASS;
				else if (type.equals("DIMENSION"))
					dsType = DataSetTableType.DIMENSION;
				else
					throw new SAXException(BuilderBundle.getString(
							"unknownDatasetTableType", type));

				// Work out the underlying table (if has one).
				String underlyingTableId = (String) attributes
						.get("underlyingTableId");
				Table underlyingTable = null;
				if (underlyingTableId != null
						&& underlyingTableId.trim().length() != 0)
					underlyingTable = (Table) this.mappedObjects
							.get(underlyingTableId);

				try {
					// Construct the dataset table.
					DataSetTable dst = new DataSetTable(name, (DataSet) schema,
							dsType, underlyingTable);
					dst.setOriginalName(originalName);
					element = dst;

					// Read and set the underlying relations.
					String[] underlyingRelationIds = ((String) attributes
							.get("underlyingRelationIds")).split("\\s*,\\s*");
					List underRels = new ArrayList();
					for (int i = 0; i < underlyingRelationIds.length; i++)
						underRels.add(this.mappedObjects
								.get(underlyingRelationIds[i]));
					dst.setUnderlyingRelations(underRels);

					// Read and set the underlying keys.
					String[] underlyingKeyIds = ((String) attributes
							.get("underlyingKeyIds")).split("\\s*,\\s*");
					List underKeys = new ArrayList();
					for (int i = 0; i < underlyingKeyIds.length; i++)
						underKeys.add(this.mappedObjects
								.get(underlyingKeyIds[i]));
					dst.setUnderlyingKeys(underKeys);
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}

			// Generic schema?
			else if (schema instanceof GenericSchema) {
				try {
					Table table = new GenericTable(name, schema);
					table.setOriginalName(originalName);
					element = table;
				} catch (Exception e) {
					throw new SAXException(e);
				}
			}

			// Others
			else
				throw new SAXException(BuilderBundle.getString(
						"unknownSchemaType", schema.getClass().getName()));

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Column (inside table).
		else if ("column".equals(eName)) {
			// What table does it belong to? Throw a wobbly if not inside one.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof Table))
				throw new SAXException(BuilderBundle
						.getString("columnOutsideTable"));
			Table tbl = (Table) this.objectStack.peek();

			// Get the id and name as these are common features.
			String id = (String) attributes.get("id");
			String name = (String) attributes.get("name");
			String originalName = (String) attributes.get("originalName");
			
			try {
				// DataSet table column?
				if (tbl instanceof DataSetTable) {
					// Work out underlying relation, if any.
					String underlyingRelationId = (String) attributes
							.get("underlyingRelationId");
					Relation underlyingRelation = null;
					if (!"null".equals(underlyingRelationId))
						underlyingRelation = (Relation) this.mappedObjects
								.get(underlyingRelationId);

					// Work out type and construct appropriate column.
					String type = (String) attributes.get("type");
					DataSetColumn column = null;
					if ("concatRelation".equals(type)) {
						column = new ConcatRelationColumn(name,
								(DataSetTable) tbl, underlyingRelation);
					} else if ("schemaName".equals(type))
						column = new SchemaNameColumn(name, (DataSetTable) tbl);
					else if ("wrapped".equals(type)) {
						Column wrappedCol = (Column) this.mappedObjects
								.get(attributes.get("wrappedColumnId"));
						column = new WrappedColumn(wrappedCol,
								(DataSetTable) tbl, underlyingRelation);
					} else
						throw new SAXException(BuilderBundle.getString(
								"unknownColumnType", type));

					// Override the name, to make sure we get the same alias as
					// the original.
					column.setName(name);
					column.setOriginalName(originalName);
					element = column;
				}

				// Generic column?
				else if (tbl instanceof GenericTable) {
					Column column = new GenericColumn(name, tbl);
					column.setOriginalName(originalName);
					element = column;
				}

				// Others
				else
					throw new SAXException(BuilderBundle.getString(
							"unknownTableType", tbl.getClass().getName()));

			} catch (Exception e) {
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
			Table tbl = (Table) this.objectStack.peek();

			// Get the ID.
			String id = (String) attributes.get("id");

			try {
				// Work out what status the key is.
				ComponentStatus status = ComponentStatus
						.get((String) attributes.get("status"));

				// Decode the column IDs from the comma-separated list.
				String[] pkColIds = ((String) attributes.get("columnIds"))
						.split("\\s*,\\s*");
				List pkCols = new ArrayList();
				for (int i = 0; i < pkColIds.length; i++)
					pkCols.add(this.mappedObjects.get(pkColIds[i]));

				// Make the key.
				PrimaryKey pk = new GenericPrimaryKey(pkCols);
				pk.setStatus(status);

				// Assign it to the table.
				tbl.setPrimaryKey(pk);
				element = pk;
			} catch (Exception e) {
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
				throw new SAXException(BuilderBundle
						.getString("fkOutsideTable"));
			Table tbl = (Table) this.objectStack.peek();

			// Get the ID and nullability.
			String id = (String) attributes.get("id");
			boolean nullable = false;

			try {
				// Work out what status it is.
				ComponentStatus status = ComponentStatus
						.get((String) attributes.get("status"));
				if (attributes.containsKey("nullable"))
					nullable = Boolean.valueOf(
							(String) attributes.get("nullable")).booleanValue();

				// Decode the column IDs from the comma-separated list.
				String[] fkColIds = ((String) attributes.get("columnIds"))
						.split("\\s*,\\s*");
				List fkCols = new ArrayList();
				for (int i = 0; i < fkColIds.length; i++)
					fkCols.add(this.mappedObjects.get(fkColIds[i]));

				// Make the key.
				ForeignKey fk = new GenericForeignKey(fkCols);
				fk.setStatus(status);
				fk.setNullable(nullable);

				// Add it to the table.
				tbl.addForeignKey(fk);
				element = fk;
			} catch (Exception e) {
				throw new SAXException(e);
			}

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Relation (anywhere).
		else if ("relation".equals(eName)) {
			// Get the ID.
			String id = (String) attributes.get("id");
			try {
				// Work out status, cardinality, and look up the keys
				// at either end.
				ComponentStatus status = ComponentStatus
						.get((String) attributes.get("status"));
				Cardinality card = Cardinality.get((String) attributes
						.get("cardinality"));
				Key firstKey = (Key) this.mappedObjects.get(attributes
						.get("firstKeyId"));
				Key secondKey = (Key) this.mappedObjects.get(attributes
						.get("secondKeyId"));

				// Make it
				Relation rel = new GenericRelation(firstKey, secondKey, card);

				// Set its status.
				rel.setStatus(status);
				element = rel;
			} catch (Exception e) {
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
				throw new SAXException(BuilderBundle
						.getString("maskedRelationOutsideDataSet"));
			DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				Relation rel = (Relation) this.mappedObjects.get(attributes
						.get("relationId"));

				// Mask it.
				w.maskRelation(rel);
				element = rel;
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}

		// Subclass Relation (inside dataset).
		else if ("subclassRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(BuilderBundle
						.getString("subclassRelationOutsideDataSet"));
			DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				Relation rel = (Relation) this.mappedObjects.get(attributes
						.get("relationId"));

				// Subclass it.
				w.flagSubclassRelation(rel);
				element = rel;
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}

		// Concat Relation (inside dataset).
		else if ("concatRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(BuilderBundle
						.getString("concatRelationOutsideDataSet"));
			DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				Relation rel = (Relation) this.mappedObjects.get(attributes
						.get("relationId"));

				// Work out what concat-only type to use.
				String type = (String) attributes.get("concatRelationType");
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
					throw new SAXException(BuilderBundle.getString(
							"unknownConcatRelationType", type));

				// Flag it as concat-only.
				w.flagConcatOnlyRelation(rel, crType);
				element = rel;
			} catch (Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		}

		// Masked Column (inside dataset).
		else if ("maskedColumn".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(BuilderBundle
						.getString("maskedColumnOutsideDataSet"));
			DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the column.
				DataSetColumn col = (DataSetColumn) this.mappedObjects
						.get(attributes.get("columnId"));

				// Mask it.
				w.maskDataSetColumn(col);
				element = col;
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}

		// Partition Column (inside dataset).
		else if ("partitionColumn".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(BuilderBundle
						.getString("partitionColumnOutsideDataSet"));
			DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the column.
				WrappedColumn col = (WrappedColumn) this.mappedObjects
						.get(attributes.get("columnId"));

				// Work out the partition type.
				String type = (String) attributes.get("partitionedColumnType");
				PartitionedColumnType resolvedType = null;
				if ("singleValue".equals(type)) {
					String value = null;
					boolean useNull = ((String) attributes.get("useNull"))
							.equals("true");
					if (!useNull)
						value = (String) attributes.get("value");
					resolvedType = new SingleValue(value, useNull);
				} else if ("valueCollection".equals(type)) {
					// Values are comma-separated.
					List valueList = new ArrayList();
					if (attributes.containsKey("values"))
						valueList.addAll(Arrays.asList(((String) attributes
								.get("values")).split("\\s*,\\s*")));
					boolean includeNull = ((String) attributes.get("useNull"))
							.equals("true");
					// Make the collection.
					resolvedType = new ValueCollection(valueList, includeNull);
				} else if ("uniqueValues".equals(type))
					resolvedType = new UniqueValues();
				else
					throw new SAXException(BuilderBundle.getString(
							"unknownPartitionColumnType", type));

				// Flag the column as partitioned.
				w.flagPartitionedWrappedColumn(col, resolvedType);
				element = col;
			} catch (Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		}

		// DataSet (anywhere).
		else if ("dataset".equals(eName)) {
			try {
				// Look up the name, optimiser type, partition on schema flag,
				// central table reference, and mart constructor reference.
				// Resolve them all.
				String name = (String) attributes.get("name");
				Boolean partitionOnSchema = Boolean.valueOf((String) attributes
						.get("partitionOnSchema"));
				Table centralTable = (Table) this.mappedObjects.get(attributes
						.get("centralTableId"));
				String optType = (String) attributes.get("optimiser");

				// Construct the dataset.
				DataSet ds = new DataSet(this.constructedMart, centralTable,
						name);

				// Work out the optimiser.
				DataSetOptimiserType opt = null;
				if ("NONE".equals(optType))
					opt = DataSetOptimiserType.NONE;
				else if ("LEFTJOIN".equals(optType))
					opt = DataSetOptimiserType.LEFTJOIN;
				else if ("COLUMN".equals(optType))
					opt = DataSetOptimiserType.COLUMN;
				else if ("TABLE".equals(optType))
					opt = DataSetOptimiserType.TABLE;
				else
					throw new SAXException(BuilderBundle.getString(
							"unknownOptimiserType", optType));

				// Assign the mart constructor, optimiser, and partition on
				// schema settings.
				ds.setDataSetOptimiserType(opt);
				if (partitionOnSchema != null)
					ds.setPartitionOnSchema(partitionOnSchema.booleanValue());
				element = ds;
			} catch (Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		}

		// Other tags are unknown.
		else
			throw new SAXException(BuilderBundle.getString("unknownTag", eName));

		// Stick the element on the stack.
		if (element != null)
			this.objectStack.push(element);
	}

	public void endElement(String namespaceURI, String sName, String qName)
			throws SAXException {
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
	private void openElement(String name, Writer xmlWriter) throws IOException {
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
	private void closeElement(String name, Writer xmlWriter) throws IOException {
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
	private void writeAttribute(String name, String value, Writer xmlWriter)
			throws IOException {
		// Write it.
		xmlWriter.write(" ");
		xmlWriter.write(name);
		xmlWriter.write("=\"");
		xmlWriter.write(value);
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
	private void writeAttribute(String name, String[] values, Writer xmlWriter)
			throws IOException {
		// Write it.
		StringBuffer sb = new StringBuffer();
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
	private void writeSchema(Schema schema, Writer xmlWriter)
			throws IOException, BuilderException {
		// What kind of schema is it?
		if (schema instanceof JDBCSchema) {
			// It's a JDBC schema.
			this.openElement("jdbcSchema", xmlWriter);
			JDBCSchema jdbcSchema = (JDBCSchema) schema;

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
			this.writeAttribute("keyguessing",
					jdbcSchema.getKeyGuessing() ? "true" : "false", xmlWriter);
		}
		// Other schema types are not recognised.
		else
			throw new BuilderException(BuilderBundle.getString(
					"unknownSchemaType", schema.getClass().getName()));

		// Write out the contents, and note the relations.
		this.writeSchemaContents(schema, xmlWriter);

		// What kind of schema was it?
		// JDBC?
		if (schema instanceof JDBCSchema)
			this.closeElement("jdbcSchema", xmlWriter);
		// Others?
		else
			throw new BuilderException(BuilderBundle.getString(
					"unknownSchemaType", schema.getClass().getName()));
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
	private void writeSchemaContents(Schema schema, Writer xmlWriter)
			throws IOException, BuilderException {
		// Write out tables inside each schema.
		for (Iterator ti = schema.getTables().iterator(); ti.hasNext();) {
			Table table = (Table) ti.next();
			String tableMappedID = "" + this.currentElementID++;
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
				Table underlyingTable = ((DataSetTable) table)
						.getUnderlyingTable();
				if (underlyingTable != null)
					this.writeAttribute("underlyingTableId",
							(String) this.reverseMappedObjects
									.get(underlyingTable), xmlWriter);

				// Write out the underlying relations.
				List underRelIds = new ArrayList();
				for (Iterator i = ((DataSetTable) table)
						.getUnderlyingRelations().iterator(); i.hasNext();)
					underRelIds.add(this.reverseMappedObjects.get(i.next()));
				this.writeAttribute("underlyingRelationIds",
						(String[]) underRelIds.toArray(new String[0]),
						xmlWriter);

				// Write out the underlying keys.
				List underKeyIds = new ArrayList();
				for (Iterator i = ((DataSetTable) table).getUnderlyingKeys()
						.iterator(); i.hasNext();)
					underKeyIds.add(this.reverseMappedObjects.get(i.next()));
				this.writeAttribute("underlyingKeyIds", (String[]) underKeyIds
						.toArray(new String[0]), xmlWriter);
			}

			// Write out columns inside each table.
			for (Iterator ci = table.getColumns().iterator(); ci.hasNext();) {
				Column col = (Column) ci.next();
				String colMappedID = "" + this.currentElementID++;
				this.reverseMappedObjects.put(col, colMappedID);

				// Start column.
				this.openElement("column", xmlWriter);
				this.writeAttribute("id", colMappedID, xmlWriter);
				this.writeAttribute("name", col.getName(), xmlWriter);
				this.writeAttribute("originalName", col.getOriginalName(),
						xmlWriter);

				// Dataset column?
				if (col instanceof DataSetColumn) {
					DataSetColumn dcol = (DataSetColumn) col;
					Relation underlyingRelation = dcol.getUnderlyingRelation();
					String underlyingRelationId = "null";
					if (underlyingRelation != null)
						underlyingRelationId = (String) this.reverseMappedObjects
								.get(underlyingRelation);
					this.writeAttribute("underlyingRelationId",
							underlyingRelationId, xmlWriter);
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
					// Others
					else
						throw new BuilderException(BuilderBundle.getString(
								"unknownDatasetColumnType", dcol.getClass()
										.getName()));
				}
				// Generic column?
				else if (col instanceof GenericColumn) {
					// Nothing extra required here.
				}
				// Others
				else
					throw new BuilderException(BuilderBundle.getString(
							"unknownColumnType", col.getClass().getName()));

				// Close off column element.
				this.closeElement("column", xmlWriter);
			}

			// Write out keys inside each table. Remember relations as
			// we go along.
			for (Iterator ki = table.getKeys().iterator(); ki.hasNext();) {
				Key key = (Key) ki.next();
				String keyMappedID = "" + this.currentElementID++;
				this.reverseMappedObjects.put(key, keyMappedID);

				String elem = null;
				if (key instanceof PrimaryKey)
					elem = "primaryKey";
				else if (key instanceof ForeignKey)
					elem = "foreignKey";
				else
					throw new BuilderException(BuilderBundle.getString(
							"unknownKey", key.getClass().getName()));

				this.openElement(elem, xmlWriter);
				this.writeAttribute("id", keyMappedID, xmlWriter);
				if (elem.equals("foreignKey"))
					this.writeAttribute("nullable", ""
							+ ((ForeignKey) key).getNullable(), xmlWriter);
				List columnIds = new ArrayList();
				for (Iterator kci = key.getColumns().iterator(); kci.hasNext();)
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
	private void writeRelations(Collection relations, Writer xmlWriter)
			throws IOException {
		// Write out relations.
		for (Iterator i = relations.iterator(); i.hasNext();) {
			Relation r = (Relation) i.next();
			String relMappedID = "" + this.currentElementID++;
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
	private void writeXML(Mart mart, Writer xmlWriter) throws IOException,
			BuilderException {
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
		Set externalRelations = new HashSet();
		for (Iterator i = mart.getSchemas().iterator(); i.hasNext();) {
			Schema schema = (Schema) i.next();
			if (schema instanceof SchemaGroup) {
				this.openElement("schemaGroup", xmlWriter);
				this.writeAttribute("name", schema.getName(), xmlWriter);
				// Write group itself.
				this.writeSchemaContents(schema, xmlWriter);
				// Write member schemas.
				for (Iterator j = ((SchemaGroup) schema).getSchemas()
						.iterator(); j.hasNext();)
					this.writeSchema((Schema) j.next(), xmlWriter);
				this.closeElement("schemaGroup", xmlWriter);
			} else {
				this.writeSchema(schema, xmlWriter);
			}
			externalRelations.addAll(schema.getExternalRelations());
		}

		// Write out relations.
		this.writeRelations(externalRelations, xmlWriter);

		// Write out mart constructors. Remember windows as we go along.

		// Write out windows.
		for (Iterator dsi = mart.getDataSets().iterator(); dsi.hasNext();) {
			DataSet ds = (DataSet) dsi.next();
			this.openElement("dataset", xmlWriter);
			this.writeAttribute("name", ds.getName(), xmlWriter);
			this.writeAttribute("centralTableId",
					(String) this.reverseMappedObjects
							.get(ds.getCentralTable()), xmlWriter);
			this.writeAttribute("alt", ds.getCentralTable().toString(),
					xmlWriter);
			this.writeAttribute("optimiser", ds.getDataSetOptimiserType()
					.getName(), xmlWriter);
			this.writeAttribute("partitionOnSchema", Boolean.toString(ds
					.getPartitionOnSchema()), xmlWriter);

			// Write out concat relations inside window. MUST come first else
			// the dataset concat-only cols will complain about not having a
			// relation to refer to.
			for (Iterator x = ds.getConcatOnlyRelations().iterator(); x
					.hasNext();) {
				Relation r = (Relation) x.next();
				this.openElement("concatRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.writeAttribute("concatRelationType", ds
						.getConcatRelationType(r).getName(), xmlWriter);
				this.writeAttribute("alt", r.toString(), xmlWriter);
				this.closeElement("concatRelation", xmlWriter);
			}

			// Write out masked relations inside window. Can go before or after.
			for (Iterator x = ds.getMaskedRelations().iterator(); x.hasNext();) {
				Relation r = (Relation) x.next();
				this.openElement("maskedRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.writeAttribute("alt", r.toString(), xmlWriter);
				this.closeElement("maskedRelation", xmlWriter);
			}

			// Write out subclass relations inside window. Can go before or
			// after.
			for (Iterator x = ds.getSubclassedRelations().iterator(); x
					.hasNext();) {
				Relation r = (Relation) x.next();
				this.openElement("subclassRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.writeAttribute("alt", r.toString(), xmlWriter);
				this.closeElement("subclassRelation", xmlWriter);
			}

			// Write out the contents of the dataset, and note the relations.
			this.writeSchemaContents(ds, xmlWriter);

			// Write out masked columns inside window. MUST go after else will
			// not have
			// dataset cols to refer to.
			for (Iterator x = ds.getMaskedDataSetColumns().iterator(); x
					.hasNext();) {
				Column c = (Column) x.next();
				this.openElement("maskedColumn", xmlWriter);
				this.writeAttribute("columnId",
						(String) this.reverseMappedObjects.get(c), xmlWriter);
				this.writeAttribute("alt", c.toString(), xmlWriter);
				this.closeElement("maskedColumn", xmlWriter);
			}

			// Write out partitioned columns inside window. MUST go after else
			// will not have dataset cols to refer to.
			for (Iterator x = ds.getPartitionedWrappedColumns().iterator(); x
					.hasNext();) {
				WrappedColumn c = (WrappedColumn) x.next();
				this.openElement("partitionColumn", xmlWriter);
				this.writeAttribute("columnId",
						(String) this.reverseMappedObjects.get(c), xmlWriter);
				PartitionedColumnType ptc = ds
						.getPartitionedWrappedColumnType(c);

				// What kind of partition is it?
				// Single value partition?
				if (ptc instanceof SingleValue) {
					SingleValue sv = (SingleValue) ptc;
					this.writeAttribute("partitionedColumnType", "singleValue",
							xmlWriter);
					String value = sv.getValue();
					this.writeAttribute("useNull", sv.getIncludeNull() ? "true"
							: "false", xmlWriter);
					if (value != null)
						this.writeAttribute("value", value, xmlWriter);
				}
				// Unique values partition?
				else if (ptc instanceof UniqueValues)
					this.writeAttribute("partitionedColumnType",
							"uniqueValues", xmlWriter);
				// Values collection partition?
				else if (ptc instanceof ValueCollection) {
					ValueCollection vc = (ValueCollection) ptc;
					this.writeAttribute("partitionedColumnType",
							"valueCollection", xmlWriter);
					// Values are comma-separated.
					List valueList = new ArrayList();
					valueList.addAll(vc.getValues());
					this.writeAttribute("useNull", vc.getIncludeNull() ? "true"
							: "false", xmlWriter);
					if (!valueList.isEmpty())
						this.writeAttribute("values", (String[]) valueList
								.toArray(new String[0]), xmlWriter);
				}
				// Others.
				else
					throw new BuilderException(BuilderBundle.getString(
							"unknownPartitionColumnType", ptc.getClass()
									.getName()));

				// Finish off.
				this.writeAttribute("alt", c.toString(), xmlWriter);
				this.closeElement("partitionColumn", xmlWriter);
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
	public static Mart load(File file) throws IOException, BuilderException {
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		// Parse the input
		MartBuilderXML loader = new MartBuilderXML();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(file, loader);
		} catch (ParserConfigurationException e) {
			throw new BuilderException(BuilderBundle
					.getString("XMLConfigFailed"), e);
		} catch (SAXException e) {
			throw new BuilderException(BuilderBundle
					.getString("XMLUnparseable"), e);
		}
		// Get the constructed object.
		Mart s = loader.constructedMart;
		// Check that it is a schema.
		if (s == null)
			throw new BuilderException(BuilderBundle.getString(
					"fileNotSchemaVersion", DTD_VERSION));
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
	public static void save(Mart schema, File file) throws IOException,
			BuilderException {
		// Open the file.
		FileWriter fw = new FileWriter(file);
		try {
			// Write it out.
			(new MartBuilderXML()).writeXML(schema, fw);
		} catch (IOException e) {
			throw e;
		} catch (BuilderException e) {
			throw e;
		} finally {
			// Close the output stream.
			fw.close();
		}
	}
}
