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

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.diagrams.Diagram;

/**
 * A diagram component that represents a dataset. It usually only has a label in
 * it.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.4, 29th August 2006
 * @since 0.1
 */
public class DataSetComponent extends BoxShapedComponent {

	private static final long serialVersionUID = 1;

	private GridBagLayout layout;

	private GridBagConstraints constraints;

	/**
	 * This color is the one used for the background of invisible datasets.
	 */
	public static Color INVISIBLE_BACKGROUND = Color.WHITE;

	/**
	 * This color is the one used for the background of visible datasets.
	 */
	public static Color VISIBLE_BACKGROUND = Color.LIGHT_GRAY;
	
	/**
	 * Bold font.
	 */
	public static Font BOLD_FONT = Font.decode("Serif-BOLD-10");

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

		// Constraints for each field.
		this.constraints = new GridBagConstraints();
		this.constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.constraints.fill = GridBagConstraints.HORIZONTAL;
		this.constraints.anchor = GridBagConstraints.CENTER;
		this.constraints.insets = new Insets(5, 5, 5, 5);

		// Calculate the components and add them to the list.
		this.recalculateDiagramComponent();
	}

	public void recalculateDiagramComponent() {
		// Remove all our components.
		this.removeAll();

		// Set the background colour.
		if (this.getDataSet().getInvisible())
			this.setBackground(DataSetComponent.INVISIBLE_BACKGROUND);
		else
			this.setBackground(DataSetComponent.VISIBLE_BACKGROUND);

		// Add the label for the schema name,
		final JLabel label = new JLabel(this.getDataSet().getName());
		label.setFont(DataSetComponent.BOLD_FONT);
		this.layout.setConstraints(label, this.constraints);
		this.add(label);
	}

	private DataSet getDataSet() {
		return (DataSet) this.getObject();
	}

	public JPopupMenu getContextMenu() {
		// First of all, work out what would have been shown by default.
		final JPopupMenu contextMenu = super.getContextMenu();
		
		// Add a divider if necessary.
		if (contextMenu.getComponentCount()>0)
			contextMenu.addSeparator();

		// Add the 'show tables' option, which opens the tab representing
		// this schema.
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
		contextMenu.add(showTables);

		// Return it. Will be further adapted by a listener elsewhere.
		return contextMenu;
	}
}
