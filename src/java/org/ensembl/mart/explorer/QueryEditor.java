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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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

import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.config.ConfigurationException;
import org.ensembl.mart.lib.config.Dataset;
import org.ensembl.mart.lib.config.MartConfiguration;
import org.ensembl.mart.lib.config.MartConfigurationFactory;
import org.ensembl.mart.lib.config.Option;

// TODO Support attribute order rearrangment via DnD
// TODO Support attribute / filter removal by DELETE key
// TODO selecting an attribute / filter should cause it to be shown in InputPanel

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

	private JSplitPane topAndBottom;

	private JSplitPane top;

	private static final Logger logger =
		Logger.getLogger(QueryEditor.class.getName());

	/** default percentage of total width allocated to the tree constituent component. */
	private double TREE_WIDTH = 0.27d;

	/** default percentage of total height allocated to the tree constituent component. */
	private double TREE_HEIGHT = 0.7d;

	private Dimension MINIMUM_SIZE = new Dimension(50, 50);

	/** Configuration defining the "query space" this editor encompasses. */
	private MartConfiguration martConfiguration;

	/** The query part of the model. */
	private Query query;

	private DefaultTreeModel treeModel;
	private DefaultMutableTreeNode rootNode;

	private JTree treeView;
	private JPanel inputPanel;
	private JPanel outputPanel;

	private TreeFilterWidget datasetSelectionPage;
	private String currentDatasetName;
	private OutputSettingsPage outputSettingsPage;

	private AttributePageSetWidget attributesPage;
	private FilterPageSetWidget filtersPage;

	private Option lastDatasetOption;

	/** Maps attributes to the tree node they are represented by. */
	private Map attributeToWidget;

	public QueryEditor(MartConfiguration config) {

		// don't use default FlowLayout manager because it won't resize components if
		// QueryEditor is resized.
		setLayout( new BorderLayout() );
    


		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				resizeSplits();
			}
             
		});

		this.martConfiguration = config;
		this.query = new Query();
		this.attributeToWidget = new HashMap();

		query.addPropertyChangeListener(this);

		initTree();
		initInputPanel();
		initOutputPanel();

		layoutPanes();

		addDatasetSelectionPage();
	}

	/**
	 * Repositions the dividers after the component has been resized to maintain
	 * the relative size of the panes.
	 */
	private void resizeSplits() {

    // must set divider by explicit values rather than
    // proportions because the proportion approach fails
    // on winxp jre 1.4 when the component is FIRST added.
    // (It does work when the component is resized).
    Dimension size = getParent().getSize();
    int treeWidth = (int)(TREE_WIDTH * size.width);
    int treeHeight = (int)((1 - TREE_WIDTH) * size.height);
    top.setDividerLocation( treeWidth );
    topAndBottom.setDividerLocation( treeHeight );

//    top.setDividerLocation( TREE_WIDTH );
//    topAndBottom.setDividerLocation( 1 - TREE_WIDTH );

		// this doesn't actually set the size to the minumum but
		// cause it to resize correctly. 
		//inputPanel.setMinimumSize(MINIMUM_SIZE);
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

		datasetSelectionPage =
			new TreeFilterWidget(query, martConfiguration.getLayout());

		lastDatasetOption = datasetSelectionPage.getOption();

		// this is the perperty name that will be included in 
		// the propertyChange evetn emitted when the widget changes.
		datasetSelectionPage.setPropertyName("dataset");
		datasetSelectionPage.addPropertyChangeListener(this);

		addPage(datasetSelectionPage);
	}

	/**
	 * Sets the relative positions of the constituent components. Layout is:
	 * <pre>
	 * tree   |    input
	 * -----------------
	 *     output
	 * </pre>
	 */
	private void layoutPanes() {

		treeView.setMinimumSize(MINIMUM_SIZE);
		inputPanel.setMinimumSize(MINIMUM_SIZE);
		outputPanel.setMinimumSize(MINIMUM_SIZE);

		top =
			new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				new JScrollPane(treeView),
				new JScrollPane(inputPanel));
		top.setOneTouchExpandable(true);

		topAndBottom =
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
    f.setSize(950, 750);
    f.setVisible(true);
    
	}

	/**
	 * Redraws the tree if there are any property changes in the Query.
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {

		String propertyName = evt.getPropertyName();
		Object newValue = evt.getNewValue();
		Object oldValue = evt.getOldValue();

		if (evt.getSource() == datasetSelectionPage
			&& datasetSelectionPage.getPropertyName().equals(propertyName)
			&& !newValue.equals(oldValue)) {

			if (oldValue != null) {

        // Confirm user really wants to change dataset
				int option =
					JOptionPane.showConfirmDialog(
						this,
						new JLabel("Changing the dataset will cause the query settings to be cleared. Continue?"),
						"Change Attributes",
						JOptionPane.YES_NO_OPTION);

        // undo if user changes mind
				if (option != JOptionPane.OK_OPTION) {

					datasetSelectionPage.removePropertyChangeListener(this);
					datasetSelectionPage.setOption(lastDatasetOption);
					datasetSelectionPage.addPropertyChangeListener(this);

					return;
				}
			}
      
			lastDatasetOption = (Option)newValue;

			datasetChanged(  martConfiguration.getDatasetByName( lastDatasetOption.getRef() ) );
      treeModel.nodeChanged( datasetSelectionPage.getNode() );
		}

		if (evt.getSource() == query) {

			if ("attribute".equals(propertyName)) {

				if (newValue != null && oldValue == null)
					insertNode(
						attributesPage.getNode(),
						((InputPageAware) newValue).getInputPage().getNode());

				else if (newValue == null && oldValue != null)
					treeModel.removeNodeFromParent(
						((InputPageAware) oldValue).getInputPage().getNode());

			} else if ("filter".equals(propertyName)) {

				if (newValue != null && oldValue == null)
					insertNode(
						filtersPage.getNode(),
						((InputPageAware) newValue).getInputPage().getNode());

				else if (newValue == null && oldValue != null)
					treeModel.removeNodeFromParent(
						((InputPageAware) oldValue).getInputPage().getNode());

			} else {
				logger.warning("Unrecognised propertyChange: " + propertyName);
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
	 * Inserts child node under parent in tree and select it.
	 * @param parent
	 * @param child
	 */
	private void insertNode(MutableTreeNode parent, MutableTreeNode child) {

		treeModel.insertNodeInto(child, parent, parent.getChildCount());

		// make node selected in tree
		treeView.setSelectionPath(
			new TreePath(rootNode).pathByAddingChild(parent).pathByAddingChild(
				child));

	}

	/**
	 * Update the model (query) and the view (tree and inputPanels).
	 */
	private void datasetChanged(Dataset dataset) {

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

		addAttributePages( dataset );
		addFilterPages( dataset );
		addOutputPage();

		// select the attributes page
		treeView.setSelectionPath(
			new TreePath(rootNode).pathByAddingChild(attributesPage.getNode()));

	}

	/**
	 * Adds output page to tree and InputPanel.
	 */
	private void addOutputPage() {
		outputSettingsPage = new OutputSettingsPage();
		outputSettingsPage.addPropertyChangeListener(this);
		addPage(outputSettingsPage);
	}

	/**
	 * 
	 */
	private void addFilterPages(Dataset dataset) {
		filtersPage =
			new FilterPageSetWidget(
				query,
				dataset);
		// TODO :2 create maps like for attributes
		addPage(filtersPage);
	}

	/**
	 * Creates the attribute pages and various maps that are useful 
	 * for relating nodes, pages and attributes.
	 */
	private void addAttributePages(Dataset dataset) {

		attributesPage =
			new AttributePageSetWidget(
				query,
				dataset);
		//    List list = attributesPage.getLeafWidgets();
		//    AttributeDescriptionWidget[] attributePages = (AttributeDescriptionWidget[]) list.toArray(new AttributeDescriptionWidget[list.size()]);
		//    for (int i = 0; i < attributePages.length; i++) {
		//      AttributeDescriptionWidget w = attributePages[i];
		//      Attribute a = w.getAttribute();
		//      attributeFieldNameToPage.put( a.getField(), w );
		//      attributeToWidget.put( a, w );
		//    }

		addPage(attributesPage);

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

			if (node.getUserObject() instanceof InputPage) {

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
