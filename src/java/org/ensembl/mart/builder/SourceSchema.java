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
	MetaDataResolverOracle resolver;
	String dataset;
	
	public SourceSchema (String config){
		
		DBAdaptor adaptor = new DBAdaptor(config);
		MetaDataResolverOracle resolver = new MetaDataResolverOracle(adaptor);
		this.adaptor=adaptor;
		this.resolver=resolver;
		
	}
	
	/**
	public ArrayList getExportedKeyTables(String main_name){

		return resolver.getExportedKeyTables(main_name);		
	}
	
	
	public ArrayList getImportedKeyTables(String main_name){

		return resolver.getImportedKeyTables(main_name);		
	}
	
	*/
	
	
	public LinkedTables createLinkedTables(String main_name,Table [] tables){
		
		Table main = resolver.getMainTable(main_name);
		LinkedTables linked = new LinkedTables();
		linked.setMainTable(main);
		linked.setReferencedTables(tables);
		return linked;
	
	}
	
	
	public LinkedTables addTableToLink (String name, String key, String extension, String cardinality, LinkedTables linked){
		
		Table table = new Table();
		table.setName(name);
		table.setKey(key);
		table.setExtension(extension);
		table.setCardinality(cardinality);		
		table.setColumns(resolver.getReferencedColumns(table));
		linked.addTable(table);
		
		return linked;
	}
	
	
	
	public Table [] getKeyTables (String table_name){
		
		Table [] exp_key_tables;
		Table [] imp_key_tables;
			exp_key_tables=resolver.getExportedKeyTables(table_name);
			imp_key_tables = resolver.getImportedKeyTables(table_name);
	
	   Table [] join_tables = new Table [exp_key_tables.length+imp_key_tables.length];
	   
	   //System.out.println("join lenght " + join_tables.length);
	   
	   System.arraycopy(exp_key_tables,0,join_tables,0,exp_key_tables.length);
	   System.arraycopy(imp_key_tables,0,join_tables,exp_key_tables.length,imp_key_tables.length);
					
		return join_tables;
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
	
}
