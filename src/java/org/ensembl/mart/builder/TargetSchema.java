/*
 * Created on Jun 14, 2004
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


public class TargetSchema {
	
	ArrayList transformations = new ArrayList();
	String dataset;
	
	public TargetSchema (SourceSchema source_schema){
		
		dataset= source_schema.dataset;
		
		for (int j=0;j<source_schema.getLinkedTables().length;j++){
	    Transformation final_table = new Transformation(source_schema.getLinkedTables()[j]);	
		final_table.dataset = dataset;
	    addTransformation(final_table);
		}
	}
	
	
	
	
	public Transformation [] getTransformations() {
		
		Transformation [] b = new Transformation[1];
		return (Transformation []) transformations.toArray(b);	
		
	}
	
	public void addTransformation(Transformation transformation){
		this.transformations.add(transformation);
			
	}
	
}