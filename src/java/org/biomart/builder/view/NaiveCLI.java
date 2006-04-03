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
import java.sql.DriverManager;
import org.biomart.builder.controller.JDBCDMDTableProvider;
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
 * @version 0.1.1, 3rd April 2006
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
     * @param tp the {@link TableProvider} to load tables from.
     * @param table the name of the table to build the dataset around.
     * @param file the output file to write the XML to.
     * @throws Exception all kinds of weird and wonderful problems may occur.
     */
    public void execute(TableProvider tp, String name, File file) throws Exception {
        Schema s = new Schema();
        s.addTableProvider(tp);
        s.synchronise(); // causes the table provider to load up its info
        Table t = tp.getTableByName(name);
        Window w = new Window(s, t, t.getName());
        w.synchronise(); // causes the dataset to regenerate
        SchemaSaver.save(s, file);
        // Replace the saver line with a call to setMartConstructor() and 
        // then call constructMart() to make SQL instead.
    }
    
    /**
     * Main method loads the JDBC driver from the first parameter,
     * the JDBC connection string from the second parameter, constructs and
     * sets up a JDBCDMDTableProvider, then uses it to generate a dataset
     * based around the table named in the third parameter. The resulting
     * schema XML is printed out to the file named in the fourth parameter.
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        if (args.length<4) {
            System.err.println("usage: java org.biomart.builder.view.NaiveCLI <driver class> <connection string> <table name> <output file>");
            return;
        }
        try {
            Class.forName(args[0]);
            TableProvider tp = new JDBCDMDTableProvider(DriverManager.getConnection(args[1]), "naive");
            String tableName = args[2];
            File file = new File(args[3]);
            (new NaiveCLI()).execute(tp, tableName, file);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
    
}
