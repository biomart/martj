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
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.resources.BuilderBundle;

/**
 * This implementation of the {@link MartConstructor} interface connects to a
 * JDBC data source in order to create a mart. It has options to output DDL to
 * file instead of running it, to run DDL directly against the database, or to
 * use JDBC to fetch/retrieve data between two databases.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.5, 20th June 2006
 * @since 0.1
 */
public class ZippedDDLMartConstructor implements MartConstructor {

	private ZippedDDLGranularity granularity;

	private File outputDDLZipFile;

	/**
	 * Creates a constructor that, when requested, will begin constructing a
	 * mart and outputting zipped DDL.
	 * 
	 * @param targetDBName
	 *            the name of the target schema the DDL will be run against.
	 * @param granularity
	 *            the granularity to which the DDL will be broken down.
	 * @param datasets
	 *            the datasets to output.
	 * @param outputDDLZipFile
	 *            the file to write the DDL to.
	 */
	public ZippedDDLMartConstructor(ZippedDDLGranularity granularity,
			File outputDDLZipFile) {
		// Remember the settings.
		this.granularity = granularity;
		this.outputDDLZipFile = outputDDLZipFile;

		// Register dialects.
		DatabaseDialect.registerDialects();
	}

	public ConstructorRunnable getConstructorRunnable(String targetSchemaName,
			Collection datasets) throws Exception {
		// Work out what kind of helper to use.
		DDLHelper helper;
		if (this.granularity.equals(ZippedDDLGranularity.MART))
			helper = new MartAsFileHelper(this.outputDDLZipFile);
		else if (this.granularity.equals(ZippedDDLGranularity.DATASET))
			helper = new DataSetAsFileHelper(this.outputDDLZipFile);
		else
			helper = new StepAsFileHelper(this.outputDDLZipFile);

		// Set the input and output dialects on the helper for each
		// schema.

		// Inputs first.
		Set inputSchemas = new HashSet();
		for (Iterator i = datasets.iterator(); i.hasNext();) {
			for (Iterator j = ((DataSet) i.next()).getTables().iterator(); j
					.hasNext();) {
				Table t = (Table) j.next();
				for (Iterator k = t.getColumns().iterator(); k.hasNext();) {
					Column c = (Column) k.next();
					if (c instanceof WrappedColumn)
						inputSchemas.add(((WrappedColumn) c).getWrappedColumn()
								.getTable().getSchema());
				}
			}
		}
		for (Iterator i = inputSchemas.iterator(); i.hasNext();) {
			Schema s = (Schema) i.next();
			helper.setInputDialect(s, DatabaseDialect.getDialect(s));
		}

		// Then the output - this is the same as the input, or has to be if
		// we are to generate useful DDL, so we can just use the first input
		// schema for this purpose.
		helper.setOutputDialect(DatabaseDialect
				.getDialect((Schema) inputSchemas.iterator().next()));

		// Construct and return the runnable that uses the helper.
		return new GenericConstructorRunnable(targetSchemaName, datasets,
				helper);
	}

	/**
	 * DDLHelper generates DDL statements for each step.
	 */
	public abstract static class DDLHelper implements Helper {
		private DatabaseDialect dialect;

		private int tempTableSeq = 0;

		private File file;

		/**
		 * Constructs a DDL helper which writes to the given file.
		 * 
		 * @param file
		 *            the file to write to.
		 */
		public DDLHelper(File file) {
			this.file = file;
		}

		/**
		 * Retrieves the file we are writing to.
		 * 
		 * @return the file we are writing to.
		 */
		public File getFile() {
			return this.file;
		}

		/**
		 * Sets the dialect to use on tables which come from the given schema.
		 * 
		 * @param schema
		 *            the schema to use the dialect on.
		 * @param dialect
		 *            the dialect to use for tables in that schema.
		 */
		public void setInputDialect(Schema schema, DatabaseDialect dialect) {
			// Ignored as is not required - the input dialect is the
			// same as the output dialect if we are to run DDL statements.
		}

		/**
		 * Sets the dialect to use to create the output tables with.
		 * 
		 * @param dialect
		 *            the dialect to use when creating output tables.
		 */
		public void setOutputDialect(DatabaseDialect dialect) {
			this.dialect = dialect;
		}

		public String getNewTempTableName() {
			return "__JDBCMART_TEMP__" + this.tempTableSeq++;
		}

		public List listDistinctValues(Column col) throws SQLException {
			return this.dialect.executeSelectDistinct(col);
		}

		/**
		 * Translates an action into commands, using
		 * {@link DatabaseDialect#getStatementsForAction(MCAction)}
		 * 
		 * @param action
		 *            the action to translate.
		 * @return the translated action.
		 * @throws Exception
		 *             if anything went wrong.
		 */
		public String[] getStatementsForAction(MCAction action)
				throws Exception {
			return dialect.getStatementsForAction(action);
		}
	}

	/**
	 * MartAsFileHelper extends DDLHelper, saves statements. Statements are
	 * saved as a single SQL file inside a Zip file.
	 */
	public static class MartAsFileHelper extends DDLHelper {

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		private ZipEntry entry;

		/**
		 * Constructs a helper which will output all DDL into a single file
		 * inside the given zip file.
		 * 
		 * @param outputZippedDDLFile
		 *            the zip file to write the DDL into.
		 */
		public MartAsFileHelper(File outputZippedDDLFile) {
			super(outputZippedDDLFile);
		}

		public void startActionsForMart() throws Exception {
			// Open the zip stream.
			this.outputFileStream = new FileOutputStream(this.getFile());
			this.outputZipStream = new ZipOutputStream(this.outputFileStream);
			this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
			this.entry = new ZipEntry("ddl.sql");
			entry.setTime(System.currentTimeMillis());
			this.outputZipStream.putNextEntry(entry);
		}

		public void startActionsForDataSet() throws Exception {
			// We don't care.
		}

		public void executeAction(MCAction action, int level) throws Exception {
			// Convert the action to some DDL.
			String[] cmd = this.getStatementsForAction(action);
			// Write the data.
			for (int i = 0; i < cmd.length; i++) {
				this.outputZipStream.write(cmd[i].getBytes());
				this.outputZipStream.write(';');
				this.outputZipStream.write(System.getProperty("line.separator")
						.getBytes());
			}
		}

		public void endActionsForDataSet() throws Exception {
			// We don't care.
		}

		public void endActionsForMart() throws Exception {
			// Close the zip stream. Will also close the
			// file output stream by default.
			this.outputZipStream.closeEntry();
			this.outputZipStream.finish();
			this.outputFileStream.flush();
			this.outputFileStream.close();
		}
	}

	/**
	 * DataSetAsFileHelper extends DDLHelper, saves statements. Statements are
	 * saved as a single SQL file per dataset inside a Zip file.
	 */
	public static class DataSetAsFileHelper extends DDLHelper {

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		private ZipEntry entry;

		private int sequence;

		/**
		 * Constructs a helper which will output all DDL into a single file per
		 * dataset inside the given zip file.
		 * 
		 * @param outputZippedDDLFile
		 *            the zip file to write the DDL into.
		 */
		public DataSetAsFileHelper(File outputZippedDDLFile) {
			super(outputZippedDDLFile);
			this.sequence = 0;
		}

		public void startActionsForMart() throws Exception {
			// Open the zip stream.
			this.outputFileStream = new FileOutputStream(this.getFile());
			this.outputZipStream = new ZipOutputStream(this.outputFileStream);
			this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
		}

		public void startActionsForDataSet() throws Exception {
			this.entry = new ZipEntry(this.sequence++ + ".sql");
			entry.setTime(System.currentTimeMillis());
			this.outputZipStream.putNextEntry(entry);
		}

		public void executeAction(MCAction action, int level) throws Exception {
			// Convert the action to some DDL.
			String[] cmd = this.getStatementsForAction(action);
			// Write the data.
			for (int i = 0; i < cmd.length; i++) {
				this.outputZipStream.write(cmd[i].getBytes());
				this.outputZipStream.write(';');
				this.outputZipStream.write(System.getProperty("line.separator")
						.getBytes());
			}
		}

		public void endActionsForDataSet() throws Exception {
			this.outputZipStream.closeEntry();
		}

		public void endActionsForMart() throws Exception {
			// Close the zip stream. Will also close the
			// file output stream by default.
			this.outputZipStream.finish();
			this.outputFileStream.flush();
			this.outputFileStream.close();
		}
	}

	/**
	 * StepAsFileHelper extends DDLHelper, saves statements. Statements are
	 * saved in folders. Folder 1 must be finished before folder 2, but files
	 * within folder 1 can be executed in any order. And so on.
	 */
	public static class StepAsFileHelper extends DDLHelper {

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		/**
		 * Constructs a helper which will output all DDL into a structured
		 * directory tree inside the given zip file.
		 * 
		 * @param outputZippedDDLFile
		 *            the zip file to write the DDL structured tree into.
		 */
		public StepAsFileHelper(File outputZippedDDLFile) {
			super(outputZippedDDLFile);
		}

		public void startActionsForMart() throws Exception {
			// Open the zip stream.
			this.outputFileStream = new FileOutputStream(this.getFile());
			this.outputZipStream = new ZipOutputStream(this.outputFileStream);
			this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
		}

		public void startActionsForDataSet() throws Exception {
			// We don't care.
		}

		public void executeAction(MCAction action, int level) throws Exception {
			try {
				// Writes the given action to file.
				// Put the next entry to the zip file.
				ZipEntry entry = new ZipEntry(level + "/" + level + "-"
						+ action.getSequence() + ".sql");
				entry.setTime(System.currentTimeMillis());
				this.outputZipStream.putNextEntry(entry);
				// Convert the action to some DDL.
				String[] cmd = this.getStatementsForAction(action);
				// Write the data.
				for (int i = 0; i < cmd.length; i++) {
					this.outputZipStream.write(cmd[i].getBytes());
					this.outputZipStream.write(';');
					this.outputZipStream.write(System.getProperty(
							"line.separator").getBytes());
				}
				// Close the entry.
				this.outputZipStream.closeEntry();
			} catch (Exception e) {
				// Make sure we don't leave open entries lying around
				// if exceptions get thrown.
				this.outputZipStream.closeEntry();
				throw e;
			}
		}

		public void endActionsForDataSet() throws Exception {
			// We don't care.
		}

		public void endActionsForMart() throws Exception {
			// Close the zip stream. Will also close the
			// file output stream by default.
			this.outputZipStream.finish();
			this.outputFileStream.flush();
			this.outputFileStream.close();
		}
	}

	/**
	 * Represents the name of various methods of constructing a DDL zip file.
	 */
	public static class ZippedDDLGranularity implements Comparable {
		private static final Map singletons = new HashMap();

		private final String name;

		/**
		 * Use this constant to refer to in-database DDL execution.
		 */
		public static final ZippedDDLGranularity MART = ZippedDDLGranularity
				.get(BuilderBundle.getString("zippedDDLMartGranularity"));

		/**
		 * Use this constant to refer to creation via JDBC import/export.
		 */
		public static final ZippedDDLGranularity DATASET = ZippedDDLGranularity
				.get(BuilderBundle.getString("zippedDDLDataSetGranularity"));

		/**
		 * Use this constant to refer to generation of DDL in a file.
		 */
		public static final ZippedDDLGranularity STEP = ZippedDDLGranularity
				.get(BuilderBundle.getString("zippedDDLStepGranularity"));

		/**
		 * The static factory method creates and returns a type with the given
		 * name. It ensures the object returned is a singleton. Note that the
		 * names of type objects are case-insensitive.
		 * 
		 * @param name
		 *            the name of the type object.
		 * @return the type object.
		 */
		public static ZippedDDLGranularity get(String name) {
			// Convert to upper case.
			name = name.toUpperCase();

			// Do we already have this one?
			// If so, then return it.
			if (singletons.containsKey(name))
				return (ZippedDDLGranularity) singletons.get(name);

			// Otherwise, create it, remember it.
			ZippedDDLGranularity t = new ZippedDDLGranularity(name);
			singletons.put(name, t);

			// Return it.
			return t;
		}

		/**
		 * The private constructor defines the name this object will display
		 * when printed.
		 * 
		 * @param name
		 *            the name of the mart constructor type.
		 */
		private ZippedDDLGranularity(String name) {
			this.name = name;
		}

		/**
		 * Displays the name of this constructor type object.
		 * 
		 * @return the name of this constructor type object.
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
			ZippedDDLGranularity t = (ZippedDDLGranularity) o;
			return this.toString().compareTo(t.toString());
		}

		public boolean equals(Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}
}
