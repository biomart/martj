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

import java.net.URL;
import java.io.File;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.TableColumn;

import org.ensembl.mart.explorer.QueryEditor;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.DSViewAdaptor;
import org.ensembl.mart.lib.config.DatasetView;
import org.ensembl.mart.lib.config.URLDSViewAdaptor;

/**
 * Tree representation of a DatasetView object.
 *
 * <p>This widget implements the MVC design pattern.
 * It's datasetView attribute is the model and can be changed.
 * The widget provides a tree
 * view representation of the model (not implemented) and offers these user control actions:
 * </p>
 *
 * <ul>
 * <li> Select a node. (not implemented) </li>
 * <li> Change the order of some nodes. (not implemented) </li>
 * <li> Add a node, context sensitive. (not implemented) </li>
 * <li> Delete a node. (not implemented) </li>
 * </ul>
 *
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * //@see org.ensembl.mart.config.DatasetView
 */
public class DatasetViewTreeWidget extends JInternalFrame {

    // NOTE see org.ensembl.mart.explorer.QueryEditor for examples of using JTree

    // TODO load tree when datasetView set, DONE!!!!!!!!
    // TODO check that resizing the widget works (manually resize via dragging "corners"), DONE!!!!!
    // TODO remove tree when setDatasetView(null), need to make sure we can add and remove at will
    // TODO add DatasetView.setSelected(XXXX), this should propagate a change event. Katerina/Craig/Darin
    // TODO Selecting a node in tree calls datasetView.setSelected(XXXX).
    // TODO Support reordering nodes of the same type e.g.change the order of attributes. Preferably by drag and drop mechanism
    // TODO Support deleting node by "delete" key, undo?
    // TODO Support deleting node by "right click | delete"
    // TODO Support adding a node (context sensitive) by "right click | add XXXX"

    private DatasetView datasetView = null;
    private static int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;
    private JDesktopPane desktop;
    private GridBagConstraints constraints;

    public DatasetViewTreeWidget(File file) {

        super("Dataset Tree " + (++openFrameCount),
                true, //resizable
                true, //closable
                true, //maximizable
                true);//iconifiable

        try {
            this.setFrameIcon(createImageIcon("MartView_cube.gif"));

            URL url = file.toURL();
            DSViewAdaptor adaptor = new URLDSViewAdaptor(url, true);

            // only view one in the file so get that one
            DatasetView view = adaptor.getDatasetViews()[0];
            this.setTitle(view.getInternalName());
            JFrame.setDefaultLookAndFeelDecorated(true);
            /*
            constraints = new GridBagConstraints();
            GridBagLayout layout = new GridBagLayout();
            this.getContentPane().setLayout(layout);
            constraints.fill = GridBagConstraints.BOTH;

            constraints.weightx = 10;
            constraints.weighty = 10;  */
            DatasetViewAttributesTable attrTable = new DatasetViewAttributesTable(
                    view, this);
            //TableColumn column = attrTable.getColumnModel().getColumn(1);

            JScrollPane treeScrollPane = new JScrollPane(new DatasetViewTree(view,
                    this, attrTable));
            JScrollPane tableScrollPane = new JScrollPane(attrTable);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    treeScrollPane, tableScrollPane);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(150);

            //Provide minimum sizes for the two components in the split pane.
            Dimension minimumSize = new Dimension(100, 50);
            treeScrollPane.setMinimumSize(minimumSize);
            tableScrollPane.setMinimumSize(minimumSize);

            this.getContentPane().add(splitPane);

            //add(this.getContentPane(), new JScrollPane(new DatasetViewTree(view, this, attrTable)), constraints, 0, 0, 1, 1);
            //add(this.getContentPane(), new JScrollPane(attrTable), constraints, 1, 0, 1, 1);

            //...Then set the window size or call pack...
            setSize(500, 400);

            //Set the window's location.
            setLocation(xOffset * openFrameCount, yOffset * openFrameCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test purposes only. Creates a frame with a JTree containing
     * a presepecified DatasetView.dtd compatible configuration file.
     * @param args
     * @throws ConfigurationException
     */
    public static void main(String[] args) throws ConfigurationException {


    }

    /**
     * @return
     */
    public DatasetView getDatasetView() {
        return datasetView;
    }

    public void addAttributesTable(JTable table) {
        add(this.getContentPane(), new JScrollPane(table), constraints, 1, 0, 1, 1);
    }

    public void add(Container cont, Component component, GridBagConstraints constraints, int x, int y, int w, int h) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        cont.add(component, constraints);
    }

    /**
     * @param view
     */
    public void setDatasetView(DatasetView view) {
        clearDatasetView();
        datasetView = view;
        loadDatasetView();
    }

    /**
     * Loads the datasetView by creating a tree to represent it
     * and displaying it.
     */
    private void loadDatasetView() {


    }

    /**
     * Removes current dataset view if one is loaded, otherwise does nothing.
     */
    private void clearDatasetView() {
        // TODO Auto-generated method stub

    }


    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = DatasetViewTreeWidget.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

}
