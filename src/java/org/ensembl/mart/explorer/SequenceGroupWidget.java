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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.ensembl.mart.guiutils.QuickFrame;
import org.ensembl.mart.lib.InvalidQueryException;
import org.ensembl.mart.lib.Query;
import org.ensembl.mart.lib.SequenceDescription;
import org.ensembl.mart.lib.config.DSAttributeGroup;
import org.ensembl.mart.util.LoggingUtil;

/**
 * Widget for viewing the current sequence attribute on a query and enabling the user to
 * add, remove or select a different one. This class implements the Model View Controller Design pattern where 
 * the query is the model, actionPerformed(...) handles user control actions and 
 * sequenceDescriptionChanged(...) updates the view when the model changes.
 * 
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 *
  * TODO Modify SequenceDescription.left/right to use -1 for unset, then update this to reflect that.
 */
public class SequenceGroupWidget
  extends GroupWidget
  implements ActionListener, TreeSelectionListener {

  private static final Logger logger =
    Logger.getLogger(SequenceGroupWidget.class.getName());

  private class LabelledTextField extends JTextField {
    private LabelledTextField(String initialValue) {
      super(initialValue);
      Dimension d = new Dimension(100, 24);
      setPreferredSize(d);
      setMaximumSize(d);
    }

    public int getTextAsInt() {
      return Integer.parseInt(getText());
    }
  }

  private final int IMAGE_WIDTH = 248;

  private final int IMAGE_HEIGHT = 69;

  private final int UNSUPPORTED = -100;
  private final int NONE = -101;

  private Feedback feedback = new Feedback(this);

  private LabelledTextField flank5 = new LabelledTextField("1000");
  private LabelledTextField flank3 = new LabelledTextField("1000");

  private JButton clearButton = new JButton("Clear");

  private JRadioButton transcript = new JRadioButton("Transcripts/proteins");

  private JRadioButton gene = new JRadioButton("Genes");
  
  private JRadioButton none = new JRadioButton();

  private JRadioButton includeGeneSequence =
    new JRadioButton("Gene sequence only");

  private JRadioButton includeGeneSequence_5_3 =
    new JRadioButton("Gene plus 5' and 3' flanks");

  private JRadioButton includeGeneSequence_5 =
    new JRadioButton("Gene plus 5' flank");

  private JRadioButton includeGeneSequence_3 =
    new JRadioButton("Gene plus 3' flank");

  private JRadioButton includeUpstream = new JRadioButton("5' upstream only");

  private JRadioButton includeDownStream =
    new JRadioButton("3' downstream only");

  private JRadioButton includeUpStreamUTROnly = new JRadioButton("5' UTR only");

  private JRadioButton includeUpStreamAndUTR =
    new JRadioButton("5' upstream and UTR");

  private JRadioButton includeDownStreamUTROnly =
    new JRadioButton("3' UTR only");

  private JRadioButton includeDownStreamAndUTR =
    new JRadioButton("3' UTR and downstream");

  private JRadioButton includeExonSequence = new JRadioButton("Exon sequences");

  private JRadioButton includecDNASequence =
    new JRadioButton("cDNA sequence only");

  private JRadioButton includeCodingSequence =
    new JRadioButton("Coding sequence only");

  private JRadioButton includeExonsPlus5Flanks =
    new JRadioButton("Exons plus 5' flanks");

  private JRadioButton includeExonsPlus3Flanks =
    new JRadioButton("Exons plus 3' flanks");

  private JRadioButton includeExonsPlus5And3Flanks =
    new JRadioButton("Exons plus 5' and 3' flanks");

  private JRadioButton includePeptide = new JRadioButton("Peptide");

  private JRadioButton includeNone = new JRadioButton();

  private JRadioButton[] typeButtons = { transcript, gene,  none };

  private JRadioButton[] includeButtons =
    {
      includeGeneSequence,
      includeGeneSequence_5_3,
      includeGeneSequence_5,
      includeUpstream,
      includeUpStreamUTROnly,
      includeUpStreamAndUTR,
      includeGeneSequence_3,
      includeDownStream,
      includeDownStreamUTROnly,
      includeDownStreamAndUTR,
      includeExonSequence,
      includecDNASequence,
      includeCodingSequence,
      includePeptide,
      includeExonsPlus5And3Flanks,
      includeExonsPlus3Flanks,
      includeExonsPlus5Flanks };

  private JComponent[] leftColumn =
    {
      includeGeneSequence,
      includeGeneSequence_5_3,
      includeGeneSequence_5,
      includeUpstream,
      includeGeneSequence_3,
      includeDownStream,
      includeExonSequence,
      includeExonsPlus5And3Flanks,
      includeExonsPlus3Flanks,
      includeExonsPlus5Flanks };

  private JComponent[] rightColumn =
    {
      includeUpStreamUTROnly,
      includeUpStreamAndUTR,
      includeDownStreamUTROnly,
      includeDownStreamAndUTR,
      includecDNASequence,
      includeCodingSequence,
      includePeptide };

  private JRadioButton[] geneButtons =
    {
      includeGeneSequence,
      includeGeneSequence_5_3,
      includeGeneSequence_5,
      includeGeneSequence_3,
      includeUpstream,
      includeDownStream,
      includeExonSequence,
      includeExonsPlus5And3Flanks,
      includeExonsPlus3Flanks,
      includeExonsPlus5Flanks };

  private JLabel schematicSequenceImageHolder = new JLabel();

  private ImageIcon blankIcon;

  /**
  * @param name
  * @param query
  * @param tree
  */
  public SequenceGroupWidget(
    String name,
    Query query,
    QueryTreeView tree,
    DSAttributeGroup attributeGroup) {

    super(name, query, tree);
    if (tree != null)
      tree.addTreeSelectionListener(this);

    buildGUI();
    sequenceDescriptionChanged(query, null, query.getSequenceDescription());
  }

  /**
   * Updates the view (GUI state) to represent the sequence description.
   * @param description
   */
  public void sequenceDescriptionChanged(
    Query sourceQuery,
    SequenceDescription oldSequenceDescription,
    SequenceDescription sd) {

    logger.fine("New sd: " + sd);

    if (sd == null) {

      includeNone.setSelected(true);
      setButtonsEnabled(includeButtons, false);
      schematicSequenceImageHolder.setIcon(blankIcon);

      flank5.setEnabled(false);
      flank3.setEnabled(false);

    } else {

      int f5 = sd.getLeftFlank();
      int f3 = sd.getRightFlank();

      switch (sd.getType()) {

        case SequenceDescription.TRANSCRIPTCODING :
          transcript.setSelected(true);
          includeCodingSequence.setSelected(true);
          schematicSequenceImageHolder.setIcon(
            loadIcon("data/image/gene_schematic_coding.gif"));
          break;

        case SequenceDescription.TRANSCRIPTPEPTIDE :
          transcript.setSelected(true);
          includePeptide.setSelected(true);
          schematicSequenceImageHolder.setIcon(
            loadIcon("data/image/gene_schematic_coding.gif"));
          break;

        case SequenceDescription.TRANSCRIPTCDNA :
          transcript.setSelected(true);
          includecDNASequence.setSelected(true);
          schematicSequenceImageHolder.setIcon(
            loadIcon("data/image/gene_schematic_cdna.gif"));
          break;

        case SequenceDescription.TRANSCRIPTEXONS :
          transcript.setSelected(true);
          if (f5 == 0 && f3 == 0) {
            includeExonSequence.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_exons.gif"));
          } else if (f5 != 0 && f3 == 0) {
            includeExonsPlus5Flanks.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_exons_5.gif"));
            flank5.setText(Integer.toString(f5));
          } else if (f5 == 0 && f3 != 0) {
            includeExonsPlus3Flanks.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_exons_3.gif"));
            flank3.setText(Integer.toString(f3));
          } else if (f5 != 0 && f3 != 0) {
            includeExonsPlus5And3Flanks.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_exons_5_3.gif"));
            flank5.setText(Integer.toString(f5));
            flank3.setText(Integer.toString(f3));
          }
          break;

        case SequenceDescription.TRANSCRIPTEXONINTRON :
          transcript.setSelected(true);
          if (f5 == 0 && f3 == 0) {
            includeGeneSequence.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_gene_only.gif"));
          } else if (f5 != 0 && f3 == 0) {
            includeGeneSequence_5.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_gene_5.gif"));
            flank5.setText(Integer.toString(f5));
          } else if (f5 == 0 && f3 != 0) {
            includeGeneSequence_3.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_gene_3.gif"));
            flank3.setText(Integer.toString(f3));
          } else if (f5 != 0 && f3 != 0) {
            includeGeneSequence_5_3.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_gene_5_3.gif"));
            flank5.setText(Integer.toString(f5));
            flank3.setText(Integer.toString(f3));
          }
          break;

        case SequenceDescription.TRANSCRIPTFLANKS :
          transcript.setSelected(true);
          if (f5 != 0 && f3 == 0) {
            includeUpstream.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_5_only.gif"));
            flank5.setText(Integer.toString(f5));
          } else if (f5 == 0 && f3 != 0) {
            includeDownStream.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_3_only.gif"));
            flank3.setText(Integer.toString(f3));
          } else {
            throw new RuntimeException(
              "Unsupported sequence description state: " + sd);
          }
          break;

        case SequenceDescription.GENEEXONINTRON :
          gene.setSelected(true);
          if (f5 == 0 && f3 == 0) {
            includeGeneSequence.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_gene_only.gif"));
          } else if (f5 != 0 && f3 == 0) {
            includeGeneSequence_5.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_gene_5.gif"));
            flank5.setText(Integer.toString(f5));
          } else if (f5 == 0 && f3 != 0) {
            includeGeneSequence_3.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_gene_3.gif"));
            flank3.setText(Integer.toString(f3));
          } else if (f5 != 0 && f3 != 0) {
            includeGeneSequence_5_3.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_gene_5_3.gif"));
            flank5.setText(Integer.toString(f5));
            flank3.setText(Integer.toString(f3));
          }
          break;

        case SequenceDescription.GENEEXONS :
          gene.setSelected(true);
          if (f5 == 0 && f3 == 0) {
            includeExonSequence.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_exons.gif"));
          } else if (f5 != 0 && f3 == 0) {
            includeExonsPlus5Flanks.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_exons_5.gif"));
            flank5.setText(Integer.toString(f5));
          } else if (f5 == 0 && f3 != 0) {
            includeExonsPlus3Flanks.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_exons_3.gif"));
            flank3.setText(Integer.toString(f3));
          } else if (f5 != 0 && f3 != 0) {
            includeExonsPlus5And3Flanks.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_exons_5_3.gif"));
            flank5.setText(Integer.toString(f5));
            flank3.setText(Integer.toString(f3));
          }
          break;

        case SequenceDescription.GENEFLANKS :
          gene.setSelected(true);
          if (f5 != 0 && f3 == 0) {
            includeUpstream.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_5_only.gif"));
            flank5.setText(Integer.toString(f5));
          } else if (f5 == 0 && f3 != 0) {
            includeDownStream.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_extent_3_only.gif"));
            flank3.setText(Integer.toString(f3));
          } else {
            throw new RuntimeException(
              "Unsupported sequence description state: " + sd);
          }
          break;

        case SequenceDescription.DOWNSTREAMUTR :
          transcript.setSelected(true);
          if (f5 == 0 && f3 == 0) {
            includeDownStreamUTROnly.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_downstream_utr.gif"));
          } else if (f5 == 0 && f3 != 0) {
            includeDownStreamAndUTR.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_downstream_utr_3.gif"));
          } else {
            throw new RuntimeException(
              "Unsupported sequence description state: " + sd);
          }
          break;

        case SequenceDescription.UPSTREAMUTR :
          transcript.setSelected(true);
          if (f5 == 0 && f3 == 0) {
            includeUpStreamUTROnly.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_upstream_utr.gif"));
          } else if (f5 != 0 && f3 == 0) {
            includeUpStreamAndUTR.setSelected(true);
            schematicSequenceImageHolder.setIcon(
              loadIcon("data/image/gene_schematic_upstream_utr_5.gif"));
          } else {
            throw new RuntimeException(
              "Unsupported sequence description state: " + sd);
          }

          break;

        default :
          throw new RuntimeException("Unsupported sequence type: " + sd);

      }
    }

    // re-enable buttons if necessary
    if (transcript.isSelected()) setButtonsEnabled(includeButtons, true);
    else if  (gene.isSelected()) setButtonsEnabled(geneButtons, true);
  }

  private void buildGUI() {

    gene.setToolTipText(
      " Transcript information ignored (one output per gene)");

    Box b = Box.createVerticalBox();

    b.add(addAll(Box.createHorizontalBox(), new JComponent[]{transcript,gene,clearButton}, true));

    b.add(
      addAll(
        Box.createHorizontalBox(),
        new JComponent[] { schematicSequenceImageHolder },
        true));

    Box columns = Box.createHorizontalBox();
    columns.add(addAll(Box.createVerticalBox(), leftColumn, false));
    columns.add(addAll(Box.createVerticalBox(), rightColumn, true));
    columns.add(Box.createHorizontalGlue());
    b.add(columns);

    b.add(
      addAll(
        Box.createHorizontalBox(),
        new Component[] {
          new JLabel("5' Flank (bp)"),
          flank5,
          Box.createHorizontalStrut(50),
          new JLabel("3' Flank (bp)"),
          flank3 },
        false));

    add(b);

    BufferedImage blank =
      new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = blank.createGraphics();
    g.setBackground(Color.WHITE);
    g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    blankIcon = new ImageIcon(blank);

    none.setSelected(true);
    ButtonGroup bg = new ButtonGroup();
    for (int i = 0; i < typeButtons.length; i++) {
      bg.add(typeButtons[i]);
      typeButtons[i].addActionListener(this);
    }

    clearButton.addActionListener(this);
    clearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        none.doClick();
      }
    });

    bg = new ButtonGroup();
    for (int i = 0; i < includeButtons.length; i++) {
      bg.add(includeButtons[i]);
      includeButtons[i].addActionListener(this);
    }
    bg.add(includeNone);

    flank3.addActionListener(this);
    flank5.addActionListener(this);

  }

  private Box addAll(
    Box container,
    Component[] components,
    boolean addGlueAtEnd) {
    for (int i = 0; i < components.length; i++)
      container.add(components[i]);
    if (addGlueAtEnd)
      container.add(Box.createGlue());
    return container;
  }

  private ImageIcon loadIcon(String filepath) {
    ImageIcon icon = null;
    try {
      BufferedImage testImage = ImageIO.read(new File(filepath));
      icon = new ImageIcon(testImage);
    } catch (IOException e) {
      System.err.println("Problem loading file: " + filepath);
      e.printStackTrace();
    }

    return icon;
  }

  /**
   * Runs a graphical test of this widget. 
   * @param args ignored
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    LoggingUtil.setAllRootHandlerLevelsToFinest();
    logger.setLevel(Level.ALL);

    DSAttributeGroup g = new DSAttributeGroup("sequences");
    Query q = new Query();
    //q.addQueryChangeListener(new DebugQueryListener(System.out));

    SequenceGroupWidget w = new SequenceGroupWidget("seq widget", q, null, g);

    new QuickFrame("Sequence Attribute Widget test", w);
  }

  /**
   * Updates the query in response to a user action (control part of model-control-view pattern).
   * Adds / removes a filtter to/from query as necessary.
   */
  public void actionPerformed(ActionEvent e) {

    Object src = e.getSource();

    flank5.setEnabled(false);
    flank3.setEnabled(false);

    if (src == clearButton) {

      changeQuery(NONE, 0, 0);

    } else if (transcript.isSelected()) {

      if (src == transcript) {

        changeQuery(NONE, 0, 0);
        enableTranscriptButtons();

      } else if (includeGeneSequence.isSelected()) {

        changeQuery(SequenceDescription.TRANSCRIPTEXONINTRON, 0, 0);

      } else if (includeGeneSequence_5_3.isSelected()) {

        changeQuery(
          SequenceDescription.TRANSCRIPTEXONINTRON,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());
        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeGeneSequence_5.isSelected()) {

        changeQuery(
          SequenceDescription.TRANSCRIPTEXONINTRON,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      } else if (includeUpstream.isSelected()) {

        changeQuery(
          SequenceDescription.TRANSCRIPTFLANKS,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      } else if (includeUpStreamUTROnly.isSelected()) {

        changeQuery(SequenceDescription.UPSTREAMUTR, 0, 0);

      } else if (includeUpStreamAndUTR.isSelected()) {

        changeQuery(SequenceDescription.UPSTREAMUTR, flank5.getTextAsInt(), 0);
        flank5.setEnabled(true);

      } else if (includeGeneSequence_3.isSelected()) {

        changeQuery(
          SequenceDescription.TRANSCRIPTEXONINTRON,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeDownStream.isSelected()) {

        changeQuery(
          SequenceDescription.TRANSCRIPTFLANKS,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeDownStreamUTROnly.isSelected()) {

        changeQuery(SequenceDescription.DOWNSTREAMUTR, 0, 0);

      } else if (includeDownStreamAndUTR.isSelected()) {

        changeQuery(
          SequenceDescription.DOWNSTREAMUTR,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeExonSequence.isSelected()) {

        changeQuery(SequenceDescription.TRANSCRIPTEXONS, 0, 0);

      } else if (includecDNASequence.isSelected()) {

        changeQuery(SequenceDescription.TRANSCRIPTCDNA, 0, 0);

      } else if (includeCodingSequence.isSelected()) {

        changeQuery(SequenceDescription.TRANSCRIPTCODING, 0, 0);

      } else if (includePeptide.isSelected()) {

        changeQuery(SequenceDescription.TRANSCRIPTPEPTIDE, 0, 0);

      } else if (includeExonsPlus5And3Flanks.isSelected()) {

        changeQuery(
          SequenceDescription.TRANSCRIPTEXONS,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());
        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeExonsPlus3Flanks.isSelected()) {

        changeQuery(
        SequenceDescription.TRANSCRIPTEXONS, 0, flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeExonsPlus5Flanks.isSelected()) {

        changeQuery(
        SequenceDescription.TRANSCRIPTEXONS, flank5.getTextAsInt(), 0);
        flank5.setEnabled(true);

      }

    } else if (gene.isSelected()) {

      if (src == gene) {

        changeQuery(NONE, 0, 0);
        enableGeneButtons();

      } else if (includeGeneSequence.isSelected()) {

        changeQuery(SequenceDescription.GENEEXONINTRON, 0, 0);

      } else if (includeGeneSequence_5_3.isSelected()) {

        changeQuery(
          SequenceDescription.GENEEXONINTRON,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());

        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeGeneSequence_5.isSelected()) {

        changeQuery(
          SequenceDescription.GENEEXONINTRON,
          flank5.getTextAsInt(),
          0);
        flank5.setEnabled(true);

      } else if (includeGeneSequence_3.isSelected()) {

        changeQuery(
          SequenceDescription.GENEEXONINTRON,
          0,
          flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeUpstream.isSelected()) {

        changeQuery(SequenceDescription.GENEFLANKS, flank5.getTextAsInt(), 0);
        flank5.setEnabled(true);

      } else if (includeDownStream.isSelected()) {

        changeQuery(SequenceDescription.GENEFLANKS, 0, flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeExonSequence.isSelected()) {

        changeQuery(SequenceDescription.GENEEXONS, 0, 0);

      } else if (includeExonsPlus5And3Flanks.isSelected()) {

        changeQuery(
          SequenceDescription.GENEEXONS,
          flank5.getTextAsInt(),
          flank3.getTextAsInt());

        flank5.setEnabled(true);
        flank3.setEnabled(true);

      } else if (includeExonsPlus3Flanks.isSelected()) {

        changeQuery(SequenceDescription.GENEEXONS, 0, flank3.getTextAsInt());
        flank3.setEnabled(true);

      } else if (includeExonsPlus5Flanks.isSelected()) {

        changeQuery(SequenceDescription.GENEEXONS, flank5.getTextAsInt(), 0);
        flank5.setEnabled(true);

      }
    }

  }

  /**
   * Updates sequence description on the query
   * @param imageFilePath image to be displayed
   * @param sequenceType sequence type (constant from SeequenceDescription), or UNSUPPORTED if unsupported
   * @param leftFlank left flank in base pairs
   * @param rightFlank rightt flank in base pairs
   */
  private void changeQuery(int sequenceType, int leftFlank, int rightFlank) {

    logger.fine(
      "type: " + sequenceType + "\t5': " + leftFlank + "\tf3': " + rightFlank);

    if (sequenceType == -1) {
      feedback.warning("Unsupported sequence type.");
      return;
    } else if (sequenceType == NONE) {

      query.setSequenceDescription(null);

    } else {

      try {

        SequenceDescription newAttribute =
          new SequenceDescription(sequenceType, leftFlank, rightFlank);

        SequenceDescription oldAttribute = query.getSequenceDescription();

        if (oldAttribute != newAttribute
         && !newAttribute.equals(oldAttribute)) {

          query.setSequenceDescription(newAttribute);
          
          System.out.println(" seq11 descripiton "+query.getSequenceDescription());
          
          sequenceDescritpionChanged(query,newAttribute,newAttribute);
          
          
        }

      } catch (InvalidQueryException e) {
        feedback.warning("Invalid sequence attribute. " + e.getMessage());
        e.printStackTrace();
      }

    }

  }

  private void enableGeneButtons() {
    for (int i = 0; i < geneButtons.length; i++)
      geneButtons[i].setEnabled(true);

  }

  private void enableTranscriptButtons() {
    for (int i = 0; i < includeButtons.length; i++)
      includeButtons[i].setEnabled(true);
  }

  private void setButtonsEnabled(JRadioButton[] buttons, boolean enabled) {
    for (int i = 0; i < buttons.length; i++)
      buttons[i].setEnabled(enabled);
  }

  /**
   * Callback method called when an item in the tree is selected.
   * Brings this widget to the front if the selected node in the tree is a sequence description.
   * TODO get scrolling to a selected attribute working properly
   */
  public void valueChanged(TreeSelectionEvent e) {

    if (query.getSequenceDescription() != null) {

      if (e.getNewLeadSelectionPath() != null
        && e.getNewLeadSelectionPath().getLastPathComponent() != null) {

        DefaultMutableTreeNode node =
          (DefaultMutableTreeNode) e
            .getNewLeadSelectionPath()
            .getLastPathComponent();

        if (node != null) {

          TreeNodeData tnd = (TreeNodeData) node.getUserObject();
          if (tnd.getSequenceDescription() != null)
            for (Component p, c = this; c != null; c = p) {
              p = c.getParent();
              if (p instanceof JTabbedPane)
                 ((JTabbedPane) p).setSelectedComponent(c);
              else if (p instanceof JScrollPane) {
                // not sure if this is being used
                Point pt = c.getLocation();
                Rectangle r = new Rectangle(pt);
                ((JScrollPane) p).scrollRectToVisible(r);

              }

            }
        }
      }
    }

  }

}
