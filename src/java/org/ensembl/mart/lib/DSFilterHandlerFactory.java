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

/**
 * Factory object providing a static method to create a specific DSFilterHandler implementing 
 * object for a given objectCode.
 *  
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DSFilterHandlerFactory {
	/**
	 * Resolves the given objectCode into a specific DSFilterHandler implementing object,
	 * based on the DomainSpecificFilter static String enums. It is planned that a future
	 * version will allow the objectCode to reference objects outside the mart explorer package
	 * that can be loaded in using the ClassLoader system.
	 * 
	 * @param objectCode - String matching one of the DomainSpecificFilter static String enums.
	 * @return DSFilterHandler dsfh - a DSFilterHandler implementing object.
	 * @see DomainSpecificFilter
	 */
  public static DSFilterHandler getInstance(String objectCode) throws InvalidQueryException {
  	DSFilterHandler dsfh = null;
  	
  	if ( objectCode.equals(DomainSpecificFilter.MARKER) )
  	  dsfh = new DSMarkerFilterHandler();
  	else if ( objectCode.equals(DomainSpecificFilter.BAND) )
  	  dsfh = new DSBandFilterHandler();
  	else if ( objectCode.equals(DomainSpecificFilter.ENCODE) || objectCode.equals(DomainSpecificFilter.QTL) )
  	  dsfh = new DSEncodeQtlFilterHandler();
  	else if ( objectCode.equals(DomainSpecificFilter.EXPRESSION) )
  	  dsfh = new DSExpressionFilterHandler();
  	else
//TODO: implement object class loader system
  	  throw new InvalidQueryException("Unsupported Domain Specific Filter Option passed.");
  	return dsfh;
  }
}
