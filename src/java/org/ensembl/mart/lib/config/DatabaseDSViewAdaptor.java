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

import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.sql.DataSource;

import org.ensembl.mart.lib.DatabaseUtil;
import org.ensembl.mart.lib.DatabaseUtil.DatabaseURLElements;

/**
 * DSViewAdaptor implimentation that retrieves DatasetView objects from
 * a Mart Database.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSViewAdaptor implements MultiDSViewAdaptor {

  private String dbpassword;
	private Logger logger = Logger.getLogger(DatabaseDSViewAdaptor.class.getName());
	private List dsviews = new ArrayList();
	private int inameIndex = 0;
	private HashMap inameMap = new HashMap();
	private int dnameIndex = 0;
	private HashMap dnameMap = new HashMap();

	private final DataSource dsvsource;
	private final String user;

	public DatabaseDSViewAdaptor(DataSource ds, String user) throws ConfigurationException {
		if (ds == null || user == null)
			throw new ConfigurationException("DatabaseDSViewAdaptor Objects must be instantiated with a DataSource and User\n");

		this.user = user;
		dsvsource = ds;

		update();
	}

  /**
   * This method should ONLY be used if the user is not concerned with network password snooping, as it does
   * not do anything to encrypt the password provided.  It is really a convenience method for users wishing to
   * create MartRegistry files with their database password attribute filled in.
   * @param password -- String password for underlying DataSource
   * @throws ConfigurationException if setDatabasePassword has already been called.
   * @see org.ensembl.mart.lib.config.DatabaseDSViewAdaptor#getMartLocations
   */
  public void setDatabasePassword(String password) throws ConfigurationException {
    if (dbpassword != null)
      throw new ConfigurationException("DatabasePassword already set\n");
    dbpassword = password;
  }
  
	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetDisplayNames()
	 */
	public String[] getDatasetDisplayNames() throws ConfigurationException {
		String[] ret = new String[dnameMap.size()];
		dnameMap.keySet().toArray(ret);
		return ret;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetInternalNames()
	 */
	public String[] getDatasetInternalNames() throws ConfigurationException {
		String[] ret = new String[inameMap.size()];
		dnameMap.keySet().toArray(ret);
		return ret;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViews()
	 */
	public DatasetView[] getDatasetViews() throws ConfigurationException {
		DatasetView[] ret = new DatasetView[dsviews.size()];
		dsviews.toArray(ret);
		return ret;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDisplayName(java.lang.String)
	 */
	public boolean supportsDisplayName(String name) {
		return dnameMap.containsKey(name);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDisplayName(java.lang.String)
	 */
	public DatasetView getDatasetViewByDisplayName(String name) throws ConfigurationException {
		return (DatasetView) dnameMap.get(name);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsInternalName(java.lang.String)
	 */
	public boolean supportsInternalName(String name) {
		return inameMap.containsKey(name);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByInternalName(java.lang.String)
	 */
	public DatasetView getDatasetViewByInternalName(String name) throws ConfigurationException {
		return (DatasetView) inameMap.get(name);
	}

	public void addDatasetView(DatasetView dsv) {
		if (!(inameMap.containsKey(dsv.getInternalName()) && dnameMap.containsKey(dsv.getDisplayName()))) {
			inameMap.put(dsv.getInternalName(), dsv);
			dnameMap.put(dsv.getDisplayName(), dsv);
			dsv.setDSViewAdaptor(this);
			dsviews.add(dsv);

		}
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.MultiDSViewAdaptor#removeDatasetView(org.ensembl.mart.lib.config.DatasetView)
	 */
	public boolean removeDatasetView(DatasetView dsv) {
		if (inameMap.containsKey(dsv.getInternalName())) {
			inameMap.remove(dsv.getInternalName());
			dnameMap.remove(dsv.getDisplayName());
			dsv.setDSViewAdaptor(null);
			dsviews.remove(dsv);
      return true;
		} else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
	 */
	//TODO:impliment caching
	public void update() throws ConfigurationException {
		String[] inms = DatabaseDatasetViewUtils.getAllInternalNames(dsvsource, user);
		for (int i = 0, n = inms.length; i < n; i++) {
			String iname = inms[i];

			if (inameMap.containsKey(iname)) {
				byte[] nDigest = DatabaseDatasetViewUtils.getDSViewMessageDigestByInternalName(dsvsource, user, iname);
				byte[] oDigest = ((DatasetView) inameMap.get(iname)).getMessageDigest();

				if (!MessageDigest.isEqual(oDigest, nDigest)) {
					removeDatasetView((DatasetView) inameMap.get(iname));
					addDatasetView(DatabaseDatasetViewUtils.getDatasetViewByInternalName(dsvsource, user, iname));
				}
			} else
				addDatasetView(DatabaseDatasetViewUtils.getDatasetViewByInternalName(dsvsource, user, iname));
		}
	}

	/**
	 * Allows client to store a single DatasetView object as a DatasetView.dtd compliant XML document into a Mart Database.
	 * Client can choose whether to compress (GZIP) the resulting XML before it is stored in the Database.
	 * @param ds -- DataSource of the Mart Database where the DatasetView.dtd compliant XML is to be stored.
	 * @param user -- RDBMS user for _meta_DatasetView_[user] table to store the document.  If null, or if _meta_DatasetView_[user] does not exist, _meta_DatasetView will be the target of the document.
	 * @param dsv -- DatasetView object to store
	 * @param compress -- if true, the resulting XML will be gzip compressed before storing into the table.
	 * @throws ConfigurationException for all underlying Exceptions
	 */
	public static void storeDatasetView(DataSource ds, String user, DatasetView dsv, boolean compress) throws ConfigurationException {
		DatabaseDatasetViewUtils.storeConfiguration(
			ds,
			user,
			dsv.getInternalName(),
			dsv.getDisplayName(),
			dsv.getDataset(),
			dsv.getDescription(),
			DatasetViewXMLUtils.DatasetViewToDocument(dsv),
			compress);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#lazyLoad(org.ensembl.mart.lib.config.DatasetView)
	 */
	public void lazyLoad(DatasetView dsv) throws ConfigurationException {
		DatasetViewXMLUtils.LoadDatasetViewWithDocument(dsv, DatabaseDatasetViewUtils.getDatasetViewDocumentByInternalName(dsvsource, user, dsv.getInternalName()));
	}

	/**
   * Note, this method will only include the DataSource password in the resulting MartLocation object
   * if the user set the password using the setDatabasePassword method of this adaptor. Otherwise, 
   * regardless of whether the underlying DataSource was created
   * with a password, the resulting DatabaseLocation element will not have
   * a password attribute.  Users may need to hand modify any MartRegistry documents
   * that they create in these cases.  Users are encouraged to use
   * passwordless, readonly access users.
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getMartLocations()
	 */
	public MartLocation[] getMartLocations() throws ConfigurationException {
		try {
			DatabaseURLElements els = DatabaseUtil.decompose(dsvsource.getConnection().getMetaData().getURL());
			
			MartLocation dbloc = new DatabaseLocation(els.host, els.port, els.databaseType, els.databaseName, user, dbpassword, dsvsource.getConnection().getMetaData().getDriverName());
			
			return new MartLocation[] { dbloc };
		} catch (IllegalArgumentException e) {
      throw new ConfigurationException("Caught IllegalArgumentException during creation of new DatabaseLocation object " + e.getMessage(), e);
		} catch (SQLException e) {
      throw new ConfigurationException("Caught SQLException during creation of new DatabaseLocation object " + e.getMessage(), e);
		} catch (ConfigurationException e) {
      throw e;
		}    
	}
}
