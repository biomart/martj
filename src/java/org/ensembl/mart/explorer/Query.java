package org.ensembl.mart.explorer;

import java.util.*;

public class Query {

	public boolean hasAttribute( Attribute attribute ) {
		return attributes.contains( attribute );
  }

	public void addAttribute( Attribute attribute ) {
		if ( !attributes.contains( attribute ) )
			attributes.add( attribute );
  }


  public void removeAttribute(  Attribute attribute ) {
		if ( attributes.contains( attribute ) )
			attributes.remove( attribute );
  }

    public List getAttributes() {
        return attributes;
    }

    public void setAttributes(List attributes) {
        this.attributes = attributes;
    }

    public List getFilters() {
        return filters;
    }

    public void setFilters(List filters) {
        this.filters = filters;
    }


    public void addFilter(Filter filter) {
      // new filters with the same hashCode() override old ones
			if ( filters.contains( filter ) )
      	filters.remove( filter );
			filters.add( filter );
    }

    public String getHost(){
            return host;
        }

    public void setHost(String host){
            this.host = host;
        }

    public String getPort(){ return port; }

    public void setPort(String port){ this.port = port; }

    public String getDatabase(){
            return database;
        }

    public void setDatabase(String database){
            this.database = database;
        }

    public String getUser(){
            return user;
        }

    public void setUser(String user){
            this.user = user;
        }

    public String getPassword(){
            return password;
        }

    public void setPassword(String password){
            this.password = password;
        }

    public ResultTarget getResultTarget(){
            return resultTarget;
        }

    public void setResultTarget(ResultTarget resultTarget){
            this.resultTarget = resultTarget;
        }

    public String toString() {
      StringBuffer buf = new StringBuffer();

			buf.append("[");
      buf.append(" host=").append(host);
      buf.append(" ,port=").append(port);
      buf.append(" ,user=").append(user);
      buf.append(" ,password=").append(password);
      buf.append(" ,attributes=").append(attributes);
      buf.append(" ,filters=").append(filters);
      buf.append(" ,resultTarget=").append(resultTarget);
      buf.append("]");

      return buf.toString();
    }

    private List attributes = new Vector();
    private List filters = new Vector();

    /** @link dependency */

  /*# Attribute lnkAttribute; */

    /** @link dependency */

  /*# Filter lnkFilter; */
  private String host;
  private String port;
  private String database;
  private String user;
  private String password;
  private ResultTarget resultTarget;
}
