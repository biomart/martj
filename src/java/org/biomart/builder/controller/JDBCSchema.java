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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.exceptions.MartBuilderInternalError;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Schema.GenericSchema;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.resources.BuilderBundle;

/**
 * <p>
 * This implementation of the {@link Schema} interface connects to a JDBC data
 * source and loads tables, keys and relations using database metadata.
 * <p>
 * If key-guessing is enabled, foreign keys are guessed instead of being read
 * from the database. Guessing works by iterating through known primary keys,
 * where the first column of the key matches the name of the table (optionally
 * with '_id' appended), then iterating through all other tables looking for
 * sets of columns with identical names, or names that have had '_key' appended.
 * If it finds a matching set, then it assumes that it has found a foreign key,
 * and establishes a relation between the two.
 * <p>
 * When using keyguessing, primary keys are read from database metadata, but if
 * this method returns no results, then each table is searched for a column with
 * the same name as the table, optionally with '_id' appended. If one is found,
 * then it is assumed that that column is the primary key for the table.
 * <p>
 * This implementation is very careful not to override any hand-made relations
 * or keys, or to reinstate any that have previously been marked as incorrect.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 2nd June 2006
 * @since 0.1
 */
public class JDBCSchema extends GenericSchema implements JDBCDataLink {
	private Connection connection;

	private String driverClassName;

	private File driverClassLocation;

	private String url;

	private String username;

	private String password;

	/**
	 * <p>
	 * Establishes a JDBC connection from the information provided, and
	 * remembers it. Nothing is read yet - if you want to read the schema data,
	 * you must use the {@link #synchronise()} method to do so.
	 * 
	 * @param driverClassLocation
	 *            the location (filesystem path) of the class to load the JDBC
	 *            driver from. This will usually be a <tt>.jar</tt> file, or a
	 *            folder containing a Java <tt>.class</tt> file. If the path
	 *            does not exist, is null, or does not contain the class
	 *            specified, then the default system class loader is used
	 *            instead.
	 * @param driverClassName
	 *            the class name of the JDBC driver, eg.
	 *            <tt>com.mysql.jdbc.Driver</tt>.
	 * @param url
	 *            the JDBC URL of the database server to connect to.
	 * @param username
	 *            the username to connect as.
	 * @param password
	 *            the password to connect as. Defaults to the empty string if
	 *            null.
	 * @param name
	 *            the name to give this schema after it has been created.
	 */
	public JDBCSchema(File driverClassLocation, String driverClassName,
			String url, String username, String password, String name,
			boolean keyGuessing) {
		// Call the GenericSchema implementation first, to set up our name, and
		// set up keyguessing.
		super(name, keyGuessing);

		// Sensible defaults.
		if (driverClassLocation != null && !driverClassLocation.exists())
			driverClassLocation = null;

		// Remember the settings.
		this.driverClassLocation = driverClassLocation;
		this.driverClassName = driverClassName;
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public Schema replicate(String newName) {
		// Make an empty copy.
		Schema newSchema = new JDBCSchema(this.driverClassLocation,
				this.driverClassName, this.url, this.username, this.password,
				newName, this.getKeyGuessing());

		// Copy the contents over.
		this.replicateContents(newSchema);

		// Return the copy.
		return newSchema;
	}

	public boolean test() throws Exception {
		// Establish the JDBC connection. May throw an exception of its own,
		// which is fine, just let it go.
		Connection connection = this.getConnection();
		// If we have no connection, we can't test it!
		if (connection == null)
			return false;

		// Get the metadata.
		DatabaseMetaData dmd = connection.getMetaData();

		// By opening, executing, then closing a DMD query we will test
		// the connection fully without actually having to read anything from
		// it.
		String catalog = connection.getCatalog();
		String schema = dmd.getUserName();
		dmd.getTables(catalog, schema, "%", null).close();

		// If we get here, it worked.
		return true;
	}

	public Connection getConnection() throws SQLException {
		// If we have not connected before, we should attempt to connect now.
		if (this.connection == null) {
			// Start out with no driver at all.
			Class loadedDriverClass = null;

			// If a path was specified for the driver, and that path exists,
			// load the driver from that path.
			if (this.driverClassLocation != null
					&& this.driverClassLocation.exists())
				try {
					ClassLoader classLoader = URLClassLoader
							.newInstance(new URL[] { this.driverClassLocation
									.toURL() });
					loadedDriverClass = classLoader
							.loadClass(this.driverClassName);
				} catch (ClassNotFoundException e) {
					SQLException e2 = new SQLException();
					e2.initCause(e);
					throw e2;
				} catch (MalformedURLException e) {
					throw new MartBuilderInternalError(e);
				}

			// If we failed to load the driver from the custom path, try the
			// system class loader instead.
			if (loadedDriverClass == null)
				try {
					loadedDriverClass = Class.forName(this.driverClassName);
				} catch (ClassNotFoundException e) {
					SQLException e2 = new SQLException();
					e2.initCause(e);
					throw e2;
				}

			// Check it really is an instance of Driver.
			if (!Driver.class.isAssignableFrom(loadedDriverClass))
				throw new ClassCastException(BuilderBundle
						.getString("driverClassNotJDBCDriver"));

			// Connect!
			Properties properties = new Properties();
			properties.setProperty("user", this.username);
			if (this.password != null)
				properties.setProperty("password", this.password);
			this.connection = DriverManager.getConnection(this.url, properties);
		}

		// Return the connection.
		return this.connection;
	}

	public String getDriverClassName() {
		return this.driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public File getDriverClassLocation() {
		return this.driverClassLocation;
	}

	public void setDriverClassLocation(File driverClassLocation) {
		this.driverClassLocation = driverClassLocation;
	}

	public String getJDBCURL() {
		return this.url;
	}

	public void setJDBCURL(String url) {
		this.url = url;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * In our case, cohabitation means that the partner link is also a
	 * {@link JDBCDataLink} and that its connection is connected to the same
	 * database server listening on the same port and connected with the same
	 * username.
	 */
	public boolean canCohabit(DataLink partner) {
		// We can't cohabit with non-JDBCDataLink partners.
		if (!(partner instanceof JDBCDataLink))
			return false;
		JDBCDataLink partnerLink = (JDBCDataLink) partner;

		// We can cohabit if the JDBC URLs and usernames are identical.
		return (partnerLink.getJDBCURL().equals(this.getJDBCURL()) && partnerLink
				.getUsername().equals(this.getUsername()));
	}

	public void synchronise() throws SQLException, BuilderException {
		// Get database metadata, catalog, and schema details.
		DatabaseMetaData dmd = this.getConnection().getMetaData();
		String catalog = this.getConnection().getCatalog();
		String schema = dmd.getUserName();

		// Create a list of existing tables. During this method, we remove from
		// this list all tables that still exist in the database. At the end of
		// the method, the list contains only those tables which no longer
		// exist, so they will be dropped.
		List tablesToBeDropped = new ArrayList(this.tables.values());

		// Create a similar list for foreign keys.
		List fksToBeDropped = new ArrayList();

		// Load tables and views from database, then loop over them.
		ResultSet dbTables = dmd.getTables(catalog, schema, "%", new String[] {
				"TABLE", "VIEW" });
		while (dbTables.next()) {
			// What is the table called?
			String dbTableName = dbTables.getString("TABLE_NAME");

			// Look to see if we already have a table by this name defined. If
			// we do, reuse it. If not, create a new table.
			Table dbTable = this.getTableByName(dbTableName);
			if (dbTable == null)
				try {
					dbTable = new GenericTable(dbTableName, this);
				} catch (Throwable t) {
					throw new MartBuilderInternalError(t);
				}

			// Table exists, so remove it from our list of tables to be dropped
			// at the end of the method.
			tablesToBeDropped.remove(dbTable);

			// Make a list of all the columns in the table. Any columns
			// remaining in this list by the end of the loop will be dropped.
			List colsToBeDropped = new ArrayList(dbTable.getColumns());

			// Load the table columns from the database, then loop over them.
			ResultSet dbTblCols = dmd.getColumns(catalog, schema, dbTableName,
					"%");
			while (dbTblCols.next()) {
				// What is the column called?
				String dbTblColName = dbTblCols.getString("COLUMN_NAME");

				// Look to see if the column already exists on this table. If it
				// does, reuse it. Else, create it.
				Column dbTblCol = dbTable.getColumnByName(dbTblColName);
				if (dbTblCol == null)
					try {
						dbTblCol = new GenericColumn(dbTblColName, dbTable);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}

				// Column exists, so remove it from our list of columns to be
				// dropped at the end of the loop.
				colsToBeDropped.remove(dbTblCol);
			}
			dbTblCols.close();

			// Drop all columns that are left in the list, as they no longer
			// exist in the database.
			for (Iterator i = colsToBeDropped.iterator(); i.hasNext();) {
				Column column = (Column) i.next();
				dbTable.removeColumn(column);
			}

			// Obtain the primary key from the database. Even in databases
			// without referential integrity, the primary key is still defined
			// and can be obtained from the metadata.
			ResultSet dbTblPKCols = dmd.getPrimaryKeys(catalog, schema,
					dbTableName);

			// Load the primary key columns into a map keyed by column position.
			// In other words, the first column in the key has a map key of 1,
			// and so on. We do this because we can't guarantee we'll read the
			// key columns from the database in the correct order. We keep the
			// map
			// sorted, so that when we iterate over it later we get back the
			// columns in the correct order.
			Map pkCols = new TreeMap();
			while (dbTblPKCols.next()) {
				String pkColName = dbTblPKCols.getString("COLUMN_NAME");
				Short pkColPosition = new Short(dbTblPKCols.getShort("KEY_SEQ"));
				pkCols.put(pkColPosition, dbTable.getColumnByName(pkColName));
			}
			dbTblPKCols.close();

			// Did DMD find a PK? If not, which is really unusual but
			// potentially may happen, attempt to find one by looking for a
			// single
			// column with the same name as the table or with '_id' appended.
			// Only do this if we are using key-guessing.
			if (pkCols.isEmpty() && this.getKeyGuessing()) {
				// Plain version first.
				Column candidateCol = dbTable.getColumnByName(dbTableName);
				// Try with '_id' appended if plain version turned up nothing.
				if (candidateCol == null)
					candidateCol = dbTable.getColumnByName(dbTableName
							+ BuilderBundle.getString("primaryKeySuffix"));
				// Found something? Add it to the primary key columns map, with
				// a dummy key of 1. (Use Short for the key because that is what
				// DMD would have used had it found anything itself).
				if (candidateCol != null)
					pkCols.put(Short.valueOf("1"), candidateCol);
			}

			// Obtain the existing primary key on the table, if the table
			// previously existed and even had one in the first place.
			PrimaryKey existingPK = dbTable.getPrimaryKey();

			// Did we find a PK on the database copy of the table?
			if (!pkCols.isEmpty()) {
				// Yes, we found a PK on the database copy of the table. So,
				// create a new key based around the columns we identified.
				PrimaryKey candidatePK;
				try {
					candidatePK = new GenericPrimaryKey(new ArrayList(pkCols
							.values()));
				} catch (Throwable t) {
					throw new MartBuilderInternalError(t);
				}

				// If the existing table has no PK, or has a PK which matches
				// and is handmade, or has a PK which does not match and is not
				// handmade, replace that PK with the one we found. This way we
				// preserve any existing handmade PKs, and don't override any
				// marked as incorrect.
				if (existingPK == null
						|| (existingPK.equals(candidatePK) && existingPK
								.getStatus().equals(ComponentStatus.HANDMADE))
						|| (!existingPK.equals(candidatePK) && !existingPK
								.getStatus().equals(ComponentStatus.HANDMADE)))
					try {
						dbTable.setPrimaryKey(candidatePK);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}
			} else {
				// No, we did not find a PK on the database copy of the table,
				// so that table should not have a PK at all. So if the existing
				// table has a PK which is not handmade, remove it.
				if (existingPK != null
						&& !existingPK.getStatus().equals(
								ComponentStatus.HANDMADE))
					try {
						dbTable.setPrimaryKey(null);
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}
			}

			// For each table, note the foreign keys that already exist in the
			// schema (not necessarily in the database), and add to the list to
			// be dropped later if no equivalents are found in the database
			// itself.
			fksToBeDropped.addAll(dbTable.getForeignKeys());
		}
		dbTables.close();

		// Remove from schema all tables not found in the database, using the
		// list we constructed above.
		for (Iterator i = tablesToBeDropped.iterator(); i.hasNext();) {
			Table existingTable = (Table) i.next();
			String tableName = existingTable.getName();
			existingTable.destroy();
			this.tables.remove(tableName);
		}

		// Are we key-guessing? Key guess the foreign keys, passing in a
		// reference to the list of existing foreign keys. After this call has
		// completed, the list will contain all those foreign keys which no
		// longer exist, and can safely be dropped.
		if (this.getKeyGuessing())
			this.synchroniseUsingKeyGuessing(fksToBeDropped);
		// Otherwise, use DMD to do the same, also passing in the list of
		// existing foreign keys to be updated as the call progresses. Also pass
		// in the DMD details so it doesn't have to work them out for itself.
		else
			this.synchroniseUsingDMD(fksToBeDropped, dmd, schema, catalog);

		// Drop any foreign keys that are left over (but not handmade ones).
		for (Iterator i = fksToBeDropped.iterator(); i.hasNext();) {
			Key k = (Key) i.next();
			if (k.getStatus().equals(ComponentStatus.HANDMADE))
				continue;
			k.destroy();
		}
	}

	/**
	 * <p>
	 * This method implements the key-guessing algorithm for foreign keys.
	 * Basically, it iterates through all known primary keys, and looks for sets
	 * of matching columns in other tables, either with the same names or with
	 * '_key' appended. Any matching sets found are assumed to be foreign keys
	 * with relations to the current primary key.
	 * <p>
	 * Relations are 1:M, except when the table at the FK end has a PK with
	 * identical column to the FK. In this case, the FK is forced to be unique,
	 * which implies that it can only partake in a 1:1 relation, so the relation
	 * is marked as such.
	 * 
	 * @param fksToBeDropped
	 *            the list of foreign keys to update as we go along. By the end
	 *            of the method, the only keys left in this list should be ones
	 *            that no longer exist in the database and may be dropped.
	 * @throws SQLException
	 *             if there was a problem talking to the database.
	 * @throws BuilderException
	 *             if there was a logical problem during construction of the set
	 *             of foreign keys.
	 */
	private void synchroniseUsingKeyGuessing(Collection fksToBeDropped)
			throws SQLException, BuilderException {
		// Loop through all the tables in the database, which is the same
		// as looping through all the primary keys.
		for (Iterator i = this.tables.values().iterator(); i.hasNext();) {
			// Obtain the table and its primary key.
			Table pkTable = (Table) i.next();
			PrimaryKey pk = pkTable.getPrimaryKey();
			// Skip all tables which have no primary key.
			if (pk == null)
				continue;

			// If an FK exists on the PK table with the same columns as the PK,
			// then we cannot use this PK to make relations to other tables.
			// This is because the FK shows that this table is not the original
			// source of the data in those columns. Some other table is the
			// original source, so we assume that relations will have been
			// established from that other table instead. So, we skip this
			// table.
			boolean pkIsAlsoAnFK = false;
			for (Iterator j = pkTable.getForeignKeys().iterator(); j.hasNext()
					&& !pkIsAlsoAnFK;) {
				Key fk = (Key) j.next();
				if (fk.getColumns().equals(pk.getColumns()))
					pkIsAlsoAnFK = true;
			}
			if (pkIsAlsoAnFK)
				continue;

			// To maintain some degree of sanity here, we assume that a PK is
			// the original source of data (and not a copy of data sourced from
			// some other table) if the first column in the PK has the same name
			// as the table it is in, or with '_id' appended, or is just 'id'
			// on its own. Any PK which does not have this property is skipped.
			Column firstPKCol = (Column) pk.getColumns().get(0);
			String firstPKColName = firstPKCol.getName();
			int idPrefixIndex = firstPKColName.indexOf(BuilderBundle
					.getString("primaryKeySuffix"));
			if (idPrefixIndex >= 0)
				firstPKColName = firstPKColName.substring(0, idPrefixIndex);
			if (!firstPKColName.equals(pkTable.getName())
					&& !firstPKColName.equals(BuilderBundle.getString("idCol")))
				continue;

			// Make a list of relations that already exist in this schema, from
			// some previous run. Any relations that are left in this list by
			// the end of the loop for this table no longer exist in the
			// database, and will be dropped.
			List relationsToBeDropped = new ArrayList(pk.getRelations());

			// Now we know that we can use this PK for certain, look for all
			// other tables (other than the one the PK itself belongs to), for
			// sets of columns with identical names, or with '_key' appended.
			// Any set that we find is going to be an FK with a relation back to
			// this PK.
			for (Iterator l = this.tables.values().iterator(); l.hasNext();) {
				// Obtain the next table to look at.
				Table fkTable = (Table) l.next();

				// Make sure the table is not the same as the PK table.
				if (fkTable.equals(pkTable))
					continue;

				// Set up an empty list for the matching columns.
				Column[] candidateFKColumns = new Column[pk.countColumns()];
				int matchingColumnCount = 0;

				// Iterate through the PK columns and find a column in the
				// target FK table with the same name, or with '_key' appended,
				// or with the PK table name and an underscore prepended.
				// If found, add that target column to the candidate FK column
				// set.
				for (int columnIndex = 0; columnIndex < pk.countColumns(); columnIndex++) {
					String pkColumnName = ((Column) pk.getColumns().get(
							columnIndex)).getName();
					// Start out by assuming no match.
					Column candidateFKColumn = null;
					// Don't try to find 'id' or 'id_key' columns as that
					// would be silly and would probably match far too much.
					if (!pkColumnName.equals(BuilderBundle.getString("idCol"))) {
						// Try equivalent name first.
						candidateFKColumn = fkTable
								.getColumnByName(pkColumnName);
						// Then try with '_key' appended, if not found.
						if (candidateFKColumn == null)
							candidateFKColumn = fkTable
									.getColumnByName(pkColumnName
											+ BuilderBundle
													.getString("foreignKeySuffix"));
					}
					// Then try with PK tablename+'_' prepended, if not found.
					if (candidateFKColumn == null)
						candidateFKColumn = fkTable.getColumnByName(pkTable
								.getName()
								+ "_" + pkColumnName);
					// Found it? Add it to the candidate list.
					if (candidateFKColumn != null) {
						candidateFKColumns[columnIndex] = candidateFKColumn;
						matchingColumnCount++;
					}
				}

				// We found a matching set, so create a FK on it!
				if (matchingColumnCount == pk.countColumns()) {
					// Create a template foreign key based around the set
					// of candidate columns we found.
					ForeignKey fk;
					try {
						fk = new GenericForeignKey(Arrays
								.asList(candidateFKColumns));
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}

					// If any FK already exists on the target table with the
					// same columns in the same order, then reuse it.
					boolean fkAlreadyExists = false;
					for (Iterator f = fkTable.getForeignKeys().iterator(); f
							.hasNext()
							&& !fkAlreadyExists;) {
						ForeignKey candidateFK = (ForeignKey) f.next();
						if (candidateFK.equals(fk)) {
							// Found one. Reuse it!
							fk = candidateFK;
							// Update the status to indicate that the FK is
							// backed by the database, if previously it was
							// handmade.
							if (fk.getStatus().equals(ComponentStatus.HANDMADE))
								fk.setStatus(ComponentStatus.INFERRED);
							// Remove the FK from the list to be dropped later,
							// as it definitely exists now.
							fksToBeDropped.remove(fk);
							// Flag the key as existing.
							fkAlreadyExists = true;
						}
					}

					// Has the key been reused, or is it a new one?
					if (!fkAlreadyExists) {
						// Its brand new, so go ahead and make the relation.
						try {
							fkTable.addForeignKey(fk);
						} catch (Throwable t) {
							throw new MartBuilderInternalError(t);
						}

						// Work out whether the relation from the FK to the PK
						// should be 1:M or 1:1. The rule is that it will be 1:M
						// in all cases except where the FK table has a PK with
						// identical columns to the FK, in which case it is 1:1,
						// as the FK is unique.
						Cardinality card = Cardinality.MANY;
						PrimaryKey fkPK = fkTable.getPrimaryKey();
						if (fkPK != null
								&& fk.getColumns().equals(fkPK.getColumns()))
							card = Cardinality.ONE;

						// Establish the relation.
						try {
							new GenericRelation(pk, fk, card);
						} catch (Throwable t) {
							throw new MartBuilderInternalError(t);
						}
					} else {
						// If the FK has been reused, check to see if it already
						// has a relation. There are three possible situations
						// here:
						// a) the relation exists between the FK and the PK
						// already, in which case we can reuse it,
						// b) the FK has no existing relation, in which case we
						// can create one, and
						// c) the FK has an existing relation to some other PK,
						// which would be logically impossible.

						// Iterate through the existing relations on the key.
						boolean relationExists = false;
						for (Iterator f = fk.getRelations().iterator(); f
								.hasNext()
								&& !relationExists;) {
							// Obtain the next relation.
							Relation candidateRel = (Relation) f.next();

							// a) a relation already exists between the FK and
							// the PK.
							if (candidateRel.getPrimaryKey().equals(pk)) {
								// Update the relation's status if handmade, as
								// it is now backed by the database.
								if (candidateRel.getStatus().equals(
										ComponentStatus.HANDMADE))
									try {
										candidateRel
												.setStatus(ComponentStatus.INFERRED);
									} catch (Throwable t) {
										throw new MartBuilderInternalError(t);
									}
								// Don't drop it at the end of the loop.
								relationsToBeDropped.remove(candidateRel);

								// Say we've found it.
								relationExists = true;
							}

							// b.i) an incorrect relation exists somewhere else,
							// which we should drop now because the database no
							// longer infers it, so in fact we need do nothing
							// here.

							else if (candidateRel.getStatus().equals(
									ComponentStatus.INFERRED_INCORRECT)) {

							}

							// b.ii) an inferred or handmade relation exists
							// somewhere else, which produces a logical
							// impossibility.
							else
								throw new MartBuilderInternalError();
						}

						// If relation did not already exist, create it.
						if (!relationExists) {
							// Work out whether the relation from the FK to the
							// PK should be 1:M or 1:1. The rule is that it will
							// be 1:M in all cases except where the FK table has
							// a PK with identical columns to the FK, in which
							// case it is 1:1, as the FK is unique.
							Cardinality card = Cardinality.MANY;
							PrimaryKey fkPK = fkTable.getPrimaryKey();
							if (fkPK != null
									&& fk.getColumns()
											.equals(fkPK.getColumns()))
								card = Cardinality.ONE;

							// Establish the relation.
							try {
								new GenericRelation(pk, fk, card);
							} catch (Throwable t) {
								throw new MartBuilderInternalError(t);
							}
						}
					}
				}
			}

			// Remove any relations that we didn't find in the database (but
			// leave the handmade ones behind).
			for (Iterator j = relationsToBeDropped.iterator(); j.hasNext();) {
				Relation r = (Relation) j.next();
				if (r.getStatus().equals(ComponentStatus.HANDMADE))
					continue;
				r.destroy();
			}
		}
	}

	/**
	 * Establish foreign keys based purely on database metadata.
	 * 
	 * @param fksToBeDropped
	 *            the list of foreign keys to update as we go along. By the end
	 *            of the method, the only keys left in this list should be ones
	 *            that no longer exist in the database and may be dropped.
	 * @param dmd
	 *            the database metadata to obtain the foreign keys from.
	 * @param schema
	 *            the database schema to read metadata from.
	 * @param catalog
	 *            the database catalog to read metadata from.
	 * @throws SQLException
	 *             if there was a problem talking to the database.
	 * @throws BuilderException
	 *             if there was a logical problem during construction of the set
	 *             of foreign keys.
	 */
	private void synchroniseUsingDMD(Collection fksToBeDropped,
			DatabaseMetaData dmd, String schema, String catalog)
			throws SQLException, BuilderException {
		// Loop through all the tables in the database, which is the same
		// as looping through all the primary keys.
		for (Iterator i = this.tables.values().iterator(); i.hasNext();) {
			// Obtain the table and its primary key.
			Table pkTable = (Table) i.next();
			PrimaryKey pk = pkTable.getPrimaryKey();
			// Skip all tables which have no primary key.
			if (pk == null)
				continue;

			// Make a list of relations that already exist in this schema, from
			// some previous run. Any relations that are left in this list by
			// the end of the loop for this table no longer exist in the
			// database, and will be dropped.
			List relationsToBeDropped = new ArrayList(pk.getRelations());

			// Identify all foreign keys in the database metadata that refer
			// to the current primary key.
			ResultSet dbTblFKCols = dmd.getExportedKeys(catalog, schema,
					pkTable.getName());

			// Loop through the results. There will be one result row per column
			// per key, so we need to build up a set of key columns in a map.
			// The map keys represent the column position within a key. Each map
			// value is a list of columns. In essence the map is a 2-D
			// representation of the foreign keys which refer to this PK, with
			// the keys of the map (Y-axis) representing the column position in
			// the FK, and the values of the map (X-axis) representing each
			// individual FK. In all cases, FK columns are assumed to be in the
			// same order as the PK columns. The map is sorted by key column
			// position.
			TreeMap dbFKs = new TreeMap();
			while (dbTblFKCols.next()) {
				String fkTblName = dbTblFKCols.getString("FKTABLE_NAME");
				String fkColName = dbTblFKCols.getString("FKCOLUMN_NAME");
				Short fkColSeq = new Short(dbTblFKCols.getShort("KEY_SEQ"));
				// Note the column.
				if (!dbFKs.containsKey(fkColSeq))
					dbFKs.put(fkColSeq, new ArrayList());
				((List) dbFKs.get(fkColSeq)).add(this.getTableByName(fkTblName)
						.getColumnByName(fkColName));
			}
			dbTblFKCols.close();

			// Only construct FKs if we actually found any.
			if (!dbFKs.isEmpty()) {
				// Identify the sequence of the first column, which may be 0 or
				// 1, depending on database implementation.
				int firstColSeq = ((Short) dbFKs.firstKey()).intValue();

				// How many columns are in the PK?
				int pkColCount = pkTable.getPrimaryKey().countColumns();

				// How many FKs do we have?
				int fkCount = ((List) dbFKs.get(dbFKs.firstKey())).size();

				// Loop through the FKs, and construct each one at a time.
				for (int j = 0; j < fkCount; j++) {
					// Set up an array to hold the FK columns.
					Column[] candidateFKColumns = new Column[pkColCount];

					// For each FK column name, look up the actual column in the
					// table.
					for (Iterator k = dbFKs.keySet().iterator(); k.hasNext();) {
						Short keySeq = (Short) k.next();
						// Convert the db-specific column index to a 0-indexed
						// figure for the array of fk columns.
						int fkColSeq = keySeq.intValue() - firstColSeq;
						candidateFKColumns[fkColSeq] = (Column) ((List) dbFKs
								.get(keySeq)).get(j);
					}

					// Create a template foreign key based around the set
					// of candidate columns we found.
					ForeignKey fk;
					try {
						fk = new GenericForeignKey(Arrays
								.asList(candidateFKColumns));
					} catch (Throwable t) {
						throw new MartBuilderInternalError(t);
					}
					Table fkTable = fk.getTable();

					// If any FK already exists on the target table with the
					// same columns in the same order, then reuse it.
					boolean fkAlreadyExists = false;
					for (Iterator f = fkTable.getForeignKeys().iterator(); f
							.hasNext()
							&& !fkAlreadyExists;) {
						ForeignKey candidateFK = (ForeignKey) f.next();
						if (candidateFK.equals(fk)) {
							// Found one. Reuse it!
							fk = candidateFK;
							// Update the status to indicate that the FK is
							// backed by the database, if previously it was
							// handmade.
							if (fk.getStatus().equals(ComponentStatus.HANDMADE))
								fk.setStatus(ComponentStatus.INFERRED);
							// Remove the FK from the list to be dropped later,
							// as it definitely exists now.
							fksToBeDropped.remove(candidateFK);
							// Flag the key as existing.
							fkAlreadyExists = true;
						}
					}

					// Has the key been reused, or is it a new one?
					if (!fkAlreadyExists) {
						// Its brand new, so go ahead and make the relation.
						try {
							fkTable.addForeignKey(fk);
						} catch (Throwable t) {
							throw new MartBuilderInternalError(t);
						}

						// Work out whether the relation from the FK to the PK
						// should be 1:M or 1:1. The rule is that it will be 1:M
						// in all cases except where the FK table has a PK with
						// identical columns to the FK, in which case it is 1:1,
						// as the FK is unique.
						Cardinality card = Cardinality.MANY;
						PrimaryKey fkPK = fkTable.getPrimaryKey();
						if (fkPK != null
								&& fk.getColumns().equals(fkPK.getColumns()))
							card = Cardinality.ONE;

						// Establish the relation.
						try {
							new GenericRelation(pk, fk, card);
						} catch (Throwable t) {
							throw new MartBuilderInternalError(t);
						}
					} else {
						// If the FK has been reused, check to see if it already
						// has a relation. There are three possible situations
						// here:
						// a) the relation exists between the FK and the PK
						// already, in which case we can reuse it,
						// b) the FK has no existing relation, in which case we
						// can create one, and
						// c) the FK has an existing relation to some other PK,
						// which would be logically impossible.

						// Iterate through the existing relations on the key.
						boolean relationExists = false;
						for (Iterator f = fk.getRelations().iterator(); f
								.hasNext()
								&& !relationExists;) {
							// Obtain the next relation.
							Relation candidateRel = (Relation) f.next();

							// a) a relation already exists between the FK and
							// the PK.
							if (candidateRel.getPrimaryKey().equals(pk)) {
								// Update the relation's status if handmade, as
								// it is now backed by the database.
								if (candidateRel.getStatus().equals(
										ComponentStatus.HANDMADE))
									try {
										candidateRel
												.setStatus(ComponentStatus.INFERRED);
									} catch (Throwable t) {
										throw new MartBuilderInternalError(t);
									}
								// Don't drop it at the end of the loop.
								relationsToBeDropped.remove(candidateRel);
								// Say we've found it.
								relationExists = true;
							}

							// b.i) an incorrect relation exists somewhere else,
							// which we should drop now because the database no
							// longer infers it, so in fact we need do nothing
							// here.

							else if (candidateRel.getStatus().equals(
									ComponentStatus.INFERRED_INCORRECT)) {

							}

							// b.ii) an inferred or handmade relation exists
							// somewhere else, which produces a logical
							// impossibility.
							else
								throw new MartBuilderInternalError();
						}

						// If relation did not already exist, create it.
						if (!relationExists) {
							// Work out whether the relation from the FK to the
							// PK should be 1:M or 1:1. The rule is that it will
							// be 1:M in all cases except where the FK table has
							// a PK with identical columns to the FK, in which
							// case it is 1:1, as the FK is unique.
							Cardinality card = Cardinality.MANY;
							PrimaryKey fkPK = fkTable.getPrimaryKey();
							if (fkPK != null
									&& fk.getColumns()
											.equals(fkPK.getColumns()))
								card = Cardinality.ONE;

							// Establish the relation.
							try {
								new GenericRelation(pk, fk, card);
							} catch (Throwable t) {
								throw new MartBuilderInternalError(t);
							}
						}
					}
				}
			}

			// Remove any relations that we didn't find in the database (but
			// leave the handmade ones behind).
			for (Iterator j = relationsToBeDropped.iterator(); j.hasNext();) {
				Relation r = (Relation) j.next();
				if (r.getStatus().equals(ComponentStatus.HANDMADE))
					continue;
				r.destroy();
			}
		}
	}
}
