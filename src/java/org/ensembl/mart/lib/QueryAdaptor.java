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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.ensembl.mart.lib;

import javax.sql.DataSource;

import org.ensembl.mart.lib.config.DatasetConfig;

/**
 * An abstract adapter class for receiving query events.
 * The methods in this class are empty. This class exists as
 * convenience for creating listener objects.
 * <P>
 * Extend this class to create a <code>Query</code> listener 
 * and override the methods for the events of interest.
 * </P>
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * 
 * @see org.ensembl.mart.lib.QueryChangeListener
 */
public abstract class QueryAdaptor implements QueryListener {

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#queryNameChanged(org.ensembl.mart.lib.Query, java.lang.String, java.lang.String)
   */
  public void queryNameChanged(
    Query sourceQuery,
    String oldName,
    String newName) {
  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#datasetChanged(org.ensembl.mart.lib.Query, java.lang.String, java.lang.String)
   */
  public void datasetChanged(
    Query source,
    String oldDataset,
    String newDataset) {
    

  }

  /* (non-Javadoc)
   * @see org.ensembl.mart.lib.QueryChangeListener#datasourceChanged(org.ensembl.mart.lib.Query, javax.sql.DataSource, javax.sql.DataSource)
   */
  public void datasourceChanged(
    Query sourceQuery,
    DataSource oldDatasource,
    DataSource newDatasource) {
    

  }

  /*
   * @see org.ensembl.mart.lib.QueryChangeListener#attributeAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void attributeAdded(
    Query sourceQuery,
    int index,
    Attribute attribute) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#attributeRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Attribute)
   */
  public void attributeRemoved(
    Query sourceQuery,
    int index,
    Attribute attribute) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#filterAdded(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void filterAdded(Query sourceQuery, int index, Filter filter) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#filterRemoved(org.ensembl.mart.lib.Query, int, org.ensembl.mart.lib.Filter)
   */
  public void filterRemoved(Query sourceQuery, int index, Filter filter) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#filterChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.Filter, org.ensembl.mart.lib.Filter)
   */
  public void filterChanged(
    Query sourceQuery,
    int index,
    Filter oldFilter,
    Filter newFilter) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#sequenceDescriptionChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.SequenceDescription, org.ensembl.mart.lib.SequenceDescription)
   */
  public void sequenceDescriptionChanged(
    Query sourceQuery,
    SequenceDescription oldSequenceDescription,
    SequenceDescription newSequenceDescription) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#limitChanged(org.ensembl.mart.lib.Query, int, int)
   */
  public void limitChanged(Query query, int oldLimit, int newLimit) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#starBasesChanged(org.ensembl.mart.lib.Query, java.lang.String[], java.lang.String[])
   */
  public void starBasesChanged(
    Query sourceQuery,
    String[] oldStarBases,
    String[] newStarBases) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#primaryKeysChanged(org.ensembl.mart.lib.Query, java.lang.String[], java.lang.String[])
   */
  public void primaryKeysChanged(
    Query sourceQuery,
    String[] oldPrimaryKeys,
    String[] newPrimaryKeys) {
    

  }

  /**
   * @see org.ensembl.mart.lib.QueryChangeListener#datasetConfigChanged(org.ensembl.mart.lib.Query, org.ensembl.mart.lib.config.DatasetConfig, org.ensembl.mart.lib.config.DatasetConfig)
   */
  public void datasetConfigChanged(
    Query query,
    DatasetConfig oldDatasetConfig,
    DatasetConfig newDatasetConfig) {
    

  }

}
