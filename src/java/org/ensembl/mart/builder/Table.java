/*
 * Created on Jun 7, 2004
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


public class Table  implements Cloneable {
	
	public String key=null;
	public String cardinality = null;
	public String extension;
	public String status;
	public boolean skip;
	public boolean final_table;
	private  String Name = null;
	private Column [] columns;
	
	
	
	
	public Object clone  ()
	throws CloneNotSupportedException
	{
		Table copy = null;
		copy = (Table) super.clone();	
		
		Column [] new_ref_col = new Column[columns.length];
		for (int n=0;n<this.getColumns().length;n++){
			try {
				Column col = (Column) this.getColumns()[n].clone();
				new_ref_col[n]=col;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		copy.columns = new_ref_col;
		return copy;
	}
	
	
	/**
	 * @return Returns the colums.
	 */
	public Column [] getColumns() {
		return columns;
	}
	/**
	 * @param columns The colums to set.
	 */
	public void setColumns(Column [] columns) {
		this.columns = columns;
	}
	
	/**
	 * @return Returns the cardinality.
	 */
	public String getCardinality() {
		return cardinality;
	}
	
	
	public void setCardinality(String card) {
		
		cardinality=card;
	}
	
	
	
	
	
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return Name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		Name = name;
	}
	
	
	/**
	 * @return Returns the key.
	 */
	public String getKey() {
		return key;
	}
	/**
	 * @param key The key to set.
	 */
	public void setKey(String key) {
		this.key = key;
	}
	/**
	 * @return Returns the extension.
	 */
	public String getExtension() {
		return extension;
	}
	/**
	 * @param extension The extension to set.
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

public boolean hasExtension(){
	
	if (getExtension() != null){
		
		return true;
	} else {return false;}
	
}


}
