/*
    Copyright (C) 2003 EBI, GRL

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
 
package org.ensembl.mart.lib;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches FieldMappers and uses Queries as the keys.
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @version $Revision$
 */
public class FieldMapperCache {
	
	public static final FieldMapperCache instance = new FieldMapperCache();
	

	private FieldMapperCache(){}
	
	/** @return hash key based on star bases 
	 * and primary keys in query, order sensitive. */
	public final Integer queryHashcode( Query query) {
		int total = 17;
		for (int i = 0; i < query.getStarBases().length; i++)
			total = total*37 + query.getStarBases()[i].hashCode();
		for (int i = 0; i < query.getPrimaryKeys().length; i++)
			total = total*37 + query.getPrimaryKeys()[i].hashCode();
		
		return new Integer(total); 
	}

	
	public final void cacheMappers( Query query, FieldMapper[] mappersValue ){
		mapperCache.put( queryHashcode(query), mappersValue );
	}

	public final FieldMapper[] cachedMappers( Query query ){
		return (FieldMapper[])mapperCache.get( queryHashcode(query) );	
	}

	public final void clear() {
		mapperCache.clear();
	}
	
	
	private final Map mapperCache = new HashMap();

}
