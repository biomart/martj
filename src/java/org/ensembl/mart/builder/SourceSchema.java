/*
 * Created on Jun 13, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

import java.util.*;


/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */
public class SourceSchema {

	ArrayList linked_tables = new ArrayList();
	DBAdaptor adaptor;
	MetaDataResolver resolver;
	String dataset;
	
	public SourceSchema (String config){
		
		DBAdaptor adaptor = new DBAdaptor(config);
		MetaDataResolver resolver = null;
		
		if (adaptor.rdbms.equals("mysql")){
		resolver = new MetaDataResolverMySQL(adaptor);
		} if (adaptor.rdbms.equals("oracle")){
		resolver = new MetaDataResolverOracle(adaptor);
		}
		this.adaptor=adaptor;
		this.resolver=resolver;
		
	}
	
	
	
	public LinkedTables createLinkedTables(String main_name,Table [] tables){
		
		Table main = resolver.getMainTable(main_name);
		LinkedTables linked = new LinkedTables();
		linked.setMainTable(main);
		linked.setReferencedTables(tables);
		return linked;
	
	}
	
	
	public Column [] getTableColumns (String name){
	
		return resolver.getReferencedColumns(name);
	}
	
	
	
	public Table [] getReferencedTables (String table_name){
		
		return resolver.getReferencedTables(table_name);
	}
	
	
	public Table getMainTable (String table_name){
		
		return resolver.getMainTable(table_name);
		
	}
	
	
	
	/**
	 * @return Returns the linkedTables.
	 */
	public LinkedTables [] getLinkedTables() {
		
		LinkedTables [] b = new LinkedTables[linked_tables.size()];
		return (LinkedTables []) linked_tables.toArray(b);	
		
	}
	
	public void addLinkedTables(LinkedTables linked){
		this.linked_tables.add(linked);
		
		
	}

	public LinkedTables getLinkedTablesByFinalTableName (String name){
		
		LinkedTables [] linked = getLinkedTables();
		LinkedTables link = new LinkedTables();
		
		for (int i=0;i<linked.length; i++){
			if (linked[i].final_table_name.equals(name)){
				link = linked[i];		
			}
		}
		return link;
	}
	
		
}
