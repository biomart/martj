#!/usr/bin/env martjython.sh
# MartExplorer Graphical User Interface.

# copyright EBI, GRL 2003

# cursor OR "active" bar whilst loading data

# clicking view results multiple times while already loading should
# pop up dialog box: "Already executing query, stop?" check state of cursor

# TODO "save to file" <NAME> and "copy to window" <NAME>

# TODO impl all updateQuery(), updatePage(), clear() methods. add
# stubs to InputPage. incl attr from query -> page!

# Move database to menu

# cache results so don't reload if query unchanged Query.equals()? or
# stateChanged -> dirty flag via state changed.

# handle gene_chrom_start / end, strand

# TODO 3 - FilterManagerPage + XXXFilterPages. add implementation for
# Filter . Clicking should cause a list of available (not already
# added) items to be shown. Selecting one of these will cause the
# respective config panel to be displayed. If the OK button is pressed
# on these then they are added to the query. Support removing filter
# and attribute items from quesry. "Delete" and or right click/delete.

# TODO 4 - Add Sequence attribute support

# TODO validation: chr start>0, end>start,
# TODO strand = -1, Unstranded +1
# TODO fetch chromosomes from db and load into drop down list.

# Handle tabs of queries / results

# Toggleable auto-update



import thread
from jarray import array
from java.lang import System, String, ClassLoader, RuntimeException
from java.lang import Thread
from java.io import File, FileOutputStream, ByteArrayOutputStream
from java.net import URL
from java.util import Arrays, Vector
from java.awt import CardLayout, Dimension, BorderLayout, Rectangle, Cursor
from java.awt.event import ActionListener, MouseAdapter
from javax.swing import JPanel, JButton, JFrame, JLabel, JComboBox, Box, BoxLayout
from javax.swing import JScrollPane, JMenu, JMenuItem, JMenuBar, JToolBar, JTree, JList
from javax.swing import ListSelectionModel, ButtonGroup, JRadioButton, JOptionPane, JTextArea
from javax.swing.event import ChangeEvent, ChangeListener, TreeSelectionListener
from javax.swing.tree import TreePath, DefaultTreeModel, DefaultMutableTreeNode
from javax.swing.border import EmptyBorder
from org.ensembl.mart.explorer import Query, IDListFilter, FieldAttribute, BasicFilter
from org.ensembl.mart.explorer import InvalidQueryException, Engine, FormatSpec
from org.apache.log4j import Logger, Level, PropertyConfigurator

# uncomment to use this logging conf file
PropertyConfigurator.configure( System.getProperty("user.home")
				+"/dev/mart-explorer/data/logging.conf" )

GAP = 5
SPACE=" &nbsp;"
        
def toVector(list):
    return Vector( Arrays.asList( list ) )

def platformSpecificPath( path ):
    return path.replace("/", File.separator )

def validate( value, name ):
    if not value or value=="" or String("").equals(value):
        raise InvalidQueryException(name + " must be set")

class EmptyMouseAdapter(MouseAdapter):
    pass

class CursorUtil:

    def __init__(self):
	self.mouseAdapter = EmptyMouseAdapter()
    
    def startWaitCursor(self, root): 
	""" Sets cursor for specified component to Wait cursor """
	root.getGlassPane().setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) )
	root.getGlassPane().addMouseListener(self.mouseAdapter)
	root.getGlassPane().setVisible(1)


    def stopWaitCursor(self, root):
	""" Sets cursor for specified component to normal cursor """
	root.getGlassPane().setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) )
	root.getGlassPane().removeMouseListener( self.mouseAdapter)
	root.getGlassPane().setVisible(0)
  

class Page(Box, ChangeListener):

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

    def label(self):
        return "<html>" + self.htmlSummary() + SPACE*50+"</html>"

    def htmlSummary(self):

	""" Returns summary description of the state of this input
	page in HTML format. This method should be implemented by
	derived classes. """

	return "TODO"

    def updateQuery(self, query):
        pass

    def updatePage(self, query):
        pass

    def clear(self):
        pass



class DummyPage(Page):
    def __init__(self, message):
	Page.__init__(self)
        self.add( JLabel( "TEMPORARY PANEL:  " +message ) )

    def addChangeListener( self, listener ):
        pass


class LabelledComboBox(Box, ActionListener):

    """ Abstract "label + combo box" component with optional radio
    button. Issues ChangeEvents to ChangeListeners if contents of
    combobox changes. Loads value from and into query through methods
    implemented in derived classes. """

    # TODO next: writing in text box should auto select radiobutton if
    # available

    def __init__(self, label, changeListener=None, radioButtonGroup=None):

        Box.__init__(self, BoxLayout.X_AXIS)

        if radioButtonGroup:
            self.radioButton = JRadioButton( label, 0 ,
					     actionPerformed=self.actionPerformed)
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

    def htmlSummary(self):
        item = self.box.selectedItem
        if item: return item
        else: return ""

    def actionPerformed( self, event=None ):
	if self.radioButton:
	    self.radioButton.selected = 1
        for l in self.changeListeners:
            l.stateChanged( self.changeEvent )

    def addChangeListener( self, listener ):
        self.changeListeners.append( listener )
        
    def setText(self, text):
        self.box.setSelectedItem( text )

    def getText(self):
	""" Returns a jython string, "" if nothing selected. """
	if self.box.selectedItem:
	    return self.box.selectedItem
	else:
	    return ""


    def isSelected(self):
        if self.radioButton: return self.radioButton.selected
        else: 0

    def getSelectedItem(self):
        return self.box.selectedItem



class SpeciesPage(Page):

    name = "speciesPage"

    """ Input component manages display and communication between
    "species" drop down list <-> query.species."""

    def __init__(self):
        Page.__init__(self)
        self.speciesBox = LabelledComboBox("Species", self)
        self.add( self.speciesBox )

    def updateQuery(self, query):
        item = self.speciesBox.getSelectedItem()
        if item: query.species = item
        else: raise InvalidQueryException("Species must be set")

    def updatePage(self, query):
        self.speciesBox.setText( query.species )
        
    def htmlSummary(self):
	desc = "<b>Species</b> "
        tmp = self.speciesBox.getText()
        if tmp:
	    desc = desc + tmp
	return desc


class FocusPage(Page):

    """ Input component manages display and communication between
    "focus" drop down list <-> query.focus."""

    name = "focus_page"

    def __init__(self):
        Page.__init__(self)
        self.box = LabelledComboBox("Focus", self)
        self.add( self.box )

    def updateQuery(self, query):
        item = self.box.getSelectedItem()
        if item: query.focus = item
        else: raise InvalidQueryException("Focus must be set")

    def updatePage(self, query):
        self.box.setText( query.focus )
        
    def htmlSummary(self):
        desc = "<b>Focus</b> "
	tmp = self.box.getText()
        if tmp:
	    desc = desc + tmp
        return desc



class DatabasePage(Page):

    name = "databasePage"

    def __init__(self):
        Page.__init__(self)

        self.host = LabelledComboBox("Host", self)
        self.port = LabelledComboBox("Port", self)
        self.database = LabelledComboBox("Database", self)
        self.user = LabelledComboBox("User", self)
        self.password = LabelledComboBox("Password", self)

        self.add( self.host )
        self.add( self.port )
        self.add( self.user )
        self.add( self.password )
        self.add( self.database )
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

    def htmlSummary(self):
        desc = "<b>Database</b> "
        tmp = self.host.getText()
        if tmp:
            desc = desc + tmp
            tmp = self.port.getText()
            if tmp:
                desc = desc + ":" + tmp
            tmp = self.database.getText()
            if tmp:
                desc = desc + "/" + tmp
        return desc

        
    def getHost(self):
        return self.host.getText()

    def getPort(self):
        return self.port.getText()

    def getUser(self):
        return self.user.getText()

    def getPassword(self):
        return self.password.getText()

    def getDatabase(self):
        return self.database.getText()

    

class ResultsPage(Page):

    name = "results_page"

    def __init__(self):
        Page.__init__(self)

        self.textArea = JTextArea(25,60)
        self.add( JScrollPane(self.textArea) )
        self.outputStream = GUIOutputStream( self.textArea ) 


    def htmlSummary(self):
	desc = "<b>Results</b> "
	return desc


    def getOutputStream(self):
        return self.outputStream


    def scrollToTop( self ):
        self.textArea.scrollRectToVisible( Rectangle(1,1) )
        

class GUIOutputStream( ByteArrayOutputStream ):

    def __init__(self, targetComponent):
        self.targetComponent = targetComponent

    def close( self ):
        ByteArrayOutputStream.close(self)
        self.targetComponent.text = self.toString( "utf8" )
        # clear buffer ready for next viewing
        ByteArrayOutputStream.reset(self)


class FormatPage(Page):

    name = "format_page"

    def __init__(self):
        Page.__init__(self)
        
        self.tabulated = JRadioButton( "Tabulated Format", 0, actionPerformed=self.actionPerformed )
	self.tab = JRadioButton( "tabs", 1, actionPerformed=self.actionPerformed )
	self.comma = JRadioButton( "comma", 0, actionPerformed=self.actionPerformed )

	self.fasta = JRadioButton( "FASTA Format", 0, actionPerformed=self.actionPerformed )

        group = ButtonGroup()
	group.add( self.tabulated )
	group.add( self.fasta )

	group2 = ButtonGroup()
	group2.add( self.tab )
	group2.add( self.comma )

	d = (500,35)
	tabulatedOptions = Box.createHorizontalBox()
	tabulatedOptions.preferredSize = d
	tabulatedOptions.maximumSize = d
	tabulatedOptions.add( Box.createHorizontalStrut(50) )
	tabulatedOptions.add( self.tabulated )
	tabulatedOptions.add( self.tab )
	tabulatedOptions.add( self.comma )
	tabulatedOptions.add( Box.createHorizontalGlue() )

	fastaOptions = Box.createHorizontalBox()
	fastaOptions.preferredSize = d
	fastaOptions.maximumSize = d
	fastaOptions.add( Box.createHorizontalStrut(50) ) 
	fastaOptions.add(self.fasta)
	fastaOptions.add( Box.createHorizontalGlue() ) 

	v = Box.createVerticalBox()
	v.add( tabulatedOptions )
        v.add( fastaOptions )
	v.add( Box.createVerticalGlue() )
	self.add( v )

	self.dependencies()


    def dependencies( self ):
	self.tab.enabled = self.tabulated.selected
	self.comma.enabled = self.tabulated.selected


    def actionPerformed(self, event=None):
	self.dependencies()
	self.stateChanged(self.changeEvent)


    def htmlSummary(self):

	desc = "<b>Format</b> "
	if self.tabulated.selected:
	    desc = desc + "Tabulated"
	    if self.tab.selected:
		desc = desc + " ( tabs ) "
	    elif self.comma.selected:
		desc = desc + " ( commas ) "
	elif self.fasta.selected:
	    desc = desc + "Fasta"
	return desc


    def getFormatSpec(self):
        if self.fasta.isSelected():
            fs = FormatSpec()
            fs.format = FormatSpec.FASTA
            return fs
        elif self.tabulated.isSelected():
            if self.tab.selected: sep = "\t"
            else: sep = ","
            return FormatSpec( FormatSpec.TABULATED, sep )
        else:
            raise InvalidQueryException("Format not set")


    def setFormatSpec(self, formatSpec):
        format = formatSpec.format
        if format==FormatSpec.FASTA:
            self.fasta.setSelected(1)
        elif format==FormatSpec.TABULATED:
            self.tabulated.setSelected(1)
            if formatSpec.separator==",":
                self.comma.setSelected(1)
            elif formatSpec.separator=="\t":
                self.tab.setSelected(1)
            else:
                raise InvalidQueryException("Unrecognised separator")
        else:
            raise InvalidQueryException("Unrecognised format.")

        self.dependencies()
        
class OutputPage(Page):

    name = "output_page"
    
    def htmlSummary(self):
	return "<html><b>Output</b></html>"



class SimpleAttributePage(Page):


    def __init__(self, attributeManager, field):
        Page.__init__(self)
        self.attributeManager = attributeManager
        self.field = field
        self.add( JLabel( self.field ) )
        self.add( JButton("remove", actionPerformed=self.actionPerformed) )
	self.name = field + "_attribute_page"

    def actionPerformed( self, event=None ):
        self.attributeManager.deselect( self )


    def htmlSummary(self):
        return self.field



class AttributeManagerPage(Page):

    """ Manages the attributes by supporting selecting and deselecting
    atributes. A list of available attributes is maintained and this
    is displayed in this page. Items selected from this list have a
    tree node constructed and inserted into the tree. Attributes
    removed from the tree (via the "remove" button on the
    SimpleAttribute page are readded to the available list."""

    name = "attribute_manager_page"

    def __init__(self):
        Page.__init__(self)
        self.selected = Vector()
        self.available = toVector(["gene_stable_id", "chr_name", "end", "strand"])
        self.availableWidget = JList( self.available,
                                      valueChanged=self.valueChanged,
                                      selectionMode=ListSelectionModel.SINGLE_SELECTION)
        panel = JPanel()
        
        panel.add( JLabel("Select an attribute to add it") )
        panel.add( JScrollPane( self.availableWidget ) )
        self.add( panel )
        # these must be set before select() is called.
        self.node = None
        self.tree = None
        self.path = None


    def valueChanged(self, event=None):
        """ Handles user clicking on an available attribute. """
        selected = self.availableWidget.selectedValue
        if selected:
            self.select( selected )


    def select( self, value ):
        
        """ Selecting an attribute means removing it from available
        and adding it to the tree."""
        
        # need to prevent the method running more than once in case it
        # is called as a result of multiple events (this happens on
        # Macs)
        if value in self.selected:
            return

        self.selected.add( value )
        self.available.remove( value )

        attributePage = SimpleAttributePage(self, value)
        node = QueryTreeNode( self.tree,
                              self.node,
                              self.node.childCount,
                              self.cardContainer,
                              attributePage)
        attributePage.node = node
        self.refreshView()


    def deselect( self, attributePage ):

        """ Remove attribute from tree and add it to the list of
        available attributes."""
        
        self.selected.remove( attributePage.field )
        self.available.add( attributePage.field )
        self.tree.model.removeNodeFromParent( attributePage.node)
        self.refreshView()


    def refreshView(self):
        self.cardContainer.show( self.node.targetCardName )

        # must explicitly call setListData() rather than .listData=
        # to avoid conversion problems on linux and windows
        self.availableWidget.setListData( self.available )

        self.tree.expandPath( self.path )


    def htmlSummary(self):
	return "<html><b>Attributes</b></html>"


    def updatePage(self, query):
        for attribute in query.attributes:
            if isinstance( attribute, FieldAttribute ):
                self.selected.add( attribute.name )
            # todo handle sequence attributes

    def updateQuery(self, query):
        for attributeName in self.selected:
            query.addAttribute( FieldAttribute( attributeName ) )
        # todo handle sequence attributes

    def clear(self):
        print "clear attribute pages"


class FilterPage(Page):

    name = "filter_page"

    def htmlSummary(self):
	return "<html><b>Filters</b></html>"

class RegionPage(Page):

    name = "region_page"

    def __init__(self):
	Page.__init__(self)
	self.chr = LabelledComboBox("Chromosome", self)
	self.start = LabelledComboBox("Start", self)
	self.end = LabelledComboBox("End", self)
	self.strand = LabelledComboBox("Strand", self)

	self.add(self.chr)
	self.add(self.start)
	self.add(self.end)
	self.add(self.strand)
    
    def htmlSummary(self):
	chr = self.chr.getText()
	if chr: chr = "chr"+chr
	else: chr = ""

	start = self.start.getText()
	if start: start = ":" + start
	else: start = ""

	end= self.end.getText()
	if end: end = "-" + end
	else: end = ""

	strand = self.strand.getText()
	if strand: strand = ":" + strand
	else: strand = ""

	return "<b>Region</b> " + chr + start + end + strand


    def updateQuery(self, query):
	chr = self.chr.getText()
	if chr and chr!="": query.addFilter( BasicFilter("chr_name","=",chr) )

	start = self.start.getText()
	if start and start!="": query.addFilter( BasicFilter("start",">=",start) )

	end = self.end.getText()
	if end and end!="": query.addFilter( BasicFilter("end","<=",end) )

	
    def updatePage(self, query):
	for f in query.filters:
	    if isinstance(f, BasicFilter):
		if f.type=="chr_name":
		    self.chr.setText( f.value )
		elif f.type=="start":
		    self.start.setText( f.value )
		elif f.type=="end":
		    self.end.setText( f.value )
		elif f.type=="strand":
		    self.strand.setText( f.value )		    


    def clear(self):
        # todo
        pass



class GeneTypeFilterPage(Page):

    name = "gene_type_filter_page"

    def __init__(self):
	Page.__init__(self)
	self.id = LabelledComboBox("Gene Type", self)
	self.add( self.id )
	
    def htmlSummary(self):
	id = self.id.getText()
	if not id: id ==""
	return "<b>Gene Type</b> " + id

    def updatePage(self, query):
	# todo
	pass


    
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



    def __init__(self, tree, parent, position, cardContainer, targetComponent):
        
        """ Adds targetComponent to cardContainer with the name
        targetCardName. Makes this a tree listener and inserts self as
        child of parent at specified position."""

        DefaultMutableTreeNode.__init__(self)
	self.cardContainer = cardContainer
	self.targetComponent = targetComponent
        if targetComponent==None:
            self.targetComponent = DummyPage( targetCardName )
	self.targetCardName = targetComponent.name
        self.tree = tree
        self.tree.addTreeSelectionListener( self )
        self.tree.model.insertNodeInto( self, parent, position)
        cardContainer.add( self.targetComponent, self.targetCardName)
        self.targetComponent.addChangeListener( self )
        

    def toString(self):
	desc = self.targetComponent.label()
        return desc


    def valueChanged(self, event):

	""" Brings the targetComponent to the front of the
	cardContainer if this was the node selected in the tree"""
	if event.newLeadSelectionPath and self == event.newLeadSelectionPath.lastPathComponent:
	    self.cardContainer.show( self.targetCardName )

    def stateChanged(self, event=None):
        # redraw tree
        self.tree.repaint()



class QueryEditor(JPanel):


    def __init__(self):
        self.rootNode = DefaultMutableTreeNode( "Query" )
        treeModel = DefaultTreeModel( self.rootNode )
        tree = JTree( treeModel )
        self.cardContainer = CardContainer()

        self.databasePage = DatabasePage()
	self.formatPage = FormatPage()
	
        dbNode = QueryTreeNode( tree, self.rootNode, 0, self.cardContainer,
				self.databasePage )
        speciesNode = QueryTreeNode( tree, self.rootNode, 1, self.cardContainer,
				     SpeciesPage())
        focusNode = QueryTreeNode( tree, self.rootNode, 2, self.cardContainer,
				   FocusPage())
        filtersNode = QueryTreeNode( tree, self.rootNode, 3, self.cardContainer,
				     FilterPage())
        regionNode = QueryTreeNode( tree, filtersNode, 0, self.cardContainer,
				    RegionPage())
	geneTypeFilterNode = QueryTreeNode( tree, filtersNode, 1, self.cardContainer,
						GeneTypeFilterPage())
        outputNode = QueryTreeNode( tree, self.rootNode, 4, self.cardContainer,
				    OutputPage())
        attributesPage = AttributeManagerPage()
        attributesNode = QueryTreeNode( tree, outputNode, 0, self.cardContainer,
					attributesPage)
        formatNode = QueryTreeNode( tree, outputNode, 1, self.cardContainer,
				    self.formatPage)
        
        # expand branches in tree
	path = TreePath(self.rootNode).pathByAddingChild( filtersNode )
	tree.expandPath( path )
        path = TreePath(self.rootNode).pathByAddingChild( outputNode )
	tree.expandPath( path )
        path = path.pathByAddingChild( attributesNode )
	tree.expandPath( path )

        attributesPage.node = attributesNode
        attributesPage.path = path
        attributesPage.tree = tree
        attributesPage.cardContainer = self.cardContainer

        self.layout = BorderLayout()
	scrollPane = JScrollPane(tree)
	scrollPane.setPreferredSize( Dimension(350,300) )
        self.add(  scrollPane, BorderLayout.WEST )
	self.add( self.cardContainer , BorderLayout.CENTER )

    def addPage( self, page ):
	self.cardContainer.add( page, page.name )


    def showPage( self, pageName ):
	self.cardContainer.show( pageName )

    def updateQuery(self, query):
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.targetComponent.updateQuery( query )

                
    def updatePage(self, query):
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.targetComponent.updateQuery( query )

                
    def updatePage(self, query):
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.targetComponent.updatePage( query )


    def clear(self):
        pass

    def getHost(self):
        return self.databasePage.getHost()

    def getPort(self):
        return self.databasePage.getPort()

    def getUser(self):
        return self.databasePage.getUser()

    def getPassword(self):
        return self.databasePage.getPassword()    

    def getDatabase(self):
        return self.databasePage.getDatabase()

    def getFormatSpec(self):
        return self.formatPage.getFormatSpec()

    def getOutputStream(self):
        return self.resultsPage.getOutputStream()



class MartGUIApplication(JFrame):

    def __init__(self, closeOperation=JFrame.DISPOSE_ON_CLOSE):
        JFrame.__init__(self, "MartExplorer", defaultCloseOperation=closeOperation, size=(1000,600))
        self.editor = QueryEditor()
	self.resultsPage = ResultsPage()
	self.editor.addPage( self.resultsPage )
        self.buildGUI()
        self.visible=1
	self.cursorUtil = CursorUtil()

    def buildGUI(self):
        self.JMenuBar = self.createMenuBar()

        panel = JPanel()
        panel.layout = BoxLayout( panel, BoxLayout.Y_AXIS )
        panel.add( self.createToolBar() )
        panel.add( self.editor )
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
        toolBar.add( JButton("View Results", actionPerformed=self.doViewResults) )
        toolBar.add( JButton("Save Results", actionPerformed=self.doSaveResults) )
        toolBar.add( JButton("Test Query", actionPerformed=self.doInsertKakaQuery) )
        return toolBar


    def doViewResults(self, event=None):
        self.viewResults()

    def doSaveResults(self, event=None):
        # open popup window window asking for filename

        # execute query, piping results to file.
        pass



    def doExit(self, event=None):
        System.exit(0)


    def doInsertKakaQuery(self, event=None):
        self.editor.databasePage.host.setText( "kaka.sanger.ac.uk" )
        self.editor.databasePage.user.setText( "anonymous" )
        self.editor.databasePage.database.setText( "ensembl_mart_11_1" )

        self.editor.formatPage.setFormatSpec( FormatSpec(FormatSpec.TABULATED, ",") )

        q = Query(species = "homo_sapiens"
                  
                  ,focus = "gene" )
	q.addFilter( BasicFilter("chr_name", "=", "22") )
	#q.addFilter( BasicFilter("start", "=", "1") )
	#q.addFilter( BasicFilter("end", "=", "1000") )
	#q.addFilter( BasicFilter("strand", "=", "1") )

        # load test data file via classpath; this works from a
        # deployed jar and from normal directory in developement
        # situations.
        url = ClassLoader.getSystemResource("data/gene_stable_id.test")
        q.addFilter( IDListFilter("gene_stable_id", url ) )
        q.addFilter( IDListFilter("gene_stable_id",
                                  array( ("ENSG00000177741"), String) ) )
        q.addAttribute( FieldAttribute("gene_stable_id") )
        #query.addFilter( IDListFilter("gene_stable_id", File( STABLE_ID_FILE).toURL() ) )
        #q.resultTarget = ResultFile( "/tmp/kaka.txt", SeparatedValueFormatter("\t") )
        # TODO need a result window
        #q.resultTarget = ResultWindow( "Results_1", SeparatedValueFormatter ("\t") ) 
        self.editor.updatePage( q )




    def viewResults( self ):
	self.editor.showPage( ResultsPage.name )
        # execute query in new thread and pipe results to window.
        os = self.resultsPage.getOutputStream()
	thread.start_new_thread( self.executeQuery, (self, os) )



    def executeQuery( self, dummy=None, outputStream=None ):


        host = self.editor.getHost()
        port = self.editor.getPort()
        user = self.editor.getUser()
        password = self.editor.getPassword()
        database = self.editor.getDatabase()

        query = Query()
        formatSpec = None
        # handles valildation and loads query
        try:
	    self.cursorUtil.startWaitCursor(self)

            validate(host, "Host")
            validate(user, "User")
            validate(database, "Database")
            formatSpec = self.editor.getFormatSpec()
            self.editor.updateQuery( query )

            engine = Engine(host,
                            port,
                            user,
                            password,
                            database)
            engine.execute( query, formatSpec, outputStream )
	    
	    # scroll to top of results
	    self.resultsPage.scrollToTop()
	    self.cursorUtil.stopWaitCursor(self)

        except (InvalidQueryException), ex:
            JOptionPane.showMessageDialog( self,
                                           "Failed to execute query: " + ex.message,
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE)
	    ex.printStackTrace()


    def doExecute(self, event=None):
        thread.start_new_thread( self.executeQuery, (self, "f") )

    def doAbout(self, event=None):
        JOptionPane.showMessageDialog(self,
                                      "MartExplorer development version. Copyright EBI and GRL.",
                                      "About MartExplorer",
                                      JOptionPane.OK_OPTION )


    def doClear(self, event=None):
        self.editor.clear()



def main(args, quitOnExit):
    # default logging level, needed by engine
    Logger.getRoot().setLevel(Level.WARN)
    
    usage = "Usage: MartExplorerGUIApplication [ [-h] [-v] [-l LOGGING_FILE_URL] ]"
    if len(args)>1:
        parameter = args[1]

        if parameter=="-v":
            BasicConfigurator.configure()
            Logger.getRoot().setLevel(Level.INFO)

        elif parameter=="-h":
            print usage
            System.exit(0)

        elif parameter=="-l":
            if len(args)>2:
                print "using logging file : ", args[2]
                PropertyConfigurator.configure( args[2] )
            else:
                print "ERROR: No logging file URL specified."
                print usage
                System.exit(0)
        else:
            Logger.getRoot().setLevel(Level.WARN)

    if quitOnExit:
        closeAction = JFrame.EXIT_ON_CLOSE
    else:
        closeAction = JFrame.DISPOSE_ON_CLOSE
    MartGUIApplication( closeAction ).visible=1

if __name__=="__main__":
    import sys
    main( sys.argv, 0 )



