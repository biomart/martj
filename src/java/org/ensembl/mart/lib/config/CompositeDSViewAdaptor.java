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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A composite DSViewAdaptor that combines the datasets from all contained 
 * DSViewAdaptors.
 */
public class CompositeDSViewAdaptor implements DSViewAdaptor {

	private List adaptors = new ArrayList();

	/**
	 * Creates instance of DSViewManager.
	 */
	public CompositeDSViewAdaptor() {
	}

	/**
	 * Adds adaptor.
	 * @param adaptor adaptor to be added.
	 */
	public void add(DSViewAdaptor adaptor) {
		adaptors.add(adaptor);
	}

	/**
	 * Remove adaptor if present.
	 * @param adaptor adaptor to be removed
	 * @return true if adaptor was removed, otherwise false.
	 */
	public boolean remove(DSViewAdaptor adaptor) {
    System.out.println( adaptor );
    System.out.println( adaptors.indexOf( adaptor ));
    System.out.println(adaptors);
		return adaptors.remove(adaptor);
	}

	/**
	 * Removes all adaptors.
	 */
	public void clear() {
		adaptors.clear();
	}

	/**
	 * Gets currently available adaptors.
	 * @return all adaptors currently managed by this instance. Empty array 
	 * if non available.
	 */
	public DSViewAdaptor[] getAdaptors() {
		return (DSViewAdaptor[]) adaptors.toArray(
			new DSViewAdaptor[adaptors.size()]);
	}

	/** 
	 * @return dataset display names for all managed adaptors.
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetDisplayNames()
	 */
	public String[] getDatasetDisplayNames() throws ConfigurationException {
		List names = new ArrayList();
		for (int i = 0, n = adaptors.size(); i < n; i++) {
			DSViewAdaptor adaptor = (DSViewAdaptor) adaptors.get(i);
			names.addAll(Arrays.asList(adaptor.getDatasetDisplayNames()));
		}
		return (String[]) names.toArray(new String[names.size()]);
	}

	/**
	 * @return dataset internal names for all managed adaptors.
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetInternalNames()
	 */
	public String[] getDatasetInternalNames() throws ConfigurationException {
		List names = new ArrayList();
		for (int i = 0, n = adaptors.size(); i < n; i++) {
			DSViewAdaptor adaptor = (DSViewAdaptor) adaptors.get(i);
			names.addAll(Arrays.asList(adaptor.getDatasetInternalNames()));
		}
		return (String[]) names.toArray(new String[names.size()]);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViews()
	 */
	public DatasetView[] getDatasetViews() throws ConfigurationException {
		List views = new ArrayList();
		for (int i = 0, n = adaptors.size(); i < n; i++) {
			DSViewAdaptor adaptor = (DSViewAdaptor) adaptors.get(i);
			views.addAll(Arrays.asList(adaptor.getDatasetViews()));
		}
		return (DatasetView[]) views.toArray(new DatasetView[views.size()]);
	}

	/**
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsDisplayName(java.lang.String)
	 */
	public boolean supportsDisplayName(String name)
		throws ConfigurationException {
		return null!=getDatasetViewByDisplayName( name );
	}

	/**
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByDisplayName(java.lang.String)
	 */
	public DatasetView getDatasetViewByDisplayName(String name)
		throws ConfigurationException {

		DatasetView result = null;
		DatasetView[] views = getDatasetViews();
		for (int i = 0, n = views.length; i < n; i++) {
			DatasetView view = views[i];
			if (view.getDisplayName().equals(name)) {
				result = view;
				break;
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#supportsInternalName(java.lang.String)
	 */
	public boolean supportsInternalName(String name)
		throws ConfigurationException {
		return getDatasetViewByInternalName(name) != null;
	}

	/**
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#getDatasetViewByInternalName(java.lang.String)
	 */
	public DatasetView getDatasetViewByInternalName(String name)
		throws ConfigurationException {

		DatasetView result = null;

		DatasetView[] views = getDatasetViews();
		for (int i = 0, n = views.length; i < n; i++) {
			DatasetView view = views[i];
			if (view.getInternalName().equals(name)) {
				result = view;
				break;
			}
		}
		return result;
	}

	/**
	 * @see org.ensembl.mart.lib.config.DSViewAdaptor#update()
	 */
	public void update() throws ConfigurationException {
		for (int i = 0, n = adaptors.size(); i < n; i++) {
			DSViewAdaptor adaptor = (DSViewAdaptor) adaptors.get(i);
		  adaptor.update();	
		}
	}

}
