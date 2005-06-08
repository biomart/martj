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
     * @return Returns the seqDataSource.
     */
    public DetailedDataSource getSeqDataSource() {
        return seqDataSource;
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
      
      seqDataSource = seqDataset.getAdaptor().getDataSource();
      seqInfo = seqDataset.getOptionalParameter();
	}

	private void addSeqConfig() throws InvalidQueryException {
	      String[] pts = seqDescription.split("\\."); //split on the period to get the dataset and seqType
	      String seqDatasetName = pts[0];
	      seqType = pts[1];
	      
	      try {
	          seqDataset = adaptor.getDatasetConfigByDatasetInternalName(seqDatasetName, "default"); //assume default for now
	      } catch (ConfigurationException e) {
	          throw new InvalidQueryException("Could not get Sequence Dataset for " + seqDatasetName + "\n", e);
	      }
	}
	
	/**
	 * Copy constructor.
	 * @param o - a SequenceDescription object
	 */
	public SequenceDescription(SequenceDescription o) {
	    seqDescription = o.getSeqDescription();
	    adaptor = o.getAdaptor();
	    seqDataSource = o.getSeqDataSource();
	    seqInfo = o.getSeqInfo();
	    leftFlank = o.getLeftFlank();
	    rightFlank = o.getRightFlank();
	    structDataset = o.getStructDataset();
	    visibleDataset = o.getVisibleDataset();
	    seqDataset = o.getSeqDataset();
		seqType = o.getSeqType();
		structDatasetName = o.getStructDatasetName();
	    structDataSource = o.getStructDataSource();
	    exportable = o.getExportable();
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
        String[] structureAttInfo = attribute.getField().split("\\.");
        String structureAttName = structureAttInfo[1];
        
        if (structDataset == null) {
            try {
              structDatasetName = structureAttInfo[0];
              structDataset = adaptor.getDatasetConfigByDatasetInternalName(structDatasetName, "default");
              structDataSource = structDataset.getAdaptor().getDataSource();
              
              if (subQuery != null)
                  initializeSubQuery();
            } catch (ConfigurationException e) {
              throw new InvalidQueryException("Could not determine sequence information\n", e);
            }
        }
        AttributeDescription structAttD = structDataset.getAttributeDescriptionByInternalName(structureAttName);
        
        if (structAttD == null)
            throw new InvalidQueryException("Could not get attribute " + structureAttName + " from " + structDataset.getDisplayName() + "\n");
        
        return new FieldAttribute(structAttD.getField(), structAttD.getTableConstraint(), structAttD.getKey());
    }
    
    public IDListFilter getFilter(Filter filter) throws InvalidQueryException {
        //as new filters come through here, the IDListFilter returned will be different each time
        subQuery.addFilter(filter);
        return getSubQueryFilter();
    }
    
    private IDListFilter getSubQueryFilter() throws InvalidQueryException {
        Importable imp = structDataset.getImportables()[0]; //there is only one
        FilterDescription structFilter = structDataset.getFilterDescriptionByInternalName(imp.getFilters());
        if (structFilter == null)
            throw new InvalidQueryException("Could not run sequence query\n");
        
        return new IDListFilter(structFilter.getField(), 
                structFilter.getTableConstraint(), 
                structFilter.getKey(), 
                subQuery);
    }
    
    public void setSubQuery(Query query) throws InvalidQueryException {
        try {
            String qDataset = query.getDataset();
            visibleDataset = adaptor.getDatasetConfigByDatasetInternalName(qDataset, "default");
        } catch (ConfigurationException e) {
           throw new InvalidQueryException("Could not get Configuration for " + query.getDataset() + "\n", e);
        }
        
        subQuery = new Query();
        subQuery.setDataset(visibleDataset.getDataset());
        subQuery.setDatasetConfig(visibleDataset);
        subQuery.setDataSource(visibleDataset.getAdaptor().getDataSource());
        subQuery.setMainTables(visibleDataset.getStarBases());
        subQuery.setPrimaryKeys(visibleDataset.getPrimaryKeys());
        
        if (structDataset != null)
          initializeSubQuery();
    }

    private void initializeSubQuery() throws InvalidQueryException {
        //must find the structureDataset Exportable with linkName == seqType
        Exportable[] structExps = structDataset.getExportables();
        
        for (int i = 0, n = structExps.length; i < n; i++) {
            if (structExps[i].getLinkName().equals(seqType)) {
                //this is the one we need. Split its attributes and add them to exportable
                String[] attNames = structExps[i].getAttributes().split("\\,");
                exportable = new Attribute[attNames.length];
                
                for (int j = 0, m = attNames.length; j < m; j++) {
                    AttributeDescription att = structDataset.getAttributeDescriptionByInternalName(
                            attNames[j]);
                    
                    exportable[j] = new FieldAttribute(att.getField(),
                                                       att.getTableConstraint(),
                                                       att.getKey());
                }
                break;
            }
        }
        
        if (exportable == null)
            throw new InvalidQueryException("Sequence type " + seqType + " does not appear to be supported\n");
        
        Importable imp = structDataset.getImportables()[0]; //there is only one
        Exportable[] exps = visibleDataset.getExportables();
        Exportable exp = null;
        for (int i = 0, n = exps.length; i < n; i++) {
            if (exps[i].getLinkName().equals(imp.getLinkName())) {
                //ignoring version, come back if bugs
                exp = exps[i];
                break;
            }
        }
        
        if (exp == null)
            throw new InvalidQueryException("Could not run sequence query\n");
        
        //note, in this case, the exportable will only have one attribute
        AttributeDescription visAtt = visibleDataset.getAttributeDescriptionByInternalName( exp.getAttributes() );
        
        if (visAtt == null)
            throw new InvalidQueryException("Could not run sequence query\n");
        
        subQuery.addAttribute(
                new FieldAttribute(visAtt.getField(), visAtt.getTableConstraint(), visAtt.getKey())
        );
    }
    
    public String getStructDatasetName() {
        return structDatasetName;
    }
    
    public DatasetConfig getStructDataset() {
        return structDataset;
    }
    
    public DetailedDataSource getStructDataSource() {
        return structDataSource;
    }
    
    public String[] getStructureMainTables() {
        return structDataset.getStarBases();
    }
    
    public String[] getStructurePrimaryKeys() {
        return structDataset.getPrimaryKeys();
    }
    
    public Attribute[] getExportable() {
        return exportable;
    }
    
    public Query getSubQuery() {
        return subQuery;
    }
    
	public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        buf.append(" seqDescription=").append(seqDescription);
        buf.append(", seqInfo=").append(seqInfo);
        buf.append(", seqDataSource=").append(seqDataSource);
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
		tmp = (31 * tmp) + seqDataSource.hashCode();
		tmp = (31 * tmp) + leftFlank;
		tmp = (31 * tmp) + rightFlank;
		tmp = (31 * tmp) + seqInfo.hashCode();
	  return tmp;	
	}

    /**
     * @return Returns the seqDataset.
     */
    public DatasetConfig getSeqDataset() {
        return seqDataset;
    }
    
    /**
     * @return Returns the visibleDataset.
     */
    public DatasetConfig getVisibleDataset() {
        return visibleDataset;
    }
    
	private DatasetConfig structDataset, visibleDataset, seqDataset;
	private String seqDescription, seqInfo, seqType, structDatasetName;
    private DSConfigAdaptor adaptor;
    private DetailedDataSource seqDataSource, structDataSource;
	private int leftFlank = 0;
	private int rightFlank = 0;
	private Attribute[] exportable;
	Query subQuery;
	
	private Logger logger = Logger.getLogger(SequenceDescription.class.getName());
}
