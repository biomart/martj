/*
 * JDBCNonRelationalTableProvider.java
 *
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
import org.biomart.builder.model.TableProvider.GenericTableProvider;

/**
 * The JDBC non-relational {@link TableProvider} implementation loads tables from a
 * database at construction time, along with all columns, then it guesses the keys and
 * relations between them based on naming conventions. Primary key columns are assumed to be
 * the table name with '_id' appended, unless the {@link DatabaseMetaData} query actually responds
 * with believable information, and foreign key columns are assumed to be the name of
 * the primary key column to which they refer, optionally appended with '_key'.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 */
public class JDBCNonRelationalTableProvider extends GenericTableProvider implements JDBCDataLink {
    /**
     * Internal reference to the JDBC connection. Transience
     * means it won't get persisted when serialized.
     */
    private transient Connection conn;
    
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
     * Creates a new instance of JDBCNonRelationalTableProvider based around
     * the given JDBC Connection.
     * @param driverClassLocation the location of the class to load the JDBC driver from.
     * Use null to use the default class loader path, which it will also fall back on if it
     * could not find the driver at the specified location, or if this location does not exist.
     * @param driverClassName the name of the JDBC driver.
     * @param url the JDBC URL of the database server to connect to.
     * @param username the username to connect as.
     * @param password the password to connect as. Defaults to "" if null.
     * @param name the name to give it.
     * @throws NullPointerException if any parameter other than driverClassLocation or password is null.
     */
    public JDBCNonRelationalTableProvider(File driverClassLocation, String driverClassName, String url, String username, String password, String name) throws NullPointerException {
        super(name);
        // Sanity check.
        if (driverClassName == null)
            throw new NullPointerException("Driver class name cannot be null.");
        if (url == null)
            throw new NullPointerException("JDBC URL cannot be null.");
        // Sensible defaults.
        if (driverClassLocation != null && !driverClassLocation.exists()) driverClassLocation = null;
        // Do it.
        this.driverClassLocation = driverClassLocation;
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    /**
     * Returns a JDBC {@link Connection} connected to this database
     * using the data supplied to all the other methods in this interface.
     * @return the {@link Connection} for this database.
     * @throws AssociationException if there was any problem finding the class.
     * @throws SQLException if there was any problem connecting.
     */
    public Connection getConnection() throws AssociationException, SQLException {
        if (this.conn == null) {
            // Load the driver from the custom path if specified.
            Class loadedDriverClass = null;
            if (this.driverClassLocation != null) {
                try {
                    ClassLoader classLoader = URLClassLoader.newInstance(new URL[]{this.driverClassLocation.toURL()});
                    loadedDriverClass = classLoader.loadClass(this.driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new AssociationException("Class specified could not be found.");
                } catch (MalformedURLException e) {
                    throw new AssertionError("Filename failed to translate into URL.");
                }
            }
            
            // Load the driver the usual way custom path missing or not working.
            try {
                loadedDriverClass = Class.forName(this.driverClassName);
            } catch (ClassNotFoundException e) {
                throw new AssociationException("Class specified could not be found.");
            }
            
            // Check it really is an instance of Driver.
            if (!Driver.class.isAssignableFrom(loadedDriverClass))
                throw new AssociationException("Class specified is not a JDBC Driver class.");
            
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
     * Getter for property driverClassName.
     * @return Value of property driverClassName.
     */
    public String getDriverClassName() {
        return this.driverClassName;
    }
    
    /**
     * Setter for property driverClassName.
     * @param driverClassName New value of property driverClassName.
     * @throws NullPointerException if the name is null.
     */
    public void setDriverClassName(String driverClassName) throws NullPointerException {
        // Sanity check.
        if (driverClassName == null)
            throw new NullPointerException("Driver class name cannot be null.");
        // Do it.
        this.driverClassName = driverClassName;
    }
    
    /**
     * Getter for property driverClassLocation. Defaults to null if not specified.
     * @return Value of property driverClassLocation.
     */
    public File getDriverClassLocation() {
        return this.driverClassLocation;
    }
    
    /**
     * Setter for property driverClassLocation. Defaults to null if not specified.
     * @param driverClassLocation New value of property driverClassLocation.
     */
    public void setDriverClassLocation(File driverClassLocation) {
        if (driverClassLocation != null && !driverClassLocation.exists()) driverClassLocation = null;
        this.driverClassLocation = driverClassLocation;
    }
    
    /**
     * Getter for property url.
     * @return Value of property url.
     */
    public String getJDBCURL() {
        return this.url;
    }
    
    /**
     * Setter for property url.
     * @param url New value of property url.
     * @throws NullPointerException if the url is null.
     */
    public void setJDBCURL(String url) throws NullPointerException {
        // Sanity check.
        if (url == null)
            throw new NullPointerException("JDBC URL cannot be null.");
        // Do it.
        this.url = url;
    }
    
    /**
     * Getter for property username.
     * @return Value of property username.
     */
    public String getUsername() {
        return this.username;
    }
    
    /**
     * Setter for property username.
     * @param username New value of property username.
     * @throws NullPointerException if the name is null.
     */
    public void setUsername(String username) throws NullPointerException {
        // Sanity check.
        if (username == null)
            throw new NullPointerException("Username cannot be null.");
        // Do it.
        this.username = username;
    }
    
    /**
     * Getter for property password.
     * @return Value of property password.
     */
    public String getPassword() {
        return this.password;
    }
    
    /**
     * Setter for property password. May be null.
     * @param password New value of property password.
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * <p>Checks to see if this {@link DataLink} 'cohabits' with another one. Cohabitation means
     * that it would be possible to write a single SQL statement that could read data from
     * both {@link DataLink}s simultaneously.</p>
     *
     * <p>In our case, cohabitation means that the partner link is also a {@link JDBCDataLink}
     * and that its connection is connected to the same database server listening on the same port
     * and connected with the same username.</p>
     *
     * @param partner the other {@link DataLink} to test for cohabitation.
     * @return true if the two can cohabit, false if not.
     * @throws NullPointerException if the partner is null.
     */
    public boolean canCohabit(DataLink partner) throws NullPointerException {
        // Sanity check.
        if (partner == null)
            throw new NullPointerException("Partner cannot be null.");
        // Do it.
        if (!(partner instanceof JDBCDataLink)) return false;
        JDBCDataLink them = (JDBCDataLink)partner;
        return (
                them.getJDBCURL().equals(this.getJDBCURL()) &&
                them.getUsername().equals(this.getUsername())
                );
    }
    
    /**
     * <p>Returns a set of unique values in a given column, which may include null. The
     * set returned will never be null itself.</p>
     *
     * <p>This version issues a 'select distinct colname from table' via JDBC.</p>
     *
     * @param column the {@link Column} to get unique values for.
     * @return a set of unique values in a given column.
     * @throws AssociationException if the column doesn't belong to us.
     * @throws SQLException if there was any problem loading the values.
     * @throws NullPointerException if the column was null.
     */
    public Collection getUniqueValues(Column column) throws AssociationException, NullPointerException, SQLException {
        // Sanity check.
        if (column == null)
            throw new NullPointerException("Column cannot be null.");
        if (!column.getTable().getTableProvider().equals(this))
            throw new AssociationException("Column doesn't belong to this table provider.");
        // Do it.
        List values = new ArrayList();
        PreparedStatement ps = this.getConnection().prepareStatement("select distinct "+column.getName()+" from "+column.getTable().getName());
        if (ps.execute()!=true)
            throw new AssertionError("Prepared statement failed to return a ResultSet.");
        ResultSet rs = ps.getResultSet();
        if (rs==null)
            throw new AssertionError("Prepared statement said it would return a ResultSet but didn't.");
        while (rs.next()) values.add(rs.getObject(1));
        rs.close();
        ps.close();
        return values;
    }
    
    /**
     * <p>Counts the unique values in a given column, which may include null.</p>
     *
     * <p>This version issues a 'select count(distinct colname) from table' via JDBC.</p>
     *
     * @param column the {@link Column} to get unique values for.
     * @return a count of the unique values in a given column.
     * @throws AssociationException if the column doesn't belong to us.
     * @throws SQLException if there was any problem counting the values.
     * @throws NullPointerException if the column was null.
     */
    public int countUniqueValues(Column column) throws AssociationException, NullPointerException, SQLException {
        // Sanity check.
        if (column == null)
            throw new NullPointerException("Column cannot be null.");
        if (!column.getTable().getTableProvider().equals(this))
            throw new AssociationException("Column doesn't belong to this table provider.");
        // Do it.
        PreparedStatement ps = this.getConnection().prepareStatement("select count(distinct "+column.getName()+") from "+column.getTable().getName());
        if (ps.execute()!=true)
            throw new AssertionError("Prepared statement failed to return a ResultSet.");
        ResultSet rs = ps.getResultSet();
        if (rs==null)
            throw new AssertionError("Prepared statement said it would return a ResultSet but didn't.");
        if (!rs.next())
            throw new AssertionError("ResultSet guaranteed to return at least 1 row but returned 0.");
        int rowCount = rs.getInt(1);
        rs.close();
        ps.close();
        return rowCount;
    }
    
    /**
     * <p>Synchronise this {@link TableProvider} with the data source that is
     * providing its tables. Synchronisation means checking the list of {@link Table}s
     * available and drop/add any that have changed, then check each {@link Column}.
     * and {@link Key} and {@link Relation} and update those too.
     * Any {@link Key} or {@link Relation} that was created by the user and is still valid,
     * ie. the underlying columns still exist, will not be affected by this operation.</p>
     *
     * <p>Primary keys, if not returned by the {@link DatabaseMetaData} are assumed to
     * be single-column keys named the same as the table but with '_id' appended. Foreign keys
     * are assumed wherever the columns from a primary key are found in a foreign table in
     * the same order and with the same name, or with '_key' appended to them. (Either all must
     * have '_key' appended, or none, it won't detect a mixture of both).</p>
     *
     * <p>This implementation reads tables and views from the schema with the same name as the
     * logged-in user only. On MySQL, for instance, this is irrelevant as it has no such
     * concept of schema, but on Oracle this means that only tables and views owned by
     * the logged-in user will appear. If you want tables from other schemas in Oracle,
     * you'll have to create views onto them from the logged-in user's schema first.</p>
     *
     * <p>This implementation ignores all tables returned by the connection's metadata
     * that do not have a type of TABLE or VIEW. See {@link DatabaseMetaData#getTables(String, String, String, String[])}
     * for details.</p>
     *
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchronise() throws SQLException, BuilderException {
        // Get database metadata.
        DatabaseMetaData dmd = this.getConnection().getMetaData();
        String catalog = this.getConnection().getCatalog();
        String schema = dmd.getUserName();
        
        // Identify and temporarily preserve previously known tables. Refer
        // to them as the removed tables as by the end of this the set will
        // only contain those tables which have been dropped.
        Set removedTables = new HashSet();
        removedTables.addAll(this.tables.keySet());
        Set removedFKs = new HashSet(); // for the foreign keys.
        
        // Load tables and columns from database.
        // Catalog = database name/SID from connection string, Schema = username.
        ResultSet dbTables = dmd.getTables(catalog, schema, "%", new String[]{"TABLE", "VIEW"});
        while (dbTables.next()) {
            String dbTableName = dbTables.getString("TABLE_NAME");
            // If its a new table, create and add it. Otherwise, just look it up.
            Table existingTable = this.getTableByName(dbTableName);
            if (existingTable==null) existingTable = new GenericTable(dbTableName, this);
            
            // For each table loaded, temporarily preserve existing columns. Refer
            // to them as removed columns as by the end of this the set will only
            // contain those columns which are to be dropped.
            Set removedCols = new HashSet();
            removedCols.addAll(existingTable.getColumns());
            
            // Get the table columns from the database.
            ResultSet dbTblCols = dmd.getColumns(catalog, schema, dbTableName, "%");
            while (dbTblCols.next()) {
                String dbTblColName = dbTblCols.getString("COLUMN_NAME");
                // If its a new column, create and add it. Otherwise, just look it up.
                Column dbTblCol = existingTable.getColumnByName(dbTblColName);
                if (dbTblCol==null) dbTblCol = new GenericColumn(dbTblColName, existingTable);
            }
            dbTblCols.close();
            
            // Remove from table all columns not found in database.
            for (Iterator i = removedCols.iterator(); i.hasNext(); ) {
                Column colName = (Column)i.next();
                existingTable.removeColumn(colName);
                i.remove(); // really get rid of it so we don't have any references left behind.
            }
            
            // Remove table from previously known list.
            removedTables.remove(dbTableName);
            
            // Load the table's primary key.
            ResultSet dbTblPKCols = dmd.getPrimaryKeys(catalog, schema, dbTableName);
            Map pkCols = new TreeMap(); // sorts by key
            while (dbTblPKCols.next()) {
                String pkColName = dbTblPKCols.getString("COLUMN_NAME");
                Short pkKeySeq = new Short(dbTblPKCols.getShort("KEY_SEQ"));
                pkCols.put(pkKeySeq, existingTable.getColumnByName(pkColName));
            }
            dbTblPKCols.close();
            
            // Did DMD find a PK? If not, attempt to find one by looking for the column with
            // the same name as the table but with '_id' appended.
            if (pkCols.isEmpty()) {
                Column candidateCol = existingTable.getColumnByName(dbTableName + "_id");
                if (candidateCol != null) pkCols.put(candidateCol.getName(), candidateCol);
            }
            
            // Did we find a PK?
            PrimaryKey existingPK = existingTable.getPrimaryKey();
            if (!pkCols.isEmpty()) {
                // Create and set the primary key (only if existing one is not the same).
                if (existingPK == null || !existingPK.getColumns().equals(pkCols)) new GenericPrimaryKey(new ArrayList(pkCols.values()));
            } else {
                // Remove the primary key on this table, but only if the existing one is not handmade.
                if (existingPK!=null && !existingPK.getStatus().equals(ComponentStatus.HANDMADE)) existingTable.setPrimaryKey(null);
            }
            
            // For each table loaded, note the foreign keys that already exist.
            for (Iterator i = existingTable.getForeignKeys().iterator(); i.hasNext(); ) {
                Key k = (Key)i.next();
                removedFKs.add(k);
            }
        }
        dbTables.close();
        
        // Remove from schema all tables not found in database.
        for (Iterator i = removedTables.iterator(); i.hasNext(); ) {
            String tableName = (String)i.next();
            Table existingTable = (Table)this.tables.get(tableName);
            existingTable.destroy();
            this.tables.remove(existingTable);
            i.remove(); // really get rid of it so we don't have any references left behind.
        }
        
        // Go through the tables we found and update relations.
        for (Iterator i = this.tables.values().iterator(); i.hasNext(); ) {
            Table existingTable = (Table)i.next();
            PrimaryKey existingPK = existingTable.getPrimaryKey();
            if (existingPK==null) continue; // no need to do this to tables without PKs
            
            // Build up a set of relations that already exist. Call it removed because by the end
            // of this it will contain a set of relations that no longer exist and should be dropped.
            Set removedRels = new HashSet();
            for (Iterator j = existingPK.getRelations().iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                removedRels.add(r);
            }
            
            // Infer relations from db referring to this primary key.
            TreeMap dbFKs = new TreeMap();
            
            // Inner iterator runs over every table looking for columns with the same
            // name or with '_key' appended.
            for (Iterator inner = this.tables.values().iterator(); inner.hasNext(); ) {
                Table fkTable = (Table)inner.next();
                if (fkTable.equals(existingTable)) continue; // Don't link to ourselves!
                
                // Check all the PK columns to find FK candidate equivalents in this fkTable.
                Column[] candidateFKColumns = new Column[existingPK.countColumns()];
                int candidateFKColumnCount = 0;
                for (Iterator k = existingPK.getColumns().iterator(); k.hasNext(); ) {
                    String pkColumnName = ((Column)k.next()).getName();
                    Column candidateFKColumn = fkTable.getColumnByName(pkColumnName);
                    if (candidateFKColumn == null) candidateFKColumn = fkTable.getColumnByName(pkColumnName + "_key");
                    if (candidateFKColumn != null) candidateFKColumns[candidateFKColumnCount++] = candidateFKColumn;
                }
                
                // We found a matching set, so create a FK on it!
                if (candidateFKColumnCount == existingPK.countColumns()) {
                    
                    ForeignKey newFK = new GenericForeignKey(Arrays.asList(candidateFKColumns));;
                    boolean newFKAlreadyExists = false;
                    // If we've already got one like that, reuse it, otherwise add it.
                    for (Iterator f = removedFKs.iterator(); f.hasNext() && !newFKAlreadyExists; ) {
                        ForeignKey candidateFK = (ForeignKey)f.next();
                        if (candidateFK.equals(newFK)) {
                            // Found one. Reuse it!
                            newFK = candidateFK;
                            f.remove(); // don't drop it any more.
                            newFKAlreadyExists =true;
                        }
                    }
                    if (!newFKAlreadyExists) fkTable.addForeignKey(newFK);
                    
                    // Check to see if there is already a relation between the PK and the FK. If
                    // so, reuse it. If not, create one.
                    boolean relationExists = false;
                    for (Iterator f = removedRels.iterator(); f.hasNext() && !relationExists; ) {
                        Relation r = (Relation)f.next();
                        if (r.getPrimaryKey().equals(existingPK) && r.getForeignKey().equals(newFK)) {
                            f.remove(); // don't drop it, just leave it untouched and reuse it.
                            relationExists = true;
                        }
                    }
                    if (!relationExists) {
                        // Create the relation.
                        new GenericRelation(existingPK, newFK, Cardinality.MANY);
                    }
                }
            }
            
            // Remove any relations that we don't find in the database (but leave
            // the handmade ones behind).
            for (Iterator j = removedRels.iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                if (r.getStatus().equals(ComponentStatus.HANDMADE)) continue;
                r.destroy();
                j.remove(); // just to make sure it's really gone properly
            }
        }
        
        // Drop any foreign keys that are left over (but not handmade ones).
        for (Iterator i = removedFKs.iterator(); i.hasNext(); ) {
            Key k = (Key)i.next();
            if (k.getStatus().equals(ComponentStatus.HANDMADE)) continue;
            k.destroy();
            i.remove(); // just to make sure it really has gone.
        }
        
        // Check and convert any 1:M relations that should be 1:1
        for (Iterator i = this.getTables().iterator(); i.hasNext(); ) {
            Table pkTable = (Table)i.next();
            PrimaryKey pk = pkTable.getPrimaryKey();
            if (pk == null) continue; // Skip tables without PKs.
            for (Iterator j = pk.getRelations().iterator(); j.hasNext(); ) {
                Relation rel = (Relation)j.next();
                if (!rel.getStatus().equals(ComponentStatus.INFERRED)) continue; // Skip incorrect and user-defined ones.
                ForeignKey fk = rel.getForeignKey();
                Table fkTable = fk.getTable();
                PrimaryKey fkTablePK = fkTable.getPrimaryKey();
                if (fkTablePK == null) continue; // Skip FK tables without PKs.
                // If foreign key = primary key on fkTable then cardinality should be 1:1
                if (fk.getColumns().equals(fkTablePK.getColumns())) rel.setFKCardinality(Cardinality.ONE);
            }
        }        
    }
}
