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
	public String reftype;
	public String extension;
	
	private  String Name = null;
	private String PK = null;
	private String FK = null;
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
	 * @return Returns the fK.
	 */
	public String getFK() {
		return FK;
	}
	/**
	 * @param fk The fK to set.
	 */
	public void setFK(String fk) {
		FK = fk;
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
	 * @return Returns the pK.
	 */
	public String getPK() {
		return PK;
	}
	/**
	 * @param pk The pK to set.
	 */
	public void setPK(String pk) {
		PK = pk;
	}
	/**
	 * @return Returns the reftype.
	 */
	public String getReftype() {
		return reftype;
	}
	/**
	 * @param reftype The reftype to set.
	 */
	public void setReftype(String reftype) {
		this.reftype = reftype;
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
