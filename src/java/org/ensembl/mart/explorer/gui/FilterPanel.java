/* Generated by Together */

package org.ensembl.mart.explorer.gui;

import javax.swing.*;
import org.ensembl.mart.explorer.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.apache.log4j.*;
import java.awt.FlowLayout;


/** Input options for specifying region to search. */
public class FilterPanel extends JPanel 
  implements QueryInputPage {

  private Logger logger = Logger.getLogger( FilterPanel.class.getName() );

  /** Creates new form RegionTab */
  public FilterPanel() {
    initGUI();
  }

  /** This method is called from within the constructor to initialize the form. */
  private void initGUI() {
    chromosomePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    chromosomePanel.add(chromosomeButton);
    chromosomePanel.add(chromosome);
    chromosomeButton.setText("Chromosome");
    chromosomeButton.setToolTipText("Limit to this chromosome");
    chromosome.setEditable(true);
    entireGenomeButton.setText("Entire Genome");
    entireGenomeButton.setToolTipText("Get data for whole genome");
    entireGenomeButton.setSelected(true);
    entireGenomePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    entireGenomePanel.add(entireGenomeButton);
    jLabel3.setText("Focus");
    jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    jPanel7.add(jLabel3);
    jPanel7.add(focus);
    focus.setEditable(true);
    stableIDs.setText("");
    jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    jPanel6.add(stableIDURLButton);
    jPanel6.add(stableIDURL);
    stableIDURL.setEditable(true);
    jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    jPanel3.add(stableIDFileButton);
    jPanel3.add(stableIDFile);
    stableIDFile.setEditable(true);
    stableIDField.setEditable(true);
    jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    jPanel1.add(jLabel1);
    jPanel1.add(stableIDField);
    jLabel1.setText("Type of Stable ID");
    jLabel1.setToolTipText("");
    stableIDPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    stableIDPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Stable IDs", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 11), new java.awt.Color(60, 60, 60)));
    stableIDPanel.add(stableIDButton);
    stableIDPanel.add(jPanel2);
    stableIDButton.setText("Stable IDs");
    stableIDButton.setToolTipText("Limit to this chromosome");
    setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
    add(regionPanel);
    add(speciesFocusPanel);
    add(stableIDPanel);
    jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));
    jPanel2.add(jPanel1);
    jPanel2.add(jPanel3);
    jPanel2.add(jPanel6);
    jPanel2.add(jPanel4);
    jPanel4.add(stableIDStringButton);
    jPanel4.add(new JScrollPane( stableIDs ));
    stableIDStringButton.setText("jRadioButton1");
    stableIDStringButton.setText("IDs");
    stableIDURLButton.setText("jRadioButton1");
    stableIDURLButton.setText("URL");
    stableIDFileButton.setText("jRadioButton1");
    stableIDFileButton.setText("File");

    stableIDGroup.add( stableIDFileButton );
    stableIDGroup.add( stableIDURLButton );
    stableIDGroup.add( stableIDStringButton );
    stableIDs.setText("");
    stableIDs.setRows(10);
    stableIDs.setColumns(30);
    speciesFocusPanel.setLayout(
        new javax.swing.BoxLayout(speciesFocusPanel, javax.swing.BoxLayout.Y_AXIS));
    speciesFocusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Species and Focus", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 11), new java.awt.Color(60, 60, 60)));
    speciesFocusPanel.add(jPanel5);
    speciesFocusPanel.add(jPanel7);
    jLabel2.setText("Species");
    jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
    jPanel5.add(jLabel2);
    jPanel5.add(species);
    species.setEditable(true);
    species.setMinimumSize(new java.awt.Dimension(250,21));
    species.setPreferredSize(new java.awt.Dimension(250,21));
    regionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Region", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 11), new java.awt.Color(60, 60, 60)));
    regionPanel.add(entireGenomePanel);
    regionPanel.add(chromosomePanel);
  }


  private void updateQueryRegion(Query query) {
    if (entireGenomeButton.isSelected()) {
      // do nothing in this case
    }
    else if (chromosomeButton.isSelected()) {
      query.addFilter(new BasicFilter("chromosome_id", "=", Tool.selected(chromosome)));
    }

  }

  private void updateQueryStableIDs(Query query) throws InvalidQueryException {
    if ( stableIDButton.isSelected() ) {

      String fieldType = (String)stableIDField.getSelectedItem();

      if ( stableIDStringButton.isSelected() ) {

        StringTokenizer tokens = new StringTokenizer( stableIDs.getText() );
        String[] identifiers = new String[ tokens.countTokens() ];
        int i = 0;
        while( tokens.hasMoreTokens() )
          identifiers[ i++ ] = tokens.nextToken();
        query.addFilter( new IDListFilter( fieldType, identifiers) );
      }

      else if ( stableIDFileButton.isSelected() ) {

        String fileName = (String)stableIDFile.getSelectedItem();
        try {
          query.addFilter( new IDListFilter( fieldType, new File( fileName ) ) );
        } catch ( IOException e ) {
          throw new InvalidQueryException( "Failed to open id file: " + fileName, e);
        }

      }
      else if ( stableIDURLButton.isSelected() ) {

        String url = (String)stableIDURL.getSelectedItem();
        try {
          query.addFilter( new IDListFilter( fieldType, new URL( url ) ) );
        } catch( Exception e ) {
          throw new InvalidQueryException( "Failed to open url: " + url, e);
        }
      }
    }
  }

  public void updateQuery(Query query) throws InvalidQueryException {
    Object s = species.getSelectedItem();
    if ( s==null ) throw new InvalidQueryException("Species must be set");
    query.setSpecies( s.toString() );
    Object f =focus.getSelectedItem();
		if ( f==null ) throw new InvalidQueryException("Focus must be set");
    query.setFocus( f.toString() );
    updateQueryRegion( query );
    updateQueryStableIDs( query );
  }

  public void updatePage(Query query) {

		Tool.prepend( query.getSpecies(), species );
    Tool.prepend( query.getFocus(), focus );

    // default behaviour, overriden by settings below
    entireGenomeButton.setSelected(true);

    Iterator iter = query.getFilters().iterator();
    while (iter.hasNext()) {

      Object o = iter.next();
      
      logger.debug("loading data from filter : " + o);

      if (o instanceof BasicFilter) {
        BasicFilter bf = (BasicFilter)o;
        if ("chromosome_id".equals(bf.getType()) && "=".equals(bf.getCondition())) {
          Tool.prepend( bf.getValue(), chromosome);
          chromosomeButton.setSelected(true);
        }
      }

      if (o instanceof IDListFilter ) {
        
        IDListFilter f = (IDListFilter)o;
        Tool.prepend( f.getType(), stableIDField );
        stableIDButton.setSelected( true );
        switch( f.getMode() ) {
          
        case IDListFilter.STRING_MODE:
          String[] ids = f.getIdentifiers();
          StringBuffer buf = new StringBuffer();
          for( int i=0; i< ids.length; ++i )
            buf.append( ids[i] ).append("\n");
          stableIDs.setText( buf.toString() );
          stableIDStringButton.setSelected( true );
          break;
          
        case IDListFilter.FILE_MODE:
          Tool.prepend(f.getFile().toString(), stableIDFile );
          stableIDFileButton.setSelected( true );
          break;
          
        case IDListFilter.URL_MODE:
          Tool.prepend(f.getUrl().toString(), stableIDURL );
          stableIDURLButton.setSelected( true );
          break;
          
        default:
          logger.warn( "unknown IDListFilter: "+ f);
        }
      }
      
    }
  }

  /**
   * Removes all selected values. 
   */
  public void clear(){
    Tool.clear( species );
    Tool.clear( focus );
		entireGenomeButton.setSelected( true );
    Tool.clear( chromosome );
    Tool.clear( stableIDField );
    stableIDs.setText( "" );
    stableIDStringButton.setSelected( false );
    Tool.clear( stableIDFile );
    stableIDFileButton.setSelected( false );
    Tool.clear( stableIDURL );
    stableIDURLButton.setSelected( false );

  }

  private JPanel chromosomePanel = new JPanel();
  private JRadioButton chromosomeButton = new JRadioButton();
  private JPanel stableIDPanel = new JPanel();
  private JCheckBox stableIDButton = new JCheckBox();
  private JPanel entireGenomePanel = new JPanel();
  private JRadioButton entireGenomeButton = new JRadioButton();
  private JComboBox chromosome = new JComboBox();
  private JComboBox stableIDField = new JComboBox();
  private ButtonGroup regionGroup = new ButtonGroup();
  private JPanel jPanel1 = new JPanel();
  private JPanel jPanel2 = new JPanel();
  private JLabel jLabel1 = new JLabel();
  private JComboBox stableIDFile = new JComboBox();
  private JPanel jPanel3 = new JPanel();
  private JComboBox stableIDURL = new JComboBox();
  private JPanel jPanel6 = new JPanel();
  private JPanel jPanel4 = new JPanel();
  private JRadioButton stableIDStringButton = new JRadioButton();
  private JRadioButton stableIDURLButton = new JRadioButton();
  private JRadioButton stableIDFileButton = new JRadioButton();
  private ButtonGroup stableIDGroup = new ButtonGroup();
  private JTextArea stableIDs = new JTextArea();
  private JPanel speciesFocusPanel = new JPanel();
  private JPanel jPanel5 = new JPanel();
  private JLabel jLabel2 = new JLabel();
  private JComboBox species = new JComboBox();
  private JPanel jPanel7 = new JPanel();
  private JLabel jLabel3 = new JLabel();
  private JComboBox focus = new JComboBox();
  private JPanel regionPanel = new JPanel();
}
