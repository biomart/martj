package org.ensembl.mart.lib;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;

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

/**
 * Base UnprocessedFilterHandler implementing object that provides a private method
 * to handle versioned ids in a manner appropriate to the dataset.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public abstract class IDListFilterHandlerBase implements UnprocessedFilterHandler {

	private final String SQL = "select versioned_ids from meta_versioned_ids where constellation_name = ?";

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.UnprocessedFilterHandler#ModifyQuery(org.ensembl.mart.lib.Engine, java.util.List, org.ensembl.mart.lib.Query)
	 */
	public abstract Query ModifyQuery(Engine engine, List filters, Query query) throws InvalidQueryException;

	/**
	 * If the dataset being querried with an IDListFilter object contains versioned ids that are output to the user, these versions must be
	 * stripped off the end of the id, if they are present, before they can be applied as a filter.  This method polls the mart meta_release_info
	 * table to determine if the dataset contains versioned ids, and, if so, strips any versions off the ids before returning them.  If the dataset does
	 * not contain versioned ids, then the input list is returned unchanged.
	 * 
	 * @param conn - java.sql.Connection object to poll the database for version information
	 * @param query_with_starbases - Query to get dataset name from
	 * @param input - String[] array of String ids needing to be normalized.
	 * @return String[] Array of String ids with versions stripped, if necessary
	 * @throws InvalidQueryException for all underlying exceptions
	 */
	protected String[] ModifyVersionedIDs(Connection conn, Query query_with_starbases, String[] input)
		throws InvalidQueryException {
		if (query_with_starbases.getStarBases().length < 1)
			throw new InvalidQueryException("Recieved Query with missing starbase\n");

		String constellation_name = query_with_starbases.getStarBases()[0]; // get the first starBase, should be valid
        //StringTokenizer constellationTokens = new StringTokenizer(constellation_name,"_");
        //String dset = constellationTokens.nextToken();
		String[] sbtokens = constellation_name.split("__");
		String dset = sbtokens[0];
        
		boolean versioned_ids = false;

		try {
			PreparedStatement ps = conn.prepareStatement(SQL);
			ps.setString(1, constellation_name);
			ResultSet rs = ps.executeQuery();
			rs.next(); // only one result
			versioned_ids = (rs.getInt(1) == 1) ? true : false;
		} catch (SQLException e) {
			if (logger.isLoggable(Level.WARNING))
				logger.warning("Problem checking dataset for versioning: " + e.getMessage());
			throw new InvalidQueryException(e.getMessage(), e);
		}

		if (versioned_ids) {
			// strip off the last .n from the id
			for (int i = 0, n = input.length; i < n; i++) {
				int versionIndex = input[i].lastIndexOf(".");
				if (versionIndex >= 0)
					input[i] = input[i].substring(0, versionIndex);
			}
		}
		
    return input;
	}

	/**
	 * Harvests an InputStreamReader for IDS, one per line.  If IDs are encountered, the list is further filtered through ModifyVersionedIDs before being returned.
	 * 
	 * @param conn - java.sql.Connection object to be passed to ModifyVersionedIDs
	 * @param query_with_starbases - Query to be passed to ModifyVersionedIDs
	 * @param instream - InputStreamReader object with IDs, one per line.
	 * @return String[] list of IDs harvested from instream, further filtered through ModifyVersionedIDs
	 * @throws InvalidQueryException for all underlying exceptions
	 */
	protected String[] HarvestStream(Connection conn, Query query_with_starbases, InputStreamReader instream)
		throws InvalidQueryException {
		String[] harvestedIds;
		try {
			List identifiers = new ArrayList();
			BufferedReader in = new BufferedReader(instream);

			for (String line = in.readLine(); line != null; line = in.readLine())
				identifiers.add(line);

			harvestedIds = new String[identifiers.size()];
			identifiers.toArray(harvestedIds);
		} catch (Exception e) {
			if (logger.isLoggable(Level.WARNING))
				logger.warning("Problem getting IDs from Stream: " + e.getMessage());
			throw new InvalidQueryException("Could not harvest IDs from Stream: " + e.getMessage(), e);
		}

		if (harvestedIds.length < 1) {
			if (logger.isLoggable(Level.WARNING))
				logger.warning("No IDS harvested from Stream\n");
			return harvestedIds;
		} else
			return ModifyVersionedIDs(conn, query_with_starbases, harvestedIds);
	}

	protected Logger logger = Logger.getLogger(IDListFilterHandlerBase.class.getName());
}
