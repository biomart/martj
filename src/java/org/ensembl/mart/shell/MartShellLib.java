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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.BasicFilter;
import org.ensembl.mart.lib.DomainSpecificFilter;
import org.ensembl.mart.lib.Engine;
import org.ensembl.mart.lib.FieldAttribute;
import org.ensembl.mart.lib.Filter;
import org.ensembl.mart.lib.FormatSpec;
import org.ensembl.mart.lib.IDListFilter;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.NullableFilter;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.FilterPage;
import org.ensembl.mart.lib.config.FilterSetDescription;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.UIAttributeDescription;
import org.ensembl.mart.lib.config.UIDSFilterDescription;
import org.ensembl.mart.lib.config.UIFilterDescription;

/**
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartShellLib {

  public MartShellLib(Engine engine, MartConfiguration martconf) {
  	this.engine = engine;
  	this.martconf = martconf;
  }
  
	public void parseQuery(String newquery) throws IOException, InvalidQueryException {
		boolean start = true;
		boolean selectClause = false;
		boolean sequenceClause = false;
		boolean fromClause = false;
		boolean whereClause = false;
		boolean limitClause = false;
		boolean intoClause = false;
		int listLevel = 0; // level of subquery/list

		StringBuffer attString = new StringBuffer();
		StringBuffer sequenceString = new StringBuffer();
		String dataset = null;
		StringBuffer whereString = new StringBuffer();
		String outformat = null;
		int limit = 0;

		StringTokenizer cTokens = new StringTokenizer(newquery, " ");

		if (cTokens.countTokens() < 2)
			throw new InvalidQueryException("\nInvalid Query Recieved " + newquery + "\n");

		while (cTokens.hasMoreTokens()) {
			String thisToken = cTokens.nextToken();
			if (start) {
				if (!(thisToken.equalsIgnoreCase(QSTART)))
					throw new InvalidQueryException("Invalid Query Recieved, should begin with select: " + newquery + "\n");
				else {
					start = false;
					selectClause = true;
				}
			} else if (selectClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement in the middle of a select statement: " + newquery + "\n");
				if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement before from statement: " + newquery + "\n");
				if (thisToken.equalsIgnoreCase(QINTO))
					throw new InvalidQueryException("Invalid Query Recieved, into statement before from statement: " + newquery + "\n");
				if (thisToken.equalsIgnoreCase(QLIMIT))
					throw new InvalidQueryException("Invalid Query Recieved, limit statement before from statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE)) {
					selectClause = false;
					sequenceClause = true;
				} else if (thisToken.equalsIgnoreCase(QFROM)) {
					selectClause = false;
					fromClause = true;
				} else
					attString.append(thisToken);
			} else if (sequenceClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement in the middle of an into statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement before from statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QINTO))
					throw new InvalidQueryException("Invalid Query Recieved, into statement before from statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT))
					throw new InvalidQueryException("Invalid Query Recieved, limit statement before from statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM)) {
					sequenceClause = false;
					fromClause = true;
				} else
					sequenceString.append(thisToken);
			} else if (fromClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after from statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after from statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE)) {
					fromClause = false;
					whereClause = true;
				} else if (thisToken.equalsIgnoreCase(QINTO)) {
					fromClause = false;
					intoClause = true;
				} else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					fromClause = false;
					limitClause = true;
				} else {
					if (dataset != null)
						throw new InvalidQueryException("Invalid Query Recieved, dataset already set, attempted to set again: " + newquery + "\n");
					else
						dataset = thisToken;
				}
			} else if (whereClause) {
				if (listLevel < 1 && thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after where statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(LSTART) || thisToken.startsWith(LSTART)) {
					listLevel++;

					if (thisToken.endsWith(LEND) || thisToken.endsWith(LEND + ",") || thisToken.endsWith(LEND + LINEEND)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == LENDCHAR)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(LINEEND) || thisToken.endsWith(LINEEND)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where statement: " + newquery + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (listLevel > 0) {
					if (thisToken.equalsIgnoreCase(LEND)) {
						listLevel--;
					} else if (thisToken.endsWith(LEND) || thisToken.endsWith(LEND + ",") || thisToken.endsWith(LEND + LINEEND)) {
						for (int i = 0, n = thisToken.length(); i < n; i++) {
							if (thisToken.charAt(i) == LENDCHAR)
								listLevel--;
						}
						whereString.append(" ").append(thisToken);
					} else if (thisToken.equalsIgnoreCase(LINEEND) || thisToken.endsWith(LINEEND)) {
						System.out.println("Token = " + thisToken);
						throw new InvalidQueryException("Recieved Invalid Query, failure to close list clause in where statement: " + newquery + "\n");
					} else
						whereString.append(" ").append(thisToken);
				} else if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after where statement, not in subquery: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM))
					throw new InvalidQueryException("Invalid Query Recieved, from statement after where statement, not in subquery: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement after where statement, not in subquery: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QINTO)) {
					whereClause = false;
					intoClause = true;
				} else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					whereClause = false;
					limitClause = true;
				} else
					whereString.append(" ").append(thisToken);
			} else if (intoClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after into statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after into statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM))
					throw new InvalidQueryException("Invalid Query Recieved, from statement into where statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement into where statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QLIMIT)) {
					intoClause = false;
					limitClause = true;
				} else {
					if (thisToken.endsWith(LINEEND))
						thisToken = thisToken.substring(0, thisToken.length() - 1);
					outformat = thisToken;
				}
			} else if (limitClause) {
				if (thisToken.equalsIgnoreCase(QSTART))
					throw new InvalidQueryException("Invalid Query Recieved, select statement after limit statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QSEQUENCE))
					throw new InvalidQueryException("Invalid Query Recieved, with statement after limit statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QFROM))
					throw new InvalidQueryException("Invalid Query Recieved, from statement into limit statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QWHERE))
					throw new InvalidQueryException("Invalid Query Recieved, where statement into limit statement: " + newquery + "\n");
				else if (thisToken.equalsIgnoreCase(QINTO))
					throw new InvalidQueryException("Invalid Query Recieved, into statement into limit statement: " + newquery + "\n");
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

		if (dataset == null)
			throw new InvalidQueryException("Invalid Query Recieved, did not set dataset: " + newquery + "\n");

		if (attString.length() == 0 && sequenceString.length() == 0)
			throw new InvalidQueryException("Invalid Query Recieved, no attributes or sequence request found: " + newquery + "\n");

		if (dataset.endsWith(LINEEND))
			dataset = dataset.substring(0, dataset.length() - 1);

		if (!martconf.containsDataset(dataset))
			throw new InvalidQueryException("Dataset " + dataset + " is not found in this mart\n");

		Dataset dset = martconf.getDatasetByName(dataset);
		Query query = new Query();
		FilterPage currentFpage = null;
		AttributePage currentApage = null;

		query.setStarBases(dset.getStarBases());
		query.setPrimaryKeys(dset.getPrimaryKeys());

		if (sequenceString.length() > 0) {
			String seqrequest = sequenceString.toString().trim();

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
			currentApage = dset.getAttributePageByName("sequences");
			query.setSequenceDescription(new SequenceDescription(typecode, left, right));
		}

		//parse attributes, if present
		if (attString.length() > 1) {
			List atts = new ArrayList();
			StringTokenizer attTokens = new StringTokenizer(attString.toString(), ",");

			while (attTokens.hasMoreTokens()) {
				String attname = attTokens.nextToken().trim(); // remove leading and trailing whitespace
				if (!dset.containsUIAttributeDescription(attname))
					throw new InvalidQueryException("Attribute " + attname + " is not found in this mart for dataset " + dataset + "\n");

				if (currentApage == null) {
					currentApage = dset.getPageForUIAttributeDescription(attname);
					atts.add(dset.getUIAttributeDescriptionByName(attname));
				} else {
					if (!currentApage.containsUIAttributeDescription(attname)) {
						if (currentApage.getInternalName().equals("sequences"))
							throw new InvalidQueryException("Cannot request attribute " + attname + " with a sequence request\n");

						currentApage = dset.getPageForUIAttributeDescription(attname);

						for (int i = 0, n = atts.size(); i < n; i++) {
							UIAttributeDescription element = (UIAttributeDescription) atts.get(i);

							if (!currentApage.containsUIAttributeDescription(element.getInternalName()))
								throw new InvalidQueryException(
									"Cannot request attributes from different Attribute Pages " + attname + " in " + currentApage + " intName is not\n");
						}
					}
					atts.add(dset.getUIAttributeDescriptionByName(attname));
				}
			}

			for (int i = 0, n = atts.size(); i < n; i++) {
				UIAttributeDescription attd = (UIAttributeDescription) atts.get(i);
				Attribute attr = new FieldAttribute(attd.getFieldName(), attd.getTableConstraint());
				query.addAttribute(attr);
			}
		}

		//parse filters, if present
		List filts = new ArrayList();

		if (whereString.length() > 0) {
			if (whereString.toString().endsWith(LINEEND))
				whereString.deleteCharAt(whereString.length() - 1);

			List filtNames = new ArrayList();
			String filterName = null;
			String cond = null;
			String val = null;
			String filterSetName = null;
			FilterSetDescription fset = null;

			start = true;
			listLevel = 0;

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
						throw new InvalidQueryException("Filter " + filterName + " not supported by mart dataset " + dataset + "\n");
					else {
						if (currentFpage == null)
							currentFpage = dset.getPageForUIFilterDescription(filterName);
						else {
							if (!currentFpage.containsUIFilterDescription(filterName)) {
								currentFpage = dset.getPageForUIFilterDescription(filterName);

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
								thisCondition = "=";
							else if (cond.equalsIgnoreCase("excluded"))
								thisCondition = "!=";
							else
								throw new InvalidQueryException("Invalid Query Recieved, Filter Name, Condition with no value: " + filterName + " " + thisToken + "\n");

							thisFilter = new BasicFilter(thisFieldName, thisTableConstraint, thisCondition, "1");
						} else
							throw new InvalidQueryException("Recieved invalid exclusive/excluded query: " + newquery + "\n");

						query.addFilter(thisFilter);
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

						if (thisToken.startsWith("file:")) {
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

							Filter thisFilter = new IDListFilter(thisFieldName, new File(thisToken));
							((IDListFilter) thisFilter).setTableConstraint(thisTableConstraint);
							query.addFilter(thisFilter);
							start = true;
							value = false;
						} else if (thisToken.equals(QSTART)) {
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
								Filter thisFilter = new IDListFilter(thisFieldName, ids);
								((IDListFilter) thisFilter).setTableConstraint(thisTableConstraint);
								query.addFilter(thisFilter);
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
								if (thisToken.charAt(i) == LENDCHAR)
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
									throw new InvalidQueryException(
										"Cannot query this filter with a list input using in qualifier: " + filterName + "in command: " + newquery + "\n");

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
								Filter thisFilter = getIDFilterForSubQuery(thisFieldName, thisTableConstraint, subquery.toString());
								query.addFilter(thisFilter);
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
								throw new InvalidQueryException("Cannot query this filter with a list input using in: " + filterName + "in command: " + newquery + "\n");

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

							query.addFilter(new BasicFilter(thisFieldName, thisTableConstraint, cond, thisToken));
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

							DomainSpecificFilter thisFilter = new DomainSpecificFilter(fds.getObjectCode(), thisHandlerParam);
							query.addDomainSpecificFilter(thisFilter);
							start = true;
							value = false;
						}
					}
				}
				//dont need else
			}
		}

		OutputStream thisOs = null;
		String thisFormat = null;
		String thisSeparator = null;
		String thisFile = null;

		if (outformat != null) {
			StringTokenizer fTokens = new StringTokenizer(outformat, ",");
			while (fTokens.hasMoreTokens()) {
				StringTokenizer tok = new StringTokenizer(fTokens.nextToken(), "=");
				if (tok.countTokens() < 2)
					throw new InvalidQueryException(
						"Recieved invalid into request: "
							+ outformat
							+ "\nmust be of format: x=y(,x=y)* where x  can be one of : "
							+ FILE
							+ "(note, use '-' for stdout, or specify a valid URL), "
							+ FORMAT
							+ "(note, use 'tab' for tab separated, 'space' for space separated, and 'comma' for comma separated), "
							+ SEPARATOR
							+ "\n");

				String key = tok.nextToken();
				String value = tok.nextToken();
				if (key.equals(FILE))
					thisFile = value;
				else if (key.equals(FORMAT))
					thisFormat = value;
				else if (key.equals(SEPARATOR)) {
					if (value.equals("tab"))
						thisSeparator = "\t";
					else if (value.equals("space"))
						thisSeparator = " ";
					else if (value.equals("comma"))
						thisSeparator = ",";
					else
						thisSeparator = value;
				} else
					throw new InvalidQueryException(
						"Recieved invalid into request: "
							+ outformat
							+ "\nmust be of format: x=y(,x=y)* where x  can be one of : "
							+ FILE
							+ "(note, use '-' for stdout, specify a valid URL), "
							+ FORMAT
							+ "(note, use 'tab' for tab separated, 'space' for space separated, and 'comma' for comma separated), "
							+ SEPARATOR
							+ "\n");
			}
		}

		if (thisFormat == null) {
			if (outputFormat != null)
				thisFormat = outputFormat;
			else
				thisFormat = DEFOUTPUTFORMAT;
		}

		if (thisFile == null) {
			if (subqueryOutput != null) {
				thisFile = "subquery";
				thisOs = subqueryOutput;
			} else if (os != null) {
			  thisFile = os.getClass().getName();
			  thisOs = os;
			} else {
				thisFile = "stdout";
				thisOs = System.out;
			}
		} else if (thisFile.equals("-")) {
			thisFile = "stdout";
			thisOs = System.out;
		} else
			thisOs = new FileOutputStream(thisFile);

		if (thisSeparator == null) {
			if (outputSeparator != null)
				thisSeparator = outputSeparator;
			else
				thisSeparator = DEFOUTPUTSEPARATOR;
		}

		FormatSpec formatspec = new FormatSpec();
		if (TABULATED.equalsIgnoreCase(thisFormat))
			formatspec.setFormat(FormatSpec.TABULATED);
		else if (FASTA.equalsIgnoreCase(thisFormat))
			formatspec.setFormat(FormatSpec.FASTA);
		else
			throw new InvalidQueryException("Invalid Format Request Recieved, must be either tabulated or fasta\n" + outputFormat + "\n");

		formatspec.setSeparator(thisSeparator);

		logger.info("Processed request for Query: \n" + query + "\n");
		logger.info("with format " + formatspec + "\n");
		logger.info("into file " + thisFile);
		logger.info("limit " + limit);

		engine.execute(query, formatspec, thisOs, limit);
	}

	public void setAlternateMartConfiguration(String confFile) {
		altConfigurationFile = confFile;
	}
	
	
	public void setDBHost(String dbhost) {
		this.martHost = dbhost;
	}


	public void setDBPort(String dbport) {
		this.martPort = dbport;
	}


	public void setDBUser(String dbuser) {
		this.martUser = dbuser;
	}


	public void setDBPass(String dbpass) {
		martPass = dbpass;
	}

	public void setDBDatabase(String db) {
		martDatabase = db;
	}
	
	public void setOutputFormat(String format) {
		this.outputFormat = format;
	}
	
	public void setOutputSeparator(String separator) {
		this.outputSeparator = separator;
	}
	
	public void setOutputStream(OutputStream os) {
		this.os = os;
	}
	
	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	public void setConfiguration(MartConfiguration martconf) {
		this.martconf = martconf;
	}
	
	private Filter getIDFilterForSubQuery(String fieldName, String tableConstraint, String nestedQuery) throws InvalidQueryException {
		nestedQuery = nestedQuery.trim();

		nestedLevel++;

		logger.info("Recieved nested query at nestedLevel " + nestedLevel + "\n");

		if (nestedLevel > MAXNESTING)
			throw new InvalidQueryException("Only " + MAXNESTING + " levels of nested Query are allowed\n");

		//validate, then call parseQuery on the subcommand
		String[] tokens = nestedQuery.split("\\s");
		if (!tokens[0].trim().equals(QSTART))
			throw new InvalidQueryException("Invalid Nested Query Recieved: no select statement " + "recieved " + tokens[0].trim() + " in " + nestedQuery + "\n");

		for (int i = 1, n = tokens.length; i < n; i++) {
			String tok = tokens[i];
			if (tok.equals("with"))
				throw new InvalidQueryException("Invalid Nested Query Recieved: with statement not allowed " + nestedQuery + "\n");
			else if (tok.equals("into"))
				throw new InvalidQueryException("Invalid Nested Query Recieved: into statement not allowed " + nestedQuery + "\n");
			//else not needed
		}

		logger.info("Recieved request for Nested Query\n:" + nestedQuery + "\n");

		subqueryOutput = new ByteArrayOutputStream();

		FormatSpec thisFormatspec = new FormatSpec(FormatSpec.TABULATED);

		thisFormatspec.setSeparator(",");
		String results = null;

		try {
			parseQuery(nestedQuery);
			results = subqueryOutput.toString();
			subqueryOutput.close();
			subqueryOutput = null;
		} catch (Exception e) {
			try {
				subqueryOutput.close();
			} catch (Exception ex) {
				subqueryOutput = null;
				throw new InvalidQueryException("Could not execute Nested Query: " + ex.getMessage());
			}
			subqueryOutput = null;
			throw new InvalidQueryException("Could not execute Nested Query: " + e.getMessage());
		}

		StringTokenizer lines = new StringTokenizer(results, "\n");
		List idlist = new ArrayList();

		while (lines.hasMoreTokens()) {
			String id = lines.nextToken();

			if (id.indexOf(".") >= 0)
				id = id.substring(0, id.lastIndexOf("."));
			if (!idlist.contains(id))
				idlist.add(id);
		}

		String[] ids = new String[idlist.size()];
		idlist.toArray(ids);
		Filter f = new IDListFilter(fieldName, ids);
		((IDListFilter) f).setTableConstraint(tableConstraint);

		nestedLevel--;
		return f;
	}

	//MartShellLib instance variables
	private Engine engine;
	private MartConfiguration martconf;
	private String martHost = null;
	private String martPort = null;
	private String martUser = null;
	private String martPass = null;
	private String martDatabase = null;
	private String altConfigurationFile = null;
	private OutputStream os;
	
	private final String DEFOUTPUTFORMAT = "tabulated"; // default to tabulated output
	private String outputFormat = null;
	private final String DEFOUTPUTSEPARATOR = "\t"; // default to tab separated
	private final String DSHELPFILE = "data/dshelp.properties"; // contains help for domain specific aspects
	private String outputSeparator = null;

	private boolean continueQuery = false;
	private StringBuffer conline = new StringBuffer();

	// query instructions
	private final String QSTART = "select";
	private final String QSEQUENCE = "sequence";
	private final String QFROM = "from";
	private final String QWHERE = "where";
	private final String QLIMIT = "limit";
	private final String QINTO = "into";
	private final String LSTART = "(";
	private final String LEND = ")";
	private final char LENDCHAR = LEND.charAt(0);
	private final String LINEEND = ";";
	private final String ID = "id";
	private final String SEQDELIMITER = "+";
	private final String EXCLUSIVE = "exclusive";
	private final String TABULATED = "tabulated";
	private final String FASTA = "fasta";

	protected final List availableCommands =
		Collections.unmodifiableList(
			Arrays.asList(
				new String[] {
					QSTART,
					QSEQUENCE,
					QFROM,
					QWHERE,
					QLIMIT,
					QINTO,
					FASTA }));
					
	// strings used to show/set output format settings
	private final String FILE = "file";
	private final String FORMAT = "format";
	private final String SEPARATOR = "separator";

	// strings used to show/set mart connection settings
	private final String MYSQLHOST = "mysqlhost";
	private final String MYSQLUSER = "mysqluser";
	private final String MYSQLPASS = "mysqlpass";
	private final String MYSQLPORT = "mysqlport";
	private final String MYSQLBASE = "mysqldbase";
	private final String ALTCONFFILE = "alternateConfigurationFile";

	// variables for subquery
	private ByteArrayOutputStream subqueryOutput = null;
	private int nestedLevel = 0;
	private final int MAXNESTING = 1; // change this to allow deeper nesting of queries inside queries

	private final String DASHES = "--------------------------------------------------------------------------------"; // describe output separator

	private List qualifiers = Arrays.asList(new String[] { "=", "!=", "<", ">", "<=", ">=", "exclusive", "excluded", "in" });
	private Logger logger = Logger.getLogger(MartShellLib.class.getName());
}
