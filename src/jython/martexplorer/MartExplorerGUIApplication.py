#!/usr/bin/env martjython.sh

# MartExplorer Graphical User Interface.

# copyright EBI, GRL 2003

# TODO implement each of the 'basic' nodes (database, species, focus,
# format, destination) using NodeInfo instances. Create separate
# config panels for each.

# TODO implement useful toString() or better summary():String methods
# on targetComponents such as DatabaseConfPanel()

# TODO add implementation for Filter and Attributes. Clicking should
# cause a list of available (not already added) items to be
# shown. Selecting one of these will cause the respective config panel
# to be displayed. If the OK button is pressed on these then they are
# added to the query.

# TODO support removing filter and attribute items from
# quesry. "Delete" and or right click/delete.

from jarray import array
from java.lang import System, String
from java.io import File
from java.net import URL
from java.awt import CardLayout, Dimension, BorderLayout
from java.awt.event import ActionListener
from javax.swing import JPanel, JButton, JFrame, JLabel, JComboBox, Box, BoxLayout, ButtonGroup, JRadioButton
from javax.swing import JScrollPane, JMenu, JMenuItem, JMenuBar, JToolBar, JTree
from javax.swing.event import ChangeEvent, ChangeListener, TreeSelectionListener
from javax.swing.tree import TreePath, DefaultTreeModel, DefaultMutableTreeNode
from javax.swing.border import EmptyBorder
from org.ensembl.mart.explorer import Query, IDListFilter, FieldAttribute

GAP = 5

class QueryInputPage:
    def updateQuery(self, query):
        pass

    def updatePage(self, query):
        pass

    def clear(self):
        pass

class BaseInputPage(QueryInputPage, Box, ChangeListener):

    """ Implements a Box container that can listen for change events
    and rebroadcast to attached listeners."""

    def __init__(self):
        Box.__init__(self, BoxLayout.Y_AXIS)
        self.changeEvent = ChangeEvent( self )
        self.changeListeners = []

    def stateChanged(self, event=None):
        for l in self.changeListeners:
            l.stateChanged( self.changeEvent )

    def addChangeListener(self, listener):
        self.changeListeners.append( listener )


class DummyInputPage(QueryInputPage, JPanel):
    def __init__(self, message):
        self.add( JLabel( "TEMPORARY PANEL:  " +message ) )

    def addChangeListener( self, listener ):
        pass

    def toString(self):
        return ""


class LabelledComboBox(QueryInputPage, Box, ActionListener):

    """ Abstract "label + combo box" component with optional radio
    button. Issues ChangeEvents to ChangeListeners if contents of
    combobox changes. Loads value from and into query through methods
    implemented in derived classes. """

    # TODO next: writing in text box should auto select radiobutton if
    # available

    def __init__(self, label, changeListener=None, radioButtonGroup=None):

        Box.__init__(self, BoxLayout.X_AXIS)

        if radioButtonGroup:
            self.radioButton = JRadioButton( label, 0 )
            radioButtonGroup.add( self.radioButton )
            self.add( self.radioButton )
        else:
            self.radioButton = None
            self.add( JLabel( label ) ) 

        self.add( Box.createHorizontalStrut( GAP*2 ))
        self.box = JComboBox()
        self.box.editable = 1
        self.add( self.box )
        
        d = Dimension(500, 35 )
        self.preferredSize = d
        self.maximumSize = d
        self.border = EmptyBorder(GAP, GAP, GAP, GAP)
        
        self.box.addActionListener( self )
        self.changeEvent = ChangeEvent( self )
        self.changeListeners = []

        if changeListener:
            self.addChangeListener( changeListener )


    def clear(self):
        Tool.clear( self.box )

    def toString(self):
        item = self.box.selectedItem
        if item: return item
        else: return "UNSET"

    def actionPerformed( self, event=None ):
        for l in self.changeListeners:
            l.stateChanged( self.changeEvent )

    def addChangeListener( self, listener ):
        self.changeListeners.append( listener )
        
    def setText(self, text):
        self.box.setSelectedItem( text )

    def getText(self):
        return self.box.selectedItem


    def isSelected(self):
        if self.radioButton: return self.radioButton.selected
        else: 0




class SpeciesInputPage(BaseInputPage):

    """ Input component manages display and communication between
    "species" drop down list <-> query.species."""

    def __init__(self):
        BaseInputPage.__init__(self)
        self.box = LabelledComboBox("Species", self)
        self.add( self.box )

    def updateQuery(self, query):
        item = self.box.selectedItem
        if item: query.species = item
        else: raise InvalidQueryException("Species must be set")

    def updatePage(self, query):
        self.box.setText( query.species )
        
    def toString(self):
        tmp = self.box.getText()
        if tmp: return tmp
        else: return "UNSET"


class FocusInputPage(BaseInputPage):

    """ Input component manages display and communication between
    "focus" drop down list <-> query.focus."""

    def __init__(self):
        BaseInputPage.__init__(self)
        self.box = LabelledComboBox("Focus", self)
        self.add( self.box )

    def updateQuery(self, query):
        item = self.box.selectedItem
        if item: query.focus = item
        else: raise InvalidQueryException("Focus must be set")

    def updatePage(self, query):
        self.box.setText( query.focus )
        
    def toString(self):
        tmp = self.box.getText()
        if tmp: return tmp
        else: return "UNSET"



class DatabaseInputPage(QueryInputPage, Box, ChangeListener):

    def __init__(self):
        Box.__init__(self, BoxLayout.Y_AXIS)

        self.host = LabelledComboBox("Host", self)
        self.port = LabelledComboBox("Port", self)
        self.database = LabelledComboBox("Database", self)
        self.user = LabelledComboBox("User", self)
        self.password = LabelledComboBox("Password", self)

        self.add( self.host )
        self.add( self.port )
        self.add( self.database )
        self.add( self.user )
        self.add( self.password )
        self.add( Box.createVerticalGlue() )
        
        self.changeEvent = ChangeEvent( self )
        self.changeListeners = []


    def updateQuery(self, query):
        # todo
        pass

    def updatePage(self, query):
        # todo
        pass

    def clear(self):
        # todo
        pass

    def stateChanged(self, event=None):
        for l in self.changeListeners:
            l.stateChanged( self.changeEvent )

    def addChangeListener(self, listener):
        self.changeListeners.append( listener )

    def toString(self):
        desc = "UNSET22"
        tmp = self.host.getText()
        if tmp:
            desc = tmp
            tmp = self.port.getText()
            if tmp:
                desc = desc + ":" + tmp
            tmp = self.database.getText()
            if tmp:
                desc = desc + "/" + tmp
        return desc
        

class DestinationInputPage(BaseInputPage):

    def __init__(self):
        BaseInputPage.__init__(self)
        group = ButtonGroup()
        
        self.file = LabelledComboBox("File", self, group)
        self.window = LabelledComboBox("Window", self, group)
        
        self.add( self.file )
        self.add( self.window )

    def toString(self):
        # TODO next: display label properlyin tree node
        if self.file.isSelected():
            label =  "FILE: "+self.file.getText()
        elif self.window.isSelected():
            label = "WINDOW: " + self.window.getText()
        else:
            label = "UNSET"

        print label
        return label



class CardContainer(JPanel):
    """A JPanel with a CardLayout plus show() method for showing a particular card."""
    
    def __init__(self):
	self.layout = CardLayout()

    def show(self, cardName):
	"""Brings the specified card name to the front of container if it is available."""
	self.layout.show( self, cardName )



class QueryTreeNode(DefaultMutableTreeNode, TreeSelectionListener, ChangeListener): 

    """Represents a TreeNode which: (1) causes the corresponding
    self.targetComponent to be displayed when it is selected, (2)
    generates custom text for display in tree via toString() method,
    (3) Listens to changes in targetComponent and cause the tree to be
    redrawn when changes detected. """



    def __init__(self, tree, parent, position, cardContainer, targetComponent, targetCardName):
        
        """ Adds targetComponent to cardContainer with the name
        targetCardName. Makes this a tree listener and inserts self as
        child of parent at specified position."""

        DefaultMutableTreeNode.__init__(self)
	self.cardContainer = cardContainer
	self.targetComponent = targetComponent
        if targetComponent==None:
            self.targetComponent = DummyInputPage( targetCardName )
	self.targetCardName = targetCardName
        self.tree = tree
        self.tree.addTreeSelectionListener( self )
        self.tree.model.insertNodeInto( self, parent, position)
        cardContainer.add( self.targetComponent, self.targetCardName)
        self.targetComponent.addChangeListener( self )
        

    def toString(self):

        # The first time the node is rendered the length of the text
        # area to be rendered is set to the length of the string
        # returned.  Any future node string longer than this will be
        # cropped. Adding blank space "makes room" for long
        # descriptions later.
        SPACE="&nbsp;"
        
        return "<html><b>"+self.targetCardName+"</b>"+SPACE+self.targetComponent.toString()+SPACE*100+"</html>"


    def valueChanged(self, event):

	""" Brings the targetComponent to the front of the
	cardContainer if this was the node selected in the tree"""
	if self == event.newLeadSelectionPath.lastPathComponent:
	    self.cardContainer.show( self.targetCardName )
            #revalidate()

    def stateChanged(self, event=None):
        # redraw tree
        self.tree.repaint()


class TreeNavigationPanel(JPanel, QueryInputPage):


    def __init__(self):
        self.rootNode = DefaultMutableTreeNode( "Query" )
        treeModel = DefaultTreeModel( self.rootNode )
        tree = JTree( treeModel )
        configPanel = CardContainer()

        self.databasePage = DatabaseInputPage()
        dbNode = QueryTreeNode( tree, self.rootNode, 0, configPanel, self.databasePage, "DATABASE" )
        speciesNode = QueryTreeNode( tree, self.rootNode, 1, configPanel, SpeciesInputPage(),"SPECIES" )
        focusNode = QueryTreeNode( tree, self.rootNode, 2, configPanel, FocusInputPage(),"focus" )
        filtersNode = QueryTreeNode( tree, self.rootNode, 3, configPanel, None,"filter" )
        regionNode = QueryTreeNode( tree, filtersNode, 0, configPanel, None,"region" )
        outputNode = QueryTreeNode( tree, self.rootNode, 4, configPanel, None,"output" )
        attributesNode = QueryTreeNode( tree, outputNode, 0, configPanel, None,"attribute" )
        formatNode = QueryTreeNode( tree, outputNode, 1, configPanel, None,"format" )
        destinationNode = QueryTreeNode( tree, outputNode, 2, configPanel,
                                         DestinationInputPage(),"destination" )
        
        # expand branches in tree
	path = TreePath(self.rootNode).pathByAddingChild( filtersNode )
	tree.expandPath( path )
        path = TreePath(self.rootNode).pathByAddingChild( outputNode )
	tree.expandPath( path )
        path = path.pathByAddingChild( attributesNode )
	tree.expandPath( path )

        self.layout = BorderLayout()
	scrollPane = JScrollPane(tree)
	scrollPane.setPreferredSize( Dimension(350,300) )
        self.add(  scrollPane, BorderLayout.WEST )
	self.add( JScrollPane( configPanel ), BorderLayout.CENTER )


    def updateQuery(self, query):
        print "updating query"
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.updateQuery( query )

                
    def updatePage(self, query):
        print "updating pages"
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.targetComponent.updatePage( query )


    def clear(self):
        pass


class MartGUIApplication(JFrame):

    def __init__(self, closeOperation=JFrame.DISPOSE_ON_CLOSE):
        JFrame.__init__(self, "MartExplorer", defaultCloseOperation=closeOperation, size=(1000,600))
        self.queryPages = TreeNavigationPanel()
        self.buildGUI()
        self.visible=1


    def buildGUI(self):
        self.JMenuBar = self.createMenuBar()

        panel = JPanel()
        panel.layout = BoxLayout( panel, BoxLayout.Y_AXIS )
        panel.add( self.createToolBar() )
        panel.add( self.queryPages )
        self.contentPane.add( panel )


    def createMenuBar(self):

        fileMenu = JMenu("File")
        fileMenu.add( JMenuItem("Exit"
                                ,toolTipText="Quits Application"
                                ,actionPerformed=self.doExit) )

        queryMenu = JMenu("Query")
        queryMenu.add( JMenuItem("Insert test query"
                                ,toolTipText="Inserts a test query into the application"
                                ,actionPerformed=self.doInsertKakaQuery) )
        queryMenu.add( JMenuItem("Execute"
                                 ,toolTipText="Executes query"
                                 ,actionPerformed=self.doExecute) )
        queryMenu.add( JMenuItem("Clear"
                                 ,toolTipText="Clears query"
                                 ,actionPerformed=self.doClear) )
        
        helpMenu = JMenu("Help")
        helpMenu.add( JMenuItem("About"
                                 ,toolTipText="Displays about information"
                                 ,actionPerformed=self.doAbout) )
        
        menuBar = JMenuBar()
        menuBar.add( fileMenu )
        menuBar.add( queryMenu )
        menuBar.add( helpMenu )

        return menuBar


    def createToolBar(self):
        toolBar = JToolBar()
        toolBar.add( JButton("Clear", actionPerformed=self.doClear) )
        toolBar.add( JButton("Execute", actionPerformed=self.doExecute) )
        toolBar.add( JButton("Test Query", actionPerformed=self.doInsertKakaQuery) )
        return toolBar

    def doExit(self, event=None):
        System.exit(0)


    def doInsertKakaQuery(self, event=None):
        self.queryPages.databasePage.host.setText( "kaka.sanger.ac.uk" )
        self.queryPages.databasePage.user.setText( "anonymous" )
        self.queryPages.databasePage.database.setText( "ensembl_mart_11_1" )

        q = Query(species = "homo_sapiens"
                  ,focus = "gene" )
        q.addFilter( IDListFilter("gene_stable_id",
                                  File( System.getProperty("user.home")+"/dev/mart-explorer/data/gene_stable_id.test") ) )
        
        q.addFilter( IDListFilter("gene_stable_id",
                                  URL( "file://" +System.getProperty("user.home")+"/dev/mart-explorer/data/gene_stable_id.test") ) )
        q.addFilter( IDListFilter("gene_stable_id",
                                  array( ("ENSG00000177741"), String) ) )
        q.addAttribute( FieldAttribute("gene_stable_id") )
        #query.addFilter( IDListFilter("gene_stable_id", File( STABLE_ID_FILE).toURL() ) )
        #q.resultTarget = ResultFile( "/tmp/kaka.txt", SeparatedValueFormatter("\t") )
        # TODO need a result window
        #q.resultTarget = ResultWindow( "Results_1", SeparatedValueFormatter ("\t") ) 
        self.queryPages.updatePage( q )



    def executeQuery( self, query ):
        Engine().execute(query)


    def doExecute(self, event=None):
        q = Query()
        try:
            self.queryPages.updateQuery( q )
            import threading
            threading.Thread(target=self.executeQuery(q)).start()

        except (Exception,RuntimeException), e:
            JOptionPane.showMessageDialog( self,
                                           "Failed to execute query: " + e.getMessage(),
                                           "Error",
                                           JOptionPane.ERROR_MESSAGE)



    def doAbout(self, event=None):
        AboutDialog( visible=1 )



    def doClear(self, event=None):
        self.queryPages.clear()


if __name__=="__main__":
    import sys
    usage = "Usage: MartExplorerGUIApplication [ [-v] [-l LOGGING_FILE_URL] ]"
    if len(sys.argv)>1:
        parameter = sys.argv[1]

        if parameter=="-v":
            BasicConfigurator.configure()
            Logger.getRoot().setLevel(Level.INFO)

        elif parameter=="-h":
            print usage

        elif parameter=="-l":
            if len(sys.argv)>2:
                print "using logging file : ", sys.argv[2]
                PropertyConfigurator.configure( sys.argv[2] )
            else:
                print "ERROR: No logging file URL specified."
                print usage
                System.exit(0)
        else:
            Logger.getRoot().setLevel(Level.WARN)
        
    # 1=don't exit JVM, fast for development purposes
    if 1: MartGUIApplication(JFrame.DISPOSE_ON_CLOSE).visible=1
    else: MartGUIApplication(JFrame.EXIT_ON_CLOSE).visible=1
