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
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.ensembl.mart.lib.DetailedDataSource;
import org.ensembl.mart.lib.InputSourceUtil;

/**
 * DSViewAdaptor implimentation for working with MartRegistry Objects.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class RegistryDSViewAdaptor extends CompositeDSViewAdaptor {

	private MartRegistry martreg; // single, underlying MartRegistry for this Adaptor
	private URL url;
	private Set martRegs = new TreeSet(); // keep a list of MartRegistry Objects pulled from RegistryLocation elements

	/**
	 * Constructs an empty RegistryDSViewAdaptor.  A URL for
	 * an existing MartRegistry document can be set later, using setExistingRegistryURL. This will cause the 
	 */
	public RegistryDSViewAdaptor() {
		super();
	}

	/**
	 * Constructs a RegistryDSViewAdaptor with a url containing a MartRegistry.dtd compliant XML Document.
	 * @param url -- URL pointing to MartRegistry.dtd compliant XML Document
	 * @throws ConfigurationException if url is null, and for all underlying URL/XML parsing Exceptions
	 */
	public RegistryDSViewAdaptor(URL url) throws ConfigurationException {
		super();
		setRegistryURL(url);
    
    adaptorName = url.toString();
	}

	/**
	 * Constructs a RegistryDSViewAdaptor with an existing MartRegistry object.
	 * Users can set a URL to refer to this MartRegistry using setRegistryURL.
	 * @param martreg -- existing MartRegistry object
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public RegistryDSViewAdaptor(MartRegistry martreg) throws ConfigurationException {
		this(martreg, null);
	}

	/**
	 * Construct a RegistryDSViewAdaptor with an existing MartRegistry object, and its URL
	 * @param martreg -- existing MartRegistry object
	 * @param url -- url refering, or to refer to this MartRegistry object
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public RegistryDSViewAdaptor(MartRegistry martreg, URL url) throws ConfigurationException {
		this.martreg = martreg;

		if (url != null) {
			this.url = url;
      adaptorName = url.toString();
    }

		loadAdaptorsFromRegistry();
	}

	/**
	 * Construct a RegistryDSViewAdaptor with an existing DSViewAdaptor object. Users can set
	 * its URL later with a call to setRegistryURL.
	 * @param adaptor -- adaptor to initialize this RegistryAdaptor with.
	 */
	public RegistryDSViewAdaptor(DSViewAdaptor adaptor) throws ConfigurationException {
		this(adaptor, null);
	}

	/**
	 * Construct a RegistryDSViewAdaptor with an existing DSViewAdaptor object, and its URL. 
	 * @param adaptor -- adaptor to initialize this RegistryAdaptor with.
	 * @param url -- url to refer to this MartRegistry object
	 */
	public RegistryDSViewAdaptor(DSViewAdaptor adaptor, URL url) throws ConfigurationException {
		adaptors.add(adaptor);
		if (url != null) {
			this.url = url;
      adaptorName = url.toString();
    }

		martreg = getMartRegistry();
	}

	/**
	 * Sets the URL refering to the underlying MartRegistry object.
	 * If no adaptors have been added, or the object was constructed with an existing MartRegistry object, 
	 * the object attempts to access this URL to create a MartRegistry object.  Otherwise, it simply records the
	 * URL for future reference.
	 * @param url -- url refering to the underlying MartRegistry object.
	 * @throws ConfigurationException if url is null, or if the url has already been set, and for all URL/XML parsing exceptions
	 */
	public void setRegistryURL(URL url) throws ConfigurationException {
		if (url == null)
			throw new ConfigurationException("Attempt to set url with a null URL\n");
      
		if (this.url != null)
			throw new ConfigurationException("A RegistryAdaptor can only work with one MartRegistry document URL\n");

		this.url = url;

		if (martreg == null) {
			if (adaptors.size() > 0)
				martreg = getMartRegistry();
			else {
				loadMartRegistryFromURL();
				loadAdaptorsFromRegistry();
			}
		}
    
    if (adaptorName == null)
      adaptorName = url.toString();
	}

	private void loadMartRegistryFromURL() throws ConfigurationException {
		try {
			martreg = MartRegistryXMLUtils.XMLStreamToMartRegistry( InputSourceUtil.getStreamForURL( url ) );
		} catch (ConfigurationException e) {
			throw e;
		} catch (IOException e) {
			throw new ConfigurationException("Caught IOException opening Stream for supplied url: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns the URL for this MartRegistryAdaptor.  May be null.
	 * @return URL url
	 */
	public URL getURL() {
		return url;
	}

	/*
	 * TODO: This method iterates through all of the MartLocation objects within the MartRegistry object underlying this
	 * MartRegistryAdaptor, determining the unique set of DSViewAdaptors to represent the object.  If two DSViewAdaptor
	 * objects are found to support the same DatasetView object (based on internalName), then the first encountered DSViewAdaptor
	 * is given precedence.  The last encountered DSViewAdaptor is either modified using the removeDatasetView command (if it is a
	 * MultiDSViewAdaptor), or thrown away. In the case where all of the DatasetView objects within a MultiDSViewAdaptor are found
	 * to be supported by previously loaded adaptors, the entire MultiDSViewAdaptor is thrown away.  This does set up the possibility that
	 * DatasetView specifications for the same Dataset from multiple adaptors which differ in their filters/attributes could be mis - handled.
	 */
	private void loadAdaptorsFromRegistry() throws ConfigurationException {
		MartLocation[] locs = martreg.getMartLocations();
		for (int i = 0, n = locs.length; i < n; i++) {
			MartLocation location = locs[i];

			if (location.getType().equals(MartLocationBase.REGISTRY)) {
				//create underlying MartRegistry objects with this, check against martreg list before creating an adaptor for it (may point to the same martreg document)

				MartRegistry subreg = null;

				try {
					subreg = MartRegistryXMLUtils.XMLStreamToMartRegistry(  InputSourceUtil.getStreamForURL( ( (RegistryLocation) location).getUrl() ) );
				} catch (ConfigurationException e) {
					throw e;
				} catch (IOException e) {
					throw new ConfigurationException("Caught IOException working with MartRegistryLocation Element URL: " + e.getMessage(), e);
				}

				if (!martRegs.contains(subreg)) {
					RegistryDSViewAdaptor adaptor = new RegistryDSViewAdaptor(subreg, url);
          
					if (!adaptors.contains(adaptor)) {
						DatasetView[] dsvs = adaptor.getDatasetViews();
						for (int j = 0, m = dsvs.length; j < m; j++) {
							DatasetView view = dsvs[j];

							if (supportsInternalName(view.getInternalName()))
								adaptor.removeDatasetView(view); // assume they are the same
						}

            adaptor.setName(location.getName());
            
						if (adaptor.getDatasetInternalNames().length > 0)
							add(adaptor);
					}
					martRegs.add(subreg);
				}
			} else if (location.getType().equals(MartLocationBase.URL)) {
				URLDSViewAdaptor adaptor = new URLDSViewAdaptor(((URLLocation) location).getUrl(), false);
        adaptor.setName(location.getName());
				add(adaptor);
			} else if (location.getType().equals(MartLocationBase.DATABASE)) {
				DatabaseLocation dbloc = (DatabaseLocation) location;

				String host = dbloc.getHost();
				String user = dbloc.getUser();
				String instanceName = dbloc.getInstanceName();
				String port = dbloc.getPort();
				String password = dbloc.getPassword();
				String databaseType = dbloc.getDatabaseType();
        String jdbcDriverClassName = dbloc.getJDBCDriverClassName();
        String name = dbloc.getName();

				// apply defaults only if both dbtype and jdbcdriver are null
				if (databaseType == null && jdbcDriverClassName == null) {
					databaseType = DetailedDataSource.DEFAULTDATABASETYPE;
					jdbcDriverClassName = DetailedDataSource.DEFAULTDRIVER;
				}


        String connectionString = DetailedDataSource.connectionURL(databaseType, host, port, instanceName);
        // use default name
        if ( name==null || "".equals(name))
          name = connectionString;

        
        //use the default poolsize of 10        
        DetailedDataSource dsource =
					new DetailedDataSource(databaseType, host, port, instanceName, connectionString, user, password, DetailedDataSource.DEFAULTPOOLSIZE, jdbcDriverClassName, name);

				DatabaseDSViewAdaptor adaptor = new DatabaseDSViewAdaptor(dsource, user);

				//iterate through this adaptor's datasetviews, checking if they are already represented
				if (!adaptors.contains(adaptor)) {
					DatasetView[] dsvs = adaptor.getDatasetViews();
					for (int j = 0, m = dsvs.length; j < m; j++) {
						DatasetView view = dsvs[j];

						if (supportsInternalName(view.getInternalName()))
							adaptor.removeDatasetView(view); // assume they are the same
					}

          adaptor.setName(location.getName());
          
					if (adaptor.getDatasetInternalNames().length > 0)
						add(adaptor);
				}
			} else
				throw new ConfigurationException("Recieved unsupported MartLocation element of type : " + location.getType() + " in MartRegistry Document\n");
		}
	}

//	/**
//	 * Adds adaptor.  
//	 * @param adaptor adaptor to be added. Do not add an ancestor CompositeDSViewAdaptor
//	 * to this instance or you will cause circular references when the getXXX() methods are called.
//	 */
//	public void add(DSViewAdaptor adaptor) {
//		if (!adaptors.contains(adaptor))
//			adaptors.add(adaptor);
//	}

	/**
	 * Returns a new MartRegistry object, with MartLocations for all of the adaptors present.
	 * @return MartRegistry object
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public MartRegistry getMartRegistry() throws ConfigurationException {
		MartRegistry nmartreg = new MartRegistry();

		for (Iterator iter = adaptors.iterator(); iter.hasNext();) {
			DSViewAdaptor adaptor = (DSViewAdaptor) iter.next();

			MartLocation[] martlocs = adaptor.getMartLocations();
			for (int i = 0, n = martlocs.length; i < n; i++) {
				nmartreg.addMartLocation(martlocs[i]);
			}
		}

		return nmartreg;
	}

	/**
	 * Writes a MartRegistry object as MartRegistry.dtd compliant XML to a File.
	 * @param mr -- MartRegistry object to store to the file system
	 * @param file -- File to write XML
	 * @throws ConfigurationException for underlying Exceptions
	 */
	public static void StoreMartRegistry(MartRegistry mr, File file) throws ConfigurationException {
		MartRegistryXMLUtils.MartRegistryToFile(mr, file);
	}

  /**
   * Allows Equality Comparisons manipulation of DSViewAdaptor objects.  Although
   * any DSViewAdaptor object can be compared with any other DSViewAdaptor object, to provide
   * consistency with the compareTo method, in practice, it is almost impossible for different DSVIewAdaptor
   * implimentations to equal.
   *
	 * Equality is based on the CompositeViewAdaptor hashCode, so that two URL
	 * sources specifying the same MartRegistry will equal.
	 */
	public boolean equals(Object o) {
		return o instanceof DSViewAdaptor && hashCode() == o.hashCode();
	}

	/**
	 * Calculates CompositeDSViewAdaptor hashCode. Two MartRegistryAdaptors specifying
	 * the same MartLocation elements, regardless of their URL source, should
	 * have the same hashCode.  A call to update on either should resolve any
	 * differences in actual DatasetView content.
	 * @see org.ensembl.mart.lib.config.CompositeDSViewAdaptor#hashCode()
	 */
	public int hashCode() {
		return super.hashCode();
	}
  
  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.config.DSViewAdaptor#getName()
   */
  public String getName() {
      return super.getName();
  }
}
