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

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

/**
 * DSViewAdaptor implimenting object designed to provide a DatasetView object from
 * from an URL.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class URLDSViewAdaptor implements DSViewAdaptor {
	private final URL dsvurl;
	private final boolean validate;
  private final int hashcode;

	private DatasetView dsv;
	private String[] inames;
	private String[] dnames;
	private Logger logger = Logger.getLogger(URLDSViewAdaptor.class.getName());

  /**
   * Construct a DSViewAdaptor from a url containing a DatasetView.dtd compliant XML Document.
   * JDOM validation is set to false.
   * @param url -- url containing a DatasetView.dtd compliant XML document
   * @throws ConfigurationException for all underlying Exceptions
   */
  public URLDSViewAdaptor(URL url) throws ConfigurationException {
    this(url, false);
  }
  
  /**
   * Construct a DSViewAdaptor from a url containing a DatasetView.dtd compliant XML Document,
   * with optional JDOM validation.
   * @param url -- url containing a DatasetView.dtd compliant XML document
   * @param validate -- if true, JDOM validates the Document against the DatasetView.dtd contained in the CLASSPATH
   * @throws ConfigurationException for all underlying Exceptions.
   */
	public URLDSViewAdaptor(URL url, boolean validate) throws ConfigurationException {
		if (url == null)
			throw new ConfigurationException("DSViewURLAdaptors must be instantiated with a URL\n");
		dsvurl = url;
		this.validate = validate;
    
    hashcode = dsvurl.hashCode();
		update();
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetDisplayNames()
	 */
	public String[] getDatasetDisplayNames() throws ConfigurationException {
		return dnames;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetInternalNames()
	 */
	public String[] getDatasetInternalNames() throws ConfigurationException {
		return inames;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViews()
	 */
	public DatasetView[] getDatasetViews() throws ConfigurationException {
		return new DatasetView[] { dsv };
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDisplayName(java.lang.String)
	 */
	public boolean supportsDisplayName(String name) {
		if (dnames[0].equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDisplayName(java.lang.String)
	 */
	public DatasetView getDatasetViewByDisplayName(String name) throws ConfigurationException {
		if (!supportsDisplayName(name))
			throw new ConfigurationException(name + " does not match the displayName of this SimpleDatasetView object\n");
		return dsv;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsInternalName(java.lang.String)
	 */
	public boolean supportsInternalName(String name) {
		if (inames[0].equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByInternalName(java.lang.String)
	 */
	public DatasetView getDatasetViewByInternalName(String name) throws ConfigurationException {
		if (!supportsInternalName(name))
			throw new ConfigurationException(name + " does not match the internalName of this SimpleDatasetView object\n");
		return dsv;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
	 */
	public void update() throws ConfigurationException {
		try {
				dsv = DatasetViewXMLUtils.XMLStreamToDatasetView(dsvurl.openStream(), validate);
		} catch (Exception e) {
			throw new ConfigurationException("Could not load DatasetView from URL: " + dsvurl + " " + e.getMessage(), e);
		}

		inames = new String[] { dsv.getInternalName()};
		dnames = new String[] { dsv.getDisplayName()};

		dsv.setDSViewAdaptor(this);
	}

	/**
	 * Useful debug output
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("[");
    buf.append(" url=").append(dsvurl.toString());
		buf.append(", dataset DisplayName=").append(dsv.getDisplayName());
		buf.append("]");

		return buf.toString();
	}

	/**
	 * Allows Equality Comparisons manipulation of SimpleDSViewAdaptor objects
	 */
	public boolean equals(Object o) {
		return o instanceof URLDSViewAdaptor && hashCode() == o.hashCode();
	}

  /**
   * Based solely on the underlying URL.
   * Two URLDSViewAdaptors should return the same hashCode if they
   * are based on the same underlying URL. If the underlying DatasetView
   * has changed between instantiation of the two URLDSViewAdaptor objects,
   * a call to update() should resolve this.
   */
	public int hashCode() {
		return hashcode;
	}

	/**
	 * Currently doesnt do anything, as URL DatasetView objects are fully loaded at instantiation.  Could change in the future.
	 */
	public void lazyLoad(DatasetView dsv) throws ConfigurationException {
		// Currently does not do anything, as URL DSViews are fully loaded at instantiation.  Could change in the future.

	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getMartLocations()
	 */
	public MartLocation[] getMartLocations() throws ConfigurationException {
		return new MartLocation[] { new URLLocation(dsvurl)};
	}
  
  /**
   * Writes a DatasetView object as DatasetView.dtd compliant XML to a File.
   * @param dsv -- DatasetView object to store to the file system
   * @param file -- File to write XML
   * @throws ConfigurationException for underlying Exceptions
   */
  public static void StoreDatasetView(DatasetView dsv, File file) throws ConfigurationException {
    DatasetViewXMLUtils.DatasetViewToFile(dsv, file);
  }
}
