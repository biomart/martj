#!/usr/bin/env martjython.sh
# MartExplorer Graphical User Interface.

# copyright EBI, GRL 2003


# editor.queryChanged / only execute if queryChanged or db settings changed.


# TODO 3 - FilterManagerPage + XXXFilterPages. add implementation for
# Filter . Clicking should cause a list of available (not already
# added) items to be shown. Selecting one of these will cause the
# respective config panel to be displayed. If the OK button is pressed
# on these then they are added to the query. Support removing filter
# and attribute items from quesry. "Delete" and or right click/delete.

# TODO "copy to window" <NAME>

# Handle invalid port- currently freezes app.

# handle gene_chrom_start / end, strand

# TODO Consider loading results piecemealfrom a file to avoid large
# files being loaded in memory.

# TODO validation: chr start>0, end>start,
# TODO strand = -1, Unstranded +1
# TODO fetch chromosomes from db and load into drop down list.

# Handle tabs of queries / results

# Toggleable auto-update

# TODO $HOME/.mart-explorer-history file

# TODO Feature group boxes list collapsable

# TODO add sequence images (see wills email)

# TODO combine species + focus into treenode=Dataset,
# page=species+focus, make the titles "species" and "focus"
# configurable.

# TODO make Attribute pages be LIST + CardContainer Panel because list
# could be v. long.

# TODO schema -> dumb_configure.xml where each table is a card in the
# Features page.

# TODO support 2 stage queries.

# TODO low: redo sequence page with clever dependency checking:
# leftFlank, leftUTR, Thing-in-middle, rightUTR, rightFlank

# TODO low: count, autocount update checkbox

# TODO low: karyotype preview

import thread
from jarray import array
from java.lang import System, String, ClassLoader, RuntimeException
from java.lang import Thread
from java.io import File, FileOutputStream, ByteArrayOutputStream
from java.net import URL
from java.util import Arrays, Vector, Collections
from java.awt import CardLayout, Dimension, BorderLayout, Rectangle, Cursor, Color
from java.awt.event import ActionListener, MouseAdapter
from javax.swing import JPanel, JButton, JFrame, JLabel, JComboBox, Box, BoxLayout
from javax.swing import JScrollPane, JMenu, JMenuItem, JMenuBar, JToolBar, JTree, JList
from javax.swing import ListSelectionModel, ButtonGroup, JRadioButton, JOptionPane
from javax.swing import JFileChooser, JTextArea, JTextField, JTabbedPane, BorderFactory
from javax.swing import JCheckBox
from javax.swing.event import ChangeEvent, ChangeListener, TreeSelectionListener
from javax.swing.tree import TreePath, DefaultTreeModel, DefaultMutableTreeNode
from javax.swing.border import EmptyBorder
from org.ensembl.mart.explorer import *
from org.apache.log4j import Logger, Level, PropertyConfigurator, BasicConfigurator

# uncomment to use this logging conf file
#PropertyConfigurator.configure( System.getProperty("user.home")
#				+"/dev/mart-explorer/data/logging.conf" )

#DEFAULT_HOST = "kaka.sanger.ac.uk"
#DEFAULT_DATABASE = "ensembl_mart_11_1"
DEFAULT_HOST = "127.0.0.1"
DEFAULT_DATABASE = "dl_testmart_newnames"
DEFAULT_PORT = "3307"
DEFAULT_USER  = "ensro"
DEFAULT_PASSWORD = ""

APPLICATION_SIZE = (1000,700)
ATTRIBUTE_WIDGET_SIZE = (170, 30)
DEFAULT_FLANKING_SIZE = "1000"
GAP = 5
SPACE=" &nbsp;"


defaultAttributeConfiguration = (
    ( "Features",
      ( "REGION",
        ( ("Chromosome Attributes", None),
          ( ("Chromosome Name","chr_name"),
            ("Start Position (bp)","gene_chrom_start"),
            ("End Position (bp)","gene_chrom_end"),
            ("Band","band"),
            ("Strand","chrom_strand")
            )
          )
        ), # REGION
      
      ( "GENE",
        ( ("Ensembl Attributes", None),
          ( ("Ensembl Gene ID","gene_stable_id"),
            ("Ensembl Transcript ID","transcript_stable_id"),
            ("External Gene ID","display_id"),
            ("Description","description"),
            ("Ensembl Peptide ID","translation_stable_id_v"),
            ("External Gene DB","db_name")
            )
          ),

        ( ("External Reference Attributes (max 3)", 3 ),
          ( ("Protein ID","display_id", "xref_protein_id"),
            ("GO ID","display_id", "xref_GO"),
            ("SPTrEMBL ID","display_id", "xref_SPTREMBL"),
            ("SWISSPROT ID","display_id", "xref_SWISSPROT"),
            ("MIM ID","display_id", "xref_MIM"),
            ("LocusLink ID","","xref_LocusLink"),
            ("HUGO ID","display_id","xref_HUGO"),
            ("GO Description","description","xref_GO"),
            ("EMBL ID","display_id","xref_EMBL"),
            ("PDB ID","display_id","xref_PDB"),
            ("RefSeq ID","display_id","xref_RefSeq"),
            ("GKB ID","display_id","xref_GKB")
             )
          ),

##         ( ("Microarray Attributes", 1),
##           ( "AFFY HG U9",
##             "Sanger HVER 1 2 1",
##             "AFFY HG U133",
##             "UMCU 19Kv1",
##             )
##           ),
        
##         ( ("Disease Attributes", None),
##           ( "Disease OMIM ID",
##             "Disease Description"
##             )
##           ),
        
         ), # GENE

##       # TODO EXPRESSION

##       # TODO MULTI SPECIES COMPARISON

##       PROTEIN

##       ), FEATURES
    
    
##     ( "SNPs",
##       ( "REGION",
##         ( ( "Chromosome Attributes",  None),
##           ( "Chromosome Name",
##             "Start Position (bp)",
##             "End Position (bp)",
##             "Band",
##             "Strand"
##             )
##           )
##         ) # REGION
##       ), # SNPS


##     ( "Structures",
##       ( "REGION",
##         ( ( "Chromosome Attributes",  None),
##           ( "Chromosome Name",
##             "Start Position (bp)",
##             "End Position (bp)",
##             "Band",
##             "Strand"
##             )
##           )
##         ) # REGION
      ),

    )


        
class AttributeManager(ChangeListener ):

    """ Listens for statechange events; these occur when a button is
    selected and deselected. Selections cause a node corresponding to
    this option to be added to the tree and conversely, deselection
    causes it to be removed. """

    def __init__(self,
                 label,
                 columnName,
                 tableConstraint,
                 attributePage,
                 group=None):

        self.attributePage = attributePage
        self.attribute = FieldAttribute( columnName )
        
        if tableConstraint:
            self.attribute.tableConstraint =  tableConstraint 
        
        self.__widget = JPanel( BorderLayout() )
        self.__widget.preferredSize = ATTRIBUTE_WIDGET_SIZE
        self.name = str(self)

        if group:
            self.button = JRadioButton(label)
            group.add( self.button )
        else:
            self.button = JCheckBox(label)

        self.button.model.addChangeListener( self )
        self.__widget.add( self.button, BorderLayout.WEST)
        self.node = DefaultMutableTreeNode( label )
	self.nodeInTree = None

    def getWidget( self ):
	return self.__widget


    def getNode( self ):
	return self.__node


    def clear( self, event=None ):
        self.button.selected = 0


    def updateQuery(self, query):
        if self.button.selected and not query.hasAttribute( self.attribute ):
            query.addAttribute( self.attribute )

    def updatePage(self, query):

        self.button.selected = query.hasAttribute( self.attribute )
        

    def stateChanged( self, event=None):

         """ Adds / removes node from tree when button clicked.  Can
         be called several times per click"""
	
	 if self.button.selected : 
	     if not self.nodeInTree: # prevents trying to add node to tree several times per "click"
		 self.attributePage.addNode( self.node )
		 self.nodeInTree = 1
	 else : 
	     if self.nodeInTree:
		 self.attributePage.removeNode( self.node )
		 self.nodeInTree = None
		 




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
        self.box.removeAllItems()

    def setItems(self, items):
        self.clear()
        map(self.box.addItem, items)

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



class Dataset:

    def __init__(self, description, starNames, primaryKeys):
        self.description = description
        self.starNames = starNames
        self.primaryKeys = primaryKeys


class DatasetPage(Page):

    name = "dataset_page"

    """ Input component manages display and communication between
    "dataset" drop down list <-> query.dataset."""

    def __init__(self):
        Page.__init__(self)
        self.datasetBox = LabelledComboBox("Dataset", self)
        self.add( self.datasetBox )
        self.configure([])

    def configure(self, datasets):
        """ Configures this widget using a list of Dataset objects. """

        self.nameToDataset = {} 
        self.descriptions = []
        self.datasets = datasets
        for d in datasets:
            description = d.description
            self.descriptions.append( description )
            self.nameToDataset[ description ] = d
            for s in d.starNames:
                # Assume star names are unique to datasets.
                self.nameToDataset[ s ] = d
        self.datasetBox.setItems( [""]+self.descriptions )


    def clear( self ):
        self.datasetBox.setText("")
        self.datasetBox.clear()
        

    def updateQuery(self, query):
        description = self.datasetBox.getSelectedItem()
        if description:
            dataset = self.nameToDataset[ description ]
            if dataset:
                query.starBases = dataset.starNames
                query.primaryKeys = dataset.primaryKeys
            else:
                raise InvalidQueryException("Unkown dataset: " + description)
        else:
            raise InvalidQueryException("Dataset must be set")


    def updatePage(self, query):
        starName = query.starBases[0]
        dataset = self.nameToDataset[ starName ]
        if dataset:
            starNames  = dataset.starNames
            # note: we ignore the primary keys from the query in preference
            # for the ones in the dataset.
            primaryKeys = dataset.primaryKeys
            self.datasetBox.setText( dataset.description )
        else:
           raise InvalidQueryException("No dataset found containing star name: "
                                       + starName ) 
        
    def htmlSummary(self):
	desc = "<b>Dataset</b> "
        tmp = self.datasetBox.getText()
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
	defaultsButton = JButton("Default Database Settings"
				 ,actionPerformed=self.defaultSettings)

        self.add( self.host )
        self.add( self.port )
        self.add( self.user )
        self.add( self.password )
        self.add( self.database )
	self.add( defaultsButton )
        self.add( Box.createVerticalGlue() )
        
        self.changeEvent = ChangeEvent( self )
        self.changeListeners = []

    def defaultSettings(self, event=None):
	self.host.setText( DEFAULT_HOST )
	self.port.setText( DEFAULT_PORT )
	self.database.setText( DEFAULT_DATABASE )
	self.user.setText( DEFAULT_USER )
	self.password.setText( DEFAULT_PASSWORD )
	


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


    def clear( self ):
        self.textArea.text=""


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

        self.group = ButtonGroup()
	self.group.add( self.tabulated )
	self.group.add( self.fasta )

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




    def clear( self ):
        self.group.remove( self.tabulated  )
        self.group.remove( self.fasta  )
        self.tabulated.setSelected(0)
        self.fasta.setSelected(0)
        self.group.add( self.tabulated  )
        self.group.add( self.fasta  )
        self.comma.setSelected(1)
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


class ColumnContainer(JPanel):

    def __init__(self, nColumns=2, components=None):
        JPanel.__init__(self)

        self.nColumns = nColumns
        self.columns = []
        self.index = 0

        box = Box.createHorizontalBox()
        for i in range(nColumns):
            self.columns.append( Box.createVerticalBox() )
        map( box.add, self.columns )
        JPanel.add(self, box)

        if components:
            map(self.add, components)


    def add( self, component):
        nColumns = len( self.columns )
        self.columns[ self.index%nColumns ].add( component ) 
        self.index = self.index + 1

    def freezeMaximumSize(self):
        self.maximumSize = self.preferredSize


class SequencePage(Page):

    def __init__(self, attributePage):
        Page.__init__(self)
        self.attributePage = attributePage
        self.name = "sequence"
	self.node = DefaultMutableTreeNode( self )
	self.nodeInTree = None
        self.clearButton = JButton("Clear", actionPerformed=self.clear)

        self.transcript = JRadioButton( "Transcripts/proteins"
                                        ,actionPerformed=self.actionPerformed)
        self.gene = JRadioButton( "Genes - transcript information ignored (one output per gene)"
                                  ,actionPerformed=self.actionPerformed)

        self.includeGeneSequence = JRadioButton( "Gene sequence only"
                                  ,actionPerformed=self.actionPerformed)
        self.includeGeneSequence_5_3 = JRadioButton( "Gene plus 5' and 3' flanks"
                                  ,actionPerformed=self.actionPerformed )
        self.includeGeneSequence_5 = JRadioButton( "Gene plus 5' flank"
                                  ,actionPerformed=self.actionPerformed )
        self.includeGeneSequence_3 = JRadioButton( "Gene plus 3' flank"
                                  ,actionPerformed=self.actionPerformed )
        self.includeUpstream = JRadioButton( "5' upstream only"
                                  ,actionPerformed=self.actionPerformed )
        self.includeDownStream = JRadioButton( "3' downstream only"
                                  ,actionPerformed=self.actionPerformed )
        self.includeUpStreamUTROnly = JRadioButton( "5' UTR only"
                                  ,actionPerformed=self.actionPerformed )
        self.includeUpStreamAndUTR = JRadioButton( "5' upstream and UTR"
                                  ,actionPerformed=self.actionPerformed )
        self.includeDownStreamUTROnly = JRadioButton( "3' UTR only"
                                  ,actionPerformed=self.actionPerformed )
        self.includeDownStreamAndUTR = JRadioButton( "3' UTR and downstream"
                                  ,actionPerformed=self.actionPerformed )
        self.includeExonSequence = JRadioButton( "Exon sequences"
                                  ,actionPerformed=self.actionPerformed )
        self.includecDNASequence = JRadioButton( "cDNA sequence only"
                                  ,actionPerformed=self.actionPerformed )
        self.includeCodingSequence = JRadioButton( "Coding sequence only"
                                  ,actionPerformed=self.actionPerformed )
        self.includePeptide = JRadioButton( "Peptide"
                                  ,actionPerformed=self.actionPerformed )

        self.includeButtons = (self.includeGeneSequence
                                  ,self.includeGeneSequence_5_3
                                  ,self.includeGeneSequence_5
                                  ,self.includeUpstream
                                  ,self.includeUpStreamUTROnly
                                  ,self.includeUpStreamAndUTR
                                  ,self.includeGeneSequence_3
                                  ,self.includeDownStream
                                  ,self.includeDownStreamUTROnly
                                  ,self.includeDownStreamAndUTR
                                  ,self.includeExonSequence
                                  ,self.includecDNASequence
                                  ,self.includeCodingSequence
                                  ,self.includePeptide)
        
        self.geneButtons = (
            self.includeGeneSequence
            ,self.includeGeneSequence_5_3
            ,self.includeGeneSequence_5
            ,self.includeGeneSequence_3
            ,self.includeUpstream
            ,self.includeDownStream
            ,self.includeExonSequence
            )

	self.transcriptOnlyButtons = (
	    self.includeUpStreamUTROnly
	    ,self.includeUpStreamAndUTR
	    ,self.includeDownStreamUTROnly
	    ,self.includeDownStreamAndUTR
	    ,self.includecDNASequence
	    ,self.includeCodingSequence
	    ,self.includePeptide
	    )

	max = (100,25)

        self.flank5Label = JLabel("5' Flanking region")
        self.flank5 = JTextField(DEFAULT_FLANKING_SIZE)
	self.flank5.maximumSize=max
        self.flank5.preferredSize=max
        flank5Panel = ColumnContainer(2,(self.flank5Label, self.flank5))

	self.flank3Label = JLabel("3' Flanking region")
        self.flank3 = JTextField(DEFAULT_FLANKING_SIZE)
        self.flank3.maximumSize=max
        self.flank3.preferredSize=max
        flank3Panel = ColumnContainer(2,(self.flank3Label, self.flank3))
        
        self.typeGroup = ButtonGroup()
        self.includeGroup = ButtonGroup()
	self.noInclude = JRadioButton()
	self.noType = JRadioButton()
        map( self.includeGroup.add, self.includeButtons + ( self.noInclude, ) )
        map( self.typeGroup.add, ( self.transcript, self.gene, self.noType) )
        self.dependencies()

        includePanel = ColumnContainer()
	includePanel.border = BorderFactory.createEmptyBorder( 0,30,0,0 )
        map( includePanel.add, self.includeButtons
             + ( flank3Panel, flank5Panel) )
        includePanel.freezeMaximumSize()

        # hPanel is capable of displaying primary buttons in top
        # left of panel it is added to.
        hPanel = Box.createHorizontalBox()
        vPanel = Box.createVerticalBox()
        map( vPanel.add, (self.gene, self.transcript) )
        hPanel.add( vPanel )
        hPanel.add( Box.createHorizontalGlue() )
        
        map( self.add, (self.clearButton
                        ,hPanel
                        , includePanel
                        , Box.createVerticalGlue()) )
	self.changeEvent = ChangeEvent( self )
	

    def dependencies( self ):

        """ Enables and disables input options based on the current
        state of the page. """

        if self.transcript.selected:
            for b in self.includeButtons:
                b.enabled = 1
        elif self.gene.selected:
            for b in self.includeButtons:
                b.enabled = 0
            for b in self.geneButtons:
                b.enabled = 1
        else:
            for b in self.includeButtons:
                b.enabled = 0

        flank5Enabled = self.includeGeneSequence_5_3.selected or self.includeGeneSequence_5.selected or self.includeUpstream.selected or self.includeUpStreamAndUTR.selected
        self.flank5.enabled = flank5Enabled
        self.flank5Label.enabled = flank5Enabled


        flank3Enabled = self.includeGeneSequence_5_3.selected or self.includeGeneSequence_3.selected or self.includeDownStream.selected or self.includeDownStreamAndUTR.selected
        self.flank3.enabled = flank3Enabled
        self.flank3Label.enabled = flank3Enabled



    def actionPerformed(self, event=None):

	# ensure only valid gene options selcted. Could convert this
	# into a fix or cancel option rather than auto fix.
	if self.gene.selected:
	    for b in self.transcriptOnlyButtons:
		if b.selected:
		    # deselect invalid option
		    self.noInclude.selected = 1
		    JOptionPane.showMessageDialog(self.rootPane
						  ,"Deselected \"" + b.text
                                                  + "\" because that option is unavailble when target is Genes."
						  ,"Invalid sequence combination."
						  ,JOptionPane.WARNING_MESSAGE )
		
	self.dependencies()
	if not self.nodeInTree:
            self.addNode()
	else:
	    self.attributePage.refreshView()


    def clear( self, event=None ):
	self.noInclude.selected = 1
	self.noType.selected = 1
        self.removeNode()
        self.flank3.text = DEFAULT_FLANKING_SIZE
        self.flank5.text = DEFAULT_FLANKING_SIZE
	self.dependencies()


    def toString(self):
	""" Provides label to tree node. """
        if self.gene.selected: focus = "Gene sequence"
        elif self.transcript.selected: focus = "Transcript sequence"
        else: focus = ""

        include=""
        for b in self.includeButtons:
            if b.selected: include = " - " +b.text
        
        # " "*20 is a hack to ensure swing leaves enough room for long lables later
	return focus + include + " "*60








    def updateQuery(self, query):
        sd = None
        if self.transcript.selected and self.includeCodingSequence.selected:
            sd = SequenceDescription( SequenceDescription.TRANSCRIPTCODING ) 
        elif self.transcript.selected and self.includePeptide.selected:
            sd = SequenceDescription( SequenceDescription.TRANSCRIPTPEPTIDE )

        if sd:
            query.sequenceDescription = sd


        
    def updatePage(self, query):

        sd = query.sequenceDescription
        if sd:
            # must bring to front BEFORE add sequence to avoid it
            # being wiped by a callback to clear()
            self.attributePage.toFront( self )

            if sd.type==SequenceDescription.TRANSCRIPTCODING:
                self.transcript.selected = 1
                self.dependencies()
                self.includeCodingSequence.selected = 1

            elif sd.type==SequenceDescription.TRANSCRIPTPEPTIDE:
                self.transcript.selected = 1
                self.dependencies()
                self.includePeptide.selected = 1

            else:
                raise InvalidQueryException("Unsupported sequence description")

            self.addNode()

        

    def addNode( self ):
        if not self.nodeInTree:
            self.attributePage.addNode( self.node )
            self.nodeInTree = 1

    def removeNode( self ):
        if self.nodeInTree:
            self.attributePage.removeNode( self.node )
            self.nodeInTree = 0
        

class AttributeSubPage(Page):

    def __init__(self, attributePage, configuration):
	Page.__init__(self)
        self.attributePage = attributePage
        self.name = configuration[0]
        self.attributeManagers = []
        self.noneButtons = []
        tabPanel = Box.createVerticalBox()
        clearButton = JButton("Clear", actionPerformed=self.clear )
        tabPanel.add( clearButton )
        for groupData in configuration[1:]:
            # groupData level
            groupPanel = Box.createVerticalBox()
            groupPanel.border=BorderFactory.createTitledBorder( groupData[0] ) 
            tabPanel.add( groupPanel ) 
            for subGroup in groupData[1:]:
                # option group level
                groupTitle = subGroup[0][0]
                
                # Support 2 column lists of options
                colPanel = ColumnContainer()
                colPanel.border = BorderFactory.createTitledBorder( subGroup[0][0] )
                groupPanel.add( colPanel )
                
                # handle radio button lists
                radio = subGroup[0][1]==1
                if radio:
                    radioGroup = ButtonGroup()
                    noneButton = JRadioButton("None", selected=1)
                    radioGroup.add( noneButton )
                    self.noneButtons.append( noneButton )
                    colPanel.add( noneButton )
                else:
                    radioGroup = None
                    clearButton = None
                    
                for attribute in subGroup[1]:
                    if len(attribute)==2:
                        am = AttributeManager( attribute[0], attribute[1], None, self, radioGroup )
                    elif len(attribute)==3:
                        am = AttributeManager( attribute[0], attribute[1], attribute[2], self, radioGroup )
                    self.attributeManagers.append( am )
                    colPanel.add( am.getWidget() )
                    
                # prevent panel being stretched by layout manager
                colPanel.freezeMaximumSize()

        self.add( tabPanel )
        self.add( Box.createVerticalGlue() )


    def addNode(self, node):
        self.attributePage.addNode( node )

    def removeNode(self, node):
        self.attributePage.removeNode( node )


    def clear(self, event=None):
        for b in self.noneButtons:
            b.selected = 1
        for am in self.attributeManagers:
            am.clear()


    def updateQuery(self, query):
        for am in self.attributeManagers:
            am.updateQuery( query )

    def updatePage(self, query):
        for am in self.attributeManagers:
            am.updatePage( query )



class AttributePage(Page, TreeSelectionListener):

    name = "attribute_manager_page"

    def __init__(self, cardContainer):
	Page.__init__(self)
	self.cardContainer = cardContainer
        self.tabs = []
	self.tabbedPane = JTabbedPane( stateChanged=self.clear)
	self.add( self.tabbedPane )
	self.addTabs(self.tabbedPane, defaultAttributeConfiguration)

	# these must be set before add/removeNode() is called.
        self.tree = None
        self.attributesNode = None
        self.attributesNodePath = None

        self.sequencePage = SequencePage(self)
	self.tabbedPane.add( self.sequencePage, self.sequencePage.name )
        self.tabs.append(self.sequencePage)



    def configure(self, node, path, tree):
        self.tree = tree
        self.attributesNode = node
        self.attributesNodePath = path
        self.tree.addTreeSelectionListener( self )



    def valueChanged(self, event):

	""" Brings the attributePage to the front of the
	cardContainer if this node or one of it's cheildren was selected."""
        path = event.newLeadSelectionPath
        if path:
            lastNode = event.newLeadSelectionPath.lastPathComponent
            for n in self.attributesNode.depthFirstEnumeration(): 
                if n == lastNode:
                    self.cardContainer.show( AttributePage.name )
                    break


    def toFront(self, tab):
        self.tabbedPane.setSelectedComponent( tab )



    def addTabs(self, tabbedPane, configuration):

	""" Adds the attribute tabs from the configuration
	string. On screen structure is:

	    tab -> group -> subGroup -> attribute widget

	"""
	
	# add All AttributeSubPages (add user attributes to features, snps)
        for tabData in configuration:
            tab = AttributeSubPage( self, tabData )
            tabbedPane.add( tab, tab.name )
            self.tabs.append( tab )
            
    

    def updateQuery(self, query):
        for tab in self.tabs:
            tab.updateQuery( query )



    def updatePage(self, query):
        for tab in self.tabs:
            tab.updatePage( query )



    def clear( self, event=None):
        for tab in self.tabs:
            tab.clear()

        

    def addNode(self, node):
	self.tree.model.insertNodeInto( node, self.attributesNode, self.attributesNode.childCount)
	self.refreshView() # opens tree attributeNode if it was closed

    def removeNode(self, node):
	self.tree.model.removeNodeFromParent( node)

    def refreshView(self):
	self.cardContainer.show( self.name )
	self.tree.expandPath( self.attributesNodePath )
	self.tree.repaint()
	
    def htmlSummary(self):
	return "<html><b>Attributes</b></html>"

    

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



    def clear( self ):
        self.chr.setText("")
        self.start.setText("")
        self.end.setText("")
        self.strand.setText("")

        

    def updateQuery(self, query):
	chr = self.chr.getText()
	if chr and chr!="": query.addFilter( BasicFilter("chr_name","=",chr) )

	start = self.start.getText()
	if start and start!="": query.addFilter( BasicFilter("gene_chrom_start",">=",start) )

	end = self.end.getText()
	if end and end!="": query.addFilter( BasicFilter("gene_chrom_end","<=",end) )

        strand = self.strand.getText()
	if strand and strand!="": query.addFilter( BasicFilter("chrom_strand","<=",strand) )

	
    def updatePage(self, query):
	for f in query.filters:
	    if isinstance(f, BasicFilter):
		if f.name=="chr_name":
		    self.chr.setText( f.value )
		elif f.name=="gene_chrom_start":
		    self.start.setText( f.value )
		elif f.name=="gene_chrom_end":
		    self.end.setText( f.value )
		elif f.name=="chrom_strand":
		    self.strand.setText( f.value )		    





    
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

	self.formatPage = FormatPage()
	
        filtersNode = QueryTreeNode( tree, self.rootNode, 0, self.cardContainer,
				     FilterPage())
        outputNode = QueryTreeNode( tree, self.rootNode, 1, self.cardContainer,
				    OutputPage())

        datasetPage = DatasetPage()
        datasetNode = QueryTreeNode( tree, filtersNode, 0, self.cardContainer,
				     datasetPage)
        regionNode = QueryTreeNode( tree, filtersNode, 1, self.cardContainer,
				    RegionPage())
        attributePage = AttributePage(self.cardContainer)
        attributesNode = QueryTreeNode( tree, outputNode, 0, self.cardContainer,
					attributePage)
        formatNode = QueryTreeNode( tree, outputNode, 1, self.cardContainer,
				    self.formatPage)
        
        # expand branches in tree
	path = TreePath(self.rootNode).pathByAddingChild( filtersNode )
	tree.expandPath( path )
        path = TreePath(self.rootNode).pathByAddingChild( outputNode )
	tree.expandPath( path )
        path = path.pathByAddingChild( attributesNode )
	tree.expandPath( path )

        attributePage.configure(attributesNode, path, tree)

        datasetPage.configure( DATASETS )

        self.layout = BorderLayout()
	scrollPane = JScrollPane(tree)
	scrollPane.setPreferredSize( Dimension(350,300) )
        self.add(  scrollPane, BorderLayout.WEST )
	self.add( JScrollPane( self.cardContainer ) , BorderLayout.CENTER )



    def addPage( self, page ):
	self.cardContainer.add( page, page.name )



    def showPage( self, pageName ):
	self.cardContainer.show( pageName )



    def clear(self):
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.targetComponent.clear()


                
    def updateQuery(self, query):
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.targetComponent.updateQuery( query )

                
                
    def updatePage(self, query):
        for node in self.rootNode.depthFirstEnumeration():
            if isinstance( node, QueryTreeNode ):
                node.targetComponent.updatePage( query )



    def getFormatSpec(self):
        return self.formatPage.getFormatSpec()



DATASETS = [
    Dataset("Human Ensembl Genes"
            , ["hsapiens_ensemblgene", "hsapiens_ensembltranscript"]
            , ["gene_id", "transcript_id"] )

    ]



class MartGUIApplication(JFrame):

    def __init__(self, closeOperation=JFrame.DISPOSE_ON_CLOSE):
        JFrame.__init__(self, "MartExplorer", defaultCloseOperation=closeOperation
                        , size=APPLICATION_SIZE)

	self.cursorUtil = CursorUtil()
	self.chooser = JFileChooser( System.getProperty("user.home") )

        self.editor = QueryEditor()
	self.databasePage = DatabasePage()
	self.resultsPage = ResultsPage()

	self.editor.addPage( self.databasePage )
	self.editor.addPage( self.resultsPage )

        self.buildGUI()
        self.visible=1
	self.editor.showPage( self.databasePage.name )


    def buildGUI(self):
        self.JMenuBar = self.createMenuBar()

        panel = JPanel()
        panel.layout = BoxLayout( panel, BoxLayout.Y_AXIS )
        panel.add( self.createToolBar() )
        panel.add( self.editor )
        self.contentPane.add( panel )


    def createMenuBar(self):

        fileMenu = JMenu("File")
	fileMenu.add( JMenuItem("Configure Database"
				,toolTipText="Database paramaters"
				,actionPerformed=self.doDatabaseSettings) )
	fileMenu.add( JMenuItem("Save Results"
                                 ,toolTipText="Saves results to file"
                                 ,actionPerformed=self.doSaveResults) )
	fileMenu.add( JMenuItem("Save Results As"
				,toolTipText="Saves results to file"
				,actionPerformed=self.doSaveResultsAs) )
        fileMenu.add( JMenuItem("Exit"
                                ,toolTipText="Quits Application"
                                ,actionPerformed=self.doExit) )

        queryMenu = JMenu("Query")
        queryMenu.add( JMenuItem("Insert test query"
                                ,toolTipText="Inserts a test query into the application"
                                ,actionPerformed=self.doInsertKakaQuery) )
        queryMenu.add( JMenuItem("View Results"
                                 ,toolTipText="Opens results in viewer"
                                 ,actionPerformed=self.doViewResults) )
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

    def doDatabaseSettings(self, event=None):
	self.editor.showPage( self.databasePage.name )


    def doViewResults(self, event=None):
        self.viewResults()

    def doSaveResults(self, event=None):
        # open popup window window asking for filename
	if self.chooser.selectedFile:
	    # execute query, piping results to file.
	    os = FileOutputStream( self.chooser.selectedFile )
	    thread.start_new_thread( self.executeQuery, (self, os) )
	else:
	    self.doSaveResultsAs()

	    

    def doSaveResultsAs(self, event=None):
        # open popup window window asking for filename
	if JFileChooser.APPROVE_OPTION == self.chooser.showSaveDialog(self):
	    # execute query, piping results to file.
	    os = FileOutputStream( self.chooser.selectedFile )
	    thread.start_new_thread( self.executeQuery, (self, os) )


    def doExit(self, event=None):
        System.exit(0)


    def doInsertKakaQuery(self, event=None):
        self.databasePage.host.setText( DEFAULT_HOST )
        self.databasePage.user.setText( DEFAULT_USER )
        self.databasePage.database.setText( DEFAULT_DATABASE )
        self.databasePage.port.setText( DEFAULT_PORT )

        self.editor.formatPage.setFormatSpec( FormatSpec(FormatSpec.TABULATED, ",") )

        q = Query()
        q.starBases = DATASETS[0].starNames
        q.primaryKeys = DATASETS[0].primaryKeys
	q.addFilter( BasicFilter("chr_name", "=", "22") )
	#q.addFilter( BasicFilter("gene_chrom_start", "=", "1") )
	q.addFilter( BasicFilter("gene_chrom_end", "=", "14000000") )
	#q.addFilter( BasicFilter("chrom_strand", "=", "1") )


        # load test data file via classpath; this works from a
        # deployed jar and from normal directory in developement
        # situations.

        url = ClassLoader.getSystemResource("data/gene_stable_id.test")
        #q.addFilter( IDListFilter("gene_stable_id", url ) )
        #q.addFilter( IDListFilter("gene_stable_id",
        #                          array( ("ENSG00000177741"), String) ) )
        q.addAttribute( FieldAttribute("gene_stable_id") )
        q.addAttribute( FieldAttribute("chr_name") )
        q.addAttribute( FieldAttribute("gene_chrom_start") )
        q.addAttribute( FieldAttribute("gene_chrom_end") )

        #query.addFilter( IDListFilter("gene_stable_id", File( STABLE_ID_FILE).toURL() ) )
        #q.resultTarget = ResultFile( "/tmp/kaka.txt", SeparatedValueFormatter("\t") )
        # TODO need a result window
        #q.resultTarget = ResultWindow( "Results_1", SeparatedValueFormatter ("\t") )

        #sd = SequenceDescription( SequenceDescription.TRANSCRIPTCODING  )
        #q.sequenceDescription = sd 
        self.editor.updatePage( q )




    def viewResults( self ):
	self.editor.showPage( ResultsPage.name )
        # execute query in new thread and pipe results to window.
        self.resultsPage.clear()
        os = self.resultsPage.getOutputStream()
	thread.start_new_thread( self.executeQuery, (self, os) )



    def executeQuery( self, dummy=None, outputStream=None ):


        host = self.databasePage.getHost()
        port = self.databasePage.getPort()
        user = self.databasePage.getUser()
        password = self.databasePage.getPassword()
        database = self.databasePage.getDatabase()

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
	    outputStream.close()

	    # scroll to top of results
	    self.resultsPage.scrollToTop()

        except (InvalidQueryException), ex:
            message = ex.message
            if len(message)>80:
                message = message[:80] + "..."
            JOptionPane.showMessageDialog( self,
                                           "Failed to execute query: "
                                           + message,
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE)
	    ex.printStackTrace()

	self.cursorUtil.stopWaitCursor(self)



    def doAbout(self, event=None):
        JOptionPane.showMessageDialog(self,
                                      "MartExplorer (development version). Copyright EBI and GRL.",
                                      "About MartExplorer",
                                      JOptionPane.OK_OPTION )


    def doClear(self, event=None):
        self.editor.clear()



def main(args, quitOnExit):
    # default logging level, needed by engine
    BasicConfigurator.configure()
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



