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

package org.biomart.builder.view.gui.diagrams.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.common.model.PartitionTable;
import org.biomart.common.resources.Resources;

/**
 * A diagram component that represents a partition table. It usually only has a
 * label in it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class PartitionTableComponent extends BoxShapedComponent {

	private static final long serialVersionUID = 1;

	private static Font BOLD_FONT = Font.decode("SansSerif-BOLD-10");

	/**
	 * The normal background for a partition table component.
	 */
	public static Color NORMAL_BACKGROUND = Color.YELLOW;

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	/**
	 * Constructs a partition table diagram component in the given diagram that
	 * displays details of a particular partition table.
	 * 
	 * @param partition
	 *            the schema to display details of.
	 * @param diagram
	 *            the diagram to display the details in.
	 */
	public PartitionTableComponent(final PartitionTable partition,
			final Diagram diagram) {
		super(partition, diagram);

		// Schema components are set out in a vertical list.
		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		// Constraints for each sub-component.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(5, 5, 5, 5);

		// Calculate the components and add them to the list.
		this.recalculateDiagramComponent();
	}

	private PartitionTable getPartitionTable() {
		return (PartitionTable) this.getObject();
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() >= 2) {
			final int index = PartitionTableComponent.this.getDiagram()
					.getMartTab().getPartitionTableTabSet().indexOfTab(
							PartitionTableComponent.this.getPartitionTable()
									.getName());
			PartitionTableComponent.this.getDiagram().getMartTab()
					.getPartitionTableTabSet().setSelectedIndex(index);
			// Mark as handled.
			eventProcessed = true;
		}
		// Pass it on up if we're not interested.
		if (!eventProcessed)
			super.processMouseEvent(evt);
	}

	public JPopupMenu getContextMenu() {
		// First of all, work out what would have been shown by default.
		final JPopupMenu contextMenu = super.getContextMenu();

		// Add a divider if necessary.
		if (contextMenu.getComponentCount() > 0)
			contextMenu.addSeparator();

		// Add the 'show tables' option, which opens the tab representing
		// this partition.
		final JMenuItem showDetails = new JMenuItem(Resources
				.get("showDetailsTitle"));
		showDetails.setMnemonic(Resources.get("showDetailsMnemonic").charAt(0));
		showDetails.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final int index = PartitionTableComponent.this.getDiagram()
						.getMartTab().getPartitionTableTabSet().indexOfTab(
								PartitionTableComponent.this
										.getPartitionTable().getName());
				PartitionTableComponent.this.getDiagram().getMartTab()
						.getPartitionTableTabSet().setSelectedIndex(index);
			}
		});
		contextMenu.add(showDetails);

		// Add a separator.
		contextMenu.addSeparator();

		// Add an option to rename this dataset.
		final JMenuItem rename = new JMenuItem(Resources
				.get("renamePartitionTableTitle"));
		rename.setMnemonic(Resources.get("renamePartitionTableMnemonic")
				.charAt(0));
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				PartitionTableComponent.this.getDiagram().getMartTab()
						.getPartitionTableTabSet().requestRenamePartitionTable(
								PartitionTableComponent.this
										.getPartitionTable());
			}
		});
		contextMenu.add(rename);

		// Add an option to replicate this dataset.
		final JMenuItem replicate = new JMenuItem(Resources
				.get("replicatePartitionTableTitle"));
		replicate.setMnemonic(Resources.get("replicatePartitionTableMnemonic")
				.charAt(0));
		replicate.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				PartitionTableComponent.this.getDiagram().getMartTab()
						.getPartitionTableTabSet()
						.requestReplicatePartitionTable(
								PartitionTableComponent.this
										.getPartitionTable());
			}
		});
		contextMenu.add(replicate);

		// Option to remove the dataset from the mart.
		final JMenuItem remove = new JMenuItem(Resources
				.get("removePartitionTableTitle"), new ImageIcon(Resources
				.getResourceAsURL("cut.gif")));
		remove.setMnemonic(Resources.get("removePartitionTableMnemonic")
				.charAt(0));
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				PartitionTableComponent.this.getDiagram().getMartTab()
						.getPartitionTableTabSet().requestRemovePartitionTable(
								PartitionTableComponent.this
										.getPartitionTable());
			}
		});
		contextMenu.add(remove);

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
	}

	public void recalculateDiagramComponent() {
		// Remove all our components.
		this.removeAll();

		// Add the label for the dataset name,
		final JTextField name = new JTextField();
		name.setFont(PartitionTableComponent.BOLD_FONT);
		this.setRenameTextField(name);
		this.layout.setConstraints(name, this.constraints);
		this.add(name);
	}

	public void performRename(final String newName) {
		this.getDiagram().getMartTab().getPartitionTableTabSet()
				.requestRenamePartitionTable(this.getPartitionTable(), newName);
	}

	public String getName() {
		return this.getPartitionTable().getName();
	}
}
