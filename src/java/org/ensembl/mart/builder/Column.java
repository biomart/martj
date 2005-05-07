/*
 * Created on Jun 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */


public class Column implements Cloneable {
	
	String name;
	String alias;
	String original_table;
	String original_name;
	String final_table_name;
	boolean deleted;
	boolean bool=false;
	boolean userAlias=false;
	
	public Object clone  ()
	throws CloneNotSupportedException
	{
		return super.clone();	
	}
	
	
	/**
	 * @return Returns boolean.
	 */
	
	public boolean hasAlias() {
		
		if(getAlias() == null || getAlias() == ""){
			return false;
		} else { return true;}
		
	}
	
	public boolean isDeleted() {
		
		if(deleted == false){
			return true;
		} else { return false;}
		
	}
	
	/**
	 * @return Returns the alias.
	 */
	public String getAlias() {
		return alias;
	}
	/**
	 * @param alias The alias to set.
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
