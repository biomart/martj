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
 
package org.ensembl.mart.lib.config;

/**
 * Object representing a DatabaseLocation element in a DatasetViewLocation element
 * within a MartRegistry.dtd compliant XML document.  
 * Note, this object has the capability of storing database passwords, but does not do anything
 * to make them secure.  Users are encouraged to use readonly, passwordless access, or user-password
 * combinations for users with limited privileges.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseLocation {
  private final String host;
  private final String port;
  private final String databaseType;
  private final String instanceName;
  private final String user;
  private final String password;
  //private final DataSource dsource;
  private final int hashcode;

  public DatabaseLocation(String host, String port, String databaseType, String instanceName, String user, String password) throws ConfigurationException {
    if (host == null || instanceName == null)
      throw new ConfigurationException("DatabaseLocation Objects must contain a host and instanceName\n");
      
      this.host = host;
      this.user = user;
      this.databaseType = databaseType;
      this.port = port;
      this.instanceName = instanceName;
      this.password = password;
      
      int tmp = host.hashCode();
      tmp = (31 * tmp) + instanceName.hashCode();
      tmp = (port != null) ? (31 * tmp) + port.hashCode() : tmp;
      tmp = (databaseType != null) ? (31 * tmp) + databaseType.hashCode() : tmp;
      tmp = (user != null) ? (31 * tmp) + user.hashCode() : tmp;
      tmp = (password != null) ? (31 * tmp) + password.hashCode() : tmp;
      hashcode = tmp;      
  }

}
