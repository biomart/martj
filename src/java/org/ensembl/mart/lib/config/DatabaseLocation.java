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
public class DatabaseLocation extends MartLocationBase {
  private final String host;
  private final String port;
  private final String databaseType;
  private final String instanceName;
  private final String user;
  private final String password;
  private final String type = MartLocationBase.DATABASE;

  private final int hashcode;

  public DatabaseLocation(String host, String port, String databaseType, String instanceName, String user, String password) throws ConfigurationException {
    if (host == null || instanceName == null || user == null)
      throw new ConfigurationException("DatabaseLocation Objects must contain a host, user and instanceName\n");
      
      this.host = host;
		  this.instanceName = instanceName;
      this.user = user;
      this.databaseType = databaseType;
      this.port = port;
      this.password = password;
      
      int tmp = host.hashCode();
		  tmp = (31 * tmp) + user.hashCode();
      tmp = (31 * tmp) + instanceName.hashCode();
      tmp = (port != null) ? (31 * tmp) + port.hashCode() : tmp;
      tmp = (databaseType != null) ? (31 * tmp) + databaseType.hashCode() : tmp;
      tmp = (password != null) ? (31 * tmp) + password.hashCode() : tmp;
      hashcode = tmp;      
  }

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.MartLocation#getType()
	 */
	public String getType() {
    return type;
	}
	
	/**
	 * Returns the type of RDBMS serving this location.  This may be null.
	 * @return String databaseType
	 */
	public String getDatabaseType() {
		return databaseType;
	}

	/**
	 * Returns the host for the RDBMS serving this location.
	 * @return String host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the name of the Mart instance.
	 * @return String instanceName
	 */
	public String getInstanceName() {
		return instanceName;
	}

	/**
	 * Returns the password for the RDBMS serving this location.  This may be null.
	 * @return String password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Returns the port for the RDBMS serving this location.  This may be null.
	 * @return String port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Returns the user for the RDBMS serving this location.
	 * @return String user
	 */
	public String getUser() {
		return user;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("Location Type=").append(type);
		buf.append(", host=").append(host);
		buf.append(", user=").append(user);
		buf.append(", instanceName=").append(instanceName);
		buf.append(", databaseType").append(databaseType);
		buf.append(", port=").append(port);
		buf.append(", password=").append(password);
		
		buf.append("]");

		return buf.toString();
	}
	
	/**
	 * Allows Equality Comparisons manipulation of DatabaseLocation objects
	 */
	public boolean equals(Object o) {
		return o instanceof DatabaseLocation && hashCode() == o.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
    return hashcode;
	}
}
