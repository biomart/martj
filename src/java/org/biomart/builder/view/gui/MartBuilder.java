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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.biomart.builder.model.DataSet;
import org.biomart.builder.model.DataSet.DataSetOptimiserType;
import org.biomart.common.model.Schema;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.view.gui.BioMartGUI;
import org.biomart.common.view.gui.dialogs.AboutDialog;

/**
 * The main window housing the MartBuilder GUI. The {@link #main(String[])}
 * method starts the GUI and opens this window.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.1
 */
public class MartBuilder extends BioMartGUI {
	private static final long serialVersionUID = 1L;

	/**
	 * Run this application and open the main window. The window stays open and
	 * the application keeps running until the window is closed.
	 * 
	 * @param args
	 *            any command line arguments that the user specified will be in
	 *            this array.
	 */
	public static void main(final String[] args) {
		// Initialise resources.
		Settings.setApplication(Settings.MARTBUILDER);
		Resources.setResourceLocation("org/biomart/builder/resources");
		// Start the application.
		new MartBuilder().launch();
	}

	private MartTabSet martTabSet;

	protected void initComponents() {
		// Make a menu bar and add it.
		this.setJMenuBar(new MartBuilderMenuBar(this));

		// Set up the set of tabs to hold the various marts.
		this.martTabSet = new MartTabSet(this);
		this.getContentPane().add(this.martTabSet, BorderLayout.CENTER);

		// Go straight to the 'New' page.
		this.martTabSet.requestNewMart();
	}

	/**
	 * Exits the application, but only with permission from the mart tabset.
	 */
	public boolean confirmExitApp() {
		return this.martTabSet.confirmCloseAllMarts();
	}

	// This is the main menu bar.
	private class MartBuilderMenuBar extends JMenuBar implements ActionListener {
		private static final long serialVersionUID = 1;

		private JMenuItem closeMart;

		private JMenuItem exit;

		private MartBuilder martBuilder;

		private JMenuItem newMart;

		private JMenuItem openMart;

		private JMenuItem saveDDL;

		private JMenuItem saveMart;

		private JMenuItem saveMartAs;

		private JMenuItem addSchema;

		private JMenuItem updateAllSchemas;

		private JMenuItem createDatasets;

		private JMenuItem removeAllDatasets;

		private JMenuItem keyguessingSchema;

		private JMenuItem partitionedSchema;

		private JMenuItem updateSchema;

		private JMenuItem renameSchema;

		private JMenuItem replicateSchema;

		private JMenuItem removeSchema;

		private JMenuItem invisibleDataset;

		private JMenuItem explainDataset;

		private JMenuItem saveDatasetDDL;

		private JMenuItem renameDataset;

		private JMenuItem replicateDataset;

		private JMenuItem removeDataset;

		private JMenuItem extendDataset;

		private JMenu optimiseDatasetSubmenu;

		private JMenuItem indexOptimiser;

		private JMenuItem aboutMartBuilder;

		/**
		 * Constructor calls super then sets up our menu items.
		 * 
		 * @param martBuilder
		 *            the mart builder gui to which we are attached.
		 */
		public MartBuilderMenuBar(final MartBuilder martBuilder) {
			super();

			// Remember our parent.
			this.martBuilder = martBuilder;

			// Exit MartBuilder.
			this.exit = new JMenuItem(Resources.get("exitTitle"));
			this.exit.setMnemonic(Resources.get("exitMnemonic").charAt(0));
			this.exit.addActionListener(this);

			// New mart.
			this.newMart = new JMenuItem(Resources.get("newMartTitle"),
					new ImageIcon(Resources.getResourceAsURL("new.gif")));
			this.newMart
					.setMnemonic(Resources.get("newMartMnemonic").charAt(0));
			this.newMart.addActionListener(this);

			// Open existing mart.
			this.openMart = new JMenuItem(Resources.get("openMartTitle"),
					new ImageIcon(Resources.getResourceAsURL("open.gif")));
			this.openMart.setMnemonic(Resources.get("openMartMnemonic").charAt(
					0));
			this.openMart.addActionListener(this);

			// Save current mart.
			this.saveMart = new JMenuItem(Resources.get("saveMartTitle"),
					new ImageIcon(Resources.getResourceAsURL("save.gif")));
			this.saveMart.setMnemonic(Resources.get("saveMartMnemonic").charAt(
					0));
			this.saveMart.addActionListener(this);

			// Save current mart as.
			this.saveMartAs = new JMenuItem(Resources.get("saveMartAsTitle"),
					new ImageIcon(Resources.getResourceAsURL("save.gif")));
			this.saveMartAs.setMnemonic(Resources.get("saveMartAsMnemonic")
					.charAt(0));
			this.saveMartAs.addActionListener(this);

			// Create DDL for current mart.
			this.saveDDL = new JMenuItem(Resources.get("saveDDLTitle"),
					new ImageIcon(Resources.getResourceAsURL("saveText.gif")));
			this.saveDDL
					.setMnemonic(Resources.get("saveDDLMnemonic").charAt(0));
			this.saveDDL.addActionListener(this);

			// Close current mart.
			this.closeMart = new JMenuItem(Resources.get("closeMartTitle"));
			this.closeMart.setMnemonic(Resources.get("closeMartMnemonic")
					.charAt(0));
			this.closeMart.addActionListener(this);

			// Add new schema.
			this.addSchema = new JMenuItem(Resources.get("addSchemaTitle"),
					new ImageIcon(Resources.getResourceAsURL("add.gif")));
			this.addSchema.setMnemonic(Resources.get("closeMartMnemonic")
					.charAt(0));
			this.addSchema.addActionListener(this);

			// Sync all schemas.
			this.updateAllSchemas = new JMenuItem(Resources
					.get("synchroniseAllSchemasTitle"), new ImageIcon(Resources
					.getResourceAsURL("refresh.gif")));
			this.updateAllSchemas.setMnemonic(Resources.get(
					"synchroniseAllSchemasMnemonic").charAt(0));
			this.updateAllSchemas.addActionListener(this);

			// Create datasets.
			this.createDatasets = new JMenuItem(Resources
					.get("suggestDataSetsTitle"));
			this.createDatasets.setMnemonic(Resources.get(
					"suggestDataSetsMnemonic").charAt(0));
			this.createDatasets.addActionListener(this);

			// Remove all datasets.
			this.removeAllDatasets = new JMenuItem(Resources
					.get("removeAllDataSetsTitle"), new ImageIcon(Resources
					.getResourceAsURL("cut.gif")));
			this.removeAllDatasets.setMnemonic(Resources.get(
					"removeAllDataSetsMnemonic").charAt(0));
			this.removeAllDatasets.addActionListener(this);

			// Keyguessing.
			this.keyguessingSchema = new JCheckBoxMenuItem(Resources
					.get("enableKeyGuessingTitle"));
			this.keyguessingSchema.setMnemonic(Resources.get(
					"enableKeyGuessingMnemonic").charAt(0));
			this.keyguessingSchema.addActionListener(this);

			// Partitioned schema.
			this.partitionedSchema = new JCheckBoxMenuItem(Resources
					.get("partitionSchemaTitle"));
			this.partitionedSchema.setMnemonic(Resources.get(
					"partitionSchemaMnemonic").charAt(0));
			this.partitionedSchema.addActionListener(this);

			// Update schema.
			this.updateSchema = new JMenuItem(Resources
					.get("updateSchemaTitle"), new ImageIcon(Resources
					.getResourceAsURL("refresh.gif")));
			this.updateSchema.setMnemonic(Resources.get("updateSchemaMnemonic")
					.charAt(0));
			this.updateSchema.addActionListener(this);

			// Rename schema.
			this.renameSchema = new JMenuItem(Resources
					.get("renameSchemaTitle"));
			this.renameSchema.setMnemonic(Resources.get("renameSchemaMnemonic")
					.charAt(0));
			this.renameSchema.addActionListener(this);

			// Replicate schema.
			this.replicateSchema = new JMenuItem(Resources
					.get("replicateSchemaTitle"));
			this.replicateSchema.setMnemonic(Resources.get(
					"replicateSchemaMnemonic").charAt(0));
			this.replicateSchema.addActionListener(this);

			// Remove schema.
			this.removeSchema = new JMenuItem(Resources
					.get("removeSchemaTitle"), new ImageIcon(Resources
					.getResourceAsURL("cut.gif")));
			this.removeSchema.setMnemonic(Resources.get("removeSchemaMnemonic")
					.charAt(0));
			this.removeSchema.addActionListener(this);

			// Invisible.
			this.invisibleDataset = new JCheckBoxMenuItem(Resources
					.get("invisibleDataSetTitle"));
			this.invisibleDataset.setMnemonic(Resources.get(
					"invisibleDataSetMnemonic").charAt(0));
			this.invisibleDataset.addActionListener(this);

			// Explain dataset.
			this.explainDataset = new JMenuItem(Resources
					.get("explainDataSetTitle"), new ImageIcon(Resources
					.getResourceAsURL("help.gif")));
			this.explainDataset.setMnemonic(Resources.get(
					"explainDataSetMnemonic").charAt(0));
			this.explainDataset.addActionListener(this);

			// Save dataset DDL.
			this.saveDatasetDDL = new JMenuItem(Resources.get("saveDDLTitle"),
					new ImageIcon(Resources.getResourceAsURL("saveText.gif")));
			this.saveDatasetDDL.setMnemonic(Resources.get("saveDDLMnemonic")
					.charAt(0));
			this.saveDatasetDDL.addActionListener(this);

			// Rename dataset.
			this.renameDataset = new JMenuItem(Resources
					.get("renameDataSetTitle"));
			this.renameDataset.setMnemonic(Resources.get(
					"renameDataSetMnemonic").charAt(0));
			this.renameDataset.addActionListener(this);

			// Replicate dataset.
			this.replicateDataset = new JMenuItem(Resources
					.get("replicateDataSetTitle"));
			this.replicateDataset.setMnemonic(Resources.get(
					"replicateDataSetMnemonic").charAt(0));
			this.replicateDataset.addActionListener(this);

			// Remove dataset.
			this.removeDataset = new JMenuItem(Resources
					.get("removeDataSetTitle"), new ImageIcon(Resources
							.getResourceAsURL("cut.gif")));
			this.removeDataset.setMnemonic(Resources.get(
					"removeDataSetMnemonic").charAt(0));
			this.removeDataset.addActionListener(this);

			// Extend dataset.
			this.extendDataset = new JMenuItem(Resources
					.get("suggestInvisibleDatasetsTitle"));
			this.extendDataset.setMnemonic(Resources.get(
					"suggestInvisibleDatasetsMnemonic").charAt(0));
			this.extendDataset.addActionListener(this);

			// Make a submenu for the optimiser type.
			this.optimiseDatasetSubmenu = new JMenu(Resources
					.get("optimiserTitle"));
			this.optimiseDatasetSubmenu.setMnemonic(Resources.get(
					"optimiserMnemonic").charAt(0));
			final ButtonGroup optGroup = new ButtonGroup();
			// Loop through the map to create the submenu.
			for (final Iterator i = DataSetOptimiserType.getTypes().entrySet()
					.iterator(); i.hasNext();) {
				final Map.Entry entry = (Map.Entry) i.next();
				final String name = (String) entry.getKey();
				final DataSetOptimiserType value = (DataSetOptimiserType) entry
						.getValue();
				final JRadioButtonMenuItem opt = new JRadioButtonMenuItem(
						Resources.get("optimiser" + name + "Title"));
				opt.setMnemonic(Resources.get("optimiser" + name + "Mnemonic")
						.charAt(0));
				opt.addActionListener(new ActionListener() {
					public void actionPerformed(final ActionEvent evt) {
						final DataSet ds = martBuilder.martTabSet
								.getSelectedMartTab().getDataSetTabSet()
								.getSelectedDataSet();
						martBuilder.martTabSet.getSelectedMartTab()
								.getDataSetTabSet().requestChangeOptimiserType(
										ds, value);
					}
				});
				optGroup.add(opt);
				this.optimiseDatasetSubmenu.add(opt);
			}
			this.optimiseDatasetSubmenu.addSeparator();
			this.indexOptimiser = new JCheckBoxMenuItem(Resources
					.get("indexOptimiserTitle"));
			this.indexOptimiser.setMnemonic(Resources.get(
					"indexOptimiserMnemonic").charAt(0));
			this.indexOptimiser.addActionListener(this);
			this.optimiseDatasetSubmenu.add(this.indexOptimiser);

			// About.
			this.aboutMartBuilder = new JMenuItem(Resources
					.get("aboutMartBuilderTitle"));
			this.aboutMartBuilder.setMnemonic(Resources.get(
					"aboutMartBuilderMnemonic").charAt(0));
			this.aboutMartBuilder.addActionListener(this);

			// Construct the file menu.
			final JMenu fileMenu = new JMenu(Resources.get("fileMenuTitle"));
			fileMenu.setMnemonic(Resources.get("fileMenuMnemonic").charAt(0));
			fileMenu.add(this.newMart);
			fileMenu.add(this.openMart);
			fileMenu.addSeparator();
			fileMenu.add(this.saveMart);
			fileMenu.add(this.saveMartAs);
			fileMenu.addSeparator();
			fileMenu.add(this.closeMart);
			fileMenu.addSeparator();
			fileMenu.add(this.exit);

			// Construct the mart menu.
			final JMenu martMenu = new JMenu(Resources.get("martMenuTitle"));
			martMenu.setMnemonic(Resources.get("martMenuMnemonic").charAt(0));
			martMenu.add(this.saveDDL);
			martMenu.addSeparator();
			martMenu.add(this.addSchema);
			martMenu.add(this.updateAllSchemas);
			martMenu.addSeparator();
			martMenu.add(this.createDatasets);
			martMenu.add(this.removeAllDatasets);

			// Construct the schema menu.
			final JMenu schemaMenu = new JMenu(Resources.get("schemaMenuTitle"));
			schemaMenu.setMnemonic(Resources.get("schemaMenuMnemonic")
					.charAt(0));
			schemaMenu.add(this.keyguessingSchema);
			schemaMenu.add(this.partitionedSchema);
			schemaMenu.addSeparator();
			schemaMenu.add(this.updateSchema);
			schemaMenu.addSeparator();
			schemaMenu.add(this.renameSchema);
			schemaMenu.add(this.replicateSchema);
			schemaMenu.add(this.removeSchema);

			// Construct the dataset menu.
			final JMenu datasetMenu = new JMenu(Resources
					.get("datasetMenuTitle"));
			datasetMenu.setMnemonic(Resources.get("datasetMenuMnemonic")
					.charAt(0));
			datasetMenu.add(this.invisibleDataset);
			datasetMenu.add(this.optimiseDatasetSubmenu);
			datasetMenu.addSeparator();
			datasetMenu.add(this.explainDataset);
			datasetMenu.add(this.saveDatasetDDL);
			datasetMenu.addSeparator();
			datasetMenu.add(this.renameDataset);
			datasetMenu.add(this.replicateDataset);
			datasetMenu.add(this.removeDataset);
			datasetMenu.addSeparator();
			datasetMenu.add(this.extendDataset);

			// Construct the help menu.
			final JMenu helpMenu = new JMenu(Resources.get("helpMenuTitle"));
			helpMenu.setMnemonic(Resources.get("helpMenuMnemonic").charAt(0));
			// TODO help
			helpMenu.add(this.aboutMartBuilder);

			// Add a listener which checks which options to enable each time the
			// menu is opened. This mean that if no mart is currently selected,
			// save and close will be disabled, and if the current mart is not
			// modified, save will be disabled, etc.
			fileMenu.addMenuListener(new MenuListener() {
				public void menuCanceled(final MenuEvent e) {
				} // Interface requirement.

				public void menuDeselected(final MenuEvent e) {
				} // Interface requirement.

				public void menuSelected(final MenuEvent e) {
					boolean hasMart = true;
					if (martBuilder.martTabSet.getSelectedMartTab() == null)
						hasMart = false;
					MartBuilderMenuBar.this.saveMart.setEnabled(hasMart
							&& martBuilder.martTabSet.getModifiedStatus());
					MartBuilderMenuBar.this.saveMartAs.setEnabled(hasMart);
					MartBuilderMenuBar.this.closeMart.setEnabled(hasMart);
				}
			});
			martMenu.addMenuListener(new MenuListener() {
				public void menuCanceled(final MenuEvent e) {
				} // Interface requirement.

				public void menuDeselected(final MenuEvent e) {
				} // Interface requirement.

				public void menuSelected(final MenuEvent e) {
					boolean hasMart = true;
					if (martBuilder.martTabSet.getSelectedMartTab() == null)
						hasMart = false;
					MartBuilderMenuBar.this.saveDDL.setEnabled(hasMart
							&& martBuilder.martTabSet.getSelectedMartTab()
									.getMart().getDataSets().size() > 0);
					MartBuilderMenuBar.this.addSchema.setEnabled(hasMart);
					MartBuilderMenuBar.this.updateAllSchemas.setEnabled(hasMart &&
							martBuilder.martTabSet.getSelectedMartTab().getSchemaTabSet().getComponentCount()>1);
					MartBuilderMenuBar.this.createDatasets.setEnabled(hasMart &&
							martBuilder.martTabSet.getSelectedMartTab().getSchemaTabSet().getComponentCount()>1);
					MartBuilderMenuBar.this.removeAllDatasets.setEnabled(hasMart &&
							martBuilder.martTabSet.getSelectedMartTab().getDataSetTabSet().getComponentCount()>1);
				}
			});
			schemaMenu.addMenuListener(new MenuListener() {
				public void menuCanceled(final MenuEvent e) {
				} // Interface requirement.

				public void menuDeselected(final MenuEvent e) {
				} // Interface requirement.

				public void menuSelected(final MenuEvent e) {
					final Schema schema;
					if (martBuilder.martTabSet.getSelectedMartTab() != null)
						schema = martBuilder.martTabSet.getSelectedMartTab()
								.getSchemaTabSet().getSelectedSchema();
					else
						schema = null;
					MartBuilderMenuBar.this.keyguessingSchema
							.setEnabled(schema != null);
					MartBuilderMenuBar.this.keyguessingSchema
							.setSelected(schema != null
									&& schema.getKeyGuessing());
					MartBuilderMenuBar.this.partitionedSchema
							.setEnabled(schema != null);
					MartBuilderMenuBar.this.partitionedSchema
							.setSelected(schema != null
									&& !schema.getPartitions().isEmpty());
					MartBuilderMenuBar.this.updateSchema
							.setEnabled(schema != null);
					MartBuilderMenuBar.this.renameSchema
							.setEnabled(schema != null);
					MartBuilderMenuBar.this.replicateSchema
							.setEnabled(schema != null);
					MartBuilderMenuBar.this.removeSchema
							.setEnabled(schema != null);
				}
			});
			datasetMenu.addMenuListener(new MenuListener() {
				public void menuCanceled(final MenuEvent e) {
				} // Interface requirement.

				public void menuDeselected(final MenuEvent e) {
				} // Interface requirement.

				public void menuSelected(final MenuEvent e) {
					final DataSet ds;
					if (martBuilder.martTabSet.getSelectedMartTab() != null)
						ds = martBuilder.martTabSet.getSelectedMartTab()
								.getDataSetTabSet().getSelectedDataSet();
					else
						ds = null;
					MartBuilderMenuBar.this.invisibleDataset
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.invisibleDataset
							.setSelected(ds != null && ds.getInvisible());
					MartBuilderMenuBar.this.explainDataset
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.saveDatasetDDL
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.renameDataset
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.replicateDataset
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.removeDataset
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.extendDataset
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.optimiseDatasetSubmenu
							.setEnabled(ds != null);
				}
			});
			this.optimiseDatasetSubmenu.addMenuListener(new MenuListener() {
				public void menuCanceled(final MenuEvent e) {
				} // Interface requirement.

				public void menuDeselected(final MenuEvent e) {
				} // Interface requirement.

				public void menuSelected(final MenuEvent e) {
					final DataSet ds;
					if (martBuilder.martTabSet.getSelectedMartTab() != null)
						ds = martBuilder.martTabSet.getSelectedMartTab()
								.getDataSetTabSet().getSelectedDataSet();
					else
						ds = null;
					MartBuilderMenuBar.this.indexOptimiser
							.setEnabled(ds != null);
					MartBuilderMenuBar.this.indexOptimiser
							.setSelected(ds != null && ds.isIndexOptimiser());
					int index = 0;
					for (final Iterator i = DataSetOptimiserType.getTypes()
							.values().iterator(); i.hasNext(); index++) {
						final DataSetOptimiserType value = (DataSetOptimiserType) i.next();
						if (ds.getDataSetOptimiserType().equals(value)) 
						((JMenuItem)MartBuilderMenuBar.this.optimiseDatasetSubmenu.getMenuComponent(index)).setSelected(true);
					}
				}
			});

			// Adds the menus to the menu bar.
			this.add(fileMenu);
			this.add(martMenu);
			this.add(schemaMenu);
			this.add(datasetMenu);
			this.add(helpMenu);
		}

		public void actionPerformed(final ActionEvent e) {
			// File menu.
			if (e.getSource() == this.exit)
				this.martBuilder.requestExitApp();
			else if (e.getSource() == this.newMart)
				this.martBuilder.martTabSet.requestNewMart();
			else if (e.getSource() == this.openMart)
				this.martBuilder.martTabSet.loadMart();
			else if (e.getSource() == this.saveMart)
				this.martBuilder.martTabSet.saveMart();
			else if (e.getSource() == this.saveMartAs)
				this.martBuilder.martTabSet.saveMartAs();
			else if (e.getSource() == this.closeMart)
				this.martBuilder.martTabSet.confirmCloseMart();
			// Mart menu.
			else if (e.getSource() == this.saveDDL)
				this.martBuilder.martTabSet.requestCreateDDL();
			else if (e.getSource() == this.addSchema)
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getSchemaTabSet().requestAddSchema();
			else if (e.getSource() == this.updateAllSchemas)
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getSchemaTabSet().requestSynchroniseAllSchemas();
			else if (e.getSource() == this.createDatasets)
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestSuggestDataSets(null);
			else if (e.getSource() == this.removeAllDatasets)
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestRemoveAllDataSets();
			// Schema menu
			else if (e.getSource() == this.keyguessingSchema) {
				final Schema schema = this.martBuilder.martTabSet
						.getSelectedMartTab().getSchemaTabSet()
						.getSelectedSchema();
				if (this.keyguessingSchema.isSelected())
					this.martBuilder.martTabSet.getSelectedMartTab()
							.getSchemaTabSet().requestEnableKeyGuessing(schema);
				else
					this.martBuilder.martTabSet.getSelectedMartTab()
							.getSchemaTabSet()
							.requestDisableKeyGuessing(schema);
			} else if (e.getSource() == this.partitionedSchema) {
				final Schema schema = this.martBuilder.martTabSet
						.getSelectedMartTab().getSchemaTabSet()
						.getSelectedSchema();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getSchemaTabSet()
						.requestModifySchemaPartitions(schema);
			} else if (e.getSource() == this.updateSchema) {
				final Schema schema = this.martBuilder.martTabSet
						.getSelectedMartTab().getSchemaTabSet()
						.getSelectedSchema();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getSchemaTabSet().requestModifySchema(schema);
			} else if (e.getSource() == this.renameSchema) {
				final Schema schema = this.martBuilder.martTabSet
						.getSelectedMartTab().getSchemaTabSet()
						.getSelectedSchema();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getSchemaTabSet().requestRenameSchema(schema);
			} else if (e.getSource() == this.replicateSchema) {
				final Schema schema = this.martBuilder.martTabSet
						.getSelectedMartTab().getSchemaTabSet()
						.getSelectedSchema();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getSchemaTabSet().requestReplicateSchema(schema);
			} else if (e.getSource() == this.removeSchema) {
				final Schema schema = this.martBuilder.martTabSet
						.getSelectedMartTab().getSchemaTabSet()
						.getSelectedSchema();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getSchemaTabSet().requestRemoveSchema(schema);
			}
			// Dataset menu.
			else if (e.getSource() == this.invisibleDataset) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				if (this.invisibleDataset.isSelected())
					this.martBuilder.martTabSet.getSelectedMartTab()
							.getDataSetTabSet().requestInvisibleDataSet(ds);
				else
					this.martBuilder.martTabSet.getSelectedMartTab()
							.getDataSetTabSet().requestVisibleDataSet(ds);
			} else if (e.getSource() == this.explainDataset) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestExplainDataSet(ds);
			} else if (e.getSource() == this.saveDatasetDDL) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestCreateDDL(ds);
			} else if (e.getSource() == this.renameDataset) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestRenameDataSet(ds);
			} else if (e.getSource() == this.replicateDataset) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestReplicateDataSet(ds);
			} else if (e.getSource() == this.removeDataset) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestRemoveDataSet(ds);
			} else if (e.getSource() == this.extendDataset) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				this.martBuilder.martTabSet.getSelectedMartTab()
						.getDataSetTabSet().requestSuggestInvisibleDatasets(ds,
								ds.getMainTable());
			} else if (e.getSource() == this.indexOptimiser) {
				final DataSet ds = this.martBuilder.martTabSet
						.getSelectedMartTab().getDataSetTabSet()
						.getSelectedDataSet();
				if (this.indexOptimiser.isSelected())
					this.martBuilder.martTabSet.getSelectedMartTab()
							.getDataSetTabSet().requestIndexOptimiser(ds);
				else
					this.martBuilder.martTabSet.getSelectedMartTab()
							.getDataSetTabSet().requestNoIndexOptimiser(ds);
			}
			// Help menu
			else if (e.getSource() == this.aboutMartBuilder)
				this.martBuilder.requestShowAbout();
		}
	}
}
