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

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.ensembl.mart.lib.config.*;

/**
 * Class DatasetConfigTreeModel extends DefaultTreeModel.
 *
 * <p>This class is written for the attributes table to implement autoscroll
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetConfig
 */

public class DatasetConfigTreeModel extends DefaultTreeModel {

	// This class represents the data model for this tree
	DatasetConfigTreeNode node;
	DatasetConfig config;

	public DatasetConfigTreeModel(DatasetConfigTreeNode node, DatasetConfig config) {
		super(node);
		this.node = node;
		this.config = config;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		DatasetConfigTreeNode current_node = (DatasetConfigTreeNode) path.getLastPathComponent();
		String value = (String) newValue;
		System.out.println("in model valueForPathChanged");
		current_node.setName(value);
		BaseConfigurationObject nodeInfo = (BaseConfigurationObject) current_node.getUserObject();
		//nodeInfo.setInternalName(value);
		//System.out.println(config.containsAttributePage(value));

		nodeChanged(current_node);
	}

	public void reload(DatasetConfigTreeNode editingNode, DatasetConfigTreeNode parentNode) {
		int start, finish;
		String parentClassName = (parentNode.getUserObject().getClass()).getName();
		String childClassName = (editingNode.getUserObject().getClass()).getName();
		start = childClassName.lastIndexOf(".") + 1;
		finish = childClassName.length();
		String childName = childClassName.substring(start, finish);

		if (parentClassName.equals("org.ensembl.mart.lib.config.DatasetConfig")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.FilterPage")) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.addFilterPage((FilterPage) editingNode.getUserObject());
				//config.removeAttributePage();

			} else if (childClassName.equals("org.ensembl.mart.lib.config.AttributePage")) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.addAttributePage((AttributePage) editingNode.getUserObject());

			} else if (childClassName.equals("org.ensembl.mart.lib.config.Importable")) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.addImportable((Importable) editingNode.getUserObject());

			} else if (childClassName.equals("org.ensembl.mart.lib.config.Exportable")) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.addExportable((Exportable) editingNode.getUserObject());

			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterPage")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.FilterGroup")) {
				FilterPage fp = (FilterPage) parentNode.getUserObject();
				fp.addFilterGroup((FilterGroup) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterGroup")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.FilterCollection")) {
				FilterGroup fg = (FilterGroup) parentNode.getUserObject();
				fg.addFilterCollection((FilterCollection) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterCollection")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.FilterDescription")) {
				FilterCollection fc = (FilterCollection) parentNode.getUserObject();
				fc.addFilterDescription((FilterDescription) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterDescription")) {

			if (childClassName.equals("org.ensembl.mart.lib.config.Option")) {
				FilterDescription fd = (FilterDescription) parentNode.getUserObject();
				fd.addOption((Option) editingNode.getUserObject());

			} 

		} else if (parentClassName.equals("org.ensembl.mart.lib.config.Option")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.PushAction")) {
				Option op = (Option) parentNode.getUserObject();
				op.addPushAction((PushAction) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.PushAction")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.Option")) {
				PushAction pa = (PushAction) parentNode.getUserObject();
				pa.addOption((Option) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributePage")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.AttributeGroup")) {
				AttributePage ap = (AttributePage) parentNode.getUserObject();
				ap.addAttributeGroup((AttributeGroup) editingNode.getUserObject());
			} 
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributeGroup")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.AttributeCollection")) {
				AttributeGroup ag = (AttributeGroup) parentNode.getUserObject();
				ag.addAttributeCollection((AttributeCollection) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributeCollection")) {
			if (childClassName.equals("org.ensembl.mart.lib.config.AttributeDescription")) {
				AttributeCollection ac = (AttributeCollection) parentNode.getUserObject();
				ac.addAttributeDescription((AttributeDescription) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributeDescription")) {

		}
		super.reload(parentNode);

	}

	public String insertNodeInto(DatasetConfigTreeNode editingNode, DatasetConfigTreeNode parentNode, int index){
		int start, finish;
		Object child = editingNode.getUserObject();
		Object parent = parentNode.getUserObject();
		String childClassName = (editingNode.getUserObject().getClass()).getName();
		start = childClassName.lastIndexOf(".") + 1;
		finish = childClassName.length();
		String childName = childClassName.substring(start, finish);

		//index is a Node index. objectIndex may be different
		int objIndex = index - DatasetConfigTreeNode.getHeterogenousOffset(parent, child);
		if (parent instanceof org.ensembl.mart.lib.config.DatasetConfig) {
			if (child instanceof org.ensembl.mart.lib.config.FilterPage) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.insertFilterPage(objIndex, (FilterPage) editingNode.getUserObject());

			} else if (child instanceof org.ensembl.mart.lib.config.AttributePage) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.insertAttributePage(objIndex, (AttributePage) editingNode.getUserObject());


			} else if (child instanceof org.ensembl.mart.lib.config.Importable) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.insertImportable(objIndex, (Importable) editingNode.getUserObject());

			} else if (child instanceof org.ensembl.mart.lib.config.Exportable) {
				config = (DatasetConfig) parentNode.getUserObject();
				config.insertExportable(objIndex, (Exportable) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a DatasetConfig.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
			if (child instanceof org.ensembl.mart.lib.config.FilterGroup) {
				FilterPage fp = (FilterPage) parentNode.getUserObject();
				fp.insertFilterGroup(objIndex, (FilterGroup) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a FilterPage.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
			if (child instanceof org.ensembl.mart.lib.config.FilterCollection) {
				FilterGroup fg = (FilterGroup) parentNode.getUserObject();
				fg.insertFilterCollection(objIndex, (FilterCollection) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a FilterGroup.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
			if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
				FilterCollection fc = (FilterCollection) parentNode.getUserObject();
				fc.insertFilterDescription(objIndex, (FilterDescription) editingNode.getUserObject());
			} else if (child instanceof org.ensembl.mart.lib.config.Option) {
				FilterCollection fc = (FilterCollection) parentNode.getUserObject();
				FilterDescription fdConvert = new FilterDescription((Option) editingNode.getUserObject());
				fc.insertFilterDescription(objIndex, fdConvert);
				editingNode.setUserObject(fdConvert);
			} else if (child instanceof org.ensembl.mart.lib.config.AttributeDescription) {
				FilterCollection fc = (FilterCollection) parentNode.getUserObject();
				//FilterDescription fdConvert = new FilterDescription((AttributeDescription) editingNode.getUserObject());
				AttributeDescription ad = (AttributeDescription) editingNode.getUserObject();
				
				FilterDescription fdConvert = null;
				try{
					fdConvert = new FilterDescription(ad.getInternalName(),
																	ad.getField(),
																	"text",
																	"=",
																	"=",
																	ad.getDisplayName(),
																	ad.getTableConstraint(),
																	ad.getKey(),
																	ad.getDescription(),
																	"",
																	"",
																	"",
																	"",
																	"",
																	"");
				}
				catch (ConfigurationException e){
					// guaranteed internal name for atts
				}
				fc.insertFilterDescription(objIndex, fdConvert);
				editingNode.setUserObject(fdConvert);
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a FilterCollection.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterDescription) {
			if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
				FilterDescription fd = (FilterDescription) parentNode.getUserObject();
				Option opConvert = new Option((FilterDescription) editingNode.getUserObject());
				fd.insertOption(objIndex, opConvert);
				editingNode.setUserObject(opConvert);
			} else if (child instanceof org.ensembl.mart.lib.config.Option) {
				FilterDescription fd = (FilterDescription) parentNode.getUserObject();
				fd.insertOption(objIndex, (Option) editingNode.getUserObject());
			} else if (child instanceof org.ensembl.mart.lib.config.PushAction) {
				FilterDescription fd = (FilterDescription) parentNode.getUserObject();
				//fd.insertPushAction(objIndex, (PushAction) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a FilterDescription.";
				return error_string;
			}
		}  else if (parent instanceof org.ensembl.mart.lib.config.Option) {
			if (child instanceof org.ensembl.mart.lib.config.PushAction) {
				Option op = (Option) parentNode.getUserObject();
				op.insertPushAction(objIndex, (PushAction) editingNode.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.PushAction) {
			if (child instanceof org.ensembl.mart.lib.config.Option) {
				PushAction pa = (PushAction) parentNode.getUserObject();
				pa.insertOption(objIndex, (Option) editingNode.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
			if (child instanceof org.ensembl.mart.lib.config.AttributeGroup) {
				AttributePage ap = (AttributePage) parentNode.getUserObject();
				ap.insertAttributeGroup(objIndex, (AttributeGroup) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in an AttributePage.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
			if (child instanceof org.ensembl.mart.lib.config.AttributeCollection) {
				AttributeGroup ag = (AttributeGroup) parentNode.getUserObject();
				ag.insertAttributeCollection(objIndex, (AttributeCollection) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in an AttributeGroup.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
			if (child instanceof org.ensembl.mart.lib.config.AttributeDescription) {
				AttributeCollection ac = (AttributeCollection) parentNode.getUserObject();
				ac.insertAttributeDescription(objIndex, (AttributeDescription) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in an AttributeCollection.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributeDescription) {
			String error_string = "Error: AttributeDescription is a leaf node, no insertions are allowed.";
			return error_string;
		}
		super.insertNodeInto(editingNode, parentNode, index);
		return "success";
	}

	public void removeNodeFromParent(DatasetConfigTreeNode node) {
		Object child = node.getUserObject();
		Object parent = ((DatasetConfigTreeNode) node.getParent()).getUserObject();
		if (parent instanceof org.ensembl.mart.lib.config.DatasetConfig) {
			if (child instanceof org.ensembl.mart.lib.config.FilterPage) {
				config = (DatasetConfig) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				config.removeFilterPage((FilterPage) node.getUserObject());
			} else if (child instanceof org.ensembl.mart.lib.config.AttributePage) {
				config = (DatasetConfig) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				config.removeAttributePage((AttributePage) node.getUserObject());
			} else if (child instanceof org.ensembl.mart.lib.config.Exportable) {
				config = (DatasetConfig) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				config.removeExportable((Exportable) node.getUserObject());
			} else if (child instanceof org.ensembl.mart.lib.config.Importable) {
				config = (DatasetConfig) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				config.removeImportable((Importable) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
			if (child instanceof org.ensembl.mart.lib.config.FilterGroup) {
				FilterPage fp = (FilterPage) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				fp.removeFilterGroup((FilterGroup) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
			if (child instanceof org.ensembl.mart.lib.config.FilterCollection) {
				FilterGroup fg = (FilterGroup) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				fg.removeFilterCollection((FilterCollection) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
			if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
				FilterCollection fc = (FilterCollection) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				fc.removeFilterDescription((FilterDescription) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.FilterDescription) {
		    if (child instanceof org.ensembl.mart.lib.config.Option) {
				FilterDescription fd = (FilterDescription) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				fd.removeOption((Option) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.Option) {
			if (child instanceof org.ensembl.mart.lib.config.PushAction) {
				Option op = (Option) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				op.removePushAction((PushAction) node.getUserObject());
			} else if (child instanceof org.ensembl.mart.lib.config.Option) {
				Option fd = (Option) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				fd.removeOption((Option) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.PushAction) {
			if (child instanceof org.ensembl.mart.lib.config.Option) {
				PushAction pa = (PushAction) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				pa.removeOption((Option) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
			if (child instanceof org.ensembl.mart.lib.config.AttributeGroup) {
				AttributePage ap = (AttributePage) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				ap.removeAttributeGroup((AttributeGroup) node.getUserObject());
			} 
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
			if (child instanceof org.ensembl.mart.lib.config.AttributeCollection) {
				AttributeGroup ag = (AttributeGroup) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				ag.removeAttributeCollection((AttributeCollection) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
			if (child instanceof org.ensembl.mart.lib.config.AttributeDescription) {
				AttributeCollection ac = (AttributeCollection) ((DatasetConfigTreeNode) node.getParent()).getUserObject();
				ac.removeAttributeDescription((AttributeDescription) node.getUserObject());
			}
		}
		super.removeNodeFromParent(node);

	}
}
