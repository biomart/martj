/*
 * Schema.java
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.resources.BuilderBundle;

/**
 * The {@link Schema} contains the set of all {@link TableProvider}s that are providing
 * data to this mart. It also has one or more {@link Window}s onto the {@link Table}s provided
 * by these, from which {@link DataSet}s are constructed.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.7, 25th April 2006
 * @since 0.1
 */
public class Schema {
    /**
     * Internal reference to the {@link TableProvider}s we are using for data.
     */
    private final Map tableProviders = new TreeMap();
    
    /**
     * Internal reference to the {@link Window}s onto the data we are using to
     * construct the marts with.
     */
    private final Map windows = new TreeMap();
    
    /**
     * Returns the set of {@link TableProvider} objects which this {@link Schema} includes
     * when building a mart. The set may be empty but it is never null.
     * @return a set of {@link TableProvider} objects.
     */
    public Collection getTableProviders() {
        return this.tableProviders.values();
    }
    
    /**
     * Returns the {@link TableProvider} object with the given name. If it doesn't exist, null is returned.
     * If the name was null, you'll get an exception.
     * @param name the name to look for.
     * @return a {@link TableProvider} object matching the specified name.
     * @throws NullPointerException if the name was null.
     */
    public TableProvider getTableProviderByName(String name) throws NullPointerException {
        // Sanity check.
        if (name == null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        // Do we have it?
        if (!this.tableProviders.containsKey(name)) return null;
        // Return it.
        return (TableProvider)this.tableProviders.get(name);
    }
    
    /**
     * Adds a {@link TableProvider} to the set which this {@link Schema} includes. An
     * exception is thrown if it already is in this set, or if it is null.
     * @param tableProvider the {@link TableProvider} to add.
     * @throws AlreadyExistsException if the provider is already in this schema.
     * @throws NullPointerException if the provider is null.
     */
    public void addTableProvider(TableProvider tableProvider) throws NullPointerException, AlreadyExistsException {
        // Sanity check.
        if (tableProvider == null)
            throw new NullPointerException(BuilderBundle.getString("tblprovIsNull"));
        if (this.tableProviders.containsKey(tableProvider.getName()))
            throw new AlreadyExistsException(BuilderBundle.getString("tblprovExists"),tableProvider.getName());
        // Do it.
        this.tableProviders.put(tableProvider.getName(),tableProvider);
    }
    
    /**
     * Renames a {@link TableProvider}. An
     * exception is thrown if that names has already been used, or if it is null.
     * @param tableProvider the {@link TableProvider} to rename.
     * @param name the new name for it.
     * @throws AlreadyExistsException if the provider name is already in this schema.
     * @throws AssociationException if the provider does not belong to us. 
     * @throws NullPointerException if the provider or name is null.
     */
    public void renameTableProvider(TableProvider tableProvider, String name) throws NullPointerException, AlreadyExistsException, AssociationException {
        // Sanity check.
        if (tableProvider == null)
            throw new NullPointerException(BuilderBundle.getString("tblprovIsNull"));
        if (name == null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        if (this.tableProviders.containsKey(name))
            throw new AlreadyExistsException(BuilderBundle.getString("tblprovExists"),tableProvider.getName());
        if (!this.tableProviders.values().contains(tableProvider))
            throw new AssociationException(BuilderBundle.getString("tblProvSchemaMismatch"));
        // Do it.
        this.tableProviders.remove(tableProvider.getName());
        tableProvider.setName(name);
        this.tableProviders.put(tableProvider.getName(),tableProvider);
    }
    
    /**
     * Removes a {@link TableProvider} from the set which this {@link Schema} includes. An
     * exception is thrown if it is null. If it is not found, nothing happens and it is ignored quietly.
     * Any {@link Window}s centred on this {@link TableProvider} are also removed.
     * @param tableProvider the {@link TableProvider} to remove.
     * @throws NullPointerException if the provider is null.
     */
    public void removeTableProvider(TableProvider tableProvider) throws NullPointerException {
        // Sanity check.
        if (tableProvider == null)
            throw new NullPointerException(BuilderBundle.getString("tblprovIsNull"));
        // Do we have it?
        if (!this.tableProviders.containsKey(tableProvider.getName())) return;
        // Do it.
        List windows = new ArrayList(this.getWindows());
        for (Iterator i = windows.iterator(); i.hasNext(); ) {
            Window w = (Window)i.next();
            if (w.getCentralTable().getTableProvider().equals(tableProvider)) this.removeWindow(w);
        }
        this.tableProviders.remove(tableProvider.getName());
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
     * @param name the name to look for.
     * @return a {@link Window} object matching the specified name.
     * @throws NullPointerException if the name was null.
     */
    public Window getWindowByName(String name) throws NullPointerException {
        // Sanity check.
        if (name == null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        // Do we have it?
        if (!this.windows.containsKey(name)) return null;
        // Return it.
        return (Window)this.windows.get(name);
    }
    
    /**
     * Adds a {@link Window} to the set which this {@link Schema} includes. An
     * exception is thrown if it already is in this set, or if it is null.
     * @param window the {@link Window} to add.
     * @throws AlreadyExistsException if the window is already in this schema.
     * @throws NullPointerException if the window is null.
     */
    public void addWindow(Window window) throws NullPointerException, AlreadyExistsException {
        // Sanity check.
        if (window == null)
            throw new NullPointerException(BuilderBundle.getString("windowIsNull"));
        if (this.windows.containsKey(window.getName()))
            throw new AlreadyExistsException(BuilderBundle.getString("windowExists"),window.getName());
        // Do it.
        this.windows.put(window.getName(), window);
    }
    
    /**
     * Given a particular {@link Table}, automatically create a number of {@link Window}s
     * based around that {@link Table} that represent the various possible subclassing scenarios.
     * It will always create one {@link Window} (with the name given) that
     * doesn't subclass anything. For every M:1 relation leading off the {@link Table}, another
     * {@link Window} will be created containing that subclass relation. Each subclass {@link Window}
     * choice will have a number appended to it after an underscore, eg. '_SC1' ,'_SC2' etc.
     * Each window created will have optimiseRelations() called on it automatically.
     * @param centralTable the {@link Table} to build predicted {@link Window}s around.
     * @param name the name to use for the windows.
     * @return the newly created windows, as well as adding them to the schema.
     * @throws AlreadyExistsException if a window already exists in this schema with the same
     *  name or any of the suffixed versions.
     * @throws NullPointerException if the table is null.
     */
    public void suggestWindows(Table centralTable, String name) throws NullPointerException, AlreadyExistsException {
        // Sanity check.
        if (centralTable== null)
            throw new NullPointerException(BuilderBundle.getString("tableIsNull"));
        // Do it.
        try {
            Window mainWin = new Window(this, centralTable, name);
            mainWin.optimiseRelations();
        } catch (Exception e) {
            AssertionError ae = new AssertionError(BuilderBundle.getString("plainWindowPredictionFailure"));
            ae.initCause(e);
            throw ae;
        }
        // Predict the subclass relations from the existing m:1 relations - simple guesser based
        // on finding foreign keys in the central table. Only marks the first candidate it finds, as
        // a subclassed table cannot have been subclassed off more than one parent.
        int suffix = 1;
        for (Iterator i = centralTable.getForeignKeys().iterator(); i.hasNext(); ) {
            Key k = (Key)i.next();
            for (Iterator j = k.getRelations().iterator(); j.hasNext(); ) {
                Relation r = (Relation)j.next();
                // Only flag potential m:1 subclass relations if they don't refer back to ourselves.
                try {
                    if (!r.getPrimaryKey().getTable().equals(centralTable)) {
                        Window scWin = new Window(this, centralTable, name+BuilderBundle.getString("subclassWindowSuffix")+(suffix++));
                        scWin.flagSubclassRelation(r);
                        scWin.optimiseRelations();
                    }
                } catch (Exception e) {
                    AssertionError ae = new AssertionError(BuilderBundle.getString("subclassPredictionFailure"));
                    ae.initCause(e);
                    throw ae;
                }
            }
        }
    }
    
    /**
     * Renames a {@link Window}. An
     * exception is thrown if that names has already been used, or if it is null.
     * @param window the {@link Window} to rename.
     * @param name the new name for it.
     * @throws AlreadyExistsException if the window name is already in this schema.
     * @throws AssociationException if the window does not belong to us. 
     * @throws NullPointerException if the window or name is null.
     */
    public void renameWindow(Window window, String name) throws NullPointerException, AlreadyExistsException, AssociationException {
        // Sanity check.
        if (window == null)
            throw new NullPointerException(BuilderBundle.getString("windowIsNull"));
        if (name == null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        if (this.windows.containsKey(name))
            throw new AlreadyExistsException(BuilderBundle.getString("windowExists"),window.getName());
        if (!this.windows.values().contains(window))
            throw new AssociationException(BuilderBundle.getString("windowSchemaMismatch"));
        // Do it.
        this.windows.remove(window.getName());
        window.setName(name);
        this.windows.put(window.getName(),window);
    }
    
    /**
     * Removes a {@link Window} from the set which this {@link Schema} includes. An
     * exception is thrown if it is null. If it is not found, nothing happens and it is ignored quietly.
     * @param window the {@link Window} to remove.
     * @throws NullPointerException if the window is null.
     */
    public void removeWindow(Window window) throws NullPointerException {
        // Sanity check.
        if (window == null)
            throw new NullPointerException(BuilderBundle.getString("windowIsNull"));
        // Do we have it?
        if (!this.windows.containsKey(window.getName())) return;
        // Do it.
        this.windows.remove(window.getName());
    }
    
    /**
     * Synchronise all {@link Window}s in this {@link Schema} so that they match up
     * with the {@link Schema}'s {@link TableProvider}(s). Any {@link Window}s that
     * are based on now-missing {@link Table}s are dropped. This is all simply a matter
     * of delegating calls and the routine does no real work itself.
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchroniseWindows() throws SQLException, BuilderException {
        // Then, Windows.
        for (Iterator i = this.windows.values().iterator(); i.hasNext(); ) {
            Window w = (Window)i.next();
            if (!this.getTableProviders().contains(w.getCentralTable().getTableProvider())) i.remove();
            else w.synchronise();
        }
    }
    
    /**
     * Synchronise this {@link Schema} with the {@link TableProvider}(s) that is(are)
     * providing its tables, then synchronising its {@link Window}s too. This is all simply a matter
     * of delegating calls and the routine does no real work itself.
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchroniseTableProviders() throws SQLException, BuilderException {
        // TableProviders first
        for (Iterator i = this.tableProviders.values().iterator(); i.hasNext(); ) {
            TableProvider tp = (TableProvider)i.next();
            tp.synchronise();
        }
        // Then, synchronise windows.
        this.synchroniseWindows();
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
