/*
 * MartUtils.java
 *
 * Created on 26 April 2006, 09:13
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
import java.sql.SQLException;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.SchemaGroup.GenericSchemaGroup;

/**
 * Tools for working with the mart from a GUI or CLI.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 26th April 2006
 * @since 0.1
 */
public class MartUtils {
    
    private MartUtils() {}
    
    public static void synchroniseMartSchemas(Mart mart) throws SQLException, BuilderException {
        mart.synchroniseSchemas();
    }
    
    public static void synchroniseMartDataSets(Mart mart) throws SQLException, BuilderException {
        mart.synchroniseDataSets();
    }
    
    public static void removeDataSetFromSchema(Mart mart, DataSet dataset) {
        mart.removeDataSet(dataset);
    }
    
    public static void renameDataSet(Mart mart, DataSet dataset, String newName) throws AlreadyExistsException, AssociationException {
        mart.renameDataSet(dataset, newName);
    }
    
    public static void createDataSet(Mart mart, Table table, String name) throws AssociationException, AlreadyExistsException {
        new DataSet(mart, table, name);
    }
    
    public static void suggestDataSets(Mart mart, Table table, String name) throws AlreadyExistsException {
        mart.suggestDataSets(table, name);
    }
    
    public static void optimiseDataSet(DataSet dataset) throws SQLException, BuilderException {
        dataset.optimiseDataSet();
    }
    
    public static void addSchemaToMart(Mart mart, Schema schema) throws AlreadyExistsException {
        mart.addSchema(schema);
    }
    
    public static void removeSchemaFromMart(Mart mart, Schema schema) {
        mart.removeSchema(schema);
    }
    
    public static void renameSchema(Mart mart, Schema schema, String newName) throws AlreadyExistsException, AssociationException {
        mart.renameSchema(schema, newName);
    }
    
    public static void synchroniseSchema(Schema schema) throws SQLException, BuilderException {
        schema.synchronise();
    }
    
    public static boolean testSchema(Schema schema) throws Exception {
        return schema.test();
    }
    
    public static Schema createJDBCSchema(File driverClassLocation, String driverClassName, String url, String username, String password, String name, boolean keyGuessing) {
        if (password != null && password.equals("")) password = null;
        return new JDBCSchema(driverClassLocation, driverClassName, url, username, password, name, keyGuessing);
    }

    public static SchemaGroup addSchemaToSchemaGroup(Mart mart, Schema schema, String groupName) throws BuilderException, SQLException {
        Schema group = mart.getSchemaByName(groupName);
        if (group == null || !(group instanceof SchemaGroup)) {            
            group = new GenericSchemaGroup(groupName);
            mart.addSchema(group);
        }
        ((SchemaGroup)group).addSchema(schema);
        group.synchronise();
        mart.removeSchema(schema);
        return (SchemaGroup)group;
    }
}
