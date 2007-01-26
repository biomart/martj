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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.biomart.builder.controller.dialects.DatabaseDialect;
import org.biomart.builder.exceptions.ConstructorException;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.common.controller.JDBCSchema;
import org.biomart.common.model.Column;
import org.biomart.common.model.Schema;
import org.biomart.common.model.Table;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * This implementation of the {@link MartConstructor} interface generates DDL
 * statements corresponding to each {@link MartConstructorAction}.
 * <p>
 * The implementation depends on both the source and target databases being
 * {@link JDBCSchema} instances, and that they are compatible as defined by
 * {@link JDBCSchema#canCohabit(DataLink)}.
 * <p>
 * DDL statements are generated and output either to a text buffer, or to one or
 * more files.
 * <p>
 * The databases must be available and online for the class to do anything, as
 * it queries the database on a number of occasions to find out things such as
 * partition values.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class SaveDDLMartConstructor implements MartConstructor {

	private boolean includeComments;

	private File outputFile;

	private StringBuffer outputStringBuffer;

	/**
	 * Creates a constructor that, when requested, will begin constructing a
	 * mart and outputting DDL to a file.
	 * 
	 * @param outputFile
	 *            the file to write the DDL to. Single-file granularity will
	 *            write this file in plain text. Multi-file granularity will
	 *            write this file as a gzipped tar archive containing many plain
	 *            text files.
	 * @param includeComments
	 *            <tt>true</tt> if comments are to be included in the DDL
	 *            statements generated, <tt>false</tt> if not.
	 */
	public SaveDDLMartConstructor(final File outputFile,
			final boolean includeComments) {
		Log.info(Resources.get("logSaveDDLFile", outputFile.getPath()));
		// Remember the settings.
		this.outputFile = outputFile;
		this.includeComments = includeComments;
		// This last call is redundant but is included for clarity.
		this.outputStringBuffer = null;
	}

	/**
	 * Creates a constructor that, when requested, will begin constructing a
	 * mart and outputting DDL to a string buffer.
	 * 
	 * @param outputStringBuffer
	 *            the string buffer to write the DDL to. This parameter can only
	 *            be used if writing to a single file for all DDL. Any other
	 *            granularity will cause an exception.
	 * @param includeComments
	 *            <tt>true</tt> if comments are to be included in the DDL
	 *            statements generated, <tt>false</tt> if not.
	 */
	public SaveDDLMartConstructor(final StringBuffer outputStringBuffer,
			final boolean includeComments) {
		Log.info(Resources.get("logSaveDDLBuffer"));
		// Remember the settings.
		this.outputStringBuffer = outputStringBuffer;
		this.includeComments = includeComments;
		// This last call is redundant but is included for clarity.
		this.outputFile = null;
	}

	public ConstructorRunnable getConstructorRunnable(
			final String targetSchemaName, final Collection datasets)
			throws Exception {
		Log.debug("Working out what DDL helper to use");
		// Work out what kind of helper to use. The helper will
		// perform the actual conversion of action to DDL and divide
		// the results into appropriate files or buffers.
		final DDLHelper helper = this.outputStringBuffer == null ? (DDLHelper) new TableAsFileHelper(
				this.outputFile, this.includeComments)
				: new SingleStringBufferHelper(this.outputStringBuffer,
						this.includeComments);
		Log.debug("Chose helper " + helper.getClass().getName());
		// Check that all the input schemas involved are cohabitable.
		Log.info(Resources.get("logCheckDDLCohabit"));

		// First, make a set of all input schemas. Note that some
		// may be groups, but since canCohabit() works on groups
		// we don't need to worry about this. We use a set to prevent
		// duplicates.
		final Set inputSchemas = new HashSet();
		for (final Iterator i = datasets.iterator(); i.hasNext();)
			for (final Iterator j = ((DataSet) i.next()).getTables().iterator(); j
					.hasNext();) {
				final Table t = (Table) j.next();
				for (final Iterator k = t.getColumns().iterator(); k.hasNext();) {
					final Column c = (Column) k.next();
					if (c instanceof WrappedColumn)
						inputSchemas.add(((WrappedColumn) c).getWrappedColumn()
								.getTable().getSchema());
				}
			}

		// Convert the set to a list.
		final List inputSchemaList = new ArrayList(inputSchemas);

		// Set the output dialect to match the first one in the list.
		Log.debug("Getting dialect");
		final DatabaseDialect dd = DatabaseDialect
				.getDialect((Schema) inputSchemaList.get(0));
		if (dd == null)
			throw new ConstructorException("unknownDialect");
		else
			helper.setDialect(dd);

		// Then, check that the rest are compatible with the first one.
		for (int i = 1; i < inputSchemaList.size(); i++) {
			final Schema schema = (Schema) inputSchemaList.get(i);
			if (!schema.canCohabit((Schema) inputSchemaList.get(0)))
				throw new ConstructorException(Resources
						.get("saveDDLMixedDataLinks"));
		}

		// Construct and return the runnable that uses the helper
		// to do the actual work. Note how the helper is it's own
		// listener - it provides both database query facilities,
		// and converts action events back into DDL appropriate for
		// the database it is connected to.
		Log.debug("Building constructor runnable");
		final ConstructorRunnable cr = new GenericConstructorRunnable(
				targetSchemaName, datasets, helper);
		cr.addMartConstructorListener(helper);
		return cr;
	}

	/**
	 * An abstract class defining the way in which a {@link Helper} should
	 * behave when required not just to read from a database, but also to
	 * generate DDL statements appropriate for that database. This is done by
	 * having it implement {@link MartConstructorListener} so that it can
	 * intercept each action generated as it occurs.
	 * <p>
	 * Note that after construction,
	 * {@link DDLHelper#setDialect(DatabaseDialect)} must be called before
	 * construction begins, else the instance will not know what kind of
	 * database it is supposed to be using.
	 */
	public abstract static class DDLHelper implements Helper,
			MartConstructorListener {
		private DatabaseDialect dialect;

		private File file;

		private boolean includeComments;

		private int tempTableSeq = 0;

		/**
		 * Constructs a DDL helper.
		 * 
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		protected DDLHelper(final boolean includeComments) {
			this.includeComments = includeComments;
		}

		/**
		 * Constructs a DDL helper which writes to the given file.
		 * 
		 * @param file
		 *            the file to write to.
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		protected DDLHelper(final File file, final boolean includeComments) {
			this.file = file;
			this.includeComments = includeComments;
		}

		/**
		 * Translates an action into commands, using
		 * {@link DatabaseDialect#getStatementsForAction(MartConstructorAction, boolean)}
		 * 
		 * @param action
		 *            the action to translate.
		 * @return the translated action. Usually the array will contain only
		 *         one entry, but when including comments or in certain other
		 *         circumstances, the DDL for the action may consist of a number
		 *         of individual statements, in which case each statement will
		 *         occupy one entry in the array. The array will be ordered in
		 *         the order the statements should be executed.
		 * @throws ConstructorException
		 *             if anything went wrong.
		 */
		protected String[] getStatementsForAction(
				final MartConstructorAction action) throws ConstructorException {
			return this.dialect.getStatementsForAction(action,
					this.includeComments);
		}

		/**
		 * Retrieves the file we are writing to.
		 * 
		 * @return the file we are writing to.
		 */
		public File getFile() {
			return this.file;
		}

		public String getNewTempTableName() {
			return "TEMP__" + this.tempTableSeq++;
		}

		public Collection listDistinctValues(final Column col) throws SQLException {
			Log.info(Resources.get("logDistinct", "" + col));
			return this.dialect.executeSelectDistinct(col);
		}

		/**
		 * Sets the dialect to use to create the output DDL with. Also resets
		 * the dialect in order to remove any existing state information.
		 * 
		 * @param dialect
		 *            the dialect to use when creating output DDL.
		 */
		public void setDialect(final DatabaseDialect dialect) {
			this.dialect = dialect;
			this.dialect.reset();
		}
	}

	/**
	 * Statements are saved altogether inside a string buffer.
	 */
	public static class SingleStringBufferHelper extends DDLHelper {

		private StringBuffer outputStringBuffer;

		/**
		 * Constructs a helper which will output all DDL into a single string
		 * buffer.
		 * 
		 * @param outputStringBuffer
		 *            the string buffer to write the DDL into.
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		public SingleStringBufferHelper(final StringBuffer outputStringBuffer,
				final boolean includeComments) {
			super(includeComments);
			this.outputStringBuffer = outputStringBuffer;
		}

		public void martConstructorEventOccurred(final int event,
				final MartConstructorAction action) throws Exception {
			if (event == MartConstructorListener.ACTION_EVENT) {
				// Convert the action to some DDL.
				final String[] cmd = this.getStatementsForAction(action);
				// Write the data.
				for (int i = 0; i < cmd.length; i++) {
					this.outputStringBuffer.append(cmd[i]);
					this.outputStringBuffer.append(";\n");
				}
			}
		}
	}

	/**
	 * Statements are saved as a single SQL file per table inside a Zip file.
	 */
	public static class TableAsFileHelper extends DDLHelper {

		private Map actions;

		private int datasetSequence;

		private int martSequence;

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		/**
		 * Constructs a helper which will output all DDL into a single file per
		 * table inside the given zip file.
		 * 
		 * @param outputFile
		 *            the zip file to write the DDL into.
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		public TableAsFileHelper(final File outputFile,
				final boolean includeComments) {
			super(outputFile, includeComments);
			this.martSequence = 0;
			this.datasetSequence = 0;
			this.actions = new HashMap();
		}

		public void martConstructorEventOccurred(final int event,
				final MartConstructorAction action) throws Exception {
			if (event == MartConstructorListener.CONSTRUCTION_STARTED) {
				// Create and open the zip file.
				Log.debug("Starting zip file " + this.getFile().getPath());
				this.outputFileStream = new FileOutputStream(this.getFile());
				this.outputZipStream = new ZipOutputStream(
						this.outputFileStream);
				this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
			} else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
				// Close the zip stream. Will also close the
				// file output stream by default.
				Log.debug("Closing zip file");
				this.outputZipStream.finish();
				this.outputFileStream.flush();
				this.outputFileStream.close();
			} else if (event == MartConstructorListener.DATASET_STARTED) {
				// Clear out action map ready for next dataset.
				Log.debug("Dataset starting");
				this.actions.clear();
			} else if (event == MartConstructorListener.DATASET_ENDED) {
				// Write out one file per table in part1 files.
				Log.debug("Dataset ending");
				for (final Iterator i = this.actions.entrySet().iterator(); i
						.hasNext();) {
					final Map.Entry actionEntry = (Map.Entry) i.next();
					final String tableName = (String) actionEntry.getKey();
					final String entryFilename = this.martSequence + "/"
							+ this.datasetSequence + "/" + tableName
							+ Resources.get("perTablePart1FileName")
							+ Resources.get("ddlExtension");
					Log.debug("Starting entry " + entryFilename);
					ZipEntry entry = new ZipEntry(entryFilename);
					entry.setTime(System.currentTimeMillis());
					this.outputZipStream.putNextEntry(entry);
					// What actions are for this table?
					final List tableActions = (List) actionEntry.getValue();
					// Write the actions for the table itself.
					for (final Iterator j = tableActions.iterator(); j
							.hasNext();) {
						final MartConstructorAction nextAction = (MartConstructorAction) j
								.next();
						// Convert the action to some DDL.
						final String[] cmd = this
								.getStatementsForAction(nextAction);
						// Write the data.
						for (int k = 0; k < cmd.length; k++) {
							this.outputZipStream.write(cmd[k].getBytes());
							this.outputZipStream.write(';');
							this.outputZipStream.write(System.getProperty(
									"line.separator").getBytes());
						}
					}
					// Done with this entry.
					Log.debug("Closing entry");
					this.outputZipStream.closeEntry();
				}
				// Bump up the dataset count for the next one.
				this.datasetSequence++;
			} else if (event == MartConstructorListener.MART_ENDED)
				// Bump up the mart count for the next one.
				this.martSequence++;
			else if (event == MartConstructorListener.ACTION_EVENT) {
				// Add the action to the current map.
				final String dsTableName = action.getDataSetTableName();
				if (!this.actions.containsKey(dsTableName))
					this.actions.put(dsTableName,
							new ArrayList());
				((List) this.actions.get(dsTableName))
						.add(action);
			}
		}
	}
}
