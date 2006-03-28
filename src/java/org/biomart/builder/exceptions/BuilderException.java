/*
 * BuilderException.java
 *
 * Created on 23 March 2006, 15:27
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

package org.biomart.builder.exceptions;

/**
 * This is the base {@link Exception} for all MartBuilder exceptions. 
 *
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 23rd March 2006
 * @since 0.1
 */
public class BuilderException extends Exception {
    
    /**
     * Creates a new instance of <code>BuilderException</code> without detail message.
     */
    public BuilderException() {
        super();
    }
        
    /**
     * Constructs an instance of <code>BuilderException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public BuilderException(String msg) {
        super(msg);
    }
        
    /**
     * Constructs an instance of <code>BuilderException</code> with the specified detail message
     * and cause.
     * @param msg the detail message.
     * @param t the underlying cause.
     */
    public BuilderException(String msg, Throwable t) {
        super(msg, t);
    }
        
    /**
     * Constructs an instance of <code>BuilderException</code> with the specified cause.
     * @param t the underlying cause.
     */
    public BuilderException(Throwable t) {
        super(t);
    }
}
