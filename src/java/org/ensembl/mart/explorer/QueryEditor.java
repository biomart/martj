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

package org.ensembl.mart.explorer;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.ensembl.mart.lib.Attribute;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.AttributePage;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;

/**
 * Provides a panel in which a user can create and edit
 * a Query.
 * 
 * <p>
 * The Panel represents the queries made available in the 
 * MartConfiguration object it is initialised with.
 * </p>
 */
public class QueryEditor
	extends JPanel
	implements PropertyChangeListener, TreeSelectionListener {

	private static final Logger logger =
		Logger.getLogger(QueryEditor.class.getName());

	/** total height of the componment */
	private int HEIGHT = 768;

	/** total width of the component */
	private int WIDTH = 1024;

	/** default percentage of total width allocated to the tree constituent component. */
	private double TREE_WIDTH = 0.4d;

	/** default percentage of total height allocated to the tree constituent component. */
	private double TREE_HEIGHT = 0.7d;

	/** Configuration defining the "query space" this editor encompasses. */
	private MartConfiguration martConfiguration;

	/** The query part of the model. */
	private Query query;

	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode rootNode;

	private JTree treeView;
	private JPanel inputPanel;
	private JPanel outputPanel;

	private DatasetSelectionPage datasetSelectionPage;
	private Dataset currentDataset;
	private OutputSettingsPage outputSettingsPage;

	private AttributePageSetWidget attributesPage;

	public QueryEditor(MartConfiguration config) {
		this.martConfiguration = config;
		this.query = new Query();

		query.addPropertyChangeListener(this);

		initTree();
		initInputPanel();
		initOutputPanel();

		initialLayout();

		addDatasetSelectionPage();
	}

	private void showInputPage(InputPage page) {
		((CardLayout) (inputPanel.getLayout())).show(inputPanel, page.getName());
	}

	/**
	 * Adds page to input panel and tree view.
	 * @param page page to be added
	 */
	private void addPage(InputPage page) {

		//  Add page to input panel.
		inputPanel.add(page.getName(), page);
		showInputPage(page);

		// Add page's node and show it
		treeModel.insertNodeInto(
			page.getNode(),
			rootNode,
			rootNode.getChildCount());
		TreePath path = new TreePath(rootNode).pathByAddingChild(page.getNode());
		treeView.makeVisible(path);

		treeView.setRootVisible(false);
	}

	/**
	 * Adds the Dataset selection page to the panel.
	 */
	private void addDatasetSelectionPage() {

		datasetSelectionPage = new DatasetSelectionPage(query, martConfiguration);
		currentDataset = null;
		addPage(datasetSelectionPage);
	}

	/**
	 * Sets the prefered sizes for constituent components and adds them
	 * to this component.
	 * All sizes are relative to the treeview dimensions. Layout is:
	 * <pre>
	 * tree   |    input
	 * -----------------
	 *     output
	 * </pre>
	 */
	private void initialLayout() {

		int topHeight = (int) (HEIGHT * TREE_HEIGHT);
		int treeWidth = (int) (WIDTH * TREE_WIDTH);
		int inputWidth = WIDTH - treeWidth;
		int outputHeight = HEIGHT - topHeight;

		treeView.setPreferredSize(new Dimension(treeWidth, treeWidth));
		inputPanel.setPreferredSize(new Dimension(inputWidth, topHeight));
		outputPanel.setPreferredSize(new Dimension(WIDTH, outputHeight));

		JSplitPane top =
			new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(treeView),
				new JScrollPane(inputPanel));
		top.setOneTouchExpandable(true);
		JSplitPane topAndBottom =
			new JSplitPane(
				JSplitPane.VERTICAL_SPLIT,
				top,
				new JScrollPane(outputPanel));
		topAndBottom.setOneTouchExpandable(true);
		add(topAndBottom);
	}

	/**
	 * 
	 */
	private void initOutputPanel() {
		outputPanel = new JPanel();
		outputPanel.add(new JLabel("output panel"));
	}

	/**
	 * 
	 */
	private void initInputPanel() {
		inputPanel = new JPanel();
		inputPanel.setLayout(new CardLayout());
		inputPanel.add("input", new JLabel("input panel"));
	}

	/**
	 * 
	 */
	private void initTree() {
		rootNode = new DefaultMutableTreeNode("Query");
		treeModel = new DefaultTreeModel(rootNode);
		treeView = new JTree(treeModel);
		treeView.addTreeSelectionListener(this);
	}

	public static void main(String[] args) throws ConfigurationException {
		String confFile = "data/XML/MartConfigurationTemplate.xml";
		URL confURL = ClassLoader.getSystemResource(confFile);
		MartConfiguration config =
			new MartConfigurationFactory().getInstance(confURL);

		QueryEditor editor = new QueryEditor(config);
		JFrame f = new JFrame("Query Editor");
		f.getContentPane().add(editor);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
	}

	/**
	 * Redraws the tree if there are any property changes in the Query.
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getSource() == query) {

			
			// update pages if dataset changed.
			if (currentDataset != datasetSelectionPage.getSelectedDataset())
				updateModelAndViewAfterDatasetChanged();

      String propertyName = evt.getPropertyName();
      Object newValue = evt.getNewValue();
      Object oldValue = evt.getOldValue();
       
      if ( "attribute".equals( propertyName ) ) {
        if ( newValue!=null && oldValue==null ) 
          updateAddedAttribute( (Attribute)newValue );
        else if ( newValue==null && oldValue!=null )
          updateRemovedAttribute( (Attribute)oldValue );
      }

			Enumeration enum = rootNode.breadthFirstEnumeration();
			while (enum.hasMoreElements())
				treeModel.nodeChanged((TreeNode) enum.nextElement());

			System.out.println("Query Changed: " + query);
		}

		if (evt.getSource() == outputSettingsPage) {
			treeModel.nodeChanged(outputSettingsPage.getNode());
		}

	}

	/**
   * Remove the tree node representing the attribute
	 * @param attribute that has been removed from Query.
	 */
	private void updateRemovedAttribute(Attribute attribute) {
		// TODO
		
	}


	/**
	 * @param attribute that has been added to the Query.
	 */
	private void updateAddedAttribute(Attribute attribute) {
		MutableTreeNode parent = attributesPage.getNode();
    String displayName = attribute.getField();

    // Use a "fake" input page so that clicking this node causes the
    // attribute page to be selected in the input panel.
    // TODO support selction of correct tab pane?
    InputPage page = new InputPage( attributesPage.getName(), null );
    page.setNodeLabel(null, displayName );
    
    MutableTreeNode child = new DefaultMutableTreeNode( page );
    treeModel.insertNodeInto( child, parent, parent.getChildCount() );
    // select node in tree
    treeView.setSelectionPath( new TreePath(rootNode).pathByAddingChild( parent ).pathByAddingChild( child ) );
	}



	/**
	 * Update the model (query) and the view (tree and inputPanels).
	 */
	private void updateModelAndViewAfterDatasetChanged() {

		// Basically we remove most things from the model and views before adding those things that should
		// be present back in.

		// Update the model
		query.removeAllAttributes();
		query.removeAllFilters();

		// enables soon to be obsolete listeners to be garbage collected once they are removed from the views.
		query.removeAllPropertyChangeListeners();
		// but we still need to listen to changes keep tree up to date
		query.addPropertyChangeListener(this);

		// We need to remove all pages from the inputPanel so that we can add pages with the same
		// name again later. e.g. attributesPage.
		inputPanel.removeAll();

		// Remove all nodes from the tree then add the datasetSelectionPage. Removing
		// the other nodes one at a time using treeModel.removeNodeFromParent( XXX.getNode() ) 
		// failed to work on jdk1.4.1@linux. 
		rootNode.removeAllChildren();
		addPage(datasetSelectionPage);
		treeModel.reload();

		currentDataset = datasetSelectionPage.getSelectedDataset();

		// Add attribute, filter and output pages to the views.
		attributesPage = new AttributePageSetWidget(query, currentDataset);
		addPage(attributesPage);
		// todo add filter page here
		outputSettingsPage = new OutputSettingsPage();
		outputSettingsPage.addPropertyChangeListener(this);
		addPage(outputSettingsPage);

    // select the attributes page
    treeView.setSelectionPath( new TreePath( rootNode ).pathByAddingChild( attributesPage.getNode() ) );

	}

	/**
	 * Show input page corresponding to selected tree node. 
	 */
	public void valueChanged(TreeSelectionEvent e) {
		if (e.getNewLeadSelectionPath() != null
			&& e.getNewLeadSelectionPath().getLastPathComponent() != null) {

			DefaultMutableTreeNode node =
				(DefaultMutableTreeNode) e
					.getNewLeadSelectionPath()
					.getLastPathComponent();
      
      if ( node.getUserObject() instanceof InputPage ) {
      
			 InputPage page = (InputPage) node.getUserObject();
			 showInputPage(page);
       
      }
		}
	}

	/**
	 * @return
	 */
	public MartConfiguration getMartConfiguration() {
		return martConfiguration;
	}

}
