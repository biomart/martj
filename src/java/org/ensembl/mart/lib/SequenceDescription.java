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

package org.ensembl.mart.lib;

import java.util.logging.Logger;

import org.ensembl.mart.lib.config.AttributeDescription;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSConfigAdaptor;
import org.ensembl.mart.lib.config.DatasetConfig;
import org.ensembl.mart.lib.config.Exportable;
import org.ensembl.mart.lib.config.FilterDescription;
import org.ensembl.mart.lib.config.Importable;


/**
 * Contains the knowledge for creating the different sequences
 * that Mart is able to generate.
 * 
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 */
public final class SequenceDescription {
	
    /**
     * @return Returns the adaptor.
     */
    public DSConfigAdaptor getAdaptor() {
        return adaptor;
    }
    /**
     * @return Returns the refDataSource.
     */
    public DetailedDataSource getRefDataSource() {
        return refDataSource;
    }
    /**
     * @return Returns the seqDescription.
     */
    public String getSeqDescription() {
        return seqDescription;
    }
    /**
     * @return Returns the seqInfo.
     */
    public String getSeqInfo() {
        return seqInfo;
    }
    /**
	 * default constructor private, so SequeceDescription cannot be subclassed
	 */
	private SequenceDescription() {
	}
	
	/**
	 * Construct a SequenceDescription of a specified type.
	 * type can be one of the static sequence types.
	 * This is useful for types that cannot have
	 * their flanking sequences extended.
	 * 
	 * @param type int
	 */
	public SequenceDescription(String seqDescription, DSConfigAdaptor adaptor) throws InvalidQueryException {
		this(seqDescription, adaptor, 0, 0);
	}
	
	/**
	 * Construct a fully qualified SequenceDescription with all
	 * necessary information.
	 * 
	 * @param type Must be one of the static enums
	 * @param leftFlank length of left flanking sequence to append to the resulting sequences
	 * @param rightFlank length of right flanking sequence to append to the resulting sequences
	 * @throws InvalidQueryException if the client requests an unknown sequence type,
	 *         or requests flanking sequence for a sequence which is not applicable for flanking sequence.
	 */
	public SequenceDescription(String seqDescription, DSConfigAdaptor adaptor, int lflank, int rflank) throws InvalidQueryException {
      this.seqDescription = seqDescription;
      this.adaptor = adaptor;
      this.leftFlank = lflank;
      this.rightFlank = rflank;
      
      addSeqConfig();
      
      refDataSource = refDataset.getAdaptor().getDataSource();
      seqInfo = refDataset.getOptionalParameter();
	}

	private void addSeqConfig() throws InvalidQueryException {
	      String[] pts = seqDescription.split("\\."); //split on the period to get the dataset and seqType
	      String seqDatasetName = pts[0];
	      seqType = pts[1];
	      
	      try {
	          refDataset = adaptor.getDatasetConfigByDatasetInternalName(seqDatasetName, "default"); //assume default for now
	      } catch (ConfigurationException e) {
	          throw new InvalidQueryException("addSeqConfig: Could not get Sequence Dataset for " + seqDatasetName + "\n", e);
	      }
	}
	
	/**
	 * Copy constructor.
	 * @param o - a SequenceDescription object
	 */
	public SequenceDescription(SequenceDescription o) {
	    seqDescription = o.getSeqDescription();
	    adaptor = o.getAdaptor();
	    refDataSource = o.getRefDataSource();
	    seqInfo = o.getSeqInfo();
	    leftFlank = o.getLeftFlank();
	    rightFlank = o.getRightFlank();
	    finalDataset = o.getFinalDataset();
	    intermediateDataset = o.getIntermediateDataset();
	    refDataset = o.getRefDataset();
		seqType = o.getSeqType();
		finalDatasetName = o.getFinalDatasetName();
	    finalDataSource = o.getFinalDataSource();
	    finalLink = o.getFinalLink();
		subQuery = o.getSubQuery();
	}
	
    /**
     * Returns the length of the left flank
     * 
     * @return int leftFlank
     */
    public int getLeftFlank() {
    	return leftFlank;
    }
    
    /**
     * Returns the length of the right flank
     * 
     * @return int rightFlank
     */
    public int getRightFlank() {
    	return rightFlank;
    }

    public String getSeqType() {
        return seqType;
    }
    
    public Attribute getAttribute(Attribute attribute) throws InvalidQueryException {
        if (attribute.getField().indexOf('.') < 1)
            return attribute;
        
        String[] structureAttInfo = attribute.getField().split("\\.");
        String structureAttName = structureAttInfo[1];
        
        if (finalDataset == null) {
            try {
              finalDatasetName = structureAttInfo[0];
              
              if (finalDatasetName.equals(refDataset.getDataset())) {
                  finalDataset = refDataset;
                  finalDataSource = refDataSource;
              } else {
                finalDataset = adaptor.getDatasetConfigByDatasetInternalName(finalDatasetName, "default");
                finalDataSource = finalDataset.getAdaptor().getDataSource();
              }
              
              if (subQuery != null)
                  initializeSubQuery();
            } catch (ConfigurationException e) {
              throw new InvalidQueryException("getAttribute: Could not determine sequence information\n", e);
            }
        }
        
        AttributeDescription structAttD = finalDataset.getAttributeDescriptionByInternalName(structureAttName);
        
        if (structAttD == null)
            throw new InvalidQueryException("getAttribute: Could not get attribute " + structureAttName + " from " + finalDataset.getDisplayName() + "\n");
        
        return new FieldAttribute(structAttD.getField(), structAttD.getTableConstraint(), structAttD.getKey());
    }
    
    public IDListFilter getFilter(Filter filter) throws InvalidQueryException {
        //for non structure intermediate queries, return null
        if (finalDataset == null)
          return null;
        
        //as new filters come through here, the IDListFilter returned will be different each time
        subQuery.addFilter(filter);
        return getSubQueryFilter();
    }

    private Importable getReferenceImportable() throws InvalidQueryException {
        Importable[] imps = refDataset.getImportables();
        Importable imp = null;
        for (int i = 0, n = imps.length; i < n; i++) {
            Importable importable = imps[i];
            if (importable.getLinkName().equals(seqType)) {
                imp = importable;
                break;
            }
        }
        
        if (imp == null)
            throw new InvalidQueryException("getReferenceImportable: Sequence " + seqType + " is not supported\n");
        
        return imp;
    }
    
    private Importable getFinalImportable() throws InvalidQueryException {
        Importable[] imps = finalDataset.getImportables();
        Importable imp = null;
        
        if (finalDataset == refDataset) {
            for (int i = 0, n = imps.length; i < n; i++) {
                if (imps[i].getLinkName().equals(seqType)) {
                    imp = imps[i];
                    break;
                }
            }
        } else
            imp = imps[0]; //there is only one
        
        if (imp == null)
            throw new InvalidQueryException("getFinalImportable: Could not get Importable for " + seqType + " from " + finalDatasetName + "\n");
        
        return imp;
    }
    
    private IDListFilter getSubQueryFilter() throws InvalidQueryException {
        FilterDescription finalListFilter = finalDataset.getFilterDescriptionByInternalName(getFinalImportable().getFilters());
        if (finalListFilter == null)
            throw new InvalidQueryException("getSubQueryFilter: Could not run sequence query\n");
        
        return new IDListFilter(finalListFilter.getField(), 
                finalListFilter.getTableConstraint(), 
                finalListFilter.getKey(), 
                subQuery);
    }
    
    public void setSubQuery(Query query) throws InvalidQueryException {
        try {
            String qDataset = query.getDataset();
            intermediateDataset = adaptor.getDatasetConfigByDatasetInternalName(qDataset, "default");
        } catch (ConfigurationException e) {
           throw new InvalidQueryException("setSubQuery: Could not get Configuration for " + query.getDataset() + "\n", e);
        }
        
        subQuery = new Query();
        subQuery.setDataset(intermediateDataset.getDataset());
        subQuery.setDatasetConfig(intermediateDataset);
        subQuery.setDataSource(intermediateDataset.getAdaptor().getDataSource());
        subQuery.setMainTables(intermediateDataset.getStarBases());
        subQuery.setPrimaryKeys(intermediateDataset.getPrimaryKeys());
    }

    private void initializeSubQuery() throws InvalidQueryException {
        //must find the finalLink
        if (finalDataset == refDataset) {
            //use intermediate exportable <-> final importable
            Exportable[] intExps = intermediateDataset.getExportables();
            Importable finalImp = getFinalImportable();
            for (int i = 0, n = intExps.length; i < n; i++) {
                Exportable exportable = intExps[i];
                if (exportable.getLinkName().equals(finalImp.getLinkName())) {
                    //this is the one we need. Split its attributes and add them to finalLink
                    String[] attNames = intExps[i].getAttributes().split("\\,");
                    finalLink = new Attribute[attNames.length];
                    
                    for (int j = 0, m = attNames.length; j < m; j++) {
                        AttributeDescription att = intermediateDataset.getAttributeDescriptionByInternalName(
                                attNames[j]);
                        
                        finalLink[j] = new FieldAttribute(att.getField(),
                                att.getTableConstraint(),
                                att.getKey());
                    }
                    break;                    
                }
            }
        } else {
            //use final exportable <-> reference importable            
            Exportable[] finalExps = finalDataset.getExportables();
            Importable refImp = getReferenceImportable();
            
            for (int i = 0, n = finalExps.length; i < n; i++) {
                if (finalExps[i].getLinkName().equals(refImp.getLinkName())) {
                    //this is the one we need. Split its attributes and add them to finalLink
                    String[] attNames = finalExps[i].getAttributes().split("\\,");
                    finalLink = new Attribute[attNames.length];
                    
                    for (int j = 0, m = attNames.length; j < m; j++) {
                        AttributeDescription att = finalDataset.getAttributeDescriptionByInternalName(
                                attNames[j]);
                        
                        finalLink[j] = new FieldAttribute(att.getField(),
                                att.getTableConstraint(),
                                att.getKey());
                    }
                    break;
                }
            }
        }
        
        if (finalLink == null)
            throw new InvalidQueryException("InitializeSubQuery: Sequence type " + seqType + " does not appear to be supported\n");
        
        //must find the intermediate exportable <-> final importable link
        //and use it to add the link attribute to the subQuery
        Importable finalImportable = getFinalImportable();
        Exportable[] intermediateExportables = intermediateDataset.getExportables();
        Exportable exp = null;
        for (int i = 0, n = intermediateExportables.length; i < n; i++) {
            if (intermediateExportables[i].getLinkName().equals(finalImportable.getLinkName())) {
                //ignoring version, come back if bugs
                exp = intermediateExportables[i];
                break;
            }
        }
        
        if (exp == null)
            throw new InvalidQueryException("InitializeSubQuery: Could not run sequence query\n");
        
        //note, in this case, the finalLink will only have one attribute
        AttributeDescription visAtt = intermediateDataset.getAttributeDescriptionByInternalName( exp.getAttributes() );
        
        if (visAtt == null)
            throw new InvalidQueryException("InitializeSubQuery: Could not run sequence query\n");
        
        subQuery.addAttribute(
                new FieldAttribute(visAtt.getField(), visAtt.getTableConstraint(), visAtt.getKey())
        );
    }
    
    public String getFinalDatasetName() {
        return finalDatasetName;
    }
    
    public DatasetConfig getFinalDataset() {
        return finalDataset;
    }
    
    public DetailedDataSource getFinalDataSource() {
        return finalDataSource;
    }
    
    public String[] getStructureMainTables() {
        return finalDataset.getStarBases();
    }
    
    public String[] getStructurePrimaryKeys() {
        return finalDataset.getPrimaryKeys();
    }
    
    public Attribute[] getFinalLink() {
        //for sequence queries not involving a structure intermediate
        if (finalLink == null && intermediateDataset != null) {
            Exportable[] intermediateExps = intermediateDataset.getExportables();
            
            for (int i = 0, n = intermediateExps.length; i < n; i++) {
                if (intermediateExps[i].getLinkName().equals(seqType)) {
                    //this is the one we need. Split its attributes and add them to finalLink
                    String[] attNames = intermediateExps[i].getAttributes().split("\\,");
                    finalLink = new Attribute[attNames.length];
                    
                    for (int j = 0, m = attNames.length; j < m; j++) {
                        AttributeDescription att = intermediateDataset.getAttributeDescriptionByInternalName(
                                attNames[j]);
                        
                        finalLink[j] = new FieldAttribute(att.getField(),
                                                           att.getTableConstraint(),
                                                           att.getKey());
                    }
                    break;
                }
            }
        }
        
        return finalLink;
    }
    
    public Query getSubQuery() {
        return subQuery;
    }
    
	public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        buf.append(" seqDescription=").append(seqDescription);
        buf.append(", seqInfo=").append(seqInfo);
        buf.append(", refDataSource=").append(refDataSource);
        buf.append(", leftFlank=").append(leftFlank);
        buf.append(", rightFlank=").append(rightFlank);
        buf.append("]");
        
        return buf.toString();		
	}
	
	/**
	 * Allows Equality Comparison manipulation of SequenceDescription objects
	 */
	public boolean equals(Object o) {
		return o instanceof SequenceDescription && hashCode() == ((SequenceDescription) o).hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		int tmp = seqDescription.hashCode();
		tmp = (31 * tmp) + refDataSource.hashCode();
		tmp = (31 * tmp) + leftFlank;
		tmp = (31 * tmp) + rightFlank;
		if (seqInfo != null)
			tmp = (31 * tmp) + seqInfo.hashCode();
	  return tmp;	
	}

    /**
     * @return Returns the refDataset.
     */
    public DatasetConfig getRefDataset() {
        return refDataset;
    }
    
    /**
     * @return Returns the intermediateDataset.
     */
    public DatasetConfig getIntermediateDataset() {
        return intermediateDataset;
    }
    
	private DatasetConfig finalDataset, intermediateDataset, refDataset;
	private String seqDescription, seqInfo, seqType, finalDatasetName;
    private DSConfigAdaptor adaptor;
    private DetailedDataSource refDataSource, finalDataSource;
	private int leftFlank = 0;
	private int rightFlank = 0;
	private Attribute[] finalLink;
	Query subQuery;
	
	private Logger logger = Logger.getLogger(SequenceDescription.class.getName());
}
