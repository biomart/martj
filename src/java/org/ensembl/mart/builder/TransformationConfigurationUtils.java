/*
 * Created on May 5, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author arek
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class TransformationConfigurationUtils {

public static void main(String[] args) throws IOException {
		
		
		String [] types = {
				
				 "DNA_motif",
				 "RNA_motif",
				 "aberration_junction",
				 "enhancer",
				 "insertion_site",
				 "mRNA",
				 "ncRNA",
				 "point_mutation",
				 "protein_binding_site",
				 "pseudogene",
				 "rRNA",
				 "region",
				 "regulatory_region",
				 "repeat_region",
				 "rescue_fragment",
				 "sequence_variant",
				 "snRNA",
				 "snoRNA",
				 "tRNA",
		};
		
		String [] dbs = {
				"Gaddataset",
				 "SO",
				 "GO",
				 "datasetBase",
				 "InterPro",
				 "PUBMED",	
		};
		
		/**
		String [] gos ={
				
				"cellular_component",
				 "molecular_function",
				 "biological_process",
				 "Biological Process (Gene Ontology)"
				 
		};
		*/
		
		String [] gos ={
				
				"Cellular Component (Gene Ontology)",
				"Molecular Function (Gene Ontology)",
				 "Biological Process (Gene Ontology)"
		};
		
		
		
		
		
		
		String configFile="/Applications/eclipse/workspace/martj-head/data/builder/new.config";
		
	
		File f = new File(configFile);
		f.delete();

		BufferedWriter out = null;
		out = new BufferedWriter(new FileWriter(configFile, true));
		
		String dataset = "fly";
		
		
		/**	
		dataset	m	feature	imported	cvterm_id	CVTERM	n1	null	cvterm_id=219	1	type_id	null	$table\tN        
		dataset	m	feature	exported	feature_id	FEATURELOC	11	null	null	1	feature_id	null	
		dataset	m	feature	exported	srcfeature_id	FEATURE	11	null	null	1	feature_id	name,uniquename	
		dataset	m	feature	imported	organism_id	ORGANISM	n1	null	null	1	organism_id	null#";
		*/
		
		int transformations=0;
		
		for (int i=0;i<1;i++){
		transformations++;
		
		String tabledm=dataset+"__gene__main";
		
		String [] first =  {"dataset","m","feature","imported","cvterm_id","CVTERM","n1","null","name=\'gene\'",""+transformations,"type_id","null",tabledm,"N"};
		String [] second = {"dataset","m","feature","exported","feature_id","FEATURELOC","11","null",	"null",""+transformations,"feature_id","null"};
		String [] third =  {"dataset","m","feature","exported","srcfeature_id","FEATURE","11","null",	"null",""+transformations,"feature_id","name,uniquename"};
		String [] fourth = {"dataset","m","feature","imported","organism_id","ORGANISM","n1","null","null",""+transformations,"organism_id","null"};
		
		String [] [] one = {first,second,third,fourth};
		
		//printConfig(one,tabledm,out);
		}
		
		
		/**
		dataset	d	feature	imported	cvterm_id	CVTERM	n1	null	name='$type'	$transformations	type_id	null	$table\tY
		dataset	d	feature	exported	feature_id	FEATURE_RELATIONSHIP	11	null	null	$transformations	subject_id	null
		dataset	d	feature	exported	object_id	FEATURE	11	null	null	$transformations	feature_id	feature_id\n#";
		*/
		
		for (int i=0;i<types.length;i++){
		
			transformations++;
			String tabledm = dataset+"__"+types[i]+"__dm";
			
			String [] fifth =   {"dataset","d","feature","imported","cvterm_id","CVTERM","n1","null","name=\'"+types[i]+"\'",""+transformations,"type_id","null",tabledm,"Y"};
			String [] sixth =   {"dataset","d","feature","exported","feature_id","FEATURE_RELATIONSHIP","11",	"null",	"null",""+transformations,"subject_id","null"};
			String [] seventh=  {"dataset","d","feature","exported","object_id","FEATURE","11","null","null",	""+transformations,"feature_id",	"feature_id"};
			
			String [] [] two ={fifth,sixth,seventh};			
		   
			//printConfig(two,tabledm,out);
		}
		
		
		/**		
		dataset	d	dbxref	exported	db_id	DB	11	null	name='$db'	$transformations	db_id	null	$table\tY
		dataset	d	dbxref	exported	dbxref_id	FEATURE_DBXREF	11	null	null	$transformations	dbxref_id	null
		dataset	d	dbxref	exported	feature_id	FEATURE_RELATIONSHIP	11	null	null	$transformations	subject_id	null
		dataset	d	dbxref	exported	object_id	FEATURE	11	null	null	$transformations	feature_id	feature_id\n#";

		*/	
		
		for (int i=0;i<dbs.length;i++){
			
				transformations++;
				String tabledm = dataset+"__"+dbs[i]+"__dm";
				
				String [] fifth =  {"dataset","d","dbxref","exported","db_id","DB",	"11","null",	"name=\'"+dbs[i]+"\'",""+transformations,"db_id",	"null",	tabledm,"Y"};
				String [] sixth =  {"dataset","d","dbxref","exported",	"dbxref_id",	"FEATURE_DBXREF","11","null","null",""+transformations,	"dbxref_id",	"null"};
				String [] seventh= {"dataset","d","dbxref","exported",	"feature_id","FEATURE_RELATIONSHIP","11","null","null",	""+transformations,	"subject_id","null"};
				String [] eight=   {"dataset","d","dbxref","exported",	"object_id",	"FEATURE","11","null","null",	""+transformations,"feature_id",	"feature_id"};
				
				String [] [] two ={fifth,sixth,seventh,eight};			
			 
			//	printConfig(two,tabledm,out);
			
			}
		
		

		/**
		  dataset	d	cvterm	imported	cv_id	CV	n1	null	name='$go'	$transformations	cv_id	null	$table\tY
		  dataset	d	cvterm	imported	dbxref_id	DBXREF	11	null	null	$transformations	dbxref_id	null
		  dataset	d	cvterm	exported	cvterm_id	FEATURE_CVTERM	11	null	null	$transformations	cvterm_id	null
		  dataset	d	cvterm	imported	feature_id	FEATURE	11	null	null	$transformations	feature_id	feature_id\n#";
		  
		 */
		
		for (int i=0;i<gos.length;i++){
			
				transformations++;
				String tb1 = dataset+"__"+gos[i]+"__dm";
			
				//char bracket ='Process';
				String tb2 = tb1.replace(' ','_');
				String tb3 = tb2.replace('(','1');
				String tabledm = tb3.replace(')','1');
				
				//System.out.println ("table "+tabledm);
				
				String [] fifth =   {"dataset","d","cvterm","imported","cv_id","CV","n1","null",	"name=\'"+gos[i]+"\'",""+transformations,"cv_id",	"null",	tabledm,"Y"};
				String [] sixth =   {"dataset","d","cvterm","imported","dbxref_id",	"DBXREF","11","null",	"null",""+transformations,"dbxref_id",	"null"};
				String [] seventh=  {"dataset","d","cvterm","exported","cvterm_id","FEATURE_CVTERM","11","null","null",""+transformations,"cvterm_id",	"null"};
				String [] eight=    {"dataset","d","cvterm","imported","feature_id","FEATURE","11","null","null",	""+transformations,"feature_id","feature_id"};
				
				String [] [] two ={fifth,sixth,seventh,eight};			
				
				//printConfig(two,tabledm,out);
			
			}	
		
		
		
		
		for (int i=0;i<1;i++){
			transformations++;
			
			String tabledm=dataset+"__gene_structure_dm";
			
             String type="mRNA";
             
			String [] first =   {"dataset","d","feature","imported","cvterm_id","CVTERM","n1","null","name=\'"+type+"\'",""+transformations,"type_id","null",tabledm,"Y"};
			String [] second =  {"dataset","d","feature","exported","feature_id","FEATURE_RELATIONSHIP","11",	"null",	"null",""+transformations,"object_id","subject_id"};
			String [] third =   {"dataset","d","feature","exported","subject_id","FEATURE","11","null","null",	""+transformations,"feature_id",	"feature_id,uniquename,type_id"};
			String [] fourth =  {"dataset","d","feature","exported","feature_id","FEATURELOC","11","null",	"null",""+transformations,"feature_id","fmin,fmax,strand"};
			String [] fith =    {"dataset","m","feature","exported","srcfeature_id","FEATURE","11","null",	"null",""+transformations,"feature_id","name,uniquename"};
			
			String [] [] one = {first,second,third,fourth};
			
			
			
			
			printConfig(one,tabledm,out);
			}
			
		
		
		
		
		
		

		out.close();
		System.out.println("WRITTEN TO: "+f);
}




private static void printConfig(String [][] lines,String table, BufferedWriter out) throws IOException{
	
	
	out.write("#"+"\n");
	out.write("#        TABLE: "+ table.toUpperCase()+"\n");
	out.write("#"+"\n");
	
	for (int i = 0; i < lines.length; i++) {
		for (int j = 0; j < lines[i].length; j++) {
			out.write(lines[i][j].concat("\t"));
		}
		out.write("\n");
	}
}



}
		
	
