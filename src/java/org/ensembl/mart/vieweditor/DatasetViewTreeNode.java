package org.ensembl.mart.vieweditor;

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

    public DatasetViewTreeNode(String name){
        this.name = name;
    }

    public void setName(String newName){
        name = newName;
    }
    public String toString(){
        return name;
    }
}
