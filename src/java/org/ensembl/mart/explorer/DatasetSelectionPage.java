/*
 * Created on Aug 4, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.ensembl.mart.explorer;

import javax.swing.JLabel;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.MartConfiguration;

/**
 * @author craig
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DatasetSelectionPage extends InputPage {

  /**
   * @param query
   * @param config
   */
  public DatasetSelectionPage(Query query, MartConfiguration config) {
    super("DatasetSelectionPage", query);

    add(new JLabel("TODO - " + getClass().getName()));
  }

  private MartConfiguration config;

}
