

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

# try this tip to get compilation / jars working
#http://lists.free.net.ph/pipermail/python/2002-April/000220.html

import pawt.swing.JPanel
from pawt.swing import *
from pawt.swing.tree import *
from org.ensembl.mart.explorer.gui import *
from java.lang import *
from javax.swing.event import *
from java.awt import *


class Dummy(QueryInputPage, JPanel):
    def __init__(self, message):
        self.add( JLabel( "TEMPORARY PANEL:  " +message ) )

    def updateQuery(self, query):
        pass

    def updatePage(self, query):
        pass

    def clear(self):
        pass


class SpeciesInputPage(QueryInputPage, JPanel):

    def __init__(self):
        self.species = JComboBox()
        self.species.editable = 1 
        self.species.minimumSize = Dimension(250,21) 
        self.species.preferredSize = self.species.minimumSize
        self.add( JLabel("Species") )
        self.add( self.species )
        
    def updateQuery(self, query):
        item = self.species.selectedItem
        if item: query.species = item
        else: raise InvalidQueryException("Species must be set")

    def updatePage(self, query):
        Tool.prepend( query.species, self.species )

    def clear(self):
        Tool.clear( self.species )

    def toString(self):
        item = self.species.selectedItem
        if item: return item
        else: return "UNSET"


class CardContainer(JPanel):
    """A JPanel with a CardLayout plus show() method for showing a particular card."""
    
    def __init__(self):
	self.layout = CardLayout()

    def show(self, cardName):
	"""Brings the specified card name to the front of container if it is available."""
	self.layout.show( self, cardName )



class QueryTreeNode(DefaultMutableTreeNode, TreeSelectionListener): 

    """Represents a TreeNode which: (1) causes the corresponding
    self.targetComponent to be displayed when it is selected, (2)
    generates custom text for display in tree via toString()
    method. """

    def __init__(self, tree, parent, position, cardContainer, targetComponent, targetCardName):
        
        """ Adds targetComponent to cardContainer with the name
        targetCardName. Makes this a tree listener and inserts self as
        child of parent at specified position."""

        DefaultMutableTreeNode.__init__(self)
	self.cardContainer = cardContainer
	self.targetComponent = targetComponent
        if targetComponent==None:
            self.targetComponent = Dummy( targetCardName )
	self.targetCardName = targetCardName
        tree.addTreeSelectionListener( self )
        tree.model.insertNodeInto( self, parent, position)
        cardContainer.add( self.targetComponent, self.targetCardName)
        

    def toString(self):
	return "<html><b>"+self.targetCardName+"</b>"+self.targetComponent.toString()+"</html>"


    def valueChanged(self, event):

	""" Brings the targetComponent to the front of the
	cardContainer if this was the node selected in the tree"""
	if self == event.newLeadSelectionPath.lastPathComponent:
            print "showing",self.targetCardName
	    self.cardContainer.show( self.targetCardName )



class tree_prototype(JPanel):


    def __init__(self):
        print "te init s2"

        rootNode = DefaultMutableTreeNode( "Query" )
        treeModel = DefaultTreeModel( rootNode )
        tree = JTree( treeModel )
        configPanel = CardContainer()
        
        dbNode = QueryTreeNode( tree, rootNode, 0, configPanel, DatabaseConfigPage(), "DATABASE" )
        speciesNode = QueryTreeNode( tree, rootNode, 1, configPanel, SpeciesInputPage(),"SPECIES" )
        focusNode = QueryTreeNode( tree, rootNode, 2, configPanel, None,"focus" )
        filtersNode = QueryTreeNode( tree, rootNode, 3, configPanel, None,"filter" )
        regionNode = QueryTreeNode( tree, filtersNode, 0, configPanel, None,"region" )
        outputNode = QueryTreeNode( tree, rootNode, 4, configPanel, None,"output" )
        attributesNode = QueryTreeNode( tree, outputNode, 0, configPanel, None,"attribute" )
        formatNode = QueryTreeNode( tree, outputNode, 1, configPanel, None,"format" )
        destinationNode = QueryTreeNode( tree, outputNode, 2, configPanel, None,"destination" )
        
        # expand branches in tree
	path = TreePath(rootNode).pathByAddingChild( filtersNode )
	tree.expandPath( path )
        path = TreePath(rootNode).pathByAddingChild( outputNode )
	tree.expandPath( path )
        path = path.pathByAddingChild( attributesNode )
	tree.expandPath( path )

        self.layout = BorderLayout()
	scrollPane = JScrollPane(tree)
	scrollPane.setPreferredSize( Dimension(250,300) )
        self.add(  scrollPane, BorderLayout.WEST )
	self.add( JScrollPane( configPanel ) )


    def updateQuery(self, query):
        pass

    def updatePage(self, query):
        pass

    def clear(self):
        pass


if __name__=="__main__":
    frame =JFrame("Explorer", defaultCloseOperation=JFrame.DISPOSE_ON_CLOSE, size=(800,500))
    frame.contentPane.add( tree_prototype() )
    frame.visible=1
