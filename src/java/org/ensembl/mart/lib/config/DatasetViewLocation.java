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
 * Object representing a DatasetViewLocation element in a MartRegistry.dtd compliant xml document.
 * Adds functionality for getting an XMLStream (for URL type DatasetViewLocation objects) or
 * a DataSource (for Database type DatasetViewLocation objects) for the underlying DatasetView.
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class DatasetViewLocation extends BaseConfigurationObject {

	/**
	 * Static enum over types of DatasetViewLocation objects supported
	 */
	public static final String URLTYPE = "url";
	public static final String DATABASETYPE = "database";

	private final int hashcode;
	private final String dataset;
	private final String locationType;
	private final URLLocation urlloc;
	private final DatabaseLocation dbloc;

	/**
	 * Constructor for a fully defined URL type DatasetViewLocation.
   * 
	 * @param internalName String name to represent the underlying DatasetView of this DatasetViewLocation. Must not be null
	 * @param displayName String name to display in an UI.
	 * @param dataset String prefix for all tables in the Mart Database for this DatasetView. Must not be null
	 * @param description String description of the DatasetView.
	 * @param locationType String type of location (must equal DatasetViewLocation.URLTYPE
	 * @param urlloc
	 * @throws ConfigurationException when required values are null, or empty strings, or locationType is incorrect
	 */
	public DatasetViewLocation(String internalName, String displayName, String dataset, String description, String locationType, URLLocation urlloc)
		throws ConfigurationException {
		super(internalName, displayName, description);

		if (dataset == null || "".equals(dataset) || locationType == null || "".equals(locationType) || urlloc == null)
			throw new ConfigurationException("URL type DatasetViewLocation objects must be instantiated with a dataset, locationType, and URLLocation object\n");

		if (!locationType.equals(URLTYPE))
			throw new ConfigurationException("Incorrect locationType passed for URL type object: " + locationType + " does not equal " + URLTYPE);

		this.dataset = dataset;
		this.locationType = locationType;
		this.urlloc = urlloc;
    dbloc = null;

		int tmp = super.hashCode();
		tmp = (31 * tmp) + dataset.hashCode();
		tmp = (31 * tmp) + this.locationType.hashCode();
		tmp = (31 * tmp) + urlloc.hashCode();
		hashcode = tmp;
	}

	/**
	 * Constructor for a fully defined Database type DatasetViewLocation.
   * 
	 * @param internalName String name to represent the underlying DatasetView of this DatasetViewLocation. Must not be null
	 * @param displayName String name to display in an UI.
	 * @param dataset String prefix for all tables in the Mart Database for this DatasetView. Must not be null
	 * @param description String description of the DatasetView.
   * @param locationType String type of location (must equal DatasetViewLocation.DATABASETYPE
	 * @param dblocation
   * @throws ConfigurationException when required values are null, or empty strings, or locationType is incorrect
	 */
	public DatasetViewLocation(
		String internalName,
		String displayName,
		String dataset,
		String description,
		String locationType,
		DatabaseLocation dblocation)
		throws ConfigurationException {
		super(internalName, displayName, description);

		if (dataset == null || "".equals(dataset) || locationType == null || "".equals(locationType) || dblocation == null)
			throw new ConfigurationException("Database type DatasetViewLocation objects must be instantiated with a dataset, locationType, and DatabaseLocation object\n");
		
    if (!locationType.equals(DATABASETYPE))
			throw new ConfigurationException("Incorrect locationType passed for URL type object: " + locationType + " does not equal " + DATABASETYPE);

		this.dataset = dataset;
		this.locationType = locationType;
		this.dbloc = dblocation;
    urlloc = null;

		int tmp = super.hashCode();
		tmp = (31 * tmp) + dataset.hashCode();
		tmp = (31 * tmp) + this.locationType.hashCode();
		tmp = (31 * tmp) + dbloc.hashCode();
		hashcode = tmp;
	}

}
