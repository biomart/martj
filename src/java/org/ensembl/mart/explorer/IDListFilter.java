/* Generated by Together */

package org.ensembl.mart.explorer;
import java.io.*;
import java.net.*;
import java.util.*;


public class IDListFilter implements Filter {

    public IDListFilter(String field, String[] identifiers) {
      this.field = field;
			this.identifiers = identifiers;
    }

    /**
     * Load identifiers from a file. Expect one identifier per line.
     */
    public IDListFilter(String field, File file) throws IOException {
      this( field, file.toURL() );
    }

    /**
     * Load identifiers from a file specified as a URL. Expect one identifier
     * per line of file.
     */
    public IDListFilter(String field, URL url) throws IOException {
      this.field = field;

      // load entries from file into identifiers array
      BufferedReader in = new BufferedReader( new InputStreamReader( url.openStream() ) );
      List lines = new ArrayList();
      for( String line = in.readLine(); line!=null; line = in.readLine() )
        lines.add( line );
      identifiers = new String[ lines.size() ];
      lines.toArray( identifiers );
    }


    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
     	buf.append(" field=").append( field );
      buf.append(", #identifiers=").append( identifiers.length);
      buf.append("]");

      return buf.toString();
    }

    public String sqlRepr(){
      StringBuffer buf = new StringBuffer();
      buf.append( field ).append( " IN (");
			for(int i=0; i<identifiers.length; ++i ) {
				if ( i>0 ) buf.append( ", " );
        buf.append("\"").append( identifiers[i] ).append("\"");
      }
      buf.append( " ) " );
			return buf.toString();
    }

    public String[] getIdentifiers(){ return identifiers; }

    public String getField(){
            return field;
        }

    private String[] identifiers;
    private String field;
}
