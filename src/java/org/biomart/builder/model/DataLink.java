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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import org.biomart.builder.exceptions.AssociationException;

/**
 * This interface defines the methods required to connect to and test a data source.
 * It doesn't define any data source specific methods, only those that are required to
 * make the rest of the system work without worrying about where the data is coming from.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 3rd April 2006
 * @since 0.1
 */
public interface DataLink {
    /**
     * Checks to see if this {@link DataLink} 'cohabits' with another one. Cohabitation means
     * that it would be possible to write a single SQL statement that could read data from
     * both {@link DataLink}s simultaneously.
     * @param partner the other {@link DataLink} to test for cohabitation.
     * @return true if the two can cohabit, false if not.
     * @throws NullPointerException if the partner is null.
     */
    public boolean canCohabit(DataLink partner) throws NullPointerException;
    
    /**
     * Checks to see if this {@link DataLink} is working properly.
     * Returns true if it is, otherwise throws an exception describing the problem. 
     * Should never return false.
     * @return true if the link is working. Should never return false, as an exception
     * will always be thrown if there is a problem.
     * @throws Exception if there is a problem.
     */
    public boolean test() throws Exception;
    
    /**
     * This inner interface defines methods required for JDBC connections only.
     */
    public interface JDBCDataLink extends DataLink {
        /**
         * Returns a JDBC {@link Connection} connected to this database
         * using the data supplied to all the other methods in this interface.
         * @return the {@link Connection} for this database.
         * @throws AssociationException if there was any problem finding the class.
         * @throws SQLException if there was any problem connecting.
         */
        public Connection getConnection() throws AssociationException, SQLException;
        
        /**
         * Getter for property driverClassName.
         * @return Value of property driverClassName.
         */
        public String getDriverClassName();
        
        /**
         * Setter for property driverClassName.
         * @param driverClassName New value of property driverClassName.
         * @throws NullPointerException if the value is null.
         */
        public void setDriverClassName(String driverClassName) throws NullPointerException;
        
        /**
         * Getter for property driverClassLocation.
         * @return Value of property driverClassLocation.
         */
        public File getDriverClassLocation();
        
        /**
         * Setter for property driverClassLocation.
         * @param driverClassLocation New value of property driverClassLocation.
         */
        public void setDriverClassLocation(File driverClassLocation);
        
        /**
         * Getter for property url.
         * @return Value of property url.
         */
        public String getJDBCURL();
        
        /**
         * Setter for property url.
         * @param url New value of property url.
         * @throws NullPointerException if the value is null.
         */
        public void setJDBCURL(String url) throws NullPointerException;
        
        /**
         * Getter for property username.
         * @return Value of property username.
         */
        public String getUsername();
        
        /**
         * Setter for property username.
         * @param username New value of property username.
         * @throws NullPointerException if the value is null.
         */
        public void setUsername(String username) throws NullPointerException;
        
        /**
         * Getter for property password.
         * @return Value of property password.
         */
        public String getPassword();
        
        /**
         * Setter for property password. May be null.
         * @param password New value of property password.
         */
        public void setPassword(String password);
    }
    
    /**
     * <p>This inner interface defines methods required for XML files only.</p>
     * <p><b>TODO:</b> Work out how it's going to work, then define it.</p>
     */
    public interface XMLDataLink extends DataLink {
    }
}
