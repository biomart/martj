/* Generated by Together */

package org.ensembl.mart.explorer;

import java.sql.*;
import java.io.*;
import org.apache.log4j.*;

public class ResultStats implements ResultTarget {

	private final static Logger logger = Logger.getLogger( ResultStats.class.getName() );

	public ResultStats(String fileName, Formatter formatter, int nLinesToPrint) {
  	this.name = fileName;
    this.formatter = formatter;
    this.nLinesToPrint = nLinesToPrint;
  }

    public String getName() {
        return name;
    }

    public void setName(String fileName) {
        this.name = fileName;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
      buf.append(" name=").append(name);
      buf.append(" ,formatter=").append(formatter);
      buf.append(" ,charCount=").append( charCount );
      buf.append(" ,lineCount=").append( lineCount );
      buf.append("]");

      return buf.toString();
    }

    public void output(ResultSet rs) throws FormatterException {
      try {
        logger.info( "Creating results stats: " + name );
        formatter.setResultSet( rs );
				FileWriter out = new FileWriter( name );
      	for( String line = formatter.readLine(); line!=null; line=formatter.readLine() ) {
        	if ( lineCount < nLinesToPrint) System.out.println(line);
        	charCount += line.length();
          lineCount++;
      	}
       }catch ( IOException e ) {
				throw new FormatterException ( e );
      }
      catch (SQLException e) {
				throw new FormatterException ( e );
      }
    }

    public int getLineCount(){
            return lineCount;
        }

    public void setCharCount(int charCount){ this.charCount = charCount; }

    public int getCharCount(){
            return charCount;
        }

    private String name;
    private Formatter formatter;
    private int lineCount;
    private int charCount;
    private int nLinesToPrint;
}
