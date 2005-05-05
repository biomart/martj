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
				"Gadfly",
				 "SO",
				 "GO",
				 "FlyBase",
				 "InterPro",
				 "PUBMED",	
		};
		
		
		String [] gos ={
				
				"cellular_component",
				 "molecular_function",
				 "biological_process",
		};
		
		
		String configFile="/Applications/eclipse/workspace/martj-head/data/builder/new.config";
		
	
		File f = new File(configFile);
		f.delete();

		BufferedWriter out = null;
		out = new BufferedWriter(new FileWriter(configFile, true));
		
		
		
		
		/**	
		fly	m	feature	imported	cvterm_id	CVTERM	n1	null	cvterm_id=219	1	type_id	null	$table\tN        
		fly	m	feature	exported	feature_id	FEATURELOC	11	null	null	1	feature_id	null	
		fly	m	feature	exported	srcfeature_id	FEATURE	11	null	null	1	feature_id	name,uniquename	
		fly	m	feature	imported	organism_id	ORGANISM	n1	null	null	1	organism_id	null#";
		*/
		
		int transformations=0;
		
		for (int i=0;i<2;i++){
		transformations++;
		
		String tabledm="fly__gene__main";
		
		String [] first =  {"fly","m","feature","imported","cvterm_id","CVTERM","n1","null","cvterm_id=219",""+transformations,"type_id","null",tabledm,"N"};
		String [] second = {"fly","m","feature","exported","feature_id","FEATURELOC","11","null",	"null",""+transformations,"feature_id","null"};
		String [] third =  {"fly","m","feature","exported","srcfeature_id","FEATURE","11","null",	"null",""+transformations,"feature_id","name,uniquename"};
		String [] fourth = {"fly","m","feature","imported","organism_id","ORGANISM","n1","null","null",""+transformations,"organism_id","null"};
		
		String [] [] one = {first,second,third,fourth};
		
		printConfig(one,tabledm,out);
		}
		
		
		/**
		fly	d	feature	imported	cvterm_id	CVTERM	n1	null	name='$type'	$transformations	type_id	null	$table\tY
		fly	d	feature	exported	feature_id	FEATURE_RELATIONSHIP	11	null	null	$transformations	subject_id	null
		fly	d	feature	exported	object_id	FEATURE	11	null	null	$transformations	feature_id	feature_id\n#";
		*/
		
		for (int i=0;i<types.length;i++){
		
			transformations++;
			String tabledm = "fly__"+types[i]+"__dm";
			
			String [] fifth =   {"fly","d","feature","imported","cvterm_id","CVTERM","n1","null","name="+types[i],""+transformations,"type_id","null",tabledm,"Y"};
			String [] sixth =   {"fly","d","feature","exported","feature_id","FEATURE_RELATIONSHIP","11",	"null",	"null",""+transformations,"subject_id","null"};
			String [] seventh=  {"fly","d","feature","exported","object_id","FEATURE","11","null","null",	""+transformations,"feature_id",	"feature_id"};
			
			String [] [] two ={fifth,sixth,seventh};			
		   
			printConfig(two,tabledm,out);
		}
		
		
		/**		
		fly	d	dbxref	exported	db_id	DB	11	null	name='$db'	$transformations	db_id	null	$table\tY
		fly	d	dbxref	exported	dbxref_id	FEATURE_DBXREF	11	null	null	$transformations	dbxref_id	null
		fly	d	dbxref	exported	feature_id	FEATURE_RELATIONSHIP	11	null	null	$transformations	subject_id	null
		fly	d	dbxref	exported	object_id	FEATURE	11	null	null	$transformations	feature_id	feature_id\n#";

		*/	
		
		for (int i=0;i<dbs.length;i++){
			
				transformations++;
				String tabledm = "fly__"+dbs[i]+"__dm";
				String [] fifth =  {"fly","d","dbxref","exported","db_id","DB",	"11","null",	"name="+dbs[i],""+transformations,"db_id",	"null",	tabledm,"Y"};
				String [] sixth =  {"fly","d","dbxref","exported",	"dbxref_id",	"FEATURE_DBXREF","11","null","null",""+transformations,	"dbxref_id",	"null"};
				String [] seventh= {"fly","d","dbxref","exported",	"feature_id","FEATURE_RELATIONSHIP","11","null","null",	""+transformations,	"subject_id","null"};
				String [] eight=   {"fly","d","dbxref","exported",	"object_id",	"FEATURE","11","null","null",	""+transformations,"feature_id",	"feature_id"};
				
				String [] [] two ={fifth,sixth,seventh,eight};			
			 
				printConfig(two,tabledm,out);
			
			}
		
		

		/**
		  fly	d	cvterm	imported	cv_id	CV	n1	null	name='$go'	$transformations	cv_id	null	$table\tY
		  fly	d	cvterm	imported	dbxref_id	DBXREF	11	null	null	$transformations	dbxref_id	null
		  fly	d	cvterm	exported	cvterm_id	FEATURE_CVTERM	11	null	null	$transformations	cvterm_id	null
		  fly	d	cvterm	imported	feature_id	FEATURE	11	null	null	$transformations	feature_id	feature_id\n#";
		  
		 */
		
		for (int i=0;i<gos.length;i++){
			
				transformations++;
				String tabledm = "fly__"+gos[i]+"__dm";
				String [] fifth =   {"fly","d","cvterm","imported","cv_id","CV","n1","null",	"name="+gos[i],""+transformations,"cv_id",	"null",	tabledm,"Y"};
				String [] sixth =   {"fly","d","cvterm","imported","dbxref_id",	"DBXREF","11","null",	"null",""+transformations,"dbxref_id",	"null"};
				String [] seventh=  {"fly","d","cvterm","exported","cvterm_id","FEATURE_CVTERM","11","null","null",""+transformations,"cvterm_id",	"null"};
				String [] eight=    {"fly","d","cvterm","imported","feature_id","FEATURE","11","null","null",	""+transformations,"feature_id","feature_id"};
				
				String [] [] two ={fifth,sixth,seventh,eight};			
				
				printConfig(two,tabledm,out);
			
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
		out.write("#"+"\n");
	}
}



}
		
	
