/*
 * BuilderBundle.java
 * Created on 10 April 2006, 11:54
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

package org.biomart.builder.resources;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Simple wrapper for resources.
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.1, 10th April 2006
 * @since 0.1
 */
public class BuilderBundle {
    /**
     * Internal reference to the resource bundle.
     */
    private static ResourceBundle bundle = ResourceBundle.getBundle("org/biomart/builder/messages");
    
    /**
     * Obtains a string from the resource bundle "org/biomart/builder/resources/messages.properties".
     * Runs it through MessageFormat before returning.
     * See {@link ResourceBundle#getString(String)} for full description of behaviour.
     * @param key the key to look up.
     * @return the matching string.
     */
    public static String getString(String key) {
        return MessageFormat.format(bundle.getString(key), new Object[]{});
    }
    
    /**
     * Obtains a string from the resource bundle "org/biomart/builder/resources/messages.properties".
     * Substitutes {0} in the resulting string for the specified value using MessageFormat.
     * See {@link ResourceBundle#getString(String)} for full description of behaviour.
     * @param key the key to look up.
     * @return the matching string.
     */
    public static String getString(String key, String value) {
        return MessageFormat.format(bundle.getString(key), new Object[]{value});
    }
    
    /**
     * Obtains a string from the resource bundle "org/biomart/builder/resources/messages.properties".
     * Substitutes {0}..{n} in the resulting string for the specified values using MessageFormat.
     * See {@link ResourceBundle#getString(String)} for full description of behaviour.
     * @param key the key to look up.
     * @return the matching string.
     */
    public static String getString(String key, String[] values) {
        return MessageFormat.format(bundle.getString(key), values);
    }
}
