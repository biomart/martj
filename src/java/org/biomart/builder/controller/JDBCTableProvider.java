/*
 * JDBCTableProvider.java
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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.ComponentStatus;
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
import org.biomart.builder.resources.BuilderBundle;

/**
 * The plain JDBC {@link TableProvider} implementation loads tables from a
 * database at construction time, along with all columns, keys and relations between them that
 * are specified in the database's {@link DatabaseMetaData} structures. Note that if a database
 * is incapable of enforcing foreign keys etc., then you should use the key-guessing version
 * of this class instead, {@link JDBCKeyGuessingTableProvider}, eg. for MyISAM tables in MySQL.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 18th April 2006
 * @since 0.1
 */
public class JDBCTableProvider extends JDBCKeyGuessingTableProvider {
    /**
     * Creates a new instance of JDBCTableProvider based around
     * the given JDBC Connection. As this is identical to JDBCKeyGuessingTableProvider,
     * it delegates upwards.
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
    public JDBCTableProvider(File driverClassLocation, String driverClassName, String url, String username, String password, String name) throws NullPointerException {
        super(driverClassLocation, driverClassName, url, username, password, name);
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
                removedCols.remove(dbTblCol); // Stop it from being dropped.
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
            
            // Did we find a PK?
            PrimaryKey existingPK = existingTable.getPrimaryKey();
            if (!pkCols.isEmpty()) {
                // Create and set the primary key (only if existing one is not the same).
                if (existingPK == null || !existingPK.getColumns().equals(pkCols)) {
                    existingTable.setPrimaryKey(new GenericPrimaryKey(new ArrayList(pkCols.values())));
                }
            } else {
                // Remove the primary key on this table, but only if the existing one is not handmade.
                if (existingPK!=null && !existingPK.getStatus().equals(ComponentStatus.HANDMADE)) {
                    existingTable.setPrimaryKey(null);
                }
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
            
            // Load relations from db referring to this primary key.
            TreeMap dbFKs = new TreeMap();
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
                    
                    ForeignKey newFK = new GenericForeignKey(Arrays.asList(candidateFKColumns));
                    boolean newFKAlreadyExists = false;
                    // If we've already got one like that, reuse it, otherwise add it.
                    for (Iterator f = newFK.getTable().getForeignKeys().iterator(); f.hasNext() && !newFKAlreadyExists; ) {
                        ForeignKey candidateFK = (ForeignKey)f.next();
                        if (candidateFK.equals(newFK)) {
                            // Found one. Reuse it!
                            newFK = candidateFK;
                            removedFKs.remove(candidateFK); // don't drop it any more.
                            newFKAlreadyExists =true;
                        }
                    }
                    
                    if (!newFKAlreadyExists) {
                        // If its new, go ahead and make the relation.
                        newFK.getTable().addForeignKey(newFK);
                        new GenericRelation(existingPK, newFK, Cardinality.MANY);
                    } else {
                        // If its not new, check to see if it already has a relation.
                        
                        // Check to see if this FK already has a link to this PK.
                        // If it has a correctly inferred connection to any other PK,
                        // complain bitterly. If it has an incorrect or handmade connection
                        // to any other PK, remove that one.
                        boolean relationExists = false;
                        for (Iterator f = newFK.getRelations().iterator(); f.hasNext() && !relationExists; ) {
                            Relation r = (Relation)f.next();
                            if (r.getPrimaryKey().equals(existingPK)) {
                                removedRels.remove(r); // don't drop it, just leave it untouched and reuse it.
                                relationExists = true;
                            } else if (r.getStatus().equals(ComponentStatus.INFERRED)) {
                                throw new AssertionError(BuilderBundle.getString("fkHasMultiplePKs"));
                            } else {
                                // It'll get removed later if we leave it in removedRels.
                                // To do that, we need do nothing.
                            }
                        }
                        
                        // If checks returned false, create a new relation.
                        if (!relationExists) new GenericRelation(existingPK, newFK, Cardinality.MANY);
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
