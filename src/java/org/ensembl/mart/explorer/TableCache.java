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

package org.ensembl.mart.explorer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches Table arrays and uses Queries as the keys.
 *
 */
public class TableCache {

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

	public final Table[] get(Query query, Connection conn) throws SQLException {
		Object tmp = mapperCache.get(queryHashcode(query));
		if (tmp != null)
			return (Table[]) tmp;
		else
			return loadFromDabase(query, conn);
	}

	public final void clear() {
		mapperCache.clear();
	}

	public Table[] loadFromDabase(Query query, Connection conn)
		throws SQLException {

		ArrayList tablesTmp = new ArrayList();

		// load all the tables that begin with one of the star names
		ResultSet rs = conn.createStatement().executeQuery("show tables");
		while (rs.next()) {
			String tableName = rs.getString(1);
			for (int i = 0; i < query.getStarBases().length; i++) {
				if (tableName.startsWith(query.getStarBases()[i])) {
					tablesTmp.add(new Table(tableName, columns(tableName, conn), ""));
				}
			}
		}

		Table[] tables = new Table[tablesTmp.size()];
		tablesTmp.toArray(tables);
		mapperCache.put(FieldMapperCache.instance.queryHashcode(query), tables);

		return tables;
	}

	private String[] columns(String table, Connection conn) throws SQLException {
		ArrayList columnsTmp = new ArrayList();

		ResultSet rs = conn.createStatement().executeQuery("describe " + table);
		while (rs.next()) {
			columnsTmp.add(rs.getString(1));
		}

		String[] columns = new String[columnsTmp.size()];
		columnsTmp.toArray(columns);
		return columns;
	}

	private final Map mapperCache = new HashMap();

}
