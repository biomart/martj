package org.ensembl.mart.explorer;

import java.sql.*;
import java.io.*;

public interface QueryRunner {
	public void execute(Connection conn, OutputStream os) throws SQLException, IOException, InvalidQueryException;
}
