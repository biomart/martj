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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Caches Table arrays and uses Queries as the keys.
 *
 */
public class TableCache {
  
  private Logger logger = Logger.getLogger(TableCache.class.getName());

	public static TableCache instance = new TableCache();

	private TableCache() {
	}

	/** @return hash key based on star bases 
	 * and primary keys in query, order sensitive. */
	public final Integer queryHashcode(Query query) {
		int total = 17;
		for (int i = 0; i < query.getStarBases().length; i++)
			total = total * 37 + query.getStarBases()[i].hashCode();
		for (int i = 0; i < query.getPrimaryKeys().length; i++)
			total = total * 37 + query.getPrimaryKeys()[i].hashCode();

		return new Integer(total);
	}

	public final void add(Query query, Table[] mappersValue) {
		mapperCache.put(queryHashcode(query), mappersValue);
	}

	public final Table[] get(Query query) throws SQLException {
		Object tmp = mapperCache.get(queryHashcode(query));
		if (tmp != null)
			return (Table[]) tmp;
		else
			return loadFromDabase(query);
	}

	public final void clear() {
		mapperCache.clear();
	}

	public Table[] loadFromDabase(Query query)
		throws SQLException {

		ArrayList tablesTmp = new ArrayList();

		// load all the tables that begin with one of the star names
    DataSource ds = query.getDataSource();
    if ( ds==null ) throw new RuntimeException("query.dataset is null");
    Connection conn = null;
    try {
    
    conn = ds.getConnection();
    String catalog = conn.getCatalog();
		ResultSet rs = conn.getMetaData().getTables( catalog, null, null, null);
		while (rs.next()) {
      // 3rd column is the table name. See MetaData documentation for more info.
			String tableName = rs.getString(3);

			for (int i = 0; i < query.getStarBases().length; i++) {
				if (tableName.toLowerCase().startsWith(query.getStarBases()[i])) {
					tablesTmp.add(new Table(tableName, columns(tableName, conn), ""));
				}
			}
    
		} 
    } finally {
      DatabaseUtil.close( conn );
    }
    

		Table[] tables = new Table[tablesTmp.size()];
    
		tablesTmp.toArray(tables);
		mapperCache.put(FieldMapperCache.instance.queryHashcode(query), tables);

		return tables;
	}

	private String[] columns(String table, Connection conn) throws SQLException {
		ArrayList columnsTmp = new ArrayList();

		ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, table, null);
		while (rs.next()) {
			//ColumnName is index 4
			columnsTmp.add(rs.getString(4));
		}

		String[] columns = new String[columnsTmp.size()];
		columnsTmp.toArray(columns);
		return columns;
	}

	private final Map mapperCache = new HashMap();

}
