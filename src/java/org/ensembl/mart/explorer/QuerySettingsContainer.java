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

package org.ensembl.mart.explorer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;

/**
 * Contains multiple cards (JComponents) that are used to set the query but only shows one at a time. Each 
 * can each be brought to the front by a key name. Similar to card layout.
 * Also responds to certain query changes.
 * 
 * <p>TODO handle ambiguous attributes/filters selected(select all) and in loaded queries(warn). 
 */
public class QuerySettingsContainer
  extends JPanel
  implements PropertyChangeListener {

  private DSViewAdaptor dsvAdaptor;

  private Query query;

  public QuerySettingsContainer(Query query, DSViewAdaptor dsvAdaptor) {
    this.query = query;
    this.dsvAdaptor = dsvAdaptor;
    query.addPropertyChangeListener(this);
  }

  /**
   * TODO Test case
   */
  public static void main(String[] args) {
  }

  /**
   * TODO Respond to changes in query.
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {

  }

  /*
   * TODO Reset the container; remove all current dsv specific cards e.g. attribute and
   * filter pages and create and add new ones.
   */
  private synchronized void setDatasetView(DatasetView dsv) {

  }

  /**
   * TODO Bring card with specified cardName to front.
   * @param cardName
   */
  public synchronized void toFront(String cardName) {

  }

  /**
   * Bring card corresponding to node to front.
   *
   */
  private void treeNodeSelected() {

  }
}
