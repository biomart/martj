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

package org.biomart.builder.view.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.biomart.builder.model.Column;
import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.Key;
import org.biomart.builder.model.Relation;
import org.biomart.builder.model.Table;
import org.biomart.builder.model.DataSet.DataSetColumn;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.builder.model.DataSet.DataSetTable;
import org.biomart.builder.model.DataSet.DataSetTableType;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.BuilderBundle;
import org.biomart.builder.view.gui.MartTabSet.MartTab;

/**
 * This context adapts dataset diagrams to display different colours, and
 * provides the context menu for interacting with dataset diagrams.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.20, 21st June 2006
 * @since 0.1
 */
public class DataSetContext extends WindowContext {
	/**
	 * Creates a new context that will adapt objects according to the settings
	 * in the specified dataset.
	 * 
	 * @param martTab
	 *            the mart tab this context appears in.
	 * @param dataset
	 *            the dataset this context will use for customising menus and
	 *            colours.
	 */
	public DataSetContext(MartTab martTab, DataSet dataset) {
		super(martTab, dataset);
	}

	public void populateContextMenu(JPopupMenu contextMenu, Object object) {

		// Did the user click on a dataset object?
		if (object instanceof DataSet) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Option to remove the dataset from the mart.
			JMenuItem remove = new JMenuItem(BuilderBundle
					.getString("removeDataSetTitle"));
			remove.setMnemonic(BuilderBundle.getString("removeDataSetMnemonic")
					.charAt(0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestRemoveDataSet(
							getDataSet());
				}
			});
			contextMenu.add(remove);

			// Option to optimise the dataset.
			JMenuItem optimise = new JMenuItem(BuilderBundle
					.getString("optimiseDataSetTitle"));
			optimise.setMnemonic(BuilderBundle.getString(
					"optimiseDataSetMnemonic").charAt(0));
			optimise.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestOptimiseDataSet(
							getDataSet());
				}
			});
			contextMenu.add(optimise);

			// Option to rename the dataset.
			JMenuItem rename = new JMenuItem(BuilderBundle
					.getString("renameDataSetTitle"));
			rename.setMnemonic(BuilderBundle.getString("renameDataSetMnemonic")
					.charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestRenameDataSet(
							getDataSet());
				}
			});
			contextMenu.add(rename);

			// Option to replicate the dataset from the mart.
			JMenuItem replicate = new JMenuItem(BuilderBundle
					.getString("replicateDataSetTitle"));
			replicate.setMnemonic(BuilderBundle.getString(
					"replicateDataSetMnemonic").charAt(0));
			replicate.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestReplicateDataSet(
							getDataSet());
				}
			});
			contextMenu.add(replicate);

			// The optimiser submenu allows the user to choose different
			// post-construction optimiser types for the dataset.
			JMenu optimiserMenu = new JMenu(BuilderBundle
					.getString("optimiserTypeTitle"));
			optimiserMenu.setMnemonic(BuilderBundle.getString(
					"optimiserTypeMnemonic").charAt(0));

			// Make a group for the different optimiser types.
			ButtonGroup optGroup = new ButtonGroup();

			// The no-optimiser option turns post-construction optimisation off.
			JRadioButtonMenuItem optNone = new JRadioButtonMenuItem(
					BuilderBundle.getString("optimiserNoneTitle"));
			optNone.setMnemonic(BuilderBundle
					.getString("optimiserNoneMnemonic").charAt(0));
			optNone.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestChangeOptimiserType(
							getDataSet(), DataSetOptimiserType.NONE);
				}
			});
			optGroup.add(optNone);
			optimiserMenu.add(optNone);
			if (this.getDataSet().getDataSetOptimiserType().equals(
					DataSetOptimiserType.NONE))
				optNone.setSelected(true);

			// The column option turns on has-column optimisation.
			JRadioButtonMenuItem optCol = new JRadioButtonMenuItem(
					BuilderBundle.getString("optimiserColumnTitle"));
			optCol.setMnemonic(BuilderBundle.getString(
					"optimiserColumnMnemonic").charAt(0));
			optCol.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestChangeOptimiserType(
							getDataSet(), DataSetOptimiserType.COLUMN);
				}
			});
			optGroup.add(optCol);
			optimiserMenu.add(optCol);
			if (this.getDataSet().getDataSetOptimiserType().equals(
					DataSetOptimiserType.COLUMN))
				optCol.setSelected(true);

			// The table option turns on table-of-has-column optimisation.
			JRadioButtonMenuItem optTbl = new JRadioButtonMenuItem(
					BuilderBundle.getString("optimiserTableTitle"));
			optTbl.setMnemonic(BuilderBundle
					.getString("optimiserTableMnemonic").charAt(0));
			optTbl.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestChangeOptimiserType(
							getDataSet(), DataSetOptimiserType.TABLE);
				}
			});
			optGroup.add(optTbl);
			optimiserMenu.add(optTbl);
			if (this.getDataSet().getDataSetOptimiserType().equals(
					DataSetOptimiserType.TABLE))
				optTbl.setSelected(true);

			// Add the optimiser type submenu to the context menu.
			contextMenu.add(optimiserMenu);

			// Option to create the DDL for the dataset.
			JMenuItem saveDDL = new JMenuItem(BuilderBundle
					.getString("saveDDLTitle"));
			saveDDL.setMnemonic(BuilderBundle.getString("saveDDLMnemonic")
					.charAt(0));
			saveDDL.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestCreateDDL(
							getDataSet());
				}
			});
			contextMenu.add(saveDDL);
		}

		// Did the user click on a dataset table?
		else if (object instanceof DataSetTable) {

			// Add a separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Work out which table we are dealing with, and what type it is.
			final DataSetTable table = (DataSetTable) object;
			DataSetTableType tableType = table.getType();

			// Option to explain how the table was constructed.
			JMenuItem explain = new JMenuItem(BuilderBundle
					.getString("explainTableTitle"));
			explain.setMnemonic(BuilderBundle.getString("explainTableMnemonic")
					.charAt(0));
			explain.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestExplainTable(table);
				}
			});
			contextMenu.add(explain);

			// Rename the column.
			JMenuItem rename = new JMenuItem(BuilderBundle
					.getString("renameTableTitle"));
			rename.setMnemonic(BuilderBundle.getString("renameTableMnemonic")
					.charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestRenameDataSetTable(
							table);
				}
			});
			contextMenu.add(rename);

			// Dimension tables have their own options.
			if (tableType.equals(DataSetTableType.DIMENSION)) {

				// The dimension can be removed by using this option. This
				// simply masks the relation that caused the dimension to exist.
				JMenuItem removeDM = new JMenuItem(BuilderBundle
						.getString("removeDimensionTitle"));
				removeDM.setMnemonic(BuilderBundle.getString(
						"removeDimensionMnemonic").charAt(0));
				removeDM.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						Relation relation = (Relation) table
								.getUnderlyingRelations().get(0);
						getMartTab().getDataSetTabSet().requestMaskRelation(
								(DataSet) table.getSchema(), relation);
					}
				});
				contextMenu.add(removeDM);

				// The dimension can be merged by using this option. This
				// changes the relation that caused the dimension to exist
				// into a 1:1 relation.
				JMenuItem mergeDM = new JMenuItem(BuilderBundle
						.getString("mergeTableTitle"));
				mergeDM.setMnemonic(BuilderBundle.getString(
						"mergeTableMnemonic").charAt(0));
				mergeDM.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						Relation relation = (Relation) table
								.getUnderlyingRelations().get(0);
						getMartTab().getSchemaTabSet()
								.requestChangeRelationCardinality(relation,
										Cardinality.ONE);
					}
				});
				contextMenu.add(mergeDM);
			}

			// Subclass tables have their own options too.
			else if (tableType.equals(DataSetTableType.MAIN_SUBCLASS)) {

				// The subclass table can be removed by using this option. This
				// simply masks the relation that caused the subclass to exist.
				JMenuItem removeDM = new JMenuItem(BuilderBundle
						.getString("removeSubclassTitle"));
				removeDM.setMnemonic(BuilderBundle.getString(
						"removeSubclassMnemonic").charAt(0));
				removeDM.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						Relation relation = (Relation) table
								.getUnderlyingRelations().get(0);
						getMartTab().getDataSetTabSet()
								.requestUnsubclassRelation(
										(DataSet) table.getSchema(), relation);
					}
				});
				contextMenu.add(removeDM);

				// The subclass table can be merged by using this option.
				// This unflags the relation that caused the subclass to
				// exist.
				JMenuItem mergeDM = new JMenuItem(BuilderBundle
						.getString("mergeTableTitle"));
				mergeDM.setMnemonic(BuilderBundle.getString(
						"mergeTableMnemonic").charAt(0));
				mergeDM.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						Relation relation = (Relation) table
								.getUnderlyingRelations().get(0);
						getMartTab().getDataSetTabSet()
								.requestUnsubclassRelation(getDataSet(),
										relation);
					}
				});
				contextMenu.add(mergeDM);
			}
		}

		// Keys have menus too.
		else if (object instanceof Key) {
			// Keys behave as though the table had been clicked on.
			Table table = ((Key) object).getTable();
			this.populateContextMenu(contextMenu, table);
		}

		// Column menu goes here.
		else if (object instanceof DataSetColumn) {
			// Columns first show the stuff that would have showed
			// had the table been clicked on.
			Table table = ((DataSetColumn) object).getTable();
			this.populateContextMenu(contextMenu, table);

			// Add separator if the menu is not empty.
			if (contextMenu.getComponentCount() > 0)
				contextMenu.addSeparator();

			// Work out which column has been clicked.
			final DataSetColumn column = (DataSetColumn) object;

			// Explain the column to the user.
			JMenuItem explain = new JMenuItem(BuilderBundle
					.getString("explainColumnTitle"));
			explain.setMnemonic(BuilderBundle
					.getString("explainColumnMnemonic").charAt(0));
			explain.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet()
							.requestExplainColumn(column);
				}
			});
			contextMenu.add(explain);

			// Mask the column.
			final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(BuilderBundle
					.getString("maskColumnTitle"));
			mask.setMnemonic(BuilderBundle.getString("maskColumnMnemonic")
					.charAt(0));
			mask.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (mask.isSelected())
						getMartTab().getDataSetTabSet().requestMaskColumn(
								getDataSet(), column);
					else
						getMartTab().getDataSetTabSet().requestUnmaskColumn(
								getDataSet(), column);
				}
			});
			contextMenu.add(mask);
			if (this.getDataSet().getMaskedDataSetColumns().contains(column))
				mask.setSelected(true);

			// Rename the column.
			JMenuItem rename = new JMenuItem(BuilderBundle
					.getString("renameColumnTitle"));
			rename.setMnemonic(BuilderBundle.getString("renameColumnMnemonic")
					.charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestRenameDataSetColumn(
							column);
				}
			});
			contextMenu.add(rename);

			// If it's a schema name column...
			if (column instanceof SchemaNameColumn) {

				// Allow the user to partition, or unpartition, by schema in
				// schema group.
				final JCheckBoxMenuItem partition = new JCheckBoxMenuItem(
						BuilderBundle.getString("partitionOnSchemaTitle"));
				partition.setMnemonic(BuilderBundle.getString(
						"partitionOnSchemaMnemonic").charAt(0));
				partition.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						if (partition.isSelected())
							getMartTab().getDataSetTabSet()
									.requestPartitionBySchema(getDataSet());
						else
							getMartTab().getDataSetTabSet()
									.requestUnpartitionBySchema(getDataSet());
					}
				});
				contextMenu.add(partition);
				if (this.getDataSet().getPartitionOnSchema())
					partition.setSelected(false);
			}

			// Else, if it's a wrapped column...
			else if (column instanceof WrappedColumn) {

				// Which column is it? And is it already partitioned?
				final WrappedColumn wrappedCol = (WrappedColumn) column;
				boolean isPartitioned = this.getDataSet()
						.getPartitionedWrappedColumns().contains(column);

				// If it is partitioned, make a submenu to change the partition
				// type.
				if (isPartitioned) {

					// Set up the partitioning submenu.
					JMenu partitionSubmenu = new JMenu(BuilderBundle
							.getString("partitionColumnSMTitle"));
					partitionSubmenu.setMnemonic(BuilderBundle.getString(
							"partitionColumnSMMnemonic").charAt(0));

					// The option to change the partition type.
					JMenuItem changepartition = new JMenuItem(BuilderBundle
							.getString("changePartitionColumnTitle"));
					changepartition.setMnemonic(BuilderBundle.getString(
							"changePartitionColumnMnemonic").charAt(0));
					changepartition.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							getMartTab().getDataSetTabSet()
									.requestPartitionByColumn(getDataSet(),
											wrappedCol);
						}
					});
					partitionSubmenu.add(changepartition);

					// The option to turn off partitioning.
					JMenuItem unpartition = new JMenuItem(BuilderBundle
							.getString("unpartitionColumnTitle"));
					unpartition.setMnemonic(BuilderBundle.getString(
							"unpartitionColumnMnemonic").charAt(0));
					unpartition.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							getMartTab().getDataSetTabSet()
									.requestUnpartitionByColumn(getDataSet(),
											wrappedCol);
						}
					});
					partitionSubmenu.add(unpartition);

					// Add the submenu to the context menu.
					contextMenu.add(partitionSubmenu);
				}

				// If it is not partitioned, allow the user to turn partitioning
				// on.
				else {

					// Option to enable partitioning.
					JMenuItem partition = new JMenuItem(BuilderBundle
							.getString("partitionColumnTitle"));
					partition.setMnemonic(BuilderBundle.getString(
							"partitionColumnMnemonic").charAt(0));
					partition.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							getMartTab().getDataSetTabSet()
									.requestPartitionByColumn(getDataSet(),
											wrappedCol);
						}
					});
					contextMenu.add(partition);
				}
			}
		}
	}

	public void customiseAppearance(JComponent component, Object object) {

		// Is it a relation?
		if (object instanceof Relation) {

			// Which relation is it?
			Relation relation = (Relation) object;

			// Highlight SUBCLASS relations.
			if (this.getDataSet().getSubclassedRelations().contains(relation))
				component.setForeground(RelationComponent.SUBCLASS_COLOUR);

			// All the rest are normal.
			else
				component.setForeground(RelationComponent.NORMAL_COLOUR);
		}

		// Is it a table?
		else if (object instanceof DataSetTable) {

			// Which table is it?
			DataSetTableType tableType = ((DataSetTable) object).getType();

			// Highlight SUBCLASS tables.
			if (tableType.equals(DataSetTableType.MAIN_SUBCLASS))
				component.setForeground(TableComponent.SUBCLASS_COLOUR);

			// Highlight DIMENSION tables.
			else if (tableType.equals(DataSetTableType.DIMENSION))
				component.setForeground(TableComponent.DIMENSION_COLOUR);

			// All others are normal.
			else
				component.setForeground(TableComponent.NORMAL_COLOUR);
		}

		// Columns.
		else if (object instanceof Column) {

			// Which column is it?
			Column column = (Column) object;

			// Fade out all MASKED columns.
			if (this.getDataSet().getMaskedDataSetColumns().contains(column))
				component.setForeground(ColumnComponent.FADED_COLOUR);

			// Blue PARTITIONED columns and the schema name if partition on
			// dataset.
			else if (this.getDataSet().getPartitionedWrappedColumns().contains(
					column)
					|| ((column instanceof SchemaNameColumn) && this
							.getDataSet().getPartitionOnSchema()))
				component.setForeground(ColumnComponent.PARTITIONED_COLOUR);

			// All others are normal.
			else
				component.setForeground(ColumnComponent.NORMAL_COLOUR);
		}
	}
}
