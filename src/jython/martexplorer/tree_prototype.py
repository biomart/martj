# Jython prototype of a "tree" based mart-explorer graphical user
# interface.

# copyright EBI, GRL 2003

# TODO implement each of the 'basic' nodes (database, species, focus,
# format, destination) using NodeInfo instances. See tmp
# EXAMPLE. Create separate config panels for each.

# TODO implement useful toString() or better summary():String methods
# on targetComponents such as DatabaseConfPanel()

# TODO add implementation for Filter and Attributes. Clicking should
# cause a list of available (not already added) items to be
# shown. Selecting one of these will cause the respective config panel
# to be displayed. If the OK button is pressed on these then they are
# added to the query.

# TODO support removing filter and attribute items from
# quesry. "Delete" and or right click/delete.

from pawt.swing import *
from pawt.swing.tree import *
from org.ensembl.mart.explorer.gui import *
from java.lang import *
from javax.swing.event import *
from java.awt import *



class CardContainer(JPanel):
    """A JPanel with a CardLayout plus show() method for showing a particular card."""
    
    def __init__(self):
	self.layout = CardLayout()

    def show(self, cardName):
	"""Brings the specified card name to the front of container if it is available."""
	self.layout.show( self, cardName )



class NodeInfo(Object, TreeSelectionListener): 

    """Represents a single 'userObject' node for inclusion in a
DefaultMutableTreeNode. It has two functions: (1) When the containing
tree node is selected it causes the corresponding self.component to be
displayed, and (2) it creates the text displayed against the tree node
in the JTree."""

    def __init__(self, cardContainer, targetComponent, targetCardName):
	self.cardContainer = cardContainer
	self.targetComponent = targetComponent
	self.targetCardName = targetCardName
    

    def toString(self):
	return "<html><b>My</b>"+self.targetComponent.toString()+"</html>"


    def valueChanged(self, event):

	""" Brings the targetComponent to the front of the
	cardContainer if this was the node selected in the tree"""
	
	if self == event.newLeadSelectionPath.lastPathComponent.userObject:
	    print self.toString(),"heard a selection"
	    self.cardContainer.show( self.targetCardName )


class TreeExplorer(JPanel):

    def __init__(self):
        print "te init s"

        self.rootNode = DefaultMutableTreeNode( "Query" )
        self.dbNode = DefaultMutableTreeNode( "Database" )
        self.speciesNode = DefaultMutableTreeNode( "Species" )
        self.focusNode = DefaultMutableTreeNode( "Focus" )
        self.filtersNode = DefaultMutableTreeNode( "Filters" )
        self.regionNode = DefaultMutableTreeNode( "Region" )
        self.outputNode = DefaultMutableTreeNode( "Output" )
        self.attributesNode = DefaultMutableTreeNode( "Attributes" )
        self.formatNode = DefaultMutableTreeNode( "Format" )
        self.destinationNode = DefaultMutableTreeNode( "Destination" )
        
        treeModel = DefaultTreeModel( self.rootNode )

        treeModel.insertNodeInto( self.dbNode, self.rootNode, 0)
        treeModel.insertNodeInto( self.speciesNode, self.rootNode, 1)
        treeModel.insertNodeInto( self.focusNode, self.rootNode, 2)
        treeModel.insertNodeInto( self.filtersNode, self.rootNode, 3)
        treeModel.insertNodeInto( self.outputNode, self.rootNode, 4)

        treeModel.insertNodeInto( self.regionNode, self.filtersNode, 0)

        treeModel.insertNodeInto( self.attributesNode, self.outputNode, 0)
        treeModel.insertNodeInto( self.formatNode, self.outputNode, 1)
        treeModel.insertNodeInto( self.destinationNode, self.outputNode, 2)
	
        self.tree = JTree( treeModel, valueChanged=self.nodeSelected )

        self.configPanel = CardContainer()

        self.databasePanel = DatabaseConfigPage()
        self.regionPanel = FilterPanel()
        self.formatPanel = ExportPanel()

        self.configPanel.add( self.databasePanel, "DATABASE"  )
	self.configPanel.add( self.regionPanel, "REGION_FILTER" )
	self.configPanel.add( self.formatPanel, "FORMAT_PANEL" )
	
	# tmp EXAMPLE
	myTreeNode = NodeInfo( self.configPanel, self.regionPanel, "REGION_FILTER" )
        treeModel.insertNodeInto( DefaultMutableTreeNode(myTreeNode), self.rootNode, 0)
        self.tree.addTreeSelectionListener( myTreeNode )

	self.expand()

        self.layout = BorderLayout()
	scrollPane = JScrollPane(self.tree)
	scrollPane.setPreferredSize( Dimension(150,300) )
        self.add(  scrollPane, BorderLayout.WEST )
	self.add( self.configPanel )






    def expand(self):
	""" Expands Filter, Output and attribute nodes. """
	path = TreePath(self.rootNode).pathByAddingChild(self.filtersNode)
	self.tree.expandPath( path )
	path = TreePath(self.rootNode).pathByAddingChild(self.outputNode)
	self.tree.expandPath( path )
	path = TreePath(self.rootNode).pathByAddingChild(self.attributesNode)
	self.tree.expandPath( path )



    def nodeSelected(self, event=None):
        node = event.newLeadSelectionPath.lastPathComponent

        if node==self.dbNode:
            print "Set database"

        elif node==self.speciesNode:
            print "Set species"

        elif node==self.focusNode:
            print "Set focus"

        elif node==self.filtersNode:
            print "Add filters"

        elif node==self.attributesNode:
            print "Add attributes"

        elif node==self.regionNode:
            print "Set region"
            
        elif node==self.formatNode:
            print "Set format"

        elif node==self.destinationNode:
            print "Set destination"

        else:
            print "ignoring selection"

    
if __name__=="__main__":
    frame =JFrame("Explorer", defaultCloseOperation=JFrame.DISPOSE_ON_CLOSE, size=(300,300))
    frame.contentPane.add( TreeExplorer() )
    frame.visible=1
