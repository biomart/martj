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
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.DomainSpecificFilter;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.NullableFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.AttributeCollection;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.UIAttributeDescription;
import org.ensembl.mart.lib.config.UIDSFilterDescription;
import org.ensembl.mart.lib.config.UIFilterDescription;

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
					if (attribute instanceof UIAttributeDescription) {
						UIAttributeDescription uiattribute = (UIAttributeDescription) attribute;

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

					if (filter instanceof UIFilterDescription) {
						UIFilterDescription uifilter = (UIFilterDescription) filter;

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
						UIDSFilterDescription uifilter = (UIDSFilterDescription) filter;
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
		LoadMaps();

		StringBuffer mqlbuf = new StringBuffer();
		boolean success = false;

		String datasetName = getDataset(query);

		//	get datasetName first
		if (datasetName == null)
			throw new InvalidQueryException("Could not determine dataset with starbases from query provided\n");

		if (!martconf.containsDataset(datasetName))
			throw new InvalidQueryException("Dataset " + datasetName + " is not supported by the martConfiguration provided\n");

		Dataset dataset = martconf.getDatasetByName(datasetName);

		success = getSelectClause(query, dataset, mqlbuf);

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
		return mqlbuf.append(LINEEND).toString();
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

	private boolean getSelectClause(Query query, Dataset dataset, StringBuffer mqlbuf) {
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
			DomainSpecificFilter[] dsfilters = query.getDomainSpecificFilters();

			for (int i = 0, n = dsfilters.length;(success && (i < n)); i++) {
				if (i > 0)
					mqlbuf.append(", ");

				DomainSpecificFilter dsfilter = dsfilters[i];
				boolean thisMapped = false;

				String objectCode = dsfilter.getObjectCode();
				String value = dsfilter.getHandlerParameter();

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
						UIFilterDescription uifilter = (UIFilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field -> internalName relationship
							thisMapped = true;
							success = mapIDListFilter(idfilter, mqlbuf.append(filtMapper.getInternalName()).append(" in "));
						}
					} else if (filtMapper.canMap(fname, tconstraint)) {
						UIFilterDescription uifilter = (UIFilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field + tableconstraint -> internalName relationship
							thisMapped = true;
							success = mapIDListFilter(idfilter, mqlbuf.append(filtMapper.getInternalName()).append(" in "));
						}
					} else {
						// filterSet
						String filterInternalName = filtMapper.getInternalName();
						UIFilterDescription uifilter = (UIFilterDescription) dataset.getUIFilterDescriptionByName(filterInternalName); // must be a UIFilterDescription

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
												mapIDListFilter(idfilter, mqlbuf.append(setMap.getInternalName()).append(".").append(filtMapper.getInternalName()).append(" in "));
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
												mapIDListFilter(idfilter, mqlbuf.append(setMap.getInternalName()).append(".").append(filtMapper.getInternalName()).append(" in "));
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
						UIFilterDescription uifilter = (UIFilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field -> internalName relationship
							thisMapped = true;
							success = mapBasicFilter(filter, mqlbuf.append(filtMapper.getInternalName()).append(" "));
						}
					} else if (filtMapper.canMap(fname, tconstraint)) {
						UIFilterDescription uifilter = (UIFilterDescription) dataset.getUIFilterDescriptionByName(filtMapper.getInternalName());
						filterSetReq = uifilter.getFilterSetReq();

						if (filterSetReq == null || filterSetReq.equals("")) {
							// perfect field + tableconstraint -> internalName relationship
							thisMapped = true;
							success = mapBasicFilter(filter, mqlbuf.append(filtMapper.getInternalName()).append(" "));
						}
					} else {
						// filterSet
						String filterInternalName = filtMapper.getInternalName();
						UIFilterDescription uifilter = (UIFilterDescription) dataset.getUIFilterDescriptionByName(filterInternalName); // must be a UIFilterDescription

						if (uifilter.inFilterSet()) {
							if (uifilter.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME)) {
								if (fname.endsWith(filtMapper.getPrimaryKey())) {
									// field modifier
									String fieldModifier = tconstraint.substring(0, tconstraint.indexOf(filtMapper.getPrimaryKey()));

									for (Iterator iterator = FiltSetMaps.iterator(); success && !(thisMapped) && iterator.hasNext();) {
										UIMapper setMap = (UIMapper) iterator.next();
										if (setMap.canMap(fieldModifier)) {
											thisMapped = true;
											success = mapBasicFilter(filter, mqlbuf.append(setMap.getInternalName()).append(".").append(filtMapper.getInternalName()).append(" "));
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
											success = mapBasicFilter(filter, mqlbuf.append(setMap.getInternalName()).append(".").append(filtMapper.getInternalName()).append(" "));
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

		if (filter instanceof NullableFilter) {
			String condition = ((NullableFilter) filter).getRightHandClause();
			if (condition.equals(NullableFilter.isNULL) || condition.equals(NullableFilter.isNULL_NUM))
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
		int listLevel = 0; // level of subquery/list

		StringBuffer attString = new StringBuffer();
		StringBuffer sequenceString = new StringBuffer();
		String dataset = null;
		StringBuffer whereString = new StringBuffer();
		int limit = 0;

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
        if (thisToken.endsWith(LINEEND))
          throw new InvalidQueryException("Invalid Query Recieved, using clause with no " + GETQSTART + " clause: " + newquery + "\n");
        else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, sequence clause before " + GETQSTART + " clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
          throw new InvalidQueryException("Invalid Query Recieved, where clause before " + GETQSTART + " clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT))
          throw new InvalidQueryException("Invalid Query Recieved, limit clause before " + GETQSTART + " clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART)) {
          usingClause = false;
          getClause = true;
        } else {
					if (dataset != null)
						throw new InvalidQueryException("Invalid Query Recieved, dataset already set, attempted to set again: " + newquery + "\n");
					else
						dataset = thisToken;
				}
			} else if (getClause) {
        if (thisToken.endsWith(LINEEND))
          thisToken = thisToken.substring(0, thisToken.indexOf(LINEEND));
          
				if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause in the middle of a " + GETQSTART + " clause: " + newquery + "\n");
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
        } else
					attString.append(thisToken);
			} else if (sequenceClause) {
        if (thisToken.endsWith(LINEEND))
          thisToken = thisToken.substring(0, thisToken.indexOf(LINEEND));
          
				if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause in the middle of a sequence clause: " + newquery + "\n");
        else if (thisToken.equalsIgnoreCase(QLIMIT)) {
          sequenceClause = false;
          limitClause = true;
        } else if (thisToken.equalsIgnoreCase(QWHERE)) {
          sequenceClause = false;
          whereClause = true;
				} else
					sequenceString.append(thisToken);
				// else if (USERSUPPLIEDMODE) {
				//   user code goes here to parse new modes
				//}
			} else if (whereClause) {
				if (listLevel < 1 && thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, sequence clause after where clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(LSTART) || thisToken.startsWith(LSTART)) {
					listLevel++;

					if (thisToken.endsWith(LEND) || thisToken.endsWith(LEND + ",") || thisToken.endsWith(LEND + LINEEND)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == LISTENDCHR)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(LINEEND) || thisToken.endsWith(LINEEND)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where clause: " + newquery + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (listLevel > 0) {
					if (thisToken.equalsIgnoreCase(LEND)) {
						listLevel--;
					} else if (thisToken.endsWith(LEND) || thisToken.endsWith(LEND + ",") || thisToken.endsWith(LEND + LINEEND)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == LISTENDCHR)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(LINEEND) || thisToken.endsWith(LINEEND)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where clause: " + newquery + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause after where clause, not in subquery: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where clause after where clause, not in subquery: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					whereClause = false;
					limitClause = true;
				} else
					whereString.append(" ").append(thisToken);
			} else if (limitClause) {
				if (thisToken.equalsIgnoreCase(GETQSTART) || thisToken.equalsIgnoreCase(USINGQSTART))
					throw new InvalidQueryException("Invalid Query Recieved, " + GETQSTART + " clause in limit clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, sequence clause in limit clause: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where clause in limit clause: " + newquery + "\n");
				else {
					if (limit > 0)
						throw new InvalidQueryException("Invalid Query Recieved, attempt to set limit twice: " + newquery + "\n");
					else {
						if (thisToken.endsWith(LINEEND))
							thisToken = thisToken.substring(0, thisToken.length() - 1);
						limit = Integer.parseInt(thisToken);
					}
				}
			}
			// else not needed, as these are the only states present
		}

		if (dataset == null) {
      if (envDataset == null)
			  throw new InvalidQueryException("Invalid Query Recieved, did not set dataset: " + newquery + "\n");
      else
        dataset = envDataset;
    }

		if (attString.length() == 0 && sequenceString.length() == 0)
			throw new InvalidQueryException("Invalid Query Recieved, no attributes or sequence request found: " + newquery + "\n");

		if (dataset.endsWith(LINEEND))
			dataset = dataset.substring(0, dataset.length() - 1);

		if (!martconf.containsDataset(dataset))
			throw new InvalidQueryException("Dataset " + dataset + " is not found in this mart\n");

		Dataset dset = martconf.getDatasetByName(dataset);
		Query query = new Query();
		FilterPage currentFpage = null;

		query.setStarBases(dset.getStarBases());
		query.setPrimaryKeys(dset.getPrimaryKeys());

		if (sequenceString.length() > 0)
			query = addSequenceDescription(query, dset, sequenceString.toString());

		//parse attributes, if present
		if (attString.length() > 1)
			query = addAttributes(query, dset, attString.toString());

		//parse filters, if present
		if (whereString.toString().endsWith(LINEEND))
			whereString.deleteCharAt(whereString.length() - 1);

		if (whereString.length() > 0)
			query = addFilters(query, dset, whereString.toString());

		query.setLimit(limit);
		return query;
	}

	private Filter getIDFilterForSubQuery(String fieldName, String tableConstraint, String nestedQuery) throws InvalidQueryException {
		nestedQuery = nestedQuery.trim();

		nestedLevel++;

		logger.info("Recieved nested query at nestedLevel " + nestedLevel + "\n");

		if (nestedLevel > MAXNESTING)
			throw new InvalidQueryException("Only " + MAXNESTING + " levels of nested Query are allowed\n");

		//validate, then call parseQuery on the subcommand
		String[] tokens = nestedQuery.split("\\s");
		if (!tokens[0].trim().equals(USINGQSTART))
			throw new InvalidQueryException("Invalid Nested Query Recieved: no using statement " + "recieved " + tokens[0].trim() + " in " + nestedQuery + "\n");

		for (int i = 1, n = tokens.length; i < n; i++) {
			String tok = tokens[i];
			if (tok.equals("with"))
				throw new InvalidQueryException("Invalid Nested Query Recieved: with statement not allowed " + nestedQuery + "\n");
			//else not needed
		}

		logger.info("Recieved request for Nested Query\n:" + nestedQuery + "\n");

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

	private Query addAttributes(Query inquery, Dataset dset, String attString) throws InvalidQueryException {
		Query newQuery = new Query(inquery);

		List atts = new ArrayList();
		Hashtable maxSelects = new Hashtable(); // will hold max-select keyed by collection.internalName
		StringTokenizer attTokens = new StringTokenizer(attString.toString(), ",");

		while (attTokens.hasMoreTokens()) {
			String attname = attTokens.nextToken().trim(); // remove leading and trailing whitespace
			if (!dset.containsUIAttributeDescription(attname))
				throw new InvalidQueryException("Attribute " + attname + " is not found in this mart for dataset " + dset.getInternalName() + "\n");

			if (currentApage == null) {
				currentApage = dset.getPageForAttribute(attname);
			} else {
				if (!currentApage.containsUIAttributeDescription(attname)) {
					if (currentApage.getInternalName().equals("sequences"))
						throw new InvalidQueryException("Cannot request attribute " + attname + " with a sequence request\n");

					currentApage = dset.getPageForAttribute(attname);

					for (int i = 0, n = atts.size(); i < n; i++) {
						UIAttributeDescription element = (UIAttributeDescription) atts.get(i);

						if (!currentApage.containsUIAttributeDescription(element.getInternalName()))
							throw new InvalidQueryException(
								"Cannot request attributes from different Attribute Pages " + attname + " in " + currentApage + " intName is not\n");
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
						throw new InvalidQueryException("You cannot select more than " + maxSelect + " attributes from AttributeCollection " + colname + "\n");
					else
						maxSelects.put(colname, new Integer(oldMax));
				} else
					maxSelects.put(colname, new Integer(1));
			}

			atts.add(dset.getUIAttributeDescriptionByName(attname));
		}

		for (int i = 0, n = atts.size(); i < n; i++) {
			UIAttributeDescription attd = (UIAttributeDescription) atts.get(i);
			Attribute attr = new FieldAttribute(attd.getFieldName(), attd.getTableConstraint());
			newQuery.addAttribute(attr);
		}

		return newQuery;
	}

	private Query addFilters(Query inquery, Dataset dset, String whereString) throws InvalidQueryException {
		Query newQuery = new Query(inquery);

		List filtNames = new ArrayList();
		String filterName = null;
		String cond = null;
		String val = null;
		String filterSetName = null;
		FilterSetDescription fset = null;

		boolean start = true;
		int listLevel = 0;

		boolean condition = false;
		boolean value = false;

		boolean isList = false;
		boolean isNested = false;

		List idlist = null; // will hold ids from a list
		StringBuffer subquery = null; // will build up a subquery

		StringTokenizer wTokens = new StringTokenizer(whereString.toString(), " ");

		while (wTokens.hasMoreTokens()) {
			String thisToken = wTokens.nextToken().trim();

			if (start) {
				//reset all values
				filterName = null;
				cond = null;
				val = null;
				filterSetName = null;
				fset = null;
				idlist = new ArrayList();
				subquery = new StringBuffer();
				isNested = false;
				isList = false;

				if (thisToken.indexOf(".") > 0) {
					StringTokenizer dtokens = new StringTokenizer(thisToken, ".");
					if (dtokens.countTokens() < 2)
						throw new InvalidQueryException("Invalid FilterSet Request, must be filtersetname.filtername: " + thisToken + "\n");
					filterSetName = dtokens.nextToken();
					filterName = dtokens.nextToken();
				} else
					filterName = thisToken;

				if (!dset.containsUIFilterDescription(filterName))
					throw new InvalidQueryException("Filter " + filterName + " not supported by mart dataset " + dset.getInternalName() + "\n");
				else {
					if (currentFpage == null)
						currentFpage = dset.getPageForFilter(filterName);
					else {
						if (!currentFpage.containsUIFilterDescription(filterName)) {
							currentFpage = dset.getPageForFilter(filterName);

							for (int i = 0, n = filtNames.size(); i < n; i++) {
								String element = (String) filtNames.get(i);
								if (!currentFpage.containsUIFilterDescription(element))
									throw new InvalidQueryException(
										"Cannot use filters from different FilterPages: filter " + filterName + " in page " + currentFpage + "filter " + element + "is not\n");
							}
						}
					}

					if (filterSetName != null) {
						if (!currentFpage.containsFilterSetDescription(filterSetName))
							throw new InvalidQueryException("Request for FilterSet that is not supported by the current FilterPage for your filter request: ");
						else
							fset = currentFpage.getFilterSetDescriptionByName(filterSetName);
					}
				}

				start = false;
				condition = true;
			} else if (condition) {
				if (!wTokens.hasMoreTokens()) {
					if (!(thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")))
						throw new InvalidQueryException("Invalid Query Recieved, filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
				}

				if (thisToken.endsWith(",")) {
					thisToken = thisToken.substring(0, thisToken.length() - 1);
					if (!(thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")))
						throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
				}

				if (thisToken.endsWith(LINEEND)) {
					thisToken = thisToken.substring(0, thisToken.length() - 1);
					if (!(thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")))
						throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");
				}

				if (thisToken.equalsIgnoreCase("exclusive") || thisToken.equalsIgnoreCase("excluded")) {
					//process exclusive/excluded filter
					String thisFieldName = null;
					String thisTableConstraint = null;

					UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);

					if (fds.inFilterSet()) {
						if (fset == null)
							throw new InvalidQueryException("Request for this filter must be specified with a filterset " + filterName + "\n");
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

					Filter thisFilter = null;

					if (fds.getType().equals("boolean")) {
						String thisCondition = null;
						if (thisToken.equalsIgnoreCase("exclusive"))
							thisCondition = NullableFilter.isNotNULL;
						else if (thisToken.equalsIgnoreCase("excluded"))
							thisCondition = NullableFilter.isNULL;
						else
							throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");

						thisFilter = new NullableFilter(thisFieldName, thisTableConstraint, thisCondition);
					} else if (fds.getType().equals("boolean_num")) {
						String thisCondition;
						if (cond.equalsIgnoreCase("exclusive"))
							thisCondition = NullableFilter.isNULL_NUM;
						else if (cond.equalsIgnoreCase("excluded"))
							thisCondition = NullableFilter.isNotNULL_NUM;
						else
							throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");

						thisFilter = new NullableFilter(thisFieldName, thisTableConstraint, thisCondition);
					} else
						throw new InvalidQueryException("Recieved invalid exclusive/excluded query\n");

					newQuery.addFilter(thisFilter);
					condition = false;
					start = true;
				} else {
					cond = thisToken;
					if (cond.equals("in"))
						isList = true;

					condition = false;
					value = true;
				}
			} else if (value) {
				if (isList) {
					//just get rid of the beginning peren if present
					if (thisToken.startsWith(LSTART)) {
						listLevel++;
						thisToken = thisToken.substring(1);
					}

					if (listLevel < 1) {
						// in File or URL
						if (thisToken.endsWith(LINEEND))
							thisToken = thisToken.substring(0, thisToken.length() - 1);
						if (thisToken.endsWith(","))
							thisToken = thisToken.substring(0, thisToken.length() - 1);

						String thisFieldName = null;
						String thisTableConstraint = null;

						UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
						if (!fds.getType().equals("list"))
							throw new InvalidQueryException("Cannot query this filter with a list input using in qualifier: " + filterName + "\n");

						if (fds.inFilterSet()) {
							if (fset == null)
								throw new InvalidQueryException(
									"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
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

						Filter thisFilter = null;
						try {
							if (thisToken.matches("\\w+\\:.*"))
								thisFilter = new IDListFilter(thisFieldName, thisTableConstraint, new URL(thisToken));
							else
								thisFilter = new IDListFilter(thisFieldName, thisTableConstraint, new File(thisToken));
						} catch (Exception e) {
							throw new InvalidQueryException("URL provided in list Filter not valid: " + e.getMessage());
						}

						newQuery.addFilter(thisFilter);
						start = true;
						value = false;
					} else if (thisToken.equals(USINGQSTART)) {
						isList = false;
						isNested = true;
						subquery.append(" ").append(thisToken);
					} else {
						if (thisToken.endsWith(","))
							thisToken = thisToken.substring(0, thisToken.length() - 1);

						if (thisToken.endsWith(LEND)) {
							value = false;
							start = true;
							listLevel--;
							thisToken = thisToken.substring(0, thisToken.length() - 1);

							//process list
							StringTokenizer idtokens = new StringTokenizer(thisToken, ",");
							while (idtokens.hasMoreTokens()) {
								idlist.add(idtokens.nextToken());
							}
							String thisFieldName = null;
							String thisTableConstraint = null;

							UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
							if (!fds.getType().equals("list"))
								throw new InvalidQueryException("Cannot query this filter with a list input using in qualifier: " + filterName + "\n");

							if (fds.inFilterSet()) {
								if (fset == null)
									throw new InvalidQueryException(
										"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
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

							String[] ids = new String[idlist.size()];
							idlist.toArray(ids);
							Filter thisFilter = new IDListFilter(thisFieldName, thisTableConstraint, ids);
							newQuery.addFilter(thisFilter);
							start = true;
							value = false;
						} else
							idlist.add(thisToken);
					}
				} else if (isNested) {
					if (thisToken.equals(LSTART) || thisToken.startsWith(LSTART))
						listLevel++;

					if (thisToken.indexOf(LEND) >= 0) {
						subquery.append(" ");
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == LISTENDCHR)
								listLevel--;
							if (listLevel > 0)
								subquery.append(thisToken.charAt(i));
						}

						if (listLevel < 1) {
							//process subquery
							String thisFieldName = null;
							String thisTableConstraint = null;

							UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
							if (!fds.getType().equals("list"))
								throw new InvalidQueryException("Cannot query this filter with a list input using in qualifier: " + filterName + "\n");

							if (fds.inFilterSet()) {
								if (fset == null)
									throw new InvalidQueryException(
										"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
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

							// subquery will overwrite page states, need to return them to original after it is parsed
							AttributePage bakApage = currentApage;
							FilterPage bakFpage = currentFpage;
              currentApage = null;
              currentFpage = null;
              
							Filter thisFilter = getIDFilterForSubQuery(thisFieldName, thisTableConstraint, subquery.toString());
							newQuery.addFilter(thisFilter);

							currentApage = bakApage;
							currentFpage = bakFpage;
							start = true;
							value = false;
						}
					} else
						subquery.append(" ").append(thisToken);
				} else {
					if (thisToken.endsWith(","))
						thisToken = thisToken.substring(0, thisToken.length() - 1);

					if (dset.getUIFilterDescriptionByName(filterName) instanceof UIFilterDescription) {
						String thisFieldName = null;
						String thisTableConstraint = null;

						UIFilterDescription fds = (UIFilterDescription) dset.getUIFilterDescriptionByName(filterName);
						if (!fds.getType().equals("list"))
							throw new InvalidQueryException("Cannot query this filter with a list input using in: " + filterName + "\n");

						if (fds.inFilterSet()) {
							if (fset == null)
								throw new InvalidQueryException(
									"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
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

						newQuery.addFilter(new BasicFilter(thisFieldName, thisTableConstraint, cond, thisToken));
						start = true;
						value = false;
					} else {
						String thisHandlerParam = null;

						UIDSFilterDescription fds = (UIDSFilterDescription) dset.getUIFilterDescriptionByName(filterName);

						if (fds.IsInFilterSet()) {
							if (fset == null)
								throw new InvalidQueryException(
									"Request for this filter must be specified with a filterset via filtersetname.filtername: " + filterName + "\n");
							else {
								if (fds.getFilterSetReq().equals(FilterSetDescription.MODFIELDNAME))
									thisHandlerParam = fset.getFieldNameModifier() + ":" + thisToken;
								else
									thisHandlerParam = fset.getTableConstraintModifier() + ":" + thisToken;
							}
						} else
							thisHandlerParam = thisToken;

						DomainSpecificFilter thisFilter = new DomainSpecificFilter(fds.getHandler(), thisHandlerParam);
						newQuery.addDomainSpecificFilter(thisFilter);
						start = true;
						value = false;
					}
				}
			}
			//dont need else
		}

		return newQuery;
	}

	boolean mapsLoaded = false; // true when LoadMaps called for first time
	// mapping hashes for QueryToMQL
	private Hashtable starBase_Dataset = new Hashtable(); // starbase to Dataset internalName
	private Hashtable field_Attribute = new Hashtable(); // dataset internal_name to List of UIMappers for Attributes
	private Hashtable field_Filter = new Hashtable(); // dataset internal_name to List of UIMappers for Filters
	private Hashtable field_FilterSet = new Hashtable(); // dataset internal_name to List of UIMappers for FilterSets  

	private String MQLError = null;

	//MartShellLib instance variables
	private MartConfiguration martconf;
	private String envDataset = null;
	private AttributePage currentApage = null; // keeps track of the AttributePage
	private FilterPage currentFpage = null; // keeps track of the FilterPage

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
	private final String LINEEND = ";";
	private final String ID = "id";
	private final String SEQDELIMITER = "+";
	private final String EXCLUSIVE = "exclusive";
	private final String TABULATED = "tabulated";
	private final String FASTA = "fasta";

	protected final List availableCommands = Collections.unmodifiableList(Arrays.asList(new String[] { QSEQUENCE, QWHERE, QLIMIT, FASTA }));

	// variables for subquery
	private int nestedLevel = 0;
	private final int MAXNESTING = 1; // change this to allow deeper nesting of queries inside queries

	private List qualifiers = Arrays.asList(new String[] { "=", "!=", "<", ">", "<=", ">=", "exclusive", "excluded", "in" });
	private Logger logger = Logger.getLogger(MartShellLib.class.getName());
}
