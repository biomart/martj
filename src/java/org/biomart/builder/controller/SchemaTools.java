/*
 * SchemaTools.java
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
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.model.Window;

/**
 * Tools for working with the schema from a GUI or CLI.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 26th April 2006
 * @since 0.1
 */
public class SchemaTools {
    
    private SchemaTools() {}
    
    public static void synchroniseSchemaTableProviders(Schema schema) throws SQLException, BuilderException {
        schema.synchroniseTableProviders();
    }
    
    public static void synchroniseSchemaWindows(Schema schema) throws SQLException, BuilderException {
        schema.synchroniseWindows();
    }
    
    public static void removeWindowFromSchema(Schema schema, Window window) {
        schema.removeWindow(window);
    }
    
    public static void renameWindow(Schema schema, Window window, String newName) throws AlreadyExistsException, AssociationException {
        schema.renameWindow(window, newName);
    }
    
    public static Window createWindow(Schema schema, Table table, String name) throws AssociationException, AlreadyExistsException {
        return new Window(schema, table, name);
    }
    
    public static void suggestWindows(Schema schema, Table table, String name) throws AlreadyExistsException {
        schema.suggestWindows(table, name);
    }
    
    public static void optimiseWindow(Window window) throws SQLException, BuilderException {
        window.optimiseRelations();
    }
    
    public static void addTableProviderToSchema(Schema schema, TableProvider tableProvider) throws AlreadyExistsException {
        schema.addTableProvider(tableProvider);
    }
    
    public static void removeTableProviderFromSchema(Schema schema, TableProvider tableProvider) {
        schema.removeTableProvider(tableProvider);
    }
    
    public static void renameTableProvider(Schema schema, TableProvider tableProvider, String newName) throws AlreadyExistsException, AssociationException {
        schema.renameTableProvider(tableProvider, newName);
    }
    
    public static void synchroniseTableProvider(TableProvider tableProvider) throws SQLException, BuilderException {
        tableProvider.synchronise();
    }
    
    public static boolean testTableProvider(TableProvider tableProvider) throws Exception {
        return tableProvider.test();
    }
    
    public static TableProvider createJDBCTableProvider(File driverClassLocation, String driverClassName, String url, String username, String password, String name) throws NullPointerException {
        if (password != null && password.equals("")) password = null;
        return new JDBCTableProvider(driverClassLocation, driverClassName, url, username, password, name);
    }
    
    public static TableProvider createJDBCKeyGuessingTableProvider(File driverClassLocation, String driverClassName, String url, String username, String password, String name) throws NullPointerException {
        if (password != null && password.equals("")) password = null;
        return new JDBCKeyGuessingTableProvider(driverClassLocation, driverClassName, url, username, password, name);
    }
}
