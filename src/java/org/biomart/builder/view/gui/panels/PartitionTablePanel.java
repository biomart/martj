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

package org.biomart.builder.view.gui.panels;

import java.awt.Color;

import javax.swing.JPanel;

import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.common.model.PartitionTable;

/**
 * Displays the contents of a partition table.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.7
 */
public class PartitionTablePanel extends JPanel {
	private static final long serialVersionUID = 1;

	/**
	 * The color we use for our background.
	 */
	public static final Color BACKGROUND_COLOUR = Color.PINK;
	
	private final PartitionTable partitionTable;

	private final MartTab martTab;

	/**
	 * Creates a new panel that displays the partition table definition.
	 * 
	 * @param martTab
	 *            the tab within which this diagram appears.
	 * @param partitionTable
	 *            the partition table to draw in this diagram.
	 */
	public PartitionTablePanel(final MartTab martTab,
			final PartitionTable partitionTable) {
		// Set our background color.
		this.setBackground(PartitionTablePanel.BACKGROUND_COLOUR);
		// Remember the schema, then lay it out.
		this.martTab = martTab;
		this.partitionTable = partitionTable;
		this.recalculatePanel();
	}

	private void recalculatePanel() {
		// First of all, remove all our existing components.
		this.removeAll();

		// TODO Add stuff.
	}

	/**
	 * Find out what mart tab we are in.
	 * 
	 * @return the mart tab.
	 */
	public MartTab getMartTab() {
		return this.martTab;
	}

	/**
	 * Returns the partition table that this diagram represents.
	 * 
	 * @return the partition table this diagram represents.
	 */
	public PartitionTable getPartitionTable() {
		return this.partitionTable;
	}
}
