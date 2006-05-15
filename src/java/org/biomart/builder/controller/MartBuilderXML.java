/*
 * MartBuilderXML.java
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.model.Column;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Key.ForeignKey;
import org.biomart.builder.model.Key.GenericForeignKey;
import org.biomart.builder.model.Key.GenericPrimaryKey;
import org.biomart.builder.model.Key.PrimaryKey;
import org.biomart.builder.model.MartConstructor;
import org.biomart.builder.model.MartConstructor.GenericMartConstructor;
import org.biomart.builder.model.SchemaGroup;
import org.biomart.builder.model.SchemaGroup.GenericSchemaGroup;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.GenericRelation;
import org.biomart.builder.model.Mart;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Schema.GenericSchema;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.PartitionedColumnType;
import org.biomart.builder.model.DataSet.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.DataSet.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.DataSet.PartitionedColumnType.ValueCollection;
import org.biomart.builder.resources.BuilderBundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>The MartBuilderXML class provides two static methods which seralize and deserialize
 * {@link Mart} objects to/from a basic XML format.</p>
 * <p>NOTE: The XML is version-specific. A formal DTD will be included with each
 * official release of MartBuilder, and subsequent releases will include new DTDs (if any aspects
 * have changed) and converter tools to translate your old files.</p>
 *
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.8, 15th May 2006
 * @since 0.1
 */
public class MartBuilderXML extends DefaultHandler {
    /**
     * Version number of XML DTD.
     */
    public static final String DTD_VERSION = "0.1";
    
    /**
     * Internal reference to the schema object constructed upon loading.
     */
    private Mart constructedMart;
    
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
     * Returns the {@link Mart} constructed during loading.
     *
     * @return the schema.
     */
    private Mart getMart() {
        return this.constructedMart;
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
        
        // Mart (top-level only).
        if ("mart".equals(eName)) {
            element = this.constructedMart = new Mart();
        }
        
        // Partitioned table provider (anywhere).
        else if ("schemaGroup".equals(eName)) {
            String name = (String)attributes.get("name");
            try {
                element = new GenericSchemaGroup(name);
                this.constructedMart.addSchema((Schema)element);
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        
        // JDBC table provider (anywhere, optionally inside partitioned table provider).
        else if ("jdbcSchema".equals(eName)) {
            File driverClassLocation = null;
            if (attributes.containsKey("driverClassLocation")) driverClassLocation = new File((String)attributes.get("driverClassLocation"));
            String driverClassName = (String)attributes.get("driverClassName");
            String url = (String)attributes.get("url");
            String username = (String)attributes.get("username");
            String password = null;
            if (attributes.containsKey("password")) password = (String)attributes.get("password");
            String name = (String)attributes.get("name");
            boolean keyguessing = ((String)attributes.get("keyguessing")).equals("true");
            try {
                element = new JDBCSchema(driverClassLocation, driverClassName, url, username, password, name, keyguessing);
                // Are we inside a partitioned one?
                if (!this.objectStack.empty() && (this.objectStack.peek() instanceof SchemaGroup)) {
                    SchemaGroup group = (SchemaGroup)this.objectStack.peek();
                    group.addSchema((Schema)element);
                } else {
                    this.constructedMart.addSchema((Schema)element);
                }
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        
        // Table (inside table provider).
        else if ("table".equals(eName)) {
            // What table provider does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Schema))
                throw new SAXException(BuilderBundle.getString("tableOutsideSchema"));
            Schema schema = (Schema)this.objectStack.peek();
            
            // Get the name and id as this is a common feature.
            String name = (String)attributes.get("name");
            String id = (String)attributes.get("id");
            try {
                // DataSet table provider?
                if (schema instanceof DataSet) {
                    // Get the additional attributes.
                    String type = (String)attributes.get("type");
                    String underTabId = (String)attributes.get("underlyingTableId");
                    DataSetTableType dsType = null;
                    if (type.equals("MAIN")) dsType = DataSetTableType.MAIN;
                    else if (type.equals("MAIN_SUBCLASS")) dsType = DataSetTableType.MAIN_SUBCLASS;
                    else if (type.equals("DIMENSION")) dsType = DataSetTableType.DIMENSION;
                    else throw new SAXException(BuilderBundle.getString("unknownDatasetTableType",type));
                    Table underlyingTable = null;
                    if (underTabId!=null && underTabId.trim().length()!=0) underlyingTable = (Table)this.mappedObjects.get(underTabId);
                    DataSetTable dst = new DataSetTable(name, (DataSet)schema, dsType, underlyingTable);
                    element = dst;
                    
                    // Read the underlying relations.
                    String[] underRelIds = ((String)attributes.get("underlyingRelationIds")).split("\\s*,\\s*");
                    List underRels = new ArrayList();
                    for (int i = 0; i < underRelIds.length; i++) underRels.add(this.mappedObjects.get(underRelIds[i]));
                    dst.setUnderlyingRelations(underRels);
                }
                // Generic table provider?
                else if (schema instanceof GenericSchema) {
                    element = new GenericTable(name, schema);
                }
                // Others
                else
                    throw new SAXException(BuilderBundle.getString("unknownSchemaType",schema.getClass().getName()));
            } catch (Exception e) {
                throw new SAXException(e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Column (inside table).
        else if ("column".equals(eName)) {
            // What table does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Table))
                throw new SAXException(BuilderBundle.getString("columnOutsideTable"));
            Table tbl = (Table)this.objectStack.peek();
            
            // Get the id as this is a common feature.
            String id = (String)attributes.get("id");
            String name = (String)attributes.get("name");
            try {
                // OLDDataSet column?
                if (tbl instanceof DataSetTable) {
                    // Work out type and relation.
                    String type = (String)attributes.get("type");
                    String underlyingRelationId = (String)attributes.get("underlyingRelationId");
                    Relation underlyingRelation = null;
                    if (!"null".equals(underlyingRelationId)) underlyingRelation = (Relation)this.mappedObjects.get(underlyingRelationId);
                    // Concat relation column?
                    if ("concatRelation".equals(type)) {
                        element = new ConcatRelationColumn(name, (DataSetTable)tbl, underlyingRelation);
                    }
                    // Schema name column?
                    else if ("schemaName".equals(type)) {
                        element = new SchemaNameColumn(name, (DataSetTable)tbl);
                    }
                    // Wrapped column?
                    else if ("wrapped".equals(type)) {
                        Column wrappedCol = (Column)this.mappedObjects.get(attributes.get("wrappedColumnId"));
                        element = new WrappedColumn(wrappedCol, (DataSetTable)tbl, underlyingRelation);
                        // Override any aliased names.
                    }
                    // Others.
                    else
                        throw new SAXException(BuilderBundle.getString("unknownColumnType",type));
                    
                    // Override the name, to make sure we get the same alias as the original.
                    ((DataSetColumn)element).setName(name);
                }
                // Generic column?
                else if (tbl instanceof GenericTable) {
                    element = new GenericColumn(name, tbl);
                }
                // Others
                else
                    throw new SAXException(BuilderBundle.getString("unknownTableType",tbl.getClass().getName()));
            } catch (SAXException e) {
                throw e;
            } catch (Exception e) {
                throw new SAXException(e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Primary key (inside table).
        else if ("primaryKey".equals(eName)) {
            // What table does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Table))
                throw new SAXException("pkOutsideTable");
            Table tbl = (Table)this.objectStack.peek();
            
            // Get the ID.
            String id = (String)attributes.get("id");
            try {
                ComponentStatus status = ComponentStatus.get((String)attributes.get("status"));
                // Decode the column IDs from the comma-separated list.
                String[] pkColIds = ((String)attributes.get("columnIds")).split("\\s*,\\s*");
                List pkCols = new ArrayList();
                for (int i = 0; i < pkColIds.length; i++) pkCols.add(this.mappedObjects.get(pkColIds[i]));
                
                // Make the key.
                PrimaryKey pk = new GenericPrimaryKey(pkCols);
                pk.setStatus(status);
                tbl.setPrimaryKey(pk);
                element = pk;
            } catch (Exception e) {
                throw new SAXException(e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Foreign key (inside table).
        else if ("foreignKey".equals(eName)) {
            // What table does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof Table))
                throw new SAXException(BuilderBundle.getString("fkOutsideTable"));
            Table tbl = (Table)this.objectStack.peek();
            
            // Get the ID.
            String id = (String)attributes.get("id");
            try {
                ComponentStatus status = ComponentStatus.get((String)attributes.get("status"));
                // Decode the column IDs from the comma-separated list.
                String[] fkColIds = ((String)attributes.get("columnIds")).split("\\s*,\\s*");
                List fkCols = new ArrayList();
                for (int i = 0; i < fkColIds.length; i++) fkCols.add(this.mappedObjects.get(fkColIds[i]));
                
                // Make the key.
                ForeignKey fk = new GenericForeignKey(fkCols);
                fk.setStatus(status);
                tbl.addForeignKey(fk);
                element = fk;
            } catch (Exception e) {
                throw new SAXException(e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Relation (anywhere).
        else if ("relation".equals(eName)) {
            // Get the ID.
            String id = (String)attributes.get("id");
            try {
                ComponentStatus status = ComponentStatus.get((String)attributes.get("status"));
                Cardinality card = Cardinality.get((String)attributes.get("fkCardinality"));
                PrimaryKey pk = (PrimaryKey)this.mappedObjects.get(attributes.get("primaryKeyId"));
                ForeignKey fk = (ForeignKey)this.mappedObjects.get(attributes.get("foreignKeyId"));
                
                // Make it
                Relation rel = new GenericRelation(pk, fk, card);
                
                // Set its status.
                rel.setStatus(status);
                element = rel;
            } catch (Exception e) {
                throw new SAXException(e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // MartConstructor (anywhere).
        else if ("genericMartConstructor".equals(eName)) {
            String id = (String)attributes.get("id");
            String name = (String)attributes.get("name");
            
            try {
                element = new GenericMartConstructor(name);
            } catch (Exception e) {
                throw new SAXException(e);
            }
            
            // Store it in the map of IDed objects.
            this.mappedObjects.put(id, element);
        }
        
        // Masked Relation (inside window).
        else if ("maskedRelation".equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof DataSet))
                throw new SAXException(BuilderBundle.getString("maskedRelationOutsideDataSet"));
            DataSet w = (DataSet)this.objectStack.peek();
            
            try {
                Relation rel = (Relation)this.mappedObjects.get(attributes.get("relationId"));
                w.maskRelation(rel);
                element = rel;
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        
        // Subclass Relation (inside window).
        else if ("subclassRelation".equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof DataSet))
                throw new SAXException(BuilderBundle.getString("subclassRelationOutsideDataSet"));
            DataSet w = (DataSet)this.objectStack.peek();
            
            try {
                Relation rel = (Relation)this.mappedObjects.get(attributes.get("relationId"));
                w.flagSubclassRelation(rel);
                element = rel;
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        
        // Concat Relation (inside window).
        else if ("concatRelation".equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof DataSet))
                throw new SAXException(BuilderBundle.getString("concatRelationOutsideDataSet"));
            DataSet w = (DataSet)this.objectStack.peek();
            
            try {
                Relation rel = (Relation)this.mappedObjects.get(attributes.get("relationId"));
                String type = (String)attributes.get("concatRelationType");
                ConcatRelationType crType = null;
                if (type.equals("COMMA")) crType = ConcatRelationType.COMMA;
                else if (type.equals("SPACE")) crType = ConcatRelationType.SPACE;
                else if (type.equals("TAB")) crType = ConcatRelationType.TAB;
                else throw new SAXException(BuilderBundle.getString("unknownConcatRelationType",type));
                w.flagConcatOnlyRelation(rel, crType);
                element = rel;
            } catch (SAXException e) {
                throw e;
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        
        // Masked Column (inside window).
        else if ("maskedColumn".equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof DataSet))
                throw new SAXException(BuilderBundle.getString("maskedColumnOutsideDataSet"));
            DataSet w = (DataSet)this.objectStack.peek();
            
            try {
                DataSetColumn col = (DataSetColumn)this.mappedObjects.get(attributes.get("columnId"));
                w.maskDataSetColumn(col);
                element = col;
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        
        // Partition Column (inside window).
        else if ("partitionColumn".equals(eName)) {
            // What window does it belong to?
            if (this.objectStack.empty() || !(this.objectStack.peek() instanceof DataSet))
                throw new SAXException(BuilderBundle.getString("partitionColumnOutsideDataSet"));
            DataSet w = (DataSet)this.objectStack.peek();
            
            try {
                WrappedColumn col = (WrappedColumn)this.mappedObjects.get(attributes.get("columnId"));
                String type = (String)attributes.get("partitionedColumnType");
                PartitionedColumnType resolvedType = null;
                
                // What kind of partition is it?
                // Single value partition?
                if ("singleValue".equals(type)) {
                    String value = null;
                    boolean useNull = ((String)attributes.get("useNull")).equals("true");
                    if (!useNull) value = (String)attributes.get("value");
                    resolvedType = new SingleValue(value, useNull);
                }
                // Unique values partition?
                else if ("uniqueValues".equals(type)) {
                    resolvedType = new UniqueValues();
                }
                // Values collection partition?
                else if ("valueCollection".equals(type)) {
                    // Values are comma-separated.
                    List valueList = new ArrayList();
                    if (attributes.containsKey("values")) valueList.addAll(Arrays.asList(((String)attributes.get("values")).split("\\s*,\\s*")));
                    boolean includeNull = ((String)attributes.get("useNull")).equals("true");
                    // Make the collection.
                    resolvedType = new ValueCollection(valueList, includeNull);
                }
                // Others.
                else
                    throw new SAXException(BuilderBundle.getString("unknownPartitionColumnType",type));
                
                // Flag the column.
                w.flagPartitionedWrappedColumn(col, resolvedType);
                element = col;
            } catch (SAXException e) {
                throw e;
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
        
        // OLDDataSet (inside window).
        else if ("dataset".equals(eName)) {
            try {
                String name = (String)attributes.get("name");
                Boolean partitionOnSchema = Boolean.valueOf((String)attributes.get("partitionOnSchema"));
                Table centralTable = (Table)this.mappedObjects.get(attributes.get("centralTableId"));
                String optType = (String)attributes.get("optimiser");
                MartConstructor mc = (MartConstructor)this.mappedObjects.get(attributes.get("martConstructorId"));
                
                DataSet ds = new DataSet(this.constructedMart, centralTable, name);
                DataSetOptimiserType opt = null;
                if ("NONE".equals(optType)) opt = DataSetOptimiserType.NONE;
                else if ("LEFTJOIN".equals(optType)) opt = DataSetOptimiserType.LEFTJOIN;
                else if ("COLUMN".equals(optType)) opt = DataSetOptimiserType.COLUMN;
                else if ("TABLE".equals(optType)) opt = DataSetOptimiserType.TABLE;
                else throw new SAXException(BuilderBundle.getString("unknownOptimiserType",optType));
                ds.setMartConstructor(mc);
                ds.setDataSetOptimiserType(opt);
                if (partitionOnSchema!=null) ds.setPartitionOnSchema(partitionOnSchema.booleanValue());
                element = ds;
            } catch (SAXException e) {
                throw e;
            } catch (Exception e) {
                throw new SAXException(e);
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
     * @throws IOException if it failed to write it.
     */
    private void openElement(String name) throws IOException {
        // Are we already partway through one?
        if (this.currentOutputElement != null) {
            // Yes, so put closing angle bracket on it.
            this.xmlWriter.write(">\n");
            // Increase the indent.
            this.currentOutputIndent++;
        }
        // Write the tag.
        for (int i = this.currentOutputIndent; i > 0; i--) this.xmlWriter.write("\t");
        this.xmlWriter.write("<");
        this.xmlWriter.write(name);
        this.currentOutputElement = name;
    }
    
    /**
     * Internal method which closes a tag in the output stream.
     * @param name the tag to close.
     * @throws IOException if it failed to write it.
     */
    private void closeElement(String name) throws IOException {
        // Can we use the simple /> method?
        if (this.currentOutputElement !=null && name.equals(this.currentOutputElement)) {
            // Yes, so put closing angle bracket on it.
            this.xmlWriter.write("/>\n");
        } else {
            // No, so use the full technique.
            // Decrease the indent.
            this.currentOutputIndent--;
            // Output the tag.
            for (int i = this.currentOutputIndent; i > 0; i--) this.xmlWriter.write("\t");
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
     * @throws IOException if it failed to write it.
     */
    private void writeAttribute(String name, String value) throws IOException {
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
     * @throws IOException if it failed to write it.
     */
    private void writeAttribute(String name, String[] values) throws IOException {
        // Write it.
        StringBuffer sb = new StringBuffer();
        for (int i  = 0; i < values.length; i++) {
            if (i>0) sb.append(",");
            sb.append(values[i]);
        }
        this.writeAttribute(name, sb.toString());
    }
    
    private void writeSchema(Schema schema) throws IOException, AssociationException {
        // What kind of tbl prov is it?
        // JDBC?
        if (schema instanceof JDBCSchema) {
            this.openElement("jdbcSchema");
            JDBCSchema jdbcSchema = (JDBCSchema)schema;
            
            if (jdbcSchema.getDriverClassLocation()!=null)
                this.writeAttribute("driverClassLocation", jdbcSchema.getDriverClassLocation().getPath());
            this.writeAttribute("driverClassName", jdbcSchema.getDriverClassName());
            this.writeAttribute("url", jdbcSchema.getJDBCURL());
            this.writeAttribute("username", jdbcSchema.getUsername());
            if (jdbcSchema.getPassword() != null)
                this.writeAttribute("password", jdbcSchema.getPassword());
            this.writeAttribute("name", jdbcSchema.getName());
            this.writeAttribute("keyguessing", jdbcSchema.getKeyGuessing()?"true":"false");
        }
        // Others?
        else
            throw new AssociationException(BuilderBundle.getString("unknownSchemaType",schema.getClass().getName()));
        
        // Write out the contents, and note the relations.
        this.writeSchemaContents(schema);
        
        // What kind of tbl prov was it?
        // JDBC?
        if (schema instanceof JDBCSchema) {
            this.closeElement("jdbcSchema");
        }
        // Others?
        else
            throw new AssociationException(BuilderBundle.getString("unknownSchemaType",schema.getClass().getName()));
    }
    
    /**
     * Internal method which writes out the tables of a table provider and remembers
     * the relations it saw as it goes along.
     *
     *
     * @param schema the {@link Schema} to write out the tables of.
     * @param relations the set of {@link Relation}s we found on the way.
     * @throws IOException if there was a problem writing to file.
     * @throws AssociationException if an unwritable kind of object was found.
     */
    private void writeSchemaContents(Schema schema) throws IOException, AssociationException {
        // Write out tables inside each provider. WITH IDS.
        for (Iterator ti = schema.getTables().iterator(); ti.hasNext(); ) {
            Table table = (Table)ti.next();
            String tableMappedID = ""+this.ID++;
            this.reverseMappedObjects.put(table, tableMappedID);
            
            // Start table.
            this.openElement("table");
            this.writeAttribute("id", tableMappedID);
            this.writeAttribute("name", table.getName());
            
            // A dataset table?
            if (table instanceof DataSetTable) {
                // Write the type.
                this.writeAttribute("type",((DataSetTable)table).getType().getName());
                Table underlyingTable = ((DataSetTable)table).getUnderlyingTable();
                if (underlyingTable != null) this.writeAttribute("underlyingTableId", (String)this.reverseMappedObjects.get(underlyingTable));
                
                // Write out the underlying relations.
                List underRelIds = new ArrayList();
                for (Iterator i = ((DataSetTable)table).getUnderlyingRelations().iterator(); i.hasNext(); ) underRelIds.add(this.reverseMappedObjects.get(i.next()));
                this.writeAttribute("underlyingRelationIds",(String[])underRelIds.toArray(new String[0]));
            }
            
            // Write out columns inside each table. WITH IDS.
            for (Iterator ci = table.getColumns().iterator(); ci.hasNext(); ) {
                Column col = (Column)ci.next();
                String colMappedID = ""+this.ID++;
                this.reverseMappedObjects.put(col, colMappedID);
                
                // Start column.
                this.openElement("column");
                this.writeAttribute("id", colMappedID);
                this.writeAttribute("name",col.getName());
                
                // Dataset column?
                if (col instanceof DataSetColumn) {
                    DataSetColumn dcol = (DataSetColumn)col;
                    Relation underlyingRelation = dcol.getUnderlyingRelation();
                    String underlyingRelationId = "null";
                    if (underlyingRelation != null) underlyingRelationId = (String)this.reverseMappedObjects.get(underlyingRelation);
                    this.writeAttribute("underlyingRelationId",underlyingRelationId);
                    this.writeAttribute("alt",underlyingRelation==null?"null":underlyingRelation.toString());
                    // Concat relation column?
                    if (dcol instanceof ConcatRelationColumn) {
                        this.writeAttribute("type","concatRelation");
                    }
                    // Schema name column?
                    else if (dcol instanceof SchemaNameColumn) {
                        this.writeAttribute("type","schemaName");
                    }
                    // Wrapped column?
                    else if (dcol instanceof WrappedColumn) {
                        this.writeAttribute("type","wrapped");
                        this.writeAttribute("wrappedColumnId",(String)this.reverseMappedObjects.get(((WrappedColumn)dcol).getWrappedColumn()));
                        this.writeAttribute("wrappedColumnAlt",((WrappedColumn)dcol).getWrappedColumn().toString());
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
                this.closeElement("column");
            }
            
            // Write out keys inside each table. WITH IDS. Remember relations as we go along.
            for (Iterator ki = table.getKeys().iterator(); ki.hasNext(); ) {
                Key key = (Key)ki.next();
                String keyMappedID = ""+this.ID++;
                this.reverseMappedObjects.put(key, keyMappedID);
                
                String elem = null;
                if (key instanceof PrimaryKey)
                    elem = "primaryKey";
                else if (key instanceof ForeignKey)
                    elem = "foreignKey";
                else
                    throw new AssociationException(BuilderBundle.getString("unknownKey",key.getClass().getName()));
                
                this.openElement(elem);
                this.writeAttribute("id", keyMappedID);
                List columnIds = new ArrayList();
                for (Iterator kci = key.getColumns().iterator(); kci.hasNext(); ) columnIds.add(this.reverseMappedObjects.get(kci.next()));
                this.writeAttribute("columnIds", (String[])columnIds.toArray(new String[0]));
                this.writeAttribute("status", key.getStatus().getName());
                this.writeAttribute("alt", key.toString());
                this.closeElement(elem);
            }
            
            // Finish table.
            this.closeElement("table");
        }
        
        // Write relations.
        this.writeRelations(schema.getInternalRelations());
    }
    
    /**
     * Internal method which writes out a set of relations.
     * @param relations the set of {@link Relation}s to write.
     * @throws IOException if there was a problem writing to file.
     */
    private void writeRelations(Collection relations) throws IOException {
        // Write out relations. WITH IDS.
        for (Iterator i = relations.iterator(); i.hasNext(); ) {
            Relation r = (Relation)i.next();
            String relMappedID = ""+this.ID++;
            this.reverseMappedObjects.put(r, relMappedID);
            this.openElement("relation");
            this.writeAttribute("id", relMappedID);
            this.writeAttribute("fkCardinality", r.getFKCardinality().getName());
            this.writeAttribute("primaryKeyId", (String)this.reverseMappedObjects.get(r.getPrimaryKey()));
            this.writeAttribute("foreignKeyId", (String)this.reverseMappedObjects.get(r.getForeignKey()));
            this.writeAttribute("status", r.getStatus().toString());
            this.writeAttribute("alt", r.toString());
            this.closeElement("relation");
        }
    }
    
    /**
     * Internal method which does the work of writing out XML files and
     * generating those funky ID tags you see in them.
     *
     * @param mart the mart to write.
     * @param ow the Writer to write the XML to.
     * @throws IOException if a write error occurs.
     * @throws AssociationException if it encounters an object not writable under the current DTD.
     */
    private void writeXML(Mart mart, Writer ow) throws IOException, AssociationException {
        // Remember the output stream.
        this.xmlWriter = ow;
        
        // Write the headers.
        this.xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        //this.xmlWriter.write("<\!DOCTYPE mart PUBLIC \"-//EBI//DTD MartBuilder "+MartBuilderXML.DTD_VERSION+"//EN\" \"http\://www.biomart.org/TR/MartBuilder-"+MartBuilderXML.DTD_VERSION+"/DTD/mart.dtd\">\n");
        
        // Initialise the ID counter.
        this.reverseMappedObjects = new HashMap();
        
        // Start by enclosing the whole lot in a <mart> tag.
        this.openElement("mart");
        
        // MartConstructor (anywhere).
        List martConstructors = new ArrayList();
        for (Iterator i = mart.getDataSets().iterator(); i.hasNext(); ) {
            DataSet ds = (DataSet)i.next();
            MartConstructor mc = ds.getMartConstructor();
            martConstructors.add(mc);
        }
        for (Iterator i = martConstructors.iterator(); i.hasNext(); ) {
            MartConstructor mc = (MartConstructor)i.next();
            String mcMappedID = ""+this.ID++;
            this.reverseMappedObjects.put(mc, mcMappedID);
            
            // Generic constructor?
            if (mc instanceof GenericMartConstructor) {
                this.openElement("genericMartConstructor");
                this.writeAttribute("id",mcMappedID);
                this.writeAttribute("name",mc.getName());
                this.closeElement("genericMartConstructor");
            }
            // Others?
            else
                throw new AssociationException(BuilderBundle.getString("unknownMartConstuctorType",mc.getClass().getName()));
        }
        
        // Write out each schema.
        List externalRelations = new ArrayList();
        for (Iterator i = mart.getSchemas().iterator(); i.hasNext(); ) {
            Schema schema = (Schema)i.next();
            if (schema instanceof SchemaGroup) {
                this.openElement("schemaGroup");
                this.writeAttribute("name", schema.getName());
                // Write group itself.
                this.writeSchemaContents(schema);
                // Write member schemas.
                for (Iterator j = ((SchemaGroup)schema).getSchemas().iterator(); j.hasNext(); )
                    this.writeSchema((Schema)j.next());
                this.closeElement("schemaGroup");
            } else {
                this.writeSchema(schema);
            }
            externalRelations.addAll(schema.getExternalRelations());
        }
        
        // Write out relations. WITH IDS.
        this.writeRelations(externalRelations);
        
        // Write out mart constructors. Remember windows as we go along.
        
        // Write out windows.
        for (Iterator dsi = mart.getDataSets().iterator(); dsi.hasNext(); ) {
            DataSet ds = (DataSet)dsi.next();
            this.openElement("dataset");
            this.writeAttribute("name",ds.getName());
            this.writeAttribute("centralTableId",(String)this.reverseMappedObjects.get(ds.getCentralTable()));
            this.writeAttribute("alt",ds.getCentralTable().toString());
            this.writeAttribute("optimiser",ds.getDataSetOptimiserType().getName());
            this.writeAttribute("martConstructorId",(String)this.reverseMappedObjects.get(ds.getMartConstructor()));
            this.writeAttribute("partitionOnSchema",Boolean.toString(ds.getPartitionOnSchema()));
            
            // Write out concat relations inside window. MUST come first else the dataset
            // concat-only cols will complain about not having a relation to refer to.
            for (Iterator x = ds.getConcatOnlyRelations().iterator(); x.hasNext(); ) {
                Relation r = (Relation)x.next();
                this.openElement("concatRelation");
                this.writeAttribute("relationId",(String)this.reverseMappedObjects.get(r));
                this.writeAttribute("concatRelationType",ds.getConcatRelationType(r).getName());
                this.writeAttribute("alt",r.toString());
                this.closeElement("concatRelation");
            }
            
            // Write out masked relations inside window. Can go before or after.
            for (Iterator x = ds.getMaskedRelations().iterator(); x.hasNext(); ) {
                Relation r = (Relation)x.next();
                this.openElement("maskedRelation");
                this.writeAttribute("relationId",(String)this.reverseMappedObjects.get(r));
                this.writeAttribute("alt",r.toString());
                this.closeElement("maskedRelation");
            }
            
            // Write out subclass relations inside window. Can go before or after.
            for (Iterator x = ds.getSubclassedRelations().iterator(); x.hasNext(); ) {
                Relation r = (Relation)x.next();
                this.openElement("subclassRelation");
                this.writeAttribute("relationId",(String)this.reverseMappedObjects.get(r));
                this.writeAttribute("alt",r.toString());
                this.closeElement("subclassRelation");
            }
                        
            // Write out the contents of the dataset, and note the relations.
            this.writeSchemaContents(ds);
            
            // Write out masked columns inside window. MUST go after else will not have
            // dataset cols to refer to.
            for (Iterator x = ds.getMaskedDataSetColumns().iterator(); x.hasNext(); ) {
                Column c = (Column)x.next();
                this.openElement("maskedColumn");
                this.writeAttribute("columnId",(String)this.reverseMappedObjects.get(c));
                this.writeAttribute("alt",c.toString());
                this.closeElement("maskedColumn");
            }
            
            // Write out partitioned columns inside window. MUST go after else will not have
            // dataset cols to refer to.
            for (Iterator x = ds.getPartitionedWrappedColumns().iterator(); x.hasNext(); ) {
                WrappedColumn c = (WrappedColumn)x.next();
                this.openElement("partitionColumn");
                this.writeAttribute("columnId",(String)this.reverseMappedObjects.get(c));
                PartitionedColumnType ptc = ds.getPartitionedWrappedColumnType(c);
                
                // What kind of partition is it?
                // Single value partition?
                if (ptc instanceof SingleValue) {
                    SingleValue sv = (SingleValue)ptc;
                    this.writeAttribute("partitionedColumnType","singleValue");
                    String value = sv.getValue();
                    this.writeAttribute("useNull",sv.getIncludeNull()?"true":"false");
                    if (value!=null) this.writeAttribute("value",value);
                }
                // Unique values partition?
                else if (ptc instanceof UniqueValues) {
                    this.writeAttribute("partitionedColumnType","uniqueValues");
                    // No extra attributes required.
                }
                // Values collection partition?
                else if (ptc instanceof ValueCollection) {
                    ValueCollection vc = (ValueCollection)ptc;
                    this.writeAttribute("partitionedColumnType","valueCollection");
                    // Values are comma-separated.
                    List valueList = new ArrayList();
                    valueList.addAll(vc.getValues());
                    this.writeAttribute("useNull",vc.getIncludeNull()?"true":"false");
                    if (!valueList.isEmpty()) this.writeAttribute("values",(String[])valueList.toArray(new String[0]));
                }
                // Others.
                else
                    throw new AssociationException(BuilderBundle.getString("unknownPartitionColumnType",ptc.getClass().getName()));
                
                // Finish off.
                this.writeAttribute("alt",c.toString());
                this.closeElement("partitionColumn");
            }
            
            // Write out relations inside dataset. WITH IDS.
            this.closeElement("dataset");
        }
        
        // Finished! Close the mart tag.
        this.closeElement("mart");
        
        // Flush.
        this.xmlWriter.flush();
    }
    
    /**
     * The load method takes a {@link File} and loads up a {@link Mart} object based
     * on the XML contents of the file. This XML is usually generated by the
     * {@link MartBuilderXML#save(Mart,File)} method.
     *
     *
     * @param file the {@link File} to load the data from.
     * @return a {@link Mart} object containing the data from the file.
     * @throws IOException if there was any problem reading the file.
     * @throws AssociationException if the content of the file was not a {@link Mart} object.
     */
    public static Mart load(File file) throws IOException, AssociationException {
        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // Parse the input
        MartBuilderXML loader = new MartBuilderXML();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(file, loader);
        } catch (ParserConfigurationException e) {
            throw new AssociationException(BuilderBundle.getString("XMLConfigFailed"), e);
        } catch (SAXException e) {
            throw new AssociationException(BuilderBundle.getString("XMLUnparseable"), e);
        }
        // Get the constructed object.
        Mart s = loader.getMart();
        // Check that it is a schema.
        if (s == null)
            throw new AssociationException(BuilderBundle.getString("fileNotSchemaVersion",DTD_VERSION));
        // Return.
        return s;
    }
    
    /**
     * The save method takes a {@link Mart} object and writes out XML describing it to
     * the given {@link File}. This XML can be read by the {@link MartBuilderXML#load(File)} method.
     *
     *
     * @param schema {@link Mart} object containing the data for the file.
     * @param file the {@link File} to save the data to.
     * @throws IOException if there was any problem writing the file.
     * @throws AssociationException if it encounters an object not writable under the current DTD.
     */
    public static void save(Mart schema, File file) throws IOException,  AssociationException {
        // Open the file.
        FileWriter fw = new FileWriter(file);
        // Write it out.
        (new MartBuilderXML()).writeXML(schema, fw);
        // Close the output stream.
        fw.close();
    }
}
