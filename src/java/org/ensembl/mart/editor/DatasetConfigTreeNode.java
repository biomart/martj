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

package org.ensembl.mart.editor;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.ensembl.mart.lib.config.*;

/**
 * Class DatasetConfigTreeNode extends DefaultMutableTreeNode.
 *
 * <p>This class is written so that the tree node is aware of the datasetconfig etc objects
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetConfig
 */

public class DatasetConfigTreeNode extends DefaultMutableTreeNode {

	protected String name;

	public DatasetConfigTreeNode(String name) {
		this.name = name;
	}

	public DatasetConfigTreeNode(String name, Object obj) {
		this.name = name;
		this.setUserObject(obj);
	}

	public void setName(String newName) {
		name = newName;
	}

	public String toString() {
		return name;
	}

	public void setUserObject(Object obj) {
		super.setUserObject(obj);

		String nodeObjectClass = obj.getClass().getName();
		if (nodeObjectClass.equals("org.ensembl.mart.lib.config.DatasetConfig")) {
			setName("DatasetConfig: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			DatasetConfig dsv = (DatasetConfig) obj;

			Importable[] imps = dsv.getImportables();
			for (int i = 0; i < imps.length; i++) {
				Importable importable = imps[i];
				String impName = importable.getLinkName();
				DatasetConfigTreeNode impNode = new DatasetConfigTreeNode("Importable:" + impName);
				impNode.setUserObject(importable);
				this.add(impNode);
			}
			Exportable[] exps = dsv.getExportables();
			for (int i = 0; i < exps.length; i++) {
				Exportable exportable = exps[i];
				String expName = exportable.getLinkName();
				DatasetConfigTreeNode expNode = new DatasetConfigTreeNode("Exportable:" + expName);        
				expNode.setUserObject(exportable);
				this.add(expNode);
			}
			SeqModule[] sms = dsv.getSeqModules();
			for (int i = 0; i < sms.length; i++) {
				SeqModule sm = sms[i];
				String smName = sm.getLinkName();
				DatasetConfigTreeNode smNode = new DatasetConfigTreeNode("SeqModule:" + smName);
				smNode.setUserObject(sm);
				this.add(smNode);
			}
			FilterPage[] fpages = dsv.getFilterPages();
			for (int i = 0; i < fpages.length; i++) {
				if (fpages[i].getClass().getName().equals("org.ensembl.mart.lib.config.FilterPage")) {
					FilterPage fp = fpages[i];
					String fpName = fp.getInternalName();
					DatasetConfigTreeNode fpNode = new DatasetConfigTreeNode("FilterPage:" + fpName);
					fpNode.setUserObject(fp);

					this.add(fpNode);
					List groups = fp.getFilterGroups();
					for (int j = 0; j < groups.size(); j++) {
						if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.FilterGroup")) {
							FilterGroup fiGroup = (FilterGroup) groups.get(j);
							String grName = fiGroup.getInternalName();
							DatasetConfigTreeNode grNode = new DatasetConfigTreeNode("FilterGroup:" + grName);
							grNode.setUserObject(fiGroup);
							FilterCollection[] collections = fiGroup.getFilterCollections();
							for (int z = 0; z < collections.length; z++) {
								FilterCollection fiCollection = collections[z];
								String colName = fiCollection.getInternalName();
								DatasetConfigTreeNode colNode = new DatasetConfigTreeNode("FilterCollection:" + colName);
								colNode.setUserObject(fiCollection);
								List descriptions = fiCollection.getFilterDescriptions();
								for (int y = 0; y < descriptions.size(); y++) {
									FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
									String desName = fiDescription.getInternalName();
									DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("FilterDescription:" + desName);
									desNode.setUserObject(fiDescription);
									Enable[] enables = fiDescription.getEnables();
									Disable[] disables = fiDescription.getDisables();
									Option[] options = fiDescription.getOptions();
									for (int k = 0; k < enables.length; k++) {
										Enable enable = enables[k];
										DatasetConfigTreeNode enableNode = new DatasetConfigTreeNode("Enable");
										enableNode.setUserObject(enable);
									}
									for (int k = 0; k < disables.length; k++) {
										Disable disable = disables[k];
										DatasetConfigTreeNode disableNode = new DatasetConfigTreeNode("Disable");
										disableNode.setUserObject(disable);
									}
									for (int k = 0; k < options.length; k++) {
										Option option = options[k];
										String optionName = option.getInternalName();
										DatasetConfigTreeNode optionNode = new DatasetConfigTreeNode("Option: " + optionName);
										optionNode.setUserObject(option);

										// code for options within options ie for expression menus
										Option[] subOptions = option.getOptions();
										for (int m = 0; m < subOptions.length; m++) {
											Option op = subOptions[m];
											String paoptionName = op.getInternalName();
											DatasetConfigTreeNode subOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
											subOptionNode.setUserObject(op);
										}
										// new code to cycle through push actions
										PushAction[] pushActions = option.getPushActions();
										for (int l = 0; l < pushActions.length; l++) {
											PushAction pa = pushActions[l];
											DatasetConfigTreeNode pushActionNode = new DatasetConfigTreeNode("PushAction");
											pushActionNode.setUserObject(pa);

											Option[] paOptions = pa.getOptions();
											for (int m = 0; m < paOptions.length; m++) {
												Option op = paOptions[m];
												String paoptionName = op.getInternalName();
												DatasetConfigTreeNode paOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
												paOptionNode.setUserObject(op);
											}

										}
										//end of new code
									}
								}
							}
						}
					}
				}
			}
			AttributePage[] apages = dsv.getAttributePages();
			for (int i = 0; i < apages.length; i++) {
				if (apages[i].getClass().getName().equals("org.ensembl.mart.lib.config.AttributePage")) {
					AttributePage ap = apages[i];
					String apName = ap.getInternalName();
					DatasetConfigTreeNode apNode = new DatasetConfigTreeNode("AttributePage:" + apName);
					apNode.setUserObject(ap);
					this.add(apNode);
					List groups = ap.getAttributeGroups();
					for (int j = 0; j < groups.size(); j++) {
						if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.AttributeGroup")) {
							AttributeGroup atGroup = (AttributeGroup) groups.get(j);
							String grName = atGroup.getInternalName();
							DatasetConfigTreeNode grNode = new DatasetConfigTreeNode("AttributeGroup:" + grName);
							grNode.setUserObject(atGroup);
							AttributeCollection[] collections = atGroup.getAttributeCollections();
							for (int z = 0; z < collections.length; z++) {
								AttributeCollection atCollection = collections[z];
								String colName = atCollection.getInternalName();
								DatasetConfigTreeNode colNode = new DatasetConfigTreeNode("AttributeCollection:" + colName);
								colNode.setUserObject(atCollection);
								List descriptions = atCollection.getAttributeDescriptions();
								for (int y = 0; y < descriptions.size(); y++) {
									AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
									String desName = atDescription.getInternalName();
									DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("AttributeDescription:" + desName);
									desNode.setUserObject(atDescription);
								}
							}
						} else if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.DSAttributeGroup")) {
							DSAttributeGroup atGroup = (DSAttributeGroup) groups.get(j);
							String grName = atGroup.getInternalName();
							DatasetConfigTreeNode grNode = new DatasetConfigTreeNode("DSAttributeGroup:" + grName);
							grNode.setUserObject(atGroup);
						}
					}

				}
			}
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterPage")) {
			setName("FilterPage: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			FilterPage fp = (FilterPage) obj;
			List groups = fp.getFilterGroups();
			for (int j = 0; j < groups.size(); j++) {
				if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.FilterGroup")) {
					FilterGroup fiGroup = (FilterGroup) groups.get(j);
					String grName = fiGroup.getInternalName();
					DatasetConfigTreeNode grNode = new DatasetConfigTreeNode("FilterGroup:" + grName);
					grNode.setUserObject(fiGroup);
					this.add(grNode);
					FilterCollection[] collections = fiGroup.getFilterCollections();
					for (int z = 0; z < collections.length; z++) {
						FilterCollection fiCollection = collections[z];
						String colName = fiCollection.getInternalName();
						DatasetConfigTreeNode colNode = new DatasetConfigTreeNode("FilterCollection:" + colName);
						colNode.setUserObject(fiCollection);
						List descriptions = fiCollection.getFilterDescriptions();
						for (int y = 0; y < descriptions.size(); y++) {
							FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
							String desName = fiDescription.getInternalName();
							DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("FilterDescription:" + desName);
							desNode.setUserObject(fiDescription);
							Enable[] enables = fiDescription.getEnables();
							Disable[] disables = fiDescription.getDisables();
							Option[] options = fiDescription.getOptions();

							for (int k = 0; k < enables.length; k++) {
								Enable enable = enables[k];
								DatasetConfigTreeNode enableNode = new DatasetConfigTreeNode("Enable");
								enableNode.setUserObject(enable);
							}
							for (int k = 0; k < disables.length; k++) {
								Disable disable = disables[k];
								DatasetConfigTreeNode disableNode = new DatasetConfigTreeNode("Disable");
								disableNode.setUserObject(disable);
							}
							for (int k = 0; k < options.length; k++) {
								Option option = options[k];
								String optionName = option.getInternalName();
								DatasetConfigTreeNode optionNode = new DatasetConfigTreeNode("Option: " + optionName);
								optionNode.setUserObject(option);

								// code for options within options ie for expression menus
								Option[] subOptions = option.getOptions();
								for (int m = 0; m < subOptions.length; m++) {
									Option op = subOptions[m];
									String paoptionName = op.getInternalName();
									DatasetConfigTreeNode subOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
									subOptionNode.setUserObject(op);
								}
								// new code to cycle through push actions
								PushAction[] pushActions = option.getPushActions();
								for (int l = 0; l < pushActions.length; l++) {
									PushAction pa = pushActions[l];
									DatasetConfigTreeNode pushActionNode = new DatasetConfigTreeNode("PushAction");
									pushActionNode.setUserObject(pa);
									Option[] paOptions = pa.getOptions();
									for (int m = 0; m < paOptions.length; m++) {
										Option op = paOptions[m];
										String paoptionName = op.getInternalName();
										DatasetConfigTreeNode paOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
										paOptionNode.setUserObject(op);
									}
								}
								//end of new code
							}
						}
					}
				}
			}

		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributePage")) {
			setName("AttributePage: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			AttributePage atPage = (AttributePage) obj;
			List groups = atPage.getAttributeGroups();
			for (int j = 0; j < groups.size(); j++) {
				if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.AttributeGroup")) {
					AttributeGroup atGroup = (AttributeGroup) groups.get(j);
					String grName = atGroup.getInternalName();
					DatasetConfigTreeNode grNode = new DatasetConfigTreeNode("AttributeGroup:" + grName);
					grNode.setUserObject(atGroup);
					this.add(grNode);
					AttributeCollection[] collections = atGroup.getAttributeCollections();
					for (int z = 0; z < collections.length; z++) {
						AttributeCollection atCollection = collections[z];
						String colName = atCollection.getInternalName();
						DatasetConfigTreeNode colNode = new DatasetConfigTreeNode("AttributeCollection:" + colName);
						colNode.setUserObject(atCollection);
						List descriptions = atCollection.getAttributeDescriptions();
						for (int y = 0; y < descriptions.size(); y++) {
							AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
							String desName = atDescription.getInternalName();
							DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("AttributeDescription:" + desName);
							desNode.setUserObject(atDescription);
						}
					}
				} else if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.DSAttributeGroup")) {
					DSAttributeGroup atGroup = (DSAttributeGroup) groups.get(j);
					String grName = atGroup.getInternalName();
					DatasetConfigTreeNode grNode = new DatasetConfigTreeNode("DSAttributeGroup:" + grName);
					grNode.setUserObject(atGroup);
					this.add(grNode);
				}
			}

		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterGroup")) {
			setName("FilterGroup: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			FilterGroup fiGroup = (FilterGroup) obj;
			FilterCollection[] collections = fiGroup.getFilterCollections();
			for (int z = 0; z < collections.length; z++) {
				FilterCollection fiCollection = collections[z];
				String colName = fiCollection.getInternalName();
				DatasetConfigTreeNode colNode = new DatasetConfigTreeNode("FilterCollection:" + colName);
				colNode.setUserObject(fiCollection);
				this.add(colNode);
				List descriptions = fiCollection.getFilterDescriptions();
				for (int y = 0; y < descriptions.size(); y++) {
					FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
					String desName = fiDescription.getInternalName();
					DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("FilterDescription:" + desName);
					desNode.setUserObject(fiDescription);
					Enable[] enables = fiDescription.getEnables();
					Disable[] disables = fiDescription.getDisables();
					Option[] options = fiDescription.getOptions();
					for (int k = 0; k < enables.length; k++) {
						Enable enable = enables[k];
						DatasetConfigTreeNode enableNode = new DatasetConfigTreeNode("Enable");
						enableNode.setUserObject(enable);
					}
					for (int k = 0; k < disables.length; k++) {
						Disable disable = disables[k];
						DatasetConfigTreeNode disableNode = new DatasetConfigTreeNode("Disable");
						disableNode.setUserObject(disable);
					}
					for (int k = 0; k < options.length; k++) {
						Option option = options[k];
						String optionName = option.getInternalName();
						DatasetConfigTreeNode optionNode = new DatasetConfigTreeNode("Option: " + optionName);
						optionNode.setUserObject(option);
						// code for options within options ie for expression menus
						Option[] subOptions = option.getOptions();
						for (int m = 0; m < subOptions.length; m++) {
							Option op = subOptions[m];
							String paoptionName = op.getInternalName();
							DatasetConfigTreeNode subOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
							subOptionNode.setUserObject(op);
						}
						// new code to cycle through push actions
						PushAction[] pushActions = option.getPushActions();
						for (int l = 0; l < pushActions.length; l++) {
							PushAction pa = pushActions[l];
							DatasetConfigTreeNode pushActionNode = new DatasetConfigTreeNode("PushAction");
							pushActionNode.setUserObject(pa);
							Option[] paOptions = pa.getOptions();
							for (int m = 0; m < paOptions.length; m++) {
								Option op = paOptions[m];
								String paoptionName = op.getInternalName();
								DatasetConfigTreeNode paOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
								paOptionNode.setUserObject(op);
							}
						}
						//end of new code
					}
				}
			}
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeGroup")) {
			setName("AttributGroup: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			AttributeGroup atGroup = (AttributeGroup) obj;
			AttributeCollection[] collections = atGroup.getAttributeCollections();
			for (int z = 0; z < collections.length; z++) {
				AttributeCollection atCollection = collections[z];
				String colName = atCollection.getInternalName();
				DatasetConfigTreeNode colNode = new DatasetConfigTreeNode("AttributeCollection:" + colName);
				this.add(colNode);
				colNode.setUserObject(atCollection);
				List descriptions = atCollection.getAttributeDescriptions();
				for (int y = 0; y < descriptions.size(); y++) {
					AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
					String desName = atDescription.getInternalName();
					DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("AttributeDescription:" + desName);
					desNode.setUserObject(atDescription);
					//colNode.add(desNode);
				}
			}
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.DSAttributeGroup")) {
			setName("DSAttributGroup: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			DSAttributeGroup atGroup = (DSAttributeGroup) obj;

		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterCollection")) {
			setName("FilterCollection: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			FilterCollection fiCollection = (FilterCollection) obj;
			List descriptions = fiCollection.getFilterDescriptions();
			for (int y = 0; y < descriptions.size(); y++) {
				FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
				String desName = fiDescription.getInternalName();
				DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("FilterDescription:" + desName);
				desNode.setUserObject(fiDescription);
				this.add(desNode);
				Enable[] enables = fiDescription.getEnables();
				Disable[] disables = fiDescription.getDisables();
				Option[] options = fiDescription.getOptions();

				for (int k = 0; k < enables.length; k++) {
					Enable enable = enables[k];
					DatasetConfigTreeNode enableNode = new DatasetConfigTreeNode("Enable");
					enableNode.setUserObject(enable);
				}
				for (int k = 0; k < disables.length; k++) {
					Disable disable = disables[k];
					DatasetConfigTreeNode disableNode = new DatasetConfigTreeNode("Disable");
					disableNode.setUserObject(disable);
				}
				for (int k = 0; k < options.length; k++) {
					Option option = options[k];
					String optionName = option.getInternalName();
					DatasetConfigTreeNode optionNode = new DatasetConfigTreeNode("Option: " + optionName);
					optionNode.setUserObject(option);

					// code for options within options ie for expression menus
					Option[] subOptions = option.getOptions();
					for (int m = 0; m < subOptions.length; m++) {
						Option op = subOptions[m];
						String paoptionName = op.getInternalName();
						DatasetConfigTreeNode subOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
						subOptionNode.setUserObject(op);
					}

					// new code to cycle through push actions
					PushAction[] pushActions = option.getPushActions();
					for (int l = 0; l < pushActions.length; l++) {
						PushAction pa = pushActions[l];
						DatasetConfigTreeNode pushActionNode = new DatasetConfigTreeNode("PushAction");
						pushActionNode.setUserObject(pa);
						Option[] paOptions = pa.getOptions();
						for (int m = 0; m < paOptions.length; m++) {
							Option op = paOptions[m];
							String paoptionName = op.getInternalName();
							DatasetConfigTreeNode paOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
							paOptionNode.setUserObject(op);
						}
					}
					//end of new code
				}
			}
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeCollection")) {
			setName("AttributeCollection: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			AttributeCollection atCollection = (AttributeCollection) obj;
			List descriptions = atCollection.getAttributeDescriptions();
			for (int y = 0; y < descriptions.size(); y++) {
				AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
				String desName = atDescription.getInternalName();
				DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("AttributeDescription:" + desName);
				desNode.setUserObject(atDescription);
				this.add(desNode);
			}
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterDescription")) {
			setName("FilterDescription: " + ((BaseNamedConfigurationObject) obj).getInternalName());

			//setName("FilterCollection: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			FilterDescription fiDescription = (FilterDescription) obj;
			Enable[] enables = fiDescription.getEnables();
			Disable[] disables = fiDescription.getDisables();
			Option[] ops = fiDescription.getOptions();
			for (int k = 0; k < enables.length; k++) {
				Enable enable = enables[k];
				DatasetConfigTreeNode enableNode = new DatasetConfigTreeNode("Enable");
				enableNode.setUserObject(enable);
				this.add(enableNode);
			}
			for (int k = 0; k < disables.length; k++) {
				Disable disable = disables[k];
				DatasetConfigTreeNode disableNode = new DatasetConfigTreeNode("Disable");
				disableNode.setUserObject(disable);
				this.add(disableNode);
			}
			for (int y = 0; y < ops.length; y++) {
				Option option = (Option) ops[y];
				String desName = option.getInternalName();
				DatasetConfigTreeNode desNode = new DatasetConfigTreeNode("Option:" + desName);
				desNode.setUserObject(option);
				this.add(desNode);

				// code for options within options ie for expression menus
				Option[] subOptions = option.getOptions();
				for (int m = 0; m < subOptions.length; m++) {
					Option op = subOptions[m];
					String paoptionName = op.getInternalName();
					DatasetConfigTreeNode subOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
					subOptionNode.setUserObject(op);
				}
				//				new code to cycle through push actions
				PushAction[] pushActions = option.getPushActions();
				for (int l = 0; l < pushActions.length; l++) {
					PushAction pa = pushActions[l];
					DatasetConfigTreeNode pushActionNode = new DatasetConfigTreeNode("PushAction");
					pushActionNode.setUserObject(pa);
					Option[] paOptions = pa.getOptions();
					for (int m = 0; m < paOptions.length; m++) {
						Option op = paOptions[m];
						String paoptionName = op.getInternalName();
						DatasetConfigTreeNode paOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
						paOptionNode.setUserObject(op);
					}
				}
				//end of new code
			}
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeDescription")) {
			setName("AttributeDescription: " + ((BaseNamedConfigurationObject) obj).getInternalName());
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Exportable")) {
			setName("Exportable: " + ((BaseNamedConfigurationObject) obj).getInternalName());
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Importable")) {
			setName("Importable: " + ((BaseNamedConfigurationObject) obj).getInternalName());
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Enable")) {
			setName("Enable");
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Disable")) {
			setName("Disable");
		}

		//else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.PushAction")) {
		//    setName("Push Action");
		//}

		else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Option")) {
			setName("Option: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			Option op = (Option) obj;

			// code for options within options ie for expression menus
			Option[] subOptions = op.getOptions();
			for (int m = 0; m < subOptions.length; m++) {
				Option op2 = subOptions[m];
				String paoptionName = op2.getInternalName();
				DatasetConfigTreeNode subOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
				subOptionNode.setUserObject(op2);
				this.add(subOptionNode);
			}

			PushAction[] pushActions = op.getPushActions();
			for (int k = 0; k < pushActions.length; k++) {
				PushAction pa = pushActions[k];

				DatasetConfigTreeNode pushActionNode = new DatasetConfigTreeNode("PushAction");
				pushActionNode.setUserObject(pa);

				this.add(pushActionNode);
				Option[] paOptions = pa.getOptions();
				for (int m = 0; m < paOptions.length; m++) {
					Option paop = paOptions[m];
					String paoptionName = paop.getInternalName();
					DatasetConfigTreeNode paOptionNode = new DatasetConfigTreeNode("Option: " + paoptionName);
					paOptionNode.setUserObject(paop);
				}
			}
		} else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.PushAction")) {
			setName("Push Action: " + ((BaseNamedConfigurationObject) obj).getInternalName());
			PushAction pa = (PushAction) obj;
			Option[] paOptions = pa.getOptions();
			for (int k = 0; k < paOptions.length; k++) {
				Option op = paOptions[k];
				String paoptionName = op.getInternalName();
				DatasetConfigTreeNode paOptionNode = new DatasetConfigTreeNode("Option" + paoptionName);
				paOptionNode.setUserObject(op);
				this.add(paOptionNode);
			}

		}
	}
}
