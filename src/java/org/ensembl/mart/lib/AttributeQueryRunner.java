package org.ensembl.mart.lib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Implimentation of the QueryRunner for executing a Query and 
 * generating Tabulated output.
 * Tabulated output is separated by a field separator specified by 
 * a FormatSpec object.  Any Query can generate tabulated output.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see Query
 * @see FormatSpec
 */
public final class AttributeQueryRunner implements QueryRunner {

	/**
	 * Constructs a TabulatedQueryRunner object to execute a Query
	 * and print tabulated output specified by the given FormatSpec
	 * 
	 * @param query - a Query Object
	 * @param format - a FormatSpec object
	 */
	public AttributeQueryRunner(Query query, FormatSpec format, Connection conn, OutputStream os) {
		this.query = query;
		this.format = format;
		this.conn = conn;
		this.osr = new OutputStreamWriter(os);
	}

	public void execute(int limit) throws SequenceException, InvalidQueryException {
		boolean moreRows = true;
		// batching output system stops when this is false
		int batchStart = 0; // start at 0 during batching

		String sql = null;
		try {
			CompiledSQLQuery csql = new CompiledSQLQuery(conn, query);
			String sqlbase = csql.toSQL();

			while (moreRows) {
        sql = sqlbase;
        
				if (limit > 0) {
					sql = sql + " limit " + limit;
					moreRows = false;
					// for client determined limits, assume they are sane, and run the entire query
				} else {
					sql += " limit " + batchStart + ", " + batchLength;
					batchStart += batchLength;
				}

				logger.debug("QUERY : " + query);
				logger.debug("SQL : " + sql);

				PreparedStatement ps = conn.prepareStatement(sql);
				int p = 1;
				for (int i = 0, n = query.getFilters().length; i < n; ++i) {
					Filter f = query.getFilters()[i];
					String value = f.getValue();
					if (value != null) {
						if (logger.isDebugEnabled())
							logger.debug("SQL (prepared statement value) : " + p + " = " + value);
						ps.setString(p++, value);
					}
				}

				ResultSet rs = ps.executeQuery();

        int rows = 0;
				while (rs.next()) {
					rows++;
					
					for (int i = 1, nColumns = rs.getMetaData().getColumnCount(); i <= nColumns; ++i) {
						if (i > 1)
							osr.write(format.getSeparator());
						String v = rs.getString(i);
						if (v != null)
							osr.write(v);
						logger.debug(v);
					}
					osr.write("\n");
					osr.flush();
				}
				if (rows < batchLength)
					moreRows = false;
				// on the odd chance that the last result set is equal in size to the batchLength, it will need to make an extra attempt.
			}
		} catch (IOException e) {
			logger.warn("Couldnt write to OutputStream\n" + e.getMessage());
			throw new InvalidQueryException(e);
		} catch (SQLException e) {
			logger.warn(e.getMessage());
			throw new InvalidQueryException(e);
		}
	}

	private int batchLength = 200000;
	private Logger logger = Logger.getLogger(AttributeQueryRunner.class.getName());
	private Query query = null;
	private FormatSpec format = null;
	private OutputStreamWriter osr;
	private Connection conn;
}
