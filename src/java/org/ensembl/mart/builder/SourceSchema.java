/*
 * Created on Jun 13, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

import java.util.ArrayList;


/**
 * @author <a href="mailto: arek@ebi.ac.uk">Arek Kasprzyk</a>
 *
 * 
 */
public class SourceSchema {

	ArrayList linked_tables = new ArrayList();
	DBAdaptor adaptor;
	MetaDataResolverOracle resolver;
	
	public SourceSchema (String config){
		
		DBAdaptor adaptor = new DBAdaptor(config);
		MetaDataResolverOracle resolver = new MetaDataResolverOracle(adaptor);
		this.adaptor=adaptor;
		this.resolver=resolver;
		
	}
	
	public Table [] getExportedKeyTables (String main_name){
		
		return resolver.getExportedKeyTables(main_name);
		
	}
	
	
	public LinkedTables createLinkedTables(String main_name,Table [] tables){
		
		Table main = resolver.getMainTable(main_name);
		LinkedTables linked = new LinkedTables();
		linked.setMainTable(main);
		linked.setReferencedTables(tables);
		return linked;
	
	}
	
	
	public LinkedTables addDimension (Table table, LinkedTables linked){
		
		table.setColumns(resolver.getReferencedColumns(table));
		linked.addTable(table);
		return linked;
	}
	
	
	
	/**
	 * @return Returns the linkedTables.
	 */
	public LinkedTables [] getLinkedTables() {
		
		LinkedTables [] b = new LinkedTables[1];
		return (LinkedTables []) linked_tables.toArray(b);	
		
	}
	
	public void addLinkedTables(LinkedTables linked){
		this.linked_tables.add(linked);
		
		
	}
	
}
