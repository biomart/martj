/*
 * SchemaSaver.java
 * Created on 03 April 2006, 10:58
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.DataLink.JDBCDataLink;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.TableProviderNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructor.GenericMartConstructor;
import org.biomart.builder.model.PartitionedTableProvider;
import org.biomart.builder.model.PartitionedTableProvider.GenericPartitionedTableProvider;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.model.TableProvider;
import org.biomart.builder.model.TableProvider.GenericTableProvider;
import org.biomart.builder.model.Window;
import org.biomart.builder.model.Window.ConcatRelationType;
import org.biomart.builder.model.Window.PartitionedColumnType;
import org.biomart.builder.model.Window.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.Window.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.Window.PartitionedColumnType.ValueCollection;
import org.biomart.builder.resources.BuilderBundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>The SchemaSaver class provides two static methods which seralize and deserialize
 * {@link Schema} objects to/from a basic XML format.</p>
 * <p>NOTE: The XML is version-specific. A formal DTD will be included with each
 * official release of MartBuilder, and subsequent releases will include new DTDs (if any aspects
 * have changed) and converter tools to translate your old files.</p>
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 5th April 2006
 * @since 0.1
 */
public class SchemaSaver extends DefaultHandler {
    /**
     * Version number of XML DTD.
     */
    public static final String DTD_VERSION = "0.1";
    
    /**
     * Internal reference to the schema object constructed upon loading.
     */
    private Schema constructedSchema;
    
    /**
     * Internal reference to the objects read from the XML keyed by ID attribute.
     */
    private Map mappedObjects;
    
    /**
     * Internal reference to the nested stack of objects read from the XML.
     */
    private Stack objectStack;
    
    /**
     * Internal reference to the output stream used to write XML to.
     */
    private Writer xmlWriter;
    
    /**
     * Internal reference to the current element being written to XML.
     */
    private String currentOutputElement;
    
    /**
     * Internal reference to the current indent of elements written to XML.
     */
    private int currentOutputIndent = 0;
    
    /**
     * Internal reference to the reverse map of objects written to XML valued with ID attribute.
     */
    private Map reverseMappedObjects;
    
    /**
     * Internal reference to the current XML writer ID ref.
     */
    private int ID = 1;
    
    
    /**
     * Returns the {@link Schema} constructed during loading.
     * @return the schema.
     */
    private Schema getSchema() {
        return this.constructedSchema;
    }
    
    /**
     * {@inheritDoc}
     */
    public void startDocument() throws SAXException {
        this.mappedObjects = new HashMap();
        this.objectStack = new Stack();
    }
    
    /**
     * {@inheritDoc}
     */
    public void endDocument() throws SAXException {
        // No action required.
    }
    
    /**
     * {@inheritDoc}
     */
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) throws SAXException {
        String eName = sName; // element name
        if ("".equals(eName)) eName = qName; // namespaceAware = false
        Map attributes = new HashMap();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aName = attrs.getLocalName(i); // Attr name
                if ("".equals(aName)) aName = attrs.getQName(i);
                String aValue = attrs.getValue(i);
                attributes.put(aName, aValue);
            }
        }
        // Start an element.
        
        // What element is it?
        Object element = null;
        
        // Schema (top-level only).
        if (BuilderBundle.getString("schema").equals(eName)) {
            element = this.constructedSchema = new Schema();
        }
        
        // Partitioned table provider (anywhere).
        else if (BuilderBundle.getString("partitionedTableProvider").equals(eName)) {
            String name = (String)attributes.get(BuilderBundle.getString("name"));
            try {
                element = new GenericPartitionedTableProvider(name);
                this.constructedSchema.addTableProvider((TableProvider)element);
            } catch (AlreadyExistsException e) {
                throw new SAXException(BuilderBundle.getString("duplicateTableProvider"),e);
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"),e);
            }
        }
        
        // JDBC table provider (anywhere, optionally inside partitioned table provider).
        else if (BuilderBundle.getString("jdbcTableProvider").equals(eName)) {
            File driverClassLocation = null;
            if (attributes.containsKey(BuilderBundle.getString("driverClassLocation"))) driverClassLocation = new File((String)attributes.get(BuilderBundle.getString("driverClassLocation")));
            String driverClassName = (String)attributes.get(BuilderBundle.getString("driverClassName"));
            String url = (String)attributes.get(BuilderBundle.getString("url"));
            String username = (String)attributes.get(BuilderBundle.getString("username"));
            String password = null;
            if (attributes.containsKey(BuilderBundle.getString("password"))) password = (String)attributes.get(BuilderBundle.getString("password"));
            String name = (String)attributes.get(BuilderBundle.getString("name"));
            String type = (String)attributes.get(BuilderBundle.getString("type"));
            try {
                if (BuilderBundle.getString("normal").equals(type)) element = new JDBCTableProvider(driverClassLocation, driverClassName, url, username, password, name);
                else if (BuilderBundle.getString("keyguessing").equals(type)) element = new JDBCKeyGuessingTableProvider(driverClassLocation, driverClassName, url, username, password, name);
                else throw new SAXException(BuilderBundle.getString("unknownJDBCTableProviderType",type));
                this.constructedSchema.addTableProvider((TableProvider)element);
            } catch (AlreadyExistsException e) {
                throw new SAXException(BuilderBundle.getString("duplicateTableProvider"),e);
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"),e);
            }
            
            // Are we inside a partitioned one?
            if (!this.objectStack.empty() && (this.objectStack.peek() instanceof PartitionedTableProvider)) {
                String partitionLabel = (String)attributes.get(BuilderBundle.getString("partitionLabel"));
                PartitionedTableProvider ptp = (PartitionedTableProvider)this.objectStack.peek();
                
                try {
                    ptp.addTableProvider(partitionLabel, (TableProvider)element);
                } catch (AssociationException e) {
                    throw new AssertionError(BuilderBundle.getString("selfMultiplyingTableProvider"));
                } catch (AlreadyExistsException e) {
                    throw new SAXException(BuilderBundle.getString("duplicateTableProviderLabel"));
                } catch (NullPointerException e) {
                    throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
                }
            }
        }
        
        // Table (inside table provider).
        else if (BuilderBundle.getString("table").equals(eName)) {
            // What table provider does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof TableProvider))
                throw new SAXException(BuilderBundle.getString("tableOutsideTableProvider"));
            TableProvider prov = (TableProvider)this.objectStack.peek();
            
            // Get the name and id as this is a common feature.
            String name = (String)attributes.get(BuilderBundle.getString("name"));
            String id = (String)attributes.get(BuilderBundle.getString("id"));
            try {
                // DataSet table provider?
                if (prov instanceof DataSet) {
                    // Get the additional attributes.
                    String type = (String)attributes.get(BuilderBundle.getString("type"));
                    DataSetTableType dsType = null;
                    if (type.equals("MAIN")) dsType = DataSetTableType.MAIN;
                    else if (type.equals("MAIN_SUBCLASS")) dsType = DataSetTableType.MAIN_SUBCLASS;
                    else if (type.equals("DIMENSION")) dsType = DataSetTableType.DIMENSION;
                    else throw new SAXException(BuilderBundle.getString("unknownDatasetTableType",type));
                    DataSetTable dst = new DataSetTable(name, (DataSet)prov, dsType);
                    element = dst;
                    
                    // Read the underlying relations.
                    String[] underRelIds = ((String)attributes.get(BuilderBundle.getString("underlyingRelationIds"))).split("\\s*,\\s*");
                    List underRels = new ArrayList();
                    for (int i = 0; i < underRelIds.length; i++) underRels.add(this.mappedObjects.get(underRelIds[i]));
                    dst.setUnderlyingRelations(underRels);
                }
                // Generic table provider?
                else if (prov instanceof GenericTableProvider) {
                    element = new GenericTable(name, prov);
                }
                // Others
                else
                    throw new SAXException(BuilderBundle.getString("unknownTableProviderType",prov.getClass().getName()));
            } catch (AlreadyExistsException e) {
                throw new SAXException(BuilderBundle.getString("duplicateTableName"));
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Column (inside table).
        else if (BuilderBundle.getString("column").equals(eName)) {
            // What table does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Table))
                throw new SAXException(BuilderBundle.getString("columnOutsideTable"));
            Table tbl = (Table)this.objectStack.peek();
            
            // Get the id as this is a common feature.
            String id = (String)attributes.get(BuilderBundle.getString("id"));
            String name = (String)attributes.get(BuilderBundle.getString("name"));
            try {
                // DataSet column?
                if (tbl instanceof DataSetTable) {
                    // Work out type and relation.
                    String type = (String)attributes.get(BuilderBundle.getString("type"));
                    String underlyingRelationId = (String)attributes.get(BuilderBundle.getString("underlyingRelationId"));
                    Relation underlyingRelation = null;
                    if (!BuilderBundle.getString("null").equals(underlyingRelationId)) underlyingRelation = (Relation)this.mappedObjects.get(underlyingRelationId);
                    // Concat relation column?
                    if (BuilderBundle.getString("concatRelation").equals(type)) {
                        element = new ConcatRelationColumn(name, (DataSetTable)tbl, underlyingRelation);
                    }
                    // TableProvider name column?
                    else if (BuilderBundle.getString("tblProvName").equals(type)) {
                        element = new TableProviderNameColumn(name, (DataSetTable)tbl);
                    }
                    // Wrapped column?
                    else if (BuilderBundle.getString("wrapped").equals(type)) {
                        Column wrappedCol = (Column)this.mappedObjects.get(attributes.get(BuilderBundle.getString("wrappedColumnId")));
                        element = new WrappedColumn(wrappedCol, (DataSetTable)tbl, underlyingRelation);
                        // Override any aliased names.
                    }
                    // Others.
                    else
                        throw new SAXException(BuilderBundle.getString("unknownColumnType",type));
                    
                    // Override the name, to make sure we get the same alias as the original.
                    ((DataSetColumn)element).setName(tbl.getName()+":"+name);
                }
                // Generic column?
                else if (tbl instanceof GenericTable) {
                    element = new GenericColumn(name, tbl);
                }
                // Others
                else
                    throw new SAXException(BuilderBundle.getString("unknownTableType",tbl.getClass().getName()));
            } catch (AssociationException e) {
                throw new AssertionError(BuilderBundle.getString("tableMismatch"));
            } catch (AlreadyExistsException e) {
                throw new SAXException(BuilderBundle.getString("duplicateColumn"));
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Primary key (inside table).
        else if (BuilderBundle.getString("primaryKey").equals(eName)) {
            // What table does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Table))
                throw new SAXException(BuilderBundle.getString("pkOutsideTable"));
            Table tbl = (Table)this.objectStack.peek();
            
            // Get the ID.
            String id = (String)attributes.get(BuilderBundle.getString("id"));
            try {
                ComponentStatus status = ComponentStatus.get((String)attributes.get(BuilderBundle.getString("status")));
                // Decode the column IDs from the comma-separated list.
                String[] pkColIds = ((String)attributes.get(BuilderBundle.getString("columnIds"))).split("\\s*,\\s*");
                List pkCols = new ArrayList();
                for (int i = 0; i < pkColIds.length; i++) pkCols.add(this.mappedObjects.get(pkColIds[i]));
                
                // Make the key.
                PrimaryKey pk = new GenericPrimaryKey(pkCols);
                pk.setStatus(status);
                tbl.setPrimaryKey(pk);
                element = pk;
            } catch (AssociationException e) {
                throw new SAXException(BuilderBundle.getString("multiTableKey"));
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttributeOrXref"), e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Foreign key (inside table).
        else if (BuilderBundle.getString("foreignKey").equals(eName)) {
            // What table does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Table))
                throw new SAXException(BuilderBundle.getString("fkOutsideTable"));
            Table tbl = (Table)this.objectStack.peek();
            
            // Get the ID.
            String id = (String)attributes.get(BuilderBundle.getString("id"));
            try {
                ComponentStatus status = ComponentStatus.get((String)attributes.get(BuilderBundle.getString("status")));
                // Decode the column IDs from the comma-separated list.
                String[] fkColIds = ((String)attributes.get(BuilderBundle.getString("columnIds"))).split("\\s*,\\s*");
                List fkCols = new ArrayList();
                for (int i = 0; i < fkColIds.length; i++) fkCols.add(this.mappedObjects.get(fkColIds[i]));
                
                // Make the key.
                ForeignKey fk = new GenericForeignKey(fkCols);
                fk.setStatus(status);
                tbl.addForeignKey(fk);
                element = fk;
            } catch (AssociationException e) {
                throw new SAXException(BuilderBundle.getString("multiTableKey"));
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttributeOrXref"), e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Relation (anywhere).
        else if (BuilderBundle.getString("relation").equals(eName)) {
            // Get the ID.
            String id = (String)attributes.get(BuilderBundle.getString("id"));
            try {
                ComponentStatus status = ComponentStatus.get((String)attributes.get(BuilderBundle.getString("status")));
                Cardinality card = Cardinality.get((String)attributes.get(BuilderBundle.getString("fkCardinality")));
                PrimaryKey pk = (PrimaryKey)this.mappedObjects.get(attributes.get(BuilderBundle.getString("primaryKeyId")));
                ForeignKey fk = (ForeignKey)this.mappedObjects.get(attributes.get(BuilderBundle.getString("foreignKeyId")));
                
                // Make it
                Relation rel = new GenericRelation(pk, fk, card);
                
                // Set its status.
                rel.setStatus(status);
                element = rel;
            } catch (AssociationException e) {
                throw new SAXException(BuilderBundle.getString("keyColumnCountMismatch"));
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttributeOrXref"), e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // MartConstructor (anywhere).
        else if (BuilderBundle.getString("genericMartConstructor").equals(eName)) {
            String id = (String)attributes.get(BuilderBundle.getString("id"));
            String name = (String)attributes.get(BuilderBundle.getString("name"));
            
            try {
                element = new GenericMartConstructor(name);
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Window (anywhere).
        else if (BuilderBundle.getString("window").equals(eName)) {
            try {
                String name = (String)attributes.get(BuilderBundle.getString("name"));
                Table centralTable = (Table)this.mappedObjects.get(attributes.get(BuilderBundle.getString("centralTableId")));
                boolean partitionOnTblProv = ((String)attributes.get(BuilderBundle.getString("partitionOnTblProv"))).equals(BuilderBundle.getString("true"));
                
                // Make it.
                Window w = new Window(this.constructedSchema, centralTable, name);
                w.setPartitionOnTableProvider(partitionOnTblProv);
                element = w;
            } catch (AlreadyExistsException e) {
                throw new SAXException(BuilderBundle.getString("duplicateWindows"), e);
            } catch (AssociationException e) {
                throw new SAXException(BuilderBundle.getString("tableNotInSchema"), e);
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttributeOrXref"), e);
            }
        }
        
        // Masked Relation (inside window).
        else if (BuilderBundle.getString("maskedRelation").equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Window))
                throw new SAXException(BuilderBundle.getString("maskedRelationOutsideWindow"));
            Window w = (Window)this.objectStack.peek();
            
            try {
                Relation rel = (Relation)this.mappedObjects.get(attributes.get(BuilderBundle.getString("relationId")));
                w.maskRelation(rel);
                element = rel;
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
        }
        
        // Subclass Relation (inside window).
        else if (BuilderBundle.getString("subclassRelation").equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Window))
                throw new SAXException(BuilderBundle.getString("subclassRelationOutsideWindow"));
            Window w = (Window)this.objectStack.peek();
            
            try {
                Relation rel = (Relation)this.mappedObjects.get(attributes.get(BuilderBundle.getString("relationId")));
                w.flagSubclassRelation(rel);
                element = rel;
            } catch (AssociationException e) {
                throw new SAXException(BuilderBundle.getString("subclassNotOnMainTable"), e);
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
        }
        
        // Concat Relation (inside window).
        else if (BuilderBundle.getString("concatRelation").equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Window))
                throw new SAXException(BuilderBundle.getString("concatRelationOutsideWindow"));
            Window w = (Window)this.objectStack.peek();
            
            try {
                Relation rel = (Relation)this.mappedObjects.get(attributes.get(BuilderBundle.getString("relationId")));
                String type = (String)attributes.get(BuilderBundle.getString("concatRelationType"));
                ConcatRelationType crType = null;
                if (type.equals("COMMA")) crType = ConcatRelationType.COMMA;
                else if (type.equals("SPACE")) crType = ConcatRelationType.SPACE;
                else if (type.equals("TAB")) crType = ConcatRelationType.TAB;
                else throw new SAXException(BuilderBundle.getString("unknownConcatRelationType",type));
                w.flagConcatOnlyRelation(rel, crType);
                element = rel;
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
        }
        
        // Masked Column (inside window).
        else if (BuilderBundle.getString("maskedColumn").equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Window))
                throw new SAXException(BuilderBundle.getString("maskedColumnOutsideWindow"));
            Window w = (Window)this.objectStack.peek();
            
            try {
                Column col = (Column)this.mappedObjects.get(attributes.get(BuilderBundle.getString("columnId")));
                w.maskColumn(col);
                element = col;
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
        }
        
        // Partition Column (inside window).
        else if (BuilderBundle.getString("partitionColumn").equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Window))
                throw new SAXException(BuilderBundle.getString("partitionColumnOutsideWindow"));
            Window w = (Window)this.objectStack.peek();
            
            try {
                Column col = (Column)this.mappedObjects.get(attributes.get(BuilderBundle.getString("columnId")));
                String type = (String)attributes.get(BuilderBundle.getString("partitionedColumnType"));
                PartitionedColumnType resolvedType = null;
                
                // What kind of partition is it?
                // Single value partition?
                if (BuilderBundle.getString("singleValue").equals(type)) {
                    String value = null;
                    boolean useNull = ((String)attributes.get(BuilderBundle.getString("useNull"))).equals(BuilderBundle.getString("true"));
                    if (!useNull) value = (String)attributes.get(BuilderBundle.getString("value"));
                    resolvedType = new SingleValue(value);
                }
                // Unique values partition?
                else if (BuilderBundle.getString("uniqueValues").equals(type)) {
                    resolvedType = new UniqueValues();
                }
                // Values collection partition?
                else if (BuilderBundle.getString("valueCollection").equals(type)) {
                    // Values are comma-separated.
                    List valueList = new ArrayList();
                    if (attributes.containsKey(BuilderBundle.getString("values"))) valueList.addAll(Arrays.asList(((String)attributes.get(BuilderBundle.getString("values"))).split("\\s*,\\s*")));
                    boolean includeNull = ((String)attributes.get(BuilderBundle.getString("useNull"))).equals(BuilderBundle.getString("true"));
                    if (includeNull) valueList.add(null);
                    // Make the collection.
                    resolvedType = new ValueCollection(valueList);
                }
                // Others.
                else
                    throw new SAXException(BuilderBundle.getString("unknownPartitionColumnType",type));
                
                // Flag the column.
                w.flagPartitionedColumn(col, resolvedType);
                element = col;
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttribute"), e);
            }
        }
        
        // DataSet (inside window).
        else if (BuilderBundle.getString("dataset").equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Window))
                throw new SAXException(BuilderBundle.getString("datasetOutsideWindow"));
            Window w = (Window)this.objectStack.peek();
            
            try {
                String optType = (String)attributes.get(BuilderBundle.getString("optimiser"));
                DataSetOptimiserType opt = null;
                if ("NONE".equals(optType)) opt = DataSetOptimiserType.NONE;
                else if ("LEFTJOIN".equals(optType)) opt = DataSetOptimiserType.LEFTJOIN;
                else if ("COLUMN".equals(optType)) opt = DataSetOptimiserType.COLUMN;
                else if ("TABLE".equals(optType)) opt = DataSetOptimiserType.TABLE;
                else throw new SAXException(BuilderBundle.getString("unknownOptimiserType",optType));
                MartConstructor mc = (MartConstructor)this.mappedObjects.get(attributes.get(BuilderBundle.getString("martConstructorId")));
                DataSet ds = w.getDataSet();
                ds.setMartConstructor(mc);
                ds.setDataSetOptimiserType(opt);
                element = ds;
            } catch (NullPointerException e) {
                throw new SAXException(BuilderBundle.getString("missingAttributeOrXref"), e);
            }
        }
        
        // Others.
        else
            throw new SAXException(BuilderBundle.getString("unknownTag",eName));
        
        // Stick the element on the stack.
        this.objectStack.push(element);
    }
    
    /**
     * {@inheritDoc}
     */
    public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
        String eName = sName; // element name
        if ("".equals(eName)) eName = qName; // namespaceAware = false
        // End an element.
        // Use currentText to obtain text value for the tag, if required.
        // Pop the element off the stack.
        this.objectStack.pop();
    }
    
    /**
     * Internal method which opens a tag in the output stream.
     * @param name the tag to open.
     * @throws NullPointerException if the tag is null.
     * @throws IOException if it failed to write it.
     */
    private void openElement(String name) throws NullPointerException, IOException {
        // Sanity check.
        if (name==null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        // Are we already partway through one?
        if (this.currentOutputElement != null) {
            // Yes, so put closing angle bracket on it.
            this.xmlWriter.write(">\n");
            // Increase the indent.
            this.currentOutputIndent++;
        }
        // Write the tag.
        for (int i = this.currentOutputIndent; i > 0; i--) this.xmlWriter.write(BuilderBundle.getString("xmlIndent"));
        this.xmlWriter.write("<");
        this.xmlWriter.write(name);
        this.currentOutputElement = name;
    }
    
    /**
     * Internal method which closes a tag in the output stream.
     * @param name the tag to close.
     * @throws NullPointerException if the tag is null.
     * @throws IOException if it failed to write it.
     */
    private void closeElement(String name) throws NullPointerException, IOException {
        // Sanity check.
        if (name==null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        // Can we use the simple /> method?
        if (this.currentOutputElement !=null && name.equals(this.currentOutputElement)) {
            // Yes, so put closing angle bracket on it.
            this.xmlWriter.write("/>\n");
        } else {
            // No, so use the full technique.
            // Decrease the indent.
            this.currentOutputIndent--;
            // Output the tag.
            for (int i = this.currentOutputIndent; i > 0; i--) this.xmlWriter.write(BuilderBundle.getString("xmlIndent"));
            this.xmlWriter.write("</");
            this.xmlWriter.write(name);
            this.xmlWriter.write(">\n");
        }
        this.currentOutputElement = null;
    }
    
    /**
     * Internal method which writes an attribute in the output stream.
     * @param name the name of the attribute.
     * @param value the value of the attribute.
     * @throws NullPointerException if the name or value is null.
     * @throws IOException if it failed to write it.
     */
    private void writeAttribute(String name, String value) throws NullPointerException, IOException {
        // Sanity check.
        if (name==null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        if (value==null)
            throw new NullPointerException(BuilderBundle.getString("valueIsNull"));
        // Write it.
        this.xmlWriter.write(" ");
        this.xmlWriter.write(name);
        this.xmlWriter.write("=\"");
        this.xmlWriter.write(value);
        this.xmlWriter.write("\"");
    }
    
    /**
     * Internal method which writes a comma-separated list of attributes in the output stream.
     * @param name the name of the attribute.
     * @param values the values of the attribute.
     * @throws NullPointerException if the name or value is null.
     * @throws IOException if it failed to write it.
     */
    private void writeAttribute(String name, String[] values) throws NullPointerException, IOException {
        // Sanity check.
        if (name==null)
            throw new NullPointerException(BuilderBundle.getString("nameIsNull"));
        if (values==null)
            throw new NullPointerException(BuilderBundle.getString("valueIsNull"));
        // Write it.
        StringBuffer sb = new StringBuffer();
        for (int i  = 0; i < values.length; i++) {
            if (i>0) sb.append(",");
            sb.append(values[i]);
        }
        this.writeAttribute(name, sb.toString());
    }
    
    /**
     * Internal method which writes out the tables of a table provider and remembers
     * the relations it saw as it goes along.
     * @param tblProv the {@link TableProvider} to write out the tables of.
     * @param relations the set of {@link Relation}s we found on the way.
     * @throws NullPointerException if any parameter was null.
     * @throws IOException if there was a problem writing to file.
     * @throws AssociationException if an unwritable kind of object was found.
     */
    private void writeTableProviderContents(TableProvider tblProv, Set relations) throws NullPointerException, IOException, AssociationException {
        // Sanity check.
        if (tblProv==null)
            throw new NullPointerException(BuilderBundle.getString("tblProvIsNull"));
        if (relations==null)
            throw new NullPointerException(BuilderBundle.getString("relationsIsNull"));
        // Do it.
        
        // Write out tables inside each provider. WITH IDS.
        for (Iterator ti = tblProv.getTables().iterator(); ti.hasNext(); ) {
            Table table = (Table)ti.next();
            String tableMappedID = ""+this.ID++;
            this.reverseMappedObjects.put(table, tableMappedID);
            
            // Start table.
            this.openElement(BuilderBundle.getString("table"));
            this.writeAttribute(BuilderBundle.getString("id"), tableMappedID);
            this.writeAttribute(BuilderBundle.getString("name"), table.getName());
            
            // A dataset table?
            if (table instanceof DataSetTable) {
                // Write the type.
                this.writeAttribute(BuilderBundle.getString("type"),((DataSetTable)table).getType().getName());
                
                // Write out the underlying relations.
                List underRelIds = new ArrayList();
                for (Iterator i = ((DataSetTable)table).getUnderlyingRelations().iterator(); i.hasNext(); ) underRelIds.add(this.reverseMappedObjects.get(i.next()));
                this.writeAttribute(BuilderBundle.getString("underlyingRelationIds"),(String[])underRelIds.toArray(new String[0]));
            }
            
            // Write out columns inside each table. WITH IDS.
            for (Iterator ci = table.getColumns().iterator(); ci.hasNext(); ) {
                Column col = (Column)ci.next();
                String colMappedID = ""+this.ID++;
                this.reverseMappedObjects.put(col, colMappedID);
                
                // Start column.
                this.openElement(BuilderBundle.getString("column"));
                this.writeAttribute(BuilderBundle.getString("id"), colMappedID);
                this.writeAttribute(BuilderBundle.getString("name"),col.getName());
                
                // Dataset column?
                if (col instanceof DataSetColumn) {
                    DataSetColumn dcol = (DataSetColumn)col;
                    Relation underlyingRelation = dcol.getUnderlyingRelation();
                    String underlyingRelationId = BuilderBundle.getString("null");
                    if (underlyingRelation != null) underlyingRelationId = (String)this.reverseMappedObjects.get(underlyingRelation);
                    this.writeAttribute(BuilderBundle.getString("underlyingRelationId"),underlyingRelationId);
                    this.writeAttribute(BuilderBundle.getString("alt"),underlyingRelation==null?BuilderBundle.getString("null"):underlyingRelation.toString());
                    // Concat relation column?
                    if (dcol instanceof ConcatRelationColumn) {
                        this.writeAttribute(BuilderBundle.getString("type"),BuilderBundle.getString("concatRelation"));
                    }
                    // TableProvider name column?
                    else if (dcol instanceof TableProviderNameColumn) {
                        this.writeAttribute(BuilderBundle.getString("type"),BuilderBundle.getString("tblProvName"));
                    }
                    // Wrapped column?
                    else if (dcol instanceof WrappedColumn) {
                        this.writeAttribute(BuilderBundle.getString("type"),BuilderBundle.getString("wrapped"));
                        this.writeAttribute(BuilderBundle.getString("wrappedColumnId"),(String)this.reverseMappedObjects.get(((WrappedColumn)dcol).getWrappedColumn()));
                        this.writeAttribute(BuilderBundle.getString("wrappedColumnAlt"),((WrappedColumn)dcol).getWrappedColumn().toString());
                    }
                    // Others
                    else
                        throw new AssociationException(BuilderBundle.getString("unknownDatasetColumnType",dcol.getClass().getName()));
                }
                // Generic column?
                else if (col instanceof GenericColumn) {
                    // Nothing extra required here.
                }
                // Others
                else
                    throw new AssociationException(BuilderBundle.getString("unknownColumnType",col.getClass().getName()));
                
                // Close off column element.
                this.closeElement(BuilderBundle.getString("column"));
            }
            
            // Write out keys inside each table. WITH IDS. Remember relations as we go along.
            for (Iterator ki = table.getKeys().iterator(); ki.hasNext(); ) {
                Key key = (Key)ki.next();
                String keyMappedID = ""+this.ID++;
                this.reverseMappedObjects.put(key, keyMappedID);
                
                String elem = null;
                if (key instanceof PrimaryKey)
                    elem = BuilderBundle.getString("primaryKey");
                else if (key instanceof ForeignKey)
                    elem = BuilderBundle.getString("foreignKey");
                else
                    throw new AssociationException(BuilderBundle.getString("unknownKey",key.getClass().getName()));
                
                this.openElement(elem);
                this.writeAttribute(BuilderBundle.getString("id"), keyMappedID);
                List columnIds = new ArrayList();
                for (Iterator kci = key.getColumns().iterator(); kci.hasNext(); ) columnIds.add(this.reverseMappedObjects.get(kci.next()));
                this.writeAttribute(BuilderBundle.getString("columnIds"), (String[])columnIds.toArray(new String[0]));
                this.writeAttribute(BuilderBundle.getString("status"), key.getStatus().getName());
                this.writeAttribute(BuilderBundle.getString("alt"), key.toString());
                this.closeElement(elem);
            }
            
            // Finish table.
            relations.addAll(table.getRelations());
            this.closeElement(BuilderBundle.getString("table"));
        }
    }
    
    /**
     * Internal method which writes out a set of relations.
     * @param relations the set of {@link Relation}s to write.
     * @throws NullPointerException if any parameter was null.
     * @throws IOException if there was a problem writing to file.
     */
    private void writeRelations(Set relations) throws NullPointerException, IOException {
        // Sanity check.
        if (relations==null)
            throw new NullPointerException(BuilderBundle.getString("relationsIsNull"));
        // Do it.
        
        // Write out relations. WITH IDS.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            String relMappedID = ""+this.ID++;
            this.reverseMappedObjects.put(r, relMappedID);
            this.openElement(BuilderBundle.getString("relation"));
            this.writeAttribute(BuilderBundle.getString("id"), relMappedID);
            this.writeAttribute(BuilderBundle.getString("fkCardinality"), r.getFKCardinality().getName());
            this.writeAttribute(BuilderBundle.getString("primaryKeyId"), (String)this.reverseMappedObjects.get(r.getPrimaryKey()));
            this.writeAttribute(BuilderBundle.getString("foreignKeyId"), (String)this.reverseMappedObjects.get(r.getForeignKey()));
            this.writeAttribute(BuilderBundle.getString("status"), r.getStatus().toString());
            this.writeAttribute(BuilderBundle.getString("alt"), r.toString());
            this.closeElement(BuilderBundle.getString("relation"));
        }
    }
    
    /**
     * Internal method which does the work of writing out XML files and
     * generating those funky ID tags you see in them.
     * @param schema the schema to write.
     * @param ow the Writer to write the XML to.
     * @throws IOException if a write error occurs.
     * @throws AssociationException if it encounters an object not writable under the current DTD.
     * @throws NullPointerException if the writer or schema specified was null.
     */
    private void writeXML(Schema schema, Writer ow) throws IOException, AssociationException, NullPointerException {
        // Sanity check.
        if (ow==null)
            throw new NullPointerException(BuilderBundle.getString("writerIsNull"));
        if (schema==null)
            throw new NullPointerException(BuilderBundle.getString("schemaIsNull"));
        
        // Remember the output stream.
        this.xmlWriter = ow;
        
        // Write the headers.
        this.xmlWriter.write(BuilderBundle.getString("xmlHeader")+"\n");
        //this.xmlWriter.write(BuilderBundle.getString("xmlDocType",DTD_VERSION)+"\n");
        
        // Initialise the ID counter.
        this.reverseMappedObjects = new HashMap();
        
        // Start by enclosing the whole lot in a <schema> tag.
        this.openElement(BuilderBundle.getString("schema"));
        
        // MartConstructor (anywhere).
        Set martConstructors = new TreeSet();
        for (Iterator i = schema.getWindows().iterator(); i.hasNext(); ) {
            Window w = (Window)i.next();
            MartConstructor mc = w.getDataSet().getMartConstructor();
            martConstructors.add(mc);
        }
        for (Iterator i = martConstructors.iterator(); i.hasNext(); ) {
            MartConstructor mc = (MartConstructor)i.next();
            String mcMappedID = ""+this.ID++;
            this.reverseMappedObjects.put(mc, mcMappedID);
            
            // Generic constructor?
            if (mc instanceof GenericMartConstructor) {
                this.openElement(BuilderBundle.getString("genericMartConstructor"));
                this.writeAttribute(BuilderBundle.getString("id"),mcMappedID);
                this.writeAttribute(BuilderBundle.getString("name"),mc.getName());
                this.closeElement(BuilderBundle.getString("genericMartConstructor"));
            }
            // Others?
            else
                throw new AssociationException(BuilderBundle.getString("unknownMartConstuctorType",mc.getClass().getName()));
        }
        
        // Check for partitioned table provider.
        Map tblProvs = new HashMap();
        Collection initialTblProvs = new HashSet(schema.getTableProviders());
        String parentTblProvPartitionName = null;
        for (Iterator i = initialTblProvs.iterator(); i.hasNext(); ) {
            TableProvider tblProv = (TableProvider)i.next();
            if (tblProv instanceof PartitionedTableProvider) {
                // Cannot nest these.
                if (parentTblProvPartitionName != null)
                    throw new AssertionError(BuilderBundle.getString("nestedPartitionedTblProv"));
                //
                parentTblProvPartitionName = tblProv.getName();
                tblProvs.putAll(((PartitionedTableProvider)tblProv).getTableProviders());
                tblProvs.put("__PARENT__TBLPROV", tblProv);
            } else {
                tblProvs.put(tblProv.getName(), tblProv);
            }
        }
        // Was it partitioned?
        if (parentTblProvPartitionName != null) {
            this.openElement(BuilderBundle.getString("partitionedTableProvider"));
            this.writeAttribute(BuilderBundle.getString("name"), parentTblProvPartitionName);
        }
        
        // Write out table providers.
        Set relations = new TreeSet();
        for (Iterator i = tblProvs.keySet().iterator(); i.hasNext(); ) {
            String label = (String)i.next();
            TableProvider tblProv = (TableProvider)tblProvs.get(label);
            // What kind of tbl prov is it?
            // JDBC?
            if ((tblProv instanceof JDBCTableProvider) || (tblProv instanceof JDBCKeyGuessingTableProvider)) {
                this.openElement(BuilderBundle.getString("jdbcTableProvider"));
                JDBCDataLink jdl = (JDBCDataLink)tblProv;
                
                if (jdl.getDriverClassLocation()!=null)
                    this.writeAttribute(BuilderBundle.getString("driverClassLocation"), jdl.getDriverClassLocation().getPath());
                this.writeAttribute(BuilderBundle.getString("driverClassName"), jdl.getDriverClassName());
                this.writeAttribute(BuilderBundle.getString("url"), jdl.getJDBCURL());
                this.writeAttribute(BuilderBundle.getString("username"), jdl.getUsername());
                if (jdl.getPassword() != null)
                    this.writeAttribute(BuilderBundle.getString("password"), jdl.getPassword());
                this.writeAttribute(BuilderBundle.getString("name"), tblProv.getName());
                if (tblProv instanceof JDBCTableProvider)
                    this.writeAttribute(BuilderBundle.getString("type"), BuilderBundle.getString("keyguessing"));
                else
                    this.writeAttribute(BuilderBundle.getString("type"), BuilderBundle.getString("normal"));
                if (parentTblProvPartitionName != null)
                    this.writeAttribute(BuilderBundle.getString("partitionLabel"), label);
            }
            // Partitioned table provider? (Supplying us with overview tables).
            else if (tblProv instanceof PartitionedTableProvider) {
                // Can ignore as header already open.
            }
            // Others?
            else
                throw new AssociationException(BuilderBundle.getString("unknownTableProviderType",tblProv.getClass().getName()));
            
            // Write out the contents, and note the relations.
            this.writeTableProviderContents(tblProv, relations);
            
            // What kind of tbl prov was it?
            // JDBC?
            if ((tblProv instanceof JDBCTableProvider) || (tblProv instanceof JDBCKeyGuessingTableProvider)) {
                this.closeElement(BuilderBundle.getString("jdbcTableProvider"));
            }
            // Partitioned table provider? (Supplying us with overview tables).
            else if (tblProv instanceof PartitionedTableProvider) {
                // Can ignore as footer will be closed later.
            }
            // Others?
            else
                throw new AssociationException(BuilderBundle.getString("unknownTableProviderType",tblProv.getClass().getName()));
        }
        
        // Check for partitioned table provider.
        if (parentTblProvPartitionName != null) {
            this.closeElement(BuilderBundle.getString("partitionedTableProvider"));
        }
        
        // Write out relations. WITH IDS.
        this.writeRelations(relations);
        
        // Write out mart constructors. Remember windows as we go along.
        
        // Write out windows.
        for (Iterator wi = schema.getWindows().iterator(); wi.hasNext(); ) {
            Window w = (Window)wi.next();
            this.openElement(BuilderBundle.getString("window"));
            this.writeAttribute(BuilderBundle.getString("name"),w.getName());
            this.writeAttribute(BuilderBundle.getString("centralTableId"),(String)this.reverseMappedObjects.get(w.getCentralTable()));
            this.writeAttribute(BuilderBundle.getString("partitionOnTblProv"),w.getPartitionOnTableProvider()?BuilderBundle.getString("true"):BuilderBundle.getString("false"));
            this.writeAttribute(BuilderBundle.getString("alt"),w.getCentralTable().toString());
            
            // Write out masked relations inside window.
            for (Iterator x = w.getMaskedRelations().iterator(); x.hasNext(); ) {
                Relation r = (Relation)x.next();
                this.openElement(BuilderBundle.getString("maskedRelation"));
                this.writeAttribute(BuilderBundle.getString("relationId"),(String)this.reverseMappedObjects.get(r));
                this.writeAttribute(BuilderBundle.getString("alt"),r.toString());
                this.closeElement(BuilderBundle.getString("maskedRelation"));
            }
            
            // Write out subclass relations inside window.
            for (Iterator x = w.getSubclassedRelations().iterator(); x.hasNext(); ) {
                Relation r = (Relation)x.next();
                this.openElement(BuilderBundle.getString("subclassRelation"));
                this.writeAttribute(BuilderBundle.getString("relationId"),(String)this.reverseMappedObjects.get(r));
                this.writeAttribute(BuilderBundle.getString("alt"),r.toString());
                this.closeElement(BuilderBundle.getString("subclassRelation"));
            }
            
            // Write out concat relations inside window.
            for (Iterator x = w.getConcatOnlyRelations().iterator(); x.hasNext(); ) {
                Relation r = (Relation)x.next();
                this.openElement(BuilderBundle.getString("concatRelation"));
                this.writeAttribute(BuilderBundle.getString("relationId"),(String)this.reverseMappedObjects.get(r));
                this.writeAttribute(BuilderBundle.getString("concatRelationType"),w.getConcatRelationType(r).getName());
                this.writeAttribute(BuilderBundle.getString("alt"),r.toString());
                this.closeElement(BuilderBundle.getString("concatRelation"));
            }
            
            // Write out masked columns inside window.
            for (Iterator x = w.getMaskedColumns().iterator(); x.hasNext(); ) {
                Column c = (Column)x.next();
                this.openElement(BuilderBundle.getString("maskedColumn"));
                this.writeAttribute(BuilderBundle.getString("columnId"),(String)this.reverseMappedObjects.get(c));
                this.writeAttribute(BuilderBundle.getString("alt"),c.toString());
                this.closeElement(BuilderBundle.getString("maskedColumn"));
            }
            
            // Write out partitioned columns inside window.
            for (Iterator x = w.getPartitionedColumns().iterator(); x.hasNext(); ) {
                Column c = (Column)x.next();
                this.openElement(BuilderBundle.getString("partitionColumn"));
                this.writeAttribute(BuilderBundle.getString("columnId"),(String)this.reverseMappedObjects.get(c));
                PartitionedColumnType ptc = w.getPartitionedColumnType(c);
                
                // What kind of partition is it?
                // Single value partition?
                if (ptc instanceof SingleValue) {
                    this.writeAttribute(BuilderBundle.getString("partitionedColumnType"),BuilderBundle.getString("singleValue"));
                    String value = ((SingleValue)ptc).getValue();
                    this.writeAttribute(BuilderBundle.getString("useNull"),value==null?BuilderBundle.getString("true"):BuilderBundle.getString("false"));
                    if (value!=null) this.writeAttribute(BuilderBundle.getString("value"),value);
                }
                // Unique values partition?
                else if (ptc instanceof UniqueValues) {
                    this.writeAttribute(BuilderBundle.getString("partitionedColumnType"),BuilderBundle.getString("uniqueValues"));
                    // No extra attributes required.
                }
                // Values collection partition?
                else if (ptc instanceof ValueCollection) {
                    this.writeAttribute(BuilderBundle.getString("partitionedColumnType"),BuilderBundle.getString("valueCollection"));
                    // Values are comma-separated.
                    List valueList = new ArrayList();
                    valueList.addAll(((ValueCollection)ptc).getValues());
                    if (valueList.contains(null)) {
                        this.writeAttribute(BuilderBundle.getString("useNull"),BuilderBundle.getString("true"));
                        valueList.remove(null);
                    } else {
                        this.writeAttribute(BuilderBundle.getString("useNull"),BuilderBundle.getString("false"));
                    }
                    if (!valueList.isEmpty()) this.writeAttribute(BuilderBundle.getString("values"),(String[])valueList.toArray(new String[0]));
                }
                // Others.
                else
                    throw new AssociationException(BuilderBundle.getString("unknownPartitionColumnType",ptc.getClass().getName()));
                
                // Finish off.
                this.writeAttribute(BuilderBundle.getString("alt"),c.toString());
                this.closeElement(BuilderBundle.getString("partitionColumn"));
            }
            
            // Write out dataset inside window.
            this.openElement(BuilderBundle.getString("dataset"));
            this.writeAttribute(BuilderBundle.getString("optimiser"),w.getDataSet().getDataSetOptimiserType().getName());
            this.writeAttribute(BuilderBundle.getString("martConstructorId"),(String)this.reverseMappedObjects.get(w.getDataSet().getMartConstructor()));
            
            // Write out the contents of the dataset, and note the relations.
            Set dsRelations = new TreeSet();
            this.writeTableProviderContents(w.getDataSet(), dsRelations);
            
            // Write out relations inside dataset. WITH IDS.
            this.writeRelations(dsRelations);
            this.closeElement(BuilderBundle.getString("dataset"));
            this.closeElement(BuilderBundle.getString("window"));
        }
        
        // Finished! Close the schema tag.
        this.closeElement(BuilderBundle.getString("schema"));
        
        // Flush.
        this.xmlWriter.flush();
    }
    
    /**
     * The load method takes a {@link File} and loads up a {@link Schema} object based
     * on the XML contents of the file. This XML is usually generated by the
     * {@link SchemaSaver#save(Schema,File)} method.
     * @param file the {@link File} to load the data from.
     * @return a {@link Schema} object containing the data from the file.
     * @throws IOException if there was any problem reading the file.
     * @throws NullPointerException if the file specified was null.
     * @throws AssociationException if the content of the file was not a {@link Schema} object.
     */
    public static Schema load(File file) throws IOException, NullPointerException, AssociationException {
        // Sanity check.
        if (file==null)
            throw new NullPointerException(BuilderBundle.getString("fileIsNull"));
        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // Parse the input
        SchemaSaver loader = new SchemaSaver();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(file, loader);
        } catch (ParserConfigurationException e) {
            throw new AssociationException(BuilderBundle.getString("XMLConfigFailed"), e);
        } catch (SAXException e) {
            throw new AssociationException(BuilderBundle.getString("XMLUnparseable"), e);
        }
        // Get the constructed object.
        Schema s = loader.getSchema();
        // Check that it is a schema.
        if (s == null)
            throw new AssociationException(BuilderBundle.getString("fileNotSchemaVersion",DTD_VERSION));
        // Return.
        return s;
    }
    
    /**
     * The save method takes a {@link Schema} object and writes out XML describing it to
     * the given {@link File}. This XML can be read by the {@link SchemaSaver#load(File)} method.
     * @param schema {@link Schema} object containing the data for the file.
     * @param file the {@link File} to save the data to.
     * @throws IOException if there was any problem writing the file.
     * @throws NullPointerException if the file or schema specified was null.
     * @throws AssociationException if it encounters an object not writable under the current DTD.
     */
    public static void save(Schema schema, File file) throws IOException, NullPointerException, AssociationException {
        // Sanity check.
        if (file==null)
            throw new NullPointerException(BuilderBundle.getString("fileIsNull"));
        if (schema==null)
            throw new NullPointerException(BuilderBundle.getString("schemaIsNull"));
        // Open the file.
        FileWriter fw = new FileWriter(file);
        // Write it out.
        (new SchemaSaver()).writeXML(schema, fw);
        // Close the output stream.
        fw.close();
    }
}
