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
 
package org.ensembl.mart.explorer.config;

import java.io.IOException;
import java.sql.Connection;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implimentation of the EntityResolver specifically desinged to handle DOCTYPE declarations
 * differently than the default provided.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartDTDEntityResolver implements EntityResolver {

   private Connection conn = null;
   
   /**
    * Constructs a MartDTDEntityResolver object to add to an XML (SAX, DOM) Parser for MartConfiguration.xml
    * to allow it to pull the DTD from a different source than that specified in the DOCTYPE declaration.
    * 
    * @param conn A java.sql.Connection object.
    */
   public MartDTDEntityResolver(Connection conn) {
   	this.conn = conn;     
   }
   
	/* (non-Javadoc)
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
	 */
	public InputSource resolveEntity(String publicID, String systemID) throws SAXException, IOException {
		
		if (systemID.equals("MartConfiguration.dtd")) {
			   try {
					return org.ensembl.mart.explorer.config.MartXMLutils.getInputSourceFor(conn, systemID);
				} catch (ConfigurationException e) {
					throw new SAXException("Could not get DTD from Database: "+e.getMessage());
				}			
		}
		else
		  return null;
	}

}
