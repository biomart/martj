
package org.ensembl.mart.explorer;

import java.util.logging.Logger;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.FilterPage;

/**
 * This is a GUI representation of a
 * the FilterPage it is created from.
 * It contains a FilterGroupWidget corresponding
 * to each of the elements filterPage.getFilterGroups().
 */
public class FilterPageWidget extends PageWidget {

  private final static Logger logger =
    Logger.getLogger(FilterPageWidget.class.getName());

  
  /**
   * @param query model
   * @param name name of this page
   * @param filterPage source object this instance represents
   */
  public FilterPageWidget(Query query, String name, FilterPage filterPage) {
    super(name, query);
    // TODO :1 create and filter group widgets. see AttributeFilterPage
  }

  



}
