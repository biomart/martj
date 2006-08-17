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
import java.util.Collections;
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
import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructorAction;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.MartConstructorAction.Drop;
import org.biomart.builder.model.MartConstructorAction.MartConstructorTableAction;
import org.biomart.builder.model.MartConstructorAction.OptimiseUpdateColumn;
import org.biomart.builder.resources.Resources;

/**
 * This implementation of the {@link MartConstructor} interface connects to a
 * JDBC data source in order to create a mart. It has options to output DDL to
 * file instead of running it, to run DDL directly against the database, or to
 * use JDBC to fetch/retrieve data between two databases.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.16, 4th August 2006
 * @since 0.1
 */
public class SaveDDLMartConstructor implements MartConstructor {

	private SaveDDLGranularity granularity;

	private File outputFile;

	private StringBuffer outputStringBuffer;

	private boolean includeComments;

	/**
	 * Creates a constructor that, when requested, will begin constructing a
	 * mart and outputting DDL.
	 * 
	 * @param granularity
	 *            the granularity to which the DDL will be broken down.
	 * @param outputFile
	 *            the file to write the DDL to. If null, then the
	 *            <tt>outputStringBuffer</tt> parameter must not be null.
	 * @param outputStringBuffer
	 *            the string buffer to write the DDL to. If null, then the
	 *            <tt>outputFile</tt> parameter must not be null. This
	 *            parameter can only be used if writing to a single file for all
	 *            DDL. Any other granularity will cause an exception.
	 * @param includeComments
	 *            <tt>true</tt> if comments are to be included, <tt>false</tt>
	 *            if not.
	 * @throws IllegalArgumentException
	 *             if the combination of <tt>granularity</tt>,
	 *             <tt>outputFile</tt> and <tt>outputStringBuffer</tt> do
	 *             not make sense.
	 */
	public SaveDDLMartConstructor(final SaveDDLGranularity granularity,
			final File outputFile, final StringBuffer outputStringBuffer,
			final boolean includeComments) throws IllegalArgumentException {
		// Check it's sensible.
		if (outputStringBuffer != null
				&& !granularity.equals(SaveDDLGranularity.SINGLE))
			throw new IllegalArgumentException(Resources
					.get("mcDDLStringBufferSingleFileOnly"));
		else if (outputStringBuffer == null && outputFile == null)
			throw new IllegalArgumentException(Resources
					.get("mcDDLNoOutputSpecified"));
		else if (outputStringBuffer != null && outputFile != null)
			throw new IllegalArgumentException(Resources
					.get("mcDDLBothOutputsSpecified"));

		// Remember the settings.
		this.granularity = granularity;
		this.outputFile = outputFile;
		this.outputStringBuffer = outputStringBuffer;
		this.includeComments = includeComments;
	}

	public ConstructorRunnable getConstructorRunnable(
			final String targetSchemaName, final Collection datasets)
			throws Exception {
		// Work out what kind of helper to use.
		DDLHelper helper;
		if (this.granularity.equals(SaveDDLGranularity.SINGLE)) {
			if (this.outputFile != null)
				helper = new SingleFileHelper(this.outputFile,
						this.includeComments);
			else
				helper = new SingleStringBufferHelper(this.outputStringBuffer,
						this.includeComments);
		} else if (this.granularity.equals(SaveDDLGranularity.MART))
			helper = new MartAsFileHelper(this.outputFile, this.includeComments);
		else if (this.granularity.equals(SaveDDLGranularity.DATASET))
			helper = new DataSetAsFileHelper(this.outputFile,
					this.includeComments);
		else if (this.granularity.equals(SaveDDLGranularity.TABLE))
			helper = new TableAsFileHelper(this.outputFile,
					this.includeComments);
		else
			helper = new StepAsFileHelper(this.outputFile, this.includeComments);

		// Check that all the input schemas have are cohabitable.
		// First, make a set of all input schemas. Note that some
		// may be groups, but since canCohabit() works on groups
		// we don't need to worry about this.
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

		// Construct and return the runnable that uses the helper.
		final ConstructorRunnable cr = new GenericConstructorRunnable(
				targetSchemaName, datasets, helper);
		cr.addMartConstructorListener(helper);
		return cr;
	}

	/**
	 * DDLHelper generates DDL statements for each step.
	 */
	public abstract static class DDLHelper implements Helper,
			MartConstructorListener {
		private DatabaseDialect dialect;

		private int tempTableSeq = 0;

		private File file;

		private boolean includeComments;

		/**
		 * Constructs a DDL helper.
		 * 
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		public DDLHelper(final boolean includeComments) {
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
		public DDLHelper(final File file, final boolean includeComments) {
			this.file = file;
			this.includeComments = includeComments;
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

		public String getNewTempTableName() {
			return "TEMP__" + this.tempTableSeq++;
		}

		public List listDistinctValues(final Column col) throws SQLException {
			return this.dialect.executeSelectDistinct(col);
		}

		/**
		 * Translates an action into commands, using
		 * {@link DatabaseDialect#getStatementsForAction(MartConstructorAction, boolean)}
		 * 
		 * @param action
		 *            the action to translate.
		 * @return the translated action.
		 * @throws Exception
		 *             if anything went wrong.
		 */
		protected String[] getStatementsForAction(
				final MartConstructorAction action) throws Exception {
			return this.dialect.getStatementsForAction(action,
					this.includeComments);
		}
	}

	/**
	 * SingleFileHelper extends DDLHelper, saves statements. Statements are
	 * saved as a single SQL file inside a Zip file.
	 */
	public static class SingleFileHelper extends DDLHelper {

		private FileOutputStream outputFileStream;

		/**
		 * Constructs a helper which will output all DDL into a single file
		 * inside the given zip file.
		 * 
		 * @param outputFile
		 *            the zip file to write the DDL into.
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		public SingleFileHelper(final File outputFile,
				final boolean includeComments) {
			super(outputFile, includeComments);
		}

		public void martConstructorEventOccurred(final int event,
				final MartConstructorAction action) throws Exception {
			if (event == MartConstructorListener.CONSTRUCTION_STARTED)
				this.outputFileStream = new FileOutputStream(this.getFile());
			else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
				this.outputFileStream.flush();
				this.outputFileStream.close();
			} else if (event == MartConstructorListener.ACTION_EVENT) {
				// Convert the action to some DDL.
				final String[] cmd = this.getStatementsForAction(action);
				// Write the data.
				for (int i = 0; i < cmd.length; i++) {
					this.outputFileStream.write(cmd[i].getBytes());
					this.outputFileStream.write(';');
					this.outputFileStream.write(System.getProperty(
							"line.separator").getBytes());
				}
			}
		}
	}

	/**
	 * SingleStringBufferHelper extends DDLHelper, saves statements. Statements
	 * are saved altogether inside a string buffer.
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
	 * MartAsFileHelper extends DDLHelper, saves statements. Statements are
	 * saved as a single SQL file per mart inside a Zip file.
	 */
	public static class MartAsFileHelper extends DDLHelper {

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		private ZipEntry entry;

		private int martSequence;

		/**
		 * Constructs a helper which will output all DDL into a single file per
		 * mart inside the given zip file.
		 * 
		 * @param outputFile
		 *            the zip file to write the DDL into.
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		public MartAsFileHelper(final File outputFile,
				final boolean includeComments) {
			super(outputFile, includeComments);
			this.martSequence = 0;
		}

		public void martConstructorEventOccurred(final int event,
				final MartConstructorAction action) throws Exception {
			if (event == MartConstructorListener.CONSTRUCTION_STARTED) {
				this.outputFileStream = new FileOutputStream(this.getFile());
				this.outputZipStream = new ZipOutputStream(
						this.outputFileStream);
				this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
			} else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
				// Close the zip stream. Will also close the
				// file output stream by default.
				this.outputZipStream.finish();
				this.outputFileStream.flush();
				this.outputFileStream.close();
			} else if (event == MartConstructorListener.MART_STARTED) {
				this.entry = new ZipEntry(this.martSequence + ".sql");
				this.entry.setTime(System.currentTimeMillis());
				this.outputZipStream.putNextEntry(this.entry);
			} else if (event == MartConstructorListener.MART_ENDED) {
				this.outputZipStream.closeEntry();
				// Bump up the mart sequence.
				this.martSequence++;
			} else if (event == MartConstructorListener.ACTION_EVENT) {
				// Convert the action to some DDL.
				final String[] cmd = this.getStatementsForAction(action);
				// Write the data.
				for (int i = 0; i < cmd.length; i++) {
					this.outputZipStream.write(cmd[i].getBytes());
					this.outputZipStream.write(';');
					this.outputZipStream.write(System.getProperty(
							"line.separator").getBytes());
				}
			}
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

		private int martSequence;

		private int datasetSequence;

		/**
		 * Constructs a helper which will output all DDL into a single file per
		 * dataset inside the given zip file.
		 * 
		 * @param outputFile
		 *            the zip file to write the DDL into.
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		public DataSetAsFileHelper(final File outputFile,
				final boolean includeComments) {
			super(outputFile, includeComments);
			this.martSequence = 0;
			this.datasetSequence = 0;
		}

		public void martConstructorEventOccurred(final int event,
				final MartConstructorAction action) throws Exception {
			if (event == MartConstructorListener.CONSTRUCTION_STARTED) {
				this.outputFileStream = new FileOutputStream(this.getFile());
				this.outputZipStream = new ZipOutputStream(
						this.outputFileStream);
				this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
			} else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
				// Close the zip stream. Will also close the
				// file output stream by default.
				this.outputZipStream.finish();
				this.outputFileStream.flush();
				this.outputFileStream.close();
			} else if (event == MartConstructorListener.DATASET_STARTED) {
				this.entry = new ZipEntry(this.martSequence + "/"
						+ this.datasetSequence + ".sql");
				this.entry.setTime(System.currentTimeMillis());
				this.outputZipStream.putNextEntry(this.entry);
			} else if (event == MartConstructorListener.DATASET_ENDED) {
				this.outputZipStream.closeEntry();
				// Bump up the dataset count for the next one.
				this.datasetSequence++;
			} else if (event == MartConstructorListener.MART_ENDED)
				// Bump up the mart count for the next one.
				this.martSequence++;
			else if (event == MartConstructorListener.ACTION_EVENT) {
				// Convert the action to some DDL.
				final String[] cmd = this.getStatementsForAction(action);
				// Write the data.
				for (int i = 0; i < cmd.length; i++) {
					this.outputZipStream.write(cmd[i].getBytes());
					this.outputZipStream.write(';');
					this.outputZipStream.write(System.getProperty(
							"line.separator").getBytes());
				}
			}
		}
	}

	/**
	 * TableAsFileHelper extends DDLHelper, saves statements. Statements are
	 * saved as a single SQL file per table inside a Zip file.
	 */
	public static class TableAsFileHelper extends DDLHelper {

		private FileOutputStream outputFileStream;

		private ZipOutputStream outputZipStream;

		private int martSequence;

		private int datasetSequence;

		private Map actions;

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
				this.outputFileStream = new FileOutputStream(this.getFile());
				this.outputZipStream = new ZipOutputStream(
						this.outputFileStream);
				this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
			} else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
				// Close the zip stream. Will also close the
				// file output stream by default.
				this.outputZipStream.finish();
				this.outputFileStream.flush();
				this.outputFileStream.close();
			} else if (event == MartConstructorListener.DATASET_STARTED)
				// Clear out action map ready for next dataset.
				this.actions.clear();
			else if (event == MartConstructorListener.DATASET_ENDED) {
				ZipEntry entry;
				// Make a list for optimise-update actions.
				final List optimiseUpdate = new ArrayList();
				// Write out one file per table in action map.
				for (final Iterator i = this.actions.entrySet().iterator(); i
						.hasNext();) {
					final Map.Entry actionEntry = (Map.Entry) i.next();
					final String tableName = (String) actionEntry.getKey();
					entry = new ZipEntry(this.martSequence + "/"
							+ this.datasetSequence + "/" + tableName + ".sql");
					entry.setTime(System.currentTimeMillis());
					this.outputZipStream.putNextEntry(entry);
					// What actions are for this table?
					final List tableActions = (List) actionEntry.getValue();
					// What is the first table action?
					MartConstructorTableAction firstAction = null;
					for (final Iterator j = tableActions.iterator(); j
							.hasNext()
							&& firstAction == null;) {
						final MartConstructorAction candidate = (MartConstructorAction) j
								.next();
						if (candidate instanceof MartConstructorTableAction)
							firstAction = (MartConstructorTableAction) candidate;
					}

					// Work out the parent actions necessary to provide
					// the temp table that the create action selects from.
					final List dependentActions = new ArrayList();
					if (firstAction != null
							&& firstAction.getParents().size() > 0) {
						dependentActions.addAll(firstAction.getParents());
						for (int j = 0; j < dependentActions.size(); j++)
							dependentActions
									.addAll(((MartConstructorAction) dependentActions
											.get(j)).getParents());
						Collections.reverse(dependentActions);
						// Insert the dependent actions before the table
						// actions.
						tableActions.addAll(0, dependentActions);
					}
					// Write the actions for the table itself.
					for (final Iterator j = tableActions.iterator(); j
							.hasNext();) {
						final MartConstructorAction nextAction = (MartConstructorAction) j
								.next();
						// Is it a optimise-update action? Save it for later and
						// don't do DDL.
						if (nextAction instanceof OptimiseUpdateColumn) {
							optimiseUpdate.add(nextAction);
							continue;
						}
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
					// Drop any inherited actions by making a list
					// of the temp table names they created and dropping
					// all those.
					if (dependentActions.size() > 0) {
						final Set tempTableNames = new HashSet();
						for (final Iterator j = dependentActions.iterator(); j
								.hasNext();) {
							MartConstructorAction candidate = (MartConstructorAction) j
									.next();
							if (candidate instanceof MartConstructorTableAction)
								tempTableNames
										.add(((MartConstructorTableAction) candidate)
												.getTargetTableName());
						}
						for (final Iterator j = tempTableNames.iterator(); j
								.hasNext();) {
							// Create and write a drop action.
							final Drop drop = new Drop(firstAction
									.getDataSetSchemaName(), firstAction
									.getDataSetTableName(), firstAction
									.getTargetTableSchema(), (String) j.next());
							// Convert the action to some DDL.
							final String[] cmd = this
									.getStatementsForAction(drop);
							// Write the data.
							for (int k = 0; k < cmd.length; k++) {
								this.outputZipStream.write(cmd[k].getBytes());
								this.outputZipStream.write(';');
								this.outputZipStream.write(System.getProperty(
										"line.separator").getBytes());
							}
						}
					}
					// Done with this entry.
					this.outputZipStream.closeEntry();
				}
				// Write out the optimise-update actions in their own file.
				if (optimiseUpdate.size() > 0) {
					entry = new ZipEntry(this.martSequence + "/"
							+ this.datasetSequence + "/_update_has_columns.sql");
					entry.setTime(System.currentTimeMillis());
					this.outputZipStream.putNextEntry(entry);
					for (final Iterator j = optimiseUpdate.iterator(); j
							.hasNext();) {
						// Convert the action to some DDL.
						final String[] cmd = this
								.getStatementsForAction((MartConstructorAction) j
										.next());
						// Write the data.
						for (int k = 0; k < cmd.length; k++) {
							this.outputZipStream.write(cmd[k].getBytes());
							this.outputZipStream.write(';');
							this.outputZipStream.write(System.getProperty(
									"line.separator").getBytes());
						}
					}
					this.outputZipStream.closeEntry();
				}
				// Bump up the dataset count for the next one.
				this.datasetSequence++;
			} else if (event == MartConstructorListener.MART_ENDED)
				// Bump up the mart count for the next one.
				this.martSequence++;
			else if (event == MartConstructorListener.ACTION_EVENT) {
				// Add the action to the current map.
				if (!this.actions.containsKey(action.getDataSetTableName()))
					this.actions.put(action.getDataSetTableName(),
							new ArrayList());
				((ArrayList) this.actions.get(action.getDataSetTableName()))
						.add(action);
			}
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
		 * @param outputFile
		 *            the zip file to write the DDL structured tree into.
		 * @param includeComments
		 *            <tt>true</tt> if comments are to be included,
		 *            <tt>false</tt> if not.
		 */
		public StepAsFileHelper(final File outputFile,
				final boolean includeComments) {
			super(outputFile, includeComments);
		}

		public void martConstructorEventOccurred(final int event,
				final MartConstructorAction action) throws Exception {
			if (event == MartConstructorListener.CONSTRUCTION_STARTED) {
				this.outputFileStream = new FileOutputStream(this.getFile());
				this.outputZipStream = new ZipOutputStream(
						this.outputFileStream);
				this.outputZipStream.setMethod(ZipOutputStream.DEFLATED);
			} else if (event == MartConstructorListener.CONSTRUCTION_ENDED) {
				// Close the zip stream. Will also close the
				// file output stream by default.
				this.outputZipStream.finish();
				this.outputFileStream.flush();
				this.outputFileStream.close();
			} else if (event == MartConstructorListener.ACTION_EVENT)
				try {
					// What level is this action? (ie. depth in graph)
					final int level = action.getDepth();
					// Writes the given action to file.
					// Put the next entry to the zip file.
					final ZipEntry entry = new ZipEntry(level + "/" + level
							+ "-" + action.getSequence() + ".sql");
					entry.setTime(System.currentTimeMillis());
					this.outputZipStream.putNextEntry(entry);
					// Convert the action to some DDL.
					final String[] cmd = this.getStatementsForAction(action);
					// Write the data.
					for (int i = 0; i < cmd.length; i++) {
						this.outputZipStream.write(cmd[i].getBytes());
						this.outputZipStream.write(';');
						this.outputZipStream.write(System.getProperty(
								"line.separator").getBytes());
					}
					// Close the entry.
					this.outputZipStream.closeEntry();
				} catch (final Exception e) {
					// Make sure we don't leave open entries lying around
					// if exceptions get thrown.
					this.outputZipStream.closeEntry();
					throw e;
				}
		}
	}

	/**
	 * Represents the name of various methods of constructing a DDL zip file.
	 */
	public static class SaveDDLGranularity implements Comparable {
		private static final Map singletons = new HashMap();

		private final String name;

		private final boolean zipped;

		/**
		 * Use this constant to refer to single-file for all output.
		 */
		public static final SaveDDLGranularity SINGLE = SaveDDLGranularity.get(
				Resources.get("saveDDLSingleGranularity"), false);

		/**
		 * Use this constant to refer to file-per-mart output.
		 */
		public static final SaveDDLGranularity MART = SaveDDLGranularity.get(
				Resources.get("saveDDLMartGranularity"), true);

		/**
		 * Use this constant to refer to file-per-dataset output.
		 */
		public static final SaveDDLGranularity DATASET = SaveDDLGranularity
				.get(Resources.get("saveDDLDataSetGranularity"), true);

		/**
		 * Use this constant to refer to file-per-tableoutput.
		 */
		public static final SaveDDLGranularity TABLE = SaveDDLGranularity.get(
				Resources.get("saveDDLTableGranularity"), true);

		/**
		 * Use this constant to refer to file-per-step output.
		 */
		public static final SaveDDLGranularity STEP = SaveDDLGranularity.get(
				Resources.get("saveDDLStepGranularity"), true);

		/**
		 * The static factory method creates and returns a type with the given
		 * name. It ensures the object returned is a singleton. Note that the
		 * names of type objects are case-sensitive.
		 * 
		 * @param name
		 *            the name of the type object.
		 * @param zipped
		 *            <tt>true</tt> if this type outputs zip files,
		 *            <tt>false</tt> if not.
		 * @return the type object.
		 */
		public static SaveDDLGranularity get(final String name,
				final boolean zipped) {
			// Do we already have this one?
			// If so, then return it.
			if (SaveDDLGranularity.singletons.containsKey(name))
				return (SaveDDLGranularity) SaveDDLGranularity.singletons
						.get(name);

			// Otherwise, create it, remember it.
			final SaveDDLGranularity t = new SaveDDLGranularity(name, zipped);
			SaveDDLGranularity.singletons.put(name, t);

			// Return it.
			return t;
		}

		/**
		 * The private constructor defines the name this object will display
		 * when printed.
		 * 
		 * @param name
		 *            the name of the mart constructor type.
		 * @param zipped
		 *            <tt>true</tt> if this type outputs zip files,
		 *            <tt>false</tt> if not.
		 */
		private SaveDDLGranularity(final String name, final boolean zipped) {
			this.name = name;
			this.zipped = zipped;
		}

		/**
		 * Returns <tt>true</tt> if this type outputs zip files,
		 * <tt>false</tt> otherwise.
		 * 
		 * @return <tt>true</tt> if output is zipped, <tt>false</tt>
		 *         otherwise.
		 */
		public boolean getZipped() {
			return this.zipped;
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

		public int compareTo(final Object o) throws ClassCastException {
			final SaveDDLGranularity t = (SaveDDLGranularity) o;
			return this.toString().compareTo(t.toString());
		}

		public boolean equals(final Object o) {
			// We are dealing with singletons so can use == happily.
			return o == this;
		}
	}
}
