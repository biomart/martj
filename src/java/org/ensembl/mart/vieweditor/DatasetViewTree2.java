package org.ensembl.mart.vieweditor;

/**
 * Created by IntelliJ IDEA.
 * User: Sony
 * Date: 14-Nov-2003
 * Time: 14:10:01
 * To change this template use Options | File Templates.
 */

import org.ensembl.mart.lib.config.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.dnd.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class DatasetViewTree2 extends JTree implements Autoscroll, MouseListener,TreeModelListener,ActionListener,TreeSelectionListener{

    public static final Insets defaultScrollInsets = new Insets(8, 8, 8, 8);
    protected Insets scrollInsets = defaultScrollInsets;


    public DatasetViewTree2(DatasetView dsView) {
        super((TreeModel) null);			// Create the JTree itself
         addMouseListener(this);
         addTreeSelectionListener(this);
        // Use horizontal and vertical lines
        putClientProperty("JTree.lineStyle", "Angled");
        setEditable(true);
        // Create the first node
        DatasetViewTreeNode rootNode = new DatasetViewTreeNode(dsView.getDisplayName());
        AttributePage[] attributePages = dsView.getAttributePages();
        FilterPage[] filterPages = dsView.getFilterPages();

        // Process the contents
        for (int i = 0; i < attributePages.length; i++) {
            AttributePage atPage = attributePages[i];
            String atName = atPage.getInternalName();
            DatasetViewTreeNode atNode = new DatasetViewTreeNode(atName);
            atNode.setUserObject(atPage);
            rootNode.add(atNode);
            List groups = atPage.getAttributeGroups();
            for (int j = 0; j < groups.size(); j++) {
                if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.AttributeGroup")) {
                    AttributeGroup atGroup = (AttributeGroup) groups.get(j);
                    String grName = atGroup.getInternalName();
                    DatasetViewTreeNode grNode = new DatasetViewTreeNode(grName);
                    grNode.setUserObject(atGroup);
                    atNode.add(grNode);
                    AttributeCollection[] collections = atGroup.getAttributeCollections();
                    for (int z = 0; z < collections.length; z++) {
                        AttributeCollection atCollection = (AttributeCollection) collections[z];
                        String colName = atCollection.getInternalName();
                        DatasetViewTreeNode colNode = new DatasetViewTreeNode(colName);
                        grNode.add(colNode);
                        colNode.setUserObject(atCollection);
                        List descriptions = atCollection.getAttributeDescriptions();
                        for (int y = 0; y < descriptions.size(); y++) {
                            AttributeDescription atDescription = (AttributeDescription) descriptions.get(y);
                            String desName = atDescription.getInternalName();
                            DatasetViewTreeNode desNode = new DatasetViewTreeNode(desName);
                            desNode.setUserObject(atDescription);
                            colNode.add(desNode);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < filterPages.length; i++) {
            FilterPage fiPage = filterPages[i];
            String fiName = fiPage.getInternalName();
            DatasetViewTreeNode fiNode = new DatasetViewTreeNode(fiName);
            fiNode.setUserObject(fiPage);
            rootNode.add(fiNode);
            List groups = fiPage.getFilterGroups();
            for (int j = 0; j < groups.size(); j++) {
                if (groups.get(j).getClass().getName().equals("org.ensembl.mart.lib.config.FilterGroup")) {
                    FilterGroup fiGroup = (FilterGroup) groups.get(j);
                    String grName = fiGroup.getInternalName();
                    DatasetViewTreeNode grNode = new DatasetViewTreeNode(grName);
                    grNode.setUserObject(fiGroup);
                    fiNode.add(grNode);
                    FilterCollection[] collections = fiGroup.getFilterCollections();
                    for (int z = 0; z < collections.length; z++) {
                        FilterCollection fiCollection = (FilterCollection) collections[z];
                        String colName = fiCollection.getInternalName();
                        DatasetViewTreeNode colNode = new DatasetViewTreeNode(colName);
                        colNode.setUserObject(fiCollection);
                        grNode.add(colNode);
                        List descriptions = fiCollection.getFilterDescriptions();
                        for (int y = 0; y < descriptions.size(); y++) {
                            FilterDescription fiDescription = (FilterDescription) descriptions.get(y);
                            String desName = fiDescription.getInternalName();
                            DatasetViewTreeNode desNode = new DatasetViewTreeNode(desName);
                            desNode.setUserObject(fiDescription);
                            colNode.add(desNode);
                        }
                    }
                }
            }
        }
        // Populate the root node with its subdirectories
        //boolean addedNodes = rootNode.populateDirectories(true);
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        model.addTreeModelListener(this);
        setModel(model);

    }

    // Returns the full pathname for a path, or null if not a known path
    public String getPathName(TreePath path) {
        Object o = path.getLastPathComponent();
        if (o instanceof DatasetViewTree.DatasetTreeNode) {
            return ((DatasetViewTree.DatasetTreeNode) o).fullName;
        }
        return null;
    }


// Autoscrolling support
    public void setScrollInsets(Insets insets) {
        this.scrollInsets = insets;
    }

    public Insets getScrollInsets() {
        return scrollInsets;
    }

    // Implementation of Autoscroll interface
    public Insets getAutoscrollInsets() {
        Rectangle r = getVisibleRect();
        Dimension size = getSize();
        Insets i = new Insets(r.y + scrollInsets.top, r.x + scrollInsets.left,
                size.height - r.y - r.height + scrollInsets.bottom,
                size.width - r.x - r.width + scrollInsets.right);
        return i;
    }

    public void valueChanged(TreeSelectionEvent e){
        DatasetViewTreeNode node = (DatasetViewTreeNode)
                                   this.getLastSelectedPathComponent();
        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        if (nodeInfo.getClass().getName().equals("org.ensembl.mart.lib.config.AttributePage"))
        //((AttributePage)nodeInfo).setInternalName();
        System.out.println(nodeInfo.getClass());

    }

    public void autoscroll(Point location) {
        JScrollPane scroller =
                (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        if (scroller != null) {
            JScrollBar hBar = scroller.getHorizontalScrollBar();
            JScrollBar vBar = scroller.getVerticalScrollBar();
            Rectangle r = getVisibleRect();
            if (location.x <= r.x + scrollInsets.left) {
                // Need to scroll left
                hBar.setValue(hBar.getValue() - hBar.getUnitIncrement(-1));
            }
            if (location.y <= r.y + scrollInsets.top) {
                // Need to scroll up
                vBar.setValue(vBar.getValue() - vBar.getUnitIncrement(-1));
            }
            if (location.x >= r.x + r.width - scrollInsets.right) {
                // Need to scroll right
                hBar.setValue(hBar.getValue() + hBar.getUnitIncrement(1));
            }
            if (location.y >= r.y + r.height - scrollInsets.bottom) {
                // Need to scroll down
                vBar.setValue(vBar.getValue() + vBar.getUnitIncrement(1));
            }
        }

    }

    public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {

        }

        public void mouseEntered(MouseEvent e) {

        }

        public void mouseExited(MouseEvent e) {

        }

        public void mouseClicked(MouseEvent e) {
           if(e.getButton()==3){
               //Create the popup menu.
               JPopupMenu popup = new JPopupMenu();
               JMenuItem menuItem = new JMenuItem("copy");
               menuItem.addActionListener(this);
               popup.add(menuItem);
               menuItem = new JMenuItem("cut");
               menuItem.addActionListener(this);
               popup.add(menuItem);
               menuItem = new JMenuItem("paste");
               menuItem.addActionListener(this);
               popup.add(menuItem);
               menuItem = new JMenuItem("insert");
               menuItem.addActionListener(this);
               popup.add(menuItem);
               menuItem = new JMenuItem("delete");
               menuItem.addActionListener(this);
               popup.add(menuItem);
               popup.show(e.getComponent(),
                       e.getX(), e.getY());
                          }
        }

      public void actionPerformed(ActionEvent e){
            if(e.getActionCommand().equals("cut"))
                cut();
            else if(e.getActionCommand().equals("copy"))
                copy();
            else if(e.getActionCommand().equals("paste"))
                paste();
            else if(e.getActionCommand().equals("insert"))
                insert();
           else if(e.getActionCommand().equals("delete"))
                delete();
        }

       private void cut(){

       }

    private void copy(){

       }

    private void paste(){

       }
    private void insert(){

       }

    private void delete(){

       }
        public void treeNodesChanged(TreeModelEvent e){
            System.out.println("treeNodesChanged");
            Object [] children = e.getChildren();
            int [] childIndices = e.getChildIndices();
            for(int i=0;i<children.length;i++){
            System.out.println("Index "+childIndices[i]+",changed value: "+children[0]);
            }
        }



        public void treeStructureChanged(TreeModelEvent e){

        }

        public void treeNodesInserted(TreeModelEvent e){
            TreePath tPath = e.getTreePath();
            //tPath.getPathComponent();
        }

        public void treeNodesRemoved(TreeModelEvent e){

        }

}


