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

import javax.swing.JLabel;

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.DSAttributeGroup;

/**
 * Widget for selecting sequence attributes.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 *
 */
public class SequenceGroupWidget extends GroupWidget {

  private DSAttributeGroup attributeGroup;

  /**
   * @param name
   * @param query
   * @param tree
   */
  public SequenceGroupWidget(String name, Query query, QueryTreeView tree,DSAttributeGroup attributeGroup) {
    super(name, query, tree);
    this.attributeGroup = attributeGroup;
      
    add(new JLabel("Sequence stuff"));

  }

  public static void main(String[] args) {
  }
}
