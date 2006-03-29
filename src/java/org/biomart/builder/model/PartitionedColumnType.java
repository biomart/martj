/*
 * PartitionedColumnType.java
 *
 * Created on 27 March 2006, 16:23
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

import java.util.Collection;
import java.util.Collections;

/**
 * Represents a method of partitioning by column. There are no methods.
 * Actual logic to divide up by column is left to the DDL generator elsewhere
 * to decide by looking at the class used.
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.2, 29th March 2006
 * @since 0.1
 */
public interface PartitionedColumnType {
    /**
     * Use this class to refer to a column partitioned by every unique value.
     */
    public class UniqueValues implements PartitionedColumnType {
        /**
         * Displays the name of this {@link PartitionedColumnType} object.
         * @return the name of this {@link PartitionedColumnType} object.
         */
        public String toString() {
            return "UniqueValues";
        }
    }
    
    /**
     * Use this class to partition on a set of values - ie. only columns with
     * one of these values will be returned.
     */
    public class ValueCollection implements PartitionedColumnType {
        /**
         * Internal reference to the values to select rows on.
         */
        private Collection values;
        
        /**
         * The constructor specifies the value to partition on. If the value is null,
         * or it is empty, then only rows with null in this column will be selected.
         * @param values the values to partition on.
         */
        public ValueCollection(Collection values) {
            if (values==null) values = Collections.EMPTY_SET;
            this.values = values;
        }

        /**
         * Displays the name of this {@link PartitionedColumnType} object.
         * @return the name of this {@link PartitionedColumnType} object.
         */
        public String toString() {
            return "ValueCollection:"+this.values;
        }
    }
    
    /**
     * Use this class to partition on a single value - ie. only rows matching this
     * value will be returned.
     */
    public class SingleValue extends ValueCollection {
        /**
         * Internal reference to the single value to select rows on.
         */
        private String value;
        
        /**
         * The constructor specifies the value to partition on. If the value is null,
         * then only rows with null in this column will be selected.
         * @param value the value to partition on.
         */
        public SingleValue(String value) {
            super(Collections.singleton(value));
            this.value = value;
        }

        /**
         * Displays the name of this {@link PartitionedColumnType} object.
         * @return the name of this {@link PartitionedColumnType} object.
         */
        public String toString() {
            return "SingleValue:"+this.value;
        }
    }
}
