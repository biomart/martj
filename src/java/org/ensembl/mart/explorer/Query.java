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
            this.host = database;
        }

    public String getUser(){
            return user;
        }

    public void setUser(String user){
            this.host = user;
        }

    public String getPassword(){
            return password;
        }

    public void setPassword(String password){
            this.host = password;
        }

    private List attributes;
    private List filters;

    /** @link dependency */

  /*# Attribute lnkAttribute; */

    /** @link dependency */

  /*# Filter lnkFilter; */
  private String host;
  private String port;
  private String database;
  private String user;
  private String password;
}
