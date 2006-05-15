/*
 * JDBCSchema.java
 * Created on 03 April 2006, 13:00
 */

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
import java.sql.PreparedStatement;
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
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataLink;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.model.Schema.GenericSchema;
import org.biomart.builder.resources.BuilderBundle;

/**
 * The JDBC key-guessing {@link Schema} implementation loads tables from a
 * database at construction time, along with all columns, then it guesses the keys and
 * relations between them based on naming conventions. Primary key columns are assumed to be
 * the table name with '_id' appended, unless the {@link DatabaseMetaData} query actually responds
 * with believable information, and foreign key columns are assumed to be the name of
 * the primary key column to which they refer, optionally appended with '_key'.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 15th May 2006
 * @since 0.1
 */
public class JDBCSchema extends GenericSchema implements JDBCDataLink {
    /**
     * Internal reference to the JDBC connection.
     */
    private Connection conn;
    
    /**
     * Internal reference to the driver class name.
     */
    private String driverClassName;
    
    /**
     * Internal reference to the driver class location.
     */
    private File driverClassLocation;
    
    /**
     * Internal reference to the JDBC url.
     */
    private String url;
    
    /**
     * Internal reference to the user name.
     */
    private String username;
    
    /**
     * Internal reference to the user password.
     */
    private String password;
    
    /**
     * Creates a new instance of JDBCSchema based around
     * the given JDBC Connection.
     *
     *
     * @param driverClassLocation the location of the class to load the JDBC driver from.
     * Use null to use the default class loader path, which it will also fall back on if it
     * could not find the driver at the specified location, or if this location does not exist.
     * @param driverClassName the name of the JDBC driver.
     * @param url the JDBC URL of the database server to connect to.
     * @param username the username to connect as.
     * @param password the password to connect as. Defaults to "" if null.
     * @param name the name to give it.
     */
    public JDBCSchema(File driverClassLocation, String driverClassName, String url, String username, String password, String name, boolean keyGuessing) {
        super(name);
        // Sensible defaults.
        if (driverClassLocation != null && !driverClassLocation.exists()) driverClassLocation = null;
        // Do it.
        this.driverClassLocation = driverClassLocation;
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.setKeyGuessing(keyGuessing);
    }
    
    /**
     * {@inheritDoc}
     * <p>Attempts to connect to the JDBC source.</p>
     */
    public boolean test() throws Exception {
        Connection connection = this.getConnection();
        // If we have no connection, we can't test it!
        if (connection == null) return false;
        // Get the metadata.
        DatabaseMetaData dmd = connection.getMetaData();
        // By opening and closing a DMD query we will test the connection fully.
        String catalog = this.getConnection().getCatalog();
        String schema = dmd.getUserName();
        ResultSet tables = dmd.getTables(catalog, schema, "%", null);
        tables.close();
        // If we get here, it worked.
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    public Connection getConnection() throws AssociationException, SQLException {
        if (this.conn == null) {
            // Load the driver from the custom path if specified.
            Class loadedDriverClass = null;
            if (this.driverClassLocation != null) {
                try {
                    ClassLoader classLoader =  URLClassLoader.newInstance(new URL[]{this.driverClassLocation.toURL()});
                    loadedDriverClass = classLoader.loadClass(this.driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new AssociationException(BuilderBundle.getString("driverClassNotFound"));
                } catch (MalformedURLException e) {
                    throw new AssertionError(BuilderBundle.getString("filenameFailedURLTest"));
                }
            }
            
            // Load the driver the usual way custom path missing or not working.
            try {
                loadedDriverClass = Class.forName(this.driverClassName);
            } catch (ClassNotFoundException e) {
                throw new AssociationException(BuilderBundle.getString("driverClassNotFound"));
            }
            
            // Check it really is an instance of Driver.
            if (!Driver.class.isAssignableFrom(loadedDriverClass))
                throw new AssociationException(BuilderBundle.getString("driverClassNotJDBCDriver"));
            
            // Connect!
            Properties connProps = new Properties();
            connProps.setProperty("user", this.username);
            if (this.password != null) connProps.setProperty("password", this.password);
            this.conn = DriverManager.getConnection(this.url, connProps);
        }
        
        // Return the connection.
        return this.conn;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDriverClassName() {
        return this.driverClassName;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDriverClassName(String driverClassName) {
        // Do it.
        this.driverClassName = driverClassName;
    }
    
    /**
     * {@inheritDoc}
     */
    public File getDriverClassLocation() {
        return this.driverClassLocation;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDriverClassLocation(File driverClassLocation) {
        if (driverClassLocation != null && !driverClassLocation.exists()) driverClassLocation = null;
        this.driverClassLocation = driverClassLocation;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getJDBCURL() {
        return this.url;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setJDBCURL(String url) {
        // Do it.
        this.url = url;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getUsername() {
        return this.username;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setUsername(String username) {
        // Do it.
        this.username = username;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getPassword() {
        return this.password;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * {@inheritDoc}
     * <p>In our case, cohabitation means that the partner link is also a {@link JDBCDataLink}
     * and that its connection is connected to the same database server listening on the same port
     * and connected with the same username.</p>
     */
    public boolean canCohabit(DataLink partner) {
        // Do it.
        if (!(partner instanceof JDBCDataLink)) return false;
        JDBCDataLink them = (JDBCDataLink)partner;
        return (
                them.getJDBCURL().equals(this.getJDBCURL()) &&
                them.getUsername().equals(this.getUsername())
                );
    }
    
    /**
     * {@inheritDoc}
     * <p>This version issues a 'select distinct colname from table' via JDBC.</p>
     */
    public Collection getUniqueValues(Column column) throws AssociationException, SQLException {
        // Sanity check.
        if (!column.getTable().getSchema().equals(this))
            throw new AssociationException(BuilderBundle.getString("columnSchemaMismatch"));
        // Do it.
        List values = new ArrayList();
        PreparedStatement ps = this.getConnection().prepareStatement("select distinct "+column.getName()+" from "+column.getTable().getName());
        if (ps.execute()!=true)
            throw new AssertionError(BuilderBundle.getString("prepStmtNotResultSet"));
        ResultSet rs = ps.getResultSet();
        if (rs==null)
            throw new AssertionError(BuilderBundle.getString("prepStmtHasNoResultSet"));
        while (rs.next()) values.add(rs.getObject(1));
        rs.close();
        ps.close();
        return values;
    }
    
    /**
     * {@inheritDoc}
     * <p>This version issues a 'select count(distinct colname) from table' via JDBC.</p>
     */
    public int countUniqueValues(Column column) throws AssociationException, SQLException {
        // Sanity check.
        if (!column.getTable().getSchema().equals(this))
            throw new AssociationException(BuilderBundle.getString("columnSchemaMismatch"));
        // Do it.
        PreparedStatement ps = this.getConnection().prepareStatement("select count(distinct "+column.getName()+") from "+column.getTable().getName());
        if (ps.execute()!=true)
            throw new AssertionError(BuilderBundle.getString("prepStmtNotResultSet"));
        ResultSet rs = ps.getResultSet();
        if (rs==null)
            throw new AssertionError(BuilderBundle.getString("prepStmtHasNoResultSet"));
        if (!rs.next())
            throw new AssertionError(BuilderBundle.getString("prepStmtFailedOneRowTest"));
        int rowCount = rs.getInt(1);
        rs.close();
        ps.close();
        return rowCount;
    }
    
    /**
     * {@inheritDoc}
     */
    public void synchronise() throws SQLException, BuilderException {
        // Get database metadata.
        DatabaseMetaData dmd = this.getConnection().getMetaData();
        String catalog = this.getConnection().getCatalog();
        String schema = dmd.getUserName();
        
        // Identify and temporarily preserve previously known tables. Refer
        // to them as the removed tables as by the end of this the set will
        // only contain those tables which have been dropped.
        List tablesToBeDropped = new ArrayList(this.tables.values());
        List fksToBeDropped = new ArrayList(); // for the foreign keys.
        
        // Load tables and columns from database.
        // Catalog = database name/SID from connection string, Schema = username.
        ResultSet dbTables = dmd.getTables(catalog, schema, "%", new String[]{"TABLE", "VIEW"});
        while (dbTables.next()) {
            String dbTableName = dbTables.getString("TABLE_NAME");
            // If its a new table, create and add it. Otherwise, just look it up.
            Table existingTable = this.getTableByName(dbTableName);
            if (existingTable==null) existingTable = new GenericTable(dbTableName, this);
            
            // Remove table from previously known list.
            tablesToBeDropped.remove(existingTable);
            
            // For each table loaded, temporarily preserve existing columns. Refer
            // to them as removed columns as by the end of this the set will only
            // contain those columns which are to be dropped.
            List colsToBeDropped = new ArrayList(existingTable.getColumns());
            
            // Get the table columns from the database.
            ResultSet dbTblCols = dmd.getColumns(catalog, schema, dbTableName, "%");
            while (dbTblCols.next()) {
                String dbTblColName = dbTblCols.getString("COLUMN_NAME");
                // If its a new column, create and add it. Otherwise, just look it up.
                Column dbTblCol = existingTable.getColumnByName(dbTblColName);
                if (dbTblCol==null) dbTblCol = new GenericColumn(dbTblColName, existingTable);
                colsToBeDropped.remove(dbTblCol); // Stop it from being dropped.
            }
            dbTblCols.close();
            
            // Remove from table all columns not found in database.
            for (Iterator i = colsToBeDropped.iterator(); i.hasNext(); ) {
                Column colName = (Column)i.next();
                existingTable.removeColumn(colName);
                i.remove(); // really get rid of it so we don't have any references left behind.
            }
            
            // Load the table's primary key.
            ResultSet dbTblPKCols = dmd.getPrimaryKeys(catalog, schema, dbTableName);
            Map pkCols = new TreeMap(); // sorts by column position
            while (dbTblPKCols.next()) {
                String pkColName = dbTblPKCols.getString("COLUMN_NAME");
                Short pkKeySeq = new Short(dbTblPKCols.getShort("KEY_SEQ"));
                pkCols.put(pkKeySeq, existingTable.getColumnByName(pkColName));
            }
            dbTblPKCols.close();
            
            // Did DMD find a PK? If not, attempt to find one by looking for the column with
            // the same name as the table but with '_id' appended.
            if (this.getKeyGuessing() && pkCols.isEmpty()) {
                Column candidateCol = existingTable.getColumnByName(dbTableName + BuilderBundle.getString("primaryKeySuffix"));
                if (candidateCol != null) pkCols.put(candidateCol.getName(), candidateCol);
            }
            
            // Did we find a PK?
            PrimaryKey existingPK = existingTable.getPrimaryKey();
            if (!pkCols.isEmpty()) {
                // Create and set the primary key (only if existing one is not the same and is not handmade).
                // (or if existing one is same and is handmade).
                PrimaryKey candidatePK = new GenericPrimaryKey(new ArrayList(pkCols.values()));
                if (existingPK == null ||
                        (existingPK.equals(candidatePK) && existingPK.getStatus().equals(ComponentStatus.HANDMADE)) ||
                        (!existingPK.equals(candidatePK) && !existingPK.getStatus().equals(ComponentStatus.HANDMADE))) {
                    existingTable.setPrimaryKey(candidatePK);
                }
            } else {
                // Remove the primary key on this table, but only if the existing one is not handmade.
                if (existingPK != null && !existingPK.getStatus().equals(ComponentStatus.HANDMADE)) {
                    existingTable.setPrimaryKey(null);
                }
            }
            
            // For each table loaded, note the foreign keys that already exist.
            fksToBeDropped.addAll(existingTable.getForeignKeys());
        }
        dbTables.close();
        
        // Remove from schema all tables not found in database.
        for (Iterator i = tablesToBeDropped.iterator(); i.hasNext(); ) {
            Table existingTable = (Table)i.next();
            String tableName = existingTable.getName();
            existingTable.destroy();
            this.tables.remove(tableName);
            i.remove(); // really get rid of it so we don't have any references left behind.
        }
        
        // Do the fk+relation bit here
        if (this.getKeyGuessing()) this.synchroniseUsingKeyGuessing(fksToBeDropped);
        else this.synchroniseUsingDMD(fksToBeDropped, dmd, schema, catalog);
        
        // Drop any foreign keys that are left over (but not handmade ones).
        for (Iterator i = fksToBeDropped.iterator(); i.hasNext(); ) {
            Key k = (Key)i.next();
            if (k.getStatus().equals(ComponentStatus.HANDMADE)) continue;
            k.destroy();
            i.remove(); // just to make sure it really has gone.
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>Primary keys, if not returned by the {@link DatabaseMetaData} are assumed to
     * be single-column keys named the same as the table but with '_id' appended. Foreign keys
     * are assumed wherever the columns from a primary key are found in a foreign table in
     * the same order and with the same name, or with '_key' appended to them. (Either all must
     * have '_key' appended, or none, it won't detect a mixture of both).</p>
     * <p>This implementation reads tables and views from the schema with the same name as the
     * logged-in user only. On MySQL, for instance, this is irrelevant as it has no such
     * concept of schema, but on Oracle this means that only tables and views owned by
     * the logged-in user will appear. If you want tables from other schemas in Oracle,
     * you'll have to create views onto them from the logged-in user's schema first.</p>
     * <p>This implementation ignores all tables returned by the connection's metadata
     * that do not have a type of TABLE or VIEW. See {@link DatabaseMetaData#getTables(String, String, String, String[])}
     * for details.</p>
     */
    public void synchroniseUsingKeyGuessing(Collection fksToBeDropped) throws SQLException, BuilderException {
        // Go through the tables we found and update relations.
        for (Iterator i = this.tables.values().iterator(); i.hasNext(); ) {
            Table existingTable = (Table)i.next();
            PrimaryKey existingPK = existingTable.getPrimaryKey();
            if (existingPK==null) continue; // no need to do this to tables without PKs
            
            // In key-guessing only, if an FK exists on the PK's table with the same columns
            // as the PK, then the PK itself cannot be used as an FK elsewhere (because the
            // FK that already exists indicates that other tables with the same columns have
            // already had FKs created back to a different table). Hence, we can skip it.
            boolean pkIsFK = false;
            for (Iterator j = existingTable.getForeignKeys().iterator(); j.hasNext() && !pkIsFK; ) {
                Key k = (Key)j.next();
                if (k.getColumns().equals(existingPK.getColumns())) pkIsFK = true;
            }
            if (pkIsFK) continue;
            
            // Build up a set of relations that already exist. Call it removed because by the end
            // of this it will contain a set of relations that no longer exist and should be dropped.
            List relationsToBeDropped = new ArrayList(existingPK.getRelations());
            
            // Inner iterator runs over every table looking for columns with the same
            // name or with '_key' appended.
            for (Iterator l = this.tables.values().iterator(); l.hasNext(); ) {
                Table fkTable = (Table)l.next();
                if (fkTable.equals(existingTable)) continue; // Don't link to ourselves!
                // Check all the PK columns to find FK candidate equivalents in this fkTable.
                Column[] candidateFKColumns = new Column[existingPK.countColumns()];
                int candidateFKColumnCount = 0;
                for (Iterator k = existingPK.getColumns().iterator(); k.hasNext(); ) {
                    String pkColumnName = ((Column)k.next()).getName();
                    Column candidateFKColumn = fkTable.getColumnByName(pkColumnName);
                    if (candidateFKColumn == null) candidateFKColumn = fkTable.getColumnByName(pkColumnName + BuilderBundle.getString("foreignKeySuffix"));
                    if (candidateFKColumn != null) candidateFKColumns[candidateFKColumnCount++] = candidateFKColumn;
                }
                
                // We found a matching set, so create a FK on it!
                if (candidateFKColumnCount == existingPK.countColumns()) {
                    
                    ForeignKey fk = new GenericForeignKey(Arrays.asList(candidateFKColumns));
                    boolean fkAlreadyExists = false;
                    // If we've already got one like that on this table, reuse it, otherwise add it.
                    for (Iterator f = fkTable.getForeignKeys().iterator(); f.hasNext() && !fkAlreadyExists; ) {
                        ForeignKey candidateFK = (ForeignKey)f.next();
                        if (candidateFK.equals(fk)) {
                            // Found one. Reuse it!
                            fk = candidateFK;
                            if (fk.getStatus().equals(ComponentStatus.HANDMADE)) fk.setStatus(ComponentStatus.INFERRED);
                            fksToBeDropped.remove(candidateFK); // don't drop it any more.
                            fkAlreadyExists =true;
                        }
                    }
                    
                    if (!fkAlreadyExists) {
                        // If its new, go ahead and make the relation.
                        fkTable.addForeignKey(fk);
                        Cardinality card = Cardinality.MANY;
                        PrimaryKey fkPK = fkTable.getPrimaryKey();
                        if (fkPK != null && fk.getColumns().equals(fkPK.getColumns())) card = Cardinality.ONE;
                        new GenericRelation(existingPK, fk, card);
                    } else {
                        // If its not new, check to see if it already has a relation.
                        
                        // Check to see if this FK already has a link to this PK.
                        // If it has a correctly inferred connection to any other PK,
                        // complain bitterly. If it has an incorrect or handmade connection
                        // to any other PK, remove that one.
                        boolean relationExists = false;
                        for (Iterator f = fk.getRelations().iterator(); f.hasNext() && !relationExists; ) {
                            Relation r = (Relation)f.next();
                            if (r.getPrimaryKey().equals(existingPK)) {
                                if (r.getStatus().equals(ComponentStatus.HANDMADE)) r.setStatus(ComponentStatus.INFERRED);
                                relationsToBeDropped.remove(r); // don't drop it, just leave it untouched and reuse it.
                                relationExists = true;
                            } else if (r.getStatus().equals(ComponentStatus.INFERRED)) {
                                throw new AssertionError(BuilderBundle.getString("fkHasMultiplePKs"));
                            } else {
                                // It'll get removed later if we leave it in relationsToBeDropped.
                                // To do that, we need do nothing.
                            }
                        }
                        
                        // If checks returned OK, create a new relation.
                        if (!relationExists) {
                            Cardinality card = Cardinality.MANY;
                            PrimaryKey fkPK = fkTable.getPrimaryKey();
                            if (fkPK != null && fk.getColumns().equals(fkPK.getColumns())) card = Cardinality.ONE;
                            new GenericRelation(existingPK, fk, card);
                        }
                    }
                }
            }
            
            // Remove any relations that we don't find in the database (but leave
            // the handmade ones behind).
            for (Iterator j = relationsToBeDropped.iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                if (r.getStatus().equals(ComponentStatus.HANDMADE)) continue;
                r.destroy();
                j.remove(); // just to make sure it's really gone properly
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * <p>This implementation reads tables and views from the schema with the same name as the
     * logged-in user only. On MySQL, for instance, this is irrelevant as it has no such
     * concept of schema, but on Oracle this means that only tables and views owned by
     * the logged-in user will appear. If you want tables from other schemas in Oracle,
     * you'll have to create views onto them from the logged-in user's schema first.</p>
     * <p>This implementation ignores all tables returned by the connection's metadata
     * that do not have a type of TABLE or VIEW. See {@link DatabaseMetaData#getTables(String, String, String, String[])}
     * for details.</p>
     */
    public void synchroniseUsingDMD(Collection fksToBeDropped, DatabaseMetaData dmd, String schema, String catalog) throws SQLException, BuilderException {
        // Go through the tables we found and update relations.
        for (Iterator i = this.tables.values().iterator(); i.hasNext(); ) {
            Table existingTable = (Table)i.next();
            PrimaryKey existingPK = existingTable.getPrimaryKey();
            if (existingPK==null) continue; // no need to do this to tables without PKs
            
            // Build up a set of relations that already exist. Call it removed because by the end
            // of this it will contain a set of relations that no longer exist and should be dropped.
            List relationsToBeDropped = new ArrayList(existingPK.getRelations());
            
            // Load relations from db referring to this primary key.
            TreeMap dbFKs = new TreeMap(); // sorts by key sequence
            ResultSet dbTblFKCols = dmd.getExportedKeys(catalog, schema, existingTable.getName());
            // Build a map of key positions to lists of columns. So, if a table has two keys, it will
            // have two entries at each key position.
            while (dbTblFKCols.next()) {
                // FK cols are assumed to be in the same order as their associated PK cols.
                String fkTblName = dbTblFKCols.getString("FKTABLE_NAME");
                String fkColName = dbTblFKCols.getString("FKCOLUMN_NAME");
                Short fkKeySeq = new Short(dbTblFKCols.getShort("KEY_SEQ"));
                // Note the column.
                if (!dbFKs.containsKey(fkKeySeq)) dbFKs.put(fkKeySeq, new ArrayList());
                ((List)dbFKs.get(fkKeySeq)).add(this.getTableByName(fkTblName).getColumnByName(fkColName));
            }
            dbTblFKCols.close();
            
            // Only construct FKs if we actually found any.
            if (!dbFKs.isEmpty()) {
                // How many keys do we have?
                int lowestKeySeq = ((Short)dbFKs.firstKey()).intValue();
                int colCount = existingTable.getPrimaryKey().countColumns();
                int keyCount = ((List)dbFKs.get(dbFKs.firstKey())).size();
                
                // Construct the FKs we found.
                for (int j = 0; j < keyCount; j++) {
                    Column[] candidateFKColumns = new Column[colCount];
                    for (Iterator k = dbFKs.keySet().iterator(); k.hasNext(); ) {
                        Short keySeq = (Short)k.next();
                        int keySeqValue = keySeq.intValue() - lowestKeySeq; // ensures 0-index
                        List l = (List)dbFKs.get(keySeq);
                        candidateFKColumns[keySeqValue] = (Column)l.get(j);
                    }
                    
                    ForeignKey fk = new GenericForeignKey(Arrays.asList(candidateFKColumns));
                    boolean fkAlreadyExists = false;
                    // If we've already got one like that, reuse it, otherwise add it.
                    for (Iterator f = fk.getTable().getForeignKeys().iterator(); f.hasNext() && !fkAlreadyExists; ) {
                        ForeignKey candidateFK = (ForeignKey)f.next();
                        if (candidateFK.equals(fk)) {
                            // Found one. Reuse it!
                            fk = candidateFK;
                            if (fk.getStatus().equals(ComponentStatus.HANDMADE)) fk.setStatus(ComponentStatus.INFERRED);
                            fksToBeDropped.remove(candidateFK); // don't drop it any more.
                            fkAlreadyExists =true;
                        }
                    }
                    
                    if (!fkAlreadyExists) {
                        // If its new, go ahead and make the relation.
                        fk.getTable().addForeignKey(fk);
                        Cardinality card = Cardinality.MANY;
                        PrimaryKey fkPK = fk.getTable().getPrimaryKey();
                        if (fkPK != null && fk.getColumns().equals(fkPK.getColumns())) card = Cardinality.ONE;
                        new GenericRelation(existingPK, fk, card);
                    } else {
                        // If its not new, check to see if it already has a relation.
                        
                        // Check to see if this FK already has a link to this PK.
                        // If it has a correctly inferred connection to any other PK,
                        // complain bitterly. If it has an incorrect or handmade connection
                        // to any other PK, remove that one.
                        boolean relationExists = false;
                        for (Iterator f = fk.getRelations().iterator(); f.hasNext() && !relationExists; ) {
                            Relation r = (Relation)f.next();
                            if (r.getPrimaryKey().equals(existingPK)) {
                                relationsToBeDropped.remove(r); // don't drop it, just leave it untouched and reuse it.
                                if (r.getStatus().equals(ComponentStatus.HANDMADE)) r.setStatus(ComponentStatus.INFERRED);
                                relationExists = true;
                            } else if (r.getStatus().equals(ComponentStatus.INFERRED)) {
                                throw new AssertionError(BuilderBundle.getString("fkHasMultiplePKs"));
                            } else {
                                // It'll get removed later if we leave it in relationsToBeDropped.
                                // To do that, we need do nothing.
                            }
                        }
                        
                        // If checks returned false, create a new relation.
                        if (!relationExists) {
                            Cardinality card = Cardinality.MANY;
                            PrimaryKey fkPK = fk.getTable().getPrimaryKey();
                            if (fkPK != null && fk.getColumns().equals(fkPK.getColumns())) card = Cardinality.ONE;
                            new GenericRelation(existingPK, fk, card);
                        }
                    }
                }
            }
            
            // Remove any relations that we don't find in the database (but leave
            // the handmade ones behind).
            for (Iterator j = relationsToBeDropped.iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                if (r.getStatus().equals(ComponentStatus.HANDMADE)) continue;
                r.destroy();
                j.remove(); // just to make sure it's really gone properly
            }
        }
    }
}
