package org.ensembl.mart.vieweditor;

import org.ensembl.mart.lib.config.BaseNamedConfigurationObject;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Created by IntelliJ IDEA.
 * User: Sony
 * Date: 16-Nov-2003
 * Time: 22:18:20
 * To change this template use Options | File Templates.
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
        BaseNamedConfigurationObject nodeObject = (BaseNamedConfigurationObject) obj;
        String nodeObjectClass = nodeObject.getClass().getName();
        if (nodeObjectClass.equals("org.ensembl.mart.lib.config.DatasetView"))
            setName("DatasetView: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterPage"))
            setName("FilterPage: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributePage"))
            setName("AttributePage: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeGroup"))
            setName("AttributeGroup: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterGroup"))
            setName("FilterGroup: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeGroup"))
            setName("AttributGroup: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterCollection"))
            setName("FilterCollection: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeCollection"))
            setName("AttributeCollection: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.FilterDescription"))
            setName("FilterDescription: " + ((BaseNamedConfigurationObject) obj).getInternalName());
        else if (nodeObjectClass.equals("org.ensembl.mart.lib.config.AttributeDescription"))
            setName("AttributeDescription: " + ((BaseNamedConfigurationObject) obj).getInternalName());

    }

    public Object clone() {
        Object clone = super.clone();
        String objClass = getUserObject().getClass().getName();
        return clone;
    }
}
