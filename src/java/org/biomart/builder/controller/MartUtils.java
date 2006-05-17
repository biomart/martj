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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.SchemaGroup.GenericSchemaGroup;
import org.biomart.builder.resources.BuilderBundle;

/**
 * Tools for working with the mart from a GUI or CLI.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.10, 17th May 2006
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
    
    public static DataSet createDataSet(Mart mart, Table table, String name) throws AssociationException, AlreadyExistsException, SQLException, BuilderException {
        DataSet dataset = new DataSet(mart, table, name);
        dataset.synchronise();
        return dataset;
    }
    
    public static Collection suggestDataSets(Mart mart, Table table, String name) throws AlreadyExistsException {
        return mart.suggestDataSets(table, name);
    }
    
    public static void optimiseDataSet(DataSet dataset) throws SQLException, BuilderException {
        dataset.optimiseDataSet();
        dataset.synchronise();
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
    
    public static void renameSchemaInSchemaGroup(Schema schema, String newName) throws AlreadyExistsException, AssociationException {
        schema.setName(newName);
    }
    
    public static void synchroniseSchema(Schema schema) throws SQLException, BuilderException {
        schema.synchronise();
    }
    
    public static boolean testSchema(Schema schema) throws Exception {
        return schema.test();
    }
    
    public static JDBCSchema createJDBCSchema(File driverClassLocation, String driverClassName, String url, String username, String password, String name, boolean keyGuessing) {
        if (password != null && password.equals("")) password = null;
        return new JDBCSchema(driverClassLocation, driverClassName, url, username, password, name, keyGuessing);
    }
    
    public static SchemaGroup addSchemaToSchemaGroup(Mart mart, Schema schema, String groupName) throws BuilderException, SQLException {
        Schema schemaGroup = mart.getSchemaByName(groupName);
        if (schemaGroup == null || !(schemaGroup instanceof SchemaGroup)) {
            schemaGroup = new GenericSchemaGroup(groupName);
            mart.addSchema(schemaGroup);
        }
        ((SchemaGroup)schemaGroup).addSchema(schema);
        schemaGroup.synchronise();
        mart.removeSchema(schema);
        return (SchemaGroup)schemaGroup;
    }
    
    public static void removeSchemaFromSchemaGroup(Mart mart, Schema schema, SchemaGroup schemaGroup) throws BuilderException, SQLException {
        schemaGroup.removeSchema(schema);
        if (schemaGroup.getSchemas().size()==0) mart.removeSchema(schemaGroup);
        else schemaGroup.synchronise();
        mart.addSchema(schema);
    }
    
    public static void changeRelationCardinality(Mart mart, Relation relation, Cardinality cardinality) throws SQLException, BuilderException {
        relation.setFKCardinality(cardinality);
        // If 1:1, make sure it isn't used as a subclass or concat-only relation in any dataset.
        if (cardinality.equals(Cardinality.ONE)) for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            DataSet ds = (DataSet)i.next();
            ds.unflagSubclassRelation(relation);
            ds.unflagConcatOnlyRelation(relation);
            ds.synchronise();
        }
    }
    
    public static void removeRelation(Mart mart, Relation relation) throws SQLException, BuilderException {
        relation.destroy();
        for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            ((DataSet)i.next()).synchronise();
        }
    }
    
    public static void maskRelation(DataSet dataset, Relation relation) throws SQLException, BuilderException {
        dataset.maskRelation(relation);
        dataset.synchronise();
    }
    
    public static void unmaskRelation(DataSet dataset, Relation relation) throws SQLException, BuilderException {
        dataset.unmaskRelation(relation);
        dataset.synchronise();
    }
    
    public static void subclassRelation(DataSet dataset, Relation relation) throws AssociationException, SQLException, BuilderException {
        dataset.flagSubclassRelation(relation);
        dataset.unflagConcatOnlyRelation(relation);
        dataset.synchronise();
    }
    
    public static void unsubclassRelation(DataSet dataset, Relation relation) throws SQLException, BuilderException {
        dataset.unflagSubclassRelation(relation);
        dataset.synchronise();
    }
    
    public static void concatOnlyRelation(DataSet dataset, Relation relation, ConcatRelationType type) throws SQLException, BuilderException {
        dataset.flagConcatOnlyRelation(relation, type);
        dataset.unflagSubclassRelation(relation);
        dataset.synchronise();
    }
    
    public static void unconcatOnlyRelation(DataSet dataset, Relation relation) throws SQLException, BuilderException {
        dataset.unflagConcatOnlyRelation(relation);
        dataset.synchronise();
    }
    
    public static void changeRelationStatus(Mart mart, Relation relation, ComponentStatus status) throws SQLException, BuilderException {
        relation.setStatus(status);
        for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            ((DataSet)i.next()).synchronise();
        }
    }
    
    public static void renameDataSetColumn(DataSetColumn col, String newName) throws AlreadyExistsException {
        col.setName(newName);
    }
    
    public static void maskColumn(DataSet dataset, DataSetColumn column) throws SQLException, BuilderException {
        dataset.maskDataSetColumn(column);
        dataset.synchronise();
    }
    
    public static void unmaskColumn(DataSet dataset, DataSetColumn column) throws SQLException, BuilderException {
        dataset.unmaskDataSetColumn(column);
        dataset.synchronise();
    }
    
    public static void partitionBySchema(DataSet dataset) {
        dataset.setPartitionOnSchema(true);
    }

    public static void unpartitionBySchema(DataSet dataset) {
        dataset.setPartitionOnSchema(false);
    }
    
    public static void partitionByColumn(DataSet dataset, WrappedColumn column, PartitionedColumnType type) throws AssociationException {
        dataset.flagPartitionedWrappedColumn(column, type);
    }

    public static void unpartitionByColumn(DataSet dataset, WrappedColumn column) {
        dataset.unflagPartitionedWrappedColumn(column);
    }
    
    public static void enableKeyGuessing(Schema schema) {
        schema.setKeyGuessing(true);
    }
    
    public static void disableKeyGuessing(Schema schema) {
        schema.setKeyGuessing(false);
    }
    
    public static void createPrimaryKey(Table table, List columns) throws SQLException, BuilderException {
        PrimaryKey pk = new GenericPrimaryKey(columns);
        pk.setStatus(ComponentStatus.HANDMADE);
        table.setPrimaryKey(pk);
    }
    
    public static void createForeignKey(Table table, List columns) throws SQLException, BuilderException {
        ForeignKey fk = new GenericForeignKey(columns);
        fk.setStatus(ComponentStatus.HANDMADE);
        table.addForeignKey(fk);
    }
    
    public static void editKeyColumns(Mart mart, Key key, List columns) throws SQLException, BuilderException {
        key.setColumns(columns);
        changeKeyStatus(mart, key, ComponentStatus.HANDMADE);
        for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            ((DataSet)i.next()).synchronise();
        }
    }
    
    public static void removeKey(Mart mart, Key key) throws SQLException, BuilderException {
        key.destroy();
        for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            ((DataSet)i.next()).synchronise();
        }
    }
    
    public static void changeKeyStatus(Mart mart, Key key, ComponentStatus status) throws SQLException, BuilderException {
        key.setStatus(status);
        for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            ((DataSet)i.next()).synchronise();
        }
    }
    
    public static void createRelation(Mart mart, Key from, Key to) throws SQLException, BuilderException {
        PrimaryKey pk;
        ForeignKey fk;
        if ((from instanceof PrimaryKey) && (to instanceof ForeignKey)) {
            pk = (PrimaryKey)from;
            fk = (ForeignKey)to;
        }
        else if ((to instanceof PrimaryKey) && (from instanceof ForeignKey)) {
            pk = (PrimaryKey)to;
            fk = (ForeignKey)from;
        } else {
            throw new AssociationException(BuilderBundle.getString("relNotBetweenPKandFK"));
        }
        Relation r = new GenericRelation(pk, fk, Cardinality.MANY);
        r.setStatus(ComponentStatus.HANDMADE);
        for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            ((DataSet)i.next()).synchronise();
        }
    }
    
    public static void changeOptimiserType(DataSet dataset, DataSetOptimiserType type) {
        dataset.setDataSetOptimiserType(type);
    }
    
    public static Schema replicateSchema(Mart mart, Schema schema, String newName) throws SQLException, BuilderException {
        Schema newSchema = schema.replicate(newName);
        mart.addSchema(newSchema);
        return newSchema;
    }
}
