package org.ensembl.mart.explorer;

import java.io.*;
import java.sql.*;
import org.apache.log4j.*;

public class TabulatedQueryRunner implements QueryRunner {

    public TabulatedQueryRunner(Query query, FormatSpec format) {
        this.query = query;
        this.format = format;
	}

    public void execute(Connection conn, OutputStream os) throws SQLException, IOException, InvalidQueryException {
        OutputStreamWriter osr =  new OutputStreamWriter(os);

        CompiledSQLQuery csql = new CompiledSQLQuery( conn, query );
        String sql = csql.toSQL();

        logger.info( "QUERY : " + query );
        logger.info( "SQL : " +sql );

        try {
            PreparedStatement ps = conn.prepareStatement( sql );
            int p=1;
            for( int i=0; i<query.getFilters().length; ++i) {
                Filter f = query.getFilters()[i];
                String value = f.getValue();
                if ( value!=null ) {
                    logger.info("SQL (prepared statement value) : "+p+" = " + value);
                    ps.setString( p++, value);
                }
            }
     
            ResultSet rs = ps.executeQuery();

		    while ( rs.next() ) {
                int nColumns = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= nColumns; ++i) {
                    if ( rs.getString(i) != null )
                        osr.write( rs.getString(i) );
                    osr.write( format.getSeparator() );
			    }
                osr.write("\n");
		    }
            osr.flush();
		}
        catch (IOException e) {
            logger.warn("Couldnt write to OutputStream\n"+e.getMessage());
            throw e;
		} 
        catch (SQLException e) {
            logger.warn(e.getMessage()+ " : " + sql);
            throw e;
        }
	}

    private Logger logger = Logger.getLogger(TabulatedQueryRunner.class.getName());
    private Query query = null;
    private FormatSpec format = null;
}
