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

import java.net.URL;

/**
 * Object representing a URLLocation element in a DatasetViewLocation element
 * of a MartRegistry.dtd compliant XML document.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class URLLocation extends MartLocationBase {

  private final URL url;
  private final int hashcode;
  
	public URLLocation(URL url, String name) {
    this.url = url;
    this.type = MartLocationBase.URL;
    
    int tmp = url.hashCode();
    
    if (name != null) {
      this.name = name;
      tmp = (31 * tmp) + name.hashCode();
    }    
    hashcode = tmp;
	}

	/**
   * Return the URL for this URLLocation
	 * @return URL
	 */
	public URL getUrl() {
		return url;
	}

 
  public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
		buf.append("Location Type=").append(type);
		buf.append("url=").append(url);
		buf.append("]");

		return buf.toString();
	}
  
  /**
	 * Allows Equality Comparisons manipulation of URLLocation objects
	 */
	public boolean equals(Object o) {
		return o instanceof URLLocation && hashCode() == o.hashCode();
	}
  
  /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
     return hashcode;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.MartLocation#getType()
	 */
	public String getType() {
    return type;
	}

}
