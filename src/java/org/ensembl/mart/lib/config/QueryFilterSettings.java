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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib.config;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class QueryFilterSettings extends BaseConfigurationObject {
  
  /**
	 * @param internalName
	 * @param displayName
	 * @param description
	 * @throws ConfigurationException
	 */
	public QueryFilterSettings(String internalName, String displayName, String description) throws ConfigurationException {
		super(internalName, displayName, description);
	}
	

  
  public abstract String getField();
  public abstract String getFieldFromContext();
  
  public abstract String getValue();
  public abstract String getValueFromContext();
  
  public abstract String getHandler();
  public abstract String getHandlerFromContext();
  
  public abstract String getTableConstraint();
  public abstract String getTableConstraintFromContext();
  
  public abstract String getType();
  public abstract String getTypeFromContext();
  
  // These will break the build - Darin to implement methods in derived class
  public abstract String getQualifier();
  public abstract String getQualifierFromContext();
    
}
