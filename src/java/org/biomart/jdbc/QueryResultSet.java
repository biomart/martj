/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.biomart.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.biomart.jdbc.resources.Resources;

/**
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by 
 * 			$Author$
 * @since 0.6
 */
public class QueryResultSet implements ResultSet {

	// What fetch size do we have? Defaults to 100.
	private int fetchSize = 100;

	// What statement are we running within?
	private QueryStatement statement;

	// What query are we running?
	private Query query;

	// Any warnings?
	private SQLWarning warnings;

	// Are we closed?
	private boolean closed = false;

	// What row num are we on? Note that the first row is row 1.
	private int currentRowNum = 0;

	// What batch row num are we on? Note that the first row is row 0.
	private int currentBatchRowNum = 0;

	// What was the last rownum?
	private int lastRowNum = 0;

	// Current batch of results. Each entry in the list is a
	// simple array of data columns.
	private List currentBatch = new ArrayList();

	// Records if last object was null or not.
	private boolean lastWasNull = false;

	/**
	 * Create object that compiles results for the given query.
	 * 
	 * @param stmt
	 *            the statement we are running within.
	 * @param query
	 *            the query to execute.
	 */
	QueryResultSet(final QueryStatement stmt, final Query query) {
		this.statement = stmt;
		this.query = query;
	}

	public boolean absolute(int row) throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public void afterLast() throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public void beforeFirst() throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public void cancelRowUpdates() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void clearWarnings() throws SQLException {
		this.warnings = null;
	}

	public void close() throws SQLException {
		try {
			if (!this.query.isClosed())
				this.query.close();
		} catch (SQLException e) {
			// We don't care. Ignore it.
		}
		this.closed = true;
		this.statement.resultSetClosed(this);
	}

	public void deleteRow() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public int findColumn(String columnName) throws SQLException {
		final int index = this.query.getColumnNames().indexOf(columnName);
		if (index < 0)
			throw new SQLException(Resources.get("unknownColName", columnName));
		return index + 1;
	}

	public boolean first() throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public Array getArray(int i) throws SQLException {
		return this.objectToArray(this.getObject(i));
	}

	public Array getArray(String colName) throws SQLException {
		return this.objectToArray(this.getObject(colName));
	}

	private Array objectToArray(Object obj) throws SQLException {
		try {
			return (Array) obj;
		} catch (ClassCastException e) {
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
		}
	}

	public InputStream getAsciiStream(int i) throws SQLException {
		return this.objectToAsciiStream(this.getObject(i));
	}

	public InputStream getAsciiStream(String colName) throws SQLException {
		return this.objectToAsciiStream(this.getObject(colName));
	}

	private InputStream objectToAsciiStream(Object obj) throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof InputStream)
			return (InputStream) obj;
		else if (obj instanceof Clob)
			return ((Clob) obj).getAsciiStream();
		else if (obj instanceof String)
			return new ByteArrayInputStream(((String) obj).getBytes());
		else
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
	}

	public BigDecimal getBigDecimal(int i) throws SQLException {
		return this.objectToBigDecimal(this.getObject(i), 0);
	}

	public BigDecimal getBigDecimal(String colName) throws SQLException {
		return this.objectToBigDecimal(this.getObject(colName), 0);
	}

	public BigDecimal getBigDecimal(int i, int scale) throws SQLException {
		return this.objectToBigDecimal(this.getObject(i), scale);
	}

	public BigDecimal getBigDecimal(String colName, int scale)
			throws SQLException {
		return this.objectToBigDecimal(this.getObject(colName), scale);
	}

	private BigDecimal objectToBigDecimal(Object obj, int scale)
			throws SQLException {
		if (obj == null)
			return new BigDecimal(0);
		else if (obj instanceof BigDecimal) {
			final BigDecimal bd = (BigDecimal) obj;
			bd.setScale(scale);
			return bd;
		} else
			try {
				final BigDecimal bd = new BigDecimal("" + obj);
				bd.setScale(scale);
				return bd;
			} catch (NumberFormatException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public InputStream getBinaryStream(int i) throws SQLException {
		return this.objectToBinaryStream(this.getObject(i));
	}

	public InputStream getBinaryStream(String colName) throws SQLException {
		return this.objectToBinaryStream(this.getObject(colName));
	}

	private InputStream objectToBinaryStream(Object obj) throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof InputStream)
			return (InputStream) obj;
		else if (obj instanceof Blob)
			return ((Blob) obj).getBinaryStream();
		else if (obj instanceof String)
			return new ByteArrayInputStream(((String) obj).getBytes());
		else
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
	}

	public Blob getBlob(int i) throws SQLException {
		return this.objectToBlob(this.getObject(i));
	}

	public Blob getBlob(String colName) throws SQLException {
		return this.objectToBlob(this.getObject(colName));
	}

	private Blob objectToBlob(Object obj) throws SQLException {
		try {
			return (Blob) obj;
		} catch (ClassCastException e) {
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
		}
	}

	public boolean getBoolean(int i) throws SQLException {
		return this.objectToBoolean(this.getObject(i));
	}

	public boolean getBoolean(String colName) throws SQLException {
		return this.objectToBoolean(this.getObject(colName));
	}

	private boolean objectToBoolean(Object obj) throws SQLException {
		if (obj == null)
			return false;
		else if (obj instanceof Boolean)
			return ((Boolean) obj).booleanValue();
		else
			return Boolean.valueOf("" + obj).booleanValue();
	}

	public byte getByte(int i) throws SQLException {
		return this.objectToByte(this.getObject(i));
	}

	public byte getByte(String colName) throws SQLException {
		return this.objectToByte(this.getObject(colName));
	}

	private byte objectToByte(Object obj) throws SQLException {
		if (obj == null)
			return 0;
		else if (obj instanceof Byte)
			return ((Byte) obj).byteValue();
		else
			try {
				return Byte.valueOf("" + obj).byteValue();
			} catch (NumberFormatException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public byte[] getBytes(int i) throws SQLException {
		return this.objectToBytes(this.getObject(i));
	}

	public byte[] getBytes(String colName) throws SQLException {
		return this.objectToBytes(this.getObject(colName));
	}

	private byte[] objectToBytes(Object obj) throws SQLException {
		if (obj == null)
			return new byte[0];
		else if (obj instanceof byte[])
			return (byte[]) obj;
		else
			return ("" + obj).getBytes();
	}

	public Reader getCharacterStream(int i) throws SQLException {
		return this.objectToCharacterStream(this.getObject(i));
	}

	public Reader getCharacterStream(String colName) throws SQLException {
		return this.objectToCharacterStream(this.getObject(colName));
	}

	private Reader objectToCharacterStream(Object obj) throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof Reader)
			return (Reader) obj;
		else if (obj instanceof Clob)
			return ((Clob) obj).getCharacterStream();
		else if (obj instanceof String)
			return new StringReader("" + obj);
		else
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
	}

	public Clob getClob(int i) throws SQLException {
		return this.objectToClob(this.getObject(i));
	}

	public Clob getClob(String colName) throws SQLException {
		return this.objectToClob(this.getObject(colName));
	}

	private Clob objectToClob(Object obj) throws SQLException {
		try {
			return (Clob) obj;
		} catch (ClassCastException e) {
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
		}
	}

	public int getConcurrency() throws SQLException {
		// We don't do transactions.
		return ResultSet.CONCUR_READ_ONLY;
	}

	public String getCursorName() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public Date getDate(int i) throws SQLException {
		return this.objectToDate(this.getObject(i), Calendar.getInstance());
	}

	public Date getDate(String colName) throws SQLException {
		return this.objectToDate(this.getObject(colName), Calendar
				.getInstance());
	}

	public Date getDate(int i, Calendar cal) throws SQLException {
		return this.objectToDate(this.getObject(i), cal);
	}

	public Date getDate(String colName, Calendar cal) throws SQLException {
		return this.objectToDate(this.getObject(colName), cal);
	}

	private Date objectToDate(Object obj, Calendar cal) throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof Date)
			return (Date) obj;
		else
			try {
				return new Date(DateFormat.getInstance().parse("" + obj)
						.getTime());
			} catch (ParseException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public double getDouble(int i) throws SQLException {
		return this.objectToDouble(this.getObject(i));
	}

	public double getDouble(String colName) throws SQLException {
		return this.objectToDouble(this.getObject(colName));
	}

	private double objectToDouble(Object obj) throws SQLException {
		if (obj == null)
			return 0.0;
		else if (obj instanceof Double)
			return ((Double) obj).doubleValue();
		else
			try {
				return Double.parseDouble("" + obj);
			} catch (NumberFormatException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public int getFetchDirection() throws SQLException {
		// We only do forwards.
		return ResultSet.FETCH_FORWARD;
	}

	public int getFetchSize() throws SQLException {
		return this.fetchSize;
	}

	public float getFloat(int i) throws SQLException {
		return this.objectToFloat(this.getObject(i));
	}

	public float getFloat(String colName) throws SQLException {
		return this.objectToFloat(this.getObject(colName));
	}

	private float objectToFloat(Object obj) throws SQLException {
		if (obj == null)
			return 0.0f;
		else if (obj instanceof Float)
			return ((Float) obj).floatValue();
		else
			try {
				return Float.parseFloat("" + obj);
			} catch (NumberFormatException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public int getInt(int i) throws SQLException {
		return this.objectToInt(this.getObject(i));
	}

	public int getInt(String colName) throws SQLException {
		return this.objectToInt(this.getObject(colName));
	}

	private int objectToInt(Object obj) throws SQLException {
		if (obj == null)
			return 0;
		else if (obj instanceof Integer)
			return ((Integer) obj).intValue();
		else
			try {
				return Integer.parseInt("" + obj);
			} catch (NumberFormatException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public long getLong(int i) throws SQLException {
		return this.objectToLong(this.getObject(i));
	}

	public long getLong(String colName) throws SQLException {
		return this.objectToLong(this.getObject(colName));
	}

	private long objectToLong(Object obj) throws SQLException {
		if (obj == null)
			return 0;
		else if (obj instanceof Long)
			return ((Long) obj).longValue();
		else
			try {
				return Long.parseLong("" + obj);
			} catch (NumberFormatException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		this.ensureOpen();
		return this.query.getResultSetMetaData();
	}

	public Object getObject(int columnIndex) throws SQLException {
		return this.getObject(columnIndex, this.statement.getConnection()
				.getTypeMap());
	}

	public Object getObject(String columnName) throws SQLException {
		return this.getObject(columnName, this.statement.getConnection()
				.getTypeMap());
	}

	public Object getObject(int i, Map map) throws SQLException {
		// For now, we ignore the type map.
		this.ensureOpen();
		if (this.isBeforeFirst())
			throw new SQLException(Resources.get("queryNotStarted"));
		else if (this.isAfterLast())
			throw new SQLException(Resources.get("queryExhausted"));
		else if (i < 1 || i > this.query.getColumnNames().size())
			throw new SQLException(Resources.get("colIndexOutRange", "" + i));
		final Object obj = ((Object[]) this.currentBatch
				.get(this.currentBatchRowNum))[i - 1];
		this.lastWasNull = obj == null;
		return obj;
	}

	public Object getObject(String colName, Map map) throws SQLException {
		int index = this.findColumn(colName);
		return this.getObject(index + 1, map);
	}

	public Ref getRef(int i) throws SQLException {
		return this.objectToRef(this.getObject(i));
	}

	public Ref getRef(String colName) throws SQLException {
		return this.objectToRef(this.getObject(colName));
	}

	private Ref objectToRef(Object obj) throws SQLException {
		try {
			return (Ref) obj;
		} catch (ClassCastException e) {
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
		}
	}

	public int getRow() throws SQLException {
		return this.currentRowNum;
	}

	public short getShort(int i) throws SQLException {
		return this.objectToShort(this.getObject(i));
	}

	public short getShort(String colName) throws SQLException {
		return this.objectToShort(this.getObject(colName));
	}

	private short objectToShort(Object obj) throws SQLException {
		if (obj == null)
			return 0;
		else if (obj instanceof Short)
			return ((Short) obj).shortValue();
		else
			try {
				return Short.parseShort("" + obj);
			} catch (NumberFormatException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public Statement getStatement() throws SQLException {
		return this.statement;
	}

	public String getString(int i) throws SQLException {
		return this.objectToString(this.getObject(i));
	}

	public String getString(String colName) throws SQLException {
		return this.objectToString(this.getObject(colName));
	}

	private String objectToString(Object obj) throws SQLException {
		if (obj == null)
			return null;
		else
			return "" + obj;
	}

	public Time getTime(int i) throws SQLException {
		return this.objectToTime(this.getObject(i), Calendar.getInstance());
	}

	public Time getTime(String colName) throws SQLException {
		return this.objectToTime(this.getObject(colName), Calendar
				.getInstance());
	}

	public Time getTime(int i, Calendar cal) throws SQLException {
		return this.objectToTime(this.getObject(i), cal);
	}

	public Time getTime(String colName, Calendar cal) throws SQLException {
		return this.objectToTime(this.getObject(colName), cal);
	}

	private Time objectToTime(Object obj, Calendar cal) throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof Time)
			return (Time) obj;
		else
			try {
				return new Time(DateFormat.getInstance().parse("" + obj)
						.getTime());
			} catch (ParseException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public Timestamp getTimestamp(int i) throws SQLException {
		return this
				.objectToTimestamp(this.getObject(i), Calendar.getInstance());
	}

	public Timestamp getTimestamp(String colName) throws SQLException {
		return this.objectToTimestamp(this.getObject(colName), Calendar
				.getInstance());
	}

	public Timestamp getTimestamp(int i, Calendar cal) throws SQLException {
		return this.objectToTimestamp(this.getObject(i), cal);
	}

	public Timestamp getTimestamp(String colName, Calendar cal)
			throws SQLException {
		return this.objectToTimestamp(this.getObject(colName), cal);
	}

	private Timestamp objectToTimestamp(Object obj, Calendar cal)
			throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof Timestamp)
			return (Timestamp) obj;
		else
			try {
				return new Timestamp(DateFormat.getInstance().parse("" + obj)
						.getTime());
			} catch (ParseException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public int getType() throws SQLException {
		// We only do forwards.
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	public URL getURL(int i) throws SQLException {
		return this.objectToURL(this.getObject(i));
	}

	public URL getURL(String colName) throws SQLException {
		return this.objectToURL(this.getObject(colName));
	}

	private URL objectToURL(Object obj) throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof URL)
			return (URL) obj;
		else
			try {
				return new URL("" + obj);
			} catch (MalformedURLException e) {
				throw new SQLException(Resources.get("noTypeConversion", obj
						.getClass().getName()));
			}
	}

	public InputStream getUnicodeStream(int i) throws SQLException {
		return this.objectToUnicodeStream(this.getObject(i));
	}

	public InputStream getUnicodeStream(String colName) throws SQLException {
		return this.objectToUnicodeStream(this.getObject(colName));
	}

	private InputStream objectToUnicodeStream(Object obj) throws SQLException {
		if (obj == null)
			return null;
		else if (obj instanceof InputStream)
			return (InputStream) obj;
		else
			throw new SQLException(Resources.get("noTypeConversion", obj
					.getClass().getName()));
	}

	public SQLWarning getWarnings() throws SQLException {
		return this.warnings;
	}

	public void insertRow() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public boolean isAfterLast() throws SQLException {
		this.ensureOpen();
		return this.currentRowNum > this.lastRowNum;
	}

	public boolean isBeforeFirst() throws SQLException {
		this.ensureOpen();
		return this.currentRowNum == 0;
	}

	public boolean isFirst() throws SQLException {
		this.ensureOpen();
		return this.currentRowNum == 1;
	}

	public boolean isLast() throws SQLException {
		this.ensureOpen();
		int lastRow = this.statement.getMaxRows();
		if (lastRow == 0 || lastRow > this.lastRowNum)
			lastRow = this.lastRowNum;
		return this.currentRowNum == this.lastRowNum;
	}

	public boolean last() throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public void moveToCurrentRow() throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public void moveToInsertRow() throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public boolean next() throws SQLException {
		this.ensureOpen();
		this.currentRowNum++;
		if (this.isAfterLast())
			return false;
		// Increment this.currentBatchRowNum.
		this.currentBatchRowNum++;
		// If not this.isLast(), check to see if this.currentBatchRowNum
		// has passed end of current batch.
		if (this.currentBatchRowNum >= this.currentBatch.size()
				&& !this.isLast()) {
			// Get next batch if it has and update this.currentBatchRowNum to 0.
			this.currentBatchRowNum = 0;
			this.currentBatch.clear();
			for (int i = 0; i < this.fetchSize && this.query.hasMoreRows(); i++)
				this.currentBatch.add(this.query.getNextRow());
			// If last batch, set this.lastRowNum to this.currentRowNum plus
			// current batch size minus 1.
			if (!this.query.hasMoreRows())
				this.lastRowNum = this.currentRowNum + this.currentBatch.size()
						- 1;
		}
		// If we get here, we found data to populate the row with.
		return true;
	}

	public boolean previous() throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public void refreshRow() throws SQLException {
		// Has no effect - we are assuming the database is static.
	}

	public boolean relative(int rows) throws SQLException {
		throw new SQLException(Resources.get("forwardOnly"));
	}

	public boolean rowDeleted() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public boolean rowInserted() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public boolean rowUpdated() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void setFetchDirection(int direction) throws SQLException {
		// We can ignore this as we read forwards only.
	}

	public void setFetchSize(int rows) throws SQLException {
		if (rows < 1)
			throw new SQLException(Resources.get("fetchSizeTooSmall"));
		// Changing here only affects new result sets, not existing ones.
		this.fetchSize = rows;
	}

	public void updateArray(int columnIndex, Array x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateArray(String columnName, Array x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateAsciiStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateAsciiStream(String columnName, InputStream x, int length)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBigDecimal(int columnIndex, BigDecimal x)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBigDecimal(String columnName, BigDecimal x)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBinaryStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBinaryStream(String columnName, InputStream x, int length)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBlob(String columnName, Blob x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBoolean(String columnName, boolean x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateByte(int columnIndex, byte x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateByte(String columnName, byte x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateBytes(String columnName, byte[] x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateCharacterStream(int columnIndex, Reader x, int length)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateCharacterStream(String columnName, Reader reader,
			int length) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateClob(String columnName, Clob x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateDate(int columnIndex, Date x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateDate(String columnName, Date x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateDouble(int columnIndex, double x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateDouble(String columnName, double x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateFloat(int columnIndex, float x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateFloat(String columnName, float x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateInt(int columnIndex, int x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateInt(String columnName, int x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateLong(int columnIndex, long x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateLong(String columnName, long x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateNull(int columnIndex) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateNull(String columnName) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateObject(int columnIndex, Object x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateObject(String columnName, Object x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateObject(int columnIndex, Object x, int scale)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateObject(String columnName, Object x, int scale)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateRef(String columnName, Ref x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateRow() throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateShort(int columnIndex, short x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateShort(String columnName, short x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateString(int columnIndex, String x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateString(String columnName, String x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateTime(int columnIndex, Time x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateTime(String columnName, Time x) throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateTimestamp(int columnIndex, Timestamp x)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public void updateTimestamp(String columnName, Timestamp x)
			throws SQLException {
		throw new SQLException(Resources.get("connectionReadOnly"));
	}

	public boolean wasNull() throws SQLException {
		return this.lastWasNull;
	}

	private boolean isClosed() {
		return this.closed;
	}

	private void ensureOpen() throws SQLException {
		if (this.isClosed())
			throw new SQLException(Resources.get("closedStatement"));
	}

	public void finalize() {
		try {
			if (!this.isClosed())
				this.close();
		} catch (SQLException e) {
			// Like, do we care?
		}
	}

}
