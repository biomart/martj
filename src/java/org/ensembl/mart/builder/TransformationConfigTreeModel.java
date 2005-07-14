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

package org.ensembl.mart.builder;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.ensembl.mart.builder.config.*;

/**
 * Class TransformationConfigTreeModel extends DefaultTreeModel.
 *
 * <p>This class is written for the attributes table to implement autoscroll
 * </p>
 *
 * @author <a href="mailto:damian@ebi.ac.uk">Damian Smedley</a>
 * //@see org.ensembl.mart.config.TransformationConfig
 */

public class TransformationConfigTreeModel extends DefaultTreeModel {

	// This class represents the data model for this tree
	TransformationConfigTreeNode node;
	TransformationConfig config;

	public TransformationConfigTreeModel(TransformationConfigTreeNode node, TransformationConfig config) {
		super(node);
		this.node = node;
		this.config = config;
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		TransformationConfigTreeNode current_node = (TransformationConfigTreeNode) path.getLastPathComponent();
		String value = (String) newValue;
		System.out.println("in model valueForPathChanged");
		current_node.setName(value);
		BaseConfigurationObject nodeInfo = (BaseConfigurationObject) current_node.getUserObject();
		nodeChanged(current_node);
	}

	public void reload(TransformationConfigTreeNode editingNode, TransformationConfigTreeNode parentNode) {
		int start, finish;
		String parentClassName = (parentNode.getUserObject().getClass()).getName();
		String childClassName = (editingNode.getUserObject().getClass()).getName();
		start = childClassName.lastIndexOf(".") + 1;
		finish = childClassName.length();
		String childName = childClassName.substring(start, finish);

		if (parentClassName.equals("org.ensembl.mart.builder.config.TransformationConfig")) {
			if (childClassName.equals("org.ensembl.mart.builder.config.Dataset")) {
				config = (TransformationConfig) parentNode.getUserObject();
				System.out.println("MODEL: ADDING DATASET");
				config.addDataset((Dataset) editingNode.getUserObject());
				//config.removeAttributePage();

			} 
		} else if (parentClassName.equals("org.ensembl.mart.builder.config.Dataset")) {
			if (childClassName.equals("org.ensembl.mart.builder.config.Transformation")) {
				Dataset fp = (Dataset) parentNode.getUserObject();
				System.out.println("MODEL: ADDING TRANSFORMATION");
				fp.addTransformation((Transformation) editingNode.getUserObject());
			}
		} else if (parentClassName.equals("org.ensembl.mart.builder.config.Transformation")) {
			if (childClassName.equals("org.ensembl.mart.builder.config.TransformationUnit")) {
				Transformation fp = (Transformation) parentNode.getUserObject();
				System.out.println("MODEL: ADDING TUNIT");
				fp.addTransformationUnit((TransformationUnit) editingNode.getUserObject());
			}
		} 
		super.reload(parentNode);

	}

	public String insertNodeInto(TransformationConfigTreeNode editingNode, TransformationConfigTreeNode parentNode, int index){
		int start, finish;
		Object child = editingNode.getUserObject();
		Object parent = parentNode.getUserObject();
		String childClassName = (editingNode.getUserObject().getClass()).getName();
		start = childClassName.lastIndexOf(".") + 1;
		finish = childClassName.length();
		String childName = childClassName.substring(start, finish);

		//index is a Node index. objectIndex may be different
		int objIndex = index - TransformationConfigTreeNode.getHeterogenousOffset(parent, child);
		if (parent instanceof org.ensembl.mart.builder.config.TransformationConfig) {
			if (child instanceof org.ensembl.mart.builder.config.Dataset) {
				config = (TransformationConfig) parentNode.getUserObject();
				System.out.println("MODEL: INSERTING DATASET");
				config.insertDataset(objIndex, (Dataset) editingNode.getUserObject());

			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a TransformationConfig.";
				return error_string;
			}
		} else if (parent instanceof org.ensembl.mart.builder.config.Dataset) {
			if (child instanceof org.ensembl.mart.builder.config.Transformation) {
				Dataset fp = (Dataset) parentNode.getUserObject();
				fp.insertTransformation(objIndex, (Transformation) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a Transformation.";
				return error_string;
			}
		}  else if (parent instanceof org.ensembl.mart.builder.config.Transformation) {
			if (child instanceof org.ensembl.mart.builder.config.TransformationUnit) {
				Transformation fp = (Transformation) parentNode.getUserObject();
				fp.insertTransformationUnit(objIndex, (TransformationUnit) editingNode.getUserObject());
			} else {
				String error_string = "Error: " + childName + " cannot be inserted in a Transformation.";
				return error_string;
			}
		} 
		else if (parent instanceof org.ensembl.mart.builder.config.TransformationUnit) {
			String error_string = "Error: TransformationUnit is a leaf node, no insertions are allowed.";
			return error_string;
		}
		super.insertNodeInto(editingNode, parentNode, index);
		return "success";
	}

	public void removeNodeFromParent(TransformationConfigTreeNode node) {
		Object child = node.getUserObject();
		Object parent = ((TransformationConfigTreeNode) node.getParent()).getUserObject();
		if (parent instanceof org.ensembl.mart.builder.config.TransformationConfig) {
			if (child instanceof org.ensembl.mart.builder.config.Dataset) {
				config = (TransformationConfig) ((TransformationConfigTreeNode) node.getParent()).getUserObject();
				config.removeDataset((Dataset) node.getUserObject());
			} 
		}  else if (parent instanceof org.ensembl.mart.builder.config.Dataset) {
			if (child instanceof org.ensembl.mart.builder.config.Transformation) {
				Dataset fp = (Dataset) ((TransformationConfigTreeNode) node.getParent()).getUserObject();
				fp.removeTransformation((Transformation) node.getUserObject());
			}
		} else if (parent instanceof org.ensembl.mart.builder.config.Transformation) {
			if (child instanceof org.ensembl.mart.builder.config.TransformationUnit) {
				Transformation fp = (Transformation) ((TransformationConfigTreeNode) node.getParent()).getUserObject();
				fp.removeTransformationUnit((TransformationUnit) node.getUserObject());
			}
		} 
		super.removeNodeFromParent(node);

	}
}
