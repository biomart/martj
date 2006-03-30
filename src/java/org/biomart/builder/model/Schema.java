/*
 * Schema.java
 *
 * Created on 27 March 2006, 12:54
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.BuilderException;

/**
 * The {@link Schema} contains the set of all {@link TableProvider}s that are providing
 * data to this mart. It also has one or more {@link Window}s onto the {@link Table}s provided
 * by these, from which {@link DataSet}s are constructed. 
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 30th March 2006
 * @since 0.1
 */
public class Schema {
    /**
     * Internal reference to the {@link TableProvider}s we are using for data.
     */
    private final Map tblProvs = new HashMap();
    
    /**
     * Internal reference to the {@link Window}s onto the data we are using to
     * construct the marts with.
     */
    private final Map windows = new HashMap();
        
    /**
     * Returns the set of {@link TableProvider} objects which this {@link Schema} includes
     * when building a mart. The set may be empty but it is never null. 
     * @return a set of {@link TableProvider} objects.
     */
    public Collection getTableProviders() {
        return this.tblProvs.values();
    }
        
    /**
     * Returns the {@link TableProvider} object with the given name. If it doesn't exist, null is returned.
     * If the name was null, you'll get an exception.
     * @return a {@link TableProvider} object matching the specified name.
     * @throws NullPointerException if the name was null.
     */
    public TableProvider getTableProviderByName(String name) throws NullPointerException {
        // Sanity check.
        if (name==null)
            throw new NullPointerException("Table provider name cannot be null.");
        // Do we have it?
        if (!this.tblProvs.containsKey(name)) return null;
        // Return it.
        return (TableProvider)this.tblProvs.get(name);
    }
    
    /**
     * Adds a {@link TableProvider} to the set which this {@link Schema} includes. An 
     * exception is thrown if it already is in this set, or if it is null.
     * @param tp the {@link TableProvider} to add.
     * @throws AlreadyExistsException if the provider is already in this schema.
     * @throws NullPointerException if the provider is null.
     */
    public void addTableProvider(TableProvider tp) throws NullPointerException, AlreadyExistsException {
        // Sanity check.
        if (tp==null)
            throw new NullPointerException("Table provider cannot be null.");
        if (this.tblProvs.containsKey(tp.getName()))
            throw new AlreadyExistsException("Table provider has already been added to this schema.",tp.getName());
        // Do it.
        this.tblProvs.put(tp.getName(),tp);
    }

    /**
     * Removes a {@link TableProvider} from the set which this {@link Schema} includes. An 
     * exception is thrown if it is null. If it is not found, nothing happens and it is ignored quietly.
     * @param tp the {@link TableProvider} to remove.
     * @throws NullPointerException if the provider is null.
     */
    public void removeTableProvider(TableProvider tp) throws NullPointerException {
        // Sanity check.
        if (tp==null)
            throw new NullPointerException("Table provider cannot be null.");
        // Do we have it?
        if (!this.tblProvs.containsKey(tp.getName())) return;
        // Do it.
        this.tblProvs.remove(tp.getName());
    }
        
    /**
     * Returns the set of {@link Window} objects which this {@link Schema} includes
     * when building a mart. The set may be empty but it is never null.
     * @return a set of {@link Window} objects.
     */
    public Collection getWindows() {
        return this.windows.values();
    }
        
    /**
     * Returns the {@link Window} object with the given name. If it doesn't exist, null is returned.
     * If the name was null, you'll get an exception.
     * @return a {@link Window} object matching the specified name.
     * @throws NullPointerException if the name was null.
     */
    public Window getWindowByName(String name) throws NullPointerException {
        // Sanity check.
        if (name==null)
            throw new NullPointerException("Window name cannot be null.");
        // Do we have it?
        if (!this.windows.containsKey(name)) return null;
        // Return it.
        return (Window)this.windows.get(name);
    }
    
    /**
     * Adds a {@link Window} to the set which this {@link Schema} includes. An 
     * exception is thrown if it already is in this set, or if it is null.
     * @param w the {@link Window} to add.
     * @throws AlreadyExistsException if the window is already in this schema.
     * @throws NullPointerException if the window is null.
     */
    public void addWindow(Window w) throws NullPointerException, AlreadyExistsException {
        // Sanity check.
        if (w==null)
            throw new NullPointerException("Window cannot be null.");
        if (this.windows.containsKey(w.getName()))
            throw new AlreadyExistsException("Window has already been added to this schema.",w.getName());
        // Do it.
        this.windows.put(w.getName(), w);
    }

    /**
     * Removes a {@link Window} from the set which this {@link Schema} includes. An 
     * exception is thrown if it is null. If it is not found, nothing happens and it is ignored quietly.
     * @param w the {@link Window} to remove.
     * @throws NullPointerException if the window is null.
     */
    public void removeWindow(Window w) throws NullPointerException {
        // Sanity check.
        if (w==null)
            throw new NullPointerException("Window cannot be null.");
        // Do we have it?
        if (!this.windows.containsKey(w.getName())) return;
        // Do it.
        this.windows.remove(w);
    }
    
    /**
     * Synchronise this {@link Schema} with the {@link TableProvider}(s) that is(are)
     * providing its tables, then synchronising its {@link Window}s too. This is all simply a matter
     * of delegating calls and the routine does no real work itself.
     *
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchronise() throws SQLException, BuilderException {
        // TableProviders first
        for (Iterator i = this.tblProvs.values().iterator(); i.hasNext(); ) {
            TableProvider tp = (TableProvider)i.next();
            tp.synchronise();
        }
        // Then, Windows.
        for (Iterator i = this.windows.values().iterator(); i.hasNext(); ) {
            Window w = (Window)i.next();
            w.synchronise();
        }
    }
    
    /**
     * Request that all the marts in all the windows be constructed now.
     * @throws SQLException if there was any data source error during
     * mart construction.
     * @throws BuilderException if there was any other kind of error in the
     * mart construction process.
     */
    public void constructMarts() throws BuilderException, SQLException {
        for (Iterator i = this.windows.values().iterator(); i.hasNext(); ) {
            Window w = (Window)i.next();
            w.constructMart();
        }
    }
}
