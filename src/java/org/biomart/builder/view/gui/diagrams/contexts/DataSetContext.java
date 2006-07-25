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

package org.biomart.builder.view.gui.diagrams.contexts;

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
import org.biomart.builder.model.DataSet.DataSetColumn.ExpressionColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.SchemaNameColumn;
import org.biomart.builder.model.DataSet.DataSetColumn.WrappedColumn;
import org.biomart.builder.model.Relation.Cardinality;
import org.biomart.builder.resources.Resources;
import org.biomart.builder.view.gui.MartTabSet.MartTab;
import org.biomart.builder.view.gui.diagrams.components.ColumnComponent;
import org.biomart.builder.view.gui.diagrams.components.RelationComponent;
import org.biomart.builder.view.gui.diagrams.components.TableComponent;

/**
 * This context adapts dataset org.biomart.builder.view.gui.diagrams to display
 * different colours, and provides the context menu for interacting with dataset
 * org.biomart.builder.view.gui.diagrams.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version 0.1.24, 24th July 2006
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
			JMenuItem remove = new JMenuItem(Resources
					.get("removeDataSetTitle"));
			remove
					.setMnemonic(Resources.get("removeDataSetMnemonic").charAt(
							0));
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestRemoveDataSet(
							getDataSet());
				}
			});
			contextMenu.add(remove);

			// Option to rename the dataset.
			JMenuItem rename = new JMenuItem(Resources
					.get("renameDataSetTitle"));
			rename
					.setMnemonic(Resources.get("renameDataSetMnemonic").charAt(
							0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestRenameDataSet(
							getDataSet());
				}
			});
			contextMenu.add(rename);

			// Option to replicate the dataset from the mart.
			JMenuItem replicate = new JMenuItem(Resources
					.get("replicateDataSetTitle"));
			replicate.setMnemonic(Resources.get("replicateDataSetMnemonic")
					.charAt(0));
			replicate.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestReplicateDataSet(
							getDataSet());
				}
			});
			contextMenu.add(replicate);

			// The optimiser submenu allows the user to choose different
			// post-construction optimiser types for the dataset.
			JMenu optimiserMenu = new JMenu(Resources.get("optimiserTypeTitle"));
			optimiserMenu.setMnemonic(Resources.get("optimiserTypeMnemonic")
					.charAt(0));

			// Make a group for the different optimiser types.
			ButtonGroup optGroup = new ButtonGroup();

			// The no-optimiser option turns post-construction optimisation off.
			JRadioButtonMenuItem optNone = new JRadioButtonMenuItem(Resources
					.get("optimiserNoneTitle"));
			optNone.setMnemonic(Resources.get("optimiserNoneMnemonic")
					.charAt(0));
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
			JRadioButtonMenuItem optCol = new JRadioButtonMenuItem(Resources
					.get("optimiserColumnTitle"));
			optCol.setMnemonic(Resources.get("optimiserColumnMnemonic").charAt(
					0));
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
			JRadioButtonMenuItem optTbl = new JRadioButtonMenuItem(Resources
					.get("optimiserTableTitle"));
			optTbl.setMnemonic(Resources.get("optimiserTableMnemonic")
					.charAt(0));
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
			JMenuItem saveDDL = new JMenuItem(Resources.get("saveDDLTitle"));
			saveDDL.setMnemonic(Resources.get("saveDDLMnemonic").charAt(0));
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
			JMenuItem explain = new JMenuItem(Resources
					.get("explainTableTitle"));
			explain
					.setMnemonic(Resources.get("explainTableMnemonic")
							.charAt(0));
			explain.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestExplainTable(table);
				}
			});
			contextMenu.add(explain);

			// Rename the table.
			JMenuItem rename = new JMenuItem(Resources.get("renameTableTitle"));
			rename.setMnemonic(Resources.get("renameTableMnemonic").charAt(0));
			rename.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestRenameDataSetTable(
							table);
				}
			});
			contextMenu.add(rename);

			// Add an expression column.
			JMenuItem expression = new JMenuItem(Resources
					.get("addExpressionColumnTitle"));
			expression.setMnemonic(Resources.get("addExpressionColumnMnemonic")
					.charAt(0));
			expression.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet().requestAddExpressionColumn(
							table);
				}
			});
			contextMenu.add(expression);

			// Dimension tables have their own options.
			if (tableType.equals(DataSetTableType.DIMENSION)) {

				// The dimension can be removed by using this option. This
				// simply masks the relation that caused the dimension to exist.
				JMenuItem removeDM = new JMenuItem(Resources
						.get("removeDimensionTitle"));
				removeDM.setMnemonic(Resources.get("removeDimensionMnemonic")
						.charAt(0));
				removeDM.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						getMartTab().getDataSetTabSet().requestMaskRelation(
								getDataSet(), table.getSourceRelation());
					}
				});
				contextMenu.add(removeDM);

				// The dimension can be merged by using this option. This
				// simply changes the relation cardinality to 1:1. This
				// affects ALL datasets, not just this one!
				JMenuItem mergeDM = new JMenuItem(Resources
						.get("mergeDimensionTitle"));
				mergeDM.setMnemonic(Resources.get("mergeDimensionMnemonic")
						.charAt(0));
				mergeDM.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						getMartTab().getSchemaTabSet()
								.requestChangeRelationCardinality(
										table.getSourceRelation(),
										Cardinality.ONE);
					}
				});
				contextMenu.add(mergeDM);
			}

			// Subclass tables have their own options too.
			else if (tableType.equals(DataSetTableType.MAIN_SUBCLASS)) {

				// The subclass table can be removed by using this option. This
				// simply masks the relation that caused the subclass to exist.
				JMenuItem unsubclass = new JMenuItem(Resources
						.get("removeSubclassTitle"));
				unsubclass.setMnemonic(Resources.get("removeSubclassMnemonic")
						.charAt(0));
				unsubclass.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						getMartTab().getDataSetTabSet()
								.requestUnsubclassRelation(getDataSet(),
										table.getSourceRelation());
					}
				});
				contextMenu.add(unsubclass);
			}

			// Main tables have their own stuff as well.
			else if (tableType.equals(DataSetTableType.MAIN)) {
				// Suggest invisible datasets.
				JMenuItem invisible = new JMenuItem(Resources
						.get("suggestInvisibleDatasetsTitle"));
				invisible.setMnemonic(Resources.get(
						"suggestInvisibleDatasetsMnemonic").charAt(0));
				invisible.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						getMartTab().getDataSetTabSet()
								.requestSuggestInvisibleDatasets(getDataSet(),
										table);
					}
				});
				contextMenu.add(invisible);
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
			JMenuItem explain = new JMenuItem(Resources
					.get("explainColumnTitle"));
			explain.setMnemonic(Resources.get("explainColumnMnemonic")
					.charAt(0));
			explain.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					getMartTab().getDataSetTabSet()
							.requestExplainColumn(column);
				}
			});
			contextMenu.add(explain);

			// Mask the column.
			final JCheckBoxMenuItem mask = new JCheckBoxMenuItem(Resources
					.get("maskColumnTitle"));
			mask.setMnemonic(Resources.get("maskColumnMnemonic").charAt(0));
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
			JMenuItem rename = new JMenuItem(Resources.get("renameColumnTitle"));
			rename.setMnemonic(Resources.get("renameColumnMnemonic").charAt(0));
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
						Resources.get("partitionOnSchemaTitle"));
				partition.setMnemonic(Resources
						.get("partitionOnSchemaMnemonic").charAt(0));
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
					JMenu partitionSubmenu = new JMenu(Resources
							.get("partitionColumnSMTitle"));
					partitionSubmenu.setMnemonic(Resources.get(
							"partitionColumnSMMnemonic").charAt(0));

					// The option to change the partition type.
					JMenuItem changepartition = new JMenuItem(Resources
							.get("changePartitionColumnTitle"));
					changepartition.setMnemonic(Resources.get(
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
					JMenuItem unpartition = new JMenuItem(Resources
							.get("unpartitionColumnTitle"));
					unpartition.setMnemonic(Resources.get(
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
					JMenuItem partition = new JMenuItem(Resources
							.get("partitionColumnTitle"));
					partition.setMnemonic(Resources.get(
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

			// Else, if it's an expression column...
			else if (column instanceof ExpressionColumn) {

				// Option to modify column.
				JMenuItem modify = new JMenuItem(Resources
						.get("modifyExpressionColumnTitle"));
				modify.setMnemonic(Resources.get(
						"modifyExpressionColumnMnemonic").charAt(0));
				modify.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						getMartTab().getDataSetTabSet()
								.requestModifyExpressionColumn(
										(ExpressionColumn) column);
					}
				});
				contextMenu.add(modify);

				// Option to remove column.
				JMenuItem remove = new JMenuItem(Resources
						.get("removeExpressionColumnTitle"));
				remove.setMnemonic(Resources.get(
						"removeExpressionColumnMnemonic").charAt(0));
				remove.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						getMartTab().getDataSetTabSet()
								.requestRemoveExpressionColumn(
										(ExpressionColumn) column);
					}
				});
				contextMenu.add(remove);

			}
		}
	}

	public void customiseAppearance(JComponent component, Object object) {

		// Is it a relation?
		if (object instanceof Relation) {

			// Which relation is it?
			Relation relation = (Relation) object;

			// What tables does it link?
			DataSetTable target = (DataSetTable) relation.getManyKey()
					.getTable();

			// Highlight SUBCLASS relations.
			if (target.getType().equals(DataSetTableType.MAIN_SUBCLASS))
				component.setForeground(RelationComponent.SUBCLASS_COLOUR);

			// All the rest are normal.
			else
				component.setForeground(RelationComponent.NORMAL_COLOUR);

			// Do the stroke.
			RelationComponent relcomp = (RelationComponent) component;
			relcomp.setStroke(RelationComponent.ONE_MANY);
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

			// Magenta EXPRESSION columns.
			else if (column instanceof ExpressionColumn)
				component.setForeground(ColumnComponent.EXPRESSION_COLOUR);

			// All others are normal.
			else
				component.setForeground(ColumnComponent.NORMAL_COLOUR);
		}
	}
}