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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.sql.DataSource;

/**
 * DSViewAdaptor implimentation that retrieves DatasetView objects from
 * a Mart Database.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatabaseDSViewAdaptor implements DSViewAdaptor {

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
		// TODO Auto-generated method stub
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
			dsviews.add(dsv);
		}
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
	 */
   //TODO:impliment caching and MD5SUM based updates
	public void update() throws ConfigurationException {
		//iterate over the iname map
		for (Iterator iter = inameMap.keySet().iterator(); iter.hasNext();) {
			String internalName = (String) iter.next();

			DatasetView newdsv = ConfigurationUtils.getDatasetView(MartXMLutils.getDatasetViewXMLStreamByInternalName(dsvsource, user, internalName), false);

			if (!dsviews.contains(newdsv)) {
				for (Iterator iterator = dsviews.iterator(); iterator.hasNext();) {
					DatasetView olddsv = (DatasetView) iterator.next();

					if (olddsv.getInternalName().equals(newdsv.getInternalName())) {
            dsviews.remove(olddsv);
            dsviews.add(newdsv);
            break;
					}
        }
			}
		}
	}
}
