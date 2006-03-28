/*
 * DataSet.java
 *
 * Created on 27 March 2006, 14:24
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.biomart.builder.exceptions.AlreadyExistsException;
import org.biomart.builder.exceptions.AssociationException;
import org.biomart.builder.exceptions.BuilderException;
import org.biomart.builder.model.Column.GenericColumn;
import org.biomart.builder.model.Table.GenericTable;

/**
 * This is the heart of the whole system, and represents a single data set in a mart.
 * The generic implementation includes the algorithm which flattens tables down into
 * a set of mart tables based on the contents of a {@link Window}.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 28th March 2006
 * @since 0.1
 */
public interface DataSet extends Comparable {
    /**
     * Returns the {@link Window} this {@link DataSet} is constructed from.
     * @return the {@link Window} for this {@link DataSet}.
     */
    public Window getWindow();
    
    /**
     * Returns the {@link DataSetTable} representing the central fact table for this {@link DataSet}.
     * @return the {@link DataSetTable} representing the fact table for this {@link DataSet}.
     * @throws NullPointerException if it could not muster up a fact table.
     */
    public DataSetTable getFactTable() throws NullPointerException;
    
    /**
     * Rebuild this {@link DataSet} based on the contents of its parent {@link Window}.
     * It will attempt to keep any custom names etc. for tables and columns in the regenerated
     * version where they had been previously specified by the user.
     * @throws SQLException if there was a problem connecting to the data source.
     * @throws BuilderException if there was any other kind of problem.
     */
    public void regenerate() throws SQLException, BuilderException;
    
    /**
     * The generic version can construct itself from any given Window.
     */
    public class GenericDataSet implements DataSet {
        /**
         * Internal reference to the parent window.
         */
        private Window window;
        
        /**
         * Internal reference to the generated fact table.
         */
        private DataSetTable factTable;
        
        /**
         * The constructor links this {@link DataSet} with a specific {@link Window}.
         * @throws NullPointerException if the window is null.
         */
        public GenericDataSet(Window window) throws NullPointerException {
            // Sanity check.
            if (window==null)
                throw new NullPointerException("Parent window cannot be null.");
            // Do it.
            this.window = window;
        }
        
        /**
         * Returns the {@link Window} this {@link DataSet} is constructed from.
         * @return the {@link Window} for this {@link DataSet}.
         */
        public Window getWindow() {
            return this.window;
        }
        
        /**
         * Returns the {@link DataSetTable} representing the central fact table for this {@link DataSet}.
         * @return the {@link DataSetTable} representing the fact table for this {@link DataSet}.
         * @throws NullPointerException if it could not muster up a fact table.
         */
        public DataSetTable getFactTable() throws NullPointerException {
            // Sanity check.
            try {
                if (this.factTable==null) this.regenerate();
            } catch (Exception e) {
                NullPointerException npe = new NullPointerException("Unable to regenerate DataSet.");
                npe.initCause(e);
                throw npe;
            }
            if (this.factTable==null)
                throw new NullPointerException("Unable to construct a fact table. Does the parent Window have a central table?");
            // Do it.
            return this.factTable;
        }
        
        /**
         * Rebuild this {@link DataSet} based on the contents of its parent {@link Window}.
         * It will attempt to keep any custom names etc. for tables and columns in the regenerated
         * version where they had been previously specified by the user.
         * @throws SQLException if there was a problem connecting to the data source.
         * @throws BuilderException if there was any other kind of problem.
         */
        public void regenerate() throws SQLException, BuilderException {
            // TODO: do the flattening work here!
        }
        
        /**
         * Displays the name of this {@link DataSet} object. The name is the same as the
         * parent {@link Window}.
         * @return the name of this {@link DataSet} object.
         */
        public String toString() {
            return this.getWindow().getName();
        }
        
        /**
         * Displays the hashcode of this object.
         * @return the hashcode of this object.
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * Sorts by comparing the toString() output.
         * @param o the object to compare to.
         * @return -1 if we are smaller, +1 if we are larger, 0 if we are equal.
         * @throws ClassCastException if the object o is not a {@link DataSet}.
         */
        public int compareTo(Object o) throws ClassCastException {
            DataSet d = (DataSet)o;
            return this.toString().compareTo(d.toString());
        }
        
        /**
         * Return true if the toString()s are identical.
         * @param o the object to compare to.
         * @return true if the toString()s match and both objects are {@link DataSet}s,
         * otherwise false.
         */
        public boolean equals(Object o) {
            if (o==null || !(o instanceof DataSet)) return false;
            DataSet d = (DataSet)o;
            return d.toString().equals(this.toString());
        }
    }
    
    /**
     * This special {@link Table} represents the merge of one or more other {@link Table}s
     * by following a series of {@link Relation}s. As such it has no real columns of its own,
     * so every column is from another table and is given an alias.
     */
    public class DataSetTable extends GenericTable {
        /**
         * Internal reference to the list of {@link Relation}s to follow to construct this table.
         */
        private final List relations = new ArrayList();
        
        /**
         * Internal reference to the type of this table.
         */
        private final DataSetTableType type;
        
        /**
         * The constructor calls the parent {@link GenericTable} constructor. It uses the default
         * TableProvider.DATASET {@link TableProvider} as a dummy parent for itself. You must
         * also supply a type that describes this as a fact table, dimension table, etc.
         * @param name the table name.
         * @param type the {@link DataSetTableType} that best describes this table.
         * @throws NullPointerException if the name or type are null.
         * @throws AlreadyExistsException if the provider, for whatever reason, refuses to
         * allow this {@link Table} to be added to it using {@link TableProvider#addTable(Table) addTable()}.
         */
        public DataSetTable(String name, DataSetTableType type) throws AlreadyExistsException, NullPointerException {
            // Super call first.
            super(name,TableProvider.DATASET);
            // Sanity check.
            if (type==null)
                throw new NullPointerException("Table type cannot be null.");
            // Do the work.
            this.type = type;
        }
        
        /**
         * Returns the type of this table specified at construction time.
         * @return the type of this table.
         */
        public DataSetTableType getType() {
            return this.type;
        }
        
        /**
         * <p>Attemps to add a {@link Column} to this table. The {@link Column} will already
         * have had it's {@link Table} parameter set to match, otherwise an
         * {@link IllegalArgumentException} will be thrown. That exception will also get thrown
         * if the {@link Column} has the same name as an existing one on this table.</p>
         *
         * <p>{@link DataSetTable}s insist that all columns are {@link DataSetColumn}s.</p>
         *
         * @param c the {@link Column} to add, which must be a {@link DataSetColumn}..
         * @throws AlreadyExistsException if the {@link Column} name has already been used on
         * this {@link Table}.
         * @throws AssociationException if the {@link Table} parameter of the {@link Column}
         * does not match.
         * @throws NullPointerException if the {@link Column} object is null.
         */
        public void addColumn(Column c) throws AlreadyExistsException, AssociationException, NullPointerException {
            // Sanity check.
            if (!(c instanceof DataSetColumn))
                throw new AssociationException("Column must be a DataSetColumn to be added to a DataSetTable.");
            // Do the work.
            super.addColumn(c);
        }
        
        /**
         * <p>Convenience method that creates and adds a {@link Column} to this {@link Table}.
         * If a {@link Column} with the same name already exists an exception will be thrown.</p>
         *
         * <p>This implementation does not allow columns to be directly created. An
         * AssertionError will be thrown if this method is called.</p>
         *
         * @param name the name of the {@link Column} to create and add.
         * @throws AlreadyExistsException if a {@link Column} with the same name already
         * exists in this {@link Table}.
         * @throws NullPointerException if the name argument is null.
         */
        public void createColumn(String name) throws AlreadyExistsException, NullPointerException {
            throw new AssertionError("Columns cannot be created on a DataSetTable.");
        }
        
        /**
         * Convenience method that wraps a {@link Column} and adds it to this {@link Table}.
         * @param c the {@link Column} to wrap and add.
         * @throws NullPointerException if the argument is null.
         */
        public void createColumn(Column c) throws NullPointerException {
            new DataSetColumn(c, this);
            // By creating it we've already added it to ourselves! (Based on DataSetColumn behaviour)
        }
        
        /**
         * Convenience method that returns all column names already used by this table.
         * @return a set of names, never null but maybe empty.
         */
        public Collection getUsedColumnNames() {
            Set names = new HashSet();
            for (Iterator i = this.getColumns().iterator(); i.hasNext(); ) {
                Column c = (Column)i.next();
                names.add(c.getName());
            }
            return names;
        }
    }
    
    /**
     * A column on a {@link DataSetTable} wraps an existing column but is otherwise identical to
     * a normal column. It assigns itself an alias if the original name is already used in the target table.
     * Can be used in keys on dataset tables.
     */
    public class DataSetColumn extends GenericColumn {
        /**
         * Internal reference to the wrapped {@link Column}.
         */
        private final Column c;
        
        /**
         * This constructor wraps an existing {@link Column} and checks that the
         * parent {@link Table} is not null. It also assigns an alias to the wrapped {@link Column}
         * if another one with the same name already exists on this table.
         * @param c the {@link Column} to wrap.
         * @param t the parent {@link Table}
         * @throws NullPointerException if either parameter is null.
         */
        public DataSetColumn(Column c, DataSetTable t) throws NullPointerException {
            // Sanity checks
            if (c==null)
                throw new NullPointerException("Wrapped column cannot be null.");
            if (t==null)
                throw new NullPointerException("Parent table cannot be null.");
            // Remember the values.
            this.c = c;
            this.t = t;
            // Work out the alias if name used already, otherwise use name as-is.
            this.name = c.getName();
            int aliasNumber = 2;
            Collection usedColumnNames = t.getUsedColumnNames();
            while (usedColumnNames.contains(this.name)) {
                // Alias is original name appended with _2, _3, _4 etc.
                this.name = c.getName()+"_"+(aliasNumber++);
            }
            // Add it to the table - throws AssociationException and AlreadyExistsException
            try {
                t.addColumn(this);
            } catch (AssociationException e) {
                throw new AssertionError("Table does not equal itself.");
            } catch (AlreadyExistsException e) {
                throw new AssertionError("Table does not report duplicate column names correctly.");
            }
        }
    }
    
    /**
     * This class defines the various different types of DataSetTable there are.
     */
    public class DataSetTableType implements Comparable {
        /**
         * Use this constant to refer to a fact table.
         */
        public static final DataSetTableType FACT = DataSetTableType.get("FACT");
        
        /**
         * Use this constant to refer to a subclass of a fact table.
         */
        public static final DataSetTableType FACT_SUBCLASS = DataSetTableType.get("FACT_SUBCLASS");
        
        /**
         * Use this constant to refer to a dimension table.
         */
        public static final DataSetTableType DIMENSION = DataSetTableType.get("DIMENSION");
        
        /**
         * Internal reference to the name of this {@link DataSetTableType}.
         */
        private final String name;
        
        /**
         * Internal reference to the set of {@link DataSetTableType} singletons.
         */
        private static final Map singletons = new HashMap();
        
        /**
         * The static factory method creates and returns a {@link DataSetTableType}
         * with the given name. It ensures the object returned is a singleton.
         * Note that the names of {@link DataSetTableType} objects are case-insensitive.
         * @param name the name of the {@link DataSetTableType} object.
         * @return the {@link DataSetTableType} object.
         */
        public static DataSetTableType get(String name) {
            // Convert to upper case.
            name = name.toUpperCase();
            // Do we already have this one?
            // If so, then return it.
            if (singletons.containsKey(name)) return (DataSetTableType)singletons.get(name);
            // Otherwise, create it, remember it, then return it.
            DataSetTableType s = new DataSetTableType(name);
            singletons.put(name,s);
            return s;
        }
        
        /**
         * The private constructor takes a single parameter, which defines the name
         * this {@link DataSetTableType} object will display when printed.
         * @param name the name of the {@link DataSetTableType}.
         */
        private DataSetTableType(String name) {
            this.name=name;
        }
        
        /**
         * Displays the name of this {@link DataSetTableType} object.
         * @return the name of this {@link DataSetTableType} object.
         */
        public String toString() {
            return this.name;
        }
        
        /**
         * Displays the hashcode of this object.
         * @return the hashcode of this object.
         */
        public int hashCode() {
            return this.toString().hashCode();
        }
        
        /**
         * Sorts by comparing the toString() output.
         * @param o the object to compare to.
         * @return -1 if we are smaller, +1 if we are larger, 0 if we are equal.
         * @throws ClassCastException if the object o is not a {@link DataSetTableType}.
         */
        public int compareTo(Object o) throws ClassCastException {
            DataSetTableType c = (DataSetTableType)o;
            return this.toString().compareTo(c.toString());
        }
        
        /**
         * Return true if the objects are identical.
         * @param o the object to compare to.
         * @return true if the names are the same and both are {@link DataSetTableType} instances,
         * otherwise false.
         */
        public boolean equals(Object o) {
            // We are dealing with singletons so can use == happily.
            return o==this;
        }
    }
}
