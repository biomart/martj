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

import java.util.Iterator;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.SchemaLayoutManager.SchemaLayoutConstraint;
import org.biomart.builder.view.gui.diagrams.components.DataSetComponent;

/**
 * This diagram draws a {@link DataSetComponent} for each dataset in a mart.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class AllDataSetsDiagram extends Diagram {
	private static final long serialVersionUID = 1;

	/**
	 * The constructor creates the diagram and associates it with a given mart
	 * tab.
	 * 
	 * @param martTab
	 *            the mart tab to associate with this diagram. It will be used
	 *            to work out who receives all user menu events, etc.
	 */
	public AllDataSetsDiagram(final MartTab martTab) {
		super(new SchemaLayoutManager(), martTab);

		// Calculate the diagram.
		this.recalculateDiagram();
	}

	public void doRecalculateDiagram() {
		// Remove all existing components.
		this.removeAll();

		// Add a DataSetComponent for each dataset.
		for (final Iterator i = this.getMartTab().getMart().getDataSets()
				.iterator(); i.hasNext();) {
			final DataSet ds = (DataSet) i.next();
			final DataSetComponent dsComponent = new DataSetComponent(ds, this);
			this.add(dsComponent, new SchemaLayoutConstraint(0));
		}

		// Resize the diagram to fit the components.
		this.resizeDiagram();
	}
}
