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

package org.ensembl.mart.vieweditor;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.ensembl.mart.lib.config.*;

/**
 * Class DatasetViewTreeModel extends DefaultTreeModel.
 *
 * <p>This class is written for the attributes table to implement autoscroll
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetView
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
        BaseConfigurationObject nodeInfo = (BaseConfigurationObject) current_node.getUserObject();
        //nodeInfo.setInternalName(value);
        //System.out.println(view.containsAttributePage(value));

        nodeChanged(current_node);
    }

    public void reload(DatasetViewTreeNode editingNode, DatasetViewTreeNode parentNode) {
        int start, finish;
        String parentClassName = (parentNode.getUserObject().getClass()).getName();
        String childClassName = (editingNode.getUserObject().getClass()).getName();
        start = childClassName.lastIndexOf(".") + 1;
        finish = childClassName.length();
        String childName = childClassName.substring(start, finish);

        if (parentClassName.equals("org.ensembl.mart.lib.config.DatasetView")) {
            if (childClassName.equals("org.ensembl.mart.lib.config.FilterPage")) {
                view = (DatasetView) parentNode.getUserObject();
                view.addFilterPage((FilterPage) editingNode.getUserObject());
                //view.removeAttributePage();

            } else if (childClassName.equals("org.ensembl.mart.lib.config.AttributePage")) {
                view = (DatasetView) parentNode.getUserObject();
                view.addAttributePage((AttributePage) editingNode.getUserObject());

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
			else if (childClassName.equals("org.ensembl.mart.lib.config.Enable")) {
				FilterDescription fd = (FilterDescription) parentNode.getUserObject();
				fd.addEnable((Enable) editingNode.getUserObject());
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
	    }
	    else if (parentClassName.equals("org.ensembl.mart.lib.config.AttributePage")) {
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

    public String insertNodeInto(DatasetViewTreeNode editingNode, DatasetViewTreeNode parentNode, int index) {
        int start,finish;
        Object child = editingNode.getUserObject();
        Object parent = parentNode.getUserObject();
        String childClassName = (editingNode.getUserObject().getClass()).getName();
        start = childClassName.lastIndexOf(".") + 1;
        finish = childClassName.length();
        String childName = childClassName.substring(start, finish);
        if (parent instanceof org.ensembl.mart.lib.config.DatasetView) {
            if (child instanceof org.ensembl.mart.lib.config.FilterPage) {
                view = (DatasetView) parentNode.getUserObject();
                view.insertFilterPage(index, (FilterPage) editingNode.getUserObject());

            } else if (child instanceof org.ensembl.mart.lib.config.AttributePage) {
                view = (DatasetView) parentNode.getUserObject();
                view.insertAttributePage(index, (AttributePage) editingNode.getUserObject());

            } else {
                String error_string = "Error: " + childName + " cannot be inserted in a DatasetView.";
                return error_string;
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
            if (child instanceof org.ensembl.mart.lib.config.FilterGroup) {
                FilterPage fp = (FilterPage) parentNode.getUserObject();
                fp.insertFilterGroup(index, (FilterGroup) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in a FilterPage.";
                return error_string;
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
            if (child instanceof org.ensembl.mart.lib.config.FilterCollection) {
                FilterGroup fg = (FilterGroup) parentNode.getUserObject();
                fg.insertFilterCollection(index, (FilterCollection) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in a FilterGroup.";
                return error_string;
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
            if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
                FilterCollection fc = (FilterCollection) parentNode.getUserObject();
                fc.insertFilterDescription(index, (FilterDescription) editingNode.getUserObject());
            } else if (child instanceof org.ensembl.mart.lib.config.Option) {
			    FilterCollection fc = (FilterCollection) parentNode.getUserObject();
			    FilterDescription fdConvert = new FilterDescription((Option) editingNode.getUserObject());
			    fc.insertFilterDescription(index, fdConvert );
			    editingNode.setUserObject(fdConvert);
		    } else {
                String error_string = "Error: " + childName + " cannot be inserted in a FilterCollection.";
                return error_string;
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterDescription) {
            if (child instanceof org.ensembl.mart.lib.config.Enable) {
                FilterDescription fd = (FilterDescription) parentNode.getUserObject();
                fd.insertEnable(index,(Enable) editingNode.getUserObject());
            } else if (child instanceof org.ensembl.mart.lib.config.Disable) {
                FilterDescription fd = (FilterDescription) parentNode.getUserObject();
                fd.insertDisable(index,(Disable) editingNode.getUserObject());
            } else if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
			    FilterDescription fd = (FilterDescription) parentNode.getUserObject();
			    Option opConvert = new Option((FilterDescription) editingNode.getUserObject());
			    fd.insertOption(index, opConvert );
				editingNode.setUserObject(opConvert);
		    }else if (child instanceof org.ensembl.mart.lib.config.Option) {
                FilterDescription fd = (FilterDescription) parentNode.getUserObject();
                fd.insertOption(index, (Option) editingNode.getUserObject());
            } else if (child instanceof org.ensembl.mart.lib.config.PushAction) {
			    FilterDescription fd = (FilterDescription) parentNode.getUserObject();
			    //fd.insertPushAction(index, (PushAction) editingNode.getUserObject());
		    } else {
                String error_string = "Error: " + childName + " cannot be inserted in a FilterDescription.";
                return error_string;
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.Enable) {
            String error_string = "Error: Enable is a leaf node, no insertions are allowed.";
            return error_string;
        } else if (parent instanceof org.ensembl.mart.lib.config.Disable) {
            String error_string = "Error: Disable is a leaf node, no insertions are allowed.";
            return error_string;
        }
        else if (parent instanceof org.ensembl.mart.lib.config.Option) {
			if (child instanceof org.ensembl.mart.lib.config.PushAction) {
				Option op = (Option) parentNode.getUserObject();
				op.insertPushAction(index,(PushAction) editingNode.getUserObject());
			}            
        }
		else if (parent instanceof org.ensembl.mart.lib.config.PushAction) {
			if (child instanceof org.ensembl.mart.lib.config.Option) {
				PushAction pa = (PushAction) parentNode.getUserObject();
				pa.insertOption(index,(Option) editingNode.getUserObject());
			}            
		}        
         else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
            if (child instanceof org.ensembl.mart.lib.config.AttributeGroup) {
                AttributePage ap = (AttributePage) parentNode.getUserObject();
                ap.insertAttributeGroup(index, (AttributeGroup) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in an AttributePage.";
                return error_string;
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
            if (child instanceof org.ensembl.mart.lib.config.AttributeCollection) {
                AttributeGroup ag = (AttributeGroup) parentNode.getUserObject();
                ag.insertAttributeCollection(index, (AttributeCollection) editingNode.getUserObject());
            } else {
                String error_string = "Error: " + childName + " cannot be inserted in an AttributeGroup.";
                return error_string;
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
            if (child instanceof org.ensembl.mart.lib.config.AttributeDescription) {
                AttributeCollection ac = (AttributeCollection) parentNode.getUserObject();
                ac.insertAttributeDescription(index, (AttributeDescription) editingNode.getUserObject());
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

    public void removeNodeFromParent(DatasetViewTreeNode node) {
        Object child = node.getUserObject();
        Object parent = ((DatasetViewTreeNode) node.getParent()).getUserObject();
        if (parent instanceof org.ensembl.mart.lib.config.DatasetView) {
            if (child instanceof org.ensembl.mart.lib.config.FilterPage) {
                view = (DatasetView) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                view.removeFilterPage((FilterPage) node.getUserObject());

            } else if (child instanceof org.ensembl.mart.lib.config.AttributePage) {
                view = (DatasetView) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                view.removeAttributePage((AttributePage) node.getUserObject());

            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterPage) {
            if (child instanceof org.ensembl.mart.lib.config.FilterGroup) {
                FilterPage fp = (FilterPage) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                fp.removeFilterGroup((FilterGroup) node.getUserObject());
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterGroup) {
            if (child instanceof org.ensembl.mart.lib.config.FilterCollection) {
                FilterGroup fg = (FilterGroup) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                fg.removeFilterCollection((FilterCollection) node.getUserObject());
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterCollection) {
            if (child instanceof org.ensembl.mart.lib.config.FilterDescription) {
                FilterCollection fc = (FilterCollection) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                fc.removeFilterDescription((FilterDescription) node.getUserObject());
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.FilterDescription) {
            if (child instanceof org.ensembl.mart.lib.config.Enable) {
                FilterDescription fd = (FilterDescription) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                fd.removeEnable((Enable) node.getUserObject());
            }
            else if (child instanceof org.ensembl.mart.lib.config.Disable) {
                FilterDescription fd = (FilterDescription) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                fd.removeDisable((Disable) node.getUserObject());
            }
            else if (child instanceof org.ensembl.mart.lib.config.Option) {
                FilterDescription fd = (FilterDescription) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                fd.removeOption((Option) node.getUserObject());
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.Option) {
		    if (child instanceof org.ensembl.mart.lib.config.PushAction) {
			    Option op = (Option) ((DatasetViewTreeNode) node.getParent()).getUserObject();
			    op.removePushAction((PushAction) node.getUserObject());
		    }		    
        } else if (parent instanceof org.ensembl.mart.lib.config.PushAction) {
			if (child instanceof org.ensembl.mart.lib.config.Option) {
				PushAction pa = (PushAction) ((DatasetViewTreeNode) node.getParent()).getUserObject();
				pa.removeOption((Option) node.getUserObject());
			}		    
		} else if (parent instanceof org.ensembl.mart.lib.config.AttributePage) {
            if (child instanceof org.ensembl.mart.lib.config.AttributeGroup) {
                AttributePage ap = (AttributePage) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                ap.removeAttributeGroup((AttributeGroup) node.getUserObject());
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.AttributeGroup) {
            if (child instanceof org.ensembl.mart.lib.config.AttributeCollection) {
                AttributeGroup ag = (AttributeGroup) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                ag.removeAttributeCollection((AttributeCollection) node.getUserObject());
            }
        } else if (parent instanceof org.ensembl.mart.lib.config.AttributeCollection) {
            if (child instanceof org.ensembl.mart.lib.config.AttributeDescription) {
                AttributeCollection ac = (AttributeCollection) ((DatasetViewTreeNode) node.getParent()).getUserObject();
                ac.removeAttributeDescription((AttributeDescription) node.getUserObject());
            }
        }
        super.removeNodeFromParent(node);

    }
}
