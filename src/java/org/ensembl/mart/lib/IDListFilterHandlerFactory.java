package org.ensembl.mart.lib;

import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.IDListFilterHandler;
import org.ensembl.mart.lib.InvalidQueryException;

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

/**
 * Factory object providing a static method to create a specific IDListFilterHandler implementing 
 * object for a given IDListFilter object based on its type
 *  
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see org.ensembl.mart.lib.IDListFilterHandler
 */
public class IDListFilterHandlerFactory {
	
	/**
	 * Resolves the given listType into an IDListFilterHandler implementing object based on the
	 * static IDListFilter type enums.  It is planned that future versions will allow the type
	 * to resolve to objects outside mart explorer package using the java ClassLoader system.
	 * 
	 * @param listType - int matching one of the IDListFilter type enums.
	 * @return IDListFilter implementing object.
	 * @throws InvalidQueryException -- currently for unsupported listType.
	 */
	public static IDListFilterHandler getInstance(int listType) throws InvalidQueryException {
		IDListFilterHandler idlh = null;
		
		switch (listType) {
			case IDListFilter.FILE:
			  idlh = new FileIDListFilterHandler();
			   break;
			   
			 case IDListFilter.URL:
			   idlh = new URLIDListFilterHandler();
			   break;
			   
			 case IDListFilter.SUBQUERY:
			   idlh = new SubQueryIDListFilterHandler();
			   break;
			   
			 default:
			   //TODO: implement Class loader system
			   throw new InvalidQueryException("Invalid IDListFilter Type passed\n");
		}
		
		return idlh;
	}
}
