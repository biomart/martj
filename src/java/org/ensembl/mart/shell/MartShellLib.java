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

package org.ensembl.mart.shell;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.BooleanFilter;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterPage;

/**
 * <p>Library allowing client code to parse Mart Query Language (MQL)
 * querries into Query Objects using MQLtoQuery, or parse Query objects into MQL querries using QueryToMQL.  
 * 
 * <!-- This is a css style for indenting lines below.  It is placed here to allow the first line to be used in the package description. -->  
 * <STYLE>
 * <!--
 * 
 * P.indent_small {
 *   text-indent: 0.2in;
 * }
 * 
 * P.indent_big {
 *   text-indent: 0.5in;
 * }
 * -->
 * </STYLE>
 * 
 * <p>Mart Query Language (MQL) is a derivative of the Structured Query Language (SQL)
 * used to query relational databases.  It follows the following format (items in angle brackets are optional):</p>
 * <p class="indent_big">&lt; using datasetViewName &lt; dataSourceName &gt; &gt; </p>
 * <p class="indent_big">get</p>
 * <p class="indent_big">&lt; attribute_list &gt;</p>
 * <p class="indent_big">&lt; sequence sequence_request &gt;</p>
 * <p class="indent_big">&lt; where filter_list &gt;</p>
 * <p class="indent_big">&lt; limit integer &gt;</p>
 * <br>
 * <p>- attribute_list is a comma-separated list of mart attributes.  These must match the internal_name of attributes in the MartConfiguration for the mart being querried.
 *    attribute_list can be omitted for sequence_requests, otherwise, at least one attribute must be specified. Specifying attributes with
 *    a sequence_request requests that those attributes (if available) are included, either as fields in tabulated output, or in the description portion of the 
 *    header in fasta output.</p>
 * <p>- sequence_request follows the pattern:</p>
 * <p class="indent_big">&lt;left_flank_length_integer+&gt;sequence_name&lt;+right_flank_length_integer&gt;</p>
 * <p class="indent_small">sequence_name must match the name of a sequence available from the system (see the SequenceDescription documentation below).
 * <p class="indent_small">If specified with a left and/or right flank_length_integer (eg., 10+gene_exon, gene_exon+10, or 10+gene_exon+10)<p>
 * <p class="indent_small">then the sequences will return the requested flanking sequence (although some sequences are not flankable).</p>
 * <p>- dataset_name must match the internal_name of a dataset made available by the MartConfiguration made available for the mart being querried.</p>
 * <p>- filter_list is a comma-separated list of filter_requests.  filter_requests must match one of the following formats:</p>
 * <p class="indent_big">- filter_name excluded|only  -- specifies that objects should be returned only if they match (only),</p>
 * <p class="indent_big">or only if they do not match (excluded) this filter.</p>
 * <p class="indent_big">- filter_name =|!=|&gt;|&gt;=|&lt;|&lt;= value</p>
 * <p class="indent_big">- filter_name in url -- specifies that the system should harvest the items in the specified url,</p>
 * <p class="indent_big">and return objects only if they satisfy the condition of filter_name in (items in the url).</p>
 * <p class="indent_big">The url must return a list of items, one per line.</p>
 * <p class="indent_big">- filter_name in (comma-separated list of items) -- note the list must be enclosed in perentheses.</p>
 * <p class="indent_big">- filter_name in (nested query) -- Nested query can be a MQL query, but it cannot include sequence_requests, or into requests.</p>
 * <p class="indent_big">It also must only return one attribute, matching filter_name.</p>
 * <p>filter_name must match the internal_name for a filter specified in the MartConfiguration for the mart being querried.  Also, in some cases, the filter_name
 *    must be further qualified with a filter_set_name prepended with a period (eg, filter_set_name.filter_name = value).  This is only the case when the filter
 *    is part of a filter_set, as specified in the MartConfiguration.</p>   
 * <br>
 * <p>- if a limit request is specified, this adds a limit integer clause to the actual SQL executed against the mart database.  Note that this does not necessarily
 *    limit the number of records returned to the specified integer.  It limits the number of mart focus objects querried, for which attributes are returned.
 *    If there is a one-many, many-many, or many-one relationship between the mart focus object, and the attribute being requested, then the number of records returned
 *    will reflect this.</p>
 * <br>
 * <p>Note, the minimal MQL request would be 'get attribute_name'.  The minimal sequence request would be 'get sequence sequence_name'.</p>
 * <p>MQL differs from SQL in not requiring (or even allowing) multiple datasets, table ALLQUALIFIERS, or joins.  You just have to specify the attributes/sequence that you want, the dataset to query, and filters to apply.</p>
 * <p>This makes simple queries which are not that hard at the SQL level, even more simple.</p>
 * <p>Because the Mart-Explorer engine resolves some filters to complex sub querries, it makes more complex underlying querries just as simple.</p>
 * <p>Finally, it makes querries for things like sequences (which are impossible using SQL) just as simple.</p>  
 * <p>MQL statements can be written on one line, or separated with newlines and whitespace to make them easier to read</p>
 * <br>
 * <p>The default output settings are tab - separated, tabulated output
 * to System.out.  Client programs can over ride these settings using the appropriate setter methods
 *      but, once set, they remain in effect for the entire session, until another call to the
 *      setter(s) is(are) made).</p>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see org.ensembl.mart.lib.SequenceDescription
 * @see org.ensembl.mart.lib.config.MartConfiguration
 */
public class MartShellLib {

	public MartShellLib(DSViewAdaptor adaptor) {
		this.adaptor = adaptor;
	}

	/**
	 * Sets the environmental dataset for the session.  This
	 * dataset can only be over ridden with another call to setDataset,
	 * or with a 'using' clause in the MQL.
	 * @param dset - String name of the dataset to use for queries
	 */
	public void setDataset(String dsetname) {
		this.envDataset = dsetname;
	}

	/**
	 * Sets the environmental Mart for the session.  This
	 * can only be over ridden with another call to setEnvMart,
	 * or, for a single query, in a using clause.
	 * @param ds - Mart to use as default
	 */
	public void setEnvMart(DataSource ds) {
		envMart = ds;
	}

	/**
	 * Sets the map between Mart names (strings) and
	 * their Mart, so that using statements can resolve
	 * the Mart for a query.
	 * @param dsmap -- Hashtable map between Mart String name, and Mart.
	 */
	public void setMartMap(Hashtable dsmap) {
		dataSourceMap = dsmap;
	}

	/**
	 * Set or Reset the DSViewAdaptor object to use in parsing Queries and MQL
	 * statements.
	 * 
	 * @param adaptor
	 */
	public void setDSViewAdaptor(DSViewAdaptor adaptor) {
		this.adaptor = adaptor;
	}

	/**
	 * Converts the Query into an MQL string.
	 * 
	 * @param query Query object to be transformed into MQL
	 * @param datasetViewquery Query object to be transformed into MQL
	 * @return String MQL statement
	 * throws InvalidQueryException for all underlying exceptions
	 */
	public String QueryToMQL(Query query, DatasetView datasetView) throws InvalidQueryException {

		StringBuffer mqlbuf = new StringBuffer();
		boolean success = getUsingClause(query, datasetView, mqlbuf);

		if (success)
			success = getGetClause(query, datasetView, mqlbuf.append(" "));

		if (success && (query.getType() == Query.SEQUENCE))
			getSequenceClause(query, mqlbuf.append(" "));

		if (success && (query.getTotalFilterCount() > 0))
			success = getWhereClause(query, datasetView, mqlbuf.append(" "));

		if (success && query.hasLimit())
			mqlbuf.append(" ").append("limit ").append(query.getLimit());

		if (!success)
			throw new InvalidQueryException("Could not compile MQL from Query\n" + MQLError + "\n");

		return mqlbuf.toString();
	}

	/**
	 * Creates a Mart Query Language command from a Query object.
	 * 
	 * @param query Query object to be transformed into MQL
	 * @return String MQL statement
	 * throws InvalidQueryException for all underlying exceptions
	 */
	public String QueryToMQL(Query query) throws InvalidQueryException, ConfigurationException {

		String datasetName = query.getDatasetInternalName();

		//	get datasetName first
		if (datasetName == null)
			throw new InvalidQueryException("Recieved null DatasetName from query provided\n");

		if (!adaptor.supportsInternalName(datasetName))
			throw new InvalidQueryException("DatasetView " + datasetName + " is not supported by the martConfiguration provided\n");

		DatasetView dataset = adaptor.getDatasetViewByInternalName(datasetName);

		return QueryToMQL(query, dataset);
	}

	private boolean getUsingClause(Query query, DatasetView dataset, StringBuffer mqlbuf) {
		boolean success = true;

		mqlbuf.append(USINGQSTART).append(" ").append(dataset.getInternalName());

		return success;
	}

	private boolean getGetClause(Query query, DatasetView dataset, StringBuffer mqlbuf) {
		Attribute[] attributes = query.getAttributes();
		mqlbuf.append(GETQSTART);

		if (attributes.length == 0) {
			if (query.getType() == Query.SEQUENCE)
				return true;
			else {
				MQLError = "Empty attributes, no Sequence.";
				return false;
			}
		}

		mqlbuf.append(" ");
		boolean success = true;

		for (int i = 0, n = attributes.length;(success && i < n); i++) {
			if (i > 0)
				mqlbuf.append(", ");

			Attribute attribute = attributes[i];
			String fname = attribute.getField();
			String tconstraint = attribute.getTableConstraint();

			if (dataset.supportsAttributeDescription(fname, tconstraint))
				mqlbuf.append(dataset.getAttributeDescriptionByFieldNameTableConstraint(fname, tconstraint).getInternalName());
			else {
				success = false;
				MQLError = "Could not map attribute " + attribute.getField() + " " + attribute.getTableConstraint();
			}
		}

		return success;
	}

	private void getSequenceClause(Query query, StringBuffer mqlbuf) {
		mqlbuf.append("sequence ");

		SequenceDescription seqd = query.getSequenceDescription();
		String seqtype = seqd.getTypeAsString();

		int lflank = seqd.getLeftFlank();
		if (lflank > 0)
			mqlbuf.append(lflank).append("+");

		mqlbuf.append(seqtype);

		int rflank = seqd.getRightFlank();
		if (rflank > 0)
			mqlbuf.append("+").append(rflank);
	}

	private boolean getWhereClause(Query query, DatasetView datasetview, StringBuffer mqlbuf) {
		boolean success = true;

		mqlbuf.append("where ");

		Filter[] filters = query.getFilters();
		for (int i = 0, n = filters.length;(success && i < n); i++) {
			if (i > 0)
				mqlbuf.append(" and ");

			Filter filter = filters[i];

			if (filter instanceof BasicFilter)
				success = mapBasicFilter((BasicFilter) filter, datasetview, mqlbuf);
			else if (filter instanceof BooleanFilter)
				success = mapBooleanFilter((BooleanFilter) filter, datasetview, mqlbuf);
			else
				success = mapIDListFilter((IDListFilter) filter, datasetview, mqlbuf);

			if (!success)
				MQLError = "Could not map filter " + filter.getField() + " " + filter.getTableConstraint();
		}

		return success;
	}

	private boolean mapBooleanFilter(BooleanFilter filter, DatasetView datasetview, StringBuffer mqlbuf) {
		String field = filter.getField();
		String tableConstraint = filter.getTableConstraint();
		if (!datasetview.supportsFilterDescription(field, tableConstraint))
			return false;

		FilterDescription fdesc = datasetview.getFilterDescriptionByFieldNameTableConstraint(field, tableConstraint);
		String filterName = fdesc.getInternalNameByFieldNameTableConstraint(field, tableConstraint);
		String filterCondition = filter.getCondition();

		mqlbuf.append(filterName);

		if (filterCondition.equals(BooleanFilter.isNULL) || filterCondition.equals(BooleanFilter.isNotNULL_NUM))
			mqlbuf.append(" excluded");
		else
			mqlbuf.append(" only");

		return true;
	}

	private boolean mapIDListFilter(IDListFilter filter, DatasetView datasetview, StringBuffer mqlbuf) {
		String field = filter.getField();
		String tableConstraint = filter.getTableConstraint();
		if (!datasetview.supportsFilterDescription(field, tableConstraint))
			return false;

		boolean success = true;
		FilterDescription fdesc = datasetview.getFilterDescriptionByFieldNameTableConstraint(field, tableConstraint);
		String filterName = fdesc.getInternalNameByFieldNameTableConstraint(field, tableConstraint);
		String filterCondition = filter.getCondition();

		mqlbuf.append(filterName).append(" in ");

		String handler = filter.getHandler();

		if (handler.equals(IDListFilter.FILE)) {
			mqlbuf.append(filter.getFile());
		} else if (handler.equals(IDListFilter.URL)) {
			mqlbuf.append(filter.getUrl());
		} else if (handler.equals(IDListFilter.SUBQUERY)) {
			Query subq = filter.getSubQuery();
			mqlbuf.append(subq.getQueryName());

			try {
				mqlbuf.insert(0, QueryToMQL(subq) + " as " + subq.getQueryName() + ";");
			} catch (Exception e) {
				success = false;
				MQLError = ("Could not map subquery:\n" + subq + "\n" + e);
			}
		} else if (handler.equals(IDListFilter.STRING)) {
			String[] ids = filter.getIdentifiers();
			mqlbuf.append("(");

			for (int i = 0, n = ids.length; i < n; i++) {
				if (i > 0)
					mqlbuf.append(", ");
				mqlbuf.append(ids[i]);
			}

			mqlbuf.append(")");
		}

		return success;
	}

	private boolean mapBasicFilter(BasicFilter filter, DatasetView datasetview, StringBuffer mqlbuf) {
		String field = filter.getField();
		String tableConstraint = filter.getTableConstraint();
		if (!datasetview.supportsFilterDescription(field, tableConstraint))
			return false;

		FilterDescription fdesc = datasetview.getFilterDescriptionByFieldNameTableConstraint(field, tableConstraint);
		mqlbuf.append(fdesc.getInternalNameByFieldNameTableConstraint(field, tableConstraint)).append(" ").append(filter.getCondition()).append(" ").append(
			filter.getValue());

		return true;
	}

	/**
	 *  Allows users to store MQL to use as subqueries in other MQL.
	 * 
	 * @param key - String name to refer to this stored Command in later queries.
	 * @param mql - String mql subquery.
	 */
	public void addStoredMQLCommand(String key, String mql) {
		if (logger.isLoggable(Level.INFO))
			logger.info("Storing command with key " + key + "\n");
		storedCommands.put(key, mql);
	}

	/**
	 * Remove a stored MQL statement with its key.
	 * @param key -- key for stored MQL command
	 */
	public void removeStoredMQLCommand(String key) {
		if (storedCommands.containsKey(key)) {
			if (logger.isLoggable(Level.INFO))
				logger.info("Removing stored MQL command for key " + key + "\n");

			storedCommands.remove(key);
		}
	}

	/**
	 * Returns the actual MQL statement stored for a particular key, or null if not stored
	 * @param key -- key for a stored MQL command
	 * @return String MQL command
	 */
	public String describeStoredMQLCommand(String key) {
		return storedCommands.getProperty(key);
	}

	/**
	 * Returns a query object for a stored procedure.  This only works for
	 * queries without bind variables.
	 * @param key -- key for a MQL command
	 * @return Query parsed from stored MQL command
	 * @throws InvalidQueryException for any query parsing Exceptions
	 */
	public Query StoredMQLCommandToQuery(String key) throws InvalidQueryException {
		return MQLtoQuery(describeStoredMQLCommand(key));
	}

	/**
	 * Get a Set containing all stored MQL Procedure keys.
	 * @return Set
	 */
	public Set getStoredMQLCommandKeys() {
		return storedCommands.keySet();
	}

	/** 
	 * Creates a Query object from a Mart Query Language command.
	 * 
	 * @param mql - String MQL command to parse into a Query object
	 * @return Query object
	 * @throws InvalidQueryException for all underlying exceptions (MQL syntax errors, DatasetView/Attributes/Sequences/Filters not found, etc.)
	 */
	public Query MQLtoQuery(String newquery) throws InvalidQueryException {
		try {
			boolean start = true;
			boolean getClause = false;
			boolean usingClause = false;
			boolean domainSpecificClause = false;
			boolean fromClause = false;
			boolean whereClause = false;
			boolean limitClause = false;
			boolean inList = false;
			boolean inBind = false;
			boolean inQuotedValue = false;
			boolean whereFilterName = false;
			boolean whereFilterCond = false;
			boolean whereFilterVal = false;

			if (logger.isLoggable(Level.INFO))
				logger.info("Recieved Query " + newquery + "\n");

			DatasetView dset = null;
			Query query = new Query();
			currentFpage = null;
			currentApage = null;
			atts = new ArrayList();
			filtNames = new ArrayList();
			maxSelects = new Hashtable();

			String filterName = null;
			String filterCondition = null;
			StringBuffer filterValue = new StringBuffer();
			StringBuffer storedCommand = new StringBuffer();
			List listFilterValues = new ArrayList();
			String domainSpecificKeyword = null;

			StringTokenizer cTokens = new StringTokenizer(newquery, " ");

			if (cTokens.countTokens() < 2)
				throw new InvalidQueryException("\nInvalid Query Recieved " + newquery + "\n");

			while (cTokens.hasMoreTokens()) {
				String thisToken = cTokens.nextToken();
				if (start) {
					if (!(thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART)))
						throw new InvalidQueryException("Invalid Query Recieved, should begin with either 'using' or 'get': " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(GETQSTART)) {
						start = false;
						getClause = true;
					} else {
						start = false;
						usingClause = true;
					}
				} else if (usingClause) {
					if (domainSpecificHandlerAvailable(thisToken))
						throw new InvalidQueryException("Invalid Query Recieved, domain specific clause before " + GETQSTART + " clause: " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(QWHERE))
						throw new InvalidQueryException("Invalid Query Recieved, where clause before " + GETQSTART + " clause: " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(QLIMIT))
						throw new InvalidQueryException("Invalid Query Recieved, limit clause before " + GETQSTART + " clause: " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART)) {
						usingClause = false;
						getClause = true;
					} else {
						if (dset != null) {
							logger.info("Recieved " + thisToken + " as appearent DatasetView, after it had already been set\n");
							throw new InvalidQueryException("Invalid Query Recieved, DatasetView already set, attempted to set again: " + newquery + "\n");
						} else {
							String martreq = null;
							String datasetviewreq = null;

							if (thisToken.indexOf(">") > 0) {
								String[] toks = thisToken.split("\\>");
								datasetviewreq = toks[0];
								martreq = toks[1];
							} else {
								martreq = null;
								datasetviewreq = thisToken;
							}

							if (!adaptor.supportsInternalName(datasetviewreq))
								throw new InvalidQueryException("DatasetView " + datasetviewreq + " has not been loaded\n");

							dset = adaptor.getDatasetViewByInternalName(datasetviewreq);
							query.setDatasetInternalName(datasetviewreq);

							if (martreq != null) {
								if (!dataSourceMap.containsKey(martreq))
									throw new InvalidQueryException("Mart " + martreq + " has not been loaded\n");
								query.setDataSource((DataSource) dataSourceMap.get(martreq));
							}

							if (logger.isLoggable(Level.INFO)) {
								logger.info("setting local dataset to " + datasetviewreq + "\n");
								if (martreq != null)
									logger.info("setting Mart to " + martreq + "\n");
							}
						}

					}
				} else if (getClause) {
					if (thisToken.endsWith(","))
						thisToken = thisToken.substring(0, thisToken.length() - 1);

					// set dataset and update query with starbases, or throw an exception if dataset not set
					if (dset == null) {
						if (envDataset == null) {
							throw new InvalidQueryException("Invalid Query Recieved, did not set DatasetView: " + newquery + "\n");
						} else {
							if (!adaptor.supportsInternalName(envDataset))
								throw new InvalidQueryException("DatasetView " + envDataset + " is not found in this mart\n");
							dset = adaptor.getDatasetViewByInternalName(envDataset);
							query.setDatasetInternalName(envDataset);
						}
					}

					if (query.getDataSource() == null) {
						//favor the env Mart over the DatasetView mart
						if (envMart != null)
							query.setDataSource(envMart);
						else if (dset.getDatasource() != null)
							query.setDataSource(dset.getDatasource());
						else
							throw new InvalidQueryException("Invalid Query Recieved, could not get a Mart from the environment or the DatasetView: " + newquery + "\n");
					}

					query.setDatasetInternalName(dset.getInternalName());
					query.setStarBases(dset.getStarBases());
					query.setPrimaryKeys(dset.getPrimaryKeys());

					if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
						throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause in the middle of a " + GETQSTART + " clause: " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(QLIMIT)) {
						getClause = false;
						limitClause = true;
					} else if (domainSpecificHandlerAvailable(thisToken)) {
						domainSpecificKeyword = thisToken;
						getClause = false;
						domainSpecificClause = true;
					} else if (thisToken.equalsIgnoreCase(QWHERE)) {
						getClause = false;
						whereClause = true;
						whereFilterName = true;
					} else {
						StringTokenizer attToks = new StringTokenizer(thisToken, ",");
						while (attToks.hasMoreTokens())
							query = addAttribute(query, dset, attToks.nextToken().trim());
					}
				} else if (domainSpecificClause) {
					if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
						throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause in the middle of a sequence clause: " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(QLIMIT)) {
						domainSpecificClause = false;
						limitClause = true;
					} else if (thisToken.equalsIgnoreCase(QWHERE)) {
						domainSpecificClause = false;
						whereClause = true;
						whereFilterName = true;
					} else
						query = modifyQueryForDomainSpecificKeyword(domainSpecificKeyword, query, dset, thisToken);
				} else if (whereClause) {
					if (thisToken.equalsIgnoreCase(QLIMIT)) {
						whereClause = false;
						limitClause = true;
					} else if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
						throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause after where clause: " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(QWHERE))
						throw new InvalidQueryException("Invalid Query Recieved, where clause after where clause: " + newquery + "\n");

					else if (thisToken.equalsIgnoreCase(FILTERDELIMITER)) {
						whereFilterCond = false;
						whereFilterVal = false;
						whereFilterName = true;
					} else if (whereFilterName) {
						if (thisToken.matches("[^>=<]+([>=<]+)[^>=<]*")) {
							Pattern pat = Pattern.compile("[^>=<]+([>=<]+)[^>=<]*");
							//one or more non qualifier characters followed immediately by a qualifier, followed by zero or more non qualifier characters
							Matcher m = pat.matcher(thisToken);

							m.find(); // know its there, just have to find it
							filterCondition = m.group(1);

							StringTokenizer filtToks = new StringTokenizer(thisToken, filterCondition);

							if (filtToks.countTokens() == 2) {
								query = addBasicFilter(query, dset, filtToks.nextToken(), filterCondition, filtToks.nextToken());

								filterValue = new StringBuffer();
								filterName = null;
								filterCondition = null;
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = false;
							} else {
								filterName = filtToks.nextToken();
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = true;
							}
						} else {
							filterName = thisToken;
							whereFilterName = false;
							whereFilterCond = true;
							whereFilterVal = false;
						}
					} else if (whereFilterCond) {
						if (BOOLEANQUALIFIERS.contains(thisToken)) {
							query = addBooleanFilter(query, dset, filterName, thisToken);

							filterValue = new StringBuffer();
							filterName = null;
							filterCondition = null;
							whereFilterName = false;
							whereFilterCond = false;
							whereFilterVal = false;
						} else if (thisToken.matches("([>=<]+)([^>=<]+)")) {
							Pattern p = Pattern.compile("([>=<]+)([^>=<]+)");
							Matcher m = p.matcher(thisToken);
							m.find();
							filterCondition = m.group(1);
							String thisFilterValue = m.group(2);

							if (logger.isLoggable(Level.INFO))
								logger.info(" storing qual value " + filterCondition + " , " + thisFilterValue + "\n");

							query = addBasicFilter(query, dset, filterName, filterCondition, thisFilterValue);

							filterValue = new StringBuffer();
							filterName = null;
							filterCondition = null;
							whereFilterName = false;
							whereFilterCond = false;
							whereFilterVal = false;
						} else {
							filterCondition = thisToken;
							whereFilterCond = false;
							whereFilterVal = true;
						}
					} else if (whereFilterVal) {
						if (thisToken.startsWith(QUOTE)) {
							String tok = thisToken.substring(1);

							inQuotedValue = true;

							if (thisToken.endsWith(QUOTE)) {
								tok = tok.substring(0, tok.length() - 1);
								query = addBasicFilter(query, dset, filterName, filterCondition, tok);

								inQuotedValue = false;
								filterValue = new StringBuffer();
								filterName = null;
								filterCondition = null;
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = false;
							} else
								filterValue.append(tok);
						} else if (inQuotedValue) {
							if (thisToken.endsWith(QUOTE)) {
								filterValue.append(" ").append(thisToken.substring(0, thisToken.length() - 1));

								query = addBasicFilter(query, dset, filterName, filterCondition, filterValue.toString());

								inQuotedValue = false;
								filterValue = new StringBuffer();
								filterName = null;
								filterCondition = null;
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = false;
							} else
								filterValue.append(" ").append(thisToken);
						} else if (thisToken.equals(LSTART)) {
							inList = true;
						} else if (thisToken.startsWith(LSTART)) {
							inList = true;

							String tmp = thisToken.substring(1);

							if (tmp.indexOf(LEND) > 0) {
								inList = false;

								tmp = tmp.substring(0, tmp.indexOf(LEND));
								StringTokenizer toks = new StringTokenizer(tmp, ",");
								while (toks.hasMoreTokens())
									listFilterValues.add(toks.nextToken().trim());

								query = addListFilter(query, dset, filterName, listFilterValues);

								filterValue = new StringBuffer();
								filterName = null;
								filterCondition = null;
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = false;
								listFilterValues = new ArrayList();
							} else {
								if (tmp.indexOf(",") >= 0) {
									StringTokenizer toks = new StringTokenizer(tmp, ",");

									while (toks.hasMoreTokens())
										listFilterValues.add(toks.nextToken().trim());
								} else {
									listFilterValues.add(tmp);
								}
							}
						} else if (inList) {
							if (thisToken.indexOf(LEND) >= 0) {
								inList = false;
								String tmp = thisToken.substring(0, thisToken.indexOf(LEND));
								if (tmp.length() > 0) {
									StringTokenizer toks = new StringTokenizer(tmp, ",");

									while (toks.hasMoreTokens())
										listFilterValues.add(toks.nextToken().trim());
								}
								query = addListFilter(query, dset, filterName, listFilterValues);

								filterValue = new StringBuffer();
								filterName = null;
								filterCondition = null;
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = false;
								listFilterValues = new ArrayList();
							} else {
								StringTokenizer toks = new StringTokenizer(thisToken, ",");

								while (toks.hasMoreTokens())
									listFilterValues.add(toks.nextToken().trim());
							}
						} else if (thisToken.indexOf(LSTART) > 0) {
							if (thisToken.endsWith(LEND)) {
								// storedCommand with bindValues, no whitespaces
								query = addListFilter(query, dset, filterName, thisToken);

								filterValue = new StringBuffer();
								filterName = null;
								filterCondition = null;
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = false;
								inBind = false;
								storedCommand = new StringBuffer();
							} else {
								//append to storedCommand
								inBind = true;
								if (storedCommand.length() > 0)
									storedCommand.append(" ");
								storedCommand.append(thisToken);
							}
						} else if (inBind) {
							if (thisToken.endsWith(LEND)) {
								// add storedCommand
								query = addListFilter(query, dset, filterName, storedCommand.append(" ").append(thisToken).toString());

								filterValue = new StringBuffer();
								filterName = null;
								filterCondition = null;
								whereFilterName = false;
								whereFilterCond = false;
								whereFilterVal = false;
								inBind = false;
								storedCommand = new StringBuffer();
							} else {
								//append to storedCommand
								if (storedCommand.length() > 0)
									storedCommand.append(" ");
								storedCommand.append(thisToken);
							}
						} else {
							if (filterCondition.equalsIgnoreCase("in")) {
								if (thisToken.indexOf(":") >= 0) {
									//url
									try {
										query = addListFilter(query, dset, filterName, new URL(thisToken));
									} catch (Exception e) {
										throw new InvalidQueryException("Error adding url filter " + filterName + " " + thisToken + " " + e.getMessage(), e);
									}
								} else if (storedCommands.containsKey(thisToken)) {
									//storedCommand without bindvalues
									query = addListFilter(query, dset, filterName, thisToken);
								} else {
									//file
									query = addListFilter(query, dset, filterName, new File(thisToken));
								}
							} else {
								query = addBasicFilter(query, dset, filterName, filterCondition, thisToken);
							}

							filterValue = new StringBuffer();
							filterName = null;
							filterCondition = null;
							whereFilterName = false;
							whereFilterCond = false;
							whereFilterVal = false;
						}
					} else
						throw new InvalidQueryException("Invalid Query Recieved, invalid filter statement in where clause: " + newquery + "\n");
				} else if (limitClause) {
					if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
						throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause in limit clause: " + newquery + "\n");
					else if (domainSpecificHandlerAvailable(thisToken))
						throw new InvalidQueryException("Invalid Query Recieved, domain specific clause in limit clause: " + newquery + "\n");
					else if (thisToken.equalsIgnoreCase(QWHERE))
						throw new InvalidQueryException("Invalid Query Recieved, where clause in limit clause: " + newquery + "\n");
					else {
						if (query.getLimit() > 0)
							throw new InvalidQueryException("Invalid Query Recieved, attempt to set limit twice: " + newquery + "\n");
						else {
							query.setLimit(Integer.parseInt(thisToken));
						}
					}
				}
				// else not needed, as these are the only states present
			}

			if (query.getAttributes().length == 0 && query.getSequenceDescription() == null)
				throw new InvalidQueryException("Invalid Query Recieved, no attributes or sequence description found " + newquery + "\n");

			return query;
		} catch (NumberFormatException e) {
			throw new InvalidQueryException("Recieved NumberFormatException parsing MQL " + e.getMessage(), e);
		} catch (InvalidQueryException e) {
			throw e;
		} catch (ConfigurationException e) {
			throw new InvalidQueryException("Recieved ConfigurationException parsing MQL " + e.getMessage(), e);
		}
	}

	private Query modifyQueryForDomainSpecificKeyword(String domainSpecificKeyword, Query query, DatasetView dset, String thisToken)
		throws InvalidQueryException {
		// can either add keywords here, or replace it with a Plugin Module
		return addSequenceDescription(query, dset, thisToken);
	}

	private boolean domainSpecificHandlerAvailable(String keyword) {
		//modify this to add other domainSpecific keywords, or just replace it with a module
		return keyword.equalsIgnoreCase(QSEQUENCE);
	}

	private boolean domainSpecificSubQueryAllowed(String keyword) {
		//  modify this to add other domainSpecific keywords, or just replace it with a module
		return !QSEQUENCE.equalsIgnoreCase(keyword);
	}

	private Filter getIDFilterForSubQuery(String fieldName, String tableConstraint, String handler, String storedCommandName) throws InvalidQueryException {

		String bindValues = null;
		if (storedCommandName.indexOf(LSTART) > 0) {
			bindValues = storedCommandName.substring(storedCommandName.indexOf(LSTART) + 1, storedCommandName.indexOf(LEND));
			storedCommandName = storedCommandName.substring(0, storedCommandName.indexOf(LSTART));
		}

		if (!storedCommands.containsKey(storedCommandName))
			throw new InvalidQueryException(storedCommandName + " is not available as a stored MQL Command\n");

		String nestedQuery = storedCommands.getProperty(storedCommandName);

		if ((bindValues != null) && (bindValues.length() > 0)) {
			List bindVariables = new ArrayList();
			StringTokenizer vtokens = new StringTokenizer(bindValues, ",");
			while (vtokens.hasMoreTokens())
				bindVariables.add(vtokens.nextToken().trim());

			Pattern bindp = Pattern.compile("\\?");
			Matcher bindm = bindp.matcher(nestedQuery);

			StringBuffer qbuf = new StringBuffer();
			int bindIter = 0;
			while (bindm.find()) {
				bindm.appendReplacement(qbuf, (String) bindVariables.get(bindIter));
				bindIter++;
			}
			bindm.appendTail(qbuf);
			nestedQuery = qbuf.toString();
		}

		//validate, then call parseQuery on the subcommand
		String[] tokens = nestedQuery.split("\\s");

		for (int i = 1, n = tokens.length; i < n; i++) {
			String tok = tokens[i];
			if (domainSpecificHandlerAvailable(tok) && !domainSpecificSubQueryAllowed(tok))
				throw new InvalidQueryException("Invalid Nested Query Recieved: domain specific statement " + tok + " is not allowed " + nestedQuery + "\n");
			//else not needed
		}

		nestedLevel++;

		if (nestedLevel > MAXNESTING) {
			nestedLevel--;
			throw new InvalidQueryException("Only " + MAXNESTING + " levels of nested Query are allowed\n");
		}

		Query subQuery = null;
		try {
			subQuery = MQLtoQuery(nestedQuery);
		} catch (Exception e) {
			nestedLevel--;
			throw new InvalidQueryException("Could not parse Nested Query : " + e.getMessage(), e);
		}

		subQuery.setQueryName(storedCommandName);

		Filter f = null;
		if (handler != null)
			f = new IDListFilter(fieldName, tableConstraint, subQuery, handler);
		else
			f = new IDListFilter(fieldName, tableConstraint, subQuery);

		nestedLevel--;
		return f;
	}

	private Query addSequenceDescription(Query inquery, DatasetView dset, String seqrequest) throws InvalidQueryException {
		currentApage = dset.getAttributePageByInternalName("sequences");
		for (int i = 0, n = atts.size(); i < n; i++) {
			String element = (String) atts.get(i);

			if (!currentApage.containsAttributeDescription(element))
				throw new InvalidQueryException("Cannot request attribute " + element + " together with sequences in the same query.\n");
		}

		Query newQuery = new Query(inquery);

		int typecode = 0;
		int left = 0;
		int right = 0;

		StringTokenizer tokens = new StringTokenizer(seqrequest, SEQDELIMITER, true);
		int n = tokens.countTokens();
		switch (n) {
			case 5 :
				// left+type+right
				left = Integer.parseInt(tokens.nextToken());
				tokens.nextToken(); // skip plus
				typecode = SequenceDescription.SEQS.indexOf(tokens.nextToken());
				tokens.nextToken();
				right = Integer.parseInt(tokens.nextToken());
				break;
			case 3 :
				// left+type || type+right
				String tmpl = tokens.nextToken();
				tokens.nextToken();
				String tmpr = tokens.nextToken();

				if (SequenceDescription.SEQS.contains(tmpl)) {
					typecode = SequenceDescription.SEQS.indexOf(tmpl);
					right = Integer.parseInt(tmpr);
				} else if (SequenceDescription.SEQS.contains(tmpr)) {
					left = Integer.parseInt(tmpl);
					typecode = SequenceDescription.SEQS.indexOf(tmpr);
				} else {
					throw new InvalidQueryException("Invalid sequence request recieved: " + seqrequest + "\n");
				}
				break;
			case 1 :
				// type
				typecode = SequenceDescription.SEQS.indexOf(seqrequest);
				break;
		}
		newQuery.setSequenceDescription(new SequenceDescription(typecode, left, right));
		return newQuery;
	}

	private Query addAttribute(Query inquery, DatasetView dset, String attname) throws InvalidQueryException {
		checkAttributeValidity(dset, attname);

		Query newQuery = new Query(inquery);
		AttributeDescription attdesc = (AttributeDescription) dset.getAttributeDescriptionByInternalName(attname);
		Attribute attr = new FieldAttribute(attdesc.getField(), attdesc.getTableConstraint());
		newQuery.addAttribute(attr);

		return newQuery;
	}

	private void checkAttributeValidity(DatasetView dset, String attname) throws InvalidQueryException {
		if (!dset.containsAttributeDescription(attname))
			throw new InvalidQueryException("Attribute " + attname + " is not found in this mart for dataset " + dset.getInternalName() + "\n");

		//check page
		if (currentApage == null) {
			currentApage = dset.getPageForAttribute(attname);
		} else {
			if (!currentApage.containsAttributeDescription(attname)) {
				currentApage = dset.getPageForAttribute(attname);

				for (int i = 0, n = atts.size(); i < n; i++) {
					String element = (String) atts.get(i);

					if (!currentApage.containsAttributeDescription(element))
						throw new InvalidQueryException(
							"Cannot request attributes "
								+ attname
								+ " and "
								+ element
								+ " together in the same query.  Use 'describe dataset "
								+ dset.getInternalName()
								+ "' for a list of attributes that can be selected together\n");
				}
			}
		}

		//check maxSelect
		AttributeCollection collection = currentApage.getCollectionForAttributeDescription(attname);
		String colname = collection.getInternalName();
		int maxSelect = collection.getMaxSelect();

		if (maxSelect > 0) {
			if (maxSelects.containsKey(colname)) {
				int oldMax = ((Integer) maxSelects.get(colname)).intValue();
				oldMax++;
				if (oldMax > maxSelect)
					throw new InvalidQueryException("You cannot select more than " + maxSelect + " attributes from AttributeCollection " + colname + "\n");
				maxSelects.put(colname, new Integer(oldMax));
			} else
				maxSelects.put(colname, new Integer(1));
		}

		atts.add(attname);
	}

	private Query addBooleanFilter(Query inquery, DatasetView dset, String filterName, String filterCondition) throws InvalidQueryException {
		checkFilterValidity(dset, filterName);

		FilterDescription fdesc = dset.getFilterDescriptionByInternalName(filterName);
		String thisType = fdesc.getType(filterName);

		if (!thisType.startsWith("boolean"))
			throw new InvalidQueryException(filterName + " is not a boolean filter, cannot process with " + filterCondition + "\n");

		if (!BOOLEANQUALIFIERS.contains(filterCondition))
			throw new InvalidQueryException(filterCondition + " is not valid for a boolean filter\n");

		String thisCondition = null;
		if (thisType.equals("boolean_num"))
		  thisCondition = BOOLEAN_NUMCONDITIONS[ BOOLEANQUALIFIERS.indexOf(filterCondition) ];
		else 
		  thisCondition = BOOLEAN_CONDITIONS[ BOOLEANQUALIFIERS.indexOf(filterCondition) ];

		String handler = fdesc.getHandler(filterName);

		Query newQuery = new Query(inquery);
		newQuery.addFilter(new BooleanFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), thisCondition, handler));
		return newQuery;
	}

	private Query addBasicFilter(Query inquery, DatasetView dset, String filterName, String filterCondition, String filterValue) throws InvalidQueryException {
		checkFilterValidity(dset, filterName);

		FilterDescription fdesc = dset.getFilterDescriptionByInternalName(filterName);

		Query newQuery = new Query(inquery);
		if (fdesc.getHandler(filterName) != null) {
			newQuery.addFilter(
				new BasicFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), filterCondition, filterValue, fdesc.getHandler(filterName)));
		} else {
			newQuery.addFilter(new BasicFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), filterCondition, filterValue));
		}
		return newQuery;
	}

	private Query addListFilter(Query inquery, DatasetView dset, String filterName, List filterValues) throws InvalidQueryException {
		checkFilterValidity(dset, filterName);

		FilterDescription fdesc = dset.getFilterDescriptionByInternalName(filterName);

		Query newQuery = new Query(inquery);

		if (fdesc.getHandler(filterName) != null)
			newQuery.addFilter(
				new IDListFilter(
					fdesc.getField(filterName),
					fdesc.getTableConstraint(filterName),
					(String[]) filterValues.toArray(new String[filterValues.size()]),
					fdesc.getHandler(filterName)));
		else
			newQuery.addFilter(
				new IDListFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), (String[]) filterValues.toArray(new String[filterValues.size()])));

		return newQuery;
	}

	private Query addListFilter(Query inquery, DatasetView dset, String filterName, File fileloc) throws InvalidQueryException {
		checkFilterValidity(dset, filterName);

		FilterDescription fdesc = dset.getFilterDescriptionByInternalName(filterName);

		Query newQuery = new Query(inquery);

		if (fdesc.getHandler(filterName) != null)
			newQuery.addFilter(new IDListFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), fileloc, fdesc.getHandler(filterName)));
		else
			newQuery.addFilter(new IDListFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), fileloc));

		return newQuery;
	}

	private Query addListFilter(Query inquery, DatasetView dset, String filterName, URL urlLoc) throws InvalidQueryException {
		checkFilterValidity(dset, filterName);

		FilterDescription fdesc = dset.getFilterDescriptionByInternalName(filterName);

		Query newQuery = new Query(inquery);

		if (fdesc.getHandler(filterName) != null)
			newQuery.addFilter(new IDListFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), urlLoc, fdesc.getHandler(filterName)));
		else
			newQuery.addFilter(new IDListFilter(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), urlLoc));

		return newQuery;
	}

	private Query addListFilter(Query inquery, DatasetView dset, String filterName, String storedQueryName) throws InvalidQueryException {
		checkFilterValidity(dset, filterName);

		FilterDescription fdesc = dset.getFilterDescriptionByInternalName(filterName);

		Query newQuery = new Query(inquery);

		// subquery will overwrite page states, need to return them to original after it is parsed
		AttributePage bakApage = currentApage;
		FilterPage bakFpage = currentFpage;
		currentApage = null;
		currentFpage = null;

		newQuery.addFilter(getIDFilterForSubQuery(fdesc.getField(filterName), fdesc.getTableConstraint(filterName), fdesc.getHandler(filterName), storedQueryName));

		currentApage = bakApage;
		currentFpage = bakFpage;
		return newQuery;
	}

	private void checkFilterValidity(DatasetView dset, String filterName) throws InvalidQueryException {
		if (!dset.containsFilterDescription(filterName))
			throw new InvalidQueryException("Filter " + filterName + " not supported by mart dataset " + dset.getInternalName() + "\n");

		if (currentFpage == null)
			currentFpage = dset.getPageForFilter(filterName);
		else {
			if (!currentFpage.containsFilterDescription(filterName)) {
				currentFpage = dset.getPageForFilter(filterName);

				for (int i = 0, n = filtNames.size(); i < n; i++) {
					String element = (String) filtNames.get(i);
					if (!currentFpage.containsFilterDescription(element))
						throw new InvalidQueryException(
							"Cannot use filters "
								+ filterName
								+ " and "
								+ element
								+ " together in the same query.  Use 'describe dataset "
								+ dset.getInternalName()
								+ "' to get a list of filters that can be used in the same query.\n");
				}
			}
		}

		filtNames.add(filterName);
	}

	boolean mapsLoaded = false; // true when LoadMaps called for first time
	// mapping hashes for QueryToMQL
	private Hashtable starBase_Dataset = new Hashtable();
	// starbase to DatasetView internalName
	private Hashtable field_Attribute = new Hashtable();
	// dataset internal_name to List of UIMappers for Attributes
	private Hashtable field_Filter = new Hashtable();
	// dataset internal_name to List of UIMappers for Filters
	private Hashtable field_FilterSet = new Hashtable();
	// dataset internal_name to List of UIMappers for FilterSets 

	private String MQLError = null;

	//MartShellLib instance variables
	private DSViewAdaptor adaptor;
	private String envDataset = null;
	private DataSource envMart = null;
	private AttributePage currentApage = null;
	// keeps track of the AttributePage
	private FilterPage currentFpage = null; // keeps track of the FilterPage
	//	will hold max-select keyed by collection.internalName
	private Hashtable maxSelects = new Hashtable();
	// will hold all previously selected UIAttributeDescriptions, for page constraint validation during addAttribute
	private List atts = new ArrayList();
	private List filtNames = new ArrayList();

	// query instructions
	public static final String GETQSTART = "get";
	public static final String USINGQSTART = "using";
	public static final String QSEQUENCE = "sequence";
	public static final String QWHERE = "where";
	public static final String QLIMIT = "limit";
	public static final char LISTSTARTCHR = '(';
	private final String LSTART = String.valueOf(LISTSTARTCHR);
	private final String QUOTE = "'";
	public static final char LISTENDCHR = ')';
	private final String LEND = String.valueOf(LISTENDCHR);
	private final String ID = "id";
	private final String SEQDELIMITER = "+";
	private final String TABULATED = "tabulated";
	private final String FASTA = "fasta";
	public static final String FILTERDELIMITER = "and";

	protected final List availableCommands = Collections.unmodifiableList(Arrays.asList(new String[] { USINGQSTART, GETQSTART }));

	// variables for subquery
	private int nestedLevel = 0;
	private final int MAXNESTING = 1;
	// change this to allow deeper nesting of queries inside queries

  //Pattern for stored Command
  public static final Pattern STOREPAT = Pattern.compile("(.*)\\s+(a|A)(s|S)\\s+(\\w+)$", Pattern.DOTALL);
  
	public static List ALLQUALIFIERS = Arrays.asList(new String[] { "=", "!=", "<", ">", "<=", ">=", "only", "excluded", "in" });

	public static List BOOLEANQUALIFIERS = Arrays.asList(new String[] { "only", "excluded" });
	public final String[] BOOLEAN_NUMCONDITIONS = { BooleanFilter.isNotNULL_NUM , BooleanFilter.isNULL_NUM	};
	public final String[] BOOLEAN_CONDITIONS = { BooleanFilter.isNotNULL, BooleanFilter.isNULL	};
 
	private Logger logger = Logger.getLogger(MartShellLib.class.getName());
	private Properties storedCommands = new Properties();
	private Hashtable dataSourceMap;
}
