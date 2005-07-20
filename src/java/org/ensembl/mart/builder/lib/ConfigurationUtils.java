/*
 * Created on May 5, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ensembl.mart.builder.lib;

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
public class ConfigurationUtils {

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
		//		 "SO",
		//		 "GO",
				 "FlyBase",
		//		 "InterPro",
		//		 "PUBMED",	
		};
		
		
		String [] gos ={
				
				"cellular_component",
				 "molecular_function",
				 "biological_process",
				 
		};
		
		/**
		String [] gos ={
				
				"Cellular Component (Gene Ontology)",
				"Molecular Function (Gene Ontology)",
				 "Biological Process (Gene Ontology)"
		};
		
		*/
		
		
		
		
		String configFile="/Applications/eclipse/workspace/martj-head/data/builder/new.config";
		
	
		/**
		 * 0  dataset
		 * 9  transformation
		 * 11 reference column names
		 * 12 reference column aliases
		 * 13 central column names
		 * 14 central column aliases
		 * 15 user table name
		 * 16 central filter
		 * 
		 */
		
		
		
		File f = new File(configFile);
		f.delete();

		BufferedWriter out = null;
		out = new BufferedWriter(new FileWriter(configFile, true));
		
		//String dataset1 = "yeast";
		//String dataset2="sc_gene_structure";
		
		String dataset1="fly";
		String dataset2="flstructure";
		
		
		int transformations=0;
		
		for (int i=0;i<1;i++){
		transformations++;
		
		String tabledm=dataset1+"__gene__main";
		
		String [] first =  {dataset1,"m","feature","imported","cvterm_id","cvterm","n1","null","name=\'gene\'",""+transformations,"type_id","null","null","feature_id,organism_id,name,uniquename,seqlen,type_id","null,null,gene_name,gene_uniquename,null,null",tabledm,"N"};
		String [] second = {dataset1,"m","feature","exported","feature_id","featureloc","11","null",	"null",""+transformations,"feature_id","fmin,fmax,strand,srcfeature_id,rank","gene_start,gene_end,null,null,null","null","null"};
		String [] third =  {dataset1,"m","feature","exported","srcfeature_id","feature","11","null",	"null",""+transformations,"feature_id","name,uniquename","chromosome_acc,chromosome","null","null"};
		String [] fourth = {dataset1,"m","feature","imported","organism_id","organism","n1","null","null",""+transformations,"organism_id","null","null","null","null"};
		
		String [] [] one = {first,second,third,fourth};
		
		printConfig(one,tabledm,out);
		}
		
		
		/**
		
		for (int i=0;i<1;i++){
		transformations++;
		
		String tabledm=dataset1+"__chromosomal_feature__main";
		
		String [] first =  {dataset1,"m","feature","imported","cvterm_id","CVTERM","n1","null","null",""+transformations,"type_id","null","null","feature_id,organism_id,name,uniquename,seqlen,type_id","null,null,feature_name,feature_uniquename,null,null",tabledm,"N"};
		String [] second = {dataset1,"m","feature","exported","feature_id","FEATURELOC","11","null",	"null",""+transformations,"feature_id","fmin,fmax,strand,srcfeature_id,rank","feature_start,feature_end,null,null,null"};
		String [] third =  {dataset1,"m","feature","exported","srcfeature_id","FEATURE","11","null",	"null",""+transformations,"feature_id","name,uniquename","chromosome_acc,chromosome"};
		String [] fourth = {dataset1,"m","feature","imported","organism_id","ORGANISM","n1","null","null",""+transformations,"organism_id","null","null"};
		
		String [] [] one = {first,second,third,fourth};
		
		//printConfig(one,tabledm,out);
		}
		
		*/
		
		
		
		
		
		for (int i=0;i<types.length;i++){
		
			transformations++;
			String tabledm = dataset1+"__"+types[i]+"__dm";
			
			String [] fifth =   {dataset1,"d","feature","imported","cvterm_id","cvterm","n1","null","name=\'"+types[i]+"\'",""+transformations,"type_id","name","type","feature_id,name,uniquename,seqlen","null",tabledm,"Y"};
			String [] sixth =   {dataset1,"d","feature","exported","feature_id","feature_relationship","11",	"null",	"null",""+transformations,"subject_id","object_id","null","null","null"};
			String [] seventh=  {dataset1,"d","feature","exported","object_id","feature","11","null","null",	""+transformations,"feature_id",	"feature_id","null","null","null"};
			
			String [] [] two ={fifth,sixth,seventh};			
		   
			printConfig(two,tabledm,out);
		}
		
		
		
		for (int i=0;i<dbs.length;i++){
			
				transformations++;
				String tabledm = dataset1+"__"+dbs[i]+"__dm";
				
				String [] fifth =  {dataset1,"d","dbxref","exported","db_id","db",	"11","null",	"name=\'"+dbs[i]+"\'",""+transformations,"db_id",	"null","null","null","null",	tabledm,"Y"};
				String [] sixth =  {dataset1,"d","dbxref","exported",	"dbxref_id",	"feature_dbxref","11","null","null",""+transformations,	"dbxref_id",	"null","null","null","null"};
				String [] seventh= {dataset1,"d","dbxref","exported",	"feature_id","feature_relationship","11","null","null",	""+transformations,	"subject_id","null","null","null","null"};
				String [] eight=   {dataset1,"d","dbxref","exported",	"object_id",	"feature","11","null","null",	""+transformations,"feature_id",	"feature_id","null","null","null"};
				
				String [] [] two ={fifth,sixth,seventh,eight};			
			 
				printConfig(two,tabledm,out);
			
			}
		
		

		
		for (int i=0;i<gos.length;i++){
			
				transformations++;
				String tb1 = dataset1+"__"+gos[i]+"__dm";
			
				String tb2 = tb1.replace(' ','_');
				String tb3 = tb2.replace('(','1');
				String tabledm = tb3.replace(')','1');
				
				String [] fifth =   {dataset1,"d","cvterm","imported","cv_id","cv","n1","null",	"name=\'"+gos[i]+"\'",""+transformations,"cv_id",	"cv_id","null","name,dbxref_id,cvterm_id","null",	tabledm,"Y"};
				String [] sixth =   {dataset1,"d","cvterm","imported","dbxref_id",	"dbxref","11","null",	"null",""+transformations,"dbxref_id",	"accession","null","null","null"};
				String [] seventh=  {dataset1,"d","cvterm","exported","cvterm_id","feature_cvterm","11","null","null",""+transformations,"cvterm_id",	"cvterm_id,feature_id","null","null","null"};
				String [] eight=    {dataset1,"d","cvterm","imported","feature_id","feature","11","null","null",	""+transformations,"feature_id","feature_id","null","null","null"};
				
				String [] [] two ={fifth,sixth,seventh,eight};			
				
				printConfig(two,tabledm,out);
			
			}	
		
		
		
		
		for (int i=0;i<1;i++){
			transformations++;
			
			String tabledm=dataset2+"__gene_structure__main";
			
             String type="mRNA";
             
			String [] first =   {dataset2,"m","feature","imported","cvterm_id","cvterm","n1","null","name=\'"+type+"\'",""+transformations,"type_id","null","null","feature_id,organism_id,name,uniquename,seqlen,type_id","null,null,transcript_name,transcript_uniquename,null,null",tabledm,"N"};
			String [] second =  {dataset2,"m","feature","exported","feature_id","feature_relationship","11",	"null",	"null",""+transformations,"object_id","subject_id","null","null","null"};
			String [] third =   {dataset2,"m","feature","exported","subject_id","feature","11","null","null",	""+transformations,"feature_id",	"feature_id,uniquename,type_id","null,exon_name,exon_type_id","null","null"};
			String [] fourth =  {dataset2,"m","feature","exported","feature_id","featureloc","11","null",	"null",""+transformations,"feature_id","fmin,fmax,strand,srcfeature_id,rank","exon_start,exon_end,null,null,null","null","null","null","null"};
			String [] fifth =   {dataset2,"m","feature","exported","srcfeature_id","feature","11","null",	"null",""+transformations,"feature_id","name,uniquename","chromosome_acc,chromosome","null","null"};
			String [] sixth =   {dataset2,"m","feature","imported","cvterm_id","cvterm","n1","null","null",""+transformations,"exon_type_id","name","exon_coding_type","null","null"};
			
			String [] [] one = {first,second,third,fourth,fifth,sixth};
			
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
		
	
