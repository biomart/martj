package org.ensembl.mart.explorer;

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
public class TabulatedQueryRunner implements QueryRunner {

    /**
     * Constructs a TabulatedQueryRunner object to execute a Query
     * and print tabulated output specified by the given FormatSpec
     * 
     * @param query - a Query Object
     * @param format - a FormatSpec object
     */
    public TabulatedQueryRunner(Query query, FormatSpec format) {
        this.query = query;
        this.format = format;
	}

    /**
     * Impliments the execute method of the interface.  For tabulated queries,
     * the SQL is executed, and the ResultSet is written to the OutputStream
     * via a OutputStreamWriter.  Each field of a ResultSet is separated by
     * the separator defined in the FormatSpec object in the output.
     */
    public void execute(Connection conn, OutputStream os) throws SQLException, IOException, InvalidQueryException {
        OutputStreamWriter osr =  new OutputStreamWriter(os);

        CompiledSQLQuery csql = new CompiledSQLQuery( conn, query );
        String sql = csql.toSQL();

        logger.debug( "QUERY : " + query );
        logger.debug( "SQL : " +sql );

        try {
            PreparedStatement ps = conn.prepareStatement( sql );
            int p=1;
            for( int i=0; i<query.getFilters().length; ++i) {
                Filter f = query.getFilters()[i];
                String value = f.getValue();
                if ( value!=null ) {
                  if ( logger.isDebugEnabled()  )
                    logger.debug("SQL (prepared statement value) : "+p+" = " + value);
                  ps.setString( p++, value);
                }
            }
            
            ResultSet rs = ps.executeQuery();
   
            while ( rs.next() ) {
              int nColumns = rs.getMetaData().getColumnCount();
              for (int i = 1; i <= nColumns; ++i) {
                if (i>1) osr.write( format.getSeparator() );
                String v = rs.getString(i);
                if (  v!= null ) osr.write( v );
                logger.debug( v );
              }
              osr.write("\n");
              osr.flush();
            }
            osr.close();
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
