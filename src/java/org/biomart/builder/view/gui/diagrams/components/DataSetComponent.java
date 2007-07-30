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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.view.gui.diagrams.Diagram;
import org.biomart.common.resources.Resources;

/**
 * A diagram component that represents a dataset. It usually only has a label in
 * it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.5
 */
public class DataSetComponent extends BoxShapedComponent {

	private static final long serialVersionUID = 1;

	private static final Font BOLD_FONT = Font.decode("SansSerif-BOLD-10");

	/**
	 * Background for invisible datasets.
	 */
	public static Color INVISIBLE_BACKGROUND = Color.WHITE;

	/**
	 * Background for visible datasets.
	 */
	public static Color VISIBLE_BACKGROUND = Color.YELLOW;

	/**
	 * Background for partition datasets.
	 */
	public static Color PARTITION_BACKGROUND = Color.RED;

	/**
	 * Background for masked datasets.
	 */
	public static Color MASKED_BACKGROUND = Color.LIGHT_GRAY;

	private GridBagConstraints constraints;

	private GridBagLayout layout;

	/**
	 * Constructs a dataset diagram component in the given diagram that displays
	 * details of a particular dataset.
	 * 
	 * @param dataset
	 *            the schema to display details of.
	 * @param diagram
	 *            the diagram to display the details in.
	 */
	public DataSetComponent(final DataSet dataset, final Diagram diagram) {
		super(dataset, diagram);

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

	private DataSet getDataSet() {
		return (DataSet) this.getObject();
	}

	protected void processMouseEvent(final MouseEvent evt) {
		boolean eventProcessed = false;
		// Is it a right-click?
		if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() >= 2
				&& !this.getDataSet().isMasked()) {
			final int index = DataSetComponent.this.getDiagram().getMartTab()
					.getDataSetTabSet().indexOfTab(
							DataSetComponent.this.getDataSet().getName());
			DataSetComponent.this.getDiagram().getMartTab().getDataSetTabSet()
					.setSelectedIndex(index);
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
		// this dataset.
		final JMenuItem showTables = new JMenuItem(Resources
				.get("showTablesTitle"));
		showTables.setMnemonic(Resources.get("showTablesMnemonic").charAt(0));
		showTables.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				final int index = DataSetComponent.this.getDiagram()
						.getMartTab().getDataSetTabSet().indexOfTab(
								DataSetComponent.this.getDataSet().getName());
				DataSetComponent.this.getDiagram().getMartTab()
						.getDataSetTabSet().setSelectedIndex(index);
			}
		});
		showTables.setEnabled(!this.getDataSet().isMasked());
		contextMenu.add(showTables);

		// Add a separator.
		contextMenu.addSeparator();

		// Add an option to rename this dataset.
		final JMenuItem rename = new JMenuItem(Resources
				.get("renameDataSetTitle"));
		rename.setMnemonic(Resources.get("renameDataSetMnemonic").charAt(0));
		rename.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				DataSetComponent.this.getDiagram().getMartTab()
						.getDataSetTabSet().requestRenameDataSet(
								DataSetComponent.this.getDataSet());
			}
		});
		contextMenu.add(rename);

		// Add an option to replicate this dataset.
		final JMenuItem replicate = new JMenuItem(Resources
				.get("replicateDataSetTitle"));
		replicate.setMnemonic(Resources.get("replicateDataSetMnemonic").charAt(
				0));
		replicate.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				DataSetComponent.this.getDiagram().getMartTab()
						.getDataSetTabSet().requestReplicateDataSet(
								DataSetComponent.this.getDataSet());
			}
		});
		contextMenu.add(replicate);

		// Option to remove the dataset from the mart.
		final JMenuItem remove = new JMenuItem(Resources
				.get("removeDataSetTitle"), new ImageIcon(Resources
				.getResourceAsURL("cut.gif")));
		remove.setMnemonic(Resources.get("removeDataSetMnemonic").charAt(0));
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				DataSetComponent.this.getDiagram().getMartTab()
						.getDataSetTabSet().requestRemoveDataSet(
								DataSetComponent.this.getDataSet());
			}
		});
		contextMenu.add(remove);

		// Separator
		contextMenu.addSeparator();

		// Invisible menu option.
		final JMenuItem invisible = new JCheckBoxMenuItem(Resources
				.get("invisibleDataSetTitle"));
		invisible.setMnemonic(Resources.get("invisibleDataSetMnemonic").charAt(
				0));
		invisible.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				if (invisible.isSelected())
					DataSetComponent.this.getDiagram().getMartTab()
							.getDataSetTabSet().requestInvisibleDataSet(
									DataSetComponent.this.getDataSet());
				else
					DataSetComponent.this.getDiagram().getMartTab()
							.getDataSetTabSet().requestVisibleDataSet(
									DataSetComponent.this.getDataSet());
			}
		});
		invisible.setSelected(this.getDataSet().isInvisible());
		contextMenu.add(invisible);

		// Masked menu option.
		final JMenuItem masked = new JCheckBoxMenuItem(Resources
				.get("maskedDataSetTitle"));
		masked.setMnemonic(Resources.get("maskedDataSetMnemonic").charAt(0));
		masked.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent evt) {
				DataSetComponent.this.getDiagram().getMartTab()
						.getDataSetTabSet().requestMaskDataSet(
								DataSetComponent.this.getDataSet(),
								masked.isSelected());
			}
		});
		masked.setSelected(this.getDataSet().isMasked());
		contextMenu.add(masked);

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
	}

	public void recalculateDiagramComponent() {
		// Remove all our components.
		this.removeAll();

		// Add the label for the dataset name,
		final JTextField name = new JTextField();
		name.setFont(DataSetComponent.BOLD_FONT);
		this.setRenameTextField(name);
		this.layout.setConstraints(name, this.constraints);
		this.add(name);
	}

	public void performRename(final String newName) {
		this.getDiagram().getMartTab().getDataSetTabSet().requestRenameDataSet(
				this.getDataSet(), newName);
	}

	public String getEditableName() {
		return this.getDataSet().getName();
	}

	public String getName() {
		final StringBuffer name = new StringBuffer();
		name.append(this.getEditableName());
		return name.toString();
	}
}
