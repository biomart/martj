package org.ensembl.mart.vieweditor;

import org.ensembl.mart.lib.config.*;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.MutableTreeNode;
import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: katerina
 * Date: 18-Nov-2003
 * Time: 18:35:08
 * To change this template use Options | File Templates.
 */
public class DatasetViewTreeModel extends DefaultTreeModel {

// This class represents the data model for this tree
    DatasetViewTreeNode node;
    DatasetView view;

    public DatasetViewTreeModel(DatasetViewTreeNode node, DatasetView view) {
        super(node);
        this.node = node;
        this.view = view;
    }

    public void valueForPathChanged(TreePath path,
                                    Object newValue) {
        DatasetViewTreeNode current_node =
                (DatasetViewTreeNode) path.getLastPathComponent();
        String value = (String) newValue;
        System.out.println("in model valueForPathChanged");
        current_node.setName(value);
        BaseNamedConfigurationObject nodeInfo = (BaseNamedConfigurationObject) current_node.getUserObject();
        //nodeInfo.setInternalName(value);
        //System.out.println(view.containsAttributePage(value));

        nodeChanged(current_node);
    }

    public String insertNodeInto(DatasetViewTreeNode editingNode, DatasetViewTreeNode parentNode, int index) {
        int start, finish;
        String parentClassName = (parentNode.getUserObject().getClass()).getName();
        String childClassName = (editingNode.getUserObject().getClass()).getName();
        start = childClassName.lastIndexOf(".") + 1;
        finish = childClassName.length();
        String childName = childClassName.substring(start, finish);

        if (parentClassName.equals("org.ensembl.mart.lib.config.DatasetView")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.FilterPage")) {
                view = (DatasetView) parentNode.getUserObject();
                //view.addFilterPage((FilterPage) editingNode.getUserObject());

            } else if (childClassName.equals("org.ensembl.mart.lib.config.AttributePage")) {
                view = (DatasetView) parentNode.getUserObject();
                //view.addAttributePage((AttributePage) editingNode.getUserObject());

            }else {
                String error_string = "Error: " + childName + " cannot be inserted in a DatasetView.";
                return error_string;
            }
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterPage")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.FilterGroup")) {
                FilterPage fp = (FilterPage) parentNode.getUserObject();
               // int position = view.indexOfFilterPage(fp);
                fp.addFilterGroup((FilterGroup) editingNode.getUserObject());
               // view.setFilterPageAt(position,fp);
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in a FilterPage.";
                return error_string;
            }
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterGroup")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.FilterCollection")) {
                FilterGroup fg = (FilterGroup) parentNode.getUserObject();
               // FilterPage fp = (FilterPage)((DatasetViewTreeNode)(parentNode.getParent())).getUserObject();
               // int fgPosition = fp.indexOfFilterGroup(fg);
               // int fpPosition = view.indexOfFilterPage(fp);
                fg.addFilterCollection((FilterCollection) editingNode.getUserObject());
               // fp.setFilterGroupAt(fgPosition,fg);
               // view.setFilterPageAt(fpPosition,fp);
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in a FilterGroup.";
                return error_string;
            }
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterCollection")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.FilterDescription")) {
                FilterCollection fc = (FilterCollection) parentNode.getUserObject();
                fc.addFilterDescription((FilterDescription) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in a FilterCollection.";
                return error_string;
            }
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.FilterDescription")) {
            String error_string = "Error: a FilterDescription is a leaf node, no insertions are allowed.";
            return error_string;
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributePage")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.AttributeGroup")) {
                AttributePage ap = (AttributePage) parentNode.getUserObject();
                ap.addAttributeGroup((AttributeGroup) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in an AttributePage.";
                return error_string;
            }
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributeGroup")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.AttributeCollection")) {
                AttributeGroup ag = (AttributeGroup) parentNode.getUserObject();
                ag.addAttributeCollection((AttributeCollection) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in an AttributeGroup.";
                return error_string;
            }
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributeCollection")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.AttributeDescription")) {
                AttributeCollection ac = (AttributeCollection) parentNode.getUserObject();
                ac.addAttributeDescription((AttributeDescription) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in an AttributeCollection.";
                return error_string;
            }
        } else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributeDescription")) {
            String error_string = "Error: AttributeDescription is a leaf node, no insertions are allowed.";
            return error_string;
        }
        System.out.println(index);
        super.insertNodeInto(editingNode, parentNode, index);
        return "success";
    }

    public void removeNodeFromParent(DatasetViewTreeNode editingNode) {
        super.removeNodeFromParent(editingNode);

    }
}
