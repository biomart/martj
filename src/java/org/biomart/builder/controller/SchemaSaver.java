/*
 * SchemaSaver.java
 *
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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractBasicConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.ComponentStatus;
import org.biomart.builder.model.ConcatRelationType;
import org.biomart.builder.model.DataSet.DataSetColumn.ConcatRelationColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.HasDimensionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.TableProviderNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableProvider;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.GenericDataSet;
import org.biomart.builder.model.Key.CompoundForeignKey;
import org.biomart.builder.model.Key.CompoundPrimaryKey;
import org.biomart.builder.model.Key.SimpleForeignKey;
import org.biomart.builder.model.Key.SimplePrimaryKey;
import org.biomart.builder.model.PartitionedColumnType.SingleValue;
import org.biomart.builder.model.PartitionedColumnType.UniqueValues;
import org.biomart.builder.model.PartitionedColumnType.ValueCollection;
import org.biomart.builder.model.PartitionedTableProvider.GenericPartitionedTableProvider;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.model.Relation.OneToMany;
import org.biomart.builder.model.Relation.OneToOne;
import org.biomart.builder.model.Schema;
import org.biomart.builder.model.Table.GenericTable;
import org.biomart.builder.model.TableProvider.GenericTableProvider;
import org.biomart.builder.model.Window;

/**
 * <p>The SchemaSaver class provides two static methods which seralize and deserialize
 * {@link Schema} objects to/from a basic XML format. There is no formal DTD as the
 * structure of the XML reflects exactly the structure of the {@link Schema} object in
 * the data model, and the structure of all objects it contains.</p>
 *
 * <p>The serialization is achieved by using a third-party product entitled XStream, including
 * the optional XPP3 parser from the same developers. The two jars required, xpp3 and xstream,
 * can be downloaded from the project website at <a href="http://xstream.codehaus.org/">http://xstream.codehaus.org/</a>.
 * MartBuilder was developed and tested against version 1.3.3 of both jars. These jars are distributed under a BSD licence available at
 * <a href="http://xstream.codehaus.org/license.html">http://xstream.codehaus.org/license.html</a>.</p>
 *
 * <p>NOTE: The XML is version-specific. A formal DTD will be included with the first
 * official release of MartBuilder, and subsequent releases will include new DTDs (if any aspects
 * have changed) and converter tools to translate your old files.</p>
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 3rd April 2006
 * @since 0.1
 */
public class SchemaSaver {
    /**
     * Internal reference to an {@link XStream} with all the aliases set up.
     */
    private static XStream xstream;
    
    /**
     * The static initialiser creates the {@link XStream} and sets up all the aliases
     * and singlteton handlers.
     */
    static {
        xstream = new XStream();
        
        xstream.registerConverter(new ComponentStatusConverter());
        xstream.registerConverter(new ConcatRelationTypeConverter());
        xstream.registerConverter(new CardinalityConverter());
        
        xstream.alias("genericColumn",GenericColumn.class);
        xstream.alias("status",ComponentStatus.class);
        xstream.alias("concatType",ConcatRelationType.class);
        
        xstream.alias("dataset",GenericDataSet.class);
        xstream.alias("datasetTable",DataSetTable.class);
        xstream.alias("datasetTableProvider",DataSetTableProvider.class);
        xstream.alias("datasetTableType",DataSetTableType.class);
        
        xstream.alias("wrappedColumn",WrappedColumn.class);
        xstream.alias("hasDimensionColumn",HasDimensionColumn.class);
        xstream.alias("concatRelationColumn",ConcatRelationColumn.class);
        xstream.alias("tableProviderNameColumn",TableProviderNameColumn.class);
        
        xstream.alias("compoundPK",CompoundPrimaryKey.class);
        xstream.alias("simplePK",SimplePrimaryKey.class);
        xstream.alias("compoundFK",CompoundForeignKey.class);
        xstream.alias("simpleFK",SimpleForeignKey.class);
        
        xstream.alias("singleValuePartition",SingleValue.class);
        xstream.alias("valueCollectionPartition",ValueCollection.class);
        xstream.alias("uniqueValuesPartition",UniqueValues.class);
        
        xstream.alias("cardinality",Cardinality.class);
        xstream.alias("oneToMany",OneToMany.class);
        xstream.alias("oneToOne",OneToOne.class);
        xstream.alias("concatRelationType",ConcatRelationType.class);
        
        xstream.alias("genericTable",GenericTable.class);
        xstream.alias("genericTableProvider",GenericTableProvider.class);
        xstream.alias("genericPartitionedTableProvider",GenericPartitionedTableProvider.class);
        
        xstream.alias("window",Window.class);
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
            throw new NullPointerException("File argument cannot be null.");
        // Open the file.
        InputStream is = new FileInputStream(file);
        // Read it in.
        Object o = xstream.fromXML(is);
        // Check that it is a schema.
        if (!(o instanceof Schema))
            throw new AssociationException("File did not contain a Schema object.");
        // Close the input stream.
        is.close();
        // Return.
        return (Schema)o;
    }
    
    /**
     * The save method takes a {@link Schema} object and writes out XML describing it to
     * the given {@link File}. This XML can be read by the {@link SchemaSaver#load(File)} method.
     * @param s {@link Schema} object containing the data for the file.
     * @param file the {@link File} to save the data to.
     * @throws IOException if there was any problem writing the file.
     * @throws NullPointerException if the file or schema specified was null.
     */
    public static void save(Schema s, File file) throws IOException, NullPointerException {
        // Sanity check.
        if (file==null)
            throw new NullPointerException("File argument cannot be null.");
        if (s==null)
            throw new NullPointerException("Schema argument cannot be null.");
        // Open the file.
        OutputStream os = new FileOutputStream(file);
        // Read it in.
        xstream.toXML(s, os);
        // Close the output stream.
        os.close();
    }
    
    /**
     * This converter deals with the various ComponentStatus singletons we have in the object model.
     */
    protected static class ComponentStatusConverter extends AbstractBasicConverter {
        /**
         * Called by XStream to determine whether to use this converter instance to marshall a particular type.
         * @param type the type to check.
         * @return true if it can convert it, false if not.
         */
        public boolean canConvert(Class type) {
            return type.equals(ComponentStatus.class);
        }
        
        /**
         * Converts singletons from strings.
         * @param str the string representing the singleton.
         * @return the singleton.
         * @throws ConversionException if the conversion failed.
         */
        protected Object fromString(String str) throws ConversionException {
            return ComponentStatus.get(str);
        }
        
        /**
         * Converts singletons to strings.
         * @param obj the singleton.
         * @return the string representing the singleton.
         * @throws ConversionException if the conversion failed.
         */
        protected String toString(Object obj) throws ConversionException {
            return ((ComponentStatus)obj).toString();
        }
    }
    
    /**
     * This converter deals with the various ConcatRelationType singletons we have in the object model.
     */
    protected static class ConcatRelationTypeConverter extends AbstractBasicConverter {
        /**
         * Called by XStream to determine whether to use this converter instance to marshall a particular type.
         * @param type the type to check.
         * @return true if it can convert it, false if not.
         */
        public boolean canConvert(Class type) {
            return type.equals(ConcatRelationType.class);
        }
        
        /**
         * Converts singletons from strings.
         * @param str the string representing the singleton.
         * @return the singleton.
         * @throws ConversionException if the conversion failed.
         */
        protected Object fromString(String str) throws ConversionException {
            return ConcatRelationType.get(str);
        }
        
        /**
         * Converts singletons to strings.
         * @param obj the singleton.
         * @return the string representing the singleton.
         * @throws ConversionException if the conversion failed.
         */
        protected String toString(Object obj) throws ConversionException {
            return ((ConcatRelationType)obj).toString();
        }
    }
    
    /**
     * This converter deals with the various Cardinality singletons we have in the object model.
     */
    protected static class CardinalityConverter extends AbstractBasicConverter {
        /**
         * Called by XStream to determine whether to use this converter instance to marshall a particular type.
         * @param type the type to check.
         * @return true if it can convert it, false if not.
         */
        public boolean canConvert(Class type) {
            return type.equals(Cardinality.class);
        }
        
        /**
         * Converts singletons from strings.
         * @param str the string representing the singleton.
         * @return the singleton.
         * @throws ConversionException if the conversion failed.
         */
        protected Object fromString(String str) throws ConversionException {
            return Cardinality.get(str);
        }
        
        /**
         * Converts singletons to strings.
         * @param obj the singleton.
         * @return the string representing the singleton.
         * @throws ConversionException if the conversion failed.
         */
        protected String toString(Object obj) throws ConversionException {
            return ((Cardinality)obj).toString();
        }
    }
}
