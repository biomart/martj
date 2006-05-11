/*
 * Mart.java
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.BuilderBundle;

/**
 * The {@link Mart} contains the set of all {@link Schema}s that are providing
 * data to this mart. It also has one or more {@link DataSet}s onto the {@link Table}s provided
 * by these, from which {@link DataSet}s are constructed.
 *
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.9, 11th May 2006
 * @since 0.1
 */
public class Mart {
    /**
     * Internal reference to the {@link Schema}s we are using for data.
     */
    private final Map schemas = new LinkedHashMap();
    
    /**
     * Internal reference to the {@link DataSet}s onto the data we are using to
     * construct the marts with.
     */
    private final Map datasets = new LinkedHashMap();
    
    /**
     * Returns the set of {@link Schema} objects which this {@link Mart} includes
     * when building a mart. The set may be empty but it is never null.
     *
     * @return a set of {@link Schema} objects.
     */
    public Collection getSchemas() {
        return this.schemas.values();
    }
    
    /**
     * Returns the {@link Schema} object with the given name. If it doesn't exist, null is returned.
     * If the name was null, you'll get an exception.
     *
     * @param name the name to look for.
     * @return a {@link TSchema object matching the specified name.
     */
    public Schema getSchemaByName(String name) {
        // Do we have it?
        if (!this.schemas.containsKey(name)) return null;
        // Return it.
        return (Schema)this.schemas.get(name);
    }
    
    /**
     * Adds a {@link Schema} to the set which this {@link Mart} includes. An
     * exception is thrown if it already is in this set, or if it is null.
     *
     *
     * @param schema the {@link Schema} to add.
     * @throws AlreadyExistsException if the provider is already in this schema.
     */
    public void addSchema(Schema schema) throws AlreadyExistsException {
        // Sanity check.
        if (this.schemas.containsKey(schema.getName()))
            throw new AlreadyExistsException(BuilderBundle.getString("schemaExists"),schema.getName());
        // Do it.
        this.schemas.put(schema.getName(),schema);
    }
    
    /**
     * Renames a {@link Schema}. An
     * exception is thrown if that names has already been used, or if it is null.
     *
     *
     * @param schema the {@link Schema} to rename.
     * @param name the new name for it.
     * @throws AlreadyExistsException if the provider name is already in this schema.
     * @throws AssociationException if the provider does not belong to us.
     */
    public void renameSchema(Schema schema, String name) throws AlreadyExistsException, AssociationException {
        // Sanity check.
        if (this.schemas.containsKey(name))
            throw new AlreadyExistsException(BuilderBundle.getString("schemaExists"),schema.getName());
        if (!this.schemas.values().contains(schema))
            throw new AssociationException(BuilderBundle.getString("schemaMartMismatch"));
        // Do it.
        this.schemas.remove(schema.getName());
        schema.setName(name);
        this.schemas.put(schema.getName(),schema);
    }
    
    /**
     * Removes a {@link Schema} from the set which this {@link Mart} includes. An
     * exception is thrown if it is null. If it is not found, nothing happens and it is ignored quietly.
     * Any {@link DataSet}s centred on this {@link Schema} are also removed.
     *
     * @param schema the {@link Schema} to remove.
     */
    public void removeSchema(Schema schema) {
        // Do we have it?
        if (!this.schemas.containsKey(schema.getName())) return;
        // Do it.
        List datasets = new ArrayList(this.getDataSets());
        for (Iterator i = datasets.iterator(); i.hasNext(); ) {
            DataSet ds = (DataSet)i.next();
            if (ds.getCentralTable().getSchema().equals(schema)) this.removeDataSet(ds);
        }
        this.schemas.remove(schema.getName());
    }
    
    /**
     * Returns the set of {@link DataSet} objects which this {@link Mart} includes
     * when building a mart. The set may be empty but it is never null.
     * @return a set of {@link DataSet} objects.
     */
    public Collection getDataSets() {
        return this.datasets.values();
    }
    
    /**
     * Returns the {@link DataSet} object with the given name. If it doesn't exist, null is returned.
     * If the name was null, you'll get an exception.
     *
     * @param name the name to look for.
     * @return a {@link WDataSet object matching the specified name.
     */
    public DataSet getDataSetByName(String name) {
        // Do we have it?
        if (!this.datasets.containsKey(name)) return null;
        // Return it.
        return (DataSet)this.datasets.get(name);
    }
    
    /**
     * Adds a {@link DataSet} to the set which this {@link Mart} includes. An
     * exception is thrown if it already is in this set, or if it is null.
     * @param dataset the {@link DataSet} to add.
     * @throws AlreadyExistsException if the dataset is already in this schema.
     */
    public void addDataSet(DataSet dataset) throws AlreadyExistsException {
        // Sanity check.
        if (this.datasets.containsKey(dataset.getName()))
            throw new AlreadyExistsException(BuilderBundle.getString("datasetExists"),dataset.getName());
        // Do it.
        this.datasets.put(dataset.getName(), dataset);
    }
    
    /**
     * Given a particular {@link Table}, automatically create a number of {@link DataSet}s
     * based around that {@link Table} that represent the various possible subclassing scenarios.
     * It will always create one {@link DataSet} (with the name given) that
     * doesn't subclass anything. For every M:1 relation leading off the {@link Table}, another
     * {@link DataSet} will be created containing that subclass relation. Each subclass {@link DataSet}
     * choice will have a number appended to it after an underscore, eg. '_SC1' ,'_SC2' etc.
     * Each window created will have optimiseDataSet() called on it automatically.
     *
     *
     * @param centralTable the {@link Table} to build predicted {@link DataSet}s around.
     * @param name the name to use for the datasets.
     * @return the newly created datasets, as well as adding them to the schema.
     * @throws AlreadyExistsException if a window already exists in this schema with the same
     *  name or any of the suffixed versions.
     */
    public Collection suggestDataSets(Table centralTable, String name) throws AlreadyExistsException {
        List newDataSets = new ArrayList();
        // Do it.
        try {
            DataSet mainWin = new DataSet(this, centralTable, name);
            mainWin.optimiseDataSet();
            newDataSets.add(mainWin);
        } catch (Exception e) {
            AssertionError ae = new AssertionError(BuilderBundle.getString("plainDataSetPredictionFailure"));
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
                    if (r.getFKCardinality().equals(Cardinality.MANY) && !r.getPrimaryKey().getTable().equals(centralTable)) {
                        DataSet scWin = new DataSet(this, centralTable, name+BuilderBundle.getString("subclassDataSetSuffix")+(suffix++));
                        scWin.flagSubclassRelation(r);
                        scWin.optimiseDataSet();
                        newDataSets.add(scWin);
                    }
                } catch (Exception e) {
                    AssertionError ae = new AssertionError(BuilderBundle.getString("subclassPredictionFailure"));
                    ae.initCause(e);
                    throw ae;
                }
            }
        }
        return newDataSets;
    }
    
    /**
     * Renames a {@link DataSet}. An
     * exception is thrown if that names has already been used, or if it is null.
     *
     *
     * @param dataset the {@link DataSet} to rename.
     * @param name the new name for it.
     * @throws AlreadyExistsException if the dataset name is already in this schema.
     * @throws AssociationException if the dataset does not belong to us.
     */
    public void renameDataSet(DataSet dataset, String name) throws AlreadyExistsException, AssociationException {
        // Sanity check.
        if (this.datasets.containsKey(name))
            throw new AlreadyExistsException(BuilderBundle.getString("datasetExists"),dataset.getName());
        if (!this.datasets.values().contains(dataset))
            throw new AssociationException(BuilderBundle.getString("datasetMartMismatch"));
        // Do it.
        this.datasets.remove(dataset.getName());
        dataset.setName(name);
        this.datasets.put(dataset.getName(),dataset);
    }
    
    /**
     * Removes a {@link DataSet} from the set which this {@link Mart} includes. An
     * exception is thrown if it is null. If it is not found, nothing happens and it is ignored quietly.
     *
     * @param dataset the {@link DataSet} to remove.
     */
    public void removeDataSet(DataSet dataset) {
        // Do we have it?
        if (!this.datasets.containsKey(dataset.getName())) return;
        // Do it.
        this.datasets.remove(dataset.getName());
    }
    
    /**
     * Synchronise all {@link DataSet}s in this {@link Mart} so that they match up
     * with the {@link Mart}'s {@link Schema}(s). Any {@link DataSet}s that
     * are based on now-missing {@link Table}s are dropped. This is all simply a matter
     * of delegating calls and the routine does no real work itself.
     *
     *
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchroniseDataSets() throws SQLException, BuilderException {
        // Then, Windows.
        for (Iterator i = this.datasets.values().iterator(); i.hasNext(); ) {
            DataSet ds = (DataSet)i.next();
            if (!this.getSchemas().contains(ds.getCentralTable().getSchema())) i.remove();
            else ds.synchronise();
        }
    }
    
    /**
     * Synchronise this {@link Mart} with the {@link Schema}(s) that is(are)
     * providing its tables, then synchronising its {@link DataSet}s too. This is all simply a matter
     * of delegating calls and the routine does no real work itself.
     *
     *
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void synchroniseSchemas() throws SQLException, BuilderException {
        // TableProviders first
        for (Iterator i = this.schemas.values().iterator(); i.hasNext(); ) {
            Schema s = (Schema)i.next();
            s.synchronise();
        }
        // Then, synchronise datasets.
        this.synchroniseDataSets();
    }
    
    /**
     * Request that all the marts in all the datasets be constructed now.
     *
     * @throws SQLException if there was any data source error during
     * mart construction.
     * @throws BuilderException if there was any other kind of error in the
     * mart construction process.
     */
    public void constructMart() throws BuilderException, SQLException {
        for (Iterator i = this.datasets.values().iterator(); i.hasNext(); ) {
            DataSet ds = (DataSet)i.next();
            ds.constructMart();
        }
    }
}
