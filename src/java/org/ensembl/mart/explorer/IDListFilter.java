/* Generated by Together */

package org.ensembl.mart.explorer;
import java.io.*;
import java.net.*;
import java.util.*;


public class IDListFilter implements Filter {

  public final static int STRING_MODE = 0;
  public final static int FILE_MODE = 1;
  public final static int URL_MODE = 2;


  public int getMode() {
    return mode;
  }

    public IDListFilter(String field, String[] identifiers) {
      this.type = field;
      this.identifiers = identifiers;
      mode = STRING_MODE;
    }

    /**
     * Load identifiers from a file. Expect one identifier per line.
     */
    public IDListFilter(String field, File file) throws IOException {
      this( field, file.toURL() );
      this.file = file;
      mode = FILE_MODE;
    }

    /**
     * Load identifiers from a file specified as a URL. Expect one identifier
     * per line of file.
     */
    public IDListFilter(String field, URL url) throws IOException {
      this.type = field;
      this.url = url;
      mode = URL_MODE;
      // load entries from file into identifiers array
      BufferedReader in = new BufferedReader( new InputStreamReader( url.openStream() ) );
      List lines = new ArrayList();
      for( String line = in.readLine(); line!=null; line = in.readLine() )
        lines.add( line );
      identifiers = new String[ lines.size() ];
      lines.toArray( identifiers );
    }

    public IDListFilter() {
    }

  public void addIdentifier(String identifier) {
    String[] tmp = identifiers;
    identifiers = new String[ tmp.length+1 ];
    System.arraycopy(tmp, 0, identifiers, 0, tmp.length);
    identifiers[ tmp.length ] = identifier;
  }

    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
     	buf.append(" field=").append( type);
      buf.append(", #identifiers=").append( identifiers.length);
      buf.append("]");

      return buf.toString();
    }

    public String getWhereClause(){
      StringBuffer buf = new StringBuffer();
      buf.append( type).append( " IN (");
      for(int i=0; i<identifiers.length; ++i ) {
        if ( i>0 ) buf.append( ", " );
        buf.append("\"").append( identifiers[i] ).append("\"");
      }
      buf.append( " ) " );
      return buf.toString();
    }

  public String getRightHandClause() {
    StringBuffer buf = new StringBuffer();
    buf.append( " IN (");
    for(int i=0; i<identifiers.length; ++i ) {
      if ( i>0 ) buf.append( ", " );
      buf.append("\"").append( identifiers[i] ).append("\"");
    }
    buf.append( " ) " );
    return buf.toString();
  }


    public String[] getIdentifiers(){ return identifiers; }

  public String getType(){
    return type;
  }

  public void setType(String type){ this.type = type; }

  public File getFile(){
    return file;
  }

  public void setFile(File file){ this.file = file; }
  
  public URL getUrl(){
    return url;
  }

  public void setUrl(URL url){ this.url = url; }

  
  public String getValue() {
    return null;
  }
  
  private String type;
  private String[] identifiers = new String[0];
  private File file;
  private URL url;
  private int mode;
}
