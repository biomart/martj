/*
	Copyright (C) 2003 EBI, GRL

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
 
 package org.ensembl.mart.explorer.config;

/**
 * Factory object to create a MartConfiguration object.
 * The xml configuration file for a specific mart is contained within
 *  the _meta_configuration table in the xml blob field.  In addition
 *  an XMLSchema for this xml file is contained within the data
 *  directory of the mart-explorer distribution.  The factory pulls
 *  the xml configuration data as a Stream from the database,
 *  and parses it to create the MartConfiguration object.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public class MartConfigurationFactory {

  /*
   * Currently designed to return a MartConfiguration specifically for
   *  dl_testmart_newnames, with Datasets with only the starBases and primaryKeys defined.
   * Need to impliment the system when the xml has been generated.
   */
  public static MartConfiguration getInstance() throws ConfigurationException {
  	//TODO change dl_testmart_newnames to ensembl_mart_14_1 when it is released
  	//TODO impliment xml system
  	String martName = "dl_testmart_newnames";
  	MartConfiguration martconf = new MartConfiguration(martName);
  	
  	String[] keys = new String[] {"gene_id", "transcript_id"}; // always the same
  	
  	String displayName = "Homo sapiens Ensembl Genes";
  	String dmartName = "hsapiens_ensemblgene";
  	String[] stars = new String[] {"hsapiens_ensemblgene", "hsapiens_ensembltranscript"} ;
  	Dataset d = new Dataset(dmartName, displayName);
  	d.setStars(stars);
  	d.setPrimaryKeys(keys);
  	martconf.addDataset(d);
  	
  	displayName = "Homo sapiens Est Genes";
		dmartName = "hsapiens_estgene";
  	stars = new String[] {"hsapiens_estgene", "hsapiens_esttranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
  	
  	displayName = "Homo sapiens Vega Genes";
		dmartName = "hsapiens_vegagene";
  	stars = new String[] {"hsapiens_vegagene", "hsapiens_vegatranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
  	
  	displayName = "Mus musculus Ensembl Genes";
		dmartName = "mmusculus_ensemblgene";
		stars = new String[] {"mmusculus_ensemblgene", "mmusculus_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Mus musculus Est Genes";
		dmartName = "mmusculus_estgene";		
		stars = new String[] {"mmusculus_estgene", "mmusculus_esttranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Rattus norvegicus Ensembl Genes";
		dmartName = "rnorvegicus_ensemblgene";				
		stars = new String[] {"rnorvegicus_ensemblgene", "rnorvegicus_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Rattus norvegicus Est Genes";		
		dmartName = "rnorvegicus_estgene";
		stars = new String[] {"rnorvegicus_estgene", "rnorvegicus_esttranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Fugu rubripes Ensembl Genes";
		dmartName = "fugu_ensemblgene";
		stars = new String[] {"frubripes_ensemblgene", "frubripes_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
				
		displayName = "Anopheles gambiae Ensembl Genes";
		dmartName = "agambiae_ensemblgene";
		stars = new String[] {"agambiae_ensemblgene", "agambiae_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
				
		displayName = "Anopheles gambiae EST Genes";
		dmartName = "agambiae_estgene";
		stars = new String[] {"agambiae_estgene", "agambiae_esttranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Drosophila melanogaster Ensembl Genes";
		dmartName = "dmelanogaster_ensemblgene";
		stars = new String[] {"dmelanogaster_ensemblgene", "dmelanogaster_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Danio rerio Ensembl Genes";
		dmartName = "drerio_ensemblgene";
		stars = new String[] {"drerio_ensemblgene", "drerio_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Caenorhabditis briggsae Ensembl Genes";
		dmartName = "cbriggsae_ensemblgene";
		stars = new String[] {"cbriggsae_ensemblgene", "cbriggsae_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Caenorhabditis briggsae Est Genes";
		dmartName = "cbriggsae_estgene";
		stars = new String[] {"cbriggsae_estgene", "cbriggsae_esttranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
		
		displayName = "Caenorhabditis elegans Ensembl Genes";
		dmartName = "celegans_ensemblgene";
		stars = new String[] {"celegans_ensemblgene", "celegans_ensembltranscript"};
		d = new Dataset(dmartName, displayName);
		d.setStars(stars);
		d.setPrimaryKeys(keys);
		martconf.addDataset(d);
				
  	return martconf;
  }
}
