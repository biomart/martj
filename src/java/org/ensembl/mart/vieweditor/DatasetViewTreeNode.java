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

import java.util.List;
import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;

import org.ensembl.mart.lib.config.*;

/**
 * Class DatasetViewTreeNode extends DefaultMutableTreeNode.
 *
 * <p>This class is written so that the tree node is aware of the datasetview etc objects
 * </p>
 *
 * @author <a href="mailto:katerina@ebi.ac.uk">Katerina Tzouvara</a>
 * //@see org.ensembl.mart.config.DatasetView
 */


public class DatasetViewTreeNode extends DefaultMutableTreeNode {

    protected String name;

    public DatasetViewTreeNode(String name) {
        this.name = name;
    }

    public DatasetViewTreeNode(String name, Object obj) {
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
        if (nodeObjectClass.equals("org.ensembl.mart.lib.config.DatasetView")) {
            setName("DatasetView: " + ((BaseNamedConfigurationObject) obj).getInternalName());
            DatasetView dsv = (DatasetView) obj;
            FilterPage[] fpages = dsv.getFilterPages();
            for (int i = 0; i < fpages.length; i++) {
                if (fpages[i].getClass().getName().equals("org.ensembl.mart.lib.config.FilterPage")) {
                    FilterPage fp = fpages[i];
                    String fpName = fp.getInternalName();
                    DatasetViewTreeNode fpNode = new DatasetViewTreeNode("FilterPage:" + fpName);
                    fpNode.setUserObject(fp);
                    this.add(fpNode);
                    List groups = fp.getFilterGroups();
                    for (int j = 0; j < groups.size(); j++) {
                        if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.FilterGroup")) {
                            FilterGroup fiGroup = (FilterGroup) groups.get(j);
                            String grName = fiGroup.getInternalName();
                            DatasetViewTreeNode grNode = new DatasetViewTreeNode("FilterGroup:" + grName);
                            grNode.setUserObject(fiGroup);
                            FilterCollection[] collections = fiGroup.getFilterCollections();
                            for (int z = 0; z < collections.length; z++) {
                                FilterCollection fiCollection = collections[z];
                                String colName = fiCollection.getInternalName();
                                DatasetViewTreeNode colNode = new DatasetViewTreeNode("FilterCollection:" + colName);
                                colNode.setUserObject(fiCollection);
                                List descriptions = fiCollection.getFilterDescriptions();
                                for (int y = 0; y < descriptions.size(); y++) {
                                    FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
                                    String desName = fiDescription.getInternalName();
                                    DatasetViewTreeNode desNode = new DatasetViewTreeNode("FilterDescription:" + desName);
                                    desNode.setUserObject(fiDescription);
                                    Enable[] enables = fiDescription.getEnables();
                                    Disable[] disables = fiDescription.getDisables();
                                    Option[] options = fiDescription.getOptions();
                                    for (int k = 0; k < enables.length; k++) {
                                        Enable enable = enables[k];
                                        DatasetViewTreeNode enableNode = new DatasetViewTreeNode("Enable");
                                        enableNode.setUserObject(enable);
                                    }
                                    for (int k = 0; k < disables.length; k++) {
                                        Disable disable = disables[k];
                                        DatasetViewTreeNode disableNode = new DatasetViewTreeNode("Disable");
                                        disableNode.setUserObject(disable);
                                    }
                                    for (int k = 0; k < options.length; k++) {
                                        Option option = options[k];
                                        String optionName = option.getInternalName();
                                        DatasetViewTreeNode optionNode = new DatasetViewTreeNode("Option: " + optionName);
                                        optionNode.setUserObject(option);
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
                    DatasetViewTreeNode apNode = new DatasetViewTreeNode("AttributePage:" + apName);
                    apNode.setUserObject(ap);
                    this.add(apNode);
                    List groups = ap.getAttributeGroups();
                    for (int j = 0; j < groups.size(); j++) {
                        if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.AttributeGroup")) {
                            AttributeGroup atGroup = (AttributeGroup) groups.get(j);
                            String grName = atGroup.getInternalName();
                            DatasetViewTreeNode grNode = new DatasetViewTreeNode("AttributeGroup:" + grName);
                            grNode.setUserObject(atGroup);
                            AttributeCollection[] collections = atGroup.getAttributeCollections();
                            for (int z = 0; z < collections.length; z++) {
                                AttributeCollection atCollection = collections[z];
                                String colName = atCollection.getInternalName();
                                DatasetViewTreeNode colNode = new DatasetViewTreeNode("AttributeCollection:" + colName);
                                colNode.setUserObject(atCollection);
                                List descriptions = atCollection.getAttributeDescriptions();
                                for (int y = 0; y < descriptions.size(); y++) {
                                    AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
                                    String desName = atDescription.getInternalName();
                                    DatasetViewTreeNode desNode = new DatasetViewTreeNode("AttributeDescription:" + desName);
                                    desNode.setUserObject(atDescription);
                                }
                            }
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
                    DatasetViewTreeNode grNode = new DatasetViewTreeNode("FilterGroup:" + grName);
                    grNode.setUserObject(fiGroup);
                    this.add(grNode);
                    FilterCollection[] collections = fiGroup.getFilterCollections();
                    for (int z = 0; z < collections.length; z++) {
                        FilterCollection fiCollection = collections[z];
                        String colName = fiCollection.getInternalName();
                        DatasetViewTreeNode colNode = new DatasetViewTreeNode("FilterCollection:" + colName);
                        colNode.setUserObject(fiCollection);
                        List descriptions = fiCollection.getFilterDescriptions();
                        for (int y = 0; y < descriptions.size(); y++) {
                            FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
                            String desName = fiDescription.getInternalName();
                            DatasetViewTreeNode desNode = new DatasetViewTreeNode("FilterDescription:" + desName);
                            desNode.setUserObject(fiDescription);
                            Enable[] enables = fiDescription.getEnables();
                            Disable[] disables = fiDescription.getDisables();
                            Option[] options = fiDescription.getOptions();
                            for (int k = 0; k < enables.length; k++) {
                                Enable enable = enables[k];
                                DatasetViewTreeNode enableNode = new DatasetViewTreeNode("Enable");
                                enableNode.setUserObject(enable);
                            }
                            for (int k = 0; k < disables.length; k++) {
                                Disable disable = disables[k];
                                DatasetViewTreeNode disableNode = new DatasetViewTreeNode("Disable");
                                disableNode.setUserObject(disable);
                            }
                            for (int k = 0; k < options.length; k++) {
                                Option option = options[k];
                                String optionName = option.getInternalName();
                                DatasetViewTreeNode optionNode = new DatasetViewTreeNode("Option: " + optionName);
                                optionNode.setUserObject(option);
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
                    DatasetViewTreeNode grNode = new DatasetViewTreeNode("AttributeGroup:" + grName);
                    grNode.setUserObject(atGroup);
                    this.add(grNode);
                    AttributeCollection[] collections = atGroup.getAttributeCollections();
                    for (int z = 0; z < collections.length; z++) {
                        AttributeCollection atCollection = collections[z];
                        String colName = atCollection.getInternalName();
                        DatasetViewTreeNode colNode = new DatasetViewTreeNode("AttributeCollection:" + colName);
                        colNode.setUserObject(atCollection);
                        List descriptions = atCollection.getAttributeDescriptions();
                        for (int y = 0; y < descriptions.size(); y++) {
                            AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
                            String desName = atDescription.getInternalName();
                            DatasetViewTreeNode desNode = new DatasetViewTreeNode("AttributeDescription:" + desName);
                            desNode.setUserObject(atDescription);
                        }
                    }
                }
            }
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterGroup")) {
            setName("FilterGroup: " + ((BaseNamedConfigurationObject) obj).getInternalName());
            FilterGroup fiGroup = (FilterGroup) obj;
            FilterCollection[] collections = fiGroup.getFilterCollections();
            for (int z = 0; z < collections.length; z++) {
                FilterCollection fiCollection = collections[z];
                String colName = fiCollection.getInternalName();
                DatasetViewTreeNode colNode = new DatasetViewTreeNode("FilterCollection:" + colName);
                colNode.setUserObject(fiCollection);
                this.add(colNode);
                List descriptions = fiCollection.getFilterDescriptions();
                for (int y = 0; y < descriptions.size(); y++) {
                    FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
                    String desName = fiDescription.getInternalName();
                    DatasetViewTreeNode desNode = new DatasetViewTreeNode("FilterDescription:" + desName);
                    desNode.setUserObject(fiDescription);
                    Enable[] enables = fiDescription.getEnables();
                    Disable[] disables = fiDescription.getDisables();
                    Option[] options = fiDescription.getOptions();
                    for (int k = 0; k < enables.length; k++) {
                        Enable enable = enables[k];
                        DatasetViewTreeNode enableNode = new DatasetViewTreeNode("Enable");
                        enableNode.setUserObject(enable);
                    }
                    for (int k = 0; k < disables.length; k++) {
                        Disable disable = disables[k];
                        DatasetViewTreeNode disableNode = new DatasetViewTreeNode("Disable");
                        disableNode.setUserObject(disable);
                    }
                    for (int k = 0; k < options.length; k++) {
                        Option option = options[k];
                        String optionName = option.getInternalName();
                        DatasetViewTreeNode optionNode = new DatasetViewTreeNode("Option: " + optionName);
                        optionNode.setUserObject(option);
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
                DatasetViewTreeNode colNode = new DatasetViewTreeNode("AttributeCollection:" + colName);
                this.add(colNode);
                colNode.setUserObject(atCollection);
                List descriptions = atCollection.getAttributeDescriptions();
                for (int y = 0; y < descriptions.size(); y++) {
                    AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
                    String desName = atDescription.getInternalName();
                    DatasetViewTreeNode desNode = new DatasetViewTreeNode("AttributeDescription:" + desName);
                    desNode.setUserObject(atDescription);
                    //colNode.add(desNode);
                }
            }
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterCollection")) {
            setName("FilterCollection: " + ((BaseNamedConfigurationObject) obj).getInternalName());
            FilterCollection fiCollection = (FilterCollection) obj;
            List descriptions = fiCollection.getFilterDescriptions();
            for (int y = 0; y < descriptions.size(); y++) {
                FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
                String desName = fiDescription.getInternalName();
                DatasetViewTreeNode desNode = new DatasetViewTreeNode("FilterDescription:" + desName);
                desNode.setUserObject(fiDescription);
                this.add(desNode);
                Enable[] enables = fiDescription.getEnables();
                Disable[] disables = fiDescription.getDisables();
                Option[] options = fiDescription.getOptions();
                for (int k = 0; k < enables.length; k++) {
                    Enable enable = enables[k];
                    DatasetViewTreeNode enableNode = new DatasetViewTreeNode("Enable");
                    enableNode.setUserObject(enable);
                }
                for (int k = 0; k < disables.length; k++) {
                    Disable disable = disables[k];
                    DatasetViewTreeNode disableNode = new DatasetViewTreeNode("Disable");
                    disableNode.setUserObject(disable);
                }
                for (int k = 0; k < options.length; k++) {
                    Option option = options[k];
                    String optionName = option.getInternalName();
                    DatasetViewTreeNode optionNode = new DatasetViewTreeNode("Option: " + optionName);
                    optionNode.setUserObject(option);
                }
            }
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeCollection")) {
            setName("AttributeCollection: " + ((BaseNamedConfigurationObject) obj).getInternalName());
            AttributeCollection atCollection = (AttributeCollection) obj;
            List descriptions = atCollection.getAttributeDescriptions();
            for (int y = 0; y < descriptions.size(); y++) {
                AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
                String desName = atDescription.getInternalName();
                DatasetViewTreeNode desNode = new DatasetViewTreeNode("AttributeDescription:" + desName);
                desNode.setUserObject(atDescription);
                this.add(desNode);
            }
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterDescription")) {
            setName("FilterDescription: " + ((BaseNamedConfigurationObject) obj).getInternalName());
            FilterDescription fiDescription = (FilterDescription) obj;
            Enable[] enables = fiDescription.getEnables();
            Disable[] disables = fiDescription.getDisables();
            Option[] options = fiDescription.getOptions();
            for (int k = 0; k < enables.length; k++) {
                Enable enable = enables[k];
                DatasetViewTreeNode enableNode = new DatasetViewTreeNode("Enable");
                enableNode.setUserObject(enable);
                this.add(enableNode);
            }
            for (int k = 0; k < disables.length; k++) {
                Disable disable = disables[k];
                DatasetViewTreeNode disableNode = new DatasetViewTreeNode("Disable");
                disableNode.setUserObject(disable);
                this.add(disableNode);
            }
            for (int k = 0; k < options.length; k++) {
                Option option = options[k];
                String optionName = option.getInternalName();
                DatasetViewTreeNode optionNode = new DatasetViewTreeNode("Option: " + optionName);
                optionNode.setUserObject(option);
                this.add(optionNode);
            }

        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeDescription")) {
            setName("AttributeDescription: " + ((BaseNamedConfigurationObject) obj).getInternalName());

        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Enable")) {
            setName("Enable");
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Disable")) {
            setName("Disable");
        } else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.Option")) {
            setName("Option: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        }
    }

}
