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
package org.ensembl.mart.lib;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UnprocessedFilterHandler implementing object designed to process requests for Genes fitting
 * defined Gene Ontology Terms.
 * If an evidence code Filter is added, its field must be of the following format:
 *  go_evidence_code:code (eg, evidence_code:IEA)
 * 
 * All  GO related Filters supplied in the List are resolved into a single IDListFilter of transcript ids 
 * added to the query.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class GOFilterHandler implements UnprocessedFilterHandler {

	private Logger logger = Logger.getLogger(GOFilterHandler.class.getName());
	private final String EVIDENCE = "evidence_code";
	private final String GOSTART = "GO:";
	private final String GOKEY = "transcript_id_key";
	private final String GOFILTERFIELD = "transcript_stable_id";
	private final String EVIDENCECODEFIELD = "evidence_code";
	private final String GODISPLAYIDFIELD = "display_id";
	private final String GODESCRIPTIONFIELD = "description";

	/* (non-Javadoc)
	 * @see org.ensembl.mart.lib.UnprocessedFilterHandler#ModifyQuery(org.ensembl.mart.lib.Engine, java.util.List, org.ensembl.mart.lib.Query)
	 */
	public Query ModifyQuery(Engine engine, List filters, Query query)
		throws InvalidQueryException {

		Connection conn = null;

		try {
			conn = query.getDataSource().getConnection();

			Query newQuery = new Query(query);

			//must get dataset from the first starBase
			String dataset = null;
			String dset= null;
			
			String[] mainTables = newQuery.getStarBases();

			for (int i = 0; i < mainTables.length; i++) {
				if (mainTables[i].matches(".*gene__main"))
					dataset = mainTables[i];
			}

			if (dataset == null) {
				if (logger.isLoggable(Level.WARNING))
					logger.warning(
						"Could not determine dataset for query, perhaps it is a snp query "
							+ newQuery);
				throw new InvalidQueryException("Could not determine dataset for query, perhaps it is a snp query ");
			}
            
            //StringTokenizer datasetTokens = new StringTokenizer(dataset,"_");
            //dset = datasetTokens.nextToken();
			String[] sbtokens = dataset.split("__");
		    dset = sbtokens[0];
                
			List goTables = new ArrayList();
			StringBuffer selectBuf = new StringBuffer("select ");
			StringBuffer fromBuf = new StringBuffer(" from ");
			StringBuffer whereBuf = new StringBuffer(" where ");
			boolean hasEvidenceCode = false;
			// set to true if an evidence code Filter is encountered
			String evidence_code_condition = null;
			String evidence_code_value = null;

			int filterInt = 0;
			for (Iterator iter = filters.iterator(); iter.hasNext();) {
				Filter element = (Filter) iter.next();

				newQuery.removeFilter(element);

				String field = element.getField();

				logger.info("FIELD IS " + field + "\n");

				String condition = element.getCondition();

				if (field.startsWith(EVIDENCE)) {
					logger.info("ITS AN EVIDENCE CODE\n");

					if (hasEvidenceCode)
						throw new InvalidQueryException("Only one evidence Code Filter is allowed in a query\n");
					hasEvidenceCode = true;

					if (condition.equals(BooleanFilter.isNULL))
						evidence_code_condition = " != ";
					else
						evidence_code_condition = " = ";

					evidence_code_value = field.split("\\:")[1];
				} else {
					String goTable = dset + "_" + element.getTableConstraint();
					String value = element.getValue();

					if (!goTables.contains(goTable)) {
						goTables.add(goTable);

						if (filterInt > 0)
							whereBuf.append(" and ");
						else
							selectBuf.append(goTable).append(".").append(GOFILTERFIELD);

						String newField =
							(value.startsWith(GOSTART))
								? GODISPLAYIDFIELD
								: GODESCRIPTIONFIELD;
						whereBuf
							.append(goTable)
							.append(".")
							.append(newField)
							.append(" ")
							.append(condition)
							.append(" '")
							.append(value)
							.append("'");
					}

					filterInt++; //only increment for non evidence_code filters
				}
			}

			//build from, and joins between go tables, if there are more than one
			if (goTables.size() < 1)
				throw new InvalidQueryException("GO Filters must include one or more non evidence code filters\n");
			else if (goTables.size() > 1) {
				int tableInt = 0;
				for (Iterator iter = goTables.iterator(); iter.hasNext();) {
					String table = (String) iter.next();

					//add evidence_code, if present
					if (hasEvidenceCode)
						whereBuf
							.append(" and ")
							.append(table)
							.append(".")
							.append(EVIDENCECODEFIELD)
							.append(" ")
							.append(evidence_code_condition)
							.append(" '")
							.append(evidence_code_value)
							.append("'");

					if (tableInt < 1) {
						fromBuf.append(table);

						//build joins between the first table and each subsequent table in the list
						for (Iterator iterator =
							goTables
								.subList(goTables.indexOf(table) + 1, goTables.size())
								.iterator();
							iterator.hasNext();
							) {
							String joinTable = (String) iterator.next();

							whereBuf
								.append(" and ")
								.append(table)
								.append(".")
								.append(GOKEY)
								.append(" = ")
								.append(joinTable)
								.append(".")
								.append(GOKEY);
						}
					} else {
						fromBuf.append(", ").append(table);
					}
					tableInt++;
				}
			} else {
				String table = (String) goTables.get(0);
				fromBuf.append(table);

				if (hasEvidenceCode)
					whereBuf
						.append(" and ")
						.append(table)
						.append(".")
						.append(EVIDENCECODEFIELD)
						.append(" ")
						.append(evidence_code_condition)
						.append(" '")
						.append(evidence_code_value)
						.append("'");
			}

			String sql = selectBuf.append(fromBuf).append(whereBuf).toString();

			if (logger.isLoggable(Level.INFO))
				logger.info("GO SQL " + sql + "\n");

			List tranIds = new ArrayList();

			PreparedStatement tps = conn.prepareStatement(sql);
			ResultSet trs = tps.executeQuery();

			while (trs.next())
				tranIds.add(trs.getString(1));

			tps.close();
			trs.close();

			if ((tranIds.size() < 1) && logger.isLoggable(Level.INFO))
				logger.info("Recieved zero transcript ids using GO SQL " + sql + "\n");

			String[] tids = new String[tranIds.size()];
			tranIds.toArray(tids);

			newQuery.addFilter(new IDListFilter("transcript_stable_id", "main", "transcript_id_key", tids));

			return newQuery;

		} catch (SQLException e) {
			throw new InvalidQueryException(
				"Recieved SQL Exception processing request for GO Filters "
					+ e.getMessage(),
				e);
		} finally {
			DetailedDataSource.close(conn);
		}
	}

}
