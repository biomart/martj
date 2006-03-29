/*
 * DataLink.java
 *
 * Created on 29 March 2006, 09:36
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

package org.biomart.builder.model;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import javax.sql.DataSource;

/**
 * This interface defines the methods required to connect to and test a data source.
 * It doesn't define any data source specific methods, only those that are required to
 * make the rest of the system work without worrying about where the data is coming from.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 29th March 2006
 * @since 0.1
 */
public interface DataLink {    
    /**
     * This method tests the database connection (if one is required) that will be used to
     * read or write data. It returns nothing if successful, but if an error is encountered
     * then an exception is thrown.
     * @throws SQLException if it needed to talk to a database and couldn't.
     */
    public void testConnection() throws SQLException;
    
    /**
     * Checks to see if this {@link DataLink} 'cohabits' with another one. Cohabitation means
     * that it would be possible to write a single SQL statement that could read data from
     * both {@link DataLink}s simultaneously.
     * @param partner the other {@link DataLink} to test for cohabitation. 
     * @return true if the two can cohabit, false if not.
     * @throws NullPointerException if the partner is null.
     */
    public boolean canCohabit(DataLink partner);
    
    /**
     * This inner interface defines methods required for JDBC connections only.
     */
    public interface JDBCDataLink extends DataLink {
        /**
         * Returns a JDBC {@link DataSource} connected to this database.
         * @return the {@link DataSource} for this database.
         */
        public DataSource getDataSource();
    }
    
    /**
     * <p>This inner interface defines methods required for XML files only.</p>
     * <p><b>TODO:</b> Work out how it's going to work, then define it.</p>
     */
    public interface XMLDataLink extends DataLink {
    }
}
