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

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSetModificationSet;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.SchemaModificationSet;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSetModificationSet.ExpressionColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.SingleValue;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.UniqueValues;
import org.biomart.builder.model.DataSetModificationSet.PartitionedColumnDefinition.ValueCollection;
import org.biomart.builder.model.SchemaModificationSet.ConcatRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedRelationDefinition;
import org.biomart.builder.model.SchemaModificationSet.RestrictedTableDefinition;
import org.biomart.common.controller.JDBCSchema;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.DataModelException;
import org.biomart.common.model.Column;
import org.biomart.common.model.ComponentStatus;
import org.biomart.common.model.Key;
import org.biomart.common.model.Relation;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.model.Column.GenericColumn;
import org.biomart.common.model.Key.ForeignKey;
import org.biomart.common.model.Key.GenericForeignKey;
import org.biomart.common.model.Key.GenericPrimaryKey;
import org.biomart.common.model.Key.PrimaryKey;
import org.biomart.common.model.Relation.Cardinality;
import org.biomart.common.model.Relation.GenericRelation;
import org.biomart.common.model.Schema.GenericSchema;
import org.biomart.common.model.Table.GenericTable;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
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
 * files. This DTD will be found in the <tt>org.biomart.builder.resources</tt>
 * package.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class MartBuilderXML extends DefaultHandler {

	private static final String CURRENT_DTD_VERSION = "0.6";

	private static final String[] SUPPORTED_DTD_VERSIONS = new String[] {
			"0.6", "0.5" };

	private static final String DTD_PUBLIC_ID_START = "-//EBI//DTD MartBuilder ";

	private static final String DTD_PUBLIC_ID_END = "//EN";

	private static final String DTD_URL_START = "http://www.biomart.org/DTD/MartBuilder-";

	private static final String DTD_URL_END = ".dtd";

	private static String currentReadingDTDVersion;

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
	 * @throws DataModelException
	 *             if the content of the file is not valid {@link Mart} XML, or
	 *             has any logical problems.
	 */
	public static Mart load(final File file) throws IOException,
			DataModelException {
		Log.info(Resources.get("logStartLoadingXMLFile", file.getPath()));
		// Use the default (non-validating) parser
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		// Parse the input
		final MartBuilderXML loader = new MartBuilderXML();
		try {
			final SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(file, loader);
		} catch (final ParserConfigurationException e) {
			throw new DataModelException(Resources.get("XMLConfigFailed"), e);
		} catch (final SAXException e) {
			throw new DataModelException(Resources.get("XMLUnparseable"), e);
		}
		// Get the constructed object.
		final Mart mart = loader.getConstructedMart();
		// Check that we got something useful.
		if (mart == null)
			throw new DataModelException(Resources.get("fileNotSchemaVersion",
					MartBuilderXML.CURRENT_DTD_VERSION));
		// Return.
		Log.info(Resources.get("logDoneLoadingXMLFile"));
		return mart;
	}

	/**
	 * The save method takes a {@link Mart} object and writes out XML describing
	 * it to the given {@link File}. This XML can be read by the
	 * {@link MartBuilderXML#load(File)} method.
	 * 
	 * @param mart
	 *            {@link Mart} object containing the data for the file.
	 * @param file
	 *            the {@link File} to save the data to.
	 * @throws IOException
	 *             if there was any problem writing the file.
	 * @throws DataModelException
	 *             if it encounters an object not writable under the current
	 *             DTD.
	 */
	public static void save(final Mart mart, final File file)
			throws IOException, DataModelException {
		Log.info(Resources.get("logStartSavingXMLFile", file.getPath()));
		// Open the file.
		final FileWriter fw = new FileWriter(file);
		try {
			// Write it out.
			(new MartBuilderXML()).writeXML(mart, fw);
		} catch (final IOException e) {
			throw e;
		} catch (final DataModelException e) {
			throw e;
		} finally {
			// Close the output stream.
			fw.close();
		}
		Log.info(Resources.get("logDoneSavingXMLFile"));
	}

	private Mart constructedMart;

	private int currentElementID;

	private String currentOutputElement;

	private int currentOutputIndent;

	private Map mappedObjects;

	private Stack objectStack;

	private Map reverseMappedObjects;

	/**
	 * This class is intended to be used only in a static context. It creates
	 * its own instances internally as required.
	 */
	private MartBuilderXML() {
		this.constructedMart = null;
		this.currentOutputElement = null;
		this.currentOutputIndent = 0;
		this.currentElementID = 1;
	}

	private Mart getConstructedMart() {
		return this.constructedMart;
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
		// Reset the current tag.
		this.currentOutputElement = null;
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

		// Update tag that we are currently writing.
		this.currentOutputElement = name;
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
		if (value == null)
			return;
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
			final String value = values[i];
			if (value != null) {
				if (i > 0)
					sb.append(",");
				sb.append(values[i]);
			}
		}
		this.writeAttribute(name, sb.length() == 0 ? null : sb.toString(),
				xmlWriter);
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
			final boolean writeExternal, final Writer xmlWriter)
			throws IOException {
		// Write out each relation in turn.
		for (final Iterator i = relations.iterator(); i.hasNext();) {
			final Relation r = (Relation) i.next();
			if (writeExternal != r.isExternal())
				continue;
			Log.debug("Writing relation: " + r);

			// Assign the relation an ID.
			final String relMappedID = "" + this.currentElementID++;
			this.reverseMappedObjects.put(r, relMappedID);

			// Write the relation.
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
			this.closeElement("relation", xmlWriter);
		}
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
	 * @throws DataModelException
	 *             if there were any logical problems with the schema.
	 */
	private void writeSchema(final Schema schema, final Writer xmlWriter)
			throws IOException, DataModelException {
		Log.debug("Writing schema: " + schema);
		// What kind of schema is it?
		if (schema instanceof JDBCSchema) {
			Log.debug("Writing JDBC schema");
			// It's a JDBC schema.
			final JDBCSchema jdbcSchema = (JDBCSchema) schema;

			// Begin the schema element.
			this.openElement("jdbcSchema", xmlWriter);

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
			throw new DataModelException(Resources.get("unknownSchemaType",
					schema.getClass().getName()));

		// Write out the contents.
		this.writeSchemaContents(schema, xmlWriter);

		// Close the schema element.
		// What kind of schema was it?
		// JDBC?
		if (schema instanceof JDBCSchema)
			this.closeElement("jdbcSchema", xmlWriter);
		// Others?
		else
			throw new DataModelException(Resources.get("unknownSchemaType",
					schema.getClass().getName()));
	}

	/**
	 * Internal method which writes out the contents of a schema.
	 * 
	 * @param schema
	 *            the {@link Schema} to write out the tables of.
	 * @param xmlWriter
	 *            the writer to write to.
	 * @throws IOException
	 *             if there was a problem writing to file.
	 * @throws AssociationException
	 *             if an unwritable kind of object was found.
	 */
	private void writeSchemaContents(final Schema schema, final Writer xmlWriter)
			throws IOException, DataModelException {
		Log.debug("Writing schema contents for " + schema);

		// Write out tables inside each schema.
		for (final Iterator ti = schema.getTables().iterator(); ti.hasNext();) {
			final Table table = (Table) ti.next();
			Log.debug("Writing table: " + table);

			// Give the table an ID.
			final String tableMappedID = "" + this.currentElementID++;
			this.reverseMappedObjects.put(table, tableMappedID);

			// Start table.
			this.openElement("table", xmlWriter);
			this.writeAttribute("id", tableMappedID, xmlWriter);
			this.writeAttribute("name", table.getName(), xmlWriter);

			// Write out columns inside each table.
			for (final Iterator ci = table.getColumns().iterator(); ci
					.hasNext();) {
				final Column col = (Column) ci.next();
				Log.debug("Writing column: " + col);

				// Give the column an ID.
				final String colMappedID = "" + this.currentElementID++;
				this.reverseMappedObjects.put(col, colMappedID);

				// Start column.
				this.openElement("column", xmlWriter);
				this.writeAttribute("id", colMappedID, xmlWriter);
				this.writeAttribute("name", col.getName(), xmlWriter);
				this.closeElement("column", xmlWriter);
			}

			// Write out keys inside each table. Remember relations as
			// we go along.
			for (final Iterator ki = table.getKeys().iterator(); ki.hasNext();) {
				final Key key = (Key) ki.next();
				Log.debug("Writing key: " + key);

				// Give the key an ID.
				final String keyMappedID = "" + this.currentElementID++;
				this.reverseMappedObjects.put(key, keyMappedID);

				// What kind of key is it?
				String elem = null;
				if (key instanceof PrimaryKey)
					elem = "primaryKey";
				else if (key instanceof ForeignKey)
					elem = "foreignKey";
				else
					throw new DataModelException(Resources.get("unknownKey",
							key.getClass().getName()));

				// Write the key.
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
				this.closeElement(elem, xmlWriter);
			}

			// Finish table.
			this.closeElement("table", xmlWriter);
		}

		// Write relations.
		this.writeRelations(schema.getRelations(), false, xmlWriter);
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
	 * @throws DataModelException
	 *             if it encounters an object not writable under the current
	 *             DTD.
	 */
	private void writeXML(final Mart mart, final Writer xmlWriter)
			throws IOException, DataModelException {
		// Write the headers.
		xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xmlWriter.write("<!DOCTYPE mart PUBLIC \""
				+ MartBuilderXML.DTD_PUBLIC_ID_START
				+ MartBuilderXML.CURRENT_DTD_VERSION
				+ MartBuilderXML.DTD_PUBLIC_ID_START + "\" \""
				+ MartBuilderXML.DTD_URL_START
				+ MartBuilderXML.CURRENT_DTD_VERSION
				+ MartBuilderXML.DTD_URL_END + "\">\n");

		// Initialise the ID counter.
		this.reverseMappedObjects = new HashMap();

		// Start by enclosing the whole lot in a <mart> tag.
		Log.debug("Writing mart: " + mart);
		this.openElement("mart", xmlWriter);

		// Write out each schema.
		final Set externalRelations = new HashSet();
		for (final Iterator i = mart.getSchemas().iterator(); i.hasNext();) {
			final Schema schema = (Schema) i.next();
			this.writeSchema(schema, xmlWriter);
			externalRelations.addAll(schema.getRelations());
		}

		// Write out relations.
		this.writeRelations(externalRelations, true, xmlWriter);

		// Write out datasets.
		for (final Iterator dsi = mart.getDataSets().iterator(); dsi.hasNext();) {
			final DataSet ds = (DataSet) dsi.next();
			Log.debug("Writing dataset: " + ds);
			this.openElement("dataset", xmlWriter);
			this.writeAttribute("name", ds.getName(), xmlWriter);
			this.writeAttribute("centralTableId",
					(String) this.reverseMappedObjects
							.get(ds.getCentralTable()), xmlWriter);
			this.writeAttribute("optimiser", ds.getDataSetOptimiserType()
					.getName(), xmlWriter);
			this.writeAttribute("invisible", Boolean
					.toString(ds.getInvisible()), xmlWriter);

			// Get schema and dataset mods.
			final SchemaModificationSet schemaMods = ds
					.getSchemaModifications();
			final DataSetModificationSet dsMods = ds.getDataSetModifications();

			// Write out subclass relations inside dataset. Can go before or
			// after.
			for (final Iterator x = schemaMods.getSubclassedRelations()
					.iterator(); x.hasNext();) {
				final Relation r = (Relation) x.next();
				this.openElement("subclassRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(r), xmlWriter);
				this.closeElement("subclassRelation", xmlWriter);
			}

			// Write out renamed columns and tables.
			for (final Iterator x = dsMods.getTableRenames().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				this.openElement("renamedTable", xmlWriter);
				this.writeAttribute("tableKey", (String) entry.getKey(),
						xmlWriter);
				this.writeAttribute("newName", (String) entry.getValue(),
						xmlWriter);
				this.closeElement("renamedTable", xmlWriter);
			}
			for (final Iterator x = dsMods.getColumnRenames().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Map) entry.getValue()).entrySet()
						.iterator(); y.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) y.next();
					this.openElement("renamedColumn", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this.writeAttribute("colKey", (String) entry2.getKey(),
							xmlWriter);
					this.writeAttribute("newName", (String) entry2.getValue(),
							xmlWriter);
					this.closeElement("renamedColumn", xmlWriter);
				}
			}

			// Write out non-inherited columns and tables.
			for (final Iterator x = dsMods.getNonInheritedColumns().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Collection) entry.getValue())
						.iterator(); y.hasNext();) {
					this.openElement("nonInheritedColumn", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this.writeAttribute("colKey", (String) y.next(), xmlWriter);
					this.closeElement("nonInheritedColumn", xmlWriter);
				}
			}

			// Write out masked columns and tables.
			for (final Iterator x = dsMods.getMaskedColumns().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Collection) entry.getValue())
						.iterator(); y.hasNext();) {
					this.openElement("maskedColumn", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this.writeAttribute("colKey", (String) y.next(), xmlWriter);
					this.closeElement("maskedColumn", xmlWriter);
				}
			}

			// Write out partitioned columns.
			for (final Iterator x = dsMods.getPartitionedColumns().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Map) entry.getValue()).entrySet()
						.iterator(); y.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) y.next();
					final PartitionedColumnDefinition pc = (PartitionedColumnDefinition) entry2
							.getValue();
					final String pcType = (pc instanceof SingleValue) ? "singleValue"
							: ((pc instanceof ValueCollection) ? "valueCollection"
									: "uniqueValues");
					this.openElement("partitionedColumn", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this.writeAttribute("colKey", (String) entry2.getKey(),
							xmlWriter);
					this.writeAttribute("partitionType", pcType, xmlWriter);
					if (pc instanceof SingleValue) {
						this.writeAttribute("partitionUseNull", ""
								+ ((SingleValue) pc).getIncludeNull(),
								xmlWriter);
						this.writeAttribute("partitionValue",
								((SingleValue) pc).getValue(), xmlWriter);
					} else if (pc instanceof ValueCollection) {
						this.writeAttribute("partitionUseNull", ""
								+ ((ValueCollection) pc).getIncludeNull(),
								xmlWriter);
						this.writeAttribute("partitionValues",
								(String[]) ((ValueCollection) pc).getValues()
										.toArray(new String[0]), xmlWriter);
					} else if (pc instanceof UniqueValues) {
						// Nothing extra needed for unique values.
					}
					this.closeElement("partitionedColumn", xmlWriter);
				}
			}

			// Write out masked relations inside dataset. Can go before or
			// after.
			for (final Iterator x = schemaMods.getMaskedRelations().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Collection) entry.getValue())
						.iterator(); y.hasNext();) {
					final Relation r = (Relation) y.next();
					this.openElement("maskedRelation", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this.writeAttribute("relationId",
							(String) this.reverseMappedObjects.get(r),
							xmlWriter);
					this.closeElement("maskedRelation", xmlWriter);
				}
			}

			// Write out masked tables inside dataset. Can go before or
			// after.
			for (final Iterator x = dsMods.getMaskedTables().iterator(); x
					.hasNext();) {
				this.openElement("maskedTable", xmlWriter);
				this.writeAttribute("tableKey", (String) x.next(), xmlWriter);
				this.closeElement("maskedTable", xmlWriter);
			}

			// Write out merged tables inside dataset. Can go before or
			// after.
			for (final Iterator x = schemaMods.getMergedRelations().iterator(); x
					.hasNext();) {
				this.openElement("mergedRelation", xmlWriter);
				this.writeAttribute("relationId",
						(String) this.reverseMappedObjects.get(x.next()),
						xmlWriter);
				this.closeElement("mergedRelation", xmlWriter);
			}

			// Write out forced relations inside dataset. Can go before or
			// after.
			for (final Iterator x = schemaMods.getForceIncludeRelations()
					.entrySet().iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Collection) entry.getValue())
						.iterator(); y.hasNext();) {
					final Relation r = (Relation) y.next();
					this.openElement("forcedRelation", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this.writeAttribute("relationId",
							(String) this.reverseMappedObjects.get(r),
							xmlWriter);
					this.closeElement("forcedRelation", xmlWriter);
				}
			}

			// Write out restricted tables.
			for (final Iterator x = schemaMods.getRestrictedTables().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Map) entry.getValue()).entrySet()
						.iterator(); y.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) y.next();
					final RestrictedTableDefinition restrict = (RestrictedTableDefinition) entry2
							.getValue();
					this.openElement("restrictedTable", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this.writeAttribute("tableId",
							(String) this.reverseMappedObjects.get(entry2
									.getKey()), xmlWriter);
					final StringBuffer cols = new StringBuffer();
					final StringBuffer names = new StringBuffer();
					for (final Iterator z = restrict.getAliases().entrySet()
							.iterator(); z.hasNext();) {
						Map.Entry entry3 = (Map.Entry) z.next();
						cols.append((String) this.reverseMappedObjects
								.get(entry3.getKey()));
						names.append((String) entry3.getValue());
						if (z.hasNext()) {
							cols.append(',');
							names.append(',');
						}
					}
					this.writeAttribute("aliasColumnIds", cols.toString(),
							xmlWriter);
					this.writeAttribute("aliasNames", names.toString(),
							xmlWriter);
					this.writeAttribute("expression", restrict.getExpression(),
							xmlWriter);
					this.closeElement("restrictedTable", xmlWriter);
				}
			}

			// Write out concat relations.
			for (final Iterator x = schemaMods.getConcatRelations().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Map) entry.getValue()).entrySet()
						.iterator(); y.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) y.next();
					for (final Iterator z = ((Map) entry2.getValue())
							.entrySet().iterator(); z.hasNext();) {
						final Map.Entry entry3 = (Map.Entry) z.next();
						this.openElement("concatRelation", xmlWriter);
						this.writeAttribute("tableKey",
								(String) entry.getKey(), xmlWriter);
						this.writeAttribute("relationId",
								(String) this.reverseMappedObjects.get(entry2
										.getKey()), xmlWriter);
						this.writeAttribute("index", "" + entry3.getKey(),
								xmlWriter);
						final ConcatRelationDefinition restrict = (ConcatRelationDefinition) entry3
								.getValue();
						final StringBuffer cols = new StringBuffer();
						final StringBuffer names = new StringBuffer();
						for (final Iterator a = restrict.getAliases()
								.entrySet().iterator(); a.hasNext();) {
							Map.Entry entry4 = (Map.Entry) a.next();
							cols.append((String) this.reverseMappedObjects
									.get(entry4.getKey()));
							names.append((String) entry4.getValue());
							if (z.hasNext()) {
								cols.append(',');
								names.append(',');
							}
						}
						this.writeAttribute("colKey", restrict.getColKey(),
								xmlWriter);
						this.writeAttribute("aliasColumnIds", cols.toString(),
								xmlWriter);
						this.writeAttribute("aliasNames", names.toString(),
								xmlWriter);
						this.writeAttribute("expression", restrict
								.getExpression(), xmlWriter);
						this.writeAttribute("rowSep", restrict.getRowSep(),
								xmlWriter);
						this.closeElement("concatRelation", xmlWriter);
					}
				}
			}

			// Write out expression columns.
			for (final Iterator x = dsMods.getExpressionColumns().entrySet()
					.iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Collection) entry.getValue())
						.iterator(); y.hasNext();) {
					final ExpressionColumnDefinition expcol = (ExpressionColumnDefinition) y
							.next();
					this.openElement("expressionColumn", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this
							.writeAttribute("colKey", expcol.getColKey(),
									xmlWriter);
					final StringBuffer cols = new StringBuffer();
					final StringBuffer names = new StringBuffer();
					for (final Iterator z = expcol.getAliases().entrySet()
							.iterator(); z.hasNext();) {
						Map.Entry entry3 = (Map.Entry) z.next();
						cols.append((String) entry3.getKey());
						names.append((String) entry3.getValue());
						if (z.hasNext()) {
							cols.append(',');
							names.append(',');
						}
					}
					this.writeAttribute("aliasColumnNames", cols.toString(),
							xmlWriter);
					this.writeAttribute("aliasNames", names.toString(),
							xmlWriter);
					this.writeAttribute("expression", expcol.getExpression(),
							xmlWriter);
					this.writeAttribute("groupBy", "" + expcol.isGroupBy(),
							xmlWriter);
					this.closeElement("expressionColumn", xmlWriter);
				}
			}

			// Write out compound relations inside dataset. Can go before or
			// after.
			for (final Iterator x = schemaMods.getCompoundRelations()
					.entrySet().iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Map) entry.getValue()).entrySet()
						.iterator(); y.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) y.next();
					this.openElement("compoundRelation", xmlWriter);
					this.writeAttribute("tableKey", (String) entry.getKey(),
							xmlWriter);
					this
							.writeAttribute("relationId",
									(String) this.reverseMappedObjects
											.get((Relation) entry2.getKey()),
									xmlWriter);
					this.writeAttribute("n", ((Integer) entry2.getValue())
							.toString(), xmlWriter);
					this.closeElement("compoundRelation", xmlWriter);
				}
			}

			// Write out restricted relations.
			for (final Iterator x = schemaMods.getRestrictedRelations()
					.entrySet().iterator(); x.hasNext();) {
				final Map.Entry entry = (Map.Entry) x.next();
				for (final Iterator y = ((Map) entry.getValue()).entrySet()
						.iterator(); y.hasNext();) {
					final Map.Entry entry2 = (Map.Entry) y.next();
					for (final Iterator z = ((Map) entry2.getValue())
							.entrySet().iterator(); z.hasNext();) {
						final Map.Entry entry3 = (Map.Entry) z.next();
						this.openElement("restrictedRelation", xmlWriter);
						this.writeAttribute("tableKey",
								(String) entry.getKey(), xmlWriter);
						this.writeAttribute("relationId",
								(String) this.reverseMappedObjects.get(entry2
										.getKey()), xmlWriter);
						this.writeAttribute("index", "" + entry3.getKey(),
								xmlWriter);
						final RestrictedRelationDefinition restrict = (RestrictedRelationDefinition) entry3
								.getValue();
						final StringBuffer lcols = new StringBuffer();
						final StringBuffer lnames = new StringBuffer();
						for (final Iterator a = restrict.getLeftAliases()
								.entrySet().iterator(); a.hasNext();) {
							Map.Entry entry4 = (Map.Entry) a.next();
							lcols.append((String) this.reverseMappedObjects
									.get(entry4.getKey()));
							lnames.append((String) entry4.getValue());
							if (z.hasNext()) {
								lcols.append(',');
								lnames.append(',');
							}
						}
						this.writeAttribute("leftAliasColumnIds", lcols
								.toString(), xmlWriter);
						this.writeAttribute("leftAliasNames",
								lnames.toString(), xmlWriter);
						final StringBuffer rcols = new StringBuffer();
						final StringBuffer rnames = new StringBuffer();
						for (final Iterator a = restrict.getRightAliases()
								.entrySet().iterator(); a.hasNext();) {
							Map.Entry entry4 = (Map.Entry) a.next();
							rcols.append((String) this.reverseMappedObjects
									.get(entry4.getKey()));
							rnames.append((String) entry4.getValue());
							if (z.hasNext()) {
								rcols.append(',');
								rnames.append(',');
							}
						}
						this.writeAttribute("rightAliasColumnIds", rcols
								.toString(), xmlWriter);
						this.writeAttribute("rightAliasNames", rnames
								.toString(), xmlWriter);
						this.writeAttribute("expression", restrict
								.getExpression(), xmlWriter);
						this.closeElement("restrictedRelation", xmlWriter);
					}
				}
			}

			// Finish dataset.
			this.closeElement("dataset", xmlWriter);
		}

		// Finished! Close the mart tag.
		this.closeElement("mart", xmlWriter);

		// Flush.
		xmlWriter.flush();
	}

	public void endDocument() throws SAXException {
		// No action required.
	}

	public void endElement(final String namespaceURI, final String sName,
			final String qName) throws SAXException {
		// Work out what element it is we are closing.
		String eName = sName;
		if ("".equals(eName))
			eName = qName;

		// Pop the element off the stack so that the next element
		// knows that it is inside the parent of this one.
		this.objectStack.pop();
	}

	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException {
		Log.debug("Resolving XML entity " + publicId + " " + systemId);
		// If the public ID is our own DTD version, then we can use our
		// own copy of the DTD in our resources bundle.
		MartBuilderXML.currentReadingDTDVersion = null;
		for (int i = 0; i < MartBuilderXML.SUPPORTED_DTD_VERSIONS.length
				&& MartBuilderXML.currentReadingDTDVersion == null; i++) {
			final String currPub = MartBuilderXML.DTD_PUBLIC_ID_START
					+ MartBuilderXML.SUPPORTED_DTD_VERSIONS[i]
					+ MartBuilderXML.DTD_PUBLIC_ID_END;
			final String currUrl = MartBuilderXML.DTD_URL_START
					+ MartBuilderXML.SUPPORTED_DTD_VERSIONS[i]
					+ MartBuilderXML.DTD_URL_END;
			if (currPub.equals(publicId) || currUrl.equals(systemId))
				MartBuilderXML.currentReadingDTDVersion = MartBuilderXML.SUPPORTED_DTD_VERSIONS[i];
		}
		if (MartBuilderXML.currentReadingDTDVersion != null) {
			final String dtdDoc = "MartBuilder-"
					+ MartBuilderXML.currentReadingDTDVersion + ".dtd";
			Log.debug("Resolved to " + dtdDoc);
			return new InputSource(Resources.getResourceAsStream(dtdDoc));
		}
		// By returning null we allow the default behaviour for all other
		// DTDs.
		else {
			Log.debug("Not resolved");
			return null;
		}
	}

	public void startDocument() throws SAXException {
		// Reset all our maps of objects to IDs and clear
		// the stack of objects waiting to be processed.
		Log.debug("Started parsing XML document");
		this.mappedObjects = new HashMap();
		this.reverseMappedObjects = new HashMap();
		this.objectStack = new Stack();
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

		// Start by assuming the tag produces an unnested element;
		Object element = "";

		// Now, attempt to recognise the tag by checking its name
		// against a set of names known to us.
		Log.debug("Reading tag " + eName);

		// Mart (top-level only).
		if ("mart".equals(eName)) {
			// Start building a new mart. There can only be one mart tag
			// per file, as if more than one is found, the later tags
			// will override the earlier ones.
			element = this.constructedMart = new Mart();
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
				schema.storeInHistory();
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

			// Generic schema?
			if (schema instanceof GenericSchema)
				try {
					final Table table = new GenericTable(name, schema);
					schema.addTable(table);
					element = table;
				} catch (final Exception e) {
					throw new SAXException(e);
				}
			else if (schema instanceof DataSet
					&& MartBuilderXML.currentReadingDTDVersion.equals("0.5")) {
				// In this case we don't care, because we need this
				// for backward compatibility with 0.5. So, ignore it
				// with no warning. We put a dummy DataSetTable on
				// the stack.
				element = new DataSetTable(name, (DataSet) schema,
						DataSetTableType.MAIN, null, null);
			} else
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

			try {
				// DataSet table column?
				if (tbl instanceof DataSetTable
						&& MartBuilderXML.currentReadingDTDVersion
								.equals("0.5")) {
					// Since 0.5 we don't bother reading this stuff.
					// But, we don't thrown an exception as it is a valid
					// tag under 0.5.
				}

				// Generic column?
				else if (tbl instanceof GenericTable) {
					final Column column = new GenericColumn(name, tbl);
					tbl.addColumn(column);
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

			// We don't do these for dataset tables since 0.6 as they
			// get regenerated automatically.
			if (tbl instanceof DataSetTable)
				return;

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

			// We don't do these for dataset tables since 0.6 as they
			// get regenerated automatically.
			if (tbl instanceof DataSetTable)
				return;

			// Get the ID.
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

				// We don't do these for dataset tables since 0.6 as they
				// get regenerated automatically. We can tell this is a
				// dataset table because at least one key ID will not
				// be found.
				if (firstKey == null || secondKey == null)
					return;

				// Make it
				final Relation rel = new GenericRelation(firstKey, secondKey,
						card);
				firstKey.addRelation(rel);
				secondKey.addRelation(rel);

				// Set its status.
				rel.setStatus(status);
				element = rel;
			} catch (final Exception e) {
				throw new SAXException(e);
			}

			// Store it in the map of IDed objects.
			this.mappedObjects.put(id, element);
		}

		// Merged Table (inside dataset).
		else if ("mergedRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("mergedRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));

				// Mask it.
				if (rel != null)
					w.getSchemaModifications().getMergedRelations().add(rel);
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Masked Table (inside dataset).
		else if ("maskedTable".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("maskedTableOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the table.
				final String tableKey = (String) attributes.get("tableKey");

				// Mask it.
				if (tableKey != null)
					w.getDataSetModifications().getMaskedTables().add(tableKey);
			} catch (final Exception e) {
				throw new SAXException(e);
			}
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
				// For 0.5-compatibility, default the key to dataset-wide.
				final String tableKey = MartBuilderXML.currentReadingDTDVersion
						.equals("0.5") ? SchemaModificationSet.DATASET
						: (String) attributes.get("tableKey");

				// Mask it.
				if (tableKey != null && rel != null) {
					final Map maskMap = w.getSchemaModifications()
							.getMaskedRelations();
					if (!maskMap.containsKey(tableKey))
						maskMap.put(tableKey, new HashSet());
					((Collection) maskMap.get(tableKey)).add(rel);
				}
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Compound Relation (inside dataset).
		else if ("compoundRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("compoundRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));
				final String tableKey = (String) attributes.get("tableKey");
				final Integer n = Integer.valueOf((String) attributes.get("n"));

				// Compound it.
				if (rel != null && tableKey != null && n != null) {
					final Map compMap = w.getSchemaModifications()
							.getCompoundRelations();
					if (!compMap.containsKey(tableKey))
						compMap.put(tableKey, new HashMap());
					((Map) compMap.get(tableKey)).put(rel, n);
				}
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Forced Relation (inside dataset).
		else if ("forcedRelation".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("forcedRelationOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));
				final String tableKey = (String) attributes.get("tableKey");

				// Mask it.
				if (rel != null && tableKey != null) {
					final Map fMap = w.getSchemaModifications()
							.getForceIncludeRelations();
					if (!fMap.containsKey(tableKey))
						fMap.put(tableKey, new HashSet());
					((Collection) fMap.get(tableKey)).add(rel);
				}
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
				if (rel != null)
					w.getSchemaModifications().setSubclassedRelation(rel);
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Non-inherited Column (inside dataset).
		else if ("nonInheritedColumn".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("nonInheritedColumnOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final String tableKey = (String) attributes.get("tableKey");
				final String colKey = (String) attributes.get("colKey");

				// Subclass it.
				if (tableKey != null && colKey != null) {
					final Map colMap = w.getDataSetModifications()
							.getNonInheritedColumns();
					if (!colMap.containsKey(tableKey))
						colMap.put(tableKey, new HashSet());
					((Collection) colMap.get(tableKey)).add(colKey);
				}
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Partitioned Column (inside dataset).
		else if ("partitionedColumn".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("partitionedColumnOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final String tableKey = (String) attributes.get("tableKey");
				final String colKey = (String) attributes.get("colKey");

				final String partitionType = (String) attributes
						.get("partitionType");
				PartitionedColumnDefinition resolvedPartitionType;
				if (partitionType == null || "null".equals(partitionType))
					resolvedPartitionType = null;
				else if ("singleValue".equals(partitionType)) {
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
						valueList.addAll(Arrays.asList(((String) attributes
								.get("partitionValues")).split("\\s*,\\s*")));
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

				// Partition it.
				if (tableKey != null && colKey != null) {
					final Map colMap = w.getDataSetModifications()
							.getPartitionedColumns();
					if (!colMap.containsKey(tableKey))
						colMap.put(tableKey, new HashMap());
					((Map) colMap.get(tableKey)).put(colKey,
							resolvedPartitionType);
				}
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Masked Column (inside dataset).
		else if ("maskedColumn".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("maskedColumnOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the relation.
				final String tableKey = (String) attributes.get("tableKey");
				final String colKey = (String) attributes.get("colKey");

				// Subclass it.
				if (tableKey != null && colKey != null) {
					final Map colMap = w.getDataSetModifications()
							.getMaskedColumns();
					if (!colMap.containsKey(tableKey))
						colMap.put(tableKey, new HashSet());
					((Collection) colMap.get(tableKey)).add(colKey);
				}
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Renamed table (inside dataset).
		else if ("renamedTable".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("renamedTableOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the values.
				final String tableKey = (String) attributes.get("tableKey");
				final String newName = (String) attributes.get("newName");
				if (tableKey != null && newName != null) {
					final Map colMap = w.getDataSetModifications()
							.getTableRenames();
					colMap.put(tableKey, newName);
				}
			} catch (final Exception e) {
				throw new SAXException(e);
			}
		}

		// Renamed column (inside dataset).
		else if ("renamedColumn".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("renamedColumnOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the values.
				final String tableKey = (String) attributes.get("tableKey");
				final String colKey = (String) attributes.get("colKey");
				final String newName = (String) attributes.get("newName");
				if (tableKey != null && colKey != null && newName != null) {
					final Map colMap = w.getDataSetModifications()
							.getColumnRenames();
					if (!colMap.containsKey(tableKey))
						colMap.put(tableKey, new HashMap());
					((Map) colMap.get(tableKey)).put(colKey, newName);
				}
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
				// Look up the restriction.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));
				final String tableKey = (String) attributes.get("tableKey");
				final int index = Integer.parseInt((String) attributes
						.get("index"));

				// Get the aliases to use for the first table.
				final Map aliases = new HashMap();
				final String[] aliasColumnIds = ((String) attributes
						.get("aliasColumnIds")).split(",");
				final String[] aliasNames = ((String) attributes
						.get("aliasNames")).split(",");
				for (int i = 0; i < aliasColumnIds.length; i++) {
					final Column wrapped = (Column) this.mappedObjects
							.get(aliasColumnIds[i]);
					if (wrapped != null)
						aliases.put(wrapped, aliasNames[i]);
				}
				// Get the expression to use.
				final String expr = (String) attributes.get("expression");
				final String rowSep = (String) attributes.get("rowSep");
				final String colKey = (String) attributes.get("colKey");

				// Flag it as restricted
				if (expr != null && rel != null && tableKey != null
						&& !aliases.isEmpty() && rowSep != null
						&& colKey != null) {
					final ConcatRelationDefinition restrict = new ConcatRelationDefinition(
							expr, aliases, rowSep, colKey);
					final Map restMap = w.getSchemaModifications()
							.getConcatRelations();
					if (!restMap.containsKey(tableKey))
						restMap.put(tableKey, new HashMap());
					if (!((Map) restMap.get(tableKey)).containsKey(rel))
						((Map) restMap.get(tableKey)).put(rel, new HashMap());
					((Map) ((Map) restMap.get(tableKey)).get(rel)).put(
							new Integer(index), restrict);
				}
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
						.get("restrictedTableOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				// Look up the restriction.
				final Table tbl = (Table) this.mappedObjects.get(attributes
						.get("tableId"));
				// Default to dataset-wide restriction if 0.5 syntax used.
				final String tableKey = MartBuilderXML.currentReadingDTDVersion
						.equals("0.5") ? SchemaModificationSet.DATASET
						: (String) attributes.get("tableKey");

				// Get the aliases to use for the first table.
				final Map aliases = new HashMap();
				final String[] aliasColumnIds = ((String) attributes
						.get("aliasColumnIds")).split(",");
				final String[] aliasNames = ((String) attributes
						.get("aliasNames")).split(",");
				for (int i = 0; i < aliasColumnIds.length; i++) {
					final Column wrapped = (Column) this.mappedObjects
							.get(aliasColumnIds[i]);
					if (wrapped != null)
						aliases.put(wrapped, aliasNames[i]);
				}
				// Get the expression to use.
				final String expr = (String) attributes.get("expression");

				// Flag it as restricted
				if (expr != null && !aliases.isEmpty() && tableKey != null
						&& tbl != null) {
					final RestrictedTableDefinition restrict = new RestrictedTableDefinition(
							expr, aliases);
					final Map restMap = w.getSchemaModifications()
							.getRestrictedTables();
					if (!restMap.containsKey(tableKey))
						restMap.put(tableKey, new HashMap());
					((Map) restMap.get(tableKey)).put(tbl, restrict);
				}
			} catch (final Exception e) {
				if (e instanceof SAXException)
					throw (SAXException) e;
				else
					throw new SAXException(e);
			}
		}

		// Expression Column (inside dataset).
		else if ("expressionColumn".equals(eName)) {
			// What dataset does it belong to? Throw a wobbly if none.
			if (this.objectStack.empty()
					|| !(this.objectStack.peek() instanceof DataSet))
				throw new SAXException(Resources
						.get("expressionColumnOutsideDataSet"));
			final DataSet w = (DataSet) this.objectStack.peek();

			try {
				final String colKey = (String) attributes.get("colKey");
				final String tableKey = (String) attributes.get("tableKey");

				// Get the aliases to use for the first table.
				final Map aliases = new HashMap();
				final String[] aliasColumnNames = ((String) attributes
						.get("aliasColumnNames")).split(",");
				final String[] aliasNames = ((String) attributes
						.get("aliasNames")).split(",");
				for (int i = 0; i < aliasColumnNames.length; i++)
					aliases.put(aliasColumnNames[i], aliasNames[i]);
				// Get the expression to use.
				final String expr = (String) attributes.get("expression");
				final boolean groupBy = Boolean.valueOf(
						(String) attributes.get("groupBy")).booleanValue();

				// Flag it as restricted
				if (expr != null && !aliases.isEmpty() && tableKey != null
						&& colKey != null) {
					final ExpressionColumnDefinition expcol = new ExpressionColumnDefinition(
							expr, aliases, groupBy, colKey);
					final Map expMap = w.getDataSetModifications()
							.getExpressionColumns();
					if (!expMap.containsKey(tableKey))
						expMap.put(tableKey, new HashSet());
					((Collection) expMap.get(tableKey)).add(expcol);
				}
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
				// Look up the restriction.
				final Relation rel = (Relation) this.mappedObjects
						.get(attributes.get("relationId"));
				final String tableKey = (String) attributes.get("tableKey");
				final int index = Integer.parseInt((String) attributes
						.get("index"));

				// Get the aliases to use for the first table.
				final Map laliases = new HashMap();
				final String[] laliasColumnIds = ((String) attributes
						.get("leftAliasColumnIds")).split(",");
				final String[] laliasNames = ((String) attributes
						.get("leftAliasNames")).split(",");
				for (int i = 0; i < laliasColumnIds.length; i++) {
					final Column wrapped = (Column) this.mappedObjects
							.get(laliasColumnIds[i]);
					if (wrapped != null)
						laliases.put(wrapped, laliasNames[i]);
				}
				// and the second
				final Map raliases = new HashMap();
				final String[] raliasColumnIds = ((String) attributes
						.get("rightAliasColumnIds")).split(",");
				final String[] raliasNames = ((String) attributes
						.get("rightAliasNames")).split(",");
				for (int i = 0; i < raliasColumnIds.length; i++) {
					final Column wrapped = (Column) this.mappedObjects
							.get(raliasColumnIds[i]);
					if (wrapped != null)
						raliases.put(wrapped, raliasNames[i]);
				}
				// Get the expression to use.
				final String expr = (String) attributes.get("expression");

				// Flag it as restricted
				if (expr != null && rel != null && tableKey != null
						&& !laliases.isEmpty() && !raliases.isEmpty()) {
					final RestrictedRelationDefinition restrict = new RestrictedRelationDefinition(
							expr, laliases, raliases);
					final Map restMap = w.getSchemaModifications()
							.getRestrictedRelations();
					if (!restMap.containsKey(tableKey))
						restMap.put(tableKey, new HashMap());
					if (!((Map) restMap.get(tableKey)).containsKey(rel))
						((Map) restMap.get(tableKey)).put(rel, new HashMap());
					((Map) ((Map) restMap.get(tableKey)).get(rel)).put(
							new Integer(index), restrict);
				}
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
				this.constructedMart.addDataSet(ds);

				// Work out the optimiser.
				DataSetOptimiserType opt = null;
				if ("NONE".equals(optType))
					opt = DataSetOptimiserType.NONE;
				else if ("COLUMN".equals(optType))
					opt = DataSetOptimiserType.COLUMN;
				else if ("COLUMN_INHERIT".equals(optType))
					opt = DataSetOptimiserType.COLUMN_INHERIT;
				else if ("COLUMN_BOOL".equals(optType))
					opt = DataSetOptimiserType.COLUMN_BOOL;
				else if ("COLUMN_BOOL_INHERIT".equals(optType))
					opt = DataSetOptimiserType.COLUMN_BOOL_INHERIT;
				else if ("TABLE".equals(optType))
					opt = DataSetOptimiserType.TABLE;
				else if ("TABLE_INHERIT".equals(optType))
					opt = DataSetOptimiserType.TABLE_INHERIT;
				else if ("TABLE_BOOL".equals(optType))
					opt = DataSetOptimiserType.TABLE_BOOL;
				else if ("TABLE_BOOL_INHERIT".equals(optType))
					opt = DataSetOptimiserType.TABLE_BOOL_INHERIT;
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

		// Stick the element on the stack so that the next element
		// knows what it is inside.
		this.objectStack.push(element);
	}
}
