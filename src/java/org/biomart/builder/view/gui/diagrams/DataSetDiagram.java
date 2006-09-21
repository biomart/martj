/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.builder.view.gui.diagrams;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * Displays the contents of a dataset within a standard diagram object. It adds
 * a series of {@link TableComponent} and {@link RelationComponent} objects when
 * the diagram is recalculated, and treats the schema object it represents as
 * the basic background object of the diagram.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author$
 * @since 0.1
 */
public class DataSetDiagram extends SchemaDiagram {
	private static final long serialVersionUID = 1;

	/**
	 * Creates a new diagram that displays the tables and relations inside a
	 * specific dataset.
	 * 
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param dataset
	 *            the dataset to draw in this diagram.
	 */
	public DataSetDiagram(final MartTab martTab, final DataSet dataset) {
		// Call the general diagram constructor first.
		super(new RadialLayout(), martTab, dataset);
	}

	/**
	 * Returns the dataset that this diagram represents.
	 * 
	 * @return the dataset this diagram represents.
	 */
	public DataSet getDataSet() {
		return (DataSet) this.getSchema();
	}
}
