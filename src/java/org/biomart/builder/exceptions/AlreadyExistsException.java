/*
 * AlreadyExistsException.java
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
 * This refers to exceptions where something is being added to something else, but
 * already exists in its destination.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 23rd March 2006
 * @since 0.1
 */
public class AlreadyExistsException extends BuilderException {
    /**
     * Constructs an instance of <code>AlreadyExistsException</code> with the specified detail message.
     * @param msg the detail message.
     * @param name the name of the object that already exists.
     */
    public AlreadyExistsException(String msg, String name) {
        super(msg + " (" + name + ")");
    }
}
