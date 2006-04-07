/*
 * NaiveCLI.java
 *
 * Created on 03 April 2006, 12:49
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

package org.biomart.builder.view;

import java.io.File;
import java.util.Collections;
import org.biomart.builder.controller.JDBCKeyGuessingTableProvider;
import org.biomart.builder.controller.JDBCTableProvider;
import org.biomart.builder.controller.SchemaSaver;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.model.Window;

/**
 * Performs a naive attempt at converting a schema into a dataset
 * based around the named table. Currently generates only schema XML,
 * and does not generate any SQL to do the work.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 5th April 2006
 * @since 0.1
 */
public class NaiveCLI {
    /**
     * Creates a new instance of NaiveCLI. Does nothing.
     */
    public NaiveCLI() {
    }
    
    /**
     * Does the work of loading up the table provider, creating a schema
     * based around it and a window focused on the named table, then
     * generating the dataset and the XML output file.
     *
     * @param tableProvider the {@link TableProvider} to load tables from.
     * @param name the name of the table to build the dataset around.
     * @param file the output file to write the XML to.
     * @throws Exception all kinds of weird and wonderful problems may occur.
     */
    public void execute(TableProvider tableProvider, String name, File file) throws Exception {
        Schema s = new Schema();
        s.addTableProvider(tableProvider);
        s.synchronise(); // causes the table provider to load up its info
        Table t = tableProvider.getTableByName(name);
        // Predict some sensible defaults.
        s.suggestWindows(t);
        // Accept the no-subclass default.
        Window w = s.getWindowByName(t.getName());
        w.synchronise(); // causes the dataset to regenerate
        // Remove the others.
        s.getWindows().retainAll(Collections.singleton(w));
        // Dump the XML.
        SchemaSaver.save(s, file);
        // Replace the saver line with a call to setMartConstructor() and
        // then call constructMart() to make SQL instead.
    }
    
    /**
     * Main method loads the JDBC driver from the first parameter,
     * the JDBC connection string from the second parameter, constructs and
     * sets up a JDBCTableProvider using the username and password in the
     * 3rd and 4th parameters, then uses it to generate a dataset
     * based around the table named in the fifth parameter. The resulting
     * schema XML is printed out to the file named in the sixth parameter.
     * The seventh parameter, if present and equal to 'NONRELATIONAL' will
     * attempt to guess relations from column names instead of using database
     * metadata.
     * If your database does not require a password, use the empty string for
     * the password and this will be converted to a null password internally.
     * 
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        if (args.length<6) {
            System.err.println("usage: java org.biomart.builder.view.NaiveCLI <driver class> <connection URL> <username> <password> <table name> <output file> [NONRELATIONAL]");
            return;
        }
        try {
            String classname = args[0];
            String url = args[1];
            String username = args[2];
            String password = args[3];
            String tableName = args[4];
            File file = new File(args[5]);
            if (password.equals("")) password = null;
            TableProvider tp;
            if (args.length>6 && args[6].equals("keyguessing"))
                tp = new JDBCKeyGuessingTableProvider(null, classname, url, username, password, "naive");
            else
                tp = new JDBCTableProvider(null, classname, url, username, password, "naive");
            (new NaiveCLI()).execute(tp, tableName, file);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
    
}
