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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.BooleanFilter;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.MapFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MapFilterDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.Option;

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
 * <p class="indent_big">select</p>
 * <p class="indent_big">&lt; attribute_list &gt;</p>
 * <p class="indent_big">&lt; sequence sequence_request &gt;</p>
 * <p class="indent_big">from dataset_name</p>
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
 * <p class="indent_big">- filter_name excluded|exclusive  -- specifies that objects should be returned only if they match (exclusive),</p>
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
 * <p>Note, the minimal MQL request would be 'select attribute_name from dataset_name'.  The minimal sequence request would be 'select sequence sequence_name from dataset'.</p>
 * <p>MQL differs from SQL in not requiring (or even allowing) multiple datasets, table qualifiers, or joins.  You just have to specify the attributes/sequence that you want, the dataset to query, and filters to apply.</p>
 * <p>This makes simple queries which are not that hard at the SQL level, even more simple.</p>
 * <p>Because the Mart-Explorer engine resolves some filters to complex sub querries, it makes more complex underlying querries just as simple.</p>
 * <p>Finally, it makes querries for things like sequences (which are impossible using SQL) just as simple.</p>  
 * <p>MQL statements can be written on one line, or separated with newlines and whitespace to make them easier to read</p>
 * <br>
 * <p>The default output settings are tab - separated, tabulated output
 * to System.out.  Client programs can over ride these settings in one of two ways:</p>
 * <ul>
 *    <li><p>using an into clause within the MQL query (takes highest priority, but only controls the output for a particular query)</p>
 *    <li><p>using the appropriate setter methods (these values are temporarily over ridden by an into clause,
 *      but, once set, they remain in effect for the entire session, until another call to the
 *      settter(s) is(are) made).</p>
 * </ul>
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see org.ensembl.mart.lib.SequenceDescription
 * @see org.ensembl.mart.lib.config.MartConfiguration
 */
public class MartShellLib {

	public MartShellLib(MartConfiguration martconf) {
		this.martconf = martconf;
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
	 * Set or Reset the MartConfiguration object to use in parsing Queries and MQL
	 * statements.
	 * 
	 * @param martconf
	 */
	public void setMartConfiguration(MartConfiguration martconf) {
		this.martconf = martconf;
	}

	private void LoadMaps() {
		if (!mapsLoaded) {
			Dataset[] dsets = martconf.getDatasets();

			for (int i = 0, n = dsets.length; i < n; i++) {
				Dataset dataset = dsets[i];
				String datasetName = dataset.getInternalName();
				starBase_Dataset.put(dataset.getStarBases()[0], datasetName); // first starbase only

				if (!field_Attribute.containsKey(datasetName))
					field_Attribute.put(datasetName, new ArrayList());

				List attMaps = (ArrayList) field_Attribute.get(datasetName);

				List attributes = dataset.getAllUIAttributeDescriptions();
				for (int j = 0, k = attributes.size(); j < k; j++) {
					Object attribute = attributes.get(j);
					if (attribute instanceof AttributeDescription) {
						AttributeDescription uiattribute = (AttributeDescription) attribute;

						String iname = uiattribute.getInternalName();
						String fname = uiattribute.getFieldName();
						String tconstraint = uiattribute.getTableConstraint();

						UIMapper attMap = null;
						if (tconstraint != null && !(tconstraint.equals("")))
							attMap = new UIMapper(fname, tconstraint, iname);
						else
							attMap = new UIMapper(fname, iname);

						if (!attMaps.contains(attMap))
							attMaps.add(attMap);
					}
					//else { UIDSAttributeDescription code goes here}
				}

				// get FilterSetDescription mappings
				if (!field_FilterSet.containsKey(datasetName))
					field_FilterSet.put(datasetName, new ArrayList());

				List fsetMaps = (ArrayList) field_FilterSet.get(datasetName);

				FilterPage[] fpages = dataset.getFilterPages();
				for (int j = 0, u = fpages.length; j < u; j++) {
					FilterPage page = fpages[j];

					FilterSetDescription[] fsetdescs = page.getAllFilterSetDescriptions();
					for (int m = 0, b = fsetdescs.length; j < b; j++) {
						FilterSetDescription description = fsetdescs[m];
						fsetMaps.add(new UIMapper(description.getFieldNameModifier(), description.getInternalName()));
						fsetMaps.add(new UIMapper(description.getTableConstraintModifier(), description.getInternalName()));
					}
				}

				if (!field_Filter.containsKey(datasetName))
					field_Filter.put(datasetName, new ArrayList());

				List fieldMaps = (ArrayList) field_Filter.get(datasetName);

				List filters = dataset.getAllUIFilterDescriptions();
				for (Iterator iter = filters.iterator(); iter.hasNext();) {
					Object filter = iter.next();

					if (filter instanceof FilterDescription) {
						FilterDescription uifilter = (FilterDescription) filter;

						String iname = uifilter.getInternalName();
						String fname = uifilter.getFieldName();
						String tconstraint = uifilter.getTableConstraint();

						UIMapper fieldMap = null;
						if (tconstraint != null && !(tconstraint.equals("")))
							fieldMap = new UIMapper(fname, tconstraint, iname);
						else
							fieldMap = new UIMapper(fname, iname);

						if (!fieldMaps.contains(fieldMap))
							fieldMaps.add(fieldMap);
					} else {
						MapFilterDescription uifilter = (MapFilterDescription) filter;
						fieldMaps.add(new UIMapper(uifilter.getHandler(), uifilter.getInternalName()));
					}
				}
				field_Attribute.put(datasetName, new ArrayList());
				field_Filter.put(datasetName, new ArrayList());
			}
		}
	}

	/**
	 * Creates a Mart Query Language command from a Query object.
	 * 
	 * @param query Query object to be transformed into MQL
	 * @return String MQL statement
	 * throws InvalidQueryException for all underlying exceptions
	 */
	public String QueryToMQL(Query query) throws InvalidQueryException {
		String commandEnd = ";";
		LoadMaps();

		StringBuffer mqlbuf = new StringBuffer();
		boolean success = false;

		String datasetName = getDataset(query);

		//	get datasetName first
		if (datasetName == null)
			throw new InvalidQueryException("Could not determine dataset with starbases from query provided\n");

		if (!martconf.containsDataset(datasetName))
			throw new InvalidQueryException(
				"Dataset " + datasetName + " is not supported by the martConfiguration provided\n");

		Dataset dataset = martconf.getDatasetByName(datasetName);

		success = getGetClause(query, dataset, mqlbuf);

		if (success && (query.getType() == Query.SEQUENCE))
			getSequenceClause(query, mqlbuf.append(" "));

		if (success)
			mqlbuf.append("from ").append(dataset);

		if (success && (query.getTotalFilterCount() > 0))
			success = getWhereClause(query, dataset, mqlbuf.append(" "));

		if (success && query.hasLimit())
			mqlbuf.append("limit ").append(query.getLimit());

		if (!success)
			throw new InvalidQueryException("Could not compile MQL from Query\n" + MQLError + "\n");
		return mqlbuf.append(commandEnd).toString();
	}

	private String getDataset(Query query) {
		String datasetName = null;

		String[] starbases = query.getStarBases();
		for (int i = 0, n = starbases.length; i < n; i++) {
			if (starBase_Dataset.containsKey(starbases[i])) {
				datasetName = (String) starBase_Dataset.get(starbases[i]);
				break;
			}
		}
		return datasetName;
	}

	private boolean getGetClause(Query query, Dataset dataset, StringBuffer mqlbuf) {
		Attribute[] attributes = query.getAttributes();
		mqlbuf.append("select");

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

		List attMaps = (ArrayList) field_Attribute.get(dataset.getInternalName());

		for (int i = 0, n = attributes.length;(success && i < n); i++) {
			if (i > 0)
				mqlbuf.append(", ");

			Attribute attribute = attributes[i];
			String fname = attribute.getField();
			String tconstraint = attribute.getTableConstraint();

			boolean thisMapped = false;

			for (Iterator iter = attMaps.iterator(); !(thisMapped) && iter.hasNext();) {
				UIMapper attMap = (UIMapper) iter.next();

				if (attMap.canMap(fname) || attMap.canMap(fname, tconstraint)) {
					mqlbuf.append(attMap.getInternalName());
					thisMapped = true;
				}
			}

			if (!thisMapped) {
				success = false;
				MQLError = "Could not map attribute " + attribute;
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

	private boolean getWhereClause(Query query, Dataset dataset, StringBuffer mqlbuf) {
		boolean success = true;

		mqlbuf.append("where ");
		List filtMaps = (ArrayList) field_Filter.get(dataset.getInternalName());
		List FiltSetMaps = (ArrayList) field_FilterSet.get(dataset.getInternalName());

		if (query.hasDomainSpecificFilters()) {
			MapFilter[] dsfilters = query.getDomainSpecificFilters();

			for (int i = 0, n = dsfilters.length;(success && (i < n)); i++) {
				if (i > 0)
					mqlbuf.append(", ");

				MapFilter dsfilter = dsfilters[i];
				boolean thisMapped = false;

				String objectCode = dsfilter.getHandler();
				String value = dsfilter.getCludgyParameter();

				for (Iterator iter = filtMaps.iterator(); !(thisMapped) && iter.hasNext();) {
					UIMapper filtMap = (UIMapper) iter.next();

					if (filtMap.canMap(objectCode)) {
						mqlbuf.append(filtMap.getInternalName()).append(" = ").append(value);
						thisMapped = true;
					}
				}

				if (!thisMapped) {
					success = false;
					MQLError = "Could not map domain specific filter " + dsfilter;
				}
			}
		}

		if (success && query.hasUnprocessedListFilters()) {
			IDListFilter[] unprocessedFilters = query.getUnprocessedListFilters();

			for (int i = 0, n = unprocessedFilters.length;(success && (i < n)); i++) {
				if (i > 0)
					mqlbuf.append(", ");

				IDListFilter idfilter = unprocessedFilters[i];
				boolean thisMapped = false;

				String fname = idfilter.getField();
				String tconstraint = idfilter.getTableConstraint();

				for (Iterator iter = filtMaps.iterator(); success && !(thisMapped) && iter.hasNext();) {
					UIMapper filtMapper = (UIMapper) iter.next();

					String filterSetReq = null;

					if (filtMapper.canMap(fname)) {
						FilterDescription uifilter =
							(FilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field -> internalName relationship
							thisMapped = true;
							success = mapIDListFilter(idfilter, mqlbuf.append(filtMapper.getInternalName()).append(" in "));
						}
					} else if (filtMapper.canMap(fname, tconstraint)) {
						FilterDescription uifilter =
							(FilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field + tableconstraint -> internalName relationship
							thisMapped = true;
							success = mapIDListFilter(idfilter, mqlbuf.append(filtMapper.getInternalName()).append(" in "));
						}
					} else {
						// filterSet
						String filterInternalName = filtMapper.getInternalName();
						FilterDescription uifilter = (FilterDescription) dataset.getUIFilterDescriptionByName(filterInternalName);
						// must be a FilterDescription

						if (uifilter.inFilterSet()) {
							if (uifilter.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
								if (fname.endsWith(filtMapper.getPrimaryKey())) {
									// field modifier
									String fieldModifier = tconstraint.substring(0, tconstraint.indexOf(filtMapper.getPrimaryKey()));

									for (Iterator iterator = FiltSetMaps.iterator(); success && !(thisMapped) && iterator.hasNext();) {
										UIMapper setMap = (UIMapper) iterator.next();
										if (setMap.canMap(fieldModifier)) {
											thisMapped = true;
											success =
												mapIDListFilter(
													idfilter,
													mqlbuf.append(setMap.getInternalName()).append(".").append(
														filtMapper.getInternalName()).append(
														" in "));
										}
									}
								}
							} else {
								if (tconstraint.endsWith(filtMapper.getCompositeKey())) {
									// table modifier
									String tableModifier = tconstraint.substring(0, tconstraint.indexOf(filtMapper.getCompositeKey()));

									for (Iterator iterator = FiltSetMaps.iterator(); success && !(thisMapped) && iterator.hasNext();) {
										UIMapper setMap = (UIMapper) iterator.next();
										if (setMap.canMap(tableModifier)) {
											thisMapped = true;
											success =
												mapIDListFilter(
													idfilter,
													mqlbuf.append(setMap.getInternalName()).append(".").append(
														filtMapper.getInternalName()).append(
														" in "));
										}
									}
								}
							}
						}
					}
				}

				if (!thisMapped) {
					success = false;
					MQLError = "Could not map IDListFilter " + idfilter;
				}
			}
		}

		Filter[] filters = query.getFilters();

		if (success && filters.length > 0) {
			for (int i = 0, n = filters.length;(success && (i < n)); i++) {
				Filter filter = filters[i];
				boolean thisMapped = false;

				String fname = filter.getField();
				String tconstraint = filter.getTableConstraint();

				for (Iterator iter = filtMaps.iterator(); success && !(thisMapped) && iter.hasNext();) {
					UIMapper filtMapper = (UIMapper) iter.next();

					String filterSetReq = null;

					if (filtMapper.canMap(fname)) {
						FilterDescription uifilter =
							(FilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field -> internalName relationship
							thisMapped = true;
							success = mapBasicFilter(filter, mqlbuf.append(filtMapper.getInternalName()).append(" "));
						}
					} else if (filtMapper.canMap(fname, tconstraint)) {
						FilterDescription uifilter =
							(FilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field + tableconstraint -> internalName relationship
							thisMapped = true;
							success = mapBasicFilter(filter, mqlbuf.append(filtMapper.getInternalName()).append(" "));
						}
					} else {
						// filterSet
						String filterInternalName = filtMapper.getInternalName();
						FilterDescription uifilter = (FilterDescription) dataset.getUIFilterDescriptionByName(filterInternalName);
						// must be a FilterDescription

						if (uifilter.inFilterSet()) {
							if (uifilter.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
								if (fname.endsWith(filtMapper.getPrimaryKey())) {
									// field modifier
									String fieldModifier = tconstraint.substring(0, tconstraint.indexOf(filtMapper.getPrimaryKey()));

									for (Iterator iterator = FiltSetMaps.iterator(); success && !(thisMapped) && iterator.hasNext();) {
										UIMapper setMap = (UIMapper) iterator.next();
										if (setMap.canMap(fieldModifier)) {
											thisMapped = true;
											success =
												mapBasicFilter(
													filter,
													mqlbuf.append(setMap.getInternalName()).append(".").append(
														filtMapper.getInternalName()).append(
														" "));
										}
									}
								}
							} else {
								if (tconstraint.endsWith(filtMapper.getCompositeKey())) {
									// table modifier
									String tableModifier = tconstraint.substring(0, tconstraint.indexOf(filtMapper.getCompositeKey()));

									for (Iterator iterator = FiltSetMaps.iterator(); success && !(thisMapped) && iterator.hasNext();) {
										UIMapper setMap = (UIMapper) iterator.next();
										if (setMap.canMap(tableModifier)) {
											thisMapped = true;
											success =
												mapBasicFilter(
													filter,
													mqlbuf.append(setMap.getInternalName()).append(".").append(
														filtMapper.getInternalName()).append(
														" "));
										}
									}
								}
							}
						}
					}
				}

				if (!thisMapped) {
					success = false;
					MQLError = "Could not map Filter " + filter;
				}
			}
		}

		return success;
	}

	private boolean mapIDListFilter(IDListFilter idfilter, StringBuffer mqlbuf) {
		boolean success = true;

		switch (idfilter.getType()) {
			case IDListFilter.FILE :
				mqlbuf.append(idfilter.getFile());
				break;

			case IDListFilter.URL :
				mqlbuf.append(idfilter.getUrl());
				break;

			case IDListFilter.SUBQUERY :
				Query subq = idfilter.getSubQuery();

				try {
					mqlbuf.append("(").append(QueryToMQL(subq)).append(")");
				} catch (InvalidQueryException e) {
					success = false;
					MQLError = ("Could not map subquery:\n" + subq + "\n" + e);
				}
				break;

			case IDListFilter.STRING :
				String[] ids = idfilter.getIdentifiers();
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

	private boolean mapBasicFilter(Filter filter, StringBuffer mqlbuf) {
		boolean success = true;

		if (filter instanceof BooleanFilter) {
			String condition = ((BooleanFilter) filter).getRightHandClause();
			if (condition.equals(BooleanFilter.isNULL) || condition.equals(BooleanFilter.isNULL_NUM))
				mqlbuf.append("excluded");
			else
				mqlbuf.append("exclusive");
		} else if (filter instanceof IDListFilter) {
			success = mapIDListFilter((IDListFilter) filter, mqlbuf.append("in "));
		} else if (filter instanceof BasicFilter) {
			BasicFilter bfilter = (BasicFilter) filter;
			mqlbuf.append(bfilter.getCondition()).append(" ").append(bfilter.getValue());
		}
		//dont need else

		return success;
	}

	/**
	 *  Allows users to store MQL to use as subqueries in other MQL.
	 * 
	 * @param key - String name to refer to this stored Command in later queries.
	 * @param mql - String mql subquery.
	 */
	public void addStoredMQLCommand(String key, String mql) {
		logger.info("Storing command with key " + key + "\n");
		storedCommands.put(key, mql);
	}

	/** 
	 * Creates a Query object from a Mart Query Language command.
	 * 
	 * @param mql - String MQL command to parse into a Query object
	 * @return Query object
	 * @throws InvalidQueryException for all underlying exceptions (MQL syntax errors, Dataset/Attributes/Sequences/Filters not found, etc.)
	 */
	public Query MQLtoQuery(String newquery) throws InvalidQueryException {
		boolean start = true;
		boolean getClause = false;
		boolean usingClause = false;
		boolean sequenceClause = false;
		boolean fromClause = false;
		boolean whereClause = false;
		boolean limitClause = false;
		boolean inList = false;
		boolean inBind = false;
		boolean whereFilterName = false;
		boolean whereFilterCond = false;
		boolean whereFilterVal = false;

		logger.info("Recieved Query " + newquery + "\n");

		Dataset dset = null;
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

		StringTokenizer cTokens = new StringTokenizer(newquery, " ");

		if (cTokens.countTokens() < 2)
			throw new InvalidQueryException("\nInvalid Query Recieved " + newquery + "\n");

		while (cTokens.hasMoreTokens()) {
			String thisToken = cTokens.nextToken();
			if (start) {
				if (!(thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART)))
					throw new InvalidQueryException(
						"Invalid Query Recieved, should begin with either 'using' or 'get': " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(GETQSTART)) {
					start = false;
					getClause = true;
				} else {
					start = false;
					usingClause = true;
				}
			} else if (usingClause) {
				if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException(
						"Invalid Query Recieved, sequence clause before " + GETQSTART + " clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException(
						"Invalid Query Recieved, where clause before " + GETQSTART + " clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT))
					throw new InvalidQueryException(
						"Invalid Query Recieved, limit clause before " + GETQSTART + " clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART)) {
					usingClause = false;
					getClause = true;
				} else {
					if (dset != null)
						throw new InvalidQueryException(
							"Invalid Query Recieved, dataset already set, attempted to set again: " + newquery + "\n");
					else {
						if (!martconf.containsDataset(thisToken))
							throw new InvalidQueryException("Dataset " + thisToken + " is not found in this mart\n");
						dset = martconf.getDatasetByName(thisToken);
					}

				}
			} else if (getClause) {
				if (thisToken.endsWith(","))
					thisToken = thisToken.substring(0, thisToken.length() - 1);

				// set dataset and update query with starbases, or throw an exception if dataset not set
				if (dset == null) {
					if (envDataset == null) {
						throw new InvalidQueryException("Invalid Query Recieved, did not set dataset: " + newquery + "\n");
					} else {
						if (!martconf.containsDataset(envDataset))
							throw new InvalidQueryException("Dataset " + envDataset + " is not found in this mart\n");
						dset = martconf.getDatasetByName(envDataset);
					}
				}
				query.setStarBases(dset.getStarBases());
				query.setPrimaryKeys(dset.getPrimaryKeys());

				if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException(
						"Invalid Query Recieved, "
							+ GETQSTART
							+ " clause in the middle of a "
							+ GETQSTART
							+ " clause: "
							+ newquery
							+ "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					getClause = false;
					limitClause = true;
				} else if (thisToken.equalsIgnoreCase(QSEQUENCE)) {
					getClause = false;
					sequenceClause = true;
					//else if (thisToken.equalsIgnoreCase(USER_SUPPLIED_KEYWORDS) {
					//  selectClause = false;
					//  USERSUPPLIEDMODE = true;
					//}
				} else if (thisToken.equalsIgnoreCase(QWHERE)) {
					getClause = false;
					whereClause = true;
					whereFilterName = true;
				} else {
					StringTokenizer attToks = new StringTokenizer(thisToken, ",");
					while (attToks.hasMoreTokens())
						query = addAttribute(query, dset, attToks.nextToken().trim());
				}
			} else if (sequenceClause) {
				if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException(
						"Invalid Query Recieved, " + GETQSTART + " clause in the middle of a sequence clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					sequenceClause = false;
					limitClause = true;
				} else if (thisToken.equalsIgnoreCase(QWHERE)) {
					sequenceClause = false;
					whereClause = true;
					whereFilterName = true;
				} else
					query = addSequenceDescription(query, dset, thisToken);
				// else if (USERSUPPLIEDMODE) {
				//   user code goes here to parse new modes
				//}
			} else if (whereClause) {
				if (thisToken.equalsIgnoreCase(QLIMIT)) {
					whereClause = false;
					limitClause = true;
				} else if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException(
						"Invalid Query Recieved, " + GETQSTART + " clause after where clause, not in subquery: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException(
						"Invalid Query Recieved, where clause after where clause, not in subquery: " + newquery + "\n");

				else if (thisToken.equalsIgnoreCase(ANDC)) {
					whereFilterCond = false;
					whereFilterVal = false;
					whereFilterName = true;
				} else if (whereFilterName) {
					if (thisToken.indexOf("=") >= 0) {
						filterCondition = "=";
						StringTokenizer filtToks = new StringTokenizer(thisToken, "=");

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
					if (thisToken.indexOf("exclu") >= 0) {
						query = addBooleanFilter(query, dset, filterName, thisToken);

						filterValue = new StringBuffer();
						filterName = null;
						filterCondition = null;
						whereFilterName = false;
						whereFilterCond = false;
						whereFilterVal = false;
					} else if (thisToken.indexOf("=") >= 0 && thisToken.length() > 1) {
						StringTokenizer filtToks = new StringTokenizer(thisToken, "=");
						query = addBasicFilter(query, dset, filterName, "=", filtToks.nextToken());

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
					if (thisToken.equals(LSTART)) {
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

							logger.info(filterName + " is an in filter with valuestart " + thisToken + "\n");

							if (thisToken.indexOf(":") >= 0) {
								//url
								try {
									query = addListFilter(query, dset, filterName, new URL(thisToken));
								} catch (Exception e) {
									throw new InvalidQueryException(
										"Error adding url filter " + filterName + " " + thisToken + " " + e.getMessage(),
										e);
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
					throw new InvalidQueryException(
						"Invalid Query Recieved, invalid filter statement in where clause: " + newquery + "\n");
			} else if (limitClause) {
				if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException(
						"Invalid Query Recieved, " + GETQSTART + " clause in limit clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException(
						"Invalid Query Recieved, sequence clause in limit clause: " + newquery + "\n");
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
			throw new InvalidQueryException(
				"Invalid Query Recieved, no attributes or sequence description found " + newquery + "\n");

		return query;
	}

	private Filter getIDFilterForSubQuery(String fieldName, String tableConstraint, String storedCommandName)
		throws InvalidQueryException {

		String bindValues = null;
		if (storedCommandName.indexOf(LSTART) > 0) {
			bindValues = storedCommandName.substring(storedCommandName.indexOf(LSTART) + 1, storedCommandName.indexOf(LEND));
			storedCommandName = storedCommandName.substring(0, storedCommandName.indexOf(LSTART));
		}

		if (!storedCommands.containsKey(storedCommandName))
			throw new InvalidQueryException(storedCommandName + " is not available as a stored MQL Command\n");

		String nestedQuery = storedCommands.getProperty(storedCommandName);

		if (!(bindValues == null) && (bindValues.length() > 0)) {
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

		nestedLevel++;

		logger.info("Recieved nested query " + nestedQuery + "\n\nat nestedLevel " + nestedLevel + "\n");

		if (nestedLevel > MAXNESTING)
			throw new InvalidQueryException("Only " + MAXNESTING + " levels of nested Query are allowed\n");

		//validate, then call parseQuery on the subcommand
		String[] tokens = nestedQuery.split("\\s");
		if (!tokens[0].trim().equals(USINGQSTART))
			throw new InvalidQueryException(
				"Invalid Nested Query Recieved: no using statement recieved " + tokens[0].trim() + " in " + nestedQuery + "\n");

		for (int i = 1, n = tokens.length; i < n; i++) {
			String tok = tokens[i];
			if (tok.equals(QSEQUENCE))
				throw new InvalidQueryException(
					"Invalid Nested Query Recieved: sequence statement not allowed " + nestedQuery + "\n");
			//else not needed
		}

		Query subQuery = null;
		try {
			subQuery = MQLtoQuery(nestedQuery);
		} catch (Exception e) {
			throw new InvalidQueryException("Could not parse Nested Query : " + e.getMessage());
		}

		Filter f = new IDListFilter(fieldName, tableConstraint, subQuery);

		nestedLevel--;
		return f;
	}

	private Query addSequenceDescription(Query inquery, Dataset dset, String seqrequest) throws InvalidQueryException {
		currentApage = dset.getAttributePageByName("sequences");
		for (int i = 0, n = atts.size(); i < n; i++) {
			String element = (String) atts.get(i);

			if (!currentApage.containsUIAttributeDescription(element))
				throw new InvalidQueryException(
					"Cannot request attribute "
						+ element
						+ " together with sequences in the same query.  Use show attributes for a list of attributes that can be selected with sequences\n");
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

	private Query addAttribute(Query inquery, Dataset dset, String attname) throws InvalidQueryException {
		checkAttributeValidity(dset, attname);

		Query newQuery = new Query(inquery);
		AttributeDescription attdesc = (AttributeDescription) dset.getUIAttributeDescriptionByName(attname);
		Attribute attr = new FieldAttribute(attdesc.getFieldName(), attdesc.getTableConstraint());
		newQuery.addAttribute(attr);

		return newQuery;
	}

	private void checkAttributeValidity(Dataset dset, String attname) throws InvalidQueryException {
		if (!dset.containsUIAttributeDescription(attname))
			throw new InvalidQueryException(
				"Attribute " + attname + " is not found in this mart for dataset " + dset.getInternalName() + "\n");

		//check page
		if (currentApage == null) {
			currentApage = dset.getPageForAttribute(attname);
		} else {
			if (!currentApage.containsUIAttributeDescription(attname)) {
				currentApage = dset.getPageForAttribute(attname);

				for (int i = 0, n = atts.size(); i < n; i++) {
					String element = (String) atts.get(i);

					if (!currentApage.containsUIAttributeDescription(element))
						throw new InvalidQueryException(
							"Cannot request attributes "
								+ attname
								+ " and "
								+ element
								+ " together in the same query.  Use show attributes for a list of attributes that can be selected together\n");
				}
			}
		}

		//check maxSelect
		AttributeCollection collection = currentApage.getCollectionForAttribute(attname);
		String colname = collection.getInternalName();
		int maxSelect = collection.getMaxSelect();

		if (maxSelect > 0) {
			if (maxSelects.containsKey(colname)) {
				int oldMax = ((Integer) maxSelects.get(colname)).intValue();
				oldMax++;
				if (oldMax > maxSelect)
					throw new InvalidQueryException(
						"You cannot select more than " + maxSelect + " attributes from AttributeCollection " + colname + "\n");
				maxSelects.put(colname, new Integer(oldMax));
			} else
				maxSelects.put(colname, new Integer(1));
		}
		
		atts.add(attname);
	}

	private Query addBooleanFilter(Query inquery, Dataset dset, String filterName, String filterCondition)
		throws InvalidQueryException {
		String filterSetName = null;

		if (filterName.indexOf(".") > 0) {
			StringTokenizer dtokens = new StringTokenizer(filterName, ".");
			if (dtokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Invalid FilterSet Request, must be filtersetname.filtername: " + filterName + "\n");
			filterSetName = dtokens.nextToken();
			filterName = dtokens.nextToken();
		}

		checkFilterValidity(dset, filterName);

		Filter thisFilter = null;
		String thisFieldName = null;
		String thisTableConstraint = null;

		if (dset.getUIFilterDescriptionByName(filterName) instanceof FilterDescription) {
			FilterDescription fds = (FilterDescription) dset.getUIFilterDescriptionByName(filterName);

			if (!(fds.getType().matches("boolean.*")))
				throw new InvalidQueryException(
					"Filter "
						+ filterName
						+ " is not a boolean filter, and does not support condition "
						+ filterCondition
						+ "\n");

			FilterSetDescription fset = null;

			if (filterSetName != null) {
				if (!currentFpage.containsFilterSetDescription(filterSetName))
					throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
				else
					fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
			}

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset " + filterName + "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getFieldName();
					}
				}
			} else {
				thisFieldName = fds.getFieldName();
				thisTableConstraint = fds.getTableConstraint();
			}

			if (fds.getType().equals("boolean")) {
				String thisCondition = null;
				if (filterCondition.equalsIgnoreCase("exclusive"))
					thisCondition = BooleanFilter.isNotNULL;
				else if (filterCondition.equalsIgnoreCase("excluded"))
					thisCondition = BooleanFilter.isNULL;
				else
					throw new InvalidQueryException(
						"Invalid Query Recieved, Filter Name, Condition with no value: "
							+ filterName
							+ " "
							+ filterCondition
							+ "\n");

				thisFilter = new BooleanFilter(thisFieldName, thisTableConstraint, thisCondition);
			} else if (fds.getType().equals("boolean_num")) {
				String thisCondition;
				if (filterCondition.equalsIgnoreCase("exclusive"))
					thisCondition = BooleanFilter.isNULL_NUM;
				else if (filterCondition.equalsIgnoreCase("excluded"))
					thisCondition = BooleanFilter.isNotNULL_NUM;
				else
					throw new InvalidQueryException(
						"Invalid Query Recieved, Filter Name, Condition with no value: "
							+ filterName
							+ " "
							+ filterCondition
							+ "\n");

				thisFilter = new BooleanFilter(thisFieldName, thisTableConstraint, thisCondition);
			}
		} else {
			//option
			Option fds = (Option) dset.getUIFilterDescriptionByName(filterName);

			if (!(fds.getType().matches("boolean.*")))
				throw new InvalidQueryException(
					"Filter "
						+ filterName
						+ " is not a boolean filter, and does not support condition "
						+ filterCondition
						+ "\n");

			FilterSetDescription fset = null;

			if (filterSetName != null) {
				if (!currentFpage.containsFilterSetDescription(filterSetName))
					throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
				else
					fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
			}

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset " + filterName + "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getField();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getField();
					}
				}
			} else {
				thisFieldName = fds.getField();
				thisTableConstraint = fds.getTableConstraint();
			}

			if (fds.getType().equals("boolean")) {
				String thisCondition = null;
				if (filterCondition.equalsIgnoreCase("exclusive"))
					thisCondition = BooleanFilter.isNotNULL;
				else if (filterCondition.equalsIgnoreCase("excluded"))
					thisCondition = BooleanFilter.isNULL;
				else
					throw new InvalidQueryException(
						"Invalid Query Recieved, Filter Name, Condition with no value: "
							+ filterName
							+ " "
							+ filterCondition
							+ "\n");

				thisFilter = new BooleanFilter(thisFieldName, thisTableConstraint, thisCondition);
			} else if (fds.getType().equals("boolean_num")) {
				String thisCondition;
				if (filterCondition.equalsIgnoreCase("exclusive"))
					thisCondition = BooleanFilter.isNULL_NUM;
				else if (filterCondition.equalsIgnoreCase("excluded"))
					thisCondition = BooleanFilter.isNotNULL_NUM;
				else
					throw new InvalidQueryException(
						"Invalid Query Recieved, Filter Name, Condition with no value: "
							+ filterName
							+ " "
							+ filterCondition
							+ "\n");

				thisFilter = new BooleanFilter(thisFieldName, thisTableConstraint, thisCondition);
			}
		}

		Query newQuery = new Query(inquery);
		newQuery.addFilter(thisFilter);
		return newQuery;
	}

	private Query addBasicFilter(
		Query inquery,
		Dataset dset,
		String filterName,
		String filterCondition,
		String filterValue)
		throws InvalidQueryException {
		String filterSetName = null;

		if (filterName.indexOf(".") > 0) {
			StringTokenizer dtokens = new StringTokenizer(filterName, ".");
			if (dtokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Invalid FilterSet Request, must be filtersetname.filtername: " + filterName + "\n");
			filterSetName = dtokens.nextToken();
			filterName = dtokens.nextToken();
		}

		checkFilterValidity(dset, filterName);

		if (dset.getUIFilterDescriptionByName(filterName) instanceof FilterDescription) {
			FilterSetDescription fset = null;

			if (filterSetName != null) {
				if (!currentFpage.containsFilterSetDescription(filterSetName))
					throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
				else
					fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
			}

			Query newQuery = new Query(inquery);
			String thisFieldName = null;
			String thisTableConstraint = null;

			FilterDescription fds = (FilterDescription) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getFieldName();
					}
				}
			} else {
				thisFieldName = fds.getFieldName();
				thisTableConstraint = fds.getTableConstraint();
			}

			newQuery.addFilter(new BasicFilter(thisFieldName, thisTableConstraint, filterCondition, filterValue));
			return newQuery;
		} else
			return addMapFilter(inquery, dset, filterName, filterValue);
	}

	private Query addListFilter(Query inquery, Dataset dset, String filterName, List filterValues)
		throws InvalidQueryException {

		String filterSetName = null;

		if (filterName.indexOf(".") > 0) {
			StringTokenizer dtokens = new StringTokenizer(filterName, ".");
			if (dtokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Invalid FilterSet Request, must be filtersetname.filtername: " + filterName + "\n");
			filterSetName = dtokens.nextToken();
			filterName = dtokens.nextToken();
		}

		checkFilterValidity(dset, filterName);

		FilterSetDescription fset = null;

		if (filterSetName != null) {
			if (!currentFpage.containsFilterSetDescription(filterSetName))
				throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
			else
				fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
		}

		String thisFieldName = null;
		String thisTableConstraint = null;

		if (dset.getUIFilterDescriptionByName(filterName) instanceof FilterDescription) {
			FilterDescription fds = (FilterDescription) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getFieldName();
					}
				}
			} else {
				thisFieldName = fds.getFieldName();
				thisTableConstraint = fds.getTableConstraint();
			}
		} else {
			//option
			Option fds = (Option) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getField();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getField();
					}
				}
			} else {
				thisFieldName = fds.getField();
				thisTableConstraint = fds.getTableConstraint();
			}
		}

		Query newQuery = new Query(inquery);
		String[] filtvalues = (String[]) filterValues.toArray(new String[filterValues.size()]);
		newQuery.addFilter(new IDListFilter(thisFieldName, thisTableConstraint, filtvalues));

		return newQuery;
	}

	private Query addListFilter(Query inquery, Dataset dset, String filterName, File fileloc)
		throws InvalidQueryException {
		String filterSetName = null;

		if (filterName.indexOf(".") > 0) {
			StringTokenizer dtokens = new StringTokenizer(filterName, ".");
			if (dtokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Invalid FilterSet Request, must be filtersetname.filtername: " + filterName + "\n");
			filterSetName = dtokens.nextToken();
			filterName = dtokens.nextToken();
		}

		checkFilterValidity(dset, filterName);

		FilterSetDescription fset = null;

		if (filterSetName != null) {
			if (!currentFpage.containsFilterSetDescription(filterSetName))
				throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
			else
				fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
		}

		String thisFieldName = null;
		String thisTableConstraint = null;

		if (dset.getUIFilterDescriptionByName(filterName) instanceof FilterDescription) {
			FilterDescription fds = (FilterDescription) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getFieldName();
					}
				}
			} else {
				thisFieldName = fds.getFieldName();
				thisTableConstraint = fds.getTableConstraint();
			}
		} else {
			//option
			Option fds = (Option) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getField();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getField();
					}
				}
			} else {
				thisFieldName = fds.getField();
				thisTableConstraint = fds.getTableConstraint();
			}
		}

		Query newQuery = new Query(inquery);
		newQuery.addFilter(new IDListFilter(thisFieldName, thisTableConstraint, fileloc));
		return newQuery;
	}

	private Query addListFilter(Query inquery, Dataset dset, String filterName, URL urlLoc)
		throws InvalidQueryException {
		String filterSetName = null;

		if (filterName.indexOf(".") > 0) {
			StringTokenizer dtokens = new StringTokenizer(filterName, ".");
			if (dtokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Invalid FilterSet Request, must be filtersetname.filtername: " + filterName + "\n");
			filterSetName = dtokens.nextToken();
			filterName = dtokens.nextToken();
		}

		checkFilterValidity(dset, filterName);

		FilterSetDescription fset = null;

		if (filterSetName != null) {
			if (!currentFpage.containsFilterSetDescription(filterSetName))
				throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
			else
				fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
		}

		String thisFieldName = null;
		String thisTableConstraint = null;

		if (dset.getUIFilterDescriptionByName(filterName) instanceof FilterDescription) {
			FilterDescription fds = (FilterDescription) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getFieldName();
					}
				}
			} else {
				thisFieldName = fds.getFieldName();
				thisTableConstraint = fds.getTableConstraint();
			}
		} else {
			//option
			Option fds = (Option) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getField();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getField();
					}
				}
			} else {
				thisFieldName = fds.getField();
				thisTableConstraint = fds.getTableConstraint();
			}
		}

		Query newQuery = new Query(inquery);
		newQuery.addFilter(new IDListFilter(thisFieldName, thisTableConstraint, urlLoc));
		return newQuery;
	}

	private Query addListFilter(Query inquery, Dataset dset, String filterName, String storedQueryName)
		throws InvalidQueryException {
		String filterSetName = null;

		if (filterName.indexOf(".") > 0) {
			StringTokenizer dtokens = new StringTokenizer(filterName, ".");
			if (dtokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Invalid FilterSet Request, must be filtersetname.filtername: " + filterName + "\n");
			filterSetName = dtokens.nextToken();
			filterName = dtokens.nextToken();
		}

		checkFilterValidity(dset, filterName);

		FilterSetDescription fset = null;

		if (filterSetName != null) {
			if (!currentFpage.containsFilterSetDescription(filterSetName))
				throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
			else
				fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
		}

		String thisFieldName = null;
		String thisTableConstraint = null;

		if (dset.getUIFilterDescriptionByName(filterName) instanceof FilterDescription) {
			FilterDescription fds = (FilterDescription) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getFieldName();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getFieldName();
					}
				}
			} else {
				thisFieldName = fds.getFieldName();
				thisTableConstraint = fds.getTableConstraint();
			}
		} else {
			//option
			Option fds = (Option) dset.getUIFilterDescriptionByName(filterName);

			if (fds.inFilterSet()) {
				if (fset == null)
					throw new InvalidQueryException(
						"Request for this filter must be specified with a filterset via filtersetname.filtername: "
							+ filterName
							+ "\n");
				else {
					if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
						thisFieldName = fset.getFieldNameModifier() + fds.getField();
						thisTableConstraint = fds.getTableConstraint();
					} else {
						thisTableConstraint = fset.getTableConstraintModifier() + fds.getTableConstraint();
						thisFieldName = fds.getField();
					}
				}
			} else {
				thisFieldName = fds.getField();
				thisTableConstraint = fds.getTableConstraint();
			}
		}

		Query newQuery = new Query(inquery);

		// subquery will overwrite page states, need to return them to original after it is parsed
		AttributePage bakApage = currentApage;
		FilterPage bakFpage = currentFpage;
		currentApage = null;
		currentFpage = null;

		newQuery.addFilter(getIDFilterForSubQuery(thisFieldName, thisTableConstraint, storedQueryName));

		currentApage = bakApage;
		currentFpage = bakFpage;
		return newQuery;
	}

	private Query addMapFilter(Query inquery, Dataset dset, String filterName, String filterValue)
		throws InvalidQueryException {
		String filterSetName = null;

		if (filterName.indexOf(".") > 0) {
			StringTokenizer dtokens = new StringTokenizer(filterName, ".");
			if (dtokens.countTokens() < 2)
				throw new InvalidQueryException(
					"Invalid FilterSet Request, must be filtersetname.filtername: " + filterName + "\n");
			filterSetName = dtokens.nextToken();
			filterName = dtokens.nextToken();
		}

		checkFilterValidity(dset, filterName);

		FilterSetDescription fset = null;

		if (filterSetName != null) {
			if (!currentFpage.containsFilterSetDescription(filterSetName))
				throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
			else
				fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
		}

		Query newQuery = new Query(inquery);

		String thisHandlerParam = null;

		MapFilterDescription fds = (MapFilterDescription) dset.getUIFilterDescriptionByName(filterName);

		if (fds.inFilterSet()) {
			if (fset == null)
				throw new InvalidQueryException(
					"Request for this filter must be specified with a filterset via filtersetname.filtername: "
						+ filterName
						+ "\n");
			else {
				if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME))
					thisHandlerParam = fset.getFieldNameModifier() + ":" + filterValue;
				else
					thisHandlerParam = fset.getTableConstraintModifier() + ":" + filterValue;
			}
		} else
			thisHandlerParam = filterValue;

		newQuery.addDomainSpecificFilter(new MapFilter(fds.getHandler(), thisHandlerParam));
		return newQuery;
	}

	private void checkFilterValidity(Dataset dset, String filterName) throws InvalidQueryException {
		if (!dset.containsUIFilterDescription(filterName))
			throw new InvalidQueryException(
				"Filter " + filterName + " not supported by mart dataset " + dset.getInternalName() + "\n");

		if (currentFpage == null)
			currentFpage = dset.getPageForFilter(filterName);
		else {
			if (!currentFpage.containsUIFilterDescription(filterName)) {
				currentFpage = dset.getPageForFilter(filterName);

				for (int i = 0, n = filtNames.size(); i < n; i++) {
					String element = (String) filtNames.get(i);
					if (!currentFpage.containsUIFilterDescription(element))
						throw new InvalidQueryException(
							"Cannot use filters "
								+ filterName
								+ " and "
								+ element
								+ " together in the same query.  Use 'show filters' to get a list of filters that can be used in the same query.\n");
				}
			}
		}

		filtNames.add(filterName);
	}

	boolean mapsLoaded = false; // true when LoadMaps called for first time
	// mapping hashes for QueryToMQL
	private Hashtable starBase_Dataset = new Hashtable();
	// starbase to Dataset internalName
	private Hashtable field_Attribute = new Hashtable();
	// dataset internal_name to List of UIMappers for Attributes
	private Hashtable field_Filter = new Hashtable();
	// dataset internal_name to List of UIMappers for Filters
	private Hashtable field_FilterSet = new Hashtable();
	// dataset internal_name to List of UIMappers for FilterSets 

	private String MQLError = null;

	//MartShellLib instance variables
	private MartConfiguration martconf;
	private String envDataset = null;
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
	private final String QLIMIT = "limit";
	public static final char LISTSTARTCHR = '(';
	private final String LSTART = String.valueOf(LISTSTARTCHR);
	public static final char LISTENDCHR = ')';
	private final String LEND = String.valueOf(LISTENDCHR);
	private final String ID = "id";
	private final String SEQDELIMITER = "+";
	private final String EXCLUSIVE = "exclusive";
	private final String TABULATED = "tabulated";
	private final String FASTA = "fasta";
	private final String ANDC = "and";

	protected final List availableCommands =
		Collections.unmodifiableList(Arrays.asList(new String[] { QSEQUENCE, QWHERE, QLIMIT, FASTA }));

	//Pattern for stored Command
	public static final Pattern STOREPAT = Pattern.compile("(.*)\\s+(a|A)(s|S)\\s+(\\w+)$", Pattern.DOTALL);

	// variables for subquery
	private int nestedLevel = 0;
	private final int MAXNESTING = 1;
	// change this to allow deeper nesting of queries inside queries

	private List qualifiers =
		Arrays.asList(new String[] { "=", "!=", "<", ">", "<=", ">=", "exclusive", "excluded", "in" });
	private Logger logger = Logger.getLogger(MartShellLib.class.getName());
	private Properties storedCommands = new Properties();
}
